package mpbuilder.calculator

import mpbuilder.commons.*

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import mpbuilder.ui.productbuilder.{BuilderEnvironment, ProductBuilderViewModel}

/** Options a host page can pass to [[CalculatorWidget.mount]]. */
final case class CalculatorConfig(
    language: Language,
    orderEmail: String,
)

/** Public entry point of the embeddable price calculator.
  *
  * The module is linked without a main initializer, so nothing runs until the
  * host page calls `MPCalculator.mount(...)`. The same artifact backs both
  * integration modes: a host page mounting into its own `<div>`, and the
  * bundled `index.html` that sites embed in an `<iframe>`.
  */
@JSExportTopLevel("MPCalculator")
object CalculatorWidget:

  /** Mount the calculator into the element matched by `selector`.
    *
    * @param selector CSS selector for the container element
    * @param config   optional `{ lang, orderEmail }` object
    */
  @JSExport
  def mount(selector: String, config: js.UndefOr[js.Dynamic]): Unit =
    Option(dom.document.querySelector(selector)) match
      case None =>
        dom.console.error(s"[MPCalculator] No element matches selector '$selector' — nothing mounted.")
      case Some(container) =>
        val cfg = parseConfig(config)

        BuilderEnvironment.init(CalculatorEnvironment.environment(cfg))
        ProductBuilderViewModel.initializeLanguage(cfg.language)

        val node = CalculatorApp()
        if dom.document.readyState == "loading" then
          renderOnDomContentLoaded(container, node)
        else
          render(container, node)

  private def parseConfig(config: js.UndefOr[js.Dynamic]): CalculatorConfig =
    val obj = config.toOption.filter(_ != null)

    def str(key: String): Option[String] =
      obj
        .flatMap(o => Option(o.selectDynamic(key)))
        .map(_.toString)
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter(v => v != "undefined" && v != "null")

    CalculatorConfig(
      language = str("lang").map(Language.fromCode).getOrElse(detectLanguage()),
      orderEmail = str("orderEmail").getOrElse(""),
    )

  /** Stored preference first, then the browser's language, then English. */
  private def detectLanguage(): Language =
    val stored =
      try Option(dom.window.localStorage.getItem("selectedLanguage"))
      catch case _: js.JavaScriptException => None

    stored match
      case Some(code) => Language.fromCode(code)
      case None =>
        if dom.window.navigator.language.toLowerCase().startsWith("cs") then Language.Cs
        else Language.En
