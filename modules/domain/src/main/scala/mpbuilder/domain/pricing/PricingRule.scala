package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

/** Area price tier for materials: if both [[MaterialAreaTier]] and [[MaterialAreaPrice]] exist for the same material,
  * [[MaterialAreaTier]] wins. The tier with the largest `minSqm` that is ≤ the component area is selected.
  */
final case class AreaTier(minSqm: BigDecimal, pricePerSqMeter: Money)

/** Spacing-based grommet tier: price per m² for a given grommet spacing. */
final case class GrommetSpacingTier(spacingMm: Int, pricePerSqMeter: Money)

enum PricingRule:
  case MaterialBasePrice(materialId: MaterialId, unitPrice: Money)
  case MaterialAreaPrice(materialId: MaterialId, pricePerSqMeter: Money)
  case MaterialSheetPrice(
     materialId: MaterialId,
     pricePerSheet: Money,
     sheetWidthMm: Double,
     sheetHeightMm: Double,
     bleedMm: Double,
     gutterMm: Double,
   )
  case FinishSurcharge(finishId: FinishId, surchargePerUnit: Money)
  case FinishTypeSurcharge(finishType: FinishType, surchargePerUnit: Money)
  case PrintingProcessSurcharge(processType: PrintingProcessType, surchargePerUnit: Money)
  case CategorySurcharge(categoryId: CategoryId, surchargePerUnit: Money)
  case QuantityTier(minQuantity: Int, maxQuantity: Option[Int], multiplier: BigDecimal)
  case SheetQuantityTier(minSheets: Int, maxSheets: Option[Int], multiplier: BigDecimal)
  case InkConfigurationFactor(frontColorCount: Int, backColorCount: Int, materialMultiplier: BigDecimal)
  case CuttingSurcharge(costPerCut: Money)
  // One-time machine setup cost; added after the volume-discount multiplier
  case FinishTypeSetupFee(finishType: FinishType, setupCost: Money)
  case FinishSetupFee(finishId: FinishId, setupCost: Money)
  // Per-unit surcharges for fold type and binding method (discountable, go into subtotal)
  case FoldTypeSurcharge(foldType: FoldType, surchargePerUnit: Money)
  case BindingMethodSurcharge(bindingMethod: BindingMethod, surchargePerUnit: Money)
  // One-time setup fees for fold type and binding method (added after discount)
  case FoldTypeSetupFee(foldType: FoldType, setupCost: Money)
  case BindingMethodSetupFee(bindingMethod: BindingMethod, setupCost: Money)
  // Manufacturing speed surcharge — applied to discounted subtotal before setup fees
  case ManufacturingSpeedSurcharge(
      tier: ManufacturingSpeed,
      multiplier: BigDecimal,
      queueMultiplierThresholds: List[QueueThreshold],
  )
  // Global price floor applied after setup fees
  case MinimumOrderPrice(minTotal: Money)
  // Area-tiered material price: selects the tier with the largest minSqm ≤ area; wins over MaterialAreaPrice
  case MaterialAreaTier(materialId: MaterialId, tiers: List[AreaTier])
  // Grommet pricing driven by spacing: area-based surcharge keyed by spacingMm
  case GrommetSpacingAreaPrice(finishId: FinishId, tiers: List[GrommetSpacingTier])
  // Linear-meter pricing for rope/accessory finishes
  case FinishLinearMeterPrice(finishId: FinishId, pricePerMeter: Money)
  // Per-piece scoring surcharge keyed on crease count (discountable, applied before quantity multiplier)
  case ScoringCountSurcharge(creaseCount: Int, surchargePerUnit: Money)
  // One-time flat setup fee for creasing/scoring (not discounted; takes precedence over FinishTypeSetupFee for Scoring)
  case ScoringSetupFee(setupCost: Money)
  // Material-based pricing for binding components (coils/wires by linear meter, case-binding boards by unit)
  case MaterialLinearPrice(materialId: MaterialId, pricePerMeter: Money)
  case MaterialFixedPrice(materialId: MaterialId, pricePerUnit: Money)
