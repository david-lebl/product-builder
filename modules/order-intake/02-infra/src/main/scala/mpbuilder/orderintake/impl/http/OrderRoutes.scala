package mpbuilder.orderintake
package impl
package http

import mpbuilder.commons.*
import mpbuilder.identity as id
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

import BasketEndpoints.{ErrorItem, ErrorResponse, PriceView}
import OrderEndpoints.*

/** Server logic for the order endpoints. */
private[orderintake] final class OrderRoutes(orders: OrderService, auth: id.AuthService):

  private val actors = Actors(auth)

  def endpoints: List[ServerEndpoint[Any, Task]] = List(
    OrderEndpoints.place.zServerLogic { case (bearer, session, lang, req) =>
      withActor(bearer, session, lang)(orders.place(_, toPlaceOrder(req))).map(toOrderResponse)
    },
    OrderEndpoints.list.zServerLogic { case (bearer, session, lang, offset, limit) =>
      withActor(bearer, session, lang)(orders.list(_, toPage(offset, limit))).map(toPagedSummaries)
    },
    OrderEndpoints.find.zServerLogic { case (bearer, session, lang, orderId) =>
      withActor(bearer, session, lang)(orders.find(_, orderId)).map(toOrderResponse)
    },
    OrderEndpoints.cancel.zServerLogic { case (bearer, session, lang, orderId, req) =>
      withActor(bearer, session, lang)(orders.cancel(_, orderId, CancelOrder(req.reason)))
        .map(toOrderResponse)
    },
    OrderEndpoints.staffSearch.zServerLogic { case (bearer, session, lang, status, query, offset, limit) =>
      withActor(bearer, session, lang)(
        orders.search(_, OrderSearch(status, query), toPage(offset, limit))
      ).map(toPagedSummaries)
    },
    OrderEndpoints.confirmPayment.zServerLogic { case (bearer, session, lang, orderId, req) =>
      withActor(bearer, session, lang)(
        orders.confirmPayment(_, orderId, ConfirmPayment(req.reference))
      ).map(toOrderResponse)
    },
    OrderEndpoints.reject.zServerLogic { case (bearer, session, lang, orderId, req) =>
      withActor(bearer, session, lang)(orders.reject(_, orderId, RejectOrder(req.reason)))
        .map(toOrderResponse)
    },
  )

  private def withActor[A](
      bearer: Option[String],
      session: Option[String],
      acceptLanguage: Option[String],
  )(f: Actor => IO[OrderError, A]): ZIO[Any, (StatusCode, ErrorResponse), A] =
    val lang = Languages.parse(acceptLanguage)
    actors.resolve(bearer, session).flatMap(actor => f(actor).mapError(toResponse(lang)))

  private def toPage(offset: Option[Int], limit: Option[Int]): Page =
    Page(offset.getOrElse(0), limit.getOrElse(20))

  // ── Requests in ─────────────────────────────────────────────────────────

  private def toPlaceOrder(req: PlaceOrderRequest): PlaceOrder =
    PlaceOrder(
      buyer = toBuyer(req.buyer),
      billing = toAddress(req.billing),
      shipping = req.shipping.map(toAddress),
      deliveryOptionId = req.deliveryOptionId,
      paymentMethod = req.paymentMethod,
      discountCode = req.discountCode,
      note = req.note,
      expectedTotal = req.expectedTotal,
    )

  /** A company name is what makes a buyer a business, and the two ids only mean anything alongside
    * one. The wire keeps them flat and optional; the domain does not.
    */
  private def toBuyer(req: BuyerRequest): OrderBuyer =
    req.company.map(_.trim).filter(_.nonEmpty) match
      case Some(company) =>
        OrderBuyer.Business(
          req.firstName,
          req.lastName,
          req.email,
          req.phone,
          company,
          req.businessId,
          req.vatId,
        )
      case None => OrderBuyer.Individual(req.firstName, req.lastName, req.email, req.phone)

  private def toAddress(req: AddressRequest): Address =
    Address(req.street, req.city, req.zip, req.country)

  // ── Views out ───────────────────────────────────────────────────────────

  private def text(value: LocalizedString): Map[String, String] =
    Language.values.map(l => l.toCode -> value(l)).toMap

  private def fromBuyer(buyer: OrderBuyer): BuyerRequest = buyer match
    case i: OrderBuyer.Individual =>
      BuyerRequest(i.firstName, i.lastName, i.email, i.phone)
    case b: OrderBuyer.Business =>
      BuyerRequest(
        b.firstName,
        b.lastName,
        b.email,
        b.phone,
        Some(b.company),
        b.businessId,
        b.vatId,
      )

  private def fromAddress(address: Address): AddressRequest =
    AddressRequest(address.street, address.city, address.zip, address.country)

  private def toStatusView(status: OrderStatusView): StatusView =
    StatusView(status.group, status.kind, status.at, status.reason)

  private def toLineView(line: OrderLineView): LineView =
    LineView(
      id = line.id,
      lineNo = line.lineNo,
      description = text(line.description),
      configuration = line.configuration,
      quantity = line.quantity,
      speed = line.speed,
      artworkId = line.artworkId,
      price = PriceView(
        line.price.total,
        line.price.currency,
        line.price.quotedAt,
        line.price.pricelistVersion,
      ),
    )

  private def toOrderResponse(order: OrderView): OrderResponse =
    OrderResponse(
      id = order.id,
      number = order.number,
      status = toStatusView(order.status),
      buyer = fromBuyer(order.buyer),
      customerId = order.customerId,
      lines = order.lines.map(toLineView),
      billing = fromAddress(order.billing),
      shipping = order.shipping.map(fromAddress),
      delivery = order.delivery.map(d =>
        CheckoutEndpoints.DeliveryChargeView(d.optionId, text(d.name), d.surcharge, d.waived, d.payable)
      ),
      paymentMethod = order.paymentMethod,
      discount = order.discount.map(fromDiscount),
      totals = TotalsView(
        order.totals.items,
        order.totals.discount,
        order.totals.delivery,
        order.totals.grand,
        order.totals.currency,
      ),
      speed = order.speed,
      note = order.note,
      placedAt = order.placedAt,
    )

  private def fromDiscount(decision: DiscountDecision): CheckoutEndpoints.DiscountView =
    decision match
      case DiscountDecision.Applied(code, DiscountBenefit.Amount(off)) =>
        CheckoutEndpoints.DiscountView(code, applied = true, Some("Amount"), Some(off), None)
      case DiscountDecision.Applied(code, DiscountBenefit.FreeDelivery) =>
        CheckoutEndpoints.DiscountView(
          code,
          applied = true,
          Some("FreeDelivery"),
          Some(Money.zero),
          None,
        )
      case DiscountDecision.Refused(code, reason) =>
        CheckoutEndpoints.DiscountView(code, applied = false, None, None, Some(text(reason)))

  private def toPagedSummaries(page: Paged[OrderSummaryView]): PagedSummaries =
    PagedSummaries(
      items = page.items.map(s =>
        SummaryView(
          s.id,
          s.number,
          toStatusView(s.status),
          s.buyerName,
          s.lineCount,
          s.total,
          s.currency,
          s.placedAt,
        )
      ),
      total = page.total,
      offset = page.offset,
      limit = page.limit,
    )

  private def toResponse(lang: Language)(error: OrderError): (StatusCode, ErrorResponse) =
    val status = error match
      case OrderError.NotFound(_)             => StatusCode.NotFound
      case OrderError.EmptyBasket             => StatusCode.UnprocessableEntity
      case OrderError.Invalid(_)              => StatusCode.UnprocessableEntity
      // Not an error the customer caused, and not one they can fix by editing the form: the price
      // moved, and they are being asked to agree to the new one.
      case OrderError.PriceChanged(_, _)      => StatusCode.Conflict
      case OrderError.CheckoutRefused(_)      => StatusCode.UnprocessableEntity
      case OrderError.IllegalTransition(_, _) => StatusCode.Conflict
      case OrderError.AlreadyClosed(_)        => StatusCode.Conflict
      case OrderError.StaffOnly               => StatusCode.Forbidden
      case OrderError.NotAuthorised           => StatusCode.Forbidden

    val items = error match
      case OrderError.Invalid(problems) =>
        problems.toList.map(p => ErrorItem(p.code, p.message(lang)))
      case other =>
        List(ErrorItem(other.toString.takeWhile(_ != '('), other.message(lang)))

    (status, ErrorResponse(items))
