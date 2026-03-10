package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.model.ManufacturingWorkflow.*
import mpbuilder.domain.pricing.Money
import mpbuilder.ui.manufacturing.*
import mpbuilder.uikit.containers.*

/** Order Progress View — tracks all active orders with workflow progress. */
object OrderProgressView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val statusFilterDef = FilterDef(
      id = "status",
      label = "Workflow Status",
      options = Val(WorkflowStatus.values.toList.map(s => (s.toString, s.toString))),
      selectedValues = Var(Set(
        WorkflowStatus.InProgress.toString,
        WorkflowStatus.Pending.toString,
        WorkflowStatus.Completed.toString,
      )),
    )

    val priorityFilterDef = FilterDef(
      id = "priority",
      label = "Priority",
      options = Val(Priority.values.toList.map(p => (p.toString, p.displayName))),
      selectedValues = Var(Priority.values.toSet.map(_.toString)),
    )

    val filteredOrders: Signal[List[ManufacturingOrder]] =
      ManufacturingViewModel.orders
        .combineWith(statusFilterDef.selectedValues.signal, priorityFilterDef.selectedValues.signal, searchVar.signal)
        .map { case (ords, statuses, priorities, query) =>
          val q = query.trim.toLowerCase
          ords
            .filter(_.approvalStatus == ApprovalStatus.Approved)
            .filter(mo => statuses.contains(mo.overallStatus.toString))
            .filter(mo => mo.workflows.exists(wf => priorities.contains(wf.priority.toString)))
            .filter { mo =>
              q.isEmpty ||
              mo.order.id.value.toLowerCase.contains(q) ||
              mo.customerName.toLowerCase.contains(q)
            }
        }

    val tableConfig = SplitTableConfig[ManufacturingOrder](
      columns = List(
        ColumnDef("Order ID", mo => span(mo.order.id.value), Some(_.order.id.value), Some("100px")),
        ColumnDef("Customer", mo => span(mo.customerName), Some(_.customerName)),
        ColumnDef("Items", mo => span(s"${mo.order.basket.items.size}"), Some(_.order.basket.items.size.toString), Some("60px")),
        ColumnDef("Progress", mo => progressBar(mo), Some(mo => f"${mo.overallCompletionRatio}%.2f")),
        ColumnDef("Status", mo => workflowStatusBadge(mo.overallStatus), Some(_.overallStatus.toString), Some("120px")),
        ColumnDef("Deadline", mo => deadlineDisplay(mo), Some(_.deadline.getOrElse(Long.MaxValue).toString)),
      ),
      rowKey = _.order.id.value,
      filters = List(statusFilterDef, priorityFilterDef),
      searchPlaceholder = "Search orders, customers…",
      onRowSelect = Some(mo => selectedId.set(Some(mo.order.id.value))),
      emptyMessage = "No orders matching your filters",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      selectedId.signal.combineWith(ManufacturingViewModel.orders).map { case (selId, ords) =>
        selId.flatMap(id => ords.find(_.order.id.value == id)).map(renderProgressPanel)
      }

    div(
      cls := "manufacturing-order-progress",
      h2(cls := "manufacturing-view-title", "Order Progress"),
      SplitTableView(
        config = tableConfig,
        items = filteredOrders,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
      ),
    )

  private def workflowStatusBadge(status: WorkflowStatus): HtmlElement =
    val (text, cls_) = status match
      case WorkflowStatus.Pending    => ("Pending", "badge badge-muted")
      case WorkflowStatus.InProgress => ("In Progress", "badge badge-active")
      case WorkflowStatus.Completed  => ("Completed", "badge badge-completed")
      case WorkflowStatus.OnHold     => ("On Hold", "badge badge-warning")
      case WorkflowStatus.Cancelled  => ("Cancelled", "badge badge-error")
    span(cls := cls_, text)

  private def progressBar(mo: ManufacturingOrder): HtmlElement =
    val ratio = mo.overallCompletionRatio
    val pct = (ratio * 100).toInt
    val completed = mo.completedStepCount
    val total = mo.totalSteps
    div(
      cls := "progress-bar-container",
      div(
        cls := "progress-bar-fill",
        width := s"$pct%",
      ),
      span(cls := "progress-bar-text", s"$completed / $total"),
    )

  private def deadlineDisplay(mo: ManufacturingOrder): HtmlElement =
    mo.deadline match
      case None => span("—")
      case Some(dl) =>
        val now = System.currentTimeMillis()
        val hoursLeft = ((dl - now) / 3600000.0).toInt
        val (text, cls_) =
          if hoursLeft < 0 then ("Overdue", "deadline deadline--overdue")
          else if hoursLeft < 6 then (s"${hoursLeft}h left", "deadline deadline--urgent")
          else if hoursLeft < 24 then ("Today", "deadline deadline--today")
          else if hoursLeft < 48 then ("Tomorrow", "deadline deadline--soon")
          else (s"${hoursLeft / 24}d left", "deadline")
        span(cls := cls_, text)

  private def renderProgressPanel(mo: ManufacturingOrder): HtmlElement =
    div(
      cls := "progress-detail-panel",

      div(
        cls := "detail-panel-header",
        h3(mo.order.id.value),
        workflowStatusBadge(mo.overallStatus),
      ),

      // Customer info
      div(
        cls := "detail-panel-section",
        h4("Customer"),
        p(mo.customerName),
        p(mo.order.checkoutInfo.contactInfo.email),
      ),

      // Per-item workflow progress
      div(
        cls := "detail-panel-section",
        h4("Item Workflows"),
        mo.workflows.zipWithIndex.map { case (wf, idx) =>
          val item = mo.order.basket.items.lift(idx)
          div(
            cls := "progress-workflow-item",
            div(cls := "progress-workflow-header",
              span(cls := "progress-workflow-name",
                item.map(i => s"${i.configuration.category.name(Language.En)} × ${i.quantity}")
                  .getOrElse(s"Item ${idx + 1}")
              ),
              span(cls := "progress-workflow-pct", s"${(wf.completionRatio * 100).toInt}%"),
            ),
            // Step chain visualization
            div(
              cls := "progress-step-chain",
              wf.steps.map { step =>
                val stepCls = step.status match
                  case StepStatus.Completed  => "step-dot step-dot--completed"
                  case StepStatus.InProgress => "step-dot step-dot--active"
                  case StepStatus.Ready      => "step-dot step-dot--ready"
                  case StepStatus.Failed     => "step-dot step-dot--failed"
                  case _                     => "step-dot"
                div(
                  cls := stepCls,
                  title := s"${step.stationType.displayName} — ${step.status}",
                  span(cls := "step-dot-icon", step.status match
                    case StepStatus.Completed  => "✓"
                    case StepStatus.InProgress => "▶"
                    case StepStatus.Ready      => "○"
                    case StepStatus.Failed     => "✗"
                    case _                     => "·"
                  ),
                  span(cls := "step-dot-label", step.stationType.displayName),
                )
              },
            ),
          )
        },
      ),

      // Deadline
      mo.deadline.map { dl =>
        div(
          cls := "detail-panel-section",
          h4("Deadline"),
          deadlineDisplay(mo),
        )
      }.getOrElse(emptyNode),
    )
