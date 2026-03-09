package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Order Approval Queue — the gatekeeper view for incoming orders.
  * Staff reviews artwork, payment, feasibility before manufacturing begins.
  */
object OrderQueueView:

  def apply(): Element =
    val state = ManufacturingViewModel.state

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Order Approval Queue"),
        p(cls := "mfg-view-subtitle", "Review and approve incoming orders for manufacturing"),
      ),

      // Split view: order list + detail panel
      div(
        cls := "mfg-split-view",

        // Left: order list
        div(
          cls := "mfg-order-list",
          children <-- state.map { s =>
            val pendingOrders = s.orders.filter(o =>
              o.approval == ApprovalStatus.Pending || o.approval == ApprovalStatus.ChangesRequested
            )
            val otherOrders = s.orders.filter(o =>
              o.approval == ApprovalStatus.Approved || o.approval == ApprovalStatus.Rejected
            )

            val pendingCards = if pendingOrders.nonEmpty then
              div(
                cls := "mfg-list-section",
                div(cls := "mfg-list-section-title", s"Pending Approval (${pendingOrders.size})"),
                pendingOrders.map(orderCard(_, s.selectedOrderId)),
              ) :: Nil
            else Nil

            val otherCards = if otherOrders.nonEmpty then
              div(
                cls := "mfg-list-section",
                div(cls := "mfg-list-section-title", "Processed"),
                otherOrders.map(orderCard(_, s.selectedOrderId)),
              ) :: Nil
            else Nil

            pendingCards ++ otherCards
          },
        ),

        // Right: selected order detail
        div(
          cls := "mfg-detail-panel",
          child <-- ManufacturingViewModel.selectedOrder.map {
            case Some(order) => orderDetail(order)
            case None => emptyDetail("Select an order to review")
          },
        ),
      ),
    )

  private def orderCard(order: ManufacturingOrder, selectedId: Option[OrderId]): Element =
    val isSelected = selectedId.contains(order.id)
    div(
      cls := s"mfg-order-card${if isSelected then " selected" else ""}",
      onClick --> { _ => ManufacturingViewModel.selectOrder(order.id) },

      div(
        cls := "mfg-order-card-header",
        span(cls := "mfg-order-id", s"Order #${order.id.value}"),
        priorityBadge(order.priority),
      ),
      div(cls := "mfg-order-customer", order.customerName),
      div(cls := "mfg-order-items-count", s"${order.items.size} item${if order.items.size != 1 then "s" else ""}"),
      div(
        cls := s"mfg-approval-badge mfg-approval-${order.approval.toString.toLowerCase}",
        order.approval.label,
      ),
    )

  private def orderDetail(order: ManufacturingOrder): Element =
    div(
      cls := "mfg-order-detail",

      // Header
      div(
        cls := "mfg-detail-header",
        h3(s"Order #${order.id.value}"),
        div(cls := "mfg-detail-customer", s"Customer: ${order.customerName}"),
        div(
          cls := s"mfg-approval-badge mfg-approval-${order.approval.toString.toLowerCase}",
          order.approval.label,
        ),
      ),

      // Items list
      div(
        cls := "mfg-detail-section",
        h4("Order Items"),
        order.items.map { item =>
          div(
            cls := "mfg-item-row",
            div(cls := "mfg-item-desc", item.productDescription),
            div(cls := "mfg-item-qty", s"Qty: ${item.quantity}"),
          )
        },
      ),

      // Notes
      if order.notes.nonEmpty then
        div(
          cls := "mfg-detail-section",
          h4("Notes"),
          p(cls := "mfg-order-notes", order.notes),
        )
      else emptyNode,

      // Priority selector
      div(
        cls := "mfg-detail-section",
        h4("Priority"),
        div(
          cls := "mfg-priority-selector",
          Priority.values.toList.map { p =>
            button(
              cls := s"mfg-priority-btn mfg-priority-${p.toString.toLowerCase}${if order.priority == p then " active" else ""}",
              p.label,
              onClick --> { _ => ManufacturingViewModel.setPriority(order.id, p) },
            )
          },
        ),
      ),

      // Action buttons (only for pending/changes-requested orders)
      if order.approval == ApprovalStatus.Pending || order.approval == ApprovalStatus.ChangesRequested then
        div(
          cls := "mfg-detail-actions",
          button(
            cls := "mfg-btn mfg-btn-primary",
            "✓ Approve & Generate Workflow",
            onClick --> { _ => ManufacturingViewModel.approveOrder(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning",
            "↩ Request Changes",
            onClick --> { _ => ManufacturingViewModel.requestChanges(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-danger",
            "✗ Reject",
            onClick --> { _ => ManufacturingViewModel.rejectOrder(order.id) },
          ),
        )
      else emptyNode,
    )

  private def emptyDetail(message: String): Element =
    div(cls := "mfg-empty-detail", span(message))

  private def priorityBadge(priority: Priority): Element =
    val cls_ = priority match
      case Priority.Rush   => "mfg-priority-badge rush"
      case Priority.Normal => "mfg-priority-badge normal"
      case Priority.Low    => "mfg-priority-badge low"
    span(cls := cls_, priority.label)
