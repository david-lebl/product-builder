package mpbuilder.sample.browser

import kyo.*
import mpbuilder.shared.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

/** Browser [[DocStore]] on IndexedDB: one object store `docs` of `{ c: collection, k: key, json }` rows keyed by
  * `[c, k]`. Used by the static demo SPAs so the sample runtime's state survives page reloads (per origin, so the
  * unified storefront and the standalone `admin/` page share it).
  *
  * Every [[write]] is one `readwrite` transaction, created synchronously when called: IndexedDB runs overlapping
  * transactions in creation order, so writes land in the order the domain made them.
  */
final class IndexedDbDocStore private (db: js.Dynamic) extends DocStore:
  import IndexedDbDocStore.*

  def loadAll(using Frame): Map[String, Map[String, String]] < Async =
    await {
      val tx  = db.transaction(js.Array(StoreName), "readonly")
      request[js.Array[js.Dynamic]](tx.objectStore(StoreName).getAll())
    }.map { rows =>
      rows.toList
        .groupBy(_.c.asInstanceOf[String])
        .view
        .mapValues(_.map(r => r.k.asInstanceOf[String] -> r.json.asInstanceOf[String]).toMap)
        .toMap
    }

  def write(ops: Seq[DocOp])(using Frame): Unit < Async =
    await {
      val tx    = db.transaction(js.Array(StoreName), "readwrite")
      val store = tx.objectStore(StoreName)
      ops.foreach {
        case DocOp.Put(c, k, json)    => store.put(js.Dynamic.literal(c = c, k = k, json = json))
        case DocOp.Delete(c, k)       => store.delete(js.Array(c, k))
        case DocOp.ClearCollection(c) => store.delete(collectionRange(c))
      }
      completion(tx)
    }

  def clearAll(using Frame): Unit < Async =
    await {
      val tx = db.transaction(js.Array(StoreName), "readwrite")
      tx.objectStore(StoreName).clear()
      completion(tx)
    }

object IndexedDbDocStore:

  val DefaultDatabase: String = "mpbuilder-demo"

  private val Version   = 1
  private val StoreName = "docs"

  private def indexedDb: Option[js.Dynamic] =
    try
      val idb = js.Dynamic.global.indexedDB
      if js.isUndefined(idb) || idb == null then None else Some(idb)
    catch case _: Throwable => None

  /** Opens (creating on first use) the demo database; `None` when IndexedDB is unavailable or blocked (some private
    * browsing modes), in which case the demo simply runs in memory.
    */
  def open(name: String = DefaultDatabase)(using Frame): Option[DocStore] < Async =
    indexedDb match
      case None => None
      case Some(idb) =>
        val opened = Abort.run[Throwable](Abort.catching[Throwable](await {
          val req = idb.open(name, Version)
          req.onupgradeneeded = ((_: js.Dynamic) => {
            val d = req.selectDynamic("result")
            if !d.objectStoreNames.contains(StoreName).asInstanceOf[Boolean] then
              val _ = d.createObjectStore(StoreName, js.Dynamic.literal(keyPath = js.Array("c", "k")))
            ()
          }): js.Function1[js.Dynamic, Unit]
          request[js.Dynamic](req)
        }))
        opened.map {
          case Result.Success(db) =>
            // Let a future schema upgrade from another tab proceed instead of blocking on this connection.
            db.onversionchange = ((_: js.Dynamic) => { db.close(); () }): js.Function1[js.Dynamic, Unit]
            Some(new IndexedDbDocStore(db))
          case failure =>
            Log.warn(s"IndexedDB unavailable, demo data will not survive a reload: $failure").andThen(None)
        }

  /** All `[c, *]` keys: arrays sort after strings in IndexedDB, so `[c, []]` is above every `[c, "key"]`. */
  private def collectionRange(c: String): js.Dynamic =
    js.Dynamic.global.IDBKeyRange.bound(js.Array(c), js.Array(c, js.Array()))

  private def request[A](req: js.Dynamic): js.Promise[A] =
    new js.Promise[A]((resolve, reject) => {
      req.onsuccess = ((_: js.Dynamic) => { resolve(req.selectDynamic("result").asInstanceOf[A]); () }): js.Function1[js.Dynamic, Unit]
      req.onerror = ((_: js.Dynamic) => { reject(req.error); () }): js.Function1[js.Dynamic, Unit]
    })

  private def completion(tx: js.Dynamic): js.Promise[Unit] =
    new js.Promise[Unit]((resolve, reject) => {
      tx.oncomplete = ((_: js.Dynamic) => { resolve(()); () }): js.Function1[js.Dynamic, Unit]
      tx.onerror = ((_: js.Dynamic) => { reject(tx.error); () }): js.Function1[js.Dynamic, Unit]
      tx.onabort = ((_: js.Dynamic) => { reject(tx.error); () }): js.Function1[js.Dynamic, Unit]
    })

  private def await[A](p: => js.Promise[A])(using Frame): A < Async =
    Sync.defer(p).map(promise => Async.fromFuture(promise.toFuture))
