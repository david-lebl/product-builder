package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

/** Compact material selector — inline label, no help or info boxes. */
object CompactMaterialSelector:
  def apply(role: ComponentRole): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials(role)
    val selectedMaterialId = ProductBuilderViewModel.selectedMaterialId(role)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "compact-row",
      label(
        cls := "compact-label",
        child.text <-- lang.map {
          case Language.En => "Material:"
          case Language.Cs => "Materiál:"
        },
      ),
      div(
        cls := "compact-field",
        select(
          disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
          children <-- availableMaterials.combineWith(lang, selectedMaterialId).map { case (materials, l, sel) =>
            val ph = option(
              l match
                case Language.En => "-- Select --"
                case Language.Cs => "-- Vyberte --",
              value := "",
              selected := sel.isEmpty,
            )
            ph :: materials.map { mat =>
              option(
                mat.name(l),
                value := mat.id.value,
                selected := sel.contains(mat.id),
              )
            }
          },
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              ProductBuilderViewModel.selectMaterial(role, MaterialId.unsafe(value))
          },
        ),
      ),
    )
