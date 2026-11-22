package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.backoffice.{MarginSimulationCmd, MarginSimulationResult}
import mpbuilder.customers.CustomerView
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{ConfirmDialog, DataTable, TabBar}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*
import java.time.{Instant, LocalDate, ZoneOffset}

/** Pricing, Promo Codes, B2B Agreements, Pricelists, Cost Sheets & Interactive Live Margin Simulator.
  *
  * Uses underline `TabBar` for distinct subviews, universal `DataTable` for Promo Codes,
  * B2B Agreements, and Pricelist entries, and `ConfirmDialog` for destructive actions.
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

  private def agreementKey(a: CustomerPriceAgreement): String =
    val tKey = a.target match
      case AgreementTarget.SpecificCustomer(cid) => s"cust:${cid.value}"
      case AgreementTarget.TierDefault(t)        => s"tier:${t.toString}"
    s"$tKey:${a.currency}"

  def render(state: BackofficeState): UI < Sync =
    for
      dialog           <- ConfirmDialog.Handle.make
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
      // New Agreement form refs
      agrTargetKindRef <- Signal.initRef("tier")
      agrTierRef       <- Signal.initRef[CustomerTier](CustomerTier.Gold)
      agrCustIdRef     <- Signal.initRef("")
      agrCurRef        <- Signal.initRef[Currency](Currency.CZK)
      agrDiscRef       <- Signal.initRef("12")
      agrMinOrdRef     <- Signal.initRef("")
      // Inline Agreement Global Edit refs (key -> (discountPct, minOrder))
      agrEditKeyRef    <- Signal.initRef[Option[String]](None)
      agrEditDiscRef   <- Signal.initRef("")
      agrEditMinRef    <- Signal.initRef("")
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
                renderPromos(state, promos, promoTableState, dialog, canManage, promoCodeRef, promoKindRef, promoValRef, promoMaxRef, promoExpiresRef, lang)

              case "agreements" =>
                renderAgreements(
                  state,
                  agrs,
                  custs,
                  focusCustOpt,
                  agrTableState,
                  dialog,
                  canManage,
                  agrTargetKindRef,
                  agrTierRef,
                  agrCustIdRef,
                  agrCurRef,
                  agrDiscRef,
                  agrMinOrdRef,
                  agrEditKeyRef,
                  agrEditDiscRef,
                  agrEditMinRef,
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

    div.style(stackMd)(
      if canManage then
        div.cssClass("mp-card")(
          h2(state.i18n.t("adm-pri-promo-create")).style(sectionTitle.margin(0.px)),
          div.cssClass("mp-field-row")(
            div.cssClass("mp-field")(
              label(if isCs then "Kód" else "Code").style(labelStyle),
              input.cssClass("mp-input").placeholder("SPRING20").value(codeRef).onInput(v => codeRef.set(v)),
            ),
            div.cssClass("mp-field")(
              label(if isCs then "Typ slevy" else "Benefit").style(labelStyle),
              kindRef.render { k =>
                select.cssClass("mp-input").value(k).onChange(v => kindRef.set(v))(
                  option.value("percent")(if isCs then "Procentuální sleva (%)" else "Percentage Off (%)"),
                  option.value("fixed")(if isCs then "Pevná částka (CZK)" else "Fixed Amount (CZK)"),
                  option.value("freeDelivery")(if isCs then "Doprava zdarma" else "Free Delivery"),
                )
              },
            ),
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
                        state.savePromoCode(promo).andThen(codeRef.set("")).andThen(expiresRef.set(""))
                yield ()
              }(state.i18n.t("adm-pri-promo-create")),
          ),
        )
      else UI.empty,

      DataTable.render[PromoCode, DiscountCodeId](
        rows = promos,
        rowKey = _.id,
        columns = columns,
        state = tableState,
        searchPlaceholder = if isCs then "Hledat kód nebo typ slevy..." else "Search promo code or benefit...",
        matchesSearch = (p, q) =>
          p.code.value.toLowerCase.contains(q) || p.id.value.toLowerCase.contains(q) || benefitLabel(p).toLowerCase.contains(q),
        filterGroups = filterGroups,
        renderDetail = Some { p =>
          div.style(stackSm)(
            div.style(rowBetween.flexWrap(FlexWrap.wrap))(
              div.style(rowSm.flexWrap(FlexWrap.wrap))(
                span(p.code.value).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                badge(p.id.value, BadgeTone.Neutral),
                promoStatusBadge(p.lifecycleStatus(now), isCs),
              ),
              if canManage then
                div.cssClass("mp-action-bar")(
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(state.togglePromoActive(p.id, !p.isActive))(
                      if p.isActive then state.i18n.t("adm-pri-promo-deactivate") else state.i18n.t("adm-pri-promo-activate")
                    ),
                  button
                    .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                    .onClick(
                      dialog.open(
                        ConfirmDialog.Config(
                          title = if isCs then "Smazat slevový kód" else "Delete Promo Code",
                          message = if isCs then s"Opravdu chcete smazat slevový kód „${p.code.value}“?" else s"Delete promo code “${p.code.value}”?",
                          confirmLabel = if isCs then "Smazat" else "Delete",
                          cancelLabel = if isCs then "Zrušit" else "Cancel",
                          tone = ConfirmDialog.DialogTone.Danger,
                          onConfirm = _ => state.deletePromoCode(p.id),
                        )
                      )
                    )(state.i18n.t("adm-cat-delete")),
                )
              else UI.empty,
            ),
            div.style(rowWrap)(
              span(s"${if isCs then "Vytvořeno" else "Created"}: ${formatInstant(p.createdAt)}").style(smallText),
              p.constraints.validFrom.map(v => span(s"· ${if isCs then "Od" else "From"}: ${formatInstant(v)}").style(smallText)).getOrElse(UI.empty),
              p.constraints.validUntil.map(v => span(s"· ${if isCs then "Do" else "Until"}: ${formatInstant(v)}").style(smallText)).getOrElse(UI.empty),
              p.constraints.minimumOrderNet.map(m => badge(s"Min net: ${m.value}", BadgeTone.Amber)).getOrElse(UI.empty),
              if p.constraints.allowedSegments.nonEmpty then badge(s"Segments: ${p.constraints.allowedSegments.mkString(", ")}", BadgeTone.Neutral) else UI.empty,
              if p.constraints.allowedCategories.nonEmpty then badge(s"Categories: ${p.constraints.allowedCategories.map(_.value).mkString(", ")}", BadgeTone.Neutral) else UI.empty,
            ),
          )
        },
        emptyMessage = if isCs then "Žádné slevové kódy neodpovídají filtru." else "No promo codes match the current filter.",
        lang = lang,
      ),
    )

  private def renderAgreements(
      state: BackofficeState,
      agreements: List[CustomerPriceAgreement],
      customers: List[CustomerView],
      focusCustOpt: Option[CustomerId],
      tableState: SignalRef[DataTable.State[String]],
      dialog: ConfirmDialog.Handle,
      canManage: Boolean,
      targetKindRef: SignalRef[String],
      tierRef: SignalRef[CustomerTier],
      custIdRef: SignalRef[String],
      curRef: SignalRef[Currency],
      discRef: SignalRef[String],
      minOrdRef: SignalRef[String],
      editKeyRef: SignalRef[Option[String]],
      editDiscRef: SignalRef[String],
      editMinRef: SignalRef[String],
      lang: Language,
  ): UI =
    val isCs = lang == Language.Cs
    val customerById = customers.map(c => c.id -> c).toMap

    def targetDisplay(t: AgreementTarget): String = t match
      case AgreementTarget.TierDefault(tier) => s"Tier: $tier"
      case AgreementTarget.SpecificCustomer(cid) =>
        customerById.get(cid) match
          case Some(c) =>
            val n = c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")
            s"$n (${cid.value})"
          case None => s"Customer: ${cid.value}"

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
        numericSortKey = Some(a => BigDecimal(a.categoryDiscounts.size + a.materialDiscounts.size + a.finishDiscounts.size)),
        alignRight = true,
        cell = a =>
          val total = a.categoryDiscounts.size + a.materialDiscounts.size + a.finishDiscounts.size
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

      // Create / Upsert Agreement card
      if canManage then
        div.cssClass("mp-card")(
          h2(state.i18n.t("adm-pri-agr-create")).style(sectionTitle.margin(0.px)),
          targetKindRef.render { kind =>
            div.cssClass("mp-field-row")(
              div.cssClass("mp-field")(
                label(if isCs then "Typ cíle" else "Target Kind").style(labelStyle),
                select.cssClass("mp-input").value(kind).onChange(v => targetKindRef.set(v))(
                  option.value("tier")(if isCs then "Cenová hladina (Tier)" else "Customer Tier"),
                  option.value("customer")(if isCs then "Konkrétní zákazník" else "Specific Customer"),
                ),
              ),
              if kind == "customer" then
                div.cssClass("mp-field")(
                  label(if isCs then "Zákazník" else "Customer").style(labelStyle),
                  custIdRef.render { selCid =>
                    val effectiveCid =
                      if selCid.nonEmpty then selCid
                      else focusCustOpt.map(_.value).orElse(customers.headOption.map(_.id.value)).getOrElse("")
                    select.cssClass("mp-input").value(effectiveCid).onChange(v => custIdRef.set(v))(
                      customers.map { c =>
                        val nm = c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")
                        option.value(c.id.value)(s"$nm (${c.id.value})")
                      }*
                    )
                  },
                )
              else
                div.cssClass("mp-field")(
                  label(if isCs then "Cenová hladina" else "Target Tier").style(labelStyle),
                  tierRef.render { t =>
                    select
                      .cssClass("mp-input")
                      .value(t.toString)
                      .onChange(v => CustomerTier.values.find(_.toString == v).map(tierRef.set).getOrElse(Kyo.unit))(
                        CustomerTier.values.toList.map(x => option.value(x.toString)(x.toString))*
                      )
                  },
                ),
              div.cssClass("mp-field")(
                label(if isCs then "Měna" else "Currency").style(labelStyle),
                curRef.render { c =>
                  select
                    .cssClass("mp-input")
                    .value(c.toString)
                    .onChange(v => Currency.values.find(_.toString == v).map(curRef.set).getOrElse(Kyo.unit))(
                      List(Currency.CZK, Currency.USD).map(x => option.value(x.toString)(x.toString))*
                    )
                },
              ),
              div.cssClass("mp-field")(
                label(if isCs then "Globální sleva (%)" else "Global Discount (%)").style(labelStyle),
                numberInput.cssClass("mp-input").value(discRef).onInput(v => discRef.set(v)),
              ),
              div.cssClass("mp-field")(
                label(if isCs then "Min. obj. bez DPH (volit.)" else "Min Order Net (opt.)").style(labelStyle),
                numberInput.cssClass("mp-input").placeholder("—").value(minOrdRef).onInput(v => minOrdRef.set(v)),
              ),
            )
          },
          div.style(rowSm)(
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .onClick {
                for
                  kind <- targetKindRef.get
                  tier <- tierRef.get
                  cidS <- custIdRef.get.map(_.trim)
                  cur  <- curRef.get
                  dStr <- discRef.get.map(_.trim)
                  mStr <- minOrdRef.get.map(_.trim)
                  pct   = scala.util.Try(BigDecimal(dStr)).getOrElse(BigDecimal(10)).min(BigDecimal(80)).max(BigDecimal(0))
                  minO  = scala.util.Try(BigDecimal(mStr)).toOption.filter(_ > 0).map(Money.apply)
                  target =
                    if kind == "customer" then
                      val chosen =
                        if cidS.nonEmpty then cidS
                        else focusCustOpt.map(_.value).orElse(customers.headOption.map(_.id.value)).getOrElse("cust-unknown")
                      AgreementTarget.SpecificCustomer(CustomerId.unsafe(chosen))
                    else AgreementTarget.TierDefault(tier)
                  existing = agreements.find(x => x.target == target && x.currency == cur)
                  agr = existing match
                    case Some(ex) => ex.copy(globalDiscount = Some(Percentage.unsafe(pct)), minimumOrderOverride = minO)
                    case None =>
                      CustomerPriceAgreement(
                        target = target,
                        currency = cur,
                        globalDiscount = Some(Percentage.unsafe(pct)),
                        minimumOrderOverride = minO,
                      )
                  _ <- state.saveAgreement(agr)
                yield ()
              }(state.i18n.t("adm-pri-agr-create")),
          ),
        )
      else UI.empty,

      // Agreements Universal DataTable
      DataTable.render[CustomerPriceAgreement, String](
        rows = visibleAgreements,
        rowKey = agreementKey,
        columns = columns,
        state = tableState,
        searchPlaceholder = if isCs then "Hledat zákazníka, hladinu nebo měnu..." else "Search customer, tier, or currency...",
        matchesSearch = (a, q) =>
          targetDisplay(a.target).toLowerCase.contains(q) || a.currency.toString.toLowerCase.contains(q),
        filterGroups = filterGroups,
        renderDetail = Some { a =>
          val key = agreementKey(a)
          editKeyRef.combineLatest(editDiscRef).combineLatest(editMinRef).render { case ((ekOpt, eDisc), eMin) =>
            val isEditing = ekOpt.contains(key)
            div.style(stackSm)(
              div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                div.style(rowSm.flexWrap(FlexWrap.wrap))(
                  span(targetDisplay(a.target)).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                  badge(a.currency.toString, BadgeTone.Neutral),
                ),
                if canManage then
                  div.cssClass("mp-action-bar")(
                    if !isEditing then
                      button
                        .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                        .onClick(
                          editKeyRef.set(Some(key))
                            .andThen(editDiscRef.set(a.globalDiscount.map(_.value.toString).getOrElse("0")))
                            .andThen(editMinRef.set(a.minimumOrderOverride.map(_.value.toString).getOrElse("")))
                        )(state.i18n.t("adm-pri-agr-edit-global"))
                    else UI.empty,
                    button
                      .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                      .onClick(
                        dialog.open(
                          ConfirmDialog.Config(
                            title = if isCs then "Smazat cenovou smlouvu" else "Delete Price Agreement",
                            message = if isCs then s"Smazat smlouvu pro ${targetDisplay(a.target)} (${a.currency})?" else s"Delete agreement for ${targetDisplay(a.target)} (${a.currency})?",
                            confirmLabel = if isCs then "Smazat" else "Delete",
                            cancelLabel = if isCs then "Zrušit" else "Cancel",
                            tone = ConfirmDialog.DialogTone.Danger,
                            onConfirm = _ => state.deleteAgreement(a.target, a.currency),
                          )
                        )
                      )(state.i18n.t("adm-cat-delete")),
                  )
                else UI.empty,
              ),

              // Inline Global Discount & Min Order editor
              if isEditing && canManage then
                div.style(Style.column.gap(8.px).padding(12.px).rounded(10.px).bg(surface).border(1.px, brandBorder))(
                  div.cssClass("mp-field-row")(
                    div.cssClass("mp-field")(
                      label(if isCs then "Globální sleva (%)" else "Global Discount (%)").style(labelStyle),
                      numberInput.cssClass("mp-input").value(eDisc).onInput(v => editDiscRef.set(v)),
                    ),
                    div.cssClass("mp-field")(
                      label(if isCs then "Min. objednávka bez DPH (prázdné = bez limitu)" else "Min Order Net Override (blank = none)").style(labelStyle),
                      numberInput.cssClass("mp-input").value(eMin).onInput(v => editMinRef.set(v)),
                    ),
                  ),
                  div.style(rowSm)(
                    button
                      .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                      .onClick {
                        val pctOpt = scala.util.Try(BigDecimal(eDisc.trim)).toOption.filter(_ > 0).map(p => Percentage.unsafe(p.min(BigDecimal(80))))
                        val minOpt = scala.util.Try(BigDecimal(eMin.trim)).toOption.filter(_ > 0).map(Money.apply)
                        state.saveAgreement(a.copy(globalDiscount = pctOpt, minimumOrderOverride = minOpt)).andThen(editKeyRef.set(None))
                      }(state.i18n.t("adm-cat-save")),
                    button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(editKeyRef.set(None))(state.i18n.t("adm-cat-cancel")),
                  ),
                )
              else UI.empty,

              // Read-only view of scoped Category / Material / Finish discounts
              div.style(stackXs)(
                span(state.i18n.t("adm-pri-agr-scoped-readonly")).style(labelStyle),
                if a.categoryDiscounts.isEmpty && a.materialDiscounts.isEmpty && a.finishDiscounts.isEmpty && a.fixedMaterialRates.isEmpty then
                  span(if isCs then "Žádné specifické slevy pro kategorie, materiály ani zušlechtění." else "No category, material, or finish overrides configured.").style(smallText)
                else
                  div.style(stackXs)(
                    if a.categoryDiscounts.nonEmpty then
                      div.style(rowWrap)(
                        span(if isCs then "Kategorie:" else "Categories:").style(smallText.fontWeight(FontWeight.w700)),
                        UI.fragment(a.categoryDiscounts.toList.sortBy(_._1.value).map { case (cid, pct) => badge(s"${cid.value}: −${pct.value}%", BadgeTone.Brand) }*),
                      )
                    else UI.empty,
                    if a.materialDiscounts.nonEmpty then
                      div.style(rowWrap)(
                        span(if isCs then "Materiály:" else "Materials:").style(smallText.fontWeight(FontWeight.w700)),
                        UI.fragment(a.materialDiscounts.toList.sortBy(_._1.value).map { case (mid, pct) => badge(s"${mid.value}: −${pct.value}%", BadgeTone.Emerald) }*),
                      )
                    else UI.empty,
                    if a.finishDiscounts.nonEmpty then
                      div.style(rowWrap)(
                        span(if isCs then "Zušlechtění:" else "Finishes:").style(smallText.fontWeight(FontWeight.w700)),
                        UI.fragment(a.finishDiscounts.toList.sortBy(_._1.value).map { case (fid, pct) => badge(s"${fid.value}: −${pct.value}%", BadgeTone.Amber) }*),
                      )
                    else UI.empty,
                    if a.fixedMaterialRates.nonEmpty then
                      div.style(rowWrap)(
                        span(if isCs then "Fixní sazby materiálů:" else "Fixed Material Rates:").style(smallText.fontWeight(FontWeight.w700)),
                        UI.fragment(a.fixedMaterialRates.toList.sortBy(_._1.value).map { case (mid, rm) => badge(s"${mid.value}: $rm", BadgeTone.Neutral) }*),
                      )
                    else UI.empty,
                  ),
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
        renderDetail = Some { r =>
          div.style(stackXs)(
            div.style(rowWrap)(
              badge(s"${r.pricelistId.value} v${r.version.value}", BadgeTone.Brand),
              badge(r.entry.id.value, BadgeTone.Neutral),
              badge(r.entry.phase.toString, BadgeTone.Neutral),
            ),
            span(s"${if isCs then "Cíl" else "Target"}: ${r.entry.target}").style(Style.fontSize(13.px).color(ink)),
            span(s"${if isCs then "Rozsah (Scope)" else "Scope"}: ${r.entry.scope}").style(smallText),
            span(s"${if isCs then "Cenový model" else "Rate Model"}: ${r.entry.rate}").style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(brand)),
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
