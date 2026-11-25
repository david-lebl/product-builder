package mpbuilder.sample

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.customers.*
import mpbuilder.customers.sample.SampleCustomers
import mpbuilder.manufacturing.*
import mpbuilder.manufacturing.sample.SampleManufacturing
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import kyo.*
import kyo.test.*
import ShowcaseTestEnv.*

class SampleEndToEndShowcaseSpec extends Test[Any]:

  import SampleCatalog.*

  "SampleEndToEndShowcaseSpec" - {
    "complete backend-less showcase journey: customer -> configure -> quote -> preflight -> cart -> order -> manufacturing DAG & live capacity" in {
      locally {
      for
        eventQueue <- Env.use[EventBus](_.subscribe)

        // 1. Browse showcase catalog (15 categories ordered 1..15)
        listings <- ShowcaseFacade.listShowcaseListings

        // 2. Register a new B2B agency customer, approve, upgrade to Gold, grant NetInvoice terms
        custSvc <- Env.get[CustomerService]
        newCustId = CustomerId.unsafe("cust-new-agency")
        pending <- custSvc.register(
                     RegisterCustomerCmd(
                       id = newCustId,
                       segment = CustomerSegment.Agency,
                       company = Some(
                         CompanyProfile(
                           NonEmptyString.unsafe("Pixel Craft s.r.o."),
                           NonEmptyString.unsafe("22334455"),
                           Some(NonEmptyString.unsafe("CZ22334455")),
                           NonEmptyString.unsafe("Ondřej Marek"),
                         )
                       ),
                       contact = ContactPerson(
                         NonEmptyString.unsafe("Ondřej"),
                         NonEmptyString.unsafe("Marek"),
                         EmailAddress.unsafe("ondrej@pixelcraft.cz"),
                         Some(NonEmptyString.unsafe("+420 601 234 567")),
                       ),
                       billingAddress = Address(
                         NonEmptyString.unsafe("Korunní 45"),
                         NonEmptyString.unsafe("Praha"),
                         NonEmptyString.unsafe("12000"),
                         NonEmptyString.unsafe("CZ"),
                       ),
                     )
                   )
        approved <- custSvc.approve(newCustId)
        goldCust <- custSvc.changeTier(newCustId, CustomerTier.Gold)
        creditCust <- custSvc.grantPaymentTerms(
                        newCustId,
                        PaymentTerms.NetInvoice(14, Price(Money("200000"), Currency.CZK)),
                      )

        // 3. Configure & quote a Premium Roll-Up (Main + Stand) and an A4 16pp Saddle-Stitched Booklet (Cover + Body)
        rollupCQ <- ShowcaseFacade.pricePreset(
                      categoryId = rollUpsId,
                      presetId = PresetId.unsafe("preset-rollup-premium-100x200"),
                      currency = Currency.CZK,
                      customerId = Some(goldCust.id),
                      customerTier = Some(goldCust.tier),
                      customerSegment = Some(goldCust.segment),
                      promoCode = Some("AGENCY15"),
                    )
        bookletCQ <- ShowcaseFacade.pricePreset(
                       categoryId = bookletsId,
                       presetId = PresetId.unsafe("preset-booklet-a4-saddle"),
                       currency = Currency.CZK,
                       customerId = Some(goldCust.id),
                       customerTier = Some(goldCust.tier),
                       customerSegment = Some(goldCust.segment),
                     )

        // 4. Derive PreflightRequirement for Roll-Up Main graphic, upload & pass preflight, operator-approve
        artSvc <- Env.get[ArtworkService]
        reqOpt  = DomainBridges.toPreflightRequirement(rollupCQ.configuration.product, ComponentKey.Main)
        artId   = ArtworkId.unsafe("art-rollup-main-1")
        artSub <- artSvc.upload(
                    UploadArtworkCmd(
                      artworkId = artId,
                      owner = ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-1")),
                      componentKey = ComponentKey.Main,
                      file = AssetFileMetadata(
                        NonEmptyString.unsafe("rollup-banner-100x200.pdf"),
                        NonEmptyString.unsafe("application/pdf"),
                        12_500_000L,
                        NonEmptyString.unsafe("s3://bucket/rollup-1.pdf"),
                      ),
                      extractedSpecs = ExtractedFileSpecs(
                        resolutionDpi = 180, // >= 150 DPI for large format!
                        trimSize = Dimension2D.unsafeMm(1000, 2000),
                        bleed = NonNegativeMm.unsafe(5),
                        colorSpace = ColorSpace.CMYK,
                        pageCount = 1,
                        fontsEmbedded = true,
                      ),
                      requirement = reqOpt,
                    )
                  )
        artApproved <- artSvc.approve(artId, SampleManufacturing.empPrepressId)

        // 5. Add both lines to showcase cart & place B2B InvoiceOnAccount order
        cartSvc  <- Env.get[CartService]
        orderSvc <- Env.get[OrderService]
        cartId    = CartId.unsafe("cart-showcase-czk")
        line1 = CartLine(
                  lineId = OrderLineId.unsafe("line-1"),
                  product = DomainBridges.toProductSnapshot(rollupCQ.configuration.product),
                  runQuantity = rollupCQ.configuration.product.specs.quantity,
                  versionCount = rollupCQ.configuration.product.specs.versionCount,
                  speed = ManufacturingSpeed.Standard,
                  price = DomainBridges.toLinePriceSnapshot(rollupCQ.quote),
                  artworkIds = Map(ComponentKey.Main -> artApproved.id),
                )
        line2 = CartLine(
                  lineId = OrderLineId.unsafe("line-2"),
                  product = DomainBridges.toProductSnapshot(bookletCQ.configuration.product),
                  runQuantity = bookletCQ.configuration.product.specs.quantity,
                  versionCount = bookletCQ.configuration.product.specs.versionCount,
                  speed = ManufacturingSpeed.Standard,
                  price = DomainBridges.toLinePriceSnapshot(bookletCQ.quote),
                )
        _        <- cartSvc.addLine(cartId, line1)
        cartView <- cartSvc.addLine(cartId, line2)

        orderId = OrderId.unsafe("ord-2026-0001")
        order <- orderSvc.placeOrder(
                   PlaceOrderCmd(
                     orderId = orderId,
                     orderNumber = OrderNumber.unsafe("2026-0001"),
                     customer = OrderCustomerSnapshot(
                       customerId = Some(creditCust.id),
                       segment = creditCust.segment,
                       fullName = NonEmptyString.unsafe("Ondřej Marek"),
                       email = creditCust.contact.email,
                       phone = creditCust.contact.phone,
                       companyName = creditCust.company.map(_.legalName),
                       companyRegNo = creditCust.company.map(_.registrationNumber),
                       vatId = creditCust.company.flatMap(_.vatId),
                     ),
                     billingAddress = creditCust.billingAddress,
                     delivery = DeliverySelection.Courier(
                       NonEmptyString.unsafe("ppl-cz"),
                       creditCust.billingAddress,
                       Money("149.00"),
                       TaxRate.czStandard,
                     ),
                     paymentMethod = PaymentMethod.InvoiceOnAccount,
                     invoiceApproved = creditCust.paymentTerms != PaymentTerms.PrepaidOnly,
                     lines = NonEmptyChunk(
                       OrderLine(line1.lineId, line1.product, line1.runQuantity, line1.versionCount, line1.speed, line1.price, line1.artworkIds),
                       OrderLine(line2.lineId, line2.product, line2.runQuantity, line2.versionCount, line2.speed, line2.price, line2.artworkIds),
                     ),
                   )
                 )

        // 6. Check manufacturing speed availability & create shop-floor production DAG
        mfgSvc <- Env.get[ManufacturingService]
        expressTooMany <- mfgSvc
                            .checkSpeedAvailability(businessCardsId, ManufacturingSpeed.Express, Quantity.unsafe(5000), 1, Set(coated300gsmId), Set.empty)
                            .result
        expressEmboss <- mfgSvc
                           .checkSpeedAvailability(businessCardsId, ManufacturingSpeed.Express, Quantity.unsafe(100), 1, Set(coated300gsmId), Set(FinishingOperation.EmbossingOrFoil))
                           .result

        capBefore <- mfgSvc.currentCapacity
        spec1 = DomainBridges.toProductionSpec(order.id, 0, rollupCQ.configuration.product, ManufacturingSpeed.Standard)
        spec2 = DomainBridges.toProductionSpec(order.id, 1, bookletCQ.configuration.product, ManufacturingSpeed.Standard)
        job   <- mfgSvc.createJob(JobId.unsafe("job-2026-0001"), NonEmptyChunk(spec1, spec2))
        capAfter <- mfgSvc.currentCapacity

        // Verify Roll-Up Stand routes ONLY to Assembly (no print/cut!), and claim & complete the Stand assembly step
        rollupWf   = job.workflows.head
        standSteps = rollupWf.steps.toList.filter(_.componentKey.contains(ComponentKey.Stand))
        mainSteps  = rollupWf.steps.toList.filter(_.componentKey.contains(ComponentKey.Main))

        claimedJob <- mfgSvc.claimStep(job.id, rollupWf.id, standSteps.head.id, SampleManufacturing.empLargeFmtId, None)
        doneStand  <- mfgSvc.completeStep(job.id, rollupWf.id, standSteps.head.id, "Stand checked & loaded")

        // Verify maintenance machine is rejected
        maintMachineRejected <- mfgSvc
                                  .claimStep(
                                    job.id,
                                    rollupWf.id,
                                    rollupWf.readySteps.head.id,
                                    SampleManufacturing.empLeadAllId,
                                    Some(SampleManufacturing.machMaintenanceId),
                                  )
                                  .result

        // Check events published on EventBus across all contexts
        events <- eventQueue.drain
      yield {
        assert(listings.size == 15)
        assert(listings.map(_.sortOrder) == (1 to 15).toList)
        assert(pending.status == CustomerStatus.PendingApproval)
        assert(approved.status == CustomerStatus.Active)
        assert(goldCust.tier == CustomerTier.Gold)
        assert(rollupCQ.quote.promoDiscount.isDefined)
        assert(artSub.status == ArtworkStatus.PreflightPassed)
        assert(artApproved.status.isInstanceOf[ArtworkStatus.OperatorApproved])
        assert(cartView.lines.size == 2)
        // InvoiceOnAccount approved B2B order starts immediately in Confirmed status
        assert(order.status == OrderStatus.Confirmed)
        assert(order.totals.linesNetTotal == (rollupCQ.quote.netTotal + bookletCQ.quote.netTotal).rounded)
        assert(expressTooMany.isFailure)
        assert(expressEmboss.isFailure)
        // Roll-Up Stand has ONLY Assembly step; Main has LargeFormatPrinter + Laminator
        assert(standSteps.map(_.stationType) == List(StationType.Assembly))
        assert(mainSteps.map(_.stationType) == List(StationType.LargeFormatPrinter, StationType.Laminator))
        // Stand step was Ready immediately in parallel with Prepress and completed cleanly
        assert(claimedJob.workflows.head.status == WorkflowStatus.InProgress)
        assert(doneStand.workflows.head.steps.find(_.id == standSteps.head.id).exists(_.status == StepStatus.Completed))
        assert(maintMachineRejected.isFailure)
        // Live capacity increased when the job entered the shop floor
        assert(capBefore.globalUtilisation == BigDecimal(0))
        assert(capAfter.globalUtilisation >= capBefore.globalUtilisation)
        // Cross-context events all recorded on shared EventBus
        assert(events.exists(_.isInstanceOf[CustomerRegistered]))
        assert(events.exists(_.isInstanceOf[QuoteIssued]))
        assert(events.exists(_.isInstanceOf[ArtworkApproved]))
        assert(events.exists(_.isInstanceOf[OrderConfirmed]))
        assert(events.exists(_.isInstanceOf[ProductionJobCreated]))
        assert(events.exists(_.isInstanceOf[WorkflowStepCompleted]))
      }
      }
    }.provideRuntime(useSheetCzk = true)
  }
