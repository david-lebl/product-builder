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
  *   - Static / backend-less demo wires `BackofficeClient.fromZio(...)` backed by `SampleShowcaseRuntime`
  *   - Future REST / Tapir / GraphQL / Kyo UI server-push integrations simply provide another `BackofficeClient`
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

  /** Lifts any ZIO `BackofficeFacade` into a pure Kyo `BackofficeClient` using `kyo-zio`. */
  def fromZio(facade: BackofficeFacade): BackofficeClient = new BackofficeClient:

    private inline def liftUIO[A](zioEff: => zio.UIO[A]): A < Async =
      Abort.run[Nothing](ZIOs.get(zioEff)).map(_.getOrThrow)

    private inline def liftIO[A](zioEff: => zio.IO[BackofficeError, A]): A < (Abort[BackofficeError] & Async) =
      ZIOs.get(zioEff)

    override def loginStaff(email: EmailAddress, rawPassword: NonEmptyString) =
      liftIO(facade.loginStaff(email, rawPassword))

    override def validateStaffSession(token: SessionToken) =
      liftIO(facade.validateStaffSession(token))

    override def logoutStaff(token: SessionToken) =
      liftUIO(facade.logoutStaff(token))

    override def getDashboardSummary(token: SessionToken) =
      liftIO(facade.getDashboardSummary(token))

    override def listOrders(token: SessionToken, filter: OrderFilter) =
      liftIO(facade.listOrders(token, filter))

    override def getOrder(token: SessionToken, orderId: OrderId) =
      liftIO(facade.getOrder(token, orderId))

    override def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
      liftIO(facade.acceptOrder(token, orderId, note))

    override def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString) =
      liftIO(facade.putOrderOnHold(token, orderId, reason))

    override def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
      liftIO(facade.resumeOrderFromHold(token, orderId, note))

    override def advanceOrderStage(token: SessionToken, orderId: OrderId, target: OrderStatus, note: Option[NonEmptyString]) =
      liftIO(facade.advanceOrderStage(token, orderId, target, note))

    override def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]) =
      liftIO(facade.confirmOrderPayment(token, orderId, note))

    override def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString) =
      liftIO(facade.cancelOrder(token, orderId, reason))

    override def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString) =
      liftIO(facade.addOrderInternalNote(token, orderId, text))

    override def listOrderArtwork(token: SessionToken, orderId: OrderId) =
      liftIO(facade.listOrderArtwork(token, orderId))

    override def approveArtwork(token: SessionToken, artworkId: ArtworkId, overrideFailures: Boolean) =
      liftIO(facade.approveArtwork(token, artworkId, overrideFailures))

    override def requestArtworkChanges(token: SessionToken, artworkId: ArtworkId, notes: NonEmptyString) =
      liftIO(facade.requestArtworkChanges(token, artworkId, notes))

    override def getCatalog(token: SessionToken) =
      liftIO(facade.getCatalog(token))

    override def getRuleset(token: SessionToken) =
      liftIO(facade.getRuleset(token))

    override def upsertCategory(token: SessionToken, category: ProductCategory) =
      liftIO(facade.upsertCategory(token, category))

    override def deleteCategory(token: SessionToken, id: CategoryId) =
      liftIO(facade.deleteCategory(token, id))

    override def upsertMaterial(token: SessionToken, material: Material) =
      liftIO(facade.upsertMaterial(token, material))

    override def deleteMaterial(token: SessionToken, id: MaterialId) =
      liftIO(facade.deleteMaterial(token, id))

    override def upsertFinish(token: SessionToken, finish: Finish) =
      liftIO(facade.upsertFinish(token, finish))

    override def deleteFinish(token: SessionToken, id: FinishId) =
      liftIO(facade.deleteFinish(token, id))

    override def upsertPrintingMethod(token: SessionToken, method: PrintingMethod) =
      liftIO(facade.upsertPrintingMethod(token, method))

    override def deletePrintingMethod(token: SessionToken, id: PrintingMethodId) =
      liftIO(facade.deletePrintingMethod(token, id))

    override def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing) =
      liftIO(facade.upsertShowcaseListing(token, listing))

    override def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId) =
      liftIO(facade.deleteShowcaseListing(token, categoryId))

    override def upsertRule(token: SessionToken, rule: CompatibilityRule) =
      liftIO(facade.upsertRule(token, rule))

    override def deleteRule(token: SessionToken, id: RuleId) =
      liftIO(facade.deleteRule(token, id))

    override def listPricelists(token: SessionToken) =
      liftIO(facade.listPricelists(token))

    override def savePricelist(token: SessionToken, pricelist: Pricelist) =
      liftIO(facade.savePricelist(token, pricelist))

    override def listAgreements(token: SessionToken) =
      liftIO(facade.listAgreements(token))

    override def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement) =
      liftIO(facade.saveAgreement(token, agreement))

    override def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency) =
      liftIO(facade.deleteAgreement(token, target, currency))

    override def listPromoCodes(token: SessionToken) =
      liftIO(facade.listPromoCodes(token))

    override def savePromoCode(token: SessionToken, promo: PromoCode) =
      liftIO(facade.savePromoCode(token, promo))

    override def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean) =
      liftIO(facade.setPromoCodeActive(token, id, active))

    override def deletePromoCode(token: SessionToken, id: DiscountCodeId) =
      liftIO(facade.deletePromoCode(token, id))

    override def listCostSheets(token: SessionToken) =
      liftIO(facade.listCostSheets(token))

    override def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet) =
      liftIO(facade.saveCostSheet(token, sheet))

    override def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd) =
      liftIO(facade.simulateMargin(token, cmd))

    override def listCustomers(token: SessionToken, filter: CustomerFilter) =
      liftIO(facade.listCustomers(token, filter))

    override def getCustomerDetail(token: SessionToken, id: CustomerId) =
      liftIO(facade.getCustomerDetail(token, id))

    override def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd) =
      liftIO(facade.registerCustomer(token, cmd))

    override def approveCustomer(token: SessionToken, id: CustomerId) =
      liftIO(facade.approveCustomer(token, id))

    override def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString) =
      liftIO(facade.suspendCustomer(token, id, reason))

    override def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString) =
      liftIO(facade.deactivateCustomer(token, id, reason))

    override def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier) =
      liftIO(facade.changeCustomerTier(token, id, tier))

    override def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms) =
      liftIO(facade.grantCustomerPaymentTerms(token, id, terms))

    override def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd) =
      liftIO(facade.updateCustomerProfile(token, cmd))

    override def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString) =
      liftIO(facade.addCustomerNote(token, id, text))

    override def listStaffMembers(token: SessionToken) =
      liftIO(facade.listStaffMembers(token))

    override def createStaffMember(token: SessionToken, cmd: CreateStaffCmd) =
      liftIO(facade.createStaffMember(token, cmd))

    override def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]) =
      liftIO(facade.updateStaffRoles(token, employeeId, roles))

    override def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean) =
      liftIO(facade.setStaffActive(token, employeeId, isActive))

    override def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString) =
      liftIO(facade.resetStaffPassword(token, email, newRawPassword))

    override def getStoreDeliverySettings(token: SessionToken) =
      liftIO(facade.getStoreDeliverySettings(token))

    override def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation) =
      liftIO(facade.upsertPickupLocation(token, loc))

    override def deletePickupLocation(token: SessionToken, id: DeliveryOptionId) =
      liftIO(facade.deletePickupLocation(token, id))

    override def upsertCourierOption(token: SessionToken, opt: CourierServiceOption) =
      liftIO(facade.upsertCourierOption(token, opt))

    override def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId) =
      liftIO(facade.deleteCourierOption(token, currency, id))

    override def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig) =
      liftIO(facade.upsertBankAccount(token, currency, account))
