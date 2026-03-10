package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*

object FormGroup:
  def apply(
    labelText: Signal[String],
    error: Signal[Option[String]] = Val(None),
    mods: Modifier[HtmlElement]*
  )(content: HtmlElement): HtmlElement =
    div(
      cls := "form-group",
      label(child.text <-- labelText),
      content,
      child.maybe <-- error.map(_.map(msg =>
        span(cls := "field-error", msg)
      )),
      mods,
    )
