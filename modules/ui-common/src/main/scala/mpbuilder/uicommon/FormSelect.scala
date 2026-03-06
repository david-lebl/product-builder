package mpbuilder.uicommon

import com.raquo.laminar.api.L.*

/** A reusable labeled `<select>` with a placeholder option and dynamic option list.
  *
  * The caller provides a reactive list of options (with text, value, and selection state)
  * plus an `onChange` callback receiving the selected value string.
  */
object FormSelect:

  /** Describes a single `<option>` element. */
  case class SelectOption(
    text: String,
    optionValue: String,
    isSelected: Boolean = false,
  )

  /** Labeled select with reactive options.
    *
    * @param labelMod        Modifier for the label element (e.g. `"Category:"` or `child.text <-- signal`)
    * @param optionsSignal   Reactive list of select options (must include placeholder if desired)
    * @param onValueChange   Callback invoked with the selected option's value string
    * @param disabledSignal  Optional reactive disabled state (defaults to always enabled)
    * @param extraMods       Additional modifiers applied to the `<select>` element
    */
  def apply(
    labelMod: Modifier[HtmlElement],
    optionsSignal: Signal[List[SelectOption]],
    onValueChange: String => Unit,
    disabledSignal: Signal[Boolean] = Val(false),
    extraMods: Seq[Modifier[HtmlElement]] = Seq.empty,
  ): HtmlElement =
    div(
      cls := "form-group",
      label(labelMod),
      select(
        disabled <-- disabledSignal,
        children <-- optionsSignal.map(opts =>
          opts.map { opt =>
            option(opt.text, value := opt.optionValue, selected := opt.isSelected)
          }
        ),
        onChange.mapToValue --> { v => onValueChange(v) },
        extraMods,
      ),
    )

  /** Labeled select with reactive options and an info box below.
    *
    * @param labelMod        Modifier for the label element
    * @param optionsSignal   Reactive list of select options
    * @param onValueChange   Callback invoked with the selected option's value string
    * @param infoSignal      Optional reactive info message shown below the select
    * @param disabledSignal  Optional reactive disabled state
    */
  def withInfo(
    labelMod: Modifier[HtmlElement],
    optionsSignal: Signal[List[SelectOption]],
    onValueChange: String => Unit,
    infoSignal: Signal[Option[String]],
    disabledSignal: Signal[Boolean] = Val(false),
  ): HtmlElement =
    div(
      cls := "form-group",
      label(labelMod),
      select(
        disabled <-- disabledSignal,
        children <-- optionsSignal.map(opts =>
          opts.map { opt =>
            option(opt.text, value := opt.optionValue, selected := opt.isSelected)
          }
        ),
        onChange.mapToValue --> { v => onValueChange(v) },
      ),
      div(
        cls := "info-box",
        child.maybe <-- infoSignal.map(_.map(msg => span(msg))),
      ),
    )
