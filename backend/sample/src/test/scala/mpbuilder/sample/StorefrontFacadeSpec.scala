package mpbuilder.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.identity.sample.{InMemoryOtpInbox, SampleIdentity}
import mpbuilder.ordering.*
import mpbuilder.ordering.sample.SampleDeliveryCatalog
import mpbuilder.shared.*
import mpbuilder.storefront.*
import zio.*
import zio.test.*
import java.time.Instant

/** End-to-end Product Builder frontend scenarios exercised through `StorefrontFacade`:
  *   1. Agency OTP login by IČO / DIČ + customer password login + employee password login
  *   2. Dual pricing (walk-in base price vs B2B customer savings) + localized EN/CS breakdown
  *   3. Configurator option & parameter cap discovery (`finishParameterLimits`, speed tier availability)
  *   4. Full shopping cart lifecycle (`createCart`, `add`, `updateCopies`, `applyPromo`, `removePromo`)
  *   5. Checkout options & submission:
  *      - Guest BankTransferQR + `FREESHIP` promo -> waived courier fee + SPD 1.0 QR payload + cart cleared
  *      - Corporate Print Shop Pro `InvoiceOnAccount` -> auto-confirmed + `InvoiceScheduled` + order history
  *      - Quick single-item checkout -> leaves existing shopping cart untouched
  *      - Promo redemption counter enforcement (`ONEUSE` single-use code fails on 2nd checkout!)
  */
