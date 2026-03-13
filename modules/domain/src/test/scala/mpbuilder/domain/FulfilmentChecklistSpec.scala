package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.FulfilmentChecklist.*

object FulfilmentChecklistSpec extends ZIOSpecDefault:

  def spec = suite("FulfilmentChecklist")(
    suite("create")(
      test("creates checklist with correct number of uncollected items") {
        val fc = FulfilmentChecklist.create(3)
        assertTrue(
          fc.collectedItems.size == 3,
          fc.collectedItems.forall(!_.collected),
          fc.collectedItems.map(_.itemIndex) == List(0, 1, 2),
        )
      },
      test("creates empty quality sign-off") {
        val fc = FulfilmentChecklist.create(1)
        assertTrue(
          !fc.qualitySignOff.passed,
          fc.qualitySignOff.signedBy.isEmpty,
          fc.qualitySignOff.notes.isEmpty,
        )
      },
      test("creates empty packaging and dispatch info") {
        val fc = FulfilmentChecklist.create(1)
        assertTrue(
          fc.packagingInfo.packagingType.isEmpty,
          !fc.dispatchInfo.dispatched,
          fc.dispatchInfo.trackingNumber.isEmpty,
        )
      },
    ),
    suite("allItemsCollected")(
      test("false when no items collected") {
        val fc = FulfilmentChecklist.create(2)
        assertTrue(!fc.allItemsCollected)
      },
      test("false when some items collected") {
        val fc = FulfilmentChecklist.create(2).copy(
          collectedItems = List(
            CollectedItem(0, collected = true, None),
            CollectedItem(1, collected = false, None),
          )
        )
        assertTrue(!fc.allItemsCollected)
      },
      test("true when all items collected") {
        val fc = FulfilmentChecklist.create(2).copy(
          collectedItems = List(
            CollectedItem(0, collected = true, Some(EmployeeId.unsafe("emp-1"))),
            CollectedItem(1, collected = true, Some(EmployeeId.unsafe("emp-1"))),
          )
        )
        assertTrue(fc.allItemsCollected)
      },
    ),
    suite("status")(
      test("NotStarted when nothing done") {
        val fc = FulfilmentChecklist.create(2)
        assertTrue(fc.status == FulfilmentStatus.NotStarted)
      },
      test("InProgress when some items collected") {
        val fc = FulfilmentChecklist.create(2).copy(
          collectedItems = List(
            CollectedItem(0, collected = true, None),
            CollectedItem(1, collected = false, None),
          )
        )
        assertTrue(fc.status == FulfilmentStatus.InProgress)
      },
      test("InProgress when quality passed but not dispatched") {
        val fc = FulfilmentChecklist.create(1).copy(
          qualitySignOff = QualitySignOff(passed = true, Some(EmployeeId.unsafe("emp-1")), "OK"),
        )
        assertTrue(fc.status == FulfilmentStatus.InProgress)
      },
      test("Completed when dispatched") {
        val fc = FulfilmentChecklist.create(1).copy(
          dispatchInfo = DispatchInfo(dispatched = true, "CZ123456", Some(100000L), Some(EmployeeId.unsafe("emp-1"))),
        )
        assertTrue(fc.status == FulfilmentStatus.Completed)
      },
    ),
    suite("completedStepsCount")(
      test("0 when nothing done") {
        val fc = FulfilmentChecklist.create(2)
        assertTrue(fc.completedStepsCount == 0, fc.totalStepsCount == 4)
      },
      test("1 when all items collected") {
        val fc = FulfilmentChecklist.create(1).copy(
          collectedItems = List(CollectedItem(0, collected = true, None)),
        )
        assertTrue(fc.completedStepsCount == 1)
      },
      test("2 when collected and quality passed") {
        val fc = FulfilmentChecklist.create(1).copy(
          collectedItems = List(CollectedItem(0, collected = true, None)),
          qualitySignOff = QualitySignOff(passed = true, None, ""),
        )
        assertTrue(fc.completedStepsCount == 2)
      },
      test("4 when fully complete") {
        val fc = FulfilmentChecklist.create(1).copy(
          collectedItems = List(CollectedItem(0, collected = true, None)),
          qualitySignOff = QualitySignOff(passed = true, None, ""),
          packagingInfo = PackagingInfo(Some(PackagingType.Box), Some("30x20x10"), Some("0.5")),
          dispatchInfo = DispatchInfo(dispatched = true, "CZ123", Some(100000L), None),
        )
        assertTrue(fc.completedStepsCount == 4)
      },
    ),
    suite("PackagingType")(
      test("display names are defined for all types") {
        assertTrue(
          PackagingType.Box.displayName == "Box",
          PackagingType.Envelope.displayName == "Envelope",
          PackagingType.Roll.displayName == "Roll",
          PackagingType.Tube.displayName == "Tube",
          PackagingType.Custom.displayName == "Custom",
        )
      },
    ),
    suite("ManufacturingOrder extensions")(
      test("isReadyForDispatch true when all workflows complete") {
        val wf = ManufacturingWorkflow(
          WorkflowId.unsafe("wf-1"), OrderId.unsafe("o-1"), 0,
          List(WorkflowStep(StepId.unsafe("s1"), StationType.Prepress, None, Set.empty,
            StepStatus.Completed, None, None, None, None, "", false)),
          WorkflowStatus.Completed, Priority.Normal, None, 100L,
        )
        val mo = ManufacturingOrder(
          null.asInstanceOf[Order], List(wf), ApprovalStatus.Approved, "", 100L, None,
        )
        assertTrue(mo.isReadyForDispatch)
      },
      test("isReadyForDispatch false when workflows still in progress") {
        val wf = ManufacturingWorkflow(
          WorkflowId.unsafe("wf-1"), OrderId.unsafe("o-1"), 0,
          List(WorkflowStep(StepId.unsafe("s1"), StationType.Prepress, None, Set.empty,
            StepStatus.InProgress, None, None, None, None, "", false)),
          WorkflowStatus.InProgress, Priority.Normal, None, 100L,
        )
        val mo = ManufacturingOrder(
          null.asInstanceOf[Order], List(wf), ApprovalStatus.Approved, "", 100L, None,
        )
        assertTrue(!mo.isReadyForDispatch)
      },
    ),
  )
