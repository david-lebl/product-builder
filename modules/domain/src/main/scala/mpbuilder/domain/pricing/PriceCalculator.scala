package mpbuilder.domain.pricing

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.SpecValue.InkConfigSpec

object PriceCalculator:

  def calculate(
      config: ProductConfiguration,
      pricelist: Pricelist,
      lang: Language = Language.En,
  ): Validation[PricingError, PriceBreakdown] =
    if config.components.nonEmpty then
      calculateMultiComponent(config, pricelist, lang)
    else
      calculateSingleComponent(config, pricelist, lang)

  private def calculateSingleComponent(
      config: ProductConfiguration,
      pricelist: Pricelist,
      lang: Language,
  ): Validation[PricingError, PriceBreakdown] =
    val rules = pricelist.rules

    extractQuantity(config.specifications).flatMap { quantity =>
      resolveMaterialUnitPrice(config.material, config.specifications, rules).map { materialUnitPrice =>
        val materialLineTotal = materialUnitPrice * quantity
        val materialLine = LineItem(
          label = s"Material: ${config.material.name(lang)}",
          unitPrice = materialUnitPrice,
          quantity = quantity,
          lineTotal = materialLineTotal,
        )

        val inkConfigLine = computeInkConfigLine(config.specifications, rules, materialUnitPrice, materialLineTotal, quantity)

        val finishLines = computeFinishLines(config.finishes, rules, quantity, lang)

        val processSurcharge = findProcessSurcharge(config.printingMethod, rules, quantity, lang)

        val categorySurcharge = findCategorySurcharge(config.category, rules, quantity, lang)

        val allLineTotals =
          materialLine.lineTotal ::
            inkConfigLine.map(_.lineTotal).toList :::
            finishLines.map(_.lineTotal) :::
            processSurcharge.map(_.lineTotal).toList :::
            categorySurcharge.map(_.lineTotal).toList

        val subtotal = allLineTotals.foldLeft(Money.zero)(_ + _)

        val multiplier = findBestQuantityTier(rules, quantity)
          .map(_.multiplier)
          .getOrElse(BigDecimal(1))

        val total = (subtotal * multiplier).rounded

        PriceBreakdown(
          materialLine = materialLine,
          inkConfigLine = inkConfigLine,
          finishLines = finishLines,
          processSurcharge = processSurcharge,
          categorySurcharge = categorySurcharge,
          subtotal = subtotal,
          quantityMultiplier = multiplier,
          total = total,
          currency = pricelist.currency,
        )
      }
    }

  private def calculateMultiComponent(
      config: ProductConfiguration,
      pricelist: Pricelist,
      lang: Language,
  ): Validation[PricingError, PriceBreakdown] =
    val rules = pricelist.rules

    extractQuantity(config.specifications).flatMap { quantity =>
      // Resolve page count for body sheet calculation
      val pageCount = config.specifications.get(SpecKind.Pages) match
        case Some(SpecValue.PagesSpec(count)) => count
        case _ => 0

      // Price each component
      val componentResultsV: Validation[PricingError, List[ComponentLineItems]] =
        config.components
          .map { comp =>
            priceComponent(comp, rules, quantity, pageCount, lang)
          }
          .foldLeft(Validation.succeed(List.empty[ComponentLineItems]): Validation[PricingError, List[ComponentLineItems]]) {
            (accV, compV) =>
              accV.zipWith(compV)(_ :+ _)
          }

      componentResultsV.map { componentLines =>
        // Use Cover component's material line as primary (backward compat)
        val coverLines = componentLines.find(_.role == ComponentRole.Cover)
        val primaryMaterialLine = coverLines.map(_.materialLine).getOrElse(componentLines.head.materialLine)
        val primaryInkConfigLine = coverLines.flatMap(_.inkConfigLine)

        // Aggregate all finish lines from all components
        val allFinishLines = componentLines.flatMap(_.finishLines)

        // Shared surcharges
        val processSurcharge = findProcessSurcharge(config.printingMethod, rules, quantity, lang)
        val categorySurcharge = findCategorySurcharge(config.category, rules, quantity, lang)

        // Sum all component material + ink + finish line totals
        val componentTotals = componentLines.flatMap { cl =>
          cl.materialLine.lineTotal ::
            cl.inkConfigLine.map(_.lineTotal).toList :::
            cl.finishLines.map(_.lineTotal)
        }

        val allLineTotals =
          componentTotals :::
            processSurcharge.map(_.lineTotal).toList :::
            categorySurcharge.map(_.lineTotal).toList

        val subtotal = allLineTotals.foldLeft(Money.zero)(_ + _)

        val multiplier = findBestQuantityTier(rules, quantity)
          .map(_.multiplier)
          .getOrElse(BigDecimal(1))

        val total = (subtotal * multiplier).rounded

        PriceBreakdown(
          materialLine = primaryMaterialLine,
          inkConfigLine = primaryInkConfigLine,
          finishLines = allFinishLines,
          processSurcharge = processSurcharge,
          categorySurcharge = categorySurcharge,
          componentLines = componentLines,
          subtotal = subtotal,
          quantityMultiplier = multiplier,
          total = total,
          currency = pricelist.currency,
        )
      }
    }

  private def priceComponent(
      comp: ProductComponent,
      rules: List[PricingRule],
      quantity: Int,
      totalPageCount: Int,
      lang: Language,
  ): Validation[PricingError, ComponentLineItems] =
    resolveMaterialUnitPriceForComponent(comp.material, rules).map { materialUnitPrice =>
      // Body material cost scales by sheet count: (totalPages - 4) / 2 sheets
      // Cover is always 1 sheet per unit
      val sheetMultiplier = comp.role match
        case ComponentRole.Cover => 1
        case ComponentRole.Body =>
          val bodyPages = math.max(totalPageCount - 4, 0) // subtract 4 cover pages
          math.max(bodyPages / 2, 1) // at least 1 sheet

      val effectiveUnitPrice = materialUnitPrice * sheetMultiplier
      val materialLineTotal = effectiveUnitPrice * quantity
      val materialLine = LineItem(
        label = s"${comp.role} material: ${comp.material.name(lang)}",
        unitPrice = effectiveUnitPrice,
        quantity = quantity,
        lineTotal = materialLineTotal,
      )

      // Ink config line for the component
      val inkConfigLine = comp.inkConfiguration.flatMap { inkConfig =>
        rules.collectFirst {
          case r: PricingRule.InkConfigurationFactor
              if r.frontColorCount == inkConfig.front.colorCount && r.backColorCount == inkConfig.back.colorCount =>
            r.materialMultiplier
        }.flatMap { multiplier =>
          if multiplier == BigDecimal(1) then scala.None
          else
            val adjustmentFactor = multiplier - BigDecimal(1)
            val unitAdjustment = effectiveUnitPrice * adjustmentFactor
            val lineTotal = materialLineTotal * adjustmentFactor
            Some(LineItem(
              label = s"${comp.role} ink: ${inkConfig.notation}",
              unitPrice = unitAdjustment,
              quantity = quantity,
              lineTotal = lineTotal,
            ))
        }
      }

      val finishLines = computeFinishLines(comp.finishes, rules, quantity, lang)

      ComponentLineItems(
        role = comp.role,
        materialLine = materialLine,
        inkConfigLine = inkConfigLine,
        finishLines = finishLines,
      )
    }

  private def resolveMaterialUnitPriceForComponent(
      material: Material,
      rules: List[PricingRule],
  ): Validation[PricingError, Money] =
    val baseRule = rules.collectFirst {
      case r: PricingRule.MaterialBasePrice if r.materialId == material.id => r
    }
    baseRule match
      case Some(bp) => Validation.succeed(bp.unitPrice)
      case None     => Validation.fail(PricingError.NoBasePriceForMaterial(material.id))

  private def extractQuantity(specs: ProductSpecifications): Validation[PricingError, Int] =
    specs.get(SpecKind.Quantity) match
      case Some(SpecValue.QuantitySpec(q)) => Validation.succeed(q.value)
      case _                              => Validation.fail(PricingError.NoQuantityInSpecifications)

  private def resolveMaterialUnitPrice(
      material: Material,
      specs: ProductSpecifications,
      rules: List[PricingRule],
  ): Validation[PricingError, Money] =
    val areaRule = rules.collectFirst {
      case r: PricingRule.MaterialAreaPrice if r.materialId == material.id => r
    }
    val baseRule = rules.collectFirst {
      case r: PricingRule.MaterialBasePrice if r.materialId == material.id => r
    }

    areaRule match
      case Some(areaPrice) =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
            Validation.succeed(areaPrice.pricePerSqMeter * areaSqM)
          case _ =>
            Validation.fail(PricingError.NoSizeForAreaPricing(material.id))
      case None =>
        baseRule match
          case Some(bp) => Validation.succeed(bp.unitPrice)
          case None     => Validation.fail(PricingError.NoBasePriceForMaterial(material.id))

  private def computeInkConfigLine(
      specs: ProductSpecifications,
      rules: List[PricingRule],
      materialUnitPrice: Money,
      materialLineTotal: Money,
      quantity: Int,
  ): Option[LineItem] =
    specs.get(SpecKind.InkConfig) match
      case Some(InkConfigSpec(config)) =>
        rules.collectFirst {
          case r: PricingRule.InkConfigurationFactor
              if r.frontColorCount == config.front.colorCount && r.backColorCount == config.back.colorCount =>
            r.materialMultiplier
        }.flatMap { multiplier =>
          if multiplier == BigDecimal(1) then scala.None
          else
            val adjustmentFactor = multiplier - BigDecimal(1)
            val unitAdjustment = materialUnitPrice * adjustmentFactor
            val lineTotal = materialLineTotal * adjustmentFactor
            Some(LineItem(
              label = s"Ink configuration: ${config.notation}",
              unitPrice = unitAdjustment,
              quantity = quantity,
              lineTotal = lineTotal,
            ))
        }
      case _ => scala.None

  private def computeFinishLines(
      finishes: List[Finish],
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): List[LineItem] =
    finishes.flatMap { finish =>
      val byId = rules.collectFirst {
        case r: PricingRule.FinishSurcharge if r.finishId == finish.id => r.surchargePerUnit
      }
      val byType = rules.collectFirst {
        case r: PricingRule.FinishTypeSurcharge if r.finishType == finish.finishType => r.surchargePerUnit
      }
      // ID-level takes precedence over type-level
      byId.orElse(byType).map { surcharge =>
        LineItem(
          label = s"Finish: ${finish.name(lang)}",
          unitPrice = surcharge,
          quantity = quantity,
          lineTotal = surcharge * quantity,
        )
      }
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

  private def findBestQuantityTier(
      rules: List[PricingRule],
      quantity: Int,
  ): Option[PricingRule.QuantityTier] =
    rules.collect {
      case r: PricingRule.QuantityTier
          if r.minQuantity <= quantity &&
            r.maxQuantity.forall(_ >= quantity) => r
    }.sortBy(_.minQuantity)(using scala.math.Ordering[Int].reverse).headOption
