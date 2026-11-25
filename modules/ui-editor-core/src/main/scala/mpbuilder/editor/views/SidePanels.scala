package mpbuilder.editor.views

import kyo.*
import kyo.UI.*
import mpbuilder.design.*
import mpbuilder.editor.*
import mpbuilder.editor.views.EditorWidgets.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon

/** Left rail + the panel of the active tool. */
object SidePanels:

  private val tabs: List[(LeftPanel, Icon, String)] = List(
    (LeftPanel.Templates, Icon.Layers, "ve-tab-templates"),
    (LeftPanel.Text, Icon.FileText, "ve-tab-text"),
    (LeftPanel.Photos, Icon.Image, "ve-tab-photos"),
    (LeftPanel.Elements, Icon.Star, "ve-tab-elements"),
    (LeftPanel.Background, Icon.Palette, "ve-tab-background"),
    (LeftPanel.Layouts, Icon.Grid, "ve-tab-layouts"),
  )

  def rail(ctrl: EditorController): UI =
    ctrl.panel.render { current =>
      nav.cssClass("ve-rail").aria("label", "Tools")(
        tabs.map { case (p, icon, key) =>
          val active = current.contains(p)
          button
            .cssClass(if active then "ve-rail-btn ve-rail-btn-active" else "ve-rail-btn")
            .aria("pressed", active.toString)
            .onClick(ctrl.panel.set(if active then None else Some(p)))(
              Icons(icon, 20),
              span(ctrl.i18n.t(key)),
            )
        }*
      )
    }

  def panel(ctrl: EditorController): UI =
    ctrl.panel.render {
      case None => UI.empty
      case Some(p) =>
        aside.cssClass("ve-panel")(
          div.cssClass("ve-panel-head")(
            h3(ctrl.i18n.t(tabs.find(_._1 == p).map(_._3).getOrElse("ve-tab-text"))).cssClass("ve-panel-title"),
            button.cssClass("mp-icon-btn mp-icon-btn-sm ve-panel-close").aria("label", "Close").onClick(ctrl.panel.set(None))(Icons(Icon.Close, 15)),
          ),
          p match
            case LeftPanel.Templates  => templatesPanel(ctrl)
            case LeftPanel.Text       => textPanel(ctrl)
            case LeftPanel.Photos     => photosPanel(ctrl)
            case LeftPanel.Elements   => elementsPanel(ctrl)
            case LeftPanel.Background => backgroundPanel(ctrl)
            case LeftPanel.Layouts    => layoutsPanel(ctrl),
        )
    }

  private def aside = UI.section

  // ── Templates ────────────────────────────────────────────────────────────

  private def templatesPanel(ctrl: EditorController): UI =
    ctrl.doc.map(_.product.kind).combineLatest(ctrl.language).combineLatest(ctrl.confirmTemplate).render { case ((kind, lang), pending) =>
      val templates = DesignTemplates.forKind(kind)
      div.cssClass("ve-stack")(
        p(ctrl.i18n.t("ve-templates-hint")).cssClass("ve-hint"),
        pending match
          case Some(key) =>
            div.cssClass("ve-confirm")(
              p(ctrl.i18n.t("ve-templates-confirm")),
              div.cssClass("ve-row")(
                button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(ctrl.applyTemplate(key))(ctrl.i18n.t("ve-templates-apply")),
                button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.confirmTemplate.set(None))(ctrl.i18n.t("ve-cancel")),
              ),
            )
          case None => UI.empty,
        if templates.isEmpty then p(ctrl.i18n.t("ve-templates-none")).cssClass("ve-hint")
        else
          div.cssClass("ve-template-grid")(
            templates.map { t =>
              val docPreview = DesignTemplates.instantiate(t, TemplateContext(lang, 2027))
              button
                .cssClass("ve-template-card")
                .onClick(ctrl.confirmTemplate.set(Some(t.key)))(
                  pageArt(DesignSvg.renderPage(docPreview, docPreview.pages.head, RenderOptions(_ => None, RenderMode.Preview, s"tp-${t.key}", clipToTrim = true))),
                  span(t.name(lang)).cssClass("ve-template-name"),
                )
            }*
          ),
      )
    }

  // ── Text ─────────────────────────────────────────────────────────────────

  private def textPanel(ctrl: EditorController): UI =
    div.cssClass("ve-stack")(
      button.cssClass("ve-text-preset ve-text-preset-h").onClick(ctrl.addText(TextPreset.Heading))(ctrl.i18n.t("ve-text-add-heading")),
      button.cssClass("ve-text-preset ve-text-preset-s").onClick(ctrl.addText(TextPreset.Subheading))(ctrl.i18n.t("ve-text-add-subheading")),
      button.cssClass("ve-text-preset ve-text-preset-b").onClick(ctrl.addText(TextPreset.Body))(ctrl.i18n.t("ve-text-add-body")),
      button.cssClass("ve-text-preset ve-text-preset-script").onClick(ctrl.addText(TextPreset.Script))(ctrl.i18n.t("ve-text-add-script")),
      p(ctrl.i18n.t("ve-text-hint")).cssClass("ve-hint"),
    )

  // ── Photos ───────────────────────────────────────────────────────────────

  private def photosPanel(ctrl: EditorController): UI =
    div.cssClass("ve-stack")(
      button
        .cssClass("mp-btn mp-btn-primary ve-upload-btn")
        .data(EditorBridge.UploadAttr, "true")(Icons(Icon.Upload, 16), span(ctrl.i18n.t("ve-photos-upload"))),
      p(ctrl.i18n.t("ve-photos-hint")).cssClass("ve-hint"),
      ctrl.doc.map(_.assets).combineLatest(ctrl.assetUrls).combineLatest(ctrl.doc.map(_.usedAssetIds)).render { case ((assets, urls), used) =>
        if assets.isEmpty then div.cssClass("ve-empty-photos")(Icons(Icon.Image, 28), p(ctrl.i18n.t("ve-photos-empty")))
        else
          div.cssClass("ve-photo-grid")(
            assets.map { a =>
              val url = urls.get(a.id)
              button
                .cssClass("ve-photo")
                .data(EditorBridge.PhotoAttr, a.id.value)
                .aria("label", a.fileName)
                .onClick(ctrl.placePhoto(a.id))(
                  url match
                    case Some(u) => img(ImgSrc.Path(u), a.fileName).cssClass("ve-photo-img")
                    case None    => Icons(Icon.Image, 20),
                  if used.contains(a.id) then span(Icons(Icon.Check, 12)).cssClass("ve-photo-used") else UI.empty,
                  span(s"${a.pixelWidth}×${a.pixelHeight}").cssClass("ve-photo-meta"),
                )
            }*
          )
      },
      button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.addPhotoSlot)(Icons(Icon.Plus, 14), span(ctrl.i18n.t("ve-photos-add-slot"))),
    )

  // ── Elements (shapes, clipart, calendar) ─────────────────────────────────

  private def elementsPanel(ctrl: EditorController): UI =
    val shapes = List(
      ShapeKind.Rectangle -> "ve-shape-rect",
      ShapeKind.Ellipse   -> "ve-shape-ellipse",
      ShapeKind.Triangle  -> "ve-shape-triangle",
      ShapeKind.Line      -> "ve-shape-line",
    )
    div.cssClass("ve-stack")(
      panelSection(ctrl.i18n.t("ve-elements-shapes"),
        div.cssClass("ve-shape-grid")(
          shapes.map { case (kind, key) =>
            button.cssClass("ve-shape-btn").aria("label", key).onClick(ctrl.addShape(kind))(shapeGlyph(kind), span(ctrl.i18n.t(key)))
          }*
        ),
      ),
      panelSection(ctrl.i18n.t("ve-elements-clipart"),
        div.cssClass("ve-clipart-grid")(
          ClipartKind.values.toList.map { kind =>
            button
              .cssClass("ve-clipart-btn")
              .aria("label", kind.toString)
              .onClick(ctrl.addClipart(kind))(
                Svg.svg.viewBox(Svg.ViewBox(0, 0, 100, 100)).width(34).height(34)(
                  Svg.path.d(Svg.PathData.raw(Clipart.path(kind))).fill(Svg.Paint.CurrentColor).fillRule(if Clipart.evenOdd(kind) then Svg.FillRule.EvenOdd else Svg.FillRule.NonZero)
                )
              )
          }*
        ),
      ),
      panelSection(ctrl.i18n.t("ve-elements-calendar"),
        button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.addCalendar)(Icons(Icon.Calendar, 15), span(ctrl.i18n.t("ve-elements-add-calendar"))),
      ),
    )

  private def shapeGlyph(kind: ShapeKind): UI =
    val shape: Svg.SvgChild = kind match
      case ShapeKind.Rectangle => Svg.rect.x(4).y(8).width(32).height(24).rx(3).fill(Svg.Paint.CurrentColor)
      case ShapeKind.Ellipse   => Svg.ellipse.cx(20).cy(20).rx(16).ry(13).fill(Svg.Paint.CurrentColor)
      case ShapeKind.Triangle  => Svg.polygon.points(Svg.Points((20.0, 5.0), (36.0, 34.0), (4.0, 34.0))).fill(Svg.Paint.CurrentColor)
      case ShapeKind.Line      => Svg.line.x1(4).y1(20).x2(36).y2(20).stroke(Svg.Paint.CurrentColor).strokeWidth(3.0)
    Svg.svg.viewBox(Svg.ViewBox(0, 0, 40, 40)).width(30).height(30)(shape)

  // ── Background ───────────────────────────────────────────────────────────

  private val gradients: List[(String, String, Double)] = List(
    ("#1e1b4b", "#4f46e5", 35),
    ("#f97316", "#db2777", 120),
    ("#0284c7", "#14b8a6", 60),
    ("#fef3c7", "#fde68a", 90),
    ("#111827", "#374151", 90),
    ("#f5efe6", "#e7d8c2", 45),
  )

  private def backgroundPanel(ctrl: EditorController): UI =
    ctrl.currentPage.render { pageOpt =>
      val current = pageOpt.map(_.background) match
        case Some(Fill.Solid(c)) => Some(c)
        case _                   => None
      div.cssClass("ve-stack")(
        panelSection(ctrl.i18n.t("ve-bg-color"), colorPicker(current, c => ctrl.setBackground(Fill.Solid(c), allPages = false))),
        panelSection(ctrl.i18n.t("ve-bg-gradients"),
          div.cssClass("ve-gradient-grid")(
            gradients.map { case (from, to, angle) =>
              button
                .cssClass("ve-gradient-btn")
                .aria("label", s"$from → $to")
                .style(Style.bgGradient(Style.GradientDirection.toBottomRight, (Style.Color.hex(from).getOrElse(Style.Color.white), 0.pct), (Style.Color.hex(to).getOrElse(Style.Color.black), 100.pct)))
                .onClick(ctrl.setBackground(Fill.Gradient(DesignColor.unsafe(from), DesignColor.unsafe(to), angle), allPages = false))
            }*
          ),
        ),
        div.cssClass("ve-row")(
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(ctrl.setBackground(Fill.Solid(DesignColor.White), allPages = false))(ctrl.i18n.t("ve-bg-reset")),
          button
            .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
            .onClick(pageOpt.fold(Kyo.unit: Unit < Async)(p => ctrl.setBackground(p.background, allPages = true)))(ctrl.i18n.t("ve-bg-apply-all")),
        ),
      )
    }

  // ── Layouts ──────────────────────────────────────────────────────────────

  private def layoutsPanel(ctrl: EditorController): UI =
    val layouts = List(
      PageLayout.FullBleed        -> "ve-layout-full",
      PageLayout.Framed           -> "ve-layout-framed",
      PageLayout.TwoColumns       -> "ve-layout-two-cols",
      PageLayout.TwoRows          -> "ve-layout-two-rows",
      PageLayout.Grid4            -> "ve-layout-grid4",
      PageLayout.PhotoWithCaption -> "ve-layout-caption",
    )
    ctrl.doc.map(_.format).render { fmt =>
      div.cssClass("ve-stack")(
        p(ctrl.i18n.t("ve-layouts-hint")).cssClass("ve-hint"),
        div.cssClass("ve-layout-grid")(
          layouts.map { case (layout, key) =>
            val b     = fmt.bleedBox
            val slots = PageLayouts.slots(layout, fmt) ++ PageLayouts.caption(layout, fmt).toList
            val captionFrame = PageLayouts.caption(layout, fmt)
            button
              .cssClass("ve-layout-btn")
              .aria("label", key)
              .onClick(ctrl.applyLayout(layout))(
                Svg.svg.viewBox(Svg.ViewBox(b.x, b.y, b.width, b.height)).cssClass("ve-layout-glyph")(
                  (Svg.rect.x(b.x).y(b.y).width(b.width).height(b.height).cssClass("ve-layout-page") +:
                    slots.map { s =>
                      val isCaption = captionFrame.contains(s)
                      Svg.rect.x(s.x).y(s.y).width(s.width).height(s.height).cssClass(if isCaption then "ve-layout-caption" else "ve-layout-slot")
                    })*
                ),
                span(ctrl.i18n.t(key)),
              )
          }*
        ),
      )
    }
