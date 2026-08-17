package mpbuilder.orderintake
package impl
package adapters

import mpbuilder.commons.*
import mpbuilder.pricing as pri
import zio.*

/** [[QuotePort]] satisfied by calling pricing's public service.
  *
  * Order-intake never computes a price, applies a discount, or reads a pricelist. It asks, and
  * stores the answer with the moment and pricelist version it came from.
  */
private[orderintake] final class PricingQuoteAdapter(
    pricing: pri.PricingService,
    now: UIO[Timestamp],
) extends QuotePort:

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
