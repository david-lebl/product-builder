package mpbuilder.manufacturing

import mpbuilder.shared.*
import kyo.*
import java.time.{DayOfWeek, Instant, LocalTime}

// ── Context-local IDs ────────────────────────────────────────────────────────

opaque type WorkflowId = String
object WorkflowId:
  def apply(raw: String): Validation[String, WorkflowId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("WorkflowId must not be empty")
  def unsafe(raw: String): WorkflowId = raw
  extension (id: WorkflowId) def value: String = id

opaque type StepId = String
object StepId:
  def apply(raw: String): Validation[String, StepId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("StepId must not be empty")
  def unsafe(raw: String): StepId = raw
  extension (id: StepId) def value: String = id

// ── Production Stations & Shop-Floor Resources ───────────────────────────────

enum StationType:
  case Prepress
  case DigitalPrinter
  case OffsetPress
  case LargeFormatPrinter
  case Letterpress
  case ScreenPrinter
  case GarmentPrinter
  case Cutter
  case Laminator
  case UVCoater
  case EmbossingFoil
  case Embroidery
  case Folder
  case Binder
  case LargeFormatFinishing
  case Assembly
  case QualityControl
  case Packaging

enum MachineStatus:
  case Online, Offline, Maintenance

final case class Employee(
    id: EmployeeId,
    name: NonEmptyString,
    stationCapabilities: Set[StationType],
    isActive: Boolean,
)

final case class Machine(
    id: MachineId,
    name: NonEmptyString,
    stationType: StationType,
    status: MachineStatus,
    notes: String = "",
)

// ── Production Spec (Anti-Corruption Input owned by `manufacturing`) ─────────

enum PrintStationKind:
  case Digital, Offset, Letterpress, LargeFormatInkjet, ScreenPrint, DTG, Sublimation

enum FinishingOperation:
  case Lamination
  case UVOrAqueousCoating
  case EmbossingOrFoil
  case Embroidery
  case DieOrContourCut
  case ScoringOrPerforation
  case LargeFormatHardware
  case HardwareAssembly

/** Printing work attached ONLY to printable components (`None` for stands/boxes/hardware!). */
final case class ComponentPrintJob(
    stationKind: PrintStationKind,
    inkNotation: String,
    sheetsToPrint: Int,
    requiresGuillotineCut: Boolean,
)

final case class ProductionComponentSpec(
    key: ComponentKey,
    materialName: String,
    printJob: Option[ComponentPrintJob],
    finishingOperations: List[FinishingOperation] = List.empty,
)

final case class ProductionSpec(
    orderId: OrderId,
    orderLineIndex: Int,
    categoryId: CategoryId,
    productLabel: String,
    quantity: Quantity,
    speed: ManufacturingSpeed,
    components: NonEmptyChunk[ProductionComponentSpec],
    hasFolding: Boolean = false,
    bindingLabel: Option[String] = None,
    /** Customer's optional production instructions for this line, surfaced on the prepress step. */
    customerInstructions: Option[String] = None,
)

// ── Workflow DAG & Fulfilment Models ─────────────────────────────────────────

enum StepStatus:
  case Waiting, Ready, InProgress, Completed, Skipped, Failed

enum WorkflowStatus:
  case Pending, InProgress, Completed, OnHold, Cancelled

enum Priority(val sortWeight: Int):
  case Rush   extends Priority(2)
  case Normal extends Priority(1)
  case Low    extends Priority(0)

object Priority:
  def fromSpeed(speed: ManufacturingSpeed): Priority = speed match
    case ManufacturingSpeed.Express  => Priority.Rush
    case ManufacturingSpeed.Standard => Priority.Normal
    case ManufacturingSpeed.Economy  => Priority.Low

final case class WorkflowStep(
    id: StepId,
    stationType: StationType,
    componentKey: Option[ComponentKey],
    dependsOn: Set[StepId],
    status: StepStatus,
    assignedTo: Option[EmployeeId] = None,
    assignedMachine: Option[MachineId] = None,
    startedAt: Option[Instant] = None,
    completedAt: Option[Instant] = None,
    notes: String = "",
    isRework: Boolean = false,
)

final case class Workflow(
    id: WorkflowId,
    orderId: OrderId,
    orderLineIndex: Int,
    steps: NonEmptyChunk[WorkflowStep],
    status: WorkflowStatus,
    priority: Priority,
    deadline: Option[Instant],
    createdAt: Instant,
):
  def readySteps: List[WorkflowStep] = steps.toList.filter(_.status == StepStatus.Ready)

  def completionRatio: Double =
    val actionable = steps.toList.filterNot(_.status == StepStatus.Skipped)
    if actionable.isEmpty then 1.0
    else actionable.count(_.status == StepStatus.Completed).toDouble / actionable.size

enum PackagingType:
  case Box, Envelope, Roll, Tube, Custom

final case class DispatchRecord(
    trackingNumber: NonEmptyString,
    dispatchedAt: Instant,
    dispatchedBy: EmployeeId,
)

final case class FulfilmentChecklist(
    collectedLineIndices: Set[Int],
    totalLineCount: Int,
    qualitySignedBy: Option[EmployeeId] = None,
    packagingType: Option[PackagingType] = None,
    dispatch: Option[DispatchRecord] = None,
):
  def isReadyToDispatch: Boolean =
    collectedLineIndices.size == totalLineCount && qualitySignedBy.isDefined && packagingType.isDefined
  def isDispatched: Boolean = dispatch.isDefined

final case class ProductionJobView(
    id: JobId,
    orderId: OrderId,
    speed: ManufacturingSpeed,
    priority: Priority,
    workflows: NonEmptyChunk[Workflow],
    fulfilment: FulfilmentChecklist,
    deadline: Option[Instant],
    createdAt: Instant,
)

// ── Capacity, Scheduling & Unified Speed Availability Policy ─────────────────

final case class WorkingHours(
    workingDays: Set[DayOfWeek],
    openTime: LocalTime,
    closeTime: LocalTime,
)

final case class StationUtilisation(
    stationType: StationType,
    activeSteps: Int,
    queuedSteps: Int,
    onlineMachines: Int,
    utilisationRatio: BigDecimal,
)

/** Snapshot exported by `manufacturing` and consumed by `pricing` via `pricing/02-o-acl-manufacturing`. */
final case class CapacitySnapshot(
    globalUtilisation: BigDecimal,
    byStation: Map[StationType, StationUtilisation],
    capturedAt: Instant,
)

/** Unified speed availability policy (replaces legacy fragmented `TierRestriction` + `CategoryTierConfig`). */
final case class SpeedAvailabilityPolicy(
    categoryId: CategoryId,
    speed: ManufacturingSpeed,
    enabled: Boolean = true,
    maxQuantity: Option[Int] = None,
    maxComponents: Option[Int] = None,
    blockedFinishOps: Set[FinishingOperation] = Set.empty,
    blockedMaterialIds: Set[MaterialId] = Set.empty,
    additionalLeadTimeDays: Int = 0,
)

// ── Errors & Events ──────────────────────────────────────────────────────────

enum ManufacturingError(val message: String):
  case JobNotFound(jobId: JobId)
      extends ManufacturingError(s"Production job '${jobId.value}' not found")
  case WorkflowNotFound(workflowId: WorkflowId)
      extends ManufacturingError(s"Workflow '${workflowId.value}' not found")
  case StepNotFound(stepId: StepId)
      extends ManufacturingError(s"Step '${stepId.value}' not found")
  case StepNotReady(stepId: StepId, actualStatus: StepStatus)
      extends ManufacturingError(s"Step '${stepId.value}' is in status $actualStatus, expected Ready")
  case StepNotInProgress(stepId: StepId, actualStatus: StepStatus)
      extends ManufacturingError(s"Step '${stepId.value}' is in status $actualStatus, expected InProgress")
  case EmployeeNotCapable(employeeId: EmployeeId, stationType: StationType)
      extends ManufacturingError(s"Employee '${employeeId.value}' is not qualified for station $stationType")
  case EmployeeInactive(employeeId: EmployeeId)
      extends ManufacturingError(s"Employee '${employeeId.value}' is inactive")
  case MachineUnavailable(machineId: MachineId, status: MachineStatus)
      extends ManufacturingError(s"Machine '${machineId.value}' is unavailable ($status)")
  case MachineStationMismatch(machineId: MachineId, expected: StationType, actual: StationType)
      extends ManufacturingError(s"Machine '${machineId.value}' is for $actual, step requires $expected")
  case SpeedTierUnavailable(categoryId: CategoryId, speed: ManufacturingSpeed, reason: String)
      extends ManufacturingError(s"Speed $speed unavailable for '${categoryId.value}': $reason")
  case NotReadyForDispatch(jobId: JobId)
      extends ManufacturingError(s"Production job '${jobId.value}' is not ready for dispatch")

final case class ProductionJobCreated(
    jobId: JobId,
    orderId: OrderId,
    workflowCount: Int,
    occurredAt: Instant,
) extends DomainEvent

final case class WorkflowStepCompleted(
    jobId: JobId,
    workflowId: WorkflowId,
    stepId: StepId,
    stationType: StationType,
    occurredAt: Instant,
) extends DomainEvent

final case class ProductionJobCompleted(
    jobId: JobId,
    orderId: OrderId,
    occurredAt: Instant,
) extends DomainEvent

final case class OrderDispatched(
    jobId: JobId,
    orderId: OrderId,
    trackingNumber: NonEmptyString,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Port (Module API) ─────────────────────────────────────────

trait ManufacturingService:
  def createJob(jobId: JobId, specs: NonEmptyChunk[ProductionSpec], deadline: Option[Instant] = None): ProductionJobView < (Async & Abort[ManufacturingError])
  def claimStep(jobId: JobId, workflowId: WorkflowId, stepId: StepId, employeeId: EmployeeId, machineId: Option[MachineId]): ProductionJobView < (Async & Abort[ManufacturingError])
  def completeStep(jobId: JobId, workflowId: WorkflowId, stepId: StepId, notes: String = ""): ProductionJobView < (Async & Abort[ManufacturingError])
  def failStepAndScheduleRework(jobId: JobId, workflowId: WorkflowId, stepId: StepId, reason: NonEmptyString): ProductionJobView < (Async & Abort[ManufacturingError])
  def currentCapacity: CapacitySnapshot < Async
  def checkSpeedAvailability(
      categoryId: CategoryId,
      speed: ManufacturingSpeed,
      quantity: Quantity,
      componentCount: Int,
      materialIds: Set[MaterialId],
      finishingOps: Set[FinishingOperation],
  ): Unit < (Async & Abort[ManufacturingError.SpeedTierUnavailable])
