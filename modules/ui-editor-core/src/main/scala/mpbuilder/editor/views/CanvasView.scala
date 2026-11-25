package mpbuilder.editor.views

import kyo.*
import kyo.UI.*
import mpbuilder.design.*
import mpbuilder.editor.*

/** The interactive page canvas.
  *
  * Two stacked layers share one coordinate system (`DesignSvg.U` units per mm, bleed box as viewBox):
  *   1. **Art** — the page rendered by the shared [[DesignSvg]] renderer (exactly what previews and the print
  *      pipeline draw), injected as markup. Elements carry `data-ve-el` for hit-testing by the browser bridge.
  *   2. **Overlay** — Kyo SVG with bleed/safe/binding guides, snap lines, the selection box and its resize /
  *      rotate handles (`data-ve-handle`). It ignores pointer events except on the handles.
  */
object CanvasView:

  private val U = DesignSvg.U

  def render(ctrl: EditorController): UI =
    val artSignal =
      ctrl.doc
        .combineLatest(ctrl.pageId)
        .combineLatest(ctrl.assetUrls)
        .combineLatest(ctrl.language)
        .map { case (((d, pid), urls), lang) =>
          val page        = d.page(pid).getOrElse(d.pages.head)
          val placeholder = if lang == mpbuilder.shared.Language.Cs then "Klikněte pro fotku" else "Add a photo"
          DesignSvg.renderPage(d, page, RenderOptions(urls.get, RenderMode.Editor, idPrefix = "cv", placeholderLabel = placeholder))
        }

    val sizeSignal = ctrl.doc.map(_.format.bleedBox).combineLatest(ctrl.zoom)

    div
      .cssClass("ve-canvas")
      .data(EditorBridge.CanvasAttr, "true")(
        sizeSignal.render { case (box, zoom) =>
          div
            .cssClass("ve-page-wrap")
            .data(EditorBridge.PageAttr, s"${box.x} ${box.y} ${box.width} ${box.height}")
            .style(Style.width((box.width * zoom).px).height((box.height * zoom).px))(
              artSignal.render(svg => div.cssClass("ve-page-art")(UI.rawHtml(svg))),
              overlay(ctrl),
            )
        }
      )

  private final case class OverlayState(
      doc: DesignDocument,
      pageId: PageId,
      selection: List[ElementId],
      preview: List[GesturePreview],
      guides: List[SnapGuide],
      zoom: Double,
      showGuides: Boolean,
  ) derives CanEqual

  private def overlay(ctrl: EditorController): UI =
    val state =
      ctrl.doc
        .combineLatest(ctrl.pageId)
        .combineLatest(ctrl.selection)
        .combineLatest(ctrl.preview)
        .combineLatest(ctrl.guides)
        .combineLatest(ctrl.zoom)
        .combineLatest(ctrl.showGuides)
        .map { case ((((((d, pid), sel), prev), gd), z), sg) => OverlayState(d, pid, sel, prev, gd, z, sg) }

    state.render { st =>
      val f    = st.doc.format
      val vb   = f.bleedBox
      val px   = 1.0 / math.max(0.1, st.zoom) // one screen pixel in mm
      val page = st.doc.page(st.pageId).getOrElse(st.doc.pages.head)
      val frames: List[(DesignElement, Placement)] =
        st.selection.flatMap(id => page.element(id)).map { el =>
          el -> st.preview.find(_.id == el.id).map(_.current).getOrElse(el.frame)
        }
      Svg.svg
        .cssClass("ve-overlay")
        .viewBox(Svg.ViewBox(vb.x * U, vb.y * U, vb.width * U, vb.height * U))
        .preserveAspectRatio(Svg.PreserveAspectRatio(Svg.Align.XMidYMid))(
          (guidesLayer(f, st.showGuides, px) ++ snapLayer(st.guides, vb) ++ selectionLayer(frames, px))*
        )
    }

  private def rect(r: Rect): Svg.Rect =
    Svg.rect.x(r.x * U).y(r.y * U).width(r.width * U).height(r.height * U)

  private def guidesLayer(f: DesignFormat, show: Boolean, px: Double): List[Svg.SvgChild] =
    if !show then Nil
    else
      val b     = f.bleedBox
      val t     = f.trimBox
      val bleedShade =
        Svg.path
          .cssClass("ve-bleed-shade")
          .fillRule(Svg.FillRule.EvenOdd)
          .d(Svg.PathData.raw(
            s"M${b.x * U} ${b.y * U}h${b.width * U}v${b.height * U}h${-b.width * U}Z M${t.x * U} ${t.y * U}v${t.height * U}h${t.width * U}v${-t.height * U}Z"
          ))
      val binding = f.bindingZone.toList.map(z => rect(z).cssClass("ve-binding-zone"))
      List(
        bleedShade,
        rect(t).cssClass("ve-trim-line").fill(Svg.Paint.None),
        rect(f.safeBox).cssClass("ve-safe-line").fill(Svg.Paint.None).strokeDasharray(Seq(6.0, 4.0)),
      ) ++ binding

  private def snapLayer(guides: List[SnapGuide], vb: Rect): List[Svg.SvgChild] =
    guides.map {
      case SnapGuide(GuideAxis.Vertical, x) =>
        Svg.line.cssClass("ve-snap-line").x1(x * U).y1(vb.y * U).x2(x * U).y2(vb.bottom * U)
      case SnapGuide(GuideAxis.Horizontal, y) =>
        Svg.line.cssClass("ve-snap-line").x1(vb.x * U).y1(y * U).x2(vb.right * U).y2(y * U)
    }

  private def selectionLayer(frames: List[(DesignElement, Placement)], px: Double): List[Svg.SvgChild] =
    val outlines: List[Svg.SvgChild] = frames.map { case (el, fr) =>
      val pts = fr.corners.map(p => (p.x * U, p.y * U))
      Svg.polygon.cssClass(if el.locked then "ve-sel-outline ve-sel-locked" else "ve-sel-outline").points(Svg.Points.from(pts))
    }
    val handles: List[Svg.SvgChild] = frames match
      case (el, fr) :: Nil if !el.locked =>
        val size   = 9 * px
        val c      = fr.center
        val local  = (dx: Double, dy: Double) => DesignGeometry.rotateAround(Point(c.x + dx, c.y + dy), c, fr.rotation)
        val hw     = fr.width / 2
        val hh     = fr.height / 2
        val isLine = el.content match
          case ElementContent.Shape(ShapeKind.Line, _, _, _) => true
          case _                                             => false
        val positions = List(
          Handle.NW -> (-hw, -hh), Handle.N -> (0.0, -hh), Handle.NE -> (hw, -hh), Handle.E -> (hw, 0.0),
          Handle.SE -> (hw, hh), Handle.S -> (0.0, hh), Handle.SW -> (-hw, hh), Handle.W -> (-hw, 0.0),
        ).filter { case (h, _) => !isLine || h == Handle.E || h == Handle.W }
        val squares = positions.map { case (h, (dx, dy)) =>
          val p = local(dx, dy)
          Svg.rect
            .cssClass(s"ve-handle ve-handle-${h.toString.toLowerCase}")
            .data(EditorBridge.HandleAttr, h.toString.toLowerCase)
            .x((p.x - size / 2) * U)
            .y((p.y - size / 2) * U)
            .width(size * U)
            .height(size * U)
            .rx(2 * px * U)
            .transform(Svg.Transform.Rotate(fr.rotation, Present(p.x * U), Present(p.y * U)))
        }
        val stemTop = local(0, -hh - 26 * px)
        val stemBot = local(0, -hh)
        val rotate = List[Svg.SvgChild](
          Svg.line.cssClass("ve-rotate-stem").x1(stemBot.x * U).y1(stemBot.y * U).x2(stemTop.x * U).y2(stemTop.y * U),
          Svg.circle
            .cssClass("ve-handle ve-handle-rotate")
            .data(EditorBridge.HandleAttr, "rotate")
            .cx(stemTop.x * U)
            .cy(stemTop.y * U)
            .r(6 * px * U),
        )
        squares ++ rotate
      case _ => Nil
    outlines ++ handles
