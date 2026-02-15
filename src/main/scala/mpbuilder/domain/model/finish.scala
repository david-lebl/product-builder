package mpbuilder.domain.model

enum FinishCategory:
  case Surface, Decorative, Structural, LargeFormat

enum FinishType:
  case Lamination, Overlamination, UVCoating, AqueousCoating, SoftTouchCoating, Varnish
  case Embossing, Debossing, FoilStamping, Thermography, EdgePainting
  case DieCut, ContourCut, KissCut, Scoring, Perforation, RoundCorners, Drilling, Numbering, Binding, Mounting
  case Grommets, Hem

object FinishType:
  extension (ft: FinishType) def finishCategory: FinishCategory = ft match
    case Lamination | Overlamination | UVCoating | AqueousCoating | SoftTouchCoating | Varnish =>
      FinishCategory.Surface
    case Embossing | Debossing | FoilStamping | Thermography | EdgePainting =>
      FinishCategory.Decorative
    case DieCut | ContourCut | KissCut | Scoring | Perforation | RoundCorners | Drilling | Numbering | Binding | Mounting =>
      FinishCategory.Structural
    case Grommets | Hem =>
      FinishCategory.LargeFormat

enum FinishSide:
  case Front, Back, Both

final case class Finish(
    id: FinishId,
    name: String,
    finishType: FinishType,
    side: FinishSide,
)
