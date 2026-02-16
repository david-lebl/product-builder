package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object CategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.allCategories
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang = ProductBuilderViewModel.currentLanguage
    
    div(
      cls := "form-group",
      label(
        child.text <-- lang.map {
          case Language.En => "Category:"
          case Language.Cs => "Kategorie:"
        }
      ),
      select(
        children <-- selectedCategoryId.combineWith(lang).map { case (selectedId, l) =>
          val currentValue = selectedId.map(_.value).getOrElse("")
          option(
            l match
              case Language.En => "-- Select a category --"
              case Language.Cs => "-- Vyberte kategorii --"
            , value := "", selected := currentValue.isEmpty) ::
          categories.map { cat =>
            option(cat.name(l), value := cat.id.value, selected := (cat.id.value == currentValue))
          }
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
        },
      ),
    )
