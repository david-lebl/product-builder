package mpbuilder.uikit.feedback

import com.raquo.laminar.api.L.*

object ValidationDisplay:
  def apply(
    errors: Signal[List[String]],
    success: Signal[Option[String]] = Val(None),
  ): HtmlElement =
    div(
      // Success message
      child.maybe <-- success.map(_.map(msg =>
        div(cls := "success-message", msg)
      )),
      // Error messages
      child.maybe <-- errors.map { errs =>
        if errs.nonEmpty then
          Some(
            div(
              cls := "error-message",
              ul(
                cls := "error-list",
                errs.map(e => li(e)),
              ),
            )
          )
        else None
      },
    )
