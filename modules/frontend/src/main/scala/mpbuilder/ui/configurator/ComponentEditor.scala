package mpbuilder.ui.configurator

import com.raquo.laminar.api.L.*
import mpbuilder.domain.Labels
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import mpbuilder.ui.I18n.t

object ComponentEditor:

  /** Material + finishes editors for every component of the chosen category,
    * including add/remove for optional components (e.g. a roll-up's stand).
    */
  def view(state: ConfiguratorState): HtmlElement =
    div(
      cls := "component-editor",
      children <-- state.category.combineWith(state.config).map {
        (category: Option[Category], config: Option[ProductConfiguration]) =>
          (category, config) match
            case (Some(cat), Some(cfg)) => cat.components.map(spec => componentSection(state, spec, cfg))
            case _                      => Nil
      },
    )

  private def componentSection(
    state: ConfiguratorState,
    spec: ComponentSpec,
    cfg: ProductConfiguration,
  ): HtmlElement =
    val present = cfg.components.exists(_.role == spec.role)
    div(
      cls := "component-section",
      div(
        cls := "component-header",
        h3(t(Labels.componentRole(spec.role))),
        when(spec.optional)(
          label(
            cls := "optional-toggle",
            input(
              typ     := "checkbox",
              checked := present,
              onInput --> (_ => state.toggleOptionalComponent(spec)),
            ),
            span(if present then " odebrat" else " přidat"),
          )
        ),
      ),
      when(present)(
        div(
          materialSelect(state, spec.role),
          finishChecklist(state, spec.role),
        )
      ),
    )

  private def materialSelect(state: ConfiguratorState, role: ComponentRole): HtmlElement =
    div(
      cls := "field",
      label("Materiál"),
      select(
        children <-- state.materialsFor(role).combineWith(state.config).map {
          (materials: List[Material], cfg: Option[ProductConfiguration]) =>
            val selectedId = cfg.flatMap(_.component(role)).map(_.materialId)
            materials.map { m =>
              option(
                value    := m.id.raw,
                selected := selectedId.contains(m.id),
                t(m.name) + m.weightGsm.map(g => s" ($g g/m²)").getOrElse(""),
              )
            }
        },
        onChange.mapToValue --> (v => state.setMaterial(role, MaterialId(v))),
      ),
    )

  private def finishChecklist(state: ConfiguratorState, role: ComponentRole): HtmlElement =
    div(
      cls := "field",
      label("Zušlechtění"),
      div(
        cls := "finish-list",
        children <-- state.finishesFor(role).combineWith(state.config).map {
          (finishes: List[(Finish, Boolean)], cfg: Option[ProductConfiguration]) =>
            if finishes.isEmpty then List(span(cls := "muted", "Žádné dostupné zušlechtění"))
            else
              finishes.map { (finish, isSelected) =>
                val selectedParams =
                  cfg.flatMap(_.component(role)).flatMap(_.finishes.find(_.finishId == finish.id)).flatMap(_.params)
                div(
                  cls := "finish-item",
                  label(
                    input(
                      typ     := "checkbox",
                      checked := isSelected,
                      onInput --> (_ => state.toggleFinish(role, finish)),
                    ),
                    span(s" ${t(finish.name)}"),
                  ),
                  when(isSelected)(paramsEditor(state, role, finish, selectedParams)),
                )
              }
        },
      ),
    )

  private def paramsEditor(
    state: ConfiguratorState,
    role: ComponentRole,
    finish: Finish,
    current: Option[FinishParams],
  ): HtmlElement =
    finish.finishType match
      case FinishType.Scoring =>
        val creases = current match { case Some(FinishParams.Creases(n)) => n; case _ => 1 }
        inlineParam(
          "Počet rylů",
          select(
            (1 to 8).map(n => option(value := n.toString, selected := n == creases, n.toString)),
            onChange.mapToValue --> (v =>
              v.toIntOption.foreach(n => state.setFinishParams(role, finish.id, FinishParams.Creases(n)))
            ),
          ),
        )
      case FinishType.Grommets =>
        val spacing = current match { case Some(FinishParams.GrommetSpacing(s)) => s; case _ => 500 }
        inlineParam(
          "Rozteč oček",
          select(
            List(300, 500).map(s => option(value := s.toString, selected := s == spacing, s"$s mm")),
            onChange.mapToValue --> (v =>
              v.toIntOption.foreach(s => state.setFinishParams(role, finish.id, FinishParams.GrommetSpacing(s)))
            ),
          ),
        )
      case FinishType.GumRope =>
        val metres = current match { case Some(FinishParams.RopeLength(m)) => m; case _ => BigDecimal(1) }
        inlineParam(
          "Délka (m)",
          input(
            typ      := "number",
            minAttr  := "0.5",
            stepAttr := "0.5",
            value    := metres.toString,
            onInput.mapToValue --> (v =>
              v.toDoubleOption.filter(_ > 0).foreach(m =>
                state.setFinishParams(role, finish.id, FinishParams.RopeLength(BigDecimal(m)))
              )
            ),
          ),
        )
      case FinishType.RoundCorners =>
        val (corners, radius) = current match
          case Some(FinishParams.RoundCorners(c, r)) => (c, r)
          case _                                     => (4, 3)
        div(
          cls := "finish-params",
          inlineParam(
            "Počet rohů",
            select(
              (1 to 4).map(n => option(value := n.toString, selected := n == corners, n.toString)),
              onChange.mapToValue --> (v =>
                v.toIntOption.foreach(c =>
                  state.setFinishParams(role, finish.id, FinishParams.RoundCorners(c, radius))
                )
              ),
            ),
          ),
          inlineParam(
            "Rádius (mm)",
            select(
              List(3, 6, 10).map(r => option(value := r.toString, selected := r == radius, s"$r mm")),
              onChange.mapToValue --> (v =>
                v.toIntOption.foreach(r =>
                  state.setFinishParams(role, finish.id, FinishParams.RoundCorners(corners, r))
                )
              ),
            ),
          ),
        )
      case _ => div()

  private def inlineParam(labelText: String, control: HtmlElement): HtmlElement =
    div(cls := "finish-params", label(cls := "param-label", labelText), control)
