package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uicommon.FormSelect
import mpbuilder.uicommon.FormSelect.SelectOption

object MaterialSelector:
  def apply(role: ComponentRole): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials(role)
    val selectedMaterialId = ProductBuilderViewModel.selectedMaterialId(role)
    val lang = ProductBuilderViewModel.currentLanguage

    FormSelect.withInfo(
      labelMod = child.text <-- lang.map {
        case Language.En => "Material:"
        case Language.Cs => "Materiál:"
      },
      optionsSignal = availableMaterials.combineWith(selectedMaterialId, lang).map { case (materials, selectedId, l) =>
        val currentValue = selectedId.map(_.value).getOrElse("")
        SelectOption(
          text = l match
            case Language.En => "-- Select a material --"
            case Language.Cs => "-- Vyberte materiál --"
          ,
          optionValue = "",
          isSelected = currentValue.isEmpty,
        ) :: materials.map { mat =>
          SelectOption(mat.name(l), mat.id.value, mat.id.value == currentValue)
        }
      },
      onValueChange = { value =>
        if value.nonEmpty then
          ProductBuilderViewModel.selectMaterial(role, MaterialId.unsafe(value))
      },
      infoSignal = availableMaterials.combineWith(lang).map { case (materials, l) =>
        materials.headOption.map { _ =>
          l match
            case Language.En => s"${materials.size} material(s) available for this category"
            case Language.Cs => s"${materials.size} materiál(ů) dostupných pro tuto kategorii"
        }
      },
      disabledSignal = ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
    )
