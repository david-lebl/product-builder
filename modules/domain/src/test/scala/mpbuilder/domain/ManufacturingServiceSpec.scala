package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

object ManufacturingServiceSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist

  private def makeItem(
      id: String,
      category: ProductCategory,
      material: Material,
      printingMethod: PrintingMethod,
      quantity: Int = 100,
  ): BasketItem =
    val config = ProductConfiguration(
      id            = ConfigurationId.unsafe(id),
      category      = category,
      printingMethod = printingMethod,
      components    = List(ProductComponent(
        role             = ComponentRole.Main,
        material         = material,
        inkConfiguration = InkConfiguration.cmyk4_4,
        finishes         = List.empty,
        sheetCount       = 1,
      )),
      specifications = ProductSpecifications.fromSpecs(List(
        SpecValue.SizeSpec(Dimension(90, 55)),
        SpecValue.QuantitySpec(Quantity.unsafe(quantity)),
      )),
    )
    val breakdown = PriceCalculator.calculate(config, pricelist).toEither.toOption.get
    BasketItem(config, quantity, breakdown)

  private val smallItem = makeItem(
    id             = "small-1",
    category       = SampleCatalog.businessCards,
    material       = SampleCatalog.coated300gsm,
    printingMethod = SampleCatalog.offsetMethod,
  )

  private val largeItem = makeItem(
    id             = "large-1",
    category       = SampleCatalog.banners,
    material       = SampleCatalog.vinyl,
    printingMethod = SampleCatalog.uvInkjetMethod,
    quantity       = 10,
  )

  private val now = 1000L

  def spec = suite("ManufacturingService")(
    suite("placeOrderFromItem")(
      test("small-format order starts as Queued") {
        val order = ManufacturingService.placeOrderFromItem(
          smallItem,
          OrderId.unsafe("ORD-1"),
          now,
        )
        assertTrue(
          order.status     == ManufacturingStatus.Queued,
          order.formatType == PrintFormatType.SmallFormat,
          order.notes      == None,
        )
      },
      test("large-format order starts as PendingApproval") {
        val order = ManufacturingService.placeOrderFromItem(
          largeItem,
          OrderId.unsafe("ORD-2"),
          now,
        )
        assertTrue(
          order.status     == ManufacturingStatus.PendingApproval,
          order.formatType == PrintFormatType.LargeFormat,
        )
      },
    ),
    suite("approve")(
      test("approves a PendingApproval order and moves it to Queued") {
        val order = ManufacturingService.placeOrderFromItem(largeItem, OrderId.unsafe("ORD-3"), now)
        val result = ManufacturingService.approve(order)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.status == ManufacturingStatus.Queued,
        )
      },
      test("fails to approve a Queued order (not pending approval)") {
        val order = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-4"), now)
        val result = ManufacturingService.approve(order)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.left.toOption.get.toList.exists(_.isInstanceOf[ManufacturingError.ApprovalNotRequired]),
        )
      },
      test("fails to approve a completed order") {
        val order    = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-5"), now)
        val terminal = order.copy(status = ManufacturingStatus.Completed)
        val result   = ManufacturingService.approve(terminal)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.left.toOption.get.toList.exists(_.isInstanceOf[ManufacturingError.OrderAlreadyTerminal]),
        )
      },
    ),
    suite("reject")(
      test("rejects a PendingApproval order and moves it to Rejected") {
        val order  = ManufacturingService.placeOrderFromItem(largeItem, OrderId.unsafe("ORD-6"), now)
        val result = ManufacturingService.reject(order, Some("Size not supported"))
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.status == ManufacturingStatus.Rejected,
          result.toEither.toOption.get.notes  == Some("Size not supported"),
        )
      },
      test("fails to reject a non-pending order") {
        val order  = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-7"), now)
        val result = ManufacturingService.reject(order)
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("advance")(
      test("advances Queued to InProduction") {
        val order  = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-8"), now)
        val result = ManufacturingService.advance(order)
        assertTrue(result.toEither.toOption.get.status == ManufacturingStatus.InProduction)
      },
      test("advances through full production workflow") {
        val order = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-9"), now)
        val result = for
          o1 <- ManufacturingService.advance(order)           // Queued → InProduction
          o2 <- ManufacturingService.advance(o1)             // InProduction → QualityCheck
          o3 <- ManufacturingService.advance(o2)             // QualityCheck → Packaging
          o4 <- ManufacturingService.advance(o3)             // Packaging → ReadyForPickup
          o5 <- ManufacturingService.advance(o4)             // ReadyForPickup → Completed
        yield o5
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.status == ManufacturingStatus.Completed,
        )
      },
      test("fails to advance a PendingApproval order") {
        val order  = ManufacturingService.placeOrderFromItem(largeItem, OrderId.unsafe("ORD-10"), now)
        val result = ManufacturingService.advance(order)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.left.toOption.get.toList.exists(_.isInstanceOf[ManufacturingError.InvalidStatusTransition]),
        )
      },
      test("fails to advance a terminal order") {
        val order    = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-11"), now)
        val terminal = order.copy(status = ManufacturingStatus.Cancelled)
        val result   = ManufacturingService.advance(terminal)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.left.toOption.get.toList.exists(_.isInstanceOf[ManufacturingError.OrderAlreadyTerminal]),
        )
      },
    ),
    suite("cancel")(
      test("cancels an active order") {
        val order  = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-12"), now)
        val result = ManufacturingService.cancel(order)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.status == ManufacturingStatus.Cancelled,
        )
      },
      test("cancels a PendingApproval order") {
        val order  = ManufacturingService.placeOrderFromItem(largeItem, OrderId.unsafe("ORD-13"), now)
        val result = ManufacturingService.cancel(order)
        assertTrue(result.toEither.toOption.get.status == ManufacturingStatus.Cancelled)
      },
      test("fails to cancel a completed order") {
        val order    = ManufacturingService.placeOrderFromItem(smallItem, OrderId.unsafe("ORD-14"), now)
        val terminal = order.copy(status = ManufacturingStatus.Completed)
        val result   = ManufacturingService.cancel(terminal)
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("nextStepLabel")(
      test("returns label for each active status") {
        assertTrue(
          ManufacturingService.nextStepLabel(ManufacturingStatus.Queued, Language.En).isDefined,
          ManufacturingService.nextStepLabel(ManufacturingStatus.InProduction, Language.En).isDefined,
          ManufacturingService.nextStepLabel(ManufacturingStatus.QualityCheck, Language.En).isDefined,
          ManufacturingService.nextStepLabel(ManufacturingStatus.Packaging, Language.En).isDefined,
          ManufacturingService.nextStepLabel(ManufacturingStatus.ReadyForPickup, Language.En).isDefined,
        )
      },
      test("returns None for non-advanceable statuses") {
        assertTrue(
          ManufacturingService.nextStepLabel(ManufacturingStatus.PendingApproval, Language.En).isEmpty,
          ManufacturingService.nextStepLabel(ManufacturingStatus.Completed, Language.En).isEmpty,
          ManufacturingService.nextStepLabel(ManufacturingStatus.Cancelled, Language.En).isEmpty,
          ManufacturingService.nextStepLabel(ManufacturingStatus.Rejected, Language.En).isEmpty,
        )
      },
    ),
    suite("PrintFormatType classification")(
      test("offset is small format") {
        assertTrue(PrintFormatType.fromProcessType(PrintingProcessType.Offset) == PrintFormatType.SmallFormat)
      },
      test("digital is small format") {
        assertTrue(PrintFormatType.fromProcessType(PrintingProcessType.Digital) == PrintFormatType.SmallFormat)
      },
      test("UV inkjet is large format") {
        assertTrue(PrintFormatType.fromProcessType(PrintingProcessType.UVCurableInkjet) == PrintFormatType.LargeFormat)
      },
      test("latex inkjet is large format") {
        assertTrue(PrintFormatType.fromProcessType(PrintingProcessType.LatexInkjet) == PrintFormatType.LargeFormat)
      },
      test("solvent inkjet is large format") {
        assertTrue(PrintFormatType.fromProcessType(PrintingProcessType.SolventInkjet) == PrintFormatType.LargeFormat)
      },
    ),
  )
