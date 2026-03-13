package mpbuilder.domain.pricing

import mpbuilder.domain.model.*

/** Generates a customer-specific `Pricelist` by overlaying `CustomerPricing`
  * discounts on top of a base `Pricelist`.
  *
  * The existing `PriceCalculator` can then be used unchanged — it simply
  * receives the adjusted pricelist and produces a `PriceBreakdown` at
  * customer-specific prices.
  *
  * For comparison display (base vs. customer price), call `PriceCalculator`
  * twice: once with the base pricelist and once with the resolved pricelist.
  */
object CustomerPricelistResolver:

  /** Resolve a customer-specific pricelist from a base pricelist and customer pricing overlay.
    *
    * @param basePricelist the base pricelist with standard prices
    * @param customerPricing the customer-specific pricing overlay
    * @param categoryId optional category of the product being priced — used to apply category discounts
    *                   to material prices when the material does not have a more specific discount
    * @return an adjusted pricelist with customer-specific prices
    */
  def resolve(
      basePricelist: Pricelist,
      customerPricing: CustomerPricing,
      categoryId: Option[CategoryId] = None,
  ): Pricelist =
    val adjustedRules = basePricelist.rules.flatMap { rule =>
      adjustRule(rule, customerPricing, basePricelist.currency, categoryId)
    }

    val withCustomQuantityTiers = customerPricing.customQuantityTiers match
      case Some(tiers) => adjustedRules ++ tiers
      case None        => adjustedRules

    val withCustomSheetTiers = customerPricing.customSheetQuantityTiers match
      case Some(tiers) => withCustomQuantityTiers ++ tiers
      case None        => withCustomQuantityTiers

    basePricelist.copy(rules = withCustomSheetTiers)

  private def adjustRule(
      rule: PricingRule,
      cp: CustomerPricing,
      currency: Currency,
      categoryId: Option[CategoryId],
  ): List[PricingRule] =
    rule match
      // --- Material price rules: apply fixed > material % > category % > global % ---
      case r: PricingRule.MaterialBasePrice =>
        List(adjustMaterialBasePrice(r, cp, currency, categoryId))

      case r: PricingRule.MaterialAreaPrice =>
        List(adjustMaterialAreaPrice(r, cp, currency, categoryId))

      case r: PricingRule.MaterialSheetPrice =>
        List(adjustMaterialSheetPrice(r, cp, currency, categoryId))

      // --- Finish surcharge rules: apply finish % > global % ---
      case r: PricingRule.FinishSurcharge =>
        List(adjustFinishSurcharge(r, cp))

      // --- FinishTypeSurcharge: apply global discount only (no per-type discounts in CustomerPricing) ---
      case r: PricingRule.FinishTypeSurcharge =>
        List(applyGlobalToFinishType(r, cp))

      // --- Quantity tiers: replace if customer has custom tiers ---
      case _: PricingRule.QuantityTier =>
        cp.customQuantityTiers match
          case Some(_) => Nil // will be added back after all rules processed
          case None    => List(rule)

      case _: PricingRule.SheetQuantityTier =>
        cp.customSheetQuantityTiers match
          case Some(_) => Nil // will be added back after all rules processed
          case None    => List(rule)

      // --- Minimum order: replace if customer has override ---
      case _: PricingRule.MinimumOrderPrice =>
        cp.minimumOrderOverride match
          case Some(minOverride) => List(PricingRule.MinimumOrderPrice(minOverride))
          case None              => List(rule)

      // --- All other rules pass through unchanged ---
      case _ => List(rule)
    end match

  // --- Material price adjustment helpers ---

  private def adjustMaterialBasePrice(
      r: PricingRule.MaterialBasePrice,
      cp: CustomerPricing,
      currency: Currency,
      categoryId: Option[CategoryId],
  ): PricingRule =
    // 1. Fixed price (currency must match)
    cp.fixedMaterialPrices.get(r.materialId) match
      case Some(fixedPrice) if fixedPrice.currency == currency =>
        r.copy(unitPrice = fixedPrice.amount)
      case _ =>
        // 2. Material-level percentage
        cp.materialDiscounts.get(r.materialId) match
          case Some(pct) => r.copy(unitPrice = pct.applyTo(r.unitPrice))
          case None =>
            // 3. Category-level percentage
            categoryId.flatMap(cp.categoryDiscounts.get) match
              case Some(pct) => r.copy(unitPrice = pct.applyTo(r.unitPrice))
              case None =>
                // 4. Global percentage
                cp.globalDiscount match
                  case Some(pct) => r.copy(unitPrice = pct.applyTo(r.unitPrice))
                  case None      => r

  private def adjustMaterialAreaPrice(
      r: PricingRule.MaterialAreaPrice,
      cp: CustomerPricing,
      currency: Currency,
      categoryId: Option[CategoryId],
  ): PricingRule =
    cp.fixedMaterialPrices.get(r.materialId) match
      case Some(fixedPrice) if fixedPrice.currency == currency =>
        r.copy(pricePerSqMeter = fixedPrice.amount)
      case _ =>
        cp.materialDiscounts.get(r.materialId) match
          case Some(pct) => r.copy(pricePerSqMeter = pct.applyTo(r.pricePerSqMeter))
          case None =>
            categoryId.flatMap(cp.categoryDiscounts.get) match
              case Some(pct) => r.copy(pricePerSqMeter = pct.applyTo(r.pricePerSqMeter))
              case None =>
                cp.globalDiscount match
                  case Some(pct) => r.copy(pricePerSqMeter = pct.applyTo(r.pricePerSqMeter))
                  case None      => r

  private def adjustMaterialSheetPrice(
      r: PricingRule.MaterialSheetPrice,
      cp: CustomerPricing,
      currency: Currency,
      categoryId: Option[CategoryId],
  ): PricingRule =
    cp.fixedMaterialPrices.get(r.materialId) match
      case Some(fixedPrice) if fixedPrice.currency == currency =>
        r.copy(pricePerSheet = fixedPrice.amount)
      case _ =>
        cp.materialDiscounts.get(r.materialId) match
          case Some(pct) => r.copy(pricePerSheet = pct.applyTo(r.pricePerSheet))
          case None =>
            categoryId.flatMap(cp.categoryDiscounts.get) match
              case Some(pct) => r.copy(pricePerSheet = pct.applyTo(r.pricePerSheet))
              case None =>
                cp.globalDiscount match
                  case Some(pct) => r.copy(pricePerSheet = pct.applyTo(r.pricePerSheet))
                  case None      => r

  // --- Finish surcharge adjustment helpers ---

  private def adjustFinishSurcharge(
      r: PricingRule.FinishSurcharge,
      cp: CustomerPricing,
  ): PricingRule =
    // Finish-specific discount takes priority over global
    cp.finishDiscounts.get(r.finishId) match
      case Some(pct) => r.copy(surchargePerUnit = pct.applyTo(r.surchargePerUnit))
      case None =>
        cp.globalDiscount match
          case Some(pct) => r.copy(surchargePerUnit = pct.applyTo(r.surchargePerUnit))
          case None      => r

  private def applyGlobalToFinishType(
      r: PricingRule.FinishTypeSurcharge,
      cp: CustomerPricing,
  ): PricingRule =
    cp.globalDiscount match
      case Some(pct) => r.copy(surchargePerUnit = pct.applyTo(r.surchargePerUnit))
      case None      => r
