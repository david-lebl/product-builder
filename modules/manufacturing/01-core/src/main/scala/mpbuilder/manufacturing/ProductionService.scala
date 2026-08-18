package mpbuilder.manufacturing

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, ZIO}

enum ProductionError extends DomainError:

  case NotFound(id: String)

  /** The work cannot be planned — a spec this shop cannot read, a product with no steps. */
  case CannotPlan(problems: NonEmptyChunk[Problem])

  case AlreadyApproved(id: String)
  case AlreadyDispatched(id: String)

  /** Dispatching something nobody approved would ship goods the shop never agreed to make. */
  case NotApproved(id: String)

  def message(lang: Language): String = this match
    case NotFound(id) =>
      lang match
        case Language.En => s"No production order '$id'"
        case Language.Cs => s"Výrobní zakázka '$id' neexistuje"
    case CannotPlan(problems) => problems.map(_.message(lang)).mkString("; ")
    case AlreadyApproved(id) =>
      lang match
        case Language.En => s"Production order '$id' is already approved"
        case Language.Cs => s"Výrobní zakázka '$id' je již schválena"
    case AlreadyDispatched(id) =>
      lang match
        case Language.En => s"Production order '$id' has already been dispatched"
        case Language.Cs => s"Výrobní zakázka '$id' již byla expedována"
    case NotApproved(id) =>
      lang match
        case Language.En => s"Production order '$id' has not been approved yet"
        case Language.Cs => s"Výrobní zakázka '$id' zatím nebyla schválena"

/** The manufacturing context's public contract: what the shop floor has been asked to make.
  *
  * It is told, never asked. Nothing here reaches back into order-intake — a request arrives as a
  * snapshot, and what happens next is reported as an event.
  */
trait ProductionService:

  /** Take an order onto the floor, planning the work for every line.
    *
    * Idempotent in the order id. An outbox delivers at least once, and the shop must not end up
    * making the same order twice because a relay retried.
    */
  def accept(request: ProductionRequest): IO[ProductionError, ProductionOrderView]

  /** Everything waiting for a decision, oldest first — it is a queue. */
  def queue: IO[ProductionError, List[ProductionOrderView]]

  def find(id: String): IO[ProductionError, ProductionOrderView]

  /** Authorise production. This is what tells order-intake the order is being made. */
  def approve(id: String, by: String): IO[ProductionError, ProductionOrderView]

  /** The goods have left. */
  def dispatch(id: String, tracking: Option[String]): IO[ProductionError, ProductionOrderView]

object ProductionService:
  def accept(request: ProductionRequest): ZIO[ProductionService, ProductionError, ProductionOrderView] =
    ZIO.serviceWithZIO(_.accept(request))

  def queue: ZIO[ProductionService, ProductionError, List[ProductionOrderView]] =
    ZIO.serviceWithZIO(_.queue)

  def find(id: String): ZIO[ProductionService, ProductionError, ProductionOrderView] =
    ZIO.serviceWithZIO(_.find(id))

  def approve(id: String, by: String): ZIO[ProductionService, ProductionError, ProductionOrderView] =
    ZIO.serviceWithZIO(_.approve(id, by))

  def dispatch(
      id: String,
      tracking: Option[String],
  ): ZIO[ProductionService, ProductionError, ProductionOrderView] =
    ZIO.serviceWithZIO(_.dispatch(id, tracking))

/** The events this context has produced and not yet handed on.
  *
  * Public because the relay in `app` drains it. An outbox rather than a direct call, for the same
  * reason order-intake has one: a subscriber that is down must not lose what happened, and must not
  * be able to fail the operation that caused it.
  */
final case class ProductionOutboxEntry(id: Long, event: ProductionEvent)

trait ProductionOutbox:
  def pending(limit: Int): zio.UIO[List[ProductionOutboxEntry]]
  def markPublished(ids: List[Long]): zio.UIO[Unit]
