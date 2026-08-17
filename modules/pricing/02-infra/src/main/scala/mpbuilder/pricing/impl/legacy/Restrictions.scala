package mpbuilder.pricing
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.manufacturing.TierRestriction
import mpbuilder.domain.model as dm
import mpbuilder.domain.pricing as dp
import mpbuilder.domain.service.{TierRestrictionValidator, UtilisationCalculator}

/** Decides whether a speed tier can be sold at all — spec §8.3.
  *
  * Two independent gates. The shop-saturation gate is global: past a critical load, Express comes
  * off the table entirely rather than being sold at an ever-escalating price. The per-category tier
  * restrictions are product-specific: quantity caps, binding methods that need curing time, and
  * excluded materials.
  */
private[pricing] object Restrictions:

  def check(
      speed: ProductionSpeed,
      config: dm.ProductConfiguration,
      restrictions: List[TierRestriction],
      context: dp.PricingContext,
  ): Option[SpeedUnavailable] =
    if speed == ProductionSpeed.Express && !UtilisationCalculator.isExpressAvailable(context.globalUtilisation)
    then Some(SpeedUnavailable.ShopSaturated)
    else
      val violations = TierRestrictionValidator.validate(
        tier = Mapping.toDomainSpeed(speed),
        restrictions = restrictions,
        categoryId = config.category.id,
        quantity = quantityOf(config),
        bindingMethod = bindingOf(config),
        finishTypes = config.components.flatMap(_.finishes.map(_.finish.finishType)).toSet,
        materialIds = config.components.map(_.material.id).toSet,
      )
      // The legacy validator reports free text, so the specific reason cannot be recovered
      // structurally here. See the TODO on SpeedUnavailable.Restricted.
      violations.headOption.map { v =>
        SpeedUnavailable.Restricted(LocalizedString(v.message(Language.En), v.message(Language.Cs)))
      }

  private def quantityOf(config: dm.ProductConfiguration): Int =
    config.specifications
      .get(dm.SpecKind.Quantity)
      .collect { case dm.SpecValue.QuantitySpec(q) => q.value }
      .getOrElse(1)

  private def bindingOf(config: dm.ProductConfiguration): Option[dm.BindingMethod] =
    config.specifications
      .get(dm.SpecKind.BindingMethod)
      .collect { case dm.SpecValue.BindingMethodSpec(m) => m }
