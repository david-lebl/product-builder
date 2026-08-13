package mpbuilder.domain.pricing

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.ids.*
import zio.json.*

enum LineKind derives JsonCodec:
  case Material, Ink, Finish, Cutting, Process, Category, Fold, Binding

/** One itemized charge. `unitDescription` says how the unit price is billed
  * ("per sheet", "per m²", "per unit", "per cut", "per metre").
  */
final case class PriceLine(
  kind: LineKind,
  label: LocalizedText,
  unitDescription: String,
  unitPrice: Money,
  billedQuantity: BigDecimal,
  total: Money,
) derives JsonCodec

/** Itemized charges for one physical component (spec §5.12: broken down
  * separately for each part of a multi-part product).
  */
final case class ComponentBreakdown(
  role: ComponentRole,
  materialId: MaterialId,
  copiesPerSheet: Option[Int],
  sheetsUsed: Option[Int],
  areaM2: Option[BigDecimal],
  lines: List[PriceLine],
  subtotal: Money,
) derives JsonCodec

enum DiscountBasis derives JsonCodec:
  case Sheets, Quantity

final case class AppliedDiscount(
  basis: DiscountBasis,
  basisCount: Int,
  multiplier: BigDecimal,
  amountOff: Money,
) derives JsonCodec

final case class AppliedSpeed(
  tier: SpeedTier,
  multiplier: BigDecimal,
  delta: Money,
) derives JsonCodec

/** One-time setup fee; `key` is the dedup key ("finish:<id>", "creasing",
  * "fold:<fold>", "binding:<binding>") — charged once per distinct key across
  * all components (spec §5.9).
  */
final case class SetupFeeLine(key: String, label: LocalizedText, fee: Money) derives JsonCodec

/** The full itemized result the customer sees (spec §5.12). */
final case class PriceBreakdown(
  currency: Currency,
  quantity: Int,
  components: List[ComponentBreakdown],
  orderLines: List[PriceLine],
  subtotal: Money,
  volumeDiscount: Option[AppliedDiscount],
  discountedSubtotal: Money,
  speedAdjustment: Option[AppliedSpeed],
  afterSpeed: Money,
  setupFees: List[SetupFeeLine],
  minimumApplied: Option[Money],
  total: Money,
) derives JsonCodec:
  def setupFeesTotal: Money =
    setupFees.foldLeft(Money.zero(currency))((acc, f) => acc + f.fee)
