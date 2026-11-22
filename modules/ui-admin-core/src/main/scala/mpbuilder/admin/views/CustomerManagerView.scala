package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.customers.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Customer Accounts & B2B Approval Manager:
  * Filter/search directory, Register new customer, Approve/Suspend/Deactivate, Tier & NetInvoice credit editor, notes & orders.
  */
object CustomerManagerView:

  private def statusTone(s: CustomerStatus): BadgeTone = s match
    case CustomerStatus.Active          => BadgeTone.Emerald
    case CustomerStatus.PendingApproval => BadgeTone.Amber
    case CustomerStatus.Suspended       => BadgeTone.Rose
    case CustomerStatus.Deactivated     => BadgeTone.Neutral

  def render(state: BackofficeState): UI < Sync =
    for
      showRegRef     <- Signal.initRef(false)
      regIdRef       <- Signal.initRef("")
      regSegRef      <- Signal.initRef[CustomerSegment](CustomerSegment.Business)
      regCompRef     <- Signal.initRef("")
      regIcoRef      <- Signal.initRef("")
      regFirstRef    <- Signal.initRef("")
      regLastRef     <- Signal.initRef("")
      regEmailRef    <- Signal.initRef("")
      regCityRef     <- Signal.initRef("Prague")
      // Detail controls
      reasonRef      <- Signal.initRef("")
      dueDaysRef     <- Signal.initRef("14")
      creditLimitRef <- Signal.initRef("100000")
      noteRef        <- Signal.initRef("")
    yield
      val combined =
        state.customers
          .combineLatest(state.customerStatusFilter)
          .combineLatest(state.customerSearch)
          .combineLatest(state.selectedCustomer)
          .combineLatest(showRegRef)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      combined.render { case ((((((allCustomers, statusFilt), queryRaw), detailOpt), showReg), authOpt), lang) =>
        val canManage = state.canManage(authOpt)
        val q         = queryRaw.trim.toLowerCase
        val filtered = allCustomers.filter { c =>
          val okStatus = statusFilt.forall(_ == c.status)
          val okQuery =
            if q.isEmpty then true
            else
              c.contact.firstName.value.toLowerCase.contains(q) ||
                c.contact.lastName.value.toLowerCase.contains(q) ||
                c.contact.email.value.toLowerCase.contains(q) ||
                c.company.exists(_.legalName.value.toLowerCase.contains(q)) ||
                c.company.exists(_.registrationNumber.value.toLowerCase.contains(q))
          okStatus && okQuery
        }

        div.style(stackMd.width(100.pct))(
          // Header
          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
            div.style(stackXs)(
              h1(state.i18n.t("adm-cust-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
              p(state.i18n.t("adm-cust-subtitle")).style(mutedText.margin(0.px)),
            ),
            if canManage then
              button
                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                .onClick(showRegRef.updateAndGet(!_).unit)(state.i18n.t("adm-cust-register-new"))
            else UI.empty,
          ),

          // Register New Customer collapsible panel
          if canManage && showReg then
            div.cssClass("mp-card")(
              h2(state.i18n.t("adm-cust-register-new")).style(sectionTitle.margin(0.px)),
              div.cssClass("mp-field-row")(
                div.cssClass("mp-field")(
                  label("Segment").style(labelStyle),
                  regSegRef.render { seg =>
                    select.cssClass("mp-input").value(seg.toString).onChange(v => CustomerSegment.values.find(_.toString == v).map(regSegRef.set).getOrElse(Kyo.unit))(
                      CustomerSegment.values.toList.map(s => option.value(s.toString)(s.toString))*
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
                               state.registerCustomer(cmd).andThen(showRegRef.set(false)).andThen(regEmailRef.set(""))
                    yield ()
                  }(state.i18n.t("adm-cat-save")),
                button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(showRegRef.set(false))(state.i18n.t("adm-cat-cancel")),
              ),
            )
          else UI.empty,

          // Filter & Search bar
          div.cssClass("mp-card").style(Style.gap(10.px).padding(14.px))(
            searchInput
              .cssClass("mp-input")
              .placeholder(if lang == Language.Cs then "Hledat jméno, e-mail, firmu nebo IČO..." else "Search name, email, company, or IČO...")
              .value(state.customerSearch)
              .onInput(v => state.customerSearch.set(v)),
            div.style(rowWrap)(
              button
                .cssClass(if statusFilt.isEmpty then "mp-pill-active" else "mp-pill")
                .onClick(state.customerStatusFilter.set(None))(
                  span(state.i18n.t("adm-cust-filter-all")),
                  span(s"(${allCustomers.size})").style(Style.fontSize(11.px).opacity(0.85)),
                ),
              UI.fragment(
                CustomerStatus.values.toList.map { st =>
                  val cnt = allCustomers.count(_.status == st)
                  button
                    .cssClass(if statusFilt.contains(st) then "mp-pill-active" else "mp-pill")
                    .onClick(state.customerStatusFilter.set(Some(st)))(
                      span(st.toString),
                      span(s"($cnt)").style(Style.fontSize(11.px).opacity(0.85)),
                    )
                }*
              ),
            ),
          ),

          // Master-Detail split
          div.cssClass("mp-admin-split")(
            // Left: Customer List
            div.cssClass("mp-admin-split-left")(
              div.style(stackSm)(
                filtered.map { c =>
                  val isSelected = detailOpt.exists(_.customer.id == c.id)
                  val title = c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}")
                  button
                    .cssClass(if isSelected then "mp-option-card-active" else "mp-option-card")
                    .style(Style.flexBasis(Length.Auto).width(100.pct))
                    .onClick(state.selectCustomer(c.id))(
                      div.style(rowBetween.width(100.pct))(
                        span(title).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                        badge(c.status.toString, statusTone(c.status)),
                      ),
                      div.style(rowWrap)(
                        badge(c.segment.toString, BadgeTone.Neutral),
                        badge(tierLabel(c.tier), BadgeTone.Brand),
                        c.paymentTerms match
                          case PaymentTerms.NetInvoice(d, _) => badge(s"Net-$d", BadgeTone.Emerald)
                          case PaymentTerms.PrepaidOnly      => badge("Prepaid", BadgeTone.Neutral),
                      ),
                      span(s"${c.contact.firstName.value} ${c.contact.lastName.value} · ${c.contact.email.value}").style(smallText),
                    )
                }*
              ),
            ),

            // Right: Selected Customer Detail
            div.cssClass("mp-admin-split-right")(
              detailOpt match
                case None =>
                  div.cssClass("mp-card").style(Style.align(Alignment.center).padding(48.px, 24.px))(
                    p(state.i18n.t("adm-cust-select-hint")).style(mutedText.margin(0.px)),
                  )

                case Some(detail) =>
                  val c = detail.customer
                  div.style(stackMd)(
                    // Profile & Status Actions
                    div.cssClass("mp-card")(
                      div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                        div.style(stackXs)(
                          h2(c.company.map(_.legalName.value).getOrElse(s"${c.contact.firstName.value} ${c.contact.lastName.value}"))
                            .style(sectionTitle.margin(0.px)),
                          span(s"${c.contact.firstName.value} ${c.contact.lastName.value} · ${c.contact.email.value}").style(smallText),
                        ),
                        div.style(rowXs)(
                          badge(c.status.toString, statusTone(c.status)),
                          badge(tierLabel(c.tier), BadgeTone.Brand),
                        ),
                      ),
                      c.company match
                        case Some(comp) =>
                          div.style(smallText)(
                            s"IČO: ${comp.registrationNumber.value}${comp.vatId.map(v => s" · DIČ: ${v.value}").getOrElse("")}"
                          )
                        case None => UI.empty,
                      div.style(smallText)(
                        s"${c.billingAddress.street.value}, ${c.billingAddress.postalCode.value} ${c.billingAddress.city.value}, ${c.billingAddress.countryCode.value}"
                      ),

                      // Lifecycle Actions (Manager/Admin)
                      if canManage then
                        div.style(Style.column.gap(10.px).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                          div.style(rowWrap)(
                            if c.status == CustomerStatus.PendingApproval || c.status == CustomerStatus.Suspended then
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(state.approveCustomer(c.id))(state.i18n.t("adm-cust-approve"))
                            else UI.empty,
                          ),
                          div.style(rowSm)(
                            input
                              .cssClass("mp-input")
                              .placeholder(if lang == Language.Cs then "Důvod pozastavení / deaktivace..." else "Reason for suspend / deactivate...")
                              .value(reasonRef)
                              .onInput(v => reasonRef.set(v)),
                            if c.status == CustomerStatus.Active then
                              button
                                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                .style(Style.flexShrink(0.0))
                                .onClick(reasonRef.get.flatMap(r => state.suspendCustomer(c.id, r).andThen(reasonRef.set(""))))(state.i18n.t("adm-cust-suspend"))
                            else UI.empty,
                            if c.status != CustomerStatus.Deactivated then
                              button
                                .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                                .style(Style.flexShrink(0.0))
                                .onClick(reasonRef.get.flatMap(r => state.deactivateCustomer(c.id, r).andThen(reasonRef.set(""))))(state.i18n.t("adm-cust-deactivate"))
                            else UI.empty,
                          ),

                          // Tier selector
                          div.style(stackXs)(
                            label(state.i18n.t("adm-cust-tier-label")).style(labelStyle),
                            div.style(rowWrap)(
                              CustomerTier.values.toList.map { t =>
                                button
                                  .cssClass(if c.tier == t then "mp-pill-active" else "mp-pill")
                                  .onClick(state.changeCustomerTier(c.id, t))(t.toString)
                              }*
                            ),
                          ),

                          // Payment Terms editor
                          div.style(stackXs)(
                            label(state.i18n.t("adm-cust-terms-label")).style(labelStyle),
                            div.style(smallText)(
                              c.paymentTerms match
                                case PaymentTerms.PrepaidOnly => "Current: Prepaid Only"
                                case PaymentTerms.NetInvoice(d, lim) =>
                                  s"Current: Net-$d · Limit ${formatMoney(lim.amount, lim.currency, lang)}"
                            ),
                            div.cssClass("mp-field-row")(
                              div.cssClass("mp-field")(
                                label("Due Days").style(smallText),
                                numberInput.cssClass("mp-input").value(dueDaysRef).onInput(v => dueDaysRef.set(v)),
                              ),
                              div.cssClass("mp-field")(
                                label("Credit Limit (CZK)").style(smallText),
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
                    ),

                    // Notes card
                    div.cssClass("mp-card")(
                      h3(state.i18n.t("adm-cust-notes-title")).style(sectionTitle.margin(0.px)),
                      div.style(stackXs)(
                        if c.notes.isEmpty then span(if lang == Language.Cs then "Žádné poznámky." else "No account notes yet.").style(smallText)
                        else
                          UI.fragment(
                            c.notes.map { n =>
                              div.style(Style.column.gap(2.px).padding(8.px, 10.px).rounded(8.px).bg(surfaceAlt))(
                                span(n.text.value).style(Style.fontSize(13.px).color(ink)),
                                span(formatInstant(n.createdAt)).style(smallText),
                              )
                            }*
                          )
                      ),
                      div.style(rowSm)(
                        input
                          .cssClass("mp-input")
                          .placeholder(if lang == Language.Cs then "Přidat poznámku k účtu..." else "Add internal account note...")
                          .value(noteRef)
                          .onInput(v => noteRef.set(v)),
                        button
                          .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                          .style(Style.flexShrink(0.0))
                          .onClick(noteRef.get.flatMap(t => state.addCustomerNote(c.id, t).andThen(noteRef.set(""))))(state.i18n.t("adm-od-notes-add")),
                      ),
                    ),

                    // Customer Orders
                    div.cssClass("mp-card")(
                      div.style(rowBetween)(
                        h3(state.i18n.t("adm-cust-orders-title")).style(sectionTitle.margin(0.px)),
                        badge(detail.orders.size.toString, BadgeTone.Neutral),
                      ),
                      if detail.orders.isEmpty then
                        span(if lang == Language.Cs then "Zákazník zatím nemá žádné objednávky." else "No orders placed by this customer yet.").style(smallText)
                      else
                        div.style(stackXs)(
                          detail.orders.map { o =>
                            div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(10.px, 12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                              div.style(rowSm)(
                                span(o.orderNumber.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
                                badge(orderStatusLabel(o.status, lang), orderStatusTone(o.status)),
                              ),
                              div.style(rowSm)(
                                span(formatMoney(o.totals.grandTotalGross, o.totals.currency, lang)).style(Style.fontWeight(FontWeight.w700)),
                                button
                                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                  .onClick(state.navigate(AdminRoute.OrderDetail(o.id)))(state.i18n.t("adm-orders-open")),
                              ),
                            )
                          }*
                        ),
                    ),
                  )
            ),
          ),
        )
      }
