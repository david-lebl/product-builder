package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

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
      horizontal = true,
      helpContent = Some(lang.map {
        case Language.En => "How the ink is applied to the material. Different methods vary in quality, cost, and suitability for different materials and quantities."
        case Language.Cs => "Způsob aplikace barvy na materiál. Různé metody se liší kvalitou, náklady a vhodností pro různé materiály a množství."
      }),
      detailHelp = Some(selectedMethodId.combineWith(lang).map { case (pmIdOpt, l) =>
        pmIdOpt.flatMap(id => catalog.printingMethods.get(id)).flatMap(_.description).map(_(l))
      }),
    )
