package mpbuilder.orderintake
package impl
package http

import mpbuilder.commons.*
import mpbuilder.identity as id
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

import BasketEndpoints.{ErrorItem, ErrorResponse, ItemView, PriceView}
import CheckoutEndpoints.*

/** Server logic for the checkout endpoints. */
private[orderintake] final class CheckoutRoutes(checkout: CheckoutService, auth: id.AuthService):

  private val actors = Actors(auth)

  def endpoints: List[ServerEndpoint[Any, Task]] = List(
    CheckoutEndpoints.options.zServerLogic { case (bearer, session) =>
      withActor(bearer, session)(checkout.options).map(toOptionsResponse)
    },
    CheckoutEndpoints.quote.zServerLogic { case (bearer, session, req) =>
      withActor(bearer, session)(
        checkout.quote(_, CheckoutDraft(req.deliveryOptionId, req.discountCode, req.paymentMethod))
      ).map(toQuoteResponse)
    },
    CheckoutEndpoints.discount.zServerLogic { case (bearer, session, req) =>
      withActor(bearer, session)(checkout.applyDiscount(_, req.code)).map(toDiscountView)
    },
    CheckoutEndpoints.speeds.zServerLogic { case (bearer, session, itemId) =>
      withActor(bearer, session)(checkout.speedOffers(_, itemId))
        .map(offers => SpeedOffersResponse(offers.map(toSpeedView)))
    },
  )

  private def withActor[A](bearer: Option[String], session: Option[String])(
      f: Actor => IO[CheckoutError, A]
  ): ZIO[Any, (StatusCode, ErrorResponse), A] =
    actors.resolve(bearer, session).flatMap(actor => f(actor).mapError(toResponse))

  // ── Views ───────────────────────────────────────────────────────────────

  private def text(value: LocalizedString): Map[String, String] =
    Language.values.map(l => l.toCode -> value(l)).toMap

  private def toOptionsResponse(options: CheckoutOptions): OptionsResponse =
    OptionsResponse(
      buyer = toBuyerView(options.buyer),
      delivery = options.delivery.map(o =>
        DeliveryOptionView(o.id, o.kind.toString, text(o.name), text(o.detail), o.surcharge, o.currency)
      ),
      payment = options.payment.map {
        case PaymentOffer.Available(method, name) =>
          PaymentOfferView(method.toString, text(name), available = true, reason = None)
        case PaymentOffer.Unavailable(method, name, reason) =>
          PaymentOfferView(method.toString, text(name), available = false, reason = Some(text(reason)))
      },
      currency = options.currency,
    )

  private def toBuyerView(buyer: Buyer): BuyerView = buyer match
    case Buyer.Guest =>
      BuyerView("Guest", None, None, None, "Guest", canPayOnAccount = false)
    case Buyer.Known(customerId, displayName, email, customerType, canPayOnAccount) =>
      BuyerView(
        kind = "Known",
        customerId = Some(customerId),
        displayName = Some(displayName),
        email = Some(email),
        customerType = customerType,
        canPayOnAccount = canPayOnAccount,
      )

  private def toQuoteResponse(quote: CheckoutQuote): QuoteResponse =
    QuoteResponse(
      lines = quote.lines.map(toItemView),
      itemsTotal = quote.itemsTotal,
      discount = quote.discount.map(toDiscountView),
      discountOff = quote.discountOff,
      delivery = quote.delivery.map(d =>
        DeliveryChargeView(d.optionId, text(d.name), d.surcharge, d.waived, d.payable)
      ),
      grandTotal = quote.grandTotal,
      currency = quote.currency,
      paymentMethod = quote.payment.map(_.toString),
      quotedAt = quote.quotedAt,
    )

  private def toDiscountView(decision: DiscountDecision): DiscountView = decision match
    case DiscountDecision.Applied(code, DiscountBenefit.Amount(off)) =>
      DiscountView(code, applied = true, benefit = Some("Amount"), off = Some(off), reason = None)
    case DiscountDecision.Applied(code, DiscountBenefit.FreeDelivery) =>
      // Worth nothing off the goods, which is exactly why the benefit is named rather than
      // reported as a zero amount.
      DiscountView(
        code,
        applied = true,
        benefit = Some("FreeDelivery"),
        off = Some(Money.zero),
        reason = None,
      )
    case DiscountDecision.Refused(code, reason) =>
      DiscountView(code, applied = false, benefit = None, off = None, reason = Some(text(reason)))

  private def toSpeedView(offer: SpeedOfferView): SpeedView = offer match
    case SpeedOfferView.Available(speed, surcharge) =>
      SpeedView(speed.toString, available = true, surcharge = Some(surcharge), reason = None)
    case SpeedOfferView.Unavailable(speed, reason) =>
      SpeedView(speed.toString, available = false, surcharge = None, reason = Some(text(reason)))

  private def toItemView(item: BasketItemView): ItemView =
    ItemView(
      id = item.id,
      description = text(item.description),
      quantity = item.quantity,
      speed = item.speed,
      artworkId = item.artworkId,
      price = PriceView(
        item.price.total,
        item.price.currency,
        item.price.quotedAt,
        item.price.pricelistVersion,
      ),
    )

  private def toResponse(error: CheckoutError): (StatusCode, ErrorResponse) =
    val status = error match
      case CheckoutError.ItemNotFound(_)            => StatusCode.NotFound
      case CheckoutError.EmptyBasket                => StatusCode.UnprocessableEntity
      case CheckoutError.LineNoLongerAvailable(_, _) => StatusCode.UnprocessableEntity
      case CheckoutError.CannotPrice(_)             => StatusCode.UnprocessableEntity
      case CheckoutError.PaymentNotOffered(_, _)    => StatusCode.UnprocessableEntity
      case CheckoutError.UnknownDelivery(_)         => StatusCode.BadRequest
      case CheckoutError.UnknownValue(_, _)         => StatusCode.BadRequest
      case CheckoutError.BuyerLookupFailed(_)       => StatusCode.InternalServerError

    // Accumulated problems stay accumulated, so a checkout page can list everything wrong with a
    // basket at once instead of one problem per attempt.
    val items = error match
      case CheckoutError.LineNoLongerAvailable(itemId, problems) =>
        problems.toList.map(p => ErrorItem(p.code, s"[$itemId] ${p.message(Language.En)}"))
      case CheckoutError.CannotPrice(problems) =>
        problems.toList.map(p => ErrorItem(p.code, p.message(Language.En)))
      case other =>
        List(ErrorItem(other.toString.takeWhile(_ != '('), other.message(Language.En)))

    (status, ErrorResponse(items))
