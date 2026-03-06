package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uicommon.FormSelect
import mpbuilder.uicommon.FormSelect.SelectOption

object CategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.allCategories
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang = ProductBuilderViewModel.currentLanguage

    FormSelect(
      labelMod = child.text <-- lang.map {
        case Language.En => "Category:"
        case Language.Cs => "Kategorie:"
      },
      optionsSignal = selectedCategoryId.combineWith(lang).map { case (selectedId, l) =>
        val currentValue = selectedId.map(_.value).getOrElse("")
        SelectOption(
          text = l match
            case Language.En => "-- Select a category --"
            case Language.Cs => "-- Vyberte kategorii --"
          ,
          optionValue = "",
          isSelected = currentValue.isEmpty,
        ) :: categories.map { cat =>
          SelectOption(cat.name(l), cat.id.value, cat.id.value == currentValue)
        }
      },
      onValueChange = { value =>
        if value.nonEmpty then
          ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
      },
    )
