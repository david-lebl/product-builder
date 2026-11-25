package mpbuilder.shared

import kyo.*
import java.time.Instant

/** Marker trait for all cross-context domain events published over the in-process EventBus. */
trait DomainEvent:
  def occurredAt: Instant

/** Minimal in-process domain event bus usable by every bounded context core.
  *
  * Inbound event adapters (`02-i-events`) subscribe to events from other contexts
  * without introducing compile-time dependencies between `01-c-core` modules.
  *
  * Cross-platform (JVM + Scala.js): publishing appends to every live subscription's buffer,
  * so delivery is synchronous, ordered and never blocks the publisher.
  */
trait EventBus:
  def publish(event: DomainEvent)(using Frame): Unit < Sync

  /** Subscribes for the lifetime of the enclosing `Scope`. */
  def subscribe(using Frame): EventBus.Subscription < (Sync & Scope)

object EventBus:

  /** A live subscription buffering every event published after it was opened. */
  final class Subscription private[EventBus] (buffer: AtomicRef[Chunk[DomainEvent]]):
    private[EventBus] def offer(event: DomainEvent)(using Frame): Unit < Sync = buffer.update(_.append(event))

    /** Returns (and removes) every event received so far, oldest first. */
    def drain(using Frame): Chunk[DomainEvent] < Sync = buffer.getAndSet(Chunk.empty)

  def init(using Frame): EventBus < Sync =
    AtomicRef.init(Chunk.empty[Subscription]).map(new InMemoryEventBus(_))

  private final class InMemoryEventBus(subscribers: AtomicRef[Chunk[Subscription]]) extends EventBus:

    override def publish(event: DomainEvent)(using Frame): Unit < Sync =
      subscribers.get.map(subs => Kyo.foreachDiscard(subs)(_.offer(event)))

    override def subscribe(using Frame): Subscription < (Sync & Scope) =
      for
        buffer <- AtomicRef.init(Chunk.empty[DomainEvent])
        sub     = new Subscription(buffer)
        _      <- subscribers.update(_.append(sub))
        _      <- Scope.ensure(subscribers.update(_.filterNot(_ eq sub)))
      yield sub
