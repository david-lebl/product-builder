package mpbuilder.admin.views.customers

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.admin.views.customers.CustomerDrafts.*
import mpbuilder.admin.views.customers.CustomerUi.*
import mpbuilder.customers.*
import mpbuilder.pricing.{AgreementTarget, CustomerPriceAgreement}
import mpbuilder.shared.*
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Pricing & Terms tab: pricing tier (with confirm step + agreement preview), payment terms editor with eligibility
  * rules and credit usage, and the custom / tier-default price agreements that apply to the customer.
  */
private[views] object CustomerCommercialTab:

  final case class Refs(
      pendingTier: SignalRef[Option[(CustomerId, CustomerTier)]],
      terms: SignalRef[Option[TermsDraft]],
  )

  def render(
      state: BackofficeState,
      c: CustomerView,
      agreements: List[CustomerPriceAgreement],
      stats: OrderStats,
      refs: Refs,
      canManage: Boolean,
      lang: Language,
  ): UI =
    val isCs    = lang == Language.Cs
    val canEdit = canManage && c.status != CustomerStatus.Deactivated
    def t(k: String): UI = span(state.i18n.t(k))

    val customAgreements = agreements.filter(_.target == AgreementTarget.SpecificCustomer(c.id))
    def tierAgreements(tier: CustomerTier) = agreements.filter(_.target == AgreementTarget.TierDefault(tier))

    def agreementBadges(a: CustomerPriceAgreement): UI =
      val overrideCount = a.categoryDiscounts.size + a.materialDiscounts.size + a.finishDiscounts.size
      div.style(rowWrap)(
        badge(a.currency.toString, BadgeTone.Neutral),
        a.globalDiscount
          .map(d => badge(s"−${d.value}% ${if isCs then "globálně" else "global"}", BadgeTone.Emerald))
          .getOrElse(badge(if isCs then "Bez globální slevy" else "No global discount", BadgeTone.Neutral)),
        a.minimumOrderOverride.map(m => badge(s"Min ${formatMoney(m, a.currency, lang)}", BadgeTone.Amber)).getOrElse(UI.empty),
        if overrideCount > 0 then badge(s"$overrideCount ${if isCs then "položkových slev" else "scoped overrides"}", BadgeTone.Neutral) else UI.empty,
      )

    // ── Tier ─────────────────────────────────────────────────────────────────
    val tierCard: UI =
      sectionCard(Icon.Star, t("adm-cust-tier-label"))(
        p(
          if isCs then "Hladina určuje, která výchozí cenová smlouva pro hladinu se na zákazníka uplatní."
          else "The tier decides which tier-default price agreement applies to this customer.",
        ).style(mutedText.margin(0.px)),
        refs.pendingTier.render { pendingOpt =>
          val pending  = pendingOpt.collect { case (id, tier) if id == c.id => tier }.filter(_ != c.tier)
          val selected = pending.getOrElse(c.tier)
          val preview  = tierAgreements(selected)
          div.style(stackSm)(
            if canEdit then
              Controls.segmented[CustomerTier](
                options = CustomerTier.values.toList.map(tier => tier -> tierBadge(tier, compact = true)),
                current = selected,
                onSelect = tier => refs.pendingTier.set(Option.when(tier != c.tier)(c.id -> tier)),
                ariaLabel = if isCs then "Cenová hladina" else "Pricing tier",
              )
            else tierBadge(c.tier),
            div.cssClass("mp-cust-inline-note")(
              Icons(Icon.Info, 14),
              span(
                if preview.isEmpty then
                  if isCs then s"Hladina $selected nemá výchozí smlouvu — platí veřejný ceník."
                  else s"$selected has no tier-default agreement — the public pricelist applies."
                else if isCs then s"Výchozí smlouva hladiny $selected:"
                else s"$selected tier default:"
              ),
              UI.fragment(preview.map(agreementBadges)*),
            ),
            pending match
              case Some(next) =>
                div.cssClass("mp-cust-confirm-row")(
                  span(
                    if isCs then s"Změnit hladinu z ${c.tier} na $next? Změna se zapíše do auditu."
                    else s"Change tier from ${c.tier} to $next? This is recorded in the audit log.",
                  ).style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(ink).flexGrow(1.0)),
                  div.style(rowXs)(
                    button
                      .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                      .onClick(
                        state.changeCustomerTier(c.id, next).map(ok => (if ok then refs.pendingTier.set(None) else Kyo.unit): Unit < Async)
                      )(Icons(Icon.Check, 14), t("adm-cust-apply-tier")),
                    button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(refs.pendingTier.set(None))(t("adm-cat-cancel")),
                  ),
                )
              case None => UI.empty,
          )
        },
      )

    // ── Payment terms ────────────────────────────────────────────────────────
    val netEligible = c.status == CustomerStatus.Active && isB2B(c.segment)
    val ineligibleReason: Option[String] =
      if !isB2B(c.segment) then
        Some(if isCs then "Faktura se splatností je dostupná jen pro firemní a agenturní účty." else "Invoice terms are only available to Business and Agency accounts.")
      else if c.status != CustomerStatus.Active then
        Some(
          if isCs then s"Fakturu se splatností lze povolit až aktivnímu účtu (nyní: ${statusLabel(c.status, lang)})."
          else s"Invoice terms can only be granted to active accounts (currently: ${statusLabel(c.status, lang)}).",
        )
      else None

    val creditUsage: UI = c.paymentTerms match
      case PaymentTerms.NetInvoice(_, lim) =>
        val used = stats.unpaid.collectFirst { case (cur, m) if cur == lim.currency => m.value }.getOrElse(BigDecimal(0))
        val pct  = if lim.amount.value > 0 then (used / lim.amount.value * 100).toDouble else 0.0
        val tone = if pct > 100 then "mp-cust-meter-over" else if pct >= 80 then "mp-cust-meter-warn" else "mp-cust-meter-ok"
        div.style(stackXs)(
          div.style(rowBetween)(
            span(t("adm-cust-credit-usage")).style(labelStyle),
            span(s"${formatMoney(Money(used), lim.currency, lang)} / ${formatMoney(lim.amount, lim.currency, lang)}")
              .cssClass("mp-num")
              .style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(ink)),
          ),
          div.cssClass("mp-cust-meter").role("meter").aria("valuenow", f"$pct%.0f").aria("valuemin", "0").aria("valuemax", "100")(
            div.cssClass(s"mp-cust-meter-fill $tone").style(Style.width(pct.min(100.0).max(0.0).pct))()
          ),
          span(
            if isCs then f"Neuhrazené objednávky čerpají $pct%.0f %% limitu." else f"Unpaid orders use $pct%.0f%% of the credit limit."
          ).style(smallText),
        )
      case PaymentTerms.PrepaidOnly => UI.empty

    val termsCard: UI =
      sectionCard(Icon.Wallet, t("adm-cust-terms-label"))(
        div.style(rowWrap)(
          span(if isCs then "Aktuálně:" else "Current:").style(smallText),
          badge(
            termsSummary(c.paymentTerms, lang),
            if c.paymentTerms == PaymentTerms.PrepaidOnly then BadgeTone.Neutral else BadgeTone.Emerald,
          ),
        ),
        creditUsage,
        ineligibleReason match
          case Some(reason) =>
            div.style(stackSm)(
              div.cssClass("mp-cust-inline-note")(Icons(Icon.Info, 14), span(reason)),
              c.paymentTerms match
                case PaymentTerms.NetInvoice(_, _) if canEdit =>
                  div.style(rowSm)(
                    button
                      .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                      .onClick(state.grantCustomerPaymentTerms(c.id, PaymentTerms.PrepaidOnly))(t("adm-cust-terms-prepaid")),
                  )
                case _ => UI.empty,
            )
          case None if !canEdit => UI.empty
          case None =>
            refs.terms.render { draftOpt =>
              val original = TermsDraft.from(c)
              val d        = draftOpt.filter(_.customerId == c.id).getOrElse(original)
              val errs     = validateTerms(d, isCs)
              val shown    = if d.showErrors then errs else Map.empty[String, String]
              val dirty    = !d.sameAs(original)
              def upd(f: TermsDraft => TermsDraft): Unit < Async = refs.terms.set(Some(f(d)))
              div.style(stackMd)(
                Controls.segmented[TermsKind](
                  options = List(
                    TermsKind.Prepaid -> t("adm-cust-terms-prepaid"),
                    TermsKind.Net     -> t("adm-cust-terms-net"),
                  ),
                  current = d.kind,
                  onSelect = k => upd(_.copy(kind = k)),
                  fullWidth = true,
                  ariaLabel = if isCs then "Typ platebních podmínek" else "Payment terms type",
                ),
                if d.kind == TermsKind.Net then
                  div.style(stackSm)(
                    div.cssClass("mp-field-row")(
                      field(t("adm-cust-f-due"), d.dueDays, v => upd(_.copy(dueDays = v)), shown.get("dueDays"), required = true, kind = FieldKind.Number),
                      field(t("adm-cust-f-limit"), d.limit, v => upd(_.copy(limit = v)), shown.get("limit"), required = true, kind = FieldKind.Number),
                      div.cssClass("mp-field")(
                        label(t("adm-cust-f-currency")).style(smallText.fontWeight(FontWeight.w600).color(inkSecondary)),
                        select
                          .cssClass("mp-input")
                          .value(d.currency.toString)
                          .onChange(v => Currency.values.find(_.toString == v).map(cur => upd(_.copy(currency = cur))).getOrElse(Kyo.unit))(
                            Currency.values.toList.map(cur => option.value(cur.toString).selected(cur == d.currency)(cur.toString))*
                          ),
                      ),
                    ),
                    div.style(rowWrap)(
                      span(if isCs then "Rychlá volba splatnosti:" else "Quick due days:").style(smallText),
                      UI.fragment(
                        List(7, 14, 30, 60).map { n =>
                          button
                            .cssClass(if d.dueDays.trim == n.toString then "mp-pill-active" else "mp-pill")
                            .onClick(upd(_.copy(dueDays = n.toString)))(s"$n")
                        }*
                      ),
                    ),
                  )
                else
                  span(
                    if isCs then "Zákazník platí objednávky předem (karta, převod) — bez úvěru."
                    else "Customer pays every order upfront (card or bank transfer) — no credit.",
                  ).style(smallText),
                if dirty then
                  div.cssClass("mp-cust-form-actions")(
                    button
                      .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                      .onClick(
                        if errs.nonEmpty then upd(_.copy(showErrors = true))
                        else
                          state
                            .grantCustomerPaymentTerms(c.id, termsValue(d))
                            .map(ok => (if ok then refs.terms.set(None) else Kyo.unit): Unit < Async)
                      )(Icons(Icon.Check, 14), t("adm-cust-save-terms")),
                    button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(refs.terms.set(None))(t("adm-cust-reset")),
                  )
                else UI.empty,
              )
            },
      )

    // ── Agreements ───────────────────────────────────────────────────────────
    val inherited = tierAgreements(c.tier)
    val agreementsCard: UI =
      sectionCard(
        Icon.Percent,
        t("adm-cust-agreements-title"),
        if canManage then
          button
            .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
            .onClick(state.openCustomerPricing(c.id))(
              t(if customAgreements.isEmpty then "adm-cust-create-agreement" else "adm-cust-manage-pricing"),
              Icons(Icon.ArrowRight, 14),
            )
        else UI.empty,
      )(
        div.style(stackSm)(
          span(if isCs then "Individuální smlouvy" else "Customer-specific").style(labelStyle),
          if customAgreements.isEmpty then
            emptyHint(if isCs then "Žádná individuální smlouva." else "No customer-specific agreement.")
          else
            div.style(stackXs)(customAgreements.map(a => div.cssClass("mp-cust-agreement")(badge(if isCs then "Individuální" else "Custom", BadgeTone.Brand), agreementBadges(a)))*),
        ),
        div.style(stackSm)(
          span(if isCs then s"Zděděno z hladiny ${c.tier}" else s"Inherited from ${c.tier} tier").style(labelStyle),
          if inherited.isEmpty then
            emptyHint(if isCs then "Hladina nemá výchozí smlouvu — platí veřejný ceník." else "Tier has no default agreement — public pricelist applies.")
          else div.style(stackXs)(inherited.map(a => div.cssClass("mp-cust-agreement")(tierBadge(c.tier, compact = true), agreementBadges(a)))*),
        ),
        if customAgreements.nonEmpty then
          span(
            if isCs then "Individuální smlouva má přednost před výchozí smlouvou hladiny ve stejné měně."
            else "A customer-specific agreement takes precedence over the tier default in the same currency.",
          ).style(smallText)
        else UI.empty,
      )

    div.style(stackMd)(tierCard, termsCard, agreementsCard)

end CustomerCommercialTab
