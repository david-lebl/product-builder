package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, ArtworkMode}
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.CheckboxField
import mpbuilder.uikit.util.Visibility

object ConfigurationForm:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      // Category Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "1. Select Product Category"
          case Language.Cs => "1. Vyberte kategorii produktu"
        }),
        CategorySelector(),
      ),

      // Printing Method Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "2. Select Printing Method"
          case Language.Cs => "2. Vyberte tiskovou metodu"
        }),
        PrintingMethodSelector(),
      ),

      // Component Configuration — dynamic sections based on category
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "3. Configure Components"
          case Language.Cs => "3. Specifikace výroby"
        }),
        children <-- ProductBuilderViewModel.componentRoles
          .combineWith(ProductBuilderViewModel.linkedComponents, lang)
          .map { case (roles, linked, l) =>
          if roles.isEmpty then
            List(
              p(cls := "info-box",
                l match
                  case Language.En => "Select a category to configure components"
                  case Language.Cs => "Vyberte kategorii pro konfiguraci komponentů"
              )
            )
          else if roles.size == 1 && roles.head == ComponentRole.Main then
            // Single-component product — no role header needed
            List(componentSection(ComponentRole.Main))
          else
            // Multi-component product — show linked toggle + conditional sections
            val toggle = div(
              cls := "linked-components-toggle",
              CheckboxField(
                label = ProductBuilderViewModel.currentLanguage.map {
                  case Language.En => "Same material and printing for all components"
                  case Language.Cs => "Stejný materiál a tisk pro všechny komponenty"
                },
                checked = ProductBuilderViewModel.linkedComponents,
                onChange = Observer[Boolean](v => ProductBuilderViewModel.setLinkedComponents(v)),
              ),
            )
            if linked then
              // Linked: shared material + ink section from the cover role, then per-component finishes
              val sharedSection = div(
                MaterialSelector(roles.head),
                InkConfigSelector(roles.head),
              )
              val finishSections = roles.map { role =>
                div(
                  cls := "component-section",
                  h4(componentRoleLabel(role, l)),
                  FinishSelector(role),
                )
              }
              toggle :: sharedSection :: finishSections
            else
              // Separate section per component
              toggle :: roles.map { role =>
                div(
                  cls := "component-section",
                  h4(componentRoleLabel(role, l)),
                  componentSection(role),
                )
              }
        },
      ),

      // Specifications
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "4. Product Specifications"
          case Language.Cs => "4. Specifikace produktu"
        }),
        SpecificationForm(),
      ),

      // Server Validate Button (price is computed live; this button reserved for future server-side validation)
      div(
        cls := "form-section",
        button(
          child.text <-- lang.map {
            case Language.En => "Validate price"
            case Language.Cs => "Ověřit cenu"
          },
          onClick --> { _ => ProductBuilderViewModel.validateConfiguration() },
        ),
      ),

      // Artwork section — visible when a valid configuration exists
      div(
        cls := "form-section artwork-section",
        Visibility.when(ProductBuilderViewModel.state.map(_.configuration.isDefined)),
        h3(child.text <-- lang.map {
          case Language.En => "5. Provide Artwork"
          case Language.Cs => "5. Poskytnutí dat"
        }),
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
                checked <-- ProductBuilderViewModel.state.map(_.artworkMode match
                  case ArtworkMode.UploadArtwork(_) => true
                  case _                            => false
                ),
                onChange --> { _ =>
                  ProductBuilderViewModel.setArtworkMode(ArtworkMode.UploadArtwork(None))
                },
              ),
              child.text <-- lang.map {
                case Language.En => " Upload Artwork"
                case Language.Cs => " Nahrát soubor"
              },
            ),
            div(
              cls := "upload-file-area",
              Visibility.when(ProductBuilderViewModel.state.map(_.artworkMode match
                case ArtworkMode.UploadArtwork(_) => true
                case _                            => false
              )),
              input(
                typ := "file",
                cls := "artwork-file-input",
                accept := ".pdf,.ai,.eps,.png,.jpg,.jpeg,.tiff,.psd",
                inContext { el =>
                  ProductBuilderViewModel.state.map(_.artworkMode).changes --> { mode =>
                    mode match
                      case ArtworkMode.UploadArtwork(None) => el.ref.value = ""
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
              child <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
                state.artworkMode match
                  case ArtworkMode.UploadArtwork(Some(fileName)) =>
                    span(cls := "uploaded-file-name", s"📎 $fileName")
                  case ArtworkMode.UploadArtwork(None) =>
                    span(cls := "upload-hint", l match
                      case Language.En => "Accepted formats: PDF, AI, EPS, PNG, JPG, TIFF, PSD"
                      case Language.Cs => "Povolené formáty: PDF, AI, EPS, PNG, JPG, TIFF, PSD"
                    )
                  case ArtworkMode.DesignInEditor(_) => emptyNode
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
                checked <-- ProductBuilderViewModel.state.map(_.artworkMode.isInstanceOf[ArtworkMode.DesignInEditor]),
                onChange --> { _ =>
                  ProductBuilderViewModel.setArtworkMode(ArtworkMode.DesignInEditor())
                },
              ),
              child.text <-- lang.map {
                case Language.En => " Design in Visual Editor"
                case Language.Cs => " Navrhnout ve vizuálním editoru"
              },
            ),
            div(
              cls := "open-editor-area",
              Visibility.when(ProductBuilderViewModel.state.map(_.artworkMode.isInstanceOf[ArtworkMode.DesignInEditor])),
              button(
                cls := "open-editor-btn",
                child.text <-- lang.map {
                  case Language.En => "Open Visual Editor →"
                  case Language.Cs => "Otevřít vizuální editor →"
                },
                onClick --> { _ =>
                  ProductBuilderViewModel.openInEditor()
                  AppRouter.navigateTo(AppRoute.CalendarBuilder)
                },
              ),
            ),
          ),
        ),
      ),

      // Add to Basket Button
      div(
        cls := "form-section",
        div(
          cls := "add-to-basket-section",
          label(child.text <-- lang.map {
            case Language.En => "Quantity to add:"
            case Language.Cs => "Množství k přidání:"
          }),
          input(
            typ := "number",
            minAttr := "1",
            value := "1",
            cls := "basket-quantity-input",
            idAttr := "basket-qty-input",
          ),
          button(
            cls := "add-to-basket-btn",
            disabled <-- ProductBuilderViewModel.state.map(_.configuration.isEmpty),
            child.text <-- lang.map {
              case Language.En => "Add to Basket"
              case Language.Cs => "Přidat do košíku"
            },
            onClick --> { _ =>
              val qtyInput = org.scalajs.dom.document.getElementById("basket-qty-input").asInstanceOf[org.scalajs.dom.html.Input]
              val qty = qtyInput.value.toIntOption.getOrElse(1)
              ProductBuilderViewModel.addToBasket(qty)
            },
          ),
        ),
      ),
    )

  private def componentSection(role: ComponentRole): Element =
    div(
      MaterialSelector(role),
      InkConfigSelector(role),
      FinishSelector(role),
    )

  private def componentRoleLabel(role: ComponentRole, lang: Language): String =
    role match
      case ComponentRole.Main => lang match
        case Language.En => "Main Component"
        case Language.Cs => "Hlavní komponent"
      case ComponentRole.Cover => lang match
        case Language.En => "Cover"
        case Language.Cs => "Obálka"
      case ComponentRole.Body => lang match
        case Language.En => "Body / Inner Pages"
        case Language.Cs => "Vnitřní část / stránky"
      case ComponentRole.Stand => lang match
        case Language.En => "Stand / Platform"
        case Language.Cs => "Stojánek / platforma"
