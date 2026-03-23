package mpbuilder.ui.calendar

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/** Converts EditorSession to/from js.Dynamic for IndexedDB storage */
object SessionCodec:

  // ─── Session ─────────────────────────────────────────────────────

  def sessionToJs(session: EditorSession): js.Dynamic =
    js.Dynamic.literal(
      "id"                    -> session.id,
      "name"                  -> session.name,
      "productType"           -> productTypeToString(session.productType),
      "productFormatId"       -> session.productFormat.id,
      "productFormatNameEn"   -> session.productFormat.nameEn,
      "productFormatNameCs"   -> session.productFormat.nameCs,
      "productFormatWidthMm"  -> session.productFormat.widthMm,
      "productFormatHeightMm" -> session.productFormat.heightMm,
      "pages"                 -> session.pages.map(pageToJs).toJSArray,
      "createdAt"             -> session.createdAt,
      "updatedAt"             -> session.updatedAt,
    )

  def sessionFromJs(obj: js.Dynamic): EditorSession =
    EditorSession(
      id = obj.id.asInstanceOf[String],
      name = obj.name.asInstanceOf[String],
      productType = productTypeFromString(obj.productType.asInstanceOf[String]),
      productFormat = ProductFormat(
        id = obj.productFormatId.asInstanceOf[String],
        nameEn = obj.productFormatNameEn.asInstanceOf[String],
        nameCs = obj.productFormatNameCs.asInstanceOf[String],
        widthMm = obj.productFormatWidthMm.asInstanceOf[Double].toInt,
        heightMm = obj.productFormatHeightMm.asInstanceOf[Double].toInt,
      ),
      pages = obj.pages.asInstanceOf[js.Array[js.Dynamic]].toList.map(pageFromJs),
      createdAt = obj.createdAt.asInstanceOf[Double],
      updatedAt = obj.updatedAt.asInstanceOf[Double],
    )

  def summaryFromJs(obj: js.Dynamic): SessionSummary =
    SessionSummary(
      id = obj.id.asInstanceOf[String],
      name = obj.name.asInstanceOf[String],
      productType = productTypeFromString(obj.productType.asInstanceOf[String]),
      productFormat = ProductFormat(
        id = obj.productFormatId.asInstanceOf[String],
        nameEn = obj.productFormatNameEn.asInstanceOf[String],
        nameCs = obj.productFormatNameCs.asInstanceOf[String],
        widthMm = obj.productFormatWidthMm.asInstanceOf[Double].toInt,
        heightMm = obj.productFormatHeightMm.asInstanceOf[Double].toInt,
      ),
      pageCount = obj.pages.asInstanceOf[js.Array[js.Dynamic]].length,
      updatedAt = obj.updatedAt.asInstanceOf[Double],
    )

  // ─── Product type ────────────────────────────────────────────────

  private def productTypeToString(pt: VisualProductType): String = pt match
    case VisualProductType.MonthlyCalendar  => "monthly"
    case VisualProductType.WeeklyCalendar   => "weekly"
    case VisualProductType.BiweeklyCalendar => "biweekly"
    case VisualProductType.PhotoBook        => "photobook"
    case VisualProductType.WallPicture      => "wallpicture"

  private def productTypeFromString(s: String): VisualProductType = s match
    case "weekly"      => VisualProductType.WeeklyCalendar
    case "biweekly"    => VisualProductType.BiweeklyCalendar
    case "photobook"   => VisualProductType.PhotoBook
    case "wallpicture" => VisualProductType.WallPicture
    case _             => VisualProductType.MonthlyCalendar

  // ─── Page ────────────────────────────────────────────────────────

  private def pageToJs(page: CalendarPage): js.Dynamic =
    js.Dynamic.literal(
      "pageNumber" -> page.pageNumber,
      "template"   -> templateToJs(page.template),
      "elements"   -> page.elements.map(elementToJs).toJSArray,
    )

  private def pageFromJs(obj: js.Dynamic): CalendarPage =
    CalendarPage(
      pageNumber = obj.pageNumber.asInstanceOf[Double].toInt,
      template = templateFromJs(obj.template),
      elements = obj.elements.asInstanceOf[js.Array[js.Dynamic]].toList.map(elementFromJs),
    )

  // ─── Template ────────────────────────────────────────────────────

  private def templateToJs(t: CalendarTemplate): js.Dynamic =
    js.Dynamic.literal(
      "templateType" -> templateTypeToString(t.templateType),
      "background"   -> backgroundToJs(t.background),
      "monthField"   -> templateTextFieldToJs(t.monthField),
      "daysGrid"     -> t.daysGrid.map(templateTextFieldToJs).toJSArray,
    )

  private def templateFromJs(obj: js.Dynamic): CalendarTemplate =
    CalendarTemplate(
      templateType = templateTypeFromString(obj.templateType.asInstanceOf[String]),
      background = backgroundFromJs(obj.background),
      monthField = templateTextFieldFromJs(obj.monthField),
      daysGrid = obj.daysGrid.asInstanceOf[js.Array[js.Dynamic]].toList.map(templateTextFieldFromJs),
    )

  private def templateTypeToString(tt: CalendarTemplateType): String = tt match
    case CalendarTemplateType.GridTemplate => "grid"

  private def templateTypeFromString(s: String): CalendarTemplateType =
    CalendarTemplateType.GridTemplate

  // ─── Background ──────────────────────────────────────────────────

  private def backgroundToJs(bg: PageBackground): js.Dynamic = bg match
    case PageBackground.SolidColor(color) =>
      js.Dynamic.literal("type" -> "solid", "color" -> color)
    case PageBackground.BackgroundImage(imageData) =>
      js.Dynamic.literal("type" -> "image", "imageData" -> imageData)

  private def backgroundFromJs(obj: js.Dynamic): PageBackground =
    obj.`type`.asInstanceOf[String] match
      case "image" => PageBackground.BackgroundImage(obj.imageData.asInstanceOf[String])
      case _       => PageBackground.SolidColor(obj.color.asInstanceOf[String])

  // ─── TemplateTextField ───────────────────────────────────────────

  private def templateTextFieldToJs(f: TemplateTextField): js.Dynamic =
    js.Dynamic.literal(
      "id"         -> f.id,
      "text"       -> f.text,
      "x"          -> f.position.x,
      "y"          -> f.position.y,
      "fontSize"   -> f.fontSize,
      "fontFamily" -> f.fontFamily,
      "color"      -> f.color,
    )

  private def templateTextFieldFromJs(obj: js.Dynamic): TemplateTextField =
    TemplateTextField(
      id = obj.id.asInstanceOf[String],
      text = obj.text.asInstanceOf[String],
      position = Position(obj.x.asInstanceOf[Double], obj.y.asInstanceOf[Double]),
      fontSize = obj.fontSize.asInstanceOf[Double].toInt,
      fontFamily = obj.fontFamily.asInstanceOf[String],
      color = obj.color.asInstanceOf[String],
    )

  // ─── Canvas elements (discriminated union) ───────────────────────

  private def elementToJs(elem: CanvasElement): js.Dynamic = elem match
    case p: PhotoElement =>
      js.Dynamic.literal(
        "type" -> "photo", "id" -> p.id, "imageData" -> p.imageData,
        "x" -> p.position.x, "y" -> p.position.y,
        "w" -> p.size.width, "h" -> p.size.height,
        "rotation" -> p.rotation, "zIndex" -> p.zIndex,
        "imageScale" -> p.imageScale,
        "imageOffsetX" -> p.imageOffsetX, "imageOffsetY" -> p.imageOffsetY,
      )
    case t: TextElement =>
      js.Dynamic.literal(
        "type" -> "text", "id" -> t.id, "text" -> t.text,
        "x" -> t.position.x, "y" -> t.position.y,
        "w" -> t.size.width, "h" -> t.size.height,
        "rotation" -> t.rotation, "zIndex" -> t.zIndex,
        "fontSize" -> t.fontSize, "fontFamily" -> t.fontFamily,
        "color" -> t.color, "bold" -> t.bold, "italic" -> t.italic,
        "textAlign" -> alignToString(t.textAlign),
      )
    case s: ShapeElement =>
      js.Dynamic.literal(
        "type" -> "shape", "id" -> s.id,
        "shapeType" -> shapeTypeToString(s.shapeType),
        "x" -> s.position.x, "y" -> s.position.y,
        "w" -> s.size.width, "h" -> s.size.height,
        "rotation" -> s.rotation, "zIndex" -> s.zIndex,
        "strokeColor" -> s.strokeColor, "fillColor" -> s.fillColor,
        "strokeWidth" -> s.strokeWidth,
      )
    case c: ClipartElement =>
      js.Dynamic.literal(
        "type" -> "clipart", "id" -> c.id, "imageData" -> c.imageData,
        "x" -> c.position.x, "y" -> c.position.y,
        "w" -> c.size.width, "h" -> c.size.height,
        "rotation" -> c.rotation, "zIndex" -> c.zIndex,
      )

  private def elementFromJs(obj: js.Dynamic): CanvasElement =
    val pos = Position(obj.x.asInstanceOf[Double], obj.y.asInstanceOf[Double])
    val size = Size(obj.w.asInstanceOf[Double], obj.h.asInstanceOf[Double])
    val rotation = obj.rotation.asInstanceOf[Double]
    val zIndex = obj.zIndex.asInstanceOf[Double].toInt

    obj.`type`.asInstanceOf[String] match
      case "photo" =>
        PhotoElement(
          id = obj.id.asInstanceOf[String], imageData = obj.imageData.asInstanceOf[String],
          position = pos, size = size, rotation = rotation, zIndex = zIndex,
          imageScale = obj.imageScale.asInstanceOf[Double],
          imageOffsetX = obj.imageOffsetX.asInstanceOf[Double],
          imageOffsetY = obj.imageOffsetY.asInstanceOf[Double],
        )
      case "text" =>
        TextElement(
          id = obj.id.asInstanceOf[String], text = obj.text.asInstanceOf[String],
          position = pos, size = size, rotation = rotation, zIndex = zIndex,
          fontSize = obj.fontSize.asInstanceOf[Double].toInt,
          fontFamily = obj.fontFamily.asInstanceOf[String],
          color = obj.color.asInstanceOf[String],
          bold = obj.bold.asInstanceOf[Boolean],
          italic = obj.italic.asInstanceOf[Boolean],
          textAlign = alignFromString(obj.textAlign.asInstanceOf[String]),
        )
      case "shape" =>
        ShapeElement(
          id = obj.id.asInstanceOf[String],
          shapeType = shapeTypeFromString(obj.shapeType.asInstanceOf[String]),
          position = pos, size = size, rotation = rotation, zIndex = zIndex,
          strokeColor = obj.strokeColor.asInstanceOf[String],
          fillColor = obj.fillColor.asInstanceOf[String],
          strokeWidth = obj.strokeWidth.asInstanceOf[Double],
        )
      case _ =>
        ClipartElement(
          id = obj.id.asInstanceOf[String],
          imageData = obj.imageData.asInstanceOf[String],
          position = pos, size = size, rotation = rotation, zIndex = zIndex,
        )

  // ─── Enum helpers ────────────────────────────────────────────────

  private def alignToString(a: TextAlignment): String = a match
    case TextAlignment.Left   => "left"
    case TextAlignment.Center => "center"
    case TextAlignment.Right  => "right"

  private def alignFromString(s: String): TextAlignment = s match
    case "center" => TextAlignment.Center
    case "right"  => TextAlignment.Right
    case _        => TextAlignment.Left

  private def shapeTypeToString(st: ShapeType): String = st match
    case ShapeType.Line      => "line"
    case ShapeType.Rectangle => "rectangle"

  private def shapeTypeFromString(s: String): ShapeType = s match
    case "line" => ShapeType.Line
    case _      => ShapeType.Rectangle
