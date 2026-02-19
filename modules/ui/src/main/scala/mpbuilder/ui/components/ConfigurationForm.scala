package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object ConfigurationForm:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val isMulti = ProductBuilderViewModel.isMultiComponent

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

      // Single-component: Material Selection (hidden for multi-component)
      div(
        cls := "form-section",
        display <-- isMulti.map(if _ then "none" else "block"),
        h3(child.text <-- lang.map {
          case Language.En => "2. Select Material"
          case Language.Cs => "2. Vyberte materiál"
        }),
        MaterialSelector(),
      ),
      
      // Printing Method Selection
      div(
        cls := "form-section",
        h3(child.text <-- lang.combineWith(isMulti).map { case (l, multi) =>
          val step = if multi then "2" else "3"
          l match
            case Language.En => s"$step. Select Printing Method"
            case Language.Cs => s"$step. Vyberte tiskovou metodu"
        }),
        PrintingMethodSelector(),
      ),
      
      // Multi-component: Component editors
      div(
        cls := "form-section",
        display <-- isMulti.map(if _ then "block" else "none"),
        h3(child.text <-- lang.map {
          case Language.En => "3. Configure Components"
          case Language.Cs => "3. Nastavení komponent"
        }),
        div(
          cls := "info-box",
          p(child.text <-- lang.map {
            case Language.En => "This product type has separate cover and body components with independent material, finish, and ink settings."
            case Language.Cs => "Tento typ produktu má samostatnou obálku a vnitřní stránky s nezávislým nastavením materiálu, úprav a inkoustu."
          }),
        ),
        div(
          cls := "component-editors",
          children <-- ProductBuilderViewModel.componentRoles.map { roles =>
            // Sort: Cover first, then Body
            val sortedRoles = roles.toList.sortBy {
              case ComponentRole.Cover => 0
              case ComponentRole.Body => 1
            }
            sortedRoles.map(role => ComponentEditor(role))
          },
        ),
      ),

      // Single-component: Finish Selection (hidden for multi-component)
      div(
        cls := "form-section",
        display <-- isMulti.map(if _ then "none" else "block"),
        h3(child.text <-- lang.map {
          case Language.En => "4. Select Finishes (Optional)"
          case Language.Cs => "4. Vyberte povrchové úpravy (volitelné)"
        }),
        FinishSelector(),
      ),
      
      // Specifications
      div(
        cls := "form-section",
        h3(child.text <-- lang.combineWith(isMulti).map { case (l, multi) =>
          val step = if multi then "4" else "5"
          l match
            case Language.En => s"$step. Product Specifications"
            case Language.Cs => s"$step. Specifikace produktu"
        }),
        SpecificationForm(),
      ),
      
      // Validate Button
      div(
        cls := "form-section",
        button(
          child.text <-- lang.map {
            case Language.En => "Calculate Price"
            case Language.Cs => "Vypočítat cenu"
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
