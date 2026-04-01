package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Advisory queue score for ordering Ready workflow steps.
  *
  * The score is purely informational — it determines default sort order
  * but employees can re-sort or filter as they wish.
  */
final case class QueueScore(
    deadlineUrgency: Int,   // 0–100 based on hours until deadline
    priorityBoost: Int,     // Rush=30, Normal=0, Low=-10
    completenessBoost: Int, // 0–20 based on % of workflow steps done
    batchAffinity: Int,     // 0–15 if matches current machine setup
    ageMinutes: Long,       // tiebreaker — minutes since workflow creation
):
  /** Total composite score (higher = should be processed sooner). */
  def total: Int = deadlineUrgency + priorityBoost + completenessBoost + batchAffinity

/** Pure queue scoring service for ordering Ready steps. */
object QueueScorer:

  /** Calculate a queue score for a workflow step.
    *
    * @param workflow the workflow containing the step
    * @param now current time in millis (for deadline and age calculations)
    * @param currentMaterialId optional material ID currently loaded on the machine
    *                          (for batch affinity scoring)
    */
  def score(
      workflow: ManufacturingWorkflow,
      now: Long,
      currentMaterialId: Option[MaterialId] = None,
      manufacturingSpeed: Option[ManufacturingSpeed] = None,
  ): QueueScore =
    val base = QueueScore(
      deadlineUrgency = calculateDeadlineUrgency(workflow.deadline, now),
      priorityBoost = calculatePriorityBoost(workflow.priority),
      completenessBoost = calculateCompletenessBoost(workflow),
      batchAffinity = 0, // batch affinity requires machine context — always 0 for now
      ageMinutes = calculateAgeMinutes(workflow.createdAt, now),
    )
    applySpeedAdjustments(base, manufacturingSpeed)

  /** Score with batch affinity for a specific material match. */
  def scoreWithAffinity(
      workflow: ManufacturingWorkflow,
      step: WorkflowStep,
      now: Long,
      currentMaterialId: Option[MaterialId],
      stepMaterialId: Option[MaterialId],
      manufacturingSpeed: Option[ManufacturingSpeed] = None,
  ): QueueScore =
    val base = score(workflow, now, currentMaterialId, manufacturingSpeed)
    val affinity = calculateBatchAffinity(currentMaterialId, stepMaterialId)
    base.copy(batchAffinity = affinity)

  /** Sort a list of scored items by descending total score, then descending age (FIFO tiebreaker). */
  def sortByScore[T](items: List[(T, QueueScore)]): List[(T, QueueScore)] =
    items.sortBy { case (_, s) => (-s.total, -s.ageMinutes) }

  // --- Scoring components ---

  /** Deadline urgency: 0–100.
    * - No deadline → 0
    * - Overdue → 100
    * - Within 2 hours → 95
    * - Within 8 hours → 80
    * - Within 24 hours → 60
    * - Within 48 hours → 40
    * - Within 72 hours → 20
    * - More than 72 hours → 5
    */
  private[domain] def calculateDeadlineUrgency(deadline: Option[Long], now: Long): Int =
    deadline match
      case None => 0
      case Some(dl) =>
        val hoursRemaining = (dl - now).toDouble / (1000.0 * 60 * 60)
        if hoursRemaining <= 0 then 100      // overdue
        else if hoursRemaining <= 2 then 95  // critical
        else if hoursRemaining <= 8 then 80  // urgent
        else if hoursRemaining <= 24 then 60 // today
        else if hoursRemaining <= 48 then 40 // tomorrow
        else if hoursRemaining <= 72 then 20 // 3 days
        else 5                               // comfortable

  /** Priority boost: Rush=30, Normal=0, Low=-10. */
  private[domain] def calculatePriorityBoost(priority: Priority): Int =
    priority match
      case Priority.Rush   => 30
      case Priority.Normal => 0
      case Priority.Low    => -10

  /** Completeness boost: 0–20 based on % of workflow steps completed.
    * An almost-done workflow gets higher priority to clear it out.
    */
  private[domain] def calculateCompletenessBoost(workflow: ManufacturingWorkflow): Int =
    import ManufacturingWorkflow.*
    val ratio = workflow.completionRatio
    (ratio * 20).toInt

  /** Batch affinity: 0–15 based on material match with current machine setup. */
  private[domain] def calculateBatchAffinity(
      currentMaterialId: Option[MaterialId],
      stepMaterialId: Option[MaterialId],
  ): Int =
    (currentMaterialId, stepMaterialId) match
      case (Some(current), Some(step)) if current == step => 15
      case _ => 0

  /** Age in minutes since workflow creation (for FIFO tiebreaker). */
  private[domain] def calculateAgeMinutes(createdAt: Long, now: Long): Long =
    val millis = now - createdAt
    if millis < 0 then 0 else millis / (1000 * 60)

  /** Apply manufacturing speed tier adjustments to queue score.
    *
    * Express: +50 priority boost (overwhelming priority, always ahead of Standard/Economy).
    * Economy: +10 batch affinity (encourages batching Economy orders together).
    */
  private[domain] def applySpeedAdjustments(
      score: QueueScore,
      speed: Option[ManufacturingSpeed],
  ): QueueScore =
    speed match
      case Some(ManufacturingSpeed.Express) =>
        score.copy(priorityBoost = score.priorityBoost + 50)
      case Some(ManufacturingSpeed.Economy) =>
        score.copy(batchAffinity = score.batchAffinity + 10)
      case _ => score
