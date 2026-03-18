package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import mpbuilder.ui.catalog.views.*
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Main catalog editor application.
  *
  * Uses the shared SideNav component from ui-framework for visual consistency
  * with ManufacturingApp and CustomerManagementApp.
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
              isActive = CatalogEditorViewModel.activeSection.map(_ == section),
              onClick = () => CatalogEditorViewModel.setSection(section),
            )
          },
        ),

        // Content area
        div(
          cls := "app-sidebar-content",
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
