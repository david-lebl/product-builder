package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.feedback.HelpInfo

object FormGroup:
  def apply(
    labelText: Signal[String],
    error: Signal[Option[String]] = Val(None),
    horizontal: Boolean = false,
    helpContent: Option[Signal[String]] = None,
    detailHelp: Option[Signal[Option[String]]] = None,
    mods: Modifier[HtmlElement]*
  )(content: HtmlElement): HtmlElement =
    div(
      cls := (if horizontal then "form-group form-group--horizontal" else "form-group"),
      div(
        cls := "form-group__label-row",
        label(child.text <-- labelText),
        child.maybe <-- Val(helpContent.map(HelpInfo(_))),
        child.maybe <-- Val(detailHelp.map(HelpInfo.fromSignal)),
      ),
      div(
        cls := "form-group__control",
        content,
        child.maybe <-- error.map(_.map(msg =>
          span(cls := "field-error", msg)
        )),
      ),
      mods,
    )
