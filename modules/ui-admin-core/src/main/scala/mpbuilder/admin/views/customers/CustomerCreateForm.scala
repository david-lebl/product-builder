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
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Customer registration form (rendered in the DataTable create pane).
  *
  * Rendering model: the form tree is static, every input is bound to its own `SignalRef[String]` (`.value(ref)` +
  * `.onInput(ref.set)`), and only small regions are reactive — the segment-dependent blocks, per-field error
  * messages and the generated-ID preview. Typing therefore never re-renders the form. Validation (required fields,
  * email format, duplicate email / ID) runs on submit against the customers loaded at that moment; errors then show
  * inline and in a summary box.
  */
private[views] object CustomerCreateForm:

  final class Refs(
      val segment: SignalRef[CustomerSegment],
      val companyName: SignalRef[String],
      val regNo: SignalRef[String],
      val vatId: SignalRef[String],
      val firstName: SignalRef[String],
      val lastName: SignalRef[String],
      val email: SignalRef[String],
      val phone: SignalRef[String],
      val role: SignalRef[String],
      val street: SignalRef[String],
      val city: SignalRef[String],
      val postalCode: SignalRef[String],
      val countryCode: SignalRef[String],
      val customId: SignalRef[String],
      val errors: SignalRef[Errors],
      val busy: SignalRef[Boolean],
  ):
    private def texts: List[SignalRef[String]] =
      List(companyName, regNo, vatId, firstName, lastName, email, phone, role, street, city, postalCode, customId)

    def draft: RegisterDraft < Sync =
      for
        seg <- segment.get
        co  <- companyName.get
        reg <- regNo.get
        vat <- vatId.get
        fn  <- firstName.get
        ln  <- lastName.get
        em  <- email.get
        ph  <- phone.get
        rl  <- role.get
        st  <- street.get
        ci  <- city.get
        pc  <- postalCode.get
        cc  <- countryCode.get
        id  <- customId.get
      yield RegisterDraft(
        segment = seg,
        companyName = co,
        regNo = reg,
        vatId = vat,
        contact = ContactDraft(fn, ln, em, ph, rl),
        billing = AddressDraft(st, ci, pc, cc),
        customId = id,
      )

    def reset: Unit < Sync =
      for
        _ <- Kyo.foreachDiscard(texts)(_.set(""))
        _ <- segment.set(CustomerSegment.Business)
        _ <- countryCode.set("CZ")
        _ <- errors.set(Map.empty)
        _ <- busy.set(false)
      yield ()
  end Refs

  object Refs:
    def make: Refs < Sync =
      for
        seg <- Signal.initRef[CustomerSegment](CustomerSegment.Business)
        co  <- Signal.initRef("")
        reg <- Signal.initRef("")
        vat <- Signal.initRef("")
        fn  <- Signal.initRef("")
        ln  <- Signal.initRef("")
        em  <- Signal.initRef("")
        ph  <- Signal.initRef("")
        rl  <- Signal.initRef("")
        st  <- Signal.initRef("")
        ci  <- Signal.initRef("")
        pc  <- Signal.initRef("")
        cc  <- Signal.initRef("CZ")
        id  <- Signal.initRef("")
        err <- Signal.initRef[Errors](Map.empty)
        bsy <- Signal.initRef(false)
      yield Refs(seg, co, reg, vat, fn, ln, em, ph, rl, st, ci, pc, cc, id, err, bsy)
  end Refs

  def render(
      state: BackofficeState,
      refs: Refs,
      lang: Language,
      onCreated: CustomerId => Unit < Async,
      onCancel: Unit < Async,
  ): UI =
    val isCs = lang == Language.Cs
    def t(k: String): UI = span(state.i18n.t(k))

    /** Input bound to `ref`, with required marker and a reactive inline error (or hint). */
    def boundField(
        lbl: UI,
        ref: SignalRef[String],
        key: String,
        required: Boolean = false,
        placeholder: String = "",
        kind: FieldKind = FieldKind.Text,
        hint: Option[String] = None,
    ): UI =
      val control: UI = kind match
        case FieldKind.Email  => emailInput.cssClass("mp-input").value(ref).placeholder(placeholder).onInput(v => ref.set(v))
        case FieldKind.Phone  => telInput.cssClass("mp-input").value(ref).placeholder(placeholder).onInput(v => ref.set(v))
        case FieldKind.Number => numberInput.cssClass("mp-input").value(ref).placeholder(placeholder).onInput(v => ref.set(v))
        case FieldKind.Text   => input.cssClass("mp-input").value(ref).placeholder(placeholder).onInput(v => ref.set(v))
      div.cssClass("mp-field")(
        label(
          lbl,
          if required then span(" *").cssClass("mp-field-required") else UI.empty,
        ).style(smallText.fontWeight(FontWeight.w600).color(inkSecondary)),
        control,
        refs.errors.render { errs =>
          errs.get(key) match
            case Some(msg) => span(msg).cssClass("mp-field-error")
            case None      => hint.map(h => span(h).style(smallText)).getOrElse(UI.empty)
        },
      )

    val submit: Unit < Async =
      refs.busy.get.flatMap { busy =>
        if busy then Kyo.unit
        else
          for
            d    <- refs.draft
            all  <- state.customers.get
            ids   = all.map(_.id.value).toSet
            errs  = validateRegister(d, ids, emailOwners(all, except = None), isCs)
            _    <- refs.errors.set(errs)
            _ <-
              (
                if errs.nonEmpty then Kyo.unit
                else
                  for
                    _   <- refs.busy.set(true)
                    res <- state.registerCustomer(registerCmd(d, ids))
                    _   <- refs.busy.set(false)
                    _ <- (res match
                           case Some(id) => refs.reset.andThen(onCreated(id))
                           case None     => Kyo.unit
                         ): Unit < Async
                  yield ()
              ): Unit < Async
          yield ()
      }

    val segmentBlock: UI =
      refs.segment.render { seg =>
        val b2b = isB2B(seg)
        div.style(stackSm)(
          span(t("adm-cust-f-segment")).style(labelStyle),
          Controls.segmented[CustomerSegment](
            options = List(CustomerSegment.Business, CustomerSegment.Agency, CustomerSegment.Individual).map(s => s -> span(segmentLabel(s, lang))),
            current = seg,
            onSelect = s => refs.segment.set(s),
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
        )
      }

    val companyBlock: UI =
      refs.segment.render { seg =>
        if !isB2B(seg) then UI.empty
        else
          div.cssClass("mp-cust-form-group")(
            span(t("adm-cust-sec-company")).cssClass("mp-cust-form-legend"),
            div.cssClass("mp-field-row")(
              boundField(t("adm-cust-f-company"), refs.companyName, "companyName", required = true, placeholder = "Atelier Nova s.r.o."),
              boundField(t("adm-cust-f-regno"), refs.regNo, "regNo", required = true, placeholder = "28991122"),
              boundField(t("adm-cust-f-vat"), refs.vatId, "vatId", placeholder = "CZ28991122"),
            ),
          )
      }

    val roleField: UI =
      refs.segment.render { seg =>
        if isB2B(seg) then boundField(t("adm-cust-f-role"), refs.role, "contact.roleTitle", placeholder = if isCs then "Např. nákup" else "e.g. Purchasing")
        else UI.empty
      }

    // Live preview of the ID that an empty "Customer ID" field will produce.
    val idPreview: UI =
      refs.segment
        .combineLatest(refs.companyName)
        .combineLatest(refs.firstName)
        .combineLatest(refs.lastName)
        .combineLatest(refs.email)
        .combineLatest(state.customers)
        .render { case (((((seg, co), fn), ln), em), all) =>
          val d  = RegisterDraft(segment = seg, companyName = co, contact = ContactDraft(fn, ln, em, "", ""))
          val id = suggestedId(d, all.map(_.id.value).toSet)
          span(
            if isCs then s"Nepovinné — při prázdném poli se použije $id"
            else s"Optional — leave empty to use $id",
          ).style(smallText.fontFamily(FontFamily.Monospace))
        }

    div.style(stackMd)(
      // 1. Customer type
      segmentBlock,
      refs.errors.render(errs => if errs.isEmpty then UI.empty else errorSummary(errs.values, lang)),

      // 2. Company (Business / Agency only)
      companyBlock,

      // 3. Primary contact
      div.cssClass("mp-cust-form-group")(
        span(if isCs then "Hlavní kontaktní osoba" else "Primary contact").cssClass("mp-cust-form-legend"),
        div.cssClass("mp-field-row")(
          boundField(t("adm-cust-f-first"), refs.firstName, "contact.firstName", required = true, placeholder = "Tereza"),
          boundField(t("adm-cust-f-last"), refs.lastName, "contact.lastName", required = true, placeholder = "Králová"),
        ),
        div.cssClass("mp-field-row")(
          boundField(t("adm-cust-f-email"), refs.email, "contact.email", required = true, kind = FieldKind.Email, placeholder = "tereza@ateliernova.cz"),
          boundField(t("adm-cust-f-phone"), refs.phone, "contact.phone", kind = FieldKind.Phone, placeholder = "+420 601 234 567"),
          roleField,
        ),
      ),

      // 4. Billing address
      div.cssClass("mp-cust-form-group")(
        span(t("adm-cust-billing")).cssClass("mp-cust-form-legend"),
        div.cssClass("mp-field-row")(
          boundField(t("adm-cust-f-street"), refs.street, "billing.street", required = true, placeholder = "Vinohradská 12"),
          boundField(t("adm-cust-f-city"), refs.city, "billing.city", required = true, placeholder = "Praha"),
        ),
        div.cssClass("mp-field-row")(
          boundField(t("adm-cust-f-postal"), refs.postalCode, "billing.postalCode", required = true, placeholder = "120 00"),
          boundField(t("adm-cust-f-country"), refs.countryCode, "billing.countryCode", required = true, placeholder = "CZ"),
        ),
      ),

      // 5. Customer ID (auto-generated unless overridden)
      div.style(stackSm)(
        boundField(t("adm-cust-f-id"), refs.customId, "customId", placeholder = "cust-…"),
        idPreview,
      ),
      div.cssClass("mp-cust-form-actions")(
        button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(submit)(Icons(Icon.Check, 14), t("adm-cust-create-submit")),
        button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(refs.reset.andThen(onCancel))(t("adm-cat-cancel")),
      ),
    )
  end render

end CustomerCreateForm
