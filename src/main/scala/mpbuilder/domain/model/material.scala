package mpbuilder.domain.model

import zio.prelude.*

enum MaterialFamily:
  case Paper, Vinyl, Cardboard, Fabric

enum MaterialProperty:
  case Recyclable, WaterResistant, Glossy, Matte, Textured, SmoothSurface

opaque type PaperWeight = Int
object PaperWeight:
  def apply(gsm: Int): Validation[String, PaperWeight] =
    if gsm > 0 && gsm <= 2000 then Validation.succeed(gsm)
    else Validation.fail(s"PaperWeight must be between 1 and 2000 gsm, got $gsm")

  def unsafe(gsm: Int): PaperWeight = gsm

  extension (w: PaperWeight) def gsm: Int = w

final case class Material(
    id: MaterialId,
    name: LocalizedString,
    family: MaterialFamily,
    weight: Option[PaperWeight],
    properties: Set[MaterialProperty],
)
