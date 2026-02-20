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

      // --- Finish surcharges (ID-level, CZK) ---
      PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.glossLaminationId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.softTouchCoatingId, Money("1.50")),
      PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.foilStampingId, Money("3.50")),

      // --- Finish surcharges (type-level, CZK) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("1")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("1.50")),

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
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy115gsmId,
        pricePerSheet = Money("9"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy130gsmId,
        pricePerSheet = Money("10"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte90gsmId,
        pricePerSheet = Money("8"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte115gsmId,
        pricePerSheet = Money("9"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte130gsmId,
        pricePerSheet = Money("10"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),

      // Medium papers (150-200gsm): ~12-14 CZK/sheet
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy150gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy170gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy200gsmId,
        pricePerSheet = Money("14"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte150gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte170gsmId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte200gsmId,
        pricePerSheet = Money("14"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.yupoId,
        pricePerSheet = Money("14"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.adhesiveStockId,
        pricePerSheet = Money("12"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.uncoatedBondId,
        pricePerSheet = Money("8"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("0.50"),
      ),

      // Heavy papers (250-350gsm): ~16-20 CZK/sheet
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy250gsmId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedGlossy350gsmId,
        pricePerSheet = Money("20"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coated300gsmId,
        pricePerSheet = Money("18"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte250gsmId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte300gsmId,
        pricePerSheet = Money("18"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedMatte350gsmId,
        pricePerSheet = Money("20"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.coatedSilk250gsmId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.kraftId,
        pricePerSheet = Money("16"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),
      PricingRule.MaterialSheetPrice(
        materialId = SampleCatalog.cottonId,
        pricePerSheet = Money("20"), sheetWidthMm = 320, sheetHeightMm = 450,
        bleedMm = 3, gutterMm = 2, minUnitPrice = Money("1.00"),
      ),

      // --- Material area price (for vinyl — CZK per sqm) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.vinylId, Money("420")),

      // --- Material base price (for corrugated — not sheet-fed) ---
      PricingRule.MaterialBasePrice(SampleCatalog.corrugatedId, Money("6")),

      // --- Cutting surcharge ---
      PricingRule.CuttingSurcharge(costPerCut = Money("0.10")),

      // --- Finish surcharges (ID-level, CZK) ---
      PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.glossLaminationId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.softTouchCoatingId, Money("1.50")),
      PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("2")),
      PricingRule.FinishSurcharge(SampleCatalog.foilStampingId, Money("3.50")),

      // --- Finish surcharges (type-level, CZK) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("1")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("1.50")),

      // --- Printing process surcharge (CZK) ---
      PricingRule.PrintingProcessSurcharge(PrintingProcessType.Letterpress, Money("5")),

      // --- Ink configuration factors ---
      PricingRule.InkConfigurationFactor(4, 4, BigDecimal("1.00")),
      PricingRule.InkConfigurationFactor(4, 0, BigDecimal("0.85")),
      PricingRule.InkConfigurationFactor(4, 1, BigDecimal("0.90")),
      PricingRule.InkConfigurationFactor(1, 0, BigDecimal("0.55")),
      PricingRule.InkConfigurationFactor(1, 1, BigDecimal("0.65")),

      // --- Quantity tiers (CZK market) ---
      PricingRule.QuantityTier(1, Some(99), BigDecimal("1.0")),
      PricingRule.QuantityTier(100, Some(499), BigDecimal("0.55")),
      PricingRule.QuantityTier(500, Some(999), BigDecimal("0.45")),
      PricingRule.QuantityTier(1000, None, BigDecimal("0.40")),
    ),
    currency = Currency.CZK,
    version = "1.0.0-czk-sheet",
  )
