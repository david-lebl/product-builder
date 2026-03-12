package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingWorkflow.*
import mpbuilder.domain.model.ManufacturingOrder.*

/** Pure analytics calculations from manufacturing data. */
object AnalyticsService:

  /** Average time (in milliseconds) per station type for completed steps. */
  def averageTimePerStation(orders: List[ManufacturingOrder]): Map[StationType, Long] =
    val completedSteps = orders
      .filter(_.approvalStatus == ApprovalStatus.Approved)
      .flatMap(_.workflows)
      .flatMap(_.steps)
      .filter(s => s.status == StepStatus.Completed && s.startedAt.isDefined && s.completedAt.isDefined)

    completedSteps
      .groupBy(_.stationType)
      .map { case (st, steps) =>
        val durations = steps.flatMap(s => for { start <- s.startedAt; end <- s.completedAt } yield end - start)
        st -> (if durations.nonEmpty then durations.sum / durations.size else 0L)
      }

  /** Station with the longest queue (most Ready steps). Returns (StationType, queueDepth). */
  def bottleneckStation(orders: List[ManufacturingOrder]): Option[(StationType, Int)] =
    val readySteps = orders
      .filter(_.approvalStatus == ApprovalStatus.Approved)
      .flatMap(_.workflows)
      .flatMap(_.steps)
      .filter(_.status == StepStatus.Ready)

    val grouped = readySteps.groupBy(_.stationType).map { case (st, steps) => (st, steps.size) }
    if grouped.isEmpty then None
    else Some(grouped.maxBy(_._2))

  /** Number of completed steps per employee. */
  def employeeThroughput(orders: List[ManufacturingOrder]): Map[EmployeeId, Int] =
    orders
      .filter(_.approvalStatus == ApprovalStatus.Approved)
      .flatMap(_.workflows)
      .flatMap(_.steps)
      .filter(s => s.status == StepStatus.Completed && s.assignedTo.isDefined)
      .groupBy(_.assignedTo.get)
      .map { case (empId, steps) => empId -> steps.size }

  /** On-time delivery rate: fraction of completed orders that met their deadline. */
  def onTimeDeliveryRate(orders: List[ManufacturingOrder]): Double =
    val completedOrders = orders.filter(o =>
      o.approvalStatus == ApprovalStatus.Approved && o.overallStatus == WorkflowStatus.Completed
    )
    if completedOrders.isEmpty then 1.0
    else
      val onTime = completedOrders.count { mo =>
        mo.deadline match
          case None => true // no deadline means always on time
          case Some(dl) =>
            // Check if the last step was completed before the deadline
            val lastCompletion = mo.workflows.flatMap(_.steps).flatMap(_.completedAt).maxOption
            lastCompletion.exists(_ <= dl)
      }
      onTime.toDouble / completedOrders.size

  /** Summary statistics for the analytics view. */
  final case class AnalyticsSummary(
      totalOrders: Int,
      completedOrders: Int,
      inProgressOrders: Int,
      avgCompletionTimeMs: Long,
      onTimeRate: Double,
      bottleneck: Option[(StationType, Int)],
      stationMetrics: List[StationMetric],
      employeeMetrics: List[EmployeeMetric],
  )

  /** Per-station performance metric. */
  final case class StationMetric(
      stationType: StationType,
      completedSteps: Int,
      avgTimeMs: Long,
      currentQueueDepth: Int,
      inProgress: Int,
  )

  /** Per-employee performance metric. */
  final case class EmployeeMetric(
      employeeId: EmployeeId,
      employeeName: String,
      completedSteps: Int,
      stationsWorked: Set[StationType],
  )

  /** Compute full analytics summary from orders and employee data. */
  def computeSummary(orders: List[ManufacturingOrder], employees: List[Employee]): AnalyticsSummary =
    val approvedOrders = orders.filter(_.approvalStatus == ApprovalStatus.Approved)
    val allSteps = approvedOrders.flatMap(_.workflows).flatMap(_.steps)
    val completedSteps = allSteps.filter(s => s.status == StepStatus.Completed && s.startedAt.isDefined && s.completedAt.isDefined)
    val avgTimeMap = averageTimePerStation(orders)
    val throughput = employeeThroughput(orders)

    val stationMetrics = StationType.values.toList.map { st =>
      val stepsForStation = allSteps.filter(_.stationType == st)
      StationMetric(
        stationType = st,
        completedSteps = stepsForStation.count(_.status == StepStatus.Completed),
        avgTimeMs = avgTimeMap.getOrElse(st, 0L),
        currentQueueDepth = stepsForStation.count(_.status == StepStatus.Ready),
        inProgress = stepsForStation.count(_.status == StepStatus.InProgress),
      )
    }.filter(m => m.completedSteps > 0 || m.currentQueueDepth > 0 || m.inProgress > 0)

    val employeeMetrics = employees.map { emp =>
      val empCompletedSteps = completedSteps.filter(_.assignedTo.contains(emp.id))
      EmployeeMetric(
        employeeId = emp.id,
        employeeName = emp.name,
        completedSteps = throughput.getOrElse(emp.id, 0),
        stationsWorked = empCompletedSteps.map(_.stationType).toSet,
      )
    }

    val avgCompletionTimeMs =
      val durations = completedSteps.flatMap(s => for { start <- s.startedAt; end <- s.completedAt } yield end - start)
      if durations.isEmpty then 0L else durations.sum / durations.size

    AnalyticsSummary(
      totalOrders = approvedOrders.size,
      completedOrders = approvedOrders.count(_.overallStatus == WorkflowStatus.Completed),
      inProgressOrders = approvedOrders.count(_.overallStatus == WorkflowStatus.InProgress),
      avgCompletionTimeMs = avgCompletionTimeMs,
      onTimeRate = onTimeDeliveryRate(orders),
      bottleneck = bottleneckStation(orders),
      stationMetrics = stationMetrics,
      employeeMetrics = employeeMetrics,
    )
