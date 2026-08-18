package mpbuilder.orderintake

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, ZIO}

enum OrderError extends DomainError:

  case NotFound(id: String)

  /** There is nothing to order. */
  case EmptyBasket

  /** The details the customer gave are not usable, with *every* problem found. */
  case Invalid(problems: NonEmptyChunk[Problem])

  /** The basket's price moved between being shown and being ordered.
    *
    * A refusal rather than a silent charge: surge pricing is real here, and the customer agreed to
    * a number. They confirm the new one and try again.
    */
  case PriceChanged(was: Money, now: Money)

  /** The checkout refused for a reason of its own — a line that can no longer be built, a payment
    * method this buyer may not use. Carried through rather than reworded.
    */
  case CheckoutRefused(underlying: CheckoutError)

  /** The transition is not legal from where the order is now. */
  case IllegalTransition(from: String, to: String)

  /** The order is finished; nothing more will happen to it. */
  case AlreadyClosed(status: String)

  /** Only staff may do this. */
  case StaffOnly

  case NotAuthorised

  def message(lang: Language): String = this match
    case NotFound(id) =>
      lang match
        case Language.En => s"No order '$id'"
        case Language.Cs => s"Objednávka '$id' neexistuje"
    case EmptyBasket =>
      lang match
        case Language.En => "There is nothing in the basket to order"
        case Language.Cs => "V košíku není nic k objednání"
    case Invalid(problems) => problems.map(_.message(lang)).mkString("; ")
    case PriceChanged(was, now) =>
      lang match
        case Language.En =>
          s"The price has changed from ${was.value} to ${now.value} since you were quoted"
        case Language.Cs =>
          s"Cena se od zobrazení nabídky změnila z ${was.value} na ${now.value}"
    case CheckoutRefused(underlying) => underlying.message(lang)
    case IllegalTransition(from, to) =>
      lang match
        case Language.En => s"An order that is '$from' cannot become '$to'"
        case Language.Cs => s"Objednávku ve stavu '$from' nelze převést na '$to'"
    case AlreadyClosed(status) =>
      lang match
        case Language.En => s"This order is already closed ($status)"
        case Language.Cs => s"Tato objednávka je již uzavřena ($status)"
    case StaffOnly =>
      lang match
        case Language.En => "Only staff may do that"
        case Language.Cs => "K této akci je oprávněn pouze personál"
    case NotAuthorised =>
      lang match
        case Language.En => "You are not allowed to do that"
        case Language.Cs => "K této akci nemáte oprávnění"

/** The order-intake context's order contract.
  *
  * This is the first thing in the system that writes something a customer can be held to, so its
  * two obligations are: the order says exactly what was agreed, and it can only move between states
  * the business actually allows.
  */
trait OrderService:

  /** Turn the current basket into an order.
    *
    * Re-prices and re-validates everything first — the basket may have been sitting for weeks —
    * and empties the basket once the order exists, so the same goods cannot be ordered twice by
    * pressing the button again.
    */
  def place(actor: Actor, input: PlaceOrder): IO[OrderError, OrderView]

  /** One order, if this actor may see it. */
  def find(actor: Actor, id: String): IO[OrderError, OrderView]

  /** The actor's own orders, newest first. */
  def list(actor: Actor, page: Page): IO[OrderError, Paged[OrderSummaryView]]

  /** Every order, for staff. Filters on the status group, never on a leaf. */
  def search(actor: Actor, criteria: OrderSearch, page: Page): IO[OrderError, Paged[OrderSummaryView]]

  /** Customer or staff, while the order is still open; staff only once it is active. */
  def cancel(actor: Actor, id: String, input: CancelOrder): IO[OrderError, OrderView]

  /** Staff record that money arrived. */
  def confirmPayment(actor: Actor, id: String, input: ConfirmPayment): IO[OrderError, OrderView]

  /** Staff refuse an order, with a reason the customer will be shown. */
  def reject(actor: Actor, id: String, input: RejectOrder): IO[OrderError, OrderView]

object OrderService:
  def place(actor: Actor, input: PlaceOrder): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO(_.place(actor, input))

  def find(actor: Actor, id: String): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO(_.find(actor, id))

  def list(actor: Actor, page: Page): ZIO[OrderService, OrderError, Paged[OrderSummaryView]] =
    ZIO.serviceWithZIO(_.list(actor, page))

  def search(
      actor: Actor,
      criteria: OrderSearch,
      page: Page,
  ): ZIO[OrderService, OrderError, Paged[OrderSummaryView]] =
    ZIO.serviceWithZIO(_.search(actor, criteria, page))

  def cancel(actor: Actor, id: String, input: CancelOrder): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO(_.cancel(actor, id, input))

  def confirmPayment(
      actor: Actor,
      id: String,
      input: ConfirmPayment,
  ): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO(_.confirmPayment(actor, id, input))

  def reject(actor: Actor, id: String, input: RejectOrder): ZIO[OrderService, OrderError, OrderView] =
    ZIO.serviceWithZIO(_.reject(actor, id, input))
