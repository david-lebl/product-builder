package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Slide-over Shopping Cart Drawer with copy steppers, promo code input + quick demo chips, and live totals. */
object CartDrawer:

  private val demoPromoCodes: List[String] = List("SAVE10", "FREESHIP", "FLAT100", "VIP25", "ONEUSE")

  def render(state: StorefrontState): UI < Async =
    for promoInput <- Signal.initRef("")
    yield
      val contentSignal =
        state.cart
          .combineLatest(state.cartError)
          .combineLatest(state.i18n.language)

      UI.when(state.cartDrawerOpen)(
        div.cssClass("mp-backdrop mp-cart-backdrop").onClickSelf(state.cartDrawerOpen.set(false))(
          div.cssClass("mp-drawer").role("dialog").aria("modal", "true")(
            // Drawer Header
            div.style(
              rowBetween
                .padding(18.px, 20.px)
                .borderBottom(1.px, Palette.border)
                .bg(surface),
            )(
              div.style(rowSm)(
                span.style(Style.row.color(brand))(Icons(Icons.Icon.Cart, 20)),
                h2(state.i18n.t("cart-title")).style(sectionTitle.margin(0.px)),
              ),
              button
                .cssClass("mp-icon-btn mp-icon-btn-sm")
                .aria("label", "Close")
                .onClick(state.cartDrawerOpen.set(false))(Icons(Icons.Icon.Close, 17)),
            ),

            // Drawer Body
            div.style(Style.column.gap(16.px).padding(18.px, 20.px).flexGrow(1.0))(
              contentSignal.render { case ((cartOpt, errOpt), lang) =>
                cartOpt match
                  case Some(cart) if cart.lines.nonEmpty =>
                    renderNonEmptyCart(state, cart, errOpt, promoInput, lang)
                  case _ =>
                    div.style(Style.column.align(Alignment.center).textAlign(TextAlign.center).gap(10.px).padding(40.px, 12.px))(
                      h3(state.i18n.t("cart-empty-title")).style(Style.margin(0.px).fontSize(17.px).color(ink)),
                      p(state.i18n.t("cart-empty-desc")).style(mutedText.margin(0.px)),
                      button
                        .cssClass("mp-btn mp-btn-primary")
                        .onClick(state.cartDrawerOpen.set(false).andThen(state.openCatalog))(
                          state.i18n.t("cart-browse-cta"),
                        ),
                    )
              },
            ),
          ),
        ),
      )

  private def renderNonEmptyCart(
      state: StorefrontState,
      cart: CartView,
      errOpt: Option[String],
      promoInput: SignalRef[String],
      lang: Language,
  ): UI =
    val cur = cart.currency

    div.style(Style.column.gap(16.px))(
      // Cart Lines
      div.style(Style.column.gap(10.px))(
        cart.lines.map { line =>
          val firstComp = line.product.components.head
          div.style(Style.column.gap(8.px).padding(14.px).rounded(12.px).border(1.px, Palette.border).bg(surfaceAlt))(
            div.style(rowBetween)(
              div.style(Style.column.gap(2.px))(
                div.style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink))(line.product.categoryName(lang)),
                div.style(smallText)(
                  s"${firstComp.materialName(lang)} · ${formatDimension(firstComp.finishedSize)} · ${line.runQuantity.value} pcs",
                ),
              ),
              button
                .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                .style(Style.color(roseInk).padding(4.px, 8.px))
                .onClick(state.removeCartLine(line.lineId))(state.i18n.t("cart-remove")),
            ),
            div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
              badge(speedLabel(line.speed, lang), BadgeTone.Brand),
              firstComp.inkNotation.map(ink => badge(ink, BadgeTone.Neutral)).getOrElse(UI.empty),
              line.totalWeightGrams.map(w => badge(formatWeight(w), BadgeTone.Neutral)).getOrElse(UI.empty),
              if line.artworkIds.nonEmpty then
                reactiveBadge(
                  state.i18n.t("cfg-artwork-attached-badge", Dict("count" -> line.artworkIds.size.toString)),
                  BadgeTone.Emerald,
                )
              else
                reactiveBadge(state.i18n.t("cfg-artwork-none-badge"), BadgeTone.Neutral),
            ),
            div.style(rowBetween)(
              div.style(Style.row.align(Alignment.center).gap(6.px))(
                span(state.i18n.t("cart-copies")).style(smallText),
                button
                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                  .style(Style.padding(2.px, 9.px))
                  .onClick(state.updateCartLineCopies(line.lineId, line.copies.value - 1))("−"),
                span(line.copies.value.toString).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).minWidth(20.px).textAlign(TextAlign.center)),
                button
                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                  .style(Style.padding(2.px, 9.px))
                  .onClick(state.updateCartLineCopies(line.lineId, line.copies.value + 1))("+"),
              ),
              span(formatMoney(line.lineGrossTotal, cur, lang)).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
            ),
          )
        }*
      ),

      // Promo Code Section
      div.style(Style.column.gap(8.px).padding(14.px).rounded(12.px).border(1.px, Palette.border))(
        label.style(labelStyle)(state.i18n.t("cart-promo-label")),
        cart.appliedPromo match
          case Some(promo) =>
            div.style(rowBetween.padding(8.px, 10.px).rounded(8.px).bg(emeraldSoft).border(1.px, emeraldBorder))(
              div.style(Style.row.gap(6.px).align(Alignment.center))(
                badge(promo.code.value, BadgeTone.Emerald),
                span(
                  if promo.freeDelivery then (if lang == Language.Cs then "Doprava zdarma" else "Free Shipping")
                  else s"−${formatMoney(promo.discountNet, cur, lang)}",
                ).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(emeraldInk)),
              ),
              button
                .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                .onClick(state.removeCartPromo)(state.i18n.t("cart-promo-remove")),
            )
          case None =>
            div.style(Style.column.gap(8.px))(
              div.style(Style.row.gap(8.px))(
                input
                  .cssClass("mp-input")
                  .style(Style.flexGrow(1.0))
                  .value(promoInput)
                  .placeholder("SAVE10, FREESHIP..."),
                button
                  .cssClass("mp-btn mp-btn-secondary")
                  .onClick(promoInput.get.flatMap(state.applyCartPromo))(state.i18n.t("cart-promo-apply")),
              ),
              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px).align(Alignment.center))(
                span(state.i18n.t("cart-promo-try")).style(smallText),
                UI.fragment(
                  demoPromoCodes.map { code =>
                    button
                      .cssClass("mp-pill")
                      .style(Style.padding(3.px, 9.px).fontSize(11.px))
                      .onClick(promoInput.set(code).andThen(state.applyCartPromo(code)))(code)
                  }*
                ),
              ),
            ),
        errOpt
          .map(msg => div.style(Style.fontSize(12.px).color(roseInk).fontWeight(FontWeight.w600))(msg))
          .getOrElse(UI.empty),
      ),

      // Totals Summary
      div.style(Style.column.gap(6.px).padding(14.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
        div.style(rowBetween.fontSize(13.px))(
          span(state.i18n.t("cart-subtotal-net")).style(Style.color(inkSecondary)),
          span(formatMoney(cart.linesNetTotal, cur, lang)).style(Style.fontWeight(FontWeight.w600)),
        ),
        if cart.promoDiscountNet.value > BigDecimal(0) then
          div.style(rowBetween.fontSize(13.px))(
            span(state.i18n.t("cart-promo-discount")).style(Style.color(emeraldInk)),
            span(s"−${formatMoney(cart.promoDiscountNet, cur, lang)}").style(Style.fontWeight(FontWeight.w700).color(emeraldInk)),
          )
        else UI.empty,
        div.style(rowBetween.fontSize(13.px))(
          span(state.i18n.t("cart-tax")).style(Style.color(inkSecondary)),
          span(formatMoney(cart.taxTotal, cur, lang)),
        ),
        div.style(rowBetween.padding(8.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
          span(state.i18n.t("cart-total-gross")).style(Style.fontWeight(FontWeight.w700).fontSize(15.px)),
          span(formatMoney(cart.grossTotal, cur, lang)).style(Style.fontWeight(FontWeight.w800).fontSize(20.px).color(ink)),
        ),
      ),

      // CTAs
      div.style(Style.column.gap(8.px))(
        button
          .cssClass("mp-btn mp-btn-primary mp-btn-lg")
          .onClick(state.startCartCheckout)(state.i18n.t("cart-checkout-cta")),
        button
          .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
          .style(Style.width(100.pct).color(roseInk))
          .onClick(state.clearCart)(state.i18n.t("cart-clear")),
      ),
    )
