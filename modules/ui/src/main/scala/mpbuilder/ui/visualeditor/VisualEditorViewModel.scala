package mpbuilder.ui.visualeditor

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js
import mpbuilder.ui.persistence.EditorSessionStore
import mpbuilder.ui.visualeditor.components.ImageGalleryPanel

/** View model for managing visual editor state */
object VisualEditorViewModel {

  // Main state
  private val stateVar: Var[EditorState] = Var(EditorState.empty)
  val state: Signal[EditorState] = stateVar.signal

  // Current session tracking for auto-save
  private val currentSessionIdVar: Var[Option[String]] = Var(None)
  val currentSessionId: Signal[Option[String]] = currentSessionIdVar.signal

  /** One-time snapshot of the current session ID (for non-reactive lookups) */
  def currentSessionIdSnapshot(): Option[String] = currentSessionIdVar.now()

  // Product context for the current session
  private val productContextVar: Var[Option[ProductContext]] = Var(None)
  val productContext: Signal[Option[ProductContext]] = productContextVar.signal

  // Save status
  private val lastSavedVar: Var[Option[Double]] = Var(None)
  val lastSaved: Signal[Option[Double]] = lastSavedVar.signal
  private val isSavingVar: Var[Boolean] = Var(false)
  val isSaving: Signal[Boolean] = isSavingVar.signal

  // Session name (user-editable)
  private val sessionNameVar: Var[Option[String]] = Var(None)
  val sessionName: Signal[Option[String]] = sessionNameVar.signal

  // Auto-save timer
  private var autoSaveTimer: Option[Int] = None
  private val AUTO_SAVE_DELAY_MS = 2000

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
  def currentPage: Signal[EditorPage] = state.map(_.currentPage)

  def currentPageIndex: Signal[Int] = state.map(_.currentPageIndex)

  /** Snapshot of the current page (for one-time lookups, not reactive) */
  def currentPageSnapshot(): EditorPage = stateVar.now().currentPage

  // ─── Auto-save ─────────────────────────────────────────────────

  private def scheduleAutoSave(): Unit =
    autoSaveTimer.foreach(dom.window.clearTimeout(_))
    autoSaveTimer = Some(dom.window.setTimeout(
      () => performAutoSave(),
      AUTO_SAVE_DELAY_MS,
    ))

  private def performAutoSave(): Unit =
    currentSessionIdVar.now().foreach { sessionId =>
      isSavingVar.set(true)
      val session = EditorSession(
        id = sessionId,
        title = inferSessionTitle(),
        sessionName = sessionNameVar.now().map(_.trim).filter(_.nonEmpty),
        configurationId = None,
        productContext = productContextVar.now(),
        editorState = stateVar.now(),
        createdAt = lastSavedVar.now().getOrElse(System.currentTimeMillis().toDouble),
        updatedAt = System.currentTimeMillis().toDouble,
        thumbnailDataUrl = None,
      )
      EditorSessionStore.save(session, () => {
        isSavingVar.set(false)
        lastSavedVar.set(Some(System.currentTimeMillis().toDouble))
      })
    }

  private def inferSessionTitle(): String =
    val st = stateVar.now()
    val typeName = st.productType match
      case VisualProductType.MonthlyCalendar  => "Monthly Calendar"
      case VisualProductType.WeeklyCalendar   => "Weekly Calendar"
      case VisualProductType.BiweeklyCalendar => "Bi-weekly Calendar"
      case VisualProductType.PhotoBook        => "Photo Book"
      case VisualProductType.WallPicture      => "Wall Picture"
      case VisualProductType.GenericProduct   => "Custom Product"
    val fmt = st.productFormat
    s"$typeName ${fmt.widthMm}x${fmt.heightMm}mm"

  // ─── Session management ────────────────────────────────────────

  /** Initialize from product builder context */
  def initializeFromProduct(ctx: ProductContext): Unit =
    val pt = ctx.visualProductType.getOrElse(VisualProductType.GenericProduct)
    val fmt = ProductFormat(
      id = "product-custom",
      nameEn = s"${ctx.widthMm.toInt}x${ctx.heightMm.toInt} mm",
      nameCs = s"${ctx.widthMm.toInt}x${ctx.heightMm.toInt} mm",
      widthMm = ctx.widthMm.toInt,
      heightMm = ctx.heightMm.toInt,
    )
    val pageCount = ctx.pageCount.getOrElse(EditorState.defaultPageCount(pt))
    productContextVar.set(Some(ctx))
    selectedElementVar.set(None)
    if pt == VisualProductType.GenericProduct then
      stateVar.set(EditorState.createGeneric(fmt, pageCount))
    else
      stateVar.set(EditorState.create(pt, fmt))

  /** Start a new empty session and assign an ID for auto-save */
  def startNewSession(sessionId: String): Unit =
    currentSessionIdVar.set(Some(sessionId))
    productContextVar.set(None)

  /** Load a saved session from IndexedDB */
  def loadSession(session: EditorSession): Unit =
    currentSessionIdVar.set(Some(session.id))
    productContextVar.set(session.productContext)
    sessionNameVar.set(session.sessionName)
    selectedElementVar.set(None)
    stateVar.set(session.editorState)
    lastSavedVar.set(Some(session.updatedAt))

  /** Set the session ID (when opening from product builder) */
  def setSessionId(sessionId: String): Unit =
    currentSessionIdVar.set(Some(sessionId))

  // Navigation
  def goToNextPage(): Unit = { stateVar.update(_.goToNext); scheduleAutoSave() }
  def goToPreviousPage(): Unit = { stateVar.update(_.goToPrevious); scheduleAutoSave() }
  def goToPage(index: Int): Unit = { stateVar.update(_.goToPage(index)); scheduleAutoSave() }

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
    // Position updates during drag — use direct update without auto-save to avoid flooding
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map { e =>
          if e.id == elementId then e.withPosition(newPosition) else e
        })
      )
    )

  /** Schedule auto-save after drag ends */
  def commitElementChange(): Unit = scheduleAutoSave()

  def updateElementPositionX(elementId: String, newX: Double): Unit =
    updateElement(elementId, e => e.withPosition(Position(newX, e.position.y)))

  def updateElementPositionY(elementId: String, newY: Double): Unit =
    updateElement(elementId, e => e.withPosition(Position(e.position.x, newY)))

  def updateElementSize(elementId: String, newSize: Size): Unit =
    // Size updates during resize — direct update without auto-save
    stateVar.update(s =>
      s.updateCurrentPage(page =>
        page.copy(elements = page.elements.map { e =>
          if e.id == elementId then e.withSize(newSize) else e
        })
      )
    )

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
    // Auto-sync to gallery
    syncImageToGallery(imageData)
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

  def setTemplateType(templateType: PageTemplateType): Unit =
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
    stateVar.set(EditorState.create(productType, newFormat))
    scheduleAutoSave()
  }

  def setProductFormat(format: ProductFormat): Unit = {
    selectedElementVar.set(None)
    stateVar.update(_.copy(productFormat = format))
    scheduleAutoSave()
  }

  // ─── Reset & language ────────────────────────────────────────────

  def reset(): Unit = {
    stateVar.set(EditorState.empty)
    selectedElementVar.set(None)
    currentSessionIdVar.set(None)
    productContextVar.set(None)
    sessionNameVar.set(None)
    lastSavedVar.set(None)
  }

  def setSessionName(name: String): Unit =
    sessionNameVar.set(if name.isEmpty then None else Some(name))
    scheduleAutoSave()

  def updateLanguage(lang: String): Unit =
    stateVar.update(state => EditorState.updateLanguage(state, lang))
    scheduleAutoSave()
}
