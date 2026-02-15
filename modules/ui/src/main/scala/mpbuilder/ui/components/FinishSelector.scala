package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object FinishSelector:
  def apply(): Element =
    val availableFinishes = ProductBuilderViewModel.availableFinishes
    val lang = ProductBuilderViewModel.currentLanguage
    
    div(
      cls := "form-group",
      label(
        child.text <-- lang.map {
          case Language.En => "Finishes (select multiple):"
          case Language.Cs => "Povrchové úpravy (vyberte více):"
        }
      ),
      div(
        cls := "checkbox-group",
        children <-- availableFinishes.combineWith(lang).map { case (finishes, l) =>
          if finishes.isEmpty then
            List(
              span(
                cls := "info-box",
                l match
                  case Language.En => "Select a category and material to see available finishes"
                  case Language.Cs => "Vyberte kategorii a materiál pro zobrazení dostupných úprav"
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
                span(finish.name(l)),
              )
            }
        },
      ),
      div(
        cls := "info-box",
        child.maybe <-- availableFinishes.combineWith(lang).map { case (finishes, l) =>
          if finishes.nonEmpty then
            Some(span(
              l match
                case Language.En => s"${finishes.size} finish(es) compatible with your selection"
                case Language.Cs => s"${finishes.size} úprav(a) kompatibilních s vaším výběrem"
            ))
          else
            None
        },
      ),
    )
