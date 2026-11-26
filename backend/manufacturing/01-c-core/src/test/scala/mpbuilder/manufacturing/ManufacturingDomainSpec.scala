package mpbuilder.manufacturing

import mpbuilder.manufacturing.core.*
import mpbuilder.shared.*
import kyo.*
import kyo.test.*
import java.time.Instant

class ManufacturingDomainSpec extends Test[Any]:

  private val now = Instant.parse("2026-03-10T10:00:00Z")

  private val rollupSpec = ProductionSpec(
    orderId = OrderId.unsafe("ord-rollup"),
    orderLineIndex = 0,
    categoryId = CategoryId.unsafe("rollups"),
    productLabel = "Roll-Up Banner 85×200",
    quantity = Quantity.unsafe(2),
    speed = ManufacturingSpeed.Express,
    components = NonEmptyChunk(
      ProductionComponentSpec(
        key = ComponentKey.Main,
        materialName = "Roll-Up PP Film 220g",
        printJob = Some(
          ComponentPrintJob(
            stationKind = PrintStationKind.LargeFormatInkjet,
            inkNotation = "4/0",
            sheetsToPrint = 0,
            requiresGuillotineCut = false,
          )
        ),
        finishingOperations = List(FinishingOperation.Lamination),
      ),
      ProductionComponentSpec(
        key = ComponentKey.Stand,
        materialName = "Aluminum Stand 85cm",
        printJob = None, // Unprinted hardware!
      ),
    ),
  )

  private val prepressEmp = Employee(
    id = EmployeeId.unsafe("emp-1"),
    name = NonEmptyString.unsafe("Anna"),
    stationCapabilities = Set(StationType.Prepress, StationType.LargeFormatPrinter, StationType.Assembly),
    isActive = true,
  )

  private val unqualifiedEmp = Employee(
    id = EmployeeId.unsafe("emp-2"),
    name = NonEmptyString.unsafe("Karel"),
    stationCapabilities = Set(StationType.Packaging),
    isActive = true,
  )

  private val onlineLfPrinter = Machine(
    id = MachineId.unsafe("m-lf-1"),
    name = NonEmptyString.unsafe("HP Latex 800"),
    stationType = StationType.LargeFormatPrinter,
    status = MachineStatus.Online,
  )

  private val offlineLfPrinter = onlineLfPrinter.copy(
    id = MachineId.unsafe("m-lf-2"),
    status = MachineStatus.Maintenance,
  )

  "ManufacturingDomainSpec" - {
    "Roll-Up workflow routes printed Main through Prepress/Print/Laminate and unprinted Stand directly to Assembly (NO Print/Cut!)" in {
      val wf = WorkflowGenerator.generate(rollupSpec, WorkflowId.unsafe("wf-rollup"), None, now)
      val mainSteps  = wf.steps.toList.filter(_.componentKey.contains(ComponentKey.Main))
      val standSteps = wf.steps.toList.filter(_.componentKey.contains(ComponentKey.Stand))

      assert(wf.priority == Priority.Rush)
      assert(mainSteps.map(_.stationType) == List(StationType.LargeFormatPrinter, StationType.Laminator))
      assert(standSteps.map(_.stationType) == List(StationType.Assembly))
      // Stand Assembly has zero print dependency and is immediately Ready!
      assert(standSteps.head.status == StepStatus.Ready)
      assert(standSteps.head.dependsOn.isEmpty)
    }

    "customer production instructions are surfaced on the Prepress step" in {
      val wf      = WorkflowGenerator.generate(rollupSpec.copy(customerInstructions = Some("Logo side is the front")), WorkflowId.unsafe("wf-note"), None, now)
      val plainWf = WorkflowGenerator.generate(rollupSpec, WorkflowId.unsafe("wf-plain"), None, now)
      val notes   = wf.steps.find(_.stationType == StationType.Prepress).get.notes
      assert(notes.contains("Customer instructions: Logo side is the front"))
      assert(!plainWf.steps.find(_.stationType == StationType.Prepress).get.notes.contains("Customer instructions"))
    }

    "WorkflowEngine validates employee capability, machine status, unlocks downstream steps, and re-wires DAG on rework" in {
      val wf = WorkflowGenerator.generate(rollupSpec, WorkflowId.unsafe("wf-dag"), None, now)
      val prepressStep = wf.steps.find(_.stationType == StationType.Prepress).get
      val printStep    = wf.steps.find(_.stationType == StationType.LargeFormatPrinter).get
      val lamStep      = wf.steps.find(_.stationType == StationType.Laminator).get

      // 1. Unqualified employee rejected
      val badEmp = WorkflowEngine.claimStep(wf, prepressStep.id, unqualifiedEmp, None, now)
      // 2. Claim & complete Prepress -> unlocks Print step
      val wf1 = WorkflowEngine.claimStep(wf, prepressStep.id, prepressEmp, None, now).toOption.get
      val wf2 = WorkflowEngine.completeStep(wf1, prepressStep.id, "Preflight OK", now).toOption.get
      val unlockedPrint = wf2.steps.find(_.id == printStep.id).get
      // 3. Machine in Maintenance rejected
      val badMach = WorkflowEngine.claimStep(wf2, printStep.id, prepressEmp, Some(offlineLfPrinter), now)
      // 4. Claim print with Online machine, then fail and schedule rework
      val wf3 = WorkflowEngine.claimStep(wf2, printStep.id, prepressEmp, Some(onlineLfPrinter), now).toOption.get
      val wf4 = WorkflowEngine.failAndScheduleRework(wf3, printStep.id, NonEmptyString.unsafe("Head strike banding")).toOption.get
      val reworkStep = wf4.steps.find(_.isRework).get
      val updatedLam = wf4.steps.find(_.id == lamStep.id).get

      assert(badEmp == Left(ManufacturingError.EmployeeNotCapable(unqualifiedEmp.id, StationType.Prepress)))
      assert(unlockedPrint.status == StepStatus.Ready)
      assert(badMach == Left(ManufacturingError.MachineUnavailable(offlineLfPrinter.id, MachineStatus.Maintenance)))
      assert(wf4.status == WorkflowStatus.OnHold)
      assert(wf4.steps.find(_.id == printStep.id).map(_.status) == Some(StepStatus.Failed))
      assert(reworkStep.status == StepStatus.Ready)
      assert(reworkStep.stationType == StationType.LargeFormatPrinter)
      // Laminator now depends on reworkStep instead of the failed printStep!
      assert(updatedLam.dependsOn == Set(reworkStep.id))
    }

    "ManufacturingServiceLive computes CapacitySnapshot and enforces unified SpeedAvailabilityPolicy" in {
      val catId = CategoryId.unsafe("booklets")
      val expressPolicy = SpeedAvailabilityPolicy(
        categoryId = catId,
        speed = ManufacturingSpeed.Express,
        enabled = true,
        maxQuantity = Some(200),
        blockedFinishOps = Set(FinishingOperation.EmbossingOrFoil),
      )

      val resources = new ShopResourceRepository:
        override def findEmployee(id: EmployeeId): Option[Employee] < Async =
          Option.when(id == prepressEmp.id)(prepressEmp)
        override def findMachine(id: MachineId): Option[Machine] < Async =
          Option.when(id == onlineLfPrinter.id)(onlineLfPrinter)
        override def listMachines: List[Machine] < Async =
          List(onlineLfPrinter)
        override def findSpeedPolicy(c: CategoryId, s: ManufacturingSpeed): Option[SpeedAvailabilityPolicy] < Async =
          Option.when(c == catId && s == ManufacturingSpeed.Express)(expressPolicy)

      val makeService: ManufacturingService < Sync =
        for
          bus <- EventBus.init
          ref <- AtomicRef.init(Map.empty[JobId, ProductionJob])
          jobs = new ProductionJobRepository:
                   override def findById(id: JobId): Option[ProductionJob] < Async = ref.get.map(_.get(id))
                   override def listActive: List[ProductionJob] < Async            = ref.get.map(_.values.toList)
                   override def save(job: ProductionJob): Unit < Async             = ref.update(_ + (job.id -> job))
        yield ManufacturingServiceLive.make(jobs, resources, bus)

      for
        svc       <- makeService
        _         <- svc.createJob(JobId.unsafe("job-1"), NonEmptyChunk.single(rollupSpec))
        cap       <- svc.currentCapacity
        okExpress <- svc.checkSpeedAvailability(catId, ManufacturingSpeed.Express, Quantity.unsafe(100), 2, Set.empty, Set.empty).either
        tooMany   <- svc.checkSpeedAvailability(catId, ManufacturingSpeed.Express, Quantity.unsafe(500), 2, Set.empty, Set.empty).either
        badFinish <- svc.checkSpeedAvailability(catId, ManufacturingSpeed.Express, Quantity.unsafe(100), 2, Set.empty, Set(FinishingOperation.EmbossingOrFoil)).either
      yield {
        assert(cap.byStation.contains(StationType.LargeFormatPrinter))
        assert(okExpress.isRight)
        assert(tooMany.isLeft)
        assert(badFinish.isLeft)
      }
    }
  }
