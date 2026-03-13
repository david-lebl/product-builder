package mpbuilder.domain.service

import mpbuilder.domain.model.*
import zio.prelude.*

/** Pure service for managing machines — CRUD operations with validation. */
object MachineManagementService:

  /** Add a new machine to the registry. */
  def addMachine(
      machines: List[Machine],
      id: MachineId,
      name: String,
      stationType: StationType,
      status: MachineStatus = MachineStatus.Online,
      currentNotes: String = "",
  ): Validation[ManagementError, List[Machine]] =
    for
      _ <- validateMachineNotExists(machines, id)
      _ <- validateName(name)
    yield machines :+ Machine(id, name.trim, stationType, status, currentNotes.trim)

  /** Update an existing machine's name and notes. */
  def updateMachine(
      machines: List[Machine],
      id: MachineId,
      name: String,
      currentNotes: String,
  ): Validation[ManagementError, List[Machine]] =
    for
      _ <- validateMachineExists(machines, id)
      _ <- validateName(name)
    yield machines.map { m =>
      if m.id == id then m.copy(name = name.trim, currentNotes = currentNotes.trim) else m
    }

  /** Change a machine's operational status. */
  def changeStatus(
      machines: List[Machine],
      id: MachineId,
      newStatus: MachineStatus,
  ): Validation[ManagementError, List[Machine]] =
    for _ <- validateMachineExists(machines, id)
    yield machines.map { m =>
      if m.id == id then m.copy(status = newStatus) else m
    }

  /** Update a machine's station type. */
  def changeStationType(
      machines: List[Machine],
      id: MachineId,
      stationType: StationType,
  ): Validation[ManagementError, List[Machine]] =
    for _ <- validateMachineExists(machines, id)
    yield machines.map { m =>
      if m.id == id then m.copy(stationType = stationType) else m
    }

  /** Remove a machine from the registry. */
  def removeMachine(
      machines: List[Machine],
      id: MachineId,
  ): Validation[ManagementError, List[Machine]] =
    for _ <- validateMachineExists(machines, id)
    yield machines.filterNot(_.id == id)

  // --- Validation helpers ---

  private def validateMachineExists(machines: List[Machine], id: MachineId): Validation[ManagementError, Unit] =
    if machines.exists(_.id == id) then Validation.unit
    else Validation.fail(ManagementError.MachineNotFound(id))

  private def validateMachineNotExists(machines: List[Machine], id: MachineId): Validation[ManagementError, Unit] =
    if machines.exists(_.id == id) then Validation.fail(ManagementError.MachineAlreadyExists(id))
    else Validation.unit

  private def validateName(name: String): Validation[ManagementError, Unit] =
    if name.trim.nonEmpty then Validation.unit
    else Validation.fail(ManagementError.MachineNameEmpty)
