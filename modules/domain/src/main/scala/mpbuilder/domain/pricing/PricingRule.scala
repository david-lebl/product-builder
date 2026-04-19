package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

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
  // Per-unit surcharge for scoring (creasing) by crease count — overrides FinishTypeSurcharge(Scoring)
  case ScoringCountSurcharge(count: Int, surchargePerUnit: Money)
  // Manufacturing speed surcharge — applied to discounted subtotal before setup fees
  case ManufacturingSpeedSurcharge(
      tier: ManufacturingSpeed,
      multiplier: BigDecimal,
      queueMultiplierThresholds: List[QueueThreshold],
  )
  // Global price floor applied after setup fees
  case MinimumOrderPrice(minTotal: Money)
