package mpbuilder.ui.configurator

import com.raquo.laminar.api.L.*
import mpbuilder.api.*
import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import mpbuilder.domain.pricing.{PriceBreakdown, PricingEngine, PricingError}
import mpbuilder.domain.rules.OptionFilter
import mpbuilder.domain.validation.{ConfigValidator, ValidationError}

/** The configurator's state core: one `Var[ProductConfiguration]` (the exact
  * value POSTed to the backend) plus derived signals that run the shared
  * OptionFilter / ConfigValidator / PricingEngine on every edit — live local
  * pricing with no server round-trip, and only-valid options on offer.
  */
final class ConfiguratorState(val bundle: CatalogResponse):
  val catalog   = bundle.catalog
  val rules     = bundle.rules
  val pricelist = bundle.pricelist

  val draft: Var[Option[ProductConfiguration]] = Var(None)
  val confirmed: Var[Option[OrderResponse]]    = Var(None)
  val serverErrors: Var[List[ErrorDto]]        = Var(Nil)
  val submitting: Var[Boolean]                 = Var(false)

  val config: Signal[Option[ProductConfiguration]] = draft.signal

  val category: Signal[Option[Category]] =
    config.map(_.flatMap(c => catalog.categoriesById.get(c.categoryId)))

  val validationErrors: Signal[List[ValidationError]] =
    config.map {
      case None    => Nil
      case Some(c) =>
        ConfigValidator.validate(catalog, rules, c).toEither.swap.toOption.map(_.toList).getOrElse(Nil)
    }

  val price: Signal[Option[Either[List[PricingError], PriceBreakdown]]] =
    config.map(_.map(c => PricingEngine.price(catalog, pricelist, c).left.map(_.toList)))

  val isValid: Signal[Boolean] =
    validationErrors.combineWith(price).map((errors: List[ValidationError], p: Option[Either[List[PricingError], PriceBreakdown]]) =>
      errors.isEmpty && p.exists(_.isRight)
    )

  // ----- edits ----------------------------------------------------------------

  private def update(f: ProductConfiguration => ProductConfiguration): Unit =
    draft.update(_.map(f))
    serverErrors.set(Nil)

  def selectCategory(category: Category): Unit =
    draft.set(Some(blankFor(category)))
    serverErrors.set(Nil)

  def applyPreset(preset: Preset): Unit =
    draft.set(Some(preset.configuration))
    serverErrors.set(Nil)

  def clearCategory(): Unit =
    draft.set(None)
    serverErrors.set(Nil)

  def setPrintingMethod(id: PrintingMethodId): Unit = update { c =>
    val withMethod = c.copy(printingMethodId = id)
    val validInks  = OptionFilter.inkConfigsFor(catalog, id)
    if validInks.exists(_.id == c.inkConfigurationId) then withMethod
    else withMethod.copy(inkConfigurationId = validInks.headOption.map(_.id).getOrElse(c.inkConfigurationId))
  }

  def setInk(id: InkConfigId): Unit = update(_.copy(inkConfigurationId = id))

  def setSpeed(tier: SpeedTier): Unit = update(_.copy(speedTier = tier))

  def setMaterial(role: ComponentRole, materialId: MaterialId): Unit = update { c =>
    c.copy(components = c.components.map { comp =>
      if comp.role != role then comp
      else
        // prune finishes the new material no longer supports (weight rules etc.)
        val kept = comp.finishes.foldLeft(List.empty[SelectedFinish]) { (acc, sf) =>
          val stillValid = OptionFilter
            .finishesFor(catalog, rules, c.categoryId, role, Some(materialId), acc)
            .exists(_.id == sf.finishId)
          if stillValid then acc :+ sf else acc
        }
        comp.copy(materialId = materialId, finishes = kept)
    })
  }

  def toggleFinish(role: ComponentRole, finish: Finish): Unit = update { c =>
    c.copy(components = c.components.map { comp =>
      if comp.role != role then comp
      else if comp.finishes.exists(_.finishId == finish.id) then
        comp.copy(finishes = comp.finishes.filterNot(_.finishId == finish.id))
      else comp.copy(finishes = comp.finishes :+ SelectedFinish(finish.id, defaultParams(finish)))
    })
  }

  def setFinishParams(role: ComponentRole, finishId: FinishId, params: FinishParams): Unit = update { c =>
    c.copy(components = c.components.map { comp =>
      if comp.role != role then comp
      else
        comp.copy(finishes = comp.finishes.map { sf =>
          if sf.finishId == finishId then sf.copy(params = Some(params)) else sf
        })
    })
  }

  def toggleOptionalComponent(spec: ComponentSpec): Unit = update { c =>
    if c.components.exists(_.role == spec.role) then
      c.copy(components = c.components.filterNot(_.role == spec.role))
    else
      val firstMaterial =
        OptionFilter.materialsFor(catalog, c.categoryId, spec.role).headOption.map(_.id)
      firstMaterial match
        case Some(m) => c.copy(components = c.components :+ ComponentConfiguration(spec.role, m))
        case None    => c
  }

  def updateDetails(f: ProductDetails => ProductDetails): Unit =
    update(c => c.copy(details = f(c.details)))

  // ----- option signals --------------------------------------------------------

  def materialsFor(role: ComponentRole): Signal[List[Material]] =
    config.map {
      case Some(c) => OptionFilter.materialsFor(catalog, c.categoryId, role)
      case None    => Nil
    }

  def finishesFor(role: ComponentRole): Signal[List[(Finish, Boolean)]] =
    config.map {
      case None => Nil
      case Some(c) =>
        c.component(role) match
          case None => Nil
          case Some(comp) =>
            val selectable = OptionFilter
              .finishesFor(catalog, rules, c.categoryId, role, Some(comp.materialId), comp.finishes)
              .map(_ -> false)
            val selected = comp.finishes
              .flatMap(sf => catalog.finishesById.get(sf.finishId))
              .map(_ -> true)
            (selected ++ selectable).distinctBy(_._1.id)
    }

  val printingMethods: Signal[List[PrintingMethod]] =
    config.map {
      case Some(c) => OptionFilter.printingMethodsFor(catalog, c.categoryId)
      case None    => Nil
    }

  val inkConfigs: Signal[List[InkConfiguration]] =
    config.map {
      case Some(c) => OptionFilter.inkConfigsFor(catalog, c.printingMethodId)
      case None    => Nil
    }

  // ----- helpers ---------------------------------------------------------------

  private def blankFor(category: Category): ProductConfiguration =
    val method = OptionFilter.printingMethodsFor(catalog, category.id).headOption
    val ink    = method.map(m => OptionFilter.inkConfigsFor(catalog, m.id)).getOrElse(Nil).headOption
    ProductConfiguration(
      categoryId = category.id,
      printingMethodId = method.map(_.id).getOrElse(PrintingMethodId("digital")),
      inkConfigurationId = ink.map(_.id).getOrElse(InkConfigId("4-0")),
      components = category.components.filterNot(_.optional).map { spec =>
        val firstMaterial = OptionFilter
          .materialsFor(catalog, category.id, spec.role)
          .headOption
          .map(_.id)
          .getOrElse(MaterialId(""))
        ComponentConfiguration(spec.role, firstMaterial)
      },
      details = ProductDetails(quantity = Some(100)),
    )

  private def defaultParams(finish: Finish): Option[FinishParams] =
    finish.finishType match
      case FinishType.Scoring      => Some(FinishParams.Creases(1))
      case FinishType.Grommets     => Some(FinishParams.GrommetSpacing(500))
      case FinishType.GumRope      => Some(FinishParams.RopeLength(BigDecimal(1)))
      case FinishType.RoundCorners => Some(FinishParams.RoundCorners(4, 3))
      case _                       => None
