package mpbuilder.domain.model

/** Visual product types supported by the editor */
enum VisualProductType:
  case MonthlyCalendar
  case WeeklyCalendar
  case BiweeklyCalendar
  case PhotoBook
  case WallPicture

  /** Default page count for this product type */
  def defaultPageCount: Int = this match
    case MonthlyCalendar  => 12
    case WeeklyCalendar   => 52
    case BiweeklyCalendar => 26
    case PhotoBook        => 12
    case WallPicture      => 1

  /** Localized display name */
  def displayName(lang: Language): String = this match
    case MonthlyCalendar  => if lang == Language.Cs then "Měsíční kalendář"  else "Monthly Calendar"
    case WeeklyCalendar   => if lang == Language.Cs then "Týdenní kalendář"  else "Weekly Calendar"
    case BiweeklyCalendar => if lang == Language.Cs then "Dvoutýdenní kalendář" else "Bi-weekly Calendar"
    case PhotoBook        => if lang == Language.Cs then "Fotokniha"         else "Photo Book"
    case WallPicture      => if lang == Language.Cs then "Obraz na zeď"      else "Wall Picture"

/** Physical product format with dimensions in mm */
final case class ProductFormat(
  id: String,
  nameEn: String,
  nameCs: String,
  widthMm: Int,
  heightMm: Int,
):
  /** Display name in the given language */
  def displayName(lang: Language): String = lang match
    case Language.En => nameEn
    case Language.Cs => nameCs

  /** Whether the format is landscape (width > height) */
  def isLandscape: Boolean = widthMm > heightMm

  /** Whether the format is square (width == height) */
  def isSquare: Boolean = widthMm == heightMm

  /** Whether the format is portrait (height > width) */
  def isPortrait: Boolean = heightMm > widthMm

  /** Orientation derived from dimensions */
  def orientation: Orientation =
    if widthMm > heightMm then Orientation.Landscape
    else Orientation.Portrait

  /** Convert to a domain Dimension */
  def toDimension: Dimension = Dimension(widthMm.toDouble, heightMm.toDouble)

  /** Convert to a SizeSpec for domain configuration */
  def toSizeSpec: SpecValue.SizeSpec = SpecValue.SizeSpec(toDimension)

object ProductFormat:
  // Calendar formats
  val WallCalendar: ProductFormat       = ProductFormat("wall-calendar",       "Wall Calendar",           "Nástěnný kalendář",           210, 297)
  val WallCalendarLarge: ProductFormat  = ProductFormat("wall-calendar-large", "Wall Calendar Large",     "Nástěnný kalendář velký",      297, 420)
  val DeskCalendar: ProductFormat       = ProductFormat("desk-calendar",       "Desk Calendar",           "Stolní kalendář",              297, 170)
  val DeskCalendarSmall: ProductFormat  = ProductFormat("desk-calendar-small", "Desk Calendar Small",     "Stolní kalendář malý",         210, 110)
  // Photo Book formats
  val PhotoBookSquare: ProductFormat    = ProductFormat("photobook-square",    "Photo Book Square",       "Fotokniha čtvercová",          210, 210)
  val PhotoBookLandscape: ProductFormat = ProductFormat("photobook-landscape", "Photo Book Landscape",    "Fotokniha na šířku",           297, 210)
  val PhotoBookPortrait: ProductFormat  = ProductFormat("photobook-portrait",  "Photo Book Portrait",     "Fotokniha na výšku",           210, 297)
  // Wall Picture formats
  val WallPictureSmall: ProductFormat     = ProductFormat("wall-picture-small",     "Wall Picture Small",     "Obraz malý",       210, 297)
  val WallPictureLarge: ProductFormat     = ProductFormat("wall-picture-large",     "Wall Picture Large",     "Obraz velký",      297, 420)
  val WallPictureLandscape: ProductFormat = ProductFormat("wall-picture-landscape", "Wall Picture Landscape", "Obraz na šířku",   420, 297)

  /** All defined formats */
  val all: List[ProductFormat] = List(
    WallCalendar, WallCalendarLarge, DeskCalendar, DeskCalendarSmall,
    PhotoBookSquare, PhotoBookLandscape, PhotoBookPortrait,
    WallPictureSmall, WallPictureLarge, WallPictureLandscape,
  )

  /** Formats applicable to each product type */
  def formatsFor(pt: VisualProductType): List[ProductFormat] = pt match
    case VisualProductType.MonthlyCalendar | VisualProductType.WeeklyCalendar | VisualProductType.BiweeklyCalendar =>
      List(WallCalendar, WallCalendarLarge, DeskCalendar, DeskCalendarSmall)
    case VisualProductType.PhotoBook =>
      List(PhotoBookSquare, PhotoBookLandscape, PhotoBookPortrait)
    case VisualProductType.WallPicture =>
      List(WallPictureSmall, WallPictureLarge, WallPictureLandscape)

  /** Default format for each product type */
  def defaultFor(pt: VisualProductType): ProductFormat = formatsFor(pt).head

  /** Find a format by its ID */
  def findById(id: String): Option[ProductFormat] = all.find(_.id == id)

  /** Whether the format is landscape (width > height) */
  def isLandscape(fmt: ProductFormat): Boolean = fmt.isLandscape
