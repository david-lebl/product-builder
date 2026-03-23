package mpbuilder.ui.calendar

import scala.scalajs.js
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom

/** IndexedDB-backed storage for editor sessions */
object EditorSessionStore:

  private val DbName = "mpbuilder-editor"
  private val DbVersion = 1
  private val StoreName = "sessions"

  private var cachedDb: Option[js.Dynamic] = None

  // ─── Database lifecycle ──────────────────────────────────────────

  private def openDb(): Future[js.Dynamic] =
    cachedDb match
      case Some(db) => Future.successful(db)
      case None =>
        val p = Promise[js.Dynamic]()
        val factory = dom.window.asInstanceOf[js.Dynamic].indexedDB
        val request = factory.open(DbName, DbVersion)

        request.onupgradeneeded = { (_: js.Any) =>
          val db = request.result
          val storeNames = db.objectStoreNames.asInstanceOf[js.Dynamic]
          val hasStore = storeNames.contains(StoreName).asInstanceOf[Boolean]
          if !hasStore then
            db.createObjectStore(StoreName, js.Dynamic.literal("keyPath" -> "id"))
        }

        request.onsuccess = { (_: js.Any) =>
          val db = request.result
          cachedDb = Some(db)
          p.success(db)
        }

        request.onerror = { (_: js.Any) =>
          p.failure(new Exception("Failed to open IndexedDB"))
        }

        p.future

  private def withStore(mode: String)(fn: js.Dynamic => Future[Unit]): Future[Unit] =
    openDb().flatMap { db =>
      val tx = db.transaction(StoreName, mode)
      val store = tx.objectStore(StoreName)
      fn(store)
    }

  // ─── CRUD operations ─────────────────────────────────────────────

  /** Save (insert or update) a session */
  def save(session: EditorSession): Future[Unit] =
    openDb().flatMap { db =>
      val p = Promise[Unit]()
      val tx = db.transaction(StoreName, "readwrite")
      val store = tx.objectStore(StoreName)
      val jsObj = SessionCodec.sessionToJs(session)
      val request = store.put(jsObj)
      request.onsuccess = { (_: js.Any) => p.success(()) }
      request.onerror = { (_: js.Any) => p.failure(new Exception("Failed to save session")) }
      p.future
    }

  /** Load a session by ID */
  def load(id: String): Future[Option[EditorSession]] =
    openDb().flatMap { db =>
      val p = Promise[Option[EditorSession]]()
      val tx = db.transaction(StoreName, "readonly")
      val store = tx.objectStore(StoreName)
      val request = store.get(id)
      request.onsuccess = { (_: js.Any) =>
        val result = request.result
        if result == null || js.isUndefined(result) then
          p.success(None)
        else
          try
            p.success(Some(SessionCodec.sessionFromJs(result.asInstanceOf[js.Dynamic])))
          catch
            case e: Exception => p.failure(e)
      }
      request.onerror = { (_: js.Any) => p.failure(new Exception("Failed to load session")) }
      p.future
    }

  /** Delete a session by ID */
  def delete(id: String): Future[Unit] =
    openDb().flatMap { db =>
      val p = Promise[Unit]()
      val tx = db.transaction(StoreName, "readwrite")
      val store = tx.objectStore(StoreName)
      val request = store.delete(id)
      request.onsuccess = { (_: js.Any) => p.success(()) }
      request.onerror = { (_: js.Any) => p.failure(new Exception("Failed to delete session")) }
      p.future
    }

  /** List all session summaries, sorted by updatedAt descending */
  def listSummaries(): Future[List[SessionSummary]] =
    openDb().flatMap { db =>
      val p = Promise[List[SessionSummary]]()
      val tx = db.transaction(StoreName, "readonly")
      val store = tx.objectStore(StoreName)
      val request = store.getAll()
      request.onsuccess = { (_: js.Any) =>
        try
          val results = request.result.asInstanceOf[js.Array[js.Dynamic]]
          val summaries = results.toList
            .map(SessionCodec.summaryFromJs)
            .sortBy(-_.updatedAt)
          p.success(summaries)
        catch
          case e: Exception => p.failure(e)
      }
      request.onerror = { (_: js.Any) => p.failure(new Exception("Failed to list sessions")) }
      p.future
    }
