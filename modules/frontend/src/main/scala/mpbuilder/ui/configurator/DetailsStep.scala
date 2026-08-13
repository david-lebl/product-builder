package mpbuilder.ui.configurator

import com.raquo.laminar.api.L.*
import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import mpbuilder.ui.I18n.t

object DetailsStep:

  /** Printing method, ink, and the details the category requires (size,
    * quantity, orientation, pages, fold, binding), plus production speed.
    */
  def view(state: ConfiguratorState): HtmlElement =
    div(
      cls := "details-step",
      h3("Specifikace"),
      methodSelect(state),
      inkSelect(state),
      child <-- state.category.combineWith(state.config).map {
        (category: Option[Category], config: Option[ProductConfiguration]) =>
          (category, config) match
            case (Some(cat), Some(cfg)) => detailFields(state, cat, cfg)
            case _                      => div()
      },
      speedSelect(state),
    )

  private def methodSelect(state: ConfiguratorState): HtmlElement =
    div(
      cls := "field",
      label("Technologie tisku"),
      select(
        children <-- state.printingMethods.combineWith(state.config).map {
          (methods: List[PrintingMethod], cfg: Option[ProductConfiguration]) =>
            methods.map { m =>
              option(
                value    := m.id.raw,
                selected := cfg.exists(_.printingMethodId == m.id),
                t(m.name),
              )
            }
        },
        onChange.mapToValue --> (v => state.setPrintingMethod(PrintingMethodId(v))),
      ),
    )

  private def inkSelect(state: ConfiguratorState): HtmlElement =
    div(
      cls := "field",
      label("Barevnost"),
      select(
        children <-- state.inkConfigs.combineWith(state.config).map {
          (inks: List[InkConfiguration], cfg: Option[ProductConfiguration]) =>
            inks.map { i =>
              option(
                value    := i.id.raw,
                selected := cfg.exists(_.inkConfigurationId == i.id),
                t(i.name),
              )
            }
        },
        onChange.mapToValue --> (v => state.setInk(InkConfigId(v))),
      ),
    )

  private def detailFields(
    state: ConfiguratorState,
    category: Category,
    cfg: ProductConfiguration,
  ): HtmlElement =
    import RequiredDetail.*
    val d = cfg.details
    div(
      when(category.requiredDetails.contains(Size))(
        div(
          cls := "field field-size",
          label("Rozměr (mm)"),
          div(
            cls := "size-inputs",
            input(
              typ := "number", minAttr := "1", placeholder := "šířka",
              value := d.size.map(_.widthMm.toString).getOrElse(""),
              onInput.mapToValue --> { v =>
                v.toIntOption.filter(_ > 0).foreach { w =>
                  state.updateDetails(det => det.copy(size = Some(DimensionsMm(w, det.size.map(_.heightMm).getOrElse(0).max(1)))))
                }
              },
            ),
            span("×"),
            input(
              typ := "number", minAttr := "1", placeholder := "výška",
              value := d.size.map(_.heightMm.toString).getOrElse(""),
              onInput.mapToValue --> { v =>
                v.toIntOption.filter(_ > 0).foreach { h =>
                  state.updateDetails(det => det.copy(size = Some(DimensionsMm(det.size.map(_.widthMm).getOrElse(0).max(1), h))))
                }
              },
            ),
          ),
        )
      ),
      when(category.requiredDetails.contains(Quantity))(
        div(
          cls := "field",
          label("Množství (ks)"),
          input(
            typ := "number", minAttr := "1",
            value := d.quantity.map(_.toString).getOrElse(""),
            onInput.mapToValue --> { v =>
              state.updateDetails(_.copy(quantity = v.toIntOption.filter(_ > 0)))
            },
          ),
        )
      ),
      when(category.requiredDetails.contains(Orientation))(
        div(
          cls := "field",
          label("Orientace"),
          select(
            option(value := "Portrait", selected := d.orientation.contains(config.Orientation.Portrait), "Na výšku"),
            option(value := "Landscape", selected := d.orientation.contains(config.Orientation.Landscape), "Na šířku"),
            onChange.mapToValue --> (v =>
              state.updateDetails(_.copy(orientation = Some(config.Orientation.valueOf(v))))
            ),
          ),
        )
      ),
      when(category.requiredDetails.contains(Pages))(
        div(
          cls := "field",
          label("Počet stran"),
          input(
            typ := "number", minAttr := "4", stepAttr := "2",
            value := d.pages.map(_.toString).getOrElse(""),
            onInput.mapToValue --> { v =>
              state.updateDetails(_.copy(pages = v.toIntOption.filter(_ > 0)))
            },
          ),
        )
      ),
      when(category.requiredDetails.contains(Fold))(
        div(
          cls := "field",
          label("Typ lomu"),
          select(
            FoldType.values.toList.map { ft =>
              option(value := ft.toString, selected := d.foldType.contains(ft), t(Labels.fold(ft)))
            },
            onChange.mapToValue --> (v =>
              state.updateDetails(_.copy(foldType = Some(FoldType.valueOf(v))))
            ),
          ),
        )
      ),
      when(category.requiredDetails.contains(Binding))(
        div(
          cls := "field",
          label("Vazba"),
          select(
            BindingMethod.values.toList.map { b =>
              option(value := b.toString, selected := d.bindingMethod.contains(b), t(Labels.binding(b)))
            },
            onChange.mapToValue --> (v =>
              state.updateDetails(_.copy(bindingMethod = Some(BindingMethod.valueOf(v))))
            ),
          ),
        )
      ),
    )

  private def speedSelect(state: ConfiguratorState): HtmlElement =
    div(
      cls := "field",
      label("Rychlost výroby"),
      div(
        cls := "speed-options",
        SpeedTier.values.toList.map { tier =>
          label(
            cls := "speed-option",
            input(
              typ  := "radio",
              nameAttr := "speed",
              checked <-- state.config.map(_.exists(_.speedTier == tier)),
              onInput --> (_ => state.setSpeed(tier)),
            ),
            span(s" ${t(Labels.speed(tier))}${speedNote(tier)}"),
          )
        },
      ),
    )

  private def speedNote(tier: SpeedTier): String = tier match
    case SpeedTier.Express  => " (+35 %)"
    case SpeedTier.Standard => ""
    case SpeedTier.Economy  => " (−15 %)"
