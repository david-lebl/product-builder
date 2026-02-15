package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object CategorySelector:
  def apply(): Element =
    val categories = ProductBuilderViewModel.catalog.categories
    
    div(
      cls := "form-group",
      label("Category:"),
      select(
        option("-- Select a category --", value := "", selected := true),
        categories.map { cat =>
          option(cat.name, value := cat.id.value)
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
        },
      ),
    )
