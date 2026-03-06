package mpbuilder.uicommon

import com.raquo.laminar.api.L.*

/** Reusable titled form section wrapper.
  *
  * Renders a `div.form-section` with an `<h3>` title and arbitrary child content.
  * Used to group related form fields under a heading — e.g. "1. Select Product Category".
  */
object FormSection:

  /** Simple titled section with static or reactive title and child content. */
  def apply(
    titleMod: Modifier[HtmlElement],
    mods: Modifier[HtmlElement]*,
  ): HtmlElement =
    div(
      cls := "form-section",
      h3(titleMod),
      mods,
    )

  /** Section without a title heading — just a `div.form-section` wrapper. */
  def untitled(
    mods: Modifier[HtmlElement]*,
  ): HtmlElement =
    div(
      cls := "form-section",
      mods,
    )

  /** Section that can be conditionally hidden via a reactive signal. */
  def conditional(
    titleMod: Modifier[HtmlElement],
    visibleSignal: Signal[Boolean],
    mods: Modifier[HtmlElement]*,
  ): HtmlElement =
    div(
      cls := "form-section",
      display <-- visibleSignal.map(v => if v then "block" else "none"),
      h3(titleMod),
      mods,
    )
