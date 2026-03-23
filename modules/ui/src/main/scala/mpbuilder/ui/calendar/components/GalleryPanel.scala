package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global

/** Sidebar panel for the shared image gallery */
object GalleryPanel {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "gallery-panel",

      h4(child.text <-- lang.map {
        case Language.En => "Image Gallery"
        case Language.Cs => "Galerie obrazku"
      }),

      // Upload controls
      div(
        cls := "gallery-upload-area",
        input(
          typ := "file",
          accept := "image/*",
          idAttr := "gallery-upload-input",
          display := "none",
          multiple := true,
          onChange --> { ev =>
            val inp = ev.target.asInstanceOf[dom.html.Input]
            val files = inp.files
            if files.length > 0 then
              (0 until files.length).foreach { i =>
                val file = files(i)
                ImageStore.addImage(file).foreach { _ =>
                  CalendarViewModel.refreshGallery()
                }
              }
              // Reset input so the same file can be re-selected
              inp.value = ""
          }
        ),
        button(
          cls := "gallery-upload-btn",
          child.text <-- lang.map {
            case Language.En => "Add Images"
            case Language.Cs => "Pridat obrazky"
          },
          onClick --> { _ =>
            dom.document.getElementById("gallery-upload-input").asInstanceOf[dom.html.Input].click()
          }
        ),
      ),

      // Broken images warning
      child.maybe <-- CalendarViewModel.brokenImageCount.map { count =>
        if count > 0 then Some(
          div(
            cls := "gallery-warning",
            child.text <-- lang.map {
              case Language.En => s"$count image(s) could not be loaded"
              case Language.Cs => s"$count obrazku nelze nacist"
            }
          )
        ) else None
      },

      // Image grid
      div(
        cls := "gallery-grid",
        children <-- CalendarViewModel.galleryImages.combineWith(lang).map { (images: List[GalleryImage], language: Language) =>
          if images.isEmpty then
            List(div(cls := "gallery-empty", language match {
              case Language.En => "No images in gallery. Upload some!"
              case Language.Cs => "V galerii nejsou zadne obrazky."
            }))
          else
            images.map(img => renderGalleryItem(img, language))
        }
      ),
    )
  }

  private def renderGalleryItem(image: GalleryImage, lang: Language): Element = {
    val sizeStr =
      if image.sizeBytes > 1024 * 1024 then s"${(image.sizeBytes / 1024.0 / 1024.0 * 10).toInt / 10.0} MB"
      else s"${(image.sizeBytes / 1024.0).toInt} KB"

    div(
      cls := "gallery-item",

      // Thumbnail
      div(
        cls := "gallery-thumb",
        if image.thumbnailDataUrl.nonEmpty then
          img(
            src := image.thumbnailDataUrl,
            draggable := false,
          )
        else
          div(cls := "gallery-thumb-placeholder", "?"),
      ),

      // Info
      div(
        cls := "gallery-item-info",
        div(cls := "gallery-item-name", image.name),
        div(cls := "gallery-item-dims", s"${image.width}x${image.height} · $sizeStr"),
      ),

      // Actions
      div(
        cls := "gallery-item-actions",
        button(
          cls := "gallery-action-btn gallery-use-btn",
          title := (lang match {
            case Language.En => "Add to current page"
            case Language.Cs => "Pridat na stranku"
          }),
          "+",
          onClick --> { _ =>
            // Load full image from store and add as photo element
            ImageStore.loadImageDataUrl(image.id).foreach {
              case Some(dataUrl) => CalendarViewModel.uploadPhoto(dataUrl)
              case None          => dom.console.warn(s"Image ${image.id} not found in store")
            }
          }
        ),
        button(
          cls := "gallery-action-btn gallery-remove-btn",
          title := (lang match {
            case Language.En => "Remove from gallery"
            case Language.Cs => "Odebrat z galerie"
          }),
          "x",
          onClick --> { _ =>
            ImageStore.deleteImage(image.id).foreach { _ =>
              CalendarViewModel.refreshGallery()
            }
          }
        ),
      ),
    )
  }
}
