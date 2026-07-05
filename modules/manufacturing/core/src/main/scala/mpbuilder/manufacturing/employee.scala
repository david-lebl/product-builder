package mpbuilder.manufacturing

import mpbuilder.kernel.*

/** An employee in the manufacturing system */
final case class Employee(
    id: EmployeeId,
    name: String,
    stationCapabilities: Set[StationType],
    isActive: Boolean,
)
