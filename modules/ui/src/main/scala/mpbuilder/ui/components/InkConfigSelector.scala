package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object InkConfigSelector:
  def apply(role: ComponentRole): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "form-group",
      label(child.text <-- lang.map {
        case Language.En => "Ink Configuration:"
        case Language.Cs => "Konfigurace inkoustu:"
      }),
      select(
        children <-- ProductBuilderViewModel.selectedInkConfig(role).combineWith(lang).map { case (selectedConfig, l) =>
          val currentValue = selectedConfig match
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
        onChange.mapToValue --> { value =>
          val inkConfig = value match
            case "4/4" => Some(InkConfiguration.cmyk4_4)
            case "4/0" => Some(InkConfiguration.cmyk4_0)
            case "4/1" => Some(InkConfiguration.cmyk4_1)
            case "1/0" => Some(InkConfiguration.mono1_0)
            case "1/1" => Some(InkConfiguration.mono1_1)
            case _ => scala.None
          inkConfig.foreach(config => ProductBuilderViewModel.selectInkConfig(role, config))
        },
      ),
    )
