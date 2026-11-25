package mpbuilder.design

import mpbuilder.shared.*

/** Inputs a template needs. `format` / `pageCount` come from the storefront configuration when the editor is
  * opened for a configured product; otherwise the product kind's default preset is used.
  */
final case class TemplateContext(
    language: Language,
    year: Int,
    startMonth: Int = 1,
    format: Option[DesignFormat] = None,
    pageCount: Option[Int] = None,
    product: Option[ProductBinding] = None,
)

final case class DesignTemplate(
    key: String,
    kind: DesignProductKind,
    name: LocalizedString,
    description: LocalizedString,
    accent: DesignColor,
    build: TemplateContext => DesignDocument,
)

/** Default physical formats & page policies per product kind (overridden by the configured product). */
object DesignPresets:

  def format(kind: DesignProductKind): DesignFormat = kind match
    case DesignProductKind.BusinessCard => DesignFormat(85, 55, bleedMm = 3, safeMarginMm = 4)
    case DesignProductKind.Flyer        => DesignFormat(148, 210, bleedMm = 3, safeMarginMm = 6)
    case DesignProductKind.Poster       => DesignFormat(297, 420, bleedMm = 3, safeMarginMm = 10)
    case DesignProductKind.Postcard     => DesignFormat(148, 105, bleedMm = 3, safeMarginMm = 5)
    case DesignProductKind.WallCalendar =>
      DesignFormat(210, 297, bleedMm = 3, safeMarginMm = 8, binding = Some(BindingGuide(BindingEdge.Top, 10)))
    case DesignProductKind.DeskCalendar =>
      DesignFormat(210, 148, bleedMm = 3, safeMarginMm = 6, binding = Some(BindingGuide(BindingEdge.Top, 9)))
    case DesignProductKind.PhotoBook => DesignFormat(210, 210, bleedMm = 3, safeMarginMm = 8)
    case DesignProductKind.Custom    => DesignFormat(210, 297, bleedMm = 3, safeMarginMm = 5)

  def pagePolicy(kind: DesignProductKind): PagePolicy = kind match
    case DesignProductKind.BusinessCard => PagePolicy(1, 2)
    case DesignProductKind.Flyer        => PagePolicy(1, 2)
    case DesignProductKind.Poster       => PagePolicy(1, 1)
    case DesignProductKind.Postcard     => PagePolicy(2, 2)
    case DesignProductKind.WallCalendar => PagePolicy(13, 13)
    case DesignProductKind.DeskCalendar => PagePolicy(13, 13)
    case DesignProductKind.PhotoBook    => PagePolicy(12, 60, step = 2)
    case DesignProductKind.Custom       => PagePolicy(1, 100)

  /** Maps a storefront category to the editor product kind (by category id / name keywords). */
  def kindForCategory(categoryId: String, pageCount: Option[Int] = None): DesignProductKind =
    val id = categoryId.toLowerCase
    if id.contains("business-card") then DesignProductKind.BusinessCard
    else if id.contains("calendar") then
      DesignProductKind.WallCalendar
    else if id.contains("postcard") then DesignProductKind.Postcard
    else if id.contains("flyer") then DesignProductKind.Flyer
    else if id.contains("poster") || id.contains("banner") || id.contains("roll-up") then DesignProductKind.Poster
    else if id.contains("booklet") || id.contains("photobook") || id.contains("photo-book") then DesignProductKind.PhotoBook
    else if pageCount.exists(_ > 2) then DesignProductKind.PhotoBook
    else DesignProductKind.Custom

  def kindLabel(kind: DesignProductKind): LocalizedString = kind match
    case DesignProductKind.BusinessCard => LocalizedString("Business card", "Vizitka")
    case DesignProductKind.Flyer        => LocalizedString("Flyer", "Leták")
    case DesignProductKind.Poster       => LocalizedString("Poster", "Plakát")
    case DesignProductKind.Postcard     => LocalizedString("Postcard", "Pohlednice")
    case DesignProductKind.WallCalendar => LocalizedString("Wall calendar", "Nástěnný kalendář")
    case DesignProductKind.DeskCalendar => LocalizedString("Desk calendar", "Stolní kalendář")
    case DesignProductKind.PhotoBook    => LocalizedString("Photo book", "Fotokniha")
    case DesignProductKind.Custom       => LocalizedString("Custom design", "Vlastní návrh")

/** Ready-made photo arrangements that can be applied to any page ("Layouts" panel). */
enum PageLayout derives CanEqual:
  case FullBleed, Framed, TwoColumns, TwoRows, Grid4, PhotoWithCaption

