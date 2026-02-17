package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** View model for managing calendar state */
object CalendarViewModel {

  // Main state
  private val stateVar: Var[CalendarState] = Var(CalendarState.empty)
  val state: Signal[CalendarState] = stateVar.signal

  // Unified element selection — a single selected element ID across all types
  private val selectedElementVar: Var[Option[String]] = Var(None)
  val selectedElement: Signal[Option[String]] = selectedElementVar.signal

  // Keep selectedPhoto as alias for backward compat in canvas rendering
  val selectedPhoto: Signal[Option[String]] = selectedElement

  // ID generation counter to avoid collisions
  private var idCounter: Int = 0

  private def generateId(prefix: String): String = {
    idCounter += 1
    s"$prefix-${System.currentTimeMillis()}-$idCounter"
  }

  private def nextZIndex(): Int = {
    val page = stateVar.now().currentPage
    if page.elements.isEmpty then 1
    else page.elements.map(_.zIndex).max + 1
  }

  // Current page as signal
  def currentPage: Signal[CalendarPage] = state.map(_.currentPage)

  def currentPageIndex: Signal[Int] = state.map(_.currentPageIndex)

  // Navigation
  def goToNextPage(): Unit = stateVar.update(_.goToNext)
  def goToPreviousPage(): Unit = stateVar.update(_.goToPrevious)
  def goToPage(index: Int): Unit = stateVar.update(_.goToPage(index))

  // ─── Generic element CRUD ────────────────────────────────────────

