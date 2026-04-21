package mpbuilder.domain.model

import zio.prelude.*

enum MaterialFamily:
  case Paper, Vinyl, Cardboard, Fabric, Plastic, Metal, Hardware

enum MaterialProperty:
  case Recyclable, WaterResistant, Glossy, Matte, Textured, SmoothSurface, Transparent

opaque type PaperWeight = Int
object PaperWeight:
  def apply(gsm: Int): Validation[String, PaperWeight] =
    if gsm > 0 && gsm <= 2000 then Validation.succeed(gsm)
    else Validation.fail(s"PaperWeight must be between 1 and 2000 gsm, got $gsm")

  def unsafe(gsm: Int): PaperWeight = gsm

  extension (w: PaperWeight) def gsm: Int = w

opaque type HexColor = String
object HexColor:
  def apply(hex: String): Validation[String, HexColor] =
    if hex.matches("#[0-9A-Fa-f]{6}") then Validation.succeed(hex)
    else Validation.fail(s"HexColor must be in #RRGGBB format, got $hex")

  def unsafe(hex: String): HexColor = hex

  extension (c: HexColor) def value: String = c

sealed trait MaterialAttribute
object MaterialAttribute:
  final case class MaxBoundEdgeLengthMm(value: Double) extends MaterialAttribute
  final case class MaxBoundThicknessMm(value: Double)  extends MaterialAttribute
  final case class Color(hex: HexColor)                extends MaterialAttribute
  final case class CoilPitchMm(value: Double)          extends MaterialAttribute

final case class Material(
    id: MaterialId,
    name: LocalizedString,
    family: MaterialFamily,
    weight: Option[PaperWeight],
    properties: Set[MaterialProperty],
    attributes: Option[Set[MaterialAttribute]] = None,
    description: Option[LocalizedString] = None,
):
  def allAttributes: Set[MaterialAttribute] = attributes.getOrElse(Set.empty)
