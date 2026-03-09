package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*

/** Main Manufacturing application — combines sidebar navigation with routed views.
  * The sidebar provides a linear workflow navigation: Order Approval → Work Queue → Progress → Employees.
  */
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

        // Linear workflow steps in the sidebar
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

        // Linear flow indicator
        div(
          cls := "mfg-flow-indicator",
          div(cls := "mfg-flow-line"),
          ManufacturingRoute.values.toList.zipWithIndex.map { case (r, idx) =>
            div(
              cls <-- route.map { current =>
                val isCurrent = current == r
                val isPast = current.ordinal > r.ordinal
                s"mfg-flow-dot${if isCurrent then " current" else if isPast then " past" else ""}"
              },
              styleAttr := s"top: ${32 + idx * 48}px",
            )
          },
        ),
      ),

      // Main content area
      div(
        cls := "mfg-content",
        child <-- route.map {
          case ManufacturingRoute.OrderQueue    => OrderQueueView()
          case ManufacturingRoute.WorkQueue     => WorkQueueView()
          case ManufacturingRoute.OrderProgress => OrderProgressView()
          case ManufacturingRoute.Employees     => EmployeesView()
        },
      ),
    )
