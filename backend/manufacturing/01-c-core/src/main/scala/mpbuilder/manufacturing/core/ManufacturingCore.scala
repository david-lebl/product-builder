package mpbuilder.manufacturing
package core

import mpbuilder.shared.*
import kyo.*
import java.time.Instant

/** Pure DAG workflow generator.
  *
  * Fixes the legacy bug where unprinted hardware components (e.g. Roll-Up Stand)
  * generated Printing and Cutting steps: only components with `printJob.isDefined`
  * enter Prepress/Print/Cut; hardware-only components route directly to Assembly!
  */
private[manufacturing] object WorkflowGenerator:

  private final class StepIdGen(prefix: String):
    private var seq = 0
    def next(): StepId =
      seq += 1
      StepId.unsafe(s"$prefix-$seq")

  def generate(
      spec: ProductionSpec,
      workflowId: WorkflowId,
      deadline: Option[Instant],
      now: Instant,
  ): Workflow =
    val gen = StepIdGen(workflowId.value)
    val hasAnyPrintable = spec.components.exists(_.printJob.isDefined)

    val prepressOpt: Option[WorkflowStep] =
      if hasAnyPrintable then
        Some(
          WorkflowStep(
            id = gen.next(),
            stationType = StationType.Prepress,
            componentKey = None,
            dependsOn = Set.empty,
            status = StepStatus.Ready,
            notes = s"Prepress for ${spec.productLabel}",
          )
        )
      else None

    val (compSteps, terminalCompStepIds) =
      spec.components.toList.foldLeft((List.empty[WorkflowStep], Set.empty[StepId])) {
        case ((accSteps, accTerminals), comp) =>
          val stepsForComp = buildComponentSteps(gen, comp, prepressOpt.map(_.id), spec.hasFolding)
          val terminalId   = stepsForComp.lastOption.map(_.id)
          (accSteps ++ stepsForComp, accTerminals ++ terminalId)
      }

    val bindingOpt: Option[WorkflowStep] = spec.bindingLabel.map { label =>
      WorkflowStep(
        id = gen.next(),
        stationType = StationType.Binder,
        componentKey = None,
        dependsOn = terminalCompStepIds,
        status = if terminalCompStepIds.isEmpty then StepStatus.Ready else StepStatus.Waiting,
        notes = s"Bind: $label",
      )
    }

    val beforeQc = bindingOpt.map(s => Set(s.id)).getOrElse(terminalCompStepIds)
    val qcStep = WorkflowStep(
      id = gen.next(),
      stationType = StationType.QualityControl,
      componentKey = None,
      dependsOn = beforeQc,
      status = if beforeQc.isEmpty then StepStatus.Ready else StepStatus.Waiting,
      notes = "Final quality inspection",
    )

    val packStep = WorkflowStep(
      id = gen.next(),
      stationType = StationType.Packaging,
      componentKey = None,
      dependsOn = Set(qcStep.id),
      status = StepStatus.Waiting,
      notes = "Package and prepare for dispatch",
    )

    val all = prepressOpt.toList ++ compSteps ++ bindingOpt.toList ++ List(qcStep, packStep)
    Workflow(
      id = workflowId,
      orderId = spec.orderId,
      orderLineIndex = spec.orderLineIndex,
      steps = NonEmptyChunk.fromIterableOption(all).getOrElse(NonEmptyChunk.single(qcStep)),
      status = WorkflowStatus.Pending,
      priority = Priority.fromSpeed(spec.speed),
      deadline = deadline,
      createdAt = now,
    )

  private def buildComponentSteps(
      gen: StepIdGen,
      comp: ProductionComponentSpec,
      prepressIdOpt: Option[StepId],
      hasFolding: Boolean,
  ): List[WorkflowStep] =
    var steps      = List.empty[WorkflowStep]
    var lastStepId = Option.empty[StepId]

    def appendStep(station: StationType, note: String, deps: Set[StepId]): Unit =
      val st = WorkflowStep(
        id = gen.next(),
        stationType = station,
        componentKey = Some(comp.key),
        dependsOn = deps,
        status = if deps.isEmpty then StepStatus.Ready else StepStatus.Waiting,
        notes = note,
      )
      steps = steps :+ st
      lastStepId = Some(st.id)

    comp.printJob match
      case Some(pj) =>
        val printStation = pj.stationKind match
          case PrintStationKind.Digital           => StationType.DigitalPrinter
          case PrintStationKind.Offset            => StationType.OffsetPress
          case PrintStationKind.Letterpress       => StationType.Letterpress
          case PrintStationKind.LargeFormatInkjet => StationType.LargeFormatPrinter
          case PrintStationKind.ScreenPrint       => StationType.ScreenPrinter
          case PrintStationKind.DTG               => StationType.GarmentPrinter
          case PrintStationKind.Sublimation       => StationType.GarmentPrinter

        appendStep(printStation, s"Print ${comp.key.value} (${comp.materialName}, ${pj.inkNotation})", prepressIdOpt.toSet)

        if comp.finishingOperations.contains(FinishingOperation.Lamination) then
          appendStep(StationType.Laminator, s"Laminate ${comp.key.value}", lastStepId.toSet)

        if comp.finishingOperations.contains(FinishingOperation.UVOrAqueousCoating) then
          appendStep(StationType.UVCoater, s"Coat ${comp.key.value}", lastStepId.toSet)

        if comp.finishingOperations.contains(FinishingOperation.EmbossingOrFoil) then
          appendStep(StationType.EmbossingFoil, s"Emboss/Foil ${comp.key.value}", lastStepId.toSet)

        if comp.finishingOperations.contains(FinishingOperation.Embroidery) then
          appendStep(StationType.Embroidery, s"Embroider ${comp.key.value}", lastStepId.toSet)

        if pj.requiresGuillotineCut || comp.finishingOperations.contains(FinishingOperation.DieOrContourCut) then
          appendStep(StationType.Cutter, s"Cut ${comp.key.value}", lastStepId.toSet)

        if hasFolding || comp.finishingOperations.contains(FinishingOperation.ScoringOrPerforation) then
          appendStep(StationType.Folder, s"Fold/Score ${comp.key.value}", lastStepId.toSet)

        if comp.finishingOperations.contains(FinishingOperation.LargeFormatHardware) then
          appendStep(StationType.LargeFormatFinishing, s"Large-format finishing ${comp.key.value}", lastStepId.toSet)

      case None =>
        // Unprinted hardware component (e.g. Roll-Up Stand, Gift Box) — NO print or cut steps!
        appendStep(StationType.Assembly, s"Prepare/assemble hardware ${comp.key.value} (${comp.materialName})", Set.empty)

    steps

