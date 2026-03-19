package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.CalendarViewModel
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Gallery showing all images the user has worked with across sessions.
  * Images can be inserted into the current page or removed from the gallery.
  */
object ImageGalleryPanel {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "image-gallery-panel",

      h3(child.text <-- lang.map {
        case Language.En => "Image Gallery"
        case Language.Cs => "Galerie obrázků"
      }),

      p(
        cls := "image-gallery-hint",
        child.text <-- lang.map {
          case Language.En => "Images from all your editing sessions. Click to insert into the current page."
          case Language.Cs => "Obrázky ze všech vašich relací úprav. Kliknutím vložíte na aktuální stránku."
        },
      ),

      child <-- CalendarViewModel.galleryImagesVar.signal.combineWith(lang).map { case (images, l) =>
        if images.isEmpty then
          div(
            cls := "image-gallery-empty",
            p(l match
              case Language.En => "No images yet. Upload photos in the editor and they will appear here."
              case Language.Cs => "Zatím žádné obrázky. Nahrajte fotky v editoru a objeví se zde."
            ),
          )
        else
          div(
            cls := "image-gallery-grid",
            images.map { imgData =>
              galleryThumbnail(imgData, l)
            },
          )
      },
    )
  }

  private def galleryThumbnail(imageData: String, lang: Language): Element = {
    val isLoaded = Var(true)

    div(
      cls := "gallery-thumbnail",

      // Try to render the image
      img(
        src := imageData,
        cls := "gallery-thumbnail-img",
        onError --> { _ => isLoaded.set(false) },
      ),

      // Warning overlay for broken images
      child <-- isLoaded.signal.map { loaded =>
        if loaded then emptyNode
        else
          div(
            cls := "gallery-thumbnail-warning",
            span("⚠"),
          )
      },

      // Action buttons
      div(
        cls := "gallery-thumbnail-actions",
        button(
          cls := "gallery-insert-btn",
          title := (lang match
            case Language.En => "Insert into current page"
            case Language.Cs => "Vložit na aktuální stránku"
          ),
          "＋",
          onClick --> { _ => CalendarViewModel.insertGalleryImage(imageData) },
        ),
        button(
          cls := "gallery-remove-btn",
          title := (lang match
            case Language.En => "Remove from gallery"
            case Language.Cs => "Odebrat z galerie"
          ),
          "✕",
          onClick --> { _ => CalendarViewModel.removeGalleryImage(imageData) },
        ),
      ),
    )
  }
}
