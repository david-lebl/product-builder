package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel
import scala.scalajs.js.Date

object WorkQueueView:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "mfg-workqueue",

      // Header
      div(
        cls := "mfg-page-header",
        h2(child.text <-- lang.map {
          case Language.En => "Work Queue"
          case Language.Cs => "Pracovni fronta"
        }),
        button(
          cls := "mfg-btn mfg-btn-primary",
          child.text <-- lang.map {
            case Language.En => "+ Add Sample Order"
            case Language.Cs => "+ Pridat testovaci objednavku"
          },
          onClick --> { _ => ManufacturingViewModel.addSampleOrder() },
        ),
      ),

      // Toolbar: station filter dropdown + search
      div(
        cls := "mfg-wq-toolbar",

        // Station multi-select dropdown
        stationFilterDropdown(lang),

        // Search input
        div(
          cls := "mfg-wq-search",
          input(
            typ := "text",
            cls := "mfg-wq-search-input",
            placeholder <-- lang.map {
              case Language.En => "Search orders..."
              case Language.Cs => "Hledat objednavky..."
            },
            value <-- ManufacturingViewModel.searchQuery,
            onInput.mapToValue --> { v => ManufacturingViewModel.searchQueryVar.set(v) },
          ),
        ),
      ),

      // Queue table
      div(
        cls := "mfg-wq-table-wrap",
        // Table header — sortable columns
        div(
          cls := "mfg-wq-row mfg-wq-header",
          sortableHeader("mfg-wq-col-id", SortColumn.Id, lang.map {
            case Language.En => "Order"
            case Language.Cs => "Obj."
          }),
          sortableHeader("mfg-wq-col-customer", SortColumn.Customer, lang.map {
            case Language.En => "Customer"
            case Language.Cs => "Zakaznik"
          }),
          sortableHeader("mfg-wq-col-product", SortColumn.Product, lang.map {
            case Language.En => "Product"
            case Language.Cs => "Produkt"
          }),
          sortableHeader("mfg-wq-col-qty", SortColumn.Qty, lang.map {
            case Language.En => "Qty"
            case Language.Cs => "Ks"
          }),
          sortableHeader("mfg-wq-col-station", SortColumn.Station, lang.map {
            case Language.En => "Station"
            case Language.Cs => "Stanice"
          }),
          sortableHeader("mfg-wq-col-status", SortColumn.Status, lang.map {
            case Language.En => "Status"
            case Language.Cs => "Stav"
          }),
          sortableHeader("mfg-wq-col-priority", SortColumn.Priority, lang.map {
            case Language.En => "Priority"
            case Language.Cs => "Priorita"
          }),
          sortableHeader("mfg-wq-col-deadline", SortColumn.Deadline, lang.map {
            case Language.En => "Deadline"
            case Language.Cs => "Termin"
          }),
          span(cls := "mfg-wq-col-actions", child.text <-- lang.map {
            case Language.En => "Actions"
            case Language.Cs => "Akce"
          }),
        ),

        // Table body
        children <-- ManufacturingViewModel.sortedFilteredOrders
          .combineWith(ManufacturingViewModel.state.map(_.stations))
          .combineWith(lang)
          .map { case (orders, stations, l) =>
            if orders.isEmpty then
              List(div(cls := "mfg-empty-message mfg-wq-empty",
                if l == Language.En then "No orders match the current filters."
                else "Zadne objednavky neodpovidaji filtrum."
              ))
            else
              orders.map(order => orderRow(order, stations, l))
          },
      ),

      // Order detail panel (slide-over)
      child <-- ManufacturingViewModel.selectedOrder
        .combineWith(ManufacturingViewModel.state)
        .combineWith(lang)
        .map { case (selectedId, mfgState, l) =>
          selectedId.flatMap(id => mfgState.orders.find(_.id == id)) match
            case Some(order) => OrderDetailPanel(order, mfgState.stations, l)
            case None        => emptyNode
        },
    )

  private def stationFilterDropdown(lang: Signal[Language]): Element =
    div(
      cls := "mfg-wq-dropdown",

      // Dropdown trigger button
      button(
        cls := "mfg-wq-dropdown-trigger",
        child <-- ManufacturingViewModel.stationFilter.combineWith(lang).map { case (filter, l) =>
          val total = ManufacturingViewModel.stations.size
          val selected = filter.size
          val label = if l == Language.En then "Stations" else "Stanice"
          span(
            s"$label ($selected/$total)",
            span(cls := "mfg-wq-dropdown-arrow", " \u25BE"),
          )
        },
        onClick --> { _ =>
          ManufacturingViewModel.stationDropdownOpenVar.update(!_)
        },
      ),

      // Dropdown menu
      child <-- ManufacturingViewModel.stationDropdownOpenVar.signal.map { isOpen =>
        if !isOpen then emptyNode
        else
          div(
            cls := "mfg-wq-dropdown-menu",
            // Select all / none
            div(
              cls := "mfg-wq-dropdown-controls",
              button(
                cls := "mfg-wq-dropdown-ctrl-btn",
                child.text <-- lang.map {
                  case Language.En => "All"
                  case Language.Cs => "Vse"
                },
                onClick --> { _ =>
                  ManufacturingViewModel.stationFilterVar.set(
                    ManufacturingViewModel.stations.map(_.id).toSet
                  )
                },
              ),
              button(
                cls := "mfg-wq-dropdown-ctrl-btn",
                child.text <-- lang.map {
                  case Language.En => "None"
                  case Language.Cs => "Zadne"
                },
                onClick --> { _ =>
                  ManufacturingViewModel.stationFilterVar.set(Set.empty)
                },
              ),
            ),
            // Station checkboxes
            ManufacturingViewModel.stations.map { station =>
              label(
                cls := "mfg-wq-dropdown-item",
                input(
                  typ := "checkbox",
                  checked <-- ManufacturingViewModel.stationFilter.map(_.contains(station.id)),
                  onChange.mapToChecked --> { _ =>
                    ManufacturingViewModel.toggleStationFilter(station.id)
                  },
                ),
                child.text <-- lang.map(l => s" ${station.name(l)}"),
              )
            },
          )
      },

    )

  private def sortableHeader(
      colCls: String,
      col: SortColumn,
      label: Signal[String],
  ): Element =
    span(
      cls := s"$colCls mfg-wq-sortable",
      child <-- label.combineWith(ManufacturingViewModel.sortColumn)
        .combineWith(ManufacturingViewModel.sortDirection)
        .map { case (text, activeCol, dir) =>
          val arrow =
            if activeCol == col then
              if dir == SortDirection.Asc then " \u25B4" else " \u25BE"
            else ""
          span(
            text,
            span(cls := "mfg-wq-sort-arrow", arrow),
          )
        },
      onClick --> { _ => ManufacturingViewModel.setSort(col) },
    )

  private def orderRow(
      order: ManufacturingOrder,
      stations: List[Station],
      lang: Language,
  ): Element =
    val status = order.currentStatus.getOrElse(OrderStatus.Completed)
    val stationName = order.currentStationId
      .flatMap(sid => stations.find(_.id == sid))
      .map(_.name(lang))
      .getOrElse("-")

    div(
      cls := s"mfg-wq-row mfg-wq-row-${statusClass(status)} mfg-wq-row-clickable",

      span(cls := "mfg-wq-col-id", order.id.value),
      span(cls := "mfg-wq-col-customer", order.customerName),
      span(cls := "mfg-wq-col-product", order.configuration.category.name(lang)),
      span(cls := "mfg-wq-col-qty", order.quantity.toString),
      span(cls := "mfg-wq-col-station", stationName),
      span(
        cls := "mfg-wq-col-status",
        span(
          cls := s"mfg-status-badge mfg-status-${statusClass(status)}",
          statusLabel(status, lang),
        ),
      ),
      span(
        cls := "mfg-wq-col-priority",
        span(
          cls := s"mfg-priority-badge mfg-priority-${order.priority.toString.toLowerCase}",
          priorityLabel(order.priority, lang),
        ),
      ),
      span(
        cls := "mfg-wq-col-deadline",
        order.deadline.map(d => formatDeadline(d, lang)).getOrElse("-"),
      ),
      span(
        cls := "mfg-wq-col-actions",
        actionButtons(order, status, lang),
      ),

      // Click row to open detail (except when clicking buttons)
      onClick --> { e =>
        val target = e.target.asInstanceOf[org.scalajs.dom.Element]
        if target.tagName != "BUTTON" then
          ManufacturingViewModel.selectOrder(order.id)
      },
    )

  private def actionButtons(
      order: ManufacturingOrder,
      status: OrderStatus,
      lang: Language,
  ): Element =
    status match
      case OrderStatus.Queued =>
        div(
          cls := "mfg-wq-actions",
          button(
            cls := "mfg-btn mfg-btn-success mfg-btn-sm",
            if lang == Language.En then "Start" else "Zahajit",
            onClick --> { _ => ManufacturingViewModel.startOrder(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning mfg-btn-sm",
            if lang == Language.En then "Hold" else "Pozastavit",
            onClick --> { _ => ManufacturingViewModel.holdOrder(order.id) },
          ),
        )
      case OrderStatus.InProgress =>
        div(
          cls := "mfg-wq-actions",
          button(
            cls := "mfg-btn mfg-btn-success mfg-btn-sm",
            if lang == Language.En then "Done" else "Hotovo",
            onClick --> { _ => ManufacturingViewModel.completeAndAdvance(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning mfg-btn-sm",
            if lang == Language.En then "Hold" else "Pozastavit",
            onClick --> { _ => ManufacturingViewModel.holdOrder(order.id) },
          ),
        )
      case OrderStatus.OnHold =>
        div(
          cls := "mfg-wq-actions",
          button(
            cls := "mfg-btn mfg-btn-primary mfg-btn-sm",
            if lang == Language.En then "Resume" else "Obnovit",
            onClick --> { _ => ManufacturingViewModel.resumeOrder(order.id) },
          ),
        )
      case OrderStatus.Completed =>
        div(
          cls := "mfg-wq-actions",
          span(
            cls := "mfg-status-badge mfg-status-completed",
            if lang == Language.En then "Done" else "Hotovo",
          ),
        )

  private def statusClass(status: OrderStatus): String = status match
    case OrderStatus.Queued     => "queued"
    case OrderStatus.InProgress => "progress"
    case OrderStatus.Completed  => "completed"
    case OrderStatus.OnHold     => "hold"

  private def statusLabel(status: OrderStatus, lang: Language): String = status match
    case OrderStatus.Queued     => if lang == Language.En then "Queued" else "Ve fronte"
    case OrderStatus.InProgress => if lang == Language.En then "In Progress" else "V procesu"
    case OrderStatus.Completed  => if lang == Language.En then "Completed" else "Dokonceno"
    case OrderStatus.OnHold     => if lang == Language.En then "On Hold" else "Pozastaveno"

  private def priorityLabel(p: OrderPriority, lang: Language): String = p match
    case OrderPriority.Low    => if lang == Language.En then "Low" else "Nizka"
    case OrderPriority.Normal => if lang == Language.En then "Normal" else "Normalni"
    case OrderPriority.High   => if lang == Language.En then "High" else "Vysoka"
    case OrderPriority.Urgent => if lang == Language.En then "Urgent" else "Pilne"

  private def formatDeadline(deadline: Long, lang: Language): String =
    val now = Date.now()
    val diffMs = deadline - now
    val diffHours = (diffMs / (1000.0 * 3600)).toInt
    val diffDays = diffHours / 24
    if diffMs < 0 then
      val overdueDays = (-diffDays).max(1)
      if lang == Language.En then s"${overdueDays}d overdue" else s"${overdueDays}d po terminu"
    else if diffDays == 0 then
      if lang == Language.En then s"${diffHours}h left" else s"${diffHours}h zbyva"
    else
      if lang == Language.En then s"${diffDays}d left" else s"${diffDays}d zbyva"
