package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import mpbuilder.pricing.*

object SamplePricelistCzk:

  /** Czech CZK pricelist for small format sheet printing.
    * Prices are estimated for the Czech market.
    * Base prices correspond to A3 flyer 4/4 CMYK per-unit prices.
    */
  val pricelistCzk: Pricelist = Pricelist(
    rules = List(
      // --- Coated Art Paper Glossy base prices (CZK per unit) ---
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy90gsmId, Money("9")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy115gsmId, Money("9")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy130gsmId, Money("9")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy150gsmId, Money("10")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy170gsmId, Money("10")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy200gsmId, Money("11")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy250gsmId, Money("11")),
      PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy350gsmId, Money("12")),

      // --- Coated Art Paper Matte base prices (CZK per unit) ---
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte90gsmId, Money("9")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte115gsmId, Money("9")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte130gsmId, Money("9")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte150gsmId, Money("10")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte170gsmId, Money("10")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte200gsmId, Money("11")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte250gsmId, Money("11")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte300gsmId, Money("12")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedMatte350gsmId, Money("12")),

      // --- Other material base prices (CZK per unit, estimated) ---
      PricingRule.MaterialBasePrice(SampleCatalog.uncoatedBondId, Money("5")),
      PricingRule.MaterialBasePrice(SampleCatalog.kraftId, Money("7")),
      PricingRule.MaterialBasePrice(SampleCatalog.corrugatedId, Money("3")),
      PricingRule.MaterialBasePrice(SampleCatalog.coatedSilk250gsmId, Money("11")),
      PricingRule.MaterialBasePrice(SampleCatalog.yupoId, Money("13")),
      PricingRule.MaterialBasePrice(SampleCatalog.adhesiveStockId, Money("7")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonId, Money("15")),

      // --- Material area price (for vinyl — CZK per sqm) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.vinylId, Money("375")),
      PricingRule.MaterialAreaPrice(SampleCatalog.clearVinylId, Money("475")),

      // --- PVC Banner 510g: area-tiered price (CZK per sqm) ---
      PricingRule.MaterialAreaTier(
        SampleCatalog.pvc510gId,
        List(
          AreaTier(BigDecimal("0"),  Money("555")),
          AreaTier(BigDecimal("2"),  Money("455")),
          AreaTier(BigDecimal("5"),  Money("405")),
          AreaTier(BigDecimal("10"), Money("355")),
        ),
      ),

      // --- Roll-Up material prices (CZK) ---
      PricingRule.MaterialAreaPrice(SampleCatalog.rollUpBannerFilmId, Money("235")),
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
      // Grommets: area-based pricing driven by spacing (replaces flat surcharge for banners)
      PricingRule.GrommetSpacingAreaPrice(
        SampleCatalog.grommetsId,
        List(
          GrommetSpacingTier(300, Money("60")),
          GrommetSpacingTier(500, Money("40")),
        ),
      ),
      // Gum rope: linear-metre pricing
      PricingRule.FinishLinearMeterPrice(SampleCatalog.gumRopeId, Money("18")),

      // --- Finish surcharges (type-level, CZK) ---
      PricingRule.FinishTypeSurcharge(FinishType.UVCoating, Money("1")),
      PricingRule.FinishTypeSurcharge(FinishType.AqueousCoating, Money("0.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Varnish, Money("1.50")),
      PricingRule.FinishTypeSurcharge(FinishType.Overlamination, Money("60")),

      // --- Scoring count surcharges (per piece, CZK; discountable) ---
      PricingRule.ScoringCountSurcharge(1, Money("0.60")),
      PricingRule.ScoringCountSurcharge(2, Money("1.00")),
      PricingRule.ScoringCountSurcharge(3, Money("1.30")),
      PricingRule.ScoringCountSurcharge(4, Money("1.50")),
      // Scoring setup fee (flat, not discounted; replaces FinishTypeSetupFee for Scoring)
      PricingRule.ScoringSetupFee(Money("60")),

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

      // --- Printing method setup fees (one-time, not discounted, CZK) ---
      PricingRule.PrintingMethodSetupFee(SampleCatalog.offsetId, Money("350")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.digitalId, Money("200")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.uvInkjetId, Money("200")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.letterpressId, Money("600")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.solventInkjetId, Money("400")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.epson8ColorId, Money("400")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.screenPrintId, Money("400")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.dtgId, Money("200")),
      PricingRule.PrintingMethodSetupFee(SampleCatalog.sublimationId, Money("200")),

      // --- Ink configuration: per-unit cost keyed by printing method (CZK) ---
      // Offset printing
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 4, 4, Money("3")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 4, 0, Money("1.50")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 4, 1, Money("1.80")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 1, 0, Money("0.45")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.offsetId, 1, 1, Money("0.75")),
      // Digital printing
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 4, 4, Money("3")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 4, 0, Money("1.50")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 4, 1, Money("1.80")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 1, 0, Money("0.45")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.digitalId, 1, 1, Money("0.75")),
      // Letterpress
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 4, 4, Money("3")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 4, 0, Money("1.50")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 4, 1, Money("1.80")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 1, 0, Money("0.45")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.letterpressId, 1, 1, Money("0.75")),
      // Screen printing (per unit; process surcharge already adds 4 CZK for ScreenPrint process)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 4, 4, Money("4")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 4, 0, Money("2")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 4, 1, Money("3")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 1, 0, Money("0.60")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.screenPrintId, 1, 1, Money("1")),
      // DTG (direct-to-garment, per unit)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 4, 4, Money("3")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 4, 0, Money("1.50")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 4, 1, Money("1.80")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 1, 0, Money("0.45")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.dtgId, 1, 1, Money("0.75")),
      // Dye sublimation (per unit)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 4, 4, Money("3")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 4, 0, Money("1.50")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 4, 1, Money("1.80")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 1, 0, Money("0.45")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.sublimationId, 1, 1, Money("0.75")),
      // UV inkjet (large format / sticker printing): per sqm
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 4, 4, Money("720")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 4, 0, Money("360")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 4, 1, Money("480")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 1, 0, Money("100")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.uvInkjetId, 1, 1, Money("160")),
      // UV inkjet direct (per unit — for UV flatbed on mugs and other base-priced items)
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 4, 4, Money("35")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 4, 0, Money("20")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 4, 1, Money("27")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 1, 0, Money("6")),
      PricingRule.InkConfigurationSheetPrice(SampleCatalog.uvInkjetId, 1, 1, Money("10")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 4, 0, Money("300")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 4, 1, Money("400")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 1, 0, Money("80")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.solventInkjetId, 1, 1, Money("140")),
      // Epson 8-color (premium sticker / label printing): per sqm
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 4, 4, Money("840")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 4, 0, Money("420")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 4, 1, Money("560")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 1, 0, Money("110")),
      PricingRule.InkConfigurationAreaPrice(SampleCatalog.epson8ColorId, 1, 1, Money("190")),

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

      // --- Minimum order price ---
      PricingRule.MinimumOrderPrice(Money("200")),

      // --- Promotional material base prices (per unit, CZK) ---
      // T-Shirts
      PricingRule.MaterialBasePrice(SampleCatalog.cottonTshirt150Id, Money("75")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonTshirt180Id, Money("85")),
      PricingRule.MaterialBasePrice(SampleCatalog.polyesterTshirtId, Money("65")),
      PricingRule.MaterialBasePrice(SampleCatalog.cottonPolyBlendId, Money("70")),
      PricingRule.MaterialBasePrice(SampleCatalog.organicCottonTshirtId, Money("105")),
      // Eco Bags
      PricingRule.MaterialBasePrice(SampleCatalog.cottonCanvasBagId, Money("52")),
      PricingRule.MaterialBasePrice(SampleCatalog.organicCottonBagId, Money("65")),
      PricingRule.MaterialBasePrice(SampleCatalog.recycledPetBagId, Money("42")),
      PricingRule.MaterialBasePrice(SampleCatalog.juteBagId, Money("58")),
      PricingRule.MaterialBasePrice(SampleCatalog.nonWovenPpBagId, Money("14")),
      // Pin Badges
      PricingRule.MaterialBasePrice(SampleCatalog.tinplateBadgeId, Money("8")),
      PricingRule.MaterialBasePrice(SampleCatalog.acrylicBadgeId, Money("11")),
      PricingRule.MaterialBasePrice(SampleCatalog.woodenBadgeId, Money("13")),
      // Cups & Mugs
      PricingRule.MaterialBasePrice(SampleCatalog.ceramicMugWhiteId, Money("45")),
      PricingRule.MaterialBasePrice(SampleCatalog.ceramicMugColoredId, Money("52")),
      PricingRule.MaterialBasePrice(SampleCatalog.magicMugId, Money("82")),
      PricingRule.MaterialBasePrice(SampleCatalog.stainlessTravelMugId, Money("130")),
      PricingRule.MaterialBasePrice(SampleCatalog.enamelMugId, Money("70")),
      PricingRule.MaterialBasePrice(SampleCatalog.glassMugId, Money("58")),

      // --- Promotional finish surcharges (per unit, CZK) ---
      PricingRule.FinishSurcharge(SampleCatalog.heatPressId, Money("10")),
      PricingRule.FinishSurcharge(SampleCatalog.labelPrintId, Money("4")),
      PricingRule.FinishSurcharge(SampleCatalog.foldBagId, Money("3")),
      PricingRule.FinishSurcharge(SampleCatalog.mylarOverlayId, Money("1.50")),
      PricingRule.FinishSurcharge(SampleCatalog.safetyPinId, Money("1")),
      PricingRule.FinishSurcharge(SampleCatalog.magnetBackId, Money("3")),
      PricingRule.FinishSurcharge(SampleCatalog.bottleOpenerId, Money("6")),
      PricingRule.FinishSurcharge(SampleCatalog.dishwasherCoatId, Money("5")),
      PricingRule.FinishSurcharge(SampleCatalog.giftBoxId, Money("35")),
      PricingRule.FinishSurcharge(SampleCatalog.glossyGlazeId, Money("4")),
      PricingRule.FinishSurcharge(SampleCatalog.embroideryId, Money("20")),
      PricingRule.FinishSurcharge(SampleCatalog.reinforcedHandlesId, Money("3")),

      // --- Promotional category surcharges (per unit, CZK) ---
      PricingRule.CategorySurcharge(SampleCatalog.tshirtsId, Money("15")),
      PricingRule.CategorySurcharge(SampleCatalog.ecoBagsId, Money("8")),
      PricingRule.CategorySurcharge(SampleCatalog.cupsId, Money("12")),

      // --- Promotional printing process surcharges (CZK) ---
      PricingRule.PrintingProcessSurcharge(PrintingProcessType.ScreenPrint, Money("4")),

      // --- Promotional finish setup fees (one-time, CZK) ---
      PricingRule.FinishSetupFee(SampleCatalog.heatPressId, Money("200")),
      PricingRule.FinishSetupFee(SampleCatalog.embroideryId, Money("500")),
      PricingRule.FinishTypeSetupFee(FinishType.Embroidery, Money("500")),

      // --- Promotional minimum order price ---
      PricingRule.MinimumOrderPrice(Money("2000")),
    ),
    currency = Currency.CZK,
    version = "1.0.0-czk",
  )

