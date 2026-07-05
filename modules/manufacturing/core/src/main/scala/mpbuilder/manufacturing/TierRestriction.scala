package mpbuilder.manufacturing

import mpbuilder.catalog.*

import mpbuilder.kernel.*


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
