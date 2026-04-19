package mpbuilder.domain.model

enum FinishCategory:
  case Surface, Decorative, Structural, LargeFormat

enum FinishType:
  case Lamination, Overlamination, UVCoating, AqueousCoating, SoftTouchCoating, Varnish
  case Embossing, Debossing, FoilStamping, Thermography, EdgePainting
  case DieCut, ContourCut, KissCut, Scoring, Perforation, RoundCorners, Drilling, Numbering, Binding, Mounting
  case Grommets, Hem
  case Embroidery

object FinishType:
  extension (ft: FinishType) def finishCategory: FinishCategory = ft match
    case Lamination | Overlamination | UVCoating | AqueousCoating | SoftTouchCoating =>
      FinishCategory.Surface
    case Embossing | Debossing | FoilStamping | Thermography | EdgePainting | Varnish =>
      FinishCategory.Decorative
    case DieCut | ContourCut | KissCut | Scoring | Perforation | RoundCorners | Drilling | Numbering | Binding | Mounting =>
      FinishCategory.Structural
    case Grommets | Hem =>
      FinishCategory.LargeFormat
    case Embroidery =>
      FinishCategory.Decorative

enum FinishSide:
  case Front, Back, Both

final case class Finish(
    id: FinishId,
    name: LocalizedString,
    finishType: FinishType,
    side: FinishSide,
    description: Option[LocalizedString] = None,
)

enum FoilColor:
  case Gold, Silver, Copper, RoseGold, Holographic

sealed trait FinishParameters
object FinishParameters:
  final case class RoundCornersParams(cornerCount: Int, radiusMm: Int) extends FinishParameters
  final case class LaminationParams(side: FinishSide) extends FinishParameters
  final case class FoilStampingParams(color: FoilColor) extends FinishParameters
  final case class GrommetParams(spacingMm: Int) extends FinishParameters
  final case class PerforationParams(pitchMm: Int) extends FinishParameters
  final case class ScoringParams(creaseCount: Int) extends FinishParameters

final case class SelectedFinish(finish: Finish, params: Option[FinishParameters] = None):
  export finish.{id, name, finishType, side}
