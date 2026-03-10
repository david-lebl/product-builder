package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.*

/** Pure in-memory manufacturing service.
  * State is held externally (in the UI view model) and passed in/out.
  */
object ManufacturingService:

  final case class ManufacturingState(
      stations: List[Station],
      orders: List[ManufacturingOrder],
  )

  object ManufacturingState:
    val empty: ManufacturingState = ManufacturingState(Nil, Nil)

  def addOrder(
      state: ManufacturingState,
      order: ManufacturingOrder,
  ): ManufacturingState =
    state.copy(orders = state.orders :+ order)

  def ordersAtStation(
      state: ManufacturingState,
      stationId: StationId,
  ): List[ManufacturingOrder] =
    state.orders.filter(_.currentStationId.contains(stationId))

  def ordersReadyForStation(
      state: ManufacturingState,
      stationId: StationId,
  ): List[ManufacturingOrder] =
    val stationIndex = state.stations.indexWhere(_.id == stationId)
    if stationIndex <= 0 then Nil
    else
      val previousStation = state.stations(stationIndex - 1)
      state.orders.filter { order =>
        order.currentStationId.contains(previousStation.id) &&
        order.steps
          .find(_.stationId == previousStation.id)
          .exists(_.status == OrderStatus.Completed)
      }

  def completedOrders(
      state: ManufacturingState,
  ): List[ManufacturingOrder] =
    state.orders.filter(_.isFullyCompleted)

  /** Pull an order from the previous station's completed pool into this station's queue. */
  def pullOrder(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
      stationId: StationId,
      now: Long,
  ): ManufacturingState =
    updateOrder(state, orderId) { order =>
      val updatedSteps = order.steps.map { step =>
        if step.stationId == stationId then
          step.copy(status = OrderStatus.Queued, queuedAt = now)
        else step
      }
      order.copy(
        steps = updatedSteps,
        currentStationId = Some(stationId),
      )
    }

  /** Start working on an order at its current station. */
  def startOrder(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
      now: Long,
  ): ManufacturingState =
    updateOrder(state, orderId) { order =>
      order.currentStationId match
        case Some(sid) =>
          val updatedSteps = order.steps.map { step =>
            if step.stationId == sid && step.status == OrderStatus.Queued then
              step.copy(status = OrderStatus.InProgress, startedAt = Some(now))
            else step
          }
          order.copy(steps = updatedSteps)
        case None => order
    }

  /** Complete work on an order at its current station. Advances to next station queue or marks fully done. */
  def completeOrder(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
      now: Long,
  ): ManufacturingState =
    updateOrder(state, orderId) { order =>
      order.currentStationId match
        case Some(sid) =>
          val updatedSteps = order.steps.map { step =>
            if step.stationId == sid && step.status == OrderStatus.InProgress then
              step.copy(status = OrderStatus.Completed, completedAt = Some(now))
            else step
          }
          // Find next station
          val currentIdx = state.stations.indexWhere(_.id == sid)
          val nextStation =
            if currentIdx >= 0 && currentIdx + 1 < state.stations.size then
              Some(state.stations(currentIdx + 1))
            else None

          nextStation match
            case Some(ns) =>
              // Stay at current station as "completed" — next station will pull
              order.copy(steps = updatedSteps)
            case None =>
              // Last station — order is fully complete, clear currentStationId
              order.copy(steps = updatedSteps, currentStationId = None)
        case None => order
    }

  /** Complete work and auto-advance to next station queue (push model). */
  def completeAndAdvance(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
      now: Long,
  ): ManufacturingState =
    updateOrder(state, orderId) { order =>
      order.currentStationId match
        case Some(sid) =>
          val updatedSteps = order.steps.map { step =>
            if step.stationId == sid && step.status == OrderStatus.InProgress then
              step.copy(status = OrderStatus.Completed, completedAt = Some(now))
            else step
          }
          val currentIdx = state.stations.indexWhere(_.id == sid)
          val nextStation =
            if currentIdx >= 0 && currentIdx + 1 < state.stations.size then
              Some(state.stations(currentIdx + 1))
            else None

          nextStation match
            case Some(ns) =>
              val advancedSteps = updatedSteps.map { step =>
                if step.stationId == ns.id then
                  step.copy(status = OrderStatus.Queued, queuedAt = now)
                else step
              }
              order.copy(steps = advancedSteps, currentStationId = Some(ns.id))
            case None =>
              order.copy(steps = updatedSteps, currentStationId = None)
        case None => order
    }

  /** Orders at any of the given stations, optionally filtered by statuses. */
  def ordersAtStations(
      state: ManufacturingState,
      stationIds: Set[StationId],
      statuses: Option[Set[OrderStatus]] = None,
  ): List[ManufacturingOrder] =
    state.orders.filter { order =>
      order.currentStationId.exists(stationIds.contains) && {
        statuses match
          case None => true
          case Some(ss) => order.currentStatus.exists(ss.contains)
      }
    }

  /** Put an order on hold at its current station. */
  def holdOrder(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
  ): ManufacturingState =
    updateOrder(state, orderId) { order =>
      order.currentStationId match
        case Some(sid) =>
          val updatedSteps = order.steps.map { step =>
            if step.stationId == sid &&
              (step.status == OrderStatus.Queued || step.status == OrderStatus.InProgress)
            then step.copy(status = OrderStatus.OnHold)
            else step
          }
          order.copy(steps = updatedSteps)
        case None => order
    }

  /** Resume a held order back to queued. */
  def resumeOrder(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
  ): ManufacturingState =
    updateOrder(state, orderId) { order =>
      order.currentStationId match
        case Some(sid) =>
          val updatedSteps = order.steps.map { step =>
            if step.stationId == sid && step.status == OrderStatus.OnHold then
              step.copy(status = OrderStatus.Queued)
            else step
          }
          order.copy(steps = updatedSteps)
        case None => order
    }

  /** Count orders at each station by status. */
  def stationSummary(
      state: ManufacturingState,
      stationId: StationId,
  ): Map[OrderStatus, Int] =
    val orders = ordersAtStation(state, stationId)
    orders
      .flatMap(_.currentStep)
      .groupBy(_.status)
      .map((status, steps) => status -> steps.size)

  private def updateOrder(
      state: ManufacturingState,
      orderId: ManufacturingOrderId,
  )(f: ManufacturingOrder => ManufacturingOrder): ManufacturingState =
    state.copy(orders = state.orders.map { order =>
      if order.id == orderId then f(order) else order
    })
