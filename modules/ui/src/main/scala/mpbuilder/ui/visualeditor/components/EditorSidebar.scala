package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Vertical icon bar with collapsible content panels for the visual editor */
object EditorSidebar {

  private val sidebarTabVar: Var[Option[String]] = Var(None)

  private def sidebarIconButton(id: String, icon: String, tooltip: Signal[String]): Element =
    button(
      cls := "sidebar-icon-btn",
      cls <-- sidebarTabVar.signal.map(active => if active.contains(id) then "active" else ""),
      icon,
      title <-- tooltip,
      onClick --> { _ =>
        val current = sidebarTabVar.now()
        if current.contains(id) then sidebarTabVar.set(None)
        else sidebarTabVar.set(Some(id))
      },
    )

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "editor-sidebar",

      // Icon strip
      div(
        cls := "sidebar-icon-strip",
        sidebarIconButton("gallery", "🖼", lang.map {
          case Language.En => "Gallery"
          case Language.Cs => "Galerie"
        }),
        sidebarIconButton("cliparts", "🎨", lang.map {
          case Language.En => "Cliparts"
          case Language.Cs => "Kliparty"
        }),
        sidebarIconButton("background", "🌆", lang.map {
          case Language.En => "Background"
          case Language.Cs => "Pozadí"
        }),
        sidebarIconButton("history", "🕓", lang.map {
          case Language.En => "History"
          case Language.Cs => "Historie"
        }),
        sidebarIconButton("elements", "📐", lang.map {
          case Language.En => "Elements"
          case Language.Cs => "Prvky"
        }),
      ),

      // Collapsible panel content
      child.maybe <-- sidebarTabVar.signal.combineWith(lang).map { case (tab, language) =>
        tab.map { tabId =>
          val (panelTitle, panelContent) = tabId match {
            case "gallery" =>
              val t = language match { case Language.En => "Gallery"; case Language.Cs => "Galerie" }
              (t, ImageGalleryPanel())
            case "cliparts" =>
              val t = language match { case Language.En => "Cliparts"; case Language.Cs => "Kliparty" }
              (t, ClipartGalleryPanel())
            case "background" =>
              val t = language match { case Language.En => "Background"; case Language.Cs => "Pozadí" }
              (t, BackgroundEditor())
            case "history" =>
              val t = language match { case Language.En => "History"; case Language.Cs => "Historie" }
              (t, SessionHistoryPanel())
            case "elements" =>
              val t = language match { case Language.En => "Elements"; case Language.Cs => "Prvky" }
              (t, ElementListEditor())
            case _ =>
              ("", div())
          }
          div(
            cls := "sidebar-panel",
            div(
              cls := "sidebar-panel-header",
              span(cls := "sidebar-panel-title", panelTitle),
              button(
                cls := "sidebar-panel-close",
                "✕",
                title := "Close",
                onClick --> { _ => sidebarTabVar.set(None) },
              ),
            ),
            div(cls := "sidebar-panel-body", panelContent),
          )
        }
      },
    )
  }
}
