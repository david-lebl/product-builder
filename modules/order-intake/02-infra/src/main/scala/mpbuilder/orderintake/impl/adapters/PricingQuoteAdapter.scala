package mpbuilder.orderintake
package impl
package adapters

import mpbuilder.commons.*
import mpbuilder.pricing as pri
import zio.*

/** [[QuotePort]] and [[DiscountPort]] satisfied by calling pricing's public service.
  *
  * Order-intake never computes a price, applies a discount, or reads a pricelist. It asks, and
  * stores the answer with the moment and pricelist version it came from. One adapter per foreign
  * context, so replacing pricing with a remote service touches exactly this file.
  */
private[orderintake] final class PricingQuoteAdapter(
    pricing: pri.PricingService,
    now: UIO[Timestamp],
) extends QuotePort
    with DiscountPort:

  def quote(
      spec: ProductSpec,
      quantity: Int,
      speed: ProductionSpeed,
      customerId: Option[String],
      currency: Currency,
  ): IO[BasketError, QuotedPrice] =
    for
      result <- pricing
        .quote(
          pri.QuoteRequest(
            spec = pri.ProductSpec(spec.payload, spec.catalogVersion),
            speed = toPricingSpeed(speed),
            customerId = customerId,
            currency = currency,
          )
        )
        .mapError(toBasketError)
      timestamp <- now
    yield QuotedPrice(
      // Two different quantities are in play, and conflating them is easy: the *configuration*
      // carries how many pieces one run produces (500 business cards), and volume tiers apply to
      // that — pricing has already accounted for it. The basket line's quantity is how many such
      // runs the customer wants, so it multiplies. Matches the legacy
      // `BasketService.calculateTotal`, which computes `priceBreakdown.total * item.quantity`.
      total = result.total * quantity,
      currency = result.currency,
      quotedAt = timestamp,
      pricelistVersion = result.pricelistVersion,
    )

  def speedOffers(spec: ProductSpec, currency: Currency): IO[BasketError, List[SpeedOfferView]] =
    pricing
      .speedOffers(pri.ProductSpec(spec.payload, spec.catalogVersion), currency)
      .mapBoth(toBasketError, _.toList.map(toOfferView))

  def offer(
      code: String,
      orderValue: Money,
      specs: List[ProductSpec],
      buyer: Buyer,
      timestamp: Timestamp,
  ): IO[CheckoutError, DiscountDecision] =
    pricing
      .applyDiscount(
        code,
        pri.DiscountContext(
          orderValue = orderValue,
          // The specs go across whole. Which categories they belong to is pricing's to read — this
          // context cannot see inside the payload, and a category-restricted code has to work.
          specs = specs.map(s => pri.ProductSpec(s.payload, s.catalogVersion)),
          customerType = Some(buyer.customerTypeName),
          customerId = buyer.customerRef,
          now = timestamp,
        ),
      )
      .mapBoth(toCheckoutError, toDecision)

  private def toOfferView(offer: pri.SpeedOffer): SpeedOfferView = offer match
    case pri.SpeedOffer.Available(speed, surcharge) =>
      SpeedOfferView.Available(toOwnSpeed(speed), surcharge)
    case pri.SpeedOffer.Unavailable(speed, reason) =>
      SpeedOfferView.Unavailable(toOwnSpeed(speed), explain(reason))

  /** Pricing's reasons are already localized text by the time they reach the boundary, so this
    * translates the *shape*, not the words.
    */
  private def explain(reason: pri.SpeedUnavailable): LocalizedString = reason match
    case pri.SpeedUnavailable.ShopSaturated =>
      LocalizedString(
        "The production floor is too busy to promise this turnaround",
        "Výroba je příliš vytížená, tuto lhůtu nelze slíbit",
      )
    case pri.SpeedUnavailable.Restricted(explanation) => explanation

  private def toDecision(outcome: pri.DiscountOutcome): DiscountDecision = outcome match
    case pri.DiscountOutcome.Applied(code, benefit, _) =>
      DiscountDecision.Applied(code, toBenefit(benefit))
    case pri.DiscountOutcome.Refused(code, reason) => DiscountDecision.Refused(code, reason)

  private def toBenefit(benefit: pri.DiscountBenefit): DiscountBenefit = benefit match
    case pri.DiscountBenefit.Amount(off)  => DiscountBenefit.Amount(off)
    case pri.DiscountBenefit.FreeDelivery => DiscountBenefit.FreeDelivery

  private def toCheckoutError(error: pri.PricingError): CheckoutError = error match
    case pri.PricingError.Rejected(problems)         => CheckoutError.CannotPrice(problems)
    case pri.PricingError.UnknownValue(field, value) => CheckoutError.UnknownValue(field, value)
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

  private def toOwnSpeed(speed: pri.ProductionSpeed): ProductionSpeed = speed match
    case pri.ProductionSpeed.Express  => ProductionSpeed.Express
    case pri.ProductionSpeed.Standard => ProductionSpeed.Standard
    case pri.ProductionSpeed.Economy  => ProductionSpeed.Economy

  private def toPricingSpeed(speed: ProductionSpeed): pri.ProductionSpeed = speed match
    case ProductionSpeed.Express  => pri.ProductionSpeed.Express
    case ProductionSpeed.Standard => pri.ProductionSpeed.Standard
    case ProductionSpeed.Economy  => pri.ProductionSpeed.Economy

  private def toBasketError(error: pri.PricingError): BasketError = error match
    case pri.PricingError.Rejected(problems) => BasketError.Rejected(problems)
    case pri.PricingError.UnknownValue(field, value) => BasketError.UnknownValue(field, value)
    case other =>
      BasketError.Rejected(
        NonEmptyChunk(
          Problem(
            other.toString.takeWhile(_ != '('),
            other.message(Language.En),
            other.message(Language.Cs),
          )
        )
      )

private[orderintake] object PricingQuoteAdapter:
  val layer: URLayer[pri.PricingService, QuotePort] =
    ZLayer.fromFunction((p: pri.PricingService) =>
      new PricingQuoteAdapter(p, Clock.instant.map(i => Timestamp(i.toEpochMilli)))
    )
