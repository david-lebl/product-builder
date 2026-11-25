package mpbuilder.design

import mpbuilder.shared.Language

enum RenderMode derives CanEqual:
  /** Interactive canvas: placeholders visible, `data-ve-*` hit-test attributes emitted. */
  case Editor
  /** Thumbnails / customer proof: placeholders shown subtly, no interaction attributes. */
  case Preview
  /** Production output (PDF pipeline): no placeholders, nothing but printable content. */
  case Print

/** Rendering options for [[DesignSvg]].
  *
  * @param assetUrl   resolves an asset to a loadable URL (blob:, data:, https: …); `None` renders a placeholder
  * @param idPrefix   prefix for `<defs>` ids — must be unique per SVG on the same HTML page
  * @param clipToTrim crop the bleed away (thumbnails & proofs); print output keeps the bleed
  */
final case class RenderOptions(
    assetUrl: AssetId => Option[String],
    mode: RenderMode = RenderMode.Preview,
    idPrefix: String = "d",
    clipToTrim: Boolean = false,
    placeholderLabel: String = "",
    widthAttr: Option[String] = None,
)

/** Pure SVG serializer of a [[DesignPage]].
  *
  * The same markup drives the browser canvas, page thumbnails, the customer proof attached to the order line,
  * and — through headless Chromium — the print PDF. User space is **10 units per millimetre** (`U`): browsers
  * clamp very small font sizes, so text is never laid out at sub-6px sizes. Wrapped text uses an XHTML
  * `<foreignObject>`, which is why the print pipeline renders with a browser engine (see the architecture doc).
  */
