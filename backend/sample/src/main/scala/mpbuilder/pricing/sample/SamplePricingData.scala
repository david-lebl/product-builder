package mpbuilder.pricing.sample

import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.pricing.*
import mpbuilder.shared.*
import java.time.Instant

/** Sample B2B customer price agreements (properly owned by `pricing`, NOT `customers`),
  * replicated from legacy `SampleCustomers` pricing blocks.
  */
object SampleAgreements:

  import SampleCatalog.*

  // Shared cross-context Customer IDs (identical values to `customers/sample/SampleCustomers`)
  val printShopProId: CustomerId    = CustomerId.unsafe("cust-print-shop-pro")
  val graphicDesignCoId: CustomerId = CustomerId.unsafe("cust-graphic-design-co")
  val megaCorpId: CustomerId        = CustomerId.unsafe("cust-mega-corp")
  val eventAgencyId: CustomerId     = CustomerId.unsafe("cust-event-agency")
  val adStudioId: CustomerId        = CustomerId.unsafe("cust-ad-studio")
  val startupHubId: CustomerId      = CustomerId.unsafe("cust-startup-hub")
  val packagingLtdId: CustomerId    = CustomerId.unsafe("cust-packaging-ltd")

  private val megaCorpVolumeCurve: VolumeCurve = VolumeCurve(
    scope = PricingScope.global,
    basis = VolumeBasis.FinishedUnits,
    tiers = NonEmptyChunk(
      VolumeTier(BigDecimal(1), Some(BigDecimal(99)), BigDecimal("1.00")),
      VolumeTier(BigDecimal(100), Some(BigDecimal(499)), BigDecimal("0.85")),
      VolumeTier(BigDecimal(500), Some(BigDecimal(1999)), BigDecimal("0.70")),
      VolumeTier(BigDecimal(2000), None, BigDecimal("0.60")),
    ),
  )

  private def forBothCurrencies(build: Currency => CustomerPriceAgreement): List[CustomerPriceAgreement] =
    List(build(Currency.USD), build(Currency.CZK))

  val all: List[CustomerPriceAgreement] =
    // Specific customer agreements
    forBothCurrencies(c =>
      CustomerPriceAgreement(
        target = AgreementTarget.SpecificCustomer(printShopProId),
        currency = c,
        globalDiscount = Some(Percentage.unsafe(BigDecimal(10))),
        materialDiscounts = Map(coated300gsmId -> Percentage.unsafe(BigDecimal(15))),
      )
    ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(graphicDesignCoId),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(5))),
          finishDiscounts = Map(matteLaminationId -> Percentage.unsafe(BigDecimal(20))),
        )
      ) ++
      List(
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(megaCorpId),
          currency = Currency.USD,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(15))),
          categoryDiscounts = Map(
            businessCardsId -> Percentage.unsafe(BigDecimal(20)),
            flyersId        -> Percentage.unsafe(BigDecimal(18)),
          ),
          fixedMaterialRates = Map(coated300gsmId -> RateModel.PerUnit(Money("0.06"))),
          customVolumeCurves = Some(List(megaCorpVolumeCurve)),
        ),
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(megaCorpId),
          currency = Currency.CZK,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(15))),
          categoryDiscounts = Map(
            businessCardsId -> Percentage.unsafe(BigDecimal(20)),
            flyersId        -> Percentage.unsafe(BigDecimal(18)),
          ),
          fixedMaterialRates = Map(coated300gsmId -> RateModel.PerSheet(Money("10"))),
          customVolumeCurves = Some(List(megaCorpVolumeCurve)),
        ),
      ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(eventAgencyId),
          currency = c,
          categoryDiscounts = Map(bannersId -> Percentage.unsafe(BigDecimal(12))),
          materialDiscounts = Map(vinylId -> Percentage.unsafe(BigDecimal(10))),
        )
      ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(adStudioId),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(8))),
          finishDiscounts = Map(
            foilStampingId -> Percentage.unsafe(BigDecimal(15)),
            embossingId    -> Percentage.unsafe(BigDecimal(10)),
          ),
        )
      ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(startupHubId),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(3))),
        )
      ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.SpecificCustomer(packagingLtdId),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(5))),
          categoryDiscounts = Map(packagingId -> Percentage.unsafe(BigDecimal(10))),
        )
      ) ++
      // Tier defaults (fallback when customer has no custom agreement)
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.TierDefault(CustomerTier.Silver),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(5))),
        )
      ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.TierDefault(CustomerTier.Gold),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(10))),
        )
      ) ++
      forBothCurrencies(c =>
        CustomerPriceAgreement(
          target = AgreementTarget.TierDefault(CustomerTier.Platinum),
          currency = c,
          globalDiscount = Some(Percentage.unsafe(BigDecimal(15))),
        )
      )

