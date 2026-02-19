package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*

object PriceCalculatorSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val configId = ConfigurationId.unsafe("test-pricing-1")

  private def makeConfig(
      category: ProductCategory,
      material: Material,
      printingMethod: PrintingMethod,
      finishes: List[Finish],
      specs: List[SpecValue],
  ): ProductConfiguration =
    ProductConfiguration(
      id = configId,
      category = category,
      material = material,
      printingMethod = printingMethod,
      finishes = finishes,
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  def spec = suite("PriceCalculator")(
    suite("valid pricing")(
      test("business card with material + finish + quantity tier") {
        // 500× coated 300gsm + matte lamination + offset → 250-999 tier (0.90×)
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.12 × 500 = 60.00
        // matte lamination: 0.03 × 500 = 15.00
        // subtotal = 75.00
        // tier 250-999: 0.90×
        // total = 75.00 × 0.90 = 67.50
        assertTrue(
          result.toEither.isRight,
          breakdown.materialLine.unitPrice == Money("0.12"),
          breakdown.materialLine.quantity == 500,
          breakdown.materialLine.lineTotal == Money("60.00"),
          breakdown.finishLines.size == 1,
          breakdown.finishLines.head.unitPrice == Money("0.03"),
          breakdown.finishLines.head.lineTotal == Money("15.00"),
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
          finishes = List(SampleCatalog.uvCoating),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // area = 1000 × 500 / 1_000_000 = 0.5 sqm
        // material unit price = 18.00 × 0.5 = 9.00
        // material line total = 9.00 × 10 = 90.00
        // UV coating (type-level): 0.04 × 10 = 0.40
        // subtotal = 90.40
        // tier 1-249: 1.0×
        // total = 90.40
        assertTrue(
          result.toEither.isRight,
          breakdown.materialLine.unitPrice == Money("9.00"),
          breakdown.materialLine.lineTotal == Money("90.00"),
          breakdown.finishLines.size == 1,
          breakdown.finishLines.head.unitPrice == Money("0.04"),
          breakdown.subtotal == Money("90.40"),
          breakdown.quantityMultiplier == BigDecimal("1.0"),
          breakdown.total == Money("90.40"),
        )
      },
      test("quantity tier discount correctly applied") {
        // 1000× coated 300gsm, no finishes, offset → 1000-4999 tier (0.80×)
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(1000)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.12 × 1000 = 120.00
        // subtotal = 120.00
        // tier 1000-4999: 0.80×
        // total = 96.00
        assertTrue(
          breakdown.subtotal == Money("120.00"),
          breakdown.quantityMultiplier == BigDecimal("0.80"),
          breakdown.total == Money("96.00"),
        )
      },
      test("multiple finish surcharges accumulated") {
        // 500× coated 300gsm + embossing + foil stamping + offset
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.embossing, SampleCatalog.foilStamping),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.12 × 500 = 60.00
        // embossing: 0.08 × 500 = 40.00
        // foil stamping: 0.15 × 500 = 75.00
        // subtotal = 175.00
        // tier 250-999: 0.90×
        // total = 157.50
        assertTrue(
          breakdown.finishLines.size == 2,
          breakdown.subtotal == Money("175.00"),
          breakdown.total == Money("157.50"),
        )
      },
      test("letterpress process surcharge applied") {
        // 500× coated 300gsm + letterpress
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.letterpressMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.12 × 500 = 60.00
        // letterpress: 0.20 × 500 = 100.00
        // subtotal = 160.00
        // tier 250-999: 0.90×
        // total = 144.00
        assertTrue(
          breakdown.processSurcharge.isDefined,
          breakdown.processSurcharge.get.unitPrice == Money("0.20"),
          breakdown.processSurcharge.get.lineTotal == Money("100.00"),
          breakdown.subtotal == Money("160.00"),
          breakdown.total == Money("144.00"),
        )
      },
      test("ID-level finish surcharge takes precedence over type-level") {
        // UV coating has both: type-level FinishTypeSurcharge(UVCoating, 0.04)
        // and no ID-level surcharge, so type-level applies.
        // Matte lamination has ID-level (0.03) and no type-level for Lamination.
        // Test: Add a type-level surcharge for Lamination and verify ID-level wins.
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
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        // ID-level surcharge (0.03) should win over type-level (0.99)
        assertTrue(
          breakdown.finishLines.head.unitPrice == Money("0.03"),
        )
      },
      test("no surcharge for finish with no pricing rule") {
        // Round corners has no pricing rule → gracefully skipped
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.roundCorners),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.finishLines.isEmpty,
          breakdown.subtotal == Money("60.00"),
        )
      },
      test("booklet with binding method priced correctly") {
        // Binding has no special pricing rule — just material + finishes
        val config = makeConfig(
          category = SampleCatalog.booklets,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.12 × 500 = 60.00
        // matte lamination: 0.03 × 500 = 15.00
        // subtotal = 75.00
        // tier 250-999: 0.90×
        // total = 67.50
        assertTrue(
          result.toEither.isRight,
          breakdown.subtotal == Money("75.00"),
          breakdown.total == Money("67.50"),
        )
      },
      test("4/0 ink configuration applies lower material multiplier than 4/4") {
        // 500× coated 300gsm + 4/0 ink config → 0.60 material multiplier
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_0),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.12 × 500 = 60.00
        // ink config: 60.00 × (0.60 - 1.0) = -24.00
        // subtotal = 60.00 + (-24.00) = 36.00
        // tier 250-999: 0.90×
        // total = 36.00 × 0.90 = 32.40
        assertTrue(
          breakdown.inkConfigLine.isDefined,
          breakdown.inkConfigLine.get.lineTotal == Money("-24.00"),
          breakdown.subtotal == Money("36.00"),
          breakdown.total == Money("32.40"),
        )
      },
      test("4/4 ink configuration produces no ink config line") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(breakdown.inkConfigLine.isEmpty)
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
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
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
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
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
          finishes = Nil,
          specs = List(
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[PricingError.NoSizeForAreaPricing]),
        )
      },
      test("calendar with new material priced correctly") {
        // 100× coated silk 250gsm (0.11) + matte lamination (0.03) + digital → no tier (1.0×)
        val config = makeConfig(
          category = SampleCatalog.calendars,
          material = SampleCatalog.coatedSilk250gsm,
          printingMethod = SampleCatalog.digitalMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.11 × 100 = 11.00
        // matte lamination: 0.03 × 100 = 3.00
        // subtotal = 14.00
        // tier: 1-249 qty → 1.0×
        // total = 14.00
        assertTrue(
          breakdown.materialLine.unitPrice == Money("0.11"),
          breakdown.subtotal == Money("14.00"),
          breakdown.total == Money("14.00"),
        )
      },
      test("Yupo synthetic material priced correctly") {
        // 500× Yupo (0.18) + UV coating (0.04) + digital → 250-999 tier (0.90×)
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.yupo,
          printingMethod = SampleCatalog.digitalMethod,
          finishes = List(SampleCatalog.uvCoating),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.18 × 500 = 90.00
        // UV coating: 0.04 × 500 = 20.00
        // subtotal = 110.00
        // tier: 250-999 qty → 0.90×
        // total = 110.00 × 0.90 = 99.00
        assertTrue(
          breakdown.materialLine.unitPrice == Money("0.18"),
          breakdown.subtotal == Money("110.00"),
          breakdown.total == Money("99.00"),
        )
      },
      test("Cotton paper with letterpress process surcharge") {
        // 150× Cotton (0.22) + letterpress surcharge (0.20) → 1-249 tier (1.0×)
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.cotton,
          printingMethod = SampleCatalog.letterpressMethod,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(150)),
            SpecValue.InkConfigSpec(InkConfiguration.cmyk4_4),
          ),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        // material: 0.22 × 150 = 33.00
        // letterpress: 0.20 × 150 = 30.00
        // subtotal = 63.00
        // tier: 1-249 qty → 1.0×
        // total = 63.00
        assertTrue(
          breakdown.materialLine.unitPrice == Money("0.22"),
          breakdown.processSurcharge.isDefined,
          breakdown.processSurcharge.get.label.contains("Letterpress"),
          breakdown.subtotal == Money("63.00"),
          breakdown.total == Money("63.00"),
        )
      },
    ),
  )
