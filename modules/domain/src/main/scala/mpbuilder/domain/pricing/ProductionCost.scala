package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

/** Production cost rules define the floor price below which we should not sell.
  *
  * These are separate from `PricingRule` because:
  *   - Pricing rules evolve with market strategy; cost rules evolve with supplier contracts
  *   - Different people manage them (sales vs. operations)
  *   - Cost rules are simpler (no tiers, no setup fees, no minimum orders)
  */
enum ProductionCostRule:
  /** Per-unit material cost (e.g., paper/cardboard per printed piece). */
  case MaterialUnitCost(materialId: MaterialId, cost: Money)

  /** Area-based material cost (e.g., vinyl, banner film per square meter). */
  case MaterialAreaCost(materialId: MaterialId, costPerM2: Money)

  /** Per-unit cost for the printing process (labor + machine time). */
  case ProcessCost(processType: PrintingProcessType, costPerUnit: Money)

  /** Per-unit cost for a specific finish operation. */
  case FinishCost(finishId: FinishId, costPerUnit: Money)

  /** Multiplier on total direct cost for overhead (e.g., 1.15 = 15% overhead). */
  case OverheadFactor(factor: BigDecimal)

  /** Per-sheet ink cost by ink configuration (e.g., Minolta charge per printed sheet). */
  case SheetInkCost(frontColorCount: Int, backColorCount: Int, costPerSheet: Money)

/** A collection of production cost rules, analogous to `Pricelist`. */
final case class ProductionCostSheet(
    rules: List[ProductionCostRule],
    currency: Currency,
)

/** Warning about margin issues. */
enum CostWarning:
  /** The selling price is below the production cost. */
  case BelowProductionCost(shortfall: Money)

  /** The margin is below a recommended threshold. */
  case LowMargin(marginPct: Percentage, threshold: Percentage)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case BelowProductionCost(shortfall) => lang match
      case Language.En => s"Selling price is below production cost by ${shortfall.value}"
      case Language.Cs => s"Prodejní cena je pod výrobními náklady o ${shortfall.value}"
    case LowMargin(marginPct, threshold) => lang match
      case Language.En => s"Margin ${marginPct.value}% is below recommended threshold of ${threshold.value}%"
      case Language.Cs => s"Marže ${marginPct.value}% je pod doporučeným prahem ${threshold.value}%"

/** Result of a production cost analysis comparing selling price to production cost. */
final case class CostAnalysis(
    productionCost: Money,
    sellingPrice: Money,
    margin: Money,
    marginPercentage: Percentage,
    isBelowCost: Boolean,
    warnings: List[CostWarning],
)
