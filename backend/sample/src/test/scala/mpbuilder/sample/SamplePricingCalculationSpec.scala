package mpbuilder.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.sample.{CatalogSampleLayer, SampleCatalog}
import mpbuilder.customers.sample.SampleCustomers
import mpbuilder.pricing.*
import mpbuilder.pricing.sample.{PricingSampleLayer, SamplePricelist}
import mpbuilder.shared.*
import zio.*
import zio.test.*

object SamplePricingCalculationSpec extends ZIOSpecDefault:

  import SampleCatalog.*

  private def priceAllPresets(
      currency: Currency,
      useSheetCzk: Boolean,
  ): ZIO[Any, ShowcaseError, List[(CategoryId, PresetId, PriceQuote)]] =
    val layer = (EventBus.live >>> PricingSampleLayer.live(useSheetCzk = useSheetCzk)) ++ CatalogSampleLayer.live()
    (for
      catalog <- ZIO.serviceWithZIO[CatalogService](_.getCatalog)
      quotes  <- ZIO.foreach(catalog.categories.values.toList.sortBy(_.id.value)) { cat =>
                   ZIO.foreach(cat.presets) { preset =>
                     ShowcaseFacade
                       .pricePreset(cat.id, preset.id, currency)
                       .map(r => (cat.id, preset.id, r.quote))
                   }
                 }
    yield quotes.flatten).provideLayer(layer)

  override def spec = suite("SamplePricingCalculationSpec")(
    test("every preset across all 15 categories prices cleanly in USD, CZK unit, and CZK SRA3 sheet pricelists") {
      for
        usdQuotes      <- priceAllPresets(Currency.USD, useSheetCzk = false)
        czkUnitQuotes  <- priceAllPresets(Currency.CZK, useSheetCzk = false)
        czkSheetQuotes <- priceAllPresets(Currency.CZK, useSheetCzk = true)
      yield assertTrue(
        usdQuotes.size >= 20,
        czkUnitQuotes.size == usdQuotes.size,
        czkSheetQuotes.size == usdQuotes.size,
        usdQuotes.forall { case (_, _, q) => q.netTotal.isPositive && q.tax.taxAmount.isZero && q.grossTotal == q.netTotal },
        czkUnitQuotes.forall { case (_, _, q) => q.netTotal.isPositive && q.tax.rate == TaxRate.czStandard && q.grossTotal > q.netTotal },
        czkSheetQuotes.forall { case (_, _, q) => q.netTotal.isPositive && q.tax.rate == TaxRate.czStandard && q.grossTotal > q.netTotal },
      )
    },

    test("CZK SRA3 sheet pricelist computes exact imposition, guillotine cutting, setup fees, double-sided lamination, and VAT") {
      val layer = (EventBus.live >>> PricingSampleLayer.live(useSheetCzk = true)) ++ CatalogSampleLayer.live()
      val cmdBasic = ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe("cfg-bc-100"),
        categoryId = businessCardsId,
        components = NonEmptyChunk.single(
          ComponentSelection(ComponentKey.Main, coated300gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_0))
        ),
        specs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(85, 55)), Quantity.unsafe(100)),
      )
      val cmdLamFront = cmdBasic.copy(
        configurationId = ConfigurationId.unsafe("cfg-bc-lam1"),
        components = NonEmptyChunk.single(
          ComponentSelection(
            ComponentKey.Main,
            coated300gsmId,
            Some(digitalId),
            Some(InkConfiguration.cmyk4_0),
            List(FinishSelection(matteLaminationId, Some(FinishSide.Front))),
          )
        ),
      )
      val cmdLamBoth = cmdBasic.copy(
        configurationId = ConfigurationId.unsafe("cfg-bc-lam2"),
        components = NonEmptyChunk.single(
          ComponentSelection(
            ComponentKey.Main,
            coated300gsmId,
            Some(digitalId),
            Some(InkConfiguration.cmyk4_0),
            List(FinishSelection(matteLaminationId, Some(FinishSide.Both))),
          )
        ),
      )

      (for
        basic    <- ShowcaseFacade.configureAndQuote(cmdBasic, QuoteId.unsafe("q-basic"), Currency.CZK)
        lamFront <- ShowcaseFacade.configureAndQuote(cmdLamFront, QuoteId.unsafe("q-lam1"), Currency.CZK)
        lamBoth  <- ShowcaseFacade.configureAndQuote(cmdLamBoth, QuoteId.unsafe("q-lam2"), Currency.CZK)
      yield
        val cq     = basic.quote.componentQuotes.head
        val sheets = cq.sheetsUsed
        val lam1   = lamFront.quote.componentQuotes.head.finishLines.head.total
        val lam2   = lamBoth.quote.componentQuotes.head.finishLines.head.total
        assertTrue(
          // 85x55mm + 3mm bleed (91x61mm) with 2mm gutter on SRA3 (320x450mm):
          // 320x450: 3*7=21 or 5*4=20 -> 21 per sheet -> ceil(100/21) = 5 sheets!
          sheets == 5,
          cq.materialLine.total == Money("70.00"), // 5 sheets * 14 CZK
          cq.inkLine.exists(_.total == Money("10.00")), // 5 sheets * 2 CZK (4/0)
          cq.cuttingLine.exists(_.total == Money("10.00")), // 20 cuts/sheet * 0.10 * 5 sheets
          basic.quote.setupFees.exists(_.total == Money("200.00")), // digital setup fee
          basic.quote.netTotal == Money("290.00"), // 70 + 10 + 10 + 200
          basic.quote.tax.taxAmount == Money("60.90"), // 21% of 290
          basic.quote.grossTotal == Money("350.90"),
          // Double-sided lamination bills 2x single-sided lamination
          lam1 == Money("30.00"), // 5 sheets * 6 CZK
          lam2 == Money("60.00"), // 5 sheets * 12 CZK
        )
      ).provideLayer(layer)
    },

    test("PVC 510g banner uses TotalJobArea tiers, grommet spacing area rates, and gum rope linear meters") {
      val layer = (EventBus.live >>> PricingSampleLayer.live(useSheetCzk = true)) ++ CatalogSampleLayer.live()
      def bannerCmd(wMm: Int, hMm: Int, qty: Int, spacing: Int, ropeMeters: BigDecimal) =
        ConfigureProductCmd(
          configurationId = ConfigurationId.unsafe(s"cfg-ban-$wMm-$qty-$spacing"),
          categoryId = bannersId,
          components = NonEmptyChunk.single(
            ComponentSelection(
              ComponentKey.Main,
              pvc510gId,
              Some(solventInkjetId),
              Some(InkConfiguration.cmyk4_0),
              List(
                FinishSelection(grommetsId, params = Some(FinishParams.Grommets(spacing))),
                FinishSelection(gumRopeId, params = Some(FinishParams.LinearLength(ropeMeters))),
              ),
            )
          ),
          specs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(wMm, hMm)), Quantity.unsafe(qty)),
        )

      (for
        small500 <- ShowcaseFacade.configureAndQuote(bannerCmd(1000, 1000, 1, 500, BigDecimal(4)), QuoteId.unsafe("q-b1"), Currency.CZK)
        small300 <- ShowcaseFacade.configureAndQuote(bannerCmd(1000, 1000, 1, 300, BigDecimal(4)), QuoteId.unsafe("q-b2"), Currency.CZK)
        largeJob <- ShowcaseFacade.configureAndQuote(bannerCmd(4000, 1500, 2, 500, BigDecimal(6)), QuoteId.unsafe("q-b3"), Currency.CZK)
      yield
        val c1 = small500.quote.componentQuotes.head
        val c2 = small300.quote.componentQuotes.head
        val c3 = largeJob.quote.componentQuotes.head
        val ropeLine = c1.finishLines.find(_.kind == ChargeKind.FinishCharge(gumRopeId, "RopeAccessory")).get
        val grom500  = c1.finishLines.find(_.kind == ChargeKind.FinishCharge(grommetsId, "Grommets")).get
        val grom300  = c2.finishLines.find(_.kind == ChargeKind.FinishCharge(grommetsId, "Grommets")).get
        assertTrue(
          // 1 m² -> tier 0: 555 CZK/m²
          c1.materialLine.unitPrice == Money("555.00"),
          // 12 m² (4×1.5m × 2) -> tier >=10: 355 CZK/m²
          c3.materialLine.unitPrice == Money("355.00"),
          c3.materialLine.total == Money("4260.00"),
          // Grommets: 300mm (60 CZK/m²) > 500mm (40 CZK/m²)
          grom500.total == Money("40.00"),
          grom300.total == Money("60.00"),
          // Gum rope: 4m * 18 CZK/m = 72 CZK
          ropeLine.total == Money("72.00"),
        )
      ).provideLayer(layer)
    },

    test("Roll-Up bills graphic per m² and unprinted Stand per unit with zero ink; Cups UV ink uses category-scoped per-unit rate") {
      val layer = (EventBus.live >>> PricingSampleLayer.live(useSheetCzk = true)) ++ CatalogSampleLayer.live()
      (for
        rollupEcon <- ShowcaseFacade.pricePreset(rollUpsId, PresetId.unsafe("preset-rollup-economy-85x200"), Currency.CZK)
        rollupPrem <- ShowcaseFacade.pricePreset(rollUpsId, PresetId.unsafe("preset-rollup-premium-100x200"), Currency.CZK)
        travelMug  <- ShowcaseFacade.pricePreset(cupsId, PresetId.unsafe("preset-cup-travel-uv"), Currency.CZK)
      yield
        val mainComp  = rollupEcon.quote.componentQuotes.find(_.key == ComponentKey.Main).get
        val standComp = rollupEcon.quote.componentQuotes.find(_.key == ComponentKey.Stand).get
        val mugComp   = travelMug.quote.componentQuotes.head
        assertTrue(
          // 0.85 * 2.0 = 1.70 m² * 235 CZK/m² = 399.50 CZK
          mainComp.materialLine.total == Money("399.50"),
          mainComp.inkLine.isDefined,
          standComp.materialLine.total == Money("590.00"),
          standComp.inkLine.isEmpty,
          rollupPrem.quote.netTotal > rollupEcon.quote.netTotal,
          // Travel mug uses category-scoped UV flatbed per-unit 4+W/0 rate (28 CZK/unit × 20 = 560 CZK)
          mugComp.inkLine.exists(l => l.unitPrice == Money("28.00") && l.total == Money("560.00")),
        )
      ).provideLayer(layer)
    },

    test("B2B customer agreements, promo code constraints, Express dynamic surge, and MarginAnalysis work accurately") {
      val busyCtx = DynamicPricingContext(
        globalUtilisation = BigDecimal("0.75"), // triggers 0.50 (+0.10) and 0.70 (+0.15) on Express -> 1.35 + 0.25 = 1.60!
        busyPeriods = List(BusyPeriodMultiplier("Pre-Christmas", BigDecimal("0.10"))), // -> 1.70!
      )
      val layer = (EventBus.live >>> PricingSampleLayer.live(useSheetCzk = true, capacityContext = busyCtx)) ++ CatalogSampleLayer.live()

      val bcPresetId = PresetId.unsafe("preset-bc-premium")
      (for
        _        <- TestClock.setTime(java.time.Instant.parse("2026-06-01T10:00:00Z"))
        walkIn   <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK)
        megaCorp <- ShowcaseFacade.pricePreset(
                      businessCardsId,
                      bcPresetId,
                      Currency.CZK,
                      customerId = Some(SampleCustomers.megaCorpId),
                      customerTier = Some(CustomerTier.Platinum),
                      customerSegment = Some(CustomerSegment.Agency),
                    )
        withSave10   <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("SAVE10"))
        withFlat100  <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("FLAT100"))
        flat100Low   <- ShowcaseFacade.pricePreset(businessCardsId, PresetId.unsafe("preset-bc-basic"), Currency.CZK, promoCode = Some("FLAT100")).exit
        withCards    <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("CARDS10"))
        cardsOnFlyer <- ShowcaseFacade.pricePreset(flyersId, PresetId.unsafe("preset-flyers-standard"), Currency.CZK, promoCode = Some("CARDS10")).exit
        vipOk        <- ShowcaseFacade.pricePreset(
                          businessCardsId,
                          bcPresetId,
                          Currency.CZK,
                          customerId = Some(SampleCustomers.printShopProId),
                          promoCode = Some("VIP25"),
                        )
        vipDenied    <- ShowcaseFacade.pricePreset(
                          businessCardsId,
                          bcPresetId,
                          Currency.CZK,
                          customerId = Some(SampleCustomers.localPrintId),
                          promoCode = Some("VIP25"),
                        ).exit
        expiredRes   <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("OLDCODE")).exit
        futureRes    <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("UPCOMING")).exit
        usedUpRes    <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("USED100")).exit
        disabledRes  <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, promoCode = Some("DISABLED")).exit
        expressRes   <- ShowcaseFacade.pricePreset(businessCardsId, bcPresetId, Currency.CZK, speed = ManufacturingSpeed.Express)
        marginRes    <- {
          val subj = DomainBridges.toPricingSubject(walkIn.configuration.product, ManufacturingSpeed.Standard)
          ZIO.serviceWithZIO[PricingService](
            _.analyzeMargin(PriceQuoteCmd(QuoteId.unsafe("q-margin"), subj, Currency.CZK))
          )
        }
      yield assertTrue(
        megaCorp.quote.netTotal < walkIn.quote.netTotal,
        withSave10.quote.netTotal < walkIn.quote.netTotal,
        withFlat100.quote.promoDiscount.exists(_.total == Money("-100.00")),
        flat100Low.isFailure,
        withCards.quote.promoDiscount.isDefined,
        cardsOnFlyer.isFailure,
        vipOk.quote.promoDiscount.isDefined,
        vipDenied.isFailure,
        expiredRes.isFailure,
        futureRes.isFailure,
        usedUpRes.isFailure,
        disabledRes.isFailure,
        // Express surge: 1.35 base + 0.10 (>0.50) + 0.15 (>0.70) + 0.10 busy = 1.70
        expressRes.quote.speedAdjustment.exists(_.kind == ChargeKind.SpeedAdjustment(ManufacturingSpeed.Express, BigDecimal("1.70"))),
        expressRes.quote.netTotal > walkIn.quote.netTotal,
        !marginRes.isBelowCost,
        marginRes.marginNet.isPositive,
      )).provideLayer(layer)
    },
  )
