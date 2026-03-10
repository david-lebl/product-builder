package mpbuilder.uikit.form

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.fields.{TextField, CheckboxField}

object FormRenderer:
  def render[T](
    form: FormState[T],
    config: Map[String, FieldConfig] = Map.empty,
  ): HtmlElement =
    div(
      cls := "form-derived",
      form.fieldOrder.map { name =>
        val field = form.fields(name)
        val cfg = config.getOrElse(name, FieldConfig())
        val labelSignal = cfg.label.getOrElse(Val(humanize(name)))
        renderField(field, labelSignal, cfg)
      },
    )

  private def renderField(
    field: FormFieldState[?],
    labelSignal: Signal[String],
    cfg: FieldConfig,
  ): HtmlElement =
    field.fieldType.inputKind match
      case InputKind.Checkbox =>
        CheckboxField(
          label = labelSignal,
          checked = field.rawVar.signal.map(_ == "true"),
          onChange = field.rawVar.writer.contramap[Boolean](_.toString),
        )
      case InputKind.Text(defaultType) =>
        val inputType = cfg.inputType.getOrElse(defaultType)
        TextField(
          label = labelSignal,
          value = field.rawVar.signal,
          onInput = field.rawVar.writer,
          inputType = inputType,
          placeholder = cfg.placeholder.getOrElse(Val("")),
          error = field.firstError,
        )
      case InputKind.Select(options) =>
        // For select types, fall back to text input for now
        TextField(
          label = labelSignal,
          value = field.rawVar.signal,
          onInput = field.rawVar.writer,
          error = field.firstError,
        )

  private def humanize(name: String): String =
    name.flatMap { c =>
      if c.isUpper then s" $c" else s"$c"
    }.trim.capitalize
