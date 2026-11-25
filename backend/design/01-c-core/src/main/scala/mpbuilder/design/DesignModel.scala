package mpbuilder.design

import mpbuilder.shared.*

/** The Visual Editor design document: a serializable, print-oriented scene graph.
  *
  * Conventions (shared by the browser editor, previews and the future server-side PDF renderer):
  *   - Geometry is in **millimetres** (`Double`), origin = top-left corner of the **trim box**. The bleed
  *     area therefore spans `-bleed .. trim + bleed` on each axis.
  *   - Font sizes are in typographic **points** (1 pt = 1/72 in = 0.352778 mm).
  *   - Rotation is in **degrees clockwise** around the element centre.
  *   - Z-order is the element order inside its page (first = bottom).
  *   - The document never carries image bytes — only [[ImageAsset]] metadata referenced by [[AssetId]].
  */

// ── Identifiers ─────────────────────────────────────────────────────────────

private object DesignIdValidation:
  inline def token(name: String, raw: String): Validation[String, String] =
    val trimmed = raw.trim
    if trimmed.isEmpty then Validation.fail(s"$name must not be empty")
    else if trimmed.length > 64 then Validation.fail(s"$name must be at most 64 characters")
    else if !trimmed.forall(c => c.isLetterOrDigit || c == '-' || c == '_' || c == '.') then
      Validation.fail(s"$name may only contain letters, digits, '-', '_' and '.'")
    else Validation.succeed(trimmed)

opaque type ElementId = String
object ElementId:
  def apply(value: String): Validation[String, ElementId] = DesignIdValidation.token("ElementId", value)
  def unsafe(value: String): ElementId                   = value
  extension (id: ElementId) def value: String            = id
  given CanEqual[ElementId, ElementId]                   = CanEqual.derived

opaque type PageId = String
object PageId:
  def apply(value: String): Validation[String, PageId] = DesignIdValidation.token("PageId", value)
  def unsafe(value: String): PageId                   = value
  extension (id: PageId) def value: String            = id
  given CanEqual[PageId, PageId]                      = CanEqual.derived

/** Identifies an image used by a design. Content-addressed (`sha256`-derived) when known, so the same photo
  * uploaded twice is stored once — locally and on the server.
  */
opaque type AssetId = String
object AssetId:
  def apply(value: String): Validation[String, AssetId] = DesignIdValidation.token("AssetId", value)
  def unsafe(value: String): AssetId                   = value
  extension (id: AssetId) def value: String            = id
  given CanEqual[AssetId, AssetId]                     = CanEqual.derived

// ── Colour & paint ──────────────────────────────────────────────────────────

/** An sRGB colour stored as lowercase `#rrggbb`. Print conversion to CMYK happens in the PDF pipeline
  * (ICC profile of the selected press), never in the editor.
  */
opaque type DesignColor = String
object DesignColor:
  private val HexDigits = "0123456789abcdef"

  def parse(raw: String): Validation[String, DesignColor] =
    val v = raw.trim.toLowerCase
    val expanded =
      if v.length == 4 && v.startsWith("#") then "#" + v.drop(1).flatMap(c => s"$c$c")
      else v
    if expanded.length == 7 && expanded.startsWith("#") && expanded.drop(1).forall(HexDigits.contains(_)) then
      Validation.succeed(expanded)
    else Validation.fail(s"Invalid colour '$raw' (expected #rrggbb)")

  def unsafe(raw: String): DesignColor = parse(raw).getOrElse(raw)

  def fromRgb(r: Int, g: Int, b: Int): DesignColor =
    def hh(v: Int): String =
      val c = math.max(0, math.min(255, v))
      s"${HexDigits.charAt(c / 16)}${HexDigits.charAt(c % 16)}"
    s"#${hh(r)}${hh(g)}${hh(b)}"

  val Black: DesignColor = "#000000"
  val White: DesignColor = "#ffffff"
  val Ink: DesignColor   = "#1f2937"

  extension (c: DesignColor)
    def hex: String = c
    def red: Int    = Integer.parseInt(c.substring(1, 3), 16)
    def green: Int  = Integer.parseInt(c.substring(3, 5), 16)
    def blue: Int   = Integer.parseInt(c.substring(5, 7), 16)

    /** Relative luminance (0 = black, 1 = white) — used to pick legible guide / placeholder colours. */
    def luminance: Double =
      def ch(v: Int): Double =
        val s = v / 255.0
        if s <= 0.03928 then s / 12.92 else math.pow((s + 0.055) / 1.055, 2.4)
      0.2126 * ch(red) + 0.7152 * ch(green) + 0.0722 * ch(blue)

  given CanEqual[DesignColor, DesignColor] = CanEqual.derived

