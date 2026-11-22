package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.backoffice.{MarginSimulationCmd, MarginSimulationResult}
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Pricing, Promo Codes, B2B Agreements, Pricelists, Cost Sheets & Interactive Live Margin Simulator. */
object PricingManagerView:

  private enum Tab:
    case Simulator, Promos, Agreements, Pricelists, CostSheets

  def render(state: BackofficeState): UI < Sync =
    for
      tabRef        <- Signal.initRef[Tab](Tab.Simulator)
      // Margin Simulator refs
      simCatRef     <- Signal.initRef("")
      simPresetRef  <- Signal.initRef("")
      simQtyRef     <- Signal.initRef("500")
      simCurRef     <- Signal.initRef[Currency](Currency.CZK)
      simSpeedRef   <- Signal.initRef[ManufacturingSpeed](ManufacturingSpeed.Standard)
      simTierRef    <- Signal.initRef("None")
      simPromoRef   <- Signal.initRef("")
      simResultRef  <- Signal.initRef[Option[MarginSimulationResult]](None)
      // Promo form refs
      promoCodeRef  <- Signal.initRef("")
      promoKindRef  <- Signal.initRef("percent")
      promoValRef   <- Signal.initRef("15")
      promoMaxRef   <- Signal.initRef("")
      // Agreement form refs
      agrTierRef    <- Signal.initRef[CustomerTier](CustomerTier.Gold)
      agrCurRef     <- Signal.initRef[Currency](Currency.CZK)
      agrDiscRef    <- Signal.initRef("12")
      // Cost sheet overhead edit ref
      overheadRef   <- Signal.initRef("1.15")
    yield
      val combined =
        tabRef
          .combineLatest(state.catalog)
          .combineLatest(state.pricelists)
          .combineLatest(state.agreements)
          .combineLatest(state.promoCodes)
          .combineLatest(state.costSheets)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      combined.render { case (((((((activeTab, catOpt), pls), agrs), promos), costs), authOpt), lang) =>
        val canManage = state.canManage(authOpt)

        div.style(stackMd.width(100.pct))(
          div.style(stackXs)(
            h1(state.i18n.t("adm-pri-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
            p(state.i18n.t("adm-pri-subtitle")).style(mutedText.margin(0.px)),
          ),

          if !canManage && activeTab != Tab.Simulator then
            alertBanner(BadgeTone.Amber, span("Read-Only"), List(if lang == Language.Cs then "Úpravy cen a slev vyžadují roli Manager nebo Admin." else "Pricing & promo mutations require Manager or Admin role."))
          else UI.empty,

          // Tabs
          div.style(rowWrap)(
            tabBtn(Tab.Simulator, activeTab, tabRef, state.i18n.t("adm-pri-tab-simulator"), None),
            tabBtn(Tab.Promos, activeTab, tabRef, state.i18n.t("adm-pri-tab-promos"), Some(promos.size)),
            tabBtn(Tab.Agreements, activeTab, tabRef, state.i18n.t("adm-pri-tab-agreements"), Some(agrs.size)),
            tabBtn(Tab.Pricelists, activeTab, tabRef, state.i18n.t("adm-pri-tab-pricelists"), Some(pls.size)),
            tabBtn(Tab.CostSheets, activeTab, tabRef, state.i18n.t("adm-pri-tab-costs"), Some(costs.size)),
          ),

          activeTab match
            case Tab.Simulator =>
              renderSimulator(state, catOpt, simCatRef, simPresetRef, simQtyRef, simCurRef, simSpeedRef, simTierRef, simPromoRef, simResultRef, lang)

            case Tab.Promos =>
              renderPromos(state, promos, canManage, promoCodeRef, promoKindRef, promoValRef, promoMaxRef, lang)

            case Tab.Agreements =>
              renderAgreements(state, agrs, canManage, agrTierRef, agrCurRef, agrDiscRef)

            case Tab.Pricelists =>
              renderPricelists(state, pls, canManage, lang)

            case Tab.CostSheets =>
              renderCostSheets(state, costs, canManage, overheadRef)
        )
      }

  private def renderSimulator(
      state: BackofficeState,
      catOpt: Option[mpbuilder.catalog.ProductCatalog],
      simCatRef: SignalRef[String],
      simPresetRef: SignalRef[String],
      simQtyRef: SignalRef[String],
      simCurRef: SignalRef[Currency],
      simSpeedRef: SignalRef[ManufacturingSpeed],
      simTierRef: SignalRef[String],
      simPromoRef: SignalRef[String],
      simResultRef: SignalRef[Option[MarginSimulationResult]],
      lang: Language,
  ): UI =
    catOpt match
      case None => div.cssClass("mp-card")(span("Loading catalog...").style(mutedText))
      case Some(cat) =>
        val catsWithPresets = cat.categories.values.toList.filter(_.presets.nonEmpty).sortBy(_.id.value)
        val simSignal =
          simCatRef
            .combineLatest(simPresetRef)
            .combineLatest(simCurRef)
            .combineLatest(simSpeedRef)
            .combineLatest(simTierRef)
            .combineLatest(simResultRef)

        simSignal.render { case (((((selCatRaw, selPresetRaw), cur), speed), tierStr), resOpt) =>
          val activeCat = catsWithPresets.find(_.id.value == selCatRaw).orElse(catsWithPresets.headOption)
          val presets   = activeCat.map(_.presets).getOrElse(Nil)
          val activePre = presets.find(_.id.value == selPresetRaw).orElse(presets.headOption)

          def runSim: Unit < Async =
            (activeCat, activePre) match
              case (Some(c), Some(p)) =>
                for
                  qtyS  <- simQtyRef.get.map(_.trim)
                  promo <- simPromoRef.get.map(_.trim)
                  qty    = Quantity.unsafe(qtyS.toIntOption.filter(_ > 0).getOrElse(500))
                  tier   = CustomerTier.values.find(_.toString == tierStr)
                  cmd = MarginSimulationCmd(
                          categoryId = c.id,
                          presetId = p.id,
                          quantity = qty,
                          currency = cur,
                          speed = speed,
                          customerTier = tier,
                          promoCode = if promo.nonEmpty then Some(promo) else None,
                        )
                  res <- state.simulateMargin(cmd)
                  _   <- simResultRef.set(res)
                yield ()
              case _ => Kyo.unit

          div.style(stackMd)(
            div.cssClass("mp-card")(
              h2(state.i18n.t("adm-pri-tab-simulator")).style(sectionTitle.margin(0.px)),
              div.cssClass("mp-field-row")(
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Kategorie" else "Category").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(activeCat.map(_.id.value).getOrElse(""))
                    .onChange { v =>
                      val firstPre = catsWithPresets.find(_.id.value == v).flatMap(_.presets.headOption).map(_.id.value).getOrElse("")
                      simCatRef.set(v).andThen(simPresetRef.set(firstPre))
                    }(
                      catsWithPresets.map(c => option.value(c.id.value)(c.name(lang)))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Šablona" else "Preset").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(activePre.map(_.id.value).getOrElse(""))
                    .onChange(v => simPresetRef.set(v))(
                      presets.map(p => option.value(p.id.value)(p.name(lang)))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Množství (ks)" else "Quantity (pcs)").style(labelStyle),
                  numberInput.cssClass("mp-input").value(simQtyRef).onInput(v => simQtyRef.set(v)),
                ),
              ),
              div.cssClass("mp-field-row")(
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Měna" else "Currency").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(cur.toString)
                    .onChange(v => Currency.values.find(_.toString == v).map(simCurRef.set).getOrElse(Kyo.unit))(
                      List(Currency.CZK, Currency.USD).map(c => option.value(c.toString)(c.toString))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Rychlost výroby" else "Speed").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(speed.toString)
                    .onChange(v => ManufacturingSpeed.values.find(_.toString == v).map(simSpeedRef.set).getOrElse(Kyo.unit))(
                      ManufacturingSpeed.values.toList.map(s => option.value(s.toString)(speedLabel(s, lang)))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Cenová hladina" else "Customer Tier").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(tierStr)
                    .onChange(v => simTierRef.set(v))(
                      (None :: CustomerTier.values.toList.map(Some(_))).map {
                        case None    => option.value("None")(if lang == Language.Cs then "Veřejná (Walk-in)" else "Public (Walk-in)")
                        case Some(t) => option.value(t.toString)(t.toString)
                      }*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Slevový kód (volitelně)" else "Promo Code (optional)").style(labelStyle),
                  input.cssClass("mp-input").placeholder("SAVE10").value(simPromoRef).onInput(v => simPromoRef.set(v)),
                ),
              ),
              div.style(rowSm)(
                button.cssClass("mp-btn mp-btn-primary").onClick(runSim)(state.i18n.t("adm-pri-sim-run")),
              ),
            ),

            // Simulation Result Card
            resOpt match
              case None => UI.empty
              case Some(res) =>
                val marginTone =
                  if res.margin.isBelowCost then BadgeTone.Rose
                  else if res.margin.warnings.nonEmpty then BadgeTone.Amber
                  else BadgeTone.Emerald
                val marginPct = res.margin.marginPercent.setScale(1, BigDecimal.RoundingMode.HALF_UP)

                div.cssClass("mp-card")(
                  div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                    h2(s"${activeCat.map(_.name(lang)).getOrElse("")} — ${res.quote.quantity.value} pcs").style(sectionTitle.margin(0.px)),
                    badge(s"Margin $marginPct%", marginTone),
                  ),
                  div.cssClass("mp-kpi-grid")(
                    statBox(state.i18n.t("adm-pri-sim-net"), formatMoney(res.quote.netTotal, res.quote.currency, lang), ink),
                    statBox(state.i18n.t("adm-pri-sim-gross"), formatMoney(res.quote.grossTotal, res.quote.currency, lang), brand),
                    statBox(state.i18n.t("adm-pri-sim-cost"), formatMoney(res.margin.productionCostNet, res.quote.currency, lang), inkSecondary),
                    statBox(
                      state.i18n.t("adm-pri-sim-margin"),
                      s"${formatMoney(res.margin.marginNet, res.quote.currency, lang)} ($marginPct%)",
                      if res.margin.isBelowCost then rose else emerald,
                    ),
                    statBox(state.i18n.t("adm-pri-sim-sheets"), res.sheetsUsedTotal.toString, ink),
                  ),
                  if res.margin.warnings.isEmpty then
                    alertBanner(BadgeTone.Emerald, state.i18n.t("adm-pri-sim-warnings"), List(if lang == Language.Cs then "Zdravá marže — nad výrobními náklady i prahem." else "Healthy margin — above production cost and threshold."))
                  else
                    alertBanner(
                      if res.margin.isBelowCost then BadgeTone.Rose else BadgeTone.Amber,
                      state.i18n.t("adm-pri-sim-warnings"),
                      res.margin.warnings.map(_.toString),
                    ),
                ),
          )
        }

  private def renderPromos(
      state: BackofficeState,
      promos: List[PromoCode],
      canManage: Boolean,
      codeRef: SignalRef[String],
      kindRef: SignalRef[String],
      valRef: SignalRef[String],
      maxRef: SignalRef[String],
      lang: Language,
  ): UI =
    div.style(stackMd)(
      if canManage then
        div.cssClass("mp-card")(
          h2(state.i18n.t("adm-pri-promo-create")).style(sectionTitle.margin(0.px)),
          div.cssClass("mp-field-row")(
            div.cssClass("mp-field")(
              label("Code").style(labelStyle),
              input.cssClass("mp-input").placeholder("SPRING20").value(codeRef).onInput(v => codeRef.set(v)),
            ),
            div.cssClass("mp-field")(
              label("Benefit").style(labelStyle),
              kindRef.render { k =>
                select.cssClass("mp-input").value(k).onChange(v => kindRef.set(v))(
                  option.value("percent")("Percentage Off (%)"),
                  option.value("fixed")("Fixed Amount (CZK)"),
                  option.value("freeDelivery")("Free Delivery"),
                )
              },
            ),
            div.cssClass("mp-field")(
              label("Value").style(labelStyle),
              numberInput.cssClass("mp-input").value(valRef).onInput(v => valRef.set(v)),
            ),
            div.cssClass("mp-field")(
              label("Max Uses (optional)").style(labelStyle),
              numberInput.cssClass("mp-input").placeholder("Unlimited").value(maxRef).onInput(v => maxRef.set(v)),
            ),
          ),
          div.style(rowSm)(
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .onClick {
                for
                  rawCode <- codeRef.get.map(_.trim.toUpperCase)
                  kind    <- kindRef.get
                  vStr    <- valRef.get.map(_.trim)
                  mStr    <- maxRef.get.map(_.trim)
                  _ <-
                    if rawCode.isEmpty then state.errorBanner.set(Some("Promo code must not be empty."))
                    else
                      val num = scala.util.Try(BigDecimal(vStr)).getOrElse(BigDecimal(10))
                      val benefit = kind match
                        case "fixed"        => PromoBenefit.FixedAmountOff(Price(Money(num), Currency.CZK))
                        case "freeDelivery" => PromoBenefit.FreeDelivery
                        case _              => PromoBenefit.PercentageOff(Percentage.unsafe(num.min(BigDecimal(90)).max(BigDecimal(1))))
                      val promo = PromoCode(
                        id = DiscountCodeId.unsafe(s"promo-${rawCode.toLowerCase}"),
                        code = NonEmptyString.unsafe(rawCode),
                        benefit = benefit,
                        constraints = PromoConstraints(maxUses = mStr.toIntOption.filter(_ > 0)),
                        isActive = true,
                      )
                      state.savePromoCode(promo).andThen(codeRef.set(""))
                yield ()
              }(state.i18n.t("adm-pri-promo-create")),
          ),
        )
      else UI.empty,

      div.cssClass("mp-card")(
        div.style(stackSm)(
          promos.sortBy(_.code.value).map { p =>
            val benefitLabel = p.benefit match
              case PromoBenefit.PercentageOff(pct) => s"${pct.value}% OFF"
              case PromoBenefit.FixedAmountOff(pr) => s"−${formatMoney(pr.amount, pr.currency, lang)}"
              case PromoBenefit.FreeDelivery       => "Free Delivery"
            val usage = s"${p.constraints.currentUses} / ${p.constraints.maxUses.map(_.toString).getOrElse("∞")}"
            div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
              div.style(rowSm.flexWrap(FlexWrap.wrap))(
                span(p.code.value).style(Style.fontWeight(FontWeight.w800).color(ink)),
                badge(benefitLabel, BadgeTone.Brand),
                badge(if p.isActive then "Active" else "Paused", if p.isActive then BadgeTone.Emerald else BadgeTone.Amber),
                span(s"Uses: $usage").style(smallText),
              ),
              if canManage then
                div.style(rowXs)(
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(state.togglePromoActive(p.id, !p.isActive))(
                      if p.isActive then state.i18n.t("adm-pri-promo-deactivate") else state.i18n.t("adm-pri-promo-activate")
                    ),
                  button
                    .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                    .onClick(state.deletePromoCode(p.id))(state.i18n.t("adm-cat-delete")),
                )
              else UI.empty,
            )
          }*
        ),
      ),
    )

  private def renderAgreements(
      state: BackofficeState,
      agreements: List[CustomerPriceAgreement],
      canManage: Boolean,
      tierRef: SignalRef[CustomerTier],
      curRef: SignalRef[Currency],
      discRef: SignalRef[String],
  ): UI =
    div.style(stackMd)(
      if canManage then
        div.cssClass("mp-card")(
          h2(state.i18n.t("adm-pri-agr-create")).style(sectionTitle.margin(0.px)),
          div.cssClass("mp-field-row")(
            div.cssClass("mp-field")(
              label("Target Tier").style(labelStyle),
              tierRef.render { t =>
                select.cssClass("mp-input").value(t.toString).onChange(v => CustomerTier.values.find(_.toString == v).map(tierRef.set).getOrElse(Kyo.unit))(
                  CustomerTier.values.toList.map(x => option.value(x.toString)(x.toString))*
                )
              },
            ),
            div.cssClass("mp-field")(
              label("Currency").style(labelStyle),
              curRef.render { c =>
                select.cssClass("mp-input").value(c.toString).onChange(v => Currency.values.find(_.toString == v).map(curRef.set).getOrElse(Kyo.unit))(
                  List(Currency.CZK, Currency.USD).map(x => option.value(x.toString)(x.toString))*
                )
              },
            ),
            div.cssClass("mp-field")(
              label("Global Discount (%)").style(labelStyle),
              numberInput.cssClass("mp-input").value(discRef).onInput(v => discRef.set(v)),
            ),
          ),
          div.style(rowSm)(
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .onClick {
                for
                  tier <- tierRef.get
                  cur  <- curRef.get
                  dStr <- discRef.get.map(_.trim)
                  pct   = scala.util.Try(BigDecimal(dStr)).getOrElse(BigDecimal(10)).min(BigDecimal(80)).max(BigDecimal(0))
                  agr   = CustomerPriceAgreement(
                            target = AgreementTarget.TierDefault(tier),
                            currency = cur,
                            globalDiscount = Some(Percentage.unsafe(pct)),
                          )
                  _ <- state.saveAgreement(agr)
                yield ()
              }(state.i18n.t("adm-pri-agr-create")),
          ),
        )
      else UI.empty,

      div.cssClass("mp-card")(
        div.style(stackSm)(
          agreements.map { a =>
            val targetStr = a.target match
              case AgreementTarget.TierDefault(t)       => s"Tier: $t"
              case AgreementTarget.SpecificCustomer(id) => s"Customer: ${id.value}"
            div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
              div.style(rowSm.flexWrap(FlexWrap.wrap))(
                span(targetStr).style(Style.fontWeight(FontWeight.w700).color(ink)),
                badge(a.currency.toString, BadgeTone.Neutral),
                a.globalDiscount.map(d => badge(s"${d.value}% global discount", BadgeTone.Emerald)).getOrElse(UI.empty),
                if a.categoryDiscounts.nonEmpty then badge(s"${a.categoryDiscounts.size} cat. overrides", BadgeTone.Brand) else UI.empty,
              ),
              if canManage then
                button
                  .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                  .onClick(state.deleteAgreement(a.target, a.currency))(state.i18n.t("adm-cat-delete"))
              else UI.empty,
            )
          }*
        ),
      ),
    )

  private def renderPricelists(
      state: BackofficeState,
      pricelists: List[Pricelist],
      canManage: Boolean,
      lang: Language,
  ): UI =
    div.style(stackMd)(
      pricelists.sortBy(_.currency.toString).map { pl =>
        div.cssClass("mp-card")(
          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
            div.style(rowSm)(
              h2(s"${pl.id.value} (${pl.currency})").style(sectionTitle.margin(0.px)),
              badge(s"v${pl.version.value}", BadgeTone.Brand),
              badge(s"${pl.entries.size} rate entries", BadgeTone.Neutral),
              badge(s"VAT ${pl.taxPolicy.defaultRate.value}%", BadgeTone.Neutral),
            ),
            pl.minimumOrderNet.map(m => badge(s"Min: ${formatMoney(m, pl.currency, lang)}", BadgeTone.Amber)).getOrElse(UI.empty),
          ),
          div.style(stackXs)(
            pl.entries.take(12).map { e =>
              div.style(rowBetween.fontSize(12.px).padding(6.px, 10.px).rounded(8.px).bg(surfaceAlt))(
                span(s"${e.id.value} · ${e.target}").style(Style.color(inkSecondary)),
                span(e.rate.toString).style(Style.fontWeight(FontWeight.w700).color(ink)),
              )
            }*
          ),
          if pl.entries.size > 12 then
            span(s"+ ${pl.entries.size - 12} more entries in this pricelist").style(smallText)
          else UI.empty,
          if canManage then
            div.style(rowSm)(
              button
                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                .onClick(state.savePricelist(pl.copy(version = bumpVersion(pl.version))))(
                  if lang == Language.Cs then "Navýšit verzi ceníku" else "Bump Pricelist Version"
                ),
            )
          else UI.empty,
        )
      }*
    )

  private def renderCostSheets(
      state: BackofficeState,
      sheets: List[ProductionCostSheet],
      canManage: Boolean,
      overheadRef: SignalRef[String],
  ): UI =
    div.style(stackMd)(
      sheets.sortBy(_.currency.toString).map { cs =>
        div.cssClass("mp-card")(
          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
            div.style(rowSm)(
              h2(s"Production Cost Sheet (${cs.currency})").style(sectionTitle.margin(0.px)),
              badge(s"${cs.entries.size} cost rules", BadgeTone.Neutral),
              badge(s"Overhead ×${cs.overheadFactor}", BadgeTone.Brand),
            ),
            if canManage then
              div.style(rowXs)(
                input
                  .cssClass("mp-input")
                  .style(Style.width(100.px))
                  .value(overheadRef)
                  .onInput(v => overheadRef.set(v)),
                button
                  .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                  .onClick {
                    overheadRef.get.flatMap { s =>
                      val factor = scala.util.Try(BigDecimal(s.trim)).getOrElse(cs.overheadFactor).max(BigDecimal("1.0"))
                      state.saveCostSheet(cs.copy(overheadFactor = factor))
                    }
                  }(state.i18n.t("adm-pri-cost-save")),
              )
            else UI.empty,
          ),
          div.style(stackXs)(
            cs.entries.take(10).map { e =>
              div.style(rowBetween.fontSize(12.px).padding(6.px, 10.px).rounded(8.px).bg(surfaceAlt))(
                span(e.target.toString).style(Style.color(inkSecondary)),
                span(e.rate.toString).style(Style.fontWeight(FontWeight.w600).color(ink)),
              )
            }*
          ),
        )
      }*
    )

  private def tabBtn(tab: Tab, active: Tab, ref: SignalRef[Tab], lbl: UI, countOpt: Option[Int]): UI =
    button
      .cssClass(if tab == active then "mp-pill-active" else "mp-pill")
      .onClick(ref.set(tab))(
        span(lbl),
        countOpt.map(c => span(s"($c)").style(Style.fontSize(11.px).opacity(0.85))).getOrElse(UI.empty),
      )

  private def statBox(lbl: UI, valueText: String, accent: Color): UI =
    div.cssClass("mp-kpi-card")(
      span(lbl).style(smallText.fontWeight(FontWeight.w600)),
      span(valueText).style(Style.fontSize(20.px).fontWeight(FontWeight.w800).color(accent)),
    )

  private def bumpVersion(v: Version): Version =
    val raw = v.value
    val dot = raw.lastIndexOf('.')
    val bumped =
      if dot >= 0 then
        raw.substring(dot + 1).toIntOption match
          case Some(n) => s"${raw.substring(0, dot)}.${n + 1}"
          case None    => s"$raw.1"
      else
        raw.toIntOption match
          case Some(n) => (n + 1).toString
          case None    => s"$raw.1"
    Version.unsafe(bumped)

