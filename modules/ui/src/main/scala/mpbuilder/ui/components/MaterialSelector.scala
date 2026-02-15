package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object MaterialSelector:
  def apply(): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials
    val selectedMaterialId = ProductBuilderViewModel.state.map(_.selectedMaterialId)
    val lang = ProductBuilderViewModel.currentLanguage
    
    div(
      cls := "form-group",
      label(
        child.text <-- lang.map {
          case Language.En => "Material:"
          case Language.Cs => "Materiál:"
        }
      ),
      select(
        disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
        children <-- availableMaterials.combineWith(selectedMaterialId, lang).map { case (materials, selectedId, l) =>
          val currentValue = selectedId.map(_.value).getOrElse("")
          option(
            l match
              case Language.En => "-- Select a material --"
              case Language.Cs => "-- Vyberte materiál --"
            , value := "", selected := currentValue.isEmpty) ::
          materials.map { mat =>
            option(mat.name(l), value := mat.id.value, selected := (mat.id.value == currentValue))
          }
        },
        onChange.mapToValue --> { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectMaterial(MaterialId.unsafe(value))
        },
      ),
      div(
        cls := "info-box",
        child.maybe <-- availableMaterials.combineWith(lang).map { case (materials, l) =>
          materials.headOption.map { _ =>
            span(
              l match
                case Language.En => s"${materials.size} material(s) available for this category"
                case Language.Cs => s"${materials.size} materiál(ů) dostupných pro tuto kategorii"
            )
          }
        },
      ),
    )
