package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Order Approval Queue — table-based view with search, filters, and detail side panel. */
object OrderQueueView:

  def apply(): Element =
    val state = ManufacturingViewModel.state
    val search = ManufacturingViewModel.searchQuery

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Order Approval Queue"),
        p(cls := "mfg-view-subtitle", "Review and approve incoming orders for manufacturing"),
      ),

      // Toolbar: search
      div(
        cls := "mfg-toolbar",
        div(
          cls := "mfg-search-field",
          input(
            typ := "text",
            cls := "mfg-search-input",
            placeholder := "Search orders, customers, products…",
            value <-- search,
            onInput.mapToValue --> { v => ManufacturingViewModel.setSearchQuery(v) },
          ),
        ),
      ),

      // Main content: table + detail panel
      div(
        cls := "mfg-table-layout",

        // Table
        div(
          cls := "mfg-table-container",
          child <-- state.map { s =>
            val filtered = filterOrders(s.orders, s.searchQuery)
            if filtered.isEmpty then
              div(cls := "mfg-empty-queue", "No orders match your search")
            else
              renderTable(filtered, s.selectedOrderId)
          },
        ),

        // Detail side panel
        div(
          cls := "mfg-side-panel",
          child <-- ManufacturingViewModel.selectedOrder.map {
            case Some(order) => orderDetail(order)
            case None => div(cls := "mfg-side-panel-empty", span("Select an order to review"))
          },
        ),
      ),
    )

  private def filterOrders(orders: List[ManufacturingOrder], searchQ: String): List[ManufacturingOrder] =
    val q = searchQ.toLowerCase.trim
    if q.isEmpty then orders
    else orders.filter { o =>
      o.id.value.toLowerCase.contains(q) ||
      o.customerName.toLowerCase.contains(q) ||
      o.items.exists(_.productDescription.toLowerCase.contains(q)) ||
      o.items.exists(_.materialDescription.toLowerCase.contains(q)) ||
      o.approval.label.toLowerCase.contains(q) ||
      o.priority.label.toLowerCase.contains(q)
    }

  private def renderTable(orders: List[ManufacturingOrder], selectedId: Option[OrderId]): Element =
    val tbl = htmlTag("table")
    val thead = htmlTag("thead")
    val tbody = htmlTag("tbody")
    val tr = htmlTag("tr")
    val th = htmlTag("th")
    val td = htmlTag("td")

    tbl(
      cls := "mfg-table",
      thead(
        tr(
          th(cls := "mfg-th", "Order"),
          th(cls := "mfg-th", "Customer"),
          th(cls := "mfg-th", "Items"),
          th(cls := "mfg-th", "Products"),
          th(cls := "mfg-th", "Status"),
          th(cls := "mfg-th", "Priority"),
          th(cls := "mfg-th", "Created"),
          th(cls := "mfg-th", "Deadline"),
          th(cls := "mfg-th", "Actions"),
        ),
      ),
      tbody(
        orders.map { order =>
          val isSelected = selectedId.contains(order.id)
          val isPending = order.approval == ApprovalStatus.Pending || order.approval == ApprovalStatus.ChangesRequested
          tr(
            cls := s"mfg-tr${if isSelected then " selected" else ""}${if isPending then " pending" else ""}",
            onClick --> { _ => ManufacturingViewModel.selectOrder(order.id) },

            td(cls := "mfg-td mfg-td-order", s"#${order.id.value}"),
            td(cls := "mfg-td", order.customerName),
            td(cls := "mfg-td mfg-td-num", order.items.size.toString),
            td(cls := "mfg-td", order.items.map(_.productDescription).mkString(", ")),
            td(cls := "mfg-td", approvalBadge(order.approval)),
            td(cls := "mfg-td", priorityBadge(order.priority)),
            td(cls := "mfg-td mfg-td-date", order.createdAt),
            td(cls := "mfg-td mfg-td-date", order.deadline.getOrElse("—")),
            td(cls := "mfg-td mfg-td-actions",
              if isPending then
                div(
                  cls := "mfg-inline-actions",
                  button(cls := "mfg-btn mfg-btn-sm mfg-btn-primary", "✓",
                    title := "Approve",
                    onClick.stopPropagation --> { _ => ManufacturingViewModel.approveOrder(order.id) }),
                  button(cls := "mfg-btn mfg-btn-sm mfg-btn-warning", "↩",
                    title := "Request Changes",
                    onClick.stopPropagation --> { _ => ManufacturingViewModel.requestChanges(order.id) }),
                  button(cls := "mfg-btn mfg-btn-sm mfg-btn-danger", "✗",
                    title := "Reject",
                    onClick.stopPropagation --> { _ => ManufacturingViewModel.rejectOrder(order.id) }),
                )
              else emptyNode,
            ),
          )
        },
      ),
    )

  private def orderDetail(order: ManufacturingOrder): Element =
    div(
      cls := "mfg-detail-content",
      div(
        cls := "mfg-detail-close-row",
        button(cls := "mfg-detail-close", "✕", onClick --> { _ => ManufacturingViewModel.deselectOrder() }),
      ),
      div(cls := "mfg-detail-header",
        h3(s"Order #${order.id.value}"),
        approvalBadge(order.approval),
        priorityBadge(order.priority),
      ),
      div(cls := "mfg-detail-customer", s"Customer: ${order.customerName}"),
      div(cls := "mfg-detail-meta",
        span(s"Created: ${order.createdAt}"),
        order.deadline.map(d => span(s" | Deadline: $d")).getOrElse(emptyNode),
      ),

      // Items
      div(
        cls := "mfg-detail-section",
        h4("Order Items"),
        order.items.map { item =>
          div(
            cls := "mfg-detail-item-card",
            div(cls := "mfg-detail-row", span(cls := "mfg-detail-label", "Product:"), span(item.productDescription)),
            div(cls := "mfg-detail-row", span(cls := "mfg-detail-label", "Material:"), span(item.materialDescription)),
            div(cls := "mfg-detail-row", span(cls := "mfg-detail-label", "Quantity:"), span(item.quantity.toString)),
            // Files
            if item.files.nonEmpty then
              div(cls := "mfg-files-list",
                item.files.map { f =>
                  a(cls := "mfg-file-link", href := f.url,
                    span(cls := "mfg-file-icon", if f.fileType == "artwork" then "🎨" else "📄"),
                    span(f.name),
                  )
                },
              )
            else emptyNode,
            // Workflow if present
            item.workflow match
              case Some(wf) =>
                div(cls := "mfg-mini-pipeline",
                  wf.steps.map { step =>
                    val sc = step.status match
                      case StepStatus.Completed  => "completed"
                      case StepStatus.InProgress => "in-progress"
                      case StepStatus.Ready      => "ready"
                      case _                     => "waiting"
                    val stepTitle = s"${step.stationType.label}: ${step.status.label}"
                    span(
                      cls := s"mfg-mini-dot $sc",
                      title := stepTitle,
                    )
                  },
                )
              case None => emptyNode,
          )
        },
      ),

      // Notes
      if order.notes.nonEmpty then
        div(cls := "mfg-detail-section",
          h4("Notes"),
          p(cls := "mfg-order-notes", order.notes),
        )
      else emptyNode,

      // Priority selector
      div(
        cls := "mfg-detail-section",
        h4("Priority"),
        div(cls := "mfg-priority-selector",
          Priority.values.toList.map { p =>
            button(
              cls := s"mfg-priority-btn mfg-priority-${p.toString.toLowerCase}${if order.priority == p then " active" else ""}",
              p.label,
              onClick --> { _ => ManufacturingViewModel.setPriority(order.id, p) },
            )
          },
        ),
      ),

      // Action buttons
      if order.approval == ApprovalStatus.Pending || order.approval == ApprovalStatus.ChangesRequested then
        div(
          cls := "mfg-detail-actions",
          button(cls := "mfg-btn mfg-btn-primary", "✓ Approve & Generate Workflow",
            onClick --> { _ => ManufacturingViewModel.approveOrder(order.id) }),
          button(cls := "mfg-btn mfg-btn-warning", "↩ Request Changes",
            onClick --> { _ => ManufacturingViewModel.requestChanges(order.id) }),
          button(cls := "mfg-btn mfg-btn-danger", "✗ Reject",
            onClick --> { _ => ManufacturingViewModel.rejectOrder(order.id) }),
        )
      else emptyNode,
    )

  private def approvalBadge(approval: ApprovalStatus): Element =
    span(
      cls := s"mfg-approval-badge mfg-approval-${approval.toString.toLowerCase}",
      approval.label,
    )

  private def priorityBadge(priority: Priority): Element =
    val cls_ = priority match
      case Priority.Rush   => "mfg-priority-badge rush"
      case Priority.Normal => "mfg-priority-badge normal"
      case Priority.Low    => "mfg-priority-badge low"
    span(cls := cls_, priority.label)
