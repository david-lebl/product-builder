package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*
import mpbuilder.ui.{Router, Page}
import mpbuilder.uikit.containers.{SideNav, SideNavItem}

/** Main manufacturing application with sidebar navigation using Waypoint URL routing. */
object ManufacturingApp:

  /** Map ManufacturingRoute enum to Page type for URL routing. */
  private def routeToPage(route: ManufacturingRoute): Page = route match
    case ManufacturingRoute.Dashboard     => Page.ManufacturingDashboard
    case ManufacturingRoute.StationQueue  => Page.ManufacturingStationQueue
    case ManufacturingRoute.OrderApproval => Page.ManufacturingOrderApproval
    case ManufacturingRoute.OrderProgress => Page.ManufacturingOrderProgress
    case ManufacturingRoute.Employees     => Page.ManufacturingEmployees
    case ManufacturingRoute.Machines      => Page.ManufacturingMachines
    case ManufacturingRoute.Analytics     => Page.ManufacturingAnalytics
    case ManufacturingRoute.Settings      => Page.ManufacturingSettings

  /** Map Page type back to ManufacturingRoute for rendering. */
  private def pageToRoute(page: Page): ManufacturingRoute = page match
    case Page.ManufacturingDashboard     => ManufacturingRoute.Dashboard
    case Page.ManufacturingStationQueue  => ManufacturingRoute.StationQueue
    case Page.ManufacturingOrderApproval => ManufacturingRoute.OrderApproval
    case Page.ManufacturingOrderProgress => ManufacturingRoute.OrderProgress
    case Page.ManufacturingEmployees     => ManufacturingRoute.Employees
    case Page.ManufacturingMachines      => ManufacturingRoute.Machines
    case Page.ManufacturingAnalytics     => ManufacturingRoute.Analytics
    case Page.ManufacturingSettings      => ManufacturingRoute.Settings
    case _ => ManufacturingRoute.Dashboard // Default fallback

  /** Signal for current manufacturing route derived from Waypoint router. */
  private val currentRouteSignal: Signal[ManufacturingRoute] =
    Router.currentPageSignal.map(pageToRoute)

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
            isActive = currentRouteSignal.map(_ == route),
            onClick = () => Router.pushState(routeToPage(route)),
          )
        },
      ),

      // Main content area - renders based on Waypoint router state
      div(
        cls := "app-sidebar-content",
        child <-- currentRouteSignal.map {
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
