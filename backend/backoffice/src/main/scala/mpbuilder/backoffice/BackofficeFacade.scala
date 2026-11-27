package mpbuilder.backoffice

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import kyo.*

/** Role-based Backoffice / Admin BFF orchestrating all bounded contexts via public APIs only.
  *
  * Every operation (except `loginStaff`) validates the `SessionToken` via `IdentityService`,
  * verifies the caller is a `Principal.Employee`, and enforces role permissions:
  *   - Any staff role (`Operator`, `Prepress`, `Manager`, `Admin`): read views, notes, artwork review
  *   - Intake staff (`Operator`, `Manager`, `Admin`): order acceptance, hold/resume, manual stage progression, payment confirmation, cancellation
  *   - `Manager` | `Admin`: catalog CRUD, pricing/agreements/promo/cost-sheet CRUD, customer management, store delivery/bank settings
  *   - `Admin`: staff directory & role administration
  */
trait BackofficeFacade:

  // ── 1. Staff Authentication ────────────────────────────────────────────────
  def loginStaff(email: EmailAddress, rawPassword: NonEmptyString): StaffAuthResult < (Async & Abort[BackofficeError])
  def validateStaffSession(token: SessionToken): StaffAuthResult < (Async & Abort[BackofficeError])
  def logoutStaff(token: SessionToken): Unit < Async

  // ── 2. Dashboard Summary ───────────────────────────────────────────────────
  def getDashboardSummary(token: SessionToken): BackofficeDashboardSummary < (Async & Abort[BackofficeError])

  // ── 3. Order Intake & Manual Lifecycle ─────────────────────────────────────
  def listOrders(token: SessionToken, filter: OrderFilter = OrderFilter()): List[OrderView] < (Async & Abort[BackofficeError])
  def getOrder(token: SessionToken, orderId: OrderId): OrderView < (Async & Abort[BackofficeError])
  def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): OrderView < (Async & Abort[BackofficeError])
  def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString): OrderView < (Async & Abort[BackofficeError])
  def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): OrderView < (Async & Abort[BackofficeError])
  def advanceOrderStage(
      token: SessionToken,
      orderId: OrderId,
      target: OrderStatus,
      note: Option[NonEmptyString] = None,
  ): OrderView < (Async & Abort[BackofficeError])
  def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): OrderView < (Async & Abort[BackofficeError])
  def markOrderPaymentStatus(
      token: SessionToken,
      orderId: OrderId,
      status: PaymentStatus,
      note: Option[NonEmptyString] = None,
  ): OrderView < (Async & Abort[BackofficeError])
  def editOrderLine(token: SessionToken, cmd: EditOrderLineCmd): OrderView < (Async & Abort[BackofficeError])
  def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString): OrderView < (Async & Abort[BackofficeError])
  def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString): OrderView < (Async & Abort[BackofficeError])

  // ── 4. Artwork Review ──────────────────────────────────────────────────────
  def listOrderArtwork(token: SessionToken, orderId: OrderId): List[ArtworkSubmissionView] < (Async & Abort[BackofficeError])
  def approveArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      overrideFailures: Boolean = false,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError])
  def requestArtworkChanges(
      token: SessionToken,
      artworkId: ArtworkId,
      notes: NonEmptyString,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError])
  def rejectArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      reason: NonEmptyString,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError])
  def uploadStaffArtworkRevision(
      token: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: StaffArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError])
  def uploadStaffOrderLineArtwork(
      token: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: StaffArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError])
  def rerunArtworkPreflight(
      token: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError])

  // ── 5. Catalog Management ──────────────────────────────────────────────────
  def getCatalog(token: SessionToken): ProductCatalog < (Async & Abort[BackofficeError])
  def getRuleset(token: SessionToken): CompatibilityRuleset < (Async & Abort[BackofficeError])
  def upsertCategory(token: SessionToken, category: ProductCategory): ProductCatalog < (Async & Abort[BackofficeError])
  def deleteCategory(token: SessionToken, id: CategoryId): ProductCatalog < (Async & Abort[BackofficeError])
  def upsertMaterial(token: SessionToken, material: Material): ProductCatalog < (Async & Abort[BackofficeError])
  def deleteMaterial(token: SessionToken, id: MaterialId): ProductCatalog < (Async & Abort[BackofficeError])
  def upsertFinish(token: SessionToken, finish: Finish): ProductCatalog < (Async & Abort[BackofficeError])
  def deleteFinish(token: SessionToken, id: FinishId): ProductCatalog < (Async & Abort[BackofficeError])
  def upsertPrintingMethod(token: SessionToken, method: PrintingMethod): ProductCatalog < (Async & Abort[BackofficeError])
  def deletePrintingMethod(token: SessionToken, id: PrintingMethodId): ProductCatalog < (Async & Abort[BackofficeError])
  def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing): ProductCatalog < (Async & Abort[BackofficeError])
  def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId): ProductCatalog < (Async & Abort[BackofficeError])
  def upsertRule(token: SessionToken, rule: CompatibilityRule): CompatibilityRuleset < (Async & Abort[BackofficeError])
  def deleteRule(token: SessionToken, id: RuleId): CompatibilityRuleset < (Async & Abort[BackofficeError])

  // ── 6. Pricing, Agreements, Promos & Margin Simulation ─────────────────────
  def listPricelists(token: SessionToken): List[Pricelist] < (Async & Abort[BackofficeError])
  def savePricelist(token: SessionToken, pricelist: Pricelist): Pricelist < (Async & Abort[BackofficeError])
  def listAgreements(token: SessionToken): List[CustomerPriceAgreement] < (Async & Abort[BackofficeError])
  def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement): CustomerPriceAgreement < (Async & Abort[BackofficeError])
  def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency): Unit < (Async & Abort[BackofficeError])
  def listPromoCodes(token: SessionToken): List[PromoCode] < (Async & Abort[BackofficeError])
  def savePromoCode(token: SessionToken, promo: PromoCode): PromoCode < (Async & Abort[BackofficeError])
  def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean): PromoCode < (Async & Abort[BackofficeError])
  def deletePromoCode(token: SessionToken, id: DiscountCodeId): Unit < (Async & Abort[BackofficeError])
  def listCostSheets(token: SessionToken): List[ProductionCostSheet] < (Async & Abort[BackofficeError])
  def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet): ProductionCostSheet < (Async & Abort[BackofficeError])
  def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd): MarginSimulationResult < (Async & Abort[BackofficeError])

  // ── 7. Customer Management ─────────────────────────────────────────────────
  def listCustomers(token: SessionToken, filter: CustomerFilter = CustomerFilter()): List[CustomerView] < (Async & Abort[BackofficeError])
  def getCustomerDetail(token: SessionToken, id: CustomerId): CustomerDetailView < (Async & Abort[BackofficeError])
  def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd): CustomerView < (Async & Abort[BackofficeError])
  def approveCustomer(token: SessionToken, id: CustomerId): CustomerView < (Async & Abort[BackofficeError])
  def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): CustomerView < (Async & Abort[BackofficeError])
  def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): CustomerView < (Async & Abort[BackofficeError])
  def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier): CustomerView < (Async & Abort[BackofficeError])
  def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms): CustomerView < (Async & Abort[BackofficeError])
  def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd): CustomerView < (Async & Abort[BackofficeError])
  def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString): CustomerView < (Async & Abort[BackofficeError])

  // ── 8. Staff Directory & Store Delivery / Bank Settings ────────────────────
  def listStaffMembers(token: SessionToken): List[StaffMemberView] < (Async & Abort[BackofficeError])
  def createStaffMember(token: SessionToken, cmd: CreateStaffCmd): StaffMemberView < (Async & Abort[BackofficeError])
  def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]): StaffMemberView < (Async & Abort[BackofficeError])
  def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean): StaffMemberView < (Async & Abort[BackofficeError])
  def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString): Unit < (Async & Abort[BackofficeError])

  def getStoreDeliverySettings(token: SessionToken): StoreDeliverySettingsView < (Async & Abort[BackofficeError])
  def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation): List[ShopPickupLocation] < (Async & Abort[BackofficeError])
  def deletePickupLocation(token: SessionToken, id: DeliveryOptionId): List[ShopPickupLocation] < (Async & Abort[BackofficeError])
  def upsertCourierOption(token: SessionToken, opt: CourierServiceOption): List[CourierServiceOption] < (Async & Abort[BackofficeError])
  def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId): List[CourierServiceOption] < (Async & Abort[BackofficeError])
  def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig): Map[Currency, BankAccountConfig] < (Async & Abort[BackofficeError])

