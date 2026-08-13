package mpbuilder.domain.pricing

import mpbuilder.domain.DimensionsMm
import mpbuilder.domain.ids.*
import zio.json.*

/** Why an order cannot be priced. Errors accumulate — a configuration with
  * several unpriceable parts reports all of them at once.
  */
enum PricingError derives JsonCodec:
  case NoQuantity
  case NoMaterialPrice(materialId: MaterialId)
  case SizeRequiredForPricing(materialId: MaterialId)
  case DoesNotFitOnSheet(materialId: MaterialId, size: DimensionsMm)
  case NoCreaseCountPrice(creases: Int)
  case NoGrommetSpacingPrice(spacingMm: Int)
  case MissingFinishParams(finishId: FinishId)
  case UnknownReference(kind: String, id: String)
