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
    val requiredRoles = category.components.filterNot(_.optional).map(_.role).toSet

    val roleCheck =
      if !requiredRoles.subsetOf(actualRoles) || !(actualRoles.subsetOf(expectedRoles)) then
        val missingErrors = (requiredRoles -- actualRoles).toList.map { role =>
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

          val finishParamChecks = comp.finishes.flatMap { sf =>
            sf.params match
              case None    => Nil
              case Some(p) => validateFinishParams(sf.finish, p)
          }

          val inkConfigCheck = printingMethod.maxColorCount match
            case Some(max) if comp.inkConfiguration.maxColorCount > max =>
              Validation.fail(ConfigurationError.InkConfigExceedsMethodColorLimit(printingMethod.id, comp.inkConfiguration, max, comp.role))
            case _ => Validation.unit

          materialCheck :: finishChecks ::: finishParamChecks ::: List(inkConfigCheck)
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

  private def validateFinishParams(
      finish: Finish,
      params: FinishParameters,
  ): List[Validation[ConfigurationError, Unit]] =
    params match
      case FinishParameters.RoundCornersParams(cornerCount, radiusMm) =>
        List(
          if Set(1, 2, 3, 4).contains(cornerCount) then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"cornerCount must be 1, 2, 3, or 4, got $cornerCount")),
          if radiusMm >= 1 && radiusMm <= 20 then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"radiusMm must be between 1 and 20, got $radiusMm")),
          if finish.finishType == FinishType.RoundCorners then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"RoundCornersParams can only be used with RoundCorners finish type")),
        )
      case FinishParameters.LaminationParams(side) =>
        List(
          if side == FinishSide.Front || side == FinishSide.Both then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"Lamination side must be Front or Both, got $side")),
          if finish.finishType == FinishType.Lamination || finish.finishType == FinishType.Overlamination || finish.finishType == FinishType.SoftTouchCoating then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"LaminationParams can only be used with Lamination, Overlamination or SoftTouchCoating finish types")),
        )
      case FinishParameters.FoilStampingParams(_) =>
        List(
          if finish.finishType == FinishType.FoilStamping then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"FoilStampingParams can only be used with FoilStamping finish type")),
        )
      case FinishParameters.GrommetParams(spacingMm) =>
        List(
          if spacingMm > 0 then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"GrommetParams spacingMm must be greater than 0, got $spacingMm")),
          if finish.finishType == FinishType.Grommets then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"GrommetParams can only be used with Grommets finish type")),
        )
      case FinishParameters.PerforationParams(pitchMm) =>
        List(
          if pitchMm > 0 then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"PerforationParams pitchMm must be greater than 0, got $pitchMm")),
          if finish.finishType == FinishType.Perforation then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"PerforationParams can only be used with Perforation finish type")),
        )
      case FinishParameters.GumRopeParams(lengthMm) =>
        List(
          if lengthMm > 0 then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"GumRopeParams lengthMm must be greater than 0, got $lengthMm")),
          if finish.finishType == FinishType.GumRope then Validation.unit
          else Validation.fail(ConfigurationError.InvalidFinishParameters(finish.id, s"GumRopeParams can only be used with GumRope finish type")),
        )
