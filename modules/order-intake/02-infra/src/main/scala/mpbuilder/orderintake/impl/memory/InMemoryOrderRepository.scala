package mpbuilder.orderintake
package impl
package memory

import zio.*

/** Orders held in a `Ref`.
  *
  * Insertion order is preserved and every listing reverses it, so "newest first" is a property of
  * this class rather than something each caller sorts for itself — which is what a Postgres index
  * on `placed_at desc` will do when it replaces this.
  */
private[orderintake] final class InMemoryOrderRepository(store: Ref[Vector[Order]])
    extends OrderRepository:

  def find(id: Order.Id): UIO[Option[Order]] = store.get.map(_.find(_.id == id))

  def save(order: Order): UIO[Order] =
    store
      .update(orders =>
        if orders.exists(_.id == order.id) then orders.map(o => if o.id == order.id then order else o)
        else orders :+ order
      )
      .as(order)

  def forCustomer(customerId: String, page: Page): UIO[Paged[Order]] =
    store.get.map(orders => paginate(orders.filter(_.data.customerId.contains(customerId)), page))

  def search(criteria: OrderSearch, page: Page): UIO[Paged[Order]] =
    store.get.map { orders =>
      val byGroup = criteria.statusGroup match
        // Matched against the group, never the leaf: a filter saved as "open" keeps meaning
        // "open" when a new open state is added.
        case Some(group) => orders.filter(_.status.group.equalsIgnoreCase(group))
        case None        => orders

      val byQuery = criteria.query.map(_.trim.toLowerCase).filter(_.nonEmpty) match
        case Some(text) =>
          byGroup.filter { order =>
            order.data.number.toString.toLowerCase.contains(text) ||
            order.data.buyer.displayName.toLowerCase.contains(text) ||
            order.data.buyer.email.toLowerCase.contains(text)
          }
        case None => byGroup

      paginate(byQuery, page)
    }

  private def paginate(orders: Vector[Order], page: Page): Paged[Order] =
    val newestFirst = orders.reverse
    val offset = page.offset.max(0)
    val limit = page.limit.max(1)
    Paged(
      items = newestFirst.slice(offset, offset + limit).toList,
      total = newestFirst.size,
      offset = offset,
      limit = limit,
    )

private[orderintake] object InMemoryOrderRepository:
  val layer: ULayer[OrderRepository] =
    ZLayer.fromZIO(Ref.make(Vector.empty[Order]).map(new InMemoryOrderRepository(_)))

/** Order numbers, counted per year.
  *
  * The sequence restarts every year, which is what makes "MP-2026-000173" both short and free of
  * cross-year collisions. A Postgres sequence per year replaces this without the domain noticing.
  */
private[orderintake] final class InMemoryOrderNumbers(store: Ref[Map[Int, Int]]) extends OrderNumbers:
  def next(year: Int): UIO[Order.Number] =
    store
      .modify { counters =>
        val sequence = counters.getOrElse(year, 0) + 1
        (sequence, counters + (year -> sequence))
      }
      .map(Order.Number(year, _))

private[orderintake] object InMemoryOrderNumbers:
  val layer: ULayer[OrderNumbers] =
    ZLayer.fromZIO(Ref.make(Map.empty[Int, Int]).map(new InMemoryOrderNumbers(_)))

/** The outbox, in memory.
  *
  * Events are appended and kept, rather than handed straight to a subscriber. That is the whole
  * point of an outbox: manufacturing being slow or down must not fail a customer's checkout, and a
  * subscriber that was not listening must still be able to catch up. Phase 6 adds the relay that
  * drains it.
  */
private[orderintake] final class InMemoryEvents(store: Ref[Vector[OrderEvent]]) extends Events:
  def publish(events: List[OrderEvent]): UIO[Unit] = store.update(_ ++ events).unit

  /** Everything published so far. The relay's seam, and what tests assert against. */
  def published: UIO[List[OrderEvent]] = store.get.map(_.toList)
