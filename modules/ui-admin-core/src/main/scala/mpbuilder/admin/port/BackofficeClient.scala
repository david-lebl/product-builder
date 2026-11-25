package mpbuilder.admin.port

import kyo.*
import mpbuilder.artwork.*
import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** Pure Kyo client port for the Backoffice / Admin UI.
  *
  * Completely decoupled from the transport layer:
  *   - Static / backend-less demo wires `BackofficeClient.fromFacade(...)` backed by `SampleShowcaseRuntime`
  *   - HTTP (`HttpClients.backoffice`) / GraphQL / Kyo UI server-push integrations simply provide another `BackofficeClient`
  */
trait BackofficeClient:

  // ── 1. Staff Authentication ────────────────────────────────────────────────
  def loginStaff(email: EmailAddress, rawPassword: NonEmptyString): StaffAuthResult < (Abort[BackofficeError] & Async)
  def validateStaffSession(token: SessionToken): StaffAuthResult < (Abort[BackofficeError] & Async)
  def logoutStaff(token: SessionToken): Unit < Async

  // ── 2. Dashboard Summary ───────────────────────────────────────────────────
  def getDashboardSummary(token: SessionToken): BackofficeDashboardSummary < (Abort[BackofficeError] & Async)

  // ── 3. Order Intake & Manual Lifecycle ─────────────────────────────────────
  def listOrders(token: SessionToken, filter: OrderFilter = OrderFilter()): List[OrderView] < (Abort[BackofficeError] & Async)
  def getOrder(token: SessionToken, orderId: OrderId): OrderView < (Abort[BackofficeError] & Async)
  def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): OrderView < (Abort[BackofficeError] & Async)
  def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString): OrderView < (Abort[BackofficeError] & Async)
  def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): OrderView < (Abort[BackofficeError] & Async)
  def advanceOrderStage(
      token: SessionToken,
      orderId: OrderId,
      target: OrderStatus,
      note: Option[NonEmptyString] = None,
  ): OrderView < (Abort[BackofficeError] & Async)
  def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): OrderView < (Abort[BackofficeError] & Async)
  def markOrderPaymentStatus(
      token: SessionToken,
      orderId: OrderId,
      status: PaymentStatus,
      note: Option[NonEmptyString] = None,
  ): OrderView < (Abort[BackofficeError] & Async)
  def editOrderLine(token: SessionToken, cmd: EditOrderLineCmd): OrderView < (Abort[BackofficeError] & Async)
  def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString): OrderView < (Abort[BackofficeError] & Async)
  def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString): OrderView < (Abort[BackofficeError] & Async)

  // ── 4. Artwork Review ──────────────────────────────────────────────────────
  def listOrderArtwork(token: SessionToken, orderId: OrderId): List[ArtworkSubmissionView] < (Abort[BackofficeError] & Async)
  def approveArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      overrideFailures: Boolean = false,
  ): ArtworkSubmissionView < (Abort[BackofficeError] & Async)
  def requestArtworkChanges(
      token: SessionToken,
      artworkId: ArtworkId,
      notes: NonEmptyString,
  ): ArtworkSubmissionView < (Abort[BackofficeError] & Async)
  def rejectArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      reason: NonEmptyString,
  ): ArtworkSubmissionView < (Abort[BackofficeError] & Async)
  def uploadStaffArtworkRevision(
      token: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: StaffArtworkUploadPayload,
  ): ArtworkSubmissionView < (Abort[BackofficeError] & Async)
  def uploadStaffOrderLineArtwork(
      token: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: StaffArtworkUploadPayload,
  ): ArtworkSubmissionView < (Abort[BackofficeError] & Async)
  def rerunArtworkPreflight(
      token: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
  ): ArtworkSubmissionView < (Abort[BackofficeError] & Async)

  // ── 5. Catalog Management ──────────────────────────────────────────────────
  def getCatalog(token: SessionToken): ProductCatalog < (Abort[BackofficeError] & Async)
  def getRuleset(token: SessionToken): CompatibilityRuleset < (Abort[BackofficeError] & Async)
  def upsertCategory(token: SessionToken, category: ProductCategory): ProductCatalog < (Abort[BackofficeError] & Async)
  def deleteCategory(token: SessionToken, id: CategoryId): ProductCatalog < (Abort[BackofficeError] & Async)
  def upsertMaterial(token: SessionToken, material: Material): ProductCatalog < (Abort[BackofficeError] & Async)
  def deleteMaterial(token: SessionToken, id: MaterialId): ProductCatalog < (Abort[BackofficeError] & Async)
  def upsertFinish(token: SessionToken, finish: Finish): ProductCatalog < (Abort[BackofficeError] & Async)
  def deleteFinish(token: SessionToken, id: FinishId): ProductCatalog < (Abort[BackofficeError] & Async)
  def upsertPrintingMethod(token: SessionToken, method: PrintingMethod): ProductCatalog < (Abort[BackofficeError] & Async)
  def deletePrintingMethod(token: SessionToken, id: PrintingMethodId): ProductCatalog < (Abort[BackofficeError] & Async)
  def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing): ProductCatalog < (Abort[BackofficeError] & Async)
  def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId): ProductCatalog < (Abort[BackofficeError] & Async)
  def upsertRule(token: SessionToken, rule: CompatibilityRule): CompatibilityRuleset < (Abort[BackofficeError] & Async)
  def deleteRule(token: SessionToken, id: RuleId): CompatibilityRuleset < (Abort[BackofficeError] & Async)

  // ── 6. Pricing, Agreements, Promos & Margin Simulation ─────────────────────
  def listPricelists(token: SessionToken): List[Pricelist] < (Abort[BackofficeError] & Async)
  def savePricelist(token: SessionToken, pricelist: Pricelist): Pricelist < (Abort[BackofficeError] & Async)
  def listAgreements(token: SessionToken): List[CustomerPriceAgreement] < (Abort[BackofficeError] & Async)
  def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement): CustomerPriceAgreement < (Abort[BackofficeError] & Async)
  def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency): Unit < (Abort[BackofficeError] & Async)
  def listPromoCodes(token: SessionToken): List[PromoCode] < (Abort[BackofficeError] & Async)
  def savePromoCode(token: SessionToken, promo: PromoCode): PromoCode < (Abort[BackofficeError] & Async)
  def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean): PromoCode < (Abort[BackofficeError] & Async)
  def deletePromoCode(token: SessionToken, id: DiscountCodeId): Unit < (Abort[BackofficeError] & Async)
  def listCostSheets(token: SessionToken): List[ProductionCostSheet] < (Abort[BackofficeError] & Async)
  def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet): ProductionCostSheet < (Abort[BackofficeError] & Async)
  def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd): MarginSimulationResult < (Abort[BackofficeError] & Async)

  // ── 7. Customer Management ─────────────────────────────────────────────────
  def listCustomers(token: SessionToken, filter: CustomerFilter = CustomerFilter()): List[CustomerView] < (Abort[BackofficeError] & Async)
  def getCustomerDetail(token: SessionToken, id: CustomerId): CustomerDetailView < (Abort[BackofficeError] & Async)
  def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd): CustomerView < (Abort[BackofficeError] & Async)
  def approveCustomer(token: SessionToken, id: CustomerId): CustomerView < (Abort[BackofficeError] & Async)
  def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): CustomerView < (Abort[BackofficeError] & Async)
  def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): CustomerView < (Abort[BackofficeError] & Async)
  def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier): CustomerView < (Abort[BackofficeError] & Async)
  def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms): CustomerView < (Abort[BackofficeError] & Async)
  def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd): CustomerView < (Abort[BackofficeError] & Async)
  def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString): CustomerView < (Abort[BackofficeError] & Async)

  // ── 8. Staff Directory & Store Delivery / Bank Settings ────────────────────
  def listStaffMembers(token: SessionToken): List[StaffMemberView] < (Abort[BackofficeError] & Async)
  def createStaffMember(token: SessionToken, cmd: CreateStaffCmd): StaffMemberView < (Abort[BackofficeError] & Async)
  def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]): StaffMemberView < (Abort[BackofficeError] & Async)
  def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean): StaffMemberView < (Abort[BackofficeError] & Async)
  def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString): Unit < (Abort[BackofficeError] & Async)

  def getStoreDeliverySettings(token: SessionToken): StoreDeliverySettingsView < (Abort[BackofficeError] & Async)
  def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation): List[ShopPickupLocation] < (Abort[BackofficeError] & Async)
  def deletePickupLocation(token: SessionToken, id: DeliveryOptionId): List[ShopPickupLocation] < (Abort[BackofficeError] & Async)
  def upsertCourierOption(token: SessionToken, opt: CourierServiceOption): List[CourierServiceOption] < (Abort[BackofficeError] & Async)
  def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId): List[CourierServiceOption] < (Abort[BackofficeError] & Async)
  def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig): Map[Currency, BankAccountConfig] < (Abort[BackofficeError] & Async)

