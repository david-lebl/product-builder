package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.components.*

object ProductBuilderApp:
  def apply(): Element =
    div(
      cls := "main-content",
      
      // Left side: Configuration form
      div(
        cls := "card",
        h2("Configure Your Product"),
        
        ConfigurationForm(),
      ),
      
      // Right side: Price preview
      div(
        cls := "price-section",
        PricePreview(),
        ValidationMessages(),
      ),
    )