enum Fill derives CanEqual:
  case NoFill
  case Solid(color: DesignColor)
  /** Two-stop linear gradient; `angleDeg` 0 = left→right, 90 = top→bottom. */
  case Gradient(from: DesignColor, to: DesignColor, angleDeg: Double)

enum StrokeDash derives CanEqual:
  case Solid, Dashed, Dotted

final case class Stroke(color: DesignColor, widthMm: Double, dash: StrokeDash = StrokeDash.Solid) derives CanEqual

// ── Geometry ────────────────────────────────────────────────────────────────

final case class Point(x: Double, y: Double) derives CanEqual:
  def +(o: Point): Point = Point(x + o.x, y + o.y)
  def -(o: Point): Point = Point(x - o.x, y - o.y)

/** Axis-aligned rectangle (mm). */
final case class Rect(x: Double, y: Double, width: Double, height: Double) derives CanEqual:
  def right: Double   = x + width
  def bottom: Double  = y + height
  def centerX: Double = x + width / 2
  def centerY: Double = y + height / 2
  def contains(p: Point): Boolean = p.x >= x && p.x <= right && p.y >= y && p.y <= bottom
  def intersects(o: Rect): Boolean = x < o.right && right > o.x && y < o.bottom && bottom > o.y
  def inset(d: Double): Rect = Rect(x + d, y + d, math.max(0.0, width - 2 * d), math.max(0.0, height - 2 * d))
  def union(o: Rect): Rect =
    val nx = math.min(x, o.x)
    val ny = math.min(y, o.y)
    Rect(nx, ny, math.max(right, o.right) - nx, math.max(bottom, o.bottom) - ny)

/** Element placement: an un-rotated box plus a clockwise rotation around its centre. */
final case class Placement(x: Double, y: Double, width: Double, height: Double, rotation: Double = 0.0) derives CanEqual:
  def centerX: Double = x + width / 2
  def centerY: Double = y + height / 2
  def center: Point   = Point(centerX, centerY)
  def rect: Rect      = Rect(x, y, width, height)

  def moveBy(dx: Double, dy: Double): Placement = copy(x = x + dx, y = y + dy)
  def moveTo(nx: Double, ny: Double): Placement = copy(x = nx, y = ny)

  /** Corners (TL, TR, BR, BL) in page coordinates, rotation applied. */
  def corners: List[Point] =
    val c = center
    List(Point(x, y), Point(x + width, y), Point(x + width, y + height), Point(x, y + height))
      .map(p => DesignGeometry.rotateAround(p, c, rotation))

  /** Axis-aligned bounding box of the rotated frame. */
  def bounds: Rect =
    if rotation == 0.0 then rect
    else
      val cs   = corners
      val minX = cs.map(_.x).min
      val minY = cs.map(_.y).min
      Rect(minX, minY, cs.map(_.x).max - minX, cs.map(_.y).max - minY)

  /** Hit test honouring rotation. */
  def contains(p: Point): Boolean =
    val local = DesignGeometry.rotateAround(p, center, -rotation)
    rect.contains(local)

// ── Typography ──────────────────────────────────────────────────────────────

enum TextHAlign derives CanEqual:
  case Left, Center, Right, Justify

enum TextVAlign derives CanEqual:
  case Top, Middle, Bottom

final case class TextStyle(
    fontKey: String = FontCatalog.DefaultKey,
    sizePt: Double = 12.0,
    weight: Int = 400,
    italic: Boolean = false,
    color: DesignColor = DesignColor.Ink,
    align: TextHAlign = TextHAlign.Left,
    verticalAlign: TextVAlign = TextVAlign.Top,
    lineHeight: Double = 1.25,
    letterSpacingEm: Double = 0.0,
    uppercase: Boolean = false,
    underline: Boolean = false,
) derives CanEqual:
  def bold: Boolean = weight >= 600

// ── Element content ─────────────────────────────────────────────────────────

enum ImageFit derives CanEqual:
  /** Fill the frame, cropping the overflow (default for photo slots). */
  case Cover
  /** Show the whole image inside the frame (letter-boxed). */
  case Contain

enum ImageFilter derives CanEqual:
  case Original, Grayscale, Sepia

