package mpbuilder.domain.validation

import zio.NonEmptyChunk
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*

object RuleEvaluator:

  def evaluate(
      rule: CompatibilityRule,
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      categoryId: CategoryId,
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

  def evaluateAll(
      rules: List[CompatibilityRule],
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      categoryId: CategoryId,
  ): Validation[ConfigurationError, Unit] =
    rules
      .map(rule => evaluate(rule, material, finishes, specifications, categoryId))
      .foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
        acc.zipRight(v),
      )
