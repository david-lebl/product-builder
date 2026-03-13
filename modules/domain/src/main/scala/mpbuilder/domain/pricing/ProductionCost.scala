package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

/** Production cost rules model the internal cost of producing a product.
  * These are separate from selling prices (`PricingRule`) and represent
  * supplier/material costs, process costs, and overhead.
  */
enum ProductionCostRule:
  /** Per-unit material cost (for base-priced materials like paper/cardboard). */
  case MaterialUnitCost(materialId: MaterialId, cost: Money)

  /** Area-based material cost (for large-format materials like vinyl, per sqm). */
  case MaterialAreaCost(materialId: MaterialId, costPerSqMeter: Money)

  /** Per-unit process cost (e.g., offset printing, digital printing). */
  case ProcessCost(processType: PrintingProcessType, costPerUnit: Money)

  /** Per-unit finish cost (e.g., lamination, embossing). */
  case FinishCost(finishId: FinishId, costPerUnit: Money)

  /** Overhead multiplier applied to total production cost (e.g., 1.15 = 15% overhead). */
  case OverheadFactor(factor: BigDecimal)

/** A collection of production cost rules, analogous to `Pricelist` for selling prices. */
final case class ProductionCostSheet(
    rules: List[ProductionCostRule],
    currency: Currency,
)

/** Result of comparing production cost to selling price. */
final case class CostAnalysis(
    productionCost: Money,
    sellingPrice: Money,
    margin: Money,
    marginPercentage: Percentage,
    isBelowCost: Boolean,
    warnings: List[CostWarning],
)

/** Warnings emitted when selling price is too close to or below production cost. */
enum CostWarning:
  case BelowProductionCost(shortfall: Money)
  case LowMargin(marginPct: Percentage, threshold: Percentage)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case BelowProductionCost(shortfall) => lang match
      case Language.En => s"Selling price is below production cost by ${shortfall.value}"
      case Language.Cs => s"Prodejní cena je pod výrobními náklady o ${shortfall.value}"
    case LowMargin(marginPct, threshold) => lang match
      case Language.En => s"Margin ${marginPct.value}% is below threshold ${threshold.value}%"
      case Language.Cs => s"Marže ${marginPct.value}% je pod prahem ${threshold.value}%"
