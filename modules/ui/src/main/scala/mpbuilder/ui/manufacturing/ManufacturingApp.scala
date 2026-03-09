package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*

/** Main Manufacturing application — combines sidebar navigation with routed views. */
object ManufacturingApp:

  def apply(): Element =
    val route = ManufacturingViewModel.currentRoute

    div(
      cls := "mfg-app",

      // Sidebar navigation
      htmlTag("nav")(
        cls := "mfg-sidebar",
        div(
          cls := "mfg-sidebar-header",
          span(cls := "mfg-sidebar-icon", "🏭"),
          span("Manufacturing"),
        ),

        ManufacturingRoute.values.toList.map { r =>
          button(
            cls <-- route.map(current =>
              if current == r then "mfg-nav-item active" else "mfg-nav-item"
            ),
            span(cls := "mfg-nav-icon", r.icon),
            span(cls := "mfg-nav-label", r.label),
            onClick --> { _ => ManufacturingViewModel.navigateTo(r) },
          )
        },
      ),

      // Main content area
      div(
        cls := "mfg-content",
        child <-- route.map {
          case ManufacturingRoute.Dashboard     => DashboardView()
          case ManufacturingRoute.OrderQueue    => OrderQueueView()
          case ManufacturingRoute.WorkQueue     => WorkQueueView()
          case ManufacturingRoute.OrderProgress => OrderProgressView()
          case ManufacturingRoute.Employees     => EmployeesView()
        },
      ),
    )
