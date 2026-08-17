# Order Intake — Bounded Context Design

> **Status:** proposed. The first context to be built for real against a database and an API.
> Assumes the module conventions, dependency rules and modeling conventions from
> [modular-architecture.md](modular-architecture.md).

## 1. Why this context first

Order intake is the only part of the system that is **completely fake today**. `Order` in
`modules/domain/src/main/scala/mpbuilder/domain/model/order.scala` is a 98-line skeleton with no
status, no timestamps, no order number, no line-item snapshot (it holds a *live* `Basket`), no
`OrderService`, and **zero tests**. `CheckoutView.scala`'s "✓ Place Order" clears the basket and
navigates away. Everything downstream — the customer portal, order history, and the whole
manufacturing module — is fed from `sample/` fixtures.

It is also the context that forces every other boundary to become real: it needs catalog to validate
a configuration, pricing to quote it, customers to identify the buyer, identity to authenticate, and
manufacturing to consume what it produces.

## 2. Boundary

| Concern | Owner | Order-intake's own view of it |
|---|---|---|
| What can be built | catalog | `ProductSpec` — an opaque, versioned snapshot it never interprets |
| What it costs | pricing | `PriceSnapshot` + `Totals`; order-intake never computes a price |
| Who the customer is | customers | `Buyer` + an optional `Customer.Id` |
| Who is signed in | identity | `Actor` derived from a verified token |
| How it gets made | manufacturing | nothing — it only **publishes** `OrderPlaced` |
| Basket, checkout, order, order history | **order-intake** | owned outright |

Under the dependency rules, `order-intake/01-core` depends on `commons` **only**. It does not import
catalog's `ProductConfiguration` or pricing's `PriceBreakdown`. It declares its own vocabulary and
its own ports (§5.2); `02-infra` adapters translate (§6.3).

### The snapshot rule

> An `Order` must never reference live catalog or pricing entities.

An order line stores a serialized `ProductSpec` (with its `catalogVersion`) and a serialized
`PriceSnapshot` (with its `pricelistVersion`). A catalog edit six months later must not retroactively
change what a customer bought.

Today `Order` holds a live `Basket` holding live `ProductConfiguration`s. **This is the single most
important thing this design fixes** — and the anti-corruption boundary makes it structural rather
than a convention someone has to remember.

### What dies

- `CheckoutInfo` and `CheckoutStep` — UI form state masquerading as domain. The wizard step belongs
  in the Laminar view model; the server receives typed commands instead.
- `CheckoutInfo.loginPassword` — vestigial; nothing consumes it.
- `DiscountService.lookupPercent` — superseded by pricing's real `DiscountService`.
- `PaymentMethod.Card` — non-functional today (§15), so it is simply not representable (§5.3 of the
  architecture doc). The UI keeps a disabled "coming soon" affordance.

## 3. Domain model (`01-core/impl`)

### 3.1 `Order`

```scala
package mpbuilder.orderintake
package impl

final case class Order(id: Order.Id, data: Order.Data)

object Order:
  opaque type Id = UUID
  object Id:
    def random: UIO[Id] = Random.nextUUID
    def apply(raw: UUID): Id = raw
    extension (id: Id) def value: UUID = id

  /** Year-scoped, human-quotable: "MP-2026-000173". */
  final case class Number(year: Int, sequence: Int):
    override def toString = f"MP-$year-$sequence%06d"

  final case class Data(
      number:   Number,
      status:   Status,
      buyer:    Buyer,
      customer: Option[Customer.Ref],       // absent for guests
      lines:    NonEmptyList[Line],
      billing:  Address,
      shipping: Option[Address],            // absent ⇒ same as billing
      delivery: Delivery,
      payment:  Payment,
      discount: DiscountOutcome,
      totals:   Totals,
      speed:    ProductionSpeed,
      note:     Option[NoteText],
      placedAt: Timestamp,
      history:  NonEmptyList[Transition],
      version:  Version                     // optimistic lock
  )

  final case class Line(id: Line.Id, data: Line.Data)
  object Line:
    opaque type Id = UUID
    final case class Data(
        lineNo:      Int,
        spec:        ProductSpec,           // opaque catalog snapshot
        description: LocalizedString,       // rendered once, at placement
        quantity:    Quantity,
        artwork:     Option[Artwork.Ref],
        price:       PriceSnapshot,         // frozen
        catalogVersion:   Version.Catalog,
        pricelistVersion: Version.Pricelist
    )

  final case class Transition(from: Option[Status], to: Status, at: Timestamp, by: Actor)
```

