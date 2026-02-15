package mpbuilder.domain.rules

import mpbuilder.domain.model.*

enum CompatibilityRule:
  case MaterialFinishIncompatible(
      materialId: MaterialId,
      finishId: FinishId,
      reason: String,
  )
  case MaterialRequiresFinish(
      materialId: MaterialId,
      requiredFinishIds: Set[FinishId],
      reason: String,
  )
  case FinishRequiresMaterialProperty(
      finishId: FinishId,
      requiredProperty: MaterialProperty,
      reason: String,
  )
  case FinishMutuallyExclusive(
      finishIdA: FinishId,
      finishIdB: FinishId,
      reason: String,
  )
  case SpecConstraint(
      categoryId: CategoryId,
      predicate: SpecPredicate,
      reason: String,
  )
