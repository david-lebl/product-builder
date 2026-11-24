package mpbuilder.storefront
package http

import api.*
import api.StorefrontApi.Tags
import api.StorefrontApiCodecs.given
import mpbuilder.artwork.ArtworkSubmissionView
import mpbuilder.customers.CustomerView
import kyo.*
import mpbuilder.api.server.ApiServer.*
import mpbuilder.catalog.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*

/** Inbound HTTP adapter: kyo-http routes for `/api/v1/storefront`, bound to `StorefrontFacade`. */
final class StorefrontRoutes private[storefront] (facade: StorefrontFacade):
  import StorefrontRoutes.{given, *}

  private def run[A](v: => A < (Async & Abort[StorefrontError]))(using Frame): A < (Async & Abort[Failure]) =
    Abort.runPartial[StorefrontError](v).map {
      case Result.Success(a) => a
      case Result.Failure(e) => Abort.fail(Failure(e))
    }

  private def ok[A: Schema](v: => A < (Async & Abort[StorefrontError]))(using Frame) =
    run(v).map(HttpResponse.ok(_))

  private def required(header: Maybe[String])(using Frame): SessionToken < Abort[Failure] =
    bearerToken(header) match
      case Some(token) => token
      case None        => Abort.fail(Failure(StorefrontError.IdentityFailure(IdentityError.SessionNotFound)))

  val handlers: Seq[HttpHandler[?, ?, ?]] = Seq(
    // ── Catalog & Product Builder
    HttpRoute.getRaw(base / "catalog").response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Active product catalog")
      .handler(_ => ok(facade.getCatalog)),
    HttpRoute.getRaw(base / "showcases").response(_.bodyJson[List[ShowcaseListing]]).apiErrors
      .doc(Tags.Catalog, "Showcase listings (sorted)")
      .handler(_ => ok(facade.listShowcaseListings)),
    HttpRoute.postRaw(base / "builder" / "compatible-finishes").request(_.bodyJson[CompatibleFinishesRequest])
      .response(_.bodyJson[List[Finish]]).apiErrors
      .doc(Tags.Catalog, "Finishes compatible with the selected component material/method")
      .handler { req =>
        val r = req.fields.body
        ok(facade.compatibleFinishes(r.categoryId, r.componentKey, r.materialId, r.printingMethodId, r.alreadySelectedFinishIds))
      },
    HttpRoute.postRaw(base / "builder" / "finish-limits").request(_.bodyJson[FinishLimitsRequest])
      .response(_.bodyJson[Map[FinishType, BigDecimal]]).apiErrors
      .doc(Tags.Catalog, "Rule-derived maxima for finish parameters")
      .handler { req =>
        val r = req.fields.body
        ok(facade.finishParameterLimits(r.categoryId, r.componentKey, r.materialId, r.printingMethodId))
      },
    HttpRoute.postRaw(base / "builder" / "quote").request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ConfigureAndQuoteRequest])
      .response(_.bodyJson[ProductBuilderQuoteResult]).apiErrors
      .doc(Tags.Catalog, "Configure a product and price it (B2B dual pricing when signed in)")
      .handler { req =>
        val r = req.fields.body
        ok(facade.configureAndQuote(r.cmd, r.currency, r.speed, bearerToken(req.fields.authorization), r.promoCode, r.lang))
      },
    HttpRoute.postRaw(base / "builder" / "preset-quote").request(_.headerOpt[String]("authorization", "Authorization").bodyJson[PricePresetRequest])
      .response(_.bodyJson[ProductBuilderQuoteResult]).apiErrors
      .doc(Tags.Catalog, "Price a category preset")
      .handler { req =>
        val r = req.fields.body
        ok(facade.pricePreset(r.categoryId, r.presetId, r.currency, r.speed, r.specsOverride, bearerToken(req.fields.authorization), r.lang))
      },
    // ── Authentication
    HttpRoute.postRaw(base / "auth" / "login").request(_.bodyJson[PasswordLoginRequest])
      .response(_.bodyJson[AuthResult]).apiErrors
      .doc(Tags.Auth, "Password login")
      .handler(req => ok(facade.loginWithPassword(req.fields.body.email, req.fields.body.password))),
    HttpRoute.postRaw(base / "auth" / "otp" / "request").request(_.bodyJson[OtpRequest])
      .response(_.bodyJson[OtpChallengeView]).apiErrors
      .doc(Tags.Auth, "Request a passwordless one-time code (e-mail, IČO or DIČ)")
      .handler(req => ok(facade.requestCustomerOtp(req.fields.body.identifier))),
    HttpRoute.postRaw(base / "auth" / "otp" / "verify").request(_.bodyJson[OtpVerifyRequest])
      .response(_.bodyJson[AuthResult]).apiErrors
      .doc(Tags.Auth, "Verify a one-time code")
      .handler(req => ok(facade.verifyOtp(req.fields.body.challengeId, req.fields.body.code))),
    HttpRoute.getRaw(base / "auth" / "me").request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Auth, "Signed-in customer")
      .handler(req => required(req.fields.authorization).map(tok => ok(facade.currentCustomer(tok)))),
    HttpRoute.postRaw(base / "auth" / "logout").request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.status(HttpStatus.NoContent)).apiErrors
      .doc(Tags.Auth, "Revoke the session")
      .handler(req => required(req.fields.authorization).map(tok => facade.logout(tok).andThen(HttpResponse.noContent))),
    // ── Cart
    HttpRoute.postRaw(base / "carts").request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CreateCartRequest])
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Create (or reset) a cart")
      .handler { req =>
        val r = req.fields.body
        ok(facade.createCart(r.cartId, r.currency, bearerToken(req.fields.authorization)))
      },
    HttpRoute.getRaw(base / "carts" / Capture[CartId]("cartId"))
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Get a cart")
      .handler(req => ok(facade.getCart(req.fields.cartId))),
    HttpRoute.postRaw(base / "carts" / Capture[CartId]("cartId") / "lines")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[AddToCartRequest])
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Add a configured product to the cart")
      .handler { req =>
        val r = req.fields.body
        ok(facade.addConfiguredProductToCart(req.fields.cartId, r.cmd, r.currency, r.speed, r.copies, bearerToken(req.fields.authorization), r.artworkIds))
      },
    HttpRoute.patchRaw(base / "carts" / Capture[CartId]("cartId") / "lines" / Capture[OrderLineId]("lineId"))
      .request(_.bodyJson[UpdateCopiesRequest])
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Change the number of copies of a cart line")
      .handler(req => ok(facade.updateCartLineCopies(req.fields.cartId, req.fields.lineId, req.fields.body.copies))),
    HttpRoute.deleteRaw(base / "carts" / Capture[CartId]("cartId") / "lines" / Capture[OrderLineId]("lineId"))
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Remove a cart line")
      .handler(req => ok(facade.removeCartLine(req.fields.cartId, req.fields.lineId))),
    HttpRoute.deleteRaw(base / "carts" / Capture[CartId]("cartId") / "lines")
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Remove all cart lines")
      .handler(req => ok(facade.clearCart(req.fields.cartId))),
    HttpRoute.putRaw(base / "carts" / Capture[CartId]("cartId") / "promo")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ApplyPromoRequest])
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Apply a promo code")
      .handler(req => ok(facade.applyCartPromo(req.fields.cartId, req.fields.body.code, bearerToken(req.fields.authorization)))),
    HttpRoute.deleteRaw(base / "carts" / Capture[CartId]("cartId") / "promo")
      .response(_.bodyJson[CartView]).apiErrors
      .doc(Tags.Cart, "Remove the applied promo code")
      .handler(req => ok(facade.removeCartPromo(req.fields.cartId))),
    // ── Checkout
    HttpRoute.postRaw(base / "checkout" / "options")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CheckoutOptionsRequest])
      .response(_.bodyJson[CheckoutOptionsView]).apiErrors
      .doc(Tags.Checkout, "Delivery & payment options with prefill")
      .handler { req =>
        val r = req.fields.body
        ok(facade.getCheckoutOptions(r.source, r.currency, bearerToken(req.fields.authorization), r.promoCode))
      },
    HttpRoute.postRaw(base / "checkout" / "preview")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CheckoutPreviewRequest])
      .response(_.bodyJson[CheckoutPreview]).apiErrors
      .doc(Tags.Checkout, "Totals preview for the selected delivery option")
      .handler { req =>
        val r = req.fields.body
        ok(facade.previewCheckout(r.source, r.currency, r.deliveryChoice, bearerToken(req.fields.authorization), r.promoCode))
      },
    HttpRoute.postRaw(base / "checkout").request(_.bodyJson[SubmitCheckoutCmd])
      .response(_.bodyJson[CheckoutConfirmation]).apiErrors
      .doc(Tags.Checkout, "Place the order")
      .handler(req => ok(facade.submitCheckout(req.fields.body))),
    // ── My account
    HttpRoute.getRaw(base / "me" / "orders").request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[OrderView]]).apiErrors
      .doc(Tags.Me, "Order history of the signed-in customer")
      .handler(req => required(req.fields.authorization).map(tok => ok(facade.listMyOrders(tok)))),
    HttpRoute.putRaw(base / "me" / "addresses")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[UpdateAddressesRequest])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Me, "Update billing & shipping addresses")
      .handler { req =>
        val r = req.fields.body
        required(req.fields.authorization).map(tok =>
          ok(facade.updateMyAddresses(tok, r.billingAddress, r.shippingAddresses, r.defaultShippingIndex))
        )
      },
    HttpRoute.getRaw(base / "me" / "orders" / Capture[OrderId]("orderId") / "payment-instructions")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[PaymentInstructions]).apiErrors
      .doc(Tags.Me, "Bank transfer / QR payment instructions")
      .handler(req => required(req.fields.authorization).map(tok => ok(facade.getOrderPaymentInstructions(tok, req.fields.orderId)))),
    HttpRoute.getRaw(base / "me" / "orders" / Capture[OrderId]("orderId") / "artwork")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[ArtworkSubmissionView]]).apiErrors
      .doc(Tags.Me, "Artwork attached to my order")
      .handler(req => required(req.fields.authorization).map(tok => ok(facade.listOrderArtworks(tok, req.fields.orderId)))),
    HttpRoute.postRaw(base / "me" / "orders" / Capture[OrderId]("orderId") / "artwork")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[OrderLineArtworkRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Me, "Upload artwork for an order line (or a new revision)")
      .handler { req =>
        val r = req.fields.body
        required(req.fields.authorization).map(tok =>
          ok(facade.uploadOrderLineArtwork(tok, req.fields.orderId, r.lineIndex, r.componentKey, r.payload))
        )
      },
    HttpRoute.postRaw(base / "me" / "orders" / Capture[OrderId]("orderId") / "artwork" / Capture[ArtworkId]("artworkId") / "revisions")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ArtworkUploadPayload])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Me, "Upload a new artwork revision")
      .handler { req =>
        required(req.fields.authorization).map(tok =>
          ok(facade.reuploadOrderArtworkPayload(tok, req.fields.orderId, req.fields.artworkId, req.fields.body))
        )
      },
    HttpRoute.postRaw(base / "me" / "orders" / Capture[OrderId]("orderId") / "artwork" / Capture[ArtworkId]("artworkId") / "reupload")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ReuploadArtworkRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Me, "Re-upload artwork (metadata only)")
      .handler { req =>
        val r = req.fields.body
        required(req.fields.authorization).map(tok =>
          ok(facade.reuploadOrderArtwork(tok, req.fields.orderId, req.fields.artworkId, r.fileName, r.resolutionDpi, r.bleedMm, r.colorSpace))
        )
      },
    // ── Draft artwork (product builder, before checkout)
    HttpRoute.postRaw(base / "artwork" / "drafts").request(_.bodyJson[DraftArtworkRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Upload draft artwork with automated preflight")
      .handler { req =>
        val r = req.fields.body
        ok(facade.uploadDraftArtwork(r.sessionId, r.componentKey, r.expectedTrimSize, r.expectedPageCount, r.payload))
      },
    HttpRoute.deleteRaw(base / "artwork" / "drafts" / Capture[ArtworkId]("artworkId"))
      .response(_.status(HttpStatus.NoContent)).apiErrors
      .doc(Tags.Artwork, "Discard a draft artwork")
      .handler(req => run(facade.deleteDraftArtwork(req.fields.artworkId)).andThen(HttpResponse.noContent)),
  )

object StorefrontRoutes:
  private[http] val base: HttpPath[Any] = api / "storefront"

  /** Error envelopes — one concrete type per HTTP status (kyo-http maps error types to statuses). */
  sealed trait Failure
  final case class BadRequest(message: String, error: StorefrontError)   extends Failure derives Schema
  final case class Unauthorized(message: String, error: StorefrontError) extends Failure derives Schema
  final case class Forbidden(message: String, error: StorefrontError)    extends Failure derives Schema
  final case class NotFound(message: String, error: StorefrontError)     extends Failure derives Schema
  final case class Conflict(message: String, error: StorefrontError)     extends Failure derives Schema

  object Failure:
    def apply(e: StorefrontError): Failure =
      mpbuilder.api.ApiConventions.statusOf(e) match
        case 401 => Unauthorized(e.message, e)
        case 403 => Forbidden(e.message, e)
        case 404 => NotFound(e.message, e)
        case 409 => Conflict(e.message, e)
        case _   => BadRequest(e.message, e)

  extension [In, Out, E](route: HttpRoute[In, Out, E])
    private inline def apiErrors: HttpRoute[In, Out, E | Failure] =
      route
        .error[BadRequest](HttpStatus.BadRequest)
        .error[Unauthorized](HttpStatus.Unauthorized)
        .error[Forbidden](HttpStatus.Forbidden)
        .error[NotFound](HttpStatus.NotFound)
        .error[Conflict](HttpStatus.Conflict)

  private given HttpCodec[CartId]      = opaqueCodec(CartId(_), _.value)
  private given HttpCodec[OrderId]     = opaqueCodec(OrderId(_), _.value)
  private given HttpCodec[OrderLineId] = opaqueCodec(OrderLineId(_), _.value)
  private given HttpCodec[ArtworkId]   = opaqueCodec(ArtworkId(_), _.value)

  /** Public constructor for composition roots: routes over any `StorefrontFacade`. */
  def apply(facade: StorefrontFacade): StorefrontRoutes = new StorefrontRoutes(facade)
