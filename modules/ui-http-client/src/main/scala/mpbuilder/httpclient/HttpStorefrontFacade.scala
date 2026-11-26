package mpbuilder.httpclient

import kyo.*
import mpbuilder.artwork.{ArtworkSubmissionView, ColorSpace}
import mpbuilder.catalog.*
import mpbuilder.customers.CustomerView
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.storefront.http.api.*
import mpbuilder.storefront.http.api.StorefrontApi.{path, seg}
import mpbuilder.storefront.http.api.StorefrontApiCodecs.given

/** `StorefrontFacade` implemented over HTTP (`/api/v1/storefront`). Drop-in replacement for the
  * in-memory facade: `StorefrontClient.fromFacade(new HttpStorefrontFacade(…))`.
  */
final class HttpStorefrontFacade(transport: HttpTransport, baseUrl: String) extends StorefrontFacade:

  private val http = new ApiCaller[StorefrontError](transport, baseUrl)
  import http.json

  private def orPanic[A](v: A < (Async & Abort[StorefrontError])): A < Async =
    Abort.run[StorefrontError](v).map {
      case Result.Success(a) => a
      case Result.Failure(e) => Abort.panic(new RuntimeException(e.message))
      case p: Result.Panic   => Abort.get(p)
    }

  override def getCatalog: ProductCatalog < Async                  = orPanic(http.call[ProductCatalog]("GET", path("catalog")))
  override def listShowcaseListings: List[ShowcaseListing] < Async = orPanic(http.call[List[ShowcaseListing]]("GET", path("showcases")))
  override def getExchangeRate(from: Currency, to: Currency): ExchangeRateQuote < (Async & Abort[StorefrontError]) =
    http.call[ExchangeRateQuote]("GET", path("currency", "rate", seg(from.toString), seg(to.toString)))

  override def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId],
  ): List[Finish] < (Async & Abort[StorefrontError]) =
    http.call[List[Finish]](
      "POST", path("builder", "compatible-finishes"),
      body = json(CompatibleFinishesRequest(categoryId, componentKey, materialId, printingMethodId, alreadySelectedFinishIds)),
    )

  override def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): Map[FinishType, BigDecimal] < (Async & Abort[StorefrontError]) =
    http.call[Map[FinishType, BigDecimal]](
      "POST", path("builder", "finish-limits"),
      body = json(FinishLimitsRequest(categoryId, componentKey, materialId, printingMethodId)),
    )

  override def configureAndQuote(
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed,
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
      lang: Language,
  ): ProductBuilderQuoteResult < (Async & Abort[StorefrontError]) =
    http.call[ProductBuilderQuoteResult](
      "POST", path("builder", "quote"), sessionToken, json(ConfigureAndQuoteRequest(cmd, currency, speed, promoCode, lang)),
    )

  override def pricePreset(
      categoryId: CategoryId,
      presetId: PresetId,
      currency: Currency,
      speed: ManufacturingSpeed,
      specsOverride: Option[ProductSpecs],
      sessionToken: Option[SessionToken],
      lang: Language,
  ): ProductBuilderQuoteResult < (Async & Abort[StorefrontError]) =
    http.call[ProductBuilderQuoteResult](
      "POST", path("builder", "preset-quote"), sessionToken,
      json(PricePresetRequest(categoryId, presetId, currency, speed, specsOverride, lang)),
    )

  // ── Authentication ─────────────────────────────────────────────────────────

  override def loginWithPassword(email: EmailAddress, rawPassword: NonEmptyString): AuthResult < (Async & Abort[StorefrontError]) =
    http.call[AuthResult]("POST", path("auth", "login"), body = json(PasswordLoginRequest(email, rawPassword)))
  override def requestCustomerOtp(identifier: LoginIdentifier): OtpChallengeView < (Async & Abort[StorefrontError]) =
    http.call[OtpChallengeView]("POST", path("auth", "otp", "request"), body = json(OtpRequest(identifier)))
  override def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): AuthResult < (Async & Abort[StorefrontError]) =
    http.call[AuthResult]("POST", path("auth", "otp", "verify"), body = json(OtpVerifyRequest(challengeId, code)))
  override def currentCustomer(sessionToken: SessionToken): CustomerView < (Async & Abort[StorefrontError]) =
    http.call[CustomerView]("GET", path("auth", "me"), Some(sessionToken))
  override def logout(sessionToken: SessionToken): Unit < Async =
    http.callUnit("POST", path("auth", "logout"), Some(sessionToken)).ignore

  // ── Cart ───────────────────────────────────────────────────────────────────

  private def cart(cartId: CartId, rest: String*): String = path(("carts" +: seg(cartId.value) +: rest)*)

  override def createCart(cartId: CartId, currency: Currency, sessionToken: Option[SessionToken]): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("POST", path("carts"), sessionToken, json(CreateCartRequest(cartId, currency)))

  override def addConfiguredProductToCart(
      cartId: CartId,
      cmd: ConfigureProductCmd,
      currency: Currency,
      speed: ManufacturingSpeed,
      copies: Quantity,
      sessionToken: Option[SessionToken],
      artworkIds: Map[ComponentKey, ArtworkId],
      productionNote: Option[String],
  ): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("POST", cart(cartId, "lines"), sessionToken, json(AddToCartRequest(cmd, currency, speed, copies, artworkIds, productionNote)))

  override def updateCartLineProductionNote(cartId: CartId, lineId: OrderLineId, note: Option[String]): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("PUT", cart(cartId, "lines", seg(lineId.value), "production-note"), body = json(ProductionNoteRequest(note)))

  override def updateCartLineCopies(cartId: CartId, lineId: OrderLineId, copies: Quantity): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("PATCH", cart(cartId, "lines", seg(lineId.value)), body = json(UpdateCopiesRequest(copies)))
  override def removeCartLine(cartId: CartId, lineId: OrderLineId): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("DELETE", cart(cartId, "lines", seg(lineId.value)))
  override def clearCart(cartId: CartId): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("DELETE", cart(cartId, "lines"))
  override def applyCartPromo(cartId: CartId, promoCode: String, sessionToken: Option[SessionToken]): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("PUT", cart(cartId, "promo"), sessionToken, json(ApplyPromoRequest(promoCode)))
  override def removeCartPromo(cartId: CartId): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("DELETE", cart(cartId, "promo"))
  override def getCart(cartId: CartId): CartView < (Async & Abort[StorefrontError]) =
    http.call[CartView]("GET", cart(cartId))

  // ── Checkout ───────────────────────────────────────────────────────────────

  override def getCheckoutOptions(
      source: CheckoutSource,
      currency: Currency,
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
  ): CheckoutOptionsView < (Async & Abort[StorefrontError]) =
    http.call[CheckoutOptionsView]("POST", path("checkout", "options"), sessionToken, json(CheckoutOptionsRequest(source, currency, promoCode)))

  override def previewCheckout(
      source: CheckoutSource,
      currency: Currency,
      deliveryChoice: Option[DeliveryOptionChoice],
      sessionToken: Option[SessionToken],
      promoCode: Option[String],
  ): CheckoutPreview < (Async & Abort[StorefrontError]) =
    http.call[CheckoutPreview](
      "POST", path("checkout", "preview"), sessionToken, json(CheckoutPreviewRequest(source, currency, deliveryChoice, promoCode)),
    )

  override def submitCheckout(cmd: SubmitCheckoutCmd): CheckoutConfirmation < (Async & Abort[StorefrontError]) =
    http.call[CheckoutConfirmation]("POST", path("checkout"), body = json(cmd))

  override def listMyOrders(sessionToken: SessionToken): List[OrderView] < (Async & Abort[StorefrontError]) =
    http.call[List[OrderView]]("GET", path("me", "orders"), Some(sessionToken))

  // ── Self-service & artwork ─────────────────────────────────────────────────

  private def myOrder(orderId: OrderId, rest: String*): String = path(("me" +: "orders" +: seg(orderId.value) +: rest)*)

  override def updateMyAddresses(
      sessionToken: SessionToken,
      billingAddress: Address,
      shippingAddresses: List[Address],
      defaultShippingIndex: Option[Int],
  ): CustomerView < (Async & Abort[StorefrontError]) =
    http.call[CustomerView](
      "PUT", path("me", "addresses"), Some(sessionToken),
      json(UpdateAddressesRequest(billingAddress, shippingAddresses, defaultShippingIndex)),
    )

  override def getOrderPaymentInstructions(sessionToken: SessionToken, orderId: OrderId): PaymentInstructions < (Async & Abort[StorefrontError]) =
    http.call[PaymentInstructions]("GET", myOrder(orderId, "payment-instructions"), Some(sessionToken))

  override def uploadDraftArtwork(
      sessionId: NonEmptyString,
      componentKey: ComponentKey,
      expectedTrimSize: Dimension2D,
      expectedPageCount: Option[Int],
      payload: ArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[StorefrontError]) =
    http.call[ArtworkSubmissionView](
      "POST", path("artwork", "drafts"),
      body = json(DraftArtworkRequest(sessionId, componentKey, expectedTrimSize, expectedPageCount, payload)),
    )

  override def deleteDraftArtwork(artworkId: ArtworkId): Unit < (Async & Abort[StorefrontError]) =
    http.callUnit("DELETE", path("artwork", "drafts", seg(artworkId.value)))

  override def listOrderArtworks(sessionToken: SessionToken, orderId: OrderId): List[ArtworkSubmissionView] < (Async & Abort[StorefrontError]) =
    http.call[List[ArtworkSubmissionView]]("GET", myOrder(orderId, "artwork"), Some(sessionToken))

  override def uploadOrderLineArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: ArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[StorefrontError]) =
    http.call[ArtworkSubmissionView](
      "POST", myOrder(orderId, "artwork"), Some(sessionToken), json(OrderLineArtworkRequest(lineIndex, componentKey, payload)),
    )

  override def updateMyOrderLineProductionNote(
      sessionToken: SessionToken,
      orderId: OrderId,
      lineId: OrderLineId,
      note: Option[String],
  ): OrderView < (Async & Abort[StorefrontError]) =
    http.call[OrderView](
      "PUT", myOrder(orderId, "lines", seg(lineId.value), "production-note"), Some(sessionToken), json(ProductionNoteRequest(note)),
    )

  override def reuploadOrderArtworkPayload(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: ArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[StorefrontError]) =
    http.call[ArtworkSubmissionView]("POST", myOrder(orderId, "artwork", seg(artworkId.value), "revisions"), Some(sessionToken), json(payload))

  override def reuploadOrderArtwork(
      sessionToken: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      fileName: NonEmptyString,
      resolutionDpi: Int,
      bleedMm: Int,
      colorSpace: ColorSpace,
  ): ArtworkSubmissionView < (Async & Abort[StorefrontError]) =
    http.call[ArtworkSubmissionView](
      "POST", myOrder(orderId, "artwork", seg(artworkId.value), "reupload"), Some(sessionToken),
      json(ReuploadArtworkRequest(fileName, resolutionDpi, bleedMm, colorSpace)),
    )
