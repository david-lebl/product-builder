package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.domain.model.*
import mpbuilder.uikit.util.Visibility

object FinishSelector:
  def apply(role: ComponentRole): Element =
    val availableFinishes = ProductBuilderViewModel.availableFinishes(role)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "form-group",
      div(
        cls := "label-with-help finish-section-label",
        label(
          child.text <-- lang.map {
            case Language.En => "Finishes:"
            case Language.Cs => "Povrchové úpravy:"
          }
        ),
        HelpInfo(lang.map {
          case Language.En => "Optional post-print treatments and finishing operations. These enhance the look, feel, and durability of the printed product. You can select multiple finishes."
          case Language.Cs => "Volitelné dokončovací operace po tisku. Vylepšují vzhled, hmat a odolnost tištěného produktu. Můžete vybrat více povrchových úprav."
        }),
      ),
      div(
        cls := "finish-list",
        children <-- availableFinishes.combineWith(lang).map { case (finishes, l) =>
          if finishes.isEmpty then
            List(
              span(
                cls := "info-note",
                l match
                  case Language.En => "Select a category and material to see available finishes"
                  case Language.Cs => "Vyberte kategorii a materiál pro zobrazení dostupných úprav"
              )
            )
          else
            finishes.map { finish => finishItem(finish, role, l) }
        },
      ),
    )

  private def finishItem(finish: Finish, role: ComponentRole, lang: Language): HtmlElement =
    val isSelected = ProductBuilderViewModel.selectedFinishIds(role).map(_.contains(finish.id))
    val currentParams = ProductBuilderViewModel.selectedFinishParams(role, finish.id)

    // Default params to initialize when a parameterized finish is first selected
    val defaultParams: Option[FinishParameters] = finish.finishType match
      case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
        Some(FinishParameters.LaminationParams(FinishSide.Both))
      case FinishType.Scoring =>
        Some(FinishParameters.ScoringParams(1))
      case _ => None

    div(
      cls := "finish-item",
      label(
        cls := "checkbox-label",
        input(
          typ := "checkbox",
          checked <-- isSelected,
          onChange.mapToChecked --> { _ =>
            ProductBuilderViewModel.toggleFinish(role, finish.id, defaultParams)
          },
        ),
        span(
          cls := "finish-label-with-help",
          span(finish.name(lang)),
          finish.description match
            case Some(desc) =>
              HelpInfo.fromSignal(Val(Some(desc(lang))))
            case None =>
              span()
          ,
        ),
      ),
      finishParamsForm(finish, role, isSelected, currentParams, lang),
    )

  private def finishParamsForm(
      finish: Finish,
      role: ComponentRole,
      isSelected: Signal[Boolean],
      currentParams: Signal[Option[FinishParameters]],
      lang: Language,
  ): HtmlElement =
    finish.finishType match
      case FinishType.RoundCorners =>
        div(
          cls := "finish-params",
          Visibility.when(isSelected),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Corners:"
                case Language.Cs => "Počet rohů:"
            ),
            div(
              cls := "finish-params-options",
              List(1, 2, 3, 4).map { count =>
                label(
                  cls := "radio-label",
                  input(
                    typ := "radio",
                    nameAttr := s"corners-${finish.id.value}-$role",
                    value := count.toString,
                    checked <-- currentParams.map {
                      case Some(FinishParameters.RoundCornersParams(c, _)) => c == count
                      case _                                               => count == 4
                    },
                    onChange --> { _ =>
                      val currentRadius = ProductBuilderViewModel.currentFinishParams(role, finish.id)
                        .collect { case FinishParameters.RoundCornersParams(_, r) => r }
                        .getOrElse(3)
                      ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.RoundCornersParams(count, currentRadius)))
                    },
                  ),
                  span(lang match
                    case Language.En => if count == 1 then "1 corner" else s"$count corners"
                    case Language.Cs => if count == 1 then "1 roh" else s"$count rohy"
                  ),
                )
              },
            ),
          ),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Radius (mm):"
                case Language.Cs => "Poloměr (mm):"
            ),
            input(
              typ := "number",
              cls := "finish-params-input",
              minAttr := "1",
              maxAttr := "20",
              placeholder := "3",
              value <-- currentParams.map {
                case Some(FinishParameters.RoundCornersParams(_, r)) => r.toString
                case _                                               => ""
              },
              onInput.mapToValue --> { v =>
                v.toIntOption.filter(r => r >= 1 && r <= 20).foreach { radius =>
                  val currentCount = ProductBuilderViewModel.currentFinishParams(role, finish.id)
                    .collect { case FinishParameters.RoundCornersParams(c, _) => c }
                    .getOrElse(4)
                  ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.RoundCornersParams(currentCount, radius)))
                }
              },
            ),
          ),
        )

      case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
        div(
          cls := "finish-params",
          Visibility.when(isSelected),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Apply to:"
                case Language.Cs => "Aplikovat na:"
            ),
            select(
              cls := "finish-params-select",
              children <-- currentParams.map { params =>
                val currentSide = params.collect { case FinishParameters.LaminationParams(s) => s }.getOrElse(FinishSide.Both)
                List(
                  option(
                    lang match
                      case Language.En => "Both sides"
                      case Language.Cs => "Obě strany",
                    value := "Both",
                    selected := (currentSide == FinishSide.Both),
                  ),
                  option(
                    lang match
                      case Language.En => "Front only"
                      case Language.Cs => "Pouze přední strana",
                    value := "Front",
                    selected := (currentSide == FinishSide.Front),
                  ),
                )
              },
              onChange.mapToValue --> { v =>
                val side = if v == "Front" then FinishSide.Front else FinishSide.Both
                ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.LaminationParams(side)))
              },
            ),
          ),
        )

      case FinishType.FoilStamping =>
        div(
          cls := "finish-params",
          Visibility.when(isSelected),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Foil color:"
                case Language.Cs => "Barva fólie:"
            ),
            select(
              cls := "finish-params-select",
              children <-- currentParams.map { params =>
                val currentColor = params.collect { case FinishParameters.FoilStampingParams(c) => c }.getOrElse(FoilColor.Gold)
                FoilColor.values.toList.map { color =>
                  option(
                    foilColorLabel(color, lang),
                    value := color.toString,
                    selected := (currentColor == color),
                  )
                }
              },
              onChange.mapToValue --> { v =>
                FoilColor.values.find(_.toString == v).foreach { color =>
                  ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.FoilStampingParams(color)))
                }
              },
            ),
          ),
        )

      case FinishType.Grommets =>
        div(
          cls := "finish-params",
          Visibility.when(isSelected),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Spacing (mm):"
                case Language.Cs => "Rozteč (mm):"
            ),
            input(
              typ := "number",
              cls := "finish-params-input",
              minAttr := "100",
              placeholder := "500",
              value <-- currentParams.map {
                case Some(FinishParameters.GrommetParams(s)) => s.toString
                case _                                       => ""
              },
              onInput.mapToValue --> { v =>
                v.toIntOption.filter(_ > 0).foreach { spacing =>
                  ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.GrommetParams(spacing)))
                }
              },
            ),
          ),
        )

      case FinishType.Perforation =>
        div(
          cls := "finish-params",
          Visibility.when(isSelected),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Pitch (mm):"
                case Language.Cs => "Rozteč děrování (mm):"
            ),
            input(
              typ := "number",
              cls := "finish-params-input",
              minAttr := "1",
              placeholder := "5",
              value <-- currentParams.map {
                case Some(FinishParameters.PerforationParams(p)) => p.toString
                case _                                           => ""
              },
              onInput.mapToValue --> { v =>
                v.toIntOption.filter(_ > 0).foreach { pitch =>
                  ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.PerforationParams(pitch)))
                }
              },
            ),
          ),
        )

      case FinishType.Scoring =>
        div(
          cls := "finish-params",
          Visibility.when(isSelected),
          div(
            cls := "finish-params-row",
            span(
              cls := "finish-params-label",
              lang match
                case Language.En => "Creases:"
                case Language.Cs => "Počet bigů:"
            ),
            div(
              cls := "finish-params-options",
              List(1, 2, 3, 4).map { count =>
                label(
                  cls := "radio-label",
                  input(
                    typ := "radio",
                    nameAttr := s"creases-${finish.id.value}-$role",
                    value := count.toString,
                    checked <-- currentParams.map {
                      case Some(FinishParameters.ScoringParams(c)) => c == count
                      case _                                       => count == 1
                    },
                    onChange --> { _ =>
                      ProductBuilderViewModel.setFinishParams(role, finish.id, Some(FinishParameters.ScoringParams(count)))
                    },
                  ),
                  span(lang match
                    case Language.En => if count == 1 then "1 crease" else s"$count creases"
                    case Language.Cs => if count == 1 then "1 big" else s"$count bigy"
                  ),
                )
              },
            ),
          ),
        )

      // Finish types without configurable parameters
      case _ => div(display := "none")

  private def foilColorLabel(color: FoilColor, lang: Language): String = color match
    case FoilColor.Gold        => lang match { case Language.En => "Gold";        case Language.Cs => "Zlatá" }
    case FoilColor.Silver      => lang match { case Language.En => "Silver";      case Language.Cs => "Stříbrná" }
    case FoilColor.Copper      => lang match { case Language.En => "Copper";      case Language.Cs => "Měděná" }
    case FoilColor.RoseGold    => lang match { case Language.En => "Rose Gold";   case Language.Cs => "Růžové zlato" }
    case FoilColor.Holographic => lang match { case Language.En => "Holographic"; case Language.Cs => "Holografická" }