`ManufacturingWorkflow.orderItemIndex` today is a **positional** reference into basket item
ordering — fragile. `Order.Line.Id` replaces it.

### 3.2 Supporting sums

Each of these replaces a set of correlated `Option`s or a stringly-typed field in today's model.

```scala
/** Replaces ContactInfo's three co-dependent Options (company / companyRegNo / vatId). */
enum Buyer:
  case Individual(name: PersonName, email: Email, phone: Phone)
  case Business  (name: PersonName, email: Email, phone: Phone, company: CompanyInfo)

/** Replaces DeliveryOption + a separate CourierService surcharge lookup. */
enum Delivery:
  case Pickup (location: ShopLocation.Id, name: LocalizedString)
  case Courier(service: CourierService.Id, name: LocalizedString,
               surcharge: Money, estimate: LocalizedString)

/** Card is deliberately absent until it works. */
enum Payment:
  case BankTransfer(symbol: VariableSymbol, qr: QrPayload, state: Payment.State)
  case OnAccount   (customer: Customer.Ref, due: Timestamp, state: Payment.State)
object Payment:
  enum State:
    case Pending
    case Settled(at: Timestamp, reference: Payment.Reference)

/** Preserves *why* no discount applied — today a bare Option throws that away. */
enum DiscountOutcome:
  case NotRequested
  case Applied (code: DiscountCode, kind: DiscountKind, amount: Money)
  case Refused (code: DiscountCode, reason: DiscountRefusal)

final case class Totals(net: Money, discount: Money, delivery: Money,
                        vat: Money, gross: Money, currency: Currency)
```

### 3.3 `Order.Status` — hierarchical

The point of the intermediate layer is that business rules dispatch on **groups**, so adding a leaf
does not break every match, and "can the customer still cancel?" is one case, not four.

```scala
sealed trait Status
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
    /** Nobody got anything; the leaf carries why. */
    sealed trait Unfulfilled extends Closed

  // ── Open ────────────────────────────────────────────────────────────────
  final case class AwaitingPayment(due: Timestamp)                        extends Open
  final case class Paid(at: Timestamp, reference: Payment.Reference)      extends Open

  // ── Active ──────────────────────────────────────────────────────────────
  final case class InProduction(since: Timestamp, production: Production.Ref) extends Active

  // ── Closed ──────────────────────────────────────────────────────────────
  final case class Completed(dispatchedAt: Timestamp,
                             tracking: Option[TrackingCode])              extends Closed.Fulfilled
  final case class Cancelled(at: Timestamp, by: Actor,
                             reason: CancellationReason)                  extends Closed.Unfulfilled
  final case class Rejected (at: Timestamp, by: Actor,
                             reason: RejectionReason)                     extends Closed.Unfulfilled
  final case class Refunded (at: Timestamp, amount: Money,
                             cancellation: Cancelled)                     extends Closed.Unfulfilled
```

Note that the leaves **carry their own evidence**. `Cancelled` cannot exist without a reason and an
actor; `Refunded` cannot exist without the cancellation that caused it. Today those live in a
parallel `List[OrderStatusChange]` with nullable `reason` and `by_user_id` columns, where nothing
stops a `Cancelled` row with a null reason.

```
                      ┌──────────────────► Closed.Unfulfilled ◄──────────┐
                      │                    (Cancelled/Rejected/Refunded) │
                      │                                                  │
  ─► Open.AwaitingPayment ──► Open.Paid ──► Active.InProduction ──► Closed.Fulfilled
                                                                    (Completed)
```

### 3.4 What the hierarchy buys

```scala
object OrderPolicy:

  def canCustomerCancel(o: Order): Boolean = o.data.status match
    case _: Status.Open   => true
    case _: Status.Active => false            // staff only, and it triggers a refund
    case _: Status.Closed => false

  def cancel(o: Order, by: Actor, reason: CancellationReason, now: Timestamp)
      : Validation[OrderError, Order] =
    o.data.status match
      case _: Status.Open                    => transitionTo(o, Status.Cancelled(now, by, reason))
      case _: Status.Active if by.isStaff    => transitionTo(o, Status.Cancelled(now, by, reason))
      case _: Status.Active                  => Validation.fail(OrderError.StaffOnly)
      case c: Status.Closed                  => Validation.fail(OrderError.AlreadyClosed(c))

  /** Analytics only ever cares about the group, never the leaf. */
  def countsTowardOnTimeDelivery(o: Order): Boolean =
    o.data.status.isInstanceOf[Status.Closed.Fulfilled]
```

