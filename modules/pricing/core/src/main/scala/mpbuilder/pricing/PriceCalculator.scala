package mpbuilder.pricing

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import zio.prelude.*

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
          .map { comp => ComponentPricing.calculateComponentBreakdown(comp, config.specifications, rules, quantity, lang, config.printingMethod.id) }
          .foldLeft(Validation.succeed(List.empty[ComponentBreakdown]): Validation[PricingError, List[ComponentBreakdown]]) {
            (accV, cbV) => accV.zipWith(cbV)(_ :+ _)
          }

      componentBreakdownsV.map { componentBreakdowns =>
        val processSurcharge = Surcharges.findProcessSurcharge(config.printingMethod, rules, quantity, lang)
        val categorySurcharge = Surcharges.findCategorySurcharge(config.category, rules, quantity, lang)

        val foldType = config.specifications.get(SpecKind.FoldType).collect {
          case SpecValue.FoldTypeSpec(ft) => ft
        }
        val bindingMethod = config.specifications.get(SpecKind.BindingMethod).collect {
          case SpecValue.BindingMethodSpec(bm) => bm
        }
        val foldSurcharge = Surcharges.findFoldSurcharge(foldType, rules, quantity, lang)
        val bindingSurcharge = Surcharges.findBindingSurcharge(bindingMethod, rules, quantity, lang)

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
          if totalSheets > 0 then Surcharges.findBestSheetQuantityTier(rules, totalSheets).map(_.multiplier)
          else None

        val multiplier = sheetTierMultiplier
          .orElse(Surcharges.findBestQuantityTier(rules, quantity).map(_.multiplier))
          .getOrElse(BigDecimal(1))

        val discountedSubtotal = (subtotal * multiplier).rounded

        // Manufacturing speed surcharge — applied after quantity discount, before setup fees
        val selectedSpeed = config.specifications.get(SpecKind.ManufacturingSpeed).collect {
          case SpecValue.ManufacturingSpeedSpec(speed) => speed
        }
        val (speedSurcharge, afterSpeedSubtotal) =
          computeSpeedSurcharge(selectedSpeed, rules, context, discountedSubtotal, lang)

        val allSelectedFinishes = config.components.flatMap(_.finishes)
        val setupFees = SetupFees.collectSetupFees(allSelectedFinishes, foldType, bindingMethod, config.printingMethod, rules, lang)
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

  private def extractQuantity(specs: ProductSpecifications): Validation[PricingError, Int] =
    specs.get(SpecKind.Quantity) match
      case Some(SpecValue.QuantitySpec(q)) => Validation.succeed(q.value)
      case _                              => Validation.fail(PricingError.NoQuantityInSpecifications)
