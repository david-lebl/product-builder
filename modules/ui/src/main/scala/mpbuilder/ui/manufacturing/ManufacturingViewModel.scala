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

  // ── Selection & filtering ─────────────────────────────────────

  def selectOrder(orderId: Option[String]): Unit =
    stateVar.update(_.copy(selectedOrderId = orderId))

  def toggleStationFilter(stationType: StationType): Unit =
    stateVar.update { s =>
      val updated =
        if s.stationFilter.contains(stationType) then s.stationFilter - stationType
        else s.stationFilter + stationType
      s.copy(stationFilter = updated)
    }

  def clearStationFilter(): Unit =
    stateVar.update(_.copy(stationFilter = Set.empty))

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
          o.copy(status = OrderStatus.Cancelled, updatedAt = Date.now().toLong, currentStationId = None)
        else o
      }
      // Free any station that had this order
      val stations = s.stations.map { st =>
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

  /** Unified single-button action: pick up a pending/in-progress order and
    * auto-assign it to the best available station for its next required step.
    */
  def pickupAndStart(orderId: String): Unit =
    stateVar.update { s =>
      s.orders.find(_.id == orderId) match
        case Some(order) if order.status == OrderStatus.Pending || order.status == OrderStatus.InProgress =>
          val nextRequired = order.requiredStationTypes.filterNot(order.completedStationTypes.contains).headOption
          nextRequired match
            case Some(stType) =>
              val stationOpt = s.stations.find(st =>
                st.stationType == stType && st.currentOrderId.isEmpty && st.isActive
              )
              stationOpt match
                case Some(station) =>
                  val updatedOrder = order.copy(
                    status = OrderStatus.AtStation,
                    currentStationId = Some(station.id),
                    updatedAt = Date.now().toLong,
                  )
                  val updatedStation = station.copy(currentOrderId = Some(orderId))
                  s.copy(
                    orders = s.orders.map(o => if o.id == orderId then updatedOrder else o),
                    stations = s.stations.map(st => if st.id == station.id then updatedStation else st),
                  )
                case None =>
                  // No available station — just mark InProgress
                  val updatedOrder = order.copy(
                    status = OrderStatus.InProgress,
                    updatedAt = Date.now().toLong,
                  )
                  s.copy(orders = s.orders.map(o => if o.id == orderId then updatedOrder else o))
            case None => s // no more steps
        case _ => s
    }

  /** Unified single-button action: complete work at current station and
    * auto-advance to next required station if available.
    */
  def completeAndAdvance(orderId: String): Unit =
    stateVar.update { s =>
      s.orders.find(_.id == orderId) match
        case Some(order) if order.status == OrderStatus.AtStation =>
          val stationOpt = order.currentStationId.flatMap(sid => s.stations.find(_.id == sid))
          stationOpt match
            case Some(station) =>
              val completedType = station.stationType
              val newCompleted  = order.completedStationTypes :+ completedType
              val remaining     = order.requiredStationTypes.filterNot(newCompleted.contains)

              // Free current station
              val freedStations = s.stations.map(st =>
                if st.id == station.id then st.copy(currentOrderId = None) else st
              )

              if remaining.isEmpty then
                // Order fully completed
                val updatedOrder = order.copy(
                  status = OrderStatus.Completed,
                  currentStationId = None,
                  completedStationTypes = newCompleted,
                  updatedAt = Date.now().toLong,
                )
                s.copy(
                  orders = s.orders.map(o => if o.id == orderId then updatedOrder else o),
                  stations = freedStations,
                )
              else
                // Try to auto-assign to next station
                val nextType = remaining.head
                val nextStationOpt = freedStations.find(st =>
                  st.stationType == nextType && st.currentOrderId.isEmpty && st.isActive
                )
                nextStationOpt match
                  case Some(nextStation) =>
                    val updatedOrder = order.copy(
                      status = OrderStatus.AtStation,
                      currentStationId = Some(nextStation.id),
                      completedStationTypes = newCompleted,
                      updatedAt = Date.now().toLong,
                    )
                    val updatedStations = freedStations.map(st =>
                      if st.id == nextStation.id then st.copy(currentOrderId = Some(orderId)) else st
                    )
                    s.copy(
                      orders = s.orders.map(o => if o.id == orderId then updatedOrder else o),
                      stations = updatedStations,
                    )
                  case None =>
                    val updatedOrder = order.copy(
                      status = OrderStatus.InProgress,
                      currentStationId = None,
                      completedStationTypes = newCompleted,
                      updatedAt = Date.now().toLong,
                    )
                    s.copy(
                      orders = s.orders.map(o => if o.id == orderId then updatedOrder else o),
                      stations = freedStations,
                    )
            case None => s
        case _ => s
    }

  def startOrder(orderId: String): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { o =>
        if o.id == orderId && o.status == OrderStatus.Pending then
          o.copy(status = OrderStatus.InProgress, updatedAt = Date.now().toLong)
        else o
      })
    }

  private def isTerminalStatus(status: OrderStatus): Boolean =
    status == OrderStatus.Completed || status == OrderStatus.Cancelled

  def removeCompletedOrders(): Unit =
    stateVar.update { s =>
      val removedIds = s.orders.filter(o => isTerminalStatus(o.status)).map(_.id).toSet
      val newSelected = s.selectedOrderId.filterNot(removedIds.contains)
      s.copy(
        orders = s.orders.filterNot(o => isTerminalStatus(o.status)),
        selectedOrderId = newSelected,
      )
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

  /** Add sample orders for demonstration purposes */
  def loadSampleData(): Unit =
    val now = scala.scalajs.js.Date.now().toLong
    val sampleFiles = List(
      AttachedFile("artwork.pdf", "pdf", 2400, now),
      AttachedFile("proof.jpg", "jpg", 850, now),
    )

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

    // Attach sample files to demo orders
    stateVar.update { s =>
      s.copy(orders = s.orders.map(o => o.copy(attachedFiles = sampleFiles)))
    }
