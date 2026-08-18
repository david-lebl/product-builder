package mpbuilder.domain.manufacturing

import mpbuilder.product.model.CategoryId
import mpbuilder.product.model.*

/** Per-category configuration overrides for manufacturing speed tiers. */
final case class CategoryTierConfig(
    categoryId: CategoryId,
    expressAvailable: Boolean,
    expressMaxQuantity: Option[Int],
    expressMultiplierOverride: Option[BigDecimal],
    economyMultiplierOverride: Option[BigDecimal],
    additionalLeadTimeDays: Int,
)