Adding, say, `Open.AwaitingArtwork` later requires **no change** to any of the above. The same shape
should be applied to manufacturing's flat `WorkflowStatus`/`StepStatus` pair when that context is
extracted.

### 3.5 `Basket`

```scala
final case class Basket(id: Basket.Id, data: Basket.Data)

object Basket:
  opaque type Id = UUID

  /** Replaces a nullable customer_id AND a nullable session_token. */
  enum Owner:
    case Anonymous (session: Session.Token)
    case Registered(customer: Customer.Ref)

  final case class Data(
      owner:     Owner,
      items:     List[Item],
      currency:  Currency,
      createdAt: Timestamp,
      updatedAt: Timestamp,
      expiresAt: Timestamp,        // 30 days anonymous, 180 days registered
      version:   Version
  )

  final case class Item(id: Item.Id, data: Item.Data)
  object Item:
    opaque type Id = UUID
    final case class Data(
        spec:     ProductSpec,
        quantity: Quantity,
        speed:    ProductionSpeed,
        artwork:  Option[Artwork.Ref],
        quote:    Quote              // price + when + against which pricelist
    )
```

**Invariants:** at most 50 items; every item quantity within its category's bounds; all items share
the basket currency; an expired basket is read-only.

**Re-pricing policy.** Displayed prices come from the stored quote — matching today's UX, where the
basket total does not shift under the customer. But `PlaceOrder` **always** re-quotes server-side. If
a line's total moved beyond a configured tolerance, the command fails with
`PriceChanged(itemId, was, now)` and the client shows a confirm-and-retry. This is the only honest
way to run surge pricing, since pricing's context is queue-driven and genuinely time-varying.

**Anonymous → registered merge.** On login, `MergeBasket` appends the anonymous items to the
customer's basket, deduplicating on `ProductSpec` hash by summing quantities.

## 4. Public API (`01-core`, public package)

The public surface speaks **DTOs and error ADTs only** — never `Order`, never `Basket`.

```scala
package mpbuilder.orderintake

trait BasketService:
  def current(actor: Actor): IO[OrderError, BasketView]
  def addItem(actor: Actor, input: AddItem): IO[OrderError, BasketView]
  def updateQuantity(actor: Actor, item: BasketItemId, qty: Int): IO[OrderError, BasketView]
  def removeItem(actor: Actor, item: BasketItemId): IO[OrderError, BasketView]
  def clear(actor: Actor): IO[OrderError, BasketView]
  def requote(actor: Actor): IO[OrderError, BasketView]
  def merge(actor: Actor, from: SessionToken): IO[OrderError, BasketView]

trait CheckoutService:
  def options(actor: Actor): IO[OrderError, CheckoutOptions]     // delivery + payment, customer-aware
  def quote(actor: Actor, draft: CheckoutDraft): IO[OrderError, CheckoutQuote]
  def applyDiscount(actor: Actor, code: String): IO[OrderError, CheckoutQuote]
  def placeOrder(actor: Actor, input: PlaceOrder): IO[OrderError, OrderView]

trait OrderService:
  def find(actor: Actor, id: OrderId): IO[OrderError, OrderView]
  def listForCustomer(actor: Actor, page: Page): IO[OrderError, Paged[OrderSummaryView]]
  def search(actor: Actor, criteria: OrderSearch, page: Page): IO[OrderError, Paged[OrderSummaryView]]
  def cancel(actor: Actor, id: OrderId, reason: String): IO[OrderError, OrderView]
  def confirmPayment(actor: Actor, id: OrderId, ref: String): IO[OrderError, OrderView]
  def reject(actor: Actor, id: OrderId, reason: String): IO[OrderError, OrderView]
```

`Live` implementations (`impl/BasketServiceLive.scala`, …) are `private[orderintake]`. They translate
DTO → domain, call the pure policies, call the ports, persist, publish — and hold **no business rules**.

