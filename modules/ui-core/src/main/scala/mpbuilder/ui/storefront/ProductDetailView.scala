package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.configurator.QuoteStatus
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Product Detail page displaying full category specs and live-priced Presets with Add-to-Cart, Quick Order & Customize CTAs. */
object ProductDetailView:

  def render(state: StorefrontState, categoryId: CategoryId): UI =
    val combined =
      state.presetQuotes
        .combineLatest(state.currency)
        .combineLatest(state.i18n.language)

    combined.render { case ((quotesMap, cur), lang) =>
      state.catalog.categories.get(categoryId) match
        case None =>
          div.cssClass("mp-card")(
            alertBanner(BadgeTone.Rose, span("Not Found"), List(s"Category '${categoryId.value}' not found")),
            button.cssClass("mp-btn mp-btn-secondary").onClick(state.openCatalog)(state.i18n.t("detail-back")),
          )
        case Some(cat) =>
          val listingOpt = state.catalog.showcases.get(categoryId)
          div.style(Style.column.gap(22.px).width(100.pct))(
            // Back breadcrumb
            div.style(Style.row.align(Alignment.center).gap(8.px))(
              button
                .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                .onClick(state.openCatalog)("← ", state.i18n.t("detail-back")),
            ),

            // Category Overview Card
            div.cssClass("mp-card")(
              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(16.px).align(Alignment.start).justify(Justification.spaceBetween))(
                div.style(Style.row.gap(14.px).align(Alignment.start).flexGrow(1.0).minWidth(240.px))(
                  listingOpt.map(sc => groupIcon(sc.group)).getOrElse(UI.empty),
                  div.style(Style.column.gap(6.px))(
                    listingOpt
                      .map(sc => badge(catalogGroupLabel(sc.group, lang), BadgeTone.Brand))
                      .getOrElse(UI.empty),
                    h1(cat.name(lang)).style(Style.margin(0.px).fontSize(26.px).fontWeight(FontWeight.w800).color(ink)),
                    listingOpt
                      .map(sc => p(sc.detailedDescription(lang)).style(mutedText.margin(0.px).maxWidth(760.px)))
                      .orElse(cat.description.map(d => p(d(lang)).style(mutedText.margin(0.px))))
                      .getOrElse(UI.empty),
                  ),
                ),
                button
                  .cssClass("mp-btn mp-btn-primary")
                  .onClick(state.openBuilder(Some(cat.id)))(state.i18n.t("detail-custom-cta")),
              ),
              // Component summary pills
              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px).align(Alignment.center).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                span(state.i18n.t("detail-components-label")).style(smallText.fontWeight(FontWeight.w700)),
                UI.fragment(
                  cat.components.toList.map { tpl =>
                    badge(
                      s"${tpl.label(lang)} (${tpl.allowedMaterialIds.size} materials)",
                      BadgeTone.Neutral,
                    )
                  }*
                ),
              ),
            ),

            // Live-Priced Presets Grid
            if cat.presets.nonEmpty then
              div.style(Style.column.gap(14.px))(
                div.style(Style.column.gap(4.px))(
                  h2(state.i18n.t("detail-presets-title")).style(sectionTitle.margin(0.px)),
                  p(state.i18n.t("detail-presets-subtitle")).style(mutedText.margin(0.px)),
                ),
                div.cssClass("mp-card-grid")(
                  cat.presets.map { preset =>
                    val quoteStatus = quotesMap.getOrElse(preset.id, QuoteStatus.Loading)
                    renderPresetCard(state, cat, preset, quoteStatus, cur, lang)
                  }*
                ),
              )
            else UI.empty,
          )
    }

  private def renderPresetCard(
      state: StorefrontState,
      cat: ProductCategory,
      preset: CategoryPreset,
      quoteStatus: QuoteStatus,
      currency: Currency,
      lang: Language,
  ): UI =
    val dimsStr = formatDimension(preset.defaultSpecs.dimensions.finishedBoundingBox)
    val qtyStr  = s"${preset.defaultSpecs.quantity.value} pcs"

    div.cssClass("mp-card-product")(
      div.style(Style.column.gap(4.px))(
        h3(preset.name(lang)).style(Style.margin(0.px).fontSize(17.px).fontWeight(FontWeight.w700).color(ink)),
        preset.description.map(d => p(d(lang)).style(mutedText.margin(0.px))).getOrElse(UI.empty),
      ),
      div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
        badge(dimsStr, BadgeTone.Neutral),
        badge(qtyStr, BadgeTone.Neutral),
        preset.defaultSpecs.binding
          .map(b => badge(s"${bindingMethodLabel(b.method, lang)}, ${b.totalPages}p", BadgeTone.Neutral))
          .getOrElse(UI.empty),
      ),
      // Live price area
      div.style(Style.column.gap(4.px).padding(12.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border).flexGrow(1.0))(
        quoteStatus match
          case QuoteStatus.Loading =>
            span(state.i18n.t("detail-loading-price")).style(mutedText)
          case QuoteStatus.Failed(err) =>
            span(err.message).style(Style.fontSize(12.px).color(roseInk))
          case QuoteStatus.Ready(res) =>
            val q = res.pricing.effectiveQuote
            div.style(Style.column.gap(4.px))(
              res.pricing.walkInBaseQuote match
                case Some(walkIn) =>
                  span(formatMoney(walkIn.grossTotal, currency, lang)).style(smallText.strikethrough)
                case None => UI.empty,
              div.style(rowBetween)(
                span(formatMoney(q.grossTotal, currency, lang)).style(Style.fontSize(22.px).fontWeight(FontWeight.w800).color(ink)),
                res.pricing.savingsPercent
                  .filter(_ > BigDecimal(0))
                  .map(pct => badge(s"-${pct.setScale(0, BigDecimal.RoundingMode.HALF_UP)}% B2B", BadgeTone.Emerald))
                  .getOrElse(UI.empty),
              ),
              span(s"${if lang == Language.Cs then "Bez DPH" else "Excl. VAT"}: ${formatMoney(q.netTotal, currency, lang)}").style(smallText),
            ),
      ),
      // Action buttons
      div.style(Style.column.gap(8.px))(
        div.style(Style.row.gap(8.px).flexWrap(FlexWrap.wrap))(
          button
            .cssClass("mp-btn mp-btn-primary mp-btn-sm")
            .style(Style.flexGrow(1.0))
            .onClick(state.addPresetToCart(cat.id, preset.id))(state.i18n.t("detail-add-cart")),
          quoteStatus match
            case QuoteStatus.Ready(res) =>
              button
                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                .style(Style.flexGrow(1.0))
                .onClick(state.startQuickOrder(res, ManufacturingSpeed.Standard))(state.i18n.t("detail-quick-order"))
            case _ => UI.empty,
        ),
        button
          .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
          .style(Style.width(100.pct))
          .onClick(state.openBuilder(Some(cat.id), Some(preset.id)))(state.i18n.t("detail-use-preset")),
      ),
    )
