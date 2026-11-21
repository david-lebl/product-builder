package mpbuilder.manufacturing

import mpbuilder.manufacturing.core.*
import mpbuilder.shared.*
import zio.*
import zio.test.*
import java.time.Instant

object ManufacturingDomainSpec extends ZIOSpecDefault:

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

  def spec = suite("ManufacturingDomainSpec")(
    test("Roll-Up workflow routes printed Main through Prepress/Print/Laminate and unprinted Stand directly to Assembly (NO Print/Cut!)") {
      val wf = WorkflowGenerator.generate(rollupSpec, WorkflowId.unsafe("wf-rollup"), None, now)
      val mainSteps  = wf.steps.toList.filter(_.componentKey.contains(ComponentKey.Main))
      val standSteps = wf.steps.toList.filter(_.componentKey.contains(ComponentKey.Stand))

      assertTrue(
        wf.priority == Priority.Rush,
        mainSteps.map(_.stationType) == List(StationType.LargeFormatPrinter, StationType.Laminator),
        standSteps.map(_.stationType) == List(StationType.Assembly),
        // Stand Assembly has zero print dependency and is immediately Ready!
        standSteps.head.status == StepStatus.Ready,
        standSteps.head.dependsOn.isEmpty,
      )
    },
    test("WorkflowEngine validates employee capability, machine status, unlocks downstream steps, and re-wires DAG on rework") {
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

      assertTrue(
        badEmp == Left(ManufacturingError.EmployeeNotCapable(unqualifiedEmp.id, StationType.Prepress)),
        unlockedPrint.status == StepStatus.Ready,
        badMach == Left(ManufacturingError.MachineUnavailable(offlineLfPrinter.id, MachineStatus.Maintenance)),
        wf4.status == WorkflowStatus.OnHold,
        wf4.steps.find(_.id == printStep.id).map(_.status) == Some(StepStatus.Failed),
        reworkStep.status == StepStatus.Ready,
        reworkStep.stationType == StationType.LargeFormatPrinter,
        // Laminator now depends on reworkStep instead of the failed printStep!
        updatedLam.dependsOn == Set(reworkStep.id),
      )
    },
    test("ManufacturingServiceLive computes CapacitySnapshot and enforces unified SpeedAvailabilityPolicy") {
      val catId = CategoryId.unsafe("booklets")
      val expressPolicy = SpeedAvailabilityPolicy(
        categoryId = catId,
        speed = ManufacturingSpeed.Express,
        enabled = true,
        maxQuantity = Some(200),
        blockedFinishOps = Set(FinishingOperation.EmbossingOrFoil),
      )

      val layer: ULayer[ManufacturingService] = ZLayer.make[ManufacturingService](
        EventBus.live,
        ZLayer.fromZIO {
          Ref.make(Map.empty[JobId, ProductionJob]).map { ref =>
            new ProductionJobRepository:
              override def findById(id: JobId): UIO[Option[ProductionJob]] = ref.get.map(_.get(id))
              override def listActive: UIO[List[ProductionJob]]            = ref.get.map(_.values.toList)
              override def save(job: ProductionJob): UIO[Unit]             = ref.update(_ + (job.id -> job))
          }
        },
        ZLayer.succeed(new ShopResourceRepository:
          override def findEmployee(id: EmployeeId): UIO[Option[Employee]] =
            ZIO.succeed(Option.when(id == prepressEmp.id)(prepressEmp))
          override def findMachine(id: MachineId): UIO[Option[Machine]] =
            ZIO.succeed(Option.when(id == onlineLfPrinter.id)(onlineLfPrinter))
          override def listMachines: UIO[List[Machine]] =
            ZIO.succeed(List(onlineLfPrinter))
          override def findSpeedPolicy(c: CategoryId, s: ManufacturingSpeed): UIO[Option[SpeedAvailabilityPolicy]] =
            ZIO.succeed(Option.when(c == catId && s == ManufacturingSpeed.Express)(expressPolicy))
        ),
        ManufacturingServiceLive.layer,
      )

      (for
        svc       <- ZIO.service[ManufacturingService]
        _         <- svc.createJob(JobId.unsafe("job-1"), NonEmptyChunk.single(rollupSpec))
        cap       <- svc.currentCapacity
        okExpress <- svc.checkSpeedAvailability(catId, ManufacturingSpeed.Express, Quantity.unsafe(100), 2, Set.empty, Set.empty).either
        tooMany   <- svc.checkSpeedAvailability(catId, ManufacturingSpeed.Express, Quantity.unsafe(500), 2, Set.empty, Set.empty).either
        badFinish <- svc.checkSpeedAvailability(catId, ManufacturingSpeed.Express, Quantity.unsafe(100), 2, Set.empty, Set(FinishingOperation.EmbossingOrFoil)).either
      yield assertTrue(
        cap.byStation.contains(StationType.LargeFormatPrinter),
        okExpress.isRight,
        tooMany.isLeft,
        badFinish.isLeft,
      )).provide(layer)
    },
  )
