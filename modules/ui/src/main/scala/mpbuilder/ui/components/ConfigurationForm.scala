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
    )
