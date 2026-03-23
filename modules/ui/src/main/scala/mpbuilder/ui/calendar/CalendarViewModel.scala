package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js
import scala.concurrent.ExecutionContext.Implicits.global

/** View model for managing calendar state */
object CalendarViewModel {

  // ─── Main editor state ─────────────────────────────────────────
  private val stateVar: Var[CalendarState] = Var(CalendarState.empty)
  val state: Signal[CalendarState] = stateVar.signal

  // Product type and format as derived signals
  val productType: Signal[VisualProductType] = state.map(_.productType)
  val productFormat: Signal[ProductFormat] = state.map(_.productFormat)

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

  private def nextZIndex(): Int =
    stateVar.now().currentPage.elements.map(_.zIndex).maxOption.getOrElse(0) + 1

  // Current page as signal
  def currentPage: Signal[CalendarPage] = state.map(_.currentPage)

  def currentPageIndex: Signal[Int] = state.map(_.currentPageIndex)

  /** Snapshot of the current page (for one-time lookups, not reactive) */
  def currentPageSnapshot(): CalendarPage = stateVar.now().currentPage

  // ─── Session state ─────────────────────────────────────────────

  private val currentSessionVar: Var[Option[SessionMeta]] = Var(None)
  val currentSession: Signal[Option[SessionMeta]] = currentSessionVar.signal

  private val saveStatusVar: Var[SaveStatus] = Var(SaveStatus.Unsaved)
  val saveStatus: Signal[SaveStatus] = saveStatusVar.signal

  private val sessionListVar: Var[List[SessionSummary]] = Var(List.empty)
  val sessionList: Signal[List[SessionSummary]] = sessionListVar.signal

  // Whether to show the resume dialog on mount
  private val showResumeDialogVar: Var[Boolean] = Var(false)
  val showResumeDialog: Signal[Boolean] = showResumeDialogVar.signal

  // Suppress auto-save during session load
  private var suppressAutoSave: Boolean = false

  // Debounce timer handle for auto-save
  private var saveTimerHandle: Option[js.timers.SetTimeoutHandle] = None
  private val AutoSaveDelayMs = 3000

  // ─── Gallery state ─────────────────────────────────────────────

  private val galleryImagesVar: Var[List[GalleryImage]] = Var(List.empty)
  val galleryImages: Signal[List[GalleryImage]] = galleryImagesVar.signal

  private val brokenImageCountVar: Var[Int] = Var(0)
  val brokenImageCount: Signal[Int] = brokenImageCountVar.signal

  /** Refresh gallery images from IndexedDB */
  def refreshGallery(): Unit =
    ImageStore.listImages().foreach { images =>
      galleryImagesVar.set(images)
    }

  // ─── Session management ────────────────────────────────────────

  /** Initialize: load session list, gallery, and decide whether to show resume dialog */
  def initSessions(): Unit =
    EditorSessionStore.listSummaries().foreach { summaries =>
      sessionListVar.set(summaries)
      if summaries.nonEmpty then
        showResumeDialogVar.set(true)
    }
    refreshGallery()

  def dismissResumeDialog(): Unit =
    showResumeDialogVar.set(false)

  /** Start a brand new session (fresh editor state) */
  def startNewSession(): Unit = {
    val now = System.currentTimeMillis().toDouble
    val id = generateId("session")
    val meta = SessionMeta(id = id, name = "Untitled", createdAt = now)
    suppressAutoSave = true
    stateVar.set(CalendarState.empty)
    selectedElementVar.set(None)
    currentSessionVar.set(Some(meta))
    saveStatusVar.set(SaveStatus.Unsaved)
    suppressAutoSave = false
    showResumeDialogVar.set(false)
    // Trigger first save immediately
    saveCurrentSession()
  }

  /** Load an existing session from IndexedDB */
  def loadSession(id: String): Unit =
    EditorSessionStore.load(id).foreach {
      case Some(session) =>
        suppressAutoSave = true
        val calState = EditorSession.toCalendarState(session)
        stateVar.set(calState)
        selectedElementVar.set(None)
        currentSessionVar.set(Some(SessionMeta(
          id = session.id,
          name = session.name,
          createdAt = session.createdAt,
          linkedConfigurationId = session.linkedConfigurationId,
        )))
        saveStatusVar.set(SaveStatus.Saved(session.updatedAt))
        suppressAutoSave = false
        showResumeDialogVar.set(false)
      case None =>
        dom.console.warn(s"Session $id not found in IndexedDB")
    }

  /** Start a new session linked to a product configuration (Phase 3) */
  def startSessionForProduct(
    configId: String,
    productType: VisualProductType,
    format: ProductFormat,
    name: String,
  ): Unit = {
    val now = System.currentTimeMillis().toDouble
    val id = generateId("session")
    val meta = SessionMeta(id = id, name = name, createdAt = now, linkedConfigurationId = Some(configId))
    suppressAutoSave = true
    stateVar.set(CalendarState.create(productType, format))
    selectedElementVar.set(None)
    currentSessionVar.set(Some(meta))
    saveStatusVar.set(SaveStatus.Unsaved)
    suppressAutoSave = false
    showResumeDialogVar.set(false)
    saveCurrentSession()
  }

