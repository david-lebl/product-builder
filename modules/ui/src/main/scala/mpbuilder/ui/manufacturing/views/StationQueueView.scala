package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.ui.manufacturing.{ManufacturingViewModel, ManufacturingUiState}
import mpbuilder.uikit.containers.{SplitTableView, ColumnDef, RowAction}

object StationQueueView:

  def apply(): HtmlElement =
    val vm = ManufacturingViewModel

    // Filter state (local to this view)
    val statusFilterVar:   Var[Set[OrderStatus]]   = Var(Set.empty)
    val priorityFilterVar: Var[Set[OrderPriority]] = Var(Set.empty)

    // Orders that are actively in the workflow (have a current station)
    val activeOrders: Signal[List[ManufacturingOrder]] =
      vm.state.combineWith(statusFilterVar.signal).combineWith(priorityFilterVar.signal).map {
        (s: ManufacturingUiState, statusFilter: Set[OrderStatus], priorityFilter: Set[OrderPriority]) =>
          s.mfgState.orders
            .filter(o => o.currentStationId.isDefined && !o.isFullyCompleted)
            .filter(o => statusFilter.isEmpty  || o.currentStatus.exists(statusFilter.contains))
            .filter(o => priorityFilter.isEmpty || priorityFilter.contains(o.priority))
      }

    val columns: List[ColumnDef[ManufacturingOrder]] = List(
      ColumnDef(
        id       = "priority",
        header   = Val("Priority"),
        render   = o => span(
          cls := s"priority-badge priority-badge--${o.priority.toString.toLowerCase}",
          o.priority.toString,
        ),
        sortKey  = Some(o => o.priority.ordinal.toString),
        widthCls = "col-narrow",
      ),
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
        id       = "product",
        header   = Val("Product"),
        render   = o => span(s"${o.configuration.category.name.value} ×${o.quantity}"),
        sortKey  = Some(o => o.configuration.category.name.value),
      ),
      ColumnDef(
        id       = "station",
        header   = Val("Current Station"),
        render   = o =>
          o.currentStationId match
            case Some(sid) =>
              span(
                child.text <-- vm.state.map(s =>
                  s.mfgState.stations.find(_.id == sid).map(_.name.value).getOrElse(sid.value)
                )
              )
            case None => span("—"),
        widthCls = "col-medium",
      ),
      ColumnDef(
        id       = "status",
        header   = Val("Status"),
        render   = o => statusBadge(o),
        sortKey  = Some(o => o.currentStatus.map(_.toString).getOrElse("")),
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
        Option.when(currentStatus.contains(OrderStatus.InProgress) || currentStatus.contains(OrderStatus.Queued))(
          RowAction[ManufacturingOrder](Val("Hold"), row => vm.holdOrder(row.id), isDestructive = false)
        ),
        Option.when(currentStatus.contains(OrderStatus.OnHold))(
          RowAction[ManufacturingOrder](Val("Resume"), row => vm.resumeOrder(row.id), isDestructive = false)
        ),
      ).flatten

    val filterBarEl: HtmlElement = div(
      cls := "filter-bar",
      span(cls := "filter-label", "Status:"),
      List(OrderStatus.Queued, OrderStatus.InProgress, OrderStatus.OnHold).map { st =>
        button(
          cls <-- statusFilterVar.signal.map(f =>
            if f.contains(st) then "filter-chip filter-chip--active" else "filter-chip"
          ),
          st.toString,
          onClick --> { _ =>
            statusFilterVar.update(f => if f.contains(st) then f - st else f + st)
          },
        )
      },
      div(cls := "filter-divider"),
      span(cls := "filter-label", "Priority:"),
      List(OrderPriority.Urgent, OrderPriority.High, OrderPriority.Normal, OrderPriority.Low).map { p =>
        button(
          cls <-- priorityFilterVar.signal.map(f =>
            if f.contains(p) then "filter-chip filter-chip--active" else "filter-chip"
          ),
          p.toString,
          onClick --> { _ =>
            priorityFilterVar.update(f => if f.contains(p) then f - p else f + p)
          },
        )
      },
    )

    SplitTableView[ManufacturingOrder](
      rows = activeOrders,
      columns = columns,
      rowKey = _.id.value,
      searchable = searchText => activeOrders.combineWith(searchText).map {
        (orders: List[ManufacturingOrder], q: String) =>
          if q.isEmpty then orders
          else
            val lq = q.toLowerCase
            orders.filter { o =>
              o.customerName.toLowerCase.contains(lq) ||
              o.orderId.value.toLowerCase.contains(lq) ||
              o.configuration.category.name.value.toLowerCase.contains(lq)
            }
      },
      filterBar = Some(filterBarEl),
      detailPanel = selectedRow => stationQueuePanel(selectedRow, vm),
      rowActions = rowActions,
      emptyMessage = Val("No active orders in the queue"),
    )

  private def stationQueuePanel(selectedRow: Signal[Option[ManufacturingOrder]], vm: ManufacturingViewModel.type): HtmlElement =
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

            // Workflow step chain
            h4("Workflow"),
            div(
              cls := "step-chain",
              order.steps.map { step =>
                div(
                  cls := s"step-chip step-chip--${step.status.toString.toLowerCase}",
                  child.text <-- vm.stationName(step.stationId),
                )
              },
            ),

            // Notes
            if order.notes.nonEmpty then
              div(
                h4("Notes"),
                p(cls := "detail-notes", order.notes),
              )
            else emptyNode,

            // Attachments
            if order.attachments.nonEmpty then
              div(
                h4("Attachments"),
                ul(
                  order.attachments.map { att =>
                    li(s"${att.name} (${formatBytes(att.sizeBytes)})")
                  },
                ),
              )
            else emptyNode,
          )
      },
    )

  private def statusBadge(o: ManufacturingOrder): HtmlElement =
    val (label, cls2) = o.currentStatus match
      case Some(OrderStatus.InProgress) => ("In Progress", "status-badge--active")
      case Some(OrderStatus.Queued)     => ("Queued",      "status-badge--queued")
      case Some(OrderStatus.OnHold)     => ("On Hold",     "status-badge--hold")
      case Some(OrderStatus.Completed)  => ("Done",        "status-badge--done")
      case None                         => ("—",           "")
    span(cls := s"status-badge $cls2", label)

  private def deadlineCls(o: ManufacturingOrder): String =
    o.deadline match
      case None => ""
      case Some(d) =>
        val diff = d - System.currentTimeMillis()
        if diff < 0 then "deadline-overdue"
        else if diff < 86400000L then "deadline-urgent"
        else ""

  private def formatBytes(bytes: Long): String =
    if bytes < 1024 then s"${bytes}B"
    else if bytes < 1024 * 1024 then s"${bytes / 1024}KB"
    else s"${bytes / (1024 * 1024)}MB"
