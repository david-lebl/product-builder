package mpbuilder.manufacturing

import zio.prelude.*

opaque type MachineId = String
object MachineId:
  def apply(value: String): Validation[String, MachineId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("MachineId must not be empty")

  def unsafe(value: String): MachineId = value

  extension (id: MachineId) def value: String = id

/** Machine status */
enum MachineStatus:
  case Online, Offline, Maintenance

object MachineStatus:
  extension (ms: MachineStatus) def displayName: String = ms match
    case Online      => "Online"
    case Offline     => "Offline"
    case Maintenance => "Maintenance"

  extension (ms: MachineStatus) def icon: String = ms match
    case Online      => "🟢"
    case Offline     => "🔴"
    case Maintenance => "🟡"

/** A registered machine */
final case class Machine(
    id: MachineId,
    name: String,
    stationType: StationType,
    status: MachineStatus,
    currentNotes: String,
)
