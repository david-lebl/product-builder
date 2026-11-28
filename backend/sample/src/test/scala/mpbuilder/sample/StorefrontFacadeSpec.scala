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
import kyo.*
import kyo.test.*
import ShowcaseTestEnv.*
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
class StorefrontFacadeSpec extends Test[Any]:

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

  "StorefrontFacadeSpec" - {
    "Agency OTP login via IČO, Customer password login, and Employee password login work seamlessly" in {
      for
        facade   <- Env.get[StorefrontFacade]
        otpInbox <- Env.get[InMemoryOtpInbox]
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
      yield {
        assert(otpAuth.customer.exists(_.company.exists(_.legalName.value == "Print Shop Pro s.r.o.")))
        assert(pwAuth.customer.exists(_.tier == CustomerTier.Silver))
        assert(empAuth.session.principal.employeeIdOpt == Some(EmployeeId.unsafe("emp-1")))
        assert(empAuth.customer.isEmpty)
        assert(notCust == Left(StorefrontError.NotACustomerSession))
        assert(suspRes.isLeft)
      }
    }.provideRuntime().atTime(validNow)

    "configureAndQuote returns dual base-vs-customer pricing, EN/CS breakdown, speed options, and finish parameter limits" in {
      for
        facade   <- Env.get[StorefrontFacade]
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
      yield {
        // MegaCorp Platinum agreement gives meaningful savings over walk-in base price
        assert(quotedCs.pricing.walkInBaseQuote.isDefined)
        assert(quotedCs.pricing.effectiveQuote.netTotal < quotedCs.pricing.walkInBaseQuote.get.netTotal)
        assert(quotedCs.pricing.savingsNet.exists(_.isPositive))
        assert(quotedCs.pricing.savingsPercent.exists(_ > BigDecimal(0)))
        // Speed tier cards populated with surcharge labels
        assert(quotedCs.speedOptions.map(_.speed).toSet == ManufacturingSpeed.values.toSet)
        assert(quotedCs.speedOptions.find(_.speed == ManufacturingSpeed.Express).exists(_.surchargePercent > BigDecimal(0)))
        assert(quotedCs.speedOptions.find(_.speed == ManufacturingSpeed.Economy).exists(_.surchargePercent < BigDecimal(0)))
        // Localized breakdown differs cleanly between CS and EN
        assert(quotedCs.localizedBreakdown.nonEmpty)
        assert(quotedEn.localizedBreakdown.nonEmpty)
        assert(quotedCs.localizedBreakdown.map(_.label) != quotedEn.localizedBreakdown.map(_.label))
        // RuleEngine discovers scoring crease cap before user picks scoring!
        assert(limits.get(FinishType.Scoring) == Some(BigDecimal(4)))
      }
    }.provideRuntime().atTime(validNow)

    "Guest checkout with cart lifecycle, FREESHIP promo, QR payment instructions, and cart clearing" in {
      for
        facade   <- Env.get[StorefrontFacade]
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
      yield {
        assert(doubled.linesNetTotal == (added.linesNetTotal * BigDecimal(2)).rounded)
        assert(withFree.appliedPromo.exists(_.freeDelivery))
        // Guest options: InvoiceOnAccount unavailable, standard courier fee waived by FREESHIP
        assert(!opts.paymentOptions.find(_.method == PaymentMethod.InvoiceOnAccount).get.available)
        assert(opts.deliveryOptions.find(_.id == SampleDeliveryCatalog.courierStandardId).exists(d => d.feeNet == Money.zero && d.freeShippingApplied))
        assert(preview.shippingNet == Money.zero)
        assert(badInv.isLeft)
        assert(confirm.order.status == OrderStatus.AwaitingPayment)
        assert(confirm.order.totals.shippingNet == Money.zero)
        assert(confirm.paymentInstructions match
          case PaymentInstructions.BankTransferQR(_, _, _, _, amt, Currency.CZK, qr) =>
            amt == confirm.order.totals.grandTotalGross && qr.startsWith("SPD*1.0*ACC:")
          case _ => false)
        // Cart cleared after basket checkout
        assert(afterCart.lines.isEmpty)
      }
    }.provideRuntime().atTime(validNow)

    "Corporate InvoiceOnAccount auto-confirms, redeems single-use promo once, quick checkout preserves cart, and listMyOrders shows history" in {
      for
        facade   <- Env.get[StorefrontFacade]
        auth     <- facade.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), SampleIdentity.demoCustomerPassword)
        token     = auth.session.token
        customer  = auth.customer.get
        custSnap  = StorefrontTranslators.toOrderCustomerSnapshot(customer)

        // Add item to persistent shopping cart
        cartId    = CartId.unsafe("cart-corp-1")
        _        <- facade.createCart(cartId, Currency.CZK, Some(token))
        _        <- facade.addConfiguredProductToCart(cartId, flyersCmd(500), Currency.CZK, sessionToken = Some(token))

        // Quick single-item checkout with single-use promo code ONEUSE (does NOT clear cart-corp-1!)
        catalog  <- facade.getCatalog
        bcCat     = catalog.categories(SampleCatalog.businessCardsId)
        quickItem = QuickOrderItem(
                      StorefrontTranslators.presetToConfigureCmd(
                        ConfigurationId.unsafe("cfg-quick-1"),
                        bcCat.id,
                        bcCat.presets.find(_.id == PresetId.unsafe("preset-bc-basic")).get,
                      ),
                      Currency.CZK,
                    )
        serverQ  <- facade.configureAndQuote(quickItem.cmd, Currency.CZK, ManufacturingSpeed.Standard, Some(token), None)
        opts     <- facade.getCheckoutOptions(CheckoutSource.QuickSingleLine(quickItem), Currency.CZK, Some(token))
        confirm1 <- facade.submitCheckout(
                      SubmitCheckoutCmd(
                        sessionToken = Some(token),
                        source = CheckoutSource.QuickSingleLine(quickItem),
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
      yield {
        assert(opts.prefill.invoiceApproved)
        assert(opts.prefill.invoiceDueDays == Some(14))
        assert(confirm1.order.status == OrderStatus.Confirmed)
        // Quick-order line is priced server-side from the configuration, not taken from the client
        assert(confirm1.order.lines.head.price.copy(quoteId = serverQ.pricing.effectiveQuote.quoteId) ==
          StorefrontTranslators.toLinePriceSnapshot(serverQ.pricing.effectiveQuote))
        assert(confirm1.order.totals.promoDiscountNet.isPositive)
        assert(confirm1.paymentInstructions == PaymentInstructions.InvoiceScheduled(14, NonEmptyString.unsafe(confirm1.order.orderNumber.value)))
        assert(cartStillFull.lines.size == 1)
        assert(reusedPromo.isLeft)
        assert(confirm2.order.status == OrderStatus.Confirmed)
        assert(history.size == 2)
        assert(history.map(_.id).toSet == Set(confirm1.order.id, confirm2.order.id))
      }
    }.provideRuntime(seedSampleOrders = false).atTime(validNow)

    "draft artwork flows through cart & checkout to order line; post-checkout upload locks once accepted unless changes are requested" in {
      import mpbuilder.artwork.*
      import mpbuilder.backoffice.*

      for
        sf       <- Env.get[StorefrontFacade]
        bo       <- Env.get[BackofficeFacade]
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
      yield {
        assert(draftArt.owner == ArtworkOwner.DraftSession(draftSess))
        assert(arts1.size == 1)
        assert(arts1.head.id == draftArt.id)
        assert(arts1.head.owner == ArtworkOwner.OrderLineRef(orderId, 0))
        assert(confirm.order.lines.head.artworkIds == Map(ComponentKey.Main -> draftArt.id))
        assert(line1Art.owner == ArtworkOwner.OrderLineRef(orderId, 1))
        assert(blocked == Left(StorefrontError.OrderAlreadyAcceptedForArtwork(orderId, OrderStatus.Accepted)))
        assert(fixedArt.revisionCount == 2)
        assert(fixedArt.latestRevision.file.fileName.value == "line2-artwork-v2.pdf")
      }
    }.provideRuntime(seedSampleOrders = false).atTime(validNow)

    "line production instructions flow from cart to order, stay customer-editable until acceptance, then lock" in {
      import mpbuilder.backoffice.*

      for
        sf       <- Env.get[StorefrontFacade]
        bo       <- Env.get[BackofficeFacade]
        auth     <- sf.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), SampleIdentity.demoCustomerPassword)
        tok       = auth.session.token
        customer  = auth.customer.get
        empAuth  <- bo.loginStaff(SampleIdentity.adminEmployeePrincipal.email, SampleIdentity.demoEmployeePassword)
        empTok    = empAuth.session.token

        cartId    = CartId.unsafe("cart-production-note")
        _        <- sf.createCart(cartId, Currency.CZK)
        added    <- sf.addConfiguredProductToCart(
                      cartId = cartId,
                      cmd = flyersCmd(500),
                      currency = Currency.CZK,
                      sessionToken = Some(tok),
                      productionNote = Some("  Logo side is the front  "),
                    )
        lineId    = added.lines.head.lineId
        blanked  <- sf.updateCartLineProductionNote(cartId, lineId, Some("   "))
        edited   <- sf.updateCartLineProductionNote(cartId, lineId, Some("Match Pantone 286 C"))

        custSnap  = OrderCustomerSnapshot(
                      customerId = Some(customer.id),
                      segment = customer.segment,
                      fullName = NonEmptyString.unsafe(s"${customer.contact.firstName.value} ${customer.contact.lastName.value}"),
                      email = customer.contact.email,
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
        orderLine = confirm.order.lines.head.lineId
        updated  <- sf.updateMyOrderLineProductionNote(tok, orderId, orderLine, Some("Match Pantone 286 C, cut tight to trim"))
        staffSees <- bo.getOrder(empTok, orderId)
        _        <- bo.acceptOrder(empTok, orderId, None)
        locked   <- sf.updateMyOrderLineProductionNote(tok, orderId, orderLine, Some("Too late")).either
      yield {
        assert(added.lines.head.productionNote.contains("Logo side is the front"))
        assert(blanked.lines.head.productionNote.isEmpty)
        assert(edited.lines.head.productionNote.contains("Match Pantone 286 C"))
        assert(confirm.order.lines.head.productionNote.contains("Match Pantone 286 C"))
        assert(updated.lines.head.productionNote.contains("Match Pantone 286 C, cut tight to trim"))
        assert(updated.statusHistory.exists(_.note.exists(_.startsWith("Production instructions updated"))))
        assert(staffSees.lines.head.productionNote == updated.lines.head.productionNote)
        assert(locked == Left(StorefrontError.OrderFailure(OrderError.ProductionNoteLocked(orderId, OrderStatus.Accepted))))
      }
    }.provideRuntime(seedSampleOrders = false).atTime(validNow)

    "the full product configuration (orientation, bleed, binding, delivery format, finish sides) is frozen on cart and order lines" in {
      val preset = SampleCatalog.booklets.presets.find(_.id == PresetId.unsafe("preset-booklet-a4-saddle")).get
      val cmd = StorefrontTranslators.presetToConfigureCmd(
        ConfigurationId.unsafe("cfg-booklet-full-spec"),
        SampleCatalog.bookletsId,
        preset,
        Some(preset.defaultSpecs.copy(orientation = Some(Orientation.Landscape), bleed = Some(NonNegativeMm.unsafe(2)))),
      )
      for
        sf      <- Env.get[StorefrontFacade]
        cartId   = CartId.unsafe("cart-full-spec")
        _       <- sf.createCart(cartId, Currency.CZK)
        added   <- sf.addConfiguredProductToCart(cartId = cartId, cmd = cmd, currency = Currency.CZK)
        _       <- sf.createCart(CartId.unsafe("cart-default-bleed"), Currency.CZK)
        defaulted <- sf.addConfiguredProductToCart(cartId = CartId.unsafe("cart-default-bleed"), cmd = flyersCmd(), currency = Currency.CZK)
        confirm <- sf.submitCheckout(
                     SubmitCheckoutCmd(
                       sessionToken = None,
                       source = CheckoutSource.FromCart(cartId),
                       customer = OrderCustomerSnapshot(
                         customerId = None,
                         segment = CustomerSegment.Individual,
                         fullName = NonEmptyString.unsafe("Eva Spec"),
                         email = EmailAddress.unsafe("eva.spec@email.cz"),
                       ),
                       billingAddress = Address(
                         NonEmptyString.unsafe("Korunní 1"),
                         NonEmptyString.unsafe("Praha"),
                         NonEmptyString.unsafe("12000"),
                         NonEmptyString.unsafe("CZ"),
                       ),
                       deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                       paymentMethod = PaymentMethod.BankTransferQR,
                     )
                   )
      yield {
        val cartProduct = added.lines.head.product
        val specs       = cartProduct.specs
        assert(specs.dimensions == ProductSpecSnapshot.Dimensions.Flat2D(Dimension2D.A4))
        assert(specs.orientation.contains(ProductSpecSnapshot.Orientation.Landscape))
        assert(specs.bleed == NonNegativeMm.unsafe(2))
        assert(specs.binding.contains(ProductSpecSnapshot.Binding(ProductSpecSnapshot.BindingMethod.SaddleStitch, 16, selfCover = false)))
        assert(specs.deliveryFormat == ProductSpecSnapshot.DeliveryFormat.CutSingles)
        val coverFinish = cartProduct.component(ComponentKey.Cover).finishes
        assert(coverFinish.map(f => (f.finishId, f.finishType, f.side)) ==
          List((SampleCatalog.matteLaminationId, "Lamination", Some(FinishSnapshot.Side.Front))))
        // Artwork expectations follow the ordered binding: 16 pp minus a separate 4-page cover
        assert(cartProduct.expectedArtworkPages(ComponentKey.Body) == 12)
        assert(cartProduct.expectedArtworkPages(ComponentKey.Cover) == 2)
        // No bleed chosen: the digital-print default (3 mm) is frozen
        assert(defaulted.lines.head.product.specs.bleed == NonNegativeMm.unsafe(3))
        // The order line keeps exactly what the cart line froze
        assert(confirm.order.lines.head.product == cartProduct)
      }
    }.provideRuntime(seedSampleOrders = false).atTime(validNow)

    "Storefront currency conversion computes EUR quotes from CZK pricelist via ExchangeRateProvider adapter" in {
      for
        facade      <- Env.get[StorefrontFacade]
        rateCzkEur  <- facade.getExchangeRate(Currency.CZK, Currency.EUR)
        rateEurCzk  <- facade.getExchangeRate(Currency.EUR, Currency.CZK)
        unsupported <- facade.getExchangeRate(Currency.CZK, Currency.GBP).either
        czkQuote    <- facade.configureAndQuote(flyersCmd(500), Currency.CZK)
        eurQuote    <- facade.configureAndQuote(flyersCmd(500), Currency.EUR)
        // Custom adapter test (e.g. future external rate provider returning 1 EUR = 20 CZK)
        customSvc    = CurrencyConversionService.make(ExchangeRateProvider.hardcoded(BigDecimal("20.00")))
        customEur   <- customSvc.convertQuoteResult(czkQuote, Currency.EUR)
      yield {
        assert(rateCzkEur.rate == BigDecimal("0.04"))
        assert(rateEurCzk.rate == BigDecimal("25.00"))
        assert(unsupported == Left(StorefrontError.UnsupportedCurrencyConversion(Currency.CZK, Currency.GBP)))
        assert(czkQuote.pricing.effectiveQuote.currency == Currency.CZK)
        assert(eurQuote.pricing.effectiveQuote.currency == Currency.EUR)
        assert(eurQuote.pricing.effectiveQuote.netTotal == (czkQuote.pricing.effectiveQuote.netTotal * rateCzkEur.rate).rounded)
        assert(
          eurQuote.pricing.effectiveQuote.grossTotal ==
            (eurQuote.pricing.effectiveQuote.netTotal + eurQuote.pricing.effectiveQuote.tax.taxAmount).rounded
        )
        assert(eurQuote.localizedBreakdown.size == czkQuote.localizedBreakdown.size)
        assert(
          eurQuote.localizedBreakdown.head.total ==
            (czkQuote.localizedBreakdown.head.total * rateCzkEur.rate).rounded
        )
        assert(customEur.pricing.effectiveQuote.currency == Currency.EUR)
        assert(customEur.pricing.effectiveQuote.netTotal == (czkQuote.pricing.effectiveQuote.netTotal * BigDecimal("0.05")).rounded)
      }
    }.provideRuntime(seedSampleOrders = false).atTime(validNow)

    "Quantity & Speed Price Matrix computes volume breaks, savings badges, speed tiers, B2B discounts and EUR conversion" in {
      for
        facade    <- Env.get[StorefrontFacade]
        tiers      = PriceMatrixTiers.forAnchor(250)
        czkMatrix <- facade.quotePriceMatrix(flyersCmd(250), tiers, Currency.CZK)
        eurMatrix <- facade.quotePriceMatrix(flyersCmd(250), tiers, Currency.EUR)
        ref500    <- facade.configureAndQuote(flyersCmd(500), Currency.CZK, ManufacturingSpeed.Standard)
        auth      <- facade.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
        b2bMatrix <- facade.quotePriceMatrix(flyersCmd(250), tiers, Currency.CZK, Some(auth.session.token))
      yield {
        assert(tiers.map(_.value) == List(100, 250, 500, 1000, 2500))
        assert(PriceMatrixTiers.forAnchor(1).map(_.value) == List(1, 2, 5, 10, 25))
        assert(czkMatrix.currency == Currency.CZK)
        assert(czkMatrix.speeds == List(ManufacturingSpeed.Economy, ManufacturingSpeed.Standard, ManufacturingSpeed.Express))
        assert(czkMatrix.rows.map(_.quantity.value) == List(100, 250, 500, 1000, 2500))

        // Baseline row has no savings badge; largest tier saves meaningfully per piece vs smallest tier
        assert(czkMatrix.rows.head.savingsVsSmallestPercent.isEmpty)
        assert(czkMatrix.rows.last.savingsVsSmallestPercent.exists(_ > BigDecimal(0)))

        // Unit net price decreases monotonically from smallest to largest tier on Standard speed
        val stdUnits = czkMatrix.rows.flatMap(_.cells.find(_.speed == ManufacturingSpeed.Standard)).map(_.unitNet.value)
        assert(stdUnits.size == 5)
        assert(stdUnits.head > stdUnits.last)

        // Economy <= Standard <= Express for the 500-piece row, and Standard cell matches configureAndQuote
        val row500 = czkMatrix.rows.find(_.quantity.value == 500).get
        val eco500 = row500.cells.find(_.speed == ManufacturingSpeed.Economy).get
        val std500 = row500.cells.find(_.speed == ManufacturingSpeed.Standard).get
        val exp500 = row500.cells.find(_.speed == ManufacturingSpeed.Express).get
        assert(eco500.netTotal.value <= std500.netTotal.value)
        assert(std500.netTotal.value <= exp500.netTotal.value)
        assert(std500.netTotal == ref500.pricing.effectiveQuote.netTotal)
        assert(std500.grossTotal == ref500.pricing.effectiveQuote.grossTotal)

        // EUR conversion matches CZK -> EUR rate
        assert(eurMatrix.currency == Currency.EUR)
        val eurStd500 = eurMatrix.rows.find(_.quantity.value == 500).get.cells.find(_.speed == ManufacturingSpeed.Standard).get
        assert(eurStd500.netTotal.value < std500.netTotal.value)

        // B2B signed-in customer receives lower prices in the matrix
        val b2bStd500 = b2bMatrix.rows.find(_.quantity.value == 500).get.cells.find(_.speed == ManufacturingSpeed.Standard).get
        assert(b2bStd500.netTotal.value < std500.netTotal.value)
      }
    }.provideRuntime(seedSampleOrders = false).atTime(validNow)
  }
