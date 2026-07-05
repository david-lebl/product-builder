package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import mpbuilder.pricing.*

object SamplePricelistUsd:

  val pricelist: Pricelist = Pricelist(
    rules = List(
      // --- Material base prices (per unit, for paper/cardboard) ---
      PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.08")),
      PricingRule.MaterialBasePrice(SampleCatalog.uncoatedBondId, Money("0.02")),
      PricingRule.MaterialBasePrice(SampleCatalog.kraftId, Money("0.06")),
      PricingRule.MaterialBasePrice(SampleCatalog.corrugatedId, Money("0.21")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedSilk250gsmId, Money("0.07")),
      PricingRule.MaterialBasePrice(SampleCatalog.yupoId, Money("0.14")),
      PricingRule.MaterialBasePrice(SampleCatalog.adhesiveStockId, Money("0.10")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonId, Money("0.18")),

      // --- Material area price (for vinyl — per sqm) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.vinylId, Money("16.20")),
      PricingRule.MaterialAreaPrice(SampleCatalog.clearVinylId, Money("20.20")),
      PricingRule.MaterialAreaPrice(SampleCatalog.pvc510gId, Money("16.20")),

      // --- Roll-Up material prices ---
      // Banner film: area price (per sqm), stand: base price (per unit)
      PricingRule.MaterialAreaPrice(SampleCatalog.rollUpBannerFilmId, Money("10.20")),
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
      PricingRule.FinishLinearMeterPrice(SampleCatalog.gumRopeId, Money("0.80")),

      // --- Finish surcharges (type-level) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("0.04")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("0.06")),
      PricingRule.FinishTypeSurcharge(FinishType.Scoring, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Perforation, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.RoundCorners, Money("0.02")),
      PricingRule.FinishTypeSurcharge(FinishType.Overlamination, Money("2.50")),

      // --- Scoring count surcharges (per piece, USD; discountable) ---
      PricingRule.ScoringCountSurcharge(1, Money("0.03")),
      PricingRule.ScoringCountSurcharge(2, Money("0.05")),
      PricingRule.ScoringCountSurcharge(3, Money("0.06")),
      PricingRule.ScoringCountSurcharge(4, Money("0.07")),
      // Scoring setup fee (flat, not discounted)
      PricingRule.ScoringSetupFee(Money("2.50")),

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

      // --- Printing method setup fees (one-time, not discounted) ---
      PricingRule.PrintingMethodSetupFee(SampleCatalog.offsetId, Money("15.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.digitalId, Money("10.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.uvInkjetId, Money("10.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.letterpressId, Money("25.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.solventInkjetId, Money("20.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.epson8ColorId, Money("20.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.screenPrintId, Money("20.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.dtgId, Money("10.00")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.sublimationId, Money("10.00")),

      // --- Ink configuration: per-unit cost keyed by printing method (USD) ---
      // Offset printing
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 4, 4, Money("0.04")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 4, 0, Money("0.02")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 4, 1, Money("0.03")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 1, 0, Money("0.005")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 1, 1, Money("0.008")),
      // Digital printing
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 4, 4, Money("0.04")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 4, 0, Money("0.02")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 4, 1, Money("0.03")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 1, 0, Money("0.005")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 1, 1, Money("0.008")),
      // Letterpress
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 4, 4, Money("0.04")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 4, 0, Money("0.02")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 4, 1, Money("0.03")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 1, 0, Money("0.005")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 1, 1, Money("0.008")),
      // UV inkjet (large format / sticker printing): per sqm
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 4, 4, Money("1.80")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 4, 0, Money("0.90")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 4, 1, Money("1.20")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 1, 0, Money("0.25")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 1, 1, Money("0.40")),
      // UV inkjet direct (per unit — for UV flatbed on mugs and other base-priced items)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 4, 4, Money("1.30")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 4, 0, Money("0.75")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 4, 1, Money("1.00")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 1, 0, Money("0.22")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 1, 1, Money("0.38")),
      // Screen printing (per unit)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 4, 4, Money("0.06")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 4, 0, Money("0.04")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 4, 1, Money("0.05")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 1, 0, Money("0.01")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 1, 1, Money("0.015")),
      // DTG (direct-to-garment, per unit)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 4, 4, Money("0.04")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 4, 0, Money("0.02")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 4, 1, Money("0.03")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 1, 0, Money("0.005")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 1, 1, Money("0.008")),
      // Dye sublimation (per unit)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 4, 4, Money("0.04")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 4, 0, Money("0.02")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 4, 1, Money("0.03")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 1, 0, Money("0.005")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 1, 1, Money("0.008")),
      // Solvent inkjet (sticker/vinyl, per sqm)
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 4, 4, Money("24.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 4, 0, Money("13.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 4, 1, Money("17.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 1, 0, Money("3.50")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 1, 1, Money("6.00")),
      // Epson 8-color (sticker/label, per sqm)
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 4, 4, Money("34.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 4, 0, Money("18.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 4, 1, Money("24.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 1, 0, Money("5.00")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 1, 1, Money("8.00")),

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

      // --- Promotional material base prices (per unit, USD) ---
      // T-Shirts
      PricingRule.MaterialBasePrice(SampleCatalog.cottonTshirt150Id, Money("3.20")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonTshirt180Id, Money("3.60")),
      PricingRule.MaterialBasePrice(SampleCatalog.polyesterTshirtId, Money("2.80")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonPolyBlendId, Money("3.00")),
      PricingRule.MaterialBasePrice(SampleCatalog.organicCottonTshirtId, Money("4.50")),
      // Eco Bags
      PricingRule.MaterialBasePrice(SampleCatalog.cottonCanvasBagId, Money("2.20")),
      PricingRule.MaterialBasePrice(SampleCatalog.organicCottonBagId, Money("2.80")),
      PricingRule.MaterialBasePrice(SampleCatalog.recycledPetBagId, Money("1.80")),
      PricingRule.MaterialBasePrice(SampleCatalog.juteBagId, Money("2.50")),
      PricingRule.MaterialBasePrice(SampleCatalog.nonWovenPpBagId, Money("0.60")),
      // Pin Badges
      PricingRule.MaterialBasePrice(SampleCatalog.tinplateBadgeId, Money("0.30")),
      PricingRule.MaterialBasePrice(SampleCatalog.acrylicBadgeId, Money("0.45")),
      PricingRule.MaterialBasePrice(SampleCatalog.woodenBadgeId, Money("0.55")),
      // Cups & Mugs
      PricingRule.MaterialBasePrice(SampleCatalog.ceramicMugWhiteId, Money("1.80")),
      PricingRule.MaterialBasePrice(SampleCatalog.ceramicMugColoredId, Money("2.20")),
      PricingRule.MaterialBasePrice(SampleCatalog.magicMugId, Money("3.50")),
      PricingRule.MaterialBasePrice(SampleCatalog.stainlessTravelMugId, Money("5.50")),
      PricingRule.MaterialBasePrice(SampleCatalog.enamelMugId, Money("3.00")),
      PricingRule.MaterialBasePrice(SampleCatalog.glassMugId, Money("2.50")),

      // --- Promotional finish surcharges (per unit, USD) ---
      PricingRule.FinishSurcharge(SampleCatalog.heatPressId, Money("0.40")),
      PricingRule.FinishSurcharge(SampleCatalog.labelPrintId, Money("0.15")),
      PricingRule.FinishSurcharge(SampleCatalog.foldBagId, Money("0.10")),
      PricingRule.FinishSurcharge(SampleCatalog.mylarOverlayId, Money("0.05")),
      PricingRule.FinishSurcharge(SampleCatalog.safetyPinId, Money("0.03")),
      PricingRule.FinishSurcharge(SampleCatalog.magnetBackId, Money("0.12")),
      PricingRule.FinishSurcharge(SampleCatalog.bottleOpenerId, Money("0.25")),
      PricingRule.FinishSurcharge(SampleCatalog.dishwasherCoatId, Money("0.20")),
      PricingRule.FinishSurcharge(SampleCatalog.giftBoxId, Money("1.40")),
      PricingRule.FinishSurcharge(SampleCatalog.glossyGlazeId, Money("0.15")),
      PricingRule.FinishSurcharge(SampleCatalog.embroideryId, Money("0.80")),
      PricingRule.FinishSurcharge(SampleCatalog.reinforcedHandlesId, Money("0.10")),

      // --- Promotional category surcharges (per unit, USD) ---
      PricingRule.CategorySurcharge(SampleCatalog.tshirtsId, Money("0.60")),
      PricingRule.CategorySurcharge(SampleCatalog.ecoBagsId, Money("0.30")),
      PricingRule.CategorySurcharge(SampleCatalog.cupsId, Money("0.50")),

      // --- Promotional printing process surcharges ---
      PricingRule.PrintingProcessSurcharge(PrintingProcessType.ScreenPrint, Money("0.15")),
    ),
    currency = Currency.USD,
    version = "1.1.0",
  )

