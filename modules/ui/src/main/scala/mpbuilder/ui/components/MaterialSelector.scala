package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object MaterialSelector:
  def apply(role: ComponentRole): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials(role)
    val selectedMaterialId = ProductBuilderViewModel.selectedMaterialId(role)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      SelectField(
        label = lang.map {
          case Language.En => "Material:"
          case Language.Cs => "Materiál:"
        },
        options = availableMaterials.combineWith(lang).map { case (materials, l) =>
          materials.map(mat => SelectOption(mat.id.value, mat.name(l)))
        },
        selected = selectedMaterialId.map(_.map(_.value).getOrElse("")),
        onChange = Observer[String] { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectMaterial(role, MaterialId.unsafe(value))
        },
        placeholder = lang.map {
          case Language.En => "-- Select a material --"
          case Language.Cs => "-- Vyberte materiál --"
        },
        disabled = ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
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
