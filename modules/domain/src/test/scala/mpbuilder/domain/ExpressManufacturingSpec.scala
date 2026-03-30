package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.service.*
import mpbuilder.domain.service.CompletionEstimator.*
import mpbuilder.domain.model.ManufacturingWorkflow.*
import mpbuilder.domain.model.ManufacturingOrder.*
import java.time.*

object ExpressManufacturingSpec extends ZIOSpecDefault:

  // ── Shared helpers ──

  private val pricelist = SamplePricelist.pricelist
  private val configId  = ConfigurationId.unsafe("test-express-1")

  private def makeConfig(speed: Option[ManufacturingSpeed], quantity: Int = 500): ProductConfiguration =
    val speedSpec = speed.map(s => SpecValue.ManufacturingSpeedSpec(s)).toList
    val specs = List(
      SpecValue.SizeSpec(Dimension(90, 55)),
      SpecValue.QuantitySpec(Quantity.unsafe(quantity)),
    ) ++ speedSpec
    ProductConfiguration(
      id = configId,
      category = SampleCatalog.businessCards,
      printingMethod = SampleCatalog.offsetMethod,
      components = List(
        ProductComponent(
          role = ComponentRole.Main,
          material = SampleCatalog.coated300gsm,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishes = List.empty,
          sheetCount = 1,
        )
      ),
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  private val hour   = 1000L * 60 * 60
  private val minute = 1000L * 60

  private def makeStep(
      st: StationType,
      status: StepStatus,
      assignedTo: Option[EmployeeId] = None,
      startedAt: Option[Long] = None,
      completedAt: Option[Long] = None,
  ): WorkflowStep = WorkflowStep(
    StepId.unsafe(s"step-${st.toString}-${status.toString}"),
    st, None, Set.empty, status, assignedTo, None, startedAt, completedAt, "",
  )

  private def makeWorkflow(
      steps: List[WorkflowStep],
      status: WorkflowStatus = WorkflowStatus.InProgress,
      priority: Priority = Priority.Normal,
      deadline: Option[Long] = None,
      createdAt: Long = 100000L,
  ): ManufacturingWorkflow =
    ManufacturingWorkflow(
      WorkflowId.unsafe("wf-1"), OrderId.unsafe("o-1"), 0,
      steps, status, priority, deadline, createdAt,
    )

  private def makeOrder(
      workflows: List[ManufacturingWorkflow],
      approval: ApprovalStatus = ApprovalStatus.Approved,
      deadline: Option[Long] = None,
      priority: Priority = Priority.Normal,
  ): ManufacturingOrder =
    ManufacturingOrder(
      null.asInstanceOf[Order], workflows, approval, "", 100000L, deadline, priority,
    )

  // ── Express surcharge rule from sample pricelist ──
  private val expressRule: PricingRule.ManufacturingSpeedSurcharge = PricingRule.ManufacturingSpeedSurcharge(
    tier = ManufacturingSpeed.Express,
    multiplier = BigDecimal("1.35"),
    queueMultiplierThresholds = List(
      QueueThreshold(BigDecimal("0.50"), BigDecimal("0.10")),
      QueueThreshold(BigDecimal("0.70"), BigDecimal("0.15")),
      QueueThreshold(BigDecimal("0.85"), BigDecimal("0.25")),
    ),
  )

  private val standardRule: PricingRule.ManufacturingSpeedSurcharge = PricingRule.ManufacturingSpeedSurcharge(
    tier = ManufacturingSpeed.Standard,
    multiplier = BigDecimal("1.00"),
    queueMultiplierThresholds = List(
      QueueThreshold(BigDecimal("0.70"), BigDecimal("0.05")),
      QueueThreshold(BigDecimal("0.85"), BigDecimal("0.10")),
    ),
  )

  private val economyRule: PricingRule.ManufacturingSpeedSurcharge = PricingRule.ManufacturingSpeedSurcharge(
    tier = ManufacturingSpeed.Economy,
    multiplier = BigDecimal("0.85"),
    queueMultiplierThresholds = List.empty,
  )

  // ── Test suites ──

  def spec = suite("ExpressManufacturing")(
    pricingSuite,
    completionEstimatorSuite,
    utilisationCalculatorSuite,
    queueScorerSuite,
    analyticsTierMetricsSuite,
  )

  // ═══════════════════════════════════════════════════════════════════
  // (a) PriceCalculator with ManufacturingSpeedSurcharge
  // ═══════════════════════════════════════════════════════════════════

  private val pricingSuite = suite("PriceCalculator speed surcharge")(
    test("no ManufacturingSpeed spec → speedSurcharge is None and price unchanged") {
      val config = makeConfig(speed = None, quantity = 500)
      val result = PriceCalculator.calculate(config, pricelist)
      val bd     = result.toEither.toOption.get
      assertTrue(
        result.toEither.isRight,
        bd.speedSurcharge.isEmpty,
        // 500 × $0.12 = $60, tier 250-999 → 0.90 → $54
        bd.total == Money("54.00"),
      )
    },
    test("Express tier with base multiplier 1.35 adds +35% surcharge line item") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Express), quantity = 500)
      val context = PricingContext.default // globalUtilisation = 0 → no queue threshold hit
      val result  = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd      = result.toEither.toOption.get
      assertTrue(
        result.toEither.isRight,
        bd.speedSurcharge.isDefined,
        bd.speedSurcharge.get.label.contains("+35%"),
        // discountedSubtotal = $54.00, surcharge = 54 * 0.35 = $18.90
        bd.speedSurcharge.get.lineTotal == Money("18.90"),
        // total = 54.00 * 1.35 = $72.90
        bd.total == Money("72.90"),
      )
    },
    test("Standard tier with multiplier 1.0 produces no surcharge line item") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Standard), quantity = 500)
      val context = PricingContext.default
      val result  = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd      = result.toEither.toOption.get
      assertTrue(
        result.toEither.isRight,
        bd.speedSurcharge.isEmpty,
        bd.total == Money("54.00"),
      )
    },
    test("Economy tier with multiplier 0.85 produces a −15% discount line item") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Economy), quantity = 500)
      val context = PricingContext.default
      val result  = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd      = result.toEither.toOption.get
      assertTrue(
        result.toEither.isRight,
        bd.speedSurcharge.isDefined,
        bd.speedSurcharge.get.label.contains("-15%"),
        // discountedSubtotal = $54.00 * 0.85 = $45.90
        bd.total == Money("45.90"),
      )
    },
    test("Express at 70% utilisation adds queue threshold adjustment") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Express), quantity = 500)
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.70"),
        busyPeriodMultipliers = List.empty,
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal(2),
      )
      val result = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd     = result.toEither.toOption.get
      // At 70% → thresholds 0.50 (+0.10) and 0.70 (+0.15) fire
      // effective multiplier = 1.35 + 0.10 + 0.15 = 1.60
      assertTrue(
        bd.speedSurcharge.isDefined,
        bd.speedSurcharge.get.label.contains("+60%"),
        // 54.00 * 1.60 = $86.40
        bd.total == Money("86.40"),
      )
    },
    test("Express surcharge is capped at expressSurchargeCap") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Express), quantity = 500)
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.90"),
        busyPeriodMultipliers = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.50"))),
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal("2.00"),
      )
      val result = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd     = result.toEither.toOption.get
      // raw = 1.35 + 0.10 + 0.15 + 0.25 + 0.50 = 2.35, capped at 2.00
      assertTrue(
        bd.speedSurcharge.isDefined,
        bd.speedSurcharge.get.label.contains("+100%"),
        // 54.00 * 2.00 = $108.00
        bd.total == Money("108.00"),
      )
    },
    test("Economy price is fixed regardless of queue utilisation") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Economy), quantity = 500)
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.90"),
        busyPeriodMultipliers = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.50"))),
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal(2),
      )
      val result = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd     = result.toEither.toOption.get
      // Economy always uses base multiplier 0.85, no dynamic adjustments
      assertTrue(
        bd.speedSurcharge.isDefined,
        bd.total == Money("45.90"),
      )
    },
    test("Speed surcharge is applied after quantity discount but before setup fees") {
      // Use a config with a finish that triggers a setup fee
      val config = ProductConfiguration(
        id = configId,
        category = SampleCatalog.businessCards,
        printingMethod = SampleCatalog.offsetMethod,
        components = List(
          ProductComponent(
            role = ComponentRole.Main,
            material = SampleCatalog.coated300gsm,
            inkConfiguration = InkConfiguration.cmyk4_4,
            finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
            sheetCount = 1,
          )
        ),
        specifications = ProductSpecifications.fromSpecs(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        )),
      )
      val context = PricingContext.default
      val result  = PriceCalculator.calculateWithContext(config, pricelist, context)
      val bd      = result.toEither.toOption.get
      // subtotal = 500 * (0.12 + 0.03) = $75.00, qty multiplier 0.90 → discountedSubtotal = $67.50
      // speed surcharge: 67.50 * 0.35 = $23.625 → rounded $23.63
      // afterSpeed: 67.50 * 1.35 = $91.125 → rounded $91.13
      // setup fees added after speed surcharge
      assertTrue(
        bd.speedSurcharge.isDefined,
        bd.quantityMultiplier == BigDecimal("0.90"),
        bd.speedSurcharge.get.lineTotal == Money("23.63"),
      )
    },
    test("backward-compatible calculate method uses default context") {
      val config  = makeConfig(speed = Some(ManufacturingSpeed.Express), quantity = 500)
      val result1 = PriceCalculator.calculate(config, pricelist)
      val result2 = PriceCalculator.calculateWithContext(config, pricelist, PricingContext.default)
      val bd1     = result1.toEither.toOption.get
      val bd2     = result2.toEither.toOption.get
      assertTrue(bd1.total == bd2.total)
    },
  )

  // ═══════════════════════════════════════════════════════════════════
  // (b) CompletionEstimator
  // ═══════════════════════════════════════════════════════════════════

  private val completionEstimatorSuite = suite("CompletionEstimator")(
    suite("estimateProductionTime")(
      test("returns correct sum of station times") {
        val estimates = List(
          StationTimeEstimate(StationType.Prepress, baseTimeMinutes = 10, perUnitSeconds = BigDecimal("0.5"), maxParallelUnits = 100),
          StationTimeEstimate(StationType.DigitalPrinter, baseTimeMinutes = 15, perUnitSeconds = BigDecimal("1.0"), maxParallelUnits = 50),
        )
        val steps = List(StationType.Prepress, StationType.DigitalPrinter)
        val result = CompletionEstimator.estimateProductionTime(steps, 100, estimates)
        // Prepress: 10 + (0.5 * 100 / 60) = 10 + 0.83 → 10 (toLong)
        // DigitalPrinter: 15 + (1.0 * 100 / 60) = 15 + 1.66 → 15 + 1 = 16
        // Total = 10 + 16 = 26
        assertTrue(result == 26L)
      },
      test("uses default 15 minutes for unknown stations") {
        val estimates = List.empty[StationTimeEstimate]
        val steps     = List(StationType.Prepress)
        val result    = CompletionEstimator.estimateProductionTime(steps, 100, estimates)
        assertTrue(result == 15L)
      },
    ),
    suite("estimateQueueWait")(
      test("Express gets position 0 → no queue wait") {
        val queues = Map(
          StationType.Prepress -> StationQueueState(queueDepth = 20, avgProcessingTimeMinutes = 10, activeMachineCount = 2),
        )
        val result = CompletionEstimator.estimateQueueWait(
          List(StationType.Prepress), ManufacturingSpeed.Express, queues,
        )
        assertTrue(result == 0L)
      },
      test("Standard gets normalPosition-based wait") {
        val queues = Map(
          StationType.Prepress -> StationQueueState(queueDepth = 20, avgProcessingTimeMinutes = 10, activeMachineCount = 2),
        )
        val result = CompletionEstimator.estimateQueueWait(
          List(StationType.Prepress), ManufacturingSpeed.Standard, queues,
        )
        // normalPosition = (20 * 0.5).toInt = 10, wait = (10 * 10) / 2 = 50
        assertTrue(result == 50L)
      },
      test("Economy gets full queue depth") {
        val queues = Map(
          StationType.Prepress -> StationQueueState(queueDepth = 20, avgProcessingTimeMinutes = 10, activeMachineCount = 2),
        )
        val result = CompletionEstimator.estimateQueueWait(
          List(StationType.Prepress), ManufacturingSpeed.Economy, queues,
        )
        // position = 20 (totalDepth), wait = (20 * 10) / 2 = 100
        assertTrue(result == 100L)
      },
    ),
    suite("approvalDelay")(
      test("Express returns 30 minutes") {
        assertTrue(CompletionEstimator.approvalDelay(ManufacturingSpeed.Express) == 30L)
      },
      test("Standard returns 180 minutes") {
        assertTrue(CompletionEstimator.approvalDelay(ManufacturingSpeed.Standard) == 180L)
      },
      test("Economy returns 360 minutes") {
        assertTrue(CompletionEstimator.approvalDelay(ManufacturingSpeed.Economy) == 360L)
      },
    ),
    suite("bufferTime")(
      test("Express returns 60 minutes") {
        assertTrue(CompletionEstimator.bufferTime(ManufacturingSpeed.Express) == 60L)
      },
      test("Standard returns 240 minutes") {
        assertTrue(CompletionEstimator.bufferTime(ManufacturingSpeed.Standard) == 240L)
      },
      test("Economy returns 480 minutes") {
        assertTrue(CompletionEstimator.bufferTime(ManufacturingSpeed.Economy) == 480L)
      },
    ),
    suite("advanceByWorkingMinutes")(
      test("correctly handles overnight rollover") {
        val wh = WorkingHours.default // 07:00–17:00 Mon–Fri
        // Start at 16:00 Monday (1 hour before close), advance 120 minutes
        val start = LocalDateTime.of(2025, 1, 6, 16, 0) // Monday
        val result = CompletionEstimator.advanceByWorkingMinutes(start, 120, wh)
        // 60 min left on Monday → rolls over, 60 min left → Tuesday 07:00 + 60 = 08:00
        assertTrue(result == LocalDateTime.of(2025, 1, 7, 8, 0))
      },
      test("correctly handles weekend skipping") {
        val wh = WorkingHours.default
        // Start at 16:00 Friday, advance 120 minutes
        val start = LocalDateTime.of(2025, 1, 10, 16, 0) // Friday
        val result = CompletionEstimator.advanceByWorkingMinutes(start, 120, wh)
        // 60 min left on Friday → rolls over, skip Sat+Sun → Monday 07:00 + 60 = 08:00
        assertTrue(result == LocalDateTime.of(2025, 1, 13, 8, 0))
      },
      test("correctly skips holidays") {
        val holiday = LocalDate.of(2025, 1, 7) // Tuesday is a holiday
        val wh = WorkingHours.default.copy(holidays = Set(holiday))
        // Start at 16:00 Monday, advance 120 minutes
        val start = LocalDateTime.of(2025, 1, 6, 16, 0) // Monday
        val result = CompletionEstimator.advanceByWorkingMinutes(start, 120, wh)
        // 60 min left on Monday → Tuesday is holiday → skip to Wednesday 07:00 + 60 = 08:00
        assertTrue(result == LocalDateTime.of(2025, 1, 8, 8, 0))
      },
      test("zero minutes returns start unchanged") {
        val wh    = WorkingHours.default
        val start = LocalDateTime.of(2025, 1, 6, 10, 0)
        val result = CompletionEstimator.advanceByWorkingMinutes(start, 0, wh)
        assertTrue(result == start)
      },
    ),
    suite("roundToHalfHour")(
      test("minute 0–14 rounds down to :00") {
        val dt = LocalDateTime.of(2025, 1, 6, 10, 12)
        val result = CompletionEstimator.roundToHalfHour(dt)
        assertTrue(result == LocalDateTime.of(2025, 1, 6, 10, 0, 0))
      },
      test("minute 15–44 rounds to :30") {
        val dt = LocalDateTime.of(2025, 1, 6, 10, 30)
        val result = CompletionEstimator.roundToHalfHour(dt)
        assertTrue(result == LocalDateTime.of(2025, 1, 6, 10, 30, 0))
      },
      test("minute 45–59 rounds up to next hour :00") {
        val dt = LocalDateTime.of(2025, 1, 6, 10, 50)
        val result = CompletionEstimator.roundToHalfHour(dt)
        assertTrue(result == LocalDateTime.of(2025, 1, 6, 11, 0, 0))
      },
    ),
    suite("formatDateTime")(
      test("formats today correctly") {
        val now = LocalDateTime.of(2025, 1, 6, 8, 0)
        val dt  = LocalDateTime.of(2025, 1, 6, 14, 30)
        val result = CompletionEstimator.formatDateTime(dt, now, Language.En)
        assertTrue(result == "Today, 14:30")
      },
      test("formats tomorrow correctly") {
        val now = LocalDateTime.of(2025, 1, 6, 8, 0)
        val dt  = LocalDateTime.of(2025, 1, 7, 10, 0)
        val result = CompletionEstimator.formatDateTime(dt, now, Language.En)
        assertTrue(result == "Tomorrow, 10:00")
      },
      test("formats this week correctly") {
        val now = LocalDateTime.of(2025, 1, 6, 8, 0) // Monday
        val dt  = LocalDateTime.of(2025, 1, 10, 9, 0) // Friday (4 days)
        val result = CompletionEstimator.formatDateTime(dt, now, Language.En)
        assertTrue(result == "Friday, 9:00")
      },
      test("formats 2+ weeks as month day") {
        val now = LocalDateTime.of(2025, 1, 6, 8, 0)
        val dt  = LocalDateTime.of(2025, 1, 25, 12, 0) // 19 days later
        val result = CompletionEstimator.formatDateTime(dt, now, Language.En)
        assertTrue(result == "January 25")
      },
      test("formats Czech locale for today") {
        val now = LocalDateTime.of(2025, 1, 6, 8, 0)
        val dt  = LocalDateTime.of(2025, 1, 6, 14, 30)
        val result = CompletionEstimator.formatDateTime(dt, now, Language.Cs)
        assertTrue(result == "Dnes, 14:30")
      },
    ),
  )

  // ═══════════════════════════════════════════════════════════════════
  // (c) UtilisationCalculator
  // ═══════════════════════════════════════════════════════════════════

  private val utilisationCalculatorSuite = suite("UtilisationCalculator")(
    test("computeGlobalUtilisation returns 0 for empty stations") {
      val result = UtilisationCalculator.computeGlobalUtilisation(List.empty)
      assertTrue(result == BigDecimal(0))
    },
    test("computeGlobalUtilisation uses maximum (bottleneck) station") {
      val stations = List(
        StationUtilisation(StationType.Prepress, queueDepth = 4, inProgressCount = 0, machineCount = 2, avgProcessingTimeMs = 1000, estimatedClearTimeMs = 5000),
        StationUtilisation(StationType.DigitalPrinter, queueDepth = 16, inProgressCount = 0, machineCount = 2, avgProcessingTimeMs = 2000, estimatedClearTimeMs = 10000),
      )
      val result = UtilisationCalculator.computeGlobalUtilisation(stations)
      // Prepress: 4 / (2*8) = 0.25
      // DigitalPrinter: 16 / (2*8) = 1.00
      // max = 1.00
      assertTrue(result == BigDecimal(1))
    },
    test("isExpressAvailable returns true below threshold") {
      assertTrue(UtilisationCalculator.isExpressAvailable(BigDecimal("0.80")))
    },
    test("isExpressAvailable returns false at threshold") {
      assertTrue(!UtilisationCalculator.isExpressAvailable(BigDecimal("0.95")))
    },
    test("isExpressAvailable returns false above threshold") {
      assertTrue(!UtilisationCalculator.isExpressAvailable(BigDecimal("1.00")))
    },
    test("computeEffectiveMultiplier returns base for Economy regardless of utilisation") {
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.90"),
        busyPeriodMultipliers = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.20"))),
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal(2),
      )
      val result = UtilisationCalculator.computeEffectiveMultiplier(
        ManufacturingSpeed.Economy, economyRule, context,
      )
      assertTrue(result == BigDecimal("0.85"))
    },
    test("computeEffectiveMultiplier adds queue thresholds for Express") {
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.70"),
        busyPeriodMultipliers = List.empty,
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal(2),
      )
      val result = UtilisationCalculator.computeEffectiveMultiplier(
        ManufacturingSpeed.Express, expressRule, context,
      )
      // base 1.35 + threshold 0.50 → +0.10, threshold 0.70 → +0.15 = 1.60
      assertTrue(result == BigDecimal("1.60"))
    },
    test("computeEffectiveMultiplier caps at expressSurchargeCap") {
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.90"),
        busyPeriodMultipliers = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.50"))),
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal("1.80"),
      )
      val result = UtilisationCalculator.computeEffectiveMultiplier(
        ManufacturingSpeed.Express, expressRule, context,
      )
      // raw = 1.35 + 0.10 + 0.15 + 0.25 + 0.50 = 2.35, capped at 1.80
      assertTrue(result == BigDecimal("1.80"))
    },
    test("computeEffectiveMultiplier adds queue thresholds for Standard at high utilisation") {
      val context = PricingContext(
        globalUtilisation = BigDecimal("0.85"),
        busyPeriodMultipliers = List.empty,
        currentTimeMillis = 0L,
        expressSurchargeCap = BigDecimal(2),
      )
      val result = UtilisationCalculator.computeEffectiveMultiplier(
        ManufacturingSpeed.Standard, standardRule, context,
      )
      // base 1.00 + threshold 0.70 → +0.05, threshold 0.85 → +0.10 = 1.15
      assertTrue(result == BigDecimal("1.15"))
    },
    test("buildPricingContext creates context from station data") {
      val stations = List(
        StationUtilisation(StationType.Prepress, queueDepth = 8, inProgressCount = 0, machineCount = 1, avgProcessingTimeMs = 1000, estimatedClearTimeMs = 5000),
      )
      val busy = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.10")))
      val ctx = UtilisationCalculator.buildPricingContext(stations, busy, 12345L, BigDecimal("2.50"))
      // utilisation = 8 / (1*8) = 1.0
      assertTrue(
        ctx.globalUtilisation == BigDecimal(1),
        ctx.busyPeriodMultipliers == busy,
        ctx.currentTimeMillis == 12345L,
        ctx.expressSurchargeCap == BigDecimal("2.50"),
      )
    },
  )

  // ═══════════════════════════════════════════════════════════════════
  // (d) QueueScorer speed adjustments
  // ═══════════════════════════════════════════════════════════════════

  private def makeQueueScorerWorkflow(
      priority: Priority = Priority.Normal,
      deadline: Option[Long] = None,
      createdAt: Long = 0L,
      completedStepCount: Int = 0,
      totalStepCount: Int = 5,
  ): ManufacturingWorkflow =
    val steps = (0 until totalStepCount).toList.map { i =>
      val status =
        if i < completedStepCount then StepStatus.Completed
        else if i == completedStepCount then StepStatus.Ready
        else StepStatus.Waiting
      WorkflowStep(
        id = StepId.unsafe(s"step-$i"),
        stationType = StationType.values(i % StationType.values.length),
        componentRole = None,
        dependsOn = if i == 0 then Set.empty else Set(StepId.unsafe(s"step-${i - 1}")),
        status = status,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = "",
      )
    }
    ManufacturingWorkflow(
      id = WorkflowId.unsafe("wf-score"),
      orderId = OrderId.unsafe("order-score"),
      orderItemIndex = 0,
      steps = steps,
      status = if completedStepCount > 0 then WorkflowStatus.InProgress else WorkflowStatus.Pending,
      priority = priority,
      deadline = deadline,
      createdAt = createdAt,
    )

  private val queueScorerSuite = suite("QueueScorer speed adjustments")(
    test("Express manufacturing speed adds +50 priority boost") {
      val now = 100 * hour
      val wf  = makeQueueScorerWorkflow(priority = Priority.Normal, createdAt = now - 1 * hour)
      val score = QueueScorer.score(wf, now, manufacturingSpeed = Some(ManufacturingSpeed.Express))
      // Normal priority boost = 0, Express adds +50
      assertTrue(score.priorityBoost == 50)
    },
    test("Economy manufacturing speed adds +10 batch affinity") {
      val now = 100 * hour
      val wf  = makeQueueScorerWorkflow(priority = Priority.Normal, createdAt = now - 1 * hour)
      val score = QueueScorer.score(wf, now, manufacturingSpeed = Some(ManufacturingSpeed.Economy))
      assertTrue(score.batchAffinity == 10)
    },
    test("Standard manufacturing speed makes no additional adjustments") {
      val now = 100 * hour
      val wf  = makeQueueScorerWorkflow(priority = Priority.Normal, createdAt = now - 1 * hour)
      val scoreWithStd = QueueScorer.score(wf, now, manufacturingSpeed = Some(ManufacturingSpeed.Standard))
      val scoreWithout = QueueScorer.score(wf, now, manufacturingSpeed = None)
      assertTrue(
        scoreWithStd.priorityBoost == scoreWithout.priorityBoost,
        scoreWithStd.batchAffinity == scoreWithout.batchAffinity,
      )
    },
    test("No speed (None) makes no adjustments") {
      val now = 100 * hour
      val wf  = makeQueueScorerWorkflow(priority = Priority.Normal, createdAt = now - 1 * hour)
      val score = QueueScorer.score(wf, now, manufacturingSpeed = None)
      // Normal priority = 0, no speed adjustment
      assertTrue(
        score.priorityBoost == 0,
        score.batchAffinity == 0,
      )
    },
    test("Express with Rush priority stacks both boosts") {
      val now = 100 * hour
      val wf  = makeQueueScorerWorkflow(priority = Priority.Rush, createdAt = now - 1 * hour)
      val score = QueueScorer.score(wf, now, manufacturingSpeed = Some(ManufacturingSpeed.Express))
      // Rush boost = 30 + Express boost = 50 → total priority boost = 80
      assertTrue(score.priorityBoost == 80)
    },
  )

  // ═══════════════════════════════════════════════════════════════════
  // (e) AnalyticsService tier metrics
  // ═══════════════════════════════════════════════════════════════════

  private val analyticsTierMetricsSuite = suite("AnalyticsService tier metrics")(
    test("computeTierMetrics returns metrics only for priorities with orders") {
      val steps = List(makeStep(StationType.Prepress, StepStatus.Ready))
      val orders = List(
        makeOrder(List(makeWorkflow(steps, priority = Priority.Rush)), priority = Priority.Rush),
      )
      val result = AnalyticsService.computeTierMetrics(orders)
      assertTrue(
        result.size == 1,
        result.head.priority == Priority.Rush,
      )
    },
    test("computeTierMetrics counts orders and completed orders per tier") {
      val completedSteps = List(
        makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(200L)),
      )
      val pendingSteps = List(makeStep(StationType.Prepress, StepStatus.Ready))

      val rushCompleted = makeOrder(
        List(makeWorkflow(completedSteps, status = WorkflowStatus.Completed, priority = Priority.Rush)),
        priority = Priority.Rush,
      )
      val rushPending = makeOrder(
        List(makeWorkflow(pendingSteps, priority = Priority.Rush)),
        priority = Priority.Rush,
      )
      val normalCompleted = makeOrder(
        List(makeWorkflow(completedSteps, status = WorkflowStatus.Completed, priority = Priority.Normal)),
        priority = Priority.Normal,
      )

      val result = AnalyticsService.computeTierMetrics(List(rushCompleted, rushPending, normalCompleted))
      val rushMetric   = result.find(_.priority == Priority.Rush).get
      val normalMetric = result.find(_.priority == Priority.Normal).get

      assertTrue(
        rushMetric.orderCount == 2,
        rushMetric.completedCount == 1,
        normalMetric.orderCount == 1,
        normalMetric.completedCount == 1,
      )
    },
    test("computeSummary includes tierMetrics") {
      val steps = List(
        makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(300L)),
        makeStep(StationType.DigitalPrinter, StepStatus.Ready),
      )
      val emp1 = EmployeeId.unsafe("emp-1")
      val employees = List(
        Employee(emp1, "Jan Novák", Set(StationType.Prepress), isActive = true),
      )
      val rushOrder = makeOrder(
        List(makeWorkflow(steps, priority = Priority.Rush)),
        priority = Priority.Rush,
      )
      val normalOrder = makeOrder(
        List(makeWorkflow(steps, priority = Priority.Normal)),
        priority = Priority.Normal,
      )
      val summary = AnalyticsService.computeSummary(List(rushOrder, normalOrder), employees)
      assertTrue(
        summary.tierMetrics.nonEmpty,
        summary.tierMetrics.exists(_.priority == Priority.Rush),
        summary.tierMetrics.exists(_.priority == Priority.Normal),
        summary.totalOrders == 2,
      )
    },
    test("computeTierMetrics empty orders returns empty list") {
      val result = AnalyticsService.computeTierMetrics(List.empty)
      assertTrue(result.isEmpty)
    },
  )
