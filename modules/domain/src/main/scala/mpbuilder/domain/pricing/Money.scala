package mpbuilder.domain.pricing

opaque type Money = BigDecimal
object Money:
  def apply(value: BigDecimal): Money = value
  def apply(value: Double): Money = BigDecimal(value)
  def apply(value: Int): Money = BigDecimal(value)
  def apply(value: String): Money = BigDecimal(value)

  val zero: Money = BigDecimal(0)

  extension (m: Money)
    def value: BigDecimal = m
    def +(other: Money): Money = m + other
    def *(factor: BigDecimal): Money = m * factor
    def *(qty: Int): Money = m * BigDecimal(qty)
    def rounded: Money = m.setScale(2, BigDecimal.RoundingMode.HALF_UP)

enum Currency:
  case USD, EUR, GBP, CZK

final case class Price(amount: Money, currency: Currency):
  def +(other: Price): Price =
    require(currency == other.currency, "Cannot add prices with different currencies")
    Price(amount + other.amount, currency)

  def *(factor: BigDecimal): Price = Price(amount * factor, currency)

object Price:
  def zero(currency: Currency): Price = Price(Money.zero, currency)
