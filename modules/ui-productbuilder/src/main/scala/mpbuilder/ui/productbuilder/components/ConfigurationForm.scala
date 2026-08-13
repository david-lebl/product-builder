package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, BuilderEnvironment}
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.CheckboxField
import mpbuilder.uikit.util.Visibility

object ConfigurationForm:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      // 1. Category Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "1. Select Product Category"
          case Language.Cs => "1. Vyberte kategorii produktu"
        }),
        CategorySelector(),
        PresetSelector(),
      ),

      // 2. Product Specifications (quantity, size, pages, orientation, fold type, binding)
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "2. Product Specifications"
          case Language.Cs => "2. Specifikace produktu"
        }),
        SpecificationForm(),
      ),

      // 3. Printing Method Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "3. Select Printing Method"
          case Language.Cs => "3. Vyberte tiskovou metodu"
        }),
        PrintingMethodSelector(),
      ),

      // 4. Component Configuration — dynamic sections based on category
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "4. Configure Components"
          case Language.Cs => "4. Specifikace výroby"
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
              cls := "linked-toggle",
              CheckboxField(
                label = ProductBuilderViewModel.currentLanguage.map {
                  case Language.En => "Same material and printing for all components"
                  case Language.Cs => "Stejný materiál a tisk pro všechny komponenty"
                },
                checked = ProductBuilderViewModel.linkedComponents,
                onChange = Observer[Boolean](v => ProductBuilderViewModel.setLinkedComponents(v)),
              ),
              span(
                cls := "linked-toggle__hint",
                child.text <-- ProductBuilderViewModel.currentLanguage.map {
                  case Language.En => "Shared settings apply to all components at once"
                  case Language.Cs => "Sdílené nastavení se aplikuje na všechny komponenty najednou"
                },
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

      // 5. Manufacturing Speed
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "5. Manufacturing Speed"
          case Language.Cs => "5. Rychlost výroby"
        }),
        SpecificationForm.manufacturingSpeedSection(),
      ),

      // Artwork section — only when the host provides an artwork integration,
      // and only once a valid configuration exists
      BuilderEnvironment.get.artwork match
        case Some(artwork) =>
          div(
            cls := "form-section artwork-section",
            Visibility.when(ProductBuilderViewModel.state.map(_.configuration.isDefined)),
            h3(child.text <-- lang.map {
              case Language.En => "6. Provide Artwork"
              case Language.Cs => "6. Poskytnutí dat"
            }),
            artwork.render(),
          )
        case None => emptyNode,

      // Add to Basket / order this one item straight away
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
          div(
            cls := "add-to-basket-actions",
            button(
              cls := "add-to-basket-btn",
              disabled <-- ProductBuilderViewModel.state.map(_.configuration.isEmpty),
              child.text <-- lang.map {
                case Language.En => "Add to Basket"
                case Language.Cs => "Přidat do košíku"
              },
              onClick --> { _ => ProductBuilderViewModel.addToBasket(quantityToAdd()) },
            ),
            // Skips the basket entirely: order just this configuration by e-mail.
            // Deliberately *not* disabled on an invalid configuration — the point
            // of ordering by e-mail is to cover what the configurator cannot, and
            // the message carries any validation issues along with it.
            button(
              cls := "order-single-btn",
              child.text <-- lang.map {
                case Language.En => "✉ Order this by e-mail"
                case Language.Cs => "✉ Objednat e-mailem"
              },
              onClick --> { _ => EmailOrderModal.open(quantityToAdd()) },
            ),
          ),
        ),
      ),
    )

  /** The "Quantity to add" field, defaulting to 1 when empty or unparseable. */
  private def quantityToAdd(): Int =
    Option(org.scalajs.dom.document.getElementById("basket-qty-input"))
      .map(_.asInstanceOf[org.scalajs.dom.html.Input].value)
      .flatMap(_.toIntOption)
      .filter(_ > 0)
      .getOrElse(1)

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
