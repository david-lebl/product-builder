package mpbuilder.catalog

import mpbuilder.kernel.*

/** Customer-facing manufacturing speed tier */
enum ManufacturingSpeed:
  case Express, Standard, Economy

object ManufacturingSpeed:
  extension (s: ManufacturingSpeed) def displayName(lang: Language): String = s match
    case Express  => lang match { case Language.En => "Express"; case Language.Cs => "Expres" }
    case Standard => lang match { case Language.En => "Standard"; case Language.Cs => "Standardní" }
    case Economy  => lang match { case Language.En => "Economy"; case Language.Cs => "Ekonomická" }

  extension (s: ManufacturingSpeed) def icon: String = s match
    case Express  => "⚡"
    case Standard => "●"
    case Economy  => "🐢"
