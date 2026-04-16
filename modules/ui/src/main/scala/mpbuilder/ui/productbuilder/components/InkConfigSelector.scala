package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object InkConfigSelector:
  private val presets: List[(String, InkConfiguration)] = List(
    "4/4"   -> InkConfiguration.cmyk4_4,
    "4/0"   -> InkConfiguration.cmyk4_0,
    "4/1"   -> InkConfiguration.cmyk4_1,
    "1/0"   -> InkConfiguration.mono1_0,
    "1/1"   -> InkConfiguration.mono1_1,
    "4/0+W" -> InkConfiguration.cmyk4_0_white,
  )

  private def presetLabels(key: String, l: Language): String = (key, l) match
    case ("4/4",   Language.En) => "4/4 CMYK both sides"
    case ("4/4",   Language.Cs) => "4/4 CMYK oboustranně"
    case ("4/0",   Language.En) => "4/0 CMYK front only"
    case ("4/0",   Language.Cs) => "4/0 CMYK jen přední strana"
    case ("4/1",   Language.En) => "4/1 CMYK front + grayscale back"
    case ("4/1",   Language.Cs) => "4/1 CMYK přední + šedá zadní"
    case ("1/0",   Language.En) => "1/0 Grayscale front only"
    case ("1/0",   Language.Cs) => "1/0 Šedá jen přední strana"
    case ("1/1",   Language.En) => "1/1 Grayscale both sides"
    case ("1/1",   Language.Cs) => "1/1 Šedá oboustranně"
    case ("4/0+W", Language.En) => "4/0+W CMYK front + white underlay (transparent material)"
    case ("4/0+W", Language.Cs) => "4/0+W CMYK přední + bílý podklad (průhledný materiál)"
    case _                      => key

  def apply(role: ComponentRole): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    val selectedValue = ProductBuilderViewModel.selectedInkConfig(role).map { configOpt =>
      configOpt.flatMap(c => presets.find(_._2 == c).map(_._1)).getOrElse("")
    }

    SelectField(
      label = lang.map {
        case Language.En => "Ink Configuration:"
        case Language.Cs => "Barevnost:"
      },
      options = lang.map { l =>
        presets.map { case (key, _) => SelectOption(key, presetLabels(key, l)) }
      },
      selected = selectedValue,
      onChange = Observer[String] { value =>
        presets.find(_._1 == value).foreach { case (_, config) =>
          ProductBuilderViewModel.selectInkConfig(role, config)
        }
      },
      placeholder = lang.map {
        case Language.En => "-- Select ink configuration --"
        case Language.Cs => "-- Vyberte konfiguraci inkoustu --"
      },
      horizontal = true,
      helpContent = Some(lang.map {
        case Language.En => "The number of ink colors used on each side of the print. Notation is front/back — e.g. 4/4 means full color CMYK on both sides, 4/0 means color on front only. More colors = higher cost."
        case Language.Cs => "Počet barev inkoustu použitých na každé straně tisku. Zápis je přední/zadní — např. 4/4 znamená plnobarevný CMYK oboustranně, 4/0 znamená barvu jen na přední straně. Více barev = vyšší cena."
      }),
    )
