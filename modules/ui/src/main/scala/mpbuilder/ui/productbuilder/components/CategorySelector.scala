package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.ui.components.HelpInfo
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object CategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.allCategories
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "selector-with-help",
      SelectField(
        label = lang.map {
          case Language.En => "Category:"
          case Language.Cs => "Kategorie:"
        },
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
      div(
        cls := "selector-help-buttons",
        HelpInfo(lang.map {
          case Language.En => "The product category determines which materials, finishes, and printing methods are available. Each category has its own set of configuration options."
          case Language.Cs => "Kategorie produktu určuje, které materiály, povrchové úpravy a tiskové metody jsou k dispozici. Každá kategorie má vlastní sadu konfiguračních možností."
        }),
        HelpInfo.fromSignal(
          selectedCategoryId.combineWith(lang).map { case (catIdOpt, l) =>
            catIdOpt.flatMap(id => catalog.categories.get(id)).flatMap(_.description).map(_(l))
          }
        ),
      ),
    )
