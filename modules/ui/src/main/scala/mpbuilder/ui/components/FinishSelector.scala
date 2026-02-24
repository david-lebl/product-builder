package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object FinishSelector:

  private val surfaceFinishTypes: Set[FinishType] = Set(
    FinishType.Lamination, FinishType.Overlamination, FinishType.UVCoating,
    FinishType.AqueousCoating, FinishType.SoftTouchCoating,
  )

  def apply(role: ComponentRole): Element =
    val availableFinishes = ProductBuilderViewModel.availableFinishes(role)
    val finishIds = ProductBuilderViewModel.selectedFinishIds(role)
    val sideOverrides = ProductBuilderViewModel.finishSideOverrides(role)
    val paramOverrides = ProductBuilderViewModel.finishParameterOverrides(role)
    val lang = ProductBuilderViewModel.currentLanguage

    val combined: Signal[(List[Finish], Set[FinishId], Map[FinishId, FinishSide], Map[FinishId, List[FinishParameter]], Language)] =
      availableFinishes.combineWith(finishIds, sideOverrides, paramOverrides, lang)

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
        children <-- combined.map { case (finishes, selectedIds, sides, params, l) =>
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
            finishes.flatMap { finish =>
              val isSelected = selectedIds.contains(finish.id)
              val checkboxEl = label(
                cls := "checkbox-label",
                input(
                  typ := "checkbox",
                  checked := isSelected,
                  onChange.mapToChecked --> { _ =>
                    ProductBuilderViewModel.toggleFinish(role, finish.id)
                  },
                ),
                span(finish.name(l)),
              )
              val extras = if isSelected then
                val sideSelector =
                  if surfaceFinishTypes.contains(finish.finishType) then
                    val currentSide = sides.getOrElse(finish.id, finish.side)
                    List(div(
                      cls := "finish-options",
                      label(
                        cls := "finish-option-label",
                        l match
                          case Language.En => "Side: "
                          case Language.Cs => "Strana: "
                      ),
                      select(
                        option(l match { case Language.En => "Both"; case Language.Cs => "Obě" },
                          value := "Both", selected := (currentSide == FinishSide.Both)),
                        option(l match { case Language.En => "Front only"; case Language.Cs => "Jen přední" },
                          value := "Front", selected := (currentSide == FinishSide.Front)),
                        option(l match { case Language.En => "Back only"; case Language.Cs => "Jen zadní" },
                          value := "Back", selected := (currentSide == FinishSide.Back)),
                        onChange.mapToValue --> { v =>
                          val side = v match
                            case "Front" => FinishSide.Front
                            case "Back"  => FinishSide.Back
                            case _       => FinishSide.Both
                          ProductBuilderViewModel.setFinishSide(role, finish.id, side)
                        },
                      ),
                    ))
                  else List.empty

                val paramInputs =
                  if finish.finishType == FinishType.RoundCorners then
                    val currentParams = params.getOrElse(finish.id, finish.parameters)
                    val currentRadius = currentParams.collectFirst { case FinishParameter.CornerRadiusMm(r) => r }.getOrElse(3.0)
                    val currentCount = currentParams.collectFirst { case FinishParameter.CornerCount(c) => c }.getOrElse(4)
                    List(div(
                      cls := "finish-options",
                      label(
                        cls := "finish-option-label",
                        l match
                          case Language.En => "Radius (mm): "
                          case Language.Cs => "Poloměr (mm): "
                      ),
                      input(
                        typ := "number",
                        stepAttr := "0.5",
                        minAttr := "1",
                        maxAttr := "10",
                        defaultValue := currentRadius.toString,
                        onInput.mapToValue --> { v =>
                          scala.util.Try(v.toDouble).toOption.foreach { r =>
                            val updated = List(FinishParameter.CornerRadiusMm(r), FinishParameter.CornerCount(currentCount))
                            ProductBuilderViewModel.setFinishParameters(role, finish.id, updated)
                          }
                        },
                      ),
                      label(
                        cls := "finish-option-label",
                        l match
                          case Language.En => " Corners: "
                          case Language.Cs => " Rohů: "
                      ),
                      select(
                        option("4", value := "4", selected := (currentCount == 4)),
                        option("2", value := "2", selected := (currentCount == 2)),
                        onChange.mapToValue --> { v =>
                          scala.util.Try(v.toInt).toOption.foreach { c =>
                            val updated = List(FinishParameter.CornerRadiusMm(currentRadius), FinishParameter.CornerCount(c))
                            ProductBuilderViewModel.setFinishParameters(role, finish.id, updated)
                          }
                        },
                      ),
                    ))
                  else List.empty

                sideSelector ++ paramInputs
              else List.empty

              checkboxEl :: extras
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
