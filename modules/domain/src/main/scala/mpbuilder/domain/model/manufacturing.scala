package mpbuilder.domain.model

import mpbuilder.domain.pricing.PriceBreakdown

enum PrintFormatType:
  case SmallFormat // Offset, Digital, Letterpress, ScreenPrint
  case LargeFormat // UVCurableInkjet, LatexInkjet, SolventInkjet

object PrintFormatType:
  def fromProcessType(processType: PrintingProcessType): PrintFormatType =
    processType match
      case PrintingProcessType.Offset          => SmallFormat
      case PrintingProcessType.Digital         => SmallFormat
      case PrintingProcessType.Letterpress     => SmallFormat
      case PrintingProcessType.ScreenPrint     => SmallFormat
      case PrintingProcessType.UVCurableInkjet => LargeFormat
      case PrintingProcessType.LatexInkjet     => LargeFormat
      case PrintingProcessType.SolventInkjet   => LargeFormat

  extension (t: PrintFormatType)
    def label(lang: Language): String = t match
      case SmallFormat => lang match
        case Language.En => "Small Format"
        case Language.Cs => "Malý formát"
      case LargeFormat => lang match
        case Language.En => "Large Format"
        case Language.Cs => "Velký formát"

enum ManufacturingStatus:
  case PendingApproval // Large format: waiting for manager approval before production
  case Queued          // In production queue (initial state for small format)
  case InProduction    // Currently being printed / produced
  case QualityCheck    // Quality control inspection
  case Packaging       // Being packaged
  case ReadyForPickup  // Packaged and waiting for delivery service or customer pickup
  case Completed       // Picked up by delivery service or customer
  case Cancelled       // Order was cancelled
  case Rejected        // Large format: approval was rejected

object ManufacturingStatus:
  extension (s: ManufacturingStatus)
    def isTerminal: Boolean = s match
      case Completed | Cancelled | Rejected => true
      case _                                => false

    def label(lang: Language): String = s match
      case PendingApproval => lang match
        case Language.En => "Pending Approval"
        case Language.Cs => "Čeká na schválení"
      case Queued => lang match
        case Language.En => "Queued"
        case Language.Cs => "Ve frontě"
      case InProduction => lang match
        case Language.En => "In Production"
        case Language.Cs => "Ve výrobě"
      case QualityCheck => lang match
        case Language.En => "Quality Check"
        case Language.Cs => "Kontrola kvality"
      case Packaging => lang match
        case Language.En => "Packaging"
        case Language.Cs => "Balení"
      case ReadyForPickup => lang match
        case Language.En => "Ready for Pickup"
        case Language.Cs => "Připraveno k vyzvednutí"
      case Completed => lang match
        case Language.En => "Completed"
        case Language.Cs => "Dokončeno"
      case Cancelled => lang match
        case Language.En => "Cancelled"
        case Language.Cs => "Zrušeno"
      case Rejected => lang match
        case Language.En => "Rejected"
        case Language.Cs => "Zamítnuto"

final case class ManufacturingOrder(
    id: OrderId,
    item: BasketItem,
    formatType: PrintFormatType,
    status: ManufacturingStatus,
    createdAt: Long,
    notes: Option[String] = None,
)
