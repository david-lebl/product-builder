package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.manufacturing.*

/** Employees View — manage employee station capabilities */
object EmployeesView:

  def apply(): Element =
    val state = ManufacturingViewModel.state

    div(
      cls := "mfg-view",

      div(
        cls := "mfg-view-header",
        h2("Employees"),
        p(cls := "mfg-view-subtitle", "Manage employee profiles and station capabilities"),
      ),

      div(
        cls := "mfg-employees-grid",
        children <-- state.map { s =>
          s.employees.map(employeeCard)
        },
      ),
    )

  private def employeeCard(emp: Employee): Element =
    div(
      cls := s"mfg-employee-card${if emp.isActive then "" else " inactive"}",

      div(
        cls := "mfg-employee-header",
        span(cls := "mfg-employee-avatar", "👤"),
        div(
          span(cls := "mfg-employee-name", emp.name),
          span(cls := s"mfg-employee-status${if emp.isActive then " active" else ""}",
            if emp.isActive then "Active" else "Inactive"
          ),
        ),
      ),

      div(
        cls := "mfg-employee-section",
        h4("Station Capabilities"),
        div(
          cls := "mfg-station-caps",
          StationType.values.toList.map { st =>
            val enabled = emp.stationCapabilities.contains(st)
            div(
              cls := s"mfg-station-cap${if enabled then " enabled" else ""}",
              span(cls := "mfg-station-cap-icon", st.icon),
              span(st.label),
            )
          },
        ),
      ),
    )
