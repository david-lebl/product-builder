package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*
import com.softwaremill.quicklens.*

object WorkflowEngineSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset

  private def buildConfig(
      categoryId: CategoryId,
      printingMethodId: PrintingMethodId,
      components: List[ComponentRequest],
      specs: List[SpecValue],
  ): ProductConfiguration =
    ConfigurationBuilder.build(
      ConfigurationRequest(categoryId, printingMethodId, components, specs),
      catalog,
      ruleset,
      ConfigurationId.unsafe("test-engine"),
    ).toEither.toOption.get

  private def simpleWorkflow: ManufacturingWorkflow =
    val config = buildConfig(
      SampleCatalog.businessCardsId,
      SampleCatalog.digitalId,
      List(ComponentRequest(ComponentRole.Main, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil)),
      List(
        SpecValue.SizeSpec(Dimension(90, 55)),
        SpecValue.QuantitySpec(Quantity.unsafe(500)),
      ),
    )
    WorkflowGenerator.generate(
      config,
      orderId = OrderId.unsafe("order-1"),
      orderItemIndex = 0,
      workflowId = WorkflowId.unsafe("wf-1"),
      createdAt = 1000000L,
    )

  private def bookletWorkflow: ManufacturingWorkflow =
    val config = buildConfig(
      SampleCatalog.bookletsId,
      SampleCatalog.digitalId,
      List(
        ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4, Nil),
        ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4, Nil),
      ),
      List(
        SpecValue.SizeSpec(Dimension(210, 297)),
        SpecValue.QuantitySpec(Quantity.unsafe(100)),
        SpecValue.PagesSpec(16),
        SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
      ),
    )
    WorkflowGenerator.generate(
      config,
      orderId = OrderId.unsafe("order-2"),
      orderItemIndex = 0,
      workflowId = WorkflowId.unsafe("wf-2"),
      createdAt = 1000000L,
    )

  private val emp1 = EmployeeId.unsafe("emp-1")
  private val now = 2000000L

  private def stepByType(wf: ManufacturingWorkflow, st: StationType): WorkflowStep =
    wf.steps.find(_.stationType == st).get

  def spec = suite("WorkflowEngine")(
    suite("startStep")(
      test("starts a Ready step successfully") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val result = WorkflowEngine.startStep(wf, prepressId, emp1, now)

        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.status == StepStatus.InProgress,
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.assignedTo.contains(emp1),
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.startedAt.contains(now),
        )
      },
      test("transitions workflow from Pending to InProgress") {
        val wf = simpleWorkflow
        assertTrue(wf.status == WorkflowStatus.Pending)

        val prepressId = stepByType(wf, StationType.Prepress).id
        val result = WorkflowEngine.startStep(wf, prepressId, emp1, now)

        assertTrue(result.toEither.toOption.get.status == WorkflowStatus.InProgress)
      },
      test("fails if step is not Ready (Waiting)") {
        val wf = simpleWorkflow
        val printId = stepByType(wf, StationType.DigitalPrinter).id

        val result = WorkflowEngine.startStep(wf, printId, emp1, now)

        assertTrue(result.toEither.isLeft)
      },
      test("fails if step does not exist") {
        val wf = simpleWorkflow
        val fakeId = StepId.unsafe("nonexistent")

        val result = WorkflowEngine.startStep(wf, fakeId, emp1, now)

        assertTrue(result.toEither.isLeft)
      },
      test("fails if workflow is Completed") {
        val wf = simpleWorkflow.copy(status = WorkflowStatus.Completed)
        val prepressId = stepByType(wf, StationType.Prepress).id

        val result = WorkflowEngine.startStep(wf, prepressId, emp1, now)

        assertTrue(result.toEither.isLeft)
      },
      test("fails if workflow is Cancelled") {
        val wf = simpleWorkflow.copy(status = WorkflowStatus.Cancelled)
        val prepressId = stepByType(wf, StationType.Prepress).id

        val result = WorkflowEngine.startStep(wf, prepressId, emp1, now)

        assertTrue(result.toEither.isLeft)
      },
      test("fails if step already completed") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id
        val completed = wf
          .modify(_.steps.eachWhere(_.id == prepressId).status).setTo(StepStatus.Completed)
          .modify(_.status).setTo(WorkflowStatus.InProgress)

        val result = WorkflowEngine.startStep(completed, prepressId, emp1, now)

        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("completeStep")(
      test("completes an InProgress step successfully") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val result = WorkflowEngine.completeStep(started, prepressId, now + 1000)

        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.status == StepStatus.Completed,
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.completedAt.contains(now + 1000),
        )
      },
      test("promotes downstream steps to Ready after completion") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val completed = WorkflowEngine.completeStep(started, prepressId, now + 1000).toEither.toOption.get

        val printStep = stepByType(completed, StationType.DigitalPrinter)
        assertTrue(printStep.status == StepStatus.Ready)
      },
      test("workflow becomes Completed when all steps are done") {
        var wf = simpleWorkflow

        // Walk through all steps sequentially
        val steps = wf.steps
        for step <- steps do
          if step.status == StepStatus.Ready then
            wf = WorkflowEngine.startStep(wf, step.id, emp1, now).toEither.toOption.get
            wf = WorkflowEngine.completeStep(wf, step.id, now + 1000).toEither.toOption.get
          else if step.status == StepStatus.Waiting then
            // Might be promoted to Ready by now
            val currentStep = wf.steps.find(_.id == step.id).get
            if currentStep.status == StepStatus.Ready then
              wf = WorkflowEngine.startStep(wf, step.id, emp1, now).toEither.toOption.get
              wf = WorkflowEngine.completeStep(wf, step.id, now + 1000).toEither.toOption.get

        assertTrue(wf.status == WorkflowStatus.Completed)
      },
      test("fails if step is not InProgress") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        // Step is Ready, not InProgress
        val result = WorkflowEngine.completeStep(wf, prepressId, now)

        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("failStep")(
      test("fails an InProgress step and sets workflow OnHold") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val result = WorkflowEngine.failStep(started, prepressId, "Artwork resolution too low", now + 500)

        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.status == StepStatus.Failed,
          result.toEither.toOption.get.status == WorkflowStatus.OnHold,
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.notes.contains("FAILED"),
          result.toEither.toOption.get.steps.find(_.id == prepressId).get.notes.contains("Artwork resolution too low"),
        )
      },
      test("fails if step is not InProgress") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val result = WorkflowEngine.failStep(wf, prepressId, "reason", now)

        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("skipStep")(
      test("skips a Ready step for optional station") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        // Complete prepress to make print step Ready
        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val completed = WorkflowEngine.completeStep(started, prepressId, now + 1000).toEither.toOption.get

        val printStep = stepByType(completed, StationType.DigitalPrinter)
        assertTrue(printStep.status == StepStatus.Ready)

        val result = WorkflowEngine.skipStep(completed, printStep.id)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.steps.find(_.id == printStep.id).get.status == StepStatus.Skipped,
        )
      },
      test("promotes downstream steps when a step is skipped") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        // Complete prepress
        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val completed = WorkflowEngine.completeStep(started, prepressId, now + 1000).toEither.toOption.get

        // Skip the print step
        val printStep = stepByType(completed, StationType.DigitalPrinter)
        val skipped = WorkflowEngine.skipStep(completed, printStep.id).toEither.toOption.get

        // Find the step that depended on printing — it should now be Ready
        val downstreamSteps = skipped.steps.filter(s =>
          s.dependsOn.contains(printStep.id) && s.status == StepStatus.Ready
        )
        assertTrue(downstreamSteps.nonEmpty)
      },
      test("cannot skip a required station (Prepress)") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val result = WorkflowEngine.skipStep(wf, prepressId)

        assertTrue(result.toEither.isLeft)
      },
      test("cannot skip a required station (QualityControl)") {
        val wf = simpleWorkflow
        val qcStep = stepByType(wf, StationType.QualityControl)

        val result = WorkflowEngine.skipStep(wf, qcStep.id)

        assertTrue(result.toEither.isLeft)
      },
      test("cannot skip a required station (Packaging)") {
        val wf = simpleWorkflow
        val packStep = stepByType(wf, StationType.Packaging)

        val result = WorkflowEngine.skipStep(wf, packStep.id)

        assertTrue(result.toEither.isLeft)
      },
      test("cannot skip an InProgress step") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val completed = WorkflowEngine.completeStep(started, prepressId, now + 1000).toEither.toOption.get

        val printStep = stepByType(completed, StationType.DigitalPrinter)
        val printing = WorkflowEngine.startStep(completed, printStep.id, emp1, now + 2000).toEither.toOption.get

        val result = WorkflowEngine.skipStep(printing, printStep.id)
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("resetStep")(
      test("resets a Completed step to Ready for rework") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val completed = WorkflowEngine.completeStep(started, prepressId, now + 1000).toEither.toOption.get

        val result = WorkflowEngine.resetStep(completed, prepressId)
        val reset = result.toEither.toOption.get
        val resetStep = reset.steps.find(_.id == prepressId).get

        assertTrue(
          result.toEither.isRight,
          resetStep.status == StepStatus.Ready,
          resetStep.assignedTo.isEmpty,
          resetStep.startedAt.isEmpty,
          resetStep.completedAt.isEmpty,
          resetStep.isRework,
        )
      },
      test("resets a Failed step to Ready") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val failed = WorkflowEngine.failStep(started, prepressId, "bad artwork", now + 500).toEither.toOption.get

        assertTrue(failed.status == WorkflowStatus.OnHold)

        val result = WorkflowEngine.resetStep(failed, prepressId)
        val reset = result.toEither.toOption.get

        assertTrue(
          reset.steps.find(_.id == prepressId).get.status == StepStatus.Ready,
          reset.status == WorkflowStatus.InProgress, // OnHold → InProgress
        )
      },
      test("reverts downstream steps to Waiting when resetting a completed step") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id

        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        val completed = WorkflowEngine.completeStep(started, prepressId, now + 1000).toEither.toOption.get

        // Print step should be Ready now
        val printStep = stepByType(completed, StationType.DigitalPrinter)
        assertTrue(printStep.status == StepStatus.Ready)

        // Reset prepress → print step should revert to Waiting
        val reset = WorkflowEngine.resetStep(completed, prepressId).toEither.toOption.get
        val printAfterReset = stepByType(reset, StationType.DigitalPrinter)

        assertTrue(printAfterReset.status == StepStatus.Waiting)
      },
      test("cannot reset a Waiting step") {
        val wf = simpleWorkflow
        val printStep = stepByType(wf, StationType.DigitalPrinter)

        val result = WorkflowEngine.resetStep(wf, printStep.id)

        assertTrue(result.toEither.isLeft)
      },
      test("cannot reset an InProgress step") {
        val wf = simpleWorkflow
        val prepressId = stepByType(wf, StationType.Prepress).id
        val started = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get

        val result = WorkflowEngine.resetStep(started, prepressId)

        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("DAG constraint enforcement")(
      test("cannot start a step with unmet dependencies") {
        val wf = simpleWorkflow
        // DigitalPrinter depends on Prepress — Prepress is Ready but not Completed
        val printStep = stepByType(wf, StationType.DigitalPrinter)

        // Force the print step to Ready to bypass status check, but deps are unmet
        val hacked = wf
          .modify(_.steps.eachWhere(_.id == printStep.id).status).setTo(StepStatus.Ready)

        val result = WorkflowEngine.startStep(hacked, printStep.id, emp1, now)

        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("multi-component workflow")(
      test("binding step becomes Ready only when all component steps are complete") {
        var wf = bookletWorkflow

        val binderStep = stepByType(wf, StationType.Binder)
        assertTrue(binderStep.status == StepStatus.Waiting)

        // Complete prepress
        val prepressId = stepByType(wf, StationType.Prepress).id
        wf = WorkflowEngine.startStep(wf, prepressId, emp1, now).toEither.toOption.get
        wf = WorkflowEngine.completeStep(wf, prepressId, now + 100).toEither.toOption.get

        // Complete all component steps one by one
        var safetyCounter = 0
        while wf.steps.exists(s => s.status == StepStatus.Ready && s.stationType != StationType.Binder && s.stationType != StationType.QualityControl && s.stationType != StationType.Packaging) && safetyCounter < 20 do
          val readyStep = wf.steps.find(s => s.status == StepStatus.Ready && s.stationType != StationType.Binder && s.stationType != StationType.QualityControl && s.stationType != StationType.Packaging).get
          wf = WorkflowEngine.startStep(wf, readyStep.id, emp1, now + safetyCounter * 100).toEither.toOption.get
          wf = WorkflowEngine.completeStep(wf, readyStep.id, now + safetyCounter * 100 + 50).toEither.toOption.get
          safetyCounter += 1

        // Now the binder step should be Ready
        val binderAfter = stepByType(wf, StationType.Binder)
        assertTrue(binderAfter.status == StepStatus.Ready)
      },
    ),
    suite("error messages")(
      test("WorkflowError messages are defined for English and Czech") {
        val errors = List(
          WorkflowError.StepNotFound(StepId.unsafe("s1")),
          WorkflowError.StepNotReady(StepId.unsafe("s1"), StepStatus.Waiting),
          WorkflowError.StepNotInProgress(StepId.unsafe("s1"), StepStatus.Ready),
          WorkflowError.StepAlreadyCompleted(StepId.unsafe("s1")),
          WorkflowError.StepAlreadySkipped(StepId.unsafe("s1")),
          WorkflowError.DependenciesNotMet(StepId.unsafe("s1"), Set(StepId.unsafe("s2"))),
          WorkflowError.WorkflowNotActive(WorkflowId.unsafe("wf1"), WorkflowStatus.Completed),
          WorkflowError.StepCannotBeSkipped(StepId.unsafe("s1"), StationType.Prepress),
          WorkflowError.StepCannotBeReset(StepId.unsafe("s1"), StepStatus.Waiting),
        )

        assertTrue(
          errors.forall(_.message.nonEmpty),
          errors.forall(_.message(Language.En).nonEmpty),
          errors.forall(_.message(Language.Cs).nonEmpty),
        )
      },
    ),
  )
