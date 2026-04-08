package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Sidebar panel showing a static catalog of cliparts grouped into categories.
  *
  * Provides:
  *   - A search field that matches against the localised name and the keyword list.
  *   - A row of category chips ("All" + each `ClipartCategory`).
  *   - A grid of SVG thumbnails. Clicking an item adds it as a `ClipartElement`
  *     to the current page.
  */
object ClipartGalleryPanel {

  // Local UI state — kept inside the object so the panel survives re-mounts
  // (when the user closes and re-opens the drawer) without losing the query.
  private val searchQueryVar: Var[String] = Var("")
  private val activeCategoryVar: Var[Option[ClipartCategory]] = Var(None)

  def apply(): HtmlElement = {
    val lang = ProductBuilderViewModel.currentLanguage

    // Filtered list of items reactively combining search + category + language
    val filteredItems: Signal[List[ClipartItem]] =
      searchQueryVar.signal
        .combineWith(activeCategoryVar.signal, lang)
        .map { (query, categoryOpt, language) =>
          val langCode = language match
            case Language.En => "en"
            case Language.Cs => "cs"
          val byQuery = SampleCliparts.search(query, langCode)
          categoryOpt.fold(byQuery)(c => byQuery.filter(_.category == c))
        }

    div(
      cls := "clipart-gallery-section",

      h4(child.text <-- lang.map {
        case Language.En => "Cliparts"
        case Language.Cs => "Kliparty"
      }),

      p(
        cls := "gallery-description",
        child.text <-- lang.map {
          case Language.En => "Click an item to add it to the current page."
          case Language.Cs => "Klikněte na položku pro přidání na aktuální stránku."
        },
      ),

      // Search field
      input(
        cls := "clipart-search-input",
        typ := "text",
        placeholder <-- lang.map {
          case Language.En => "Search cliparts..."
          case Language.Cs => "Hledat kliparty..."
        },
        controlled(
          value <-- searchQueryVar.signal,
          onInput.mapToValue --> { v => searchQueryVar.set(v) },
        ),
      ),

      // Category filter chips
      div(
        cls := "clipart-category-chips",
        // "All" chip
        button(
          tpe := "button",
          cls <-- activeCategoryVar.signal.map(opt =>
            if opt.isEmpty then "clipart-chip clipart-chip--active" else "clipart-chip"
          ),
          child.text <-- lang.map {
            case Language.En => "All"
            case Language.Cs => "Vše"
          },
          onClick --> { _ => activeCategoryVar.set(None) },
        ),
        // One chip per category
        ClipartCategory.values.toList.map { category =>
          button(
            tpe := "button",
            cls <-- activeCategoryVar.signal.map(opt =>
              if opt.contains(category) then "clipart-chip clipart-chip--active" else "clipart-chip"
            ),
            child.text <-- lang.map { l =>
              SampleCliparts.categoryLabel(category, l match {
                case Language.En => "en"
                case Language.Cs => "cs"
              })
            },
            onClick --> { _ =>
              activeCategoryVar.update {
                case Some(c) if c == category => None
                case _                        => Some(category)
              }
            },
          )
        },
      ),

      // Grid of clipart thumbnails
      div(
        cls := "clipart-grid",
        children <-- filteredItems.combineWith(lang).map { (items, language) =>
          if items.isEmpty then
            List(div(
              cls := "clipart-empty",
              language match {
                case Language.En => "No cliparts match your search."
                case Language.Cs => "Žádné kliparty neodpovídají vyhledávání."
              }
            ))
          else
            items.map { item => renderItem(item, language) }
        },
      ),
    )
  }

  private def renderItem(item: ClipartItem, language: Language): HtmlElement = {
    val displayName = language match {
      case Language.En => item.nameEn
      case Language.Cs => item.nameCs
    }
    div(
      cls := "clipart-item",
      title := displayName,
      img(
        cls := "clipart-thumb",
        src := item.svgDataUrl,
        alt := displayName,
        draggable := false,
      ),
      div(cls := "clipart-name", displayName),
      onClick --> { _ => VisualEditorViewModel.addClipart(item.svgDataUrl) },
    )
  }
}
