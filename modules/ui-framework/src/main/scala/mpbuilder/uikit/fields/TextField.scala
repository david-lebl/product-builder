package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*

object TextField:
  def apply(
    label: Signal[String],
    value: Signal[String],
    onInput: Observer[String],
    inputType: String = "text",
    placeholder: Signal[String] = Val(""),
    disabled: Signal[Boolean] = Val(false),
    error: Signal[Option[String]] = Val(None),
    horizontal: Boolean = false,
    helpContent: Option[Signal[String]] = None,
    detailHelp: Option[Signal[Option[String]]] = None,
    mods: Modifier[HtmlElement]*
  ): HtmlElement =
    FormGroup(
      labelText = label,
      error = error,
      horizontal = horizontal,
      helpContent = helpContent,
      detailHelp = detailHelp,
    )(
      input(
        typ := inputType,
        com.raquo.laminar.api.L.placeholder <-- placeholder,
        com.raquo.laminar.api.L.value <-- value,
        com.raquo.laminar.api.L.disabled <-- disabled,
        com.raquo.laminar.api.L.onInput.mapToValue --> onInput,
        mods,
      )
    )
