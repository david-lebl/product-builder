package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.util.Visibility

/** Compact finish selector — no help, no info boxes, same toggle logic. */
object CompactFinishSelector:
  def apply(role: ComponentRole): Element =
    val availableFinishes = ProductBuilderViewModel.availableFinishes(role)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      div(
        cls := "compact-row",
        label(
          cls := "compact-label",
          child.text <-- lang.map {
            case Language.En => "Finishes:"
            case Language.Cs => "Úpravy:"
          },
        ),
        div(
          cls := "compact-field",
          div(
            cls := "compact-finish-list",
            children <-- availableFinishes.combineWith(lang).map { case (finishes, l) =>
              if finishes.isEmpty then
                List(span(cls := "compact-hint",
                  l match
                    case Language.En => "Select category & material"
                    case Language.Cs => "Vyberte kategorii a materiál"
                ))
              else
                finishes.map { finish => compactFinishItem(finish, role, l) }
            },
          ),
        ),
      ),
    )

  private def compactFinishItem(finish: Finish, role: ComponentRole, lang: Language): HtmlElement =
    val isSelected = ProductBuilderViewModel.selectedFinishIds(role).map(_.contains(finish.id))
    val currentParams = ProductBuilderViewModel.selectedFinishParams(role, finish.id)

    val defaultParams: Option[FinishParameters] = finish.finishType match
      case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
        Some(FinishParameters.LaminationParams(FinishSide.Both))
      case _ => None

    div(
      cls := "compact-finish-item",
      label(
        cls := "checkbox-label",
        input(
          typ := "checkbox",
          checked <-- isSelected,
          onChange.mapToChecked --> { _ =>
            ProductBuilderViewModel.toggleFinish(role, finish.id, defaultParams)
          },
        ),
        span(finish.name(lang)),
      ),
      compactFinishParams(finish, role, isSelected, currentParams, lang),
    )

  private def compactFinishParams(
      finish: Finish,
      role: ComponentRole,
      isSelected: Signal[Boolean],
      currentParams: Signal[Option[FinishParameters]],
      lang: Language,
  ): HtmlElement =
    finish.finishType match
      case FinishType.RoundCorners =>
        div(
          cls := "compact-finish-params",
          Visibility.when(isSelected),
          span(
            cls := "compact-finish-params-label",
            lang match
              case Language.En => "Corners:"
              case Language.Cs => "Rohy:"
          ),
          div(
            cls := "compact-finish-params-inline",
            List(1, 2, 3, 4).map { count =>
              label(
                cls := "radio-label",
                input(
                  typ := "radio",
                  nameAttr := s"compact-corners-${finish.id.value}-$role",
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
                span(count.toString),
              )
            },
          ),
          span(
            cls := "compact-finish-params-label",
            lang match
              case Language.En => "R:"
              case Language.Cs => "R:"
          ),
          input(
            typ := "number",
            cls := "compact-params-input",
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
        )

      case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
        div(
          cls := "compact-finish-params",
          Visibility.when(isSelected),
          span(
            cls := "compact-finish-params-label",
            lang match
              case Language.En => "Side:"
              case Language.Cs => "Strana:"
          ),
          select(
            cls := "compact-params-select",
            children <-- currentParams.map { params =>
              val currentSide = params.collect { case FinishParameters.LaminationParams(s) => s }.getOrElse(FinishSide.Both)
              List(
                option(
                  lang match
                    case Language.En => "Both"
                    case Language.Cs => "Obě",
                  value := "Both",
                  selected := (currentSide == FinishSide.Both),
                ),
                option(
                  lang match
                    case Language.En => "Front"
                    case Language.Cs => "Přední",
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
        )

      case FinishType.FoilStamping =>
        div(
          cls := "compact-finish-params",
          Visibility.when(isSelected),
          span(
            cls := "compact-finish-params-label",
            lang match
              case Language.En => "Foil:"
              case Language.Cs => "Fólie:"
          ),
          select(
            cls := "compact-params-select",
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
        )

      case FinishType.Grommets =>
        div(
          cls := "compact-finish-params",
          Visibility.when(isSelected),
          span(
            cls := "compact-finish-params-label",
            lang match
              case Language.En => "Spacing:"
              case Language.Cs => "Rozteč:"
          ),
          input(
            typ := "number",
            cls := "compact-params-input",
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
        )

      case FinishType.Perforation =>
        div(
          cls := "compact-finish-params",
          Visibility.when(isSelected),
          span(
            cls := "compact-finish-params-label",
            lang match
              case Language.En => "Pitch:"
              case Language.Cs => "Rozteč:"
          ),
          input(
            typ := "number",
            cls := "compact-params-input",
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
        )

      case _ => div(display := "none")

  private def foilColorLabel(color: FoilColor, lang: Language): String = color match
    case FoilColor.Gold        => lang match { case Language.En => "Gold";    case Language.Cs => "Zlatá" }
    case FoilColor.Silver      => lang match { case Language.En => "Silver";  case Language.Cs => "Stříbrná" }
    case FoilColor.Copper      => lang match { case Language.En => "Copper";  case Language.Cs => "Měděná" }
    case FoilColor.RoseGold    => lang match { case Language.En => "Rose Gold"; case Language.Cs => "Růžové zlato" }
    case FoilColor.Holographic => lang match { case Language.En => "Holo";    case Language.Cs => "Holografická" }
