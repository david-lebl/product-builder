package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Sidebar panel for session name editing, save status, and session history */
object SessionPanel {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "session-panel",

      // ─── Current session info ─────────────────────────────────
      div(
        cls := "session-current",

        h4(child.text <-- lang.map {
          case Language.En => "Current Session"
          case Language.Cs => "Aktualni relace"
        }),

        // Session name (editable)
        div(
          cls := "session-name-row",
          child <-- CalendarViewModel.currentSession.map {
            case Some(meta) =>
              input(
                typ := "text",
                cls := "session-name-input",
                controlled(
                  value <-- CalendarViewModel.currentSession.map(_.map(_.name).getOrElse("")),
                  onInput.mapToValue --> { v =>
                    CalendarViewModel.renameSession(v)
                  }
                ),
              )
            case None =>
              span(cls := "session-no-active", child.text <-- lang.map {
                case Language.En => "No active session"
                case Language.Cs => "Zadna aktivni relace"
              })
          }
        ),

        // Save status indicator
        div(
          cls := "session-save-status",
          child.text <-- CalendarViewModel.saveStatus.combineWith(lang).map { (status, language) =>
            status match
              case SaveStatus.Unsaved => language match
                case Language.En => "Unsaved changes"
                case Language.Cs => "Neulozene zmeny"
              case SaveStatus.Saving => language match
                case Language.En => "Saving..."
                case Language.Cs => "Ukladani..."
              case SaveStatus.Saved(ts) =>
                val timeStr = formatTime(ts)
                language match
                  case Language.En => s"Saved · $timeStr"
                  case Language.Cs => s"Ulozeno · $timeStr"
          },
          cls <-- CalendarViewModel.saveStatus.map {
            case SaveStatus.Saved(_) => "status-saved"
            case SaveStatus.Saving   => "status-saving"
            case SaveStatus.Unsaved  => "status-unsaved"
          }
        ),

        // Manual save button
        button(
          cls := "session-save-btn",
          child.text <-- lang.map {
            case Language.En => "Save Now"
            case Language.Cs => "Ulozit nyni"
          },
          disabled <-- CalendarViewModel.currentSession.map(_.isEmpty),
          onClick --> { _ => CalendarViewModel.saveCurrentSession() }
        ),
      ),

      hr(),

      // ─── Session history ──────────────────────────────────────
      div(
        cls := "session-history",

        h4(child.text <-- lang.map {
          case Language.En => "Session History"
          case Language.Cs => "Historie relaci"
        }),

        div(
          cls := "session-list",
          children <-- CalendarViewModel.sessionList.combineWith(
            CalendarViewModel.currentSession, lang
          ).map { (sessions: List[SessionSummary], currentMeta: Option[SessionMeta], language: Language) =>
            val currentId = currentMeta.map(_.id)
            if sessions.isEmpty then
              List(div(cls := "session-empty", language match {
                case Language.En => "No saved sessions"
                case Language.Cs => "Zadne ulozene relace"
              }))
            else
              sessions.map { summary =>
                renderHistoryRow(summary, currentId, language)
              }
          }
        ),
      ),
    )
  }

  private def renderHistoryRow(
    summary: SessionSummary,
    currentId: Option[String],
    lang: Language,
  ): Element = {
    val isCurrent = currentId.contains(summary.id)
    val typeName = productTypeName(summary.productType, lang)
    val dims = s"${summary.productFormat.widthMm}x${summary.productFormat.heightMm} mm"
    val timeStr = formatTime(summary.updatedAt)

    div(
      cls := "session-history-row",
      cls := "current" -> isCurrent,

      div(
        cls := "session-history-info",
        div(
          cls := "session-history-name",
          summary.name,
          if isCurrent then span(cls := "session-current-badge", lang match {
            case Language.En => " (active)"
            case Language.Cs => " (aktivni)"
          }) else emptyMod,
        ),
        div(
          cls := "session-history-meta",
          s"$typeName · $dims · ${summary.pageCount} ",
          lang match {
            case Language.En => "pages"
            case Language.Cs => "stranek"
          },
        ),
        div(cls := "session-history-time", timeStr),
      ),

      div(
        cls := "session-history-actions",
        if !isCurrent then
          button(
            cls := "session-action-btn session-load-btn",
            lang match {
              case Language.En => "Load"
              case Language.Cs => "Nacist"
            },
            onClick --> { _ => CalendarViewModel.loadSession(summary.id) }
          )
        else emptyMod,
        button(
          cls := "session-action-btn session-delete-btn",
          lang match {
            case Language.En => "Delete"
            case Language.Cs => "Smazat"
          },
          onClick.compose(_.withCurrentValueOf(ProductBuilderViewModel.currentLanguage)) --> { case (_, currentLang) =>
            val msg = currentLang match
              case Language.En => s"Delete session \"${summary.name}\"?"
              case Language.Cs => s"Smazat relaci \"${summary.name}\"?"
            if org.scalajs.dom.window.confirm(msg) then
              CalendarViewModel.deleteSession(summary.id)
          }
        ),
      ),
    )
  }

  private def productTypeName(pt: VisualProductType, lang: Language): String = pt match
    case VisualProductType.MonthlyCalendar  => if lang == Language.En then "Monthly Calendar" else "Mesicni kalendar"
    case VisualProductType.WeeklyCalendar   => if lang == Language.En then "Weekly Calendar" else "Tydenni kalendar"
    case VisualProductType.BiweeklyCalendar => if lang == Language.En then "Bi-weekly Calendar" else "Dvoutydenni kalendar"
    case VisualProductType.PhotoBook        => if lang == Language.En then "Photo Book" else "Fotokniha"
    case VisualProductType.WallPicture      => if lang == Language.En then "Wall Picture" else "Obraz na zed"

  private def formatTime(epochMs: Double): String =
    val date = new scalajs.js.Date(epochMs)
    val day = date.getDate().toInt
    val month = (date.getMonth() + 1).toInt
    val year = date.getFullYear().toInt
    val hours = f"${date.getHours().toInt}%02d"
    val minutes = f"${date.getMinutes().toInt}%02d"
    s"$day.$month.$year $hours:$minutes"
}
