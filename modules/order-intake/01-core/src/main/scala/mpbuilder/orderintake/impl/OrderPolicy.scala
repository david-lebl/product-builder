package mpbuilder.orderintake
package impl

import mpbuilder.commons.*
import zio.NonEmptyChunk

/** Every order rule, as pure functions.
  *
  * The transitions are the reason this file exists. An order is the first thing here a customer can
  * hold the shop to, and "which states may follow which" is exactly the knowledge that rots when it
  * is spread across the handlers that happen to need it.
  */
private[orderintake] object OrderPolicy:

  /** How long a customer has to pay before the shop stops holding the slot. */
  val BankTransferDays = 7

  /** An invoiced customer pays after delivery, so their window is the payment term, not a hold. */
  val OnAccountDays = 30

  def initialStatus(payment: PaymentMethod, now: Timestamp): Order.Status =
    payment match
      case PaymentMethod.InvoiceOnAccount =>
        // Deliberately *not* `Paid`. An invoice that has not been settled is not money in the
        // bank, and an order list that says otherwise would misreport the shop's own position.
        Order.Status.AwaitingPayment(now.plusDays(OnAccountDays))
      case _ => Order.Status.AwaitingPayment(now.plusDays(BankTransferDays))

  // ── Who may do what ─────────────────────────────────────────────────────

  /** A customer may call off an order the shop has not started making.
    *
    * Dispatching on the group rather than the leaf is the point of the hierarchy: adding a new
    * open state later needs no change here.
    */
  def canCustomerCancel(order: Order): Boolean = order.status match
    case _: Order.Status.Open   => true
    case _: Order.Status.Active => false
    case _: Order.Status.Closed => false

  def isStaff(actor: Actor): Boolean = actor match
    case Actor.Authenticated(_, _, staff) => staff
    case Actor.Anonymous(_)               => false

  // ── Transitions ─────────────────────────────────────────────────────────

  def cancel(
      order: Order,
      by: Actor,
      reason: String,
      now: Timestamp,
  ): Either[OrderError, Order] =
    order.status match
      case closed: Order.Status.Closed => Left(OrderError.AlreadyClosed(closed.kind))
      case _: Order.Status.Open        => Right(transition(order, Order.Status.Cancelled(now, by, reason), by, now))
      case _: Order.Status.Active =>
        // Work has started, so calling it off is a decision with a cost. Staff only.
        if isStaff(by) then Right(transition(order, Order.Status.Cancelled(now, by, reason), by, now))
        else Left(OrderError.StaffOnly)

  def confirmPayment(
      order: Order,
      by: Actor,
      reference: String,
      now: Timestamp,
  ): Either[OrderError, Order] =
    if !isStaff(by) then Left(OrderError.StaffOnly)
    else
      order.status match
        case _: Order.Status.AwaitingPayment =>
          Right(transition(order, Order.Status.Paid(now, reference), by, now))
        case closed: Order.Status.Closed => Left(OrderError.AlreadyClosed(closed.kind))
        case other => Left(OrderError.IllegalTransition(other.kind, "Paid"))

  def reject(
      order: Order,
      by: Actor,
      reason: String,
      now: Timestamp,
  ): Either[OrderError, Order] =
    if !isStaff(by) then Left(OrderError.StaffOnly)
    else
      order.status match
        case closed: Order.Status.Closed => Left(OrderError.AlreadyClosed(closed.kind))
        case _: Order.Status.Open        => Right(transition(order, Order.Status.Rejected(now, by, reason), by, now))
        // Once the shop has started making it, refusing it is a cancellation — which owes the
        // customer their money back — not a rejection.
        case other => Left(OrderError.IllegalTransition(other.kind, "Rejected"))

  /** Manufacturing has approved the order and started work. Reached by event, not by request. */
  def startProduction(order: Order, by: Actor, now: Timestamp): Either[OrderError, Order] =
    order.status match
      case _: Order.Status.Paid        => Right(transition(order, Order.Status.InProduction(now), by, now))
      case closed: Order.Status.Closed => Left(OrderError.AlreadyClosed(closed.kind))
      case other                       => Left(OrderError.IllegalTransition(other.kind, "InProduction"))

  def complete(
      order: Order,
      by: Actor,
      tracking: Option[String],
      now: Timestamp,
  ): Either[OrderError, Order] =
    order.status match
      case _: Order.Status.Active      => Right(transition(order, Order.Status.Completed(now, tracking), by, now))
      case closed: Order.Status.Closed => Left(OrderError.AlreadyClosed(closed.kind))
      case other                       => Left(OrderError.IllegalTransition(other.kind, "Completed"))

  private def transition(order: Order, to: Order.Status, by: Actor, now: Timestamp): Order =
    order.copy(data =
      order.data.copy(
        status = to,
        history = order.data.history :+ Order.Transition(Some(order.status), to, now, by),
        version = order.data.version + 1,
      )
    )

  // ── Buyer details ───────────────────────────────────────────────────────

  /** Check everything the customer typed, and report every problem at once.
    *
    * Accumulating rather than short-circuiting is this codebase's established contract, and it is
    * the difference between a customer fixing one field per attempt and fixing the form.
    */
  def validate(input: PlaceOrder): Either[OrderError, Unit] =
    val problems =
      buyerProblems(input.buyer) ++
        addressProblems("billing", input.billing) ++
        input.shipping.toList.flatMap(addressProblems("shipping", _))

    NonEmptyChunk.fromIterableOption(problems) match
      case Some(found) => Left(OrderError.Invalid(found))
      case None        => Right(())

  private def buyerProblems(buyer: OrderBuyer): List[Problem] =
    val base = List(
      required("buyer.firstName", buyer.firstName, "First name", "Jméno"),
      required("buyer.lastName", buyer.lastName, "Last name", "Příjmení"),
      required("buyer.phone", buyer.phone, "Phone", "Telefon"),
      emailProblem(buyer.email),
    ).flatten

    val business = buyer match
      case b: OrderBuyer.Business =>
        required("buyer.company", b.company, "Company name", "Název firmy").toList
      case _: OrderBuyer.Individual => Nil

    base ++ business

  private def addressProblems(field: String, address: Address): List[Problem] =
    List(
      required(s"$field.street", address.street, "Street", "Ulice"),
      required(s"$field.city", address.city, "City", "Město"),
      required(s"$field.zip", address.zip, "Postcode", "PSČ"),
      required(s"$field.country", address.country, "Country", "Země"),
    ).flatten

  private def required(field: String, value: String, en: String, cs: String): Option[Problem] =
    Option.when(value.trim.isEmpty)(
      Problem("Required", s"$en is required ($field)", s"$cs je povinné ($field)")
    )

  /** Deliberately shallow: an address either has the shape of one or it does not, and only sending
    * a message proves it works. Rejecting valid addresses for failing a stricter pattern is the
    * more expensive mistake.
    */
  private def emailProblem(email: String): Option[Problem] =
    val trimmed = email.trim
    val looksLikeOne =
      trimmed.count(_ == '@') == 1 && {
        val Array(local, domain) = trimmed.split("@", 2)
        local.nonEmpty && domain.contains('.') && !domain.startsWith(".") && !domain.endsWith(".")
      }
    Option.unless(looksLikeOne)(
      Problem("InvalidEmail", s"'$email' is not a valid e-mail address", s"'$email' není platná e-mailová adresa")
    )
