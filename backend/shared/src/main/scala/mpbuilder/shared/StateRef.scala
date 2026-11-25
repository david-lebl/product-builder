package mpbuilder.shared

import kyo.*

// ── Mutable state cell for in-memory / browser-persistent adapters ───────────

/** Mutable state cell behind the sample (backend-less) repository adapters.
  *
  * Adapters:
  *   - [[StateRef.init]] — plain `AtomicRef` (tests, JVM `--storage=memory`, calculator widget)
  *   - [[StateRef.persistentMap]] / [[StateRef.persistentValue]] — the same `AtomicRef`, written through
  *     to a [[DocStore]] after every change (browser IndexedDB for the static demo SPAs)
  *
  * Reads never touch the store: the whole state is loaded once at boot and lives in memory.
  */
trait StateRef[A]:
  def get(using Frame): A < Sync
  def updateAndGet(f: A => A)(using Frame): A < Async
  def update(f: A => A)(using Frame): Unit < Async = updateAndGet(f).unit
  def set(value: A)(using Frame): Unit < Async     = update(_ => value)

object StateRef:

  /** Pure in-memory cell. */
  def init[A](initial: A)(using Frame): StateRef[A] < Sync =
    AtomicRef.init(initial).map(new InMemoryStateRef(_))

  /** Cell over a `Map` persisted entry-by-entry: one document per key in `collection`.
    *
    * `loaded = None` means the collection was never written: the cell starts from `initial` (the sample seeds) and
    * its first change writes the complete map, so untouched seeds survive the next boot as well.
    */
  def persistentMap[K, V](
      store: DocStore,
      collection: String,
      loaded: Option[Map[K, V]],
      initial: => Map[K, V],
      encodeKey: K => String,
      encodeValue: V => String,
  )(using Frame): StateRef[Map[K, V]] < Sync =
    for
      ref          <- AtomicRef.init(loaded.getOrElse(initial))
      materialized <- AtomicBoolean.init(loaded.isDefined)
    yield new PersistentStateRef[Map[K, V]](ref, materialized, store) {
      def fullWrite(now: Map[K, V]): Seq[DocOp] =
        DocOp.ClearCollection(collection) +: now.toSeq.map((k, v) => DocOp.Put(collection, encodeKey(k), encodeValue(v)))
      def diffWrite(old: Map[K, V], now: Map[K, V]): Seq[DocOp] =
        val puts = now.toSeq.collect {
          case (k, v) if !old.get(k).exists(o => (o.asInstanceOf[AnyRef] eq v.asInstanceOf[AnyRef]) || o == v) =>
            DocOp.Put(collection, encodeKey(k), encodeValue(v))
        }
        val deletes = (old.keySet -- now.keySet).toSeq.map(k => DocOp.Delete(collection, encodeKey(k)))
        puts ++ deletes
      def markerCollection: String = collection
    }

  /** Cell persisted as a single document (`collection/value`). */
  def persistentValue[A](
      store: DocStore,
      collection: String,
      loaded: Option[A],
      initial: => A,
      encode: A => String,
  )(using Frame): StateRef[A] < Sync =
    for
      ref          <- AtomicRef.init(loaded.getOrElse(initial))
      materialized <- AtomicBoolean.init(loaded.isDefined)
    yield new PersistentStateRef[A](ref, materialized, store) {
      def fullWrite(now: A): Seq[DocOp] = Seq(DocOp.Put(collection, StateRef.ValueKey, encode(now)))
      def diffWrite(old: A, now: A): Seq[DocOp] =
        if (old.asInstanceOf[AnyRef] eq now.asInstanceOf[AnyRef]) || old == now then Nil else fullWrite(now)
      def markerCollection: String = collection
    }

  /** Document key of a [[persistentValue]] cell. */
  val ValueKey: String = "value"

  /** Reserved collection recording which collections were materialized (key = collection name). */
  val MetaCollection: String = "__meta"

final class InMemoryStateRef[A](ref: AtomicRef[A]) extends StateRef[A]:
  def get(using Frame): A < Sync                       = ref.get
  def updateAndGet(f: A => A)(using Frame): A < Async = ref.updateAndGet(f)

/** Write-through cell: memory first (atomic), then the minimal [[DocOp]]s for the change in one store batch. */
abstract class PersistentStateRef[A](
    ref: AtomicRef[A],
    materialized: AtomicBoolean,
    store: DocStore,
) extends StateRef[A]:

  protected def fullWrite(now: A): Seq[DocOp]
  protected def diffWrite(old: A, now: A): Seq[DocOp]
  protected def markerCollection: String

  def get(using Frame): A < Sync = ref.get

  def updateAndGet(f: A => A)(using Frame): A < Async =
    for
      change <- ref.modify(old => { val now = f(old); ((old, now), now) })
      wasMat <- materialized.getAndSet(true)
      (old, now) = change
      ops = if wasMat then diffWrite(old, now)
            else fullWrite(now) :+ DocOp.Put(StateRef.MetaCollection, markerCollection, "1")
      _ <- (if ops.isEmpty then Kyo.unit else persist(ops)): Unit < Async
    yield now

  /** A failed write never fails the domain operation (memory stays authoritative for this session); the next change
    * rewrites the whole state so the store catches up.
    */
  private def persist(ops: Seq[DocOp])(using Frame): Unit < Async =
    Abort.run[Throwable](Abort.catching[Throwable](store.write(ops))).map {
      case Result.Success(_) => Kyo.unit
      case failure =>
        materialized.set(false).andThen(Log.warn(s"State persistence failed for '$markerCollection': $failure"))
    }

// ── Document store port ──────────────────────────────────────────────────────

/** One mutation of a [[DocStore]]; a batch of them is applied atomically. */
enum DocOp:
  case Put(collection: String, key: String, json: String)
  case Delete(collection: String, key: String)
  case ClearCollection(collection: String)

/** Minimal key/value document store (`collection` → `key` → JSON) for persisting the backend-less demo state.
  *
  * Adapters:
  *   - [[DocStore.inMemory]] — cross-platform, for tests
  *   - `mpbuilder.sample.browser.IndexedDbDocStore` — browser IndexedDB (static demo SPAs)
  */
trait DocStore:
  /** Every document, grouped by collection. */
  def loadAll(using Frame): Map[String, Map[String, String]] < Async
  def write(ops: Seq[DocOp])(using Frame): Unit < Async
  def clearAll(using Frame): Unit < Async

object DocStore:

  def inMemory(using Frame): InMemoryDocStore < Sync =
    AtomicRef.init(Map.empty[String, Map[String, String]]).map(new InMemoryDocStore(_))

  /** Applies a batch to a snapshot (shared by adapters that keep an in-memory mirror). */
  def applyOps(state: Map[String, Map[String, String]], ops: Seq[DocOp]): Map[String, Map[String, String]] =
    ops.foldLeft(state) {
      case (s, DocOp.Put(c, k, json))      => s.updated(c, s.getOrElse(c, Map.empty).updated(k, json))
      case (s, DocOp.Delete(c, k))         => s.updatedWith(c)(_.map(_ - k))
      case (s, DocOp.ClearCollection(c))   => s - c
    }

final class InMemoryDocStore(ref: AtomicRef[Map[String, Map[String, String]]]) extends DocStore:
  def loadAll(using Frame): Map[String, Map[String, String]] < Async = ref.get
  def write(ops: Seq[DocOp])(using Frame): Unit < Async               = ref.updateAndGet(DocStore.applyOps(_, ops)).unit
  def clearAll(using Frame): Unit < Async                             = ref.set(Map.empty)
