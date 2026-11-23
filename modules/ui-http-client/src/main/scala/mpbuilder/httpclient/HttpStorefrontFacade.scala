package mpbuilder.httpclient

import mpbuilder.storefront.http.api.*
import mpbuilder.storefront.http.api.StorefrontEndpoints as E
import mpbuilder.artwork.{ArtworkSubmissionView, ColorSpace}
import mpbuilder.catalog.*
import mpbuilder.customers.CustomerView
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import zio.*

/** `StorefrontFacade` implemented over HTTP (`/api/v1/storefront`). Drop-in replacement for the
  * in-memory facade: `StorefrontClient.fromZio(new HttpStorefrontFacade(transport))`.
  */
final class HttpStorefrontFacade(http: HttpTransport) extends StorefrontFacade:

  private def run[I, O](e: sttp.tapir.PublicEndpoint[I, StorefrontError, O, Any])(in: I): IO[StorefrontError, O] =
    http.call(e)(in)

  override def getCatalog: UIO[ProductCatalog]                  = run(E.getCatalog)(()).orDieWith(e => new RuntimeException(e.message))
  override def listShowcaseListings: UIO[List[ShowcaseListing]] = run(E.listShowcases)(()).orDieWith(e => new RuntimeException(e.message))

  override def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId],
  ): IO[StorefrontError, List[Finish]] =
    run(E.compatibleFinishes)(CompatibleFinishesRequest(categoryId, componentKey, materialId, printingMethodId, alreadySelectedFinishIds))

  override def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): IO[StorefrontError, Map[FinishType, BigDecimal]] =
    run(E.finishParameterLimits)(FinishLimitsRequest(categoryId, componentKey, materialId, printingMethodId))

  override def configureAndQuote(
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed,
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
      lang: Language,
  ): IO[StorefrontError, ProductBuilderQuoteResult] =
    run(E.configureAndQuote)((sessionToken, ConfigureAndQuoteRequest(cmd, currency, speed, promoCode, lang)))

  override def pricePreset(
      categoryId: CategoryId,
      presetId: PresetId,
      currency: Currency,
      speed: ManufacturingSpeed,
      specsOverride: Option[ProductSpecs],
      sessionToken: Option[SessionToken],
      lang: Language,
  ): IO[StorefrontError, ProductBuilderQuoteResult] =
    run(E.pricePreset)((sessionToken, PricePresetRequest(categoryId, presetId, currency, speed, specsOverride, lang)))

  // ── Authentication ─────────────────────────────────────────────────────────

  override def loginWithPassword(email: EmailAddress, rawPassword: NonEmptyString): IO[StorefrontError, AuthResult] =
    run(E.loginWithPassword)(PasswordLoginRequest(email, rawPassword))
  override def requestCustomerOtp(identifier: LoginIdentifier): IO[StorefrontError, OtpChallengeView] =
    run(E.requestOtp)(OtpRequest(identifier))
  override def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): IO[StorefrontError, AuthResult] =
    run(E.verifyOtp)(OtpVerifyRequest(challengeId, code))
  override def currentCustomer(sessionToken: SessionToken): IO[StorefrontError, CustomerView] =
    run(E.currentCustomer)(sessionToken)
  override def logout(sessionToken: SessionToken): UIO[Unit] =
    run(E.logout)(sessionToken).ignore

  // ── Cart ───────────────────────────────────────────────────────────────────

  override def createCart(cartId: CartId, currency: Currency, sessionToken: Option[SessionToken]): IO[StorefrontError, CartView] =
    run(E.createCart)((sessionToken, CreateCartRequest(cartId, currency)))

  override def addConfiguredProductToCart(
      cartId: CartId,
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed,
      copies: Quantity,
      sessionToken: Option[SessionToken],
      artworkIds: Map[ComponentKey, ArtworkId],
  ): IO[StorefrontError, CartView] =
    run(E.addToCart)((cartId, sessionToken, AddToCartRequest(cmd, currency, speed, copies, artworkIds)))

  override def updateCartLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): IO[StorefrontError, CartView] =
    run(E.updateCartLineCopies)((cartId, lineId, UpdateCopiesRequest(copies)))
  override def removeCartLine(cartId: CartId, lineId: OrderLineId): IO[StorefrontError, CartView] =
    run(E.removeCartLine)((cartId, lineId))
  override def clearCart(cartId: CartId): IO[StorefrontError, CartView] =
    run(E.clearCart)(cartId)
  override def applyCartPromo(cartId: CartId, promoCode: String, sessionToken: Option[SessionToken]): IO[StorefrontError, CartView] =
    run(E.applyCartPromo)((cartId, sessionToken, ApplyPromoRequest(promoCode)))
  override def removeCartPromo(cartId: CartId): IO[StorefrontError, CartView] =
    run(E.removeCartPromo)(cartId)
  override def getCart(cartId: CartId): IO[StorefrontError, CartView] =
    run(E.getCart)(cartId)

  // ── Checkout ───────────────────────────────────────────────────────────────

  override def getCheckoutOptions(
      source: CheckoutSource,
      currency: Currency,
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
  ): IO[StorefrontError, CheckoutOptionsView] =
    run(E.checkoutOptions)((sessionToken, CheckoutOptionsRequest(source, currency, promoCode)))

  override def previewCheckout(
      source: CheckoutSource,
      currency: Currency,
      deliveryChoice: Option[DeliveryOptionChoice],
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
  ): IO[StorefrontError, CheckoutPreview] =
    run(E.previewCheckout)((sessionToken, CheckoutPreviewRequest(source, currency, deliveryChoice, promoCode)))

  override def submitCheckout(cmd: SubmitCheckoutCmd): IO[StorefrontError, CheckoutConfirmation] =
    run(E.submitCheckout)(cmd)

  override def listMyOrders(sessionToken: SessionToken): IO[StorefrontError, List[OrderView]] =
    run(E.listMyOrders)(sessionToken)

  // ── Self-service & artwork ─────────────────────────────────────────────────

  override def updateMyAddresses(
      sessionToken: SessionToken,
      billingAddress: Address,
      shippingAddresses: List[Address],
      defaultShippingIndex: Option[Int],
  ): IO[StorefrontError, CustomerView] =
    run(E.updateMyAddresses)((sessionToken, UpdateAddressesRequest(billingAddress, shippingAddresses, defaultShippingIndex)))

  override def getOrderPaymentInstructions(sessionToken: SessionToken, orderId: OrderId): IO[StorefrontError, PaymentInstructions] =
    run(E.paymentInstructions)((orderId, sessionToken))

  override def uploadDraftArtwork(
      sessionId: NonEmptyString,
      componentKey: ComponentKey,
      expectedTrimSize: Dimension2D,
      expectedPageCount: Option[Int],
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    run(E.uploadDraftArtwork)(DraftArtworkRequest(sessionId, componentKey, expectedTrimSize, expectedPageCount, payload))

  override def deleteDraftArtwork(artworkId: ArtworkId): IO[StorefrontError, Unit] =
    run(E.deleteDraftArtwork)(artworkId)

  override def listOrderArtworks(sessionToken: SessionToken, orderId: OrderId): IO[StorefrontError, List[ArtworkSubmissionView]] =
    run(E.listOrderArtworks)((orderId, sessionToken))

  override def uploadOrderLineArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    run(E.uploadOrderLineArtwork)((orderId, sessionToken, OrderLineArtworkRequest(lineIndex, componentKey, payload)))

  override def reuploadOrderArtworkPayload(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: ArtworkUploadPayload,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    run(E.reuploadOrderArtworkPayload)((orderId, artworkId, sessionToken, payload))

  override def reuploadOrderArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      fileName: NonEmptyString,
      resolutionDpi: Int,
      bleedMm: Int,
      colorSpace: ColorSpace,
  ): IO[StorefrontError, ArtworkSubmissionView] =
    run(E.reuploadOrderArtwork)((orderId, artworkId, sessionToken, ReuploadArtworkRequest(fileName, resolutionDpi, bleedMm, colorSpace)))
