package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.persistence.EditorSessionStore
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language
import org.scalajs.dom
import org.scalajs.dom.FileReader
import scala.scalajs.js

/** Sidebar panel for managing a persistent image gallery across all sessions */
object ImageGalleryPanel {

  // Gallery state — list of image references
  private val galleryVar: Var[List[ImageReference]] = Var(List.empty)

  // Track which images have load errors (broken/missing)
  private val brokenImagesVar: Var[Set[String]] = Var(Set.empty)

  private val DefaultLinkedFileName = "linked-image"

  // ID generation using random() for Scala.js compatibility (no java.util.UUID)
  private var idCounter: Int = 0
  private def generateImageId(): String = {
    idCounter += 1
    val rand = (js.Math.random() * 1000000).toInt
    s"gallery-${System.currentTimeMillis()}-$idCounter-$rand"
  }

  /** Create an ImageReference from a URL string */
  private def imageRefFromUrl(url: String, dataUrl: String, sizeBytes: Long): ImageReference =
    ImageReference(
      id = generateImageId(),
      dataUrl = dataUrl,
      fileName = Some(url.split("/").lastOption.getOrElse(DefaultLinkedFileName)),
      addedAt = System.currentTimeMillis().toDouble,
      sizeBytes = sizeBytes,
      usedInSessions = Set.empty,
    )

  /** Refresh gallery from IndexedDB */
  def refreshGallery(): Unit =
    EditorSessionStore.listAllImages { images =>
      galleryVar.set(images)
    }

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "gallery-section",

      // Load gallery on mount
      onMountCallback { _ => refreshGallery() },

      h4(child.text <-- lang.map {
        case Language.En => "Image Gallery"
        case Language.Cs => "Galerie obrázků"
      }),

      p(
        cls := "gallery-description",
        child.text <-- lang.map {
          case Language.En => "Upload and manage images across all sessions. Click to add an image to the current page."
          case Language.Cs => "Nahrajte a spravujte obrázky napříč všemi relacemi. Kliknutím přidáte obrázek na aktuální stránku."
        },
      ),

