package mpbuilder.ui.configurator

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.{ArtworkComponents, BrowserPickedFile, CommonComponents}
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

enum ConfiguratorHost:
  case Storefront(
      onAddToCart: (DraftConfig, ProductBuilderQuoteResult) => Unit < Async,
      onQuickOrder: (DraftConfig, ProductBuilderQuoteResult) => Unit < Async,
      onPreviewArtwork: (ArtworkSubmissionView, Option[String]) => Unit < Sync = (_, _) => Kyo.unit,
      onLinkCopied: String => Unit < Async = _ => Kyo.unit,
      onArtworkUploaded: () => Unit < Async = () => Kyo.unit,
      onArtworkRemoved: () => Unit < Async = () => Kyo.unit,
  )
  case Calculator(
      orderEmail: Option[String],
      onRequestEmailOrder: (DraftConfig, ProductBuilderQuoteResult) => Unit < Async,
  )

/** Shared, fully responsive Kyo UI Product Configurator used by both the Storefront SPA and Standalone Calculator Widget. */
object ConfiguratorView:

  private val standardSizes: List[(String, Dimension2D)] = List(
    "85×55" -> Dimension2D(PositiveMm.unsafe(85), PositiveMm.unsafe(55)),
    "90×50" -> Dimension2D(PositiveMm.unsafe(90), PositiveMm.unsafe(50)),
    "A6"    -> Dimension2D.A6,
    "DL"    -> Dimension2D(PositiveMm.unsafe(99), PositiveMm.unsafe(210)),
    "A5"    -> Dimension2D.A5,
    "A4"    -> Dimension2D.A4,
    "A3"    -> Dimension2D.A3,
  )

  private val standardQuantities: List[Int] = List(25, 50, 100, 250, 500, 1000, 2500, 5000)

  def render(
      ctrl: ConfiguratorController,
      host: ConfiguratorHost,
      showHeader: Boolean = true,
  ): UI =
    val combined =
      ctrl.draft
        .combineLatest(ctrl.catalogRef)
        .combineLatest(ctrl.i18n.language)
        .combineLatest(ctrl.compatibleFinishesMap)
        .combineLatest(ctrl.finishLimitsMap)

    val quoteCombined =
      ctrl.quoteStatus
        .combineLatest(ctrl.draft)
        .combineLatest(ctrl.currency)
        .combineLatest(ctrl.i18n.language)

    div.style(Style.column.gap(20.px).width(100.pct))(
      if showHeader then
        div.style(Style.column.gap(4.px))(
          h1(ctrl.i18n.t("cfg-title")).style(Style.fontSize(26.px).fontWeight(FontWeight.w800).margin(0.px).color(ink)),
          p(ctrl.i18n.t("cfg-subtitle")).style(mutedText.margin(0.px)),
        )
      else UI.empty,
      div.cssClass("mp-split-layout")(
        // Left: 5-step configuration & artwork form
        div.cssClass("mp-split-main")(
          combined.render { case ((((d, catalog), lang), finMap), limMap) =>
            ctrl.currentCategory(d, catalog) match
              case None =>
                alertBanner(
                  BadgeTone.Rose,
                  span(if lang == Language.Cs then "Produkt není dostupný" else "Product Unavailable"),
                  List(if lang == Language.Cs then "Žádná aktivní kategorie produktu není momentálně k dispozici." else "No active product category is currently available."),
                )
              case Some(cat) =>
                div.style(Style.column.gap(20.px))(
                  renderProductAndPresetCard(ctrl, catalog, cat, d, lang),
                  renderComponentsCard(ctrl, catalog, cat, d, finMap, limMap, lang),
                  renderSpecsCard(ctrl, cat, d, lang),
                  renderSpeedCard(ctrl, d, host, lang),
                  host match
                    case sf: ConfiguratorHost.Storefront =>
                      renderArtworkCard(ctrl, cat, d, sf, lang)
                    case _: ConfiguratorHost.Calculator =>
                      UI.empty,
                )
          },
        ),
        // Right: Sticky Live Price Summary
        div.cssClass("mp-split-aside")(
          quoteCombined.render { case (((status, d), cur), lang) =>
            renderQuoteSidebar(ctrl, status, d, cur, lang, host)
          },
        ),
      ),
    )

  // ── Step 1: Product Category & Quick-Start Presets ─────────────────────────

  private def renderProductAndPresetCard(
      ctrl: ConfiguratorController,
      catalog: ProductCatalog,
      cat: ProductCategory,
      d: DraftConfig,
      lang: Language,
  ): UI =
    val orderedCategories: List[ProductCategory] =
      val showcases = catalog.showcases
      catalog.categories.values.toList
        .filter(_.enabled)
        .sortBy { c =>
          showcases.get(c.id).map(_.sortOrder).getOrElse(999)
        }

    val availablePresets = cat.presets.filter(ConfiguratorController.isPresetAvailable(_, catalog))

    div.cssClass("mp-card")(
      div.style(rowBetween)(
        h2(ctrl.i18n.t("cfg-section-product")).style(sectionTitle.margin(0.px)),
        catalog.showcases.get(cat.id).map(sc => badge(catalogGroupLabel(sc.group, lang), BadgeTone.Brand)).getOrElse(UI.empty),
      ),
      div.cssClass("mp-field")(
        label.style(labelStyle)(ctrl.i18n.t("cfg-category-label")),
        select
          .cssClass("mp-input")
          .value(d.categoryId.value)
          .onChange(v => ctrl.selectCategory(CategoryId.unsafe(v)))(
            orderedCategories.map { c =>
              option.value(c.id.value).selected(c.id == d.categoryId)(c.name(lang))
            }*
          ),
      ),
      cat.description.map(desc => p(desc(lang)).style(mutedText.margin(0.px))).getOrElse(UI.empty),
      if availablePresets.nonEmpty then
        div.style(Style.column.gap(8.px))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-presets-label")),
          div.cssClass("mp-option-grid")(
            availablePresets.map { preset =>
              val isActive = d.activePresetId.contains(preset.id)
              val cls      = if isActive then "mp-option-card-active" else "mp-option-card"
              button
                .cssClass(cls)
                .onClick(ctrl.applyPreset(cat.id, preset.id))(
                  div.style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink))(preset.name(lang)),
                  preset.description
                    .map(desc => div.style(smallText)(desc(lang)))
                    .getOrElse(UI.empty),
                  div.style(Style.row.gap(6.px).flexWrap(FlexWrap.wrap).margin(4.px, 0.px, 0.px, 0.px))(
                    badge(s"${preset.defaultSpecs.quantity.value} pcs", BadgeTone.Neutral),
                    badge(formatDimension(preset.defaultSpecs.dimensions.finishedBoundingBox), BadgeTone.Neutral),
                  ),
                )
            }*
          ),
        )
      else UI.empty,
    )

  // ── Step 2: Material, Printing Method, Ink & Compatible Finishes ───────────

  private def renderComponentsCard(
      ctrl: ConfiguratorController,
      catalog: ProductCatalog,
      cat: ProductCategory,
      d: DraftConfig,
      finMap: Map[ComponentKey, List[Finish]],
      limMap: Map[ComponentKey, Map[FinishType, BigDecimal]],
      lang: Language,
  ): UI =
    val activeComp = d.components.find(_.key == d.activeComponentKey).orElse(d.components.headOption)

    div.cssClass("mp-card")(
      h2(ctrl.i18n.t("cfg-section-components")).style(sectionTitle.margin(0.px)),
      if cat.isMultiComponent then
        div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
          cat.components.toList.map { tpl =>
            val isActive = tpl.key == d.activeComponentKey
            button
              .cssClass(if isActive then "mp-pill-active" else "mp-pill")
              .onClick(ctrl.selectComponentTab(tpl.key))(
                tpl.label(lang),
              )
          }*
        )
      else UI.empty,
      activeComp match
        case None => UI.empty
        case Some(comp) =>
          val tplOpt = cat.templateFor(comp.key)
          renderComponentEditor(ctrl, catalog, cat, tplOpt, comp, finMap.getOrElse(comp.key, Nil), limMap.getOrElse(comp.key, Map.empty), lang),
    )

  private def renderComponentEditor(
      ctrl: ConfiguratorController,
      catalog: ProductCatalog,
      cat: ProductCategory,
      tplOpt: Option[ComponentTemplate],
      comp: DraftComponent,
      compatibleFinishes: List[Finish],
      paramLimits: Map[FinishType, BigDecimal],
      lang: Language,
  ): UI =
    val allowedMaterials: List[Material] =
      val ids = tplOpt.map(_.allowedMaterialIds).getOrElse(catalog.materials.keySet)
      ids.toList.flatMap(catalog.materials.get).filter(_.enabled).sortBy(_.name(lang))

    val isPrintable = tplOpt.forall(_.printable)

    val allowedMethods: List[PrintingMethod] =
      if !isPrintable then Nil
      else
        val ids = tplOpt.flatMap(_.allowedPrintingMethodIds).getOrElse(cat.allowedPrintingMethodIds)
        val pool = if ids.nonEmpty then ids else catalog.printingMethods.keySet
        pool.toList.flatMap(catalog.printingMethods.get).filter(_.enabled).sortBy(_.name(lang))

    val activeMethod = comp.printingMethodId.flatMap(catalog.printingMethods.get).filter(_.enabled)

    div.style(Style.column.gap(18.px))(
      // Material selector
      div.style(Style.column.gap(8.px))(
        label.style(labelStyle)(ctrl.i18n.t("cfg-component-material")),
        div.cssClass("mp-option-grid")(
          allowedMaterials.map { mat =>
            val isSel = mat.id == comp.materialId
            button
              .cssClass(if isSel then "mp-option-card-active" else "mp-option-card")
              .onClick(ctrl.updateMaterial(comp.key, mat.id))(
                div.style(Style.fontWeight(FontWeight.w700).fontSize(13.px).color(ink))(mat.name(lang)),
                div.style(Style.row.flexWrap(FlexWrap.wrap).gap(4.px).margin(4.px, 0.px, 0.px, 0.px))(
                  badge(formatMaterialWeight(mat.weight), if isSel then BadgeTone.Brand else BadgeTone.Neutral),
                  badge(mat.surfaceCoating.toString, BadgeTone.Neutral),
                  UI.fragment(
                    mat.properties.take(2).toList.map(p => badge(p.toString, BadgeTone.Emerald))*
                  ),
                ),
              )
          }*
        ),
      ),

      // Printing method & Ink configuration
      if !isPrintable then
        div.style(Style.padding(10.px, 12.px).rounded(10.px).bg(surfaceAlt).color(inkSecondary).fontSize(13.px))(
          ctrl.i18n.t("cfg-non-printable"),
        )
      else
        div.style(Style.column.gap(16.px))(
          div.style(Style.column.gap(8.px))(
            label.style(labelStyle)(ctrl.i18n.t("cfg-component-print-method")),
            div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
              allowedMethods.map { pm =>
                val isSel = comp.printingMethodId.contains(pm.id)
                button
                  .cssClass(if isSel then "mp-pill-active" else "mp-pill")
                  .onClick(ctrl.updatePrintingMethod(comp.key, pm.id))(
                    pm.name(lang),
                  )
              }*
            ),
          ),
          div.style(Style.column.gap(8.px))(
            label.style(labelStyle)(ctrl.i18n.t("cfg-component-ink")),
            renderInkSelector(ctrl, comp, activeMethod, lang),
          ),
        ),

      // Compatible Finishes grouped by FinishCategory
      div.style(Style.column.gap(10.px))(
        label.style(labelStyle)(ctrl.i18n.t("cfg-component-finishes")),
        if compatibleFinishes.isEmpty then
          div.style(mutedText)(ctrl.i18n.t("cfg-no-finishes"))
        else
          val grouped = compatibleFinishes.groupBy(_.finishType.category).toList.sortBy(_._1.ordinal)
          div.style(Style.column.gap(14.px))(
            grouped.map { case (fCat, finishes) =>
              div.style(Style.column.gap(8.px))(
                div.style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(inkMuted).letterSpacing(0.4.px))(
                  finishCategoryLabel(fCat, lang).toUpperCase,
                ),
                div.cssClass("mp-option-grid")(
                  finishes.sortBy(_.name(lang)).map { fin =>
                    renderFinishCard(ctrl, comp, fin, paramLimits.get(fin.finishType), lang)
                  }*
                ),
              )
            }*
          ),
      ),
    )

  private def renderInkSelector(
      ctrl: ConfiguratorController,
      comp: DraftComponent,
      methodOpt: Option[PrintingMethod],
      lang: Language,
  ): UI =
    val allowDouble = methodOpt.forall(_.supportsDoubleSided)
    val allowWhite  = methodOpt.exists(_.supportedSpecialInks.contains(SpecialInk.White))
    val maxColors   = methodOpt.flatMap(_.maxColorsPerSide).getOrElse(8)

    val candidates: List[(String, InkConfiguration)] = List(
      (if lang == Language.Cs then "4/0 (Plnobarevný 1str.)" else "4/0 (Full Color 1-sided)")   -> InkConfiguration.cmyk4_0,
      (if lang == Language.Cs then "4/4 (Plnobarevný 2str.)" else "4/4 (Full Color 2-sided)")   -> InkConfiguration.cmyk4_4,
      (if lang == Language.Cs then "4/1 (Barva / ČB)" else "4/1 (Color / B&W)")                 -> InkConfiguration.cmyk4_1,
      (if lang == Language.Cs then "1/0 (Černobílý 1str.)" else "1/0 (Black & White 1-sided)")  -> InkConfiguration.mono1_0,
      (if lang == Language.Cs then "1/1 (Černobílý 2str.)" else "1/1 (Black & White 2-sided)")  -> InkConfiguration.mono1_1,
      (if lang == Language.Cs then "4+W/0 (CMYK + Bílá 1str.)" else "4+W/0 (CMYK + White)")     -> InkConfiguration.cmyk4W_0,
      (if lang == Language.Cs then "4+W/4+W (CMYK + Bílá 2str.)" else "4+W/4+W (CMYK + White 2s)") -> InkConfiguration.cmyk4W_4W,
    ).filter { case (_, ink) =>
      (allowDouble || ink.isSingleSided) &&
      (allowWhite || !ink.allSpecialInks.contains(SpecialInk.White)) &&
      ink.maxChannelsPerSide <= maxColors
    }

    div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
      candidates.map { case (lbl, ink) =>
        val isSel = comp.inkConfiguration.contains(ink)
        button
          .cssClass(if isSel then "mp-pill-active" else "mp-pill")
          .onClick(ctrl.updateInk(comp.key, ink))(lbl)
      }*
    )

  private def renderFinishCard(
      ctrl: ConfiguratorController,
      comp: DraftComponent,
      finish: Finish,
      limitOpt: Option[BigDecimal],
      lang: Language,
  ): UI =
    val selectionOpt = comp.finishes.find(_.finishId == finish.id)
    val isSelected   = selectionOpt.isDefined

    div.cssClass(if isSelected then "mp-option-card-active" else "mp-option-card")(
      div
        .style(rowBetween.cursor(Cursor.pointer))
        .onClick(ctrl.toggleFinish(comp.key, finish))(
          div.style(Style.column.gap(2.px))(
            div.style(Style.fontWeight(FontWeight.w700).fontSize(13.px).color(ink))(finish.name(lang)),
            finish.description.map(desc => div.style(smallText)(desc(lang))).getOrElse(UI.empty),
          ),
          span.style(
            Style.row
              .align(Alignment.center)
              .justify(Justification.center)
              .width(24.px)
              .height(24.px)
              .flexShrink(0.0)
              .rounded(999.px)
              .bg(if isSelected then brand else surfaceAlt)
              .color(if isSelected then Color.white else inkMuted)
              .border(1.px, if isSelected then brand else Palette.border)
              .transition(TransitionProperty.all, 140, Easing.easeOut),
          )(Icons(if isSelected then Icons.Icon.Check else Icons.Icon.Plus, 14, 2.4)),
        ),
      selectionOpt match
        case None => UI.empty
        case Some(sel) =>
          div.style(Style.column.gap(8.px).margin(8.px, 0.px, 0.px, 0.px).padding(8.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
            // Side selector if Selectable
            finish.sidePolicy match
              case FinishSidePolicy.Selectable(allowed, _) =>
                div.style(Style.column.gap(4.px))(
                  div.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-finish-side")),
                  div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
                    allowed.toList.sortBy(_.ordinal).map { side =>
                      val activeSide = sel.requestedSide.contains(side)
                      button
                        .cssClass(if activeSide then "mp-pill-active" else "mp-pill")
                        .style(Style.padding(4.px, 10.px).fontSize(12.px))
                        .onClick(ctrl.updateFinishSide(comp.key, finish.id, side))(
                          finishSideLabel(side, lang),
                        )
                    }*
                  ),
                )
              case _ => UI.empty,

            // Parameter editor based on FinishParamSchema
            renderFinishParamEditor(ctrl, comp.key, finish, sel, limitOpt, lang),
          ),
    )

  private def renderFinishParamEditor(
      ctrl: ConfiguratorController,
      compKey: ComponentKey,
      finish: Finish,
      sel: FinishSelection,
      limitOpt: Option[BigDecimal],
      lang: Language,
  ): UI =
    finish.paramSchema match
      case FinishParamSchema.NoParams => UI.empty

      case FinishParamSchema.RoundCornersSchema(allowedCounts, minR, maxR) =>
        val (curCount, curRad) = sel.params match
          case Some(FinishParams.RoundCorners(c, r)) => (c, r)
          case _                                     => (allowedCounts.toList.sorted.lastOption.getOrElse(4), minR)
        div.style(Style.column.gap(6.px))(
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px).align(Alignment.center))(
            span(ctrl.i18n.t("cfg-param-corners")).style(smallText.fontWeight(FontWeight.w600)),
            UI.fragment(
              allowedCounts.toList.sorted.map { c =>
                button
                  .cssClass(if c == curCount then "mp-pill-active" else "mp-pill")
                  .style(Style.padding(3.px, 9.px).fontSize(12.px))
                  .onClick(ctrl.updateFinishParams(compKey, finish.id, FinishParams.RoundCorners(c, curRad)))(c.toString)
              }*
            ),
          ),
          div.style(Style.row.gap(8.px).align(Alignment.center))(
            span(ctrl.i18n.t("cfg-param-radius")).style(smallText.fontWeight(FontWeight.w600)),
            numberInput
              .cssClass("mp-input")
              .style(Style.width(84.px).padding(5.px, 8.px))
              .value(curRad.toString)
              .min(minR.toDouble)
              .max(maxR.toDouble)
              .onChange { v =>
                v.toIntOption match
                  case Some(r) if r >= minR && r <= maxR =>
                    ctrl.updateFinishParams(compKey, finish.id, FinishParams.RoundCorners(curCount, r))
                  case _ => Kyo.unit
              },
          ),
        )

      case FinishParamSchema.FoilColorSchema(allowedColors) =>
        val curColor = sel.params match
          case Some(FinishParams.FoilStamping(c)) => c
          case _                                  => allowedColors.headOption.getOrElse(FoilColor.Gold)
        div.style(Style.column.gap(4.px))(
          div.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-param-foil-color")),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
            allowedColors.toList.sortBy(_.ordinal).map { c =>
              button
                .cssClass(if c == curColor then "mp-pill-active" else "mp-pill")
                .style(Style.padding(3.px, 9.px).fontSize(12.px))
                .onClick(ctrl.updateFinishParams(compKey, finish.id, FinishParams.FoilStamping(c)))(foilColorLabel(c, lang))
            }*
          ),
        )

      case FinishParamSchema.GrommetSpacingSchema(spacings) =>
        val curSpacing = sel.params match
          case Some(FinishParams.Grommets(s)) => s
          case _                              => spacings.toList.sorted.headOption.getOrElse(500)
        div.style(Style.column.gap(4.px))(
          div.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-param-spacing")),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
            spacings.toList.sorted.map { s =>
              button
                .cssClass(if s == curSpacing then "mp-pill-active" else "mp-pill")
                .style(Style.padding(3.px, 9.px).fontSize(12.px))
                .onClick(ctrl.updateFinishParams(compKey, finish.id, FinishParams.Grommets(s)))(s"${s} mm")
            }*
          ),
        )

      case FinishParamSchema.PerforationSchema(minP, maxP) =>
        val effMax = limitOpt.map(_.toInt.min(maxP)).getOrElse(maxP)
        val curP = sel.params match
          case Some(FinishParams.Perforation(p)) => p
          case _                                 => math.max(minP, 50)
        div.style(Style.row.gap(8.px).align(Alignment.center))(
          span(ctrl.i18n.t("cfg-param-pitch")).style(smallText.fontWeight(FontWeight.w600)),
          numberInput
            .cssClass("mp-input")
            .style(Style.width(90.px).padding(5.px, 8.px))
            .value(curP.toString)
            .min(minP.toDouble)
            .max(effMax.toDouble)
            .onChange { v =>
              v.toIntOption match
                case Some(p) if p >= minP && p <= effMax =>
                  ctrl.updateFinishParams(compKey, finish.id, FinishParams.Perforation(p))
                case _ => Kyo.unit
            },
        )

      case FinishParamSchema.LinearLengthSchema(minM, maxM) =>
        val effMax = limitOpt.map(_.min(maxM)).getOrElse(maxM)
        val curM = sel.params match
          case Some(FinishParams.LinearLength(m)) => m
          case _                                  => minM.max(BigDecimal(1))
        div.style(Style.row.gap(8.px).align(Alignment.center))(
          span(ctrl.i18n.t("cfg-param-length")).style(smallText.fontWeight(FontWeight.w600)),
          numberInput
            .cssClass("mp-input")
            .style(Style.width(90.px).padding(5.px, 8.px))
            .value(curM.toString)
            .min(minM.toDouble)
            .max(effMax.toDouble)
            .step(0.5)
            .onChange { v =>
              scala.util.Try(BigDecimal(v)).toOption match
                case Some(m) if m >= minM && m <= effMax =>
                  ctrl.updateFinishParams(compKey, finish.id, FinishParams.LinearLength(m))
                case _ => Kyo.unit
            },
        )

      case FinishParamSchema.CreaseCountSchema(minC, maxC) =>
        val effMax = limitOpt.map(_.toInt.min(maxC)).getOrElse(maxC)
        val curC = sel.params match
          case Some(FinishParams.Scoring(c)) => c
          case _                             => minC
        div.style(Style.row.gap(8.px).align(Alignment.center))(
          span(ctrl.i18n.t("cfg-param-creases")).style(smallText.fontWeight(FontWeight.w600)),
          numberInput
            .cssClass("mp-input")
            .style(Style.width(84.px).padding(5.px, 8.px))
            .value(curC.toString)
            .min(minC.toDouble)
            .max(effMax.toDouble)
            .onChange { v =>
              v.toIntOption match
                case Some(c) if c >= minC && c <= effMax =>
                  ctrl.updateFinishParams(compKey, finish.id, FinishParams.Scoring(c))
                case _ => Kyo.unit
            },
          span(ctrl.i18n.t("cfg-param-max-hint", Dict("max" -> effMax.toString))).style(smallText),
        )

  // ── Step 3: Dimensions, Quantity, Versions, Orientation, Binding & Delivery ─

  private def renderSpecsCard(
      ctrl: ConfiguratorController,
      cat: ProductCategory,
      d: DraftConfig,
      lang: Language,
  ): UI =
    val specs = d.specs
    div.cssClass("mp-card")(
      h2(ctrl.i18n.t("cfg-section-specs")).style(sectionTitle.margin(0.px)),

      // Dimensions editor (archetype-aware)
      renderDimensionsEditor(ctrl, specs.dimensions, lang),

      // Quantity pills + custom input + Artwork Versions
      div.style(Style.column.gap(8.px))(
        label.style(labelStyle)(ctrl.i18n.t("cfg-quantity-label")),
        div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
          standardQuantities.map { q =>
            val active = specs.quantity.value == q
            button
              .cssClass(if active then "mp-pill-active" else "mp-pill")
              .style(Style.padding(6.px, 12.px))
              .onClick(ctrl.updateSpecs(_.copy(quantity = Quantity.unsafe(q))))(q.toString)
          }*
        ),
        div.cssClass("mp-field-row")(
          div.cssClass("mp-field")(
            label.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-quantity-label")),
            numberInput
              .cssClass("mp-input")
              .value(specs.quantity.value.toString)
              .min(1.0)
              .max(1000000.0)
              .onChange { v =>
                v.toIntOption.filter(_ >= 1) match
                  case Some(q) => ctrl.updateSpecs(_.copy(quantity = Quantity.unsafe(q)))
                  case None    => Kyo.unit
              },
          ),
          div.cssClass("mp-field")(
            label.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-versions-label")),
            numberInput
              .cssClass("mp-input")
              .value(specs.versionCount.value.toString)
              .min(1.0)
              .max(50.0)
              .onChange { v =>
                v.toIntOption.filter(n => n >= 1 && n <= 50) match
                  case Some(vc) => ctrl.updateSpecs(_.copy(versionCount = VersionCount.unsafe(vc)))
                  case None     => Kyo.unit
              },
            span(ctrl.i18n.t("cfg-versions-hint")).style(smallText),
          ),
        ),
      ),

      // Orientation (when required or present)
      if cat.specRequirements.requiresOrientation || specs.orientation.isDefined then
        val curOrient = specs.orientation.getOrElse(Orientation.Portrait)
        div.style(Style.column.gap(6.px))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-orientation-label")),
          div.style(Style.row.gap(8.px))(
            button
              .cssClass(if curOrient == Orientation.Portrait then "mp-pill-active" else "mp-pill")
              .onClick(ctrl.updateSpecs(_.copy(orientation = Some(Orientation.Portrait))))(
                ctrl.i18n.t("cfg-orientation-portrait"),
              ),
            button
              .cssClass(if curOrient == Orientation.Landscape then "mp-pill-active" else "mp-pill")
              .onClick(ctrl.updateSpecs(_.copy(orientation = Some(Orientation.Landscape))))(
                ctrl.i18n.t("cfg-orientation-landscape"),
              ),
          ),
        )
      else UI.empty,

      // Binding (when required or present)
      if cat.specRequirements.requiresBinding || specs.binding.isDefined then
        val curBind = specs.binding.getOrElse(BindingSpec(BindingMethod.SaddleStitch, 16))
        div.style(Style.column.gap(10.px).padding(12.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-binding-method")),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
            BindingMethod.values.toList.map { bm =>
              button
                .cssClass(if bm == curBind.method then "mp-pill-active" else "mp-pill")
                .onClick(ctrl.updateSpecs(_.copy(binding = Some(curBind.copy(method = bm)))))(
                  bindingMethodLabel(bm, lang),
                )
            }*
          ),
          div.cssClass("mp-field-row")(
            div.cssClass("mp-field")(
              label.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-binding-pages")),
              numberInput
                .cssClass("mp-input")
                .value(curBind.totalPages.toString)
                .min(4.0)
                .max(1000.0)
                .step(4.0)
                .onChange { v =>
                  v.toIntOption.filter(_ >= 4) match
                    case Some(p) => ctrl.updateSpecs(_.copy(binding = Some(curBind.copy(totalPages = p))))
                    case None    => Kyo.unit
                },
            ),
            div.style(Style.row.align(Alignment.center).gap(8.px).padding(18.px, 0.px, 0.px, 0.px))(
              checkbox
                .checked(curBind.selfCover)
                .onChange(checked => ctrl.updateSpecs(_.copy(binding = Some(curBind.copy(selfCover = checked))))),
              span(ctrl.i18n.t("cfg-binding-self-cover")).style(Style.fontSize(13.px).color(inkSecondary)),
            ),
          ),
        )
      else UI.empty,

      // Delivery Format
      div.style(Style.column.gap(6.px))(
        label.style(labelStyle)(ctrl.i18n.t("cfg-delivery-format")),
        div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
          List(
            DeliveryFormat.CutSingles,
            DeliveryFormat.KissCutSheets,
            DeliveryFormat.RollLabels(76, 1),
            DeliveryFormat.PackedUnits,
          ).map { df =>
            val isSel = specs.deliveryFormat == df
            button
              .cssClass(if isSel then "mp-pill-active" else "mp-pill")
              .onClick(ctrl.updateSpecs(_.copy(deliveryFormat = df)))(deliveryFormatLabel(df, lang))
          }*
        ),
      ),
    )

  private def renderDimensionsEditor(
      ctrl: ConfiguratorController,
      dims: ProductDimensions,
      lang: Language,
  ): UI =
    dims match
      case ProductDimensions.Flat2D(size) =>
        div.style(Style.column.gap(8.px))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-dimensions-label")),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
            standardSizes.map { case (lbl, dim) =>
              val active = size == dim
              button
                .cssClass(if active then "mp-pill-active" else "mp-pill")
                .style(Style.padding(5.px, 10.px).fontSize(12.px))
                .onClick(ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Flat2D(dim))))(lbl)
            }*
          ),
          div.cssClass("mp-field-row")(
            mmInputField(ctrl.i18n.t("cfg-width-mm"), size.width.value) { w =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Flat2D(Dimension2D(w, size.height))))
            },
            mmInputField(ctrl.i18n.t("cfg-height-mm"), size.height.value) { h =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Flat2D(Dimension2D(size.width, h))))
            },
          ),
        )

      case ProductDimensions.Folded2D(size, foldType) =>
        div.style(Style.column.gap(10.px))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-dimensions-label")),
          div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
            standardSizes.filter(e => e._1.startsWith("A") || e._1 == "DL").map { case (lbl, dim) =>
              val active = size == dim
              button
                .cssClass(if active then "mp-pill-active" else "mp-pill")
                .style(Style.padding(5.px, 10.px).fontSize(12.px))
                .onClick(ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Folded2D(dim, foldType))))(lbl)
            }*
          ),
          div.cssClass("mp-field-row")(
            mmInputField(ctrl.i18n.t("cfg-width-mm"), size.width.value) { w =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Folded2D(Dimension2D(w, size.height), foldType)))
            },
            mmInputField(ctrl.i18n.t("cfg-height-mm"), size.height.value) { h =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Folded2D(Dimension2D(size.width, h), foldType)))
            },
          ),
          div.style(Style.column.gap(6.px))(
            label.style(smallText.fontWeight(FontWeight.w600))(ctrl.i18n.t("cfg-fold-type")),
            div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
              FoldType.values.toList.map { ft =>
                button
                  .cssClass(if ft == foldType then "mp-pill-active" else "mp-pill")
                  .style(Style.padding(5.px, 10.px).fontSize(12.px))
                  .onClick(ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Folded2D(size, ft))))(foldTypeLabel(ft, lang))
              }*
            ),
          ),
        )

      case ProductDimensions.Box3D(box, customBlank) =>
        div.style(Style.column.gap(8.px))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-dimensions-label")),
          div.cssClass("mp-field-row")(
            mmInputField(ctrl.i18n.t("cfg-length-mm"), box.length.value) { l =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Box3D(box.copy(length = l), customBlank)))
            },
            mmInputField(ctrl.i18n.t("cfg-width-mm"), box.width.value) { w =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Box3D(box.copy(width = w), customBlank)))
            },
            mmInputField(ctrl.i18n.t("cfg-height-mm"), box.height.value) { h =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Box3D(box.copy(height = h), customBlank)))
            },
          ),
        )

      case ProductDimensions.Circular(diameter) =>
        div.style(Style.column.gap(8.px))(
          label.style(labelStyle)(ctrl.i18n.t("cfg-dimensions-label")),
          div.cssClass("mp-field-row")(
            mmInputField(ctrl.i18n.t("cfg-diameter-mm"), diameter.value) { d =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.Circular(d)))
            },
          ),
        )

      case ProductDimensions.PrintArea(area, variantOpt) =>
        div.style(Style.column.gap(8.px))(
          div.style(rowBetween)(
            label.style(labelStyle)(ctrl.i18n.t("cfg-dimensions-label")),
            variantOpt.map(v => badge(v, BadgeTone.Brand)).getOrElse(UI.empty),
          ),
          div.cssClass("mp-field-row")(
            mmInputField(ctrl.i18n.t("cfg-width-mm"), area.width.value) { w =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.PrintArea(Dimension2D(w, area.height), variantOpt)))
            },
            mmInputField(ctrl.i18n.t("cfg-height-mm"), area.height.value) { h =>
              ctrl.updateSpecs(_.copy(dimensions = ProductDimensions.PrintArea(Dimension2D(area.width, h), variantOpt)))
            },
          ),
        )

  private def mmInputField(lbl: Signal[String], value: BigDecimal)(onValid: PositiveMm => Unit < Async): UI =
    div.cssClass("mp-field")(
      label.style(smallText.fontWeight(FontWeight.w600))(lbl),
      numberInput
        .cssClass("mp-input")
        .value(value.bigDecimal.stripTrailingZeros.toPlainString)
        .min(10.0)
        .max(10000.0)
        .onChange { raw =>
          scala.util.Try(BigDecimal(raw)).toOption match
            case Some(bd) if bd > BigDecimal(0) =>
              PositiveMm(bd).toOption match
                case Some(mm) => onValid(mm)
                case None     => Kyo.unit
            case _ => Kyo.unit
        },
    )

  // ── Step 4: Production Speed Tiers ─────────────────────────────────────────

  private def renderSpeedCard(
      ctrl: ConfiguratorController,
      d: DraftConfig,
      host: ConfiguratorHost,
      lang: Language,
  ): UI =
    val isCalculator = host match
      case _: ConfiguratorHost.Calculator => true
      case _                              => false

    div.cssClass("mp-card")(
      h2(ctrl.i18n.t("cfg-section-speed")).style(sectionTitle.margin(0.px)),
      ctrl.quoteStatus.render { status =>
        val options: List[SpeedTierOption] = status match
          case QuoteStatus.Ready(res) if res.speedOptions.nonEmpty => res.speedOptions
          case _ =>
            List(
              SpeedTierOption(ManufacturingSpeed.Economy, true, None, BigDecimal("0.85"), BigDecimal("0.85"), BigDecimal(-15)),
              SpeedTierOption(ManufacturingSpeed.Standard, true, None, BigDecimal("1.00"), BigDecimal("1.00"), BigDecimal(0)),
              SpeedTierOption(ManufacturingSpeed.Express, true, None, BigDecimal("1.35"), BigDecimal("1.35"), BigDecimal(35)),
            )

        div.cssClass("mp-option-grid")(
          options.map { opt =>
            val isSel = d.speed == opt.speed
            val cls   = if isSel then "mp-option-card-active" else "mp-option-card"
            val pct   = opt.surchargePercent.setScale(0, BigDecimal.RoundingMode.HALF_UP)
            val pctBadge =
              if !opt.available then badge(if lang == Language.Cs then "Nedostupné" else "Unavailable", BadgeTone.Rose)
              else if pct > 0 then badge(s"+$pct%", BadgeTone.Amber)
              else if pct < 0 then badge(s"$pct%", BadgeTone.Emerald)
              else badge("0%", BadgeTone.Neutral)

            val descKey = opt.speed match
              case ManufacturingSpeed.Express  => if isCalculator then "calc-speed-express-range" else "cfg-speed-express-desc"
              case ManufacturingSpeed.Standard => if isCalculator then "calc-speed-standard-range" else "cfg-speed-standard-desc"
              case ManufacturingSpeed.Economy  => if isCalculator then "calc-speed-economy-range" else "cfg-speed-economy-desc"

            button
              .cssClass(cls)
              .disabled(!opt.available)
              .onClick(ctrl.updateSpeed(opt.speed))(
                div.style(rowBetween)(
                  span(speedLabel(opt.speed, lang)).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                  pctBadge,
                ),
                div.style(smallText)(ctrl.i18n.t(descKey)),
                opt.unavailableReason
                  .map(r => div.style(Style.fontSize(12.px).color(roseInk))(r))
                  .getOrElse(UI.empty),
              )
          }*
        )
      },
    )

  // ── Step 5: Print Artwork Upload (Storefront Mode) ─────────────────────────

  private def renderArtworkCard(
      ctrl: ConfiguratorController,
      cat: ProductCategory,
      d: DraftConfig,
      sf: ConfiguratorHost.Storefront,
      lang: Language,
  ): UI =
    val isCs          = lang == Language.Cs
    val expectedTrim  = d.specs.dimensions.finishedBoundingBox
    val printableComps = d.components.filter(c => cat.templateFor(c.key).forall(_.printable))

    if printableComps.isEmpty then UI.empty
    else
      div.cssClass("mp-card")(
        div.style(rowBetween.flexWrap(FlexWrap.wrap))(
          div.style(stackXs)(
            h2(ctrl.i18n.t("cfg-section-artwork")).style(sectionTitle.margin(0.px)),
            p(ctrl.i18n.t("cfg-artwork-subtitle")).style(smallText.margin(0.px)),
          ),
          if d.artworks.nonEmpty then
            reactiveBadge(ctrl.i18n.t("cfg-artwork-attached-badge", Dict("count" -> d.artworks.size.toString)), BadgeTone.Emerald)
          else
            badge(if isCs then "Volitelné" else "Optional", BadgeTone.Neutral),
        ),
        div.style(stackMd)(
          printableComps.map { comp =>
            val compLabel = cat.templateFor(comp.key).map(_.label(lang)).getOrElse(comp.key.value)
            val zoneId    = s"cfg-art-${d.categoryId.value}-${comp.key.value}"
            val onPick: BrowserPickedFile => Unit < Async = file =>
              ctrl.uploadDraftArtwork(comp.key, file).flatMap {
                case Some(_) => sf.onArtworkUploaded()
                case None    => Kyo.unit
              }

            div.style(stackSm.padding(12.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
              if printableComps.size > 1 then
                div.style(rowBetween)(
                  span(compLabel).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                  badge(formatDimension(expectedTrim), BadgeTone.Neutral),
                )
              else UI.empty,

              d.artworks.get(comp.key) match
                case Some(art) =>
                  val rev = art.latestRevision
                  val issues = rev.preflight.toList.flatMap(_.checks).collect {
                    case ch if ch.outcome.isInstanceOf[CheckOutcome.Failed]  => ("rose", ch.kind.toString, ch.outcome.asInstanceOf[CheckOutcome.Failed].detail)
                    case ch if ch.outcome.isInstanceOf[CheckOutcome.Warning] => ("amber", ch.kind.toString, ch.outcome.asInstanceOf[CheckOutcome.Warning].detail)
                  }
                  div.style(stackSm)(
                    div.style(
                      Style.row
                        .align(Alignment.center)
                        .justify(Justification.spaceBetween)
                        .gap(12.px)
                        .flexWrap(FlexWrap.wrap)
                        .padding(10.px, 12.px)
                        .rounded(10.px)
                        .bg(surface)
                        .border(1.px, Palette.border),
                    )(
                      div.style(Style.row.align(Alignment.center).gap(12.px).minWidth(0.px).flexGrow(1.0))(
                        ArtworkComponents.renderThumbnail(
                          rev,
                          sizePx = 56,
                          onClick = Some(() => sf.onPreviewArtwork(art, Some(s"${cat.name(lang)} · $compLabel"))),
                        ),
                        div.style(stackXs.minWidth(0.px))(
                          div.style(rowXs.flexWrap(FlexWrap.wrap))(
                            span(rev.file.fileName.value).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                            artworkStatusBadge(art.status, lang),
                          ),
                          div.style(rowXs.flexWrap(FlexWrap.wrap))(
                            span(ArtworkComponents.formatFileSize(rev.file.sizeBytes)).style(smallText),
                            badge(s"${rev.extractedSpecs.resolutionDpi} DPI", BadgeTone.Neutral),
                            badge(rev.extractedSpecs.colorSpace.toString, BadgeTone.Brand),
                            badge(s"${rev.extractedSpecs.bleed.value} mm ${if isCs then "spad." else "bleed"}", BadgeTone.Neutral),
                          ),
                        ),
                      ),
                      div.style(rowXs.flexWrap(FlexWrap.wrap))(
                        button
                          .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                          .onClick(sf.onPreviewArtwork(art, Some(s"${cat.name(lang)} · $compLabel")))(
                            ctrl.i18n.t("cfg-artwork-preview"),
                          ),
                        ArtworkComponents.downloadButton(rev, lang),
                        ArtworkComponents.copyLinkButton(rev, lang, onCopied = () => sf.onLinkCopied(rev.file.shareableUrl)),
                        button
                          .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                          .onClick(ctrl.removeDraftArtwork(comp.key).andThen(sf.onArtworkRemoved()))(
                            ctrl.i18n.t("cfg-artwork-remove"),
                          ),
                      ),
                    ),
                    if issues.nonEmpty then
                      div.style(stackXs)(
                        issues.map { case (tone, kind, msg) =>
                          div.style(
                            Style
                              .padding(6.px, 10.px)
                              .rounded(8.px)
                              .bg(if tone == "rose" then roseSoft else amberSoft)
                              .color(if tone == "rose" then roseInk else amberInk)
                              .border(1.px, if tone == "rose" then roseBorder else amberBorder)
                              .fontSize(12.px),
                          )(s"⚠ $kind: $msg")
                        }*
                      )
                    else UI.empty,
                    ArtworkComponents.dropZone(
                      zoneId = zoneId,
                      expectedTrim = expectedTrim,
                      componentKey = comp.key,
                      lang = lang,
                      onFilePicked = onPick,
                      titleOpt = Some(if isCs then "Přetáhněte jiný soubor pro nahrazení" else "Drop another file to replace"),
                      compact = true,
                      showDemoQuickAttach = true,
                    ),
                  )

                case None =>
                  ArtworkComponents.dropZone(
                    zoneId = zoneId,
                    expectedTrim = expectedTrim,
                    componentKey = comp.key,
                    lang = lang,
                    onFilePicked = onPick,
                    compact = false,
                    showDemoQuickAttach = true,
                  ),
            )
          }*
        ),
      )

  // ── Sticky Right Sidebar: Live Quote, B2B Savings, Breakdown & CTAs ────────

  private def renderQuoteSidebar(
      ctrl: ConfiguratorController,
      status: QuoteStatus,
      d: DraftConfig,
      currency: Currency,
      lang: Language,
      host: ConfiguratorHost,
  ): UI =
    div.cssClass("mp-card").style(Style.gap(16.px))(
      div.style(rowBetween)(
        h2(ctrl.i18n.t("cfg-quote-title")).style(sectionTitle.margin(0.px)),
        badge(currency.toString, BadgeTone.Neutral),
      ),
      status match
        case QuoteStatus.Loading =>
          div.style(Style.padding(24.px, 0.px).textAlign(TextAlign.center).color(inkMuted))(
            ctrl.i18n.t("cfg-quote-calculating"),
          )

        case QuoteStatus.Failed(err) =>
          val messages = err match
            case StorefrontError.CatalogFailure(CatalogError.ConfigurationValidationFailed(errs)) =>
              errs.toList.map(_.message)
            case other => List(other.message)
          alertBanner(BadgeTone.Rose, span(ctrl.i18n.t("cfg-errors-title")), messages)

        case QuoteStatus.Ready(res) =>
          val q      = res.pricing.effectiveQuote
          val unitNet = Money((q.netTotal.value / BigDecimal(q.quantity.value.max(1)))).rounded
          val vatPct = q.tax.rate.value.bigDecimal.stripTrailingZeros.toPlainString

          div.style(Style.column.gap(14.px))(
            // Headline price block
            div.style(Style.column.gap(6.px).padding(16.px).rounded(14.px).bg(surfaceAlt).border(1.px, Palette.border))(
              // Optional walk-in strikethrough for B2B
              res.pricing.walkInBaseQuote match
                case Some(walkIn) =>
                  div.style(rowBetween)(
                    span(ctrl.i18n.t("cfg-quote-walkin")).style(smallText),
                    span(formatMoney(walkIn.grossTotal, currency, lang)).style(smallText.strikethrough),
                  )
                case None => UI.empty,
              div.style(rowBetween)(
                span(ctrl.i18n.t("cfg-quote-gross")).style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(inkSecondary)),
                span(formatMoney(q.grossTotal, currency, lang)).style(Style.fontSize(26.px).fontWeight(FontWeight.w800).color(ink)),
              ),
              div.style(rowBetween)(
                span(ctrl.i18n.t("cfg-quote-net")).style(smallText),
                span(formatMoney(q.netTotal, currency, lang)).style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(inkSecondary)),
              ),
              div.style(rowBetween)(
                span(ctrl.i18n.t("cfg-quote-vat", Dict("rate" -> vatPct))).style(smallText),
                span(formatMoney(q.tax.taxAmount, currency, lang)).style(smallText),
              ),
              div.style(rowBetween)(
                span(ctrl.i18n.t("cfg-quote-unit")).style(smallText),
                span(formatMoney(unitNet, currency, lang)).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(brand)),
              ),
            ),

            // B2B Savings Banner
            (res.pricing.savingsNet, res.pricing.savingsPercent) match
              case (Some(sav), Some(pct)) if sav.value > BigDecimal(0) =>
                div.style(
                  Style.padding(10.px, 12.px)
                    .rounded(10.px)
                    .bg(emeraldSoft)
                    .border(1.px, emeraldBorder)
                    .color(emeraldInk)
                    .fontSize(13.px)
                    .fontWeight(FontWeight.w600),
                )(
                  ctrl.i18n.t(
                    "cfg-quote-savings",
                    Dict(
                      "amount" -> formatMoney(sav, currency, lang),
                      "pct"    -> pct.setScale(1, BigDecimal.RoundingMode.HALF_UP).toString,
                    ),
                  ),
                )
              case _ => UI.empty,

            // Volume multiplier & attached artwork badges
            if q.volumeMultiplier != BigDecimal(1) || d.artworks.nonEmpty then
              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
                if q.volumeMultiplier != BigDecimal(1) then
                  badge(
                    s"${if lang == Language.Cs then "Množstevní koef." else "Volume mult."}: ${q.volumeMultiplier}×",
                    BadgeTone.Brand,
                  )
                else UI.empty,
                if d.artworks.nonEmpty then
                  reactiveBadge(
                    ctrl.i18n.t("cfg-artwork-attached-badge", Dict("count" -> d.artworks.size.toString)),
                    BadgeTone.Emerald,
                  )
                else UI.empty,
              )
            else UI.empty,

            // Host-specific CTA buttons
            host match
              case ConfiguratorHost.Storefront(onAddToCart, onQuickOrder, _, _, _, _) =>
                div.style(Style.column.gap(8.px))(
                  button
                    .cssClass("mp-btn mp-btn-primary mp-btn-lg")
                    .onClick(onAddToCart(d, res))(Icons(Icons.Icon.Cart, 18), span(ctrl.i18n.t("cfg-btn-add-cart"))),
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-lg")
                    .onClick(onQuickOrder(d, res))(Icons(Icons.Icon.Zap, 17), span(ctrl.i18n.t("cfg-btn-quick-order"))),
                )
              case ConfiguratorHost.Calculator(_, onRequestEmailOrder) =>
                button
                  .cssClass("mp-btn mp-btn-primary mp-btn-lg")
                  .onClick(onRequestEmailOrder(d, res))(Icons(Icons.Icon.Mail, 17), span(ctrl.i18n.t("cfg-btn-email-order"))),

            // Prepress advisories / warnings
            if res.configuration.warnings.nonEmpty then
              alertBanner(
                BadgeTone.Amber,
                span(ctrl.i18n.t("cfg-warnings-title")),
                res.configuration.warnings.map(_.reason(lang)),
              )
            else UI.empty,

            // Press imposition & weight details
            renderImpositionDetails(ctrl, res.configuration.product, lang),

            // Localized breakdown
            if res.localizedBreakdown.nonEmpty then
              div.style(Style.column.gap(6.px).borderTop(1.px, Palette.border).padding(12.px, 0.px, 0.px, 0.px))(
                div.style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(inkMuted).letterSpacing(0.3.px))(
                  ctrl.i18n.t("cfg-quote-breakdown"),
                ),
                div.style(Style.column.gap(4.px))(
                  res.localizedBreakdown.map { line =>
                    div.style(rowBetween.fontSize(12.px))(
                      span(line.label).style(Style.color(inkSecondary)),
                      span(formatMoney(line.total, currency, lang)).style(Style.fontWeight(FontWeight.w600).color(ink)),
                    )
                  }*
                ),
              )
            else UI.empty,
          ),
    )

  private def renderImpositionDetails(
      ctrl: ConfiguratorController,
      prod: ConfiguredProduct,
      lang: Language,
  ): UI =
    div.style(Style.column.gap(6.px).borderTop(1.px, Palette.border).padding(12.px, 0.px, 0.px, 0.px))(
      div.style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(inkMuted).letterSpacing(0.3.px))(
        ctrl.i18n.t("cfg-quote-imposition"),
      ),
      prod.weight match
        case Some(w) =>
          div.style(rowBetween.fontSize(12.px))(
            span(ctrl.i18n.t("cfg-quote-weight")).style(Style.color(inkSecondary)),
            span(formatWeight(w.totalWeight.value)).style(Style.fontWeight(FontWeight.w600).color(ink)),
          )
        case None => UI.empty,
      UI.fragment(
        prod.components.toList.map { c =>
          val summary = c.geometry.imposition match
            case ImpositionResult.SheetImposition(_, sheetSz, pcs, sheets, _, _) =>
              s"$sheets × ${formatDimension(sheetSz)} ($pcs/sh)"
            case ImpositionResult.RollImposition(_, rollW, _, linM, sqM) =>
              s"${linM.setScale(2, BigDecimal.RoundingMode.HALF_UP)} m (${sqM.value.setScale(2, BigDecimal.RoundingMode.HALF_UP)} m², ${rollW.value.intValue}mm)"
            case ImpositionResult.DiscreteUnit(units) =>
              s"$units pcs"
          div.style(rowBetween.fontSize(12.px))(
            span(c.key.value).style(Style.color(inkSecondary)),
            span(summary).style(Style.fontWeight(FontWeight.w600).color(ink)),
          )
        }*
      ),
    )
