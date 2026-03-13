package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*

object QueueScorerSpec extends ZIOSpecDefault:

  private val hour = 1000L * 60 * 60
  private val minute = 1000L * 60

  private def makeWorkflow(
      priority: Priority = Priority.Normal,
      deadline: Option[Long] = None,
      createdAt: Long = 0L,
      completedStepCount: Int = 0,
      totalStepCount: Int = 5,
  ): ManufacturingWorkflow =
    val steps = (0 until totalStepCount).toList.map { i =>
      val status =
        if i < completedStepCount then StepStatus.Completed
        else if i == completedStepCount then StepStatus.Ready
        else StepStatus.Waiting
      WorkflowStep(
        id = StepId.unsafe(s"step-$i"),
        stationType = StationType.values(i % StationType.values.length),
        componentRole = None,
        dependsOn = if i == 0 then Set.empty else Set(StepId.unsafe(s"step-${i - 1}")),
        status = status,
        assignedTo = None,
        assignedMachine = None,
        startedAt = None,
        completedAt = None,
        notes = "",
      )
    }
    ManufacturingWorkflow(
      id = WorkflowId.unsafe("wf-score"),
      orderId = OrderId.unsafe("order-score"),
      orderItemIndex = 0,
      steps = steps,
      status = if completedStepCount > 0 then WorkflowStatus.InProgress else WorkflowStatus.Pending,
      priority = priority,
      deadline = deadline,
      createdAt = createdAt,
    )

  def spec = suite("QueueScorer")(
    suite("deadline urgency")(
      test("no deadline returns 0") {
        val urgency = QueueScorer.calculateDeadlineUrgency(None, 0L)
        assertTrue(urgency == 0)
      },
      test("overdue returns 100") {
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(1000L), 2000L)
        assertTrue(urgency == 100)
      },
      test("within 2 hours returns 95") {
        val now = 100 * hour
        val deadline = now + 1 * hour
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(deadline), now)
        assertTrue(urgency == 95)
      },
      test("within 8 hours returns 80") {
        val now = 100 * hour
        val deadline = now + 5 * hour
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(deadline), now)
        assertTrue(urgency == 80)
      },
      test("within 24 hours returns 60") {
        val now = 100 * hour
        val deadline = now + 12 * hour
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(deadline), now)
        assertTrue(urgency == 60)
      },
      test("within 48 hours returns 40") {
        val now = 100 * hour
        val deadline = now + 36 * hour
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(deadline), now)
        assertTrue(urgency == 40)
      },
      test("within 72 hours returns 20") {
        val now = 100 * hour
        val deadline = now + 60 * hour
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(deadline), now)
        assertTrue(urgency == 20)
      },
      test("more than 72 hours returns 5") {
        val now = 100 * hour
        val deadline = now + 100 * hour
        val urgency = QueueScorer.calculateDeadlineUrgency(Some(deadline), now)
        assertTrue(urgency == 5)
      },
    ),
    suite("priority boost")(
      test("Rush gives +30") {
        assertTrue(QueueScorer.calculatePriorityBoost(Priority.Rush) == 30)
      },
      test("Normal gives 0") {
        assertTrue(QueueScorer.calculatePriorityBoost(Priority.Normal) == 0)
      },
      test("Low gives -10") {
        assertTrue(QueueScorer.calculatePriorityBoost(Priority.Low) == -10)
      },
    ),
    suite("completeness boost")(
      test("no completed steps gives 0") {
        val wf = makeWorkflow(completedStepCount = 0, totalStepCount = 5)
        val boost = QueueScorer.calculateCompletenessBoost(wf)
        assertTrue(boost == 0)
      },
      test("80% complete gives 16") {
        val wf = makeWorkflow(completedStepCount = 4, totalStepCount = 5)
        val boost = QueueScorer.calculateCompletenessBoost(wf)
        assertTrue(boost == 16)
      },
      test("100% complete gives 20") {
        val wf = makeWorkflow(completedStepCount = 5, totalStepCount = 5)
        val boost = QueueScorer.calculateCompletenessBoost(wf)
        assertTrue(boost == 20)
      },
    ),
    suite("batch affinity")(
      test("matching material gives 15") {
        val matId = MaterialId.unsafe("mat-1")
        val affinity = QueueScorer.calculateBatchAffinity(Some(matId), Some(matId))
        assertTrue(affinity == 15)
      },
      test("different material gives 0") {
        val affinity = QueueScorer.calculateBatchAffinity(
          Some(MaterialId.unsafe("mat-1")),
          Some(MaterialId.unsafe("mat-2")),
        )
        assertTrue(affinity == 0)
      },
      test("no current material gives 0") {
        val affinity = QueueScorer.calculateBatchAffinity(None, Some(MaterialId.unsafe("mat-1")))
        assertTrue(affinity == 0)
      },
      test("no step material gives 0") {
        val affinity = QueueScorer.calculateBatchAffinity(Some(MaterialId.unsafe("mat-1")), None)
        assertTrue(affinity == 0)
      },
    ),
    suite("age calculation")(
      test("age in minutes is calculated correctly") {
        val age = QueueScorer.calculateAgeMinutes(0L, 30 * minute)
        assertTrue(age == 30L)
      },
      test("negative age is clamped to 0") {
        val age = QueueScorer.calculateAgeMinutes(1000L, 0L)
        assertTrue(age == 0L)
      },
    ),
    suite("composite scoring")(
      test("Rush + overdue scores higher than Normal + comfortable deadline") {
        val now = 100 * hour
        val rushWf = makeWorkflow(priority = Priority.Rush, deadline = Some(now - 1 * hour), createdAt = now - 2 * hour)
        val normalWf = makeWorkflow(priority = Priority.Normal, deadline = Some(now + 100 * hour), createdAt = now - 1 * hour)

        val rushScore = QueueScorer.score(rushWf, now)
        val normalScore = QueueScorer.score(normalWf, now)

        assertTrue(rushScore.total > normalScore.total)
      },
      test("sortByScore orders by descending total, then descending age") {
        val now = 100 * hour

        val highScore = QueueScorer.score(
          makeWorkflow(priority = Priority.Rush, deadline = Some(now - 1 * hour), createdAt = now - 3 * hour),
          now,
        )
        val lowScore = QueueScorer.score(
          makeWorkflow(priority = Priority.Low, deadline = Some(now + 100 * hour), createdAt = now - 1 * hour),
          now,
        )

        val sorted = QueueScorer.sortByScore(List(("low", lowScore), ("high", highScore)))

        assertTrue(
          sorted.head._1 == "high",
          sorted.last._1 == "low",
        )
      },
      test("FIFO tiebreaker: older workflow sorts first when totals equal") {
        val now = 100 * hour

        val olderWf = makeWorkflow(priority = Priority.Normal, createdAt = now - 3 * hour)
        val newerWf = makeWorkflow(priority = Priority.Normal, createdAt = now - 1 * hour)

        val olderScore = QueueScorer.score(olderWf, now)
        val newerScore = QueueScorer.score(newerWf, now)

        // Same total (both Normal priority, no deadline)
        assertTrue(olderScore.total == newerScore.total)

        val sorted = QueueScorer.sortByScore(List(("newer", newerScore), ("older", olderScore)))
        assertTrue(sorted.head._1 == "older")
      },
      test("QueueScore total is sum of components") {
        val score = QueueScore(
          deadlineUrgency = 60,
          priorityBoost = 30,
          completenessBoost = 10,
          batchAffinity = 15,
          ageMinutes = 100,
        )
        assertTrue(score.total == 60 + 30 + 10 + 15)
      },
    ),
  )
