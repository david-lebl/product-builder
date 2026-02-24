package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.internal.model.*

object PrintingMethodSelector:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val optionsSignal = ProductBuilderViewModel.availablePrintingMethods.combineWith(lang).map {
      case (methods, l) => methods.map(m => SelectOption(m.id.value, m.name(l)))
    }
    val selectedValue = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId.map(_.value).getOrElse(""))
    val disabled = ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty)

    SelectField(
      labelSignal = lang.map {
        case Language.En => "Printing Method:"
        case Language.Cs => "Tisková metoda:"
      },
      placeholderSignal = lang.map {
        case Language.En => "-- Select a printing method --"
        case Language.Cs => "-- Vyberte tiskovou metodu --"
      },
      optionsSignal = optionsSignal,
      selectedValue = selectedValue,
      onSelect = value => ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value)),
      disabledSignal = disabled,
    )
