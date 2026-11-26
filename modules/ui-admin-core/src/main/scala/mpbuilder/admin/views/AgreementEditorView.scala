package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.pricing.{AgreementDraft, AgreementJson}
import mpbuilder.admin.pricing.AgreementDraft.{DiscountRow, FixedRateRow, Issue, RateKind, Section, TargetKind}
import mpbuilder.admin.state.BackofficeState
import mpbuilder.catalog.ProductCatalog
import mpbuilder.customers.CustomerView
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.Controls
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Full B2B price agreement editor: a structured form (target, global terms, category / material /
  * finish discounts, fixed material rates) and a JSON mode covering every field of the aggregate,
  * including custom volume curves and advanced rate models the form only displays.
  */
object AgreementEditorView:

  enum Mode:
    case Form, Json

  /** Mutable editor state; one session per editor instance (create panel, detail panel). */
  final class Session(
      val draft: SignalRef[AgreementDraft],
      val mode: SignalRef[Mode],
      val json: SignalRef[String],
      val issues: SignalRef[List[Issue]],
  ):
    def load(d: AgreementDraft): Unit < Async =
      draft.set(d).andThen(mode.set(Mode.Form)).andThen(json.set("")).andThen(issues.set(Nil))

  object Session:
    def make(initial: AgreementDraft): Session < Sync =
      for
        d <- Signal.initRef(initial)
        m <- Signal.initRef[Mode](Mode.Form)
        j <- Signal.initRef("")
        i <- Signal.initRef[List[Issue]](Nil)
      yield Session(d, m, j, i)

  private final case class Choice(id: String, label: String)

  // Every `<select>` marks its current `<option>` as `selected`: Kyo UI re-renders regions as HTML
  // strings and browsers ignore a `value` attribute on `<select>`, so without it the control snaps
  // back to its first option after each re-render (i.e. right after a pick).

  private def customerName(c: CustomerView): String =
    c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")

  /** Resolves the draft (form or JSON mode) into an agreement, or the issues preventing it. */
  def resolve(session: Session, defaultCustomerId: Option[String]): Either[List[Issue], CustomerPriceAgreement] < Sync =
    for
      mode  <- session.mode.get
      draft <- session.draft.get
      json  <- session.json.get
    yield mode match
      case Mode.Form =>
        val d =
          if draft.targetKind == TargetKind.Customer && draft.customerId.trim.isEmpty then
            draft.copy(customerId = defaultCustomerId.getOrElse(""))
          else draft
        d.toAgreement
      case Mode.Json =>
        AgreementJson.decode(json).left.map(e => List(Issue(s"Invalid JSON: $e", s"Neplatný JSON: $e")))

  def render(
      state: BackofficeState,
      session: Session,
      catalog: Option[ProductCatalog],
      customers: List[CustomerView],
      existing: List[CustomerPriceAgreement],
      original: Option[CustomerPriceAgreement],
      defaultCustomerId: Option[String],
      lang: Language,
      onDone: () => Unit < Async,
  ): UI =
    val isCs = lang == Language.Cs

    val categoryChoices = catalog.toList.flatMap(_.categories.values).map(c => Choice(c.id.value, c.name(lang))).sortBy(_.label)
    val materialChoices = catalog.toList.flatMap(_.materials.values).map(m => Choice(m.id.value, m.name(lang))).sortBy(_.label)
    val finishChoices   = catalog.toList.flatMap(_.finishes.values).map(f => Choice(f.id.value, f.name(lang))).sortBy(_.label)
    def choicesFor(s: Section) = s match
      case Section.Category => categoryChoices
      case Section.Material => materialChoices
      case Section.Finish   => finishChoices

    val customerDefault = defaultCustomerId.orElse(customers.headOption.map(_.id.value))

    def switchMode(target: Mode): Unit < Async =
      session.mode.get.flatMap { current =>
        if current == target then Kyo.unit
        else
          resolve(session, customerDefault).flatMap {
            case Left(is) => session.issues.set(is)
            case Right(agr) =>
              val sync: Unit < Async = target match
                case Mode.Json => session.json.set(AgreementJson.encode(agr))
                case Mode.Form => session.draft.set(AgreementDraft.from(agr))
              sync.andThen(session.issues.set(Nil)).andThen(session.mode.set(target))
          }
      }

    def save: Unit < Async =
      resolve(session, customerDefault).flatMap {
        case Left(is) => session.issues.set(is)
        case Right(agr) =>
          val newKey  = AgreementDraft.keyOf(agr)
          val origKey = original.map(AgreementDraft.keyOf)
          val unknownCustomer = agr.target match
            case AgreementTarget.SpecificCustomer(cid) => !customers.exists(_.id == cid)
            case _                                     => false
          if !origKey.contains(newKey) && existing.exists(x => AgreementDraft.keyOf(x) == newKey) then
            session.issues.set(
              List(
                Issue(
                  "An agreement for this target and currency already exists — edit that one instead.",
                  "Smlouva pro tento cíl a měnu už existuje — upravte raději ji.",
                )
              )
            )
          else if unknownCustomer then
            session.issues.set(List(Issue("The agreement targets an unknown customer.", "Smlouva odkazuje na neznámého zákazníka.")))
          else
            state.replaceAgreement(original.map(o => (o.target, o.currency)), agr).flatMap { ok =>
              if ok then session.issues.set(Nil).andThen(onDone()) else Kyo.unit
            }
      }

    // ── Small building blocks ────────────────────────────────────────────────

    def decimalInput(value: String, placeholder: String)(onInput: String => Unit < Async): UI =
      input.cssClass("mp-input").inputFilter(InputFilter.Decimal).placeholder(placeholder).value(value).onInput(onInput).onChange(onInput)

    def choiceSelect(current: String, choices: List[Choice], placeholder: String)(onChange: String => Unit < Async): UI =
      val known  = choices.exists(_.id == current)
      val extra  = if current.nonEmpty && !known then List(Choice(current, s"$current (${if isCs then "není v katalogu" else "not in catalog"})")) else Nil
      select.cssClass("mp-input").value(current).onChange(onChange)(
        option.value("").selected(current.isEmpty)(placeholder),
        UI.fragment((extra ++ choices).map(c => option.value(c.id).selected(c.id == current)(s"${c.label} · ${c.id}"))*),
      )

    def removeButton(onClick: Unit < Async): UI =
      button
        .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
        .aria("label", if isCs then "Odebrat" else "Remove")
        .onClick(onClick)("✕")

    def sectionBox(title: String, hint: String)(body: UI*): UI =
      div.style(Style.column.gap(10.px).padding(14.px).rounded(12.px).bg(surface).border(1.px, Palette.border))(
        div.style(stackXs)(
          span(title).style(Style.fontSize(14.px).fontWeight(FontWeight.w700).color(ink)),
          if hint.nonEmpty then span(hint).style(smallText) else UI.empty,
        ),
        UI.fragment(body*),
      )

    val rowStyle = Style.row.gap(8.px).align(Alignment.center).flexWrap(FlexWrap.wrap)

    def update(f: AgreementDraft => AgreementDraft): Unit < Async = session.draft.updateAndGet(f).unit

    // ── Form sections ────────────────────────────────────────────────────────

    def targetSection(d: AgreementDraft): UI =
      val currencies = Currency.values.toList
      sectionBox(
        if isCs then "Cíl smlouvy" else "Agreement target",
        if isCs then "Smlouva pro konkrétního zákazníka má přednost před výchozí smlouvou jeho hladiny."
        else "A customer-specific agreement takes precedence over the default for the customer's tier.",
      )(
        div.cssClass("mp-field-row")(
          div.cssClass("mp-field")(
            label(if isCs then "Typ cíle" else "Target kind").style(labelStyle),
            select.cssClass("mp-input").value(d.targetKind.toString).onChange(v => update(_.copy(targetKind = TargetKind.valueOf(v))))(
              option.value(TargetKind.Tier.toString).selected(d.targetKind == TargetKind.Tier)(if isCs then "Cenová hladina (Tier)" else "Customer tier"),
              option.value(TargetKind.Customer.toString).selected(d.targetKind == TargetKind.Customer)(if isCs then "Konkrétní zákazník" else "Specific customer"),
            ),
          ),
          d.targetKind match
            case TargetKind.Customer =>
              val current = if d.customerId.nonEmpty then d.customerId else customerDefault.getOrElse("")
              div.cssClass("mp-field")(
                label(if isCs then "Zákazník" else "Customer").style(labelStyle),
                select.cssClass("mp-input").value(current).onChange(v => update(_.copy(customerId = v)))(
                  customers.sortBy(customerName).map(c => option.value(c.id.value).selected(c.id.value == current)(s"${customerName(c)} (${c.id.value})"))*
                ),
              )
            case TargetKind.Tier =>
              div.cssClass("mp-field")(
                label(if isCs then "Cenová hladina" else "Tier").style(labelStyle),
                select.cssClass("mp-input").value(d.tier.toString).onChange(v => update(_.copy(tier = CustomerTier.valueOf(v))))(
                  CustomerTier.values.toList.map(t => option.value(t.toString).selected(t == d.tier)(t.toString))*
                ),
              ),
          div.cssClass("mp-field")(
            label(if isCs then "Měna" else "Currency").style(labelStyle),
            select.cssClass("mp-input").value(d.currency.toString).onChange(v => update(_.copy(currency = Currency.valueOf(v))))(
              currencies.map(c => option.value(c.toString).selected(c == d.currency)(c.toString))*
            ),
          ),
        ),
      )

    def globalSection(d: AgreementDraft): UI =
      sectionBox(
        if isCs then "Globální podmínky" else "Global terms",
        if isCs then "Globální sleva platí pro všechny proměnné položky, pokud ji nepřebije specifičtější sleva. Jednorázové přípravné poplatky se nezlevňují."
        else "The global discount applies to every variable charge unless a more specific discount overrides it. One-time setup fees are never discounted.",
      )(
        div.cssClass("mp-field-row")(
          div.cssClass("mp-field")(
            label(if isCs then "Globální sleva (%)" else "Global discount (%)").style(labelStyle),
            decimalInput(d.globalDiscount, "—")(v => update(_.copy(globalDiscount = v))),
          ),
          div.cssClass("mp-field")(
            label(if isCs then s"Min. objednávka bez DPH (${d.currency})" else s"Minimum order net (${d.currency})").style(labelStyle),
            decimalInput(d.minimumOrder, if isCs then "dle ceníku" else "pricelist default")(v => update(_.copy(minimumOrder = v))),
          ),
        ),
      )

    def discountSection(d: AgreementDraft, s: Section): UI =
      val (title, hint, addLabel, pick) = s match
        case Section.Category =>
          (
            if isCs then "Slevy na kategorie produktů" else "Product category discounts",
            if isCs then "Přebíjí globální slevu pro všechny položky produktu v dané kategorii." else "Overrides the global discount for every charge of products in that category.",
            if isCs then "+ Přidat kategorii" else "+ Add category",
            if isCs then "Vyberte kategorii…" else "Select category…",
          )
        case Section.Material =>
          (
            if isCs then "Slevy na materiály" else "Material discounts",
            if isCs then "Přebíjí slevu kategorie i globální slevu u ceny materiálu." else "Overrides category and global discounts on the material charge.",
            if isCs then "+ Přidat materiál" else "+ Add material",
            if isCs then "Vyberte materiál…" else "Select material…",
          )
        case Section.Finish =>
          (
            if isCs then "Slevy na zušlechtění" else "Finish discounts",
            if isCs then "Přebíjí slevu kategorie i globální slevu u ceny zušlechtění." else "Overrides category and global discounts on the finish charge.",
            if isCs then "+ Přidat zušlechtění" else "+ Add finish",
            if isCs then "Vyberte zušlechtění…" else "Select finish…",
          )
      val rows = d.section(s)
      sectionBox(title, hint)(
        (rows.zipWithIndex.map { case (row, i) =>
          div.style(rowStyle)(
            div.style(Style.flexGrow(1).minWidth(200.px))(
              choiceSelect(row.key, choicesFor(s), pick)(v => update(_.updateSection(s)(_.updated(i, row.copy(key = v))))),
            ),
            div.style(Style.row.gap(6.px).align(Alignment.center).width(130.px))(
              decimalInput(row.percent, "%")(v => update(_.updateSection(s)(_.updated(i, row.copy(percent = v))))),
              span("%").style(smallText),
            ),
            removeButton(update(_.updateSection(s)(rs => rs.patch(i, Nil, 1)))),
          )
        } :+
          div(
            button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(update(_.updateSection(s)(_ :+ DiscountRow())))(addLabel)
          ))*
      )

    def fixedRatesSection(d: AgreementDraft): UI =
      sectionBox(
        if isCs then "Fixní sazby materiálů" else "Fixed material rates",
        if isCs then s"Nahrazuje ceníkovou sazbu materiálu (v ${d.currency}); má přednost před všemi procentními slevami."
        else s"Replaces the pricelist material rate (in ${d.currency}); takes precedence over every percentage discount.",
      )(
        (d.fixedMaterialRates.zipWithIndex.map { case (row, i) =>
          def set(r: FixedRateRow) = update(x => x.copy(fixedMaterialRates = x.fixedMaterialRates.updated(i, r)))
          div.style(rowStyle)(
            div.style(Style.flexGrow(1).minWidth(200.px))(
              choiceSelect(row.materialId, materialChoices, if isCs then "Vyberte materiál…" else "Select material…")(v => set(row.copy(materialId = v))),
            ),
            row.kind match
              case RateKind.Advanced =>
                span(
                  s"${row.advanced.map(_.productPrefix).getOrElse("?")} — ${if isCs then "upravte v režimu JSON" else "edit in JSON mode"}"
                ).style(smallText.fontWeight(FontWeight.w600))
              case kind =>
                UI.fragment(
                  div.style(Style.width(160.px))(
                    select.cssClass("mp-input").value(kind.toString).onChange(v => set(row.copy(kind = RateKind.valueOf(v))))(
                      RateKind.editable.map(k => option.value(k.toString).selected(k == kind)(RateKind.label(k, isCs)))*
                    )
                  ),
                  div.style(Style.row.gap(6.px).align(Alignment.center).width(150.px))(
                    decimalInput(row.amount, "0.00")(v => set(row.copy(amount = v))),
                    span(d.currency.toString).style(smallText),
                  ),
                ),
            removeButton(update(x => x.copy(fixedMaterialRates = x.fixedMaterialRates.patch(i, Nil, 1)))),
          )
        } :+
          div(
            button
              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
              .onClick(update(x => x.copy(fixedMaterialRates = x.fixedMaterialRates :+ FixedRateRow())))(
                if isCs then "+ Přidat fixní sazbu" else "+ Add fixed rate"
              )
          ))*
      )

    def volumeSection(d: AgreementDraft): UI =
      sectionBox(
        if isCs then "Vlastní množstevní křivky" else "Custom volume curves",
        if isCs then "Pokud jsou nastaveny, zcela nahrazují množstevní slevy ceníku. Upravují se v režimu JSON."
        else "When set, they fully replace the pricelist volume discounts. Edit them in JSON mode.",
      )(
        d.customVolumeCurves match
          case None => span(if isCs then "Používají se množstevní křivky ceníku." else "Uses the pricelist volume curves.").style(smallText)
          case Some(curves) =>
            div.style(stackSm)(
              UI.fragment(curves.map(c => volumeCurveSummary(c, catalog, lang))*),
              div(
                button
                  .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                  .onClick(update(_.copy(customVolumeCurves = None)))(
                    if isCs then "Vrátit na křivky ceníku" else "Revert to pricelist curves"
                  )
              ),
            ),
      )

    // ── Assembly ─────────────────────────────────────────────────────────────

    session.mode.combineLatest(session.issues).render { case (mode, issues) =>
      div.style(stackMd)(
        div.style(rowBetween.flexWrap(FlexWrap.wrap))(
          Controls.segmented[Mode](
            Seq(Mode.Form -> span(if isCs then "Formulář" else "Form"), Mode.Json -> span("JSON")),
            mode,
            m => switchMode(m),
            ariaLabel = if isCs then "Režim editoru" else "Editor mode",
          ),
          span(
            if isCs then "Priorita: fixní sazba > materiál / zušlechtění > kategorie > globální"
            else "Precedence: fixed rate > material / finish > category > global"
          ).style(smallText),
        ),
        if issues.nonEmpty then
          alertBanner(BadgeTone.Rose, span(if isCs then "Smlouvu nelze uložit" else "Cannot save agreement"), issues.map(_.text(isCs)))
        else UI.empty,
        mode match
          case Mode.Form =>
            session.draft.render { d =>
              div.style(stackMd)(
                targetSection(d),
                globalSection(d),
                discountSection(d, Section.Category),
                discountSection(d, Section.Material),
                discountSection(d, Section.Finish),
                fixedRatesSection(d),
                volumeSection(d),
              )
            }
          case Mode.Json =>
            div.style(stackSm)(
              span(
                if isCs then "Celá smlouva ve formátu API (/api/v1/backoffice/pricing/agreements). Hodnoty se ověřují stejně jako na serveru."
                else "The whole agreement in its API wire format (/api/v1/backoffice/pricing/agreements). Values are validated exactly like on the server."
              ).style(smallText),
              textarea
                .cssClass("mp-input")
                .value(session.json)
                .aria("label", "JSON")
                .style(Style.minHeight(420.px).fontFamily(FontFamily.Monospace).fontSize(12.px).lineHeight(1.5)),
              div.style(stackXs)(
                span(if isCs then "Referenční tvary hodnot" else "Value shapes").style(smallText.fontWeight(FontWeight.w700)),
                UI.fragment(
                  List(
                    """"currency": {"CZK": {}}  ·  "target": {"TierDefault": {"tier": {"Gold": {}}}}""",
                    """"categoryDiscounts": {"<category-id>": "12.5"}  (0–100 %)""",
                    """"fixedMaterialRates": {"<material-id>": {"PerSheet": {"rate": "10"}}}  ·  PerUnit, PerSqMeter, PerLinearMeter, PerPerimeterMeter, FlatFee {"amount"}, TieredArea, ByDiscreteParam""",
                    """"customVolumeCurves": [{"scope": {"categoryId": "<id>"}, "basis": {"FinishedUnits": {}}, "tiers": [{"minThreshold": "1", "maxThreshold": "99", "multiplier": "0.95"}]}]  ·  TotalPressSheets, TotalAreaSqM""",
                    """"customVolumeCurves": null  (= pricelist curves)""",
                  ).map(t => code(t).style(Style.fontSize(11.px).color(inkSecondary).fontFamily(FontFamily.Monospace)))*
                ),
              ),
            ),
        div.style(rowSm)(
          button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(save)(state.i18n.t("adm-pri-agr-create")),
          button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(session.issues.set(Nil).andThen(onDone()))(state.i18n.t("adm-cat-cancel")),
        ),
      )
    }

  // ── Read-only presentation ─────────────────────────────────────────────────

  def describeRate(rm: RateModel, currency: Currency, lang: Language): String =
    val isCs = lang == Language.Cs
    def m(x: Money) = formatMoney(x, currency, lang)
    rm match
      case RateModel.PerUnit(r)              => s"${m(r)} / ${if isCs then "ks" else "unit"}"
      case RateModel.PerSheet(r)             => s"${m(r)} / ${if isCs then "arch" else "sheet"}"
      case RateModel.PerSqMeter(r, minA)     => s"${m(r)} / m²" + minA.map(a => s" (min ${a.value} m²)").getOrElse("")
      case RateModel.PerLinearMeter(r)       => s"${m(r)} / ${if isCs then "bm" else "lin. m"}"
      case RateModel.PerPerimeterMeter(r)    => s"${m(r)} / ${if isCs then "m obvodu" else "perimeter m"}"
      case RateModel.FlatFee(a)              => s"${m(a)} ${if isCs then "paušál" else "flat"}"
      case RateModel.TieredArea(tiers, _)    => s"${if isCs then "Pásma plochy" else "Area tiers"} (${tiers.size})"
      case RateModel.ByDiscreteParam(map, _) => s"${if isCs then "Tabulka" else "Lookup table"} (${map.size})"

  def volumeCurveSummary(c: VolumeCurve, catalog: Option[ProductCatalog], lang: Language): UI =
    val isCs = lang == Language.Cs
    val scope = List(
      c.scope.categoryId.map(id => catalog.flatMap(_.categories.get(id)).map(_.name(lang)).getOrElse(id.value)),
      c.scope.printingMethodId.map(id => catalog.flatMap(_.printingMethods.get(id)).map(_.name(lang)).getOrElse(id.value)),
    ).flatten match
      case Nil => if isCs then "Vše" else "All products"
      case xs  => xs.mkString(" · ")
    val basis = c.basis match
      case VolumeBasis.FinishedUnits    => if isCs then "kusy" else "units"
      case VolumeBasis.TotalPressSheets => if isCs then "archy" else "sheets"
      case VolumeBasis.TotalAreaSqM     => "m²"
    div.style(rowWrap)(
      span(s"$scope ($basis):").style(smallText.fontWeight(FontWeight.w700)),
      UI.fragment(c.tiers.toList.map { t =>
        val range = s"${t.minThreshold}${t.maxThreshold.map(x => s"–$x").getOrElse("+")}"
        badge(s"$range × ${t.multiplier}", BadgeTone.Neutral)
      }*),
    )

  /** Read-only overview of every agreement rule, with catalog names resolved. */
  def renderSummary(a: CustomerPriceAgreement, catalog: Option[ProductCatalog], lang: Language): UI =
    val isCs = lang == Language.Cs
    def catName(id: CategoryId) = catalog.flatMap(_.categories.get(id)).map(_.name(lang)).getOrElse(id.value)
    def matName(id: MaterialId) = catalog.flatMap(_.materials.get(id)).map(_.name(lang)).getOrElse(id.value)
    def finName(id: FinishId)   = catalog.flatMap(_.finishes.get(id)).map(_.name(lang)).getOrElse(id.value)

    def group(title: String, items: List[UI]): UI =
      if items.isEmpty then UI.empty
      else div.style(stackXs)(span(title).style(smallText.fontWeight(FontWeight.w700)), div.style(rowWrap)(UI.fragment(items*)))

    val nothing =
      a.globalDiscount.isEmpty && a.minimumOrderOverride.isEmpty && a.categoryDiscounts.isEmpty && a.materialDiscounts.isEmpty &&
        a.finishDiscounts.isEmpty && a.fixedMaterialRates.isEmpty && a.customVolumeCurves.isEmpty

    div.style(stackSm)(
      if nothing then span(if isCs then "Smlouva zatím nemá žádná pravidla." else "This agreement has no rules yet.").style(smallText)
      else UI.empty,
      group(
        if isCs then "Kategorie" else "Categories",
        a.categoryDiscounts.toList.sortBy((k, _) => catName(k)).map((k, p) => badge(s"${catName(k)}: −${p.value}%", BadgeTone.Brand)),
      ),
      group(
        if isCs then "Materiály" else "Materials",
        a.materialDiscounts.toList.sortBy((k, _) => matName(k)).map((k, p) => badge(s"${matName(k)}: −${p.value}%", BadgeTone.Emerald)),
      ),
      group(
        if isCs then "Zušlechtění" else "Finishes",
        a.finishDiscounts.toList.sortBy((k, _) => finName(k)).map((k, p) => badge(s"${finName(k)}: −${p.value}%", BadgeTone.Amber)),
      ),
      group(
        if isCs then "Fixní sazby materiálů" else "Fixed material rates",
        a.fixedMaterialRates.toList.sortBy((k, _) => matName(k)).map((k, rm) => badge(s"${matName(k)}: ${describeRate(rm, a.currency, lang)}", BadgeTone.Neutral)),
      ),
      a.customVolumeCurves match
        case Some(curves) =>
          div.style(stackXs)(
            span(if isCs then "Vlastní množstevní křivky" else "Custom volume curves").style(smallText.fontWeight(FontWeight.w700)),
            UI.fragment(curves.map(c => volumeCurveSummary(c, catalog, lang))*),
          )
        case None => UI.empty,
    )
