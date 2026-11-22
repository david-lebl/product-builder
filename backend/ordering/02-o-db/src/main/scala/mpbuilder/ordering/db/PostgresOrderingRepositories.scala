package mpbuilder.ordering
package db

import com.augustnagro.magnum.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*
import zio.*

private[ordering] object OrderingDbCodecs:
  given JsonValueCodec[Cart]  = JsonCodecMaker.make(config)
  given JsonValueCodec[Order] = JsonCodecMaker.make(config)

import OrderingDbCodecs.given

private[ordering] final class PostgresCartRepository(db: Database) extends CartRepository:

  override def findById(id: CartId): UIO[Option[Cart]] =
    db.run(sql"SELECT doc::text FROM ordering.carts WHERE id = ${id.value}".query[String].run().headOption.map(fromJsonUnsafe[Cart]))

  override def save(cart: Cart): UIO[Unit] =
    val json = toJson(cart)
    db.tx(
      sql"""INSERT INTO ordering.carts (id, customer_id, currency, doc)
            VALUES (${cart.id.value}, ${cart.customerId.map(_.value)}, ${cart.currency.toString}, $json::jsonb)
            ON CONFLICT (id) DO UPDATE SET
              customer_id = EXCLUDED.customer_id, currency = EXCLUDED.currency,
              doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

/** Orders persist as a JSONB aggregate (without history) plus an insert-only `order_status_events`
  * table that is the source of truth for the audit timeline.
  */
private[ordering] final class PostgresOrderRepository(db: Database) extends OrderRepository:

  private def hydrate(rows: Vector[(String, String)], events: Vector[(String, String)]): List[Order] =
    val byOrder = events.toList.groupMap(_._1)(r => fromJsonUnsafe[OrderStatusEvent](r._2))
    rows.toList.map { case (id, doc) => fromJsonUnsafe[Order](doc).copy(statusHistory = byOrder.getOrElse(id, Nil)) }

  override def findById(id: OrderId): UIO[Option[Order]] =
    db.run {
      val rows = sql"SELECT id, doc::text FROM ordering.orders WHERE id = ${id.value}".query[(String, String)].run()
      val evs  = sql"SELECT order_id, doc::text FROM ordering.order_status_events WHERE order_id = ${id.value} ORDER BY seq"
                   .query[(String, String)].run()
      hydrate(rows, evs).headOption
    }

  override def findByCustomer(customerId: CustomerId): UIO[List[Order]] =
    db.run {
      val rows = sql"SELECT id, doc::text FROM ordering.orders WHERE customer_id = ${customerId.value} ORDER BY placed_at"
                   .query[(String, String)].run()
      val evs  = sql"""SELECT e.order_id, e.doc::text FROM ordering.order_status_events e
                       JOIN ordering.orders o ON o.id = e.order_id
                       WHERE o.customer_id = ${customerId.value} ORDER BY e.order_id, e.seq""".query[(String, String)].run()
      hydrate(rows, evs)
    }

  override def listAll: UIO[List[Order]] =
    db.run {
      val rows = sql"SELECT id, doc::text FROM ordering.orders ORDER BY placed_at".query[(String, String)].run()
      val evs  = sql"SELECT order_id, doc::text FROM ordering.order_status_events ORDER BY order_id, seq".query[(String, String)].run()
      hydrate(rows, evs)
    }

  override def save(order: Order): UIO[Unit] =
    val json = toJson(order.copy(statusHistory = Nil))
    db.tx {
      sql"""INSERT INTO ordering.orders
              (id, order_number, customer_id, customer_email, status, payment_status, currency, grand_total, placed_at, doc)
            VALUES (${order.id.value}, ${order.orderNumber.value}, ${order.customer.customerId.map(_.value)},
                    ${order.customer.email.value}, ${order.status.kind.toString}, ${order.payment.status.toString},
                    ${order.totals.currency.toString}, ${order.totals.grandTotalGross.value}, ${order.placedAt}, $json::jsonb)
            ON CONFLICT (id) DO UPDATE SET
              order_number   = EXCLUDED.order_number,
              customer_id    = EXCLUDED.customer_id,
              customer_email = EXCLUDED.customer_email,
              status         = EXCLUDED.status,
              payment_status = EXCLUDED.payment_status,
              currency       = EXCLUDED.currency,
              grand_total    = EXCLUDED.grand_total,
              doc            = EXCLUDED.doc,
              updated_at     = now()""".update.run()
      // Append only the events not yet persisted (history is immutable).
      val persisted = sql"SELECT count(*) FROM ordering.order_status_events WHERE order_id = ${order.id.value}".query[Long].run().head.toInt
      order.statusHistory.zipWithIndex.drop(persisted).foreach { case (ev, seq) =>
        sql"""INSERT INTO ordering.order_status_events (order_id, seq, to_status, occurred_at, doc)
              VALUES (${order.id.value}, $seq, ${ev.to.kind.toString}, ${ev.occurredAt}, ${toJson(ev)}::jsonb)
              ON CONFLICT (order_id, seq) DO NOTHING""".update.run()
      }
    }

private[ordering] final class PostgresDeliveryCatalogRepository(db: Database) extends DeliveryCatalogRepository:

  override def pickupLocations: UIO[List[ShopPickupLocation]] =
    db.run(sql"SELECT doc::text FROM ordering.pickup_locations ORDER BY position".query[String].run().toList.map(fromJsonUnsafe[ShopPickupLocation]))

  override def savePickupLocation(loc: ShopPickupLocation): UIO[Unit] =
    val json = toJson(loc)
    db.tx(
      sql"""INSERT INTO ordering.pickup_locations (id, doc) VALUES (${loc.id.value}, $json::jsonb)
            ON CONFLICT (id) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

  override def deletePickupLocation(id: DeliveryOptionId): UIO[Unit] =
    db.tx(sql"DELETE FROM ordering.pickup_locations WHERE id = ${id.value}".update.run()).unit

  override def courierOptions(currency: Currency): UIO[List[CourierServiceOption]] =
    db.run(
      sql"SELECT doc::text FROM ordering.courier_options WHERE currency = ${currency.toString} ORDER BY position"
        .query[String].run().toList.map(fromJsonUnsafe[CourierServiceOption])
    )

  override def allCourierOptions: UIO[List[CourierServiceOption]] =
    db.run(sql"SELECT doc::text FROM ordering.courier_options ORDER BY currency, position".query[String].run().toList.map(fromJsonUnsafe[CourierServiceOption]))

  override def saveCourierOption(opt: CourierServiceOption): UIO[Unit] =
    val json = toJson(opt)
    db.tx(
      sql"""INSERT INTO ordering.courier_options (currency, id, doc) VALUES (${opt.currency.toString}, ${opt.id.value}, $json::jsonb)
            ON CONFLICT (currency, id) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

  override def deleteCourierOption(id: DeliveryOptionId, currency: Currency): UIO[Unit] =
    db.tx(sql"DELETE FROM ordering.courier_options WHERE currency = ${currency.toString} AND id = ${id.value}".update.run()).unit

  override def bankAccount(currency: Currency): UIO[Option[BankAccountConfig]] =
    db.run(
      sql"SELECT doc::text FROM ordering.bank_accounts WHERE currency = ${currency.toString}"
        .query[String].run().headOption.map(fromJsonUnsafe[BankAccountConfig])
    )

  override def allBankAccounts: UIO[Map[Currency, BankAccountConfig]] =
    db.run(
      sql"SELECT currency, doc::text FROM ordering.bank_accounts".query[(String, String)].run()
        .flatMap { case (cur, doc) => Currency.values.find(_.toString == cur).map(_ -> fromJsonUnsafe[BankAccountConfig](doc)) }
        .toMap
    )

  override def saveBankAccount(currency: Currency, config: BankAccountConfig): UIO[Unit] =
    val json = toJson(config)
    db.tx(
      sql"""INSERT INTO ordering.bank_accounts (currency, doc) VALUES (${currency.toString}, $json::jsonb)
            ON CONFLICT (currency) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

object OrderingDb:
  val migrations: MigrationSet = MigrationSet("ordering")

  private[ordering] def cartRepository: URLayer[Database, CartRepository] =
    ZLayer.fromFunction((db: Database) => new PostgresCartRepository(db))
  private[ordering] def orderRepository: URLayer[Database, OrderRepository] =
    ZLayer.fromFunction((db: Database) => new PostgresOrderRepository(db))
  private[ordering] def deliveryRepository: URLayer[Database, DeliveryCatalogRepository] =
    ZLayer.fromFunction((db: Database) => new PostgresDeliveryCatalogRepository(db))

  /** `CartService`, `CheckoutOptionsService` and `OrderService` backed by PostgreSQL. */
  val services: URLayer[Database & EventBus, CartService & CheckoutOptionsService & OrderService] =
    (cartRepository >>> OrderingCoreLayers.cartLayer) ++
      (deliveryRepository >>> OrderingCoreLayers.checkoutOptionsLayer) ++
      ((orderRepository ++ ZLayer.service[EventBus]) >>> OrderingCoreLayers.orderLayer)

  /** Seeds delivery settings, demo carts and demo orders on an empty database. */
  private[ordering] def seedIfEmpty(
      pickups: List[ShopPickupLocation],
      couriers: List[CourierServiceOption],
      bankAccounts: Map[Currency, BankAccountConfig],
      carts: List[Cart],
      orders: List[Order],
  ): URIO[Database, Boolean] =
    ZIO.serviceWithZIO[Database] { db =>
      val delivery = new PostgresDeliveryCatalogRepository(db)
      delivery.pickupLocations.flatMap { existing =>
        ZIO
          .when(existing.isEmpty) {
            val cartRepo  = new PostgresCartRepository(db)
            val orderRepo = new PostgresOrderRepository(db)
            ZIO.foreachDiscard(pickups)(delivery.savePickupLocation) *>
              ZIO.foreachDiscard(couriers)(delivery.saveCourierOption) *>
              ZIO.foreachDiscard(bankAccounts.toList)((c, a) => delivery.saveBankAccount(c, a)) *>
              ZIO.foreachDiscard(carts)(cartRepo.save) *>
              ZIO.foreachDiscard(orders)(orderRepo.save)
          }
          .map(_.isDefined)
      }
    }
