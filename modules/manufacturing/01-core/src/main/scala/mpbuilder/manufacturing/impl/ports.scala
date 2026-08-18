package mpbuilder.manufacturing
package impl

import mpbuilder.commons.*
import zio.{IO, UIO}

private[manufacturing] trait ProductionRepository:
  def find(id: ProductionOrder.Id): UIO[Option[ProductionOrder]]

  /** The production order for a commercial order, if this shop already took it.
    *
    * What makes `accept` idempotent, and an outbox delivers at least once.
    */
  def findByOrderId(orderId: String): UIO[Option[ProductionOrder]]

  def save(order: ProductionOrder): UIO[ProductionOrder]

  /** Everything still awaiting a decision, oldest first. */
  def awaitingApproval: UIO[List[ProductionOrder]]

/** Turning a product into the steps needed to make it.
  *
  * In this context's own words: it hands over an opaque spec and a quantity, and is told which
  * stations the work passes through and in what order. How that is derived — today, the legacy
  * `WorkflowGenerator` — belongs entirely to the adapter.
  */
private[manufacturing] trait WorkflowPort:
  def plan(
      spec: String,
      lineNo: Int,
      orderId: String,
      quantity: Int,
  ): IO[ProductionError, List[ProductionOrder.Step]]

private[manufacturing] trait Events:
  def publish(events: List[ProductionEvent]): UIO[Unit]

private[manufacturing] trait Ids:
  def next: UIO[String]
