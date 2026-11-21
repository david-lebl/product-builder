package mpbuilder.sample

import mpbuilder.artwork.ArtworkService
import mpbuilder.artwork.sample.ArtworkSampleLayer
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.CatalogSampleLayer
import mpbuilder.customers.CustomerService
import mpbuilder.customers.sample.CustomerSampleLayer
import mpbuilder.identity.IdentityService
import mpbuilder.identity.sample.{IdentitySampleLayer, InMemoryOtpInbox}
import mpbuilder.manufacturing.ManufacturingService
import mpbuilder.manufacturing.sample.ManufacturingSampleLayer
import mpbuilder.ordering.{CartService, CheckoutOptionsService, OrderService}
import mpbuilder.ordering.sample.OrderingSampleLayer
import mpbuilder.pricing.*
import mpbuilder.pricing.sample.PricingSampleLayer
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade
import zio.*

/** Unified backend-less in-memory runtime wiring all 7 bounded-context services,
  * `EventBus`, and the `StorefrontFacade` BFF.
  *
  * Ready to power:
  *   - Full cross-context integration tests
  *   - Standalone UI showcase / interactive product configurator without a database or HTTP backend
  */
object SampleShowcaseRuntime:

  type ShowcaseEnv =
    EventBus &
      CatalogService &
      PricingService &
      CustomerService &
      IdentityService &
      InMemoryOtpInbox &
      ArtworkService &
      CartService &
      CheckoutOptionsService &
      OrderService &
      ManufacturingService &
      StorefrontFacade

  /** Builds a fresh, isolated in-memory runtime with live shop-floor capacity bridged
    * from `ManufacturingService.currentCapacity` into `PricingService`, plus `IdentityService`
    * bridged to `CustomerService` and `StorefrontFacade` coordinating all public ports.
    */
  def layer(
      useSheetCzk: Boolean = true,
      busyPeriods: List[BusyPeriodMultiplier] = Nil,
  ): ULayer[ShowcaseEnv] =
    val busLayer: ULayer[EventBus] = EventBus.live

    val mfgLayer: ZLayer[EventBus, Nothing, ManufacturingService] =
      ManufacturingSampleLayer.live

    val pricingLayer: ZLayer[ManufacturingService & EventBus, Nothing, PricingService] =
      PricingSampleLayer.liveWithCapacityZIO[ManufacturingService](
        fetch = ZIO.serviceWithZIO[ManufacturingService](
          _.currentCapacity.map(DomainBridges.toDynamicPricingContext(_, busyPeriods))
        ),
        useSheetCzk = useSheetCzk,
      )

    val customerAndIdentityLayer: ZLayer[EventBus, Nothing, CustomerService & IdentityService & InMemoryOtpInbox] =
      (CustomerSampleLayer.live() ++ ZLayer.service[EventBus]) >+> IdentitySampleLayer.live()

    val baseServicesLayer =
      ZLayer.service[EventBus] ++
        CatalogSampleLayer.live() ++
        customerAndIdentityLayer ++
        ArtworkSampleLayer.live ++
        OrderingSampleLayer.live ++
        (mfgLayer >+> pricingLayer)

    busLayer >>> (baseServicesLayer >+> StorefrontFacade.layer)

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

  def listShowcaseListings: URIO[CatalogService, List[ShowcaseListing]] =
    ZIO.serviceWithZIO[CatalogService](_.getCatalog.map(_.showcases.values.toList.sortBy(_.sortOrder)))

  def configureAndQuote(
      cmd: ConfigureProductCmd,
      quoteId: QuoteId,
      currency: Currency,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
      customerId: Option[CustomerId] = None,
      customerTier: Option[CustomerTier] = None,
      customerSegment: Option[CustomerSegment] = None,
      promoCode: Option[String] = None,
  ): ZIO[CatalogService & PricingService, ShowcaseError, ConfiguredAndQuoted] =
    for
      cfgRes <- ZIO.serviceWithZIO[CatalogService](_.configure(cmd)).mapError(ShowcaseError.CatalogFailure.apply)
      subject = DomainBridges.toPricingSubject(cfgRes.product, speed)
      quote  <- ZIO
                  .serviceWithZIO[PricingService](
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
                  .mapError(ShowcaseError.PricingFailure.apply)
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
  ): ZIO[CatalogService & PricingService, ShowcaseError, ConfiguredAndQuoted] =
    for
      cat    <- ZIO.serviceWithZIO[CatalogService](_.getCategory(categoryId)).mapError(ShowcaseError.CatalogFailure.apply)
      preset <- ZIO.fromOption(cat.presets.find(_.id == presetId)).orElseFail(ShowcaseError.PresetNotFound(categoryId, presetId))
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
