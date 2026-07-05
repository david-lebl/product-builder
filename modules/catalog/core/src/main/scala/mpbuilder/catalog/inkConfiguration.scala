package mpbuilder.catalog

enum InkType:
  case CMYK, PMS, Grayscale, White, None

final case class InkSetup(inkType: InkType, colorCount: Int)

object InkSetup:
  val cmyk: InkSetup = InkSetup(InkType.CMYK, 4)
  val none: InkSetup = InkSetup(InkType.None, 0)
  val grayscale: InkSetup = InkSetup(InkType.Grayscale, 1)
  val white: InkSetup = InkSetup(InkType.White, 1)
  def pms(n: Int): InkSetup = InkSetup(InkType.PMS, n)

final case class InkConfiguration(front: InkSetup, back: InkSetup):
  def notation: String =
    if back.inkType == InkType.White then s"${front.colorCount}/0+W"
    else s"${front.colorCount}/${back.colorCount}"
  def maxColorCount: Int = math.max(front.colorCount, back.colorCount)
  def isSingleSided: Boolean = back.inkType == InkType.None || back.inkType == InkType.White
  def isDoubleSided: Boolean = back.inkType != InkType.None && back.inkType != InkType.White

object InkConfiguration:
  val cmyk4_4: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.cmyk)
  val cmyk4_0: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.none)
  val cmyk4_1: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.grayscale)
  val mono1_0: InkConfiguration = InkConfiguration(InkSetup.grayscale, InkSetup.none)
  val mono1_1: InkConfiguration = InkConfiguration(InkSetup.grayscale, InkSetup.grayscale)
  val cmyk4_0_white: InkConfiguration = InkConfiguration(InkSetup.cmyk, InkSetup.white)
  val noInk: InkConfiguration = InkConfiguration(InkSetup.none, InkSetup.none)
