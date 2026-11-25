package mpbuilder.httpclient

import kyo.*
import mpbuilder.artwork.*
import mpbuilder.backoffice.*
import mpbuilder.backoffice.http.api.*
import mpbuilder.backoffice.http.api.BackofficeApi.{path, seg}
import mpbuilder.backoffice.http.api.BackofficeApiCodecs.given
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** `BackofficeFacade` implemented over HTTP (`/api/v1/admin`). RBAC is enforced server-side. */
final class HttpBackofficeFacade(transport: HttpTransport, baseUrl: String) extends BackofficeFacade:

  private val http = new ApiCaller[BackofficeError](transport, baseUrl)
  import http.json

  override def loginStaff(email: EmailAddress, rawPassword: NonEmptyString): StaffAuthResult < (Async & Abort[BackofficeError]) =
    http.call[StaffAuthResult]("POST", path("auth", "login"), None, json(StaffLoginRequest(email = email, password = rawPassword)))

  override def validateStaffSession(token: SessionToken): StaffAuthResult < (Async & Abort[BackofficeError]) =
    http.call[StaffAuthResult]("GET", path("auth", "session"), Some(token), None)

  override def logoutStaff(token: SessionToken): Unit < Async =
    http.callUnit("POST", path("auth", "logout"), Some(token), None).ignore

  override def getDashboardSummary(token: SessionToken): BackofficeDashboardSummary < (Async & Abort[BackofficeError]) =
    http.call[BackofficeDashboardSummary]("GET", path("dashboard"), Some(token), None)

  override def listOrders(token: SessionToken, filter: OrderFilter): List[OrderView] < (Async & Abort[BackofficeError]) =
    http.call[List[OrderView]]("POST", path("orders", "search"), Some(token), json(filter))

  override def getOrder(token: SessionToken, orderId: OrderId): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("GET", path("orders", seg(orderId.value)), Some(token), None)

  override def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "accept"), Some(token), json(NoteRequest(note = note)))

  override def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "hold"), Some(token), json(ReasonRequest(reason = reason)))

  override def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "resume"), Some(token), json(NoteRequest(note = note)))

  override def advanceOrderStage(token: SessionToken, orderId: OrderId, target: OrderStatus, note: Option[NonEmptyString]): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "advance"), Some(token), json(AdvanceStageRequest(target = target, note = note)))

  override def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString]): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "payment", "confirm"), Some(token), json(NoteRequest(note = note)))

  override def markOrderPaymentStatus(token: SessionToken, orderId: OrderId, status: PaymentStatus, note: Option[NonEmptyString]): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "payment", "status"), Some(token), json(PaymentStatusRequest(status = status, note = note)))

  override def editOrderLine(token: SessionToken, cmd: EditOrderLineCmd): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", "line-edits"), Some(token), json(cmd))

  override def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "cancel"), Some(token), json(ReasonRequest(reason = reason)))

  override def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString): OrderView < (Async & Abort[BackofficeError]) =
    http.call[OrderView]("POST", path("orders", seg(orderId.value), "notes"), Some(token), json(TextRequest(text = text)))

  override def listOrderArtwork(token: SessionToken, orderId: OrderId): List[ArtworkSubmissionView] < (Async & Abort[BackofficeError]) =
    http.call[List[ArtworkSubmissionView]]("GET", path("orders", seg(orderId.value), "artwork"), Some(token), None)

  override def approveArtwork(token: SessionToken, artworkId: ArtworkId, overrideFailures: Boolean): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    http.call[ArtworkSubmissionView]("POST", path("artwork", seg(artworkId.value), "approve"), Some(token), json(ApproveArtworkRequest(overrideFailures = overrideFailures)))

  override def requestArtworkChanges(token: SessionToken, artworkId: ArtworkId, notes: NonEmptyString): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    http.call[ArtworkSubmissionView]("POST", path("artwork", seg(artworkId.value), "request-changes"), Some(token), json(TextRequest(text = notes)))

  override def rejectArtwork(token: SessionToken, artworkId: ArtworkId, reason: NonEmptyString): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    http.call[ArtworkSubmissionView]("POST", path("artwork", seg(artworkId.value), "reject"), Some(token), json(ReasonRequest(reason = reason)))

  override def uploadStaffArtworkRevision(token: SessionToken, orderId: OrderId, artworkId: ArtworkId, payload: StaffArtworkUploadPayload): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    http.call[ArtworkSubmissionView]("POST", path("orders", seg(orderId.value), "artwork", seg(artworkId.value), "revisions"), Some(token), json(payload))

  override def uploadStaffOrderLineArtwork(token: SessionToken, orderId: OrderId, lineIndex: Int, componentKey: ComponentKey, payload: StaffArtworkUploadPayload): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    http.call[ArtworkSubmissionView]("POST", path("orders", seg(orderId.value), "artwork"), Some(token), json(StaffOrderLineArtworkRequest(lineIndex = lineIndex, componentKey = componentKey, payload = payload)))

  override def rerunArtworkPreflight(token: SessionToken, orderId: OrderId, artworkId: ArtworkId): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    http.call[ArtworkSubmissionView]("POST", path("orders", seg(orderId.value), "artwork", seg(artworkId.value), "preflight"), Some(token), None)

  override def getCatalog(token: SessionToken): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("GET", path("catalog"), Some(token), None)

  override def getRuleset(token: SessionToken): CompatibilityRuleset < (Async & Abort[BackofficeError]) =
    http.call[CompatibilityRuleset]("GET", path("catalog", "ruleset"), Some(token), None)

  override def upsertCategory(token: SessionToken, category: ProductCategory): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("PUT", path("catalog", "categories"), Some(token), json(category))

  override def deleteCategory(token: SessionToken, id: CategoryId): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("DELETE", path("catalog", "categories", seg(id.value)), Some(token), None)

  override def upsertMaterial(token: SessionToken, material: Material): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("PUT", path("catalog", "materials"), Some(token), json(material))

  override def deleteMaterial(token: SessionToken, id: MaterialId): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("DELETE", path("catalog", "materials", seg(id.value)), Some(token), None)

  override def upsertFinish(token: SessionToken, finish: Finish): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("PUT", path("catalog", "finishes"), Some(token), json(finish))

  override def deleteFinish(token: SessionToken, id: FinishId): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("DELETE", path("catalog", "finishes", seg(id.value)), Some(token), None)

  override def upsertPrintingMethod(token: SessionToken, method: PrintingMethod): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("PUT", path("catalog", "printing-methods"), Some(token), json(method))

  override def deletePrintingMethod(token: SessionToken, id: PrintingMethodId): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("DELETE", path("catalog", "printing-methods", seg(id.value)), Some(token), None)

  override def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("PUT", path("catalog", "showcases"), Some(token), json(listing))

  override def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId): ProductCatalog < (Async & Abort[BackofficeError]) =
    http.call[ProductCatalog]("DELETE", path("catalog", "showcases", seg(categoryId.value)), Some(token), None)

  override def upsertRule(token: SessionToken, rule: CompatibilityRule): CompatibilityRuleset < (Async & Abort[BackofficeError]) =
    http.call[CompatibilityRuleset]("PUT", path("catalog", "rules"), Some(token), json(rule))

  override def deleteRule(token: SessionToken, id: RuleId): CompatibilityRuleset < (Async & Abort[BackofficeError]) =
    http.call[CompatibilityRuleset]("DELETE", path("catalog", "rules", seg(id.value)), Some(token), None)

  override def listPricelists(token: SessionToken): List[Pricelist] < (Async & Abort[BackofficeError]) =
    http.call[List[Pricelist]]("GET", path("pricing", "pricelists"), Some(token), None)

  override def savePricelist(token: SessionToken, pricelist: Pricelist): Pricelist < (Async & Abort[BackofficeError]) =
    http.call[Pricelist]("PUT", path("pricing", "pricelists"), Some(token), json(pricelist))

  override def listAgreements(token: SessionToken): List[CustomerPriceAgreement] < (Async & Abort[BackofficeError]) =
    http.call[List[CustomerPriceAgreement]]("GET", path("pricing", "agreements"), Some(token), None)

  override def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement): CustomerPriceAgreement < (Async & Abort[BackofficeError]) =
    http.call[CustomerPriceAgreement]("PUT", path("pricing", "agreements"), Some(token), json(agreement))

  override def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency): Unit < (Async & Abort[BackofficeError]) =
    http.callUnit("POST", path("pricing", "agreements", "delete"), Some(token), json(DeleteAgreementRequest(target = target, currency = currency)))

  override def listPromoCodes(token: SessionToken): List[PromoCode] < (Async & Abort[BackofficeError]) =
    http.call[List[PromoCode]]("GET", path("pricing", "promo-codes"), Some(token), None)

  override def savePromoCode(token: SessionToken, promo: PromoCode): PromoCode < (Async & Abort[BackofficeError]) =
    http.call[PromoCode]("PUT", path("pricing", "promo-codes"), Some(token), json(promo))

  override def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean): PromoCode < (Async & Abort[BackofficeError]) =
    http.call[PromoCode]("POST", path("pricing", "promo-codes", seg(id.value), "active"), Some(token), json(ActiveRequest(active = active)))

  override def deletePromoCode(token: SessionToken, id: DiscountCodeId): Unit < (Async & Abort[BackofficeError]) =
    http.callUnit("DELETE", path("pricing", "promo-codes", seg(id.value)), Some(token), None)

  override def listCostSheets(token: SessionToken): List[ProductionCostSheet] < (Async & Abort[BackofficeError]) =
    http.call[List[ProductionCostSheet]]("GET", path("pricing", "cost-sheets"), Some(token), None)

  override def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet): ProductionCostSheet < (Async & Abort[BackofficeError]) =
    http.call[ProductionCostSheet]("PUT", path("pricing", "cost-sheets"), Some(token), json(sheet))

  override def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd): MarginSimulationResult < (Async & Abort[BackofficeError]) =
    http.call[MarginSimulationResult]("POST", path("pricing", "margin-simulation"), Some(token), json(cmd))

  override def listCustomers(token: SessionToken, filter: CustomerFilter): List[CustomerView] < (Async & Abort[BackofficeError]) =
    http.call[List[CustomerView]]("POST", path("customers", "search"), Some(token), json(filter))

  override def getCustomerDetail(token: SessionToken, id: CustomerId): CustomerDetailView < (Async & Abort[BackofficeError]) =
    http.call[CustomerDetailView]("GET", path("customers", seg(id.value)), Some(token), None)

  override def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers"), Some(token), json(cmd))

  override def approveCustomer(token: SessionToken, id: CustomerId): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers", seg(id.value), "approve"), Some(token), None)

  override def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers", seg(id.value), "suspend"), Some(token), json(ReasonRequest(reason = reason)))

  override def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers", seg(id.value), "deactivate"), Some(token), json(ReasonRequest(reason = reason)))

  override def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers", seg(id.value), "tier"), Some(token), json(TierRequest(tier = tier)))

  override def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers", seg(id.value), "payment-terms"), Some(token), json(terms))

  override def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("PUT", path("customers", "profile"), Some(token), json(cmd))

  override def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString): CustomerView < (Async & Abort[BackofficeError]) =
    http.call[CustomerView]("POST", path("customers", seg(id.value), "notes"), Some(token), json(TextRequest(text = text)))

  override def listStaffMembers(token: SessionToken): List[StaffMemberView] < (Async & Abort[BackofficeError]) =
    http.call[List[StaffMemberView]]("GET", path("staff"), Some(token), None)

  override def createStaffMember(token: SessionToken, cmd: CreateStaffCmd): StaffMemberView < (Async & Abort[BackofficeError]) =
    http.call[StaffMemberView]("POST", path("staff"), Some(token), json(cmd))

  override def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]): StaffMemberView < (Async & Abort[BackofficeError]) =
    http.call[StaffMemberView]("PUT", path("staff", seg(employeeId.value), "roles"), Some(token), json(RolesRequest(roles = roles)))

  override def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean): StaffMemberView < (Async & Abort[BackofficeError]) =
    http.call[StaffMemberView]("POST", path("staff", seg(employeeId.value), "active"), Some(token), json(ActiveRequest(active = isActive)))

  override def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString): Unit < (Async & Abort[BackofficeError]) =
    http.callUnit("POST", path("staff", "password-reset"), Some(token), json(PasswordResetRequest(email = email, newPassword = newRawPassword)))

  override def getStoreDeliverySettings(token: SessionToken): StoreDeliverySettingsView < (Async & Abort[BackofficeError]) =
    http.call[StoreDeliverySettingsView]("GET", path("settings", "delivery"), Some(token), None)

  override def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation): List[ShopPickupLocation] < (Async & Abort[BackofficeError]) =
    http.call[List[ShopPickupLocation]]("PUT", path("settings", "pickup-locations"), Some(token), json(loc))

  override def deletePickupLocation(token: SessionToken, id: DeliveryOptionId): List[ShopPickupLocation] < (Async & Abort[BackofficeError]) =
    http.call[List[ShopPickupLocation]]("DELETE", path("settings", "pickup-locations", seg(id.value)), Some(token), None)

  override def upsertCourierOption(token: SessionToken, opt: CourierServiceOption): List[CourierServiceOption] < (Async & Abort[BackofficeError]) =
    http.call[List[CourierServiceOption]]("PUT", path("settings", "couriers"), Some(token), json(opt))

  override def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId): List[CourierServiceOption] < (Async & Abort[BackofficeError]) =
    http.call[List[CourierServiceOption]]("DELETE", path("settings", "couriers", seg(currency.toString), seg(id.value)), Some(token), None)

  override def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig): Map[Currency, BankAccountConfig] < (Async & Abort[BackofficeError]) =
    http.call[Map[Currency, BankAccountConfig]]("PUT", path("settings", "bank-accounts", seg(currency.toString)), Some(token), json(account))
