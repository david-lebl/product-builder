package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*

/** Preset selector — renders card tiles when a category has more than one preset,
  * or nothing when only one (auto-applied) preset exists.
  */
object PresetSelector:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "preset-selector",
      children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        val categoryOpt = state.selectedCategoryId.flatMap(catalog.categories.get)
        categoryOpt match
          case Some(cat) if cat.presets.size > 1 =>
            val sectionTitle = div(
              cls := "preset-selector-title",
              l match
                case Language.En => "Choose a starting configuration:"
                case Language.Cs => "Vyberte výchozí konfiguraci:"
            )
            val cards = cat.presets.map { preset =>
              val isSelected = state.selectedPresetId.contains(preset.id)
              div(
                cls := (if isSelected then "preset-card preset-card-selected" else "preset-card"),
                div(
                  cls := "preset-card-name",
                  preset.name(l),
                ),
                preset.description.map { desc =>
                  div(
                    cls := "preset-card-description",
                    desc(l),
                  )
                }.getOrElse(emptyNode),
                onClick --> { _ =>
                  ProductBuilderViewModel.selectPreset(cat.id, preset.id)
                },
              )
            }
            sectionTitle :: cards
          case _ =>
            // Single preset or no presets — nothing to show (auto-applied)
            List.empty
      },
    )
