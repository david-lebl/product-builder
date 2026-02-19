package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

enum PricingError:
  case NoBasePriceForMaterial(materialId: MaterialId, role: ComponentRole)
  case NoQuantityInSpecifications
  case NoSizeForAreaPricing(materialId: MaterialId, role: ComponentRole)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case NoBasePriceForMaterial(materialId, role) => lang match
      case Language.En => s"No base price found for material '${materialId.value}' in component '$role'"
      case Language.Cs => s"Nebyla nalezena základní cena pro materiál '${materialId.value}' v komponentu '$role'"
    case NoQuantityInSpecifications => lang match
      case Language.En => "Quantity specification is required for pricing"
      case Language.Cs => "Pro výpočet ceny je vyžadována specifikace množství"
    case NoSizeForAreaPricing(materialId, role) => lang match
      case Language.En => s"Area-based pricing requires size specification for material '${materialId.value}' in component '$role'"
      case Language.Cs => s"Plošná kalkulace vyžaduje specifikaci rozměrů pro materiál '${materialId.value}' v komponentu '$role'"
