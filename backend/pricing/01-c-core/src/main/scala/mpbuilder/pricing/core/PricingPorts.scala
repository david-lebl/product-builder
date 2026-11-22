package mpbuilder.pricing
package core

import mpbuilder.shared.*
import zio.*

/** Outbound ports owned by `pricing` — implemented by `02-o-db` and `02-o-acl-*` adapters. */

private[pricing] trait PricelistRepository:
  def findActive(currency: Currency): IO[PricingError.PricelistNotFound, Pricelist]
  def listAll: UIO[List[Pricelist]] =
    ZIO.foreach(Currency.values.toList)(c => findActive(c).option).map(_.flatten)
  def save(pricelist: Pricelist): UIO[Unit] = ZIO.unit

private[pricing] trait CustomerAgreementRepository:
  def findForCustomer(customerId: CustomerId, currency: Currency): UIO[Option[CustomerPriceAgreement]]
  def findForTier(tier: CustomerTier, currency: Currency): UIO[Option[CustomerPriceAgreement]]
  def listAll: UIO[List[CustomerPriceAgreement]]                                = ZIO.succeed(Nil)
  def save(agreement: CustomerPriceAgreement): UIO[Unit]                        = ZIO.unit
  def delete(target: AgreementTarget, currency: Currency): UIO[Unit]            = ZIO.unit

private[pricing] trait PromoCodeRepository:
  def findByCode(code: String): UIO[Option[PromoCode]]
  def findById(id: DiscountCodeId): UIO[Option[PromoCode]] =
    listAll.map(_.find(_.id == id))
  def listAll: UIO[List[PromoCode]]                      = ZIO.succeed(Nil)
  def save(promo: PromoCode): UIO[Unit]                  = ZIO.unit
  def delete(id: DiscountCodeId): UIO[Unit]              = ZIO.unit
  def incrementUsage(codeId: DiscountCodeId): UIO[Unit]

private[pricing] trait CostSheetRepository:
  def findActive(currency: Currency): UIO[Option[ProductionCostSheet]]
  def listAll: UIO[List[ProductionCostSheet]] =
    ZIO.foreach(Currency.values.toList)(findActive).map(_.flatten)
  def save(sheet: ProductionCostSheet): UIO[Unit] = ZIO.unit

/** Outbound ACL port for shop-floor load (implemented by `pricing/02-o-acl-manufacturing`). */
private[pricing] trait ShopCapacityPort:
  def currentPricingContext: UIO[DynamicPricingContext]
