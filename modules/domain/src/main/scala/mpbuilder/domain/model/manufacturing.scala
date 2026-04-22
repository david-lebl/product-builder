package mpbuilder.domain.model

import zio.prelude.*
import mpbuilder.domain.manufacturing.PartnerId

// --- Manufacturing IDs ---

opaque type WorkflowId = String
object WorkflowId:
  def apply(value: String): Validation[String, WorkflowId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("WorkflowId must not be empty")

  def unsafe(value: String): WorkflowId = value

  extension (id: WorkflowId) def value: String = id

opaque type StepId = String
object StepId:
  def apply(value: String): Validation[String, StepId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("StepId must not be empty")

  def unsafe(value: String): StepId = value

  extension (id: StepId) def value: String = id

opaque type EmployeeId = String
object EmployeeId:
  def apply(value: String): Validation[String, EmployeeId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("EmployeeId must not be empty")

  def unsafe(value: String): EmployeeId = value

  extension (id: EmployeeId) def value: String = id

opaque type MachineId = String
object MachineId:
  def apply(value: String): Validation[String, MachineId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("MachineId must not be empty")

  def unsafe(value: String): MachineId = value

  extension (id: MachineId) def value: String = id

// --- Manufacturing Enums ---

/** Production station types derived from domain processing steps */
enum StationType:
  case Prepress
  case DigitalPrinter
  case OffsetPress
  case LargeFormatPrinter
  case Letterpress
  case Cutter
  case Laminator
  case UVCoater
  case EmbossingFoil
  case Folder
  case Binder
  case LargeFormatFinishing
  case QualityControl
  case Packaging
  case ExternalPartner  // represents hand-off to an external manufacturing partner

object StationType:
  extension (st: StationType) def displayName: String = st match
    case Prepress            => "Prepress"
    case DigitalPrinter      => "Digital Printer"
    case OffsetPress         => "Offset Press"
    case LargeFormatPrinter  => "Large Format Printer"
    case Letterpress         => "Letterpress"
    case Cutter              => "Cutter"
    case Laminator           => "Laminator"
    case UVCoater            => "UV Coater"
    case EmbossingFoil       => "Embossing / Foil"
    case Folder              => "Folder"
    case Binder              => "Binder"
    case LargeFormatFinishing => "Large Format Finishing"
    case QualityControl      => "Quality Control"
    case Packaging           => "Packaging & Dispatch"
    case ExternalPartner     => "External Partner"

  extension (st: StationType) def icon: String = st match
    case Prepress            => "📋"
    case DigitalPrinter      => "🖨️"
    case OffsetPress         => "🏭"
    case LargeFormatPrinter  => "🖼️"
    case Letterpress         => "🔤"
    case Cutter              => "✂️"
    case Laminator           => "🔲"
    case UVCoater            => "✨"
    case EmbossingFoil       => "💎"
    case Folder              => "📐"
    case Binder              => "📚"
    case LargeFormatFinishing => "🔧"
    case QualityControl      => "✅"
    case Packaging           => "📦"
    case ExternalPartner     => "🏭"

/** Status of an individual workflow step */
enum StepStatus:
  case Waiting     // dependencies not yet met
  case Ready       // all dependencies met, awaiting pickup
  case InProgress  // employee has claimed the step
  case Completed   // step finished successfully
  case Skipped     // step not needed (e.g., no cutting required)
  case Failed      // step failed, workflow on hold

/** Aggregate status of a manufacturing workflow */
enum WorkflowStatus:
  case Pending     // created but not started
  case InProgress  // at least one step is InProgress or Completed
  case Completed   // all steps Completed or Skipped
  case OnHold      // a step Failed, awaiting resolution
  case Cancelled   // workflow cancelled

/** Order priority flag */
enum Priority:
  case Rush, Normal, Low

object Priority:
  extension (p: Priority) def displayName: String = p match
    case Rush   => "Rush"
    case Normal => "Normal"
    case Low    => "Low"

  extension (p: Priority) def sortWeight: Int = p match
    case Rush   => 2
    case Normal => 1
    case Low    => 0

/** Customer-facing manufacturing speed tier */
enum ManufacturingSpeed:
  case Express, Standard, Economy

object ManufacturingSpeed:
  extension (s: ManufacturingSpeed) def toPriority: Priority = s match
    case Express  => Priority.Rush
    case Standard => Priority.Normal
    case Economy  => Priority.Low

  extension (s: ManufacturingSpeed) def displayName(lang: Language): String = s match
    case Express  => lang match { case Language.En => "Express"; case Language.Cs => "Expres" }
    case Standard => lang match { case Language.En => "Standard"; case Language.Cs => "Standardní" }
    case Economy  => lang match { case Language.En => "Economy"; case Language.Cs => "Ekonomická" }

  extension (s: ManufacturingSpeed) def icon: String = s match
    case Express  => "⚡"
    case Standard => "●"
    case Economy  => "🐢"

/** Status of an order in the approval queue */
enum ApprovalStatus:
  case Placed
  case Approved
  case Rejected
  case PendingChanges
  case OnHold

/** Payment verification status */
enum PaymentStatus:
  case Pending, Confirmed, Failed

object PaymentStatus:
  extension (ps: PaymentStatus) def displayName: String = ps match
    case Pending   => "Pending"
    case Confirmed => "Confirmed"
    case Failed    => "Failed"

  extension (ps: PaymentStatus) def icon: String = ps match
    case Pending   => "⏳"
    case Confirmed => "✅"
    case Failed    => "❌"

/** Artwork check flag status */
enum CheckStatus:
  case NotChecked, Passed, Warning, Failed

object CheckStatus:
  extension (cs: CheckStatus) def displayName: String = cs match
    case NotChecked => "Not Checked"
    case Passed     => "Passed"
    case Warning    => "Warning"
    case Failed     => "Failed"

  extension (cs: CheckStatus) def icon: String = cs match
    case NotChecked => "⬜"
    case Passed     => "✅"
    case Warning    => "⚠️"
    case Failed     => "❌"

/** Artwork review check for prepress file validation */
final case class ArtworkCheck(
    resolution: CheckStatus,
    bleed: CheckStatus,
    colorProfile: CheckStatus,
    notes: String,
)

object ArtworkCheck:
  val unchecked: ArtworkCheck = ArtworkCheck(CheckStatus.NotChecked, CheckStatus.NotChecked, CheckStatus.NotChecked, "")

  extension (ac: ArtworkCheck)
    def isFullyPassed: Boolean =
      ac.resolution == CheckStatus.Passed && ac.bleed == CheckStatus.Passed && ac.colorProfile == CheckStatus.Passed

    def hasIssues: Boolean =
      ac.resolution == CheckStatus.Failed || ac.bleed == CheckStatus.Failed || ac.colorProfile == CheckStatus.Failed

    def hasWarnings: Boolean =
      !ac.hasIssues && (ac.resolution == CheckStatus.Warning || ac.bleed == CheckStatus.Warning || ac.colorProfile == CheckStatus.Warning)

// --- Fulfilment Types ---

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

// --- Manufacturing Data Types ---

/** A single step in a manufacturing workflow */
final case class WorkflowStep(
    id: StepId,
    stationType: StationType,
    componentRole: Option[ComponentRole],
    dependsOn: Set[StepId],
    status: StepStatus,
    assignedTo: Option[EmployeeId],
    assignedMachine: Option[MachineId],
    startedAt: Option[Long],
    completedAt: Option[Long],
    notes: String,
    isRework: Boolean = false,
    assignedPartner: Option[PartnerId] = None,
    estimatedCompletionOverride: Option[java.time.Instant] = None,
)

/** A manufacturing workflow for a single order item */
final case class ManufacturingWorkflow(
    id: WorkflowId,
    orderId: OrderId,
    orderItemIndex: Int,
    steps: List[WorkflowStep],
    status: WorkflowStatus,
    priority: Priority,
    deadline: Option[Long],
    createdAt: Long,
)

object ManufacturingWorkflow:
  extension (wf: ManufacturingWorkflow)
    /** Steps that are ready for pickup */
    def readySteps: List[WorkflowStep] =
      wf.steps.filter(_.status == StepStatus.Ready)

    /** Steps currently being worked on */
    def inProgressSteps: List[WorkflowStep] =
      wf.steps.filter(_.status == StepStatus.InProgress)

    /** Steps that have been completed */
    def completedSteps: List[WorkflowStep] =
      wf.steps.filter(_.status == StepStatus.Completed)

    /** Fraction of completed steps (0.0 to 1.0) */
    def completionRatio: Double =
      val actionable = wf.steps.filterNot(_.status == StepStatus.Skipped)
      if actionable.isEmpty then 1.0
      else actionable.count(_.status == StepStatus.Completed).toDouble / actionable.size

    /** Re-evaluate step readiness based on current state */
    def evaluateReadiness: ManufacturingWorkflow =
      val completedIds = wf.steps.filter(s =>
        s.status == StepStatus.Completed || s.status == StepStatus.Skipped
      ).map(_.id).toSet

      val updatedSteps = wf.steps.map { step =>
        if step.status == StepStatus.Waiting && step.dependsOn.subsetOf(completedIds) then
          step.copy(status = StepStatus.Ready)
        else step
      }
      wf.copy(steps = updatedSteps)

/** A manufacturing order combining order info with workflows */
final case class ManufacturingOrder(
    order: Order,
    workflows: List[ManufacturingWorkflow],
    approvalStatus: ApprovalStatus,
    approvalNotes: String,
    createdAt: Long,
    deadline: Option[Long],
    priority: Priority = Priority.Normal,
    paymentStatus: PaymentStatus = PaymentStatus.Pending,
    artworkCheck: ArtworkCheck = ArtworkCheck.unchecked,
    fulfilment: Option[FulfilmentChecklist] = None,
)

object ManufacturingOrder:
  extension (mo: ManufacturingOrder)
    def overallStatus: WorkflowStatus =
      if mo.workflows.exists(_.status == WorkflowStatus.OnHold) then WorkflowStatus.OnHold
      else if mo.workflows.forall(_.status == WorkflowStatus.Completed) then WorkflowStatus.Completed
      else if mo.workflows.exists(w =>
        w.status == WorkflowStatus.InProgress || w.status == WorkflowStatus.Completed
      ) then WorkflowStatus.InProgress
      else if mo.workflows.forall(_.status == WorkflowStatus.Cancelled) then WorkflowStatus.Cancelled
      else WorkflowStatus.Pending

    def overallCompletionRatio: Double =
      if mo.workflows.isEmpty then 1.0
      else mo.workflows.map(_.completionRatio).sum / mo.workflows.size

    def totalSteps: Int = mo.workflows.flatMap(_.steps).size

    def completedStepCount: Int =
      mo.workflows.flatMap(_.steps).count(s =>
        s.status == StepStatus.Completed || s.status == StepStatus.Skipped
      )

    def customerName: String =
      val info = mo.order.checkoutInfo.contactInfo
      s"${info.firstName} ${info.lastName}".trim match
        case "" => "(Guest)"
        case n  => n

    def itemSummary: String =
      val items = mo.order.basket.items
      if items.isEmpty then "No items"
      else if items.size == 1 then
        s"${items.head.quantity}× ${items.head.configuration.category.name(Language.En)}"
      else
        val first = s"${items.head.quantity}× ${items.head.configuration.category.name(Language.En)}"
        s"${items.size} items: $first…"

    def isReadyForDispatch: Boolean =
      mo.workflows.nonEmpty && mo.workflows.forall(_.status == WorkflowStatus.Completed)

    def isDispatched: Boolean =
      mo.fulfilment.exists(_.isDispatched)

/** An employee in the manufacturing system */
final case class Employee(
    id: EmployeeId,
    name: String,
    stationCapabilities: Set[StationType],
    isActive: Boolean,
)

/** Machine status */
enum MachineStatus:
  case Online, Offline, Maintenance

object MachineStatus:
  extension (ms: MachineStatus) def displayName: String = ms match
    case Online      => "Online"
    case Offline     => "Offline"
    case Maintenance => "Maintenance"

  extension (ms: MachineStatus) def icon: String = ms match
    case Online      => "🟢"
    case Offline     => "🔴"
    case Maintenance => "🟡"

/** A registered machine */
final case class Machine(
    id: MachineId,
    name: String,
    stationType: StationType,
    status: MachineStatus,
    currentNotes: String,
)
