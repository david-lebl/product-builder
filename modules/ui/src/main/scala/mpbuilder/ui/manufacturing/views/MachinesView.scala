package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.MachineStatus.*
import mpbuilder.domain.model.StationType.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.uikit.containers.*

/** Machines View — machine registry with status tracking. */
object MachinesView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val statusFilterDef = FilterDef(
      id = "status",
      label = "Status",
      options = Val(MachineStatus.values.toList.map(s => (s.toString, s.displayName))),
      selectedValues = Var(MachineStatus.values.toSet.map(_.toString)),
    )

    val stationFilterDef = FilterDef(
      id = "station",
      label = "Station Type",
      options = Val(StationType.values.toList.map(st => (st.toString, st.displayName))),
      selectedValues = Var(StationType.values.toSet.map(_.toString)),
    )

    val filteredMachines: Signal[List[Machine]] =
      ManufacturingViewModel.machines.signal
        .combineWith(statusFilterDef.selectedValues.signal, stationFilterDef.selectedValues.signal, searchVar.signal)
        .map { case (ms, statusFilter, stFilter, query) =>
          val q = query.trim.toLowerCase
          ms
            .filter(m => statusFilter.contains(m.status.toString))
            .filter(m => stFilter.contains(m.stationType.toString))
            .filter { m =>
              q.isEmpty ||
              m.name.toLowerCase.contains(q) ||
              m.id.value.toLowerCase.contains(q) ||
              m.stationType.displayName.toLowerCase.contains(q) ||
              m.currentNotes.toLowerCase.contains(q)
            }
        }

    val tableConfig = SplitTableConfig[Machine](
      columns = List(
        ColumnDef("Machine", m => span(cls := "machine-name", m.name), Some(_.name)),
        ColumnDef("Station Type", m => span(
          cls := "station-label",
          span(cls := "station-label-icon", m.stationType.icon),
          m.stationType.displayName,
        ), Some(_.stationType.displayName), Some("180px")),
        ColumnDef("Status", m => machineStatusBadge(m.status), Some(_.status.toString), Some("120px")),
        ColumnDef("Notes", m => span(cls := "machine-notes-preview",
          if m.currentNotes.nonEmpty then m.currentNotes else "—")),
        ColumnDef("Actions", m => machineActions(m), width = Some("200px")),
      ),
      rowKey = _.id.value,
      filters = List(statusFilterDef, stationFilterDef),
      searchPlaceholder = "Search machines, stations…",
      onRowSelect = Some(m => selectedId.set(Some(m.id.value))),
      emptyMessage = "No machines matching your filters",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      selectedId.signal.combineWith(ManufacturingViewModel.machines.signal).map { case (selId, ms) =>
        selId.flatMap(id => ms.find(_.id.value == id)).map(m =>
          renderMachinePanel(m, () => selectedId.set(None))
        )
      }

    div(
      cls := "manufacturing-machines",
      h2(cls := "manufacturing-view-title", "Machines"),
      SplitTableView(
        config = tableConfig,
        items = filteredMachines,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
      ),
    )

  private def machineStatusBadge(status: MachineStatus): HtmlElement =
    val (text, cls_) = status match
      case MachineStatus.Online      => ("Online", "badge badge-completed")
      case MachineStatus.Offline     => ("Offline", "badge badge-error")
      case MachineStatus.Maintenance => ("Maintenance", "badge badge-warning")
    span(cls := cls_, s"${status.icon} $text")

  private def machineActions(m: Machine): HtmlElement =
    div(
      cls := "machine-actions",
      m.status match
        case MachineStatus.Online =>
          List(
            button(
              cls := "btn-warning btn-sm",
              "🟡 Maintenance",
              onClick.stopPropagation --> { _ => ManufacturingViewModel.changeMachineStatus(m.id, MachineStatus.Maintenance) },
            ),
            button(
              cls := "btn-danger btn-sm",
              "🔴 Offline",
              onClick.stopPropagation --> { _ => ManufacturingViewModel.changeMachineStatus(m.id, MachineStatus.Offline) },
            ),
          )
        case MachineStatus.Offline | MachineStatus.Maintenance =>
          List(
            button(
              cls := "btn-success btn-sm",
              "🟢 Online",
              onClick.stopPropagation --> { _ => ManufacturingViewModel.changeMachineStatus(m.id, MachineStatus.Online) },
            ),
          ),
    )

  private def renderMachinePanel(m: Machine, onClose: () => Unit): HtmlElement =
    div(
      cls := "machine-detail-panel",

      button(
        cls := "detail-panel-close",
        "×",
        onClick --> { _ => onClose() },
      ),

      div(
        cls := "detail-panel-header",
        h3(m.name),
        machineStatusBadge(m.status),
      ),

      // Machine ID
      div(
        cls := "detail-panel-section",
        h4("Machine ID"),
        p(m.id.value),
      ),

      // Station type
      div(
        cls := "detail-panel-section",
        h4("Station Type"),
        div(
          cls := "machine-station-type",
          span(cls := "station-label-icon", m.stationType.icon),
          span(m.stationType.displayName),
        ),
      ),

      // Notes
      div(
        cls := "detail-panel-section",
        h4("Notes"),
        if m.currentNotes.nonEmpty then p(m.currentNotes)
        else p(cls := "text-muted", "No notes"),
      ),

      // Status controls
      div(
        cls := "detail-panel-section",
        h4("Change Status"),
        div(
          cls := "machine-status-controls",
          MachineStatus.values.toList.map { status =>
            button(
              cls := (if m.status == status then "btn-primary" else "btn-secondary"),
              s"${status.icon} ${status.displayName}",
              disabled := (m.status == status),
              onClick --> { _ => ManufacturingViewModel.changeMachineStatus(m.id, status) },
            )
          },
        ),
      ),
    )
