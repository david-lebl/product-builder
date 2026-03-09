package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Generates a linear manufacturing workflow from a ProductConfiguration.
  * The workflow is derived purely from the configuration — the same data
  * used for pricing drives the manufacturing route.
  */
object WorkflowGenerator:

  /** Generate a linear workflow for one order item.
    * Steps are ordered for linear progression:
    * Prepress → Printing → Surface finishes → Cutting → Folding → Binding → QC → Packaging
    */
  def generate(
      orderId: OrderId,
      itemIndex: Int,
      config: ProductConfiguration,
  ): ManufacturingWorkflow =
    val steps = scala.collection.mutable.ListBuffer.empty[WorkflowStep]
    var idx = 0
    var stepCounter = 0

    def nextStepId(): StepId =
      stepCounter += 1
      StepId.unsafe(s"step-${orderId.value}-$itemIndex-$stepCounter")

    def addStep(stationType: StationType, role: Option[ComponentRole]): Unit =
      steps += WorkflowStep(
        id = nextStepId(),
        stationType = stationType,
        componentRole = role,
        stepIndex = idx,
        status = if idx == 0 then StepStatus.Ready else StepStatus.Waiting,
        assignedTo = None,
        notes = "",
      )
      idx += 1

    // 1. Prepress — always first
    addStep(StationType.Prepress, None)

    // 2. Printing — per component, based on printing process type
    val printStation = config.printingMethod.processType match
      case PrintingProcessType.Digital                          => StationType.DigitalPrinter
      case PrintingProcessType.Offset                           => StationType.OffsetPress
      case PrintingProcessType.UVCurableInkjet |
           PrintingProcessType.LatexInkjet |
           PrintingProcessType.SolventInkjet                    => StationType.LargeFormatPrinter
      case PrintingProcessType.Letterpress                      => StationType.Letterpress
      case PrintingProcessType.ScreenPrint                      => StationType.DigitalPrinter // fallback

    for comp <- config.components do
      addStep(printStation, Some(comp.role))

    // 3. Surface finishes (lamination, UV coating) — per component
    for comp <- config.components do
      val surfaceFinishes = comp.finishes.filter { sf =>
        sf.finishType match
          case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
            true
          case _ => false
      }
      if surfaceFinishes.nonEmpty then
        addStep(StationType.Laminator, Some(comp.role))

      val uvFinishes = comp.finishes.filter { sf =>
        sf.finishType match
          case FinishType.UVCoating | FinishType.AqueousCoating | FinishType.Varnish =>
            true
          case _ => false
      }
      if uvFinishes.nonEmpty then
        addStep(StationType.UVCoater, Some(comp.role))

    // 4. Decorative finishes (embossing, foil) — per component
    for comp <- config.components do
      val decorativeFinishes = comp.finishes.filter { sf =>
        sf.finishType match
          case FinishType.Embossing | FinishType.Debossing |
               FinishType.FoilStamping | FinishType.Thermography =>
            true
          case _ => false
      }
      if decorativeFinishes.nonEmpty then
        addStep(StationType.EmbossingFoil, Some(comp.role))

    // 5. Cutting — for sheet-based materials or die-cut finishes
    val hasCuttingFinish = config.components.exists { comp =>
      comp.finishes.exists { sf =>
        sf.finishType match
          case FinishType.DieCut | FinishType.ContourCut | FinishType.KissCut => true
          case _ => false
      }
    }
    if hasCuttingFinish || config.components.exists(_.sheetCount > 0) then
      addStep(StationType.Cutter, None)

    // 6. Folding — if fold type spec present
    val hasFold = config.specifications.get(SpecKind.FoldType).isDefined
    if hasFold then
      addStep(StationType.Folder, None)

    // 7. Binding — if binding method spec present
    val hasBinding = config.specifications.get(SpecKind.BindingMethod).isDefined
    if hasBinding then
      addStep(StationType.Binder, None)

    // 8. Large format finishing — grommets, hem, mounting
    val hasLargeFormatFinishing = config.components.exists { comp =>
      comp.finishes.exists { sf =>
        sf.finishType match
          case FinishType.Grommets | FinishType.Hem | FinishType.Mounting => true
          case _ => false
      }
    }
    if hasLargeFormatFinishing then
      addStep(StationType.LargeFormatFinishing, None)

    // 9. Quality Control — always
    addStep(StationType.QualityControl, None)

    // 10. Packaging — always last
    addStep(StationType.Packaging, None)

    ManufacturingWorkflow(
      id = WorkflowId.unsafe(s"wf-${orderId.value}-$itemIndex"),
      orderId = orderId,
      orderItemIndex = itemIndex,
      steps = steps.toList,
      status = WorkflowStatus.Pending,
      priority = Priority.Normal,
    )
