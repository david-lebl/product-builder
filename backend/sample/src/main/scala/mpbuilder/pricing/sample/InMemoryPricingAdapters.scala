package mpbuilder.pricing.sample

import mpbuilder.pricing.*
import mpbuilder.pricing.core.*
import mpbuilder.shared.*
import zio.*

/** In-memory `PricelistRepository` backed by `Ref`, allowing live pricelist switching
  * (e.g. toggling between unit-based and SRA3 sheet-based CZK pricelist in a UI showcase).
  */
final class InMemoryPricelistRepository(
    ref: Ref[Map[Currency, Pricelist]],
) extends PricelistRepository:

  override def findActive(currency: Currency): IO[PricingError.PricelistNotFound, Pricelist] =
    ref.get.flatMap(m => ZIO.fromOption(m.get(currency)).orElseFail(new PricingError.PricelistNotFound(currency)))

  def setActive(pricelist: Pricelist): UIO[Unit] =
    ref.update(_ + (pricelist.currency -> pricelist))

final class InMemoryCustomerAgreementRepository(
    ref: Ref[List[CustomerPriceAgreement]],
) extends CustomerAgreementRepository:

  override def findForCustomer(customerId: CustomerId, currency: Currency): UIO[Option[CustomerPriceAgreement]] =
    ref.get.map(_.find(a => a.currency == currency && a.target == AgreementTarget.SpecificCustomer(customerId)))

  override def findForTier(tier: CustomerTier, currency: Currency): UIO[Option[CustomerPriceAgreement]] =
    ref.get.map(_.find(a => a.currency == currency && a.target == AgreementTarget.TierDefault(tier)))

  def upsert(agreement: CustomerPriceAgreement): UIO[Unit] =
    ref.update(list => list.filterNot(a => a.target == agreement.target && a.currency == agreement.currency) :+ agreement)

final class InMemoryPromoCodeRepository(
    ref: Ref[Map[String, PromoCode]],
) extends PromoCodeRepository:

  override def findByCode(code: String): UIO[Option[PromoCode]] =
    ref.get.map(_.get(code.trim.toUpperCase))

  def save(promo: PromoCode): UIO[Unit] =
    ref.update(_ + (promo.code.value.trim.toUpperCase -> promo))

final class InMemoryCostSheetRepository(
    ref: Ref[Map[Currency, ProductionCostSheet]],
) extends CostSheetRepository:

  override def findActive(currency: Currency): UIO[Option[ProductionCostSheet]] =
    ref.get.map(_.get(currency))

final class StaticShopCapacityPort(
    ref: Ref[DynamicPricingContext],
) extends ShopCapacityPort:

  override def currentPricingContext: UIO[DynamicPricingContext] = ref.get

  def setContext(ctx: DynamicPricingContext): UIO[Unit] = ref.set(ctx)

/** Adapter turning any `UIO[DynamicPricingContext]` supplier (e.g. bridged from `ManufacturingService.currentCapacity`)
  * into the `private[pricing]` `ShopCapacityPort`.
  */
final class LiveShopCapacityPort(
    fetch: UIO[DynamicPricingContext],
) extends ShopCapacityPort:
  override def currentPricingContext: UIO[DynamicPricingContext] = fetch

object PricingSampleLayer:

  def makeLiveCapacityPort(fetch: UIO[DynamicPricingContext]): ShopCapacityPort =
    new LiveShopCapacityPort(fetch)

  def defaultPricelists(useSheetCzk: Boolean = true): Map[Currency, Pricelist] =
    Map(
      Currency.USD -> SamplePricelist.pricelistUsd,
      Currency.CZK -> (if useSheetCzk then SamplePricelist.pricelistCzkSheet else SamplePricelist.pricelistCzk),
    )

  def pricelistRepoLayer(
      pricelists: Map[Currency, Pricelist] = defaultPricelists(),
  ): ULayer[InMemoryPricelistRepository] =
    ZLayer.fromZIO(Ref.make(pricelists).map(new InMemoryPricelistRepository(_)))

  def agreementRepoLayer(
      agreements: List[CustomerPriceAgreement] = SampleAgreements.all,
  ): ULayer[InMemoryCustomerAgreementRepository] =
    ZLayer.fromZIO(Ref.make(agreements).map(new InMemoryCustomerAgreementRepository(_)))

  def promoRepoLayer(
      promos: List[PromoCode] = SamplePromoCodes.all,
  ): ULayer[InMemoryPromoCodeRepository] =
    ZLayer.fromZIO(
      Ref.make(promos.map(p => p.code.value.trim.toUpperCase -> p).toMap).map(new InMemoryPromoCodeRepository(_))
    )

  def costSheetRepoLayer(
      sheets: Map[Currency, ProductionCostSheet] = Map(
        Currency.USD -> SampleCostSheets.costSheetUsd,
        Currency.CZK -> SampleCostSheets.costSheetCzk,
      ),
  ): ULayer[InMemoryCostSheetRepository] =
    ZLayer.fromZIO(Ref.make(sheets).map(new InMemoryCostSheetRepository(_)))

  def staticCapacityLayer(
      ctx: DynamicPricingContext = DynamicPricingContext.default,
  ): ULayer[StaticShopCapacityPort] =
    ZLayer.fromZIO(Ref.make(ctx).map(new StaticShopCapacityPort(_)))

  /** Self-contained `PricingService` layer with static capacity context (ideal for unit tests). */
  def live(
      useSheetCzk: Boolean = true,
      capacityContext: DynamicPricingContext = DynamicPricingContext.default,
  ): ZLayer[EventBus, Nothing, PricingService] =
    (pricelistRepoLayer(defaultPricelists(useSheetCzk)) ++
      agreementRepoLayer() ++
      promoRepoLayer() ++
      costSheetRepoLayer() ++
      staticCapacityLayer(capacityContext) ++
      ZLayer.service[EventBus]) >>> PricingServiceLive.layer

  /** `PricingService` layer wired to any cross-context dynamic capacity effect (e.g.
    * `ManufacturingService.currentCapacity`) without leaking `private[pricing] ShopCapacityPort`.
    */
  def liveWithCapacityZIO[R: Tag](
      fetch: URIO[R, DynamicPricingContext],
      useSheetCzk: Boolean = true,
  ): ZLayer[R & EventBus, Nothing, PricingService] =
    val capLayer: ZLayer[R, Nothing, ShopCapacityPort] =
      ZLayer.fromZIO(ZIO.environment[R].map(env => new LiveShopCapacityPort(fetch.provideEnvironment(env))))
    (pricelistRepoLayer(defaultPricelists(useSheetCzk)) ++
      agreementRepoLayer() ++
      promoRepoLayer() ++
      costSheetRepoLayer() ++
      capLayer ++
      ZLayer.service[EventBus]) >>> PricingServiceLive.layer
