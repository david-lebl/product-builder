package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*

case class SelectOption(value: String, display: String)

object SelectField:
  def apply(
    label: Signal[String],
    options: Signal[List[SelectOption]],
    selected: Signal[String],
    onChange: Observer[String],
    placeholder: Signal[String] = Val(""),
    disabled: Signal[Boolean] = Val(false),
    error: Signal[Option[String]] = Val(None),
    mods: Modifier[HtmlElement]*
  ): HtmlElement =
    FormGroup(label, error)(
      select(
        com.raquo.laminar.api.L.disabled <-- disabled,
        children <-- options.combineWith(selected, placeholder).map { case (opts, sel, ph) =>
          val placeholderOpt =
            if ph.nonEmpty then
              List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
            else Nil
          placeholderOpt ++ opts.map { opt =>
            option(
              opt.display,
              value := opt.value,
              com.raquo.laminar.api.L.selected := (opt.value == sel),
            )
          }
        },
        com.raquo.laminar.api.L.onChange.mapToValue --> onChange,
        mods,
      )
    )
