package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{TextField, SelectField, SelectOption}
import mpbuilder.uikit.util.Visibility

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

    // Single Var for width+height so both dimensions are updated atomically
    val sizePairVar = Var(("", ""))
    val widthSignal  = sizePairVar.signal.map(_._1)
    val heightSignal = sizePairVar.signal.map(_._2)

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
            sizePairVar.set((dim.widthMm.toInt.toString, dim.heightMm.toInt.toString))
            val presetKey = SizePreset.values.find(p =>
              p.widthMm == dim.widthMm.toInt && p.heightMm == dim.heightMm.toInt
            ).map(_.key).getOrElse("custom")
            sizePresetVar.set(presetKey)
          case None =>
            sizePairVar.set(("", ""))
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

      // Size preset selector
      SelectField(
        label = lang.map {
          case Language.En => "Size:"
          case Language.Cs => "Rozměr:"
        },
        options = lang.map { l =>
          val presetOptions = SizePreset.values.toList.map { preset =>
            SelectOption(preset.key, preset.label(l))
          }
          val customLabel = l match
            case Language.En => "Custom"
            case Language.Cs => "Vlastní"
          presetOptions :+ SelectOption("custom", customLabel)
        },
        selected = sizePresetVar.signal,
        onChange = Observer[String] { v =>
          sizePresetVar.set(v)
          SizePreset.values.find(_.key == v) match
            case Some(preset) =>
              sizePairVar.set((preset.widthMm.toString, preset.heightMm.toString))
              ProductBuilderViewModel.replaceSpecification(
                SpecValue.SizeSpec(Dimension(preset.widthMm.toDouble, preset.heightMm.toDouble))
              )
            case None =>
              if v == "custom" then
                sizePairVar.set(("", ""))
        },
        placeholder = lang.map {
          case Language.En => "-- Select size --"
          case Language.Cs => "-- Vyberte rozměr --"
        },
      ),

      // Custom size inputs - visible when "Custom" is selected
      div(
        cls := "form-group",
        Visibility.when(sizePresetVar.signal.map(_ == "custom")),
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
            value <-- widthSignal,
            onInput.mapToValue --> { v =>
              val currentH = sizePairVar.now()._2
              sizePairVar.set((v, currentH))
              (v.toDoubleOption, currentH.toDoubleOption) match
                case (Some(w), Some(h)) if w > 0 && h > 0 =>
                  ProductBuilderViewModel.replaceSpecification(SpecValue.SizeSpec(Dimension(w, h)))
                case _ => ()
            },
          ),
          span("\u00d7", styleAttr := "line-height: 40px;"),
          input(
            typ := "number",
            placeholder <-- lang.map {
              case Language.En => "Height (mm)"
              case Language.Cs => "Výška (mm)"
            },
            styleAttr := "flex: 1;",
            value <-- heightSignal,
            onInput.mapToValue --> { v =>
              val currentW = sizePairVar.now()._1
              sizePairVar.set((currentW, v))
              (currentW.toDoubleOption, v.toDoubleOption) match
                case (Some(w), Some(h)) if w > 0 && h > 0 =>
                  ProductBuilderViewModel.replaceSpecification(SpecValue.SizeSpec(Dimension(w, h)))
                case _ => ()
            },
          ),
        ),
      ),

      // Pages (for multi-page products)
      div(
        cls := "form-group",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.Pages))),
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
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.Orientation))),
        SelectField(
          label = lang.map {
            case Language.En => "Orientation:"
            case Language.Cs => "Orientace:"
          },
          options = lang.map { l =>
            List(
              SelectOption("portrait", l match { case Language.En => "Portrait"; case Language.Cs => "Na výšku" }),
              SelectOption("landscape", l match { case Language.En => "Landscape"; case Language.Cs => "Na šířku" }),
            )
          },
          selected = ProductBuilderViewModel.selectedOrientation.map {
            case Some(Orientation.Portrait) => "portrait"
            case Some(Orientation.Landscape) => "landscape"
            case None => ""
          },
          onChange = Observer[String] { value =>
            value match
              case "portrait" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                ProductBuilderViewModel.addSpecification(SpecValue.OrientationSpec(Orientation.Portrait))
              case "landscape" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                ProductBuilderViewModel.addSpecification(SpecValue.OrientationSpec(Orientation.Landscape))
              case _ => ()
          },
          placeholder = lang.map {
            case Language.En => "-- Select orientation --"
            case Language.Cs => "-- Vyberte orientaci --"
          },
        ),
      ),

      // Fold Type (required for Brochures)
      div(
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.FoldType))),
        SelectField(
          label = lang.map {
            case Language.En => "Fold Type:"
            case Language.Cs => "Typ skladu:"
          },
          options = lang.map { l =>
            FoldType.values.toList.map(ft => SelectOption(ft.toString, foldTypeLabel(ft, l)))
          },
          selected = ProductBuilderViewModel.selectedFoldType.map(_.map(_.toString).getOrElse("")),
          onChange = Observer[String] { value =>
            if value.nonEmpty then
              FoldType.values.find(_.toString == value).foreach { ft =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.FoldTypeSpec])
                ProductBuilderViewModel.addSpecification(SpecValue.FoldTypeSpec(ft))
              }
          },
          placeholder = lang.map {
            case Language.En => "-- Select fold type --"
            case Language.Cs => "-- Vyberte typ skladu --"
          },
        ),
      ),

      // Binding Method (required for Booklets)
      div(
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.BindingMethod))),
        SelectField(
          label = lang.map {
            case Language.En => "Binding Method:"
            case Language.Cs => "Typ vazby:"
          },
          options = lang.map { l =>
            BindingMethod.values.toList.map(bm => SelectOption(bm.toString, bindingMethodLabel(bm, l)))
          },
          selected = ProductBuilderViewModel.selectedBindingMethod.map(_.map(_.toString).getOrElse("")),
          onChange = Observer[String] { value =>
            if value.nonEmpty then
              BindingMethod.values.find(_.toString == value).foreach { bm =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.BindingMethodSpec])
                ProductBuilderViewModel.addSpecification(SpecValue.BindingMethodSpec(bm))
              }
          },
          placeholder = lang.map {
            case Language.En => "-- Select binding method --"
            case Language.Cs => "-- Vyberte typ vazby --"
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
