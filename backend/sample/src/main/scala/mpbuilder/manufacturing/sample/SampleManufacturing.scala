package mpbuilder.manufacturing.sample

import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.manufacturing.*
import mpbuilder.manufacturing.core.{ManufacturingServiceLive, ProductionJob, ProductionJobRepository, ShopResourceRepository}
import mpbuilder.shared.*
import zio.*
import java.time.{DayOfWeek, LocalTime}

/** Sample shop-floor resources, employees, machines, working hours, and unified
  * `SpeedAvailabilityPolicy` rules replicated from legacy `SampleManufacturing` + `SampleTierRestrictions`.
  */
object SampleManufacturing:

  import SampleCatalog.*

  // ── Employees ──────────────────────────────────────────────────────────────
  val empPrepressId: EmployeeId   = EmployeeId.unsafe("emp-1")
  val empDigitalId: EmployeeId    = EmployeeId.unsafe("emp-2")
  val empLargeFmtId: EmployeeId   = EmployeeId.unsafe("emp-3")
  val empFinishingId: EmployeeId  = EmployeeId.unsafe("emp-4")
  val empLeadAllId: EmployeeId    = EmployeeId.unsafe("emp-5")
  val empInactiveId: EmployeeId   = EmployeeId.unsafe("emp-inactive")

  val employees: List[Employee] = List(
    Employee(empPrepressId, NonEmptyString.unsafe("Jakub Dvořák (Prepress)"), Set(StationType.Prepress, StationType.QualityControl), isActive = true),
    Employee(empDigitalId, NonEmptyString.unsafe("Petra Nováková (Press Operator)"), Set(StationType.DigitalPrinter, StationType.OffsetPress, StationType.Letterpress), isActive = true),
    Employee(empLargeFmtId, NonEmptyString.unsafe("Martin Svoboda (Large Format)"), Set(StationType.LargeFormatPrinter, StationType.LargeFormatFinishing, StationType.Assembly), isActive = true),
    Employee(empFinishingId, NonEmptyString.unsafe("Klára Veselá (Finishing & Bindery)"), Set(StationType.Cutter, StationType.Laminator, StationType.UVCoater, StationType.EmbossingFoil, StationType.Folder, StationType.Binder, StationType.Packaging), isActive = true),
    Employee(empLeadAllId, NonEmptyString.unsafe("Tomáš Procházka (Shop Lead)"), StationType.values.toSet, isActive = true),
    Employee(empInactiveId, NonEmptyString.unsafe("Former Operator"), Set(StationType.DigitalPrinter), isActive = false),
  )

  // ── Machines ───────────────────────────────────────────────────────────────
  val machDigital1Id: MachineId     = MachineId.unsafe("mach-xerox-iridesse")
  val machOffset1Id: MachineId      = MachineId.unsafe("mach-heidelberg-sm52")
  val machLargeFmt1Id: MachineId    = MachineId.unsafe("mach-mimaki-jv300")
  val machUvFlatbedId: MachineId    = MachineId.unsafe("mach-swissqprint-nyala")
  val machLaminatorId: MachineId    = MachineId.unsafe("mach-komfi-delta")
  val machCutterId: MachineId       = MachineId.unsafe("mach-polar-78")
  val machFolderId: MachineId       = MachineId.unsafe("mach-mbo-t530")
  val machBinderId: MachineId       = MachineId.unsafe("mach-horizon-bq270")
  val machDtgId: MachineId          = MachineId.unsafe("mach-brother-gtx")
  val machScreenId: MachineId       = MachineId.unsafe("mach-mhm-synchro")
  val machMaintenanceId: MachineId  = MachineId.unsafe("mach-heidelberg-offline")

  val machines: List[Machine] = List(
    Machine(machDigital1Id, NonEmptyString.unsafe("Xerox Iridesse Production Press"), StationType.DigitalPrinter, MachineStatus.Online),
    Machine(machOffset1Id, NonEmptyString.unsafe("Heidelberg Speedmaster 52"), StationType.OffsetPress, MachineStatus.Online),
    Machine(machLargeFmt1Id, NonEmptyString.unsafe("Mimaki JV300-160 Solvent"), StationType.LargeFormatPrinter, MachineStatus.Online),
    Machine(machUvFlatbedId, NonEmptyString.unsafe("swissQprint Nyala UV"), StationType.LargeFormatPrinter, MachineStatus.Online),
    Machine(machLaminatorId, NonEmptyString.unsafe("Komfi Delta 52 Laminator"), StationType.Laminator, MachineStatus.Online),
    Machine(machCutterId, NonEmptyString.unsafe("Polar 78 Guillotine Cutter"), StationType.Cutter, MachineStatus.Online),
    Machine(machFolderId, NonEmptyString.unsafe("MBO T530 Folding Machine"), StationType.Folder, MachineStatus.Online),
    Machine(machBinderId, NonEmptyString.unsafe("Horizon BQ-270 Perfect Binder"), StationType.Binder, MachineStatus.Online),
    Machine(machDtgId, NonEmptyString.unsafe("Brother GTX Pro DTG"), StationType.GarmentPrinter, MachineStatus.Online),
    Machine(machScreenId, NonEmptyString.unsafe("MHM Synchroprint Screen Press"), StationType.ScreenPrinter, MachineStatus.Online),
    Machine(machMaintenanceId, NonEmptyString.unsafe("Heidelberg Windmill (Maintenance)"), StationType.Letterpress, MachineStatus.Maintenance, "Scheduled roller service"),
  )

  // ── Working Hours ──────────────────────────────────────────────────────────
  val workingHours: WorkingHours = WorkingHours(
    workingDays = Set(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
    openTime = LocalTime.of(8, 0),
    closeTime = LocalTime.of(17, 0),
  )

  // ── Unified Speed Availability Policies (from legacy SampleTierRestrictions) ─
  val speedPolicies: List[SpeedAvailabilityPolicy] = List(
    // Sheet & bound categories: Express max 2000 units, no custom embossing/foil rush dies
    SpeedAvailabilityPolicy(businessCardsId, ManufacturingSpeed.Express, maxQuantity = Some(2000), blockedFinishOps = Set(FinishingOperation.EmbossingOrFoil)),
    SpeedAvailabilityPolicy(flyersId, ManufacturingSpeed.Express, maxQuantity = Some(2000)),
    SpeedAvailabilityPolicy(brochuresId, ManufacturingSpeed.Express, maxQuantity = Some(2000)),
    SpeedAvailabilityPolicy(bookletsId, ManufacturingSpeed.Express, maxQuantity = Some(2000)),
    SpeedAvailabilityPolicy(postcardsId, ManufacturingSpeed.Express, maxQuantity = Some(2000)),
    SpeedAvailabilityPolicy(stickersId, ManufacturingSpeed.Express, maxQuantity = Some(2000)),
    SpeedAvailabilityPolicy(calendarsId, ManufacturingSpeed.Express, maxQuantity = Some(2000)),
    // Large format & packaging: Express max 500 units
    SpeedAvailabilityPolicy(bannersId, ManufacturingSpeed.Express, maxQuantity = Some(500)),
    SpeedAvailabilityPolicy(rollUpsId, ManufacturingSpeed.Express, maxQuantity = Some(500)),
    SpeedAvailabilityPolicy(packagingId, ManufacturingSpeed.Express, maxQuantity = Some(500)),
  )

final class InMemoryProductionJobRepository(
    ref: Ref[Map[JobId, ProductionJob]],
) extends ProductionJobRepository:

  override def findById(id: JobId): UIO[Option[ProductionJob]] =
    ref.get.map(_.get(id))

  override def listActive: UIO[List[ProductionJob]] =
    ref.get.map(_.values.filterNot(_.isAllWorkflowsCompleted).toList)

  override def save(job: ProductionJob): UIO[Unit] =
    ref.update(_ + (job.id -> job))

final class InMemoryShopResourceRepository(
    employeesRef: Ref[Map[EmployeeId, Employee]],
    machinesRef: Ref[Map[MachineId, Machine]],
    policiesRef: Ref[Map[(CategoryId, ManufacturingSpeed), SpeedAvailabilityPolicy]],
) extends ShopResourceRepository:

  override def findEmployee(id: EmployeeId): UIO[Option[Employee]] =
    employeesRef.get.map(_.get(id))

  override def findMachine(id: MachineId): UIO[Option[Machine]] =
    machinesRef.get.map(_.get(id))

  override def listMachines: UIO[List[Machine]] =
    machinesRef.get.map(_.values.toList)

  override def findSpeedPolicy(categoryId: CategoryId, speed: ManufacturingSpeed): UIO[Option[SpeedAvailabilityPolicy]] =
    policiesRef.get.map(_.get((categoryId, speed)))

object ManufacturingSampleLayer:

  val jobRepositoryLayer: ULayer[InMemoryProductionJobRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[JobId, ProductionJob]).map(new InMemoryProductionJobRepository(_)))

  def resourceRepositoryLayer(
      employees: List[Employee] = SampleManufacturing.employees,
      machines: List[Machine] = SampleManufacturing.machines,
      policies: List[SpeedAvailabilityPolicy] = SampleManufacturing.speedPolicies,
  ): ULayer[InMemoryShopResourceRepository] =
    ZLayer.fromZIO {
      for
        eRef <- Ref.make(employees.map(e => e.id -> e).toMap)
        mRef <- Ref.make(machines.map(m => m.id -> m).toMap)
        pRef <- Ref.make(policies.map(p => (p.categoryId, p.speed) -> p).toMap)
      yield new InMemoryShopResourceRepository(eRef, mRef, pRef)
    }

  val live: ZLayer[EventBus, Nothing, ManufacturingService] =
    (jobRepositoryLayer ++ resourceRepositoryLayer() ++ ZLayer.service[EventBus]) >>> ManufacturingServiceLive.layer
