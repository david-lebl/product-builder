package mpbuilder.domain.pricing

import zio.prelude.*
import mpbuilder.domain.model.*

/** Calculates production cost (floor price) from a configuration and cost rules.
  *
  * This is intentionally simpler than `PriceCalculator`:
  *   - No tiers, no setup fees, no minimum orders
  *   - Computes direct costs for materials, processes, and finishes
  *   - Applies an overhead factor to the total
  *
  * Used for margin analysis: compare selling price to production cost.
  */
object ProductionCostCalculator:

  private val defaultLowMarginThreshold = Percentage.unsafe(BigDecimal("15"))

  /** Calculate the production cost for a configuration.
    *
    * @param config the product configuration
    * @param costSheet the production cost rules
    * @return the total production cost, or a pricing error if quantity/size is missing
    */
  def calculateCost(
      config: ProductConfiguration,
      costSheet: ProductionCostSheet,
  ): Validation[PricingError, Money] =
    extractQuantity(config.specifications).flatMap { quantity =>
      val componentCosts = config.components.map { comp =>
        calculateComponentCost(comp, config.specifications, costSheet.rules, quantity)
      }

      val componentCostsV = componentCosts.foldLeft(
        Validation.succeed(Money.zero): Validation[PricingError, Money]
      ) { (accV, costV) =>
        accV.zipWith(costV)(_ + _)
      }

      componentCostsV.map { directCost =>
        val processCost = findProcessCost(config.printingMethod, costSheet.rules, quantity)
        val totalDirectCost = directCost + processCost
        val overhead = findOverheadFactor(costSheet.rules)
        (totalDirectCost * overhead).rounded
      }
    }

  /** Analyze margin by comparing selling price to production cost.
    *
    * @param config the product configuration
    * @param pricelist the base pricelist (or customer-resolved pricelist)
    * @param costSheet the production cost rules
    * @param lowMarginThreshold optional threshold below which a low-margin warning is issued (default: 15%)
    * @return a `CostAnalysis` with margin info and warnings
    */
  def analyze(
      config: ProductConfiguration,
      pricelist: Pricelist,
      costSheet: ProductionCostSheet,
      lowMarginThreshold: Percentage = defaultLowMarginThreshold,
  ): Validation[PricingError, CostAnalysis] =
    val costV = calculateCost(config, costSheet)
    val priceV = PriceCalculator.calculate(config, pricelist)

    costV.zipWith(priceV) { (prodCost, breakdown) =>
      val sellingPrice = breakdown.total
      val margin = sellingPrice + Money(-(prodCost.value))
      val marginPct =
        if prodCost.value > BigDecimal(0) then
          val raw = (margin.value / prodCost.value * BigDecimal(100))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          Percentage.unsafe(raw.max(BigDecimal(0)))
        else
          Percentage.unsafe(BigDecimal(100))

      val isBelowCost = sellingPrice.value < prodCost.value

      val warnings: List[CostWarning] =
        val belowCostWarning =
          if isBelowCost then
            val shortfall = Money(prodCost.value - sellingPrice.value)
            List(CostWarning.BelowProductionCost(shortfall.rounded))
          else Nil

        val lowMarginWarning =
          if !isBelowCost && marginPct.value < lowMarginThreshold.value then
            List(CostWarning.LowMargin(marginPct, lowMarginThreshold))
          else Nil

        belowCostWarning ++ lowMarginWarning

      CostAnalysis(
        productionCost = prodCost,
        sellingPrice = sellingPrice,
        margin = margin.rounded,
        marginPercentage = marginPct,
        isBelowCost = isBelowCost,
        warnings = warnings,
      )
    }

  /** Analyze margin with customer-specific pricing by resolving the pricelist first. */
  def analyzeWithCustomerPricing(
      config: ProductConfiguration,
      basePricelist: Pricelist,
      customerPricing: CustomerPricing,
      costSheet: ProductionCostSheet,
      lowMarginThreshold: Percentage = defaultLowMarginThreshold,
  ): Validation[PricingError, CostAnalysis] =
    val resolvedPricelist = CustomerPricelistResolver.resolve(
      basePricelist,
      customerPricing,
      Some(config.category.id),
    )
    analyze(config, resolvedPricelist, costSheet, lowMarginThreshold)

  // --- Private helpers ---

  private def extractQuantity(specs: ProductSpecifications): Validation[PricingError, Int] =
    specs.get(SpecKind.Quantity) match
      case Some(SpecValue.QuantitySpec(q)) => Validation.succeed(q.value)
      case _                              => Validation.fail(PricingError.NoQuantityInSpecifications)

  private def calculateComponentCost(
      comp: ProductComponent,
      specs: ProductSpecifications,
      rules: List[ProductionCostRule],
      quantity: Int,
  ): Validation[PricingError, Money] =
    val effectiveQuantity = comp.sheetCount * quantity

    // Area-based materials
    val areaCost = rules.collectFirst {
      case r: ProductionCostRule.MaterialAreaCost if r.materialId == comp.material.id => r
    }

    areaCost match
      case Some(ac) =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
            val unitCost = ac.costPerM2 * areaSqM
            val materialCost = unitCost * effectiveQuantity
            val finishCost = calculateFinishCosts(comp.finishes, rules, quantity)
            Validation.succeed(materialCost + finishCost)
          case _ =>
            Validation.fail(PricingError.NoSizeForAreaPricing(comp.material.id, comp.role))
      case None =>
        // Unit-based materials
        val unitCost = rules.collectFirst {
          case r: ProductionCostRule.MaterialUnitCost if r.materialId == comp.material.id => r.cost
        }.getOrElse(Money.zero)

        val materialCost = unitCost * effectiveQuantity
        val finishCost = calculateFinishCosts(comp.finishes, rules, quantity)
        Validation.succeed(materialCost + finishCost)

  private def calculateFinishCosts(
      finishes: List[SelectedFinish],
      rules: List[ProductionCostRule],
      quantity: Int,
  ): Money =
    finishes.foldLeft(Money.zero) { (acc, finish) =>
      val finishCost = rules.collectFirst {
        case r: ProductionCostRule.FinishCost if r.finishId == finish.id => r.costPerUnit
      }.getOrElse(Money.zero)
      acc + (finishCost * quantity)
    }

  private def findProcessCost(
      printingMethod: PrintingMethod,
      rules: List[ProductionCostRule],
      quantity: Int,
  ): Money =
    rules.collectFirst {
      case r: ProductionCostRule.ProcessCost if r.processType == printingMethod.processType =>
        r.costPerUnit * quantity
    }.getOrElse(Money.zero)

  private def findOverheadFactor(rules: List[ProductionCostRule]): BigDecimal =
    rules.collectFirst {
      case r: ProductionCostRule.OverheadFactor => r.factor
    }.getOrElse(BigDecimal(1))
