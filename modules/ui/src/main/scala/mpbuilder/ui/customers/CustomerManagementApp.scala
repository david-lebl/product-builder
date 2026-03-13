package mpbuilder.ui.customers

import com.raquo.laminar.api.L.*
import mpbuilder.ui.customers.views.*

/** Main customer management application.
  *
  * Uses the same dark-sidebar layout as ManufacturingApp and CatalogEditorApp
  * for visual consistency. Provides sidebar navigation for Customers and Discount Codes.
  */
object CustomerManagementApp:

  def apply(): HtmlElement =
    div(
      cls := "catalog-editor-app",

      // Status message bar
      child <-- CustomerManagementViewModel.statusMessage.map {
        case Some(msg) =>
          div(
            cls := "catalog-status-bar",
            span(msg),
            button(cls := "btn btn-sm", "✕", onClick --> { _ => CustomerManagementViewModel.clearStatus() }),
          )
        case None => emptyNode
      },

      // Main layout: sidebar + content
      div(
        cls := "catalog-layout",

        // Dark sidebar navigation
        htmlTag("nav")(
          cls := "catalog-sidebar",
          div(
            cls := "catalog-sidebar-header",
            span(cls := "catalog-sidebar-icon", "👤"),
            span(cls := "catalog-sidebar-title", "Customers"),
          ),
          div(
            cls := "catalog-sidebar-nav",
            CustomerSection.values.toList.map { section =>
              button(
                cls <-- CustomerManagementViewModel.activeSection.map { active =>
                  if active == section then "catalog-nav-btn catalog-nav-btn--active"
                  else "catalog-nav-btn"
                },
                span(cls := "catalog-nav-icon", sectionIcon(section)),
                span(cls := "catalog-nav-label", sectionName(section)),
                onClick --> { _ => CustomerManagementViewModel.setSection(section) },
              )
            },
          ),
        ),

        // Content area
        div(
          cls := "catalog-content",
          child <-- CustomerManagementViewModel.activeSection.map {
            case CustomerSection.Customers     => CustomersView()
            case CustomerSection.DiscountCodes => DiscountCodesView()
          },
        ),
      ),
    )

  private def sectionIcon(section: CustomerSection): String = section match
    case CustomerSection.Customers     => "👤"
    case CustomerSection.DiscountCodes => "🏷️"

  private def sectionName(section: CustomerSection): String = section match
    case CustomerSection.Customers     => "Customers"
    case CustomerSection.DiscountCodes => "Discount Codes"
