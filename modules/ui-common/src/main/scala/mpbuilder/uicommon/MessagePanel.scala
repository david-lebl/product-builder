package mpbuilder.uicommon

import com.raquo.laminar.api.L.*

/** Reusable message panel for displaying success, error, warning, or info messages.
  *
  * Provides static and reactive variants.
  */
object MessagePanel:

  /** A success message box. */
  def success(mods: Modifier[HtmlElement]*): HtmlElement =
    div(cls := "success-message", mods)

  /** An error message box with a title and a list of error strings. */
  def error(
    titleMod: Modifier[HtmlElement],
    messages: List[String],
  ): HtmlElement =
    div(
      cls := "error-message",
      strong(titleMod),
      ul(
        cls := "error-list",
        messages.map(msg => li(msg)),
      ),
    )

  /** A generic info box. */
  def info(mods: Modifier[HtmlElement]*): HtmlElement =
    div(cls := "info-box", mods)

  /** Reactive success/error panel driven by signals.
    *
    * Shows a success message when `showSuccess` emits `true`,
    * and an error list when `errors` is non-empty.
    *
    * @param showSuccess     Signal indicating whether to show the success message
    * @param successContent  Modifier for the success message content
    * @param errors          Reactive list of error strings
    * @param errorTitle      Modifier for the error list title
    */
  def reactive(
    showSuccess: Signal[Boolean],
    successContent: Modifier[HtmlElement],
    errors: Signal[List[String]],
    errorTitle: Modifier[HtmlElement],
  ): HtmlElement =
    div(
      // Success
      child.maybe <-- showSuccess.map { ok =>
        if ok then Some(div(cls := "success-message", successContent))
        else None
      },
      // Errors
      child.maybe <-- errors.map { errs =>
        if errs.nonEmpty then
          Some(div(
            cls := "error-message",
            strong(errorTitle),
            ul(cls := "error-list", errs.map(msg => li(msg))),
          ))
        else None
      },
    )
