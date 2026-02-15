package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.Language

object ValidationMessages:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Validation Status"
        case Language.Cs => "Stav validace"
      }),
      
      // Success message
      child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        if state.validationErrors.isEmpty && state.configuration.isDefined then
          Some(
            div(
              cls := "success-message",
              l match
                case Language.En => "✓ Configuration is valid! Price calculated successfully."
                case Language.Cs => "✓ Konfigurace je platná! Cena byla úspěšně vypočtena."
            )
          )
        else
          None
      },
      
      // Error messages
      child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        if state.validationErrors.nonEmpty then
          Some(
            div(
              cls := "error-message",
              strong(l match
                case Language.En => "Validation Errors:"
                case Language.Cs => "Chyby validace:"
              ),
              ul(
                cls := "error-list",
                state.validationErrors.map { error =>
                  li(error)
                },
              ),
            )
          )
        else
          None
      },
    )
