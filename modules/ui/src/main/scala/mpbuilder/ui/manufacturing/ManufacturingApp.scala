package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.views.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Main manufacturing module view with sidebar navigation. */
object ManufacturingApp:

  def apply(): Element =
    val lang  = ProductBuilderViewModel.currentLanguage
    val state = ManufacturingViewModel.state

    div(
      cls := "mfg-app",

      // Sidebar
      div(
        cls := "mfg-sidebar",
        div(
          cls := "mfg-sidebar-header",
          span(cls := "mfg-sidebar-icon", "🏭"),
          span(
            cls := "mfg-sidebar-title",
            child.text <-- lang.map {
              case Language.En => "Manufacturing"
              case Language.Cs => "Výroba"
            },
          ),
        ),
        div(
          cls := "mfg-sidebar-nav",
          ManufacturingRoute.values.toList.map { route =>
            child <-- state.map(_.currentRoute).combineWith(lang).map { case (current, l) =>
              val isActive   = current == route
              val isDisabled = !route.isAvailable

              button(
                cls := s"mfg-sidebar-link${if isActive then " active" else ""}${if isDisabled then " disabled" else ""}",
                disabled := isDisabled,
                span(cls := "mfg-sidebar-link-icon", route.icon),
                span(cls := "mfg-sidebar-link-text", route.label(l)),
                if isDisabled then
                  span(cls := "mfg-sidebar-coming-soon", l match
                    case Language.En => "Soon"
                    case Language.Cs => "Brzy"
                  )
                else emptyNode,
                onClick --> { _ => ManufacturingViewModel.navigateTo(route) },
              )
            }
          }
        ),

        // Roadmap info at the bottom of sidebar
        div(
          cls := "mfg-sidebar-footer",
          child.text <-- lang.map {
            case Language.En => "v0.2 — Work Queue"
            case Language.Cs => "v0.2 — Pracovní fronta"
          },
        ),
      ),

      // Main content area
      div(
        cls := "mfg-content",
        child <-- state.map(_.currentRoute).map {
          case ManufacturingRoute.Dashboard => DashboardView(state, lang)
          case ManufacturingRoute.WorkQueue => WorkQueueView(state, lang)
          case ManufacturingRoute.Stations  => StationsView(state, lang)
          case _                            => DashboardView(state, lang) // fallback
        },
      ),
    )
