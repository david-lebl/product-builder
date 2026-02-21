package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.components.*
import mpbuilder.domain.model.Language
import mpbuilder.domain.pricing.{Money, Currency}

object ProductBuilderApp:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val priceOpen = Var(false)

    div(
      // Main content grid
      div(
        cls := "main-content",

        // Left side: Configuration form
        div(
          cls := "card",
          h2(child.text <-- lang.map {
            case Language.En => "Product Parameters"
            case Language.Cs => "Parametry produktu"
          }),

          ConfigurationForm(),
        ),

        // Right side: Price preview + Validation (sticky on desktop, pinned bottom on mobile)
        div(
          cls <-- priceOpen.signal.map(o =>
            if o then "price-section price-expanded" else "price-section"
          ),

          // Mobile summary bar — always visible on mobile, hidden on desktop via CSS
          div(
            cls := "price-mobile-summary",
            div(
              cls := "price-mobile-total",
              child.text <-- ProductBuilderViewModel.state.map { state =>
                state.priceBreakdown match
                  case Some(b) => formatMoney(b.total, b.currency)
                  case None => "—"
              },
            ),
            div(
              cls <-- ProductBuilderViewModel.state.map { state =>
                if state.validationErrors.isEmpty && state.configuration.isDefined then
                  "price-mobile-status valid"
                else if state.validationErrors.nonEmpty then
                  "price-mobile-status invalid"
                else
                  "price-mobile-status"
              },
              child.text <-- ProductBuilderViewModel.state.map { state =>
                if state.validationErrors.isEmpty && state.configuration.isDefined then "✓"
                else if state.validationErrors.nonEmpty then "✗"
                else "…"
              },
            ),
            button(
              cls := "price-expand-btn",
              child.text <-- priceOpen.signal.map(o => if o then "▲" else "▼"),
              onClick --> { _ => priceOpen.update(!_) },
            ),
          ),

          // Detail content — always visible on desktop, toggle on mobile
          div(
            cls := "price-detail-content",
            PricePreview(),
            ValidationMessages(),
          ),
        ),
      ),

      // Basket drawer — slides in from the right
      div(
        cls <-- AppRouter.basketOpen.signal.map(o => if o then "basket-drawer open" else "basket-drawer"),
        div(
          cls := "basket-drawer-close-row",
          button(
            cls := "basket-drawer-close",
            "×",
            onClick --> { _ => AppRouter.basketOpen.set(false) },
          ),
        ),
        BasketView(),
      ),

      // Backdrop overlay — closes basket drawer when clicked
      div(
        cls <-- AppRouter.basketOpen.signal.map(o => if o then "basket-overlay visible" else "basket-overlay"),
        onClick --> { _ => AppRouter.basketOpen.set(false) },
      ),
    )

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
