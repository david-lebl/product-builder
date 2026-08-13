package mpbuilder.ui.configurator

import com.raquo.laminar.api.L.*
import mpbuilder.ui.I18n.t

object CategoryStep:

  /** Category grid + preset quick-order chips, shown until a category is chosen. */
  def view(state: ConfiguratorState): HtmlElement =
    div(
      cls := "category-step",
      h2("Vyberte produkt"),
      div(
        cls := "category-grid",
        state.catalog.categories.map { category =>
          val presets = state.bundle.presets.filter(_.categoryId == category.id)
          div(
            cls := "category-card",
            h3(t(category.name)),
            button(
              cls := "btn btn-primary",
              "Konfigurovat",
              onClick --> (_ => state.selectCategory(category)),
            ),
            when(presets.nonEmpty)(
              div(
                cls := "preset-chips",
                span(cls := "preset-label", "Rychlá objednávka:"),
                presets.map { preset =>
                  button(
                    cls := "chip",
                    t(preset.name),
                    onClick --> (_ => state.applyPreset(preset)),
                  )
                },
              )
            ),
          )
        },
      ),
    )
