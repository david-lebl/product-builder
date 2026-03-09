package mpbuilder.domain.model

import zio.prelude.*

// ─── Manufacturing IDs ──────────────────────────────────────────────

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

// ─── Manufacturing Enums ────────────────────────────────────────────

/** Manufacturing stations — derived from product configuration processing steps */
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

object StationType:
  extension (st: StationType) def label: String = st match
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
    case Packaging           => "Packaging"

  extension (st: StationType) def icon: String = st match
    case Prepress            => "📋"
    case DigitalPrinter      => "🖨️"
    case OffsetPress         => "🏭"
    case LargeFormatPrinter  => "🖼️"
    case Letterpress         => "🔤"
    case Cutter              => "✂️"
    case Laminator           => "🔲"
    case UVCoater            => "✨"
    case EmbossingFoil       => "⬛"
    case Folder              => "📁"
    case Binder              => "📚"
    case LargeFormatFinishing => "🛠️"
    case QualityControl      => "🔍"
    case Packaging           => "📦"

/** Status of an individual workflow step */
enum StepStatus:
  case Waiting     // Dependencies not yet met
  case Ready       // All dependencies met, awaiting pickup
  case InProgress  // An employee is working on this step
  case Completed   // Step is done
  case Skipped     // Step was skipped (not needed)
  case Failed      // Step failed, needs attention

object StepStatus:
  extension (ss: StepStatus) def label: String = ss match
    case Waiting    => "Waiting"
    case Ready      => "Ready"
    case InProgress => "In Progress"
    case Completed  => "Completed"
    case Skipped    => "Skipped"
    case Failed     => "Failed"

/** Overall workflow status */
enum WorkflowStatus:
  case Pending
  case InProgress
  case Completed
  case OnHold
  case Cancelled

object WorkflowStatus:
  extension (ws: WorkflowStatus) def label: String = ws match
    case Pending    => "Pending"
    case InProgress => "In Progress"
    case Completed  => "Completed"
    case OnHold     => "On Hold"
    case Cancelled  => "Cancelled"

/** Priority for queue ordering */
enum Priority:
  case Rush, Normal, Low

object Priority:
  extension (p: Priority) def label: String = p match
    case Rush   => "Rush"
    case Normal => "Normal"
    case Low    => "Low"

/** Approval status for orders */
enum ApprovalStatus:
  case Pending
  case Approved
  case Rejected
  case ChangesRequested

object ApprovalStatus:
  extension (as: ApprovalStatus) def label: String = as match
    case Pending          => "Pending"
    case Approved         => "Approved"
    case Rejected         => "Rejected"
    case ChangesRequested => "Changes Requested"

// ─── Manufacturing Domain Objects ───────────────────────────────────

/** A single step in the manufacturing workflow — linearized for user simplicity */
final case class WorkflowStep(
    id: StepId,
    stationType: StationType,
    componentRole: Option[ComponentRole],
    stepIndex: Int,
    status: StepStatus,
    assignedTo: Option[EmployeeId],
    notes: String,
)

/** The manufacturing workflow for one order item (basket item) */
final case class ManufacturingWorkflow(
    id: WorkflowId,
    orderId: OrderId,
    orderItemIndex: Int,
    steps: List[WorkflowStep],
    status: WorkflowStatus,
    priority: Priority,
)

object ManufacturingWorkflow:
  /** Advance the workflow: complete the current step and make the next one Ready */
  def completeCurrentStep(workflow: ManufacturingWorkflow): ManufacturingWorkflow =
    val updatedSteps = workflow.steps match
      case Nil => Nil
      case steps =>
        val currentIdx = steps.indexWhere(s => s.status == StepStatus.InProgress)
        if currentIdx < 0 then steps
        else
          val completed = steps.updated(currentIdx, steps(currentIdx).copy(status = StepStatus.Completed))
          val nextIdx = currentIdx + 1
          if nextIdx < completed.length && completed(nextIdx).status == StepStatus.Waiting then
            completed.updated(nextIdx, completed(nextIdx).copy(status = StepStatus.Ready))
          else completed
    val allDone = updatedSteps.forall(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped)
    workflow.copy(
      steps = updatedSteps,
      status = if allDone then WorkflowStatus.Completed else workflow.status,
    )

  /** Start (pick up) the first Ready step only — enforces linear progression */
  def startCurrentStep(workflow: ManufacturingWorkflow, employeeId: Option[EmployeeId]): ManufacturingWorkflow =
    val readyIdx = workflow.steps.indexWhere(_.status == StepStatus.Ready)
    if readyIdx < 0 then workflow
    else
      val updatedSteps = workflow.steps.updated(
        readyIdx,
        workflow.steps(readyIdx).copy(status = StepStatus.InProgress, assignedTo = employeeId),
      )
      workflow.copy(
        steps = updatedSteps,
        status = WorkflowStatus.InProgress,
      )

  /** Get the current active step (first non-completed, non-skipped step) */
  def currentStep(workflow: ManufacturingWorkflow): Option[WorkflowStep] =
    workflow.steps.find(s => s.status != StepStatus.Completed && s.status != StepStatus.Skipped)

  /** Get completion percentage */
  def completionPercent(workflow: ManufacturingWorkflow): Int =
    if workflow.steps.isEmpty then 0
    else
      val done = workflow.steps.count(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped)
      (done * 100) / workflow.steps.size

/** An order in the manufacturing system */
final case class ManufacturingOrder(
    id: OrderId,
    customerName: String,
    items: List[ManufacturingOrderItem],
    approval: ApprovalStatus,
    priority: Priority,
    notes: String,
)

/** One item within a manufacturing order */
final case class ManufacturingOrderItem(
    itemIndex: Int,
    productDescription: String,
    quantity: Int,
    workflow: Option[ManufacturingWorkflow],
)

/** An employee who works at manufacturing stations */
final case class Employee(
    id: EmployeeId,
    name: String,
    stationCapabilities: Set[StationType],
    isActive: Boolean,
)
