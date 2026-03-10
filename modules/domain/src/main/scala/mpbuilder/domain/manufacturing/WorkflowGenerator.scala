package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.*

/** Derives a list of `ProductionStep`s from a `ProductConfiguration` and the available stations.
  *
  * Rules:
  *   - Always starts with Prepress
  *   - Printer station is chosen by `PrintingMethod.processType`
  *   - Cutter is always included (virtually all sheet-fed print jobs require cutting)
  *   - Finish types drive Laminator / UVCoater / EmbossingFoil steps
  *   - FoldType spec drives Folder
  *   - BindingMethod spec drives Binder
  *   - Always ends with QualityControl then Packaging
  *   - Duplicate station types are deduplicated while preserving order
  */
object WorkflowGenerator:

  def generate(
      config: ProductConfiguration,
      stations: List[Station],
      now: Long,
  ): List[ProductionStep] =
    // Index stations by type (take first match per type)
    val stationByType: Map[StationType, Station] =
      stations.foldLeft(Map.empty[StationType, Station]) { (acc, s) =>
        if acc.contains(s.stationType) then acc else acc + (s.stationType -> s)
      }

    def stepFor(st: StationType): Option[ProductionStep] =
      stationByType.get(st).map(s => ProductionStep(s.id, OrderStatus.Queued, now))

    // Printer station derived from process type
    val printerType: StationType = config.printingMethod.processType match
      case PrintingProcessType.Digital | PrintingProcessType.UVCurableInkjet =>
        StationType.DigitalPrinter
      case PrintingProcessType.Offset =>
        StationType.OffsetPress
      case PrintingProcessType.Letterpress =>
        StationType.Letterpress
      case PrintingProcessType.LatexInkjet | PrintingProcessType.SolventInkjet =>
        StationType.LargeFormatPrinter
      case PrintingProcessType.ScreenPrint =>
        StationType.DigitalPrinter

    // Finish-driven station types
    val allFinishTypes: List[FinishType] =
      config.components.flatMap(_.finishes.map(_.finishType)).distinct

    val finishStationTypes: List[StationType] = allFinishTypes.flatMap {
      case FinishType.Lamination | FinishType.Overlamination |
           FinishType.AqueousCoating | FinishType.SoftTouchCoating =>
        Some(StationType.Laminator)
      case FinishType.UVCoating =>
        Some(StationType.UVCoater)
      case FinishType.Embossing | FinishType.Debossing |
           FinishType.FoilStamping | FinishType.Thermography =>
        Some(StationType.EmbossingFoil)
      case FinishType.Binding =>
        Some(StationType.Binder)
      case FinishType.Grommets | FinishType.Hem =>
        Some(StationType.LargeFormatFinishing)
      case FinishType.DieCut | FinishType.ContourCut | FinishType.KissCut |
           FinishType.Scoring | FinishType.Perforation | FinishType.RoundCorners |
           FinishType.Drilling | FinishType.Numbering | FinishType.Mounting |
           FinishType.Varnish | FinishType.EdgePainting =>
        None
    }.distinct

    // Spec-driven station types
    val hasFold    = config.specifications.get(SpecKind.FoldType).isDefined
    val hasBinding = config.specifications.get(SpecKind.BindingMethod).isDefined

    val specStationTypes: List[StationType] =
      (if hasFold then List(StationType.Folder) else Nil) ++
      (if hasBinding then List(StationType.Binder) else Nil)

    // Build ordered station type list, deduplicating
    val ordered: List[StationType] =
      deduplicate(
        List(StationType.Prepress) ++
        List(printerType) ++
        List(StationType.Cutter) ++
        finishStationTypes ++
        specStationTypes ++
        List(StationType.QualityControl, StationType.Packaging)
      )

    ordered.flatMap(stepFor)

  private def deduplicate(types: List[StationType]): List[StationType] =
    types.foldLeft((List.empty[StationType], Set.empty[StationType])) {
      case ((acc, seen), t) =>
        if seen.contains(t) then (acc, seen)
        else (acc :+ t, seen + t)
    }._1