### The effect seam

- **Pure domain** (`impl`, no ZIO): aggregates plus `OrderPolicy`, `BasketPolicy`, `TotalsCalculator`,
  all returning `Validation[OrderError, A]`. Every business rule lives here; the tests are cheap.
- **`*Live` services** (`impl`, ZIO): orchestration only.

### Errors

```scala
enum OrderError extends DomainError:          // bilingual message(lang), as every existing ADT
  case BasketNotFound, BasketExpired, BasketEmpty, TooManyItems, MixedCurrency
  case ItemNotFound(item: BasketItemId)
  case InvalidQuantity(min: Int, max: Int)
  case SpecNoLongerValid(item: BasketItemId, reasons: NonEmptyList[String])
  case PriceChanged(item: BasketItemId, was: Money, now: Money)
  case DiscountRefused(code: String, reason: DiscountRefusal)
  case MinimumOrderNotMet(required: Money, actual: Money)
  case DeliveryUnavailable(reason: String)
  case PaymentNotAllowed(kind: String)
  case BuyerIncomplete(fields: NonEmptyList[String])
  case InvalidAddress(field: String), InvalidEmail(value: String), InvalidVatId(value: String)
  case OrderNotFound, StaffOnly, ConcurrentModification
  case AlreadyClosed(status: Order.Status.Closed)
  case IllegalTransition(from: Order.Status, to: String)
```

## 5. Ports (`01-core/impl`, all `private[orderintake]`)

### 5.1 Own infrastructure

```scala
private[orderintake] trait BasketRepository:
  def find(id: Basket.Id): IO[OrderError, Option[Basket]]
  def findByOwner(owner: Basket.Owner): IO[OrderError, Option[Basket]]
  def save(basket: Basket): IO[OrderError, Basket]        // optimistic lock on version
  def delete(id: Basket.Id): IO[OrderError, Unit]

private[orderintake] trait OrderRepository:
  def find(id: Order.Id): IO[OrderError, Option[Order]]
  def findByNumber(n: Order.Number): IO[OrderError, Option[Order]]
  def findForCustomer(c: Customer.Ref, page: Page): IO[OrderError, Paged[Order]]
  def search(criteria: OrderSearch, page: Page): IO[OrderError, Paged[Order]]
  def save(order: Order): IO[OrderError, Order]

private[orderintake] trait OrderNumbers:
  def next(year: Int): UIO[Order.Number]

private[orderintake] trait Events:                        // outbox write, same transaction
  def publish(events: NonEmptyList[OrderEvent]): IO[OrderError, Unit]

private[orderintake] trait Transactor:
  def transact[A](f: => IO[OrderError, A]): IO[OrderError, A]
```

### 5.2 Anti-corruption ports onto other contexts

Declared **in order-intake's vocabulary**. Nothing here mentions catalog, pricing, customers or
identity types — that is the whole point.

```scala
private[orderintake] trait ProductPort:
  /** Turn a client's configuration request into an opaque, versioned spec we can store. */
  def specFor(request: ProductRequest): IO[OrderError, ProductSpec]
  /** Is this spec still buildable against the current catalog? */
  def stillBuildable(spec: ProductSpec): IO[OrderError, Validation[String, Unit]]
  /** One-line human description, rendered once at placement. */
  def describe(spec: ProductSpec, lang: Language): UIO[LocalizedString]

private[orderintake] trait QuotePort:
  def quote(spec: ProductSpec, qty: Quantity, speed: ProductionSpeed,
            customer: Option[Customer.Ref], currency: Currency): IO[OrderError, Quote]
  def quoteAll(request: BasketQuoteRequest): IO[OrderError, BasketQuote]
  def resolveDiscount(code: DiscountCode, ctx: DiscountContext)
      : IO[OrderError, DiscountOutcome]
  /** Every speed with its surcharge, or the reason it is unavailable (spec §8.3). */
  def speedOffers(spec: ProductSpec, qty: Quantity): UIO[NonEmptyList[SpeedOffer]]

private[orderintake] trait BuyerPort:
  def lookup(customer: Customer.Ref): IO[OrderError, Option[BuyerProfile]]
  def lookupByEmail(email: Email): IO[OrderError, Option[BuyerProfile]]
  def registerFromCheckout(buyer: Buyer, billing: Address): IO[OrderError, Customer.Ref]

private[orderintake] trait ActorPort:
  def resolve(token: BearerToken): IO[OrderError, Actor]
  def issueGuestSession: UIO[Session.Token]
```

