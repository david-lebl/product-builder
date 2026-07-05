package mpbuilder.domain.pricing

import mpbuilder.catalog.*

import mpbuilder.domain.model.*
import mpbuilder.kernel.*

/** Customer-specific pricing overlay on top of a base Pricelist.
  *
  * Discount resolution precedence (most specific wins):
  *   1. Fixed price on material → replaces base material price entirely
  *   2. Material-level percentage → applied to base material price
  *   3. Category-level percentage → applied to material prices for that category
  *   4. Global percentage → applied to remaining undiscounted components
  */
final case class CustomerPricing(
    globalDiscount: Option[Percentage] = None,
    categoryDiscounts: Map[CategoryId, Percentage] = Map.empty,
    materialDiscounts: Map[MaterialId, Percentage] = Map.empty,
    fixedMaterialPrices: Map[MaterialId, Price] = Map.empty,
    finishDiscounts: Map[FinishId, Percentage] = Map.empty,
    customQuantityTiers: Option[List[PricingRule.QuantityTier]] = None,
    customSheetQuantityTiers: Option[List[PricingRule.SheetQuantityTier]] = None,
    minimumOrderOverride: Option[Money] = None,
)

object CustomerPricing:
  val empty: CustomerPricing = CustomerPricing()

/** Sealed ADT representation of individual customer pricing rules.
  * Useful for serialization and UI editing of customer pricing configurations.
  */
enum CustomerPricingRule:
  case GlobalPercentageDiscount(percentage: Percentage)
  case CategoryPercentageDiscount(categoryId: CategoryId, percentage: Percentage)
  case MaterialPercentageDiscount(materialId: MaterialId, percentage: Percentage)
  case MaterialFixedPrice(materialId: MaterialId, price: Price)
  case FinishPercentageDiscount(finishId: FinishId, percentage: Percentage)
