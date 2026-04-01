package mpbuilder.domain.pricing

/** Queue utilisation threshold that triggers an additional pricing multiplier.
  *
  * When globalUtilisation >= minUtilisation, the additionalMultiplier is
  * added to the base manufacturing speed multiplier.
  */
final case class QueueThreshold(
    minUtilisation: BigDecimal,
    additionalMultiplier: BigDecimal,
)
