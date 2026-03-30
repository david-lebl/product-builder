package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.Money
import zio.prelude.*
import com.softwaremill.quicklens.*

/** Context for validating a discount code against an order */
final case class DiscountValidationContext(
    orderValue: Money,
    categoryIds: Set[CategoryId],
    customerType: Option[CustomerType],
    customerId: Option[CustomerId],
    now: Long,
)

/** Pure service for discount code management — replaces hardcoded DiscountService. */
object DiscountCodeService:

  /** Look up a discount code by its string value (case-insensitive). */
  def findByCode(codes: List[DiscountCode], code: String): Option[DiscountCode] =
    val normalized = code.trim.toUpperCase
    codes.find(_.code.toUpperCase == normalized)

  /** Validate a discount code against order context. Returns the code if valid. */
  def validate(
      codes: List[DiscountCode],
      code: String,
      context: DiscountValidationContext,
  ): Validation[DiscountCodeError, DiscountCode] =
    findByCode(codes, code) match
      case None => Validation.fail(DiscountCodeError.CodeNotFound(code.trim))
      case Some(dc) =>
        for
          _ <- validateActive(dc)
          _ <- validateNotExpired(dc, context.now)
          _ <- validateStarted(dc, context.now)
          _ <- validateNotExhausted(dc)
          _ <- validateMinimumOrder(dc, context.orderValue)
          _ <- validateCategories(dc, context.categoryIds)
          _ <- validateCustomer(dc, context.customerType, context.customerId)
        yield dc

  /** Apply a validated discount code to a subtotal, returning a DiscountResult. */
  def applyDiscount(
      codes: List[DiscountCode],
      code: String,
      subtotal: Money,
      context: DiscountValidationContext,
  ): Validation[DiscountCodeError, DiscountResult] =
    validate(codes, code, context).map { dc =>
      val discountAmount = dc.discountType match
        case DiscountType.Percentage(pct) =>
          subtotal * (pct / BigDecimal(100))
        case DiscountType.FixedAmount(amount) =>
          // Cap the discount at the subtotal (don't go negative)
          if amount.value > subtotal.value then subtotal else amount
        case DiscountType.FreeDelivery =>
          Money.zero // Delivery discount is handled separately; no subtotal reduction

      val finalTotal = Money(subtotal.value - discountAmount.value).atLeast(Money.zero)

      DiscountResult(
        originalTotal = subtotal,
        discountAmount = discountAmount,
        finalTotal = finalTotal,
        appliedCode = dc,
      )
    }

  // --- CRUD operations ---

  /** Create a new discount code. Validates uniqueness. */
  def createCode(
      codes: List[DiscountCode],
      newCode: DiscountCode,
  ): Validation[DiscountCodeError, List[DiscountCode]] =
    for
      _ <- validateUniqueCode(codes, newCode.code, None)
      _ <- validateDiscountValue(newCode.discountType)
    yield codes :+ newCode

  /** Update an existing discount code. */
  def updateCode(
      codes: List[DiscountCode],
      codeId: DiscountCodeId,
      updatedCode: DiscountCode,
  ): Validation[DiscountCodeError, List[DiscountCode]] =
    for
      _ <- validateCodeIdExists(codes, codeId)
      _ <- validateUniqueCode(codes, updatedCode.code, Some(codeId))
      _ <- validateDiscountValue(updatedCode.discountType)
    yield codes.map(c => if c.id == codeId then updatedCode else c)

  /** Deactivate a discount code. */
  def deactivateCode(
      codes: List[DiscountCode],
      codeId: DiscountCodeId,
  ): Validation[DiscountCodeError, List[DiscountCode]] =
    for _ <- validateCodeIdExists(codes, codeId)
    yield codes.map(c => if c.id == codeId then c.copy(isActive = false) else c)

  /** Increment usage count for a discount code. */
  def incrementUsage(
      codes: List[DiscountCode],
      codeId: DiscountCodeId,
  ): Validation[DiscountCodeError, List[DiscountCode]] =
    for _ <- validateCodeIdExists(codes, codeId)
    yield codes.map { c =>
      if c.id == codeId then
        c.modify(_.constraints.currentUses).using(_ + 1)
      else c
    }

  // --- Validation helpers ---

  private def validateActive(dc: DiscountCode): Validation[DiscountCodeError, Unit] =
    if dc.isActive then Validation.unit
    else Validation.fail(DiscountCodeError.CodeInactive(dc.code))

  private def validateNotExpired(dc: DiscountCode, now: Long): Validation[DiscountCodeError, Unit] =
    dc.constraints.validUntil match
      case Some(until) if now > until => Validation.fail(DiscountCodeError.CodeExpired(dc.code))
      case _ => Validation.unit

  private def validateStarted(dc: DiscountCode, now: Long): Validation[DiscountCodeError, Unit] =
    dc.constraints.validFrom match
      case Some(from) if now < from => Validation.fail(DiscountCodeError.CodeNotYetValid(dc.code))
      case _ => Validation.unit

  private def validateNotExhausted(dc: DiscountCode): Validation[DiscountCodeError, Unit] =
    dc.constraints.maxUses match
      case Some(max) if dc.constraints.currentUses >= max =>
        Validation.fail(DiscountCodeError.CodeExhausted(dc.code))
      case _ => Validation.unit

  private def validateMinimumOrder(dc: DiscountCode, orderValue: Money): Validation[DiscountCodeError, Unit] =
    dc.constraints.minimumOrderValue match
      case Some(min) if orderValue.value < min.value =>
        Validation.fail(DiscountCodeError.BelowMinimumOrder(dc.code, min, orderValue))
      case _ => Validation.unit

  private def validateCategories(dc: DiscountCode, categoryIds: Set[CategoryId]): Validation[DiscountCodeError, Unit] =
    if dc.constraints.allowedCategories.isEmpty then Validation.unit
    else if categoryIds.exists(dc.constraints.allowedCategories.contains) then Validation.unit
    else Validation.fail(DiscountCodeError.CategoryNotEligible(dc.code, categoryIds))

  private def validateCustomer(
      dc: DiscountCode,
      customerType: Option[CustomerType],
      customerId: Option[CustomerId],
  ): Validation[DiscountCodeError, Unit] =
    val typeOk = dc.constraints.allowedCustomerTypes.isEmpty ||
      customerType.exists(dc.constraints.allowedCustomerTypes.contains)
    val idOk = dc.constraints.allowedCustomerIds.isEmpty ||
      customerId.exists(dc.constraints.allowedCustomerIds.contains)
    if typeOk && idOk then Validation.unit
    else Validation.fail(DiscountCodeError.CustomerNotEligible(dc.code))

  private def validateUniqueCode(
      codes: List[DiscountCode],
      code: String,
      excludeId: Option[DiscountCodeId],
  ): Validation[DiscountCodeError, Unit] =
    val normalized = code.trim.toUpperCase
    val exists = codes.exists { c =>
      c.code.toUpperCase == normalized && !excludeId.contains(c.id)
    }
    if exists then Validation.fail(DiscountCodeError.DuplicateCode(code.trim))
    else Validation.unit

  private def validateCodeIdExists(
      codes: List[DiscountCode],
      codeId: DiscountCodeId,
  ): Validation[DiscountCodeError, Unit] =
    if codes.exists(_.id == codeId) then Validation.unit
    else Validation.fail(DiscountCodeError.CodeIdNotFound(codeId))

  private def validateDiscountValue(dt: DiscountType): Validation[DiscountCodeError, Unit] =
    dt match
      case DiscountType.Percentage(v) =>
        if v > BigDecimal(0) && v <= BigDecimal(100) then Validation.unit
        else Validation.fail(DiscountCodeError.InvalidDiscountValue(s"Percentage must be between 0 and 100, got $v"))
      case DiscountType.FixedAmount(v) =>
        if v.value > BigDecimal(0) then Validation.unit
        else Validation.fail(DiscountCodeError.InvalidDiscountValue(s"Fixed amount must be positive, got ${v.value}"))
      case DiscountType.FreeDelivery => Validation.unit
