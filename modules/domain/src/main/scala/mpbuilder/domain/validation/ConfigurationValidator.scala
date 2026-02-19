package mpbuilder.domain.validation

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*

object ConfigurationValidator:

  def validate(
      category: ProductCategory,
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      ruleset: CompatibilityRuleset,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    val structural = validateStructural(category, material, finishes, specifications, printingMethod)
    structural.flatMap(_ =>
      RuleEvaluator.evaluateAll(
        ruleset.rules,
        material,
        finishes,
        specifications,
        category.id,
        printingMethod,
      ),
    )

  def validateMultiComponent(
      category: ProductCategory,
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      ruleset: CompatibilityRuleset,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    val structural = validateMultiComponentStructural(category, components, specifications, printingMethod)
    structural.flatMap { _ =>
      // Run rule evaluation per component
      val componentRuleChecks = components.map { comp =>
        RuleEvaluator.evaluateAll(
          ruleset.rules,
          comp.material,
          comp.finishes,
          specifications,
          category.id,
          printingMethod,
        )
      }
      componentRuleChecks.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
        acc.zipRight(v),
      )
    }

  private def validateMultiComponentStructural(
      category: ProductCategory,
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    // Check all required roles are present
    val roleChecks = category.componentRoles.toList.map { role =>
      if components.exists(_.role == role) then Validation.unit
      else Validation.fail(ConfigurationError.MissingRequiredComponent(category.id, role))
    }

    // Check each component's material is allowed
    val materialChecks = components.map { comp =>
      val allowedMaterials = category.allowedMaterialIdsByRole
        .getOrElse(comp.role, category.allowedMaterialIds)
      if allowedMaterials.contains(comp.material.id) then Validation.unit
      else Validation.fail(ConfigurationError.InvalidComponentMaterial(category.id, comp.role, comp.material.id))
    }

    // Check each component's finishes are allowed
    val finishChecks = components.flatMap { comp =>
      comp.finishes.map { finish =>
        if category.allowedFinishIds.contains(finish.id) then Validation.unit
        else Validation.fail(ConfigurationError.InvalidCategoryFinish(category.id, finish.id))
      }
    }

    // Check each component has ink configuration
    val inkConfigChecks = components.map { comp =>
      comp.inkConfiguration match
        case None => Validation.fail(ConfigurationError.MissingComponentInkConfig(comp.role))
        case Some(inkConfig) =>
          printingMethod.maxColorCount match
            case Some(max) if inkConfig.maxColorCount > max =>
              Validation.fail(ConfigurationError.ComponentInkConfigExceedsMethodColorLimit(
                comp.role, printingMethod.id, inkConfig, max))
            case _ => Validation.unit
    }

    // Check shared spec requirements (exclude InkConfig for multi-component — it's per-component)
    val sharedSpecKinds = category.requiredSpecKinds - SpecKind.InkConfig
    val specChecks = sharedSpecKinds.toList.map { kind =>
      if specifications.specKinds.contains(kind) then Validation.unit
      else Validation.fail(ConfigurationError.MissingRequiredSpec(category.id, kind))
    }

    // Check printing method
    val printingMethodCheck =
      if category.allowedPrintingMethodIds.isEmpty || category.allowedPrintingMethodIds.contains(printingMethod.id) then
        Validation.unit
      else Validation.fail(ConfigurationError.InvalidCategoryPrintingMethod(category.id, printingMethod.id))

    val allChecks = roleChecks ::: materialChecks ::: finishChecks ::: inkConfigChecks ::: specChecks ::: List(printingMethodCheck)
    allChecks.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
      acc.zipRight(v),
    )

  private def validateStructural(
      category: ProductCategory,
      material: Material,
      finishes: List[Finish],
      specifications: ProductSpecifications,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    val materialCheck =
      if category.allowedMaterialIds.contains(material.id) then Validation.unit
      else Validation.fail(ConfigurationError.InvalidCategoryMaterial(category.id, material.id))

    val finishChecks = finishes.map { finish =>
      if category.allowedFinishIds.contains(finish.id) then Validation.unit
      else Validation.fail(ConfigurationError.InvalidCategoryFinish(category.id, finish.id))
    }

    val specChecks = category.requiredSpecKinds.toList.map { kind =>
      if specifications.specKinds.contains(kind) then Validation.unit
      else Validation.fail(ConfigurationError.MissingRequiredSpec(category.id, kind))
    }

    val printingMethodCheck =
      if category.allowedPrintingMethodIds.isEmpty || category.allowedPrintingMethodIds.contains(printingMethod.id) then
        Validation.unit
      else Validation.fail(ConfigurationError.InvalidCategoryPrintingMethod(category.id, printingMethod.id))

    val inkConfigCheck = (specifications.get(SpecKind.InkConfig), printingMethod.maxColorCount) match
      case (Some(SpecValue.InkConfigSpec(config)), Some(max)) =>
        if config.maxColorCount > max then
          Validation.fail(ConfigurationError.InkConfigExceedsMethodColorLimit(printingMethod.id, config, max))
        else Validation.unit
      case _ => Validation.unit

    val allChecks = materialCheck :: finishChecks ::: specChecks ::: List(printingMethodCheck, inkConfigCheck)
    allChecks.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
      acc.zipRight(v),
    )
