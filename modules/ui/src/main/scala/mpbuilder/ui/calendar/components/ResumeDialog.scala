package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.CalendarViewModel
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Modal dialog asking the user whether to resume a previous editing session. */
object ResumeDialog {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls <-- CalendarViewModel.showResumeDialogVar.signal.map { show =>
        if show then "resume-dialog-overlay visible" else "resume-dialog-overlay"
      },

      div(
        cls := "resume-dialog",

        h3(child.text <-- lang.map {
          case Language.En => "Resume Previous Work?"
          case Language.Cs => "Pokračovat v předchozí práci?"
        }),

        p(child.text <-- lang.map {
          case Language.En => "You have a previous editing session. Would you like to continue where you left off, or start a new project?"
          case Language.Cs => "Máte předchozí relaci úprav. Chcete pokračovat tam, kde jste skončili, nebo začít nový projekt?"
        }),

        // Show session info
        child <-- CalendarViewModel.resumeSessionIdVar.signal.combineWith(lang).map { case (sessionIdOpt, l) =>
          sessionIdOpt.flatMap { sid =>
            CalendarViewModel.sessionListVar.now().find(_.id == sid).map { meta =>
              div(
                cls := "resume-session-info",
                strong(meta.title),
                span(s" — ${meta.productType}, ${meta.pageCount} "),
                span(l match
                  case Language.En => if meta.pageCount == 1 then "page" else "pages"
                  case Language.Cs => if meta.pageCount == 1 then "stránka" else "stránek"
                ),
              )
            }
          }.getOrElse(emptyNode)
        },

        div(
          cls := "resume-dialog-actions",
          button(
            cls := "resume-continue-btn",
            child.text <-- lang.map {
              case Language.En => "Continue"
              case Language.Cs => "Pokračovat"
            },
            onClick --> { _ => CalendarViewModel.resumeSession() },
          ),
          button(
            cls := "resume-new-btn",
            child.text <-- lang.map {
              case Language.En => "Start New"
              case Language.Cs => "Začít nový"
            },
            onClick --> { _ => CalendarViewModel.startFresh() },
          ),
        ),
      ),
    )
  }
}
