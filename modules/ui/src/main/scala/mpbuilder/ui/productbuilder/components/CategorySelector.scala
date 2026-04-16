package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.domain.model.*


object CategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.allCategories
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "form-group form-group--horizontal",
      div(
        cls := "label-with-help",
        label(child.text <-- lang.map {
          case Language.En => "Category:"
          case Language.Cs => "Kategorie:"
        }),
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
      div(
        cls := "form-group__control",
        select(
          children <-- lang.combineWith(selectedCategoryId).map { case (l, selOpt) =>
            val sel = selOpt.map(_.value).getOrElse("")
            val ph = l match
              case Language.En => "-- Select a category --"
              case Language.Cs => "-- Vyberte kategorii --"
            val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
            placeholderOpt ++ categories.map { cat =>
              option(cat.name(l), value := cat.id.value, com.raquo.laminar.api.L.selected := (cat.id.value == sel))
            }
          },
          onChange.mapToValue --> Observer[String] { value =>
            if value.nonEmpty then
              ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
          },
        ),
      ),
    )
