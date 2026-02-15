package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.pricing.Money

object PricePreview:
  def apply(): Element =
    div(
      cls := "card",
      h2("Price Preview"),
      
      // Price display
      div(
        cls := "price-display",
        div(cls := "label", "Total Price"),
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
        children <-- ProductBuilderViewModel.state.map { state =>
          state.priceBreakdown match
            case Some(breakdown) =>
              List(
                h3("Breakdown:"),
              ) ++ breakdown.lineItems.map { lineItem =>
                div(
                  cls := "price-line-item",
                  span(lineItem.description),
                  span(formatMoney(lineItem.amount)),
                )
              } ++ List(
                div(
                  cls := "price-line-item",
                  span("Subtotal:"),
                  span(formatMoney(breakdown.subtotal)),
                ),
                div(
                  cls := "price-line-item",
                  span(s"Quantity Multiplier (${breakdown.quantityMultiplier}×):"),
                  span(if breakdown.quantityMultiplier < 1.0 then s"-${formatMoney(Money.unsafe(breakdown.subtotal.value * (1.0 - breakdown.quantityMultiplier)))}" else "$0.00"),
                ),
                div(
                  cls := "price-line-item",
                  strong("Total:"),
                  strong(formatMoney(breakdown.total)),
                ),
              )
            case None =>
              List(
                p("Configure your product and click 'Calculate Price' to see the pricing breakdown."),
              )
        },
      ),
    )
  
  private def formatMoney(money: Money): String =
    f"$$${money.value}%.2f"
