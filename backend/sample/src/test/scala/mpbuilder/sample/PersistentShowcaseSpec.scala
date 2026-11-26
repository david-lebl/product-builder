package mpbuilder.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.sample.{CatalogSampleWiring, SampleCatalog, SampleRules}
import mpbuilder.customers.sample.CustomerSampleWiring
import mpbuilder.ordering.sample.OrderingSampleWiring
import mpbuilder.pricing.sample.*
import mpbuilder.identity.sample.SampleIdentity
import mpbuilder.ordering.*
import mpbuilder.ordering.sample.SampleDeliveryCatalog
import mpbuilder.shared.*
import mpbuilder.storefront.*
import kyo.*
import kyo.test.*
import ShowcaseTestEnv.*
import java.time.Instant

/** The browser demo's persistence ([[SampleStorage]] over a [[DocStore]]), with a page reload simulated by booting a
  * second [[SampleShowcaseRuntime]] from the same store (`DocStore.inMemory` stands in for IndexedDB).
  */
class PersistentShowcaseSpec extends Test[Any]:

  private val validNow = Instant.parse("2025-06-15T12:00:00Z")

  private val pragueAddress = Address(
    street = NonEmptyString.unsafe("Vinohradská 10"),
    city = NonEmptyString.unsafe("Praha"),
    postalCode = NonEmptyString.unsafe("12000"),
    countryCode = NonEmptyString.unsafe("CZ"),
  )

  private val guest = OrderCustomerSnapshot(
    customerId = None,
    segment = CustomerSegment.Individual,
    fullName = NonEmptyString.unsafe("Anna Nová"),
    email = EmailAddress.unsafe("anna@example.com"),
  )

  private val flyers = ConfigureProductCmd(
    configurationId = ConfigurationId.unsafe("cfg-flyers"),
    categoryId = SampleCatalog.flyersId,
    components = NonEmptyChunk.single(
      ComponentSelection(
        key = ComponentKey.Main,
        materialId = SampleCatalog.coatedGlossy170gsmId,
        printingMethodId = Some(SampleCatalog.digitalId),
        inkConfiguration = Some(InkConfiguration.cmyk4_4),
      )
    ),
    specs = ProductSpecs(
      dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
      quantity = Quantity.unsafe(250),
      orientation = Some(Orientation.Portrait),
    ),
  )

  private def boot(store: DocStore): ShowcaseRuntime < Async =
    SampleStorage.load(store).map(storage => SampleShowcaseRuntime.make(storage = storage))

  private def placeGuestOrder(facade: StorefrontFacade, cart: String, promo: Option[String]) =
    val cartId = CartId.unsafe(cart)
    for
      _       <- facade.createCart(cartId, Currency.CZK)
      _       <- facade.addConfiguredProductToCart(cartId, flyers, Currency.CZK, copies = Quantity.unsafe(1))
      _       <- Kyo.foreachDiscard(promo.toList)(code => facade.applyCartPromo(cartId, code).unit)
      confirm <- facade.submitCheckout(
                   SubmitCheckoutCmd(
                     sessionToken = None,
                     source = CheckoutSource.FromCart(cartId),
                     customer = guest,
                     billingAddress = pragueAddress,
                     deliveryChoice = DeliveryOptionChoice.Courier(SampleDeliveryCatalog.courierStandardId, pragueAddress),
                     paymentMethod = PaymentMethod.BankTransferQR,
                   )
                 )
    yield confirm.order

  "PersistentShowcaseSpec" - {
    "orders, promo usage, sessions and the ID sequence survive a reload" in {
      for
        store    <- DocStore.inMemory
        first    <- boot(store)
        order1   <- placeGuestOrder(first.storefront, "cart-1", Some("FREESHIP"))
        promos1  <- first.pricing.listPromoCodes
        auth     <- first.storefront.loginWithPassword(EmailAddress.unsafe("marie@graphicdesign.cz"), SampleIdentity.demoCustomerPassword)
        // ── page reload ──
        second   <- boot(store)
        restored <- second.orders.getOrder(order1.id)
        all      <- second.orders.listOrders()
        promos2  <- second.pricing.listPromoCodes
        customer <- second.storefront.currentCustomer(auth.session.token)
        relogin  <- second.storefront.loginWithPassword(EmailAddress.unsafe("marie@graphicdesign.cz"), SampleIdentity.demoCustomerPassword)
        order2   <- placeGuestOrder(second.storefront, "cart-2", None)
      yield {
        assert(restored == order1)
        // The 4 seeded demo orders are kept alongside the new one.
        assert(all.map(_.id).toSet == Set(order1.id) ++ SampleSeedIds.orders)
        def uses(ps: List[mpbuilder.pricing.PromoCode]) = ps.find(_.code.value == "FREESHIP").map(_.constraints.currentUses)
        assert(uses(promos2) == uses(promos1))
        assert(uses(promos2).exists(_ > 0))
        assert(customer.contact.email.value == "marie@graphicdesign.cz")
        assert(relogin.session.token != auth.session.token)
        // The ID sequence continues, so new orders never overwrite stored ones.
        assert(order2.id != order1.id && order2.orderNumber != order1.orderNumber)
      }
    }.atTime(validNow)

    "every persisted sample collection round-trips losslessly through its stored JSON" in {
      for
        store  <- DocStore.inMemory
        s1     <- SampleStorage.load(store)
        // Saving an unchanged value still materializes the whole collection (first write).
        cat1   <- CatalogSampleWiring.repository(storage = s1)
        _      <- cat1.saveCatalog(SampleCatalog.catalog)
        _      <- cat1.saveRuleset(SampleRules.ruleset)
        pl1    <- PricingSampleWiring.pricelistRepository(storage = s1)
        _      <- pl1.save(SamplePricelist.pricelistUsd)
        ag1    <- PricingSampleWiring.agreementRepository(storage = s1)
        _      <- ag1.save(SampleAgreements.all.head)
        pc1    <- PricingSampleWiring.promoRepository(storage = s1)
        _      <- pc1.save(SamplePromoCodes.all.head)
        cs1    <- PricingSampleWiring.costSheetRepository(storage = s1)
        _      <- cs1.save(SampleCostSheets.costSheetUsd)
        cu1    <- CustomerSampleWiring.repository(storage = s1)
        seedCu <- cu1.listAll
        _      <- cu1.save(seedCu.head)
        dc1    <- OrderingSampleWiring.deliveryCatalog(s1)
        _      <- dc1.savePickupLocation(SampleDeliveryCatalog.pickupLocations.head)
        _      <- dc1.saveCourierOption(SampleDeliveryCatalog.courierOptionsByCurrency(Currency.CZK).head)
        _      <- dc1.saveBankAccount(Currency.CZK, SampleDeliveryCatalog.bankAccounts(Currency.CZK))
        stored <- store.loadAll
        // ── reload, with empty seeds so everything must come from the store ──
        s2     <- SampleStorage.load(store)
        cat2   <- CatalogSampleWiring.repository(storage = s2)
        catalog <- cat2.loadCatalog
        ruleset <- cat2.loadRuleset
        pl2    <- PricingSampleWiring.pricelistRepository(Map.empty, s2)
        pls    <- pl2.listAll
        ag2    <- PricingSampleWiring.agreementRepository(Nil, s2)
        ags    <- ag2.listAll
        pc2    <- PricingSampleWiring.promoRepository(Nil, s2)
        pcs    <- pc2.listAll
        cs2    <- PricingSampleWiring.costSheetRepository(Map.empty, s2)
        css    <- cs2.listAll
        cu2    <- CustomerSampleWiring.repository(Nil, s2)
        cus    <- cu2.listAll
        dc2    <- OrderingSampleWiring.deliveryCatalog(s2)
        picks  <- dc2.pickupLocations
        cours  <- dc2.allCourierOptions
        banks  <- dc2.allBankAccounts
      yield {
        assert(Set("catalog.catalog", "pricing.pricelists", "customers.customers", "ordering.courierOptions").subsetOf(stored.keySet))
        assert(catalog == SampleCatalog.catalog)
        assert(ruleset == SampleRules.ruleset)
        assert(pls.toSet == PricingSampleWiring.defaultPricelists().values.toSet)
        assert(ags.toSet == SampleAgreements.all.toSet)
        assert(pcs.toSet == SamplePromoCodes.all.toSet)
        assert(css.toSet == Set(SampleCostSheets.costSheetUsd, SampleCostSheets.costSheetCzk))
        assert(seedCu.nonEmpty && cus == seedCu)
        // Re-saving a pickup location moves it last; the stored list keeps that order.
        assert(picks == SampleDeliveryCatalog.pickupLocations.tail :+ SampleDeliveryCatalog.pickupLocations.head)
        assert(cours.toSet == SampleDeliveryCatalog.courierOptionsByCurrency.values.flatten.toSet)
        assert(banks == SampleDeliveryCatalog.bankAccounts)
      }
    }

    "untouched collections are not stored, and reset brings back the sample data" in {
      for
        store   <- DocStore.inMemory
        first   <- boot(store)
        fresh   <- store.loadAll
        order   <- placeGuestOrder(first.storefront, "cart-1", None)
        written <- store.loadAll
        _       <- SampleStorage.reset(store)
        second  <- boot(store)
        orders  <- second.orders.listOrders()
      yield {
        // Booting only stores what it changed (the seeded demo login credentials), never the sample data itself.
        assert(fresh.keySet.subsetOf(Set(StateRef.MetaCollection, "identity.credentials", "identity.sequence")))
        assert(written.get("ordering.orders").exists(_.contains(order.id.value)))
        // The first write of a collection stores it completely (seeds included).
        assert(SampleSeedIds.orders.map(_.value).subsetOf(written("ordering.orders").keySet))
        assert(!written.contains("catalog.catalog"))
        assert(orders.map(_.id).toSet == SampleSeedIds.orders)
      }
    }.atTime(validNow)

    "an unreadable collection falls back to the sample data" in {
      for
        store  <- DocStore.inMemory
        _      <- SampleStorage.reset(store)
        _      <- store.write(
                    Seq(
                      DocOp.Put("ordering.orders", "ord-broken", """{"not":"an order"}"""),
                      DocOp.Put(StateRef.MetaCollection, "ordering.orders", "1"),
                    )
                  )
        rt     <- boot(store)
        orders <- rt.orders.listOrders()
      yield assert(orders.map(_.id).toSet == SampleSeedIds.orders)
    }.atTime(validNow)

    "a different stored format version wipes the store" in {
      for
        store <- DocStore.inMemory
        _     <- store.write(Seq(DocOp.Put(StateRef.MetaCollection, "formatVersion", "0"), DocOp.Put("ordering.orders", "x", "{}")))
        _     <- boot(store)
        after <- store.loadAll
      yield {
        assert(after.get(StateRef.MetaCollection).flatMap(_.get("formatVersion")).contains(SampleStorage.FormatVersion))
        assert(!after.contains("ordering.orders"))
      }
    }
  }

private object SampleSeedIds:
  val orders: Set[OrderId] = Set("0901", "0902", "0903", "0904").map(n => OrderId.unsafe(s"ord-seed-$n"))
