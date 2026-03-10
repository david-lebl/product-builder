package mpbuilder.uikit.util

import com.raquo.laminar.api.L.*

object Visibility:
  def when(visible: Signal[Boolean]): Modifier[HtmlElement] =
    display <-- visible.map(v => if v then "" else "none")

  def unless(hidden: Signal[Boolean]): Modifier[HtmlElement] =
    display <-- hidden.map(h => if h then "none" else "")
