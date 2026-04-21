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
  case TechnologyConstraint(
      predicate: ConfigurationPredicate,
      reason: String,
  )
  // Scoring/creasing crease-count caps (effective cap = min across all applicable rules)
  case ScoringMaxCreasesForCategory(
      categoryId: CategoryId,
      maxCreases: Int,
      reason: String,
  )
  case ScoringMaxCreasesForMaterial(
      materialId: MaterialId,
      maxCreases: Int,
      reason: String,
  )
  case ScoringMaxCreasesForPrintingProcess(
      processType: PrintingProcessType,
      maxCreases: Int,
      reason: String,
  )
  // Binding material's physical spec constrains the product size on the bound edge
  case BindingMaterialConstrainsSize(
      reason: String,
  )
  // Enforces that a Binding component is present when a specific binding method is chosen
  case ComponentRequired(
      role: ComponentRole,
      whenBindingMethod: BindingMethod,
      reason: String,
  )
