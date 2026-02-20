package mpbuilder.domain.validation

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ProductCategory.*
import mpbuilder.domain.rules.*

object ConfigurationValidator:

  def validate(
      category: ProductCategory,
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      ruleset: CompatibilityRuleset,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    val structural = validateStructural(category, components, specifications, printingMethod)
    structural.flatMap(_ =>
      RuleEvaluator.evaluateAll(
        ruleset.rules,
        components,
        specifications,
        category.id,
        printingMethod,
      ),
    )

  private def validateStructural(
      category: ProductCategory,
      components: List[ProductComponent],
      specifications: ProductSpecifications,
      printingMethod: PrintingMethod,
  ): Validation[ConfigurationError, Unit] =
    val expectedRoles = category.components.map(_.role).toSet
    val actualRoles = components.map(_.role).toSet

    val roleCheck =
      if expectedRoles != actualRoles then
        val missingErrors = (expectedRoles -- actualRoles).toList.map { role =>
          ConfigurationError.MissingComponent(category.id, role)
        }
        val extraError =
          if (actualRoles -- expectedRoles).nonEmpty then
            List(ConfigurationError.InvalidComponentRoles(category.id, expectedRoles, actualRoles))
          else Nil
        val allErrors = missingErrors ::: extraError
        allErrors.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, e) =>
          acc.zipRight(Validation.fail(e)),
        )
      else Validation.unit

    val componentChecks = components.flatMap { comp =>
      category.componentFor(comp.role) match
        case None => List(Validation.fail(ConfigurationError.InvalidComponentRoles(category.id, expectedRoles, actualRoles)))
        case Some(template) =>
          val materialCheck =
            if template.allowedMaterialIds.contains(comp.material.id) then Validation.unit
            else Validation.fail(ConfigurationError.InvalidCategoryMaterial(category.id, comp.material.id, comp.role))

          val finishChecks = comp.finishes.map { finish =>
            if template.allowedFinishIds.contains(finish.id) then Validation.unit
            else Validation.fail(ConfigurationError.InvalidCategoryFinish(category.id, finish.id, comp.role))
          }

          val inkConfigCheck = printingMethod.maxColorCount match
            case Some(max) if comp.inkConfiguration.maxColorCount > max =>
              Validation.fail(ConfigurationError.InkConfigExceedsMethodColorLimit(printingMethod.id, comp.inkConfiguration, max, comp.role))
            case _ => Validation.unit

          materialCheck :: finishChecks ::: List(inkConfigCheck)
    }

    val specChecks = category.requiredSpecKinds.toList.map { kind =>
      if specifications.specKinds.contains(kind) then Validation.unit
      else Validation.fail(ConfigurationError.MissingRequiredSpec(category.id, kind))
    }

    val printingMethodCheck =
      if category.allowedPrintingMethodIds.isEmpty || category.allowedPrintingMethodIds.contains(printingMethod.id) then
        Validation.unit
      else Validation.fail(ConfigurationError.InvalidCategoryPrintingMethod(category.id, printingMethod.id))

    val allChecks = roleCheck :: componentChecks ::: specChecks ::: List(printingMethodCheck)
    allChecks.foldLeft(Validation.unit: Validation[ConfigurationError, Unit])((acc, v) =>
      acc.zipRight(v),
    )
