package mpbuilder.backoffice
package http

import api.BackofficeEndpoints as E
import sttp.tapir.ztapir.*

/** Inbound HTTP adapter: binds the pure Backoffice Tapir contracts to `BackofficeFacade` (RBAC inside). */
final class BackofficeRoutes private[backoffice] (f: BackofficeFacade):

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    E.login.zServerLogic(r => f.loginStaff(r.email, r.password)),
    E.validateSession.zServerLogic(t => f.validateStaffSession(t)),
    E.logout.zServerLogic(t => f.logoutStaff(t)),
    E.dashboard.zServerLogic(t => f.getDashboardSummary(t)),
    // ── orders
    E.listOrders.zServerLogic((t, filter) => f.listOrders(t, filter)),
    E.getOrder.zServerLogic((t, id) => f.getOrder(t, id)),
    E.acceptOrder.zServerLogic((t, id, r) => f.acceptOrder(t, id, r.note)),
    E.putOnHold.zServerLogic((t, id, r) => f.putOrderOnHold(t, id, r.reason)),
    E.resumeOrder.zServerLogic((t, id, r) => f.resumeOrderFromHold(t, id, r.note)),
    E.advanceStage.zServerLogic((t, id, r) => f.advanceOrderStage(t, id, r.target, r.note)),
    E.confirmPayment.zServerLogic((t, id, r) => f.confirmOrderPayment(t, id, r.note)),
    E.markPaymentStatus.zServerLogic((t, id, r) => f.markOrderPaymentStatus(t, id, r.status, r.note)),
    E.editOrderLine.zServerLogic((t, cmd) => f.editOrderLine(t, cmd)),
    E.cancelOrder.zServerLogic((t, id, r) => f.cancelOrder(t, id, r.reason)),
    E.addInternalNote.zServerLogic((t, id, r) => f.addOrderInternalNote(t, id, r.text)),
    // ── artwork
    E.listOrderArtwork.zServerLogic((t, id) => f.listOrderArtwork(t, id)),
    E.approveArtwork.zServerLogic((t, id, r) => f.approveArtwork(t, id, r.overrideFailures)),
    E.requestArtworkChanges.zServerLogic((t, id, r) => f.requestArtworkChanges(t, id, r.text)),
    E.rejectArtwork.zServerLogic((t, id, r) => f.rejectArtwork(t, id, r.reason)),
    E.uploadStaffRevision.zServerLogic((t, orderId, artworkId, p) => f.uploadStaffArtworkRevision(t, orderId, artworkId, p)),
    E.uploadStaffOrderLineArtwork.zServerLogic((t, orderId, r) =>
      f.uploadStaffOrderLineArtwork(t, orderId, r.lineIndex, r.componentKey, r.payload)
    ),
    E.rerunPreflight.zServerLogic((t, orderId, artworkId) => f.rerunArtworkPreflight(t, orderId, artworkId)),
    // ── catalog
    E.getCatalog.zServerLogic(t => f.getCatalog(t)),
    E.getRuleset.zServerLogic(t => f.getRuleset(t)),
    E.upsertCategory.zServerLogic((t, x) => f.upsertCategory(t, x)),
    E.deleteCategory.zServerLogic((t, id) => f.deleteCategory(t, id)),
    E.upsertMaterial.zServerLogic((t, x) => f.upsertMaterial(t, x)),
    E.deleteMaterial.zServerLogic((t, id) => f.deleteMaterial(t, id)),
    E.upsertFinish.zServerLogic((t, x) => f.upsertFinish(t, x)),
    E.deleteFinish.zServerLogic((t, id) => f.deleteFinish(t, id)),
    E.upsertMethod.zServerLogic((t, x) => f.upsertPrintingMethod(t, x)),
    E.deleteMethod.zServerLogic((t, id) => f.deletePrintingMethod(t, id)),
    E.upsertShowcase.zServerLogic((t, x) => f.upsertShowcaseListing(t, x)),
    E.deleteShowcase.zServerLogic((t, id) => f.deleteShowcaseListing(t, id)),
    E.upsertRule.zServerLogic((t, x) => f.upsertRule(t, x)),
    E.deleteRule.zServerLogic((t, id) => f.deleteRule(t, id)),
    // ── pricing
    E.listPricelists.zServerLogic(t => f.listPricelists(t)),
    E.savePricelist.zServerLogic((t, x) => f.savePricelist(t, x)),
    E.listAgreements.zServerLogic(t => f.listAgreements(t)),
    E.saveAgreement.zServerLogic((t, x) => f.saveAgreement(t, x)),
    E.deleteAgreement.zServerLogic((t, r) => f.deleteAgreement(t, r.target, r.currency)),
    E.listPromoCodes.zServerLogic(t => f.listPromoCodes(t)),
    E.savePromoCode.zServerLogic((t, x) => f.savePromoCode(t, x)),
    E.setPromoCodeActive.zServerLogic((t, id, r) => f.setPromoCodeActive(t, id, r.active)),
    E.deletePromoCode.zServerLogic((t, id) => f.deletePromoCode(t, id)),
    E.listCostSheets.zServerLogic(t => f.listCostSheets(t)),
    E.saveCostSheet.zServerLogic((t, x) => f.saveCostSheet(t, x)),
    E.simulateMargin.zServerLogic((t, cmd) => f.simulateMargin(t, cmd)),
    // ── customers
    E.listCustomers.zServerLogic((t, filter) => f.listCustomers(t, filter)),
    E.getCustomer.zServerLogic((t, id) => f.getCustomerDetail(t, id)),
    E.registerCustomer.zServerLogic((t, cmd) => f.registerCustomer(t, cmd)),
    E.approveCustomer.zServerLogic((t, id) => f.approveCustomer(t, id)),
    E.suspendCustomer.zServerLogic((t, id, r) => f.suspendCustomer(t, id, r.reason)),
    E.deactivateCustomer.zServerLogic((t, id, r) => f.deactivateCustomer(t, id, r.reason)),
    E.changeTier.zServerLogic((t, id, r) => f.changeCustomerTier(t, id, r.tier)),
    E.grantPaymentTerms.zServerLogic((t, id, terms) => f.grantCustomerPaymentTerms(t, id, terms)),
    E.updateProfile.zServerLogic((t, cmd) => f.updateCustomerProfile(t, cmd)),
    E.addCustomerNote.zServerLogic((t, id, r) => f.addCustomerNote(t, id, r.text)),
    // ── staff & settings
    E.listStaff.zServerLogic(t => f.listStaffMembers(t)),
    E.createStaff.zServerLogic((t, cmd) => f.createStaffMember(t, cmd)),
    E.updateStaffRoles.zServerLogic((t, id, r) => f.updateStaffRoles(t, id, r.roles)),
    E.setStaffActive.zServerLogic((t, id, r) => f.setStaffActive(t, id, r.active)),
    E.resetStaffPassword.zServerLogic((t, r) => f.resetStaffPassword(t, r.email, r.newPassword)),
    E.getDeliverySettings.zServerLogic(t => f.getStoreDeliverySettings(t)),
    E.upsertPickupLocation.zServerLogic((t, x) => f.upsertPickupLocation(t, x)),
    E.deletePickupLocation.zServerLogic((t, id) => f.deletePickupLocation(t, id)),
    E.upsertCourierOption.zServerLogic((t, x) => f.upsertCourierOption(t, x)),
    E.deleteCourierOption.zServerLogic((t, cur, id) => f.deleteCourierOption(t, cur, id)),
    E.upsertBankAccount.zServerLogic((t, cur, acc) => f.upsertBankAccount(t, cur, acc)),
  )

object BackofficeRoutes:
  /** Public constructor for composition roots: routes over any `BackofficeFacade`. */
  def apply(facade: BackofficeFacade): BackofficeRoutes = new BackofficeRoutes(facade)
