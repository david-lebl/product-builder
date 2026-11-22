package mpbuilder.sample

import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.customers.*
import mpbuilder.customers.sample.SampleCustomers
import mpbuilder.identity.*
import mpbuilder.identity.sample.SampleIdentity
import mpbuilder.ordering.*
import mpbuilder.ordering.sample.SampleDeliveryCatalog
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import zio.*
import zio.test.*

object BackofficeFacadeSpec extends ZIOSpecDefault:

  private val adminEmail    = SampleIdentity.adminEmployeePrincipal.email
  private val managerEmail  = SampleIdentity.managerEmployeePrincipal.email
  private val prepressEmail = SampleIdentity.prepressEmployeePrincipal.email
  private val staffPass     = SampleIdentity.demoEmployeePassword

  private val customerEmail = EmailAddress.unsafe("jan@printshoppro.cz")
  private val customerPass  = SampleIdentity.demoCustomerPassword

  def spec = suite("BackofficeFacadeSpec")(
    test("enforces staff authentication and RBAC across Customer, Prepress, Manager, and Admin roles") {
      for
        sf       <- ZIO.service[StorefrontFacade]
        bo       <- ZIO.service[BackofficeFacade]
        // Customer session must be rejected by Backoffice
        custAuth <- sf.loginWithPassword(customerEmail, customerPass)
        custDash <- bo.getDashboardSummary(custAuth.session.token).either
        custLogin <- bo.loginStaff(customerEmail, customerPass).either

        // Invalid token rejected
        badTok   <- bo.getDashboardSummary(SessionToken.unsafe("non-existent-token")).either

        // Prepress: can view dashboard/orders and add internal note, CANNOT accept order / mutate catalog / manage staff
        prepAuth <- bo.loginStaff(prepressEmail, staffPass)
        prepDash <- bo.getDashboardSummary(prepAuth.session.token)
        orders   <- bo.listOrders(prepAuth.session.token)
        firstOrd  = orders.head
        prepAcc  <- bo.acceptOrder(prepAuth.session.token, firstOrd.id, None).either
        prepCat  <- bo.deleteMaterial(prepAuth.session.token, MaterialId.unsafe("non-existent")).either
        prepNote <- bo.addOrderInternalNote(prepAuth.session.token, firstOrd.id, NonEmptyString.unsafe("Checked fonts"))

        // Manager: CAN list staff and edit catalog/customers, CANNOT create staff (Admin-only)
        mgrAuth  <- bo.loginStaff(managerEmail, staffPass)
        staff    <- bo.listStaffMembers(mgrAuth.session.token)
        mgrAdd   <- bo
                      .createStaffMember(
                        mgrAuth.session.token,
                        CreateStaffCmd(
                          EmployeeId.unsafe("emp-99"),
                          NonEmptyString.unsafe("Unauth New"),
                          EmailAddress.unsafe("unauth@printshop.cz"),
                          Set(EmployeeRole.Operator),
                          NonEmptyString.unsafe("pass1234"),
                        ),
                      )
                      .either

        // Admin: CAN create staff, update roles, deactivate
        admAuth  <- bo.loginStaff(adminEmail, staffPass)
        created  <- bo.createStaffMember(
                      admAuth.session.token,
                      CreateStaffCmd(
                        EmployeeId.unsafe("emp-10"),
                        NonEmptyString.unsafe("Jana Novotná"),
                        EmailAddress.unsafe("jana@printshop.cz"),
                        Set(EmployeeRole.Operator),
                        NonEmptyString.unsafe("newpass123"),
                      ),
                    )
        janaAuth <- bo.loginStaff(EmailAddress.unsafe("jana@printshop.cz"), NonEmptyString.unsafe("newpass123"))
        promoted <- bo.updateStaffRoles(admAuth.session.token, created.employeeId, Set(EmployeeRole.Operator, EmployeeRole.Prepress))
        disabled <- bo.setStaffActive(admAuth.session.token, created.employeeId, isActive = false)
        blocked  <- bo.loginStaff(EmailAddress.unsafe("jana@printshop.cz"), NonEmptyString.unsafe("newpass123")).either
      yield assertTrue(
        custDash == Left(BackofficeError.NotAnEmployeeSession),
        custLogin == Left(BackofficeError.NotAnEmployeeSession),
        badTok.isLeft,
        prepDash.totalOrders >= 4,
        prepAcc.left.exists(_.isInstanceOf[BackofficeError.Forbidden]),
        prepCat.left.exists(_.isInstanceOf[BackofficeError.Forbidden]),
        prepNote.internalNotes.exists(_.text.value == "Checked fonts"),
        staff.size == 3,
        mgrAdd.left.exists(_.isInstanceOf[BackofficeError.Forbidden]),
        janaAuth.employee.employeeId == created.employeeId,
        promoted.roles == Set(EmployeeRole.Operator, EmployeeRole.Prepress),
        !disabled.isActive,
        blocked.isLeft,
      )
    }.provide(SampleShowcaseRuntime.layer()),

    test("E2E Storefront checkout immediately lands in Backoffice Order Intake and flows through full manual lifecycle") {
      for
        sf      <- ZIO.service[StorefrontFacade]
        bo      <- ZIO.service[BackofficeFacade]
        admAuth <- bo.loginStaff(adminEmail, staffPass)

        // 1. Customer configures Business Cards preset and submits checkout via StorefrontFacade
        cat     <- sf.getCatalog
        bcCat    = cat.categories(SampleCatalog.businessCardsId)
        preset   = bcCat.presets.head
        quoteRes <- sf.configureAndQuote(
                      cmd = StorefrontTranslators.presetToConfigureCmd(
                        ConfigurationId.unsafe("cfg-e2e-intake"),
                        bcCat.id,
                        preset,
                      ),
                      currency = Currency.CZK,
                      speed = ManufacturingSpeed.Standard,
                      sessionToken = None,
                      lang = Language.En,
                    )
        line     = StorefrontTranslators.toCartLine(
                     lineId = OrderLineId.unsafe("line-e2e-1"),
                     product = quoteRes.configuration.product,
                     speed = ManufacturingSpeed.Standard,
                     quote = quoteRes.pricing.effectiveQuote,
                   )
        confirm <- sf.submitCheckout(
                     SubmitCheckoutCmd(
                       sessionToken = None,
                       source = CheckoutSource.QuickSingleLine(line),
                       customer = OrderCustomerSnapshot(
                         customerId = None,
                         segment = CustomerSegment.Individual,
                         fullName = NonEmptyString.unsafe("Tereza Horáková"),
                         email = EmailAddress.unsafe("tereza@example.cz"),
                       ),
                       billingAddress = Address(
                         NonEmptyString.unsafe("Spálená 12"),
                         NonEmptyString.unsafe("Praha"),
                         NonEmptyString.unsafe("11000"),
                         NonEmptyString.unsafe("CZ"),
                       ),
                       deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                       paymentMethod = PaymentMethod.BankTransferQR,
                       customerNote = Some("Call when ready"),
                     )
                   )
        orderId  = confirm.order.id

        // 2. Staff sees it in Order Intake queue immediately
        intake   <- bo.listOrders(admAuth.session.token, OrderFilter(query = Some("Tereza")))
        accepted <- bo.acceptOrder(admAuth.session.token, orderId, Some(NonEmptyString.unsafe("Artwork OK")))
        paid     <- bo.confirmOrderPayment(admAuth.session.token, orderId, Some(NonEmptyString.unsafe("TX-5501")))
        held     <- bo.putOrderOnHold(admAuth.session.token, orderId, NonEmptyString.unsafe("Confirming paper finish"))
        resumed  <- bo.resumeOrderFromHold(admAuth.session.token, orderId, Some(NonEmptyString.unsafe("Customer confirmed silk")))
        inProd   <- bo.advanceOrderStage(admAuth.session.token, orderId, OrderStatus.InProduction, None)
        ready    <- bo.advanceOrderStage(admAuth.session.token, orderId, OrderStatus.ReadyForDispatch, None)
        done     <- bo.advanceOrderStage(admAuth.session.token, orderId, OrderStatus.Completed, Some(NonEmptyString.unsafe("Picked up")))
      yield assertTrue(
        intake.exists(_.id == orderId),
        accepted.status == OrderStatus.Accepted,
        paid.payment.status == PaymentStatus.Confirmed,
        held.status.isInstanceOf[OrderStatus.OnHold],
        resumed.status == OrderStatus.Accepted,
        inProd.status == OrderStatus.InProduction,
        ready.status == OrderStatus.ReadyForDispatch,
        done.status == OrderStatus.Completed,
        done.statusHistory.size == 8,
      )
    }.provide(SampleShowcaseRuntime.layer()),

    test("Catalog CRUD enforces EntityInUse protection and reflects live edits in Storefront") {
      for
        sf       <- ZIO.service[StorefrontFacade]
        bo       <- ZIO.service[BackofficeFacade]
        mgrAuth  <- bo.loginStaff(managerEmail, staffPass)
        initial  <- bo.getCatalog(mgrAuth.session.token)
        // Deleting an actively used material must fail with EntityInUse
        inUseId   = initial.categories.values.head.components.head.allowedMaterialIds.head
        blocked  <- bo.deleteMaterial(mgrAuth.session.token, inUseId).either

        // Upserting a new material bumps version and is visible in Storefront immediately
        newMatId  = MaterialId.unsafe("mat-bamboo-300")
        baseMat   = initial.materials.values.head
        newMat    = baseMat.copy(id = newMatId, name = LocalizedString("Bamboo Paper 300g", "Bambusový papír 300g"))
        afterAdd <- bo.upsertMaterial(mgrAuth.session.token, newMat)
        sfCat    <- sf.getCatalog
        afterDel <- bo.deleteMaterial(mgrAuth.session.token, newMatId)
      yield assertTrue(
        blocked.left.exists {
          case BackofficeError.CatalogFailure(_: CatalogError.EntityInUse) => true
          case _                                                           => false
        },
        afterAdd.materials.contains(newMatId),
        sfCat.materials.contains(newMatId),
        afterAdd.version != initial.version,
        !afterDel.materials.contains(newMatId),
      )
    }.provide(SampleShowcaseRuntime.layer()),

    test("Pricing CRUD, Promo Codes, live Margin Simulation, Customer Approval, and Store Settings work end-to-end") {
      for
        bo       <- ZIO.service[BackofficeFacade]
        mgrAuth  <- bo.loginStaff(managerEmail, staffPass)
        tok       = mgrAuth.session.token

        // 1. Promo code CRUD
        promos0  <- bo.listPromoCodes(tok)
        newPromo  = PromoCode(
                      id = DiscountCodeId.unsafe("promo-summer-26"),
                      code = NonEmptyString.unsafe("SUMMER26"),
                      benefit = PromoBenefit.PercentageOff(Percentage.unsafe(BigDecimal(15))),
                      constraints = PromoConstraints(),
                      isActive = true,
                    )
        _        <- bo.savePromoCode(tok, newPromo)
        toggled  <- bo.setPromoCodeActive(tok, newPromo.id, active = false)
        promos1  <- bo.listPromoCodes(tok)
        _        <- bo.deletePromoCode(tok, newPromo.id)

        // 2. Margin simulation on Business Cards preset
        cat      <- bo.getCatalog(tok)
        bcCat     = cat.categories(SampleCatalog.businessCardsId)
        sim      <- bo.simulateMargin(
                      tok,
                      MarginSimulationCmd(
                        categoryId = bcCat.id,
                        presetId = bcCat.presets.head.id,
                        quantity = Quantity.unsafe(500),
                        currency = Currency.CZK,
                      ),
                    )

        // 3. Customer approval + tier + NetInvoice terms + note
        pending  <- bo.listCustomers(tok, CustomerFilter(status = Some(CustomerStatus.PendingApproval)))
        pendId    = SampleCustomers.pendingCoId
        approved <- bo.approveCustomer(tok, pendId)
        gold     <- bo.changeCustomerTier(tok, pendId, CustomerTier.Gold)
        credited <- bo.grantCustomerPaymentTerms(tok, pendId, PaymentTerms.NetInvoice(14, Price(Money("50000"), Currency.CZK)))
        noted    <- bo.addCustomerNote(tok, pendId, NonEmptyString.unsafe("Verified VAT registration"))
        detail   <- bo.getCustomerDetail(tok, pendId)

        // 4. Store delivery & bank settings CRUD
        settings0 <- bo.getStoreDeliverySettings(tok)
        newShop    = ShopPickupLocation(
                       id = DeliveryOptionId.unsafe("shop-plzen"),
                       name = LocalizedString("Plzeň — Republic Square 2", "Plzeň — náměstí Republiky 2"),
                       address = LocalizedString("Republic Square 2, 301 00 Plzeň", "náměstí Republiky 2, 301 00 Plzeň"),
                     )
        shops1    <- bo.upsertPickupLocation(tok, newShop)
        shops2    <- bo.deletePickupLocation(tok, newShop.id)
        banks1    <- bo.upsertBankAccount(
                       tok,
                       Currency.EUR,
                       BankAccountConfig(
                         iban = NonEmptyString.unsafe("DE89370400440532013000"),
                         bic = Some(NonEmptyString.unsafe("COBADEFFXXX")),
                         accountHolder = NonEmptyString.unsafe("MP PrintShop s.r.o."),
                       ),
                     )
      yield assertTrue(
        promos1.size == promos0.size + 1,
        !toggled.isActive,
        sim.quote.netTotal.value > BigDecimal(0),
        sim.margin.productionCostNet.value >= BigDecimal(0),
        pending.exists(_.id == pendId),
        approved.status == CustomerStatus.Active,
        gold.tier == CustomerTier.Gold,
        credited.paymentTerms.isInstanceOf[PaymentTerms.NetInvoice],
        noted.notes.exists(_.text.value == "Verified VAT registration"),
        detail.customer.id == pendId,
        shops1.size == settings0.pickupLocations.size + 1,
        shops2.size == settings0.pickupLocations.size,
        banks1.contains(Currency.EUR),
      )
    }.provide(SampleShowcaseRuntime.layer()),
  )
