package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import mpbuilder.ui.catalog.views.*
import mpbuilder.ui.{Router, Page}
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Main catalog editor application.
  *
  * Uses the shared SideNav component from ui-framework for visual consistency
  * with ManufacturingApp and CustomerManagementApp.
  * Provides sidebar navigation for switching between catalog entity types
  * (Categories, Materials, Finishes, Printing Methods, Rules, Pricelist)
  * and an export/import section for JSON persistence.
  *
  * Navigation uses Waypoint for URL-based routing with browser history support.
  */
object CatalogEditorApp:

  /** Map CatalogSection enum to Page type for URL routing. */
  private def sectionToPage(section: CatalogSection): Page = section match
    case CatalogSection.Categories      => Page.CatalogCategories
    case CatalogSection.Materials       => Page.CatalogMaterials
    case CatalogSection.Finishes        => Page.CatalogFinishes
    case CatalogSection.PrintingMethods => Page.CatalogPrintingMethods
    case CatalogSection.Rules           => Page.CatalogRules
    case CatalogSection.Pricelist       => Page.CatalogPricelist
    case CatalogSection.Export          => Page.CatalogExport

  /** Map Page type back to CatalogSection for rendering. */
  private def pageToSection(page: Page): CatalogSection = page match
    case Page.CatalogCategories      => CatalogSection.Categories
    case Page.CatalogMaterials       => CatalogSection.Materials
    case Page.CatalogFinishes        => CatalogSection.Finishes
    case Page.CatalogPrintingMethods => CatalogSection.PrintingMethods
    case Page.CatalogRules           => CatalogSection.Rules
    case Page.CatalogPricelist       => CatalogSection.Pricelist
    case Page.CatalogExport          => CatalogSection.Export
    case _ => CatalogSection.Categories // Default fallback

  /** Signal for current catalog section derived from Waypoint router. */
  private val currentSectionSignal: Signal[CatalogSection] =
    Router.currentPageSignal.map(pageToSection)

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

      // Main layout: sidebar + content
      div(
        cls := "app-sidebar-layout",

        // Sidebar navigation
        SideNav(
          icon = "📋",
          title = "Catalog",
          items = CatalogSection.values.toList.map { section =>
            SideNavItem(
              icon = sectionIcon(section),
              label = sectionName(section),
              isActive = currentSectionSignal.map(_ == section),
              onClick = () => {
                CatalogEditorViewModel.setEditState(EditState.None)
                Router.pushState(sectionToPage(section))
              },
            )
          },
        ),

        // Content area - renders based on Waypoint router state
        div(
          cls := "app-sidebar-content",
          child <-- currentSectionSignal.map {
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
