package mpbuilder.manufacturing
package impl
package memory

import zio.*

/** Production orders held in a `Ref`. */
private[manufacturing] final class InMemoryProductionRepository(store: Ref[Vector[ProductionOrder]])
    extends ProductionRepository:

  def find(id: ProductionOrder.Id): UIO[Option[ProductionOrder]] = store.get.map(_.find(_.id == id))

  def findByOrderId(orderId: String): UIO[Option[ProductionOrder]] =
    store.get.map(_.find(_.data.orderId == orderId))

  def save(order: ProductionOrder): UIO[ProductionOrder] =
    store
      .update(orders =>
        if orders.exists(_.id == order.id) then orders.map(o => if o.id == order.id then order else o)
        else orders :+ order
      )
      .as(order)

  /** Oldest first: it is a queue, and the shop works through it in the order things arrived. */
  def awaitingApproval: UIO[List[ProductionOrder]] =
    store.get.map(_.filter(_.isAwaitingApproval).toList)

/** The outbox, in memory.
  *
  * Entries are numbered and kept until the relay says it has passed them on, so a relay that dies
  * mid-batch re-delivers rather than loses. That makes delivery at-least-once, which is why
  * `ProductionService.accept` is idempotent.
  */
private[manufacturing] final class InMemoryEvents(
    store: Ref[Vector[ProductionOutboxEntry]],
    counter: Ref[Long],
) extends Events
    with ProductionOutbox:

  def publish(events: List[ProductionEvent]): UIO[Unit] =
    ZIO
      .foreach(events) { event =>
        counter.updateAndGet(_ + 1).map(ProductionOutboxEntry(_, event))
      }
      .flatMap(entries => store.update(_ ++ entries))
      .unit

  def pending(limit: Int): UIO[List[ProductionOutboxEntry]] =
    store.get.map(_.take(limit.max(1)).toList)

  def markPublished(ids: List[Long]): UIO[Unit] =
    store.update(_.filterNot(entry => ids.contains(entry.id))).unit

private[manufacturing] object RandomIds extends Ids:
  def next: UIO[String] = Random.nextUUID.map(_.toString)
