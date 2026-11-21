package mpbuilder.shared

import zio.*
import java.time.Instant

/** Marker trait for all cross-context domain events published over the in-process EventBus. */
trait DomainEvent:
  def occurredAt: Instant

/** Minimal in-process domain event bus usable by every bounded context core.
  *
  * Inbound event adapters (`02-i-events`) subscribe to events from other contexts
  * without introducing compile-time dependencies between `01-c-core` modules.
  */
trait EventBus:
  def publish(event: DomainEvent): UIO[Unit]
  def subscribe: ZIO[Scope, Nothing, Dequeue[DomainEvent]]

object EventBus:
  def publish(event: DomainEvent): ZIO[EventBus, Nothing, Unit] =
    ZIO.serviceWithZIO[EventBus](_.publish(event))

  val live: ULayer[EventBus] =
    ZLayer.fromZIO:
      Hub.unbounded[DomainEvent].map: hub =>
        new EventBus:
          override def publish(event: DomainEvent): UIO[Unit] =
            hub.publish(event).unit
          override def subscribe: ZIO[Scope, Nothing, Dequeue[DomainEvent]] =
            hub.subscribe
