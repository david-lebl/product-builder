package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel

object ManufacturingApp:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "mfg-layout",

      // Side menu
      div(
        cls := "mfg-sidebar",
        div(
          cls := "mfg-sidebar-header",
          child.text <-- lang.map {
            case Language.En => "Manufacturing"
            case Language.Cs => "Vyroba"
          },
        ),
        ul(
          cls := "mfg-sidebar-nav",

          // Dashboard
          li(
            cls <-- ManufacturingViewModel.route.map {
              case ManufacturingRoute.Dashboard => "mfg-nav-item active"
              case _                           => "mfg-nav-item"
            },
            a(
              href := "#",
              child.text <-- lang.map {
                case Language.En => "Dashboard"
                case Language.Cs => "Prehled"
              },
              onClick.preventDefault --> { _ =>
                ManufacturingViewModel.navigateTo(ManufacturingRoute.Dashboard)
              },
            ),
          ),

          // Work Queue
          li(
            cls <-- ManufacturingViewModel.route.map {
              case ManufacturingRoute.WorkQueue => "mfg-nav-item active"
              case _                           => "mfg-nav-item"
            },
            a(
              href := "#",
              child.text <-- lang.map {
                case Language.En => "Work Queue"
                case Language.Cs => "Pracovni fronta"
              },
              // Badge with total active orders
              child <-- ManufacturingViewModel.state.map { s =>
                val active = s.orders.count(o => !o.isFullyCompleted && o.currentStationId.isDefined)
                if active > 0 then
                  span(cls := "mfg-nav-badge", active.toString)
                else emptyNode
              },
              onClick.preventDefault --> { _ =>
                ManufacturingViewModel.navigateTo(ManufacturingRoute.WorkQueue)
              },
            ),
          ),

          // Stations section header
          li(
            cls := "mfg-nav-section",
            child.text <-- lang.map {
              case Language.En => "Stations"
              case Language.Cs => "Stanice"
            },
          ),

          // Station links
          children <-- lang.map { l =>
            ManufacturingViewModel.stations.map { station =>
              li(
                cls <-- ManufacturingViewModel.route.map {
                  case ManufacturingRoute.StationView(sid) if sid == station.id =>
                    "mfg-nav-item active"
                  case _ => "mfg-nav-item"
                },
                a(
                  href := "#",
                  span(station.name(l)),
                  // Badge with count
                  child <-- ManufacturingViewModel.stationCounts(station.id).map { counts =>
                    val total = counts.values.sum
                    if total > 0 then
                      span(cls := "mfg-nav-badge", total.toString)
                    else emptyNode
                  },
                  onClick.preventDefault --> { _ =>
                    ManufacturingViewModel.navigateTo(
                      ManufacturingRoute.StationView(station.id)
                    )
                  },
                ),
              )
            }
          },

          // Orders section
          li(cls := "mfg-nav-section",
            child.text <-- lang.map {
              case Language.En => "Orders"
              case Language.Cs => "Objednavky"
            },
          ),
          li(
            cls <-- ManufacturingViewModel.route.map {
              case ManufacturingRoute.AllOrders => "mfg-nav-item active"
              case _                           => "mfg-nav-item"
            },
            a(
              href := "#",
              child.text <-- lang.map {
                case Language.En => "All Orders"
                case Language.Cs => "Vsechny objednavky"
              },
              onClick.preventDefault --> { _ =>
                ManufacturingViewModel.navigateTo(ManufacturingRoute.AllOrders)
              },
            ),
          ),
          li(
            cls <-- ManufacturingViewModel.route.map {
              case ManufacturingRoute.CompletedOrders => "mfg-nav-item active"
              case _                                 => "mfg-nav-item"
            },
            a(
              href := "#",
              child.text <-- lang.map {
                case Language.En => "Completed"
                case Language.Cs => "Dokoncene"
              },
              onClick.preventDefault --> { _ =>
                ManufacturingViewModel.navigateTo(ManufacturingRoute.CompletedOrders)
              },
            ),
          ),
        ),
      ),

      // Main content area
      div(
        cls := "mfg-content",
        child <-- ManufacturingViewModel.route.map {
          case ManufacturingRoute.Dashboard        => DashboardView()
          case ManufacturingRoute.WorkQueue         => WorkQueueView()
          case ManufacturingRoute.StationView(sid)  => StationView(sid)
          case ManufacturingRoute.AllOrders         => AllOrdersView()
          case ManufacturingRoute.CompletedOrders   => CompletedOrdersView()
        },
      ),
    )
