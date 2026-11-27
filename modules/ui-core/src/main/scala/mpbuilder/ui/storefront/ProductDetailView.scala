package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.configurator.{ConfiguratorController, QuoteStatus}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** E-shop style Product Detail page.
  *
  * Desktop: product image gallery on the left, buy box on the right. The buy box leads with the platform's primary
  * paths — configure your own product, or design it in the Design Studio — and offers ready-made presets as a
  * secondary "quick start" (preset picker, live price, Add-to-Cart / Quick Order). Below: full description,
  * specifications, a preset comparison, a Design Studio promo and the "how it works" steps. Tablet and mobile stack
  * everything.
  *
  * @param selectedPreset preset picked in the buy box (falls back to the first available preset)
  * @param onDesignInStudio opens the Design Studio on the chosen preset (storefront hosts that embed the editor)
  * @param designStudioHref plain link to the standalone Design Studio page (fallback when not embedded)
  */
object ProductDetailView:

  def render(
      state: StorefrontState,
      categoryId: CategoryId,
      selectedPreset: SignalRef[Option[PresetId]],
      onDesignInStudio: Option[(ProductCategory, PresetId) => Unit < Async] = None,
      designStudioHref: Option[String] = None,
  ): UI =
    val combined =
      state.catalogRef
        .combineLatest(state.presetQuotes)
        .combineLatest(state.currency)
        .combineLatest(state.i18n.language)
        .combineLatest(selectedPreset)
        .combineLatest(state.presetQuantities)

    combined.render { case (((((catalog, quotesMap), cur), lang), selectedOpt), qtyMap) =>
      catalog.categories.get(categoryId) match
        case None =>
          div.cssClass("mp-card")(
            alertBanner(BadgeTone.Rose, span(state.i18n.t("detail-not-found-title")), List(categoryId.value)),
            button.cssClass("mp-btn mp-btn-secondary").onClick(state.openCatalog)(state.i18n.t("detail-back")),
          )
        case Some(cat) if !cat.enabled =>
          div.cssClass("mp-card")(
            alertBanner(
              BadgeTone.Amber,
              span(state.i18n.t("detail-unavailable-title")),
              List(cat.name(lang)),
            ),
            button.cssClass("mp-btn mp-btn-secondary").onClick(state.openCatalog)(state.i18n.t("detail-back")),
          )
        case Some(cat) =>
          val listingOpt       = catalog.showcases.get(categoryId)
          val availablePresets = cat.presets.filter(ConfiguratorController.isPresetAvailable(_, catalog))
          val activePreset     = selectedOpt.flatMap(id => availablePresets.find(_.id == id)).orElse(availablePresets.headOption)
          val ctx              = Ctx(state, catalog, cat, listingOpt, quotesMap, qtyMap, cur, lang, onDesignInStudio, designStudioHref)

          div.style(Style.column.gap(28.px).width(100.pct))(
            renderBreadcrumb(ctx),
            div.cssClass("mp-pd-top")(
              renderGallery(ctx),
              renderBuyBox(ctx, availablePresets, activePreset, selectedPreset),
            ),
            renderTrustStrip(ctx),
            div.cssClass("mp-pd-details")(
              div.cssClass("mp-pd-details-main")(
                renderDescription(ctx),
                if availablePresets.size > 1 then renderPresetComparison(ctx, availablePresets, activePreset, selectedPreset) else UI.empty,
              ),
              div.cssClass("mp-pd-details-aside")(
                renderSpecs(ctx, activePreset),
                renderHowItWorks(ctx),
              ),
            ),
            renderStudioPromo(ctx, activePreset),
          )
    }

  private final case class Ctx(
      state: StorefrontState,
      catalog: ProductCatalog,
      cat: ProductCategory,
      listing: Option[ShowcaseListing],
      quotes: Map[PresetId, QuoteStatus],
      quantities: Map[PresetId, Quantity],
      currency: Currency,
      lang: Language,
      onDesignInStudio: Option[(ProductCategory, PresetId) => Unit < Async],
      designStudioHref: Option[String],
  ):
    def t(key: String): Signal[String] = state.i18n.t(key)
    val isCs: Boolean      = lang == Language.Cs
    def group: Option[CatalogGroup] = listing.map(_.group)
    /** Quick-start quantity of a preset (the preset's default until the customer changes it). */
    def qtyOf(preset: CategoryPreset): Int = quantities.getOrElse(preset.id, preset.defaultSpecs.quantity).value

  // ── Breadcrumb ─────────────────────────────────────────────────────────────

  private def renderBreadcrumb(c: Ctx): UI =
    nav.cssClass("mp-pd-breadcrumb").aria("label", "Breadcrumb")(
      button.cssClass("mp-pd-crumb").onClick(c.state.openCatalog)(Icons(Icon.ArrowLeft, 14), c.t("nav-catalog")),
      c.group
        .map(g =>
          UI.fragment(
            span(Icons(Icon.ChevronRight, 13)).style(Style.color(inkMuted).row.align(Alignment.center)),
            span(catalogGroupLabel(g, c.lang)).style(Style.color(inkMuted)),
          )
        )
        .getOrElse(UI.empty),
      span(Icons(Icon.ChevronRight, 13)).style(Style.color(inkMuted).row.align(Alignment.center)),
      span(c.cat.name(c.lang)).style(Style.color(ink).fontWeight(FontWeight.w600)),
    )

  // ── Left: Gallery ──────────────────────────────────────────────────────────

  private def renderGallery(c: Ctx): UI =
    val title = c.cat.name(c.lang)
    val media = c.listing.flatMap(_.imageUrl) match
      case Some(url) =>
        div.cssClass("mp-pd-media")(img(UI.ImgSrc.Path(url), title).cssClass("mp-product-img"))
      case None =>
        val (accent, soft) = c.group.map(groupAccent).getOrElse((brand, brandSoft))
        div
          .cssClass("mp-pd-media mp-pd-media-glyph")
          .style(Style.bgGradient(GradientDirection.toBottomRight, (soft, 0.pct), (surface, 100.pct)).color(accent))(
            div.cssClass("mp-product-glyph").style(Style.width(96.px).height(96.px).rounded(28.px))(
              Icons(c.group.map(groupGlyph).getOrElse(Icon.Package), 44, 1.6)
            )
          )

    div.cssClass("mp-pd-gallery")(
      media,
      div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
        c.group.map(g => badge(catalogGroupLabel(g, c.lang), BadgeTone.Brand)).getOrElse(UI.empty),
        span.cssClass("mp-chip")(Icons(Icon.Zap, 13), c.t("detail-feat-live-price")),
        span.cssClass("mp-chip")(Icons(Icon.CheckCircle, 13), c.t("detail-feat-preflight")),
        if c.onDesignInStudio.isDefined || c.designStudioHref.isDefined then
          span.cssClass("mp-chip")(Icons(Icon.Palette, 13), c.t("detail-feat-design-online"))
        else UI.empty,
      ),
    )

  // ── Right: Buy Box ─────────────────────────────────────────────────────────
  //
  // Custom configuration is the primary path: the buy box leads with "configure your own" and
  // "design it online"; ready-made presets are a secondary quick start for newcomers (and for
  // reorder-style products such as business cards) with their own add-to-cart / quick-order.

  private def renderBuyBox(
      c: Ctx,
      presets: List[CategoryPreset],
      active: Option[CategoryPreset],
      selectedRef: SignalRef[Option[PresetId]],
  ): UI =
    val shortDesc = c.listing.map(_.tagline(c.lang)).orElse(c.cat.description.map(_(c.lang)))

    div.cssClass("mp-pd-buy")(
      div.style(stackSm)(
        h1(c.cat.name(c.lang)).cssClass("mp-pd-title"),
        shortDesc.map(d => p(d).style(Style.fontSize(16.px).lineHeight(1.55).color(inkSecondary).margin(0.px))).getOrElse(UI.empty),
        div.style(rowWrap)(
          UI.fragment(c.cat.components.toList.map(tpl => span.cssClass("mp-chip")(Icons(Icon.Layers, 13), tpl.label(c.lang)))*),
          if presets.nonEmpty then
            span.cssClass("mp-chip")(
              Icons(Icon.Sparkles, 13),
              c.state.i18n.t(pluralKey("catalog-presets", presets.size, c.lang), Dict("count" -> presets.size.toString)),
            )
          else UI.empty,
        ),
        renderFromPrice(c, presets),
      ),
      renderCreatePaths(c, active),
      if presets.nonEmpty then renderQuickStart(c, presets, active, selectedRef)
      else p(c.t("detail-no-presets")).style(smallText.margin(0.px)),
    )

  /** "From X" teaser — the cheapest ready preset quote, so the custom-first box still anchors a price. */
  private def renderFromPrice(c: Ctx, presets: List[CategoryPreset]): UI =
    val cheapest = presets.flatMap(p =>
      c.quotes.get(p.id).collect { case QuoteStatus.Ready(res) => res.pricing.effectiveQuote.grossTotal }
    ).minByOption(_.value)
    cheapest match
      case Some(price) =>
        div.style(rowSm)(
          span(c.state.i18n.t("detail-from-price", Dict("price" -> formatMoney(price, c.currency, c.lang))))
            .cssClass("mp-num")
            .style(Style.fontSize(18.px).fontWeight(FontWeight.w800).color(ink)),
          span(c.t("detail-from-price-note")).style(smallText),
        )
      case None => UI.empty

  private def pathCard(icon: Icon, titleKey: String, descKey: String, c: Ctx): UI =
    UI.fragment(
      span.cssClass("mp-pd-path-icon")(Icons(icon, 20)),
      div.cssClass("mp-pd-path-copy")(
        span(c.t(titleKey)).cssClass("mp-pd-path-title"),
        span(c.t(descKey)).cssClass("mp-pd-path-desc"),
      ),
      span.cssClass("mp-pd-path-arrow")(Icons(Icon.ArrowRight, 18)),
    )

  private def renderCreatePaths(c: Ctx, active: Option[CategoryPreset]): UI =
    val configure =
      button
        .cssClass("mp-pd-path mp-pd-path-primary")
        .onClick(c.state.openBuilder(Some(c.cat.id), active.map(_.id)))(
          pathCard(Icon.Sliders, "detail-configure-title", "detail-configure-desc", c)
        )

    val studio: UI =
      (c.onDesignInStudio, active) match
        case (Some(open), Some(preset)) =>
          button
            .cssClass("mp-pd-path mp-pd-path-studio")
            .onClick(open(c.cat, preset.id))(pathCard(Icon.Palette, "detail-studio-title", "detail-studio-desc", c))
        case _ =>
          c.designStudioHref match
            case Some(href) =>
              a.cssClass("mp-pd-path mp-pd-path-studio")
                .href(UI.Href.Path(href))(pathCard(Icon.Palette, "detail-studio-title", "detail-studio-desc", c))
            case None => UI.empty

    val startHint: UI =
      active match
        case Some(preset) =>
          div.style(Style.row.align(Alignment.center).gap(6.px).flexWrap(FlexWrap.wrap))(
            span(c.state.i18n.t("detail-configure-hint", Dict("preset" -> preset.name(c.lang)))).style(smallText),
            button
              .cssClass("mp-pd-link")
              .onClick(c.state.openBuilder(Some(c.cat.id)))(c.t("detail-start-blank")),
          )
        case None => UI.empty

    div.cssClass("mp-pd-paths")(
      span(c.t("detail-make-own-title")).style(labelStyle),
      configure,
      studio,
      startHint,
    )

  private def presetSummary(preset: CategoryPreset, lang: Language, quantity: Int): String =
    val dims = formatDimension(preset.defaultSpecs.dimensions.finishedBoundingBox)
    val qty  = s"$quantity ${if lang == Language.Cs then "ks" else "pcs"}"
    val bind = preset.defaultSpecs.binding.map(b => s"${bindingMethodLabel(b.method, lang)}, ${b.totalPages} ${if lang == Language.Cs then "str." else "pp"}")
    (List(dims, qty) ++ bind).mkString(" · ")

  private def renderQuickStart(
      c: Ctx,
      presets: List[CategoryPreset],
      active: Option[CategoryPreset],
      selectedRef: SignalRef[Option[PresetId]],
  ): UI =
    div.cssClass("mp-pd-quick")(
      div.style(stackXs)(
        div.style(rowSm)(
          span(Icons(Icon.Zap, 15)).style(Style.color(brand).row.align(Alignment.center)),
          span(c.t("detail-quick-start-title")).style(Style.fontSize(15.px).fontWeight(FontWeight.w700).color(ink)),
        ),
        p(c.t("detail-quick-start-desc")).style(smallText.margin(0.px)),
      ),
      div.cssClass("mp-pd-preset-list").role("radiogroup")(
        presets.map(preset => renderPresetOption(c, preset, active.exists(_.id == preset.id), selectedRef))*
      ),
      active.map(renderPresetOrder(c, _)).getOrElse(UI.empty),
    )

  private def renderPresetOption(c: Ctx, preset: CategoryPreset, isActive: Boolean, selectedRef: SignalRef[Option[PresetId]]): UI =
    val priceText = c.quotes.get(preset.id) match
      case Some(QuoteStatus.Ready(res)) => span(formatMoney(res.pricing.effectiveQuote.grossTotal, c.currency, c.lang)).cssClass("mp-num")
      case Some(QuoteStatus.Failed(_))  => span("—")
      case _                            => span("…")

    button
      .cssClass(if isActive then "mp-pd-preset mp-pd-preset-active" else "mp-pd-preset")
      .role("radio")
      .aria("checked", isActive.toString)
      .onClick(selectedRef.set(Some(preset.id)))(
        span.cssClass("mp-pd-radio")(if isActive then span.cssClass("mp-pd-radio-dot") else UI.empty),
        div.style(Style.column.gap(2.px).flexGrow(1.0).minWidth(0.px))(
          span(preset.name(c.lang)).style(Style.fontSize(14.px).fontWeight(FontWeight.w700).color(ink)),
          span(presetSummary(preset, c.lang, c.qtyOf(preset))).style(smallText),
        ),
        priceText.style(Style.fontSize(14.px).fontWeight(FontWeight.w700).color(ink).whiteSpace(WhiteSpace.noWrap)),
      )

  /** Quick quantity choices around the preset's default: a log-scale ladder, the 6 values nearest the default. */
  private def quantityChoices(default: Int): List[Int] =
    val ladder = List(1, 2, 5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000)
    def dist(q: Int): Double = math.abs(math.log(q.toDouble) - math.log(default.toDouble))
    (default :: ladder).distinct.sortBy(dist).take(6).sorted

  private val MaxPresetQuantity = 1000000

  private def renderQuantityPicker(c: Ctx, preset: CategoryPreset): UI =
    val current = c.qtyOf(preset)
    def setQty(q: Int): Unit < Async =
      if q == current then Kyo.unit else c.state.setPresetQuantity(c.cat.id, preset.id, Quantity.unsafe(q))
    // Commit on change (blur / Enter) only: re-pricing re-renders the box, which would fight the caret while typing.
    def applyRaw(raw: String): Unit < Async =
      raw.trim.toIntOption match
        case Some(n) if n >= 1 && n <= MaxPresetQuantity => setQty(n)
        case _                                          => Kyo.unit
    val choices = quantityChoices(preset.defaultSpecs.quantity.value)

    div.cssClass("mp-pd-qty")(
      div.style(rowBetween)(
        label.style(labelStyle)(c.t("detail-quantity-label")),
        if current != preset.defaultSpecs.quantity.value then
          button.cssClass("mp-pd-link").onClick(setQty(preset.defaultSpecs.quantity.value))(c.t("detail-quantity-reset"))
        else UI.empty,
      ),
      div.cssClass("mp-pd-qty-row")(
        UI.fragment(
          choices.map { q =>
            button
              .cssClass(if q == current then "mp-pill-active" else "mp-pill")
              .style(Style.padding(6.px, 12.px))
              .onClick(setQty(q))(q.toString)
          }*
        ),
        input
          .cssClass("mp-input mp-pd-qty-input")
          .aria("label", if c.isCs then "Vlastní počet kusů" else "Custom quantity")
          .value(current.toString)
          .inputFilter(InputFilter.Digits)
          .onChange(applyRaw),
      ),
    )

  /** Price + order-as-is actions for the selected preset (secondary to the custom paths above). */
  private def renderPresetOrder(c: Ctx, preset: CategoryPreset): UI =
    val quote = c.quotes.getOrElse(preset.id, QuoteStatus.Loading)

    div.cssClass("mp-pd-price-box")(
      renderQuantityPicker(c, preset),
      quote match
        case QuoteStatus.Loading =>
          div.style(rowSm)(Icons(Icon.Clock, 16), span(c.t("detail-loading-price")).style(mutedText))
        case QuoteStatus.Failed(err) =>
          span(err.message).style(Style.fontSize(13.px).color(roseInk))
        case QuoteStatus.Ready(res) =>
          val q = res.pricing.effectiveQuote
          val qty = c.qtyOf(preset)
          div.style(stackXs)(
            div.style(Style.row.align(Alignment.end).gap(10.px).flexWrap(FlexWrap.wrap))(
              span(formatMoney(q.grossTotal, c.currency, c.lang)).cssClass("mp-pd-price mp-num"),
              res.pricing.walkInBaseQuote
                .map(w => span(formatMoney(w.grossTotal, c.currency, c.lang)).style(mutedText.strikethrough.padding(0.px, 0.px, 4.px, 0.px)))
                .getOrElse(UI.empty),
              res.pricing.savingsPercent
                .filter(_ > BigDecimal(0))
                .map(pct => span(badge(s"-${pct.setScale(0, BigDecimal.RoundingMode.HALF_UP)}% B2B", BadgeTone.Emerald)).style(Style.padding(0.px, 0.px, 4.px, 0.px)))
                .getOrElse(UI.empty),
            ),
            span(
              s"${if c.isCs then "Bez DPH" else "Excl. VAT"}: ${formatMoney(q.netTotal, c.currency, c.lang)}" +
                (if qty > 0 then s" · ${formatMoney(q.grossTotal / qty, c.currency, c.lang)} / ${if c.isCs then "ks" else "pc"}" else "")
            ).style(smallText),
          ),
      div.style(rowSm.padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
        span(Icons(Icon.Truck, 15)).style(Style.color(emerald).row.align(Alignment.center)),
        span(c.t("detail-dispatch-note")).style(smallText),
      ),
      div.style(Style.row.gap(8.px).flexWrap(FlexWrap.wrap))(
        button
          .cssClass("mp-btn mp-btn-secondary")
          .style(Style.flexGrow(1.0).flexBasis(150.px))
          .onClick(c.state.addPresetToCart(c.cat.id, preset.id))(Icons(Icon.Cart, 16), span(c.t("detail-add-cart"))),
        quote match
          case QuoteStatus.Ready(_) =>
            button
              .cssClass("mp-btn mp-btn-ghost")
              .style(Style.flexGrow(1.0).flexBasis(130.px))
              .onClick(c.state.startPresetQuickOrder(c.cat.id, preset.id))(Icons(Icon.Zap, 15), span(c.t("detail-quick-order")))
          case _ => UI.empty,
      ),
    )

  // ── Trust strip ────────────────────────────────────────────────────────────

  private def renderTrustStrip(c: Ctx): UI =
    def item(icon: Icon, titleKey: String, descKey: String): UI =
      div.cssClass("mp-pd-trust-item")(
        span.cssClass("mp-pd-trust-icon")(Icons(icon, 18)),
        div.style(Style.column.gap(2.px).minWidth(0.px))(
          span(c.t(titleKey)).style(Style.fontSize(14.px).fontWeight(FontWeight.w700).color(ink)),
          span(c.t(descKey)).style(smallText),
        ),
      )
    div.cssClass("mp-pd-trust")(
      item(Icon.Zap, "detail-trust-price-title", "detail-trust-price-desc"),
      item(Icon.Shield, "detail-trust-proof-title", "detail-trust-proof-desc"),
      item(Icon.Truck, "detail-trust-ship-title", "detail-trust-ship-desc"),
      item(Icon.Percent, "detail-trust-b2b-title", "detail-trust-b2b-desc"),
    )

  // ── Below: Description ─────────────────────────────────────────────────────

  private def renderDescription(c: Ctx): UI =
    val long = c.listing.map(_.detailedDescription(c.lang)).filter(_.nonEmpty)
    val catDesc = c.cat.description.map(_(c.lang)).filter(d => !long.contains(d))
    div.cssClass("mp-card")(
      h2(c.t("detail-about-title")).style(sectionTitle.margin(0.px)),
      long.map(d => p(d).style(Style.fontSize(15.px).lineHeight(1.7).color(inkSecondary).margin(0.px))).getOrElse(UI.empty),
      catDesc.map(d => p(d).style(Style.fontSize(15.px).lineHeight(1.7).color(inkSecondary).margin(0.px))).getOrElse(UI.empty),
      ul.cssClass("mp-pd-bullets")(
        li(Icons(Icon.Check, 15), span(c.t("detail-bullet-materials"))),
        li(Icons(Icon.Check, 15), span(c.t("detail-bullet-artwork"))),
        li(Icons(Icon.Check, 15), span(c.t("detail-bullet-imposition"))),
      ),
    )

  // ── Below: Preset comparison ───────────────────────────────────────────────

  private def renderPresetComparison(
      c: Ctx,
      presets: List[CategoryPreset],
      active: Option[CategoryPreset],
      selectedRef: SignalRef[Option[PresetId]],
  ): UI =
    div.style(stackMd)(
      div.style(stackXs)(
        h2(c.t("detail-presets-title")).style(sectionTitle.margin(0.px)),
        p(c.t("detail-presets-subtitle")).style(mutedText.margin(0.px)),
      ),
      div.cssClass("mp-pd-compare")(
        presets.map { preset =>
          val isActive = active.exists(_.id == preset.id)
          val quote    = c.quotes.getOrElse(preset.id, QuoteStatus.Loading)
          div.cssClass(if isActive then "mp-pd-compare-card mp-pd-compare-card-active" else "mp-pd-compare-card")(
            div.style(stackXs)(
              div.style(rowBetween)(
                h3(preset.name(c.lang)).style(Style.margin(0.px).fontSize(16.px).fontWeight(FontWeight.w700).color(ink)),
                if isActive then span(Icons(Icon.CheckCircle, 18)).style(Style.color(brand).row.align(Alignment.center)) else UI.empty,
              ),
              preset.description.map(d => p(d(c.lang)).style(smallText.margin(0.px))).getOrElse(UI.empty),
            ),
            presetFacts(c, preset),
            div.style(rowBetween.padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
              quote match
                case QuoteStatus.Ready(res) =>
                  span(formatMoney(res.pricing.effectiveQuote.grossTotal, c.currency, c.lang))
                    .cssClass("mp-num")
                    .style(Style.fontSize(18.px).fontWeight(FontWeight.w800).color(ink))
                case QuoteStatus.Failed(_) => span("—").style(mutedText)
                case QuoteStatus.Loading   => span(c.t("detail-loading-price")).style(smallText),
              button
                .cssClass(if isActive then "mp-btn mp-btn-secondary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                .disabled(isActive)
                .onClick(selectedRef.set(Some(preset.id)))(c.t(if isActive then "detail-selected" else "detail-select")),
            ),
          )
        }*
      ),
    )

  private def presetFacts(c: Ctx, preset: CategoryPreset): UI =
    def fact(icon: Icon, text: String): UI =
      li(span(Icons(icon, 14)).style(Style.color(inkMuted).row.align(Alignment.center)), span(text))
    val comps = preset.components.toList.flatMap { cp =>
      val mat = c.catalog.materials.get(cp.materialId).map(_.name(c.lang))
      val pm  = cp.printingMethodId.flatMap(c.catalog.printingMethods.get).map(_.name(c.lang))
      val ink = cp.inkConfiguration.map(_.notation)
      val fin = cp.finishes.flatMap(f => c.catalog.finishes.get(f.finishId)).map(_.name(c.lang))
      val printing = (pm.toList ++ ink.map(i => if pm.isDefined then s"($i)" else i).toList).mkString(" ")
      List(
        mat.map(m => fact(Icon.Layers, m)),
        Option.when(printing.nonEmpty)(fact(Icon.Printer, printing)),
        Option.when(fin.nonEmpty)(fact(Icon.Sparkles, fin.mkString(", "))),
      ).flatten
    }
    ul.cssClass("mp-pd-facts")(
      UI.fragment((fact(Icon.Ruler, presetSummary(preset, c.lang, c.qtyOf(preset))) :: comps)*)
    )

  // ── Below: Specifications ──────────────────────────────────────────────────

  private def renderSpecs(c: Ctx, active: Option[CategoryPreset]): UI =
    def row(label: UI, value: String): UI =
      div.cssClass("mp-pd-spec-row")(span.style(labelStyle)(label), span(value).style(Style.fontSize(13.px).color(ink).textAlign(TextAlign.right)))

    val compRows = c.cat.components.toList.map { tpl =>
      val mats  = tpl.allowedMaterialIds.count(id => c.catalog.materials.get(id).exists(_.enabled))
      val fins  = tpl.allowedFinishIds.count(id => c.catalog.finishes.get(id).exists(_.enabled))
      val value =
        if c.isCs then s"$mats materiálů · $fins úprav"
        else s"$mats materials · $fins finishes"
      row(span(tpl.label(c.lang)), value)
    }
    val methods = c.cat.allowedPrintingMethodIds.toList.flatMap(c.catalog.printingMethods.get).filter(_.enabled).map(_.name(c.lang)).sorted

    div.cssClass("mp-card")(
      h2(c.t("detail-specs-title")).style(sectionTitle.margin(0.px)),
      div.style(Style.column)(
        UI.fragment((compRows ++
          Option.when(methods.nonEmpty)(row(span(c.t("detail-spec-printing")), methods.mkString(", "))).toList ++
          active.map(p => row(span(c.t("detail-starting-specs")), presetSummary(p, c.lang, p.defaultSpecs.quantity.value))).toList ++
          active.flatMap(_.defaultSpecs.bleed).map(b => row(span(c.t("detail-spec-bleed")), s"${b.value} mm")).toList)*)
      ),
    )

  private def renderHowItWorks(c: Ctx): UI =
    def step(n: Int, key: String): UI =
      li(span(n.toString).cssClass("mp-pd-step-num"), span(c.t(key)).style(Style.fontSize(13.px).color(inkSecondary).lineHeight(1.45)))
    div.cssClass("mp-card")(
      h2(c.t("detail-how-title")).style(sectionTitle.margin(0.px)),
      ol.cssClass("mp-pd-steps")(
        step(1, "detail-how-1"),
        step(2, "detail-how-2"),
        step(3, "detail-how-3"),
        step(4, "detail-how-4"),
      ),
    )

  // ── Below: Design Studio promo ─────────────────────────────────────────────

  private def renderStudioPromo(c: Ctx, active: Option[CategoryPreset]): UI =
    val cta: UI =
      (c.onDesignInStudio, active) match
        case (Some(open), Some(preset)) =>
          button.cssClass("mp-btn mp-studio-cta").onClick(open(c.cat, preset.id))(
            Icons(Icon.Palette, 17),
            span(c.t("detail-design-studio-cta")),
            Icons(Icon.ArrowRight, 16),
          )
        case _ =>
          c.designStudioHref match
            case Some(href) =>
              a.cssClass("mp-btn mp-studio-cta").href(UI.Href.Path(href))(
                Icons(Icon.Palette, 17),
                span(c.t("studio-open")),
                Icons(Icon.ArrowRight, 16),
              )
            case None => UI.empty
    if c.onDesignInStudio.isEmpty && c.designStudioHref.isEmpty then UI.empty
    else StudioPromo.render(c.state, cta)

/** Design Studio promotional band shared by the catalog and the product detail page. */
object StudioPromo:

  def render(state: StorefrontState, cta: UI): UI =
    def feat(icon: Icon, key: String): UI =
      li(span(Icons(icon, 15)).cssClass("mp-studio-feat-icon"), span(state.i18n.t(key)))

    section.cssClass("mp-studio-promo")(
      div.cssClass("mp-studio-copy")(
        span.cssClass("mp-studio-eyebrow")(Icons(Icon.Sparkles, 13), state.i18n.t("studio-eyebrow")),
        h2(state.i18n.t("studio-title")).cssClass("mp-studio-title"),
        p(state.i18n.t("studio-subtitle")).style(Style.margin(0.px).fontSize(15.px).lineHeight(1.6).color(Color.rgba(255, 255, 255, 0.86))),
        ul.cssClass("mp-studio-feats")(
          feat(Icon.Grid, "studio-feat-templates"),
          feat(Icon.Image, "studio-feat-photos"),
          feat(Icon.Ruler, "studio-feat-bleed"),
          feat(Icon.CheckCircle, "studio-feat-preflight"),
        ),
        div.style(Style.row.flexWrap(FlexWrap.wrap).gap(10.px).margin(4.px, 0.px, 0.px, 0.px))(cta),
      ),
      div.cssClass("mp-studio-visual").aria("hidden", "true")(
        div.cssClass("mp-studio-window")(
          div.cssClass("mp-studio-window-bar")(span(), span(), span()),
          div.cssClass("mp-studio-window-body")(
            div.cssClass("mp-studio-tools")(
              span(Icons(Icon.Pencil, 14)),
              span(Icons(Icon.Image, 14)),
              span(Icons(Icon.Layers, 14)),
              span(Icons(Icon.Palette, 14)),
            ),
            div.cssClass("mp-studio-canvas")(
              div.cssClass("mp-studio-card")(
                div.cssClass("mp-studio-card-photo"),
                div.cssClass("mp-studio-card-lines")(span(), span(), span()),
              )
            ),
          ),
        )
      ),
    )
end StudioPromo
