package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.components.*
import mpbuilder.domain.model.Language

object ProductBuilderApp:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "main-content",

      // Language selector
      div(
        cls := "language-selector",
        label("Language / Jazyk: "),
        select(
          option("English", value := "en"),
          option("Čeština", value := "cs"),
          onChange.mapToValue --> { value =>
            value match
              case "cs" => ProductBuilderViewModel.setLanguage(Language.Cs)
              case _    => ProductBuilderViewModel.setLanguage(Language.En)
          },
        ),
      ),
      
      // Left side: Configuration form
      div(
        cls := "card",
        h2(child.text <-- lang.map {
          case Language.En => "Configure Your Product"
          case Language.Cs => "Nakonfigurujte svůj produkt"
        }),
        
        ConfigurationForm(),
      ),
      
      // Right side: Price preview
      div(
        cls := "price-section",
        PricePreview(),
        ValidationMessages(),
      ),
    )
