package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*

object ManufacturingApp:

  def apply(): HtmlElement =
    val vm = ManufacturingViewModel

    div(
      cls := "manufacturing-app",

      // Tab navigation
      div(
        cls := "manufacturing-tabs",
        tabButton("Dashboard",       ManufacturingView.Dashboard,     vm),
        tabButton("Station Queue",   ManufacturingView.StationQueue,  vm),
        tabButton("Order Approval",  ManufacturingView.OrderApproval, vm),
        tabButton("Order Progress",  ManufacturingView.OrderProgress, vm),
      ),

      // View content
      div(
        cls := "manufacturing-content",
        child <-- vm.state.map(_.activeView).map {
          case ManufacturingView.Dashboard    => DashboardView()
          case ManufacturingView.StationQueue => StationQueueView()
          case ManufacturingView.OrderApproval => OrderApprovalView()
          case ManufacturingView.OrderProgress => OrderProgressView()
        },
      ),
    )

  private def tabButton(label: String, view: ManufacturingView, vm: ManufacturingViewModel.type): HtmlElement =
    button(
      cls <-- vm.state.map(_.activeView).map { active =>
        if active == view then "mfg-tab mfg-tab--active" else "mfg-tab"
      },
      label,
      onClick --> { _ => vm.setView(view) },
    )
