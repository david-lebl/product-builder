package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Work Queue — the primary operator view.
  * Shows all Ready/InProgress steps across approved orders in a unified queue.
  * Linear workflow: operators pick up one step at a time and advance linearly.
  */
object WorkQueueView:

  /** A queue entry represents a single actionable workflow step */
  private case class QueueEntry(
      order: ManufacturingOrder,
      item: ManufacturingOrderItem,
      step: WorkflowStep,
      workflow: ManufacturingWorkflow,
  )

  def apply(): Element =
    val state = ManufacturingViewModel.state
    val filter = ManufacturingViewModel.stationFilter

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Work Queue"),
        p(cls := "mfg-view-subtitle", "Pick up and complete jobs at your stations"),
      ),

      // Station filter toggles
      div(
        cls := "mfg-station-filters",
        span(cls := "mfg-filter-label", "Filter by station:"),
        button(
          cls <-- filter.map(f => if f.isEmpty then "mfg-station-toggle active" else "mfg-station-toggle"),
          "All Stations",
          onClick --> { _ => ManufacturingViewModel.setStationFilter(None) },
        ),
        StationType.values.toList.map { st =>
          button(
            cls <-- filter.map(f =>
              if f.contains(st) then "mfg-station-toggle active" else "mfg-station-toggle"
            ),
            s"${st.icon} ${st.label}",
            onClick --> { _ =>
              ManufacturingViewModel.setStationFilter(
                if ManufacturingViewModel.currentState().stationFilter.contains(st) then None
                else Some(st)
              )
            },
          )
        },
      ),

      // Queue entries
      div(
        cls := "mfg-work-queue",
        children <-- state.combineWith(filter).map { case (s, stFilter) =>
          val entries = collectQueueEntries(s, stFilter)
          val (inProgress, ready) = entries.partition(_.step.status == StepStatus.InProgress)

          val sections = scala.collection.mutable.ListBuffer.empty[Element]

          if inProgress.nonEmpty then
            sections += div(
              cls := "mfg-queue-section",
              div(cls := "mfg-queue-section-title in-progress", s"▶ In Progress (${inProgress.size})"),
              inProgress.map(queueCard),
            )

          if ready.nonEmpty then
            sections += div(
              cls := "mfg-queue-section",
              div(cls := "mfg-queue-section-title ready", s"● Ready (${ready.size})"),
              ready.map(queueCard),
            )

          if sections.isEmpty then
            sections += div(cls := "mfg-empty-queue", "No jobs available for selected stations")

          sections.toList
        },
      ),
    )

  private def collectQueueEntries(state: ManufacturingState, stFilter: Option[StationType]): List[QueueEntry] =
    val entries = for
      order <- state.orders if order.approval == ApprovalStatus.Approved
      item  <- order.items
      wf    <- item.workflow.toList
      step  <- wf.steps if step.status == StepStatus.Ready || step.status == StepStatus.InProgress
      if stFilter.isEmpty || stFilter.contains(step.stationType)
    yield QueueEntry(order, item, step, wf)
    // Sort: InProgress first, then Rush > Normal > Low, then by order ID
    entries.sortBy(e => (
      if e.step.status == StepStatus.InProgress then 0 else 1,
      e.order.priority.ordinal,
      e.order.id.value,
    ))

  private def queueCard(entry: QueueEntry): Element =
    val isInProgress = entry.step.status == StepStatus.InProgress
    val completionPct = ManufacturingWorkflow.completionPercent(entry.workflow)

    div(
      cls := s"mfg-queue-card${if isInProgress then " in-progress" else ""}",

      // Card header
      div(
        cls := "mfg-queue-card-header",
        span(cls := "mfg-queue-order-id", s"Order #${entry.order.id.value}"),
        priorityBadge(entry.order.priority),
        span(cls := "mfg-queue-customer", entry.order.customerName),
      ),

      // Step info
      div(
        cls := "mfg-queue-step-info",
        span(cls := "mfg-step-icon", entry.step.stationType.icon),
        span(cls := "mfg-step-name", entry.step.stationType.label),
        entry.step.componentRole.map(r => span(cls := "mfg-step-component", s"(${r.toString})")).getOrElse(emptyNode),
      ),

      // Product description
      div(cls := "mfg-queue-product", entry.item.productDescription),

      // Progress bar
      div(
        cls := "mfg-progress-bar-container",
        div(cls := "mfg-progress-bar", styleAttr := s"width: $completionPct%"),
        span(cls := "mfg-progress-text", s"$completionPct% complete"),
      ),

      // Action buttons
      div(
        cls := "mfg-queue-actions",
        if isInProgress then
          button(
            cls := "mfg-btn mfg-btn-primary",
            "✓ Complete & Advance",
            onClick --> { _ =>
              ManufacturingViewModel.completeAndAdvance(entry.order.id, entry.item.itemIndex)
            },
          )
        else
          button(
            cls := "mfg-btn mfg-btn-secondary",
            "▶ Pick Up & Start",
            onClick --> { _ =>
              ManufacturingViewModel.pickupAndStart(entry.order.id, entry.item.itemIndex)
            },
          ),
      ),
    )

  private def priorityBadge(priority: Priority): Element =
    val cls_ = priority match
      case Priority.Rush   => "mfg-priority-badge rush"
      case Priority.Normal => "mfg-priority-badge normal"
      case Priority.Low    => "mfg-priority-badge low"
    span(cls := cls_, priority.label)
