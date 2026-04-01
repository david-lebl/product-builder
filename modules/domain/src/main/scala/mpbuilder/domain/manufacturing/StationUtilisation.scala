package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.StationType

/** Real-time utilisation metrics for a production station. */
final case class StationUtilisation(
    stationType: StationType,
    queueDepth: Int,
    inProgressCount: Int,
    machineCount: Int,
    avgProcessingTimeMs: Long,
    estimatedClearTimeMs: Long,
):
  /** Utilisation ratio based on queue depth relative to machine capacity.
    * Returns 1.0 (fully saturated) if no machines are available.
    */
  def utilisationRatio: BigDecimal =
    if machineCount == 0 then BigDecimal(1)
    else
      val optimalThroughput = 8 // configurable: optimal queue depth per machine
      BigDecimal(queueDepth + inProgressCount) / (machineCount * optimalThroughput)

object StationUtilisation:
  /** Compute global utilisation as the maximum (bottleneck) station utilisation. */
  def globalUtilisation(stations: List[StationUtilisation]): BigDecimal =
    if stations.isEmpty then BigDecimal(0)
    else stations.map(_.utilisationRatio).max