      // Upload buttons row
      div(
        cls := "gallery-upload-row",

        // Hidden file input for gallery upload (multiple)
        input(
          typ := "file",
          accept := "image/*",
          idAttr := "gallery-upload-input",
          display := "none",
          htmlAttr("multiple", com.raquo.laminar.codecs.BooleanAsAttrPresenceCodec) := true,
          onChange --> { ev =>
            val inp = ev.target.asInstanceOf[dom.html.Input]
            val files = inp.files
            if files.length > 0 then
              readFilesSequentially(files, 0)
            inp.value = "" // Reset so same file can be re-selected
          }
        ),

        button(
          cls := "gallery-upload-btn",
          child.text <-- lang.map {
            case Language.En => "📁 Upload Images"
            case Language.Cs => "📁 Nahrát obrázky"
          },
          onClick --> { _ =>
            dom.document.getElementById("gallery-upload-input").asInstanceOf[dom.html.Input].click()
          }
        ),

        button(
          cls := "gallery-link-btn",
          child.text <-- lang.map {
            case Language.En => "🔗 Link from URL"
            case Language.Cs => "🔗 Odkaz z URL"
          },
          onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
            val promptMsg = currentLang match {
              case Language.En => "Enter image URL (one per line for multiple):"
              case Language.Cs => "Zadejte URL obrázku (jeden na řádek pro více):"
            }
            val result = dom.window.prompt(promptMsg)
            if result != null && result.nonEmpty then
              val urls = result.split("\n").map(_.trim).filter(_.nonEmpty)
              urls.foreach { url =>
                addImageFromUrl(url)
              }
          }
        ),
      ),

      // Gallery grid
      div(
        cls := "gallery-grid",
        children <-- galleryVar.signal.combineWith(brokenImagesVar.signal, lang).map { (images: List[ImageReference], broken: Set[String], l: Language) =>
          if images.isEmpty then
            List(div(
              cls := "gallery-empty",
              l match {
                case Language.En => "No images in gallery. Upload or link images to get started."
                case Language.Cs => "V galerii nejsou žádné obrázky. Nahrajte nebo přilinkujte obrázky."
              }
            ))
          else
            images.map { img =>
              renderGalleryItem(img, broken.contains(img.id), l)
            }
        }
      ),
    )
  }

  private def renderGalleryItem(image: ImageReference, isBroken: Boolean, lang: Language): Element = {
    val sizeStr = formatFileSize(image.sizeBytes)
    val nameStr = image.fileName.getOrElse(image.id.take(12) + "...")

    div(
      cls := (if isBroken then "gallery-item gallery-item-broken" else "gallery-item"),

      // Thumbnail
      div(
        cls := "gallery-item-thumb",
        if isBroken then
          div(
            cls := "gallery-item-broken-overlay",
            span(cls := "gallery-broken-icon", "⚠️"),
            span(cls := "gallery-broken-text", lang match {
              case Language.En => "Image unavailable"
              case Language.Cs => "Obrázek nedostupný"
            }),
          )
        else
          img(
            src := image.dataUrl,
            alt := nameStr,
            cls := "gallery-thumb-img",
            // Detect load errors
            onError --> { _ =>
              brokenImagesVar.update(_ + image.id)
            },
          ),
      ),

      // Info
      div(
        cls := "gallery-item-info",
        div(cls := "gallery-item-name", title := nameStr, nameStr),
        div(cls := "gallery-item-size", sizeStr),
      ),

      // Actions
      div(
        cls := "gallery-item-actions",
        if isBroken then
          // Relink action for broken images
          button(
            cls := "gallery-btn-relink",
            lang match {
              case Language.En => "Relink"
              case Language.Cs => "Přelinkovat"
            },
            onClick --> { _ =>
              relinkImage(image, lang)
            }
          )
        else
          // Add to page
          button(
            cls := "gallery-btn-add",
            lang match {
              case Language.En => "Add to page"
              case Language.Cs => "Přidat na stránku"
            },
            onClick --> { _ =>
              VisualEditorViewModel.uploadPhoto(image.dataUrl)
              // Track usage
              val sessionId = VisualEditorViewModel.currentSessionIdSnapshot()
              sessionId.foreach { sid =>
                val updated = image.copy(usedInSessions = image.usedInSessions + sid)
                EditorSessionStore.saveImage(updated, () => refreshGallery())
              }
            }
          ),
        button(
          cls := "gallery-btn-remove",
          lang match {
            case Language.En => "Remove"
            case Language.Cs => "Odstranit"
          },
          onClick --> { ev =>
            ev.stopPropagation()
            EditorSessionStore.deleteImage(image.id, () => refreshGallery())
          }
        ),
      ),
    )
  }

  /** Read multiple files sequentially from a FileList */
  private def readFilesSequentially(files: dom.FileList, index: Int): Unit =
    if index < files.length then
      val file = files(index)
      val reader = new FileReader()
      reader.onload = { _ =>
        val imageData = reader.result.asInstanceOf[String]
        val imgRef = ImageReference(
          id = generateImageId(),
          dataUrl = imageData,
          fileName = Some(file.name),
          addedAt = System.currentTimeMillis().toDouble,
          sizeBytes = file.size.toLong,
          usedInSessions = Set.empty,
        )
        EditorSessionStore.saveImage(imgRef, () => {
          refreshGallery()
          // Read next file
          readFilesSequentially(files, index + 1)
        })
      }
      reader.readAsDataURL(file)

  /** Add an image from a URL by creating a temporary Image element to validate and load it */
  private def addImageFromUrl(url: String): Unit = {
    val imgEl = dom.document.createElement("img").asInstanceOf[dom.html.Image]
    val imgDyn = imgEl.asInstanceOf[js.Dynamic]
    imgDyn.crossOrigin = "anonymous"
    imgEl.onload = { (_: dom.Event) =>
      // Create a canvas to convert to data URL
      val canvas = dom.document.createElement("canvas").asInstanceOf[dom.html.Canvas]
      canvas.width = imgEl.naturalWidth
      canvas.height = imgEl.naturalHeight
      val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      ctx.drawImage(imgEl, 0, 0)
      try {
        val dataUrl = canvas.toDataURL("image/png")
        EditorSessionStore.saveImage(imageRefFromUrl(url, dataUrl, dataUrl.length.toLong), () => refreshGallery())
      } catch {
        case e: Exception =>
          // CORS or tainted canvas — fall back to raw URL reference
          dom.console.warn(s"Canvas export failed for $url, saving as URL reference: ${e.getMessage}")
          EditorSessionStore.saveImage(imageRefFromUrl(url, url, 0L), () => refreshGallery())
      }
    }
    imgDyn.onerror = { (_: dom.Event) =>
      // Image failed to load — save as URL reference (will show as broken)
      dom.console.warn(s"Failed to load image from URL: $url")
      EditorSessionStore.saveImage(imageRefFromUrl(url, url, 0L), () => refreshGallery())
    }
    imgEl.src = url
  }

  /** Prompt user to relink a broken image */
  private def relinkImage(image: ImageReference, lang: Language): Unit = {
    // Create a hidden file input for relinking
    val fileInput = dom.document.createElement("input").asInstanceOf[dom.html.Input]
    fileInput.`type` = "file"
    fileInput.accept = "image/*"
    fileInput.onchange = { (_: dom.Event) =>
      val files = fileInput.files
      if files.length > 0 then
        val file = files(0)
        val reader = new FileReader()
        reader.onload = { _ =>
          val imageData = reader.result.asInstanceOf[String]
          val updated = image.copy(
            dataUrl = imageData,
            fileName = Some(file.name),
            sizeBytes = file.size.toLong,
          )
          EditorSessionStore.saveImage(updated, () => {
            brokenImagesVar.update(_ - image.id)
            refreshGallery()
          })
        }
        reader.readAsDataURL(file)
    }
    fileInput.click()
  }

  private def formatFileSize(bytes: Long): String =
    if bytes <= 0 then "—"
    else if bytes < 1024 then s"${bytes}B"
    else if bytes < 1024 * 1024 then s"${bytes / 1024}KB"
    else s"${(bytes.toDouble / (1024 * 1024) * 10).round / 10.0}MB"
}
