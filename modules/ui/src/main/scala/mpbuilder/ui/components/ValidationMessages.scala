package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.Language
import mpbuilder.uicommon.MessagePanel

object ValidationMessages:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "card",
      h2(child.text <-- lang.map {
        case Language.En => "Validation Status"
        case Language.Cs => "Stav validace"
      }),

      MessagePanel.reactive(
        showSuccess = ProductBuilderViewModel.state.map(s =>
          s.validationErrors.isEmpty && s.configuration.isDefined
        ),
        successContent = child.text <-- lang.map {
          case Language.En => "✓ Configuration is valid! Price calculated successfully."
          case Language.Cs => "✓ Konfigurace je platná! Cena byla úspěšně vypočtena."
        },
        errors = ProductBuilderViewModel.state.map(_.validationErrors),
        errorTitle = child.text <-- lang.map {
          case Language.En => "Validation Errors:"
          case Language.Cs => "Chyby validace:"
        },
      ),
    )
