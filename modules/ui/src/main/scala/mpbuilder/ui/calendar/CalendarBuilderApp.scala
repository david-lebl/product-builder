package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.components.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language
import mpbuilder.uikit.containers.{Tabs, TabDef}
import org.scalajs.dom
import scala.scalajs.js

object CalendarBuilderApp {

  // Sidebar tab state: "elements", "background", or "sessions"
  private val sidebarTabVar: Var[String] = Var("elements")

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "calendar-builder-app",

      // Initialize sessions on mount
      onMountCallback { _ =>
        CalendarViewModel.initSessions()

        // Register beforeunload to save on exit
        dom.window.addEventListener("beforeunload", { (_: dom.Event) =>
          CalendarViewModel.saveCurrentSession()
        })
      },

      // Auto-save: schedule save whenever editor state changes
      stateChangeObserver,

      // Update page titles when language changes
      lang.changes --> { language =>
        CalendarViewModel.updateLanguage(language.toCode)
      },

      // Resume dialog overlay (conditionally visible)
      SessionResumeDialog(),

      // Header with save status
      div(
        cls := "calendar-header",
        h1(child.text <-- lang.map {
          case Language.En => "Visual Product Editor"
          case Language.Cs => "Vizualni editor produktu"
        }),
        p(child.text <-- lang.map {
          case Language.En => "Create your custom visual product — upload photos, add text, shapes and customize each page"
          case Language.Cs => "Vytvorte si vlastni vizualni produkt — nahrajte fotky, pridejte text, tvary a prizpusobte kazdou stranku"
        }),

        // Save status indicator (inline in header)
        div(
          cls := "header-save-status",
          child.text <-- CalendarViewModel.saveStatus.combineWith(lang).map { (status, language) =>
            status match
              case SaveStatus.Unsaved => ""
              case SaveStatus.Saving => language match
                case Language.En => "Saving..."
                case Language.Cs => "Ukladani..."
              case SaveStatus.Saved(ts) =>
                val date = new js.Date(ts)
                val hours = f"${date.getHours().toInt}%02d"
                val minutes = f"${date.getMinutes().toInt}%02d"
                language match
                  case Language.En => s"Saved · $hours:$minutes"
                  case Language.Cs => s"Ulozeno · $hours:$minutes"
          },
          cls <-- CalendarViewModel.saveStatus.map {
            case SaveStatus.Saved(_) => "status-saved"
            case SaveStatus.Saving   => "status-saving"
            case SaveStatus.Unsaved  => "status-unsaved"
          }
        ),
      ),