`ProductSpec` is opaque to order-intake — an `opaque type ProductSpec = Json` plus a
`Version.Catalog`. Order-intake stores it, hashes it for deduplication, and hands it back to the
adapter. It never interprets it.

## 6. Adapters (`02-infra`)

### 6.1 Postgres schema (`impl/postgres`, schema `order_intake`, Flyway-migrated)

```sql
baskets(id uuid pk, owner_kind text, session_token text, customer_id uuid,
        currency text, created_at, updated_at, expires_at, version bigint)
        -- unique partial index on customer_id where owner_kind = 'registered'
        -- CHECK: exactly one of session_token / customer_id is non-null

basket_items(id uuid pk, basket_id uuid fk cascade, position int,
        spec_json jsonb, spec_hash text, catalog_version text,
        quantity int, speed text, artwork_id uuid null,
        quoted_price_json jsonb, quoted_at, pricelist_version text)

orders(id uuid pk, order_number text unique, customer_id uuid null,
        status_group text, status_kind text, status_json jsonb,     -- see below
        buyer_json jsonb, billing_json jsonb, shipping_json jsonb null,
        delivery_json jsonb, payment_json jsonb, discount_json jsonb,
        totals_json jsonb, currency text, note text null, speed text,
        placed_at, updated_at, version bigint)
        -- indexes: (customer_id, placed_at desc), (status_group, placed_at desc)

order_lines(id uuid pk, order_id uuid fk, line_no int, spec_json jsonb,
        description_json jsonb, quantity int, artwork_id uuid null, price_json jsonb,
        catalog_version text, pricelist_version text)

order_transitions(id bigserial, order_id uuid fk, from_kind text null, to_kind text,
        at timestamptz, by_actor_json jsonb)

outbox(id bigserial pk, aggregate_id uuid, event_type text, payload jsonb,
        occurred_at, published_at timestamptz null)      -- partial index where published_at is null

order_number_seq_2026 ...   -- one sequence per year, created lazily
```

**Persisting the hierarchical status** — three columns rather than one:

- `status_kind` — the leaf (`'AwaitingPayment'`, `'Refunded'`, …), used to pick the decoder
- `status_json` — the leaf's payload (reason, actor, timestamps, references)
- `status_group` — denormalized `'open' | 'active' | 'closed_fulfilled' | 'closed_unfulfilled'`,
  written by the DAO from the domain value, and **the only status column any query filters on**

That keeps "all open orders" a single indexed predicate, while the leaf payload stays a typed sum in
the domain and never becomes a pile of nullable columns.

`jsonb` for `spec_json` and `price_json` is deliberate: they are deep, versioned, never queried
field-by-field, and must survive catalog schema evolution. Everything actually queried (status group,
customer, date, number, totals) is a real column.

### 6.2 tapir endpoints (`impl/http`)

```
POST   /api/v1/baskets                            → BasketView            (anonymous or authed)
GET    /api/v1/baskets/current                    → BasketView
POST   /api/v1/baskets/current/items              AddItem → BasketView
PATCH  /api/v1/baskets/current/items/{itemId}     UpdateQuantity → BasketView
DELETE /api/v1/baskets/current/items/{itemId}     → BasketView
DELETE /api/v1/baskets/current                    → BasketView
POST   /api/v1/baskets/current/requote            → BasketView
POST   /api/v1/baskets/current/merge              MergeRequest → BasketView

GET    /api/v1/checkout/options                   → CheckoutOptions
POST   /api/v1/checkout/quote                     CheckoutDraft → CheckoutQuote
POST   /api/v1/checkout/discount                  ApplyDiscount → CheckoutQuote
POST   /api/v1/orders                             PlaceOrder → OrderView              (201)

GET    /api/v1/orders                             → Paged[OrderSummaryView]  (own orders)
GET    /api/v1/orders/{id}                        → OrderView                (owner or staff)
POST   /api/v1/orders/{id}/cancel                 CancelRequest → OrderView

GET    /api/v1/staff/orders                       → Paged[OrderSummaryView]  (role: staff)
POST   /api/v1/staff/orders/{id}/confirm-payment  → OrderView
POST   /api/v1/staff/orders/{id}/reject           → OrderView
```

