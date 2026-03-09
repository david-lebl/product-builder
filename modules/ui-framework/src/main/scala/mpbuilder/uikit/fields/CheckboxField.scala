package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*

object CheckboxField:
  def apply(
    label: Signal[String],
    checked: Signal[Boolean],
    onChange: Observer[Boolean],
    disabled: Signal[Boolean] = Val(false),
    mods: Modifier[HtmlElement]*
  ): HtmlElement =
    div(
      cls := "form-group",
      com.raquo.laminar.api.L.label(
        cls := "checkbox-label",
        input(
          typ := "checkbox",
          com.raquo.laminar.api.L.checked <-- checked,
          com.raquo.laminar.api.L.disabled <-- disabled,
          com.raquo.laminar.api.L.onChange.mapToChecked --> onChange,
        ),
        span(child.text <-- label),
      ),
      mods,
    )
