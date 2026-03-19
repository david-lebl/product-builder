package mpbuilder.ui.calendar

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.JSON

/** Metadata for a saved editor session, stored in the session index. */
case class SessionMeta(
  id: String,
  title: String,
  lastUpdated: Double,
  productType: String,
  formatId: String,
  pageCount: Int,
  sourceConfigId: Option[String] = None,
)

/** Persistence layer for visual editor sessions using browser localStorage.
  *
  * Keys:
  *   - `editor-sessions-index` — JSON array of [[SessionMeta]]
  *   - `editor-session-{id}`   — JSON blob of full [[CalendarState]] (simplified)
  *   - `editor-pending-session` — session ID passed from the product builder
  *   - `editor-image-gallery`  — JSON array of image data URLs collected across sessions
  */
object EditorSessionStore {

  private val IndexKey         = "editor-sessions-index"
  private val SessionPrefix    = "editor-session-"
  private val PendingKey       = "editor-pending-session"
  private val ImageGalleryKey  = "editor-image-gallery"

  // ── Pending session (product builder → editor hand-off) ──────────

  def setPendingSessionId(id: Option[String]): Unit =
    try id match
      case Some(sid) => dom.window.localStorage.setItem(PendingKey, sid)
      case None      => dom.window.localStorage.removeItem(PendingKey)
    catch case _: Exception => ()

  def getPendingSessionId: Option[String] =
    try Option(dom.window.localStorage.getItem(PendingKey)).filter(_.nonEmpty)
    catch case _: Exception => None

  def clearPendingSessionId(): Unit =
    try dom.window.localStorage.removeItem(PendingKey)
    catch case _: Exception => ()

  // ── Session index ────────────────────────────────────────────────

  def listSessions(): List[SessionMeta] =
    try
      Option(dom.window.localStorage.getItem(IndexKey)) match
        case Some(jsonStr) if jsonStr.nonEmpty =>
          val arr = JSON.parse(jsonStr).asInstanceOf[js.Array[js.Dynamic]]
          arr.toList.map { d =>
            SessionMeta(
              id = d.id.asInstanceOf[String],
              title = d.title.asInstanceOf[String],
              lastUpdated = d.lastUpdated.asInstanceOf[Double],
              productType = d.productType.asInstanceOf[String],
              formatId = d.formatId.asInstanceOf[String],
              pageCount = d.pageCount.asInstanceOf[Double].toInt,
              sourceConfigId =
                if js.isUndefined(d.sourceConfigId) || d.sourceConfigId == null then None
                else Some(d.sourceConfigId.asInstanceOf[String]),
            )
          }
        case _ => List.empty
    catch case _: Exception => List.empty

  private def saveIndex(sessions: List[SessionMeta]): Unit =
    try
      val arr = js.Array[js.Dynamic]()
      sessions.foreach { s =>
        val obj = js.Dynamic.literal(
          id = s.id,
          title = s.title,
          lastUpdated = s.lastUpdated,
          productType = s.productType,
          formatId = s.formatId,
          pageCount = s.pageCount,
        )
        s.sourceConfigId.foreach(cid => obj.updateDynamic("sourceConfigId")(cid))
        arr.push(obj)
      }
      dom.window.localStorage.setItem(IndexKey, JSON.stringify(arr))
    catch case _: Exception => ()

  // ── Session data (save / load / delete) ──────────────────────────

  /** Save or update a session. The CalendarState is serialised to a simplified JSON. */
  def saveSession(
    sessionId: String,
    title: String,
    state: CalendarState,
    sourceConfigId: Option[String] = None,
  ): Unit =
    val now = System.currentTimeMillis().toDouble
    val meta = SessionMeta(
      id = sessionId,
      title = title,
      lastUpdated = now,
      productType = state.productType.toString,
      formatId = state.productFormat.id,
      pageCount = state.pages.size,
      sourceConfigId = sourceConfigId,
    )

    // Update index
    val existingIndex = listSessions().filterNot(_.id == sessionId)
    saveIndex(meta :: existingIndex)

