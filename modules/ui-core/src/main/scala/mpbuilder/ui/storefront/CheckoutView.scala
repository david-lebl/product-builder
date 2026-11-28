package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Effectfully initialized reactive state for a single Checkout session (FromCart or QuickSingleLine). */
final class CheckoutSession(
    val source: CheckoutSource,
    val fullNameRef: SignalRef[String],
    val emailRef: SignalRef[String],
    val phoneRef: SignalRef[String],
    val companyRef: SignalRef[String],
    val icoRef: SignalRef[String],
    val dicRef: SignalRef[String],
    val billStreetRef: SignalRef[String],
    val billCityRef: SignalRef[String],
    val billPostalRef: SignalRef[String],
    val billCountryRef: SignalRef[String],
    val sameShipRef: SignalRef[Boolean],
    val shipStreetRef: SignalRef[String],
    val shipCityRef: SignalRef[String],
    val shipPostalRef: SignalRef[String],
    val shipCountryRef: SignalRef[String],
    val savedShipRef: SignalRef[List[SavedShippingAddress]],
    val optionsRef: SignalRef[Option[CheckoutOptionsView]],
    val selectedDeliv: SignalRef[Option[DeliveryOptionQuote]],
    val selectedPay: SignalRef[PaymentMethod],
    val promoOverride: SignalRef[String],
    val noteRef: SignalRef[String],
    val termsAcceptedRef: SignalRef[Boolean],
    val previewRef: SignalRef[Option[CheckoutPreview]],
    val errorRef: SignalRef[Option[String]],
    val submittingRef: SignalRef[Boolean],
    val confirmationRef: SignalRef[Option[CheckoutConfirmation]],
):

  private def buildAddress(st: String, ci: String, po: String, co: String): Either[String, Address] =
    for
      s <- NonEmptyString(st.trim).toEither.left.map(_.mkString(", "))
      c <- NonEmptyString(ci.trim).toEither.left.map(_.mkString(", "))
      p <- NonEmptyString(po.trim).toEither.left.map(_.mkString(", "))
      k <- NonEmptyString(co.trim.toUpperCase).toEither.left.map(_.mkString(", "))
    yield Address(s, c, p, k)

  private def buildDeliveryChoice(opt: DeliveryOptionQuote): Either[String, DeliveryOptionChoice] < Sync =
    if opt.isPickup then
      Kyo.lift(Right(DeliveryOptionChoice.ShopPickup(opt.id)))
    else
      for
        same <- sameShipRef.get
        addrEither <-
          if same then
            for
              s <- billStreetRef.get
              c <- billCityRef.get
              p <- billPostalRef.get
              k <- billCountryRef.get
            yield buildAddress(s, c, p, k)
          else
            for
              s <- shipStreetRef.get
              c <- shipCityRef.get
              p <- shipPostalRef.get
              k <- shipCountryRef.get
            yield buildAddress(s, c, p, k)
      yield addrEither.map(addr => DeliveryOptionChoice.Courier(opt.id, addr))

  /** Overwrites contact, billing and shipping fields with the values the account provides (blank fields stay untouched). */
  def applyPrefill(prefill: CheckoutPrefill): Unit < Sync =
    def put(ref: SignalRef[String], v: Option[String]): Unit < Sync =
      v.filter(_.trim.nonEmpty).map(ref.set).getOrElse(Kyo.unit)
    val cust = prefill.customer
    val bill = prefill.billingAddress
    val ship = prefill.shippingAddresses.headOption.filterNot(a => bill.contains(a))
    val default = cust.flatMap(_.defaultShippingAddress)
    for
      _ <- savedShipRef.set(prefill.shippingAddresses.map(a => SavedShippingAddress(a, default.contains(a))))
      _ <- put(fullNameRef, cust.map(c => s"${c.contact.firstName.value} ${c.contact.lastName.value}"))
      _ <- put(emailRef, cust.map(_.contact.email.value))
      _ <- put(phoneRef, cust.flatMap(_.contact.phone).map(_.value))
      _ <- put(companyRef, cust.flatMap(_.company).map(_.legalName.value))
      _ <- put(icoRef, cust.flatMap(_.company).map(_.registrationNumber.value))
      _ <- put(dicRef, cust.flatMap(_.company).flatMap(_.vatId).map(_.value))
      _ <- put(billStreetRef, bill.map(_.street.value))
      _ <- put(billCityRef, bill.map(_.city.value))
      _ <- put(billPostalRef, bill.map(_.postalCode.value))
      _ <- put(billCountryRef, bill.map(_.countryCode.value))
      _ <- ship match
             case Some(a) => selectSavedShipping(a)
             case None    => Kyo.unit
    yield ()

  /** One-click pick of a saved delivery address: ships separately from billing and fills the shipping fields. */
  def selectSavedShipping(a: Address): Unit < Sync =
    for
      _ <- sameShipRef.set(false)
      _ <- shipStreetRef.set(a.street.value)
      _ <- shipCityRef.set(a.city.value)
      _ <- shipPostalRef.set(a.postalCode.value)
      _ <- shipCountryRef.set(a.countryCode.value)
    yield ()

  /** Switches to a one-off shipping address typed by hand (blank fields, country kept). */
  def selectNewShipping: Unit < Sync =
    for
      _ <- sameShipRef.set(false)
      _ <- shipStreetRef.set("")
      _ <- shipCityRef.set("")
      _ <- shipPostalRef.set("")
    yield ()

  /** Called after the customer signs in on the checkout page: pulls the account prefill and re-prices for the account. */
  def applyAccountPrefill(state: StorefrontState): Unit < Async =
    for
      cur <- state.currency.get
      tok <- state.authSession.get.map(_.map(_.token))
      res <- Abort.run[StorefrontError](state.client.getCheckoutOptions(source, cur, tok, None))
      _ <- res match
             case Result.Success(opts) => applyPrefill(opts.prefill)
             case _                    => Kyo.unit
      _ <- refreshPreview(state)
    yield ()

  /** Keeps the delivery / payment selections valid after the available options change (e.g. sign-in / sign-out). */
  private def reconcileSelections(opts: CheckoutOptionsView): Unit < Sync =
    for
      del <- selectedDeliv.get
      _ <- selectedDeliv.set(
             del.flatMap(d => opts.deliveryOptions.find(o => o.id == d.id && o.available))
               .orElse(opts.deliveryOptions.find(_.available))
           )
      pay <- selectedPay.get
      _ <- if opts.paymentOptions.exists(o => o.method == pay && o.available) then Kyo.unit
           else opts.paymentOptions.find(_.available).map(o => selectedPay.set(o.method)).getOrElse(Kyo.unit)
    yield ()

  def refreshPreview(state: StorefrontState): Unit < Async =
    for
      cNow    <- state.currency.get
      tNow    <- state.authSession.get.map(_.map(_.token))
      promo   <- promoOverride.get.map(s => Option(s.trim).filter(_.nonEmpty))
      optsRes <- Abort.run[StorefrontError](state.client.getCheckoutOptions(source, cNow, tNow, promo))
      _ <- optsRes.toMaybe.toOption match
             case Some(o) => optionsRef.set(Some(o)).andThen(reconcileSelections(o))
             case None    => Kyo.unit
      delOpt <- selectedDeliv.get
      choiceOpt = delOpt.map { opt =>
                    if opt.isPickup then DeliveryOptionChoice.ShopPickup(opt.id)
                    else
                      DeliveryOptionChoice.Courier(
                        opt.id,
                        Address(
                          NonEmptyString.unsafe("Preview Street 1"),
                          NonEmptyString.unsafe("Prague"),
                          NonEmptyString.unsafe("11000"),
                          NonEmptyString.unsafe("CZ"),
                        ),
                      )
                  }
      prevRes <- Abort.run[StorefrontError](state.client.previewCheckout(source, cNow, choiceOpt, tNow, promo))
      _ <- prevRes match
             case Result.Success(prev) => previewRef.set(Some(prev)).andThen(errorRef.set(None))
             case Result.Failure(err)  => errorRef.set(Some(err.message))
             case _                    => Kyo.unit
    yield ()

  /** Applies the typed promo code. Cart checkouts also persist it on the cart (or remove it when the field is
    * cleared) so the cart drawer stays in sync; quick checkouts only use it as a per-order override.
    */
  def applyPromo(state: StorefrontState): Unit < Async =
    source match
      case CheckoutSource.FromCart(_) =>
        for
          code <- promoOverride.get.map(_.trim)
          _    <- if code.isEmpty then state.removeCartPromo else state.applyCartPromo(code)
          _    <- refreshPreview(state)
        yield ()
      case _ => refreshPreview(state)

  def submitOrder(state: StorefrontState): Unit < Async =
    for
      _    <- errorRef.set(None)
      _    <- submittingRef.set(true)
      fn   <- fullNameRef.get
      em   <- emailRef.get
      ph   <- phoneRef.get
      co   <- companyRef.get
      ico  <- icoRef.get
      dic  <- dicRef.get
      bs   <- billStreetRef.get
      bc   <- billCityRef.get
      bp   <- billPostalRef.get
      bk   <- billCountryRef.get
      delO <- selectedDeliv.get
      payM <- selectedPay.get
      pr   <- promoOverride.get.map(s => Option(s.trim).filter(_.nonEmpty))
      nt   <- noteRef.get.map(s => Option(s.trim).filter(_.nonEmpty))
      terms <- termsAcceptedRef.get
      cust <- state.currentCustomer.get
      tok  <- state.authSession.get.map(_.map(_.token))

      valErrMsg <- state.i18n.now("checkout-validation-error")
      termsErrMsg <- state.i18n.now("checkout-consent-required")

      validated: Either[String, (OrderCustomerSnapshot, Address, DeliveryOptionChoice)] <-
        (for
          fullName <- NonEmptyString(fn.trim).toEither.left.map(_ => valErrMsg)
          email    <- EmailAddress(em.trim).toEither.left.map(_ => valErrMsg)
          billAddr <- buildAddress(bs, bc, bp, bk).left.map(_ => valErrMsg)
          delOpt   <- delO.toRight(valErrMsg)
          _        <- Either.cond(terms, (), termsErrMsg)
        yield (fullName, email, billAddr, delOpt)) match
          case Left(msg) => Kyo.lift(Left(msg))
          case Right((fullName, email, billAddr, delOpt)) =>
            buildDeliveryChoice(delOpt).map {
              case Left(_) => Left(valErrMsg)
              case Right(choice) =>
                val snap = OrderCustomerSnapshot(
                  customerId = cust.map(_.id),
                  segment = cust.map(_.segment).getOrElse(
                    if co.trim.nonEmpty then CustomerSegment.Business else CustomerSegment.Individual
                  ),
                  fullName = fullName,
                  email = email,
                  phone = NonEmptyString(ph.trim).toOption,
                  companyName = NonEmptyString(co.trim).toOption,
                  companyRegNo = NonEmptyString(ico.trim).toOption,
                  vatId = NonEmptyString(dic.trim).toOption,
                )
                Right((snap, billAddr, choice))
            }

      _ <- validated match
             case Left(msg) => errorRef.set(Some(msg))
             case Right((snap, billAddr, choice)) =>
               val cmd = SubmitCheckoutCmd(
                 sessionToken = tok,
                 source = source,
                 customer = snap,
                 billingAddress = billAddr,
                 deliveryChoice = choice,
                 paymentMethod = payM,
                 termsAccepted = terms,
                 promoCode = pr,
                 customerNote = nt,
               )
               Abort.run[StorefrontError](state.client.submitCheckout(cmd)).flatMap {
                 case Result.Success(conf) =>
                   for
                     _ <- confirmationRef.set(Some(conf))
                     _ <- state.onOrderPlaced(source)
                   yield ()
                 case Result.Failure(err) => errorRef.set(Some(err.message))
                 case Result.Panic(ex)    => errorRef.set(Some(Option(ex.getMessage).getOrElse("Checkout failed")))
               }
      _ <- submittingRef.set(false)
    yield ()

