package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.EditorSession

/** Modal popup shown when opening the editor standalone with existing sessions in IndexedDB */
object ResumePopup {
  def apply(
    sessions: List[EditorSession],
    onResume: EditorSession => Unit,
    onStartNew: => Unit,
  ): Element = {
    div(
      cls := "resume-popup-overlay",

      div(
        cls := "resume-popup",

        h3("Continue where you left off?"),
        p("You have saved work in progress. Choose a session to continue, or start fresh."),

        div(
          cls := "resume-sessions-list",
          sessions.map { session =>
            val fmt = session.editorState.productFormat
            val displayName = session.sessionName.filter(_.nonEmpty).getOrElse(session.title)
            val detail = s"${fmt.widthMm}x${fmt.heightMm}mm | ${session.editorState.pages.size} pages"
            val editedTime = formatTimestamp(session.updatedAt)
            div(
              cls := "resume-session-card",
              div(
                cls := "resume-session-info",
                div(cls := "resume-session-name", displayName),
                div(cls := "resume-session-detail", detail),
                div(cls := "resume-session-time", s"Last edited: $editedTime"),
              ),
              button(
                cls := "resume-session-load-btn",
                "Resume",
                onClick --> { _ => onResume(session) },
              ),
            )
          }
        ),

        div(
          cls := "resume-actions",
          button(
            cls := "add-element-btn",
            "Start New",
            onClick --> { _ => onStartNew },
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
