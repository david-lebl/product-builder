package mpbuilder.pricing

import mpbuilder.pricing.core.*
import mpbuilder.shared.*
import zio.*
import zio.test.*
import java.time.Instant

object PricingDomainSpec extends ZIOSpecDefault:

  private val now = Instant.parse("2026-03-10T12:00:00Z")

  private val flyerCatId   = CategoryId.unsafe("flyers")
  private val bannerCatId  = CategoryId.unsafe("banners")
  private val art300Id     = MaterialId.unsafe("art-300")
  private val bannerMatId  = MaterialId.unsafe("frontlit-510")
  private val digitalId    = PrintingMethodId.unsafe("digital")
  private val offsetId     = PrintingMethodId.unsafe("offset")
  private val uvInkjetId   = PrintingMethodId.unsafe("uv-inkjet")
  private val scoringId    = FinishId.unsafe("scoring")
  private val grommetsId   = FinishId.unsafe("grommets")
  private val hemId        = FinishId.unsafe("hem")
  private val matteLamId   = FinishId.unsafe("matte-lam")

  private val basePricelist = Pricelist(
    id = PricelistId.unsafe("czk-2026"),
    version = Version.unsafe("2026.1"),
    currency = Currency.CZK,
    entries = List(
      // Sheet material per press sheet
      PriceEntry(
        PriceEntryId.unsafe("mat-art300"),
        PricingTarget.MaterialRate(art300Id),
        rate = RateModel.PerSheet(Money("6.00")),
      ),
      // Banner material: TieredArea using TotalJobArea!
      PriceEntry(
        PriceEntryId.unsafe("mat-banner"),
        PricingTarget.MaterialRate(bannerMatId),
        rate = RateModel.TieredArea(
          NonEmptyChunk(
            AreaTier(SquareMeters.unsafe(BigDecimal(0)), Money("400.00")),
            AreaTier(SquareMeters.unsafe(BigDecimal(10)), Money("300.00")),
          ),
          basis = AreaTierBasis.TotalJobArea,
        ),
      ),
      // Distinct ink rates: 4+W/0 vs 4/1 vs 4/4
      PriceEntry(
        PriceEntryId.unsafe("ink-uv-4w0"),
        PricingTarget.InkRate(uvInkjetId, InkBillingKey("4+W", "0")),
        rate = RateModel.PerSqMeter(Money("150.00")),
      ),
      PriceEntry(
        PriceEntryId.unsafe("ink-uv-41"),
        PricingTarget.InkRate(uvInkjetId, InkBillingKey("4", "1")),
        rate = RateModel.PerSqMeter(Money("90.00")),
      ),
      PriceEntry(
        PriceEntryId.unsafe("ink-dig-44"),
        PricingTarget.InkRate(digitalId, InkBillingKey("4", "4")),
        rate = RateModel.PerSheet(Money("4.00")),
      ),
      PriceEntry(
        PriceEntryId.unsafe("ink-off-44"),
        PricingTarget.InkRate(offsetId, InkBillingKey("4", "4")),
        rate = RateModel.PerSheet(Money("2.00")),
      ),
      // Offset plate setup fee (OneTimeSetup — NOT discounted by volume or speed!)
      PriceEntry(
        PriceEntryId.unsafe("setup-offset"),
        PricingTarget.PrintingMethodRate(offsetId),
        phase = ChargePhase.OneTimeSetup,
        rate = RateModel.FlatFee(Money("1200.00")),
      ),
      // Scoring finish: ByDiscreteParam (1 crease vs 2 creases) per unit
      PriceEntry(
        PriceEntryId.unsafe("fin-scoring"),
        PricingTarget.FinishRate(scoringId),
        rate = RateModel.ByDiscreteParam(Map(1 -> Money("1.00"), 2 -> Money("1.80"))),
      ),
      // Grommets finish: ByDiscreteParam (spacing mm) scaled by area
      PriceEntry(
        PriceEntryId.unsafe("fin-grommets"),
        PricingTarget.FinishRate(grommetsId),
        rate = RateModel.ByDiscreteParam(Map(250 -> Money("60.00"), 500 -> Money("30.00")), scaleByArea = true),
      ),
      // Hem finish: PerPerimeterMeter
      PriceEntry(
        PriceEntryId.unsafe("fin-hem"),
        PricingTarget.FinishRate(hemId),
        rate = RateModel.PerPerimeterMeter(Money("20.00")),
      ),
      // Lamination: PerSheet multiplied by sides
      PriceEntry(
        PriceEntryId.unsafe("fin-lam"),
        PricingTarget.FinishRate(matteLamId, multiplyBySides = true),
        rate = RateModel.PerSheet(Money("5.00")),
      ),
      // Version change fee (OneTimeSetup per extra artwork version)
      PriceEntry(
        PriceEntryId.unsafe("ver-change"),
        PricingTarget.VersionChange,
        phase = ChargePhase.OneTimeSetup,
        rate = RateModel.PerUnit(Money("150.00")),
      ),
    ),
    volumeCurves = List(
      // Global curve
      VolumeCurve(
        scope = PricingScope.global,
        basis = VolumeBasis.FinishedUnits,
        tiers = NonEmptyChunk(
          VolumeTier(BigDecimal(1), Some(BigDecimal(499)), BigDecimal("1.00")),
          VolumeTier(BigDecimal(500), None, BigDecimal("0.85")),
        ),
      ),
      // Method-scoped curve for Offset (more aggressive volume discount than Digital!)
      VolumeCurve(
        scope = PricingScope.forMethod(offsetId),
        basis = VolumeBasis.FinishedUnits,
        tiers = NonEmptyChunk(
          VolumeTier(BigDecimal(1), Some(BigDecimal(499)), BigDecimal("1.00")),
          VolumeTier(BigDecimal(500), None, BigDecimal("0.60")),
        ),
      ),
    ),
    speedPolicies = Map(
      ManufacturingSpeed.Standard -> SpeedSurchargePolicy(ManufacturingSpeed.Standard, BigDecimal("1.00"), subjectToSurge = false),
      ManufacturingSpeed.Economy  -> SpeedSurchargePolicy(ManufacturingSpeed.Economy, BigDecimal("0.90"), subjectToSurge = false),
      ManufacturingSpeed.Express  -> SpeedSurchargePolicy(
        ManufacturingSpeed.Express,
        baseMultiplier = BigDecimal("1.35"),
        queueThresholds = List(QueueThreshold(BigDecimal("0.80"), BigDecimal("0.20"))),
        subjectToSurge = true,
      ),
    ),
    minimumOrderNet = Some(Money("250.00")),
    taxPolicy = TaxPolicy(defaultRate = TaxRate.czStandard),
  )

  private def bannerSubject(qty: Int, ink: InkBillingKey): PricingSubject =
    PricingSubject(
      categoryId = bannerCatId,
      quantity = Quantity.unsafe(qty),
      components = NonEmptyChunk(
        PricingComponent(
          key = ComponentKey.Main,
          materialId = bannerMatId,
          stockFormatId = None,
          printingMethodId = Some(uvInkjetId),
          printingProcessKey = Some("UVCurableInkjet"),
          inkKey = Some(ink),
          unitsPerProduct = 1,
          totalFlatUnits = qty,
          sheetsUsed = 0,
          guillotineCutsPerSheet = 0,
          areaPerUnitSqM = SquareMeters.unsafe(BigDecimal("1.50")), // 1.5 m² each
          perimeterPerUnitM = BigDecimal("5.00"),                   // 5 m perimeter
          finishes = List(
            PricingFinish(grommetsId, "Grommets", discreteParam = Some(500)),
            PricingFinish(hemId, "Hem"),
          ),
        )
      ),
    )

  def spec = suite("PricingDomainSpec")(
    test("TieredArea evaluates TotalJobArea (10 × 1.5 m² = 15 m² hits 10 m²+ tier) and prices 4+W/0 distinctly from 4/1") {
      val sWhite = bannerSubject(qty = 10, ink = InkBillingKey("4+W", "0"))
      val s41    = bannerSubject(qty = 10, ink = InkBillingKey("4", "1"))
      val qWhite = PricingEngine.calculate(QuoteId.unsafe("q-w"), sWhite, basePricelist, DynamicPricingContext.default, None, None, None, now).toEither.toOption.get
      val q41    = PricingEngine.calculate(QuoteId.unsafe("q-41"), s41, basePricelist, DynamicPricingContext.default, None, None, None, now).toEither.toOption.get

      // 15 m² @ 300 CZK/m² (hit the >=10 m² tier!) = 4500 CZK material
      // 4+W/0 ink: 15 m² @ 150 = 2250 vs 4/1 ink: 15 m² @ 90 = 1350 -> diff = 900 CZK
      // Grommets (spacing 500, scaleByArea=true): 15 m² @ 30 = 450 CZK
      // Hem (5m perimeter × 10 = 50m @ 20): 1000 CZK
      assertTrue(
        qWhite.componentQuotes.head.materialLine.total == Money("4500.00"),
        qWhite.componentQuotes.head.inkLine.map(_.total) == Some(Money("2250.00")),
        q41.componentQuotes.head.inkLine.map(_.total) == Some(Money("1350.00")),
        qWhite.netTotal - q41.netTotal == Money("900.00"),
        qWhite.tax.taxAmount == TaxRate.czStandard.taxOnNet(qWhite.netTotal),
        qWhite.grossTotal == qWhite.netTotal + qWhite.tax.taxAmount,
      )
    },
    test("CustomerPriceAgreement transforms TieredArea & ByDiscreteParam uniformly via RateModel.mapMoney while leaving OneTimeSetup untouched") {
      val agreement = CustomerPriceAgreement(
        target = AgreementTarget.SpecificCustomer(CustomerId.unsafe("cust-vip")),
        currency = Currency.CZK,
        globalDiscount = Some(Percentage.unsafe(BigDecimal(10))), // 10% off all variable rates
      )
      val subj = bannerSubject(qty = 10, ink = InkBillingKey("4+W", "0"))
      val baseQ = PricingEngine.calculate(QuoteId.unsafe("q-base"), subj, basePricelist, DynamicPricingContext.default, None, None, None, now).toEither.toOption.get
      val vipQ  = PricingEngine.calculate(QuoteId.unsafe("q-vip"), subj, basePricelist, DynamicPricingContext.default, Some(agreement), None, None, now).toEither.toOption.get

      // Material (TieredArea 300 -> 270 -> 4050), Grommets (ByDiscreteParam 30 -> 27 -> 405)
      assertTrue(
        vipQ.componentQuotes.head.materialLine.total == Money("4050.00"),
        vipQ.componentQuotes.head.finishLines.find(_.kind == ChargeKind.FinishCharge(grommetsId, "Grommets")).map(_.total) == Some(Money("405.00")),
        vipQ.netTotal < baseQ.netTotal,
      )
    },
    test("scoped VolumeCurves differentiate Offset vs Digital, and VersionCount adds setup fees without losing run volume tier") {
      def flyerSubj(methodId: PrintingMethodId, versions: Int) = PricingSubject(
        categoryId = flyerCatId,
        quantity = Quantity.unsafe(500),
        versionCount = VersionCount.unsafe(versions),
        components = NonEmptyChunk(
          PricingComponent(
            key = ComponentKey.Main,
            materialId = art300Id,
            stockFormatId = Some(StockFormatId.unsafe("sra3")),
            printingMethodId = Some(methodId),
            printingProcessKey = Some(if methodId == offsetId then "Offset" else "Digital"),
            inkKey = Some(InkBillingKey("4", "4")),
            unitsPerProduct = 1,
            totalFlatUnits = 500,
            sheetsUsed = 125, // 4-up A5 on SRA3
            guillotineCutsPerSheet = 3,
            areaPerUnitSqM = Dimension2D.A5.areaSqM,
            perimeterPerUnitM = Dimension2D.A5.perimeterMeters,
            finishes = List(
              PricingFinish(scoringId, "Scoring", discreteParam = Some(2)),
              PricingFinish(matteLamId, "Lamination", sideMultiplier = 2),
            ),
          )
        ),
      )

      val digQ = PricingEngine.calculate(QuoteId.unsafe("q-dig"), flyerSubj(digitalId, 5), basePricelist, DynamicPricingContext.default, None, None, None, now).toEither.toOption.get
      val offQ = PricingEngine.calculate(QuoteId.unsafe("q-off"), flyerSubj(offsetId, 5), basePricelist, DynamicPricingContext.default, None, None, None, now).toEither.toOption.get

      // 5 versions = 4 extra versions × 150 CZK = 600 CZK setup fee
      val digVersionFee = digQ.setupFees.find(_.kind == ChargeKind.VersionChangeCharge(4)).map(_.total)
      val offPlateFee   = offQ.setupFees.find(_.kind == ChargeKind.SetupFee(PricingTarget.PrintingMethodRate(offsetId))).map(_.total)

      assertTrue(
        digQ.volumeMultiplier == BigDecimal("0.85"), // global curve
        offQ.volumeMultiplier == BigDecimal("0.60"), // method-scoped Offset curve
        digVersionFee == Some(Money("600.00")),
        offPlateFee == Some(Money("1200.00")),
        // Lamination Both sides: 125 sheets × 2 sides × 5 CZK = 1250 CZK
        digQ.componentQuotes.head.finishLines.find(_.kind == ChargeKind.FinishCharge(matteLamId, "Lamination")).map(_.total) == Some(Money("1250.00")),
      )
    },
    test("dynamic Express surge respects cap, minimum order floor applies, and promo code validates constraints") {
      val smallSubj = PricingSubject(
        categoryId = flyerCatId,
        quantity = Quantity.unsafe(10),
        components = NonEmptyChunk(
          PricingComponent(
            key = ComponentKey.Main,
            materialId = art300Id,
            stockFormatId = None,
            printingMethodId = Some(digitalId),
            printingProcessKey = Some("Digital"),
            inkKey = Some(InkBillingKey("4", "4")),
            unitsPerProduct = 1,
            totalFlatUnits = 10,
            sheetsUsed = 5,
            guillotineCutsPerSheet = 2,
            areaPerUnitSqM = Dimension2D.A5.areaSqM,
            perimeterPerUnitM = Dimension2D.A5.perimeterMeters,
          )
        ),
        speed = ManufacturingSpeed.Express,
      )
      val busyCtx = DynamicPricingContext(
        globalUtilisation = BigDecimal("0.90"), // triggers +0.20
        busyPeriods = List(BusyPeriodMultiplier("Christmas", BigDecimal("0.80"))),
        expressMultiplierCap = BigDecimal("1.80"), // 1.35 + 0.20 + 0.80 = 2.35 -> capped to 1.80!
      )
      val promo = PromoCode(
        id = DiscountCodeId.unsafe("save10"),
        code = NonEmptyString.unsafe("SAVE10"),
        benefit = PromoBenefit.PercentageOff(Percentage.unsafe(BigDecimal(10))),
        constraints = PromoConstraints(validUntil = Some(now.plusSeconds(3600))),
        isActive = true,
      )
      val expiredPromo = promo.copy(constraints = PromoConstraints(validUntil = Some(now.minusSeconds(60))))
      val pCtx = PromoContext(now = now)

      val okQ  = PricingEngine.calculate(QuoteId.unsafe("q-min"), smallSubj, basePricelist, busyCtx, None, Some(promo), Some(pCtx), now).toEither.toOption.get
      val badQ = PricingEngine.calculate(QuoteId.unsafe("q-exp"), smallSubj, basePricelist, busyCtx, None, Some(expiredPromo), Some(pCtx), now)

      assertTrue(
        okQ.speedAdjustment.exists(_.kind == ChargeKind.SpeedAdjustment(ManufacturingSpeed.Express, BigDecimal("1.80"))),
        // Variable = 5*(6+4)=50, ×1.80 = 90 -> bumped to min floor 250.00 -> 10% promo = -25.00 -> net 225.00
        okQ.minimumAdjustment.isDefined,
        okQ.promoDiscount.map(_.total) == Some(Money("-25.00")),
        okQ.netTotal == Money("225.00"),
        badQ.isFailure,
      )
    },
    test("MarginAnalysis detects below-cost selling prices") {
      val subj = bannerSubject(qty = 10, ink = InkBillingKey("4", "1"))
      val q = PricingEngine.calculate(QuoteId.unsafe("q-m"), subj, basePricelist, DynamicPricingContext.default, None, None, None, now).toEither.toOption.get
      val expensiveCostSheet = ProductionCostSheet(
        currency = Currency.CZK,
        entries = List(
          CostEntry(PricingTarget.MaterialRate(bannerMatId), RateModel.PerSqMeter(Money("500.00"))),
        ),
        overheadFactor = BigDecimal("1.10"),
      )
      val analysis = PricingEngine.analyzeMargin(subj, q, expensiveCostSheet).toEither.toOption.get
      assertTrue(
        analysis.isBelowCost,
        analysis.marginNet.isNegative,
        analysis.warnings.exists(_.isInstanceOf[MarginWarning.BelowProductionCost]),
      )
    },
  )
