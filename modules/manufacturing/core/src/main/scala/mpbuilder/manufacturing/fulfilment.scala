package mpbuilder.manufacturing

import mpbuilder.kernel.*

/** Packaging type selection for order dispatch */
enum PackagingType:
  case Box, Envelope, Roll, Tube, Custom

object PackagingType:
  extension (pt: PackagingType) def displayName: String = pt match
    case Box      => "Box"
    case Envelope => "Envelope"
    case Roll     => "Roll"
    case Tube     => "Tube"
    case Custom   => "Custom"

/** Fulfilment status for the dispatch workflow */
enum FulfilmentStatus:
  case NotStarted, InProgress, Completed

/** Status of an individual collected item */
final case class CollectedItem(
    itemIndex: Int,
    collected: Boolean,
    verifiedBy: Option[EmployeeId],
)

/** Quality check sign-off record */
final case class QualitySignOff(
    passed: Boolean,
    signedBy: Option[EmployeeId],
    notes: String,
)

object QualitySignOff:
  val empty: QualitySignOff = QualitySignOff(passed = false, signedBy = None, notes = "")

/** Packaging details for shipping */
final case class PackagingInfo(
    packagingType: Option[PackagingType],
    dimensionsCm: Option[String],
    weightKg: Option[String],
)

object PackagingInfo:
  val empty: PackagingInfo = PackagingInfo(None, None, None)

/** Dispatch confirmation record */
final case class DispatchInfo(
    dispatched: Boolean,
    trackingNumber: String,
    dispatchedAt: Option[Long],
    dispatchedBy: Option[EmployeeId],
)

object DispatchInfo:
  val empty: DispatchInfo = DispatchInfo(dispatched = false, "", None, None)

/** Complete fulfilment checklist for an order */
final case class FulfilmentChecklist(
    collectedItems: List[CollectedItem],
    qualitySignOff: QualitySignOff,
    packagingInfo: PackagingInfo,
    dispatchInfo: DispatchInfo,
)

object FulfilmentChecklist:
  def create(itemCount: Int): FulfilmentChecklist =
    val count = Math.max(0, itemCount)
    FulfilmentChecklist(
      collectedItems = (0 until count).map(i => CollectedItem(i, collected = false, verifiedBy = None)).toList,
      qualitySignOff = QualitySignOff.empty,
      packagingInfo = PackagingInfo.empty,
      dispatchInfo = DispatchInfo.empty,
    )

  extension (fc: FulfilmentChecklist)
    def allItemsCollected: Boolean = fc.collectedItems.forall(_.collected)
    def isQualityPassed: Boolean = fc.qualitySignOff.passed
    def isPackaged: Boolean = fc.packagingInfo.packagingType.isDefined
    def isDispatched: Boolean = fc.dispatchInfo.dispatched

    def status: FulfilmentStatus =
      if fc.isDispatched then FulfilmentStatus.Completed
      else if fc.collectedItems.exists(_.collected) || fc.isQualityPassed || fc.isPackaged
      then FulfilmentStatus.InProgress
      else FulfilmentStatus.NotStarted

    def completedStepsCount: Int =
      val s1 = if fc.allItemsCollected then 1 else 0
      val s2 = if fc.isQualityPassed then 1 else 0
      val s3 = if fc.isPackaged then 1 else 0
      val s4 = if fc.isDispatched then 1 else 0
      s1 + s2 + s3 + s4

    def totalStepsCount: Int = 4
