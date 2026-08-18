package mpbuilder.orderintake

import mpbuilder.catalog.json.given
import mpbuilder.catalog as cat
import mpbuilder.commons.*
import mpbuilder.customers as cus
import mpbuilder.pricing as pri
import zio.*
import zio.json.*
import zio.test.*

/** Contract tests for [[OrderService]].
  *
  * Wired to [[Stubs]] for the same reason the basket and checkout suites are: this module cannot
  * see another context's implementation, and specifying against the contracts is what keeps these
  * tests valid when those implementations are replaced.
  *
  * Each test builds its own layer rather than sharing one. Orders accumulate — that is the point of
  * them — so a shared store would make every listing assertion depend on run order.
  */
object OrderServiceSpec extends ZIOSpecDefault:

  private val services =
    ZLayer.succeed[cat.CatalogService](Stubs.StubCatalog()) ++
      ZLayer.succeed[pri.PricingService](Stubs.StubPricing()) ++
      ZLayer.succeed[cus.CustomerService](Stubs.StubCustomers()) >>>
      OrderIntakeModule.inMemory()

  private def configuration(quantity: Int = 500): String =
    cat
      .ConfigurationRequestDto(
        categoryId = "cat-business-cards",
        printingMethodId = "pm-digital",
        components = List(
          cat.ComponentRequestDto(
            role = "Main",
            materialId = "mat-coated-300",
            ink = cat.InkConfigurationDto(cat.InkSetupDto("CMYK", 4), cat.InkSetupDto("CMYK", 4)),
          )
        ),
        specifications = cat.SpecificationsDto(
          size = Some(cat.SizeDto(90, 55)),
          quantity = Some(quantity),
        ),
      )
      .toJson

  private val guest = Actor.Anonymous("order-session")
  private val customer =
    Actor.Authenticated("user-1", Some(Stubs.StubCustomers.approvedId), isStaff = false)
  private val staff = Actor.Authenticated("user-staff", None, isStaff = true)

  private def fill(actor: Actor, copies: Int = 1) =
    BasketService.addItem(actor, AddItem(configuration(), copies))

  private val validOrder = PlaceOrder(
    buyer = OrderBuyer.Individual("Jan", "Novák", "jan@example.com", "+420123456789"),
    billing = Address("Wenceslas Square 1", "Praha", "11000", "CZ"),
    paymentMethod = "BankTransferQR",
  )

  /** The stub prices a 500-piece run at 10 each with a 20 % volume discount above 100. */
  private val onePack = Money(4000)

  def spec = suite("OrderService")(
    suite("placing")(
      test("an empty basket cannot become an order") {
        for error <- OrderService.place(guest, validOrder).flip
        yield assertTrue(error == OrderError.EmptyBasket)
      },
      test("freezes what the checkout quoted") {
        for
          _ <- fill(guest, copies = 2)
          order <- OrderService.place(guest, validOrder)
        yield assertTrue(
          order.lines.size == 1,
          order.lines.head.quantity == 2,
          order.lines.head.lineNo == 1,
          order.totals.items == Money(8000),
          order.totals.grand == Money(8000),
          order.totals.currency == Currency.CZK,
        )
      },
      test("keeps the configuration, so the line still reads years later") {
        // The snapshot rule: an order must not depend on a catalog that has moved on.
        for
          _ <- fill(guest)
          order <- OrderService.place(guest, validOrder)
        yield assertTrue(
          order.lines.head.configuration.contains("cat-business-cards"),
          order.lines.head.description(Language.En).nonEmpty,
          order.lines.head.description(Language.Cs) != order.lines.head.description(Language.En),
        )
      },
      test("a new order is awaiting payment") {
        for
          _ <- fill(guest)
          order <- OrderService.place(guest, validOrder)
        yield assertTrue(order.status.group == "open", order.status.kind == "AwaitingPayment")
      },
      test("gives the order a human-quotable number") {
        for
          _ <- fill(guest)
          order <- OrderService.place(guest, validOrder)
        yield assertTrue(order.number.matches("""MP-\d{4}-\d{6}"""))
      },
      test("numbers are unique and increasing") {
        for
          _ <- fill(guest)
          first <- OrderService.place(guest, validOrder)
          _ <- fill(guest)
          second <- OrderService.place(guest, validOrder)
        yield
          // Sortable as text, which is the point of zero-padding the sequence: an order list
          // ordered by number is ordered by age.
          val increases = second.number.compareTo(first.number) > 0
          assertTrue(first.number != second.number, increases)
      },
      test("empties the basket, so the same goods cannot be ordered twice") {
        for
          _ <- fill(guest)
          _ <- OrderService.place(guest, validOrder)
          basket <- BasketService.current(guest)
          again <- OrderService.place(guest, validOrder).flip
        yield assertTrue(basket.items.isEmpty, again == OrderError.EmptyBasket)
      },
      test("refuses incomplete details, listing every problem") {
        for
          _ <- fill(guest)
          error <- OrderService
            .place(
              guest,
              validOrder.copy(
                buyer = OrderBuyer.Individual("", "", "nonsense", ""),
                billing = Address("", "", "", ""),
              ),
            )
            .flip
        yield assertTrue(error match
          case OrderError.Invalid(problems) => problems.size >= 7
          case _                            => false
        )
      },
      test("a payment method the buyer may not use is refused") {
        for
          _ <- fill(guest)
          error <- OrderService.place(guest, validOrder.copy(paymentMethod = "InvoiceOnAccount")).flip
        yield assertTrue(error match
          case OrderError.CheckoutRefused(CheckoutError.PaymentNotOffered(method, _)) =>
            method == PaymentMethod.InvoiceOnAccount
          case _ => false
        )
      },
      test("a delivery choice is charged and frozen onto the order") {
        for
          _ <- fill(guest)
          order <- OrderService.place(
            guest,
            validOrder.copy(deliveryOptionId = Some("courier-standard")),
          )
        yield assertTrue(
          order.delivery.map(_.optionId).contains("courier-standard"),
          order.totals.delivery == Money(99),
          order.totals.grand == (onePack + Money(99)).rounded,
        )
      },
      test("a discount is frozen onto the order") {
        for
          _ <- fill(guest)
          order <- OrderService.place(guest, validOrder.copy(discountCode = Some("TENOFF")))
        yield assertTrue(
          order.totals.discount == Money(400),
          order.totals.grand == Money(3600),
          order.discount.exists(_.isInstanceOf[DiscountDecision.Applied]),
        )
      },
      test("a price that moved since the quote is refused, not charged") {
        // The only honest way to run surge pricing: the customer agreed to a number, and if it is
        // no longer the number they are asked to agree again.
        for
          _ <- fill(guest)
          error <- OrderService.place(guest, validOrder.copy(expectedTotal = Some(Money(1)))).flip
        yield assertTrue(error match
          case OrderError.PriceChanged(was, current) => was == Money(1) && current == onePack
          case _                                     => false
        )
      },
      test("a price that still matches is accepted") {
        for
          _ <- fill(guest)
          order <- OrderService.place(guest, validOrder.copy(expectedTotal = Some(onePack)))
        yield assertTrue(order.totals.grand == onePack)
      },
      test("a signed-in customer's order is linked to them; a guest's is not") {
        for
          _ <- fill(customer)
          mine <- OrderService.place(customer, validOrder)
          _ <- fill(guest)
          theirs <- OrderService.place(guest, validOrder)
        yield assertTrue(
          mine.customerId.contains(Stubs.StubCustomers.approvedId),
          theirs.customerId.isEmpty,
        )
      },
    ),
    suite("reading")(
      test("a customer sees their own order") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          found <- OrderService.find(customer, placed.id)
        yield assertTrue(found.id == placed.id, found.number == placed.number)
      },
      test("someone else's order is reported as missing, not as forbidden") {
        // "It exists but is not yours" is worth nothing to a stranger and something to a guesser.
        val other = Actor.Authenticated("user-9", Some("cust-9"), isStaff = false)
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          error <- OrderService.find(other, placed.id).flip
        yield assertTrue(error == OrderError.NotFound(placed.id))
      },
      test("staff see every order") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          found <- OrderService.find(staff, placed.id)
        yield assertTrue(found.id == placed.id)
      },
      test("a customer's list holds their orders, newest first") {
        for
          _ <- fill(customer)
          first <- OrderService.place(customer, validOrder)
          _ <- fill(customer)
          second <- OrderService.place(customer, validOrder)
          listed <- OrderService.list(customer, Page())
        yield assertTrue(
          listed.total == 2,
          listed.items.map(_.id) == List(second.id, first.id),
        )
      },
      test("a guest cannot list orders") {
        for error <- OrderService.list(guest, Page()).flip
        yield assertTrue(error == OrderError.NotAuthorised)
      },
      test("paging reports the full total, not the page size") {
        for
          _ <- ZIO.foreachDiscard(1 to 3)(_ => fill(customer) *> OrderService.place(customer, validOrder))
          page <- OrderService.list(customer, Page(offset = 0, limit = 2))
        yield assertTrue(page.items.size == 2, page.total == 3, page.limit == 2)
      },
      test("only staff may search") {
        for error <- OrderService.search(customer, OrderSearch(), Page()).flip
        yield assertTrue(error == OrderError.StaffOnly)
      },
      test("staff search filters on the status group") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          _ <- OrderService.cancel(customer, placed.id, CancelOrder("changed my mind"))
          _ <- fill(customer)
          _ <- OrderService.place(customer, validOrder)
          open <- OrderService.search(staff, OrderSearch(statusGroup = Some("open")), Page())
          closed <- OrderService.search(staff, OrderSearch(statusGroup = Some("closed_unfulfilled")), Page())
        yield assertTrue(open.total == 1, closed.total == 1, closed.items.head.id == placed.id)
      },
      test("staff search finds an order by its number") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          found <- OrderService.search(staff, OrderSearch(query = Some(placed.number)), Page())
        yield assertTrue(found.total == 1, found.items.head.id == placed.id)
      },
    ),
    suite("changing")(
      test("a customer cancels their own open order") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          cancelled <- OrderService.cancel(customer, placed.id, CancelOrder("changed my mind"))
        yield assertTrue(
          cancelled.status.group == "closed_unfulfilled",
          cancelled.status.kind == "Cancelled",
          cancelled.status.reason.contains("changed my mind"),
        )
      },
      test("a cancelled order cannot be cancelled again") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          _ <- OrderService.cancel(customer, placed.id, CancelOrder("first"))
          error <- OrderService.cancel(customer, placed.id, CancelOrder("second")).flip
        yield assertTrue(error == OrderError.AlreadyClosed("Cancelled"))
      },
      test("staff confirm payment") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          paid <- OrderService.confirmPayment(staff, placed.id, ConfirmPayment("VS-2026-1"))
        yield assertTrue(
          paid.status.kind == "Paid",
          paid.status.group == "open",
          paid.status.reason.contains("VS-2026-1"),
        )
      },
      test("a customer cannot confirm their own payment") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          error <- OrderService.confirmPayment(customer, placed.id, ConfirmPayment("VS-1")).flip
        yield assertTrue(error == OrderError.StaffOnly)
      },
      test("staff reject an order with a reason") {
        for
          _ <- fill(customer)
          placed <- OrderService.place(customer, validOrder)
          rejected <- OrderService.reject(staff, placed.id, RejectOrder("cannot print this"))
        yield assertTrue(
          rejected.status.kind == "Rejected",
          rejected.status.group == "closed_unfulfilled",
          rejected.status.reason.contains("cannot print this"),
        )
      },
      test("changing an unknown order reports it as missing") {
        for error <- OrderService.cancel(customer, "no-such-order", CancelOrder("x")).flip
        yield assertTrue(error == OrderError.NotFound("no-such-order"))
      },
    ),
  ).provide(services)
