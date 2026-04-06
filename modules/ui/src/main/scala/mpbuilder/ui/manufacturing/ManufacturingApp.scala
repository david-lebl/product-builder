package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Main manufacturing application with sidebar navigation. */
object ManufacturingApp:

  def apply(): HtmlElement =
    div(
      cls := "app-sidebar-layout",

      // Sidebar navigation
      SideNav(
        icon = "🏭",
        title = "Manufacturing",
        items = ManufacturingRoute.values.filter(_.isAvailable).toList.map { route =>
          SideNavItem(
            icon = route.icon,
            label = route.label,
            isActive = ManufacturingViewModel.currentRoute.signal.map(_ == route),
            onClick = () => ManufacturingViewModel.currentRoute.set(route),
          )
        },
      ),

      // Main content area
      div(
        cls := "app-sidebar-content",
        child <-- ManufacturingViewModel.currentRoute.signal.map {
          case ManufacturingRoute.Dashboard     => DashboardView()
          case ManufacturingRoute.StationQueue  => StationQueueView()
          case ManufacturingRoute.OrderApproval => OrderApprovalView()
          case ManufacturingRoute.OrderProgress => OrderProgressView()
          case ManufacturingRoute.Employees     => EmployeesView()
          case ManufacturingRoute.Machines      => MachinesView()
          case ManufacturingRoute.Analytics     => AnalyticsView()
          case ManufacturingRoute.Settings      => ManufacturingSettingsView()
        },
      ),
    )
