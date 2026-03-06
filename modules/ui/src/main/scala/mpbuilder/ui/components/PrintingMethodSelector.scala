package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uicommon.FormSelect
import mpbuilder.uicommon.FormSelect.SelectOption

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    val lang = ProductBuilderViewModel.currentLanguage

    FormSelect(
      labelMod = child.text <-- lang.map {
        case Language.En => "Printing Method:"
        case Language.Cs => "Tisková metoda:"
      },
      optionsSignal = availableMethods.combineWith(selectedMethodId, lang).map { case (methods, selectedId, l) =>
        val currentValue = selectedId.map(_.value).getOrElse("")
        SelectOption(
          text = l match
            case Language.En => "-- Select a printing method --"
            case Language.Cs => "-- Vyberte tiskovou metodu --"
          ,
          optionValue = "",
          isSelected = currentValue.isEmpty,
        ) :: methods.map { method =>
          SelectOption(method.name(l), method.id.value, method.id.value == currentValue)
        }
      },
      onValueChange = { value =>
        if value.nonEmpty then
          ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
      },
      disabledSignal = ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
    )
