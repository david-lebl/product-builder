package mpbuilder.ui.configurator

import com.raquo.laminar.api.L.*
import mpbuilder.api.ErrorText
import mpbuilder.domain.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.validation.ValidationError
import mpbuilder.ui.Format
import mpbuilder.ui.I18n.t

object PricePanel:

  /** Live itemized price breakdown, or the complete accumulated error list. */
  def view(state: ConfiguratorState): HtmlElement =
    div(
      cls := "price-panel",
      h3("Cena"),
      child <-- state.validationErrors
        .combineWith(state.price, state.serverErrors.signal)
        .map(render(state, _, _, _)),
    )

  private def render(
    state: ConfiguratorState,
    validationErrors: List[ValidationError],
    price: Option[Either[List[PricingError], PriceBreakdown]],
    serverErrors: List[mpbuilder.api.ErrorDto],
  ): HtmlElement =
    val validationDtos = validationErrors.map(ErrorText.dto(state.catalog, _))
    val pricingDtos = price match
      case Some(Left(errors)) => errors.map(ErrorText.dto(state.catalog, _))
      case _                  => Nil
    val allErrors = validationDtos ++ pricingDtos ++ serverErrors

    div(
      when(allErrors.nonEmpty)(errorList(allErrors)),
      price match
        case Some(Right(breakdown)) => breakdownTable(breakdown)
        case _                      => div(cls := "muted", "Cenu spočítáme, jakmile bude konfigurace úplná."),
    )

  private def errorList(errors: List[mpbuilder.api.ErrorDto]): HtmlElement =
    div(
      cls := "error-panel",
      h4(s"Konfigurace obsahuje problémy (${errors.size})"),
      ul(errors.map(e => li(t(e.message)))),
    )

  private def breakdownTable(b: PriceBreakdown): HtmlElement =
    div(
      cls := "breakdown",
      b.components.map(componentSection(b, _)),
      when(b.orderLines.nonEmpty)(
        div(
          cls := "breakdown-section",
          h4("Příplatky k zakázce"),
          table(b.orderLines.map(lineRow)),
        )
      ),
      table(
        cls := "breakdown-summary",
        tr(td("Mezisoučet"), td(cls := "num", Format.money(b.subtotal))),
        b.volumeDiscount.map { d =>
          val basis = d.basis match
            case DiscountBasis.Sheets   => s"${d.basisCount} archů"
            case DiscountBasis.Quantity => s"${d.basisCount} ks"
          tr(
            td(s"Množstevní sleva ($basis, ×${d.multiplier})"),
            td(cls := "num", s"−${Format.money(d.amountOff)}"),
          )
        },
        b.speedAdjustment.map { s =>
          tr(
            td(s"Rychlost výroby: ${t(Labels.speed(s.tier))} (×${s.multiplier})"),
            td(cls := "num", (if s.delta.amount >= 0 then "+" else "−") + Format.money(s.delta.copy(amount = s.delta.amount.abs))),
          )
        },
        b.setupFees.map { fee =>
          tr(td(s"Jednorázová příprava: ${t(fee.label)}"), td(cls := "num", s"+${Format.money(fee.fee)}"))
        },
        b.minimumApplied.map { min =>
          tr(cls := "minimum-note", td("Uplatněna minimální cena objednávky"), td(cls := "num", Format.money(min)))
        },
        tr(cls := "total-row", td(strong("Celkem")), td(cls := "num", strong(Format.money(b.total)))),
      ),
    )

  private def componentSection(b: PriceBreakdown, c: ComponentBreakdown): HtmlElement =
    div(
      cls := "breakdown-section",
      h4(
        t(Labels.componentRole(c.role)),
        c.sheetsUsed.map(s => span(cls := "muted", s"  ($s archů, ${c.copiesPerSheet.getOrElse(0)} ks/arch)")),
      ),
      table(
        c.lines.map(lineRow),
        tr(cls := "subtotal-row", td("Mezisoučet části"), td(cls := "num", Format.money(c.subtotal))),
      ),
    )

  private def lineRow(line: PriceLine): HtmlElement =
    tr(
      td(t(line.label), span(cls := "muted", s" — ${Format.money(line.unitPrice)} ${unitCs(line.unitDescription)} × ${quantityLabel(line.billedQuantity)}")),
      td(cls := "num", Format.money(line.total)),
    )

  private def unitCs(unit: String): String = unit match
    case "per sheet" => "za arch"
    case "per m²"    => "za m²"
    case "per unit"  => "za kus"
    case "per cut"   => "za řez"
    case "per metre" => "za metr"
    case other       => other

  private def quantityLabel(q: BigDecimal): String =
    if q.isWhole then q.toBigInt.toString else q.setScale(2, BigDecimal.RoundingMode.HALF_UP).toString
