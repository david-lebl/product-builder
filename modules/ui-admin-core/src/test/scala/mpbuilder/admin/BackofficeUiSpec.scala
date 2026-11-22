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
import zio.ZIO
import zio.test.*

object BackofficeUiSpec extends ZIOSpecDefault:

  private def withSharedClients[A](
      f: (StorefrontClient, BackofficeClient) => A < (Abort[Any] & Async),
  ): ZIO[Any, Any, A] =
    ZIO.scoped {
      SampleShowcaseRuntime.layer().build.flatMap { env =>
        val sfClient = StorefrontClient.fromZio(env.get[StorefrontFacade], env.get[InMemoryOtpInbox].codeFor)
        val boClient = BackofficeClient.fromZio(env.get[BackofficeFacade])
        ZIOs.run(f(sfClient, boClient))
      }
    }

  private def renderHtml(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle) {
      UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse(""))
    }

  override def spec: Spec[TestEnvironment, Any] =
    suite("Kyo UI Backoffice & Unified Demo Spec")(
      test("AdminTheme stylesheet emits responsive breakpoints and backoffice layout rules") {
        val css = AdminTheme.adminStylesheet.render
        assertTrue(
          css.contains("@media (max-width: 960px)"),
          css.contains("@media (max-width: 640px)"),
          css.contains(".mp-admin-shell"),
          css.contains(".mp-admin-sidebar"),
          css.contains(".mp-kpi-grid"),
          css.contains(".mp-timeline"),
        )
      },
      test("AdminRoute hash routing round-trips both unified (#/admin/...) and standalone (#/...) prefixes") {
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
        assertTrue(
          routes.forall(r => AdminRoute.fromHash(AdminRoute.toHash(r, "#/admin"), requireAdminPrefix = true).contains(r)),
          routes.forall(r => AdminRoute.fromHash(AdminRoute.toHash(r, "#"), requireAdminPrefix = false).contains(r)),
          AdminRoute.fromHash("#/catalog", requireAdminPrefix = true).isEmpty,
          AdminRoute.fromHash("#/admin", requireAdminPrefix = true).contains(AdminRoute.Dashboard),
        )
      },
      test("AdminTranslations renders EN & CS cleanly with zero missing-key markers") {
        ZIOs.run {
          for
            i18n    <- AdminTranslations.init(Language.En)
            enDash  <- I18n.let(i18n.handle)(i18n.now("adm-dash-title"))
            enBrand <- I18n.let(i18n.handle)(i18n.now("brand-name"))
            _       <- i18n.setLanguage(Language.Cs)
            csDash  <- I18n.let(i18n.handle)(i18n.now("adm-dash-title"))
            csOrd   <- I18n.let(i18n.handle)(i18n.now("adm-nav-orders"))
          yield assertTrue(
            enBrand == "PrintCraft Studio",
            enDash == "Operations Dashboard",
            csDash == "Provozní přehled",
            csOrd == "Příjem objednávek",
            !enDash.contains("‹") && !csDash.contains("‹"),
          )
        }
      },
      test("Staff login gate, RBAC read-only Prepress view, and full Admin EN/CS views render cleanly") {
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
          yield assertTrue(
            loginHtml.contains("Staff Backoffice Sign In"),
            loginHtml.contains("admin@printshop.cz"),
            !loginHtml.contains("‹") && !loginHtml.contains("›"),
            prepress.isRight,
            prepHtml.contains("Read-only Intake (Prepress)"),
            !prepHtml.contains("‹") && !prepHtml.contains("›"),
            admin.isRight,
            dashEnHtml.contains("Operations Dashboard"),
            dashEnHtml.contains("ORD-2026-0901"),
            !dashEnHtml.contains("‹") && !dashEnHtml.contains("›"),
            dashCsHtml.contains("Provozní přehled"),
            dashCsHtml.contains("Příjem objednávek"),
            !dashCsHtml.contains("‹") && !dashCsHtml.contains("›"),
          )
        }
      },
      test("Unified E2E: Storefront checkout lands live in Backoffice Order Intake, flows to Completed, and reflects back in Storefront") {
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
          yield assertTrue(
            queue.exists(_.id == newOrdId),
            finalOrd.exists(_.status == OrderStatus.Completed),
            finalOrd.exists(_.internalNotes.exists(_.text.value.contains("Customer confirmed delivery"))),
            finalOrd.exists(_.statusHistory.size >= 7),
            detHtml.contains(conf.order.orderNumber.value),
            detHtml.contains("Customer confirmed delivery"),
            detHtml.contains("Status Audit Trail"),
            !detHtml.contains("‹") && !detHtml.contains("›"),
            sfOrders.find(_.id == newOrdId).exists(_.status == OrderStatus.Completed),
          )
        }
      },
      test("Catalog, Pricing + Margin Simulator, Customers, and Staff/Settings screens render cleanly and mutate live") {
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
          yield assertTrue(
            catHtml.contains("Catalog &amp; Capabilities Inspector"),
            catHtml.contains("mp-tabs"),
            catHtml.contains("mp-dt-table"),
            !catHtml.contains("‹") && !catHtml.contains("›"),
            priHtml.contains("Margin Simulator"),
            priHtml.contains("mp-tabs"),
            !priHtml.contains("‹") && !priHtml.contains("›"),
            simRes.exists(_.quote.grossTotal.value > 0),
            simRes.exists(_.margin.productionCostNet.value > 0),
            custHtml.contains("Customer Accounts &amp; B2B Approvals"),
            custHtml.contains("mp-dt-table"),
            !custHtml.contains("‹") && !custHtml.contains("›"),
            setHtml.contains("Staff Directory &amp; Store Settings"),
            setHtml.contains("mp-tabs"),
            setHtml.contains("admin@printshop.cz"),
            !setHtml.contains("‹") && !setHtml.contains("›"),
          )
        }
      },
      test("Phase 2B refinements: Order Intake DataTable split filters, priority/deadline, payment toggle, line edit, artwork review, customer contacts/audit & pricing deep-link") {
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
          yield assertTrue(
            ordersHtml.contains("mp-dt-root"),
            ordersHtml.contains("mp-dt-table"),
            ordersHtml.contains("mp-dt-header-top"),
            ordersHtml.contains("mp-dt-title-block"),
            ordersHtml.contains("mp-dt-header-filters"),
            ordersHtml.contains("mp-dt-ms-wrap"),
            ordersHtml.contains("mp-dt-check"),
            !ordersHtml.contains(">Open<"),
            ordersHtml.contains("Intake Status"),
            ordersHtml.contains("Payment Status"),
            ordersHtml.contains("Priority"),
            !ordersHtml.contains("‹") && !ordersHtml.contains("›"),
            ord2Opt.exists(_.isPaid),
            ord2Opt.exists(_.priority == OrderPriority.Urgent),
            ord2Opt.exists(_.lines.head.specNote.contains("Pantone 286 C spot check")),
            detEnHtml.contains("mp-detail-head-row"),
            detEnHtml.contains("mp-detail-tags-row"),
            detEnHtml.contains("mp-action-bar"),
            detEnHtml.contains("mp-action-menu-wrap"),
            detEnHtml.contains("Next stage:"),
            detEnHtml.contains("Pantone 286 C spot check"),
            detEnHtml.contains("Artwork &amp; Preflight Review"),
            !detEnHtml.contains("‹") && !detEnHtml.contains("›"),
            embOrdHtml.contains("mp-tabs-sm"),
            embOrdHtml.contains("mp-detail-head-row"),
            embOrdHtml.contains("mp-action-menu-wrap"),
            updatedB2b.exists(_.additionalContacts.exists(_.email.value == "klara@printshoppro.cz")),
            updatedB2b.exists(_.auditLog.exists(e => e.action == "ProfileUpdated" && e.actor.isInstanceOf[CustomerActor.Employee])),
            focusTab == "agreements",
            focusCid.contains(b2bCust.id),
            agrHtml.contains("mp-dt-table"),
            promoHtml.contains("Lifecycle"),
            plHtml.contains("mp-dt-table"),
            !plHtml.contains("‹") && !plHtml.contains("›"),
            !plCsHtml.contains("‹") && !plCsHtml.contains("›"),
          )
        }
      },
    )
