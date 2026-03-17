package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    val lang = ProductBuilderViewModel.currentLanguage

    val selectedDescription: Signal[Option[LocalizedString]] =
      selectedMethodId.combineWith(availableMethods).map { case (selId, methods) =>
        selId.flatMap(id => methods.find(_.id == id)).flatMap(_.description)
      }

    div(
      SelectField(
        label = lang.map {
          case Language.En => "Printing Method:"
          case Language.Cs => "Tisková metoda:"
        },
        options = availableMethods.combineWith(lang).map { case (methods, l) =>
          methods.map(method => SelectOption(method.id.value, method.name(l)))
        },
        selected = selectedMethodId.map(_.map(_.value).getOrElse("")),
        onChange = Observer[String] { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
        },
        placeholder = lang.map {
          case Language.En => "-- Select a printing method --"
          case Language.Cs => "-- Vyberte tiskovou metodu --"
        },
        disabled = ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
      ),
      HelpInfo.fromSignal(selectedDescription, lang),
    )
