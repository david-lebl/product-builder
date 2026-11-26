package mpbuilder.admin.views.customers

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.customers.*
import mpbuilder.ordering.{OrderStatus, OrderView, PaymentStatus}
import mpbuilder.shared.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Shared formatting, form-field and card helpers for the Customer Manager screens. */
private[views] object CustomerUi:

  def personName(cp: ContactPerson): String = s"${cp.firstName.value} ${cp.lastName.value}"

  def displayName(c: CustomerView): String = c.company.map(_.legalName.value).getOrElse(personName(c.contact))

  def formatAddress(a: Address): String =
    s"${a.street.value}, ${a.postalCode.value} ${a.city.value}, ${a.countryCode.value}"

  def isB2B(segment: CustomerSegment): Boolean =
    segment == CustomerSegment.Business || segment == CustomerSegment.Agency

  def statusTone(s: CustomerStatus): BadgeTone = s match
    case CustomerStatus.Active          => BadgeTone.Emerald
    case CustomerStatus.PendingApproval => BadgeTone.Amber
    case CustomerStatus.Suspended       => BadgeTone.Rose
    case CustomerStatus.Deactivated     => BadgeTone.Neutral

  def statusLabel(s: CustomerStatus, lang: Language): String = (s, lang) match
    case (CustomerStatus.Active, Language.Cs)          => "Aktivní"
    case (CustomerStatus.Active, _)                   => "Active"
    case (CustomerStatus.PendingApproval, Language.Cs) => "Čeká na schválení"
    case (CustomerStatus.PendingApproval, _)          => "Pending approval"
    case (CustomerStatus.Suspended, Language.Cs)       => "Pozastaven"
    case (CustomerStatus.Suspended, _)                => "Suspended"
    case (CustomerStatus.Deactivated, Language.Cs)     => "Deaktivován"
    case (CustomerStatus.Deactivated, _)              => "Deactivated"

  def segmentLabel(s: CustomerSegment, lang: Language): String = (s, lang) match
    case (CustomerSegment.Business, Language.Cs)   => "Firma"
    case (CustomerSegment.Business, _)             => "Business"
    case (CustomerSegment.Agency, Language.Cs)     => "Agentura"
    case (CustomerSegment.Agency, _)               => "Agency"
    case (CustomerSegment.Individual, Language.Cs) => "Jednotlivec"
    case (CustomerSegment.Individual, _)           => "Individual"

  def termsSummary(t: PaymentTerms, lang: Language): String = t match
    case PaymentTerms.PrepaidOnly => if lang == Language.Cs then "Platba předem" else "Prepaid only"
    case PaymentTerms.NetInvoice(d, lim) =>
      val due = if lang == Language.Cs then s"Faktura $d dní" else s"Net $d days"
      s"$due · ${formatMoney(lim.amount, lim.currency, lang)}"

  /** Latest audit summary for an action (e.g. the reason recorded with the last suspension). */
  def latestAudit(c: CustomerView, action: String): Option[CustomerAuditEntry] =
    c.auditLog.filter(_.action == action).maxByOption(_.occurredAt)

  // ── Order statistics ───────────────────────────────────────────────────────

  final case class OrderStats(
      count: Int,
      openCount: Int,
      lifetime: List[(Currency, Money)],
      unpaid: List[(Currency, Money)],
      lastOrder: Option[OrderView],
  )

  private def isCancelled(o: OrderView): Boolean = o.status match
    case OrderStatus.Cancelled(_) => true
    case _                        => false

  private def sumByCurrency(orders: List[OrderView]): List[(Currency, Money)] =
    orders
      .groupMapReduce(_.totals.currency)(_.totals.grandTotalGross)(_ + _)
      .toList
      .sortBy(_._1.ordinal)

  def orderStats(orders: List[OrderView]): OrderStats =
    val live = orders.filterNot(isCancelled)
    OrderStats(
      count = orders.size,
      openCount = live.count(o => !o.status.isTerminal),
      lifetime = sumByCurrency(live),
      unpaid = sumByCurrency(live.filter(o => o.payment.status != PaymentStatus.Confirmed && o.payment.status != PaymentStatus.Refunded)),
      lastOrder = orders.maxByOption(_.placedAt),
    )

  def formatAmounts(amounts: List[(Currency, Money)], lang: Language): String =
    if amounts.isEmpty then "—"
    else amounts.map { case (cur, m) => formatMoney(m, cur, lang) }.mkString(" · ")

  def formatDate(i: java.time.Instant): String = formatInstant(i).take(10)

  // ── Form fields ────────────────────────────────────────────────────────────

  enum FieldKind:
    case Text, Email, Phone, Number

  /** Labelled input with required marker, inline error message and optional hint. */
  def field(
      lbl: UI,
      value: String,
      onChange: String => Unit < Async,
      error: Option[String] = None,
      required: Boolean = false,
      placeholder: String = "",
      kind: FieldKind = FieldKind.Text,
      hint: Option[String] = None,
  ): UI =
    val cls     = if error.isDefined then "mp-input mp-input-invalid" else "mp-input"
    val invalid = error.isDefined.toString
    val control: UI = kind match
      case FieldKind.Email  => emailInput.cssClass(cls).value(value).placeholder(placeholder).aria("invalid", invalid).onInput(onChange)
      case FieldKind.Phone  => telInput.cssClass(cls).value(value).placeholder(placeholder).aria("invalid", invalid).onInput(onChange)
      case FieldKind.Number => numberInput.cssClass(cls).value(value).placeholder(placeholder).aria("invalid", invalid).onInput(onChange)
      case FieldKind.Text   => input.cssClass(cls).value(value).placeholder(placeholder).aria("invalid", invalid).onInput(onChange)
    div.cssClass("mp-field")(
      label(
        lbl,
        if required then span(" *").cssClass("mp-field-required") else UI.empty,
      ).style(smallText.fontWeight(FontWeight.w600).color(inkSecondary)),
      control,
      error match
        case Some(msg) => span(msg).cssClass("mp-field-error")
        case None      => hint.map(h => span(h).style(smallText)).getOrElse(UI.empty),
    )

  /** Rose summary box listing validation problems after a failed save attempt. */
  def errorSummary(errors: Iterable[String], lang: Language): UI =
    if errors.isEmpty then UI.empty
    else
      div.cssClass("mp-cust-error-box").role("alert")(
        div.style(rowXs)(
          Icons(Icon.AlertTriangle, 15),
          span(if lang == Language.Cs then "Opravte prosím zvýrazněná pole:" else "Please fix the highlighted fields:")
            .style(Style.fontWeight(FontWeight.w700)),
        ),
        ul.style(Style.margin(0.px).padding(0.px, 0.px, 0.px, 18.px))(errors.toList.distinct.map(e => li(e))*),
      )

  // ── Cards ──────────────────────────────────────────────────────────────────

  /** Titled section card with an icon and a right-aligned slot for dedicated action buttons. */
  def sectionCard(icon: Icon, title: UI, actions: UI = UI.empty, editing: Boolean = false)(body: UI*): UI =
    div.cssClass(if editing then "mp-cust-card mp-cust-card-editing" else "mp-cust-card")(
      div.cssClass("mp-cust-card-head")(
        div.style(rowSm.minWidth(0.px))(
          span(Icons(icon, 15)).cssClass("mp-cust-card-icon"),
          h3(title).style(Style.margin(0.px).fontSize(14.px).fontWeight(FontWeight.w800).color(ink)),
        ),
        div.style(rowXs.flexShrink(0.0).flexWrap(FlexWrap.wrap))(actions),
      ),
      UI.fragment(body*),
    )

  /** Small ghost button with a leading icon, used for in-card actions (Edit, Add, Remove...). */
  def iconAction(icon: Icon, text: UI, onClick: => Any < Async, tone: String = "mp-btn-ghost"): UI =
    button.cssClass(s"mp-btn $tone mp-btn-sm mp-cust-card-btn").onClick(onClick)(Icons(icon, 14), text)

  def formActions(saveLabel: UI, cancelLabel: UI, onSave: => Any < Async, onCancel: => Any < Async): UI =
    div.cssClass("mp-cust-form-actions")(
      button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(onSave)(Icons(Icon.Check, 14), saveLabel),
      button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(onCancel)(cancelLabel),
    )

  def emptyHint(text: String): UI = span(text).cssClass("mp-cust-empty")

end CustomerUi
