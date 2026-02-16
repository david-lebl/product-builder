package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.components.*
import mpbuilder.domain.model.Language

object ProductBuilderApp:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      // Language selector - positioned outside the grid
      div(
        cls := "language-selector",
        label("Language / Jazyk: "),
        select(
          value <-- lang.map(_.toCode),
          option("English", value := "en"),
          option("Čeština", value := "cs"),
          onChange.mapToValue --> { code =>
            ProductBuilderViewModel.setLanguage(Language.fromCode(code))
          },
        ),
      ),
      
      // Main content grid
      div(
        cls := "main-content",
        
        // Left side: Configuration form
        div(
          cls := "card",
          h2(child.text <-- lang.map {
            case Language.En => "Configure Your Product"
            case Language.Cs => "Nakonfigurujte svůj produkt"
          }),
          
          ConfigurationForm(),
        ),
        
        // Right side: Price preview and basket
        div(
          cls := "price-section",
          PricePreview(),
          ValidationMessages(),
          BasketView(),
        ),
      ),
    )
