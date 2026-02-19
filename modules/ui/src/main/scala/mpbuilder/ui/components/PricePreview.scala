package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.domain.model.Language

object PricePreview:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Price Preview"
        case Language.Cs => "Náhled ceny"
      }),
      
      // Price display
      div(
        cls := "price-display",
        div(cls := "label", child.text <-- lang.map {
          case Language.En => "Total Price"
          case Language.Cs => "Celková cena"
        }),
        div(
          cls := "amount",
          child.text <-- ProductBuilderViewModel.state.map { state =>
            state.priceBreakdown match
              case Some(breakdown) => formatMoney(breakdown.total, breakdown.currency)
              case None => "0,00 Kč"
          },
        ),
      ),
      
      // Price breakdown
      div(
        cls := "price-breakdown",
        children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          state.priceBreakdown match
            case Some(breakdown) =>
              val cur = breakdown.currency
              List(
                h3(l match
                  case Language.En => "Breakdown:"
                  case Language.Cs => "Rozpis:"
                ),
                div(
                  cls := "price-line-item",
                  span(breakdown.materialLine.label),
                  span(formatMoney(breakdown.materialLine.lineTotal, cur)),
                ),
              ) ++ breakdown.finishLines.map { finishLine =>
                div(
                  cls := "price-line-item",
                  span(finishLine.label),
                  span(formatMoney(finishLine.lineTotal, cur)),
                )
              } ++ (
                breakdown.processSurcharge.map { proc =>
                  div(
                    cls := "price-line-item",
                    span(proc.label),
                    span(formatMoney(proc.lineTotal, cur)),
                  )
                }.toList
              ) ++ (
                breakdown.categorySurcharge.map { cat =>
                  div(
                    cls := "price-line-item",
                    span(cat.label),
                    span(formatMoney(cat.lineTotal, cur)),
                  )
                }.toList
              ) ++ List(
                div(
                  cls := "price-line-item",
                  span(l match
                    case Language.En => "Subtotal:"
                    case Language.Cs => "Mezisoučet:"
                  ),
                  span(formatMoney(breakdown.subtotal, cur)),
                ),
                div(
                  cls := "price-line-item",
                  span(l match
                    case Language.En => s"Quantity Multiplier (${breakdown.quantityMultiplier}×):"
                    case Language.Cs => s"Množstevní koeficient (${breakdown.quantityMultiplier}×):"
                  ),
                  span(if breakdown.quantityMultiplier < 1.0 then s"-${formatMoney(Money(breakdown.subtotal.value * (1.0 - breakdown.quantityMultiplier)), cur)}" else formatMoney(Money.zero, cur)),
                ),
                div(
                  cls := "price-line-item",
                  strong(l match
                    case Language.En => "Total:"
                    case Language.Cs => "Celkem:"
                  ),
                  strong(formatMoney(breakdown.total, cur)),
                ),
              )
            case None =>
              List(
                p(l match
                  case Language.En => "Configure your product to see the pricing breakdown."
                  case Language.Cs => "Nakonfigurujte svůj produkt pro zobrazení rozpisu ceny."
                ),
              )
        },
      ),
    )
  
  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
