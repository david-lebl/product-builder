package mpbuilder.pricing.sample

import mpbuilder.pricing.*
import mpbuilder.pricing.core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.sample.SampleStorage
import mpbuilder.shared.*
import kyo.*

/** In-memory `PricelistRepository` backed by `Ref`, allowing live pricelist switching
  * (e.g. toggling between unit-based and SRA3 sheet-based CZK pricelist in a UI showcase).
  */
final class InMemoryPricelistRepository(
    ref: StateRef[Map[Currency, Pricelist]],
) extends PricelistRepository:

  override def findActive(currency: Currency): Pricelist < (Async & Abort[PricingError.PricelistNotFound]) =
    ref.get.flatMap(m => m.get(currency).orFail(new PricingError.PricelistNotFound(currency)))

  override def listAll: List[Pricelist] < Async =
    ref.get.map(_.values.toList)

  override def save(pricelist: Pricelist): Unit < Async =
    ref.update(_ + (pricelist.currency -> pricelist))

  def setActive(pricelist: Pricelist): Unit < Async =
    save(pricelist)

final class InMemoryCustomerAgreementRepository(
    ref: StateRef[List[CustomerPriceAgreement]],
) extends CustomerAgreementRepository:

  override def findForCustomer(customerId: CustomerId, currency: Currency): Option[CustomerPriceAgreement] < Async =
    ref.get.map(_.find(a => a.currency == currency && a.target == AgreementTarget.SpecificCustomer(customerId)))

  override def findForTier(tier: CustomerTier, currency: Currency): Option[CustomerPriceAgreement] < Async =
    ref.get.map(_.find(a => a.currency == currency && a.target == AgreementTarget.TierDefault(tier)))

  override def listAll: List[CustomerPriceAgreement] < Async =
    ref.get

  override def save(agreement: CustomerPriceAgreement): Unit < Async =
    ref.update(list => list.filterNot(a => a.target == agreement.target && a.currency == agreement.currency) :+ agreement)

  override def delete(target: AgreementTarget, currency: Currency): Unit < Async =
    ref.update(_.filterNot(a => a.target == target && a.currency == currency))

  def upsert(agreement: CustomerPriceAgreement): Unit < Async =
    save(agreement)

final class InMemoryPromoCodeRepository(
    ref: StateRef[Map[String, PromoCode]],
) extends PromoCodeRepository:

  override def findByCode(code: String): Option[PromoCode] < Async =
    ref.get.map(_.get(code.trim.toUpperCase))

  override def listAll: List[PromoCode] < Async =
    ref.get.map(_.values.toList)

  override def incrementUsage(codeId: DiscountCodeId): Unit < Async =
    ref.update { m =>
      m.map { case (k, promo) =>
        if promo.id == codeId then
          k -> promo.copy(constraints = promo.constraints.copy(currentUses = promo.constraints.currentUses + 1))
        else k -> promo
      }
    }

  override def save(promo: PromoCode): Unit < Async =
    ref.update { m =>
      val withoutSameId = m.filterNot { case (_, existing) => existing.id == promo.id }
      withoutSameId + (promo.code.value.trim.toUpperCase -> promo)
    }

  override def delete(id: DiscountCodeId): Unit < Async =
    ref.update(_.filterNot { case (_, p) => p.id == id })

final class InMemoryCostSheetRepository(
    ref: StateRef[Map[Currency, ProductionCostSheet]],
) extends CostSheetRepository:

  override def findActive(currency: Currency): Option[ProductionCostSheet] < Async =
    ref.get.map(_.get(currency))

  override def listAll: List[ProductionCostSheet] < Async =
    ref.get.map(_.values.toList)

  override def save(sheet: ProductionCostSheet): Unit < Async =
    ref.update(_ + (sheet.currency -> sheet))

final class StaticShopCapacityPort(
    ref: AtomicRef[DynamicPricingContext],
) extends ShopCapacityPort:

  override def currentPricingContext: DynamicPricingContext < Async = ref.get

  def setContext(ctx: DynamicPricingContext): Unit < Async = ref.set(ctx)

