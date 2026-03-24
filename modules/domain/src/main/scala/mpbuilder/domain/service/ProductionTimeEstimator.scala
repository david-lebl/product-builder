package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Estimates production time for a product configuration.
  *
  * Phase 1: Static estimation based on station time estimates and working hours.
  * No live queue data — uses base production time + tier-specific buffers.
  */
object ProductionTimeEstimator:

  /** Default station time estimates (configurable per shop). */
  val defaultStationEstimates: List[StationTimeEstimate] = List(
    StationTimeEstimate(StationType.Prepress, baseTimeMinutes = 30, perUnitSeconds = BigDecimal(0)),
    StationTimeEstimate(StationType.DigitalPrinter, baseTimeMinutes = 15, perUnitSeconds = BigDecimal("0.5")),
    StationTimeEstimate(StationType.OffsetPress, baseTimeMinutes = 45, perUnitSeconds = BigDecimal("0.1")),
    StationTimeEstimate(StationType.LargeFormatPrinter, baseTimeMinutes = 20, perUnitSeconds = BigDecimal("2.0")),
    StationTimeEstimate(StationType.Letterpress, baseTimeMinutes = 30, perUnitSeconds = BigDecimal("1.0")),
    StationTimeEstimate(StationType.Cutter, baseTimeMinutes = 5, perUnitSeconds = BigDecimal("0.2")),
    StationTimeEstimate(StationType.Laminator, baseTimeMinutes = 10, perUnitSeconds = BigDecimal("0.3")),
    StationTimeEstimate(StationType.UVCoater, baseTimeMinutes = 10, perUnitSeconds = BigDecimal("0.3")),
    StationTimeEstimate(StationType.EmbossingFoil, baseTimeMinutes = 15, perUnitSeconds = BigDecimal("0.5")),
    StationTimeEstimate(StationType.Folder, baseTimeMinutes = 5, perUnitSeconds = BigDecimal("0.3")),
    StationTimeEstimate(StationType.Binder, baseTimeMinutes = 10, perUnitSeconds = BigDecimal("0.5")),
    StationTimeEstimate(StationType.LargeFormatFinishing, baseTimeMinutes = 15, perUnitSeconds = BigDecimal("1.0")),
    StationTimeEstimate(StationType.QualityControl, baseTimeMinutes = 15, perUnitSeconds = BigDecimal(0)),
    StationTimeEstimate(StationType.Packaging, baseTimeMinutes = 10, perUnitSeconds = BigDecimal(0)),
  )

  /** Estimate total production time in minutes for a configuration.
    *
    * Derives the workflow steps from the configuration and sums up
    * station time estimates for each step.
    */
  def estimateProductionMinutes(
      config: ProductConfiguration,
      quantity: Int,
      stationEstimates: List[StationTimeEstimate] = defaultStationEstimates,
  ): Int =
    val steps = deriveStepTypes(config)
    steps.map { stationType =>
      estimateStepMinutes(stationType, quantity, stationEstimates)
    }.sum

  /** Estimate completion time in minutes from order placement.
    *
    * Includes approval delay, production time, and buffer based on tier.
    */
  def estimateCompletionMinutes(
      config: ProductConfiguration,
      quantity: Int,
      speed: ManufacturingSpeed,
      stationEstimates: List[StationTimeEstimate] = defaultStationEstimates,
  ): Int =
    val productionMinutes = estimateProductionMinutes(config, quantity, stationEstimates)
    val approvalMinutes = approvalDelay(speed)
    val bufferMinutes = buffer(speed)
    approvalMinutes + productionMinutes + bufferMinutes

  /** Phase 2: Queue-aware completion estimation.
    *
    * Includes queue wait time per station based on the tier's queue position.
    * Express orders jump to front, Standard waits after Rush, Economy goes to end.
    */
  def estimateCompletionWithQueueMinutes(
      config: ProductConfiguration,
      quantity: Int,
      speed: ManufacturingSpeed,
      queueStates: List[StationQueueState],
      stationEstimates: List[StationTimeEstimate] = defaultStationEstimates,
  ): Int =
    val steps = deriveStepTypes(config)
    val productionMinutes = steps.map { stationType =>
      estimateStepMinutes(stationType, quantity, stationEstimates)
    }.sum
    val queueWaitMinutes = steps.map { stationType =>
      estimateQueueWaitMinutes(stationType, speed, queueStates)
    }.sum
    val approvalMinutes = approvalDelay(speed)
    val bufferMinutes = buffer(speed)
    approvalMinutes + productionMinutes + queueWaitMinutes + bufferMinutes

  /** Estimate queue wait time in minutes for a single station. */
  def estimateQueueWaitMinutes(
      stationType: StationType,
      speed: ManufacturingSpeed,
      queueStates: List[StationQueueState],
  ): Int =
    queueStates.find(_.stationType == stationType) match
      case None => 0
      case Some(qs) =>
        if qs.avgProcessingTimeMs <= 0 || qs.activeMachineCount <= 0 then 0
        else
          val position = speed match
            case ManufacturingSpeed.Express  => 0                // front of queue
            case ManufacturingSpeed.Standard => qs.normalPosition // after Rush items
            case ManufacturingSpeed.Economy  => qs.totalDepth    // end of queue
          val waitMs = (position.toLong * qs.avgProcessingTimeMs) / qs.activeMachineCount.toLong
          (waitMs / 60000).toInt // convert ms to minutes

  /** Phase 2: Express cutoff enforcement.
    *
    * If Express is selected and the current time is past the Express cutoff,
    * the estimate shifts to start at the next business day morning.
    * Similarly for Standard with the standard cutoff.
    *
    * @return adjusted startMinuteOfDay and startDayOfWeek after cutoff enforcement
    */
  def enforceCutoff(
      speed: ManufacturingSpeed,
      startMinuteOfDay: Int,
      startDayOfWeek: Int,
      schedule: ShopSchedule,
  ): (Int, Int) =
    val cutoff = speed match
      case ManufacturingSpeed.Express  => Some(schedule.expressCutoffMinute)
      case ManufacturingSpeed.Standard => Some(schedule.standardCutoffMinute)
      case ManufacturingSpeed.Economy  => None // no cutoff for Economy
    cutoff match
      case Some(cutoffMinute) if startMinuteOfDay >= cutoffMinute =>
        // Past cutoff — shift to next business day opening
        val wh = schedule.workingHours
        val (calAdv, nextDay, _) = advanceToNextWorkStart(startMinuteOfDay, startDayOfWeek, wh)
        (wh.openTime, nextDay)
      case _ =>
        (startMinuteOfDay, startDayOfWeek)

  /** Phase 2: Full calendar-aware completion estimate with queue and cutoff.
    *
    * Computes the estimated calendar minutes from now until completion,
    * accounting for queue wait times, working hours, weekends, and cutoff enforcement.
    */
  def estimateCalendarCompletionMinutes(
      config: ProductConfiguration,
      quantity: Int,
      speed: ManufacturingSpeed,
      startMinuteOfDay: Int,
      startDayOfWeek: Int,
      schedule: ShopSchedule,
      queueStates: List[StationQueueState] = Nil,
      stationEstimates: List[StationTimeEstimate] = defaultStationEstimates,
  ): Int =
    // Enforce cutoff — may shift start to next business day
    val (adjustedMinute, adjustedDay) = enforceCutoff(speed, startMinuteOfDay, startDayOfWeek, schedule)

    // Calculate the calendar gap from original start to adjusted start
    val cutoffGap = if adjustedMinute == startMinuteOfDay && adjustedDay == startDayOfWeek then 0
    else
      // Count calendar minutes from original position to the adjusted position
      val wh = schedule.workingHours
      var gap = 0
      var curMin = startMinuteOfDay
      var curDay = startDayOfWeek
      // Advance to end of current day
      gap += (1440 - curMin)
      curMin = 0
      curDay = if curDay == 7 then 1 else curDay + 1
      // Skip non-working days
      while curDay != adjustedDay do
        gap += 1440
        curDay = if curDay == 7 then 1 else curDay + 1
      // Add time to adjusted minute
      gap += adjustedMinute
      gap

    // Calculate work minutes including queue wait
    val workMinutes = if queueStates.nonEmpty then
      estimateCompletionWithQueueMinutes(config, quantity, speed, queueStates, stationEstimates)
    else
      estimateCompletionMinutes(config, quantity, speed, stationEstimates)

    // Convert work minutes to calendar minutes respecting working hours, starting from adjusted time
    cutoffGap + workingMinutesToCalendarMinutes(workMinutes, adjustedMinute, adjustedDay, schedule)

  /** Compute estimated completion as working-day-aware minute offset.
    *
    * Given a starting time (minutes from midnight on a given day-of-week 1-7),
    * advances through working hours to find when the given number of work minutes
    * will be completed.
    *
    * Returns the total calendar minutes elapsed (including non-working time).
    */
  def workingMinutesToCalendarMinutes(
      workMinutes: Int,
      startMinuteOfDay: Int,
      startDayOfWeek: Int,
      schedule: ShopSchedule,
  ): Int =
    val wh = schedule.workingHours
    var remainingWork = workMinutes
    var calendarElapsed = 0
    var currentMinute = startMinuteOfDay
    var currentDay = startDayOfWeek

    // If starting outside working hours, advance to next working period
    if !wh.workDays.contains(currentDay) || currentMinute >= wh.closeTime then
      val (advCal, advDay, advMin) = advanceToNextWorkStart(currentMinute, currentDay, wh)
      calendarElapsed += advCal
      currentDay = advDay
      currentMinute = advMin
    else if currentMinute < wh.openTime then
      calendarElapsed += (wh.openTime - currentMinute)
      currentMinute = wh.openTime

    while remainingWork > 0 do
      val availableToday = wh.closeTime - currentMinute
      if remainingWork <= availableToday then
        calendarElapsed += remainingWork
        remainingWork = 0
      else
        remainingWork -= availableToday
        calendarElapsed += availableToday
        // Advance to next working day
        val (advCal, advDay, _) = advanceToNextWorkStart(wh.closeTime, currentDay, wh)
        calendarElapsed += advCal
        currentDay = advDay
        currentMinute = wh.openTime

    calendarElapsed

  /** Approval delay in minutes per tier. */
  def approvalDelay(speed: ManufacturingSpeed): Int = speed match
    case ManufacturingSpeed.Express  => 30    // fast-tracked, ~30 min
    case ManufacturingSpeed.Standard => 120   // 2h normal queue
    case ManufacturingSpeed.Economy  => 360   // 6h batched

  /** Buffer time in minutes per tier. */
  def buffer(speed: ManufacturingSpeed): Int = speed match
    case ManufacturingSpeed.Express  => 60     // 1h safety margin
    case ManufacturingSpeed.Standard => 240    // 4h safety margin
    case ManufacturingSpeed.Economy  => 480    // 1 business day (8h)

  /** Derive the list of station types that a configuration would pass through.
    * Simplified version of WorkflowGenerator logic for time estimation.
    */
  def deriveStepTypes(config: ProductConfiguration): List[StationType] =
    val steps = List.newBuilder[StationType]

    // Prepress is always first
    steps += StationType.Prepress

    // Per-component steps
    config.components.foreach { comp =>
      // Printing
      val printStation = config.printingMethod.processType match
        case PrintingProcessType.Digital                                          => StationType.DigitalPrinter
        case PrintingProcessType.Offset                                          => StationType.OffsetPress
        case PrintingProcessType.UVCurableInkjet                                 => StationType.LargeFormatPrinter
        case PrintingProcessType.Letterpress                                     => StationType.Letterpress
        case PrintingProcessType.ScreenPrint                                     => StationType.DigitalPrinter
        case PrintingProcessType.LatexInkjet | PrintingProcessType.SolventInkjet => StationType.LargeFormatPrinter
      steps += printStation

      // Surface finishing
      val hasSurfaceFinish = comp.finishes.exists(sf =>
        sf.finishType == FinishType.Lamination ||
          sf.finishType == FinishType.Overlamination ||
          sf.finishType == FinishType.SoftTouchCoating
      )
      if hasSurfaceFinish then steps += StationType.Laminator

      // UV/coating
      val hasUvFinish = comp.finishes.exists(sf =>
        sf.finishType == FinishType.UVCoating ||
          sf.finishType == FinishType.AqueousCoating ||
          sf.finishType == FinishType.Varnish
      )
      if hasUvFinish then steps += StationType.UVCoater

      // Decorative finishing
      val hasDecorativeFinish = comp.finishes.exists(sf =>
        sf.finishType == FinishType.Embossing ||
          sf.finishType == FinishType.Debossing ||
          sf.finishType == FinishType.FoilStamping ||
          sf.finishType == FinishType.Thermography
      )
      if hasDecorativeFinish then steps += StationType.EmbossingFoil

      // Cutting
      val needsCutting = comp.sheetCount > 0 || comp.finishes.exists(sf =>
        sf.finishType == FinishType.DieCut ||
          sf.finishType == FinishType.ContourCut ||
          sf.finishType == FinishType.KissCut
      )
      if needsCutting then steps += StationType.Cutter

      // Large format finishing
      val hasLargeFormatFinish = comp.finishes.exists(sf =>
        sf.finishType == FinishType.Grommets ||
          sf.finishType == FinishType.Hem ||
          sf.finishType == FinishType.Mounting
      )
      if hasLargeFormatFinish then steps += StationType.LargeFormatFinishing

      // Folding
      val hasFolding = config.specifications.get(SpecKind.FoldType).isDefined
      if hasFolding && (comp.role == ComponentRole.Main || comp.role == ComponentRole.Body) then
        steps += StationType.Folder
    }

    // Binding
    if config.specifications.get(SpecKind.BindingMethod).isDefined then
      steps += StationType.Binder

    // QC and Packaging always at end
    steps += StationType.QualityControl
    steps += StationType.Packaging

    steps.result()

  /** Estimate time in minutes for a single step. */
  private def estimateStepMinutes(
      stationType: StationType,
      quantity: Int,
      estimates: List[StationTimeEstimate],
  ): Int =
    estimates.find(_.stationType == stationType) match
      case Some(est) =>
        val perUnitMinutes = (est.perUnitSeconds * quantity / BigDecimal(60)).setScale(0, BigDecimal.RoundingMode.CEILING).toInt
        est.baseTimeMinutes + perUnitMinutes
      case None => 15 // default fallback

  /** Advance to the start of the next working day. Returns (calendarMinutesElapsed, newDayOfWeek, openTime). */
  private def advanceToNextWorkStart(
      currentMinute: Int,
      currentDay: Int,
      wh: WorkingHours,
  ): (Int, Int, Int) =
    var cal = 0
    var day = currentDay
    var minute = currentMinute

    // Advance to end of current day if not already past
    if minute < 1440 then
      cal += (1440 - minute)
      minute = 0
      day = if day == 7 then 1 else day + 1

    // Skip non-working days
    while !wh.workDays.contains(day) do
      cal += 1440
      day = if day == 7 then 1 else day + 1

    // Advance to open time
    cal += wh.openTime
    (cal, day, wh.openTime)
