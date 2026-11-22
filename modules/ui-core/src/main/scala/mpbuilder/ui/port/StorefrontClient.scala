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
  *   - Static / backend-less frontend wires `StorefrontClient.fromZio(...)` backed by `SampleShowcaseRuntime`
  *   - Future REST / GraphQL / Kyo UI server-push integrations simply provide another `StorefrontClient`
  */
trait StorefrontClient:

  // ── Catalog & Product Builder ──────────────────────────────────────────────

  def getCatalog: ProductCatalog < Async
  def listShowcaseListings: List[ShowcaseListing] < Async

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
  ): CartView < (Abort[StorefrontError] & Async)

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

object StorefrontClient:

  /** Lifts any ZIO `StorefrontFacade` into a pure Kyo `StorefrontClient` using `kyo-zio`. */
  def fromZio(
      facade: StorefrontFacade,
      demoOtpLookup: OtpChallengeId => zio.UIO[Option[OtpCode]] = _ => zio.ZIO.none,
  ): StorefrontClient = new StorefrontClient:

    private inline def liftUIO[A](zioEff: => zio.UIO[A]): A < Async =
      Abort.run[Nothing](ZIOs.get(zioEff)).map(_.getOrThrow)

    private inline def liftIO[A](zioEff: => zio.IO[StorefrontError, A]): A < (Abort[StorefrontError] & Async) =
      ZIOs.get(zioEff)

    override def getCatalog: ProductCatalog < Async =
      liftUIO(facade.getCatalog)

    override def listShowcaseListings: List[ShowcaseListing] < Async =
      liftUIO(facade.listShowcaseListings)

    override def compatibleFinishes(
        categoryId: CategoryId,
        componentKey: ComponentKey,
        materialId: MaterialId,
        printingMethodId: Option[PrintingMethodId],
        alreadySelectedFinishIds: Set[FinishId],
    ): List[Finish] < (Abort[StorefrontError] & Async) =
      liftIO(facade.compatibleFinishes(categoryId, componentKey, materialId, printingMethodId, alreadySelectedFinishIds))

    override def finishParameterLimits(
        categoryId: CategoryId,
        componentKey: ComponentKey,
        materialId: MaterialId,
        printingMethodId: Option[PrintingMethodId],
    ): Map[FinishType, BigDecimal] < (Abort[StorefrontError] & Async) =
      liftIO(facade.finishParameterLimits(categoryId, componentKey, materialId, printingMethodId))

    override def configureAndQuote(
        cmd: ConfigureProductCmd,
        currency: Currency,
        speed: ManufacturingSpeed,
        sessionToken: Option[SessionToken],
        promoCode: Option[String],
        lang: Language,
    ): ProductBuilderQuoteResult < (Abort[StorefrontError] & Async) =
      liftIO(facade.configureAndQuote(cmd, currency, speed, sessionToken, promoCode, lang))

    override def pricePreset(
        categoryId: CategoryId,
        presetId: PresetId,
        currency: Currency,
        speed: ManufacturingSpeed,
        specsOverride: Option[ProductSpecs],
        sessionToken: Option[SessionToken],
        lang: Language,
    ): ProductBuilderQuoteResult < (Abort[StorefrontError] & Async) =
      liftIO(facade.pricePreset(categoryId, presetId, currency, speed, specsOverride, sessionToken, lang))

    override def loginWithPassword(
        email: EmailAddress,
        rawPassword: NonEmptyString,
    ): AuthResult < (Abort[StorefrontError] & Async) =
      liftIO(facade.loginWithPassword(email, rawPassword))

    override def requestCustomerOtp(
        identifier: LoginIdentifier,
    ): OtpChallengeView < (Abort[StorefrontError] & Async) =
      liftIO(facade.requestCustomerOtp(identifier))

    override def verifyOtp(
        challengeId: OtpChallengeId,
        code: OtpCode,
    ): AuthResult < (Abort[StorefrontError] & Async) =
      liftIO(facade.verifyOtp(challengeId, code))

    override def currentCustomer(
        sessionToken: SessionToken,
    ): CustomerView < (Abort[StorefrontError] & Async) =
      liftIO(facade.currentCustomer(sessionToken))

    override def logout(sessionToken: SessionToken): Unit < Async =
      liftUIO(facade.logout(sessionToken))

    override def demoOtpCode(challengeId: OtpChallengeId): Option[OtpCode] < Async =
      liftUIO(demoOtpLookup(challengeId))

    override def createCart(
        cartId: CartId,
        currency: Currency,
        sessionToken: Option[SessionToken],
    ): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.createCart(cartId, currency, sessionToken))

    override def addConfiguredProductToCart(
        cartId: CartId,
        cmd: ConfigureProductCmd,
        currency: Currency,
        speed: ManufacturingSpeed,
        copies: Quantity,
        sessionToken: Option[SessionToken],
    ): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.addConfiguredProductToCart(cartId, cmd, currency, speed, copies, sessionToken))

    override def updateCartLineCopies(
        cartId: CartId,
        lineId: OrderLineId,
        copies: Quantity,
    ): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.updateCartLineCopies(cartId, lineId, copies))

    override def removeCartLine(
        cartId: CartId,
        lineId: OrderLineId,
    ): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.removeCartLine(cartId, lineId))

    override def clearCart(cartId: CartId): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.clearCart(cartId))

    override def applyCartPromo(
        cartId: CartId,
        promoCode: String,
        sessionToken: Option[SessionToken],
    ): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.applyCartPromo(cartId, promoCode, sessionToken))

    override def removeCartPromo(cartId: CartId): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.removeCartPromo(cartId))

    override def getCart(cartId: CartId): CartView < (Abort[StorefrontError] & Async) =
      liftIO(facade.getCart(cartId))

    override def getCheckoutOptions(
        source: CheckoutSource,
        currency: Currency,
        sessionToken: Option[SessionToken],
        promoCode: Option[String],
    ): CheckoutOptionsView < (Abort[StorefrontError] & Async) =
      liftIO(facade.getCheckoutOptions(source, currency, sessionToken, promoCode))

    override def previewCheckout(
        source: CheckoutSource,
        currency: Currency,
        deliveryChoice: Option[DeliveryOptionChoice],
        sessionToken: Option[SessionToken],
        promoCode: Option[String],
    ): CheckoutPreview < (Abort[StorefrontError] & Async) =
      liftIO(facade.previewCheckout(source, currency, deliveryChoice, sessionToken, promoCode))

    override def submitCheckout(
        cmd: SubmitCheckoutCmd,
    ): CheckoutConfirmation < (Abort[StorefrontError] & Async) =
      liftIO(facade.submitCheckout(cmd))

    override def listMyOrders(
        sessionToken: SessionToken,
    ): List[OrderView] < (Abort[StorefrontError] & Async) =
      liftIO(facade.listMyOrders(sessionToken))
