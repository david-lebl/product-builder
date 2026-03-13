package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.model.ManufacturingWorkflow.*
import mpbuilder.domain.model.FulfilmentChecklist.*
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
        ColumnDef("Status", mo => orderStatusBadge(mo), Some(_.overallStatus.toString), Some("140px")),
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

  private def orderStatusBadge(mo: ManufacturingOrder): HtmlElement =
    if mo.isDispatched then span(cls := "badge badge-completed", "🚚 Dispatched")
    else if mo.isReadyForDispatch then span(cls := "badge badge-ready", "📦 Ready for Dispatch")
    else workflowStatusBadge(mo.overallStatus)

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
        orderStatusBadge(mo),
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

      // Fulfilment checklist (shown when ready for dispatch)
      mo.fulfilment.map { fc =>
        renderFulfilmentChecklist(mo.order.id.value, fc, mo)
      }.getOrElse {
        if mo.isReadyForDispatch then
          div(
            cls := "detail-panel-section",
            h4("Fulfilment"),
            p(cls := "text-muted", "Fulfilment checklist will appear when all workflows complete."),
          )
        else emptyNode
      },
    )

  private def renderFulfilmentChecklist(orderId: String, fc: FulfilmentChecklist, mo: ManufacturingOrder): HtmlElement =
    div(
      cls := "fulfilment-section",

      // Fulfilment progress summary
      div(
        cls := "detail-panel-section",
        h4("Fulfilment Checklist"),
        div(
          cls := "fulfilment-progress",
          span(cls := "fulfilment-progress-text", s"${fc.completedStepsCount} / ${fc.totalStepsCount} steps"),
          div(
            cls := "progress-bar-container",
            div(
              cls := "progress-bar-fill",
              width := s"${(fc.completedStepsCount * 100) / fc.totalStepsCount}%",
            ),
          ),
        ),
      ),

      // Step 1: Collect items
      div(
        cls := "detail-panel-section",
        h4(s"1. Collect Items ${if fc.allItemsCollected then "✅" else ""}"),
        div(
          cls := "fulfilment-collect-items",
          mo.order.basket.items.zipWithIndex.map { case (item, idx) =>
            val ci = fc.collectedItems.find(_.itemIndex == idx)
            val isCollected = ci.exists(_.collected)
            div(
              cls := (if isCollected then "fulfilment-item fulfilment-item--collected" else "fulfilment-item"),
              input(
                typ := "checkbox",
                checked := isCollected,
                onChange --> { _ =>
                  ManufacturingViewModel.toggleItemCollected(orderId, idx)
                },
              ),
              span(
                cls := "fulfilment-item-name",
                s"${item.configuration.category.name(Language.En)} × ${item.quantity}",
              ),
              if isCollected then span(cls := "fulfilment-item-check", "✓") else emptyNode,
            )
          },
        ),
      ),

      // Step 2: Quality check
      div(
        cls := "detail-panel-section",
        h4(s"2. Quality Check ${if fc.isQualityPassed then "✅" else ""}"),
        if fc.isQualityPassed then
          div(
            cls := "fulfilment-qc-passed",
            span("✅ Quality check passed"),
            if fc.qualitySignOff.notes.nonEmpty then p(cls := "fulfilment-qc-notes", fc.qualitySignOff.notes) else emptyNode,
          )
        else
          div(
            cls := "fulfilment-qc-actions",
            button(
              cls := "btn-success btn-sm",
              "✓ Pass QC",
              onClick --> { _ => ManufacturingViewModel.signOffQuality(orderId, passed = true, "") },
            ),
          ),
      ),

      // Step 3: Package
      div(
        cls := "detail-panel-section",
        h4(s"3. Package ${if fc.isPackaged then "✅" else ""}"),
        if fc.isPackaged then
          div(
            cls := "fulfilment-packaging-info",
            fc.packagingInfo.packagingType.map(pt => p(s"📦 Type: ${pt.displayName}")).getOrElse(emptyNode),
            fc.packagingInfo.dimensionsCm.map(d => p(s"📐 Dimensions: ${d} cm")).getOrElse(emptyNode),
            fc.packagingInfo.weightKg.map(w => p(s"⚖️ Weight: ${w} kg")).getOrElse(emptyNode),
          )
        else
          div(
            cls := "fulfilment-packaging-buttons",
            PackagingType.values.toList.map { pt =>
              button(
                cls := "btn-secondary btn-sm",
                pt.displayName,
                onClick --> { _ => ManufacturingViewModel.setPackaging(orderId, pt, "", "") },
              )
            },
          ),
      ),

      // Step 4: Dispatch
      div(
        cls := "detail-panel-section",
        h4(s"4. Dispatch ${if fc.isDispatched then "✅" else ""}"),
        if fc.isDispatched then
          div(
            cls := "fulfilment-dispatch-info",
            span("🚚 Order dispatched"),
            if fc.dispatchInfo.trackingNumber.nonEmpty then
              p(s"Tracking: ${fc.dispatchInfo.trackingNumber}")
            else emptyNode,
          )
        else
          div(
            cls := "fulfilment-dispatch-actions",
            button(
              cls := "btn-success",
              "🚚 Confirm Dispatch",
              disabled := !fc.allItemsCollected || !fc.isQualityPassed || !fc.isPackaged,
              onClick --> { _ => ManufacturingViewModel.confirmDispatch(orderId, "") },
            ),
            if !fc.allItemsCollected || !fc.isQualityPassed || !fc.isPackaged then
              span(cls := "fulfilment-dispatch-hint", "Complete steps 1-3 first")
            else emptyNode,
          ),
      ),
    )
