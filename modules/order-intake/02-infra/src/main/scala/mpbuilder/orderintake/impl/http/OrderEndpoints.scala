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

/** Wire shapes and endpoint descriptions for orders.
  *
  * The status arrives as a group plus a leaf, both plain strings. A client branches on the group —
  * a badge, a filter — so a leaf added later renders sensibly instead of falling off a match.
  */
private[orderintake] object OrderEndpoints:

  // ── Requests ────────────────────────────────────────────────────────────

  final case class BuyerRequest(
      firstName: String,
      lastName: String,
      email: String,
      phone: String,
      /** Present makes this a business buyer; the two ids are only meaningful with it. */
      company: Option[String] = None,
      businessId: Option[String] = None,
      vatId: Option[String] = None,
  )

  final case class AddressRequest(street: String, city: String, zip: String, country: String)

  final case class PlaceOrderRequest(
      buyer: BuyerRequest,
      billing: AddressRequest,
      shipping: Option[AddressRequest] = None,
      deliveryOptionId: Option[String] = None,
      paymentMethod: String,
      discountCode: Option[String] = None,
      note: Option[String] = None,
      expectedTotal: Option[Money] = None,
  )

  final case class ReasonRequest(reason: String)
  final case class PaymentReferenceRequest(reference: String)

  // ── Responses ───────────────────────────────────────────────────────────

  final case class StatusView(
      group: String,
      kind: String,
      at: Option[Timestamp],
      reason: Option[String],
  )

  final case class LineView(
      id: String,
      lineNo: Int,
      description: Map[String, String],
      configuration: String,
      quantity: Int,
      speed: String,
      artworkId: Option[String],
      price: BasketEndpoints.PriceView,
  )

  final case class TotalsView(
      items: Money,
      discount: Money,
      delivery: Money,
      grand: Money,
      currency: Currency,
  )

  final case class OrderResponse(
      id: String,
      number: String,
      status: StatusView,
      buyer: BuyerRequest,
      customerId: Option[String],
      lines: List[LineView],
      billing: AddressRequest,
      shipping: Option[AddressRequest],
      delivery: Option[CheckoutEndpoints.DeliveryChargeView],
      paymentMethod: String,
      discount: Option[CheckoutEndpoints.DiscountView],
      totals: TotalsView,
      speed: String,
      note: Option[String],
      placedAt: Timestamp,
  )

  final case class SummaryView(
      id: String,
      number: String,
      status: StatusView,
      buyerName: String,
      lineCount: Int,
      total: Money,
      currency: Currency,
      placedAt: Timestamp,
  )

  final case class PagedSummaries(items: List[SummaryView], total: Int, offset: Int, limit: Int)

  given Schema[Money] = Schema.schemaForBigDecimal.map(v => Some(Money(v)))(_.value)
  given Schema[Timestamp] = Schema.schemaForLong.map(v => Some(Timestamp(v)))(_.epochMillis)
  given Schema[Currency] = Schema.derivedEnumeration[Currency].defaultStringBased

  given JsonCodec[BuyerRequest] = DeriveJsonCodec.gen
  given JsonCodec[AddressRequest] = DeriveJsonCodec.gen
  given JsonCodec[PlaceOrderRequest] = DeriveJsonCodec.gen
  given JsonCodec[ReasonRequest] = DeriveJsonCodec.gen
  given JsonCodec[PaymentReferenceRequest] = DeriveJsonCodec.gen
  given JsonCodec[StatusView] = DeriveJsonCodec.gen
  given JsonCodec[LineView] = DeriveJsonCodec.gen
  given JsonCodec[TotalsView] = DeriveJsonCodec.gen
  given JsonCodec[OrderResponse] = DeriveJsonCodec.gen
  given JsonCodec[SummaryView] = DeriveJsonCodec.gen
  given JsonCodec[PagedSummaries] = DeriveJsonCodec.gen

  private val base = endpoint
    .in(header[Option[String]]("Authorization").and(header[Option[String]]("X-Basket-Session")))
    .in(BasketEndpoints.languageInput)
    .errorOut(
      oneOf[(StatusCode, ErrorResponse)](
        oneOfVariantValueMatcher(statusCode.and(jsonBody[ErrorResponse]))({ case _ => true })
      )
    )

  private val customerBase = base.in("api" / "v1" / "orders").tag("orders")
  private val staffBase = base.in("api" / "v1" / "staff" / "orders").tag("staff-orders")

  private val paging = query[Option[Int]]("offset").and(query[Option[Int]]("limit"))

  val place =
    customerBase.post
      .in(jsonBody[PlaceOrderRequest])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[OrderResponse])
      .summary("Turn the current basket into an order")
      .description(
        "Re-validates and re-prices every line first. Supply `expectedTotal` to be refused with " +
          "`PriceChanged` rather than charged a total you were not shown."
      )

  val list =
    customerBase.get
      .in(paging)
      .out(jsonBody[PagedSummaries])
      .summary("The caller's own orders, newest first")

  val find =
    customerBase.get
      .in(path[String]("orderId"))
      .out(jsonBody[OrderResponse])
      .summary("One order, if it is yours")

  val cancel =
    customerBase.post
      .in(path[String]("orderId") / "cancel")
      .in(jsonBody[ReasonRequest])
      .out(jsonBody[OrderResponse])
      .summary("Call off an order that is not yet in production")

  val staffSearch =
    staffBase.get
      .in(query[Option[String]]("status").and(query[Option[String]]("q")).and(paging))
      .out(jsonBody[PagedSummaries])
      .summary("Every order, for staff")
      .description(
        "`status` filters on the status *group* — `open`, `active`, `closed_fulfilled`, " +
          "`closed_unfulfilled` — so a saved filter survives a new status being added."
      )

  val confirmPayment =
    staffBase.post
      .in(path[String]("orderId") / "confirm-payment")
      .in(jsonBody[PaymentReferenceRequest])
      .out(jsonBody[OrderResponse])
      .summary("Record that payment arrived")

  val reject =
    staffBase.post
      .in(path[String]("orderId") / "reject")
      .in(jsonBody[ReasonRequest])
      .out(jsonBody[OrderResponse])
      .summary("Refuse an order, with a reason")

  val all = List(place, list, find, cancel, staffSearch, confirmPayment, reject)
