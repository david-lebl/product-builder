package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import zio.prelude.Validation
import mpbuilder.domain.model.*
import mpbuilder.domain.service.{ManufacturingService, ManufacturingError}
import mpbuilder.ui.ProductBuilderViewModel

/** In-memory store and operations for the manufacturing queue.
 *  Uses a Var[List[ManufacturingOrder]] as the backing store — no persistence.
 */
object ManufacturingViewModel:

  private val ordersVar: Var[List[ManufacturingOrder]] = Var(List.empty)
  val orders: Signal[List[ManufacturingOrder]]          = ordersVar.signal

  private val messageVar: Var[Option[String]] = Var(None)
  val message: Signal[Option[String]]          = messageVar.signal

  private var orderSeq: Int = 0

  private def generateOrderId(): OrderId =
    orderSeq += 1
    OrderId.unsafe(s"ORD-$orderSeq")

  /** Create one ManufacturingOrder per basket item and add them to the queue.
   *  Small-format items start as Queued; large-format items start as PendingApproval.
   *  Returns the number of orders placed.
   */
  def placeOrdersFromBasket(basket: Basket): Int =
    val now = System.currentTimeMillis()
    val newOrders = basket.items.map { item =>
      ManufacturingService.placeOrderFromItem(item, generateOrderId(), now)
    }
    ordersVar.update(_ ++ newOrders)
    newOrders.length

  def approve(orderId: OrderId): Unit =
    applyUpdate(orderId)(ManufacturingService.approve)

  def reject(orderId: OrderId, reason: Option[String] = None): Unit =
    applyUpdate(orderId)(order => ManufacturingService.reject(order, reason))

  def advance(orderId: OrderId): Unit =
    applyUpdate(orderId)(ManufacturingService.advance)

  def cancel(orderId: OrderId): Unit =
    applyUpdate(orderId)(ManufacturingService.cancel)

  def clearMessage(): Unit =
    messageVar.set(None)

  /** Count of orders that are not yet in a terminal state. */
  val activeOrderCount: Signal[Int] =
    orders.map(_.count(!_.status.isTerminal))

  private def applyUpdate(
      orderId: OrderId,
  )(f: ManufacturingOrder => Validation[ManufacturingError, ManufacturingOrder]): Unit =
    val lang          = ProductBuilderViewModel.stateVar.now().language
    val currentOrders = ordersVar.now()
    currentOrders.find(_.id == orderId) match
      case None =>
        messageVar.set(Some(ManufacturingError.OrderNotFound(orderId).message(lang)))
      case Some(order) =>
        f(order).fold(
          errors  => messageVar.set(Some(errors.map(_.message(lang)).toList.mkString(", "))),
          updated =>
            ordersVar.update(_.map(o => if o.id == orderId then updated else o))
            messageVar.set(None),
        )
