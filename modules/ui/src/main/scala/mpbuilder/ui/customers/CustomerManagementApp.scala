package mpbuilder.ui.customers

import com.raquo.laminar.api.L.*
import mpbuilder.ui.customers.views.*
import mpbuilder.ui.{Router, Page}
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Main customer management application.
  *
  * Uses the shared SideNav component from ui-framework for visual consistency
  * with ManufacturingApp and CatalogEditorApp. Provides sidebar navigation
  * for Customers and Discount Codes.
  *
  * Navigation uses Waypoint for URL-based routing with browser history support.
  */
object CustomerManagementApp:

  /** Map CustomerSection enum to Page type for URL routing. */
  private def sectionToPage(section: CustomerSection): Page = section match
    case CustomerSection.Customers       => Page.CustomersList
    case CustomerSection.CustomerPricing => Page.CustomerPricing
    case CustomerSection.DiscountCodes   => Page.DiscountCodes

  /** Map Page type back to CustomerSection for rendering. */
  private def pageToSection(page: Page): CustomerSection = page match
    case Page.CustomersList    => CustomerSection.Customers
    case Page.CustomerPricing  => CustomerSection.CustomerPricing
    case Page.DiscountCodes    => CustomerSection.DiscountCodes
    case _ => CustomerSection.Customers // Default fallback

  /** Signal for current customer section derived from Waypoint router. */
  private val currentSectionSignal: Signal[CustomerSection] =
    Router.currentPageSignal.map(pageToSection)

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
              isActive = currentSectionSignal.map(_ == section),
              onClick = () => Router.pushState(sectionToPage(section)),
            )
          },
        ),

        // Content area - renders based on Waypoint router state
        div(
          cls := "app-sidebar-content",
          child <-- currentSectionSignal.map {
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