object DesignSvg:

  val U: Double = 10.0

  private[design] def num(v: Double): String =
    val r = math.round(v * 1000.0) / 1000.0
    if r == r.toLong.toDouble then r.toLong.toString else r.toString

  private def u(mm: Double): String = num(mm * U)

  def escapeXml(s: String): String =
    val sb = new StringBuilder(s.length + 16)
    s.foreach {
      case '&'  => sb.append("&amp;")
      case '<'  => sb.append("&lt;")
      case '>'  => sb.append("&gt;")
      case '"'  => sb.append("&quot;")
      case '\'' => sb.append("&#39;")
      case c if c < ' ' && c != '\n' && c != '\t' => ()
      case c    => sb.append(c)
    }
    sb.toString

  def viewBox(format: DesignFormat, clipToTrim: Boolean): Rect =
    if clipToTrim then format.trimBox else format.bleedBox

  def renderPage(doc: DesignDocument, page: DesignPage, opts: RenderOptions): String =
    val format = doc.format
    val vb     = viewBox(format, opts.clipToTrim)
    val sb     = new StringBuilder(4096)
    val width  = opts.widthAttr.map(w => s""" width="$w"""").getOrElse("")
    sb.append(
      s"""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="${u(vb.x)} ${u(vb.y)} ${u(vb.width)} ${u(vb.height)}"$width preserveAspectRatio="xMidYMid meet" style="display:block;overflow:hidden">"""
    )
    val defs = new StringBuilder
    defs.append(
      s"""<clipPath id="${opts.idPrefix}-page"><rect x="${u(vb.x)}" y="${u(vb.y)}" width="${u(vb.width)}" height="${u(vb.height)}"/></clipPath>"""
    )
    defs.append(
      s"""<filter id="${opts.idPrefix}-gray"><feColorMatrix type="saturate" values="0"/></filter>""" +
        s"""<filter id="${opts.idPrefix}-sepia"><feColorMatrix type="matrix" values="0.393 0.769 0.189 0 0 0.349 0.686 0.168 0 0 0.272 0.534 0.131 0 0 0 0 0 1 0"/></filter>"""
    )
    val body = new StringBuilder
    body.append(fillRect(format.bleedBox, page.background, s"${opts.idPrefix}-bg", defs))
    page.elements.foreach(el => body.append(renderElement(doc, el, opts, defs)))
    sb.append("<defs>").append(defs).append("</defs>")
    sb.append(s"""<g clip-path="url(#${opts.idPrefix}-page)">""").append(body).append("</g>")
    sb.append("</svg>")
    sb.toString

  private def paint(fill: Fill, gradientId: String, defs: StringBuilder): String = fill match
    case Fill.NoFill      => "none"
    case Fill.Solid(c)    => c.hex
    case Fill.Gradient(from, to, angle) =>
      defs.append(
        s"""<linearGradient id="$gradientId" gradientTransform="rotate(${num(angle)} 0.5 0.5)"><stop offset="0" stop-color="${from.hex}"/><stop offset="1" stop-color="${to.hex}"/></linearGradient>"""
      )
      s"url(#$gradientId)"

  private def fillRect(r: Rect, fill: Fill, gradientId: String, defs: StringBuilder): String =
    fill match
      case Fill.NoFill => ""
      case other =>
        s"""<rect x="${u(r.x)}" y="${u(r.y)}" width="${u(r.width)}" height="${u(r.height)}" fill="${paint(other, gradientId, defs)}"/>"""

  private def strokeAttrs(stroke: Option[Stroke]): String = stroke match
    case None => """ stroke="none""""
    case Some(s) =>
      val dash = s.dash match
        case StrokeDash.Solid  => ""
        case StrokeDash.Dashed => s""" stroke-dasharray="${u(s.widthMm * 4)} ${u(s.widthMm * 2.5)}""""
        case StrokeDash.Dotted => s""" stroke-dasharray="0 ${u(s.widthMm * 2)}" stroke-linecap="round""""
      s""" stroke="${s.color.hex}" stroke-width="${u(s.widthMm)}"$dash"""

  private def renderElement(doc: DesignDocument, el: DesignElement, opts: RenderOptions, defs: StringBuilder): String =
    val fr        = el.frame
    val rotate    = if fr.rotation != 0.0 then s""" transform="rotate(${num(fr.rotation)} ${u(fr.centerX)} ${u(fr.centerY)})"""" else ""
    val opacity   = if el.opacity < 1.0 then s""" opacity="${num(el.opacity)}"""" else ""
    val hit =
      if opts.mode == RenderMode.Editor then
        s""" data-ve-el="${escapeXml(el.id.value)}"${if el.locked then " data-ve-locked=\"true\"" else ""} style="cursor:${if el.locked then "default" else "move"}""""
      else ""
    val inner = el.content match
      case ElementContent.Text(text, style)                      => renderText(fr, text, style)
      case img: ElementContent.Image                             => renderImage(doc, el, img, opts, defs)
      case ElementContent.Shape(kind, fill, stroke, radius)      => renderShape(el.id, fr, kind, fill, stroke, radius, opts, defs)
      case ElementContent.Clipart(art, color)                    => renderClipart(fr, art, color)
      case ElementContent.Calendar(spec)                         => renderCalendar(fr, spec)
    s"""<g$hit$rotate$opacity>$inner</g>"""

  // ── Text ─────────────────────────────────────────────────────────────────

  def cssTextStyle(style: TextStyle): String =
    val face    = FontCatalog.get(style.fontKey)
    val weight  = face.nearestWeight(style.weight)
    val justify = style.verticalAlign match
      case TextVAlign.Top    => "flex-start"
      case TextVAlign.Middle => "center"
      case TextVAlign.Bottom => "flex-end"
    val align = style.align match
      case TextHAlign.Left    => "left"
      case TextHAlign.Center  => "center"
      case TextHAlign.Right   => "right"
      case TextHAlign.Justify => "justify"
    val fontPx = num(Units.ptToMm(style.sizePt) * U)
    List(
      "width:100%",
      "height:100%",
      "display:flex",
      "flex-direction:column",
      s"justify-content:$justify",
      "box-sizing:border-box",
      "margin:0",
      "padding:0",
      s"font-family:${face.cssFamily.replace("\"", "'")}",
      s"font-size:${fontPx}px",
      s"font-weight:$weight",
      s"font-style:${if style.italic && face.hasItalic then "italic" else "normal"}",
      s"line-height:${num(style.lineHeight)}",
      s"letter-spacing:${num(style.letterSpacingEm)}em",
      s"color:${style.color.hex}",
      s"text-align:$align",
      s"text-transform:${if style.uppercase then "uppercase" else "none"}",
      s"text-decoration:${if style.underline then "underline" else "none"}",
      "white-space:pre-wrap",
      "overflow-wrap:break-word",
      "word-break:normal",
      "font-kerning:normal",
      "-webkit-font-smoothing:antialiased",
    ).mkString(";")

  private def renderText(fr: Placement, text: String, style: TextStyle): String =
    s"""<foreignObject x="${u(fr.x)}" y="${u(fr.y)}" width="${u(fr.width)}" height="${u(fr.height)}" style="overflow:visible">""" +
      s"""<div xmlns="http://www.w3.org/1999/xhtml" style="${cssTextStyle(style)}"><div>${escapeXml(text)}</div></div></foreignObject>"""

  // ── Images ───────────────────────────────────────────────────────────────

  private def renderImage(
      doc: DesignDocument,
      el: DesignElement,
      img: ElementContent.Image,
      opts: RenderOptions,
      defs: StringBuilder,
  ): String =
    val fr     = el.frame
    val clipId = s"${opts.idPrefix}-c-${el.id.value}"
    val radius = if img.cornerRadiusMm > 0 then s""" rx="${u(img.cornerRadiusMm)}"""" else ""
    defs.append(s"""<clipPath id="$clipId"><rect x="${u(fr.x)}" y="${u(fr.y)}" width="${u(fr.width)}" height="${u(fr.height)}"$radius/></clipPath>""")
    val resolved = for
      id    <- img.asset
      asset <- doc.asset(id)
      url   <- opts.assetUrl(id)
    yield (asset, url)
    val strokeEl = img.stroke match
      case Some(s) =>
        s"""<rect x="${u(fr.x)}" y="${u(fr.y)}" width="${u(fr.width)}" height="${u(fr.height)}"$radius fill="none"${strokeAttrs(Some(s))}/>"""
      case None => ""
    resolved match
      case Some((asset, url)) =>
        val p = DesignGeometry.imagePlacement(fr, img.crop, asset.pixelWidth, asset.pixelHeight)
        val filter = img.filter match
          case ImageFilter.Original  => ""
          case ImageFilter.Grayscale => s""" filter="url(#${opts.idPrefix}-gray)""""
          case ImageFilter.Sepia     => s""" filter="url(#${opts.idPrefix}-sepia)""""
        val safeUrl = escapeXml(url)
        s"""<g clip-path="url(#$clipId)"><image href="$safeUrl" xlink:href="$safeUrl" x="${u(fr.x + p.x)}" y="${u(fr.y + p.y)}" width="${u(p.width)}" height="${u(p.height)}" preserveAspectRatio="none"$filter/></g>$strokeEl"""
      case None =>
        opts.mode match
          case RenderMode.Print => strokeEl
          case _ =>
            val iconSize = math.min(fr.width, fr.height) * 0.22
            val cx       = fr.centerX
            val cy       = fr.centerY - (if opts.placeholderLabel.nonEmpty then iconSize * 0.25 else 0.0)
            val label =
              if opts.mode == RenderMode.Editor && opts.placeholderLabel.nonEmpty && fr.width > 18 && fr.height > 14 then
                val fs = math.max(2.2, math.min(fr.width * 0.06, 5.0))
                s"""<text x="${u(cx)}" y="${u(cy + iconSize * 0.95)}" text-anchor="middle" dominant-baseline="hanging" font-family="Inter, system-ui, sans-serif" font-size="${u(fs)}" font-weight="600" fill="#64748b">${escapeXml(opts.placeholderLabel)}</text>"""
              else ""
            val s  = iconSize
            val x0 = cx - s / 2
            val y0 = cy - s / 2
            val icon =
              s"""<rect x="${u(x0)}" y="${u(y0)}" width="${u(s)}" height="${u(s * 0.8)}" rx="${u(s * 0.12)}" fill="none" stroke="#94a3b8" stroke-width="${u(s * 0.07)}"/>""" +
                s"""<circle cx="${u(x0 + s * 0.32)}" cy="${u(y0 + s * 0.28)}" r="${u(s * 0.09)}" fill="#94a3b8"/>""" +
                s"""<path d="M${u(x0 + s * 0.08)} ${u(y0 + s * 0.7)} L${u(x0 + s * 0.4)} ${u(y0 + s * 0.42)} L${u(x0 + s * 0.6)} ${u(y0 + s * 0.6)} L${u(x0 + s * 0.74)} ${u(y0 + s * 0.48)} L${u(x0 + s * 0.92)} ${u(y0 + s * 0.7)}" fill="none" stroke="#94a3b8" stroke-width="${u(s * 0.07)}" stroke-linejoin="round"/>"""
            s"""<g clip-path="url(#$clipId)"><rect x="${u(fr.x)}" y="${u(fr.y)}" width="${u(fr.width)}" height="${u(fr.height)}" fill="#e2e8f0"/>$icon$label</g>$strokeEl"""

  // ── Shapes & clipart ─────────────────────────────────────────────────────

  private def renderShape(
      id: ElementId,
      fr: Placement,
      kind: ShapeKind,
      fill: Fill,
      stroke: Option[Stroke],
      radius: Double,
      opts: RenderOptions,
      defs: StringBuilder,
  ): String =
    val gid = s"${opts.idPrefix}-g-${id.value}"
    val f   = paint(fill, gid, defs)
    val st  = strokeAttrs(stroke)
    kind match
      case ShapeKind.Rectangle =>
        val rx = if radius > 0 then s""" rx="${u(radius)}"""" else ""
        s"""<rect x="${u(fr.x)}" y="${u(fr.y)}" width="${u(fr.width)}" height="${u(fr.height)}"$rx fill="$f"$st/>"""
      case ShapeKind.Ellipse =>
        s"""<ellipse cx="${u(fr.centerX)}" cy="${u(fr.centerY)}" rx="${u(fr.width / 2)}" ry="${u(fr.height / 2)}" fill="$f"$st/>"""
      case ShapeKind.Triangle =>
        s"""<polygon points="${u(fr.centerX)},${u(fr.y)} ${u(fr.x + fr.width)},${u(fr.y + fr.height)} ${u(fr.x)},${u(fr.y + fr.height)}" fill="$f"$st/>"""
      case ShapeKind.Line =>
        val lineColor = fill match
          case Fill.Solid(c)          => c
          case Fill.Gradient(c, _, _) => c
          case Fill.NoFill            => DesignColor.Ink
        val lineStroke = stroke.getOrElse(Stroke(lineColor, math.max(0.2, fr.height)))
        // Transparent hit area keeps thin lines easy to grab in the editor.
        val hitArea =
          if opts.mode == RenderMode.Editor then
            s"""<rect x="${u(fr.x)}" y="${u(fr.centerY - 2)}" width="${u(fr.width)}" height="${u(4)}" fill="transparent"/>"""
          else ""
        hitArea + s"""<line x1="${u(fr.x)}" y1="${u(fr.centerY)}" x2="${u(fr.x + fr.width)}" y2="${u(fr.centerY)}"${strokeAttrs(Some(lineStroke))}/>"""

  private def renderClipart(fr: Placement, art: ClipartKind, color: DesignColor): String =
    val sx   = num(fr.width * U / 100.0)
    val sy   = num(fr.height * U / 100.0)
    val rule = if Clipart.evenOdd(art) then """ fill-rule="evenodd"""" else ""
    s"""<path transform="translate(${u(fr.x)} ${u(fr.y)}) scale($sx $sy)" d="${Clipart.path(art)}" fill="${color.hex}"$rule/>"""

  // ── Calendar grid ────────────────────────────────────────────────────────

  private def renderCalendar(fr: Placement, spec: CalendarSpec): String =
    val face      = FontCatalog.get(spec.fontKey)
    val family    = escapeXml(face.cssFamily)
    val rows      = CalendarMath.monthGrid(spec)
    val cols      = if spec.showWeekNumbers then 8 else 7
    val headerH   = if spec.showHeader then fr.height * 0.2 else 0.0
    val weekdayH  = if spec.showWeekdays then fr.height * 0.1 else 0.0
    val gridH     = fr.height - headerH - weekdayH
    val rowH      = gridH / math.max(1, rows.size)
    val colW      = fr.width / cols
    val dayOffset = if spec.showWeekNumbers then 1 else 0
    val sb        = new StringBuilder
    // Transparent backing rect keeps the whole grid clickable in the editor.
    sb.append(s"""<rect x="${u(fr.x)}" y="${u(fr.y)}" width="${u(fr.width)}" height="${u(fr.height)}" fill="transparent"/>""")
    if spec.showHeader then
      val fs = math.min(headerH * 0.62, fr.width * 0.09)
      val title = s"${CalendarMath.monthName(spec.month, spec.language)} ${spec.year}"
      sb.append(
        s"""<text x="${u(fr.x + colW * 0.15)}" y="${u(fr.y + headerH * 0.5)}" dominant-baseline="central" font-family="$family" font-size="${u(fs)}" font-weight="${face.nearestWeight(700)}" fill="${spec.headerColor.hex}">${escapeXml(title)}</text>"""
      )
    val textSize = math.min(rowH * 0.42, colW * 0.42)
    if spec.showWeekdays then
      val y = fr.y + headerH + weekdayH * 0.5
      (0 until 7).foreach { c =>
        val dow     = CalendarMath.weekdayAt(spec.weekStart, c)
        val weekend = dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY
        val x       = fr.x + (c + dayOffset + 0.5) * colW
        sb.append(
          s"""<text x="${u(x)}" y="${u(y)}" text-anchor="middle" dominant-baseline="central" font-family="$family" font-size="${u(textSize * 0.72)}" font-weight="${face.nearestWeight(600)}" fill="${(if weekend then spec.weekendColor else spec.mutedColor).hex}">${escapeXml(CalendarMath.weekdayLabel(spec.weekStart, c, spec.language))}</text>"""
        )
      }
    val gridTop = fr.y + headerH + weekdayH
    rows.zipWithIndex.foreach { case (row, r) =>
      val cy = gridTop + (r + 0.5) * rowH
      if spec.showWeekNumbers then
        sb.append(
          s"""<text x="${u(fr.x + colW * 0.5)}" y="${u(cy)}" text-anchor="middle" dominant-baseline="central" font-family="$family" font-size="${u(textSize * 0.6)}" fill="${spec.mutedColor.hex}">${row.isoWeek}</text>"""
        )
      row.cells.zipWithIndex.foreach { case (cell, c) =>
        val x = fr.x + (c + dayOffset) * colW
        if spec.showGrid then
          sb.append(
            s"""<rect x="${u(x)}" y="${u(gridTop + r * rowH)}" width="${u(colW)}" height="${u(rowH)}" fill="none" stroke="${spec.mutedColor.hex}" stroke-width="${u(0.2)}"/>"""
          )
        cell.day.foreach { d =>
          val color = if cell.holiday || cell.weekend then spec.weekendColor else spec.textColor
          val (tx, anchor, baseline, ty) =
            if spec.showGrid then (x + colW * 0.12, "start", "hanging", gridTop + r * rowH + rowH * 0.12)
            else (x + colW * 0.5, "middle", "central", cy)
          sb.append(
            s"""<text x="${u(tx)}" y="${u(ty)}" text-anchor="$anchor" dominant-baseline="$baseline" font-family="$family" font-size="${u(if spec.showGrid then textSize * 0.8 else textSize)}" font-weight="${face.nearestWeight(if cell.holiday then 700 else 500)}" fill="${color.hex}">$d</text>"""
          )
        }
      }
    }
    sb.toString

  // ── Data URLs ────────────────────────────────────────────────────────────

  private val Base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

  def base64Utf8(str: String): String =
    val bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    val sb    = new StringBuilder(((bytes.length + 2) / 3) * 4)
    var i     = 0
    while i < bytes.length do
      val b0 = bytes(i) & 0xff
      val b1 = if i + 1 < bytes.length then bytes(i + 1) & 0xff else 0
      val b2 = if i + 2 < bytes.length then bytes(i + 2) & 0xff else 0
      sb.append(Base64Chars.charAt(b0 >>> 2))
      sb.append(Base64Chars.charAt(((b0 & 0x03) << 4) | (b1 >>> 4)))
      sb.append(if i + 1 < bytes.length then Base64Chars.charAt(((b1 & 0x0f) << 2) | (b2 >>> 6)) else '=')
      sb.append(if i + 2 < bytes.length then Base64Chars.charAt(b2 & 0x3f) else '=')
      i += 3
    sb.toString

  def svgDataUrl(svg: String): String = s"data:image/svg+xml;base64,${base64Utf8(svg)}"

  /** Human page label ("Front", "March", "Page 4"). */
  def pageLabel(doc: DesignDocument, page: DesignPage, lang: Language): String =
    val idx = doc.pageIndex(page.id) + 1
    val cs  = lang == Language.Cs
    page.role match
      case PageRole.Single    => if cs then "Stránka" else "Page"
      case PageRole.Front     => if cs then "Líc" else "Front"
      case PageRole.Back      => if cs then "Rub" else "Back"
      case PageRole.Cover     => if cs then "Obálka" else "Cover"
      case PageRole.BackCover => if cs then "Zadní obálka" else "Back cover"
      case PageRole.Month(m)  => CalendarMath.monthName(m, lang)
      case PageRole.Inner     => if cs then s"Strana $idx" else s"Page $idx"
