package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Work Queue — table-based operator view with detail side panel.
  * Shows all Ready/InProgress steps across approved orders in a filterable, sortable table.
  */
object WorkQueueView:

  /** A queue entry represents a single actionable workflow step */
  private case class QueueEntry(
      order: ManufacturingOrder,
      item: ManufacturingOrderItem,
      step: WorkflowStep,
      workflow: ManufacturingWorkflow,
      recommendedOrder: Int,
  )

  def apply(): Element =
    val state = ManufacturingViewModel.state
    val filter = ManufacturingViewModel.stationFilter
    val search = ManufacturingViewModel.searchQuery

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Work Queue"),
        p(cls := "mfg-view-subtitle", "Pick up and complete jobs at your stations"),
      ),

      // Toolbar: search + station filters
      div(
        cls := "mfg-toolbar",
        // Search field
        div(
          cls := "mfg-search-field",
          input(
            typ := "text",
            cls := "mfg-search-input",
            placeholder := "Search orders, products, customers…",
            value <-- search,
            onInput.mapToValue --> { v => ManufacturingViewModel.setSearchQuery(v) },
          ),
        ),
        // Multi-station filter
        div(
          cls := "mfg-station-filters",
          span(cls := "mfg-filter-label", "Stations:"),
          button(
            cls <-- filter.map(f => if f.isEmpty then "mfg-station-toggle active" else "mfg-station-toggle"),
            "All",
            onClick --> { _ => ManufacturingViewModel.clearStationFilter() },
          ),
          StationType.values.toList.map { st =>
            button(
              cls <-- filter.map(f =>
                if f.contains(st) then "mfg-station-toggle active" else "mfg-station-toggle"
              ),
              s"${st.icon} ${st.label}",
              onClick --> { _ => ManufacturingViewModel.toggleStationFilter(st) },
            )
          },
        ),
      ),

      // Main content: table + detail panel
      div(
        cls := "mfg-table-layout",

        // Table
        div(
          cls := "mfg-table-container",
          child <-- state.map { s =>
            val stFilter = s.stationFilter
            val searchQ = s.searchQuery
            val entries = collectQueueEntries(s, stFilter, searchQ)
            if entries.isEmpty then
              div(cls := "mfg-empty-queue", "No jobs match your filters")
            else
              renderTable(entries, s.selectedOrderId, s.selectedItemIndex)
          },
        ),

        // Detail side panel
        div(
          cls := "mfg-side-panel",
          child <-- state.map { s =>
            (s.selectedOrderId, s.selectedItemIndex) match
              case (Some(orderId), Some(itemIdx)) =>
                s.orders.find(_.id == orderId) match
                  case Some(order) =>
                    order.items.find(_.itemIndex == itemIdx) match
                      case Some(item) => renderDetailPanel(order, item)
                      case None => emptyPanel("Select a row to view details")
                  case None => emptyPanel("Select a row to view details")
              case (Some(orderId), None) =>
                s.orders.find(_.id == orderId) match
                  case Some(order) => renderOrderOverview(order)
                  case None => emptyPanel("Select a row to view details")
              case _ => emptyPanel("Select a row to view details")
          },
        ),
      ),
    )

  private def collectQueueEntries(state: ManufacturingState, stFilter: Set[StationType], searchQ: String): List[QueueEntry] =
    val lowerSearch = searchQ.toLowerCase.trim
    var rank = 0
    val entries = for
      order <- state.orders if order.approval == ApprovalStatus.Approved
      item  <- order.items
      wf    <- item.workflow.toList
      step  <- wf.steps if step.status == StepStatus.Ready || step.status == StepStatus.InProgress
      if stFilter.isEmpty || stFilter.contains(step.stationType)
      if lowerSearch.isEmpty || order.id.value.toLowerCase.contains(lowerSearch) ||
         order.customerName.toLowerCase.contains(lowerSearch) ||
         item.productDescription.toLowerCase.contains(lowerSearch) ||
         item.materialDescription.toLowerCase.contains(lowerSearch) ||
         step.stationType.label.toLowerCase.contains(lowerSearch)
    yield
      rank += 1
      QueueEntry(order, item, step, wf, rank)
    entries.sortBy(e => (
      if e.step.status == StepStatus.InProgress then 0 else 1,
      e.order.priority.ordinal,
      e.order.id.value,
    ))

  private def renderTable(entries: List[QueueEntry], selectedOrderId: Option[OrderId], selectedItemIdx: Option[Int]): Element =
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
          th(cls := "mfg-th", "#"),
          th(cls := "mfg-th", "Order"),
          th(cls := "mfg-th", "Customer"),
          th(cls := "mfg-th", "Product"),
          th(cls := "mfg-th", "Material"),
          th(cls := "mfg-th", "Station"),
          th(cls := "mfg-th", "Status"),
          th(cls := "mfg-th", "Priority"),
          th(cls := "mfg-th", "Created"),
          th(cls := "mfg-th", "Deadline"),
          th(cls := "mfg-th", "Actions"),
        ),
      ),
      tbody(
        entries.map { entry =>
          val isSelected = selectedOrderId.contains(entry.order.id) && selectedItemIdx.contains(entry.item.itemIndex)
          val isInProgress = entry.step.status == StepStatus.InProgress
          tr(
            cls := s"mfg-tr${if isSelected then " selected" else ""}${if isInProgress then " in-progress" else ""}",
            onClick --> { _ => ManufacturingViewModel.selectOrderItem(entry.order.id, entry.item.itemIndex) },

            td(cls := "mfg-td mfg-td-num", entry.recommendedOrder.toString),
            td(cls := "mfg-td mfg-td-order", s"#${entry.order.id.value}"),
            td(cls := "mfg-td", entry.order.customerName),
            td(cls := "mfg-td", entry.item.productDescription),
            td(cls := "mfg-td mfg-td-material", entry.item.materialDescription),
            td(cls := "mfg-td",
              span(cls := "mfg-station-chip", s"${entry.step.stationType.icon} ${entry.step.stationType.label}"),
              entry.step.componentRole.map(r => span(cls := "mfg-step-component", s"(${r.toString})")).getOrElse(emptyNode),
            ),
            td(cls := "mfg-td", statusBadge(entry.step.status)),
            td(cls := "mfg-td", priorityBadge(entry.order.priority)),
            td(cls := "mfg-td mfg-td-date", entry.order.createdAt),
            td(cls := "mfg-td mfg-td-date", entry.order.deadline.getOrElse("—")),
            td(cls := "mfg-td mfg-td-actions",
              if isInProgress then
                button(
                  cls := "mfg-btn mfg-btn-sm mfg-btn-primary",
                  "✓ Complete",
                  onClick.stopPropagation --> { _ =>
                    ManufacturingViewModel.completeAndAdvance(entry.order.id, entry.item.itemIndex)
                  },
                )
              else
                button(
                  cls := "mfg-btn mfg-btn-sm mfg-btn-secondary",
                  "▶ Start",
                  onClick.stopPropagation --> { _ =>
                    ManufacturingViewModel.pickupAndStart(entry.order.id, entry.item.itemIndex)
                  },
                ),
            ),
          )
        },
      ),
    )

  private def renderDetailPanel(order: ManufacturingOrder, item: ManufacturingOrderItem): Element =
    div(
      cls := "mfg-detail-content",

      // Close button
      div(
        cls := "mfg-detail-close-row",
        button(cls := "mfg-detail-close", "✕", onClick --> { _ => ManufacturingViewModel.deselectOrder() }),
      ),

      // Header
      div(
        cls := "mfg-detail-header",
        h3(s"Order #${order.id.value}"),
        priorityBadge(order.priority),
      ),
      div(cls := "mfg-detail-customer", s"Customer: ${order.customerName}"),
      div(cls := "mfg-detail-meta",
        span(s"Created: ${order.createdAt}"),
        order.deadline.map(d => span(s" | Deadline: $d")).getOrElse(emptyNode),
      ),

      // Product info
      div(
        cls := "mfg-detail-section",
        h4("Product Details"),
        div(cls := "mfg-detail-row", span(cls := "mfg-detail-label", "Product:"), span(item.productDescription)),
        div(cls := "mfg-detail-row", span(cls := "mfg-detail-label", "Material:"), span(item.materialDescription)),
        div(cls := "mfg-detail-row", span(cls := "mfg-detail-label", "Quantity:"), span(item.quantity.toString)),
      ),

      // Files
      if item.files.nonEmpty then
        div(
          cls := "mfg-detail-section",
          h4("Files"),
          div(
            cls := "mfg-files-list",
            item.files.map { f =>
              a(
                cls := "mfg-file-link",
                href := f.url,
                span(cls := "mfg-file-icon", if f.fileType == "artwork" then "🎨" else "📄"),
                span(f.name),
                span(cls := "mfg-file-type", f.fileType),
              )
            },
          ),
        )
      else emptyNode,

      // Workflow timeline
      item.workflow match
        case Some(wf) =>
          div(
            cls := "mfg-detail-section",
            h4("Workflow Timeline"),
            div(
              cls := "mfg-timeline",
              wf.steps.map { step =>
                val statusCls = step.status match
                  case StepStatus.Completed  => "completed"
                  case StepStatus.InProgress => "in-progress"
                  case StepStatus.Ready      => "ready"
                  case StepStatus.Failed     => "failed"
                  case StepStatus.Skipped    => "skipped"
                  case StepStatus.Waiting    => "waiting"
                div(
                  cls := s"mfg-timeline-step $statusCls",
                  div(cls := "mfg-timeline-dot"),
                  div(
                    cls := "mfg-timeline-info",
                    span(cls := "mfg-timeline-station", s"${step.stationType.icon} ${step.stationType.label}"),
                    step.componentRole.map(r => span(cls := "mfg-timeline-role", s" (${r.toString})")).getOrElse(emptyNode),
                    span(cls := s"mfg-step-status-sm $statusCls", step.status.label),
                  ),
                )
              },
            ),
            div(
              cls := "mfg-progress-bar-container",
              div(cls := "mfg-progress-bar", styleAttr := s"width: ${ManufacturingWorkflow.completionPercent(wf)}%"),
            ),
            span(cls := "mfg-progress-text", s"${ManufacturingWorkflow.completionPercent(wf)}% complete"),
          )
        case None => emptyNode,

      // Other items in this order
      if order.items.size > 1 then
        div(
          cls := "mfg-detail-section",
          h4("Other Items in This Order"),
          order.items.filter(_.itemIndex != item.itemIndex).map { other =>
            div(
              cls := "mfg-other-item",
              div(cls := "mfg-other-item-desc", other.productDescription),
              div(cls := "mfg-other-item-meta", s"${other.materialDescription} | Qty: ${other.quantity}"),
              other.workflow match
                case Some(wf) =>
                  div(
                    cls := "mfg-mini-pipeline",
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
                case None => span(cls := "mfg-other-item-meta", "No workflow yet"),
            )
          },
        )
      else emptyNode,

      // Notes
      if order.notes.nonEmpty then
        div(
          cls := "mfg-detail-section",
          h4("Notes"),
          p(cls := "mfg-order-notes", order.notes),
        )
      else emptyNode,
    )

  private def renderOrderOverview(order: ManufacturingOrder): Element =
    div(
      cls := "mfg-detail-content",
      div(
        cls := "mfg-detail-close-row",
        button(cls := "mfg-detail-close", "✕", onClick --> { _ => ManufacturingViewModel.deselectOrder() }),
      ),
      div(cls := "mfg-detail-header", h3(s"Order #${order.id.value}"), priorityBadge(order.priority)),
      div(cls := "mfg-detail-customer", s"Customer: ${order.customerName}"),
      div(cls := "mfg-detail-section",
        h4("Items"),
        order.items.map { item =>
          div(cls := "mfg-item-row",
            div(cls := "mfg-item-desc", item.productDescription),
            div(cls := "mfg-item-qty", s"Qty: ${item.quantity}"),
          )
        },
      ),
    )

  private def emptyPanel(msg: String): Element =
    div(cls := "mfg-side-panel-empty", span(msg))

  private def priorityBadge(priority: Priority): Element =
    val cls_ = priority match
      case Priority.Rush   => "mfg-priority-badge rush"
      case Priority.Normal => "mfg-priority-badge normal"
      case Priority.Low    => "mfg-priority-badge low"
    span(cls := cls_, priority.label)

  private def statusBadge(status: StepStatus): Element =
    val cls_ = status match
      case StepStatus.InProgress => "mfg-status-badge in-progress"
      case StepStatus.Ready      => "mfg-status-badge ready"
      case StepStatus.Completed  => "mfg-status-badge completed"
      case StepStatus.Failed     => "mfg-status-badge failed"
      case _                     => "mfg-status-badge"
    span(cls := cls_, status.label)
