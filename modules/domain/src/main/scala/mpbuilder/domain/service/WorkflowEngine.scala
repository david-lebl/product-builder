package mpbuilder.domain.service

import mpbuilder.domain.model.*
import zio.prelude.*

/** Pure state-transition engine for manufacturing workflows.
  *
  * All operations return `Validation[WorkflowError, ManufacturingWorkflow]`
  * and enforce DAG constraints. Errors are accumulated (not short-circuit).
  */
object WorkflowEngine:

  /** Required stations that cannot be skipped. */
  private val requiredStations: Set[StationType] =
    Set(StationType.Prepress, StationType.QualityControl, StationType.Packaging)

  /** Start a step — transitions from Ready → InProgress.
    *
    * Validates:
    * - Step exists in the workflow
    * - Workflow is active (Pending or InProgress)
    * - Step is in Ready status
    * - All dependencies are met (Completed or Skipped)
    *
    * Side effects on the workflow:
    * - Step status becomes InProgress, assignedTo and startedAt are set
    * - Workflow status becomes InProgress if it was Pending
    */
  def startStep(
      wf: ManufacturingWorkflow,
      stepId: StepId,
      employeeId: EmployeeId,
      now: Long = System.currentTimeMillis(),
  ): Validation[WorkflowError, ManufacturingWorkflow] =
    validateWorkflowActive(wf).flatMap { _ =>
      findStep(wf, stepId).flatMap { step =>
        validateStepReady(step).flatMap { _ =>
          validateDependenciesMet(wf, step).map { _ =>
            val updatedSteps = wf.steps.map { s =>
              if s.id == stepId then
                s.copy(
                  status = StepStatus.InProgress,
                  assignedTo = Some(employeeId),
                  startedAt = Some(now),
                )
              else s
            }
            val newStatus =
              if wf.status == WorkflowStatus.Pending then WorkflowStatus.InProgress
              else wf.status
            wf.copy(steps = updatedSteps, status = newStatus)
          }
        }
      }
    }

  /** Complete a step — transitions from InProgress → Completed.
    *
    * Validates:
    * - Step exists in the workflow
    * - Workflow is active (InProgress)
    * - Step is InProgress
    *
    * Side effects on the workflow:
    * - Step status becomes Completed, completedAt is set
    * - Downstream steps are re-evaluated for readiness (Waiting → Ready)
    * - If all steps are Completed/Skipped, workflow status becomes Completed
    */
  def completeStep(
      wf: ManufacturingWorkflow,
      stepId: StepId,
      now: Long = System.currentTimeMillis(),
  ): Validation[WorkflowError, ManufacturingWorkflow] =
    validateWorkflowActive(wf).flatMap { _ =>
      findStep(wf, stepId).flatMap { step =>
        validateStepInProgress(step).map { _ =>
          val updatedSteps = wf.steps.map { s =>
            if s.id == stepId then
              s.copy(status = StepStatus.Completed, completedAt = Some(now))
            else s
          }
          val intermediate = wf.copy(steps = updatedSteps)
          val promoted = promoteReadySteps(intermediate)
          val finalStatus = deriveWorkflowStatus(promoted)
          promoted.copy(status = finalStatus)
        }
      }
    }

  /** Fail a step — transitions from InProgress → Failed.
    *
    * Validates:
    * - Step exists in the workflow
    * - Workflow is active
    * - Step is InProgress
    *
    * Side effects:
    * - Step status becomes Failed, notes are appended with failure reason
    * - Workflow status becomes OnHold
    */
  def failStep(
      wf: ManufacturingWorkflow,
      stepId: StepId,
      reason: String,
      now: Long = System.currentTimeMillis(),
  ): Validation[WorkflowError, ManufacturingWorkflow] =
    validateWorkflowActive(wf).flatMap { _ =>
      findStep(wf, stepId).flatMap { step =>
        validateStepInProgress(step).map { _ =>
          val updatedSteps = wf.steps.map { s =>
            if s.id == stepId then
              val newNotes =
                if s.notes.isEmpty then s"FAILED: $reason"
                else s"${s.notes} | FAILED: $reason"
              s.copy(status = StepStatus.Failed, completedAt = Some(now), notes = newNotes)
            else s
          }
          wf.copy(steps = updatedSteps, status = WorkflowStatus.OnHold)
        }
      }
    }

  /** Skip a step — transitions from Waiting or Ready → Skipped.
    *
    * Validates:
    * - Step exists in the workflow
    * - Workflow is active
    * - Step is Waiting or Ready
    * - Step is not a required station (Prepress, QC, Packaging)
    *
    * Side effects:
    * - Step status becomes Skipped
    * - Downstream steps are re-evaluated for readiness
    */
  def skipStep(
      wf: ManufacturingWorkflow,
      stepId: StepId,
  ): Validation[WorkflowError, ManufacturingWorkflow] =
    validateWorkflowActive(wf).flatMap { _ =>
      findStep(wf, stepId).flatMap { step =>
        validateStepSkippable(step).map { _ =>
          val updatedSteps = wf.steps.map { s =>
            if s.id == stepId then s.copy(status = StepStatus.Skipped)
            else s
          }
          val intermediate = wf.copy(steps = updatedSteps)
          val promoted = promoteReadySteps(intermediate)
          val finalStatus = deriveWorkflowStatus(promoted)
          promoted.copy(status = finalStatus)
        }
      }
    }

  /** Reset a step — transitions from Completed, Failed, or Skipped → Ready for rework.
    *
    * Validates:
    * - Step exists in the workflow
    * - Step is Completed, Failed, or Skipped
    * - All dependencies are still met (Completed or Skipped)
    *
    * Side effects:
    * - Step status becomes Ready, assignedTo/startedAt/completedAt are cleared
    * - isRework flag is set to true
    * - Downstream steps that depended on this step revert to Waiting
    * - Workflow status becomes InProgress if it was Completed or OnHold
    */
  def resetStep(
      wf: ManufacturingWorkflow,
      stepId: StepId,
  ): Validation[WorkflowError, ManufacturingWorkflow] =
    findStep(wf, stepId).flatMap { step =>
      validateStepResettable(step).flatMap { _ =>
        validateDependenciesMet(wf, step).map { _ =>
          // Reset the step itself
          val updatedSteps = wf.steps.map { s =>
            if s.id == stepId then
              s.copy(
                status = StepStatus.Ready,
                assignedTo = None,
                startedAt = None,
                completedAt = None,
                isRework = true,
              )
            else s
          }

          // Revert downstream steps that depend (directly or transitively) on this step
          val downstreamIds = findDownstreamSteps(wf, stepId)
          val withReverted = updatedSteps.map { s =>
            if downstreamIds.contains(s.id) && (s.status == StepStatus.Ready || s.status == StepStatus.Completed || s.status == StepStatus.Skipped) then
              s.copy(
                status = StepStatus.Waiting,
                assignedTo = None,
                startedAt = None,
                completedAt = None,
              )
            else s
          }

          val intermediate = wf.copy(steps = withReverted)
          val promoted = promoteReadySteps(intermediate)
          val newStatus =
            if wf.status == WorkflowStatus.Completed || wf.status == WorkflowStatus.OnHold then WorkflowStatus.InProgress
            else wf.status
          promoted.copy(status = newStatus)
        }
      }
    }

  // --- Private helpers ---

  private def findStep(wf: ManufacturingWorkflow, stepId: StepId): Validation[WorkflowError, WorkflowStep] =
    wf.steps.find(_.id == stepId) match
      case Some(step) => Validation.succeed(step)
      case None       => Validation.fail(WorkflowError.StepNotFound(stepId))

  private def validateWorkflowActive(wf: ManufacturingWorkflow): Validation[WorkflowError, Unit] =
    wf.status match
      case WorkflowStatus.Pending | WorkflowStatus.InProgress => Validation.succeed(())
      case other => Validation.fail(WorkflowError.WorkflowNotActive(wf.id, other))

  private def validateStepReady(step: WorkflowStep): Validation[WorkflowError, Unit] =
    step.status match
      case StepStatus.Ready => Validation.succeed(())
      case StepStatus.Completed => Validation.fail(WorkflowError.StepAlreadyCompleted(step.id))
      case StepStatus.Skipped   => Validation.fail(WorkflowError.StepAlreadySkipped(step.id))
      case other                => Validation.fail(WorkflowError.StepNotReady(step.id, other))

  private def validateStepInProgress(step: WorkflowStep): Validation[WorkflowError, Unit] =
    step.status match
      case StepStatus.InProgress => Validation.succeed(())
      case StepStatus.Completed  => Validation.fail(WorkflowError.StepAlreadyCompleted(step.id))
      case other                 => Validation.fail(WorkflowError.StepNotInProgress(step.id, other))

  private def validateDependenciesMet(wf: ManufacturingWorkflow, step: WorkflowStep): Validation[WorkflowError, Unit] =
    val completedOrSkippedIds = wf.steps
      .filter(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped)
      .map(_.id)
      .toSet
    val unmet = step.dependsOn -- completedOrSkippedIds
    if unmet.isEmpty then Validation.succeed(())
    else Validation.fail(WorkflowError.DependenciesNotMet(step.id, unmet))

  private def validateStepSkippable(step: WorkflowStep): Validation[WorkflowError, Unit] =
    step.status match
      case StepStatus.Waiting | StepStatus.Ready =>
        if requiredStations.contains(step.stationType) then
          Validation.fail(WorkflowError.StepCannotBeSkipped(step.id, step.stationType))
        else
          Validation.succeed(())
      case StepStatus.Completed => Validation.fail(WorkflowError.StepAlreadyCompleted(step.id))
      case StepStatus.Skipped   => Validation.fail(WorkflowError.StepAlreadySkipped(step.id))
      case other                => Validation.fail(WorkflowError.StepNotReady(step.id, other))

  private def validateStepResettable(step: WorkflowStep): Validation[WorkflowError, Unit] =
    step.status match
      case StepStatus.Completed | StepStatus.Failed | StepStatus.Skipped => Validation.succeed(())
      case other => Validation.fail(WorkflowError.StepCannotBeReset(step.id, other))

  /** Promote Waiting steps to Ready when all their dependencies are Completed or Skipped. */
  private def promoteReadySteps(wf: ManufacturingWorkflow): ManufacturingWorkflow =
    val completedOrSkippedIds = wf.steps
      .filter(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped)
      .map(_.id)
      .toSet

    val updatedSteps = wf.steps.map { step =>
      if step.status == StepStatus.Waiting && step.dependsOn.subsetOf(completedOrSkippedIds) then
        step.copy(status = StepStatus.Ready)
      else step
    }
    wf.copy(steps = updatedSteps)

  /** Derive aggregate workflow status from step states. */
  private def deriveWorkflowStatus(wf: ManufacturingWorkflow): WorkflowStatus =
    val steps = wf.steps
    if steps.exists(_.status == StepStatus.Failed) then WorkflowStatus.OnHold
    else if steps.forall(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped) then WorkflowStatus.Completed
    else if steps.exists(s => s.status == StepStatus.InProgress || s.status == StepStatus.Completed) then WorkflowStatus.InProgress
    else WorkflowStatus.Pending

  /** Find all steps that depend (directly or transitively) on the given step. */
  private def findDownstreamSteps(wf: ManufacturingWorkflow, stepId: StepId): Set[StepId] =
    var downstream = Set.empty[StepId]
    var frontier = Set(stepId)
    while frontier.nonEmpty do
      val next = wf.steps
        .filter(s => s.dependsOn.intersect(frontier).nonEmpty && !downstream.contains(s.id))
        .map(_.id)
        .toSet
      downstream = downstream ++ next
      frontier = next
    downstream
