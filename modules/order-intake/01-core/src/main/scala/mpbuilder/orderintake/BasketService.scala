package mpbuilder.orderintake

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, ZIO}

enum BasketError extends DomainError:

  case NotFound
  case Expired
  case ItemNotFound(itemId: String)
  case TooManyItems(limit: Int)
  case InvalidQuantity(given_ : Int, min: Int, max: Int)

  /** The basket already holds a different currency. Mixing them would make the total meaningless. */
  case MixedCurrency(basket: Currency, item: Currency)

  /** The configuration is no longer buildable — a material withdrawn, a rule tightened — or it
    * could not be priced. Carries every reason, so the customer sees them all at once.
    */
  case Rejected(problems: NonEmptyChunk[Problem])

  case UnknownValue(field: String, value: String)
  case NotAuthorised

  def message(lang: Language): String = this match
    case NotFound =>
      lang match
        case Language.En => "Basket not found"
        case Language.Cs => "Košík nenalezen"
    case Expired =>
      lang match
        case Language.En => "This basket has expired"
        case Language.Cs => "Platnost košíku vypršela"
    case ItemNotFound(id) =>
      lang match
        case Language.En => s"No item '$id' in this basket"
        case Language.Cs => s"Položka '$id' v košíku není"
    case TooManyItems(limit) =>
      lang match
        case Language.En => s"A basket may hold at most $limit items"
        case Language.Cs => s"Košík může obsahovat nejvýše $limit položek"
    case InvalidQuantity(givenQty, min, max) =>
      lang match
        case Language.En => s"Quantity $givenQty is outside the allowed range $min–$max"
        case Language.Cs => s"Množství $givenQty je mimo povolený rozsah $min–$max"
    case MixedCurrency(basket, item) =>
      lang match
        case Language.En => s"This basket is priced in $basket; that item is priced in $item"
        case Language.Cs => s"Košík je v měně $basket; položka je v měně $item"
    case Rejected(problems) => problems.map(_.message(lang)).mkString("; ")
    case UnknownValue(field, value) =>
      lang match
        case Language.En => s"Unsupported value '$value' for $field"
        case Language.Cs => s"Nepodporovaná hodnota '$value' pro $field"
    case NotAuthorised =>
      lang match
        case Language.En => "You are not allowed to do that"
        case Language.Cs => "K této akci nemáte oprávnění"

/** The order-intake context's basket contract.
  *
  * Every operation returns the whole basket rather than an acknowledgement: a client that renders a
  * basket after every change should not have to guess what changed, and a second round-trip to
  * re-read it would be a chance for the two to disagree.
  */
trait BasketService:

  /** The actor's basket, created empty if they have none. */
  def current(actor: Actor): IO[BasketError, BasketView]

  def addItem(actor: Actor, input: AddItem): IO[BasketError, BasketView]

  def updateQuantity(actor: Actor, itemId: String, input: UpdateQuantity): IO[BasketError, BasketView]

  def removeItem(actor: Actor, itemId: String): IO[BasketError, BasketView]

  def clear(actor: Actor): IO[BasketError, BasketView]

  /** Re-price every line against today's prices.
    *
    * Stored quotes are shown to the customer so their total does not shift while they shop, but
    * surge pricing is genuinely time-varying, so the basket has to be able to catch up on demand —
    * and must, before an order is placed.
    */
  def requote(actor: Actor): IO[BasketError, BasketView]

  /** Fold an anonymous basket into the signed-in customer's, on login.
    *
    * Items are deduplicated by product fingerprint: adding the same thing twice, once before and
    * once after signing in, should mean a larger quantity rather than two identical lines.
    */
  def merge(actor: Actor, fromSession: String): IO[BasketError, BasketView]

object BasketService:
  def current(actor: Actor): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.current(actor))

  def addItem(actor: Actor, input: AddItem): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.addItem(actor, input))

  def updateQuantity(
      actor: Actor,
      itemId: String,
      input: UpdateQuantity,
  ): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.updateQuantity(actor, itemId, input))

  def removeItem(actor: Actor, itemId: String): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.removeItem(actor, itemId))

  def clear(actor: Actor): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.clear(actor))

  def requote(actor: Actor): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.requote(actor))

  def merge(actor: Actor, fromSession: String): ZIO[BasketService, BasketError, BasketView] =
    ZIO.serviceWithZIO(_.merge(actor, fromSession))
