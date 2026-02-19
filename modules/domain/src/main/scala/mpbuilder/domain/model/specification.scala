package mpbuilder.domain.model

import zio.prelude.*

final case class Dimension(widthMm: Double, heightMm: Double)

opaque type Quantity = Int
object Quantity:
  def apply(value: Int): Validation[String, Quantity] =
    if value > 0 then Validation.succeed(value)
    else Validation.fail(s"Quantity must be positive, got $value")

  def unsafe(value: Int): Quantity = value

  extension (q: Quantity) def value: Int = q

enum ColorMode:
  case CMYK, PMS, Grayscale

enum Orientation:
  case Portrait, Landscape

enum FoldType:
  case Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold

enum BindingMethod:
  case SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding

final case class InkConfiguration(
    frontColors: Int,
    backColors: Int,
)

object InkConfiguration:
  val `4/4`: InkConfiguration = InkConfiguration(4, 4)
  val `4/0`: InkConfiguration = InkConfiguration(4, 0)
  val `4/1`: InkConfiguration = InkConfiguration(4, 1)
  val `1/0`: InkConfiguration = InkConfiguration(1, 0)
  val `1/1`: InkConfiguration = InkConfiguration(1, 1)

  def apply(frontColors: Int, backColors: Int): InkConfiguration =
    new InkConfiguration(frontColors, backColors)

  extension (ic: InkConfiguration)
    def label: String = s"${ic.frontColors}/${ic.backColors}"
    def isDoubleSided: Boolean = ic.backColors > 0

enum SpecValue:
  case SizeSpec(dimension: Dimension)
  case QuantitySpec(quantity: Quantity)
  case ColorModeSpec(mode: ColorMode)
  case OrientationSpec(orientation: Orientation)
  case BleedSpec(bleedMm: Double)
  case PagesSpec(count: Int)
  case FoldTypeSpec(foldType: FoldType)
  case BindingMethodSpec(method: BindingMethod)
  case InkConfigurationSpec(inkConfiguration: InkConfiguration)

object SpecValue:
  def specKind(sv: SpecValue): SpecKind = sv match
    case _: SpecValue.SizeSpec              => SpecKind.Size
    case _: SpecValue.QuantitySpec          => SpecKind.Quantity
    case _: SpecValue.ColorModeSpec         => SpecKind.ColorMode
    case _: SpecValue.OrientationSpec       => SpecKind.Orientation
    case _: SpecValue.BleedSpec             => SpecKind.Bleed
    case _: SpecValue.PagesSpec             => SpecKind.Pages
    case _: SpecValue.FoldTypeSpec          => SpecKind.FoldType
    case _: SpecValue.BindingMethodSpec     => SpecKind.BindingMethod
    case _: SpecValue.InkConfigurationSpec  => SpecKind.InkConfiguration

final case class ProductSpecifications(specs: Map[SpecKind, SpecValue]):
  def get(kind: SpecKind): Option[SpecValue] = specs.get(kind)
  def specKinds: Set[SpecKind] = specs.keySet

object ProductSpecifications:
  val empty: ProductSpecifications = ProductSpecifications(Map.empty)

  def fromSpecs(specs: Seq[SpecValue]): ProductSpecifications =
    ProductSpecifications(specs.map(s => SpecValue.specKind(s) -> s).toMap)