object StorefrontFacadeSpec extends ZIOSpecDefault:

  private val validNow = Instant.parse("2025-06-15T12:00:00Z")

  private val pragueAddress = Address(
    street = NonEmptyString.unsafe("Vinohradská 10"),
    city = NonEmptyString.unsafe("Praha"),
    postalCode = NonEmptyString.unsafe("12000"),
    countryCode = NonEmptyString.unsafe("CZ"),
  )

  private val guestCustomer = OrderCustomerSnapshot(
    customerId = None,
    segment = CustomerSegment.Individual,
    fullName = NonEmptyString.unsafe("Anna Nová"),
    email = EmailAddress.unsafe("anna@example.com"),
    phone = Some(NonEmptyString.unsafe("+420 600 111 222")),
  )

  private def flyersCmd(qty: Int = 500): ConfigureProductCmd =
    ConfigureProductCmd(
      configurationId = ConfigurationId.unsafe(s"cfg-flyers-$qty"),
      categoryId = SampleCatalog.flyersId,
      components = NonEmptyChunk.single(
        ComponentSelection(
          key = ComponentKey.Main,
          materialId = SampleCatalog.coatedGlossy170gsmId,
          printingMethodId = Some(SampleCatalog.digitalId),
          inkConfiguration = Some(InkConfiguration.cmyk4_4),
        )
      ),
      specs = ProductSpecs(
        dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
        quantity = Quantity.unsafe(qty),
        orientation = Some(Orientation.Portrait),
      ),
    )

  def spec = suite("StorefrontFacadeSpec")(
    test("Agency OTP login via IČO, Customer password login, and Employee password login work seamlessly") {
      for
        _        <- TestClock.setTime(validNow)
        facade   <- ZIO.service[StorefrontFacade]
        otpInbox <- ZIO.service[InMemoryOtpInbox]
        // 1) Agency OTP login via Czech Company Registration Number (IČO = 12345678 -> Print Shop Pro)
        ch       <- facade.requestCustomerOtp(LoginIdentifier.CompanyRegNo(NonEmptyString.unsafe("12345678")))
        codeOpt  <- otpInbox.codeFor(ch.challengeId)
        otpAuth  <- facade.verifyOtp(ch.challengeId, codeOpt.get)
        // 2) Customer password login
        pwAuth   <- facade.loginWithPassword(EmailAddress.unsafe("marie@graphicdesign.cz"), SampleIdentity.demoCustomerPassword)
        // 3) Employee password login (no CustomerView attached)
        empAuth  <- facade.loginWithPassword(SampleIdentity.adminEmployeePrincipal.email, SampleIdentity.demoEmployeePassword)
        notCust  <- facade.currentCustomer(empAuth.session.token).either
        // 4) Suspended customer blocked from OTP login
        suspRes  <- facade.requestCustomerOtp(LoginIdentifier.Email(EmailAddress.unsafe("karel@suspendedco.cz"))).either
      yield assertTrue(
        otpAuth.customer.exists(_.company.exists(_.legalName.value == "Print Shop Pro s.r.o.")),
        pwAuth.customer.exists(_.tier == CustomerTier.Silver),
        empAuth.session.principal.employeeIdOpt == Some(EmployeeId.unsafe("emp-1")),
        empAuth.customer.isEmpty,
        notCust == Left(StorefrontError.NotACustomerSession),
        suspRes.isLeft,
      )
    }.provide(SampleShowcaseRuntime.layer()),

    test("configureAndQuote returns dual base-vs-customer pricing, EN/CS breakdown, speed options, and finish parameter limits") {
      for
        _        <- TestClock.setTime(validNow)
        facade   <- ZIO.service[StorefrontFacade]
        auth     <- facade.loginWithPassword(EmailAddress.unsafe("petr@megacorp.cz"), SampleIdentity.demoCustomerPassword)
        // Configure flyers as Platinum customer (MegaCorp) in CZK
        quotedCs <- facade.configureAndQuote(
                      cmd = flyersCmd(500),
                      currency = Currency.CZK,
                      speed = ManufacturingSpeed.Standard,
                      sessionToken = Some(auth.session.token),
                      lang = Language.Cs,
                    )
        quotedEn <- facade.configureAndQuote(
                      cmd = flyersCmd(500),
                      currency = Currency.CZK,
                      speed = ManufacturingSpeed.Standard,
                      sessionToken = Some(auth.session.token),
                      lang = Language.En,
                    )
        // Check finish parameter cap discovery (Scoring max creases on heavy 350gsm board on Digital)
        limits   <- facade.finishParameterLimits(
                      categoryId = SampleCatalog.brochuresId,
                      componentKey = ComponentKey.Main,
                      materialId = SampleCatalog.coatedGlossy350gsmId,
                      printingMethodId = Some(SampleCatalog.digitalId),
                    )
      yield assertTrue(
        // MegaCorp Platinum agreement gives meaningful savings over walk-in base price
        quotedCs.pricing.walkInBaseQuote.isDefined,
        quotedCs.pricing.effectiveQuote.netTotal < quotedCs.pricing.walkInBaseQuote.get.netTotal,
        quotedCs.pricing.savingsNet.exists(_.isPositive),
        quotedCs.pricing.savingsPercent.exists(_ > BigDecimal(0)),
        // Speed tier cards populated with surcharge labels
        quotedCs.speedOptions.map(_.speed).toSet == ManufacturingSpeed.values.toSet,
        quotedCs.speedOptions.find(_.speed == ManufacturingSpeed.Express).exists(_.surchargePercent > BigDecimal(0)),
        quotedCs.speedOptions.find(_.speed == ManufacturingSpeed.Economy).exists(_.surchargePercent < BigDecimal(0)),
        // Localized breakdown differs cleanly between CS and EN
        quotedCs.localizedBreakdown.nonEmpty,
        quotedEn.localizedBreakdown.nonEmpty,
        quotedCs.localizedBreakdown.map(_.label) != quotedEn.localizedBreakdown.map(_.label),
        // RuleEngine discovers scoring crease cap before user picks scoring!
        limits.get(FinishType.Scoring) == Some(BigDecimal(4)),
      )
    }.provide(SampleShowcaseRuntime.layer()),

    test("Guest checkout with cart lifecycle, FREESHIP promo, QR payment instructions, and cart clearing") {
      for
        _        <- TestClock.setTime(validNow)
        facade   <- ZIO.service[StorefrontFacade]
        cartId    = CartId.unsafe("cart-guest-1")
        _        <- facade.createCart(cartId, Currency.CZK)
        added    <- facade.addConfiguredProductToCart(cartId, flyersCmd(250), Currency.CZK, copies = Quantity.unsafe(1))
        lineId    = added.lines.head.lineId
        doubled  <- facade.updateCartLineCopies(cartId, lineId, Quantity.unsafe(2))
        // Apply FREESHIP promo to cart
        withFree <- facade.applyCartPromo(cartId, "FREESHIP")
        opts     <- facade.getCheckoutOptions(CheckoutSource.FromCart(cartId), Currency.CZK)
        preview  <- facade.previewCheckout(
                      source = CheckoutSource.FromCart(cartId),
                      currency = Currency.CZK,
                      deliveryChoice = Some(DeliveryOptionChoice.Courier(SampleDeliveryCatalog.courierStandardId, pragueAddress)),
                    )
        // Guest cannot choose InvoiceOnAccount
        badInv   <- facade
                      .submitCheckout(
                        SubmitCheckoutCmd(
                          sessionToken = None,
                          source = CheckoutSource.FromCart(cartId),
                          customer = guestCustomer,
                          billingAddress = pragueAddress,
                          deliveryChoice = DeliveryOptionChoice.Courier(SampleDeliveryCatalog.courierStandardId, pragueAddress),
                          paymentMethod = PaymentMethod.InvoiceOnAccount,
                        )
                      )
                      .either
        // Guest succeeds with BankTransferQR
        confirm  <- facade.submitCheckout(
                      SubmitCheckoutCmd(
                        sessionToken = None,
                        source = CheckoutSource.FromCart(cartId),
                        customer = guestCustomer,
                        billingAddress = pragueAddress,
                        deliveryChoice = DeliveryOptionChoice.Courier(SampleDeliveryCatalog.courierStandardId, pragueAddress),
                        paymentMethod = PaymentMethod.BankTransferQR,
                        customerNote = Some("Leave at reception"),
                      )
                    )
        afterCart <- facade.getCart(cartId)
      yield assertTrue(
        doubled.linesNetTotal == (added.linesNetTotal * BigDecimal(2)).rounded,
        withFree.appliedPromo.exists(_.freeDelivery),
        // Guest options: InvoiceOnAccount unavailable, standard courier fee waived by FREESHIP
        !opts.paymentOptions.find(_.method == PaymentMethod.InvoiceOnAccount).get.available,
        opts.deliveryOptions.find(_.id == SampleDeliveryCatalog.courierStandardId).exists(d => d.feeNet == Money.zero && d.freeShippingApplied),
        preview.shippingNet == Money.zero,
        badInv.isLeft,
        confirm.order.status == OrderStatus.AwaitingPayment,
        confirm.order.totals.shippingNet == Money.zero,
        confirm.paymentInstructions match
          case PaymentInstructions.BankTransferQR(_, _, _, _, amt, Currency.CZK, qr) =>
            amt == confirm.order.totals.grandTotalGross && qr.startsWith("SPD*1.0*ACC:")
          case _ => false,
        // Cart cleared after basket checkout
        afterCart.lines.isEmpty,
      )
    }.provide(SampleShowcaseRuntime.layer()),

    test("Corporate InvoiceOnAccount auto-confirms, redeems single-use promo once, quick checkout preserves cart, and listMyOrders shows history") {
      for
        _        <- TestClock.setTime(validNow)
        facade   <- ZIO.service[StorefrontFacade]
        auth     <- facade.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), SampleIdentity.demoCustomerPassword)
        token     = auth.session.token
        customer  = auth.customer.get
        custSnap  = StorefrontTranslators.toOrderCustomerSnapshot(customer)

        // Add item to persistent shopping cart
        cartId    = CartId.unsafe("cart-corp-1")
        _        <- facade.createCart(cartId, Currency.CZK, Some(token))
        _        <- facade.addConfiguredProductToCart(cartId, flyersCmd(500), Currency.CZK, sessionToken = Some(token))

        // Quick single-item checkout with single-use promo code ONEUSE (does NOT clear cart-corp-1!)
        quickQ   <- facade.pricePreset(
                      SampleCatalog.businessCardsId,
                      PresetId.unsafe("preset-bc-basic"),
                      Currency.CZK,
                      sessionToken = Some(token),
                    )
        quickLine = StorefrontTranslators.toCartLine(
                      OrderLineId.unsafe("quick-1"),
                      quickQ.configuration.product,
                      ManufacturingSpeed.Standard,
                      quickQ.pricing.effectiveQuote,
                    )
        opts     <- facade.getCheckoutOptions(CheckoutSource.QuickSingleLine(quickLine), Currency.CZK, Some(token))
        confirm1 <- facade.submitCheckout(
                      SubmitCheckoutCmd(
                        sessionToken = Some(token),
                        source = CheckoutSource.QuickSingleLine(quickLine),
                        customer = custSnap,
                        billingAddress = customer.billingAddress,
                        deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                        paymentMethod = PaymentMethod.InvoiceOnAccount,
                        promoCode = Some("ONEUSE"),
                      )
                    )
        // Cart is STILL intact after quick checkout!
        cartStillFull <- facade.getCart(cartId)
        // Reusing ONEUSE on cart checkout now fails because redemption incremented currentUses to maxUses (1)!
        reusedPromo   <- facade
                           .submitCheckout(
                             SubmitCheckoutCmd(
                               sessionToken = Some(token),
                               source = CheckoutSource.FromCart(cartId),
                               customer = custSnap,
                               billingAddress = customer.billingAddress,
                               deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                               paymentMethod = PaymentMethod.InvoiceOnAccount,
                               promoCode = Some("ONEUSE"),
                             )
                           )
                           .either
        // Checkout cart without ONEUSE succeeds
        confirm2 <- facade.submitCheckout(
                      SubmitCheckoutCmd(
                        sessionToken = Some(token),
                        source = CheckoutSource.FromCart(cartId),
                        customer = custSnap,
                        billingAddress = customer.billingAddress,
                        deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                        paymentMethod = PaymentMethod.InvoiceOnAccount,
                      )
                    )
        history  <- facade.listMyOrders(token)
      yield assertTrue(
        opts.prefill.invoiceApproved,
        opts.prefill.invoiceDueDays == Some(14),
        confirm1.order.status == OrderStatus.Confirmed,
        confirm1.order.totals.promoDiscountNet.isPositive,
        confirm1.paymentInstructions == PaymentInstructions.InvoiceScheduled(14, NonEmptyString.unsafe(confirm1.order.orderNumber.value)),
        cartStillFull.lines.size == 1,
        reusedPromo.isLeft,
        confirm2.order.status == OrderStatus.Confirmed,
        history.size == 2,
        history.map(_.id).toSet == Set(confirm1.order.id, confirm2.order.id),
      )
    }.provide(SampleShowcaseRuntime.layer(seedSampleOrders = false)),

    test("draft artwork flows through cart & checkout to order line; post-checkout upload locks once accepted unless changes are requested") {
      import mpbuilder.artwork.*
      import mpbuilder.backoffice.*

      for
        _        <- TestClock.setTime(validNow)
        sf       <- ZIO.service[StorefrontFacade]
        bo       <- ZIO.service[BackofficeFacade]
        auth     <- sf.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), SampleIdentity.demoCustomerPassword)
        tok       = auth.session.token
        customer  = auth.customer.get
        empAuth  <- bo.loginStaff(SampleIdentity.adminEmployeePrincipal.email, SampleIdentity.demoEmployeePassword)
        empTok    = empAuth.session.token

        // 1. Upload draft artwork during product configuration
        draftSess = NonEmptyString.unsafe("draft-sess-test-1")
        draftArt <- sf.uploadDraftArtwork(
                      sessionId = draftSess,
                      componentKey = ComponentKey.Main,
                      expectedTrimSize = Dimension2D.A5,
                      expectedPageCount = Some(2),
                      payload = ArtworkUploadPayload(
                        fileName = NonEmptyString.unsafe("flyers-draft-press.pdf"),
                        mimeType = NonEmptyString.unsafe("application/pdf"),
                        sizeBytes = 1_850_000L,
                        downloadUrl = Some(NonEmptyString.unsafe("https://cdn.printcraft.demo/flyers-draft-press.pdf")),
                        previewUrl = Some(NonEmptyString.unsafe("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=")),
                        resolutionDpi = 300,
                        bleedMm = 3,
                        colorSpace = ColorSpace.CMYK,
                        pageCount = Some(2),
                      ),
                    )

        // 2. Add two items to cart: line 0 with draft artwork, line 1 without artwork yet
        cartId    = CartId.unsafe("cart-artwork-e2e")
        _        <- sf.createCart(cartId, Currency.CZK)
        _        <- sf.addConfiguredProductToCart(
                      cartId = cartId,
                      cmd = flyersCmd(500),
                      currency = Currency.CZK,
                      speed = ManufacturingSpeed.Standard,
                      copies = Quantity.unsafe(1),
                      sessionToken = Some(tok),
                      artworkIds = Map(ComponentKey.Main -> draftArt.id),
                    )
        _        <- sf.addConfiguredProductToCart(
                      cartId = cartId,
                      cmd = flyersCmd(1000),
                      currency = Currency.CZK,
                      speed = ManufacturingSpeed.Standard,
                      copies = Quantity.unsafe(1),
                      sessionToken = Some(tok),
                      artworkIds = Map.empty,
                    )

        // 3. Submit checkout -> draft artwork owner reassigned to OrderLineRef(order.id, 0)
        custSnap = OrderCustomerSnapshot(
                     customerId = Some(customer.id),
                     segment = customer.segment,
                     fullName = NonEmptyString.unsafe(s"${customer.contact.firstName.value} ${customer.contact.lastName.value}"),
                     email = customer.contact.email,
                     phone = customer.contact.phone,
                     companyName = customer.company.map(_.legalName),
                   )
        confirm  <- sf.submitCheckout(
                      SubmitCheckoutCmd(
                        sessionToken = Some(tok),
                        source = CheckoutSource.FromCart(cartId),
                        customer = custSnap,
                        billingAddress = customer.billingAddress,
                        deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                        paymentMethod = PaymentMethod.InvoiceOnAccount,
                      )
                    )
        orderId   = confirm.order.id
        arts1    <- sf.listOrderArtworks(tok, orderId)

        // 4. Before acceptance (order is Confirmed), customer uploads missing artwork to line 1 -> succeeds
        line1Art <- sf.uploadOrderLineArtwork(
                      sessionToken = tok,
                      orderId = orderId,
                      lineIndex = 1,
                      componentKey = ComponentKey.Main,
                      payload = ArtworkUploadPayload(
                        fileName = NonEmptyString.unsafe("line2-artwork.pdf"),
                        mimeType = NonEmptyString.unsafe("application/pdf"),
                        sizeBytes = 900_000L,
                        resolutionDpi = 300,
                        bleedMm = 3,
                        colorSpace = ColorSpace.CMYK,
                        pageCount = Some(1),
                      ),
                    )

        // 5. Staff accepts the order -> subsequent customer new-upload is blocked with OrderAlreadyAcceptedForArtwork
        _        <- bo.acceptOrder(empTok, orderId, Some(NonEmptyString.unsafe("Accepted for prepress")))
        blocked  <- sf
                      .uploadOrderLineArtwork(
                        sessionToken = tok,
                        orderId = orderId,
                        lineIndex = 1,
                        componentKey = ComponentKey.Main,
                        payload = ArtworkUploadPayload(
                          fileName = NonEmptyString.unsafe("late-upload.pdf"),
                          mimeType = NonEmptyString.unsafe("application/pdf"),
                          sizeBytes = 500_000L,
                        ),
                      )
                      .either

        // 6. Staff requests changes on line1Art -> customer reuploadOrderArtworkPayload succeeds even after acceptance!
        _        <- bo.requestArtworkChanges(empTok, line1Art.id, NonEmptyString.unsafe("Please outline fonts"))
        fixedArt <- sf.reuploadOrderArtworkPayload(
                      sessionToken = tok,
                      orderId = orderId,
                      artworkId = line1Art.id,
                      payload = ArtworkUploadPayload(
                        fileName = NonEmptyString.unsafe("line2-artwork-v2.pdf"),
                        mimeType = NonEmptyString.unsafe("application/pdf"),
                        sizeBytes = 1_100_000L,
                        downloadUrl = Some(NonEmptyString.unsafe("https://cdn.printcraft.demo/line2-artwork-v2.pdf")),
                        resolutionDpi = 300,
                        bleedMm = 3,
                        colorSpace = ColorSpace.CMYK,
                        pageCount = Some(1),
                      ),
                    )
      yield assertTrue(
        draftArt.owner == ArtworkOwner.DraftSession(draftSess),
        arts1.size == 1,
        arts1.head.id == draftArt.id,
        arts1.head.owner == ArtworkOwner.OrderLineRef(orderId, 0),
        confirm.order.lines.head.artworkIds == Map(ComponentKey.Main -> draftArt.id),
        line1Art.owner == ArtworkOwner.OrderLineRef(orderId, 1),
        blocked == Left(StorefrontError.OrderAlreadyAcceptedForArtwork(orderId, OrderStatus.Accepted)),
        fixedArt.revisionCount == 2,
        fixedArt.latestRevision.file.fileName.value == "line2-artwork-v2.pdf",
      )
    }.provide(SampleShowcaseRuntime.layer(seedSampleOrders = false)),
  )
