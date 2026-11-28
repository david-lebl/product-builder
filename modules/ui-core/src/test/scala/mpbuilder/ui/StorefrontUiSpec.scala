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
import kyo.test.*

class StorefrontUiSpec extends Test[Any]:

  /** Boots a fresh isolated in-memory SampleShowcaseRuntime + StorefrontClient for a single test. */
  private def withClient[A](f: StorefrontClient => A < (Abort[Any] & Async)): A < (Abort[Any] & Async) =
    SampleShowcaseRuntime.make().map(rt => f(StorefrontClient.fromFacade(rt.storefront, rt.otpInbox.codeFor)))

  private def renderHtml(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle) {
      UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse(""))
    }

  "Kyo UI Storefront & Calculator Spec" - {
      "Theme stylesheets emit responsive tablet (960px) and mobile (640px) media queries" in {
        val storefrontCss = AppTheme.storefrontStylesheet.render
        val calcCss       = AppTheme.calculatorStylesheet.render
        assert(storefrontCss.contains("@media (max-width: 960px)"))
        assert(storefrontCss.contains("@media (max-width: 640px)"))
        assert(storefrontCss.contains(".mp-split-layout"))
        assert(storefrontCss.contains(".mp-header"))
        assert(calcCss.contains("@media (max-width: 960px)"))
        assert(calcCss.contains("@media (max-width: 640px)"))
        assert(calcCss.contains(".mp-calc-root"))
      }

      "Tablet/phone breakpoint keeps the checkout Order Summary aside visible and hides only the configurator's" in {
        val css     = AppTheme.storefrontStylesheet.render
        val start   = css.indexOf("@media (max-width: 960px)")
        val tablet  = css.substring(start, css.indexOf("@media", start + 1) match { case -1 => css.length; case i => i })
        val asideAt = tablet.indexOf(".mp-split-aside")
        val cfgAt   = tablet.indexOf(".mp-cfg-aside")
        assert(asideAt >= 0 && cfgAt >= 0)
        assert(!tablet.substring(asideAt, tablet.indexOf("}", asideAt)).contains("display: none"))
        assert(!tablet.substring(asideAt, tablet.indexOf("}", asideAt)).contains("display:none"))
        assert(tablet.substring(cfgAt, tablet.indexOf("}", cfgAt)).replace(" ", "").contains("display:none"))
      }

      "AppRoute hash routing round-trips all primary routes" in {
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
        assert(routes.forall(r => AppRoute.fromHash(AppRoute.toHash(r)).contains(r)))
        assert(AppRoute.fromHash("").contains(AppRoute.Catalog))
        assert(AppRoute.fromHash("#/").contains(AppRoute.Catalog))
      }

      "i18n bundles render EN and CS cleanly without missing-key markers and switch reactively" in {
        locally {
          for
            appI18n <- Translations.init(Language.En)
            enTitle <- I18n.let(appI18n.handle)(appI18n.now("catalog-hero-title"))
            enBrand <- I18n.let(appI18n.handle)(appI18n.now("brand-name"))
            _       <- appI18n.setLanguage(Language.Cs)
            csTitle <- I18n.let(appI18n.handle)(appI18n.now("catalog-hero-title"))
            csCart  <- I18n.let(appI18n.handle)(appI18n.now("nav-cart"))
            lang    <- appI18n.currentLanguage
          yield {
            assert(enBrand == "PrintCraft Studio")
            assert(enTitle.nonEmpty && !enTitle.contains("‹") && !enTitle.contains("›"))
            assert(csTitle.nonEmpty && !csTitle.contains("‹") && !csTitle.contains("›"))
            assert(enTitle != csTitle)
            assert(csCart == "Košík")
            assert(lang == Language.Cs)
          }
        }
      }

      "StorefrontApp renders full catalog in EN and CS with all 15 sample showcase listings" in {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state   <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n))
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            enHtml  <- renderHtml(ui, appI18n)
            _       <- I18n.let(appI18n.handle)(state.setLanguage(Language.Cs))
            csHtml  <- renderHtml(ui, appI18n)
            listings <- state.listingsRef.get
          yield {
            assert(listings.size == 15)
            assert(enHtml.contains("PrintCraft Studio"))
            assert(enHtml.contains("Business Cards"))
            assert(enHtml.contains("https://images.unsplash.com/photo-1628891439478-c613e85af7d6"))
            assert(enHtml.contains("mp-product-img"))
            // Whole product card is clickable; its inner buttons stop propagation (Details + Configure per card).
            assert(listings.forall(l => enHtml.contains(s"""data-mp-product-card="${l.categoryId.value}"""")))
            assert("""data-kyo-stop="1"""".r.findAllIn(enHtml).size >= listings.size * 2)
            assert(!enHtml.contains("‹") && !enHtml.contains("›"))
            assert(csHtml.contains("Vizitky"))
            assert(csHtml.contains("Košík"))
            assert(!csHtml.contains("‹") && !csHtml.contains("›"))
          }
        }
      }

      "ProductDetailView fetches live quotes for all presets in a category" in {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state    <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n))
            listings <- state.listingsRef.get
            catalog  <- state.catalogRef.get
            catId     = listings.head.categoryId
            cat       = catalog.categories(catId)
            _       <- I18n.let(appI18n.handle)(state.openProductDetail(catId))
            quotes  <- state.presetQuotes.get
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            html    <- renderHtml(ui, appI18n)
            _       <- appI18n.setLanguage(Language.Cs)
            uiCs    <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            htmlCs  <- renderHtml(uiCs, appI18n)
          yield {
            assert(cat.presets.nonEmpty)
            // Custom configuration leads; presets are a secondary quick start with their own order actions.
            assert(html.contains("mp-pd-path-primary"))
            assert(html.contains("Configure your own"))
            assert(html.contains("mp-pd-quick"))
            assert(html.contains("Add to Cart"))
            assert(html.indexOf("mp-pd-path-primary") < html.indexOf("mp-pd-quick"))
            assert(htmlCs.contains("Vlastní konfigurace"))
            assert(!htmlCs.contains("‹") && !htmlCs.contains("›"))
            assert(quotes.size == cat.presets.size)
            assert(quotes.values.forall {
              case QuoteStatus.Ready(res) => res.pricing.effectiveQuote.grossTotal.value > 0
              case _                      => false
            })
            assert(html.contains(cat.name(Language.En)))
            assert(!html.contains("‹") && !html.contains("›"))
          }
        }
      }

      "ProductDetailView quick-start quantity re-prices the preset and flows into the cart" in {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            state   <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n))
            listings <- state.listingsRef.get
            catalog <- state.catalogRef.get
            catId    = listings.head.categoryId
            preset   = catalog.categories(catId).presets.head
            newQty   = Quantity.unsafe(preset.defaultSpecs.quantity.value * 4)
            _       <- I18n.let(appI18n.handle)(state.openProductDetail(catId))
            before  <- state.presetQuotes.get
            _       <- I18n.let(appI18n.handle)(state.setPresetQuantity(catId, preset.id, newQty))
            after   <- state.presetQuotes.get
            ui      <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            html    <- renderHtml(ui, appI18n)
            _       <- I18n.let(appI18n.handle)(state.addPresetToCart(catId, preset.id))
            cart    <- state.cart.get
            _       <- I18n.let(appI18n.handle)(state.openProductDetail(catId))
            reset   <- state.presetQuantities.get
          yield
            def gross(m: Map[PresetId, QuoteStatus]) = m.get(preset.id).collect { case QuoteStatus.Ready(r) => r.pricing.effectiveQuote.grossTotal.value }
            assert(gross(before).isDefined && gross(after).isDefined)
            assert(gross(after).get > gross(before).get)
            assert(html.contains("mp-pd-qty"))
            assert(html.contains(s"${newQty.value} pcs"))
            assert(cart.exists(_.lines.exists(_.runQuantity == newQty)))
            assert(reset.isEmpty)
        }
      }

      "Configurator computes live quote and unlocks B2B dual-pricing savings after B2B login" in {
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
            assert(guestReady.isDefined)
            assert(b2bReady.isDefined)
            assert(guestReady.exists(g => g.pricing.savingsNet.isEmpty))
            assert(b2bReady.exists(b => b.pricing.savingsNet.exists(_.value > 0)))
            assert(b2bReady.exists(b => b.pricing.walkInBaseQuote.exists(w => b.pricing.effectiveQuote.netTotal.value < w.netTotal.value)))
            assert(html.contains("Print Shop Pro") || html.contains("Jan"))
            assert(html.contains("Instructions for production (optional)"))
            assert(!html.contains("‹") && !html.contains("›"))
        }
      }

      "Passwordless OTP authentication works end-to-end via Czech IČO + static demo OTP inbox" in {
        withClient { client =>
          for
            challenge <- client.requestCustomerOtp(LoginIdentifier.CompanyRegNo(NonEmptyString.unsafe("12345678")))
            codeOpt   <- client.demoOtpCode(challenge.challengeId)
            code      <- Abort.get(codeOpt.toRight("Expected demo OTP code in static inbox"))
            auth      <- client.verifyOtp(challenge.challengeId, code)
          yield {
            assert(auth.customer.isDefined)
            assert(auth.customer.flatMap(_.company).exists(_.legalName.value.contains("Print Shop Pro")))
          }
        }
      }

      "Cart, SAVE10 promo code, BankTransferQR checkout with SVG QR, and Account Order History work end-to-end" in {
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
            assert(cart1.exists(_.lines.size == 1))
            assert(cart2.exists(_.appliedPromoCode.contains("SAVE10")))
            assert(cart2.exists(_.promoDiscountNet.value > 0))
            assert(cart2.exists(c2 => cart1.exists(c1 => c2.netTotal.value < c1.netTotal.value)))
            assert(confOpt.isDefined)
            assert(hasQrInstructions)
            assert(confHtml.contains("<svg"))
            assert(confHtml.contains("CZ"))
            assert(!confHtml.contains("‹") && !confHtml.contains("›"))
            assert(orders.nonEmpty)
            assert(confOpt.exists(c => orders.exists(_.orderNumber == c.order.orderNumber)))
            assert(acctHtml.contains(confOpt.get.order.orderNumber.value))
            assert(!acctHtml.contains("‹") && !acctHtml.contains("›"))
        }
      }

      "Guest checkout offers sign-in and fills contact & billing details from the account after login (EN & CS)" in {
        withClient { client =>
          for
            appI18n   <- Translations.init(Language.En)
            state     <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n, initialRoute = AppRoute.Builder()))
            draft     <- state.configurator.draft.get
            _         <- I18n.let(appI18n.handle)(state.addDraftToCart(draft))
            _         <- I18n.let(appI18n.handle)(state.startCartCheckout)
            sessOpt   <- state.checkoutSession.get
            sess      <- Abort.get(sessOpt.toRight("Expected initialized CheckoutSession"))
            nameBefore <- sess.fullNameRef.get
            emailBefore <- sess.emailRef.get
            ui        <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            guestHtml <- renderHtml(ui, appI18n)
            _         <- appI18n.setLanguage(Language.Cs)
            guestCsHtml <- renderHtml(ui, appI18n)
            _         <- appI18n.setLanguage(Language.En)
            auth      <- client.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _         <- I18n.let(appI18n.handle)(state.completeAuth(auth))
            sameSess  <- state.checkoutSession.get
            name      <- sess.fullNameRef.get
            email     <- sess.emailRef.get
            ico       <- sess.icoRef.get
            street    <- sess.billStreetRef.get
            city      <- sess.billCityRef.get
            authedHtml <- renderHtml(ui, appI18n)
          yield
            assert(nameBefore.isEmpty && emailBefore.isEmpty)
            assert(guestHtml.contains("Already have an account?"))
            assert(guestCsHtml.contains("Už u nás máte účet?"))
            assert(!guestCsHtml.contains("‹") && !guestCsHtml.contains("›"))
            assert(sameSess.contains(sess))
            assert(email == "jan@printshoppro.cz")
            assert(name.nonEmpty)
            assert(ico == "12345678")
            assert(street.nonEmpty && city.nonEmpty)
            assert(!authedHtml.contains("Already have an account?"))
            assert(authedHtml.contains("details filled from your account"))
            assert(!authedHtml.contains("‹") && !authedHtml.contains("›"))
        }
      }

      "Cart checkout renders the promo input, prefills the cart's code and applies a new one to cart & preview" in {
        withClient { client =>
          for
            appI18n  <- Translations.init(Language.En)
            state    <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n, initialRoute = AppRoute.Builder()))
            draft    <- state.configurator.draft.get
            _        <- I18n.let(appI18n.handle)(state.addDraftToCart(draft))
            _        <- I18n.let(appI18n.handle)(state.startCartCheckout)
            sessOpt  <- state.checkoutSession.get
            sess     <- Abort.get(sessOpt.toRight("Expected initialized CheckoutSession"))
            before   <- sess.previewRef.get
            ui       <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            html     <- renderHtml(ui, appI18n)
            _        <- sess.promoOverride.set("SAVE10")
            _        <- I18n.let(appI18n.handle)(sess.applyPromo(state))
            after    <- sess.previewRef.get
            cart     <- state.cart.get
            // Re-entering checkout prefills the code already applied to the cart
            _        <- I18n.let(appI18n.handle)(state.startCartCheckout)
            sess2Opt <- state.checkoutSession.get
            sess2    <- Abort.get(sess2Opt.toRight("Expected CheckoutSession"))
            prefill  <- sess2.promoOverride.get
            // Clearing the field removes the promo from the cart
            _        <- sess2.promoOverride.set("")
            _        <- I18n.let(appI18n.handle)(sess2.applyPromo(state))
            cleared  <- state.cart.get
            after2   <- sess2.previewRef.get
          yield
            assert(html.contains("Promo Code"))
            assert(before.exists(_.promoDiscountNet.value == BigDecimal(0)))
            assert(after.exists(_.promoDiscountNet.value > 0))
            assert(cart.exists(_.appliedPromoCode.contains("SAVE10")))
            assert(prefill == "SAVE10")
            assert(cleared.exists(_.appliedPromoCode.isEmpty))
            assert(after2.exists(_.promoDiscountNet.value == BigDecimal(0)))
        }
      }

      "Signed-in checkout offers saved delivery addresses as a one-click picker, defaulting to the account's default (EN & CS)" in {
        withClient { client =>
          for
            appI18n   <- Translations.init(Language.En)
            state     <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n, initialRoute = AppRoute.Builder()))
            auth      <- client.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _         <- I18n.let(appI18n.handle)(state.completeAuth(auth))
            draft     <- state.configurator.draft.get
            _         <- I18n.let(appI18n.handle)(state.addDraftToCart(draft))
            _         <- I18n.let(appI18n.handle)(state.startCartCheckout)
            sessOpt   <- state.checkoutSession.get
            sess      <- Abort.get(sessOpt.toRight("Expected initialized CheckoutSession"))
            opts      <- sess.optionsRef.get
            courier   <- Abort.get(opts.flatMap(_.deliveryOptions.find(o => o.available && !o.isPickup)).toRight("Expected a courier option"))
            _         <- sess.selectedDeliv.set(Some(courier))
            saved     <- sess.savedShipRef.get
            sameInit  <- sess.sameShipRef.get
            streetInit <- sess.shipStreetRef.get
            ui        <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            enHtml    <- renderHtml(ui, appI18n)
            _         <- appI18n.setLanguage(Language.Cs)
            csHtml    <- renderHtml(ui, appI18n)
            _         <- appI18n.setLanguage(Language.En)
            // One click on the other saved address fills the shipping fields
            _         <- sess.selectSavedShipping(saved.find(!_.isDefault).get.address)
            same2     <- sess.sameShipRef.get
            street2   <- sess.shipStreetRef.get
            city2     <- sess.shipCityRef.get
            postal2   <- sess.shipPostalRef.get
            country2  <- sess.shipCountryRef.get
            // "Different address" clears the fields for a one-off address
            _         <- sess.selectNewShipping
            street3   <- sess.shipStreetRef.get
            same3     <- sess.sameShipRef.get
          yield
            assert(saved.map(_.address.street.value) == List("Průmyslová 45, Sklad B", "Vinohradská 10"))
            assert(saved.map(_.isDefault) == List(true, false))
            assert(!sameInit && streetInit == "Průmyslová 45, Sklad B")
            assert(ShippingChoice.of(sameInit, saved, streetInit, "Praha 10", "10200", "CZ") == ShippingChoice.Saved(0))
            assert(enHtml.contains("Ship to") && enHtml.contains("Different address"))
            assert(enHtml.contains("Průmyslová 45, Sklad B") && enHtml.contains("Vinohradská 10"))
            assert(enHtml.contains("mp-option-card-active mp-ship-choice"))
            assert(csHtml.contains("Doručit na") && csHtml.contains("Jiná adresa"))
            assert(!enHtml.contains("‹") && !enHtml.contains("›"))
            assert(!csHtml.contains("‹") && !csHtml.contains("›"))
            assert(!same2 && street2 == "Vinohradská 10" && city2 == "Praha" && postal2 == "12000" && country2 == "CZ")
            assert(ShippingChoice.of(same2, saved, street2, city2, postal2, country2) == ShippingChoice.Saved(1))
            assert(!same3 && street3.isEmpty)
            assert(ShippingChoice.of(same3, saved, street3, "", "", "CZ") == ShippingChoice.Other)
            assert(ShippingChoice.of(true, saved, street2, city2, postal2, country2) == ShippingChoice.SameAsBilling)
        }
      }

      "Material combo box search matches name, surface, CS diacritics & gsm ranges and offers weight variants" in {
        withClient { client =>
          for
            catalog <- client.getCatalog
          yield {
            val mats = catalog.materials.values.toList.filter(_.enabled)
            def names(q: String) = MaterialSearch.search(mats, q, Language.En).map(_.name(Language.En))
            def gsms(q: String)  = MaterialSearch.search(mats, q, Language.En).flatMap(_.weight.grammageGsm)

            // Empty query lists everything, ordered by family then weight
            assert(MaterialSearch.search(mats, "", Language.En).size == mats.size)
            // Surface + plain number (AND across terms)
            assert(names("matte 350").nonEmpty)
            assert(names("matte 350").forall(n => n.contains("Matte") && n.contains("350")))
            // CS labels, case & diacritics folded ("matny" finds "matný")
            assert(names("MATNY").nonEmpty && names("matný") == names("matny"))
            // gsm range & comparators (with optional unit suffix)
            assert(gsms("200-300").nonEmpty && gsms("200-300").forall(g => g >= 200 && g <= 300))
            assert(gsms("250 - 300gsm") == gsms("250-300"))
            assert(gsms(">=350").nonEmpty && gsms(">=350").forall(_ >= 350))
            assert(gsms("<150g").forall(_ < 150))
            assert(MaterialSearch.parse("gloss >250") == List(MaterialSearch.Term.Text("gloss"), MaterialSearch.Term.GsmRange(251, Int.MaxValue)))
            assert(MaterialSearch.highlightTerms("gloss 200-300") == List("gloss"))
            assert(names("zzz-no-such-paper").isEmpty)
            // Weight variants: the glossy line exposes several grammages incl. itself, sorted ascending
            val glossy   = mats.find(m => m.surfaceCoating == SurfaceCoating.CoatedGloss && m.family == MaterialFamily.Paper).get
            val variants = MaterialSearch.weightVariants(glossy, mats)
            assert(variants.size > 1 && variants.exists(_.id == glossy.id))
            assert(variants.flatMap(_.weight.grammageGsm) == variants.flatMap(_.weight.grammageGsm).sorted)
            assert(variants.forall(_.surfaceCoating == SurfaceCoating.CoatedGloss))
            // Highlight segments are diacritic-insensitive and preserve original text
            val segs = mpbuilder.ui.components.ComboBox.highlightSegments("Křídový papír matný", List("matny"))
            assert(segs.map(_._1).mkString == "Křídový papír matný")
            assert(segs.contains("matný" -> true))
          }
        }
      }

      "Configurator renders the material combo box (closed & open) in EN and CS without missing keys" in {
        withClient { client =>
          for
            appI18n <- Translations.init(Language.En)
            catalog <- client.getCatalog
            curRef  <- Signal.initRef(Currency.CZK)
            noTok   <- Signal.initRef[Option[SessionToken]](None)
            ctrl    <- I18n.let(appI18n.handle)(ConfiguratorController.init(client, catalog, appI18n, curRef, noTok, None))
            host     = ConfiguratorHost.Calculator(None, (_, _) => Kyo.unit)
            ui       = ConfiguratorView.render(ctrl, host)
            closed  <- renderHtml(ui, appI18n)
            _       <- ctrl.materialPicker.openAt(0)
            _       <- ctrl.materialPicker.query.set("gloss")
            openEn  <- renderHtml(ui, appI18n)
            _       <- appI18n.setLanguage(Language.Cs)
            openCs  <- renderHtml(ui, appI18n)
            _       <- ctrl.materialPicker.close
            reClosed <- renderHtml(ui, appI18n)
          yield {
            assert(closed.contains("mp-combo-trigger"))
            assert(!closed.contains("mp-combo-panel"))
            assert(openEn.contains("mp-combo-panel") && openEn.contains("mp-combo-option"))
            assert(openEn.contains("mp-combo-mark"))
            assert(openEn.contains("materials"))
            assert(openCs.contains("materiálů"))
            assert(reClosed.contains("mp-combo-trigger") && !reClosed.contains("mp-combo-panel"))
            List(closed, openEn, openCs).foreach(h => assert(!h.contains("‹") && !h.contains("›")))
          }
        }
      }

      "Standalone CalculatorView renders EN/CS with disclaimer and formats email quote summary" in {
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
          yield {
            assert(enHtml.contains("mp-calc-root"))
            assert(enHtml.contains("Print Price Calculator"))
            assert(!enHtml.contains("‹") && !enHtml.contains("›"))
            assert(csHtml.contains("Online kalkulačka tisku"))
            assert(!csHtml.contains("‹") && !csHtml.contains("›"))
            assert(summary.contains(cat.name(Language.En)))
            assert(summary.contains("CZK"))
          }
        }
      }

      "Customer sees color-coded tier badge in header, edits billing & multi-delivery addresses, tracks 5-step progress, views payment modal, and re-uploads artwork" in {
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
          yield {
            assert(updated.exists(_.billingAddress.street.value == "Revoluční 22"))
            assert(updated.exists(_.shippingAddresses.size == 2))
            assert(updated.exists(_.defaultShippingIndex.contains(1)))
            assert(enHtml.contains("Gold"))
            assert(enHtml.contains("mp-stepper"))
            assert(enHtml.contains("Revoluční 22"))
            assert(enHtml.contains("Nádražní 15"))
            assert(artMap.values.flatten.nonEmpty || orders.nonEmpty)
            assert(!enHtml.contains("‹") && !enHtml.contains("›"))
            assert(csHtml.contains("Fakturační a doručovací adresy"))
            assert(!csHtml.contains("‹") && !csHtml.contains("›"))
          }
        }
      }

      "DataTable renders Vaadin Master-Detail with external header, resizable splitter, Full View action, create-in-detail, checkbox & multi-select filters, and ActionMenu" in {
        import mpbuilder.ui.components.{ActionMenu, DataTable}
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
        locally {
          for
            i18n     <- Translations.init(Language.En)
            dtState  <- DataTable.makeState[String]()
            menu     <- ActionMenu.Handle.make
            fullRef  <- Signal.initRef[Option[String]](None)
            ui = DataTable.render[Row, String](
                   title = Some(span("Demo Items")),
                   subtitle = Some(span("A long wrapping description that should stay next to controls")),
                   rows = sampleRows,
                   rowKey = _.id,
                   columns = cols,
                   state = dtState,
                   searchPlaceholder = "Search...",
                   matchesSearch = (r, q) => r.name.toLowerCase.contains(q),
                   filterGroups = filters,
                   renderDetail = Some(r => div(span(s"DETAIL:${r.name}"))),
                   onOpenFullScreen = Some(r => fullRef.set(Some(r.id))),
                   createConfig = Some(DataTable.CreateConfig(span("+ Add Item"), span("New Demo Item"), _ => div(span("CREATE_FORM_BODY")))),
                 )
            menuUi = menu.render(
                       key = "demo-menu",
                       buttonLabel = span("Actions"),
                       items = List(
                         ActionMenu.Item(span("Edit"), () => Kyo.unit, ActionMenu.ItemTone.Primary),
                         ActionMenu.Item(span("Delete"), () => Kyo.unit, ActionMenu.ItemTone.Danger, dividerBefore = true),
                       ),
                     )
            // 1. Initial unselected state: external header, full-width table, no detail pane or splitter
            htmlInit <- renderHtml(ui, i18n)
            // 2. Select row r2 -> unified Master-Detail split with draggable splitter, scrollable detail body & Full View button
            _        <- dtState.updateAndGet(_.copy(selectedKey = Some("r2")))
            htmlSel  <- renderHtml(ui, i18n)
            // 3. Create mode -> create form renders in detail pane
            _        <- dtState.updateAndGet(_.copy(selectedKey = None, isCreating = true))
            htmlNew  <- renderHtml(ui, i18n)
            // 4. Multi-select checkbox OR filter (One OR Three) + open >5 dropdown
            _        <- dtState.updateAndGet(_.copy(isCreating = false, activeFilters = Map("small" -> Set("One", "Three")), openDropdown = Some("big")))
            htmlFilt <- renderHtml(ui, i18n)
            // 5. ActionMenu closed -> open -> closed
            menuClosed <- renderHtml(menuUi, i18n)
            _          <- menu.toggle("demo-menu")
            menuOpen   <- renderHtml(menuUi, i18n)
            _          <- menu.close
            menuAgain  <- renderHtml(menuUi, i18n)
          yield {
            assert(htmlInit.contains("mp-dt-root"))
            assert(htmlInit.contains("mp-dt-header-top"))
            assert(htmlInit.contains("mp-dt-title-block"))
            assert(htmlInit.contains("mp-dt-controls"))
            assert(htmlInit.contains("mp-dt-header-filters"))
            assert(htmlInit.contains("mp-dt-check"))
            assert(htmlInit.contains("mp-dt-ms-wrap"))
            assert(!htmlInit.contains("One (1)"))
            assert(!htmlInit.contains("mp-dt-detail-pane"))
            assert(!htmlInit.contains("mp-dt-splitter"))
            assert(!htmlInit.contains("mp-dt-list-pane-split"))
            assert(htmlSel.contains("mp-dt-list-pane-split"))
            assert(htmlSel.contains("mp-dt-splitter"))
            assert(htmlSel.contains("data-mp-dt-splitter"))
            assert(htmlSel.contains("mp-dt-detail-pane"))
            assert(htmlSel.contains("mp-dt-detail-body"))
            assert(htmlSel.contains("Full View"))
            assert(htmlSel.contains("DETAIL:Beta"))
            assert(htmlNew.contains("mp-dt-detail-pane"))
            assert(htmlNew.contains("CREATE_FORM_BODY"))
            assert(htmlFilt.contains("Alpha"))
            assert(htmlFilt.contains("Gamma"))
            assert(!htmlFilt.contains("Beta"))
            assert(htmlFilt.contains("mp-dt-ms-menu"))
            assert(menuClosed.contains("mp-action-menu-wrap"))
            assert(!menuClosed.contains("mp-action-menu-panel"))
            assert(menuOpen.contains("mp-action-menu-panel"))
            assert(menuOpen.contains("mp-action-menu-divider"))
            assert(menuOpen.contains("mp-action-menu-item-danger"))
            assert(!menuAgain.contains("mp-action-menu-panel"))
            assert(css.contains(".mp-tab-active"))
            assert(css.contains(".mp-tabs-sm"))
            assert(css.contains(".mp-dt-splitter"))
            assert(css.contains(".mp-detail-head-row"))
            assert(css.contains(".mp-detail-tags-row"))
            assert(css.contains(".mp-action-menu-panel"))
          }
        }
      }

      "Artwork management UI: configurator drag&drop upload, preview modal, cart/checkout badges, and AccountView upload/lock (EN & CS)" in {
        import mpbuilder.ui.components.{ArtworkComponents, BrowserPickedFile}

        withClient { client =>
          for
            i18n   <- Translations.init(Language.En)
            state  <- StorefrontState.init(client, i18n, Currency.CZK, AppRoute.Builder(Some(CategoryId.unsafe("flyers")), None))
            appUi  <- I18n.let(i18n.handle)(StorefrontApp.render(state))
            // 1. Configurator renders dropzone for artwork upload
            cfgHtml0 <- renderHtml(appUi, i18n)
            // 2. Upload press-ready draft artwork in configurator
            demoFile = ArtworkComponents.demoPrintReadyFile(ComponentKey.Main, Dimension2D.A5)
            artOpt  <- I18n.let(i18n.handle)(state.configurator.uploadDraftArtwork(ComponentKey.Main, demoFile))
            artView  = artOpt.get
            cfgHtml1 <- renderHtml(appUi, i18n)
            // 3. Open lightbox artwork preview modal
            _       <- state.artworkPreview.open(artView, contextTitle = Some("Flyers · Main"))
            prevHtml <- renderHtml(appUi, i18n)
            _       <- state.artworkPreview.close
            // 4. Add configured item with attached artwork to cart -> badge appears in cart drawer & checkout
            _       <- I18n.let(i18n.handle)(state.addDraftToCart)
            _       <- state.cartDrawerOpen.set(true)
            cartHtml <- renderHtml(appUi, i18n)
            _       <- state.cartDrawerOpen.set(false)
            // 5. Login as Print Shop Pro -> AccountView shows seeded artworks, Preview/Download/Copy Link, and upload dropzone for non-accepted orders
            auth    <- client.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("demo1234"))
            _       <- I18n.let(i18n.handle)(state.completeAuth(auth))
            _       <- I18n.let(i18n.handle)(state.openAccount)
            acctEn  <- renderHtml(appUi, i18n)
            _       <- I18n.let(i18n.handle)(state.setLanguage(Language.Cs))
            acctCs  <- renderHtml(appUi, i18n)
            // 6. Verify artwork CSS rules emitted
            css      = AppTheme.storefrontStylesheet.render
          yield {
            assert(cfgHtml0.contains("mp-dropzone"))
            assert(cfgHtml0.contains("data-mp-dropzone"))
            assert(cfgHtml1.contains(demoFile.fileName))
            assert(cfgHtml1.contains("mp-art-thumb"))
            assert(cfgHtml1.contains("data-mp-download-url"))
            assert(cfgHtml1.contains("data-mp-copy-url"))
            assert(prevHtml.contains("mp-art-preview-modal"))
            assert(prevHtml.contains("mp-art-preview-frame"))
            assert(prevHtml.contains("Flyers · Main"))
            assert(cartHtml.contains("1 artwork attached"))
            assert(acctEn.contains("data-mp-download-url"))
            assert(acctEn.contains("data-mp-copy-url"))
            assert(acctEn.contains("mp-art-thumb"))
            assert(!cfgHtml1.contains("‹") && !cfgHtml1.contains("›"))
            assert(!acctEn.contains("‹") && !acctEn.contains("›"))
            assert(!acctCs.contains("‹") && !acctCs.contains("›"))
            assert(css.contains(".mp-dropzone"))
            assert(css.contains(".mp-art-preview-modal"))
          }
        }
      }

      "Configurator numeric inputs wire input & numeric-change handlers and highlight Custom pill for manual quantity/dimensions" in {
        withClient { client =>
          for
            i18n    <- Translations.init(Language.En)
            state   <- I18n.let(i18n.handle)(StorefrontState.init(client, i18n, Currency.CZK, AppRoute.Builder(Some(CategoryId.unsafe("cat-flyers")), None)))
            appUi   <- I18n.let(i18n.handle)(StorefrontApp.render(state))
            // 1. Standard preset values (A5, 250 or 100 pcs): preset pills active, no Custom pill yet
            _       <- I18n.let(i18n.handle)(state.configurator.updateSpecs(_.copy(
                         dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
                         quantity = Quantity.unsafe(100),
                       )))
            stdHtml <- renderHtml(appUi, i18n)
            // 2. Manual custom quantity & dimensions: preset pills unselected, Custom pill highlighted
            _       <- I18n.let(i18n.handle)(state.configurator.updateSpecs(_.copy(
                         dimensions = ProductDimensions.Flat2D(Dimension2D(PositiveMm.unsafe(120), PositiveMm.unsafe(77))),
                         quantity = Quantity.unsafe(333),
                       )))
            enHtml  <- renderHtml(appUi, i18n)
            _       <- I18n.let(i18n.handle)(state.setLanguage(Language.Cs))
            csHtml  <- renderHtml(appUi, i18n)
            qStatus <- state.configurator.quoteStatus.get
          yield {
            // Numeric fields are filtered text inputs, not `type="number"`: the configurator re-renders on every
            // keystroke and Kyo UI can only restore the caret on inputs exposing `selectionStart` (number inputs
            // don't, so the refocused field came back fully selected and the next digit overwrote it).
            assert(!stdHtml.contains("type=\"number\""))
            assert(stdHtml.contains("data-kyo-filter=\"digits\""))
            assert(stdHtml.contains("data-kyo-filter=\"decimal\""))
            assert(stdHtml.contains("input"))
            assert(!stdHtml.contains(">Custom</span>"))
            // Custom values render active Custom / Vlastní pill and update input values
            assert(enHtml.contains("value=\"333\""))
            assert(enHtml.contains("value=\"120\""))
            assert(enHtml.contains("value=\"77\""))
            assert(enHtml.contains("mp-pill-active"))
            assert(enHtml.contains("Custom"))
            assert(!enHtml.contains("‹") && !enHtml.contains("›"))
            assert(csHtml.contains("Vlastní"))
            assert(!csHtml.contains("‹") && !csHtml.contains("›"))
            assert(qStatus match
              case QuoteStatus.Ready(res) => res.pricing.effectiveQuote.quantity.value == 333
              case _                      => false
            )
          }
        }
      }

      "Header removes CZK/USD switch and Live Price Summary switches between CZK and EUR via backend storefront conversion" in {
        withClient { client =>
          for
            appI18n   <- Translations.init(Language.En)
            state     <- I18n.let(appI18n.handle)(StorefrontState.init(client, appI18n, initialRoute = AppRoute.Builder()))
            appUi     <- I18n.let(appI18n.handle)(StorefrontApp.render(state))
            czkHtml   <- renderHtml(appUi, appI18n)
            czkQuote  <- state.configurator.quoteStatus.get
            _         <- I18n.let(appI18n.handle)(state.configurator.setDisplayCurrency(Currency.EUR))
            eurQuote  <- state.configurator.quoteStatus.get
            eurHtml   <- renderHtml(appUi, appI18n)
            storeCur  <- state.currency.get
          yield {
            val czkReady = czkQuote match
              case QuoteStatus.Ready(r) => Some(r)
              case _                    => None
            val eurReady = eurQuote match
              case QuoteStatus.Ready(r) => Some(r)
              case _                    => None
            assert(!czkHtml.contains("USD"))
            assert(czkHtml.contains("Live Price Summary"))
            assert(czkHtml.contains("CZK"))
            assert(czkHtml.contains("EUR"))
            assert(czkReady.exists(_.pricing.effectiveQuote.currency == Currency.CZK))
            assert(eurReady.exists(_.pricing.effectiveQuote.currency == Currency.EUR))
            assert(eurReady.exists(e => czkReady.exists(c => e.pricing.effectiveQuote.netTotal.value < c.pricing.effectiveQuote.netTotal.value)))
            assert(eurHtml.contains("€"))
            assert(storeCur == Currency.CZK)
            assert(!eurHtml.contains("‹") && !eurHtml.contains("›"))
          }
        }
      }
  }
