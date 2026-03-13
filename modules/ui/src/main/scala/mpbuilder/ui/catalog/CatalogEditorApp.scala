package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import mpbuilder.ui.catalog.views.*

/** Main catalog editor application.
  *
  * Provides a sidebar navigation for switching between catalog entity types
  * (Categories, Materials, Finishes, Printing Methods, Rules, Pricelist)
  * and an export/import section for JSON persistence.
  */
object CatalogEditorApp:

  def apply(): HtmlElement =
    div(
      cls := "catalog-editor-app",

      // Header
      div(
        cls := "catalog-header",
        h1("Catalog Editor"),
        p("Configure your product catalog, compatibility rules, and pricelists."),
      ),

      // Status message bar
      child <-- CatalogEditorViewModel.statusMessage.map {
        case Some(msg) =>
          div(
            cls := "catalog-status-bar",
            span(msg),
            button(cls := "btn btn-sm", "✕", onClick --> { _ => CatalogEditorViewModel.clearStatus() }),
          )
        case None => emptyNode
      },

      // Main layout: sidebar + content
      div(
        cls := "catalog-layout",

        // Sidebar navigation
        div(
          cls := "catalog-sidebar",
          CatalogSection.values.toList.map { section =>
            button(
              cls <-- CatalogEditorViewModel.activeSection.map { active =>
                if active == section then "catalog-nav-btn catalog-nav-btn--active"
                else "catalog-nav-btn"
              },
              sectionLabel(section),
              onClick --> { _ => CatalogEditorViewModel.setSection(section) },
            )
          },
        ),

        // Content area
        div(
          cls := "catalog-content",
          child <-- CatalogEditorViewModel.activeSection.map {
            case CatalogSection.Categories      => CategoryEditorView()
            case CatalogSection.Materials        => MaterialEditorView()
            case CatalogSection.Finishes         => FinishEditorView()
            case CatalogSection.PrintingMethods  => PrintingMethodEditorView()
            case CatalogSection.Rules            => RulesEditorView()
            case CatalogSection.Pricelist        => PricelistEditorView()
            case CatalogSection.Export           => ExportImportView()
          },
        ),
      ),
    )

  private def sectionLabel(section: CatalogSection): String = section match
    case CatalogSection.Categories     => "📦 Categories"
    case CatalogSection.Materials      => "📄 Materials"
    case CatalogSection.Finishes       => "✨ Finishes"
    case CatalogSection.PrintingMethods => "🖨 Printing Methods"
    case CatalogSection.Rules          => "📏 Rules"
    case CatalogSection.Pricelist      => "💰 Pricelist"
    case CatalogSection.Export         => "📤 Export / Import"