/** A delivery address saved on the signed-in account, offered as a one-click choice at checkout. */
final case class SavedShippingAddress(address: Address, isDefault: Boolean)

/** Which shipping choice the current form state corresponds to. */
enum ShippingChoice:
  case SameAsBilling
  case Saved(index: Int)
  case Other

object ShippingChoice:
  /** Derives the active choice from the form: a saved address is active while the shipping fields still match it. */
  def of(sameShip: Boolean, saved: List[SavedShippingAddress], street: String, city: String, postal: String, country: String): ShippingChoice =
    if sameShip then SameAsBilling
    else
      def norm(s: String) = s.trim
      val idx = saved.indexWhere { s =>
        val a = s.address
        norm(street) == a.street.value && norm(city) == a.city.value &&
        norm(postal) == a.postalCode.value && norm(country).toUpperCase == a.countryCode.value.toUpperCase
      }
      if idx >= 0 then Saved(idx) else Other

object CheckoutSession:

  def init(state: StorefrontState, source: CheckoutSource): CheckoutSession < Async =
    for
      cur            <- state.currency.get
      tok            <- state.authSession.get.map(_.map(_.token))
      initialOptsRes <- Abort.run[StorefrontError](state.client.getCheckoutOptions(source, cur, tok, None))
      initOpts        = initialOptsRes.toMaybe.toOption
      initDelivOpt    = initOpts.flatMap(_.deliveryOptions.find(_.available))
      initPayOpt      = initOpts.flatMap(_.paymentOptions.find(_.available)).map(_.method).getOrElse(PaymentMethod.BankTransferQR)

      fullNameRef     <- Signal.initRef("")
      emailRef        <- Signal.initRef("")
      phoneRef        <- Signal.initRef("")
      companyRef      <- Signal.initRef("")
      icoRef          <- Signal.initRef("")
      dicRef          <- Signal.initRef("")
      billStreetRef   <- Signal.initRef("")
      billCityRef     <- Signal.initRef("")
      billPostalRef   <- Signal.initRef("")
      billCountryRef  <- Signal.initRef("CZ")

      sameShipRef     <- Signal.initRef(true)
      shipStreetRef   <- Signal.initRef("")
      shipCityRef     <- Signal.initRef("")
      shipPostalRef   <- Signal.initRef("")
      shipCountryRef  <- Signal.initRef("CZ")
      savedShipRef    <- Signal.initRef[List[SavedShippingAddress]](Nil)

      optionsRef      <- Signal.initRef[Option[CheckoutOptionsView]](initOpts)
      selectedDeliv   <- Signal.initRef[Option[DeliveryOptionQuote]](initDelivOpt)
      selectedPay     <- Signal.initRef[PaymentMethod](initPayOpt)
      cartPromo       <- source match
                           case CheckoutSource.FromCart(_) => state.cart.get.map(_.flatMap(_.appliedPromoCode).getOrElse(""))
                           case _                          => Kyo.lift("")
      promoOverride   <- Signal.initRef(cartPromo)
      noteRef         <- Signal.initRef("")
      termsAcceptedRef <- Signal.initRef(false)
      previewRef      <- Signal.initRef[Option[CheckoutPreview]](None)
      errorRef        <- Signal.initRef[Option[String]](None)
      submittingRef   <- Signal.initRef(false)
      confirmationRef <- Signal.initRef[Option[CheckoutConfirmation]](None)

      session = new CheckoutSession(
                  source,
                  fullNameRef,
                  emailRef,
                  phoneRef,
                  companyRef,
                  icoRef,
                  dicRef,
                  billStreetRef,
                  billCityRef,
                  billPostalRef,
                  billCountryRef,
                  sameShipRef,
                  shipStreetRef,
                  shipCityRef,
                  shipPostalRef,
                  shipCountryRef,
                  savedShipRef,
                  optionsRef,
                  selectedDeliv,
                  selectedPay,
                  promoOverride,
                  noteRef,
                  termsAcceptedRef,
                  previewRef,
                  errorRef,
                  submittingRef,
                  confirmationRef,
                )
      _ <- initOpts.map(o => session.applyPrefill(o.prefill)).getOrElse(Kyo.unit)
      _ <- session.refreshPreview(state)
    yield session

