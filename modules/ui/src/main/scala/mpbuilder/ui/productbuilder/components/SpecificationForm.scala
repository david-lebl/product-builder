package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.domain.model.*
import mpbuilder.domain.service.{CompletionEstimator, TierRestrictionValidator}
import mpbuilder.uikit.fields.{TextField, SelectField, SelectOption}
import mpbuilder.uikit.util.Visibility

object SpecificationForm:
  private val CustomSizeKey = "custom"

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
        cls := "form-group form-group--horizontal",
        div(
          cls := "form-group__label-row",
          label(child.text <-- lang.map {
            case Language.En => "Quantity:"
            case Language.Cs => "Množství:"
          }),
        ),
        div(
          cls := "form-group__control",
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
          presetOptions :+ SelectOption(CustomSizeKey, customLabel)
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
              if v == CustomSizeKey then
                sizePairVar.set(("", ""))
        },
        placeholder = lang.map {
          case Language.En => "-- Select size --"
          case Language.Cs => "-- Vyberte rozměr --"
        },
        horizontal = true,
      ),

      // Custom size inputs - always visible, disabled unless "Custom" is selected
      div(
        cls := "form-group form-group--horizontal",
        div(
          cls := "form-group__label-row",
          label(child.text <-- lang.map {
            case Language.En => "Custom size (mm):"
            case Language.Cs => "Vlastní rozměr (mm):"
          }),
          HelpInfo(lang.map {
            case Language.En => "Use custom size for non-standard dimensions. Enter width and height in millimeters."
            case Language.Cs => "Použijte vlastní rozměr pro nestandardní formáty. Zadejte šířku a výšku v milimetrech."
          }),
        ),
        div(
          cls := "form-group__control",
          div(
            cls := "custom-size-inputs",
            input(
              typ := "number",
              placeholder <-- lang.map {
                case Language.En => "Width"
                case Language.Cs => "Šířka"
              },
              value <-- widthSignal,
              disabled <-- sizePresetVar.signal.map(_ != CustomSizeKey),
              onInput.mapToValue --> { v =>
                val currentH = sizePairVar.now()._2
                sizePairVar.set((v, currentH))
                (v.toDoubleOption, currentH.toDoubleOption) match
                  case (Some(w), Some(h)) if w > 0 && h > 0 =>
                    ProductBuilderViewModel.replaceSpecification(SpecValue.SizeSpec(Dimension(w, h)))
                  case _ => ()
              },
            ),
            span(cls := "custom-size-separator", "\u00d7"),
            input(
              typ := "number",
              placeholder <-- lang.map {
                case Language.En => "Height"
                case Language.Cs => "Výška"
              },
              value <-- heightSignal,
              disabled <-- sizePresetVar.signal.map(_ != CustomSizeKey),
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

      // Pages (for multi-page products)
      div(
        cls := "form-group form-group--horizontal",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.Pages))),
        div(
          cls := "form-group__label-row",
          label(child.text <-- lang.map {
            case Language.En => "Pages:"
            case Language.Cs => "Počet stran:"
          }),
        ),
        div(
          cls := "form-group__control",
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
          horizontal = true,
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
          horizontal = true,
          helpContent = Some(lang.map {
            case Language.En => "How the printed sheet is folded. Half fold creates 4 panels, tri-fold creates 6 panels. Z-fold and accordion are great for step-by-step guides. Gate fold opens like a gate for dramatic reveals."
            case Language.Cs => "Způsob skládání tištěného archu. Půlený sklad vytváří 4 panely, trojsklad 6 panelů. Z-sklad a harmonika jsou skvělé pro postupné návody. Bránový sklad se otevírá jako brána pro dramatické odhalení."
          }),
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
          horizontal = true,
          helpContent = Some(lang.map {
            case Language.En => "How the pages are held together. Saddle stitch (stapled) is cheapest for thin booklets. Perfect binding (glued spine) is for thicker publications. Wire-O and spiral allow the book to lay flat when open."
            case Language.Cs => "Způsob spojení stránek. Sešitová vazba (sešitá) je nejlevnější pro tenké brožury. Lepená vazba (lepený hřbet) je pro silnější publikace. Wire-O a kroužková vazba umožňují, aby kniha ležela naplocho při otevření."
          }),
        ),
      ),
    )

  /** Manufacturing Speed Tier selector — rendered separately from product specifications */
  def manufacturingSpeedSection(): Element =
    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "form-group",

      // Manufacturing Speed Tier (always visible when category is selected)
      div(
        Visibility.when(requiredSpecs.map(_.nonEmpty)),
        div(
          cls := "speed-tier-section",
          div(
            cls := "speed-tier-cards",
            {
              def formatCompletion(est: Option[CompletionEstimator.CompletionEstimate], l: Language): Option[String] =
                est.map { e =>
                  e.formatEarliest(ProductBuilderViewModel.currentLocalDateTime, l)
                }

              // Express: completion estimate + disabled signal from utilisation + tier violations
              val expressEstimate = ProductBuilderViewModel.completionEstimate(ManufacturingSpeed.Express)
              val expressCompletion = expressEstimate.combineWith(lang).map { (est: Option[CompletionEstimator.CompletionEstimate], l: Language) =>
                formatCompletion(est, l)
              }
              val expressViolations = ProductBuilderViewModel.tierViolations(ManufacturingSpeed.Express)
              val expressUtilDisabled = ProductBuilderViewModel.expressAvailable.map(!_)
              val expressViolDisabled = expressViolations.map(_.nonEmpty)
              val expressDisabled = expressUtilDisabled.combineWith(expressViolDisabled).map { (u: Boolean, v: Boolean) => u || v }
              val expressWarning = expressUtilDisabled.combineWith(expressViolations, lang).map { (utilDis: Boolean, viols: List[TierRestrictionValidator.TierViolation], l: Language) =>
                if utilDis then Some(l match
                  case Language.En => "Express not available — high demand"
                  case Language.Cs => "Expres nedostupný — vysoká poptávka"
                )
                else viols.headOption.map(_.message(l))
              }

              val stdEstimate = ProductBuilderViewModel.completionEstimate(ManufacturingSpeed.Standard)
              val stdCompletion = stdEstimate.combineWith(lang).map { (est: Option[CompletionEstimator.CompletionEstimate], l: Language) =>
                formatCompletion(est, l)
              }

              val ecoEstimate = ProductBuilderViewModel.completionEstimate(ManufacturingSpeed.Economy)
              val ecoCompletion = ecoEstimate.combineWith(lang).map { (est: Option[CompletionEstimator.CompletionEstimate], l: Language) =>
                formatCompletion(est, l)
              }

              List(
                speedTierCard(
                  speed = ManufacturingSpeed.Express,
                  icon = "⚡",
                  selected = ProductBuilderViewModel.selectedManufacturingSpeed,
                  lang = lang,
                  disabledSignal = expressDisabled,
                  warningSignal = expressWarning,
                  completionSignal = expressCompletion,
                ),
                speedTierCard(
                  speed = ManufacturingSpeed.Standard,
                  icon = "●",
                  selected = ProductBuilderViewModel.selectedManufacturingSpeed,
                  lang = lang,
                  completionSignal = stdCompletion,
                ),
                speedTierCard(
                  speed = ManufacturingSpeed.Economy,
                  icon = "🐢",
                  selected = ProductBuilderViewModel.selectedManufacturingSpeed,
                  lang = lang,
                  completionSignal = ecoCompletion,
                ),
              )
            },
          ),
        ),
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

  private def speedTierCard(
    speed: ManufacturingSpeed,
    icon: String,
    selected: Signal[Option[ManufacturingSpeed]],
    lang: Signal[Language],
    disabledSignal: Signal[Boolean] = Val(false),
    warningSignal: Signal[Option[String]] = Val(None),
    completionSignal: Signal[Option[String]] = Val(None),
  ): HtmlElement =
    val isSelected = selected.map(_.contains(speed))
    val (nameEn, nameCs) = speed match
      case ManufacturingSpeed.Express  => ("Express", "Expres")
      case ManufacturingSpeed.Standard => ("Standard", "Standardní")
      case ManufacturingSpeed.Economy  => ("Economy", "Ekonomická")
    val (priceEn, priceCs) = speed match
      case ManufacturingSpeed.Express  => ("+35%", "+35 %")
      case ManufacturingSpeed.Standard => ("base price", "základní cena")
      case ManufacturingSpeed.Economy  => ("−15%", "−15 %")
    val (timeEn, timeCs) = speed match
      case ManufacturingSpeed.Express  => ("Same day / next business day", "Tentýž den / příští pracovní den")
      case ManufacturingSpeed.Standard => ("2–5 business days", "2–5 pracovních dnů")
      case ManufacturingSpeed.Economy  => ("5–10 business days", "5–10 pracovních dnů")
    val (descEn, descCs) = speed match
      case ManufacturingSpeed.Express  => ("Ideal for urgent orders", "Ideální pro naléhavé objednávky")
      case ManufacturingSpeed.Standard => ("Recommended for most orders", "Doporučeno pro většinu objednávek")
      case ManufacturingSpeed.Economy  => ("Best value for non-urgent orders", "Nejlepší cena pro neurgentní objednávky")

    val cardCls = isSelected.combineWith(disabledSignal).map { (sel: Boolean, dis: Boolean) =>
      if dis then "speed-tier-card speed-tier-card--disabled"
      else if sel then "speed-tier-card speed-tier-card--selected"
      else "speed-tier-card"
    }

    // Dynamic time text: use completion estimate if available, otherwise fall back to static
    val timeText = completionSignal.combineWith(lang).map { (est: Option[String], l: Language) =>
      est.getOrElse(if l == Language.En then timeEn else timeCs)
    }

    com.raquo.laminar.api.L.label(
      cls <-- cardCls,
      input(
        typ := "radio",
        nameAttr := "manufacturing-speed",
        value := speed.toString,
        checked <-- isSelected,
        disabled <-- disabledSignal,
        com.raquo.laminar.api.L.onChange --> { _ =>
          ProductBuilderViewModel.replaceSpecification(SpecValue.ManufacturingSpeedSpec(speed))
        },
      ),
      div(
        cls := "speed-tier-card__header",
        span(cls := "speed-tier-card__icon", icon),
        span(cls := "speed-tier-card__name", child.text <-- lang.map { l => if l == Language.En then nameEn else nameCs }),
        span(cls := "speed-tier-card__price", child.text <-- lang.map { l => if l == Language.En then priceEn else priceCs }),
      ),
      div(
        cls := "speed-tier-card__time",
        span(cls := "speed-tier-card__time-icon", "🕐"),
        span(child.text <-- timeText),
      ),
      div(
        cls := "speed-tier-card__desc",
        child.text <-- lang.map { l => if l == Language.En then descEn else descCs },
      ),
      // Warning message when disabled
      div(
        cls := "speed-tier-card__warning",
        Visibility.when(warningSignal.map(_.isDefined)),
        child.text <-- warningSignal.map(_.getOrElse("")),
      ),
    )
