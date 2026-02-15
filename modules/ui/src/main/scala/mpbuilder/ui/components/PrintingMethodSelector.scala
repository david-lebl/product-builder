package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    
    div(
      cls := "form-group",
      label("Printing Method:"),
      select(
        disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
        children <-- availableMethods.combineWith(selectedMethodId).map { case (methods, selectedId) =>
          val currentValue = selectedId.map(_.value).getOrElse("")
          option("-- Select a printing method --", value := "", selected := currentValue.isEmpty) ::
          methods.map { method =>
            option(method.name, value := method.id.value, selected := (method.id.value == currentValue))
          }
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
        },
      ),
    )
