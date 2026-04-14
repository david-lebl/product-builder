package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.domain.sample.SampleCatalog
import mpbuilder.domain.service.{CompletionEstimator, TierRestrictionValidator}
import mpbuilder.uikit.util.Visibility

/** Compact specification form — inline labels, no notes/info boxes. */
object CompactSpecificationForm:

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
    val defaultSpecsStream = ProductBuilderViewModel.specResetBus.events

    def defaultQuantity(specs: List[SpecValue]): String =
      specs.collectFirst { case SpecValue.QuantitySpec(q) => q.value.toString }.getOrElse("")
    def defaultPages(specs: List[SpecValue]): String =
      specs.collectFirst { case SpecValue.PagesSpec(p) => p.toString }.getOrElse("")

    val sizePairVar = Var(("", ""))
    val widthSignal  = sizePairVar.signal.map(_._1)
    val heightSignal = sizePairVar.signal.map(_._2)
    val sizePresetVar = Var("")

    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      // Reset size when category changes
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
        cls := "compact-row",
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Quantity:"
          case Language.Cs => "Množství:"
        }),
        div(
          cls := "compact-field",
          input(
            typ := "number",
            placeholder := "1000",
            value <-- defaultSpecsStream.map(defaultQuantity),
            onInput.mapToValue.map(_.toIntOption) --> { qtyOpt =>
              qtyOpt.foreach { qty =>
                if qty > 0 then
                  ProductBuilderViewModel.removeSpecification(classOf[SpecValue.QuantitySpec])
                  ProductBuilderViewModel.addSpecification(SpecValue.QuantitySpec(Quantity.unsafe(qty)))
              }
            },
          ),
        ),
      ),

      // Size
      div(
        cls := "compact-row",
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Size:"
          case Language.Cs => "Rozměr:"
        }),
        div(
          cls := "compact-field",
          select(
            children <-- lang.combineWith(sizePresetVar.signal).map { case (l, sel) =>
              val ph = option(
                l match
                  case Language.En => "-- Select --"
                  case Language.Cs => "-- Vyberte --",
                value := "",
                selected := sel.isEmpty,
              )
              val presetOpts = SizePreset.values.toList.map { preset =>
                option(preset.label(l), value := preset.key, selected := (preset.key == sel))
              }
              val custom = option(
                l match
                  case Language.En => "Custom"
                  case Language.Cs => "Vlastní",
                value := "custom",
                selected := (sel == "custom"),
              )
              ph :: presetOpts ::: List(custom)
            },
            onChange.mapToValue --> { v =>
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
          ),
        ),
      ),

      // Custom size inputs
      div(
        cls := "compact-row",
        Visibility.when(sizePresetVar.signal.map(_ == "custom")),
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "W × H (mm):"
          case Language.Cs => "Š × V (mm):"
        }),
        div(
          cls := "compact-field",
          div(
            cls := "compact-size-inputs",
            input(
              typ := "number",
              placeholder <-- lang.map {
                case Language.En => "Width"
                case Language.Cs => "Šířka"
              },
              cls := "compact-size-input",
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
            span("\u00d7"),
            input(
              typ := "number",
              placeholder <-- lang.map {
                case Language.En => "Height"
                case Language.Cs => "Výška"
              },
              cls := "compact-size-input",
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
      ),

      // Pages
      div(
        cls := "compact-row",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.Pages))),
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Pages:"
          case Language.Cs => "Stran:"
        }),
        div(
          cls := "compact-field",
          input(
            typ := "number",
            placeholder := "8",
            value <-- defaultSpecsStream.map(defaultPages),
            onInput.mapToValue.map(_.toIntOption) --> { pagesOpt =>
              pagesOpt.foreach { pages =>
                if pages > 0 then
                  ProductBuilderViewModel.removeSpecification(classOf[SpecValue.PagesSpec])
                  ProductBuilderViewModel.addSpecification(SpecValue.PagesSpec(pages))
              }
            },
          ),
        ),
      ),

      // Orientation
      div(
        cls := "compact-row",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.Orientation))),
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Orientation:"
          case Language.Cs => "Orientace:"
        }),
        div(
          cls := "compact-field",
          select(
            children <-- lang.combineWith(ProductBuilderViewModel.selectedOrientation).map { case (l, sel) =>
              val ph = option(
                l match
                  case Language.En => "-- Select --"
                  case Language.Cs => "-- Vyberte --",
                value := "",
                selected := sel.isEmpty,
              )
              List(
                ph,
                option(
                  l match { case Language.En => "Portrait"; case Language.Cs => "Na výšku" },
                  value := "portrait",
                  selected := sel.contains(Orientation.Portrait),
                ),
                option(
                  l match { case Language.En => "Landscape"; case Language.Cs => "Na šířku" },
                  value := "landscape",
                  selected := sel.contains(Orientation.Landscape),
                ),
              )
            },
            onChange.mapToValue --> { value =>
              value match
                case "portrait" =>
                  ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                  ProductBuilderViewModel.addSpecification(SpecValue.OrientationSpec(Orientation.Portrait))
                case "landscape" =>
                  ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                  ProductBuilderViewModel.addSpecification(SpecValue.OrientationSpec(Orientation.Landscape))
                case _ => ()
            },
          ),
        ),
      ),

      // Fold Type
      div(
        cls := "compact-row",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.FoldType))),
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Fold:"
          case Language.Cs => "Sklad:"
        }),
        div(
          cls := "compact-field",
          select(
            children <-- lang.combineWith(ProductBuilderViewModel.selectedFoldType).map { case (l, sel) =>
              val ph = option(
                l match
                  case Language.En => "-- Select --"
                  case Language.Cs => "-- Vyberte --",
                value := "",
                selected := sel.isEmpty,
              )
              ph :: FoldType.values.toList.map { ft =>
                option(
                  foldTypeLabel(ft, l),
                  value := ft.toString,
                  selected := sel.contains(ft),
                )
              }
            },
            onChange.mapToValue --> { value =>
              if value.nonEmpty then
                FoldType.values.find(_.toString == value).foreach { ft =>
                  ProductBuilderViewModel.removeSpecification(classOf[SpecValue.FoldTypeSpec])
                  ProductBuilderViewModel.addSpecification(SpecValue.FoldTypeSpec(ft))
                }
            },
          ),
        ),
      ),

      // Binding Method
      div(
        cls := "compact-row",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.BindingMethod))),
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Binding:"
          case Language.Cs => "Vazba:"
        }),
        div(
          cls := "compact-field",
          select(
            children <-- lang.combineWith(ProductBuilderViewModel.selectedBindingMethod).map { case (l, sel) =>
              val ph = option(
                l match
                  case Language.En => "-- Select --"
                  case Language.Cs => "-- Vyberte --",
                value := "",
                selected := sel.isEmpty,
              )
              ph :: BindingMethod.values.toList.map { bm =>
                option(
                  bindingMethodLabel(bm, l),
                  value := bm.toString,
                  selected := sel.contains(bm),
                )
              }
            },
            onChange.mapToValue --> { value =>
              if value.nonEmpty then
                BindingMethod.values.find(_.toString == value).foreach { bm =>
                  ProductBuilderViewModel.removeSpecification(classOf[SpecValue.BindingMethodSpec])
                  ProductBuilderViewModel.addSpecification(SpecValue.BindingMethodSpec(bm))
                }
            },
          ),
        ),
      ),
    )

  /** Compact manufacturing speed selector — uses simple radio buttons instead of cards. */
  def manufacturingSpeedSection(): Element =
    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      Visibility.when(requiredSpecs.map(_.nonEmpty)),
      div(
        cls := "compact-row",
        label(cls := "compact-label", child.text <-- lang.map {
          case Language.En => "Speed:"
          case Language.Cs => "Rychlost:"
        }),
        div(
          cls := "compact-field",
          div(
            cls := "compact-speed-options",
            {
              val expressViolations = ProductBuilderViewModel.tierViolations(ManufacturingSpeed.Express)
              val expressUtilDisabled = ProductBuilderViewModel.expressAvailable.map(!_)
              val expressViolDisabled = expressViolations.map(_.nonEmpty)
              val expressDisabled = expressUtilDisabled.combineWith(expressViolDisabled).map { (u: Boolean, v: Boolean) => u || v }

              List(
                compactSpeedOption(ManufacturingSpeed.Express, "⚡", expressDisabled),
                compactSpeedOption(ManufacturingSpeed.Standard, "●", Val(false)),
                compactSpeedOption(ManufacturingSpeed.Economy, "🐢", Val(false)),
              )
            },
          ),
        ),
      ),
    )

  private def compactSpeedOption(
    speed: ManufacturingSpeed,
    icon: String,
    disabledSignal: Signal[Boolean],
  ): HtmlElement =
    val lang = ProductBuilderViewModel.currentLanguage
    val isSelected = ProductBuilderViewModel.selectedManufacturingSpeed.map(_.contains(speed))
    val (nameEn, nameCs) = speed match
      case ManufacturingSpeed.Express  => ("Express", "Expres")
      case ManufacturingSpeed.Standard => ("Standard", "Standard")
      case ManufacturingSpeed.Economy  => ("Economy", "Ekonom")
    val (priceEn, priceCs) = speed match
      case ManufacturingSpeed.Express  => ("+35%", "+35%")
      case ManufacturingSpeed.Standard => ("base", "základ")
      case ManufacturingSpeed.Economy  => ("−15%", "−15%")

    com.raquo.laminar.api.L.label(
      cls <-- isSelected.combineWith(disabledSignal).map { (sel: Boolean, dis: Boolean) =>
        if dis then "compact-speed-option compact-speed-option--disabled"
        else if sel then "compact-speed-option compact-speed-option--selected"
        else "compact-speed-option"
      },
      input(
        typ := "radio",
        nameAttr := "compact-manufacturing-speed",
        value := speed.toString,
        checked <-- isSelected,
        disabled <-- disabledSignal,
        com.raquo.laminar.api.L.onChange --> { _ =>
          ProductBuilderViewModel.replaceSpecification(SpecValue.ManufacturingSpeedSpec(speed))
        },
      ),
      span(s"$icon "),
      span(child.text <-- lang.map { l => if l == Language.En then nameEn else nameCs }),
      span(cls := "compact-speed-price", child.text <-- lang.map { l => if l == Language.En then s" ($priceEn)" else s" ($priceCs)" }),
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
    case BindingMethod.SaddleStitch    => lang match { case Language.En => "Saddle Stitch";    case Language.Cs => "V1 – sešitová" }
    case BindingMethod.PerfectBinding  => lang match { case Language.En => "Perfect Binding";  case Language.Cs => "V2 – lepená" }
    case BindingMethod.SpiralBinding   => lang match { case Language.En => "Spiral Binding";   case Language.Cs => "Kroužková" }
    case BindingMethod.WireOBinding    => lang match { case Language.En => "Wire-O Binding";   case Language.Cs => "Wire-O" }
    case BindingMethod.CaseBinding     => lang match { case Language.En => "Case Binding";     case Language.Cs => "V8 – tuhá" }
