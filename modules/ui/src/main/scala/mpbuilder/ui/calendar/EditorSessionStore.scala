package mpbuilder.ui.calendar

import org.scalajs.dom
import scala.scalajs.js
import scala.util.Try

/**
 * Browser localStorage facade for persisting editor sessions.
 *
 * Storage keys:
 *   - editor-sessions-index  → JSON array of session metadata
 *   - editor-session-{id}    → JSON object with full session state
 *   - editor-pending-session → PB→editor handoff (product context)
 *   - editor-image-gallery   → cross-session image gallery metadata
 */
object EditorSessionStore:

  // ─── Keys ──────────────────────────────────────────────────────────
  private val IndexKey          = "editor-sessions-index"
  private val SessionKeyPrefix  = "editor-session-"
  private val PendingSessionKey = "editor-pending-session"
  private val GalleryKey        = "editor-image-gallery"

  // ─── Session CRUD ──────────────────────────────────────────────────

  /** Save a session (creates or updates) */
  def save(session: EditorSession): Unit =
    Try {
      val json = SessionCodec.encodeSession(session)
      dom.window.localStorage.setItem(SessionKeyPrefix + session.id, json)
      updateIndex(session)
    }.getOrElse(())

  /** Load a session by ID */
  def load(id: String): Option[EditorSession] =
    Try {
      Option(dom.window.localStorage.getItem(SessionKeyPrefix + id))
        .flatMap(json => SessionCodec.decodeSession(json))
    }.getOrElse(None)

  /** Delete a session by ID */
  def delete(id: String): Unit =
    Try {
      dom.window.localStorage.removeItem(SessionKeyPrefix + id)
      removeFromIndex(id)
    }.getOrElse(())

  /** List all session summaries, sorted by most recently updated */
  def listSummaries(): List[SessionSummary] =
    Try {
      Option(dom.window.localStorage.getItem(IndexKey))
        .flatMap(json => SessionCodec.decodeSummaries(json))
        .getOrElse(List.empty)
        .sortBy(-_.updatedAt)
    }.getOrElse(List.empty)

  /** Get the most recently updated session */
  def getLatest(): Option[EditorSession] =
    listSummaries().headOption.flatMap(s => load(s.id))

  // ─── Pending Session (PB → Editor Handoff) ────────────────────────

  /** Store a pending session request from the product builder */
  def setPendingSession(
    configurationId: String,
    productType: VisualProductType,
    format: ProductFormat,
    pageCount: Int,
    productDescription: String,
  ): Unit =
    Try {
      val d = js.Dynamic.literal(
        "configurationId"    -> configurationId,
        "productType"        -> SessionCodec.encodeProductType(productType),
        "formatId"           -> format.id,
        "formatNameEn"       -> format.nameEn,
        "formatNameCs"       -> format.nameCs,
        "formatWidthMm"      -> format.widthMm,
        "formatHeightMm"     -> format.heightMm,
        "pageCount"          -> pageCount,
        "productDescription" -> productDescription,
      )
      dom.window.localStorage.setItem(PendingSessionKey, js.JSON.stringify(d))
    }.getOrElse(())

  /** Read and clear the pending session request */
  def consumePendingSession(): Option[PendingEditorSession] =
    Try {
      val raw = Option(dom.window.localStorage.getItem(PendingSessionKey))
      raw.map { json =>
        dom.window.localStorage.removeItem(PendingSessionKey)
        val d = js.JSON.parse(json)
        PendingEditorSession(
          configurationId    = d.configurationId.asInstanceOf[String],
          productType        = SessionCodec.decodeProductType(d.productType.asInstanceOf[String]),
          format             = ProductFormat(
            id       = d.formatId.asInstanceOf[String],
            nameEn   = d.formatNameEn.asInstanceOf[String],
            nameCs   = d.formatNameCs.asInstanceOf[String],
            widthMm  = d.formatWidthMm.asInstanceOf[Double].toInt,
            heightMm = d.formatHeightMm.asInstanceOf[Double].toInt,
          ),
          pageCount          = d.pageCount.asInstanceOf[Double].toInt,
          productDescription = d.productDescription.asInstanceOf[String],
        )
      }
    }.getOrElse(None)

  /** Find a session linked to a specific configuration ID */
  def findByConfigurationId(configId: String): Option[EditorSession] =
    listSummaries()
      .find(_.linkedConfigurationId.contains(configId))
      .flatMap(s => load(s.id))

  // ─── Gallery ───────────────────────────────────────────────────────

  /** Save gallery images metadata */
  def saveGallery(images: List[GalleryImage]): Unit =
    Try {
      val arr = js.Array(images.map(SessionCodec.encodeGalleryImage)*)
      dom.window.localStorage.setItem(GalleryKey, js.JSON.stringify(arr))
    }.getOrElse(())

  /** Load gallery images metadata */
  def loadGallery(): List[GalleryImage] =
    Try {
      Option(dom.window.localStorage.getItem(GalleryKey))
        .flatMap(json => SessionCodec.decodeGallery(json))
        .getOrElse(List.empty)
    }.getOrElse(List.empty)

  // ─── Private helpers ───────────────────────────────────────────────

  private def updateIndex(session: EditorSession): Unit =
    val summaries = listSummaries().filterNot(_.id == session.id)
    val updated = EditorSession.toSummary(session) :: summaries
    val arr = js.Array(updated.map(SessionCodec.encodeSummary)*)
    dom.window.localStorage.setItem(IndexKey, js.JSON.stringify(arr))

  private def removeFromIndex(id: String): Unit =
    val summaries = listSummaries().filterNot(_.id == id)
    val arr = js.Array(summaries.map(SessionCodec.encodeSummary)*)
    dom.window.localStorage.setItem(IndexKey, js.JSON.stringify(arr))


