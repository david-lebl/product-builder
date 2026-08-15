package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ArtworkIntegration, ProductBuilderViewModel}
import mpbuilder.ui.visualeditor.EditorBridge
import mpbuilder.domain.model.*
import mpbuilder.uikit.util.Visibility

/** How the customer will provide artwork for the configured product */
sealed trait ArtworkMode
object ArtworkMode:
  case class UploadArtwork(fileName: Option[String] = None) extends ArtworkMode
  case class DesignInEditor(artworkId: Option[ArtworkId] = None) extends ArtworkMode

/** The full SPA's artwork step: upload a file, or design it in the visual editor.
  *
  * State lives here rather than in `BuilderState` because the standalone
  * calculator has no visual editor and no way to transfer a file — it supplies
  * no `ArtworkIntegration` at all and the section is simply not rendered.
  */
object VisualEditorArtwork extends ArtworkIntegration:

  private val modeVar: Var[ArtworkMode] = Var(ArtworkMode.UploadArtwork())
  private val basketItemArtworkVar: Var[Map[ConfigurationId, ArtworkMode]] = Var(Map.empty)

  val mode: Signal[ArtworkMode] = modeVar.signal

  def setMode(m: ArtworkMode): Unit = modeVar.set(m)
  def setUploadedFileName(name: Option[String]): Unit = modeVar.set(ArtworkMode.UploadArtwork(name))
  def setEditorArtworkId(artworkId: ArtworkId): Unit =
    modeVar.set(ArtworkMode.DesignInEditor(Some(artworkId)))

  override def onAddedToBasket(config: ProductConfiguration): Unit =
    basketItemArtworkVar.update(_ + (config.id -> modeVar.now()))

  override def onRemovedFromBasket(configId: ConfigurationId): Unit =
    basketItemArtworkVar.update(_ - configId)

  override def onBasketCleared(): Unit =
    basketItemArtworkVar.set(Map.empty)

  override def onFormReset(): Unit =
    modeVar.set(ArtworkMode.UploadArtwork())

  override def emailSummary(configId: ConfigurationId, lang: Language): Option[String] =
    basketItemArtworkVar.now().get(configId).map {
      case ArtworkMode.UploadArtwork(Some(fileName)) => lang match
        case Language.En => s"Artwork: $fileName"
        case Language.Cs => s"Data: $fileName"
      case ArtworkMode.UploadArtwork(None) => lang match
        case Language.En => "Artwork: not uploaded yet"
        case Language.Cs => "Data: ještě nenahrána"
      case _: ArtworkMode.DesignInEditor => lang match
        case Language.En => "Design: created in Visual Editor"
        case Language.Cs => "Design: vytvořen ve vizuálním editoru"
    }

  override def renderBasketItem(configId: ConfigurationId, lang: Language): Element =
    div(
      child <-- basketItemArtworkVar.signal.map { byConfig =>
        byConfig.get(configId) match
          case Some(ArtworkMode.UploadArtwork(Some(fileName))) =>
            span(lang match
              case Language.En => s"📎 Artwork: $fileName"
              case Language.Cs => s"📎 Data: $fileName"
            )
          case Some(ArtworkMode.UploadArtwork(None)) =>
            span(cls := "artwork-pending", lang match
              case Language.En => "📎 Artwork: not uploaded yet"
              case Language.Cs => "📎 Data: ještě nenahrána"
            )
          case Some(ArtworkMode.DesignInEditor(Some(artworkId))) =>
            div(
              span(lang match
                case Language.En => "🎨 Design: created in Visual Editor"
                case Language.Cs => "🎨 Design: vytvořen ve vizuálním editoru"
              ),
              button(
                cls := "edit-design-btn",
                lang match
                  case Language.En => "Edit Design"
                  case Language.Cs => "Upravit design"
                ,
                onClick --> { _ =>
                  AppRouter.navigateTo(AppRoute.VisualEditor(Some(artworkId.value)))
                },
              ),
            )
          case Some(ArtworkMode.DesignInEditor(None)) =>
            span(lang match
              case Language.En => "🎨 Design: created in Visual Editor"
              case Language.Cs => "🎨 Design: vytvořen ve vizuálním editoru"
            )
          case None => emptyNode
      },
    )

  override def render(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "artwork-options",

      // Upload Artwork option
      div(
        cls := "artwork-option",
        label(
          cls := "artwork-option-label",
          input(
            typ := "radio",
            nameAttr := "artworkMode",
            value := "upload",
            checked <-- mode.map {
              case ArtworkMode.UploadArtwork(_) => true
              case _                            => false
            },
            onChange --> { _ => setMode(ArtworkMode.UploadArtwork()) },
          ),
          child.text <-- lang.map {
            case Language.En => " Upload Artwork"
            case Language.Cs => " Nahrát soubor"
          },
        ),
        div(
          cls := "upload-file-area",
          Visibility.when(mode.map {
            case ArtworkMode.UploadArtwork(_) => true
            case _                            => false
          }),
          input(
            typ := "file",
            cls := "artwork-file-input",
            accept := ".pdf,.ai,.eps,.png,.jpg,.jpeg,.tiff,.psd",
            inContext { el =>
              mode.changes --> { m =>
                m match
                  case ArtworkMode.UploadArtwork(None) | ArtworkMode.UploadArtwork(Some("")) => el.ref.value = ""
                  case _                               =>
              }
            },
            onChange --> { e =>
              val fileInput = e.target.asInstanceOf[org.scalajs.dom.html.Input]
              val fileName =
                if fileInput.files.length > 0 then Some(fileInput.files(0).name)
                else None
              setUploadedFileName(fileName)
            },
          ),
          child <-- mode.combineWith(lang).map { case (m, l) =>
            m match
              case ArtworkMode.UploadArtwork(Some(fileName)) =>
                span(cls := "uploaded-file-name", s"📎 $fileName")
              case ArtworkMode.UploadArtwork(None) =>
                span(cls := "upload-hint", l match
                  case Language.En => "Accepted formats: PDF, AI, EPS, PNG, JPG, TIFF, PSD"
                  case Language.Cs => "Povolené formáty: PDF, AI, EPS, PNG, JPG, TIFF, PSD"
                )
              case _: ArtworkMode.DesignInEditor => emptyNode
          },
        ),
      ),

      // Design in Visual Editor option
      div(
        cls := "artwork-option",
        label(
          cls := "artwork-option-label",
          input(
            typ := "radio",
            nameAttr := "artworkMode",
            value := "design",
            checked <-- mode.map {
              case _: ArtworkMode.DesignInEditor => true
              case _                             => false
            },
            onChange --> { _ => setMode(ArtworkMode.DesignInEditor()) },
          ),
          child.text <-- lang.map {
            case Language.En => " Design in Visual Editor"
            case Language.Cs => " Navrhnout ve vizuálním editoru"
          },
        ),
        div(
          cls := "open-editor-area",
          Visibility.when(mode.map {
            case _: ArtworkMode.DesignInEditor => true
            case _                             => false
          }),
          button(
            cls := "open-editor-btn",
            child.text <-- lang.map {
              case Language.En => "Open Visual Editor →"
              case Language.Cs => "Otevřít vizuální editor →"
            },
            onClick --> { _ =>
              ProductBuilderViewModel.stateVar.now().configuration match
                case Some(config) =>
                  val artworkId = modeVar.now() match
                    case ArtworkMode.DesignInEditor(Some(existing)) => existing
                    case _                                          => ArtworkId.generate()
                  setEditorArtworkId(artworkId)
                  EditorBridge.openEditorForProduct(config, artworkId)
                case None =>
                  AppRouter.navigateTo(AppRoute.VisualEditor())
            },
          ),
        ),
      ),
    )
