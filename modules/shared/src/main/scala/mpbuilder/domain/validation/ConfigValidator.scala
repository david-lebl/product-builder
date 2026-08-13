package mpbuilder.domain.validation

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import mpbuilder.domain.rules.CompatibilityRule
import zio.NonEmptyChunk
import zio.prelude.Validation

/** Validates a product configuration against the catalog's structural
  * constraints and the cross-cutting compatibility rules — accumulating every
  * problem (spec §4: report all issues in a single pass).
  */
object ConfigValidator:

  def validate(
    catalog: Catalog,
    rules: List[CompatibilityRule],
    config: ProductConfiguration,
  ): Validation[ValidationError, ProductConfiguration] =
    catalog.categoriesById.get(config.categoryId) match
      case None =>
        Validation.fail(ValidationError.UnknownReference("category", config.categoryId.raw))
      case Some(category) =>
        accumulate(
          checkPrintingMethodAndInk(catalog, category, config)
            ++ checkComponents(catalog, category, config, rules)
            ++ checkDetails(category, config)
            ++ checkOrderRules(config, rules)
        ).map(_ => config)

  def validateContact(contact: CustomerContact): Validation[ValidationError, CustomerContact] =
    val emailOk = contact.email.count(_ == '@') == 1
      && contact.email.split('@').forall(part => part.nonEmpty && !part.startsWith(".") && !part.endsWith("."))
      && contact.email.split('@').lift(1).exists(_.contains('.'))
    accumulate(
      List(
        Option.when(contact.name.trim.isEmpty)(ValidationError.ContactInvalid("name", "Name is required")),
        Option.when(!emailOk)(ValidationError.ContactInvalid("email", "Valid email is required")),
        Option.when(contact.phone.trim.isEmpty)(ValidationError.ContactInvalid("phone", "Phone is required")),
      ).flatten
    ).map(_ => contact)

  // ---------------------------------------------------------------------------

  private def accumulate(errors: List[ValidationError]): Validation[ValidationError, Unit] =
    NonEmptyChunk.fromIterableOption(errors) match
      case Some(nec) => Validation.failNonEmptyChunk(nec)
      case None      => Validation.unit

  private def checkPrintingMethodAndInk(
    catalog: Catalog,
    category: Category,
    config: ProductConfiguration,
  ): List[ValidationError] =
    val methodErrors = catalog.printingMethodsById.get(config.printingMethodId) match
      case None =>
        List(ValidationError.UnknownReference("printingMethod", config.printingMethodId.raw))
      case Some(method) =>
        val allowed =
          if category.allowedPrintingMethods.allows(method.id) then Nil
          else List(ValidationError.PrintingMethodNotAllowed(method.id))
        val inkErrors = catalog.inkConfigurationsById.get(config.inkConfigurationId) match
          case None =>
            List(ValidationError.UnknownReference("inkConfiguration", config.inkConfigurationId.raw))
          case Some(ink) =>
            method.maxColors match
              case Some(max) if ink.maxColorsUsed > max =>
                List(ValidationError.TooManyInkColors(method.id, max, ink.maxColorsUsed))
              case _ => Nil
        allowed ++ inkErrors
    methodErrors

  private def checkComponents(
    catalog: Catalog,
    category: Category,
    config: ProductConfiguration,
    rules: List[CompatibilityRule],
  ): List[ValidationError] =
    val missing = category.components
      .filterNot(_.optional)
      .filter(spec => config.component(spec.role).isEmpty)
      .map(spec => ValidationError.MissingRequiredComponent(spec.role))

    val perComponent = config.components.flatMap { comp =>
      category.componentSpec(comp.role) match
        case None => List(ValidationError.UnexpectedComponent(comp.role))
        case Some(spec) =>
          val materialErrors = catalog.materialsById.get(comp.materialId) match
            case None => List(ValidationError.UnknownReference("material", comp.materialId.raw))
            case Some(_) if !spec.allowedMaterials.allows(comp.materialId) =>
              List(ValidationError.MaterialNotAllowedInCategory(comp.role, comp.materialId))
            case Some(_) => Nil
          materialErrors
            ++ comp.finishes.flatMap(checkFinish(catalog, spec, comp, _, rules, config))
            ++ checkFinishCombinations(catalog, comp, rules)
    }

    missing ++ perComponent

  private def checkFinish(
    catalog: Catalog,
    spec: ComponentSpec,
    comp: ComponentConfiguration,
    selected: SelectedFinish,
    rules: List[CompatibilityRule],
    config: ProductConfiguration,
  ): List[ValidationError] =
    catalog.finishesById.get(selected.finishId) match
      case None => List(ValidationError.UnknownReference("finish", selected.finishId.raw))
      case Some(finish) =>
        val allowed =
          if spec.allowedFinishes.allows(finish.id) then Nil
          else List(ValidationError.FinishNotAllowedInCategory(comp.role, finish.id))

        val params = (finish.finishType, selected.params) match
          case (FinishType.Scoring, Some(FinishParams.Creases(n))) =>
            val cap = rules.collectFirst {
              case CompatibilityRule.MaxCreases(catId, matId, max)
                  if catId.forall(_ == config.categoryId) && matId.forall(_ == comp.materialId) =>
                max
            }
            cap match
              case Some(max) if n > max => List(ValidationError.TooManyCreases(max, n))
              case _ if n < 1           => List(ValidationError.MissingFinishParams(comp.role, finish.id))
              case _                    => Nil
          case (FinishType.Scoring, _) => List(ValidationError.MissingFinishParams(comp.role, finish.id))
          case (FinishType.Grommets, Some(FinishParams.GrommetSpacing(_)))    => Nil
          case (FinishType.Grommets, _) => List(ValidationError.MissingFinishParams(comp.role, finish.id))
          case (FinishType.GumRope, Some(FinishParams.RopeLength(m))) if m > 0 => Nil
          case (FinishType.GumRope, _)  => List(ValidationError.MissingFinishParams(comp.role, finish.id))
          case (FinishType.RoundCorners, Some(FinishParams.RoundCorners(c, r))) if c >= 1 && c <= 4 && r > 0 => Nil
          case (FinishType.RoundCorners, _) => List(ValidationError.MissingFinishParams(comp.role, finish.id))
          case _ => Nil

        val weight = rules.collectFirst {
          case CompatibilityRule.FinishTypeMinWeight(t, minGsm) if t == finish.finishType =>
            catalog.materialsById
              .get(comp.materialId)
              .flatMap(_.weightGsm)
              .filter(_ < minGsm)
              .map(actual => ValidationError.FinishRequiresMinWeight(comp.role, finish.finishType, minGsm, actual))
        }.flatten.toList

        allowed ++ params ++ weight

  private def checkFinishCombinations(
    catalog: Catalog,
    comp: ComponentConfiguration,
    rules: List[CompatibilityRule],
  ): List[ValidationError] =
    val selected: List[Finish] =
      comp.finishes.flatMap(sf => catalog.finishesById.get(sf.finishId))
    val selectedIds   = selected.map(_.id).toSet
    val selectedTypes = selected.map(_.finishType)

    rules.flatMap {
      case CompatibilityRule.OnePerFinishType(t) =>
        Option.when(selectedTypes.count(_ == t) > 1)(
          ValidationError.OnePerFinishTypeExceeded(comp.role, t)
        )
      case CompatibilityRule.MutuallyExclusiveFinishTypes(a, b) =>
        Option.when(selectedTypes.contains(a) && selectedTypes.contains(b))(
          ValidationError.MutuallyExclusiveFinishTypes(comp.role, a, b)
        )
      case CompatibilityRule.MutuallyExclusiveFinishes(a, b) =>
        Option.when(selectedIds.contains(a) && selectedIds.contains(b))(
          ValidationError.MutuallyExclusiveFinishes(comp.role, a, b)
        )
      case _ => None
    }

  private def checkDetails(category: Category, config: ProductConfiguration): List[ValidationError] =
    import RequiredDetail.*
    val d = config.details
    category.requiredDetails.toList.flatMap { detail =>
      val present = detail match
        case Size        => d.size.isDefined
        case Quantity    => d.quantity.isDefined
        case Orientation => d.orientation.isDefined
        case Pages       => d.pages.exists(_ > 0)
        case Fold        => d.foldType.isDefined
        case Binding     => d.bindingMethod.isDefined
      Option.unless(present)(ValidationError.MissingDetail(detail))
    } ++ d.quantity.filter(_ <= 0).map(ValidationError.InvalidQuantity(_)).toList

  private def checkOrderRules(
    config: ProductConfiguration,
    rules: List[CompatibilityRule],
  ): List[ValidationError] =
    rules.flatMap {
      case CompatibilityRule.QuantityLimit(catId, min, max) if catId == config.categoryId =>
        config.details.quantity
          .filter(q => q < min || q > max)
          .map(ValidationError.QuantityOutOfRange(min, max, _))
      case CompatibilityRule.SizeLimit(catId, min, max) if catId == config.categoryId =>
        config.details.size
          .filter(s =>
            s.widthMm < min.widthMm || s.heightMm < min.heightMm ||
              s.widthMm > max.widthMm || s.heightMm > max.heightMm
          )
          .map(ValidationError.SizeOutOfRange(min, max, _))
      case _ => None
    }
