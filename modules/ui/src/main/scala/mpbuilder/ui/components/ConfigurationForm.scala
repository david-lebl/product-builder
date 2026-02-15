package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object ConfigurationForm:
  def apply(): Element =
    div(
      // Category Selection
      div(
        cls := "form-section",
        h3("1. Select Product Category"),
        CategorySelector(),
      ),
      
      // Material Selection
      div(
        cls := "form-section",
        h3("2. Select Material"),
        MaterialSelector(),
      ),
      
      // Printing Method Selection
      div(
        cls := "form-section",
        h3("3. Select Printing Method"),
        PrintingMethodSelector(),
      ),
      
      // Finish Selection
      div(
        cls := "form-section",
        h3("4. Select Finishes (Optional)"),
        FinishSelector(),
      ),
      
      // Specifications
      div(
        cls := "form-section",
        h3("5. Product Specifications"),
        SpecificationForm(),
      ),
      
      // Validate Button
      div(
        cls := "form-section",
        button(
          "Calculate Price",
          onClick --> { _ => ProductBuilderViewModel.validateConfiguration() },
        ),
      ),
    )
