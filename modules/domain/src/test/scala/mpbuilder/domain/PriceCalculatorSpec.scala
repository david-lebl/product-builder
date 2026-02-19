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
      finishes: List[Finish],
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
          finishes = List(SampleCatalog.matteLamination),
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
          finishes = List(SampleCatalog.uvCoating),
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
          finishes = List(SampleCatalog.embossing, SampleCatalog.foilStamping),
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
          finishes = List(SampleCatalog.matteLamination),
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
          finishes = List(SampleCatalog.roundCorners),
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
            ProductComponent(ComponentRole.Cover, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLamination), sheetCount = 1),
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
            ProductComponent(ComponentRole.Cover, SampleCatalog.coatedSilk250gsm, InkConfiguration.cmyk4_0, List(SampleCatalog.glossLamination), sheetCount = 1),
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
          finishes = List(SampleCatalog.matteLamination),
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
          finishes = List(SampleCatalog.uvCoating),
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
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        // material: 12 × 1 = 12
        // tier 1-99: 1.0×
        // total = 12
        assertTrue(
          result.toEither.isRight,
          breakdown.materialLine.unitPrice == Money("12"),
          breakdown.total == Money("12.00"),
          breakdown.currency == Currency.CZK,
        )
      },
      test("flyer with coated matte 350gsm 4/4 at 1 pc") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedMatte350gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        // material: 15 × 1 = 15
        // tier 1-99: 1.0×
        // total = 15
        assertTrue(
          breakdown.materialLine.unitPrice == Money("15"),
          breakdown.total == Money("15.00"),
          breakdown.currency == Currency.CZK,
        )
      },
      test("flyer with coated glossy 90gsm 4/0 at 1 pc applies ink factor") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy90gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_0),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        // material: 12 × 1 = 12
        // ink config 4/0: 12 × (0.85 - 1.0) = -1.80
        // subtotal = 12 + (-1.80) = 10.20
        // tier 1-99: 1.0×
        // total = 10.20 (approximates 10 Kč from price table)
        assertTrue(
          breakdown.inkConfigLine.isDefined,
          breakdown.inkConfigLine.get.lineTotal == Money("-1.80"),
          breakdown.subtotal == Money("10.20"),
          breakdown.total == Money("10.20"),
        )
      },
      test("flyer with coated glossy 130gsm 4/4 at 1000 pcs applies quantity tier") {
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedGlossy130gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1000)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelistCzk)
        val breakdown = result.toEither.toOption.get
        // material: 12 × 1000 = 12000
        // tier 1000+: 0.40×
        // total = 12000 × 0.40 = 4800 (i.e. 4.80 Kč/pc, approximates 6 Kč from table)
        assertTrue(
          breakdown.materialLine.unitPrice == Money("12"),
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
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )
        val configMatte = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.coatedMatte150gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(420, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val glossyResult = PriceCalculator.calculate(configGlossy, pricelistCzk)
        val matteResult = PriceCalculator.calculate(configMatte, pricelistCzk)
        val glossyBreakdown = glossyResult.toEither.toOption.get
        val matteBreakdown = matteResult.toEither.toOption.get
        assertTrue(
          glossyBreakdown.materialLine.unitPrice == Money("13"),
          matteBreakdown.materialLine.unitPrice == Money("13"),
          glossyBreakdown.total == matteBreakdown.total,
        )
      },
    ),
  )
