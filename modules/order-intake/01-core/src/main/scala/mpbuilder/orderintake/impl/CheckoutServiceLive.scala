package mpbuilder.orderintake
package impl

import mpbuilder.commons.*
import zio.*

/** Orchestration only. Every rule lives in [[CheckoutPolicy]] or behind a port. */
private[orderintake] final class CheckoutServiceLive(
    baskets: Baskets,
    products: ProductPort,
    quotes: QuotePort,
    discounts: DiscountPort,
    buyers: BuyerPort,
    delivery: DeliveryCatalog,
    now: UIO[Timestamp],
    defaultCurrency: Currency,
) extends CheckoutService:

  def options(actor: Actor): IO[CheckoutError, CheckoutOptions] =
    for
      buyer <- buyers.resolve(actor)
      basket <- baskets.load(actor)
      currency = basket.currency.getOrElse(defaultCurrency)
      shipping <- delivery.options(currency)
    yield CheckoutOptions(
      buyer = buyer,
      delivery = shipping,
      payment = CheckoutPolicy.paymentOffers(buyer),
      currency = currency,
    )

  def quote(actor: Actor, draft: CheckoutDraft): IO[CheckoutError, CheckoutQuote] =
    for
      basket <- baskets.load(actor)
      _ <- ZIO.fail(CheckoutError.EmptyBasket).when(basket.data.items.isEmpty)
      buyer <- buyers.resolve(actor)
      currency = basket.currency.getOrElse(defaultCurrency)
      // Re-validated and re-quoted, not read from the stored quotes: this is the number the
      // customer is about to agree to pay, and the basket may have been sitting for weeks.
      lines <- ZIO.foreach(basket.data.items)(repriced(_, buyer, currency))
      itemsTotal = lines.map(_.data.price.total).foldLeft(Money.zero)(_ + _)
      decision <- ZIO.foreach(draft.discountCode.map(_.trim).filter(_.nonEmpty))(
        offer(_, itemsTotal, basket, buyer)
      )
      chosen <- ZIO.foreach(draft.deliveryOptionId)(deliveryOption(_, currency))
      method <- ZIO.foreach(draft.paymentMethod)(parsePayment)
      _ <- ZIO.foreach(method)(m => ZIO.fromEither(CheckoutPolicy.checkPayment(buyer, m)))
      charge = chosen.map(CheckoutPolicy.charge(_, CheckoutPolicy.waivesDelivery(decision)))
      off = CheckoutPolicy.discountOff(decision)
      timestamp <- now
    yield CheckoutQuote(
      lines = lines.map(toItemView),
      itemsTotal = itemsTotal,
      discount = decision,
      discountOff = off,
      delivery = charge,
      grandTotal = CheckoutPolicy.grandTotal(itemsTotal, off, charge),
      currency = currency,
      payment = method,
      quotedAt = timestamp,
    )

  def applyDiscount(actor: Actor, code: String): IO[CheckoutError, DiscountDecision] =
    for
      basket <- baskets.load(actor)
      _ <- ZIO.fail(CheckoutError.EmptyBasket).when(basket.data.items.isEmpty)
      buyer <- buyers.resolve(actor)
      // Against the stored quotes rather than fresh ones: the customer is asking about this code,
      // and re-pricing the basket underneath them mid-question would be a surprising place to
      // discover that their total moved.
      decision <- offer(code, basket.total, basket, buyer)
    yield decision

  def speedOffers(actor: Actor, itemId: String): IO[CheckoutError, List[SpeedOfferView]] =
    for
      basket <- baskets.load(actor)
      item <- ZIO
        .fromOption(basket.findItem(Basket.Item.Id(itemId)))
        .orElseFail(CheckoutError.ItemNotFound(itemId))
      currency = basket.currency.getOrElse(defaultCurrency)
      offers <- quotes.speedOffers(item.data.spec, currency).mapError(fromBasketError(Some(itemId)))
      // Pricing answers for one production run; this endpoint is about a basket *line*, which may
      // be several runs. Reporting the per-run figure against a line of four would understate the
      // cost of the switch by three quarters — the same mistake the line quote already had to fix.
    yield offers.map {
      case SpeedOfferView.Available(speed, surcharge) =>
        SpeedOfferView.Available(speed, (surcharge * item.data.quantity).rounded)
      case unavailable => unavailable
    }

  // ── internals ────────────────────────────────────────────────────────────

  private def repriced(
      item: Basket.Item,
      buyer: Buyer,
      currency: Currency,
  ): IO[CheckoutError, Basket.Item] =
    val id = item.id.value
    for
      // A line that can no longer be built has to stop the checkout. Letting it through would sell
      // something the shop cannot make.
      _ <- products.stillBuildable(item.data.spec).mapError(fromBasketError(Some(id)))
      price <- quotes
        .quote(item.data.spec, item.data.quantity, item.data.speed, buyer.customerRef, currency)
        .mapError(fromBasketError(None))
    yield item.copy(data = item.data.copy(price = price))

  private def offer(
      code: String,
      orderValue: Money,
      basket: Basket,
      buyer: Buyer,
  ): IO[CheckoutError, DiscountDecision] =
    now.flatMap { timestamp =>
      discounts.offer(code, orderValue, basket.data.items.map(_.data.spec), buyer, timestamp)
    }

  private def deliveryOption(id: String, currency: Currency): IO[CheckoutError, DeliveryOption] =
    delivery
      .options(currency)
      .flatMap(all => ZIO.fromOption(all.find(_.id == id)))
      .orElseFail(CheckoutError.UnknownDelivery(id))

  private def parsePayment(raw: String): IO[CheckoutError, PaymentMethod] =
    ZIO
      .fromOption(PaymentMethod.parse(raw))
      .orElseFail(CheckoutError.UnknownValue("paymentMethod", raw))

  /** Basket-level failures become checkout's own vocabulary without losing their reasons.
    *
    * `itemId` is present when the failure is attributable to one line, which is what lets the
    * client point at the offending row rather than at the basket.
    */
  private def fromBasketError(itemId: Option[String])(error: BasketError): CheckoutError =
    error match
      case BasketError.Rejected(problems) =>
        itemId match
          case Some(id) => CheckoutError.LineNoLongerAvailable(id, problems)
          case None     => CheckoutError.CannotPrice(problems)
      case BasketError.UnknownValue(field, value) => CheckoutError.UnknownValue(field, value)
      case BasketError.ItemNotFound(id)           => CheckoutError.ItemNotFound(id)
      case other =>
        CheckoutError.CannotPrice(
          NonEmptyChunk(
            Problem(
              other.toString.takeWhile(_ != '('),
              other.message(Language.En),
              other.message(Language.Cs),
            )
          )
        )

  private def toItemView(item: Basket.Item): BasketItemView =
    BasketItemView(
      id = item.id.value,
      description = item.data.description,
      quantity = item.data.quantity,
      speed = item.data.speed.toString,
      artworkId = item.data.artworkId,
      price = item.data.price,
    )
