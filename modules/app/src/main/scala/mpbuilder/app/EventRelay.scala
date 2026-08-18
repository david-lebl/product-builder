package mpbuilder.app

import mpbuilder.manufacturing as mf
import mpbuilder.orderintake as oi
import zio.*

/** Carrying events between contexts.
  *
  * Neither side knows the other exists. Order-intake writes to its outbox and forgets; manufacturing
  * is handed work and reports back to its own. This file is the only thing that has both in scope,
  * which is what makes either of them extractable: replacing it with Kafka changes nothing but this
  * file.
  *
  * Delivery is at-least-once. An entry is removed only after the subscriber has taken it, so a
  * relay that dies mid-batch re-delivers rather than loses — and every consumer here is idempotent
  * for exactly that reason.
  */
object EventRelay:

  /** How long to wait before looking again when there was nothing to do.
    *
    * Polling rather than pushing because an outbox is a table: the eventual Postgres version is
    * polled the same way, and building on a push here would be building on something that has to
    * be taken out again.
    */
  private val idle = 200.millis

  /** How many entries to take at once. Small, because a batch is re-delivered whole if the relay
    * dies part-way through it.
    */
  private val batch = 32

  def run: ZIO[
    oi.OrderOutbox & oi.OrderLifecycle & mf.ProductionService & mf.ProductionOutbox,
    Nothing,
    Unit,
  ] =
    for
      _ <- pump(toManufacturing).forever.forkDaemon
      _ <- pump(toOrderIntake).forever.forkDaemon
      _ <- ZIO.logInfo("Event relay running: order-intake ⇄ manufacturing")
    yield ()

  private def pump[R](step: ZIO[R, Nothing, Int]): ZIO[R, Nothing, Unit] =
    step.flatMap(handled => ZIO.sleep(idle).when(handled == 0)).unit

  /** Order-intake → manufacturing: an order was placed, so plan the work. */
  private val toManufacturing: ZIO[oi.OrderOutbox & mf.ProductionService, Nothing, Int] =
    for
      outbox <- ZIO.service[oi.OrderOutbox]
      production <- ZIO.service[mf.ProductionService]
      entries <- outbox.pending(batch)
      _ <- ZIO.foreachDiscard(entries) { entry =>
        // Manufacturing decides what an order-intake event means to it — including that most of
        // them mean nothing. Payment and cancellation say nothing to the shop floor yet.
        mf.ManufacturingInbound
          .accept(production, entry.event)
          .foldZIO(
            // A job the shop cannot plan must not block every job behind it. It is logged and the
            // entry is dropped; without this, one unreadable spec stops the queue forever.
            error => ZIO.logError(s"Production refused ${entry.event.number}: ${error.message}"),
            {
              case Some(accepted) =>
                ZIO.logInfo(s"Order ${entry.event.number} queued for production as ${accepted.id}")
              case None => ZIO.unit
            },
          )
      }
      _ <- outbox.markPublished(entries.map(_.id)).when(entries.nonEmpty)
    yield entries.size

  /** Manufacturing → order-intake: work was approved or shipped, so move the order. */
  private val toOrderIntake: ZIO[mf.ProductionOutbox & oi.OrderLifecycle, Nothing, Int] =
    for
      outbox <- ZIO.service[mf.ProductionOutbox]
      orders <- ZIO.service[oi.OrderLifecycle]
      entries <- outbox.pending(batch)
      _ <- ZIO.foreachDiscard(entries) { entry =>
        val applied = entry.event match
          case approved: mf.ProductionEvent.OrderApproved =>
            orders.productionStarted(approved.orderId)
          case dispatched: mf.ProductionEvent.OrderDispatched =>
            orders.dispatched(dispatched.orderId, dispatched.tracking)

        applied.catchAll(error =>
          // The order refused the move — already cancelled, say. That is the order's business and
          // not a reason to stop the relay or to keep retrying forever.
          ZIO.logWarning(s"Order ${entry.event.orderId} did not accept the update: ${error.message}")
        )
      }
      _ <- outbox.markPublished(entries.map(_.id)).when(entries.nonEmpty)
    yield entries.size
