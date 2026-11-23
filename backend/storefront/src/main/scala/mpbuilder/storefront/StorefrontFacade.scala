package mpbuilder.storefront

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.manufacturing.ManufacturingService
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import zio.*
import java.time.ZoneOffset

/** Storefront BFF / Application Orchestration Facade for the Product Builder frontend.
  *
  * Coordinates ONLY the public inbound ports of the bounded contexts:
  *   - `CatalogService`
  *   - `PricingService`
  *   - `CustomerService`
  *   - `IdentityService`
  *   - `ArtworkService`
  *   - `CartService`, `CheckoutOptionsService`, `OrderService`
  *   - `ManufacturingService` (speed availability policy only)
  */
trait StorefrontFacade:

  // ── Catalog & Product Builder ──────────────────────────────────────────────

  def getCatalog: UIO[ProductCatalog]
  def listShowcaseListings: UIO[List[ShowcaseListing]]

  def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId] = Set.empty,
  ): IO[StorefrontError, List[Finish]]

  def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): IO[StorefrontError, Map[FinishType, BigDecimal]]

  def configureAndQuote(
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      sessionToken: Option[SessionToken] = None,
      promoCode: Option[String] = None,
      lang: Language = Language.En,
  ): IO[StorefrontError, ProductBuilderQuoteResult]

  def pricePreset(
      categoryId: CategoryId,
      presetId: PresetId,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      specsOverride: Option[ProductSpecs] = None,
      sessionToken: Option[SessionToken] = None,
      lang: Language = Language.En,
  ): IO[StorefrontError, ProductBuilderQuoteResult]

  // ── Authentication & Session ───────────────────────────────────────────────

  def loginWithPassword(email: EmailAddress, rawPassword: NonEmptyString): IO[StorefrontError, AuthResult]
  def requestCustomerOtp(identifier: LoginIdentifier): IO[StorefrontError, OtpChallengeView]
  def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): IO[StorefrontError, AuthResult]
  def currentCustomer(sessionToken: SessionToken): IO[StorefrontError, CustomerView]
  def logout(sessionToken: SessionToken): UIO[Unit]

  // ── Shopping Cart ──────────────────────────────────────────────────────────

  def createCart(
      cartId: CartId,
      currency: Currency,
      sessionToken: Option[SessionToken] = None,
  ): IO[StorefrontError, CartView]

  def addConfiguredProductToCart(
      cartId: CartId,
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      copies: Quantity = Quantity.one,
      sessionToken: Option[SessionToken] = None,
      artworkIds: Map[ComponentKey, ArtworkId] = Map.empty,
  ): IO[StorefrontError, CartView]

  def updateCartLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): IO[StorefrontError, CartView]
  def removeCartLine(cartId: CartId, lineId: OrderLineId): IO[StorefrontError, CartView]
  def clearCart(cartId: CartId): IO[StorefrontError, CartView]

  def applyCartPromo(
      cartId: CartId,
      promoCode: String,
      sessionToken: Option[SessionToken] = None,
  ): IO[StorefrontError, CartView]

  def removeCartPromo(cartId: CartId): IO[StorefrontError, CartView]
  def getCart(cartId: CartId): IO[StorefrontError, CartView]

  // ── Checkout & Order History ───────────────────────────────────────────────

  def getCheckoutOptions(
      source: CheckoutSource,
      currency: Currency,
      sessionToken: Option[SessionToken] = None,
      promoCode: Option[String] = None,
  ): IO[StorefrontError, CheckoutOptionsView]

  def previewCheckout(
      source: CheckoutSource,
      currency: Currency,
      deliveryChoice: Option[DeliveryOptionChoice],
      sessionToken: Option[SessionToken] = None,
      promoCode: Option[String] = None,
  ): IO[StorefrontError, CheckoutPreview]

  def submitCheckout(cmd: SubmitCheckoutCmd): IO[StorefrontError, CheckoutConfirmation]

  def listMyOrders(sessionToken: SessionToken): IO[StorefrontError, List[OrderView]]

  // ── Customer Self-Service (Addresses, Payment Re-view, Artwork Upload/Re-upload) ──

  def updateMyAddresses(
      sessionToken: SessionToken,
      billingAddress: Address,
      shippingAddresses: List[Address],
      defaultShippingIndex: Option[Int],
  ): IO[StorefrontError, CustomerView]

  def getOrderPaymentInstructions(
      sessionToken: SessionToken,
      orderId: OrderId,
  ): IO[StorefrontError, PaymentInstructions]

  def uploadDraftArtwork(
      sessionId: NonEmptyString,
      componentKey: ComponentKey,
      expectedTrimSize: Dimension2D,
      expectedPageCount: Option[Int] = None,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView]

  def deleteDraftArtwork(artworkId: ArtworkId): IO[StorefrontError, Unit]

  def listOrderArtworks(
      sessionToken: SessionToken,
      orderId: OrderId,
  ): IO[StorefrontError, List[ArtworkSubmissionView]]

  def uploadOrderLineArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView]

  def reuploadOrderArtworkPayload(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView]

  def reuploadOrderArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      fileName: NonEmptyString,
      resolutionDpi: Int = 300,
      bleedMm: Int = 3,
      colorSpace: ColorSpace = ColorSpace.CMYK,
  ): IO[StorefrontError, ArtworkSubmissionView]

