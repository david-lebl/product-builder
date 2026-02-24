package mpbuilder.ui.components

import com.raquo.laminar.api.L.*

/** A single item in a SelectField dropdown. */
case class SelectOption(value: String, label: String)

/** Reusable labelled `<select>` field used by the various selector components. */
object SelectField:

  def apply(
      labelSignal: Signal[String],
      placeholderSignal: Signal[String],
      optionsSignal: Signal[List[SelectOption]],
      selectedValue: Signal[String],
      onSelect: String => Unit,
      disabledSignal: Signal[Boolean] = Val(false),
  ): Element =
    div(
      cls := "form-group",
      label(child.text <-- labelSignal),
      select(
        disabled <-- disabledSignal,
        children <-- placeholderSignal.combineWith(optionsSignal, selectedValue).map {
          case (placeholder, opts, currentVal) =>
            option(placeholder, value := "", selected := currentVal.isEmpty) ::
            opts.map { opt =>
              option(opt.label, value := opt.value, selected := opt.value == currentVal)
            }
        },
        onChange.mapToValue --> { v =>
          if v.nonEmpty then onSelect(v)
        },
      ),
    )
