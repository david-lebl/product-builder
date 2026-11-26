package mpbuilder.admin

import kyo.*
import mpbuilder.admin.i18n.AdminTranslations
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.theme.AdminTheme
import mpbuilder.backoffice.{BackofficeFacade, MarginSimulationCmd}
import mpbuilder.customers.*
import mpbuilder.identity.sample.InMemoryOtpInbox
import mpbuilder.ordering.*
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.port.StorefrontClient
import mpbuilder.ui.storefront.{AppRoute, StorefrontState}
import kyo.test.*

class BackofficeUiSpec extends Test[Any]:

  private def withSharedClients[A](
      f: (StorefrontClient, BackofficeClient) => A < (Abort[Any] & Async),
  ): A < (Abort[Any] & Async) =
    SampleShowcaseRuntime.make().map { rt =>
      f(StorefrontClient.fromFacade(rt.storefront, rt.otpInbox.codeFor), BackofficeClient.fromFacade(rt.backoffice))
    }

  private def renderHtml(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle) {
      UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse(""))
    }

  "Kyo UI Backoffice & Unified Demo Spec" - {
      "AdminTheme stylesheet emits responsive breakpoints and backoffice layout rules" in {
        val css = AdminTheme.adminStylesheet.render
        assert(css.contains("@media (max-width: 960px)"))
        assert(css.contains("@media (max-width: 640px)"))
        assert(css.contains(".mp-admin-shell"))
        assert(css.contains(".mp-admin-sidebar"))
        assert(css.contains(".mp-kpi-grid"))
        assert(css.contains(".mp-timeline"))
      }

      "AdminTheme document CSS pins DataTable views to the viewport with an internally scrolling table" in {
        val css = AdminTheme.documentCss
        assert(css.contains(".mp-admin-workspace:has(.mp-dt-root){height:100vh"))
        assert(css.contains(".mp-dt-table-scroll{flex:1 1 0;min-height:0;overflow:auto}"))
        assert(css.contains(".mp-dt-th{position:sticky;top:0"))
        assert(css.contains("max-width:2400px"))
      }

      "AdminRoute hash routing round-trips both unified (#/admin/...) and standalone (#/...) prefixes" in {
        val ordId = OrderId.unsafe("ord-seed-0901")
        val routes = List(
          AdminRoute.Dashboard,
          AdminRoute.Orders,
          AdminRoute.OrderDetail(ordId),
          AdminRoute.Catalog,
          AdminRoute.Pricing,
          AdminRoute.Customers,
          AdminRoute.StaffAndSettings,
        )
        assert(routes.forall(r => AdminRoute.fromHash(AdminRoute.toHash(r, "#/admin"), requireAdminPrefix = true).contains(r)))
        assert(routes.forall(r => AdminRoute.fromHash(AdminRoute.toHash(r, "#"), requireAdminPrefix = false).contains(r)))
        assert(AdminRoute.fromHash("#/catalog", requireAdminPrefix = true).isEmpty)
        assert(AdminRoute.fromHash("#/admin", requireAdminPrefix = true).contains(AdminRoute.Dashboard))
      }

      "AdminTranslations renders EN & CS cleanly with zero missing-key markers" in {
        locally {
          for
            i18n    <- AdminTranslations.init(Language.En)
            enDash  <- I18n.let(i18n.handle)(i18n.now("adm-dash-title"))
            enBrand <- I18n.let(i18n.handle)(i18n.now("brand-name"))
            _       <- i18n.setLanguage(Language.Cs)
            csDash  <- I18n.let(i18n.handle)(i18n.now("adm-dash-title"))
            csOrd   <- I18n.let(i18n.handle)(i18n.now("adm-nav-orders"))
          yield {
            assert(enBrand == "PrintCraft Studio")
            assert(enDash == "Operations Dashboard")
            assert(csDash == "Provozní přehled")
            assert(csOrd == "Příjem objednávek")
            assert(!enDash.contains("‹") && !csDash.contains("‹"))
          }
        }
      }

      "Staff login gate, RBAC read-only Prepress view, and full Admin EN/CS views render cleanly" in {
        withSharedClients { (_, boClient) =>
          for
            i18n       <- AdminTranslations.init(Language.En)
            state      <- BackofficeState.init(boClient, i18n)
            ui         <- I18n.let(i18n.handle)(BackofficeApp.render(state))
            loginHtml  <- renderHtml(ui, i18n)
            // Log in as Prepress (read-only intake/management)
            prepress   <- I18n.let(i18n.handle)(state.login("prepress@printshop.cz", "staff1234"))
            _          <- I18n.let(i18n.handle)(state.navigate(AdminRoute.OrderDetail(OrderId.unsafe("ord-seed-0902"))))
            prepHtml   <- renderHtml(ui, i18n)
            _          <- I18n.let(i18n.handle)(state.logout)
            // Log in as Admin
            admin      <- I18n.let(i18n.handle)(state.login("admin@printshop.cz", "staff1234"))
            _          <- I18n.let(i18n.handle)(state.navigate(AdminRoute.Dashboard))
            dashEnHtml <- renderHtml(ui, i18n)
            _          <- I18n.let(i18n.handle)(state.setLanguage(Language.Cs))
            dashCsHtml <- renderHtml(ui, i18n)
          yield {
            assert(loginHtml.contains("Staff Backoffice Sign In"))
            assert(loginHtml.contains("admin@printshop.cz"))
            assert(!loginHtml.contains("‹") && !loginHtml.contains("›"))
            assert(prepress.isRight)
            assert(prepHtml.contains("Read-only Intake (Prepress)"))
            assert(!prepHtml.contains("‹") && !prepHtml.contains("›"))
            assert(admin.isRight)
            assert(dashEnHtml.contains("Operations Dashboard"))
            assert(dashEnHtml.contains("ORD-2026-0901"))
            assert(!dashEnHtml.contains("‹") && !dashEnHtml.contains("›"))
            assert(dashCsHtml.contains("Provozní přehled"))
            assert(dashCsHtml.contains("Příjem objednávek"))
            assert(!dashCsHtml.contains("‹") && !dashCsHtml.contains("›"))
          }
        }
      }

      "Unified E2E: Storefront checkout lands live in Backoffice Order Intake, flows to Completed, and reflects back in Storefront" in {
        withSharedClients { (sfClient, boClient) =>
          for
            i18n    <- AdminTranslations.init(Language.En)
            // 1. Customer logs in & places order in Storefront
            sfState <- I18n.let(i18n.handle)(StorefrontState.init(sfClient, i18n, initialRoute = AppRoute.Builder()))
            cAuth   <- sfClient.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _       <- I18n.let(i18n.handle)(sfState.completeAuth(cAuth))
            draft   <- sfState.configurator.draft.get
            _       <- I18n.let(i18n.handle)(sfState.addDraftToCart(draft))
            _       <- I18n.let(i18n.handle)(sfState.startCartCheckout)
            sessOpt <- sfState.checkoutSession.get
            sess    <- Abort.get(sessOpt.toRight("Expected checkout session"))
            _       <- sess.selectedPay.set(PaymentMethod.InvoiceOnAccount)
            _       <- I18n.let(i18n.handle)(sess.submitOrder(sfState))
            conf    <- sess.confirmationRef.get.flatMap(opt => Abort.get(opt.toRight("Expected confirmation")))
            newOrdId = conf.order.id

            // 2. Staff opens Backoffice on the SAME runtime, sees new order, accepts -> holds -> resumes -> completes
            boState <- BackofficeState.init(boClient, i18n)
            _       <- I18n.let(i18n.handle)(boState.login("manager@printshop.cz", "staff1234"))
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Orders))
            queue   <- boState.orders.get
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.OrderDetail(newOrdId)))
            _       <- I18n.let(i18n.handle)(boState.acceptOrder(newOrdId, "Specs verified"))
            _       <- I18n.let(i18n.handle)(boState.putOrderOnHold(newOrdId, "Waiting for Pantone match"))
            _       <- I18n.let(i18n.handle)(boState.resumeOrderFromHold(newOrdId, "Pantone confirmed"))
            _       <- I18n.let(i18n.handle)(boState.advanceOrderStage(newOrdId, OrderStatus.InProduction, "Press #1"))
            _       <- I18n.let(i18n.handle)(boState.advanceOrderStage(newOrdId, OrderStatus.ReadyForDispatch))
            _       <- I18n.let(i18n.handle)(boState.advanceOrderStage(newOrdId, OrderStatus.Dispatched))
            _       <- I18n.let(i18n.handle)(boState.advanceOrderStage(newOrdId, OrderStatus.Completed))
            _       <- I18n.let(i18n.handle)(boState.addOrderNote(newOrdId, "Customer confirmed delivery"))
            finalOrd <- boState.selectedOrder.get

            // 3. Render OrderDetailView and also verify Storefront customer sees Completed status
            boUi      <- I18n.let(i18n.handle)(BackofficeApp.render(boState))
            detHtml   <- renderHtml(boUi, i18n)
            _         <- I18n.let(i18n.handle)(sfState.refreshOrders)
            sfOrders  <- sfState.orders.get
          yield {
            assert(queue.exists(_.id == newOrdId))
            assert(finalOrd.exists(_.status == OrderStatus.Completed))
            assert(finalOrd.exists(_.internalNotes.exists(_.text.value.contains("Customer confirmed delivery"))))
            assert(finalOrd.exists(_.statusHistory.size >= 7))
            assert(detHtml.contains(conf.order.orderNumber.value))
            assert(detHtml.contains("Customer confirmed delivery"))
            assert(detHtml.contains("Status Audit Trail"))
            assert(!detHtml.contains("‹") && !detHtml.contains("›"))
            assert(sfOrders.find(_.id == newOrdId).exists(_.status == OrderStatus.Completed))
          }
        }
      }

      "Catalog, Pricing + Margin Simulator, Customers, and Staff/Settings screens render cleanly and mutate live" in {
        withSharedClients { (_, boClient) =>
          for
            i18n    <- AdminTranslations.init(Language.En)
            boState <- BackofficeState.init(boClient, i18n)
            _       <- I18n.let(i18n.handle)(boState.login("admin@printshop.cz", "staff1234"))
            boUi    <- I18n.let(i18n.handle)(BackofficeApp.render(boState))

            // Catalog
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Catalog))
            catHtml <- renderHtml(boUi, i18n)

            // Pricing & live Margin Simulator
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Pricing))
            priHtml <- renderHtml(boUi, i18n)
            catOpt  <- boState.catalog.get
            cat     <- Abort.get(catOpt.toRight("Expected catalog"))
            firstCat = cat.categories.get(CategoryId.unsafe("cat-business-cards")).getOrElse(cat.categories.values.find(_.presets.nonEmpty).get)
            simRes  <- I18n.let(i18n.handle)(
                         boState.simulateMargin(
                           MarginSimulationCmd(
                             categoryId = firstCat.id,
                             presetId = firstCat.presets.head.id,
                             quantity = Quantity.unsafe(500),
                             currency = Currency.CZK,
                           )
                         )
                       )

            // Customers
            _        <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Customers))
            custs    <- boState.customers.get
            _        <- I18n.let(i18n.handle)(boState.selectCustomer(custs.head.id))
            custHtml <- renderHtml(boUi, i18n)

            // Staff & Store Settings
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.StaffAndSettings))
            setHtml <- renderHtml(boUi, i18n)
          yield {
            assert(catHtml.contains("Catalog &amp; Capabilities Inspector"))
            assert(catHtml.contains("mp-tabs"))
            assert(catHtml.contains("mp-dt-table"))
            assert(!catHtml.contains("‹") && !catHtml.contains("›"))
            assert(priHtml.contains("Margin Simulator"))
            assert(priHtml.contains("mp-tabs"))
            assert(!priHtml.contains("‹") && !priHtml.contains("›"))
            assert(simRes.exists(_.quote.grossTotal.value > 0))
            assert(simRes.exists(_.margin.productionCostNet.value > 0))
            assert(custHtml.contains("Customer Accounts &amp; B2B Approvals"))
            assert(custHtml.contains("mp-dt-table"))
            assert(!custHtml.contains("‹") && !custHtml.contains("›"))
            assert(setHtml.contains("Staff Directory &amp; Store Settings"))
            assert(setHtml.contains("mp-tabs"))
            assert(setHtml.contains("admin@printshop.cz"))
            assert(!setHtml.contains("‹") && !setHtml.contains("›"))
          }
        }
      }

      "Margin Simulator re-renders with the chosen selections (option selected) and a digit-filtered quantity" in {
        withSharedClients { (_, boClient) =>
          for
            i18n    <- AdminTranslations.init(Language.En)
            boState <- BackofficeState.init(boClient, i18n)
            _       <- I18n.let(i18n.handle)(boState.login("admin@printshop.cz", "staff1234"))
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Pricing))
            catOpt  <- boState.catalog.get
            cat     <- Abort.get(catOpt.toRight("Expected catalog"))
            withPre  = cat.categories.values.toList.filter(_.presets.nonEmpty).sortBy(_.id.value)
            // Pick a non-default category & its last preset, so a fallback to the first option would be visible.
            selCat   = withPre.last
            selPre   = selCat.presets.last
            catRef  <- Signal.initRef(selCat.id.value)
            preRef  <- Signal.initRef(selPre.id.value)
            qtyRef  <- Signal.initRef("1200")
            curRef  <- Signal.initRef[Currency](Currency.USD)
            spdRef  <- Signal.initRef[ManufacturingSpeed](ManufacturingSpeed.Express)
            tierRef <- Signal.initRef("Gold")
            promoRef <- Signal.initRef("")
            resRef  <- Signal.initRef[Option[mpbuilder.backoffice.MarginSimulationResult]](None)
            ui = mpbuilder.admin.views.PricingManagerView.renderSimulator(
                   boState, catOpt, catRef, preRef, qtyRef, curRef, spdRef, tierRef, promoRef, resRef, Language.En,
                 )
            html <- renderHtml(ui, i18n)
          yield {
            assert(withPre.size > 1)
            assert(html.contains(s"""value="${selCat.id.value}" selected"""))
            assert(html.contains(s"""value="${selPre.id.value}" selected"""))
            assert(html.contains("""value="USD" selected"""))
            assert(!html.contains("""value="CZK" selected"""))
            assert(html.contains("""value="Express" selected"""))
            assert(html.contains("""value="Gold" selected"""))
            assert(!html.contains("""value="None" selected"""))
            assert(html.contains("data-kyo-filter=\"digits\""))
            assert(!html.contains("type=\"number\""))
          }
        }
      }

      "Phase 2B refinements: Order Intake DataTable split filters, priority/deadline, payment toggle, line edit, artwork review, customer contacts/audit & pricing deep-link" in {
        withSharedClients { (_, boClient) =>
          for
            i18n    <- AdminTranslations.init(Language.En)
            boState <- BackofficeState.init(boClient, i18n)
            _       <- I18n.let(i18n.handle)(boState.login("admin@printshop.cz", "staff1234"))
            boUi    <- I18n.let(i18n.handle)(BackofficeApp.render(boState))

            // 1. Order Intake queue renders DataTable + split filter sections + Priority & Deadline
            _          <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Orders))
            ordersHtml <- renderHtml(boUi, i18n)

            // 2. Order Detail: decoupled payment toggle, line edit + plain-text spec note, artwork review
            ordId      = OrderId.unsafe("ord-seed-0901")
            _         <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.OrderDetail(ordId)))
            ord1Opt   <- boState.selectedOrder.get
            ord1      <- Abort.get(ord1Opt.toRight("Expected seeded order ord-seed-0901"))
            _         <- I18n.let(i18n.handle)(boState.markOrderPaymentStatus(ordId, PaymentStatus.Confirmed, "Wire settled"))
            _         <- I18n.let(i18n.handle)(
                           boState.editOrderLine(
                             EditOrderLineCmd(
                               orderId = ordId,
                               lineId = ord1.lines.head.lineId,
                               speed = Some(ManufacturingSpeed.Express),
                               specNote = Some(Some("Pantone 286 C spot check")),
                             )
                           )
                         )
            ord2Opt   <- boState.selectedOrder.get
            arts      <- boState.selectedOrderArtwork.get
            _         <- arts.headOption match
                           case Some(a) => I18n.let(i18n.handle)(boState.requestArtworkChanges(ordId, a.id, "Increase to 300 DPI"))
                           case None    => Kyo.unit
            detEnHtml <- renderHtml(boUi, i18n)

            // 3. Customers: multi-contact profile edit + Employee audit entry + Pricing deep-link
            _         <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Customers))
            custs     <- boState.customers.get
            b2bCust   <- Abort.get(
                           custs.find(c => c.segment == CustomerSegment.Business || c.segment == CustomerSegment.Agency)
                             .toRight("Expected B2B customer")
                         )
            extraCp    = ContactPerson(
                           firstName = NonEmptyString.unsafe("Klára"),
                           lastName = NonEmptyString.unsafe("Benešová"),
                           email = EmailAddress.unsafe("klara@printshoppro.cz"),
                           roleTitle = Some(NonEmptyString.unsafe("Production Coordinator")),
                         )
            _         <- I18n.let(i18n.handle)(
                           boState.updateCustomerProfile(
                             UpdateCustomerProfileCmd(
                               id = b2bCust.id,
                               company = b2bCust.company,
                               contact = b2bCust.contact,
                               billingAddress = b2bCust.billingAddress,
                               tags = b2bCust.tags,
                               additionalContacts = Some(b2bCust.additionalContacts :+ extraCp),
                             )
                           )
                         )
            custs2    <- boState.customers.get
            updatedB2b = custs2.find(_.id == b2bCust.id)
            _         <- I18n.let(i18n.handle)(boState.openCustomerPricing(b2bCust.id))
            focusTab  <- boState.pricingFocusTab.get
            focusCid  <- boState.pricingFocusCustomerId.get
            agrHtml   <- renderHtml(boUi, i18n)

            // 4. Switch to Promo Codes & Pricelists tabs + CS localization check
            _         <- boState.pricingFocusTab.set("promos")
            promoHtml <- renderHtml(boUi, i18n)
            _         <- boState.pricingFocusTab.set("pricelists")
            plHtml    <- renderHtml(boUi, i18n)
            _         <- I18n.let(i18n.handle)(boState.setLanguage(Language.Cs))
            plCsHtml  <- renderHtml(boUi, i18n)
            // 5. Embedded OrderDetailView renders compact inner sub-tabs (mp-tabs-sm)
            _             <- I18n.let(i18n.handle)(boState.setLanguage(Language.En))
            _             <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.OrderDetail(ordId)))
            embOrdUi      <- I18n.let(i18n.handle)(mpbuilder.admin.views.OrderDetailView.render(boState, embedded = true))
            embOrdHtml    <- renderHtml(embOrdUi, i18n)
          yield {
            assert(ordersHtml.contains("mp-dt-root"))
            assert(ordersHtml.contains("mp-dt-table"))
            assert(ordersHtml.contains("Order # ↓"))
            assert(
              ordersHtml.indexOf("ORD-2026-0904") < ordersHtml.indexOf("ORD-2026-0903") &&
                ordersHtml.indexOf("ORD-2026-0903") < ordersHtml.indexOf("ORD-2026-0902") &&
                ordersHtml.indexOf("ORD-2026-0902") < ordersHtml.indexOf("ORD-2026-0901"),
            )
            assert(ordersHtml.contains("mp-dt-header-top"))
            assert(ordersHtml.contains("mp-dt-title-block"))
            assert(ordersHtml.contains("mp-dt-header-filters"))
            assert(ordersHtml.contains("mp-dt-ms-wrap"))
            assert(ordersHtml.contains("mp-dt-check"))
            assert(!ordersHtml.contains(">Open<"))
            assert(ordersHtml.contains("Intake Status"))
            assert(ordersHtml.contains("Payment Status"))
            assert(ordersHtml.contains("Priority"))
            assert(!ordersHtml.contains("‹") && !ordersHtml.contains("›"))
            assert(ord2Opt.exists(_.isPaid))
            assert(ord2Opt.exists(_.priority == OrderPriority.Urgent))
            assert(ord2Opt.exists(_.lines.head.specNote.contains("Pantone 286 C spot check")))
            assert(detEnHtml.contains("mp-detail-head-row"))
            assert(detEnHtml.contains("mp-detail-tags-row"))
            assert(detEnHtml.contains("mp-action-bar"))
            assert(detEnHtml.contains("mp-action-menu-wrap"))
            assert(detEnHtml.contains("Next stage:"))
            assert(detEnHtml.contains("Pantone 286 C spot check"))
            assert(detEnHtml.contains("Artwork &amp; Preflight Review"))
            assert(!detEnHtml.contains("‹") && !detEnHtml.contains("›"))
            assert(embOrdHtml.contains("mp-tabs-sm"))
            assert(embOrdHtml.contains("mp-detail-head-row"))
            assert(embOrdHtml.contains("mp-action-menu-wrap"))
            assert(updatedB2b.exists(_.additionalContacts.exists(_.email.value == "klara@printshoppro.cz")))
            assert(updatedB2b.exists(_.auditLog.exists(e => e.action == "ProfileUpdated" && e.actor.isInstanceOf[CustomerActor.Employee])))
            assert(focusTab == "agreements")
            assert(focusCid.contains(b2bCust.id))
            assert(agrHtml.contains("mp-dt-table"))
            assert(promoHtml.contains("Lifecycle"))
            assert(plHtml.contains("mp-dt-table"))
            assert(!plHtml.contains("‹") && !plHtml.contains("›"))
            assert(!plCsHtml.contains("‹") && !plCsHtml.contains("›"))
          }
        }
      }

      "Artwork management in Backoffice: OrderDetail preview/download/copy-link/revisions/staff-upload and Customer Detail Artworks tab (EN & CS)" in {
        import mpbuilder.customers.sample.SampleCustomers
        import mpbuilder.ui.components.ArtworkComponents

        withSharedClients { (_, boClient) =>
          for
            i18n   <- AdminTranslations.init(Language.En)
            bo     <- BackofficeState.init(boClient, i18n, AdminRoute.Dashboard)
            boUi   <- I18n.let(i18n.handle)(BackofficeApp.render(bo, onBackToStorefront = None))
            _      <- I18n.let(i18n.handle)(bo.login("admin@printshop.cz", "staff1234"))

            // 1. Open seeded order ord-seed-0902 in OrderDetail -> verify thumbnail, preview, download, copy-link, rerun preflight, staff upload dropzone
            ord2Id  = OrderId.unsafe("ord-seed-0902")
            _      <- I18n.let(i18n.handle)(bo.navigate(AdminRoute.OrderDetail(ord2Id)))
            odEn0  <- renderHtml(boUi, i18n)
            arts0  <- bo.selectedOrderArtwork.get
            art0    = arts0.head

            // 2. Staff uploads a prepress-fixed revision and opens the artwork preview lightbox modal
            fixFile = ArtworkComponents.demoPrintReadyFile(art0.componentKey, art0.latestRevision.extractedSpecs.trimSize)
            _      <- I18n.let(i18n.handle)(bo.uploadStaffArtworkRevision(ord2Id, art0.id, art0.latestRevision.extractedSpecs.trimSize, fixFile))
            arts1  <- bo.selectedOrderArtwork.get
            updated = arts1.find(_.id == art0.id).get
            _      <- bo.artworkPreview.open(updated, contextTitle = Some("Order #0902 · Main"))
            prevHtml <- renderHtml(boUi, i18n)
            _      <- bo.artworkPreview.close

            // 3. Re-run preflight cleanly via state
            _      <- I18n.let(i18n.handle)(bo.rerunArtworkPreflight(ord2Id, art0.id))
            arts2  <- bo.selectedOrderArtwork.get
            rerun   = arts2.find(_.id == art0.id).get

            // 4. Open Customers -> select Print Shop Pro -> verify CustomerDetail loads artworks with order link & createdAt
            _      <- I18n.let(i18n.handle)(bo.navigate(AdminRoute.Customers))
            _      <- I18n.let(i18n.handle)(bo.selectCustomer(SampleCustomers.printShopProId))
            selDet <- bo.selectedCustomer.get
            custEn <- renderHtml(boUi, i18n)
            _      <- I18n.let(i18n.handle)(bo.setLanguage(Language.Cs))
            custCs <- renderHtml(boUi, i18n)
            _      <- I18n.let(i18n.handle)(bo.navigate(AdminRoute.OrderDetail(ord2Id)))
            odCs   <- renderHtml(boUi, i18n)
          yield {
            assert(odEn0.contains("mp-art-thumb"))
            assert(odEn0.contains("data-mp-download-url"))
            assert(odEn0.contains("data-mp-copy-url"))
            assert(odEn0.contains("Upload Prepress Fix"))
            assert(odEn0.contains("Revision History ("))
            assert(odEn0.contains("Re-run Preflight"))
            assert(odEn0.contains("Preview"))
            assert(updated.revisionCount == art0.revisionCount + 1)
            assert(rerun.revisionCount == updated.revisionCount)
            assert(rerun.latestRevision.preflight.isDefined)
            assert(prevHtml.contains("mp-art-preview-modal"))
            assert(prevHtml.contains("Order #0902 · Main"))
            assert(selDet.exists(_.artworks.nonEmpty))
            assert(selDet.exists(_.artworks.exists(_.orderId == ord2Id)))
            assert(!odEn0.contains("‹") && !odEn0.contains("›"))
            assert(!odCs.contains("‹") && !odCs.contains("›"))
            assert(!custEn.contains("‹") && !custEn.contains("›"))
            assert(!custCs.contains("‹") && !custCs.contains("›"))
          }
        }
      }

      "Catalog availability toggles across categories, showcases, materials, finishes, print methods, and rules reflect live on Storefront (EN & CS)" in {
        withSharedClients { (sfClient, boClient) =>
          for
            i18n    <- AdminTranslations.init(Language.En)
            sfState <- I18n.let(i18n.handle)(StorefrontState.init(sfClient, i18n))
            boState <- BackofficeState.init(boClient, i18n, AdminRoute.Catalog, onDataMutated = () => sfState.refreshCatalog)
            boUi    <- I18n.let(i18n.handle)(BackofficeApp.render(boState, onBackToStorefront = None))
            _       <- I18n.let(i18n.handle)(boState.login("manager@printshop.cz", "staff1234"))
            _       <- I18n.let(i18n.handle)(boState.navigate(AdminRoute.Catalog))
            cat0    <- boState.catalog.get.flatMap(opt => Abort.get(opt.toRight("Expected catalog")))
            rs0     <- boState.ruleset.get.flatMap(opt => Abort.get(opt.toRight("Expected ruleset")))
            list0   <- sfState.listingsRef.get

            // 1. Disable a showcase listing -> disappears from storefront listings while category stays enabled
            scTarget = cat0.showcases.values.head
            _       <- I18n.let(i18n.handle)(boState.upsertShowcaseListing(scTarget.copy(enabled = false)))
            list1   <- sfState.listingsRef.get

            // 2. Disable another product category -> its showcase also disappears from storefront listings
            catTarget = cat0.categories.values.find(_.id != scTarget.categoryId).get
            _       <- I18n.let(i18n.handle)(boState.upsertCategory(catTarget.copy(enabled = false)))
            list2   <- sfState.listingsRef.get

            // 3. Disable a material, finish, printing method, and rule
            matTarget = cat0.materials.values.head
            finTarget = cat0.finishes.values.head
            pmTarget  = cat0.printingMethods.values.head
            ruleTarget = rs0.rules.head
            _       <- I18n.let(i18n.handle)(boState.upsertMaterial(matTarget.copy(enabled = false)))
            _       <- I18n.let(i18n.handle)(boState.upsertFinish(finTarget.copy(enabled = false)))
            _       <- I18n.let(i18n.handle)(boState.upsertPrintingMethod(pmTarget.copy(enabled = false)))
            _       <- I18n.let(i18n.handle)(boState.upsertRule(ruleTarget.copy(enabled = false)))

            cat1    <- boState.catalog.get.flatMap(opt => Abort.get(opt.toRight("Expected updated catalog")))
            rs1     <- boState.ruleset.get.flatMap(opt => Abort.get(opt.toRight("Expected updated ruleset")))
            enHtml  <- renderHtml(boUi, i18n)
            _       <- I18n.let(i18n.handle)(boState.setLanguage(Language.Cs))
            csHtml  <- renderHtml(boUi, i18n)

            // 4. Re-enable showcase & category -> restored on storefront
            _       <- I18n.let(i18n.handle)(boState.upsertShowcaseListing(scTarget.copy(enabled = true)))
            _       <- I18n.let(i18n.handle)(boState.upsertCategory(catTarget.copy(enabled = true)))
            list3   <- sfState.listingsRef.get
          yield {
            assert(list0.size == 15)
            assert(list1.size == 14)
            assert(!list1.exists(_.categoryId == scTarget.categoryId))
            assert(list2.size == 13)
            assert(!list2.exists(_.categoryId == catTarget.id))
            assert(!cat1.categories(catTarget.id).enabled)
            assert(!cat1.materials(matTarget.id).enabled)
            assert(!cat1.finishes(finTarget.id).enabled)
            assert(!cat1.printingMethods(pmTarget.id).enabled)
            assert(!cat1.showcases(scTarget.categoryId).enabled)
            assert(!rs1.rules.find(_.id == ruleTarget.id).get.enabled)
            assert(list3.size == 15)
            assert(enHtml.contains("Enabled"))
            assert(enHtml.contains("Disabled"))
            assert(enHtml.contains("Availability"))
            assert(!enHtml.contains("‹") && !enHtml.contains("›"))
            assert(csHtml.contains("Aktivní"))
            assert(csHtml.contains("Neaktivní"))
            assert(csHtml.contains("Dostupnost"))
            assert(!csHtml.contains("‹") && !csHtml.contains("›"))
          }
        }
      }
  }
