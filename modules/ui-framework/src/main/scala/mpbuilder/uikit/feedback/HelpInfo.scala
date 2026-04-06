package mpbuilder.uikit.feedback

import com.raquo.laminar.api.L.*

object HelpInfo:

  /** Static field-level help button (?) with popup text shown on hover / click. */
  def apply(text: Signal[String]): Element =
    val open = Var(false)
    div(
      cls := "help-info-wrapper",
      button(
        cls := "help-info-trigger",
        typ := "button",
        "?",
        onClick --> { _ => open.update(!_) },
      ),
      div(
        cls <-- open.signal.map(o => if o then "help-info-popup help-info-popup--visible" else "help-info-popup"),
        child.text <-- text,
      ),
      div(
        cls <-- open.signal.map(o => if o then "help-info-backdrop help-info-backdrop--visible" else "help-info-backdrop"),
        onClick --> { _ => open.set(false) },
      ),
    )

  /** Reactive item description button (i) that only appears when a description is available. */
  def fromSignal(description: Signal[Option[String]]): Element =
    val open = Var(false)
    div(
      cls := "help-info-wrapper",
      child.maybe <-- description.map {
        case Some(_) =>
          Some(
            button(
              cls := "help-info-trigger help-info-trigger--detail",
              typ := "button",
              "i",
              onClick --> { _ => open.update(!_) },
            )
          )
        case None => None
      },
      description --> { desc =>
        if desc.isEmpty then open.set(false)
      },
      div(
        cls <-- open.signal.combineWith(description).map { case (o, desc) =>
          if o && desc.isDefined then "help-info-popup help-info-popup--visible" else "help-info-popup"
        },
        child.text <-- description.map(_.getOrElse("")),
      ),
      div(
        cls <-- open.signal.combineWith(description).map { case (o, desc) =>
          if o && desc.isDefined then "help-info-backdrop help-info-backdrop--visible" else "help-info-backdrop"
        },
        onClick --> { _ => open.set(false) },
      ),
    )