/** Pure Checkout & Order Confirmation UI rendering a `CheckoutSession`. */
object CheckoutView:

  /** One-click picker over the account's saved delivery addresses, plus "same as billing" and "different address". */
  private def savedShippingPicker(state: StorefrontState, session: CheckoutSession, saved: List[SavedShippingAddress], sameShip: Boolean): UI =
    val fields =
      session.shipStreetRef
        .combineLatest(session.shipCityRef)
        .combineLatest(session.shipPostalRef)
        .combineLatest(session.shipCountryRef)
    div.style(Style.column.gap(8.px))(
      span(state.i18n.t("checkout-ship-to")).style(labelStyle),
      fields.render { case (((street, city), postal), country) =>
        val choice = ShippingChoice.of(sameShip, saved, street, city, postal, country)
        val titleStyle = Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)
        def card(active: Boolean, onPick: Unit < Async, title: UI, desc: UI) =
          button
            .cssClass(if active then "mp-option-card-active mp-ship-choice" else "mp-option-card mp-ship-choice")
            .onClick(onPick.andThen(session.refreshPreview(state)))(title, desc)
        val billingCard = card(
          choice == ShippingChoice.SameAsBilling,
          session.sameShipRef.set(true),
          span(state.i18n.t("checkout-ship-same-billing")).style(titleStyle),
          div.style(smallText)(state.i18n.t("checkout-ship-same-billing-desc")),
        )
        val savedCards = saved.zipWithIndex.map { case (s, idx) =>
          card(
            choice == ShippingChoice.Saved(idx),
            session.selectSavedShipping(s.address),
            div.style(rowBetween)(
              span(s.address.street.value).style(titleStyle),
              if s.isDefault then reactiveBadge(state.i18n.t("account-default-badge"), BadgeTone.Emerald) else UI.empty,
            ),
            div.style(smallText)(s"${s.address.postalCode.value} ${s.address.city.value}, ${s.address.countryCode.value}"),
          )
        }
        val newCard = card(
          choice == ShippingChoice.Other,
          session.selectNewShipping,
          span(state.i18n.t("checkout-ship-new")).style(titleStyle),
          div.style(smallText)(state.i18n.t("checkout-ship-new-desc")),
        )
        val cards: Seq[UI.Ast.HtmlChildVal] = (billingCard :: savedCards ::: List(newCard)).map(c => c: UI.Ast.HtmlChildVal)
        div.cssClass("mp-option-grid")(cards*)
      },
    )

  def render(state: StorefrontState, session: CheckoutSession): UI =
    val combined =
      session.confirmationRef
        .combineLatest(session.optionsRef)
        .combineLatest(session.selectedDeliv)
        .combineLatest(session.selectedPay)
        .combineLatest(session.previewRef)
        .combineLatest(session.errorRef)
        .combineLatest(session.submittingRef)
        .combineLatest(session.sameShipRef)
        .combineLatest(state.i18n.language)

    combined.render { case ((((((((confOpt, optsOpt), delOpt), payMethod), prevOpt), errOpt), submitting), sameShip), lang) =>
      confOpt match
        case Some(confirmation) =>
          renderConfirmation(state, confirmation, lang)
        case None =>
          val isQuick = session.source match
            case _: CheckoutSource.QuickSingleLine => true
            case _                                 => false

          div.style(Style.column.gap(20.px).width(100.pct))(
            div.style(rowBetween)(
              h1(state.i18n.t("checkout-title")).style(Style.margin(0.px).fontSize(26.px).fontWeight(FontWeight.w800).color(ink)),
              button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(state.openCatalog)("← ", state.i18n.t("checkout-back-catalog")),
            ),
            if isQuick then
              div.style(Style.padding(12.px, 14.px).rounded(12.px).bg(brandSoft).border(1.px, brandBorder).color(brandDark).fontSize(13.px).fontWeight(FontWeight.w600))(
                state.i18n.t("checkout-quick-banner"),
              )
            else UI.empty,
            errOpt.map(msg => alertBanner(BadgeTone.Rose, span("Error"), List(msg))).getOrElse(UI.empty),

            div.cssClass("mp-split-layout")(
              // Left form steps
              div.cssClass("mp-split-main")(
                // Step 1: Contact & Billing
                div.cssClass("mp-card")(
                  h2(state.i18n.t("checkout-step-contact")).style(sectionTitle.margin(0.px)),
                  accountPrompt(state),
                  div.cssClass("mp-field-row")(
                    textField(state.i18n.t("checkout-full-name"), session.fullNameRef, "Jan Novák"),
                    div.cssClass("mp-field")(
                      label.style(labelStyle)(state.i18n.t("checkout-email")),
                      emailInput.cssClass("mp-input").value(session.emailRef).placeholder("jan@example.com"),
                    ),
                    div.cssClass("mp-field")(
                      label.style(labelStyle)(state.i18n.t("checkout-phone")),
                      telInput.cssClass("mp-input").value(session.phoneRef).placeholder("+420 601 234 567"),
                    ),
                  ),
                  div.cssClass("mp-field-row")(
                    textField(state.i18n.t("checkout-company"), session.companyRef, "Print Shop Pro s.r.o."),
                    textField(state.i18n.t("checkout-ico"), session.icoRef, "12345678"),
                    textField(state.i18n.t("checkout-dic"), session.dicRef, "CZ12345678"),
                  ),
                  div.cssClass("mp-field-row")(
                    textField(state.i18n.t("checkout-street"), session.billStreetRef, "Vodičkova 12"),
                    textField(state.i18n.t("checkout-city"), session.billCityRef, "Praha"),
                    textField(state.i18n.t("checkout-postal"), session.billPostalRef, "11000"),
                    textField(state.i18n.t("checkout-country"), session.billCountryRef, "CZ"),
                  ),
                ),

                // Step 2: Delivery Method
                div.cssClass("mp-card")(
                  h2(state.i18n.t("checkout-step-delivery")).style(sectionTitle.margin(0.px)),
                  optsOpt match
                    case None => span("Loading delivery options...").style(mutedText)
                    case Some(opts) =>
                      div.cssClass("mp-option-grid")(
                        opts.deliveryOptions.map { dOpt =>
                          val isSel = delOpt.exists(_.id == dOpt.id)
                          button
                            .cssClass(if isSel then "mp-option-card-active" else "mp-option-card")
                            .disabled(!dOpt.available)
                            .onClick(session.selectedDeliv.set(Some(dOpt)).andThen(session.refreshPreview(state)))(
                              div.style(rowBetween)(
                                span(dOpt.name(lang)).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                                if dOpt.freeShippingApplied || dOpt.feeGross.value == BigDecimal(0) then
                                  reactiveBadge(state.i18n.t("checkout-free-badge"), BadgeTone.Emerald)
                                else
                                  badge(formatMoney(dOpt.feeGross, dOpt.currency, lang), BadgeTone.Neutral),
                              ),
                              div.style(smallText)(dOpt.description(lang)),
                              dOpt.unavailableReason
                                .map(r => div.style(Style.fontSize(12.px).color(roseInk))(r))
                                .getOrElse(UI.empty),
                            )
                        }*
                      ),
                  if delOpt.exists(!_.isPickup) then
                    div.style(Style.column.gap(10.px).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                      session.savedShipRef.render { saved =>
                        if saved.isEmpty then
                          div.style(Style.row.align(Alignment.center).gap(8.px))(
                            checkbox.checked(session.sameShipRef),
                            span(state.i18n.t("checkout-same-shipping")).style(Style.fontSize(13.px).color(inkSecondary)),
                          )
                        else savedShippingPicker(state, session, saved, sameShip)
                      },
                      if !sameShip then
                        div.style(Style.column.gap(8.px))(
                          span(state.i18n.t("checkout-shipping-title")).style(labelStyle),
                          div.cssClass("mp-field-row")(
                            textField(state.i18n.t("checkout-street"), session.shipStreetRef, "Doručovací 5"),
                            textField(state.i18n.t("checkout-city"), session.shipCityRef, "Brno"),
                            textField(state.i18n.t("checkout-postal"), session.shipPostalRef, "60200"),
                            textField(state.i18n.t("checkout-country"), session.shipCountryRef, "CZ"),
                          ),
                        )
                      else UI.empty,
                    )
                  else UI.empty,
                ),

                // Step 3: Payment Method
                div.cssClass("mp-card")(
                  h2(state.i18n.t("checkout-step-payment")).style(sectionTitle.margin(0.px)),
                  optsOpt match
                    case None => span("Loading payment options...").style(mutedText)
                    case Some(opts) =>
                      div.cssClass("mp-option-grid")(
                        opts.paymentOptions.map { pOpt =>
                          val isSel = payMethod == pOpt.method
                          val (titleKey, descKey) = pOpt.method match
                            case PaymentMethod.BankTransferQR   => ("checkout-pay-qr", "checkout-pay-qr-desc")
                            case PaymentMethod.Card             => ("checkout-pay-card", "checkout-pay-card-desc")
                            case PaymentMethod.InvoiceOnAccount => ("checkout-pay-invoice", "checkout-pay-invoice-desc")
                          button
                            .cssClass(if isSel then "mp-option-card-active" else "mp-option-card")
                            .disabled(!pOpt.available)
                            .onClick(session.selectedPay.set(pOpt.method))(
                              div.style(rowBetween)(
                                span(state.i18n.t(titleKey)).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                                if pOpt.method == PaymentMethod.InvoiceOnAccount && pOpt.available then
                                  opts.prefill.invoiceDueDays
                                    .map(d => badge(s"Net $d", BadgeTone.Emerald))
                                    .getOrElse(UI.empty)
                                else UI.empty,
                              ),
                              div.style(smallText)(state.i18n.t(descKey)),
                              pOpt.unavailableReason
                                .map(r => div.style(Style.fontSize(12.px).color(roseInk))(r))
                                .getOrElse(UI.empty),
                            )
                        }*
                      ),
                ),

                // Step 4: Promo Override & Order Note
                div.cssClass("mp-card")(
                  h2(state.i18n.t("checkout-step-note")).style(sectionTitle.margin(0.px)),
                  div.style(Style.row.gap(8.px).align(Alignment.end))(
                    div.cssClass("mp-field")(
                      label.style(labelStyle)(state.i18n.t("cart-promo-label")),
                      input.cssClass("mp-input").value(session.promoOverride).placeholder("SAVE10, FREESHIP..."),
                    ),
                    button
                      .cssClass("mp-btn mp-btn-secondary")
                      .onClick(session.applyPromo(state))(state.i18n.t("cart-promo-apply")),
                  ),
                  div.cssClass("mp-field")(
                    textarea.cssClass("mp-input").value(session.noteRef).placeholder("Delivery instructions or PO reference...").style(Style.minHeight(76.px)),
                  ),
                ),
              ),

              // Right Summary Sidebar
              div.cssClass("mp-split-aside")(
                div.cssClass("mp-card")(
                  h2(state.i18n.t("checkout-summary-title")).style(sectionTitle.margin(0.px)),
                  prevOpt match
                    case None =>
                      span(state.i18n.t("cfg-quote-calculating")).style(mutedText)
                    case Some(prev) =>
                      val pCur = prev.currency
                      div.style(Style.column.gap(12.px))(
                        div.style(Style.column.gap(8.px))(
                          prev.lines.map { ln =>
                            div.style(Style.column.gap(4.px))(
                              div.style(rowBetween.fontSize(13.px))(
                                span(s"${ln.copies.value}× ${ln.product.categoryName(lang)} (${ln.runQuantity.value} pcs)")
                                  .style(Style.color(inkSecondary)),
                                span(formatMoney(ln.lineNetTotal, pCur, lang)).style(Style.fontWeight(FontWeight.w600)),
                              ),
                              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(4.px))(
                                if ln.artworkIds.nonEmpty then
                                  reactiveBadge(
                                    state.i18n.t("cfg-artwork-attached-badge", Dict("count" -> ln.artworkIds.size.toString)),
                                    BadgeTone.Emerald,
                                  )
                                else
                                  reactiveBadge(state.i18n.t("cfg-artwork-none-badge"), BadgeTone.Neutral),
                                if ln.productionNote.isDefined then
                                  reactiveBadge(state.i18n.t("note-production-badge"), BadgeTone.Brand)
                                else UI.empty,
                              ),
                            )
                          }*
                        ),
                        div.style(Style.column.gap(6.px).padding(12.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                          div.style(rowBetween.fontSize(13.px))(
                            span(state.i18n.t("cart-subtotal-net")).style(Style.color(inkSecondary)),
                            span(formatMoney(prev.itemsNetTotal, pCur, lang)),
                          ),
                          if prev.promoDiscountNet.value > BigDecimal(0) then
                            div.style(rowBetween.fontSize(13.px))(
                              span(state.i18n.t("cart-promo-discount")).style(Style.color(emeraldInk)),
                              span(s"−${formatMoney(prev.promoDiscountNet, pCur, lang)}").style(Style.color(emeraldInk).fontWeight(FontWeight.w700)),
                            )
                          else UI.empty,
                          div.style(rowBetween.fontSize(13.px))(
                            span(state.i18n.t("checkout-shipping")).style(Style.color(inkSecondary)),
                            span(formatMoney(prev.shippingNet, pCur, lang)),
                          ),
                          div.style(rowBetween.fontSize(13.px))(
                            span(state.i18n.t("cart-tax")).style(Style.color(inkSecondary)),
                            span(formatMoney(prev.taxTotal, pCur, lang)),
                          ),
                          div.style(rowBetween.padding(8.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                            span(state.i18n.t("cart-total-gross")).style(Style.fontWeight(FontWeight.w700)),
                            span(formatMoney(prev.grandTotalGross, pCur, lang)).style(Style.fontSize(22.px).fontWeight(FontWeight.w800).color(ink)),
                          ),
                        ),
                        consentCheckbox(state, session, optsOpt.map(_.legal).getOrElse(LegalTerms.Current), lang),
                        button
                          .cssClass("mp-btn mp-btn-primary mp-btn-lg")
                          .disabled(submitting)
                          .onClick(session.submitOrder(state))(
                            if submitting then state.i18n.t("checkout-placing") else state.i18n.t("checkout-place-order"),
                          ),
                      ),
                ),
              ),
            ),
          )
    }

  private def renderConfirmation(
      state: StorefrontState,
      conf: CheckoutConfirmation,
      lang: Language,
  ): UI =
    val order = conf.order
    div.style(Style.column.gap(20.px).maxWidth(820.px).margin(0.px, Length.Auto).width(100.pct))(
      div.cssClass("mp-card").style(Style.gap(16.px))(
        div.style(Style.row.align(Alignment.center).gap(10.px))(
          reactiveBadge(state.i18n.t("confirm-badge"), BadgeTone.Emerald),
          span(order.orderNumber.value).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
        ),
        h1(state.i18n.t("confirm-title")).style(Style.margin(0.px).fontSize(26.px).fontWeight(FontWeight.w800).color(ink)),
        div.cssClass("mp-field-row")(
          div.cssClass("mp-field")(
            span(state.i18n.t("confirm-order-number")).style(smallText.fontWeight(FontWeight.w700)),
            span(order.orderNumber.value).style(Style.fontWeight(FontWeight.w700).fontSize(15.px)),
          ),
          div.cssClass("mp-field")(
            span(state.i18n.t("confirm-status")).style(smallText.fontWeight(FontWeight.w700)),
            span(orderStatusLabel(order.status, lang)).style(Style.fontWeight(FontWeight.w700).fontSize(15.px)),
          ),
          div.cssClass("mp-field")(
            span(state.i18n.t("confirm-amount")).style(smallText.fontWeight(FontWeight.w700)),
            span(formatMoney(order.totals.grandTotalGross, order.totals.currency, lang))
              .style(Style.fontWeight(FontWeight.w800).fontSize(18.px).color(brand)),
          ),
        ),
      ),

      // Payment Instructions Card
      div.cssClass("mp-card")(
        h2(state.i18n.t("confirm-payment-title")).style(sectionTitle.margin(0.px)),
        conf.paymentInstructions match
          case PaymentInstructions.BankTransferQR(iban, bic, holder, vs, amount, cur, qrPayload) =>
            div.style(Style.row.flexWrap(FlexWrap.wrap).gap(24.px).align(Alignment.start))(
              div.style(Style.column.align(Alignment.center).gap(8.px))(
                qrCodeVisual(qrPayload, 168),
                span(state.i18n.t("confirm-qr-title")).style(smallText.fontWeight(FontWeight.w700)),
              ),
              div.style(Style.column.gap(8.px).flexGrow(1.0).minWidth(240.px))(
                kvRow(state.i18n.t("confirm-iban"), iban.value),
                bic.map(b => kvRow(state.i18n.t("confirm-bic"), b.value)).getOrElse(UI.empty),
                kvRow(state.i18n.t("confirm-holder"), holder.value),
                kvRow(state.i18n.t("confirm-vs"), vs.value),
                kvRow(state.i18n.t("confirm-amount"), formatMoney(amount, cur, lang)),
                div.style(Style.column.gap(4.px).padding(8.px).rounded(8.px).bg(surfaceAlt).border(1.px, Palette.border))(
                  span(state.i18n.t("confirm-qr-payload")).style(smallText.fontWeight(FontWeight.w700)),
                  code(qrPayload).style(Style.fontSize(11.px).color(inkSecondary).fontFamily(FontFamily.Monospace)),
                ),
              ),
            )

          case PaymentInstructions.InvoiceScheduled(dueDays, invoiceRef) =>
            div.style(Style.padding(14.px).rounded(12.px).bg(emeraldSoft).border(1.px, emeraldBorder).color(emeraldInk))(
              div.style(Style.fontWeight(FontWeight.w700))(state.i18n.t("confirm-invoice-title")),
              p(state.i18n.t("confirm-invoice-desc", Dict("ref" -> invoiceRef.value, "days" -> dueDays.toString)))
                .style(Style.margin(4.px, 0.px, 0.px, 0.px).fontSize(14.px)),
            )

          case PaymentInstructions.CardPending(_, message) =>
            div.style(Style.padding(14.px).rounded(12.px).bg(brandSoft).border(1.px, brandBorder).color(brandDark))(
              div.style(Style.fontWeight(FontWeight.w700))(state.i18n.t("confirm-card-title")),
              p(message).style(Style.margin(4.px, 0.px, 0.px, 0.px).fontSize(14.px)),
            ),
      ),

      // Ordered Items & Actions
      div.cssClass("mp-card")(
        h2(state.i18n.t("confirm-items-title")).style(sectionTitle.margin(0.px)),
        div.style(Style.column.gap(8.px))(
          order.lines.toList.map { ln =>
            div.style(rowBetween.fontSize(14.px))(
              span(s"${ln.copies.value}× ${ln.product.categoryName(lang)} · ${ln.runQuantity.value} pcs (${speedLabel(ln.speed, lang)})"),
              span(formatMoney(ln.lineGrossTotal, ln.price.currency, lang)).style(Style.fontWeight(FontWeight.w700)),
            )
          }*
        ),
        div.style(Style.row.flexWrap(FlexWrap.wrap).gap(10.px).padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
          button.cssClass("mp-btn mp-btn-primary").onClick(state.openBuilder())(state.i18n.t("confirm-btn-new")),
          button.cssClass("mp-btn mp-btn-secondary").onClick(state.openAccount)(state.i18n.t("confirm-btn-orders")),
        ),
      ),
    )

  /** Sign-in call-to-action for guests; a "signed in as" note once the customer is logged in. */
  private def accountPrompt(state: StorefrontState): UI =
    state.authSession.render {
      case None =>
        div.cssClass("mp-checkout-login")
          .style(rowBetween.flexWrap(FlexWrap.wrap).gap(10.px).padding(12.px, 14.px).rounded(12.px).bg(brandSoft).border(1.px, brandBorder))(
            div.style(Style.column.gap(2.px).flexGrow(1.0).minWidth(200.px))(
              span(state.i18n.t("checkout-login-title")).style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(brandDark)),
              span(state.i18n.t("checkout-login-hint")).style(Style.fontSize(13.px).color(brandDark)),
            ),
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .onClick(state.authModalOpen.set(true))(
                Icons(Icons.Icon.LogIn, 15),
                span(state.i18n.t("checkout-login-cta")),
              ),
          )
      case Some(sess) =>
        div.style(Style.row.align(Alignment.center).gap(8.px).padding(10.px, 14.px).rounded(12.px).bg(emeraldSoft).border(1.px, emeraldBorder))(
          span(state.i18n.t("checkout-logged-in-as", Dict("name" -> sess.principal.displayName.value)))
            .style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(emeraldInk)),
        )
    }

  /** Required Terms & Conditions / privacy-notice (GDPR) consent shown right above "Place Order". */
  private def consentCheckbox(state: StorefrontState, session: CheckoutSession, legal: LegalTerms, lang: Language): UI =
    def docLink(url: String, key: String): UI =
      a.cssClass("mp-consent-link").href(UI.Href.Path(url)).target(UI.Target.Blank)(state.i18n.t(key))
    label.cssClass("mp-consent")(
      checkbox.checked(session.termsAcceptedRef),
      span(
        span(state.i18n.t("checkout-consent-prefix")),
        " ",
        docLink(legal.termsUrl(lang), "checkout-consent-terms-link"),
        " ",
        span(state.i18n.t("checkout-consent-middle")),
        " ",
        docLink(legal.privacyUrl(lang), "checkout-consent-privacy-link"),
        span(state.i18n.t("checkout-consent-suffix")),
      ),
    )

  private def textField(lbl: Signal[String], ref: SignalRef[String], ph: String): UI =
    div.cssClass("mp-field")(
      label.style(labelStyle)(lbl),
      input.cssClass("mp-input").value(ref).placeholder(ph),
    )

  private def kvRow(lbl: Signal[String], value: String): UI =
    div.style(rowBetween.fontSize(13.px))(
      span(lbl).style(Style.color(inkSecondary)),
      span(value).style(Style.fontWeight(FontWeight.w700).color(ink)),
    )
