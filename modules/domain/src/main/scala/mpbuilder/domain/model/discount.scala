package mpbuilder.domain.model

import mpbuilder.domain.pricing.Money

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
    allowedCustomerTypes: Set[CustomerType] = Set.empty,
    allowedCustomerIds: Set[CustomerId] = Set.empty,
)

/** A discount code entity with type, constraints, and usage tracking */
final case class DiscountCode(
    id: DiscountCodeId,
    code: String,
    discountType: DiscountType,
    constraints: DiscountConstraints,
    isActive: Boolean,
    createdBy: Option[EmployeeId],
    createdAt: Long,
):
  /** Increment the usage counter in the nested constraints. */
  def withIncrementedUsage: DiscountCode =
    copy(constraints = constraints.copy(currentUses = constraints.currentUses + 1))

/** Result of applying a discount code to a subtotal */
final case class DiscountResult(
    originalTotal: Money,
    discountAmount: Money,
    finalTotal: Money,
    appliedCode: DiscountCode,
)
