package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Clipart gallery panel with predefined SVG cliparts, categories, and search */
object ClipartGalleryPanel {

  private val AllCategory = "all"

  private val categories: List[(String, String, String)] = List(
    ("all", "All", "Vše"),
    ("shapes", "Shapes", "Tvary"),
    ("arrows", "Arrows", "Šipky"),
    ("decorations", "Decorations", "Dekorace"),
    ("nature", "Nature", "Příroda"),
    ("symbols", "Symbols", "Symboly"),
    ("emoji", "Emoji", "Emoji"),
    ("badges", "Badges", "Odznaky"),
  )

  // State
  private val searchVar: Var[String] = Var("")
  private val categoryVar: Var[String] = Var(AllCategory)

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "clipart-gallery-section",

      p(
        cls := "gallery-description",
        child.text <-- lang.map {
          case Language.En => "Click any clipart to add it to your page."
          case Language.Cs => "Kliknutím přidáte klipart na stránku."
        },
      ),

      // Search
      div(
        cls := "clipart-search-row",
        input(
          typ := "text",
          cls := "clipart-search-input",
          placeholder <-- lang.map {
            case Language.En => "Search cliparts..."
            case Language.Cs => "Hledat kliparty..."
          },
          controlled(
            value <-- searchVar.signal,
            onInput.mapToValue --> { v => searchVar.set(v) }
          ),
        ),
      ),

      // Category filter buttons
      div(
        cls := "clipart-categories",
        children <-- lang.map { language =>
          categories.map { case (catId, nameEn, nameCs) =>
            button(
              cls := "clipart-category-btn",
              cls <-- categoryVar.signal.map(c => if c == catId then "active" else ""),
              language match {
                case Language.En => nameEn
                case Language.Cs => nameCs
              },
              onClick --> { _ => categoryVar.set(catId) },
            )
          }
        },
      ),

      // Clipart grid
      div(
        cls := "clipart-grid",
        children <-- searchVar.signal.combineWith(categoryVar.signal).map { (search: String, cat: String) =>
          val filtered = SampleCliparts.all.filter { item =>
            val matchesCat = cat == AllCategory || item.category == cat
            val matchesSearch = search.isEmpty || item.name.toLowerCase.contains(search.toLowerCase)
            matchesCat && matchesSearch
          }

          if filtered.isEmpty then
            List(div(cls := "clipart-empty", child.text <-- lang.map {
              case Language.En => "No cliparts found"
              case Language.Cs => "Žádné kliparty nenalezeny"
            }))
          else
            filtered.map { item =>
              div(
                cls := "clipart-item",
                title := item.name,
                img(
                  src := item.svgData,
                  styleAttr := "width: 100%; height: 100%; object-fit: contain;",
                  draggable := false,
                ),
                onClick --> { _ =>
                  VisualEditorViewModel.addClipart(item.svgData)
                },
              )
            }
        },
      ),
    )
  }
}