/** All 20 promo / discount codes replicated from legacy `SampleDiscountCodes`. */
object SamplePromoCodes:

  import SampleCatalog.*

  private def pctCode(
      id: String,
      code: String,
      pct: Int,
      constraints: PromoConstraints = PromoConstraints(),
      active: Boolean = true,
      createdAtMs: Long = 1715000000000L,
  ): PromoCode =
    PromoCode(
      id = DiscountCodeId.unsafe(id),
      code = NonEmptyString.unsafe(code),
      benefit = PromoBenefit.PercentageOff(Percentage.unsafe(BigDecimal(pct))),
      constraints = constraints,
      isActive = active,
      createdAt = Instant.ofEpochMilli(createdAtMs),
    )

  val save5: PromoCode     = pctCode("dc-save5", "SAVE5", 5, createdAtMs = 1710000000000L)
  val save10: PromoCode    = pctCode("dc-save10", "SAVE10", 10, createdAtMs = 1718000000000L)
  val save15: PromoCode    = pctCode("dc-save15", "SAVE15", 15, createdAtMs = 1711000000000L)
  val save20: PromoCode    = pctCode("dc-save20", "SAVE20", 20, createdAtMs = 1712000000000L)
  val save25: PromoCode    = pctCode("dc-save25", "SAVE25", 25, createdAtMs = 1713000000000L)
  val save30: PromoCode    = pctCode("dc-save30", "SAVE30", 30, createdAtMs = 1714000000000L)
  val save40: PromoCode    = pctCode("dc-save40", "SAVE40", 40, createdAtMs = 1714500000000L)
  val save50: PromoCode    = pctCode("dc-save50", "SAVE50", 50, createdAtMs = 1714800000000L)
  val welcome20: PromoCode = pctCode("dc-welcome20", "WELCOME20", 20, createdAtMs = 1717000000000L)
  val summer10: PromoCode  = pctCode("dc-summer10", "SUMMER10", 10, createdAtMs = 1716000000000L)
  val print15: PromoCode   = pctCode("dc-print15", "PRINT15", 15, createdAtMs = 1716500000000L)

  val fixedOff100: PromoCode = PromoCode(
    id = DiscountCodeId.unsafe("dc-fixed-off-100"),
    code = NonEmptyString.unsafe("FLAT100"),
    benefit = PromoBenefit.FixedAmountOff(Price(Money("100"), Currency.CZK)),
    constraints = PromoConstraints(minimumOrderNet = Some(Money("500"))),
    isActive = true,
    createdAt = Instant.ofEpochMilli(1719000000000L),
  )

  val freeShip: PromoCode = PromoCode(
    id = DiscountCodeId.unsafe("dc-free-ship"),
    code = NonEmptyString.unsafe("FREESHIP"),
    benefit = PromoBenefit.FreeDelivery,
    constraints = PromoConstraints(),
    isActive = true,
    createdAt = Instant.ofEpochMilli(1720000000000L),
  )

  val vipOnly: PromoCode = pctCode(
    id = "dc-vip-only",
    code = "VIP25",
    pct = 25,
    constraints = PromoConstraints(
      allowedCustomerIds = Set(SampleAgreements.printShopProId, SampleAgreements.megaCorpId)
    ),
    createdAtMs = 1719500000000L,
  )

  val expired: PromoCode = pctCode(
    id = "dc-expired",
    code = "OLDCODE",
    pct = 10,
    constraints = PromoConstraints(validUntil = Some(Instant.ofEpochMilli(1690000000000L))),
  )

  val future: PromoCode = pctCode(
    id = "dc-future",
    code = "UPCOMING",
    pct = 15,
    constraints = PromoConstraints(validFrom = Some(Instant.parse("2099-01-01T00:00:00Z"))),
  )

  val exhausted: PromoCode = pctCode(
    id = "dc-exhausted",
    code = "USED100",
    pct = 10,
    constraints = PromoConstraints(maxUses = Some(100), currentUses = 100),
  )

  val inactive: PromoCode = pctCode(
    id = "dc-inactive",
    code = "DISABLED",
    pct = 20,
    active = false,
  )

  val cardsOnly: PromoCode = pctCode(
    id = "dc-cards-only",
    code = "CARDS10",
    pct = 10,
    constraints = PromoConstraints(allowedCategories = Set(businessCardsId)),
  )

  val agencyOnly: PromoCode = pctCode(
    id = "dc-agency-only",
    code = "AGENCY15",
    pct = 15,
    constraints = PromoConstraints(allowedSegments = Set(CustomerSegment.Agency)),
  )

  val singleUse: PromoCode = pctCode(
    id = "dc-single-use",
    code = "ONEUSE",
    pct = 10,
    constraints = PromoConstraints(maxUses = Some(1), currentUses = 0),
  )

  val all: List[PromoCode] = List(
    save5, save10, save15, save20, save25, save30, save40, save50,
    welcome20, summer10, print15,
    fixedOff100, freeShip, vipOnly,
    expired, future, exhausted, inactive,
    cardsOnly, agencyOnly, singleUse,
  )

