package mpbuilder.ui.calendar

/** Position on the calendar page (in pixels) */
case class Position(x: Double, y: Double)

/** Size of an element (in pixels) */
case class Size(width: Double, height: Double)

/** Text alignment for text elements */
enum TextAlignment:
  case Left, Center, Right

/** Shape types for shape elements */
enum ShapeType:
  case Line, Rectangle

/** Background options for a calendar page */
enum PageBackground:
  case SolidColor(color: String)
  case BackgroundImage(imageData: String)

/** Calendar template types */
enum CalendarTemplateType:
  case GridTemplate

/** Visual product types supported by the editor */
enum VisualProductType:
  case MonthlyCalendar
  case WeeklyCalendar
  case BiweeklyCalendar
  case PhotoBook
  case WallPicture

/** Physical product format with dimensions in mm */
case class ProductFormat(
  id: String,
  nameEn: String,
  nameCs: String,
  widthMm: Int,
  heightMm: Int,
)

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

  /** Whether the format is landscape (width > height) */
  def isLandscape(fmt: ProductFormat): Boolean = fmt.widthMm > fmt.heightMm

/** Canvas element ADT — all user-placed items on a calendar page */
sealed trait CanvasElement:
  def id: String
  def position: Position
  def size: Size
  def rotation: Double
  def zIndex: Int
  def withPosition(p: Position): CanvasElement
  def withSize(s: Size): CanvasElement
  def withRotation(r: Double): CanvasElement
  def withZIndex(z: Int): CanvasElement

/** Photo element on a calendar page */
case class PhotoElement(
  id: String,
  imageData: String,
  position: Position,
  size: Size,
  rotation: Double = 0.0,
  zIndex: Int = 0,
  imageScale: Double = 1.0,
  imageOffsetX: Double = 0.0,
  imageOffsetY: Double = 0.0,
) extends CanvasElement:
  def withPosition(p: Position): PhotoElement = copy(position = p)
  def withSize(s: Size): PhotoElement = copy(size = s)
  def withRotation(r: Double): PhotoElement = copy(rotation = r)
  def withZIndex(z: Int): PhotoElement = copy(zIndex = z)

/** User-editable text element on a calendar page */
case class TextElement(
  id: String,
  text: String,
  position: Position,
  size: Size,
  rotation: Double = 0.0,
  zIndex: Int = 0,
  fontSize: Int = 14,
  fontFamily: String = "Arial",
  color: String = "#000000",
  bold: Boolean = false,
  italic: Boolean = false,
  textAlign: TextAlignment = TextAlignment.Left,
) extends CanvasElement:
  def withPosition(p: Position): TextElement = copy(position = p)
  def withSize(s: Size): TextElement = copy(size = s)
  def withRotation(r: Double): TextElement = copy(rotation = r)
  def withZIndex(z: Int): TextElement = copy(zIndex = z)

/** Shape element on a calendar page */
case class ShapeElement(
  id: String,
  shapeType: ShapeType,
  position: Position,
  size: Size,
  rotation: Double = 0.0,
  zIndex: Int = 0,
  strokeColor: String = "#000000",
  fillColor: String = "transparent",
  strokeWidth: Double = 2.0,
) extends CanvasElement:
  def withPosition(p: Position): ShapeElement = copy(position = p)
  def withSize(s: Size): ShapeElement = copy(size = s)
  def withRotation(r: Double): ShapeElement = copy(rotation = r)
  def withZIndex(z: Int): ShapeElement = copy(zIndex = z)

/** Clipart / sticker element on a calendar page */
case class ClipartElement(
  id: String,
  imageData: String,
  position: Position,
  size: Size,
  rotation: Double = 0.0,
  zIndex: Int = 0,
) extends CanvasElement:
  def withPosition(p: Position): ClipartElement = copy(position = p)
  def withSize(s: Size): ClipartElement = copy(size = s)
  def withRotation(r: Double): ClipartElement = copy(rotation = r)
  def withZIndex(z: Int): ClipartElement = copy(zIndex = z)

/** Template text field for locked month/day labels (NOT a CanvasElement) */
case class TemplateTextField(
  id: String,
  text: String,
  position: Position,
  fontSize: Int = 14,
  fontFamily: String = "Arial",
  color: String = "#000000",
)

/** Calendar template */
case class CalendarTemplate(
  templateType: CalendarTemplateType = CalendarTemplateType.GridTemplate,
  background: PageBackground = PageBackground.SolidColor("#ffffff"),
  monthField: TemplateTextField,
  daysGrid: List[TemplateTextField],
)

/** A single page in the calendar */
case class CalendarPage(
  pageNumber: Int,
  template: CalendarTemplate,
  elements: List[CanvasElement] = List.empty,
)

