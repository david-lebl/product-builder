package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

enum PricingRule:
  case MaterialBasePrice(materialId: MaterialId, unitPrice: Money)
  case MaterialAreaPrice(materialId: MaterialId, pricePerSqMeter: Money)
  case FinishSurcharge(finishId: FinishId, surchargePerUnit: Money)
  case FinishTypeSurcharge(finishType: FinishType, surchargePerUnit: Money)
  case PrintingProcessSurcharge(processType: PrintingProcessType, surchargePerUnit: Money)
  case CategorySurcharge(categoryId: CategoryId, surchargePerUnit: Money)
  case QuantityTier(minQuantity: Int, maxQuantity: Option[Int], multiplier: BigDecimal)
  case InkConfigurationFactor(frontColorCount: Int, backColorCount: Int, materialMultiplier: BigDecimal)
  case MaterialSheetPrice(
      materialId: MaterialId,
      pricePerSheet: Money,
      sheetWidthMm: Double,
      sheetHeightMm: Double,
      bleedMm: Double,
      gutterMm: Double,
      minUnitPrice: Money,
  )
  case CuttingSurcharge(costPerCut: Money)
