package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.views.customers.*
import mpbuilder.admin.views.customers.CustomerDrafts.{ProfileDraft, Section, TermsDraft}
import mpbuilder.admin.views.customers.CustomerUi.*
import mpbuilder.backoffice.CustomerArtworkEntry
import mpbuilder.customers.*
import mpbuilder.shared.*
import mpbuilder.ui.components.{ActionMenu, ArtworkComponents, ConfirmDialog, Controls, DataTable, Icons, TabBar}
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Customer Accounts & B2B Approvals Manager.
  *
  *   - Master-detail DataTable; the create pane hosts a segment-aware registration form ([[CustomerCreateForm]]).
  *   - Detail header: identity + badges, status-aware primary buttons (Approve / Reactivate / Add note) and an
  *     overflow menu for rarer or destructive actions; a status banner; KPI tiles (orders, lifetime value, unpaid,
  *     payment terms) that jump to the relevant tab.
  *   - Tabs: Profile (per-section cards with dedicated Edit buttons), Pricing & Terms (tier, payment terms, price
  *     agreements), Orders, Artworks, Notes & Activity.
  */
object CustomerManagerView:

  private enum DetailTab:
    case Profile, Commercial, Orders, Artworks, Activity

  def render(state: BackofficeState): UI < Sync =
    for
      // Open the already-selected customer (e.g. arriving from the dashboard approval queue).
      initialSel     <- state.selectedCustomer.get
      tableState     <- Signal.initRef(
                          DataTable.State[CustomerId](
                            sort = Some(DataTable.SortSpec("name", ascending = true)),
                            pageSize = 10,
                            selectedKey = initialSel.map(_.customer.id),
                          )
                        )
      dialog         <- ConfirmDialog.Handle.make
      actionMenu     <- ActionMenu.Handle.make
      detailTabRef   <- Signal.initRef[DetailTab](DetailTab.Profile)
      editingRef     <- Signal.initRef[Option[(CustomerId, Section)]](None)
      profileDraft   <- Signal.initRef[Option[ProfileDraft]](None)
      pendingTierRef <- Signal.initRef[Option[(CustomerId, CustomerTier)]](None)
      termsRef       <- Signal.initRef[Option[TermsDraft]](None)
      createRefs     <- CustomerCreateForm.Refs.make
      noteRef        <- Signal.initRef("")
    yield
      val profileRefs    = CustomerProfileTab.Refs(editingRef, profileDraft)
      val commercialRefs = CustomerCommercialTab.Refs(pendingTierRef, termsRef)

      val resetDetailState: Unit < Async =
        for
          _ <- actionMenu.close
          _ <- editingRef.set(None)
          _ <- profileDraft.set(None)
          _ <- pendingTierRef.set(None)
          _ <- termsRef.set(None)
          _ <- noteRef.set("")
        yield ()

      val combined =
        state.customers
          .combineLatest(state.agreements)
          .combineLatest(state.orders)
          .combineLatest(state.selectedCustomer)
          .combineLatest(editingRef)
          .combineLatest(detailTabRef)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      UI.fragment(
        combined.render { case (((((((allCustomers, allAgreements), allOrders), selectedDetailOpt), editingOpt), activeTab), authOpt), lang) =>
          val isCs      = lang == Language.Cs
          def t(k: String): UI = span(state.i18n.t(k))
          val canManage = state.canManage(authOpt)

          val columns: List[DataTable.Column[CustomerView]] = List(
            DataTable.Column[CustomerView](
              id = "name",
              header = if isCs then "Zákazník / Firma" else "Customer / Company",
              sortKey = Some(displayName),
              cell = c =>
                div.style(Style.column.gap(2.px))(
                  span(displayName(c)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                  span(s"${personName(c.contact)} · ${c.contact.email.value}").style(smallText),
                ),
            ),
            DataTable.Column[CustomerView](
              id = "segment",
              header = "Segment",
              sortKey = Some(_.segment.toString),
              cell = c => badge(segmentLabel(c.segment, lang), BadgeTone.Neutral),
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
              cell = c => badge(statusLabel(c.status, lang), statusTone(c.status)),
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
              options = CustomerStatus.values.toList.map(st => DataTable.FilterOption[CustomerView](st.toString, statusLabel(st, lang), _.status == st)),
            ),
            DataTable.FilterGroup[CustomerView](
              id = "tier",
              label = if isCs then "Cenová hladina" else "Tier",
              options = CustomerTier.values.toList.map(tr => DataTable.FilterOption[CustomerView](tr.toString, tr.toString, _.tier == tr)),
            ),
            DataTable.FilterGroup[CustomerView](
              id = "segment",
              label = "Segment",
              options = CustomerSegment.values.toList.map(s => DataTable.FilterOption[CustomerView](s.toString, segmentLabel(s, lang), _.segment == s)),
            ),
          )

          def renderCustomerDetail(c: CustomerView): UI =
            val customerOrders   = allOrders.filter(_.customer.customerId.contains(c.id)).sortBy(_.placedAt).reverse
            val stats            = orderStats(customerOrders)
            val title            = displayName(c)
            val editing          = editingOpt.collect { case (id, sec) if id == c.id => sec }
            val customerArtworks = selectedDetailOpt.filter(_.customer.id == c.id).map(_.artworks).getOrElse(List.empty[CustomerArtworkEntry])

            def goTab(tab: DetailTab): Unit < Async = actionMenu.close.andThen(detailTabRef.set(tab))

            def openEdit(sec: Section): Unit < Async =
              goTab(DetailTab.Profile)
                .andThen(profileDraft.set(Some(ProfileDraft.from(c, sec))))
                .andThen(editingRef.set(Some(c.id -> sec)))

            val approveDialog: Unit < Async =
              dialog.open(
                ConfirmDialog.Config(
                  title = if c.status == CustomerStatus.Suspended then (if isCs then "Obnovit účet" else "Reactivate account")
                  else (if isCs then "Schválit zákaznický účet" else "Approve customer account"),
                  message =
                    if isCs then s"Aktivovat účet „$title“? Zákazník bude moci objednávat a lze mu nastavit fakturu se splatností."
                    else s"Activate “$title”? The customer can order again and becomes eligible for invoice terms.",
                  confirmLabel = if c.status == CustomerStatus.Suspended then (if isCs then "Obnovit" else "Reactivate") else (if isCs then "Schválit" else "Approve"),
                  cancelLabel = if isCs then "Zrušit" else "Cancel",
                  tone = ConfirmDialog.DialogTone.Primary,
                  onConfirm = _ => state.approveCustomer(c.id).unit,
                )
              )

            val suspendDialog: Unit < Async =
              dialog.open(
                ConfirmDialog.Config(
                  title = if isCs then "Pozastavit zákaznický účet" else "Suspend customer account",
                  message =
                    if isCs then s"Pozastavit účet „$title“? Účet lze později obnovit. Akce se zapíše do auditu."
                    else s"Suspend “$title”? You can reactivate it later. This is recorded in the audit log.",
                  confirmLabel = if isCs then "Pozastavit účet" else "Suspend account",
                  cancelLabel = if isCs then "Zrušit" else "Cancel",
                  tone = ConfirmDialog.DialogTone.Amber,
                  reasonLabel = Some(if isCs then "Důvod pozastavení" else "Suspension reason"),
                  reasonPlaceholder = Some(if isCs then "Např. neuhrazené faktury po splatnosti..." else "e.g. Overdue invoices..."),
                  reasonRequired = true,
                  reasonErrorText = if isCs then "Zadejte důvod pozastavení." else "Please enter a suspension reason.",
                  onConfirm = reasonOpt => state.suspendCustomer(c.id, reasonOpt.getOrElse("")).unit,
                )
              )

            val deactivateDialog: Unit < Async =
              dialog.open(
                ConfirmDialog.Config(
                  title = if isCs then "Deaktivovat zákaznický účet" else "Deactivate customer account",
                  message =
                    if isCs then s"Trvale deaktivovat účet „$title“? Akce je nevratná, zruší fakturu se splatností a zapíše se do auditu."
                    else s"Permanently deactivate “$title”? This cannot be undone, revokes invoice terms and is recorded in the audit log.",
                  confirmLabel = if isCs then "Deaktivovat" else "Deactivate",
                  cancelLabel = if isCs then "Zrušit" else "Cancel",
                  tone = ConfirmDialog.DialogTone.Danger,
                  reasonLabel = Some(if isCs then "Důvod deaktivace" else "Deactivation reason"),
                  reasonPlaceholder = Some(if isCs then "Např. ukončení spolupráce..." else "e.g. Account closed at customer request..."),
                  reasonRequired = true,
                  reasonErrorText = if isCs then "Zadejte důvod deaktivace." else "Please enter a deactivation reason.",
                  onConfirm = reasonOpt => state.deactivateCustomer(c.id, reasonOpt.getOrElse("")).unit,
                )
              )

            val isOpen = c.status != CustomerStatus.Deactivated

            // Primary, always-visible header buttons
            val headerButtons: UI =
              if !canManage then UI.empty
              else
                UI.fragment(
                  c.status match
                    case CustomerStatus.PendingApproval =>
                      button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(approveDialog)(Icons(Icon.Check, 14), t("adm-cust-approve"))
                    case CustomerStatus.Suspended =>
                      button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(approveDialog)(Icons(Icon.Play, 14), t("adm-cust-reactivate"))
                    case _ => UI.empty,
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(goTab(DetailTab.Activity))(Icons(Icon.Message, 14), t("adm-cust-add-note")),
                )

            // Overflow menu: editing shortcuts, pricing deep-link and lifecycle actions
            val menuItems: List[ActionMenu.Item] =
              if !canManage then Nil
              else
                val editItems =
                  if !isOpen then Nil
                  else
                    (if isB2B(c.segment) then List(ActionMenu.Item(t("adm-cust-menu-edit-company"), () => openEdit(Section.Company))) else Nil) ++ List(
                      ActionMenu.Item(t("adm-cust-menu-edit-contacts"), () => openEdit(Section.Contacts)),
                      ActionMenu.Item(t("adm-cust-menu-edit-addresses"), () => openEdit(Section.Addresses)),
                      ActionMenu.Item(t("adm-cust-menu-terms"), () => goTab(DetailTab.Commercial)),
                    )
                val pricingItem = List(ActionMenu.Item(t("adm-cust-manage-pricing"), () => state.openCustomerPricing(c.id), dividerBefore = editItems.nonEmpty))
                val suspendItem =
                  if c.status == CustomerStatus.Active then
                    List(ActionMenu.Item(t("adm-cust-suspend"), () => suspendDialog, ActionMenu.ItemTone.Amber, dividerBefore = true))
                  else Nil
                val deactivateItem =
                  if isOpen then
                    List(ActionMenu.Item(t("adm-cust-deactivate"), () => deactivateDialog, ActionMenu.ItemTone.Danger, dividerBefore = suspendItem.isEmpty))
                  else Nil
                editItems ++ pricingItem ++ suspendItem ++ deactivateItem

            val statusBanner: UI = c.status match
              case CustomerStatus.PendingApproval =>
                div.cssClass("mp-cust-banner mp-cust-banner-amber")(
                  Icons(Icon.Clock, 16),
                  div.style(stackXs.flexGrow(1.0).minWidth(0.px))(
                    span(if isCs then "Čeká na schválení" else "Awaiting approval").style(Style.fontWeight(FontWeight.w800)),
                    span(
                      if isCs then "Ověřte firemní údaje (IČO, DIČ). Fakturu se splatností lze povolit až po schválení."
                      else "Verify company details (Reg. No., VAT ID). Invoice terms can only be granted after approval.",
                    ).style(Style.fontSize(12.px)),
                  ),
                )
              case CustomerStatus.Suspended =>
                div.cssClass("mp-cust-banner mp-cust-banner-rose")(
                  Icons(Icon.Pause, 16),
                  div.style(stackXs.flexGrow(1.0).minWidth(0.px))(
                    span(if isCs then "Účet je pozastaven" else "Account suspended").style(Style.fontWeight(FontWeight.w800)),
                    latestAudit(c, "Suspended")
                      .map(e => span(s"${e.summary} · ${formatDate(e.occurredAt)}").style(Style.fontSize(12.px)))
                      .getOrElse(UI.empty),
                  ),
                )
              case CustomerStatus.Deactivated =>
                div.cssClass("mp-cust-banner mp-cust-banner-neutral")(
                  Icons(Icon.Lock, 16),
                  div.style(stackXs.flexGrow(1.0).minWidth(0.px))(
                    span(if isCs then "Účet je deaktivován — pouze pro čtení" else "Account deactivated — read only").style(Style.fontWeight(FontWeight.w800)),
                    latestAudit(c, "Deactivated")
                      .map(e => span(s"${e.summary} · ${formatDate(e.occurredAt)}").style(Style.fontSize(12.px)))
                      .getOrElse(UI.empty),
                  ),
                )
              case CustomerStatus.Active => UI.empty

            def statTile(lbl: UI, value: String, sub: String, tab: DetailTab): UI =
              button.cssClass("mp-cust-stat").onClick(goTab(tab))(
                span(lbl).cssClass("mp-cust-stat-label"),
                span(value).cssClass("mp-cust-stat-value mp-num"),
                span(sub).cssClass("mp-cust-stat-sub"),
              )

            val statsStrip: UI =
              div.cssClass("mp-cust-stats")(
                statTile(
                  t("adm-cust-kpi-orders"),
                  stats.count.toString,
                  if isCs then s"${stats.openCount} rozpracovaných" else s"${stats.openCount} in progress",
                  DetailTab.Orders,
                ),
                statTile(
                  t("adm-cust-kpi-ltv"),
                  formatAmounts(stats.lifetime, lang),
                  stats.lastOrder.map(o => s"${if isCs then "Poslední" else "Last"}: ${formatDate(o.placedAt)}").getOrElse(if isCs then "Zatím bez objednávek" else "No orders yet"),
                  DetailTab.Orders,
                ),
                statTile(
                  t("adm-cust-kpi-unpaid"),
                  formatAmounts(stats.unpaid, lang),
                  if isCs then "Neuhrazené aktivní objednávky" else "Unpaid active orders",
                  DetailTab.Orders,
                ),
                statTile(
                  t("adm-cust-terms-label"),
                  c.paymentTerms match
                    case PaymentTerms.PrepaidOnly        => if isCs then "Předem" else "Prepaid"
                    case PaymentTerms.NetInvoice(d, _)   => s"Net $d",
                  c.paymentTerms match
                    case PaymentTerms.NetInvoice(_, lim) => s"${if isCs then "Limit" else "Limit"} ${formatMoney(lim.amount, lim.currency, lang)}"
                    case PaymentTerms.PrepaidOnly        => s"${if isCs then "Hladina" else "Tier"} ${c.tier}",
                  DetailTab.Commercial,
                ),
              )

            val detailTabs = List(
              TabBar.TabItem(DetailTab.Profile, if isCs then "Profil" else "Profile", Some(1 + c.additionalContacts.size)),
              TabBar.TabItem(DetailTab.Commercial, if isCs then "Ceny a podmínky" else "Pricing & Terms", None),
              TabBar.TabItem(DetailTab.Orders, if isCs then "Objednávky" else "Orders", Some(customerOrders.size)),
              TabBar.TabItem(DetailTab.Artworks, if isCs then "Tisková data" else "Artworks", Some(customerArtworks.size)),
              TabBar.TabItem(DetailTab.Activity, if isCs then "Poznámky a aktivita" else "Notes & Activity", Some(c.notes.size)),
            )

            div.style(stackMd.width(100.pct))(
              div.cssClass("mp-detail-head")(
                div.cssClass("mp-detail-head-row")(
                  div.style(rowMd.minWidth(0.px).flexGrow(1.0))(
                    Controls.avatar(title, "lg"),
                    div.style(Style.column.gap(2.px).minWidth(0.px))(
                      h2(title).style(Style.margin(0.px).fontSize(19.px).fontWeight(FontWeight.w800).color(ink)),
                      span(s"${personName(c.contact)} · ${c.contact.email.value}").style(smallText),
                    ),
                  ),
                ),
                div.cssClass("mp-detail-tags-row")(
                  badge(statusLabel(c.status, lang), statusTone(c.status)),
                  tierBadge(c.tier),
                  badge(segmentLabel(c.segment, lang), BadgeTone.Neutral),
                  c.company.map(comp => badge(s"IČO ${comp.registrationNumber.value}", BadgeTone.Neutral)).getOrElse(UI.empty),
                  span(s"${c.id.value} · ${if isCs then "od" else "since"} ${formatDate(c.createdAt)}").style(smallText.fontFamily(FontFamily.Monospace)),
                ),
                if canManage then
                  div.cssClass("mp-cust-head-actions")(
                    headerButtons,
                    actionMenu.render(key = s"cust-${c.id.value}", buttonLabel = span(t("adm-cust-more")), items = menuItems),
                  )
                else UI.empty,
              ),
              statusBanner,
              statsStrip,
              TabBar.render[DetailTab](items = detailTabs, active = activeTab, onSelect = tab => goTab(tab), compact = true),
              activeTab match
                case DetailTab.Profile =>
                  CustomerProfileTab.render(state, c, allCustomers, editing, profileRefs, dialog, canManage, lang)
                case DetailTab.Commercial =>
                  CustomerCommercialTab.render(state, c, allAgreements, stats, commercialRefs, canManage, lang)
                case DetailTab.Orders =>
                  renderOrders(state, customerOrders, stats, lang)
                case DetailTab.Artworks =>
                  renderArtworks(state, customerArtworks, lang)
                case DetailTab.Activity =>
                  renderActivity(state, c, noteRef, canManage, lang),
            )

          DataTable.render[CustomerView, CustomerId](
            title = Some(t("adm-cust-title")),
            subtitle = Some(t("adm-cust-subtitle")),
            rows = allCustomers,
            rowKey = _.id,
            columns = columns,
            state = tableState,
            searchPlaceholder = if isCs then "Hledat jméno, e-mail, firmu, IČO nebo kontaktní osobu..." else "Search name, email, company, IČO, or contact person...",
            matchesSearch = (c, q) =>
              (c.contact :: c.additionalContacts).exists(cp =>
                cp.firstName.value.toLowerCase.contains(q) || cp.lastName.value.toLowerCase.contains(q) || cp.email.value.toLowerCase.contains(q)
              ) ||
                c.id.value.toLowerCase.contains(q) ||
                c.company.exists(co => co.legalName.value.toLowerCase.contains(q) || co.registrationNumber.value.toLowerCase.contains(q)) ||
                c.tags.exists(_.toLowerCase.contains(q)),
            filterGroups = filterGroups,
            onRowSelect = Some(c => resetDetailState.andThen(state.selectCustomer(c.id))),
            renderDetail = Some(renderCustomerDetail),
            detailTitle = Some(c => s"${if isCs then "Zákazník" else "Customer"} · ${c.id.value}"),
            createConfig =
              if canManage then
                Some(
                  DataTable.CreateConfig(
                    buttonLabel = t("adm-cust-register-new"),
                    title = t("adm-cust-register-title"),
                    renderForm = closeCreate =>
                      CustomerCreateForm.render(
                        state,
                        createRefs,
                        lang,
                        onCreated = id =>
                          for
                            _ <- resetDetailState
                            _ <- detailTabRef.set(DetailTab.Profile)
                            _ <- tableState.updateAndGet(_.copy(isCreating = false, selectedKey = Some(id)))
                          yield (),
                        onCancel = closeCreate(),
                      ),
                  )
                )
              else None,
            emptyMessage = if isCs then "Aktuálním filtrům neodpovídají žádní zákazníci." else "No customers match the current filters.",
            lang = lang,
          )
        },
        dialog.render,
      )

  // ── Orders tab ─────────────────────────────────────────────────────────────

  private def renderOrders(state: BackofficeState, orders: List[mpbuilder.ordering.OrderView], stats: OrderStats, lang: Language): UI =
    val isCs = lang == Language.Cs
    if orders.isEmpty then
      Controls.emptyState(
        Icon.Inbox,
        span(if isCs then "Zatím žádné objednávky" else "No orders yet"),
        span(if isCs then "Objednávky zákazníka se zde zobrazí po jejich odeslání." else "Orders placed by this customer will appear here."),
      )
    else
      div.style(stackSm)(
        div.style(rowWrap)(
          span(if isCs then s"${stats.count} objednávek · ${stats.openCount} rozpracovaných" else s"${stats.count} orders · ${stats.openCount} in progress")
            .style(labelStyle),
        ),
        UI.fragment(
          orders.map { o =>
            button.cssClass("mp-cust-order-row").onClick(state.navigate(AdminRoute.OrderDetail(o.id)))(
              div.style(Style.column.gap(4.px).minWidth(0.px).flexGrow(1.0).align(Alignment.start))(
                div.style(rowXs.flexWrap(FlexWrap.wrap))(
                  span(o.orderNumber.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
                  span(formatDate(o.placedAt)).style(smallText),
                ),
                div.style(rowXs.flexWrap(FlexWrap.wrap))(
                  badge(orderStatusLabel(o.status, lang), orderStatusTone(o.status)),
                  paymentStatusBadge(o.payment.status, lang),
                ),
              ),
              span(formatMoney(o.totals.grandTotalGross, o.totals.currency, lang)).cssClass("mp-num").style(Style.fontWeight(FontWeight.w700).color(ink)),
              Icons(Icon.ChevronRight, 16),
            )
          }*
        ),
      )

  // ── Artworks tab ───────────────────────────────────────────────────────────

  private def renderArtworks(state: BackofficeState, artworks: List[CustomerArtworkEntry], lang: Language): UI =
    val isCs = lang == Language.Cs
    if artworks.isEmpty then p(state.i18n.t("adm-cust-artworks-empty")).style(mutedText.margin(0.px))
    else
      div.style(stackSm)(
        artworks.map { entry =>
          val art      = entry.artwork
          val rev      = art.latestRevision
          val ctxTitle = Some(s"${entry.orderNumber.value} · ${entry.productName(lang)}")
          div.cssClass("mp-cust-card")(
            div.style(rowBetween.flexWrap(FlexWrap.wrap))(
              div.style(Style.row.align(Alignment.center).gap(12.px).minWidth(0.px).flexGrow(1.0))(
                ArtworkComponents.renderThumbnail(rev, sizePx = 52, onClick = Some(() => state.artworkPreview.open(art, contextTitle = ctxTitle))),
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
                      .onClick(state.navigate(AdminRoute.OrderDetail(entry.orderId)))(entry.orderNumber.value),
                    span(s"· #${entry.lineIndex + 1} ${entry.productName(lang)} (${art.componentKey.value})")
                      .style(smallText.fontWeight(FontWeight.w600).color(inkSecondary)),
                  ),
                  span(
                    s"${ArtworkComponents.formatFileSize(rev.file.sizeBytes)} · ${rev.extractedSpecs.resolutionDpi} DPI · ${rev.extractedSpecs.colorSpace} · ${if isCs then "Vytvořeno" else "Created"}: ${formatInstant(art.createdAt)}"
                  ).style(smallText),
                ),
              ),
              div.style(rowXs.flexWrap(FlexWrap.wrap))(
                button
                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                  .onClick(state.artworkPreview.open(art, contextTitle = ctxTitle))(state.i18n.t("adm-od-artwork-preview")),
                ArtworkComponents.downloadButton(rev, lang),
                ArtworkComponents.copyLinkButton(rev, lang, onCopied = () => state.notifyLinkCopied(rev.file.shareableUrl)),
              ),
            ),
          )
        }*
      )

  // ── Notes & Activity tab ───────────────────────────────────────────────────

  private def renderActivity(state: BackofficeState, c: CustomerView, noteRef: SignalRef[String], canManage: Boolean, lang: Language): UI =
    val isCs = lang == Language.Cs
    div.style(stackMd)(
      sectionCard(Icon.Message, span(state.i18n.t("adm-cust-notes-title")))(
        if canManage then
          div.style(stackSm)(
            textarea
              .cssClass("mp-input")
              .style(Style.minHeight(72.px))
              .placeholder(
                if isCs then "Interní poznámka (zákazník ji neuvidí) — např. preferuje doručení kurýrem..."
                else "Internal note (never shown to the customer) — e.g. prefers courier delivery...",
              )
              .value(noteRef)
              .onInput(v => noteRef.set(v)),
            div.style(rowSm)(
              button
                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                .onClick(noteRef.get.flatMap(text => state.addCustomerNote(c.id, text).map(ok => (if ok then noteRef.set("") else Kyo.unit): Unit < Async)))(
                  Icons(Icon.Plus, 14),
                  state.i18n.t("adm-od-notes-add"),
                ),
            ),
          )
        else UI.empty,
        if c.notes.isEmpty then emptyHint(if isCs then "Žádné poznámky." else "No account notes yet.")
        else
          div.style(stackXs)(
            c.notes.sortBy(_.createdAt).reverse.map { n =>
              div.cssClass("mp-cust-note")(
                span(n.text.value).style(Style.fontSize(13.px).color(ink)),
                span(s"${formatInstant(n.createdAt)}${n.authorId.map(a => s" · ${a.value}").getOrElse("")}").style(smallText),
              )
            }*
          ),
      ),
      sectionCard(Icon.Clock, span(state.i18n.t("adm-cust-audit-title")), badge(c.auditLog.size.toString, BadgeTone.Neutral))(
        if c.auditLog.isEmpty then emptyHint(if isCs then "Žádné auditní záznamy." else "No audit entries recorded.")
        else
          div.cssClass("mp-timeline")(
            c.auditLog.sortBy(_.occurredAt).reverse.map { entry =>
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
    )

  private def actorLabel(a: CustomerActor, lang: Language): String = a match
    case CustomerActor.System                => if lang == Language.Cs then "Systém" else "System"
    case CustomerActor.CustomerSelf(_, name) => s"${if lang == Language.Cs then "Zákazník" else "Customer"}: ${name.value}"
    case CustomerActor.Employee(id, nameOpt) => s"${if lang == Language.Cs then "Zaměstnanec" else "Staff"}: ${nameOpt.map(_.value).getOrElse(id.value)}"

end CustomerManagerView
