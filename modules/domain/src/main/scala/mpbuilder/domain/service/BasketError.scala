package mpbuilder.domain.service

import mpbuilder.domain.model.*

enum BasketError:
  case InvalidQuantity(quantity: Int)
  case ConfigurationNotFound(configurationId: ConfigurationId)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case InvalidQuantity(quantity) => lang match
      case Language.En => s"Invalid quantity: $quantity. Quantity must be positive."
      case Language.Cs => s"Neplatné množství: $quantity. Množství musí být kladné."
    case ConfigurationNotFound(configurationId) => lang match
      case Language.En => s"Configuration not found in basket: ${configurationId.value}"
      case Language.Cs => s"Konfigurace nebyla nalezena v košíku: ${configurationId.value}"
