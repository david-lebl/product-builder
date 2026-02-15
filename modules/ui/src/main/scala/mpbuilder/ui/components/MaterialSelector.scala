package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object MaterialSelector:
  def apply(): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials
    
    div(
      cls := "form-group",
      label("Material:"),
      select(
        disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
        option("-- Select a material --", value := "", selected := true),
        children <-- availableMaterials.map { materials =>
          materials.map { mat =>
            option(mat.name, value := mat.id.value)
          }
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectMaterial(MaterialId.unsafe(value))
        },
      ),
      div(
        cls := "info-box",
        child.maybe <-- availableMaterials.map { materials =>
          materials.headOption.map { _ =>
            span(s"${materials.size} material(s) available for this category")
          }
        },
      ),
    )
