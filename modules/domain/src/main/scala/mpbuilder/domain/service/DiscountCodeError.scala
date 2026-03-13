package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.Money

/** Error ADT for discount code operations. */
enum DiscountCodeError:
  case CodeNotFound(code: String)
  case CodeExpired(code: String)
  case CodeNotYetValid(code: String)
  case CodeExhausted(code: String)
  case CodeInactive(code: String)
  case BelowMinimumOrder(code: String, minimum: Money, actual: Money)
  case CategoryNotEligible(code: String, categoryIds: Set[CategoryId])
  case CustomerNotEligible(code: String)
  case DuplicateCode(code: String)
  case CodeIdNotFound(id: DiscountCodeId)
  case InvalidDiscountValue(detail: String)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case CodeNotFound(c) => lang match
      case Language.En => s"Discount code '$c' not found"
      case Language.Cs => s"Slevový kód '$c' nebyl nalezen"
    case CodeExpired(c) => lang match
      case Language.En => s"Discount code '$c' has expired"
      case Language.Cs => s"Slevový kód '$c' vypršel"
    case CodeNotYetValid(c) => lang match
      case Language.En => s"Discount code '$c' is not yet valid"
      case Language.Cs => s"Slevový kód '$c' ještě není platný"
    case CodeExhausted(c) => lang match
      case Language.En => s"Discount code '$c' has reached its maximum number of uses"
      case Language.Cs => s"Slevový kód '$c' dosáhl maximálního počtu použití"
    case CodeInactive(c) => lang match
      case Language.En => s"Discount code '$c' is not active"
      case Language.Cs => s"Slevový kód '$c' není aktivní"
    case BelowMinimumOrder(c, min, actual) => lang match
      case Language.En => s"Discount code '$c' requires a minimum order of ${min.value}, current total is ${actual.value}"
      case Language.Cs => s"Slevový kód '$c' vyžaduje minimální objednávku ${min.value}, aktuální součet je ${actual.value}"
    case CategoryNotEligible(c, _) => lang match
      case Language.En => s"Discount code '$c' is not valid for the selected product categories"
      case Language.Cs => s"Slevový kód '$c' není platný pro vybrané kategorie produktů"
    case CustomerNotEligible(c) => lang match
      case Language.En => s"Discount code '$c' is not available for your account"
      case Language.Cs => s"Slevový kód '$c' není dostupný pro váš účet"
    case DuplicateCode(c) => lang match
      case Language.En => s"A discount code '$c' already exists"
      case Language.Cs => s"Slevový kód '$c' již existuje"
    case CodeIdNotFound(id) => lang match
      case Language.En => s"Discount code with ID '${id.value}' not found"
      case Language.Cs => s"Slevový kód s ID '${id.value}' nebyl nalezen"
    case InvalidDiscountValue(detail) => lang match
      case Language.En => s"Invalid discount value: $detail"
      case Language.Cs => s"Neplatná hodnota slevy: $detail"
