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
      PricingRule.MaterialAreaPrice(SampleCatalog.clearVinylId, Money("22.00")),

      // --- Roll-Up material prices ---
      // Banner film: area price (per sqm), stand: base price (per unit)
      PricingRule.MaterialAreaPrice(SampleCatalog.rollUpBannerFilmId, Money("12.00")),
      PricingRule.MaterialBasePrice(SampleCatalog.rollUpStandEconomyId, Money("25.00")),
      PricingRule.MaterialBasePrice(SampleCatalog.rollUpStandPremiumId, Money("55.00")),

      // --- Finish surcharges (ID-level) ---
      PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("0.03")),
      PricingRule.FinishSurcharge(SampleCatalog.glossLaminationId, Money("0.03")),
      PricingRule.FinishSurcharge(SampleCatalog.softTouchCoatingId, Money("0.05")),
      PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("0.08")),
      PricingRule.FinishSurcharge(SampleCatalog.debossingId, Money("0.08")),
      PricingRule.FinishSurcharge(SampleCatalog.foilStampingId, Money("0.15")),
      PricingRule.FinishSurcharge(SampleCatalog.dieCutId, Money("0.10")),
      PricingRule.FinishSurcharge(SampleCatalog.kissCutId, Money("0.05")),
      PricingRule.FinishSurcharge(SampleCatalog.grommetsId, Money("0.25")),

      // --- Finish surcharges (type-level) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("0.04")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("0.06")),
      PricingRule.FinishTypeSurcharge(FinishType.Scoring, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Perforation, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.RoundCorners, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Overlamination, Money("2.50")),

      // --- Fold type surcharges (per unit, USD) ---
      PricingRule.FoldTypeSurcharge(FoldType.Half, Money("0.02")),
      PricingRule.FoldTypeSurcharge(FoldType.Tri, Money("0.03")),
      PricingRule.FoldTypeSurcharge(FoldType.Gate, Money("0.04")),
      PricingRule.FoldTypeSurcharge(FoldType.Accordion, Money("0.04")),
      PricingRule.FoldTypeSurcharge(FoldType.ZFold, Money("0.03")),
      PricingRule.FoldTypeSurcharge(FoldType.RollFold, Money("0.04")),
      PricingRule.FoldTypeSurcharge(FoldType.FrenchFold, Money("0.04")),
      PricingRule.FoldTypeSurcharge(FoldType.CrossFold, Money("0.05")),

      // --- Binding method surcharges (per unit, USD) ---
      PricingRule.BindingMethodSurcharge(BindingMethod.SaddleStitch, Money("0.05")),
      PricingRule.BindingMethodSurcharge(BindingMethod.PerfectBinding, Money("0.12")),
      PricingRule.BindingMethodSurcharge(BindingMethod.SpiralBinding, Money("0.20")),
      PricingRule.BindingMethodSurcharge(BindingMethod.WireOBinding, Money("0.25")),
      PricingRule.BindingMethodSurcharge(BindingMethod.CaseBinding, Money("0.60")),

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

      // --- Manufacturing speed surcharges ---
      PricingRule.ManufacturingSpeedSurcharge(
        tier = ManufacturingSpeed.Express,
        multiplier = BigDecimal("1.35"),
        queueMultiplierThresholds = List(
          QueueThreshold(BigDecimal("0.50"), BigDecimal("0.10")),
          QueueThreshold(BigDecimal("0.70"), BigDecimal("0.15")),
          QueueThreshold(BigDecimal("0.85"), BigDecimal("0.25")),
        ),
      ),
      PricingRule.ManufacturingSpeedSurcharge(
        tier = ManufacturingSpeed.Standard,
        multiplier = BigDecimal("1.00"),
        queueMultiplierThresholds = List(
          QueueThreshold(BigDecimal("0.70"), BigDecimal("0.05")),
          QueueThreshold(BigDecimal("0.85"), BigDecimal("0.10")),
        ),
      ),
      PricingRule.ManufacturingSpeedSurcharge(
        tier = ManufacturingSpeed.Economy,
        multiplier = BigDecimal("0.85"),
        queueMultiplierThresholds = List.empty,
      ),
    ),
    currency = Currency.USD,
    version = "1.1.0",
  )

  /** Czech CZK pricelist for small format sheet printing.
    * Prices are estimated for the Czech market.
    * Base prices correspond to A3 flyer 4/4 CMYK per-unit prices.
    */
  val pricelistCzk: Pricelist = Pricelist(
    rules = List(
      // --- Coated Art Paper Glossy base prices (CZK per unit) ---
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy90gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy115gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy130gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy150gsmId, Money("13")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy170gsmId, Money("13")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy200gsmId, Money("14")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy250gsmId, Money("14")),
      PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("15")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy350gsmId, Money("15")),

      // --- Coated Art Paper Matte base prices (CZK per unit) ---
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte90gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte115gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte130gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte150gsmId, Money("13")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte170gsmId, Money("13")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte200gsmId, Money("14")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte250gsmId, Money("14")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte300gsmId, Money("15")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte350gsmId, Money("15")),

      // --- Other material base prices (CZK per unit, estimated) ---
      PricingRule.MaterialBasePrice(SampleCatalog.uncoatedBondId, Money("8")),
      PricingRule.MaterialBasePrice(SampleCatalog.kraftId, Money("10")),
      PricingRule.MaterialBasePrice(SampleCatalog.corrugatedId, Money("6")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedSilk250gsmId, Money("14")),
      PricingRule.MaterialBasePrice(SampleCatalog.yupoId, Money("16")),
      PricingRule.MaterialBasePrice(SampleCatalog.adhesiveStockId, Money("10")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonId, Money("18")),

      // --- Material area price (for vinyl — CZK per sqm) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.vinylId, Money("420")),
      PricingRule.MaterialAreaPrice(SampleCatalog.clearVinylId, Money("520")),

      // --- Roll-Up material prices (CZK) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.rollUpBannerFilmId, Money("280")),
      PricingRule.MaterialBasePrice(SampleCatalog.rollUpStandEconomyId, Money("590")),
      PricingRule.MaterialBasePrice(SampleCatalog.rollUpStandPremiumId, Money("1290")),

      // --- Finish surcharges (ID-level, CZK) ---
      PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.glossLaminationId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.softTouchCoatingId, Money("1.50")),
      PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.debossingId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.foilStampingId, Money("3.50")),
      PricingRule.FinishSurcharge(SampleCatalog.dieCutId, Money("3")),
      PricingRule.FinishSurcharge(SampleCatalog.kissCutId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.grommetsId, Money("8")),

      // --- Finish surcharges (type-level, CZK) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("1")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("1.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Overlamination, Money("60")),

      // --- Fold type surcharges (per unit, CZK) ---
      PricingRule.FoldTypeSurcharge(FoldType.Half, Money("0.50")),
      PricingRule.FoldTypeSurcharge(FoldType.Tri, Money("1.00")),
      PricingRule.FoldTypeSurcharge(FoldType.Gate, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.Accordion, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.ZFold, Money("1.00")),
      PricingRule.FoldTypeSurcharge(FoldType.RollFold, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.FrenchFold, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.CrossFold, Money("2.00")),

      // --- Binding method surcharges (per unit, CZK) ---
      PricingRule.BindingMethodSurcharge(BindingMethod.SaddleStitch, Money("2")),
      PricingRule.BindingMethodSurcharge(BindingMethod.PerfectBinding, Money("5")),
      PricingRule.BindingMethodSurcharge(BindingMethod.SpiralBinding, Money("8")),
      PricingRule.BindingMethodSurcharge(BindingMethod.WireOBinding, Money("10")),
      PricingRule.BindingMethodSurcharge(BindingMethod.CaseBinding, Money("25")),

      // --- Printing process surcharge (CZK) ---
      PricingRule.PrintingProcessSurcharge(PrintingProcessType.Letterpress, Money("5")),

      // --- Ink configuration factors ---
      // 4/4 CMYK both sides: full price
      PricingRule.InkConfigurationFactor(4, 4, BigDecimal("1.00")),
      // 4/0 CMYK front only: ~85% of 4/4 price (estimated from Czech market data)
      PricingRule.InkConfigurationFactor(4, 0, BigDecimal("0.85")),
      // 4/1 CMYK front + mono back
      PricingRule.InkConfigurationFactor(4, 1, BigDecimal("0.90")),
      // 1/0 Mono front only
      PricingRule.InkConfigurationFactor(1, 0, BigDecimal("0.55")),
      // 1/1 Mono both sides
      PricingRule.InkConfigurationFactor(1, 1, BigDecimal("0.65")),

      // --- Quantity tiers (CZK market, steeper volume discounts) ---
      PricingRule.QuantityTier(1, Some(99), BigDecimal("1.0")),
      PricingRule.QuantityTier(100, Some(499), BigDecimal("0.55")),
      PricingRule.QuantityTier(500, Some(999), BigDecimal("0.45")),
      PricingRule.QuantityTier(1000, None, BigDecimal("0.40")),

      // --- Finish setup fees (one-time, not discounted) ---
      // Lamination / coating
      PricingRule.FinishSetupFee(SampleCatalog.matteLaminationId, Money("50")),
      PricingRule.FinishSetupFee(SampleCatalog.glossLaminationId, Money("50")),
      PricingRule.FinishSetupFee(SampleCatalog.softTouchCoatingId, Money("80")),
      PricingRule.FinishTypeSetupFee(FinishType.Lamination, Money("50")),
      PricingRule.FinishSetupFee(SampleCatalog.uvCoatingId, Money("80")),
      PricingRule.FinishSetupFee(SampleCatalog.varnishId, Money("120")),
      // Embossing / stamping — custom die required
      PricingRule.FinishSetupFee(SampleCatalog.embossingId, Money("350")),
      PricingRule.FinishSetupFee(SampleCatalog.debossingId, Money("350")),
      PricingRule.FinishSetupFee(SampleCatalog.foilStampingId, Money("450")),
      // Cutting — custom die / plotter setup
      PricingRule.FinishSetupFee(SampleCatalog.dieCutId, Money("600")),
      PricingRule.FinishSetupFee(SampleCatalog.kissCutId, Money("200")),
      // Structural — machine/blade setup
      PricingRule.FinishTypeSetupFee(FinishType.Scoring, Money("50")),
      PricingRule.FinishTypeSetupFee(FinishType.Perforation, Money("60")),
      PricingRule.FinishTypeSetupFee(FinishType.RoundCorners, Money("40")),

      // --- Fold type setup fees (one-time, not discounted) ---
      PricingRule.FoldTypeSetupFee(FoldType.Half, Money("80")),
      PricingRule.FoldTypeSetupFee(FoldType.Tri, Money("100")),
      PricingRule.FoldTypeSetupFee(FoldType.Gate, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.Accordion, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.ZFold, Money("100")),
      PricingRule.FoldTypeSetupFee(FoldType.RollFold, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.FrenchFold, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.CrossFold, Money("150")),

      // --- Binding method setup fees (one-time, not discounted) ---
      PricingRule.BindingMethodSetupFee(BindingMethod.SaddleStitch, Money("80")),
      PricingRule.BindingMethodSetupFee(BindingMethod.PerfectBinding, Money("150")),
      PricingRule.BindingMethodSetupFee(BindingMethod.SpiralBinding, Money("100")),
      PricingRule.BindingMethodSetupFee(BindingMethod.WireOBinding, Money("100")),
      PricingRule.BindingMethodSetupFee(BindingMethod.CaseBinding, Money("400")),

      // --- Minimum order price ---
      PricingRule.MinimumOrderPrice(Money("500")),
    ),
    currency = Currency.CZK,
    version = "1.0.0-czk",
  )

  /** Czech CZK pricelist using sheet-based pricing.
    * Prices are per SRA3 sheet (320×450mm), with cutting surcharge.
    * Sheet pricing takes precedence over base pricing for materials that have it.
    */
  val pricelistCzkSheet: Pricelist = Pricelist(
    rules = List(
      // --- Material sheet prices (CZK per SRA3 sheet 320×450mm) ---
      // Light papers (90-130gsm): ~8-10 CZK/sheet
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy90gsmId,
        pricePerSheet = Money("8"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy115gsmId,
        pricePerSheet = Money("9"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy130gsmId,
        pricePerSheet = Money("10"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte90gsmId,
        pricePerSheet = Money("8"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte115gsmId,
        pricePerSheet = Money("9"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte130gsmId,
        pricePerSheet = Money("10"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),

      // Medium papers (150-200gsm): ~12-14 CZK/sheet
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy150gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy170gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy200gsmId,
        pricePerSheet = Money("14"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte150gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte170gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte200gsmId,
        pricePerSheet = Money("14"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.yupoId,
        pricePerSheet = Money("14"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.adhesiveStockId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.uncoatedBondId,
        pricePerSheet = Money("8"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),

      // Heavy papers (250-350gsm): ~16-20 CZK/sheet
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy250gsmId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy350gsmId,
        pricePerSheet = Money("20"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coated300gsmId,
        pricePerSheet = Money("18"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte250gsmId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte300gsmId,
        pricePerSheet = Money("18"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte350gsmId,
        pricePerSheet = Money("20"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedSilk250gsmId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.kraftId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.cottonId,
        pricePerSheet = Money("20"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2,
      ),

      // --- Material area price (for vinyl — CZK per sqm) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.vinylId, Money("420")),
      PricingRule.MaterialAreaPrice(SampleCatalog.clearVinylId, Money("520")),

      // --- Roll-Up material prices (CZK) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.rollUpBannerFilmId, Money("280")),
      PricingRule.MaterialBasePrice(SampleCatalog.rollUpStandEconomyId, Money("590")),
      PricingRule.MaterialBasePrice(SampleCatalog.rollUpStandPremiumId, Money("1290")),

      // --- Material base price (for corrugated — not sheet-fed) ---
      PricingRule.MaterialBasePrice(SampleCatalog.corrugatedId, Money("6")),

      // --- Cutting surcharge ---
      PricingRule.CuttingSurcharge(costPerCut = Money("0.10")),

      // --- Finish surcharges (ID-level, CZK) ---
      // Lamination rates are higher on sheet pricelist (more coverage per sheet)
      PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("6")),
      PricingRule.FinishSurcharge(SampleCatalog.glossLaminationId, Money("6")),
      PricingRule.FinishSurcharge(SampleCatalog.softTouchCoatingId, Money("9")),
      PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.debossingId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.foilStampingId, Money("3.50")),
      PricingRule.FinishSurcharge(SampleCatalog.dieCutId, Money("3")),
      PricingRule.FinishSurcharge(SampleCatalog.kissCutId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.grommetsId, Money("8")),

      // --- Finish surcharges (type-level, CZK) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("1")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("1.50")),
      PricingRule.FinishTypeSurcharge(FinishType.RoundCorners, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Perforation, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Scoring, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Overlamination, Money("60")),

      // --- Fold type surcharges (per unit, CZK) ---
      PricingRule.FoldTypeSurcharge(FoldType.Half, Money("1.00")),
      PricingRule.FoldTypeSurcharge(FoldType.Tri, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.Gate, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.Accordion, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.ZFold, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.RollFold, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.FrenchFold, Money("1.50")),
      PricingRule.FoldTypeSurcharge(FoldType.CrossFold, Money("2.00")),

      // --- Binding method surcharges (per unit, CZK) ---
      PricingRule.BindingMethodSurcharge(BindingMethod.SaddleStitch, Money("1")),
      PricingRule.BindingMethodSurcharge(BindingMethod.PerfectBinding, Money("5")),
      PricingRule.BindingMethodSurcharge(BindingMethod.SpiralBinding, Money("15")),
      PricingRule.BindingMethodSurcharge(BindingMethod.WireOBinding, Money("18")),
      PricingRule.BindingMethodSurcharge(BindingMethod.CaseBinding, Money("25")),

      // --- Printing process surcharge (CZK) ---
      PricingRule.PrintingProcessSurcharge(PrintingProcessType.Letterpress, Money("5")),

      // --- Ink configuration factors ---
      PricingRule.InkConfigurationFactor(4, 4, BigDecimal("1.00")),
      PricingRule.InkConfigurationFactor(4, 0, BigDecimal("0.85")),
      PricingRule.InkConfigurationFactor(4, 1, BigDecimal("0.90")),
      PricingRule.InkConfigurationFactor(1, 0, BigDecimal("0.55")),
      PricingRule.InkConfigurationFactor(1, 1, BigDecimal("0.65")),

      // --- Sheet quantity tiers (discount based on total physical sheets) ---
      PricingRule.SheetQuantityTier(1, Some(49), BigDecimal("1.0")),
      PricingRule.SheetQuantityTier(50, Some(249), BigDecimal("0.90")),
      PricingRule.SheetQuantityTier(250, Some(999), BigDecimal("0.80")),
      PricingRule.SheetQuantityTier(1000, None, BigDecimal("0.70")),

      // --- Finish setup fees (one-time, not discounted) ---
      // Lamination / coating
      PricingRule.FinishSetupFee(SampleCatalog.matteLaminationId, Money("300")),
      PricingRule.FinishSetupFee(SampleCatalog.glossLaminationId, Money("300")),
      PricingRule.FinishSetupFee(SampleCatalog.softTouchCoatingId, Money("300")),
      PricingRule.FinishTypeSetupFee(FinishType.Lamination, Money("300")),
      PricingRule.FinishSetupFee(SampleCatalog.uvCoatingId, Money("300")),
      PricingRule.FinishSetupFee(SampleCatalog.varnishId, Money("300")),
      // Embossing / stamping — custom die required
      PricingRule.FinishSetupFee(SampleCatalog.embossingId, Money("350")),
      PricingRule.FinishSetupFee(SampleCatalog.debossingId, Money("350")),
      PricingRule.FinishSetupFee(SampleCatalog.foilStampingId, Money("450")),
      // Cutting — custom die / plotter setup
      PricingRule.FinishSetupFee(SampleCatalog.dieCutId, Money("600")),
      PricingRule.FinishSetupFee(SampleCatalog.kissCutId, Money("200")),
      // Structural — machine/blade setup
      PricingRule.FinishTypeSetupFee(FinishType.Scoring, Money("50")),
      PricingRule.FinishTypeSetupFee(FinishType.Perforation, Money("60")),
      PricingRule.FinishTypeSetupFee(FinishType.RoundCorners, Money("40")),

      // --- Fold type setup fees (one-time, not discounted) ---
      PricingRule.FoldTypeSetupFee(FoldType.Half, Money("80")),
      PricingRule.FoldTypeSetupFee(FoldType.Tri, Money("100")),
      PricingRule.FoldTypeSetupFee(FoldType.Gate, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.Accordion, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.ZFold, Money("100")),
      PricingRule.FoldTypeSetupFee(FoldType.RollFold, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.FrenchFold, Money("120")),
      PricingRule.FoldTypeSetupFee(FoldType.CrossFold, Money("150")),

      // --- Binding method setup fees (one-time, not discounted) ---
      PricingRule.BindingMethodSetupFee(BindingMethod.SaddleStitch, Money("50")),
      PricingRule.BindingMethodSetupFee(BindingMethod.PerfectBinding, Money("150")),
      PricingRule.BindingMethodSetupFee(BindingMethod.SpiralBinding, Money("100")),
      PricingRule.BindingMethodSetupFee(BindingMethod.WireOBinding, Money("100")),
      PricingRule.BindingMethodSetupFee(BindingMethod.CaseBinding, Money("400")),

      // --- Minimum order price ---
      PricingRule.MinimumOrderPrice(Money("500")),
    ),
    currency = Currency.CZK,
    version = "1.0.0-czk-sheet",
  )
