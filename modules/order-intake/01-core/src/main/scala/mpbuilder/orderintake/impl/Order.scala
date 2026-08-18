package mpbuilder.orderintake
package impl

import mpbuilder.commons.*

private[orderintake] final case class Order(id: Order.Id, data: Order.Data)

private[orderintake] object Order:

  opaque type Id = String
  object Id:
    def apply(raw: String): Id = raw
    extension (id: Id) def value: String = id

  /** Year-scoped and human-quotable: "MP-2026-000173".
    *
    * A customer reads this out on the phone, so it is short, sortable and cannot collide across
    * years. No distributed-id machinery, because there is one process issuing them.
    */
  final case class Number(year: Int, sequence: Int):
    override def toString: String = f"MP-$year-$sequence%06d"

  /** Where an order is in its commercial life.
    *
    * Three groups with leaves underneath, rather than one flat enum. Rules dispatch on the
    * *group* — "can the customer still cancel this?" is one case, not four — so a new leaf can be
    * added without touching a single existing match.
    *
    * Every leaf carries its own evidence. `Cancelled` cannot exist without a reason and an actor;
    * `Refunded` cannot exist without the cancellation that caused it. The alternative, which this
    * replaces, is a parallel history table with nullable `reason` and `by` columns where nothing
    * stops a cancellation that says nothing about why.
    */
  sealed trait Status:
    /** The coarse group, as a stable string. Rules use the types; this is for storage and for
      * queries, which must keep working when a leaf is added.
      */
    def group: String = this match
      case _: Status.Open                => "open"
      case _: Status.Active              => "active"
      case _: Status.Closed.Fulfilled    => "closed_fulfilled"
      case _: Status.Closed.Unfulfilled  => "closed_unfulfilled"

    /** The leaf, as a stable string, for display and storage. */
    def kind: String = getClass.getSimpleName.stripSuffix("$")

  object Status:

    /** Commercially live; production not yet authorised. The customer may still act. */
    sealed trait Open extends Status

    /** Production authorised; the shop owes the customer goods. */
    sealed trait Active extends Status

    /** Terminal — no further work will happen. */
    sealed trait Closed extends Status
    object Closed:
      /** The customer got what they paid for. */
      sealed trait Fulfilled extends Closed
      /** Nobody got anything, and the leaf says why. */
      sealed trait Unfulfilled extends Closed

    final case class AwaitingPayment(due: Timestamp) extends Open
    final case class Paid(at: Timestamp, reference: String) extends Open

    final case class InProduction(since: Timestamp) extends Active

    final case class Completed(dispatchedAt: Timestamp, tracking: Option[String])
        extends Closed.Fulfilled

    final case class Cancelled(at: Timestamp, by: Actor, reason: String) extends Closed.Unfulfilled
    final case class Rejected(at: Timestamp, by: Actor, reason: String) extends Closed.Unfulfilled
    final case class Refunded(at: Timestamp, amount: Money, cancellation: Cancelled)
        extends Closed.Unfulfilled

  /** One thing bought, frozen at the moment it was bought. */
  final case class Line(id: Line.Id, data: Line.Data)
  object Line:
    opaque type Id = String
    object Id:
      def apply(raw: String): Id = raw
      extension (id: Id) def value: String = id

    final case class Data(
        lineNo: Int,
        /** The catalog snapshot, opaque here as everywhere else in this context. */
        spec: ProductSpec,
        /** Rendered once, at placement, so the line reads the same in ten years. */
        description: LocalizedString,
        quantity: Int,
        speed: ProductionSpeed,
        artworkId: Option[String],
        price: QuotedPrice,
    )

  /** What the order cost, frozen. Recomputing any of this later would be re-writing history. */
  final case class Totals(
      items: Money,
      discount: Money,
      delivery: Money,
      grand: Money,
      currency: Currency,
  )

  final case class Transition(from: Option[Status], to: Status, at: Timestamp, by: Actor)

  final case class Data(
      number: Number,
      status: Status,
      buyer: OrderBuyer,
      customerId: Option[String],
      lines: List[Line],
      billing: Address,
      /** Absent means "same as billing" — not "unknown". */
      shipping: Option[Address],
      delivery: Option[DeliveryCharge],
      payment: PaymentMethod,
      discount: Option[DiscountDecision],
      totals: Totals,
      speed: ProductionSpeed,
      note: Option[String],
      placedAt: Timestamp,
      history: List[Transition],
      version: Long,
  )

  extension (o: Order)
    def status: Status = o.data.status

    def isOwnedBy(actor: Actor): Boolean = actor match
      case Actor.Anonymous(_) => false
      case Actor.Authenticated(userId, customerId, _) =>
        o.data.customerId.exists(id => customerId.contains(id) || id == userId)

    /** Staff see every order; a customer sees their own. A guest order has no owner to match, so
      * it is reachable only through the response that created it.
      */
    def isVisibleTo(actor: Actor): Boolean = actor match
      case Actor.Authenticated(_, _, isStaff) if isStaff => true
      case other                                         => o.isOwnedBy(other)
