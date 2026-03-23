package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.components.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language
import mpbuilder.uikit.containers.{Tabs, TabDef}

object CalendarBuilderApp {

  // Sidebar tab state: "elements", "background", or "gallery"
  private val sidebarTabVar: Var[String] = Var("elements")

  // Whether to show the session resume dialog
  private val showResumeDialogVar: Var[Boolean] = Var(false)

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    // On mount: check for pending session from product builder, or show resume dialog
    val pending = CalendarViewModel.checkPendingSession()
    pending match
      case Some(p) =>
        CalendarViewModel.initFromProductConfig(p)
      case None =>
        val sessions = CalendarViewModel.sessionList
        // Show resume dialog if sessions exist (check imperatively)
        if EditorSessionStore.listSummaries().nonEmpty then
          showResumeDialogVar.set(true)

    div(
      cls := "calendar-builder-app",

      // Update page titles when language changes
      lang.changes --> { language =>
        CalendarViewModel.updateLanguage(language.toCode)
      },

      // Session resume dialog (overlay)
      child.maybe <-- showResumeDialogVar.signal.map { show =>
        if show then
          Some(SessionResumeDialog(
            sessions = CalendarViewModel.sessionList,
            onContinue = { sessionId =>
              CalendarViewModel.loadSession(sessionId)
              showResumeDialogVar.set(false)
            },
            onNewSession = { () =>
              CalendarViewModel.newSession()
              showResumeDialogVar.set(false)
            },
            onDismiss = { () =>
              showResumeDialogVar.set(false)
            },
          ))
        else None
      },

      // Session panel (always visible at top)
      SessionPanel(),

      // Product context bar (when linked to a product configuration)
      ProductContextBar(),

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
              case VisualProductType.CustomProduct    => "custom"
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
            option(value := "custom", child.text <-- lang.map {
              case Language.En => "Custom Product (4 pages)"
              case Language.Cs => "Vlastní produkt (4 stránky)"
            }),
            onChange.mapToValue --> { v =>
              val pt = v match {
                case "weekly"      => VisualProductType.WeeklyCalendar
                case "biweekly"    => VisualProductType.BiweeklyCalendar
                case "photobook"   => VisualProductType.PhotoBook
                case "wallpicture" => VisualProductType.WallPicture
                case "custom"      => VisualProductType.CustomProduct
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
            case Language.Cs => "Formát:"
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
                  case Language.En => s"${fmt.nameEn} (${fmt.widthMm}×${fmt.heightMm} mm)"
                  case Language.Cs => s"${fmt.nameCs} (${fmt.widthMm}×${fmt.heightMm} mm)"
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
                case Language.En => "Page Elements"
                case Language.Cs => "Prvky stránky"
              }, () => div(cls := "calendar-controls-card", ElementListEditor())),
              TabDef("background", lang.map {
                case Language.En => "Background"
                case Language.Cs => "Pozadí"
              }, () => div(cls := "calendar-controls-card", BackgroundEditor())),
              TabDef("gallery", lang.map {
                case Language.En => "Image Gallery"
                case Language.Cs => "Galerie obrázků"
              }, () => div(cls := "calendar-controls-card", GalleryPanel())),
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
