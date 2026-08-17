package mpbuilder.pricing

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, UIO, ZIO}

enum PricingError extends DomainError:

  /** The product could not be priced — a missing material price, an unsupported rule combination.
    * Carries every problem found, not just the first.
    */
  case Rejected(problems: NonEmptyChunk[Problem])

  /** The spec payload could not be read. Corrupt stored data, never a user error. */
  case MalformedSpec(detail: String)

  /** No pricelist exists for the requested currency. */
  case NoPricelist(currency: Currency)

  case UnknownValue(field: String, value: String)

  def message(lang: Language): String = this match
    case Rejected(problems) => problems.map(_.message(lang)).mkString("; ")
    case MalformedSpec(detail) =>
      lang match
        case Language.En => s"Product specification could not be read: $detail"
        case Language.Cs => s"Specifikaci produktu nelze přečíst: $detail"
    case NoPricelist(currency) =>
      lang match
        case Language.En => s"No price list is available in $currency"
        case Language.Cs => s"Pro měnu $currency není k dispozici ceník"
    case UnknownValue(field, value) =>
      lang match
        case Language.En => s"Unsupported value '$value' for $field"
        case Language.Cs => s"Nepodporovaná hodnota '$value' pro $field"

/** The pricing context's public contract: what something costs, and what discounts apply.
  *
  * Pricing owns discount codes. That is what stops the system from growing a second, divergent
  * discount implementation: a caller cannot compute a discount itself, because it can only ask for
  * a quote, and a quote already has the discount in it.
  */
trait PricingService:

  /** Price one configured product. */
  def quote(request: QuoteRequest): IO[PricingError, PriceQuote]

  /** Price several lines together. Fails if any single line cannot be priced — a basket total that
    * silently omits a line would be worse than no total.
    */
  def quoteAll(requests: List[QuoteRequest]): IO[PricingError, BasketQuote]

  /** Every speed tier, priced or explained. Never empty: Standard is always offered. */
  def speedOffers(spec: ProductSpec, currency: Currency): IO[PricingError, NonEmptyChunk[SpeedOffer]]

  /** Offer a discount code against a running total. */
  def applyDiscount(code: String, context: DiscountContext): IO[PricingError, DiscountOutcome]

object PricingService:
  def quote(request: QuoteRequest): ZIO[PricingService, PricingError, PriceQuote] =
    ZIO.serviceWithZIO(_.quote(request))

  def quoteAll(requests: List[QuoteRequest]): ZIO[PricingService, PricingError, BasketQuote] =
    ZIO.serviceWithZIO(_.quoteAll(requests))

  def speedOffers(
      spec: ProductSpec,
      currency: Currency,
  ): ZIO[PricingService, PricingError, NonEmptyChunk[SpeedOffer]] =
    ZIO.serviceWithZIO(_.speedOffers(spec, currency))

  def applyDiscount(
      code: String,
      context: DiscountContext,
  ): ZIO[PricingService, PricingError, DiscountOutcome] =
    ZIO.serviceWithZIO(_.applyDiscount(code, context))
