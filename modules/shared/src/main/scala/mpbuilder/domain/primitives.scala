package mpbuilder.domain

import zio.json.*

import scala.math.BigDecimal.RoundingMode

enum Currency derives JsonCodec:
  case CZK, USD

/** Exact monetary amount. All intermediate pricing results are rounded to two
  * decimal places via [[round2]] as they are produced (spec §5.11), so no
  * rounding error can accumulate across steps.
  */
final case class Money(amount: BigDecimal, currency: Currency) derives JsonCodec:
  private def sameCurrency(other: Money): Unit =
    require(currency == other.currency, s"currency mismatch: $currency vs ${other.currency}")

  def +(other: Money): Money = { sameCurrency(other); copy(amount = amount + other.amount) }
  def -(other: Money): Money = { sameCurrency(other); copy(amount = amount - other.amount) }
  def *(factor: BigDecimal): Money = copy(amount = amount * factor)
  def round2: Money = copy(amount = amount.setScale(2, RoundingMode.HALF_UP))
  def max(other: Money): Money = { sameCurrency(other); if amount >= other.amount then this else other }
  def <(other: Money): Boolean = { sameCurrency(other); amount < other.amount }
  def isZero: Boolean = amount == BigDecimal(0)

object Money:
  def zero(currency: Currency): Money = Money(BigDecimal(0), currency)

final case class LocalizedText(en: String, cs: String) derives JsonCodec

object LocalizedText:
  /** For labels that are identical in both languages (proper names, numbers). */
  def plain(text: String): LocalizedText = LocalizedText(text, text)

/** Finished-item trim size in millimetres (before bleed). */
final case class DimensionsMm(widthMm: Int, heightMm: Int) derives JsonCodec:
  def areaM2: BigDecimal = BigDecimal(widthMm) * BigDecimal(heightMm) / BigDecimal(1000000)

/** A per-category allow-list; `All` means unrestricted (Free Configuration). */
enum AllowList[A]:
  case All[A]() extends AllowList[A]
  case Only(ids: Set[A])

  def allows(id: A): Boolean = this match
    case All()     => true
    case Only(ids) => ids.contains(id)

object AllowList:
  def of[A](ids: A*): AllowList[A] = Only(ids.toSet)

  given [A: {JsonEncoder, JsonDecoder}] => JsonCodec[AllowList[A]] =
    JsonCodec.derived
