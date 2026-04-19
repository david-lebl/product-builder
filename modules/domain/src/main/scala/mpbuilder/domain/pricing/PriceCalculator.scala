package mpbuilder.domain.pricing

import zio.prelude.*
import mpbuilder.domain.model.*

object PriceCalculator:

  /** Calculate price without dynamic context (backward-compatible). */
  def calculate(
      config: ProductConfiguration,
      pricelist: Pricelist,
      lang: Language = Language.En,
  ): Validation[PricingError, PriceBreakdown] =
    calculateWithContext(config, pricelist, PricingContext.default, lang)

  /** Calculate price with dynamic pricing context (queue utilisation, busy periods). */
  def calculateWithContext(
      config: ProductConfiguration,
      pricelist: Pricelist,
      context: PricingContext,
      lang: Language = Language.En,
  ): Validation[PricingError, PriceBreakdown] =
    val rules = pricelist.rules

    extractQuantity(config.specifications).flatMap { quantity =>
      val componentBreakdownsV: Validation[PricingError, List[ComponentBreakdown]] =
        config.components
          .map { comp => calculateComponentBreakdown(comp, config.specifications, rules, quantity, lang) }
          .foldLeft(Validation.succeed(List.empty[ComponentBreakdown]): Validation[PricingError, List[ComponentBreakdown]]) {
            (accV, cbV) => accV.zipWith(cbV)(_ :+ _)
          }

      componentBreakdownsV.map { componentBreakdowns =>
        val processSurcharge = findProcessSurcharge(config.printingMethod, rules, quantity, lang)
        val categorySurcharge = findCategorySurcharge(config.category, rules, quantity, lang)

        val foldType = config.specifications.get(SpecKind.FoldType).collect {
          case SpecValue.FoldTypeSpec(ft) => ft
        }
        val bindingMethod = config.specifications.get(SpecKind.BindingMethod).collect {
          case SpecValue.BindingMethodSpec(bm) => bm
        }
        val foldSurcharge = findFoldSurcharge(foldType, rules, quantity, lang)
        val bindingSurcharge = findBindingSurcharge(bindingMethod, rules, quantity, lang)

        val componentTotals = componentBreakdowns.flatMap { cb =>
          cb.materialLine.lineTotal ::
            cb.cuttingLine.map(_.lineTotal).toList :::
            cb.inkConfigLine.map(_.lineTotal).toList :::
            cb.finishLines.map(_.lineTotal)
        }

        val allLineTotals =
          componentTotals :::
            processSurcharge.map(_.lineTotal).toList :::
            categorySurcharge.map(_.lineTotal).toList :::
            foldSurcharge.map(_.lineTotal).toList :::
            bindingSurcharge.map(_.lineTotal).toList

        val subtotal = allLineTotals.foldLeft(Money.zero)(_ + _)

        val totalSheets = componentBreakdowns.map(_.sheetsUsed).sum
        val sheetTierMultiplier =
          if totalSheets > 0 then findBestSheetQuantityTier(rules, totalSheets).map(_.multiplier)
          else None

        val multiplier = sheetTierMultiplier
          .orElse(findBestQuantityTier(rules, quantity).map(_.multiplier))
          .getOrElse(BigDecimal(1))

        val discountedSubtotal = (subtotal * multiplier).rounded

        // Manufacturing speed surcharge — applied after quantity discount, before setup fees
        val selectedSpeed = config.specifications.get(SpecKind.ManufacturingSpeed).collect {
          case SpecValue.ManufacturingSpeedSpec(speed) => speed
        }
        val (speedSurcharge, afterSpeedSubtotal) =
          computeSpeedSurcharge(selectedSpeed, rules, context, discountedSubtotal, lang)

        val allSelectedFinishes = config.components.flatMap(_.finishes)
        val setupFees = collectSetupFees(allSelectedFinishes, foldType, bindingMethod, rules, lang)
        val totalSetupFees = setupFees.map(_.lineTotal).foldLeft(Money.zero)(_ + _)
        val billable = (afterSpeedSubtotal + totalSetupFees).rounded

        val minimumRule = rules.collectFirst { case r: PricingRule.MinimumOrderPrice => r }
        val (total, minimumApplied) = minimumRule match
          case Some(minRule) if billable.value < minRule.minTotal.value =>
            (minRule.minTotal.rounded, Some(billable))
          case _ =>
            (billable, None)

        PriceBreakdown(
          componentBreakdowns = componentBreakdowns,
          processSurcharge = processSurcharge,
          categorySurcharge = categorySurcharge,
          foldSurcharge = foldSurcharge,
          bindingSurcharge = bindingSurcharge,
          subtotal = subtotal,
          quantityMultiplier = multiplier,
          speedSurcharge = speedSurcharge,
          setupFees = setupFees,
          minimumApplied = minimumApplied,
          total = total,
          currency = pricelist.currency,
          quantity = quantity,
        )
      }
    }

  /** Compute the manufacturing speed surcharge line item and the adjusted subtotal.
    *
    * The speed multiplier is: base multiplier + queue threshold adjustments + busy period adjustments,
    * capped at expressSurchargeCap. Economy prices are fixed (no dynamic adjustment).
    */
  private def computeSpeedSurcharge(
      selectedSpeed: Option[ManufacturingSpeed],
      rules: List[PricingRule],
      context: PricingContext,
      discountedSubtotal: Money,
      lang: Language,
  ): (Option[LineItem], Money) =
    selectedSpeed match
      case None => (None, discountedSubtotal)
      case Some(speed) =>
        val speedRule = rules.collectFirst {
          case r: PricingRule.ManufacturingSpeedSurcharge if r.tier == speed => r
        }
        speedRule match
          case None => (None, discountedSubtotal)
          case Some(rule) =>
            val baseMultiplier = rule.multiplier

            // Queue-based dynamic adjustments (not applied to Economy)
            val queueAdjustment =
              if speed == ManufacturingSpeed.Economy then BigDecimal(0)
              else
                rule.queueMultiplierThresholds
                  .filter(_.minUtilisation <= context.globalUtilisation)
                  .map(_.additionalMultiplier)
                  .foldLeft(BigDecimal(0))(_ + _)

            // Busy period adjustments (not applied to Economy)
            val busyAdjustment =
              if speed == ManufacturingSpeed.Economy then BigDecimal(0)
              else
                context.busyPeriodMultipliers
                  .map(_.additionalMultiplier)
                  .foldLeft(BigDecimal(0))(_ + _)

            val rawMultiplier = baseMultiplier + queueAdjustment + busyAdjustment
            val cappedMultiplier = rawMultiplier.min(context.expressSurchargeCap)
            val effectiveMultiplier = if speed == ManufacturingSpeed.Economy then baseMultiplier else cappedMultiplier

            if effectiveMultiplier == BigDecimal(1) then
              (None, discountedSubtotal)
            else
              val adjustmentFactor = effectiveMultiplier - BigDecimal(1)
              val surchargeAmount = (discountedSubtotal * adjustmentFactor).rounded
              val afterSpeed = (discountedSubtotal * effectiveMultiplier).rounded
              val label = speed match
                case ManufacturingSpeed.Express => lang match
                  case Language.En => s"Express manufacturing: +${(adjustmentFactor * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP)}%"
                  case Language.Cs => s"Expresní výroba: +${(adjustmentFactor * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP)} %"
                case ManufacturingSpeed.Standard => lang match
                  case Language.En => s"Standard manufacturing: +${(adjustmentFactor * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP)}%"
                  case Language.Cs => s"Standardní výroba: +${(adjustmentFactor * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP)} %"
                case ManufacturingSpeed.Economy => lang match
                  case Language.En => s"Economy discount: ${(adjustmentFactor * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP)}%"
                  case Language.Cs => s"Ekonomická sleva: ${(adjustmentFactor * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP)} %"
              val lineItem = LineItem(
                label = label,
                unitPrice = surchargeAmount,
                quantity = 1,
                lineTotal = surchargeAmount,
              )
              (Some(lineItem), afterSpeed)

  private def calculateComponentBreakdown(
      comp: ProductComponent,
      specs: ProductSpecifications,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Validation[PricingError, ComponentBreakdown] =
    val effectiveQuantity = comp.sheetCount * quantity

    // Area tier rules (volume-based area pricing) for the material
    val areaTierRules = rules.collect {
      case r: PricingRule.MaterialAreaTier if r.materialId == comp.material.id => r
    }
    val areaRule = rules.collectFirst {
      case r: PricingRule.MaterialAreaPrice if r.materialId == comp.material.id => r
    }
    val sheetRule = rules.collectFirst {
      case r: PricingRule.MaterialSheetPrice if r.materialId == comp.material.id => r
    }
    val baseRule = rules.collectFirst {
      case r: PricingRule.MaterialBasePrice if r.materialId == comp.material.id => r
    }

    // If area tiers exist, try to use them; otherwise fall back to flat area price
    val effectiveAreaRule: Option[Either[PricingRule.MaterialAreaTier, PricingRule.MaterialAreaPrice]] =
      if areaTierRules.nonEmpty then
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            val unitAreaM2 = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
            val totalAreaM2 = unitAreaM2 * effectiveQuantity
            findBestAreaTier(areaTierRules, totalAreaM2).map(Left(_))
              .orElse(areaRule.map(Right(_)))
          case _ => areaRule.map(Right(_))
      else
        areaRule.map(Right(_))

    effectiveAreaRule match
      case Some(tierOrFlat) =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
            val pricePerSqM = tierOrFlat match
              case Left(tier) => tier.pricePerSqMeter
              case Right(flat) => flat.pricePerSqMeter
            val isAreaTiered = tierOrFlat.isLeft
            val unitPrice = pricePerSqM * areaSqM
            val materialLineTotal = unitPrice * effectiveQuantity
            val materialLine = LineItem(
              label = s"Material: ${comp.material.name(lang)}",
              unitPrice = unitPrice,
              quantity = effectiveQuantity,
              lineTotal = materialLineTotal,
            )
            // Skip ink config adjustment for area-tiered materials (price includes printing)
            val inkConfigLine =
              if isAreaTiered then None
              else computeInkConfigLine(comp.inkConfiguration, rules, unitPrice, materialLineTotal, effectiveQuantity)
            val finishLines = computeFinishLines(comp.finishes, rules, quantity, lang, Some(areaSqM))
            Validation.succeed(ComponentBreakdown(
              role = comp.role,
              materialLine = materialLine,
              cuttingLine = None,
              inkConfigLine = inkConfigLine,
              finishLines = finishLines,
              sheetsUsed = 0,
            ))
          case _ =>
            Validation.fail(PricingError.NoSizeForAreaPricing(comp.material.id, comp.role))

      case None =>
        sheetRule match
          case Some(sp) =>
            specs.get(SpecKind.Size) match
              case Some(SpecValue.SizeSpec(dim)) =>
                // For saddle-stitch booklets, each folded sheet has a flat (unfolded) width that
                // is twice the finished page width.  Using the finished page width would overestimate
                // piecesPerSheet by 2×, halving the reported sheets used.
                val isSaddleStitchFolded =
                  specs.get(SpecKind.BindingMethod).contains(SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch)) &&
                    (comp.role == ComponentRole.Cover || comp.role == ComponentRole.Body)
                val flatItemW = if isSaddleStitchFolded then dim.widthMm.toDouble * 2 else dim.widthMm.toDouble
                val pps = SheetNesting.piecesPerSheet(
                  sp.sheetWidthMm, sp.sheetHeightMm,
                  flatItemW, dim.heightMm.toDouble,
                  sp.bleedMm, sp.gutterMm,
                )
                val sheetsUsed = math.ceil(effectiveQuantity.toDouble / pps).toInt
                val materialLineTotal = sp.pricePerSheet * sheetsUsed
                val materialLine = LineItem(
                  label = s"Material: ${comp.material.name(lang)}",
                  unitPrice = sp.pricePerSheet,
                  quantity = sheetsUsed,
                  lineTotal = materialLineTotal,
                )

                val perPiecePrice = materialLineTotal / effectiveQuantity
                val inkConfigLine = computeInkConfigLine(comp.inkConfiguration, rules, perPiecePrice, materialLineTotal, effectiveQuantity)

                val cuttingRule = rules.collectFirst { case r: PricingRule.CuttingSurcharge => r }
                val numCuts = pps - 1
                val cuttingLine =
                  if numCuts > 0 then
                    cuttingRule.map { cr =>
                      val costPerSheet = cr.costPerCut * numCuts
                      LineItem(
                        label = "Cutting surcharge",
                        unitPrice = costPerSheet,
                        quantity = sheetsUsed,
                        lineTotal = (costPerSheet * sheetsUsed).rounded,
                      )
                    }
                  else None

                val finishLines = computeFinishLines(comp.finishes, rules, quantity, lang, None)
                Validation.succeed(ComponentBreakdown(
                  role = comp.role,
                  materialLine = materialLine,
                  cuttingLine = cuttingLine,
                  inkConfigLine = inkConfigLine,
                  finishLines = finishLines,
                  sheetsUsed = sheetsUsed,
                ))
              case _ =>
                Validation.fail(PricingError.NoSizeForSheetPricing(comp.material.id, comp.role))

          case None =>
            baseRule match
              case Some(bp) =>
                val materialLineTotal = bp.unitPrice * effectiveQuantity
                val materialLine = LineItem(
                  label = s"Material: ${comp.material.name(lang)}",
                  unitPrice = bp.unitPrice,
                  quantity = effectiveQuantity,
                  lineTotal = materialLineTotal,
                )
                val inkConfigLine = computeInkConfigLine(comp.inkConfiguration, rules, bp.unitPrice, materialLineTotal, effectiveQuantity)
                val finishLines = computeFinishLines(comp.finishes, rules, quantity, lang, None)
                Validation.succeed(ComponentBreakdown(
                  role = comp.role,
                  materialLine = materialLine,
                  cuttingLine = None,
                  inkConfigLine = inkConfigLine,
                  finishLines = finishLines,
                  sheetsUsed = 0,
                ))
              case None =>
                Validation.fail(PricingError.NoBasePriceForMaterial(comp.material.id, comp.role))

  private def extractQuantity(specs: ProductSpecifications): Validation[PricingError, Int] =
    specs.get(SpecKind.Quantity) match
      case Some(SpecValue.QuantitySpec(q)) => Validation.succeed(q.value)
      case _                              => Validation.fail(PricingError.NoQuantityInSpecifications)

  private def computeInkConfigLine(
      inkConfig: InkConfiguration,
      rules: List[PricingRule],
      materialUnitPrice: Money,
      materialLineTotal: Money,
      effectiveQuantity: Int,
  ): Option[LineItem] =
    rules.collectFirst {
      case r: PricingRule.InkConfigurationFactor
          if r.frontColorCount == inkConfig.front.colorCount && r.backColorCount == inkConfig.back.colorCount =>
        r.materialMultiplier
    }.flatMap { multiplier =>
      if multiplier == BigDecimal(1) then scala.None
      else
        val adjustmentFactor = multiplier - BigDecimal(1)
        val unitAdjustment = materialUnitPrice * adjustmentFactor
        val lineTotal = materialLineTotal * adjustmentFactor
        Some(LineItem(
          label = s"Ink configuration: ${inkConfig.notation}",
          unitPrice = unitAdjustment,
          quantity = effectiveQuantity,
          lineTotal = lineTotal,
        ))
    }

  private def collectSetupFees(
      finishes: List[SelectedFinish],
      foldType: Option[FoldType],
      bindingMethod: Option[BindingMethod],
      rules: List[PricingRule],
      lang: Language,
  ): List[LineItem] =
    val uniqueByIdFinishes = finishes.distinctBy(_.id)

    val (idItems, coveredTypes) = uniqueByIdFinishes.foldLeft((List.empty[LineItem], Set.empty[FinishType])) {
      case ((items, types), finish) =>
        rules.collectFirst {
          case r: PricingRule.FinishSetupFee if r.finishId == finish.id => r.setupCost
        } match
          case Some(cost) =>
            (items :+ LineItem(s"Setup: ${finish.name(lang)}", cost, 1, cost), types + finish.finishType)
          case None =>
            (items, types)
    }

    val typeItems = uniqueByIdFinishes
      .distinctBy(_.finishType)
      .filterNot(f => coveredTypes.contains(f.finishType))
      .flatMap { finish =>
        rules.collectFirst {
          case r: PricingRule.FinishTypeSetupFee if r.finishType == finish.finishType => r.setupCost
        }.map { cost =>
          LineItem(s"Setup: ${finish.name(lang)}", cost, 1, cost)
        }
      }

    val foldFeeItem = foldType.flatMap { ft =>
      rules.collectFirst {
        case r: PricingRule.FoldTypeSetupFee if r.foldType == ft => r.setupCost
      }.map { cost => LineItem(s"Setup: ${foldTypeName(ft, lang)}", cost, 1, cost) }
    }.toList

    val bindingFeeItem = bindingMethod.flatMap { bm =>
      rules.collectFirst {
        case r: PricingRule.BindingMethodSetupFee if r.bindingMethod == bm => r.setupCost
      }.map { cost => LineItem(s"Setup: ${bindingMethodName(bm, lang)}", cost, 1, cost) }
    }.toList

    idItems ++ typeItems ++ foldFeeItem ++ bindingFeeItem

  private def findFoldSurcharge(
      foldType: Option[FoldType],
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    foldType.flatMap { ft =>
      rules.collectFirst {
        case r: PricingRule.FoldTypeSurcharge if r.foldType == ft => r.surchargePerUnit
      }.map { surcharge =>
        LineItem(
          label = s"Fold: ${foldTypeName(ft, lang)}",
          unitPrice = surcharge,
          quantity = quantity,
          lineTotal = surcharge * quantity,
        )
      }
    }

  private def findBindingSurcharge(
      bindingMethod: Option[BindingMethod],
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    bindingMethod.flatMap { bm =>
      rules.collectFirst {
        case r: PricingRule.BindingMethodSurcharge if r.bindingMethod == bm => r.surchargePerUnit
      }.map { surcharge =>
        LineItem(
          label = s"Binding: ${bindingMethodName(bm, lang)}",
          unitPrice = surcharge,
          quantity = quantity,
          lineTotal = surcharge * quantity,
        )
      }
    }

  private def foldTypeName(ft: FoldType, lang: Language): String = ft match
    case FoldType.Half       => lang match { case Language.Cs => "Přeložení na půl";    case _ => "Half Fold" }
    case FoldType.Tri        => lang match { case Language.Cs => "Trojsložení";          case _ => "Tri Fold" }
    case FoldType.Gate       => lang match { case Language.Cs => "Okénkové složení";    case _ => "Gate Fold" }
    case FoldType.Accordion  => lang match { case Language.Cs => "Harmonikové složení"; case _ => "Accordion Fold" }
    case FoldType.ZFold      => lang match { case Language.Cs => "Z-složení";           case _ => "Z-Fold" }
    case FoldType.RollFold   => lang match { case Language.Cs => "Rolovací složení";    case _ => "Roll Fold" }
    case FoldType.FrenchFold => lang match { case Language.Cs => "Francouzské složení"; case _ => "French Fold" }
    case FoldType.CrossFold  => lang match { case Language.Cs => "Křížové složení";     case _ => "Cross Fold" }

  private def bindingMethodName(bm: BindingMethod, lang: Language): String = bm match
    case BindingMethod.SaddleStitch   => lang match { case Language.Cs => "Sešití na svorky"; case _ => "Saddle Stitch" }
    case BindingMethod.PerfectBinding => lang match { case Language.Cs => "Lepená vazba";      case _ => "Perfect Binding" }
    case BindingMethod.SpiralBinding  => lang match { case Language.Cs => "Spirálová vazba";   case _ => "Spiral Binding" }
    case BindingMethod.WireOBinding   => lang match { case Language.Cs => "Wire-O vazba";      case _ => "Wire-O Binding" }
    case BindingMethod.CaseBinding    => lang match { case Language.Cs => "Pevná vazba";       case _ => "Case Binding" }

  private def computeFinishLines(
      finishes: List[SelectedFinish],
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
      unitAreaM2: Option[BigDecimal] = None,
  ): List[LineItem] =
    finishes.flatMap { finish =>
      // --- Grommet spacing area surcharge ---
      finish.params match
        case Some(FinishParameters.GrommetParams(spacingMm)) =>
          val grommetAreaRule = rules.collect {
            case r: PricingRule.GrommetSpacingAreaSurcharge if r.spacingMm == spacingMm => r
          }.headOption
          grommetAreaRule match
            case Some(rule) =>
              unitAreaM2.map { area =>
                val unitSurcharge = (rule.pricePerSqMeter * area).rounded
                LineItem(
                  label = s"Finish: ${finish.name(lang)} (${spacingMm / 10}cm)",
                  unitPrice = unitSurcharge,
                  quantity = quantity,
                  lineTotal = unitSurcharge * quantity,
                )
              }.orElse(flatFinishSurcharge(finish, rules, quantity, lang))
            case None =>
              flatFinishSurcharge(finish, rules, quantity, lang)

        // --- Gum rope length surcharge ---
        case Some(FinishParameters.GumRopeParams(lengthMm)) =>
          val lengthRule = rules.collectFirst {
            case r: PricingRule.FinishLengthSurcharge if r.finishId == finish.id => r
          }
          lengthRule.map { rule =>
            val lengthM = BigDecimal(lengthMm) / BigDecimal(1000)
            val unitSurcharge = (rule.pricePerMeter * lengthM).rounded
            LineItem(
              label = s"Finish: ${finish.name(lang)} (${lengthM.setScale(1, BigDecimal.RoundingMode.HALF_UP)}m)",
              unitPrice = unitSurcharge,
              quantity = quantity,
              lineTotal = unitSurcharge * quantity,
            )
          }.orElse(flatFinishSurcharge(finish, rules, quantity, lang))

        // --- Standard flat surcharge ---
        case _ =>
          flatFinishSurcharge(finish, rules, quantity, lang)
    }

  private def flatFinishSurcharge(
      finish: SelectedFinish,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    val byId = rules.collectFirst {
      case r: PricingRule.FinishSurcharge if r.finishId == finish.id => r.surchargePerUnit
    }
    val byType = rules.collectFirst {
      case r: PricingRule.FinishTypeSurcharge if r.finishType == finish.finishType => r.surchargePerUnit
    }
    // ID-level takes precedence over type-level
    byId.orElse(byType).map { surcharge =>
      // Lamination (and overlamination / soft-touch coating) applied to both sides
      // costs twice as much: each side is an independent pass on press.
      val sideFactor = finish.params match
        case Some(FinishParameters.LaminationParams(FinishSide.Both)) => BigDecimal(2)
        case _                                                         => BigDecimal(1)
      val effectiveSurcharge = surcharge * sideFactor
      LineItem(
        label = s"Finish: ${finish.name(lang)}",
        unitPrice = effectiveSurcharge,
        quantity = quantity,
        lineTotal = effectiveSurcharge * quantity,
      )
    }

  private def findProcessSurcharge(
      method: PrintingMethod,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    rules.collectFirst {
      case r: PricingRule.PrintingProcessSurcharge if r.processType == method.processType =>
        LineItem(
          label = s"Process: ${method.name(lang)}",
          unitPrice = r.surchargePerUnit,
          quantity = quantity,
          lineTotal = r.surchargePerUnit * quantity,
        )
    }

  private def findCategorySurcharge(
      category: ProductCategory,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    rules.collectFirst {
      case r: PricingRule.CategorySurcharge if r.categoryId == category.id =>
        LineItem(
          label = s"Category: ${category.name(lang)}",
          unitPrice = r.surchargePerUnit,
          quantity = quantity,
          lineTotal = r.surchargePerUnit * quantity,
        )
    }

  private def findBestAreaTier(
      areaTierRules: List[PricingRule.MaterialAreaTier],
      totalAreaM2: BigDecimal,
  ): Option[PricingRule.MaterialAreaTier] =
    areaTierRules
      .filter(r => r.minAreaM2 <= totalAreaM2 && r.maxAreaM2.forall(_ >= totalAreaM2))
      .sortBy(_.minAreaM2)(using scala.math.Ordering[BigDecimal].reverse)
      .headOption

  private def findBestQuantityTier(
      rules: List[PricingRule],
      quantity: Int,
  ): Option[PricingRule.QuantityTier] =
    rules.collect {
      case r: PricingRule.QuantityTier
          if r.minQuantity <= quantity &&
            r.maxQuantity.forall(_ >= quantity) => r
    }.sortBy(_.minQuantity)(using scala.math.Ordering[Int].reverse).headOption

  private def findBestSheetQuantityTier(
      rules: List[PricingRule],
      totalSheets: Int,
  ): Option[PricingRule.SheetQuantityTier] =
    rules.collect {
      case r: PricingRule.SheetQuantityTier
          if r.minSheets <= totalSheets &&
            r.maxSheets.forall(_ >= totalSheets) => r
    }.sortBy(_.minSheets)(using scala.math.Ordering[Int].reverse).headOption

  private object SheetNesting:
    def piecesPerSheet(
        sheetW: Double,
        sheetH: Double,
        itemW: Double,
        itemH: Double,
        bleedMm: Double,
        gutterMm: Double,
    ): Int =
      val effectiveW = itemW + 2 * bleedMm
      val effectiveH = itemH + 2 * bleedMm

      def countOrientation(ew: Double, eh: Double): Int =
        val cols = math.floor((sheetW + gutterMm) / (ew + gutterMm)).toInt
        val rows = math.floor((sheetH + gutterMm) / (eh + gutterMm)).toInt
        cols * rows

      val normal = countOrientation(effectiveW, effectiveH)
      val rotated = countOrientation(effectiveH, effectiveW)
      math.max(math.max(normal, rotated), 1)
