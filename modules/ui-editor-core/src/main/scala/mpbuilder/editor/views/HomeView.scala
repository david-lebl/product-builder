package mpbuilder.editor.views

import kyo.*
import kyo.UI.*
import mpbuilder.design.*
import mpbuilder.editor.*
import mpbuilder.editor.views.EditorWidgets.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon

/** "My designs" + "Start a new design" (template gallery). */
object HomeView:

  private val kinds: List[(DesignProductKind, Icon)] = List(
    DesignProductKind.BusinessCard -> Icon.CreditCard,
    DesignProductKind.Flyer        -> Icon.FileText,
    DesignProductKind.Poster       -> Icon.Image,
    DesignProductKind.Postcard     -> Icon.Mail,
    DesignProductKind.WallCalendar -> Icon.Calendar,
    DesignProductKind.DeskCalendar -> Icon.Calendar,
    DesignProductKind.PhotoBook    -> Icon.Book,
    DesignProductKind.Custom       -> Icon.Sparkles,
  )

  def render(ctrl: EditorController): UI =
    val launchKind = ctrl.launch.kind
    div.cssClass("ve-home")(
      header.cssClass("ve-home-head")(
        div.cssClass("ve-home-brand")(
          span(Icons(Icon.Palette, 20)).cssClass("ve-home-logo"),
          div(
            h1(ctrl.i18n.t("ve-home-title")).cssClass("ve-home-title"),
            p(
              ctrl.launch.title match
                case Some(title) => ctrl.i18n.t("ve-home-subtitle-product", Dict("product" -> title))
                case None        => ctrl.i18n.t("ve-home-subtitle")
            ).cssClass("ve-hint"),
          ),
        ),
        ctrl.host.onClose match
          case Some(cb) => button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(cb())(Icons(Icon.ArrowLeft, 15), span(ctrl.i18n.t("ve-back-to-shop")))
          case None     => Controls.languageSwitch(ctrl.i18n, l => ctrl.i18n.setLanguage(l)),
      ),
      launchFormatBanner(ctrl),
      myDesigns(ctrl, launchKind),
      newDesign(ctrl, launchKind),
    )

  private def launchFormatBanner(ctrl: EditorController): UI =
    ctrl.launch.format match
      case None => UI.empty
      case Some(f) =>
        div.cssClass("ve-callout ve-callout-brand")(
          Icons(Icon.Ruler, 16),
          span(ctrl.i18n.t("ve-home-format", Dict("w" -> fmt(f.trimWidthMm), "h" -> fmt(f.trimHeightMm), "bleed" -> fmt(f.bleedMm)))),
        )

  private def myDesigns(ctrl: EditorController, launchKind: Option[DesignProductKind]): UI =
    ctrl.designs.combineLatest(ctrl.language).render { case (all, lang) =>
      val relevant = launchKind match
        case Some(k) => all.filter(_.project.document.product.kind == k)
        case None    => all
      if relevant.isEmpty then UI.empty
      else
        UI.section.cssClass("ve-home-section")(
          h2(ctrl.i18n.t(if launchKind.isDefined then "ve-home-continue" else "ve-home-my-designs")).cssClass("ve-home-h2"),
          p(ctrl.i18n.t("ve-home-local-note")).cssClass("ve-hint"),
          div.cssClass("ve-card-grid")(
            relevant.take(24).map { local =>
              val pr   = local.project
              val doc  = pr.document
              div.cssClass("ve-design-card")(
                button.cssClass("ve-design-open").aria("label", pr.name).onClick(ctrl.openDesign(pr.id))(
                  pageArt(
                    DesignSvg.renderPage(doc, doc.pages.head, RenderOptions(_ => None, RenderMode.Preview, s"hm-${pr.id.value}", clipToTrim = true)),
                    "ve-card-art",
                  ),
                ),
                div.cssClass("ve-design-meta")(
                  div.cssClass("ve-stack-tight")(
                    span(pr.name).cssClass("ve-design-name"),
                    span(s"${DesignPresets.kindLabel(doc.product.kind)(lang)} · ${doc.pages.size} ${if lang == Language.Cs then "str." else "pp."} · ${formatDate(pr.updatedAt)}").cssClass("ve-hint"),
                  ),
                  div.cssClass("ve-row ve-row-tight")(
                    toolButton(Icon.Copy, "Duplicate", ctrl.duplicateDesign(pr.id)),
                    toolButton(Icon.Trash, "Delete", ctrl.deleteDesign(pr.id)),
                  ),
                ),
              )
            }*
          ),
        )
    }

  private def formatDate(i: java.time.Instant): String =
    val d = i.atOffset(java.time.ZoneOffset.UTC).toLocalDate
    s"${d.getDayOfMonth}. ${d.getMonthValue}. ${d.getYear}"

  private def newDesign(ctrl: EditorController, launchKind: Option[DesignProductKind]): UI =
    ctrl.homeKind.combineLatest(ctrl.language).render { case (selectedKind, lang) =>
      val kind      = selectedKind.getOrElse(DesignProductKind.BusinessCard)
      val templates = DesignTemplates.forKind(kind)
      UI.section.cssClass("ve-home-section")(
        h2(ctrl.i18n.t("ve-home-new")).cssClass("ve-home-h2"),
        if launchKind.isEmpty then
          div.cssClass("ve-kind-row")(
            kinds.map { case (k, icon) =>
              val active = k == kind
              button
                .cssClass(if active then "ve-kind ve-kind-active" else "ve-kind")
                .aria("pressed", active.toString)
                .onClick(ctrl.homeKind.set(Some(k)))(Icons(icon, 18), span(DesignPresets.kindLabel(k)(lang)))
            }*
          )
        else UI.empty,
        div.cssClass("ve-card-grid")(
          UI.fragment(templates.map { t =>
            val previewDoc = DesignTemplates.instantiate(t, TemplateContext(lang, 2027, format = ctrl.launch.format, pageCount = ctrl.launch.pageCount, product = ctrl.launch.product))
            button
              .cssClass("ve-template-tile")
              .onClick(ctrl.createFromTemplate(Some(t.key), kind))(
                pageArt(
                  DesignSvg.renderPage(previewDoc, previewDoc.pages.head, RenderOptions(_ => None, RenderMode.Preview, s"nt-${t.key}", clipToTrim = true)),
                  "ve-card-art",
                ),
                span(t.name(lang)).cssClass("ve-design-name"),
                span(t.description(lang)).cssClass("ve-hint"),
              )
          }*),
            button
              .cssClass("ve-template-tile ve-template-blank")
              .onClick(ctrl.createFromTemplate(None, kind))(
                div.cssClass("ve-blank-art")(Icons(Icon.Plus, 28)),
                span(ctrl.i18n.t("ve-home-blank")).cssClass("ve-design-name"),
                span(ctrl.i18n.t("ve-home-blank-hint")).cssClass("ve-hint"),
              ),
        ),
      )
    }
