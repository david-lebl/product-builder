package mpbuilder.manufacturing

import mpbuilder.manufacturing.impl.adapters.OrderIntakeAdapter
import mpbuilder.orderintake as oi
import zio.{IO, ZIO}

/** This context's door for things that happened in order-intake.
  *
  * Public because a relay has to be able to knock on it; the translation behind it stays private.
  * That is the difference between a boundary and a suggestion: `app` hands over an event it found
  * in an outbox and gets back what the shop floor made of it, without ever seeing how the two
  * vocabularies line up.
  */
object ManufacturingInbound:

  /** Take an order-intake event.
    *
    * `None` means the event said nothing to the shop floor — an order being paid or cancelled is
    * not work — which is a normal answer, not a failure.
    */
  def accept(
      production: ProductionService,
      event: oi.OrderEvent,
  ): IO[ProductionError, Option[ProductionOrderView]] =
    OrderIntakeAdapter.toRequest(event) match
      case Some(request) => production.accept(request).map(Some(_))
      case None          => ZIO.none
