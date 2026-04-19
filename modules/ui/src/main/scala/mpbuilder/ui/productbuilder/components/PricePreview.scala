package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.pricing.{Money, Currency, ComponentBreakdown, PriceBreakdown}
import mpbuilder.domain.model.{Language, ComponentRole}
import mpbuilder.domain.weight.WeightBreakdown

object PricePreview:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val breakdownOpen = Var(true)

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Price Preview"
        case Language.Cs => "Náhled ceny"
      }),

      // Price display
      div(
        cls := "price-display-stack",
        div(
          cls := "price-display",
          div(cls := "label", child.text <-- lang.map {
            case Language.En => "Total Price"
            case Language.Cs => "Celková cena"
          }),
          // When customer logged in: show base price (strikethrough), customer price, and savings
          child <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
            (state.priceBreakdown, state.basePriceBreakdown) match
              case (Some(breakdown), Some(baseBreakdown)) =>
                // Customer is logged in — show dual pricing
                val savings = Money(baseBreakdown.total.value - breakdown.total.value)
                val savingsPct = if baseBreakdown.total.value > BigDecimal(0) then
                  (savings.value * 100 / baseBreakdown.total.value).setScale(1, BigDecimal.RoundingMode.HALF_UP)
                else BigDecimal(0)
                div(
                  div(cls := "price-base-strikethrough",
                    span(formatMoney(baseBreakdown.total, baseBreakdown.currency)),
                  ),
                  div(cls := "amount price-customer-price",
                    l match
                      case Language.En => s"Your price: ${formatMoney(breakdown.total, breakdown.currency)}"
                      case Language.Cs => s"Vaše cena: ${formatMoney(breakdown.total, breakdown.currency)}",
                  ),
                  if savings.value > BigDecimal(0) then
                    div(cls := "price-savings",
                      l match
                        case Language.En => s"You save: ${formatMoney(savings, breakdown.currency)} ($savingsPct%)"
                        case Language.Cs => s"Ušetříte: ${formatMoney(savings, breakdown.currency)} ($savingsPct%)",
                    )
                  else emptyNode,
                  pricePerItemLine(breakdown, l).getOrElse(emptyNode),
                )
              case (Some(breakdown), None) =>
                // No customer logged in — standard pricing
                div(
                  div(cls := "amount", formatMoney(breakdown.total, breakdown.currency)),
                  pricePerItemLine(breakdown, l).getOrElse(emptyNode),
                )
              case _ =>
                div(cls := "amount", "0,00 Kč")
          },
        ),
        button(
          cls := "price-preview-action-btn",
          aria.expanded <-- breakdownOpen.signal,
          aria.controls := "price-breakdown-section",
          child.text <-- lang.map {
            case Language.En => "Validate price"
            case Language.Cs => "Ověřit cenu"
          },
          onClick --> { _ =>
            ProductBuilderViewModel.validateConfiguration()
            breakdownOpen.update(!_)
          },
        ),
        div(
          cls <-- breakdownOpen.signal.map(open =>
            if open then "price-preview-action-arrows open" else "price-preview-action-arrows closed"
          ),
          aria.hidden := true,
        ),
      ),

      // Weight display (shown when weight can be calculated)
      child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        state.weightBreakdown.map { wb =>
          div(
            cls := "weight-display",
            div(cls := "label", if l == Language.Cs then "Hmotnost" else "Weight"),
            div(
              cls := "price-line-item",
              span(if l == Language.Cs then "Na kus:" else "Per item:"),
              span(formatWeight(wb.weightPerItemG)),
            ),
            div(
              cls := "price-line-item",
              span(if l == Language.Cs then "Celkem:" else "Total:"),
              span(formatWeight(wb.totalWeightG)),
            ),
          )
        }
      },

      // Price breakdown
      div(
        idAttr := "price-breakdown-section",
        cls <-- breakdownOpen.signal.map(open =>
          if open then "price-breakdown" else "price-breakdown price-breakdown-collapsed"
        ),
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

              val surchargeLines =
                List(breakdown.processSurcharge, breakdown.categorySurcharge, breakdown.foldSurcharge, breakdown.bindingSurcharge)
                  .flatten
                  .map { item =>
                    div(
                      cls := "price-line-item",
                      span(item.label),
                      span(formatMoney(item.lineTotal, cur)),
                    )
                  }

              val totalSheets = breakdown.componentBreakdowns.map(_.sheetsUsed).sum
              val isSheetTier = totalSheets > 0

              val subtotalAndMultiplierLines = List(
                hr(cls := "price-subtotal-divider"),
                div(
                  cls := "price-line-item price-subtotal",
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
              )

              val speedSurchargeLine = breakdown.speedSurcharge.map { item =>
                div(
                  cls := "price-line-item",
                  span(item.label),
                  span(formatMoney(item.lineTotal, cur)),
                )
              }.toList

              val setupFeeLines = breakdown.setupFees.map { fee =>
                div(
                  cls := "price-line-item",
                  span(fee.label),
                  span(formatMoney(fee.lineTotal, cur)),
                )
              }

              val minimumLine = breakdown.minimumApplied.map { originalBillable =>
                div(
                  cls := "price-line-item price-minimum-indicator",
                  span(l match
                    case Language.En => s"Minimum order applied (was ${formatMoney(originalBillable, cur)}):"
                    case Language.Cs => s"Použito minimální ceny (původně ${formatMoney(originalBillable, cur)}):"
                  ),
                  span(""),
                )
              }.toList

              val totalLine = List(
                div(
                  cls := "price-line-item",
                  strong(l match
                    case Language.En => "Total:"
                    case Language.Cs => "Celkem:"
                  ),
                  strong(formatMoney(breakdown.total, cur)),
                ),
              )

              val totalLines = subtotalAndMultiplierLines ++ speedSurchargeLine ++ setupFeeLines ++ minimumLine ++ totalLine

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

  private def formatWeight(grams: Double): String =
    if grams >= 1000.0 then f"${grams / 1000.0}%.3f kg"
    else f"${grams}%.2f g"

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"

  private def pricePerItemLine(breakdown: PriceBreakdown, lang: Language): Option[Element] =
    if breakdown.quantity > 1 then
      val perItemPrice = formatMoney((breakdown.total / breakdown.quantity).rounded, breakdown.currency)
      Some(div(
        cls := "price-per-item-main",
        lang match
          case Language.En => s"Price per item (${breakdown.quantity} pcs): $perItemPrice"
          case Language.Cs => s"Cena za kus (${breakdown.quantity} ks): $perItemPrice",
      ))
    else None

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
      case ComponentRole.Stand => lang match
        case Language.En => "Stand"
        case Language.Cs => "Stojánek"
      case ComponentRole.FrontCover => lang match
        case Language.En => "Front Cover"
        case Language.Cs => "Přední deska"
      case ComponentRole.BackCover => lang match
        case Language.En => "Back Cover"
        case Language.Cs => "Zadní deska"
      case ComponentRole.Binding => lang match
        case Language.En => "Binding"
        case Language.Cs => "Vazba"
      case ComponentRole.HangingStrip => lang match
        case Language.En => "Hanging Strip"
        case Language.Cs => "Závěsný proužek"
      case ComponentRole.CaseBoard => lang match
        case Language.En => "Case Board"
        case Language.Cs => "Tvrdá deska"
      case ComponentRole.Endpaper => lang match
        case Language.En => "Endpaper"
        case Language.Cs => "Předsádka"
      case ComponentRole.Packaging => lang match
        case Language.En => "Packaging"
        case Language.Cs => "Balení"
