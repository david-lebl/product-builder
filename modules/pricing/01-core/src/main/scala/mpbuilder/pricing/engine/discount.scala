package mpbuilder.pricing.engine

import mpbuilder.product.model.*

import mpbuilder.commons.*
import zio.prelude.Validation


/** Type of discount applied by a discount code */
enum DiscountType:
  case Percentage(value: BigDecimal)
  case FixedAmount(value: Money)
  case FreeDelivery

  def displayName: LocalizedString = this match
    case Percentage(_)  => LocalizedString("Percentage", "Procentuální")
    case FixedAmount(_) => LocalizedString("Fixed Amount", "Pevná částka")
    case FreeDelivery   => LocalizedString("Free Delivery", "Doprava zdarma")

/** Constraints that govern when a discount code can be used */
final case class DiscountConstraints(
    validFrom: Option[Long] = None,
    validUntil: Option[Long] = None,
    maxUses: Option[Int] = None,
    currentUses: Int = 0,
    minimumOrderValue: Option[Money] = None,
    allowedCategories: Set[CategoryId] = Set.empty,
    allowedCustomerTypes: Set[String] = Set.empty,
    allowedCustomerIds: Set[CustomerRef] = Set.empty,
)

/** A discount code entity with type, constraints, and usage tracking */
final case class DiscountCode(
    id: DiscountCodeId,
    code: String,
    discountType: DiscountType,
    constraints: DiscountConstraints,
    isActive: Boolean,
    createdBy: Option[String],
    createdAt: Long,
)

/** Result of applying a discount code to a subtotal */
final case class DiscountResult(
    originalTotal: Money,
    discountAmount: Money,
    finalTotal: Money,
    appliedCode: DiscountCode,
)

/** Pricing owns discount codes, so it owns their identity too. */
opaque type DiscountCodeId = String
object DiscountCodeId:
  def apply(value: String): Validation[String, DiscountCodeId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("DiscountCodeId must not be empty")

  def unsafe(value: String): DiscountCodeId = value

  extension (id: DiscountCodeId) def value: String = id
