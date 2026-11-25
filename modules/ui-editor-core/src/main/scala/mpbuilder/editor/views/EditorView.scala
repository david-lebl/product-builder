package mpbuilder.editor.views

import kyo.*
import kyo.UI.*
import mpbuilder.design.*
import mpbuilder.editor.*
import mpbuilder.editor.views.EditorWidgets.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon

/** The editing workspace: top bar, tool rail + panel, canvas, inspector and page strip. */
object EditorView:

  def render(ctrl: EditorController): UI =
    div.cssClass("ve-workspace")(
      topBar(ctrl),
      div.cssClass("ve-body")(
        SidePanels.rail(ctrl),
        SidePanels.panel(ctrl),
        div.cssClass("ve-stage")(
          CanvasView.render(ctrl),
          pageStrip(ctrl),
        ),
        InspectorView.render(ctrl),
      ),
      reviewDialog(ctrl),
    )

  // ── Top bar ──────────────────────────────────────────────────────────────

  private def topBar(ctrl: EditorController): UI =
    header.cssClass("ve-topbar")(
      button
        .cssClass("mp-btn mp-btn-ghost mp-btn-sm ve-back")
        .onClick(if ctrl.host.onClose.isDefined then ctrl.close else ctrl.goHome)(
          Icons(Icon.ArrowLeft, 16),
          span(ctrl.i18n.t(if ctrl.host.onClose.isDefined then "ve-back-to-shop" else "ve-my-designs")).cssClass("ve-hide-sm"),
        ),
      ctrl.session.render {
        case None => (UI.empty: UI)
        case Some(s) =>
          input
            .cssClass("ve-name-input")
            .aria("label", "Design name")
            .value(s.name)
            .onChange(v => ctrl.rename(v))
      },
      ctrl.doc.combineLatest(ctrl.language).render { case (d, lang) =>
        span(s"${DesignPresets.kindLabel(d.product.kind)(lang)} · ${fmt(d.format.trimWidthMm)} × ${fmt(d.format.trimHeightMm)} mm").cssClass("ve-format-chip ve-hide-md")
      },
      div.cssClass("ve-topbar-spacer"),
      ctrl.canUndo.combineLatest(ctrl.canRedo).render { case (u, r) =>
        div.cssClass("ve-row ve-row-tight")(
          toolButton(Icon.ArrowLeft, "Undo (Ctrl+Z)", ctrl.undo, disabled = !u),
          toolButton(Icon.ArrowRight, "Redo (Ctrl+Shift+Z)", ctrl.redo, disabled = !r),
        )
      },
      ctrl.zoom.combineLatest(ctrl.zoomMode).render { case (z, mode) =>
        div.cssClass("ve-zoom ve-hide-sm")(
          toolButton(Icon.Minus, "Zoom out", ctrl.zoomBy(1 / 1.2)),
          button.cssClass(if mode == ZoomMode.Fit then "ve-zoom-value ve-zoom-fit" else "ve-zoom-value").onClick(ctrl.applyFit)(s"${math.round(z / 3.7795 * 100)} %"),
          toolButton(Icon.Plus, "Zoom in", ctrl.zoomBy(1.2)),
        )
      },
      ctrl.showGuides.render { g =>
        iconToggle(Icon.Ruler, "Guides", g, ctrl.showGuides.set(!g))
      },
      saveIndicator(ctrl),
      button
        .cssClass("mp-btn mp-btn-primary ve-done")
        .onClick(ctrl.openReview)(
          Icons(Icon.Check, 16),
          span(ctrl.i18n.t(if ctrl.host.onComplete.isDefined then ctrl.host.completeLabelKey else "ve-preview")),
        ),
    )

  private def saveIndicator(ctrl: EditorController): UI =
    ctrl.saveStatus.render {
      case SaveStatus.Idle      => UI.empty
      case SaveStatus.Dirty     => span(ctrl.i18n.t("ve-status-unsaved")).cssClass("ve-status ve-status-dirty ve-hide-sm")
      case SaveStatus.Saving    => span(ctrl.i18n.t("ve-status-saving")).cssClass("ve-status ve-status-saving ve-hide-sm")
      case SaveStatus.Saved     => span(Icons(Icon.Check, 13), span(ctrl.i18n.t("ve-status-saved"))).cssClass("ve-status ve-status-saved ve-hide-sm")
      case SaveStatus.Failed(_) => span(ctrl.i18n.t("ve-status-failed")).cssClass("ve-status ve-status-failed")
    }

  // ── Page strip ───────────────────────────────────────────────────────────

  private def pageStrip(ctrl: EditorController): UI =
    div.cssClass("ve-pagestrip")(
      ctrl.doc.combineLatest(ctrl.pageId).combineLatest(ctrl.assetUrls).combineLatest(ctrl.language).render { case (((d, pid), urls), lang) =>
        div.cssClass("ve-thumbs")(
          d.pages.toList.map { page =>
            val active = page.id == pid
            button
              .cssClass(if active then "ve-thumb ve-thumb-active" else "ve-thumb")
              .aria("label", DesignSvg.pageLabel(d, page, lang))
              .onClick(ctrl.selectPage(page.id))(
                div
                  .cssClass("ve-art ve-thumb-art")
                  .style(Style.width((66 * d.format.trimWidthMm / d.format.trimHeightMm).px))(
                    UI.rawHtml(DesignSvg.renderPage(d, page, RenderOptions(urls.get, RenderMode.Preview, s"th-${page.id.value}", clipToTrim = true)))
                  ),
                span(DesignSvg.pageLabel(d, page, lang)).cssClass("ve-thumb-label"),
              )
          }*
        )
      },
      ctrl.doc.render { d =>
        val policy = d.pagePolicy
        if policy.isFixed then (UI.empty: UI)
        else
          div.cssClass("ve-page-actions")(
            toolButton(Icon.Plus, "Add page", ctrl.addPage, disabled = !policy.canAdd(d.pages.size)),
            if policy.step == 1 then toolButton(Icon.Copy, "Duplicate page", ctrl.duplicatePage, disabled = !policy.canAdd(d.pages.size)) else UI.empty,
            if policy.step == 1 then toolButton(Icon.ChevronLeft, "Move page left", ctrl.movePage(-1)) else UI.empty,
            if policy.step == 1 then toolButton(Icon.ChevronRight, "Move page right", ctrl.movePage(1)) else UI.empty,
            toolButton(Icon.Trash, "Delete page", ctrl.removePage, disabled = !policy.canRemove(d.pages.size)),
          )
      },
    )

  // ── Review ("proof") dialog ──────────────────────────────────────────────

  private def reviewDialog(ctrl: EditorController): UI =
    ctrl.reviewOpen.render {
      case false => (UI.empty: UI)
      case true =>
        ctrl.doc.combineLatest(ctrl.assetUrls).combineLatest(ctrl.language).render { case ((d, urls), lang) =>
          val issues  = DesignPreflight.check(d)
          val errors  = issues.count(_.severity == IssueSeverity.Error)
          val warns   = issues.count(_.severity == IssueSeverity.Warning)
          val proof   = DesignSvg.renderPage(d, d.pages.head, RenderOptions(urls.get, RenderMode.Preview, "dl", clipToTrim = true))
          div.cssClass("mp-modal-backdrop ve-review-backdrop")(
            div.cssClass("mp-modal ve-review").role("dialog").aria("modal", "true")(
              div.cssClass("ve-review-head")(
                h2(ctrl.i18n.t("ve-review-title")).cssClass("ve-review-title"),
                button.cssClass("mp-icon-btn mp-icon-btn-sm").aria("label", "Close").onClick(ctrl.reviewOpen.set(false))(Icons(Icon.Close, 16)),
              ),
              p(ctrl.i18n.t("ve-review-subtitle")).cssClass("ve-hint"),
              div.cssClass("ve-review-pages")(
                d.pages.toList.map { page =>
                  div.cssClass("ve-review-page")(
                    pageArt(DesignSvg.renderPage(d, page, RenderOptions(urls.get, RenderMode.Preview, s"rv-${page.id.value}", clipToTrim = true)), "ve-review-art"),
                    span(DesignSvg.pageLabel(d, page, lang)).cssClass("ve-thumb-label"),
                  )
                }*
              ),
              if errors + warns == 0 then div.cssClass("ve-check ve-check-ok")(Icons(Icon.CheckCircle, 16), span(ctrl.i18n.t("ve-checks-ok")))
              else InspectorView.checksPanel(ctrl, d, lang),
              p(ctrl.i18n.t("ve-review-colour-note")).cssClass("ve-hint"),
              div.cssClass("ve-review-actions")(
                button
                  .cssClass("mp-btn mp-btn-secondary")
                  .data("ve-download", DesignSvg.svgDataUrl(proof))
                  .data("ve-download-name", "design-proof.svg")(Icons(Icon.Download, 15), span(ctrl.i18n.t("ve-review-download"))),
                div.cssClass("ve-topbar-spacer"),
                button.cssClass("mp-btn mp-btn-secondary").onClick(ctrl.reviewOpen.set(false))(ctrl.i18n.t("ve-review-back")),
                if ctrl.host.onComplete.isDefined then
                  button
                    .cssClass("mp-btn mp-btn-primary")
                    .onClick(ctrl.complete)(
                      Icons(Icon.Check, 16),
                      span(ctrl.i18n.t(if errors > 0 then "ve-review-confirm-anyway" else ctrl.host.completeLabelKey)),
                    )
                else UI.empty,
              ),
            )
          )
        }
    }
