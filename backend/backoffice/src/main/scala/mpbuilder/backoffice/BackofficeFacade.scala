package mpbuilder.backoffice

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import zio.*

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
  def loginStaff(email: EmailAddress, rawPassword: NonEmptyString): IO[BackofficeError, StaffAuthResult]
  def validateStaffSession(token: SessionToken): IO[BackofficeError, StaffAuthResult]
  def logoutStaff(token: SessionToken): UIO[Unit]

  // ── 2. Dashboard Summary ───────────────────────────────────────────────────
  def getDashboardSummary(token: SessionToken): IO[BackofficeError, BackofficeDashboardSummary]

  // ── 3. Order Intake & Manual Lifecycle ─────────────────────────────────────
  def listOrders(token: SessionToken, filter: OrderFilter = OrderFilter()): IO[BackofficeError, List[OrderView]]
  def getOrder(token: SessionToken, orderId: OrderId): IO[BackofficeError, OrderView]
  def acceptOrder(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): IO[BackofficeError, OrderView]
  def putOrderOnHold(token: SessionToken, orderId: OrderId, reason: NonEmptyString): IO[BackofficeError, OrderView]
  def resumeOrderFromHold(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): IO[BackofficeError, OrderView]
  def advanceOrderStage(
      token: SessionToken,
      orderId: OrderId,
      target: OrderStatus,
      note: Option[NonEmptyString] = None,
  ): IO[BackofficeError, OrderView]
  def confirmOrderPayment(token: SessionToken, orderId: OrderId, note: Option[NonEmptyString] = None): IO[BackofficeError, OrderView]
  def cancelOrder(token: SessionToken, orderId: OrderId, reason: NonEmptyString): IO[BackofficeError, OrderView]
  def addOrderInternalNote(token: SessionToken, orderId: OrderId, text: NonEmptyString): IO[BackofficeError, OrderView]

  // ── 4. Artwork Review ──────────────────────────────────────────────────────
  def listOrderArtwork(token: SessionToken, orderId: OrderId): IO[BackofficeError, List[ArtworkSubmissionView]]
  def approveArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      overrideFailures: Boolean = false,
  ): IO[BackofficeError, ArtworkSubmissionView]
  def requestArtworkChanges(
      token: SessionToken,
      artworkId: ArtworkId,
      notes: NonEmptyString,
  ): IO[BackofficeError, ArtworkSubmissionView]

  // ── 5. Catalog Management ──────────────────────────────────────────────────
  def getCatalog(token: SessionToken): IO[BackofficeError, ProductCatalog]
  def getRuleset(token: SessionToken): IO[BackofficeError, CompatibilityRuleset]
  def upsertCategory(token: SessionToken, category: ProductCategory): IO[BackofficeError, ProductCatalog]
  def deleteCategory(token: SessionToken, id: CategoryId): IO[BackofficeError, ProductCatalog]
  def upsertMaterial(token: SessionToken, material: Material): IO[BackofficeError, ProductCatalog]
  def deleteMaterial(token: SessionToken, id: MaterialId): IO[BackofficeError, ProductCatalog]
  def upsertFinish(token: SessionToken, finish: Finish): IO[BackofficeError, ProductCatalog]
  def deleteFinish(token: SessionToken, id: FinishId): IO[BackofficeError, ProductCatalog]
  def upsertPrintingMethod(token: SessionToken, method: PrintingMethod): IO[BackofficeError, ProductCatalog]
  def deletePrintingMethod(token: SessionToken, id: PrintingMethodId): IO[BackofficeError, ProductCatalog]
  def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing): IO[BackofficeError, ProductCatalog]
  def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId): IO[BackofficeError, ProductCatalog]
  def upsertRule(token: SessionToken, rule: CompatibilityRule): IO[BackofficeError, CompatibilityRuleset]
  def deleteRule(token: SessionToken, id: RuleId): IO[BackofficeError, CompatibilityRuleset]

  // ── 6. Pricing, Agreements, Promos & Margin Simulation ─────────────────────
  def listPricelists(token: SessionToken): IO[BackofficeError, List[Pricelist]]
  def savePricelist(token: SessionToken, pricelist: Pricelist): IO[BackofficeError, Pricelist]
  def listAgreements(token: SessionToken): IO[BackofficeError, List[CustomerPriceAgreement]]
  def saveAgreement(token: SessionToken, agreement: CustomerPriceAgreement): IO[BackofficeError, CustomerPriceAgreement]
  def deleteAgreement(token: SessionToken, target: AgreementTarget, currency: Currency): IO[BackofficeError, Unit]
  def listPromoCodes(token: SessionToken): IO[BackofficeError, List[PromoCode]]
  def savePromoCode(token: SessionToken, promo: PromoCode): IO[BackofficeError, PromoCode]
  def setPromoCodeActive(token: SessionToken, id: DiscountCodeId, active: Boolean): IO[BackofficeError, PromoCode]
  def deletePromoCode(token: SessionToken, id: DiscountCodeId): IO[BackofficeError, Unit]
  def listCostSheets(token: SessionToken): IO[BackofficeError, List[ProductionCostSheet]]
  def saveCostSheet(token: SessionToken, sheet: ProductionCostSheet): IO[BackofficeError, ProductionCostSheet]
  def simulateMargin(token: SessionToken, cmd: MarginSimulationCmd): IO[BackofficeError, MarginSimulationResult]

  // ── 7. Customer Management ─────────────────────────────────────────────────
  def listCustomers(token: SessionToken, filter: CustomerFilter = CustomerFilter()): IO[BackofficeError, List[CustomerView]]
  def getCustomerDetail(token: SessionToken, id: CustomerId): IO[BackofficeError, CustomerDetailView]
  def registerCustomer(token: SessionToken, cmd: RegisterCustomerCmd): IO[BackofficeError, CustomerView]
  def approveCustomer(token: SessionToken, id: CustomerId): IO[BackofficeError, CustomerView]
  def suspendCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): IO[BackofficeError, CustomerView]
  def deactivateCustomer(token: SessionToken, id: CustomerId, reason: NonEmptyString): IO[BackofficeError, CustomerView]
  def changeCustomerTier(token: SessionToken, id: CustomerId, tier: CustomerTier): IO[BackofficeError, CustomerView]
  def grantCustomerPaymentTerms(token: SessionToken, id: CustomerId, terms: PaymentTerms): IO[BackofficeError, CustomerView]
  def updateCustomerProfile(token: SessionToken, cmd: UpdateCustomerProfileCmd): IO[BackofficeError, CustomerView]
  def addCustomerNote(token: SessionToken, id: CustomerId, text: NonEmptyString): IO[BackofficeError, CustomerView]

  // ── 8. Staff Directory & Store Delivery / Bank Settings ────────────────────
  def listStaffMembers(token: SessionToken): IO[BackofficeError, List[StaffMemberView]]
  def createStaffMember(token: SessionToken, cmd: CreateStaffCmd): IO[BackofficeError, StaffMemberView]
  def updateStaffRoles(token: SessionToken, employeeId: EmployeeId, roles: Set[EmployeeRole]): IO[BackofficeError, StaffMemberView]
  def setStaffActive(token: SessionToken, employeeId: EmployeeId, isActive: Boolean): IO[BackofficeError, StaffMemberView]
  def resetStaffPassword(token: SessionToken, email: EmailAddress, newRawPassword: NonEmptyString): IO[BackofficeError, Unit]

  def getStoreDeliverySettings(token: SessionToken): IO[BackofficeError, StoreDeliverySettingsView]
  def upsertPickupLocation(token: SessionToken, loc: ShopPickupLocation): IO[BackofficeError, List[ShopPickupLocation]]
  def deletePickupLocation(token: SessionToken, id: DeliveryOptionId): IO[BackofficeError, List[ShopPickupLocation]]
  def upsertCourierOption(token: SessionToken, opt: CourierServiceOption): IO[BackofficeError, List[CourierServiceOption]]
  def deleteCourierOption(token: SessionToken, currency: Currency, id: DeliveryOptionId): IO[BackofficeError, List[CourierServiceOption]]
  def upsertBankAccount(token: SessionToken, currency: Currency, account: BankAccountConfig): IO[BackofficeError, Map[Currency, BankAccountConfig]]

