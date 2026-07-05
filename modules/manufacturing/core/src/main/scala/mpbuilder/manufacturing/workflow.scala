package mpbuilder.manufacturing

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import zio.prelude.*

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

extension (s: ManufacturingSpeed) def toPriority: Priority = s match
  case ManufacturingSpeed.Express  => Priority.Rush
  case ManufacturingSpeed.Standard => Priority.Normal
  case ManufacturingSpeed.Economy  => Priority.Low

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