    // Store serialised state
    try
      val json = serializeState(state)
      dom.window.localStorage.setItem(SessionPrefix + sessionId, json)
    catch case _: Exception => ()

    // Collect images into gallery
    collectImages(state)

  def loadSession(sessionId: String): Option[CalendarState] =
    try
      Option(dom.window.localStorage.getItem(SessionPrefix + sessionId))
        .filter(_.nonEmpty)
        .flatMap(json => deserializeState(json))
    catch case _: Exception => None

  def deleteSession(sessionId: String): Unit =
    try
      dom.window.localStorage.removeItem(SessionPrefix + sessionId)
      val updated = listSessions().filterNot(_.id == sessionId)
      saveIndex(updated)
    catch case _: Exception => ()

  def updateSessionTitle(sessionId: String, newTitle: String): Unit =
    val sessions = listSessions()
    val updated = sessions.map(s => if s.id == sessionId then s.copy(title = newTitle) else s)
    saveIndex(updated)

  /** Check whether any saved session exists */
  def hasAnySavedSession: Boolean = listSessions().nonEmpty

  /** Get the most recent session */
  def mostRecentSession: Option[SessionMeta] =
    listSessions().sortBy(-_.lastUpdated).headOption

  // ── Image gallery ────────────────────────────────────────────────

  def getGalleryImages: List[String] =
    try
      Option(dom.window.localStorage.getItem(ImageGalleryKey)) match
        case Some(jsonStr) if jsonStr.nonEmpty =>
          val arr = JSON.parse(jsonStr).asInstanceOf[js.Array[String]]
          arr.toList
        case _ => List.empty
    catch case _: Exception => List.empty

  def removeGalleryImage(imageData: String): Unit =
    try
      val images = getGalleryImages.filterNot(_ == imageData)
      val arr = js.Array[String](images*)
      dom.window.localStorage.setItem(ImageGalleryKey, JSON.stringify(arr))
    catch case _: Exception => ()

  /** Scan a CalendarState and add any non-empty image data to the gallery. */
  private def collectImages(state: CalendarState): Unit =
    try
      val existing = getGalleryImages.toSet
      val newImages = state.pages.flatMap(_.elements).flatMap {
        case p: PhotoElement   if p.imageData.nonEmpty => Some(p.imageData)
        case c: ClipartElement if c.imageData.nonEmpty => Some(c.imageData)
        case _ => None
      }.filterNot(existing.contains)

      if newImages.nonEmpty then
        val all = (existing ++ newImages).toList
        val arr = js.Array[String](all*)
        dom.window.localStorage.setItem(ImageGalleryKey, JSON.stringify(arr))
    catch case _: Exception => ()

  // ── Simplified CalendarState serialisation ───────────────────────
  // We use a minimal JSON schema: product type, format, pages (with
  // elements). This is sufficient for localStorage persistence while
  // avoiding a full ZIO-JSON codec dependency in the UI module.

  private def serializeState(state: CalendarState): String =
    def withType(obj: js.Dynamic, t: String): js.Dynamic =
      obj.updateDynamic("type")(t)
      obj

