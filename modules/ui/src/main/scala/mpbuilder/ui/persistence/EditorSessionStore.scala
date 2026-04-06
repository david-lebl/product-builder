package mpbuilder.ui.persistence

import org.scalajs.dom
import scala.scalajs.js
import mpbuilder.ui.visualeditor.*

/** Typed CRUD for editor sessions backed by IndexedDB.
  * Uses callback-based API consistent with the rest of the UI (no Futures/Promises).
  * Uses js.Dynamic for IndexedDB transaction calls to avoid IDBTransactionMode type issues.
  */
object EditorSessionStore:

  private val DB_NAME = "mpbuilder-editor"
  private val DB_VERSION = 1
  private val SESSIONS_STORE = "sessions"
  private val IMAGES_STORE = "images"

  private var cachedDb: Option[dom.IDBDatabase] = None

  private def withDb(callback: dom.IDBDatabase => Unit): Unit =
    cachedDb match
      case Some(db) => callback(db)
      case None =>
        val idbFactory = dom.window.asInstanceOf[js.Dynamic].indexedDB.asInstanceOf[dom.IDBFactory]
        val request = idbFactory.open(DB_NAME, DB_VERSION)
        request.onupgradeneeded = { event =>
          val db = event.target.asInstanceOf[js.Dynamic].result
          if !db.objectStoreNames.contains(SESSIONS_STORE).asInstanceOf[Boolean] then
            db.createObjectStore(SESSIONS_STORE, js.Dynamic.literal(keyPath = "id"))
          if !db.objectStoreNames.contains(IMAGES_STORE).asInstanceOf[Boolean] then
            db.createObjectStore(IMAGES_STORE, js.Dynamic.literal(keyPath = "id"))
        }
        request.onsuccess = { event =>
          val db = event.target.asInstanceOf[js.Dynamic].result.asInstanceOf[dom.IDBDatabase]
          cachedDb = Some(db)
          callback(db)
        }
        request.onerror = { _ =>
          dom.console.error("Failed to open IndexedDB")
        }

  /** Get an object store via js.Dynamic to avoid IDBTransactionMode type issues */
  private def getStore(db: dom.IDBDatabase, storeName: String, mode: String): dom.IDBObjectStore =
    val dbDyn = db.asInstanceOf[js.Dynamic]
    val tx = dbDyn.transaction(js.Array(storeName), mode)
    tx.objectStore(storeName).asInstanceOf[dom.IDBObjectStore]

  /** Save (create or update) an editor session */
  def save(session: EditorSession, onComplete: () => Unit = () => ()): Unit =
    withDb { db =>
      val store = getStore(db, SESSIONS_STORE, "readwrite")
      val data = Serialization.sessionToJs(session)
      val request = store.put(data)
      request.onsuccess = { _ => onComplete() }
      request.onerror = { _ => dom.console.error("Failed to save session") }
    }

  /** Load a session by ID */
  def load(id: String, onResult: Option[EditorSession] => Unit): Unit =
    withDb { db =>
      val store = getStore(db, SESSIONS_STORE, "readonly")
      val request = store.get(id)
      request.onsuccess = { event =>
        val result = event.target.asInstanceOf[js.Dynamic].result
        if result == null || js.isUndefined(result) then
          onResult(None)
        else
          onResult(Some(Serialization.sessionFromJs(result)))
      }
      request.onerror = { _ => onResult(None) }
    }

  /** List all sessions, sorted by updatedAt descending */
  def listAll(onResult: List[EditorSession] => Unit): Unit =
    withDb { db =>
      val store = getStore(db, SESSIONS_STORE, "readonly")
      val request = store.getAll()
      request.onsuccess = { event =>
        val results = event.target.asInstanceOf[js.Dynamic].result.asInstanceOf[js.Array[js.Dynamic]]
        val sessions = results.toList.map(Serialization.sessionFromJs)
        onResult(sessions.sortBy(-_.updatedAt))
      }
      request.onerror = { _ => onResult(List.empty) }
    }

  /** Delete a session by ID */
  def delete(id: String, onComplete: () => Unit = () => ()): Unit =
    withDb { db =>
      val store = getStore(db, SESSIONS_STORE, "readwrite")
      val request = store.delete(id)
      request.onsuccess = { _ => onComplete() }
      request.onerror = { _ => dom.console.error("Failed to delete session") }
    }

  /** Delete all sessions */
  def deleteAllSessions(onComplete: () => Unit = () => ()): Unit =
    withDb { db =>
      val store = getStore(db, SESSIONS_STORE, "readwrite")
      val request = store.clear()
      request.onsuccess = { _ => onComplete() }
      request.onerror = { _ => dom.console.error("Failed to clear sessions") }
    }

  // ─── Image Gallery CRUD ──────────────────────────────────────────

  /** Save (create or update) a gallery image */
  def saveImage(image: ImageReference, onComplete: () => Unit = () => ()): Unit =
    withDb { db =>
      val store = getStore(db, IMAGES_STORE, "readwrite")
      val data = Serialization.imageReferenceToJs(image)
      val request = store.put(data)
      request.onsuccess = { _ => onComplete() }
      request.onerror = { _ => dom.console.error("Failed to save image") }
    }

  /** Load a gallery image by ID */
  def loadImage(id: String, onResult: Option[ImageReference] => Unit): Unit =
    withDb { db =>
      val store = getStore(db, IMAGES_STORE, "readonly")
      val request = store.get(id)
      request.onsuccess = { event =>
        val result = event.target.asInstanceOf[js.Dynamic].result
        if result == null || js.isUndefined(result) then
          onResult(None)
        else
          onResult(Some(Serialization.imageReferenceFromJs(result)))
      }
      request.onerror = { _ => onResult(None) }
    }

  /** List all gallery images, sorted by addedAt descending */
  def listAllImages(onResult: List[ImageReference] => Unit): Unit =
    withDb { db =>
      val store = getStore(db, IMAGES_STORE, "readonly")
      val request = store.getAll()
      request.onsuccess = { event =>
        val results = event.target.asInstanceOf[js.Dynamic].result.asInstanceOf[js.Array[js.Dynamic]]
        val images = results.toList.map(Serialization.imageReferenceFromJs)
        onResult(images.sortBy(-_.addedAt))
      }
      request.onerror = { _ => onResult(List.empty) }
    }

  /** Delete a gallery image by ID */
  def deleteImage(id: String, onComplete: () => Unit = () => ()): Unit =
    withDb { db =>
      val store = getStore(db, IMAGES_STORE, "readwrite")
      val request = store.delete(id)
      request.onsuccess = { _ => onComplete() }
      request.onerror = { _ => dom.console.error("Failed to delete image") }
    }

  /** Delete all gallery images */
  def deleteAllImages(onComplete: () => Unit = () => ()): Unit =
    withDb { db =>
      val store = getStore(db, IMAGES_STORE, "readwrite")
      val request = store.clear()
      request.onsuccess = { _ => onComplete() }
      request.onerror = { _ => dom.console.error("Failed to clear images") }
    }