/** Pending editor session info passed from the product builder */
case class PendingEditorSession(
  configurationId: String,
  productType: VisualProductType,
  format: ProductFormat,
  pageCount: Int,
  productDescription: String,
)


/**
 * JSON codec helpers for converting between Scala types and JS Dynamic
 * for localStorage persistence.
 */
private[calendar] object SessionCodec:

  /** Safely read an optional string from a JS dynamic value */
  private def optionalString(v: js.Dynamic): Option[String] =
    if v == null || js.isUndefined(v) then None else Some(v.asInstanceOf[String])

  // ─── Product Type ──────────────────────────────────────────────────

  def encodeProductType(pt: VisualProductType): String = pt match
    case VisualProductType.MonthlyCalendar  => "monthly"
    case VisualProductType.WeeklyCalendar   => "weekly"
    case VisualProductType.BiweeklyCalendar => "biweekly"
    case VisualProductType.PhotoBook        => "photobook"
    case VisualProductType.WallPicture      => "wallpicture"
    case VisualProductType.CustomProduct    => "custom"

  def decodeProductType(s: String): VisualProductType = s match
    case "weekly"      => VisualProductType.WeeklyCalendar
    case "biweekly"    => VisualProductType.BiweeklyCalendar
    case "photobook"   => VisualProductType.PhotoBook
    case "wallpicture" => VisualProductType.WallPicture
    case "custom"      => VisualProductType.CustomProduct
    case _             => VisualProductType.MonthlyCalendar

  // ─── Text Alignment ────────────────────────────────────────────────

  private def encodeTextAlignment(a: TextAlignment): String = a match
    case TextAlignment.Left   => "left"
    case TextAlignment.Center => "center"
    case TextAlignment.Right  => "right"

  private def decodeTextAlignment(s: String): TextAlignment = s match
    case "center" => TextAlignment.Center
    case "right"  => TextAlignment.Right
    case _        => TextAlignment.Left

  // ─── Shape Type ────────────────────────────────────────────────────

  private def encodeShapeType(st: ShapeType): String = st match
    case ShapeType.Line      => "line"
    case ShapeType.Rectangle => "rectangle"

  private def decodeShapeType(s: String): ShapeType = s match
    case "rectangle" => ShapeType.Rectangle
    case _           => ShapeType.Line

  // ─── Page Background ───────────────────────────────────────────────

  private def encodeBackground(bg: PageBackground): js.Dynamic = bg match
    case PageBackground.SolidColor(c) =>
      js.Dynamic.literal("type" -> "solid", "color" -> c)
    case PageBackground.BackgroundImage(d) =>
      js.Dynamic.literal("type" -> "image", "data" -> d)

  private def decodeBackground(d: js.Dynamic): PageBackground =
    if d.selectDynamic("type").asInstanceOf[String] == "image" then
      PageBackground.BackgroundImage(d.data.asInstanceOf[String])
    else
      PageBackground.SolidColor(d.color.asInstanceOf[String])

  // ─── Position & Size ───────────────────────────────────────────────

  private def encodePosition(p: Position): js.Dynamic =
    js.Dynamic.literal("x" -> p.x, "y" -> p.y)

  private def decodePosition(d: js.Dynamic): Position =
    Position(d.x.asInstanceOf[Double], d.y.asInstanceOf[Double])

  private def encodeSize(s: Size): js.Dynamic =
    js.Dynamic.literal("w" -> s.width, "h" -> s.height)

  private def decodeSize(d: js.Dynamic): Size =
    Size(d.w.asInstanceOf[Double], d.h.asInstanceOf[Double])

  // ─── Canvas Elements ───────────────────────────────────────────────

  private def encodeElement(e: CanvasElement): js.Dynamic = e match
    case p: PhotoElement =>
      js.Dynamic.literal(
        "type" -> "photo", "id" -> p.id, "imageData" -> p.imageData,
        "pos" -> encodePosition(p.position), "size" -> encodeSize(p.size),
        "rot" -> p.rotation, "z" -> p.zIndex,
        "scale" -> p.imageScale, "offX" -> p.imageOffsetX, "offY" -> p.imageOffsetY,
      )
    case t: TextElement =>
      js.Dynamic.literal(
        "type" -> "text", "id" -> t.id, "text" -> t.text,
        "pos" -> encodePosition(t.position), "size" -> encodeSize(t.size),
        "rot" -> t.rotation, "z" -> t.zIndex,
        "fontSize" -> t.fontSize, "fontFamily" -> t.fontFamily,
        "color" -> t.color, "bold" -> t.bold, "italic" -> t.italic,
        "align" -> encodeTextAlignment(t.textAlign),
      )
    case s: ShapeElement =>
      js.Dynamic.literal(
        "type" -> "shape", "id" -> s.id, "shapeType" -> encodeShapeType(s.shapeType),
        "pos" -> encodePosition(s.position), "size" -> encodeSize(s.size),
        "rot" -> s.rotation, "z" -> s.zIndex,
        "stroke" -> s.strokeColor, "fill" -> s.fillColor, "strokeW" -> s.strokeWidth,
      )
    case c: ClipartElement =>
      js.Dynamic.literal(
        "type" -> "clipart", "id" -> c.id, "imageData" -> c.imageData,
        "pos" -> encodePosition(c.position), "size" -> encodeSize(c.size),
        "rot" -> c.rotation, "z" -> c.zIndex,
      )

  private def decodeElement(d: js.Dynamic): Option[CanvasElement] =
    val elemType = d.selectDynamic("type").asInstanceOf[String]
    val id   = d.id.asInstanceOf[String]
    val pos  = decodePosition(d.pos)
    val size = decodeSize(d.size)
    val rot  = d.rot.asInstanceOf[Double]
    val z    = d.z.asInstanceOf[Double].toInt
    elemType match
      case "photo" =>
        Some(PhotoElement(id, d.imageData.asInstanceOf[String], pos, size, rot, z,
          d.scale.asInstanceOf[Double], d.offX.asInstanceOf[Double], d.offY.asInstanceOf[Double]))
      case "text" =>
        Some(TextElement(id, d.text.asInstanceOf[String], pos, size, rot, z,
          d.fontSize.asInstanceOf[Double].toInt, d.fontFamily.asInstanceOf[String],
          d.color.asInstanceOf[String], d.bold.asInstanceOf[Boolean], d.italic.asInstanceOf[Boolean],
          decodeTextAlignment(d.align.asInstanceOf[String])))
      case "shape" =>
        Some(ShapeElement(id, decodeShapeType(d.shapeType.asInstanceOf[String]), pos, size, rot, z,
          d.stroke.asInstanceOf[String], d.fill.asInstanceOf[String], d.strokeW.asInstanceOf[Double]))
      case "clipart" =>
        Some(ClipartElement(id, d.imageData.asInstanceOf[String], pos, size, rot, z))
      case _ => None

  // ─── Template Text Field ───────────────────────────────────────────

  private def encodeTemplateField(f: TemplateTextField): js.Dynamic =
    js.Dynamic.literal(
      "id" -> f.id, "text" -> f.text, "pos" -> encodePosition(f.position),
      "fontSize" -> f.fontSize, "fontFamily" -> f.fontFamily, "color" -> f.color,
    )

  private def decodeTemplateField(d: js.Dynamic): TemplateTextField =
    TemplateTextField(
      d.id.asInstanceOf[String], d.text.asInstanceOf[String], decodePosition(d.pos),
      d.fontSize.asInstanceOf[Double].toInt, d.fontFamily.asInstanceOf[String], d.color.asInstanceOf[String],
    )

  // ─── Calendar Template ─────────────────────────────────────────────

  private def encodeTemplate(t: CalendarTemplate): js.Dynamic =
    js.Dynamic.literal(
      "bg" -> encodeBackground(t.background),
      "monthField" -> encodeTemplateField(t.monthField),
      "daysGrid" -> js.Array(t.daysGrid.map(encodeTemplateField)*)
    )

  private def decodeTemplate(d: js.Dynamic): CalendarTemplate =
    val grid = d.daysGrid.asInstanceOf[js.Array[js.Dynamic]].toList.map(decodeTemplateField)
    CalendarTemplate(
      background = decodeBackground(d.bg),
      monthField = decodeTemplateField(d.monthField),
      daysGrid = grid,
    )

  // ─── Calendar Page ─────────────────────────────────────────────────

  private def encodePage(p: CalendarPage): js.Dynamic =
    js.Dynamic.literal(
      "num" -> p.pageNumber,
      "template" -> encodeTemplate(p.template),
      "elements" -> js.Array(p.elements.map(encodeElement)*)
    )

  private def decodePage(d: js.Dynamic): CalendarPage =
    CalendarPage(
      pageNumber = d.num.asInstanceOf[Double].toInt,
      template = decodeTemplate(d.template),
      elements = d.elements.asInstanceOf[js.Array[js.Dynamic]].toList.flatMap(decodeElement),
    )

  // ─── Product Format ────────────────────────────────────────────────

  private def encodeFormat(f: ProductFormat): js.Dynamic =
    js.Dynamic.literal(
      "id" -> f.id, "nameEn" -> f.nameEn, "nameCs" -> f.nameCs,
      "w" -> f.widthMm, "h" -> f.heightMm,
    )

  private def decodeFormat(d: js.Dynamic): ProductFormat =
    ProductFormat(
      d.id.asInstanceOf[String], d.nameEn.asInstanceOf[String], d.nameCs.asInstanceOf[String],
      d.w.asInstanceOf[Double].toInt, d.h.asInstanceOf[Double].toInt,
    )

  // ─── EditorSession (full) ──────────────────────────────────────────

  def encodeSession(s: EditorSession): String =
    val d = js.Dynamic.literal(
      "id"           -> s.id,
      "name"         -> s.name,
      "productType"  -> encodeProductType(s.productType),
      "format"       -> encodeFormat(s.productFormat),
      "pages"        -> js.Array(s.pages.map(encodePage)*),
      "imageRefs"    -> js.Array(s.imageReferences.toSeq*),
      "linkedConfig" -> s.linkedConfigurationId.orNull,
      "createdAt"    -> s.createdAt,
      "updatedAt"    -> s.updatedAt,
    )
    js.JSON.stringify(d)

  def decodeSession(json: String): Option[EditorSession] =
    scala.util.Try {
      val d = js.JSON.parse(json)
      EditorSession(
        id              = d.id.asInstanceOf[String],
        name            = d.name.asInstanceOf[String],
        productType     = decodeProductType(d.productType.asInstanceOf[String]),
        productFormat   = decodeFormat(d.format),
        pages           = d.pages.asInstanceOf[js.Array[js.Dynamic]].toList.map(decodePage),
        imageReferences = d.imageRefs.asInstanceOf[js.Array[String]].toSet,
        linkedConfigurationId = optionalString(d.linkedConfig),
        createdAt       = d.createdAt.asInstanceOf[Double],
        updatedAt       = d.updatedAt.asInstanceOf[Double],
      )
    }.toOption

  // ─── SessionSummary ────────────────────────────────────────────────

  def encodeSummary(s: SessionSummary): js.Dynamic =
    js.Dynamic.literal(
      "id"           -> s.id,
      "name"         -> s.name,
      "productType"  -> encodeProductType(s.productType),
      "format"       -> encodeFormat(s.productFormat),
      "pageCount"    -> s.pageCount,
      "elementCount" -> s.elementCount,
      "linkedConfig" -> s.linkedConfigurationId.orNull,
      "createdAt"    -> s.createdAt,
      "updatedAt"    -> s.updatedAt,
    )

  def decodeSummaries(json: String): Option[List[SessionSummary]] =
    scala.util.Try {
      val arr = js.JSON.parse(json).asInstanceOf[js.Array[js.Dynamic]]
      arr.toList.map { d =>
        SessionSummary(
          id              = d.id.asInstanceOf[String],
          name            = d.name.asInstanceOf[String],
          productType     = decodeProductType(d.productType.asInstanceOf[String]),
          productFormat   = decodeFormat(d.format),
          pageCount       = d.pageCount.asInstanceOf[Double].toInt,
          elementCount    = d.elementCount.asInstanceOf[Double].toInt,
          linkedConfigurationId = optionalString(d.linkedConfig),
          createdAt       = d.createdAt.asInstanceOf[Double],
          updatedAt       = d.updatedAt.asInstanceOf[Double],
        )
      }
    }.toOption

  // ─── Gallery Image ─────────────────────────────────────────────────

  def encodeGalleryImage(g: GalleryImage): js.Dynamic =
    js.Dynamic.literal(
      "id" -> g.id, "name" -> g.name, "thumb" -> g.thumbnailDataUrl,
      "w" -> g.width, "h" -> g.height, "addedAt" -> g.addedAt, "size" -> g.sizeBytes.toDouble,
    )

  def decodeGallery(json: String): Option[List[GalleryImage]] =
    scala.util.Try {
      val arr = js.JSON.parse(json).asInstanceOf[js.Array[js.Dynamic]]
      arr.toList.map { d =>
        GalleryImage(
          d.id.asInstanceOf[String], d.name.asInstanceOf[String], d.thumb.asInstanceOf[String],
          d.w.asInstanceOf[Double].toInt, d.h.asInstanceOf[Double].toInt,
          d.addedAt.asInstanceOf[Double], d.size.asInstanceOf[Double].toLong,
        )
      }
    }.toOption
