package mpbuilder.uicommon

import com.raquo.laminar.api.L.*

/** Reusable form field wrapper: a `div.form-group` containing a label and arbitrary content.
  *
  * Optionally supports a reactive validation error signal. When an error is present,
  * the field gains a `form-group--error` CSS class and a small error hint is rendered
  * below the content.
  */
object FormField:

  /** Simple form field with a label and content — no validation. */
  def apply(
    labelMod: Modifier[HtmlElement],
    mods: Modifier[HtmlElement]*,
  ): HtmlElement =
    div(
      cls := "form-group",
      label(labelMod),
      mods,
    )

  /** Form field with optional reactive validation error. */
  def validated(
    labelMod: Modifier[HtmlElement],
    errorSignal: Signal[Option[String]],
    mods: Modifier[HtmlElement]*,
  ): HtmlElement =
    div(
      cls := "form-group",
      cls <-- errorSignal.map(e => if e.isDefined then "form-group--error" else ""),
      label(labelMod),
      mods,
      child.maybe <-- errorSignal.map(_.map(msg =>
        span(cls := "form-field-error", msg)
      )),
    )
