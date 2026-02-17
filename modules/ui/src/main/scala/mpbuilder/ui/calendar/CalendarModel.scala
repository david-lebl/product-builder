package mpbuilder.ui.calendar

/** Position on the calendar page (in pixels) */
case class Position(x: Double, y: Double)

/** Size of an element (in pixels) */
case class Size(width: Double, height: Double)

/** Photo element on a calendar page */
case class PhotoElement(
  id: String,
  imageData: String, // Base64 or URL
  position: Position,
  size: Size,
  rotation: Double = 0.0, // degrees
)

/** Text field on a calendar page */
case class TextField(
  id: String,
  text: String,
  position: Position,
  fontSize: Int = 14,
  fontFamily: String = "Arial",
  color: String = "#000000",
  locked: Boolean = false, // locked fields (month/day) cannot be edited/moved
)

/** Calendar template background */
case class CalendarTemplate(
  backgroundImage: String, // URL or base64
  monthField: TextField, // locked text field for month name
  daysGrid: List[TextField], // locked text fields for days
)

/** A single page in the calendar */
case class CalendarPage(
  pageNumber: Int, // 1-12 for each month
  template: CalendarTemplate,
  photo: Option[PhotoElement] = None,
  customTextFields: List[TextField] = List.empty,
)

/** Complete calendar state */
case class CalendarState(
  pages: List[CalendarPage], // 12 pages
  currentPageIndex: Int = 0, // 0-11
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
    val months = List(
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    )
    
    val pages = months.zipWithIndex.map { case (monthName, index) =>
      // Create a simple template for each month
      val template = CalendarTemplate(
        backgroundImage = "white", // placeholder
        monthField = TextField(
          id = s"month-${index + 1}",
          text = monthName,
          position = Position(50, 30),
          fontSize = 24,
          fontFamily = "Arial",
          locked = true
        ),
        daysGrid = createDaysGrid(index + 1)
      )
      
      CalendarPage(
        pageNumber = index + 1,
        template = template
      )
    }
    
    CalendarState(pages)
  }
  
  /** Create a grid of day text fields for a month */
  private def createDaysGrid(month: Int): List[TextField] = {
    // Simple 7x6 grid for calendar days
    // For now, just create placeholders - actual calendar logic can be added later
    val daysInMonth = month match {
      case 2 => 28 // February (simplified)
      case 4 | 6 | 9 | 11 => 30
      case _ => 31
    }
    
    (1 to daysInMonth).map { day =>
      val row = (day - 1) / 7
      val col = (day - 1) % 7
      TextField(
        id = s"day-$month-$day",
        text = day.toString,
        position = Position(50 + col * 80, 80 + row * 50),
        fontSize = 12,
        fontFamily = "Arial",
        locked = true
      )
    }.toList
  }
}
