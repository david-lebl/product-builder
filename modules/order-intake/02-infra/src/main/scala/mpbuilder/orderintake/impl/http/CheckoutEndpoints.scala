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

import BasketEndpoints.{ErrorResponse, ItemView}

/** Wire shapes and endpoint descriptions for checkout.
  *
  * The domain's sums are flattened on the wire into a boolean plus an optional reason. That is a
  * deliberate loss: a Scala 3 enum with parameterised cases becomes an OpenAPI `oneOf` that most
  * client generators render badly, and `available` with a `reason` is what a form actually branches
  * on. The sums stay sums everywhere behind this file.
  */
private[orderintake] object CheckoutEndpoints:

  // ── Requests ────────────────────────────────────────────────────────────

  final case class QuoteRequest(
      deliveryOptionId: Option[String] = None,
      discountCode: Option[String] = None,
      paymentMethod: Option[String] = None,
  )
  final case class DiscountRequest(code: String)

  // ── Responses ───────────────────────────────────────────────────────────

  final case class BuyerView(
      kind: String,
      customerId: Option[String],
      displayName: Option[String],
      email: Option[String],
      customerType: String,
      canPayOnAccount: Boolean,
  )

  final case class DeliveryOptionView(
      id: String,
      kind: String,
      name: Map[String, String],
      detail: Map[String, String],
      surcharge: Money,
      currency: Currency,
  )

  final case class PaymentOfferView(
      method: String,
      name: Map[String, String],
      available: Boolean,
      /** Present exactly when `available` is false. */
      reason: Option[Map[String, String]],
  )

  final case class OptionsResponse(
      buyer: BuyerView,
      delivery: List[DeliveryOptionView],
      payment: List[PaymentOfferView],
      currency: Currency,
  )

  final case class DiscountView(
      code: String,
      applied: Boolean,
      /** `Amount` or `FreeDelivery`; absent when the code was refused. */
      benefit: Option[String],
      off: Option[Money],
      reason: Option[Map[String, String]],
  )

  final case class DeliveryChargeView(
      optionId: String,
      name: Map[String, String],
      surcharge: Money,
      waived: Boolean,
      payable: Money,
  )

  final case class QuoteResponse(
      lines: List[ItemView],
      itemsTotal: Money,
      discount: Option[DiscountView],
      discountOff: Money,
      delivery: Option[DeliveryChargeView],
      grandTotal: Money,
      currency: Currency,
      paymentMethod: Option[String],
      quotedAt: Timestamp,
  )

  final case class SpeedView(
      speed: String,
      available: Boolean,
      surcharge: Option[Money],
      reason: Option[Map[String, String]],
  )

  final case class SpeedOffersResponse(offers: List[SpeedView])

  // Same reason as in BasketEndpoints: tapir cannot see through an opaque type.
  given Schema[Money] = Schema.schemaForBigDecimal.map(v => Some(Money(v)))(_.value)
  given Schema[Timestamp] = Schema.schemaForLong.map(v => Some(Timestamp(v)))(_.epochMillis)
  given Schema[Currency] = Schema.derivedEnumeration[Currency].defaultStringBased

  given JsonCodec[QuoteRequest] = DeriveJsonCodec.gen
  given JsonCodec[DiscountRequest] = DeriveJsonCodec.gen
  given JsonCodec[BuyerView] = DeriveJsonCodec.gen
  given JsonCodec[DeliveryOptionView] = DeriveJsonCodec.gen
  given JsonCodec[PaymentOfferView] = DeriveJsonCodec.gen
  given JsonCodec[OptionsResponse] = DeriveJsonCodec.gen
  given JsonCodec[DiscountView] = DeriveJsonCodec.gen
  given JsonCodec[DeliveryChargeView] = DeriveJsonCodec.gen
  given JsonCodec[QuoteResponse] = DeriveJsonCodec.gen
  given JsonCodec[SpeedView] = DeriveJsonCodec.gen
  given JsonCodec[SpeedOffersResponse] = DeriveJsonCodec.gen

  private val base = endpoint
    .in("api" / "v1" / "checkout")
    .in(header[Option[String]]("Authorization").and(header[Option[String]]("X-Basket-Session")))
    .errorOut(
      oneOf[(StatusCode, ErrorResponse)](
        oneOfVariantValueMatcher(statusCode.and(jsonBody[ErrorResponse]))({ case _ => true })
      )
    )
    .tag("checkout")

  val options =
    base.get
      .in("options")
      .out(jsonBody[OptionsResponse])
      .summary("Delivery and payment choices for this buyer")
      .description(
        "Every payment method is listed whether or not it is on offer; an unavailable one carries " +
          "the reason, so the customer is never left guessing why invoicing is missing."
      )

  val quote =
    base.post
      .in("quote")
      .in(jsonBody[QuoteRequest])
      .out(jsonBody[QuoteResponse])
      .summary("Price the basket as it would be ordered")
      .description(
        "Re-validates and re-prices every line against today's catalog and prices. A refused " +
          "discount code is part of a successful answer, not an error."
      )

  val discount =
    base.post
      .in("discount")
      .in(jsonBody[DiscountRequest])
      .out(jsonBody[DiscountView])
      .summary("Offer a discount code against the current basket")

  val speeds =
    base.get
      .in("items" / path[String]("itemId") / "speeds")
      .out(jsonBody[SpeedOffersResponse])
      .summary("Production speeds available for a basket line")

  val all = List(options, quote, discount, speeds)
