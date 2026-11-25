package mpbuilder.editor.views

import kyo.*
import kyo.UI.*
import mpbuilder.design.*
import mpbuilder.editor.*
import mpbuilder.editor.views.EditorWidgets.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon

/** Right-hand property inspector: the selected element(s), or the page & design when nothing is selected. */
object InspectorView:

  def render(ctrl: EditorController): UI =
    ctrl.selectedElements.combineLatest(ctrl.language).render { case (selected, lang) =>
      UI.section.cssClass(if selected.isEmpty then "ve-inspector" else "ve-inspector ve-inspector-has-selection")(
        selected match
          case Nil       => pageInspector(ctrl, lang)
          case el :: Nil => elementInspector(ctrl, el, lang)
          case many      => multiInspector(ctrl, many)
      )
    }

  private def t(ctrl: EditorController, key: String): Signal[String] = ctrl.i18n.t(key)

  // ── Common ───────────────────────────────────────────────────────────────

  private def header(ctrl: EditorController, titleKey: String, el: Option[DesignElement]): UI =
    div.cssClass("ve-inspector-head")(
      h3(t(ctrl, titleKey)).cssClass("ve-panel-title"),
      div.cssClass("ve-row ve-row-tight")(
        el match
          case Some(e) =>
            iconToggle(if e.locked then Icon.Lock else Icon.Shield, if e.locked then "Unlock" else "Lock", e.locked, ctrl.updateSelected(x => x.copy(locked = !x.locked)))
          case None => UI.empty,
        toolButton(Icon.Copy, "Duplicate", ctrl.duplicateSelected),
        toolButton(Icon.Trash, "Delete", ctrl.deleteSelected),
        toolButton(Icon.Close, "Deselect", ctrl.clearSelection),
      ),
    )

  private def arrange(ctrl: EditorController): UI =
    panelSection(t(ctrl, "ve-insp-arrange"),
      div.cssClass("ve-row ve-row-wrap")(
        toolButton(Icon.ChevronLeft, "Align left", ctrl.alignSelected(AlignMode.Left)),
        toolButton(Icon.Minus, "Align centre", ctrl.alignSelected(AlignMode.CenterH)),
        toolButton(Icon.ChevronRight, "Align right", ctrl.alignSelected(AlignMode.Right)),
        toolButton(Icon.ChevronDown, "Align top", ctrl.alignSelected(AlignMode.Top)),
        toolButton(Icon.Grid, "Align middle", ctrl.alignSelected(AlignMode.Middle)),
        toolButton(Icon.ChevronDown, "Align bottom", ctrl.alignSelected(AlignMode.Bottom)),
      ),
      div.cssClass("ve-row ve-row-wrap")(
        button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.reorderSelected(LayerMove.ToFront))(t(ctrl, "ve-insp-to-front")),
        button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.reorderSelected(LayerMove.Forward))(t(ctrl, "ve-insp-forward")),
        button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.reorderSelected(LayerMove.Backward))(t(ctrl, "ve-insp-backward")),
        button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.reorderSelected(LayerMove.ToBack))(t(ctrl, "ve-insp-to-back")),
      ),
    )

  private def geometry(ctrl: EditorController, el: DesignElement): UI =
    val fr = el.frame
    panelSection(t(ctrl, "ve-insp-position"),
      div.cssClass("ve-grid2")(
        field(t(ctrl, "ve-insp-x"), number(fr.x, v => ctrl.setSelectedPlacement(_.copy(x = v)))),
        field(t(ctrl, "ve-insp-y"), number(fr.y, v => ctrl.setSelectedPlacement(_.copy(y = v)))),
        field(t(ctrl, "ve-insp-w"), number(fr.width, v => ctrl.setSelectedPlacement(_.copy(width = v)), min = 0.5)),
        field(t(ctrl, "ve-insp-h"), number(fr.height, v => ctrl.setSelectedPlacement(_.copy(height = v)), min = 0.2)),
        field(t(ctrl, "ve-insp-rotation"), number(fr.rotation, v => ctrl.setSelectedPlacement(_.copy(rotation = DesignGeometry.normalizeAngle(v))), step = 1, min = -360, max = 360)),
        field(t(ctrl, "ve-insp-opacity"), slider(el.opacity * 100, 0, 100, 1, v => ctrl.updateSelected(_.copy(opacity = v / 100.0), Some("opacity")))),
      ),
    )

  // ── Element inspectors ───────────────────────────────────────────────────

  private def elementInspector(ctrl: EditorController, el: DesignElement, lang: Language): UI =
    val (titleKey, specific) = el.content match
      case c: ElementContent.Text     => ("ve-insp-text", textInspector(ctrl, el, c))
      case c: ElementContent.Image    => ("ve-insp-image", imageInspector(ctrl, el, c))
      case c: ElementContent.Shape    => ("ve-insp-shape", shapeInspector(ctrl, c))
      case c: ElementContent.Clipart  => ("ve-insp-clipart", clipartInspector(ctrl, c))
      case c: ElementContent.Calendar => ("ve-insp-calendar", calendarInspector(ctrl, c.spec, lang, document = false))
    div.cssClass("ve-stack")(
      header(ctrl, titleKey, Some(el)),
      if el.locked then p(t(ctrl, "ve-insp-locked-hint")).cssClass("ve-hint") else UI.empty,
      specific,
      geometry(ctrl, el),
      arrange(ctrl),
    )

  private def multiInspector(ctrl: EditorController, many: List[DesignElement]): UI =
    div.cssClass("ve-stack")(
      header(ctrl, "ve-insp-multi", None),
      p(ctrl.i18n.t("ve-insp-multi-count", Dict("count" -> many.size.toString))).cssClass("ve-hint"),
      arrange(ctrl),
    )

  private def textInspector(ctrl: EditorController, el: DesignElement, c: ElementContent.Text): UI =
    val st = c.style
    def setStyle(f: TextStyle => TextStyle, key: String): Unit < Async =
      ctrl.updateSelectedContent({
        case tx: ElementContent.Text => tx.copy(style = f(tx.style))
        case other                   => other
      }, Some(key))
    val face = FontCatalog.get(st.fontKey)
    div.cssClass("ve-stack")(
      textarea
        .cssClass("mp-input ve-textarea")
        .data(EditorBridge.TextInputAttr, el.id.value)
        .value(c.text)
        .onInput(v => ctrl.updateSelectedContent({
          case tx: ElementContent.Text => tx.copy(text = v)
          case other                   => other
        }, Some(s"text-${el.id.value}"))),
      field(t(ctrl, "ve-insp-font"),
        selectBox[String](FontCatalog.all.map(f => f.key -> f.family), st.fontKey, k => setStyle(s => s.copy(fontKey = k, weight = FontCatalog.get(k).nearestWeight(s.weight)), "font")),
      ),
      div.cssClass("ve-grid2")(
        field(t(ctrl, "ve-insp-size"), number(st.sizePt, v => setStyle(_.copy(sizePt = v), "size"), step = 1, min = 3, max = 400)),
        field(t(ctrl, "ve-insp-line-height"), number(st.lineHeight, v => setStyle(_.copy(lineHeight = v), "lh"), step = 0.05, min = 0.6, max = 4, decimals = 2)),
      ),
      div.cssClass("ve-row ve-row-wrap")(
        textToggle("B", "Bold", st.bold, setStyle(s => s.copy(weight = if s.bold then 400 else face.nearestWeight(700)), "bold"), "ve-tool-bold"),
        textToggle("I", "Italic", st.italic, setStyle(s => s.copy(italic = !s.italic), "italic"), "ve-tool-italic"),
        textToggle("U", "Underline", st.underline, setStyle(s => s.copy(underline = !s.underline), "underline"), "ve-tool-underline"),
        textToggle("Aa", "Uppercase", st.uppercase, setStyle(s => s.copy(uppercase = !s.uppercase), "upper")),
        span.cssClass("ve-divider"),
        textToggle("⇤", "Left", st.align == TextHAlign.Left, setStyle(_.copy(align = TextHAlign.Left), "align")),
        textToggle("↔", "Centre", st.align == TextHAlign.Center, setStyle(_.copy(align = TextHAlign.Center), "align")),
        textToggle("⇥", "Right", st.align == TextHAlign.Right, setStyle(_.copy(align = TextHAlign.Right), "align")),
        textToggle("☰", "Justify", st.align == TextHAlign.Justify, setStyle(_.copy(align = TextHAlign.Justify), "align")),
      ),
      field(t(ctrl, "ve-insp-valign"),
        selectBox[TextVAlign](
          List(TextVAlign.Top -> "↑", TextVAlign.Middle -> "↕", TextVAlign.Bottom -> "↓"),
          st.verticalAlign,
          v => setStyle(_.copy(verticalAlign = v), "valign"),
        ),
      ),
      field(t(ctrl, "ve-insp-letter-spacing"), slider(st.letterSpacingEm * 100, -10, 50, 1, v => setStyle(_.copy(letterSpacingEm = v / 100.0), "ls"))),
      panelSection(t(ctrl, "ve-insp-color"), colorPicker(Some(st.color), col => setStyle(_.copy(color = col), "color"))),
    )

  private def imageInspector(ctrl: EditorController, el: DesignElement, c: ElementContent.Image): UI =
    def setImage(f: ElementContent.Image => ElementContent.Image, key: String): Unit < Async =
      ctrl.updateSelectedContent({
        case img: ElementContent.Image => f(img)
        case other                     => other
      }, Some(key))
    ctrl.doc.render { d =>
      val asset = c.asset.flatMap(d.asset)
      val dpi   = asset.map(a => DesignGeometry.effectiveDpi(el.frame, c.crop, a.pixelWidth, a.pixelHeight))
      div.cssClass("ve-stack")(
        asset match
          case None =>
            div.cssClass("ve-callout")(
              p(t(ctrl, "ve-insp-image-empty")),
              button.cssClass("mp-btn mp-btn-primary mp-btn-sm").data(EditorBridge.UploadAttr, "true")(Icons(Icon.Upload, 14), span(t(ctrl, "ve-photos-upload"))),
            )
          case Some(a) =>
            div.cssClass("ve-row ve-row-between")(
              span(a.fileName).cssClass("ve-file-name"),
              dpi match
                case Some(v) if v < DesignPreflight.MinDpi         => span(s"$v DPI").cssClass("ve-pill ve-pill-bad")
                case Some(v) if v < DesignPreflight.RecommendedDpi => span(s"$v DPI").cssClass("ve-pill ve-pill-warn")
                case Some(v)                                       => span(s"$v DPI").cssClass("ve-pill ve-pill-good")
                case None                                          => UI.empty,
            ),
        if asset.isDefined then
          div.cssClass("ve-stack")(
            div.cssClass("ve-row")(
              textToggle(ctrl_t(ctrl, "Fill"), "Fill", c.crop.fit == ImageFit.Cover, setImage(i => i.copy(crop = i.crop.copy(fit = ImageFit.Cover)), "fit")),
              textToggle(ctrl_t(ctrl, "Fit"), "Fit", c.crop.fit == ImageFit.Contain, setImage(i => i.copy(crop = i.crop.copy(fit = ImageFit.Contain)), "fit")),
              button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(setImage(i => i.copy(asset = None), "clear"))(t(ctrl, "ve-insp-image-remove")),
            ),
            field(t(ctrl, "ve-insp-zoom"), slider(c.crop.zoom * 100, 100, 400, 5, v => setImage(i => i.copy(crop = i.crop.copy(zoom = v / 100.0)), "zoom"))),
            field(t(ctrl, "ve-insp-focus-x"), slider(c.crop.focusX * 100, 0, 100, 1, v => setImage(i => i.copy(crop = i.crop.copy(focusX = v / 100.0)), "fx"))),
            field(t(ctrl, "ve-insp-focus-y"), slider(c.crop.focusY * 100, 0, 100, 1, v => setImage(i => i.copy(crop = i.crop.copy(focusY = v / 100.0)), "fy"))),
            field(t(ctrl, "ve-insp-filter"),
              selectBox[ImageFilter](
                List(ImageFilter.Original -> "Original", ImageFilter.Grayscale -> "B&W", ImageFilter.Sepia -> "Sepia"),
                c.filter,
                f => setImage(_.copy(filter = f), "filter"),
              ),
            ),
          )
        else UI.empty,
        field(t(ctrl, "ve-insp-radius"), slider(c.cornerRadiusMm, 0, 30, 0.5, v => setImage(_.copy(cornerRadiusMm = v), "radius"))),
        panelSection(t(ctrl, "ve-insp-border"),
          div.cssClass("ve-row")(
            textToggle("—", "No border", c.stroke.isEmpty, setImage(_.copy(stroke = None), "border")),
            textToggle("▢", "Border", c.stroke.isDefined, setImage(i => i.copy(stroke = Some(i.stroke.getOrElse(Stroke(DesignColor.White, 1.5)))), "border")),
          ),
          c.stroke match
            case Some(s) =>
              div.cssClass("ve-stack")(
                field(t(ctrl, "ve-insp-border-width"), slider(s.widthMm, 0.2, 8, 0.1, v => setImage(i => i.copy(stroke = i.stroke.map(_.copy(widthMm = v))), "bw"))),
                colorPicker(Some(s.color), col => setImage(i => i.copy(stroke = i.stroke.map(_.copy(color = col))), "bc")),
              )
            case None => UI.empty,
        ),
      )
    }

  /** Static (non-reactive) label for compact toggle buttons. */
  private def ctrl_t(ctrl: EditorController, en: String): String = en

  private def shapeInspector(ctrl: EditorController, c: ElementContent.Shape): UI =
    def setShape(f: ElementContent.Shape => ElementContent.Shape, key: String): Unit < Async =
      ctrl.updateSelectedContent({
        case s: ElementContent.Shape => f(s)
        case other                   => other
      }, Some(key))
    val fillColor = c.fill match
      case Fill.Solid(col)          => Some(col)
      case Fill.Gradient(col, _, _) => Some(col)
      case Fill.NoFill              => None
    div.cssClass("ve-stack")(
      panelSection(t(ctrl, "ve-insp-fill"),
        div.cssClass("ve-row")(
          textToggle("∅", "No fill", c.fill == Fill.NoFill, setShape(_.copy(fill = Fill.NoFill), "fill")),
        ),
        colorPicker(fillColor, col => setShape(_.copy(fill = Fill.Solid(col)), "fill")),
      ),
      panelSection(t(ctrl, "ve-insp-border"),
        div.cssClass("ve-row")(
          textToggle("—", "No border", c.stroke.isEmpty, setShape(_.copy(stroke = None), "stroke")),
          textToggle("▢", "Border", c.stroke.isDefined, setShape(s => s.copy(stroke = Some(s.stroke.getOrElse(Stroke(DesignColor.Ink, 0.5)))), "stroke")),
          textToggle("┄", "Dashed", c.stroke.exists(_.dash == StrokeDash.Dashed), setShape(s => s.copy(stroke = Some(s.stroke.getOrElse(Stroke(DesignColor.Ink, 0.5)).copy(dash = StrokeDash.Dashed))), "stroke")),
        ),
        c.stroke match
          case Some(s) =>
            div.cssClass("ve-stack")(
              field(t(ctrl, "ve-insp-border-width"), slider(s.widthMm, 0.1, 6, 0.1, v => setShape(x => x.copy(stroke = x.stroke.map(_.copy(widthMm = v))), "sw"))),
              colorPicker(Some(s.color), col => setShape(x => x.copy(stroke = x.stroke.map(_.copy(color = col))), "sc")),
            )
          case None => UI.empty,
      ),
      if c.kind == ShapeKind.Rectangle then
        field(t(ctrl, "ve-insp-radius"), slider(c.cornerRadiusMm, 0, 40, 0.5, v => setShape(_.copy(cornerRadiusMm = v), "radius")))
      else UI.empty,
    )

  private def clipartInspector(ctrl: EditorController, c: ElementContent.Clipart): UI =
    panelSection(t(ctrl, "ve-insp-color"),
      colorPicker(Some(c.color), col =>
        ctrl.updateSelectedContent({
          case cl: ElementContent.Clipart => cl.copy(color = col)
          case other                      => other
        }, Some("clip-color"))
      )
    )

  /** Calendar settings; `document = true` applies to every calendar in the design (page inspector). */
  private def calendarInspector(ctrl: EditorController, spec: CalendarSpec, lang: Language, document: Boolean): UI =
    def update(f: CalendarSpec => CalendarSpec): Unit < Async =
      if document then ctrl.updateCalendars(f)
      else
        ctrl.updateSelectedContent({
          case ElementContent.Calendar(s) => ElementContent.Calendar(f(s))
          case other                      => other
        }, Some("calendar"))
    val months = (1 to 12).toList.map(m => m -> CalendarMath.monthName(m, lang))
    val years  = (spec.year - 2 to spec.year + 3).toList.map(y => y -> y.toString)
    div.cssClass("ve-stack")(
      div.cssClass("ve-grid2")(
        field(t(ctrl, if document then "ve-cal-start-month" else "ve-cal-month"),
          selectBox[Int](months, spec.month, m => if document then ctrl.setCalendarStart(spec.year, m) else update(_.copy(month = m)))),
        field(t(ctrl, "ve-cal-year"),
          selectBox[Int](years, spec.year, y => if document then ctrl.setCalendarStart(y, spec.month) else update(_.copy(year = y)))),
      ),
      div.cssClass("ve-grid2")(
        field(t(ctrl, "ve-cal-language"),
          selectBox[Language](List(Language.Cs -> "Čeština", Language.En -> "English"), spec.language, l => update(_.copy(language = l)))),
        field(t(ctrl, "ve-cal-week-start"),
          selectBox[WeekStart](List(WeekStart.Monday -> (if lang == Language.Cs then "Pondělí" else "Monday"), WeekStart.Sunday -> (if lang == Language.Cs then "Neděle" else "Sunday")), spec.weekStart, w => update(_.copy(weekStart = w)))),
      ),
      div.cssClass("ve-row ve-row-wrap")(
        textToggle("CZ", "Czech holidays", spec.holidays == HolidaySet.Czech, update(s => s.copy(holidays = if s.holidays == HolidaySet.Czech then HolidaySet.NoHolidays else HolidaySet.Czech))),
        textToggle("#", "Week numbers", spec.showWeekNumbers, update(s => s.copy(showWeekNumbers = !s.showWeekNumbers))),
        textToggle("▦", "Grid", spec.showGrid, update(s => s.copy(showGrid = !s.showGrid))),
        textToggle("Aa", "Header", spec.showHeader, update(s => s.copy(showHeader = !s.showHeader))),
      ),
      field(t(ctrl, "ve-insp-font"), selectBox[String](FontCatalog.all.map(f => f.key -> f.family), spec.fontKey, k => update(_.copy(fontKey = k)))),
      panelSection(t(ctrl, "ve-cal-accent"), colorPicker(Some(spec.headerColor), col => update(_.copy(headerColor = col)))),
      panelSection(t(ctrl, "ve-cal-weekend"), colorPicker(Some(spec.weekendColor), col => update(_.copy(weekendColor = col)))),
    )

  // ── Page & design inspector ──────────────────────────────────────────────

  private def pageInspector(ctrl: EditorController, lang: Language): UI =
    ctrl.doc.combineLatest(ctrl.pageId).render { case (d, pid) =>
      val page   = d.page(pid).getOrElse(d.pages.head)
      val f      = d.format
      val calendar = d.allElements.collectFirst { case DesignElement(_, _, ElementContent.Calendar(s), _, _, _) => s }
      div.cssClass("ve-stack")(
        div.cssClass("ve-inspector-head")(h3(DesignSvg.pageLabel(d, page, lang)).cssClass("ve-panel-title")),
        div.cssClass("ve-facts")(
          fact(t(ctrl, "ve-fact-format"), s"${fmt(f.trimWidthMm)} × ${fmt(f.trimHeightMm)} mm"),
          fact(t(ctrl, "ve-fact-bleed"), s"${fmt(f.bleedMm)} mm"),
          fact(t(ctrl, "ve-fact-safe"), s"${fmt(f.safeMarginMm)} mm"),
          fact(t(ctrl, "ve-fact-pages"), d.pages.size.toString),
        ),
        div.cssClass("ve-legend")(
          span.cssClass("ve-legend-swatch ve-legend-bleed"), span(t(ctrl, "ve-legend-bleed")),
          span.cssClass("ve-legend-swatch ve-legend-safe"), span(t(ctrl, "ve-legend-safe")),
        ),
        calendar match
          case Some(spec) => panelSection(t(ctrl, "ve-cal-settings"), calendarInspector(ctrl, spec, lang, document = true))
          case None       => UI.empty,
        checksPanel(ctrl, d, lang),
      )
    }

  private def fact(label: Signal[String], value: String): UI =
    div.cssClass("ve-fact")(span(label).cssClass("ve-fact-label"), span(value).cssClass("ve-fact-value"))

  def checksPanel(ctrl: EditorController, d: DesignDocument, lang: Language): UI =
    val issues = DesignPreflight.check(d)
    panelSection(t(ctrl, "ve-checks-title"),
      if issues.isEmpty then div.cssClass("ve-check ve-check-ok")(Icons(Icon.CheckCircle, 16), span(t(ctrl, "ve-checks-ok")))
      else
        div.cssClass("ve-stack ve-stack-tight")(
          issues.take(30).map { issue =>
            val page  = d.page(issue.pageId)
            val label = page.map(p => DesignSvg.pageLabel(d, p, lang)).getOrElse("")
            val tone = issue.severity match
              case IssueSeverity.Error   => "ve-check ve-check-error"
              case IssueSeverity.Warning => "ve-check ve-check-warn"
              case IssueSeverity.Info    => "ve-check ve-check-info"
            button
              .cssClass(tone)
              .onClick(ctrl.selectPage(issue.pageId).andThen(ctrl.select(issue.elementId.toList)))(
                Icons(if issue.severity == IssueSeverity.Error then Icon.XCircle else Icon.AlertTriangle, 15),
                span.cssClass("ve-check-text")(span(issueText(ctrl, issue.kind)), span(s" · $label").cssClass("ve-check-page")),
              )
          }*
        ),
    )

  def issueText(ctrl: EditorController, kind: IssueKind): Signal[String] = kind match
    case IssueKind.VeryLowResolution(dpi) => ctrl.i18n.t("ve-issue-very-low-res", Dict("dpi" -> dpi.toString))
    case IssueKind.LowResolution(dpi)     => ctrl.i18n.t("ve-issue-low-res", Dict("dpi" -> dpi.toString))
    case IssueKind.EmptyPhotoSlot         => ctrl.i18n.t("ve-issue-empty-slot")
    case IssueKind.OutsideSafeArea        => ctrl.i18n.t("ve-issue-safe-area")
    case IssueKind.ExtendToBleed          => ctrl.i18n.t("ve-issue-bleed")
    case IssueKind.InBindingZone          => ctrl.i18n.t("ve-issue-binding")
    case IssueKind.TextTooSmall(pt)       => ctrl.i18n.t("ve-issue-text-small", Dict("size" -> fmt(pt)))
    case IssueKind.EmptyText              => ctrl.i18n.t("ve-issue-empty-text")
    case IssueKind.MissingImage           => ctrl.i18n.t("ve-issue-missing-image")
    case IssueKind.NotUploaded            => ctrl.i18n.t("ve-issue-not-uploaded")
