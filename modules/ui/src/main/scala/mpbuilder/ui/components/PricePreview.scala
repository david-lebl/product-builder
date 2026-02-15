package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.pricing.Money
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
              case Some(breakdown) => formatMoney(breakdown.total)
              case None => "$0.00"
          },
        ),
      ),
      
      // Price breakdown
      div(
        cls := "price-breakdown",
        children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          state.priceBreakdown match
            case Some(breakdown) =>
              List(
                h3(l match
                  case Language.En => "Breakdown:"
                  case Language.Cs => "Rozpis:"
                ),
                div(
                  cls := "price-line-item",
                  span(breakdown.materialLine.label),
                  span(formatMoney(breakdown.materialLine.lineTotal)),
                ),
              ) ++ breakdown.finishLines.map { finishLine =>
                div(
                  cls := "price-line-item",
                  span(finishLine.label),
                  span(formatMoney(finishLine.lineTotal)),
                )
              } ++ (
                breakdown.processSurcharge.map { proc =>
                  div(
                    cls := "price-line-item",
                    span(proc.label),
                    span(formatMoney(proc.lineTotal)),
                  )
                }.toList
              ) ++ (
                breakdown.categorySurcharge.map { cat =>
                  div(
                    cls := "price-line-item",
                    span(cat.label),
                    span(formatMoney(cat.lineTotal)),
                  )
                }.toList
              ) ++ List(
                div(
                  cls := "price-line-item",
                  span(l match
                    case Language.En => "Subtotal:"
                    case Language.Cs => "Mezisoučet:"
                  ),
                  span(formatMoney(breakdown.subtotal)),
                ),
                div(
                  cls := "price-line-item",
                  span(l match
                    case Language.En => s"Quantity Multiplier (${breakdown.quantityMultiplier}×):"
                    case Language.Cs => s"Množstevní koeficient (${breakdown.quantityMultiplier}×):"
                  ),
                  span(if breakdown.quantityMultiplier < 1.0 then s"-${formatMoney(Money(breakdown.subtotal.value * (1.0 - breakdown.quantityMultiplier)))}" else "$0.00"),
                ),
                div(
                  cls := "price-line-item",
                  strong(l match
                    case Language.En => "Total:"
                    case Language.Cs => "Celkem:"
                  ),
                  strong(formatMoney(breakdown.total)),
                ),
              )
            case None =>
              List(
                p(l match
                  case Language.En => "Configure your product and click 'Calculate Price' to see the pricing breakdown."
                  case Language.Cs => "Nakonfigurujte svůj produkt a klikněte na 'Vypočítat cenu' pro zobrazení rozpisu."
                ),
              )
        },
      ),
    )
  
  private def formatMoney(money: Money): String =
    f"$$${money.value}%.2f"
