package mpbuilder.storefront
package http

import api.StorefrontEndpoints as E
import sttp.tapir.ztapir.*

/** Inbound HTTP adapter: binds the pure Storefront Tapir contracts to `StorefrontFacade`. */
final class StorefrontRoutes private[storefront] (facade: StorefrontFacade):

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    E.getCatalog.zServerLogic(_ => facade.getCatalog),
    E.listShowcases.zServerLogic(_ => facade.listShowcaseListings),
    E.compatibleFinishes.zServerLogic(r =>
      facade.compatibleFinishes(r.categoryId, r.componentKey, r.materialId, r.printingMethodId, r.alreadySelectedFinishIds)
    ),
    E.finishParameterLimits.zServerLogic(r =>
      facade.finishParameterLimits(r.categoryId, r.componentKey, r.materialId, r.printingMethodId)
    ),
    E.configureAndQuote.zServerLogic((tok, r) => facade.configureAndQuote(r.cmd, r.currency, r.speed, tok, r.promoCode, r.lang)),
    E.pricePreset.zServerLogic((tok, r) =>
      facade.pricePreset(r.categoryId, r.presetId, r.currency, r.speed, r.specsOverride, tok, r.lang)
    ),
    // ── auth
    E.loginWithPassword.zServerLogic(r => facade.loginWithPassword(r.email, r.password)),
    E.requestOtp.zServerLogic(r => facade.requestCustomerOtp(r.identifier)),
    E.verifyOtp.zServerLogic(r => facade.verifyOtp(r.challengeId, r.code)),
    E.currentCustomer.zServerLogic(tok => facade.currentCustomer(tok)),
    E.logout.zServerLogic(tok => facade.logout(tok)),
    // ── cart
    E.createCart.zServerLogic((tok, r) => facade.createCart(r.cartId, r.currency, tok)),
    E.getCart.zServerLogic(id => facade.getCart(id)),
    E.addToCart.zServerLogic((id, tok, r) =>
      facade.addConfiguredProductToCart(id, r.cmd, r.currency, r.speed, r.copies, tok, r.artworkIds)
    ),
    E.updateCartLineCopies.zServerLogic((id, line, r) => facade.updateCartLineCopies(id, line, r.copies)),
    E.removeCartLine.zServerLogic((id, line) => facade.removeCartLine(id, line)),
    E.clearCart.zServerLogic(id => facade.clearCart(id)),
    E.applyCartPromo.zServerLogic((id, tok, r) => facade.applyCartPromo(id, r.code, tok)),
    E.removeCartPromo.zServerLogic(id => facade.removeCartPromo(id)),
    // ── checkout
    E.checkoutOptions.zServerLogic((tok, r) => facade.getCheckoutOptions(r.source, r.currency, tok, r.promoCode)),
    E.previewCheckout.zServerLogic((tok, r) =>
      facade.previewCheckout(r.source, r.currency, r.deliveryChoice, tok, r.promoCode)
    ),
    E.submitCheckout.zServerLogic(cmd => facade.submitCheckout(cmd)),
    // ── my account
    E.listMyOrders.zServerLogic(tok => facade.listMyOrders(tok)),
    E.updateMyAddresses.zServerLogic((tok, r) =>
      facade.updateMyAddresses(tok, r.billingAddress, r.shippingAddresses, r.defaultShippingIndex)
    ),
    E.paymentInstructions.zServerLogic((orderId, tok) => facade.getOrderPaymentInstructions(tok, orderId)),
    E.listOrderArtworks.zServerLogic((orderId, tok) => facade.listOrderArtworks(tok, orderId)),
    E.uploadOrderLineArtwork.zServerLogic((orderId, tok, r) =>
      facade.uploadOrderLineArtwork(tok, orderId, r.lineIndex, r.componentKey, r.payload)
    ),
    E.reuploadOrderArtworkPayload.zServerLogic((orderId, artworkId, tok, payload) =>
      facade.reuploadOrderArtworkPayload(tok, orderId, artworkId, payload)
    ),
    E.reuploadOrderArtwork.zServerLogic((orderId, artworkId, tok, r) =>
      facade.reuploadOrderArtwork(tok, orderId, artworkId, r.fileName, r.resolutionDpi, r.bleedMm, r.colorSpace)
    ),
    // ── draft artwork
    E.uploadDraftArtwork.zServerLogic(r =>
      facade.uploadDraftArtwork(r.sessionId, r.componentKey, r.expectedTrimSize, r.expectedPageCount, r.payload)
    ),
    E.deleteDraftArtwork.zServerLogic(id => facade.deleteDraftArtwork(id)),
  )

object StorefrontRoutes:
  /** Public constructor for composition roots: routes over any `StorefrontFacade`. */
  def apply(facade: StorefrontFacade): StorefrontRoutes = new StorefrontRoutes(facade)
