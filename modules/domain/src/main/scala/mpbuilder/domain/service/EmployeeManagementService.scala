package mpbuilder.domain.service

import mpbuilder.domain.model.*
import zio.prelude.*

/** Pure service for managing employees — CRUD operations with validation. */
object EmployeeManagementService:

  /** Add a new employee to the roster. */
  def addEmployee(
      employees: List[Employee],
      id: EmployeeId,
      name: String,
      stationCapabilities: Set[StationType],
      isActive: Boolean = true,
  ): Validation[ManagementError, List[Employee]] =
    for
      _ <- validateEmployeeNotExists(employees, id)
      _ <- validateName(name)
      _ <- validateCapabilities(stationCapabilities)
    yield employees :+ Employee(id, name.trim, stationCapabilities, isActive)

  /** Update an existing employee's name and active status. */
  def updateEmployee(
      employees: List[Employee],
      id: EmployeeId,
      name: String,
      isActive: Boolean,
  ): Validation[ManagementError, List[Employee]] =
    for
      _ <- validateEmployeeExists(employees, id)
      _ <- validateName(name)
    yield employees.map { e =>
      if e.id == id then e.copy(name = name.trim, isActive = isActive) else e
    }

  /** Update an employee's station capabilities. */
  def updateCapabilities(
      employees: List[Employee],
      id: EmployeeId,
      stationCapabilities: Set[StationType],
  ): Validation[ManagementError, List[Employee]] =
    for
      _ <- validateEmployeeExists(employees, id)
      _ <- validateCapabilities(stationCapabilities)
    yield employees.map { e =>
      if e.id == id then e.copy(stationCapabilities = stationCapabilities) else e
    }

  /** Toggle an employee's active status. */
  def toggleActive(
      employees: List[Employee],
      id: EmployeeId,
  ): Validation[ManagementError, List[Employee]] =
    for _ <- validateEmployeeExists(employees, id)
    yield employees.map { e =>
      if e.id == id then e.copy(isActive = !e.isActive) else e
    }

  /** Remove an employee from the roster. */
  def removeEmployee(
      employees: List[Employee],
      id: EmployeeId,
  ): Validation[ManagementError, List[Employee]] =
    for _ <- validateEmployeeExists(employees, id)
    yield employees.filterNot(_.id == id)

  // --- Validation helpers ---

  private def validateEmployeeExists(employees: List[Employee], id: EmployeeId): Validation[ManagementError, Unit] =
    if employees.exists(_.id == id) then Validation.unit
    else Validation.fail(ManagementError.EmployeeNotFound(id))

  private def validateEmployeeNotExists(employees: List[Employee], id: EmployeeId): Validation[ManagementError, Unit] =
    if employees.exists(_.id == id) then Validation.fail(ManagementError.EmployeeAlreadyExists(id))
    else Validation.unit

  private def validateName(name: String): Validation[ManagementError, Unit] =
    if name.trim.nonEmpty then Validation.unit
    else Validation.fail(ManagementError.EmployeeNameEmpty)

  private def validateCapabilities(caps: Set[StationType]): Validation[ManagementError, Unit] =
    if caps.nonEmpty then Validation.unit
    else Validation.fail(ManagementError.EmployeeNoCapabilities)
