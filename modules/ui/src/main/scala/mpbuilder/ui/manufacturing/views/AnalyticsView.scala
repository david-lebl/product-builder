package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.model.StationType.*
import mpbuilder.domain.service.AnalyticsService
import mpbuilder.domain.service.AnalyticsService.*
import mpbuilder.ui.manufacturing.*

/** Analytics View — performance metrics and shop analytics. */
object AnalyticsView:

  def apply(): HtmlElement =
    val analyticsSignal: Signal[AnalyticsSummary] =
      ManufacturingViewModel.orders.combineWith(ManufacturingViewModel.employees.signal).map {
        case (ords, emps) => AnalyticsService.computeSummary(ords, emps)
      }

    div(
      cls := "manufacturing-analytics",
      h2(cls := "manufacturing-view-title", "Analytics & Reporting"),

      // KPI cards row
      div(
        cls := "analytics-kpi-row",
        children <-- analyticsSignal.map { summary =>
          List(
            kpiCard("Total Orders", summary.totalOrders.toString, "📋", "kpi-total"),
            kpiCard("Completed", summary.completedOrders.toString, "✅", "kpi-completed"),
            kpiCard("In Progress", summary.inProgressOrders.toString, "🏭", "kpi-progress"),
            kpiCard("On-Time Rate", f"${summary.onTimeRate * 100}%.0f%%", "⏱️", "kpi-ontime"),
            kpiCard("Avg Step Time", formatDuration(summary.avgCompletionTimeMs), "⚡", "kpi-avgtime"),
          )
        },
      ),

      // Bottleneck alert
      child <-- analyticsSignal.map { summary =>
        summary.bottleneck match
          case Some((st, depth)) if depth > 1 =>
            div(
              cls := "analytics-bottleneck-alert",
              span(cls := "bottleneck-icon", "⚠️"),
              span(cls := "bottleneck-text",
                s"Bottleneck detected: ${st.displayName} has $depth jobs waiting in queue"
              ),
            )
          case _ => emptyNode
      },

      // Station metrics table
      h3(cls := "analytics-section-title", "Station Performance"),
      div(
        cls := "analytics-table-container",
        table(
          cls := "analytics-table",
          thead(
            tr(
              th("Station"),
              th("Completed"),
              th("Avg Time"),
              th("Queue"),
              th("In Progress"),
              th("Load"),
            ),
          ),
          tbody(
            children <-- analyticsSignal.map { summary =>
              summary.stationMetrics.map { m =>
                tr(
                  td(
                    cls := "analytics-station-cell",
                    span(cls := "station-label-icon", m.stationType.icon),
                    span(m.stationType.displayName),
                  ),
                  td(m.completedSteps.toString),
                  td(formatDuration(m.avgTimeMs)),
                  td(
                    if m.currentQueueDepth > 3 then span(cls := "analytics-high", m.currentQueueDepth.toString)
                    else span(m.currentQueueDepth.toString)
                  ),
                  td(m.inProgress.toString),
                  td(loadIndicator(m.currentQueueDepth, m.inProgress)),
                )
              }
            },
          ),
        ),
      ),

      // Employee metrics table
      h3(cls := "analytics-section-title", "Employee Throughput"),
      div(
        cls := "analytics-table-container",
        table(
          cls := "analytics-table",
          thead(
            tr(
              th("Employee"),
              th("Completed Steps"),
              th("Stations Worked"),
            ),
          ),
          tbody(
            children <-- analyticsSignal.map { summary =>
              summary.employeeMetrics.sortBy(-_.completedSteps).map { m =>
                tr(
                  td(cls := "employee-name", m.employeeName),
                  td(
                    if m.completedSteps > 0 then
                      div(
                        cls := "analytics-bar-cell",
                        div(
                          cls := "analytics-bar",
                          width := s"${Math.min(m.completedSteps * 20, 100)}%",
                        ),
                        span(m.completedSteps.toString),
                      )
                    else span("0"),
                  ),
                  td(
                    div(
                      cls := "station-caps-list",
                      m.stationsWorked.toList.sortBy(_.ordinal).map { st =>
                        span(cls := "station-cap-chip", st.icon, " ", st.displayName)
                      },
                      if m.stationsWorked.isEmpty then span(cls := "text-muted", "—") else emptyNode,
                    ),
                  ),
                )
              }
            },
          ),
        ),
      ),
    )

  private def kpiCard(title: String, value: String, icon: String, modifier: String): HtmlElement =
    div(
      cls := s"analytics-kpi-card $modifier",
      div(cls := "analytics-kpi-icon", icon),
      div(
        cls := "analytics-kpi-content",
        div(cls := "analytics-kpi-value", value),
        div(cls := "analytics-kpi-title", title),
      ),
    )

  private def formatDuration(ms: Long): String =
    if ms <= 0 then "—"
    else if ms < 60000 then s"${ms / 1000}s"
    else if ms < 3600000 then s"${ms / 60000}m"
    else s"${ms / 3600000}h ${(ms % 3600000) / 60000}m"

  private def loadIndicator(queue: Int, inProgress: Int): HtmlElement =
    val total = queue + inProgress
    val cls_ =
      if total == 0 then "analytics-load analytics-load--idle"
      else if total <= 2 then "analytics-load analytics-load--light"
      else if total <= 5 then "analytics-load analytics-load--moderate"
      else "analytics-load analytics-load--heavy"
    val label =
      if total == 0 then "Idle"
      else if total <= 2 then "Light"
      else if total <= 5 then "Moderate"
      else "Heavy"
    span(cls := cls_, label)
