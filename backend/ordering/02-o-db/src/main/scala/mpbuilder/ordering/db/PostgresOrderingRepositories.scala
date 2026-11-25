package mpbuilder.ordering
package db

import core.*
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.platform.db.{Database, KeyedDoc, MigrationSet}
import mpbuilder.shared.*

private[ordering] object OrderingDbCodecs:
  given Schema[Cart]  = Schema.derived
  given Schema[Order] = Schema.derived

  given SqlSchema.Column[Cart]                 = Sql.jsonColumn
  given SqlSchema.Column[Order]                = Sql.jsonColumn
  given SqlSchema.Column[OrderStatusEvent]     = Sql.jsonColumn
  given SqlSchema.Column[ShopPickupLocation]   = Sql.jsonColumn
  given SqlSchema.Column[CourierServiceOption] = Sql.jsonColumn
  given SqlSchema.Column[BankAccountConfig]    = Sql.jsonColumn

import OrderingDbCodecs.given

private[ordering] final class PostgresCartRepository(db: Database) extends CartRepository:

  override def findById(id: CartId): Option[Cart] < Async =
    db.run(sql"SELECT doc FROM ordering.carts WHERE id = ${id.value}".as[Cart].run.map(_.headOption))

  override def save(cart: Cart): Unit < Async =
    db.tx(
      sql"""INSERT INTO ordering.carts (id, customer_id, currency, doc)
            VALUES (${cart.id.value}, ${cart.customerId.map(_.value)}, ${cart.currency.toString}, $cart)
            ON CONFLICT (id) DO UPDATE SET
              customer_id = EXCLUDED.customer_id, currency = EXCLUDED.currency,
              doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

/** Orders persist as a JSONB aggregate (without history) plus an insert-only `order_status_events`
  * table that is the source of truth for the audit timeline.
  */
private[ordering] final class PostgresOrderRepository(db: Database) extends OrderRepository:

  private def hydrate(rows: Chunk[KeyedDoc[Order]], events: Chunk[KeyedDoc[OrderStatusEvent]]): List[Order] =
    val byOrder = events.toList.groupMap(_.key)(_.doc)
    rows.toList.map(r => r.doc.copy(statusHistory = byOrder.getOrElse(r.key, Nil)))

  override def findById(id: OrderId): Option[Order] < Async =
    db.run {
      for
        rows <- sql"SELECT id AS key, doc FROM ordering.orders WHERE id = ${id.value}".as[KeyedDoc[Order]].run
        evs  <- sql"""SELECT order_id AS key, doc FROM ordering.order_status_events
                      WHERE order_id = ${id.value} ORDER BY seq""".as[KeyedDoc[OrderStatusEvent]].run
      yield hydrate(rows, evs).headOption
    }

  override def findByCustomer(customerId: CustomerId): List[Order] < Async =
    db.run {
      for
        rows <- sql"""SELECT id AS key, doc FROM ordering.orders
                      WHERE customer_id = ${customerId.value} ORDER BY placed_at""".as[KeyedDoc[Order]].run
        evs  <- sql"""SELECT e.order_id AS key, e.doc FROM ordering.order_status_events e
                      JOIN ordering.orders o ON o.id = e.order_id
                      WHERE o.customer_id = ${customerId.value} ORDER BY e.order_id, e.seq""".as[KeyedDoc[OrderStatusEvent]].run
      yield hydrate(rows, evs)
    }

  override def listAll: List[Order] < Async =
    db.run {
      for
        rows <- sql"SELECT id AS key, doc FROM ordering.orders ORDER BY placed_at".as[KeyedDoc[Order]].run
        evs  <- sql"""SELECT order_id AS key, doc FROM ordering.order_status_events
                      ORDER BY order_id, seq""".as[KeyedDoc[OrderStatusEvent]].run
      yield hydrate(rows, evs)
    }

  override def save(order: Order): Unit < Async =
    val doc = order.copy(statusHistory = Nil)
    db.tx {
      for
        _         <- sql"""INSERT INTO ordering.orders
                             (id, order_number, customer_id, customer_email, status, payment_status, currency, grand_total, placed_at, doc)
                           VALUES (${order.id.value}, ${order.orderNumber.value}, ${order.customer.customerId.map(_.value)},
                                   ${order.customer.email.value}, ${order.status.kind.toString}, ${order.payment.status.toString},
                                   ${order.totals.currency.toString}, ${order.totals.grandTotalGross.value}, ${order.placedAt}, $doc)
                           ON CONFLICT (id) DO UPDATE SET
                             order_number   = EXCLUDED.order_number,
                             customer_id    = EXCLUDED.customer_id,
                             customer_email = EXCLUDED.customer_email,
                             status         = EXCLUDED.status,
                             payment_status = EXCLUDED.payment_status,
                             currency       = EXCLUDED.currency,
                             grand_total    = EXCLUDED.grand_total,
                             doc            = EXCLUDED.doc,
                             updated_at     = now()""".execute
        // Append only the events not yet persisted (history is immutable).
        persisted <- sql"SELECT count(*) FROM ordering.order_status_events WHERE order_id = ${order.id.value}".as[Long].run
        _         <- Kyo.foreachDiscard(order.statusHistory.zipWithIndex.drop(persisted.head.toInt)) { case (ev, seq) =>
                       sql"""INSERT INTO ordering.order_status_events (order_id, seq, to_status, occurred_at, doc)
                             VALUES (${order.id.value}, $seq, ${ev.to.kind.toString}, ${ev.occurredAt}, $ev)
                             ON CONFLICT (order_id, seq) DO NOTHING""".execute
                     }
      yield ()
    }

private[ordering] final class PostgresDeliveryCatalogRepository(db: Database) extends DeliveryCatalogRepository:

  override def pickupLocations: List[ShopPickupLocation] < Async =
    db.run(sql"SELECT doc FROM ordering.pickup_locations ORDER BY position".as[ShopPickupLocation].run.map(_.toList))

  override def savePickupLocation(loc: ShopPickupLocation): Unit < Async =
    db.tx(
      sql"""INSERT INTO ordering.pickup_locations (id, doc) VALUES (${loc.id.value}, $loc)
            ON CONFLICT (id) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

  override def deletePickupLocation(id: DeliveryOptionId): Unit < Async =
    db.tx(sql"DELETE FROM ordering.pickup_locations WHERE id = ${id.value}".execute).unit

  override def courierOptions(currency: Currency): List[CourierServiceOption] < Async =
    db.run(
      sql"SELECT doc FROM ordering.courier_options WHERE currency = ${currency.toString} ORDER BY position"
        .as[CourierServiceOption].run.map(_.toList)
    )

  override def allCourierOptions: List[CourierServiceOption] < Async =
    db.run(sql"SELECT doc FROM ordering.courier_options ORDER BY currency, position".as[CourierServiceOption].run.map(_.toList))

  override def saveCourierOption(opt: CourierServiceOption): Unit < Async =
    db.tx(
      sql"""INSERT INTO ordering.courier_options (currency, id, doc) VALUES (${opt.currency.toString}, ${opt.id.value}, $opt)
            ON CONFLICT (currency, id) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

  override def deleteCourierOption(id: DeliveryOptionId, currency: Currency): Unit < Async =
    db.tx(sql"DELETE FROM ordering.courier_options WHERE currency = ${currency.toString} AND id = ${id.value}".execute).unit

  override def bankAccount(currency: Currency): Option[BankAccountConfig] < Async =
    db.run(
      sql"SELECT doc FROM ordering.bank_accounts WHERE currency = ${currency.toString}".as[BankAccountConfig].run.map(_.headOption)
    )

  override def allBankAccounts: Map[Currency, BankAccountConfig] < Async =
    db.run(
      sql"SELECT currency AS key, doc FROM ordering.bank_accounts".as[KeyedDoc[BankAccountConfig]].run.map { rows =>
        rows.flatMap(r => Currency.values.find(_.toString == r.key).map(_ -> r.doc)).toMap
      }
    )

  override def saveBankAccount(currency: Currency, config: BankAccountConfig): Unit < Async =
    db.tx(
      sql"""INSERT INTO ordering.bank_accounts (currency, doc) VALUES (${currency.toString}, $config)
            ON CONFLICT (currency) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

/** PostgreSQL-backed ordering repositories. */
private[ordering] final case class OrderingRepositories(
    carts: PostgresCartRepository,
    orders: PostgresOrderRepository,
    delivery: PostgresDeliveryCatalogRepository,
)

/** `CartService`, `CheckoutOptionsService` and `OrderService` backed by PostgreSQL. */
final case class OrderingServices(carts: CartService, checkoutOptions: CheckoutOptionsService, orders: OrderService)

object OrderingDb:
  val migrations: MigrationSet = MigrationSet("ordering", "V1__ordering.sql")

  private[ordering] def repositories(db: Database): OrderingRepositories =
    OrderingRepositories(new PostgresCartRepository(db), new PostgresOrderRepository(db), new PostgresDeliveryCatalogRepository(db))

  def services(db: Database, eventBus: EventBus): OrderingServices =
    val r = repositories(db)
    OrderingServices(
      OrderingCoreLayers.cartService(r.carts),
      OrderingCoreLayers.checkoutOptionsService(r.delivery),
      OrderingCoreLayers.orderService(r.orders, eventBus),
    )

  /** Seeds delivery settings, demo carts and demo orders on an empty database. */
  private[ordering] def seedIfEmpty(
      db: Database,
      pickups: List[ShopPickupLocation],
      couriers: List[CourierServiceOption],
      bankAccounts: Map[Currency, BankAccountConfig],
      carts: List[Cart],
      orders: List[Order],
  ): Boolean < Async =
    val r = repositories(db)
    r.delivery.pickupLocations.map { existing =>
      if existing.nonEmpty then false
      else
        for
          _ <- Kyo.foreachDiscard(pickups)(r.delivery.savePickupLocation)
          _ <- Kyo.foreachDiscard(couriers)(r.delivery.saveCourierOption)
          _ <- Kyo.foreachDiscard(bankAccounts.toList)((c, a) => r.delivery.saveBankAccount(c, a))
          _ <- Kyo.foreachDiscard(carts)(r.carts.save)
          _ <- Kyo.foreachDiscard(orders)(r.orders.save)
        yield true
    }
