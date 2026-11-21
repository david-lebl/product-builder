package mpbuilder.pricing
package core

import mpbuilder.shared.*
import zio.*

/** Outbound ports owned by `pricing` — implemented by `02-o-db` and `02-o-acl-*` adapters. */

private[pricing] trait PricelistRepository:
  def findActive(currency: Currency): IO[PricingError.PricelistNotFound, Pricelist]

private[pricing] trait CustomerAgreementRepository:
  def findForCustomer(customerId: CustomerId, currency: Currency): UIO[Option[CustomerPriceAgreement]]
  def findForTier(tier: CustomerTier, currency: Currency): UIO[Option[CustomerPriceAgreement]]

private[pricing] trait PromoCodeRepository:
  def findByCode(code: String): UIO[Option[PromoCode]]

private[pricing] trait CostSheetRepository:
  def findActive(currency: Currency): UIO[Option[ProductionCostSheet]]

/** Outbound ACL port for shop-floor load (implemented by `pricing/02-o-acl-manufacturing`). */
private[pricing] trait ShopCapacityPort:
  def currentPricingContext: UIO[DynamicPricingContext]
