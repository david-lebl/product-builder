package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*

/** Compact ink configuration selector — inline label, no help. */
object CompactInkConfigSelector:
  private val presets: List[(String, InkConfiguration)] = List(
    "4/4"   -> InkConfiguration.cmyk4_4,
    "4/0"   -> InkConfiguration.cmyk4_0,
    "4/1"   -> InkConfiguration.cmyk4_1,
    "1/0"   -> InkConfiguration.mono1_0,
    "1/1"   -> InkConfiguration.mono1_1,
    "4/0+W" -> InkConfiguration.cmyk4_0_white,
  )

  private def presetLabel(key: String, l: Language): String = (key, l) match
    case ("4/4",   Language.En) => "4/4 CMYK both sides"
    case ("4/4",   Language.Cs) => "4/4 CMYK oboustranně"
    case ("4/0",   Language.En) => "4/0 CMYK front only"
    case ("4/0",   Language.Cs) => "4/0 CMYK jen přední"
    case ("4/1",   Language.En) => "4/1 CMYK front + gray back"
    case ("4/1",   Language.Cs) => "4/1 CMYK přední + šedá"
    case ("1/0",   Language.En) => "1/0 Gray front only"
    case ("1/0",   Language.Cs) => "1/0 Šedá jen přední"
    case ("1/1",   Language.En) => "1/1 Gray both sides"
    case ("1/1",   Language.Cs) => "1/1 Šedá oboustranně"
    case ("4/0+W", Language.En) => "4/0+W CMYK + white"
    case ("4/0+W", Language.Cs) => "4/0+W CMYK + bílá"
    case _                      => key

  def apply(role: ComponentRole): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val selectedValue = ProductBuilderViewModel.selectedInkConfig(role).map { configOpt =>
      configOpt.flatMap(c => presets.find(_._2 == c).map(_._1)).getOrElse("")
    }

    div(
      cls := "compact-row",
      label(
        cls := "compact-label",
        child.text <-- lang.map {
          case Language.En => "Ink:"
          case Language.Cs => "Barvy:"
        },
      ),
      div(
        cls := "compact-field",
        select(
          children <-- lang.combineWith(selectedValue).map { case (l, sel) =>
            val ph = option(
              l match
                case Language.En => "-- Select --"
                case Language.Cs => "-- Vyberte --",
              value := "",
              selected := sel.isEmpty,
            )
            ph :: presets.map { case (key, _) =>
              option(
                presetLabel(key, l),
                value := key,
                selected := (key == sel),
              )
            }
          },
          onChange.mapToValue --> { value =>
            presets.find(_._1 == value).foreach { case (_, config) =>
              ProductBuilderViewModel.selectInkConfig(role, config)
            }
          },
        ),
      ),
    )