object BackofficeFacade:
  def make(
      catalog: CatalogService,
      pricing: PricingService,
      customers: CustomerService,
      identity: IdentityService,
      orders: OrderService,
      checkoutOptions: CheckoutOptionsService,
      artwork: ArtworkService,
      blobs: BlobStorage,
  ): BackofficeFacade =
    new BackofficeFacadeLive(catalog, pricing, customers, identity, orders, checkoutOptions, artwork, blobs)

final class BackofficeFacadeLive(
    catalogService: CatalogService,
    pricingService: PricingService,
    customerService: CustomerService,
    identityService: IdentityService,
    orderService: OrderService,
    checkoutOptionsService: CheckoutOptionsService,
    artworkService: ArtworkService,
    blobStorage: BlobStorage,
) extends BackofficeFacade:

  private val AnyStaffRoles: Set[EmployeeRole]     = EmployeeRole.values.toSet
  private val IntakeRoles: Set[EmployeeRole]       = Set(EmployeeRole.Operator, EmployeeRole.Manager, EmployeeRole.Admin)
  private val ManagerAdminRoles: Set[EmployeeRole] = Set(EmployeeRole.Manager, EmployeeRole.Admin)
  private val AdminOnlyRoles: Set[EmployeeRole]    = Set(EmployeeRole.Admin)

  private def authorize(
      token: SessionToken,
      allowedRoles: Set[EmployeeRole],
  ): Principal.Employee < (Async & Abort[BackofficeError]) =
    for
      session <- identityService
                   .validateSession(token)
                   .mapAbort(e => BackofficeError.Unauthenticated(e.message))
      emp     <- session.principal match
                   case e: Principal.Employee => Kyo.lift(e)
                   case _                     => Abort.fail(BackofficeError.NotAnEmployeeSession)
      _       <- Effects.failWhen(emp.roles.intersect(allowedRoles).isEmpty)(BackofficeError.Forbidden(allowedRoles, emp.roles))
    yield emp

  // ── 1. Staff Authentication ────────────────────────────────────────────────

  override def loginStaff(
      email: EmailAddress,
      rawPassword: NonEmptyString,
  ): StaffAuthResult < (Async & Abort[BackofficeError]) =
    for
      session <- identityService
                   .loginWithPassword(email, rawPassword)
                   .mapAbort(BackofficeError.IdentityFailure.apply)
      emp     <- session.principal match
                   case e: Principal.Employee => Kyo.lift(e)
                   case _ =>
                     identityService.logout(session.token) *>
                       Abort.fail(BackofficeError.NotAnEmployeeSession)
    yield StaffAuthResult(session, emp)

  override def validateStaffSession(token: SessionToken): StaffAuthResult < (Async & Abort[BackofficeError]) =
    for
      session <- identityService
                   .validateSession(token)
                   .mapAbort(e => BackofficeError.Unauthenticated(e.message))
      emp     <- session.principal match
                   case e: Principal.Employee => Kyo.lift(e)
                   case _                     => Abort.fail(BackofficeError.NotAnEmployeeSession)
    yield StaffAuthResult(session, emp)

  override def logoutStaff(token: SessionToken): Unit < Async =
    identityService.logout(token)

  // ── 2. Dashboard Summary ───────────────────────────────────────────────────

  override def getDashboardSummary(token: SessionToken): BackofficeDashboardSummary < (Async & Abort[BackofficeError]) =
    for
      _         <- authorize(token, AnyStaffRoles)
      orders    <- orderService.listOrders(OrderFilter())
      customers <- customerService.listCustomers(CustomerFilter())
      promos    <- pricingService.listPromoCodes
      catalog   <- catalogService.getCatalog
    yield
      val pendingCustomers = customers.filter(_.status == CustomerStatus.PendingApproval)
      BackofficeDashboardSummary(
        totalOrders = orders.size,
        ordersAwaitingIntake = orders.count(o =>
          o.status == OrderStatus.Placed ||
            o.status == OrderStatus.AwaitingPayment ||
            o.status == OrderStatus.Confirmed
        ),
        ordersAwaitingPayment = orders.count(_.status == OrderStatus.AwaitingPayment),
        ordersAccepted = orders.count(_.status == OrderStatus.Accepted),
        ordersInProduction = orders.count(_.status == OrderStatus.InProduction),
        ordersReadyForDispatch = orders.count(_.status == OrderStatus.ReadyForDispatch),
        ordersOnHold = orders.count(_.status.isInstanceOf[OrderStatus.OnHold]),
        ordersCompleted = orders.count(_.status == OrderStatus.Completed),
        pendingB2BCustomers = pendingCustomers.size,
        totalCustomers = customers.size,
        activePromoCodes = promos.count(_.isActive),
        catalogCategoryCount = catalog.categories.size,
        catalogMaterialCount = catalog.materials.size,
        recentOrders = orders.take(10),
        pendingCustomers = pendingCustomers,
      )

  // ── 3. Order Intake & Manual Lifecycle ─────────────────────────────────────

  override def listOrders(
      token: SessionToken,
      filter: OrderFilter,
  ): List[OrderView] < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> orderService.listOrders(filter)

  override def getOrder(token: SessionToken, orderId: OrderId): OrderView < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *>
      orderService.getOrder(orderId).mapAbort(BackofficeError.OrderFailure.apply)

  override def acceptOrder(
      token: SessionToken,
      orderId: OrderId,
      note: Option[NonEmptyString],
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .acceptOrder(orderId, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def putOrderOnHold(
      token: SessionToken,
      orderId: OrderId,
      reason: NonEmptyString,
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .putOnHold(orderId, emp.employeeId, Some(emp.displayName), reason)
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def resumeOrderFromHold(
      token: SessionToken,
      orderId: OrderId,
      note: Option[NonEmptyString],
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .resumeFromHold(orderId, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def advanceOrderStage(
      token: SessionToken,
      orderId: OrderId,
      target: OrderStatus,
      note: Option[NonEmptyString],
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .advanceStage(orderId, target, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def confirmOrderPayment(
      token: SessionToken,
      orderId: OrderId,
      note: Option[NonEmptyString],
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .confirmPaymentByStaff(orderId, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def markOrderPaymentStatus(
      token: SessionToken,
      orderId: OrderId,
      status: PaymentStatus,
      note: Option[NonEmptyString],
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- orderService
               .markPaymentStatus(orderId, status, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def editOrderLine(
      token: SessionToken,
      cmd: EditOrderLineCmd,
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles + EmployeeRole.Prepress)
      res <- orderService
               .editOrderLine(cmd, emp.employeeId, Some(emp.displayName))
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def cancelOrder(
      token: SessionToken,
      orderId: OrderId,
      reason: NonEmptyString,
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .cancelByStaff(orderId, emp.employeeId, Some(emp.displayName), reason)
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  override def addOrderInternalNote(
      token: SessionToken,
      orderId: OrderId,
      text: NonEmptyString,
  ): OrderView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- orderService
               .addInternalNote(orderId, emp.employeeId, Some(emp.displayName), text)
               .mapAbort(BackofficeError.OrderFailure.apply)
    yield res

  // ── 4. Artwork Review ──────────────────────────────────────────────────────

  override def listOrderArtwork(
      token: SessionToken,
      orderId: OrderId,
  ): List[ArtworkSubmissionView] < (Async & Abort[BackofficeError]) =
    for
      _     <- authorize(token, AnyStaffRoles)
      order <- orderService.getOrder(orderId).mapAbort(BackofficeError.OrderFailure.apply)
      byIdx <- Effects.traverse(order.lines.toList.zipWithIndex) { case (_, idx) =>
                 artworkService.listByOwner(ArtworkOwner.OrderLineRef(orderId, idx))
               }
    yield byIdx.flatten

  override def approveArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      overrideFailures: Boolean,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- artworkService
               .approve(artworkId, emp.employeeId, overrideFailures)
               .mapAbort(BackofficeError.ArtworkFailure.apply)
    yield res

  override def requestArtworkChanges(
      token: SessionToken,
      artworkId: ArtworkId,
      notes: NonEmptyString,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *>
      artworkService.requestChanges(artworkId, notes).mapAbort(BackofficeError.ArtworkFailure.apply)

  override def rejectArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      reason: NonEmptyString,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- artworkService
               .reject(artworkId, emp.employeeId, reason)
               .mapAbort(BackofficeError.ArtworkFailure.apply)
    yield res

  override def uploadStaffArtworkRevision(
      token: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
      payload: StaffArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    for
      _        <- authorize(token, AnyStaffRoles)
      order    <- orderService.getOrder(orderId).mapAbort(BackofficeError.OrderFailure.apply)
      existing <- artworkService.getById(artworkId).mapAbort(BackofficeError.ArtworkFailure.apply)
      lineOpt   = existing.owner match
                    case ArtworkOwner.OrderLineRef(_, idx) => order.lines.lift(idx)
                    case _                                 => None
      compOpt   = lineOpt.map(_.product.component(existing.componentKey))
      trimSize  = compOpt.map(_.finishedSize).getOrElse(existing.latestRevision.extractedSpecs.trimSize)
      pageCnt   = payload.pageCount.getOrElse(existing.latestRevision.extractedSpecs.pageCount)
      req       = preflightRequirementFor(trimSize, pageCnt, lineOpt.map(_.product.bleed))
      revNum    = existing.revisionCount + 1
      file     <- assetFile(s"uploads/${orderId.value}/${artworkId.value}-v$revNum-${sanitizeFileSegment(payload.fileName.value)}", payload)
      updated  <- artworkService
                    .uploadRevision(
                      UploadRevisionCmd(
                        artworkId = artworkId,
                        file = file,
                        extractedSpecs = ExtractedFileSpecs(
                          resolutionDpi = payload.resolutionDpi,
                          trimSize = trimSize,
                          bleed = NonNegativeMm.unsafe(payload.bleedMm.max(0)),
                          colorSpace = payload.colorSpace,
                          pageCount = pageCnt,
                          fontsEmbedded = payload.fontsEmbedded,
                        ),
                        requirement = Some(req),
                      )
                    )
                    .mapAbort(BackofficeError.ArtworkFailure.apply)
    yield updated

  override def uploadStaffOrderLineArtwork(
      token: SessionToken,
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      payload: StaffArtworkUploadPayload,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    for
      _     <- authorize(token, AnyStaffRoles)
      order <- orderService.getOrder(orderId).mapAbort(BackofficeError.OrderFailure.apply)
      line  <- order.lines.lift(lineIndex).orFail(BackofficeError.OrderFailure(OrderError.OrderLineNotFound(orderId, OrderLineId.unsafe(s"index-$lineIndex"))))
      existingList <- artworkService.listByOwner(ArtworkOwner.OrderLineRef(orderId, lineIndex))
      existingOpt   = existingList.find(_.componentKey == componentKey)
      res <- existingOpt match
               case Some(existing) if !existing.status.isInstanceOf[ArtworkStatus.OperatorApproved] =>
                 uploadStaffArtworkRevision(token, orderId, existing.id, payload)
               case _ =>
                 val comp     = line.product.component(componentKey)
                 val trimSize = comp.finishedSize
                 val pageCnt  = payload.pageCount.getOrElse(line.product.expectedArtworkPages(componentKey))
                 val req      = preflightRequirementFor(trimSize, pageCnt, Some(line.product.bleed))
                 for
                   now   <- Effects.now
                   rnd   <- Random.nextInt(8999).map(_ + 1000)
                   artId  = ArtworkId.unsafe(s"art-staff-${now.toEpochMilli}-$rnd")
                   file  <- assetFile(s"uploads/${orderId.value}/${artId.value}-${sanitizeFileSegment(payload.fileName.value)}", payload)
                   view  <- artworkService
                              .upload(
                                UploadArtworkCmd(
                                  artworkId = artId,
                                  owner = ArtworkOwner.OrderLineRef(orderId, lineIndex),
                                  componentKey = componentKey,
                                  file = file,
                                  extractedSpecs = ExtractedFileSpecs(
                                    resolutionDpi = payload.resolutionDpi,
                                    trimSize = trimSize,
                                    bleed = NonNegativeMm.unsafe(payload.bleedMm.max(0)),
                                    colorSpace = payload.colorSpace,
                                    pageCount = pageCnt,
                                    fontsEmbedded = payload.fontsEmbedded,
                                  ),
                                  requirement = Some(req),
                                )
                              )
                              .mapAbort(BackofficeError.ArtworkFailure.apply)
                   _ <- orderService.attachArtwork(orderId, lineIndex, componentKey, artId).mapAbort(BackofficeError.OrderFailure.apply)
                 yield view
    yield res

  override def rerunArtworkPreflight(
      token: SessionToken,
      orderId: OrderId,
      artworkId: ArtworkId,
  ): ArtworkSubmissionView < (Async & Abort[BackofficeError]) =
    for
      _        <- authorize(token, AnyStaffRoles)
      order    <- orderService.getOrder(orderId).mapAbort(BackofficeError.OrderFailure.apply)
      existing <- artworkService.getById(artworkId).mapAbort(BackofficeError.ArtworkFailure.apply)
      lineOpt   = existing.owner match
                    case ArtworkOwner.OrderLineRef(_, idx) => order.lines.lift(idx)
                    case _                                 => None
      compOpt   = lineOpt.map(_.product.component(existing.componentKey))
      trimSize  = compOpt.map(_.finishedSize).getOrElse(existing.latestRevision.extractedSpecs.trimSize)
      pageCnt   = lineOpt.map(_.product.expectedArtworkPages(existing.componentKey)).getOrElse(existing.latestRevision.extractedSpecs.pageCount)
      req       = preflightRequirementFor(trimSize, pageCnt, lineOpt.map(_.product.bleed))
      updated  <- artworkService.runPreflight(artworkId, req).mapAbort(BackofficeError.ArtworkFailure.apply)
    yield updated

  /** Preflight requirement for an order-line artwork; the bleed is the ordered one (3 mm when the artwork has no line). */
  private def preflightRequirementFor(
      trimSize: Dimension2D,
      pageCount: Int,
      orderedBleed: Option[NonNegativeMm],
  ): PreflightRequirement =
    val isLargeFormat = trimSize.width.value >= BigDecimal(400) || trimSize.height.value >= BigDecimal(400)
    PreflightRequirement(
      expectedTrimSize = trimSize,
      requiredBleed = orderedBleed.getOrElse(NonNegativeMm.unsafe(3)),
      minResolutionDpi = if isLargeFormat then 150 else 300,
      expectedPageCount = Some(pageCount),
    )

  /** Builds artwork file metadata, moving inline `data:` URL payloads into `BlobStorage` (Phase 5). */
  private def assetFile(storageKey: String, payload: StaffArtworkUploadPayload): AssetFileMetadata < (Async & Abort[BackofficeError]) =
    BlobIngest
      .ingest(blobStorage, storageKey, payload.downloadUrl.map(_.value), payload.previewUrl.orElse(payload.downloadUrl).map(_.value))
      .mapAbort(BackofficeError.BlobFailure.apply)
      .map { ing =>
        AssetFileMetadata(
          fileName = payload.fileName,
          mimeType = payload.mimeType,
          sizeBytes = ing.sizeBytes.getOrElse(payload.sizeBytes).max(1L),
          storageKey = NonEmptyString.unsafe(storageKey),
          checksumSha256 = ing.checksumSha256.map(NonEmptyString.unsafe),
          downloadUrl = ing.downloadUrl.map(NonEmptyString.unsafe),
          previewUrl = ing.previewUrl.map(NonEmptyString.unsafe),
        )
      }

  private def sanitizeFileSegment(raw: String): String =
    val cleaned = raw.trim.replaceAll("[^a-zA-Z0-9._-]+", "-")
    if cleaned.nonEmpty then cleaned else "artwork.pdf"

  // ── 5. Catalog Management ──────────────────────────────────────────────────

  override def getCatalog(token: SessionToken): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> catalogService.getCatalog

  override def getRuleset(token: SessionToken): CompatibilityRuleset < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> catalogService.getRuleset

  override def upsertCategory(token: SessionToken, category: ProductCategory): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertCategory(category).mapAbort(BackofficeError.CatalogFailure.apply)

  override def deleteCategory(token: SessionToken, id: CategoryId): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteCategory(id).mapAbort(BackofficeError.CatalogFailure.apply)

  override def upsertMaterial(token: SessionToken, material: Material): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertMaterial(material).mapAbort(BackofficeError.CatalogFailure.apply)

  override def deleteMaterial(token: SessionToken, id: MaterialId): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteMaterial(id).mapAbort(BackofficeError.CatalogFailure.apply)

  override def upsertFinish(token: SessionToken, finish: Finish): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertFinish(finish).mapAbort(BackofficeError.CatalogFailure.apply)

  override def deleteFinish(token: SessionToken, id: FinishId): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteFinish(id).mapAbort(BackofficeError.CatalogFailure.apply)

  override def upsertPrintingMethod(token: SessionToken, method: PrintingMethod): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertPrintingMethod(method).mapAbort(BackofficeError.CatalogFailure.apply)

  override def deletePrintingMethod(token: SessionToken, id: PrintingMethodId): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deletePrintingMethod(id).mapAbort(BackofficeError.CatalogFailure.apply)

  override def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertShowcaseListing(listing).mapAbort(BackofficeError.CatalogFailure.apply)

  override def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId): ProductCatalog < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteShowcaseListing(categoryId).mapAbort(BackofficeError.CatalogFailure.apply)

  override def upsertRule(token: SessionToken, rule: CompatibilityRule): CompatibilityRuleset < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertRule(rule).mapAbort(BackofficeError.CatalogFailure.apply)

  override def deleteRule(token: SessionToken, id: RuleId): CompatibilityRuleset < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteRule(id).mapAbort(BackofficeError.CatalogFailure.apply)

  // ── 6. Pricing, Agreements, Promos & Margin Simulation ─────────────────────

  override def listPricelists(token: SessionToken): List[Pricelist] < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> pricingService.listPricelists

  override def savePricelist(token: SessionToken, pricelist: Pricelist): Pricelist < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      pricingService.savePricelist(pricelist).mapAbort(BackofficeError.PricingFailure.apply)

  override def listAgreements(token: SessionToken): List[CustomerPriceAgreement] < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> pricingService.listAgreements

  override def saveAgreement(
      token: SessionToken,
      agreement: CustomerPriceAgreement,
  ): CustomerPriceAgreement < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      pricingService.saveAgreement(agreement).mapAbort(BackofficeError.PricingFailure.apply)

  override def deleteAgreement(
      token: SessionToken,
      target: AgreementTarget,
      currency: Currency,
  ): Unit < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *> pricingService.deleteAgreement(target, currency)

  override def listPromoCodes(token: SessionToken): List[PromoCode] < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> pricingService.listPromoCodes

  override def savePromoCode(token: SessionToken, promo: PromoCode): PromoCode < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      pricingService.savePromoCode(promo).mapAbort(BackofficeError.PricingFailure.apply)

  override def setPromoCodeActive(
      token: SessionToken,
      id: DiscountCodeId,
      active: Boolean,
  ): PromoCode < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      pricingService.setPromoCodeActive(id, active).mapAbort(BackofficeError.PricingFailure.apply)

  override def deletePromoCode(token: SessionToken, id: DiscountCodeId): Unit < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *> pricingService.deletePromoCode(id)

  override def listCostSheets(token: SessionToken): List[ProductionCostSheet] < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> pricingService.listCostSheets

  override def saveCostSheet(
      token: SessionToken,
      sheet: ProductionCostSheet,
  ): ProductionCostSheet < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      pricingService.saveCostSheet(sheet).mapAbort(BackofficeError.PricingFailure.apply)

  override def simulateMargin(
      token: SessionToken,
      cmd: MarginSimulationCmd,
  ): MarginSimulationResult < (Async & Abort[BackofficeError]) =
    for
      _      <- authorize(token, AnyStaffRoles)
      cat    <- catalogService.getCategory(cmd.categoryId).mapAbort(BackofficeError.CatalogFailure.apply)
      preset <- (cat.presets.find(_.id == cmd.presetId)).orFail(BackofficeError.PresetNotFound(cmd.categoryId, cmd.presetId))
      specs   = preset.defaultSpecs.copy(quantity = cmd.quantity)
      cfgCmd  = ConfigureProductCmd(
                  configurationId = ConfigurationId.unsafe(s"sim-${cmd.presetId.value}-${cmd.quantity.value}"),
                  categoryId = cmd.categoryId,
                  components = preset.components.map { cp =>
                    ComponentSelection(
                      key = cp.key,
                      materialId = cp.materialId,
                      printingMethodId = cp.printingMethodId,
                      inkConfiguration = cp.inkConfiguration,
                      finishes = cp.finishes,
                    )
                  },
                  specs = specs,
                )
      cfgRes <- catalogService.configure(cfgCmd).mapAbort(BackofficeError.CatalogFailure.apply)
      subject = toPricingSubject(cfgRes.product, cmd.speed)
      qCmd    = PriceQuoteCmd(
                  quoteId = QuoteId.unsafe(s"sim-q-${cmd.presetId.value}"),
                  subject = subject,
                  currency = cmd.currency,
                  customerId = cmd.customerId,
                  customerTier = cmd.customerTier,
                  promoCode = cmd.promoCode,
                )
      quote  <- pricingService.quote(qCmd).mapAbort(BackofficeError.PricingFailure.apply)
      margin <- pricingService.analyzeMargin(qCmd).mapAbort(BackofficeError.PricingFailure.apply)
      sheets  = subject.components.map(_.sheetsUsed).sum
    yield MarginSimulationResult(quote, margin, sheets)

  // ── 7. Customer Management ─────────────────────────────────────────────────

  override def listCustomers(
      token: SessionToken,
      filter: CustomerFilter,
  ): List[CustomerView] < (Async & Abort[BackofficeError]) =
    authorize(token, AnyStaffRoles) *> customerService.listCustomers(filter)

  override def getCustomerDetail(
      token: SessionToken,
      id: CustomerId,
  ): CustomerDetailView < (Async & Abort[BackofficeError]) =
    for
      _          <- authorize(token, AnyStaffRoles)
      customer   <- customerService.getById(id).mapAbort(BackofficeError.CustomerFailure.apply)
      orders     <- orderService.listOrdersByCustomer(id)
      agreements <- pricingService.listAgreements.map(
                      _.filter(_.target == AgreementTarget.SpecificCustomer(id))
                    )
      artLists   <- Effects.traverse(orders) { ord =>
                      Effects.traverse(ord.lines.toList.zipWithIndex) { case (line, idx) =>
                          artworkService.listByOwner(ArtworkOwner.OrderLineRef(ord.id, idx)).map { arts =>
                            arts.map { art =>
                              CustomerArtworkEntry(
                                artwork = art,
                                orderId = ord.id,
                                orderNumber = ord.orderNumber,
                                orderStatus = ord.status,
                                lineIndex = idx,
                                productName = line.product.categoryName,
                              )
                            }
                          }
                        }
                        .map(_.flatten)
                    }
      allArtworks = artLists.flatten.sortBy(_.artwork.createdAt)(using Ordering[java.time.Instant].reverse)
    yield CustomerDetailView(customer, orders, agreements, allArtworks)

  override def registerCustomer(
      token: SessionToken,
      cmd: RegisterCustomerCmd,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      customerService.register(cmd).mapAbort(BackofficeError.CustomerFailure.apply)

  override def approveCustomer(token: SessionToken, id: CustomerId): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService
               .approve(id, CustomerActor.Employee(emp.employeeId, Some(emp.displayName)))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  override def suspendCustomer(
      token: SessionToken,
      id: CustomerId,
      reason: NonEmptyString,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService
               .suspend(id, reason, Some(emp.employeeId), Some(emp.displayName))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  override def deactivateCustomer(
      token: SessionToken,
      id: CustomerId,
      reason: NonEmptyString,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService
               .deactivate(id, reason, Some(emp.employeeId), Some(emp.displayName))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  override def changeCustomerTier(
      token: SessionToken,
      id: CustomerId,
      tier: CustomerTier,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService
               .changeTier(id, tier, CustomerActor.Employee(emp.employeeId, Some(emp.displayName)))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  override def grantCustomerPaymentTerms(
      token: SessionToken,
      id: CustomerId,
      terms: PaymentTerms,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService
               .grantPaymentTerms(id, terms, CustomerActor.Employee(emp.employeeId, Some(emp.displayName)))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  override def updateCustomerProfile(
      token: SessionToken,
      cmd: UpdateCustomerProfileCmd,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService
               .updateProfile(cmd.copy(actor = CustomerActor.Employee(emp.employeeId, Some(emp.displayName))))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  override def addCustomerNote(
      token: SessionToken,
      id: CustomerId,
      text: NonEmptyString,
  ): CustomerView < (Async & Abort[BackofficeError]) =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- customerService
               .addNote(id, text, Some(emp.employeeId), Some(emp.displayName))
               .mapAbort(BackofficeError.CustomerFailure.apply)
    yield res

  // ── 8. Staff Directory & Store Delivery / Bank Settings ────────────────────

  override def listStaffMembers(token: SessionToken): List[StaffMemberView] < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *> identityService.listStaffMembers

  override def createStaffMember(
      token: SessionToken,
      cmd: CreateStaffCmd,
  ): StaffMemberView < (Async & Abort[BackofficeError]) =
    authorize(token, AdminOnlyRoles) *>
      identityService.createStaffMember(cmd).mapAbort(BackofficeError.IdentityFailure.apply)

  override def updateStaffRoles(
      token: SessionToken,
      employeeId: EmployeeId,
      roles: Set[EmployeeRole],
  ): StaffMemberView < (Async & Abort[BackofficeError]) =
    authorize(token, AdminOnlyRoles) *>
      identityService.updateStaffRoles(employeeId, roles).mapAbort(BackofficeError.IdentityFailure.apply)

  override def setStaffActive(
      token: SessionToken,
      employeeId: EmployeeId,
      isActive: Boolean,
  ): StaffMemberView < (Async & Abort[BackofficeError]) =
    authorize(token, AdminOnlyRoles) *>
      identityService.setStaffActive(employeeId, isActive).mapAbort(BackofficeError.IdentityFailure.apply)

  override def resetStaffPassword(
      token: SessionToken,
      email: EmailAddress,
      newRawPassword: NonEmptyString,
  ): Unit < (Async & Abort[BackofficeError]) =
    authorize(token, AdminOnlyRoles) *>
      identityService.resetPassword(email, newRawPassword).mapAbort(BackofficeError.IdentityFailure.apply)

  override def getStoreDeliverySettings(token: SessionToken): StoreDeliverySettingsView < (Async & Abort[BackofficeError]) =
    for
      _        <- authorize(token, AnyStaffRoles)
      pickups  <- checkoutOptionsService.listAllPickupLocations
      couriers <- checkoutOptionsService.listAllCourierOptions
      banks    <- checkoutOptionsService.listBankAccounts
    yield StoreDeliverySettingsView(
      pickupLocations = pickups,
      couriersByCurrency = couriers.groupBy(_.currency),
      bankAccounts = banks,
    )

  override def upsertPickupLocation(
      token: SessionToken,
      loc: ShopPickupLocation,
  ): List[ShopPickupLocation] < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.upsertPickupLocation(loc) *>
      checkoutOptionsService.listAllPickupLocations

  override def deletePickupLocation(
      token: SessionToken,
      id: DeliveryOptionId,
  ): List[ShopPickupLocation] < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.deletePickupLocation(id) *>
      checkoutOptionsService.listAllPickupLocations

  override def upsertCourierOption(
      token: SessionToken,
      opt: CourierServiceOption,
  ): List[CourierServiceOption] < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.upsertCourierOption(opt) *>
      checkoutOptionsService.listAllCourierOptions.map(_.filter(_.currency == opt.currency))

  override def deleteCourierOption(
      token: SessionToken,
      currency: Currency,
      id: DeliveryOptionId,
  ): List[CourierServiceOption] < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.deleteCourierOption(id, currency) *>
      checkoutOptionsService.listAllCourierOptions.map(_.filter(_.currency == currency))

  override def upsertBankAccount(
      token: SessionToken,
      currency: Currency,
      account: BankAccountConfig,
  ): Map[Currency, BankAccountConfig] < (Async & Abort[BackofficeError]) =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.upsertBankAccount(currency, account) *>
      checkoutOptionsService.listBankAccounts

  // ── Internal pure translator for Margin Simulation ─────────────────────────

  private def toPricingSubject(product: ConfiguredProduct, speed: ManufacturingSpeed): PricingSubject =
    val pricingComponents = product.components.map { comp =>
      val (stockFormatId, sheetsUsed, cutsPerSheet) = comp.geometry.imposition match
        case ImpositionResult.SheetImposition(fmtId, _, _, sheets, cuts, _) => (Some(fmtId), sheets, cuts)
        case ImpositionResult.RollImposition(fmtId, _, _, _, _)             => (Some(fmtId), 0, 0)
        case ImpositionResult.DiscreteUnit(_)                               => (None, 0, 0)

      val pricingFinishes = comp.finishes.map { sf =>
        val (discreteParam, linearMeters) = sf.params match
          case Some(FinishParams.Scoring(cnt))       => (Some(cnt), None)
          case Some(FinishParams.Grommets(sp))       => (Some(sp), None)
          case Some(FinishParams.Perforation(p))     => (Some(p), None)
          case Some(FinishParams.RoundCorners(c, _)) => (Some(c), None)
          case Some(FinishParams.LinearLength(m))    => (None, Some(m))
          case _                                     => (None, None)

        PricingFinish(
          finishId = sf.id,
          finishTypeKey = sf.finishType.toString,
          sideMultiplier = sf.sideMultiplier,
          discreteParam = discreteParam,
          linearMeters = linearMeters,
        )
      }

      PricingComponent(
        key = comp.key,
        materialId = comp.material.id,
        stockFormatId = stockFormatId,
        printingMethodId = comp.print.map(_.printingMethod.id),
        printingProcessKey = comp.print.map(_.printingMethod.processType.toString),
        inkKey = comp.print.map(p => InkBillingKey(p.ink.front.notation, p.ink.back.notation)),
        unitsPerProduct = comp.geometry.unitsPerProduct,
        totalFlatUnits = comp.geometry.totalFlatUnits,
        sheetsUsed = sheetsUsed,
        guillotineCutsPerSheet = cutsPerSheet,
        areaPerUnitSqM = comp.geometry.areaPerUnitSqM,
        perimeterPerUnitM = comp.geometry.perimeterPerUnitM,
        finishes = pricingFinishes,
      )
    }

    PricingSubject(
      categoryId = product.categoryId,
      quantity = product.specs.quantity,
      versionCount = product.specs.versionCount,
      components = pricingComponents,
      foldKey = product.specs.dimensions.foldTypeOpt.map(_.toString),
      bindingKey = product.specs.binding.map(_.method.toString),
      speed = speed,
    )
