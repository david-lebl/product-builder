package mpbuilder.ui

import mpbuilder.commons.*

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import mpbuilder.ui.productbuilder.{BuilderEnvironment, ProductBuilderViewModel}

object Main:
  def main(args: Array[String]): Unit =
    // Tell the shared product configurator it is running inside the full SPA:
    // simulated shop-floor queue, visual editor artwork, checkout wizard.
    BuilderEnvironment.init(FullAppEnvironment.environment)

    // Detect browser language from navigator.language or localStorage
    val detectedLanguage = detectBrowserLanguage()

    // Initialize the view model with the detected language
    ProductBuilderViewModel.initializeLanguage(detectedLanguage)
    
    renderOnDomContentLoaded(
      dom.document.getElementById("app-root"),
      AppRouter()
    )
  
  private def detectBrowserLanguage(): Language =
    try
      // First check if user has previously selected a language
      val storedLang = Option(dom.window.localStorage.getItem("selectedLanguage"))
      
      storedLang match
        case Some(code) => Language.fromCode(code)
        case None => browserLanguageFromNavigator()
    catch
      case _: scala.scalajs.js.JavaScriptException =>
        // If localStorage access fails (e.g., private browsing), fall back to browser language
        browserLanguageFromNavigator()
  
  private def browserLanguageFromNavigator(): Language =
    val browserLang = dom.window.navigator.language.toLowerCase()
    if browserLang.startsWith("cs") then Language.Cs
    else Language.En
