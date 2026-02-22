package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object SpecificationForm:
  private enum SizePreset(val nameEn: String, val nameCs: String, val widthMm: Int, val heightMm: Int):
    case A3           extends SizePreset("A3",            "A3",       297, 420)
    case A4           extends SizePreset("A4",            "A4",       210, 297)
    case A5           extends SizePreset("A5",            "A5",       148, 210)
    case A6           extends SizePreset("A6",            "A6",       105, 148)
    case DL           extends SizePreset("DL",            "DL",        99, 210)
    case BusinessCard extends SizePreset("Business Card", "Vizitka",   90,  55)
    case Square148    extends SizePreset("Square",        "Čtverec",  148, 148)
    case Square210    extends SizePreset("Square",        "Čtverec",  210, 210)

    def key: String = ordinal.toString

    def label(lang: Language): String =
      val name = lang match
        case Language.En => nameEn
        case Language.Cs => nameCs
      s"$name ($widthMm \u00d7 $heightMm mm)"

  def apply(): Element =
    // EventStream that fires when category changes, carries default specs
    val defaultSpecsStream = ProductBuilderViewModel.specResetBus.events

    // Helper to extract default values from specs
    def defaultQuantity(specs: List[SpecValue]): String =
      specs.collectFirst { case SpecValue.QuantitySpec(q) => q.value.toString }.getOrElse("")
    def defaultPages(specs: List[SpecValue]): String =
      specs.collectFirst { case SpecValue.PagesSpec(p) => p.toString }.getOrElse("")
    def defaultSizePresetKey(specs: List[SpecValue]): String =
      specs.collectFirst { case SpecValue.SizeSpec(dim) =>
        SizePreset.values.find(p => p.widthMm == dim.widthMm.toInt && p.heightMm == dim.heightMm.toInt)
          .map(_.key).getOrElse("custom")
      }.getOrElse("")

    // Local Vars for width/height to combine into size spec
    val widthVar = Var("")
    val heightVar = Var("")
    val sizeSignal = widthVar.signal.combineWith(heightVar.signal)

    // Size preset selector: "" = no selection (custom), or a preset key
    val sizePresetVar = Var("")

    // Signal for required spec kinds based on selected category
    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds

    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "form-group",

      // Reset local width/height Vars and preset when category changes, apply defaults
      defaultSpecsStream --> { specs =>
        specs.collectFirst { case SpecValue.SizeSpec(dim) => dim } match
          case Some(dim) =>
            widthVar.set(dim.widthMm.toInt.toString)
            heightVar.set(dim.heightMm.toInt.toString)
            val presetKey = SizePreset.values.find(p =>
              p.widthMm == dim.widthMm.toInt && p.heightMm == dim.heightMm.toInt
            ).map(_.key).getOrElse("custom")
            sizePresetVar.set(presetKey)
          case None =>
            widthVar.set("")
            heightVar.set("")
            sizePresetVar.set("")
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
          value <-- defaultSpecsStream.map(defaultQuantity),
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

      // Observer that updates the spec when both width and height are available
      // (placed on the parent div so it fires for both presets and custom input)
      sizeSignal --> { case (widthStr, heightStr) =>
        (widthStr.toDoubleOption, heightStr.toDoubleOption) match
          case (Some(w), Some(h)) if w > 0 && h > 0 =>
            ProductBuilderViewModel.removeSpecification(classOf[SpecValue.SizeSpec])
            ProductBuilderViewModel.addSpecification(
              SpecValue.SizeSpec(Dimension(w, h))
            )
          case _ => ()
      },

      // Size preset selector
      div(
        cls := "form-group",
        label(child.text <-- lang.map {
          case Language.En => "Size:"
          case Language.Cs => "Rozměr:"
        }),
        select(
          children <-- sizePresetVar.signal.combineWith(lang).map { case (currentPreset, l) =>
            val placeholderLabel = l match
              case Language.En => "-- Select size --"
              case Language.Cs => "-- Vyberte rozměr --"
            val customLabel = l match
              case Language.En => "Custom"
              case Language.Cs => "Vlastní"
            val presetOptions = SizePreset.values.toList.map { preset =>
              option(preset.label(l), value := preset.key, selected := (currentPreset == preset.key))
            }
            option(placeholderLabel, value := "", selected := currentPreset.isEmpty) ::
            (presetOptions :+ option(customLabel, value := "custom", selected := (currentPreset == "custom")))
          },
          value <-- defaultSpecsStream.map(defaultSizePresetKey),
          onChange.mapToValue --> { v =>
            sizePresetVar.set(v)
            SizePreset.values.find(_.key == v) match
              case Some(preset) =>
                widthVar.set(preset.widthMm.toString)
                heightVar.set(preset.heightMm.toString)
              case None =>
                if v == "custom" then
                  widthVar.set("")
                  heightVar.set("")
          },
        ),
      ),

      // Custom size inputs - visible when "Custom" is selected
      div(
        cls := "form-group",
        display <-- sizePresetVar.signal.map(p => if p == "custom" then "block" else "none"),
        label(child.text <-- lang.map {
          case Language.En => "Custom Size (Width x Height in mm):"
          case Language.Cs => "Vlastní rozměr (šířka × výška v mm):"
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
            value <-- widthVar.signal,
            onInput.mapToValue --> { v => widthVar.set(v) },
          ),
          span("\u00d7", styleAttr := "line-height: 40px;"),
          input(
            typ := "number",
            placeholder <-- lang.map {
              case Language.En => "Height (mm)"
              case Language.Cs => "Výška (mm)"
            },
            styleAttr := "flex: 1;",
            value <-- heightVar.signal,
            onInput.mapToValue --> { v => heightVar.set(v) },
          ),
        ),
      ),

      // Pages (for multi-page products)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.Pages) then "block" else "none"
        ),
        label(child.text <-- lang.map {
          case Language.En => "Number of Pages:"
          case Language.Cs => "Počet stran:"
        }),
        input(
          typ := "number",
          placeholder := "e.g., 8",
          value <-- defaultSpecsStream.map(defaultPages),
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
              option(foldTypeLabel(ft, l), value := ft.toString, selected := (ft.toString == currentValue))
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
          children <-- ProductBuilderViewModel.selectedBindingMethod.combineWith(lang).map { case (selectedBindingMethod, l) =>
            val currentValue = selectedBindingMethod.map(_.toString).getOrElse("")
            option(
              l match
                case Language.En => "-- Select binding method --"
                case Language.Cs => "-- Vyberte typ vazby --"
              , value := "", selected := currentValue.isEmpty) ::
            BindingMethod.values.map { bm =>
              option(bindingMethodLabel(bm, l), value := bm.toString, selected := (bm.toString == currentValue))
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

  private def foldTypeLabel(ft: FoldType, lang: Language): String = ft match
    case FoldType.Half        => lang match { case Language.En => "Half Fold";    case Language.Cs => "Půlený sklad" }
    case FoldType.Tri         => lang match { case Language.En => "Tri-Fold";     case Language.Cs => "Trojsklad" }
    case FoldType.Gate        => lang match { case Language.En => "Gate Fold";    case Language.Cs => "Bránový sklad" }
    case FoldType.Accordion   => lang match { case Language.En => "Accordion";    case Language.Cs => "Harmonika" }
    case FoldType.ZFold       => lang match { case Language.En => "Z-Fold";       case Language.Cs => "Z-sklad" }
    case FoldType.RollFold    => lang match { case Language.En => "Roll Fold";    case Language.Cs => "Rolový sklad" }
    case FoldType.FrenchFold  => lang match { case Language.En => "French Fold";  case Language.Cs => "Francouzský sklad" }
    case FoldType.CrossFold   => lang match { case Language.En => "Cross Fold";   case Language.Cs => "Křížový sklad" }

  private def bindingMethodLabel(bm: BindingMethod, lang: Language): String = bm match
    case BindingMethod.SaddleStitch    => lang match { case Language.En => "Saddle Stitch";    case Language.Cs => "V1 – sešitová vazba" }
    case BindingMethod.PerfectBinding  => lang match { case Language.En => "Perfect Binding";  case Language.Cs => "V2 – lepená vazba" }
    case BindingMethod.SpiralBinding   => lang match { case Language.En => "Spiral Binding";   case Language.Cs => "Kroužková vazba" }
    case BindingMethod.WireOBinding    => lang match { case Language.En => "Wire-O Binding";   case Language.Cs => "Wire-O vazba" }
    case BindingMethod.CaseBinding     => lang match { case Language.En => "Case Binding";     case Language.Cs => "V8 – tuhá vazba" }