  private def addElement(element: CanvasElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page => page.copy(elements = page.elements :+ element))
    )

  def removeElement(elementId: String): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.filterNot(_.id == elementId))
      )
    )
    if selectedElementVar.now().contains(elementId) then
      selectedElementVar.set(None)
  }

  private def updateElement(elementId: String, updater: CanvasElement => CanvasElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map { e =>
          if e.id == elementId then updater(e) else e
        })
      )
    )

  def updateElementPosition(elementId: String, newPosition: Position): Unit =
    updateElement(elementId, _.withPosition(newPosition))

  def updateElementSize(elementId: String, newSize: Size): Unit =
    updateElement(elementId, _.withSize(newSize))

  def updateElementRotation(elementId: String, rotation: Double): Unit =
    updateElement(elementId, _.withRotation(rotation))

  // ─── Selection ───────────────────────────────────────────────────

  def selectElement(elementId: String): Unit =
    selectedElementVar.set(Some(elementId))

  def deselectElement(): Unit =
    selectedElementVar.set(None)

  // Legacy aliases used by canvas
  def selectPhoto(photoId: String): Unit = selectElement(photoId)
  def deselectPhoto(): Unit = deselectElement()

  // ─── Z-ordering ──────────────────────────────────────────────────

  def bringToFront(elementId: String): Unit = {
    val maxZ = stateVar.now().currentPage.elements.map(_.zIndex).maxOption.getOrElse(0)
    updateElement(elementId, _.withZIndex(maxZ + 1))
  }

  def sendToBack(elementId: String): Unit = {
    val minZ = stateVar.now().currentPage.elements.map(_.zIndex).minOption.getOrElse(0)
    updateElement(elementId, _.withZIndex(minZ - 1))
  }

  // ─── Duplicate ───────────────────────────────────────────────────

  def duplicateElement(elementId: String): Unit = {
    val page = stateVar.now().currentPage
    page.elements.find(_.id == elementId).foreach { elem =>
      val offset = Position(20, 20)
      val newId = generateId(elem match {
        case _: PhotoElement   => "photo"
        case _: TextElement    => "text"
        case _: ShapeElement   => "shape"
        case _: ClipartElement => "clipart"
      })
      val newZ = nextZIndex()
      val dup: CanvasElement = elem match {
        case p: PhotoElement   => p.copy(id = newId, position = Position(p.position.x + offset.x, p.position.y + offset.y), zIndex = newZ)
        case t: TextElement    => t.copy(id = newId, position = Position(t.position.x + offset.x, t.position.y + offset.y), zIndex = newZ)
        case s: ShapeElement   => s.copy(id = newId, position = Position(s.position.x + offset.x, s.position.y + offset.y), zIndex = newZ)
        case c: ClipartElement => c.copy(id = newId, position = Position(c.position.x + offset.x, c.position.y + offset.y), zIndex = newZ)
      }
      addElement(dup)
      selectedElementVar.set(Some(newId))
    }
  }

  // ─── Photo operations ────────────────────────────────────────────

  def uploadPhoto(imageData: String): Unit = {
    val photoId = generateId("photo")
    val photo = PhotoElement(
      id = photoId,
      imageData = imageData,
      position = Position(100, 100),
      size = Size(300, 200),
      zIndex = nextZIndex(),
    )
    addElement(photo)
    selectedElementVar.set(Some(photoId))
  }

  def removePhoto(photoId: String): Unit = removeElement(photoId)

  def updatePhotoPosition(photoId: String, newPosition: Position): Unit =
    updateElementPosition(photoId, newPosition)

  def updatePhotoSize(photoId: String, newSize: Size): Unit =
    updateElementSize(photoId, newSize)

  def updatePhotoRotation(photoId: String, rotation: Double): Unit =
    updateElementRotation(photoId, rotation)

  // ─── Text element operations ─────────────────────────────────────

  def addTextField(): Unit = {
    val textId = generateId("text")
    val textElement = TextElement(
      id = textId,
      text = "New Text",
      position = Position(200, 200),
      size = Size(200, 40),
      fontSize = 16,
      fontFamily = "Arial",
      color = "#000000",
      zIndex = nextZIndex(),
    )
    addElement(textElement)
    selectedElementVar.set(Some(textId))
  }

  def removeTextField(textId: String): Unit = removeElement(textId)

  def updateTextField(textId: String, updater: TextElement => TextElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map {
          case e: TextElement if e.id == textId => updater(e)
          case other => other
        })
      )
    )

  def updateTextFieldText(textId: String, newText: String): Unit =
    updateTextField(textId, _.copy(text = newText))

  def updateTextFieldPosition(textId: String, newPosition: Position): Unit =
    updateTextField(textId, _.copy(position = newPosition))

  def updateTextFieldFontSize(textId: String, newSize: Int): Unit =
    updateTextField(textId, _.copy(fontSize = newSize))

  def updateTextFieldColor(textId: String, newColor: String): Unit =
    updateTextField(textId, _.copy(color = newColor))

  def updateTextFieldFontFamily(textId: String, newFont: String): Unit =
    updateTextField(textId, _.copy(fontFamily = newFont))

  def updateTextFieldBold(textId: String, bold: Boolean): Unit =
    updateTextField(textId, _.copy(bold = bold))

  def updateTextFieldItalic(textId: String, italic: Boolean): Unit =
    updateTextField(textId, _.copy(italic = italic))

  def updateTextFieldAlign(textId: String, align: TextAlignment): Unit =
    updateTextField(textId, _.copy(textAlign = align))

  // ─── Shape operations ────────────────────────────────────────────

  def addShape(shapeType: ShapeType): Unit = {
    val shapeId = generateId("shape")
    val shape = ShapeElement(
      id = shapeId,
      shapeType = shapeType,
      position = Position(150, 150),
      size = shapeType match {
        case ShapeType.Line      => Size(200, 4)
        case ShapeType.Rectangle => Size(150, 100)
      },
      zIndex = nextZIndex(),
    )
    addElement(shape)
    selectedElementVar.set(Some(shapeId))
  }

  def updateShape(shapeId: String, updater: ShapeElement => ShapeElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map {
          case e: ShapeElement if e.id == shapeId => updater(e)
          case other => other
        })
      )
    )

  // ─── Clipart operations ──────────────────────────────────────────

  def addClipart(imageData: String): Unit = {
    val clipId = generateId("clipart")
    val clip = ClipartElement(
      id = clipId,
      imageData = imageData,
      position = Position(150, 150),
      size = Size(100, 100),
      zIndex = nextZIndex(),
    )
    addElement(clip)
    selectedElementVar.set(Some(clipId))
  }

  // ─── Background operations ───────────────────────────────────────

  def setBackgroundColor(color: String): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(template = page.template.copy(background = PageBackground.SolidColor(color)))
      )
    )

  def setBackgroundImage(imageData: String): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(template = page.template.copy(background = PageBackground.BackgroundImage(imageData)))
      )
    )

  // ─── Template operations ─────────────────────────────────────────

  def setTemplateType(templateType: CalendarTemplateType): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(template = page.template.copy(templateType = templateType))
      )
    )

  // ─── Reset & language ────────────────────────────────────────────

  def reset(): Unit = {
    stateVar.set(CalendarState.empty)
    selectedElementVar.set(None)
  }

  def updateLanguage(lang: String): Unit =
    stateVar.update(state => CalendarState.updateLanguage(state, lang))
}
