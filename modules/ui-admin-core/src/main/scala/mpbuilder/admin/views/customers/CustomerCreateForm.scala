package mpbuilder.admin.views.customers

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.BackofficeState
import mpbuilder.admin.views.customers.CustomerDrafts.*
import mpbuilder.admin.views.customers.CustomerUi.*
import mpbuilder.customers.*
import mpbuilder.shared.*
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.theme.AppTheme.*

/** Customer registration form (rendered in the DataTable create pane).
  *
  * Segment-aware (company block only for Business / Agency), full billing address, inline validation incl.
  * duplicate-email and duplicate-ID checks, and an auto-generated readable customer ID.
  */
private[views] object CustomerCreateForm:

  def render(
      state: BackofficeState,
      draftRef: SignalRef[RegisterDraft],
      allCustomers: List[CustomerView],
      lang: Language,
      onCreated: CustomerId => Unit < Async,
      onCancel: Unit < Async,
  ): UI =
    val isCs        = lang == Language.Cs
    val existingIds = allCustomers.map(_.id.value).toSet
    val owners      = emailOwners(allCustomers, except = None)
    def t(k: String): UI = span(state.i18n.t(k))

    draftRef.render { d =>
      val errs  = validateRegister(d, existingIds, owners, isCs)
      val shown = if d.showErrors then errs else Map.empty[String, String]
      val b2b   = isB2B(d.segment)
      def upd(f: RegisterDraft => RegisterDraft): Unit < Async = draftRef.set(f(d))
      def updContact(f: ContactDraft => ContactDraft): Unit < Async = upd(x => x.copy(contact = f(x.contact)))
      def updBilling(f: AddressDraft => AddressDraft): Unit < Async = upd(x => x.copy(billing = f(x.billing)))

      val submit: Unit < Async =
        if errs.nonEmpty then upd(_.copy(showErrors = true))
        else
          state.registerCustomer(registerCmd(d, existingIds)).flatMap {
            case Some(id) => draftRef.set(RegisterDraft()).andThen(onCreated(id))
            case None     => Kyo.unit
          }

      div.style(stackMd)(
        // 1. Customer type
        div.style(stackSm)(
          span(t("adm-cust-f-segment")).style(labelStyle),
          Controls.segmented[CustomerSegment](
            options = List(CustomerSegment.Business, CustomerSegment.Agency, CustomerSegment.Individual).map(s => s -> span(segmentLabel(s, lang))),
            current = d.segment,
            onSelect = s => upd(_.copy(segment = s)),
            fullWidth = true,
            ariaLabel = if isCs then "Typ zákazníka" else "Customer type",
          ),
          div.cssClass("mp-cust-inline-note")(
            Icons(Icon.Info, 14),
            span(
              if b2b then
                if isCs then "Firemní účet začíná ve stavu „Čeká na schválení“. Po schválení lze nastavit fakturu se splatností."
                else "Business accounts start as Pending approval. Once approved you can grant invoice terms."
              else if isCs then "Účet jednotlivce je aktivní ihned, platí vždy předem."
              else "Individual accounts are active immediately and always pay upfront.",
            ),
          ),
        ),
        if d.showErrors then errorSummary(errs.values, lang) else UI.empty,

        // 2. Company
        if b2b then
          div.cssClass("mp-cust-form-group")(
            span(t("adm-cust-sec-company")).cssClass("mp-cust-form-legend"),
            div.cssClass("mp-field-row")(
              field(t("adm-cust-f-company"), d.companyName, v => upd(_.copy(companyName = v)), shown.get("companyName"), required = true, placeholder = "Atelier Nova s.r.o."),
              field(t("adm-cust-f-regno"), d.regNo, v => upd(_.copy(regNo = v)), shown.get("regNo"), required = true, placeholder = "28991122"),
              field(t("adm-cust-f-vat"), d.vatId, v => upd(_.copy(vatId = v)), placeholder = "CZ28991122"),
            ),
          )
        else UI.empty,

        // 3. Primary contact
        div.cssClass("mp-cust-form-group")(
          span(if isCs then "Hlavní kontaktní osoba" else "Primary contact").cssClass("mp-cust-form-legend"),
          div.cssClass("mp-field-row")(
            field(t("adm-cust-f-first"), d.contact.firstName, v => updContact(_.copy(firstName = v)), shown.get("contact.firstName"), required = true, placeholder = "Tereza"),
            field(t("adm-cust-f-last"), d.contact.lastName, v => updContact(_.copy(lastName = v)), shown.get("contact.lastName"), required = true, placeholder = "Králová"),
          ),
          div.cssClass("mp-field-row")(
            field(t("adm-cust-f-email"), d.contact.email, v => updContact(_.copy(email = v)), shown.get("contact.email"), required = true, kind = FieldKind.Email, placeholder = "tereza@ateliernova.cz"),
            field(t("adm-cust-f-phone"), d.contact.phone, v => updContact(_.copy(phone = v)), kind = FieldKind.Phone, placeholder = "+420 601 234 567"),
            if b2b then field(t("adm-cust-f-role"), d.contact.roleTitle, v => updContact(_.copy(roleTitle = v)), placeholder = if isCs then "Např. nákup" else "e.g. Purchasing")
            else UI.empty,
          ),
        ),

        // 4. Billing address
        div.cssClass("mp-cust-form-group")(
          span(t("adm-cust-billing")).cssClass("mp-cust-form-legend"),
          div.cssClass("mp-field-row")(
            field(t("adm-cust-f-street"), d.billing.street, v => updBilling(_.copy(street = v)), shown.get("billing.street"), required = true, placeholder = "Vinohradská 12"),
            field(t("adm-cust-f-city"), d.billing.city, v => updBilling(_.copy(city = v)), shown.get("billing.city"), required = true, placeholder = "Praha"),
          ),
          div.cssClass("mp-field-row")(
            field(t("adm-cust-f-postal"), d.billing.postalCode, v => updBilling(_.copy(postalCode = v)), shown.get("billing.postalCode"), required = true, placeholder = "120 00"),
            field(t("adm-cust-f-country"), d.billing.countryCode, v => updBilling(_.copy(countryCode = v)), shown.get("billing.countryCode"), required = true, placeholder = "CZ"),
          ),
        ),

        // 5. Customer ID (auto-generated unless overridden)
        field(
          t("adm-cust-f-id"),
          d.customId,
          v => upd(_.copy(customId = v)),
          shown.get("customId"),
          placeholder = suggestedId(d, existingIds),
          hint = Some(
            if isCs then "Nepovinné — prázdné pole vygeneruje ID automaticky (viz zástupný text)."
            else "Optional — leave empty to use the generated ID shown as placeholder.",
          ),
        ),

        div.cssClass("mp-cust-form-actions")(
          button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(submit)(Icons(Icon.Check, 14), t("adm-cust-create-submit")),
          button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(draftRef.set(RegisterDraft()).andThen(onCancel))(t("adm-cat-cancel")),
        ),
      )
    }

end CustomerCreateForm
