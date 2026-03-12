package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*

object MachineManagementServiceSpec extends ZIOSpecDefault:

  private val machine1 = Machine(
    MachineId.unsafe("m-1"),
    "Konica Minolta C4080",
    StationType.DigitalPrinter,
    MachineStatus.Online,
    "CMYK calibrated, 300gsm loaded",
  )

  private val machine2 = Machine(
    MachineId.unsafe("m-2"),
    "Zünd G3 Cutter",
    StationType.Cutter,
    MachineStatus.Online,
    "",
  )

  private val machines = List(machine1, machine2)

  def spec = suite("MachineManagementService")(
    suite("addMachine")(
      test("adds a new machine to the registry") {
        val result = MachineManagementService.addMachine(
          machines,
          MachineId.unsafe("m-3"),
          "Heidelberg Speedmaster",
          StationType.OffsetPress,
        )
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == 3,
          updated.last.name == "Heidelberg Speedmaster",
          updated.last.stationType == StationType.OffsetPress,
          updated.last.status == MachineStatus.Online,
        )
      },
      test("fails if machine ID already exists") {
        val result = MachineManagementService.addMachine(
          machines,
          MachineId.unsafe("m-1"),
          "Duplicate",
          StationType.DigitalPrinter,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if name is empty") {
        val result = MachineManagementService.addMachine(
          machines,
          MachineId.unsafe("m-3"),
          "",
          StationType.Cutter,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("trims name and notes") {
        val result = MachineManagementService.addMachine(
          machines,
          MachineId.unsafe("m-3"),
          "  Trimmed  ",
          StationType.Cutter,
          currentNotes = "  Some note  ",
        )
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.last.name == "Trimmed",
          updated.last.currentNotes == "Some note",
        )
      },
    ),
    suite("updateMachine")(
      test("updates name and notes") {
        val result = MachineManagementService.updateMachine(
          machines,
          MachineId.unsafe("m-1"),
          "Updated Name",
          "New notes",
        )
        val updated = result.toEither.toOption.get
        val m = updated.find(_.id == MachineId.unsafe("m-1")).get
        assertTrue(m.name == "Updated Name", m.currentNotes == "New notes")
      },
      test("fails if machine not found") {
        val result = MachineManagementService.updateMachine(
          machines,
          MachineId.unsafe("unknown"),
          "Name",
          "",
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if new name is empty") {
        val result = MachineManagementService.updateMachine(
          machines,
          MachineId.unsafe("m-1"),
          "  ",
          "notes",
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("changeStatus")(
      test("changes machine status to Offline") {
        val result = MachineManagementService.changeStatus(
          machines,
          MachineId.unsafe("m-1"),
          MachineStatus.Offline,
        )
        val updated = result.toEither.toOption.get
        val m = updated.find(_.id == MachineId.unsafe("m-1")).get
        assertTrue(m.status == MachineStatus.Offline)
      },
      test("changes machine status to Maintenance") {
        val result = MachineManagementService.changeStatus(
          machines,
          MachineId.unsafe("m-1"),
          MachineStatus.Maintenance,
        )
        val updated = result.toEither.toOption.get
        val m = updated.find(_.id == MachineId.unsafe("m-1")).get
        assertTrue(m.status == MachineStatus.Maintenance)
      },
      test("fails if machine not found") {
        val result = MachineManagementService.changeStatus(
          machines,
          MachineId.unsafe("unknown"),
          MachineStatus.Offline,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("changeStationType")(
      test("changes machine station type") {
        val result = MachineManagementService.changeStationType(
          machines,
          MachineId.unsafe("m-2"),
          StationType.Laminator,
        )
        val updated = result.toEither.toOption.get
        val m = updated.find(_.id == MachineId.unsafe("m-2")).get
        assertTrue(m.stationType == StationType.Laminator)
      },
      test("fails if machine not found") {
        val result = MachineManagementService.changeStationType(
          machines,
          MachineId.unsafe("unknown"),
          StationType.Cutter,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("removeMachine")(
      test("removes a machine") {
        val result = MachineManagementService.removeMachine(machines, MachineId.unsafe("m-1"))
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == 1,
          updated.head.id == MachineId.unsafe("m-2"),
        )
      },
      test("fails if machine not found") {
        val result = MachineManagementService.removeMachine(machines, MachineId.unsafe("unknown"))
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("MachineStatus extensions")(
      test("display names are defined for all statuses") {
        assertTrue(
          MachineStatus.Online.displayName == "Online",
          MachineStatus.Offline.displayName == "Offline",
          MachineStatus.Maintenance.displayName == "Maintenance",
        )
      },
      test("icons are defined for all statuses") {
        assertTrue(
          MachineStatus.Online.icon.nonEmpty,
          MachineStatus.Offline.icon.nonEmpty,
          MachineStatus.Maintenance.icon.nonEmpty,
        )
      },
    ),
  )
