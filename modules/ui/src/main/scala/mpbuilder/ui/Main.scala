package mpbuilder.ui

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import mpbuilder.domain.model.Language

object Main:
  def main(args: Array[String]): Unit =
    // Detect browser language from navigator.language or localStorage
    val detectedLanguage = detectBrowserLanguage()
    
    // Initialize the view model with the detected language
    ProductBuilderViewModel.initializeLanguage(detectedLanguage)
    
    renderOnDomContentLoaded(
      dom.document.getElementById("app-root"),
      ProductBuilderApp()
    )
  
  private def detectBrowserLanguage(): Language =
    // First check if user has previously selected a language
    val storedLang = Option(dom.window.localStorage.getItem("selectedLanguage"))
    
    storedLang match
      case Some("Cs") => Language.Cs
      case Some("En") => Language.En
      case _ =>
        // Fall back to browser language detection
        val browserLang = dom.window.navigator.language.toLowerCase()
        if browserLang.startsWith("cs") then Language.Cs
        else Language.En
