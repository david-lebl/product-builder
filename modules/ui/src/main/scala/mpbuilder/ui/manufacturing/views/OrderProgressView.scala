package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Order Progress View — pipeline visualization showing all active orders
  * and their linear step progression through manufacturing.
  */
object OrderProgressView:

  def apply(): Element =
    val state = ManufacturingViewModel.state

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Order Progress"),
        p(cls := "mfg-view-subtitle", "Track all orders through the manufacturing pipeline"),
      ),

      div(
        cls := "mfg-progress-list",
        children <-- state.map { s =>
          val approvedOrders = s.orders.filter(_.approval == ApprovalStatus.Approved)
          if approvedOrders.isEmpty then
            List(div(cls := "mfg-empty-detail", "No approved orders in production"))
          else
            approvedOrders.map(orderProgressCard)
        },
      ),
    )

  private def orderProgressCard(order: ManufacturingOrder): Element =
    div(
      cls := "mfg-progress-card",

      // Order header
      div(
        cls := "mfg-progress-card-header",
        div(
          cls := "mfg-progress-order-info",
          span(cls := "mfg-order-id", s"Order #${order.id.value}"),
          span(cls := "mfg-order-customer", s" — ${order.customerName}"),
          priorityBadge(order.priority),
        ),
      ),

      // Per-item workflow pipelines
      order.items.map { item =>
        item.workflow match
          case Some(wf) => workflowPipeline(item, wf)
          case None =>
            div(
              cls := "mfg-pipeline-row",
              span(cls := "mfg-pipeline-item-name", item.productDescription),
              span(cls := "mfg-pipeline-no-workflow", "Workflow not yet generated"),
            )
      },
    )

  private def workflowPipeline(item: ManufacturingOrderItem, wf: ManufacturingWorkflow): Element =
    val pct = ManufacturingWorkflow.completionPercent(wf)

    div(
      cls := "mfg-pipeline-row",

      // Item description
      div(
        cls := "mfg-pipeline-item-header",
        span(cls := "mfg-pipeline-item-name", item.productDescription),
        span(cls := "mfg-pipeline-pct", s"$pct%"),
      ),

      // Step sequence — linear pipeline
      div(
        cls := "mfg-pipeline-steps",
        wf.steps.map { step =>
          val statusCls = step.status match
            case StepStatus.Completed  => "completed"
            case StepStatus.InProgress => "in-progress"
            case StepStatus.Ready      => "ready"
            case StepStatus.Failed     => "failed"
            case StepStatus.Skipped    => "skipped"
            case StepStatus.Waiting    => "waiting"

          div(
            cls := s"mfg-pipeline-step $statusCls",
            title := s"${step.stationType.label}: ${step.status.label}",
            div(cls := "mfg-pipeline-step-icon", step.status match
              case StepStatus.Completed  => "✓"
              case StepStatus.InProgress => "▶"
              case StepStatus.Ready      => "●"
              case StepStatus.Failed     => "✗"
              case StepStatus.Skipped    => "⏭"
              case StepStatus.Waiting    => "○"
            ),
            div(cls := "mfg-pipeline-step-label", step.stationType.label),
          )
        },
      ),

      // Overall progress bar
      div(
        cls := "mfg-progress-bar-container",
        div(cls := "mfg-progress-bar", styleAttr := s"width: $pct%"),
      ),
    )

  private def priorityBadge(priority: Priority): Element =
    val cls_ = priority match
      case Priority.Rush   => "mfg-priority-badge rush"
      case Priority.Normal => "mfg-priority-badge normal"
      case Priority.Low    => "mfg-priority-badge low"
    span(cls := cls_, priority.label)