/** Pure DAG state transition engine for workflows. */
private[manufacturing] object WorkflowEngine:

  def claimStep(
      wf: Workflow,
      stepId: StepId,
      employee: Employee,
      machineOpt: Option[Machine],
      now: Instant,
  ): Either[ManufacturingError, Workflow] =
    for
      step <- wf.steps.find(_.id == stepId).toRight(ManufacturingError.StepNotFound(stepId))
      _    <- Either.cond(step.status == StepStatus.Ready, (), ManufacturingError.StepNotReady(stepId, step.status))
      _    <- Either.cond(employee.isActive, (), ManufacturingError.EmployeeInactive(employee.id))
      _    <- Either.cond(
                employee.stationCapabilities.contains(step.stationType),
                (),
                ManufacturingError.EmployeeNotCapable(employee.id, step.stationType),
              )
      _    <- machineOpt match
                case None => Right(())
                case Some(m) =>
                  for
                    _ <- Either.cond(m.status == MachineStatus.Online, (), ManufacturingError.MachineUnavailable(m.id, m.status))
                    _ <- Either.cond(
                           m.stationType == step.stationType,
                           (),
                           ManufacturingError.MachineStationMismatch(m.id, step.stationType, m.stationType),
                         )
                  yield ()
    yield
      val updatedSteps = wf.steps.map { s =>
        if s.id == stepId then
          s.copy(
            status = StepStatus.InProgress,
            assignedTo = Some(employee.id),
            assignedMachine = machineOpt.map(_.id),
            startedAt = Some(now),
          )
        else s
      }
      wf.copy(steps = updatedSteps, status = deriveWorkflowStatus(updatedSteps))

  def completeStep(
      wf: Workflow,
      stepId: StepId,
      notes: String,
      now: Instant,
  ): Either[ManufacturingError, Workflow] =
    for
      step <- wf.steps.find(_.id == stepId).toRight(ManufacturingError.StepNotFound(stepId))
      _    <- Either.cond(step.status == StepStatus.InProgress, (), ManufacturingError.StepNotInProgress(stepId, step.status))
    yield
      val markedDone = wf.steps.map { s =>
        if s.id == stepId then
          s.copy(
            status = StepStatus.Completed,
            completedAt = Some(now),
            notes = if notes.nonEmpty then notes else s.notes,
          )
        else s
      }
      val unlocked = unlockReadySteps(markedDone)
      wf.copy(steps = unlocked, status = deriveWorkflowStatus(unlocked))

  def failAndScheduleRework(
      wf: Workflow,
      stepId: StepId,
      reason: NonEmptyString,
  ): Either[ManufacturingError, Workflow] =
    for
      step <- wf.steps.find(_.id == stepId).toRight(ManufacturingError.StepNotFound(stepId))
      _    <- Either.cond(step.status == StepStatus.InProgress, (), ManufacturingError.StepNotInProgress(stepId, step.status))
    yield
      val reworkId = StepId.unsafe(s"${step.id.value}-rework-${wf.steps.size + 1}")
      val reworkStep = WorkflowStep(
        id = reworkId,
        stationType = step.stationType,
        componentKey = step.componentKey,
        dependsOn = step.dependsOn,
        status = StepStatus.Ready,
        notes = s"Rework for ${step.id.value}: ${reason.value}",
        isRework = true,
      )
      // Re-wire downstream steps that depended on the failed step to depend on the rework step instead
      val updated = wf.steps.map { s =>
        if s.id == stepId then s.copy(status = StepStatus.Failed, notes = reason.value)
        else if s.dependsOn.contains(stepId) then s.copy(dependsOn = (s.dependsOn - stepId) + reworkId)
        else s
      }.appended(reworkStep)
      wf.copy(steps = updated, status = WorkflowStatus.OnHold)

  private def unlockReadySteps(steps: NonEmptyChunk[WorkflowStep]): NonEmptyChunk[WorkflowStep] =
    val doneIds = steps.toList.filter(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped).map(_.id).toSet
    steps.map { s =>
      if s.status == StepStatus.Waiting && s.dependsOn.subsetOf(doneIds) then s.copy(status = StepStatus.Ready)
      else s
    }

  private def deriveWorkflowStatus(steps: NonEmptyChunk[WorkflowStep]): WorkflowStatus =
    val list = steps.toList
    if list.forall(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped) then WorkflowStatus.Completed
    else if list.exists(_.status == StepStatus.InProgress) || list.exists(_.status == StepStatus.Completed) then WorkflowStatus.InProgress
    else if list.exists(_.status == StepStatus.Failed) then WorkflowStatus.OnHold
    else WorkflowStatus.Pending

