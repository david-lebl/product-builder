package mpbuilder.ui

import mpbuilder.domain.{Currency, LocalizedText, Money}

/** Stage 1 renders Czech; switching to English is this one constant. */
object I18n:
  def t(text: LocalizedText): String = text.cs

object Format:
  def money(m: Money): String =
    val amount = m.amount.setScale(2, BigDecimal.RoundingMode.HALF_UP)
    m.currency match
      case Currency.CZK => s"$amount Kč"
      case Currency.USD => s"$$$amount"
