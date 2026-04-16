package mpbuilder.ui

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.SplitRender
import mpbuilder.ui.visualeditor.VisualEditorApp
import mpbuilder.ui.productbuilder.{ProductBuilderApp, ProductBuilderViewModel, BuilderState, LoginState, ArtworkMode}
import mpbuilder.ui.components.{CheckoutView, CustomerPortalView, LoginWidget, OrderHistoryView}
import mpbuilder.ui.manufacturing.ManufacturingApp
import mpbuilder.ui.catalog.CatalogEditorApp
import mpbuilder.ui.customers.CustomerManagementApp
import mpbuilder.ui.productcatalog.ProductCatalogApp
import mpbuilder.domain.model.{CategoryId, Language}

// Legacy route type for backwards compatibility during migration
sealed trait AppRoute
object AppRoute {
  case object ProductCatalog extends AppRoute
  case object ProductBuilder extends AppRoute
  case class VisualEditor(artworkId: Option[String] = None) extends AppRoute
  case object Checkout extends AppRoute
  case object Manufacturing extends AppRoute
  case object CatalogEditor extends AppRoute
  case object CustomerManagement extends AppRoute
  case object OrderHistory extends AppRoute
  case object CustomerPortal extends AppRoute
  /** Detail page for a specific product in the catalog. */
  case class ProductDetail(categoryId: CategoryId) extends AppRoute
}

object AppRouter {
  val basketOpen: Var[Boolean] = Var(false)

  /** Convert legacy AppRoute to new Page type for navigation. */
  private def toPage(route: AppRoute): Page = route match
    case AppRoute.ProductCatalog => Page.ProductCatalog
    case AppRoute.ProductBuilder => Page.ProductBuilder
    case AppRoute.VisualEditor(artId) => Page.VisualEditor(artId)
    case AppRoute.Checkout => Page.Checkout
    case AppRoute.Manufacturing => Page.ManufacturingDashboard
    case AppRoute.CatalogEditor => Page.CatalogCategories
    case AppRoute.CustomerManagement => Page.CustomersList
    case AppRoute.OrderHistory => Page.OrderHistory
    case AppRoute.CustomerPortal => Page.CustomerPortal
    case AppRoute.ProductDetail(cid) => Page.ProductDetail(cid.value)

  /** Convert new Page type to legacy AppRoute for compatibility. */
  private def toAppRoute(page: Page): AppRoute = page match
    case Page.ProductCatalog => AppRoute.ProductCatalog
    case Page.ProductBuilder => AppRoute.ProductBuilder
    case Page.VisualEditor(artId) => AppRoute.VisualEditor(artId)
    case Page.Checkout => AppRoute.Checkout
    case Page.OrderHistory => AppRoute.OrderHistory
    case Page.CustomerPortal => AppRoute.CustomerPortal
    case Page.ProductDetail(id) => AppRoute.ProductDetail(CategoryId.unsafe(id))
    case _: Page.ManufacturingPage => AppRoute.Manufacturing
    case _: Page.CatalogPage => AppRoute.CatalogEditor
    case _: Page.CustomerPage => AppRoute.CustomerManagement

  /** Current route signal derived from Waypoint router's page signal. */
  val currentRoute: Signal[AppRoute] = Router.currentPageSignal.map(toAppRoute)

