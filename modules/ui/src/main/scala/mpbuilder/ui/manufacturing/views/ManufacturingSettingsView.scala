package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.sample.{SampleManufacturing, SampleTierRestrictions}
import mpbuilder.domain.manufacturing.{ShopSchedule, WorkingHours}
import mpbuilder.ui.manufacturing.*
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

/** Settings view for manufacturing configuration. */
object ManufacturingSettingsView:

  def apply(): HtmlElement =
    div(
      cls := "manufacturing-settings",

      h2(cls := "manufacturing-view-title", "Settings"),

      // Working Hours
      div(
        cls := "settings-section",
        h3("Working Hours"),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Open Time:"),
          input(
            cls := "settings-input",
            typ := "time",
            value <-- ManufacturingViewModel.settingsOpenTime.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsOpenTime.writer,
          ),
        ),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Close Time:"),
          input(
            cls := "settings-input",
            typ := "time",
            value <-- ManufacturingViewModel.settingsCloseTime.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsCloseTime.writer,
          ),
        ),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Work Days:"),
          div(
            styleAttr := "display: flex; gap: 6px; flex-wrap: wrap;",
            DayOfWeek.values.toList.map { day =>
              val shortName = day.toString.take(3)
              label(
                cls := "radio-label",
                input(
                  typ := "checkbox",
                  checked <-- ManufacturingViewModel.settingsWorkDays.signal.map(_.contains(day)),
                  onChange.mapToChecked --> { isChecked =>
                    ManufacturingViewModel.settingsWorkDays.update { days =>
                      if isChecked then days + day else days - day
                    }
                  },
                ),
                shortName,
              )
            },
          ),
        ),
      ),

      // Cutoff Times
      div(
        cls := "settings-section",
        h3("Cutoff Times"),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Express Cutoff:"),
          input(
            cls := "settings-input",
            typ := "time",
            value <-- ManufacturingViewModel.settingsExpressCutoff.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsExpressCutoff.writer,
          ),
        ),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Standard Cutoff:"),
          input(
            cls := "settings-input",
            typ := "time",
            value <-- ManufacturingViewModel.settingsStandardCutoff.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsStandardCutoff.writer,
          ),
        ),
      ),

      // Pricing
      div(
        cls := "settings-section",
        h3("Pricing"),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Express Base Multiplier:"),
          input(
            cls := "settings-input",
            typ := "number",
            stepAttr := "0.01",
            value <-- ManufacturingViewModel.settingsExpressMultiplier.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsExpressMultiplier.writer,
          ),
        ),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Economy Base Multiplier:"),
          input(
            cls := "settings-input",
            typ := "number",
            stepAttr := "0.01",
            value <-- ManufacturingViewModel.settingsEconomyMultiplier.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsEconomyMultiplier.writer,
          ),
        ),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Express Surcharge Cap:"),
          input(
            cls := "settings-input",
            typ := "number",
            stepAttr := "0.1",
            value <-- ManufacturingViewModel.settingsExpressSurchargeCap.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsExpressSurchargeCap.writer,
          ),
        ),
      ),

      // Queue Thresholds
      div(
        cls := "settings-section",
        h3("Queue Thresholds"),
        div(
          cls := "settings-row",
          span(cls := "settings-label", "Express Critical Threshold:"),
          div(
            styleAttr := "display: flex; align-items: center; gap: 6px;",
            input(
              cls := "settings-input",
              typ := "number",
              minAttr := "0",
              maxAttr := "100",
              value <-- ManufacturingViewModel.settingsExpressCriticalThreshold.signal,
              onInput.mapToValue --> ManufacturingViewModel.settingsExpressCriticalThreshold.writer,
            ),
            span(cls := "settings-value", "%"),
          ),
        ),
        p(
          styleAttr := "font-size: 0.82rem; color: #6b7280; margin-top: 0.5rem;",
          "When global utilisation reaches this threshold, Express manufacturing is disabled.",
        ),
      ),

      // Busy Periods
      div(
        cls := "settings-section",
        h3("Busy Periods"),
        table(
          cls := "settings-table",
          thead(
            tr(
              th("Condition"),
              th("Additional Multiplier"),
            ),
          ),
          tbody(
            SampleManufacturing.busyPeriodMultipliers.map { bp =>
              val condition = List(
                bp.dayOfWeek.map(days => s"Days: ${days.map(_.toString.take(3)).mkString(", ")}"),
                bp.monthRange.map { case (s, e) => s"Months: ${s.toString.take(3)}–${e.toString.take(3)}" },
                bp.timeAfter.map(t => s"After: $t"),
              ).flatten.mkString("; ")
              tr(
                td(condition),
                td(span(cls := "settings-badge settings-badge--active", s"+${(bp.additionalMultiplier * 100).setScale(0)}%")),
              )
            },
          ),
        ),
      ),

      // Station Time Estimates
      div(
        cls := "settings-section",
        h3("Station Time Estimates"),
        table(
          cls := "settings-table",
          thead(
            tr(
              th("Station"),
              th("Base Time (min)"),
              th("Per-Unit (sec)"),
              th("Max Parallel"),
            ),
          ),
          tbody(
            SampleManufacturing.stationTimeEstimates.map { est =>
              tr(
                td(est.stationType.displayName),
                td(est.baseTimeMinutes.toString),
                td(est.perUnitSeconds.toString),
                td(est.maxParallelUnits.toString),
              )
            },
          ),
        ),
      ),

      // Holiday Calendar
      div(
        cls := "settings-section",
        h3("Holiday Calendar"),
        div(
          cls := "settings-row",
          input(
            cls := "settings-input",
            typ := "date",
            styleAttr := "width: 180px !important;",
            value <-- ManufacturingViewModel.settingsNewHoliday.signal,
            onInput.mapToValue --> ManufacturingViewModel.settingsNewHoliday.writer,
          ),
          button(
            cls := "btn btn-primary",
            "Add Holiday",
            onClick --> { _ => ManufacturingViewModel.addHoliday() },
          ),
        ),
        div(
          cls := "settings-holiday-list",
          children <-- ManufacturingViewModel.settingsHolidays.signal.map { holidays =>
            holidays.map { date =>
              span(
                cls := "settings-holiday-tag",
                date.toString,
                span(
                  styleAttr := "cursor: pointer; margin-left: 4px;",
                  "✕",
                  onClick --> { _ => ManufacturingViewModel.removeHoliday(date) },
                ),
              )
            }
          },
        ),
        p(
          styleAttr := "font-size: 0.82rem; color: #6b7280; margin-top: 0.5rem;",
          children <-- ManufacturingViewModel.settingsHolidays.signal.map { h =>
            if h.isEmpty then List(span("No holidays configured. Working hours apply on all weekdays."))
            else List(span(s"${h.size} holiday(s) configured."))
          },
        ),
      ),
    )
