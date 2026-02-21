package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.pricing.{Money, Currency, ComponentBreakdown}
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
              val headerLine = List(
                h3(l match
                  case Language.En => "Breakdown:"
                  case Language.Cs => "Rozpis:"
                ),
              )

              val componentLines = breakdown.componentBreakdowns.flatMap { cb =>
                val roleLabel = componentRoleLabel(cb.role, l)
                val showRoleHeader = breakdown.componentBreakdowns.size > 1

                val roleHeader =
                  if showRoleHeader then
                    List(h4(cls := "component-role-header", roleLabel))
                  else
                    List.empty

                val materialLine = List(
                  div(
                    cls := "price-line-item",
                    span(cb.materialLine.label),
                    span(formatMoney(cb.materialLine.lineTotal, cur)),
                  ),
                )

                val sheetsLine =
                  if cb.sheetsUsed > 0 then
                    List(div(
                      cls := "price-line-item",
                      span(l match
                        case Language.En => s"Sheets used: ${cb.sheetsUsed}"
                        case Language.Cs => s"Použité archy: ${cb.sheetsUsed}"
                      ),
                      span(""),
                    ))
                  else List.empty

                val cuttingLine = cb.cuttingLine.map { cutting =>
                  div(
                    cls := "price-line-item",
                    span(cutting.label),
                    span(formatMoney(cutting.lineTotal, cur)),
                  )
                }.toList

                val inkLine = cb.inkConfigLine.map { ink =>
                  div(
                    cls := "price-line-item",
                    span(ink.label),
                    span(formatMoney(ink.lineTotal, cur)),
                  )
                }.toList

                val finishLines = cb.finishLines.map { finishLine =>
                  div(
                    cls := "price-line-item",
                    span(finishLine.label),
                    span(formatMoney(finishLine.lineTotal, cur)),
                  )
                }

                roleHeader ++ materialLine ++ sheetsLine ++ cuttingLine ++ inkLine ++ finishLines
              }

              val surchargeLines = breakdown.processSurcharge.map { proc =>
                div(
                  cls := "price-line-item",
                  span(proc.label),
                  span(formatMoney(proc.lineTotal, cur)),
                )
              }.toList ++ breakdown.categorySurcharge.map { cat =>
                div(
                  cls := "price-line-item",
                  span(cat.label),
                  span(formatMoney(cat.lineTotal, cur)),
                )
              }.toList

              val totalSheets = breakdown.componentBreakdowns.map(_.sheetsUsed).sum
              val isSheetTier = totalSheets > 0

              val totalLines = List(
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
                  span(
                    if isSheetTier then
                      l match
                        case Language.En => s"Sheet Tier ($totalSheets sheets, ${breakdown.quantityMultiplier}\u00d7):"
                        case Language.Cs => s"Archová sleva ($totalSheets archů, ${breakdown.quantityMultiplier}\u00d7):"
                    else
                      l match
                        case Language.En => s"Quantity Multiplier (${breakdown.quantityMultiplier}\u00d7):"
                        case Language.Cs => s"Množstevní koeficient (${breakdown.quantityMultiplier}\u00d7):"
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

              headerLine ++ componentLines ++ surchargeLines ++ totalLines

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

  private def componentRoleLabel(role: ComponentRole, lang: Language): String =
    role match
      case ComponentRole.Main => lang match
        case Language.En => "Main"
        case Language.Cs => "Hlavní"
      case ComponentRole.Cover => lang match
        case Language.En => "Cover"
        case Language.Cs => "Obálka"
      case ComponentRole.Body => lang match
        case Language.En => "Body"
        case Language.Cs => "Vnitřní část"