final class StorefrontFacadeLive(
    catalogService: CatalogService,
    pricingService: PricingService,
    customerService: CustomerService,
    identityService: IdentityService,
    artworkService: ArtworkService,
    cartService: CartService,
    checkoutOptionsService: CheckoutOptionsService,
    orderService: OrderService,
    manufacturingService: ManufacturingService,
    idSequence: IdSequence,
    blobStorage: BlobStorage,
) extends StorefrontFacade:

  override def getCatalog: UIO[ProductCatalog] =
    catalogService.getCatalog

  override def listShowcaseListings: UIO[List[ShowcaseListing]] =
    catalogService.getCatalog.map(_.showcases.values.toList.sortBy(_.sortOrder))

  override def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId],
  ): IO[StorefrontError, List[Finish]] =
    catalogService
      .compatibleFinishes(categoryId, componentKey, materialId, printingMethodId, alreadySelectedFinishIds)
      .mapError(StorefrontError.CatalogFailure.apply)

  override def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): IO[StorefrontError, Map[FinishType, BigDecimal]] =
    catalogService
      .finishParameterLimits(categoryId, componentKey, materialId, printingMethodId)
      .mapError(StorefrontError.CatalogFailure.apply)

  override def configureAndQuote(
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed,
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
      lang: Language,
  ): IO[StorefrontError, ProductBuilderQuoteResult] =
    for
      customerOpt  <- resolveOptionalCustomer(sessionToken)
      catalog      <- catalogService.getCatalog
      cfgRes       <- catalogService.configure(cmd).mapError(StorefrontError.CatalogFailure.apply)
      subject       = StorefrontTranslators.toPricingSubject(cfgRes.product, speed)
      effectiveQId <- nextQuoteId
      effectiveQ   <- pricingService
                        .quote(
                          PriceQuoteCmd(
                            quoteId = effectiveQId,
                            subject = subject,
                            currency = currency,
                            customerId = customerOpt.map(_.id),
                            customerTier = customerOpt.map(_.tier),
                            customerSegment = customerOpt.map(_.segment),
                            promoCode = promoCode,
                          )
                        )
                        .mapError(StorefrontError.PricingFailure.apply)
      walkInQOpt   <- customerOpt match
                        case None => ZIO.none
                        case Some(_) =>
                          for
                            baseQId <- nextQuoteId
                            baseQ   <- pricingService
                                         .quote(
                                           PriceQuoteCmd(
                                             quoteId = baseQId,
                                             subject = subject,
                                             currency = currency,
                                             promoCode = promoCode,
                                           )
                                         )
                                         .mapError(StorefrontError.PricingFailure.apply)
                          yield Some(baseQ)
      dualPricing   = buildDualPriceQuote(effectiveQ, walkInQOpt)
      speedOpts    <- evaluateSpeedOptions(cfgRes.product, currency)
      localized     = ChargeFormatter.formatQuote(effectiveQ, catalog, lang)
    yield ProductBuilderQuoteResult(cfgRes, dualPricing, speedOpts, localized)

  override def pricePreset(
      categoryId: CategoryId,
      presetId: PresetId,
      currency: Currency,
      speed: ManufacturingSpeed,
      specsOverride: Option[ProductSpecs],
      sessionToken: Option[SessionToken],
      lang: Language,
  ): IO[StorefrontError, ProductBuilderQuoteResult] =
    for
      cat    <- catalogService.getCategory(categoryId).mapError(StorefrontError.CatalogFailure.apply)
      preset <- ZIO.fromOption(cat.presets.find(_.id == presetId)).orElseFail(StorefrontError.PresetNotFound(categoryId, presetId))
      seq    <- idSequence.next
      cmd     = StorefrontTranslators.presetToConfigureCmd(
                  configId = ConfigurationId.unsafe(s"cfg-${presetId.value}-$seq"),
                  categoryId = categoryId,
                  preset = preset,
                  specsOverride = specsOverride,
                )
      res    <- configureAndQuote(cmd, currency, speed, sessionToken, promoCode = None, lang = lang)
    yield res

  // ── Auth ───────────────────────────────────────────────────────────────────

  override def loginWithPassword(email: EmailAddress, rawPassword: NonEmptyString): IO[StorefrontError, AuthResult] =
    for
      session  <- identityService.loginWithPassword(email, rawPassword).mapError(StorefrontError.IdentityFailure.apply)
      customer <- loadCustomerForPrincipal(session.principal)
    yield AuthResult(session, customer)

  override def requestCustomerOtp(identifier: LoginIdentifier): IO[StorefrontError, OtpChallengeView] =
    identityService.requestCustomerOtp(identifier).mapError(StorefrontError.IdentityFailure.apply)

  override def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): IO[StorefrontError, AuthResult] =
    for
      session  <- identityService.verifyOtp(challengeId, code).mapError(StorefrontError.IdentityFailure.apply)
      customer <- loadCustomerForPrincipal(session.principal)
    yield AuthResult(session, customer)

  override def currentCustomer(sessionToken: SessionToken): IO[StorefrontError, CustomerView] =
    for
      session <- identityService.validateSession(sessionToken).mapError(StorefrontError.IdentityFailure.apply)
      cid     <- ZIO.fromOption(session.principal.customerIdOpt).orElseFail(StorefrontError.NotACustomerSession)
      cust    <- customerService.getById(cid).mapError(StorefrontError.CustomerFailure.apply)
    yield cust

  override def logout(sessionToken: SessionToken): UIO[Unit] =
    identityService.logout(sessionToken)

  // ── Cart ───────────────────────────────────────────────────────────────────

  override def createCart(
      cartId: CartId,
      currency: Currency,
      sessionToken: Option[SessionToken],
  ): IO[StorefrontError, CartView] =
    for
      customerOpt <- resolveOptionalCustomer(sessionToken)
      cart        <- cartService.createCart(cartId, currency, customerOpt.map(_.id)).mapError(StorefrontError.OrderFailure.apply)
    yield cart

  override def addConfiguredProductToCart(
      cartId: CartId,
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed,
      copies: Quantity,
      sessionToken: Option[SessionToken],
      artworkIds: Map[ComponentKey, ArtworkId],
  ): IO[StorefrontError, CartView] =
    for
      quoted <- configureAndQuote(cmd, currency, speed, sessionToken, promoCode = None)
      lineId <- nextLineId
      line    = StorefrontTranslators.toCartLine(
                  lineId = lineId,
                  product = quoted.configuration.product,
                  speed = speed,
                  quote = quoted.pricing.effectiveQuote,
                  copies = copies,
                  artworkIds = artworkIds,
                )
      cart   <- cartService.addLine(cartId, line).mapError(StorefrontError.OrderFailure.apply)
    yield cart

  override def updateCartLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): IO[StorefrontError, CartView] =
    cartService.updateLineCopies(cartId, lineId, copies).mapError(StorefrontError.OrderFailure.apply)

  override def removeCartLine(cartId: CartId, lineId: OrderLineId): IO[StorefrontError, CartView] =
    cartService.removeLine(cartId, lineId).mapError(StorefrontError.OrderFailure.apply)

  override def clearCart(cartId: CartId): IO[StorefrontError, CartView] =
    cartService.clearCart(cartId).mapError(StorefrontError.OrderFailure.apply)

  override def applyCartPromo(
      cartId: CartId,
      promoCode: String,
      sessionToken: Option[SessionToken],
  ): IO[StorefrontError, CartView] =
    for
      cart        <- cartService.getCart(cartId).mapError(StorefrontError.OrderFailure.apply)
      _           <- ZIO.fail(StorefrontError.OrderFailure(OrderError.EmptyCart(cartId))).when(cart.lines.isEmpty)
      customerOpt <- resolveOptionalCustomer(sessionToken)
      eval        <- evaluateCartPromo(promoCode, cart.lines, cart.currency, customerOpt)
      snap         = AppliedPromoSnapshot(eval.codeId, eval.code, eval.discountNet, eval.freeDelivery)
      updated     <- cartService.applyPromo(cartId, snap).mapError(StorefrontError.OrderFailure.apply)
    yield updated

  override def removeCartPromo(cartId: CartId): IO[StorefrontError, CartView] =
    cartService.removePromo(cartId).mapError(StorefrontError.OrderFailure.apply)

  override def getCart(cartId: CartId): IO[StorefrontError, CartView] =
    cartService.getCart(cartId).mapError(StorefrontError.OrderFailure.apply)

  // ── Checkout & Order History ───────────────────────────────────────────────

  override def getCheckoutOptions(
      source: CheckoutSource,
      currency: Currency,
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
  ): IO[StorefrontError, CheckoutOptionsView] =
    for
      customerOpt                     <- resolveOptionalCustomer(sessionToken)
      loaded                          <- loadCheckoutLines(source, sessionToken, ZIO.succeed(quickPreviewLineId))
      (lines, existingPromoOpt)        = loaded
      effectivePromoOpt               <- resolveEffectivePromo(promoCode, existingPromoOpt, lines, currency, customerOpt)
      itemsNet                         = lines.map(_.lineNetTotal).foldLeft(Money.zero)(_ + _).rounded
      totalWeight                      = aggregateWeights(lines)
      freeDelivery                     = effectivePromoOpt.exists(_.freeDelivery)
      deliveryOptions                 <- checkoutOptionsService.availableDeliveryOptions(currency, itemsNet, totalWeight, freeDelivery)
      (invoiceApproved, invoiceDueOpt) = invoiceEligibility(customerOpt)
      paymentOptions                  <- checkoutOptionsService.availablePaymentOptions(invoiceApproved)
      orderedShippings                 = customerOpt.fold(Nil) { c =>
                                           c.defaultShippingIndex match
                                             case Some(idx) if idx >= 0 && idx < c.shippingAddresses.size =>
                                               c.shippingAddresses(idx) :: c.shippingAddresses.patch(idx, Nil, 1)
                                             case _ => c.shippingAddresses
                                         }
      prefill                          = CheckoutPrefill(
                                           customer = customerOpt,
                                           billingAddress = customerOpt.map(_.billingAddress),
                                           shippingAddresses = orderedShippings,
                                           invoiceApproved = invoiceApproved,
                                           invoiceDueDays = invoiceDueOpt,
                                         )
    yield CheckoutOptionsView(prefill, deliveryOptions, paymentOptions)

  override def previewCheckout(
      source: CheckoutSource,
      currency: Currency,
      deliveryChoice: Option[DeliveryOptionChoice],
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
  ): IO[StorefrontError, CheckoutPreview] =
    for
      customerOpt               <- resolveOptionalCustomer(sessionToken)
      loaded                    <- loadCheckoutLines(source, sessionToken, ZIO.succeed(quickPreviewLineId))
      (lines, existingPromoOpt)  = loaded
      effectivePromoOpt         <- resolveEffectivePromo(promoCode, existingPromoOpt, lines, currency, customerOpt)
      itemsNet                   = lines.map(_.lineNetTotal).foldLeft(Money.zero)(_ + _).rounded
      totalWeight                = aggregateWeights(lines)
      freeDelivery               = effectivePromoOpt.exists(_.freeDelivery)
      deliveryOpt               <- ZIO.foreach(deliveryChoice) { choice =>
                                     checkoutOptionsService
                                       .resolveDelivery(choice, currency, itemsNet, totalWeight, freeDelivery)
                                       .mapError(StorefrontError.OrderFailure.apply)
                                   }
      promoDisc                  = effectivePromoOpt.map(_.discountNet).getOrElse(Money.zero).atMost(itemsNet).rounded
      discountedNet              = (itemsNet - promoDisc).rounded
      linesTax                   = computeLinesTax(lines, itemsNet, discountedNet)
      (shipNet, shipTax)         = deliveryOpt match
                                     case Some(DeliverySelection.Courier(_, _, feeNet, taxRate)) =>
                                       (feeNet.rounded, taxRate.taxOnNet(feeNet))
                                     case _ => (Money.zero, Money.zero)
      totalTax                   = (linesTax + shipTax).rounded
      grandGross                 = (discountedNet + shipNet + totalTax).rounded
    yield CheckoutPreview(
      lines = lines,
      currency = currency,
      itemsNetTotal = itemsNet,
      promoDiscountNet = promoDisc,
      appliedPromo = effectivePromoOpt,
      delivery = deliveryOpt,
      shippingNet = shipNet,
      taxTotal = totalTax,
      grandTotalGross = grandGross,
      totalWeightGrams = totalWeight,
    )

  override def submitCheckout(cmd: SubmitCheckoutCmd): IO[StorefrontError, CheckoutConfirmation] =
    for
      customerOpt                     <- resolveOptionalCustomer(cmd.sessionToken)
      loaded                          <- loadCheckoutLines(cmd.source, cmd.sessionToken, nextLineId)
      (lines, existingPromoOpt)        = loaded
      nonEmptyLines                   <- ZIO
                                           .fromOption(NonEmptyChunk.fromIterableOption(lines))
                                           .orElseFail(
                                             cmd.source match
                                               case CheckoutSource.FromCart(cid) => StorefrontError.OrderFailure(OrderError.EmptyCart(cid))
                                               case _                            => StorefrontError.OrderFailure(OrderError.EmptyCart(CartId.unsafe("quick")))
                                           )
      currency                         = nonEmptyLines.head.price.currency
      effectivePromoOpt               <- resolveEffectivePromo(cmd.promoCode, existingPromoOpt, lines, currency, customerOpt)
      itemsNet                         = lines.map(_.lineNetTotal).foldLeft(Money.zero)(_ + _).rounded
      totalWeight                      = aggregateWeights(lines)
      freeDelivery                     = effectivePromoOpt.exists(_.freeDelivery)
      delivery                        <- checkoutOptionsService
                                           .resolveDelivery(cmd.deliveryChoice, currency, itemsNet, totalWeight, freeDelivery)
                                           .mapError(StorefrontError.OrderFailure.apply)
      (invoiceApproved, invoiceDueOpt) = invoiceEligibility(customerOpt)
      payOpts                         <- checkoutOptionsService.availablePaymentOptions(invoiceApproved)
      chosenPay                       <- ZIO
                                           .fromOption(payOpts.find(_.method == cmd.paymentMethod))
                                           .orElseFail(StorefrontError.PaymentMethodNotAllowed(cmd.paymentMethod, "Unknown payment method"))
      _                               <- ZIO
                                           .fail(
                                             StorefrontError.PaymentMethodNotAllowed(
                                               cmd.paymentMethod,
                                               chosenPay.unavailableReason.getOrElse("Not available"),
                                             )
                                           )
                                           .unless(chosenPay.available)
      orderId                         <- nextOrderId
      orderNumber                     <- nextOrderNumber
      orderCustomer                    = customerOpt.fold(cmd.customer)(c => cmd.customer.copy(customerId = Some(c.id), segment = c.segment))
      order                           <- orderService
                                           .placeOrder(
                                             PlaceOrderCmd(
                                               orderId = orderId,
                                               orderNumber = orderNumber,
                                               customer = orderCustomer,
                                               billingAddress = cmd.billingAddress,
                                               delivery = delivery,
                                               paymentMethod = cmd.paymentMethod,
                                               invoiceApproved = invoiceApproved,
                                               lines = nonEmptyLines.map(_.toOrderLine),
                                               customerNote = cmd.customerNote,
                                               appliedPromo = effectivePromoOpt,
                                             )
                                           )
                                           .mapError(StorefrontError.OrderFailure.apply)
      // Transfer any draft artworks attached during configuration to their permanent OrderLineRef owners
      _                               <- ZIO.foreachDiscard(nonEmptyLines.toList.zipWithIndex) { case (line, lineIdx) =>
                                           ZIO.foreachDiscard(line.artworkIds.values.toList) { artId =>
                                             artworkService
                                               .reassignOwner(artId, ArtworkOwner.OrderLineRef(order.id, lineIdx))
                                               .mapError(StorefrontError.ArtworkFailure.apply)
                                           }
                                         }
      _                               <- ZIO.foreachDiscard(effectivePromoOpt)(p =>
                                           pricingService.redeemPromoCode(p.codeId).mapError(StorefrontError.PricingFailure.apply)
                                         )
      _                               <- cmd.source match
                                           case CheckoutSource.FromCart(cartId) =>
                                             cartService.clearCart(cartId).mapError(StorefrontError.OrderFailure.apply).unit
                                           case CheckoutSource.QuickSingleLine(_) =>
                                             ZIO.unit
      instructions                    <- checkoutOptionsService.generatePaymentInstructions(
                                           orderNumber = order.orderNumber,
                                           method = cmd.paymentMethod,
                                           grandTotalGross = order.totals.grandTotalGross,
                                           currency = order.totals.currency,
                                           invoiceDueDays = invoiceDueOpt,
                                         )
    yield CheckoutConfirmation(order, instructions)

  override def listMyOrders(sessionToken: SessionToken): IO[StorefrontError, List[OrderView]] =
    for
      customer <- currentCustomer(sessionToken)
      orders   <- orderService.listOrdersByCustomer(customer.id)
    yield orders

  override def updateMyAddresses(
      sessionToken: SessionToken,
      billingAddress: Address,
      shippingAddresses: List[Address],
      defaultShippingIndex: Option[Int],
  ): IO[StorefrontError, CustomerView] =
    for
      customer <- currentCustomer(sessionToken)
      updated  <- customerService
                    .updateAddresses(
                      UpdateCustomerAddressesCmd(
                        id = customer.id,
                        billingAddress = billingAddress,
                        shippingAddresses = shippingAddresses,
                        defaultShippingIndex = defaultShippingIndex,
                        actor = CustomerActor.CustomerSelf(
                          customer.id,
                          NonEmptyString.unsafe(s"${customer.contact.firstName.value} ${customer.contact.lastName.value}"),
                        ),
                      )
                    )
                    .mapError(StorefrontError.CustomerFailure.apply)
    yield updated

  override def getOrderPaymentInstructions(
      sessionToken: SessionToken,
      orderId: OrderId,
  ): IO[StorefrontError, PaymentInstructions] =
    for
      customer <- currentCustomer(sessionToken)
      order    <- loadOwnedOrder(customer.id, orderId)
      (_, dueOpt) = invoiceEligibility(Some(customer))
      instr    <- checkoutOptionsService.generatePaymentInstructions(
                    orderNumber = order.orderNumber,
                    method = order.payment.method,
                    grandTotalGross = order.totals.grandTotalGross,
                    currency = order.totals.currency,
                    invoiceDueDays = dueOpt,
                  )
    yield instr

  override def uploadDraftArtwork(
      sessionId: NonEmptyString,
      componentKey: ComponentKey,
      expectedTrimSize: Dimension2D,
      expectedPageCount: Option[Int],
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    val trimSize = payload.trimSizeOverride.getOrElse(expectedTrimSize)
    val pageCnt  = payload.pageCount.orElse(expectedPageCount).getOrElse(1)
    val req      = buildPreflightRequirement(expectedTrimSize, expectedPageCount)
    for
      artId <- nextArtworkId
      file  <- assetFile(s"drafts/${sessionId.value}/${artId.value}-${sanitizeFileSegment(payload.fileName.value)}", payload)
      view  <- artworkService
                 .upload(
                   UploadArtworkCmd(
                     artworkId = artId,
                     owner = ArtworkOwner.DraftSession(sessionId),
                     componentKey = componentKey,
                     file = file,
                     extractedSpecs = ExtractedFileSpecs(
                       resolutionDpi = payload.resolutionDpi,
                       trimSize = trimSize,
                       bleed = NonNegativeMm.unsafe(payload.bleedMm.max(0)),
                       colorSpace = payload.colorSpace,
                       pageCount = pageCnt,
                       fontsEmbedded = payload.fontsEmbedded,
                       hasUnflattenedTransparency = payload.hasUnflattenedTransparency,
                     ),
                     requirement = Some(req),
                   )
                 )
                 .mapError(StorefrontError.ArtworkFailure.apply)
    yield view

  override def deleteDraftArtwork(artworkId: ArtworkId): IO[StorefrontError, Unit] =
    for
      existing <- artworkService.getById(artworkId).option
      _        <- artworkService.deleteDraft(artworkId).mapError(StorefrontError.ArtworkFailure.apply)
      // Best-effort cleanup of stored print files & previews of the discarded draft.
      keys      = existing.toList.flatMap(_.revisions.map(_.file.storageKey.value))
      _        <- ZIO.foreachDiscard(keys)(k => blobStorage.delete(k).ignore *> blobStorage.delete(s"$k.preview").ignore)
    yield ()

  override def listOrderArtworks(
      sessionToken: SessionToken,
      orderId: OrderId,
  ): IO[StorefrontError, List[ArtworkSubmissionView]] =
    for
      customer <- currentCustomer(sessionToken)
      order    <- loadOwnedOrder(customer.id, orderId)
      byIdx    <- ZIO.foreach(order.lines.toList.zipWithIndex) { case (_, idx) =>
                    artworkService.listByOwner(ArtworkOwner.OrderLineRef(orderId, idx))
                  }
    yield byIdx.flatten

  override def uploadOrderLineArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    for
      customer <- currentCustomer(sessionToken)
      order    <- loadOwnedOrder(customer.id, orderId)
      _        <- ZIO
                    .fail(StorefrontError.OrderAlreadyAcceptedForArtwork(orderId, order.status))
                    .unless(order.status.allowsCustomerArtworkUpload)
      line     <- ZIO
                    .fromOption(order.lines.lift(lineIndex))
                    .orElseFail(StorefrontError.OrderFailure(OrderError.OrderLineNotFound(orderId, OrderLineId.unsafe(s"index-$lineIndex"))))
      existingList <- artworkService.listByOwner(ArtworkOwner.OrderLineRef(orderId, lineIndex))
      existingOpt   = existingList.find(_.componentKey == componentKey)
      res <- existingOpt match
               case Some(existing) =>
                 reuploadOrderArtworkPayload(sessionToken, orderId, existing.id, payload)
               case None =>
                 val comp     = line.product.components.find(_.key == componentKey).getOrElse(line.product.components.head)
                 val expPages = defaultPageCountFor(comp)
                 val trimSize = payload.trimSizeOverride.getOrElse(comp.finishedSize)
                 val pageCnt  = payload.pageCount.getOrElse(expPages)
                 val req      = buildPreflightRequirement(comp.finishedSize, Some(expPages))
                 for
                   artId <- nextArtworkId
                   file  <- assetFile(s"uploads/${orderId.value}/${artId.value}-${sanitizeFileSegment(payload.fileName.value)}", payload)
                   view  <- artworkService
                              .upload(
                                UploadArtworkCmd(
                                  artworkId = artId,
                                  owner = ArtworkOwner.OrderLineRef(orderId, lineIndex),
                                  componentKey = componentKey,
                                  file = file,
                                  extractedSpecs = ExtractedFileSpecs(
                                    resolutionDpi = payload.resolutionDpi,
                                    trimSize = trimSize,
                                    bleed = NonNegativeMm.unsafe(payload.bleedMm.max(0)),
                                    colorSpace = payload.colorSpace,
                                    pageCount = pageCnt,
                                    fontsEmbedded = payload.fontsEmbedded,
                                    hasUnflattenedTransparency = payload.hasUnflattenedTransparency,
                                  ),
                                  requirement = Some(req),
                                )
                              )
                              .mapError(StorefrontError.ArtworkFailure.apply)
                   _ <- orderService.attachArtwork(orderId, lineIndex, componentKey, artId).mapError(StorefrontError.OrderFailure.apply)
                 yield view
    yield res

  override def reuploadOrderArtworkPayload(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    for
      customer <- currentCustomer(sessionToken)
      order    <- loadOwnedOrder(customer.id, orderId)
      existing <- artworkService.getById(artworkId).mapError(StorefrontError.ArtworkFailure.apply)
      _        <- existing.owner match
                    case ArtworkOwner.OrderLineRef(oid, _) if oid == orderId => ZIO.unit
                    case _ => ZIO.fail(StorefrontError.OrderNotOwned(orderId))
      needsRevision = existing.status match
                        case ArtworkStatus.ChangesRequested(_) | ArtworkStatus.PreflightFailed => true
                        case _                                                                 => false
      canUpload = order.status.allowsCustomerArtworkUpload || (!order.status.isTerminal && needsRevision)
      _        <- ZIO.fail(StorefrontError.OrderAlreadyAcceptedForArtwork(orderId, order.status)).unless(canUpload)
      lineOpt   = existing.owner match
                    case ArtworkOwner.OrderLineRef(_, idx) => order.lines.lift(idx)
                    case _                                 => None
      compOpt   = lineOpt.flatMap(_.product.components.find(_.key == existing.componentKey)).orElse(lineOpt.map(_.product.components.head))
      expTrim   = compOpt.map(_.finishedSize).getOrElse(existing.latestRevision.extractedSpecs.trimSize)
      trimSize  = payload.trimSizeOverride.getOrElse(expTrim)
      pageCnt   = payload.pageCount.getOrElse(existing.latestRevision.extractedSpecs.pageCount)
      req       = buildPreflightRequirement(expTrim, Some(existing.latestRevision.extractedSpecs.pageCount))
      revNum    = existing.revisionCount + 1
      file     <- assetFile(s"uploads/${orderId.value}/${artworkId.value}-v$revNum-${sanitizeFileSegment(payload.fileName.value)}", payload)
      updated  <- artworkService
                    .uploadRevision(
                      UploadRevisionCmd(
                        artworkId = artworkId,
                        file = file,
                        extractedSpecs = ExtractedFileSpecs(
                          resolutionDpi = payload.resolutionDpi,
                          trimSize = trimSize,
                          bleed = NonNegativeMm.unsafe(payload.bleedMm.max(0)),
                          colorSpace = payload.colorSpace,
                          pageCount = pageCnt,
                          fontsEmbedded = payload.fontsEmbedded,
                          hasUnflattenedTransparency = payload.hasUnflattenedTransparency,
                        ),
                        requirement = Some(req),
                      )
                    )
                    .mapError(StorefrontError.ArtworkFailure.apply)
    yield updated

  override def reuploadOrderArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      fileName: NonEmptyString,
      resolutionDpi: Int,
      bleedMm: Int,
      colorSpace: ColorSpace,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    reuploadOrderArtworkPayload(
      sessionToken = sessionToken,
      orderId = orderId,
      artworkId = artworkId,
      payload = ArtworkUploadPayload(
        fileName = fileName,
        resolutionDpi = resolutionDpi,
        bleedMm = bleedMm,
        colorSpace = colorSpace,
        sizeBytes = 6_400_000L,
      ),
    )

  // ── Private Helpers ────────────────────────────────────────────────────────

  private def loadOwnedOrder(customerId: CustomerId, orderId: OrderId): IO[StorefrontError, OrderView] =
    for
      order <- orderService.getOrder(orderId).mapError(StorefrontError.OrderFailure.apply)
      _     <- ZIO.fail(StorefrontError.OrderNotOwned(orderId)).unless(order.customer.customerId.contains(customerId))
    yield order

  private def resolveOptionalCustomer(sessionToken: Option[SessionToken]): IO[StorefrontError, Option[CustomerView]] =
    sessionToken match
      case None => ZIO.none
      case Some(tok) =>
        for
          session  <- identityService.validateSession(tok).mapError(StorefrontError.IdentityFailure.apply)
          customer <- loadCustomerForPrincipal(session.principal)
        yield customer

  private def loadCustomerForPrincipal(principal: Principal): IO[StorefrontError, Option[CustomerView]] =
    principal.customerIdOpt match
      case None      => ZIO.none
      case Some(cid) => customerService.getById(cid).mapError(StorefrontError.CustomerFailure.apply).map(Some(_))

  private def buildDualPriceQuote(effective: PriceQuote, walkInOpt: Option[PriceQuote]): DualPriceQuote =
    walkInOpt match
      case Some(base) if base.netTotal > effective.netTotal =>
        val savingsNet = (base.netTotal - effective.netTotal).rounded
        val pct =
          if base.netTotal.isPositive then
            ((savingsNet.value / base.netTotal.value) * BigDecimal(100)).setScale(1, BigDecimal.RoundingMode.HALF_UP)
          else BigDecimal(0)
        DualPriceQuote(effective, Some(base), Some(savingsNet), Some(pct))
      case other =>
        DualPriceQuote(effective, other, None, None)

  private def evaluateSpeedOptions(
      product: ConfiguredProduct,
      currency: Currency,
  ): IO[StorefrontError, List[SpeedTierOption]] =
    val materialIds  = product.components.toList.map(_.material.id).toSet
    val finishingOps = product.components.toList.flatMap(_.finishes).flatMap(sf => StorefrontTranslators.toFinishingOperation(sf.finishType)).toSet
    for
      multipliers <- pricingService.speedMultipliers(currency).mapError(StorefrontError.PricingFailure.apply)
      options     <- ZIO.foreach(multipliers) { m =>
                       manufacturingService
                         .checkSpeedAvailability(
                           categoryId = product.categoryId,
                           speed = m.speed,
                           quantity = product.specs.quantity,
                           componentCount = product.components.size,
                           materialIds = materialIds,
                           finishingOps = finishingOps,
                         )
                         .either
                         .map {
                           case Right(_) =>
                             SpeedTierOption(m.speed, available = true, unavailableReason = None, m.baseMultiplier, m.effectiveMultiplier, m.surchargePercent)
                           case Left(err) =>
                             SpeedTierOption(m.speed, available = false, unavailableReason = Some(err.reason), m.baseMultiplier, m.effectiveMultiplier, m.surchargePercent)
                         }
                     }
    yield options

  /** Line ID used for quick-order lines in options / preview, so repeated previews don't consume the ID sequence. */
  private val quickPreviewLineId: OrderLineId = OrderLineId.unsafe("quick-preview")

  /** Loads the lines to check out. Cart lines carry the server-side price snapshot taken at add-to-cart time;
    * a quick-order item is always re-configured and re-priced here — the client never supplies a price.
    */
  private def loadCheckoutLines(
      source: CheckoutSource,
      sessionToken: Option[SessionToken],
      quickLineId: UIO[OrderLineId],
  ): IO[StorefrontError, (List[CartLine], Option[AppliedPromoSnapshot])] =
    source match
      case CheckoutSource.FromCart(cartId) =>
        cartService.getCart(cartId).mapError(StorefrontError.OrderFailure.apply).map(c => (c.lines, c.appliedPromo))
      case CheckoutSource.QuickSingleLine(item) =>
        for
          quoted <- configureAndQuote(item.cmd, item.currency, item.speed, sessionToken, promoCode = None)
          lineId <- quickLineId
        yield (
          List(
            StorefrontTranslators.toCartLine(
              lineId = lineId,
              product = quoted.configuration.product,
              speed = item.speed,
              quote = quoted.pricing.effectiveQuote,
              copies = item.copies,
              artworkIds = item.artworkIds,
            )
          ),
          None,
        )

  private def resolveEffectivePromo(
      promoCodeOverride: Option[String],
      existingPromo: Option[AppliedPromoSnapshot],
      lines: List[CartLine],
      currency: Currency,
      customerOpt: Option[CustomerView],
  ): IO[StorefrontError, Option[AppliedPromoSnapshot]] =
    val rawCodeOpt = promoCodeOverride.map(_.trim).filter(_.nonEmpty).orElse(existingPromo.map(_.code.value))
    rawCodeOpt match
      case None => ZIO.none
      case Some(code) if lines.nonEmpty =>
        evaluateCartPromo(code, lines, currency, customerOpt).map { eval =>
          Some(AppliedPromoSnapshot(eval.codeId, eval.code, eval.discountNet, eval.freeDelivery))
        }
      case Some(_) => ZIO.none

  private def evaluateCartPromo(
      code: String,
      lines: List[CartLine],
      currency: Currency,
      customerOpt: Option[CustomerView],
  ): IO[StorefrontError, PromoEvaluation] =
    val orderNet = lines.map(_.lineNetTotal).foldLeft(Money.zero)(_ + _).rounded
    val catIds   = lines.map(_.product.categoryId).toSet
    pricingService
      .evaluatePromoCode(
        EvaluatePromoCmd(
          code = code,
          orderNetTotal = orderNet,
          categoryIds = catIds,
          currency = currency,
          customerId = customerOpt.map(_.id),
          customerSegment = customerOpt.map(_.segment),
        )
      )
      .mapError(StorefrontError.PricingFailure.apply)

  private def invoiceEligibility(customerOpt: Option[CustomerView]): (Boolean, Option[Int]) =
    customerOpt match
      case Some(c) if c.status == CustomerStatus.Active =>
        c.paymentTerms match
          case PaymentTerms.NetInvoice(days, _) => (true, Some(days))
          case PaymentTerms.PrepaidOnly         => (false, None)
      case _ => (false, None)

  private def aggregateWeights(lines: List[CartLine]): Option[BigDecimal] =
    val ws = lines.map(_.totalWeightGrams)
    if ws.nonEmpty && ws.forall(_.isDefined) then Some(ws.flatten.sum) else None

  private def computeLinesTax(lines: List[CartLine], rawNet: Money, discountedNet: Money): Money =
    if !rawNet.isPositive || !discountedNet.isPositive then Money.zero
    else
      val distinctRates = lines.map(_.price.taxRate).distinct
      distinctRates match
        case single :: Nil => single.taxOnNet(discountedNet)
        case _ =>
          val ratio = discountedNet.value / rawNet.value
          lines.map(l => l.price.taxRate.taxOnNet((l.lineNetTotal * ratio).rounded)).foldLeft(Money.zero)(_ + _).rounded

  private def nextQuoteId: UIO[QuoteId] =
    idSequence.next.map(n => QuoteId.unsafe(s"q-$n"))

  private def nextLineId: UIO[OrderLineId] =
    idSequence.next.map(n => OrderLineId.unsafe(s"line-$n"))

  private def nextOrderId: UIO[OrderId] =
    idSequence.next.map(n => OrderId.unsafe(s"ord-$n"))

  private def nextOrderNumber: UIO[OrderNumber] =
    for
      now <- Clock.instant
      n   <- idSequence.next
      year = now.atZone(ZoneOffset.UTC).getYear
    yield OrderNumber.unsafe(f"ORD-$year-$n%04d")

  private def nextArtworkId: UIO[ArtworkId] =
    idSequence.next.map(n => ArtworkId.unsafe(s"art-$n"))

  private def buildPreflightRequirement(
      expectedTrimSize: Dimension2D,
      expectedPageCount: Option[Int],
  ): PreflightRequirement =
    val isLargeFormat = expectedTrimSize.width.value >= BigDecimal(400) || expectedTrimSize.height.value >= BigDecimal(400)
    PreflightRequirement(
      expectedTrimSize = expectedTrimSize,
      requiredBleed = NonNegativeMm.unsafe(3),
      minResolutionDpi = if isLargeFormat then 150 else 300,
      expectedPageCount = expectedPageCount,
    )

  private def defaultPageCountFor(comp: ComponentSnapshot): Int =
    comp.inkNotation match
      case Some(n) if n.endsWith("/0") => 1
      case Some(_)                     => 2
      case None                        => 1

  /** Builds artwork file metadata, moving inline `data:` URL payloads into `BlobStorage` (Phase 5). */
  private def assetFile(storageKey: String, payload: ArtworkUploadPayload): IO[StorefrontError, AssetFileMetadata] =
    BlobIngest
      .ingest(blobStorage, storageKey, payload.downloadUrl.map(_.value), payload.previewUrl.orElse(payload.downloadUrl).map(_.value))
      .mapError(StorefrontError.BlobFailure.apply)
      .map { ing =>
        AssetFileMetadata(
          fileName = payload.fileName,
          mimeType = payload.mimeType,
          sizeBytes = ing.sizeBytes.getOrElse(payload.sizeBytes).max(1L),
          storageKey = NonEmptyString.unsafe(storageKey),
          checksumSha256 = ing.checksumSha256.map(NonEmptyString.unsafe),
          downloadUrl = ing.downloadUrl.map(NonEmptyString.unsafe),
          previewUrl = ing.previewUrl.map(NonEmptyString.unsafe),
        )
      }

  private def sanitizeFileSegment(raw: String): String =
    val cleaned = raw.trim.replaceAll("[^a-zA-Z0-9._-]+", "-")
    if cleaned.nonEmpty then cleaned else "artwork.pdf"

object StorefrontFacade:
  val layer: ZLayer[
    CatalogService & PricingService & CustomerService & IdentityService & ArtworkService & CartService & CheckoutOptionsService & OrderService & ManufacturingService & IdSequence & BlobStorage,
    Nothing,
    StorefrontFacade,
  ] =
    ZLayer.fromZIO {
      for
        cat   <- ZIO.service[CatalogService]
        prc   <- ZIO.service[PricingService]
        cust  <- ZIO.service[CustomerService]
        id    <- ZIO.service[IdentityService]
        art   <- ZIO.service[ArtworkService]
        cart  <- ZIO.service[CartService]
        opts  <- ZIO.service[CheckoutOptionsService]
        ord   <- ZIO.service[OrderService]
        mfg   <- ZIO.service[ManufacturingService]
        seq   <- ZIO.service[IdSequence]
        blobs <- ZIO.service[BlobStorage]
      yield new StorefrontFacadeLive(cat, prc, cust, id, art, cart, opts, ord, mfg, seq, blobs)
    }
