package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*

/** Reactive state management for the manufacturing UI.
  * Uses in-memory sample data for demo purposes.
  */
object ManufacturingViewModel:

  // ─── State ────────────────────────────────────────────────────────

  private val stateVar: Var[ManufacturingState] = Var(
    ManufacturingState.empty.copy(
      orders = SampleManufacturingData.orders,
      employees = SampleManufacturingData.employees,
      currentEmployeeId = Some(EmployeeId.unsafe("emp-1")),
    )
  )

  val state: Signal[ManufacturingState] = stateVar.signal

  /** Expose current state for imperative access in event handlers */
  def currentState(): ManufacturingState = stateVar.now()

  val selectedOrder: Signal[Option[ManufacturingOrder]] =
    state.map(s => s.selectedOrderId.flatMap(id => s.orders.find(_.id == id)))

  val currentRoute: Signal[ManufacturingRoute] = state.map(_.selectedRoute)

  val stationFilter: Signal[Set[StationType]] = state.map(_.stationFilter)

  val searchQuery: Signal[String] = state.map(_.searchQuery)

  // ─── Navigation ───────────────────────────────────────────────────

  def navigateTo(route: ManufacturingRoute): Unit =
    stateVar.update(_.copy(selectedRoute = route, selectedOrderId = None, selectedItemIndex = None))

  def selectOrder(orderId: OrderId): Unit =
    stateVar.update(_.copy(selectedOrderId = Some(orderId), selectedItemIndex = None))

  def selectOrderItem(orderId: OrderId, itemIndex: Int): Unit =
    stateVar.update(_.copy(selectedOrderId = Some(orderId), selectedItemIndex = Some(itemIndex)))

  def deselectOrder(): Unit =
    stateVar.update(_.copy(selectedOrderId = None, selectedItemIndex = None))

  def toggleStationFilter(station: StationType): Unit =
    stateVar.update { s =>
      val updated = if s.stationFilter.contains(station) then s.stationFilter - station
                    else s.stationFilter + station
      s.copy(stationFilter = updated)
    }

  def clearStationFilter(): Unit =
    stateVar.update(_.copy(stationFilter = Set.empty))

  def setSearchQuery(query: String): Unit =
    stateVar.update(_.copy(searchQuery = query))

  // ─── Order Approval Actions ───────────────────────────────────────

  def approveOrder(orderId: OrderId): Unit =
    stateVar.update { s =>
      val updated = s.orders.map { order =>
        if order.id == orderId then
          val approvedOrder = order.copy(approval = ApprovalStatus.Approved)
          val itemsWithWorkflows = approvedOrder.items.map { item =>
            if item.workflow.isEmpty then
              item.copy(workflow = Some(createDefaultWorkflow(orderId, item.itemIndex, item.productDescription)))
            else item
          }
          approvedOrder.copy(items = itemsWithWorkflows)
        else order
      }
      s.copy(orders = updated)
    }

  def rejectOrder(orderId: OrderId): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { order =>
        if order.id == orderId then order.copy(approval = ApprovalStatus.Rejected)
        else order
      })
    }

  def requestChanges(orderId: OrderId): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { order =>
        if order.id == orderId then order.copy(approval = ApprovalStatus.ChangesRequested)
        else order
      })
    }

  def setPriority(orderId: OrderId, priority: Priority): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { order =>
        if order.id == orderId then order.copy(priority = priority)
        else order
      })
    }

  // ─── Workflow Step Actions (linear progression) ───────────────────

  def pickupAndStart(orderId: OrderId, itemIndex: Int): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { order =>
        if order.id == orderId then
          order.copy(items = order.items.map { item =>
            if item.itemIndex == itemIndex then
              item.workflow match
                case Some(wf) =>
                  val updated = ManufacturingWorkflow.startCurrentStep(wf, s.currentEmployeeId)
                  item.copy(workflow = Some(updated))
                case None => item
            else item
          })
        else order
      })
    }

  def completeAndAdvance(orderId: OrderId, itemIndex: Int): Unit =
    stateVar.update { s =>
      s.copy(orders = s.orders.map { order =>
        if order.id == orderId then
          order.copy(items = order.items.map { item =>
            if item.itemIndex == itemIndex then
              item.workflow match
                case Some(wf) =>
                  val updated = ManufacturingWorkflow.completeCurrentStep(wf)
                  item.copy(workflow = Some(updated))
                case None => item
            else item
          })
        else order
      })
    }

  // ─── Helper ───────────────────────────────────────────────────────

  private var wfCounter: Int = 100
  private var stepIdCounter: Int = 1000

  private def nextStepId(): StepId =
    stepIdCounter += 1
    StepId.unsafe(s"step-$stepIdCounter")

  private def createDefaultWorkflow(orderId: OrderId, itemIndex: Int, description: String): ManufacturingWorkflow =
    wfCounter += 1
    val desc = description.toLowerCase

    val stationSequence = scala.collection.mutable.ListBuffer[StationType]()
    stationSequence += StationType.Prepress
    stationSequence += StationType.DigitalPrinter

    if desc.contains("laminat") then stationSequence += StationType.Laminator
    if desc.contains("uv") || desc.contains("varnish") then stationSequence += StationType.UVCoater
    if desc.contains("emboss") || desc.contains("foil") then stationSequence += StationType.EmbossingFoil

    stationSequence += StationType.Cutter

    if desc.contains("fold") || desc.contains("brochure") then stationSequence += StationType.Folder
    if desc.contains("bind") || desc.contains("booklet") || desc.contains("book") then stationSequence += StationType.Binder
    if desc.contains("grommet") || desc.contains("banner") then stationSequence += StationType.LargeFormatFinishing

    stationSequence += StationType.QualityControl
    stationSequence += StationType.Packaging

    val steps = stationSequence.toList.zipWithIndex.map { case (st, idx) =>
      WorkflowStep(
        id = nextStepId(),
        stationType = st,
        componentRole = if st == StationType.DigitalPrinter then Some(ComponentRole.Main) else None,
        stepIndex = idx,
        status = if idx == 0 then StepStatus.Ready else StepStatus.Waiting,
        assignedTo = None,
        notes = "",
      )
    }

    ManufacturingWorkflow(
      id = WorkflowId.unsafe(s"wf-$wfCounter"),
      orderId = orderId,
      orderItemIndex = itemIndex,
      steps = steps,
      status = WorkflowStatus.Pending,
      priority = Priority.Normal,
    )

