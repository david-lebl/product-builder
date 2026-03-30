package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.*

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
