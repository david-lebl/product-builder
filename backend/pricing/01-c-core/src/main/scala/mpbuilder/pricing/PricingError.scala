package mpbuilder.pricing

import mpbuilder.shared.*

/** Typed domain errors for the Pricing bounded context. */
enum PricingError(val message: String):
  case PricelistNotFound(currency: Currency)
      extends PricingError(s"No active pricelist found for currency $currency")
  case MissingMaterialRate(materialId: MaterialId, componentKey: ComponentKey)
      extends PricingError(s"No rate found for material '${materialId.value}' on component '${componentKey.value}'")
  case NoMatchingAreaTier(materialId: MaterialId, areaSqM: SquareMeters)
      extends PricingError(s"No area tier covers ${areaSqM.value} m² for material '${materialId.value}'")
  case MissingDiscreteParam(target: PricingTarget, componentKey: ComponentKey)
      extends PricingError(s"Discrete parameter required to price $target on '${componentKey.value}'")
  case MissingDiscreteRate(target: PricingTarget, paramValue: Int)
      extends PricingError(s"No discrete rate defined for $target with value $paramValue")
  case MissingLinearLength(target: PricingTarget, componentKey: ComponentKey)
      extends PricingError(s"Linear length (meters) required to price $target on '${componentKey.value}'")
  case CurrencyMismatch(expected: Currency, actual: Currency)
      extends PricingError(s"Currency mismatch: expected $expected, got $actual")
  case PromoCodeNotFound(code: String)
      extends PricingError(s"Promo code '$code' not found")
  case PromoCodeInactive(code: String)
      extends PricingError(s"Promo code '$code' is inactive")
  case PromoCodeExpired(code: String)
      extends PricingError(s"Promo code '$code' is outside its validity window")
  case PromoCodeUsageLimitReached(code: String)
      extends PricingError(s"Promo code '$code' has reached its usage limit")
  case PromoCodeNotEligible(code: String, reason: String)
      extends PricingError(s"Promo code '$code' is not eligible: $reason")
  case PricingValidationFailed(errors: zio.NonEmptyChunk[PricingError])
      extends PricingError(s"Pricing failed with ${errors.size} error(s): ${errors.map(_.message).mkString("; ")}")
