package mpbuilder.orderintake
package impl
package http

import mpbuilder.commons.*
import mpbuilder.commons.json.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.json.*

/** Wire shapes and endpoint descriptions for the basket. */
private[orderintake] object BasketEndpoints:

  final case class AddItemRequest(
      configuration: String,
      quantity: Int,
      speed: String = "Standard",
      artworkId: Option[String] = None,
  )
  final case class UpdateQuantityRequest(quantity: Int)
  final case class MergeRequest(fromSession: String)

  final case class PriceView(
      total: Money,
      currency: Currency,
      quotedAt: Timestamp,
      pricelistVersion: String,
  )
  final case class ItemView(
      id: String,
      description: Map[String, String],
      quantity: Int,
      speed: String,
      artworkId: Option[String],
      price: PriceView,
  )
  final case class BasketResponse(
      id: String,
      items: List[ItemView],
      total: Money,
      currency: Currency,
      itemCount: Int,
      expiresAt: Timestamp,
  )

  final case class ErrorItem(code: String, message: String)
  final case class ErrorResponse(errors: List[ErrorItem])

  // tapir cannot see through an opaque type, so the kernel types need their schemas stated. They
  // live here rather than in `commons` because commons is cross-compiled to JS and must not carry
  // a server-side dependency.
  given Schema[Money] = Schema.schemaForBigDecimal.map(v => Some(Money(v)))(_.value)
  given Schema[Timestamp] = Schema.schemaForLong.map(v => Some(Timestamp(v)))(_.epochMillis)
  given Schema[Currency] = Schema.derivedEnumeration[Currency].defaultStringBased

  given JsonCodec[AddItemRequest] = DeriveJsonCodec.gen
  given JsonCodec[UpdateQuantityRequest] = DeriveJsonCodec.gen
  given JsonCodec[MergeRequest] = DeriveJsonCodec.gen
  given JsonCodec[PriceView] = DeriveJsonCodec.gen
  given JsonCodec[ItemView] = DeriveJsonCodec.gen
  given JsonCodec[BasketResponse] = DeriveJsonCodec.gen
  given JsonCodec[ErrorItem] = DeriveJsonCodec.gen
  given JsonCodec[ErrorResponse] = DeriveJsonCodec.gen

  /** Identifies the caller.
    *
    * A bearer token when signed in; otherwise an opaque session token the client keeps. Both are
    * optional at the type level so one endpoint description serves both cases — the server logic
    * turns the pair into an [[Actor]], and rejects the request if neither is present.
    */
  private val actorInput: EndpointInput[(Option[String], Option[String])] =
    header[Option[String]]("Authorization").and(header[Option[String]]("X-Basket-Session"))

  private val base = endpoint
    .in("api" / "v1" / "baskets")
    .in(actorInput)
    .errorOut(
      oneOf[(StatusCode, ErrorResponse)](
        oneOfVariantValueMatcher(statusCode.and(jsonBody[ErrorResponse]))({ case _ => true })
      )
    )
    .tag("basket")

  type Auth = (Option[String], Option[String])

  val current =
    base.get.in("current").out(jsonBody[BasketResponse]).summary("The caller's basket")

  val addItem =
    base.post
      .in("current" / "items")
      .in(jsonBody[AddItemRequest])
      .out(jsonBody[BasketResponse])
      .summary("Add a configured product")
      .description(
        "`configuration` is a catalog configuration request, as JSON. Order-intake passes it " +
          "through to the catalog context without interpreting it."
      )

  val updateQuantity =
    base.patch
      .in("current" / "items" / path[String]("itemId"))
      .in(jsonBody[UpdateQuantityRequest])
      .out(jsonBody[BasketResponse])
      .summary("Change how many of a line are wanted")

  val removeItem =
    base.delete
      .in("current" / "items" / path[String]("itemId"))
      .out(jsonBody[BasketResponse])
      .summary("Remove a line")

  val clear =
    base.delete.in("current").out(jsonBody[BasketResponse]).summary("Empty the basket")

  val requote =
    base.post
      .in("current" / "requote")
      .out(jsonBody[BasketResponse])
      .summary("Re-price every line against today's prices")

  val merge =
    base.post
      .in("current" / "merge")
      .in(jsonBody[MergeRequest])
      .out(jsonBody[BasketResponse])
      .summary("Fold an anonymous basket into the signed-in customer's")

  val all = List(current, addItem, updateQuantity, removeItem, clear, requote, merge)
