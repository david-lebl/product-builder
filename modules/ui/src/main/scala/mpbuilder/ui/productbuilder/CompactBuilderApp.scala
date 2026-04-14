package mpbuilder.ui.productbuilder

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.components.*
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.uikit.fields.{SelectField, SelectOption, CheckboxField}
import mpbuilder.uikit.util.Visibility

/** Compact product builder view for experienced employees.
  *
  * Same functionality as ProductBuilderApp but with:
  *   - Inline labels (label left, field right) via CSS class
  *   - No help/info elements
  *   - Customer selector at the top
  */
object CompactBuilderApp:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val priceOpen = Var(false)

    div(
      cls := "compact-builder",

      // Main content grid (same structure as ProductBuilderApp)
      div(
        cls := "main-content",

        // Left side: compact configuration form
        div(
          cls := "card",
          h2(child.text <-- lang.map {
            case Language.En => "Quick Entry"
            case Language.Cs => "Rychlé zadání"
          }),

          // Customer selector section
          customerSelector(),

          // Compact configuration form
          CompactConfigurationForm(),
        ),

        // Right side: Price preview + Validation (sticky on desktop)
        div(
          cls <-- priceOpen.signal.map(o =>
            if o then "price-section price-expanded" else "price-section"
          ),

          // Mobile summary bar
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

          // Detail content
          div(
            cls := "price-detail-content",
            PricePreview(),
            ValidationMessages(),
          ),
        ),
      ),

      // Basket drawer
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

      // Backdrop overlay
      div(
        cls <-- AppRouter.basketOpen.signal.map(o => if o then "basket-overlay visible" else "basket-overlay"),
        onClick --> { _ => AppRouter.basketOpen.set(false) },
      ),
    )

  /** Customer selector dropdown for employees to pick customer for this order. */
  private def customerSelector(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val customers = ProductBuilderViewModel.allCustomers
      .filter(_.status == CustomerStatus.Active)

    val selectedCustomerId = ProductBuilderViewModel.currentCustomer.map(_.map(_.id.value).getOrElse(""))

    div(
      cls := "compact-customer-selector",
      div(
        cls := "compact-row",
        label(
          cls := "compact-label",
          child.text <-- lang.map {
            case Language.En => "Customer:"
            case Language.Cs => "Zákazník:"
          },
        ),
        div(
          cls := "compact-field",
          select(
            children <-- lang.combineWith(selectedCustomerId).map { case (l, sel) =>
              val placeholder = option(
                l match
                  case Language.En => "-- No customer (list price) --"
                  case Language.Cs => "-- Bez zákazníka (ceníková cena) --",
                value := "",
                selected := sel.isEmpty,
              )
              val customerOptions = customers.map { c =>
                val displayName = c.companyInfo.map(_.companyName).getOrElse(
                  s"${c.contactInfo.firstName} ${c.contactInfo.lastName}"
                )
                val tierLabel = c.tier.displayName(l)
                option(
                  s"$displayName ($tierLabel)",
                  value := c.id.value,
                  selected := (c.id.value == sel),
                )
              }
              placeholder :: customerOptions
            },
            onChange.mapToValue --> { value =>
              if value.nonEmpty then
                ProductBuilderViewModel.selectCustomerDirect(CustomerId.unsafe(value))
              else
                ProductBuilderViewModel.clearCustomerSelection()
            },
          ),
        ),
      ),
      // Show selected customer info inline
      child.maybe <-- ProductBuilderViewModel.currentCustomer.combineWith(lang).map { case (custOpt, l) =>
        custOpt.map { c =>
          val discount = c.pricing.globalDiscount.map(d => s" · ${d.value}%").getOrElse("")
          div(
            cls := "compact-customer-info",
            span(
              l match
                case Language.En => s"${c.tier.displayName(l)} tier$discount"
                case Language.Cs => s"Úroveň: ${c.tier.displayName(l)}$discount"
            ),
          )
        }
      },
    )

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
