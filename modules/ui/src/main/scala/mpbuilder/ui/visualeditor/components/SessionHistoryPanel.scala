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
        children <-- sessionsVar.signal.combineWith(lang).map { case (sessions, l) =>
          if sessions.isEmpty then
            List(div(cls := "empty-elements", l match
              case Language.En => "No saved sessions"
              case Language.Cs => "Zadne ulozene relace"
            ))
          else
            sessions.map { session =>
              renderSessionItem(session, sessionsVar, l)
            }
        }
      ),
    )
  }

  private def renderSessionItem(session: EditorSession, sessionsVar: Var[List[EditorSession]], lang: Language): Element = {
    val updatedStr = formatTimestamp(session.updatedAt)
    val fmt = session.editorState.productFormat

    div(
      cls := "session-item element-item",

      div(
        cls := "element-item-row",
        div(
          cls := "session-item-info",
          div(cls := "session-title", session.title),
          div(cls := "session-meta", s"${fmt.widthMm}x${fmt.heightMm}mm | ${session.editorState.pages.size} pages | $updatedStr"),
        ),
        div(
          cls := "element-actions",
          button(
            cls := "element-action-btn",
            title := (lang match { case Language.En => "Load"; case Language.Cs => "Nacist" }),
            ">>",
            onClick --> { _ =>
              VisualEditorViewModel.loadSession(session)
            }
          ),
          button(
            cls := "element-action-btn element-delete-btn",
            title := (lang match { case Language.En => "Delete"; case Language.Cs => "Smazat" }),
            "x",
            onClick --> { ev =>
              ev.stopPropagation()
              EditorSessionStore.delete(session.id, () => {
                EditorSessionStore.listAll(sessions => sessionsVar.set(sessions))
              })
            }
          ),
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
