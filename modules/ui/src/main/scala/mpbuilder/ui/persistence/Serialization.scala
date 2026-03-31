package mpbuilder.ui.persistence

import scala.scalajs.js
import mpbuilder.ui.visualeditor.*

/** Conversion between EditorState models and js.Dynamic for IndexedDB storage. */
object Serialization:

  // ─── EditorSession ─────────────────────────────────────────────

  def sessionToJs(s: EditorSession): js.Dynamic =
    js.Dynamic.literal(
      id = s.id,
      title = s.title,
      sessionName = s.sessionName.getOrElse(null),
      configurationId = s.configurationId.getOrElse(null),
      productContext = s.productContext.map(productContextToJs).getOrElse(null),
      editorState = editorStateToJs(s.editorState),
      createdAt = s.createdAt,
      updatedAt = s.updatedAt,
      thumbnailDataUrl = s.thumbnailDataUrl.getOrElse(null),
    )

  def sessionFromJs(d: js.Dynamic): EditorSession =
    EditorSession(
      id = d.id.asInstanceOf[String],
      title = d.title.asInstanceOf[String],
      sessionName = nullableStr(d.sessionName),
      configurationId = Option(d.configurationId.asInstanceOf[String]).filter(_ != null),
      productContext = {
        val ctx = d.productContext
        if ctx == null || js.isUndefined(ctx) then None
        else Some(productContextFromJs(ctx))
      },
      editorState = editorStateFromJs(d.editorState.asInstanceOf[js.Dynamic]),
      createdAt = d.createdAt.asInstanceOf[Double],
      updatedAt = d.updatedAt.asInstanceOf[Double],
      thumbnailDataUrl = {
        val v = d.thumbnailDataUrl
        if v == null || js.isUndefined(v) then None
        else Some(v.asInstanceOf[String])
      },
    )

  // ─── ProductContext ────────────────────────────────────────────

  private def productContextToJs(ctx: ProductContext): js.Dynamic =
    js.Dynamic.literal(
      widthMm = ctx.widthMm,
      heightMm = ctx.heightMm,
      pageCount = ctx.pageCount.map(_.toDouble).getOrElse(null.asInstanceOf[Double]),
      categoryId = ctx.categoryId.getOrElse(null),
      categoryName = ctx.categoryName.getOrElse(null),
      bindingMethod = ctx.bindingMethod.getOrElse(null),
      visualProductType = ctx.visualProductType.map(_.toString).getOrElse(null),
    )

  private def productContextFromJs(d: js.Dynamic): ProductContext =
    ProductContext(
      widthMm = d.widthMm.asInstanceOf[Double],
      heightMm = d.heightMm.asInstanceOf[Double],
      pageCount = {
        val v = d.pageCount
        if v == null || js.isUndefined(v) then None
        else Some(v.asInstanceOf[Double].toInt)
      },
      categoryId = nullableStr(d.categoryId),
      categoryName = nullableStr(d.categoryName),
      bindingMethod = nullableStr(d.bindingMethod),
      visualProductType = nullableStr(d.visualProductType).flatMap(parseVisualProductType),
    )

  // ─── EditorState ───────────────────────────────────────────────

  private def editorStateToJs(st: EditorState): js.Dynamic =
    js.Dynamic.literal(
      pages = js.Array(st.pages.map(editorPageToJs)*),
      currentPageIndex = st.currentPageIndex,
      productType = st.productType.toString,
      productFormat = productFormatToJs(st.productFormat),
    )

  private def editorStateFromJs(d: js.Dynamic): EditorState =
    val pagesArr = d.pages.asInstanceOf[js.Array[js.Dynamic]]
    EditorState(
      pages = pagesArr.toList.map(editorPageFromJs),
      currentPageIndex = d.currentPageIndex.asInstanceOf[Double].toInt,
      productType = parseVisualProductType(d.productType.asInstanceOf[String]).getOrElse(VisualProductType.MonthlyCalendar),
      productFormat = productFormatFromJs(d.productFormat.asInstanceOf[js.Dynamic]),
    )

  // ─── EditorPage ────────────────────────────────────────────────

  private def editorPageToJs(p: EditorPage): js.Dynamic =
    js.Dynamic.literal(
      pageNumber = p.pageNumber,
      template = pageTemplateToJs(p.template),
      elements = js.Array(p.elements.map(canvasElementToJs)*),
    )

  private def editorPageFromJs(d: js.Dynamic): EditorPage =
    EditorPage(
      pageNumber = d.pageNumber.asInstanceOf[Double].toInt,
      template = pageTemplateFromJs(d.template.asInstanceOf[js.Dynamic]),
      elements = d.elements.asInstanceOf[js.Array[js.Dynamic]].toList.map(canvasElementFromJs),
    )

  // ─── PageTemplate ──────────────────────────────────────────────

  private def pageTemplateToJs(t: PageTemplate): js.Dynamic =
    js.Dynamic.literal(
      templateType = t.templateType.toString,
      background = backgroundToJs(t.background),
      monthField = templateTextFieldToJs(t.monthField),
      daysGrid = js.Array(t.daysGrid.map(templateTextFieldToJs)*),
    )

  private def pageTemplateFromJs(d: js.Dynamic): PageTemplate =
    PageTemplate(
      templateType = PageTemplateType.GridTemplate, // Only one for now
      background = backgroundFromJs(d.background.asInstanceOf[js.Dynamic]),
      monthField = templateTextFieldFromJs(d.monthField.asInstanceOf[js.Dynamic]),
      daysGrid = d.daysGrid.asInstanceOf[js.Array[js.Dynamic]].toList.map(templateTextFieldFromJs),
    )

  // ─── TemplateTextField ─────────────────────────────────────────

  private def templateTextFieldToJs(f: TemplateTextField): js.Dynamic =
    js.Dynamic.literal(
      id = f.id, text = f.text,
      x = f.position.x, y = f.position.y,
      fontSize = f.fontSize, fontFamily = f.fontFamily, color = f.color,
    )

  private def templateTextFieldFromJs(d: js.Dynamic): TemplateTextField =
    TemplateTextField(
      id = d.id.asInstanceOf[String],
      text = d.text.asInstanceOf[String],
      position = Position(d.x.asInstanceOf[Double], d.y.asInstanceOf[Double]),
      fontSize = d.fontSize.asInstanceOf[Double].toInt,
      fontFamily = d.fontFamily.asInstanceOf[String],
      color = d.color.asInstanceOf[String],
    )

  // ─── PageBackground ────────────────────────────────────────────

  private def backgroundToJs(bg: PageBackground): js.Dynamic = bg match
    case PageBackground.SolidColor(color) =>
      js.Dynamic.literal(`type` = "solid", color = color)
    case PageBackground.BackgroundImage(imageData) =>
      js.Dynamic.literal(`type` = "image", imageData = imageData)

  private def backgroundFromJs(d: js.Dynamic): PageBackground =
    d.`type`.asInstanceOf[String] match
      case "image" => PageBackground.BackgroundImage(d.imageData.asInstanceOf[String])
      case _       => PageBackground.SolidColor(d.color.asInstanceOf[String])

  // ─── CanvasElement ─────────────────────────────────────────────

  private def canvasElementToJs(e: CanvasElement): js.Dynamic = e match
    case p: PhotoElement =>
      js.Dynamic.literal(
        `type` = "photo", id = p.id, imageData = p.imageData,
        x = p.position.x, y = p.position.y, w = p.size.width, h = p.size.height,
        rotation = p.rotation, zIndex = p.zIndex,
        imageScale = p.imageScale, imageOffsetX = p.imageOffsetX, imageOffsetY = p.imageOffsetY,
      )
    case t: TextElement =>
      js.Dynamic.literal(
        `type` = "text", id = t.id, text = t.text,
        x = t.position.x, y = t.position.y, w = t.size.width, h = t.size.height,
        rotation = t.rotation, zIndex = t.zIndex,
        fontSize = t.fontSize, fontFamily = t.fontFamily, color = t.color,
        bold = t.bold, italic = t.italic, textAlign = t.textAlign.toString,
      )
    case s: ShapeElement =>
      js.Dynamic.literal(
        `type` = "shape", id = s.id, shapeType = s.shapeType.toString,
        x = s.position.x, y = s.position.y, w = s.size.width, h = s.size.height,
        rotation = s.rotation, zIndex = s.zIndex,
        strokeColor = s.strokeColor, fillColor = s.fillColor, strokeWidth = s.strokeWidth,
      )
    case c: ClipartElement =>
      js.Dynamic.literal(
        `type` = "clipart", id = c.id, imageData = c.imageData,
        x = c.position.x, y = c.position.y, w = c.size.width, h = c.size.height,
        rotation = c.rotation, zIndex = c.zIndex,
      )

  private def canvasElementFromJs(d: js.Dynamic): CanvasElement =
    val pos = Position(d.x.asInstanceOf[Double], d.y.asInstanceOf[Double])
    val size = Size(d.w.asInstanceOf[Double], d.h.asInstanceOf[Double])
    val rot = d.rotation.asInstanceOf[Double]
    val z = d.zIndex.asInstanceOf[Double].toInt

    d.`type`.asInstanceOf[String] match
      case "photo" =>
        PhotoElement(
          id = d.id.asInstanceOf[String], imageData = d.imageData.asInstanceOf[String],
          position = pos, size = size, rotation = rot, zIndex = z,
          imageScale = d.imageScale.asInstanceOf[Double],
          imageOffsetX = d.imageOffsetX.asInstanceOf[Double],
          imageOffsetY = d.imageOffsetY.asInstanceOf[Double],
        )
      case "text" =>
        TextElement(
          id = d.id.asInstanceOf[String], text = d.text.asInstanceOf[String],
          position = pos, size = size, rotation = rot, zIndex = z,
          fontSize = d.fontSize.asInstanceOf[Double].toInt,
          fontFamily = d.fontFamily.asInstanceOf[String],
          color = d.color.asInstanceOf[String],
          bold = d.bold.asInstanceOf[Boolean],
          italic = d.italic.asInstanceOf[Boolean],
          textAlign = parseTextAlignment(d.textAlign.asInstanceOf[String]),
        )
      case "shape" =>
        ShapeElement(
          id = d.id.asInstanceOf[String],
          shapeType = if d.shapeType.asInstanceOf[String] == "Line" then ShapeType.Line else ShapeType.Rectangle,
          position = pos, size = size, rotation = rot, zIndex = z,
          strokeColor = d.strokeColor.asInstanceOf[String],
          fillColor = d.fillColor.asInstanceOf[String],
          strokeWidth = d.strokeWidth.asInstanceOf[Double],
        )
      case "clipart" =>
        ClipartElement(
          id = d.id.asInstanceOf[String], imageData = d.imageData.asInstanceOf[String],
          position = pos, size = size, rotation = rot, zIndex = z,
        )
      case _ =>
        // Fallback: treat as clipart
        ClipartElement(id = "unknown", imageData = "", position = pos, size = size, rotation = rot, zIndex = z)

  // ─── ProductFormat ─────────────────────────────────────────────

  private def productFormatToJs(f: ProductFormat): js.Dynamic =
    js.Dynamic.literal(
      id = f.id, nameEn = f.nameEn, nameCs = f.nameCs,
      widthMm = f.widthMm, heightMm = f.heightMm,
    )

  private def productFormatFromJs(d: js.Dynamic): ProductFormat =
    ProductFormat(
      id = d.id.asInstanceOf[String],
      nameEn = d.nameEn.asInstanceOf[String],
      nameCs = d.nameCs.asInstanceOf[String],
      widthMm = d.widthMm.asInstanceOf[Double].toInt,
      heightMm = d.heightMm.asInstanceOf[Double].toInt,
    )

  // ─── Helpers ───────────────────────────────────────────────────

  private def nullableStr(v: js.Dynamic): Option[String] =
    if v == null || js.isUndefined(v) then None
    else Some(v.asInstanceOf[String])

  private def parseVisualProductType(s: String): Option[VisualProductType] = s match
    case "MonthlyCalendar"  => Some(VisualProductType.MonthlyCalendar)
    case "WeeklyCalendar"   => Some(VisualProductType.WeeklyCalendar)
    case "BiweeklyCalendar" => Some(VisualProductType.BiweeklyCalendar)
    case "PhotoBook"        => Some(VisualProductType.PhotoBook)
    case "WallPicture"      => Some(VisualProductType.WallPicture)
    case "GenericProduct"   => Some(VisualProductType.GenericProduct)
    case _                  => None

  private def parseTextAlignment(s: String): TextAlignment = s match
    case "Center" => TextAlignment.Center
    case "Right"  => TextAlignment.Right
    case _        => TextAlignment.Left
