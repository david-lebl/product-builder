package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.persistence.EditorSessionStore
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.{Language, ArtworkId}

/** Sidebar panel showing saved editor sessions */
object SessionHistoryPanel {
  def apply(): Element = {
    val sessionsVar: Var[List[EditorSession]] = Var(List.empty)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "session-history-section",

      // Load sessions on mount
      onMountCallback { _ =>
        EditorSessionStore.listAll(sessions => sessionsVar.set(sessions))
      },

      h4(child.text <-- lang.map {
        case Language.En => "Saved Sessions"
        case Language.Cs => "Ulozene relace"
      }),

      // New session button
      button(
        cls := "add-element-btn",
        child.text <-- lang.map {
          case Language.En => "+ New Session"
          case Language.Cs => "+ Nova relace"
        },
        onClick --> { _ =>
          val newId = ArtworkId.generate().value
          VisualEditorViewModel.reset()
          VisualEditorViewModel.startNewSession(newId)
        }
      ),

      // Session list
      div(
        cls := "session-list",
        children <-- sessionsVar.signal.combineWith(VisualEditorViewModel.currentSessionId, lang).map { (sessions: List[EditorSession], activeId: Option[String], l: Language) =>
          if sessions.isEmpty then
            List(div(cls := "empty-elements", l match
              case Language.En => "No saved sessions"
              case Language.Cs => "Zadne ulozene relace"
            ))
          else
            sessions.map { session =>
              renderSessionItem(session, sessionsVar, l, activeId)
            }
        }
      ),
    )
  }

  private def renderSessionItem(session: EditorSession, sessionsVar: Var[List[EditorSession]], lang: Language, activeSessionId: Option[String]): Element = {
    val updatedStr = formatTimestamp(session.updatedAt)
    val fmt = session.editorState.productFormat
    val isActive = activeSessionId.contains(session.id)
    val displayName = session.sessionName.filter(_.nonEmpty).getOrElse(session.title)
    val detail = s"${fmt.widthMm}x${fmt.heightMm}mm | ${session.editorState.pages.size} pages"

    div(
      cls := (if isActive then "session-tile session-tile-active" else "session-tile"),

      div(
        cls := "session-tile-content",
        div(
          cls := "session-tile-header",
          div(cls := "session-tile-name", displayName),
          if isActive then span(cls := "session-active-badge", lang match {
            case Language.En => "Active"
            case Language.Cs => "Aktivní"
          }) else emptyNode,
        ),
        div(cls := "session-tile-detail", detail),
        div(cls := "session-tile-time", updatedStr),
      ),
      div(
        cls := "session-tile-actions",
        button(
          cls := "session-btn-load",
          lang match { case Language.En => "Load"; case Language.Cs => "Načíst" },
          onClick --> { _ =>
            VisualEditorViewModel.loadSession(session)
          }
        ),
        button(
          cls := "session-btn-delete",
          lang match { case Language.En => "Delete"; case Language.Cs => "Smazat" },
          onClick --> { ev =>
            ev.stopPropagation()
            EditorSessionStore.delete(session.id, () => {
              EditorSessionStore.listAll(sessions => sessionsVar.set(sessions))
            })
          }
        ),
      ),
    )
  }

  private def formatTimestamp(ts: Double): String = {
    val now = System.currentTimeMillis().toDouble
    val diffSeconds = ((now - ts) / 1000).toInt
    if diffSeconds < 60 then "just now"
    else if diffSeconds < 3600 then s"${diffSeconds / 60}m ago"
    else if diffSeconds < 86400 then s"${diffSeconds / 3600}h ago"
    else s"${diffSeconds / 86400}d ago"
  }
}
