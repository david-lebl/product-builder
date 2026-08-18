package mpbuilder.manufacturing
package impl

import mpbuilder.commons.*

private[manufacturing] final case class ProductionOrder(
    id: ProductionOrder.Id,
    data: ProductionOrder.Data,
)

private[manufacturing] object ProductionOrder:

  opaque type Id = String
  object Id:
    def apply(raw: String): Id = raw
    extension (id: Id) def value: String = id

  final case class Line(
      lineId: String,
      lineNo: Int,
      description: LocalizedString,
      quantity: Int,
      steps: List[Step],
  )

  final case class Step(
      id: String,
      station: String,
      componentRole: Option[String],
      dependsOn: Set[String],
  )

  final case class Data(
      /** The commercial order this came from. A reference, never the order itself. */
      orderId: String,
      orderNumber: String,
      customerName: String,
      status: ProductionStatus,
      speed: String,
      lines: List[Line],
      receivedAt: Timestamp,
  )

  extension (o: ProductionOrder)
    def stepCount: Int = o.data.lines.map(_.steps.size).sum
    def isAwaitingApproval: Boolean = o.data.status == ProductionStatus.AwaitingApproval

/** Every production rule, as pure functions. */
private[manufacturing] object ProductionPolicy:

  def approve(
      order: ProductionOrder,
      by: String,
      now: Timestamp,
  ): Either[ProductionError, ProductionOrder] =
    order.data.status match
      case ProductionStatus.AwaitingApproval =>
        Right(order.copy(data = order.data.copy(status = ProductionStatus.Approved(now, by))))
      case _: ProductionStatus.Approved   => Left(ProductionError.AlreadyApproved(order.id.value))
      case _: ProductionStatus.Dispatched => Left(ProductionError.AlreadyDispatched(order.id.value))

  def dispatch(
      order: ProductionOrder,
      tracking: Option[String],
      now: Timestamp,
  ): Either[ProductionError, ProductionOrder] =
    order.data.status match
      case _: ProductionStatus.Approved =>
        Right(order.copy(data = order.data.copy(status = ProductionStatus.Dispatched(now, tracking))))
      // Shipping something nobody approved would send goods the shop never agreed to make.
      case ProductionStatus.AwaitingApproval => Left(ProductionError.NotApproved(order.id.value))
      case _: ProductionStatus.Dispatched    => Left(ProductionError.AlreadyDispatched(order.id.value))