object PageLayouts:

  /** Photo-slot frames of a layout for the given format (mm). */
  def slots(layout: PageLayout, format: DesignFormat): List[Placement] =
    val w   = format.trimWidthMm
    val h   = format.trimHeightMm
    val b   = format.bleedMm
    val m   = format.safeMarginMm
    val gap = math.max(2.0, math.min(w, h) * 0.025)
    layout match
      case PageLayout.FullBleed => List(Placement(-b, -b, w + 2 * b, h + 2 * b))
      case PageLayout.Framed    => List(Placement(m, m, w - 2 * m, h - 2 * m))
      case PageLayout.TwoColumns =>
        val cw = (w - 2 * m - gap) / 2
        List(Placement(m, m, cw, h - 2 * m), Placement(m + cw + gap, m, cw, h - 2 * m))
      case PageLayout.TwoRows =>
        val rh = (h - 2 * m - gap) / 2
        List(Placement(m, m, w - 2 * m, rh), Placement(m, m + rh + gap, w - 2 * m, rh))
      case PageLayout.Grid4 =>
        val cw = (w - 2 * m - gap) / 2
        val rh = (h - 2 * m - gap) / 2
        for
          r <- List(0, 1)
          c <- List(0, 1)
        yield Placement(m + c * (cw + gap), m + r * (rh + gap), cw, rh)
      case PageLayout.PhotoWithCaption => List(Placement(m, m, w - 2 * m, (h - 2 * m) * 0.78))

  /** Caption frame for layouts that include one. */
  def caption(layout: PageLayout, format: DesignFormat): Option[Placement] = layout match
    case PageLayout.PhotoWithCaption =>
      val m = format.safeMarginMm
      val h = format.trimHeightMm
      Some(Placement(m, m + (h - 2 * m) * 0.82, format.trimWidthMm - 2 * m, (h - 2 * m) * 0.16))
    case _ => None

  /** Replaces the page's photo slots with the layout, keeping already placed photos (in order) and all other elements. */
  def apply(doc: DesignDocument, pageId: PageId, layout: PageLayout, language: Language): DesignDocument =
    doc.page(pageId) match
      case None => doc
      case Some(page) =>
        val existingImages = page.elements.collect { case DesignElement(_, _, img: ElementContent.Image, _, false, _) => img }
        val keptAssets     = existingImages.flatMap(_.asset)
        val others         = page.elements.filterNot(e => e.isImage && !e.locked)
        val frames         = slots(layout, doc.format)
        val ids            = DesignIds.freshElementIds(doc.copy(pages = doc.pages.map(p => if p.id == pageId then p.copy(elements = others) else p)), frames.size + 1, "slot")
        val photos = frames.zipWithIndex.map { case (fr, i) =>
          DesignElement(ids(i), fr, ElementContent.Image(keptAssets.lift(i)))
        }
        val captionEl = caption(layout, doc.format).filterNot(_ => others.exists(_.isText)).map { fr =>
          DesignElement(
            ids.last,
            fr,
            ElementContent.Text(
              if language == Language.Cs then "Popisek fotografie" else "Photo caption",
              TextStyle(fontKey = "lora", sizePt = math.max(8.0, Units.mmToPt(fr.height * 0.32)), italic = true, align = TextHAlign.Center, verticalAlign = TextVAlign.Middle),
            ),
          )
        }
        DesignOps.updatePage(doc, pageId)(_.copy(elements = photos.toVector ++ others ++ captionEl.toVector))

