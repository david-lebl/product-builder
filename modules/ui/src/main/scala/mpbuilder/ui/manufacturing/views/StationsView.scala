package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.domain.model.Language

object StationsView:

  def apply(state: Signal[ManufacturingState], lang: Signal[Language]): Element =
    div(
      cls := "mfg-stations",

      // Header
      div(
        cls := "mfg-view-header",
        h2(child.text <-- lang.map {
          case Language.En => "Production Stations"
          case Language.Cs => "Výrobní stanice"
        }),
        p(
          cls := "mfg-view-subtitle",
          child.text <-- lang.map {
            case Language.En => "Manage production stations and monitor current work"
            case Language.Cs => "Správa výrobních stanic a sledování aktuální práce"
          },
        ),
      ),

      // Group stations by type
      child <-- state.combineWith(lang).map { case (s, l) =>
        val grouped = s.stations.groupBy(_.stationType)
        val typeOrder = StationType.values.toList.filter(grouped.contains)

        div(
          typeOrder.map { stType =>
            val stations = grouped(stType)
            div(
              cls := "mfg-station-type-group",
              h3(cls := "mfg-station-type-header", s"${stType.icon} ${stType.label(l)}"),
              div(
                cls := "mfg-station-cards",
                stations.map { station =>
                  stationCard(station, s, l)
                }
              ),
            )
          }
        )
      },
    )

  private def stationCard(station: ManufacturingStation, state: ManufacturingState, l: Language): Element =
    val orderOpt = station.currentOrderId.flatMap(oid => state.orders.find(_.id == oid))

    div(
      cls := s"mfg-station-card ${if station.isActive then "" else "mfg-station-inactive"}",

      // Header
      div(
        cls := "mfg-station-card-header",
        div(
          span(cls := "mfg-station-name", station.name),
          span(cls := s"mfg-station-status-dot ${if station.isActive then (if orderOpt.isDefined then "busy" else "free") else "disabled"}"),
        ),
        button(
          cls := s"mfg-btn mfg-btn-sm ${if station.isActive then "mfg-btn-secondary" else "mfg-btn-primary"}",
          disabled := station.currentOrderId.isDefined,
          if station.isActive then
            (if l == Language.Cs then "Vypnout" else "Disable")
          else
            (if l == Language.Cs then "Zapnout" else "Enable"),
          onClick --> { _ => ManufacturingViewModel.toggleStationActive(station.id) },
        ),
      ),

      // Current order or available status
      div(
        cls := "mfg-station-card-body",
        orderOpt match
          case Some(order) =>
            div(
              cls := "mfg-station-current-order",
              div(
                cls := "mfg-station-order-info",
                span(cls := "mfg-station-order-id", order.id),
                span(cls := "mfg-station-order-customer", order.customerName),
                span(cls := "mfg-station-order-product", order.productDescription),
                span(cls := "mfg-station-order-qty", s"${if l == Language.Cs then "Množství" else "Qty"}: ${order.quantity}"),
              ),
              button(
                cls := "mfg-btn mfg-btn-sm mfg-btn-success",
                if l == Language.Cs then "✓ Dokončit práci" else "✓ Complete Work",
                onClick --> { _ => ManufacturingViewModel.completeStationWork(station.id) },
              ),
            )
          case None =>
            if station.isActive then
              div(
                cls := "mfg-station-available",
                span(cls := "mfg-station-available-icon", "✓"),
                span(l match
                  case Language.En => "Available — assign an order from the queue"
                  case Language.Cs => "Volná — přiřaďte objednávku z fronty"
                ),
              )
            else
              div(
                cls := "mfg-station-disabled",
                span(cls := "mfg-station-disabled-icon", "⏸"),
                span(l match
                  case Language.En => "Station disabled"
                  case Language.Cs => "Stanice vypnuta"
                ),
              )
      ),
    )
