package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.TierRestriction

/** Sample tier restrictions for Express manufacturing. */
object SampleTierRestrictions:

  val restrictions: List[TierRestriction] = List(
    // Business cards: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-business-cards"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Flyers: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-flyers"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Brochures: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-brochures"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Booklets: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-booklets"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Postcards: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-postcards"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Stickers: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-stickers"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Calendars: Express max 2000
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-calendars"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(2000),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Banners / large format: Express max 500
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-banners"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(500),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Roll-ups: Express max 500
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-roll-ups"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(500),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
    // Packaging: Express max 500
    TierRestriction(
      categoryId = CategoryId.unsafe("cat-packaging"),
      tier = ManufacturingSpeed.Express,
      maxQuantity = Some(500),
      maxComponents = None,
      maxFinishes = None,
      allowedFinishTypes = None,
      blockedMaterials = None,
    ),
  )
