package mpbuilder.manufacturing

import mpbuilder.kernel.*
import mpbuilder.ordering.*

/** Status of an order in the approval queue */
enum ApprovalStatus:
  case Placed
  case Approved
  case Rejected
  case PendingChanges
  case OnHold

/** Payment verification status */
enum PaymentStatus:
  case Pending, Confirmed, Failed

object PaymentStatus:
  extension (ps: PaymentStatus) def displayName: String = ps match
    case Pending   => "Pending"
    case Confirmed => "Confirmed"
    case Failed    => "Failed"

  extension (ps: PaymentStatus) def icon: String = ps match
    case Pending   => "⏳"
    case Confirmed => "✅"
    case Failed    => "❌"

/** A manufacturing order combining order info with workflows */
final case class ManufacturingOrder(
    order: Order,
    workflows: List[ManufacturingWorkflow],
    approvalStatus: ApprovalStatus,
    approvalNotes: String,
    createdAt: Long,
    deadline: Option[Long],
    priority: Priority = Priority.Normal,
    paymentStatus: PaymentStatus = PaymentStatus.Pending,
    artworkCheck: ArtworkCheck = ArtworkCheck.unchecked,
    fulfilment: Option[FulfilmentChecklist] = None,
)

object ManufacturingOrder:
  extension (mo: ManufacturingOrder)
    def overallStatus: WorkflowStatus =
      if mo.workflows.exists(_.status == WorkflowStatus.OnHold) then WorkflowStatus.OnHold
      else if mo.workflows.forall(_.status == WorkflowStatus.Completed) then WorkflowStatus.Completed
      else if mo.workflows.exists(w =>
        w.status == WorkflowStatus.InProgress || w.status == WorkflowStatus.Completed
      ) then WorkflowStatus.InProgress
      else if mo.workflows.forall(_.status == WorkflowStatus.Cancelled) then WorkflowStatus.Cancelled
      else WorkflowStatus.Pending

    def overallCompletionRatio: Double =
      if mo.workflows.isEmpty then 1.0
      else mo.workflows.map(_.completionRatio).sum / mo.workflows.size

    def totalSteps: Int = mo.workflows.flatMap(_.steps).size

    def completedStepCount: Int =
      mo.workflows.flatMap(_.steps).count(s =>
        s.status == StepStatus.Completed || s.status == StepStatus.Skipped
      )

    def customerName: String =
      val info = mo.order.checkoutInfo.contactInfo
      s"${info.firstName} ${info.lastName}".trim match
        case "" => "(Guest)"
        case n  => n

    def itemSummary: String =
      val items = mo.order.basket.items
      if items.isEmpty then "No items"
      else if items.size == 1 then
        s"${items.head.quantity}× ${items.head.configuration.category.name(Language.En)}"
      else
        val first = s"${items.head.quantity}× ${items.head.configuration.category.name(Language.En)}"
        s"${items.size} items: $first…"

    def isReadyForDispatch: Boolean =
      mo.workflows.nonEmpty && mo.workflows.forall(_.status == WorkflowStatus.Completed)

    def isDispatched: Boolean =
      mo.fulfilment.exists(_.isDispatched)
