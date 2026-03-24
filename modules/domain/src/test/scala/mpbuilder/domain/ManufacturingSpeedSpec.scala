package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.service.*

object ManufacturingSpeedSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val configId = ConfigurationId.unsafe("test-speed-1")

  private def makeConfig(
      specs: List[SpecValue],
      finishes: List[SelectedFinish] = Nil,
  ): ProductConfiguration =
    ProductConfiguration(
      id = configId,
      category = SampleCatalog.businessCards,
      printingMethod = SampleCatalog.offsetMethod,
      components = List(ProductComponent(
        role = ComponentRole.Main,
        material = SampleCatalog.coated300gsm,
        inkConfiguration = InkConfiguration.cmyk4_4,
        finishes = finishes,
        sheetCount = 1,
      )),
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  def spec = suite("ManufacturingSpeed")(
    suite("ManufacturingSpeed enum")(
      test("toPriority maps correctly") {
        assertTrue(
          ManufacturingSpeed.Express.toPriority == Priority.Rush,
          ManufacturingSpeed.Standard.toPriority == Priority.Normal,
          ManufacturingSpeed.Economy.toPriority == Priority.Low,
        )
      },
      test("displayName provides EN and CS") {
        assertTrue(
          ManufacturingSpeed.Express.displayName(Language.En) == "Express",
          ManufacturingSpeed.Express.displayName(Language.Cs) == "Expresní",
          ManufacturingSpeed.Standard.displayName(Language.En) == "Standard",
          ManufacturingSpeed.Economy.displayName(Language.En) == "Economy",
          ManufacturingSpeed.Economy.displayName(Language.Cs) == "Ekonomická",
        )
      },
    ),

    suite("pricing with manufacturing speed")(
      test("Express multiplier increases price") {
        // 500× coated 300gsm business cards → subtotal 60.00, qty tier 0.90 → discounted 54.00
        // Express 1.35× → 54.00 × 1.35 = 72.90
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          breakdown.speedMultiplier == BigDecimal("1.35"),
          breakdown.speedSurcharge.isDefined,
          breakdown.speedSurcharge.get.label == "Express manufacturing",
          breakdown.total == Money("72.90"),
        )
      },
      test("Standard multiplier leaves price unchanged") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Standard),
        ))

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.00"),
          breakdown.speedSurcharge.isEmpty,
          breakdown.total == Money("54.00"),
        )
      },
      test("Economy multiplier reduces price") {
        // 500× coated 300gsm → discounted subtotal 54.00
        // Economy 0.85× → 54.00 × 0.85 = 45.90
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Economy),
        ))

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("0.85"),
          breakdown.speedSurcharge.isDefined,
          breakdown.speedSurcharge.get.label == "Economy discount",
          breakdown.total == Money("45.90"),
        )
      },
      test("no speed spec defaults to multiplier 1.0") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.speedMultiplier == BigDecimal(1),
          breakdown.speedSurcharge.isEmpty,
          breakdown.total == Money("54.00"),
        )
      },
      test("speed multiplier applies before setup fees") {
        // Use a custom pricelist with setup fees to test ordering
        val customPricelist = Pricelist(
          rules = List(
            PricingRule.MaterialBasePrice(SampleCatalog.coated300gsmId, Money("0.12")),
            PricingRule.FinishSurcharge(SampleCatalog.embossingId, Money("0.08")),
            PricingRule.FinishSetupFee(SampleCatalog.embossingId, Money("100")),
            PricingRule.QuantityTier(1, None, BigDecimal("1.0")),
            PricingRule.ManufacturingSpeedSurcharge(ManufacturingSpeed.Express, BigDecimal("1.50")),
          ),
          currency = Currency.USD,
          version = "test-speed",
        )

        val config = makeConfig(
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
          ),
          finishes = List(SelectedFinish(SampleCatalog.embossing)),
        )

        val result = PriceCalculator.calculate(config, customPricelist)
        val breakdown = result.toEither.toOption.get
        // subtotal = 12.00 (material) + 8.00 (embossing) = 20.00
        // quantity multiplier = 1.0, so discounted = 20.00
        // Express 1.50× → 20.00 × 1.50 = 30.00
        // + setup fee 100.00 = 130.00
        assertTrue(
          breakdown.setupFees.nonEmpty,
          breakdown.setupFees.head.lineTotal == Money("100"),
          breakdown.speedMultiplier == BigDecimal("1.50"),
          breakdown.total == Money("130.00"),
        )
      },
    ),

    suite("tier restrictions")(
      test("Express available within quantity limit") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val result = ManufacturingSpeedValidator.validate(
          config, ManufacturingSpeed.Express, SamplePricelist.tierRestrictions,
        )
        assertTrue(result.toEither.isRight)
      },
      test("Express rejected when quantity exceeds limit") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(5000)),
        ))
        val result = ManufacturingSpeedValidator.validate(
          config, ManufacturingSpeed.Express, SamplePricelist.tierRestrictions,
        )
        assertTrue(
          result.toEither.isLeft,
          result.fold(
            errors => errors.head.isInstanceOf[ManufacturingSpeedValidator.SpeedValidationError.QuantityExceedsLimit],
            _ => false,
          ),
        )
      },
      test("Standard always available") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(100000)),
        ))
        val result = ManufacturingSpeedValidator.validate(
          config, ManufacturingSpeed.Standard, SamplePricelist.tierRestrictions,
        )
        assertTrue(result.toEither.isRight)
      },
      test("Economy always available") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(100000)),
        ))
        val result = ManufacturingSpeedValidator.validate(
          config, ManufacturingSpeed.Economy, SamplePricelist.tierRestrictions,
        )
        assertTrue(result.toEither.isRight)
      },
      test("availableTiers excludes Express when over limit") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(5000)),
        ))
        val tiers = ManufacturingSpeedValidator.availableTiers(config, SamplePricelist.tierRestrictions)
        assertTrue(
          !tiers.contains(ManufacturingSpeed.Express),
          tiers.contains(ManufacturingSpeed.Standard),
          tiers.contains(ManufacturingSpeed.Economy),
        )
      },
      test("validation error messages are localized") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(5000)),
        ))
        val result = ManufacturingSpeedValidator.validate(
          config, ManufacturingSpeed.Express, SamplePricelist.tierRestrictions,
        )
        val errors = result.fold(_.toList, _ => Nil)
        assertTrue(
          errors.head.message(Language.En).contains("not available for quantities over 2000"),
          errors.head.message(Language.Cs).contains("není dostupný pro množství nad 2000"),
        )
      },
    ),

    suite("production time estimation")(
      test("estimates production time for basic business card") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val minutes = ProductionTimeEstimator.estimateProductionMinutes(config, 500)
        // Prepress 30 + DigitalPrinter (15 + ceil(500*0.1/60)) + Cutter (5 + ceil(500*0.2/60)) + QC 15 + Packaging 10
        // Actually uses Offset (45 + ceil(500*0.1/60)=1) = 46
        // Cutter (5 + ceil(500*0.2/60)=2) = 7
        assertTrue(minutes > 0)
      },
      test("Express has shorter total completion estimate than Standard") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val expressMinutes = ProductionTimeEstimator.estimateCompletionMinutes(
          config, 500, ManufacturingSpeed.Express,
        )
        val standardMinutes = ProductionTimeEstimator.estimateCompletionMinutes(
          config, 500, ManufacturingSpeed.Standard,
        )
        val economyMinutes = ProductionTimeEstimator.estimateCompletionMinutes(
          config, 500, ManufacturingSpeed.Economy,
        )
        assertTrue(
          expressMinutes < standardMinutes,
          standardMinutes < economyMinutes,
        )
      },
      test("deriveStepTypes includes correct stations for business card") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val steps = ProductionTimeEstimator.deriveStepTypes(config)
        assertTrue(
          steps.contains(StationType.Prepress),
          steps.contains(StationType.OffsetPress),
          steps.contains(StationType.Cutter),
          steps.contains(StationType.QualityControl),
          steps.contains(StationType.Packaging),
        )
      },
      test("deriveStepTypes includes laminator when lamination finish present") {
        val config = makeConfig(
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
          finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
        )
        val steps = ProductionTimeEstimator.deriveStepTypes(config)
        assertTrue(steps.contains(StationType.Laminator))
      },
      test("working hours calculation rolls over to next day") {
        val schedule = ShopSchedule.default
        // Start at 16:00 (960 min), need 120 min of work
        // Available today: 1020 - 960 = 60 min
        // Remaining: 60 min carried to next day
        // Next day starts at 420 (07:00), finishes at 480 (08:00)
        // Calendar: 60 (today) + (1440-960) overnight + 420 (tomorrow open) + 60 (work) = ...
        val result = ProductionTimeEstimator.workingMinutesToCalendarMinutes(
          workMinutes = 120,
          startMinuteOfDay = 960,
          startDayOfWeek = 1, // Monday
          schedule = schedule,
        )
        // 60 min work today + overnight gap (1440-1020=420 non-work + 420 morning) + 60 min work tomorrow
        assertTrue(result > 120) // Calendar time > work time due to overnight gap
      },
      test("working hours skips weekends") {
        val schedule = ShopSchedule.default
        // Start at 16:00 (960 min) on Friday (day 5), need 120 min of work
        // Available today: 60 min
        // Remaining: 60 min → skips Saturday+Sunday → Monday at 07:00+60 = 08:00
        val result = ProductionTimeEstimator.workingMinutesToCalendarMinutes(
          workMinutes = 120,
          startMinuteOfDay = 960,
          startDayOfWeek = 5, // Friday
          schedule = schedule,
        )
        // Calendar elapsed should include the full weekend
        assertTrue(result > 2 * 1440) // More than 2 days (weekend)
      },
    ),

    suite("WorkflowGenerator integration")(
      test("ManufacturingSpeed.toPriority integrates with WorkflowGenerator") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))
        val speed = config.specifications.get(SpecKind.ManufacturingSpeed).collect {
          case SpecValue.ManufacturingSpeedSpec(s) => s
        }.getOrElse(ManufacturingSpeed.Standard)

        val workflow = WorkflowGenerator.generate(
          config = config,
          orderId = OrderId.unsafe("order-1"),
          orderItemIndex = 0,
          workflowId = WorkflowId.unsafe("wf-1"),
          priority = speed.toPriority,
          createdAt = 1000L,
        )
        assertTrue(
          workflow.priority == Priority.Rush,
        )
      },
      test("Economy tier maps to Low priority") {
        val speed = ManufacturingSpeed.Economy
        val workflow = WorkflowGenerator.generate(
          config = makeConfig(List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          )),
          orderId = OrderId.unsafe("order-2"),
          orderItemIndex = 0,
          workflowId = WorkflowId.unsafe("wf-2"),
          priority = speed.toPriority,
          createdAt = 1000L,
        )
        assertTrue(workflow.priority == Priority.Low)
      },
    ),

    suite("SpecKind and SpecValue integration")(
      test("ManufacturingSpeedSpec maps to ManufacturingSpeed kind") {
        val sv = SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express)
        assertTrue(SpecValue.specKind(sv) == SpecKind.ManufacturingSpeed)
      },
      test("ProductSpecifications stores and retrieves ManufacturingSpeed") {
        val specs = ProductSpecifications.fromSpecs(Seq(
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Economy),
        ))
        val retrieved = specs.get(SpecKind.ManufacturingSpeed)
        assertTrue(
          retrieved.isDefined,
          retrieved.get == SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Economy),
        )
      },
    ),
  )