/** Adapter turning any `DynamicPricingContext < Async` supplier (e.g. bridged from `ManufacturingService.currentCapacity`)
  * into the `private[pricing]` `ShopCapacityPort`.
  */
final class LiveShopCapacityPort(
    fetch: => DynamicPricingContext < Async,
) extends ShopCapacityPort:
  override def currentPricingContext: DynamicPricingContext < Async = fetch

object PricingSampleWiring:

  def makeLiveCapacityPort(fetch: => DynamicPricingContext < Async): ShopCapacityPort =
    new LiveShopCapacityPort(fetch)

  def defaultPricelists(useSheetCzk: Boolean = true): Map[Currency, Pricelist] =
    Map(
      Currency.USD -> SamplePricelist.pricelistUsd,
      Currency.CZK -> (if useSheetCzk then SamplePricelist.pricelistCzkSheet else SamplePricelist.pricelistCzk),
    )

  def pricelistRepository(
      pricelists: Map[Currency, Pricelist] = defaultPricelists(),
      storage: SampleStorage = SampleStorage.memory,
  ): InMemoryPricelistRepository < Sync =
    storage.map("pricing.pricelists", pricelists)(_.toString, Currency.valueOf).map(new InMemoryPricelistRepository(_))

  def agreementRepository(
      agreements: List[CustomerPriceAgreement] = SampleAgreements.all,
      storage: SampleStorage = SampleStorage.memory,
  ): InMemoryCustomerAgreementRepository < Sync =
    storage.value("pricing.agreements", agreements).map(new InMemoryCustomerAgreementRepository(_))

  def promoRepository(
      promos: List[PromoCode] = SamplePromoCodes.all,
      storage: SampleStorage = SampleStorage.memory,
  ): InMemoryPromoCodeRepository < Sync =
    storage
      .map("pricing.promoCodes", promos.map(p => p.code.value.trim.toUpperCase -> p).toMap)(identity, identity)
      .map(new InMemoryPromoCodeRepository(_))

  def costSheetRepository(
      sheets: Map[Currency, ProductionCostSheet] = Map(
        Currency.USD -> SampleCostSheets.costSheetUsd,
        Currency.CZK -> SampleCostSheets.costSheetCzk,
      ),
      storage: SampleStorage = SampleStorage.memory,
  ): InMemoryCostSheetRepository < Sync =
    storage.map("pricing.costSheets", sheets)(_.toString, Currency.valueOf).map(new InMemoryCostSheetRepository(_))

  def staticCapacity(
      ctx: DynamicPricingContext = DynamicPricingContext.default,
  ): StaticShopCapacityPort < Sync =
    AtomicRef.init(ctx).map(new StaticShopCapacityPort(_))

  /** Self-contained `PricingService` with a static capacity context (ideal for unit tests). */
  def service(
      eventBus: EventBus,
      useSheetCzk: Boolean = true,
      capacityContext: DynamicPricingContext = DynamicPricingContext.default,
  ): PricingService < Sync =
    staticCapacity(capacityContext).map(withCapacity(eventBus, _, useSheetCzk))

  /** `PricingService` wired to any cross-context dynamic capacity effect (e.g.
    * `ManufacturingService.currentCapacity`) without leaking `private[pricing] ShopCapacityPort`.
    */
  def serviceWithLiveCapacity(
      eventBus: EventBus,
      fetch: => DynamicPricingContext < Async,
      useSheetCzk: Boolean = true,
      storage: SampleStorage = SampleStorage.memory,
  ): PricingService < Sync =
    withCapacity(eventBus, new LiveShopCapacityPort(fetch), useSheetCzk, storage)

  private def withCapacity(
      eventBus: EventBus,
      capacity: ShopCapacityPort,
      useSheetCzk: Boolean,
      storage: SampleStorage = SampleStorage.memory,
  ): PricingService < Sync =
    for
      pricelists <- pricelistRepository(defaultPricelists(useSheetCzk), storage)
      agreements <- agreementRepository(storage = storage)
      promos     <- promoRepository(storage = storage)
      costSheets <- costSheetRepository(storage = storage)
    yield PricingServiceLive.make(pricelists, agreements, promos, costSheets, capacity, eventBus)
