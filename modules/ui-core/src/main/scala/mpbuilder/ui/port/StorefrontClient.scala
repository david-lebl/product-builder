package mpbuilder.ui.port

import kyo.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.storefront.*

/** Pure Kyo client port for the Storefront UI.
  *
  * Completely decoupled from the transport layer:
  *   - Static / backend-less frontend wires `StorefrontClient.fromFacade(...)` backed by `SampleShowcaseRuntime`
  *   - Future REST / GraphQL / Kyo UI server-push integrations simply provide another `StorefrontClient`
  */
trait StorefrontClient:

  // ── Catalog & Product Builder ──────────────────────────────────────────────

  def getCatalog: ProductCatalog < Async
  def listShowcaseListings: List[ShowcaseListing] < Async
  def getExchangeRate(from: Currency, to: Currency): ExchangeRateQuote < (Abort[StorefrontError] & Async)

  def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId] = Set.empty,
  ): List[Finish] < (Abort[StorefrontError] & Async)

  def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): Map[FinishType, BigDecimal] < (Abort[StorefrontError] & Async)

  def configureAndQuote(
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      sessionToken: Option[SessionToken] = None,
      promoCode: Option[String] = None,
      lang: Language = Language.En,
  ): ProductBuilderQuoteResult < (Abort[StorefrontError] & Async)

  def pricePreset(
      categoryId: CategoryId,
      presetId: PresetId,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      specsOverride: Option[ProductSpecs] = None,
      sessionToken: Option[SessionToken] = None,
      lang: Language = Language.En,
  ): ProductBuilderQuoteResult < (Abort[StorefrontError] & Async)

  // ── Authentication & Session ───────────────────────────────────────────────

  def loginWithPassword(
      email: EmailAddress,
      rawPassword: NonEmptyString,
  ): AuthResult < (Abort[StorefrontError] & Async)

  def requestCustomerOtp(
      identifier: LoginIdentifier,
  ): OtpChallengeView < (Abort[StorefrontError] & Async)

  def verifyOtp(
      challengeId: OtpChallengeId,
      code: OtpCode,
  ): AuthResult < (Abort[StorefrontError] & Async)

  def currentCustomer(
      sessionToken: SessionToken,
  ): CustomerView < (Abort[StorefrontError] & Async)

  /** Re-hydrates a still-valid session from a persisted token; fails when it expired or was revoked. */
  def restoreSession(
      sessionToken: SessionToken,
  ): AuthResult < (Abort[StorefrontError] & Async)

  def logout(sessionToken: SessionToken): Unit < Async

  /** Optional demo-only OTP lookup so the static showcase UI can display the generated 6-digit code.
    * Real REST/GraphQL/server implementations always return `None`.
    */
  def demoOtpCode(challengeId: OtpChallengeId): Option[OtpCode] < Async = None

  // ── Shopping Cart ──────────────────────────────────────────────────────────

  def createCart(
      cartId: CartId,
      currency: Currency,
      sessionToken: Option[SessionToken] = None,
  ): CartView < (Abort[StorefrontError] & Async)

  def addConfiguredProductToCart(
      cartId: CartId,
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      copies: Quantity = Quantity.one,
      sessionToken: Option[SessionToken] = None,
      artworkIds: Map[ComponentKey, ArtworkId] = Map.empty,
      productionNote: Option[String] = None,
  ): CartView < (Abort[StorefrontError] & Async)

  def updateCartLineProductionNote(
      cartId: CartId,
      lineId: OrderLineId,
      note: Option[String],
  ): CartView < (Abort[StorefrontError] & Async)

  def updateMyOrderLineProductionNote(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineId: OrderLineId,
      note: Option[String],
  ): OrderView < (Abort[StorefrontError] & Async)

  def updateCartLineCopies(
      cartId: CartId,
      lineId: OrderLineId,
      copies: Quantity,
  ): CartView < (Abort[StorefrontError] & Async)

  def removeCartLine(
      cartId: CartId,
      lineId: OrderLineId,
  ): CartView < (Abort[StorefrontError] & Async)

  def clearCart(cartId: CartId): CartView < (Abort[StorefrontError] & Async)

  def applyCartPromo(
      cartId: CartId,
      promoCode: String,
      sessionToken: Option[SessionToken] = None,
  ): CartView < (Abort[StorefrontError] & Async)

  def removeCartPromo(cartId: CartId): CartView < (Abort[StorefrontError] & Async)

  def getCart(cartId: CartId): CartView < (Abort[StorefrontError] & Async)

  // ── Checkout & Order History ───────────────────────────────────────────────

  def getCheckoutOptions(
      source: CheckoutSource,
      currency: Currency,
      sessionToken: Option[SessionToken] = None,
      promoCode: Option[String] = None,
  ): CheckoutOptionsView < (Abort[StorefrontError] & Async)

  def previewCheckout(
      source: CheckoutSource,
      currency: Currency,
      deliveryChoice: Option[DeliveryOptionChoice],
      sessionToken: Option[SessionToken] = None,
      promoCode: Option[String] = None,
  ): CheckoutPreview < (Abort[StorefrontError] & Async)

  def submitCheckout(
      cmd: SubmitCheckoutCmd,
  ): CheckoutConfirmation < (Abort[StorefrontError] & Async)

  def listMyOrders(
      sessionToken: SessionToken,
  ): List[OrderView] < (Abort[StorefrontError] & Async)

  // ── Customer Self-Service & Artwork Management ─────────────────────────────

  def updateMyAddresses(
      sessionToken: SessionToken,
      billingAddress: Address,
      shippingAddresses: List[Address],
      defaultShippingIndex: Option[Int],
  ): CustomerView < (Abort[StorefrontError] & Async)

  def getOrderPaymentInstructions(
      sessionToken: SessionToken,
      orderId: OrderId,
  ): PaymentInstructions < (Abort[StorefrontError] & Async)

  def listOrderArtworks(
      sessionToken: SessionToken,
      orderId: OrderId,
  ): List[mpbuilder.artwork.ArtworkSubmissionView] < (Abort[StorefrontError] & Async)

  def uploadDraftArtwork(
      sessionId: NonEmptyString,
      componentKey: ComponentKey,
      expectedTrim: Dimension2D,
      expectedPages: Option[Int],
      payload: ArtworkUploadPayload,
      sessionToken: Option[SessionToken] = None,
  ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async)

  def deleteDraftArtwork(
      artworkId: ArtworkId,
  ): Unit < (Abort[StorefrontError] & Async)

  def uploadOrderLineArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: ArtworkUploadPayload,
  ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async)

  def reuploadOrderArtworkPayload(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: ArtworkUploadPayload,
  ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async)

  def reuploadOrderArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      fileName: NonEmptyString,
      resolutionDpi: Int = 300,
      bleedMm: Int = 3,
      colorSpace: mpbuilder.artwork.ColorSpace = mpbuilder.artwork.ColorSpace.CMYK,
  ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async)