    val pagesArr = js.Array[js.Dynamic]()
    state.pages.foreach { page =>
      val elementsArr = js.Array[js.Dynamic]()
      page.elements.foreach { elem =>
        val obj = elem match
          case p: PhotoElement =>
            withType(js.Dynamic.literal(
              id = p.id,
              imageData = p.imageData,
              x = p.position.x, y = p.position.y,
              w = p.size.width, h = p.size.height,
              rotation = p.rotation, zIndex = p.zIndex,
              imageScale = p.imageScale,
              imageOffsetX = p.imageOffsetX,
              imageOffsetY = p.imageOffsetY,
            ), "photo")
          case t: TextElement =>
            withType(js.Dynamic.literal(
              id = t.id,
              text = t.text,
              x = t.position.x, y = t.position.y,
              w = t.size.width, h = t.size.height,
              rotation = t.rotation, zIndex = t.zIndex,
              fontSize = t.fontSize, fontFamily = t.fontFamily,
              color = t.color, bold = t.bold, italic = t.italic,
              textAlign = t.textAlign.toString,
            ), "text")
          case s: ShapeElement =>
            withType(js.Dynamic.literal(
              id = s.id,
              shapeType = s.shapeType.toString,
              x = s.position.x, y = s.position.y,
              w = s.size.width, h = s.size.height,
              rotation = s.rotation, zIndex = s.zIndex,
              strokeColor = s.strokeColor,
              fillColor = s.fillColor,
              strokeWidth = s.strokeWidth,
            ), "shape")
          case c: ClipartElement =>
            withType(js.Dynamic.literal(
              id = c.id,
              imageData = c.imageData,
              x = c.position.x, y = c.position.y,
              w = c.size.width, h = c.size.height,
              rotation = c.rotation, zIndex = c.zIndex,
            ), "clipart")
        elementsArr.push(obj)
      }

      val bgObj = page.template.background match
        case PageBackground.SolidColor(color) =>
          withType(js.Dynamic.literal(color = color), "solid")
        case PageBackground.BackgroundImage(data) =>
          withType(js.Dynamic.literal(imageData = data), "image")

      val monthFieldObj = js.Dynamic.literal(
        id = page.template.monthField.id,
        text = page.template.monthField.text,
        x = page.template.monthField.position.x,
        y = page.template.monthField.position.y,
        fontSize = page.template.monthField.fontSize,
        fontFamily = page.template.monthField.fontFamily,
        color = page.template.monthField.color,
      )

      val pageObj = js.Dynamic.literal(
        pageNumber = page.pageNumber,
        elements = elementsArr,
        background = bgObj,
        monthField = monthFieldObj,
        templateType = page.template.templateType.toString,
      )
      pagesArr.push(pageObj)
    }

    val root = js.Dynamic.literal(
      productType = state.productType.toString,
      formatId = state.productFormat.id,
      formatNameEn = state.productFormat.nameEn,
      formatNameCs = state.productFormat.nameCs,
      formatWidthMm = state.productFormat.widthMm,
      formatHeightMm = state.productFormat.heightMm,
      currentPageIndex = state.currentPageIndex,
      pages = pagesArr,
    )
    JSON.stringify(root)

  private def deserializeState(json: String): Option[CalendarState] =
    try
      val root = JSON.parse(json)
      val productType = root.productType.asInstanceOf[String] match
        case "MonthlyCalendar"  => VisualProductType.MonthlyCalendar
        case "WeeklyCalendar"   => VisualProductType.WeeklyCalendar
        case "BiweeklyCalendar" => VisualProductType.BiweeklyCalendar
        case "PhotoBook"        => VisualProductType.PhotoBook
        case "WallPicture"      => VisualProductType.WallPicture
        case _                  => VisualProductType.MonthlyCalendar

      val formatId = root.formatId.asInstanceOf[String]
      val format = ProductFormat.formatsFor(productType).find(_.id == formatId).getOrElse {
        // Fallback for custom formats
        ProductFormat(
          formatId,
          root.formatNameEn.asInstanceOf[String],
          root.formatNameCs.asInstanceOf[String],
          root.formatWidthMm.asInstanceOf[Double].toInt,
          root.formatHeightMm.asInstanceOf[Double].toInt,
        )
      }

