package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.internal.model.*

object CategorySelector:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val optionsSignal = lang.map { l =>
      ProductBuilderViewModel.allCategories.map(cat => SelectOption(cat.id.value, cat.name(l)))
    }
    val selectedValue = ProductBuilderViewModel.state.map(_.selectedCategoryId.map(_.value).getOrElse(""))

    SelectField(
      labelSignal = lang.map {
        case Language.En => "Category:"
        case Language.Cs => "Kategorie:"
      },
      placeholderSignal = lang.map {
        case Language.En => "-- Select a category --"
        case Language.Cs => "-- Vyberte kategorii --"
      },
      optionsSignal = optionsSignal,
      selectedValue = selectedValue,
      onSelect = value => ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value)),
    )
