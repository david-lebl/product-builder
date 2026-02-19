package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

object SamplePricelist:

  val pricelist: Pricelist = Pricelist(
    rules = List(
      // --- Material base prices (per unit, for paper/cardboard) ---
      PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.12")),
      PricingRule.MaterialBasePrice(SampleCatalog.uncoatedBondId, Money("0.06")),
      PricingRule.MaterialBasePrice(SampleCatalog.kraftId, Money("0.10")),
      PricingRule.MaterialBasePrice(SampleCatalog.corrugatedId, Money("0.25")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedSilk250gsmId, Money("0.11")),
      PricingRule.MaterialBasePrice(SampleCatalog.yupoId, Money("0.18")),
      PricingRule.MaterialBasePrice(SampleCatalog.adhesiveStockId, Money("0.14")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonId, Money("0.22")),

      // --- Material area price (for vinyl — per sqm) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.vinylId, Money("18.00")),

      // --- Finish surcharges (ID-level) ---
      PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("0.03")),
      PricingRule.FinishSurcharge(SampleCatalog.glossLaminationId, Money("0.03")),
      PricingRule.FinishSurcharge(SampleCatalog.softTouchCoatingId, Money("0.05")),
      PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("0.08")),
      PricingRule.FinishSurcharge(SampleCatalog.foilStampingId, Money("0.15")),

      // --- Finish surcharges (type-level) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("0.04")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("0.06")),

      // --- Printing process surcharge ---
      PricingRule.PrintingProcessSurcharge(PrintingProcessType.Letterpress, Money("0.20")),

      // --- Ink configuration factors ---
      PricingRule.InkConfigurationFactor(4, 4, BigDecimal("1.00")),
      PricingRule.InkConfigurationFactor(4, 0, BigDecimal("0.60")),
      PricingRule.InkConfigurationFactor(4, 1, BigDecimal("0.75")),
      PricingRule.InkConfigurationFactor(1, 0, BigDecimal("0.40")),
      PricingRule.InkConfigurationFactor(1, 1, BigDecimal("0.55")),

      // --- Quantity tiers ---
      PricingRule.QuantityTier(1, Some(249), BigDecimal("1.0")),
      PricingRule.QuantityTier(250, Some(999), BigDecimal("0.90")),
      PricingRule.QuantityTier(1000, Some(4999), BigDecimal("0.80")),
      PricingRule.QuantityTier(5000, None, BigDecimal("0.70")),
    ),
    currency = Currency.USD,
    version = "1.1.0",
  )
