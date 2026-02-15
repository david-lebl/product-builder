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

      // --- Quantity tiers ---
      PricingRule.QuantityTier(1, Some(249), BigDecimal("1.0")),
      PricingRule.QuantityTier(250, Some(999), BigDecimal("0.90")),
      PricingRule.QuantityTier(1000, Some(4999), BigDecimal("0.80")),
      PricingRule.QuantityTier(5000, None, BigDecimal("0.70")),
    ),
    currency = Currency.USD,
    version = "1.0.0",
  )