      val pagesArr = root.pages.asInstanceOf[js.Array[js.Dynamic]]
      val pages = pagesArr.toList.map { pageObj =>
        val pageNumber = pageObj.pageNumber.asInstanceOf[Double].toInt

        val bg = pageObj.background.`type`.asInstanceOf[String] match
          case "image" => PageBackground.BackgroundImage(pageObj.background.imageData.asInstanceOf[String])
          case _       => PageBackground.SolidColor(pageObj.background.color.asInstanceOf[String])

        val monthField = TemplateTextField(
          id = pageObj.monthField.id.asInstanceOf[String],
          text = pageObj.monthField.text.asInstanceOf[String],
          position = Position(pageObj.monthField.x.asInstanceOf[Double], pageObj.monthField.y.asInstanceOf[Double]),
          fontSize = pageObj.monthField.fontSize.asInstanceOf[Double].toInt,
          fontFamily = pageObj.monthField.fontFamily.asInstanceOf[String],
          color = pageObj.monthField.color.asInstanceOf[String],
        )

        val templateType = pageObj.templateType.asInstanceOf[String] match
          case "GridTemplate" => CalendarTemplateType.GridTemplate
          case _              => CalendarTemplateType.GridTemplate

        val elements = pageObj.elements.asInstanceOf[js.Array[js.Dynamic]].toList.map { e =>
          e.`type`.asInstanceOf[String] match
            case "photo" =>
              PhotoElement(
                id = e.id.asInstanceOf[String],
                imageData = e.imageData.asInstanceOf[String],
                position = Position(e.x.asInstanceOf[Double], e.y.asInstanceOf[Double]),
                size = Size(e.w.asInstanceOf[Double], e.h.asInstanceOf[Double]),
                rotation = e.rotation.asInstanceOf[Double],
                zIndex = e.zIndex.asInstanceOf[Double].toInt,
                imageScale = e.imageScale.asInstanceOf[Double],
                imageOffsetX = e.imageOffsetX.asInstanceOf[Double],
                imageOffsetY = e.imageOffsetY.asInstanceOf[Double],
              )
            case "text" =>
              val align = e.textAlign.asInstanceOf[String] match
                case "Center" => TextAlignment.Center
                case "Right"  => TextAlignment.Right
                case _        => TextAlignment.Left
              TextElement(
                id = e.id.asInstanceOf[String],
                text = e.text.asInstanceOf[String],
                position = Position(e.x.asInstanceOf[Double], e.y.asInstanceOf[Double]),
                size = Size(e.w.asInstanceOf[Double], e.h.asInstanceOf[Double]),
                rotation = e.rotation.asInstanceOf[Double],
                zIndex = e.zIndex.asInstanceOf[Double].toInt,
                fontSize = e.fontSize.asInstanceOf[Double].toInt,
                fontFamily = e.fontFamily.asInstanceOf[String],
                color = e.color.asInstanceOf[String],
                bold = e.bold.asInstanceOf[Boolean],
                italic = e.italic.asInstanceOf[Boolean],
                textAlign = align,
              )
            case "shape" =>
              val st = e.shapeType.asInstanceOf[String] match
                case "Rectangle" => ShapeType.Rectangle
                case _           => ShapeType.Line
              ShapeElement(
                id = e.id.asInstanceOf[String],
                shapeType = st,
                position = Position(e.x.asInstanceOf[Double], e.y.asInstanceOf[Double]),
                size = Size(e.w.asInstanceOf[Double], e.h.asInstanceOf[Double]),
                rotation = e.rotation.asInstanceOf[Double],
                zIndex = e.zIndex.asInstanceOf[Double].toInt,
                strokeColor = e.strokeColor.asInstanceOf[String],
                fillColor = e.fillColor.asInstanceOf[String],
                strokeWidth = e.strokeWidth.asInstanceOf[Double],
              )
            case "clipart" =>
              ClipartElement(
                id = e.id.asInstanceOf[String],
                imageData = e.imageData.asInstanceOf[String],
                position = Position(e.x.asInstanceOf[Double], e.y.asInstanceOf[Double]),
                size = Size(e.w.asInstanceOf[Double], e.h.asInstanceOf[Double]),
                rotation = e.rotation.asInstanceOf[Double],
                zIndex = e.zIndex.asInstanceOf[Double].toInt,
              )
            case _ =>
              PhotoElement(e.id.asInstanceOf[String], "", Position(0, 0), Size(100, 100))
        }

        CalendarPage(
          pageNumber = pageNumber,
          template = CalendarTemplate(
            templateType = templateType,
            background = bg,
            monthField = monthField,
            daysGrid = List.empty, // days grid is regenerated from template type
          ),
          elements = elements,
        )
      }

      val currentPageIndex = root.currentPageIndex.asInstanceOf[Double].toInt
      Some(CalendarState(pages, currentPageIndex.min(pages.length - 1).max(0), productType, format))
    catch
      case _: Exception => None
}
