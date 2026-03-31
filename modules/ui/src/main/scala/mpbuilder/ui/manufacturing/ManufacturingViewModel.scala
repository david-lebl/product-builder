package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingWorkflow.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.domain.manufacturing.{ShopSchedule, WorkingHours, StationTimeEstimate}
import mpbuilder.domain.pricing.BusyPeriodMultiplier
import java.time.{DayOfWeek, LocalDate, LocalTime}

/** Reactive state management for the manufacturing UI. */
object ManufacturingViewModel:

  // --- State ---
  val currentRoute: Var[ManufacturingRoute] = Var(ManufacturingRoute.Dashboard)
  val manufacturingOrders: Var[List[ManufacturingOrder]] = Var(generateSampleOrders())
  val selectedOrderId: Var[Option[String]] = Var(None)
  val selectedQueueItemId: Var[Option[String]] = Var(None)
  val searchQuery: Var[String] = Var("")

  // Station queue filters
  val stationFilter: Var[Set[StationType]] = Var(StationType.values.toSet)
  val statusFilter: Var[Set[StepStatus]] = Var(Set(StepStatus.Ready, StepStatus.InProgress))
  val priorityFilter: Var[Set[Priority]] = Var(Priority.values.toSet)

  // Approval filters
  val approvalStatusFilter: Var[Set[ApprovalStatus]] = Var(Set(ApprovalStatus.Placed, ApprovalStatus.PendingChanges))

  // Progress filters
  val progressStatusFilter: Var[Set[WorkflowStatus]] = Var(Set(WorkflowStatus.InProgress, WorkflowStatus.Pending))

  // Employee & Machine state
  val employees: Var[List[Employee]] = Var(generateSampleEmployees())
  val machines: Var[List[Machine]] = Var(generateSampleMachines())
  val currentEmployeeId: Var[Option[EmployeeId]] = Var(Some(EmployeeId.unsafe("emp-1")))
  val selectedEmployeeId: Var[Option[String]] = Var(None)
  val selectedMachineId: Var[Option[String]] = Var(None)

  // --- Settings state ---
  val settingsOpenTime: Var[String] = Var("07:00")
  val settingsCloseTime: Var[String] = Var("17:00")
  val settingsWorkDays: Var[Set[DayOfWeek]] = Var(Set(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
  val settingsExpressCutoff: Var[String] = Var("14:00")
  val settingsStandardCutoff: Var[String] = Var("16:00")
  val settingsExpressMultiplier: Var[String] = Var("1.35")
  val settingsEconomyMultiplier: Var[String] = Var("0.85")
  val settingsExpressSurchargeCap: Var[String] = Var("2.00")
  val settingsExpressCriticalThreshold: Var[String] = Var("95")
  val settingsHolidays: Var[List[LocalDate]] = Var(List.empty)
  val settingsNewHoliday: Var[String] = Var("")

  def addHoliday(): Unit =
    val dateStr = settingsNewHoliday.now()
    if dateStr.nonEmpty then
      try
        val date = LocalDate.parse(dateStr)
        settingsHolidays.update(h => (h :+ date).distinct.sorted)
        settingsNewHoliday.set("")
      catch case _: Exception => ()

  def removeHoliday(date: LocalDate): Unit =
    settingsHolidays.update(_.filterNot(_ == date))

  // --- Derived signals ---

  val orders: Signal[List[ManufacturingOrder]] = manufacturingOrders.signal

  val dashboardSummary: Signal[DashboardSummary] = orders.map { ords =>
    DashboardSummary(
      awaitingApproval = ords.count(_.approvalStatus == ApprovalStatus.Placed),
      inProduction = ords.count(o => o.approvalStatus == ApprovalStatus.Approved && o.overallStatus == WorkflowStatus.InProgress),
      readyForDispatch = ords.count(o => o.overallStatus == WorkflowStatus.Completed),
      overdue = ords.count(o => o.deadline.exists(_ < System.currentTimeMillis())),
      todaysCompletions = ords.count(o => o.overallStatus == WorkflowStatus.Completed),
    )
  }

  val stationStatuses: Signal[List[StationStatus]] = orders.map { ords =>
    val allSteps = ords.filter(_.approvalStatus == ApprovalStatus.Approved).flatMap(_.workflows).flatMap(_.steps)
    StationType.values.toList.map { st =>
      val stepsForStation = allSteps.filter(_.stationType == st)
      StationStatus(
        stationType = st,
        queueDepth = stepsForStation.count(_.status == StepStatus.Ready),
        hasInProgress = stepsForStation.exists(_.status == StepStatus.InProgress),
      )
    }
  }

  val queueItems: Signal[List[QueueItem]] = orders.combineWith(stationFilter.signal, statusFilter.signal).map {
    case (ords, stFilter, stStatus) =>
      val approvedOrders = ords.filter(_.approvalStatus == ApprovalStatus.Approved)
      for
        order <- approvedOrders
        wf <- order.workflows
        step <- wf.steps
        if stFilter.contains(step.stationType)
        if stStatus.contains(step.status)
      yield QueueItem(step, wf, order)
  }

  val approvalOrders: Signal[List[ManufacturingOrder]] =
    orders.combineWith(approvalStatusFilter.signal).map { case (ords, statuses) =>
      ords.filter(o => statuses.contains(o.approvalStatus))
    }

  val progressOrders: Signal[List[ManufacturingOrder]] =
    orders.combineWith(progressStatusFilter.signal).map { case (ords, statuses) =>
      ords.filter(o => o.approvalStatus == ApprovalStatus.Approved && statuses.contains(o.overallStatus))
    }

  /** Current employee (resolved from currentEmployeeId). */
  val currentEmployee: Signal[Option[Employee]] =
    currentEmployeeId.signal.combineWith(employees.signal).map { case (optId, emps) =>
      optId.flatMap(id => emps.find(_.id == id))
    }

  /** Steps currently claimed by the logged-in employee. */
  val myInProgressJobs: Signal[List[QueueItem]] =
    orders.combineWith(currentEmployeeId.signal).map { case (ords, optEmpId) =>
      optEmpId match
        case None => Nil
        case Some(empId) =>
          val approvedOrders = ords.filter(_.approvalStatus == ApprovalStatus.Approved)
          for
            order <- approvedOrders
            wf <- order.workflows
            step <- wf.steps
            if step.status == StepStatus.InProgress
            if step.assignedTo.contains(empId)
          yield QueueItem(step, wf, order)
    }

  // --- Actions ---

  def selectOrder(orderId: String): Unit =
    selectedOrderId.set(Some(orderId))

  def deselectOrder(): Unit =
    selectedOrderId.set(None)

  def selectQueueItem(stepId: String): Unit =
    selectedQueueItemId.set(Some(stepId))

  def deselectQueueItem(): Unit =
    selectedQueueItemId.set(None)

  def approveOrder(orderId: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then
          val workflows = mo.order.basket.items.zipWithIndex.map { case (item, idx) =>
            WorkflowGenerator.generate(
              item.configuration,
              mo.order.id,
              idx,
              WorkflowId.unsafe(s"wf-${orderId}-$idx"),
              mo.priority,
              mo.deadline,
              System.currentTimeMillis(),
            )
          }
          mo.copy(
            approvalStatus = ApprovalStatus.Approved,
            workflows = workflows,
          )
        else mo
      }
    }

  def rejectOrder(orderId: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(approvalStatus = ApprovalStatus.Rejected)
        else mo
      }
    }

  def holdOrder(orderId: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(approvalStatus = ApprovalStatus.OnHold)
        else mo
      }
    }

  def requestChanges(orderId: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(approvalStatus = ApprovalStatus.PendingChanges)
        else mo
      }
    }

  def setOrderPriority(orderId: String, priority: Priority): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(priority = priority)
        else mo
      }
    }

  def setOrderDeadline(orderId: String, deadline: Option[Long]): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(deadline = deadline)
        else mo
      }
    }

  def setPaymentStatus(orderId: String, status: PaymentStatus): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(paymentStatus = status)
        else mo
      }
    }

  def updateArtworkCheck(orderId: String, artworkCheck: ArtworkCheck): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(artworkCheck = artworkCheck)
        else mo
      }
    }

  def setApprovalNotes(orderId: String, notes: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then mo.copy(approvalNotes = notes)
        else mo
      }
    }

  def startStep(stepId: String): Unit =
    val empId = currentEmployeeId.now()
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        mo.copy(workflows = mo.workflows.map { wf =>
          val updated = wf.copy(
            steps = wf.steps.map { s =>
              if s.id.value == stepId && s.status == StepStatus.Ready then
                s.copy(status = StepStatus.InProgress, assignedTo = empId, startedAt = Some(System.currentTimeMillis()))
              else s
            },
            status = WorkflowStatus.InProgress,
          )
          updated
        })
      }
    }

  def completeStep(stepId: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        val updatedMo = mo.copy(workflows = mo.workflows.map { wf =>
          val updatedSteps = wf.steps.map { s =>
            if s.id.value == stepId && s.status == StepStatus.InProgress then
              s.copy(status = StepStatus.Completed, completedAt = Some(System.currentTimeMillis()))
            else s
          }
          val updatedWf = wf.copy(steps = updatedSteps)
          val evaluated = updatedWf.evaluateReadiness
          val newStatus =
            if evaluated.steps.forall(s => s.status == StepStatus.Completed || s.status == StepStatus.Skipped)
            then WorkflowStatus.Completed
            else WorkflowStatus.InProgress
          evaluated.copy(status = newStatus)
        })
        maybeCreateFulfilmentChecklist(updatedMo)
      }
    }

  /** Auto-create fulfilment checklist when all workflows are completed. */
  private def maybeCreateFulfilmentChecklist(mo: ManufacturingOrder): ManufacturingOrder =
    if mo.isReadyForDispatch && mo.fulfilment.isEmpty then
      mo.copy(fulfilment = Some(FulfilmentChecklist.create(mo.order.basket.items.size)))
    else mo

  // --- Fulfilment Actions ---

  def toggleItemCollected(orderId: String, itemIndex: Int): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then
          val empId = currentEmployeeId.now()
          mo.copy(fulfilment = mo.fulfilment.map { fc =>
            fc.copy(collectedItems = fc.collectedItems.map { ci =>
              if ci.itemIndex == itemIndex then ci.copy(collected = !ci.collected, verifiedBy = empId)
              else ci
            })
          })
        else mo
      }
    }

  def signOffQuality(orderId: String, passed: Boolean, notes: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then
          val empId = currentEmployeeId.now()
          mo.copy(fulfilment = mo.fulfilment.map { fc =>
            fc.copy(qualitySignOff = QualitySignOff(passed, empId, notes))
          })
        else mo
      }
    }

  def setPackaging(orderId: String, packagingType: PackagingType, dimensions: String, weight: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then
          mo.copy(fulfilment = mo.fulfilment.map { fc =>
            fc.copy(packagingInfo = PackagingInfo(
              Some(packagingType),
              if dimensions.trim.nonEmpty then Some(dimensions.trim) else None,
              if weight.trim.nonEmpty then Some(weight.trim) else None,
            ))
          })
        else mo
      }
    }

  def confirmDispatch(orderId: String, trackingNumber: String): Unit =
    manufacturingOrders.update { ords =>
      ords.map { mo =>
        if mo.order.id.value == orderId then
          val empId = currentEmployeeId.now()
          mo.copy(fulfilment = mo.fulfilment.map { fc =>
            fc.copy(dispatchInfo = DispatchInfo(
              dispatched = true,
              trackingNumber.trim,
              Some(System.currentTimeMillis()),
              empId,
            ))
          })
        else mo
      }
    }

  // --- Employee & Machine Actions ---

  def selectEmployee(employeeId: String): Unit =
    selectedEmployeeId.set(Some(employeeId))

  def deselectEmployee(): Unit =
    selectedEmployeeId.set(None)

  def selectMachine(machineId: String): Unit =
    selectedMachineId.set(Some(machineId))

  def deselectMachine(): Unit =
    selectedMachineId.set(None)

  def setCurrentEmployee(empId: Option[EmployeeId]): Unit =
    currentEmployeeId.set(empId)

  def addEmployee(id: EmployeeId, name: String, capabilities: Set[StationType]): Unit =
    employees.update { emps =>
      EmployeeManagementService.addEmployee(emps, id, name, capabilities)
        .toEither.getOrElse(emps)
    }

  def toggleEmployeeActive(id: EmployeeId): Unit =
    employees.update { emps =>
      EmployeeManagementService.toggleActive(emps, id)
        .toEither.getOrElse(emps)
    }

  def updateEmployeeCapabilities(id: EmployeeId, capabilities: Set[StationType]): Unit =
    employees.update { emps =>
      EmployeeManagementService.updateCapabilities(emps, id, capabilities)
        .toEither.getOrElse(emps)
    }

  def addMachine(id: MachineId, name: String, stationType: StationType): Unit =
    machines.update { ms =>
      MachineManagementService.addMachine(ms, id, name, stationType)
        .toEither.getOrElse(ms)
    }

  def changeMachineStatus(id: MachineId, status: MachineStatus): Unit =
    machines.update { ms =>
      MachineManagementService.changeStatus(ms, id, status)
        .toEither.getOrElse(ms)
    }

  def updateMachineNotes(id: MachineId, name: String, notes: String): Unit =
    machines.update { ms =>
      MachineManagementService.updateMachine(ms, id, name, notes)
        .toEither.getOrElse(ms)
    }

  // --- Sample data generation ---

  private def generateSampleOrders(): List[ManufacturingOrder] =
    val catalog = SampleCatalog.catalog
    val ruleset = SampleRules.ruleset
    val now = System.currentTimeMillis()

    def makeOrder(
        id: String,
        firstName: String,
        lastName: String,
        email: String,
        items: List[(ProductConfiguration, Int)],
        approval: ApprovalStatus,
        deadlineOffset: Long,
        customerId: Option[CustomerId] = None,
    ): ManufacturingOrder =
      val pricelist = SamplePricelist.pricelistCzkSheet
      val basketItems = items.map { case (config, qty) =>
        val priceResult = mpbuilder.domain.pricing.PriceCalculator.calculate(config, pricelist)
        val breakdown = priceResult.toEither.toOption.get
        BasketItem(config, qty, breakdown)
      }
      val basket = Basket(BasketId.unsafe(s"basket-$id"), basketItems)
      val contact = ContactInfo(firstName, lastName, email, "", None, None, None)
      val checkoutInfo = CheckoutInfo(
        contactInfo = contact,
        deliveryOption = Some(DeliveryOption.CourierStandard),
        paymentMethod = Some(PaymentMethod.BankTransferQR),
      )
      val total = basketItems.map(_.priceBreakdown.total).reduce((a, b) => Money(a.value + b.value))
      val order = Order(OrderId.unsafe(id), basket, checkoutInfo, total, Currency.CZK, customerId)
      val deadline = now + deadlineOffset

      val workflows = if approval == ApprovalStatus.Approved then
        basketItems.zipWithIndex.map { case (item, idx) =>
          WorkflowGenerator.generate(
            item.configuration,
            order.id,
            idx,
            WorkflowId.unsafe(s"wf-$id-$idx"),
            Priority.Normal,
            Some(deadline),
            now - (deadlineOffset / 2),
          )
        }
      else Nil

      ManufacturingOrder(order, workflows, approval, "", now - (deadlineOffset / 4), Some(deadline))

    def buildConfig(
        catId: CategoryId,
        pmId: PrintingMethodId,
        components: List[ComponentRequest],
        specs: List[SpecValue],
    ): ProductConfiguration =
      ConfigurationBuilder.build(
        ConfigurationRequest(catId, pmId, components, specs),
        catalog,
        ruleset,
        ConfigurationId.unsafe(s"cfg-${catId.value}"),
      ).toEither.toOption.get

    // Build a few sample configurations
    val businessCards = buildConfig(
      SampleCatalog.businessCardsId,
      SampleCatalog.digitalId,
      List(ComponentRequest(ComponentRole.Main, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4,
        List(FinishSelection(SampleCatalog.matteLaminationId)))),
      List(SpecValue.SizeSpec(Dimension(90, 55)), SpecValue.QuantitySpec(Quantity.unsafe(500))),
    )

    val brochures = buildConfig(
      SampleCatalog.brochuresId,
      SampleCatalog.digitalId,
      List(ComponentRequest(ComponentRole.Main, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil)),
      List(SpecValue.SizeSpec(Dimension(297, 210)), SpecValue.QuantitySpec(Quantity.unsafe(200)),
        SpecValue.FoldTypeSpec(FoldType.Tri)),
    )

    val banners = buildConfig(
      SampleCatalog.bannersId,
      SampleCatalog.uvInkjetId,
      List(ComponentRequest(ComponentRole.Main, SampleCatalog.vinylId, InkConfiguration.cmyk4_0, Nil)),
      List(SpecValue.SizeSpec(Dimension(1000, 500)), SpecValue.QuantitySpec(Quantity.unsafe(5))),
    )

    val booklet = buildConfig(
      SampleCatalog.bookletsId,
      SampleCatalog.digitalId,
      List(
        ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4,
          List(FinishSelection(SampleCatalog.matteLaminationId))),
        ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy115gsmId, InkConfiguration.cmyk4_4, Nil),
      ),
      List(SpecValue.SizeSpec(Dimension(210, 297)), SpecValue.QuantitySpec(Quantity.unsafe(100)),
        SpecValue.PagesSpec(16), SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch)),
    )

    val stickers = buildConfig(
      SampleCatalog.stickersId,
      SampleCatalog.digitalId,
      List(ComponentRequest(ComponentRole.Main, SampleCatalog.adhesiveStockId, InkConfiguration.cmyk4_0,
        List(FinishSelection(SampleCatalog.kissCutId)))),
      List(SpecValue.SizeSpec(Dimension(50, 50)), SpecValue.QuantitySpec(Quantity.unsafe(1000))),
    )

    val hour = 3600000L
    val day = 86400000L

    val order1 = makeOrder("ORD-001", "Jan", "Novák", "jan@example.com",
      List((businessCards, 500)), ApprovalStatus.Approved, 2 * day)
      .copy(priority = Priority.Normal, paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, "Files OK"))
    val order2 = makeOrder("ORD-002", "Marie", "Svobodová", "marie@example.com",
      List((brochures, 200)), ApprovalStatus.Approved, 3 * day)
      .copy(priority = Priority.Normal, paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, ""))
    val order3 = makeOrder("ORD-003", "Petr", "Dvořák", "petr@example.com",
      List((banners, 5)), ApprovalStatus.Placed, 5 * day)
      .copy(paymentStatus = PaymentStatus.Pending,
        artworkCheck = ArtworkCheck(CheckStatus.NotChecked, CheckStatus.NotChecked, CheckStatus.NotChecked, ""))
    val order4 = makeOrder("ORD-004", "Eva", "Černá", "eva@example.com",
      List((booklet, 100)), ApprovalStatus.Approved, 4 * day)
      .copy(priority = Priority.Rush, paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Warning, "sRGB color profile — converted to CMYK"))
    val order5 = makeOrder("ORD-005", "Tomáš", "Procházka", "tomas@example.com",
      List((stickers, 1000), (businessCards, 500)), ApprovalStatus.Placed, 1 * day)
      .copy(priority = Priority.Rush, paymentStatus = PaymentStatus.Pending,
        artworkCheck = ArtworkCheck(CheckStatus.Failed, CheckStatus.Passed, CheckStatus.NotChecked, "Resolution only 72 DPI — customer needs to resend"))
    val order6 = makeOrder("ORD-006", "Lukáš", "Kučera", "lukas@example.com",
      List((brochures, 200)), ApprovalStatus.Approved, 6 * hour)
      .copy(priority = Priority.Low, paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, ""))

    // Simulate some progress on order1 (prepress completed)
    val order1WithProgress = order1.copy(
      workflows = order1.workflows.map { wf =>
        val updated = wf.copy(
          steps = wf.steps.map { s =>
            if s.stationType == StationType.Prepress then
              s.copy(status = StepStatus.Completed, completedAt = Some(now - hour))
            else s
          },
          status = WorkflowStatus.InProgress,
        )
        updated.evaluateReadiness
      }
    )

    // ── Sample orders for Print Shop Pro (IČO 12345678) ─────────────────
    val pspId = Some(SampleCustomers.printShopProId)
    val pspContact = ContactInfo("Jan", "Novák", "jan@printshoppro.cz", "+420 123 456 789",
      Some("Print Shop Pro s.r.o."), Some("12345678"), Some("CZ12345678"))

    // PSP-001: Business Cards — approved, in production, payment confirmed, artwork OK
    val pspOrder1Raw = makeOrder("PSP-001", "Jan", "Novák", "jan@printshoppro.cz",
      List((businessCards, 1000)), ApprovalStatus.Approved, 3 * day, pspId)
      .copy(priority = Priority.Normal, paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, "Files OK"))
    val pspOrder1 = pspOrder1Raw.copy(
      order = pspOrder1Raw.order.copy(checkoutInfo = pspOrder1Raw.order.checkoutInfo.copy(contactInfo = pspContact)),
      workflows = pspOrder1Raw.workflows.map { wf =>
        val updated = wf.copy(
          steps = wf.steps.map { s =>
            if s.stationType == StationType.Prepress || s.stationType == StationType.DigitalPrinter then
              s.copy(status = StepStatus.Completed, completedAt = Some(now - 2 * hour))
            else s
          },
          status = WorkflowStatus.InProgress,
        )
        updated.evaluateReadiness
      }
    )

    // PSP-002: Brochures — artwork changes requested (PendingChanges), payment confirmed
    val pspOrder2Raw = makeOrder("PSP-002", "Jan", "Novák", "jan@printshoppro.cz",
      List((brochures, 300)), ApprovalStatus.PendingChanges, 5 * day, pspId)
      .copy(paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Failed, CheckStatus.Passed, CheckStatus.Warning,
          "Resolution only 72 DPI — please resend at 300 DPI minimum. Color profile is sRGB, needs CMYK conversion."),
        approvalNotes = "Artwork files require corrections before we can proceed. Please re-upload with higher resolution.")
    val pspOrder2 = pspOrder2Raw.copy(
      order = pspOrder2Raw.order.copy(checkoutInfo = pspOrder2Raw.order.checkoutInfo.copy(contactInfo = pspContact))
    )

    // PSP-003: Stickers — placed, awaiting payment, artwork not checked
    val pspOrder3Raw = makeOrder("PSP-003", "Jan", "Novák", "jan@printshoppro.cz",
      List((stickers, 500)), ApprovalStatus.Placed, 7 * day, pspId)
      .copy(paymentStatus = PaymentStatus.Pending,
        artworkCheck = ArtworkCheck.unchecked)
    val pspOrder3 = pspOrder3Raw.copy(
      order = pspOrder3Raw.order.copy(checkoutInfo = pspOrder3Raw.order.checkoutInfo.copy(contactInfo = pspContact))
    )

    // PSP-004: Booklets — fully dispatched (appears in "Recent Orders" last 30 days)
    val pspOrder4Raw = makeOrder("PSP-004", "Jan", "Novák", "jan@printshoppro.cz",
      List((booklet, 50)), ApprovalStatus.Approved, day, pspId)
      .copy(priority = Priority.Normal, paymentStatus = PaymentStatus.Confirmed,
        artworkCheck = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, "All checks passed"),
        fulfilment = Some(FulfilmentChecklist(
          collectedItems = List(CollectedItem(0, collected = true, verifiedBy = Some(EmployeeId.unsafe("emp-1")))),
          qualitySignOff = QualitySignOff(passed = true, signedBy = Some(EmployeeId.unsafe("emp-2")), notes = "Quality OK"),
          packagingInfo = PackagingInfo(Some(PackagingType.Box), Some("30×20×5 cm"), Some("0.8")),
          dispatchInfo = DispatchInfo(dispatched = true, trackingNumber = "CZ123456789CZ",
            dispatchedAt = Some(now - 2 * day), dispatchedBy = Some(EmployeeId.unsafe("emp-1"))),
        ))
      )
    val pspOrder4Completed = pspOrder4Raw.copy(
      order = pspOrder4Raw.order.copy(checkoutInfo = pspOrder4Raw.order.checkoutInfo.copy(contactInfo = pspContact)),
      workflows = pspOrder4Raw.workflows.map(wf =>
        wf.copy(
          status = WorkflowStatus.Completed,
          steps = wf.steps.map(s => s.copy(status = StepStatus.Completed, completedAt = Some(now - 3 * day))),
        )
      ),
      createdAt = now - 10 * day,
    )

    List(order1WithProgress, order2, order3, order4, order5, order6,
      pspOrder1, pspOrder2, pspOrder3, pspOrder4Completed)

  private def generateSampleEmployees(): List[Employee] =
    List(
      Employee(
        EmployeeId.unsafe("emp-1"),
        "Jan Novák",
        Set(StationType.DigitalPrinter, StationType.Cutter, StationType.Laminator),
        isActive = true,
      ),
      Employee(
        EmployeeId.unsafe("emp-2"),
        "Marie Svobodová",
        Set(StationType.Prepress, StationType.QualityControl),
        isActive = true,
      ),
      Employee(
        EmployeeId.unsafe("emp-3"),
        "Petr Dvořák",
        Set(StationType.OffsetPress, StationType.LargeFormatPrinter, StationType.Cutter),
        isActive = true,
      ),
      Employee(
        EmployeeId.unsafe("emp-4"),
        "Eva Černá",
        Set(StationType.Folder, StationType.Binder, StationType.Packaging),
        isActive = true,
      ),
      Employee(
        EmployeeId.unsafe("emp-5"),
        "Tomáš Procházka",
        Set(StationType.UVCoater, StationType.EmbossingFoil, StationType.LargeFormatFinishing),
        isActive = false,
      ),
    )

  private def generateSampleMachines(): List[Machine] =
    List(
      Machine(
        MachineId.unsafe("mach-1"),
        "Konica Minolta C4080",
        StationType.DigitalPrinter,
        MachineStatus.Online,
        "CMYK calibrated, 300gsm coated loaded",
      ),
      Machine(
        MachineId.unsafe("mach-2"),
        "Konica Minolta C3080",
        StationType.DigitalPrinter,
        MachineStatus.Maintenance,
        "Fuser replacement scheduled",
      ),
      Machine(
        MachineId.unsafe("mach-3"),
        "Heidelberg Speedmaster 52",
        StationType.OffsetPress,
        MachineStatus.Online,
        "",
      ),
      Machine(
        MachineId.unsafe("mach-4"),
        "Zünd G3 L-2500",
        StationType.Cutter,
        MachineStatus.Online,
        "Kiss-cut blade installed",
      ),
      Machine(
        MachineId.unsafe("mach-5"),
        "GMP QTOPIC-380",
        StationType.Laminator,
        MachineStatus.Online,
        "Matte film loaded",
      ),
      Machine(
        MachineId.unsafe("mach-6"),
        "Roland TrueVIS VG3-640",
        StationType.LargeFormatPrinter,
        MachineStatus.Online,
        "",
      ),
      Machine(
        MachineId.unsafe("mach-7"),
        "Duplo DC-618",
        StationType.Folder,
        MachineStatus.Offline,
        "Paper jam — awaiting service",
      ),
      Machine(
        MachineId.unsafe("mach-8"),
        "Horizon BQ-470",
        StationType.Binder,
        MachineStatus.Online,
        "Perfect binding setup",
      ),
    )
