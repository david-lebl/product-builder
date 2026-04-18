package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.domain.model.*
import mpbuilder.domain.sample.SampleCatalog
import mpbuilder.domain.service.{CompletionEstimator, TierRestrictionValidator}
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
        cls := "form-group form-group--horizontal",
        label(child.text <-- lang.map {
          case Language.En => "Quantity:"
          case Language.Cs => "Množství:"
        }),
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

      // Size: preset selector + always-visible width/height fields
      div(
        cls := "form-group form-group--horizontal",
        label(child.text <-- lang.map {
          case Language.En => "Size:"
          case Language.Cs => "Rozměr:"
        }),
        div(
          cls := "form-group__control size-composite-field",
          select(
            cls := "size-preset-select",
            children <-- lang.combineWith(sizePresetVar.signal).map { case (l, sel) =>
              val ph = l match
                case Language.En => "-- Select size --"
                case Language.Cs => "-- Vyberte rozměr --"
              val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
              val presetOptions = SizePreset.values.toList.map { preset =>
                option(preset.label(l), value := preset.key, com.raquo.laminar.api.L.selected := (preset.key == sel))
              }
              val customLabel = l match
                case Language.En => "Custom"
                case Language.Cs => "Vlastní"
              placeholderOpt ++ presetOptions :+ option(customLabel, value := "custom", com.raquo.laminar.api.L.selected := (sel == "custom"))
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
          div(
            cls := "size-dimensions-row",
            input(
              typ := "number",
              cls := "size-dim-input",
              placeholder <-- lang.map {
                case Language.En => "W (mm)"
                case Language.Cs => "Š (mm)"
              },
              disabled <-- sizePresetVar.signal.map(v => v.nonEmpty && v != "custom"),
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
            span(cls := "size-dim-separator", "\u00d7"),
            input(
              typ := "number",
              cls := "size-dim-input",
              placeholder <-- lang.map {
                case Language.En => "H (mm)"
                case Language.Cs => "V (mm)"
              },
              disabled <-- sizePresetVar.signal.map(v => v.nonEmpty && v != "custom"),
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

      // Pages (for multi-page products)
      div(
        cls := "form-group form-group--horizontal",
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.Pages))),
        label(child.text <-- lang.map {
          case Language.En => "Pages:"
          case Language.Cs => "Stran:"
        }),
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
        div(
          cls := "form-group form-group--horizontal",
          label(child.text <-- lang.map {
            case Language.En => "Orientation:"
            case Language.Cs => "Orientace:"
          }),
          div(
            cls := "form-group__control",
            select(
              children <-- lang.combineWith(ProductBuilderViewModel.selectedOrientation).map { case (l, selOpt) =>
                val sel = selOpt match
                  case Some(Orientation.Portrait) => "portrait"
                  case Some(Orientation.Landscape) => "landscape"
                  case None => ""
                val ph = l match
                  case Language.En => "-- Select orientation --"
                  case Language.Cs => "-- Vyberte orientaci --"
                val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
                placeholderOpt ++ List(
                  option(l match { case Language.En => "Portrait"; case Language.Cs => "Na výšku" }, value := "portrait", com.raquo.laminar.api.L.selected := (sel == "portrait")),
                  option(l match { case Language.En => "Landscape"; case Language.Cs => "Na šířku" }, value := "landscape", com.raquo.laminar.api.L.selected := (sel == "landscape")),
                )
              },
              onChange.mapToValue --> Observer[String] { value =>
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
      ),

      // Fold Type (required for Brochures)
      div(
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.FoldType))),
        div(
          cls := "form-group form-group--horizontal",
          div(
            cls := "label-with-help",
            label(child.text <-- lang.map {
              case Language.En => "Fold Type:"
              case Language.Cs => "Typ skladu:"
            }),
            HelpInfo(lang.map {
              case Language.En => "How the printed sheet is folded. Half fold creates 4 panels, tri-fold creates 6 panels. Z-fold and accordion are great for step-by-step guides. Gate fold opens like a gate for dramatic reveals."
              case Language.Cs => "Způsob skládání tištěného archu. Půlený sklad vytváří 4 panely, trojsklad 6 panelů. Z-sklad a harmonika jsou skvělé pro postupné návody. Bránový sklad se otevírá jako brána pro dramatické odhalení."
            }),
          ),
          div(
            cls := "form-group__control",
            select(
              children <-- lang.combineWith(ProductBuilderViewModel.selectedFoldType).map { case (l, selOpt) =>
                val sel = selOpt.map(_.toString).getOrElse("")
                val ph = l match
                  case Language.En => "-- Select fold type --"
                  case Language.Cs => "-- Vyberte typ skladu --"
                val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
                placeholderOpt ++ FoldType.values.toList.map { ft =>
                  option(foldTypeLabel(ft, l), value := ft.toString, com.raquo.laminar.api.L.selected := (ft.toString == sel))
                }
              },
              onChange.mapToValue --> Observer[String] { value =>
                if value.nonEmpty then
                  FoldType.values.find(_.toString == value).foreach { ft =>
                    ProductBuilderViewModel.removeSpecification(classOf[SpecValue.FoldTypeSpec])
                    ProductBuilderViewModel.addSpecification(SpecValue.FoldTypeSpec(ft))
                  }
              },
            ),
          ),
        ),
      ),

      // Binding Method (required for Booklets)
      div(
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.BindingMethod))),
        div(
          cls := "form-group form-group--horizontal",
          div(
            cls := "label-with-help",
            label(child.text <-- lang.map {
              case Language.En => "Binding:"
              case Language.Cs => "Typ vazby:"
            }),
            HelpInfo(lang.map {
              case Language.En => "How the pages are held together. Saddle stitch (stapled) is cheapest for thin booklets. Perfect binding (glued spine) is for thicker publications. Plastic O-Binding uses a plastic ring; Metal Wire Binding uses a metal wire — both allow the book to lay flat when open."
              case Language.Cs => "Způsob spojení stránek. Sešitová vazba (sešitá) je nejlevnější pro tenké brožury. Lepená vazba (lepený hřbet) je pro silnější publikace. Plastová O-vazba používá plastový kroužek; kovová drátová vazba používá kovový drát — obě umožňují, aby kniha ležela naplocho při otevření."
            }),
          ),
          div(
            cls := "form-group__control",
            select(
              children <-- lang.combineWith(ProductBuilderViewModel.selectedBindingMethod).map { case (l, selOpt) =>
                val sel = selOpt.map(_.toString).getOrElse("")
                val ph = l match
                  case Language.En => "-- Select binding method --"
                  case Language.Cs => "-- Vyberte typ vazby --"
                val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
                placeholderOpt ++ BindingMethod.values.toList.map { bm =>
                  option(bindingMethodLabel(bm, l), value := bm.toString, com.raquo.laminar.api.L.selected := (bm.toString == sel))
                }
              },
              onChange.mapToValue --> Observer[String] { value =>
                if value.nonEmpty then
                  BindingMethod.values.find(_.toString == value).foreach { bm =>
                    ProductBuilderViewModel.removeSpecification(classOf[SpecValue.BindingMethodSpec])
                    ProductBuilderViewModel.addSpecification(SpecValue.BindingMethodSpec(bm))
                  }
              },
            ),
          ),
        ),
      ),

      // Calendar Cover Option (required for Calendars)
      div(
        Visibility.when(requiredSpecs.map(_.contains(SpecKind.CalendarCover))),
        div(
          cls := "form-group form-group--horizontal",
          div(
            cls := "label-with-help",
            label(child.text <-- lang.map {
              case Language.En => "Physical Cover:"
              case Language.Cs => "Fyzický kryt:"
            }),
            HelpInfo(lang.map {
              case Language.En => "Add a physical protective cover to the calendar. Front cover is a transparent plastic sheet. Back cover is 350gsm cardboard in the colour of your choice."
              case Language.Cs => "Přidejte fyzický ochranný kryt ke kalendáři. Přední kryt je průhledná plastová fólie. Zadní kryt je kartón 350g ve vámi zvolené barvě."
            }),
          ),
          div(
            cls := "form-group__control",
            select(
              children <-- lang.combineWith(ProductBuilderViewModel.selectedCalendarCover).map {
                case (l, selOpt) =>
                  val sel = selOpt.map(_._1.toString).getOrElse("")
                  val ph = l match
                    case Language.En => "-- Select cover option --"
                    case Language.Cs => "-- Vyberte typ krytu --"
                  val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
                  placeholderOpt ++ CalendarCoverOption.values.toList.map { opt =>
                    option(calendarCoverLabel(opt, l), value := opt.toString, com.raquo.laminar.api.L.selected := (opt.toString == sel))
                  }
              },
              onChange.mapToValue --> Observer[String] { value =>
                if value.nonEmpty then
                  CalendarCoverOption.values.find(_.toString == value).foreach { opt =>
                    val currentColor = ProductBuilderViewModel.selectedCalendarCover.now().flatMap(_._2)
                    val backColor = if opt == CalendarCoverOption.BackOnly || opt == CalendarCoverOption.FrontAndBack then
                      Some(currentColor.getOrElse(CoverColor.White))
                    else None
                    ProductBuilderViewModel.removeSpecification(classOf[SpecValue.CalendarCoverSpec])
                    ProductBuilderViewModel.addSpecification(SpecValue.CalendarCoverSpec(opt, backColor))
                  }
              },
            ),
          ),
        ),
        // Back cover colour (shown only when back cover is selected)
        div(
          Visibility.when(ProductBuilderViewModel.selectedCalendarCover.map { sel =>
            sel.exists { case (opt, _) => opt == CalendarCoverOption.BackOnly || opt == CalendarCoverOption.FrontAndBack }
          }),
          cls := "form-group form-group--horizontal",
          label(child.text <-- lang.map {
            case Language.En => "Back Cover Colour:"
            case Language.Cs => "Barva zadního krytu:"
          }),
          div(
            cls := "form-group__control",
            select(
              children <-- lang.combineWith(ProductBuilderViewModel.selectedCalendarCover).map {
                case (l, selOpt) =>
                  val sel = selOpt.flatMap(_._2).map(_.toString).getOrElse("")
                  val ph = l match
                    case Language.En => "-- Select colour --"
                    case Language.Cs => "-- Vyberte barvu --"
                  val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
                  placeholderOpt ++ CoverColor.values.toList.map { c =>
                    option(coverColorLabel(c, l), value := c.toString, com.raquo.laminar.api.L.selected := (c.toString == sel))
                  }
              },
              onChange.mapToValue --> Observer[String] { value =>
                if value.nonEmpty then
                  CoverColor.values.find(_.toString == value).foreach { c =>
                    val currentOpt = ProductBuilderViewModel.selectedCalendarCover.now().map(_._1).getOrElse(CalendarCoverOption.BackOnly)
                    ProductBuilderViewModel.removeSpecification(classOf[SpecValue.CalendarCoverSpec])
                    ProductBuilderViewModel.addSpecification(SpecValue.CalendarCoverSpec(currentOpt, Some(c)))
                  }
              },
            ),
          ),
        ),
      ),

      // Binding Colour (shown for Plastic O-Binding or Metal Wire Binding)
      div(
        Visibility.when(ProductBuilderViewModel.selectedBindingMethod.map {
          case Some(BindingMethod.PlasticOBinding) | Some(BindingMethod.MetalWireBinding) => true
          case _ => false
        }),
        div(
          cls := "form-group form-group--horizontal",
          div(
            cls := "label-with-help",
            label(child.text <-- lang.map {
              case Language.En => "Binding Colour:"
              case Language.Cs => "Barva vazby:"
            }),
            HelpInfo(lang.map {
              case Language.En => "Choose the colour of the plastic ring or metal wire. Black is standard and has no extra charge. Other colours have a small surcharge."
              case Language.Cs => "Vyberte barvu plastového kroužku nebo kovového drátu. Černá je standardní a bez příplatku. Ostatní barvy mají malý příplatek."
            }),
          ),
          div(
            cls := "form-group__control",
            select(
              children <-- lang.combineWith(ProductBuilderViewModel.selectedBindingColor).map {
                case (l, selOpt) =>
                  val sel = selOpt.map(_.toString).getOrElse("")
                  val ph = l match
                    case Language.En => "-- Select colour --"
                    case Language.Cs => "-- Vyberte barvu --"
                  val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
                  placeholderOpt ++ BindingColor.values.toList.map { c =>
                    option(bindingColorLabel(c, l), value := c.toString, com.raquo.laminar.api.L.selected := (c.toString == sel))
                  }
              },
              onChange.mapToValue --> Observer[String] { value =>
                if value.nonEmpty then
                  BindingColor.values.find(_.toString == value).foreach { c =>
                    ProductBuilderViewModel.removeSpecification(classOf[SpecValue.BindingColorSpec])
                    ProductBuilderViewModel.addSpecification(SpecValue.BindingColorSpec(c))
                  }
              },
            ),
          ),
        ),
      ),

      div(
        cls := "info-note",
        span(child.text <-- lang.map {
          case Language.En => "Additional options may appear based on your product category."
          case Language.Cs => "Další možnosti se mohou zobrazit na základě kategorie produktu."
        }),
      ),
    )

  /** Manufacturing Speed Tier selector — rendered separately from product specifications */
  def manufacturingSpeedSection(): Element =
    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      // Manufacturing Speed Tier (always visible when category is selected)
      div(
        Visibility.when(requiredSpecs.map(_.nonEmpty)),
        div(
          cls := "speed-tier-section",
          div(
            cls := "speed-tier-cards speed-tier-cards--horizontal",
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
    case BindingMethod.SaddleStitch    => lang match { case Language.En => "Saddle Stitch";       case Language.Cs => "V1 – sešitová vazba" }
    case BindingMethod.PerfectBinding  => lang match { case Language.En => "Perfect Binding";     case Language.Cs => "V2 – lepená vazba" }
    case BindingMethod.PlasticOBinding => lang match { case Language.En => "Plastic O-Binding";   case Language.Cs => "Plastová O-vazba" }
    case BindingMethod.MetalWireBinding => lang match { case Language.En => "Metal Wire Binding"; case Language.Cs => "Kovová drátová vazba" }
    case BindingMethod.CaseBinding     => lang match { case Language.En => "Case Binding";        case Language.Cs => "V8 – tuhá vazba" }

  private def calendarCoverLabel(opt: CalendarCoverOption, lang: Language): String = opt match
    case CalendarCoverOption.NoCover      => lang match { case Language.En => "No cover";                             case Language.Cs => "Bez krytu" }
    case CalendarCoverOption.FrontOnly    => lang match { case Language.En => "Front only (transparent plastic)";     case Language.Cs => "Pouze přední (průhledný plast)" }
    case CalendarCoverOption.BackOnly     => lang match { case Language.En => "Back only (350gsm cardboard)";         case Language.Cs => "Pouze zadní (kartón 350g)" }
    case CalendarCoverOption.FrontAndBack => lang match { case Language.En => "Front + back (plastic + cardboard)";   case Language.Cs => "Přední + zadní (plast + kartón)" }

  private def coverColorLabel(c: CoverColor, lang: Language): String = c match
    case CoverColor.White => lang match { case Language.En => "White"; case Language.Cs => "Bílá" }
    case CoverColor.Black => lang match { case Language.En => "Black"; case Language.Cs => "Černá" }
    case CoverColor.Red   => lang match { case Language.En => "Red";   case Language.Cs => "Červená" }
    case CoverColor.Blue  => lang match { case Language.En => "Blue";  case Language.Cs => "Modrá" }
    case CoverColor.Green => lang match { case Language.En => "Green"; case Language.Cs => "Zelená" }

  private def bindingColorLabel(c: BindingColor, lang: Language): String = c match
    case BindingColor.Black  => lang match { case Language.En => "Black (standard)"; case Language.Cs => "Černá (standard)" }
    case BindingColor.White  => lang match { case Language.En => "White";            case Language.Cs => "Bílá" }
    case BindingColor.Silver => lang match { case Language.En => "Silver";           case Language.Cs => "Stříbrná" }
    case BindingColor.Gold   => lang match { case Language.En => "Gold";             case Language.Cs => "Zlatá" }
    case BindingColor.Red    => lang match { case Language.En => "Red";              case Language.Cs => "Červená" }
    case BindingColor.Blue   => lang match { case Language.En => "Blue";             case Language.Cs => "Modrá" }

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
