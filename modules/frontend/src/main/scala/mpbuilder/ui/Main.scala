package mpbuilder.ui

import com.raquo.laminar.api.L.*
import org.scalajs.dom

@main def run(): Unit =
  renderOnDomContentLoaded(dom.document.getElementById("app"), App.view())
