package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*

object PriceCalculatorSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val pricelistCzk = SamplePricelist.pricelistCzk
  private val configId = ConfigurationId.unsafe("test-pricing-1")

  private def makeConfig(
      category: ProductCategory,
      material: Material,
      printingMethod: PrintingMethod,
      inkConfig: InkConfiguration,
      finishes: List[SelectedFinish],
      specs: List[SpecValue],
  ): ProductConfiguration =
    ProductConfiguration(
      id = configId,
      category = category,
      printingMethod = printingMethod,
      components = List(ProductComponent(
        role = ComponentRole.Main,
        material = material,
        inkConfiguration = inkConfig,
        finishes = finishes,
        sheetCount = 1,
      )),
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  private def firstBreakdown(bd: PriceBreakdown): ComponentBreakdown =
    bd.componentBreakdowns.head

  def spec = suite("PriceCalculator")(
    suite("valid pricing")(
      test("business card with material + finish + quantity tier") {
        // 500× coated 300gsm + matte lamination + offset → 250-999 tier (0.90×)
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("0.12"),
          cb.materialLine.quantity == 500,
          cb.materialLine.lineTotal == Money("60.00"),
          cb.finishLines.size == 1,
          cb.finishLines.head.unitPrice == Money("0.03"),
          cb.finishLines.head.lineTotal == Money("15.00"),
          breakdown.subtotal == Money("75.00"),
          breakdown.quantityMultiplier == BigDecimal("0.90"),
          breakdown.total == Money("67.50"),
          breakdown.currency == Currency.USD,
        )
      },
      test("banner with area-based calculation") {
        // 10× vinyl 1000×500mm + UV coating + UV inkjet
        val config = makeConfig(
          category = SampleCatalog.banners,
          material = SampleCatalog.vinyl,
          printingMethod = SampleCatalog.uvInkjetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.uvCoating)),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("9.00"),
          cb.materialLine.lineTotal == Money("90.00"),
          cb.finishLines.size == 1,
          cb.finishLines.head.unitPrice == Money("0.04"),
          breakdown.subtotal == Money("90.40"),
          breakdown.quantityMultiplier == BigDecimal("1.0"),
          breakdown.total == Money("90.40"),
        )
      },
      test("quantity tier discount correctly applied") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(1000)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.subtotal == Money("120.00"),
          breakdown.quantityMultiplier == BigDecimal("0.80"),
          breakdown.total == Money("96.00"),
        )
      },
      test("multiple finish surcharges accumulated") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.embossing), SelectedFinish(SampleCatalog.foilStamping)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.finishLines.size == 2,
          breakdown.subtotal == Money("175.00"),
          breakdown.total == Money("157.50"),
        )
      },
      test("letterpress process surcharge applied") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.letterpressMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.processSurcharge.isDefined,
          breakdown.processSurcharge.get.unitPrice == Money("0.20"),
          breakdown.processSurcharge.get.lineTotal == Money("100.00"),
          breakdown.subtotal == Money("160.00"),
          breakdown.total == Money("144.00"),
        )
      },
      test("ID-level finish surcharge takes precedence over type-level") {
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("0.03")),
            PricingRule.FinishTypeSurcharge(FinishType.Lamination, Money("0.99")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )

        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.finishLines.head.unitPrice == Money("0.03"),
        )
      },
      test("no surcharge for finish with no pricing rule") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.roundCorners)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.finishLines.isEmpty,
          breakdown.subtotal == Money("60.00"),
        )
      },
      test("lamination both sides doubles the finish surcharge") {
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("0.05")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination, Some(FinishParameters.LaminationParams(FinishSide.Both)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.finishLines.size == 1,
          cb.finishLines.head.unitPrice == Money("0.10"),   // 0.05 × 2
          cb.finishLines.head.lineTotal == Money("10.00"),  // 0.10 × 100
        )
      },
      test("lamination front only keeps original surcharge (1×)") {
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("0.05")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination, Some(FinishParameters.LaminationParams(FinishSide.Front)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.finishLines.size == 1,
          cb.finishLines.head.unitPrice == Money("0.05"),   // unchanged
          cb.finishLines.head.lineTotal == Money("5.00"),   // 0.05 × 100
        )
      },
      test("booklet with cover and body priced correctly") {
        // Cover: coated300gsm (0.12) × 1 sheet × 500 = 60.00, matte lam: 0.03 × 500 = 15.00
        // Body: coated300gsm (0.12) × 7 sheets × 500 = 420.00 (saddle stitch 32 pages: (32/4)-1 = 7)
        // subtotal = 60.00 + 15.00 + 420.00 = 495.00
        // tier 250-999: 0.90×
        // total = 495.00 × 0.90 = 445.50
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.booklets,
          printingMethod = SampleCatalog.offsetMethod,
          components = List(
            ProductComponent(ComponentRole.Cover, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, List(SelectedFinish(SampleCatalog.matteLamination)), sheetCount = 1),
            ProductComponent(ComponentRole.Body, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, Nil, sheetCount = 7),
          ),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          )),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val coverBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Cover).get
        val bodyBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Body).get
        assertTrue(
          result.toEither.isRight,
          coverBd.materialLine.quantity == 500,  // 1 sheet × 500
          coverBd.materialLine.lineTotal == Money("60.00"),
          coverBd.finishLines.size == 1,
          coverBd.finishLines.head.lineTotal == Money("15.00"),
          bodyBd.materialLine.quantity == 3500,  // 7 sheets × 500
          bodyBd.materialLine.lineTotal == Money("420.00"),
          breakdown.subtotal == Money("495.00"),
          breakdown.quantityMultiplier == BigDecimal("0.90"),
          breakdown.total == Money("445.50"),
        )
      },
      test("calendar with different materials per component") {
        // Cover: coatedSilk250gsm (0.11) × 1 × 100 = 11.00
        // Cover ink config (4/0, multiplier 0.60): 11.00 × (0.60 - 1.0) = -4.40
        // Cover gloss lam: 0.03 × 100 = 3.00
        // Body: coated300gsm (0.12) × 6 × 100 = 72.00  (spiral 14 pages: (14-2)/2 = 6)
        // subtotal = 11.00 + (-4.40) + 3.00 + 72.00 = 81.60
        // tier 1-249: 1.0×
        // total = 81.60
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.calendars,
          printingMethod = SampleCatalog.digitalMethod,
          components = List(
            ProductComponent(ComponentRole.Cover, SampleCatalog.coatedSilk250gsm, InkConfiguration.cmyk4_0, List(SelectedFinish(SampleCatalog.glossLamination)), sheetCount = 1),
            ProductComponent(ComponentRole.Body, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, Nil, sheetCount = 6),
          ),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
          )),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val coverBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Cover).get
        val bodyBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Body).get
        assertTrue(
          result.toEither.isRight,
          coverBd.materialLine.unitPrice == Money("0.11"),
          coverBd.materialLine.lineTotal == Money("11.00"),
          bodyBd.materialLine.unitPrice == Money("0.12"),
          bodyBd.materialLine.lineTotal == Money("72.00"),
          breakdown.subtotal == Money("81.60"),
          breakdown.total == Money("81.60"),
        )
      },
      test("4/0 ink configuration applies lower material multiplier than 4/4") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_0,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.inkConfigLine.isDefined,
          cb.inkConfigLine.get.lineTotal == Money("-24.00"),
          breakdown.subtotal == Money("36.00"),
          breakdown.total == Money("32.40"),
        )
      },
      test("4/4 ink configuration produces no ink config line") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(cb.inkConfigLine.isEmpty)
      },
    ),
    suite("error cases")(
      test("missing material base price returns NoBasePriceForMaterial") {
        val emptyPricelist = Pricelist(
          rules = List(
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )

        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, emptyPricelist)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[PricingError.NoBasePriceForMaterial]),
        )
      },
      test("missing quantity spec returns NoQuantityInSpecifications") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[PricingError.NoQuantityInSpecifications.type]),
        )
      },
      test("area pricing without size spec returns NoSizeForAreaPricing") {
        val config = makeConfig(
          category = SampleCatalog.banners,
          material = SampleCatalog.vinyl,
          printingMethod = SampleCatalog.uvInkjetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[PricingError.NoSizeForAreaPricing]),
        )
      },
      test("calendar with new material priced correctly") {
        val config = makeConfig(
          category = SampleCatalog.calendars,
          material = SampleCatalog.coatedSilk250gsm,
          printingMethod = SampleCatalog.digitalMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.materialLine.unitPrice == Money("0.11"),
          breakdown.subtotal == Money("14.00"),
          breakdown.total == Money("14.00"),
        )
      },
      test("Yupo synthetic material priced correctly") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.yupo,
          printingMethod = SampleCatalog.digitalMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.uvCoating)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.materialLine.unitPrice == Money("0.18"),
          breakdown.subtotal == Money("110.00"),
          breakdown.total == Money("99.00"),
        )
      },
      test("Cotton paper with letterpress process surcharge") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.cotton,
          printingMethod = SampleCatalog.letterpressMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(150)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          cb.materialLine.unitPrice == Money("0.22"),
          breakdown.processSurcharge.isDefined,
          breakdown.processSurcharge.get.label.contains("Letterpress"),
          breakdown.subtotal == Money("63.00"),
          breakdown.total == Money("63.00"),
        )
      },
    ),
    suite("CZK pricelist")(
      test("flyer with coated glossy 90gsm 4/4 at 1 pc") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // material: 12 × 1 = 12
        // tier 1-99: 1.0×, no setup fees → billable = 12
        // minimum 500 triggered → total = 500
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("12"),
          breakdown.minimumApplied.isDefined,
          breakdown.total == Money("500.00"),
          breakdown.currency == Currency.CZK,
        )
      },
      test("flyer with coated matte 350gsm 4/4 at 1 pc") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedMatte350gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // material: 15 × 1 = 15
        // tier 1-99: 1.0×, no setup fees → billable = 15
        // minimum 500 triggered → total = 500
        assertTrue(
          cb.materialLine.unitPrice == Money("15"),
          breakdown.minimumApplied.isDefined,
          breakdown.total == Money("500.00"),
          breakdown.currency == Currency.CZK,
        )
      },
      test("flyer with coated glossy 90gsm 4/0 at 1 pc applies ink factor") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_0,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // material: 12 × 1 = 12
        // ink config 4/0: 12 × (0.85 - 1.0) = -1.80
        // subtotal = 12 + (-1.80) = 10.20
        // tier 1-99: 1.0×, no setup fees → billable = 10.20
        // minimum 500 triggered → total = 500
        assertTrue(
          cb.inkConfigLine.isDefined,
          cb.inkConfigLine.get.lineTotal == Money("-1.80"),
          breakdown.subtotal == Money("10.20"),
          breakdown.minimumApplied.isDefined,
          breakdown.total == Money("500.00"),
        )
      },
      test("flyer with coated glossy 130gsm 4/4 at 1000 pcs applies quantity tier") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy130gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1000)),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // material: 12 × 1000 = 12000
        // tier 1000+: 0.40×
        // total = 12000 × 0.40 = 4800 (i.e. 4.80 Kč/pc, approximates 6 Kč from table)
        assertTrue(
          cb.materialLine.unitPrice == Money("12"),
          breakdown.quantityMultiplier == BigDecimal("0.40"),
          breakdown.total == Money("4800.00"),
          breakdown.currency == Currency.CZK,
        )
      },
      test("matte and glossy at same weight have same base price") {
        val configGlossy = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy150gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val configMatte = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedMatte150gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )

        val glossyResult = PriceCalculator.calculate(configGlossy, pricelistCzk)
        val matteResult = PriceCalculator.calculate(configMatte, pricelistCzk)
        val glossyBreakdown = glossyResult.toEither.toOption.get
        val matteBreakdown = matteResult.toEither.toOption.get
        val glossyCb = firstBreakdown(glossyBreakdown)
        val matteCb = firstBreakdown(matteBreakdown)
        assertTrue(
          glossyCb.materialLine.unitPrice == Money("13"),
          matteCb.materialLine.unitPrice == Money("13"),
          glossyBreakdown.total == matteBreakdown.total,
        )
      },
    ),
    suite("sheet-based pricing")(
      test("A3 flyer on SRA3 sheet — 1 piece/sheet, no cutting") {
        // A3 = 420×297mm, effective = 426×303 (3mm bleed each side)
        // SRA3 = 320×450mm
        // Normal: cols=floor((320+2)/(426+2))=0, rows=... → 0
        // Rotated: cols=floor((320+2)/(303+2))=1, rows=floor((450+2)/(426+2))=1 → 1
        // So 1 piece/sheet, unitPrice = 8/1 = 8, no cuts
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("8"),
          cb.materialLine.quantity == 100,
          cb.materialLine.lineTotal == Money("800"),
          cb.cuttingLine.isEmpty,
        )
      },
      test("A4 flyer on SRA3 sheet — 2 pieces/sheet, 1 cut") {
        // A4 = 210×297mm, effective = 216×303 (3mm bleed each side)
        // SRA3 = 320×450mm
        // Normal: cols=floor(322/218)=1, rows=floor(452/305)=1 → 1
        // Rotated: cols=floor(322/305)=1, rows=floor(452/218)=2 → 2
        // So 2 pieces/sheet, sheetsUsed = ceil(100/2) = 50
        // Material: 8 CZK/sheet × 50 = 400
        // Cuts: 1 cut × 0.10 = 0.10 CZK/sheet × 50 = 5.00
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("8"),
          cb.materialLine.quantity == 50,
          cb.materialLine.lineTotal == Money("400"),
          cb.cuttingLine.isDefined,
          cb.cuttingLine.get.unitPrice == Money("0.10"),
          cb.cuttingLine.get.quantity == 50,
          cb.cuttingLine.get.lineTotal == Money("5.00"),
          breakdown.subtotal == Money("405"),
        )
      },
      test("business card on SRA3 sheet — many pieces/sheet, whole-sheet pricing") {
        // Business card = 90×55mm, effective = 96×61 (3mm bleed each side)
        // SRA3 = 320×450mm
        // Normal: cols=floor(322/98)=3, rows=floor(452/63)=7 → 21
        // Rotated: cols=floor(322/63)=5, rows=floor(452/98)=4 → 20
        // So 21 pieces/sheet, sheetsUsed = ceil(100/21) = 5
        // Material: 18 CZK/sheet × 5 = 90
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("18"),
          cb.materialLine.quantity == 5,
          cb.materialLine.lineTotal == Money("90"),
          cb.cuttingLine.isDefined,
        )
      },
      test("small item on SRA3 sheet — whole-sheet pricing") {
        // 40×30mm item, effective = 46×36 (3mm bleed)
        // SRA3 = 320×450mm
        // Normal: cols=floor(322/48)=6, rows=floor(452/38)=11 → 66
        // Rotated: cols=floor(322/38)=8, rows=floor(452/48)=9 → 72
        // 72 pieces/sheet, sheetsUsed = ceil(100/72) = 2
        // Material: 18 CZK/sheet × 2 = 36
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(40, 30)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("18"),
          cb.materialLine.quantity == 2,
          cb.materialLine.lineTotal == Money("36"),
        )
      },
      test("missing SizeSpec returns NoSizeForSheetPricing error") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[PricingError.NoSizeForSheetPricing]),
        )
      },
      test("area pricing takes precedence over sheet pricing") {
        // Create a pricelist with both area and sheet rules for the same material
        val mixedPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialAreaPrice(SampleCatalog.coatedGlossy90gsmId, Money("20.00")),
            PricingRule.MaterialSheetPrice(
              SampleCatalog.coatedGlossy90gsmId, Money("8"),
              320, 450, 3, 2,
            ),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.CZK,
          version = "test",
        )

        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )

        val result = PriceCalculator.calculate(config, mixedPricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Area: 0.210 × 0.297 × 20.00 = 1.2474
        val expectedArea = Money("20.00") * (BigDecimal("0.210") * BigDecimal("0.297"))
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == expectedArea,
          cb.cuttingLine.isEmpty, // no cutting for area pricing
        )
      },
      test("sheet pricing takes precedence over base pricing") {
        // Pricelist with both sheet and base rules for coatedGlossy90gsm
        val mixedPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialSheetPrice(
              SampleCatalog.coatedGlossy90gsmId, Money("8"),
              320, 450, 3, 2,
            ),
            PricingRule.MaterialBasePrice(SampleCatalog.coatedGlossy90gsmId, Money("99")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.CZK,
          version = "test",
        )

        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )

        val result = PriceCalculator.calculate(config, mixedPricelist)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Sheet pricing: 1 piece/sheet → unitPrice = 8 (not 99 from base)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("8"),
        )
      },
      test("sheet pricing + ink config factor interaction") {
        // A4 on SRA3: 2 pieces/sheet, sheetsUsed = 50
        // Material: 8 × 50 = 400, perPiecePrice = 4
        // ink config 4/0 factor = 0.85 → adjustment = 4 × (0.85 - 1.0) = -0.60
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_0,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("8"),
          cb.inkConfigLine.isDefined,
          cb.inkConfigLine.get.unitPrice == Money("-0.60"),
        )
      },
      test("rotated orientation yields more pieces") {
        // 150×100mm item, effective = 156×106 (3mm bleed)
        // SRA3 = 320×450mm
        // Normal: cols=floor(322/158)=2, rows=floor(452/108)=4 → 8
        // Rotated: cols=floor(322/108)=2, rows=floor(452/158)=2 → 4
        // Normal wins with 8 pieces, sheetsUsed = ceil(100/8) = 13
        // Material: 8 CZK/sheet × 13 = 104
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(150, 100)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.materialLine.unitPrice == Money("8"),
          cb.materialLine.quantity == 13,
          cb.cuttingLine.isDefined,
          cb.cuttingLine.get.quantity == 13,
        )
      },
    ),
    suite("sheet quantity tiers")(
      test("100 business cards — 5 sheets — no discount (tier 1-49)") {
        // Business card 90×55mm on SRA3: 21 pieces/sheet
        // 100 / 21 = ceil(4.76) = 5 sheets → tier 1-49 → multiplier 1.0
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.sheetsUsed == 5,
          breakdown.quantityMultiplier == BigDecimal("1.0"),
        )
      },
      test("100 A4 flyers — 50 sheets — 10% discount (tier 50-249)") {
        // A4 210×297mm on SRA3: 2 pieces/sheet
        // 100 / 2 = 50 sheets → tier 50-249 → multiplier 0.90
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          result.toEither.isRight,
          cb.sheetsUsed == 50,
          breakdown.quantityMultiplier == BigDecimal("0.90"),
        )
      },
      test("multi-component booklet — total sheets determines tier") {
        // Saddle-stitch: each folded sheet has flat width = 2× finished page width (90→180mm).
        // Cover: coated300gsm, 90×55mm flat=180×55, pps=10, sheetCount=1, effectiveQty=100
        //   sheetsUsed = ceil(100/10) = 10
        // Body: coated300gsm, 90×55mm flat=180×55, pps=10, sheetCount=7, effectiveQty=700
        //   sheetsUsed = ceil(700/10) = 70
        // totalSheets = 10 + 70 = 80 → tier 50-249 → multiplier 0.90
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.booklets,
          printingMethod = SampleCatalog.offsetMethod,
          components = List(
            ProductComponent(ComponentRole.Cover, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, Nil, sheetCount = 1),
            ProductComponent(ComponentRole.Body, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, Nil, sheetCount = 7),
          ),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          )),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val coverBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Cover).get
        val bodyBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Body).get
        val totalSheets = coverBd.sheetsUsed + bodyBd.sheetsUsed
        assertTrue(
          result.toEither.isRight,
          coverBd.sheetsUsed == 10,
          bodyBd.sheetsUsed == 70,
          totalSheets == 80,
          breakdown.quantityMultiplier == BigDecimal("0.90"),
        )
      },
      test("saddle-stitch 8-page A4 booklet: 10 copies require 20 sheets") {
        // Each saddle-stitch sheet folds to give 4 A4 pages; flat size = 420×297mm.
        // On SRA3 (320×450mm) with bleed=3/gutter=2: pps=1 per component.
        // Cover: sheetCount=1, effectiveQty=10, sheetsUsed=ceil(10/1)=10
        // Body: sheetCount=(8/4)-1=1, effectiveQty=10, sheetsUsed=ceil(10/1)=10
        // totalSheets = 20 (matches the expected physical production sheet count)
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.booklets,
          printingMethod = SampleCatalog.offsetMethod,
          components = List(
            ProductComponent(ComponentRole.Cover, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, Nil, sheetCount = 1),
            ProductComponent(ComponentRole.Body, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, Nil, sheetCount = 1),
          ),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
            SpecValue.PagesSpec(8),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          )),
        )

        val result = PriceCalculator.calculate(config, SamplePricelist.pricelistCzkSheet)
        val breakdown = result.toEither.toOption.get
        val coverBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Cover).get
        val bodyBd = breakdown.componentBreakdowns.find(_.role == ComponentRole.Body).get
        val totalSheets = coverBd.sheetsUsed + bodyBd.sheetsUsed
        assertTrue(
          result.toEither.isRight,
          coverBd.sheetsUsed == 10,
          bodyBd.sheetsUsed == 10,
          totalSheets == 20,
        )
      },
      test("pricelist with only QuantityTier — uses product quantity (backward compat)") {
        // Pricelist with QuantityTier only (no SheetQuantityTier)
        // Even though sheet pricing is used, falls back to product quantity tiers
        val baseOnlyPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialSheetPrice(
              SampleCatalog.coatedGlossy90gsmId, Money("8"),
              320, 450, 3, 2,
            ),
            PricingRule.CuttingSurcharge(costPerCut = Money("0.10")),
            PricingRule.QuantityTier(1, Some(99), BigDecimal("1.0")),
            PricingRule.QuantityTier(100, Some(499), BigDecimal("0.90")),
            PricingRule.QuantityTier(500, None, BigDecimal("0.80")),
          ),
          currency = Currency.CZK,
          version = "test-compat",
        )

        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = PriceCalculator.calculate(config, baseOnlyPricelist)
        val breakdown = result.toEither.toOption.get
        // 100 qty → tier 100-499 → multiplier 0.90 (product quantity, not sheets)
        assertTrue(
          result.toEither.isRight,
          breakdown.quantityMultiplier == BigDecimal("0.90"),
        )
      },
    ),
    suite("setup fees and minimum order price")(
      test("setup fee added after quantity multiplier, not discounted") {
        // material: 0.10 × 100 = 10.00
        // tier 0.50×: discountedSubtotal = 5.00
        // setup fee: 30.00 (not discounted)
        // billable = 5.00 + 30.00 = 35.00
        // no minimum → total = 35.00
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSetupFee(SampleCatalog.matteLaminationId, Money("30")),
            PricingRule.QuantityTier(1, None, BigDecimal("0.50")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          breakdown.quantityMultiplier == BigDecimal("0.50"),
          breakdown.setupFees.size == 1,
          breakdown.setupFees.head.lineTotal == Money("30"),
          breakdown.minimumApplied.isEmpty,
          breakdown.total == Money("35.00"),
        )
      },
      test("ID-level FinishSetupFee overrides type-level FinishTypeSetupFee") {
        // ID-level: 20; type-level: 99 — ID wins
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSetupFee(SampleCatalog.matteLaminationId, Money("20")),
            PricingRule.FinishTypeSetupFee(FinishType.Lamination, Money("99")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.setupFees.size == 1,
          breakdown.setupFees.head.lineTotal == Money("20"),
        )
      },
      test("type-level FinishTypeSetupFee applies when no ID-level rule") {
        // No ID-level rule for softTouchCoating → type-level Lamination fee applies
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishTypeSetupFee(FinishType.SoftTouchCoating, Money("45")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.softTouchCoating)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.setupFees.size == 1,
          breakdown.setupFees.head.lineTotal == Money("45"),
        )
      },
      test("same finish on two components charged once (deduplication)") {
        // Matte lamination on both Cover and Body → setup fee charged only once
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSetupFee(SampleCatalog.matteLaminationId, Money("50")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.booklets,
          printingMethod = SampleCatalog.offsetMethod,
          components = List(
            ProductComponent(ComponentRole.Cover, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, List(SelectedFinish(SampleCatalog.matteLamination)), sheetCount = 1),
            ProductComponent(ComponentRole.Body, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, List(SelectedFinish(SampleCatalog.matteLamination)), sheetCount = 1),
          ),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(8),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          )),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.setupFees.size == 1,
          breakdown.setupFees.head.lineTotal == Money("50"),
        )
      },
      test("MinimumOrderPrice applied when billable is below floor") {
        // material: 0.10 × 5 = 0.50, tier 1.0× → discountedSubtotal = 0.50
        // no setup fees → billable = 0.50
        // minimum = 100 → total = 100, minimumApplied = Some(0.50)
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
            PricingRule.MinimumOrderPrice(Money("100")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(5)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          breakdown.minimumApplied.isDefined,
          breakdown.minimumApplied.get == Money("0.50"),
          breakdown.total == Money("100.00"),
        )
      },
      test("MinimumOrderPrice not applied when billable is above floor") {
        // material: 0.10 × 2000 = 200, no tier → total = 200 > minimum 100
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
            PricingRule.MinimumOrderPrice(Money("100")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(2000)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          breakdown.minimumApplied.isEmpty,
          breakdown.total == Money("200.00"),
        )
      },
      test("minimum compared against combined billable (discounted subtotal + setup fees)") {
        // material: 0.10 × 10 = 1.00, tier 0.50× → discountedSubtotal = 0.50
        // setup fee: 40 → billable = 40.50
        // minimum = 100 → floor triggered (40.50 < 100), minimumApplied = Some(40.50)
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.10")),
            PricingRule.FinishSetupFee(SampleCatalog.matteLaminationId, Money("40")),
            PricingRule.QuantityTier(1, None, BigDecimal("0.50")),
            PricingRule.MinimumOrderPrice(Money("100")),
          ),
          currency = Currency.USD,
          version = "test",
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )
        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          breakdown.setupFees.size == 1,
          breakdown.minimumApplied.isDefined,
          breakdown.minimumApplied.get == Money("40.50"),
          breakdown.total == Money("100.00"),
        )
      },
      test("setupFees empty and minimumApplied is None with old-style pricelist") {
        // USD pricelist has no setup fee or minimum rules → backward compat
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          breakdown.setupFees.isEmpty,
          breakdown.minimumApplied.isEmpty,
        )
      },
    ),
  )
