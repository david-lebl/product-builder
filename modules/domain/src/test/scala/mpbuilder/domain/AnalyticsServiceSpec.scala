package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*

object AnalyticsServiceSpec extends ZIOSpecDefault:

  private def makeStep(
      st: StationType,
      status: StepStatus,
      assignedTo: Option[EmployeeId] = None,
      startedAt: Option[Long] = None,
      completedAt: Option[Long] = None,
  ): WorkflowStep = WorkflowStep(
    StepId.unsafe(s"step-${st.toString}-${status.toString}"),
    st, None, Set.empty, status, assignedTo, None, startedAt, completedAt, "",
  )

  private def makeWorkflow(steps: List[WorkflowStep], status: WorkflowStatus = WorkflowStatus.InProgress): ManufacturingWorkflow =
    ManufacturingWorkflow(
      WorkflowId.unsafe("wf-1"), OrderId.unsafe("o-1"), 0,
      steps, status, Priority.Normal, None, 100000L,
    )

  private def makeOrder(
      workflows: List[ManufacturingWorkflow],
      approval: ApprovalStatus = ApprovalStatus.Approved,
      deadline: Option[Long] = None,
  ): ManufacturingOrder =
    ManufacturingOrder(
      null.asInstanceOf[Order], workflows, approval, "", 100000L, deadline,
    )

  private val emp1 = EmployeeId.unsafe("emp-1")
  private val emp2 = EmployeeId.unsafe("emp-2")

  def spec = suite("AnalyticsService")(
    suite("averageTimePerStation")(
      test("computes average time for completed steps") {
        val steps = List(
          makeStep(StationType.DigitalPrinter, StepStatus.Completed, startedAt = Some(1000L), completedAt = Some(3000L)),
          makeStep(StationType.DigitalPrinter, StepStatus.Completed, startedAt = Some(2000L), completedAt = Some(6000L)),
        )
        val orders = List(makeOrder(List(makeWorkflow(steps))))
        val result = AnalyticsService.averageTimePerStation(orders)
        assertTrue(result(StationType.DigitalPrinter) == 3000L) // (2000 + 4000) / 2
      },
      test("ignores non-completed steps") {
        val steps = List(
          makeStep(StationType.DigitalPrinter, StepStatus.InProgress, startedAt = Some(1000L)),
          makeStep(StationType.Cutter, StepStatus.Completed, startedAt = Some(1000L), completedAt = Some(2000L)),
        )
        val orders = List(makeOrder(List(makeWorkflow(steps))))
        val result = AnalyticsService.averageTimePerStation(orders)
        assertTrue(!result.contains(StationType.DigitalPrinter), result(StationType.Cutter) == 1000L)
      },
      test("returns empty map when no completed steps") {
        val orders = List(makeOrder(List(makeWorkflow(List(makeStep(StationType.Prepress, StepStatus.Ready))))))
        val result = AnalyticsService.averageTimePerStation(orders)
        assertTrue(result.isEmpty)
      },
    ),
    suite("bottleneckStation")(
      test("identifies station with most ready steps") {
        val steps = List(
          makeStep(StationType.Cutter, StepStatus.Ready),
          makeStep(StationType.Cutter, StepStatus.Ready),
          makeStep(StationType.DigitalPrinter, StepStatus.Ready),
        )
        val orders = List(makeOrder(List(makeWorkflow(steps))))
        val result = AnalyticsService.bottleneckStation(orders)
        assertTrue(result == Some((StationType.Cutter, 2)))
      },
      test("returns None when no ready steps") {
        val steps = List(makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(200L)))
        val orders = List(makeOrder(List(makeWorkflow(steps, WorkflowStatus.Completed))))
        val result = AnalyticsService.bottleneckStation(orders)
        assertTrue(result.isEmpty)
      },
    ),
    suite("employeeThroughput")(
      test("counts completed steps per employee") {
        val steps = List(
          makeStep(StationType.DigitalPrinter, StepStatus.Completed, assignedTo = Some(emp1), startedAt = Some(100L), completedAt = Some(200L)),
          makeStep(StationType.Cutter, StepStatus.Completed, assignedTo = Some(emp1), startedAt = Some(200L), completedAt = Some(300L)),
          makeStep(StationType.Prepress, StepStatus.Completed, assignedTo = Some(emp2), startedAt = Some(50L), completedAt = Some(150L)),
        )
        val orders = List(makeOrder(List(makeWorkflow(steps))))
        val result = AnalyticsService.employeeThroughput(orders)
        assertTrue(result(emp1) == 2, result(emp2) == 1)
      },
      test("ignores steps without assigned employee") {
        val steps = List(
          makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(200L)),
        )
        val orders = List(makeOrder(List(makeWorkflow(steps))))
        val result = AnalyticsService.employeeThroughput(orders)
        assertTrue(result.isEmpty)
      },
    ),
    suite("onTimeDeliveryRate")(
      test("1.0 when no completed orders") {
        val orders = List(makeOrder(List(makeWorkflow(List(makeStep(StationType.Prepress, StepStatus.InProgress))))))
        val result = AnalyticsService.onTimeDeliveryRate(orders)
        assertTrue(result == 1.0)
      },
      test("1.0 when completed order has no deadline") {
        val steps = List(makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(200L)))
        val orders = List(makeOrder(List(makeWorkflow(steps, WorkflowStatus.Completed)), deadline = None))
        val result = AnalyticsService.onTimeDeliveryRate(orders)
        assertTrue(result == 1.0)
      },
      test("1.0 when completed before deadline") {
        val steps = List(makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(200L)))
        val orders = List(makeOrder(List(makeWorkflow(steps, WorkflowStatus.Completed)), deadline = Some(500L)))
        val result = AnalyticsService.onTimeDeliveryRate(orders)
        assertTrue(result == 1.0)
      },
      test("0.0 when all completed after deadline") {
        val steps = List(makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(600L)))
        val orders = List(makeOrder(List(makeWorkflow(steps, WorkflowStatus.Completed)), deadline = Some(500L)))
        val result = AnalyticsService.onTimeDeliveryRate(orders)
        assertTrue(result == 0.0)
      },
      test("0.5 when half on-time") {
        val onTime = makeOrder(
          List(makeWorkflow(List(makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(200L))), WorkflowStatus.Completed)),
          deadline = Some(500L),
        )
        val late = makeOrder(
          List(makeWorkflow(List(makeStep(StationType.Prepress, StepStatus.Completed, startedAt = Some(100L), completedAt = Some(600L))), WorkflowStatus.Completed)),
          deadline = Some(500L),
        )
        val result = AnalyticsService.onTimeDeliveryRate(List(onTime, late))
        assertTrue(result == 0.5)
      },
    ),
    suite("computeSummary")(
      test("computes summary with station and employee metrics") {
        val steps = List(
          makeStep(StationType.Prepress, StepStatus.Completed, assignedTo = Some(emp1), startedAt = Some(100L), completedAt = Some(300L)),
          makeStep(StationType.DigitalPrinter, StepStatus.Ready),
        )
        val employees = List(
          Employee(emp1, "Jan Novák", Set(StationType.Prepress), isActive = true),
          Employee(emp2, "Marie S.", Set(StationType.DigitalPrinter), isActive = true),
        )
        val orders = List(makeOrder(List(makeWorkflow(steps))))
        val summary = AnalyticsService.computeSummary(orders, employees)
        assertTrue(
          summary.totalOrders == 1,
          summary.inProgressOrders == 1,
          summary.stationMetrics.nonEmpty,
          summary.employeeMetrics.size == 2,
          summary.employeeMetrics.find(_.employeeId == emp1).get.completedSteps == 1,
        )
      },
    ),
  )
