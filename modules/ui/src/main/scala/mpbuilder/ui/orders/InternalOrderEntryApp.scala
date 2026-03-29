package mpbuilder.ui.orders

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Internal order entry application — compact UI for employees to create orders.
  *
  * Layout follows the same sidebar pattern as ManufacturingApp and CustomerManagementApp.
  * Sections: Order Items (main work area) and Order Summary.
  */
object InternalOrderEntryApp:

  def apply(): HtmlElement =
    div(
      cls := "catalog-editor-app",

      div(
        cls := "app-sidebar-layout",

        // Sidebar navigation
        SideNav(
          icon = "📝",
          title = "New Order",
          items = OrderEntrySection.values.toList.map { section =>
            SideNavItem(
              icon = sectionIcon(section),
              label = sectionLabel(section),
              isActive = InternalOrderEntryViewModel.activeSection.map(_ == section),
              onClick = () => InternalOrderEntryViewModel.setSection(section),
            )
          },
        ),

        // Content area
        div(
          cls := "app-sidebar-content",

          // Customer selection bar (always visible at top)
          CustomerSelectionBar(),

          // Section content
          child <-- InternalOrderEntryViewModel.activeSection.map {
            case OrderEntrySection.OrderItems   => OrderItemsView()
            case OrderEntrySection.OrderSummary => OrderSummaryView()
          },
        ),
      ),
    )

  private def sectionIcon(section: OrderEntrySection): String = section match
    case OrderEntrySection.OrderItems   => "📦"
    case OrderEntrySection.OrderSummary => "📊"

  private def sectionLabel(section: OrderEntrySection): String = section match
    case OrderEntrySection.OrderItems   => "Order Items"
    case OrderEntrySection.OrderSummary => "Summary"
