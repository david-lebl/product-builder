package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*

case class RadioOption(value: String, label: Signal[String])

object RadioGroup:
  def apply(
    label: Signal[String],
    name: String,
    options: Signal[List[RadioOption]],
    selected: Signal[String],
    onChange: Observer[String],
    disabled: Signal[Boolean] = Val(false),
    mods: Modifier[HtmlElement]*
  ): HtmlElement =
    FormGroup(label)(
      div(
        cls := "radio-group",
        children <-- options.combineWith(selected).map { case (opts, sel) =>
          opts.map { opt =>
            com.raquo.laminar.api.L.label(
              cls := "radio-label",
              input(
                typ := "radio",
                nameAttr := name,
                value := opt.value,
                checked := (opt.value == sel),
                com.raquo.laminar.api.L.onChange.mapToValue --> onChange,
                com.raquo.laminar.api.L.disabled <-- disabled,
              ),
              span(child.text <-- opt.label),
            )
          }
        },
        mods,
      )
    )
