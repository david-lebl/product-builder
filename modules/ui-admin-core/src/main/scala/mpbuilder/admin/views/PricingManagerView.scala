package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.pricing.{AgreementDraft, AgreementJson}
import mpbuilder.admin.state.BackofficeState
import mpbuilder.catalog.ProductCatalog
import mpbuilder.backoffice.{MarginSimulationCmd, MarginSimulationResult}
import mpbuilder.customers.CustomerView
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{ActionMenu, ConfirmDialog, DataTable, TabBar}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*
import java.time.{Instant, LocalDate, ZoneOffset}

/** Pricing, Promo Codes, B2B Agreements, Pricelists, Cost Sheets & Interactive Live Margin Simulator.
  *
  * Uses refined `TabBar` with full-width bottom rule, Vaadin-style Master-Detail `DataTable` for
  * Promo Codes, B2B Agreements, and Pricelist entries, `ActionMenu` dropdowns in detail headers,
  * and `ConfirmDialog` for destructive actions.
  */
object PricingManagerView:

  private final case class PricelistEntryRow(
      rowId: String,
      pricelistId: PricelistId,
      currency: Currency,
      version: Version,
      entry: PriceEntry,
  )

  private def promoStatusBadge(st: PromoLifecycleStatus, isCs: Boolean): UI = st match
    case PromoLifecycleStatus.Active    => badge(if isCs then "Aktivní" else "Active", BadgeTone.Emerald)
    case PromoLifecycleStatus.Expired   => badge(if isCs then "Expirovaný" else "Expired", BadgeTone.Rose)
    case PromoLifecycleStatus.Exhausted => badge(if isCs then "Vyčerpaný" else "Exhausted", BadgeTone.Amber)
    case PromoLifecycleStatus.Inactive  => badge(if isCs then "Neaktivní" else "Inactive", BadgeTone.Neutral)

  private def agreementKey(a: CustomerPriceAgreement): String = AgreementDraft.keyOf(a)

  def render(state: BackofficeState): UI < Sync =
    for
      dialog           <- ConfirmDialog.Handle.make
      actionMenu       <- ActionMenu.Handle.make
      promoTableState  <- DataTable.makeState[DiscountCodeId](defaultSort = None, pageSize = 10)
      agrTableState    <- DataTable.makeState[String](defaultSort = None, pageSize = 10)
      plTableState     <- DataTable.makeState[String](defaultSort = Some(DataTable.SortSpec("id", ascending = true)), pageSize = 25)
      // Margin Simulator refs
      simCatRef        <- Signal.initRef("")
      simPresetRef     <- Signal.initRef("")
      simQtyRef        <- Signal.initRef("500")
      simCurRef        <- Signal.initRef[Currency](Currency.CZK)
      simSpeedRef      <- Signal.initRef[ManufacturingSpeed](ManufacturingSpeed.Standard)
      simTierRef       <- Signal.initRef("None")
      simPromoRef      <- Signal.initRef("")
      simResultRef     <- Signal.initRef[Option[MarginSimulationResult]](None)
      // Promo form refs
      promoCodeRef     <- Signal.initRef("")
      promoKindRef     <- Signal.initRef("percent")
      promoValRef      <- Signal.initRef("15")
      promoMaxRef      <- Signal.initRef("")
      promoExpiresRef  <- Signal.initRef("")
      // B2B agreement editor sessions (create panel / detail panel) + key of the agreement being edited
      agrCreate        <- AgreementEditorView.Session.make(AgreementDraft())
      agrEdit          <- AgreementEditorView.Session.make(AgreementDraft())
      agrEditKeyRef    <- Signal.initRef[Option[String]](None)
      // Cost sheet overhead edit ref
      overheadRef      <- Signal.initRef("1.15")
    yield
      val combined =
        state.pricingFocusTab
          .combineLatest(state.pricingFocusCustomerId)
          .combineLatest(state.catalog)
          .combineLatest(state.pricelists)
          .combineLatest(state.agreements)
          .combineLatest(state.promoCodes)
          .combineLatest(state.costSheets)
          .combineLatest(state.customers)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      UI.fragment(
        combined.render { case (((((((((activeTab, focusCustOpt), catOpt), pls), agrs), promos), costs), custs), authOpt), lang) =>
          val isCs      = lang == Language.Cs
          val canManage = state.canManage(authOpt)
          val totalPlEntries = pls.map(_.entries.size).sum

          val tabs = List(
            TabBar.TabItem("simulator", if isCs then "Simulátor marže" else "Margin Simulator", None),
            TabBar.TabItem("promos", if isCs then "Slevové kódy" else "Promo Codes", Some(promos.size)),
            TabBar.TabItem("agreements", if isCs then "B2B smlouvy" else "B2B Agreements", Some(agrs.size)),
            TabBar.TabItem("pricelists", if isCs then "Ceníky" else "Pricelists", Some(totalPlEntries)),
            TabBar.TabItem("costs", if isCs then "Nákladové listy" else "Cost Sheets", Some(costs.size)),
          )

          div.style(stackMd.width(100.pct))(
            div.style(stackXs)(
              h1(state.i18n.t("adm-pri-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
              p(state.i18n.t("adm-pri-subtitle")).style(mutedText.margin(0.px)),
            ),

            if !canManage && activeTab != "simulator" then
              alertBanner(
                BadgeTone.Amber,
                span(if isCs then "Pouze čtení" else "Read-Only"),
                List(if isCs then "Úpravy cen a slev vyžadují roli Manager nebo Admin." else "Pricing & promo mutations require Manager or Admin role."),
              )
            else UI.empty,

            // Underline TabBar (distinct from rounded filter chips)
            TabBar.render[String](
              items = tabs,
              active = activeTab,
              onSelect = t => state.pricingFocusTab.set(t),
            ),

            activeTab match
              case "promos" =>
                renderPromos(state, promos, promoTableState, dialog, actionMenu, canManage, promoCodeRef, promoKindRef, promoValRef, promoMaxRef, promoExpiresRef, lang)

              case "agreements" =>
                renderAgreements(
                  state,
                  agrs,
                  custs,
                  catOpt,
                  focusCustOpt,
                  agrTableState,
                  dialog,
                  actionMenu,
                  canManage,
                  agrCreate,
                  agrEdit,
                  agrEditKeyRef,
                  lang,
                )

              case "pricelists" =>
                renderPricelists(state, pls, plTableState, canManage, lang)

              case "costs" =>
                renderCostSheets(state, costs, canManage, overheadRef)

              case _ =>
                renderSimulator(state, catOpt, simCatRef, simPresetRef, simQtyRef, simCurRef, simSpeedRef, simTierRef, simPromoRef, simResultRef, lang)
          )
        },
        dialog.render,
      )

  // Every `<select>` below marks its current `<option>` as `selected`: Kyo UI re-renders regions as HTML
  // strings and browsers ignore a `value` attribute on `<select>`, so without `selected` the control
  // snapped back to its first option on each re-render (e.g. right after picking a value).
  private[admin] def renderSimulator(
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

        def resolve(catRaw: String, presetRaw: String) =
          val c = catsWithPresets.find(_.id.value == catRaw).orElse(catsWithPresets.headOption)
          val p = c.flatMap(cc => cc.presets.find(_.id.value == presetRaw).orElse(cc.presets.headOption))
          (c, p)

        // Reads every input at run time (not values captured by the last render), so the simulation
        // always reflects what is currently entered.
        val runSim: Unit < Async =
          for
            catRaw    <- simCatRef.get
            presetRaw <- simPresetRef.get
            qtyS      <- simQtyRef.get.map(_.trim)
            cur       <- simCurRef.get
            speed     <- simSpeedRef.get
            tierStr   <- simTierRef.get
            promo     <- simPromoRef.get.map(_.trim)
            _ <- resolve(catRaw, presetRaw) match
                   case (Some(c), Some(p)) =>
                     val cmd = MarginSimulationCmd(
                       categoryId = c.id,
                       presetId = p.id,
                       quantity = Quantity.unsafe(qtyS.toIntOption.filter(_ > 0).getOrElse(500)),
                       currency = cur,
                       speed = speed,
                       customerTier = CustomerTier.values.find(_.toString == tierStr),
                       promoCode = if promo.nonEmpty then Some(promo) else None,
                     )
                     state.simulateMargin(cmd).map(simResultRef.set)
                   case _ => Kyo.unit: Unit < Async
          yield ()

        // Selection changes re-run the simulation immediately once a result is shown.
        def rerunIfShown: Unit < Async =
          simResultRef.get.map(r => if r.isDefined then runSim else Kyo.unit: Unit < Async)

        val simSignal =
          simCatRef
            .combineLatest(simPresetRef)
            .combineLatest(simCurRef)
            .combineLatest(simSpeedRef)
            .combineLatest(simTierRef)
            .combineLatest(simResultRef)

        simSignal.render { case (((((selCatRaw, selPresetRaw), cur), speed), tierStr), resOpt) =>
          val (activeCat, activePre) = resolve(selCatRaw, selPresetRaw)
          val presets                = activeCat.map(_.presets).getOrElse(Nil)
          val activeCatId            = activeCat.map(_.id.value).getOrElse("")
          val activePreId            = activePre.map(_.id.value).getOrElse("")

          div.style(stackMd)(
            div.cssClass("mp-card")(
              h2(state.i18n.t("adm-pri-tab-simulator")).style(sectionTitle.margin(0.px)),
              div.cssClass("mp-field-row")(
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Kategorie" else "Category").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(activeCatId)
                    .onChange { v =>
                      val firstPre = catsWithPresets.find(_.id.value == v).flatMap(_.presets.headOption).map(_.id.value).getOrElse("")
                      simPresetRef.set(firstPre).andThen(simCatRef.set(v)).andThen(rerunIfShown)
                    }(
                      catsWithPresets.map(c => option.value(c.id.value).selected(c.id.value == activeCatId)(c.name(lang)))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Šablona" else "Preset").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(activePreId)
                    .onChange(v => simPresetRef.set(v).andThen(rerunIfShown))(
                      presets.map(p => option.value(p.id.value).selected(p.id.value == activePreId)(p.name(lang)))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Množství (ks)" else "Quantity (pcs)").style(labelStyle),
                  // Text input + digit filter (not type="number"): number inputs lose the caret on re-render.
                  input
                    .cssClass("mp-input")
                    .inputFilter(InputFilter.Digits)
                    .value(simQtyRef)
                    .onInput(v => simQtyRef.set(v))
                    .onChange(v => simQtyRef.set(v).andThen(rerunIfShown)),
                ),
              ),
              div.cssClass("mp-field-row")(
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Měna" else "Currency").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(cur.toString)
                    .onChange(v => Currency.values.find(_.toString == v).map(c => simCurRef.set(c).andThen(rerunIfShown)).getOrElse(Kyo.unit))(
                      List(Currency.CZK, Currency.USD).map(c => option.value(c.toString).selected(c == cur)(c.toString))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Rychlost výroby" else "Speed").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(speed.toString)
                    .onChange(v => ManufacturingSpeed.values.find(_.toString == v).map(sp => simSpeedRef.set(sp).andThen(rerunIfShown)).getOrElse(Kyo.unit))(
                      ManufacturingSpeed.values.toList.map(s => option.value(s.toString).selected(s == speed)(speedLabel(s, lang)))*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Cenová hladina" else "Customer Tier").style(labelStyle),
                  select
                    .cssClass("mp-input")
                    .value(tierStr)
                    .onChange(v => simTierRef.set(v).andThen(rerunIfShown))(
                      (None :: CustomerTier.values.toList.map(Some(_))).map {
                        case None    => option.value("None").selected(tierStr == "None")(if lang == Language.Cs then "Veřejná (Walk-in)" else "Public (Walk-in)")
                        case Some(t) => option.value(t.toString).selected(tierStr == t.toString)(t.toString)
                      }*
                    ),
                ),
                div.cssClass("mp-field")(
                  label(if lang == Language.Cs then "Slevový kód (volitelně)" else "Promo Code (optional)").style(labelStyle),
                  input
                    .cssClass("mp-input")
                    .placeholder("SAVE10")
                    .value(simPromoRef)
                    .onInput(v => simPromoRef.set(v))
                    .onChange(v => simPromoRef.set(v).andThen(rerunIfShown)),
                ),
              ),
              div.style(rowSm)(
                button.cssClass("mp-btn mp-btn-primary").onClick(runSim)(state.i18n.t("adm-pri-sim-run")),
              ),
            ),

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
      tableState: SignalRef[DataTable.State[DiscountCodeId]],
      dialog: ConfirmDialog.Handle,
      actionMenu: ActionMenu.Handle,
      canManage: Boolean,
      codeRef: SignalRef[String],
      kindRef: SignalRef[String],
      valRef: SignalRef[String],
      maxRef: SignalRef[String],
      expiresRef: SignalRef[String],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val now  = Instant.now()

    def benefitLabel(p: PromoCode): String = p.benefit match
      case PromoBenefit.PercentageOff(pct) => s"${pct.value}% OFF"
      case PromoBenefit.FixedAmountOff(pr) => s"−${formatMoney(pr.amount, pr.currency, lang)}"
      case PromoBenefit.FreeDelivery       => if isCs then "Doprava zdarma" else "Free Delivery"

    val columns: List[DataTable.Column[PromoCode]] = List(
      DataTable.Column[PromoCode](
        id = "code",
        header = if isCs then "Kód" else "Code",
        sortKey = Some(_.code.value),
        cell = p => span(p.code.value).style(Style.fontWeight(FontWeight.w800).color(ink)),
      ),
      DataTable.Column[PromoCode](
        id = "benefit",
        header = if isCs then "Sleva" else "Benefit",
        sortKey = Some(benefitLabel),
        cell = p => badge(benefitLabel(p), BadgeTone.Brand),
      ),
      DataTable.Column[PromoCode](
        id = "lifecycle",
        header = if isCs then "Stav" else "Status",
        sortKey = Some(_.lifecycleStatus(now).toString),
        cell = p => promoStatusBadge(p.lifecycleStatus(now), isCs),
      ),
      DataTable.Column[PromoCode](
        id = "uses",
        header = if isCs then "Využití" else "Uses",
        numericSortKey = Some(p => BigDecimal(p.constraints.currentUses)),
        cell = p => span(s"${p.constraints.currentUses} / ${p.constraints.maxUses.map(_.toString).getOrElse("∞")}").style(smallText),
      ),
      DataTable.Column[PromoCode](
        id = "createdAt",
        header = if isCs then "Vytvořeno" else "Created At",
        numericSortKey = Some(p => BigDecimal(p.createdAt.toEpochMilli)),
        cell = p => span(formatInstant(p.createdAt)).style(smallText),
      ),
      DataTable.Column[PromoCode](
        id = "expiresAt",
        header = if isCs then "Platnost do" else "Expires At",
        sortKey = Some(p => p.constraints.validUntil.map(formatInstant).getOrElse("9999")),
        cell = p =>
          span(p.constraints.validUntil.map(formatInstant).getOrElse(if isCs then "Bez omezení" else "No expiry")).style(smallText),
      ),
    )

    val filterGroups: List[DataTable.FilterGroup[PromoCode]] = List(
      DataTable.FilterGroup[PromoCode](
        id = "lifecycle",
        label = if isCs then "Stav kódu" else "Lifecycle",
        options = List(
          DataTable.FilterOption("Active", if isCs then "Aktivní" else "Active", _.lifecycleStatus(now) == PromoLifecycleStatus.Active),
          DataTable.FilterOption("Expired", if isCs then "Expirované" else "Expired", _.lifecycleStatus(now) == PromoLifecycleStatus.Expired),
          DataTable.FilterOption("Exhausted", if isCs then "Vyčerpané" else "Exhausted", _.lifecycleStatus(now) == PromoLifecycleStatus.Exhausted),
          DataTable.FilterOption("Inactive", if isCs then "Neaktivní" else "Inactive", _.lifecycleStatus(now) == PromoLifecycleStatus.Inactive),
        ),
      )
    )

    DataTable.render[PromoCode, DiscountCodeId](
      title = Some(span(if isCs then "Slevové kódy" else "Promo Codes")),
      rows = promos,
      rowKey = _.id,
      columns = columns,
      state = tableState,
      searchPlaceholder = if isCs then "Hledat kód nebo typ slevy..." else "Search promo code or benefit...",
      matchesSearch = (p, q) =>
        p.code.value.toLowerCase.contains(q) || p.id.value.toLowerCase.contains(q) || benefitLabel(p).toLowerCase.contains(q),
      filterGroups = filterGroups,
      detailTitle = Some(p => p.code.value),
      createConfig =
        if canManage then
          Some(
            DataTable.CreateConfig(
              buttonLabel = UI.fragment(span("+ "), state.i18n.t("adm-pri-promo-create")),
              title = span(state.i18n.t("adm-pri-promo-create")),
              renderForm = onClose =>
                div.style(stackMd)(
                  div.cssClass("mp-field-row")(
                    div.cssClass("mp-field")(
                      label(if isCs then "Kód" else "Code").style(labelStyle),
                      input.cssClass("mp-input").placeholder("SPRING20").value(codeRef).onInput(v => codeRef.set(v)),
                    ),
                    div.cssClass("mp-field")(
                      label(if isCs then "Typ slevy" else "Benefit").style(labelStyle),
                      kindRef.render { k =>
                        select.cssClass("mp-input").value(k).onChange(v => kindRef.set(v))(
                          option.value("percent").selected(k == "percent")(if isCs then "Procentuální sleva (%)" else "Percentage Off (%)"),
                          option.value("fixed").selected(k == "fixed")(if isCs then "Pevná částka (CZK)" else "Fixed Amount (CZK)"),
                          option.value("freeDelivery").selected(k == "freeDelivery")(if isCs then "Doprava zdarma" else "Free Delivery"),
                        )
                      },
                    ),
                  ),
                  div.cssClass("mp-field-row")(
                    div.cssClass("mp-field")(
                      label(if isCs then "Hodnota" else "Value").style(labelStyle),
                      numberInput.cssClass("mp-input").value(valRef).onInput(v => valRef.set(v)),
                    ),
                    div.cssClass("mp-field")(
                      label(if isCs then "Max. použití" else "Max Uses").style(labelStyle),
                      numberInput.cssClass("mp-input").placeholder(if isCs then "Neomezeno" else "Unlimited").value(maxRef).onInput(v => maxRef.set(v)),
                    ),
                    div.cssClass("mp-field")(
                      label(if isCs then "Platnost do (YYYY-MM-DD)" else "Expires (YYYY-MM-DD)").style(labelStyle),
                      input.cssClass("mp-input").placeholder("2026-12-31").value(expiresRef).onInput(v => expiresRef.set(v)),
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
                          expStr  <- expiresRef.get.map(_.trim)
                          _ <-
                            if rawCode.isEmpty then state.errorBanner.set(Some("Promo code must not be empty."))
                            else
                              val validUntilOpt: Option[Instant] =
                                if expStr.isEmpty then None
                                else
                                  scala.util.Try(LocalDate.parse(expStr).atTime(23, 59, 59).toInstant(ZoneOffset.UTC)).toOption
                              if expStr.nonEmpty && validUntilOpt.isEmpty then
                                state.errorBanner.set(Some("Expiration date must use YYYY-MM-DD format (e.g. 2026-12-31)."))
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
                                  constraints = PromoConstraints(
                                    validUntil = validUntilOpt,
                                    maxUses = mStr.toIntOption.filter(_ > 0),
                                  ),
                                  isActive = true,
                                )
                                state.savePromoCode(promo).andThen(codeRef.set("")).andThen(expiresRef.set("")).andThen(onClose())
                        yield ()
                      }(state.i18n.t("adm-pri-promo-create")),
                    button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(onClose())(state.i18n.t("adm-cat-cancel")),
                  ),
                ),
            )
          )
        else None,
      renderDetail = Some { p =>
        val promoMenuItems = List(
          ActionMenu.Item(
            label = if p.isActive then state.i18n.t("adm-pri-promo-deactivate") else state.i18n.t("adm-pri-promo-activate"),
            onClick = () => state.togglePromoActive(p.id, !p.isActive),
            tone = if p.isActive then ActionMenu.ItemTone.Amber else ActionMenu.ItemTone.Primary,
          ),
          ActionMenu.Item(
            label = state.i18n.t("adm-cat-delete"),
            onClick = () =>
              dialog.open(
                ConfirmDialog.Config(
                  title = if isCs then "Smazat slevový kód" else "Delete Promo Code",
                  message = if isCs then s"Opravdu chcete smazat slevový kód „${p.code.value}“?" else s"Delete promo code “${p.code.value}”?",
                  confirmLabel = if isCs then "Smazat" else "Delete",
                  cancelLabel = if isCs then "Zrušit" else "Cancel",
                  tone = ConfirmDialog.DialogTone.Danger,
                  onConfirm = _ => state.deletePromoCode(p.id),
                )
              ),
            tone = ActionMenu.ItemTone.Danger,
            dividerBefore = true,
          ),
        )

        div.style(stackMd)(
          div.cssClass("mp-detail-head")(
            div.cssClass("mp-detail-head-row")(
              h3(p.code.value).style(Style.margin(0.px).fontSize(17.px).fontWeight(FontWeight.w800).color(ink)),
              if canManage then
                actionMenu.render(
                  key = s"promo-${p.id.value}",
                  buttonLabel = span(if isCs then "Akce" else "Actions"),
                  items = promoMenuItems,
                )
              else UI.empty,
            ),
            div.cssClass("mp-detail-tags-row")(
              badge(benefitLabel(p), BadgeTone.Brand),
              promoStatusBadge(p.lifecycleStatus(now), isCs),
              badge(p.id.value, BadgeTone.Neutral),
            ),
          ),
          div.style(stackXs)(
            span(s"${if isCs then "Vytvořeno" else "Created"}: ${formatInstant(p.createdAt)}").style(smallText),
            span(s"${if isCs then "Využití" else "Usage"}: ${p.constraints.currentUses} / ${p.constraints.maxUses.map(_.toString).getOrElse("∞")}").style(smallText),
            p.constraints.validFrom.map(v => span(s"${if isCs then "Platnost od" else "Valid From"}: ${formatInstant(v)}").style(smallText)).getOrElse(UI.empty),
            p.constraints.validUntil.map(v => span(s"${if isCs then "Platnost do" else "Valid Until"}: ${formatInstant(v)}").style(smallText)).getOrElse(UI.empty),
          ),
          div.style(rowWrap)(
            p.constraints.minimumOrderNet.map(m => badge(s"Min net: ${m.value}", BadgeTone.Amber)).getOrElse(UI.empty),
            if p.constraints.allowedSegments.nonEmpty then badge(s"Segments: ${p.constraints.allowedSegments.mkString(", ")}", BadgeTone.Neutral) else UI.empty,
            if p.constraints.allowedCategories.nonEmpty then badge(s"Categories: ${p.constraints.allowedCategories.map(_.value).mkString(", ")}", BadgeTone.Neutral) else UI.empty,
          ),
        )
      },
      emptyMessage = if isCs then "Žádné slevové kódy neodpovídají filtru." else "No promo codes match the current filter.",
      lang = lang,
    )

  private def renderAgreements(
      state: BackofficeState,
      agreements: List[CustomerPriceAgreement],
      customers: List[CustomerView],
      catalog: Option[ProductCatalog],
      focusCustOpt: Option[CustomerId],
      tableState: SignalRef[DataTable.State[String]],
      dialog: ConfirmDialog.Handle,
      actionMenu: ActionMenu.Handle,
      canManage: Boolean,
      createSession: AgreementEditorView.Session,
      editSession: AgreementEditorView.Session,
      editKeyRef: SignalRef[Option[String]],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val customerById = customers.map(c => c.id -> c).toMap
    val newDraft = AgreementDraft(
      targetKind = if focusCustOpt.isDefined then AgreementDraft.TargetKind.Customer else AgreementDraft.TargetKind.Tier,
      customerId = focusCustOpt.map(_.value).getOrElse(""),
    )

    def targetDisplay(t: AgreementTarget): String = t match
      case AgreementTarget.TierDefault(tier) => s"Tier: $tier"
      case AgreementTarget.SpecificCustomer(cid) =>
        customerById.get(cid) match
          case Some(c) =>
            val n = c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")
            s"$n (${cid.value})"
          case None => s"Customer: ${cid.value}"

    def scopedCount(a: CustomerPriceAgreement): Int =
      a.categoryDiscounts.size + a.materialDiscounts.size + a.finishDiscounts.size + a.fixedMaterialRates.size +
        a.customVolumeCurves.map(_.size).getOrElse(0)

    val visibleAgreements = focusCustOpt match
      case None => agreements
      case Some(cid) =>
        val custTier = customerById.get(cid).map(_.tier)
        agreements.filter { a =>
          a.target match
            case AgreementTarget.SpecificCustomer(id) => id == cid
            case AgreementTarget.TierDefault(t)       => custTier.contains(t)
        }

    val columns: List[DataTable.Column[CustomerPriceAgreement]] = List(
      DataTable.Column[CustomerPriceAgreement](
        id = "target",
        header = if isCs then "Cíl smlouvy" else "Target",
        sortKey = Some(a => targetDisplay(a.target)),
        cell = a =>
          a.target match
            case AgreementTarget.TierDefault(t) =>
              div.style(rowXs)(tierBadge(t), span(if isCs then "Výchozí pro hladinu" else "Tier Default").style(smallText))
            case AgreementTarget.SpecificCustomer(cid) =>
              div.style(rowXs)(badge(if isCs then "Zákazník" else "Customer", BadgeTone.Brand), span(targetDisplay(a.target)).style(Style.fontWeight(FontWeight.w700).color(ink))),
      ),
      DataTable.Column[CustomerPriceAgreement](
        id = "currency",
        header = if isCs then "Měna" else "Currency",
        sortKey = Some(_.currency.toString),
        cell = a => badge(a.currency.toString, BadgeTone.Neutral),
      ),
      DataTable.Column[CustomerPriceAgreement](
        id = "global",
        header = if isCs then "Globální sleva" else "Global Discount",
        numericSortKey = Some(a => a.globalDiscount.map(_.value).getOrElse(BigDecimal(0))),
        cell = a =>
          a.globalDiscount match
            case Some(d) => badge(s"${d.value}%", BadgeTone.Emerald)
            case None    => span("—").style(smallText),
      ),
      DataTable.Column[CustomerPriceAgreement](
        id = "minOrder",
        header = if isCs then "Min. objednávka" else "Min Order Override",
        numericSortKey = Some(a => a.minimumOrderOverride.map(_.value).getOrElse(BigDecimal(0))),
        cell = a =>
          a.minimumOrderOverride match
            case Some(m) => span(formatMoney(m, a.currency, lang)).style(Style.fontWeight(FontWeight.w600).color(ink))
            case None    => span("—").style(smallText),
      ),
      DataTable.Column[CustomerPriceAgreement](
        id = "scoped",
        header = if isCs then "Položkové slevy" else "Scoped Discounts",
        numericSortKey = Some(a => BigDecimal(scopedCount(a))),
        alignRight = true,
        cell = a =>
          val total = scopedCount(a)
          if total > 0 then badge(s"$total", BadgeTone.Brand) else span("0").style(smallText),
      ),
    )

    val filterGroups: List[DataTable.FilterGroup[CustomerPriceAgreement]] = List(
      DataTable.FilterGroup[CustomerPriceAgreement](
        id = "targetType",
        label = if isCs then "Typ cíle" else "Target Type",
        options = List(
          DataTable.FilterOption("Customer", if isCs then "Zákazník" else "Customer", _.target.isInstanceOf[AgreementTarget.SpecificCustomer]),
          DataTable.FilterOption("Tier", if isCs then "Hladina" else "Tier", _.target.isInstanceOf[AgreementTarget.TierDefault]),
        ),
      ),
      DataTable.FilterGroup[CustomerPriceAgreement](
        id = "currency",
        label = if isCs then "Měna" else "Currency",
        options = List(Currency.CZK, Currency.USD).map { c =>
          DataTable.FilterOption(c.toString, c.toString, _.currency == c)
        },
      ),
    )

    div.style(stackMd)(
      // Focus customer banner (when deep-linked from CustomerManagerView)
      focusCustOpt match
        case Some(cid) =>
          div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(10.px, 14.px).rounded(12.px).bg(brandSoft).border(1.px, brandBorder))(
            span(
              if isCs then s"Filtrováno pro zákazníka: ${targetDisplay(AgreementTarget.SpecificCustomer(cid))}"
              else s"Filtered for customer: ${targetDisplay(AgreementTarget.SpecificCustomer(cid))}",
            ).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(brandDark)),
            button
              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
              .onClick(state.pricingFocusCustomerId.set(None))(if isCs then "Zobrazit všechny smlouvy" else "Show All Agreements"),
          )
        case None => UI.empty,

      // Agreements Universal DataTable (side-split detail & create-in-detail)
      DataTable.render[CustomerPriceAgreement, String](
        title = Some(span(if isCs then "B2B cenové smlouvy" else "B2B Price Agreements")),
        rows = visibleAgreements,
        rowKey = agreementKey,
        columns = columns,
        state = tableState,
        searchPlaceholder = if isCs then "Hledat zákazníka, hladinu nebo měnu..." else "Search customer, tier, or currency...",
        matchesSearch = (a, q) =>
          targetDisplay(a.target).toLowerCase.contains(q) || a.currency.toString.toLowerCase.contains(q),
        filterGroups = filterGroups,
        detailTitle = Some(a => s"${targetDisplay(a.target)} (${a.currency})"),
        createConfig =
          if canManage then
            Some(
              DataTable.CreateConfig(
                buttonLabel = UI.fragment(span("+ "), state.i18n.t("adm-pri-agr-create")),
                title = span(state.i18n.t("adm-pri-agr-create")),
                renderForm = onClose =>
                  AgreementEditorView.render(
                    state = state,
                    session = createSession,
                    catalog = catalog,
                    customers = customers,
                    existing = agreements,
                    original = None,
                    defaultCustomerId = focusCustOpt.map(_.value),
                    lang = lang,
                    onDone = () => createSession.load(newDraft).andThen(onClose()),
                  ),
              )
            )
          else None,
        renderDetail = Some { a =>
          val key = agreementKey(a)
          editKeyRef.render { ekOpt =>
            val isEditing = ekOpt.contains(key) && canManage
            val agrMenuItems: List[ActionMenu.Item] =
              List(
                if !isEditing then
                  Some(
                    ActionMenu.Item(
                      label = state.i18n.t("adm-pri-agr-edit"),
                      onClick = () => editSession.load(AgreementDraft.from(a)).andThen(editKeyRef.set(Some(key))),
                      tone = ActionMenu.ItemTone.Primary,
                    )
                  )
                else None,
                Some(
                  ActionMenu.Item(
                    label = state.i18n.t("adm-pri-agr-edit-json"),
                    onClick = () =>
                      editSession
                        .load(AgreementDraft.from(a))
                        .andThen(editSession.json.set(AgreementJson.encode(a)))
                        .andThen(editSession.mode.set(AgreementEditorView.Mode.Json))
                        .andThen(editKeyRef.set(Some(key))),
                  )
                ),
                Some(
                  ActionMenu.Item(
                    label = state.i18n.t("adm-cat-delete"),
                    onClick = () =>
                      dialog.open(
                        ConfirmDialog.Config(
                          title = if isCs then "Smazat cenovou smlouvu" else "Delete Price Agreement",
                          message = if isCs then s"Smazat smlouvu pro ${targetDisplay(a.target)} (${a.currency})?" else s"Delete agreement for ${targetDisplay(a.target)} (${a.currency})?",
                          confirmLabel = if isCs then "Smazat" else "Delete",
                          cancelLabel = if isCs then "Zrušit" else "Cancel",
                          tone = ConfirmDialog.DialogTone.Danger,
                          onConfirm = _ => state.deleteAgreement(a.target, a.currency),
                        )
                      ),
                    tone = ActionMenu.ItemTone.Danger,
                    dividerBefore = true,
                  )
                ),
              ).flatten

            div.style(stackMd)(
              div.cssClass("mp-detail-head")(
                div.cssClass("mp-detail-head-row")(
                  h3(targetDisplay(a.target)).style(Style.margin(0.px).fontSize(17.px).fontWeight(FontWeight.w800).color(ink)),
                  if canManage then
                    actionMenu.render(
                      key = s"agr-$key",
                      buttonLabel = span(if isCs then "Akce" else "Actions"),
                      items = agrMenuItems,
                    )
                  else UI.empty,
                ),
                div.cssClass("mp-detail-tags-row")(
                  a.target match
                    case AgreementTarget.TierDefault(t) => tierBadge(t)
                    case AgreementTarget.SpecificCustomer(_) => badge(if isCs then "Zákazník" else "Customer", BadgeTone.Brand),
                  badge(a.currency.toString, BadgeTone.Neutral),
                  a.globalDiscount.map(d => badge(s"${if isCs then "Globální" else "Global"} −${d.value}%", BadgeTone.Emerald)).getOrElse(UI.empty),
                  a.minimumOrderOverride.map(m => badge(s"Min: ${formatMoney(m, a.currency, lang)}", BadgeTone.Amber)).getOrElse(UI.empty),
                ),
              ),
              if isEditing then
                AgreementEditorView.render(
                  state = state,
                  session = editSession,
                  catalog = catalog,
                  customers = customers,
                  existing = agreements,
                  original = Some(a),
                  defaultCustomerId = None,
                  lang = lang,
                  onDone = () => editKeyRef.set(None),
                )
              else
                div.style(stackSm)(
                  span(state.i18n.t("adm-pri-agr-rules")).style(labelStyle),
                  AgreementEditorView.renderSummary(a, catalog, lang),
                  if canManage then
                    div(
                      button
                        .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                        .onClick(editSession.load(AgreementDraft.from(a)).andThen(editKeyRef.set(Some(key))))(state.i18n.t("adm-pri-agr-edit"))
                    )
                  else UI.empty,
                ),
            )
          }
        },
        emptyMessage = if isCs then "Žádné B2B smlouvy neodpovídají filtru." else "No B2B agreements match the current filter.",
        lang = lang,
      ),
    )

  private def renderPricelists(
      state: BackofficeState,
      pricelists: List[Pricelist],
      tableState: SignalRef[DataTable.State[String]],
      canManage: Boolean,
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val allRows: List[PricelistEntryRow] = pricelists.sortBy(_.currency.toString).flatMap { pl =>
      pl.entries.map(e => PricelistEntryRow(s"${pl.id.value}:${e.id.value}", pl.id, pl.currency, pl.version, e))
    }

    val columns: List[DataTable.Column[PricelistEntryRow]] = List(
      DataTable.Column[PricelistEntryRow](
        id = "pricelist",
        header = if isCs then "Ceník" else "Pricelist",
        sortKey = Some(r => s"${r.pricelistId.value} ${r.currency}"),
        cell = r =>
          div.style(rowXs)(
            span(r.pricelistId.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
            badge(r.currency.toString, BadgeTone.Brand),
          ),
      ),
      DataTable.Column[PricelistEntryRow](
        id = "id",
        header = if isCs then "ID sazby" else "Rate ID",
        sortKey = Some(_.entry.id.value),
        cell = r => span(r.entry.id.value).style(Style.fontWeight(FontWeight.w600).color(inkSecondary)),
      ),
      DataTable.Column[PricelistEntryRow](
        id = "target",
        header = if isCs then "Cíl" else "Target",
        sortKey = Some(_.entry.target.toString),
        cell = r => span(r.entry.target.toString).style(Style.fontSize(12.px).color(ink)),
      ),
      DataTable.Column[PricelistEntryRow](
        id = "phase",
        header = if isCs then "Fáze" else "Phase",
        sortKey = Some(_.entry.phase.toString),
        cell = r =>
          val tone = if r.entry.phase == ChargePhase.OneTimeSetup then BadgeTone.Amber else BadgeTone.Neutral
          badge(r.entry.phase.toString, tone),
      ),
      DataTable.Column[PricelistEntryRow](
        id = "rate",
        header = if isCs then "Model sazby" else "Rate Model",
        sortKey = Some(_.entry.rate.toString),
        cell = r => span(r.entry.rate.toString).style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(ink)),
      ),
    )

    val filterGroups: List[DataTable.FilterGroup[PricelistEntryRow]] = List(
      DataTable.FilterGroup[PricelistEntryRow](
        id = "pricelist",
        label = if isCs then "Ceník" else "Pricelist",
        options = pricelists.sortBy(_.currency.toString).map { pl =>
          DataTable.FilterOption(pl.id.value, s"${pl.id.value} (${pl.currency})", _.pricelistId == pl.id)
        },
      ),
      DataTable.FilterGroup[PricelistEntryRow](
        id = "phase",
        label = if isCs then "Fáze" else "Phase",
        options = ChargePhase.values.toList.map { ph =>
          DataTable.FilterOption(ph.toString, ph.toString, _.entry.phase == ph)
        },
      ),
    )

    div.style(stackMd)(
      // Pricelist metadata header cards
      div.style(rowWrap)(
        pricelists.sortBy(_.currency.toString).map { pl =>
          div.cssClass("mp-card").style(Style.flexGrow(1.0).minWidth(280.px).gap(8.px))(
            div.style(rowBetween.flexWrap(FlexWrap.wrap))(
              div.style(rowXs)(
                span(s"${pl.id.value} (${pl.currency})").style(Style.fontWeight(FontWeight.w800).color(ink)),
                badge(s"v${pl.version.value}", BadgeTone.Brand),
              ),
              if canManage then
                button
                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                  .onClick(state.savePricelist(pl.copy(version = bumpVersion(pl.version))))(
                    if isCs then "Navýšit verzi" else "Bump Version",
                  )
              else UI.empty,
            ),
            div.style(rowWrap)(
              badge(s"${pl.entries.size} ${if isCs then "sazeb" else "rates"}", BadgeTone.Neutral),
              badge(s"VAT ${pl.taxPolicy.defaultRate.value}%", BadgeTone.Neutral),
              badge(s"${pl.volumeCurves.size} ${if isCs then "obj. křivek" else "vol. curves"}", BadgeTone.Neutral),
              pl.minimumOrderNet.map(m => badge(s"Min: ${formatMoney(m, pl.currency, lang)}", BadgeTone.Amber)).getOrElse(UI.empty),
            ),
          )
        }*
      ),

      // Full Pricelist Rate Entries DataTable
      DataTable.render[PricelistEntryRow, String](
        title = Some(span(if isCs then "Položky ceníků" else "Pricelist Rate Entries")),
        rows = allRows,
        rowKey = _.rowId,
        columns = columns,
        state = tableState,
        searchPlaceholder = if isCs then "Hledat ID sazby, materiál, metodu, zušlechtění nebo sazbu..." else "Search rate ID, material, method, finish, or rate...",
        matchesSearch = (r, q) =>
          r.entry.id.value.toLowerCase.contains(q) ||
            r.entry.target.toString.toLowerCase.contains(q) ||
            r.entry.rate.toString.toLowerCase.contains(q) ||
            r.pricelistId.value.toLowerCase.contains(q),
        filterGroups = filterGroups,
        detailTitle = Some(r => s"${r.pricelistId.value} · ${r.entry.id.value}"),
        renderDetail = Some { r =>
          div.style(stackMd)(
            div.cssClass("mp-detail-head")(
              div.cssClass("mp-detail-head-row")(
                h3(r.entry.id.value).style(Style.margin(0.px).fontSize(17.px).fontWeight(FontWeight.w800).color(ink)),
              ),
              div.cssClass("mp-detail-tags-row")(
                badge(s"${r.pricelistId.value} v${r.version.value}", BadgeTone.Brand),
                badge(r.currency.toString, BadgeTone.Neutral),
                badge(r.entry.phase.toString, if r.entry.phase == ChargePhase.OneTimeSetup then BadgeTone.Amber else BadgeTone.Neutral),
              ),
            ),
            div.style(stackXs)(
              span(s"${if isCs then "Cíl" else "Target"}: ${r.entry.target}").style(Style.fontSize(13.px).color(ink)),
              span(s"${if isCs then "Rozsah (Scope)" else "Scope"}: ${r.entry.scope}").style(smallText),
              span(s"${if isCs then "Cenový model" else "Rate Model"}: ${r.entry.rate}").style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(brand)),
            ),
          )
        },
        emptyMessage = if isCs then "Žádné sazby neodpovídají filtru." else "No pricelist entries match the current filter.",
        lang = lang,
      ),
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
            cs.entries.map { e =>
              div.style(rowBetween.fontSize(12.px).padding(6.px, 10.px).rounded(8.px).bg(surfaceAlt))(
                span(e.target.toString).style(Style.color(inkSecondary)),
                span(e.rate.toString).style(Style.fontWeight(FontWeight.w600).color(ink)),
              )
            }*
          ),
        )
      }*
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
