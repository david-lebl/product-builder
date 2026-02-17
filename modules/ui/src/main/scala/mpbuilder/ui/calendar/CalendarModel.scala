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
}

object CalendarState {
  /** Create a new calendar with 12 blank pages */
  def empty: CalendarState = {
    val monthsEn = List(
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    )

    val pages = monthsEn.zipWithIndex.map { case (monthName, index) =>
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
      )
    }

    CalendarState(pages)
  }

  /** Update month names based on language */
  def updateLanguage(state: CalendarState, lang: String): CalendarState = {
    val months = if lang == "cs" then
      List("Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
           "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec")
    else
      List("January", "February", "March", "April", "May", "June",
           "July", "August", "September", "October", "November", "December")

    val updatedPages = state.pages.zipWithIndex.map { case (page, index) =>
      val updatedTemplate = page.template.copy(
        monthField = page.template.monthField.copy(text = months(index))
      )
      page.copy(template = updatedTemplate)
    }

    state.copy(pages = updatedPages)
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
