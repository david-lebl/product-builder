package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*

object EmployeeManagementServiceSpec extends ZIOSpecDefault:

  private val emp1 = Employee(
    EmployeeId.unsafe("emp-1"),
    "Jan Novák",
    Set(StationType.DigitalPrinter, StationType.Cutter),
    isActive = true,
  )

  private val emp2 = Employee(
    EmployeeId.unsafe("emp-2"),
    "Marie Svobodová",
    Set(StationType.Prepress, StationType.QualityControl),
    isActive = true,
  )

  private val employees = List(emp1, emp2)

  def spec = suite("EmployeeManagementService")(
    suite("addEmployee")(
      test("adds a new employee to the roster") {
        val result = EmployeeManagementService.addEmployee(
          employees,
          EmployeeId.unsafe("emp-3"),
          "Petr Dvořák",
          Set(StationType.Laminator, StationType.Folder),
        )
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == 3,
          updated.last.name == "Petr Dvořák",
          updated.last.stationCapabilities == Set(StationType.Laminator, StationType.Folder),
          updated.last.isActive,
        )
      },
      test("fails if employee ID already exists") {
        val result = EmployeeManagementService.addEmployee(
          employees,
          EmployeeId.unsafe("emp-1"),
          "Duplicate",
          Set(StationType.Cutter),
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if name is empty") {
        val result = EmployeeManagementService.addEmployee(
          employees,
          EmployeeId.unsafe("emp-3"),
          "  ",
          Set(StationType.Cutter),
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if capabilities are empty") {
        val result = EmployeeManagementService.addEmployee(
          employees,
          EmployeeId.unsafe("emp-3"),
          "Valid Name",
          Set.empty,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("trims the employee name") {
        val result = EmployeeManagementService.addEmployee(
          employees,
          EmployeeId.unsafe("emp-3"),
          "  Trimmed Name  ",
          Set(StationType.Cutter),
        )
        val updated = result.toEither.toOption.get
        assertTrue(updated.last.name == "Trimmed Name")
      },
    ),
    suite("updateEmployee")(
      test("updates name and active status") {
        val result = EmployeeManagementService.updateEmployee(
          employees,
          EmployeeId.unsafe("emp-1"),
          "Jan Updated",
          isActive = false,
        )
        val updated = result.toEither.toOption.get
        val emp = updated.find(_.id == EmployeeId.unsafe("emp-1")).get
        assertTrue(emp.name == "Jan Updated", !emp.isActive)
      },
      test("fails if employee not found") {
        val result = EmployeeManagementService.updateEmployee(
          employees,
          EmployeeId.unsafe("unknown"),
          "Name",
          isActive = true,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if new name is empty") {
        val result = EmployeeManagementService.updateEmployee(
          employees,
          EmployeeId.unsafe("emp-1"),
          "",
          isActive = true,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("updateCapabilities")(
      test("updates station capabilities") {
        val newCaps = Set(StationType.Prepress, StationType.Packaging)
        val result = EmployeeManagementService.updateCapabilities(
          employees,
          EmployeeId.unsafe("emp-1"),
          newCaps,
        )
        val updated = result.toEither.toOption.get
        val emp = updated.find(_.id == EmployeeId.unsafe("emp-1")).get
        assertTrue(emp.stationCapabilities == newCaps)
      },
      test("fails if capabilities are empty") {
        val result = EmployeeManagementService.updateCapabilities(
          employees,
          EmployeeId.unsafe("emp-1"),
          Set.empty,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if employee not found") {
        val result = EmployeeManagementService.updateCapabilities(
          employees,
          EmployeeId.unsafe("unknown"),
          Set(StationType.Cutter),
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("toggleActive")(
      test("toggles active to inactive") {
        val result = EmployeeManagementService.toggleActive(employees, EmployeeId.unsafe("emp-1"))
        val updated = result.toEither.toOption.get
        val emp = updated.find(_.id == EmployeeId.unsafe("emp-1")).get
        assertTrue(!emp.isActive)
      },
      test("toggles inactive back to active") {
        val inactive = employees.map(e => if e.id == EmployeeId.unsafe("emp-1") then e.copy(isActive = false) else e)
        val result = EmployeeManagementService.toggleActive(inactive, EmployeeId.unsafe("emp-1"))
        val updated = result.toEither.toOption.get
        val emp = updated.find(_.id == EmployeeId.unsafe("emp-1")).get
        assertTrue(emp.isActive)
      },
      test("fails if employee not found") {
        val result = EmployeeManagementService.toggleActive(employees, EmployeeId.unsafe("unknown"))
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("removeEmployee")(
      test("removes an employee") {
        val result = EmployeeManagementService.removeEmployee(employees, EmployeeId.unsafe("emp-1"))
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == 1,
          updated.head.id == EmployeeId.unsafe("emp-2"),
        )
      },
      test("fails if employee not found") {
        val result = EmployeeManagementService.removeEmployee(employees, EmployeeId.unsafe("unknown"))
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("error messages")(
      test("all errors have English and Czech messages") {
        val errors: List[ManagementError] = List(
          ManagementError.EmployeeNotFound(EmployeeId.unsafe("x")),
          ManagementError.EmployeeNameEmpty,
          ManagementError.EmployeeAlreadyExists(EmployeeId.unsafe("x")),
          ManagementError.EmployeeNoCapabilities,
          ManagementError.MachineNotFound(MachineId.unsafe("x")),
          ManagementError.MachineNameEmpty,
          ManagementError.MachineAlreadyExists(MachineId.unsafe("x")),
        )
        assertTrue(errors.forall { e =>
          e.message(Language.En).nonEmpty && e.message(Language.Cs).nonEmpty
        })
      },
    ),
  )
