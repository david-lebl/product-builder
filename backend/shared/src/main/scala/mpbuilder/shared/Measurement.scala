package mpbuilder.shared

import zio.prelude.Validation

/** Physical measurements using `BigDecimal` to avoid floating-point equality issues. */

opaque type PositiveMm = BigDecimal
object PositiveMm:
  def apply(mm: BigDecimal): Validation[String, PositiveMm] =
    if mm > BigDecimal(0) then Validation.succeed(mm)
    else Validation.fail(s"Measurement must be positive (> 0 mm), got $mm")

  def unsafe(mm: BigDecimal): PositiveMm = mm
  def unsafe(mm: Int): PositiveMm        = BigDecimal(mm)

  given scala.math.Ordering[PositiveMm] = scala.math.Ordering.BigDecimal

  extension (m: PositiveMm)
    def value: BigDecimal                   = m
    def *(factor: BigDecimal): PositiveMm   = m * factor
    def *(factor: Int): PositiveMm          = m * BigDecimal(factor)
    def +(extra: NonNegativeMm): PositiveMm = m + extra.value
    def >=(other: PositiveMm): Boolean      = m >= other
    def <=(other: PositiveMm): Boolean      = m <= other
    def >(other: PositiveMm): Boolean       = m > other
    def <(other: PositiveMm): Boolean       = m < other

opaque type NonNegativeMm = BigDecimal
object NonNegativeMm:
  val zero: NonNegativeMm = BigDecimal(0)

  def apply(mm: BigDecimal): Validation[String, NonNegativeMm] =
    if mm >= BigDecimal(0) then Validation.succeed(mm)
    else Validation.fail(s"Measurement must be non-negative (>= 0 mm), got $mm")

  def unsafe(mm: BigDecimal): NonNegativeMm = mm
  def unsafe(mm: Int): NonNegativeMm        = BigDecimal(mm)

  extension (m: NonNegativeMm)
    def value: BigDecimal = m
    def *(factor: Int): NonNegativeMm = m * BigDecimal(factor)

opaque type SquareMeters = BigDecimal
object SquareMeters:
  val zero: SquareMeters = BigDecimal(0)

  def apply(sqm: BigDecimal): Validation[String, SquareMeters] =
    if sqm >= BigDecimal(0) then Validation.succeed(sqm)
    else Validation.fail(s"Area must be non-negative, got $sqm m²")

  def unsafe(sqm: BigDecimal): SquareMeters = sqm

  given scala.math.Ordering[SquareMeters] = scala.math.Ordering.BigDecimal

  extension (a: SquareMeters)
    def value: BigDecimal                   = a
    def +(other: SquareMeters): SquareMeters = a + other
    def *(count: Int): SquareMeters         = a * BigDecimal(count)
    def >=(other: SquareMeters): Boolean    = a >= other
    def <=(other: SquareMeters): Boolean    = a <= other
    def <(other: SquareMeters): Boolean     = a < other

opaque type Grams = BigDecimal
object Grams:
  val zero: Grams = BigDecimal(0)

  def apply(g: BigDecimal): Validation[String, Grams] =
    if g >= BigDecimal(0) then Validation.succeed(g)
    else Validation.fail(s"Weight must be non-negative, got $g g")

  def unsafe(g: BigDecimal): Grams = g

  extension (g: Grams)
    def value: BigDecimal             = g
    def +(other: Grams): Grams        = g + other
    def *(count: Int): Grams          = g * BigDecimal(count)
    def toKilograms: BigDecimal       = g / BigDecimal(1000)

opaque type PaperWeight = Int
object PaperWeight:
  def apply(gsm: Int): Validation[String, PaperWeight] =
    if gsm > 0 && gsm <= 2000 then Validation.succeed(gsm)
    else Validation.fail(s"PaperWeight must be between 1 and 2000 gsm, got $gsm")

  def unsafe(gsm: Int): PaperWeight = gsm

  extension (w: PaperWeight) def gsm: Int = w

final case class Dimension2D(width: PositiveMm, height: PositiveMm):
  def areaSqM: SquareMeters =
    SquareMeters.unsafe((width.value * height.value) / BigDecimal(1_000_000))

  def perimeterMeters: BigDecimal =
    (BigDecimal(2) * (width.value + height.value)) / BigDecimal(1000)

  def isSquare: Boolean = width.value == height.value

  def fitsInside(container: Dimension2D): Boolean =
    (width <= container.width && height <= container.height) ||
      (height <= container.width && width <= container.height)

  def rotated: Dimension2D = Dimension2D(height, width)

object Dimension2D:
  def mm(width: Int, height: Int): Validation[String, Dimension2D] =
    Validation.validateWith(PositiveMm(BigDecimal(width)), PositiveMm(BigDecimal(height)))(Dimension2D.apply)

  def unsafeMm(width: Int, height: Int): Dimension2D =
    Dimension2D(PositiveMm.unsafe(width), PositiveMm.unsafe(height))

  val A6: Dimension2D  = unsafeMm(105, 148)
  val A5: Dimension2D  = unsafeMm(148, 210)
  val A4: Dimension2D  = unsafeMm(210, 297)
  val A3: Dimension2D  = unsafeMm(297, 420)
  val DL: Dimension2D  = unsafeMm(99, 210)
  val SRA3: Dimension2D = unsafeMm(320, 450)
  val B2: Dimension2D  = unsafeMm(500, 707)

/** Three-dimensional box / packaging specification (e.g. FEFCO folding carton). */
final case class Dimension3D(length: PositiveMm, width: PositiveMm, height: PositiveMm):
  /** Approximate flat die-cut blank footprint if no custom dieline is provided. */
  def approximateBlank2D: Dimension2D =
    val flatW = PositiveMm.unsafe(BigDecimal(2) * (length.value + width.value) + BigDecimal(30))
    val flatH = PositiveMm.unsafe(height.value + BigDecimal(2) * width.value + BigDecimal(30))
    Dimension2D(flatW, flatH)

/** Positive order quantity (number of finished items). */
opaque type Quantity = Int
object Quantity:
  val one: Quantity = 1

  def apply(value: Int): Validation[String, Quantity] =
    if value > 0 then Validation.succeed(value)
    else Validation.fail(s"Quantity must be positive, got $value")

  def unsafe(value: Int): Quantity = value

  extension (q: Quantity) def value: Int = q

/** Number of distinct artwork versions sharing a print run (e.g. 5 employees × 100 cards = 500 pcs, 5 versions). */
opaque type VersionCount = Int
object VersionCount:
  val single: VersionCount = 1

  def apply(value: Int): Validation[String, VersionCount] =
    if value > 0 then Validation.succeed(value)
    else Validation.fail(s"VersionCount must be positive, got $value")

  def unsafe(value: Int): VersionCount = value

  extension (v: VersionCount)
    def value: Int = v
    def extraVersions: Int = math.max(0, v - 1)
