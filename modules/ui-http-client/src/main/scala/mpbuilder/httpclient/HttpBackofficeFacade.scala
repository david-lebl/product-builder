package mpbuilder.httpclient

import mpbuilder.backoffice.http.api.*
import mpbuilder.backoffice.http.api.BackofficeEndpoints as E
import mpbuilder.artwork.*
import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import zio.*

/** `BackofficeFacade` implemented over HTTP (`/api/v1/admin`). RBAC is enforced server-side. */
final class HttpBackofficeFacade(http: HttpTransport) extends BackofficeFacade:

  private def run[I, O](e: sttp.tapir.PublicEndpoint[I, BackofficeError, O, Any])(in: I): IO[BackofficeError, O] =
    http.call(e)(in)

  // ── Staff auth & dashboard ─────────────────────────────────────────────────
  override def loginStaff(email: EmailAddress, rawPassword: NonEmptyString) = run(E.login)(StaffLoginRequest(email, rawPassword))
  override def validateStaffSession(token: SessionToken)                    = run(E.validateSession)(token)
  override def logoutStaff(token: SessionToken): UIO[Unit]                 = run(E.logout)(token).ignore
  override def getDashboardSummary(token: SessionToken)                    = run(E.dashboard)(token)

  // ── Orders ─────────────────────────────────────────────────────────────────
  override def listOrders(token: SessionToken, filter: OrderFilter) = run(E.listOrders)((token, filter))
  override def getOrder(token: SessionToken, orderId: OrderId)      = run(E.getOrder)((token, orderId))
  override def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
    run(E.acceptOrder)((token, orderId, NoteRequest(note)))
  override def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString) =
    run(E.putOnHold)((token, orderId, ReasonRequest(reason)))
  override def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
    run(E.resumeOrder)((token, orderId, NoteRequest(note)))
  override def advanceOrderStage(token: SessionToken, orderId: OrderId, target: OrderStatus, note: Option[NonEmptyString]) =
    run(E.advanceStage)((token, orderId, AdvanceStageRequest(target, note)))
  override def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
    run(E.confirmPayment)((token, orderId, NoteRequest(note)))
  override def markOrderPaymentStatus(token: SessionToken, orderId: OrderId, status: PaymentStatus, note: Option[NonEmptyString]) =
    run(E.markPaymentStatus)((token, orderId, PaymentStatusRequest(status, note)))
  override def editOrderLine(token: SessionToken, cmd: EditOrderLineCmd) = run(E.editOrderLine)((token, cmd))
  override def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString) =
    run(E.cancelOrder)((token, orderId, ReasonRequest(reason)))
  override def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString) =
    run(E.addInternalNote)((token, orderId, TextRequest(text)))

  // ── Artwork review ─────────────────────────────────────────────────────────
  override def listOrderArtwork(token: SessionToken, orderId: OrderId) = run(E.listOrderArtwork)((token, orderId))
  override def approveArtwork(token: SessionToken, artworkId: ArtworkId, overrideFailures: Boolean) =
    run(E.approveArtwork)((token, artworkId, ApproveArtworkRequest(overrideFailures)))
  override def requestArtworkChanges(token: SessionToken, artworkId: ArtworkId, notes: NonEmptyString) =
    run(E.requestArtworkChanges)((token, artworkId, TextRequest(notes)))
  override def rejectArtwork(token: SessionToken, artworkId: ArtworkId, reason: NonEmptyString) =
    run(E.rejectArtwork)((token, artworkId, ReasonRequest(reason)))
  override def uploadStaffArtworkRevision(token: SessionToken, orderId: OrderId, artworkId: ArtworkId, payload: StaffArtworkUploadPayload) =
    run(E.uploadStaffRevision)((token, orderId, artworkId, payload))
  override def uploadStaffOrderLineArtwork(
      token: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: StaffArtworkUploadPayload,
  ) = run(E.uploadStaffOrderLineArtwork)((token, orderId, StaffOrderLineArtworkRequest(lineIndex, componentKey, payload)))
  override def rerunArtworkPreflight(token: SessionToken, orderId: OrderId, artworkId: ArtworkId) =
    run(E.rerunPreflight)((token, orderId, artworkId))

  // ── Catalog ────────────────────────────────────────────────────────────────
  override def getCatalog(token: SessionToken)                                   = run(E.getCatalog)(token)
  override def getRuleset(token: SessionToken)                                   = run(E.getRuleset)(token)
  override def upsertCategory(token: SessionToken, category: ProductCategory)    = run(E.upsertCategory)((token, category))
  override def deleteCategory(token: SessionToken, id: CategoryId)               = run(E.deleteCategory)((token, id))
  override def upsertMaterial(token: SessionToken, material: Material)           = run(E.upsertMaterial)((token, material))
  override def deleteMaterial(token: SessionToken, id: MaterialId)               = run(E.deleteMaterial)((token, id))
  override def upsertFinish(token: SessionToken, finish: Finish)                 = run(E.upsertFinish)((token, finish))
  override def deleteFinish(token: SessionToken, id: FinishId)                   = run(E.deleteFinish)((token, id))
  override def upsertPrintingMethod(token: SessionToken, method: PrintingMethod) = run(E.upsertMethod)((token, method))
  override def deletePrintingMethod(token: SessionToken, id: PrintingMethodId)   = run(E.deleteMethod)((token, id))
  override def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing) = run(E.upsertShowcase)((token, listing))
  override def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId)   = run(E.deleteShowcase)((token, categoryId))
  override def upsertRule(token: SessionToken, rule: CompatibilityRule)          = run(E.upsertRule)((token, rule))
  override def deleteRule(token: SessionToken, id: RuleId)                       = run(E.deleteRule)((token, id))

  // ── Pricing ────────────────────────────────────────────────────────────────
  override def listPricelists(token: SessionToken)                                 = run(E.listPricelists)(token)
  override def savePricelist(token: SessionToken, pricelist: Pricelist)            = run(E.savePricelist)((token, pricelist))
  override def listAgreements(token: SessionToken)                                 = run(E.listAgreements)(token)
  override def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement) = run(E.saveAgreement)((token, agreement))
  override def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency) =
    run(E.deleteAgreement)((token, DeleteAgreementRequest(target, currency)))
  override def listPromoCodes(token: SessionToken)                       = run(E.listPromoCodes)(token)
  override def savePromoCode(token: SessionToken, promo: PromoCode)      = run(E.savePromoCode)((token, promo))
  override def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean) =
    run(E.setPromoCodeActive)((token, id, ActiveRequest(active)))
  override def deletePromoCode(token: SessionToken, id: DiscountCodeId)  = run(E.deletePromoCode)((token, id))
  override def listCostSheets(token: SessionToken)                       = run(E.listCostSheets)(token)
  override def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet) = run(E.saveCostSheet)((token, sheet))
  override def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd)  = run(E.simulateMargin)((token, cmd))

  // ── Customers ──────────────────────────────────────────────────────────────
  override def listCustomers(token: SessionToken, filter: CustomerFilter)       = run(E.listCustomers)((token, filter))
  override def getCustomerDetail(token: SessionToken, id: CustomerId)           = run(E.getCustomer)((token, id))
  override def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd)  = run(E.registerCustomer)((token, cmd))
  override def approveCustomer(token: SessionToken, id: CustomerId)             = run(E.approveCustomer)((token, id))
  override def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString) =
    run(E.suspendCustomer)((token, id, ReasonRequest(reason)))
  override def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString) =
    run(E.deactivateCustomer)((token, id, ReasonRequest(reason)))
  override def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier) =
    run(E.changeTier)((token, id, TierRequest(tier)))
  override def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms) =
    run(E.grantPaymentTerms)((token, id, terms))
  override def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd) = run(E.updateProfile)((token, cmd))
  override def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString) =
    run(E.addCustomerNote)((token, id, TextRequest(text)))

  // ── Staff & settings ───────────────────────────────────────────────────────
  override def listStaffMembers(token: SessionToken)                    = run(E.listStaff)(token)
  override def createStaffMember(token: SessionToken, cmd: CreateStaffCmd) = run(E.createStaff)((token, cmd))
  override def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]) =
    run(E.updateStaffRoles)((token, employeeId, RolesRequest(roles)))
  override def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean) =
    run(E.setStaffActive)((token, employeeId, ActiveRequest(isActive)))
  override def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString) =
    run(E.resetStaffPassword)((token, PasswordResetRequest(email, newRawPassword)))
  override def getStoreDeliverySettings(token: SessionToken)                     = run(E.getDeliverySettings)(token)
  override def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation) = run(E.upsertPickupLocation)((token, loc))
  override def deletePickupLocation(token: SessionToken, id: DeliveryOptionId)    = run(E.deletePickupLocation)((token, id))
  override def upsertCourierOption(token: SessionToken, opt: CourierServiceOption) = run(E.upsertCourierOption)((token, opt))
  override def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId) =
    run(E.deleteCourierOption)((token, currency, id))
  override def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig) =
    run(E.upsertBankAccount)((token, currency, account))
