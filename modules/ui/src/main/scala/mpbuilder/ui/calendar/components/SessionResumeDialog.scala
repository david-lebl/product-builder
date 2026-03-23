package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Modal dialog shown on editor entry when previous sessions exist */
object SessionResumeDialog {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage
    val sessions = CalendarViewModel.sessionList
    val show = CalendarViewModel.showResumeDialog

    div(
      cls := "session-resume-overlay",
      cls <-- show.map(s => if s then "visible" else ""),

      div(
        cls := "session-resume-dialog",

        h2(child.text <-- lang.map {
          case Language.En => "Welcome Back"
          case Language.Cs => "Vitejte zpet"
        }),

        p(cls := "resume-subtitle", child.text <-- lang.map {
          case Language.En => "Continue with a previous session or start fresh."
          case Language.Cs => "Pokracujte v predchozi relaci nebo zacnete znovu."
        }),

        // Session list
        div(
          cls := "resume-session-list",
          children <-- sessions.combineWith(lang).map { (list, language) =>
            list.map { summary =>
              renderSessionRow(summary, language)
            }
          }
        ),

        // Start new button
        div(
          cls := "resume-actions",
          button(
            cls := "resume-new-btn",
            child.text <-- lang.map {
              case Language.En => "Start New Session"
              case Language.Cs => "Zahajit novou relaci"
            },
            onClick --> { _ =>
              CalendarViewModel.startNewSession()
            }
          ),
        ),
      ),
    )
  }

  private def renderSessionRow(summary: SessionSummary, lang: Language): Element = {
    val typeName = productTypeName(summary.productType, lang)
    val dims = s"${summary.productFormat.widthMm}x${summary.productFormat.heightMm} mm"
    val timeStr = formatTime(summary.updatedAt)

    div(
      cls := "resume-session-row",

      div(
        cls := "resume-session-info",
        div(cls := "resume-session-name", summary.name),
        div(
          cls := "resume-session-meta",
          span(typeName),
          span(s" · $dims"),
          span(s" · ${summary.pageCount} ", lang match {
            case Language.En => "pages"
            case Language.Cs => "stranek"
          }),
        ),
        div(cls := "resume-session-time", timeStr),
      ),

      button(
        cls := "resume-load-btn",
        lang match {
          case Language.En => "Continue"
          case Language.Cs => "Pokracovat"
        },
        onClick --> { _ =>
          CalendarViewModel.loadSession(summary.id)
        }
      ),
    )
  }

  private def productTypeName(pt: VisualProductType, lang: Language): String = pt match
    case VisualProductType.MonthlyCalendar => lang match
      case Language.En => "Monthly Calendar"
      case Language.Cs => "Mesicni kalendar"
    case VisualProductType.WeeklyCalendar => lang match
      case Language.En => "Weekly Calendar"
      case Language.Cs => "Tydenni kalendar"
    case VisualProductType.BiweeklyCalendar => lang match
      case Language.En => "Bi-weekly Calendar"
      case Language.Cs => "Dvoutydenni kalendar"
    case VisualProductType.PhotoBook => lang match
      case Language.En => "Photo Book"
      case Language.Cs => "Fotokniha"
    case VisualProductType.WallPicture => lang match
      case Language.En => "Wall Picture"
      case Language.Cs => "Obraz na zed"

  private def formatTime(epochMs: Double): String =
    val date = new scalajs.js.Date(epochMs)
    val day = date.getDate().toInt
    val month = (date.getMonth() + 1).toInt
    val year = date.getFullYear().toInt
    val hours = f"${date.getHours().toInt}%02d"
    val minutes = f"${date.getMinutes().toInt}%02d"
    s"$day.$month.$year $hours:$minutes"
}