Endpoint *descriptions* are declared in `01-core`'s public package so the Scala.js client is
generated from the same source; server logic lives in `02-infra/impl/http`.

`OrderSearch` filters on the **status group**, not leaves — `?status=open` rather than
`?status=AwaitingPayment,Paid`, so a new leaf doesn't break saved staff filters.

Error responses carry the **full accumulated list**, localized via `Accept-Language`:

```json
{ "errors": [
  { "code": "PriceChanged", "message": "…", "field": "items[2]", "details": { "was": …, "now": … } }
] }
```

**Auth:** a tapir `securityInput` extracting `Authorization: Bearer` (or the anonymous
`X-Basket-Session` header), resolved through `ActorPort` into an `Actor` that every server logic receives.

### 6.3 Anti-corruption adapters (`impl/adapters`)

One class per port, each calling another context's **public service trait** and translating:

| Adapter | Implements | Calls |
|---|---|---|
| `CatalogProductAdapter` | `ProductPort` | `catalog.ConfigurationService` |
| `PricingQuoteAdapter` | `QuotePort` | `pricing.PricingService`, `pricing.DiscountService` |
| `CustomerBuyerAdapter` | `BuyerPort` | `customers.CustomerService` |
| `IdentityActorAdapter` | `ActorPort` | `identity.AuthService` |

These are the **only** files in the whole context that know the other contexts exist. Extracting
pricing to its own service later replaces exactly one of them with an sttp client against the same
port signature.

### 6.4 Outbox relay

A `ZStream` in `app` polling `outbox where published_at is null` and dispatching to in-process
subscribers. Swappable for Kafka or RabbitMQ later without touching any core.

## 7. Events

Published (JSON, via the transactional outbox):

| Event | Consumers |
|---|---|
| `OrderPlaced(orderId, number, lines, buyer, deadline, priority, speed, placedAt)` | **manufacturing** (generate workflows), notifications, analytics |
| `OrderPaid(orderId, method, paidAt, reference)` | manufacturing (payment gate on approval), notifications |
| `OrderCancelled(orderId, reason, by)` | manufacturing (cancel workflows), notifications |
| `OrderRejected(orderId, reason)` | notifications |
| `BasketAbandoned(basketId, customer?)` | notifications (later) |

Consumed from manufacturing (via `manufacturing/01-core`'s public event types, translated in
`order-intake/02-infra`): `OrderApproved` → `Active.InProduction`, `OrderDispatched` →
`Closed.Fulfilled`.

## 8. Implementation phases

Assumes Track A (commons, public service façades over `legacy-domain`, `app` skeleton, identity) from
[modular-architecture.md](modular-architecture.md) §7 is done.

**Phase 3 — basket, server-side.** `Basket` aggregate + policies (pure); `BasketRepository` +
Postgres adapter; `ProductPort`/`QuotePort` adapters; `/api/v1/baskets/*`; a `BasketBackend` field on
`BuilderEnvironment`, SPA switched to `HttpBasketBackend`.
*Verify:* add / update / remove / clear persists across a page reload and across devices;
`ui-calculator` still works with the network disabled.

**Phase 4 — checkout & quoting. ✅ backend done.** `CheckoutService` with `options` / `quote` /
`applyDiscount` / `speedOffers`; `GET /checkout/options`, `POST /checkout/quote`,
`POST /checkout/discount`, `GET /checkout/items/{itemId}/speeds`; plus
`PATCH /baskets/current/items/{itemId}/speed`, so a customer shown a speed offer can take it.

The buyer is resolved through `BuyerPort` → `CustomerBuyerAdapter` → `customers`; delivery options
moved server-side into `StaticDeliveryCatalog`; every payment rule lives in the pure
`CheckoutPolicy`, which lists **every** method with a reason when it is not on offer rather than
silently omitting it.

Two contract defects in pricing had to be fixed for any of it to work: `DiscountContext` now carries
the specs rather than category ids a caller cannot see (so category-restricted codes can be accepted
at all), and `DiscountOutcome.Applied` carries a `DiscountBenefit` rather than a bare amount (so a
free-delivery code is distinguishable from a code worth nothing).

