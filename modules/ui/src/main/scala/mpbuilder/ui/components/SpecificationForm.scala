package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object SpecificationForm:
  def apply(): Element =
    // EventStream that fires when category changes, used to clear all inputs
    val clearFieldsStream = ProductBuilderViewModel.specResetBus.events.mapTo("")

    // Local Vars for width/height to combine into size spec
    val widthVar = Var("")
    val heightVar = Var("")
    val sizeSignal = widthVar.signal.combineWith(heightVar.signal)

    // Signal for required spec kinds based on selected category
    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds

    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "form-group",

      // Reset local width/height Vars when category changes
      clearFieldsStream --> { _ =>
        widthVar.set("")
        heightVar.set("")
      },

      // Quantity
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Quantity:"
          case Language.Cs => "Množství:"
        }),
        input(
          typ := "number",
          placeholder := "e.g., 1000",
          value <-- clearFieldsStream,
          onInput.mapToValue.map(_.toIntOption) --> { qtyOpt =>
            qtyOpt.foreach { qty =>
              if qty > 0 then
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.QuantitySpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.QuantitySpec(Quantity.unsafe(qty))
                )
            }
          },
        ),
      ),

      // Size (Width x Height in mm)
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Size (Width x Height in mm):"
          case Language.Cs => "Rozměr (šířka × výška v mm):"
        }),
        div(
          styleAttr := "display: flex; gap: 10px;",
          input(
            typ := "number",
            placeholder <-- lang.map {
              case Language.En => "Width (mm)"
              case Language.Cs => "Šířka (mm)"
            },
            styleAttr := "flex: 1;",
            value <-- clearFieldsStream,
            onInput.mapToValue --> { v => widthVar.set(v) },
          ),
          span("×", styleAttr := "line-height: 40px;"),
          input(
            typ := "number",
            placeholder <-- lang.map {
              case Language.En => "Height (mm)"
              case Language.Cs => "Výška (mm)"
            },
            styleAttr := "flex: 1;",
            value <-- clearFieldsStream,
            onInput.mapToValue --> { v => heightVar.set(v) },
          ),
        ),
        // Observer that updates the spec when both width and height are available
        sizeSignal --> { case (widthStr, heightStr) =>
          (widthStr.toDoubleOption, heightStr.toDoubleOption) match
            case (Some(w), Some(h)) if w > 0 && h > 0 =>
              ProductBuilderViewModel.removeSpecification(classOf[SpecValue.SizeSpec])
              ProductBuilderViewModel.addSpecification(
                SpecValue.SizeSpec(Dimension(w, h))
              )
            case _ => ()
        },
      ),

      // Pages (for multi-page products)
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Number of Pages (optional, for booklets/brochures):"
          case Language.Cs => "Počet stran (volitelné, pro brožurky/brožury):"
        }),
        input(
          typ := "number",
          placeholder := "e.g., 8",
          value <-- clearFieldsStream,
          onInput.mapToValue.map(_.toIntOption) --> { pagesOpt =>
            pagesOpt.foreach { pages =>
              if pages > 0 then
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.PagesSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.PagesSpec(pages)
                )
            }
          },
        ),
      ),

      // Ink Configuration
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Ink Configuration:"
          case Language.Cs => "Konfigurace inkoustu:"
        }),
        select(
          children <-- ProductBuilderViewModel.selectedInkConfig.combineWith(lang).map { case (selectedConfig, l) =>
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
          value <-- clearFieldsStream,
          onChange.mapToValue --> { value =>
            val inkConfig = value match
              case "4/4" => Some(InkConfiguration.cmyk4_4)
              case "4/0" => Some(InkConfiguration.cmyk4_0)
              case "4/1" => Some(InkConfiguration.cmyk4_1)
              case "1/0" => Some(InkConfiguration.mono1_0)
              case "1/1" => Some(InkConfiguration.mono1_1)
              case _ => scala.None
            inkConfig.foreach { config =>
              ProductBuilderViewModel.removeSpecification(classOf[SpecValue.InkConfigSpec])
              ProductBuilderViewModel.addSpecification(SpecValue.InkConfigSpec(config))
            }
          },
        ),
      ),

      // Orientation (required for Flyers)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.Orientation) then "block" else "none"
        ),
        label(child.text <-- lang.map {
          case Language.En => "Orientation:"
          case Language.Cs => "Orientace:"
        }),
        select(
          children <-- ProductBuilderViewModel.selectedOrientation.combineWith(lang).map { case (selectedOrientation, l) =>
            val currentValue = selectedOrientation match
              case Some(Orientation.Portrait) => "portrait"
              case Some(Orientation.Landscape) => "landscape"
              case None => ""
            List(
              option(
                l match
                  case Language.En => "-- Select orientation --"
                  case Language.Cs => "-- Vyberte orientaci --"
                , value := "", selected := currentValue.isEmpty),
              option(
                l match
                  case Language.En => "Portrait"
                  case Language.Cs => "Na výšku"
                , value := "portrait", selected := (currentValue == "portrait")),
              option(
                l match
                  case Language.En => "Landscape"
                  case Language.Cs => "Na šířku"
                , value := "landscape", selected := (currentValue == "landscape")),
            )
          },
          value <-- clearFieldsStream,
          onChange.mapToValue --> { value =>
            value match
              case "portrait" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.OrientationSpec(Orientation.Portrait)
                )
              case "landscape" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.OrientationSpec(Orientation.Landscape)
                )
              case _ => ()
          },
        ),
      ),

      // Fold Type (required for Brochures)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.FoldType) then "block" else "none"
        ),
        label(child.text <-- lang.map {
          case Language.En => "Fold Type:"
          case Language.Cs => "Typ skladu:"
        }),
        select(
          children <-- ProductBuilderViewModel.selectedFoldType.combineWith(lang).map { case (selectedFoldType, l) =>
            val currentValue = selectedFoldType.map(_.toString).getOrElse("")
            option(
              l match
                case Language.En => "-- Select fold type --"
                case Language.Cs => "-- Vyberte typ skladu --"
              , value := "", selected := currentValue.isEmpty) ::
            FoldType.values.map { ft =>
              option(ft.toString, value := ft.toString, selected := (ft.toString == currentValue))
            }.toList
          },
          value <-- clearFieldsStream,
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              FoldType.values.find(_.toString == value).foreach { ft =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.FoldTypeSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.FoldTypeSpec(ft)
                )
              }
          },
        ),
      ),

      // Binding Method (required for Booklets)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.BindingMethod) then "block" else "none"
        ),
        label(child.text <-- lang.map {
          case Language.En => "Binding Method:"
          case Language.Cs => "Typ vazby:"
        }),
        select(
          children <-- ProductBuilderViewModel.selectedBindingMethod.combineWith(lang).map { case (selectedBindingMethod, l) =>
            val currentValue = selectedBindingMethod.map(_.toString).getOrElse("")
            option(
              l match
                case Language.En => "-- Select binding method --"
                case Language.Cs => "-- Vyberte typ vazby --"
              , value := "", selected := currentValue.isEmpty) ::
            BindingMethod.values.map { bm =>
              option(bm.toString, value := bm.toString, selected := (bm.toString == currentValue))
            }.toList
          },
          value <-- clearFieldsStream,
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              BindingMethod.values.find(_.toString == value).foreach { bm =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.BindingMethodSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.BindingMethodSpec(bm)
                )
              }
          },
        ),
      ),

      div(
        cls := "info-box",
        p(child.text <-- lang.map {
          case Language.En => "Note: Additional specifications like binding type or lamination can be added based on your product category."
          case Language.Cs => "Poznámka: Další specifikace jako typ vazby nebo laminace mohou být přidány na základě kategorie produktu."
        }),
      ),
    )
