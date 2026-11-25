package mpbuilder.pricing
package core

import mpbuilder.shared.*

/** Resolves a `CustomerPriceAgreement` onto a base `Pricelist`.
  *
  * Because every `RateModel` implements `mapMoney`, ALL rate types (`PerUnit`,
  * `PerSheet`, `PerSqMeter`, `TieredArea`, `PerLinearMeter`, `PerPerimeterMeter`,
  * `ByDiscreteParam`, `FlatFee`) receive customer discounts uniformly — eliminating
  * the legacy bug where newly added pricing rules were silently skipped.
  */
private[pricing] object AgreementResolver:

  def resolve(
      base: Pricelist,
      agreement: CustomerPriceAgreement,
      categoryId: CategoryId,
  ): Pricelist =
    if agreement.currency != base.currency then base
    else
      val adjustedEntries = base.entries.map { entry =>
        // One-time setup fees are fixed operational costs and are not percentage-discounted
        if entry.phase == ChargePhase.OneTimeSetup then entry
        else adjustVariableEntry(entry, agreement, categoryId)
      }

      base.copy(
        entries = adjustedEntries,
        volumeCurves = agreement.customVolumeCurves.getOrElse(base.volumeCurves),
        minimumOrderNet = agreement.minimumOrderOverride.orElse(base.minimumOrderNet),
      )

  private def adjustVariableEntry(
      entry: PriceEntry,
      ag: CustomerPriceAgreement,
      categoryId: CategoryId,
  ): PriceEntry =
    entry.target match
      case PricingTarget.MaterialRate(materialId, _) =>
        // Precedence: fixed material rate > material % > category % > global %
        ag.fixedMaterialRates.get(materialId) match
          case Some(fixedRate) => entry.copy(rate = fixedRate)
          case None =>
            val pctOpt =
              ag.materialDiscounts.get(materialId)
                .orElse(ag.categoryDiscounts.get(categoryId))
                .orElse(ag.globalDiscount)
            pctOpt.fold(entry)(pct => entry.copy(rate = entry.rate.mapMoney(pct.applyDiscount)))

      case PricingTarget.FinishRate(finishId, _) =>
        // Precedence: finish % > category % > global %
        val pctOpt =
          ag.finishDiscounts.get(finishId)
            .orElse(ag.categoryDiscounts.get(categoryId))
            .orElse(ag.globalDiscount)
        pctOpt.fold(entry)(pct => entry.copy(rate = entry.rate.mapMoney(pct.applyDiscount)))

      case _ =>
        // Category % > global % applied uniformly to ink, finish-type, fold, binding, cutting, etc.
        val pctOpt = ag.categoryDiscounts.get(categoryId).orElse(ag.globalDiscount)
        pctOpt.fold(entry)(pct => entry.copy(rate = entry.rate.mapMoney(pct.applyDiscount)))