/** Complete calendar state */
case class CalendarState(
  pages: List[CalendarPage],
  currentPageIndex: Int = 0,
  productType: VisualProductType = VisualProductType.MonthlyCalendar,
  productFormat: ProductFormat = ProductFormat.WallCalendar,
) {
  def currentPage: CalendarPage = pages(currentPageIndex)

  def updateCurrentPage(updater: CalendarPage => CalendarPage): CalendarState =
    copy(pages = pages.updated(currentPageIndex, updater(currentPage)))

  def goToNext: CalendarState =
    if currentPageIndex < pages.length - 1 then
      copy(currentPageIndex = currentPageIndex + 1)
    else
      this

  def goToPrevious: CalendarState =
    if currentPageIndex > 0 then
      copy(currentPageIndex = currentPageIndex - 1)
    else
      this

  def goToPage(index: Int): CalendarState =
    if index >= 0 && index < pages.length then
      copy(currentPageIndex = index)
    else
      this

  /** Apply a background to all pages */
  def applyBackgroundToAll(bg: PageBackground): CalendarState =
    copy(pages = pages.map(page =>
      page.copy(template = page.template.copy(background = bg))
    ))

  /** Apply a template type to all pages */
  def applyTemplateTypeToAll(tt: CalendarTemplateType): CalendarState =
    copy(pages = pages.map(page =>
      page.copy(template = page.template.copy(templateType = tt))
    ))
}

object CalendarState {
  /** Create a default state for the given product type and format */
  def create(
    productType: VisualProductType = VisualProductType.MonthlyCalendar,
    format: ProductFormat = ProductFormat.WallCalendar,
    lang: String = "en",
  ): CalendarState = {
    val pages = productType match {
      case VisualProductType.MonthlyCalendar   => createMonthlyCalendarPages(lang)
      case VisualProductType.WeeklyCalendar    => createWeeklyCalendarPages(lang)
      case VisualProductType.BiweeklyCalendar  => createBiweeklyCalendarPages(lang)
      case VisualProductType.PhotoBook         => createPhotoBookPages(lang)
      case VisualProductType.WallPicture       => createWallPicturePages(lang)
    }
    CalendarState(pages, productType = productType, productFormat = format)
  }

  /** Create a new calendar with 12 blank pages (backward compat) */
  def empty: CalendarState = create()

  /** Update page titles based on language */
  def updateLanguage(state: CalendarState, lang: String): CalendarState = {
    state.productType match {
      case VisualProductType.MonthlyCalendar =>
        val months = monthNames(lang)
        val updatedPages = state.pages.zipWithIndex.map { case (page, index) =>
          val updatedTemplate = page.template.copy(
            monthField = page.template.monthField.copy(text = months(index))
          )
          page.copy(template = updatedTemplate)
        }
        state.copy(pages = updatedPages)

      case VisualProductType.WeeklyCalendar =>
        val weekLabel = if lang == "cs" then "Týden" else "Week"
        val updatedPages = state.pages.zipWithIndex.map { case (page, index) =>
          val updatedTemplate = page.template.copy(
            monthField = page.template.monthField.copy(text = s"$weekLabel ${index + 1}")
          )
          page.copy(template = updatedTemplate)
        }
        state.copy(pages = updatedPages)

      case VisualProductType.BiweeklyCalendar =>
        val weeksLabel = if lang == "cs" then "Týdny" else "Weeks"
        val updatedPages = state.pages.zipWithIndex.map { case (page, index) =>
          val startWeek = index * 2 + 1
          val endWeek = startWeek + 1
          val updatedTemplate = page.template.copy(
            monthField = page.template.monthField.copy(text = s"$weeksLabel $startWeek–$endWeek")
          )
          page.copy(template = updatedTemplate)
        }
        state.copy(pages = updatedPages)

      case VisualProductType.PhotoBook =>
        // Photo book only has a small page number — no language update needed
        state

      case VisualProductType.WallPicture =>
        // Wall picture has no visible text — no language update needed
        state
    }
  }

  /** Default page count for each product type */
  def defaultPageCount(productType: VisualProductType): Int = productType match {
    case VisualProductType.MonthlyCalendar  => 12
    case VisualProductType.WeeklyCalendar   => 52
    case VisualProductType.BiweeklyCalendar => 26
    case VisualProductType.PhotoBook        => 12
    case VisualProductType.WallPicture      => 1
  }

  private def monthNames(lang: String): List[String] =
    if lang == "cs" then
      List("Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
           "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec")
    else
      List("January", "February", "March", "April", "May", "June",
           "July", "August", "September", "October", "November", "December")

  // ─── Factory methods for each product type ─────────────────────

