package mpbuilder.domain.sample

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.pricing.*
import SampleIds.{category as cat, finish as fin, ink as inks, material as mat, method as met}

/** The CZK sheet-based pricelist — a faithful transcription of
  * docs/ideas/full-catalog-czk-price-list.md §2–§9.
  */
object SamplePricelistCzk:

  import PricingRule.*
  import FinishTarget.{OfType, Specific}

  private def bd(s: String): BigDecimal = BigDecimal(s)

  // §2.1 sheet-priced papers (CZK per SRA3 sheet)
  private val glossySheetPrices = Map(90 -> 4, 115 -> 5, 130 -> 6, 150 -> 8, 170 -> 8, 200 -> 10, 250 -> 12, 350 -> 16)
  private val matteSheetPrices  = glossySheetPrices + (300 -> 14)

  private val sheetPrices: List[PricingRule] =
    glossySheetPrices.toList.map((gsm, p) => MaterialSheetPrice(mat.coatedGlossy(gsm), BigDecimal(p)))
      ++ matteSheetPrices.toList.map((gsm, p) => MaterialSheetPrice(mat.coatedMatte(gsm), BigDecimal(p)))
      ++ List(
        MaterialSheetPrice(mat.coatedArt300, BigDecimal(14)),
        MaterialSheetPrice(mat.coatedSilk250, BigDecimal(12)),
        MaterialSheetPrice(mat.uncoatedBond120, BigDecimal(4)),
        MaterialSheetPrice(mat.kraft250, BigDecimal(12)),
        MaterialSheetPrice(mat.cotton300, BigDecimal(16)),
        MaterialSheetPrice(mat.yupoSynthetic, BigDecimal(10)),
        MaterialSheetPrice(mat.adhesiveStock, BigDecimal(8)),
      )

  // §2.2 area-priced (CZK per m²) and §2.3 tiered
  private val areaPrices: List[PricingRule] = List(
    MaterialAreaPrice(mat.adhesiveVinyl, BigDecimal(375)),
    MaterialAreaPrice(mat.clearAdhesiveVinyl, BigDecimal(475)),
    MaterialAreaPrice(mat.polyesterBannerFilm, BigDecimal(235)),
    MaterialTieredAreaPrice(
      mat.pvcBanner510,
      List(
        AreaTier(BigDecimal(0), BigDecimal(555)),
        AreaTier(BigDecimal(2), BigDecimal(455)),
        AreaTier(BigDecimal(5), BigDecimal(405)),
        AreaTier(BigDecimal(10), BigDecimal(355)),
      ),
    ),
  )

  // §2.4 flat per-unit + §2.5 promotional (CZK per unit)
  private val unitPrices: List[PricingRule] = List(
    mat.corrugatedCardboard -> 2, mat.rollUpStandEconomy -> 590, mat.rollUpStandPremium -> 1290,
    mat.cottonTshirt150 -> 75, mat.cottonTshirt180 -> 85, mat.polyesterTshirt -> 65,
    mat.cottonPolyBlendTshirt -> 70, mat.organicCottonTshirt -> 105,
    mat.cottonCanvasBag -> 52, mat.organicCottonBag -> 65, mat.recycledPetBag -> 42,
    mat.juteBag -> 58, mat.nonWovenPpBag -> 14,
    mat.tinplateBadge -> 8, mat.acrylicBadge -> 11, mat.woodenBadge -> 13,
    mat.ceramicMugWhite -> 45, mat.ceramicMugColored -> 52, mat.magicMug -> 82,
    mat.stainlessTravelMug -> 130, mat.enamelMug -> 70, mat.glassMug -> 58,
  ).map((id, p) => MaterialUnitPrice(id, BigDecimal(p)))

  // §4.1 ink pricing — digital/offset/letterpress per sheet, UV inkjet per m²
  private val sheetInkPrices = List(
    inks.fullColorBoth -> bd("4"), inks.fullColorFront -> bd("2"), inks.fullFrontBlackBack -> bd("2.5"),
    inks.blackFront -> bd("0.6"), inks.blackBoth -> bd("1"),
  )
  private val areaInkPrices = List(
    inks.fullColorBoth -> bd("45"), inks.fullColorFront -> bd("22"), inks.fullFrontBlackBack -> bd("30"),
    inks.blackFront -> bd("6"), inks.blackBoth -> bd("10"),
  )
  private val inkPrices: List[PricingRule] =
    (for
      method       <- List(met.digital, met.offset, met.letterpress)
      (ink, price) <- sheetInkPrices
    yield InkPricePerSheet(method, ink, price))
      ++ areaInkPrices.map((ink, price) => InkPricePerM2(met.uvInkjet, ink, price))

  // §3.1 finish-specific surcharges + one-time setup fees
  private val finishSpecific: List[PricingRule] = List(
    FinishSurcharge(Specific(fin.matteLamination), bd("6")),
    FinishSetupFee(Specific(fin.matteLamination), bd("300")),
    FinishSurcharge(Specific(fin.glossLamination), bd("6")),
    FinishSetupFee(Specific(fin.glossLamination), bd("300")),
    FinishSurcharge(Specific(fin.softTouchCoating), bd("9")),
    FinishSetupFee(Specific(fin.softTouchCoating), bd("300")),
    FinishSurcharge(Specific(fin.embossing), bd("2")),
    FinishSetupFee(Specific(fin.embossing), bd("350")),
    FinishSurcharge(Specific(fin.debossing), bd("2")),
    FinishSetupFee(Specific(fin.debossing), bd("350")),
    FinishSurcharge(Specific(fin.foilStamping), bd("3.5")),
    FinishSetupFee(Specific(fin.foilStamping), bd("450")),
    FinishSurcharge(Specific(fin.dieCut), bd("3")),
    FinishSetupFee(Specific(fin.dieCut), bd("600")),
    FinishSurcharge(Specific(fin.kissCut), bd("2")),
    FinishSetupFee(Specific(fin.kissCut), bd("200")),
    FinishSetupFee(Specific(fin.uvCoating), bd("300")),   // surcharge is type-level (§3.2)
    FinishSetupFee(Specific(fin.spotVarnish), bd("300")), // surcharge is type-level (§3.2)
  )

  // §3.2 finish-type surcharges
  private val finishTypeLevel: List[PricingRule] = List(
    FinishSetupFee(OfType(FinishType.Lamination), bd("300")),
    FinishSurcharge(OfType(FinishType.UvCoating), bd("1")),
    FinishSurcharge(OfType(FinishType.AqueousCoating), bd("0.5")),
    FinishSurcharge(OfType(FinishType.SpotVarnish), bd("1.5")),
    FinishSurcharge(OfType(FinishType.RoundCorners), bd("0.5")),
    FinishSetupFee(OfType(FinishType.RoundCorners), bd("40")),
    FinishSurcharge(OfType(FinishType.Perforation), bd("0.5")),
    FinishSetupFee(OfType(FinishType.Perforation), bd("60")),
    FinishSurcharge(OfType(FinishType.Overlamination), bd("60")),
  )

  // §3.3 parameterized finishes
  private val parameterized: List[PricingRule] =
    List(1 -> "0.60", 2 -> "1.00", 3 -> "1.30", 4 -> "1.50", 5 -> "1.70", 6 -> "1.90", 7 -> "2.10", 8 -> "2.30")
      .map((n, p) => CreaseCountPrice(n, bd(p)))
      ++ List(
        CreasingSetupFee(bd("60")),
        GrommetSpacingPrice(300, bd("60")),
        GrommetSpacingPrice(500, bd("40")),
        LinearLengthPrice(OfType(FinishType.GumRope), bd("18")),
      )

  // §3.4 promotional finishes (per unit)
  private val promotionalFinishes: List[PricingRule] = List(
    FinishSurcharge(Specific(fin.heatPressTransfer), bd("10")),
    FinishSetupFee(Specific(fin.heatPressTransfer), bd("200")),
    FinishSurcharge(Specific(fin.labelTagPrinting), bd("4")),
    FinishSurcharge(Specific(fin.foldBagPackaging), bd("3")),
    FinishSurcharge(Specific(fin.mylarOverlay), bd("1.5")),
    FinishSurcharge(Specific(fin.safetyPinBack), bd("1")),
    FinishSurcharge(Specific(fin.magnetBack), bd("3")),
    FinishSurcharge(Specific(fin.bottleOpenerBack), bd("6")),
    FinishSurcharge(Specific(fin.dishwasherCoating), bd("5")),
    FinishSurcharge(Specific(fin.giftBoxPackaging), bd("35")),
    FinishSurcharge(Specific(fin.ceramicGlaze), bd("4")),
    FinishSurcharge(Specific(fin.embroidery), bd("20")),
    FinishSetupFee(Specific(fin.embroidery), bd("500")),
    FinishSurcharge(Specific(fin.reinforcedHandles), bd("3")),
  )

  // §4 process surcharges, §5 folds/bindings, §6 category + cutting
  private val processAndCategory: List[PricingRule] = List(
    ProcessSurcharge(met.letterpress, bd("5")),
    ProcessSurcharge(met.screenPrinting, bd("4")),
    CategorySurcharge(cat.tShirts, bd("15")),
    CategorySurcharge(cat.ecoBags, bd("8")),
    CategorySurcharge(cat.cupsMugs, bd("12")),
    CuttingSurcharge(bd("0.10")),
  )

  private val folds: List[PricingRule] = List(
    (FoldType.HalfFold, "1.00", 80), (FoldType.TriFold, "1.50", 100), (FoldType.GateFold, "1.50", 120),
    (FoldType.AccordionFold, "1.50", 120), (FoldType.ZFold, "1.50", 100), (FoldType.RollFold, "1.50", 120),
    (FoldType.FrenchFold, "1.50", 120), (FoldType.CrossFold, "2.00", 150),
  ).flatMap((f, s, fee) => List(FoldSurcharge(f, bd(s)), FoldSetupFee(f, BigDecimal(fee))))

  private val bindings: List[PricingRule] = List(
    (BindingMethod.SaddleStitch, 1, 50), (BindingMethod.PerfectBinding, 5, 150),
    (BindingMethod.SpiralBinding, 15, 100), (BindingMethod.WireOBinding, 18, 100),
    (BindingMethod.CaseBinding, 25, 400),
  ).flatMap((b, s, fee) => List(BindingSurcharge(b, BigDecimal(s)), BindingSetupFee(b, BigDecimal(fee))))

  // §7 sheet-based volume discounts, §8 speed, §9 minimum
  private val orderLevel: List[PricingRule] = List(
    SheetVolumeDiscount(
      List(
        DiscountTier(1, bd("1.00")),
        DiscountTier(50, bd("0.90")),
        DiscountTier(250, bd("0.80")),
        DiscountTier(1000, bd("0.70")),
      )
    ),
    SpeedMultiplier(SpeedTier.Express, bd("1.35")),
    SpeedMultiplier(SpeedTier.Standard, bd("1.00")),
    SpeedMultiplier(SpeedTier.Economy, bd("0.85")),
    MinimumOrderPrice(bd("200")),
  )

  val pricelist: Pricelist = Pricelist(
    version = "1.0.0-czk-sheet",
    currency = Currency.CZK,
    rules = sheetPrices ++ areaPrices ++ unitPrices ++ inkPrices ++ finishSpecific ++ finishTypeLevel
      ++ parameterized ++ promotionalFinishes ++ processAndCategory ++ folds ++ bindings ++ orderLevel,
  )
