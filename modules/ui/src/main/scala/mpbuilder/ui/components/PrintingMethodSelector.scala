package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    val lang = ProductBuilderViewModel.currentLanguage
    
    div(
      cls := "form-group",
      label(
        child.text <-- lang.map {
          case Language.En => "Printing Method:"
          case Language.Cs => "Tisková metoda:"
        }
      ),
      select(
        disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
        children <-- availableMethods.combineWith(selectedMethodId, lang).map { case (methods, selectedId, l) =>
          val currentValue = selectedId.map(_.value).getOrElse("")
          option(
            l match
              case Language.En => "-- Select a printing method --"
              case Language.Cs => "-- Vyberte tiskovou metodu --"
            , value := "", selected := currentValue.isEmpty) ::
          methods.map { method =>
            option(method.name(l), value := method.id.value, selected := (method.id.value == currentValue))
          }
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
        },
      ),
    )
