package mpbuilder.domain.model

enum FinishType:
  case Lamination, UVCoating, Embossing, FoilStamping, Varnish, DieCut

enum FinishSide:
  case Front, Back, Both

final case class Finish(
    id: FinishId,
    name: String,
    finishType: FinishType,
    side: FinishSide,
)
