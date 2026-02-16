package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

enum PricingError:
  case NoBasePriceForMaterial(materialId: MaterialId)
  case NoQuantityInSpecifications
  case NoSizeForAreaPricing(materialId: MaterialId)
  case InvalidQuantity(quantity: Int)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case NoBasePriceForMaterial(materialId) => lang match
      case Language.En => s"No base price found for material '${materialId.value}'"
      case Language.Cs => s"Nebyla nalezena základní cena pro materiál '${materialId.value}'"
    case NoQuantityInSpecifications => lang match
      case Language.En => "Quantity specification is required for pricing"
      case Language.Cs => "Pro výpočet ceny je vyžadována specifikace množství"
    case NoSizeForAreaPricing(materialId) => lang match
      case Language.En => s"Area-based pricing requires size specification for material '${materialId.value}'"
      case Language.Cs => s"Plošná kalkulace vyžaduje specifikaci rozměrů pro materiál '${materialId.value}'"
    case InvalidQuantity(quantity) => lang match
      case Language.En => s"Invalid quantity: $quantity. Quantity must be positive."
      case Language.Cs => s"Neplatné množství: $quantity. Množství musí být kladné."
