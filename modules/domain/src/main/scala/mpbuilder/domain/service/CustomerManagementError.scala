package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Error ADT for customer management operations. */
enum CustomerManagementError:
  case DuplicateBusinessId(businessId: String)
  case DuplicateEmail(email: String)
  case CustomerNotFound(customerId: CustomerId)
  case InvalidStatus(from: CustomerStatus, to: CustomerStatus)
  case MissingRequiredField(fieldName: String)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case DuplicateBusinessId(id) => lang match
      case Language.En => s"A customer with business ID '$id' already exists"
      case Language.Cs => s"Zákazník s IČO '$id' již existuje"
    case DuplicateEmail(email) => lang match
      case Language.En => s"A customer with email '$email' already exists"
      case Language.Cs => s"Zákazník s emailem '$email' již existuje"
    case CustomerNotFound(id) => lang match
      case Language.En => s"Customer '${id.value}' not found"
      case Language.Cs => s"Zákazník '${id.value}' nebyl nalezen"
    case InvalidStatus(from, to) => lang match
      case Language.En => s"Cannot transition from ${from.displayName(Language.En)} to ${to.displayName(Language.En)}"
      case Language.Cs => s"Nelze přejít ze stavu ${from.displayName(Language.Cs)} do stavu ${to.displayName(Language.Cs)}"
    case MissingRequiredField(field) => lang match
      case Language.En => s"Required field '$field' is missing or empty"
      case Language.Cs => s"Povinné pole '$field' chybí nebo je prázdné"
