package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js

/**
 * Scala.js-safe UUID-like identifier generation using js.Math.random.
 * Not cryptographically secure — suitable for client-side session and element IDs only.
 */
private[ui] object IdGen:
  def uuid(): String =
    def hex4(): String = ((1 + js.Math.random()) * 0x10000).toInt.toHexString.substring(1)
    s"${hex4()}${hex4()}-${hex4()}-${hex4()}-${hex4()}-${hex4()}${hex4()}${hex4()}"

/** View model for managing calendar state */
object CalendarViewModel {

  // Main state
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

  // ─── Session persistence state ───────────────────────────────────
  private val currentSessionIdVar: Var[Option[String]] = Var(None)
  val currentSessionId: Signal[Option[String]] = currentSessionIdVar.signal

  private val currentSessionNameVar: Var[String] = Var("Untitled")
  val currentSessionName: Signal[String] = currentSessionNameVar.signal

  private val linkedConfigIdVar: Var[Option[String]] = Var(None)
  val linkedConfigId: Signal[Option[String]] = linkedConfigIdVar.signal

  private val linkedProductDescriptionVar: Var[Option[String]] = Var(None)
  val linkedProductDescription: Signal[Option[String]] = linkedProductDescriptionVar.signal

  private val saveStatusVar: Var[String] = Var("")
  val saveStatus: Signal[String] = saveStatusVar.signal

  // Gallery images
  private val galleryImagesVar: Var[List[GalleryImage]] = Var(EditorSessionStore.loadGallery())
  val galleryImages: Signal[List[GalleryImage]] = galleryImagesVar.signal

  // Session list (for resume dialog and session panel)
  private val sessionListVar: Var[List[SessionSummary]] = Var(EditorSessionStore.listSummaries())
  val sessionList: Signal[List[SessionSummary]] = sessionListVar.signal

  // Auto-save debounce timer
  private var autoSaveTimer: Option[Int] = None
  private val AutoSaveDelayMs = 3000

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
  def goToNextPage(): Unit = { stateVar.update(_.goToNext); scheduleAutoSave() }
  def goToPreviousPage(): Unit = { stateVar.update(_.goToPrevious); scheduleAutoSave() }
  def goToPage(index: Int): Unit = { stateVar.update(_.goToPage(index)); scheduleAutoSave() }

  // ─── Session persistence ─────────────────────────────────────────

  /** Schedule a debounced auto-save */
  private def scheduleAutoSave(): Unit =
    autoSaveTimer.foreach(id => dom.window.clearTimeout(id))
    saveStatusVar.set("Saving...")
    autoSaveTimer = Some(dom.window.setTimeout(() => {
      saveCurrentSession()
    }, AutoSaveDelayMs))

  /** Save the current session to localStorage immediately */
  def saveCurrentSession(): Unit =
    val sessionId = currentSessionIdVar.now().getOrElse {
      val newId = IdGen.uuid()
      currentSessionIdVar.set(Some(newId))
      newId
    }
    val session = EditorSession.fromState(
      id = sessionId,
      name = currentSessionNameVar.now(),
      state = stateVar.now(),
      linkedConfigurationId = linkedConfigIdVar.now(),
      createdAt = EditorSessionStore.load(sessionId).map(_.createdAt)
        .getOrElse(System.currentTimeMillis().toDouble),
    )
    EditorSessionStore.save(session)
    sessionListVar.set(EditorSessionStore.listSummaries())
    val now = new js.Date()
    val timeStr = f"${now.getHours().toInt}%02d:${now.getMinutes().toInt}%02d"
    saveStatusVar.set(s"Saved · $timeStr")

  /** Load an existing session */
  def loadSession(sessionId: String): Unit =
    EditorSessionStore.load(sessionId).foreach { session =>
      currentSessionIdVar.set(Some(session.id))
      currentSessionNameVar.set(session.name)
      linkedConfigIdVar.set(session.linkedConfigurationId)
      stateVar.set(EditorSession.toState(session))
      selectedElementVar.set(None)
      saveStatusVar.set("Loaded")
    }

  /** Start a new empty session (discarding current state) */
  def newSession(): Unit =
    val newId = IdGen.uuid()
    currentSessionIdVar.set(Some(newId))
    currentSessionNameVar.set("Untitled")
    linkedConfigIdVar.set(None)
    linkedProductDescriptionVar.set(None)
    stateVar.set(CalendarState.empty)
    selectedElementVar.set(None)
    saveStatusVar.set("")

  /** Rename the current session */
  def renameSession(newName: String): Unit =
    currentSessionNameVar.set(newName)
    scheduleAutoSave()

  /** Delete a session by ID */
  def deleteSession(sessionId: String): Unit =
    EditorSessionStore.delete(sessionId)
    sessionListVar.set(EditorSessionStore.listSummaries())
    // If we deleted the current session, start fresh
    if currentSessionIdVar.now().contains(sessionId) then
      newSession()

