package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.domain.model.Language

object OrderQueueView:

  // ── Form state for adding new orders ───────────────────────────
  private val customerNameVar      = Var("")
  private val productDescriptionVar = Var("")
  private val quantityVar          = Var("100")
  private val priorityVar          = Var(OrderPriority.Normal)
  private val notesVar             = Var("")
  private val showFormVar          = Var(false)

  // Station type checkboxes for required workflow
  private val requiredStationsVar  = Var(Set.empty[StationType])

  def apply(state: Signal[ManufacturingState], lang: Signal[Language]): Element =
    div(
      cls := "mfg-order-queue",

      // Header + actions
      div(
        cls := "mfg-view-header",
        div(
          cls := "mfg-view-header-row",
          div(
            h2(child.text <-- lang.map {
              case Language.En => "Order Queue"
              case Language.Cs => "Fronta objednávek"
            }),
            p(
              cls := "mfg-view-subtitle",
              child.text <-- lang.map {
                case Language.En => "Manage incoming manufacturing orders"
                case Language.Cs => "Správa příchozích výrobních objednávek"
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

      // New order form (toggleable)
      child <-- showFormVar.signal.combineWith(lang).map { case (show, l) =>
        if show then newOrderForm(l) else div()
      },

      // Order table
      child <-- state.combineWith(lang).map { case (s, l) =>
        if s.orders.isEmpty then
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
          )
        else
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
                    th(if l == Language.Cs then "Akce" else "Actions"),
                  ),
                ),
                tbody(
                  s.orders.sortBy(o => (orderStatusSort(o.status), -prioritySort(o.priority), -o.createdAt)).map { order =>
                    tr(
                      cls := s"mfg-order-row ${order.status.cssClass}",
                      td(cls := "mfg-order-id", order.id),
                      td(order.customerName),
                      td(order.productDescription),
                      td(cls := "mfg-order-qty", order.quantity.toString),
                      td(span(cls := s"mfg-priority-badge ${order.priority.cssClass}", order.priority.label(l))),
                      td(span(cls := s"mfg-status-badge ${order.status.cssClass}", order.status.label(l))),
                      td(progressCell(order, l)),
                      td(cls := "mfg-order-actions", orderActions(order, s, l)),
                    )
                  }
                ),
              ),
            ),
          )
      },
    )

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

  private def orderActions(order: ManufacturingOrder, state: ManufacturingState, l: Language): Element =
    div(
      cls := "mfg-action-buttons",
      order.status match
        case OrderStatus.Pending =>
          div(
            button(
              cls := "mfg-btn mfg-btn-sm mfg-btn-primary",
              if l == Language.Cs then "Zahájit" else "Start",
              onClick --> { _ => ManufacturingViewModel.startOrder(order.id) },
            ),
            button(
              cls := "mfg-btn mfg-btn-sm mfg-btn-danger",
              if l == Language.Cs then "Zrušit" else "Cancel",
              onClick --> { _ => ManufacturingViewModel.cancelOrder(order.id) },
            ),
          )
        case OrderStatus.InProgress =>
          // Show available stations for next required step
          val nextRequired = order.requiredStationTypes.filterNot(order.completedStationTypes.contains).headOption
          nextRequired match
            case Some(stType) =>
              val availableStations = state.stations.filter(st =>
                st.stationType == stType && st.currentOrderId.isEmpty && st.isActive
              )
              if availableStations.nonEmpty then
                div(
                  availableStations.map { st =>
                    button(
                      cls := "mfg-btn mfg-btn-sm mfg-btn-assign",
                      s"→ ${st.name}",
                      onClick --> { _ => ManufacturingViewModel.assignOrderToStation(order.id, st.id) },
                    )
                  },
                  button(
                    cls := "mfg-btn mfg-btn-sm mfg-btn-danger",
                    if l == Language.Cs then "Zrušit" else "Cancel",
                    onClick --> { _ => ManufacturingViewModel.cancelOrder(order.id) },
                  ),
                )
              else
                div(
                  span(cls := "mfg-waiting-label", l match
                    case Language.En => s"Waiting for ${stType.label(l)}"
                    case Language.Cs => s"Čeká na ${stType.label(l)}"
                  ),
                  button(
                    cls := "mfg-btn mfg-btn-sm mfg-btn-danger",
                    if l == Language.Cs then "Zrušit" else "Cancel",
                    onClick --> { _ => ManufacturingViewModel.cancelOrder(order.id) },
                  ),
                )
            case None =>
              span(cls := "mfg-waiting-label", if l == Language.Cs then "Čeká" else "Waiting")
        case OrderStatus.AtStation =>
          val stationName = order.currentStationId.flatMap(sid => state.stations.find(_.id == sid)).map(_.name).getOrElse("?")
          span(cls := "mfg-at-station-label", s"@ $stationName")
        case OrderStatus.Completed =>
          span(cls := "mfg-completed-label", "✅")
        case OrderStatus.Cancelled =>
          span(cls := "mfg-cancelled-label", "—")
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
