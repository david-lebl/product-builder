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

enum InkType:
  case CMYK, PMS, Grayscale, None

final case class InkSetup(inkType: InkType, colorCount: Int)

object InkSetup:
  val cmyk: InkSetup = InkSetup(InkType.CMYK, 4)
  val none: InkSetup = InkSetup(InkType.None, 0)
  val grayscale: InkSetup = InkSetup(InkType.Grayscale, 1)
  def pms(n: Int): InkSetup = InkSetup(InkType.PMS, n)

final case class InkConfiguration(front: InkSetup, back: InkSetup):
  def notation: String = s"${front.colorCount}/${back.colorCount}"
  def maxColorCount: Int = math.max(front.colorCount, back.colorCount)
  def isSingleSided: Boolean = back.inkType == InkType.None
  def isDoubleSided: Boolean = back.inkType != InkType.None

object InkConfiguration:
  val cmyk4_4: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.cmyk)
  val cmyk4_0: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.none)
  val cmyk4_1: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.grayscale)
  val mono1_0: InkConfiguration = InkConfiguration(InkSetup.grayscale, InkSetup.none)
  val mono1_1: InkConfiguration = InkConfiguration(InkSetup.grayscale, InkSetup.grayscale)

enum Orientation:
  case Portrait, Landscape

enum FoldType:
  case Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold

enum BindingMethod:
  case SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding

enum SpecValue:
  case SizeSpec(dimension: Dimension)
  case QuantitySpec(quantity: Quantity)
  case InkConfigSpec(config: InkConfiguration)
  case OrientationSpec(orientation: Orientation)
  case BleedSpec(bleedMm: Double)
  case PagesSpec(count: Int)
  case FoldTypeSpec(foldType: FoldType)
  case BindingMethodSpec(method: BindingMethod)

object SpecValue:
  def specKind(sv: SpecValue): SpecKind = sv match
    case _: SpecValue.SizeSpec          => SpecKind.Size
    case _: SpecValue.QuantitySpec      => SpecKind.Quantity
    case _: SpecValue.InkConfigSpec      => SpecKind.InkConfig
    case _: SpecValue.OrientationSpec   => SpecKind.Orientation
    case _: SpecValue.BleedSpec         => SpecKind.Bleed
    case _: SpecValue.PagesSpec         => SpecKind.Pages
    case _: SpecValue.FoldTypeSpec      => SpecKind.FoldType
    case _: SpecValue.BindingMethodSpec => SpecKind.BindingMethod

final case class ProductSpecifications(specs: Map[SpecKind, SpecValue]):
  def get(kind: SpecKind): Option[SpecValue] = specs.get(kind)
  def specKinds: Set[SpecKind] = specs.keySet

object ProductSpecifications:
  val empty: ProductSpecifications = ProductSpecifications(Map.empty)

  def fromSpecs(specs: Seq[SpecValue]): ProductSpecifications =
    ProductSpecifications(specs.map(s => SpecValue.specKind(s) -> s).toMap)
