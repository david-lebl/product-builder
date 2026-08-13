package mpbuilder.domain.validation

import mpbuilder.domain.DimensionsMm
import mpbuilder.domain.catalog.{ComponentRole, FinishType, RequiredDetail}
import mpbuilder.domain.ids.*
import zio.json.*

/** Everything that can be wrong with a configuration. Validation accumulates —
  * the customer sees every problem at once, never one at a time (spec §4).
  */
enum ValidationError derives JsonCodec:
  case UnknownReference(kind: String, id: String)
  case MissingDetail(detail: RequiredDetail)
  case InvalidQuantity(actual: Int)
  case MissingRequiredComponent(role: ComponentRole)
  case UnexpectedComponent(role: ComponentRole)
  case MaterialNotAllowedInCategory(role: ComponentRole, materialId: MaterialId)
  case FinishNotAllowedInCategory(role: ComponentRole, finishId: FinishId)
  case PrintingMethodNotAllowed(methodId: PrintingMethodId)
  case TooManyInkColors(methodId: PrintingMethodId, maxColors: Int, requested: Int)
  case OnePerFinishTypeExceeded(role: ComponentRole, finishType: FinishType)
  case MutuallyExclusiveFinishTypes(role: ComponentRole, a: FinishType, b: FinishType)
  case MutuallyExclusiveFinishes(role: ComponentRole, a: FinishId, b: FinishId)
  case FinishRequiresMinWeight(role: ComponentRole, finishType: FinishType, minGsm: Int, actualGsm: Int)
  case QuantityOutOfRange(min: Int, max: Int, actual: Int)
  case SizeOutOfRange(min: DimensionsMm, max: DimensionsMm, actual: DimensionsMm)
  case TooManyCreases(max: Int, actual: Int)
  case MissingFinishParams(role: ComponentRole, finishId: FinishId)
  case ContactInvalid(field: String, message: String)
