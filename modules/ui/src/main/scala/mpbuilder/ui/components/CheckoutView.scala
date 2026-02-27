package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.{ProductBuilderViewModel, AppRouter, AppRoute, BuilderState}
import mpbuilder.domain.model.*
import mpbuilder.domain.model.CheckoutStep.*
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.domain.service.BasketService

object CheckoutView:

  def apply(): Element =
    val lang  = ProductBuilderViewModel.currentLanguage
    val state = ProductBuilderViewModel.state

    div(
      cls := "checkout-page",

      // Shown only if basket is empty (edge case — user navigated here directly)
      child <-- state.combineWith(lang).map { case (s, l) =>
        s.checkoutInfo match
          case None =>
            div(
              cls := "card",
              p(l match
                case Language.En => "No active checkout. Please add items to your basket first."
                case Language.Cs => "Žádná aktivní objednávka. Nejprve přidejte položky do košíku."
              ),
              button(
                cls := "btn-secondary",
                l match
                  case Language.En => "← Back to Shop"
                  case Language.Cs => "← Zpět do obchodu",
                onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductBuilder) },
              ),
            )

          case Some(info) =>
            div(
              // Step indicator
              checkoutStepBar(info.step, l),

              // Step content
              info.step match
                case Authentication  => stepAuthentication(info, l)
                case ContactDetails  => stepContactDetails(info, l)
                case Delivery        => stepDelivery(info, l)
                case Payment         => stepPayment(info, l)
                case Summary         => stepSummary(info, s, l)
            )
      },
    )

  // ── Step indicator ─────────────────────────────────────────────────────────

  private def checkoutStepBar(current: CheckoutStep, l: Language): Element =
    val steps: List[(CheckoutStep, String, String)] = List(
      (Authentication, "1", if l == Language.Cs then "Přihlášení"   else "Sign In"),
      (ContactDetails, "2", if l == Language.Cs then "Kontakt"      else "Contact"),
      (Delivery,       "3", if l == Language.Cs then "Doprava"      else "Delivery"),
      (Payment,        "4", if l == Language.Cs then "Platba"       else "Payment"),
      (Summary,        "5", if l == Language.Cs then "Shrnutí"      else "Summary"),
    )
    val ordinal = current.ordinal
    div(
      cls := "checkout-steps",
      steps.map { case (step, num, label) =>
        val stepOrdinal = step.ordinal
        val cls_ =
          if stepOrdinal < ordinal then "checkout-step checkout-step--done"
          else if stepOrdinal == ordinal then "checkout-step checkout-step--active"
          else "checkout-step"
        div(
          cls := cls_,
          span(cls := "checkout-step-num", if stepOrdinal < ordinal then "✓" else num),
          span(cls := "checkout-step-label", label),
        )
      },
    )

  // ── Step 1: Authentication ──────────────────────────────────────────────────

  private def stepAuthentication(info: CheckoutInfo, l: Language): Element =
    val loginEmailVar    = Var(info.loginEmail)
    val loginPasswordVar = Var(info.loginPassword)

    div(
      cls := "checkout-card card",
      h2(cls := "checkout-step-title",
        if l == Language.Cs then "Přihlášení nebo pokračovat jako host"
        else "Sign In or Continue as Guest"
      ),

      // Guest option
      div(
        cls := "checkout-auth-options",
        div(
          cls := "checkout-auth-option",
          h3(if l == Language.Cs then "Pokračovat jako host" else "Continue as Guest"),
          p(cls := "checkout-auth-desc",
            if l == Language.Cs then "Nemusíte se registrovat — vyplňte kontaktní údaje a dokončete objednávku. Platba musí být provedena předem."
            else "No account needed — fill in your details and complete your order. Payment is required upfront."
          ),
          button(
            cls := "checkout-btn",
            if l == Language.Cs then "Pokračovat jako host →" else "Continue as Guest →",
            onClick --> { _ =>
              ProductBuilderViewModel.updateCheckoutInfo(info.copy(
                customerType = CustomerType.Guest,
              ))
              ProductBuilderViewModel.checkoutNextStep()
            },
          ),
        ),

        div(cls := "checkout-auth-divider", if l == Language.Cs then "nebo" else "or"),

        // Registered user login
        div(
          cls := "checkout-auth-option",
          h3(if l == Language.Cs then "Přihlásit se" else "Sign In"),
          p(cls := "checkout-auth-desc",
            if l == Language.Cs then "Přihlaste se ke svému účtu pro rychlejší objednávku a přístup k historii objednávek."
            else "Sign in to your account for a faster checkout and access to your order history."
          ),
          div(
            cls := "form-group",
            label(if l == Language.Cs then "E-mail" else "Email"),
            input(
              typ := "email",
              placeholder := (if l == Language.Cs then "vas@email.cz" else "your@email.com"),
              value <-- loginEmailVar,
              onInput.mapToValue --> loginEmailVar.writer,
            ),
          ),
          div(
            cls := "form-group",
            label(if l == Language.Cs then "Heslo" else "Password"),
            input(
              typ := "password",
              placeholder := "••••••••",
              value <-- loginPasswordVar,
              onInput.mapToValue --> loginPasswordVar.writer,
            ),
          ),
          button(
            cls := "checkout-btn",
            if l == Language.Cs then "Přihlásit se →" else "Sign In →",
            onClick --> { _ =>
              ProductBuilderViewModel.updateCheckoutInfo(info.copy(
                customerType = CustomerType.Registered,
                loginEmail = loginEmailVar.now(),
                loginPassword = loginPasswordVar.now(),
              ))
              ProductBuilderViewModel.checkoutNextStep()
            },
          ),
        ),
      ),
    )

  // ── Step 2: Contact Details ─────────────────────────────────────────────────

  private def stepContactDetails(info: CheckoutInfo, l: Language): Element =
    val ci = info.contactInfo
    val sa = info.shippingAddress
    val fnVar       = Var(ci.firstName)
    val lnVar       = Var(ci.lastName)
    val emailVar    = Var(ci.email)
    val phoneVar    = Var(ci.phone)
    val companyVar  = Var(ci.company.getOrElse(""))
    val streetVar   = Var(sa.street)
    val cityVar     = Var(sa.city)
    val zipVar      = Var(sa.zip)
    val countryVar  = Var(sa.country)
    val codeVar     = Var(info.discountCode)
    val noteVar     = Var(info.note)

    def isValid: Boolean =
      fnVar.now().trim.nonEmpty && lnVar.now().trim.nonEmpty &&
      emailVar.now().trim.nonEmpty && phoneVar.now().trim.nonEmpty &&
      streetVar.now().trim.nonEmpty && cityVar.now().trim.nonEmpty &&
      zipVar.now().trim.nonEmpty && countryVar.now().trim.nonEmpty

    div(
      cls := "checkout-card card",
      h2(cls := "checkout-step-title",
        if l == Language.Cs then "Kontaktní a doručovací údaje" else "Contact & Delivery Details"
      ),

      // Personal info
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Osobní údaje" else "Personal Information"
      ),
      div(
        cls := "checkout-form-row",
        div(
          cls := "form-group",
          label(if l == Language.Cs then "Jméno *" else "First Name *"),
          input(typ := "text", placeholder := (if l == Language.Cs then "Jan" else "John"),
            value <-- fnVar, onInput.mapToValue --> fnVar.writer),
        ),
        div(
          cls := "form-group",
          label(if l == Language.Cs then "Příjmení *" else "Last Name *"),
          input(typ := "text", placeholder := (if l == Language.Cs then "Novák" else "Smith"),
            value <-- lnVar, onInput.mapToValue --> lnVar.writer),
        ),
      ),
      div(
        cls := "checkout-form-row",
        div(
          cls := "form-group",
          label(if l == Language.Cs then "E-mail *" else "Email *"),
          input(typ := "email", placeholder := (if l == Language.Cs then "jan@email.cz" else "john@email.com"),
            value <-- emailVar, onInput.mapToValue --> emailVar.writer),
        ),
        div(
          cls := "form-group",
          label(if l == Language.Cs then "Telefon *" else "Phone *"),
          input(typ := "tel", placeholder := "+420 123 456 789",
            value <-- phoneVar, onInput.mapToValue --> phoneVar.writer),
        ),
      ),
      div(
        cls := "form-group",
        label(if l == Language.Cs then "Firma (nepovinné)" else "Company (optional)"),
        input(typ := "text", placeholder := (if l == Language.Cs then "Vaše firma s.r.o." else "Your Company Ltd."),
          value <-- companyVar, onInput.mapToValue --> companyVar.writer),
      ),

      // Shipping address
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Doručovací adresa" else "Shipping Address"
      ),
      div(
        cls := "form-group",
        label(if l == Language.Cs then "Ulice a číslo popisné *" else "Street & Number *"),
        input(typ := "text", placeholder := (if l == Language.Cs then "Ulice 123" else "123 Main Street"),
          value <-- streetVar, onInput.mapToValue --> streetVar.writer),
      ),
      div(
        cls := "checkout-form-row",
        div(
          cls := "form-group",
          label(if l == Language.Cs then "Město *" else "City *"),
          input(typ := "text", placeholder := (if l == Language.Cs then "Praha" else "Prague"),
            value <-- cityVar, onInput.mapToValue --> cityVar.writer),
        ),
        div(
          cls := "form-group",
          label(if l == Language.Cs then "PSČ *" else "ZIP / Postal Code *"),
          input(typ := "text", placeholder := "110 00",
            value <-- zipVar, onInput.mapToValue --> zipVar.writer),
        ),
      ),
      div(
        cls := "form-group",
        label(if l == Language.Cs then "Země *" else "Country *"),
        input(typ := "text", placeholder := (if l == Language.Cs then "Česká republika" else "Czech Republic"),
          value <-- countryVar, onInput.mapToValue --> countryVar.writer),
      ),

      // Discount code
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Slevový kód (nepovinné)" else "Discount Code (optional)"
      ),
      div(
        cls := "checkout-discount-row",
        input(
          typ := "text",
          cls := "checkout-discount-input",
          placeholder := (if l == Language.Cs then "Zadejte slevový kód" else "Enter discount code"),
          value <-- codeVar,
          onInput.mapToValue --> codeVar.writer,
        ),
        button(
          cls := "btn-secondary checkout-discount-apply",
          if l == Language.Cs then "Použít" else "Apply",
          // No real integration yet — UI only
        ),
      ),

      // Note
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Poznámka k objednávce (nepovinné)" else "Order Note (optional)"
      ),
      div(
        cls := "form-group",
        textArea(
          cls := "checkout-note",
          placeholder := (if l == Language.Cs then "Zvláštní požadavky nebo poznámky k objednávce…"
                          else "Special requirements or notes for your order…"),
          value <-- noteVar,
          onInput.mapToValue --> noteVar.writer,
        ),
      ),

      checkoutNavButtons(
        l = l,
        onBack = Some(() => ProductBuilderViewModel.checkoutPrevStep()),
        onNext = () => {
          if isValid then
            ProductBuilderViewModel.updateCheckoutInfo(info.copy(
              contactInfo = ContactInfo(
                firstName = fnVar.now().trim,
                lastName  = lnVar.now().trim,
                email     = emailVar.now().trim,
                phone     = phoneVar.now().trim,
                company   = Some(companyVar.now().trim).filter(_.nonEmpty),
              ),
              shippingAddress = ShippingAddress(
                street  = streetVar.now().trim,
                city    = cityVar.now().trim,
                zip     = zipVar.now().trim,
                country = countryVar.now().trim,
              ),
              discountCode = codeVar.now().trim,
              note         = noteVar.now().trim,
            ))
            ProductBuilderViewModel.checkoutNextStep()
        },
      ),
    )

  // ── Step 3: Delivery ────────────────────────────────────────────────────────

  private def stepDelivery(info: CheckoutInfo, l: Language): Element =
    val selectedVar: Var[Option[DeliveryOption]] = Var(info.deliveryOption)

    div(
      cls := "checkout-card card",
      h2(cls := "checkout-step-title",
        if l == Language.Cs then "Způsob dopravy" else "Delivery Method"
      ),

      // Pickup options
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Osobní odběr" else "Pickup at Shop"
      ),
      div(
        cls := "checkout-options",
        ProductBuilderViewModel.shopLocations.map { loc =>
          val opt = DeliveryOption.PickupAtShop(loc.id)
          div(
            cls <-- selectedVar.signal.map(sel =>
              if sel.contains(opt) then "checkout-option checkout-option--selected"
              else "checkout-option"
            ),
            onClick --> { _ => selectedVar.set(Some(opt)) },
            div(cls := "checkout-option-title", "🏪 " + loc.name(l)),
            div(cls := "checkout-option-desc",  loc.address(l)),
            div(cls := "checkout-option-price",
              if l == Language.Cs then "Zdarma" else "Free"
            ),
          )
        },
      ),

      // Courier options
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Doručení kurýrem" else "Courier Delivery"
      ),
      div(
        cls := "checkout-options",
        ProductBuilderViewModel.courierServices.map { svc =>
          val opt: DeliveryOption = svc.id match
            case "courier-express"  => DeliveryOption.CourierExpress
            case "courier-economy"  => DeliveryOption.CourierEconomy
            case _                  => DeliveryOption.CourierStandard
          div(
            cls <-- selectedVar.signal.map(sel =>
              if sel.contains(opt) then "checkout-option checkout-option--selected"
              else "checkout-option"
            ),
            onClick --> { _ => selectedVar.set(Some(opt)) },
            div(cls := "checkout-option-title", "🚚 " + svc.name(l)),
            div(cls := "checkout-option-desc",  svc.estimatedDays(l)),
            div(cls := "checkout-option-price", formatMoney(svc.surcharge, svc.currency)),
          )
        },
      ),

      checkoutNavButtons(
        l = l,
        onBack = Some(() => ProductBuilderViewModel.checkoutPrevStep()),
        onNext = () => {
          selectedVar.now() match
            case Some(opt) =>
              ProductBuilderViewModel.updateCheckoutInfo(info.copy(deliveryOption = Some(opt)))
              ProductBuilderViewModel.checkoutNextStep()
            case None => // must select a delivery option
        },
        nextEnabled = selectedVar.signal.map(_.isDefined),
      ),
    )

  // ── Step 4: Payment ─────────────────────────────────────────────────────────

  private def stepPayment(info: CheckoutInfo, l: Language): Element =
    val selectedVar: Var[Option[PaymentMethod]] = Var(info.paymentMethod)

    // Corporate registered users may use invoice; guests must pay upfront
    val canUseInvoice = info.customerType == CustomerType.RegisteredCorporate

    div(
      cls := "checkout-card card",
      h2(cls := "checkout-step-title",
        if l == Language.Cs then "Způsob platby" else "Payment Method"
      ),

      if !canUseInvoice then
        p(cls := "checkout-payment-note",
          if l == Language.Cs then "ℹ️ Nepřihlášení zákazníci a zákazníci bez schválení musí uhradit objednávku předem."
          else "ℹ️ Guest and non-approved customers must pay before the order is processed."
        )
      else emptyNode,

      div(
        cls := "checkout-options",

        // Bank transfer with QR code
        div(
          cls <-- selectedVar.signal.map(sel =>
            if sel.contains(PaymentMethod.BankTransferQR) then "checkout-option checkout-option--selected"
            else "checkout-option"
          ),
          onClick --> { _ => selectedVar.set(Some(PaymentMethod.BankTransferQR)) },
          div(cls := "checkout-option-title", "🏦 " + (if l == Language.Cs then "Bankovní převod (QR kód)" else "Bank Transfer (QR Code)")),
          div(cls := "checkout-option-desc",
            if l == Language.Cs then "Po dokončení objednávky obdržíte platební instrukce a QR kód."
            else "After placing your order you will receive payment instructions and a QR code."
          ),
        ),

        // Card payment (future)
        div(
          cls <-- selectedVar.signal.map(sel =>
            if sel.contains(PaymentMethod.Card) then "checkout-option checkout-option--selected checkout-option--future"
            else "checkout-option checkout-option--future"
          ),
          div(cls := "checkout-option-title", "💳 " + (if l == Language.Cs then "Platba kartou (připravujeme)" else "Card Payment (coming soon)")),
          div(cls := "checkout-option-desc",
            if l == Language.Cs then "Platba online kartou bude brzy k dispozici."
            else "Online card payment will be available soon."
          ),
        ),

        // Invoice on account — corporate only
        if canUseInvoice then
          div(
            cls <-- selectedVar.signal.map(sel =>
              if sel.contains(PaymentMethod.InvoiceOnAccount) then "checkout-option checkout-option--selected"
              else "checkout-option"
            ),
            onClick --> { _ => selectedVar.set(Some(PaymentMethod.InvoiceOnAccount)) },
            div(cls := "checkout-option-title", "📄 " + (if l == Language.Cs then "Faktura (firemní zákazník)" else "Invoice on Account (corporate)")),
            div(cls := "checkout-option-desc",
              if l == Language.Cs then "Jako schválený firemní zákazník budete fakturováni po dokončení objednávky."
              else "As an approved corporate customer you will be invoiced after order completion."
            ),
          )
        else emptyNode,
      ),

      checkoutNavButtons(
        l = l,
        onBack = Some(() => ProductBuilderViewModel.checkoutPrevStep()),
        onNext = () => {
          selectedVar.now() match
            case Some(method) =>
              ProductBuilderViewModel.updateCheckoutInfo(info.copy(paymentMethod = Some(method)))
              ProductBuilderViewModel.checkoutNextStep()
            case None => // must select a payment method
        },
        nextEnabled = selectedVar.signal.map(_.isDefined),
      ),
    )

  // ── Step 5: Summary ─────────────────────────────────────────────────────────

  private def stepSummary(info: CheckoutInfo, s: BuilderState, l: Language): Element =
    val basketCalc = BasketService.calculateTotal(s.basket)

    div(
      cls := "checkout-card card",
      h2(cls := "checkout-step-title",
        if l == Language.Cs then "Shrnutí objednávky" else "Order Summary"
      ),

      // Basket items
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Položky" else "Items"
      ),
      div(
        cls := "checkout-summary-items",
        s.basket.items.map { item =>
          div(
            cls := "checkout-summary-item",
            span(cls := "checkout-summary-item-name",
              s"${item.quantity}× ${item.configuration.category.name(l)}"
            ),
            span(cls := "checkout-summary-item-price",
              formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)
            ),
          )
        },
      ),

      // Delivery info
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Doprava" else "Delivery"
      ),
      div(
        cls := "checkout-summary-row",
        span(deliveryLabel(info.deliveryOption, l)),
        span(deliveryCost(info.deliveryOption, l)),
      ),

      // Payment info
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Platba" else "Payment"
      ),
      div(
        cls := "checkout-summary-row",
        span(paymentLabel(info.paymentMethod, l)),
      ),

      // Contact
      h3(cls := "checkout-section-title",
        if l == Language.Cs then "Kontakt" else "Contact"
      ),
      div(
        cls := "checkout-summary-row",
        span(s"${info.contactInfo.firstName} ${info.contactInfo.lastName}"),
      ),
      div(
        cls := "checkout-summary-row",
        span(info.contactInfo.email),
      ),
      div(
        cls := "checkout-summary-row",
        span(info.contactInfo.phone),
      ),
      info.contactInfo.company.map { co =>
        div(cls := "checkout-summary-row", span(co))
      }.getOrElse(emptyNode),
      div(
        cls := "checkout-summary-row",
        span(s"${info.shippingAddress.street}, ${info.shippingAddress.zip} ${info.shippingAddress.city}, ${info.shippingAddress.country}"),
      ),

      // Discount code
      if info.discountCode.nonEmpty then
        div(
          cls := "checkout-summary-row",
          span("🏷️ " + (if l == Language.Cs then s"Slevový kód: ${info.discountCode}" else s"Discount code: ${info.discountCode}")),
        )
      else emptyNode,

      // Note
      if info.note.nonEmpty then
        div(
          cls := "checkout-summary-row checkout-summary-note",
          span("📝 " + info.note),
        )
      else emptyNode,

      // Total
      div(
        cls := "checkout-total",
        span(if l == Language.Cs then "Celkem:" else "Total:"),
        strong(formatMoney(basketCalc.total, basketCalc.currency)),
      ),

      // Payment note for bank transfer
      if info.paymentMethod.contains(PaymentMethod.BankTransferQR) then
        div(
          cls := "checkout-qr-note info-box",
          if l == Language.Cs then
            "📋 Po potvrzení objednávky vám zašleme platební instrukce včetně QR kódu na váš e-mail."
          else
            "📋 After confirming your order, we will send payment instructions including a QR code to your email."
        )
      else emptyNode,

      // Navigation
      div(
        cls := "checkout-nav",
        button(
          cls := "btn-secondary",
          if l == Language.Cs then "← Zpět" else "← Back",
          onClick --> { _ => ProductBuilderViewModel.checkoutPrevStep() },
        ),
        button(
          cls := "checkout-btn checkout-submit-btn",
          if l == Language.Cs then "✓ Potvrdit objednávku" else "✓ Place Order",
          onClick --> { _ =>
            // UI-only: just show confirmation and clear basket
            ProductBuilderViewModel.clearBasket()
            ProductBuilderViewModel.cancelCheckout()
            AppRouter.navigateTo(AppRoute.ProductBuilder)
          },
        ),
      ),
    )

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private def checkoutNavButtons(
    l: Language,
    onBack: Option[() => Unit],
    onNext: () => Unit,
    nextEnabled: Signal[Boolean] = Val(true),
  ): Element =
    div(
      cls := "checkout-nav",
      onBack match
        case Some(back) =>
          button(
            cls := "btn-secondary",
            if l == Language.Cs then "← Zpět" else "← Back",
            onClick --> { _ => back() },
          )
        case None =>
          button(
            cls := "btn-secondary",
            if l == Language.Cs then "← Zpět do košíku" else "← Back to Basket",
            onClick --> { _ =>
              AppRouter.navigateTo(AppRoute.ProductBuilder)
              AppRouter.basketOpen.set(true)
            },
          ),
      button(
        cls := "checkout-btn",
        disabled <-- nextEnabled.map(!_),
        if l == Language.Cs then "Pokračovat →" else "Continue →",
        onClick --> { _ => onNext() },
      ),
    )

  private def deliveryLabel(opt: Option[DeliveryOption], l: Language): String =
    opt match
      case None => if l == Language.Cs then "—" else "—"
      case Some(DeliveryOption.PickupAtShop(locId)) =>
        val loc = ProductBuilderViewModel.shopLocations.find(_.id == locId)
        val name = loc.map(_.name(l)).getOrElse(locId)
        if l == Language.Cs then s"Osobní odběr — $name" else s"Pickup — $name"
      case Some(DeliveryOption.CourierStandard) =>
        if l == Language.Cs then "Standardní doručení" else "Standard Delivery"
      case Some(DeliveryOption.CourierExpress) =>
        if l == Language.Cs then "Expresní doručení" else "Express Delivery"
      case Some(DeliveryOption.CourierEconomy) =>
        if l == Language.Cs then "Ekonomické doručení" else "Economy Delivery"

  private def deliveryCost(opt: Option[DeliveryOption], l: Language): String =
    opt match
      case Some(DeliveryOption.PickupAtShop(_)) => if l == Language.Cs then "Zdarma" else "Free"
      case Some(opt) =>
        val svcId = opt match
          case DeliveryOption.CourierExpress  => "courier-express"
          case DeliveryOption.CourierEconomy  => "courier-economy"
          case _                              => "courier-standard"
        ProductBuilderViewModel.courierServices.find(_.id == svcId)
          .map(s => formatMoney(s.surcharge, s.currency))
          .getOrElse("—")
      case None => "—"

  private def paymentLabel(opt: Option[PaymentMethod], l: Language): String =
    opt match
      case None                                    => "—"
      case Some(PaymentMethod.BankTransferQR)      =>
        if l == Language.Cs then "Bankovní převod (QR kód)" else "Bank Transfer (QR Code)"
      case Some(PaymentMethod.Card)                =>
        if l == Language.Cs then "Platba kartou" else "Card Payment"
      case Some(PaymentMethod.InvoiceOnAccount)    =>
        if l == Language.Cs then "Faktura" else "Invoice on Account"

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
