package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.StationType.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.ui.{Router, Page}

/** Dashboard view — landing page showing shop-wide status at a glance. */
object DashboardView:

  def apply(): HtmlElement =
    div(
      cls := "manufacturing-dashboard",

      // Header
      h2(cls := "manufacturing-view-title", "Dashboard"),

      // Summary cards
      div(
        cls := "dashboard-summary-cards",
        children <-- ManufacturingViewModel.dashboardSummary.map { summary =>
          List(
            summaryCard("Awaiting Approval", summary.awaitingApproval.toString, "📋", "card-approval"),
            summaryCard("In Production", summary.inProduction.toString, "🏭", "card-production"),
            summaryCard("Ready for Dispatch", summary.readyForDispatch.toString, "📦", "card-dispatch"),
            summaryCard("Overdue", summary.overdue.toString, "⚠️", "card-overdue"),
            summaryCard("Today's Completions", summary.todaysCompletions.toString, "✅", "card-completed"),
          )
        },
      ),

      // Station status strip
      h3(cls := "dashboard-section-title", "Station Status"),
      div(
        cls := "dashboard-station-strip",
        children <-- ManufacturingViewModel.stationStatuses.map { statuses =>
          statuses.map { ss =>
            val statusCls =
              if ss.hasInProgress then "station-tile station-tile--working"
              else if ss.queueDepth > 5 then "station-tile station-tile--busy"
              else if ss.queueDepth > 0 then "station-tile station-tile--queued"
              else "station-tile station-tile--idle"
            div(
              cls := statusCls,
              div(
                cls := "station-tile-header",
                span(cls := "station-tile-icon", ss.stationType.icon),
                span(cls := "station-tile-name", ss.stationType.displayName),
              ),
              span(cls := "station-tile-count",
                if ss.queueDepth > 0 then s"${ss.queueDepth} in queue"
                else if ss.hasInProgress then "Working"
                else "Idle",
              ),
            )
          }
        },
      ),

      // Recent orders
      h3(cls := "dashboard-section-title", "Recent Orders"),
      div(
        cls := "dashboard-recent-orders",
        table(
          cls := "dashboard-table",
          thead(
            tr(
              th("Order ID"),
              th("Customer"),
              th("Items"),
              th("Status"),
              th("Progress"),
            ),
          ),
          tbody(
            children <-- ManufacturingViewModel.orders.map { ords =>
              ords.take(10).map { mo =>
                tr(
                  cls := "dashboard-table-row",
                  onClick --> { _ =>
                    ManufacturingViewModel.selectOrder(mo.order.id.value)
                    Router.pushState(Page.ManufacturingOrderProgress)
                  },
                  td(cls := "dashboard-td", mo.order.id.value),
                  td(cls := "dashboard-td", mo.customerName),
                  td(cls := "dashboard-td", mo.itemSummary),
                  td(cls := "dashboard-td", statusBadge(mo)),
                  td(cls := "dashboard-td", progressBar(mo)),
                )
              }
            },
          ),
        ),
      ),

      // My In-Progress Jobs section (shown when logged in as an employee)
      child <-- ManufacturingViewModel.currentEmployee.combineWith(ManufacturingViewModel.myInProgressJobs).map {
        case (Some(emp), jobs) =>
          div(
            cls := "dashboard-my-jobs",
            h3(cls := "dashboard-section-title", s"My In-Progress Jobs (${emp.name})"),
            if jobs.isEmpty then
              div(cls := "dashboard-my-jobs-empty", "No jobs currently in progress — pick one from the Station Queue!")
            else
              table(
                cls := "dashboard-table",
                thead(
                  tr(
                    th("Order"),
                    th("Station"),
                    th("Product"),
                    th("Actions"),
                  ),
                ),
                tbody(
                  jobs.map { qi =>
                    tr(
                      cls := "dashboard-table-row",
                      td(cls := "dashboard-td", qi.order.order.id.value),
                      td(cls := "dashboard-td",
                        span(cls := "station-label",
                          span(cls := "station-label-icon", qi.step.stationType.icon),
                          qi.step.stationType.displayName,
                        ),
                      ),
                      td(cls := "dashboard-td", qi.order.itemSummary),
                      td(cls := "dashboard-td",
                        button(
                          cls := "btn-success btn-sm",
                          "✓ Complete",
                          onClick.stopPropagation --> { _ =>
                            ManufacturingViewModel.completeStep(qi.step.id.value)
                          },
                        ),
                      ),
                    )
                  },
                ),
              ),
          )
        case _ => emptyNode
      },
    )

  private def summaryCard(title: String, value: String, icon: String, modifier: String): HtmlElement =
    div(
      cls := s"summary-card $modifier",
      div(cls := "summary-card-icon", icon),
      div(
        cls := "summary-card-content",
        div(cls := "summary-card-value", value),
        div(cls := "summary-card-title", title),
      ),
    )

  private def statusBadge(mo: ManufacturingOrder): HtmlElement =
    val (text, badgeCls) = mo.approvalStatus match
      case ApprovalStatus.Placed         => ("Awaiting Approval", "badge badge-pending")
      case ApprovalStatus.Approved       =>
        mo.overallStatus match
          case WorkflowStatus.Completed  => ("Completed", "badge badge-completed")
          case WorkflowStatus.InProgress => ("In Production", "badge badge-active")
          case WorkflowStatus.OnHold     => ("On Hold", "badge badge-warning")
          case _                         => ("Approved", "badge badge-info")
      case ApprovalStatus.Rejected       => ("Rejected", "badge badge-error")
      case ApprovalStatus.PendingChanges => ("Pending Changes", "badge badge-warning")
      case ApprovalStatus.OnHold         => ("On Hold", "badge badge-warning")
    span(cls := badgeCls, text)

  private def progressBar(mo: ManufacturingOrder): HtmlElement =
    if mo.approvalStatus != ApprovalStatus.Approved then span("—")
    else
      val ratio = mo.overallCompletionRatio
      val pct = (ratio * 100).toInt
      div(
        cls := "progress-bar-container",
        div(
          cls := "progress-bar-fill",
          width := s"$pct%",
        ),
        span(cls := "progress-bar-text", s"$pct%"),
      )
