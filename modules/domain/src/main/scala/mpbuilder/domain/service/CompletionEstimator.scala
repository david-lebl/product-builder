package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.*
import java.time.*

/** Pure estimation service for production completion dates.
  *
  * Computes estimated completion based on:
  * - Production duration (from station time estimates and workflow steps)
  * - Queue wait time (position depends on manufacturing speed tier)
  * - Approval delay (tier-dependent)
  * - Buffer time (tier-dependent safety margin)
  * - Working hours (respects shop schedule, weekends, holidays)
  */
object CompletionEstimator:

  /** Estimated completion result with a range for confidence display. */
  final case class CompletionEstimate(
      earliestCompletion: LocalDateTime,
      latestCompletion: LocalDateTime,
      productionMinutes: Long,
      queueWaitMinutes: Long,
      approvalDelayMinutes: Long,
      bufferMinutes: Long,
  ):
    /** Format the estimate for display based on proximity to now. */
    def formatEarliest(now: LocalDateTime, lang: Language): String =
      formatDateTime(earliestCompletion, now, lang)

    /** Format the range for display. */
    def formatRange(now: LocalDateTime, lang: Language): String =
      val e = formatDateTime(earliestCompletion, now, lang)
      val l = formatDateTime(latestCompletion, now, lang)
      if e == l then e else s"$e – $l"

  /** Estimate completion date for an order with a given manufacturing speed. */
  def estimate(
      steps: List[StationType],
      quantity: Int,
      speed: ManufacturingSpeed,
      stationEstimates: List[StationTimeEstimate],
      stationQueues: Map[StationType, StationQueueState],
      schedule: ShopSchedule,
      orderTime: LocalDateTime,
  ): CompletionEstimate =
    val productionMinutes = estimateProductionTime(steps, quantity, stationEstimates)
    val queueWaitMinutes = estimateQueueWait(steps, speed, stationQueues)
    val approvalMinutes = approvalDelay(speed)
    val bufferMin = bufferTime(speed)

    val totalMinutes = productionMinutes + queueWaitMinutes + approvalMinutes

    // Check if order is placed after cutoff
    val effectiveStart = adjustForCutoff(orderTime, speed, schedule)

    val earliestEnd = advanceByWorkingMinutes(effectiveStart, totalMinutes, schedule.workingHours)
    val latestEnd = advanceByWorkingMinutes(earliestEnd, bufferMin, schedule.workingHours)

    // Round to nearest 30-minute block
    val earliestRounded = roundToHalfHour(earliestEnd)
    val latestRounded = roundToHalfHour(latestEnd)

    CompletionEstimate(
      earliestCompletion = earliestRounded,
      latestCompletion = latestRounded,
      productionMinutes = productionMinutes,
      queueWaitMinutes = queueWaitMinutes,
      approvalDelayMinutes = approvalMinutes,
      bufferMinutes = bufferMin,
    )

  /** Estimate completion date for an order routed to an external manufacturing partner.
    *
    * Bypasses queue/speed math. The estimate accounts for:
    * 1. In-house prepress duration.
    * 2. Partner lead time (min for earliest, max for latest), in working days.
    * 3. A fixed 1-business-day QC+packaging buffer.
    */
  def estimateExternal(
      partner: ExternalPartner,
      quantity: Int,
      stationEstimates: List[StationTimeEstimate],
      schedule: ShopSchedule,
      orderTime: LocalDateTime,
  ): CompletionEstimate =
    val workingMinutesPerDay = Duration
      .between(schedule.workingHours.openTime, schedule.workingHours.closeTime)
      .toMinutes

    val prepressMinutes = estimateProductionTime(List(StationType.Prepress), quantity, stationEstimates)
    val leadTimeMinMinutes = partner.leadTimeBusinessDays._1.toLong * workingMinutesPerDay
    val leadTimeMaxMinutes = partner.leadTimeBusinessDays._2.toLong * workingMinutesPerDay
    val qcPackagingBuffer = workingMinutesPerDay  // 1 business day

    val effectiveStart = if isWorkingDay(orderTime.toLocalDate, schedule.workingHours) then orderTime
      else
        val nextDay = nextWorkingDay(orderTime.toLocalDate, schedule.workingHours)
        LocalDateTime.of(nextDay, schedule.workingHours.openTime)

    val afterPrepress = advanceByWorkingMinutes(effectiveStart, prepressMinutes, schedule.workingHours)
    val earliestWithBuffer = advanceByWorkingMinutes(afterPrepress, leadTimeMinMinutes + qcPackagingBuffer, schedule.workingHours)
    val latestWithBuffer = advanceByWorkingMinutes(afterPrepress, leadTimeMaxMinutes + qcPackagingBuffer, schedule.workingHours)

    CompletionEstimate(
      earliestCompletion = roundToHalfHour(earliestWithBuffer),
      latestCompletion = roundToHalfHour(latestWithBuffer),
      productionMinutes = prepressMinutes,
      queueWaitMinutes = leadTimeMinMinutes,
      approvalDelayMinutes = 0L,
      bufferMinutes = qcPackagingBuffer,
    )

  /** Queue state for a single station, used for queue wait estimation. */
  final case class StationQueueState(
      queueDepth: Int,
      avgProcessingTimeMinutes: Long,
      activeMachineCount: Int,
  ):
    def normalPosition: Int = (queueDepth * 0.5).toInt // after Rush orders
    def totalDepth: Int = queueDepth

  /** Estimate total production time in minutes from station time estimates. */
  def estimateProductionTime(
      steps: List[StationType],
      quantity: Int,
      stationEstimates: List[StationTimeEstimate],
  ): Long =
    val estimateMap = stationEstimates.map(e => e.stationType -> e).toMap
    steps.map { stationType =>
      estimateMap.get(stationType) match
        case Some(est) =>
          val perUnitTime = (est.perUnitSeconds * quantity / 60).toLong
          est.baseTimeMinutes.toLong + perUnitTime
        case None =>
          15L // default 15 minutes for unknown stations
    }.sum

  /** Estimate queue wait time in minutes based on tier and queue state. */
  def estimateQueueWait(
      steps: List[StationType],
      speed: ManufacturingSpeed,
      stationQueues: Map[StationType, StationQueueState],
  ): Long =
    steps.map { stationType =>
      stationQueues.get(stationType) match
        case None => 0L
        case Some(queueState) =>
          val position = speed match
            case ManufacturingSpeed.Express  => 0
            case ManufacturingSpeed.Standard => queueState.normalPosition
            case ManufacturingSpeed.Economy  => queueState.totalDepth
          val machines = queueState.activeMachineCount.max(1)
          (position.toLong * queueState.avgProcessingTimeMinutes) / machines
    }.sum

  /** Approval delay in *working* minutes per tier.
    *
    * These are consumed by `advanceByWorkingMinutes` and therefore represent
    * time inside working hours (10 h / day by default), not wall-clock time.
    * Numbers are calibrated so earliest-completion lands at:
    *   - Express:  ~next business day morning (orders before the 14:00 cutoff)
    *   - Standard: ~2 business days later
    *   - Economy:  ~4 business days later
    */
  def approvalDelay(speed: ManufacturingSpeed): Long = speed match
    case ManufacturingSpeed.Express  => 480   // ≈ 1 working day: prioritized review + prepress
    case ManufacturingSpeed.Standard => 1200  // ≈ 2 working days: normal prepress queue
    case ManufacturingSpeed.Economy  => 2400  // ≈ 4 working days: batched prepress

  /** Buffer time in minutes per tier. */
  def bufferTime(speed: ManufacturingSpeed): Long = speed match
    case ManufacturingSpeed.Express  => 60    // 1h safety margin
    case ManufacturingSpeed.Standard => 240   // 4h safety margin
    case ManufacturingSpeed.Economy  => 480   // 1 business day

  /** Adjust start time if order is placed after cutoff. */
  private def adjustForCutoff(
      orderTime: LocalDateTime,
      speed: ManufacturingSpeed,
      schedule: ShopSchedule,
  ): LocalDateTime =
    val cutoff = speed match
      case ManufacturingSpeed.Express  => schedule.expressCutoffTime
      case ManufacturingSpeed.Standard => schedule.standardCutoffTime
      case ManufacturingSpeed.Economy  => schedule.standardCutoffTime

    val wh = schedule.workingHours
    val orderDate = orderTime.toLocalDate
    val orderTimeOfDay = orderTime.toLocalTime

    // If outside working hours or after cutoff, start next business day
    if !isWorkingDay(orderDate, wh) || orderTimeOfDay.isAfter(cutoff) || orderTimeOfDay.isAfter(wh.closeTime) then
      val nextDay = nextWorkingDay(orderDate, wh)
      LocalDateTime.of(nextDay, wh.openTime)
    else if orderTimeOfDay.isBefore(wh.openTime) then
      if isWorkingDay(orderDate, wh) then LocalDateTime.of(orderDate, wh.openTime)
      else
        val nextDay = nextWorkingDay(orderDate, wh)
        LocalDateTime.of(nextDay, wh.openTime)
    else
      orderTime

  /** Advance a LocalDateTime by the given number of working minutes, respecting working hours. */
  def advanceByWorkingMinutes(
      start: LocalDateTime,
      minutes: Long,
      wh: WorkingHours,
  ): LocalDateTime =
    if minutes <= 0 then return start

    var remaining = minutes
    var current = start

    // If starting outside working hours, jump to next working period
    if !isWorkingDay(current.toLocalDate, wh) || current.toLocalTime.isBefore(wh.openTime) then
      val nextDay = if isWorkingDay(current.toLocalDate, wh) then current.toLocalDate else nextWorkingDay(current.toLocalDate, wh)
      current = LocalDateTime.of(nextDay, wh.openTime)
    else if current.toLocalTime.isAfter(wh.closeTime) then
      val nextDay = nextWorkingDay(current.toLocalDate, wh)
      current = LocalDateTime.of(nextDay, wh.openTime)

    while remaining > 0 do
      val endOfDay = LocalDateTime.of(current.toLocalDate, wh.closeTime)
      val minutesUntilClose = Duration.between(current, endOfDay).toMinutes

      if minutesUntilClose <= 0 then
        // Already at or past closing — move to next working day
        val nextDay = nextWorkingDay(current.toLocalDate, wh)
        current = LocalDateTime.of(nextDay, wh.openTime)
      else if remaining <= minutesUntilClose then
        current = current.plusMinutes(remaining)
        remaining = 0
      else
        remaining -= minutesUntilClose
        val nextDay = nextWorkingDay(current.toLocalDate, wh)
        current = LocalDateTime.of(nextDay, wh.openTime)

    current

  /** Check if a date is a working day. */
  def isWorkingDay(date: LocalDate, wh: WorkingHours): Boolean =
    wh.workDays.contains(date.getDayOfWeek) && !wh.holidays.contains(date)

  /** Find the next working day after the given date. */
  def nextWorkingDay(date: LocalDate, wh: WorkingHours): LocalDate =
    var next = date.plusDays(1)
    while !isWorkingDay(next, wh) do
      next = next.plusDays(1)
    next

  /** Round a LocalDateTime to the nearest 30-minute block. */
  def roundToHalfHour(dt: LocalDateTime): LocalDateTime =
    val minute = dt.getMinute
    val roundedMinute = if minute < 15 then 0 else if minute < 45 then 30 else 0
    val adjusted = if minute >= 45 then dt.plusHours(1) else dt
    adjusted.withMinute(roundedMinute).withSecond(0).withNano(0)

  /** Format a datetime relative to now for display. */
  def formatDateTime(dt: LocalDateTime, now: LocalDateTime, lang: Language): String =
    val today = now.toLocalDate
    val targetDate = dt.toLocalDate
    val daysBetween = Duration.between(now.atZone(ZoneOffset.UTC), dt.atZone(ZoneOffset.UTC)).toDays

    val timeStr = f"${dt.getHour}%d:${dt.getMinute}%02d"

    if targetDate == today then
      lang match
        case Language.En => s"Today, $timeStr"
        case Language.Cs => s"Dnes, $timeStr"
    else if targetDate == today.plusDays(1) then
      lang match
        case Language.En => s"Tomorrow, $timeStr"
        case Language.Cs => s"Zítra, $timeStr"
    else if daysBetween <= 7 then
      val dayName = dayOfWeekName(dt.getDayOfWeek, lang)
      s"$dayName, $timeStr"
    else if daysBetween <= 14 then
      val dayName = dayOfWeekName(dt.getDayOfWeek, lang)
      val monthDay = monthDayStr(dt, lang)
      s"$dayName, $monthDay"
    else
      monthDayStr(dt, lang)

  private def dayOfWeekName(dow: DayOfWeek, lang: Language): String = lang match
    case Language.En => dow match
      case DayOfWeek.MONDAY    => "Monday"
      case DayOfWeek.TUESDAY   => "Tuesday"
      case DayOfWeek.WEDNESDAY => "Wednesday"
      case DayOfWeek.THURSDAY  => "Thursday"
      case DayOfWeek.FRIDAY    => "Friday"
      case DayOfWeek.SATURDAY  => "Saturday"
      case DayOfWeek.SUNDAY    => "Sunday"
    case Language.Cs => dow match
      case DayOfWeek.MONDAY    => "Pondělí"
      case DayOfWeek.TUESDAY   => "Úterý"
      case DayOfWeek.WEDNESDAY => "Středa"
      case DayOfWeek.THURSDAY  => "Čtvrtek"
      case DayOfWeek.FRIDAY    => "Pátek"
      case DayOfWeek.SATURDAY  => "Sobota"
      case DayOfWeek.SUNDAY    => "Neděle"

  private def monthDayStr(dt: LocalDateTime, lang: Language): String =
    val mn = monthName(dt.getMonth, lang)
    s"$mn ${dt.getDayOfMonth}"

  private def monthName(month: Month, lang: Language): String = lang match
    case Language.En => month match
      case Month.JANUARY   => "January"
      case Month.FEBRUARY  => "February"
      case Month.MARCH     => "March"
      case Month.APRIL     => "April"
      case Month.MAY       => "May"
      case Month.JUNE      => "June"
      case Month.JULY      => "July"
      case Month.AUGUST    => "August"
      case Month.SEPTEMBER => "September"
      case Month.OCTOBER   => "October"
      case Month.NOVEMBER  => "November"
      case Month.DECEMBER  => "December"
    case Language.Cs => month match
      case Month.JANUARY   => "leden"
      case Month.FEBRUARY  => "únor"
      case Month.MARCH     => "březen"
      case Month.APRIL     => "duben"
      case Month.MAY       => "květen"
      case Month.JUNE      => "červen"
      case Month.JULY      => "červenec"
      case Month.AUGUST    => "srpen"
      case Month.SEPTEMBER => "září"
      case Month.OCTOBER   => "říjen"
      case Month.NOVEMBER  => "listopad"
      case Month.DECEMBER  => "prosinec"
