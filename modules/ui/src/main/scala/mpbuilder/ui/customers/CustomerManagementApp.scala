package mpbuilder.ui.customers

import com.raquo.laminar.api.L.*
import mpbuilder.ui.customers.views.*
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Main customer management application.
  *
  * Uses the shared SideNav component from ui-framework for visual consistency
  * with ManufacturingApp and CatalogEditorApp. Provides sidebar navigation
  * for Customers and Discount Codes.
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
        cls := "app-sidebar-layout",

        // Sidebar navigation
        SideNav(
          icon = "👤",
          title = "Customers",
          items = CustomerSection.values.toList.map { section =>
            SideNavItem(
              icon = sectionIcon(section),
              label = sectionName(section),
              isActive = CustomerManagementViewModel.activeSection.map(_ == section),
              onClick = () => CustomerManagementViewModel.setSection(section),
            )
          },
        ),

        // Content area
        div(
          cls := "app-sidebar-content",
          child <-- CustomerManagementViewModel.activeSection.map {
            case CustomerSection.Customers       => CustomersView()
            case CustomerSection.CustomerPricing => CustomerPricingView()
            case CustomerSection.DiscountCodes   => DiscountCodesView()
          },
        ),
      ),
    )

  private def sectionIcon(section: CustomerSection): String = section match
    case CustomerSection.Customers       => "👤"
    case CustomerSection.CustomerPricing => "💰"
    case CustomerSection.DiscountCodes   => "🏷️"

  private def sectionName(section: CustomerSection): String = section match
    case CustomerSection.Customers       => "Customers"
    case CustomerSection.CustomerPricing => "Customer Pricing"
    case CustomerSection.DiscountCodes   => "Discount Codes"
