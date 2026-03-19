package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language
import mpbuilder.uikit.feedback.ValidationDisplay

object ValidationMessages:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Validation Status"
        case Language.Cs => "Stav validace"
      }),

      ValidationDisplay(
        errors = ProductBuilderViewModel.state.map(_.validationErrors),
        success = ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          if state.validationErrors.isEmpty && state.configuration.isDefined then
            Some(l match
              case Language.En => "✓ Configuration is valid! Price calculated successfully."
              case Language.Cs => "✓ Konfigurace je platná! Cena byla úspěšně vypočtena."
            )
          else
            None
        },
      ),
    )
