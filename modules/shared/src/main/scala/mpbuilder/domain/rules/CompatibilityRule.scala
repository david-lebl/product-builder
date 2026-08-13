package mpbuilder.domain.rules

import mpbuilder.domain.DimensionsMm
import mpbuilder.domain.catalog.FinishType
import mpbuilder.domain.ids.*
import zio.json.*

/** Cross-cutting compatibility rules (spec §4). Category→material/finish/method
  * allow-lists and printing-method color limits are structural catalog data and
  * validated directly; these rules carry everything that cuts across it.
  * They ship to the client with the catalog so UI filtering and validation run
  * on identical data.
  */
enum CompatibilityRule derives JsonCodec:
  /** At most one finish of this type per component (e.g. one lamination). */
  case OnePerFinishType(finishType: FinishType)

  /** These two finish types can never be combined on one component. */
  case MutuallyExclusiveFinishTypes(a: FinishType, b: FinishType)

  /** These two specific finishes can never be combined on one component. */
  case MutuallyExclusiveFinishes(a: FinishId, b: FinishId)

  /** A finish of this type needs a material of at least this weight. */
  case FinishTypeMinWeight(finishType: FinishType, minGsm: Int)

  /** Order quantity limits for a category. */
  case QuantityLimit(categoryId: CategoryId, min: Int, max: Int)

  /** Allowed size envelope for a category. */
  case SizeLimit(categoryId: CategoryId, min: DimensionsMm, max: DimensionsMm)

  /** Cap on the number of creases; optional category/material scoping. */
  case MaxCreases(categoryId: Option[CategoryId], materialId: Option[MaterialId], max: Int)
