package mpbuilder.domain.model

enum PrintingProcessType:
  case Offset, Digital, Letterpress, ScreenPrint, UVCurableInkjet, LatexInkjet, SolventInkjet

final case class PrintingMethod(
    id: PrintingMethodId,
    name: LocalizedString,
    processType: PrintingProcessType,
    maxColorCount: Option[Int],
)
