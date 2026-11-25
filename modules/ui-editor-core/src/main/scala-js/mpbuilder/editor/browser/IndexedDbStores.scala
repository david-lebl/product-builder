package mpbuilder.editor.browser

import kyo.*
import mpbuilder.design.*
import mpbuilder.editor.*
import mpbuilder.json.DesignCodecs
import mpbuilder.shared.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

/** Browser persistence of the Visual Editor in IndexedDB (database `mpbuilder-design-studio`):
  *
  *   - `designs`: `{ id, json, syncedRevision, pendingPush, updatedAt }` — the [[DesignProject]] as kyo-schema JSON.
  *   - `assets`:  `{ id, blob, mime, name, width, height, size, sha256 }` — original photo bytes (never downscaled:
  *     print needs the full resolution). Content-addressed ids make re-uploads of the same photo free.
  *
  * IndexedDB (not `localStorage`) because photos are many megabytes; it survives reloads, is per-origin and works
  * offline. Unavailable IndexedDB (rare private modes) falls back to an in-memory store for the session.
  */
object IndexedDbStores:

  private val DbName      = "mpbuilder-design-studio"
  private val DbVersion   = 1
  private val DesignStore = "designs"
  private val AssetStore  = "assets"

  private var dbPromise: Option[js.Promise[js.Dynamic]] = None

  def available: Boolean =
    try !js.isUndefined(dom.window.asInstanceOf[js.Dynamic].indexedDB) && dom.window.asInstanceOf[js.Dynamic].indexedDB != null
    catch case _: Throwable => false

  private def db: js.Promise[js.Dynamic] =
    dbPromise.getOrElse {
      val p = new js.Promise[js.Dynamic]((resolve, reject) => {
        val req = dom.window.asInstanceOf[js.Dynamic].indexedDB.open(DbName, DbVersion)
        req.onupgradeneeded = { (_: js.Dynamic) =>
          val d = req.selectDynamic("result")
          if !d.objectStoreNames.contains(DesignStore).asInstanceOf[Boolean] then
            val _ = d.createObjectStore(DesignStore, js.Dynamic.literal(keyPath = "id"))
          if !d.objectStoreNames.contains(AssetStore).asInstanceOf[Boolean] then
            val _ = d.createObjectStore(AssetStore, js.Dynamic.literal(keyPath = "id"))
          ()
        }: js.Function1[js.Dynamic, Unit]
        req.onsuccess = ((_: js.Dynamic) => { resolve(req.selectDynamic("result")); () }): js.Function1[js.Dynamic, Unit]
        req.onerror = ((_: js.Dynamic) => { reject(req.error); () }): js.Function1[js.Dynamic, Unit]
      })
      dbPromise = Some(p)
      p
    }

  private def request[A](store: String, mode: String)(op: js.Dynamic => js.Dynamic): js.Promise[A] =
    db.`then`[A]((d: js.Dynamic) =>
      new js.Promise[A]((resolve, reject) => {
        val tx  = d.transaction(js.Array(store), mode)
        val req = op(tx.objectStore(store))
        req.onsuccess = ((_: js.Dynamic) => { resolve(req.selectDynamic("result").asInstanceOf[A]); () }): js.Function1[js.Dynamic, Unit]
        req.onerror = ((_: js.Dynamic) => { reject(req.error); () }): js.Function1[js.Dynamic, Unit]
      }): js.Thenable[A] | A
    )

  private def await[A](p: => js.Promise[A]): A < Async = Async.fromFuture(p.toFuture)

  // ── Designs ──────────────────────────────────────────────────────────────

  private def decodeRow(row: js.Dynamic): Option[LocalDesign] =
    if row == null || js.isUndefined(row) then None
    else
      DesignCodecs.decodeProject(row.json.asInstanceOf[String]) match
        case Right(project) =>
          val synced = if js.isUndefined(row.syncedRevision) || row.syncedRevision == null then None else Some(row.syncedRevision.asInstanceOf[Double].toLong)
          val pending = !js.isUndefined(row.pendingPush) && row.pendingPush.asInstanceOf[Boolean]
          Some(LocalDesign(project, synced, pending))
        case Left(err) =>
          dom.console.warn(s"Skipping unreadable design ${row.id}: $err")
          None

  val designStore: DesignStore = new DesignStore:
    def list: List[LocalDesign] < Async =
      await(request[js.Array[js.Dynamic]](DesignStore, "readonly")(_.getAll())).map(rows =>
        rows.toList.flatMap(decodeRow).sortBy(_.project.updatedAt).reverse
      )

    def load(id: DesignId): Option[LocalDesign] < Async =
      await(request[js.Dynamic](DesignStore, "readonly")(_.get(id.value))).map(decodeRow)

    def save(design: LocalDesign): Unit < Async =
      val row = js.Dynamic.literal(
        id = design.project.id.value,
        json = DesignCodecs.encodeProject(design.project),
        syncedRevision = design.syncedRevision.map(_.toDouble).fold[js.Any](null)(d => d),
        pendingPush = design.pendingPush,
        updatedAt = design.project.updatedAt.toEpochMilli.toDouble,
      )
      await(request[js.Any](DesignStore, "readwrite")(_.put(row))).unit

    def delete(id: DesignId): Unit < Async =
      await(request[js.Any](DesignStore, "readwrite")(_.delete(id.value))).unit

  // ── Photo blobs ──────────────────────────────────────────────────────────

  private val urlCache = scala.collection.mutable.Map.empty[String, String]

  def putAsset(asset: ImageAsset, blob: dom.Blob): js.Promise[js.Any] =
    request[js.Any](AssetStore, "readwrite")(_.put(js.Dynamic.literal(
      id = asset.id.value,
      blob = blob,
      mime = asset.mimeType,
      name = asset.fileName,
      width = asset.pixelWidth,
      height = asset.pixelHeight,
      size = asset.sizeBytes.toDouble,
      sha256 = asset.sha256.orNull,
    )))

  def objectUrl(id: AssetId, blob: dom.Blob): String =
    urlCache.getOrElseUpdate(id.value, dom.URL.createObjectURL(blob))

  val assetResolver: AssetResolver = new AssetResolver:
    def resolve(asset: ImageAsset): Option[String] < Async =
      asset.storage match
        case AssetStorage.Builtin(url) => Some(url)
        case _ =>
          urlCache.get(asset.id.value) match
            case Some(url) => Some(url)
            case None =>
              await(request[js.Dynamic](AssetStore, "readonly")(_.get(asset.id.value))).map { row =>
                if row == null || js.isUndefined(row) then None
                else Some(objectUrl(asset.id, row.blob.asInstanceOf[dom.Blob]))
              }
