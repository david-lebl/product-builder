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

      // Header with add sample order button
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

      // Station overview cards
      div(
        cls := "mfg-station-grid",
        ManufacturingViewModel.stations.map { station =>
          stationCard(station, lang)
        },
      ),

      // Summary stats
      div(
        cls := "mfg-summary-row",
        statCard(
          lang.map {
            case Language.En => "Total Orders"
            case Language.Cs => "Celkem objednavek"
          },
          ManufacturingViewModel.state.map(_.orders.size.toString),
        ),
        statCard(
          lang.map {
            case Language.En => "In Progress"
            case Language.Cs => "V procesu"
          },
          ManufacturingViewModel.state.map { s =>
            s.orders.count(o => !o.isFullyCompleted && o.currentStationId.isDefined).toString
          },
        ),
        statCard(
          lang.map {
            case Language.En => "Completed"
            case Language.Cs => "Dokonceno"
          },
          ManufacturingViewModel.state.map { s =>
            s.orders.count(_.isFullyCompleted).toString
          },
        ),
      ),
    )

  private def stationCard(station: Station, lang: Signal[Language]): Element =
    div(
      cls := "mfg-station-card",
      onClick --> { _ =>
        ManufacturingViewModel.navigateTo(ManufacturingRoute.StationView(station.id))
      },
      div(
        cls := "mfg-station-card-icon",
        stationIcon(station.stationType),
      ),
      div(
        cls := "mfg-station-card-info",
        h3(station.name(Language.En)),
        child <-- lang.combineWith(ManufacturingViewModel.stationCounts(station.id)).map {
          case (l, counts) =>
            val queued = counts.getOrElse(OrderStatus.Queued, 0)
            val inProgress = counts.getOrElse(OrderStatus.InProgress, 0)
            val onHold = counts.getOrElse(OrderStatus.OnHold, 0)
            div(
              cls := "mfg-station-card-stats",
              if queued > 0 then
                span(cls := "mfg-stat-queued", s"$queued ${if l == Language.En then "queued" else "ve fronte"}")
              else emptyNode,
              if inProgress > 0 then
                span(cls := "mfg-stat-progress", s"$inProgress ${if l == Language.En then "in progress" else "v procesu"}")
              else emptyNode,
              if onHold > 0 then
                span(cls := "mfg-stat-hold", s"$onHold ${if l == Language.En then "on hold" else "pozastaveno"}")
              else emptyNode,
              if queued == 0 && inProgress == 0 && onHold == 0 then
                span(cls := "mfg-stat-empty", if l == Language.En then "Empty" else "Prazdne")
              else emptyNode,
            )
        },
      ),
    )

  private def statCard(label: Signal[String], value: Signal[String]): Element =
    div(
      cls := "mfg-stat-card",
      div(cls := "mfg-stat-value", child.text <-- value),
      div(cls := "mfg-stat-label", child.text <-- label),
    )

  private def stationIcon(st: StationType): String = st match
    case StationType.Printing     => "P"
    case StationType.Cutting      => "C"
    case StationType.Lamination   => "L"
    case StationType.Folding      => "F"
    case StationType.Binding      => "B"
    case StationType.QualityCheck => "Q"
    case StationType.Packaging    => "K"
