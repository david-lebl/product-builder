package mpbuilder.manufacturing

import mpbuilder.commons.*
import mpbuilder.manufacturing.impl.*
import mpbuilder.manufacturing.impl.legacy.LegacyWorkflowAdapter
import mpbuilder.manufacturing.impl.memory.{InMemoryEvents, InMemoryProductionRepository, RandomIds}
import zio.*

/** How the manufacturing context is assembled.
  *
  * It takes no other context as input. Nothing here needs one: work arrives as a snapshot the
  * relay hands over, and what happens on the floor leaves as an event. That absence is the whole
  * point — it is what makes this context movable.
  */
object ManufacturingModule:

  def inMemory: ULayer[ProductionService & ProductionOutbox] =
    ZLayer.fromZIOEnvironment {
      for
        orderStore <- Ref.make(Vector.empty[ProductionOrder])
        eventStore <- Ref.make(Vector.empty[ProductionOutboxEntry])
        eventCounter <- Ref.make(0L)
        outbox = new InMemoryEvents(eventStore, eventCounter)
        service = ProductionServiceLive(
          orders = new InMemoryProductionRepository(orderStore),
          // Workflow planning still runs on the legacy generator. Phase 10 replaces this one
          // class; nothing in `01-core` knows it exists.
          workflows = new LegacyWorkflowAdapter,
          events = outbox,
          ids = RandomIds,
          now = Clock.instant.map(i => Timestamp(i.toEpochMilli)),
        )
      yield ZEnvironment[ProductionService](service).add[ProductionOutbox](outbox)
    }
