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

      // Color Mode
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Color Mode:"
          case Language.Cs => "Barevný režim:"
        }),
        select(
          value <-- clearFieldsStream,
          children <-- lang.map { l =>
            List(
              option(
                l match
                  case Language.En => "-- Select color mode --"
                  case Language.Cs => "-- Vyberte barevný režim --"
                , value := ""),
              option(
                l match
                  case Language.En => "CMYK (Full Color)"
                  case Language.Cs => "CMYK (plné barvy)"
                , value := "cmyk"),
              option(
                l match
                  case Language.En => "Grayscale"
                  case Language.Cs => "Stupně šedi"
                , value := "grayscale"),
            )
          },
          onChange.mapToValue --> { value =>
            value match
              case "cmyk" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.ColorModeSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.ColorModeSpec(ColorMode.CMYK)
                )
              case "grayscale" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.ColorModeSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.ColorModeSpec(ColorMode.Grayscale)
                )
              case _ => ()
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
          value <-- clearFieldsStream,
          children <-- lang.map { l =>
            List(
              option(
                l match
                  case Language.En => "-- Select orientation --"
                  case Language.Cs => "-- Vyberte orientaci --"
                , value := ""),
              option(
                l match
                  case Language.En => "Portrait"
                  case Language.Cs => "Na výšku"
                , value := "portrait"),
              option(
                l match
                  case Language.En => "Landscape"
                  case Language.Cs => "Na šířku"
                , value := "landscape"),
            )
          },
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
          value <-- clearFieldsStream,
          children <-- lang.map { l =>
            option(
              l match
                case Language.En => "-- Select fold type --"
                case Language.Cs => "-- Vyberte typ skladu --"
              , value := "") ::
            FoldType.values.map { ft =>
              option(ft.toString, value := ft.toString)
            }.toList
          },
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
          value <-- clearFieldsStream,
          children <-- lang.map { l =>
            option(
              l match
                case Language.En => "-- Select binding method --"
                case Language.Cs => "-- Vyberte typ vazby --"
              , value := "") ::
            BindingMethod.values.map { bm =>
              option(bm.toString, value := bm.toString)
            }.toList
          },
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
