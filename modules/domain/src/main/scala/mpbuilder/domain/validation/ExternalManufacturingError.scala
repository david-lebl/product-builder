package mpbuilder.domain.validation

import mpbuilder.domain.model.Language
import mpbuilder.domain.manufacturing.PartnerId
import java.time.LocalDate

/** Errors related to external manufacturing partner routing. */
enum ExternalManufacturingError:
  /** All candidate partners for the configuration are currently unavailable. */
  case PartnerUnavailable(
      partnerId: PartnerId,
      unavailableUntil: Option[LocalDate],
  )

  def message(lang: Language): String = this match
    case PartnerUnavailable(id, None) =>
      lang match
        case Language.En => s"Partner '${id.value}' is currently unavailable"
        case Language.Cs => s"Partner '${id.value}' není momentálně dostupný"
    case PartnerUnavailable(id, Some(until)) =>
      lang match
        case Language.En => s"Partner '${id.value}' is unavailable until $until"
        case Language.Cs => s"Partner '${id.value}' není dostupný do $until"