  /** Initialize from a product builder configuration (Phase 3) */
  def initFromProductConfig(pending: PendingEditorSession): Unit =
    // Check if a session already exists for this configuration
    EditorSessionStore.findByConfigurationId(pending.configurationId) match
      case Some(existing) =>
        loadSession(existing.id)
      case None =>
        val newId = IdGen.uuid()
        currentSessionIdVar.set(Some(newId))
        currentSessionNameVar.set(pending.productDescription)
        linkedConfigIdVar.set(Some(pending.configurationId))
        linkedProductDescriptionVar.set(Some(pending.productDescription))
        val newState = CalendarState.create(pending.productType, pending.format)
        stateVar.set(newState)
        selectedElementVar.set(None)
        saveCurrentSession()

  /** Check for and consume a pending session from the product builder */
  def checkPendingSession(): Option[PendingEditorSession] =
    EditorSessionStore.consumePendingSession()

  /** Export current session as a JSON string */
  def exportSession(): Option[String] =
    currentSessionIdVar.now().flatMap(EditorSessionStore.load).map(SessionCodec.encodeSession)

  /** Import a session from a JSON string */
  def importSession(json: String): Boolean =
    SessionCodec.decodeSession(json) match
      case Some(session) =>
        // Assign a new ID to avoid conflicts
        val newSession = session.copy(
          id = IdGen.uuid(),
          updatedAt = System.currentTimeMillis().toDouble,
        )
        EditorSessionStore.save(newSession)
        loadSession(newSession.id)
        true
      case None => false

  // ─── Gallery operations ──────────────────────────────────────────

  /** Add an image to the gallery from a base64 data URL */
  def addToGallery(name: String, thumbnailDataUrl: String, width: Int, height: Int, sizeBytes: Long): Unit =
    val image = GalleryImage(
      id = IdGen.uuid(),
      name = name,
      thumbnailDataUrl = thumbnailDataUrl,
      width = width,
      height = height,
      addedAt = System.currentTimeMillis().toDouble,
      sizeBytes = sizeBytes,
    )
    val updated = image :: galleryImagesVar.now()
    galleryImagesVar.set(updated)
    EditorSessionStore.saveGallery(updated)

  /** Remove an image from the gallery */
  def removeFromGallery(imageId: String): Unit =
    val updated = galleryImagesVar.now().filterNot(_.id == imageId)
    galleryImagesVar.set(updated)
    EditorSessionStore.saveGallery(updated)

  // ─── Generic element CRUD ────────────────────────────────────────

  private def addElement(element: CanvasElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page => page.copy(elements = page.elements :+ element))
    )
    scheduleAutoSave()

  def removeElement(elementId: String): Unit = {
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.filterNot(_.id == elementId))
      )
    )
    if selectedElementVar.now().contains(elementId) then
      selectedElementVar.set(None)
    scheduleAutoSave()
  }

  private def updateElement(elementId: String, updater: CanvasElement => CanvasElement): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map { e =>
          if e.id == elementId then updater(e) else e
        })
      )
    )
    scheduleAutoSave()

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
    scheduleAutoSave()

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
    scheduleAutoSave()

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
    scheduleAutoSave()

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
    scheduleAutoSave()

  def setBackgroundImage(imageData: String): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(template = page.template.copy(background = PageBackground.BackgroundImage(imageData)))
      )
    )
    scheduleAutoSave()

  def applyBackgroundToAllPages(): Unit =
    val currentBg = stateVar.now().currentPage.template.background
    stateVar.update(_.applyBackgroundToAll(currentBg))
    scheduleAutoSave()

  // ─── Template operations ─────────────────────────────────────────

  def setTemplateType(templateType: CalendarTemplateType): Unit =
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(template = page.template.copy(templateType = templateType))
      )
    )
    scheduleAutoSave()

  def applyTemplateToAllPages(): Unit =
    val currentTt = stateVar.now().currentPage.template.templateType
    stateVar.update(_.applyTemplateTypeToAll(currentTt))
    scheduleAutoSave()

  // ─── Product type & format ───────────────────────────────────────

  def setProductType(productType: VisualProductType): Unit = {
    selectedElementVar.set(None)
    val newFormat = ProductFormat.defaultFor(productType)
    stateVar.set(CalendarState.create(productType, newFormat))
    scheduleAutoSave()
  }

  def setProductFormat(format: ProductFormat): Unit = {
    selectedElementVar.set(None)
    stateVar.update(_.copy(productFormat = format))
    scheduleAutoSave()
  }

  // ─── Reset & language ────────────────────────────────────────────

  def reset(): Unit = {
    stateVar.set(CalendarState.empty)
    selectedElementVar.set(None)
    newSession()
  }

  def updateLanguage(lang: String): Unit =
    stateVar.update(state => CalendarState.updateLanguage(state, lang))
}
