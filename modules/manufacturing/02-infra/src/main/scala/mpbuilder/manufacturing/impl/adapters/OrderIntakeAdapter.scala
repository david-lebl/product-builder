package mpbuilder.manufacturing
package impl
package adapters

import mpbuilder.commons.*
import mpbuilder.orderintake as oi

/** Translating order-intake's events into this context's own vocabulary.
  *
  * The only file in manufacturing that knows order-intake exists. It reads an `OrderPlaced` and
  * produces a [[ProductionRequest]] — a snapshot of what to make, with no commercial concepts in
  * it at all.
  *
  * This is what replaces `ManufacturingOrder` embedding the whole `Order`. The shop floor gets
  * told what to make; it cannot reach back into the order to find out, which is precisely why the
  * two contexts can be pulled apart later.
  */
private[manufacturing] object OrderIntakeAdapter:

  /** A production request, for the events that describe something to make.
    *
    * Returns `None` for the rest: an order being paid or cancelled is not work, and this context
    * has no opinion about it.
    */
  def toRequest(event: oi.OrderEvent): Option[ProductionRequest] = event match
    case placed: oi.OrderEvent.OrderPlaced =>
      Some(
        ProductionRequest(
          orderId = placed.orderId,
          orderNumber = placed.number,
          customerName = placed.buyerName,
          lines = placed.lines.map(toLine),
          speed = placed.speed,
          placedAt = placed.placedAt,
        )
      )
    case _ => None

  private def toLine(line: oi.OrderLineView): ProductionRequestLine =
    ProductionRequestLine(
      // The line's id, carried across unchanged. The legacy workflow refers to its line by
      // position in the order, which quietly means something else as soon as a line is removed.
      lineId = line.id,
      lineNo = line.lineNo,
      description = line.description,
      spec = line.configuration,
      quantity = line.quantity,
    )
