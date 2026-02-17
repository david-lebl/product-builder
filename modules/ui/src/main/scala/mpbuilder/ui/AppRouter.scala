package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.CalendarBuilderApp
import mpbuilder.domain.model.Language

sealed trait AppRoute
object AppRoute {
  case object ProductBuilder extends AppRoute
  case object CalendarBuilder extends AppRoute
}

object AppRouter {
  private val currentRouteVar: Var[AppRoute] = Var(AppRoute.ProductBuilder)
  val currentRoute: Signal[AppRoute] = currentRouteVar.signal
  
  def navigateTo(route: AppRoute): Unit = {
    currentRouteVar.set(route)
  }
  
  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage
    
    div(
      // Navigation header
      div(
        cls := "app-navigation",
        button(
          cls := "nav-link",
          cls <-- currentRoute.map {
            case AppRoute.ProductBuilder => "active"
            case _ => ""
          },
          child.text <-- lang.map {
            case Language.En => "Product Builder"
            case Language.Cs => "Stavitel produktů"
          },
          onClick --> { _ => navigateTo(AppRoute.ProductBuilder) }
        ),
        button(
          cls := "nav-link",
          cls <-- currentRoute.map {
            case AppRoute.CalendarBuilder => "active"
            case _ => ""
          },
          "Calendar Builder",
          onClick --> { _ => navigateTo(AppRoute.CalendarBuilder) }
        )
      ),
      
      // Route content
      child <-- currentRoute.map {
        case AppRoute.ProductBuilder => ProductBuilderApp()
        case AppRoute.CalendarBuilder => CalendarBuilderApp()
      }
    )
  }
}
