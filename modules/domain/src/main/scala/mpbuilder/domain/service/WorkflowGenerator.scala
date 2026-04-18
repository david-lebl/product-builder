package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Derives a manufacturing workflow from a product configuration.
  *
  * Pure function: `ProductConfiguration => List[WorkflowStep]`
  *
  * The workflow step sequence and dependencies are fully determinable from the
  * configuration data — the same data already used for pricing.
  */
object WorkflowGenerator:

  /** Step ID counter scoped to a single generate() call. */
  private class StepIdGen(prefix: String):
    private var counter = 0
    def next(): StepId =
      counter += 1
      StepId.unsafe(s"$prefix-$counter")

  /** Generate a complete manufacturing workflow for a product configuration. */
  def generate(
      config: ProductConfiguration,
      orderId: OrderId,
      orderItemIndex: Int,
      workflowId: WorkflowId,
      priority: Priority = Priority.Normal,
      deadline: Option[Long] = None,
      createdAt: Long = System.currentTimeMillis(),
  ): ManufacturingWorkflow =
    val gen = StepIdGen(workflowId.value)

    // 1. Prepress — always first, no dependencies
    val prepressStep = WorkflowStep(
      id = gen.next(),
      stationType = StationType.Prepress,
      componentRole = None,
      dependsOn = Set.empty,
      status = StepStatus.Ready,
      assignedTo = None,
      assignedMachine = None,
      startedAt = None,
      completedAt = None,
      notes = s"Prepress for ${config.category.name(Language.En)}",
    )

    // 2. Per-component steps (printing, finishing, cutting, folding)
    val componentSteps = config.components.flatMap { comp =>
      generateComponentSteps(gen, comp, config, prepressStep.id)
    }

    // 3. Cross-component steps (binding, QC, packaging)
    val lastComponentSteps = findLastStepsPerComponent(componentSteps, config.components)

    val bindingStep = generateBindingStep(gen, config, lastComponentSteps)
    val afterBindingIds = bindingStep.map(s => Set(s.id)).getOrElse(lastComponentSteps)

    val qcStep = WorkflowStep(
      id = gen.next(),
      stationType = StationType.QualityControl,
      componentRole = None,
      dependsOn = afterBindingIds,
      status = StepStatus.Waiting,
      assignedTo = None,
      assignedMachine = None,
      startedAt = None,
      completedAt = None,
      notes = "Final quality inspection",
    )

    val packagingStep = WorkflowStep(
      id = gen.next(),
      stationType = StationType.Packaging,
      componentRole = None,
      dependsOn = Set(qcStep.id),
      status = StepStatus.Waiting,
      assignedTo = None,
      assignedMachine = None,
      startedAt = None,
      completedAt = None,
      notes = "Package and prepare for dispatch",
    )

    val allSteps = List(prepressStep) ++ componentSteps ++ bindingStep.toList ++ List(qcStep, packagingStep)

    ManufacturingWorkflow(
      id = workflowId,
      orderId = orderId,
      orderItemIndex = orderItemIndex,
      steps = allSteps,
      status = WorkflowStatus.Pending,
      priority = priority,
      deadline = deadline,
      createdAt = createdAt,
    )

  /** Generate workflow steps for a single product component. */
  private def generateComponentSteps(
      gen: StepIdGen,
      component: ProductComponent,
      config: ProductConfiguration,
      prepressStepId: StepId,
  ): List[WorkflowStep] =
    val role = component.role
    var steps = List.empty[WorkflowStep]

    // Printing step — depends on prepress
    val printStation = printingStationFor(config.printingMethod.processType)
    val printStep = WorkflowStep(
      id = gen.next(),
      stationType = printStation,
      componentRole = Some(role),
      dependsOn = Set(prepressStepId),
      status = StepStatus.Waiting,
      assignedTo = None,
      assignedMachine = None,
      startedAt = None,
      completedAt = None,
      notes = s"Print ${role} — ${component.material.name(Language.En)}, ${component.inkConfiguration.notation}",
    )
    steps = steps :+ printStep

    var lastStepId = printStep.id

    // Surface finishing (lamination, UV coating) — depends on printing
    val surfaceFinishes = component.finishes.filter(sf =>
      sf.finishType match
        case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
          true
        case _ => false
    )
    if surfaceFinishes.nonEmpty then
      val lamStep = WorkflowStep(
        id = gen.next(),
        stationType = StationType.Laminator,
        componentRole = Some(role),
        dependsOn = Set(lastStepId),
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = s"Laminate ${role} — ${surfaceFinishes.map(_.name(Language.En)).mkString(", ")}",
      )
      steps = steps :+ lamStep
      lastStepId = lamStep.id

    val uvFinishes = component.finishes.filter(sf =>
      sf.finishType match
        case FinishType.UVCoating | FinishType.AqueousCoating | FinishType.Varnish => true
        case _ => false
    )
    if uvFinishes.nonEmpty then
      val uvStep = WorkflowStep(
        id = gen.next(),
        stationType = StationType.UVCoater,
        componentRole = Some(role),
        dependsOn = Set(lastStepId),
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = s"UV/Aqueous coat ${role} — ${uvFinishes.map(_.name(Language.En)).mkString(", ")}",
      )
      steps = steps :+ uvStep
      lastStepId = uvStep.id

    // Decorative finishing (embossing, foil) — depends on surface finish or printing
    val decorativeFinishes = component.finishes.filter(sf =>
      sf.finishType match
        case FinishType.Embossing | FinishType.Debossing | FinishType.FoilStamping | FinishType.Thermography =>
          true
        case _ => false
    )
    if decorativeFinishes.nonEmpty then
      val embossStep = WorkflowStep(
        id = gen.next(),
        stationType = StationType.EmbossingFoil,
        componentRole = Some(role),
        dependsOn = Set(lastStepId),
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = s"Emboss/Foil ${role} — ${decorativeFinishes.map(_.name(Language.En)).mkString(", ")}",
      )
      steps = steps :+ embossStep
      lastStepId = embossStep.id

    // Cutting — depends on finishing or printing
    val needsCutting = component.sheetCount > 0 || component.finishes.exists(sf =>
      sf.finishType match
        case FinishType.DieCut | FinishType.ContourCut | FinishType.KissCut => true
        case _ => false
    )
    if needsCutting then
      val cutStep = WorkflowStep(
        id = gen.next(),
        stationType = StationType.Cutter,
        componentRole = Some(role),
        dependsOn = Set(lastStepId),
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = s"Cut ${role}",
      )
      steps = steps :+ cutStep
      lastStepId = cutStep.id

    // Large format finishing (grommets, hem) — depends on cutting or printing
    val largeFormatFinishes = component.finishes.filter(sf =>
      sf.finishType match
        case FinishType.Grommets | FinishType.Hem | FinishType.Mounting | FinishType.GumRope => true
        case _ => false
    )
    if largeFormatFinishes.nonEmpty then
      val lfStep = WorkflowStep(
        id = gen.next(),
        stationType = StationType.LargeFormatFinishing,
        componentRole = Some(role),
        dependsOn = Set(lastStepId),
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = s"Large format finish ${role} — ${largeFormatFinishes.map(_.name(Language.En)).mkString(", ")}",
      )
      steps = steps :+ lfStep
      lastStepId = lfStep.id

    // Folding — depends on cutting
    val hasFolding = config.specifications.get(SpecKind.FoldType).isDefined
    if hasFolding && (role == ComponentRole.Main || role == ComponentRole.Body) then
      val foldStep = WorkflowStep(
        id = gen.next(),
        stationType = StationType.Folder,
        componentRole = Some(role),
        dependsOn = Set(lastStepId),
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = s"Fold ${role}",
      )
      steps = steps :+ foldStep

    steps

  /** Generate a binding step if the configuration requires it. */
  private def generateBindingStep(
      gen: StepIdGen,
      config: ProductConfiguration,
      lastComponentStepIds: Set[StepId],
  ): Option[WorkflowStep] =
    config.specifications.get(SpecKind.BindingMethod).map { _ =>
      WorkflowStep(
        id = gen.next(),
        stationType = StationType.Binder,
        componentRole = None,
        dependsOn = lastComponentStepIds,
        status = StepStatus.Waiting,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = "Binding assembly",
      )
    }

  /** Find the last step for each component (the step that has no other step depending on it within the same component). */
  private def findLastStepsPerComponent(
      componentSteps: List[WorkflowStep],
      components: List[ProductComponent],
  ): Set[StepId] =
    components.flatMap { comp =>
      val roleSteps = componentSteps.filter(_.componentRole.contains(comp.role))
      val allDeps = roleSteps.flatMap(_.dependsOn).toSet
      val lastSteps = roleSteps.filterNot(s => roleSteps.exists(_.dependsOn.contains(s.id)))
      if lastSteps.nonEmpty then lastSteps.map(_.id)
      else roleSteps.lastOption.map(_.id).toList
    }.toSet

  /** Map printing process type to station type. */
  private def printingStationFor(processType: PrintingProcessType): StationType =
    processType match
      case PrintingProcessType.Digital                                => StationType.DigitalPrinter
      case PrintingProcessType.Offset                                => StationType.OffsetPress
      case PrintingProcessType.UVCurableInkjet                       => StationType.LargeFormatPrinter
      case PrintingProcessType.Letterpress                           => StationType.Letterpress
      case PrintingProcessType.ScreenPrint                           => StationType.DigitalPrinter
      case PrintingProcessType.LatexInkjet | PrintingProcessType.SolventInkjet => StationType.LargeFormatPrinter
