package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*

object FormGroup:
  def apply(
    labelText: Signal[String],
    error: Signal[Option[String]] = Val(None),
    mods: Modifier[HtmlElement]*
  )(content: HtmlElement): HtmlElement =
    div(
      cls := "form-group form-group--horizontal",
      label(child.text <-- labelText),
      div(
        cls := "form-group__control",
        content,
        child.maybe <-- error.map(_.map(msg =>
          span(cls := "field-error", msg)
        )),
      ),
      mods,
    )
