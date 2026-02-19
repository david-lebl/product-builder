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
      
      // Material Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "2. Select Material"
          case Language.Cs => "2. Vyberte materiál"
        }),
        MaterialSelector(),
      ),
      
      // Printing Method Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "3. Select Printing Method"
          case Language.Cs => "3. Vyberte tiskovou metodu"
        }),
        PrintingMethodSelector(),
      ),
      
      // Finish Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "4. Select Finishes (Optional)"
          case Language.Cs => "4. Vyberte povrchové úpravy (volitelné)"
        }),
        FinishSelector(),
      ),
      
      // Specifications
      div(
        cls := "form-section",
        h3(child.text <-- lang.map {
          case Language.En => "5. Product Specifications"
          case Language.Cs => "5. Specifikace produktu"
        }),
        SpecificationForm(),
      ),
      
      // Server Validate Button (price is computed live; this button reserved for future server-side validation)
      div(
        cls := "form-section",
        button(
          child.text <-- lang.map {
            case Language.En => "Validate with Server"
            case Language.Cs => "Ověřit na serveru"
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
