package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*

object ProductionCostCalculatorSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val costSheet = SampleProductionCosts.costSheet
  private val costSheetNoOverhead = SampleProductionCosts.costSheetNoOverhead
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

  /** Standard test config: 500x coated 300gsm business cards with matte lamination, offset. */
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

  /** Area-based config: 10x vinyl banners 1000x500mm with UV coating, UV inkjet. */
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

  def spec = suite("ProductionCostCalculator")(
    suite("calculateCost")(
      test("calculates cost for base-priced material with finish and overhead") {
        val result = ProductionCostCalculator.calculateCost(standardConfig, costSheet)
        val cost = result.toEither.toOption.get
        // Material: 0.05 * 500 = 25.00
        // Finish (matte lam): 0.01 * 500 = 5.00
        // Process (offset): 0.01 * 500 = 5.00
        // Raw total: 35.00
        // With 1.15 overhead: 35.00 * 1.15 = 40.25
        assertTrue(cost == Money("40.25"))
      },
      test("calculates cost for area-priced material") {
        val result = ProductionCostCalculator.calculateCost(areaConfig, costSheet)
        val cost = result.toEither.toOption.get
        // Material: vinyl 8.00/sqm, area = 1000*500/1_000_000 = 0.5 sqm, unitCost = 4.00, total = 4.00 * 10 = 40.00
        // Finish (UV coating): 0.02 * 10 = 0.20
        // Process (UV inkjet): 0.03 * 10 = 0.30
        // Raw total: 40.50
        // With 1.15 overhead: 40.50 * 1.15 = 46.575 → 46.58
        assertTrue(cost == Money("46.58"))
      },
      test("calculates cost without overhead factor") {
        val result = ProductionCostCalculator.calculateCost(standardConfig, costSheetNoOverhead)
        val cost = result.toEither.toOption.get
        // Material: 0.05 * 500 = 25.00
        // Finish (matte lam): 0.01 * 500 = 5.00
        // Process (offset): 0.01 * 500 = 5.00
        // Raw total: 35.00 (no overhead)
        assertTrue(cost == Money("35.00"))
      },
      test("returns zero cost for material not in cost sheet") {
        val unknownMaterial = SampleCatalog.coated300gsm.copy(
          id = MaterialId.unsafe("mat-unknown")
        )
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = unknownMaterial,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = ProductionCostCalculator.calculateCost(config, costSheet)
        val cost = result.toEither.toOption.get
        // Only process cost: offset 0.01 * 100 = 1.00, with 1.15 overhead = 1.15
        assertTrue(cost == Money("1.15"))
      },
      test("fails when quantity is missing") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          inkConfig = InkConfiguration.cmyk4_4,
          finishes = List.empty,
          specs = List(SpecValue.SizeSpec(Dimension(90, 55))),
        )
        val result = ProductionCostCalculator.calculateCost(config, costSheet)
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("analyze")(
      test("healthy margin produces no warnings") {
        val result = ProductionCostCalculator.analyze(standardConfig, pricelist, costSheet)
        val analysis = result.toEither.toOption.get
        assertTrue(
          analysis.productionCost == Money("40.25"),
          analysis.sellingPrice.value > analysis.productionCost.value,
          !analysis.isBelowCost,
          analysis.margin.value > BigDecimal(0),
          analysis.warnings.isEmpty,
        )
      },
      test("below-cost scenario produces BelowProductionCost warning") {
        // Create an extremely expensive cost sheet to force below-cost
        val expensiveCostSheet = ProductionCostSheet(
          rules = List(
            ProductionCostRule.MaterialUnitCost(SampleCatalog.coated300gsmId, Money("5.00")),
            ProductionCostRule.ProcessCost(PrintingProcessType.Offset, Money("1.00")),
            ProductionCostRule.FinishCost(SampleCatalog.matteLaminationId, Money("1.00")),
          ),
          currency = Currency.USD,
        )
        val result = ProductionCostCalculator.analyze(standardConfig, pricelist, expensiveCostSheet)
        val analysis = result.toEither.toOption.get
        // Material cost: 5.00 * 500 = 2500, finish: 1.00 * 500 = 500, process: 1.00 * 500 = 500
        // Total cost: 3500 (no overhead)
        // Selling price is much less than 3500
        assertTrue(
          analysis.isBelowCost,
          analysis.margin.value < BigDecimal(0),
          analysis.warnings.exists(_.isInstanceOf[CostWarning.BelowProductionCost]),
        )
      },
      test("low margin produces LowMargin warning") {
        // Create a cost sheet that produces costs just below the selling price
        // Standard config selling price at 500 qty with 0.90 multiplier
        // Let's calculate: base price = 0.12 * 500 = 60, finish = 0.03 * 500 = 15, subtotal = 75
        // With 0.90 multiplier = 67.50, the selling total ≈ 67.50
        // We want cost to be around 90% of selling price → ~60
        val tightCostSheet = ProductionCostSheet(
          rules = List(
            ProductionCostRule.MaterialUnitCost(SampleCatalog.coated300gsmId, Money("0.10")),
            ProductionCostRule.ProcessCost(PrintingProcessType.Offset, Money("0.01")),
            ProductionCostRule.FinishCost(SampleCatalog.matteLaminationId, Money("0.01")),
          ),
          currency = Currency.USD,
        )
        val result = ProductionCostCalculator.analyze(standardConfig, pricelist, tightCostSheet)
        val analysis = result.toEither.toOption.get
        assertTrue(
          !analysis.isBelowCost,
          analysis.marginPercentage.value < BigDecimal(20),
          analysis.warnings.exists(_.isInstanceOf[CostWarning.LowMargin]),
        )
      },
      test("custom low-margin threshold triggers warning") {
        val result = ProductionCostCalculator.analyze(
          standardConfig,
          pricelist,
          costSheet,
          lowMarginThreshold = Percentage.unsafe(BigDecimal(80)),
        )
        val analysis = result.toEither.toOption.get
        // Even with a healthy margin, 80% threshold should trigger a warning
        // unless the margin is extraordinarily high
        assertTrue(
          analysis.warnings.exists(_.isInstanceOf[CostWarning.LowMargin]),
        )
      },
      test("margin analysis with customer discounts") {
        // Apply a 50% global discount to reduce selling price
        val customerPricing = CustomerPricing(
          globalDiscount = Some(Percentage.unsafe(BigDecimal(50))),
        )
        val customerPricelist = CustomerPricelistResolver.resolve(pricelist, customerPricing)
        val result = ProductionCostCalculator.analyze(standardConfig, customerPricelist, costSheet)
        val analysis = result.toEither.toOption.get
        // With 50% discount, selling price is halved, margin should be tighter
        val baseResult = ProductionCostCalculator.analyze(standardConfig, pricelist, costSheet)
        val baseAnalysis = baseResult.toEither.toOption.get
        assertTrue(
          analysis.sellingPrice.value < baseAnalysis.sellingPrice.value,
          analysis.marginPercentage.value < baseAnalysis.marginPercentage.value,
        )
      },
    ),
    suite("CostWarning messages")(
      test("BelowProductionCost has bilingual messages") {
        val warning = CostWarning.BelowProductionCost(Money("10.50"))
        assertTrue(
          warning.message(Language.En).contains("below production cost"),
          warning.message(Language.Cs).contains("pod výrobními náklady"),
        )
      },
      test("LowMargin has bilingual messages") {
        val warning = CostWarning.LowMargin(Percentage.unsafe(BigDecimal(8)), Percentage.unsafe(BigDecimal(20)))
        assertTrue(
          warning.message(Language.En).contains("below threshold"),
          warning.message(Language.Cs).contains("pod prahem"),
        )
      },
    ),
  )
