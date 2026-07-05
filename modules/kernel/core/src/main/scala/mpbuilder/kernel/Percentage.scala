package mpbuilder.kernel

import zio.prelude.*

/** A percentage value in the range 0–100. */
opaque type Percentage = BigDecimal
object Percentage:
  def apply(value: BigDecimal): Validation[String, Percentage] =
    if value >= BigDecimal(0) && value <= BigDecimal(100) then Validation.succeed(value)
    else Validation.fail(s"Percentage must be between 0 and 100, got $value")

  def unsafe(value: BigDecimal): Percentage = value

  val zero: Percentage = BigDecimal(0)

  extension (p: Percentage)
    def value: BigDecimal = p

    /** Apply this percentage as a discount to the given money amount.
      * E.g. 10% applied to 100 returns 90 (the discounted price).
      */
    def applyTo(money: Money): Money =
      money * (BigDecimal(1) - p / BigDecimal(100))