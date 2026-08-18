package mpbuilder.orderintake

import mpbuilder.commons.*
import mpbuilder.orderintake.impl.{Order, OrderPolicy}
import zio.test.*

/** The order's rules, tested directly.
  *
  * The transition table is the whole point of this suite. An order is the first thing in the system
  * a customer can hold the shop to, and "which state may follow which" is exactly the knowledge
  * that rots when it is spread across the handlers that happen to need it.
  */
object OrderPolicySpec extends ZIOSpecDefault:

  private val now = Timestamp(1_700_000_000_000L)
  private val later = now.plusDays(1)

  private val customer = Actor.Authenticated("user-1", Some("cust-1"), isStaff = false)
  private val staff = Actor.Authenticated("user-2", None, isStaff = true)
  private val guest = Actor.Anonymous("session-1")

  private def orderWith(status: Order.Status): Order =
    Order(
      Order.Id("order-1"),
      Order.Data(
        number = Order.Number(2026, 173),
        status = status,
        buyer = OrderBuyer.Individual("Jan", "Novák", "jan@example.com", "+420123456789"),
        customerId = Some("cust-1"),
        lines = Nil,
        billing = Address("Street 1", "Praha", "11000", "CZ"),
        shipping = None,
        delivery = None,
        payment = PaymentMethod.BankTransferQR,
        discount = None,
        totals = Order.Totals(Money(100), Money.zero, Money.zero, Money(100), Currency.CZK),
        speed = ProductionSpeed.Standard,
        note = None,
        approvedForProduction = None,
        placedAt = now,
        history = Nil,
        version = 0,
      ),
    )

  private val awaitingPayment = orderWith(Order.Status.AwaitingPayment(now.plusDays(7)))
  private val paid = orderWith(Order.Status.Paid(now, "VS-1"))
  private val inProduction = orderWith(Order.Status.InProduction(now))
  private val completed = orderWith(Order.Status.Completed(now, None))
  private val cancelled =
    orderWith(Order.Status.Cancelled(now, customer, "changed my mind"))

  private def validPlaceOrder = PlaceOrder(
    buyer = OrderBuyer.Individual("Jan", "Novák", "jan@example.com", "+420123456789"),
    billing = Address("Street 1", "Praha", "11000", "CZ"),
    paymentMethod = "BankTransferQR",
  )

  def spec = suite("OrderPolicy")(
    suite("status groups")(
      test("every status reports the group its rules dispatch on") {
        assertTrue(
          awaitingPayment.status.group == "open",
          paid.status.group == "open",
          inProduction.status.group == "active",
          completed.status.group == "closed_fulfilled",
          cancelled.status.group == "closed_unfulfilled",
        )
      },
      test("the leaf is reported separately from the group") {
        // A client branches on the group and displays the leaf. Conflating them is what makes a
        // new status a breaking change.
        assertTrue(
          awaitingPayment.status.kind == "AwaitingPayment",
          completed.status.kind == "Completed",
          cancelled.status.kind == "Cancelled",
        )
      },
    ),
    suite("placing")(
      test("a new order is awaiting payment, not paid") {
        assertTrue(
          OrderPolicy.initialStatus(PaymentMethod.BankTransferQR, now).group == "open",
          OrderPolicy.initialStatus(PaymentMethod.BankTransferQR, now).kind == "AwaitingPayment",
        )
      },
      test("an invoiced order is also awaiting payment, with a longer term") {
        // Marking an unsettled invoice as Paid would misreport the shop's own position the moment
        // anyone looked at the order list.
        val transfer = OrderPolicy.initialStatus(PaymentMethod.BankTransferQR, now)
        val invoice = OrderPolicy.initialStatus(PaymentMethod.InvoiceOnAccount, now)
        assertTrue(
          invoice.kind == "AwaitingPayment",
          (transfer, invoice) match
            case (Order.Status.AwaitingPayment(a), Order.Status.AwaitingPayment(b)) =>
              b.epochMillis > a.epochMillis
            case _ => false,
        )
      },
    ),
    suite("cancelling")(
      test("a customer may cancel while the order is open") {
        assertTrue(
          OrderPolicy.canCustomerCancel(awaitingPayment),
          OrderPolicy.canCustomerCancel(paid),
          OrderPolicy.cancel(awaitingPayment, customer, "changed my mind", later).isRight,
        )
      },
      test("a customer may not cancel once the shop is making it") {
        assertTrue(
          !OrderPolicy.canCustomerCancel(inProduction),
          OrderPolicy.cancel(inProduction, customer, "changed my mind", later) ==
            Left(OrderError.StaffOnly),
        )
      },
      test("staff may cancel an order in production") {
        assertTrue(OrderPolicy.cancel(inProduction, staff, "machine broke", later).isRight)
      },
      test("a closed order cannot be cancelled again") {
        assertTrue(
          OrderPolicy.cancel(completed, staff, "too late", later) ==
            Left(OrderError.AlreadyClosed("Completed")),
          OrderPolicy.cancel(cancelled, staff, "again", later) ==
            Left(OrderError.AlreadyClosed("Cancelled")),
        )
      },
      test("cancelling records who did it and why") {
        // The evidence lives in the status itself, so a cancellation that says nothing about why
        // is not representable.
        val result = OrderPolicy.cancel(awaitingPayment, customer, "ordered by mistake", later)
        assertTrue(result.map(_.status) match
          case Right(Order.Status.Cancelled(at, by, reason)) =>
            at == later && by == customer && reason == "ordered by mistake"
          case _ => false
        )
      },
      test("cancelling appends to the history, keeping the state it came from") {
        val result = OrderPolicy.cancel(awaitingPayment, customer, "mistake", later)
        assertTrue(result.map(_.data.history) match
          case Right(List(transition)) =>
            transition.from.contains(awaitingPayment.status) &&
              transition.to.kind == "Cancelled" &&
              transition.by == Order.By.Person(customer)
          case _ => false
        )
      },
    ),
    suite("payment")(
      test("staff may record that money arrived") {
        assertTrue(OrderPolicy.confirmPayment(awaitingPayment, staff, "VS-99", later).map(_.status) match
          case Right(Order.Status.Paid(at, reference)) => at == later && reference == "VS-99"
          case _                                       => false
        )
      },
      test("a customer may not mark their own order paid") {
        assertTrue(
          OrderPolicy.confirmPayment(awaitingPayment, customer, "VS-99", later) ==
            Left(OrderError.StaffOnly),
          OrderPolicy.confirmPayment(awaitingPayment, guest, "VS-99", later) ==
            Left(OrderError.StaffOnly),
        )
      },
      test("an order already paid cannot be paid again") {
        assertTrue(
          OrderPolicy.confirmPayment(paid, staff, "VS-99", later) ==
            Left(OrderError.IllegalTransition("Paid", "Paid"))
        )
      },
    ),
    suite("rejecting")(
      test("staff may reject an open order") {
        assertTrue(OrderPolicy.reject(awaitingPayment, staff, "cannot print this", later).isRight)
      },
      test("an order in production is cancelled, not rejected") {
        // Refusing work already under way owes the customer their money back, which is what a
        // cancellation means and a rejection does not.
        assertTrue(
          OrderPolicy.reject(inProduction, staff, "too late", later) ==
            Left(OrderError.IllegalTransition("InProduction", "Rejected"))
        )
      },
      test("a customer may not reject their own order") {
        assertTrue(
          OrderPolicy.reject(awaitingPayment, customer, "nope", later) == Left(OrderError.StaffOnly)
        )
      },
    ),
    suite("production")(
      test("production starts once the order is paid and the work approved") {
        assertTrue(OrderPolicy.startProduction(paid, later).map(_.status.kind) == Right("InProduction"))
      },
      test("approval arriving before payment is remembered, not refused") {
        // It used to be refused, which dropped it: the order then sat paid, approved, and never
        // made, because nothing would ever say "approved" again.
        val approved = OrderPolicy.startProduction(awaitingPayment, later)
        assertTrue(
          approved.map(_.status.kind) == Right("AwaitingPayment"),
          approved.map(_.data.approvedForProduction.isDefined) == Right(true),
        )
      },
      test("payment then starts production, when approval already arrived") {
        val result = for
          approved <- OrderPolicy.startProduction(awaitingPayment, later)
          paidLater <- OrderPolicy.confirmPayment(approved, staff, "VS-1", later)
        yield paidLater
        assertTrue(result.map(_.status.kind) == Right("InProduction"))
      },
      test("payment alone does not start production") {
        assertTrue(
          OrderPolicy.confirmPayment(awaitingPayment, staff, "VS-1", later).map(_.status.kind) ==
            Right("Paid")
        )
      },
      test("approving twice is a no-op, because an outbox delivers at least once") {
        val result = for
          started <- OrderPolicy.startProduction(paid, later)
          again <- OrderPolicy.startProduction(started, later)
        yield again
        assertTrue(result.map(_.status.kind) == Right("InProduction"))
      },
      test("approving a closed order is refused") {
        assertTrue(
          OrderPolicy.startProduction(cancelled, later) ==
            Left(OrderError.AlreadyClosed("Cancelled"))
        )
      },
      test("a system-driven transition records the system, not a person") {
        // Manufacturing approving work moves the order, and no person made that request. An
        // order's history naming whoever happened to be nearby would simply be false.
        assertTrue(OrderPolicy.startProduction(paid, later).map(_.data.history.last.by) ==
          Right(Order.By.System))
      },
      test("an order completes from production") {
        assertTrue(
          OrderPolicy.complete(inProduction, Some("TRACK-1"), later).map(_.status) match
            case Right(Order.Status.Completed(at, tracking)) =>
              at == later && tracking.contains("TRACK-1")
            case _ => false
        )
      },
      test("a closed order cannot complete") {
        assertTrue(
          OrderPolicy.complete(cancelled, None, later) ==
            Left(OrderError.AlreadyClosed("Cancelled"))
        )
      },
    ),
    suite("validation")(
      test("accepts complete details") {
        assertTrue(OrderPolicy.validate(validPlaceOrder).isRight)
      },
      test("reports every missing field at once, not the first") {
        // The difference between a customer fixing the form and fixing one field per attempt.
        val empty = validPlaceOrder.copy(
          buyer = OrderBuyer.Individual("", "", "not-an-email", ""),
          billing = Address("", "", "", ""),
        )
        assertTrue(OrderPolicy.validate(empty) match
          case Left(OrderError.Invalid(problems)) => problems.size >= 7
          case _                                  => false
        )
      },
      test("a business buyer must name the company") {
        val business = validPlaceOrder.copy(
          buyer = OrderBuyer.Business("Jan", "Novák", "jan@example.com", "+420123", "", None, None)
        )
        assertTrue(business.buyer.displayName.nonEmpty, OrderPolicy.validate(business).isLeft)
      },
      test("a shipping address is checked when one is given") {
        val badShipping = validPlaceOrder.copy(shipping = Some(Address("", "", "", "")))
        assertTrue(OrderPolicy.validate(badShipping) match
          case Left(OrderError.Invalid(problems)) =>
            problems.exists(_.message(Language.En).contains("shipping"))
          case _ => false
        )
      },
      test("an address is not required for the shipping that was not given") {
        assertTrue(OrderPolicy.validate(validPlaceOrder.copy(shipping = None)).isRight)
      },
      test("rejects an e-mail address that is not one") {
        val bad = List("plain", "no@domain", "@example.com", "two@@example.com", "trailing@dot.")
        assertTrue(bad.forall { email =>
          OrderPolicy
            .validate(
              validPlaceOrder.copy(buyer = OrderBuyer.Individual("Jan", "Novák", email, "+420123"))
            )
            .isLeft
        })
      },
      test("problems are localized to both languages") {
        assertTrue(OrderPolicy.validate(validPlaceOrder.copy(billing = Address("", "", "", ""))) match
          case Left(error) => error.message(Language.Cs) != error.message(Language.En)
          case _           => false
        )
      },
    ),
  )