  private def createMonthlyCalendarPages(lang: String): List[CalendarPage] = {
    val months = monthNames(lang)
    months.zipWithIndex.map { case (monthName, index) =>
      val template = CalendarTemplate(
        monthField = TemplateTextField(
          id = s"month-${index + 1}",
          text = monthName,
          position = Position(50, 30),
          fontSize = 24,
          fontFamily = "Arial",
        ),
        daysGrid = createDaysGrid(index + 1),
      )
      CalendarPage(
        pageNumber = index + 1,
        template = template,
        elements = List(
          PhotoElement(
            id = s"img-month-${index + 1}",
            imageData = "",
            position = Position(50, 350),
            size = Size(460, 220),
          )
        ),
      )
    }
  }

  private def createWeeklyCalendarPages(lang: String): List[CalendarPage] = {
    val weekLabel = if lang == "cs" then "Týden" else "Week"
    val dayLabels = if lang == "cs" then
      List("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    else
      List("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    (1 to 52).map { weekNum =>
      val template = CalendarTemplate(
        monthField = TemplateTextField(
          id = s"week-$weekNum",
          text = s"$weekLabel $weekNum",
          position = Position(50, 30),
          fontSize = 24,
          fontFamily = "Arial",
        ),
        daysGrid = dayLabels.zipWithIndex.map { case (dayName, col) =>
          TemplateTextField(
            id = s"day-w$weekNum-$col",
            text = dayName,
            position = Position(50 + col * 80, 80),
            fontSize = 12,
            fontFamily = "Arial",
          )
        },
      )
      CalendarPage(
        pageNumber = weekNum,
        template = template,
        elements = List(
          PhotoElement(
            id = s"img-week-$weekNum",
            imageData = "",
            position = Position(50, 130),
            size = Size(460, 250),
          )
        ),
      )
    }.toList
  }

  private def createBiweeklyCalendarPages(lang: String): List[CalendarPage] = {
    val weeksLabel = if lang == "cs" then "Týdny" else "Weeks"
    val dayLabels = if lang == "cs" then
      List("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    else
      List("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    (1 to 26).map { biweekNum =>
      val startWeek = (biweekNum - 1) * 2 + 1
      val endWeek = startWeek + 1
      val template = CalendarTemplate(
        monthField = TemplateTextField(
          id = s"biweek-$biweekNum",
          text = s"$weeksLabel $startWeek–$endWeek",
          position = Position(50, 30),
          fontSize = 24,
          fontFamily = "Arial",
        ),
        daysGrid = (0 until 14).map { day =>
          val row = day / 7
          val col = day % 7
          TemplateTextField(
            id = s"day-bw$biweekNum-$day",
            text = dayLabels(col),
            position = Position(50 + col * 80, 80 + row * 50),
            fontSize = 12,
            fontFamily = "Arial",
          )
        }.toList,
      )
      CalendarPage(
        pageNumber = biweekNum,
        template = template,
        elements = List(
          PhotoElement(
            id = s"img-biweek-$biweekNum",
            imageData = "",
            position = Position(50, 200),
            size = Size(460, 220),
          )
        ),
      )
    }.toList
  }

  private def createPhotoBookPages(lang: String): List[CalendarPage] = {
    (1 to 12).map { pageNum =>
      val template = CalendarTemplate(
        monthField = TemplateTextField(
          id = s"page-num-$pageNum",
          text = pageNum.toString,
          position = Position(270, 570),
          fontSize = 10,
          fontFamily = "Arial",
          color = "#999999",
        ),
        daysGrid = List.empty,
      )
      CalendarPage(
        pageNumber = pageNum,
        template = template,
        elements = List(
          PhotoElement(
            id = s"img-page-$pageNum",
            imageData = "",
            position = Position(30, 30),
            size = Size(500, 520),
          )
        ),
      )
    }.toList
  }

  private def createWallPicturePages(lang: String): List[CalendarPage] = {
    List(
      CalendarPage(
        pageNumber = 1,
        template = CalendarTemplate(
          monthField = TemplateTextField(
            id = "picture-hidden",
            text = "",
            position = Position(0, 0),
            fontSize = 0,
          ),
          daysGrid = List.empty,
        ),
        elements = List(
          PhotoElement(
            id = "img-picture-1",
            imageData = "",
            position = Position(20, 20),
            size = Size(520, 555),
          )
        ),
      )
    )
  }

  /** Create a grid of day template text fields for a month */
  private def createDaysGrid(month: Int): List[TemplateTextField] = {
    val daysInMonth = month match {
      case 2 => 28
      case 4 | 6 | 9 | 11 => 30
      case _ => 31
    }

    (1 to daysInMonth).map { day =>
      val row = (day - 1) / 7
      val col = (day - 1) % 7
      TemplateTextField(
        id = s"day-$month-$day",
        text = day.toString,
        position = Position(50 + col * 80, 80 + row * 50),
        fontSize = 12,
        fontFamily = "Arial",
      )
    }.toList
  }
}
