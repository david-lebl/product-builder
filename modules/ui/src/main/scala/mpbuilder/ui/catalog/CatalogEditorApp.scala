package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import mpbuilder.ui.catalog.views.*

/** Main catalog editor application.
  *
  * Uses the same dark-sidebar layout as ManufacturingApp for visual consistency.
  * Provides sidebar navigation for switching between catalog entity types
  * (Categories, Materials, Finishes, Printing Methods, Rules, Pricelist)
  * and an export/import section for JSON persistence.
  */
object CatalogEditorApp:

  def apply(): HtmlElement =
    div(
      cls := "catalog-editor-app",

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

      // Main layout: sidebar + content (ManufacturingApp-style)
      div(
        cls := "catalog-layout",

        // Dark sidebar navigation
        htmlTag("nav")(
          cls := "catalog-sidebar",
          div(
            cls := "catalog-sidebar-header",
            span(cls := "catalog-sidebar-icon", "📋"),
            span(cls := "catalog-sidebar-title", "Catalog"),
          ),
          div(
            cls := "catalog-sidebar-nav",
            CatalogSection.values.toList.map { section =>
              button(
                cls <-- CatalogEditorViewModel.activeSection.map { active =>
                  if active == section then "catalog-nav-btn catalog-nav-btn--active"
                  else "catalog-nav-btn"
                },
                span(cls := "catalog-nav-icon", sectionIcon(section)),
                span(cls := "catalog-nav-label", sectionName(section)),
                onClick --> { _ => CatalogEditorViewModel.setSection(section) },
              )
            },
          ),
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

  private def sectionIcon(section: CatalogSection): String = section match
    case CatalogSection.Categories     => "📦"
    case CatalogSection.Materials      => "📄"
    case CatalogSection.Finishes       => "✨"
    case CatalogSection.PrintingMethods => "🖨"
    case CatalogSection.Rules          => "📏"
    case CatalogSection.Pricelist      => "💰"
    case CatalogSection.Export         => "📤"

  private def sectionName(section: CatalogSection): String = section match
    case CatalogSection.Categories     => "Categories"
    case CatalogSection.Materials      => "Materials"
    case CatalogSection.Finishes       => "Finishes"
    case CatalogSection.PrintingMethods => "Printing Methods"
    case CatalogSection.Rules          => "Rules"
    case CatalogSection.Pricelist      => "Pricelist"
    case CatalogSection.Export         => "Export / Import"
