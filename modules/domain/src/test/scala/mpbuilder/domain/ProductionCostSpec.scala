package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*

object ProductionCostSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val costSheet = SampleProductionCosts.costSheet
  private val configId = ConfigurationId.unsafe("test-cost-1")

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

  /** Standard test config: 500× coated 300gsm business cards with matte lamination, offset. */
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

  /** Area-based config: 10× vinyl banners 1000×500mm with UV inkjet. */
  private val areaConfig = makeConfig(
    category = SampleCatalog.banners,
    material = SampleCatalog.vinyl,
    printingMethod = SampleCatalog.uvInkjetMethod,
    inkConfig = InkConfiguration.cmyk4_4,
    finishes = Nil,
    specs = List(
      SpecValue.SizeSpec(Dimension(1000, 500)),
      SpecValue.QuantitySpec(Quantity.unsafe(10)),
    ),
  )

  def spec = suite("ProductionCostCalculator")(
    suite("calculateCost")(
      test("unit-based material cost with process and finish costs") {
        // 500× coated 300gsm @ $0.05 = $25.00 material
        // 500× offset @ $0.01 = $5.00 process
        // 500× matte lam @ $0.01 = $5.00 finish
        // Direct total = $35.00
        // With 1.15 overhead = $40.25
        val result = ProductionCostCalculator.calculateCost(standardConfig, costSheet)
        val cost = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          cost == Money("40.25"),
        )
      },
      test("area-based material cost with process cost") {
        // 10× vinyl 1000×500mm = 0.5 sqm each
        // 10× $8.00 × 0.5 = $40.00 material
        // 10× UV inkjet @ $0.03 = $0.30 process
        // Direct = $40.30
        // With 1.15 overhead = $46.35 (rounded)
        val result = ProductionCostCalculator.calculateCost(areaConfig, costSheet)
        val cost = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          cost == Money("46.35"),
        )
      },
      test("zero cost when no matching rules") {
        // Use a material with no cost rule
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm.copy(id = MaterialId.unsafe("mat-unknown")),
          printingMethod = SampleCatalog.offsetMethod.copy(processType = PrintingProcessType.ScreenPrint),
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = ProductionCostCalculator.calculateCost(config, costSheet)
        val cost = result.toEither.toOption.get
        assertTrue(cost == Money("0.00"))
      },
      test("fails when quantity is missing") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = Nil,
          specs = List(SpecValue.SizeSpec(Dimension(90, 55))),
        )
        val result = ProductionCostCalculator.calculateCost(config, costSheet)
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("analyze — margin analysis")(
      test("healthy margin with base pricing") {
        // Selling price for standardConfig ≈ $67.50 (from PriceCalculator)
        // Production cost ≈ $40.25
        // Margin ≈ $27.25
        val result = ProductionCostCalculator.analyze(standardConfig, pricelist, costSheet)
        val analysis = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          analysis.productionCost == Money("40.25"),
          analysis.sellingPrice == Money("67.50"),
          analysis.margin == Money("27.25"),
          !analysis.isBelowCost,
          analysis.warnings.isEmpty,
        )
      },
      test("below-cost detection when selling price is too low") {
        // Create a pricelist with very low material prices → selling price below cost
        val cheapPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.01")),
            PricingRule.FinishSurcharge(SampleCatalog.matteLaminationId, Money("0.003")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
          ),
          currency = Currency.USD,
          version = "cheap",
        )
        val result = ProductionCostCalculator.analyze(standardConfig, cheapPricelist, costSheet)
        val analysis = result.toEither.toOption.get
        assertTrue(
          analysis.isBelowCost,
          analysis.warnings.exists {
            case CostWarning.BelowProductionCost(_) => true
            case _                                  => false
          },
        )
      },
      test("low margin warning when margin is below threshold") {
        // Use a threshold of 80% — the standard config has ~67% margin which is below 80%
        val highThreshold = Percentage.unsafe(BigDecimal("80"))
        val result = ProductionCostCalculator.analyze(
          standardConfig,
          pricelist,
          costSheet,
          lowMarginThreshold = highThreshold,
        )
        val analysis = result.toEither.toOption.get
        assertTrue(
          !analysis.isBelowCost,
          analysis.warnings.exists {
            case CostWarning.LowMargin(_, threshold) => threshold.value == BigDecimal("80")
            case _                                    => false
          },
        )
      },
      test("no warnings when margin is above threshold") {
        val lowThreshold = Percentage.unsafe(BigDecimal("5"))
        val result = ProductionCostCalculator.analyze(
          standardConfig,
          pricelist,
          costSheet,
          lowMarginThreshold = lowThreshold,
        )
        val analysis = result.toEither.toOption.get
        assertTrue(
          !analysis.isBelowCost,
          analysis.warnings.isEmpty,
        )
      },
      test("area-based configuration margin analysis") {
        // Banner selling price: 10× vinyl 0.5sqm @ $18.00/sqm = $90.00 (no finishes, no tier discount)
        // Production cost: 10× vinyl 0.5sqm @ $8.00/sqm = $40.00 + 10× UV inkjet @ $0.03 = $0.30
        // Direct = $40.30, with 1.15 overhead = $46.35
        val result = ProductionCostCalculator.analyze(areaConfig, pricelist, costSheet)
        val analysis = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          analysis.productionCost == Money("46.35"),
          analysis.sellingPrice == Money("90.00"),
          !analysis.isBelowCost,
          analysis.warnings.isEmpty,
        )
      },
    ),
    suite("analyzeWithCustomerPricing — customer discount impact")(
      test("customer discount reduces margin") {
        val customerPricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))),
        )
        val baseResult = ProductionCostCalculator.analyze(standardConfig, pricelist, costSheet)
        val customerResult = ProductionCostCalculator.analyzeWithCustomerPricing(
          standardConfig, pricelist, customerPricing, costSheet,
        )
        val baseAnalysis = baseResult.toEither.toOption.get
        val customerAnalysis = customerResult.toEither.toOption.get
        assertTrue(
          // Production cost stays the same
          customerAnalysis.productionCost == baseAnalysis.productionCost,
          // Selling price is lower with customer discount
          customerAnalysis.sellingPrice.value < baseAnalysis.sellingPrice.value,
          // Margin is lower
          customerAnalysis.margin.value < baseAnalysis.margin.value,
        )
      },
      test("large customer discount triggers below-cost warning") {
        // 90% global discount should bring selling price well below production cost
        val heavyDiscount = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal("90"))),
        )
        val result = ProductionCostCalculator.analyzeWithCustomerPricing(
          standardConfig, pricelist, heavyDiscount, costSheet,
        )
        val analysis = result.toEither.toOption.get
        assertTrue(
          analysis.isBelowCost,
          analysis.warnings.exists {
            case CostWarning.BelowProductionCost(_) => true
            case _                                  => false
          },
        )
      },
    ),
    suite("CostWarning messages")(
      test("BelowProductionCost English message") {
        val warning = CostWarning.BelowProductionCost(Money("15.50"))
        assertTrue(warning.message(Language.En).contains("below production cost"))
      },
      test("BelowProductionCost Czech message") {
        val warning = CostWarning.BelowProductionCost(Money("15.50"))
        assertTrue(warning.message(Language.Cs).contains("pod výrobními náklady"))
      },
      test("LowMargin English message") {
        val warning = CostWarning.LowMargin(
          Percentage.unsafe(BigDecimal("8")),
          Percentage.unsafe(BigDecimal("15")),
        )
        assertTrue(warning.message(Language.En).contains("below recommended threshold"))
      },
      test("LowMargin Czech message") {
        val warning = CostWarning.LowMargin(
          Percentage.unsafe(BigDecimal("8")),
          Percentage.unsafe(BigDecimal("15")),
        )
        assertTrue(warning.message(Language.Cs).contains("pod doporučeným prahem"))
      },
    ),
  )
