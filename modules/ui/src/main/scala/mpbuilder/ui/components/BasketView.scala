package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.domain.model.{Language, ConfigurationId}

object BasketView:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val basketCalc = ProductBuilderViewModel.basketCalculation

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Shopping Basket"
        case Language.Cs => "Nákupní košík"
      }),

      // Basket message (success or error)
      div(
        child.maybe <-- ProductBuilderViewModel.state.map { state =>
          state.basketMessage.map { msg =>
            div(
              cls := "info-box",
              msg,
              button(
                cls := "close-msg-btn",
                "×",
                onClick --> { _ =>
                  ProductBuilderViewModel.clearBasketMessage()
                },
              ),
            )
          }
        },
      ),

      // Basket items
      div(
        cls := "basket-items",
        children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.basket.items.isEmpty then
            List(
              p(
                cls := "empty-basket",
                l match
                  case Language.En => "Your basket is empty. Configure a product and click 'Add to Basket'."
                  case Language.Cs => "Váš košík je prázdný. Nakonfigurujte produkt a klikněte na 'Přidat do košíku'."
              ),
            )
          else
            state.basket.items.map { item =>
              div(
                cls := "basket-item",
                div(
                  cls := "basket-item-header",
                  div(
                    cls := "basket-item-title",
                    strong(item.configuration.category.name(l)),
                    span(cls := "basket-item-material", s" • ${item.configuration.material.name(l)}"),
                  ),
                  button(
                    cls := "remove-btn",
                    "×",
                    title := (l match
                      case Language.En => "Remove from basket"
                      case Language.Cs => "Odebrat z košíku"
                    ),
                    onClick --> { _ =>
                      ProductBuilderViewModel.removeFromBasket(item.configuration.id)
                    },
                  ),
                ),
                div(
                  cls := "basket-item-details",
                  div(
                    cls := "basket-item-specs",
                    span(l match
                      case Language.En => s"Method: ${item.configuration.printingMethod.name(l)}"
                      case Language.Cs => s"Metoda: ${item.configuration.printingMethod.name(l)}"
                    ),
                    if item.configuration.finishes.nonEmpty then
                      span(
                        cls := "basket-item-finishes",
                        l match
                          case Language.En => s" | Finishes: ${item.configuration.finishes.map(_.name(l)).mkString(", ")}"
                          case Language.Cs => s" | Povrchové úpravy: ${item.configuration.finishes.map(_.name(l)).mkString(", ")}"
                      )
                    else emptyNode,
                  ),
                  div(
                    cls := "basket-item-quantity",
                    label(l match
                      case Language.En => "Quantity: "
                      case Language.Cs => "Množství: "
                    ),
                    input(
                      typ := "number",
                      minAttr := "1",
                      value := item.quantity.toString,
                      cls := "quantity-input",
                      onInput.mapToValue.map(_.toIntOption.getOrElse(1)) --> { newQty =>
                        ProductBuilderViewModel.updateBasketQuantity(item.configuration.id, newQty)
                      },
                    ),
                  ),
                  div(
                    cls := "basket-item-price",
                    span(cls := "unit-price", l match
                      case Language.En => s"Unit: ${formatMoney(item.priceBreakdown.total, item.priceBreakdown.currency)}"
                      case Language.Cs => s"Jednotka: ${formatMoney(item.priceBreakdown.total, item.priceBreakdown.currency)}"
                    ),
                    span(cls := "line-total", l match
                      case Language.En => s"Total: ${formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)}"
                      case Language.Cs => s"Celkem: ${formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)}"
                    ),
                  ),
                ),
              )
            }
        },
      ),

      // Basket total
      div(
        cls := "basket-total",
        child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.basket.items.nonEmpty then
            Some(
              div(
                cls := "basket-total-row",
                strong(l match
                  case Language.En => "Basket Total:"
                  case Language.Cs => "Celkem v košíku:"
                ),
                strong(child.text <-- basketCalc.map(calc => formatMoney(calc.total, calc.currency))),
              )
            )
          else
            None
        },
      ),

      // Basket actions
      div(
        cls := "basket-actions",
        child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.basket.items.nonEmpty then
            Some(
              button(
                cls := "clear-basket-btn",
                l match
                  case Language.En => "Clear Basket"
                  case Language.Cs => "Vyprázdnit košík"
                ,
                onClick --> { _ =>
                  ProductBuilderViewModel.clearBasket()
                },
              )
            )
          else
            None
        },
      ),
    )

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
