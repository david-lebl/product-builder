package mpbuilder.pricing

import mpbuilder.catalog.*
import mpbuilder.kernel.*

/** Order-level surcharge lines and quantity-tier lookups. */
private[pricing] object Surcharges:

  def findFoldSurcharge(
      foldType: Option[FoldType],
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    foldType.flatMap { ft =>
      rules.collectFirst {
        case r: PricingRule.FoldTypeSurcharge if r.foldType == ft => r.surchargePerUnit
      }.map { surcharge =>
        LineItem(
          label = s"Fold: ${foldTypeName(ft, lang)}",
          unitPrice = surcharge,
          quantity = quantity,
          lineTotal = surcharge * quantity,
        )
      }
    }

  def findBindingSurcharge(
      bindingMethod: Option[BindingMethod],
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    bindingMethod.flatMap { bm =>
      rules.collectFirst {
        case r: PricingRule.BindingMethodSurcharge if r.bindingMethod == bm => r.surchargePerUnit
      }.map { surcharge =>
        LineItem(
          label = s"Binding: ${bindingMethodName(bm, lang)}",
          unitPrice = surcharge,
          quantity = quantity,
          lineTotal = surcharge * quantity,
        )
      }
    }

  def findProcessSurcharge(
      method: PrintingMethod,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    rules.collectFirst {
      case r: PricingRule.PrintingProcessSurcharge if r.processType == method.processType =>
        LineItem(
          label = s"Process: ${method.name(lang)}",
          unitPrice = r.surchargePerUnit,
          quantity = quantity,
          lineTotal = r.surchargePerUnit * quantity,
        )
    }

  def findCategorySurcharge(
      category: ProductCategory,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
  ): Option[LineItem] =
    rules.collectFirst {
      case r: PricingRule.CategorySurcharge if r.categoryId == category.id =>
        LineItem(
          label = s"Category: ${category.name(lang)}",
          unitPrice = r.surchargePerUnit,
          quantity = quantity,
          lineTotal = r.surchargePerUnit * quantity,
        )
    }

  def findBestQuantityTier(
      rules: List[PricingRule],
      quantity: Int,
  ): Option[PricingRule.QuantityTier] =
    rules.collect {
      case r: PricingRule.QuantityTier
          if r.minQuantity <= quantity &&
            r.maxQuantity.forall(_ >= quantity) => r
    }.sortBy(_.minQuantity)(using scala.math.Ordering[Int].reverse).headOption

  def findBestSheetQuantityTier(
      rules: List[PricingRule],
      totalSheets: Int,
  ): Option[PricingRule.SheetQuantityTier] =
    rules.collect {
      case r: PricingRule.SheetQuantityTier
          if r.minSheets <= totalSheets &&
            r.maxSheets.forall(_ >= totalSheets) => r
    }.sortBy(_.minSheets)(using scala.math.Ordering[Int].reverse).headOption

  def foldTypeName(ft: FoldType, lang: Language): String = ft match
    case FoldType.Half       => lang match { case Language.Cs => "Přeložení na půl";    case _ => "Half Fold" }
    case FoldType.Tri        => lang match { case Language.Cs => "Trojsložení";          case _ => "Tri Fold" }
    case FoldType.Gate       => lang match { case Language.Cs => "Okénkové složení";    case _ => "Gate Fold" }
    case FoldType.Accordion  => lang match { case Language.Cs => "Harmonikové složení"; case _ => "Accordion Fold" }
    case FoldType.ZFold      => lang match { case Language.Cs => "Z-složení";           case _ => "Z-Fold" }
    case FoldType.RollFold   => lang match { case Language.Cs => "Rolovací složení";    case _ => "Roll Fold" }
    case FoldType.FrenchFold => lang match { case Language.Cs => "Francouzské složení"; case _ => "French Fold" }
    case FoldType.CrossFold  => lang match { case Language.Cs => "Křížové složení";     case _ => "Cross Fold" }

  def bindingMethodName(bm: BindingMethod, lang: Language): String = bm match
    case BindingMethod.SaddleStitch   => lang match { case Language.Cs => "Sešití na svorky"; case _ => "Saddle Stitch" }
    case BindingMethod.PerfectBinding => lang match { case Language.Cs => "Lepená vazba";      case _ => "Perfect Binding" }
    case BindingMethod.SpiralBinding  => lang match { case Language.Cs => "Spirálová vazba";   case _ => "Spiral Binding" }
    case BindingMethod.WireOBinding   => lang match { case Language.Cs => "Wire-O vazba";      case _ => "Wire-O Binding" }
    case BindingMethod.CaseBinding    => lang match { case Language.Cs => "Pevná vazba";       case _ => "Case Binding" }
