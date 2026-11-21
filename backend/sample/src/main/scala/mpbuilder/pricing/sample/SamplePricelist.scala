package mpbuilder.pricing.sample

import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.pricing.*
import mpbuilder.shared.*
import zio.NonEmptyChunk

/** Full sample pricelists replicated and modernized from legacy `SamplePricelist`:
  *   - `pricelistUsd`: USD unit/area pricelist (0% tax) covering all 50 materials & 9 methods
  *   - `pricelistCzk`: CZK unit/area pricelist (21% Czech VAT) covering all 50 materials & 9 methods
  *   - `pricelistCzkSheet`: CZK SRA3 press-sheet pricelist with imposition-driven `PerSheet` material,
  *     ink & lamination rates, guillotine cutting charges, `TotalPressSheets` volume curve,
  *     tiered PVC banner area pricing, grommet spacing area rates, and one-time setup fees
  */
object SamplePricelist:

  import SampleCatalog.*

  val pricelistUsdId: PricelistId      = PricelistId.unsafe("pl-usd-standard")
  val pricelistCzkId: PricelistId      = PricelistId.unsafe("pl-czk-unit")
  val pricelistCzkSheetId: PricelistId = PricelistId.unsafe("pl-czk-sheet")

  // ── Helper DSL for concise, readable PriceEntry construction ───────────────
  private final class EntryBuilder(prefix: String):
    private var seq = 0
    private val buf = scala.collection.mutable.ListBuffer.empty[PriceEntry]

    def add(
        target: PricingTarget,
        rate: RateModel,
        scope: PricingScope = PricingScope.global,
        phase: ChargePhase = ChargePhase.VariableDiscountable,
    ): Unit =
      seq += 1
      buf += PriceEntry(PriceEntryId.unsafe(s"$prefix-$seq"), target, scope, phase, rate)

    def matUnit(id: MaterialId, amt: String): Unit =
      add(PricingTarget.MaterialRate(id), RateModel.PerUnit(Money(amt)))

    def matSheet(id: MaterialId, amt: String): Unit =
      add(PricingTarget.MaterialRate(id), RateModel.PerSheet(Money(amt)))

    def matArea(id: MaterialId, amt: String): Unit =
      add(PricingTarget.MaterialRate(id), RateModel.PerSqMeter(Money(amt)))

    def finUnit(id: FinishId, amt: String, bySides: Boolean = false): Unit =
      add(PricingTarget.FinishRate(id, bySides), RateModel.PerUnit(Money(amt)))

    def finSheet(id: FinishId, amt: String, bySides: Boolean = false): Unit =
      add(PricingTarget.FinishRate(id, bySides), RateModel.PerSheet(Money(amt)))

    def finTypeUnit(typeKey: String, amt: String, bySides: Boolean = false): Unit =
      add(PricingTarget.FinishTypeRate(typeKey, bySides), RateModel.PerUnit(Money(amt)))

    def finTypeArea(typeKey: String, amt: String): Unit =
      add(PricingTarget.FinishTypeRate(typeKey), RateModel.PerSqMeter(Money(amt)))

    def setup(target: PricingTarget, amt: String): Unit =
      add(target, RateModel.FlatFee(Money(amt)), phase = ChargePhase.OneTimeSetup)

    def inkRates(
        pmId: PrintingMethodId,
        r44: String,
        r40: String,
        r41: String,
        r10: String,
        r11: String,
        r4W0: Option[String] = None,
        r20: Option[String] = None,
        asArea: Boolean = false,
        asSheet: Boolean = false,
        scope: PricingScope = PricingScope.global,
    ): Unit =
      def mkRate(s: String): RateModel =
        if asArea then RateModel.PerSqMeter(Money(s))
        else if asSheet then RateModel.PerSheet(Money(s))
        else RateModel.PerUnit(Money(s))

      add(PricingTarget.InkRate(pmId, InkBillingKey("4", "4")), mkRate(r44), scope)
      add(PricingTarget.InkRate(pmId, InkBillingKey("4", "0")), mkRate(r40), scope)
      add(PricingTarget.InkRate(pmId, InkBillingKey("4", "1")), mkRate(r41), scope)
      add(PricingTarget.InkRate(pmId, InkBillingKey("1", "0")), mkRate(r10), scope)
      add(PricingTarget.InkRate(pmId, InkBillingKey("1", "1")), mkRate(r11), scope)
      r4W0.foreach(r => add(PricingTarget.InkRate(pmId, InkBillingKey("4+W", "0")), mkRate(r), scope))
      r20.foreach(r => add(PricingTarget.InkRate(pmId, InkBillingKey("2", "0")), mkRate(r), scope))

    def result: List[PriceEntry] = buf.toList

  // ── Shared Speed Policies ──────────────────────────────────────────────────

  val standardSpeedPolicies: Map[ManufacturingSpeed, SpeedSurchargePolicy] = Map(
    ManufacturingSpeed.Express -> SpeedSurchargePolicy(
      speed = ManufacturingSpeed.Express,
      baseMultiplier = BigDecimal("1.35"),
      queueThresholds = List(
        QueueThreshold(BigDecimal("0.50"), BigDecimal("0.10")),
        QueueThreshold(BigDecimal("0.70"), BigDecimal("0.15")),
        QueueThreshold(BigDecimal("0.85"), BigDecimal("0.25")),
      ),
      subjectToSurge = true,
    ),
    ManufacturingSpeed.Standard -> SpeedSurchargePolicy(
      speed = ManufacturingSpeed.Standard,
      baseMultiplier = BigDecimal("1.00"),
      queueThresholds = List(
        QueueThreshold(BigDecimal("0.70"), BigDecimal("0.05")),
        QueueThreshold(BigDecimal("0.85"), BigDecimal("0.10")),
      ),
      subjectToSurge = true,
    ),
    ManufacturingSpeed.Economy -> SpeedSurchargePolicy(
      speed = ManufacturingSpeed.Economy,
      baseMultiplier = BigDecimal("0.85"),
      queueThresholds = List.empty,
      subjectToSurge = false,
    ),
  )

  private val unitVolumeCurve: VolumeCurve = VolumeCurve(
    scope = PricingScope.global,
    basis = VolumeBasis.FinishedUnits,
    tiers = NonEmptyChunk(
      VolumeTier(BigDecimal(1), Some(BigDecimal(249)), BigDecimal("1.00")),
      VolumeTier(BigDecimal(250), Some(BigDecimal(999)), BigDecimal("0.90")),
      VolumeTier(BigDecimal(1000), Some(BigDecimal(4999)), BigDecimal("0.80")),
      VolumeTier(BigDecimal(5000), None, BigDecimal("0.70")),
    ),
  )

  private val sheetVolumeCurve: VolumeCurve = VolumeCurve(
    scope = PricingScope.global,
    basis = VolumeBasis.TotalPressSheets,
    tiers = NonEmptyChunk(
      VolumeTier(BigDecimal(1), Some(BigDecimal(49)), BigDecimal("1.00")),
      VolumeTier(BigDecimal(50), Some(BigDecimal(249)), BigDecimal("0.90")),
      VolumeTier(BigDecimal(250), Some(BigDecimal(999)), BigDecimal("0.80")),
      VolumeTier(BigDecimal(1000), None, BigDecimal("0.70")),
    ),
  )

  // ═══════════════════════════════════════════════════════════════════════════
  // 1. USD Pricelist (Unit & Area Based, 0% Tax)
  // ═══════════════════════════════════════════════════════════════════════════

  val pricelistUsd: Pricelist =
    val b = new EntryBuilder("usd")

    // Core papers & stocks (per unit)
    b.matUnit(coated300gsmId, "0.08")
    b.matUnit(uncoatedBondId, "0.02")
    b.matUnit(kraftId, "0.06")
    b.matUnit(corrugatedId, "0.21")
    b.matUnit(coatedSilk250gsmId, "0.07")
    b.matUnit(yupoId, "0.14")
    b.matUnit(adhesiveStockId, "0.10")
    b.matUnit(cottonId, "0.18")

    // Coated glossy & matte series (USD per unit)
    b.matUnit(coatedGlossy90gsmId, "0.04")
    b.matUnit(coatedGlossy115gsmId, "0.04")
    b.matUnit(coatedGlossy130gsmId, "0.05")
    b.matUnit(coatedGlossy150gsmId, "0.05")
    b.matUnit(coatedGlossy170gsmId, "0.06")
    b.matUnit(coatedGlossy200gsmId, "0.06")
    b.matUnit(coatedGlossy250gsmId, "0.07")
    b.matUnit(coatedGlossy350gsmId, "0.09")

    b.matUnit(coatedMatte90gsmId, "0.04")
    b.matUnit(coatedMatte115gsmId, "0.04")
    b.matUnit(coatedMatte130gsmId, "0.05")
    b.matUnit(coatedMatte150gsmId, "0.05")
    b.matUnit(coatedMatte170gsmId, "0.06")
    b.matUnit(coatedMatte200gsmId, "0.06")
    b.matUnit(coatedMatte250gsmId, "0.07")
    b.matUnit(coatedMatte300gsmId, "0.08")
    b.matUnit(coatedMatte350gsmId, "0.09")

    // Wide-format vinyl & banners (per sqm)
    b.matArea(vinylId, "16.20")
    b.matArea(clearVinylId, "20.20")
    b.matArea(pvc510gId, "16.20")

    // Roll-Up
    b.matArea(rollUpBannerFilmId, "10.20")
    b.matUnit(rollUpStandEconomyId, "25.00")
    b.matUnit(rollUpStandPremiumId, "55.00")

    // Promotional blanks
    b.matUnit(cottonTshirt150Id, "3.20")
    b.matUnit(cottonTshirt180Id, "3.60")
    b.matUnit(polyesterTshirtId, "2.80")
    b.matUnit(cottonPolyBlendId, "3.00")
    b.matUnit(organicCottonTshirtId, "4.50")

    b.matUnit(cottonCanvasBagId, "2.20")
    b.matUnit(organicCottonBagId, "2.80")
    b.matUnit(recycledPetBagId, "1.80")
    b.matUnit(juteBagId, "2.50")
    b.matUnit(nonWovenPpBagId, "0.60")

    b.matUnit(tinplateBadgeId, "0.30")
    b.matUnit(acrylicBadgeId, "0.45")
    b.matUnit(woodenBadgeId, "0.55")

    b.matUnit(ceramicMugWhiteId, "1.80")
    b.matUnit(ceramicMugColoredId, "2.20")
    b.matUnit(magicMugId, "3.50")
    b.matUnit(stainlessTravelMugId, "5.50")
    b.matUnit(enamelMugId, "3.00")
    b.matUnit(glassMugId, "2.50")

    // Finishes
    b.finUnit(matteLaminationId, "0.03", bySides = true)
    b.finUnit(glossLaminationId, "0.03", bySides = true)
    b.finUnit(softTouchCoatingId, "0.05", bySides = true)
    b.finUnit(embossingId, "0.08")
    b.finUnit(debossingId, "0.08")
    b.finUnit(foilStampingId, "0.15")
    b.finUnit(dieCutId, "0.10")
    b.finUnit(kissCutId, "0.05")
    b.add(
      PricingTarget.FinishRate(grommetsId),
      RateModel.ByDiscreteParam(
        Map(250 -> Money("3.20"), 300 -> Money("2.60"), 500 -> Money("1.75"), 1000 -> Money("1.10")),
        scaleByArea = true,
      ),
    )
    b.add(PricingTarget.FinishRate(gumRopeId), RateModel.PerLinearMeter(Money("0.80")))
    b.add(
      PricingTarget.FinishRate(scoringId),
      RateModel.ByDiscreteParam(
        Map(
          1 -> Money("0.03"), 2 -> Money("0.05"), 3 -> Money("0.06"), 4 -> Money("0.07"),
          5 -> Money("0.08"), 6 -> Money("0.09"), 7 -> Money("0.10"), 8 -> Money("0.11"),
        ),
        scaleByArea = false,
      ),
    )

    // Finish type fallbacks
    b.finTypeUnit("UVCoating", "0.04", bySides = true)
    b.finTypeUnit("AqueousCoating", "0.02", bySides = true)
    b.finTypeUnit("Varnish", "0.06", bySides = true)
    b.finTypeUnit("Perforation", "0.02")
    b.finTypeUnit("RoundCorners", "0.02")
    b.finTypeArea("Overlamination", "2.50")

    // Promotional finishes
    b.finUnit(heatPressId, "0.40")
    b.finUnit(labelPrintId, "0.15")
    b.finUnit(foldBagId, "0.10")
    b.finUnit(mylarOverlayId, "0.05")
    b.finUnit(safetyPinId, "0.03")
    b.finUnit(magnetBackId, "0.12")
    b.finUnit(bottleOpenerId, "0.25")
    b.finUnit(dishwasherCoatId, "0.20")
    b.finUnit(giftBoxId, "1.40")
    b.finUnit(glossyGlazeId, "0.15")
    b.finUnit(embroideryId, "0.80")
    b.finUnit(reinforcedHandlesId, "0.10")

    // Folds
    b.add(PricingTarget.FoldRate("Half"), RateModel.PerUnit(Money("0.02")))
    b.add(PricingTarget.FoldRate("Tri"), RateModel.PerUnit(Money("0.03")))
    b.add(PricingTarget.FoldRate("Gate"), RateModel.PerUnit(Money("0.04")))
    b.add(PricingTarget.FoldRate("Accordion"), RateModel.PerUnit(Money("0.04")))
    b.add(PricingTarget.FoldRate("ZFold"), RateModel.PerUnit(Money("0.03")))
    b.add(PricingTarget.FoldRate("RollFold"), RateModel.PerUnit(Money("0.04")))
    b.add(PricingTarget.FoldRate("FrenchFold"), RateModel.PerUnit(Money("0.04")))
    b.add(PricingTarget.FoldRate("CrossFold"), RateModel.PerUnit(Money("0.05")))

    // Bindings
    b.add(PricingTarget.BindingRate("SaddleStitch"), RateModel.PerUnit(Money("0.05")))
    b.add(PricingTarget.BindingRate("PerfectBinding"), RateModel.PerUnit(Money("0.12")))
    b.add(PricingTarget.BindingRate("SpiralBinding"), RateModel.PerUnit(Money("0.20")))
    b.add(PricingTarget.BindingRate("WireOBinding"), RateModel.PerUnit(Money("0.25")))
    b.add(PricingTarget.BindingRate("CaseBinding"), RateModel.PerUnit(Money("0.60")))

    // Process & category surcharges
    b.add(PricingTarget.PrintingProcessRate("Letterpress"), RateModel.PerUnit(Money("0.20")))
    b.add(PricingTarget.PrintingProcessRate("ScreenPrint"), RateModel.PerUnit(Money("0.15")))
    b.add(PricingTarget.CategoryRate(tshirtsId), RateModel.PerUnit(Money("0.60")))
    b.add(PricingTarget.CategoryRate(ecoBagsId), RateModel.PerUnit(Money("0.30")))
    b.add(PricingTarget.CategoryRate(cupsId), RateModel.PerUnit(Money("0.50")))

    // Ink rates
    b.inkRates(offsetId, "0.04", "0.02", "0.03", "0.005", "0.008")
    b.inkRates(digitalId, "0.04", "0.02", "0.03", "0.005", "0.008", r4W0 = Some("0.04"))
    b.inkRates(letterpressId, "0.04", "0.02", "0.03", "0.005", "0.008", r20 = Some("0.02"))
    b.inkRates(uvInkjetId, "1.80", "0.90", "1.20", "0.25", "0.40", r4W0 = Some("1.35"), asArea = true)
    b.inkRates(uvInkjetId, "1.30", "0.75", "1.00", "0.22", "0.38", r4W0 = Some("1.05"), scope = PricingScope.forCategory(cupsId))
    b.inkRates(solventInkjetId, "24.00", "13.00", "17.00", "3.50", "6.00", asArea = true)
    b.inkRates(epson8ColorId, "34.00", "18.00", "24.00", "5.00", "8.00", r4W0 = Some("22.00"), asArea = true)
    b.inkRates(screenPrintId, "0.06", "0.04", "0.05", "0.01", "0.015", r4W0 = Some("0.06"))
    b.inkRates(dtgId, "0.04", "0.02", "0.03", "0.005", "0.008", r4W0 = Some("0.05"))
    b.inkRates(sublimationId, "0.04", "0.02", "0.03", "0.005", "0.008")

    // One-time setup fees
    b.setup(PricingTarget.PrintingMethodRate(offsetId), "15.00")
    b.setup(PricingTarget.PrintingMethodRate(digitalId), "10.00")
    b.setup(PricingTarget.PrintingMethodRate(uvInkjetId), "10.00")
    b.setup(PricingTarget.PrintingMethodRate(letterpressId), "25.00")
    b.setup(PricingTarget.PrintingMethodRate(solventInkjetId), "20.00")
    b.setup(PricingTarget.PrintingMethodRate(epson8ColorId), "20.00")
    b.setup(PricingTarget.PrintingMethodRate(screenPrintId), "20.00")
    b.setup(PricingTarget.PrintingMethodRate(dtgId), "10.00")
    b.setup(PricingTarget.PrintingMethodRate(sublimationId), "10.00")
    b.setup(PricingTarget.FinishRate(scoringId), "2.50")
    b.setup(PricingTarget.VersionChange, "10.00")

    Pricelist(
      id = pricelistUsdId,
      version = Version.unsafe("2.0.0-usd"),
      currency = Currency.USD,
      entries = b.result,
      volumeCurves = List(unitVolumeCurve),
      speedPolicies = standardSpeedPolicies,
      minimumOrderNet = Some(Money("10.00")),
      taxPolicy = TaxPolicy(defaultRate = TaxRate.zero),
    )

  // ═══════════════════════════════════════════════════════════════════════════
  // 2. Shared CZK entries helper (used by both `pricelistCzk` and `pricelistCzkSheet`)
  // ═══════════════════════════════════════════════════════════════════════════

  private def addSharedCzkNonPaperEntries(b: EntryBuilder): Unit =
    // Wide-format vinyl & tiered PVC 510g banner
    b.matArea(vinylId, "375")
    b.matArea(clearVinylId, "475")
    b.add(
      PricingTarget.MaterialRate(pvc510gId),
      RateModel.TieredArea(
        tiers = NonEmptyChunk(
          AreaTier(SquareMeters.unsafe(BigDecimal("0")), Money("555")),
          AreaTier(SquareMeters.unsafe(BigDecimal("2")), Money("455")),
          AreaTier(SquareMeters.unsafe(BigDecimal("5")), Money("405")),
          AreaTier(SquareMeters.unsafe(BigDecimal("10")), Money("355")),
        ),
        basis = AreaTierBasis.TotalJobArea,
      ),
    )

    // Roll-Up
    b.matArea(rollUpBannerFilmId, "235")
    b.matUnit(rollUpStandEconomyId, "590")
    b.matUnit(rollUpStandPremiumId, "1290")

    // Corrugated cardboard (not SRA3 sheet-fed)
    b.matUnit(corrugatedId, "2")

    // Promotional blanks (CZK per unit)
    b.matUnit(cottonTshirt150Id, "75")
    b.matUnit(cottonTshirt180Id, "85")
    b.matUnit(polyesterTshirtId, "65")
    b.matUnit(cottonPolyBlendId, "70")
    b.matUnit(organicCottonTshirtId, "105")

    b.matUnit(cottonCanvasBagId, "52")
    b.matUnit(organicCottonBagId, "65")
    b.matUnit(recycledPetBagId, "42")
    b.matUnit(juteBagId, "58")
    b.matUnit(nonWovenPpBagId, "14")

    b.matUnit(tinplateBadgeId, "8")
    b.matUnit(acrylicBadgeId, "11")
    b.matUnit(woodenBadgeId, "13")

    b.matUnit(ceramicMugWhiteId, "45")
    b.matUnit(ceramicMugColoredId, "52")
    b.matUnit(magicMugId, "82")
    b.matUnit(stainlessTravelMugId, "130")
    b.matUnit(enamelMugId, "70")
    b.matUnit(glassMugId, "58")

    // Finishes common across CZK pricelists
    b.finUnit(embossingId, "2")
    b.finUnit(debossingId, "2")
    b.finUnit(foilStampingId, "3.50")
    b.finUnit(dieCutId, "3")
    b.finUnit(kissCutId, "2")

    // Grommets: area-based pricing driven by spacing mm
    b.add(
      PricingTarget.FinishRate(grommetsId),
      RateModel.ByDiscreteParam(
        Map(250 -> Money("75"), 300 -> Money("60"), 500 -> Money("40"), 1000 -> Money("25")),
        scaleByArea = true,
      ),
    )

    // Gum rope: linear-meter pricing
    b.add(PricingTarget.FinishRate(gumRopeId), RateModel.PerLinearMeter(Money("18")))

    // Scoring count surcharges (1..8 creases)
    b.add(
      PricingTarget.FinishRate(scoringId),
      RateModel.ByDiscreteParam(
        Map(
          1 -> Money("0.60"), 2 -> Money("1.00"), 3 -> Money("1.30"), 4 -> Money("1.50"),
          5 -> Money("1.70"), 6 -> Money("1.90"), 7 -> Money("2.10"), 8 -> Money("2.30"),
        ),
        scaleByArea = false,
      ),
    )

    // Type-level finish fallbacks
    b.finTypeUnit("UVCoating", "1", bySides = true)
    b.finTypeUnit("AqueousCoating", "0.50", bySides = true)
    b.finTypeUnit("Varnish", "1.50", bySides = true)
    b.finTypeUnit("RoundCorners", "0.50")
    b.finTypeUnit("Perforation", "0.50")
    b.finTypeArea("Overlamination", "60")

    // Promotional finishes
    b.finUnit(heatPressId, "10")
    b.finUnit(labelPrintId, "4")
    b.finUnit(foldBagId, "3")
    b.finUnit(mylarOverlayId, "1.50")
    b.finUnit(safetyPinId, "1")
    b.finUnit(magnetBackId, "3")
    b.finUnit(bottleOpenerId, "6")
    b.finUnit(dishwasherCoatId, "5")
    b.finUnit(giftBoxId, "35")
    b.finUnit(glossyGlazeId, "4")
    b.finUnit(embroideryId, "20")
    b.finUnit(reinforcedHandlesId, "3")

    // Folds
    b.add(PricingTarget.FoldRate("Half"), RateModel.PerUnit(Money("1.00")))
    b.add(PricingTarget.FoldRate("Tri"), RateModel.PerUnit(Money("1.50")))
    b.add(PricingTarget.FoldRate("Gate"), RateModel.PerUnit(Money("1.50")))
    b.add(PricingTarget.FoldRate("Accordion"), RateModel.PerUnit(Money("1.50")))
    b.add(PricingTarget.FoldRate("ZFold"), RateModel.PerUnit(Money("1.50")))
    b.add(PricingTarget.FoldRate("RollFold"), RateModel.PerUnit(Money("1.50")))
    b.add(PricingTarget.FoldRate("FrenchFold"), RateModel.PerUnit(Money("1.50")))
    b.add(PricingTarget.FoldRate("CrossFold"), RateModel.PerUnit(Money("2.00")))

    // Bindings
    b.add(PricingTarget.BindingRate("SaddleStitch"), RateModel.PerUnit(Money("1")))
    b.add(PricingTarget.BindingRate("PerfectBinding"), RateModel.PerUnit(Money("5")))
    b.add(PricingTarget.BindingRate("SpiralBinding"), RateModel.PerUnit(Money("15")))
    b.add(PricingTarget.BindingRate("WireOBinding"), RateModel.PerUnit(Money("18")))
    b.add(PricingTarget.BindingRate("CaseBinding"), RateModel.PerUnit(Money("25")))

    // Process & category surcharges
    b.add(PricingTarget.PrintingProcessRate("Letterpress"), RateModel.PerUnit(Money("5")))
    b.add(PricingTarget.PrintingProcessRate("ScreenPrint"), RateModel.PerUnit(Money("4")))
    b.add(PricingTarget.CategoryRate(tshirtsId), RateModel.PerUnit(Money("15")))
    b.add(PricingTarget.CategoryRate(ecoBagsId), RateModel.PerUnit(Money("8")))
    b.add(PricingTarget.CategoryRate(cupsId), RateModel.PerUnit(Money("12")))

    // Large-format & promotional inks
    b.inkRates(uvInkjetId, "720", "360", "480", "100", "160", r4W0 = Some("480"), asArea = true)
    b.inkRates(uvInkjetId, "35", "20", "27", "6", "10", r4W0 = Some("28"), scope = PricingScope.forCategory(cupsId))
    b.inkRates(solventInkjetId, "600", "300", "400", "80", "140", asArea = true)
    b.inkRates(epson8ColorId, "840", "420", "560", "110", "190", r4W0 = Some("540"), asArea = true)
    b.inkRates(screenPrintId, "4", "2", "3", "0.60", "1", r4W0 = Some("3.50"))
    b.inkRates(dtgId, "6", "3", "4", "1", "1.80", r4W0 = Some("5"))
    b.inkRates(sublimationId, "5", "2.50", "3.50", "0.80", "1.50")

    // One-time setup fees
    b.setup(PricingTarget.PrintingMethodRate(offsetId), "350")
    b.setup(PricingTarget.PrintingMethodRate(digitalId), "200")
    b.setup(PricingTarget.PrintingMethodRate(uvInkjetId), "200")
    b.setup(PricingTarget.PrintingMethodRate(letterpressId), "600")
    b.setup(PricingTarget.PrintingMethodRate(solventInkjetId), "400")
    b.setup(PricingTarget.PrintingMethodRate(epson8ColorId), "400")
    b.setup(PricingTarget.PrintingMethodRate(screenPrintId), "400")
    b.setup(PricingTarget.PrintingMethodRate(dtgId), "200")
    b.setup(PricingTarget.PrintingMethodRate(sublimationId), "200")

    b.setup(PricingTarget.FinishRate(matteLaminationId), "300")
    b.setup(PricingTarget.FinishRate(glossLaminationId), "300")
    b.setup(PricingTarget.FinishRate(softTouchCoatingId), "300")
    b.setup(PricingTarget.FinishRate(uvCoatingId), "300")
    b.setup(PricingTarget.FinishRate(varnishId), "300")
    b.setup(PricingTarget.FinishRate(embossingId), "350")
    b.setup(PricingTarget.FinishRate(debossingId), "350")
    b.setup(PricingTarget.FinishRate(foilStampingId), "450")
    b.setup(PricingTarget.FinishRate(dieCutId), "600")
    b.setup(PricingTarget.FinishRate(kissCutId), "200")
    b.setup(PricingTarget.FinishRate(scoringId), "60")
    b.setup(PricingTarget.FinishTypeRate("Perforation"), "60")
    b.setup(PricingTarget.FinishTypeRate("RoundCorners"), "40")
    b.setup(PricingTarget.FinishRate(heatPressId), "200")
    b.setup(PricingTarget.FinishRate(embroideryId), "500")

    b.setup(PricingTarget.FoldRate("Half"), "80")
    b.setup(PricingTarget.FoldRate("Tri"), "100")
    b.setup(PricingTarget.FoldRate("Gate"), "120")
    b.setup(PricingTarget.FoldRate("Accordion"), "120")
    b.setup(PricingTarget.FoldRate("ZFold"), "100")
    b.setup(PricingTarget.FoldRate("RollFold"), "120")
    b.setup(PricingTarget.FoldRate("FrenchFold"), "120")
    b.setup(PricingTarget.FoldRate("CrossFold"), "150")

    b.setup(PricingTarget.BindingRate("SaddleStitch"), "50")
    b.setup(PricingTarget.BindingRate("PerfectBinding"), "150")
    b.setup(PricingTarget.BindingRate("SpiralBinding"), "100")
    b.setup(PricingTarget.BindingRate("WireOBinding"), "100")
    b.setup(PricingTarget.BindingRate("CaseBinding"), "400")

    b.setup(PricingTarget.VersionChange, "150")

  // ═══════════════════════════════════════════════════════════════════════════
  // 2a. CZK Base Unit Pricelist (21% Czech VAT)
  // ═══════════════════════════════════════════════════════════════════════════

  val pricelistCzk: Pricelist =
    val b = new EntryBuilder("czk-unit")

    // Papers (per unit)
    b.matUnit(coatedGlossy90gsmId, "9")
    b.matUnit(coatedGlossy115gsmId, "9")
    b.matUnit(coatedGlossy130gsmId, "9")
    b.matUnit(coatedGlossy150gsmId, "10")
    b.matUnit(coatedGlossy170gsmId, "10")
    b.matUnit(coatedGlossy200gsmId, "11")
    b.matUnit(coatedGlossy250gsmId, "11")
    b.matUnit(coated300gsmId, "12")
    b.matUnit(coatedGlossy350gsmId, "12")

    b.matUnit(coatedMatte90gsmId, "9")
    b.matUnit(coatedMatte115gsmId, "9")
    b.matUnit(coatedMatte130gsmId, "9")
    b.matUnit(coatedMatte150gsmId, "10")
    b.matUnit(coatedMatte170gsmId, "10")
    b.matUnit(coatedMatte200gsmId, "11")
    b.matUnit(coatedMatte250gsmId, "11")
    b.matUnit(coatedMatte300gsmId, "12")
    b.matUnit(coatedMatte350gsmId, "12")

    b.matUnit(uncoatedBondId, "4")
    b.matUnit(kraftId, "10")
    b.matUnit(coatedSilk250gsmId, "11")
    b.matUnit(yupoId, "14")
    b.matUnit(adhesiveStockId, "10")
    b.matUnit(cottonId, "15")

    // Unit lamination rates
    b.finUnit(matteLaminationId, "1.50", bySides = true)
    b.finUnit(glossLaminationId, "1.50", bySides = true)
    b.finUnit(softTouchCoatingId, "2.50", bySides = true)

    // Sheet-fed press inks (per unit)
    b.inkRates(offsetId, "4", "2", "2.50", "0.60", "1")
    b.inkRates(digitalId, "4", "2", "2.50", "0.60", "1", r4W0 = Some("4"))
    b.inkRates(letterpressId, "4", "2", "2.50", "0.60", "1", r20 = Some("2"))

    addSharedCzkNonPaperEntries(b)

    Pricelist(
      id = pricelistCzkId,
      version = Version.unsafe("2.0.0-czk-unit"),
      currency = Currency.CZK,
      entries = b.result,
      volumeCurves = List(unitVolumeCurve),
      speedPolicies = standardSpeedPolicies,
      minimumOrderNet = Some(Money("200")),
      taxPolicy = TaxPolicy(defaultRate = TaxRate.czStandard),
    )

  // ═══════════════════════════════════════════════════════════════════════════
  // 2b. CZK SRA3 Sheet-Based Pricelist (21% Czech VAT, Imposition & Cutting!)
  // ═══════════════════════════════════════════════════════════════════════════

  val pricelistCzkSheet: Pricelist =
    val b = new EntryBuilder("czk-sheet")

    // Light & medium papers (90–200gsm): 5–10 CZK per SRA3 sheet
    b.matSheet(coatedGlossy90gsmId, "5")
    b.matSheet(coatedGlossy115gsmId, "6")
    b.matSheet(coatedGlossy130gsmId, "6")
    b.matSheet(coatedGlossy150gsmId, "7")
    b.matSheet(coatedGlossy170gsmId, "8")
    b.matSheet(coatedGlossy200gsmId, "10")
    b.matSheet(coatedGlossy250gsmId, "12")
    b.matSheet(coated300gsmId, "14")
    b.matSheet(coatedGlossy350gsmId, "16")

    b.matSheet(coatedMatte90gsmId, "5")
    b.matSheet(coatedMatte115gsmId, "6")
    b.matSheet(coatedMatte130gsmId, "6")
    b.matSheet(coatedMatte150gsmId, "7")
    b.matSheet(coatedMatte170gsmId, "8")
    b.matSheet(coatedMatte200gsmId, "10")
    b.matSheet(coatedMatte250gsmId, "12")
    b.matSheet(coatedMatte300gsmId, "14")
    b.matSheet(coatedMatte350gsmId, "16")

    b.matSheet(uncoatedBondId, "4")
    b.matSheet(coatedSilk250gsmId, "12")
    b.matSheet(kraftId, "12")
    b.matSheet(cottonId, "16")
    b.matSheet(yupoId, "16")
    b.matSheet(adhesiveStockId, "12")

    // Guillotine cutting per sheet
    b.add(PricingTarget.GuillotineCutting, RateModel.PerUnit(Money("0.10")))

    // Sheet-level lamination (more coverage per SRA3 press sheet)
    b.finSheet(matteLaminationId, "6", bySides = true)
    b.finSheet(glossLaminationId, "6", bySides = true)
    b.finSheet(softTouchCoatingId, "9", bySides = true)

    // Sheet-fed press inks (per SRA3 press sheet!)
    b.inkRates(offsetId, "4", "2", "2.50", "0.60", "1", asSheet = true)
    b.inkRates(digitalId, "4", "2", "2.50", "0.60", "1", r4W0 = Some("4"), asSheet = true)
    b.inkRates(letterpressId, "4", "2", "2.50", "0.60", "1", r20 = Some("2"), asSheet = true)

    addSharedCzkNonPaperEntries(b)

    Pricelist(
      id = pricelistCzkSheetId,
      version = Version.unsafe("2.0.0-czk-sheet"),
      currency = Currency.CZK,
      entries = b.result,
      // Sheet jobs use TotalPressSheets curve; roll/discrete jobs seamlessly fall back to FinishedUnits!
      volumeCurves = List(sheetVolumeCurve, unitVolumeCurve),
      speedPolicies = standardSpeedPolicies,
      minimumOrderNet = Some(Money("200")),
      taxPolicy = TaxPolicy(defaultRate = TaxRate.czStandard),
    )
