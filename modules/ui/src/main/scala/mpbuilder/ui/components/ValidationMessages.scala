package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel

object ValidationMessages:
  def apply(): Element =
    div(
      cls := "card",
      h2("Validation Status"),
      
      // Success message
      child.maybe <-- ProductBuilderViewModel.state.map { state =>
        if state.validationErrors.isEmpty && state.configuration.isDefined then
          Some(
            div(
              cls := "success-message",
              "✓ Configuration is valid! Price calculated successfully.",
            )
          )
        else
          None
      },
      
      // Error messages
      child.maybe <-- ProductBuilderViewModel.state.map { state =>
        if state.validationErrors.nonEmpty then
          Some(
            div(
              cls := "error-message",
              strong("Validation Errors:"),
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