/** Sample production cost sheets (USD & CZK) replicated from legacy `SampleProductionCosts`. */
object SampleCostSheets:

  import SampleCatalog.*

  val costSheetUsd: ProductionCostSheet = ProductionCostSheet(
    currency = Currency.USD,
    overheadFactor = BigDecimal("1.15"),
    entries = List(
      CostEntry(PricingTarget.MaterialRate(coated300gsmId), RateModel.PerUnit(Money("0.04"))),
      CostEntry(PricingTarget.MaterialRate(coatedMatte350gsmId), RateModel.PerUnit(Money("0.05"))),
      CostEntry(PricingTarget.MaterialRate(uncoatedBondId), RateModel.PerUnit(Money("0.01"))),
      CostEntry(PricingTarget.MaterialRate(kraftId), RateModel.PerUnit(Money("0.03"))),
      CostEntry(PricingTarget.MaterialRate(corrugatedId), RateModel.PerUnit(Money("0.12"))),
      CostEntry(PricingTarget.MaterialRate(coatedSilk250gsmId), RateModel.PerUnit(Money("0.04"))),
      CostEntry(PricingTarget.MaterialRate(yupoId), RateModel.PerUnit(Money("0.08"))),
      CostEntry(PricingTarget.MaterialRate(adhesiveStockId), RateModel.PerUnit(Money("0.05"))),
      CostEntry(PricingTarget.MaterialRate(cottonId), RateModel.PerUnit(Money("0.10"))),
      CostEntry(PricingTarget.MaterialRate(vinylId), RateModel.PerSqMeter(Money("8.00"))),
      CostEntry(PricingTarget.MaterialRate(clearVinylId), RateModel.PerSqMeter(Money("10.00"))),
      CostEntry(PricingTarget.MaterialRate(pvc510gId), RateModel.PerSqMeter(Money("7.50"))),
      CostEntry(PricingTarget.MaterialRate(rollUpBannerFilmId), RateModel.PerSqMeter(Money("5.00"))),
      CostEntry(PricingTarget.MaterialRate(rollUpStandEconomyId), RateModel.PerUnit(Money("14.00"))),
      CostEntry(PricingTarget.MaterialRate(rollUpStandPremiumId), RateModel.PerUnit(Money("30.00"))),
      CostEntry(PricingTarget.FinishRate(matteLaminationId, true), RateModel.PerUnit(Money("0.01"))),
      CostEntry(PricingTarget.FinishRate(glossLaminationId, true), RateModel.PerUnit(Money("0.01"))),
      CostEntry(PricingTarget.FinishRate(softTouchCoatingId, true), RateModel.PerUnit(Money("0.02"))),
      CostEntry(PricingTarget.FinishRate(embossingId), RateModel.PerUnit(Money("0.04"))),
      CostEntry(PricingTarget.FinishRate(foilStampingId), RateModel.PerUnit(Money("0.07"))),
      CostEntry(PricingTarget.FinishRate(dieCutId), RateModel.PerUnit(Money("0.05"))),
    ),
  )

  val costSheetCzk: ProductionCostSheet = ProductionCostSheet(
    currency = Currency.CZK,
    overheadFactor = BigDecimal("1.15"),
    entries = List(
      CostEntry(PricingTarget.MaterialRate(coated300gsmId), RateModel.PerSheet(Money("6.00"))),
      CostEntry(PricingTarget.MaterialRate(coatedMatte350gsmId), RateModel.PerSheet(Money("7.00"))),
      CostEntry(PricingTarget.MaterialRate(coatedGlossy130gsmId), RateModel.PerSheet(Money("2.50"))),
      CostEntry(PricingTarget.MaterialRate(uncoatedBondId), RateModel.PerSheet(Money("1.80"))),
      CostEntry(PricingTarget.MaterialRate(kraftId), RateModel.PerSheet(Money("5.00"))),
      CostEntry(PricingTarget.MaterialRate(corrugatedId), RateModel.PerUnit(Money("1.10"))),
      CostEntry(PricingTarget.MaterialRate(coatedSilk250gsmId), RateModel.PerSheet(Money("5.00"))),
      CostEntry(PricingTarget.MaterialRate(vinylId), RateModel.PerSqMeter(Money("180.00"))),
      CostEntry(PricingTarget.MaterialRate(clearVinylId), RateModel.PerSqMeter(Money("225.00"))),
      CostEntry(PricingTarget.MaterialRate(pvc510gId), RateModel.PerSqMeter(Money("170.00"))),
      CostEntry(PricingTarget.MaterialRate(rollUpBannerFilmId), RateModel.PerSqMeter(Money("110.00"))),
      CostEntry(PricingTarget.MaterialRate(rollUpStandEconomyId), RateModel.PerUnit(Money("320.00"))),
      CostEntry(PricingTarget.MaterialRate(rollUpStandPremiumId), RateModel.PerUnit(Money("700.00"))),
      CostEntry(PricingTarget.FinishRate(matteLaminationId, true), RateModel.PerSheet(Money("2.50"))),
      CostEntry(PricingTarget.FinishRate(glossLaminationId, true), RateModel.PerSheet(Money("2.50"))),
      CostEntry(PricingTarget.FinishRate(embossingId), RateModel.PerUnit(Money("1.00"))),
      CostEntry(PricingTarget.FinishRate(foilStampingId), RateModel.PerUnit(Money("1.60"))),
    ),
  )
