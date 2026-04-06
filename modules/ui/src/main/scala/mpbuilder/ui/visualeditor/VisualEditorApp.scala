package mpbuilder.ui.visualeditor

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.components.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.domain.model.{Language, ArtworkId}
import mpbuilder.uikit.containers.{Tabs, TabDef}
import mpbuilder.ui.persistence.EditorSessionStore
import org.scalajs.dom

object VisualEditorApp {

  // Sidebar tab state
  private val sidebarTabVar: Var[String] = Var("elements")

  // Resume popup state — non-empty list means popup is shown
  private val resumeSessionsVar: Var[List[EditorSession]] = Var(List.empty)

  private def closeResumeAndStartNew(): Unit = {
    val newId = ArtworkId.generate().value
    VisualEditorViewModel.startNewSession(newId)
    resumeSessionsVar.set(List.empty)
  }

  def apply(artworkId: Option[String] = None): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "calendar-builder-app",

      // Initialize on mount
      onMountCallback { _ =>
        // Check if we have a product context from the bridge
        val ctx = EditorBridge.consumeContext()
        ctx match
          case Some(productCtx) =>
            // Coming from product builder — initialize with product dimensions
            VisualEditorViewModel.initializeFromProduct(productCtx)
            artworkId.foreach(id => VisualEditorViewModel.setSessionId(id))
          case None =>
            artworkId match
              case Some(id) =>
                // Opening with a specific artwork ID — try to load from IndexedDB
                EditorSessionStore.load(id, {
                  case Some(session) => VisualEditorViewModel.loadSession(session)
                  case None =>
                    // New session with this ID
                    VisualEditorViewModel.startNewSession(id)
                })
              case None =>
                // Standalone open — check for in-progress sessions
                EditorSessionStore.listAll { sessions =>
                  if sessions.nonEmpty then
                    resumeSessionsVar.set(sessions)
                  else
                    val newId = ArtworkId.generate().value
                    VisualEditorViewModel.startNewSession(newId)
                }
      },

      // Update page titles when language changes
      lang.changes --> { language =>
        VisualEditorViewModel.updateLanguage(language.toCode)
      },

      // Resume popup overlay — shown when resumeSessionsVar is non-empty
      child.maybe <-- resumeSessionsVar.signal.map { sessions =>
        if sessions.nonEmpty then Some(
          ResumePopup(
            sessions = resumeSessionsVar.signal,
            lang = lang,
            onResume = { session =>
              VisualEditorViewModel.loadSession(session)
              resumeSessionsVar.set(List.empty)
            },
            onStartNew = closeResumeAndStartNew(),
            onDelete = { session =>
              EditorSessionStore.delete(session.id, () => {
                val updated = resumeSessionsVar.now().filter(_.id != session.id)
                resumeSessionsVar.set(updated)
                if updated.isEmpty then closeResumeAndStartNew()
              })
            },
            onClearAll = {
              EditorSessionStore.deleteAllSessions(() => {
                resumeSessionsVar.set(List.empty)
                val newId = ArtworkId.generate().value
                VisualEditorViewModel.startNewSession(newId)
              })
            },
          )
        ) else None
      },

