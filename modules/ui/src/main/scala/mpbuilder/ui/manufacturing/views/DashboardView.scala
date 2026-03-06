package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.domain.model.Language

object DashboardView:

  def apply(state: Signal[ManufacturingState], lang: Signal[Language]): Element =
    div(
      cls := "mfg-dashboard",

      // Header
      div(
        cls := "mfg-view-header",
        h2(child.text <-- lang.map {
          case Language.En => "Manufacturing Dashboard"
          case Language.Cs => "Přehled výroby"
        }),
        p(
          cls := "mfg-view-subtitle",
          child.text <-- lang.map {
            case Language.En => "Overview of current production status"
            case Language.Cs => "Přehled aktuálního stavu výroby"
          },
        ),
      ),

      // Stats cards row
      div(
        cls := "mfg-stats-row",
        statCard(state, lang, "pending"),
        statCard(state, lang, "in-progress"),
        statCard(state, lang, "completed"),
        statCard(state, lang, "stations-active"),
      ),

      // Recent orders
      div(
        cls := "mfg-card",
        h3(child.text <-- lang.map {
          case Language.En => "Recent Orders"
          case Language.Cs => "Poslední objednávky"
        }),
        child <-- state.combineWith(lang).map { case (s, l) =>
          val recent = s.orders.sortBy(-_.createdAt).take(5)
          if recent.isEmpty then
            p(
              cls := "mfg-empty-message",
              l match
                case Language.En => "No orders yet. Add some from the Order Queue view."
                case Language.Cs => "Zatím žádné objednávky. Přidejte je ve frontě objednávek."
            )
          else
            div(
              recent.map { order =>
                div(
                  cls := "mfg-recent-order-item",
                  div(
                    cls := "mfg-recent-order-info",
                    span(cls := "mfg-recent-order-id", order.id),
                    span(cls := "mfg-recent-order-customer", order.customerName),
                    span(cls := "mfg-recent-order-product", order.productDescription),
                  ),
                  span(cls := s"mfg-status-badge ${order.status.cssClass}", order.status.label(l)),
                )
              }
            )
        },
      ),

      // Station status overview
      div(
        cls := "mfg-card",
        h3(child.text <-- lang.map {
          case Language.En => "Station Status"
          case Language.Cs => "Stav stanic"
        }),
        child <-- state.combineWith(lang).map { case (s, l) =>
          div(
            cls := "mfg-station-grid",
            s.stations.map { station =>
              val orderInfo = station.currentOrderId.flatMap(oid => s.orders.find(_.id == oid))
              div(
                cls := s"mfg-station-mini-card ${if station.isActive then "" else "mfg-station-inactive"}",
                div(
                  cls := "mfg-station-mini-header",
                  span(station.stationType.icon),
                  span(station.name),
                ),
                div(
                  cls := "mfg-station-mini-status",
                  orderInfo match
                    case Some(order) =>
                      span(cls := "mfg-station-busy", s"${order.id} — ${order.customerName}")
                    case None =>
                      if station.isActive then
                        span(cls := "mfg-station-free", l match
                          case Language.En => "Available"
                          case Language.Cs => "Volná"
                        )
                      else
                        span(cls := "mfg-station-disabled-label", l match
                          case Language.En => "Disabled"
                          case Language.Cs => "Vypnuta"
                        )
                ),
              )
            }
          )
        },
      ),
    )

  private def statCard(
    state: Signal[ManufacturingState],
    lang: Signal[Language],
    kind: String,
  ): Element =
    div(
      cls := s"mfg-stat-card mfg-stat-$kind",
      child <-- state.combineWith(lang).map { case (s, l) =>
        val (value, label, icon) = kind match
          case "pending" =>
            val count = s.orders.count(o => o.status == OrderStatus.Pending)
            (count.toString, if l == Language.Cs then "Čekající" else "Pending", "⏳")
          case "in-progress" =>
            val count = s.orders.count(o => o.status == OrderStatus.InProgress || o.status == OrderStatus.AtStation)
            (count.toString, if l == Language.Cs then "Probíhající" else "In Progress", "⚙️")
          case "completed" =>
            val count = s.orders.count(_.status == OrderStatus.Completed)
            (count.toString, if l == Language.Cs then "Dokončené" else "Completed", "✅")
          case "stations-active" =>
            val active = s.stations.count(_.isActive)
            val total  = s.stations.size
            (s"$active/$total", if l == Language.Cs then "Aktivní stanice" else "Active Stations", "🏭")
          case _ => ("0", "", "")

        div(
          span(cls := "mfg-stat-icon", icon),
          span(cls := "mfg-stat-value", value),
          span(cls := "mfg-stat-label", label),
        )
      },
    )
