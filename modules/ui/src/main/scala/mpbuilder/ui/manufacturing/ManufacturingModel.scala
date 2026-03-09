package mpbuilder.ui.manufacturing

import mpbuilder.domain.model.*

/** Routes for manufacturing sidebar navigation */
enum ManufacturingRoute:
  case Dashboard
  case OrderQueue
  case WorkQueue
  case OrderProgress
  case Employees

object ManufacturingRoute:
  extension (r: ManufacturingRoute)
    def label: String = r match
      case Dashboard     => "Dashboard"
      case OrderQueue    => "Order Approval"
      case WorkQueue     => "Work Queue"
      case OrderProgress => "Order Progress"
      case Employees     => "Employees"

    def icon: String = r match
      case Dashboard     => "📊"
      case OrderQueue    => "📋"
      case WorkQueue     => "🔧"
      case OrderProgress => "📈"
      case Employees     => "👥"

/** Column sort direction */
enum SortDirection:
  case Asc, Desc

/** Which column is being sorted in work queue */
enum WorkQueueSortColumn:
  case Id, Order, Customer, Product, Material, Status, Priority, CreatedAt, Deadline

/** Aggregate state for the manufacturing UI */
final case class ManufacturingState(
    orders: List[ManufacturingOrder],
    employees: List[Employee],
    selectedOrderId: Option[OrderId],
    selectedItemIndex: Option[Int],
    selectedRoute: ManufacturingRoute,
    stationFilter: Set[StationType],
    searchQuery: String,
    currentEmployeeId: Option[EmployeeId],
    sortColumn: Option[WorkQueueSortColumn],
    sortDirection: SortDirection,
)

object ManufacturingState:
  val empty: ManufacturingState = ManufacturingState(
    orders = Nil,
    employees = Nil,
    selectedOrderId = None,
    selectedItemIndex = None,
    selectedRoute = ManufacturingRoute.Dashboard,
    stationFilter = Set.empty,
    searchQuery = "",
    currentEmployeeId = None,
    sortColumn = None,
    sortDirection = SortDirection.Asc,
  )
