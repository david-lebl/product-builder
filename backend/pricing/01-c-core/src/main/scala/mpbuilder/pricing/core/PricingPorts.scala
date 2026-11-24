package mpbuilder.pricing
package core

import mpbuilder.shared.*
import kyo.*

/** Outbound ports owned by `pricing` — implemented by `02-o-db` and `02-o-acl-*` adapters. */

private[pricing] trait PricelistRepository:
  def findActive(currency: Currency): Pricelist < (Async & Abort[PricingError.PricelistNotFound])
  def listAll: List[Pricelist] < Async =
    Effects.traverse(Currency.values.toList)(c => findActive(c).orNone).map(_.flatten)
  def save(pricelist: Pricelist): Unit < Async = Kyo.unit

private[pricing] trait CustomerAgreementRepository:
  def findForCustomer(customerId: CustomerId, currency: Currency): Option[CustomerPriceAgreement] < Async
  def findForTier(tier: CustomerTier, currency: Currency): Option[CustomerPriceAgreement] < Async
  def listAll: List[CustomerPriceAgreement] < Async                                = Kyo.lift(Nil)
  def save(agreement: CustomerPriceAgreement): Unit < Async                        = Kyo.unit
  def delete(target: AgreementTarget, currency: Currency): Unit < Async            = Kyo.unit

private[pricing] trait PromoCodeRepository:
  def findByCode(code: String): Option[PromoCode] < Async
  def findById(id: DiscountCodeId): Option[PromoCode] < Async =
    listAll.map(_.find(_.id == id))
  def listAll: List[PromoCode] < Async                      = Kyo.lift(Nil)
  def save(promo: PromoCode): Unit < Async                  = Kyo.unit
  def delete(id: DiscountCodeId): Unit < Async              = Kyo.unit
  def incrementUsage(codeId: DiscountCodeId): Unit < Async

private[pricing] trait CostSheetRepository:
  def findActive(currency: Currency): Option[ProductionCostSheet] < Async
  def listAll: List[ProductionCostSheet] < Async =
    Effects.traverse(Currency.values.toList)(findActive).map(_.flatten)
  def save(sheet: ProductionCostSheet): Unit < Async = Kyo.unit

/** Outbound ACL port for shop-floor load (implemented by `pricing/02-o-acl-manufacturing`). */
private[pricing] trait ShopCapacityPort:
  def currentPricingContext: DynamicPricingContext < Async
