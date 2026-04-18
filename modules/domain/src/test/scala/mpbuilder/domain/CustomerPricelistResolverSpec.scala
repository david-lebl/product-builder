package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*

object CustomerPricelistResolverSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val configId = ConfigurationId.unsafe("test-cust-pricing-1")

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

  /** Standard test config: 500x coated 300gsm business cards with matte lamination. */
  private val standardConfig = makeConfig(
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

  /** Area-based config: 10x vinyl banners 1000x500mm with UV coating. */
  private val areaConfig = makeConfig(
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

  def spec = suite("CustomerPricelistResolver")(
    suite("empty pricing")(
      test("empty CustomerPricing produces identical breakdown") {
        val baseResult = PriceCalculator.calculate(standardConfig, pricelist)
        val resolved = CustomerPricelistResolver.resolve(pricelist, CustomerPricing.empty)
        val customerResult = PriceCalculator.calculate(standardConfig, resolved)
        val base = baseResult.toEither.toOption.get
        val customer = customerResult.toEither.toOption.get
        assertTrue(
          base.total == customer.total,
          base.subtotal == customer.subtotal,
          base.quantityMultiplier == customer.quantityMultiplier,
        )
      },
    ),
    suite("global discount")(
      test("10% global discount reduces material and finish prices") {
        val pricing = CustomerPricing(globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))))
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Base: material $0.08, finish $0.03
        // After 10%: material $0.072, finish $0.027
        assertTrue(
          cb.materialLine.unitPrice == Money("0.072"),
          cb.finishLines.head.unitPrice == Money("0.027"),
        )
      },
      test("global discount applied to area-based material") {
        val pricing = CustomerPricing(globalDiscount = Some(Percentage.unsafe(BigDecimal("20"))))
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(areaConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Base vinyl: $18.00/sqm, after 20%: $14.40/sqm
        // Area: 1.0 * 0.5 = 0.5 sqm → unit price = $14.40 * 0.5 = $7.20
        assertTrue(cb.materialLine.unitPrice == Money("7.20"))
      },
    ),
    suite("material-specific discount")(
      test("material percentage discount overrides global") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))),
          materialDiscounts = Map(
            SampleCatalog.coated300gsmId -> Percentage.unsafe(BigDecimal("25")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Material: 25% off $0.08 = $0.06 (not 10% global)
        // Finish: 10% off $0.03 = $0.027 (global applies to finish)
        assertTrue(
          cb.materialLine.unitPrice == Money("0.06"),
          cb.finishLines.head.unitPrice == Money("0.027"),
        )
      },
      test("material discount on area-priced material") {
        val pricing = CustomerPricing(
          materialDiscounts = Map(
            SampleCatalog.vinylId -> Percentage.unsafe(BigDecimal("15")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(areaConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Base vinyl: $18.00/sqm, after 15%: $15.30/sqm
        // Area: 0.5 sqm → unit price = $15.30 * 0.5 = $7.65
        assertTrue(cb.materialLine.unitPrice == Money("7.65"))
      },
    ),
    suite("fixed material price")(
      test("fixed price replaces base material price") {
        val pricing = CustomerPricing(
          fixedMaterialPrices = Map(
            SampleCatalog.coated300gsmId -> Price(Money("0.08"), Currency.USD),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(cb.materialLine.unitPrice == Money("0.08"))
      },
      test("fixed price takes precedence over material percentage") {
        val pricing = CustomerPricing(
          materialDiscounts = Map(
            SampleCatalog.coated300gsmId -> Percentage.unsafe(BigDecimal("50")),
          ),
          fixedMaterialPrices = Map(
            SampleCatalog.coated300gsmId -> Price(Money("0.07"), Currency.USD),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Fixed $0.07 takes priority, not 50% off $0.08 = $0.04
        assertTrue(cb.materialLine.unitPrice == Money("0.07"))
      },
      test("fixed price with wrong currency is ignored") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))),
          fixedMaterialPrices = Map(
            SampleCatalog.coated300gsmId -> Price(Money("2.00"), Currency.CZK), // wrong currency
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // CZK fixed price ignored for USD pricelist → falls through to global 10%
        assertTrue(cb.materialLine.unitPrice == Money("0.072"))
      },
      test("fixed price on area-priced material") {
        val pricing = CustomerPricing(
          fixedMaterialPrices = Map(
            SampleCatalog.vinylId -> Price(Money("15.00"), Currency.USD),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(areaConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Fixed $15.00/sqm × 0.5 sqm = $7.50
        assertTrue(cb.materialLine.unitPrice == Money("7.50"))
      },
    ),
    suite("category discount")(
      test("category discount applied when categoryId matches") {
        val pricing = CustomerPricing(
          categoryDiscounts = Map(
            SampleCatalog.businessCardsId -> Percentage.unsafe(BigDecimal("15")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing, Some(SampleCatalog.businessCardsId))
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // 15% off $0.08 = $0.068
        assertTrue(cb.materialLine.unitPrice == Money("0.068"))
      },
      test("category discount not applied when different category") {
        val pricing = CustomerPricing(
          categoryDiscounts = Map(
            SampleCatalog.bannersId -> Percentage.unsafe(BigDecimal("30")),
          ),
        )
        // Configuring business cards but discount is for banners → no discount
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing, Some(SampleCatalog.businessCardsId))
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(cb.materialLine.unitPrice == Money("0.08"))
      },
      test("material discount overrides category discount") {
        val pricing = CustomerPricing(
          categoryDiscounts = Map(
            SampleCatalog.businessCardsId -> Percentage.unsafe(BigDecimal("10")),
          ),
          materialDiscounts = Map(
            SampleCatalog.coated300gsmId -> Percentage.unsafe(BigDecimal("20")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing, Some(SampleCatalog.businessCardsId))
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Material 20% overrides category 10% → $0.08 * 0.80 = $0.064
        assertTrue(cb.materialLine.unitPrice == Money("0.064"))
      },
      test("category discount overrides global discount") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("5"))),
          categoryDiscounts = Map(
            SampleCatalog.businessCardsId -> Percentage.unsafe(BigDecimal("15")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing, Some(SampleCatalog.businessCardsId))
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Category 15% overrides global 5% → $0.08 * 0.85 = $0.068
        // But finish should use global 5% (no finish-specific discount)
        assertTrue(
          cb.materialLine.unitPrice == Money("0.068"),
          cb.finishLines.head.unitPrice == Money("0.0285"),
        )
      },
    ),
    suite("finish discount")(
      test("finish-specific discount applied") {
        val pricing = CustomerPricing(
          finishDiscounts = Map(
            SampleCatalog.matteLaminationId -> Percentage.unsafe(BigDecimal("20")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Material unchanged, finish 20% off $0.03 = $0.024
        assertTrue(
          cb.materialLine.unitPrice == Money("0.08"),
          cb.finishLines.head.unitPrice == Money("0.024"),
        )
      },
      test("finish discount overrides global for that finish") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))),
          finishDiscounts = Map(
            SampleCatalog.matteLaminationId -> Percentage.unsafe(BigDecimal("30")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        // Material: 10% global → $0.072
        // Matte lamination: 30% specific → $0.03 * 0.70 = $0.021 (not 10% = $0.027)
        assertTrue(
          cb.materialLine.unitPrice == Money("0.072"),
          cb.finishLines.head.unitPrice == Money("0.021"),
        )
      },
    ),
    suite("custom quantity tiers")(
      test("custom tiers replace base tiers") {
        val pricing = CustomerPricing(
          customQuantityTiers = Some(List(
            PricingRule.QuantityTier(1, Some(99), BigDecimal("1.0")),
            PricingRule.QuantityTier(100, None, BigDecimal("0.70")),
          )),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        // 500 qty → matches custom tier 100+ → multiplier 0.70
        // Base pricelist for 500: 250-999 tier → 0.90
        assertTrue(breakdown.quantityMultiplier == BigDecimal("0.70"))
      },
      test("without custom tiers, base tiers remain") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("5"))),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        // Base pricelist tier for 500: 0.90 (unchanged)
        assertTrue(breakdown.quantityMultiplier == BigDecimal("0.90"))
      },
    ),
    suite("minimum order override")(
      test("minimum order override replaces base minimum") {
        // Create a config that would normally fall below minimum, then check override
        val smallConfig = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val pricing = CustomerPricing(
          minimumOrderOverride = Some(Money("0.01")),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(smallConfig, resolved)
        val breakdown = result.toEither.toOption.get
        // With base minimum order, the total would be raised to the floor.
        // With override of $0.01, the actual calculated price is used
        // since $0.12 (1 × $0.12) > $0.01
        assertTrue(breakdown.minimumApplied.isEmpty)
      },
    ),
    suite("price comparison")(
      test("base vs customer price can be compared via two calculations") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))),
          materialDiscounts = Map(
            SampleCatalog.coated300gsmId -> Percentage.unsafe(BigDecimal("20")),
          ),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)

        val baseResult = PriceCalculator.calculate(standardConfig, pricelist)
        val customerResult = PriceCalculator.calculate(standardConfig, resolved)

        val baseBreakdown = baseResult.toEither.toOption.get
        val customerBreakdown = customerResult.toEither.toOption.get

        // Base: $0.08 material, $0.04 ink surcharge, $0.03 finish
        // Customer: $0.064 material (20% off), $0.027 finish (10% off), ink $0.04 unchanged
        assertTrue(
          baseBreakdown.total.value > customerBreakdown.total.value,
          baseBreakdown.currency == customerBreakdown.currency,
        )
      },
    ),
    suite("combined discounts")(
      test("all discount types work together with proper precedence") {
        val pricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("5"))),
          materialDiscounts = Map(
            SampleCatalog.coated300gsmId -> Percentage.unsafe(BigDecimal("15")),
          ),
          finishDiscounts = Map(
            SampleCatalog.matteLaminationId -> Percentage.unsafe(BigDecimal("25")),
          ),
          customQuantityTiers = Some(List(
            PricingRule.QuantityTier(1, Some(99), BigDecimal("1.0")),
            PricingRule.QuantityTier(100, None, BigDecimal("0.75")),
          )),
        )
        val resolved = CustomerPricelistResolver.resolve(pricelist, pricing)
        val result = PriceCalculator.calculate(standardConfig, resolved)
        val breakdown = result.toEither.toOption.get
        val cb = firstBreakdown(breakdown)
        assertTrue(
          // Material: 15% specific (not 5% global) → $0.08 * 0.85 = $0.068
          cb.materialLine.unitPrice == Money("0.068"),
          // Finish: 25% specific (not 5% global) → $0.03 * 0.75 = $0.0225
          cb.finishLines.head.unitPrice == Money("0.0225"),
          // Custom tier: 500 → 0.75 (not base 0.90)
          breakdown.quantityMultiplier == BigDecimal("0.75"),
        )
      },
    ),
    suite("Percentage opaque type")(
      test("valid percentage is created") {
        val result = Percentage(BigDecimal("50"))
        assertTrue(result.toEither.isRight)
      },
      test("percentage at boundary 0 is valid") {
        val result = Percentage(BigDecimal("0"))
        assertTrue(result.toEither.isRight)
      },
      test("percentage at boundary 100 is valid") {
        val result = Percentage(BigDecimal("100"))
        assertTrue(result.toEither.isRight)
      },
      test("negative percentage is rejected") {
        val result = Percentage(BigDecimal("-1"))
        assertTrue(result.toEither.isLeft)
      },
      test("percentage above 100 is rejected") {
        val result = Percentage(BigDecimal("101"))
        assertTrue(result.toEither.isLeft)
      },
      test("applyTo computes correct discounted amount") {
        val pct = Percentage.unsafe(BigDecimal("10"))
        val money = Money("100.00")
        assertTrue(pct.applyTo(money) == Money("90.00"))
      },
      test("zero percentage returns same amount") {
        val pct = Percentage.unsafe(BigDecimal("0"))
        val money = Money("50.00")
        assertTrue(pct.applyTo(money) == Money("50.00"))
      },
      test("100 percentage returns zero") {
        val pct = Percentage.unsafe(BigDecimal("100"))
        val money = Money("50.00")
        assertTrue(pct.applyTo(money) == Money("0.00"))
      },
    ),
  )
