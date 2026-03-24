package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Calculates station utilisation from manufacturing data and derives
  * dynamic pricing multipliers based on queue state.
  *
  * Phase 2: Dynamic pricing and estimation.
  */
object UtilisationCalculator:

  /** Compute average duration from a list of completed steps for a station. */
  private def avgCompletionTimeMs(steps: List[WorkflowStep]): Long =
    val durations = steps.flatMap { s =>
      for
        started <- s.startedAt
        completed <- s.completedAt
      yield completed - started
    }
    if durations.nonEmpty then durations.sum / durations.size else 0L

  /** Calculate per-station utilisation from current manufacturing orders and machines. */
  def calculateStationUtilisation(
      orders: List[ManufacturingOrder],
      machines: List[Machine],
  ): List[StationUtilisation] =
    val activeSteps = orders.flatMap(_.workflows).flatMap(_.steps)

    val queueDepthByStation = activeSteps
      .filter(s => s.status == StepStatus.Waiting || s.status == StepStatus.Ready)
      .groupBy(_.stationType)
      .map { case (st, steps) => st -> steps.size }

    val inProgressByStation = activeSteps
      .filter(_.status == StepStatus.InProgress)
      .groupBy(_.stationType)
      .map { case (st, steps) => st -> steps.size }

    val avgTimeByStation = activeSteps
      .filter(_.status == StepStatus.Completed)
      .groupBy(_.stationType)
      .map { case (st, steps) => st -> avgCompletionTimeMs(steps) }

    val activeMachinesByStation = machines
      .filter(_.status == MachineStatus.Online)
      .groupBy(_.stationType)
      .map { case (st, ms) => st -> ms.size }

    StationType.values.toList.map { st =>
      StationUtilisation(
        stationType = st,
        queueDepth = queueDepthByStation.getOrElse(st, 0),
        inProgressCount = inProgressByStation.getOrElse(st, 0),
        machineCount = activeMachinesByStation.getOrElse(st, 0),
        avgProcessingTimeMs = avgTimeByStation.getOrElse(st, 0L),
      )
    }

  /** Global utilisation is the maximum station utilisation (bottleneck-driven).
    *
    * Using max rather than average because if any single station is saturated,
    * the entire pipeline is constrained.
    */
  def globalUtilisation(stations: List[StationUtilisation]): BigDecimal =
    import StationUtilisation.*
    if stations.isEmpty then BigDecimal(0)
    else stations.map(_.utilisationRatio).max

  /** Compute the effective speed multiplier including queue-based dynamic surcharges.
    *
    * Finds the highest applicable queue threshold (where minUtilisation <= globalUtil)
    * and adds its additionalMultiplier to the base multiplier.
    */
  def effectiveSpeedMultiplier(
      baseMultiplier: BigDecimal,
      thresholds: List[QueueThreshold],
      globalUtil: BigDecimal,
  ): BigDecimal =
    val applicableExtra = thresholds
      .filter(_.minUtilisation <= globalUtil)
      .sortBy(_.minUtilisation)
      .lastOption
      .map(_.additionalMultiplier)
      .getOrElse(BigDecimal(0))
    baseMultiplier + applicableExtra

  /** Compute the total busy period additional multiplier for the given time.
    *
    * All matching busy period multipliers are summed (they stack).
    */
  def busyPeriodExtra(
      multipliers: List[BusyPeriodMultiplier],
      dayOfWeek: Int,
      month: Int,
      minuteOfDay: Int,
  ): BigDecimal =
    import BusyPeriodMultiplier.*
    multipliers
      .filter(_.appliesAt(dayOfWeek, month, minuteOfDay))
      .map(_.additionalMultiplier)
      .foldLeft(BigDecimal(0))(_ + _)

  /** Compute the final effective multiplier for a speed tier, including
    * queue thresholds, busy period multipliers, and cap.
    *
    * @param baseMultiplier   the base multiplier from the pricing rule
    * @param thresholds       queue utilisation thresholds for dynamic surcharges
    * @param globalUtil        current global utilisation (0.0–1.0+)
    * @param busyMultipliers  busy period multipliers
    * @param dayOfWeek        current ISO day of week (1=Monday..7=Sunday)
    * @param month            current month (1=January..12=December)
    * @param minuteOfDay      current minute of day from midnight
    * @param cap              maximum effective multiplier (e.g. 2.0)
    * @param speed            the manufacturing speed tier
    * @return effective multiplier, capped and economy-protected
    */
  def computeEffectiveMultiplier(
      baseMultiplier: BigDecimal,
      thresholds: List[QueueThreshold],
      globalUtil: BigDecimal,
      busyMultipliers: List[BusyPeriodMultiplier],
      dayOfWeek: Int,
      month: Int,
      minuteOfDay: Int,
      cap: BigDecimal,
      speed: ManufacturingSpeed,
  ): BigDecimal =
    speed match
      // Economy price never changes — it's the anchor customers can always rely on
      case ManufacturingSpeed.Economy => baseMultiplier
      case _ =>
        val afterQueue = effectiveSpeedMultiplier(baseMultiplier, thresholds, globalUtil)
        val busyExtra = busyPeriodExtra(busyMultipliers, dayOfWeek, month, minuteOfDay)
        val raw = afterQueue + busyExtra
        // Cap only applies to Express and Standard; minimum is the base multiplier
        raw.min(cap)

  /** Derive queue state per station for completion time estimation.
    *
    * For each station, calculates the queue position where a new order
    * of the given tier would be inserted.
    */
  def deriveQueueStates(
      orders: List[ManufacturingOrder],
      machines: List[Machine],
  ): List[StationQueueState] =
    val activeSteps = orders.flatMap(_.workflows).flatMap(_.steps)
    val workflows = orders.flatMap(_.workflows)

    val activeMachinesByStation = machines
      .filter(_.status == MachineStatus.Online)
      .groupBy(_.stationType)
      .map { case (st, ms) => st -> ms.size }

    val avgTimeByStation = activeSteps
      .filter(_.status == StepStatus.Completed)
      .groupBy(_.stationType)
      .map { case (st, steps) => st -> avgCompletionTimeMs(steps) }

    StationType.values.toList.map { st =>
      val queuedSteps = activeSteps.filter(s =>
        s.stationType == st && (s.status == StepStatus.Waiting || s.status == StepStatus.Ready)
      )
      val totalDepth = queuedSteps.size

      // Steps belonging to Rush priority workflows come first
      val rushStepCount = queuedSteps.count { step =>
        workflows.exists(wf => wf.steps.exists(_.id == step.id) && wf.priority == Priority.Rush)
      }

      StationQueueState(
        stationType = st,
        normalPosition = rushStepCount,
        totalDepth = totalDepth,
        avgProcessingTimeMs = avgTimeByStation.getOrElse(st, 0L),
        activeMachineCount = activeMachinesByStation.getOrElse(st, 0),
      )
    }

  /** Check if Express is available at the given utilisation level.
    *
    * Express is disabled when global utilisation >= 95% (at capacity).
    */
  def isExpressAvailable(globalUtil: BigDecimal): Boolean =
    globalUtil < BigDecimal("0.95")
