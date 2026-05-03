package mpbuilder.domain.model

enum PrintingProcessType:
  case Offset, Digital, Letterpress, ScreenPrint, UVCurableInkjet, LatexInkjet, SolventInkjet

object PrintingProcessType:
  extension (p: PrintingProcessType)
    /** True for roll-fed / flatbed inkjet processes that print one face at a time. */
    def isLargeFormatInkjet: Boolean =
      p == UVCurableInkjet || p == SolventInkjet || p == LatexInkjet

final case class PrintingMethod(
    id: PrintingMethodId,
    name: LocalizedString,
    processType: PrintingProcessType,
    maxColorCount: Option[Int],
    description: Option[LocalizedString] = None,
)
