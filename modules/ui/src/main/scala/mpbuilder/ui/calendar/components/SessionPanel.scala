package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.productbuilder.ProductBuilderViewModel

/** Collapsible session management panel in the editor sidebar */
object SessionPanel {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage
    val expandedVar = Var(false)

    div(
      cls := "session-panel",

      // Session info bar (always visible)
      div(
        cls := "session-info-bar",

        // Session name (editable)
        div(
          cls := "session-name-row",
          span(cls := "session-label", "📄 "),
          input(
            cls := "session-name-input",
            typ := "text",
            value <-- CalendarViewModel.currentSessionName,
            onInput.mapToValue --> { name =>
              CalendarViewModel.renameSession(name)
            },
          ),
        ),

        // Save status
        div(
          cls := "session-save-status",
          child.text <-- CalendarViewModel.saveStatus,
        ),

        // Toggle expand
        button(
          cls := "session-expand-btn",
          child.text <-- expandedVar.signal.map(if _ then "▲" else "▼"),
          title <-- lang.map {
            case Language.En => "Session management"
            case Language.Cs => "Správa relací"
          },
          onClick --> { _ => expandedVar.update(!_) },
        ),
      ),

      // Linked product context bar (when session is linked to a product configuration)
      child.maybe <-- CalendarViewModel.linkedProductDescription.map {
        case Some(desc) =>
          Some(div(
            cls := "session-linked-product",
            span(cls := "linked-product-icon", "🔗"),
            span(cls := "linked-product-text", desc),
          ))
        case None => None
      },

      // Expanded: session list
      div(
        cls := "session-list-panel",
        cls <-- expandedVar.signal.map(if _ then "session-list-panel-open" else ""),

        // Actions
        div(
          cls := "session-list-actions",
          button(
            cls := "session-action-btn session-new-btn",
            child.text <-- lang.map {
              case Language.En => "+ New Session"
              case Language.Cs => "+ Nová relace"
            },
            onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
              val confirmMsg = currentLang match
                case Language.En => "Start a new session? Current work will be saved."
                case Language.Cs => "Začít novou relaci? Aktuální práce bude uložena."
              if org.scalajs.dom.window.confirm(confirmMsg) then
                CalendarViewModel.saveCurrentSession()
                CalendarViewModel.newSession()
            },
          ),
          button(
            cls := "session-action-btn session-save-btn",
            child.text <-- lang.map {
              case Language.En => "💾 Save Now"
              case Language.Cs => "💾 Uložit"
            },
            onClick --> { _ => CalendarViewModel.saveCurrentSession() },
          ),
        ),

        // Export/Import
        div(
          cls := "session-list-actions",
          button(
            cls := "session-action-btn session-export-btn",
            child.text <-- lang.map {
              case Language.En => "📤 Export"
              case Language.Cs => "📤 Export"
            },
            onClick --> { _ =>
              CalendarViewModel.exportSession().foreach { json =>
                val blobOpts = new org.scalajs.dom.BlobPropertyBag { `type` = "application/json" }
                val blob = new org.scalajs.dom.Blob(
                  scala.scalajs.js.Array(json),
                  blobOpts,
                )
                val url = org.scalajs.dom.URL.createObjectURL(blob)
                val a = org.scalajs.dom.document.createElement("a").asInstanceOf[org.scalajs.dom.HTMLAnchorElement]
                a.href = url
                a.setAttribute("download", "editor-session.json")
                a.click()
                org.scalajs.dom.URL.revokeObjectURL(url)
              }
            },
          ),
          label(
            cls := "session-action-btn session-import-btn",
            child.text <-- lang.map {
              case Language.En => "📥 Import"
              case Language.Cs => "📥 Import"
            },
            input(
              typ := "file",
              accept := ".json",
              display := "none",
              onChange --> { ev =>
                val input = ev.target.asInstanceOf[org.scalajs.dom.HTMLInputElement]
                val files = input.files
                if files.length > 0 then
                  val reader = new org.scalajs.dom.FileReader()
                  reader.onload = { _ =>
                    val json = reader.result.asInstanceOf[String]
                    CalendarViewModel.importSession(json)
                  }
                  reader.readAsText(files(0))
              },
            ),
          ),
        ),

        // Session list
        h4(cls := "session-list-heading", child.text <-- lang.map {
          case Language.En => "Saved Sessions"
          case Language.Cs => "Uložené relace"
        }),
        div(
          cls := "session-list",
          children <-- CalendarViewModel.sessionList.combineWith(CalendarViewModel.currentSessionId).combineWith(lang).map {
            (sessions: List[SessionSummary], currentId: Option[String], l: Language) =>
              if sessions.isEmpty then
                List(div(cls := "session-list-empty", l match
                  case Language.En => "No saved sessions"
                  case Language.Cs => "Žádné uložené relace"
                ))
              else
                sessions.map { summary =>
                  val isCurrent = currentId.contains(summary.id)
                  val productName = l match
                    case Language.En => formatProductTypeEn(summary.productType)
                    case Language.Cs => formatProductTypeCs(summary.productType)

                  div(
                    cls := s"session-list-item${if isCurrent then " session-list-item-current" else ""}",
                    div(
                      cls := "session-list-item-info",
                      div(cls := "session-list-item-name",
                        if isCurrent then s"● ${summary.name}" else summary.name
                      ),
                      div(cls := "session-list-item-detail",
                        s"$productName · ${summary.pageCount} ",
                        l match
                          case Language.En => "pages"
                          case Language.Cs => "stránek"
                      ),
                    ),
                    div(
                      cls := "session-list-item-actions",
                      if !isCurrent then
                        button(
                          cls := "session-item-load-btn",
                          l match { case Language.En => "Load"; case Language.Cs => "Načíst" },
                          onClick --> { _ =>
                            CalendarViewModel.saveCurrentSession()
                            CalendarViewModel.loadSession(summary.id)
                          },
                        )
                      else emptyNode,
                      button(
                        cls := "session-item-delete-btn",
                        "×",
                        onClick --> { _ => CalendarViewModel.deleteSession(summary.id) },
                      ),
                    ),
                  )
                }
          }
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
}
