package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

enum PriceMatrixStatus:
  case Loading
  case Ready(matrix: PriceMatrix)
  case Failed(error: StorefrontError)

/** Interactive Quantity × ManufacturingSpeed price tier table shared by the Configurator,
  * Calculator Widget, and Product Detail page.
  *
  * Clicking any available `(quantity, speed)` cell immediately applies that run size and speed.
  */
object PriceMatrixView:

  def render(
      status: PriceMatrixStatus,
      selectedQuantity: Quantity,
      selectedSpeed: ManufacturingSpeed,
      i18n: AppI18n,
      lang: Language,
      subtitle: Signal[String],
      onSelect: (Quantity, ManufacturingSpeed) => Unit < Async,
      speedOptions: List[SpeedTierOption] = Nil,
  ): UI =
    status match
      case PriceMatrixStatus.Failed(_) => UI.empty
      case PriceMatrixStatus.Ready(m) if m.rows.isEmpty => UI.empty
      case _ =>
        div.cssClass("mp-card mp-price-matrix")(
          div.style(rowBetween.flexWrap(FlexWrap.wrap).align(Alignment.end))(
            div.style(stackXs)(
              div.style(rowSm)(
                span(Icons(Icon.Percent, 16)).style(Style.color(brand).row.align(Alignment.center)),
                h2(i18n.t("matrix-title")).style(sectionTitle.margin(0.px)),
              ),
              p(subtitle).style(smallText.margin(0.px)),
            ),
            span(i18n.t("matrix-unit-note")).style(smallText),
          ),
          status match
            case PriceMatrixStatus.Loading =>
              div.style(rowSm.padding(12.px, 0.px).color(inkMuted))(
                Icons(Icon.Clock, 15),
                span(i18n.t("matrix-loading")).style(mutedText),
              )
            case PriceMatrixStatus.Failed(_) =>
              UI.empty
            case PriceMatrixStatus.Ready(matrix) =>
              renderTable(matrix, selectedQuantity, selectedSpeed, i18n, lang, onSelect, speedOptions),
        )

  private def renderTable(
      matrix: PriceMatrix,
      selectedQuantity: Quantity,
      selectedSpeed: ManufacturingSpeed,
      i18n: AppI18n,
      lang: Language,
      onSelect: (Quantity, ManufacturingSpeed) => Unit < Async,
      speedOptions: List[SpeedTierOption],
  ): UI =
    val isCs      = lang == Language.Cs
    val pcsSuffix = if isCs then "ks" else "pcs"
    val pcSuffix  = if isCs then "ks" else "pc"
    val optBySpeed = speedOptions.map(o => o.speed -> o).toMap

    div.cssClass("mp-matrix-scroll")(
      div.cssClass("mp-matrix-grid mp-num").role("grid").aria("label", if isCs then "Cenová matice" else "Price matrix")(
        // Column headers
        div.cssClass("mp-matrix-header").role("row")(
          div.cssClass("mp-matrix-qty-col").role("columnheader")(
            span(i18n.t("matrix-col-qty")).style(labelStyle),
          ),
          UI.fragment(
            matrix.speeds.map { speed =>
              val pctBadge = optBySpeed.get(speed).map(_.surchargePercent.setScale(0, BigDecimal.RoundingMode.HALF_UP)) match
                case Some(pct) if pct < 0 => badge(s"$pct%", BadgeTone.Emerald)
                case Some(pct) if pct > 0 => badge(s"+$pct%", BadgeTone.Amber)
                case _                    => UI.empty
              div.cssClass("mp-matrix-speed-col").role("columnheader")(
                div.style(rowXs.justify(Justification.center).flexWrap(FlexWrap.wrap))(
                  span(speedLabel(speed, lang)).style(labelStyle),
                  pctBadge,
                ),
              )
            }*
          ),
        ),
        // Quantity rows
        UI.fragment(
          matrix.rows.map { row =>
            val isSelectedRow = row.quantity == selectedQuantity
            val rowCls        = if isSelectedRow then "mp-matrix-row mp-matrix-row-active" else "mp-matrix-row"
            val cellBySpeed   = row.cells.map(c => c.speed -> c).toMap

            div.cssClass(rowCls).role("row")(
              div.cssClass("mp-matrix-qty-col").role("rowheader")(
                span(s"${formatQuantity(row.quantity.value, lang)} $pcsSuffix")
                  .style(Style.fontSize(14.px).fontWeight(FontWeight.w700).color(if isSelectedRow then brandDark else ink)),
                row.savingsVsSmallestPercent match
                  case Some(pct) if pct > BigDecimal(0) =>
                    reactiveBadge(i18n.t("matrix-save-badge", Dict("pct" -> pct.toString)), BadgeTone.Emerald)
                  case _ =>
                    UI.empty,
              ),
              UI.fragment(
                matrix.speeds.map { speed =>
                  div.cssClass("mp-matrix-speed-col").role("gridcell")(
                    cellBySpeed.get(speed) match
                      case Some(cell) if cell.available =>
                        val isActive = isSelectedRow && cell.speed == selectedSpeed
                        val btnCls   = if isActive then "mp-matrix-cell-btn mp-matrix-cell-active" else "mp-matrix-cell-btn"
                        val unitCls  = if isActive then "mp-matrix-unit mp-matrix-unit-active" else "mp-matrix-unit"
                        button
                          .cssClass(btnCls)
                          .aria("pressed", isActive.toString)
                          .onClick(onSelect(row.quantity, cell.speed))(
                            div.style(rowXs.justify(Justification.center))(
                              if isActive then span(Icons(Icon.Check, 13, 2.5)).style(Style.color(brand).row.align(Alignment.center))
                              else UI.empty,
                              span(s"${formatMoney(cell.unitNet, matrix.currency, lang)}/$pcSuffix").cssClass(unitCls),
                            ),
                            span(formatMoney(cell.grossTotal, matrix.currency, lang)).cssClass("mp-matrix-total"),
                          )
                      case Some(cell) =>
                        val baseBtn = button
                          .cssClass("mp-matrix-cell-btn mp-matrix-cell-disabled")
                          .disabled(true)
                        val titled = cell.unavailableReason.fold(baseBtn)(r => baseBtn.data("tooltip", r))
                        titled(
                          span("—").style(Style.fontSize(13.px).fontWeight(FontWeight.w700)),
                          span(i18n.t("matrix-unavailable")).cssClass("mp-matrix-total"),
                        )
                      case None =>
                        button.cssClass("mp-matrix-cell-btn mp-matrix-cell-disabled").disabled(true)(span("—"))
                  )
                }*
              ),
            )
          }*
        ),
      ),
    )