  /** Find an existing session linked to a configuration ID */
  def findSessionForConfig(configId: String): Option[SessionSummary] =
    sessionListVar.now().find(_.linkedConfigurationId.contains(configId))

  /** Delete a session from IndexedDB */
  def deleteSession(id: String): Unit =
    EditorSessionStore.delete(id).foreach { _ =>
      refreshSessionList()
      // If we just deleted the current session, clear it
      if currentSessionVar.now().exists(_.id == id) then
        currentSessionVar.set(None)
        saveStatusVar.set(SaveStatus.Unsaved)
    }

  /** Rename the current session */
  def renameSession(newName: String): Unit =
    currentSessionVar.now().foreach { meta =>
      currentSessionVar.set(Some(meta.copy(name = newName)))
      scheduleAutoSave()
    }

  /** Refresh the session list from IndexedDB */
  def refreshSessionList(): Unit =
    EditorSessionStore.listSummaries().foreach { summaries =>
      sessionListVar.set(summaries)
    }

  /** Schedule a debounced auto-save */
  def scheduleAutoSave(): Unit =
    if !suppressAutoSave then
      saveTimerHandle.foreach(js.timers.clearTimeout)
      saveTimerHandle = Some(js.timers.setTimeout(AutoSaveDelayMs.toDouble) {
        saveCurrentSession()
      })
      saveStatusVar.set(SaveStatus.Unsaved)

  /** Immediately save the current session to IndexedDB */
  def saveCurrentSession(): Unit =
    currentSessionVar.now().foreach { meta =>
      saveStatusVar.set(SaveStatus.Saving)
      val session = EditorSession.fromState(stateVar.now(), meta)
      EditorSessionStore.save(session).foreach { _ =>
        saveStatusVar.set(SaveStatus.Saved(session.updatedAt))
        refreshSessionList()
      }
    }

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

  def updateElementPositionX(elementId: String, newX: Double): Unit =
    updateElement(elementId, e => e.withPosition(Position(newX, e.position.y)))

  def updateElementPositionY(elementId: String, newY: Double): Unit =
    updateElement(elementId, e => e.withPosition(Position(e.position.x, newY)))

  def updateElementSize(elementId: String, newSize: Size): Unit =
    updateElement(elementId, _.withSize(newSize))

  def updateElementSizeWidth(elementId: String, newWidth: Double): Unit =
    updateElement(elementId, e => e.withSize(Size(newWidth, e.size.height)))

  def updateElementSizeHeight(elementId: String, newHeight: Double): Unit =
    updateElement(elementId, e => e.withSize(Size(e.size.width, newHeight)))

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

  def updatePhoto(photoId: String, updater: PhotoElement => PhotoElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map {
          case e: PhotoElement if e.id == photoId => updater(e)
          case other => other
        })
      )
    )

  def updatePhotoImageScale(photoId: String, scale: Double): Unit =
    updatePhoto(photoId, _.copy(imageScale = math.max(1.0, scale)))

  def updatePhotoImageOffset(photoId: String, offsetX: Double, offsetY: Double): Unit =
    updatePhoto(photoId, _.copy(imageOffsetX = offsetX, imageOffsetY = offsetY))

  def clearPhotoImage(photoId: String): Unit =
    updatePhoto(photoId, _.copy(imageData = "", imageScale = 1.0, imageOffsetX = 0.0, imageOffsetY = 0.0))

  def replacePhotoImage(photoId: String, imageData: String): Unit =
    updatePhoto(photoId, _.copy(imageData = imageData, imageScale = 1.0, imageOffsetX = 0.0, imageOffsetY = 0.0))

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

  def applyBackgroundToAllPages(): Unit =
    val currentBg = stateVar.now().currentPage.template.background
    stateVar.update(_.applyBackgroundToAll(currentBg))

  // ─── Template operations ─────────────────────────────────────────

  def setTemplateType(templateType: CalendarTemplateType): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(template = page.template.copy(templateType = templateType))
      )
    )

  def applyTemplateToAllPages(): Unit =
    val currentTt = stateVar.now().currentPage.template.templateType
    stateVar.update(_.applyTemplateTypeToAll(currentTt))

  // ─── Product type & format ───────────────────────────────────────

  def setProductType(productType: VisualProductType): Unit = {
    selectedElementVar.set(None)
    val newFormat = ProductFormat.defaultFor(productType)
    stateVar.set(CalendarState.create(productType, newFormat))
  }

  def setProductFormat(format: ProductFormat): Unit = {
    selectedElementVar.set(None)
    stateVar.update(_.copy(productFormat = format))
  }

  // ─── Reset & language ────────────────────────────────────────────

  def reset(): Unit = {
    stateVar.set(CalendarState.empty)
    selectedElementVar.set(None)
  }

  def updateLanguage(lang: String): Unit =
    stateVar.update(state => CalendarState.updateLanguage(state, lang))
}
