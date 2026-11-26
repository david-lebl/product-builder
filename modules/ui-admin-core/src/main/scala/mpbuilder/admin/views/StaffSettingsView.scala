package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.identity.{CreateStaffCmd, EmployeeRole}
import mpbuilder.ordering.{BankAccountConfig, CourierServiceOption, DeliveryOptionId, ShopPickupLocation}
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.TabBar
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Staff Directory (Admin) & Store Delivery / Bank Transfer QR Settings (Manager | Admin). */
object StaffSettingsView:

  private enum Tab:
    case Staff, Delivery, Bank

  def render(state: BackofficeState): UI < Sync =
    for
      tabRef        <- Signal.initRef[Tab](Tab.Staff)
      // New staff refs
      stNameRef     <- Signal.initRef("")
      stEmailRef    <- Signal.initRef("")
      stPassRef     <- Signal.initRef("staff1234")
      stRoleRef     <- Signal.initRef[EmployeeRole](EmployeeRole.Operator)
      // Pickup location refs
      pkIdRef       <- Signal.initRef("")
      pkNameEnRef   <- Signal.initRef("")
      pkNameCsRef   <- Signal.initRef("")
      pkAddrRef     <- Signal.initRef("")
      // Courier refs
      crIdRef       <- Signal.initRef("")
      crNameEnRef   <- Signal.initRef("")
      crNameCsRef   <- Signal.initRef("")
      crCurRef      <- Signal.initRef[Currency](Currency.CZK)
      crFeeRef      <- Signal.initRef("149")
      crFreeRef     <- Signal.initRef("2500")
      // Bank account refs
      bkCurRef      <- Signal.initRef[Currency](Currency.CZK)
      bkIbanRef     <- Signal.initRef("")
      bkBicRef      <- Signal.initRef("")
      bkHolderRef   <- Signal.initRef("")
    yield
      val combined =
        tabRef
          .combineLatest(state.staffMembers)
          .combineLatest(state.deliverySettings)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      combined.render { case ((((activeTab, staffList), delivOpt), authOpt), lang) =>
        val isCs      = lang == Language.Cs
        val canManage = state.canManage(authOpt)
        val isAdmin   = state.isAdmin(authOpt)

        val tabs = List(
          TabBar.TabItem(Tab.Staff, if isCs then "Zaměstnanci" else "Staff Directory", Some(staffList.size)),
          TabBar.TabItem(Tab.Delivery, if isCs then "Odběr a dopravci" else "Pickup & Couriers", delivOpt.map(_.pickupLocations.size)),
          TabBar.TabItem(Tab.Bank, if isCs then "Bankovní QR účty" else "Bank QR Accounts", delivOpt.map(_.bankAccounts.size)),
        )

        div.style(stackMd.width(100.pct))(
          div.style(stackXs)(
            h1(state.i18n.t("adm-set-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
            p(state.i18n.t("adm-set-subtitle")).style(mutedText.margin(0.px)),
          ),

          TabBar.render[Tab](
            items = tabs,
            active = activeTab,
            onSelect = t => tabRef.set(t),
          ),

          activeTab match
            case Tab.Staff =>
              div.style(stackMd)(
                if !isAdmin then
                  alertBanner(BadgeTone.Amber, span("Admin Role Required"), List(if lang == Language.Cs then "Zakládání zaměstnanců a změna rolí vyžaduje roli Admin." else "Staff creation and role management require the Admin role."))
                else
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-set-staff-add")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-field-row")(
                      div.cssClass("mp-field")(
                        label("Display Name").style(labelStyle),
                        input.cssClass("mp-input").placeholder("Lukáš Novotný").value(stNameRef).onInput(v => stNameRef.set(v)),
                      ),
                      div.cssClass("mp-field")(
                        label("Work Email").style(labelStyle),
                        emailInput.cssClass("mp-input").placeholder("lukas@printshop.cz").value(stEmailRef).onInput(v => stEmailRef.set(v)),
                      ),
                      div.cssClass("mp-field")(
                        label("Initial Password").style(labelStyle),
                        input.cssClass("mp-input").value(stPassRef).onInput(v => stPassRef.set(v)),
                      ),
                      div.cssClass("mp-field")(
                        label("Primary Role").style(labelStyle),
                        stRoleRef.render { r =>
                          select.cssClass("mp-input").value(r.toString).onChange(v => EmployeeRole.values.find(_.toString == v).map(stRoleRef.set).getOrElse(Kyo.unit))(
                            EmployeeRole.values.toList.map(x => option.value(x.toString).selected(x == r)(x.toString))*
                          )
                        },
                      ),
                    ),
                    div.style(rowSm)(
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .onClick {
                          for
                            name <- stNameRef.get.map(_.trim)
                            em   <- stEmailRef.get.map(_.trim)
                            pw   <- stPassRef.get
                            role <- stRoleRef.get
                            _ <- (EmailAddress(em).toEither, NonEmptyString(name).toEither, NonEmptyString(pw).toEither) match
                                   case (Left(e), _, _) => state.errorBanner.set(Some(e.mkString(", ")))
                                   case (_, Left(e), _) => state.errorBanner.set(Some(e.mkString(", ")))
                                   case (_, _, Left(e)) => state.errorBanner.set(Some(e.mkString(", ")))
                                   case (Right(email), Right(dn), Right(pass)) =>
                                     val empId = EmployeeId.unsafe(s"emp-${em.takeWhile(_ != '@').toLowerCase}")
                                     state.createStaffMember(CreateStaffCmd(empId, dn, email, Set(role), pass))
                                       .andThen(stNameRef.set(""))
                                       .andThen(stEmailRef.set(""))
                          yield ()
                        }(state.i18n.t("adm-set-staff-add")),
                    ),
                  ),

                div.cssClass("mp-card")(
                  if staffList.isEmpty then
                    span(if lang == Language.Cs then "Seznam zaměstnanců je dostupný pro role Manager a Admin." else "Staff directory is visible to Manager and Admin roles.").style(mutedText)
                  else
                    div.style(stackSm)(
                      staffList.sortBy(_.employeeId.value).map { emp =>
                        div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                          div.style(stackXs)(
                            div.style(rowSm.flexWrap(FlexWrap.wrap))(
                              span(emp.displayName.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
                              badge(emp.employeeId.value, BadgeTone.Neutral),
                              badge(if emp.isActive then "Active" else "Inactive", if emp.isActive then BadgeTone.Emerald else BadgeTone.Rose),
                            ),
                            span(emp.email.value).style(smallText),
                            div.style(rowWrap)(
                              EmployeeRole.values.toList.map { r =>
                                val has = emp.roles.contains(r)
                                if isAdmin then
                                  button
                                    .cssClass(if has then "mp-pill-active" else "mp-pill")
                                    .style(Style.padding(3.px, 10.px).fontSize(11.px))
                                    .onClick {
                                      val next = if has then emp.roles - r else emp.roles + r
                                      state.updateStaffRoles(emp.employeeId, next)
                                    }(r.toString)
                                else if has then badge(r.toString, BadgeTone.Brand)
                                else UI.empty
                              }*
                            ),
                          ),
                          if isAdmin then
                            button
                              .cssClass(if emp.isActive then "mp-btn mp-btn-danger mp-btn-sm" else "mp-btn mp-btn-primary mp-btn-sm")
                              .onClick(state.setStaffActive(emp.employeeId, !emp.isActive))(
                                if emp.isActive then state.i18n.t("adm-set-staff-deactivate") else state.i18n.t("adm-set-staff-activate")
                              )
                          else UI.empty,
                        )
                      }*
                    ),
                ),
              )

            case Tab.Delivery =>
              delivOpt match
                case None => div.cssClass("mp-card")(span("Loading settings...").style(mutedText))
                case Some(ds) =>
                  div.style(stackMd)(
                    // Pickup Locations
                    div.cssClass("mp-card")(
                      h2(if lang == Language.Cs then "Odběrná místa" else "Shop Pickup Locations").style(sectionTitle.margin(0.px)),
                      if canManage then
                        div.style(stackSm)(
                          div.cssClass("mp-field-row")(
                            textField("Location ID", "shop-ostrava", pkIdRef),
                            textField("Name (EN)", "Ostrava Pick-Up Point", pkNameEnRef),
                            textField("Name (CS)", "Výdejna Ostrava", pkNameCsRef),
                            textField("Address", "Nádražní 15, Ostrava", pkAddrRef),
                          ),
                          div.style(rowSm)(
                            button
                              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                              .onClick {
                                for
                                  id   <- pkIdRef.get.map(_.trim)
                                  en   <- pkNameEnRef.get.map(_.trim)
                                  cs   <- pkNameCsRef.get.map(_.trim)
                                  addr <- pkAddrRef.get.map(_.trim)
                                  _ <-
                                    if id.isEmpty || en.isEmpty || addr.isEmpty then
                                      state.errorBanner.set(Some("Location ID, Name, and Address are required."))
                                    else
                                      val loc = ShopPickupLocation(
                                        id = DeliveryOptionId.unsafe(id),
                                        name = LocalizedString(en, if cs.nonEmpty then cs else en),
                                        address = LocalizedString(addr, addr),
                                        openingHours = Some(LocalizedString("Mon–Fri 8:00–17:00", "Po–Pá 8:00–17:00")),
                                        isActive = true,
                                      )
                                      state.upsertPickupLocation(loc).andThen(pkIdRef.set("")).andThen(pkNameEnRef.set("")).andThen(pkNameCsRef.set("")).andThen(pkAddrRef.set(""))
                                yield ()
                              }(state.i18n.t("adm-set-pickup-add")),
                          ),
                        )
                      else UI.empty,
                      div.style(stackSm)(
                        ds.pickupLocations.map { loc =>
                          div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                            div.style(stackXs)(
                              div.style(rowSm)(
                                span(loc.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                                badge(loc.id.value, BadgeTone.Neutral),
                              ),
                              span(loc.address(lang)).style(smallText),
                            ),
                            if canManage then
                              button
                                .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                                .onClick(state.deletePickupLocation(loc.id))(state.i18n.t("adm-cat-delete"))
                            else UI.empty,
                          )
                        }*
                      ),
                    ),

                    // Courier Options
                    div.cssClass("mp-card")(
                      h2(if lang == Language.Cs then "Kurýrní dopravci" else "Courier Services").style(sectionTitle.margin(0.px)),
                      if canManage then
                        div.style(stackSm)(
                          div.cssClass("mp-field-row")(
                            textField("Courier ID", "courier-ppl-czk", crIdRef),
                            textField("Name (EN)", "PPL Parcel CZ", crNameEnRef),
                            textField("Name (CS)", "PPL Balík ČR", crNameCsRef),
                            div.cssClass("mp-field")(
                              label("Currency").style(labelStyle),
                              crCurRef.render { c =>
                                select.cssClass("mp-input").value(c.toString).onChange(v => Currency.values.find(_.toString == v).map(crCurRef.set).getOrElse(Kyo.unit))(
                                  List(Currency.CZK, Currency.USD).map(x => option.value(x.toString).selected(x == c)(x.toString))*
                                )
                              },
                            ),
                            textField("Fee Net", "149", crFeeRef),
                            textField("Free Over Net", "2500", crFreeRef),
                          ),
                          div.style(rowSm)(
                            button
                              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                              .onClick {
                                for
                                  id    <- crIdRef.get.map(_.trim)
                                  en    <- crNameEnRef.get.map(_.trim)
                                  cs    <- crNameCsRef.get.map(_.trim)
                                  cur   <- crCurRef.get
                                  feeS  <- crFeeRef.get.map(_.trim)
                                  freeS <- crFreeRef.get.map(_.trim)
                                  _ <-
                                    if id.isEmpty || en.isEmpty then
                                      state.errorBanner.set(Some("Courier ID and Name are required."))
                                    else
                                      val fee  = scala.util.Try(BigDecimal(feeS)).getOrElse(BigDecimal(149))
                                      val free = scala.util.Try(BigDecimal(freeS)).toOption.filter(_ > 0).map(Money.apply)
                                      val opt = CourierServiceOption(
                                        id = DeliveryOptionId.unsafe(id),
                                        name = LocalizedString(en, if cs.nonEmpty then cs else en),
                                        estimatedDelivery = LocalizedString("1–2 business days", "1–2 pracovní dny"),
                                        feeNet = Money(fee),
                                        currency = cur,
                                        taxRate = TaxRate.czStandard,
                                        freeShippingThresholdNet = free,
                                      )
                                      state.upsertCourierOption(opt).andThen(crIdRef.set("")).andThen(crNameEnRef.set("")).andThen(crNameCsRef.set(""))
                                yield ()
                              }(state.i18n.t("adm-set-courier-add")),
                          ),
                        )
                      else UI.empty,
                      div.style(stackSm)(
                        ds.couriersByCurrency.toList.sortBy(_._1.toString).flatMap { case (cur, opts) =>
                          opts.map { o =>
                            div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                              div.style(stackXs)(
                                div.style(rowSm.flexWrap(FlexWrap.wrap))(
                                  span(o.name(lang)).style(Style.fontWeight(FontWeight.w700).color(ink)),
                                  badge(cur.toString, BadgeTone.Brand),
                                  badge(formatMoney(o.feeNet, cur, lang), BadgeTone.Neutral),
                                  o.freeShippingThresholdNet.map(f => badge(s"Free > ${formatMoney(f, cur, lang)}", BadgeTone.Emerald)).getOrElse(UI.empty),
                                ),
                                span(s"${o.id.value} · ${o.estimatedDelivery(lang)}").style(smallText),
                              ),
                              if canManage then
                                button
                                  .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                                  .onClick(state.deleteCourierOption(cur, o.id))(state.i18n.t("adm-cat-delete"))
                              else UI.empty,
                            )
                          }
                        }*
                      ),
                    ),
                  )

            case Tab.Bank =>
              delivOpt match
                case None => div.cssClass("mp-card")(span("Loading bank accounts...").style(mutedText))
                case Some(ds) =>
                  div.style(stackMd)(
                    if canManage then
                      div.cssClass("mp-card")(
                        h2(state.i18n.t("adm-set-bank-save")).style(sectionTitle.margin(0.px)),
                        div.cssClass("mp-field-row")(
                          div.cssClass("mp-field")(
                            label("Currency").style(labelStyle),
                            bkCurRef.render { c =>
                              select.cssClass("mp-input").value(c.toString).onChange(v => Currency.values.find(_.toString == v).map(bkCurRef.set).getOrElse(Kyo.unit))(
                                List(Currency.CZK, Currency.USD, Currency.EUR).map(x => option.value(x.toString).selected(x == c)(x.toString))*
                              )
                            },
                          ),
                          textField("IBAN", "CZ6508000000192000145399", bkIbanRef),
                          textField("BIC / SWIFT", "GIBACZPX", bkBicRef),
                          textField("Account Holder", "PrintCraft Studio s.r.o.", bkHolderRef),
                        ),
                        div.style(rowSm)(
                          button
                            .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                            .onClick {
                              for
                                cur    <- bkCurRef.get
                                iban   <- bkIbanRef.get.map(_.trim)
                                bic    <- bkBicRef.get.map(_.trim)
                                holder <- bkHolderRef.get.map(_.trim)
                                _ <-
                                  if iban.isEmpty || holder.isEmpty then
                                    state.errorBanner.set(Some("IBAN and Account Holder are required."))
                                  else
                                    val acc = BankAccountConfig(
                                      iban = NonEmptyString.unsafe(iban),
                                      bic = NonEmptyString(bic).toOption,
                                      accountHolder = NonEmptyString.unsafe(holder),
                                    )
                                    state.upsertBankAccount(cur, acc)
                              yield ()
                            }(state.i18n.t("adm-set-bank-save")),
                        ),
                      )
                    else UI.empty,

                    div.cssClass("mp-card")(
                      div.style(stackSm)(
                        ds.bankAccounts.toList.sortBy(_._1.toString).map { case (cur, acc) =>
                          div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                            div.style(stackXs)(
                              div.style(rowSm)(
                                badge(cur.toString, BadgeTone.Brand),
                                span(acc.accountHolder.value).style(Style.fontWeight(FontWeight.w700).color(ink)),
                              ),
                              span(s"IBAN: ${acc.iban.value}${acc.bic.map(b => s" · BIC: ${b.value}").getOrElse("")}").style(smallText),
                            ),
                            if canManage then
                              button
                                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                .onClick(
                                  bkCurRef.set(cur)
                                    .andThen(bkIbanRef.set(acc.iban.value))
                                    .andThen(bkBicRef.set(acc.bic.map(_.value).getOrElse("")))
                                    .andThen(bkHolderRef.set(acc.accountHolder.value))
                                )(state.i18n.t("adm-cat-edit"))
                            else UI.empty,
                          )
                        }*
                      ),
                    ),
                  )
        )
      }

  private def textField(lbl: String, ph: String, ref: SignalRef[String]): UI =
    div.cssClass("mp-field")(
      label(lbl).style(labelStyle),
      input.cssClass("mp-input").placeholder(ph).value(ref).onInput(v => ref.set(v)),
    )