      // Header
      div(
        cls := "calendar-header",
        div(
          cls := "calendar-header-top",
          h1(child.text <-- lang.map {
            case Language.En => "Visual Product Editor"
            case Language.Cs => "Vizuální editor produktů"
          }),
        ),
        p(child.text <-- lang.map {
          case Language.En => "Create your custom visual product — upload photos, add text, shapes and customize each page"
          case Language.Cs => "Vytvořte si vlastní vizuální produkt — nahrajte fotky, přidejte text, tvary a přizpůsobte každou stránku"
        }),
        // Session name input row with save indicator
        div(
          cls := "session-name-row",
          input(
            cls := "session-name-input",
            typ := "text",
            placeholder <-- lang.map {
              case Language.En => "Name your project..."
              case Language.Cs => "Pojmenujte svůj projekt..."
            },
            value <-- VisualEditorViewModel.sessionName.map(_.getOrElse("")),
            onInput.mapToValue --> { v => VisualEditorViewModel.setSessionName(v) },
          ),
          SaveIndicator(),
        ),
        // Product context info (when coming from product builder)
        child.maybe <-- VisualEditorViewModel.productContext.combineWith(lang).map { case (ctxOpt, l) =>
          ctxOpt.map { ctx =>
            div(
              cls := "product-context-info",
              span(cls := "context-badge", l match
                case Language.En => s"Product: ${ctx.categoryName.getOrElse("Custom")} — ${ctx.widthMm.toInt}×${ctx.heightMm.toInt} mm"
                case Language.Cs => s"Produkt: ${ctx.categoryName.getOrElse("Vlastní")} — ${ctx.widthMm.toInt}×${ctx.heightMm.toInt} mm"
              ),
            )
          }
        },
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
            // Disable when coming from product builder
            disabled <-- VisualEditorViewModel.productContext.map(_.isDefined),
            value <-- VisualEditorViewModel.productType.map {
              case VisualProductType.MonthlyCalendar  => "monthly"
              case VisualProductType.WeeklyCalendar   => "weekly"
              case VisualProductType.BiweeklyCalendar => "biweekly"
              case VisualProductType.PhotoBook        => "photobook"
              case VisualProductType.WallPicture      => "wallpicture"
              case VisualProductType.GenericProduct   => "generic"
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
            option(value := "generic", child.text <-- lang.map {
              case Language.En => "Custom Product"
              case Language.Cs => "Vlastní produkt"
            }),
            onChange.mapToValue --> { v =>
              val pt = v match {
                case "weekly"      => VisualProductType.WeeklyCalendar
                case "biweekly"    => VisualProductType.BiweeklyCalendar
                case "photobook"   => VisualProductType.PhotoBook
                case "wallpicture" => VisualProductType.WallPicture
                case "generic"     => VisualProductType.GenericProduct
                case _             => VisualProductType.MonthlyCalendar
              }
              VisualEditorViewModel.setProductType(pt)
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
          child <-- VisualEditorViewModel.state.combineWith(VisualEditorViewModel.productContext, lang).map { case (st, ctxOpt, language) =>
            val pt = st.productType
            val currentFmt = st.productFormat
            val formats = ProductFormat.formatsFor(pt)
            if ctxOpt.isDefined || formats.isEmpty then
              // Product context or generic product — show fixed format
              span(cls := "format-fixed", language match {
                case Language.En => s"${currentFmt.nameEn} (${currentFmt.widthMm}×${currentFmt.heightMm} mm)"
                case Language.Cs => s"${currentFmt.nameCs} (${currentFmt.widthMm}×${currentFmt.heightMm} mm)"
              })
            else
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
                  formats.find(_.id == selectedId).foreach(VisualEditorViewModel.setProductFormat)
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
              TabDef("gallery", lang.map {
                case Language.En => "Gallery"
                case Language.Cs => "Galerie"
              }, () => div(cls := "calendar-controls-card", ImageGalleryPanel())),
              TabDef("background", lang.map {
                case Language.En => "Background"
                case Language.Cs => "Pozadí"
              }, () => div(cls := "calendar-controls-card", BackgroundEditor())),
              TabDef("history", lang.map {
                case Language.En => "History"
                case Language.Cs => "Historie"
              }, () => div(cls := "calendar-controls-card", SessionHistoryPanel())),
            ),
            activeTab = sidebarTabVar,
          ),
        ),

        // Center: Canvas with product overlay
        div(
          cls := "calendar-canvas-area",
          EditorPageCanvas()
        ),
      ),

      // Page navigation strip
      PageNavigation(),

      // Footer with actions
      div(
        cls := "calendar-footer",

        // Return to Product Builder (when opened from product builder)
        child.maybe <-- VisualEditorViewModel.productContext.combineWith(lang).map { case (ctxOpt, l) =>
          ctxOpt.map { _ =>
            button(
              cls := "return-btn",
              child.text <-- lang.map {
                case Language.En => "← Return to Product Builder"
                case Language.Cs => "← Zpět na konfigurátor"
              },
              onClick --> { _ =>
                VisualEditorViewModel.commitElementChange() // trigger final save
                AppRouter.navigateTo(AppRoute.ProductBuilder)
              }
            )
          }
        },

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
              VisualEditorViewModel.reset()
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
