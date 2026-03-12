package mpbuilder.domain.pricing

import mpbuilder.domain.model.*
import zio.prelude.*

/** A percentage value in the range 0–100. */
opaque type Percentage = BigDecimal
object Percentage:
  def apply(value: BigDecimal): Validation[String, Percentage] =
    if value >= BigDecimal(0) && value <= BigDecimal(100) then Validation.succeed(value)
    else Validation.fail(s"Percentage must be between 0 and 100, got $value")

  def unsafe(value: BigDecimal): Percentage = value

  val zero: Percentage = BigDecimal(0)

  extension (p: Percentage)
    def value: BigDecimal = p

    /** Apply this percentage as a discount to the given money amount.
      * E.g. 10% applied to 100 returns 90 (the discounted price).
      */
    def applyTo(money: Money): Money =
      money * (BigDecimal(1) - p / BigDecimal(100))

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
