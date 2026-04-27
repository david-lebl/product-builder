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

      // Show email order escape hatch when there are validation errors
      child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        if state.validationErrors.nonEmpty then
          Some(div(
            cls := "email-order-validation-hint",
            p(
              cls := "email-order-validation-msg",
              l match
                case Language.En => "Having trouble? You can still request a quote via email."
                case Language.Cs => "Máte problémy? Stále můžete požádat o nabídku e-mailem."
            ),
            EmailOrderForm(),
          ))
        else
          None
      },
    )
