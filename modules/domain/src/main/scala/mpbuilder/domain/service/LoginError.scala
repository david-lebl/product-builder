package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Error ADT for login operations. */
enum LoginError:
  case CustomerNotFound(identifier: String, identifierType: IdentifierType)
  case OtpExpired(customerId: CustomerId)
  case OtpInvalid(customerId: CustomerId)
  case CustomerInactive(customerId: CustomerId)
  case CustomerSuspended(customerId: CustomerId)
  case SessionExpired(sessionId: SessionId)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case CustomerNotFound(id, idType) => lang match
      case Language.En => s"No customer found with ${idType.displayName(Language.En)} '$id'"
      case Language.Cs => s"Zákazník s ${idType.displayName(Language.Cs)} '$id' nebyl nalezen"
    case OtpExpired(cid) => lang match
      case Language.En => s"The verification code has expired"
      case Language.Cs => s"Ověřovací kód vypršel"
    case OtpInvalid(cid) => lang match
      case Language.En => s"The verification code is invalid"
      case Language.Cs => s"Ověřovací kód je neplatný"
    case CustomerInactive(cid) => lang match
      case Language.En => s"Customer account '${cid.value}' is inactive"
      case Language.Cs => s"Zákaznický účet '${cid.value}' je neaktivní"
    case CustomerSuspended(cid) => lang match
      case Language.En => s"Customer account '${cid.value}' is suspended"
      case Language.Cs => s"Zákaznický účet '${cid.value}' je pozastaven"
    case SessionExpired(sid) => lang match
      case Language.En => s"Session has expired"
      case Language.Cs => s"Relace vypršela"
