package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.CalendarBuilderApp
import mpbuilder.ui.catalog.CatalogEditorApp
import mpbuilder.ui.components.CheckoutView
import mpbuilder.domain.model.Language

sealed trait AppRoute
object AppRoute {
  case object ProductBuilder extends AppRoute
  case object CalendarBuilder extends AppRoute
  case object CatalogEditor extends AppRoute
  case object Checkout extends AppRoute
}

object AppRouter {
  private val currentRouteVar: Var[AppRoute] = Var(AppRoute.ProductBuilder)
  val currentRoute: Signal[AppRoute] = currentRouteVar.signal
  val basketOpen: Var[Boolean] = Var(false)

  def navigateTo(route: AppRoute): Unit = {
    basketOpen.set(false)
    currentRouteVar.set(route)
  }

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      // Sticky wrapper: top bar + navigation
      div(
        cls := "top-bar-wrapper",

        // Top bar: logo, user, language, basket
        div(
          cls := "top-bar",
          span(cls := "top-bar-logo", "Product Builder"),
          div(cls := "top-bar-spacer"),

          // User indicator
          div(
            cls := "top-bar-user",
            span(cls := "top-bar-user-icon", "👤"),
            span(child.text <-- lang.map {
              case Language.En => "Guest"
              case Language.Cs => "Host"
            }),
          ),

          // Language selector
          div(
            cls := "language-selector",
            label("Language / Jazyk: "),
            select(
              value <-- lang.map(_.toCode),
              option("EN", value := "en"),
              option("CZ", value := "cs"),
              onChange.mapToValue --> { code =>
                ProductBuilderViewModel.setLanguage(Language.fromCode(code))
              },
            ),
          ),

          // Basket button in top bar
          button(
            cls := "nav-basket-btn",
            cls <-- currentRoute.map {
              case AppRoute.ProductBuilder => ""
              case _ => "nav-basket-btn-hidden"
            },
            child <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
              val count = state.basket.items.size
              span(
                cls := "nav-basket-content",
                span(cls := "basket-icon", "🛒"),
                if count > 0 then span(cls := "basket-badge", if count > 99 then "99+" else count.toString) else emptyNode,
                span(cls := "basket-btn-label", l match
                  case Language.En => " Basket"
                  case Language.Cs => " Košík"
                ),
              )
            },
            onClick --> { _ => basketOpen.update(!_) },
          ),
        ),

        // Navigation bar — hidden during checkout
        div(
          cls <-- currentRoute.map {
            case AppRoute.Checkout => "app-navigation app-navigation--hidden"
            case _                 => "app-navigation"
          },
          button(
            cls := "nav-link",
            cls <-- currentRoute.map {
              case AppRoute.ProductBuilder => "active"
              case _ => ""
            },
            child.text <-- lang.map {
              case Language.En => "Product Parameters"
              case Language.Cs => "Parametry produktu"
            },
            onClick --> { _ => navigateTo(AppRoute.ProductBuilder) }
          ),
          button(
            cls := "nav-link",
            cls <-- currentRoute.map {
              case AppRoute.CalendarBuilder => "active"
              case _ => ""
            },
            child.text <-- lang.map {
              case Language.En => "Visual Editor"
              case Language.Cs => "Vizuální editor"
            },
            onClick --> { _ => navigateTo(AppRoute.CalendarBuilder) }
          ),
          button(
            cls := "nav-link",
            cls <-- currentRoute.map {
              case AppRoute.CatalogEditor => "active"
              case _ => ""
            },
            child.text <-- lang.map {
              case Language.En => "Catalog Editor"
              case Language.Cs => "Editor katalogu"
            },
            onClick --> { _ => navigateTo(AppRoute.CatalogEditor) }
          ),
        ),
      ),

      // Route content
      child <-- currentRoute.map {
        case AppRoute.ProductBuilder  => ProductBuilderApp()
        case AppRoute.CalendarBuilder => CalendarBuilderApp()
        case AppRoute.CatalogEditor   => CatalogEditorApp()
        case AppRoute.Checkout        => CheckoutView()
      }
    )
  }
}
