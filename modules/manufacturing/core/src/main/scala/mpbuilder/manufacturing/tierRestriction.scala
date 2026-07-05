package mpbuilder.manufacturing

import mpbuilder.catalog.*
import mpbuilder.kernel.*

/** Per-category configuration overrides for manufacturing speed tiers. */
final case class CategoryTierConfig(
    categoryId: CategoryId,
    expressAvailable: Boolean,
    expressMaxQuantity: Option[Int],
    expressMultiplierOverride: Option[BigDecimal],
    economyMultiplierOverride: Option[BigDecimal],
    additionalLeadTimeDays: Int,
)

/** Per-category constraints on manufacturing speed tier availability. */
final case class TierRestriction(
    categoryId: CategoryId,
    tier: ManufacturingSpeed,
    maxQuantity: Option[Int],
    maxComponents: Option[Int],
    maxFinishes: Option[Int],
    allowedFinishTypes: Option[Set[FinishType]],
    blockedMaterials: Option[Set[MaterialId]],
)