/** Sample manufacturing data for demo purposes */
object SampleManufacturingData:

  val employees: List[Employee] = List(
    Employee(EmployeeId.unsafe("emp-1"), "Jan Novák", Set(StationType.DigitalPrinter, StationType.Laminator, StationType.Cutter), isActive = true),
    Employee(EmployeeId.unsafe("emp-2"), "Marie Svobodová", Set(StationType.Prepress, StationType.QualityControl), isActive = true),
    Employee(EmployeeId.unsafe("emp-3"), "Petr Dvořák", Set(StationType.Folder, StationType.Binder, StationType.Packaging), isActive = true),
  )

  private def mkStep(id: String, st: StationType, idx: Int, status: StepStatus, role: Option[ComponentRole] = None): WorkflowStep =
    WorkflowStep(StepId.unsafe(id), st, role, idx, status, None, "")

  private def mkFile(name: String, fileType: String): ManufacturingFile =
    ManufacturingFile(name, fileType, s"#file-$name")

  val orders: List[ManufacturingOrder] = List(
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1042"),
      customerName = "Petr Horák",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Business Cards",
          materialDescription = "350gsm Glossy Coated",
          quantity = 500,
          workflow = Some(ManufacturingWorkflow(
            id = WorkflowId.unsafe("wf-1"),
            orderId = OrderId.unsafe("ORD-1042"),
            orderItemIndex = 0,
            steps = List(
              mkStep("s1", StationType.Prepress, 0, StepStatus.Completed),
              mkStep("s2", StationType.DigitalPrinter, 1, StepStatus.Completed, Some(ComponentRole.Main)),
              mkStep("s3", StationType.Laminator, 2, StepStatus.InProgress, Some(ComponentRole.Main)),
              mkStep("s4", StationType.Cutter, 3, StepStatus.Waiting),
              mkStep("s5", StationType.QualityControl, 4, StepStatus.Waiting),
              mkStep("s6", StationType.Packaging, 5, StepStatus.Waiting),
            ),
            status = WorkflowStatus.InProgress,
            priority = Priority.Rush,
          )),
          files = List(mkFile("business-cards-front.pdf", "artwork"), mkFile("business-cards-back.pdf", "artwork")),
        ),
      ),
      approval = ApprovalStatus.Approved,
      priority = Priority.Rush,
      notes = "Customer requested rush delivery",
      createdAt = "2026-03-07 09:15",
      deadline = Some("2026-03-09 14:00"),
    ),
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1038"),
      customerName = "Jana Nováková",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Brochure A4",
          materialDescription = "170gsm Matte Coated",
          quantity = 200,
          workflow = Some(ManufacturingWorkflow(
            id = WorkflowId.unsafe("wf-2"),
            orderId = OrderId.unsafe("ORD-1038"),
            orderItemIndex = 0,
            steps = List(
              mkStep("s7", StationType.Prepress, 0, StepStatus.Ready),
              mkStep("s8", StationType.DigitalPrinter, 1, StepStatus.Waiting, Some(ComponentRole.Cover)),
              mkStep("s9", StationType.DigitalPrinter, 2, StepStatus.Waiting, Some(ComponentRole.Body)),
              mkStep("s10", StationType.Cutter, 3, StepStatus.Waiting),
              mkStep("s11", StationType.Folder, 4, StepStatus.Waiting),
              mkStep("s12", StationType.Binder, 5, StepStatus.Waiting),
              mkStep("s13", StationType.QualityControl, 6, StepStatus.Waiting),
              mkStep("s14", StationType.Packaging, 7, StepStatus.Waiting),
            ),
            status = WorkflowStatus.Pending,
            priority = Priority.Normal,
          )),
          files = List(mkFile("brochure-cover.pdf", "artwork"), mkFile("brochure-body.pdf", "artwork"), mkFile("brochure-imposed.pdf", "prepress")),
        ),
      ),
      approval = ApprovalStatus.Approved,
      priority = Priority.Normal,
      notes = "",
      createdAt = "2026-03-06 14:30",
      deadline = Some("2026-03-10 17:00"),
    ),
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1045"),
      customerName = "Martin Procházka",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Flyers A5",
          materialDescription = "135gsm Glossy Coated",
          quantity = 1000,
          workflow = None,
          files = List(mkFile("flyers-artwork.pdf", "artwork")),
        ),
        ManufacturingOrderItem(
          itemIndex = 1,
          productDescription = "Stickers Die Cut",
          materialDescription = "Vinyl Adhesive",
          quantity = 500,
          workflow = None,
          files = List(mkFile("stickers-artwork.pdf", "artwork"), mkFile("stickers-dieline.pdf", "dieline")),
        ),
      ),
      approval = ApprovalStatus.Pending,
      priority = Priority.Normal,
      notes = "",
      createdAt = "2026-03-08 11:20",
      deadline = Some("2026-03-12 12:00"),
    ),
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1044"),
      customerName = "Eva Králová",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Booklet A5",
          materialDescription = "200gsm Cover + 120gsm Body",
          quantity = 100,
          workflow = None,
          files = List(mkFile("booklet-cover.pdf", "artwork"), mkFile("booklet-pages.pdf", "artwork")),
        ),
      ),
      approval = ApprovalStatus.Pending,
      priority = Priority.Low,
      notes = "Payment via bank transfer — awaiting confirmation",
      createdAt = "2026-03-08 08:45",
      deadline = Some("2026-03-14 17:00"),
    ),
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1035"),
      customerName = "Tomáš Černý",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Posters A3",
          materialDescription = "200gsm Glossy Coated",
          quantity = 300,
          workflow = Some(ManufacturingWorkflow(
            id = WorkflowId.unsafe("wf-5"),
            orderId = OrderId.unsafe("ORD-1035"),
            orderItemIndex = 0,
            steps = List(
              mkStep("s20", StationType.Prepress, 0, StepStatus.Completed),
              mkStep("s21", StationType.DigitalPrinter, 1, StepStatus.Completed, Some(ComponentRole.Main)),
              mkStep("s22", StationType.Cutter, 2, StepStatus.Completed),
              mkStep("s23", StationType.QualityControl, 3, StepStatus.Completed),
              mkStep("s24", StationType.Packaging, 4, StepStatus.Completed),
            ),
            status = WorkflowStatus.Completed,
            priority = Priority.Normal,
          )),
          files = List(mkFile("posters-artwork.pdf", "artwork")),
        ),
      ),
      approval = ApprovalStatus.Approved,
      priority = Priority.Normal,
      notes = "",
      createdAt = "2026-03-05 10:00",
      deadline = Some("2026-03-08 17:00"),
    ),
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1046"),
      customerName = "Lucie Benešová",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Calendars Wall A3",
          materialDescription = "250gsm Glossy + Spiral",
          quantity = 50,
          workflow = Some(ManufacturingWorkflow(
            id = WorkflowId.unsafe("wf-6"),
            orderId = OrderId.unsafe("ORD-1046"),
            orderItemIndex = 0,
            steps = List(
              mkStep("s30", StationType.Prepress, 0, StepStatus.Completed),
              mkStep("s31", StationType.DigitalPrinter, 1, StepStatus.Ready, Some(ComponentRole.Main)),
              mkStep("s32", StationType.Cutter, 2, StepStatus.Waiting),
              mkStep("s33", StationType.Binder, 3, StepStatus.Waiting),
              mkStep("s34", StationType.QualityControl, 4, StepStatus.Waiting),
              mkStep("s35", StationType.Packaging, 5, StepStatus.Waiting),
            ),
            status = WorkflowStatus.InProgress,
            priority = Priority.Normal,
          )),
          files = List(mkFile("calendar-pages.pdf", "artwork")),
        ),
      ),
      approval = ApprovalStatus.Approved,
      priority = Priority.Normal,
      notes = "",
      createdAt = "2026-03-07 16:00",
      deadline = Some("2026-03-11 17:00"),
    ),
    ManufacturingOrder(
      id = OrderId.unsafe("ORD-1047"),
      customerName = "Jiří Kolář",
      items = List(
        ManufacturingOrderItem(
          itemIndex = 0,
          productDescription = "Banners Large Format",
          materialDescription = "PVC Vinyl 440gsm",
          quantity = 2,
          workflow = None,
          files = List(mkFile("banner-artwork.pdf", "artwork")),
        ),
      ),
      approval = ApprovalStatus.Pending,
      priority = Priority.Rush,
      notes = "Event on Saturday — needs express production",
      createdAt = "2026-03-09 07:30",
      deadline = Some("2026-03-10 12:00"),
    ),
  )
