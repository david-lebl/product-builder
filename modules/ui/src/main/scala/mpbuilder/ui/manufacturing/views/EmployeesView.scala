package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.MachineStatus.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.uikit.containers.*

/** Employees View — manage employee profiles and station capabilities. */
object EmployeesView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val activeFilterDef = FilterDef(
      id = "active",
      label = "Status",
      options = Val(List(("active", "Active"), ("inactive", "Inactive"))),
      selectedValues = Var(Set("active", "inactive")),
    )

    val filteredEmployees: Signal[List[Employee]] =
      ManufacturingViewModel.employees.signal
        .combineWith(activeFilterDef.selectedValues.signal, searchVar.signal)
        .map { case (emps, activeFilter, query) =>
          val q = query.trim.toLowerCase
          emps
            .filter { e =>
              (e.isActive && activeFilter.contains("active")) ||
              (!e.isActive && activeFilter.contains("inactive"))
            }
            .filter { e =>
              q.isEmpty ||
              e.name.toLowerCase.contains(q) ||
              e.id.value.toLowerCase.contains(q) ||
              e.stationCapabilities.exists(_.displayName.toLowerCase.contains(q))
            }
        }

    val tableConfig = SplitTableConfig[Employee](
      columns = List(
        ColumnDef("Name", e => span(cls := "employee-name", e.name), Some(_.name)),
        ColumnDef("Status", e => employeeStatusBadge(e), Some(e => if e.isActive then "1" else "0"), Some("100px")),
        ColumnDef("Stations", e => stationCapsList(e), width = Some("280px")),
        ColumnDef("Actions", e => employeeActions(e), width = Some("120px")),
      ),
      rowKey = _.id.value,
      filters = List(activeFilterDef),
      searchPlaceholder = "Search employees, stations…",
      onRowSelect = Some(e => selectedId.set(Some(e.id.value))),
      emptyMessage = "No employees matching your filters",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      selectedId.signal.combineWith(ManufacturingViewModel.employees.signal).map { case (selId, emps) =>
        selId.flatMap(id => emps.find(_.id.value == id)).map(e =>
          renderEmployeePanel(e, () => selectedId.set(None))
        )
      }

    div(
      cls := "manufacturing-employees",
      h2(cls := "manufacturing-view-title", "Employees"),

      // Current employee selector
      div(
        cls := "current-employee-selector",
        span(cls := "current-employee-label", "Logged in as: "),
        child <-- ManufacturingViewModel.employees.signal.combineWith(ManufacturingViewModel.currentEmployeeId.signal).map {
          case (emps, optId) =>
            val activeEmps = emps.filter(_.isActive)
            select(
              cls := "current-employee-select",
              option(value := "", "— No employee —",
                selected := optId.isEmpty),
              activeEmps.map { e =>
                option(
                  value := e.id.value,
                  e.name,
                  selected := optId.contains(e.id),
                )
              },
              onChange.mapToValue --> { v =>
                if v.isEmpty then ManufacturingViewModel.setCurrentEmployee(None)
                else ManufacturingViewModel.setCurrentEmployee(Some(EmployeeId.unsafe(v)))
              },
            )
        },
      ),

      SplitTableView(
        config = tableConfig,
        items = filteredEmployees,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
      ),
    )

  private def employeeStatusBadge(e: Employee): HtmlElement =
    if e.isActive then span(cls := "badge badge-completed", "Active")
    else span(cls := "badge badge-muted", "Inactive")

  private def stationCapsList(e: Employee): HtmlElement =
    div(
      cls := "station-caps-list",
      e.stationCapabilities.toList.sortBy(_.ordinal).map { st =>
        span(cls := "station-cap-chip", st.icon, " ", st.displayName)
      },
    )

  private def employeeActions(e: Employee): HtmlElement =
    div(
      cls := "employee-actions",
      button(
        cls := (if e.isActive then "btn-warning btn-sm" else "btn-success btn-sm"),
        if e.isActive then "Deactivate" else "Activate",
        onClick.stopPropagation --> { _ => ManufacturingViewModel.toggleEmployeeActive(e.id) },
      ),
    )

  private def renderEmployeePanel(e: Employee, onClose: () => Unit): HtmlElement =
    div(
      cls := "employee-detail-panel",

      button(
        cls := "detail-panel-close",
        "×",
        onClick --> { _ => onClose() },
      ),

      div(
        cls := "detail-panel-header",
        h3(e.name),
        employeeStatusBadge(e),
      ),

      // Employee ID
      div(
        cls := "detail-panel-section",
        h4("Employee ID"),
        p(e.id.value),
      ),

      // Station capabilities with toggles
      div(
        cls := "detail-panel-section",
        h4("Station Capabilities"),
        div(
          cls := "capability-toggles",
          StationType.values.toList.map { st =>
            val isEnabled = e.stationCapabilities.contains(st)
            div(
              cls := (if isEnabled then "capability-toggle capability-toggle--active" else "capability-toggle"),
              span(cls := "capability-toggle-icon", st.icon),
              span(cls := "capability-toggle-name", st.displayName),
              onClick --> { _ =>
                val newCaps = if isEnabled then e.stationCapabilities - st else e.stationCapabilities + st
                if newCaps.nonEmpty then
                  ManufacturingViewModel.updateEmployeeCapabilities(e.id, newCaps)
              },
            )
          },
        ),
      ),

      // Actions
      div(
        cls := "detail-panel-section detail-panel-actions",
        button(
          cls := (if e.isActive then "btn-warning" else "btn-success"),
          if e.isActive then "🚫 Deactivate Employee" else "✓ Activate Employee",
          onClick --> { _ => ManufacturingViewModel.toggleEmployeeActive(e.id) },
        ),
      ),
    )