// ── Internal Aggregate, Ports & Live Service ─────────────────────────────────

private[manufacturing] final case class ProductionJob(
    id: JobId,
    orderId: OrderId,
    speed: ManufacturingSpeed,
    priority: Priority,
    workflows: NonEmptyChunk[Workflow],
    fulfilment: FulfilmentChecklist,
    deadline: Option[Instant],
    createdAt: Instant,
):
  def isAllWorkflowsCompleted: Boolean = workflows.forall(_.status == WorkflowStatus.Completed)

  def toView: ProductionJobView =
    ProductionJobView(id, orderId, speed, priority, workflows, fulfilment, deadline, createdAt)

private[manufacturing] trait ProductionJobRepository:
  def findById(id: JobId): Option[ProductionJob] < Async
  def listActive: List[ProductionJob] < Async
  def save(job: ProductionJob): Unit < Async

private[manufacturing] trait ShopResourceRepository:
  def findEmployee(id: EmployeeId): Option[Employee] < Async
  def findMachine(id: MachineId): Option[Machine] < Async
  def listMachines: List[Machine] < Async
  def findSpeedPolicy(categoryId: CategoryId, speed: ManufacturingSpeed): Option[SpeedAvailabilityPolicy] < Async

private[manufacturing] final class ManufacturingServiceLive(
    jobRepo: ProductionJobRepository,
    resourceRepo: ShopResourceRepository,
    eventBus: EventBus,
) extends ManufacturingService:

  override def createJob(
      jobId: JobId,
      specs: NonEmptyChunk[ProductionSpec],
      deadline: Option[Instant],
  ): ProductionJobView < (Async & Abort[ManufacturingError]) =
    for
      now <- Effects.now
      wfs  = specs.zipWithIndex.map { case (spec, idx) =>
               WorkflowGenerator.generate(spec, WorkflowId.unsafe(s"${jobId.value}-wf-$idx"), deadline, now)
             }
      head = specs.head
      job  = ProductionJob(
               id = jobId,
               orderId = head.orderId,
               speed = head.speed,
               priority = Priority.fromSpeed(head.speed),
               workflows = wfs,
               fulfilment = FulfilmentChecklist(Set.empty, specs.size),
               deadline = deadline,
               createdAt = now,
             )
      _   <- jobRepo.save(job)
      _   <- eventBus.publish(ProductionJobCreated(job.id, job.orderId, wfs.size, now))
    yield job.toView

  override def claimStep(
      jobId: JobId,
      workflowId: WorkflowId,
      stepId: StepId,
      employeeId: EmployeeId,
      machineId: Option[MachineId],
  ): ProductionJobView < (Async & Abort[ManufacturingError]) =
    for
      now     <- Effects.now
      job     <- jobRepo.findById(jobId).someOrFail(ManufacturingError.JobNotFound(jobId))
      wf      <- (job.workflows.find(_.id == workflowId)).orFail(ManufacturingError.WorkflowNotFound(workflowId))
      emp     <- resourceRepo.findEmployee(employeeId).someOrFail(ManufacturingError.EmployeeInactive(employeeId))
      machOpt <- Effects.traverse(machineId)(mid =>
                   resourceRepo.findMachine(mid).someOrFail(ManufacturingError.MachineUnavailable(mid, MachineStatus.Offline))
                 )
      updWf   <- (WorkflowEngine.claimStep(wf, stepId, emp, machOpt, now)).orAbort
      updJob   = job.copy(workflows = job.workflows.map(w => if w.id == workflowId then updWf else w))
      _       <- jobRepo.save(updJob)
    yield updJob.toView

  override def completeStep(
      jobId: JobId,
      workflowId: WorkflowId,
      stepId: StepId,
      notes: String,
  ): ProductionJobView < (Async & Abort[ManufacturingError]) =
    for
      now    <- Effects.now
      job    <- jobRepo.findById(jobId).someOrFail(ManufacturingError.JobNotFound(jobId))
      wf     <- (job.workflows.find(_.id == workflowId)).orFail(ManufacturingError.WorkflowNotFound(workflowId))
      step   <- (wf.steps.find(_.id == stepId)).orFail(ManufacturingError.StepNotFound(stepId))
      updWf  <- (WorkflowEngine.completeStep(wf, stepId, notes, now)).orAbort
      updJob  = job.copy(workflows = job.workflows.map(w => if w.id == workflowId then updWf else w))
      _      <- jobRepo.save(updJob)
      _      <- eventBus.publish(WorkflowStepCompleted(job.id, workflowId, stepId, step.stationType, now))
      _      <- eventBus.publish(ProductionJobCompleted(job.id, job.orderId, now)).when(updJob.isAllWorkflowsCompleted)
    yield updJob.toView

  override def failStepAndScheduleRework(
      jobId: JobId,
      workflowId: WorkflowId,
      stepId: StepId,
      reason: NonEmptyString,
  ): ProductionJobView < (Async & Abort[ManufacturingError]) =
    for
      job   <- jobRepo.findById(jobId).someOrFail(ManufacturingError.JobNotFound(jobId))
      wf    <- (job.workflows.find(_.id == workflowId)).orFail(ManufacturingError.WorkflowNotFound(workflowId))
      updWf <- (WorkflowEngine.failAndScheduleRework(wf, stepId, reason)).orAbort
      updJob = job.copy(workflows = job.workflows.map(w => if w.id == workflowId then updWf else w))
      _     <- jobRepo.save(updJob)
    yield updJob.toView

  override def currentCapacity: CapacitySnapshot < Async =
    for
      now      <- Effects.now
      jobs     <- jobRepo.listActive
      machines <- resourceRepo.listMachines
      allSteps  = jobs.flatMap(_.workflows.toList).flatMap(_.steps.toList)
      byStation = StationType.values.map { st =>
                    val active = allSteps.count(s => s.stationType == st && s.status == StepStatus.InProgress)
                    val queued = allSteps.count(s => s.stationType == st && s.status == StepStatus.Ready)
                    val online = machines.count(m => m.stationType == st && m.status == MachineStatus.Online)
                    val ratio  =
                      if online == 0 then BigDecimal(0)
                      else (BigDecimal(active + queued) / BigDecimal(online * 4)).min(BigDecimal(1)).setScale(2, BigDecimal.RoundingMode.HALF_UP)
                    st -> StationUtilisation(st, active, queued, online, ratio)
                  }.toMap
      onlineStations = byStation.values.filter(_.onlineMachines > 0).toList
      globalRatio    =
        if onlineStations.isEmpty then BigDecimal(0)
        else (onlineStations.map(_.utilisationRatio).sum / BigDecimal(onlineStations.size)).setScale(2, BigDecimal.RoundingMode.HALF_UP)
    yield CapacitySnapshot(globalRatio, byStation, now)

  override def checkSpeedAvailability(
      categoryId: CategoryId,
      speed: ManufacturingSpeed,
      quantity: Quantity,
      componentCount: Int,
      materialIds: Set[MaterialId],
      finishingOps: Set[FinishingOperation],
  ): Unit < (Async & Abort[ManufacturingError.SpeedTierUnavailable]) =
    resourceRepo.findSpeedPolicy(categoryId, speed).flatMap {
      case None => Kyo.unit
      case Some(p) if !p.enabled =>
        Abort.fail(ManufacturingError.SpeedTierUnavailable(categoryId, speed, s"$speed tier is disabled for this category"))
      case Some(p) if p.maxQuantity.exists(quantity.value > _) =>
        Abort.fail(ManufacturingError.SpeedTierUnavailable(categoryId, speed, s"Quantity ${quantity.value} exceeds max ${p.maxQuantity.get}"))
      case Some(p) if p.maxComponents.exists(componentCount > _) =>
        Abort.fail(ManufacturingError.SpeedTierUnavailable(categoryId, speed, s"Component count $componentCount exceeds max ${p.maxComponents.get}"))
      case Some(p) if (p.blockedMaterialIds intersect materialIds).nonEmpty =>
        Abort.fail(ManufacturingError.SpeedTierUnavailable(categoryId, speed, "Selected material is not eligible for this speed tier"))
      case Some(p) if (p.blockedFinishOps intersect finishingOps).nonEmpty =>
        Abort.fail(ManufacturingError.SpeedTierUnavailable(categoryId, speed, "Selected finishing operation cannot be rushed"))
      case Some(_) => Kyo.unit
    }

private[manufacturing] object ManufacturingServiceLive:
  def make(jobs: ProductionJobRepository, resources: ShopResourceRepository, eventBus: EventBus): ManufacturingService =
    ManufacturingServiceLive(jobs, resources, eventBus)
