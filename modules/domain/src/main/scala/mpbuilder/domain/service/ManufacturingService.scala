package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*

object ManufacturingService:

  /** Create a new manufacturing order from a single basket item.
   *  Small-format orders start as Queued; large-format orders start as PendingApproval.
   */
  def placeOrderFromItem(
      item: BasketItem,
      orderId: OrderId,
      createdAt: Long,
  ): ManufacturingOrder =
    val formatType    = PrintFormatType.fromProcessType(item.configuration.printingMethod.processType)
    val initialStatus = formatType match
      case PrintFormatType.SmallFormat => ManufacturingStatus.Queued
      case PrintFormatType.LargeFormat => ManufacturingStatus.PendingApproval
    ManufacturingOrder(
      id         = orderId,
      item       = item,
      formatType = formatType,
      status     = initialStatus,
      createdAt  = createdAt,
    )

  /** Approve a large-format order that is PendingApproval — moves it to Queued. */
  def approve(
      order: ManufacturingOrder,
  ): Validation[ManufacturingError, ManufacturingOrder] =
    order.status match
      case ManufacturingStatus.PendingApproval =>
        Validation.succeed(order.copy(status = ManufacturingStatus.Queued))
      case ManufacturingStatus.Completed | ManufacturingStatus.Cancelled | ManufacturingStatus.Rejected =>
        Validation.fail(ManufacturingError.OrderAlreadyTerminal(order.id, order.status))
      case _ =>
        Validation.fail(ManufacturingError.ApprovalNotRequired(order.id))

  /** Reject a large-format order that is PendingApproval — moves it to Rejected. */
  def reject(
      order: ManufacturingOrder,
      reason: Option[String] = None,
  ): Validation[ManufacturingError, ManufacturingOrder] =
    order.status match
      case ManufacturingStatus.PendingApproval =>
        Validation.succeed(order.copy(status = ManufacturingStatus.Rejected, notes = reason))
      case ManufacturingStatus.Completed | ManufacturingStatus.Cancelled | ManufacturingStatus.Rejected =>
        Validation.fail(ManufacturingError.OrderAlreadyTerminal(order.id, order.status))
      case _ =>
        Validation.fail(ManufacturingError.ApprovalNotRequired(order.id))

  /** Advance an order to the next production state:
   *  Queued → InProduction → QualityCheck → Packaging → ReadyForPickup → Completed
   */
  def advance(
      order: ManufacturingOrder,
  ): Validation[ManufacturingError, ManufacturingOrder] =
    order.status match
      case ManufacturingStatus.Queued         => Validation.succeed(order.copy(status = ManufacturingStatus.InProduction))
      case ManufacturingStatus.InProduction   => Validation.succeed(order.copy(status = ManufacturingStatus.QualityCheck))
      case ManufacturingStatus.QualityCheck   => Validation.succeed(order.copy(status = ManufacturingStatus.Packaging))
      case ManufacturingStatus.Packaging      => Validation.succeed(order.copy(status = ManufacturingStatus.ReadyForPickup))
      case ManufacturingStatus.ReadyForPickup => Validation.succeed(order.copy(status = ManufacturingStatus.Completed))
      case ManufacturingStatus.PendingApproval =>
        Validation.fail(ManufacturingError.InvalidStatusTransition(order.status, ManufacturingStatus.Queued))
      case ManufacturingStatus.Completed | ManufacturingStatus.Cancelled | ManufacturingStatus.Rejected =>
        Validation.fail(ManufacturingError.OrderAlreadyTerminal(order.id, order.status))

  /** Cancel any non-terminal order. */
  def cancel(
      order: ManufacturingOrder,
  ): Validation[ManufacturingError, ManufacturingOrder] =
    order.status match
      case ManufacturingStatus.Completed | ManufacturingStatus.Cancelled | ManufacturingStatus.Rejected =>
        Validation.fail(ManufacturingError.OrderAlreadyTerminal(order.id, order.status))
      case _ =>
        Validation.succeed(order.copy(status = ManufacturingStatus.Cancelled))

  /** Human-readable label for the next production step, used in action buttons. */
  def nextStepLabel(status: ManufacturingStatus, lang: Language): Option[String] =
    status match
      case ManufacturingStatus.Queued         => Some(lang match
        case Language.En => "Start Production"
        case Language.Cs => "Zahájit výrobu"
      )
      case ManufacturingStatus.InProduction   => Some(lang match
        case Language.En => "Send to Quality Check"
        case Language.Cs => "Poslat na kontrolu kvality"
      )
      case ManufacturingStatus.QualityCheck   => Some(lang match
        case Language.En => "Start Packaging"
        case Language.Cs => "Zahájit balení"
      )
      case ManufacturingStatus.Packaging      => Some(lang match
        case Language.En => "Ready for Pickup"
        case Language.Cs => "Připraveno k vyzvednutí"
      )
      case ManufacturingStatus.ReadyForPickup => Some(lang match
        case Language.En => "Mark Completed"
        case Language.Cs => "Označit jako dokončeno"
      )
      case ManufacturingStatus.PendingApproval | ManufacturingStatus.Completed |
           ManufacturingStatus.Cancelled      | ManufacturingStatus.Rejected => None
