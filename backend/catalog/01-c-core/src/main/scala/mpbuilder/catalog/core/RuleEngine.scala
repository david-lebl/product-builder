package mpbuilder.catalog
package core

import mpbuilder.shared.*
import zio.prelude.{AssociativeBothCovariantOps, Validation}

/** Single canonical compatibility rule evaluator.
  *
  * Used BOTH for full configuration validation (separating `Error` vs `Warning` rules)
  * AND for partial-selection UI option filtering (`isCandidateFinishCompatible`),
  * guaranteeing zero drift between option filtering and final validation.
  */
private[catalog] object RuleEngine:

  final case class EvaluationOutcome(
      errors: List[CatalogError.RuleViolation],
      warnings: List[ConfigurationWarning],
  ):
    def toValidation: Validation[CatalogError, List[ConfigurationWarning]] =
      errors match
        case Nil => Validation.succeed(warnings)
        case head :: tail =>
          tail.foldLeft(Validation.fail(head): Validation[CatalogError, List[ConfigurationWarning]]) { (acc, err) =>
            acc.zipRight(Validation.fail(err))
          }

  def evaluateAll(
      ruleset: CompatibilityRuleset,
      categoryId: CategoryId,
      components: List[ConfiguredComponent],
      specs: ProductSpecs,
  ): EvaluationOutcome =
    val applicable = ruleset.rules.filter { rule =>
      rule.scope match
        case RuleScope.Global            => true
        case RuleScope.Category(catId)   => catId == categoryId
    }

    val (errs, warns) = applicable.foldLeft((List.empty[CatalogError.RuleViolation], List.empty[ConfigurationWarning])) {
      case ((eAcc, wAcc), rule) =>
        val triggered = evalConfig(rule.when, components, Some(specs))
        if !triggered then (eAcc, wAcc)
        else
          val satisfied = evalConfig(rule.mustHold, components, Some(specs))
          if satisfied then (eAcc, wAcc)
          else
            rule.severity match
              case RuleSeverity.Error   => (eAcc :+ CatalogError.RuleViolation(rule.id, rule.reason), wAcc)
              case RuleSeverity.Warning => (eAcc, wAcc :+ ConfigurationWarning(rule.id, rule.reason))
    }
    EvaluationOutcome(errs, warns)

  /** Evaluates whether adding `candidate` finish to a partial component selection violates
    * any component-level `Error` rule (without requiring full specs).
    *
    * Because it runs the EXACT same predicate interpreter as `evaluateAll`, newly added
    * rules automatically work for option filtering with zero duplicate code.
    */
  def isCandidateFinishCompatible(
      ruleset: CompatibilityRuleset,
      categoryId: CategoryId,
      componentKey: ComponentKey,
      material: Material,
      printingMethod: Option[PrintingMethod],
      existingFinishes: List[SelectedFinish],
      candidate: Finish,
  ): Boolean =
    val dummyGeometry = ComponentGeometry(
      finishedSize = Dimension2D.A4,
      flatSize = Dimension2D.A4,
      unitsPerProduct = 1,
      totalFlatUnits = 1,
      areaPerUnitSqM = Dimension2D.A4.areaSqM,
      perimeterPerUnitM = Dimension2D.A4.perimeterMeters,
      imposition = ImpositionResult.DiscreteUnit(1),
    )
    val syntheticPrint = printingMethod.map(pm => PrintSpec(pm, InkConfiguration.cmyk4_0))
    val candidateSelected = SelectedFinish(
      finish = candidate,
      appliedSide = candidate.sidePolicy match
        case FinishSidePolicy.Fixed(s)            => Some(s)
        case FinishSidePolicy.Selectable(_, dflt) => Some(dflt)
        case FinishSidePolicy.NotApplicable       => None,
      params = None,
    )
    val syntheticComp = ConfiguredComponent(
      key = componentKey,
      material = material,
      print = syntheticPrint,
      finishes = existingFinishes :+ candidateSelected,
      geometry = dummyGeometry,
    )

    val applicableErrorRules = ruleset.rules.filter { rule =>
      rule.severity == RuleSeverity.Error &&
      (rule.scope == RuleScope.Global || rule.scope == RuleScope.Category(categoryId)) &&
      !mentionsSpec(rule.when) && !mentionsSpec(rule.mustHold) &&
      (printingMethod.isDefined || (!mentionsPrinting(rule.when) && !mentionsPrinting(rule.mustHold)))
    }

    applicableErrorRules.forall { rule =>
      val triggered = evalConfig(rule.when, List(syntheticComp), specsOpt = None)
      !triggered || evalConfig(rule.mustHold, List(syntheticComp), specsOpt = None)
    }

  private def evalConfig(
      pred: ConfigPredicate,
      components: List[ConfiguredComponent],
      specsOpt: Option[ProductSpecs],
  ): Boolean =
    pred match
      case ConfigPredicate.Always => true
      case ConfigPredicate.ForAllComponents(cp) =>
        components.forall(evalComponent(cp, _))
      case ConfigPredicate.ExistsComponent(cp) =>
        components.exists(evalComponent(cp, _))
      case ConfigPredicate.ForComponent(key, cp) =>
        components.find(_.key == key).exists(evalComponent(cp, _))
      case ConfigPredicate.DimensionCheck(dp) =>
        specsOpt.forall(s => evalDimension(dp, s.dimensions))
      case ConfigPredicate.QuantityCheck(qp) =>
        specsOpt.forall(s => evalQuantity(qp, s.quantity))
      case ConfigPredicate.BindingCheck(bp) =>
        specsOpt.forall(s => evalBinding(bp, s.binding))
      case ConfigPredicate.FoldCheck(fp) =>
        specsOpt.forall(s => evalFold(fp, s.dimensions.foldTypeOpt))
      case ConfigPredicate.And(l, r) =>
        evalConfig(l, components, specsOpt) && evalConfig(r, components, specsOpt)
      case ConfigPredicate.Or(l, r) =>
        evalConfig(l, components, specsOpt) || evalConfig(r, components, specsOpt)
      case ConfigPredicate.Not(inner) =>
        !evalConfig(inner, components, specsOpt)

  private def evalComponent(pred: ComponentPredicate, comp: ConfiguredComponent): Boolean =
    pred match
      case ComponentPredicate.AnyComponent => true
      case ComponentPredicate.KeyIs(k)     => comp.key == k
      case ComponentPredicate.MaterialIs(id) => comp.material.id == id
      case ComponentPredicate.MaterialFamilyIs(f) => comp.material.family == f
      case ComponentPredicate.SurfaceCoatingIs(c) => comp.material.surfaceCoating == c
      case ComponentPredicate.HasMaterialProperty(p) => comp.material.properties.contains(p)
      case ComponentPredicate.MinGrammage(min) => comp.material.weight.grammageGsm.exists(_ >= min)
      case ComponentPredicate.MaxGrammage(max) => comp.material.weight.grammageGsm.exists(_ <= max)
      case ComponentPredicate.HasFinish(id) => comp.finishes.exists(_.id == id)
      case ComponentPredicate.HasFinishType(ft) => comp.finishes.exists(_.finishType == ft)
      case ComponentPredicate.FinishCategoryCountAtMost(cat, max) =>
        comp.finishes.count(_.finishType.category == cat) <= max
      case ComponentPredicate.FinishParamMax(ft, maxVal) =>
        comp.finishes
          .filter(_.finishType == ft)
          .forall(_.params.flatMap(_.numericMetric).forall(_ <= maxVal))
      case ComponentPredicate.HasPrintingProcess(pt) =>
        comp.print.exists(_.printingMethod.processType == pt)
      case ComponentPredicate.HasPrintingMethod(id) =>
        comp.print.exists(_.printingMethod.id == id)
      case ComponentPredicate.AllowedProcessInks(allowed) =>
        comp.print.forall { ps =>
          val fOk = ps.ink.front.process == ProcessInk.None || allowed.contains(ps.ink.front.process)
          val bOk = ps.ink.back.process == ProcessInk.None || allowed.contains(ps.ink.back.process)
          fOk && bOk
        }
      case ComponentPredicate.HasSpecialInk(ink) =>
        comp.print.exists(_.ink.allSpecialInks.contains(ink))
      case ComponentPredicate.MaxColorsPerSide(max) =>
        comp.print.forall(_.ink.maxChannelsPerSide <= max)
      case ComponentPredicate.IsSingleSided =>
        comp.print.forall(_.ink.isSingleSided)
      case ComponentPredicate.IsPrintable =>
        comp.print.isDefined
      case ComponentPredicate.And(l, r) =>
        evalComponent(l, comp) && evalComponent(r, comp)
      case ComponentPredicate.Or(l, r) =>
        evalComponent(l, comp) || evalComponent(r, comp)
      case ComponentPredicate.Not(inner) =>
        !evalComponent(inner, comp)

  private def evalDimension(pred: DimensionPredicate, dims: ProductDimensions): Boolean =
    val box = dims.finishedBoundingBox
    pred match
      case DimensionPredicate.MinSize(minW, minH) => box.width >= minW && box.height >= minH
      case DimensionPredicate.MaxSize(maxW, maxH) => box.width <= maxW && box.height <= maxH
      case DimensionPredicate.IsSquare            => box.isSquare
      case DimensionPredicate.AllowedExactSizes(allowed) =>
        allowed.contains(box) || allowed.contains(box.rotated)

  private def evalQuantity(pred: QuantityPredicate, qty: Quantity): Boolean =
    pred match
      case QuantityPredicate.MinQuantity(min) => qty.value >= min
      case QuantityPredicate.MaxQuantity(max) => qty.value <= max

  private def evalBinding(pred: BindingPredicate, bindingOpt: Option[BindingSpec]): Boolean =
    pred match
      case BindingPredicate.AllowedMethods(allowed) =>
        bindingOpt.forall(b => allowed.contains(b.method))
      case BindingPredicate.MethodIn(methods) =>
        bindingOpt.exists(b => methods.contains(b.method))
      case BindingPredicate.MinPages(min) =>
        bindingOpt.forall(_.totalPages >= min)
      case BindingPredicate.MaxPages(max) =>
        bindingOpt.forall(_.totalPages <= max)
      case BindingPredicate.PagesDivisibleBy(n) =>
        bindingOpt.forall(b => n > 0 && b.totalPages % n == 0)

  private def evalFold(pred: FoldPredicate, foldOpt: Option[FoldType]): Boolean =
    pred match
      case FoldPredicate.AllowedFolds(allowed) => foldOpt.forall(allowed.contains)
      case FoldPredicate.FoldIs(ft)            => foldOpt.contains(ft)

  private def mentionsSpec(p: ConfigPredicate): Boolean = p match
    case _: ConfigPredicate.DimensionCheck | _: ConfigPredicate.QuantityCheck |
        _: ConfigPredicate.BindingCheck | _: ConfigPredicate.FoldCheck =>
      true
    case ConfigPredicate.And(l, r) => mentionsSpec(l) || mentionsSpec(r)
    case ConfigPredicate.Or(l, r)  => mentionsSpec(l) || mentionsSpec(r)
    case ConfigPredicate.Not(i)    => mentionsSpec(i)
    case _                         => false

  private def mentionsPrinting(p: ConfigPredicate): Boolean = p match
    case ConfigPredicate.ForAllComponents(cp) => mentionsPrintingComp(cp)
    case ConfigPredicate.ExistsComponent(cp)  => mentionsPrintingComp(cp)
    case ConfigPredicate.ForComponent(_, cp)  => mentionsPrintingComp(cp)
    case ConfigPredicate.And(l, r)            => mentionsPrinting(l) || mentionsPrinting(r)
    case ConfigPredicate.Or(l, r)             => mentionsPrinting(l) || mentionsPrinting(r)
    case ConfigPredicate.Not(i)               => mentionsPrinting(i)
    case _                                    => false

  private def mentionsPrintingComp(p: ComponentPredicate): Boolean = p match
    case _: ComponentPredicate.HasPrintingProcess | _: ComponentPredicate.HasPrintingMethod |
        _: ComponentPredicate.AllowedProcessInks | _: ComponentPredicate.HasSpecialInk |
        _: ComponentPredicate.MaxColorsPerSide | ComponentPredicate.IsSingleSided =>
      true
    case ComponentPredicate.And(l, r) => mentionsPrintingComp(l) || mentionsPrintingComp(r)
    case ComponentPredicate.Or(l, r)  => mentionsPrintingComp(l) || mentionsPrintingComp(r)
    case ComponentPredicate.Not(i)    => mentionsPrintingComp(i)
    case _                            => false
