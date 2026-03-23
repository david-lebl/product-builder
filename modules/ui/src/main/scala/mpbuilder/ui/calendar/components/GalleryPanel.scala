package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.productbuilder.ProductBuilderViewModel

/** Image gallery panel for cross-session image management */
object GalleryPanel {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "gallery-panel",

      // Upload button
      div(
        cls := "gallery-upload-area",
        label(
          cls := "gallery-upload-btn",
          child.text <-- lang.map {
            case Language.En => "📁 Add Images"
            case Language.Cs => "📁 Přidat obrázky"
          },
          input(
            typ := "file",
            accept := "image/*",
            multiple := true,
            display := "none",
            onChange --> { ev =>
              val input = ev.target.asInstanceOf[org.scalajs.dom.HTMLInputElement]
              val files = input.files
              (0 until files.length).foreach { i =>
                val file = files(i)
                processImageFile(file)
              }
              // Reset input so the same file can be re-selected
              input.value = ""
            },
          ),
        ),
      ),

      // Gallery grid
      div(
        cls := "gallery-grid",
        children <-- CalendarViewModel.galleryImages.combineWith(lang).map { case (images, l) =>
          if images.isEmpty then
            List(div(cls := "gallery-empty", l match
              case Language.En => "No images in gallery. Upload images to reuse them across sessions."
              case Language.Cs => "V galerii nejsou žádné obrázky. Nahrajte obrázky pro použití napříč relacemi."
            ))
          else
            images.sortBy(-_.addedAt).map { img =>
              div(
                cls := "gallery-item",
                div(
                  cls := "gallery-item-thumb",
                  imgTag(
                    src := img.thumbnailDataUrl,
                    alt := img.name,
                  ),
                ),
                div(
                  cls := "gallery-item-info",
                  div(cls := "gallery-item-name", truncateName(img.name, 20)),
                  div(cls := "gallery-item-size", s"${img.width}×${img.height}"),
                ),
                div(
                  cls := "gallery-item-actions",
                  button(
                    cls := "gallery-use-btn",
                    title <-- lang.map {
                      case Language.En => "Add to current page"
                      case Language.Cs => "Přidat na aktuální stránku"
                    },
                    "➕",
                    onClick --> { _ =>
                      CalendarViewModel.uploadPhoto(img.thumbnailDataUrl)
                    },
                  ),
                  button(
                    cls := "gallery-delete-btn",
                    title <-- lang.map {
                      case Language.En => "Remove from gallery"
                      case Language.Cs => "Odebrat z galerie"
                    },
                    "×",
                    onClick --> { _ =>
                      CalendarViewModel.removeFromGallery(img.id)
                    },
                  ),
                ),
              )
            }
        },
      ),
    )
  }

  /** Process an uploaded image file: create thumbnail and add to gallery */
  private def processImageFile(file: org.scalajs.dom.File): Unit =
    val reader = new org.scalajs.dom.FileReader()
    reader.onload = { _ =>
      val dataUrl = reader.result.asInstanceOf[String]
      // Create a thumbnail using a canvas element
      val img = org.scalajs.dom.document.createElement("img").asInstanceOf[org.scalajs.dom.HTMLImageElement]
      img.onload = { _ =>
        val thumbSize = 150
        val canvas = org.scalajs.dom.document.createElement("canvas").asInstanceOf[org.scalajs.dom.HTMLCanvasElement]
        val scale = math.min(thumbSize.toDouble / img.width, thumbSize.toDouble / img.height)
        canvas.width = (img.width * scale).toInt
        canvas.height = (img.height * scale).toInt
        val ctx = canvas.getContext("2d").asInstanceOf[org.scalajs.dom.CanvasRenderingContext2D]
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
        val thumbnailDataUrl = canvas.toDataURL("image/jpeg", 0.7)

        CalendarViewModel.addToGallery(
          name = file.name,
          thumbnailDataUrl = thumbnailDataUrl,
          width = img.width.toInt,
          height = img.height.toInt,
          sizeBytes = file.size.toLong,
        )
      }
      img.src = dataUrl
    }
    reader.readAsDataURL(file)

  private def truncateName(name: String, maxLen: Int): String =
    if name.length <= maxLen then name
    else name.take(maxLen - 3) + "..."

  private def imgTag = htmlTag("img")
}
