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
  // Global price floor applied after setup fees
  case MinimumOrderPrice(minTotal: Money)
