package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object ComponentEditor:
  def apply(role: ComponentRole): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val compState = ProductBuilderViewModel.componentState(role)
    val availableMaterials = ProductBuilderViewModel.availableMaterialsForRole(role)
    val availableFinishes = ProductBuilderViewModel.availableFinishesForRole(role)

    div(
      cls := "component-editor",
      h4(child.text <-- lang.map { l =>
        val roleName = role match
          case ComponentRole.Cover => l match
            case Language.En => "Cover"
            case Language.Cs => "Obálka"
          case ComponentRole.Body => l match
            case Language.En => "Body (Inner Pages)"
            case Language.Cs => "Vnitřní stránky"
        roleName
      }),

      // Material selector for this component
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Material:"
          case Language.Cs => "Materiál:"
        }),
        select(
          children <-- availableMaterials.combineWith(compState, lang).map { case (materials, cs, l) =>
            val currentValue = cs.selectedMaterialId.map(_.value).getOrElse("")
            option(
              l match
                case Language.En => "-- Select material --"
                case Language.Cs => "-- Vyberte materiál --"
              , value := "", selected := currentValue.isEmpty) ::
            materials.map { mat =>
              option(mat.name(l), value := mat.id.value, selected := (mat.id.value == currentValue))
            }
          },
          onChange.mapToValue --> { v =>
            if v.nonEmpty then
              ProductBuilderViewModel.selectComponentMaterial(role, MaterialId.unsafe(v))
          },
        ),
      ),

      // Finish selector for this component
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Finishes:"
          case Language.Cs => "Povrchové úpravy:"
        }),
        div(
          cls := "checkbox-group",
          children <-- availableFinishes.combineWith(compState, lang).map { case (finishes, cs, l) =>
            if finishes.isEmpty then
              List(span(
                cls := "info-box",
                l match
                  case Language.En => "Select a material to see available finishes"
                  case Language.Cs => "Vyberte materiál pro zobrazení dostupných úprav"
              ))
            else
              finishes.map { finish =>
                label(
                  cls := "checkbox-label",
                  input(
                    typ := "checkbox",
                    checked <-- compState.map(_.selectedFinishIds.contains(finish.id)),
                    onChange.mapToChecked --> { _ =>
                      ProductBuilderViewModel.toggleComponentFinish(role, finish.id)
                    },
                  ),
                  span(finish.name(l)),
                )
              }
          },
        ),
      ),

      // Ink configuration for this component
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Ink Configuration:"
          case Language.Cs => "Konfigurace inkoustu:"
        }),
        select(
          children <-- compState.combineWith(lang).map { case (cs, l) =>
            val currentValue = cs.selectedInkConfig match
              case Some(c) if c == InkConfiguration.cmyk4_4 => "4/4"
              case Some(c) if c == InkConfiguration.cmyk4_0 => "4/0"
              case Some(c) if c == InkConfiguration.cmyk4_1 => "4/1"
              case Some(c) if c == InkConfiguration.mono1_0 => "1/0"
              case Some(c) if c == InkConfiguration.mono1_1 => "1/1"
              case _ => ""
            List(
              option(
                l match
                  case Language.En => "-- Select ink configuration --"
                  case Language.Cs => "-- Vyberte konfiguraci inkoustu --"
                , value := "", selected := currentValue.isEmpty),
              option(
                l match
                  case Language.En => "4/4 CMYK both sides"
                  case Language.Cs => "4/4 CMYK oboustranně"
                , value := "4/4", selected := (currentValue == "4/4")),
              option(
                l match
                  case Language.En => "4/0 CMYK front only"
                  case Language.Cs => "4/0 CMYK jen přední strana"
                , value := "4/0", selected := (currentValue == "4/0")),
              option(
                l match
                  case Language.En => "4/1 CMYK front + grayscale back"
                  case Language.Cs => "4/1 CMYK přední + šedá zadní"
                , value := "4/1", selected := (currentValue == "4/1")),
              option(
                l match
                  case Language.En => "1/0 Grayscale front only"
                  case Language.Cs => "1/0 Šedá jen přední strana"
                , value := "1/0", selected := (currentValue == "1/0")),
              option(
                l match
                  case Language.En => "1/1 Grayscale both sides"
                  case Language.Cs => "1/1 Šedá oboustranně"
                , value := "1/1", selected := (currentValue == "1/1")),
            )
          },
          onChange.mapToValue --> { v =>
            val inkConfig = v match
              case "4/4" => Some(InkConfiguration.cmyk4_4)
              case "4/0" => Some(InkConfiguration.cmyk4_0)
              case "4/1" => Some(InkConfiguration.cmyk4_1)
              case "1/0" => Some(InkConfiguration.mono1_0)
              case "1/1" => Some(InkConfiguration.mono1_1)
              case _ => scala.None
            inkConfig.foreach { config =>
              ProductBuilderViewModel.selectComponentInkConfig(role, config)
            }
          },
        ),
      ),
    )
