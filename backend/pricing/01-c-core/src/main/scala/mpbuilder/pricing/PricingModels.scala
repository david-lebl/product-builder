package mpbuilder.pricing

import mpbuilder.shared.*
import zio.NonEmptyChunk
import zio.prelude.Validation
import java.time.Instant

// ── Context-local IDs ────────────────────────────────────────────────────────

opaque type PriceEntryId = String
object PriceEntryId:
  def apply(raw: String): Validation[String, PriceEntryId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("PriceEntryId must not be empty")
  def unsafe(raw: String): PriceEntryId = raw
  extension (id: PriceEntryId) def value: String = id

// ── Orthogonal Pricing Algebra (Target × RateModel × Scope × Phase) ──────────

/** Optional scoping allowing rates and volume curves to specialize by category and/or printing method. */
final case class PricingScope(
    categoryId: Option[CategoryId] = None,
    printingMethodId: Option[PrintingMethodId] = None,
):
  def matches(catId: CategoryId, pmId: Option[PrintingMethodId]): Boolean =
    categoryId.forall(_ == catId) && printingMethodId.forall(p => pmId.contains(p))

  /** Higher specificity wins when multiple rules match: (cat+pm)=3 > pm=2 > cat=1 > global=0. */
  def specificity: Int =
    (if categoryId.isDefined then 1 else 0) + (if printingMethodId.isDefined then 2 else 0)

object PricingScope:
  val global: PricingScope = PricingScope()
  def forCategory(id: CategoryId): PricingScope = PricingScope(categoryId = Some(id))
  def forMethod(id: PrintingMethodId): PricingScope = PricingScope(printingMethodId = Some(id))
  def forCategoryAndMethod(catId: CategoryId, pmId: PrintingMethodId): PricingScope =
    PricingScope(Some(catId), Some(pmId))

enum AreaTierBasis:
  /** Tier threshold compared against a single piece's area. */
  case PerItemArea
  /** Tier threshold compared against the total job area (`itemArea × quantity`). */
  case TotalJobArea

final case class AreaTier(minArea: SquareMeters, ratePerSqM: Money)

/** Orthogonal rate model: HOW a charge scales — completely independent of WHAT is charged.
  *
  * `mapMoney` guarantees that customer agreements and currency adjustments transform
  * every rate variant exhaustively without silent fall-through bugs.
  */
enum RateModel:
  case PerUnit(rate: Money)
  case PerSheet(rate: Money)
  case PerSqMeter(rate: Money, minBillableArea: Option[SquareMeters] = None)
  case TieredArea(tiers: NonEmptyChunk[AreaTier], basis: AreaTierBasis = AreaTierBasis.TotalJobArea)
  case PerLinearMeter(rate: Money)
  case PerPerimeterMeter(rate: Money)
  /** Discrete lookup table keyed by an integer parameter (e.g. crease count, grommet spacing mm). */
  case ByDiscreteParam(rates: Map[Int, Money], scaleByArea: Boolean = false)
  case FlatFee(amount: Money)

  def mapMoney(f: Money => Money): RateModel = this match
    case PerUnit(r)             => PerUnit(f(r))
    case PerSheet(r)            => PerSheet(f(r))
    case PerSqMeter(r, minA)    => PerSqMeter(f(r), minA)
    case TieredArea(ts, b)      => TieredArea(ts.map(t => t.copy(ratePerSqM = f(t.ratePerSqM))), b)
    case PerLinearMeter(r)      => PerLinearMeter(f(r))
    case PerPerimeterMeter(r)   => PerPerimeterMeter(f(r))
    case ByDiscreteParam(m, sa) => ByDiscreteParam(m.view.mapValues(f).toMap, sa)
    case FlatFee(a)             => FlatFee(f(a))

enum ChargePhase:
  /** Variable charges subject to volume curves and manufacturing speed multipliers. */
  case VariableDiscountable
  /** One-time tooling/plate/machine setup fee — added AFTER volume discount and speed multiplier. */
  case OneTimeSetup

/** Canonical ink billing key — distinguishes special inks (White, Metallic) from regular 4/1! */
final case class InkBillingKey(
    frontNotation: String,
    backNotation: String,
):
  def notation: String = s"$frontNotation/$backNotation"

/** WHAT is being priced (independent of HOW it scales). */
enum PricingTarget:
  case MaterialRate(materialId: MaterialId, stockFormatId: Option[StockFormatId] = None)
  case InkRate(printingMethodId: PrintingMethodId, inkKey: InkBillingKey)
  case FinishRate(finishId: FinishId, multiplyBySides: Boolean = false)
  case FinishTypeRate(finishTypeKey: String, multiplyBySides: Boolean = false)
  case PrintingMethodRate(printingMethodId: PrintingMethodId)
  case PrintingProcessRate(processKey: String)
  case FoldRate(foldKey: String)
  case BindingRate(bindingKey: String)
  case CategoryRate(categoryId: CategoryId)
  case GuillotineCutting
  case VersionChange

final case class PriceEntry(
    id: PriceEntryId,
    target: PricingTarget,
    scope: PricingScope = PricingScope.global,
    phase: ChargePhase = ChargePhase.VariableDiscountable,
    rate: RateModel,
)

// ── Scoped Volume Curves, Speed Surge & Tax Policies ─────────────────────────

enum VolumeBasis:
  case FinishedUnits
  case TotalPressSheets
  case TotalAreaSqM

final case class VolumeTier(
    minThreshold: BigDecimal,
    maxThreshold: Option[BigDecimal],
    multiplier: BigDecimal,
):
  def matches(volume: BigDecimal): Boolean =
    volume >= minThreshold && maxThreshold.forall(volume <= _)

final case class VolumeCurve(
    scope: PricingScope,
    basis: VolumeBasis,
    tiers: NonEmptyChunk[VolumeTier],
)

final case class QueueThreshold(
    minUtilisation: BigDecimal,
    additionalMultiplier: BigDecimal,
)

final case class BusyPeriodMultiplier(
    name: String,
    additionalMultiplier: BigDecimal,
)

final case class SpeedSurchargePolicy(
    speed: ManufacturingSpeed,
    baseMultiplier: BigDecimal,
    queueThresholds: List[QueueThreshold] = List.empty,
    subjectToSurge: Boolean = true,
)

final case class TaxPolicy(
    defaultRate: TaxRate,
    categoryOverrides: Map[CategoryId, TaxRate] = Map.empty,
):
  def rateFor(categoryId: CategoryId): TaxRate =
    categoryOverrides.getOrElse(categoryId, defaultRate)

/** Structured Pricelist aggregate.
  *
  * Replaces the flat `List[PricingRule]` bag with explicit sections and
  * deterministic specificity-based resolution.
  */
final case class Pricelist(
    id: PricelistId,
    version: Version,
    currency: Currency,
    entries: List[PriceEntry],
    volumeCurves: List[VolumeCurve],
    speedPolicies: Map[ManufacturingSpeed, SpeedSurchargePolicy],
    minimumOrderNet: Option[Money],
    taxPolicy: TaxPolicy,
)

// ── Pricing Subject (Anti-Corruption Input owned by `pricing`) ───────────────

/** Finish billing input — decoupled from catalog's internal Finish entity. */
final case class PricingFinish(
    finishId: FinishId,
    finishTypeKey: String,
    sideMultiplier: Int = 1,
    discreteParam: Option[Int] = None,
    linearMeters: Option[BigDecimal] = None,
)

/** Component billing input — decoupled from catalog's internal ProductComponent. */
final case class PricingComponent(
    key: ComponentKey,
    materialId: MaterialId,
    stockFormatId: Option[StockFormatId],
    printingMethodId: Option[PrintingMethodId],
    printingProcessKey: Option[String],
    inkKey: Option[InkBillingKey],
    unitsPerProduct: Int,
    totalFlatUnits: Int,
    sheetsUsed: Int,
    guillotineCutsPerSheet: Int,
    areaPerUnitSqM: SquareMeters,
    perimeterPerUnitM: BigDecimal,
    finishes: List[PricingFinish] = List.empty,
)

/** Complete billable subject handed to `PricingEngine` (zero dependency on `catalog`). */
final case class PricingSubject(
    categoryId: CategoryId,
    quantity: Quantity,
    versionCount: VersionCount = VersionCount.single,
    components: NonEmptyChunk[PricingComponent],
    foldKey: Option[String] = None,
    bindingKey: Option[String] = None,
    speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
)

final case class DynamicPricingContext(
    globalUtilisation: BigDecimal = BigDecimal(0),
    busyPeriods: List[BusyPeriodMultiplier] = List.empty,
    expressMultiplierCap: BigDecimal = BigDecimal(2),
)

object DynamicPricingContext:
  val default: DynamicPricingContext = DynamicPricingContext()

// ── Customer Price Agreements (Owned by `pricing`, NOT `customers`!) ─────────

enum AgreementTarget:
  case SpecificCustomer(customerId: CustomerId)
  case TierDefault(tier: CustomerTier)

final case class CustomerPriceAgreement(
    target: AgreementTarget,
    currency: Currency,
    globalDiscount: Option[Percentage] = None,
    categoryDiscounts: Map[CategoryId, Percentage] = Map.empty,
    materialDiscounts: Map[MaterialId, Percentage] = Map.empty,
    fixedMaterialRates: Map[MaterialId, RateModel] = Map.empty,
    finishDiscounts: Map[FinishId, Percentage] = Map.empty,
    customVolumeCurves: Option[List[VolumeCurve]] = None,
    minimumOrderOverride: Option[Money] = None,
)

// ── Promo Codes / Vouchers ───────────────────────────────────────────────────

enum PromoBenefit:
  case PercentageOff(percent: Percentage)
  case FixedAmountOff(amount: Price)
  case FreeDelivery

final case class PromoConstraints(
    validFrom: Option[Instant] = None,
    validUntil: Option[Instant] = None,
    maxUses: Option[Int] = None,
    currentUses: Int = 0,
    minimumOrderNet: Option[Money] = None,
    allowedCategories: Set[CategoryId] = Set.empty,
    allowedSegments: Set[CustomerSegment] = Set.empty,
    allowedCustomerIds: Set[CustomerId] = Set.empty,
)

enum PromoLifecycleStatus:
  case Active, Expired, Exhausted, Inactive

final case class PromoCode(
    id: DiscountCodeId,
    code: NonEmptyString,
    benefit: PromoBenefit,
    constraints: PromoConstraints,
    isActive: Boolean,
    createdAt: Instant = Instant.EPOCH,
):
  def lifecycleStatus(now: Instant): PromoLifecycleStatus =
    if !isActive then PromoLifecycleStatus.Inactive
    else if constraints.validUntil.exists(_.isBefore(now)) then PromoLifecycleStatus.Expired
    else if constraints.maxUses.exists(max => constraints.currentUses >= max) then PromoLifecycleStatus.Exhausted
    else PromoLifecycleStatus.Active

final case class PromoContext(
    now: Instant,
    customerId: Option[CustomerId] = None,
    customerSegment: Option[CustomerSegment] = None,
)

/** Result of evaluating a promo code against an order/cart net subtotal. */
final case class PromoEvaluation(
    codeId: DiscountCodeId,
    code: NonEmptyString,
    benefit: PromoBenefit,
    discountNet: Money,
    freeDelivery: Boolean,
)

/** Effective speed tier multiplier (including live capacity surge) for storefront display. */
final case class SpeedMultiplierSummary(
    speed: ManufacturingSpeed,
    baseMultiplier: BigDecimal,
    effectiveMultiplier: BigDecimal,
    surchargePercent: BigDecimal,
)

// ── Language-Independent Structured Quote Output ─────────────────────────────

/** Typed charge classifier — eliminates localized strings from the pricing engine! */
enum ChargeKind:
  case MaterialCharge(materialId: MaterialId, stockFormatId: Option[StockFormatId])
  case CuttingCharge(cutsPerSheet: Int)
  case InkCharge(printingMethodId: PrintingMethodId, inkKey: InkBillingKey)
  case FinishCharge(finishId: FinishId, finishTypeKey: String)
  case PrintingMethodCharge(printingMethodId: PrintingMethodId)
  case PrintingProcessCharge(processKey: String)
  case CategoryCharge(categoryId: CategoryId)
  case FoldCharge(foldKey: String)
  case BindingCharge(bindingKey: String)
  case VersionChangeCharge(extraVersions: Int)
  case SpeedAdjustment(speed: ManufacturingSpeed, effectiveMultiplier: BigDecimal)
  case SetupFee(target: PricingTarget)
  case MinimumOrderAdjustment(floorNet: Money, calculatedNet: Money)
  case PromoDiscount(codeId: DiscountCodeId, code: String)

final case class ChargeLine(
    kind: ChargeKind,
    componentKey: Option[ComponentKey],
    unitPrice: Money,
    quantity: BigDecimal,
    total: Money,
)

final case class ComponentQuote(
    key: ComponentKey,
    materialLine: ChargeLine,
    cuttingLine: Option[ChargeLine],
    inkLine: Option[ChargeLine],
    finishLines: List[ChargeLine],
    sheetsUsed: Int,
):
  def totalNet: Money =
    val parts = materialLine.total :: cuttingLine.map(_.total).toList ::: inkLine.map(_.total).toList ::: finishLines.map(_.total)
    parts.foldLeft(Money.zero)(_ + _).rounded

final case class TaxSummary(
    rate: TaxRate,
    netAmount: Money,
    taxAmount: Money,
    grossAmount: Money,
)

final case class PriceQuote(
    quoteId: QuoteId,
    pricelistId: PricelistId,
    pricelistVersion: Version,
    currency: Currency,
    quantity: Quantity,
    versionCount: VersionCount,
    componentQuotes: NonEmptyChunk[ComponentQuote],
    orderCharges: List[ChargeLine],
    variableSubtotalNet: Money,
    volumeMultiplier: BigDecimal,
    discountedSubtotalNet: Money,
    speedAdjustment: Option[ChargeLine],
    setupFees: List[ChargeLine],
    minimumAdjustment: Option[ChargeLine],
    promoDiscount: Option[ChargeLine],
    tax: TaxSummary,
    netTotal: Money,
    grossTotal: Money,
    calculatedAt: Instant,
)

// ── Production Cost & Margin Analysis ────────────────────────────────────────

final case class CostEntry(
    target: PricingTarget,
    rate: RateModel,
)

final case class ProductionCostSheet(
    currency: Currency,
    entries: List[CostEntry],
    overheadFactor: BigDecimal = BigDecimal(1),
)

enum MarginWarning:
  case BelowProductionCost(shortfall: Money)
  case LowMargin(actualPercent: Percentage, thresholdPercent: Percentage)

final case class MarginAnalysis(
    productionCostNet: Money,
    sellingPriceNet: Money,
    marginNet: Money,
    marginPercent: BigDecimal,
    isBelowCost: Boolean,
    warnings: List[MarginWarning],
)