object BackofficeClient:

  /** Exposes a (Kyo) `BackofficeFacade` — in-memory or HTTP-backed — as the UI's `BackofficeClient` port. */
  def fromFacade(facade: BackofficeFacade): BackofficeClient = new BackofficeClient:



    override def loginStaff(email: EmailAddress, rawPassword: NonEmptyString) =
      facade.loginStaff(email, rawPassword)

    override def validateStaffSession(token: SessionToken) =
      facade.validateStaffSession(token)

    override def logoutStaff(token: SessionToken) =
      facade.logoutStaff(token)

    override def getDashboardSummary(token: SessionToken) =
      facade.getDashboardSummary(token)

    override def listOrders(token: SessionToken, filter: OrderFilter) =
      facade.listOrders(token, filter)

    override def getOrder(token: SessionToken, orderId: OrderId) =
      facade.getOrder(token, orderId)

    override def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
      facade.acceptOrder(token, orderId, note)

    override def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString) =
      facade.putOrderOnHold(token, orderId, reason)

    override def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
      facade.resumeOrderFromHold(token, orderId, note)

    override def advanceOrderStage(token: SessionToken, orderId: OrderId, target: OrderStatus, note: Option[NonEmptyString]) =
      facade.advanceOrderStage(token, orderId, target, note)

    override def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
      facade.confirmOrderPayment(token, orderId, note)

    override def markOrderPaymentStatus(token: SessionToken, orderId: OrderId, status: PaymentStatus, note: Option[NonEmptyString]) =
      facade.markOrderPaymentStatus(token, orderId, status, note)

    override def editOrderLine(token: SessionToken, cmd: EditOrderLineCmd) =
      facade.editOrderLine(token, cmd)

    override def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString) =
      facade.cancelOrder(token, orderId, reason)

    override def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString) =
      facade.addOrderInternalNote(token, orderId, text)

    override def listOrderArtwork(token: SessionToken, orderId: OrderId) =
      facade.listOrderArtwork(token, orderId)

    override def approveArtwork(token: SessionToken, artworkId: ArtworkId, overrideFailures: Boolean) =
      facade.approveArtwork(token, artworkId, overrideFailures)

    override def requestArtworkChanges(token: SessionToken, artworkId: ArtworkId, notes: NonEmptyString) =
      facade.requestArtworkChanges(token, artworkId, notes)

    override def rejectArtwork(token: SessionToken, artworkId: ArtworkId, reason: NonEmptyString) =
      facade.rejectArtwork(token, artworkId, reason)

    override def uploadStaffArtworkRevision(
        token: SessionToken,
        orderId: OrderId,
        artworkId: ArtworkId,
        payload: StaffArtworkUploadPayload,
    ) =
      facade.uploadStaffArtworkRevision(token, orderId, artworkId, payload)

    override def uploadStaffOrderLineArtwork(
        token: SessionToken,
        orderId: OrderId,
        lineIndex: Int,
        componentKey: ComponentKey,
        payload: StaffArtworkUploadPayload,
    ) =
      facade.uploadStaffOrderLineArtwork(token, orderId, lineIndex, componentKey, payload)

    override def rerunArtworkPreflight(token: SessionToken, orderId: OrderId, artworkId: ArtworkId) =
      facade.rerunArtworkPreflight(token, orderId, artworkId)

    override def getCatalog(token: SessionToken) =
      facade.getCatalog(token)

    override def getRuleset(token: SessionToken) =
      facade.getRuleset(token)

    override def upsertCategory(token: SessionToken, category: ProductCategory) =
      facade.upsertCategory(token, category)

    override def deleteCategory(token: SessionToken, id: CategoryId) =
      facade.deleteCategory(token, id)

    override def upsertMaterial(token: SessionToken, material: Material) =
      facade.upsertMaterial(token, material)

    override def deleteMaterial(token: SessionToken, id: MaterialId) =
      facade.deleteMaterial(token, id)

    override def upsertFinish(token: SessionToken, finish: Finish) =
      facade.upsertFinish(token, finish)

    override def deleteFinish(token: SessionToken, id: FinishId) =
      facade.deleteFinish(token, id)

    override def upsertPrintingMethod(token: SessionToken, method: PrintingMethod) =
      facade.upsertPrintingMethod(token, method)

    override def deletePrintingMethod(token: SessionToken, id: PrintingMethodId) =
      facade.deletePrintingMethod(token, id)

    override def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing) =
      facade.upsertShowcaseListing(token, listing)

    override def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId) =
      facade.deleteShowcaseListing(token, categoryId)

    override def upsertRule(token: SessionToken, rule: CompatibilityRule) =
      facade.upsertRule(token, rule)

    override def deleteRule(token: SessionToken, id: RuleId) =
      facade.deleteRule(token, id)

    override def listPricelists(token: SessionToken) =
      facade.listPricelists(token)

    override def savePricelist(token: SessionToken, pricelist: Pricelist) =
      facade.savePricelist(token, pricelist)

    override def listAgreements(token: SessionToken) =
      facade.listAgreements(token)

    override def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement) =
      facade.saveAgreement(token, agreement)

    override def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency) =
      facade.deleteAgreement(token, target, currency)

    override def listPromoCodes(token: SessionToken) =
      facade.listPromoCodes(token)

    override def savePromoCode(token: SessionToken, promo: PromoCode) =
      facade.savePromoCode(token, promo)

    override def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean) =
      facade.setPromoCodeActive(token, id, active)

    override def deletePromoCode(token: SessionToken, id: DiscountCodeId) =
      facade.deletePromoCode(token, id)

    override def listCostSheets(token: SessionToken) =
      facade.listCostSheets(token)

    override def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet) =
      facade.saveCostSheet(token, sheet)

    override def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd) =
      facade.simulateMargin(token, cmd)

    override def listCustomers(token: SessionToken, filter: CustomerFilter) =
      facade.listCustomers(token, filter)

    override def getCustomerDetail(token: SessionToken, id: CustomerId) =
      facade.getCustomerDetail(token, id)

    override def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd) =
      facade.registerCustomer(token, cmd)

    override def approveCustomer(token: SessionToken, id: CustomerId) =
      facade.approveCustomer(token, id)

    override def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString) =
      facade.suspendCustomer(token, id, reason)

    override def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString) =
      facade.deactivateCustomer(token, id, reason)

    override def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier) =
      facade.changeCustomerTier(token, id, tier)

    override def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms) =
      facade.grantCustomerPaymentTerms(token, id, terms)

    override def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd) =
      facade.updateCustomerProfile(token, cmd)

    override def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString) =
      facade.addCustomerNote(token, id, text)

    override def listStaffMembers(token: SessionToken) =
      facade.listStaffMembers(token)

    override def createStaffMember(token: SessionToken, cmd: CreateStaffCmd) =
      facade.createStaffMember(token, cmd)

    override def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]) =
      facade.updateStaffRoles(token, employeeId, roles)

    override def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean) =
      facade.setStaffActive(token, employeeId, isActive)

    override def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString) =
      facade.resetStaffPassword(token, email, newRawPassword)

    override def getStoreDeliverySettings(token: SessionToken) =
      facade.getStoreDeliverySettings(token)

    override def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation) =
      facade.upsertPickupLocation(token, loc)

    override def deletePickupLocation(token: SessionToken, id: DeliveryOptionId) =
      facade.deletePickupLocation(token, id)

    override def upsertCourierOption(token: SessionToken, opt: CourierServiceOption) =
      facade.upsertCourierOption(token, opt)

    override def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId) =
      facade.deleteCourierOption(token, currency, id)

    override def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig) =
      facade.upsertBankAccount(token, currency, account)
