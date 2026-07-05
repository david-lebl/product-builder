package mpbuilder.manufacturing

/** Production station types derived from domain processing steps */
enum StationType:
  case Prepress
  case DigitalPrinter
  case OffsetPress
  case LargeFormatPrinter
  case Letterpress
  case Cutter
  case Laminator
  case UVCoater
  case EmbossingFoil
  case Folder
  case Binder
  case LargeFormatFinishing
  case QualityControl
  case Packaging

object StationType:
  extension (st: StationType) def displayName: String = st match
    case Prepress            => "Prepress"
    case DigitalPrinter      => "Digital Printer"
    case OffsetPress         => "Offset Press"
    case LargeFormatPrinter  => "Large Format Printer"
    case Letterpress         => "Letterpress"
    case Cutter              => "Cutter"
    case Laminator           => "Laminator"
    case UVCoater            => "UV Coater"
    case EmbossingFoil       => "Embossing / Foil"
    case Folder              => "Folder"
    case Binder              => "Binder"
    case LargeFormatFinishing => "Large Format Finishing"
    case QualityControl      => "Quality Control"
    case Packaging           => "Packaging & Dispatch"

  extension (st: StationType) def icon: String = st match
    case Prepress            => "📋"
    case DigitalPrinter      => "🖨️"
    case OffsetPress         => "🏭"
    case LargeFormatPrinter  => "🖼️"
    case Letterpress         => "🔤"
    case Cutter              => "✂️"
    case Laminator           => "🔲"
    case UVCoater            => "✨"
    case EmbossingFoil       => "💎"
    case Folder              => "📐"
    case Binder              => "📚"
    case LargeFormatFinishing => "🔧"
    case QualityControl      => "✅"
    case Packaging           => "📦"

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
