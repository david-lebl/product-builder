package mpbuilder.pricing
package core

import mpbuilder.shared.*
import java.time.Instant

/** Pure, language-independent pricing, promo-code, and margin engine.
  *
  * Calculation pipeline:
  *   1. Component charges (Material, Cutting, Ink, Finishes) — all rounded `HALF_UP`
  *   2. Order-level variable charges (Category, Process, Method, Fold, Binding, VersionChange)
  *   3. Scoped `VolumeCurve` multiplier (most specific scope wins: cat+method > method > cat > global)
  *   4. Manufacturing speed multiplier + dynamic queue/busy-period surge (capped)
  *   5. Deduplicated one-time setup fees (NOT volume-discounted, NOT speed-multiplied)
  *   6. Minimum order floor (net)
  *   7. Promo code discount (validated against constraints)
  *   8. Tax / VAT calculation (`TaxPolicy`)
  */
private[pricing] object PricingEngine:

  def calculate(
      quoteId: QuoteId,
      subject: PricingSubject,
      basePricelist: Pricelist,
      context: DynamicPricingContext = DynamicPricingContext.default,
      agreement: Option[CustomerPriceAgreement] = None,
      promoCode: Option[PromoCode] = None,
      promoContext: Option[PromoContext] = None,
      now: Instant = Instant.EPOCH,
  ): Validation[PricingError, PriceQuote] =
    val pricelist = agreement.fold(basePricelist)(ag => AgreementResolver.resolve(basePricelist, ag, subject.categoryId))
    val primaryMethodId = subject.components.toList.flatMap(_.printingMethodId).headOption

    val componentQuotesV: Validation[PricingError, NonEmptyChunk[ComponentQuote]] =
      val listV = subject.components.toList.foldLeft(
        Validation.succeed(List.empty[ComponentQuote]): Validation[PricingError, List[ComponentQuote]]
      ) { (accV, comp) =>
        accV.zipWith(calculateComponent(comp, subject.categoryId, subject.quantity, pricelist))(_ :+ _)
      }
      listV.flatMap { list =>
        NonEmptyChunk.fromIterableOption(list) match
          case Some(nec) => Validation.succeed(nec)
          case None      => Validation.fail(PricingError.PricelistNotFound(pricelist.currency))
      }

    componentQuotesV.flatMap { componentQuotes =>
      val orderCharges = computeOrderVariableCharges(subject, primaryMethodId, pricelist)

      val variableSubtotalNet =
        (componentQuotes.map(_.totalNet).reduceLeft(_ + _) +
          orderCharges.map(_.total).foldLeft(Money.zero)(_ + _)).rounded

      val volumeMultiplier      = resolveVolumeMultiplier(subject, componentQuotes, primaryMethodId, pricelist)
      val discountedSubtotalNet = (variableSubtotalNet * volumeMultiplier).rounded

      val (speedAdjustment, afterSpeedNet) =
        computeSpeedAdjustment(subject.speed, pricelist, context, discountedSubtotalNet)

      val setupFees      = collectSetupFees(subject, primaryMethodId, pricelist)
      val totalSetupNet  = setupFees.map(_.total).foldLeft(Money.zero)(_ + _).rounded
      val billableNet    = (afterSpeedNet + totalSetupNet).rounded

      val (afterFloorNet, minAdjustment) = pricelist.minimumOrderNet match
        case Some(floor) if billableNet < floor =>
          val delta = (floor - billableNet).rounded
          val line = ChargeLine(
            kind = ChargeKind.MinimumOrderAdjustment(floor, billableNet),
            componentKey = None,
            unitPrice = delta,
            quantity = BigDecimal(1),
            total = delta,
          )
          (floor.rounded, Some(line))
        case _ => (billableNet, None)

      val promoV: Validation[PricingError, (Option[ChargeLine], Money)] = promoCode match
        case None => Validation.succeed((None, afterFloorNet))
        case Some(code) =>
          val ctx = promoContext.getOrElse(PromoContext(now))
          applyPromoCode(code, subject.categoryId, afterFloorNet, pricelist.currency, ctx)

      promoV.map { case (promoLine, finalNet) =>
        val vatRate   = pricelist.taxPolicy.rateFor(subject.categoryId)
        val taxAmount = vatRate.taxOnNet(finalNet)
        val gross     = (finalNet + taxAmount).rounded
        val taxSummary = TaxSummary(
          rate = vatRate,
          netAmount = finalNet,
          taxAmount = taxAmount,
          grossAmount = gross,
        )

        PriceQuote(
          quoteId = quoteId,
          pricelistId = pricelist.id,
          pricelistVersion = pricelist.version,
          currency = pricelist.currency,
          quantity = subject.quantity,
          versionCount = subject.versionCount,
          componentQuotes = componentQuotes,
          orderCharges = orderCharges,
          variableSubtotalNet = variableSubtotalNet,
          volumeMultiplier = volumeMultiplier,
          discountedSubtotalNet = discountedSubtotalNet,
          speedAdjustment = speedAdjustment,
          setupFees = setupFees,
          minimumAdjustment = minAdjustment,
          promoDiscount = promoLine,
          tax = taxSummary,
          netTotal = finalNet,
          grossTotal = gross,
          calculatedAt = now,
        )
      }
    }

  private def calculateComponent(
      comp: PricingComponent,
      categoryId: CategoryId,
      orderQty: Quantity,
      pricelist: Pricelist,
  ): Validation[PricingError, ComponentQuote] =
    // Material rate: prefer exact stockFormatId match, fall back to material-wide rate
    val materialEntryOpt =
      comp.stockFormatId.flatMap { sfId =>
        findEntry(pricelist, PricingTarget.MaterialRate(comp.materialId, Some(sfId)), ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId)
      }.orElse(
        findEntry(pricelist, PricingTarget.MaterialRate(comp.materialId, None), ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId)
      )

    val materialLineV: Validation[PricingError, ChargeLine] = materialEntryOpt match
      case None => Validation.fail(PricingError.MissingMaterialRate(comp.materialId, comp.key))
      case Some(entry) =>
        evaluateRate(
          rate = entry.rate,
          target = entry.target,
          kind = ChargeKind.MaterialCharge(comp.materialId, comp.stockFormatId),
          comp = comp,
          orderQty = orderQty,
          sideMultiplier = 1,
          discreteParam = None,
          linearMeters = None,
        )

    val cuttingLineOpt: Option[ChargeLine] =
      if comp.sheetsUsed > 0 && comp.guillotineCutsPerSheet > 0 then
        findEntry(pricelist, PricingTarget.GuillotineCutting, ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId).map { entry =>
          val costPerCut = entry.rate match
            case RateModel.PerUnit(r)  => r
            case RateModel.PerSheet(r) => r
            case RateModel.FlatFee(r)  => r
            case _                     => Money.zero
          val costPerSheet = (costPerCut * comp.guillotineCutsPerSheet).rounded
          val total        = (costPerSheet * comp.sheetsUsed).rounded
          ChargeLine(
            kind = ChargeKind.CuttingCharge(comp.guillotineCutsPerSheet),
            componentKey = Some(comp.key),
            unitPrice = costPerSheet,
            quantity = BigDecimal(comp.sheetsUsed),
            total = total,
          )
        }
      else None

    val inkLineV: Validation[PricingError, Option[ChargeLine]] =
      (comp.printingMethodId, comp.inkKey) match
        case (Some(pmId), Some(inkKey)) =>
          findEntry(pricelist, PricingTarget.InkRate(pmId, inkKey), ChargePhase.VariableDiscountable, categoryId, Some(pmId)) match
            case None => Validation.succeed(None)
            case Some(entry) =>
              evaluateRate(
                rate = entry.rate,
                target = entry.target,
                kind = ChargeKind.InkCharge(pmId, inkKey),
                comp = comp,
                orderQty = orderQty,
                sideMultiplier = 1,
                discreteParam = None,
                linearMeters = None,
              ).map(Some(_))
        case _ => Validation.succeed(None)

    val finishLinesV: Validation[PricingError, List[ChargeLine]] =
      comp.finishes.foldLeft(
        Validation.succeed(List.empty[ChargeLine]): Validation[PricingError, List[ChargeLine]]
      ) { (accV, fin) =>
        val byId = findEntry(pricelist, PricingTarget.FinishRate(fin.finishId, true), ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId)
          .orElse(findEntry(pricelist, PricingTarget.FinishRate(fin.finishId, false), ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId))
        val byType = findEntry(pricelist, PricingTarget.FinishTypeRate(fin.finishTypeKey, true), ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId)
          .orElse(findEntry(pricelist, PricingTarget.FinishTypeRate(fin.finishTypeKey, false), ChargePhase.VariableDiscountable, categoryId, comp.printingMethodId))

        val finLineV: Validation[PricingError, Option[ChargeLine]] = byId.orElse(byType) match
          case None => Validation.succeed(None)
          case Some(entry) =>
            val multBySides = entry.target match
              case PricingTarget.FinishRate(_, m)     => m
              case PricingTarget.FinishTypeRate(_, m) => m
              case _                                  => false
            val sideMult = if multBySides then fin.sideMultiplier else 1
            evaluateRate(
              rate = entry.rate,
              target = entry.target,
              kind = ChargeKind.FinishCharge(fin.finishId, fin.finishTypeKey),
              comp = comp,
              orderQty = orderQty,
              sideMultiplier = sideMult,
              discreteParam = fin.discreteParam,
              linearMeters = fin.linearMeters,
            ).map(Some(_))

        accV.zipWith(finLineV)((acc, opt) => acc ++ opt.toList)
      }

    Validation.validateWith(materialLineV, inkLineV, finishLinesV) { (matLine, inkLine, finLines) =>
      ComponentQuote(
        key = comp.key,
        materialLine = matLine,
        cuttingLine = cuttingLineOpt,
        inkLine = inkLine,
        finishLines = finLines,
        sheetsUsed = comp.sheetsUsed,
      )
    }

  /** Evaluates ANY `RateModel` cleanly — billing unit is driven by the rate itself, NOT by the material! */
  private def evaluateRate(
      rate: RateModel,
      target: PricingTarget,
      kind: ChargeKind,
      comp: PricingComponent,
      orderQty: Quantity,
      sideMultiplier: Int,
      discreteParam: Option[Int],
      linearMeters: Option[BigDecimal],
  ): Validation[PricingError, ChargeLine] =
    val sideMult = BigDecimal(sideMultiplier)
    rate match
      case RateModel.PerUnit(r) =>
        val qty   = BigDecimal(comp.totalFlatUnits)
        val unit  = (r * sideMult).rounded
        val total = (unit * qty).rounded
        Validation.succeed(ChargeLine(kind, Some(comp.key), unit, qty, total))

      case RateModel.PerSheet(r) =>
        val sheets = if comp.sheetsUsed > 0 then comp.sheetsUsed else comp.totalFlatUnits
        val unit   = (r * sideMult).rounded
        val total  = (unit * BigDecimal(sheets)).rounded
        Validation.succeed(ChargeLine(kind, Some(comp.key), unit, BigDecimal(sheets), total))

      case RateModel.PerSqMeter(r, minAreaOpt) =>
        val rawTotalArea = comp.areaPerUnitSqM.value * BigDecimal(comp.totalFlatUnits)
        val billableArea = minAreaOpt.fold(rawTotalArea)(minA => rawTotalArea.max(minA.value))
        val effectiveRate = (r * sideMult).rounded
        val total = (effectiveRate * billableArea).rounded
        Validation.succeed(ChargeLine(kind, Some(comp.key), effectiveRate, billableArea, total))

      case RateModel.TieredArea(tiers, basis) =>
        val totalArea = comp.areaPerUnitSqM * comp.totalFlatUnits
        val lookupArea = basis match
          case AreaTierBasis.PerItemArea  => comp.areaPerUnitSqM
          case AreaTierBasis.TotalJobArea => totalArea
        val selected = tiers.toList.filter(_.minArea <= lookupArea).maxByOption(_.minArea)
        selected match
          case None => Validation.fail(PricingError.NoMatchingAreaTier(comp.materialId, lookupArea))
          case Some(tier) =>
            val effectiveRate = (tier.ratePerSqM * sideMult).rounded
            val total         = (effectiveRate * totalArea.value).rounded
            Validation.succeed(ChargeLine(kind, Some(comp.key), effectiveRate, totalArea.value, total))

      case RateModel.PerLinearMeter(r) =>
        linearMeters match
          case None => Validation.fail(PricingError.MissingLinearLength(target, comp.key))
          case Some(meters) =>
            val totalMeters = meters * BigDecimal(orderQty.value)
            val unitRate    = (r * sideMult).rounded
            val total       = (unitRate * totalMeters).rounded
            Validation.succeed(ChargeLine(kind, Some(comp.key), unitRate, totalMeters, total))

      case RateModel.PerPerimeterMeter(r) =>
        val totalPerimeter = comp.perimeterPerUnitM * BigDecimal(comp.totalFlatUnits)
        val unitRate       = (r * sideMult).rounded
        val total          = (unitRate * totalPerimeter).rounded
        Validation.succeed(ChargeLine(kind, Some(comp.key), unitRate, totalPerimeter, total))

      case RateModel.ByDiscreteParam(rates, scaleByArea) =>
        discreteParam match
          case None => Validation.fail(PricingError.MissingDiscreteParam(target, comp.key))
          case Some(pv) =>
            rates.get(pv) match
              case None => Validation.fail(PricingError.MissingDiscreteRate(target, pv))
              case Some(baseRate) =>
                val effectiveRate = (baseRate * sideMult).rounded
                if scaleByArea then
                  val totalArea = comp.areaPerUnitSqM.value * BigDecimal(orderQty.value)
                  val total     = (effectiveRate * totalArea).rounded
                  Validation.succeed(ChargeLine(kind, Some(comp.key), effectiveRate, totalArea, total))
                else
                  val qty   = BigDecimal(orderQty.value)
                  val total = (effectiveRate * qty).rounded
                  Validation.succeed(ChargeLine(kind, Some(comp.key), effectiveRate, qty, total))

      case RateModel.FlatFee(amount) =>
        val rounded = (amount * sideMult).rounded
        Validation.succeed(ChargeLine(kind, Some(comp.key), rounded, BigDecimal(1), rounded))

  private def computeOrderVariableCharges(
      subject: PricingSubject,
      primaryMethodId: Option[PrintingMethodId],
      pricelist: Pricelist,
  ): List[ChargeLine] =
    val qty = BigDecimal(subject.quantity.value)

    val processLines = subject.components.toList
      .flatMap(_.printingProcessKey)
      .distinct
      .flatMap { procKey =>
        findEntry(pricelist, PricingTarget.PrintingProcessRate(procKey), ChargePhase.VariableDiscountable, subject.categoryId, primaryMethodId)
          .map(e => evalSimpleOrderRate(e.rate, ChargeKind.PrintingProcessCharge(procKey), qty))
      }

    val categoryLine =
      findEntry(pricelist, PricingTarget.CategoryRate(subject.categoryId), ChargePhase.VariableDiscountable, subject.categoryId, primaryMethodId)
        .map(e => evalSimpleOrderRate(e.rate, ChargeKind.CategoryCharge(subject.categoryId), qty))

    val foldLine = subject.foldKey.flatMap { fk =>
      findEntry(pricelist, PricingTarget.FoldRate(fk), ChargePhase.VariableDiscountable, subject.categoryId, primaryMethodId)
        .map(e => evalSimpleOrderRate(e.rate, ChargeKind.FoldCharge(fk), qty))
    }

    val bindingLine = subject.bindingKey.flatMap { bk =>
      findEntry(pricelist, PricingTarget.BindingRate(bk), ChargePhase.VariableDiscountable, subject.categoryId, primaryMethodId)
        .map(e => evalSimpleOrderRate(e.rate, ChargeKind.BindingCharge(bk), qty))
    }

    val versionChangeLine =
      if subject.versionCount.extraVersions > 0 then
        findEntry(pricelist, PricingTarget.VersionChange, ChargePhase.VariableDiscountable, subject.categoryId, primaryMethodId).map { e =>
          val extra = BigDecimal(subject.versionCount.extraVersions)
          evalSimpleOrderRate(e.rate, ChargeKind.VersionChangeCharge(subject.versionCount.extraVersions), extra)
        }
      else None

    processLines ++ categoryLine.toList ++ foldLine.toList ++ bindingLine.toList ++ versionChangeLine.toList

  private def evalSimpleOrderRate(rate: RateModel, kind: ChargeKind, qty: BigDecimal): ChargeLine =
    rate match
      case RateModel.FlatFee(amt) => ChargeLine(kind, None, amt.rounded, BigDecimal(1), amt.rounded)
      case RateModel.PerUnit(r)   => ChargeLine(kind, None, r.rounded, qty, (r * qty).rounded)
      case RateModel.PerSheet(r)  => ChargeLine(kind, None, r.rounded, qty, (r * qty).rounded)
      case _                      => ChargeLine(kind, None, Money.zero, qty, Money.zero)

  private def resolveVolumeMultiplier(
      subject: PricingSubject,
      componentQuotes: NonEmptyChunk[ComponentQuote],
      primaryMethodId: Option[PrintingMethodId],
      pricelist: Pricelist,
  ): BigDecimal =
    val totalSheets = componentQuotes.map(_.sheetsUsed).sum
    val totalArea   = subject.components.toList.map(c => c.areaPerUnitSqM.value * BigDecimal(c.totalFlatUnits)).sum

    val matchingCurves = pricelist.volumeCurves
      .filter(_.scope.matches(subject.categoryId, primaryMethodId))
      .filter { curve =>
        curve.basis match
          case VolumeBasis.TotalPressSheets => totalSheets > 0
          case VolumeBasis.TotalAreaSqM     => totalArea > BigDecimal(0)
          case VolumeBasis.FinishedUnits    => true
      }

    // Prefer highest scope specificity, then TotalPressSheets > TotalAreaSqM > FinishedUnits
    val bestCurve = matchingCurves.sortBy { c =>
      val basisPriority = c.basis match
        case VolumeBasis.TotalPressSheets => 2
        case VolumeBasis.TotalAreaSqM     => 1
        case VolumeBasis.FinishedUnits    => 0
      (-c.scope.specificity, -basisPriority)
    }.headOption

    bestCurve
      .flatMap { curve =>
        val metric = curve.basis match
          case VolumeBasis.TotalPressSheets => BigDecimal(totalSheets)
          case VolumeBasis.TotalAreaSqM     => totalArea
          case VolumeBasis.FinishedUnits    => BigDecimal(subject.quantity.value)
        curve.tiers.toList.filter(_.matches(metric)).maxByOption(_.minThreshold).map(_.multiplier)
      }
      .getOrElse(BigDecimal(1))

  def speedMultiplierSummary(
      speed: ManufacturingSpeed,
      pricelist: Pricelist,
      context: DynamicPricingContext,
  ): SpeedMultiplierSummary =
    pricelist.speedPolicies.get(speed) match
      case None =>
        SpeedMultiplierSummary(speed, BigDecimal(1), BigDecimal(1), BigDecimal(0))
      case Some(policy) =>
        val queueAdd =
          if !policy.subjectToSurge then BigDecimal(0)
          else policy.queueThresholds.filter(_.minUtilisation <= context.globalUtilisation).map(_.additionalMultiplier).sum

        val busyAdd =
          if !policy.subjectToSurge then BigDecimal(0)
          else context.busyPeriods.map(_.additionalMultiplier).sum

        val rawMult = policy.baseMultiplier + queueAdd + busyAdd
        val effMult = if policy.subjectToSurge then rawMult.min(context.expressMultiplierCap) else policy.baseMultiplier
        val pct     = ((effMult - BigDecimal(1)) * BigDecimal(100)).setScale(0, BigDecimal.RoundingMode.HALF_UP)
        SpeedMultiplierSummary(speed, policy.baseMultiplier, effMult, pct)

  private def computeSpeedAdjustment(
      speed: ManufacturingSpeed,
      pricelist: Pricelist,
      context: DynamicPricingContext,
      discountedSubtotalNet: Money,
  ): (Option[ChargeLine], Money) =
    val summary = speedMultiplierSummary(speed, pricelist, context)
    val effMult = summary.effectiveMultiplier

    if effMult == BigDecimal(1) then (None, discountedSubtotalNet)
    else
      val afterSpeed = (discountedSubtotalNet * effMult).rounded
      val delta      = (afterSpeed - discountedSubtotalNet).rounded
      val line = ChargeLine(
        kind = ChargeKind.SpeedAdjustment(speed, effMult),
        componentKey = None,
        unitPrice = delta,
        quantity = BigDecimal(1),
        total = delta,
      )
      (Some(line), afterSpeed)

  private def collectSetupFees(
      subject: PricingSubject,
      primaryMethodId: Option[PrintingMethodId],
      pricelist: Pricelist,
  ): List[ChargeLine] =
    val allFinishes = subject.components.toList.flatMap(_.finishes)
    val distinctById = allFinishes.distinctBy(_.finishId)

    val (finishIdLines, coveredTypes) = distinctById.foldLeft((List.empty[ChargeLine], Set.empty[String])) {
      case ((acc, covered), fin) =>
        findEntry(pricelist, PricingTarget.FinishRate(fin.finishId, false), ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId)
          .orElse(findEntry(pricelist, PricingTarget.FinishRate(fin.finishId, true), ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId)) match
          case Some(e) =>
            val amt = extractFlat(e.rate)
            (acc :+ ChargeLine(ChargeKind.SetupFee(e.target), None, amt, BigDecimal(1), amt), covered + fin.finishTypeKey)
          case None => (acc, covered)
    }

    val finishTypeLines = distinctById
      .distinctBy(_.finishTypeKey)
      .filterNot(f => coveredTypes.contains(f.finishTypeKey))
      .flatMap { fin =>
        findEntry(pricelist, PricingTarget.FinishTypeRate(fin.finishTypeKey, false), ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId)
          .orElse(findEntry(pricelist, PricingTarget.FinishTypeRate(fin.finishTypeKey, true), ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId))
          .map { e =>
            val amt = extractFlat(e.rate)
            ChargeLine(ChargeKind.SetupFee(e.target), None, amt, BigDecimal(1), amt)
          }
      }

    val methodLines = subject.components.toList
      .flatMap(_.printingMethodId)
      .distinct
      .flatMap { pmId =>
        findEntry(pricelist, PricingTarget.PrintingMethodRate(pmId), ChargePhase.OneTimeSetup, subject.categoryId, Some(pmId)).map { e =>
          val amt = extractFlat(e.rate)
          ChargeLine(ChargeKind.SetupFee(e.target), None, amt, BigDecimal(1), amt)
        }
      }

    val foldLine = subject.foldKey.flatMap { fk =>
      findEntry(pricelist, PricingTarget.FoldRate(fk), ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId).map { e =>
        val amt = extractFlat(e.rate)
        ChargeLine(ChargeKind.SetupFee(e.target), None, amt, BigDecimal(1), amt)
      }
    }.toList

    val bindingLine = subject.bindingKey.flatMap { bk =>
      findEntry(pricelist, PricingTarget.BindingRate(bk), ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId).map { e =>
        val amt = extractFlat(e.rate)
        ChargeLine(ChargeKind.SetupFee(e.target), None, amt, BigDecimal(1), amt)
      }
    }.toList

    val versionSetupLine =
      if subject.versionCount.extraVersions > 0 then
        findEntry(pricelist, PricingTarget.VersionChange, ChargePhase.OneTimeSetup, subject.categoryId, primaryMethodId).map { e =>
          val extra = BigDecimal(subject.versionCount.extraVersions)
          val unit  = extractFlat(e.rate)
          val total = (unit * extra).rounded
          ChargeLine(ChargeKind.VersionChangeCharge(subject.versionCount.extraVersions), None, unit, extra, total)
        }.toList
      else Nil

    finishIdLines ++ finishTypeLines ++ methodLines ++ foldLine ++ bindingLine ++ versionSetupLine

  /** Validates and evaluates a promo code against a single-item or multi-item cart net total. */
  def evaluatePromo(
      promo: PromoCode,
      categoryIds: Set[CategoryId],
      netBeforePromo: Money,
      currency: Currency,
      ctx: PromoContext,
  ): Validation[PricingError, PromoEvaluation] =
    val codeStr = promo.code.value
    val c       = promo.constraints

    val activeV = if promo.isActive then Validation.unit else Validation.fail(PricingError.PromoCodeInactive(codeStr))
    val windowV =
      val afterStart  = c.validFrom.forall(f => !ctx.now.isBefore(f))
      val beforeEnd   = c.validUntil.forall(u => !ctx.now.isAfter(u))
      if afterStart && beforeEnd then Validation.unit else Validation.fail(PricingError.PromoCodeExpired(codeStr))
    val usageV =
      if c.maxUses.forall(m => c.currentUses < m) then Validation.unit
      else Validation.fail(PricingError.PromoCodeUsageLimitReached(codeStr))
    val minOrderV =
      if c.minimumOrderNet.forall(min => netBeforePromo >= min) then Validation.unit
      else Validation.fail(PricingError.PromoCodeNotEligible(codeStr, "Order net total below minimum"))
    val catV =
      if c.allowedCategories.isEmpty || categoryIds.exists(c.allowedCategories.contains) then Validation.unit
      else Validation.fail(PricingError.PromoCodeNotEligible(codeStr, s"Categories '${categoryIds.map(_.value).mkString(", ")}' not eligible"))
    val segV =
      if c.allowedSegments.isEmpty || ctx.customerSegment.exists(c.allowedSegments.contains) then Validation.unit
      else Validation.fail(PricingError.PromoCodeNotEligible(codeStr, "Customer segment not eligible"))
    val custV =
      if c.allowedCustomerIds.isEmpty || ctx.customerId.exists(c.allowedCustomerIds.contains) then Validation.unit
      else Validation.fail(PricingError.PromoCodeNotEligible(codeStr, "Customer not eligible"))

    val allChecks = List(activeV, windowV, usageV, minOrderV, catV, segV, custV)
      .foldLeft(Validation.unit: Validation[PricingError, Unit])(_.zipRight(_))

    allChecks.flatMap { _ =>
      promo.benefit match
        case PromoBenefit.PercentageOff(pct) =>
          val discount = pct.of(netBeforePromo).atMost(netBeforePromo).rounded
          Validation.succeed(PromoEvaluation(promo.id, promo.code, promo.benefit, discount, freeDelivery = false))
        case PromoBenefit.FixedAmountOff(price) =>
          if price.currency != currency then
            Validation.fail(PricingError.CurrencyMismatch(currency, price.currency))
          else
            val discount = price.amount.atMost(netBeforePromo).rounded
            Validation.succeed(PromoEvaluation(promo.id, promo.code, promo.benefit, discount, freeDelivery = false))
        case PromoBenefit.FreeDelivery =>
          Validation.succeed(PromoEvaluation(promo.id, promo.code, promo.benefit, Money.zero, freeDelivery = true))
    }

  private def applyPromoCode(
      promo: PromoCode,
      categoryId: CategoryId,
      netBeforePromo: Money,
      currency: Currency,
      ctx: PromoContext,
  ): Validation[PricingError, (Option[ChargeLine], Money)] =
    evaluatePromo(promo, Set(categoryId), netBeforePromo, currency, ctx).map { eval =>
      if eval.discountNet.isPositive then
        val line = ChargeLine(ChargeKind.PromoDiscount(eval.codeId, eval.code.value), None, -eval.discountNet, BigDecimal(1), -eval.discountNet)
        (Some(line), (netBeforePromo - eval.discountNet).rounded)
      else
        (None, netBeforePromo)
    }

  /** Calculates production cost & compares against net selling price from a quote. */
  def analyzeMargin(
      subject: PricingSubject,
      quote: PriceQuote,
      costSheet: ProductionCostSheet,
      lowMarginThreshold: Percentage = Percentage.unsafe(BigDecimal(15)),
  ): Validation[PricingError, MarginAnalysis] =
    if costSheet.currency != quote.currency then
      Validation.fail(PricingError.CurrencyMismatch(quote.currency, costSheet.currency))
    else
      val directCost = subject.components.toList.foldLeft(Money.zero) { (acc, comp) =>
        val matCost = costSheet.entries
          .collectFirst { case CostEntry(PricingTarget.MaterialRate(id, _), rate) if id == comp.materialId => rate }
          .flatMap(r => evaluateRate(r, PricingTarget.MaterialRate(comp.materialId), ChargeKind.MaterialCharge(comp.materialId, None), comp, subject.quantity, 1, None, None).toOption)
          .map(_.total)
          .getOrElse(Money.zero)

        val finCost = comp.finishes.foldLeft(Money.zero) { (fAcc, fin) =>
          val fc = costSheet.entries
            .collectFirst { case CostEntry(PricingTarget.FinishRate(id, _), rate) if id == fin.finishId => rate }
            .flatMap(r => evaluateRate(r, PricingTarget.FinishRate(fin.finishId), ChargeKind.FinishCharge(fin.finishId, fin.finishTypeKey), comp, subject.quantity, fin.sideMultiplier, fin.discreteParam, fin.linearMeters).toOption)
            .map(_.total)
            .getOrElse(Money.zero)
          fAcc + fc
        }
        acc + matCost + finCost
      }

      val prodCost  = (directCost * costSheet.overheadFactor).rounded
      val selling   = quote.netTotal
      val marginNet = (selling - prodCost).rounded
      val marginPct =
        if prodCost.isPositive then ((marginNet.value / prodCost.value) * BigDecimal(100)).setScale(2, BigDecimal.RoundingMode.HALF_UP)
        else BigDecimal(100)
      val isBelowCost = selling < prodCost

      val warnings =
        if isBelowCost then List(MarginWarning.BelowProductionCost((prodCost - selling).rounded))
        else if marginPct < lowMarginThreshold.value then
          List(MarginWarning.LowMargin(Percentage.unsafe(marginPct.max(BigDecimal(0)).min(BigDecimal(100))), lowMarginThreshold))
        else Nil

      Validation.succeed(
        MarginAnalysis(
          productionCostNet = prodCost,
          sellingPriceNet = selling,
          marginNet = marginNet,
          marginPercent = marginPct,
          isBelowCost = isBelowCost,
          warnings = warnings,
        )
      )

  private def findEntry(
      pricelist: Pricelist,
      target: PricingTarget,
      phase: ChargePhase,
      categoryId: CategoryId,
      printingMethodId: Option[PrintingMethodId],
  ): Option[PriceEntry] =
    pricelist.entries
      .filter(e => e.phase == phase && e.target == target && e.scope.matches(categoryId, printingMethodId))
      .maxByOption(_.scope.specificity)

  private def extractFlat(rate: RateModel): Money = rate match
    case RateModel.FlatFee(amt) => amt.rounded
    case RateModel.PerUnit(r)   => r.rounded
    case RateModel.PerSheet(r)  => r.rounded
    case _                      => Money.zero