  /** Navigate to a route using the Waypoint router with History API. */
  def navigateTo(route: AppRoute): Unit = {
    basketOpen.set(false)
    Router.pushState(toPage(route))
  }

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage
    val pageSignal = Router.currentPageSignal

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
            cls <-- pageSignal.map {
              case Page.ProductBuilder | Page.ProductCatalog | _: Page.ProductDetail => ""
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
                  case Language.Cs => " Košík"
                ),
              )
            },
            onClick --> { _ => basketOpen.update(!_) },
          ),
        ),

        // Navigation bar — hidden during checkout
        div(
          cls <-- pageSignal.map {
            case Page.Checkout => "app-navigation app-navigation--hidden"
            case _             => "app-navigation"
          },
          a(
            cls := "nav-link",
            href := Router.relativeUrlForPage(Page.ProductCatalog),
            cls <-- pageSignal.map {
              case Page.ProductCatalog | _: Page.ProductDetail => "nav-link active"
              case _ => "nav-link"
            },
            child.text <-- lang.map {
              case Language.En => "Products"
              case Language.Cs => "Produkty"
            },
            onClick.preventDefault --> { _ =>
              basketOpen.set(false)
              Router.pushState(Page.ProductCatalog)
            }
          ),
          a(
            cls := "nav-link",
            href := Router.relativeUrlForPage(Page.ProductBuilder),
            cls <-- pageSignal.map {
              case Page.ProductBuilder => "nav-link active"
              case _ => "nav-link"
            },
            child.text <-- lang.map {
              case Language.En => "Product Parameters"
              case Language.Cs => "Parametry produktu"
            },
            onClick.preventDefault --> { _ =>
              basketOpen.set(false)
              Router.pushState(Page.ProductBuilder)
            }
          ),
          a(
            cls := "nav-link",
            href := Router.relativeUrlForPage(Page.VisualEditor()),
            cls <-- pageSignal.map {
              case _: Page.VisualEditor => "nav-link active"
              case _ => "nav-link"
            },
            child.text <-- lang.map {
              case Language.En => "Visual Editor"
              case Language.Cs => "Vizuální editor"
            },
            onClick.preventDefault --> { _ =>
              basketOpen.set(false)
              Router.pushState(Page.VisualEditor())
            }
          ),
          a(
            cls := "nav-link",
            href := Router.relativeUrlForPage(Page.ManufacturingDashboard),
            cls <-- pageSignal.map {
              case _: Page.ManufacturingPage => "nav-link active"
              case _ => "nav-link"
            },
            child.text <-- lang.map {
              case Language.En => "Manufacturing"
              case Language.Cs => "Výroba"
            },
            onClick.preventDefault --> { _ =>
              basketOpen.set(false)
              Router.pushState(Page.ManufacturingDashboard)
            }
          ),
          a(
            cls := "nav-link",
            href := Router.relativeUrlForPage(Page.CatalogCategories),
            cls <-- pageSignal.map {
              case _: Page.CatalogPage => "nav-link active"
              case _ => "nav-link"
            },
            child.text <-- lang.map {
              case Language.En => "Catalog Editor"
              case Language.Cs => "Editor katalogu"
            },
            onClick.preventDefault --> { _ =>
              basketOpen.set(false)
              Router.pushState(Page.CatalogCategories)
            }
          ),
          a(
            cls := "nav-link",
            href := Router.relativeUrlForPage(Page.CustomersList),
            cls <-- pageSignal.map {
              case _: Page.CustomerPage => "nav-link active"
              case _ => "nav-link"
            },
            child.text <-- lang.map {
              case Language.En => "Customers"
              case Language.Cs => "Zákazníci"
            },
            onClick.preventDefault --> { _ =>
              basketOpen.set(false)
              Router.pushState(Page.CustomersList)
            }
          ),

          // My Orders — visible only when logged in
          child <-- ProductBuilderViewModel.loginState.combineWith(lang).map { case (ls, l) =>
            ls match
              case _: LoginState.LoggedIn =>
                a(
                  cls := "nav-link",
                  href := Router.relativeUrlForPage(Page.CustomerPortal),
                  cls <-- pageSignal.map {
                    case Page.CustomerPortal => "nav-link active"
                    case _ => "nav-link"
                  },
                  child.text <-- lang.map {
                    case Language.En => "My Orders"
                    case Language.Cs => "Moje objednávky"
                  },
                  onClick.preventDefault --> { _ =>
                    basketOpen.set(false)
                    Router.pushState(Page.CustomerPortal)
                  }
                )
              case _ => emptyNode
          },
        ),
      ),

      // Route content - using SplitRender for efficient page rendering
      child <-- pageSignal.map { page =>
        page match
          case Page.ProductCatalog => ProductCatalogApp()
          case Page.ProductDetail(categoryIdStr) => ProductCatalogApp.detailPage(CategoryId.unsafe(categoryIdStr))
          case Page.ProductBuilder => ProductBuilderApp()
          case Page.VisualEditor(artId) => VisualEditorApp(artId)
          case Page.Checkout => CheckoutView()
          case Page.OrderHistory => OrderHistoryView()
          case Page.CustomerPortal => CustomerPortalView()
          case _: Page.ManufacturingPage => ManufacturingApp()
          case _: Page.CatalogPage => CatalogEditorApp()
          case _: Page.CustomerPage => CustomerManagementApp()
      }
    )
  }
}
