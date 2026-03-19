package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.{CalendarViewModel, SessionMeta}
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Panel showing saved editor sessions with load/delete actions. */
object SessionHistoryPanel {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "session-history-panel",

      h3(child.text <-- lang.map {
        case Language.En => "Saved Sessions"
        case Language.Cs => "Uložené relace"
      }),

      // Session list
      child <-- CalendarViewModel.sessionListVar.signal.combineWith(lang).map { case (sessions, l) =>
        if sessions.isEmpty then
          div(
            cls := "session-history-empty",
            p(l match
              case Language.En => "No saved sessions yet. Your work will be auto-saved as you edit."
              case Language.Cs => "Zatím žádné uložené relace. Vaše práce bude automaticky uložena při úpravách."
            ),
          )
        else
          div(
            cls := "session-history-list",
            sessions.sortBy(-_.lastUpdated).map { meta =>
              sessionCard(meta, l)
            },
          )
      },
    )
  }

  private def sessionCard(meta: SessionMeta, lang: Language): Element = {
    val isCurrentSession = CalendarViewModel.currentSessionId.map(_.contains(meta.id))

    div(
      cls := "session-card",
      cls <-- isCurrentSession.map(active => if active then "session-card-active" else ""),

      // Session info
      div(
        cls := "session-card-info",
        div(
          cls := "session-card-title",
          strong(meta.title),
        ),
        div(
          cls := "session-card-meta",
          span {
            val pagesLabel = lang match
              case Language.En => if meta.pageCount == 1 then "page" else "pages"
              case Language.Cs => if meta.pageCount == 1 then "stránka" else "stránek"
            s"${meta.productType} · ${meta.pageCount} $pagesLabel"
          },
          span(" · "),
          span(formatRelativeTime(meta.lastUpdated, lang)),
        ),
      ),

      // Actions
      div(
        cls := "session-card-actions",
        button(
          cls := "session-load-btn",
          child.text <-- ProductBuilderViewModel.currentLanguage.map {
            case Language.En => "Load"
            case Language.Cs => "Načíst"
          },
          onClick --> { _ => CalendarViewModel.loadSession(meta.id) },
        ),
        button(
          cls := "session-delete-btn",
          child.text <-- ProductBuilderViewModel.currentLanguage.map {
            case Language.En => "Delete"
            case Language.Cs => "Smazat"
          },
          onClick --> { _ => CalendarViewModel.deleteSession(meta.id) },
        ),
      ),
    )
  }

  private def formatRelativeTime(epochMillis: Double, lang: Language): String = {
    val now = System.currentTimeMillis().toDouble
    val diffMs = now - epochMillis
    val diffMinutes = (diffMs / 60000).toLong
    val diffHours = (diffMs / 3600000).toLong
    val diffDays = (diffMs / 86400000).toLong

    lang match
      case Language.En =>
        if diffMinutes < 1 then "just now"
        else if diffMinutes < 60 then s"${diffMinutes}m ago"
        else if diffHours < 24 then s"${diffHours}h ago"
        else s"${diffDays}d ago"
      case Language.Cs =>
        if diffMinutes < 1 then "právě teď"
        else if diffMinutes < 60 then s"před ${diffMinutes}min"
        else if diffHours < 24 then s"před ${diffHours}h"
        else s"před ${diffDays}d"
  }
}
