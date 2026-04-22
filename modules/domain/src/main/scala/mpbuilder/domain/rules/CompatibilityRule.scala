package mpbuilder.domain.rules

import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.PartnerId

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
  /** Routes a configuration to an external partner instead of rejecting it.
    * When this rule matches, the configuration is VALID — it is accepted and
    * routed to one of the candidatePartners rather than raising a ConfigurationError.
    */
  case RequiresExternalPartner(
      categoryId: CategoryId,
      predicate: ConfigurationPredicate,
      candidatePartners: Set[PartnerId],
      reason: LocalizedString,
  )
