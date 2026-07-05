package mpbuilder.pricing

import mpbuilder.catalog.*
import mpbuilder.kernel.*

/** One-time setup fee lines (finishes, folding, binding, printing method). */
private[pricing] object SetupFees:

  def collectSetupFees(
      finishes: List[SelectedFinish],
      foldType: Option[FoldType],
      bindingMethod: Option[BindingMethod],
      printingMethod: PrintingMethod,
      rules: List[PricingRule],
      lang: Language,
  ): List[LineItem] =
    val uniqueByIdFinishes = finishes.distinctBy(_.id)

    val (idItems, coveredTypes) = uniqueByIdFinishes.foldLeft((List.empty[LineItem], Set.empty[FinishType])) {
      case ((items, types), finish) =>
        rules.collectFirst {
          case r: PricingRule.FinishSetupFee if r.finishId == finish.id => r.setupCost
        } match
          case Some(cost) =>
            (items :+ LineItem(s"Setup: ${finish.name(lang)}", cost, 1, cost), types + finish.finishType)
          case None =>
            (items, types)
    }

    // ScoringSetupFee: flat one-time fee for any Scoring finish; takes precedence over FinishTypeSetupFee for Scoring
    val hasScoringFinish = uniqueByIdFinishes.exists(_.finishType == FinishType.Scoring)
    val (scoringSetupItem, scoringTypeCovered) =
      if hasScoringFinish && !coveredTypes.contains(FinishType.Scoring) then
        rules.collectFirst { case r: PricingRule.ScoringSetupFee => r.setupCost } match
          case Some(cost) =>
            val label = lang match
              case Language.En => "Setup: Creasing"
              case Language.Cs => "Příprava: Bigování"
            (Some(LineItem(label, cost, 1, cost)), true)
          case None => (None, false)
      else (None, false)

    val extendedCoveredTypes = if scoringTypeCovered then coveredTypes + FinishType.Scoring else coveredTypes

    val typeItems = uniqueByIdFinishes
      .distinctBy(_.finishType)
      .filterNot(f => extendedCoveredTypes.contains(f.finishType))
      .flatMap { finish =>
        rules.collectFirst {
          case r: PricingRule.FinishTypeSetupFee if r.finishType == finish.finishType => r.setupCost
        }.map { cost =>
          LineItem(s"Setup: ${finish.name(lang)}", cost, 1, cost)
        }
      }

    val foldFeeItem = foldType.flatMap { ft =>
      rules.collectFirst {
        case r: PricingRule.FoldTypeSetupFee if r.foldType == ft => r.setupCost
      }.map { cost => LineItem(s"Setup: ${Surcharges.foldTypeName(ft, lang)}", cost, 1, cost) }
    }.toList

    val bindingFeeItem = bindingMethod.flatMap { bm =>
      rules.collectFirst {
        case r: PricingRule.BindingMethodSetupFee if r.bindingMethod == bm => r.setupCost
      }.map { cost => LineItem(s"Setup: ${Surcharges.bindingMethodName(bm, lang)}", cost, 1, cost) }
    }.toList

    val printingMethodFeeItem = rules.collectFirst {
      case r: PricingRule.PrintingMethodSetupFee if r.printingMethodId == printingMethod.id => r.setupCost
    }.map { cost => LineItem(s"Setup: ${printingMethod.name(lang)}", cost, 1, cost) }.toList

    idItems ++ scoringSetupItem.toList ++ typeItems ++ foldFeeItem ++ bindingFeeItem ++ printingMethodFeeItem
