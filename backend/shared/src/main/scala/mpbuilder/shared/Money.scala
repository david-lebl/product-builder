package mpbuilder.shared

import zio.prelude.Validation
import java.math.MathContext

/** Monetary value represented as arbitrary-precision `BigDecimal`.
  *
  * No `Double` constructor exists by design to prevent floating-point drift.
  * All rounding uses `HALF_UP` to 2 decimal places.
  */
opaque type Money = BigDecimal
object Money:
  val zero: Money = BigDecimal(0)

  def apply(value: BigDecimal): Money = value
  def apply(value: Int): Money        = BigDecimal(value)
  def apply(value: Long): Money       = BigDecimal(value)
  def apply(value: String): Money     = BigDecimal(value)

  def nonNegative(value: BigDecimal): Validation[String, Money] =
    if value >= BigDecimal(0) then Validation.succeed(value)
    else Validation.fail(s"Money must be non-negative, got $value")

  given scala.math.Ordering[Money] = scala.math.Ordering.BigDecimal

  private[shared] def round2(m: BigDecimal): Money =
    m.setScale(2, BigDecimal.RoundingMode.HALF_UP)

  extension (m: Money)
    def value: BigDecimal       = m
    def +(other: Money): Money  = m + other
    def -(other: Money): Money  = m - other
    def unary_- : Money         = -m
    def *(factor: BigDecimal): Money = m * factor
    def *(qty: Int): Money      = m * BigDecimal(qty)
    def /(divisor: Int): Money  =
      BigDecimal(m.bigDecimal.divide(BigDecimal(divisor).bigDecimal, MathContext.DECIMAL128))
    def /(divisor: BigDecimal): Money =
      BigDecimal(m.bigDecimal.divide(divisor.bigDecimal, MathContext.DECIMAL128))
    def atLeast(floor: Money): Money = if m >= floor then m else floor
    def atMost(cap: Money): Money    = if m <= cap then m else cap
    def rounded: Money               = Money.round2(m)
    def isZero: Boolean              = m == BigDecimal(0)
    def isPositive: Boolean          = m > BigDecimal(0)
    def isNegative: Boolean          = m < BigDecimal(0)
    def <(other: Money): Boolean     = m < other
    def <=(other: Money): Boolean    = m <= other
    def >(other: Money): Boolean     = m > other
    def >=(other: Money): Boolean    = m >= other

enum Currency:
  case CZK, EUR, USD, GBP

/** Currency-tagged monetary amount. */
final case class Price(amount: Money, currency: Currency):
  def +(other: Price): Validation[String, Price] =
    if currency == other.currency then Validation.succeed(Price(amount + other.amount, currency))
    else Validation.fail(s"Cannot add prices in different currencies: $currency vs ${other.currency}")

  def *(factor: BigDecimal): Price = Price(Money.round2(amount.value * factor), currency)
  def rounded: Price               = Price(Money.round2(amount.value), currency)

object Price:
  def zero(currency: Currency): Price = Price(Money.zero, currency)

/** Percentage in `[0, 100]`. */
opaque type Percentage = BigDecimal
object Percentage:
  val zero: Percentage = BigDecimal(0)
  val hundred: Percentage = BigDecimal(100)

  def apply(value: BigDecimal): Validation[String, Percentage] =
    if value >= BigDecimal(0) && value <= BigDecimal(100) then Validation.succeed(value)
    else Validation.fail(s"Percentage must be between 0 and 100, got $value")

  def unsafe(value: BigDecimal): Percentage = value

  extension (p: Percentage)
    def value: BigDecimal = p
    /** Multiplier representing a discount of `p`% (e.g. 15% -> 0.85). */
    def discountMultiplier: BigDecimal = BigDecimal(1) - p / BigDecimal(100)
    /** Apply as a discount to `money` and round to currency precision. */
    def applyDiscount(money: Money): Money = Money.round2(money.value * p.discountMultiplier)
    /** Fraction of `money` corresponding to `p`% (e.g. 20% of 100 -> 20). */
    def of(money: Money): Money = Money.round2(money.value * (p / BigDecimal(100)))

/** VAT / sales tax rate in `[0, 100]`. */
opaque type TaxRate = BigDecimal
object TaxRate:
  val zero: TaxRate       = BigDecimal(0)
  val czStandard: TaxRate = BigDecimal(21)
  val czReduced: TaxRate  = BigDecimal(12)

  def apply(value: BigDecimal): Validation[String, TaxRate] =
    if value >= BigDecimal(0) && value <= BigDecimal(100) then Validation.succeed(value)
    else Validation.fail(s"TaxRate must be between 0 and 100, got $value")

  def unsafe(value: BigDecimal): TaxRate = value

  extension (r: TaxRate)
    def value: BigDecimal             = r
    def taxOnNet(net: Money): Money   = Money.round2(net.value * (r / BigDecimal(100)))
    def netToGross(net: Money): Money = Money.round2(net.value + r.taxOnNet(net).value)
