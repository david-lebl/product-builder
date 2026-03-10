package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.manufacturing.ManufacturingService.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.*

enum ManufacturingView:
  case Dashboard, StationQueue, OrderApproval, OrderProgress

case class ManufacturingUiState(
    mfgState: ManufacturingState,
    activeView: ManufacturingView,
    searchText: String,
    stationFilter: Set[StationType],
    statusFilter: Set[OrderStatus],
    priorityFilter: Set[OrderPriority],
    selectedOrderId: Option[ManufacturingOrderId],
)

object ManufacturingViewModel:

  // ── Seed helpers ────────────────────────────────────────────────────────────

  private val pricelist = SamplePricelist.pricelist

  private def makeConfig(
      id: String,
      category: ProductCategory,
      printingMethod: PrintingMethod,
      material: Material,
      inkConfig: InkConfiguration,
      finishes: List[SelectedFinish],
      specs: List[SpecValue],
  ): ProductConfiguration =
    ProductConfiguration(
      id = ConfigurationId.unsafe(id),
      category = category,
      printingMethod = printingMethod,
      components = List(
        ProductComponent(
          role = ComponentRole.Main,
          material = material,
          inkConfiguration = inkConfig,
          finishes = finishes,
          sheetCount = 1,
        )
      ),
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  private def seedBreakdown(config: ProductConfiguration): PriceBreakdown =
    PriceCalculator
      .calculate(config, pricelist)
      .fold(
        _ => PriceBreakdown(
          componentBreakdowns = Nil,
          processSurcharge = None,
          categorySurcharge = None,
          foldSurcharge = None,
          bindingSurcharge = None,
          subtotal = Money("0"),
          quantityMultiplier = BigDecimal(1),
          setupFees = Nil,
          minimumApplied = None,
          total = Money("0"),
          currency = Currency.USD,
          quantity = 1,
        ),
        bd => bd,
      )

  // ── Seeded configurations ────────────────────────────────────────────────────

  private val now = 1741600000000L // ≈ 2026-03-10 00:00:00 UTC

  private val configBC = makeConfig(
    id = "cfg-seed-bc",
    category = SampleCatalog.businessCards,
    printingMethod = SampleCatalog.digitalMethod,
    material = SampleCatalog.coated300gsm,
    inkConfig = InkConfiguration.cmyk4_0,
    finishes = List(SelectedFinish(SampleCatalog.matteLamination)),
    specs = List(
      SpecValue.SizeSpec(Dimension(90, 55)),
      SpecValue.QuantitySpec(Quantity.unsafe(500)),
    ),
  )

  private val configFlyers = makeConfig(
    id = "cfg-seed-fly",
    category = SampleCatalog.flyers,
    printingMethod = SampleCatalog.digitalMethod,
    material = SampleCatalog.coatedGlossy130gsm,
    inkConfig = InkConfiguration.cmyk4_4,
    finishes = Nil,
    specs = List(
      SpecValue.SizeSpec(Dimension(210, 148)),
      SpecValue.QuantitySpec(Quantity.unsafe(1000)),
      SpecValue.OrientationSpec(Orientation.Landscape),
    ),
  )

  private val configBrochures = makeConfig(
    id = "cfg-seed-bro",
    category = SampleCatalog.brochures,
    printingMethod = SampleCatalog.digitalMethod,
    material = SampleCatalog.coatedGlossy150gsm,
    inkConfig = InkConfiguration.cmyk4_4,
    finishes = Nil,
    specs = List(
      SpecValue.SizeSpec(Dimension(210, 297)),
      SpecValue.QuantitySpec(Quantity.unsafe(250)),
      SpecValue.FoldTypeSpec(FoldType.Tri),
    ),
  )

  private val configFlyersSmall = makeConfig(
    id = "cfg-seed-fly2",
    category = SampleCatalog.flyers,
    printingMethod = SampleCatalog.digitalMethod,
    material = SampleCatalog.coatedGlossy115gsm,
    inkConfig = InkConfiguration.cmyk4_0,
    finishes = Nil,
    specs = List(
      SpecValue.SizeSpec(Dimension(148, 105)),
      SpecValue.QuantitySpec(Quantity.unsafe(2000)),
      SpecValue.OrientationSpec(Orientation.Portrait),
    ),
  )

  private val configBCPending = makeConfig(
    id = "cfg-seed-bc2",
    category = SampleCatalog.businessCards,
    printingMethod = SampleCatalog.digitalMethod,
    material = SampleCatalog.coated300gsm,
    inkConfig = InkConfiguration.cmyk4_4,
    finishes = Nil,
    specs = List(
      SpecValue.SizeSpec(Dimension(90, 55)),
      SpecValue.QuantitySpec(Quantity.unsafe(100)),
    ),
  )

  // ── Seeded steps ─────────────────────────────────────────────────────────────

  private def stepsForStations(stationList: List[Station], now: Long): List[ProductionStep] =
    WorkflowGenerator.generate(
      ProductConfiguration(
        id = ConfigurationId.unsafe("dummy"),
        category = SampleCatalog.businessCards,
        printingMethod = SampleCatalog.digitalMethod,
        components = List(ProductComponent(ComponentRole.Main, SampleCatalog.coated300gsm, InkConfiguration.cmyk4_0, Nil, 1)),
        specifications = ProductSpecifications.fromSpecs(List(
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
          SpecValue.SizeSpec(Dimension(90, 55)),
        )),
      ),
      stationList,
      now,
    )

  private val allStations = SampleStations.allStations

  /** Mark steps up to the given stationId as Completed, current as InProgress, rest as Queued. */
  private def advanceStepsTo(
      steps: List[ProductionStep],
      targetId: StationId,
      status: OrderStatus,
  ): List[ProductionStep] =
    var found = false
    steps.map { step =>
      if step.stationId == targetId then
        found = true
        step.copy(status = status, startedAt = if status == OrderStatus.InProgress then Some(now - 3600000L) else None)
      else if !found then
        step.copy(status = OrderStatus.Completed, completedAt = Some(now - 7200000L))
      else step
    }

  // ── Seeded orders ─────────────────────────────────────────────────────────────

  private def seedOrder(
      id: String,
      orderId: String,
      customer: String,
      config: ProductConfiguration,
      quantity: Int,
      priority: OrderPriority,
      deadline: Option[Long],
      notes: String,
      steps: List[ProductionStep],
      currentStationId: Option[StationId],
      createdAt: Long,
  ): ManufacturingOrder =
    ManufacturingOrder(
      id = ManufacturingOrderId.unsafe(id),
      orderId = OrderId.unsafe(orderId),
      customerName = customer,
      configuration = config,
      quantity = quantity,
      priceBreakdown = seedBreakdown(config),
      steps = steps,
      currentStationId = currentStationId,
      priority = priority,
      deadline = deadline,
      notes = notes,
      attachments = List(
        Attachment("artwork.pdf", "application/pdf", 2_450_000L, now - 86400000L),
      ),
      createdAt = createdAt,
    )

  private val bcSteps = advanceStepsTo(
    WorkflowGenerator.generate(configBC, allStations, now - 86400000L),
    SampleStations.printing.id,
    OrderStatus.InProgress,
  )

  private val flyerSteps = advanceStepsTo(
    WorkflowGenerator.generate(configFlyers, allStations, now - 43200000L),
    SampleStations.prepress.id,
    OrderStatus.InProgress,
  )

  private val brochureSteps = advanceStepsTo(
    WorkflowGenerator.generate(configBrochures, allStations, now - 172800000L),
    SampleStations.folding.id,
    OrderStatus.InProgress,
  )

  // Completed order: all steps done, no current station
  private val completedFlyerSteps =
    WorkflowGenerator.generate(configFlyersSmall, allStations, now - 259200000L)
      .map(_.copy(status = OrderStatus.Completed, completedAt = Some(now - 3600000L)))

  // Pending approval: no steps started, no current station
  private val pendingBcSteps = Nil

  private val seedOrders: List[ManufacturingOrder] = List(
    seedOrder(
      id          = "mfg-001",
      orderId     = "ord-4521",
      customer    = "Alice Nováková",
      config      = configBC,
      quantity    = 500,
      priority    = OrderPriority.High,
      deadline    = Some(now + 2 * 86400000L),
      notes       = "Rush order — conference next week.",
      steps       = bcSteps,
      currentStationId = Some(SampleStations.printing.id),
      createdAt   = now - 86400000L,
    ),
    seedOrder(
      id          = "mfg-002",
      orderId     = "ord-4522",
      customer    = "Bob Kratochvíl",
      config      = configFlyers,
      quantity    = 1000,
      priority    = OrderPriority.Normal,
      deadline    = Some(now + 5 * 86400000L),
      notes       = "",
      steps       = flyerSteps,
      currentStationId = Some(SampleStations.prepress.id),
      createdAt   = now - 43200000L,
    ),
    seedOrder(
      id          = "mfg-003",
      orderId     = "ord-4523",
      customer    = "TechCorp s.r.o.",
      config      = configBrochures,
      quantity    = 250,
      priority    = OrderPriority.Urgent,
      deadline    = Some(now + 86400000L),
      notes       = "Client approved proof — proceed immediately.",
      steps       = brochureSteps,
      currentStationId = Some(SampleStations.folding.id),
      createdAt   = now - 172800000L,
    ),
    seedOrder(
      id          = "mfg-004",
      orderId     = "ord-4519",
      customer    = "Eva Blažková",
      config      = configFlyersSmall,
      quantity    = 2000,
      priority    = OrderPriority.Low,
      deadline    = None,
      notes       = "No rush.",
      steps       = completedFlyerSteps,
      currentStationId = None,
      createdAt   = now - 259200000L,
    ),
    seedOrder(
      id          = "mfg-005",
      orderId     = "ord-4524",
      customer    = "Jan Horák",
      config      = configBCPending,
      quantity    = 100,
      priority    = OrderPriority.Normal,
      deadline    = Some(now + 7 * 86400000L),
      notes       = "Awaiting artwork approval.",
      steps       = pendingBcSteps,
      currentStationId = None,
      createdAt   = now - 3600000L,
    ),
  )

  private val initialState: ManufacturingUiState = ManufacturingUiState(
    mfgState = seedOrders.foldLeft(ManufacturingState(allStations, Nil)) { (s, o) =>
      ManufacturingService.addOrder(s, o)
    },
    activeView   = ManufacturingView.Dashboard,
    searchText   = "",
    stationFilter  = Set.empty,
    statusFilter   = Set.empty,
    priorityFilter = Set.empty,
    selectedOrderId = None,
  )

  // ── State ────────────────────────────────────────────────────────────────────

  val stateVar: Var[ManufacturingUiState] = Var(initialState)
  val state: Signal[ManufacturingUiState] = stateVar.signal

  // ── Navigation ───────────────────────────────────────────────────────────────

  def setView(view: ManufacturingView): Unit =
    stateVar.update(_.copy(activeView = view, selectedOrderId = None))

  // ── Derived signals ───────────────────────────────────────────────────────────

  val orders: Signal[List[ManufacturingOrder]] =
    state.map(_.mfgState.orders)

  val queuedOrders: Signal[List[ManufacturingOrder]] =
    state.map { s =>
      s.mfgState.orders.filter { o =>
        o.currentStationId.isDefined && o.currentStatus.contains(OrderStatus.Queued)
      }
    }

  val inProgressOrders: Signal[List[ManufacturingOrder]] =
    state.map { s =>
      s.mfgState.orders.filter { o =>
        o.currentStatus.contains(OrderStatus.InProgress)
      }
    }

  val completedOrders: Signal[List[ManufacturingOrder]] =
    state.map(s => ManufacturingService.completedOrders(s.mfgState))

  /** Orders not yet in the workflow (awaiting approval). */
  val pendingApprovalOrders: Signal[List[ManufacturingOrder]] =
    state.map { s =>
      s.mfgState.orders.filter(o => o.currentStationId.isEmpty && !o.isFullyCompleted)
    }

  val onHoldOrders: Signal[List[ManufacturingOrder]] =
    state.map { s =>
      s.mfgState.orders.filter(o => o.currentStatus.contains(OrderStatus.OnHold))
    }

  // ── Actions ──────────────────────────────────────────────────────────────────

  def pullOrder(orderId: ManufacturingOrderId, stationId: StationId): Unit =
    stateVar.update { s =>
      s.copy(mfgState = ManufacturingService.pullOrder(s.mfgState, orderId, stationId, System.currentTimeMillis()))
    }

  def startOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update { s =>
      s.copy(mfgState = ManufacturingService.startOrder(s.mfgState, orderId, System.currentTimeMillis()))
    }

  def completeOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update { s =>
      s.copy(mfgState = ManufacturingService.completeAndAdvance(s.mfgState, orderId, System.currentTimeMillis()))
    }

  def holdOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update { s =>
      s.copy(mfgState = ManufacturingService.holdOrder(s.mfgState, orderId))
    }

  def resumeOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update { s =>
      s.copy(mfgState = ManufacturingService.resumeOrder(s.mfgState, orderId))
    }

  /** Approve: pull into the first available station (Prepress). */
  def approveOrder(orderId: ManufacturingOrderId): Unit =
    stateVar.update { s =>
      val firstStation = s.mfgState.stations.headOption
      firstStation match
        case Some(st) =>
          // Generate steps for this order then pull it in
          val orderOpt = s.mfgState.orders.find(_.id == orderId)
          orderOpt match
            case Some(order) =>
              val steps = WorkflowGenerator.generate(order.configuration, s.mfgState.stations, System.currentTimeMillis())
              val updatedOrder = order.copy(steps = steps)
              val updatedState = s.mfgState.copy(orders = s.mfgState.orders.map(o => if o.id == orderId then updatedOrder else o))
              val pulled = ManufacturingService.pullOrder(updatedState, orderId, st.id, System.currentTimeMillis())
              s.copy(mfgState = pulled)
            case None => s
        case None => s
    }

  def selectOrder(id: Option[ManufacturingOrderId]): Unit =
    stateVar.update(_.copy(selectedOrderId = id))

  // ── Helpers ──────────────────────────────────────────────────────────────────

  def stationName(stationId: StationId): Signal[String] =
    state.map { s =>
      s.mfgState.stations.find(_.id == stationId).map(_.name.value).getOrElse(stationId.value)
    }

  def orderById(id: ManufacturingOrderId): Signal[Option[ManufacturingOrder]] =
    state.map(s => s.mfgState.orders.find(_.id == id))

  def formatDeadline(deadlineMs: Long): String =
    val now = System.currentTimeMillis()
    val diff = deadlineMs - now
    val days = diff / 86400000L
    if days < 0 then s"${-days}d overdue"
    else if days == 0 then "Today"
    else if days == 1 then "Tomorrow"
    else s"In ${days}d"
