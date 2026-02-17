package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.components.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.Language

object CalendarBuilderApp {
  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "calendar-builder-app",

      // Update calendar month names when language changes
      lang.changes --> { language =>
        CalendarViewModel.updateLanguage(language.toCode)
      },

      // Header
      div(
        cls := "calendar-header",
        h1(child.text <-- lang.map {
          case Language.En => "Photo Calendar Builder"
          case Language.Cs => "Tvůrce foto kalendáře"
        }),
        p(child.text <-- lang.map {
          case Language.En => "Create your custom photo calendar - upload photos, add text, shapes and customize each month"
          case Language.Cs => "Vytvořte si vlastní foto kalendář - nahrajte fotky, přidejte text, tvary a přizpůsobte každý měsíc"
        })
      ),

      // Main content area
      div(
        cls := "calendar-main-content",

        // Left sidebar: Unified element editor + background
        div(
          cls := "calendar-sidebar",

          div(
            cls := "calendar-controls-card",

            // Unified element list & forms
            ElementListEditor(),

            hr(),

            // Background & template editor
            BackgroundEditor(),
          )
        ),

        // Center: Calendar page canvas
        div(
          cls := "calendar-canvas-area",
          CalendarPageCanvas()
        ),

        // Right sidebar: Page navigation
        div(
          cls := "calendar-navigation-sidebar",

          div(
            cls := "calendar-nav-card",
            PageNavigation()
          )
        )
      ),

      // Footer with actions
      div(
        cls := "calendar-footer",
        button(
          cls := "reset-btn",
          child.text <-- lang.map {
            case Language.En => "Reset Calendar"
            case Language.Cs => "Resetovat kalendář"
          },
          onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
            val confirmMsg = currentLang match {
              case Language.En => "Are you sure you want to reset the calendar? All changes will be lost."
              case Language.Cs => "Opravdu chcete resetovat kalendář? Všechny změny budou ztraceny."
            }
            if org.scalajs.dom.window.confirm(confirmMsg) then
              CalendarViewModel.reset()
          }
        ),
        button(
          cls := "preview-btn",
          child.text <-- lang.map {
            case Language.En => "Preview All Pages"
            case Language.Cs => "Náhled všech stránek"
          },
          onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
            val msg = currentLang match {
              case Language.En => "Preview feature coming soon!"
              case Language.Cs => "Funkce náhledu bude brzy k dispozici!"
            }
            org.scalajs.dom.window.alert(msg)
          }
        )
      )
    )
  }
}
