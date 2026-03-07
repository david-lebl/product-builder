package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel

object DashboardView:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "mfg-dashboard",

      // Header
      div(
        cls := "mfg-page-header",
        h2(child.text <-- lang.map {
          case Language.En => "Manufacturing Dashboard"
          case Language.Cs => "Prehled vyroby"
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

      // ── Status overview cards ──
      div(
        cls := "mfg-dash-status-row",
        statusCard(
          "mfg-dash-card-queued",
          lang.map { case Language.En => "Queued"; case Language.Cs => "Ve fronte" },
          ManufacturingViewModel.state.map { s =>
            s.orders.count(o =>
              o.currentStatus.contains(OrderStatus.Queued)
            ).toString
          },
        ),
        statusCard(
          "mfg-dash-card-progress",
          lang.map { case Language.En => "In Progress"; case Language.Cs => "V procesu" },
          ManufacturingViewModel.state.map { s =>
            s.orders.count(o =>
              o.currentStatus.contains(OrderStatus.InProgress)
            ).toString
          },
        ),
        statusCard(
          "mfg-dash-card-hold",
          lang.map { case Language.En => "On Hold"; case Language.Cs => "Pozastaveno" },
          ManufacturingViewModel.state.map { s =>
            s.orders.count(o =>
              o.currentStatus.contains(OrderStatus.OnHold)
            ).toString
          },
        ),
        statusCard(
          "mfg-dash-card-completed",
          lang.map { case Language.En => "Completed"; case Language.Cs => "Dokonceno" },
          ManufacturingViewModel.state.map(_.orders.count(_.isFullyCompleted).toString),
        ),
        statusCard(
          "mfg-dash-card-total",
          lang.map { case Language.En => "Total Orders"; case Language.Cs => "Celkem" },
          ManufacturingViewModel.state.map(_.orders.size.toString),
        ),
      ),

      // ── Station status overview ──
      div(
        cls := "mfg-dash-section",
        h3(
          cls := "mfg-dash-section-title",
          child.text <-- lang.map {
            case Language.En => "Station Status"
            case Language.Cs => "Stav stanic"
          },
        ),
        div(
          cls := "mfg-dash-stations",
          ManufacturingViewModel.stations.map { station =>
            stationStatusCard(station, lang)
          },
        ),
      ),

      // ── Recent orders table ──
      div(
        cls := "mfg-dash-section",
        div(
          cls := "mfg-dash-section-header",
          h3(
            cls := "mfg-dash-section-title",
            child.text <-- lang.map {
              case Language.En => "Recent Orders"
              case Language.Cs => "Posledni objednavky"
            },
          ),
          button(
            cls := "mfg-btn mfg-btn-sm mfg-btn-outline",
            child.text <-- lang.map {
              case Language.En => "View All"
              case Language.Cs => "Zobrazit vse"
            },
            onClick --> { _ =>
              ManufacturingViewModel.navigateTo(ManufacturingRoute.AllOrders)
            },
          ),
        ),
        recentOrdersTable(lang),
      ),
    )

  // ── Status overview card ──

  private def statusCard(
      cardCls: String,
      label: Signal[String],
      value: Signal[String],
  ): Element =
    div(
      cls := s"mfg-dash-card $cardCls",
      div(cls := "mfg-dash-card-value", child.text <-- value),
      div(cls := "mfg-dash-card-label", child.text <-- label),
    )

  // ── Station status card ──

  private def stationStatusCard(station: Station, lang: Signal[Language]): Element =
    div(
      cls := "mfg-dash-station",
      onClick --> { _ =>
        ManufacturingViewModel.navigateTo(ManufacturingRoute.StationView(station.id))
      },

      // Status dot
      child <-- ManufacturingViewModel.stationStatus(station.id).map { status =>
        div(cls := s"mfg-dash-station-dot mfg-dash-dot-${statusCls(status)}")
      },

      // Info
      div(
        cls := "mfg-dash-station-info",
        div(
          cls := "mfg-dash-station-name-row",
          span(cls := "mfg-dash-station-icon", stationIcon(station.stationType)),
          child.text <-- lang.map(l => station.name(l)),
        ),
        // Status label + counts
        child <-- ManufacturingViewModel.stationStatus(station.id)
          .combineWith(ManufacturingViewModel.stationCounts(station.id))
          .combineWith(lang)
          .map { case (status, counts, l) =>
            val queued = counts.getOrElse(OrderStatus.Queued, 0)
            val inProgress = counts.getOrElse(OrderStatus.InProgress, 0)
            val onHold = counts.getOrElse(OrderStatus.OnHold, 0)
            div(
              cls := "mfg-dash-station-meta",
              span(
                cls := s"mfg-dash-station-status mfg-dash-status-${statusCls(status)}",
                statusLabel(status, l),
              ),
              if queued + inProgress + onHold > 0 then
                span(cls := "mfg-dash-station-counts",
                  List(
                    if queued > 0 then Some(s"$queued ${if l == Language.En then "queued" else "ceka"}") else None,
                    if inProgress > 0 then Some(s"$inProgress ${if l == Language.En then "active" else "aktivni"}") else None,
                    if onHold > 0 then Some(s"$onHold ${if l == Language.En then "held" else "pozast."}") else None,
                  ).flatten.mkString(" · ")
                )
              else emptyNode,
            )
          },
      ),

      // Toggle disable button
      child <-- lang.map { l =>
        button(
          cls := "mfg-dash-station-toggle",
          child.text <-- ManufacturingViewModel.disabledStations.map { disabled =>
            if disabled.contains(station.id) then
              if l == Language.En then "Enable" else "Povolit"
            else
              if l == Language.En then "Disable" else "Vypnout"
          },
          onClick.stopPropagation --> { _ =>
            ManufacturingViewModel.toggleStationDisabled(station.id)
          },
        )
      },
    )

  // ── Recent orders table ──

  private def recentOrdersTable(lang: Signal[Language]): Element =
    div(
      cls := "mfg-dash-table-wrap",

      // Header
      div(
        cls := "mfg-dash-table-row mfg-dash-table-header",
        span(child.text <-- lang.map { case Language.En => "Order"; case Language.Cs => "Obj." }),
        span(child.text <-- lang.map { case Language.En => "Customer"; case Language.Cs => "Zakaznik" }),
        span(child.text <-- lang.map { case Language.En => "Product"; case Language.Cs => "Produkt" }),
        span(child.text <-- lang.map { case Language.En => "Qty"; case Language.Cs => "Ks" }),
        span(child.text <-- lang.map { case Language.En => "Station"; case Language.Cs => "Stanice" }),
        span(child.text <-- lang.map { case Language.En => "Status"; case Language.Cs => "Stav" }),
        span(child.text <-- lang.map { case Language.En => "Priority"; case Language.Cs => "Priorita" }),
      ),

      // Body
      children <-- ManufacturingViewModel.recentOrders(8)
        .combineWith(ManufacturingViewModel.state.map(_.stations))
        .combineWith(lang)
        .map { case (orders, stations, l) =>
          if orders.isEmpty then
            List(div(cls := "mfg-empty-message",
              if l == Language.En then "No orders yet. Add a sample order to get started."
              else "Zatim zadne objednavky. Pridejte testovaci objednavku."
            ))
          else
            orders.map { order =>
              val status = order.currentStatus.getOrElse(OrderStatus.Completed)
              val stationName = order.currentStationId
                .flatMap(sid => stations.find(_.id == sid))
                .map(_.name(l))
                .getOrElse(if l == Language.En then "Done" else "Hotovo")

              div(
                cls := s"mfg-dash-table-row mfg-dash-table-row-clickable mfg-dash-trow-${orderStatusCls(status)}",
                span(cls := "mfg-dash-tcol-id", order.id.value),
                span(cls := "mfg-dash-tcol-text", order.customerName),
                span(cls := "mfg-dash-tcol-text", order.configuration.category.name(l)),
                span(cls := "mfg-dash-tcol-qty", order.quantity.toString),
                span(cls := "mfg-dash-tcol-station", stationName),
                span(
                  span(
                    cls := s"mfg-status-badge mfg-status-${orderStatusCls(status)}",
                    orderStatusLabel(status, l),
                  ),
                ),
                span(
                  span(
                    cls := s"mfg-priority-badge mfg-priority-${order.priority.toString.toLowerCase}",
                    priorityLabel(order.priority, l),
                  ),
                ),
                onClick --> { _ =>
                  ManufacturingViewModel.selectOrder(order.id)
                  ManufacturingViewModel.navigateTo(ManufacturingRoute.WorkQueue)
                },
              )
            }
        },
    )

  // ── Helpers ──

  private def statusCls(s: StationStatus): String = s match
    case StationStatus.Available => "available"
    case StationStatus.Busy      => "busy"
    case StationStatus.Disabled  => "disabled"

  private def statusLabel(s: StationStatus, l: Language): String = s match
    case StationStatus.Available => if l == Language.En then "Available" else "K dispozici"
    case StationStatus.Busy      => if l == Language.En then "Busy" else "Pracuje"
    case StationStatus.Disabled  => if l == Language.En then "Disabled" else "Vypnuto"

  private def orderStatusCls(s: OrderStatus): String = s match
    case OrderStatus.Queued     => "queued"
    case OrderStatus.InProgress => "progress"
    case OrderStatus.Completed  => "completed"
    case OrderStatus.OnHold     => "hold"

  private def orderStatusLabel(s: OrderStatus, l: Language): String = s match
    case OrderStatus.Queued     => if l == Language.En then "Queued" else "Ve fronte"
    case OrderStatus.InProgress => if l == Language.En then "In Progress" else "V procesu"
    case OrderStatus.Completed  => if l == Language.En then "Completed" else "Dokonceno"
    case OrderStatus.OnHold     => if l == Language.En then "On Hold" else "Pozastaveno"

  private def priorityLabel(p: OrderPriority, l: Language): String = p match
    case OrderPriority.Low    => if l == Language.En then "Low" else "Nizka"
    case OrderPriority.Normal => if l == Language.En then "Normal" else "Normalni"
    case OrderPriority.High   => if l == Language.En then "High" else "Vysoka"
    case OrderPriority.Urgent => if l == Language.En then "Urgent" else "Pilne"

  private def stationIcon(st: StationType): String = st match
    case StationType.Printing     => "P"
    case StationType.Cutting      => "C"
    case StationType.Lamination   => "L"
    case StationType.Folding      => "F"
    case StationType.Binding      => "B"
    case StationType.QualityCheck => "Q"
    case StationType.Packaging    => "K"
