package mpbuilder.domain.pricing

import zio.prelude.*
import mpbuilder.domain.model.*

object PriceCalculator:

  def calculate(
      config: ProductConfiguration,
      pricelist: Pricelist,
      lang: Language = Language.En,
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

        val finishLines = computeFinishLines(config.finishes, rules, quantity, lang)

        val processSurcharge = findProcessSurcharge(config.printingMethod, rules, quantity, lang)

        val categorySurcharge = findCategorySurcharge(config.category, rules, quantity, lang)

        val doubleSidedSurcharge = findDoubleSidedSurcharge(config.specifications, rules, quantity, lang)

        val allLineTotals =
          materialLine.lineTotal ::
            finishLines.map(_.lineTotal) :::
            processSurcharge.map(_.lineTotal).toList :::
            categorySurcharge.map(_.lineTotal).toList :::
            doubleSidedSurcharge.map(_.lineTotal).toList

        val subtotal = allLineTotals.foldLeft(Money.zero)(_ + _)

        val multiplier = findBestQuantityTier(rules, quantity)
          .map(_.multiplier)
          .getOrElse(BigDecimal(1))

        val total = (subtotal * multiplier).rounded

        PriceBreakdown(
          materialLine = materialLine,
          finishLines = finishLines,
          processSurcharge = processSurcharge,
          categorySurcharge = categorySurcharge,
          doubleSidedSurcharge = doubleSidedSurcharge,
          subtotal = subtotal,
          quantityMultiplier = multiplier,
          total = total,
          currency = pricelist.currency,
        )
      }
    }

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

  private def findDoubleSidedSurcharge(
      specs: ProductSpecifications,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    specs.get(SpecKind.InkConfiguration) match
      case Some(SpecValue.InkConfigurationSpec(ic)) if ic.isDoubleSided =>
        rules.collectFirst {
          case r: PricingRule.DoubleSidedPrintSurcharge =>
            LineItem(
              label = lang match
                case Language.En => s"Double-sided printing (${ic.label})"
                case Language.Cs => s"Oboustranný tisk (${ic.label})",
              unitPrice = r.surchargePerUnit,
              quantity = quantity,
              lineTotal = r.surchargePerUnit * quantity,
            )
        }
      case _ => None
