package mpbuilder.manufacturing

import mpbuilder.commons.*

/** An order to be made, in manufacturing's own words.
  *
  * Note what is absent: any type belonging to order-intake. This context is told what to make by
  * an event carrying a snapshot, and builds this from it. Today's `ManufacturingOrder` embeds the
  * whole `Order` instead, which drags the commercial model — and, through it, live catalog
  * entities — into the shop floor. That coupling is the single hardest one in the codebase, and
  * this type is what replaces it.
  */
final case class ProductionRequest(
    orderId: String,
    orderNumber: String,
    customerName: String,
    lines: List[ProductionRequestLine],
    speed: String,
    placedAt: Timestamp,
)

final case class ProductionRequestLine(
    /** The order line's identity, carried across as-is.
      *
      * The legacy workflow refers to its line by *position* in the order, which silently means
      * something different the moment a line is removed. An id cannot drift like that.
      */
    lineId: String,
    lineNo: Int,
    description: LocalizedString,
    /** The catalog snapshot. Opaque to this context's core, exactly as it is to order-intake. */
    spec: String,
    quantity: Int,
)

/** Where an order is on the shop floor.
  *
  * Deliberately separate from the order's *commercial* status. An order can be paid and not yet
  * approved for production, or approved and not yet paid; collapsing the two is what makes
  * "why is this not being made?" unanswerable.
  */
enum ProductionStatus:
  case AwaitingApproval
  case Approved(at: Timestamp, by: String)
  case Dispatched(at: Timestamp, tracking: Option[String])

  def kind: String = this match
    case AwaitingApproval => "AwaitingApproval"
    case _: Approved      => "Approved"
    case _: Dispatched    => "Dispatched"

/** One step the shop floor has to perform. */
final case class ProductionStepView(
    id: String,
    station: String,
    componentRole: Option[String],
    dependsOn: Set[String],
)

final case class ProductionLineView(
    lineId: String,
    lineNo: Int,
    description: LocalizedString,
    quantity: Int,
    steps: List[ProductionStepView],
)

final case class ProductionOrderView(
    id: String,
    orderId: String,
    orderNumber: String,
    customerName: String,
    status: ProductionStatus,
    speed: String,
    lines: List[ProductionLineView],
    stepCount: Int,
    receivedAt: Timestamp,
)

/** What manufacturing tells the rest of the system.
  *
  * Order-intake consumes these to move the order's own status — approval starts production,
  * dispatch completes it — and translates them at its boundary, as this context translates
  * order-intake's.
  */
enum ProductionEvent:
  case OrderApproved(orderId: String, productionId: String, at: Timestamp)
  case OrderDispatched(
      orderId: String,
      productionId: String,
      tracking: Option[String],
      at: Timestamp,
  )

  def orderId: String
  def productionId: String
