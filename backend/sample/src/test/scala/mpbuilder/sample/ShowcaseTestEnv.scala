package mpbuilder.sample

import kyo.*
import mpbuilder.artwork.ArtworkService
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.catalog.CatalogService
import mpbuilder.catalog.sample.CatalogSampleWiring
import mpbuilder.customers.CustomerService
import mpbuilder.identity.IdentityService
import mpbuilder.identity.sample.InMemoryOtpInbox
import mpbuilder.manufacturing.ManufacturingService
import mpbuilder.ordering.{CartService, CheckoutOptionsService, OrderService}
import mpbuilder.pricing.{DynamicPricingContext, PricingService}
import mpbuilder.pricing.sample.PricingSampleWiring
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade

/** Provides showcase services to specs through Kyo's `Env` effect (`Env.get[StorefrontFacade]`, …). */
object ShowcaseTestEnv:

  type Services =
    EventBus & CatalogService & PricingService & CustomerService & IdentityService & InMemoryOtpInbox & ArtworkService &
      CartService & CheckoutOptionsService & OrderService & ManufacturingService & BlobStorage & IdSequence &
      StorefrontFacade & BackofficeFacade

  def services(rt: ShowcaseRuntime): TypeMap[Services] =
    TypeMap(rt.eventBus, rt.catalog, rt.pricing, rt.customers)
      .add(rt.identity)
      .add(rt.otpInbox)
      .add(rt.artwork)
      .add(rt.carts)
      .add(rt.checkoutOptions)
      .add(rt.orders)
      .add(rt.manufacturing)
      .add(rt.blobs)
      .add(rt.idSequence)
      .add(rt.storefront)
      .add(rt.backoffice)

  def catalogAndPricing(
      useSheetCzk: Boolean = true,
      capacityContext: DynamicPricingContext = DynamicPricingContext.default,
  ): TypeMap[CatalogService & PricingService] < Sync =
    for
      bus     <- EventBus.init
      pricing <- PricingSampleWiring.service(bus, useSheetCzk, capacityContext)
      catalog <- CatalogSampleWiring.service()
    yield TypeMap(catalog, pricing)

  extension [A, S](v: A < S)
    /** Runs `v` under a controlled clock pinned to `instant` (replaces ZIO's `TestClock.setTime`). */
    def atTime(instant: java.time.Instant)(using Frame): A < (S & Async) =
      Clock.withTimeControl(tc => tc.set(kyo.Instant.fromJava(instant)).andThen(v))

  extension [A, S](v: A < (Env[Services] & S))
    /** Runs the spec body against a fresh in-memory [[SampleShowcaseRuntime]]. */
    def provideRuntime(
        useSheetCzk: Boolean = true,
        seedSampleOrders: Boolean = true,
    )(using Frame): A < (S & Async) =
      SampleShowcaseRuntime.make(useSheetCzk = useSheetCzk, seedSampleOrders = seedSampleOrders).map(rt => Env.runAll(services(rt))(v))

  extension [A, S](v: A < (Env[CatalogService & PricingService] & S))
    def provideCatalogAndPricing(env: TypeMap[CatalogService & PricingService] < Sync)(using Frame): A < (S & Sync) =
      env.map(e => Env.runAll(e)(v))

  extension [A, S](v: A < (Env[CatalogService] & S))
    def provideCatalog(using Frame): A < (S & Sync) =
      CatalogSampleWiring.service().map(c => Env.run(c)(v))
