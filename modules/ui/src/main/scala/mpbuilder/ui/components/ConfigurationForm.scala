package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

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
          case Language.Cs => "3. Nakonfigurujte komponenty"
        }),
        children <-- ProductBuilderViewModel.componentRoles.combineWith(lang).map { case (roles, l) =>
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
            // Multi-component product — show labeled sections
            roles.map { role =>
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
