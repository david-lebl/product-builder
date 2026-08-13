package mpbuilder.domain.pricing

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.ids.*
import zio.json.*

/** Addresses a finish surcharge/fee either at one specific finish product or at
  * a whole finish type; a specific rule always beats a type-level rule (spec §5.4).
  */
enum FinishTarget derives JsonCodec:
  case Specific(finishId: FinishId)
  case OfType(finishType: FinishType)

/** Area tier: applies when the per-item area is at least `minAreaM2`. */
final case class AreaTier(minAreaM2: BigDecimal, pricePerM2: BigDecimal) derives JsonCodec

/** Volume tier: applies when the basis count (sheets or quantity) is at least `minCount`. */
final case class DiscountTier(minCount: Int, multiplier: BigDecimal) derives JsonCodec

/** One independent pricing rule; a pricelist is nothing but a bag of these
  * (spec §5). All amounts are in the pricelist's currency.
  */
enum PricingRule derives JsonCodec:
  // Step 2 — material cost, resolved in this priority order (spec §5.2)
  case MaterialTieredAreaPrice(materialId: MaterialId, tiers: List[AreaTier])
  case MaterialAreaPrice(materialId: MaterialId, pricePerM2: BigDecimal)
  case MaterialSheetPrice(materialId: MaterialId, pricePerSheet: BigDecimal)
  case MaterialUnitPrice(materialId: MaterialId, pricePerUnit: BigDecimal)
  // Step 3 — ink cost, billed in the unit of the component's material mode
  case InkPricePerSheet(methodId: PrintingMethodId, inkConfigId: InkConfigId, pricePerSheet: BigDecimal)
  case InkPricePerM2(methodId: PrintingMethodId, inkConfigId: InkConfigId, pricePerM2: BigDecimal)
  case InkPricePerUnit(methodId: PrintingMethodId, inkConfigId: InkConfigId, pricePerUnit: BigDecimal)
  // Step 4 — finish charges, resolved in this priority order (spec §5.4)
  case CreaseCountPrice(creases: Int, pricePerUnit: BigDecimal)
  case GrommetSpacingPrice(spacingMm: Int, pricePerM2: BigDecimal)
  case LinearLengthPrice(target: FinishTarget, pricePerMetre: BigDecimal)
  case FinishSurcharge(target: FinishTarget, amount: BigDecimal)
  // Step 5 — flat per-item surcharges + per-cut cutting charge
  case ProcessSurcharge(methodId: PrintingMethodId, pricePerUnit: BigDecimal)
  case CategorySurcharge(categoryId: CategoryId, pricePerUnit: BigDecimal)
  case FoldSurcharge(foldType: FoldType, pricePerUnit: BigDecimal)
  case BindingSurcharge(bindingMethod: BindingMethod, pricePerUnit: BigDecimal)
  case CuttingSurcharge(pricePerCut: BigDecimal)
  // Step 7 — volume discounts
  case SheetVolumeDiscount(tiers: List[DiscountTier])
  case QuantityVolumeDiscount(tiers: List[DiscountTier])
  // Step 8 — production speed (static base multipliers in stage 1)
  case SpeedMultiplier(tier: SpeedTier, multiplier: BigDecimal)
  // Step 9 — one-time setup fees
  case FinishSetupFee(target: FinishTarget, fee: BigDecimal)
  case CreasingSetupFee(fee: BigDecimal)
  case FoldSetupFee(foldType: FoldType, fee: BigDecimal)
  case BindingSetupFee(bindingMethod: BindingMethod, fee: BigDecimal)
  // Step 10 — minimum order floor
  case MinimumOrderPrice(amount: BigDecimal)

final case class Pricelist(
  version: String,
  currency: Currency,
  rules: List[PricingRule],
) derives JsonCodec
