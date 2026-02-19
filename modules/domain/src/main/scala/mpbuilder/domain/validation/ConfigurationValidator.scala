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
