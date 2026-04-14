package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, ArtworkMode}
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.ui.visualeditor.EditorBridge
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{CheckboxField, SelectField, SelectOption}
import mpbuilder.uikit.util.Visibility

/** Compact configuration form for experienced employees.
  *
  * Same functionality as ConfigurationForm but:
  *   - All fields use inline (horizontal) layout via .compact-row wrapper
  *   - HelpInfo / info-box elements are omitted
  *   - Section headings are smaller
  */
object CompactConfigurationForm:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "compact-form",

      // 1. Category + Preset
      div(
        cls := "compact-section",
        h4(
          cls := "compact-section-title",
          child.text <-- lang.map {
            case Language.En => "1. Category"
            case Language.Cs => "1. Kategorie"
          },
        ),
        CompactCategorySelector(),
        CompactPresetSelector(),
      ),

      // 2. Product Specifications
      div(
        cls := "compact-section",
        h4(
          cls := "compact-section-title",
          child.text <-- lang.map {
            case Language.En => "2. Specifications"
            case Language.Cs => "2. Specifikace"
          },
        ),
        CompactSpecificationForm(),
      ),

      // 3. Printing Method
      div(
        cls := "compact-section",
        h4(
          cls := "compact-section-title",
          child.text <-- lang.map {
            case Language.En => "3. Printing Method"
            case Language.Cs => "3. Tisková metoda"
          },
        ),
        CompactPrintingMethodSelector(),
      ),

      // 4. Component Configuration
      div(
        cls := "compact-section",
        h4(
          cls := "compact-section-title",
          child.text <-- lang.map {
            case Language.En => "4. Components"
            case Language.Cs => "4. Komponenty"
          },
        ),
        children <-- ProductBuilderViewModel.componentRoles
          .combineWith(ProductBuilderViewModel.linkedComponents, lang)
          .map { case (roles, linked, l) =>
          if roles.isEmpty then
            List(
              span(cls := "compact-hint",
                l match
                  case Language.En => "Select a category first"
                  case Language.Cs => "Nejprve vyberte kategorii"
              )
            )
          else if roles.size == 1 && roles.head == ComponentRole.Main then
            List(componentSection(ComponentRole.Main))
          else
            val toggle = div(
              cls := "compact-row",
              CheckboxField(
                label = ProductBuilderViewModel.currentLanguage.map {
                  case Language.En => "Same material and printing for all"
                  case Language.Cs => "Stejný materiál a tisk pro vše"
                },
                checked = ProductBuilderViewModel.linkedComponents,
                onChange = Observer[Boolean](v => ProductBuilderViewModel.setLinkedComponents(v)),
              ),
            )
            if linked then
              val sharedSection = div(
                CompactMaterialSelector(roles.head),
                CompactInkConfigSelector(roles.head),
              )
              val finishSections = roles.map { role =>
                div(
                  cls := "compact-component",
                  span(cls := "compact-component-title", componentRoleLabel(role, l)),
                  CompactFinishSelector(role),
                )
              }
              toggle :: sharedSection :: finishSections
            else
              toggle :: roles.map { role =>
                div(
                  cls := "compact-component",
                  span(cls := "compact-component-title", componentRoleLabel(role, l)),
                  componentSection(role),
                )
              }
        },
      ),

      // 5. Manufacturing Speed
      div(
        cls := "compact-section",
        h4(
          cls := "compact-section-title",
          child.text <-- lang.map {
            case Language.En => "5. Speed"
            case Language.Cs => "5. Rychlost"
          },
        ),
        CompactSpecificationForm.manufacturingSpeedSection(),
      ),

      // Validate button
      div(
        cls := "compact-section",
        button(
          child.text <-- lang.map {
            case Language.En => "Validate price"
            case Language.Cs => "Ověřit cenu"
          },
          onClick --> { _ => ProductBuilderViewModel.validateConfiguration() },
        ),
      ),

      // Artwork section (visible when configuration is valid)
      div(
        cls := "compact-section",
        Visibility.when(ProductBuilderViewModel.state.map(_.configuration.isDefined)),
        h4(
          cls := "compact-section-title",
          child.text <-- lang.map {
            case Language.En => "6. Artwork"
            case Language.Cs => "6. Data"
          },
        ),
        div(
          cls := "compact-artwork",

          // Upload option
          label(
            cls := "compact-artwork-option",
            input(
              typ := "radio",
              nameAttr := "compactArtworkMode",
              value := "upload",
              checked <-- ProductBuilderViewModel.state.map(_.artworkMode match
                case ArtworkMode.UploadArtwork(_) => true
                case _                            => false
              ),
              onChange --> { _ =>
                ProductBuilderViewModel.setArtworkMode(ArtworkMode.UploadArtwork())
              },
            ),
            child.text <-- lang.map {
              case Language.En => " Upload"
              case Language.Cs => " Nahrát"
            },
          ),
          div(
            Visibility.when(ProductBuilderViewModel.state.map(_.artworkMode match
              case ArtworkMode.UploadArtwork(_) => true
              case _                            => false
            )),
            input(
              typ := "file",
              cls := "compact-file-input",
              accept := ".pdf,.ai,.eps,.png,.jpg,.jpeg,.tiff,.psd",
              inContext { el =>
                ProductBuilderViewModel.state.map(_.artworkMode).changes --> { mode =>
                  mode match
                    case ArtworkMode.UploadArtwork(None) | ArtworkMode.UploadArtwork(Some("")) => el.ref.value = ""
                    case _                               =>
                }
              },
              onChange --> { e =>
                val fileInput = e.target.asInstanceOf[org.scalajs.dom.html.Input]
                val fileName =
                  if fileInput.files.length > 0 then Some(fileInput.files(0).name)
                  else None
                ProductBuilderViewModel.setUploadedFileName(fileName)
              },
            ),
          ),

          // Design in editor option
          label(
            cls := "compact-artwork-option",
            input(
              typ := "radio",
              nameAttr := "compactArtworkMode",
              value := "design",
              checked <-- ProductBuilderViewModel.state.map(_.artworkMode match
                case _: ArtworkMode.DesignInEditor => true
                case _                             => false
              ),
              onChange --> { _ =>
                ProductBuilderViewModel.setArtworkMode(ArtworkMode.DesignInEditor())
              },
            ),
            child.text <-- lang.map {
              case Language.En => " Editor"
              case Language.Cs => " Editor"
            },
          ),
          div(
            Visibility.when(ProductBuilderViewModel.state.map(_.artworkMode match
              case _: ArtworkMode.DesignInEditor => true
              case _                             => false
            )),
            button(
              cls := "compact-editor-btn",
              child.text <-- lang.map {
                case Language.En => "Open Editor →"
                case Language.Cs => "Otevřít editor →"
              },
              onClick --> { _ =>
                val state = ProductBuilderViewModel.stateVar.now()
                state.configuration match
                  case Some(config) =>
                    val artworkId = state.artworkMode match
                      case ArtworkMode.DesignInEditor(Some(existing)) => existing
                      case _ => ArtworkId.generate()
                    ProductBuilderViewModel.setEditorArtworkId(artworkId)
                    EditorBridge.openEditorForProduct(config, artworkId)
                  case None =>
                    AppRouter.navigateTo(AppRoute.VisualEditor())
              },
            ),
          ),
        ),
      ),

      // Add to Basket
      div(
        cls := "compact-section",
        div(
          cls := "compact-basket-row",
          label(child.text <-- lang.map {
            case Language.En => "Qty:"
            case Language.Cs => "Ks:"
          }),
          input(
            typ := "number",
            minAttr := "1",
            value := "1",
            cls := "compact-qty-input",
            idAttr := "compact-basket-qty",
          ),
          button(
            cls := "add-to-basket-btn",
            disabled <-- ProductBuilderViewModel.state.map(_.configuration.isEmpty),
            child.text <-- lang.map {
              case Language.En => "Add to Basket"
              case Language.Cs => "Do košíku"
            },
            onClick --> { _ =>
              val qtyInput = org.scalajs.dom.document.getElementById("compact-basket-qty").asInstanceOf[org.scalajs.dom.html.Input]
              val qty = qtyInput.value.toIntOption.getOrElse(1)
              ProductBuilderViewModel.addToBasket(qty)
            },
          ),
        ),
      ),
    )

  private def componentSection(role: ComponentRole): Element =
    div(
      CompactMaterialSelector(role),
      CompactInkConfigSelector(role),
      CompactFinishSelector(role),
    )

  private def componentRoleLabel(role: ComponentRole, lang: Language): String =
    role match
      case ComponentRole.Main => lang match
        case Language.En => "Main"
        case Language.Cs => "Hlavní"
      case ComponentRole.Cover => lang match
        case Language.En => "Cover"
        case Language.Cs => "Obálka"
      case ComponentRole.Body => lang match
        case Language.En => "Body"
        case Language.Cs => "Vnitřní část"
      case ComponentRole.Stand => lang match
        case Language.En => "Stand"
        case Language.Cs => "Stojánek"
