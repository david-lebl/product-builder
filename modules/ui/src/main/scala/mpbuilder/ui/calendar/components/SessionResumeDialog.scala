package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.productbuilder.ProductBuilderViewModel

/** Modal dialog shown on editor entry when previous sessions exist */
object SessionResumeDialog {

  def apply(
    sessions: Signal[List[SessionSummary]],
    onContinue: String => Unit,
    onNewSession: () => Unit,
    onDismiss: () => Unit,
  ): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "session-resume-overlay",

      div(
        cls := "session-resume-dialog",

        h2(child.text <-- lang.map {
          case Language.En => "Welcome Back"
          case Language.Cs => "Vítejte zpět"
        }),

        p(cls := "session-resume-subtitle", child.text <-- lang.map {
          case Language.En => "You have previous editor sessions. Would you like to continue where you left off?"
          case Language.Cs => "Máte předchozí relace editoru. Chcete pokračovat tam, kde jste skončili?"
        }),

        // Session list
        div(
          cls := "session-resume-list",
          children <-- sessions.combineWith(lang).map { case (list, l) =>
            list.take(5).map { summary =>
              val productName = l match
                case Language.En => formatProductTypeEn(summary.productType)
                case Language.Cs => formatProductTypeCs(summary.productType)
              val formatName = l match
                case Language.En => summary.productFormat.nameEn
                case Language.Cs => summary.productFormat.nameCs
              val timeStr = formatTimestamp(summary.updatedAt)

              div(
                cls := "session-resume-item",
                onClick --> { _ => onContinue(summary.id) },

                div(cls := "session-resume-item-name", summary.name),
                div(cls := "session-resume-item-detail",
                  span(s"$productName · $formatName"),
                  span(s" · ${summary.pageCount} ", child.text <-- lang.map {
                    case Language.En => "pages"
                    case Language.Cs => "stránek"
                  }),
                  span(s" · ${summary.elementCount} ", child.text <-- lang.map {
                    case Language.En => "elements"
                    case Language.Cs => "prvků"
                  }),
                ),
                div(cls := "session-resume-item-time", timeStr),
              )
            }
          }
        ),

        // Actions
        div(
          cls := "session-resume-actions",
          button(
            cls := "session-resume-new-btn",
            child.text <-- lang.map {
              case Language.En => "Start New Session"
              case Language.Cs => "Začít novou relaci"
            },
            onClick --> { _ => onNewSession() },
          ),
          button(
            cls := "session-resume-dismiss-btn",
            child.text <-- lang.map {
              case Language.En => "Close"
              case Language.Cs => "Zavřít"
            },
            onClick --> { _ => onDismiss() },
          ),
        ),
      ),
    )
  }

  private def formatProductTypeEn(pt: VisualProductType): String = pt match
    case VisualProductType.MonthlyCalendar  => "Monthly Calendar"
    case VisualProductType.WeeklyCalendar   => "Weekly Calendar"
    case VisualProductType.BiweeklyCalendar => "Bi-weekly Calendar"
    case VisualProductType.PhotoBook        => "Photo Book"
    case VisualProductType.WallPicture      => "Wall Picture"
    case VisualProductType.CustomProduct    => "Custom Product"

  private def formatProductTypeCs(pt: VisualProductType): String = pt match
    case VisualProductType.MonthlyCalendar  => "Měsíční kalendář"
    case VisualProductType.WeeklyCalendar   => "Týdenní kalendář"
    case VisualProductType.BiweeklyCalendar => "Dvoutýdenní kalendář"
    case VisualProductType.PhotoBook        => "Fotokniha"
    case VisualProductType.WallPicture      => "Obraz na zeď"
    case VisualProductType.CustomProduct    => "Vlastní produkt"

  private def formatTimestamp(epoch: Double): String =
    val date = new scala.scalajs.js.Date(epoch)
    val day = f"${date.getDate().toInt}%02d"
    val month = f"${(date.getMonth() + 1).toInt}%02d"
    val hours = f"${date.getHours().toInt}%02d"
    val minutes = f"${date.getMinutes().toInt}%02d"
    s"$day.$month. $hours:$minutes"
}