/** How the image is placed inside its frame: `zoom` ≥ 1 enlarges beyond the base fit, the focus point
  * (0..1 on each axis) selects which part of the image stays visible when cropping.
  */
final case class ImageCrop(fit: ImageFit = ImageFit.Cover, zoom: Double = 1.0, focusX: Double = 0.5, focusY: Double = 0.5)
    derives CanEqual

enum ShapeKind derives CanEqual:
  case Rectangle, Ellipse, Line, Triangle

enum WeekStart derives CanEqual:
  case Monday, Sunday

enum HolidaySet derives CanEqual:
  case NoHolidays, Czech

/** A live month grid. Rendered from its parameters (never stored as text), so the language, week start,
  * holidays and styling can change at any time and the print renderer reproduces it exactly.
  */
final case class CalendarSpec(
    year: Int,
    month: Int,
    language: Language = Language.En,
    weekStart: WeekStart = WeekStart.Monday,
    holidays: HolidaySet = HolidaySet.NoHolidays,
    showHeader: Boolean = true,
    showWeekdays: Boolean = true,
    showWeekNumbers: Boolean = false,
    showGrid: Boolean = false,
    fontKey: String = FontCatalog.DefaultKey,
    textColor: DesignColor = DesignColor.Ink,
    weekendColor: DesignColor = DesignColor.unsafe("#dc2626"),
    headerColor: DesignColor = DesignColor.Ink,
    mutedColor: DesignColor = DesignColor.unsafe("#9ca3af"),
) derives CanEqual

enum ElementContent derives CanEqual:
  case Text(text: String, style: TextStyle)
  /** A photo slot; `asset = None` is an empty placeholder the customer fills in. */
  case Image(
      asset: Option[AssetId],
      crop: ImageCrop = ImageCrop(),
      cornerRadiusMm: Double = 0.0,
      stroke: Option[Stroke] = None,
      filter: ImageFilter = ImageFilter.Original,
  )
  case Shape(kind: ShapeKind, fill: Fill, stroke: Option[Stroke] = None, cornerRadiusMm: Double = 0.0)
  case Clipart(art: ClipartKind, color: DesignColor)
  case Calendar(spec: CalendarSpec)

  def kindName: String = this match
    case _: Text     => "text"
    case _: Image    => "image"
    case _: Shape    => "shape"
    case _: Clipart  => "clipart"
    case _: Calendar => "calendar"

final case class DesignElement(
    id: ElementId,
    frame: Placement,
    content: ElementContent,
    opacity: Double = 1.0,
    locked: Boolean = false,
    name: Option[String] = None,
) derives CanEqual:
  def isText: Boolean  = content.isInstanceOf[ElementContent.Text]
  def isImage: Boolean = content.isInstanceOf[ElementContent.Image]

// ── Pages, format & document ────────────────────────────────────────────────

enum PageRole derives CanEqual:
  case Single
  case Front
  case Back
  case Cover
  case BackCover
  case Month(month: Int)
  case Inner

final case class DesignPage(
    id: PageId,
    role: PageRole,
    background: Fill = Fill.Solid(DesignColor.White),
    elements: Vector[DesignElement] = Vector.empty,
) derives CanEqual:
  def element(id: ElementId): Option[DesignElement] = elements.find(_.id == id)

enum BindingEdge derives CanEqual:
  case Top, Left, Right, Bottom

/** A no-content strip reserved for wire-o / spiral binding holes. */
final case class BindingGuide(edge: BindingEdge, depthMm: Double) derives CanEqual

