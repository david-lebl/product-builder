package mpbuilder.calculator

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.productbuilder.ProductBuilderApp
import mpbuilder.ui.productbuilder.ProductBuilderViewModel

/** The standalone calculator's shell.
  *
  * A thin bar (language switch + basket button) over the shared
  * [[ProductBuilderApp]] — no navigation, no login, no routing.
  */
object CalculatorApp:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "mp-calculator",

      div(
        cls := "calculator-bar",

        div(cls := "top-bar-spacer"),

        // Language selector
        div(
          cls := "language-selector",
          select(
            value <-- lang.map(_.toCode),
            option("EN", value := "en"),
            option("CZ", value := "cs"),
            onChange.mapToValue --> { code =>
              ProductBuilderViewModel.setLanguage(Language.fromCode(code))
            },
          ),
        ),

        // Basket button
        button(
          cls := "nav-basket-btn",
          child <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
            val count = state.basket.items.size
            span(
              cls := "nav-basket-content",
              span(cls := "basket-icon", "🛒"),
              if count > 0 then span(cls := "basket-badge", if count > 99 then "99+" else count.toString)
              else emptyNode,
              span(cls := "basket-btn-label", l match
                case Language.En => " Basket"
                case Language.Cs => " Košík"
              ),
            )
          },
          onClick --> { _ => CalculatorEnvironment.basketOpen.update(!_) },
        ),
      ),

      ProductBuilderApp(),
    )
