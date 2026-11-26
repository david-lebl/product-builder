package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.backoffice.CustomerArtworkEntry
import mpbuilder.customers.*
import mpbuilder.pricing.AgreementTarget
import mpbuilder.shared.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{ActionMenu, ArtworkComponents, ConfirmDialog, DataTable, TabBar}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Customer Accounts & B2B Approvals Manager:
  *   - Vaadin-style Master-Detail DataTable with resizable splitter & create-in-detail
  *   - Single-line detail header with collapsed `ActionMenu` dropdown (Approve, Edit, Suspend, Deactivate)
  *     and status/tier/ID badges cleanly below the headline
  *   - Single-column scrollable detail pane organized into compact sub-tabs:
  *     Overview (Profile/Contacts/Addresses/Tier & Terms), Pricing & Orders, Artworks, and Notes & Audit
  */
object CustomerManagerView:

  private enum DetailTab:
    case Overview, PricingOrders, Artworks, NotesAudit

  private final case class ContactDraft(
      firstName: String,
      lastName: String,
      email: String,
      phone: String,
      roleTitle: String,
  )

  private final case class EditDraft(
      customerId: CustomerId,
      companyName: String,
      regNo: String,
      vatId: String,
      mainFirst: String,
      mainLast: String,
      mainEmail: String,
      mainPhone: String,
      mainRole: String,
      additionalContacts: List[ContactDraft],
      street: String,
      city: String,
      postalCode: String,
      countryCode: String,
  )

  private object EditDraft:
    def fromCustomer(c: CustomerView): EditDraft = EditDraft(
      customerId = c.id,
      companyName = c.company.map(_.legalName.value).getOrElse(""),
      regNo = c.company.map(_.registrationNumber.value).getOrElse(""),
      vatId = c.company.flatMap(_.vatId.map(_.value)).getOrElse(""),
      mainFirst = c.contact.firstName.value,
      mainLast = c.contact.lastName.value,
      mainEmail = c.contact.email.value,
      mainPhone = c.contact.phone.map(_.value).getOrElse(""),
      mainRole = c.contact.roleTitle.map(_.value).getOrElse(""),
      additionalContacts = c.additionalContacts.map { cp =>
        ContactDraft(
          firstName = cp.firstName.value,
          lastName = cp.lastName.value,
          email = cp.email.value,
          phone = cp.phone.map(_.value).getOrElse(""),
          roleTitle = cp.roleTitle.map(_.value).getOrElse(""),
        )
      },
      street = c.billingAddress.street.value,
      city = c.billingAddress.city.value,
      postalCode = c.billingAddress.postalCode.value,
      countryCode = c.billingAddress.countryCode.value,
    )

  private def statusTone(s: CustomerStatus): BadgeTone = s match
    case CustomerStatus.Active          => BadgeTone.Emerald
    case CustomerStatus.PendingApproval => BadgeTone.Amber
    case CustomerStatus.Suspended       => BadgeTone.Rose
    case CustomerStatus.Deactivated     => BadgeTone.Neutral

  private def actorLabel(a: CustomerActor, lang: Language): String = a match
    case CustomerActor.System                    => "System"
    case CustomerActor.CustomerSelf(_, name)     => s"${if lang == Language.Cs then "Zákazník" else "Customer"}: ${name.value}"
    case CustomerActor.Employee(id, nameOpt)     => s"${if lang == Language.Cs then "Zaměstnanec" else "Staff"}: ${nameOpt.map(_.value).getOrElse(id.value)}"

  def render(state: BackofficeState): UI < Sync =
    for
      tableState     <- DataTable.makeState[CustomerId](defaultSort = Some(DataTable.SortSpec("name", ascending = true)), pageSize = 10)
      dialog         <- ConfirmDialog.Handle.make
      actionMenu     <- ActionMenu.Handle.make
      detailTabRef   <- Signal.initRef[DetailTab](DetailTab.Overview)
      regIdRef       <- Signal.initRef("")
      regSegRef      <- Signal.initRef[CustomerSegment](CustomerSegment.Business)
      regCompRef     <- Signal.initRef("")
      regIcoRef      <- Signal.initRef("")
      regFirstRef    <- Signal.initRef("")
      regLastRef     <- Signal.initRef("")
      regEmailRef    <- Signal.initRef("")
      regCityRef     <- Signal.initRef("Prague")
      // Inline profile & contacts editor
      editDraftRef   <- Signal.initRef[Option[EditDraft]](None)
      // Detail controls
      dueDaysRef     <- Signal.initRef("14")
      creditLimitRef <- Signal.initRef("100000")
      noteRef        <- Signal.initRef("")
    yield
      val combined =
        state.customers
          .combineLatest(state.agreements)
          .combineLatest(state.orders)
          .combineLatest(state.selectedCustomer)
          .combineLatest(editDraftRef)
          .combineLatest(detailTabRef)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      UI.fragment(
        combined.render { case (((((((allCustomers, allAgreements), allOrders), selectedDetailOpt), editDraftOpt), activeDetailTab), authOpt), lang) =>
          val isCs      = lang == Language.Cs
          val canManage = state.canManage(authOpt)

          val columns: List[DataTable.Column[CustomerView]] = List(
            DataTable.Column[CustomerView](
              id = "name",
              header = if isCs then "Zákazník / Firma" else "Customer / Company",
              sortKey = Some(c => c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")),
              cell = c =>
                val title = c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")
                div.style(Style.column.gap(2.px))(
                  span(title).style(Style.fontWeight(FontWeight.w700).color(ink)),
                  span(s"${c.contact.firstName.value} ${c.contact.lastName.value} · ${c.contact.email.value}").style(smallText),
                ),
            ),
            DataTable.Column[CustomerView](
              id = "segment",
              header = if isCs then "Segment" else "Segment",
              sortKey = Some(_.segment.toString),
              cell = c => badge(c.segment.toString, BadgeTone.Neutral),
            ),
            DataTable.Column[CustomerView](
              id = "tier",
              header = if isCs then "Cenová hladina" else "Tier",
              numericSortKey = Some(c => BigDecimal(c.tier.ordinal)),
              cell = c => tierBadge(c.tier),
            ),
            DataTable.Column[CustomerView](
              id = "status",
              header = if isCs then "Stav" else "Status",
              sortKey = Some(_.status.toString),
              cell = c => badge(c.status.toString, statusTone(c.status)),
            ),
            DataTable.Column[CustomerView](
              id = "terms",
              header = if isCs then "Platební podmínky" else "Payment Terms",
              sortKey = Some(_.paymentTerms.toString),
              cell = c =>
                c.paymentTerms match
                  case PaymentTerms.NetInvoice(d, lim) =>
                    div.style(Style.column.gap(1.px))(
                      badge(s"Net-$d", BadgeTone.Emerald),
                      span(formatMoney(lim.amount, lim.currency, lang)).style(smallText),
                    )
                  case PaymentTerms.PrepaidOnly =>
                    badge(if isCs then "Předem" else "Prepaid", BadgeTone.Neutral),
            ),
            DataTable.Column[CustomerView](
              id = "contacts",
              header = if isCs then "Kontakty" else "Contacts",
              numericSortKey = Some(c => BigDecimal(1 + c.additionalContacts.size)),
              alignRight = true,
              cell = c => span(s"${1 + c.additionalContacts.size}").style(Style.fontWeight(FontWeight.w700).color(inkSecondary)),
            ),
          )

          val filterGroups: List[DataTable.FilterGroup[CustomerView]] = List(
            DataTable.FilterGroup[CustomerView](
              id = "status",
              label = if isCs then "Stav účtu" else "Status",
              options = CustomerStatus.values.toList.map { st =>
                DataTable.FilterOption[CustomerView](st.toString, st.toString, _.status == st)
              },
            ),
            DataTable.FilterGroup[CustomerView](
              id = "tier",
              label = if isCs then "Cenová hladina" else "Tier",
              options = CustomerTier.values.toList.map { t =>
                DataTable.FilterOption[CustomerView](t.toString, t.toString, _.tier == t)
              },
            ),
          )

          def renderCustomerDetail(c: CustomerView): UI =
            val customerOrders = allOrders.filter(_.customer.customerId.contains(c.id))
            val matchingAgreements = allAgreements.filter { a =>
              a.target match
                case AgreementTarget.SpecificCustomer(cid) => cid == c.id
                case AgreementTarget.TierDefault(t)        => t == c.tier
            }
            val isEditing = editDraftOpt.exists(_.customerId == c.id)
            val title     = c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")

            val menuItems: List[ActionMenu.Item] =
              if !canManage then Nil
              else
                val approveItem =
                  if c.status == CustomerStatus.PendingApproval || c.status == CustomerStatus.Suspended then
                    List(
                      ActionMenu.Item(
                        label = span(state.i18n.t("adm-cust-approve")),
                        tone = ActionMenu.ItemTone.Primary,
                        onClick = () =>
                          dialog.open(
                            ConfirmDialog.Config(
                              title = if isCs then "Schválit zákaznický účet" else "Approve Customer Account",
                              message =
                                if isCs then s"Opravdu chcete schválit a aktivovat účet „$title“?"
                                else s"Approve and activate customer account “$title”?",
                              confirmLabel = if isCs then "Schválit" else "Approve",
                              cancelLabel = if isCs then "Zrušit" else "Cancel",
                              tone = ConfirmDialog.DialogTone.Primary,
                              onConfirm = _ => state.approveCustomer(c.id),
                            )
                          ),
                      )
                    )
                  else Nil

                val editItem = List(
                  ActionMenu.Item(
                    label =
                      if isEditing then span(if isCs then "Zavřít úpravu profilu" else "Close Profile Editor")
                      else span(state.i18n.t("adm-cust-edit-profile")),
                    onClick = () =>
                      if isEditing then editDraftRef.set(None)
                      else detailTabRef.set(DetailTab.Overview).andThen(editDraftRef.set(Some(EditDraft.fromCustomer(c)))),
                  ),
                  ActionMenu.Item(
                    label = span(state.i18n.t("adm-cust-edit-in-pricing")),
                    onClick = () => state.openCustomerPricing(c.id),
                  ),
                )

                val suspendItem =
                  if c.status == CustomerStatus.Active then
                    List(
                      ActionMenu.Item(
                        label = span(state.i18n.t("adm-cust-suspend")),
                        tone = ActionMenu.ItemTone.Amber,
                        dividerBefore = true,
                        onClick = () =>
                          dialog.open(
                            ConfirmDialog.Config(
                              title = if isCs then "Pozastavit zákaznický účet" else "Suspend Customer Account",
                              message =
                                if isCs then s"Pozastavit účet „$title“? Tato akce bude zaznamenána v auditu."
                                else s"Suspend account “$title”? This action will be recorded in the audit log.",
                              confirmLabel = if isCs then "Pozastavit účet" else "Suspend Account",
                              cancelLabel = if isCs then "Zrušit" else "Cancel",
                              tone = ConfirmDialog.DialogTone.Amber,
                              reasonLabel = Some(if isCs then "Důvod pozastavení" else "Suspension reason"),
                              reasonPlaceholder = Some(if isCs then "Např. neuhrazené faktury po splatnosti..." else "e.g. Overdue invoices..."),
                              reasonRequired = true,
                              reasonErrorText = if isCs then "Zadejte důvod pozastavení." else "Please enter a suspension reason.",
                              onConfirm = reasonOpt => state.suspendCustomer(c.id, reasonOpt.getOrElse("")),
                            )
                          ),
                      )
                    )
                  else Nil

                val deactivateItem =
                  if c.status != CustomerStatus.Deactivated then
                    List(
                      ActionMenu.Item(
                        label = span(state.i18n.t("adm-cust-deactivate")),
                        tone = ActionMenu.ItemTone.Danger,
                        dividerBefore = suspendItem.isEmpty,
                        onClick = () =>
                          dialog.open(
                            ConfirmDialog.Config(
                              title = if isCs then "Deaktivovat zákaznický účet" else "Deactivate Customer Account",
                              message =
                                if isCs then s"Trvale deaktivovat účet „$title“? Tato akce je nevratná a bude zaznamenána v auditu."
                                else s"Permanently deactivate account “$title”? This will be recorded in the audit log.",
                              confirmLabel = if isCs then "Deaktivovat" else "Deactivate",
                              cancelLabel = if isCs then "Zrušit" else "Cancel",
                              tone = ConfirmDialog.DialogTone.Danger,
                              reasonLabel = Some(if isCs then "Důvod deaktivace" else "Deactivation reason"),
                              reasonPlaceholder = Some(if isCs then "Např. ukončení spolupráce..." else "e.g. Account closed at customer request..."),
                              reasonRequired = true,
                              reasonErrorText = if isCs then "Zadejte důvod deaktivace." else "Please enter a deactivation reason.",
                              onConfirm = reasonOpt => state.deactivateCustomer(c.id, reasonOpt.getOrElse("")),
                            )
                          ),
                      )
                    )
                  else Nil

                approveItem ++ editItem ++ suspendItem ++ deactivateItem

            val customerArtworks: List[CustomerArtworkEntry] =
              selectedDetailOpt.filter(_.customer.id == c.id).map(_.artworks).getOrElse(Nil)

            val detailTabs = List(
              TabBar.TabItem(DetailTab.Overview, if isCs then "Přehled" else "Overview", None),
              TabBar.TabItem(
                DetailTab.PricingOrders,
                if isCs then "Ceny a objednávky" else "Pricing & Orders",
                Some(customerOrders.size + matchingAgreements.size),
              ),
              TabBar.TabItem(
                DetailTab.Artworks,
                if isCs then "Tisková data" else "Artworks",
                Some(customerArtworks.size),
              ),
              TabBar.TabItem(
                DetailTab.NotesAudit,
                if isCs then "Poznámky a audit" else "Notes & Audit",
                Some(c.notes.size + c.auditLog.size),
              ),
            )

            div.style(stackMd.width(100.pct))(
              // Detail Header: Row 1 = Headline + single Actions dropdown; Row 2 = Tags below headline
              div.cssClass("mp-detail-head")(
                div.cssClass("mp-detail-head-row")(
                  h2(title).style(Style.margin(0.px).fontSize(19.px).fontWeight(FontWeight.w800).color(ink).minWidth(0.px).flexGrow(1.0)),
                  actionMenu.render(
                    key = s"cust-${c.id.value}",
                    buttonLabel = span(if isCs then "Akce" else "Actions"),
                    items = menuItems,
                  ),
                ),
                div.cssClass("mp-detail-tags-row")(
                  badge(c.status.toString, statusTone(c.status)),
                  tierBadge(c.tier),
                  badge(c.segment.toString, BadgeTone.Neutral),
                  badge(c.id.value, BadgeTone.Neutral),
                ),
                c.company match
                  case Some(comp) =>
                    span(
                      s"IČO: ${comp.registrationNumber.value}${comp.vatId.map(v => s" · DIČ: ${v.value}").getOrElse("")}",
                    ).style(smallText)
                  case None => UI.empty,
              ),

              // Compact inner tabs for clean single-column navigation
              TabBar.render[DetailTab](
                items = detailTabs,
                active = activeDetailTab,
                onSelect = t => detailTabRef.set(t),
                compact = true,
              ),

              // Single-column scrollable tab content
              activeDetailTab match
                case DetailTab.Overview =>
                  div.style(stackMd)(
                    // Inline Profile & Multi-Contact Editor (when toggled from Actions menu)
                    if isEditing && canManage then
                      editDraftOpt match
                        case Some(draft) => renderProfileEditor(state, c, draft, editDraftRef, isCs)
                        case None        => UI.empty
                    else UI.empty,

                    // Contact Persons Section
                    div.style(stackSm)(
                      div.style(rowBetween)(
                        h3(state.i18n.t("adm-cust-contacts-title")).style(sectionTitle.margin(0.px)),
                        badge(s"${1 + c.additionalContacts.size}", BadgeTone.Neutral),
                      ),
                      div.style(Style.column.gap(3.px).padding(10.px, 12.px).rounded(10.px).bg(brandSoft).border(1.px, brandBorder))(
                        div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                          span(s"${c.contact.firstName.value} ${c.contact.lastName.value}").style(Style.fontWeight(FontWeight.w700).color(ink)),
                          div.style(rowXs)(
                            badge(if isCs then "Hlavní kontakt" else "Primary Contact", BadgeTone.Brand),
                            c.contact.roleTitle.map(r => badge(r.value, BadgeTone.Neutral)).getOrElse(UI.empty),
                          ),
                        ),
                        span(s"${c.contact.email.value}${c.contact.phone.map(p => s" · ${p.value}").getOrElse("")}").style(smallText),
                      ),
                      if c.additionalContacts.nonEmpty then
                        div.style(stackXs)(
                          c.additionalContacts.map { cp =>
                            div.style(Style.column.gap(3.px).padding(10.px, 12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                              div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                                span(s"${cp.firstName.value} ${cp.lastName.value}").style(Style.fontWeight(FontWeight.w700).color(ink)),
                                cp.roleTitle.map(r => badge(r.value, BadgeTone.Neutral)).getOrElse(UI.empty),
                              ),
                              span(s"${cp.email.value}${cp.phone.map(p => s" · ${p.value}").getOrElse("")}").style(smallText),
                            )
                          }*
                        )
                      else UI.empty,
                    ),

                    // Billing & Shipping Addresses
                    div.style(Style.column.gap(8.px).padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                      div.style(Style.column.gap(2.px))(
                        span(if isCs then "Fakturační adresa" else "Billing Address").style(labelStyle),
                        span(
                          s"${c.billingAddress.street.value}, ${c.billingAddress.postalCode.value} ${c.billingAddress.city.value}, ${c.billingAddress.countryCode.value}",
                        ).style(smallText),
                      ),
                      if c.shippingAddresses.nonEmpty then
                        div.style(stackXs)(
                          span(if isCs then "Doručovací adresy" else "Delivery Addresses").style(labelStyle),
                          UI.fragment(
                            c.shippingAddresses.zipWithIndex.map { case (addr, idx) =>
                              val isDef = c.defaultShippingIndex.getOrElse(0) == idx
                              div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                span(s"${addr.street.value}, ${addr.postalCode.value} ${addr.city.value}, ${addr.countryCode.value}").style(smallText),
                                if isDef then badge(if isCs then "Výchozí" else "Default", BadgeTone.Emerald) else UI.empty,
                              )
                            }*
                          ),
                        )
                      else UI.empty,
                    ),

                    // Tier & Payment Terms (Manager/Admin)
                    if canManage then
                      div.style(Style.column.gap(12.px).padding(14.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(stackXs)(
                          label(state.i18n.t("adm-cust-tier-label")).style(labelStyle),
                          div.style(rowWrap)(
                            CustomerTier.values.toList.map { t =>
                              button
                                .cssClass(if c.tier == t then "mp-pill-active" else "mp-pill")
                                .onClick(state.changeCustomerTier(c.id, t))(
                                  tierBadge(t, compact = true),
                                )
                            }*
                          ),
                        ),
                        div.style(Style.column.gap(8.px).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                          label(state.i18n.t("adm-cust-terms-label")).style(labelStyle),
                          div.style(smallText)(
                            c.paymentTerms match
                              case PaymentTerms.PrepaidOnly =>
                                if isCs then "Aktuálně: Pouze platba předem" else "Current: Prepaid Only"
                              case PaymentTerms.NetInvoice(d, lim) =>
                                s"${if isCs then "Aktuálně" else "Current"}: Net-$d · Limit ${formatMoney(lim.amount, lim.currency, lang)}"
                          ),
                          div.cssClass("mp-field-row")(
                            div.cssClass("mp-field")(
                              label(if isCs then "Splatnost (dny)" else "Due Days").style(smallText),
                              numberInput.cssClass("mp-input").value(dueDaysRef).onInput(v => dueDaysRef.set(v)),
                            ),
                            div.cssClass("mp-field")(
                              label(if isCs then "Kreditní limit (CZK)" else "Credit Limit (CZK)").style(smallText),
                              numberInput.cssClass("mp-input").value(creditLimitRef).onInput(v => creditLimitRef.set(v)),
                            ),
                          ),
                          div.style(rowWrap)(
                            button
                              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                              .onClick {
                                for
                                  dStr <- dueDaysRef.get.map(_.trim)
                                  lStr <- creditLimitRef.get.map(_.trim)
                                  days  = dStr.toIntOption.filter(_ > 0).getOrElse(14)
                                  lim   = scala.util.Try(BigDecimal(lStr)).getOrElse(BigDecimal(100000)).max(BigDecimal(1))
                                  _    <- state.grantCustomerPaymentTerms(c.id, PaymentTerms.NetInvoice(days, Price(Money(lim), Currency.CZK)))
                                yield ()
                              }(state.i18n.t("adm-cust-save-terms")),
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(state.grantCustomerPaymentTerms(c.id, PaymentTerms.PrepaidOnly))(state.i18n.t("adm-cust-terms-prepaid")),
                          ),
                        ),
                      )
                    else UI.empty,
                  )

                case DetailTab.PricingOrders =>
                  div.style(stackMd)(
                    // Customer Pricing & Agreements
                    div.style(stackSm)(
                      div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                        h3(state.i18n.t("adm-cust-agreements-title")).style(sectionTitle.margin(0.px)),
                        button
                          .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                          .onClick(state.openCustomerPricing(c.id))(span(state.i18n.t("adm-cust-edit-in-pricing")), Icons(Icon.ArrowRight, 14)),
                      ),
                      if matchingAgreements.isEmpty then
                        span(
                          if isCs then "Používá standardní veřejný ceník (žádná aktivní smlouva)."
                          else "Uses standard public pricelist (no active custom agreement).",
                        ).style(smallText)
                      else
                        div.style(stackXs)(
                          matchingAgreements.map { a =>
                            val scopeBadge = a.target match
                              case AgreementTarget.SpecificCustomer(_) =>
                                badge(if isCs then "Individuální smlouva" else "Customer-Specific", BadgeTone.Brand)
                              case AgreementTarget.TierDefault(t) =>
                                tierBadge(t, compact = true)
                            val overrideCount = a.categoryDiscounts.size + a.materialDiscounts.size + a.finishDiscounts.size
                            div.style(Style.column.gap(4.px).padding(10.px, 12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                              div.style(rowWrap)(
                                scopeBadge,
                                badge(a.currency.toString, BadgeTone.Neutral),
                                a.globalDiscount.map(d => badge(s"${d.value}% ${if isCs then "globální sleva" else "global off"}", BadgeTone.Emerald)).getOrElse(UI.empty),
                                a.minimumOrderOverride.map(m => badge(s"Min ${formatMoney(m, a.currency, lang)}", BadgeTone.Amber)).getOrElse(UI.empty),
                                if overrideCount > 0 then badge(s"$overrideCount ${if isCs then "položkových slev" else "scoped overrides"}", BadgeTone.Neutral) else UI.empty,
                              ),
                            )
                          }*
                        ),
                    ),

                    // Order History
                    div.style(Style.column.gap(10.px).padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                      div.style(rowBetween)(
                        h3(state.i18n.t("adm-cust-orders-title")).style(sectionTitle.margin(0.px)),
                        badge(customerOrders.size.toString, BadgeTone.Neutral),
                      ),
                      if customerOrders.isEmpty then
                        span(if isCs then "Zákazník zatím nemá žádné objednávky." else "No orders placed by this customer yet.").style(smallText)
                      else
                        div.style(stackXs)(
                          customerOrders.map { o =>
                            div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(10.px, 12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                              div.style(Style.column.gap(4.px))(
                                span(o.orderNumber.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
                                div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                  badge(orderStatusLabel(o.status, lang), orderStatusTone(o.status)),
                                  paymentStatusBadge(o.payment.status, lang),
                                ),
                              ),
                              div.style(rowSm)(
                                span(formatMoney(o.totals.grandTotalGross, o.totals.currency, lang)).style(Style.fontWeight(FontWeight.w700)),
                                button
                                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                  .onClick(state.navigate(AdminRoute.OrderDetail(o.id)))(span(state.i18n.t("adm-orders-open")), Icons(Icon.ChevronRight, 15)),
                              ),
                            )
                          }*
                        ),
                    ),
                  )

                case DetailTab.Artworks =>
                  div.style(stackSm)(
                    div.style(rowBetween)(
                      h3(state.i18n.t("adm-cust-artworks-title")).style(sectionTitle.margin(0.px)),
                      badge(customerArtworks.size.toString, BadgeTone.Neutral),
                    ),
                    if customerArtworks.isEmpty then
                      p(state.i18n.t("adm-cust-artworks-empty")).style(mutedText.margin(0.px))
                    else
                      div.style(stackSm)(
                        customerArtworks.map { entry =>
                          val art      = entry.artwork
                          val rev      = art.latestRevision
                          val ctxTitle = Some(s"${entry.orderNumber.value} · ${entry.productName(lang)}")

                          div.style(
                            Style.column
                              .gap(8.px)
                              .padding(12.px)
                              .rounded(12.px)
                              .bg(surfaceAlt)
                              .border(1.px, Palette.border),
                          )(
                            div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                              div.style(Style.row.align(Alignment.center).gap(12.px).minWidth(0.px).flexGrow(1.0))(
                                ArtworkComponents.renderThumbnail(
                                  rev,
                                  sizePx = 52,
                                  onClick = Some(() => state.artworkPreview.open(art, contextTitle = ctxTitle)),
                                ),
                                div.style(stackXs.minWidth(0.px))(
                                  div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                    span(rev.file.fileName.value).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                                    artworkStatusBadge(art.status, lang),
                                    badge(s"v${art.revisionCount}", BadgeTone.Neutral),
                                  ),
                                  div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                    button
                                      .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                                      .style(Style.padding(1.px, 6.px).color(brand).fontWeight(FontWeight.w700))
                                      .onClick(state.navigate(AdminRoute.OrderDetail(entry.orderId)))(
                                        entry.orderNumber.value,
                                      ),
                                    span(s"· #${entry.lineIndex + 1} ${entry.productName(lang)} (${art.componentKey.value})")
                                      .style(smallText.fontWeight(FontWeight.w600).color(inkSecondary)),
                                  ),
                                  div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                    span(s"${ArtworkComponents.formatFileSize(rev.file.sizeBytes)} · ${rev.extractedSpecs.resolutionDpi} DPI · ${rev.extractedSpecs.colorSpace}")
                                      .style(smallText),
                                    span(s"· ${if isCs then "Vytvořeno" else "Created"}: ${formatInstant(art.createdAt)}")
                                      .style(smallText),
                                  ),
                                ),
                              ),
                              div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                button
                                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                  .onClick(state.artworkPreview.open(art, contextTitle = ctxTitle))(
                                    state.i18n.t("adm-od-artwork-preview"),
                                  ),
                                ArtworkComponents.downloadButton(rev, lang),
                                ArtworkComponents.copyLinkButton(
                                  rev,
                                  lang,
                                  onCopied = () => state.notifyLinkCopied(rev.file.shareableUrl),
                                ),
                              ),
                            ),
                            div.style(Style.row.align(Alignment.center).gap(6.px).flexWrap(FlexWrap.wrap).padding(6.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                              span(if isCs then "URL:" else "URL:").style(smallText.fontWeight(FontWeight.w700)),
                              code(rev.file.shareableUrl).style(Style.fontSize(11.px).color(inkMuted).fontFamily(FontFamily.Monospace)),
                            ),
                          )
                        }*
                      ),
                  )

                case DetailTab.NotesAudit =>
                  div.style(stackMd)(
                    // Internal Account Notes
                    div.style(stackSm)(
                      h3(state.i18n.t("adm-cust-notes-title")).style(sectionTitle.margin(0.px)),
                      div.style(stackXs)(
                        if c.notes.isEmpty then span(if isCs then "Žádné poznámky." else "No account notes yet.").style(smallText)
                        else
                          UI.fragment(
                            c.notes.map { n =>
                              div.style(Style.column.gap(2.px).padding(8.px, 10.px).rounded(8.px).bg(surfaceAlt).border(1.px, Palette.border))(
                                span(n.text.value).style(Style.fontSize(13.px).color(ink)),
                                span(formatInstant(n.createdAt)).style(smallText),
                              )
                            }*
                          )
                      ),
                      div.style(rowSm)(
                        input
                          .cssClass("mp-input")
                          .placeholder(if isCs then "Přidat poznámku k účtu..." else "Add internal account note...")
                          .value(noteRef)
                          .onInput(v => noteRef.set(v)),
                        button
                          .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                          .style(Style.flexShrink(0.0))
                          .onClick(noteRef.get.flatMap(t => state.addCustomerNote(c.id, t).andThen(noteRef.set(""))))(state.i18n.t("adm-od-notes-add")),
                      ),
                    ),

                    // Audit Trail
                    div.style(Style.column.gap(10.px).padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                      div.style(rowBetween)(
                        h3(state.i18n.t("adm-cust-audit-title")).style(sectionTitle.margin(0.px)),
                        badge(c.auditLog.size.toString, BadgeTone.Neutral),
                      ),
                      if c.auditLog.isEmpty then
                        span(if isCs then "Žádné auditní záznamy." else "No audit entries recorded.").style(smallText)
                      else
                        div.cssClass("mp-timeline")(
                          c.auditLog.reverse.map { entry =>
                            div.style(stackXs)(
                              div.style(rowSm.flexWrap(FlexWrap.wrap))(
                                badge(entry.action, BadgeTone.Neutral),
                                span(entry.summary).style(Style.fontSize(12.px).fontWeight(FontWeight.w600).color(ink)),
                              ),
                              span(s"${actorLabel(entry.actor, lang)} · ${formatInstant(entry.occurredAt)}").style(smallText),
                            )
                          }*
                        ),
                    ),
                  ),
            )

          DataTable.render[CustomerView, CustomerId](
            title = Some(span(state.i18n.t("adm-cust-title"))),
            subtitle = Some(span(state.i18n.t("adm-cust-subtitle"))),
            rows = allCustomers,
            rowKey = _.id,
            columns = columns,
            state = tableState,
            searchPlaceholder = if isCs then "Hledat jméno, e-mail, firmu, IČO nebo kontaktní osobu..." else "Search name, email, company, IČO, or contact person...",
            matchesSearch = (c, q) =>
              c.contact.firstName.value.toLowerCase.contains(q) ||
                c.contact.lastName.value.toLowerCase.contains(q) ||
                c.contact.email.value.toLowerCase.contains(q) ||
                c.id.value.toLowerCase.contains(q) ||
                c.company.exists(_.legalName.value.toLowerCase.contains(q)) ||
                c.company.exists(_.registrationNumber.value.toLowerCase.contains(q)) ||
                c.additionalContacts.exists(ac =>
                  ac.firstName.value.toLowerCase.contains(q) ||
                    ac.lastName.value.toLowerCase.contains(q) ||
                    ac.email.value.toLowerCase.contains(q)
                ),
            filterGroups = filterGroups,
            onRowSelect = Some(c => actionMenu.close.andThen(editDraftRef.set(None)).andThen(state.selectCustomer(c.id))),
            renderDetail = Some(renderCustomerDetail),
            detailTitle = Some(c => s"${if isCs then "Zákazník" else "Customer"} · ${c.id.value}"),
            createConfig =
              if canManage then
                Some(
                  DataTable.CreateConfig(
                    buttonLabel = span(state.i18n.t("adm-cust-register-new")),
                    title = span(state.i18n.t("adm-cust-register-new")),
                    renderForm = closeCreate =>
                      renderRegisterPanel(state, regIdRef, regSegRef, regCompRef, regIcoRef, regFirstRef, regLastRef, regEmailRef, regCityRef, closeCreate),
                  )
                )
              else None,
            emptyMessage = if isCs then "Aktuálním filtrům neodpovídají žádní zákazníci." else "No customers match the current filters.",
            lang = lang,
          )
        },
        dialog.render,
      )

  private def renderProfileEditor(
      state: BackofficeState,
      c: CustomerView,
      draft: EditDraft,
      draftRef: SignalRef[Option[EditDraft]],
      isCs: Boolean,
  ): UI =
    val supportsMultiContacts = c.segment == CustomerSegment.Business || c.segment == CustomerSegment.Agency

    def update(f: EditDraft => EditDraft): Unit < Async =
      draftRef.set(Some(f(draft)))

    div.cssClass("mp-card").style(Style.border(1.px, brandBorder))(
      h3(state.i18n.t("adm-cust-edit-profile")).style(sectionTitle.margin(0.px)),
      if supportsMultiContacts then
        div.cssClass("mp-field-row")(
          editField(if isCs then "Název firmy" else "Company Name", draft.companyName, v => update(_.copy(companyName = v))),
          editField("IČO / Reg. No", draft.regNo, v => update(_.copy(regNo = v))),
          editField("DIČ / VAT ID", draft.vatId, v => update(_.copy(vatId = v))),
        )
      else UI.empty,

      // Main Contact Person
      span(if isCs then "Hlavní kontaktní osoba" else "Primary Contact Person").style(labelStyle),
      div.cssClass("mp-field-row")(
        editField(if isCs then "Jméno" else "First Name", draft.mainFirst, v => update(_.copy(mainFirst = v))),
        editField(if isCs then "Příjmení" else "Last Name", draft.mainLast, v => update(_.copy(mainLast = v))),
        editField("Email", draft.mainEmail, v => update(_.copy(mainEmail = v))),
        editField(if isCs then "Telefon" else "Phone", draft.mainPhone, v => update(_.copy(mainPhone = v))),
        editField(if isCs then "Pozice / Role" else "Role / Title", draft.mainRole, v => update(_.copy(mainRole = v))),
      ),

      // Additional Contact Persons (Business / Agency only)
      if supportsMultiContacts then
        div.style(stackSm)(
          div.style(rowBetween)(
            span(if isCs then "Další kontaktní osoby" else "Additional Contact Persons").style(labelStyle),
            button
              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
              .onClick(
                update(d => d.copy(additionalContacts = d.additionalContacts :+ ContactDraft("", "", "", "", "")))
              )(state.i18n.t("adm-cust-add-contact")),
          ),
          UI.fragment(
            draft.additionalContacts.zipWithIndex.map { case (cp, idx) =>
              def updateCp(f: ContactDraft => ContactDraft): Unit < Async =
                update(d => d.copy(additionalContacts = d.additionalContacts.updated(idx, f(cp))))
              div.style(Style.column.gap(8.px).padding(10.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                div.style(rowBetween)(
                  span(s"#${idx + 1}").style(smallText.fontWeight(FontWeight.w700)),
                  button
                    .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                    .onClick(
                      update(d => d.copy(additionalContacts = d.additionalContacts.patch(idx, Nil, 1)))
                    )(state.i18n.t("adm-cat-delete")),
                ),
                div.cssClass("mp-field-row")(
                  editField(if isCs then "Jméno" else "First Name", cp.firstName, v => updateCp(_.copy(firstName = v))),
                  editField(if isCs then "Příjmení" else "Last Name", cp.lastName, v => updateCp(_.copy(lastName = v))),
                  editField("Email", cp.email, v => updateCp(_.copy(email = v))),
                  editField(if isCs then "Telefon" else "Phone", cp.phone, v => updateCp(_.copy(phone = v))),
                  editField(if isCs then "Role" else "Role", cp.roleTitle, v => updateCp(_.copy(roleTitle = v))),
                ),
              )
            }*
          ),
        )
      else UI.empty,

      // Billing Address
      span(if isCs then "Fakturační adresa" else "Billing Address").style(labelStyle),
      div.cssClass("mp-field-row")(
        editField(if isCs then "Ulice" else "Street", draft.street, v => update(_.copy(street = v))),
        editField(if isCs then "Město" else "City", draft.city, v => update(_.copy(city = v))),
        editField(if isCs then "PSČ" else "Postal Code", draft.postalCode, v => update(_.copy(postalCode = v))),
        editField(if isCs then "Země" else "Country", draft.countryCode, v => update(_.copy(countryCode = v))),
      ),

      div.style(rowSm)(
        button
          .cssClass("mp-btn mp-btn-primary mp-btn-sm")
          .onClick {
            val validatedMain =
              (
                NonEmptyString(draft.mainFirst.trim).toEither,
                NonEmptyString(draft.mainLast.trim).toEither,
                EmailAddress(draft.mainEmail.trim).toEither,
              )
            validatedMain match
              case (Left(e), _, _) => state.errorBanner.set(Some(e.mkString(", ")))
              case (_, Left(e), _) => state.errorBanner.set(Some(e.mkString(", ")))
              case (_, _, Left(e)) => state.errorBanner.set(Some(e.mkString(", ")))
              case (Right(fn), Right(ln), Right(em)) =>
                val mainContact = ContactPerson(
                  firstName = fn,
                  lastName = ln,
                  email = em,
                  phone = Option.when(draft.mainPhone.trim.nonEmpty)(NonEmptyString.unsafe(draft.mainPhone.trim)),
                  roleTitle = Option.when(draft.mainRole.trim.nonEmpty)(NonEmptyString.unsafe(draft.mainRole.trim)),
                )
                val parsedExtras: Either[String, List[ContactPerson]] =
                  draft.additionalContacts.foldLeft[Either[String, List[ContactPerson]]](Right(Nil)) { (acc, cp) =>
                    acc.flatMap { list =>
                      (NonEmptyString(cp.firstName.trim).toEither, NonEmptyString(cp.lastName.trim).toEither, EmailAddress(cp.email.trim).toEither) match
                        case (Right(cfn), Right(cln), Right(cem)) =>
                          Right(
                            list :+ ContactPerson(
                              firstName = cfn,
                              lastName = cln,
                              email = cem,
                              phone = Option.when(cp.phone.trim.nonEmpty)(NonEmptyString.unsafe(cp.phone.trim)),
                              roleTitle = Option.when(cp.roleTitle.trim.nonEmpty)(NonEmptyString.unsafe(cp.roleTitle.trim)),
                            )
                          )
                        case _ =>
                          Left(if isCs then "Každý další kontakt vyžaduje platné jméno, příjmení a e-mail." else "Every additional contact requires valid first name, last name, and email.")
                    }
                  }
                parsedExtras match
                  case Left(err) => state.errorBanner.set(Some(err))
                  case Right(extras) =>
                    val companyOpt =
                      if supportsMultiContacts then
                        Some(
                          CompanyProfile(
                            legalName = NonEmptyString.unsafe(if draft.companyName.trim.nonEmpty then draft.companyName.trim else s"${fn.value} ${ln.value}"),
                            registrationNumber = NonEmptyString.unsafe(if draft.regNo.trim.nonEmpty then draft.regNo.trim else "00000000"),
                            vatId = Option.when(draft.vatId.trim.nonEmpty)(NonEmptyString.unsafe(draft.vatId.trim)),
                            contactPerson = NonEmptyString.unsafe(s"${fn.value} ${ln.value}"),
                          )
                        )
                      else None
                    val billing = Address(
                      street = NonEmptyString.unsafe(if draft.street.trim.nonEmpty then draft.street.trim else c.billingAddress.street.value),
                      city = NonEmptyString.unsafe(if draft.city.trim.nonEmpty then draft.city.trim else c.billingAddress.city.value),
                      postalCode = NonEmptyString.unsafe(if draft.postalCode.trim.nonEmpty then draft.postalCode.trim else c.billingAddress.postalCode.value),
                      countryCode = NonEmptyString.unsafe(if draft.countryCode.trim.nonEmpty then draft.countryCode.trim else c.billingAddress.countryCode.value),
                    )
                    val cmd = UpdateCustomerProfileCmd(
                      id = c.id,
                      company = companyOpt,
                      contact = mainContact,
                      billingAddress = billing,
                      tags = c.tags,
                      additionalContacts = if supportsMultiContacts then Some(extras) else None,
                    )
                    state.updateCustomerProfile(cmd).andThen(draftRef.set(None))
          }(state.i18n.t("adm-cat-save")),
        button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(draftRef.set(None))(state.i18n.t("adm-cat-cancel")),
      ),
    )

  private def editField(lbl: String, current: String, onChange: String => Unit < Async): UI =
    div.cssClass("mp-field")(
      label(lbl).style(smallText.fontWeight(FontWeight.w600)),
      input.cssClass("mp-input").value(current).onInput(onChange),
    )

  private def renderRegisterPanel(
      state: BackofficeState,
      regIdRef: SignalRef[String],
      regSegRef: SignalRef[CustomerSegment],
      regCompRef: SignalRef[String],
      regIcoRef: SignalRef[String],
      regFirstRef: SignalRef[String],
      regLastRef: SignalRef[String],
      regEmailRef: SignalRef[String],
      regCityRef: SignalRef[String],
      onClose: () => Unit < Async,
  ): UI =
    div.style(stackMd)(
      div.cssClass("mp-field-row")(
        div.cssClass("mp-field")(
          label("Segment").style(labelStyle),
          regSegRef.render { seg =>
            select
              .cssClass("mp-input")
              .value(seg.toString)
              .onChange(v => CustomerSegment.values.find(_.toString == v).map(regSegRef.set).getOrElse(Kyo.unit))(
                CustomerSegment.values.toList.map(s => option.value(s.toString).selected(s == seg)(s.toString))*
              )
          },
        ),
        div.cssClass("mp-field")(
          label("Customer ID (optional)").style(labelStyle),
          input.cssClass("mp-input").placeholder("cust-new-studio").value(regIdRef).onInput(v => regIdRef.set(v)),
        ),
        div.cssClass("mp-field")(
          label("Company Name (B2B)").style(labelStyle),
          input.cssClass("mp-input").placeholder("Atelier Nova s.r.o.").value(regCompRef).onInput(v => regCompRef.set(v)),
        ),
        div.cssClass("mp-field")(
          label("Reg No / IČO").style(labelStyle),
          input.cssClass("mp-input").placeholder("28991122").value(regIcoRef).onInput(v => regIcoRef.set(v)),
        ),
      ),
      div.cssClass("mp-field-row")(
        div.cssClass("mp-field")(
          label("First Name").style(labelStyle),
          input.cssClass("mp-input").placeholder("Tereza").value(regFirstRef).onInput(v => regFirstRef.set(v)),
        ),
        div.cssClass("mp-field")(
          label("Last Name").style(labelStyle),
          input.cssClass("mp-input").placeholder("Králová").value(regLastRef).onInput(v => regLastRef.set(v)),
        ),
        div.cssClass("mp-field")(
          label("Email").style(labelStyle),
          emailInput.cssClass("mp-input").placeholder("tereza@ateliernova.cz").value(regEmailRef).onInput(v => regEmailRef.set(v)),
        ),
        div.cssClass("mp-field")(
          label("City").style(labelStyle),
          input.cssClass("mp-input").value(regCityRef).onInput(v => regCityRef.set(v)),
        ),
      ),
      div.style(rowSm)(
        button
          .cssClass("mp-btn mp-btn-primary mp-btn-sm")
          .onClick {
            for
              seg   <- regSegRef.get
              idRaw <- regIdRef.get.map(_.trim)
              comp  <- regCompRef.get.map(_.trim)
              ico   <- regIcoRef.get.map(_.trim)
              fn    <- regFirstRef.get.map(_.trim)
              ln    <- regLastRef.get.map(_.trim)
              em    <- regEmailRef.get.map(_.trim)
              city  <- regCityRef.get.map(_.trim)
              _ <- (EmailAddress(em).toEither, NonEmptyString(fn).toEither, NonEmptyString(ln).toEither) match
                     case (Left(e), _, _) => state.errorBanner.set(Some(e.mkString(", ")))
                     case (_, Left(e), _) => state.errorBanner.set(Some(e.mkString(", ")))
                     case (_, _, Left(e)) => state.errorBanner.set(Some(e.mkString(", ")))
                     case (Right(email), Right(first), Right(last)) =>
                       val cid = CustomerId.unsafe(if idRaw.nonEmpty then idRaw else s"cust-${em.takeWhile(_ != '@').toLowerCase}")
                       val compOpt =
                         if comp.nonEmpty then
                           Some(
                             CompanyProfile(
                               legalName = NonEmptyString.unsafe(comp),
                               registrationNumber = NonEmptyString.unsafe(if ico.nonEmpty then ico else "00000000"),
                               vatId = None,
                               contactPerson = NonEmptyString.unsafe(s"${first.value} ${last.value}"),
                             )
                           )
                         else None
                       val cmd = RegisterCustomerCmd(
                         id = cid,
                         segment = seg,
                         company = compOpt,
                         contact = ContactPerson(first, last, email),
                         billingAddress = Address(
                           street = NonEmptyString.unsafe("Main Street 1"),
                           city = NonEmptyString.unsafe(if city.nonEmpty then city else "Prague"),
                           postalCode = NonEmptyString.unsafe("110 00"),
                           countryCode = NonEmptyString.unsafe("CZ"),
                         ),
                       )
                       state.registerCustomer(cmd).andThen(onClose()).andThen(regEmailRef.set(""))
            yield ()
          }(state.i18n.t("adm-cat-save")),
        button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(onClose())(state.i18n.t("adm-cat-cancel")),
      ),
    )