object DesignTemplates:

  private val Indigo    = DesignColor.unsafe("#4f46e5")
  private val DeepBlue  = DesignColor.unsafe("#1e1b4b")
  private val Coral     = DesignColor.unsafe("#f97316")
  private val Forest    = DesignColor.unsafe("#166534")
  private val Sand      = DesignColor.unsafe("#f5efe6")
  private val Muted     = DesignColor.unsafe("#6b7280")
  private val Ink       = DesignColor.Ink
  private val White     = DesignColor.White

  /** Per-template id allocation (deterministic → reproducible templates & tests). */
  private final class Ids(prefix: String):
    private var n = 0
    def next(): ElementId =
      n += 1
      ElementId.unsafe(s"$prefix-$n")

  private def tr(lang: Language)(en: String, cs: String): String = if lang == Language.Cs then cs else en

  /** Font size (pt) as a fraction of the page's short side — keeps templates proportional on any format. */
  private def sizeFor(format: DesignFormat, fraction: Double): Double =
    math.max(5.0, math.round(Units.mmToPt(math.min(format.trimWidthMm, format.trimHeightMm) * fraction) * 2) / 2.0)

  private def text(ids: Ids, frame: Placement, value: String, style: TextStyle): DesignElement =
    DesignElement(ids.next(), frame, ElementContent.Text(value, style))

  private def photo(ids: Ids, frame: Placement, radius: Double = 0.0): DesignElement =
    DesignElement(ids.next(), frame, ElementContent.Image(None, cornerRadiusMm = radius))

  private def box(ids: Ids, frame: Placement, fill: Fill, opacity: Double = 1.0, radius: Double = 0.0): DesignElement =
    DesignElement(ids.next(), frame, ElementContent.Shape(ShapeKind.Rectangle, fill, None, radius), opacity = opacity)

  private def art(ids: Ids, frame: Placement, kind: ClipartKind, color: DesignColor, opacity: Double = 1.0): DesignElement =
    DesignElement(ids.next(), frame, ElementContent.Clipart(kind, color), opacity = opacity)

  private def doc(ctx: TemplateContext, kind: DesignProductKind, pages: Vector[DesignPage]): DesignDocument =
    DesignDocument(
      formatVersion = DesignDocument.CurrentFormatVersion,
      product = ctx.product.getOrElse(ProductBinding(kind)),
      format = formatOf(ctx, kind),
      pagePolicy = DesignPresets.pagePolicy(kind),
      pages = pages,
    )

  private def formatOf(ctx: TemplateContext, kind: DesignProductKind): DesignFormat =
    ctx.format.getOrElse(DesignPresets.format(kind))

  private def pid(n: Int): PageId = PageId.unsafe(s"page-$n")

  // ── Business cards ───────────────────────────────────────────────────────

  private def businessCardClassic(ctx: TemplateContext): DesignDocument =
    val f    = formatOf(ctx, DesignProductKind.BusinessCard)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l    = ctx.language
    val i1   = Ids("front")
    val front = DesignPage(
      pid(1),
      PageRole.Front,
      Fill.Solid(White),
      Vector(
        box(i1, Placement(-b, -b, b + w * 0.06, h + 2 * b), Fill.Solid(Indigo)),
        text(i1, Placement(w * 0.14, h * 0.30, w * 0.80, h * 0.20), tr(l)("Jane Cooper", "Jana Nováková"),
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.13), weight = 700, color = Ink)),
        text(i1, Placement(w * 0.14, h * 0.51, w * 0.80, h * 0.13), tr(l)("Creative Director", "Kreativní ředitelka"),
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.075), weight = 500, color = Indigo, letterSpacingEm = 0.04, uppercase = true)),
        DesignElement(i1.next(), Placement(w * 0.14, h * 0.70, w * 0.18, 0.8), ElementContent.Shape(ShapeKind.Rectangle, Fill.Solid(Indigo))),
      ),
    )
    val i2 = Ids("back")
    val back = DesignPage(
      pid(2),
      PageRole.Back,
      Fill.Solid(Sand),
      Vector(
        text(i2, Placement(m + 2, h * 0.22, w - 2 * m - 4, h * 0.56), "+420 777 123 456\nhello@studio.example\nwww.studio.example",
          TextStyle(sizePt = sizeFor(f, 0.072), color = Ink, lineHeight = 1.6, verticalAlign = TextVAlign.Middle)),
        art(i2, Placement(w - m - h * 0.22, h * 0.39, h * 0.22, h * 0.22), ClipartKind.Sparkle, Indigo),
      ),
    )
    doc(ctx, DesignProductKind.BusinessCard, Vector(front, back))

  private def businessCardBold(ctx: TemplateContext): DesignDocument =
    val f    = formatOf(ctx, DesignProductKind.BusinessCard)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l    = ctx.language
    val i1   = Ids("front")
    val front = DesignPage(
      pid(1),
      PageRole.Front,
      Fill.Gradient(DeepBlue, Indigo, 35),
      Vector(
        art(i1, Placement(w * 0.62, -h * 0.25, h * 0.95, h * 0.95), ClipartKind.Circle, White, opacity = 0.08),
        text(i1, Placement(m + 1, h * 0.34, w * 0.78, h * 0.22), tr(l)("NORTHWIND", "SEVERKA"),
          TextStyle(fontKey = "bebas", sizePt = sizeFor(f, 0.2), color = White, letterSpacingEm = 0.08)),
        text(i1, Placement(m + 1, h * 0.58, w * 0.78, h * 0.12), tr(l)("Coffee roasters since 2009", "Pražírna kávy od roku 2009"),
          TextStyle(sizePt = sizeFor(f, 0.07), color = DesignColor.unsafe("#c7d2fe"))),
      ),
    )
    val i2 = Ids("back")
    val back = DesignPage(
      pid(2),
      PageRole.Back,
      Fill.Solid(White),
      Vector(
        box(i2, Placement(-b, h * 0.82, w + 2 * b, h * 0.18 + b), Fill.Solid(Indigo)),
        text(i2, Placement(m + 1, m + 1, w * 0.8, h * 0.2), tr(l)("Martin Black", "Martin Černý"),
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.1), weight = 700, color = DeepBlue)),
        text(i2, Placement(m + 1, h * 0.36, w * 0.8, h * 0.4), "+420 602 987 654\nmartin@northwind.example\nKarlova 12, Praha",
          TextStyle(sizePt = sizeFor(f, 0.068), color = Ink, lineHeight = 1.5)),
      ),
    )
    doc(ctx, DesignProductKind.BusinessCard, Vector(front, back))

  private def businessCardPhoto(ctx: TemplateContext): DesignDocument =
    val f    = formatOf(ctx, DesignProductKind.BusinessCard)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l    = ctx.language
    val i1   = Ids("front")
    val front = DesignPage(
      pid(1),
      PageRole.Front,
      Fill.Solid(White),
      Vector(
        photo(i1, Placement(-b, -b, w * 0.45 + b, h + 2 * b)),
        text(i1, Placement(w * 0.5, h * 0.3, w * 0.5 - m, h * 0.2), tr(l)("Eva Green", "Eva Zelená"),
          TextStyle(fontKey = "playfair", sizePt = sizeFor(f, 0.12), weight = 700, color = Ink)),
        text(i1, Placement(w * 0.5, h * 0.52, w * 0.5 - m, h * 0.3), tr(l)("Photographer\n+420 731 000 111", "Fotografka\n+420 731 000 111"),
          TextStyle(sizePt = sizeFor(f, 0.065), color = Muted, lineHeight = 1.45)),
      ),
    )
    val i2 = Ids("back")
    val back = DesignPage(
      pid(2),
      PageRole.Back,
      Fill.Solid(Ink),
      Vector(
        text(i2, Placement(m, h * 0.4, w - 2 * m, h * 0.2), "evagreen.example",
          TextStyle(fontKey = "playfair", sizePt = sizeFor(f, 0.09), italic = true, color = White, align = TextHAlign.Center, verticalAlign = TextVAlign.Middle)),
      ),
    )
    doc(ctx, DesignProductKind.BusinessCard, Vector(front, back))

  // ── Flyers & posters ─────────────────────────────────────────────────────

  private def flyerEvent(ctx: TemplateContext): DesignDocument =
    val kind = ctx.product.map(_.kind).filter(k => k == DesignProductKind.Poster || k == DesignProductKind.Flyer).getOrElse(DesignProductKind.Flyer)
    val f    = formatOf(ctx, kind)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l    = ctx.language
    val ids  = Ids("p1")
    val page = DesignPage(
      pid(1),
      PageRole.Single,
      Fill.Solid(White),
      Vector(
        photo(ids, Placement(-b, -b, w + 2 * b, h * 0.55 + b)),
        box(ids, Placement(m, h * 0.50, w * 0.42, h * 0.06), Fill.Solid(Coral), radius = 1.5),
        text(ids, Placement(m, h * 0.50, w * 0.42, h * 0.06), tr(l)("SAT 14 JUNE · 7 PM", "SO 14. ČERVNA · 19:00"),
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.03), weight = 700, color = White, align = TextHAlign.Center, verticalAlign = TextVAlign.Middle)),
        text(ids, Placement(m, h * 0.59, w - 2 * m, h * 0.17), tr(l)("Summer Night Concert", "Letní noční koncert"),
          TextStyle(fontKey = "oswald", sizePt = sizeFor(f, 0.085), weight = 700, color = Ink, lineHeight = 1.05, uppercase = true)),
        text(ids, Placement(m, h * 0.77, w - 2 * m, h * 0.12), tr(l)("Live music, food trucks and fireworks at the riverside park. Free entry.", "Živá hudba, food trucky a ohňostroj v parku u řeky. Vstup zdarma."),
          TextStyle(sizePt = sizeFor(f, 0.03), color = Muted, lineHeight = 1.45)),
        box(ids, Placement(-b, h * 0.93, w + 2 * b, h * 0.07 + b), Fill.Solid(Ink)),
        text(ids, Placement(m, h * 0.93, w - 2 * m, h * 0.07), "www.riverside-events.example",
          TextStyle(sizePt = sizeFor(f, 0.026), color = White, verticalAlign = TextVAlign.Middle)),
      ),
    )
    doc(ctx, kind, Vector(page))

  private def posterTypographic(ctx: TemplateContext): DesignDocument =
    val kind = ctx.product.map(_.kind).filter(k => k == DesignProductKind.Poster || k == DesignProductKind.Flyer).getOrElse(DesignProductKind.Poster)
    val f    = formatOf(ctx, kind)
    val (w, h, m) = (f.trimWidthMm, f.trimHeightMm, f.safeMarginMm)
    val l    = ctx.language
    val ids  = Ids("p1")
    val page = DesignPage(
      pid(1),
      PageRole.Single,
      Fill.Gradient(Coral, DesignColor.unsafe("#db2777"), 120),
      Vector(
        art(ids, Placement(w * 0.55, h * 0.06, w * 0.5, w * 0.5), ClipartKind.Sun, White, opacity = 0.18),
        text(ids, Placement(m, h * 0.30, w - 2 * m, h * 0.36), tr(l)("Big\nSummer\nSale", "Velký\nletní\nvýprodej"),
          TextStyle(fontKey = "bebas", sizePt = sizeFor(f, 0.2), color = White, lineHeight = 0.92)),
        text(ids, Placement(m, h * 0.70, w * 0.7, h * 0.08), tr(l)("Up to 50 % off everything in store", "Slevy až 50 % na vše v obchodě"),
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.04), weight = 600, color = White)),
        art(ids, Placement(w - m - w * 0.16, h - m - w * 0.16, w * 0.16, w * 0.16), ClipartKind.Badge, White),
        text(ids, Placement(w - m - w * 0.16, h - m - w * 0.16, w * 0.16, w * 0.16), "-50%",
          TextStyle(fontKey = "bebas", sizePt = sizeFor(f, 0.05), color = DesignColor.unsafe("#db2777"), align = TextHAlign.Center, verticalAlign = TextVAlign.Middle)),
      ),
    )
    doc(ctx, kind, Vector(page))

  private def posterPhoto(ctx: TemplateContext): DesignDocument =
    val kind = ctx.product.map(_.kind).filter(k => k == DesignProductKind.Poster || k == DesignProductKind.Flyer).getOrElse(DesignProductKind.Poster)
    val f    = formatOf(ctx, kind)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l    = ctx.language
    val ids  = Ids("p1")
    val page = DesignPage(
      pid(1),
      PageRole.Single,
      Fill.Solid(Ink),
      Vector(
        photo(ids, Placement(-b, -b, w + 2 * b, h + 2 * b)),
        box(ids, Placement(-b, h * 0.68, w + 2 * b, h * 0.32 + b), Fill.Gradient(DesignColor.unsafe("#111827"), DesignColor.Black, 90), opacity = 0.72),
        text(ids, Placement(m, h * 0.72, w - 2 * m, h * 0.12), tr(l)("Mountains are calling", "Hory volají"),
          TextStyle(fontKey = "playfair", sizePt = sizeFor(f, 0.075), weight = 700, color = White)),
        text(ids, Placement(m, h * 0.85, w - 2 * m, h * 0.08), tr(l)("Photo exhibition · Gallery North · May 2–30", "Výstava fotografií · Galerie Sever · 2.–30. května"),
          TextStyle(sizePt = sizeFor(f, 0.03), color = DesignColor.unsafe("#e5e7eb"), letterSpacingEm = 0.02)),
      ),
    )
    doc(ctx, kind, Vector(page))

  // ── Postcards ────────────────────────────────────────────────────────────

  private def postcardPhoto(ctx: TemplateContext): DesignDocument =
    val f    = formatOf(ctx, DesignProductKind.Postcard)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l    = ctx.language
    val i1   = Ids("front")
    val front = DesignPage(
      pid(1),
      PageRole.Front,
      Fill.Solid(White),
      Vector(
        photo(i1, Placement(-b, -b, w + 2 * b, h + 2 * b)),
        text(i1, Placement(m, h - m - h * 0.16, w - 2 * m, h * 0.16), tr(l)("Greetings from Prague", "Pozdrav z Prahy"),
          TextStyle(fontKey = "dancing", sizePt = sizeFor(f, 0.12), weight = 700, color = White)),
      ),
    )
    val i2 = Ids("back")
    val line = (y: Double) =>
      DesignElement(i2.next(), Placement(w * 0.56, y, w * 0.44 - m, 0.35), ElementContent.Shape(ShapeKind.Rectangle, Fill.Solid(DesignColor.unsafe("#9ca3af"))))
    val back = DesignPage(
      pid(2),
      PageRole.Back,
      Fill.Solid(White),
      Vector(
        text(i2, Placement(m, m, w * 0.48 - m, h - 2 * m), tr(l)("Dear friends,\nwrite your message here…", "Milí přátelé,\nsem napište svůj vzkaz…"),
          TextStyle(fontKey = "caveat", sizePt = sizeFor(f, 0.075), color = Ink, lineHeight = 1.35)),
        DesignElement(i2.next(), Placement(w * 0.52, m, 0.35, h - 2 * m), ElementContent.Shape(ShapeKind.Rectangle, Fill.Solid(DesignColor.unsafe("#d1d5db")))),
        DesignElement(i2.next(), Placement(w - m - h * 0.22, m, h * 0.18, h * 0.22), ElementContent.Shape(ShapeKind.Rectangle, Fill.NoFill, Some(Stroke(DesignColor.unsafe("#9ca3af"), 0.35, StrokeDash.Dashed)))),
        line(h * 0.5),
        line(h * 0.62),
        line(h * 0.74),
      ),
    )
    doc(ctx, DesignProductKind.Postcard, Vector(front, back))

  // ── Calendars ────────────────────────────────────────────────────────────

  private def monthSequence(ctx: TemplateContext, count: Int): List[(Int, Int)] =
    (0 until count).toList.map { i =>
      val total = (ctx.startMonth - 1) + i
      (ctx.year + total / 12, total % 12 + 1)
    }

  private def calendarSpec(ctx: TemplateContext, year: Int, month: Int, accent: DesignColor, grid: Boolean): CalendarSpec =
    CalendarSpec(
      year = year,
      month = month,
      language = ctx.language,
      weekStart = if ctx.language == Language.Cs then WeekStart.Monday else WeekStart.Monday,
      holidays = if ctx.language == Language.Cs then HolidaySet.Czech else HolidaySet.NoHolidays,
      showGrid = grid,
      fontKey = "montserrat",
      headerColor = accent,
      weekendColor = DesignColor.unsafe("#dc2626"),
    )

  private def wallCalendar(ctx: TemplateContext, minimal: Boolean): DesignDocument =
    val f      = formatOf(ctx, DesignProductKind.WallCalendar)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l      = ctx.language
    val accent = if minimal then Forest else Indigo
    val topPad = f.binding.map(_.depthMm).getOrElse(0.0)
    val ic     = Ids("cover")
    val cover = DesignPage(
      pid(1),
      PageRole.Cover,
      Fill.Solid(if minimal then Sand else Ink),
      if minimal then
        Vector(
          photo(ic, Placement(m, topPad + m, w - 2 * m, h * 0.62)),
          text(ic, Placement(m, h * 0.74, w - 2 * m, h * 0.1), ctx.year.toString,
            TextStyle(fontKey = "playfair", sizePt = sizeFor(f, 0.12), weight = 700, color = accent, align = TextHAlign.Center)),
          text(ic, Placement(m, h * 0.85, w - 2 * m, h * 0.05), tr(l)("Family calendar", "Rodinný kalendář"),
            TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.035), color = Muted, align = TextHAlign.Center, letterSpacingEm = 0.1, uppercase = true)),
        )
      else
        Vector(
          photo(ic, Placement(-b, -b, w + 2 * b, h + 2 * b)),
          box(ic, Placement(-b, h * 0.74, w + 2 * b, h * 0.26 + b), Fill.Solid(Ink), opacity = 0.55),
          text(ic, Placement(m, h * 0.77, w - 2 * m, h * 0.12), ctx.year.toString,
            TextStyle(fontKey = "bebas", sizePt = sizeFor(f, 0.2), color = White)),
          text(ic, Placement(m, h * 0.9, w - 2 * m, h * 0.05), tr(l)("Our year in pictures", "Náš rok ve fotkách"),
            TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.04), weight = 500, color = White)),
        ),
    )
    val months = monthSequence(ctx, 12).zipWithIndex.map { case ((year, month), i) =>
      val ids  = Ids(s"m${i + 1}")
      val spec = calendarSpec(ctx, year, month, accent, grid = minimal)
      val elements =
        if minimal then
          Vector(
            photo(ids, Placement(m, topPad + m, w - 2 * m, h * 0.5)),
            DesignElement(ids.next(), Placement(m + 2, h * 0.6, w - 2 * m - 4, h * 0.36), ElementContent.Calendar(spec)),
          )
        else
          Vector(
            photo(ids, Placement(-b, -b, w + 2 * b, h * 0.58 + b)),
            DesignElement(ids.next(), Placement(m + 2, h * 0.62, w - 2 * m - 4, h * 0.34), ElementContent.Calendar(spec)),
          )
      DesignPage(pid(i + 2), PageRole.Month(month), Fill.Solid(if minimal then Sand else White), elements)
    }
    doc(ctx, DesignProductKind.WallCalendar, (cover +: months).toVector)

  private def deskCalendar(ctx: TemplateContext): DesignDocument =
    val f      = formatOf(ctx, DesignProductKind.DeskCalendar)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l      = ctx.language
    val topPad = f.binding.map(_.depthMm).getOrElse(0.0)
    val ic     = Ids("cover")
    val cover = DesignPage(
      pid(1),
      PageRole.Cover,
      Fill.Solid(White),
      Vector(
        photo(ic, Placement(-b, -b, w * 0.6 + b, h + 2 * b)),
        text(ic, Placement(w * 0.64, h * 0.38, w * 0.36 - m, h * 0.2), ctx.year.toString,
          TextStyle(fontKey = "bebas", sizePt = sizeFor(f, 0.2), color = Coral)),
        text(ic, Placement(w * 0.64, h * 0.6, w * 0.36 - m, h * 0.12), tr(l)("Desk calendar", "Stolní kalendář"),
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.05), weight = 600, color = Ink)),
      ),
    )
    val months = monthSequence(ctx, 12).zipWithIndex.map { case ((year, month), i) =>
      val ids = Ids(s"m${i + 1}")
      DesignPage(
        pid(i + 2),
        PageRole.Month(month),
        Fill.Solid(White),
        Vector(
          photo(ids, Placement(-b, -b, w * 0.5 + b, h + 2 * b)),
          DesignElement(ids.next(), Placement(w * 0.54, topPad + m, w * 0.46 - m, h - topPad - 2 * m), ElementContent.Calendar(calendarSpec(ctx, year, month, Coral, grid = false))),
        ),
      )
    }
    doc(ctx, DesignProductKind.DeskCalendar, (cover +: months).toVector)

  // ── Photo book ───────────────────────────────────────────────────────────

  private def photoBook(ctx: TemplateContext): DesignDocument =
    val f      = formatOf(ctx, DesignProductKind.PhotoBook)
    val (w, h, b, m) = (f.trimWidthMm, f.trimHeightMm, f.bleedMm, f.safeMarginMm)
    val l      = ctx.language
    val policy = DesignPresets.pagePolicy(DesignProductKind.PhotoBook)
    val count  = ctx.pageCount.filter(policy.allows).getOrElse(policy.minPages)
    val ic     = Ids("cover")
    val cover = DesignPage(
      pid(1),
      PageRole.Cover,
      Fill.Solid(Sand),
      Vector(
        photo(ic, Placement(m, m, w - 2 * m, h * 0.68)),
        text(ic, Placement(m, h * 0.76, w - 2 * m, h * 0.1), tr(l)("Our Summer", "Naše léto"),
          TextStyle(fontKey = "playfair", sizePt = sizeFor(f, 0.075), weight = 700, color = Ink, align = TextHAlign.Center)),
        text(ic, Placement(m, h * 0.87, w - 2 * m, h * 0.05), ctx.year.toString,
          TextStyle(fontKey = "montserrat", sizePt = sizeFor(f, 0.035), color = Muted, align = TextHAlign.Center, letterSpacingEm = 0.2)),
      ),
    )
    val layouts = Vector(PageLayout.FullBleed, PageLayout.TwoColumns, PageLayout.Grid4, PageLayout.PhotoWithCaption, PageLayout.Framed, PageLayout.TwoRows)
    val base    = doc(ctx, DesignProductKind.PhotoBook, Vector(cover))
    val inner = (2 to count).foldLeft(base) { (acc, n) =>
      val page   = DesignPage(pid(n), PageRole.Inner, Fill.Solid(White))
      val withPg = acc.copy(pages = acc.pages :+ page)
      PageLayouts.apply(withPg, page.id, layouts((n - 2) % layouts.size), ctx.language)
    }
    inner

  // ── Blank ────────────────────────────────────────────────────────────────

  def blank(ctx: TemplateContext, kind: DesignProductKind): DesignDocument =
    val policy = DesignPresets.pagePolicy(kind)
    val count  = ctx.pageCount.map(c => math.max(policy.minPages, math.min(policy.maxPages, c))).getOrElse(policy.minPages)
    val roles: Int => PageRole = i =>
      if count == 1 then PageRole.Single
      else if count == 2 then (if i == 0 then PageRole.Front else PageRole.Back)
      else if i == 0 then PageRole.Cover
      else PageRole.Inner
    val pages = (0 until count).toVector.map(i => DesignPage(pid(i + 1), roles(i), Fill.Solid(White)))
    val d     = doc(ctx, kind, pages)
    // The configured product dictates the page count: widen the policy to accept it.
    if policy.allows(count) then d else d.copy(pagePolicy = PagePolicy(count, count))

  // ── Catalog ──────────────────────────────────────────────────────────────

  val all: List[DesignTemplate] = List(
    DesignTemplate("bc-classic", DesignProductKind.BusinessCard, LocalizedString("Classic", "Klasická"),
      LocalizedString("Clean white card with an accent stripe", "Čistá bílá vizitka s barevným pruhem"), Indigo, businessCardClassic),
    DesignTemplate("bc-bold", DesignProductKind.BusinessCard, LocalizedString("Bold", "Výrazná"),
      LocalizedString("Deep gradient front, crisp white back", "Tmavý přechod na líci, čistý rub"), DeepBlue, businessCardBold),
    DesignTemplate("bc-photo", DesignProductKind.BusinessCard, LocalizedString("Photo", "S fotkou"),
      LocalizedString("Portrait photo beside your name", "Portrétní fotka vedle jména"), Ink, businessCardPhoto),
    DesignTemplate("flyer-event", DesignProductKind.Flyer, LocalizedString("Event", "Akce"),
      LocalizedString("Photo header, date badge and details", "Fotka, štítek s datem a podrobnosti"), Coral, flyerEvent),
    DesignTemplate("poster-sale", DesignProductKind.Poster, LocalizedString("Typographic", "Typografický"),
      LocalizedString("Big headline on a vivid gradient", "Velký nadpis na výrazném přechodu"), DesignColor.unsafe("#db2777"), posterTypographic),
    DesignTemplate("poster-photo", DesignProductKind.Poster, LocalizedString("Full photo", "Celá fotka"),
      LocalizedString("Full-bleed photo with a caption band", "Fotka na spad s popiskem"), Ink, posterPhoto),
    DesignTemplate("postcard-photo", DesignProductKind.Postcard, LocalizedString("Holiday greetings", "Pozdrav z cest"),
      LocalizedString("Photo front, handwritten-style back", "Fotka na líci, rub s místem pro vzkaz"), Forest, postcardPhoto),
    DesignTemplate("wall-calendar-photo", DesignProductKind.WallCalendar, LocalizedString("Photo calendar", "Fotokalendář"),
      LocalizedString("Cover + 12 months, full-bleed photos", "Titulní strana + 12 měsíců, fotky na spad"), Indigo, wallCalendar(_, minimal = false)),
    DesignTemplate("wall-calendar-minimal", DesignProductKind.WallCalendar, LocalizedString("Minimal", "Minimalistický"),
      LocalizedString("Framed photos on warm paper with a grid", "Fotky v rámečku na teplém podkladu"), Forest, wallCalendar(_, minimal = true)),
    DesignTemplate("desk-calendar", DesignProductKind.DeskCalendar, LocalizedString("Desk calendar", "Stolní kalendář"),
      LocalizedString("Photo left, month right", "Fotka vlevo, měsíc vpravo"), Coral, deskCalendar),
    DesignTemplate("photobook-classic", DesignProductKind.PhotoBook, LocalizedString("Classic photo book", "Klasická fotokniha"),
      LocalizedString("Cover and varied photo layouts", "Obálka a různá rozvržení fotek"), Ink, photoBook),
  )

  def forKind(kind: DesignProductKind): List[DesignTemplate] =
    // Flyers & posters share layouts (they only differ in size).
    all.filter(t =>
      t.kind == kind ||
        (Set(DesignProductKind.Flyer, DesignProductKind.Poster).contains(kind) && Set(DesignProductKind.Flyer, DesignProductKind.Poster).contains(t.kind))
    )

  def find(key: String): Option[DesignTemplate] = all.find(_.key == key)

  /** Builds a template, re-targeted to the configured product's format when the context carries one. */
  def instantiate(template: DesignTemplate, ctx: TemplateContext): DesignDocument =
    val built = template.build(ctx)
    val bound = ctx.product match
      case Some(binding) => built.copy(product = binding)
      case None          => built
    // A single-sided configuration drops the template's back page (and fixes the page count).
    ctx.pageCount match
      case Some(n) if n >= 1 && n < bound.pages.size && bound.pagePolicy.step == 1 =>
        bound.copy(pages = bound.pages.take(n), pagePolicy = PagePolicy(n, n))
      case _ => bound
