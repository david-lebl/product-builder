package mpbuilder.domain.validation

import zio.NonEmptyChunk
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.FinishType.finishCategory
import mpbuilder.domain.rules.*

object RuleEvaluator:

  def evaluate(
      rule: CompatibilityRule,
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      categoryId: CategoryId,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    rule match
      case CompatibilityRule.MaterialFinishIncompatible(matId, finId, reason) =>
        val violations = components.flatMap { comp =>
          if comp.material.id == matId && comp.finishes.exists(_.id == finId) then
            List(ConfigurationError.IncompatibleMaterialFinish(matId, finId, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.MaterialRequiresFinish(matId, requiredFinishIds, reason) =>
        val violations = components.flatMap { comp =>
          if comp.material.id == matId && !comp.finishes.exists(f => requiredFinishIds.contains(f.id)) then
            List(ConfigurationError.MissingRequiredFinish(matId, requiredFinishIds, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.FinishRequiresMaterialProperty(finId, requiredProp, reason) =>
        val violations = components.flatMap { comp =>
          if comp.finishes.exists(_.id == finId) && !comp.material.properties.contains(requiredProp) then
            List(ConfigurationError.FinishMissingMaterialProperty(finId, requiredProp, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.FinishMutuallyExclusive(finIdA, finIdB, reason) =>
        val violations = components.flatMap { comp =>
          val finishIds = comp.finishes.map(_.id).toSet
          if finishIds.contains(finIdA) && finishIds.contains(finIdB) then
            List(ConfigurationError.MutuallyExclusiveFinishes(finIdA, finIdB, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.SpecConstraint(catId, predicate, reason) =>
        if categoryId == catId then
          evaluateSpecPredicate(predicate, specifications, catId, reason)
        else Validation.unit

      case CompatibilityRule.MaterialPropertyFinishTypeIncompatible(property, finishType, reason) =>
        val violations = components.flatMap { comp =>
          if comp.material.properties.contains(property) && comp.finishes.exists(_.finishType == finishType) then
            List(ConfigurationError.IncompatibleMaterialPropertyFinish(property, finishType, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.MaterialFamilyFinishTypeIncompatible(family, finishType, reason) =>
        val violations = components.flatMap { comp =>
          if comp.material.family == family && comp.finishes.exists(_.finishType == finishType) then
            List(ConfigurationError.IncompatibleMaterialFamilyFinish(family, finishType, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.MaterialWeightFinishType(finishType, minWeightGsm, reason) =>
        val violations = components.flatMap { comp =>
          if comp.finishes.exists(_.finishType == finishType) then
            val actualWeight = comp.material.weight.map(_.gsm)
            actualWeight match
              case Some(w) if w >= minWeightGsm => Nil
              case _ =>
                List(ConfigurationError.FinishWeightRequirementNotMet(finishType, minWeightGsm, actualWeight, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.FinishTypeMutuallyExclusive(ftA, ftB, reason) =>
        val violations = components.flatMap { comp =>
          val finishTypes = comp.finishes.map(_.finishType).toSet
          if finishTypes.contains(ftA) && finishTypes.contains(ftB) then
            List(ConfigurationError.MutuallyExclusiveFinishTypes(ftA, ftB, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.FinishCategoryExclusive(category, reason) =>
        val violations = components.flatMap { comp =>
          val categoryFinishes = comp.finishes.filter(_.finishType.finishCategory == category)
          if categoryFinishes.size > 1 then
            List(ConfigurationError.FinishCategoryLimitExceeded(category, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.FinishRequiresFinishType(finId, requiredFinishType, reason) =>
        val violations = components.flatMap { comp =>
          if comp.finishes.exists(_.id == finId) && !comp.finishes.exists(_.finishType == requiredFinishType) then
            List(ConfigurationError.FinishMissingDependentFinishType(finId, requiredFinishType, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.FinishRequiresPrintingProcess(finishType, requiredProcessTypes, reason) =>
        val violations = components.flatMap { comp =>
          if comp.finishes.exists(_.finishType == finishType) && !requiredProcessTypes.contains(printingMethod.processType) then
            List(ConfigurationError.FinishRequiresPrintingProcessViolation(finishType, requiredProcessTypes, reason))
          else Nil
        }
        violations.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )

      case CompatibilityRule.ConfigurationConstraint(catId, predicate, reason) =>
        if categoryId == catId then
          evaluateConfigurationPredicate(predicate, components, specifications, printingMethod) match
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
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      printingMethod: PrintingMethod,
  ): Boolean =
    predicate match
      case ConfigurationPredicate.Spec(sp) =>
        evaluateSpecPredicate(sp, specifications, CategoryId.unsafe("_"), "").toEither.isRight
      case ConfigurationPredicate.HasMaterialProperty(property) =>
        components.exists(_.material.properties.contains(property))
      case ConfigurationPredicate.HasMaterialFamily(family) =>
        components.exists(_.material.family == family)
      case ConfigurationPredicate.HasPrintingProcess(processType) =>
        printingMethod.processType == processType
      case ConfigurationPredicate.HasMinWeight(minGsm) =>
        components.exists(_.material.weight.exists(_.gsm >= minGsm))
      case ConfigurationPredicate.AllowedInkTypes(inkTypes) =>
        components.forall { comp =>
          val frontOk = comp.inkConfiguration.front.inkType == InkType.None || inkTypes.contains(comp.inkConfiguration.front.inkType)
          val backOk = comp.inkConfiguration.back.inkType == InkType.None || inkTypes.contains(comp.inkConfiguration.back.inkType)
          frontOk && backOk
        }
      case ConfigurationPredicate.MaxColorCountPerSide(max) =>
        components.forall { comp =>
          comp.inkConfiguration.front.colorCount <= max && comp.inkConfiguration.back.colorCount <= max
        }
      case ConfigurationPredicate.And(left, right) =>
        evaluateConfigurationPredicate(left, components, specifications, printingMethod) &&
          evaluateConfigurationPredicate(right, components, specifications, printingMethod)
      case ConfigurationPredicate.Or(left, right) =>
        evaluateConfigurationPredicate(left, components, specifications, printingMethod) ||
          evaluateConfigurationPredicate(right, components, specifications, printingMethod)
      case ConfigurationPredicate.Not(inner) =>
        !evaluateConfigurationPredicate(inner, components, specifications, printingMethod)

  def evaluateAll(
      rules: List[CompatibilityRule],
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      categoryId: CategoryId,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    rules
      .map(rule => evaluate(rule, components, specifications, categoryId, printingMethod))
      .foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
        acc.zipRight(v),
      )
