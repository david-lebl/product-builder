package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.StationType.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.model.ManufacturingWorkflow.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.uikit.containers.*

/** Station Queue View — primary operator view using SplitTableView. */
object StationQueueView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedStepId: Var[Option[String]] = Var(None)

    val stationFilterDef = FilterDef(
      id = "station",
      label = "Station",
      options = Val(StationType.values.toList.map(st => (st.toString, st.displayName))),
      selectedValues = Var(StationType.values.toSet.map(_.toString)),
    )

    val statusFilterDef = FilterDef(
      id = "status",
      label = "Status",
      options = Val(List(
        (StepStatus.Ready.toString, "Ready"),
        (StepStatus.InProgress.toString, "In Progress"),
      )),
      selectedValues = Var(Set(StepStatus.Ready.toString, StepStatus.InProgress.toString)),
    )

    val priorityFilterDef = FilterDef(
      id = "priority",
      label = "Priority",
      options = Val(Priority.values.toList.map(p => (p.toString, p.displayName))),
      selectedValues = Var(Priority.values.toSet.map(_.toString)),
    )

    // Filtered queue items
    val filteredItems: Signal[List[QueueItem]] =
      val filterState = stationFilterDef.selectedValues.signal
        .combineWith(statusFilterDef.selectedValues.signal, priorityFilterDef.selectedValues.signal)
      ManufacturingViewModel.queueItems
        .combineWith(filterState, searchVar.signal)
        .map { case (items, (stFilter, stStatus, pFilter), query) =>
          val q = query.trim.toLowerCase
          items
            .filter(qi => stFilter.contains(qi.step.stationType.toString))
            .filter(qi => stStatus.contains(qi.step.status.toString))
            .filter(qi => pFilter.contains(qi.workflow.priority.toString))
            .filter { qi =>
              q.isEmpty ||
              qi.order.order.id.value.toLowerCase.contains(q) ||
              qi.order.customerName.toLowerCase.contains(q) ||
              qi.step.stationType.displayName.toLowerCase.contains(q)
            }
        }

    val tableConfig = SplitTableConfig[QueueItem](
      columns = List(
        ColumnDef("Priority", qi => priorityBadge(qi.workflow.priority),
          Some(qi => qi.workflow.priority.sortWeight.toString), Some("80px")),
        ColumnDef("Order", qi => span(qi.order.order.id.value),
          Some(_.order.order.id.value), Some("100px")),
        ColumnDef("Customer", qi => span(qi.order.customerName),
          Some(_.order.customerName)),
        ColumnDef("Product", qi => span(qi.order.itemSummary)),
        ColumnDef("Station", qi => span(
          cls := "station-label",
          span(cls := "station-label-icon", qi.step.stationType.icon),
          qi.step.stationType.displayName,
        ), Some(_.step.stationType.displayName)),
        ColumnDef("Status", qi => stepStatusBadge(qi.step.status),
          Some(_.step.status.toString), Some("110px")),
        ColumnDef("Actions", qi => actionButtons(qi), width = Some("160px")),
      ),
      rowKey = _.step.id.value,
      filters = List(stationFilterDef, statusFilterDef, priorityFilterDef),
      searchPlaceholder = "Search orders, customers, stations…",
      onRowSelect = Some(qi => selectedStepId.set(Some(qi.step.id.value))),
      emptyMessage = "No items in the queue matching your filters",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      selectedStepId.signal.combineWith(ManufacturingViewModel.queueItems).map { case (selId, items) =>
        selId.flatMap(id => items.find(_.step.id.value == id)).map(renderDetailPanel)
      }

    div(
      cls := "manufacturing-station-queue",
      h2(cls := "manufacturing-view-title", "Station Queue"),
      SplitTableView(
        config = tableConfig,
        items = filteredItems,
        selectedKey = selectedStepId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
      ),
    )

  private def priorityBadge(priority: Priority): HtmlElement =
    val (text, cls_) = priority match
      case Priority.Rush   => ("Rush", "badge badge-error")
      case Priority.Normal => ("Normal", "badge badge-info")
      case Priority.Low    => ("Low", "badge badge-muted")
    span(cls := cls_, text)

  private def stepStatusBadge(status: StepStatus): HtmlElement =
    val (text, cls_) = status match
      case StepStatus.Ready      => ("Ready", "badge badge-ready")
      case StepStatus.InProgress => ("In Progress", "badge badge-active")
      case StepStatus.Completed  => ("Completed", "badge badge-completed")
      case StepStatus.Waiting    => ("Waiting", "badge badge-muted")
      case StepStatus.Skipped    => ("Skipped", "badge badge-muted")
      case StepStatus.Failed     => ("Failed", "badge badge-error")
    span(cls := cls_, text)

  private def actionButtons(qi: QueueItem): HtmlElement =
    div(
      cls := "queue-actions",
      qi.step.status match
        case StepStatus.Ready =>
          button(
            cls := "btn-primary btn-sm",
            "▶ Start",
            onClick.stopPropagation --> { _ => ManufacturingViewModel.startStep(qi.step.id.value) },
          )
        case StepStatus.InProgress =>
          button(
            cls := "btn-success btn-sm",
            "✓ Complete",
            onClick.stopPropagation --> { _ => ManufacturingViewModel.completeStep(qi.step.id.value) },
          )
        case _ => emptyNode,
    )

  private def renderDetailPanel(qi: QueueItem): HtmlElement =
    import ManufacturingWorkflow.*
    div(
      cls := "queue-detail-panel",

      // Close button
      button(
        cls := "detail-panel-close",
        "×",
        onClick --> { _ => ManufacturingViewModel.selectedQueueItemId.set(None) },
      ),

      // Order header
      div(
        cls := "detail-panel-header",
        h3(qi.order.order.id.value),
        span(cls := "detail-panel-customer", qi.order.customerName),
      ),

      // Current step info
      div(
        cls := "detail-panel-section",
        h4("Current Step"),
        div(cls := "detail-step-info",
          span(cls := "detail-step-station", qi.step.stationType.icon, " ", qi.step.stationType.displayName),
          stepStatusBadge(qi.step.status),
        ),
        if qi.step.notes.nonEmpty then p(cls := "detail-step-notes", qi.step.notes) else emptyNode,
      ),

      // Workflow progress
      div(
        cls := "detail-panel-section",
        h4("Workflow Progress"),
        div(
          cls := "workflow-steps-list",
          qi.workflow.steps.map { step =>
            val stepCls = step.status match
              case StepStatus.Completed  => "workflow-step-item workflow-step-item--completed"
              case StepStatus.InProgress => "workflow-step-item workflow-step-item--active"
              case StepStatus.Ready      => "workflow-step-item workflow-step-item--ready"
              case _                     => "workflow-step-item"
            div(
              cls := stepCls,
              span(cls := "workflow-step-icon", step.status match
                case StepStatus.Completed  => "✓"
                case StepStatus.InProgress => "▶"
                case StepStatus.Ready      => "○"
                case _                     => "·"
              ),
              span(cls := "workflow-step-name", step.stationType.displayName),
              step.componentRole.map(r => span(cls := "workflow-step-role", s"(${r})")).getOrElse(emptyNode),
            )
          },
        ),
      ),

      // Order items
      div(
        cls := "detail-panel-section",
        h4("Order Items"),
        qi.order.order.basket.items.map { item =>
          div(cls := "detail-order-item",
            span(cls := "detail-item-name", item.configuration.category.name(Language.En)),
            span(cls := "detail-item-qty", s"× ${item.quantity}"),
            span(cls := "detail-item-material", item.configuration.components.headOption.map(_.material.name(Language.En)).getOrElse("")),
          )
        },
      ),
    )
