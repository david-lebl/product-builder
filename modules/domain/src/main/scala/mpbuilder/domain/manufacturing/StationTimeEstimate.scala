package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.StationType

/** Configurable time estimate for a production station.
  *
  * Used by CompletionEstimator to predict production duration.
  */
final case class StationTimeEstimate(
    stationType: StationType,
    baseTimeMinutes: Int,
    perUnitSeconds: BigDecimal,
    maxParallelUnits: Int,
)
