package mpbuilder.domain.manufacturing

import mpbuilder.catalog.model.CategoryId
import mpbuilder.catalog.model.*

/** Per-category configuration overrides for manufacturing speed tiers. */
final case class CategoryTierConfig(
    categoryId: CategoryId,
    expressAvailable: Boolean,
    expressMaxQuantity: Option[Int],
    expressMultiplierOverride: Option[BigDecimal],
    economyMultiplierOverride: Option[BigDecimal],
    additionalLeadTimeDays: Int,
)
