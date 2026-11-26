package mpbuilder.admin.views.customers

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.admin.views.customers.CustomerDrafts.*
import mpbuilder.admin.views.customers.CustomerUi.*
import mpbuilder.customers.*
import mpbuilder.shared.*
import mpbuilder.ui.components.{ConfirmDialog, Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Profile tab: Company, Contacts, Addresses and Tags cards, each with its own Edit button and inline editor.
  *
  * Only one section is edited at a time. The editing section lives in `editingRef` (rarely changes, part of the
  * parent render) while the keystroke-level draft lives in `draftRef`, rendered locally so typing only re-renders
  * the open form.
  */
private[views] object CustomerProfileTab:

  final case class Refs(
      editing: SignalRef[Option[(CustomerId, Section)]],
      draft: SignalRef[Option[ProfileDraft]],
  )

  def render(
      state: BackofficeState,
      c: CustomerView,
      allCustomers: List[CustomerView],
      editing: Option[Section],
      refs: Refs,
      dialog: ConfirmDialog.Handle,
      canManage: Boolean,
      lang: Language,
  ): UI =
    val isCs        = lang == Language.Cs
    val b2b         = isB2B(c.segment)
    val owners      = emailOwners(allCustomers, except = Some(c.id))
    val canEdit     = canManage && c.status != CustomerStatus.Deactivated
    def t(k: String): UI = span(state.i18n.t(k))

    def startEdit(sec: Section, f: ProfileDraft => ProfileDraft = identity): Unit < Async =
      refs.draft.set(Some(f(ProfileDraft.from(c, sec)))).andThen(refs.editing.set(Some(c.id -> sec)))

    val closeEdit: Unit < Async = refs.draft.set(None).andThen(refs.editing.set(None))

    def save(cmd: UpdateCustomerProfileCmd): Unit < Async =
      state.updateCustomerProfile(cmd).map(ok => (if ok then closeEdit else Kyo.unit): Unit < Async)

    def confirmSave(title: String, message: String, confirmLabel: String, cmd: UpdateCustomerProfileCmd): Unit < Async =
      dialog.open(
        ConfirmDialog.Config(
          title = title,
          message = message,
          confirmLabel = confirmLabel,
          cancelLabel = if isCs then "Zrušit" else "Cancel",
          tone = ConfirmDialog.DialogTone.Danger,
          onConfirm = _ => state.updateCustomerProfile(cmd).unit,
        )
      )

    /** Edit button in a card header — hidden while any section is being edited. */
    def editButton(sec: Section): UI =
      if canEdit && editing.isEmpty then iconAction(Icon.Pencil, t("adm-cust-edit"), startEdit(sec)) else UI.empty

    /** Locally-rendered editor for one section (re-renders on each keystroke without touching the rest of the page). */
    def editor(sec: Section)(body: (ProfileDraft, Errors, (ProfileDraft => ProfileDraft) => Unit < Async) => UI): UI =
      refs.draft.render {
        case Some(d) if d.customerId == c.id && d.section == sec =>
          val errs  = validateProfile(d, c.segment, owners, isCs)
          val shown = if d.showErrors then errs else Map.empty[String, String]
          def upd(f: ProfileDraft => ProfileDraft): Unit < Async = refs.draft.set(Some(f(d)))
          div.style(stackMd)(
            if d.showErrors then errorSummary(errs.values, lang) else UI.empty,
            body(d, shown, upd),
            formActions(
              t("adm-cust-save"),
              t("adm-cat-cancel"),
              if errs.nonEmpty then upd(_.copy(showErrors = true)) else save(profileCmd(d, c)),
              closeEdit,
            ),
          )
        case _ => UI.empty
      }

    def contactFields(prefix: String, cd: ContactDraft, errs: Errors, set: ContactDraft => Unit < Async): UI =
      div.cssClass("mp-field-row")(
        field(t("adm-cust-f-first"), cd.firstName, v => set(cd.copy(firstName = v)), errs.get(s"$prefix.firstName"), required = true),
        field(t("adm-cust-f-last"), cd.lastName, v => set(cd.copy(lastName = v)), errs.get(s"$prefix.lastName"), required = true),
        field(t("adm-cust-f-email"), cd.email, v => set(cd.copy(email = v)), errs.get(s"$prefix.email"), required = true, kind = FieldKind.Email),
        field(t("adm-cust-f-phone"), cd.phone, v => set(cd.copy(phone = v)), kind = FieldKind.Phone, placeholder = "+420 601 234 567"),
        field(t("adm-cust-f-role"), cd.roleTitle, v => set(cd.copy(roleTitle = v)), placeholder = if isCs then "Např. nákup" else "e.g. Purchasing"),
      )

    def addressFields(prefix: String, a: AddressDraft, errs: Errors, set: AddressDraft => Unit < Async): UI =
      div.cssClass("mp-field-row")(
        field(t("adm-cust-f-street"), a.street, v => set(a.copy(street = v)), errs.get(s"$prefix.street"), required = true),
        field(t("adm-cust-f-city"), a.city, v => set(a.copy(city = v)), errs.get(s"$prefix.city"), required = true),
        field(t("adm-cust-f-postal"), a.postalCode, v => set(a.copy(postalCode = v)), errs.get(s"$prefix.postalCode"), required = true),
        field(t("adm-cust-f-country"), a.countryCode, v => set(a.copy(countryCode = v)), errs.get(s"$prefix.countryCode"), required = true, placeholder = "CZ"),
      )

    // ── Company ──────────────────────────────────────────────────────────────
    val companyCard: UI =
      if !b2b then UI.empty
      else
        val isEditing = editing.contains(Section.Company)
        sectionCard(Icon.Building, t("adm-cust-sec-company"), if isEditing then UI.empty else editButton(Section.Company), isEditing)(
          if isEditing then
            editor(Section.Company) { (d, errs, upd) =>
              div.cssClass("mp-field-row")(
                field(t("adm-cust-f-company"), d.companyName, v => upd(_.copy(companyName = v)), errs.get("companyName"), required = true),
                field(t("adm-cust-f-regno"), d.regNo, v => upd(_.copy(regNo = v)), errs.get("regNo"), required = true),
                field(t("adm-cust-f-vat"), d.vatId, v => upd(_.copy(vatId = v)), placeholder = "CZ12345678"),
              )
            }
          else
            c.company match
              case Some(comp) =>
                div.cssClass("mp-cust-kv")(
                  kv(t("adm-cust-f-company"), comp.legalName.value),
                  kv(t("adm-cust-f-regno"), comp.registrationNumber.value),
                  kv(t("adm-cust-f-vat"), comp.vatId.map(_.value).getOrElse("—")),
                )
              case None => emptyHint(if isCs then "Firemní údaje chybí." else "Company details missing.")
        )

    // ── Contacts ─────────────────────────────────────────────────────────────
    val contactsCard: UI =
      val isEditing = editing.contains(Section.Contacts)
      val headActions =
        if isEditing || !canEdit || editing.nonEmpty then UI.empty
        else
          UI.fragment(
            if b2b then iconAction(Icon.Plus, t("adm-cust-add-contact-short"), startEdit(Section.Contacts, d => d.copy(extras = d.extras :+ ContactDraft.empty)))
            else UI.empty,
            editButton(Section.Contacts),
          )

      def contactRow(cp: ContactPerson, primary: Boolean, actions: UI): UI =
        div.cssClass(if primary then "mp-cust-contact mp-cust-contact-primary" else "mp-cust-contact")(
          Controls.avatar(personName(cp), "sm"),
          div.style(Style.column.gap(2.px).minWidth(0.px).flexGrow(1.0))(
            div.style(rowXs.flexWrap(FlexWrap.wrap))(
              span(personName(cp)).style(Style.fontWeight(FontWeight.w700).color(ink)),
              if primary then badge(if isCs then "Hlavní kontakt" else "Primary", BadgeTone.Brand) else UI.empty,
              cp.roleTitle.map(r => badge(r.value, BadgeTone.Neutral)).getOrElse(UI.empty),
            ),
            div.style(rowXs.flexWrap(FlexWrap.wrap))(
              span(Icons(Icon.Mail, 12)).style(Style.color(inkMuted)),
              span(cp.email.value).style(smallText),
              cp.phone
                .map(ph => UI.fragment(span(Icons(Icon.Phone, 12)).style(Style.color(inkMuted).margin(0.px, 0.px, 0.px, 6.px)), span(ph.value).style(smallText)))
                .getOrElse(UI.empty),
            ),
          ),
          div.style(rowXs.flexShrink(0.0).flexWrap(FlexWrap.wrap))(actions),
        )

      sectionCard(Icon.Users, t("adm-cust-contacts-title"), headActions, isEditing)(
        if isEditing then
          editor(Section.Contacts) { (d, errs, upd) =>
            div.style(stackMd)(
              div.style(stackSm)(
                span(if isCs then "Hlavní kontaktní osoba" else "Primary contact").style(labelStyle),
                contactFields("primary", d.primary, errs, cd => upd(_.copy(primary = cd))),
              ),
              if b2b then
                div.style(stackSm)(
                  UI.fragment(d.extras.zipWithIndex.map { case (cd, i) =>
                    div.cssClass("mp-cust-subform")(
                      div.style(rowBetween)(
                        span(s"${if isCs then "Další kontakt" else "Contact"} #${i + 2}").style(labelStyle),
                        iconAction(Icon.Trash, t("adm-cust-remove"), upd(dd => dd.copy(extras = dd.extras.patch(i, Nil, 1))), "mp-btn-ghost mp-cust-danger"),
                      ),
                      contactFields(s"extras.$i", cd, errs, nc => upd(dd => dd.copy(extras = dd.extras.updated(i, nc)))),
                    )
                  }*),
                  div.style(rowSm)(
                    iconAction(Icon.Plus, t("adm-cust-add-contact-short"), upd(dd => dd.copy(extras = dd.extras :+ ContactDraft.empty)), "mp-btn-secondary")
                  ),
                )
              else emptyHint(if isCs then "Účet jednotlivce má jedinou kontaktní osobu." else "Individual accounts have a single contact person."),
            )
          }
        else
          div.style(stackSm)(
            contactRow(c.contact, primary = true, UI.empty),
            UI.fragment(
              c.additionalContacts.zipWithIndex.map { case (cp, i) =>
                contactRow(
                  cp,
                  primary = false,
                  if canEdit && editing.isEmpty then
                    UI.fragment(
                      button
                        .cssClass("mp-btn mp-btn-ghost mp-btn-sm mp-cust-card-btn")
                        .onClick(promoteContactCmd(c, i).map(save).getOrElse(Kyo.unit))(Icons(Icon.Star, 13), t("adm-cust-make-primary")),
                      button
                        .cssClass("mp-icon-btn mp-icon-btn-sm mp-cust-danger")
                        .aria("label", if isCs then s"Odebrat ${personName(cp)}" else s"Remove ${personName(cp)}")
                        .onClick(
                          confirmSave(
                            if isCs then "Odebrat kontaktní osobu" else "Remove contact person",
                            if isCs then s"Odebrat „${personName(cp)}“ z kontaktů účtu?" else s"Remove “${personName(cp)}” from this account's contacts?",
                            if isCs then "Odebrat" else "Remove",
                            removeContactCmd(c, i),
                          )
                        )(Icons(Icon.Trash, 14)),
                    )
                  else UI.empty,
                )
              }*
            ),
            if b2b && c.additionalContacts.isEmpty then
              emptyHint(if isCs then "Žádné další kontaktní osoby." else "No additional contact persons yet.")
            else UI.empty,
          )
      )

    // ── Addresses ────────────────────────────────────────────────────────────
    val addressesCard: UI =
      val isEditing = editing.contains(Section.Addresses)
      val headActions =
        if isEditing || !canEdit || editing.nonEmpty then UI.empty
        else
          UI.fragment(
            iconAction(Icon.Plus, t("adm-cust-add-address"), startEdit(Section.Addresses, d => d.copy(shipping = d.shipping :+ AddressDraft.empty))),
            editButton(Section.Addresses),
          )
      sectionCard(Icon.Truck, t("adm-cust-sec-addresses"), headActions, isEditing)(
        if isEditing then
          editor(Section.Addresses) { (d, errs, upd) =>
            div.style(stackMd)(
              div.style(stackSm)(
                span(t("adm-cust-billing")).style(labelStyle),
                addressFields("billing", d.billing, errs, a => upd(_.copy(billing = a))),
              ),
              div.style(stackSm)(
                span(t("adm-cust-shipping")).style(labelStyle),
                  UI.fragment(d.shipping.zipWithIndex.map { case (a, i) =>
                    val isDef = d.defaultShipping == i
                    div.cssClass("mp-cust-subform")(
                      div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                        div.style(rowXs)(
                          span(s"#${i + 1}").style(labelStyle),
                          if isDef then badge(if isCs then "Výchozí" else "Default", BadgeTone.Emerald)
                          else
                            button
                              .cssClass("mp-btn mp-btn-ghost mp-btn-sm mp-cust-card-btn")
                              .onClick(upd(_.copy(defaultShipping = i)))(t("adm-cust-set-default")),
                        ),
                        iconAction(
                          Icon.Trash,
                          t("adm-cust-remove"),
                          upd(dd =>
                            dd.copy(
                              shipping = dd.shipping.patch(i, Nil, 1),
                              defaultShipping = if i < dd.defaultShipping then dd.defaultShipping - 1 else if i == dd.defaultShipping then 0 else dd.defaultShipping,
                            )
                          ),
                          "mp-btn-ghost mp-cust-danger",
                        ),
                      ),
                      addressFields(s"shipping.$i", a, errs, na => upd(dd => dd.copy(shipping = dd.shipping.updated(i, na)))),
                    )
                  }*),
                  div.style(rowSm)(
                    iconAction(Icon.Plus, t("adm-cust-add-address"), upd(dd => dd.copy(shipping = dd.shipping :+ AddressDraft.empty)), "mp-btn-secondary"),
                    if d.shipping.isEmpty then
                      span(if isCs then "Bez doručovací adresy se použije fakturační." else "Without a delivery address, the billing address is used.").style(smallText)
                    else UI.empty,
                  ),
              ),
            )
          }
        else
          div.style(stackSm)(
            div.cssClass("mp-cust-address")(
              span(t("adm-cust-billing")).style(labelStyle),
              span(formatAddress(c.billingAddress)).style(Style.fontSize(13.px).color(ink)),
            ),
            div.style(stackXs)(
              span(t("adm-cust-shipping")).style(labelStyle),
              (if c.shippingAddresses.isEmpty then
                   emptyHint(if isCs then "Žádné doručovací adresy — použije se fakturační adresa." else "No delivery addresses — billing address is used.")
                 else
                   UI.fragment(c.shippingAddresses.zipWithIndex.map { case (addr, i) =>
                     val isDef = c.defaultShippingIndex.getOrElse(0) == i
                     div.cssClass("mp-cust-address-row")(
                       span(formatAddress(addr)).style(Style.fontSize(13.px).color(ink).flexGrow(1.0).minWidth(0.px)),
                       div.style(rowXs.flexShrink(0.0))(
                         if isDef then badge(if isCs then "Výchozí" else "Default", BadgeTone.Emerald)
                         else if canEdit && editing.isEmpty then
                           button
                             .cssClass("mp-btn mp-btn-ghost mp-btn-sm mp-cust-card-btn")
                             .onClick(save(setDefaultShippingCmd(c, i)))(t("adm-cust-set-default"))
                         else UI.empty,
                         if canEdit && editing.isEmpty then
                           button
                             .cssClass("mp-icon-btn mp-icon-btn-sm mp-cust-danger")
                             .aria("label", if isCs then "Odebrat adresu" else "Remove address")
                             .onClick(
                               confirmSave(
                                 if isCs then "Odebrat doručovací adresu" else "Remove delivery address",
                                 if isCs then s"Odebrat adresu „${formatAddress(addr)}“?" else s"Remove address “${formatAddress(addr)}”?",
                                 if isCs then "Odebrat" else "Remove",
                                 removeShippingCmd(c, i),
                               )
                             )(Icons(Icon.Trash, 14))
                         else UI.empty,
                       ),
                     )
                   }*)),
            ),
          )
      )

    // ── Tags ─────────────────────────────────────────────────────────────────
    val tagsCard: UI =
      val isEditing = editing.contains(Section.Tags)
      sectionCard(Icon.Tag, t("adm-cust-sec-tags"), if isEditing then UI.empty else editButton(Section.Tags), isEditing)(
        if isEditing then
          editor(Section.Tags) { (d, _, upd) =>
            field(
              t("adm-cust-f-tags"),
              d.tags,
              v => upd(_.copy(tags = v)),
              placeholder = if isCs then "vip, pravidelný, agentura" else "vip, recurring, reseller",
              hint = Some(if isCs then "Oddělte štítky čárkou." else "Separate tags with commas."),
            )
          }
        else if c.tags.isEmpty then emptyHint(if isCs then "Bez štítků." else "No tags.")
        else div.style(rowWrap)(c.tags.toList.sorted.map(tag => badge(tag, BadgeTone.Neutral))*)
      )

    div.style(stackMd)(companyCard, contactsCard, addressesCard, tagsCard)

  private def kv(k: UI, v: String): UI =
    div.cssClass("mp-cust-kv-item")(
      span(k).style(smallText),
      span(v).style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(ink)),
    )

end CustomerProfileTab
