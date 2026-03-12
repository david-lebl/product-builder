package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.ui.manufacturing.{ManufacturingViewModel, ManufacturingUiState}
import mpbuilder.uikit.containers.{SplitTableView, ColumnDef, RowAction}

object OrderProgressView:

  def apply(): HtmlElement =
    val vm = ManufacturingViewModel

    val statusFilterVar: Var[Set[OrderStatus]] = Var(Set.empty)

    // Orders in workflow or fully completed, filtered by selected status chips
    val progressOrders: Signal[List[ManufacturingOrder]] =
      vm.state.combineWith(statusFilterVar.signal).map {
        (s: ManufacturingUiState, statusFilter: Set[OrderStatus]) =>
          s.mfgState.orders
            .filter(o => o.currentStationId.isDefined || o.isFullyCompleted)
            .filter(o =>
              statusFilter.isEmpty ||
              o.currentStatus.exists(statusFilter.contains) ||
              (statusFilter.contains(OrderStatus.Completed) && o.isFullyCompleted && o.currentStationId.isEmpty)
            )
      }

    val columns: List[ColumnDef[ManufacturingOrder]] = List(
      ColumnDef(
        id       = "orderId",
        header   = Val("Order ID"),
        render   = o => span(o.orderId.value),
        sortKey  = Some(_.orderId.value),
        widthCls = "col-narrow",
      ),
      ColumnDef(
        id       = "customer",
        header   = Val("Customer"),
        render   = o => span(o.customerName),
        sortKey  = Some(_.customerName),
      ),
      ColumnDef(
        id       = "progress",
        header   = Val("Progress"),
        render   = o => progressBar(o),
        widthCls = "col-wide",
      ),
      ColumnDef(
        id       = "bottleneck",
        header   = Val("Current Station"),
        render   = o =>
          o.currentStationId match
            case Some(sid) =>
              span(
                child.text <-- vm.state.map(s =>
                  s.mfgState.stations.find(_.id == sid).map(_.name.value).getOrElse(sid.value)
                )
              )
            case None =>
              span(if o.isFullyCompleted then "Complete" else "—"),
        widthCls = "col-medium",
      ),
      ColumnDef(
        id       = "status",
        header   = Val("Status"),
        render   = o =>
          val (label, cls2) = o.currentStatus match
            case Some(OrderStatus.InProgress) => ("In Progress", "status-badge--active")
            case Some(OrderStatus.Queued)     => ("Queued",      "status-badge--queued")
            case Some(OrderStatus.OnHold)     => ("On Hold",     "status-badge--hold")
            case Some(OrderStatus.Completed)  => ("At Station",  "status-badge--done")
            case None =>
              if o.isFullyCompleted then ("Completed", "status-badge--done")
              else ("Pending", "")
          span(cls := s"status-badge $cls2", label),
        widthCls = "col-narrow",
      ),
      ColumnDef(
        id       = "deadline",
        header   = Val("Deadline"),
        render   = o => span(
          cls := deadlineCls(o),
          o.deadline.map(ManufacturingViewModel.formatDeadline).getOrElse("—"),
        ),
        sortKey  = Some(o => o.deadline.map(_.toString).getOrElse("9" * 15)),
        widthCls = "col-narrow",
      ),
    )

    def rowActions(o: ManufacturingOrder): List[RowAction[ManufacturingOrder]] =
      val currentStatus = o.currentStatus
      List(
        Option.when(currentStatus.contains(OrderStatus.Queued))(
          RowAction[ManufacturingOrder](Val("Start"), row => vm.startOrder(row.id), isDestructive = false)
        ),
        Option.when(currentStatus.contains(OrderStatus.InProgress))(
          RowAction[ManufacturingOrder](Val("Complete"), row => vm.completeOrder(row.id), isDestructive = false)
        ),
        Option.when(currentStatus.contains(OrderStatus.OnHold))(
          RowAction[ManufacturingOrder](Val("Resume"), row => vm.resumeOrder(row.id), isDestructive = false)
        ),
      ).flatten

    val filterBarEl: HtmlElement = div(
      cls := "filter-bar",
      span(cls := "filter-label", "Status:"),
      List(OrderStatus.InProgress, OrderStatus.OnHold, OrderStatus.Completed).map { st =>
        button(
          cls <-- statusFilterVar.signal.map(f =>
            if f.contains(st) then "filter-chip filter-chip--active" else "filter-chip"
          ),
          st.toString,
          onClick --> { _ => statusFilterVar.update(f => if f.contains(st) then f - st else f + st) },
        )
      },
    )

    SplitTableView[ManufacturingOrder](
      rows = progressOrders,
      columns = columns,
      rowKey = _.id.value,
      searchable = searchText => progressOrders.combineWith(searchText).map {
        (orders: List[ManufacturingOrder], q: String) =>
          if q.isEmpty then orders
          else
            val lq = q.toLowerCase
            orders.filter { o =>
              o.customerName.toLowerCase.contains(lq) ||
              o.orderId.value.toLowerCase.contains(lq)
            }
      },
      filterBar = Some(filterBarEl),
      detailPanel = selectedRow => progressDetailPanel(selectedRow, vm),
      rowActions = rowActions,
      emptyMessage = Val("No orders in progress"),
    )

  private def progressBar(order: ManufacturingOrder): HtmlElement =
    val total = order.totalSteps
    val done  = order.stepsCompleted
    val pct   = if total == 0 then 0 else (done * 100) / total
    div(
      cls := "progress-bar-container",
      div(
        cls := "progress-bar-track",
        div(cls := "progress-bar-fill", styleAttr := s"width: ${pct}%"),
      ),
      span(cls := "progress-bar-label", s"$done/$total"),
    )

  private def progressDetailPanel(
      selectedRow: Signal[Option[ManufacturingOrder]],
      vm: ManufacturingViewModel.type,
  ): HtmlElement =
    div(
      cls := "detail-panel",
      child <-- selectedRow.map {
        case None => div()
        case Some(order) =>
          div(
            h3(cls := "detail-panel-title", s"Order ${order.orderId.value}"),
            p(cls := "detail-panel-customer", order.customerName),
            p(cls := "detail-panel-product",
              s"${order.configuration.category.name.value} ×${order.quantity}"),

            // Step-by-step chain
            h4("Production Steps"),
            div(
              cls := "step-chain step-chain--vertical",
              order.steps.map { step =>
                div(
                  cls := s"step-row step-row--${step.status.toString.toLowerCase}",
                  div(
                    cls := s"step-dot step-dot--${step.status.toString.toLowerCase}",
                  ),
                  div(
                    cls := "step-row-content",
                    div(
                      cls        := "step-row-name",
                      child.text <-- vm.stationName(step.stationId),
                    ),
                    div(cls := "step-row-status", step.status.toString),
                  ),
                )
              },
            ),

            // Fulfilment checklist
            h4("Fulfilment Checklist"),
            div(
              cls := "checklist",
              checklistItem("Collected from last station", order.isFullyCompleted),
              checklistItem("Quality control passed",      order.steps.lastOption.exists(_.status == OrderStatus.Completed)),
              checklistItem("Packaged",                    order.isFullyCompleted),
              checklistItem("Ready for dispatch",          order.isFullyCompleted),
            ),
          )
      },
    )

  private def checklistItem(label: String, checked: Boolean): HtmlElement =
    div(
      cls := s"checklist-item${if checked then " checklist-item--done" else ""}",
      span(cls := "checklist-check", if checked then "✓" else "○"),
      span(cls := "checklist-label", label),
    )

  private def deadlineCls(o: ManufacturingOrder): String =
    o.deadline match
      case None => ""
      case Some(d) =>
        val diff = d - System.currentTimeMillis()
        if diff < 0 then "deadline-overdue"
        else if diff < 86400000L then "deadline-urgent"
        else ""
