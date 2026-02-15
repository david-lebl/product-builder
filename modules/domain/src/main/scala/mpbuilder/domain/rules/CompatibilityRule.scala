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
  case MaterialPropertyFinishTypeIncompatible(
      property: MaterialProperty,
      finishType: FinishType,
      reason: String,
  )
  case MaterialFamilyFinishTypeIncompatible(
      family: MaterialFamily,
      finishType: FinishType,
      reason: String,
  )
  case MaterialWeightFinishType(
      finishType: FinishType,
      minWeightGsm: Int,
      reason: String,
  )
  case FinishTypeMutuallyExclusive(
      finishTypeA: FinishType,
      finishTypeB: FinishType,
      reason: String,
  )
  case FinishCategoryExclusive(
      category: FinishCategory,
      reason: String,
  )
  case FinishRequiresFinishType(
      finishId: FinishId,
      requiredFinishType: FinishType,
      reason: String,
  )
  case FinishRequiresPrintingProcess(
      finishType: FinishType,
      requiredProcessTypes: Set[PrintingProcessType],
      reason: String,
  )
  case ConfigurationConstraint(
      categoryId: CategoryId,
      predicate: ConfigurationPredicate,
      reason: String,
  )
