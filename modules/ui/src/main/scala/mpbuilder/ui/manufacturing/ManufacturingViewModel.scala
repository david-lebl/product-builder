package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import scala.scalajs.js.Date

/** In-memory manufacturing state management.
  *
  * Manages the order queue, stations, and internal routing for the
  * manufacturing module.  Designed as an in-memory implementation;
  * a future HTTP-backed version will keep the same reactive API.
  */
object ManufacturingViewModel:

  private val stateVar: Var[ManufacturingState] = Var(ManufacturingState.initial)

  val state: Signal[ManufacturingState] = stateVar.signal

  private var nextOrderId: Int = 1

  // ── Routing ────────────────────────────────────────────────────

  def navigateTo(route: ManufacturingRoute): Unit =
    if route.isAvailable then
      stateVar.update(_.copy(currentRoute = route))

  // ── Order CRUD ─────────────────────────────────────────────────

  def addOrder(
    customerName: String,
    productDescription: String,
    quantity: Int,
    priority: OrderPriority,
    requiredStationTypes: List[StationType],
    notes: String = "",
  ): Unit =
    val now = Date.now().toLong
    val order = ManufacturingOrder(
      id = s"ORD-${"%04d".format(nextOrderId)}",
      customerName = customerName,
      productDescription = productDescription,
      quantity = quantity,
      status = OrderStatus.Pending,
      priority = priority,
      requiredStationTypes = requiredStationTypes,
      createdAt = now,
      updatedAt = now,
      notes = notes,
    )
    nextOrderId += 1
    stateVar.update(s => s.copy(orders = s.orders :+ order))

  def cancelOrder(orderId: String): Unit =
    stateVar.update { s =>
      val updated = s.orders.map { o =>
        if o.id == orderId && o.status != OrderStatus.Completed then
          // Free the station if order was assigned to one
          o.currentStationId.foreach(sid => freeStationInternal(s, sid))
          o.copy(status = OrderStatus.Cancelled, updatedAt = Date.now().toLong, currentStationId = None)
        else o
      }
      val stations = updated.find(_.id == orderId).flatMap(_.currentStationId) match
        case Some(_) => s.stations // already handled
        case None =>
          // Free any station that had this order
          s.stations.map { st =>
            if st.currentOrderId.contains(orderId) then st.copy(currentOrderId = None) else st
          }
      s.copy(orders = updated, stations = stations)
    }

  def assignOrderToStation(orderId: String, stationId: String): Unit =
    stateVar.update { s =>
      val stationOpt = s.stations.find(_.id == stationId)
      val orderOpt   = s.orders.find(_.id == orderId)

      (stationOpt, orderOpt) match
        case (Some(station), Some(order))
            if station.currentOrderId.isEmpty && station.isActive
              && (order.status == OrderStatus.Pending || order.status == OrderStatus.InProgress) =>
          val updatedOrder = order.copy(
            status = OrderStatus.AtStation,
            currentStationId = Some(stationId),
            updatedAt = Date.now().toLong,
          )
          val updatedStation = station.copy(currentOrderId = Some(orderId))
          s.copy(
            orders = s.orders.map(o => if o.id == orderId then updatedOrder else o),
            stations = s.stations.map(st => if st.id == stationId then updatedStation else st),
          )
        case _ => s // invalid assignment — no-op
    }

  def completeStationWork(stationId: String): Unit =
    stateVar.update { s =>
      val stationOpt = s.stations.find(_.id == stationId)
      stationOpt match
        case Some(station) =>
          station.currentOrderId match
            case Some(orderId) =>
              val orderOpt = s.orders.find(_.id == orderId)
              orderOpt match
                case Some(order) =>
                  val completedType = station.stationType
                  val newCompleted  = order.completedStationTypes :+ completedType
                  val remaining     = order.requiredStationTypes.filterNot(newCompleted.contains)
                  val newStatus     = if remaining.isEmpty then OrderStatus.Completed else OrderStatus.InProgress
                  val updatedOrder = order.copy(
                    status = newStatus,
                    currentStationId = None,
                    completedStationTypes = newCompleted,
                    updatedAt = Date.now().toLong,
                  )
                  val updatedStation = station.copy(currentOrderId = None)
                  s.copy(
                    orders = s.orders.map(o => if o.id == orderId then updatedOrder else o),
                    stations = s.stations.map(st => if st.id == stationId then updatedStation else st),
                  )
                case None => s
            case None => s
        case None => s
    }

  def startOrder(orderId: String): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { o =>
        if o.id == orderId && o.status == OrderStatus.Pending then
          o.copy(status = OrderStatus.InProgress, updatedAt = Date.now().toLong)
        else o
      })
    }

  def removeCompletedOrders(): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.filterNot(o => o.status == OrderStatus.Completed || o.status == OrderStatus.Cancelled))
    }

  // ── Station management ────────────────────────────────────────

  def toggleStationActive(stationId: String): Unit =
    stateVar.update { s =>
      s.copy(stations = s.stations.map { st =>
        if st.id == stationId && st.currentOrderId.isEmpty then
          st.copy(isActive = !st.isActive)
        else st
      })
    }

  // ── Helpers ───────────────────────────────────────────────────

  private def freeStationInternal(s: ManufacturingState, stationId: String): Unit =
    // Note: This is called within update — the caller handles state mutation.
    ()

  /** Add sample orders for demonstration purposes */
  def loadSampleData(): Unit =
    addOrder("Acme Corp", "Business Cards 500pcs", 500, OrderPriority.Normal,
      List(StationType.DigitalPrinting, StationType.Cutting), "Standard 90x50mm")
    addOrder("Design Studio", "A3 Posters 100pcs", 100, OrderPriority.High,
      List(StationType.LargeFormat, StationType.Lamination, StationType.Cutting), "Glossy lamination")
    addOrder("BookWorm Ltd", "Brochures A4 200pcs", 200, OrderPriority.Normal,
      List(StationType.OffsetPrinting, StationType.Folding, StationType.Binding), "Saddle stitch binding")
    addOrder("Quick Print", "Flyers A5 1000pcs", 1000, OrderPriority.Low,
      List(StationType.DigitalPrinting, StationType.Cutting))
    addOrder("Premium Press", "Photo Books 50pcs", 50, OrderPriority.Urgent,
      List(StationType.DigitalPrinting, StationType.Lamination, StationType.Binding, StationType.QualityControl, StationType.Packaging),
      "Premium quality — rush order")
