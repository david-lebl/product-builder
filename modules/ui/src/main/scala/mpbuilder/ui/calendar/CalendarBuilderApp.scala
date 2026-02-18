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

      // Update page titles when language changes
      lang.changes --> { language =>
        CalendarViewModel.updateLanguage(language.toCode)
      },

      // Header
      div(
        cls := "calendar-header",
        h1(child.text <-- lang.map {
          case Language.En => "Visual Product Editor"
          case Language.Cs => "Vizuální editor produktů"
        }),
        p(child.text <-- lang.map {
          case Language.En => "Create your custom visual product — upload photos, add text, shapes and customize each page"
          case Language.Cs => "Vytvořte si vlastní vizuální produkt — nahrajte fotky, přidejte text, tvary a přizpůsobte každou stránku"
        })
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
              case Language.Cs => "Měsíční kalendář (12 stránek)"
            }),
            option(value := "weekly", child.text <-- lang.map {
              case Language.En => "Weekly Calendar (52 pages)"
              case Language.Cs => "Týdenní kalendář (52 stránek)"
            }),
            option(value := "biweekly", child.text <-- lang.map {
              case Language.En => "Bi-weekly Calendar (26 pages)"
              case Language.Cs => "Dvoutýdenní kalendář (26 stránek)"
            }),
            option(value := "photobook", child.text <-- lang.map {
              case Language.En => "Photo Book (12 pages)"
              case Language.Cs => "Fotokniha (12 stránek)"
            }),
            option(value := "wallpicture", child.text <-- lang.map {
              case Language.En => "Wall Picture (1 page)"
              case Language.Cs => "Obraz na zeď (1 stránka)"
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

        div(
          cls := "selector-group",
          label(child.text <-- lang.map {
            case Language.En => "Format:"
            case Language.Cs => "Formát:"
          }),
          select(
            cls := "format-select",
            value <-- CalendarViewModel.productFormat.map {
              case ProductFormat.Basic     => "basic"
              case ProductFormat.Landscape => "landscape"
            },
            option(value := "basic", child.text <-- lang.map {
              case Language.En => "Basic (Portrait)"
              case Language.Cs => "Základní (Na výšku)"
            }),
            option(value := "landscape", child.text <-- lang.map {
              case Language.En => "Landscape"
              case Language.Cs => "Na šířku"
            }),
            onChange.mapToValue --> { v =>
              val fmt = v match {
                case "landscape" => ProductFormat.Landscape
                case _           => ProductFormat.Basic
              }
              CalendarViewModel.setProductFormat(fmt)
            }
          ),
        ),
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
            case Language.En => "Reset Product"
            case Language.Cs => "Resetovat produkt"
          },
          onClick.compose(_.withCurrentValueOf(lang)) --> { case (_, currentLang) =>
            val confirmMsg = currentLang match {
              case Language.En => "Are you sure you want to reset? All changes will be lost."
              case Language.Cs => "Opravdu chcete resetovat? Všechny změny budou ztraceny."
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
