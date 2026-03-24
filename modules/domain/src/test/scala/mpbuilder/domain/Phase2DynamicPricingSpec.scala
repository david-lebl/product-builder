package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.service.*

object Phase2DynamicPricingSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val configId = ConfigurationId.unsafe("test-phase2-1")

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

  def spec = suite("Phase 2: Dynamic Pricing and Estimation")(

    // ─── StationUtilisation model ──────────────────────────────────────────

    suite("StationUtilisation")(
      test("utilisationRatio is 0 when queue is empty") {
        import StationUtilisation.*
        val su = StationUtilisation(StationType.DigitalPrinter, queueDepth = 0, inProgressCount = 0, machineCount = 2, avgProcessingTimeMs = 5000)
        assertTrue(su.utilisationRatio == BigDecimal(0))
      },
      test("utilisationRatio reflects load") {
        import StationUtilisation.*
        val su = StationUtilisation(StationType.DigitalPrinter, queueDepth = 6, inProgressCount = 2, machineCount = 2, avgProcessingTimeMs = 5000)
        // (6+2) / (2*4) = 8/8 = 1.0
        assertTrue(su.utilisationRatio == BigDecimal(1))
      },
      test("utilisationRatio is 1 when no machines") {
        import StationUtilisation.*
        val su = StationUtilisation(StationType.Cutter, queueDepth = 3, inProgressCount = 0, machineCount = 0, avgProcessingTimeMs = 5000)
        assertTrue(su.utilisationRatio == BigDecimal(1))
      },
      test("utilisationRatio can exceed 1 when overloaded") {
        import StationUtilisation.*
        val su = StationUtilisation(StationType.DigitalPrinter, queueDepth = 10, inProgressCount = 2, machineCount = 1, avgProcessingTimeMs = 5000)
        // (10+2) / (1*4) = 3.0
        assertTrue(su.utilisationRatio == BigDecimal(3))
      },
      test("estimatedClearTimeMs calculates correctly") {
        import StationUtilisation.*
        val su = StationUtilisation(StationType.DigitalPrinter, queueDepth = 4, inProgressCount = 1, machineCount = 2, avgProcessingTimeMs = 10000)
        // (4+1) * 10000 / 2 = 25000
        assertTrue(su.estimatedClearTimeMs == 25000L)
      },
    ),

    // ─── QueueThreshold & dynamic multiplier ───────────────────────────────

    suite("UtilisationCalculator - dynamic multiplier")(
      test("no thresholds returns base multiplier") {
        val result = UtilisationCalculator.effectiveSpeedMultiplier(
          baseMultiplier = BigDecimal("1.35"),
          thresholds = Nil,
          globalUtil = BigDecimal("0.80"),
        )
        assertTrue(result == BigDecimal("1.35"))
      },
      test("applies highest matching queue threshold") {
        val thresholds = List(
          QueueThreshold(BigDecimal("0.50"), BigDecimal("0.00")),
          QueueThreshold(BigDecimal("0.70"), BigDecimal("0.15")),
          QueueThreshold(BigDecimal("0.85"), BigDecimal("0.40")),
        )
        val result = UtilisationCalculator.effectiveSpeedMultiplier(
          baseMultiplier = BigDecimal("1.35"),
          thresholds = thresholds,
          globalUtil = BigDecimal("0.80"), // matches 0.50 and 0.70, highest applicable is 0.70
        )
        // 1.35 + 0.15 = 1.50
        assertTrue(result == BigDecimal("1.50"))
      },
      test("applies all matching thresholds up to highest") {
        val thresholds = List(
          QueueThreshold(BigDecimal("0.50"), BigDecimal("0.00")),
          QueueThreshold(BigDecimal("0.70"), BigDecimal("0.15")),
          QueueThreshold(BigDecimal("0.85"), BigDecimal("0.40")),
        )
        val result = UtilisationCalculator.effectiveSpeedMultiplier(
          baseMultiplier = BigDecimal("1.35"),
          thresholds = thresholds,
          globalUtil = BigDecimal("0.90"), // matches all three
        )
        // 1.35 + 0.40 = 1.75
        assertTrue(result == BigDecimal("1.75"))
      },
      test("no matching thresholds returns base multiplier") {
        val thresholds = List(
          QueueThreshold(BigDecimal("0.70"), BigDecimal("0.15")),
        )
        val result = UtilisationCalculator.effectiveSpeedMultiplier(
          baseMultiplier = BigDecimal("1.35"),
          thresholds = thresholds,
          globalUtil = BigDecimal("0.50"), // below threshold
        )
        assertTrue(result == BigDecimal("1.35"))
      },
    ),

    // ─── BusyPeriodMultiplier ──────────────────────────────────────────────

    suite("BusyPeriodMultiplier")(
      test("appliesAt matches day of week") {
        import BusyPeriodMultiplier.*
        val bp = BusyPeriodMultiplier(dayOfWeek = Some(Set(1, 5)), monthRange = None, timeAfterMinute = None, additionalMultiplier = BigDecimal("0.05"))
        assertTrue(
          bp.appliesAt(1, 6, 600),  // Monday in June
          bp.appliesAt(5, 6, 600),  // Friday in June
          !bp.appliesAt(3, 6, 600), // Wednesday
        )
      },
      test("appliesAt matches month range") {
        import BusyPeriodMultiplier.*
        val bp = BusyPeriodMultiplier(dayOfWeek = None, monthRange = Some((11, 12)), timeAfterMinute = None, additionalMultiplier = BigDecimal("0.10"))
        assertTrue(
          bp.appliesAt(1, 11, 600),  // November
          bp.appliesAt(1, 12, 600),  // December
          !bp.appliesAt(1, 10, 600), // October
          !bp.appliesAt(1, 1, 600),  // January
        )
      },
      test("appliesAt handles year-wrapping month range") {
        import BusyPeriodMultiplier.*
        val bp = BusyPeriodMultiplier(dayOfWeek = None, monthRange = Some((11, 2)), timeAfterMinute = None, additionalMultiplier = BigDecimal("0.10"))
        assertTrue(
          bp.appliesAt(1, 11, 600),  // November
          bp.appliesAt(1, 12, 600),  // December
          bp.appliesAt(1, 1, 600),   // January
          bp.appliesAt(1, 2, 600),   // February
          !bp.appliesAt(1, 6, 600),  // June
        )
      },
      test("appliesAt matches time after") {
        import BusyPeriodMultiplier.*
        val bp = BusyPeriodMultiplier(dayOfWeek = None, monthRange = None, timeAfterMinute = Some(840), additionalMultiplier = BigDecimal("0.03"))
        assertTrue(
          bp.appliesAt(1, 6, 840),   // Exactly 14:00
          bp.appliesAt(1, 6, 900),   // 15:00
          !bp.appliesAt(1, 6, 600),  // 10:00
        )
      },
      test("appliesAt requires all conditions to match") {
        import BusyPeriodMultiplier.*
        val bp = BusyPeriodMultiplier(dayOfWeek = Some(Set(1)), monthRange = Some((11, 12)), timeAfterMinute = Some(840), additionalMultiplier = BigDecimal("0.10"))
        assertTrue(
          bp.appliesAt(1, 11, 900),   // Monday in Nov after 14:00 ✓
          !bp.appliesAt(3, 11, 900),  // Wednesday in Nov after 14:00 ✗ (wrong day)
          !bp.appliesAt(1, 6, 900),   // Monday in Jun after 14:00 ✗ (wrong month)
          !bp.appliesAt(1, 11, 600),  // Monday in Nov before 14:00 ✗ (wrong time)
        )
      },
      test("busyPeriodExtra sums all matching multipliers") {
        val multipliers = List(
          BusyPeriodMultiplier(dayOfWeek = Some(Set(1)), monthRange = None, timeAfterMinute = None, additionalMultiplier = BigDecimal("0.05")),
          BusyPeriodMultiplier(dayOfWeek = None, monthRange = Some((11, 12)), timeAfterMinute = None, additionalMultiplier = BigDecimal("0.10")),
          BusyPeriodMultiplier(dayOfWeek = None, monthRange = None, timeAfterMinute = Some(840), additionalMultiplier = BigDecimal("0.03")),
        )
        // Monday in November after 14:00 → all three match → 0.05 + 0.10 + 0.03 = 0.18
        val result = UtilisationCalculator.busyPeriodExtra(multipliers, dayOfWeek = 1, month = 11, minuteOfDay = 900)
        assertTrue(result == BigDecimal("0.18"))
      },
    ),

    // ─── computeEffectiveMultiplier ────────────────────────────────────────

    suite("UtilisationCalculator - computeEffectiveMultiplier")(
      test("Economy multiplier never changes regardless of context") {
        val result = UtilisationCalculator.computeEffectiveMultiplier(
          baseMultiplier = BigDecimal("0.85"),
          thresholds = List(QueueThreshold(BigDecimal("0.50"), BigDecimal("0.50"))),
          globalUtil = BigDecimal("0.90"),
          busyMultipliers = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.20"))),
          dayOfWeek = 1, month = 11, minuteOfDay = 900,
          cap = BigDecimal("2.00"),
          speed = ManufacturingSpeed.Economy,
        )
        assertTrue(result == BigDecimal("0.85"))
      },
      test("Express respects surcharge cap") {
        val result = UtilisationCalculator.computeEffectiveMultiplier(
          baseMultiplier = BigDecimal("1.35"),
          thresholds = List(QueueThreshold(BigDecimal("0.50"), BigDecimal("0.80"))),
          globalUtil = BigDecimal("0.90"),
          busyMultipliers = List(BusyPeriodMultiplier(None, None, None, BigDecimal("0.50"))),
          dayOfWeek = 1, month = 1, minuteOfDay = 600,
          cap = BigDecimal("2.00"),
          speed = ManufacturingSpeed.Express,
        )
        // base 1.35 + queue 0.80 + busy 0.50 = 2.65, capped to 2.00
        assertTrue(result == BigDecimal("2.00"))
      },
      test("Standard gets dynamic surcharges") {
        val result = UtilisationCalculator.computeEffectiveMultiplier(
          baseMultiplier = BigDecimal("1.00"),
          thresholds = List(QueueThreshold(BigDecimal("0.70"), BigDecimal("0.05"))),
          globalUtil = BigDecimal("0.80"),
          busyMultipliers = Nil,
          dayOfWeek = 1, month = 1, minuteOfDay = 600,
          cap = BigDecimal("2.00"),
          speed = ManufacturingSpeed.Standard,
        )
        // base 1.00 + queue 0.05 = 1.05
        assertTrue(result == BigDecimal("1.05"))
      },
    ),

    // ─── globalUtilisation ─────────────────────────────────────────────────

    suite("UtilisationCalculator - globalUtilisation")(
      test("returns 0 for empty station list") {
        assertTrue(UtilisationCalculator.globalUtilisation(Nil) == BigDecimal(0))
      },
      test("returns max (bottleneck) station utilisation") {
        val stations = List(
          StationUtilisation(StationType.DigitalPrinter, 2, 1, 2, 5000),
          StationUtilisation(StationType.Cutter, 8, 2, 1, 5000), // higher load
        )
        import StationUtilisation.*
        val result = UtilisationCalculator.globalUtilisation(stations)
        // Cutter: (8+2)/(1*4) = 2.5 is higher than DigitalPrinter: (2+1)/(2*4) = 0.375
        assertTrue(result == stations(1).utilisationRatio)
      },
    ),

    // ─── Express availability ──────────────────────────────────────────────

    suite("UtilisationCalculator - Express availability")(
      test("Express available below 95% utilisation") {
        assertTrue(UtilisationCalculator.isExpressAvailable(BigDecimal("0.80")))
      },
      test("Express unavailable at 95% utilisation") {
        assertTrue(!UtilisationCalculator.isExpressAvailable(BigDecimal("0.95")))
      },
      test("Express unavailable above 95% utilisation") {
        assertTrue(!UtilisationCalculator.isExpressAvailable(BigDecimal("1.20")))
      },
    ),

    // ─── PriceCalculator with PricingContext ───────────────────────────────

    suite("PriceCalculator with dynamic pricing context")(
      test("without PricingContext, uses base multiplier (backward compat)") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))
        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.35"),
          breakdown.total == Money("72.90"),
        )
      },
      test("with high utilisation, Express price increases via queue thresholds") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))
        val ctx = PricingContext(
          globalUtilisation = BigDecimal("0.80"),
          // 0.80 >= 0.70 threshold → +0.15 → effective multiplier = 1.50
        )
        val result = PriceCalculator.calculate(config, pricelist, pricingContext = ctx)
        val breakdown = result.toEither.toOption.get
        // Base subtotal = 54.00, Express 1.50× = 81.00
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.50"),
          breakdown.total == Money("81.00"),
        )
      },
      test("with very high utilisation, Express price hits 85% threshold") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))
        val ctx = PricingContext(
          globalUtilisation = BigDecimal("0.90"),
          // 0.90 >= 0.85 threshold → +0.40 → effective multiplier = 1.75
        )
        val result = PriceCalculator.calculate(config, pricelist, pricingContext = ctx)
        val breakdown = result.toEither.toOption.get
        // Base subtotal = 54.00, Express 1.75× = 94.50
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.75"),
          breakdown.total == Money("94.50"),
        )
      },
      test("Economy price unaffected by high utilisation") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Economy),
        ))
        val ctx = PricingContext(globalUtilisation = BigDecimal("0.90"))
        val result = PriceCalculator.calculate(config, pricelist, pricingContext = ctx)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("0.85"),
          breakdown.total == Money("45.90"),
        )
      },
      test("Standard gets mild surcharge at high utilisation") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Standard),
        ))
        val ctx = PricingContext(
          globalUtilisation = BigDecimal("0.80"),
          // Standard: 0.80 >= 0.70 threshold → +0.05 → 1.05
        )
        val result = PriceCalculator.calculate(config, pricelist, pricingContext = ctx)
        val breakdown = result.toEither.toOption.get
        // 54.00 × 1.05 = 56.70
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.05"),
          breakdown.total == Money("56.70"),
        )
      },
      test("busy period multipliers increase Express price") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))
        val ctx = PricingContext(
          globalUtilisation = BigDecimal("0.40"), // below queue thresholds
          busyPeriodMultipliers = SamplePricelist.busyPeriodMultipliers,
          currentDayOfWeek = Some(1),   // Monday
          currentMonth = Some(11),      // November
          currentMinuteOfDay = Some(900), // 15:00
        )
        val result = PriceCalculator.calculate(config, pricelist, pricingContext = ctx)
        val breakdown = result.toEither.toOption.get
        // No queue threshold hit (0.40 < 0.50)
        // Busy period: Monday +0.05, Nov +0.10, after 14:00 +0.03 = +0.18
        // Express multiplier = 1.35 + 0.18 = 1.53
        // 54.00 × 1.53 = 82.62
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.53"),
          breakdown.total == Money("82.62"),
        )
      },
      test("Express surcharge capped at 2.0") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        ))
        val ctx = PricingContext(
          globalUtilisation = BigDecimal("0.90"), // +0.40 from queue
          busyPeriodMultipliers = SamplePricelist.busyPeriodMultipliers,
          currentDayOfWeek = Some(1),   // Monday: +0.05
          currentMonth = Some(11),      // Nov: +0.10
          currentMinuteOfDay = Some(900), // After 14:00: +0.03
          expressSurchargeCap = BigDecimal("2.00"),
        )
        val result = PriceCalculator.calculate(config, pricelist, pricingContext = ctx)
        val breakdown = result.toEither.toOption.get
        // 1.35 + 0.40 (queue) + 0.18 (busy) = 1.93, within cap
        // 54.00 × 1.93 = 104.22
        assertTrue(
          breakdown.speedMultiplier == BigDecimal("1.93"),
          breakdown.total == Money("104.22"),
        )
      },
    ),

    // ─── Queue-aware completion estimation ─────────────────────────────────

    suite("ProductionTimeEstimator - queue-aware estimation")(
      test("queue wait increases completion time for Economy") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val queueStates = List(
          StationQueueState(StationType.OffsetPress, normalPosition = 3, totalDepth = 8, avgProcessingTimeMs = 600000, activeMachineCount = 1),
          StationQueueState(StationType.Cutter, normalPosition = 1, totalDepth = 4, avgProcessingTimeMs = 300000, activeMachineCount = 1),
        )
        val withoutQueue = ProductionTimeEstimator.estimateCompletionMinutes(config, 500, ManufacturingSpeed.Economy)
        val withQueue = ProductionTimeEstimator.estimateCompletionWithQueueMinutes(config, 500, ManufacturingSpeed.Economy, queueStates)
        assertTrue(withQueue > withoutQueue) // Queue wait adds time
      },
      test("Express has zero queue wait") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val queueStates = List(
          StationQueueState(StationType.OffsetPress, normalPosition = 3, totalDepth = 8, avgProcessingTimeMs = 600000, activeMachineCount = 1),
        )
        val expressWait = ProductionTimeEstimator.estimateQueueWaitMinutes(StationType.OffsetPress, ManufacturingSpeed.Express, queueStates)
        assertTrue(expressWait == 0)
      },
      test("Standard waits behind Rush items only") {
        val queueStates = List(
          StationQueueState(StationType.OffsetPress, normalPosition = 2, totalDepth = 8, avgProcessingTimeMs = 600000, activeMachineCount = 1),
        )
        val standardWait = ProductionTimeEstimator.estimateQueueWaitMinutes(StationType.OffsetPress, ManufacturingSpeed.Standard, queueStates)
        // position 2 * 600000ms / 1 machine = 1200000ms = 20 minutes
        assertTrue(standardWait == 20)
      },
      test("Economy waits at end of queue") {
        val queueStates = List(
          StationQueueState(StationType.OffsetPress, normalPosition = 2, totalDepth = 8, avgProcessingTimeMs = 600000, activeMachineCount = 1),
        )
        val economyWait = ProductionTimeEstimator.estimateQueueWaitMinutes(StationType.OffsetPress, ManufacturingSpeed.Economy, queueStates)
        // position 8 * 600000ms / 1 machine = 4800000ms = 80 minutes
        assertTrue(economyWait == 80)
      },
      test("multiple machines reduce queue wait time") {
        val queueStates = List(
          StationQueueState(StationType.OffsetPress, normalPosition = 2, totalDepth = 8, avgProcessingTimeMs = 600000, activeMachineCount = 2),
        )
        val economyWait = ProductionTimeEstimator.estimateQueueWaitMinutes(StationType.OffsetPress, ManufacturingSpeed.Economy, queueStates)
        // position 8 * 600000ms / 2 machines = 2400000ms = 40 minutes
        assertTrue(economyWait == 40)
      },
    ),

    // ─── Express cutoff enforcement ────────────────────────────────────────

    suite("ProductionTimeEstimator - Express cutoff enforcement")(
      test("before Express cutoff, no adjustment") {
        val schedule = ShopSchedule.default // Express cutoff at 14:00 (840 min)
        val (minute, day) = ProductionTimeEstimator.enforceCutoff(
          ManufacturingSpeed.Express, startMinuteOfDay = 600, startDayOfWeek = 1, schedule,
        )
        assertTrue(minute == 600, day == 1)
      },
      test("after Express cutoff, shifts to next business day opening") {
        val schedule = ShopSchedule.default // Express cutoff at 14:00 (840 min)
        val (minute, day) = ProductionTimeEstimator.enforceCutoff(
          ManufacturingSpeed.Express, startMinuteOfDay = 900, startDayOfWeek = 1, schedule,
        )
        assertTrue(minute == 420, day == 2) // 07:00 Tuesday
      },
      test("after Express cutoff on Friday, shifts to Monday opening") {
        val schedule = ShopSchedule.default
        val (minute, day) = ProductionTimeEstimator.enforceCutoff(
          ManufacturingSpeed.Express, startMinuteOfDay = 900, startDayOfWeek = 5, schedule,
        )
        assertTrue(minute == 420, day == 1) // 07:00 Monday
      },
      test("Standard has its own cutoff time") {
        val schedule = ShopSchedule.default // Standard cutoff at 16:00 (960 min)
        val (minute, day) = ProductionTimeEstimator.enforceCutoff(
          ManufacturingSpeed.Standard, startMinuteOfDay = 900, startDayOfWeek = 1, schedule,
        )
        // 900 < 960, so no adjustment
        assertTrue(minute == 900, day == 1)
      },
      test("Standard after cutoff shifts to next day") {
        val schedule = ShopSchedule.default
        val (minute, day) = ProductionTimeEstimator.enforceCutoff(
          ManufacturingSpeed.Standard, startMinuteOfDay = 970, startDayOfWeek = 1, schedule,
        )
        assertTrue(minute == 420, day == 2)
      },
      test("Economy has no cutoff") {
        val schedule = ShopSchedule.default
        val (minute, day) = ProductionTimeEstimator.enforceCutoff(
          ManufacturingSpeed.Economy, startMinuteOfDay = 1000, startDayOfWeek = 1, schedule,
        )
        assertTrue(minute == 1000, day == 1) // no adjustment
      },
    ),

    // ─── Calendar-aware completion ─────────────────────────────────────────

    suite("ProductionTimeEstimator - calendar completion with cutoff")(
      test("Express after cutoff takes longer due to next-day shift") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val schedule = ShopSchedule.default
        // Before cutoff
        val beforeCutoff = ProductionTimeEstimator.estimateCalendarCompletionMinutes(
          config, 500, ManufacturingSpeed.Express, startMinuteOfDay = 600, startDayOfWeek = 1, schedule,
        )
        // After cutoff
        val afterCutoff = ProductionTimeEstimator.estimateCalendarCompletionMinutes(
          config, 500, ManufacturingSpeed.Express, startMinuteOfDay = 900, startDayOfWeek = 1, schedule,
        )
        assertTrue(afterCutoff > beforeCutoff)
      },
      test("queue states increase calendar completion time") {
        val config = makeConfig(List(
          SpecValue.SizeSpec(Dimension(90, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ))
        val schedule = ShopSchedule.default
        val queueStates = List(
          StationQueueState(StationType.OffsetPress, normalPosition = 3, totalDepth = 10, avgProcessingTimeMs = 600000, activeMachineCount = 1),
        )
        val withoutQueue = ProductionTimeEstimator.estimateCalendarCompletionMinutes(
          config, 500, ManufacturingSpeed.Standard, 600, 1, schedule,
        )
        val withQueue = ProductionTimeEstimator.estimateCalendarCompletionMinutes(
          config, 500, ManufacturingSpeed.Standard, 600, 1, schedule, queueStates = queueStates,
        )
        assertTrue(withQueue > withoutQueue)
      },
    ),

    // ─── UtilisationCalculator.calculateStationUtilisation ─────────────────

    suite("UtilisationCalculator - station utilisation from orders")(
      test("calculates utilisation from manufacturing orders and machines") {
        val orderId = OrderId.unsafe("test-order")
        val wfId = WorkflowId.unsafe("wf-1")
        val step1 = WorkflowStep(
          id = StepId.unsafe("s1"), stationType = StationType.DigitalPrinter,
          componentRole = Some(ComponentRole.Main), dependsOn = Set.empty,
          status = StepStatus.Ready, assignedTo = None, assignedMachine = None,
          startedAt = None, completedAt = None, notes = "",
        )
        val step2 = step1.copy(id = StepId.unsafe("s2"), status = StepStatus.Waiting)
        val step3 = step1.copy(id = StepId.unsafe("s3"), status = StepStatus.InProgress)
        val step4 = step1.copy(id = StepId.unsafe("s4"), status = StepStatus.Completed, startedAt = Some(0), completedAt = Some(10000))
        val workflow = ManufacturingWorkflow(
          id = wfId, orderId = orderId, orderItemIndex = 0,
          steps = List(step1, step2, step3, step4),
          status = WorkflowStatus.InProgress, priority = Priority.Normal,
          deadline = None, createdAt = 0L,
        )
        val order = ManufacturingOrder(
          order = null.asInstanceOf[Order],
          workflows = List(workflow),
          approvalStatus = ApprovalStatus.Approved,
          approvalNotes = "",
          createdAt = 0L,
          deadline = None,
        )
        val machines = List(
          Machine(MachineId.unsafe("m1"), "Printer 1", StationType.DigitalPrinter, MachineStatus.Online, ""),
          Machine(MachineId.unsafe("m2"), "Printer 2", StationType.DigitalPrinter, MachineStatus.Online, ""),
        )
        val utilisations = UtilisationCalculator.calculateStationUtilisation(List(order), machines)
        val printerUtil = utilisations.find(_.stationType == StationType.DigitalPrinter).get
        assertTrue(
          printerUtil.queueDepth == 2,      // Ready + Waiting
          printerUtil.inProgressCount == 1,  // InProgress
          printerUtil.machineCount == 2,     // 2 online machines
          printerUtil.avgProcessingTimeMs == 10000L, // 10s from completed step
        )
      },
    ),
  )
