package mpbuilder.domain.model

enum PrintingProcessType:
  case Offset, Digital, Letterpress, ScreenPrint, UVCurableInkjet, LatexInkjet, SolventInkjet

final case class PrintingMethod(
    id: PrintingMethodId,
    name: String,
    processType: PrintingProcessType,
    maxColorCount: Option[Int],
)
