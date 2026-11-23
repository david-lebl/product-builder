package mpbuilder.storefront
package http.api

import mpbuilder.api.ApiSchemas.given
import mpbuilder.api.ApiSupport.*
import StorefrontApiCodecs.given
import mpbuilder.artwork.ArtworkSubmissionView
import mpbuilder.catalog.*
import mpbuilder.customers.CustomerView
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import sttp.model.StatusCode
import sttp.tapir.*

/** Pure Tapir contracts for the customer-facing Storefront BFF (`/api/v1/storefront`). */
object StorefrontEndpoints:

  given Codec[String, CartId, CodecFormat.TextPlain]      = opaqueCodec(CartId(_))(_.value)
  given Codec[String, OrderId, CodecFormat.TextPlain]     = opaqueCodec(OrderId(_))(_.value)
  given Codec[String, OrderLineId, CodecFormat.TextPlain] = opaqueCodec(OrderLineId(_))(_.value)
  given Codec[String, ArtworkId, CodecFormat.TextPlain]   = opaqueCodec(ArtworkId(_))(_.value)

  private val base: PublicEndpoint[Unit, StorefrontError, Unit, Any] =
    endpoint
      .in("api" / apiVersion / "storefront")
      .errorOut(typedErrorOut[StorefrontError](_.message, "StorefrontError"))

  private val catalogBase  = base.tag("Storefront · Catalog & Product Builder")
  private val authBase     = base.in("auth").tag("Storefront · Authentication")
  private val cartBase     = base.in("carts").tag("Storefront · Cart")
  private val checkoutBase = base.in("checkout").tag("Storefront · Checkout")
  private val meBase       = base.in("me").tag("Storefront · My Account")
  private val artworkBase  = base.tag("Storefront · Artwork")

  private val cartId  = path[CartId]("cartId")
  private val orderId = path[OrderId]("orderId")

  // ── Catalog & Product Builder ──────────────────────────────────────────────

  val getCatalog =
    catalogBase.get.in("catalog").out(body[ProductCatalog]).summary("Active product catalog")

  val listShowcases =
    catalogBase.get.in("showcases").out(jsonList[ShowcaseListing]).summary("Showcase listings (sorted)")

  val compatibleFinishes =
    catalogBase.post.in("builder" / "compatible-finishes").in(body[CompatibleFinishesRequest])
      .out(jsonList[Finish]).summary("Finishes compatible with the selected component material/method")

  val finishParameterLimits =
    catalogBase.post.in("builder" / "finish-limits").in(body[FinishLimitsRequest])
      .out(jsonNamed[Map[FinishType, BigDecimal]]("Map_FinishType_BigDecimal")).summary("Rule-derived maxima for finish parameters")

  val configureAndQuote =
    catalogBase.post.in("builder" / "quote").in(optionalBearer).in(body[ConfigureAndQuoteRequest])
      .out(body[ProductBuilderQuoteResult]).summary("Configure a product and price it (B2B dual pricing when signed in)")

  val pricePreset =
    catalogBase.post.in("builder" / "preset-quote").in(optionalBearer).in(body[PricePresetRequest])
      .out(body[ProductBuilderQuoteResult]).summary("Price a category preset")

  // ── Authentication ─────────────────────────────────────────────────────────

  val loginWithPassword =
    authBase.post.in("login").in(body[PasswordLoginRequest]).out(body[AuthResult]).summary("Password login")

  val requestOtp =
    authBase.post.in("otp" / "request").in(body[OtpRequest]).out(body[OtpChallengeView])
      .summary("Request a passwordless one-time code (e-mail, IČO or DIČ)")

  val verifyOtp =
    authBase.post.in("otp" / "verify").in(body[OtpVerifyRequest]).out(body[AuthResult]).summary("Verify a one-time code")

  val currentCustomer =
    authBase.get.in("me").in(bearer).out(body[CustomerView]).summary("Signed-in customer")

  val logout =
    authBase.post.in("logout").in(bearer).out(statusCode(StatusCode.NoContent)).summary("Revoke the session")

  // ── Cart ───────────────────────────────────────────────────────────────────

  val createCart =
    cartBase.post.in(optionalBearer).in(body[CreateCartRequest]).out(body[CartView]).summary("Create (or reset) a cart")

  val getCart =
    cartBase.get.in(cartId).out(body[CartView]).summary("Get a cart")

  val addToCart =
    cartBase.post.in(cartId / "lines").in(optionalBearer).in(body[AddToCartRequest])
      .out(body[CartView]).summary("Add a configured product to the cart")

  val updateCartLineCopies =
    cartBase.patch.in(cartId / "lines" / path[OrderLineId]("lineId")).in(body[UpdateCopiesRequest])
      .out(body[CartView]).summary("Change the number of copies of a cart line")

  val removeCartLine =
    cartBase.delete.in(cartId / "lines" / path[OrderLineId]("lineId")).out(body[CartView]).summary("Remove a cart line")

  val clearCart =
    cartBase.delete.in(cartId / "lines").out(body[CartView]).summary("Remove all cart lines")

  val applyCartPromo =
    cartBase.put.in(cartId / "promo").in(optionalBearer).in(body[ApplyPromoRequest])
      .out(body[CartView]).summary("Apply a promo code")

  val removeCartPromo =
    cartBase.delete.in(cartId / "promo").out(body[CartView]).summary("Remove the applied promo code")

  // ── Checkout ───────────────────────────────────────────────────────────────

  val checkoutOptions =
    checkoutBase.post.in("options").in(optionalBearer).in(body[CheckoutOptionsRequest])
      .out(body[CheckoutOptionsView]).summary("Delivery & payment options with prefill")

  val previewCheckout =
    checkoutBase.post.in("preview").in(optionalBearer).in(body[CheckoutPreviewRequest])
      .out(body[CheckoutPreview]).summary("Totals preview for the selected delivery option")

  val submitCheckout =
    checkoutBase.post.in(body[SubmitCheckoutCmd]).out(body[CheckoutConfirmation]).summary("Place the order")

  // ── My account ─────────────────────────────────────────────────────────────

  val listMyOrders =
    meBase.get.in("orders").in(bearer).out(jsonList[OrderView]).summary("Order history of the signed-in customer")

  val updateMyAddresses =
    meBase.put.in("addresses").in(bearer).in(body[UpdateAddressesRequest])
      .out(body[CustomerView]).summary("Update billing & shipping addresses")

  val paymentInstructions =
    meBase.get.in("orders" / orderId / "payment-instructions").in(bearer)
      .out(body[PaymentInstructions]).summary("Bank transfer / QR payment instructions")

  val listOrderArtworks =
    meBase.get.in("orders" / orderId / "artwork").in(bearer)
      .out(jsonList[ArtworkSubmissionView]).summary("Artwork attached to my order")

  val uploadOrderLineArtwork =
    meBase.post.in("orders" / orderId / "artwork").in(bearer).in(body[OrderLineArtworkRequest])
      .out(body[ArtworkSubmissionView]).summary("Upload artwork for an order line (or a new revision)")

  val reuploadOrderArtworkPayload =
    meBase.post.in("orders" / orderId / "artwork" / path[ArtworkId]("artworkId") / "revisions").in(bearer)
      .in(body[ArtworkUploadPayload]).out(body[ArtworkSubmissionView]).summary("Upload a new artwork revision")

  val reuploadOrderArtwork =
    meBase.post.in("orders" / orderId / "artwork" / path[ArtworkId]("artworkId") / "reupload").in(bearer)
      .in(body[ReuploadArtworkRequest]).out(body[ArtworkSubmissionView]).summary("Re-upload artwork (metadata only)")

  // ── Draft artwork (product builder, before checkout) ───────────────────────

  val uploadDraftArtwork =
    artworkBase.post.in("artwork" / "drafts").in(body[DraftArtworkRequest])
      .out(body[ArtworkSubmissionView]).summary("Upload draft artwork with automated preflight")

  val deleteDraftArtwork =
    artworkBase.delete.in("artwork" / "drafts" / path[ArtworkId]("artworkId"))
      .out(statusCode(StatusCode.NoContent)).summary("Discard a draft artwork")

  val all: List[AnyEndpoint] = List(
    getCatalog, listShowcases, compatibleFinishes, finishParameterLimits, configureAndQuote, pricePreset,
    loginWithPassword, requestOtp, verifyOtp, currentCustomer, logout,
    createCart, getCart, addToCart, updateCartLineCopies, removeCartLine, clearCart, applyCartPromo, removeCartPromo,
    checkoutOptions, previewCheckout, submitCheckout,
    listMyOrders, updateMyAddresses, paymentInstructions, listOrderArtworks, uploadOrderLineArtwork,
    reuploadOrderArtworkPayload, reuploadOrderArtwork, uploadDraftArtwork, deleteDraftArtwork,
  )
