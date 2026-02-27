package mpbuilder.domain.service

import mpbuilder.domain.model.*

enum ManufacturingError:
  case OrderNotFound(orderId: OrderId)
  case InvalidStatusTransition(from: ManufacturingStatus, to: ManufacturingStatus)
  case ApprovalNotRequired(orderId: OrderId)
  case OrderAlreadyTerminal(orderId: OrderId, status: ManufacturingStatus)

  def message(lang: Language): String = this match
    case OrderNotFound(orderId) => lang match
      case Language.En => s"Order not found: ${orderId.value}"
      case Language.Cs => s"Objednávka nenalezena: ${orderId.value}"
    case InvalidStatusTransition(from, to) => lang match
      case Language.En => s"Cannot transition from ${from.label(lang)} to ${to.label(lang)}"
      case Language.Cs => s"Nelze přejít ze stavu ${from.label(lang)} do stavu ${to.label(lang)}"
    case ApprovalNotRequired(orderId) => lang match
      case Language.En => s"Order ${orderId.value} does not require approval"
      case Language.Cs => s"Objednávka ${orderId.value} nevyžaduje schválení"
    case OrderAlreadyTerminal(orderId, status) => lang match
      case Language.En => s"Order ${orderId.value} is already in terminal state: ${status.label(lang)}"
      case Language.Cs => s"Objednávka ${orderId.value} je již v konečném stavu: ${status.label(lang)}"
