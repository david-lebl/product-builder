package mpbuilder.orderintake

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, ZIO}

enum CheckoutError extends DomainError:

  /** There is nothing to check out. Distinct from a missing basket: the basket exists, it is empty. */
  case EmptyBasket

  case ItemNotFound(itemId: String)

  /** A line can no longer be built — a material withdrawn, a rule tightened since it was added. */
  case LineNoLongerAvailable(itemId: String, problems: NonEmptyChunk[Problem])

  /** A line could not be priced at all. A fault, not a choice the customer can correct. */
  case CannotPrice(problems: NonEmptyChunk[Problem])

  case UnknownDelivery(optionId: String)
  case UnknownValue(field: String, value: String)

  /** The chosen payment method is not on offer for this buyer, with the reason they were shown. */
  case PaymentNotOffered(method: PaymentMethod, reason: LocalizedString)

  /** The customer record could not be read. Never the customer's fault. */
  case BuyerLookupFailed(detail: String)

  def message(lang: Language): String = this match
    case EmptyBasket =>
      lang match
        case Language.En => "There is nothing in the basket to order"
        case Language.Cs => "V košíku není nic k objednání"
    case ItemNotFound(id) =>
      lang match
        case Language.En => s"No item '$id' in this basket"
        case Language.Cs => s"Položka '$id' v košíku není"
    case LineNoLongerAvailable(id, problems) =>
      val reasons = problems.map(_.message(lang)).mkString("; ")
      lang match
        case Language.En => s"Item '$id' can no longer be produced: $reasons"
        case Language.Cs => s"Položku '$id' již nelze vyrobit: $reasons"
    case CannotPrice(problems) => problems.map(_.message(lang)).mkString("; ")
    case UnknownDelivery(id) =>
      lang match
        case Language.En => s"No delivery option '$id'"
        case Language.Cs => s"Způsob doručení '$id' neexistuje"
    case UnknownValue(field, value) =>
      lang match
        case Language.En => s"Unsupported value '$value' for $field"
        case Language.Cs => s"Nepodporovaná hodnota '$value' pro $field"
    case PaymentNotOffered(method, reason) =>
      lang match
        case Language.En => s"$method is not available: ${reason(lang)}"
        case Language.Cs => s"$method není k dispozici: ${reason(lang)}"
    case BuyerLookupFailed(detail) =>
      lang match
        case Language.En => s"Your account could not be read: $detail"
        case Language.Cs => s"Váš účet nelze načíst: $detail"

/** The order-intake context's checkout contract: what the basket costs to actually order.
  *
  * Deliberately holds no state of its own. The five-step wizard is client state — the server is
  * asked "what would this cost" as often as the customer changes their mind, and each answer stands
  * alone. That is what keeps a half-finished checkout from being something the server has to store,
  * expire and reconcile.
  */
trait CheckoutService:

  /** What may be chosen: delivery options and payment methods, for this particular buyer. */
  def options(actor: Actor): IO[CheckoutError, CheckoutOptions]

  /** Price the basket as it would be ordered, with the choices made so far.
    *
    * Every line is re-validated and re-quoted. A basket may have been sitting for weeks, and this
    * is the number the customer is about to agree to.
    */
  def quote(actor: Actor, draft: CheckoutDraft): IO[CheckoutError, CheckoutQuote]

  /** Offer a discount code against the current basket.
    *
    * Separate from [[quote]] because the customer types a code and wants an answer about *that*,
    * without having chosen delivery or payment yet.
    */
  def applyDiscount(actor: Actor, code: String): IO[CheckoutError, DiscountDecision]

  /** The speed tiers available for one basket line, priced or explained. */
  def speedOffers(actor: Actor, itemId: String): IO[CheckoutError, List[SpeedOfferView]]

object CheckoutService:
  def options(actor: Actor): ZIO[CheckoutService, CheckoutError, CheckoutOptions] =
    ZIO.serviceWithZIO(_.options(actor))

  def quote(actor: Actor, draft: CheckoutDraft): ZIO[CheckoutService, CheckoutError, CheckoutQuote] =
    ZIO.serviceWithZIO(_.quote(actor, draft))

  def applyDiscount(actor: Actor, code: String): ZIO[CheckoutService, CheckoutError, DiscountDecision] =
    ZIO.serviceWithZIO(_.applyDiscount(actor, code))

  def speedOffers(actor: Actor, itemId: String): ZIO[CheckoutService, CheckoutError, List[SpeedOfferView]] =
    ZIO.serviceWithZIO(_.speedOffers(actor, itemId))
