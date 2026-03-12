package mpbuilder.ui.manufacturing

import mpbuilder.domain.model.*

/** Route definitions for the manufacturing UI. */
enum ManufacturingRoute(val label: String, val icon: String, val isAvailable: Boolean = true):
  case Dashboard      extends ManufacturingRoute("Dashboard", "📊")
  case StationQueue   extends ManufacturingRoute("Station Queue", "🏭")
  case OrderApproval  extends ManufacturingRoute("Order Approval", "📋")
  case OrderProgress  extends ManufacturingRoute("Order Progress", "📦")
  case Employees      extends ManufacturingRoute("Employees", "👥", isAvailable = false)

/** Summary card data for the dashboard. */
final case class DashboardSummary(
    awaitingApproval: Int,
    inProduction: Int,
    readyForDispatch: Int,
    overdue: Int,
    todaysCompletions: Int,
)

/** Station status for the dashboard status strip. */
final case class StationStatus(
    stationType: StationType,
    queueDepth: Int,
    hasInProgress: Boolean,
)

/** Deadline filter options. */
enum DeadlineFilter:
  case All, Today, Tomorrow, ThisWeek, Overdue

/** Filter state for the station queue view. */
final case class StationQueueFilters(
    stationTypes: Set[StationType],
    statuses: Set[StepStatus],
    priorities: Set[Priority],
    deadlineFilter: DeadlineFilter,
)

object StationQueueFilters:
  val default: StationQueueFilters = StationQueueFilters(
    stationTypes = StationType.values.toSet,
    statuses = Set(StepStatus.Ready, StepStatus.InProgress),
    priorities = Priority.values.toSet,
    deadlineFilter = DeadlineFilter.All,
  )

/** Filter state for order approval view. */
final case class ApprovalFilters(
    statuses: Set[ApprovalStatus],
)

object ApprovalFilters:
  val default: ApprovalFilters = ApprovalFilters(
    statuses = Set(ApprovalStatus.Placed, ApprovalStatus.PendingChanges),
  )

/** Filter state for order progress view. */
final case class ProgressFilters(
    statuses: Set[WorkflowStatus],
    priorities: Set[Priority],
)

object ProgressFilters:
  val default: ProgressFilters = ProgressFilters(
    statuses = Set(WorkflowStatus.InProgress, WorkflowStatus.Pending),
    priorities = Priority.values.toSet,
  )

/** A queue item representing a step ready for pickup in the station queue. */
final case class QueueItem(
    step: WorkflowStep,
    workflow: ManufacturingWorkflow,
    order: ManufacturingOrder,
)
