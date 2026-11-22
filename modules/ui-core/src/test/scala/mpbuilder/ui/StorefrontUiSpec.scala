package mpbuilder.ui

import kyo.*
import mpbuilder.catalog.*
import mpbuilder.identity.*
import mpbuilder.identity.sample.InMemoryOtpInbox
import mpbuilder.ordering.*
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.calculator.{CalculatorConfig, CalculatorView}
import mpbuilder.ui.configurator.*
import mpbuilder.ui.i18n.{AppI18n, Translations}
import mpbuilder.ui.port.StorefrontClient
import mpbuilder.ui.storefront.*
import mpbuilder.ui.theme.AppTheme
import zio.ZIO
import zio.test.*

object StorefrontUiSpec extends ZIOSpecDefault:

  /** Boots a fresh isolated in-memory SampleShowcaseRuntime + StorefrontClient for a single test. */
  private def withClient[A](f: StorefrontClient => A < (Abort[Any] & Async)): ZIO[Any, Any, A] =
    ZIO.scoped {
      SampleShowcaseRuntime.layer().build.flatMap { env =>
        val facade   = env.get[StorefrontFacade]
        val otpInbox = env.get[InMemoryOtpInbox]
        val client   = StorefrontClient.fromZio(facade, otpInbox.codeFor)
        ZIOs.run(f(client))
      }
    }

  private def renderHtml(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle) {
      UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse(""))
    }

  override def spec: Spec[TestEnvironment, Any] =
    suite("Kyo UI Storefront & Calculator Spec")(
      test("Theme stylesheets emit responsive tablet (960px) and mobile (640px) media queries") {
        val storefrontCss = AppTheme.storefrontStylesheet.render
        val calcCss       = AppTheme.calculatorStylesheet.render
        assertTrue(
          storefrontCss.contains("@media (max-width: 960px)"),
          storefrontCss.contains("@media (max-width: 640px)"),
          storefrontCss.contains(".mp-split-layout"),
          storefrontCss.contains(".mp-header"),
          calcCss.contains("@media (max-width: 960px)"),
          calcCss.contains("@media (max-width: 640px)"),
          calcCss.contains(".mp-calc-root"),
        )
      },
      test("AppRoute hash routing round-trips all primary routes") {
        val catId    = CategoryId.unsafe("business-cards")
        val presetId = PresetId.unsafe("bc-standard")
        val routes = List(
          AppRoute.Catalog,
          AppRoute.ProductDetail(catId),
          AppRoute.Builder(None, None),
          AppRoute.Builder(Some(catId), None),
          AppRoute.Builder(Some(catId), Some(presetId)),
          AppRoute.Account,
        )
        assertTrue(
          routes.forall(r => AppRoute.fromHash(AppRoute.toHash(r)).contains(r)),
          AppRoute.fromHash("").contains(AppRoute.Catalog),
          AppRoute.fromHash("#/").contains(AppRoute.Catalog),
        )
      },
      test("i18n bundles render EN and CS cleanly without missing-key markers and switch reactively") {
        ZIOs.run {
          for
            appI18n <- Translations.init(Language.En)
            enTitle <- I18n.let(appI18n.handle)(appI18n.now("catalog-hero-title"))
            enBrand <- I18n.let(appI18n.handle)(appI18n.now("brand-name"))
            _       <- appI18n.setLanguage(Language.Cs)
            csTitle <- I18n.let(appI18n.handle)(appI18n.now("catalog-hero-title"))
            csCart  <- I18n.let(appI18n.handle)(appI18n.now("nav-cart"))
            lang    <- appI18n.currentLanguage
          yield assertTrue(
            enBrand == "PrintCraft Studio",
            enTitle.nonEmpty && !enTitle.contains("‹") && !enTitle.contains("›"),
            csTitle.nonEmpty && !csTitle.contains("‹") && !csTitle.contains("›"),
            enTitle != csTitle,
            csCart == "Košík",
            lang == Language.Cs,
          )
        }
      },
      test("StorefrontApp renders full catalog in EN and CS with all 15 sample showcase listings") {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state   <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n))
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            enHtml  <- renderHtml(ui, appI18n)
            _       <- I18n.let(appI18n.handle)(state.setLanguage(Language.Cs))
            csHtml  <- renderHtml(ui, appI18n)
          yield assertTrue(
            state.listings.size == 15,
            enHtml.contains("PrintCraft Studio"),
            enHtml.contains("Business Cards"),
            !enHtml.contains("‹") && !enHtml.contains("›"),
            csHtml.contains("Vizitky"),
            csHtml.contains("Košík"),
            !csHtml.contains("‹") && !csHtml.contains("›"),
          )
        }
      },
      test("ProductDetailView fetches live quotes for all presets in a category") {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state   <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n))
            catId    = state.listings.head.categoryId
            cat      = state.catalog.categories(catId)
            _       <- I18n.let(appI18n.handle)(state.openProductDetail(catId))
            quotes  <- state.presetQuotes.get
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            html    <- renderHtml(ui, appI18n)
          yield assertTrue(
            cat.presets.nonEmpty,
            quotes.size == cat.presets.size,
            quotes.values.forall {
              case QuoteStatus.Ready(res) => res.pricing.effectiveQuote.grossTotal.value > 0
              case _                      => false
            },
            html.contains(cat.name(Language.En)),
            !html.contains("‹") && !html.contains("›"),
          )
        }
      },
      test("Configurator computes live quote and unlocks B2B dual-pricing savings after B2B login") {
        withClient { client =>
          for
            appI18n    <- Translations.init(Language.En)
            state      <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n, initialRoute = AppRoute.Builder()))
            guestQuote <- state.configurator.quoteStatus.get
            auth       <- client.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _          <- I18n.let(appI18n.handle)(state.completeAuth(auth))
            b2bQuote   <- state.configurator.quoteStatus.get
            ui         <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            html       <- renderHtml(ui, appI18n)
          yield
            val guestReady = guestQuote match
              case QuoteStatus.Ready(r) => Some(r)
              case _                    => None
            val b2bReady = b2bQuote match
              case QuoteStatus.Ready(r) => Some(r)
              case _                    => None
            assertTrue(
              guestReady.isDefined,
              b2bReady.isDefined,
              guestReady.exists(g => g.pricing.savingsNet.isEmpty),
              b2bReady.exists(b => b.pricing.savingsNet.exists(_.value > 0)),
              b2bReady.exists(b => b.pricing.walkInBaseQuote.exists(w => b.pricing.effectiveQuote.netTotal.value < w.netTotal.value)),
              html.contains("Print Shop Pro") || html.contains("Jan"),
              !html.contains("‹") && !html.contains("›"),
            )
        }
      },
      test("Passwordless OTP authentication works end-to-end via Czech IČO + static demo OTP inbox") {
        withClient { client =>
          for
            challenge <- client.requestCustomerOtp(LoginIdentifier.CompanyRegNo(NonEmptyString.unsafe("12345678")))
            codeOpt   <- client.demoOtpCode(challenge.challengeId)
            code      <- Abort.get(codeOpt.toRight("Expected demo OTP code in static inbox"))
            auth      <- client.verifyOtp(challenge.challengeId, code)
          yield assertTrue(
            auth.customer.isDefined,
            auth.customer.flatMap(_.company).exists(_.legalName.value.contains("Print Shop Pro")),
          )
        }
      },
      test("Cart, SAVE10 promo code, BankTransferQR checkout with SVG QR, and Account Order History work end-to-end") {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state   <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n, initialRoute = AppRoute.Builder()))
            auth    <- client.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _       <- I18n.let(appI18n.handle)(state.completeAuth(auth))
            draft   <- state.configurator.draft.get
            _       <- I18n.let(appI18n.handle)(state.addDraftToCart(draft))
            cart1   <- state.cart.get
            _       <- I18n.let(appI18n.handle)(state.applyCartPromo("SAVE10"))
            cart2   <- state.cart.get
            _       <- I18n.let(appI18n.handle)(state.startCartCheckout)
            sessOpt <- state.checkoutSession.get
            sess    <- Abort.get(sessOpt.toRight("Expected initialized CheckoutSession"))
            _       <- sess.selectedPay.set(PaymentMethod.BankTransferQR)
            _       <- I18n.let(appI18n.handle)(sess.submitOrder(state))
            confOpt <- sess.confirmationRef.get
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            confHtml <- renderHtml(ui, appI18n)
            _       <- I18n.let(appI18n.handle)(state.openAccount)
            orders  <- state.orders.get
            acctHtml <- renderHtml(ui, appI18n)
          yield
            val hasQrInstructions = confOpt.exists { c =>
              c.paymentInstructions match
                case _: PaymentInstructions.BankTransferQR => true
                case _                                     => false
            }
            assertTrue(
              cart1.exists(_.lines.size == 1),
              cart2.exists(_.appliedPromoCode.contains("SAVE10")),
              cart2.exists(_.promoDiscountNet.value > 0),
              cart2.exists(c2 => cart1.exists(c1 => c2.netTotal.value < c1.netTotal.value)),
              confOpt.isDefined,
              hasQrInstructions,
              confHtml.contains("<svg"),
              confHtml.contains("CZ"),
              !confHtml.contains("‹") && !confHtml.contains("›"),
              orders.nonEmpty,
              confOpt.exists(c => orders.exists(_.orderNumber == c.order.orderNumber)),
              acctHtml.contains(confOpt.get.order.orderNumber.value),
              !acctHtml.contains("‹") && !acctHtml.contains("›"),
            )
        }
      },
      test("Standalone CalculatorView renders EN/CS with disclaimer and formats email quote summary") {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            calcUi  <- I18n.let(appI18n.handle)(
                         CalculatorView.render(
                           client = client,
                           i18n = appI18n,
                           config = CalculatorConfig(initialLanguage = Language.En, initialCurrency = Currency.CZK),
                         )
                       )
            enHtml  <- renderHtml(calcUi, appI18n)
            _       <- appI18n.setLanguage(Language.Cs)
            csHtml  <- renderHtml(calcUi, appI18n)
            catalog <- client.getCatalog
            cat      = catalog.categories.values.head
            draft    = ConfiguratorController.initialDraftForCategory(cat, catalog)
            curRef  <- Signal.initRef(Currency.CZK)
            noTok   <- Signal.initRef[Option[SessionToken]](None)
            ctrl    <- I18n.let(appI18n.handle)(ConfiguratorController.init(client, catalog, appI18n, curRef, noTok, Some(cat.id)))
            qStatus <- ctrl.quoteStatus.get
            summary  = qStatus match
                         case QuoteStatus.Ready(res) => CalculatorView.formatEmailSummary(catalog, draft, res, Currency.CZK, Language.En)
                         case _                      => ""
          yield assertTrue(
            enHtml.contains("mp-calc-root"),
            enHtml.contains("Print Price Calculator"),
            !enHtml.contains("‹") && !enHtml.contains("›"),
            csHtml.contains("Online kalkulačka tisku"),
            !csHtml.contains("‹") && !csHtml.contains("›"),
            summary.contains(cat.name(Language.En)),
            summary.contains("CZK"),
          )
        }
      },
      test("Customer sees color-coded tier badge in header, edits billing & multi-delivery addresses, tracks 5-step progress, views payment modal, and re-uploads artwork") {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state   <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n))
            auth    <- client.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _       <- I18n.let(appI18n.handle)(state.completeAuth(auth))
            _       <- I18n.let(appI18n.handle)(state.openAccount)
            // Update billing + 2 delivery addresses with index 1 as default
            newBilling = Address(
                           NonEmptyString.unsafe("Revoluční 22"),
                           NonEmptyString.unsafe("Praha"),
                           NonEmptyString.unsafe("110 00"),
                           NonEmptyString.unsafe("CZ"),
                         )
            deliv1 = Address(
                       NonEmptyString.unsafe("Sklepní 4"),
                       NonEmptyString.unsafe("Brno"),
                       NonEmptyString.unsafe("602 00"),
                       NonEmptyString.unsafe("CZ"),
                     )
            deliv2 = Address(
                       NonEmptyString.unsafe("Nádražní 15"),
                       NonEmptyString.unsafe("Ostrava"),
                       NonEmptyString.unsafe("702 00"),
                       NonEmptyString.unsafe("CZ"),
                     )
            _       <- I18n.let(appI18n.handle)(state.updateMyAddresses(newBilling, List(deliv1, deliv2), Some(1)))
            updated <- state.currentCustomer.get
            // Open payment modal for first order (if any) and test artwork reupload if seeded
            orders  <- state.orders.get
            _       <- orders.headOption match
                         case Some(o) => I18n.let(appI18n.handle)(state.openPaymentInstructions(o))
                         case None    => Kyo.unit
            artMap  <- state.orderArtworks.get
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            enHtml  <- renderHtml(ui, appI18n)
            _       <- I18n.let(appI18n.handle)(state.setLanguage(Language.Cs))
            csHtml  <- renderHtml(ui, appI18n)
            _       <- state.closePaymentInstructions
          yield assertTrue(
            updated.exists(_.billingAddress.street.value == "Revoluční 22"),
            updated.exists(_.shippingAddresses.size == 2),
            updated.exists(_.defaultShippingIndex.contains(1)),
            enHtml.contains("Gold"),
            enHtml.contains("mp-stepper"),
            enHtml.contains("Revoluční 22"),
            enHtml.contains("Nádražní 15"),
            artMap.values.flatten.nonEmpty || orders.nonEmpty,
            !enHtml.contains("‹") && !enHtml.contains("›"),
            csHtml.contains("Fakturační a doručovací adresy"),
            !csHtml.contains("‹") && !csHtml.contains("›"),
          )
        }
      },
      test("DataTable supports full-width -> vertical side-split detail, create-in-detail, checkbox filters (no counts), and multi-select textfield for >5 options") {
        import mpbuilder.ui.components.DataTable
        import kyo.UI.*
        final case class Row(id: String, name: String, smallTag: String, bigTag: String)
        val sampleRows = List(
          Row("r1", "Alpha", "One", "G1"),
          Row("r2", "Beta", "Two", "G2"),
          Row("r3", "Gamma", "Three", "G6"),
        )
        val cols = List(
          DataTable.Column[Row]("name", "Name", r => span(r.name), sortKey = Some(_.name)),
        )
        val filters = List(
          DataTable.FilterGroup[Row](
            id = "small",
            label = "Small",
            options = List("One", "Two", "Three").map(v => DataTable.FilterOption(v, v, _.smallTag == v)),
          ),
          DataTable.FilterGroup[Row](
            id = "big",
            label = "Many",
            options = List("G1", "G2", "G3", "G4", "G5", "G6").map(v => DataTable.FilterOption(v, v, _.bigTag == v)),
          ),
        )
        val css = AppTheme.storefrontStylesheet.render
        ZIOs.run {
          for
            i18n    <- Translations.init(Language.En)
            dtState <- DataTable.makeState[String]()
            ui = DataTable.render[Row, String](
                   title = Some(span("Demo Items")),
                   rows = sampleRows,
                   rowKey = _.id,
                   columns = cols,
                   state = dtState,
                   searchPlaceholder = "Search...",
                   matchesSearch = (r, q) => r.name.toLowerCase.contains(q),
                   filterGroups = filters,
                   renderDetail = Some(r => div(span(s"DETAIL:${r.name}"))),
                   createConfig = Some(DataTable.CreateConfig(span("+ Add Item"), span("New Demo Item"), _ => div(span("CREATE_FORM_BODY")))),
                 )
            // 1. Initial unselected state: full-width table, no detail pane, checkbox & multi-select rendered, no counts
            htmlInit <- renderHtml(ui, i18n)
            // 2. Select row r2 -> vertical side-split opens detail pane
            _        <- dtState.updateAndGet(_.copy(selectedKey = Some("r2")))
            htmlSel  <- renderHtml(ui, i18n)
            // 3. Create mode -> create form renders in detail pane
            _        <- dtState.updateAndGet(_.copy(selectedKey = None, isCreating = true))
            htmlNew  <- renderHtml(ui, i18n)
            // 4. Multi-select checkbox OR filter (One OR Three) + open >5 dropdown
            _        <- dtState.updateAndGet(_.copy(isCreating = false, activeFilters = Map("small" -> Set("One", "Three")), openDropdown = Some("big")))
            htmlFilt <- renderHtml(ui, i18n)
          yield assertTrue(
            htmlInit.contains("mp-dt-header-top"),
            htmlInit.contains("mp-dt-header-filters"),
            htmlInit.contains("mp-dt-check"),
            htmlInit.contains("mp-dt-ms-wrap"),
            !htmlInit.contains("One (1)"),
            !htmlInit.contains("mp-dt-detail-pane"),
            !htmlInit.contains("mp-dt-list-pane-split"),
            htmlSel.contains("mp-dt-list-pane-split"),
            htmlSel.contains("mp-dt-detail-pane"),
            htmlSel.contains("DETAIL:Beta"),
            htmlNew.contains("mp-dt-detail-pane"),
            htmlNew.contains("CREATE_FORM_BODY"),
            htmlFilt.contains("Alpha"),
            htmlFilt.contains("Gamma"),
            !htmlFilt.contains("Beta"),
            htmlFilt.contains("mp-dt-ms-menu"),
            css.contains(".mp-tab-active"),
            css.contains(".mp-dt-list-pane-split"),
          )
        }
      },
    )
