package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.manufacturing.ManufacturingService.*
import mpbuilder.ui.manufacturing.ManufacturingViewModel

object DashboardView:

  def apply(): HtmlElement =
    val vm = ManufacturingViewModel

    div(
      cls := "dashboard-view",

      h2(cls := "dashboard-title", "Manufacturing Dashboard"),

      // Summary cards row
      div(
        cls := "dashboard-cards",
        summaryCard("Awaiting Approval", vm.pendingApprovalOrders.map(_.size.toString), "card-approval"),
        summaryCard("In Production",     vm.inProgressOrders.map(_.size.toString),     "card-production"),
        summaryCard("Queued",            vm.queuedOrders.map(_.size.toString),          "card-queued"),
        summaryCard("Completed",         vm.completedOrders.map(_.size.toString),       "card-completed"),
        summaryCard("On Hold",           vm.onHoldOrders.map(_.size.toString),          "card-hold"),
      ),

      // Station status strip
      h3(cls := "dashboard-section-title", "Station Status"),
      div(
        cls := "station-strip",
        children <-- vm.state.map { s =>
          s.mfgState.stations.map { station =>
            val summary = ManufacturingService.stationSummary(s.mfgState, station.id)
            val inProgressCount = summary.getOrElse(OrderStatus.InProgress, 0)
            val queuedCount     = summary.getOrElse(OrderStatus.Queued, 0)
            div(
              cls := "station-card",
              div(cls := "station-card-name", station.name.value),
              div(cls := "station-card-type", station.stationType.toString),
              div(
                cls := "station-card-counts",
                if inProgressCount > 0 then
                  span(cls := "station-badge station-badge--active", s"$inProgressCount active")
                else emptyNode,
                if queuedCount > 0 then
                  span(cls := "station-badge station-badge--queued", s"$queuedCount queued")
                else emptyNode,
                if inProgressCount == 0 && queuedCount == 0 then
                  span(cls := "station-badge station-badge--idle", "idle")
                else emptyNode,
              ),
            )
          }
        },
      ),

      // Recent orders table
      h3(cls := "dashboard-section-title", "Recent Orders"),
      table(
        cls := "data-table",
        thead(
          tr(
            th(cls := "data-table-th", "Order ID"),
            th(cls := "data-table-th", "Customer"),
            th(cls := "data-table-th", "Product"),
            th(cls := "data-table-th", "Priority"),
            th(cls := "data-table-th", "Status"),
            th(cls := "data-table-th", "Deadline"),
          ),
        ),
        tbody(
          children <-- vm.orders.map { orders =>
            orders.take(10).map { order =>
              tr(
                cls := "data-table-row",
                td(cls := "data-table-td", order.orderId.value),
                td(cls := "data-table-td", order.customerName),
                td(cls := "data-table-td",
                  s"${order.configuration.category.name.value} ×${order.quantity}"),
                td(cls := "data-table-td",
                  span(
                    cls := s"priority-badge priority-badge--${order.priority.toString.toLowerCase}",
                    order.priority.toString,
                  ),
                ),
                td(cls := "data-table-td", orderStatusLabel(order)),
                td(cls := "data-table-td",
                  order.deadline.map(d => ManufacturingViewModel.formatDeadline(d)).getOrElse("—"),
                ),
              )
            }
          },
        ),
      ),
    )

  private def summaryCard(title: String, value: Signal[String], extraCls: String): HtmlElement =
    div(
      cls := s"summary-card $extraCls",
      div(cls := "summary-card-value", child.text <-- value),
      div(cls := "summary-card-title", title),
    )

  private def orderStatusLabel(order: ManufacturingOrder): String =
    if order.isFullyCompleted then "Completed"
    else
      order.currentStatus match
        case Some(OrderStatus.InProgress) => "In Progress"
        case Some(OrderStatus.Queued)     => "Queued"
        case Some(OrderStatus.OnHold)     => "On Hold"
        case Some(OrderStatus.Completed)  => "Completed"
        case None =>
          if order.steps.isEmpty then "Awaiting Approval"
          else "Pending"
