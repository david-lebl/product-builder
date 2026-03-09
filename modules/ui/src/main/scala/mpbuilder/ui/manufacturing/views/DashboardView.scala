package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Dashboard View — summary cards, recent orders, station status/utilisation. */
object DashboardView:

  def apply(): Element =
    val state = ManufacturingViewModel.state

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Manufacturing Dashboard"),
        p(cls := "mfg-view-subtitle", "Overview of production status and station utilisation"),
      ),

      // Summary cards row
      div(
        cls := "mfg-dashboard-cards",
        children <-- state.map { s =>
          val totalOrders = s.orders.size
          val pendingApproval = s.orders.count(o => o.approval == ApprovalStatus.Pending || o.approval == ApprovalStatus.ChangesRequested)
          val inProduction = s.orders.count(o => o.approval == ApprovalStatus.Approved &&
            o.items.exists(_.workflow.exists(_.status == WorkflowStatus.InProgress)))
          val completed = s.orders.count(o => o.approval == ApprovalStatus.Approved &&
            o.items.forall(_.workflow.exists(_.status == WorkflowStatus.Completed)))
          val rushOrders = s.orders.count(_.priority == Priority.Rush)

          val totalSteps = s.orders.flatMap(_.items).flatMap(_.workflow.toList).flatMap(_.steps)
          val readySteps = totalSteps.count(_.status == StepStatus.Ready)
          val inProgressSteps = totalSteps.count(_.status == StepStatus.InProgress)

          List(
            summaryCard("📦", "Total Orders", totalOrders.toString, "mfg-card-blue"),
            summaryCard("⏳", "Pending Approval", pendingApproval.toString, "mfg-card-amber"),
            summaryCard("🔧", "In Production", inProduction.toString, "mfg-card-green"),
            summaryCard("✅", "Completed", completed.toString, "mfg-card-gray"),
            summaryCard("🔴", "Rush Orders", rushOrders.toString, "mfg-card-red"),
            summaryCard("●", "Ready Steps", readySteps.toString, "mfg-card-teal"),
            summaryCard("▶", "Active Steps", inProgressSteps.toString, "mfg-card-purple"),
          )
        },
      ),

      // Two-column layout: Recent Orders + Station Status
      div(
        cls := "mfg-dashboard-grid",

        // Recent orders
        div(
          cls := "mfg-dashboard-section",
          h3("Recent Orders"),
          child <-- state.map { s =>
            val recent = s.orders.sortBy(_.createdAt).reverse.take(5)
            renderRecentOrders(recent)
          },
        ),

        // Station status / utilisation
        div(
          cls := "mfg-dashboard-section",
          h3("Station Status & Utilisation"),
          child <-- state.map { s =>
            renderStationStatus(s)
          },
        ),
      ),
    )

  private def summaryCard(icon: String, label: String, value: String, colorCls: String): Element =
    div(
      cls := s"mfg-summary-card $colorCls",
      div(cls := "mfg-summary-icon", icon),
      div(
        cls := "mfg-summary-body",
        div(cls := "mfg-summary-value", value),
        div(cls := "mfg-summary-label", label),
      ),
    )

  private def renderRecentOrders(orders: List[ManufacturingOrder]): Element =
    val tbl = htmlTag("table")
    val thead = htmlTag("thead")
    val tbody = htmlTag("tbody")
    val tr = htmlTag("tr")
    val th = htmlTag("th")
    val td = htmlTag("td")

    tbl(
      cls := "mfg-table mfg-table-compact",
      thead(
        tr(
          th(cls := "mfg-th", "Order"),
          th(cls := "mfg-th", "Customer"),
          th(cls := "mfg-th", "Items"),
          th(cls := "mfg-th", "Status"),
          th(cls := "mfg-th", "Priority"),
          th(cls := "mfg-th", "Created"),
        ),
      ),
      tbody(
        orders.map { order =>
          val workflowStatus = if order.approval != ApprovalStatus.Approved then order.approval.label
            else if order.items.forall(_.workflow.exists(_.status == WorkflowStatus.Completed)) then "Completed"
            else if order.items.exists(_.workflow.exists(_.status == WorkflowStatus.InProgress)) then "In Production"
            else "Pending Start"
          tr(
            cls := "mfg-tr",
            onClick --> { _ =>
              ManufacturingViewModel.selectOrder(order.id)
              ManufacturingViewModel.navigateTo(
                if order.approval == ApprovalStatus.Pending || order.approval == ApprovalStatus.ChangesRequested
                then ManufacturingRoute.OrderQueue
                else ManufacturingRoute.WorkQueue
              )
            },
            td(cls := "mfg-td mfg-td-order", s"#${order.id.value}"),
            td(cls := "mfg-td", order.customerName),
            td(cls := "mfg-td mfg-td-num", order.items.size.toString),
            td(cls := "mfg-td",
              span(cls := s"mfg-approval-badge mfg-approval-${order.approval.toString.toLowerCase}", workflowStatus),
            ),
            td(cls := "mfg-td", priorityBadge(order.priority)),
            td(cls := "mfg-td mfg-td-date", order.createdAt),
          )
        },
      ),
    )

  private def renderStationStatus(state: ManufacturingState): Element =
    val allSteps = state.orders
      .filter(_.approval == ApprovalStatus.Approved)
      .flatMap(_.items)
      .flatMap(_.workflow.toList)
      .flatMap(_.steps)

    div(
      cls := "mfg-station-status-grid",
      StationType.values.toList.map { st =>
        val stepsForStation = allSteps.filter(_.stationType == st)
        val inProgress = stepsForStation.count(_.status == StepStatus.InProgress)
        val ready = stepsForStation.count(_.status == StepStatus.Ready)
        val completed = stepsForStation.count(_.status == StepStatus.Completed)
        val total = stepsForStation.size
        val utilPct = if total > 0 then ((completed + inProgress) * 100) / total else 0
        val isActive = inProgress > 0

        div(
          cls := s"mfg-station-card${if isActive then " active" else ""}",
          div(cls := "mfg-station-card-header",
            span(cls := "mfg-station-card-icon", st.icon),
            span(cls := "mfg-station-card-name", st.label),
          ),
          div(cls := "mfg-station-card-stats",
            div(cls := "mfg-station-stat",
              span(cls := "mfg-station-stat-val", inProgress.toString),
              span(cls := "mfg-station-stat-label", "Active"),
            ),
            div(cls := "mfg-station-stat",
              span(cls := "mfg-station-stat-val", ready.toString),
              span(cls := "mfg-station-stat-label", "Queue"),
            ),
            div(cls := "mfg-station-stat",
              span(cls := "mfg-station-stat-val", completed.toString),
              span(cls := "mfg-station-stat-label", "Done"),
            ),
          ),
          if total > 0 then
            div(cls := "mfg-station-bar-container",
              div(cls := "mfg-station-bar", styleAttr := s"width: $utilPct%"),
              span(cls := "mfg-station-bar-label", s"$utilPct%"),
            )
          else emptyNode,
        )
      },
    )

  private def priorityBadge(priority: Priority): Element =
    val cls_ = priority match
      case Priority.Rush   => "mfg-priority-badge rush"
      case Priority.Normal => "mfg-priority-badge normal"
      case Priority.Low    => "mfg-priority-badge low"
    span(cls := cls_, priority.label)
