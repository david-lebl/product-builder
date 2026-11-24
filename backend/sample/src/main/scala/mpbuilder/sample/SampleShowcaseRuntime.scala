package mpbuilder.sample

import mpbuilder.artwork.ArtworkService
import mpbuilder.artwork.sample.ArtworkSampleWiring
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.CatalogSampleWiring
import mpbuilder.customers.CustomerService
import mpbuilder.customers.sample.CustomerSampleWiring
import mpbuilder.identity.IdentityService
import mpbuilder.identity.sample.{IdentitySampleWiring, InMemoryOtpInbox}
import mpbuilder.manufacturing.ManufacturingService
import mpbuilder.manufacturing.sample.ManufacturingSampleWiring
import mpbuilder.ordering.{CartService, CheckoutOptionsService, OrderService}
import mpbuilder.ordering.sample.OrderingSampleWiring
import mpbuilder.pricing.*
import mpbuilder.pricing.sample.PricingSampleWiring
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade
import kyo.*

/** Every service of one fully wired, in-memory showcase runtime (all 7 bounded contexts + both BFF facades). */
final case class ShowcaseRuntime(
    eventBus: EventBus,
    catalog: CatalogService,
    pricing: PricingService,
    customers: CustomerService,
    identity: IdentityService,
    otpInbox: InMemoryOtpInbox,
    artwork: ArtworkService,
    carts: CartService,
    checkoutOptions: CheckoutOptionsService,
    orders: OrderService,
    manufacturing: ManufacturingService,
    blobs: BlobStorage,
    idSequence: IdSequence,
    storefront: StorefrontFacade,
    backoffice: BackofficeFacade,
)

/** Unified backend-less in-memory runtime wiring all 7 bounded-context services,
  * `EventBus`, `StorefrontFacade`, and `BackofficeFacade`.
  *
  * Ready to power:
  *   - Full cross-context integration tests
  *   - Standalone UI showcase & Staff Backoffice without a database or HTTP backend
  */
object SampleShowcaseRuntime:

  /** Builds a fresh, isolated in-memory runtime with live shop-floor capacity bridged
    * from `ManufacturingService.currentCapacity` into `PricingService`, plus `IdentityService`
    * bridged to `CustomerService`, `StorefrontFacade`, and `BackofficeFacade` sharing the
    * exact same in-memory state.
    */
  def make(
      useSheetCzk: Boolean = true,
      busyPeriods: List[BusyPeriodMultiplier] = Nil,
      seedSampleOrders: Boolean = true,
      blobStorage: => BlobStorage < Sync = BlobStorage.inMemory(),
  ): ShowcaseRuntime < Async =
    for
      bus              <- EventBus.init
      blobs            <- blobStorage
      ids              <- IdSequence.inMemory()
      catalog          <- CatalogSampleWiring.service()
      customers        <- CustomerSampleWiring.service(bus)
      (identity, inbox) <- IdentitySampleWiring.service(customers, bus)
      artwork          <- ArtworkSampleWiring.service(bus, seedSampleArtworks = seedSampleOrders)
      ordering         <- OrderingSampleWiring.services(bus, seedSampleOrders = seedSampleOrders)
      manufacturing    <- ManufacturingSampleWiring.service(bus)
      pricing          <- PricingSampleWiring.serviceWithLiveCapacity(
                            bus,
                            manufacturing.currentCapacity.map(DomainBridges.toDynamicPricingContext(_, busyPeriods)),
                            useSheetCzk = useSheetCzk,
                          )
      storefront        = StorefrontFacade.make(
                            catalog, pricing, customers, identity, artwork, ordering.carts, ordering.checkoutOptions,
                            ordering.orders, manufacturing, ids, blobs,
                          )
      backoffice        = BackofficeFacade.make(
                            catalog, pricing, customers, identity, ordering.orders, ordering.checkoutOptions, artwork, blobs,
                          )
    yield ShowcaseRuntime(
      bus, catalog, pricing, customers, identity, inbox, artwork, ordering.carts, ordering.checkoutOptions, ordering.orders,
      manufacturing, blobs, ids, storefront, backoffice,
    )

/** Error union returned by high-level showcase orchestration operations. */
enum ShowcaseError(val message: String):
  case CatalogFailure(error: CatalogError) extends ShowcaseError(error.message)
  case PricingFailure(error: PricingError) extends ShowcaseError(error.message)
  case PresetNotFound(categoryId: CategoryId, presetId: PresetId)
      extends ShowcaseError(s"Preset '${presetId.value}' not found in category '${categoryId.value}'")

final case class ConfiguredAndQuoted(
    configuration: ConfigurationResult,
    quote: PriceQuote,
)

/** High-level showcase facade combining `CatalogService` + `PricingService` via `DomainBridges`. */
object ShowcaseFacade:

  def listShowcaseListings: List[ShowcaseListing] < (Async & Env[CatalogService]) =
    Env.use[CatalogService](_.getCatalog.map(_.showcases.values.toList.sortBy(_.sortOrder)))

  def configureAndQuote(
      cmd: ConfigureProductCmd,
      quoteId: QuoteId,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      customerId: Option[CustomerId] = None,
      customerTier: Option[CustomerTier] = None,
      customerSegment: Option[CustomerSegment] = None,
      promoCode: Option[String] = None,
  ): ConfiguredAndQuoted < (Async & Abort[ShowcaseError] & Env[CatalogService & PricingService]) =
    for
      cfgRes <- Env.use[CatalogService](_.configure(cmd)).mapAbort(ShowcaseError.CatalogFailure.apply)
      subject = DomainBridges.toPricingSubject(cfgRes.product, speed)
      quote  <- Env.use[PricingService](
                    _.quote(
                      PriceQuoteCmd(
                        quoteId = quoteId,
                        subject = subject,
                        currency = currency,
                        customerId = customerId,
                        customerTier = customerTier,
                        customerSegment = customerSegment,
                        promoCode = promoCode,
                      )
                    )
                  )
                  .mapAbort(ShowcaseError.PricingFailure.apply)
    yield ConfiguredAndQuoted(cfgRes, quote)

  def pricePreset(
      categoryId: CategoryId,
      presetId: PresetId,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      specsOverride: Option[ProductSpecs] = None,
      customerId: Option[CustomerId] = None,
      customerTier: Option[CustomerTier] = None,
      customerSegment: Option[CustomerSegment] = None,
      promoCode: Option[String] = None,
  ): ConfiguredAndQuoted < (Async & Abort[ShowcaseError] & Env[CatalogService & PricingService]) =
    for
      cat    <- Env.use[CatalogService](_.getCategory(categoryId)).mapAbort(ShowcaseError.CatalogFailure.apply)
      preset <- (cat.presets.find(_.id == presetId)).orFail(ShowcaseError.PresetNotFound(categoryId, presetId))
      cmd     = DomainBridges.presetToConfigureCmd(
                  configId = ConfigurationId.unsafe(s"cfg-${presetId.value}"),
                  categoryId = categoryId,
                  preset = preset,
                  specsOverride = specsOverride,
                )
      res    <- configureAndQuote(
                  cmd = cmd,
                  quoteId = QuoteId.unsafe(s"q-${presetId.value}"),
                  currency = currency,
                  speed = speed,
                  customerId = customerId,
                  customerTier = customerTier,
                  customerSegment = customerSegment,
                  promoCode = promoCode,
                )
    yield res