*Verified:* over HTTP against the real engine — `CARDS10` accepted on business cards and refused on
flyers, `FREESHIP` waiving the courier surcharge while leaving the goods untouched, `OLDCODE` refused
as expired, `AGENCY15` refused for the wrong account type, a guest refused invoice-on-account with
the reason, and speed offers carrying per-line surcharges.
*Outstanding:* the SPA switch; `DiscountService.lookupPercent` goes with it.

**Phase 5 — the `Order` aggregate.** `Status` hierarchy + `OrderPolicy` transitions, `Order.Number`
generator, orders / lines / transitions / outbox tables, `PlaceOrder` with the re-quote guard,
`ConfirmPayment` / `cancel` / `reject`, order-read endpoints. `CheckoutView`'s "Place Order" becomes
real; `OrderHistoryView` and `CustomerPortalView` read live data.
*Verify:* placing an order writes `orders` + `order_lines` + an `OrderPlaced` outbox row with
`status_group='open'`; the portal shows it; a stale price is rejected with confirm-and-retry;
property test — every reachable `Status` is produced by exactly one legal transition path.

**Phase 6 — events → manufacturing.** Outbox relay; manufacturing subscribes to `OrderPlaced` and
generates workflows from its own `ProductionOrder`. **`ManufacturingOrder` stops embedding `Order`.**
*Verify:* a placed order appears in the approval queue with correct workflows; approving it moves the
order to `Active.InProduction` in the customer portal.

## 9. Frontend migration

`modules/ui-productbuilder/.../ProductBuilderViewModel.scala` (916 LOC) currently holds basket,
`checkoutInfo`, `checkoutBasket` and `loginState` in one global `Var[BuilderState]`. It keeps that
shape — but mutations become API calls. This is exactly what the existing **`BuilderEnvironment`
seam** is for (`modules/ui-productbuilder/.../BuilderEnvironment.scala`): add a
`basketBackend: BasketBackend` field.

- `FullAppEnvironment` (SPA) → `HttpBasketBackend`, using the tapir-generated client from
  `order-intake/01-core`'s JS target.
- `CalculatorEnvironment` (widget) → `LocalBasketBackend`, today's in-memory `BasketService` plus
  `EmailOrderModal`. **The widget keeps working with zero network.**

`CheckoutView.scala` (990 LOC) keeps its five-step wizard as pure client state; "✓ Place Order" calls
`POST /api/v1/orders` and renders accumulated field errors. `LoginWidget.scala` switches from mock
OTP to the real `/api/v1/auth/*`. `OrderHistoryView.scala` and `CustomerPortalView.scala` switch from
`sample` data to `GET /api/v1/orders` — becoming genuinely useful for the first time.

The UI renders status from the **group**, so a new leaf gets a sensible default badge rather than a
`MatchError`.

## 10. Design choices, justified

| Choice | Why |
|---|---|
| Basket in order-intake, not its own context | It is the pre-order state of the same commercial process; splitting it would make the handoff an anti-corruption boundary for no gain. |
| Basket server-side | Cross-device basket, authoritative re-pricing, abandoned-cart data. |
| Hierarchical `Status` with data-carrying leaves | Rules dispatch on groups so leaves can be added freely; evidence (reason, actor, refund amount) cannot go missing, because the leaf cannot be constructed without it. |
| `status_group` denormalized in Postgres | Keeps group queries a single indexed predicate without flattening the sum into nullable columns. |
| Order number `MP-2026-000173`, year-scoped sequence | Human-quotable on the phone, sortable, no cross-year collision, no distributed-ID machinery needed in a monolith. |
| Optimistic locking on both aggregates | Concurrent basket edits from two tabs are common; pessimistic locks in an HTTP request path are not worth it. |
| `PlaceOrder` submits the whole wizard at once | Lets `Validation` accumulate every error into one response — the codebase's established contract, which a step-by-step server machine would throw away. |
| `ProductSpec` opaque to order-intake | Makes the snapshot rule structural: order-intake *cannot* reach into a live configuration, because it cannot see the type. |
| `jsonb` snapshots rather than normalized tables | Specs are deep and versioned and never queried field-by-field; normalizing would couple the order schema to catalog schema evolution. |
| Discounts owned by pricing | Closes the dual-discount gap by construction: there is only one place a discount can be computed. |
| Manufacturing gets a snapshot via an event | Removes the hardest existing coupling and makes manufacturing extractable without change. |
