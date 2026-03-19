package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** View model for managing calendar state */
object CalendarViewModel {

  // Main state
  private val stateVar: Var[CalendarState] = Var(CalendarState.empty)
  val state: Signal[CalendarState] = stateVar.signal

  // Active session tracking
  private val currentSessionIdVar: Var[Option[String]] = Var(None)
  val currentSessionId: Signal[Option[String]] = currentSessionIdVar.signal

  private val sessionTitleVar: Var[String] = Var("Untitled")
  val sessionTitle: Signal[String] = sessionTitleVar.signal

  // Session list (refreshed from localStorage)
  val sessionListVar: Var[List[SessionMeta]] = Var(EditorSessionStore.listSessions())

  // Image gallery (refreshed from localStorage)
  val galleryImagesVar: Var[List[String]] = Var(EditorSessionStore.getGalleryImages)

  // Resume dialog visibility
  val showResumeDialogVar: Var[Boolean] = Var(false)
  val resumeSessionIdVar: Var[Option[String]] = Var(None)

  // Auto-save debounce timer handle
  private var autoSaveTimerHandle: Int = 0

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
    currentSessionIdVar.set(None)
    sessionTitleVar.set("Untitled")
  }

  def updateLanguage(lang: String): Unit =
    stateVar.update(state => CalendarState.updateLanguage(state, lang))

  // ─── Session management ──────────────────────────────────────────

  /** Initialize editor from product builder configuration.
    * Called by ProductBuilderViewModel.openInEditor().
    */
  def initFromProductConfig(widthMm: Int, heightMm: Int, pages: Int, sessionId: String): Unit = {
    val newState = CalendarState.createCustom(pages, widthMm, heightMm)
    stateVar.set(newState)
    selectedElementVar.set(None)
    currentSessionIdVar.set(Some(sessionId))
    sessionTitleVar.set(s"Custom ${widthMm}×${heightMm}mm")
  }

  /** Trigger auto-save with a 2-second debounce. */
  def scheduleAutoSave(): Unit = {
    if autoSaveTimerHandle != 0 then
      dom.window.clearTimeout(autoSaveTimerHandle)
    autoSaveTimerHandle = dom.window.setTimeout(
      () => performAutoSave(),
      2000,
    )
  }

  /** Perform the actual save to localStorage. */
  private def performAutoSave(): Unit = {
    autoSaveTimerHandle = 0
    val sid = currentSessionIdVar.now().getOrElse {
      val newId = s"session-${System.currentTimeMillis()}-${scala.util.Random.nextInt(10000)}"
      currentSessionIdVar.set(Some(newId))
      newId
    }
    val title = sessionTitleVar.now()
    EditorSessionStore.saveSession(sid, title, stateVar.now())
    refreshSessionList()
    refreshGalleryImages()
  }

  /** Load a session from localStorage. */
  def loadSession(sessionId: String): Unit = {
    EditorSessionStore.loadSession(sessionId) match
      case Some(loadedState) =>
        stateVar.set(loadedState)
        selectedElementVar.set(None)
        currentSessionIdVar.set(Some(sessionId))
        val meta = EditorSessionStore.listSessions().find(_.id == sessionId)
        sessionTitleVar.set(meta.map(_.title).getOrElse("Untitled"))
      case None => ()
  }

  /** Delete a session from localStorage. */
  def deleteSession(sessionId: String): Unit = {
    EditorSessionStore.deleteSession(sessionId)
    // If we deleted the current session, clear session tracking
    if currentSessionIdVar.now().contains(sessionId) then
      currentSessionIdVar.set(None)
    refreshSessionList()
  }

  /** Update session title. */
  def updateSessionTitle(newTitle: String): Unit = {
    sessionTitleVar.set(newTitle)
    currentSessionIdVar.now().foreach { sid =>
      EditorSessionStore.updateSessionTitle(sid, newTitle)
      refreshSessionList()
    }
  }

  def refreshSessionList(): Unit =
    sessionListVar.set(EditorSessionStore.listSessions())

  def refreshGalleryImages(): Unit =
    galleryImagesVar.set(EditorSessionStore.getGalleryImages)

  /** Check for pending/recent sessions when the editor mounts.
    * Shows the resume dialog if applicable.
    */
  def checkForResumableSession(): Unit = {
    val pendingId = EditorSessionStore.getPendingSessionId
    pendingId match
      case Some(pid) =>
        // Coming from product builder — already initialized, just mark as active
        EditorSessionStore.clearPendingSessionId()
        currentSessionIdVar.set(Some(pid))
      case None =>
        // Opening editor directly — check for recent work
        EditorSessionStore.mostRecentSession match
          case Some(recent) =>
            resumeSessionIdVar.set(Some(recent.id))
            showResumeDialogVar.set(true)
          case None =>
            () // No saved sessions, start fresh
  }

  /** User chose to resume the offered session. */
  def resumeSession(): Unit = {
    resumeSessionIdVar.now().foreach(loadSession)
    showResumeDialogVar.set(false)
    resumeSessionIdVar.set(None)
  }

  /** User chose to start fresh (decline resume). */
  def startFresh(): Unit = {
    showResumeDialogVar.set(false)
    resumeSessionIdVar.set(None)
  }

  /** Remove an image from the gallery. */
  def removeGalleryImage(imageData: String): Unit = {
    EditorSessionStore.removeGalleryImage(imageData)
    refreshGalleryImages()
  }

  /** Insert a gallery image into the current page as a new photo element. */
  def insertGalleryImage(imageData: String): Unit = {
    uploadPhoto(imageData)
  }

  /** Laminar modifier that triggers auto-save on every state change. */
  def autoSaveBinder: Binder[Element] =
    state.changes --> { _ => scheduleAutoSave() }
}
