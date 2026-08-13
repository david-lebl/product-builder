package mpbuilder.server.persistence

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

import io.getquill.*
import io.getquill.jdbczio.Quill
import mpbuilder.api.{OrderStatus, OrderSummary}
import mpbuilder.domain.Currency
import mpbuilder.domain.config.{CustomerContact, ProductConfiguration}
import mpbuilder.domain.ids.*
import mpbuilder.domain.pricing.PriceBreakdown
import org.postgresql.util.PGobject
import zio.*
import zio.json.*

trait OrderRepository:
  def insert(order: Order): Task[Unit]
  def byId(id: UUID): Task[Option[Order]]
  def list(limit: Int): Task[List[OrderSummary]]

object OrderRepository:
  def insert(order: Order): RIO[OrderRepository, Unit]          = ZIO.serviceWithZIO(_.insert(order))
  def byId(id: UUID): RIO[OrderRepository, Option[Order]]       = ZIO.serviceWithZIO(_.byId(id))
  def list(limit: Int): RIO[OrderRepository, List[OrderSummary]] = ZIO.serviceWithZIO(_.list(limit))

  val live: ZLayer[Quill.Postgres[SnakeCase], Nothing, OrderRepository] =
    ZLayer.fromFunction(OrderRepositoryLive(_))

/** Raw JSONB column content. */
final case class JsonB(value: String)

private final case class OrderRow(
  id: UUID,
  createdAt: Instant,
  customerName: String,
  customerEmail: String,
  customerPhone: String,
  customerCompany: Option[String],
  configuration: JsonB,
  priceBreakdown: JsonB,
  currency: String,
  totalAmount: BigDecimal,
  status: String,
)

final class OrderRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends OrderRepository:
  import quill.*

  private given JsonbEncoder: Encoder[JsonB] =
    encoder(
      java.sql.Types.OTHER,
      (index, value, row) => {
        val pg = new PGobject()
        pg.setType("jsonb")
        pg.setValue(value.value)
        row.setObject(index, pg)
      },
    )

  private given JsonbDecoder: Decoder[JsonB] =
    decoder(row => index => JsonB(row.getString(index)))

  private inline def orders = quote(querySchema[OrderRow]("orders"))

  override def insert(order: Order): Task[Unit] =
    val row = toRow(order)
    run(quote(orders.insertValue(lift(row)))).unit

  override def byId(id: UUID): Task[Option[Order]] =
    run(quote(orders.filter(_.id == lift(id))))
      .map(_.headOption)
      .flatMap {
        case None      => ZIO.none
        case Some(row) => ZIO.fromEither(fromRow(row)).mapError(msg => new RuntimeException(msg)).asSome
      }

  override def list(limit: Int): Task[List[OrderSummary]] =
    run(quote(orders.sortBy(_.createdAt)(Ord.desc).take(lift(limit)))).flatMap { rows =>
      ZIO.foreach(rows) { row =>
        ZIO
          .fromEither(row.configuration.value.fromJson[ProductConfiguration])
          .mapBoth(
            msg => new RuntimeException(s"corrupt configuration for order ${row.id}: $msg"),
            config =>
              OrderSummary(
                id = row.id,
                createdAt = row.createdAt,
                customerName = row.customerName,
                categoryId = config.categoryId.raw,
                totalAmount = row.totalAmount,
                currency = Currency.valueOf(row.currency),
                status = OrderStatus.valueOf(row.status),
              ),
          )
      }
    }

  private def toRow(order: Order): OrderRow =
    OrderRow(
      id = order.id,
      createdAt = order.createdAt,
      customerName = order.contact.name,
      customerEmail = order.contact.email,
      customerPhone = order.contact.phone,
      customerCompany = order.contact.company,
      configuration = JsonB(order.configuration.toJson),
      priceBreakdown = JsonB(order.breakdown.toJson),
      currency = order.currency.toString,
      totalAmount = order.totalAmount,
      status = order.status.toString,
    )

  private def fromRow(row: OrderRow): Either[String, Order] =
    for
      config    <- row.configuration.value.fromJson[ProductConfiguration]
      breakdown <- row.priceBreakdown.value.fromJson[PriceBreakdown]
    yield Order(
      id = row.id,
      createdAt = row.createdAt,
      contact = CustomerContact(row.customerName, row.customerEmail, row.customerPhone, row.customerCompany),
      configuration = config,
      breakdown = breakdown,
      status = OrderStatus.valueOf(row.status),
    )
