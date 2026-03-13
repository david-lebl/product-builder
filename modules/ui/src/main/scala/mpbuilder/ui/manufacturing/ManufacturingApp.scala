package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*

/** Main manufacturing application with sidebar navigation. */
object ManufacturingApp:

  def apply(): HtmlElement =
    div(
      cls := "manufacturing-app",

      // Sidebar navigation
      htmlTag("nav")(
        cls := "manufacturing-sidebar",
        div(
          cls := "manufacturing-sidebar-header",
          span(cls := "manufacturing-sidebar-icon", "🏭"),
          span(cls := "manufacturing-sidebar-title", "Manufacturing"),
        ),
        div(
          cls := "manufacturing-sidebar-nav",
          ManufacturingRoute.values.filter(_.isAvailable).toList.map { route =>
            button(
              cls <-- ManufacturingViewModel.currentRoute.signal.map { current =>
                if current == route then "manufacturing-nav-item manufacturing-nav-item--active"
                else "manufacturing-nav-item"
              },
              span(cls := "manufacturing-nav-icon", route.icon),
              span(cls := "manufacturing-nav-label", route.label),
              onClick --> { _ => ManufacturingViewModel.currentRoute.set(route) },
            )
          },
        ),
      ),

      // Main content area
      div(
        cls := "manufacturing-content",
        child <-- ManufacturingViewModel.currentRoute.signal.map {
          case ManufacturingRoute.Dashboard     => DashboardView()
          case ManufacturingRoute.StationQueue  => StationQueueView()
          case ManufacturingRoute.OrderApproval => OrderApprovalView()
          case ManufacturingRoute.OrderProgress => OrderProgressView()
          case ManufacturingRoute.Employees     => EmployeesView()
          case ManufacturingRoute.Machines      => MachinesView()
          case ManufacturingRoute.Analytics     => AnalyticsView()
        },
      ),
    )
