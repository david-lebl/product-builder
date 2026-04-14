package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

/** Compact category selector — inline label, no help buttons or descriptions. */
object CompactCategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.allCategories
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "compact-row",
      label(
        cls := "compact-label",
        child.text <-- lang.map {
          case Language.En => "Category:"
          case Language.Cs => "Kategorie:"
        },
      ),
      div(
        cls := "compact-field",
        select(
          children <-- lang.combineWith(selectedCategoryId).map { case (l, sel) =>
            val ph = option(
              l match
                case Language.En => "-- Select --"
                case Language.Cs => "-- Vyberte --",
              value := "",
              selected := sel.isEmpty,
            )
            ph :: categories.map { cat =>
              option(
                cat.name(l),
                value := cat.id.value,
                selected := sel.contains(cat.id),
              )
            }
          },
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
          },
        ),
      ),
    )
