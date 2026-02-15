package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object FinishSelector:
  def apply(): Element =
    val availableFinishes = ProductBuilderViewModel.availableFinishes
    
    div(
      cls := "form-group",
      label("Finishes (select multiple):"),
      div(
        cls := "checkbox-group",
        children <-- availableFinishes.map { finishes =>
          if finishes.isEmpty then
            List(
              span(
                cls := "info-box",
                "Select a category and material to see available finishes",
              )
            )
          else
            finishes.map { finish =>
              label(
                cls := "checkbox-label",
                input(
                  typ := "checkbox",
                  checked <-- ProductBuilderViewModel.state.map(_.selectedFinishIds.contains(finish.id)),
                  onChange.mapToChecked --> { _ =>
                    ProductBuilderViewModel.toggleFinish(finish.id)
                  },
                ),
                span(finish.name),
              )
            }
        },
      ),
      div(
        cls := "info-box",
        child.maybe <-- availableFinishes.map { finishes =>
          if finishes.nonEmpty then
            Some(span(s"${finishes.size} finish(es) compatible with your selection"))
          else
            None
        },
      ),
    )
