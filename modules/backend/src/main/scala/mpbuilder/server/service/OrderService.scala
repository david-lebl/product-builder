package mpbuilder.server.service

import java.util.UUID

import mpbuilder.api.*
import mpbuilder.domain.catalog.Catalog
import mpbuilder.domain.pricing.{Pricelist, PricingEngine}
import mpbuilder.domain.rules.CompatibilityRule
import mpbuilder.domain.sample.*
import mpbuilder.domain.validation.ConfigValidator
import mpbuilder.server.persistence.{Order, OrderRepository}
import zio.*
import zio.prelude.Validation

/** Creating an order: re-validate and re-price the configuration
  * authoritatively (never trusting a client-computed total), then persist it
  * into the new-orders queue with status Placed.
  */
trait OrderService:
  def create(request: CreateOrderRequest): IO[OrderService.Rejection | Throwable, OrderResponse]
  def byId(id: UUID): Task[Option[OrderResponse]]
  def list(limit: Int): Task[List[OrderSummary]]
  def catalogResponse: UIO[CatalogResponse]

object OrderService:
  /** The request was well-formed but the configuration/contact is invalid —
    * carries every accumulated problem for the 422 payload.
    */
  final case class Rejection(errors: List[ErrorDto])

  val live: ZLayer[OrderRepository, Nothing, OrderService] =
    ZLayer.fromFunction(OrderServiceLive(_))

final class OrderServiceLive(repository: OrderRepository) extends OrderService:

  private val catalog: Catalog             = SampleCatalog.catalog
  private val rules: List[CompatibilityRule] = SampleRules.rules
  private val pricelist: Pricelist         = SamplePricelistCzk.pricelist

  override val catalogResponse: UIO[CatalogResponse] =
    ZIO.succeed(CatalogResponse(catalog, SamplePresets.presets, rules, pricelist))

  override def create(request: CreateOrderRequest): IO[OrderService.Rejection | Throwable, OrderResponse] =
    for
      validated <- ZIO
        .fromEither(
          Validation
            .validateWith(
              ConfigValidator.validate(catalog, rules, request.configuration),
              ConfigValidator.validateContact(request.contact),
            )((config, contact) => (config, contact))
            .toEither
        )
        .mapError(errors => OrderService.Rejection(errors.toList.map(ErrorText.dto(catalog, _))))
      (config, contact) = validated
      breakdown <- ZIO
        .fromEither(PricingEngine.price(catalog, pricelist, config))
        .mapError(errors => OrderService.Rejection(errors.toList.map(ErrorText.dto(catalog, _))))
      id        <- Random.nextUUID
      createdAt <- Clock.instant
      order = Order(id, createdAt, contact, config, breakdown, OrderStatus.Placed)
      _ <- repository.insert(order)
    yield toResponse(order)

  override def byId(id: UUID): Task[Option[OrderResponse]] =
    repository.byId(id).map(_.map(toResponse))

  override def list(limit: Int): Task[List[OrderSummary]] =
    repository.list(limit)

  private def toResponse(order: Order): OrderResponse =
    OrderResponse(
      id = order.id,
      createdAt = order.createdAt,
      status = order.status,
      contact = order.contact,
      configuration = order.configuration,
      breakdown = order.breakdown,
    )
