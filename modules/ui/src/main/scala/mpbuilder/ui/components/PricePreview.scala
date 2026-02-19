package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.pricing.{Money, ComponentLineItems}
import mpbuilder.domain.model.{Language, ComponentRole}

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
              if breakdown.componentLines.nonEmpty then
                renderMultiComponentBreakdown(breakdown, l)
              else
                renderSingleComponentBreakdown(breakdown, l)
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

  private def renderSingleComponentBreakdown(breakdown: mpbuilder.domain.pricing.PriceBreakdown, l: Language): List[Element] =
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
    ) ++ breakdown.inkConfigLine.map { inkLine =>
      div(
        cls := "price-line-item",
        span(inkLine.label),
        span(formatMoney(inkLine.lineTotal)),
      )
    }.toList ++ breakdown.finishLines.map { finishLine =>
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
    ) ++ renderTotals(breakdown, l)

  private def renderMultiComponentBreakdown(breakdown: mpbuilder.domain.pricing.PriceBreakdown, l: Language): List[Element] =
    List(
      h3(l match
        case Language.En => "Breakdown by Component:"
        case Language.Cs => "Rozpis dle komponent:"
      ),
    ) ++ breakdown.componentLines.flatMap { cl =>
      renderComponentLines(cl, l)
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
    ) ++ renderTotals(breakdown, l)

  private def renderComponentLines(cl: ComponentLineItems, l: Language): List[Element] =
    val roleName = cl.role match
      case ComponentRole.Cover => l match
        case Language.En => "Cover"
        case Language.Cs => "Obálka"
      case ComponentRole.Body => l match
        case Language.En => "Body"
        case Language.Cs => "Vnitřní stránky"

    List(
      h4(
        cls := "component-section-header",
        roleName,
      ),
      div(
        cls := "price-line-item",
        span(cl.materialLine.label),
        span(formatMoney(cl.materialLine.lineTotal)),
      ),
    ) ++ cl.inkConfigLine.map { inkLine =>
      div(
        cls := "price-line-item",
        span(inkLine.label),
        span(formatMoney(inkLine.lineTotal)),
      )
    }.toList ++ cl.finishLines.map { finishLine =>
      div(
        cls := "price-line-item",
        span(finishLine.label),
        span(formatMoney(finishLine.lineTotal)),
      )
    }

  private def renderTotals(breakdown: mpbuilder.domain.pricing.PriceBreakdown, l: Language): List[Element] =
    List(
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
  
  private def formatMoney(money: Money): String =
    f"$$${money.value}%.2f"
