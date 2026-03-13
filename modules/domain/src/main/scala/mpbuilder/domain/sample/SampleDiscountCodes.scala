package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.Money

object SampleDiscountCodes:

  // --- Discount Code IDs ---
  val save5Id: DiscountCodeId       = DiscountCodeId.unsafe("dc-save5")
  val save10Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save10")
  val save15Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save15")
  val save20Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save20")
  val save25Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save25")
  val save30Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save30")
  val save40Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save40")
  val save50Id: DiscountCodeId      = DiscountCodeId.unsafe("dc-save50")
  val welcome20Id: DiscountCodeId   = DiscountCodeId.unsafe("dc-welcome20")
  val summer10Id: DiscountCodeId    = DiscountCodeId.unsafe("dc-summer10")
  val print15Id: DiscountCodeId     = DiscountCodeId.unsafe("dc-print15")
  val fixedOff100Id: DiscountCodeId = DiscountCodeId.unsafe("dc-fixed-off-100")
  val freeShipId: DiscountCodeId    = DiscountCodeId.unsafe("dc-free-ship")
  val vipOnlyId: DiscountCodeId     = DiscountCodeId.unsafe("dc-vip-only")
  val expiredId: DiscountCodeId     = DiscountCodeId.unsafe("dc-expired")
  val futureId: DiscountCodeId      = DiscountCodeId.unsafe("dc-future")
  val exhaustedId: DiscountCodeId   = DiscountCodeId.unsafe("dc-exhausted")
  val inactiveId: DiscountCodeId    = DiscountCodeId.unsafe("dc-inactive")
  val cardsOnlyId: DiscountCodeId   = DiscountCodeId.unsafe("dc-cards-only")
  val agencyOnlyId: DiscountCodeId  = DiscountCodeId.unsafe("dc-agency-only")

  private val baseTime = 1700000000000L // Nov 14, 2023

  // --- Migrated from hardcoded DiscountService ---
  val save5: DiscountCode = DiscountCode(
    id = save5Id, code = "SAVE5",
    discountType = DiscountType.Percentage(BigDecimal(5)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save10: DiscountCode = DiscountCode(
    id = save10Id, code = "SAVE10",
    discountType = DiscountType.Percentage(BigDecimal(10)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save15: DiscountCode = DiscountCode(
    id = save15Id, code = "SAVE15",
    discountType = DiscountType.Percentage(BigDecimal(15)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save20: DiscountCode = DiscountCode(
    id = save20Id, code = "SAVE20",
    discountType = DiscountType.Percentage(BigDecimal(20)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save25: DiscountCode = DiscountCode(
    id = save25Id, code = "SAVE25",
    discountType = DiscountType.Percentage(BigDecimal(25)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save30: DiscountCode = DiscountCode(
    id = save30Id, code = "SAVE30",
    discountType = DiscountType.Percentage(BigDecimal(30)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save40: DiscountCode = DiscountCode(
    id = save40Id, code = "SAVE40",
    discountType = DiscountType.Percentage(BigDecimal(40)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val save50: DiscountCode = DiscountCode(
    id = save50Id, code = "SAVE50",
    discountType = DiscountType.Percentage(BigDecimal(50)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val welcome20: DiscountCode = DiscountCode(
    id = welcome20Id, code = "WELCOME20",
    discountType = DiscountType.Percentage(BigDecimal(20)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val summer10: DiscountCode = DiscountCode(
    id = summer10Id, code = "SUMMER10",
    discountType = DiscountType.Percentage(BigDecimal(10)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  val print15: DiscountCode = DiscountCode(
    id = print15Id, code = "PRINT15",
    discountType = DiscountType.Percentage(BigDecimal(15)),
    constraints = DiscountConstraints(), isActive = true,
    createdBy = None, createdAt = baseTime,
  )

  // --- New rich discount codes ---

  /** Fixed amount discount: 100 CZK off, requires minimum 500 CZK order */
  val fixedOff100: DiscountCode = DiscountCode(
    id = fixedOff100Id, code = "FLAT100",
    discountType = DiscountType.FixedAmount(Money(100)),
    constraints = DiscountConstraints(
      minimumOrderValue = Some(Money(500)),
    ),
    isActive = true, createdBy = Some(EmployeeId.unsafe("emp-1")),
    createdAt = baseTime + 86400000L,
  )

  /** Free delivery code */
  val freeShip: DiscountCode = DiscountCode(
    id = freeShipId, code = "FREESHIP",
    discountType = DiscountType.FreeDelivery,
    constraints = DiscountConstraints(),
    isActive = true, createdBy = Some(EmployeeId.unsafe("emp-1")),
    createdAt = baseTime + 86400000L,
  )

  /** VIP-only code restricted to specific customer IDs */
  val vipOnly: DiscountCode = DiscountCode(
    id = vipOnlyId, code = "VIP25",
    discountType = DiscountType.Percentage(BigDecimal(25)),
    constraints = DiscountConstraints(
      allowedCustomerIds = Set(
        SampleCustomers.printShopProId,
        SampleCustomers.megaCorpId,
      ),
    ),
    isActive = true, createdBy = Some(EmployeeId.unsafe("emp-1")),
    createdAt = baseTime + 172800000L,
  )

  /** Expired code — validUntil in the past */
  val expired: DiscountCode = DiscountCode(
    id = expiredId, code = "OLDCODE",
    discountType = DiscountType.Percentage(BigDecimal(10)),
    constraints = DiscountConstraints(
      validUntil = Some(1690000000000L), // well in the past
    ),
    isActive = true, createdBy = None, createdAt = baseTime - 86400000L * 30,
  )

  /** Future code — validFrom in the future */
  val future: DiscountCode = DiscountCode(
    id = futureId, code = "UPCOMING",
    discountType = DiscountType.Percentage(BigDecimal(15)),
    constraints = DiscountConstraints(
      validFrom = Some(1900000000000L), // far in the future
    ),
    isActive = true, createdBy = None, createdAt = baseTime,
  )

  /** Exhausted code — maxUses reached */
  val exhausted: DiscountCode = DiscountCode(
    id = exhaustedId, code = "USED100",
    discountType = DiscountType.Percentage(BigDecimal(10)),
    constraints = DiscountConstraints(
      maxUses = Some(100),
      currentUses = 100,
    ),
    isActive = true, createdBy = None, createdAt = baseTime,
  )

  /** Inactive code — manually deactivated */
  val inactive: DiscountCode = DiscountCode(
    id = inactiveId, code = "DISABLED",
    discountType = DiscountType.Percentage(BigDecimal(20)),
    constraints = DiscountConstraints(),
    isActive = false, createdBy = None, createdAt = baseTime,
  )

  /** Category-restricted code — only for business cards */
  val cardsOnly: DiscountCode = DiscountCode(
    id = cardsOnlyId, code = "CARDS10",
    discountType = DiscountType.Percentage(BigDecimal(10)),
    constraints = DiscountConstraints(
      allowedCategories = Set(SampleCatalog.businessCardsId),
    ),
    isActive = true, createdBy = Some(EmployeeId.unsafe("emp-1")),
    createdAt = baseTime + 259200000L,
  )

  /** Customer type-restricted code — only for agencies */
  val agencyOnly: DiscountCode = DiscountCode(
    id = agencyOnlyId, code = "AGENCY15",
    discountType = DiscountType.Percentage(BigDecimal(15)),
    constraints = DiscountConstraints(
      allowedCustomerTypes = Set(CustomerType.Agency),
    ),
    isActive = true, createdBy = Some(EmployeeId.unsafe("emp-1")),
    createdAt = baseTime + 345600000L,
  )

  /** All sample discount codes */
  val all: List[DiscountCode] = List(
    save5, save10, save15, save20, save25, save30, save40, save50,
    welcome20, summer10, print15,
    fixedOff100, freeShip, vipOnly,
    expired, future, exhausted, inactive,
    cardsOnly, agencyOnly,
  )
