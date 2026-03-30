package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.StationUtilisation
import mpbuilder.domain.pricing.*

/** Pure calculation service for queue utilisation and dynamic pricing multipliers.
  *
  * Computes the effective speed multiplier for a manufacturing speed tier based on
  * current station utilisation, queue thresholds, busy period multipliers, and a
  * configurable surcharge cap.
  */
object UtilisationCalculator:

  /** Compute global utilisation from station metrics. Uses bottleneck (max) approach. */
  def computeGlobalUtilisation(stations: List[StationUtilisation]): BigDecimal =
    StationUtilisation.globalUtilisation(stations)

  /** Compute the effective speed multiplier for a given tier and pricing context.
    *
    * For Express: base multiplier + queue thresholds + busy period, capped at expressSurchargeCap.
    * For Standard: base multiplier + mild queue threshold at high utilisation.
    * For Economy: base multiplier only (no dynamic adjustment).
    */
  def computeEffectiveMultiplier(
      tier: ManufacturingSpeed,
      rule: PricingRule.ManufacturingSpeedSurcharge,
      context: PricingContext,
  ): BigDecimal =
    val baseMultiplier = rule.multiplier

    if tier == ManufacturingSpeed.Economy then baseMultiplier
    else
      val queueAdjustment = rule.queueMultiplierThresholds
        .filter(_.minUtilisation <= context.globalUtilisation)
        .map(_.additionalMultiplier)
        .foldLeft(BigDecimal(0))(_ + _)

      val busyAdjustment = context.busyPeriodMultipliers
        .map(_.additionalMultiplier)
        .foldLeft(BigDecimal(0))(_ + _)

      val raw = baseMultiplier + queueAdjustment + busyAdjustment
      raw.min(context.expressSurchargeCap)

  /** Determine whether Express manufacturing is available given current utilisation.
    *
    * Express is unavailable when global utilisation >= the critical threshold (typically 95%).
    */
  def isExpressAvailable(
      globalUtilisation: BigDecimal,
      criticalThreshold: BigDecimal = BigDecimal("0.95"),
  ): Boolean =
    globalUtilisation < criticalThreshold

  /** Build a PricingContext from current station utilisation and time-based multipliers. */
  def buildPricingContext(
      stations: List[StationUtilisation],
      activeBusyPeriods: List[BusyPeriodMultiplier],
      currentTimeMillis: Long,
      expressSurchargeCap: BigDecimal = BigDecimal(2),
  ): PricingContext =
    PricingContext(
      globalUtilisation = computeGlobalUtilisation(stations),
      busyPeriodMultipliers = activeBusyPeriods,
      currentTimeMillis = currentTimeMillis,
      expressSurchargeCap = expressSurchargeCap,
    )
