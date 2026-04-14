package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

/** Compact printing method selector — inline label, no help or info. */
object CompactPrintingMethodSelector:
  def apply(): Element =
    val availableMethods = ProductBuilderViewModel.availablePrintingMethods
    val selectedMethodId = ProductBuilderViewModel.state.map(_.selectedPrintingMethodId)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "compact-row",
      label(
        cls := "compact-label",
        child.text <-- lang.map {
          case Language.En => "Printing:"
          case Language.Cs => "Tisk:"
        },
      ),
      div(
        cls := "compact-field",
        select(
          disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
          children <-- availableMethods.combineWith(lang, selectedMethodId).map { case (methods, l, sel) =>
            val ph = option(
              l match
                case Language.En => "-- Select --"
                case Language.Cs => "-- Vyberte --",
              value := "",
              selected := sel.isEmpty,
            )
            ph :: methods.map { method =>
              option(
                method.name(l),
                value := method.id.value,
                selected := sel.contains(method.id),
              )
            }
          },
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              ProductBuilderViewModel.selectPrintingMethod(PrintingMethodId.unsafe(value))
          },
        ),
      ),
    )
