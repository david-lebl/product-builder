package mpbuilder.domain.validation

import zio.NonEmptyChunk
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.FinishType.finishCategory
import mpbuilder.domain.rules.*

object RuleEvaluator:

  def evaluate(
      rule: CompatibilityRule,
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      categoryId: CategoryId,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    rule match
      case CompatibilityRule.MaterialFinishIncompatible(matId, finId, reason) =>
        if material.id == matId && finishes.exists(_.id == finId) then
          Validation.fail(ConfigurationError.IncompatibleMaterialFinish(matId, finId, reason))
        else Validation.unit

      case CompatibilityRule.MaterialRequiresFinish(matId, requiredFinishIds, reason) =>
        if material.id == matId && !finishes.exists(f => requiredFinishIds.contains(f.id)) then
          Validation.fail(ConfigurationError.MissingRequiredFinish(matId, requiredFinishIds, reason))
        else Validation.unit

      case CompatibilityRule.FinishRequiresMaterialProperty(finId, requiredProp, reason) =>
        if finishes.exists(_.id == finId) && !material.properties.contains(requiredProp) then
          Validation.fail(ConfigurationError.FinishMissingMaterialProperty(finId, requiredProp, reason))
        else Validation.unit

      case CompatibilityRule.FinishMutuallyExclusive(finIdA, finIdB, reason) =>
        val finishIds = finishes.map(_.id).toSet
        if finishIds.contains(finIdA) && finishIds.contains(finIdB) then
          Validation.fail(ConfigurationError.MutuallyExclusiveFinishes(finIdA, finIdB, reason))
        else Validation.unit

      case CompatibilityRule.SpecConstraint(catId, predicate, reason) =>
        if categoryId == catId then
          evaluateSpecPredicate(predicate, specifications, catId, reason)
        else Validation.unit

      case CompatibilityRule.MaterialPropertyFinishTypeIncompatible(property, finishType, reason) =>
        if material.properties.contains(property) && finishes.exists(_.finishType == finishType) then
          Validation.fail(ConfigurationError.IncompatibleMaterialPropertyFinish(property, finishType, reason))
        else Validation.unit

      case CompatibilityRule.MaterialFamilyFinishTypeIncompatible(family, finishType, reason) =>
        if material.family == family && finishes.exists(_.finishType == finishType) then
          Validation.fail(ConfigurationError.IncompatibleMaterialFamilyFinish(family, finishType, reason))
        else Validation.unit

      case CompatibilityRule.MaterialWeightFinishType(finishType, minWeightGsm, reason) =>
        if finishes.exists(_.finishType == finishType) then
          val actualWeight = material.weight.map(_.gsm)
          actualWeight match
            case Some(w) if w >= minWeightGsm => Validation.unit
            case _ =>
              Validation.fail(ConfigurationError.FinishWeightRequirementNotMet(finishType, minWeightGsm, actualWeight, reason))
        else Validation.unit

      case CompatibilityRule.FinishTypeMutuallyExclusive(ftA, ftB, reason) =>
        val finishTypes = finishes.map(_.finishType).toSet
        if finishTypes.contains(ftA) && finishTypes.contains(ftB) then
          Validation.fail(ConfigurationError.MutuallyExclusiveFinishTypes(ftA, ftB, reason))
        else Validation.unit

      case CompatibilityRule.FinishCategoryExclusive(category, reason) =>
        val categoryFinishes = finishes.filter(_.finishType.finishCategory == category)
        if categoryFinishes.size > 1 then
          Validation.fail(ConfigurationError.FinishCategoryLimitExceeded(category, reason))
        else Validation.unit

      case CompatibilityRule.FinishRequiresFinishType(finId, requiredFinishType, reason) =>
        if finishes.exists(_.id == finId) && !finishes.exists(_.finishType == requiredFinishType) then
          Validation.fail(ConfigurationError.FinishMissingDependentFinishType(finId, requiredFinishType, reason))
        else Validation.unit

      case CompatibilityRule.FinishRequiresPrintingProcess(finishType, requiredProcessTypes, reason) =>
        if finishes.exists(_.finishType == finishType) && !requiredProcessTypes.contains(printingMethod.processType) then
          Validation.fail(ConfigurationError.FinishRequiresPrintingProcessViolation(finishType, requiredProcessTypes, reason))
        else Validation.unit

      case CompatibilityRule.ConfigurationConstraint(catId, predicate, reason) =>
        if categoryId == catId then
          evaluateConfigurationPredicate(predicate, material, finishes, specifications, printingMethod) match
            case false => Validation.fail(ConfigurationError.ConfigurationConstraintViolation(catId, reason))
            case true  => Validation.unit
        else Validation.unit

  private def evaluateSpecPredicate(
      predicate: SpecPredicate,
      specs: ProductSpecifications,
      categoryId: CategoryId,
      reason: String,
  ): Validation[ConfigurationError, Unit] =
    predicate match
      case SpecPredicate.MinDimension(minW, minH) =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            if dim.widthMm < minW || dim.heightMm < minH then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.MaxDimension(maxW, maxH) =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            if dim.widthMm > maxW || dim.heightMm > maxH then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.MinQuantity(min) =>
        specs.get(SpecKind.Quantity) match
          case Some(SpecValue.QuantitySpec(q)) =>
            if q.value < min then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.MaxQuantity(max) =>
        specs.get(SpecKind.Quantity) match
          case Some(SpecValue.QuantitySpec(q)) =>
            if q.value > max then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.AllowedColorModes(modes) =>
        specs.get(SpecKind.ColorMode) match
          case Some(SpecValue.ColorModeSpec(mode)) =>
            if !modes.contains(mode) then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.AllowedBindingMethods(methods) =>
        specs.get(SpecKind.BindingMethod) match
          case Some(SpecValue.BindingMethodSpec(method)) =>
            if !methods.contains(method) then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.AllowedFoldTypes(foldTypes) =>
        specs.get(SpecKind.FoldType) match
          case Some(SpecValue.FoldTypeSpec(ft)) =>
            if !foldTypes.contains(ft) then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.MinPages(min) =>
        specs.get(SpecKind.Pages) match
          case Some(SpecValue.PagesSpec(count)) =>
            if count < min then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

      case SpecPredicate.MaxPages(max) =>
        specs.get(SpecKind.Pages) match
          case Some(SpecValue.PagesSpec(count)) =>
            if count > max then
              Validation.fail(ConfigurationError.SpecConstraintViolation(categoryId, predicate, reason))
            else Validation.unit
          case _ => Validation.unit

  def evaluateConfigurationPredicate(
      predicate: ConfigurationPredicate,
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      printingMethod: PrintingMethod,
  ): Boolean =
    predicate match
      case ConfigurationPredicate.Spec(sp) =>
        // A spec predicate "passes" if it doesn't produce errors
        evaluateSpecPredicate(sp, specifications, CategoryId.unsafe("_"), "").toEither.isRight
      case ConfigurationPredicate.HasMaterialProperty(property) =>
        material.properties.contains(property)
      case ConfigurationPredicate.HasMaterialFamily(family) =>
        material.family == family
      case ConfigurationPredicate.HasPrintingProcess(processType) =>
        printingMethod.processType == processType
      case ConfigurationPredicate.HasMinWeight(minGsm) =>
        material.weight.exists(_.gsm >= minGsm)
      case ConfigurationPredicate.And(left, right) =>
        evaluateConfigurationPredicate(left, material, finishes, specifications, printingMethod) &&
          evaluateConfigurationPredicate(right, material, finishes, specifications, printingMethod)
      case ConfigurationPredicate.Or(left, right) =>
        evaluateConfigurationPredicate(left, material, finishes, specifications, printingMethod) ||
          evaluateConfigurationPredicate(right, material, finishes, specifications, printingMethod)
      case ConfigurationPredicate.Not(inner) =>
        !evaluateConfigurationPredicate(inner, material, finishes, specifications, printingMethod)

  def evaluateAll(
      rules: List[CompatibilityRule],
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      categoryId: CategoryId,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    rules
      .map(rule => evaluate(rule, material, finishes, specifications, categoryId, printingMethod))
      .foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
        acc.zipRight(v),
      )
