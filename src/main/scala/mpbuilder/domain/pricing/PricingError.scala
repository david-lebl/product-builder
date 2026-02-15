package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

enum PricingError:
  case NoBasePriceForMaterial(materialId: MaterialId)
  case NoQuantityInSpecifications
  case NoSizeForAreaPricing(materialId: MaterialId)

  def message: String = this match
    case NoBasePriceForMaterial(materialId) =>
      s"No base price found for material '${materialId.value}'"
    case NoQuantityInSpecifications =>
      "Quantity specification is required for pricing"
    case NoSizeForAreaPricing(materialId) =>
      s"Area-based pricing requires size specification for material '${materialId.value}'"
