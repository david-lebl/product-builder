package mpbuilder.uikit.showcase

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.example.UiKitShowcase
import org.scalajs.dom

object Main:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app-root"), UiKitShowcase())
