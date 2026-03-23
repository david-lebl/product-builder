package mpbuilder.ui.calendar

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom

/** IndexedDB store for gallery images (metadata + blobs) */
object ImageStore:

  private val DbName = "mpbuilder-gallery"
  private val DbVersion = 1
  private val MetaStore = "images"
  private val BlobStore = "blobs"

  private var cachedDb: Option[js.Dynamic] = None

  private def openDb(): Future[js.Dynamic] =
    cachedDb match
      case Some(db) => Future.successful(db)
      case None =>
        val p = Promise[js.Dynamic]()
        val factory = dom.window.asInstanceOf[js.Dynamic].indexedDB
        val request = factory.open(DbName, DbVersion)

        request.onupgradeneeded = { (_: js.Any) =>
          val db = request.result
          val storeNames = db.objectStoreNames
          if !storeNames.contains(MetaStore).asInstanceOf[Boolean] then
            db.createObjectStore(MetaStore, js.Dynamic.literal("keyPath" -> "id"))
          if !storeNames.contains(BlobStore).asInstanceOf[Boolean] then
            db.createObjectStore(BlobStore, js.Dynamic.literal("keyPath" -> "id"))
        }

        request.onsuccess = { (_: js.Any) =>
          val db = request.result
          cachedDb = Some(db)
          p.success(db)
        }

        request.onerror = { (_: js.Any) =>
          p.failure(new Exception("Failed to open gallery IndexedDB"))
        }

        p.future

  // ─── Gallery CRUD ─────────────────────────────────────────────

  /** Add an image from a File object. Generates thumbnail, stores blob + metadata. */
  def addImage(file: dom.File): Future[GalleryImage] =
    for
      dataUrl  <- readFileAsDataUrl(file)
      dims     <- getImageDimensions(dataUrl)
      thumb    <- generateThumbnail(dataUrl, 120, 120)
      gallery  <- storeImage(file, dataUrl, dims, thumb)
    yield gallery

  /** List all gallery image metadata, sorted by addedAt descending */
  def listImages(): Future[List[GalleryImage]] =
    openDb().flatMap { db =>
      val p = Promise[List[GalleryImage]]()
      val tx = db.transaction(MetaStore, "readonly")
      val store = tx.objectStore(MetaStore)
      val request = store.getAll()
      request.onsuccess = { (_: js.Any) =>
        try
          val results = request.result.asInstanceOf[js.Array[js.Dynamic]]
          val images = results.toList.map(galleryFromJs).sortBy(-_.addedAt)
          p.success(images)
        catch case e: Exception => p.failure(e)
      }
      request.onerror = { (_: js.Any) => p.failure(new Exception("Failed to list images")) }
      p.future
    }

  /** Load image blob as a data URL for rendering */
  def loadImageDataUrl(imageId: String): Future[Option[String]] =
    openDb().flatMap { db =>
      val p = Promise[Option[String]]()
      val tx = db.transaction(BlobStore, "readonly")
      val store = tx.objectStore(BlobStore)
      val request = store.get(imageId)
      request.onsuccess = { (_: js.Any) =>
        val result = request.result
        if result == null || js.isUndefined(result) then p.success(None)
        else p.success(Some(result.asInstanceOf[js.Dynamic].dataUrl.asInstanceOf[String]))
      }
      request.onerror = { (_: js.Any) => p.failure(new Exception("Failed to load image blob")) }
      p.future
    }

  /** Delete image (metadata + blob) */
  def deleteImage(imageId: String): Future[Unit] =
    openDb().flatMap { db =>
      val p1 = Promise[Unit]()
      val p2 = Promise[Unit]()
      val tx = db.transaction(js.Array(MetaStore, BlobStore), "readwrite")
      val metaReq = tx.objectStore(MetaStore).delete(imageId)
      val blobReq = tx.objectStore(BlobStore).delete(imageId)
      metaReq.onsuccess = { (_: js.Any) => p1.success(()) }
      metaReq.onerror = { (_: js.Any) => p1.failure(new Exception("Failed to delete image meta")) }
      blobReq.onsuccess = { (_: js.Any) => p2.success(()) }
      blobReq.onerror = { (_: js.Any) => p2.failure(new Exception("Failed to delete image blob")) }
      for _ <- p1.future; _ <- p2.future yield ()
    }

  // ─── Internal helpers ─────────────────────────────────────────

  private def storeImage(
    file: dom.File,
    dataUrl: String,
    dims: (Int, Int),
    thumbnailDataUrl: String,
  ): Future[GalleryImage] =
    val id = s"img-${System.currentTimeMillis()}-${(Math.random() * 1000000).toInt}"
    val now = System.currentTimeMillis().toDouble
    val gallery = GalleryImage(
      id = id,
      name = file.name,
      thumbnailDataUrl = thumbnailDataUrl,
      width = dims._1,
      height = dims._2,
      addedAt = now,
      sizeBytes = file.size.toLong,
    )

    openDb().flatMap { db =>
      val p = Promise[GalleryImage]()
      val tx = db.transaction(js.Array(MetaStore, BlobStore), "readwrite")

      // Store metadata
      val metaObj = galleryToJs(gallery)
      tx.objectStore(MetaStore).put(metaObj)

      // Store blob as data URL string
      val blobObj = js.Dynamic.literal("id" -> id, "dataUrl" -> dataUrl)
      val blobReq = tx.objectStore(BlobStore).put(blobObj)

      blobReq.onsuccess = { (_: js.Any) => p.success(gallery) }
      blobReq.onerror = { (_: js.Any) => p.failure(new Exception("Failed to store image")) }
      p.future
    }

  private def readFileAsDataUrl(file: dom.File): Future[String] =
    val p = Promise[String]()
    val reader = new dom.FileReader()
    reader.onload = { _ =>
      p.success(reader.result.asInstanceOf[String])
    }
    reader.onerror = { _ =>
      p.failure(new Exception("Failed to read file"))
    }
    reader.readAsDataURL(file)
    p.future

  private def getImageDimensions(dataUrl: String): Future[(Int, Int)] =
    val p = Promise[(Int, Int)]()
    val img = dom.document.createElement("img").asInstanceOf[dom.html.Image]
    img.onload = { _ =>
      p.success((img.naturalWidth, img.naturalHeight))
    }
    img.addEventListener("error", { (_: dom.Event) =>
      p.success((0, 0))
    })
    img.src = dataUrl
    p.future

  private def generateThumbnail(dataUrl: String, maxW: Int, maxH: Int): Future[String] =
    val p = Promise[String]()
    val img = dom.document.createElement("img").asInstanceOf[dom.html.Image]
    img.onload = { _ =>
      val canvas = dom.document.createElement("canvas").asInstanceOf[dom.html.Canvas]
      val ratio = Math.min(maxW.toDouble / img.naturalWidth, maxH.toDouble / img.naturalHeight)
      val w = (img.naturalWidth * ratio).toInt
      val h = (img.naturalHeight * ratio).toInt
      canvas.width = w
      canvas.height = h
      val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      ctx.drawImage(img, 0, 0, w, h)
      p.success(canvas.toDataURL("image/jpeg", 0.7))
    }
    img.addEventListener("error", { (_: dom.Event) =>
      p.success("")
    })
    img.src = dataUrl
    p.future

  // ─── JS serialization ─────────────────────────────────────────

  private def galleryToJs(g: GalleryImage): js.Dynamic =
    js.Dynamic.literal(
      "id"               -> g.id,
      "name"             -> g.name,
      "thumbnailDataUrl" -> g.thumbnailDataUrl,
      "width"            -> g.width,
      "height"           -> g.height,
      "addedAt"          -> g.addedAt,
      "sizeBytes"        -> g.sizeBytes.toDouble,
    )

  private def galleryFromJs(obj: js.Dynamic): GalleryImage =
    GalleryImage(
      id = obj.id.asInstanceOf[String],
      name = obj.name.asInstanceOf[String],
      thumbnailDataUrl = obj.thumbnailDataUrl.asInstanceOf[String],
      width = obj.width.asInstanceOf[Double].toInt,
      height = obj.height.asInstanceOf[Double].toInt,
      addedAt = obj.addedAt.asInstanceOf[Double],
      sizeBytes = obj.sizeBytes.asInstanceOf[Double].toLong,
    )
