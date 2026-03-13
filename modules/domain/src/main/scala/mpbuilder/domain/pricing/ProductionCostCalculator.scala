package mpbuilder.domain.pricing

import zio.prelude.*
import mpbuilder.domain.model.*

/** Calculates production costs from a `ProductConfiguration` and `ProductionCostSheet`,
  * and performs margin analysis by comparing production costs to selling prices.
  *
  * The calculator mirrors the structure of `PriceCalculator` — it walks through
  * components, resolves material costs, adds process and finish costs, and applies
  * the overhead factor.
  */
object ProductionCostCalculator:

  /** Default low-margin warning threshold (20%). */
  val defaultLowMarginThreshold: Percentage = Percentage.unsafe(BigDecimal(20))

  /** Calculate the total production cost for a configuration.
    *
    * @param config the product configuration
    * @param costSheet production cost rules
    * @return the total production cost, or validation errors
    */
  def calculateCost(
      config: ProductConfiguration,
      costSheet: ProductionCostSheet,
  ): Validation[PricingError, Money] =
    val rules = costSheet.rules

    extractQuantity(config.specifications).map { quantity =>
      val componentCosts = config.components.map { comp =>
        calculateComponentCost(comp, config.specifications, rules, quantity)
      }

      val totalComponentCost = componentCosts.foldLeft(Money.zero)(_ + _)

      val processCost = findProcessCost(config.printingMethod, rules, quantity)

      val rawCost = totalComponentCost + processCost

      val overheadFactor = rules.collectFirst {
        case ProductionCostRule.OverheadFactor(factor) => factor
      }.getOrElse(BigDecimal(1))

      (rawCost * overheadFactor).rounded
    }

  /** Analyze the margin between selling price and production cost.
    *
    * Computes the selling price using `PriceCalculator` with optionally customer-adjusted
    * pricelist, calculates production cost, and compares them.
    *
    * @param config the product configuration
    * @param pricelist the base pricelist (or customer-adjusted pricelist)
    * @param costSheet production cost rules
    * @param lowMarginThreshold threshold below which a low-margin warning is emitted (default 20%)
    * @return a `CostAnalysis` with margin details and warnings
    */
  def analyze(
      config: ProductConfiguration,
      pricelist: Pricelist,
      costSheet: ProductionCostSheet,
      lowMarginThreshold: Percentage = defaultLowMarginThreshold,
  ): Validation[PricingError, CostAnalysis] =
    val costV = calculateCost(config, costSheet)
    val priceV = PriceCalculator.calculate(config, pricelist)

    costV.zipWith(priceV) { (productionCost, breakdown) =>
      val sellingPrice = breakdown.total
      val margin = sellingPrice + Money(-(productionCost.value))
      val isBelowCost = margin.value < BigDecimal(0)

      val marginPct =
        if sellingPrice.value > BigDecimal(0) then
          Percentage.unsafe(((margin.value / sellingPrice.value) * BigDecimal(100)).setScale(2, BigDecimal.RoundingMode.HALF_UP))
        else
          Percentage.zero

      val warnings = List.newBuilder[CostWarning]

      if isBelowCost then
        val shortfall = Money(-(margin.value))
        warnings += CostWarning.BelowProductionCost(shortfall)

      if !isBelowCost && marginPct.value < lowMarginThreshold.value && marginPct.value >= BigDecimal(0) then
        warnings += CostWarning.LowMargin(marginPct, lowMarginThreshold)

      CostAnalysis(
        productionCost = productionCost,
        sellingPrice = sellingPrice,
        margin = margin,
        marginPercentage = marginPct,
        isBelowCost = isBelowCost,
        warnings = warnings.result(),
      )
    }

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
  ): Money =
    val effectiveQuantity = comp.sheetCount * quantity

    val materialCost = findMaterialCost(comp.material.id, specs, rules, effectiveQuantity)
    val finishCost = computeFinishCosts(comp.finishes, rules, quantity)

    materialCost + finishCost

  private def findMaterialCost(
      materialId: MaterialId,
      specs: ProductSpecifications,
      rules: List[ProductionCostRule],
      effectiveQuantity: Int,
  ): Money =
    // Area-based cost takes precedence (mirrors PriceCalculator logic)
    val areaCost = rules.collectFirst {
      case ProductionCostRule.MaterialAreaCost(mid, costPerSqM) if mid == materialId =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
            val unitCost = costPerSqM * areaSqM
            unitCost * effectiveQuantity
          case _ => Money.zero
    }

    areaCost.getOrElse {
      // Fall back to unit cost
      val unitCost = rules.collectFirst {
        case ProductionCostRule.MaterialUnitCost(mid, cost) if mid == materialId => cost
      }
      unitCost.map(_ * effectiveQuantity).getOrElse(Money.zero)
    }

  private def computeFinishCosts(
      finishes: List[SelectedFinish],
      rules: List[ProductionCostRule],
      quantity: Int,
  ): Money =
    finishes.foldLeft(Money.zero) { (acc, finish) =>
      val finishCost = rules.collectFirst {
        case ProductionCostRule.FinishCost(fid, costPerUnit) if fid == finish.id =>
          val sideFactor = finish.params match
            case Some(FinishParameters.LaminationParams(FinishSide.Both)) => BigDecimal(2)
            case _                                                         => BigDecimal(1)
          (costPerUnit * sideFactor) * quantity
      }.getOrElse(Money.zero)
      acc + finishCost
    }

  private def findProcessCost(
      method: PrintingMethod,
      rules: List[ProductionCostRule],
      quantity: Int,
  ): Money =
    rules.collectFirst {
      case ProductionCostRule.ProcessCost(pt, costPerUnit) if pt == method.processType =>
        costPerUnit * quantity
    }.getOrElse(Money.zero)
