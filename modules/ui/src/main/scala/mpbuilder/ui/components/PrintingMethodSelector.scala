package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    
    div(
      cls := "form-group",
      label("Printing Method:"),
      select(
        disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
        option("-- Select a printing method --", value := "", selected := true),
        children <-- availableMethods.map { methods =>
          methods.map { method =>
            option(method.name, value := method.id.value)
          }
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
        },
      ),
    )
