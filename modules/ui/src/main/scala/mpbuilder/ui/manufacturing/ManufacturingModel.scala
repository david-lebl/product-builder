package mpbuilder.ui.manufacturing

import mpbuilder.domain.model.*

/** Routes for manufacturing sidebar navigation */
enum ManufacturingRoute:
  case OrderQueue
  case WorkQueue
  case OrderProgress
  case Employees

object ManufacturingRoute:
  extension (r: ManufacturingRoute)
    def label: String = r match
      case OrderQueue   => "Order Approval"
      case WorkQueue    => "Work Queue"
      case OrderProgress => "Order Progress"
      case Employees    => "Employees"

    def icon: String = r match
      case OrderQueue   => "📋"
      case WorkQueue    => "🔧"
      case OrderProgress => "📊"
      case Employees    => "👥"

/** Aggregate state for the manufacturing UI */
final case class ManufacturingState(
    orders: List[ManufacturingOrder],
    employees: List[Employee],
    selectedOrderId: Option[OrderId],
    selectedRoute: ManufacturingRoute,
    stationFilter: Option[StationType],
    currentEmployeeId: Option[EmployeeId],
)

object ManufacturingState:
  val empty: ManufacturingState = ManufacturingState(
    orders = Nil,
    employees = Nil,
    selectedOrderId = None,
    selectedRoute = ManufacturingRoute.OrderQueue,
    stationFilter = None,
    currentEmployeeId = None,
  )