object BackofficeFacade:
  val layer: ZLayer[
    CatalogService & PricingService & CustomerService & IdentityService & OrderService & CheckoutOptionsService & ArtworkService,
    Nothing,
    BackofficeFacade,
  ] = ZLayer.fromFunction(BackofficeFacadeLive.apply)

final class BackofficeFacadeLive(
    catalogService: CatalogService,
    pricingService: PricingService,
    customerService: CustomerService,
    identityService: IdentityService,
    orderService: OrderService,
    checkoutOptionsService: CheckoutOptionsService,
    artworkService: ArtworkService,
) extends BackofficeFacade:

  private val AnyStaffRoles: Set[EmployeeRole]     = EmployeeRole.values.toSet
  private val IntakeRoles: Set[EmployeeRole]       = Set(EmployeeRole.Operator, EmployeeRole.Manager, EmployeeRole.Admin)
  private val ManagerAdminRoles: Set[EmployeeRole] = Set(EmployeeRole.Manager, EmployeeRole.Admin)
  private val AdminOnlyRoles: Set[EmployeeRole]    = Set(EmployeeRole.Admin)

  private def authorize(
      token: SessionToken,
      allowedRoles: Set[EmployeeRole],
  ): IO[BackofficeError, Principal.Employee] =
    for
      session <- identityService
                   .validateSession(token)
                   .mapError(e => BackofficeError.Unauthenticated(e.message))
      emp     <- session.principal match
                   case e: Principal.Employee => ZIO.succeed(e)
                   case _                     => ZIO.fail(BackofficeError.NotAnEmployeeSession)
      _       <- ZIO
                   .fail(BackofficeError.Forbidden(allowedRoles, emp.roles))
                   .when(emp.roles.intersect(allowedRoles).isEmpty)
    yield emp

  // ── 1. Staff Authentication ────────────────────────────────────────────────

  override def loginStaff(
      email: EmailAddress,
      rawPassword: NonEmptyString,
  ): IO[BackofficeError, StaffAuthResult] =
    for
      session <- identityService
                   .loginWithPassword(email, rawPassword)
                   .mapError(BackofficeError.IdentityFailure.apply)
      emp     <- session.principal match
                   case e: Principal.Employee => ZIO.succeed(e)
                   case _ =>
                     identityService.logout(session.token) *>
                       ZIO.fail(BackofficeError.NotAnEmployeeSession)
    yield StaffAuthResult(session, emp)

  override def validateStaffSession(token: SessionToken): IO[BackofficeError, StaffAuthResult] =
    for
      session <- identityService
                   .validateSession(token)
                   .mapError(e => BackofficeError.Unauthenticated(e.message))
      emp     <- session.principal match
                   case e: Principal.Employee => ZIO.succeed(e)
                   case _                     => ZIO.fail(BackofficeError.NotAnEmployeeSession)
    yield StaffAuthResult(session, emp)

  override def logoutStaff(token: SessionToken): UIO[Unit] =
    identityService.logout(token)

  // ── 2. Dashboard Summary ───────────────────────────────────────────────────

  override def getDashboardSummary(token: SessionToken): IO[BackofficeError, BackofficeDashboardSummary] =
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
  ): IO[BackofficeError, List[OrderView]] =
    authorize(token, AnyStaffRoles) *> orderService.listOrders(filter)

  override def getOrder(token: SessionToken, orderId: OrderId): IO[BackofficeError, OrderView] =
    authorize(token, AnyStaffRoles) *>
      orderService.getOrder(orderId).mapError(BackofficeError.OrderFailure.apply)

  override def acceptOrder(
      token: SessionToken,
      orderId: OrderId,
      note: Option[NonEmptyString],
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .acceptOrder(orderId, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  override def putOrderOnHold(
      token: SessionToken,
      orderId: OrderId,
      reason: NonEmptyString,
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .putOnHold(orderId, emp.employeeId, Some(emp.displayName), reason)
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  override def resumeOrderFromHold(
      token: SessionToken,
      orderId: OrderId,
      note: Option[NonEmptyString],
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .resumeFromHold(orderId, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  override def advanceOrderStage(
      token: SessionToken,
      orderId: OrderId,
      target: OrderStatus,
      note: Option[NonEmptyString],
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .advanceStage(orderId, target, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  override def confirmOrderPayment(
      token: SessionToken,
      orderId: OrderId,
      note: Option[NonEmptyString],
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .confirmPaymentByStaff(orderId, emp.employeeId, Some(emp.displayName), note.map(_.value))
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  override def cancelOrder(
      token: SessionToken,
      orderId: OrderId,
      reason: NonEmptyString,
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, IntakeRoles)
      res <- orderService
               .cancelByStaff(orderId, emp.employeeId, Some(emp.displayName), reason)
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  override def addOrderInternalNote(
      token: SessionToken,
      orderId: OrderId,
      text: NonEmptyString,
  ): IO[BackofficeError, OrderView] =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- orderService
               .addInternalNote(orderId, emp.employeeId, Some(emp.displayName), text)
               .mapError(BackofficeError.OrderFailure.apply)
    yield res

  // ── 4. Artwork Review ──────────────────────────────────────────────────────

  override def listOrderArtwork(
      token: SessionToken,
      orderId: OrderId,
  ): IO[BackofficeError, List[ArtworkSubmissionView]] =
    for
      _     <- authorize(token, AnyStaffRoles)
      order <- orderService.getOrder(orderId).mapError(BackofficeError.OrderFailure.apply)
      byIdx <- ZIO.foreach(order.lines.toList.zipWithIndex) { case (_, idx) =>
                 artworkService.listByOwner(ArtworkOwner.OrderLineRef(orderId, idx))
               }
    yield byIdx.flatten

  override def approveArtwork(
      token: SessionToken,
      artworkId: ArtworkId,
      overrideFailures: Boolean,
  ): IO[BackofficeError, ArtworkSubmissionView] =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- artworkService
               .approve(artworkId, emp.employeeId, overrideFailures)
               .mapError(BackofficeError.ArtworkFailure.apply)
    yield res

  override def requestArtworkChanges(
      token: SessionToken,
      artworkId: ArtworkId,
      notes: NonEmptyString,
  ): IO[BackofficeError, ArtworkSubmissionView] =
    authorize(token, AnyStaffRoles) *>
      artworkService.requestChanges(artworkId, notes).mapError(BackofficeError.ArtworkFailure.apply)

  // ── 5. Catalog Management ──────────────────────────────────────────────────

  override def getCatalog(token: SessionToken): IO[BackofficeError, ProductCatalog] =
    authorize(token, AnyStaffRoles) *> catalogService.getCatalog

  override def getRuleset(token: SessionToken): IO[BackofficeError, CompatibilityRuleset] =
    authorize(token, AnyStaffRoles) *> catalogService.getRuleset

  override def upsertCategory(token: SessionToken, category: ProductCategory): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertCategory(category).mapError(BackofficeError.CatalogFailure.apply)

  override def deleteCategory(token: SessionToken, id: CategoryId): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteCategory(id).mapError(BackofficeError.CatalogFailure.apply)

  override def upsertMaterial(token: SessionToken, material: Material): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertMaterial(material).mapError(BackofficeError.CatalogFailure.apply)

  override def deleteMaterial(token: SessionToken, id: MaterialId): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteMaterial(id).mapError(BackofficeError.CatalogFailure.apply)

  override def upsertFinish(token: SessionToken, finish: Finish): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertFinish(finish).mapError(BackofficeError.CatalogFailure.apply)

  override def deleteFinish(token: SessionToken, id: FinishId): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteFinish(id).mapError(BackofficeError.CatalogFailure.apply)

  override def upsertPrintingMethod(token: SessionToken, method: PrintingMethod): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertPrintingMethod(method).mapError(BackofficeError.CatalogFailure.apply)

  override def deletePrintingMethod(token: SessionToken, id: PrintingMethodId): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deletePrintingMethod(id).mapError(BackofficeError.CatalogFailure.apply)

  override def upsertShowcaseListing(token: SessionToken, listing: ShowcaseListing): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertShowcaseListing(listing).mapError(BackofficeError.CatalogFailure.apply)

  override def deleteShowcaseListing(token: SessionToken, categoryId: CategoryId): IO[BackofficeError, ProductCatalog] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteShowcaseListing(categoryId).mapError(BackofficeError.CatalogFailure.apply)

  override def upsertRule(token: SessionToken, rule: CompatibilityRule): IO[BackofficeError, CompatibilityRuleset] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.upsertRule(rule).mapError(BackofficeError.CatalogFailure.apply)

  override def deleteRule(token: SessionToken, id: RuleId): IO[BackofficeError, CompatibilityRuleset] =
    authorize(token, ManagerAdminRoles) *>
      catalogService.deleteRule(id).mapError(BackofficeError.CatalogFailure.apply)

  // ── 6. Pricing, Agreements, Promos & Margin Simulation ─────────────────────

  override def listPricelists(token: SessionToken): IO[BackofficeError, List[Pricelist]] =
    authorize(token, AnyStaffRoles) *> pricingService.listPricelists

  override def savePricelist(token: SessionToken, pricelist: Pricelist): IO[BackofficeError, Pricelist] =
    authorize(token, ManagerAdminRoles) *>
      pricingService.savePricelist(pricelist).mapError(BackofficeError.PricingFailure.apply)

  override def listAgreements(token: SessionToken): IO[BackofficeError, List[CustomerPriceAgreement]] =
    authorize(token, AnyStaffRoles) *> pricingService.listAgreements

  override def saveAgreement(
      token: SessionToken,
      agreement: CustomerPriceAgreement,
  ): IO[BackofficeError, CustomerPriceAgreement] =
    authorize(token, ManagerAdminRoles) *>
      pricingService.saveAgreement(agreement).mapError(BackofficeError.PricingFailure.apply)

  override def deleteAgreement(
      token: SessionToken,
      target: AgreementTarget,
      currency: Currency,
  ): IO[BackofficeError, Unit] =
    authorize(token, ManagerAdminRoles) *> pricingService.deleteAgreement(target, currency)

  override def listPromoCodes(token: SessionToken): IO[BackofficeError, List[PromoCode]] =
    authorize(token, AnyStaffRoles) *> pricingService.listPromoCodes

  override def savePromoCode(token: SessionToken, promo: PromoCode): IO[BackofficeError, PromoCode] =
    authorize(token, ManagerAdminRoles) *>
      pricingService.savePromoCode(promo).mapError(BackofficeError.PricingFailure.apply)

  override def setPromoCodeActive(
      token: SessionToken,
      id: DiscountCodeId,
      active: Boolean,
  ): IO[BackofficeError, PromoCode] =
    authorize(token, ManagerAdminRoles) *>
      pricingService.setPromoCodeActive(id, active).mapError(BackofficeError.PricingFailure.apply)

  override def deletePromoCode(token: SessionToken, id: DiscountCodeId): IO[BackofficeError, Unit] =
    authorize(token, ManagerAdminRoles) *> pricingService.deletePromoCode(id)

  override def listCostSheets(token: SessionToken): IO[BackofficeError, List[ProductionCostSheet]] =
    authorize(token, AnyStaffRoles) *> pricingService.listCostSheets

  override def saveCostSheet(
      token: SessionToken,
      sheet: ProductionCostSheet,
  ): IO[BackofficeError, ProductionCostSheet] =
    authorize(token, ManagerAdminRoles) *>
      pricingService.saveCostSheet(sheet).mapError(BackofficeError.PricingFailure.apply)

  override def simulateMargin(
      token: SessionToken,
      cmd: MarginSimulationCmd,
  ): IO[BackofficeError, MarginSimulationResult] =
    for
      _      <- authorize(token, AnyStaffRoles)
      cat    <- catalogService.getCategory(cmd.categoryId).mapError(BackofficeError.CatalogFailure.apply)
      preset <- ZIO
                  .fromOption(cat.presets.find(_.id == cmd.presetId))
                  .orElseFail(BackofficeError.PresetNotFound(cmd.categoryId, cmd.presetId))
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
      cfgRes <- catalogService.configure(cfgCmd).mapError(BackofficeError.CatalogFailure.apply)
      subject = toPricingSubject(cfgRes.product, cmd.speed)
      qCmd    = PriceQuoteCmd(
                  quoteId = QuoteId.unsafe(s"sim-q-${cmd.presetId.value}"),
                  subject = subject,
                  currency = cmd.currency,
                  customerId = cmd.customerId,
                  customerTier = cmd.customerTier,
                  promoCode = cmd.promoCode,
                )
      quote  <- pricingService.quote(qCmd).mapError(BackofficeError.PricingFailure.apply)
      margin <- pricingService.analyzeMargin(qCmd).mapError(BackofficeError.PricingFailure.apply)
      sheets  = subject.components.map(_.sheetsUsed).sum
    yield MarginSimulationResult(quote, margin, sheets)

  // ── 7. Customer Management ─────────────────────────────────────────────────

  override def listCustomers(
      token: SessionToken,
      filter: CustomerFilter,
  ): IO[BackofficeError, List[CustomerView]] =
    authorize(token, AnyStaffRoles) *> customerService.listCustomers(filter)

  override def getCustomerDetail(
      token: SessionToken,
      id: CustomerId,
  ): IO[BackofficeError, CustomerDetailView] =
    for
      _          <- authorize(token, AnyStaffRoles)
      customer   <- customerService.getById(id).mapError(BackofficeError.CustomerFailure.apply)
      orders     <- orderService.listOrdersByCustomer(id)
      agreements <- pricingService.listAgreements.map(
                      _.filter(_.target == AgreementTarget.SpecificCustomer(id))
                    )
    yield CustomerDetailView(customer, orders, agreements)

  override def registerCustomer(
      token: SessionToken,
      cmd: RegisterCustomerCmd,
  ): IO[BackofficeError, CustomerView] =
    authorize(token, ManagerAdminRoles) *>
      customerService.register(cmd).mapError(BackofficeError.CustomerFailure.apply)

  override def approveCustomer(token: SessionToken, id: CustomerId): IO[BackofficeError, CustomerView] =
    authorize(token, ManagerAdminRoles) *>
      customerService.approve(id).mapError(BackofficeError.CustomerFailure.apply)

  override def suspendCustomer(
      token: SessionToken,
      id: CustomerId,
      reason: NonEmptyString,
  ): IO[BackofficeError, CustomerView] =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService.suspend(id, reason, Some(emp.employeeId)).mapError(BackofficeError.CustomerFailure.apply)
    yield res

  override def deactivateCustomer(
      token: SessionToken,
      id: CustomerId,
      reason: NonEmptyString,
  ): IO[BackofficeError, CustomerView] =
    for
      emp <- authorize(token, ManagerAdminRoles)
      res <- customerService.deactivate(id, reason, Some(emp.employeeId)).mapError(BackofficeError.CustomerFailure.apply)
    yield res

  override def changeCustomerTier(
      token: SessionToken,
      id: CustomerId,
      tier: CustomerTier,
  ): IO[BackofficeError, CustomerView] =
    authorize(token, ManagerAdminRoles) *>
      customerService.changeTier(id, tier).mapError(BackofficeError.CustomerFailure.apply)

  override def grantCustomerPaymentTerms(
      token: SessionToken,
      id: CustomerId,
      terms: PaymentTerms,
  ): IO[BackofficeError, CustomerView] =
    authorize(token, ManagerAdminRoles) *>
      customerService.grantPaymentTerms(id, terms).mapError(BackofficeError.CustomerFailure.apply)

  override def updateCustomerProfile(
      token: SessionToken,
      cmd: UpdateCustomerProfileCmd,
  ): IO[BackofficeError, CustomerView] =
    authorize(token, ManagerAdminRoles) *>
      customerService.updateProfile(cmd).mapError(BackofficeError.CustomerFailure.apply)

  override def addCustomerNote(
      token: SessionToken,
      id: CustomerId,
      text: NonEmptyString,
  ): IO[BackofficeError, CustomerView] =
    for
      emp <- authorize(token, AnyStaffRoles)
      res <- customerService.addNote(id, text, Some(emp.employeeId)).mapError(BackofficeError.CustomerFailure.apply)
    yield res

  // ── 8. Staff Directory & Store Delivery / Bank Settings ────────────────────

  override def listStaffMembers(token: SessionToken): IO[BackofficeError, List[StaffMemberView]] =
    authorize(token, ManagerAdminRoles) *> identityService.listStaffMembers

  override def createStaffMember(
      token: SessionToken,
      cmd: CreateStaffCmd,
  ): IO[BackofficeError, StaffMemberView] =
    authorize(token, AdminOnlyRoles) *>
      identityService.createStaffMember(cmd).mapError(BackofficeError.IdentityFailure.apply)

  override def updateStaffRoles(
      token: SessionToken,
      employeeId: EmployeeId,
      roles: Set[EmployeeRole],
  ): IO[BackofficeError, StaffMemberView] =
    authorize(token, AdminOnlyRoles) *>
      identityService.updateStaffRoles(employeeId, roles).mapError(BackofficeError.IdentityFailure.apply)

  override def setStaffActive(
      token: SessionToken,
      employeeId: EmployeeId,
      isActive: Boolean,
  ): IO[BackofficeError, StaffMemberView] =
    authorize(token, AdminOnlyRoles) *>
      identityService.setStaffActive(employeeId, isActive).mapError(BackofficeError.IdentityFailure.apply)

  override def resetStaffPassword(
      token: SessionToken,
      email: EmailAddress,
      newRawPassword: NonEmptyString,
  ): IO[BackofficeError, Unit] =
    authorize(token, AdminOnlyRoles) *>
      identityService.resetPassword(email, newRawPassword).mapError(BackofficeError.IdentityFailure.apply)

  override def getStoreDeliverySettings(token: SessionToken): IO[BackofficeError, StoreDeliverySettingsView] =
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
  ): IO[BackofficeError, List[ShopPickupLocation]] =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.upsertPickupLocation(loc) *>
      checkoutOptionsService.listAllPickupLocations

  override def deletePickupLocation(
      token: SessionToken,
      id: DeliveryOptionId,
  ): IO[BackofficeError, List[ShopPickupLocation]] =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.deletePickupLocation(id) *>
      checkoutOptionsService.listAllPickupLocations

  override def upsertCourierOption(
      token: SessionToken,
      opt: CourierServiceOption,
  ): IO[BackofficeError, List[CourierServiceOption]] =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.upsertCourierOption(opt) *>
      checkoutOptionsService.listAllCourierOptions.map(_.filter(_.currency == opt.currency))

  override def deleteCourierOption(
      token: SessionToken,
      currency: Currency,
      id: DeliveryOptionId,
  ): IO[BackofficeError, List[CourierServiceOption]] =
    authorize(token, ManagerAdminRoles) *>
      checkoutOptionsService.deleteCourierOption(id, currency) *>
      checkoutOptionsService.listAllCourierOptions.map(_.filter(_.currency == currency))

  override def upsertBankAccount(
      token: SessionToken,
      currency: Currency,
      account: BankAccountConfig,
  ): IO[BackofficeError, Map[Currency, BankAccountConfig]] =
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
