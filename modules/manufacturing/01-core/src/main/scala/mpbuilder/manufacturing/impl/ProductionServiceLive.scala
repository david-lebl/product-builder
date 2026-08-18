package mpbuilder.manufacturing
package impl

import mpbuilder.commons.*
import zio.*

/** Orchestration only. Every rule lives in [[ProductionPolicy]] or behind a port. */
private[manufacturing] final class ProductionServiceLive(
    orders: ProductionRepository,
    workflows: WorkflowPort,
    events: Events,
    ids: Ids,
    now: UIO[Timestamp],
) extends ProductionService:

  def accept(request: ProductionRequest): IO[ProductionError, ProductionOrderView] =
    orders.findByOrderId(request.orderId).flatMap {
      // Already on the floor. An outbox delivers at least once, so a retried delivery must be a
      // no-op rather than a second run of the same job.
      case Some(existing) => ZIO.succeed(toView(existing))
      case None =>
        for
          lines <- ZIO.foreach(request.lines)(planned(_, request.orderId))
          timestamp <- now
          id <- ids.next
          order = ProductionOrder(
            ProductionOrder.Id(id),
            ProductionOrder.Data(
              orderId = request.orderId,
              orderNumber = request.orderNumber,
              customerName = request.customerName,
              status = ProductionStatus.AwaitingApproval,
              speed = request.speed,
              lines = lines,
              receivedAt = timestamp,
            ),
          )
          saved <- orders.save(order)
        yield toView(saved)
    }

  def queue: IO[ProductionError, List[ProductionOrderView]] =
    orders.awaitingApproval.map(_.map(toView))

  def find(id: String): IO[ProductionError, ProductionOrderView] =
    load(id).map(toView)

  def approve(id: String, by: String): IO[ProductionError, ProductionOrderView] =
    for
      order <- load(id)
      timestamp <- now
      approved <- ZIO.fromEither(ProductionPolicy.approve(order, by, timestamp))
      saved <- orders.save(approved)
      _ <- events.publish(
        List(ProductionEvent.OrderApproved(saved.data.orderId, saved.id.value, timestamp))
      )
    yield toView(saved)

  def dispatch(id: String, tracking: Option[String]): IO[ProductionError, ProductionOrderView] =
    for
      order <- load(id)
      timestamp <- now
      dispatched <- ZIO.fromEither(ProductionPolicy.dispatch(order, tracking, timestamp))
      saved <- orders.save(dispatched)
      _ <- events.publish(
        List(
          ProductionEvent.OrderDispatched(saved.data.orderId, saved.id.value, tracking, timestamp)
        )
      )
    yield toView(saved)

  // ── internals ────────────────────────────────────────────────────────────

  private def load(id: String): IO[ProductionError, ProductionOrder] =
    orders
      .find(ProductionOrder.Id(id))
      .flatMap(ZIO.fromOption(_).orElseFail(ProductionError.NotFound(id)))

  private def planned(
      line: ProductionRequestLine,
      orderId: String,
  ): IO[ProductionError, ProductionOrder.Line] =
    workflows
      .plan(line.spec, line.lineNo, orderId, line.quantity)
      .map(steps =>
        ProductionOrder.Line(
          lineId = line.lineId,
          lineNo = line.lineNo,
          description = line.description,
          quantity = line.quantity,
          steps = steps,
        )
      )

  private def toView(order: ProductionOrder): ProductionOrderView =
    ProductionOrderView(
      id = order.id.value,
      orderId = order.data.orderId,
      orderNumber = order.data.orderNumber,
      customerName = order.data.customerName,
      status = order.data.status,
      speed = order.data.speed,
      lines = order.data.lines.map(line =>
        ProductionLineView(
          lineId = line.lineId,
          lineNo = line.lineNo,
          description = line.description,
          quantity = line.quantity,
          steps = line.steps.map(s =>
            ProductionStepView(s.id, s.station, s.componentRole, s.dependsOn)
          ),
        )
      ),
      stepCount = order.stepCount,
      receivedAt = order.data.receivedAt,
    )
