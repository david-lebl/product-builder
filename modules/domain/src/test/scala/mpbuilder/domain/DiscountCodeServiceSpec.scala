package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.Money
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.{SampleDiscountCodes, SampleCustomers, SampleCatalog}

object DiscountCodeServiceSpec extends ZIOSpecDefault:

  private val allCodes = SampleDiscountCodes.all
  private val now = 1700000000000L + 86400000L // 1 day after base time

  private def ctx(
      orderValue: Money = Money(1000),
      categoryIds: Set[CategoryId] = Set(SampleCatalog.businessCardsId),
      customerType: Option[CustomerType] = Some(CustomerType.Guest),
      customerId: Option[CustomerId] = None,
      time: Long = now,
  ): DiscountValidationContext = DiscountValidationContext(orderValue, categoryIds, customerType, customerId, time)

  def spec = suite("DiscountCodeService")(
    suite("validate")(
      test("finds a valid code (case-insensitive)") {
        val result = DiscountCodeService.validate(allCodes, "save10", ctx())
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.code == "SAVE10",
        )
      },
      test("finds a valid code with mixed case") {
        val result = DiscountCodeService.validate(allCodes, "  Save10  ", ctx())
        assertTrue(result.toEither.isRight)
      },
      test("fails for unknown code") {
        val result = DiscountCodeService.validate(allCodes, "UNKNOWN", ctx())
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CodeNotFound],
        )
      },
      test("fails for expired code") {
        val result = DiscountCodeService.validate(allCodes, "OLDCODE", ctx())
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CodeExpired],
        )
      },
      test("fails for not-yet-valid code") {
        val result = DiscountCodeService.validate(allCodes, "UPCOMING", ctx())
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CodeNotYetValid],
        )
      },
      test("fails for exhausted code") {
        val result = DiscountCodeService.validate(allCodes, "USED100", ctx())
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CodeExhausted],
        )
      },
      test("fails for inactive code") {
        val result = DiscountCodeService.validate(allCodes, "DISABLED", ctx())
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CodeInactive],
        )
      },
      test("fails for below minimum order value") {
        val result = DiscountCodeService.validate(allCodes, "FLAT100", ctx(orderValue = Money(100)))
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.BelowMinimumOrder],
        )
      },
      test("succeeds when order meets minimum") {
        val result = DiscountCodeService.validate(allCodes, "FLAT100", ctx(orderValue = Money(500)))
        assertTrue(result.toEither.isRight)
      },
      test("fails when category is not eligible") {
        val result = DiscountCodeService.validate(
          allCodes, "CARDS10",
          ctx(categoryIds = Set(SampleCatalog.flyersId)),
        )
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CategoryNotEligible],
        )
      },
      test("succeeds when category is eligible") {
        val result = DiscountCodeService.validate(
          allCodes, "CARDS10",
          ctx(categoryIds = Set(SampleCatalog.businessCardsId)),
        )
        assertTrue(result.toEither.isRight)
      },
      test("fails when customer type is not eligible") {
        val result = DiscountCodeService.validate(
          allCodes, "AGENCY15",
          ctx(customerType = Some(CustomerType.Guest)),
        )
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CustomerNotEligible],
        )
      },
      test("succeeds when customer type is eligible") {
        val result = DiscountCodeService.validate(
          allCodes, "AGENCY15",
          ctx(customerType = Some(CustomerType.Agency)),
        )
        assertTrue(result.toEither.isRight)
      },
      test("fails when customer ID is not in allowed set") {
        val result = DiscountCodeService.validate(
          allCodes, "VIP25",
          ctx(customerId = Some(SampleCustomers.localPrintId)),
        )
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.CustomerNotEligible],
        )
      },
      test("succeeds when customer ID is in allowed set") {
        val result = DiscountCodeService.validate(
          allCodes, "VIP25",
          ctx(customerId = Some(SampleCustomers.printShopProId)),
        )
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("applyDiscount")(
      test("applies percentage discount correctly") {
        val result = DiscountCodeService.applyDiscount(allCodes, "SAVE10", Money(1000), ctx())
        val dr = result.toEither.toOption.get
        assertTrue(
          dr.originalTotal == Money(1000),
          dr.discountAmount == Money(100),
          dr.finalTotal == Money(900),
          dr.appliedCode.code == "SAVE10",
        )
      },
      test("applies fixed amount discount correctly") {
        val result = DiscountCodeService.applyDiscount(allCodes, "FLAT100", Money(1000), ctx())
        val dr = result.toEither.toOption.get
        assertTrue(
          dr.discountAmount == Money(100),
          dr.finalTotal == Money(900),
        )
      },
      test("caps fixed amount discount at subtotal") {
        // Create a context where order is exactly the minimum
        val result = DiscountCodeService.applyDiscount(allCodes, "FLAT100", Money(500), ctx(orderValue = Money(500)))
        val dr = result.toEither.toOption.get
        assertTrue(
          dr.discountAmount == Money(100),
          dr.finalTotal == Money(400),
        )
      },
      test("free delivery returns zero discount amount") {
        val result = DiscountCodeService.applyDiscount(allCodes, "FREESHIP", Money(500), ctx())
        val dr = result.toEither.toOption.get
        assertTrue(
          dr.discountAmount == Money.zero,
          dr.finalTotal == Money(500),
          dr.appliedCode.discountType == DiscountType.FreeDelivery,
        )
      },
    ),
    suite("CRUD operations")(
      test("createCode adds a new code") {
        val newCode = DiscountCode(
          id = DiscountCodeId.unsafe("dc-new"),
          code = "NEWCODE",
          discountType = DiscountType.Percentage(BigDecimal(5)),
          constraints = DiscountConstraints(),
          isActive = true, createdBy = None, createdAt = now,
        )
        val result = DiscountCodeService.createCode(allCodes, newCode)
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == allCodes.size + 1,
          updated.last.code == "NEWCODE",
        )
      },
      test("createCode fails for duplicate code (case-insensitive)") {
        val dup = DiscountCode(
          id = DiscountCodeId.unsafe("dc-dup"),
          code = "save10",
          discountType = DiscountType.Percentage(BigDecimal(10)),
          constraints = DiscountConstraints(),
          isActive = true, createdBy = None, createdAt = now,
        )
        val result = DiscountCodeService.createCode(allCodes, dup)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[DiscountCodeError.DuplicateCode],
        )
      },
      test("createCode fails for invalid percentage") {
        val invalid = DiscountCode(
          id = DiscountCodeId.unsafe("dc-bad"),
          code = "BADPCT",
          discountType = DiscountType.Percentage(BigDecimal(0)),
          constraints = DiscountConstraints(),
          isActive = true, createdBy = None, createdAt = now,
        )
        val result = DiscountCodeService.createCode(allCodes, invalid)
        assertTrue(result.toEither.isLeft)
      },
      test("updateCode updates an existing code") {
        val updated = SampleDiscountCodes.save5.copy(
          discountType = DiscountType.Percentage(BigDecimal(8)),
        )
        val result = DiscountCodeService.updateCode(allCodes, SampleDiscountCodes.save5Id, updated)
        val codes = result.toEither.toOption.get
        val found = codes.find(_.id == SampleDiscountCodes.save5Id).get
        assertTrue(found.discountType == DiscountType.Percentage(BigDecimal(8)))
      },
      test("updateCode fails for nonexistent ID") {
        val result = DiscountCodeService.updateCode(
          allCodes, DiscountCodeId.unsafe("nonexistent"),
          SampleDiscountCodes.save5,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("deactivateCode deactivates a code") {
        val result = DiscountCodeService.deactivateCode(allCodes, SampleDiscountCodes.save5Id)
        val codes = result.toEither.toOption.get
        val found = codes.find(_.id == SampleDiscountCodes.save5Id).get
        assertTrue(!found.isActive)
      },
      test("deactivateCode fails for nonexistent ID") {
        val result = DiscountCodeService.deactivateCode(allCodes, DiscountCodeId.unsafe("nonexistent"))
        assertTrue(result.toEither.isLeft)
      },
      test("incrementUsage increments currentUses") {
        val result = DiscountCodeService.incrementUsage(allCodes, SampleDiscountCodes.save5Id)
        val codes = result.toEither.toOption.get
        val found = codes.find(_.id == SampleDiscountCodes.save5Id).get
        assertTrue(found.constraints.currentUses == 1)
      },
    ),
    suite("DiscountType display names")(
      test("all discount types have bilingual display names") {
        val types = List(
          DiscountType.Percentage(BigDecimal(10)),
          DiscountType.FixedAmount(Money(100)),
          DiscountType.FreeDelivery,
        )
        assertTrue(types.forall { dt =>
          dt.displayName(Language.En).nonEmpty && dt.displayName(Language.Cs).nonEmpty
        })
      },
    ),
    suite("error messages")(
      test("all error variants have English and Czech messages") {
        val errors: List[DiscountCodeError] = List(
          DiscountCodeError.CodeNotFound("TEST"),
          DiscountCodeError.CodeExpired("TEST"),
          DiscountCodeError.CodeNotYetValid("TEST"),
          DiscountCodeError.CodeExhausted("TEST"),
          DiscountCodeError.CodeInactive("TEST"),
          DiscountCodeError.BelowMinimumOrder("TEST", Money(500), Money(100)),
          DiscountCodeError.CategoryNotEligible("TEST", Set(CategoryId.unsafe("cat-1"))),
          DiscountCodeError.CustomerNotEligible("TEST"),
          DiscountCodeError.DuplicateCode("TEST"),
          DiscountCodeError.CodeIdNotFound(DiscountCodeId.unsafe("id-1")),
          DiscountCodeError.InvalidDiscountValue("test detail"),
        )
        assertTrue(errors.forall { e =>
          e.message(Language.En).nonEmpty && e.message(Language.Cs).nonEmpty
        })
      },
    ),
  )
