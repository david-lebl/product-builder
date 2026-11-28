package mpbuilder.sample

import mpbuilder.artwork.*
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
import kyo.*
import kyo.test.*
import ShowcaseTestEnv.*

class BackofficeFacadeSpec extends Test[Any]:

  private val adminEmail    = SampleIdentity.adminEmployeePrincipal.email
  private val managerEmail  = SampleIdentity.managerEmployeePrincipal.email
  private val prepressEmail = SampleIdentity.prepressEmployeePrincipal.email
  private val staffPass     = SampleIdentity.demoEmployeePassword

  private val customerEmail = EmailAddress.unsafe("jan@printshoppro.cz")
  private val customerPass  = SampleIdentity.demoCustomerPassword

  "BackofficeFacadeSpec" - {
    "enforces staff authentication and RBAC across Customer, Prepress, Manager, and Admin roles" in {
      for
        sf       <- Env.get[StorefrontFacade]
        bo       <- Env.get[BackofficeFacade]
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
      yield {
        assert(custDash == Left(BackofficeError.NotAnEmployeeSession))
        assert(custLogin == Left(BackofficeError.NotAnEmployeeSession))
        assert(badTok.isLeft)
        assert(prepDash.totalOrders >= 4)
        assert(prepAcc.left.exists(_.isInstanceOf[BackofficeError.Forbidden]))
        assert(prepCat.left.exists(_.isInstanceOf[BackofficeError.Forbidden]))
        assert(prepNote.internalNotes.exists(_.text.value == "Checked fonts"))
        assert(staff.size == 3)
        assert(mgrAdd.left.exists(_.isInstanceOf[BackofficeError.Forbidden]))
        assert(janaAuth.employee.employeeId == created.employeeId)
        assert(promoted.roles == Set(EmployeeRole.Operator, EmployeeRole.Prepress))
        assert(!disabled.isActive)
        assert(blocked.isLeft)
      }
    }.provideRuntime()

    "E2E Storefront checkout immediately lands in Backoffice Order Intake and flows through full manual lifecycle" in {
      for
        sf      <- Env.get[StorefrontFacade]
        bo      <- Env.get[BackofficeFacade]
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
        item     = QuickOrderItem(
                     cmd = StorefrontTranslators.presetToConfigureCmd(
                       ConfigurationId.unsafe("cfg-e2e-intake"),
                       bcCat.id,
                       preset,
                     ),
                     currency = Currency.CZK,
                   )
        confirm <- sf.submitCheckout(
                     SubmitCheckoutCmd(
                       sessionToken = None,
                       source = CheckoutSource.QuickSingleLine(item),
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
                       termsAccepted = true,
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
      yield {
        assert(intake.exists(_.id == orderId))
        assert(accepted.status == OrderStatus.Accepted)
        assert(paid.payment.status == PaymentStatus.Confirmed)
        assert(held.status.isInstanceOf[OrderStatus.OnHold])
        assert(resumed.status == OrderStatus.Accepted)
        assert(inProd.status == OrderStatus.InProduction)
        assert(ready.status == OrderStatus.ReadyForDispatch)
        assert(done.status == OrderStatus.Completed)
        assert(done.statusHistory.size == 8)
      }
    }.provideRuntime()

    "Catalog CRUD enforces EntityInUse protection and reflects live edits in Storefront" in {
      for
        sf       <- Env.get[StorefrontFacade]
        bo       <- Env.get[BackofficeFacade]
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
      yield {
        assert(blocked.left.exists {
          case BackofficeError.CatalogFailure(_: CatalogError.EntityInUse) => true
          case _                                                           => false
        })
        assert(afterAdd.materials.contains(newMatId))
        assert(sfCat.materials.contains(newMatId))
        assert(afterAdd.version != initial.version)
        assert(!afterDel.materials.contains(newMatId))
      }
    }.provideRuntime()

    "Pricing CRUD, Promo Codes, live Margin Simulation, Customer Approval, and Store Settings work end-to-end" in {
      for
        bo       <- Env.get[BackofficeFacade]
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
      yield {
        assert(promos1.size == promos0.size + 1)
        assert(!toggled.isActive)
        assert(sim.quote.netTotal.value > BigDecimal(0))
        assert(sim.margin.productionCostNet.value >= BigDecimal(0))
        assert(pending.exists(_.id == pendId))
        assert(approved.status == CustomerStatus.Active)
        assert(gold.tier == CustomerTier.Gold)
        assert(credited.paymentTerms.isInstanceOf[PaymentTerms.NetInvoice])
        assert(noted.notes.exists(_.text.value == "Verified VAT registration"))
        assert(detail.customer.id == pendId)
        assert(shops1.size == settings0.pickupLocations.size + 1)
        assert(shops2.size == settings0.pickupLocations.size)
        assert(banks1.contains(Currency.EUR))
      }
    }.provideRuntime()

    "Customer multi-contacts & audit trail, Promo createdAt/lifecycle, Order priority/deadline/payment/line-edit, and Artwork review" in {
      for
        bo       <- Env.get[BackofficeFacade]
        mgrAuth  <- bo.loginStaff(managerEmail, staffPass)
        tok       = mgrAuth.session.token

        // 1. Business customer profile edit with multiple contacts records employee audit entry
        pspBefore <- bo.getCustomerDetail(tok, SampleCustomers.printShopProId)
        extraC     = ContactPerson(
                       NonEmptyString.unsafe("Eva"),
                       NonEmptyString.unsafe("Nováková"),
                       EmailAddress.unsafe("eva@printshoppro.cz"),
                       Some(NonEmptyString.unsafe("+420 777 111 222")),
                       Some(NonEmptyString.unsafe("Billing Lead")),
                     )
        psp        = pspBefore.customer
        edited    <- bo.updateCustomerProfile(
                       tok,
                       UpdateCustomerProfileCmd(
                         id = psp.id,
                         company = psp.company,
                         contact = psp.contact,
                         billingAddress = psp.billingAddress,
                         tags = psp.tags,
                         additionalContacts = Some(psp.additionalContacts :+ extraC),
                       ),
                     )
        // Individual customer rejects additional contacts
        indivCust <- bo.registerCustomer(
                       tok,
                       RegisterCustomerCmd(
                         id = CustomerId.unsafe("cust-indiv-test"),
                         segment = CustomerSegment.Individual,
                         company = None,
                         contact = ContactPerson(
                           NonEmptyString.unsafe("Anna"),
                           NonEmptyString.unsafe("Svobodová"),
                           EmailAddress.unsafe("anna.indiv@example.cz"),
                         ),
                         billingAddress = psp.billingAddress,
                       ),
                     )
        indivBad  <- bo
                       .updateCustomerProfile(
                         tok,
                         UpdateCustomerProfileCmd(
                           id = indivCust.id,
                           company = indivCust.company,
                           contact = indivCust.contact,
                           billingAddress = indivCust.billingAddress,
                           tags = indivCust.tags,
                           additionalContacts = Some(List(extraC)),
                         ),
                       )
                       .either

        // 2. Promo codes newest-first ordering + lifecycle classification
        now       <- Effects.now
        freshPromo = PromoCode(
                       id = DiscountCodeId.unsafe("promo-newest-99"),
                       code = NonEmptyString.unsafe("NEWEST99"),
                       benefit = PromoBenefit.PercentageOff(Percentage.unsafe(BigDecimal(10))),
                       constraints = PromoConstraints(validUntil = Some(now.plusSeconds(86400L * 30))),
                       isActive = true,
                     )
        _         <- bo.savePromoCode(tok, freshPromo)
        promos    <- bo.listPromoCodes(tok)
        statuses   = promos.map(_.lifecycleStatus(now)).toSet

        // 3. Order priority, deadline, decoupled payment toggle, line editing & artwork reject
        ord2      <- bo.getOrder(tok, OrderId.unsafe("ord-seed-0902"))
        // Mark payment Confirmed without changing stage, then toggle back to Pending
        paidOrd   <- bo.markOrderPaymentStatus(tok, ord2.id, PaymentStatus.Confirmed, Some(NonEmptyString.unsafe("Manual bank match")))
        unpaidOrd <- bo.markOrderPaymentStatus(tok, ord2.id, PaymentStatus.Pending, Some(NonEmptyString.unsafe("Reverted")))
        // Edit line quantity, speed, and specNote
        line0      = ord2.lines.head
        editedOrd <- bo.editOrderLine(
                       tok,
                       EditOrderLineCmd(
                         orderId = ord2.id,
                         lineId = line0.lineId,
                         copies = Some(Quantity.unsafe(2)),
                         speed = Some(ManufacturingSpeed.Economy),
                         specNote = Some(Some("Use Pantone 286C match")),
                       ),
                     )
        // Artwork list & reject
        arts      <- bo.listOrderArtwork(tok, OrderId.unsafe("ord-seed-0901"))
        rejected  <- bo.rejectArtwork(tok, arts.head.id, NonEmptyString.unsafe("Corrupted PDF"))
      yield {
        assert(edited.additionalContacts.exists(_.email.value == "eva@printshoppro.cz"))
        assert(edited.auditLog.exists { e =>
          e.action == "ProfileUpdated" && e.actor.isInstanceOf[CustomerActor.Employee]
        })
        assert(indivBad.left.exists {
          case BackofficeError.CustomerFailure(_: CustomerError.MultipleContactsNotAllowed) => true
          case _                                                                            => false
        })
        assert(promos.head.code.value == "NEWEST99")
        assert(statuses.contains(PromoLifecycleStatus.Active))
        assert(statuses.contains(PromoLifecycleStatus.Expired))
        assert(statuses.contains(PromoLifecycleStatus.Exhausted))
        assert(statuses.contains(PromoLifecycleStatus.Inactive))
        assert(ord2.priority == OrderPriority.Urgent)
        assert(ord2.calculatedDeadline == ord2.placedAt.plusSeconds(24L * 3600L))
        assert(paidOrd.payment.status == PaymentStatus.Confirmed)
        assert(paidOrd.status == ord2.status)
        assert(unpaidOrd.payment.status == PaymentStatus.Pending)
        assert(editedOrd.priority == OrderPriority.Economy)
        assert(editedOrd.lines.head.copies.value == 2)
        assert(editedOrd.lines.head.specNote.contains("Use Pantone 286C match"))
        assert(editedOrd.totals.linesNetTotal.value > ord2.totals.linesNetTotal.value)
        assert(rejected.status == ArtworkStatus.Rejected(NonEmptyString.unsafe("Corrupted PDF")))
      }
    }.provideRuntime().atTime(java.time.Instant.parse("2026-07-01T12:00:00Z"))

    "Customer self-service address editing, payment instructions re-view, and artwork re-upload" in {
      for
        sf      <- Env.get[StorefrontFacade]
        auth    <- sf.loginWithPassword(customerEmail, customerPass)
        tok      = auth.session.token
        cust0    = auth.customer.get

        // 1. Update billing + multiple delivery addresses with default index
        newBill  = cust0.billingAddress.copy(street = NonEmptyString.unsafe("Na Příkopě 22"))
        deliv1   = Address(NonEmptyString.unsafe("Křižíkova 10"), NonEmptyString.unsafe("Praha"), NonEmptyString.unsafe("18600"), NonEmptyString.unsafe("CZ"))
        deliv2   = Address(NonEmptyString.unsafe("Masarykova 5"), NonEmptyString.unsafe("Brno"), NonEmptyString.unsafe("60200"), NonEmptyString.unsafe("CZ"))
        updated <- sf.updateMyAddresses(tok, newBill, List(deliv1, deliv2), Some(1))

        // 2. View payment instructions for unpaid order ord-seed-0902
        orders  <- sf.listMyOrders(tok)
        ord2     = orders.find(_.id == OrderId.unsafe("ord-seed-0902")).get
        instr   <- sf.getOrderPaymentInstructions(tok, ord2.id)
        instrOk  = instr match
                     case PaymentInstructions.InvoiceScheduled(due, ref)            => due > 0 && ref.value.nonEmpty
                     case PaymentInstructions.BankTransferQR(_, _, _, _, amt, _, _) => amt == ord2.totals.grandTotalGross
                     case PaymentInstructions.CardPending(_, msg)                   => msg.nonEmpty

        // 3. List order artworks and re-upload corrected revision for ChangesRequested artwork
        arts0   <- sf.listOrderArtworks(tok, ord2.id)
        art0     = arts0.head
        art1    <- sf.reuploadOrderArtwork(
                     sessionToken = tok,
                     orderId = ord2.id,
                     artworkId = art0.id,
                     fileName = NonEmptyString.unsafe("printshoppro-a5-flyers-cmyk-v2.pdf"),
                     resolutionDpi = 300,
                     bleedMm = 3,
                     colorSpace = ColorSpace.CMYK,
                   )
      yield {
        assert(updated.billingAddress.street.value == "Na Příkopě 22")
        assert(updated.shippingAddresses.size == 2)
        assert(updated.defaultShippingIndex.contains(1))
        assert(updated.defaultShippingAddress.contains(deliv2))
        assert(updated.auditLog.exists(_.actor.isInstanceOf[CustomerActor.CustomerSelf]))
        assert(instrOk)
        assert(art0.status.isInstanceOf[ArtworkStatus.ChangesRequested])
        assert(art1.revisionCount == art0.revisionCount + 1)
        assert(art1.status == ArtworkStatus.PreflightPassed)
      }
    }.provideRuntime()

    "Staff artwork management: upload revision, upload missing order-line artwork, rerun preflight, and customer detail artworks tab data" in {
      for
        bo       <- Env.get[BackofficeFacade]
        auth     <- bo.loginStaff(adminEmail, staffPass)
        tok       = auth.session.token

        // 1. Seeded customer Print Shop Pro has aggregated artworks across their seeded orders with URLs & createdAt
        custDet  <- bo.getCustomerDetail(tok, SampleCustomers.printShopProId)
        ord2Id    = OrderId.unsafe("ord-seed-0902")
        arts0    <- bo.listOrderArtwork(tok, ord2Id)
        art0      = arts0.head

        // 2. Staff uploads a prepress-fixed revision onto existing artwork -> increments revision & passes preflight
        fixedRev <- bo.uploadStaffArtworkRevision(
                      token = tok,
                      orderId = ord2Id,
                      artworkId = art0.id,
                      payload = StaffArtworkUploadPayload(
                        fileName = NonEmptyString.unsafe("prepress-fixed-v3.pdf"),
                        mimeType = NonEmptyString.unsafe("application/pdf"),
                        sizeBytes = 3_200_000L,
                        downloadUrl = Some(NonEmptyString.unsafe("https://assets.printcraft.demo/prepress-fixed-v3.pdf")),
                        previewUrl = Some(NonEmptyString.unsafe("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=")),
                        resolutionDpi = 300,
                        bleedMm = 3,
                        colorSpace = ColorSpace.CMYK,
                        pageCount = Some(art0.latestRevision.extractedSpecs.pageCount),
                        fontsEmbedded = true,
                      ),
                    )

        // 3. Re-run preflight creates a fresh re-evaluated revision cleanly
        rerun    <- bo.rerunArtworkPreflight(tok, ord2Id, art0.id)

        // 4. Staff uploads a new artwork directly onto an order line component
        added    <- bo.uploadStaffOrderLineArtwork(
                      token = tok,
                      orderId = ord2Id,
                      lineIndex = 0,
                      componentKey = ComponentKey.Cover,
                      payload = StaffArtworkUploadPayload(
                        fileName = NonEmptyString.unsafe("cover-spot-uv.pdf"),
                        mimeType = NonEmptyString.unsafe("application/pdf"),
                        sizeBytes = 1_500_000L,
                        downloadUrl = Some(NonEmptyString.unsafe("https://assets.printcraft.demo/cover-spot-uv.pdf")),
                        resolutionDpi = 300,
                        bleedMm = 3,
                        colorSpace = ColorSpace.CMYK,
                        pageCount = Some(2),
                      ),
                    )

        // 5. Customer detail artworks list now includes the newly uploaded cover artwork
        custDet2 <- bo.getCustomerDetail(tok, SampleCustomers.printShopProId)
      yield {
        assert(custDet.artworks.nonEmpty)
        assert(custDet.artworks.forall(e => e.orderNumber.value.nonEmpty && e.artwork.latestRevision.file.shareableUrl.startsWith("https://")))
        assert(custDet.artworks.forall(e => e.artwork.createdAt != java.time.Instant.EPOCH))
        assert(fixedRev.revisionCount == art0.revisionCount + 1)
        assert(fixedRev.allRevisions.size == fixedRev.revisionCount)
        assert(fixedRev.status == ArtworkStatus.PreflightPassed)
        assert(rerun.revisionCount == fixedRev.revisionCount)
        assert(rerun.latestRevision.preflight.exists(_.isCleanPass))
        assert(rerun.status == ArtworkStatus.PreflightPassed)
        assert(added.componentKey == ComponentKey.Cover)
        assert(custDet2.artworks.exists(_.artwork.id == added.id))
      }
    }.provideRuntime()
  }
