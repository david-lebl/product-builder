package mpbuilder.catalog

import mpbuilder.kernel.*

enum SpecPredicate:
  case MinDimension(minWidthMm: Double, minHeightMm: Double)
  case MaxDimension(maxWidthMm: Double, maxHeightMm: Double)
  case MinQuantity(min: Int)
  case MaxQuantity(max: Int)
  case AllowedBindingMethods(methods: Set[BindingMethod])
  case AllowedFoldTypes(foldTypes: Set[FoldType])
  case MinPages(min: Int)
  case MaxPages(max: Int)
  case PagesDivisibleBy(n: Int)
  case SquareDimension()
  case AllowedDimensions(sizes: Set[(Double, Double)])

enum ConfigurationPredicate:
  case Spec(predicate: SpecPredicate)
  case HasMaterialProperty(property: MaterialProperty)
  case HasMaterialFamily(family: MaterialFamily)
  case HasPrintingProcess(processType: PrintingProcessType)
  case HasMinWeight(minGsm: Int)
  case AllowedInkTypes(inkTypes: Set[InkType])
  case MaxColorCountPerSide(max: Int)
  case BindingMethodIs(methods: Set[BindingMethod])
  case HasInkType(inkType: InkType)
  case HasFinishId(finishId: FinishId)
  /** True when every component's ink configuration is single-sided (back side is None or White). */
  case IsSingleSided
  case And(left: ConfigurationPredicate, right: ConfigurationPredicate)
  case Or(left: ConfigurationPredicate, right: ConfigurationPredicate)
  case Not(inner: ConfigurationPredicate)


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


final case class CompatibilityRuleset(
    rules: List[CompatibilityRule],
    version: String,
)
