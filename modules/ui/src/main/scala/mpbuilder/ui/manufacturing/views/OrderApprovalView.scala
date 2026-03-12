package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.ui.manufacturing.ManufacturingViewModel
import mpbuilder.uikit.containers.{SplitTableView, ColumnDef, RowAction}

object OrderApprovalView:

  def apply(): HtmlElement =
    val vm = ManufacturingViewModel

    val priorityFilterVar: Var[Set[OrderPriority]] = Var(Set.empty)

    val approvalOrders: Signal[List[ManufacturingOrder]] =
      vm.pendingApprovalOrders.combineWith(priorityFilterVar.signal).map {
        (orders: List[ManufacturingOrder], priorityFilter: Set[OrderPriority]) =>
          orders.filter(o => priorityFilter.isEmpty || priorityFilter.contains(o.priority))
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
        id       = "created",
        header   = Val("Received"),
        render   = o => span(formatRelative(o.createdAt)),
        sortKey  = Some(o => (-o.createdAt).toString),
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
        id       = "total",
        header   = Val("Total"),
        render   = o => span(
          s"${o.priceBreakdown.currency} ${o.priceBreakdown.total.value.setScale(2)}"
        ),
        sortKey  = Some(o => o.priceBreakdown.total.value.toString),
        widthCls = "col-narrow",
      ),
      ColumnDef(
        id       = "deadline",
        header   = Val("Deadline"),
        render   = o => span(o.deadline.map(ManufacturingViewModel.formatDeadline).getOrElse("—")),
        sortKey  = Some(o => o.deadline.map(_.toString).getOrElse("9" * 15)),
        widthCls = "col-narrow",
      ),
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
    )

    def rowActions(o: ManufacturingOrder): List[RowAction[ManufacturingOrder]] = List(
      RowAction[ManufacturingOrder](Val("Approve"), row => vm.approveOrder(row.id), isDestructive = false),
      RowAction[ManufacturingOrder](Val("Reject"),  row => vm.holdOrder(row.id),   isDestructive = true),
    )

    val filterBarEl: HtmlElement = div(
      cls := "filter-bar",
      span(cls := "filter-label", "Priority:"),
      List(OrderPriority.Urgent, OrderPriority.High, OrderPriority.Normal, OrderPriority.Low).map { p =>
        button(
          cls <-- priorityFilterVar.signal.map(f =>
            if f.contains(p) then "filter-chip filter-chip--active" else "filter-chip"
          ),
          p.toString,
          onClick --> { _ => priorityFilterVar.update(f => if f.contains(p) then f - p else f + p) },
        )
      },
    )

    SplitTableView[ManufacturingOrder](
      rows = approvalOrders,
      columns = columns,
      rowKey = _.id.value,
      searchable = searchText => approvalOrders.combineWith(searchText).map {
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
      detailPanel = selectedRow => approvalDetailPanel(selectedRow),
      rowActions = rowActions,
      emptyMessage = Val("No orders awaiting approval"),
    )

  private def approvalDetailPanel(selectedRow: Signal[Option[ManufacturingOrder]]): HtmlElement =
    div(
      cls := "detail-panel",
      child <-- selectedRow.map {
        case None => div()
        case Some(order) =>
          div(
            h3(cls := "detail-panel-title", s"Order ${order.orderId.value}"),
            p(cls := "detail-panel-customer", order.customerName),

            // Product specs
            h4("Product Specifications"),
            table(
              cls := "spec-table",
              tbody(
                tr(td("Category"),   td(order.configuration.category.name.value)),
                tr(td("Method"),     td(order.configuration.printingMethod.name.value)),
                tr(td("Quantity"),   td(order.quantity.toString)),
                tr(td("Components"), td(order.configuration.components.length.toString)),
              ),
            ),

            // Pricing
            h4("Pricing"),
            div(
              cls := "price-summary",
              div(cls := "price-row",
                span("Subtotal"),
                span(s"${order.priceBreakdown.currency} ${order.priceBreakdown.subtotal.value.setScale(2)}"),
              ),
              div(cls := "price-row price-row--total",
                span("Total"),
                span(s"${order.priceBreakdown.currency} ${order.priceBreakdown.total.value.setScale(2)}"),
              ),
            ),

            // Notes
            if order.notes.nonEmpty then
              div(h4("Notes"), p(cls := "detail-notes", order.notes))
            else emptyNode,

            // Attachments
            if order.attachments.nonEmpty then
              div(
                h4("Files"),
                ul(order.attachments.map(att => li(s"${att.name}"))),
              )
            else emptyNode,
          )
      },
    )

  private def formatRelative(ms: Long): String =
    val diff = System.currentTimeMillis() - ms
    val hours = diff / 3600000L
    if hours < 1 then "Just now"
    else if hours < 24 then s"${hours}h ago"
    else s"${hours / 24}d ago"