object StorefrontClient:

  /** Exposes a (Kyo) `StorefrontFacade` — in-memory or HTTP-backed — as the UI's `StorefrontClient` port. */
  def fromFacade(
      facade: StorefrontFacade,
      demoOtpLookup: OtpChallengeId => Option[OtpCode] < Async = _ => None,
  ): StorefrontClient = new StorefrontClient:



    override def getCatalog: ProductCatalog < Async =
      facade.getCatalog

    override def listShowcaseListings: List[ShowcaseListing] < Async =
      facade.listShowcaseListings

    override def getExchangeRate(from: Currency, to: Currency): ExchangeRateQuote < (Abort[StorefrontError] & Async) =
      facade.getExchangeRate(from, to)

    override def compatibleFinishes(
        categoryId: CategoryId,
        componentKey: ComponentKey,
        materialId: MaterialId,
        printingMethodId: Option[PrintingMethodId],
        alreadySelectedFinishIds: Set[FinishId],
    ): List[Finish] < (Abort[StorefrontError] & Async) =
      facade.compatibleFinishes(categoryId, componentKey, materialId, printingMethodId, alreadySelectedFinishIds)

    override def finishParameterLimits(
        categoryId: CategoryId,
        componentKey: ComponentKey,
        materialId: MaterialId,
        printingMethodId: Option[PrintingMethodId],
    ): Map[FinishType, BigDecimal] < (Abort[StorefrontError] & Async) =
      facade.finishParameterLimits(categoryId, componentKey, materialId, printingMethodId)

    override def configureAndQuote(
        cmd: ConfigureProductCmd,
        currency: Currency,
        speed: ManufacturingSpeed,
        sessionToken: Option[SessionToken],
        promoCode: Option[String],
        lang: Language,
    ): ProductBuilderQuoteResult < (Abort[StorefrontError] & Async) =
      facade.configureAndQuote(cmd, currency, speed, sessionToken, promoCode, lang)

    override def pricePreset(
        categoryId: CategoryId,
        presetId: PresetId,
        currency: Currency,
        speed: ManufacturingSpeed,
        specsOverride: Option[ProductSpecs],
        sessionToken: Option[SessionToken],
        lang: Language,
    ): ProductBuilderQuoteResult < (Abort[StorefrontError] & Async) =
      facade.pricePreset(categoryId, presetId, currency, speed, specsOverride, sessionToken, lang)

    override def loginWithPassword(
        email: EmailAddress,
        rawPassword: NonEmptyString,
    ): AuthResult < (Abort[StorefrontError] & Async) =
      facade.loginWithPassword(email, rawPassword)

    override def requestCustomerOtp(
        identifier: LoginIdentifier,
    ): OtpChallengeView < (Abort[StorefrontError] & Async) =
      facade.requestCustomerOtp(identifier)

    override def verifyOtp(
        challengeId: OtpChallengeId,
        code: OtpCode,
    ): AuthResult < (Abort[StorefrontError] & Async) =
      facade.verifyOtp(challengeId, code)

    override def currentCustomer(
        sessionToken: SessionToken,
    ): CustomerView < (Abort[StorefrontError] & Async) =
      facade.currentCustomer(sessionToken)

    override def restoreSession(
        sessionToken: SessionToken,
    ): AuthResult < (Abort[StorefrontError] & Async) =
      facade.restoreSession(sessionToken)

    override def logout(sessionToken: SessionToken): Unit < Async =
      facade.logout(sessionToken)

    override def demoOtpCode(challengeId: OtpChallengeId): Option[OtpCode] < Async =
      locally(demoOtpLookup(challengeId))

    override def createCart(
        cartId: CartId,
        currency: Currency,
        sessionToken: Option[SessionToken],
    ): CartView < (Abort[StorefrontError] & Async) =
      facade.createCart(cartId, currency, sessionToken)

    override def addConfiguredProductToCart(
        cartId: CartId,
        cmd: ConfigureProductCmd,
        currency: Currency,
        speed: ManufacturingSpeed,
        copies: Quantity,
        sessionToken: Option[SessionToken],
        artworkIds: Map[ComponentKey, ArtworkId],
        productionNote: Option[String],
    ): CartView < (Abort[StorefrontError] & Async) =
      facade.addConfiguredProductToCart(cartId, cmd, currency, speed, copies, sessionToken, artworkIds, productionNote)

    override def updateCartLineProductionNote(
        cartId: CartId,
        lineId: OrderLineId,
        note: Option[String],
    ): CartView < (Abort[StorefrontError] & Async) =
      facade.updateCartLineProductionNote(cartId, lineId, note)

    override def updateMyOrderLineProductionNote(
        sessionToken: SessionToken,
        orderId: OrderId,
        lineId: OrderLineId,
        note: Option[String],
    ): OrderView < (Abort[StorefrontError] & Async) =
      facade.updateMyOrderLineProductionNote(sessionToken, orderId, lineId, note)

    override def updateCartLineCopies(
        cartId: CartId,
        lineId: OrderLineId,
        copies: Quantity,
    ): CartView < (Abort[StorefrontError] & Async) =
      facade.updateCartLineCopies(cartId, lineId, copies)

    override def removeCartLine(
        cartId: CartId,
        lineId: OrderLineId,
    ): CartView < (Abort[StorefrontError] & Async) =
      facade.removeCartLine(cartId, lineId)

    override def clearCart(cartId: CartId): CartView < (Abort[StorefrontError] & Async) =
      facade.clearCart(cartId)

    override def applyCartPromo(
        cartId: CartId,
        promoCode: String,
        sessionToken: Option[SessionToken],
    ): CartView < (Abort[StorefrontError] & Async) =
      facade.applyCartPromo(cartId, promoCode, sessionToken)

    override def removeCartPromo(cartId: CartId): CartView < (Abort[StorefrontError] & Async) =
      facade.removeCartPromo(cartId)

    override def getCart(cartId: CartId): CartView < (Abort[StorefrontError] & Async) =
      facade.getCart(cartId)

    override def getCheckoutOptions(
        source: CheckoutSource,
        currency: Currency,
        sessionToken: Option[SessionToken],
        promoCode: Option[String],
    ): CheckoutOptionsView < (Abort[StorefrontError] & Async) =
      facade.getCheckoutOptions(source, currency, sessionToken, promoCode)

    override def previewCheckout(
        source: CheckoutSource,
        currency: Currency,
        deliveryChoice: Option[DeliveryOptionChoice],
        sessionToken: Option[SessionToken],
        promoCode: Option[String],
    ): CheckoutPreview < (Abort[StorefrontError] & Async) =
      facade.previewCheckout(source, currency, deliveryChoice, sessionToken, promoCode)

    override def submitCheckout(
        cmd: SubmitCheckoutCmd,
    ): CheckoutConfirmation < (Abort[StorefrontError] & Async) =
      facade.submitCheckout(cmd)

    override def listMyOrders(
        sessionToken: SessionToken,
    ): List[OrderView] < (Abort[StorefrontError] & Async) =
      facade.listMyOrders(sessionToken)

    override def updateMyAddresses(
        sessionToken: SessionToken,
        billingAddress: Address,
        shippingAddresses: List[Address],
        defaultShippingIndex: Option[Int],
    ): CustomerView < (Abort[StorefrontError] & Async) =
      facade.updateMyAddresses(sessionToken, billingAddress, shippingAddresses, defaultShippingIndex)

    override def getOrderPaymentInstructions(
        sessionToken: SessionToken,
        orderId: OrderId,
    ): PaymentInstructions < (Abort[StorefrontError] & Async) =
      facade.getOrderPaymentInstructions(sessionToken, orderId)

    override def listOrderArtworks(
        sessionToken: SessionToken,
        orderId: OrderId,
    ): List[mpbuilder.artwork.ArtworkSubmissionView] < (Abort[StorefrontError] & Async) =
      facade.listOrderArtworks(sessionToken, orderId)

    override def uploadDraftArtwork(
        sessionId: NonEmptyString,
        componentKey: ComponentKey,
        expectedTrim: Dimension2D,
        expectedPages: Option[Int],
        payload: ArtworkUploadPayload,
        sessionToken: Option[SessionToken],
    ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async) =
      facade.uploadDraftArtwork(sessionId, componentKey, expectedTrim, expectedPages, payload)

    override def deleteDraftArtwork(
        artworkId: ArtworkId,
    ): Unit < (Abort[StorefrontError] & Async) =
      facade.deleteDraftArtwork(artworkId)

    override def uploadOrderLineArtwork(
        sessionToken: SessionToken,
        orderId: OrderId,
        lineIndex: Int,
        componentKey: ComponentKey,
        payload: ArtworkUploadPayload,
    ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async) =
      facade.uploadOrderLineArtwork(sessionToken, orderId, lineIndex, componentKey, payload)

    override def reuploadOrderArtworkPayload(
        sessionToken: SessionToken,
        orderId: OrderId,
        artworkId: ArtworkId,
        payload: ArtworkUploadPayload,
    ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async) =
      facade.reuploadOrderArtworkPayload(sessionToken, orderId, artworkId, payload)

    override def reuploadOrderArtwork(
        sessionToken: SessionToken,
        orderId: OrderId,
        artworkId: ArtworkId,
        fileName: NonEmptyString,
        resolutionDpi: Int,
        bleedMm: Int,
        colorSpace: mpbuilder.artwork.ColorSpace,
    ): mpbuilder.artwork.ArtworkSubmissionView < (Abort[StorefrontError] & Async) =
      facade.reuploadOrderArtwork(sessionToken, orderId, artworkId, fileName, resolutionDpi, bleedMm, colorSpace)
