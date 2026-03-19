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
            button(
              cls := "resume-session-btn",
              div(
                cls := "resume-session-info",
                strong(session.title),
                span(cls := "resume-session-meta", s"${fmt.widthMm}x${fmt.heightMm}mm | ${session.editorState.pages.size} pages"),
              ),
              onClick --> { _ => onResume(session) },
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
}