/** Physical page geometry of the product being designed. */
final case class DesignFormat(
    trimWidthMm: Double,
    trimHeightMm: Double,
    bleedMm: Double = 3.0,
    safeMarginMm: Double = 4.0,
    binding: Option[BindingGuide] = None,
) derives CanEqual:
  def trimBox: Rect  = Rect(0, 0, trimWidthMm, trimHeightMm)
  def bleedBox: Rect = Rect(-bleedMm, -bleedMm, trimWidthMm + 2 * bleedMm, trimHeightMm + 2 * bleedMm)
  def safeBox: Rect =
    val base = trimBox.inset(safeMarginMm)
    binding match
      case None => base
      case Some(BindingGuide(edge, depth)) =>
        val extra = math.max(0.0, depth - safeMarginMm)
        edge match
          case BindingEdge.Top    => base.copy(y = base.y + extra, height = math.max(0.0, base.height - extra))
          case BindingEdge.Bottom => base.copy(height = math.max(0.0, base.height - extra))
          case BindingEdge.Left   => base.copy(x = base.x + extra, width = math.max(0.0, base.width - extra))
          case BindingEdge.Right  => base.copy(width = math.max(0.0, base.width - extra))
  def bindingZone: Option[Rect] = binding.map { case BindingGuide(edge, depth) =>
    edge match
      case BindingEdge.Top    => Rect(-bleedMm, -bleedMm, trimWidthMm + 2 * bleedMm, depth + bleedMm)
      case BindingEdge.Bottom => Rect(-bleedMm, trimHeightMm - depth, trimWidthMm + 2 * bleedMm, depth + bleedMm)
      case BindingEdge.Left   => Rect(-bleedMm, -bleedMm, depth + bleedMm, trimHeightMm + 2 * bleedMm)
      case BindingEdge.Right  => Rect(trimWidthMm - depth, -bleedMm, depth + bleedMm, trimHeightMm + 2 * bleedMm)
  }
  def isLandscape: Boolean = trimWidthMm > trimHeightMm

/** Allowed page count of the product (`step` = 2 for products printed as sheets of front+back, 4 for saddle stitch). */
final case class PagePolicy(minPages: Int, maxPages: Int, step: Int = 1) derives CanEqual:
  def isFixed: Boolean            = minPages == maxPages
  def allows(count: Int): Boolean = count >= minPages && count <= maxPages && (count - minPages) % math.max(1, step) == 0
  def canAdd(count: Int): Boolean    = allows(count + math.max(1, step))
  def canRemove(count: Int): Boolean = allows(count - math.max(1, step))

enum DesignProductKind derives CanEqual:
  case BusinessCard, Flyer, Poster, Postcard, WallCalendar, DeskCalendar, PhotoBook, Custom

/** What the design is for — links the document back to the storefront configuration it was opened from. */
final case class ProductBinding(
    kind: DesignProductKind,
    categoryId: Option[CategoryId] = None,
    componentKey: Option[ComponentKey] = None,
    label: Option[String] = None,
) derives CanEqual

enum AssetStorage derives CanEqual:
  /** Bytes exist only in this browser (IndexedDB). Must be uploaded before ordering / cross-device sync. */
  case DeviceOnly
  /** Uploaded to the server blob store (`BlobStorage` key). */
  case Uploaded(storageKey: String)
  /** Shipped with the application (stock photo / sample); never uploaded. */
  case Builtin(url: String)

final case class ImageAsset(
    id: AssetId,
    fileName: String,
    mimeType: String,
    pixelWidth: Int,
    pixelHeight: Int,
    sizeBytes: Long,
    sha256: Option[String] = None,
    storage: AssetStorage = AssetStorage.DeviceOnly,
) derives CanEqual:
  def aspect: Double = if pixelHeight <= 0 then 1.0 else pixelWidth.toDouble / pixelHeight.toDouble

final case class DesignDocument(
    formatVersion: Int,
    product: ProductBinding,
    format: DesignFormat,
    pagePolicy: PagePolicy,
    pages: Vector[DesignPage],
    assets: Vector[ImageAsset] = Vector.empty,
) derives CanEqual:
  def page(id: PageId): Option[DesignPage]   = pages.find(_.id == id)
  def pageIndex(id: PageId): Int             = pages.indexWhere(_.id == id)
  def asset(id: AssetId): Option[ImageAsset] = assets.find(_.id == id)
  def allElements: Vector[DesignElement]     = pages.flatMap(_.elements)
  def usedAssetIds: Set[AssetId] =
    allElements.flatMap(_.content match
      case ElementContent.Image(Some(a), _, _, _, _) => Some(a)
      case _                                         => None
    ).toSet
  def pendingUploads: Vector[ImageAsset] =
    val used = usedAssetIds
    assets.filter(a => used.contains(a.id) && a.storage == AssetStorage.DeviceOnly)

object DesignDocument:
  /** Bump on breaking model changes and add a JSON-level migration (see docs/analysis/visual-editor-architecture.md). */
  val CurrentFormatVersion: Int = 1

/** Millimetre / point conversions used across renderers. */
object Units:
  val MmPerPt: Double   = 25.4 / 72.0
  val MmPerInch: Double = 25.4
  def ptToMm(pt: Double): Double = pt * MmPerPt
  def mmToPt(mm: Double): Double = mm / MmPerPt