      // Product type and format selectors
      div(
        cls := "product-selectors",

        div(
          cls := "selector-group",
          label(child.text <-- lang.map {
            case Language.En => "Product Type:"
            case Language.Cs => "Typ produktu:"
          }),
          select(
            cls := "product-type-select",
            value <-- CalendarViewModel.productType.map {
              case VisualProductType.MonthlyCalendar  => "monthly"
              case VisualProductType.WeeklyCalendar   => "weekly"
              case VisualProductType.BiweeklyCalendar => "biweekly"
              case VisualProductType.PhotoBook        => "photobook"
              case VisualProductType.WallPicture      => "wallpicture"
            },
            option(value := "monthly", child.text <-- lang.map {
              case Language.En => "Monthly Calendar (12 pages)"
              case Language.Cs => "Mesicni kalendar (12 stranek)"
            }),
            option(value := "weekly", child.text <-- lang.map {
              case Language.En => "Weekly Calendar (52 pages)"
              case Language.Cs => "Tydenni kalendar (52 stranek)"
            }),
            option(value := "biweekly", child.text <-- lang.map {
              case Language.En => "Bi-weekly Calendar (26 pages)"
              case Language.Cs => "Dvoutydenni kalendar (26 stranek)"
            }),
            option(value := "photobook", child.text <-- lang.map {
              case Language.En => "Photo Book (12 pages)"
              case Language.Cs => "Fotokniha (12 stranek)"
            }),
            option(value := "wallpicture", child.text <-- lang.map {
              case Language.En => "Wall Picture (1 page)"
              case Language.Cs => "Obraz na zed (1 stranka)"
            }),
            onChange.mapToValue --> { v =>
              val pt = v match {
                case "weekly"      => VisualProductType.WeeklyCalendar
                case "biweekly"    => VisualProductType.BiweeklyCalendar
                case "photobook"   => VisualProductType.PhotoBook
                case "wallpicture" => VisualProductType.WallPicture
                case _             => VisualProductType.MonthlyCalendar
              }
              CalendarViewModel.setProductType(pt)
            }
          ),
        ),

        // Dynamic format selector driven by product type
        div(
          cls := "selector-group",
          label(child.text <-- lang.map {
            case Language.En => "Format:"
            case Language.Cs => "Format:"
          }),
          child <-- CalendarViewModel.state.combineWith(lang).map { case (st, language) =>
            val pt = st.productType
            val currentFmt = st.productFormat
            val formats = ProductFormat.formatsFor(pt)
            select(
              cls := "format-select",
              value := currentFmt.id,
              formats.map { fmt =>
                val lbl = language match {
                  case Language.En => s"${fmt.nameEn} (${fmt.widthMm}x${fmt.heightMm} mm)"
                  case Language.Cs => s"${fmt.nameCs} (${fmt.widthMm}x${fmt.heightMm} mm)"
                }
                option(value := fmt.id, selected := (fmt.id == currentFmt.id), lbl)
              },
              onChange.mapToValue --> { selectedId =>
                formats.find(_.id == selectedId).foreach(CalendarViewModel.setProductFormat)
              }
            )
          },
        ),
      ),

      // Main content area: 2-column layout (sidebar + canvas)
      div(
        cls := "calendar-main-content",

        // Left sidebar with tabs
        div(
          cls := "calendar-sidebar",
          Tabs(
            tabs = List(
              TabDef("elements", lang.map {
                case Language.En => "Elements"
                case Language.Cs => "Prvky"
              }, () => div(cls := "calendar-controls-card", ElementListEditor())),
              TabDef("background", lang.map {
                case Language.En => "Background"
                case Language.Cs => "Pozadi"
              }, () => div(cls := "calendar-controls-card", BackgroundEditor())),
              TabDef("sessions", lang.map {
                case Language.En => "Sessions"
                case Language.Cs => "Relace"
              }, () => div(cls := "calendar-controls-card", SessionPanel())),
            ),
            activeTab = sidebarTabVar,
          ),
        ),

        // Center: Calendar page canvas
        div(
          cls := "calendar-canvas-area",
          CalendarPageCanvas()
        ),
      ),

      // Page navigation strip (horizontal, scrollable)
      PageNavigation(),

      // Footer with actions
      div(
        cls := "calendar-footer",
        button(
          cls := "reset-btn",
          child.text <-- lang.map {
            case Language.En => "Reset Product"
            case Language.Cs => "Resetovat produkt"
          },
          onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
            val confirmMsg = currentLang match {
              case Language.En => "Are you sure you want to reset? All changes will be lost."
              case Language.Cs => "Opravdu chcete resetovat? Vsechny zmeny budou ztraceny."
            }
            if org.scalajs.dom.window.confirm(confirmMsg) then
              CalendarViewModel.reset()
          }
        ),
        button(
          cls := "preview-btn",
          child.text <-- lang.map {
            case Language.En => "Preview All Pages"
            case Language.Cs => "Nahled vsech stranek"
          },
          onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
            val msg = currentLang match {
              case Language.En => "Preview feature coming soon!"
              case Language.Cs => "Funkce nahledu bude brzy k dispozici!"
            }
            org.scalajs.dom.window.alert(msg)
          }
        )
      )
    )
  }

  /** Observer that triggers auto-save on any state change */
  private def stateChangeObserver: Modifier[Element] =
    CalendarViewModel.state.changes --> { _ =>
      CalendarViewModel.scheduleAutoSave()
    }
}
