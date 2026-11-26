package mpbuilder.sample

import kyo.*
import mpbuilder.json.JsonSupport
import mpbuilder.shared.*

/** Where the sample (backend-less) runtime keeps the state of its repositories.
  *
  *   - [[SampleStorage.memory]] — fresh sample seeds on every start (tests, `--storage=memory`, calculator widget).
  *   - [[SampleStorage.load]] — state restored from a [[DocStore]] (browser IndexedDB) and written through on every
  *     change, so orders, customers, catalog/pricing edits… survive a page reload of the static demo.
  *
  * Collections that were never changed are not stored at all: they start from the sample seeds, and their first
  * change writes them completely. Documents are the kyo-schema JSON of the domain aggregates (`backend/json`, the
  * same encoding as the PostgreSQL JSONB rows); a collection that no longer decodes falls back to its seeds.
  */
final class SampleStorage private (
    store: Option[DocStore],
    snapshot: Map[String, Map[String, String]],
):

  def isPersistent: Boolean = store.isDefined

  /** A `Map` cell stored as one document per entry. */
  def map[K, V](collection: String, initial: => Map[K, V])(encodeKey: K => String, decodeKey: String => K)(using
      Schema[V],
      Frame,
  ): StateRef[Map[K, V]] < Sync =
    store match
      case None => StateRef.init(initial)
      case Some(s) =>
        val loaded = stored(collection).map { docs =>
          decodeAll(collection, docs.toList) { (k, json) =>
            JsonSupport.fromJson[V](json).flatMap(v => attempt(decodeKey(k)).map(_ -> v))
          }.map(_.toMap)
        }
        warnOnFailure(collection, loaded).map { ok =>
          StateRef.persistentMap(s, collection, ok, initial, encodeKey, JsonSupport.toJson[V](_))
        }

  /** A cell stored as a single document. */
  def value[A](collection: String, initial: => A)(using Schema[A], Frame): StateRef[A] < Sync =
    store match
      case None => StateRef.init(initial)
      case Some(s) =>
        val loaded = stored(collection).map { docs =>
          docs.get(StateRef.ValueKey) match
            case Some(json) => JsonSupport.fromJson[A](json)
            case None       => Left("missing value document")
        }
        warnOnFailure(collection, loaded).map { ok =>
          StateRef.persistentValue(s, collection, ok, initial, JsonSupport.toJson[A](_))
        }

  /** Monotonic counter (e.g. for [[IdSequence]]) that keeps counting across reloads. */
  def counter(collection: String, start: Long)(using Frame): StateRef[Long] < Sync =
    value[Long](collection, start)

  /** Documents of a collection that was materialized before, `None` when it still holds the seeds. */
  private def stored(collection: String): Option[Map[String, String]] =
    if snapshot.get(StateRef.MetaCollection).exists(_.contains(collection)) then
      Some(snapshot.getOrElse(collection, Map.empty))
    else None

  private def attempt[A](a: => A): Either[String, A] =
    try Right(a)
    catch case e: Throwable => Left(String.valueOf(e.getMessage))

  private def decodeAll[A](collection: String, docs: List[(String, String)])(
      f: (String, String) => Either[String, A]
  ): Either[String, List[A]] =
    docs.foldLeft[Either[String, List[A]]](Right(Nil)) { (acc, kv) =>
      acc.flatMap(list => f(kv._1, kv._2).left.map(err => s"'${kv._1}': $err").map(_ :: list))
    }

  private def warnOnFailure[A](collection: String, loaded: Option[Either[String, A]])(using Frame): Option[A] < Sync =
    loaded match
      case Some(Left(err)) =>
        Log.warn(s"Stored '$collection' is unreadable ($err); starting from the sample data").andThen(None)
      case Some(Right(a)) => Some(a)
      case None           => None

object SampleStorage:

  /** Bump when stored documents become incompatible: every browser then starts again from the sample seeds. */
  val FormatVersion: String = "1"

  private val FormatVersionKey = "formatVersion"

  val memory: SampleStorage = new SampleStorage(None, Map.empty)

  /** Loads every stored document (one read at boot); an outdated or missing format version wipes the store. */
  def load(store: DocStore)(using Frame): SampleStorage < Async =
    store.loadAll.map { all =>
      if all.get(StateRef.MetaCollection).flatMap(_.get(FormatVersionKey)).contains(FormatVersion) then
        new SampleStorage(Some(store), all)
      else
        reset(store).andThen(new SampleStorage(Some(store), Map.empty))
    }

  /** Forgets every stored change: the next start uses the sample seeds again. */
  def reset(store: DocStore)(using Frame): Unit < Async =
    store.clearAll.andThen(store.write(Seq(DocOp.Put(StateRef.MetaCollection, FormatVersionKey, FormatVersion))))
