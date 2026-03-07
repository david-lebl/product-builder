package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.manufacturing.ManufacturingService.ManufacturingState
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.SampleCatalog
import mpbuilder.ui.ProductBuilderViewModel
import scala.scalajs.js.Date

sealed trait ManufacturingRoute
object ManufacturingRoute:
  case object Dashboard extends ManufacturingRoute
  case object WorkQueue extends ManufacturingRoute
  case class StationView(stationId: StationId) extends ManufacturingRoute
  case object AllOrders extends ManufacturingRoute
  case object CompletedOrders extends ManufacturingRoute

enum SortColumn:
  case Id, Customer, Product, Qty, Station, Status, Priority, Deadline, Created

enum SortDirection:
  case Asc, Desc
  def toggle: SortDirection = this match
    case Asc  => Desc
    case Desc => Asc

object ManufacturingViewModel:

  private val stateVar: Var[ManufacturingState] = Var(
    ManufacturingState(
      stations = SampleStations.allStations,
      orders = Nil,
    )
  )

  val state: Signal[ManufacturingState] = stateVar.signal

  val routeVar: Var[ManufacturingRoute] = Var(ManufacturingRoute.Dashboard)
  val route: Signal[ManufacturingRoute] = routeVar.signal

  private var orderCounter: Int = 0

  // Station filter for work queue — all stations enabled by default
  val stationFilterVar: Var[Set[StationId]] = Var(
    SampleStations.allStations.map(_.id).toSet
  )
  val stationFilter: Signal[Set[StationId]] = stationFilterVar.signal

  // Search query
  val searchQueryVar: Var[String] = Var("")
  val searchQuery: Signal[String] = searchQueryVar.signal

  // Sort state
  val sortColumnVar: Var[SortColumn] = Var(SortColumn.Priority)
  val sortDirectionVar: Var[SortDirection] = Var(SortDirection.Desc)
  val sortColumn: Signal[SortColumn] = sortColumnVar.signal
  val sortDirection: Signal[SortDirection] = sortDirectionVar.signal

  // Selected order for detail view
  val selectedOrderVar: Var[Option[ManufacturingOrderId]] = Var(None)
  val selectedOrder: Signal[Option[ManufacturingOrderId]] = selectedOrderVar.signal

  // Station filter dropdown open state
  val stationDropdownOpenVar: Var[Boolean] = Var(false)

  def stations: List[Station] = SampleStations.allStations

  def navigateTo(r: ManufacturingRoute): Unit = routeVar.set(r)

  def toggleStationFilter(stationId: StationId): Unit =
    stationFilterVar.update { current =>
      if current.contains(stationId) then current - stationId
      else current + stationId
    }

  def setSort(col: SortColumn): Unit =
    if sortColumnVar.now() == col then
      sortDirectionVar.update(_.toggle)
    else
      sortColumnVar.set(col)
      sortDirectionVar.set(SortDirection.Asc)

  def selectOrder(id: ManufacturingOrderId): Unit =
    selectedOrderVar.set(Some(id))

  def closeDetail(): Unit =
    selectedOrderVar.set(None)

  def updateOrderPriority(orderId: ManufacturingOrderId, priority: OrderPriority): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { o =>
        if o.id == orderId then o.copy(priority = priority) else o
      })
    }

  def updateOrderNotes(orderId: ManufacturingOrderId, notes: String): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { o =>
        if o.id == orderId then o.copy(notes = notes) else o
      })
    }

  private def now(): Long = Date.now().toLong

  def addSampleOrder(): Unit =
    orderCounter += 1
    val catalog = SampleCatalog.catalog
    val (_, category) = catalog.categories.head
    val (_, material) = catalog.materials.head
    val (_, printingMethod) = catalog.printingMethods.head

    val configId = ConfigurationId.unsafe(s"mfg-config-$orderCounter")
    val component = ProductComponent(
      role = ComponentRole.Main,
      material = material,
      inkConfiguration = InkConfiguration.cmyk4_0,
      finishes = Nil,
      sheetCount = 1,
    )
    val config = ProductConfiguration(
      id = configId,
      category = category,
      printingMethod = printingMethod,
      components = List(component),
      specifications = ProductSpecifications(Map(
        SpecKind.Quantity -> SpecValue.QuantitySpec(Quantity.unsafe(100)),
        SpecKind.Size -> SpecValue.SizeSpec(Dimension(210, 297)),
      )),
    )

    val breakdown = PriceBreakdown(
      componentBreakdowns = Nil,
      processSurcharge = None,
      categorySurcharge = None,
      foldSurcharge = None,
      bindingSurcharge = None,
      subtotal = Money("100.00"),
      quantityMultiplier = BigDecimal(1),
      setupFees = Nil,
      minimumApplied = None,
      total = Money("100.00"),
      currency = Currency.CZK,
    )

    val orderId = ManufacturingOrderId.unsafe(s"MFG-${1000 + orderCounter}")
    val currentTime = now()
    val firstStation = SampleStations.allStations.head

    val steps = SampleStations.allStations.map { station =>
      ProductionStep(
        stationId = station.id,
        status = if station.id == firstStation.id then OrderStatus.Queued else OrderStatus.Queued,
        queuedAt = if station.id == firstStation.id then currentTime else 0L,
      )
    }

    val priorities = List(OrderPriority.Low, OrderPriority.Normal, OrderPriority.High, OrderPriority.Urgent)
    val priority = priorities((orderCounter - 1) % priorities.size)
    val deadlineOffset = priority match
      case OrderPriority.Urgent => 1L * 24 * 3600 * 1000
      case OrderPriority.High   => 3L * 24 * 3600 * 1000
      case OrderPriority.Normal => 7L * 24 * 3600 * 1000
      case OrderPriority.Low    => 14L * 24 * 3600 * 1000

    val sampleAttachments = List(
      Attachment(s"artwork_$orderCounter.pdf", "application/pdf", 2_500_000L, currentTime),
      Attachment(s"proof_$orderCounter.jpg", "image/jpeg", 850_000L, currentTime),
    )

    val order = ManufacturingOrder(
      id = orderId,
      orderId = OrderId.unsafe(s"ORD-${1000 + orderCounter}"),
      customerName = s"Customer $orderCounter",
      configuration = config,
      quantity = 100 * orderCounter,
      priceBreakdown = breakdown,
      steps = steps,
      currentStationId = Some(firstStation.id),
      priority = priority,
      deadline = Some(currentTime + deadlineOffset),
      notes = if orderCounter % 3 == 0 then "Rush order - customer requested expedited processing" else "",
      attachments = sampleAttachments,
      createdAt = currentTime,
    )

    stateVar.update(s => ManufacturingService.addOrder(s, order))

  def pullOrder(orderId: ManufacturingOrderId, stationId: StationId): Unit =
    stateVar.update(s => ManufacturingService.pullOrder(s, orderId, stationId, now()))

  def startOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update(s => ManufacturingService.startOrder(s, orderId, now()))

  def completeOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update(s => ManufacturingService.completeOrder(s, orderId, now()))

  def completeAndAdvance(orderId: ManufacturingOrderId): Unit =
    stateVar.update(s => ManufacturingService.completeAndAdvance(s, orderId, now()))

  def holdOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update(s => ManufacturingService.holdOrder(s, orderId))

  def resumeOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update(s => ManufacturingService.resumeOrder(s, orderId))

  def ordersAtStation(stationId: StationId): Signal[List[ManufacturingOrder]] =
    state.map(s => ManufacturingService.ordersAtStation(s, stationId))

  def ordersReadyForStation(stationId: StationId): Signal[List[ManufacturingOrder]] =
    state.map(s => ManufacturingService.ordersReadyForStation(s, stationId))

  def completedOrders: Signal[List[ManufacturingOrder]] =
    state.map(s => ManufacturingService.completedOrders(s))

  def filteredOrders: Signal[List[ManufacturingOrder]] =
    state.combineWith(stationFilter).combineWith(searchQuery).map { case (s, filter, query) =>
      val stationFiltered = ManufacturingService.ordersAtStations(s, filter)
      if query.trim.isEmpty then stationFiltered
      else
        val q = query.trim.toLowerCase
        stationFiltered.filter { o =>
          o.id.value.toLowerCase.contains(q) ||
          o.orderId.value.toLowerCase.contains(q) ||
          o.customerName.toLowerCase.contains(q) ||
          o.configuration.category.name(Language.En).toLowerCase.contains(q) ||
          o.configuration.category.name(Language.Cs).toLowerCase.contains(q)
        }
    }

  def sortedFilteredOrders: Signal[List[ManufacturingOrder]] =
    filteredOrders
      .combineWith(sortColumn)
      .combineWith(sortDirection)
      .combineWith(state.map(_.stations))
      .map { case (orders, col, dir, stations) =>
        val sorted = col match
          case SortColumn.Id       => orders.sortBy(_.id.value)
          case SortColumn.Customer => orders.sortBy(_.customerName.toLowerCase)
          case SortColumn.Product  => orders.sortBy(_.configuration.category.name(Language.En).toLowerCase)
          case SortColumn.Qty      => orders.sortBy(_.quantity)
          case SortColumn.Station  =>
            orders.sortBy(o => o.currentStationId.flatMap(sid => stations.find(_.id == sid)).map(_.sortOrder).getOrElse(Int.MaxValue))
          case SortColumn.Status   => orders.sortBy(_.currentStatus.map(_.ordinal).getOrElse(Int.MaxValue))
          case SortColumn.Priority => orders.sortBy(_.priority.ordinal)
          case SortColumn.Deadline => orders.sortBy(_.deadline.getOrElse(Long.MaxValue))
          case SortColumn.Created  => orders.sortBy(_.createdAt)

        if dir == SortDirection.Desc then sorted.reverse else sorted
      }

  def stationCounts(stationId: StationId): Signal[Map[OrderStatus, Int]] =
    state.map(s => ManufacturingService.stationSummary(s, stationId))
