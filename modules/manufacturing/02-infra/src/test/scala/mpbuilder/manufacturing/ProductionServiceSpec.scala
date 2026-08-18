package mpbuilder.manufacturing

import mpbuilder.commons.*
import mpbuilder.catalog.codec.ConfigurationCodecs.given
import mpbuilder.catalog.model as cm
import mpbuilder.domain.model as dm
import mpbuilder.catalog.model.*
import mpbuilder.catalog.sample.{SampleCatalog, SampleRules}
import mpbuilder.catalog.service.{ConfigurationBuilder, ConfigurationRequest}
import mpbuilder.orderintake as oi
import zio.*
import zio.json.*
import zio.test.*

/** Contract tests for [[ProductionService]], including the translation from order-intake.
  *
  * The translation is the point of the phase: the shop floor is *told* what to make and never
  * reaches back for it. What arrives is a snapshot, and these tests are what stop that quietly
  * becoming a lookup again.
  */
object ProductionServiceSpec extends ZIOSpecDefault:

  private val services = ManufacturingModule.inMemory

  private def spec(quantity: Int = 500): String =
    ConfigurationBuilder
      .build(
        ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            cm.ComponentRequest(
              cm.ComponentRole.Main,
              SampleCatalog.coated300gsmId,
              cm.InkConfiguration.cmyk4_4,
              Nil,
            )
          ),
          specs = List(
            cm.SpecValue.SizeSpec(Dimension(90, 55)),
            cm.SpecValue.QuantitySpec(Quantity.unsafe(quantity)),
          ),
        ),
        SampleCatalog.catalog,
        SampleRules.ruleset,
        cm.ConfigurationId.unsafe("fixture"),
      )
      .toEither
      .fold(errors => throw new AssertionError(s"fixture did not build: $errors"), _.toJson)

  private val placedAt = Timestamp(1_700_000_000_000L)

  private def placedEvent(orderId: String = "order-1", lines: Int = 1): oi.OrderEvent.OrderPlaced =
    oi.OrderEvent.OrderPlaced(
      orderId = orderId,
      number = "MP-2026-000173",
      buyerName = "Print Shop Pro",
      customerId = Some("cust-1"),
      lines = (1 to lines).toList.map(n =>
        oi.OrderLineView(
          id = s"line-$n",
          lineNo = n,
          description = LocalizedString("500× Business Cards", "500× Vizitky"),
          configuration = spec(),
          quantity = 2,
          speed = "Standard",
          artworkId = None,
          price = oi.QuotedPrice(Money(1000), Currency.CZK, placedAt, "pricelist-v1"),
        )
      ),
      speed = "Standard",
      total = Money(1000),
      currency = Currency.CZK,
      placedAt = placedAt,
    )

  private def accept(event: oi.OrderEvent) =
    ZIO.serviceWithZIO[ProductionService](ManufacturingInbound.accept(_, event))

  def spec = suite("ProductionService")(
    suite("taking work from order-intake")(
      test("a placed order becomes a production order awaiting approval") {
        for accepted <- accept(placedEvent())
        yield assertTrue(accepted.exists(_.status == ProductionStatus.AwaitingApproval))
      },
      test("the work is planned into real steps") {
        // The whole reason the spec travels with the event: the shop has to know what to do, and
        // it cannot ask the catalog what this product was.
        for accepted <- accept(placedEvent())
        yield
          val order = accepted.get
          assertTrue(
            order.stepCount > 0,
            order.lines.head.steps.map(_.station).contains("Prepress"),
            order.lines.head.steps.exists(_.station.contains("Printer")),
          )
      },
      test("steps depend on each other, so the floor knows the order to work in") {
        for accepted <- accept(placedEvent())
        yield
          val steps = accepted.get.lines.head.steps
          assertTrue(
            steps.exists(_.dependsOn.isEmpty),
            steps.exists(_.dependsOn.nonEmpty),
            // Every dependency names a step that exists in the same line.
            steps.flatMap(_.dependsOn).forall(id => steps.exists(_.id == id)),
          )
      },
      test("the order's own line id is carried across, not its position") {
        // Position quietly means something else as soon as a line is removed.
        for accepted <- accept(placedEvent(lines = 2))
        yield assertTrue(accepted.get.lines.map(_.lineId) == List("line-1", "line-2"))
      },
      test("what the shop floor is told is a snapshot, with no way back to the order") {
        for accepted <- accept(placedEvent())
        yield
          val order = accepted.get
          assertTrue(
            order.orderId == "order-1",
            order.orderNumber == "MP-2026-000173",
            order.customerName == "Print Shop Pro",
            order.lines.head.description(Language.Cs) != order.lines.head.description(Language.En),
          )
      },
      test("delivering the same order twice does not make it twice") {
        // An outbox delivers at least once, so this has to be a no-op rather than a second job.
        for
          first <- accept(placedEvent())
          second <- accept(placedEvent())
          queue <- ProductionService.queue
        yield assertTrue(first.get.id == second.get.id, queue.size == 1)
      },
      test("an event that is not work is ignored") {
        for
          accepted <- accept(
            oi.OrderEvent.OrderPaid("order-1", "MP-2026-000173", "BankTransferQR", "VS-1", placedAt)
          )
          queue <- ProductionService.queue
        yield assertTrue(accepted.isEmpty, queue.isEmpty)
      },
      test("a spec the shop cannot read is refused, and says so in both languages") {
        val broken = placedEvent().copy(lines =
          List(placedEvent().lines.head.copy(configuration = "{not a configuration}"))
        )
        for error <- accept(broken).flip
        yield assertTrue(error match
          case ProductionError.CannotPlan(problems) =>
            problems.head.message(Language.En) != problems.head.message(Language.Cs)
          case _ => false
        )
      },
    ),
    suite("the queue")(
      test("holds what is awaiting approval, and drops it once approved") {
        for
          accepted <- accept(placedEvent())
          before <- ProductionService.queue
          _ <- ProductionService.approve(accepted.get.id, "operator-1")
          after <- ProductionService.queue
        yield assertTrue(before.size == 1, after.isEmpty)
      },
      test("is oldest first, because it is a queue") {
        for
          first <- accept(placedEvent(orderId = "order-1"))
          second <- accept(placedEvent(orderId = "order-2"))
          queue <- ProductionService.queue
        yield assertTrue(queue.map(_.id) == List(first.get.id, second.get.id))
      },
    ),
    suite("approving and dispatching")(
      test("approving records who authorised it") {
        for
          accepted <- accept(placedEvent())
          approved <- ProductionService.approve(accepted.get.id, "operator-1")
        yield assertTrue(approved.status match
          case ProductionStatus.Approved(_, by) => by == "operator-1"
          case _                                => false
        )
      },
      test("approving twice is refused") {
        for
          accepted <- accept(placedEvent())
          _ <- ProductionService.approve(accepted.get.id, "operator-1")
          error <- ProductionService.approve(accepted.get.id, "operator-2").flip
        yield assertTrue(error == ProductionError.AlreadyApproved(accepted.get.id))
      },
      test("nothing is dispatched that nobody approved") {
        // Shipping unapproved work sends goods the shop never agreed to make.
        for
          accepted <- accept(placedEvent())
          error <- ProductionService.dispatch(accepted.get.id, None).flip
        yield assertTrue(error == ProductionError.NotApproved(accepted.get.id))
      },
      test("dispatching keeps the tracking code") {
        for
          accepted <- accept(placedEvent())
          _ <- ProductionService.approve(accepted.get.id, "operator-1")
          dispatched <- ProductionService.dispatch(accepted.get.id, Some("TRACK-9"))
        yield assertTrue(dispatched.status match
          case ProductionStatus.Dispatched(_, tracking) => tracking.contains("TRACK-9")
          case _                                        => false
        )
      },
      test("an unknown production order is reported as missing") {
        for error <- ProductionService.approve("no-such-order", "operator-1").flip
        yield assertTrue(error == ProductionError.NotFound("no-such-order"))
      },
    ),
    suite("what it reports back")(
      test("approval and dispatch are published for order-intake to pick up") {
        for
          accepted <- accept(placedEvent())
          _ <- ProductionService.approve(accepted.get.id, "operator-1")
          _ <- ProductionService.dispatch(accepted.get.id, Some("TRACK-9"))
          outbox <- ZIO.service[ProductionOutbox]
          pending <- outbox.pending(10)
        yield assertTrue(
          pending.size == 2,
          pending.head.event.isInstanceOf[ProductionEvent.OrderApproved],
          pending.last.event.isInstanceOf[ProductionEvent.OrderDispatched],
          // Named by the commercial order, because that is what the other side knows.
          pending.forall(_.event.orderId == "order-1"),
        )
      },
      test("an entry stays until the relay says it took it") {
        // At-least-once: a relay that dies mid-batch must re-deliver, not lose.
        for
          accepted <- accept(placedEvent())
          _ <- ProductionService.approve(accepted.get.id, "operator-1")
          outbox <- ZIO.service[ProductionOutbox]
          before <- outbox.pending(10)
          stillThere <- outbox.pending(10)
          _ <- outbox.markPublished(before.map(_.id))
          after <- outbox.pending(10)
        yield assertTrue(before.size == 1, stillThere.size == 1, after.isEmpty)
      },
    ),
  ).provide(services)
