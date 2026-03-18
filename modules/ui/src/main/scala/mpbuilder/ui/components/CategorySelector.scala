package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object CategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.allCategories
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      div(
        cls := "label-with-help",
        label(child.text <-- lang.map {
          case Language.En => "Category:"
          case Language.Cs => "Kategorie:"
        }),
        child.maybe <-- ProductBuilderViewModel.selectedCategoryDescription.combineWith(lang).map {
          case (Some(desc), l) => Some(HelpInfo(desc, l))
          case _ => None
        },
      ),
      SelectField(
        label = Val(""),
        options = lang.map { l =>
          categories.map(cat => SelectOption(cat.id.value, cat.name(l)))
        },
        selected = selectedCategoryId.map(_.map(_.value).getOrElse("")),
        onChange = Observer[String] { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
        },
        placeholder = lang.map {
          case Language.En => "-- Select a category --"
          case Language.Cs => "-- Vyberte kategorii --"
        },
      ),
    )
