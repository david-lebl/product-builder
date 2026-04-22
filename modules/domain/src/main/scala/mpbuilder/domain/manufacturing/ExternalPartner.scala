package mpbuilder.domain.manufacturing

import zio.prelude.*
import mpbuilder.domain.model.*
import java.time.LocalDate

// --- Partner ID ---

opaque type PartnerId = String
object PartnerId:
  def apply(value: String): Validation[String, PartnerId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("PartnerId must not be empty")

  def unsafe(value: String): PartnerId = value

  extension (id: PartnerId) def value: String = id

// --- Partner Availability ---

/** Availability state for an external manufacturing partner. */
final case class PartnerAvailability(
    onVacation: Boolean,
    unavailableUntil: Option[LocalDate],
    blockedDates: Set[LocalDate],
)

object PartnerAvailability:
  val available: PartnerAvailability =
    PartnerAvailability(onVacation = false, unavailableUntil = None, blockedDates = Set.empty)

  extension (pa: PartnerAvailability)
    /** Returns true if the partner is unavailable on the given date. */
    def isUnavailableOn(date: LocalDate): Boolean =
      pa.onVacation ||
        pa.unavailableUntil.exists(u => !date.isAfter(u)) ||
        pa.blockedDates.contains(date)

    def isAvailableOn(date: LocalDate): Boolean = !pa.isUnavailableOn(date)

// --- External Partner ---

/** An external manufacturing partner that handles jobs the shop cannot fulfill in-house. */
final case class ExternalPartner(
    id: PartnerId,
    name: LocalizedString,
    /** Which in-house station types this partner substitutes. */
    capabilities: Set[StationType],
    /** Categories this partner can handle. */
    supportedCategories: Set[CategoryId],
    /** (min, max) business days lead time. */
    leadTimeBusinessDays: (Int, Int),
    /** Price markup multiplier, e.g. 1.15 = 15% above base price. */
    priceMarkup: BigDecimal,
    availability: PartnerAvailability,
    /** Contact info for operator notes (email / phone / URL). */
    contact: String,
)

// --- Partner Tier Policy ---

/** Policy asserting that external partners offer only Standard manufacturing speed. */
object PartnerTierPolicy:
  /** The only tier available when routing to an external partner. */
  val allowedTier: ManufacturingSpeed = ManufacturingSpeed.Standard

  val disabledTiers: Set[ManufacturingSpeed] =
    Set(ManufacturingSpeed.Express, ManufacturingSpeed.Economy)
