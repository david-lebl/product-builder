package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.EditorSession
import mpbuilder.domain.model.Language
import org.scalajs.dom

/** Modal popup shown when opening the editor standalone with existing sessions in IndexedDB.
 *  Reuses the same session-tile structure as SessionHistoryPanel.
 */
object ResumePopup {
  def apply(
    sessions: Signal[List[EditorSession]],
    lang: Signal[Language],
    onResume: EditorSession => Unit,
    onStartNew: => Unit,
    onDelete: EditorSession => Unit,
    onClearAll: => Unit,
  ): Element = {
    div(
      cls := "resume-popup-overlay",

      div(
        cls := "resume-popup",

        h3(child.text <-- lang.map {
          case Language.En => "Continue where you left off?"
          case Language.Cs => "Pokračovat tam, kde jste skončili?"
        }),
        p(child.text <-- lang.map {
          case Language.En => "You have saved work in progress. Choose a session to continue, or start fresh."
          case Language.Cs => "Máte rozdělanou práci. Vyberte relaci pro pokračování nebo začněte znovu."
        }),

        // Scrollable session list
        div(
          cls := "resume-sessions-list",
          children <-- sessions.combineWith(lang).map { (sess, l) =>
            sess.map(session => renderSessionTile(session, l, onResume, onDelete))
          }
        ),

        // Bottom actions — Clear All (left, 1/3) + New Session (right, 2/3)
        div(
          cls := "resume-popup-actions",
          button(
            cls := "clear-all-btn",
            child.text <-- lang.map {
              case Language.En => "🗑 Clear All"
              case Language.Cs => "🗑 Smazat vše"
            },
            onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
              val confirmMsg = currentLang match {
                case Language.En => "Delete all saved sessions? This cannot be undone."
                case Language.Cs => "Smazat všechny uložené relace? Toto nelze vrátit zpět."
              }
              if dom.window.confirm(confirmMsg) then onClearAll
            },
          ),
          button(
            cls := "add-element-btn",
            child.text <-- lang.map {
              case Language.En => "+ New Session"
              case Language.Cs => "+ Nová relace"
            },
            onClick --> { _ => onStartNew },
          ),
        ),
      ),
    )
  }

  private def renderSessionTile(session: EditorSession, lang: Language, onResume: EditorSession => Unit, onDelete: EditorSession => Unit): Element = {
    val fmt = session.editorState.productFormat
    val displayName = session.sessionName.filter(_.nonEmpty).getOrElse(session.title)
    val detail = s"${fmt.widthMm}x${fmt.heightMm}mm | ${session.editorState.pages.size} pages"
    val updatedStr = formatTimestamp(session.updatedAt)

    div(
      cls := "session-tile",
      div(
        cls := "session-tile-content",
        div(
          cls := "session-tile-header",
          div(cls := "session-tile-name", displayName),
        ),
        div(cls := "session-tile-detail", detail),
        div(cls := "session-tile-time", lang match {
          case Language.En => s"Last edited: $updatedStr"
          case Language.Cs => s"Naposledy upraveno: $updatedStr"
        }),
      ),
      div(
        cls := "session-tile-actions",
        button(
          cls := "session-btn-load",
          lang match { case Language.En => "Load"; case Language.Cs => "Načíst" },
          onClick --> { _ => onResume(session) },
        ),
        button(
          cls := "session-btn-delete",
          lang match { case Language.En => "Delete"; case Language.Cs => "Smazat" },
          onClick --> { _ => onDelete(session) },
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
