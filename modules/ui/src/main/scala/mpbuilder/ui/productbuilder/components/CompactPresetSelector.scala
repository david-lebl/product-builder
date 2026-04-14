package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*

/** Compact preset selector — shows preset cards only when multiple presets exist,
  * without extra descriptions.
  */
object CompactPresetSelector:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "compact-presets",
      children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        val categoryOpt = state.selectedCategoryId.flatMap(catalog.categories.get)
        categoryOpt match
          case Some(cat) if cat.presets.size > 1 =>
            cat.presets.map { preset =>
              val isSelected = state.selectedPresetId.contains(preset.id)
              button(
                cls := (if isSelected then "compact-preset-btn compact-preset-btn--selected" else "compact-preset-btn"),
                preset.name(l),
                onClick --> { _ =>
                  ProductBuilderViewModel.selectPreset(cat.id, preset.id)
                },
              )
            }
          case _ => List.empty
      },
    )
