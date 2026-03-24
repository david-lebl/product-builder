package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*

/** Validates whether a manufacturing speed tier is feasible for a given configuration.
  *
  * Returns accumulated validation errors for all violated constraints.
  */
object ManufacturingSpeedValidator:

  enum SpeedValidationError:
    case QuantityExceedsLimit(speed: ManufacturingSpeed, maxQuantity: Int, requestedQuantity: Int)
    case TooManyComponents(speed: ManufacturingSpeed, maxComponents: Int, actualComponents: Int)
    case TooManyFinishes(speed: ManufacturingSpeed, maxFinishes: Int, actualFinishes: Int)

    def message: String = message(Language.En)

    def message(lang: Language): String = this match
      case QuantityExceedsLimit(speed, max, requested) => lang match
        case Language.En => s"${speed.displayName(Language.En)} is not available for quantities over $max (requested: $requested)"
        case Language.Cs => s"${speed.displayName(Language.Cs)} není dostupný pro množství nad $max (požadováno: $requested)"
      case TooManyComponents(speed, max, actual) => lang match
        case Language.En => s"${speed.displayName(Language.En)} is limited to $max components (configuration has $actual)"
        case Language.Cs => s"${speed.displayName(Language.En)} je omezen na $max komponent (konfigurace má $actual)"
      case TooManyFinishes(speed, max, actual) => lang match
        case Language.En => s"${speed.displayName(Language.En)} is limited to $max finishes (configuration has $actual)"
        case Language.Cs => s"${speed.displayName(Language.En)} je omezen na $max dokončení (konfigurace má $actual)"

  /** Validate that the given speed tier is feasible for the configuration.
    *
    * @param config product configuration to validate
    * @param speed  desired manufacturing speed tier
    * @param restrictions tier restrictions for this category (if any)
    * @return Validation with accumulated errors, or the validated speed
    */
  def validate(
      config: ProductConfiguration,
      speed: ManufacturingSpeed,
      restrictions: List[TierRestriction],
  ): Validation[SpeedValidationError, ManufacturingSpeed] =
    val applicable = restrictions.filter(r => r.categoryId == config.category.id && r.tier == speed)
    if applicable.isEmpty then Validation.succeed(speed)
    else
      val quantity = config.specifications.get(SpecKind.Quantity).collect {
        case SpecValue.QuantitySpec(q) => q.value
      }.getOrElse(1)

      val componentCount = config.components.size
      val finishCount = config.components.flatMap(_.finishes).size

      val errors = applicable.flatMap { restriction =>
        val qtyError = restriction.maxQuantity.flatMap { max =>
          if quantity > max then Some(SpeedValidationError.QuantityExceedsLimit(speed, max, quantity))
          else None
        }
        val compError = restriction.maxComponents.flatMap { max =>
          if componentCount > max then Some(SpeedValidationError.TooManyComponents(speed, max, componentCount))
          else None
        }
        val finishError = restriction.maxFinishes.flatMap { max =>
          if finishCount > max then Some(SpeedValidationError.TooManyFinishes(speed, max, finishCount))
          else None
        }
        qtyError.toList ++ compError.toList ++ finishError.toList
      }

      if errors.isEmpty then Validation.succeed(speed)
      else
        errors.tail.foldLeft(Validation.fail(errors.head): Validation[SpeedValidationError, ManufacturingSpeed]) {
          (acc, err) => acc.zipWith(Validation.fail(err))((_, _) => speed)
        }

  /** Check which speed tiers are available for a given configuration. */
  def availableTiers(
      config: ProductConfiguration,
      restrictions: List[TierRestriction],
  ): List[ManufacturingSpeed] =
    ManufacturingSpeed.values.toList.filter { speed =>
      validate(config, speed, restrictions).toEither.isRight
    }
