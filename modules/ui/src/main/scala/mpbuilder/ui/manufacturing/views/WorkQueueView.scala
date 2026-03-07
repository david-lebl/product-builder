package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.domain.model.Language

/** Unified work queue view that replaces the separate Order Queue and Stations
  * views.  Users can filter by station type, pick up / complete orders with
  * a single button, and click a row to open a side detail panel.
  */
object WorkQueueView:

  // ── Form state for adding new orders ───────────────────────────
  private val customerNameVar       = Var("")
  private val productDescriptionVar = Var("")
  private val quantityVar           = Var("100")
  private val priorityVar           = Var(OrderPriority.Normal)
  private val notesVar              = Var("")
  private val showFormVar           = Var(false)
  private val requiredStationsVar   = Var(Set.empty[StationType])

  def apply(state: Signal[ManufacturingState], lang: Signal[Language]): Element =
    div(
      cls := "mfg-work-queue",

      // Header + actions
      div(
        cls := "mfg-view-header",
        div(
          cls := "mfg-view-header-row",
          div(
            h2(child.text <-- lang.map {
              case Language.En => "Work Queue"
              case Language.Cs => "Pracovní fronta"
            }),
            p(
              cls := "mfg-view-subtitle",
              child.text <-- lang.map {
                case Language.En => "Pick up, process, and complete orders in one place"
                case Language.Cs => "Převezměte, zpracujte a dokončete objednávky na jednom místě"
              },
            ),
          ),
          div(
            cls := "mfg-header-actions",
            button(
              cls := "mfg-btn mfg-btn-primary",
              child.text <-- lang.map {
                case Language.En => "+ New Order"
                case Language.Cs => "+ Nová objednávka"
              },
              onClick --> { _ => showFormVar.update(!_) },
            ),
            button(
              cls := "mfg-btn mfg-btn-secondary",
              child.text <-- lang.map {
                case Language.En => "Load Sample Data"
                case Language.Cs => "Načíst vzorová data"
              },
              onClick --> { _ => ManufacturingViewModel.loadSampleData() },
            ),
            button(
              cls := "mfg-btn mfg-btn-danger",
              child.text <-- lang.map {
                case Language.En => "Clear Completed"
                case Language.Cs => "Vymazat dokončené"
              },
              onClick --> { _ => ManufacturingViewModel.removeCompletedOrders() },
            ),
          ),
        ),
      ),

      // Station filter chips
      child <-- state.combineWith(lang).map { case (s, l) =>
        div(
          cls := "mfg-filter-bar",
          span(
            cls := "mfg-filter-label",
            l match
              case Language.En => "Filter by station:"
              case Language.Cs => "Filtr dle stanice:"
          ),
          // "All" chip
          button(
            cls := s"mfg-filter-chip${if s.stationFilter.isEmpty then " active" else ""}",
            l match
              case Language.En => "All"
              case Language.Cs => "Vše",
            onClick --> { _ => ManufacturingViewModel.clearStationFilter() },
          ),
          StationType.values.toList.map { st =>
            button(
              cls := s"mfg-filter-chip${if s.stationFilter.contains(st) then " active" else ""}",
              s"${st.icon} ${st.label(l)}",
              onClick --> { _ => ManufacturingViewModel.toggleStationFilter(st) },
            )
          },
        )
      },

      // New order form (toggleable)
      child <-- showFormVar.signal.combineWith(lang).map { case (show, l) =>
        if show then newOrderForm(l) else div()
      },

      // Main content: table + detail panel
      div(
        cls := "mfg-wq-split",

        // Order table
        child <-- state.combineWith(lang).map { case (s, l) =>
          val filtered = filterOrders(s)
          if filtered.isEmpty && s.orders.isEmpty then
            div(
              cls := "mfg-wq-table-pane",
              div(
                cls := "mfg-card mfg-empty-state",
                div(cls := "mfg-empty-icon", "📋"),
                p(l match
                  case Language.En => "No orders in the queue"
                  case Language.Cs => "Ve frontě nejsou žádné objednávky"
                ),
                p(
                  cls := "mfg-empty-hint",
                  l match
                    case Language.En => "Click \"+ New Order\" or \"Load Sample Data\" to get started."
                    case Language.Cs => "Klikněte na \"+ Nová objednávka\" nebo \"Načíst vzorová data\"."
                ),
              ),
            )
          else if filtered.isEmpty then
            div(
              cls := "mfg-wq-table-pane",
              div(
                cls := "mfg-card mfg-empty-state",
                div(cls := "mfg-empty-icon", "🔍"),
                p(l match
                  case Language.En => "No orders match the selected station filter"
                  case Language.Cs => "Žádné objednávky neodpovídají vybranému filtru stanice"
                ),
              ),
            )
          else
            div(
              cls := "mfg-wq-table-pane",
              div(
                cls := "mfg-card",
                div(
                  cls := "mfg-table-wrapper",
                  table(
                    cls := "mfg-table",
                    thead(
                      tr(
                        th("ID"),
                        th(if l == Language.Cs then "Zákazník" else "Customer"),
                        th(if l == Language.Cs then "Produkt" else "Product"),
                        th(if l == Language.Cs then "Množství" else "Qty"),
                        th(if l == Language.Cs then "Priorita" else "Priority"),
                        th(if l == Language.Cs then "Stav" else "Status"),
                        th(if l == Language.Cs then "Postup" else "Progress"),
                        th(if l == Language.Cs then "Akce" else "Action"),
                      ),
                    ),
                    tbody(
                      filtered.sortBy(o => (orderStatusSort(o.status), -prioritySort(o.priority), -o.createdAt)).map { order =>
                        tr(
                          cls := s"mfg-order-row ${order.status.cssClass}${if s.selectedOrderId.contains(order.id) then " mfg-row-selected" else ""}",
                          onClick --> { _ => ManufacturingViewModel.selectOrder(Some(order.id)) },
                          td(cls := "mfg-order-id", order.id),
                          td(order.customerName),
                          td(order.productDescription),
                          td(cls := "mfg-order-qty", order.quantity.toString),
                          td(span(cls := s"mfg-priority-badge ${order.priority.cssClass}", order.priority.label(l))),
                          td(span(cls := s"mfg-status-badge ${order.status.cssClass}", order.status.label(l))),
                          td(progressCell(order, l)),
                          td(cls := "mfg-order-actions", actionButton(order, s, l)),
                        )
                      }
                    ),
                  ),
                ),
              ),
            )
        },

        // Detail side panel
        child <-- state.combineWith(lang).map { case (s, l) =>
          s.selectedOrderId.flatMap(id => s.orders.find(_.id == id)) match
            case Some(order) => OrderDetailPanel(order, s, l)
            case None        => emptyDetailPanel(l)
        },
      ),
    )

  /** Filter orders by selected station types: show orders whose next required
    * station (or current station) matches any of the selected filter types.
    * If filter is empty, show all orders.
    */
  private def filterOrders(s: ManufacturingState): List[ManufacturingOrder] =
    if s.stationFilter.isEmpty then s.orders
    else
      s.orders.filter { order =>
        // Current station type
        val currentStationType = order.currentStationId
          .flatMap(sid => s.stations.find(_.id == sid))
          .map(_.stationType)

        // Next required station type
        val nextRequired = order.requiredStationTypes
          .filterNot(order.completedStationTypes.contains)
          .headOption

        val relevantTypes = currentStationType.toSet ++ nextRequired.toSet ++
          order.requiredStationTypes.toSet // also match by any required station

        relevantTypes.exists(s.stationFilter.contains)
      }

  /** Single unified action button per order row */
  private def actionButton(order: ManufacturingOrder, s: ManufacturingState, l: Language): Element =
    order.status match
      case OrderStatus.Pending | OrderStatus.InProgress =>
        val nextRequired = order.requiredStationTypes.filterNot(order.completedStationTypes.contains).headOption
        val hasAvailableStation = nextRequired.exists { stType =>
          s.stations.exists(st => st.stationType == stType && st.currentOrderId.isEmpty && st.isActive)
        }
        val nextLabel = nextRequired.map(_.label(l)).getOrElse("")
        if hasAvailableStation then
          button(
            cls := "mfg-btn mfg-btn-sm mfg-btn-primary",
            l match
              case Language.En => s"▶ Pick up — $nextLabel"
              case Language.Cs => s"▶ Převzít — $nextLabel",
            onClick.stopPropagation --> { _ => ManufacturingViewModel.pickupAndStart(order.id) },
          )
        else
          span(
            cls := "mfg-waiting-label",
            l match
              case Language.En => s"Waiting — $nextLabel"
              case Language.Cs => s"Čeká — $nextLabel"
          )
      case OrderStatus.AtStation =>
        val stationName = order.currentStationId.flatMap(sid => s.stations.find(_.id == sid)).map(_.name).getOrElse("?")
        button(
          cls := "mfg-btn mfg-btn-sm mfg-btn-success",
          l match
            case Language.En => s"✓ Complete — $stationName"
            case Language.Cs => s"✓ Dokončit — $stationName",
          onClick.stopPropagation --> { _ => ManufacturingViewModel.completeAndAdvance(order.id) },
        )
      case OrderStatus.Completed =>
        span(cls := "mfg-completed-label", "✅")
      case OrderStatus.Cancelled =>
        span(cls := "mfg-cancelled-label", "—")

  private def progressCell(order: ManufacturingOrder, l: Language): Element =
    val total     = order.requiredStationTypes.size
    val completed = order.completedStationTypes.size
    if total == 0 then span("—")
    else
      div(
        cls := "mfg-progress",
        div(
          cls := "mfg-progress-bar",
          div(
            cls := "mfg-progress-fill",
            width := s"${if total > 0 then (completed * 100) / total else 0}%",
          ),
        ),
        span(cls := "mfg-progress-text", s"$completed/$total"),
      )

  private def emptyDetailPanel(l: Language): Element =
    div(
      cls := "mfg-wq-detail-pane",
      div(
        cls := "mfg-detail-empty",
        div(cls := "mfg-detail-empty-icon", "👈"),
        p(l match
          case Language.En => "Select an order to view details"
          case Language.Cs => "Vyberte objednávku pro zobrazení detailů"
        ),
      ),
    )

  private def newOrderForm(l: Language): Element =
    div(
      cls := "mfg-card mfg-new-order-form",
      h3(if l == Language.Cs then "Nová objednávka" else "New Order"),

      div(
        cls := "mfg-form-grid",
        div(
          cls := "mfg-form-group",
          label(if l == Language.Cs then "Zákazník" else "Customer Name"),
          input(
            typ := "text",
            placeholder := (if l == Language.Cs then "Název zákazníka" else "Customer name"),
            controlled(
              value <-- customerNameVar.signal,
              onInput.mapToValue --> customerNameVar.writer,
            ),
          ),
        ),
        div(
          cls := "mfg-form-group",
          label(if l == Language.Cs then "Popis produktu" else "Product Description"),
          input(
            typ := "text",
            placeholder := (if l == Language.Cs then "Popis produktu" else "e.g. Business Cards 500pcs"),
            controlled(
              value <-- productDescriptionVar.signal,
              onInput.mapToValue --> productDescriptionVar.writer,
            ),
          ),
        ),
        div(
          cls := "mfg-form-group",
          label(if l == Language.Cs then "Množství" else "Quantity"),
          input(
            typ := "number",
            minAttr := "1",
            controlled(
              value <-- quantityVar.signal,
              onInput.mapToValue --> quantityVar.writer,
            ),
          ),
        ),
        div(
          cls := "mfg-form-group",
          label(if l == Language.Cs then "Priorita" else "Priority"),
          select(
            value <-- priorityVar.signal.map(_.ordinal.toString),
            OrderPriority.values.toList.map { p =>
              option(value := p.ordinal.toString, p.label(l))
            },
            onChange.mapToValue --> { v =>
              priorityVar.set(OrderPriority.fromOrdinal(v.toInt))
            },
          ),
        ),
      ),

      // Required stations checkboxes
      div(
        cls := "mfg-form-group",
        label(if l == Language.Cs then "Požadované stanice (workflow)" else "Required Stations (workflow)"),
        div(
          cls := "mfg-station-checkboxes",
          StationType.values.toList.map { st =>
            label(
              cls := "mfg-checkbox-label",
              input(
                typ := "checkbox",
                checked <-- requiredStationsVar.signal.map(_.contains(st)),
                onChange.mapToChecked --> { isChecked =>
                  requiredStationsVar.update { current =>
                    if isChecked then current + st else current - st
                  }
                },
              ),
              span(s"${st.icon} ${st.label(l)}"),
            )
          }
        ),
      ),

      // Notes
      div(
        cls := "mfg-form-group",
        label(if l == Language.Cs then "Poznámky" else "Notes"),
        input(
          typ := "text",
          placeholder := (if l == Language.Cs then "Volitelné poznámky" else "Optional notes"),
          controlled(
            value <-- notesVar.signal,
            onInput.mapToValue --> notesVar.writer,
          ),
        ),
      ),

      // Buttons
      div(
        cls := "mfg-form-actions",
        button(
          cls := "mfg-btn mfg-btn-primary",
          if l == Language.Cs then "Vytvořit objednávku" else "Create Order",
          onClick --> { _ =>
            val qty = quantityVar.now().toIntOption.getOrElse(1)
            val stations = requiredStationsVar.now().toList
            if customerNameVar.now().nonEmpty && productDescriptionVar.now().nonEmpty && stations.nonEmpty then
              ManufacturingViewModel.addOrder(
                customerNameVar.now(),
                productDescriptionVar.now(),
                qty,
                priorityVar.now(),
                stations,
                notesVar.now(),
              )
              // Reset form
              customerNameVar.set("")
              productDescriptionVar.set("")
              quantityVar.set("100")
              priorityVar.set(OrderPriority.Normal)
              notesVar.set("")
              requiredStationsVar.set(Set.empty)
              showFormVar.set(false)
          },
        ),
        button(
          cls := "mfg-btn mfg-btn-secondary",
          if l == Language.Cs then "Zrušit" else "Cancel",
          onClick --> { _ => showFormVar.set(false) },
        ),
      ),
    )

  private def orderStatusSort(status: OrderStatus): Int = status match
    case OrderStatus.Pending    => 0
    case OrderStatus.InProgress => 1
    case OrderStatus.AtStation  => 2
    case OrderStatus.Completed  => 3
    case OrderStatus.Cancelled  => 4

  private def prioritySort(priority: OrderPriority): Int = priority match
    case OrderPriority.Low    => 0
    case OrderPriority.Normal => 1
    case OrderPriority.High   => 2
    case OrderPriority.Urgent => 3
