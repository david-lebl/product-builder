package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.VisualEditorApp
import mpbuilder.ui.productbuilder.{ProductBuilderApp, ProductBuilderViewModel, BuilderState, LoginState, ArtworkMode}
import mpbuilder.ui.components.{CheckoutView, CustomerPortalView, LoginWidget, OrderHistoryView}
import mpbuilder.ui.manufacturing.ManufacturingApp
import mpbuilder.ui.catalog.CatalogEditorApp
import mpbuilder.ui.customers.CustomerManagementApp
import mpbuilder.domain.model.Language

sealed trait AppRoute
object AppRoute {
  case object ProductBuilder extends AppRoute
  case class VisualEditor(artworkId: Option[String] = None) extends AppRoute
  case object Checkout extends AppRoute
  case object Manufacturing extends AppRoute
  case object CatalogEditor extends AppRoute
  case object CustomerManagement extends AppRoute
  case object OrderHistory extends AppRoute
  case object CustomerPortal extends AppRoute
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

          // Login widget — replaces the generic user indicator
          LoginWidget(),

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
                span(cls := "basket-icon", "\uD83D\uDED2"),
                if count > 0 then span(cls := "basket-badge", if count > 99 then "99+" else count.toString) else emptyNode,
                span(cls := "basket-btn-label", l match
                  case Language.En => " Basket"
                  case Language.Cs => " Kosik"
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
              case _: AppRoute.VisualEditor => "active"
              case _ => ""
            },
            child.text <-- lang.map {
              case Language.En => "Visual Editor"
              case Language.Cs => "Vizuální editor"
            },
            onClick --> { _ => navigateTo(AppRoute.VisualEditor()) }
          ),
          button(
            cls := "nav-link",
            cls <-- currentRoute.map {
              case AppRoute.Manufacturing => "active"
              case _ => ""
            },
            child.text <-- lang.map {
              case Language.En => "Manufacturing"
              case Language.Cs => "Vyroba"
            },
            onClick --> { _ => navigateTo(AppRoute.Manufacturing) }
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
          button(
            cls := "nav-link",
            cls <-- currentRoute.map {
              case AppRoute.CustomerManagement => "active"
              case _ => ""
            },
            child.text <-- lang.map {
              case Language.En => "Customers"
              case Language.Cs => "Zakaznici"
            },
            onClick --> { _ => navigateTo(AppRoute.CustomerManagement) }
          ),

          // My Orders — visible only when logged in
          child <-- ProductBuilderViewModel.loginState.combineWith(lang).map { case (ls, l) =>
            ls match
              case _: LoginState.LoggedIn =>
                button(
                  cls := "nav-link",
                  cls <-- currentRoute.map {
                    case AppRoute.CustomerPortal => "active"
                    case _ => ""
                  },
                  child.text <-- lang.map {
                    case Language.En => "My Orders"
                    case Language.Cs => "Moje objednavky"
                  },
                  onClick --> { _ => navigateTo(AppRoute.CustomerPortal) }
                )
              case _ => emptyNode
          },
        ),
      ),

      // Route content
      child <-- currentRoute.map {
        case AppRoute.ProductBuilder       => ProductBuilderApp()
        case AppRoute.VisualEditor(artId)  => VisualEditorApp(artId)
        case AppRoute.Checkout             => CheckoutView()
        case AppRoute.Manufacturing        => ManufacturingApp()
        case AppRoute.CatalogEditor        => CatalogEditorApp()
        case AppRoute.CustomerManagement   => CustomerManagementApp()
        case AppRoute.OrderHistory         => OrderHistoryView()
        case AppRoute.CustomerPortal       => CustomerPortalView()
      }
    )
  }
}
