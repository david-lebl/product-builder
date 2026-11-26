package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
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
    val optionsRef: SignalRef[Option[CheckoutOptionsView]],
    val selectedDeliv: SignalRef[Option[DeliveryOptionQuote]],
    val selectedPay: SignalRef[PaymentMethod],
    val promoOverride: SignalRef[String],
    val noteRef: SignalRef[String],
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

  def refreshPreview(state: StorefrontState): Unit < Async =
    for
      cNow   <- state.currency.get
      tNow   <- state.authSession.get.map(_.map(_.token))
      delOpt <- selectedDeliv.get
      promo  <- promoOverride.get.map(s => Option(s.trim).filter(_.nonEmpty))
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
      optsRes <- Abort.run[StorefrontError](state.client.getCheckoutOptions(source, cNow, tNow, promo))
      _       <- optsRes.toMaybe.toOption.map(o => optionsRef.set(Some(o))).getOrElse(Kyo.unit)
      prevRes <- Abort.run[StorefrontError](state.client.previewCheckout(source, cNow, choiceOpt, tNow, promo))
      _ <- prevRes match
             case Result.Success(prev) => previewRef.set(Some(prev)).andThen(errorRef.set(None))
             case Result.Failure(err)  => errorRef.set(Some(err.message))
             case _                    => Kyo.unit
    yield ()

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
      cust <- state.currentCustomer.get
      tok  <- state.authSession.get.map(_.map(_.token))

      valErrMsg <- state.i18n.now("checkout-validation-error")

      validated: Either[String, (OrderCustomerSnapshot, Address, DeliveryOptionChoice)] <-
        (for
          fullName <- NonEmptyString(fn.trim).toEither.left.map(_ => valErrMsg)
          email    <- EmailAddress(em.trim).toEither.left.map(_ => valErrMsg)
          billAddr <- buildAddress(bs, bc, bp, bk).left.map(_ => valErrMsg)
          delOpt   <- delO.toRight(valErrMsg)
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

object CheckoutSession:

  def init(state: StorefrontState, source: CheckoutSource): CheckoutSession < Async =
    for
      cur            <- state.currency.get
      tok            <- state.authSession.get.map(_.map(_.token))
      initialOptsRes <- Abort.run[StorefrontError](state.client.getCheckoutOptions(source, cur, tok, None))
      initOpts        = initialOptsRes.toMaybe.toOption
      prefill         = initOpts.map(_.prefill)
      initCust        = prefill.flatMap(_.customer)
      initBill        = prefill.flatMap(_.billingAddress)
      initDelivOpt    = initOpts.flatMap(_.deliveryOptions.find(_.available))
      initPayOpt      = initOpts.flatMap(_.paymentOptions.find(_.available)).map(_.method).getOrElse(PaymentMethod.BankTransferQR)

      fullNameRef     <- Signal.initRef(initCust.map(c => s"${c.contact.firstName.value} ${c.contact.lastName.value}").getOrElse(""))
      emailRef        <- Signal.initRef(initCust.map(_.contact.email.value).getOrElse(""))
      phoneRef        <- Signal.initRef(initCust.flatMap(_.contact.phone).map(_.value).getOrElse(""))
      companyRef      <- Signal.initRef(initCust.flatMap(_.company).map(_.legalName.value).getOrElse(""))
      icoRef          <- Signal.initRef(initCust.flatMap(_.company).map(_.registrationNumber.value).getOrElse(""))
      dicRef          <- Signal.initRef(initCust.flatMap(_.company).flatMap(_.vatId).map(_.value).getOrElse(""))
      billStreetRef   <- Signal.initRef(initBill.map(_.street.value).getOrElse(""))
      billCityRef     <- Signal.initRef(initBill.map(_.city.value).getOrElse(""))
      billPostalRef   <- Signal.initRef(initBill.map(_.postalCode.value).getOrElse(""))
      billCountryRef  <- Signal.initRef(initBill.map(_.countryCode.value).getOrElse("CZ"))

      sameShipRef     <- Signal.initRef(true)
      shipStreetRef   <- Signal.initRef("")
      shipCityRef     <- Signal.initRef("")
      shipPostalRef   <- Signal.initRef("")
      shipCountryRef  <- Signal.initRef("CZ")

      optionsRef      <- Signal.initRef[Option[CheckoutOptionsView]](initOpts)
      selectedDeliv   <- Signal.initRef[Option[DeliveryOptionQuote]](initDelivOpt)
      selectedPay     <- Signal.initRef[PaymentMethod](initPayOpt)
      promoOverride   <- Signal.initRef("")
      noteRef         <- Signal.initRef("")
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
                  optionsRef,
                  selectedDeliv,
                  selectedPay,
                  promoOverride,
                  noteRef,
                  previewRef,
                  errorRef,
                  submittingRef,
                  confirmationRef,
                )
      _ <- session.refreshPreview(state)
    yield session

/** Pure Checkout & Order Confirmation UI rendering a `CheckoutSession`. */
object CheckoutView:

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
                      div.style(Style.row.align(Alignment.center).gap(8.px))(
                        checkbox.checked(session.sameShipRef),
                        span(state.i18n.t("checkout-same-shipping")).style(Style.fontSize(13.px).color(inkSecondary)),
                      ),
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
                  if isQuick then
                    div.style(Style.row.gap(8.px).align(Alignment.end))(
                      div.cssClass("mp-field")(
                        label.style(labelStyle)(state.i18n.t("cart-promo-label")),
                        input.cssClass("mp-input").value(session.promoOverride).placeholder("SAVE10, FREESHIP..."),
                      ),
                      button
                        .cssClass("mp-btn mp-btn-secondary")
                        .onClick(session.refreshPreview(state))(state.i18n.t("cart-promo-apply")),
                    )
                  else UI.empty,
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
