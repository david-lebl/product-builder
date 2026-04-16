package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.domain.model.*

object PrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "form-group form-group--horizontal",
      div(
        cls := "label-with-help",
        label(child.text <-- lang.map {
          case Language.En => "Print method:"
          case Language.Cs => "Tisková metoda:"
        }),
        HelpInfo(lang.map {
          case Language.En => "How the ink is applied to the material. Different methods vary in quality, cost, and suitability for different materials and quantities."
          case Language.Cs => "Způsob aplikace barvy na materiál. Různé metody se liší kvalitou, náklady a vhodností pro různé materiály a množství."
        }),
        HelpInfo.fromSignal(
          selectedMethodId.combineWith(lang).map { case (pmIdOpt, l) =>
            pmIdOpt.flatMap(id => catalog.printingMethods.get(id)).flatMap(_.description).map(_(l))
          }
        ),
      ),
      div(
        cls := "form-group__control",
        select(
          disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
          children <-- availableMethods.combineWith(lang, selectedMethodId).map { case (methods, l, selOpt) =>
            val sel = selOpt.map(_.value).getOrElse("")
            val ph = l match
              case Language.En => "-- Select a printing method --"
              case Language.Cs => "-- Vyberte tiskovou metodu --"
            val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
            placeholderOpt ++ methods.map { method =>
              option(method.name(l), value := method.id.value, com.raquo.laminar.api.L.selected := (method.id.value == sel))
            }
          },
          onChange.mapToValue --> Observer[String] { value =>
            if value.nonEmpty then
              ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
          },
        ),
      ),
    )
