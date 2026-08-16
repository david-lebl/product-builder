# Order Intake — Bounded Context Design

> **Status:** proposed. The first feature module to be built for real against a database and an API.
> Assumes the module conventions and dependency rule from
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

| Concern | Owner | Order-intake sees it as |
|---|---|---|
| What can be built | catalog | `ConfigurationSnapshot` — an **immutable copy** captured at add-to-basket time |
| What it costs | pricing | `PriceQuote` returned by `PricingPort`; order-intake never computes a price itself |
| Who the customer is | customers | `CustomerId` + a `CustomerSummary` DTO |
| Who is signed in | identity | `Principal(userId, roles, customerId?)` from a verified JWT |
| How it gets made | manufacturing | nothing — order-intake only **publishes** `OrderPlaced` |
| Basket, checkout, order, order history | **order-intake** | owned outright |

### The snapshot rule

> An `Order` must never reference live catalog or pricing entities.

When an item enters the basket, order-intake stores a serialized `ConfigurationSnapshot` (with its
`catalogVersion`) and a serialized `PriceBreakdown` (with its `pricelistVersion`). A catalog edit six
months later must not retroactively change what a customer bought.

Today `Order` holds a live `Basket` holding live `ProductConfiguration`s. **This is the single most
important thing this design fixes.**

### What moves, what dies

**Moves into order-intake:** `Basket`, `BasketItem`, `BasketService`, `Order`, `DeliveryOption`,
`PaymentMethod`, `ShopLocation`, `CourierService`, `Address`, `ContactInfo`, `CustomerType`.
**New IDs:** `BasketId`, `OrderId` (existing), `OrderLineId`, `OrderNumber`.

**Dies:**
- `CheckoutInfo` and `CheckoutStep` — UI form state masquerading as domain. The wizard step belongs
  in the Laminar view model; the server receives typed commands instead.
- `CheckoutInfo.loginPassword` — vestigial; nothing consumes it.
- `DiscountService.lookupPercent` — superseded by the real `DiscountCodeService` behind `PricingPort`.

## 3. Aggregates

### 3.1 `Basket` (aggregate root)

```scala
final case class Basket(
    id:        BasketId,
    owner:     BasketOwner,          // Anonymous(sessionToken) | Registered(CustomerId)
    items:     List[BasketItem],
    currency:  Currency,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    expiresAt: Timestamp,            // 30 days anonymous, 180 days registered
    version:   Long                  // optimistic lock
)

final case class BasketItem(
    id:               BasketItemId,
    configuration:    ConfigurationSnapshot,   // opaque to order-intake, stored as jsonb
    quantity:         Quantity,
    speed:            ManufacturingSpeed,
    artworkId:        Option[ArtworkId],
    quotedPrice:      PriceBreakdown,          // snapshot — may go stale
    quotedAt:         Timestamp,
    pricelistVersion: String
)
```

**Invariants:** at most 50 items; every item quantity within its category's bounds; all items share
the basket currency; an expired basket is read-only.

**Re-pricing policy.** Displayed prices come from the stored quote — matching today's UX, where the
basket total does not shift under the customer. But `PlaceOrder` **always** re-quotes server-side. If
a line's total moved by more than a configured tolerance, the command fails with
`PriceChanged(itemId, was, now)` and the client shows a confirm-and-retry. This is the only honest
way to run surge pricing, since `PricingContext` is queue-driven and genuinely time-varying.

**Anonymous → registered merge.** On login, `MergeBasket(anonymousToken, customerId)` appends the
anonymous items to the customer's basket, deduplicating on configuration hash by summing quantities.

### 3.2 `Order` (aggregate root)

```scala
final case class Order(
    id:       OrderId,
    number:   OrderNumber,               // "MP-2026-000173" — year-scoped Postgres sequence
    status:   OrderStatus,
    customer: OrderCustomer,             // customerId? + snapshotted ContactInfo + CustomerType
    lines:    NonEmptyList[OrderLine],
    billing:  Address,
    shipping: Option[Address],
    delivery: DeliveryChoice,            // option + snapshotted name and surcharge
    payment:  PaymentIntent,             // method + status + reference/VS + QR payload
    discount: Option[AppliedDiscount],   // code + type + amount, all snapshotted
    totals:   OrderTotals,               // net, discount, delivery, vat, grand; + currency
    note:     Option[String],
    speed:    ManufacturingSpeed,
    placedAt: Timestamp,
    history:  List[OrderStatusChange],   // status, at, byPrincipal, reason
    version:  Long
)

final case class OrderLine(
    id:               OrderLineId,
    lineNo:           Int,
    configuration:    ConfigurationSnapshot,
    description:      LocalizedString,   // human-readable, rendered once at placement
    quantity:         Quantity,
    artworkId:        Option[ArtworkId],
    price:            PriceBreakdown,    // frozen
    catalogVersion:   String,
    pricelistVersion: String
)
```

`ManufacturingWorkflow.orderItemIndex` today is a **positional** reference into basket item
ordering — fragile. `OrderLineId` replaces it.

### 3.3 Status lifecycle

```
                    ┌──────────────► Cancelled ◄──────┐
                    │                                  │
  Draft ─► AwaitingPayment ─► Paid ─► InProduction ─► Completed
    │                                                  │
    └──────────────► Rejected                       Refunded
```

| Transition | Trigger | Guard |
|---|---|---|
| `Draft → AwaitingPayment` | `PlaceOrder` (customer) | re-quote matches; configurations still valid; ≥ 1 line |
| `AwaitingPayment → Paid` | `ConfirmPayment` (staff or payment webhook) | — |
| `AwaitingPayment → Paid` | automatic | `PaymentMethod.InvoiceOnAccount` **and** customer is `RegisteredCorporate` + `Active` |
| `Paid → InProduction` | `OrderApproved` event from manufacturing | — |
| `InProduction → Completed` | `OrderDispatched` event from manufacturing | — |
| `Draft`/`AwaitingPayment → Cancelled` | `CancelOrder` (customer or staff) | not yet in production |
| `Paid`/`InProduction → Cancelled` | `CancelOrder` (**staff only**) | requires reason; triggers `Refunded` |
| `AwaitingPayment → Rejected` | `RejectOrder` (staff) | requires reason |

`Draft` exists only inside the `PlaceOrder` transaction — a submitted order is never left in `Draft`.

> This **commercial** lifecycle is distinct from manufacturing's `ApprovalStatus` /
> `WorkflowStatus` / `FulfilmentChecklist`, which stay in the manufacturing module and drive this one
> via events.

## 4. Commands, events, errors

### Commands (`00-contract`, each carrying a `Principal`)

```
Basket:    CreateBasket · AddItem · UpdateItemQuantity · RemoveItem · ClearBasket
           · MergeBasket · RequoteBasket
Checkout:  StartCheckout · SetContactDetails · SetAddresses · SetDelivery
           · ApplyDiscountCode · RemoveDiscountCode · SetPaymentMethod · PlaceOrder
Order:     ConfirmPayment · CancelOrder · RejectOrder · AddOrderNote
```

The five-step wizard is **not** a server state machine. The client collects data and `PlaceOrder`
submits it all at once, validated as a whole with `Validation` accumulation — so the server returns
every problem in one response instead of one step at a time. `ApplyDiscountCode` and `RequoteBasket`
are the only mid-wizard round-trips, because both need server authority.

### Events published (`00-contract`, JSON, via transactional outbox)

| Event | Consumers |
|---|---|
| `OrderPlaced(orderId, number, lines, customer, deadline, priority, speed, placedAt)` | **manufacturing** (generate workflows), notifications (confirmation e-mail), analytics |
| `OrderPaid(orderId, method, paidAt, reference)` | manufacturing (payment gate on approval), notifications |
| `OrderCancelled(orderId, reason, byPrincipal)` | manufacturing (cancel workflows), notifications |
| `OrderRejected(orderId, reason)` | notifications |
| `BasketAbandoned(basketId, customerId?)` | notifications (later) |

**Consumed** from manufacturing: `OrderApproved`, `OrderDispatched`.

### Errors

`sealed trait OrderIntakeError extends DomainError`, with bilingual EN/CS `message(lang)` matching
every existing error ADT:

```
BasketNotFound · BasketExpired · BasketEmpty · ItemNotFound · TooManyItems
InvalidQuantity(min, max) · MixedCurrency
ConfigurationNoLongerValid(itemId, reasons: List[ConfigurationError])
PriceChanged(itemId, was: Money, now: Money)
DiscountCodeInvalid(code, reason) · MinimumOrderNotMet(required, actual)
DeliveryOptionUnavailable · PaymentMethodNotAllowed(method, customerType)
ContactDetailsIncomplete(fields) · InvalidAddress(field) · InvalidEmail · InvalidVatId
OrderNotFound · IllegalStatusTransition(from, to) · NotAuthorised · ConcurrentModification
```

## 5. Ports (`01-core`)

### The effect seam

`01-core` has two layers, and the split matters:

- **Domain layer** — pure, `Validation[OrderIntakeError, A]`, **no ZIO**. Aggregate methods and pure
  services (`OrderPolicy.canTransition`, `BasketPolicy.validateItem`, `TotalsCalculator`). Every
  business rule lives here, and the tests are cheap.
- **Application layer** — `ZIO[Any, OrderIntakeError, A]` use-case classes that orchestrate: load via
  a repository port, call the pure domain, call outbound ports, persist, publish.
  **No business rules here.**

### Driven ports (traits in `01-core`, implemented in `02-infra`)

```scala
trait BasketRepository:
  def find(id: BasketId): IO[OrderIntakeError, Option[Basket]]
  def findByOwner(owner: BasketOwner): IO[OrderIntakeError, Option[Basket]]
  def save(basket: Basket): IO[OrderIntakeError, Basket]      // optimistic lock on version
  def delete(id: BasketId): IO[OrderIntakeError, Unit]

trait OrderRepository:
  def find(id: OrderId): IO[OrderIntakeError, Option[Order]]
  def findByNumber(n: OrderNumber): IO[OrderIntakeError, Option[Order]]
  def findForCustomer(id: CustomerId, page: Page): IO[OrderIntakeError, PagedResult[OrderSummary]]
  def search(criteria: OrderSearch, page: Page): IO[OrderIntakeError, PagedResult[OrderSummary]]
  def save(order: Order): IO[OrderIntakeError, Order]

trait OrderNumberGenerator:
  def next(year: Int): UIO[OrderNumber]

trait EventPublisher:                                          // outbox write, same transaction
  def publish(events: List[OrderIntakeEvent]): IO[OrderIntakeError, Unit]

trait Transactor:
  def transact[A](f: => IO[OrderIntakeError, A]): IO[OrderIntakeError, A]
```

### Ports into other modules

Declared in the **other** module's `00-contract`:

```scala
// catalog/00-contract
trait CatalogPort:
  def snapshot(request: ConfigurationRequest): IO[CatalogError, ConfigurationSnapshot]
  def revalidate(snapshot: ConfigurationSnapshot): IO[CatalogError, Validation[ConfigurationError, Unit]]
  def describe(snapshot: ConfigurationSnapshot, lang: Language): UIO[LocalizedString]
  def currentVersion: UIO[CatalogVersion]

// pricing/00-contract
trait PricingPort:
  def quoteLine(snapshot: ConfigurationSnapshot, qty: Quantity, speed: ManufacturingSpeed,
                customer: Option[CustomerId], currency: Currency): IO[PricingError, PriceBreakdown]
  def quoteBasket(request: BasketQuoteRequest): IO[PricingError, BasketQuote]
  def applyDiscount(code: String, ctx: DiscountContext)
      : IO[PricingError, Validation[DiscountError, AppliedDiscount]]
  def availableSpeeds(snapshot: ConfigurationSnapshot, qty: Quantity): UIO[Set[ManufacturingSpeed]]

// customers/00-contract
trait CustomerPort:
  def find(id: CustomerId): IO[CustomerError, Option[CustomerSummary]]
  def findByEmail(email: String): IO[CustomerError, Option[CustomerSummary]]
  def registerFromCheckout(details: ContactInfo, company: Option[CompanyInfo])
      : IO[CustomerError, CustomerId]

// identity/00-contract
trait IdentityPort:
  def verify(token: JwtToken): IO[AuthError, Principal]
  def issueGuestSession(): UIO[SessionToken]
```

`PricingPort.availableSpeeds` folds in today's `TierRestrictionValidator` plus the express-cutoff
logic (business spec §8.3), so order-intake never has to know about station saturation.

## 6. Adapters (`02-infra`)

### 6.1 Postgres schema (schema `order_intake`, Flyway-migrated)

```sql
baskets(id uuid pk, owner_kind text, session_token text, customer_id uuid,
        currency text, created_at, updated_at, expires_at, version bigint)
        -- unique partial index on customer_id where owner_kind = 'registered'

basket_items(id uuid pk, basket_id uuid fk cascade, position int,
        configuration_json jsonb, catalog_version text, quantity int, speed text,
        artwork_id uuid null, quoted_price_json jsonb, quoted_at, pricelist_version text)

orders(id uuid pk, order_number text unique, status text, customer_id uuid null,
        customer_type text, contact_json jsonb, billing_json jsonb, shipping_json jsonb null,
        delivery_json jsonb, payment_json jsonb, discount_json jsonb null,
        totals_json jsonb, currency text, note text null, speed text,
        placed_at, updated_at, version bigint)
        -- indexes: (customer_id, placed_at desc), (status, placed_at desc)

order_lines(id uuid pk, order_id uuid fk, line_no int, configuration_json jsonb,
        description_json jsonb, quantity int, artwork_id uuid null, price_json jsonb,
        catalog_version text, pricelist_version text)

order_status_history(id bigserial, order_id uuid fk, from_status text, to_status text,
        at timestamptz, by_user_id uuid null, reason text null)

outbox(id bigserial pk, aggregate_id uuid, event_type text, payload jsonb,
        occurred_at, published_at timestamptz null)      -- partial index where published_at is null

order_number_seq_2026 ...   -- one sequence per year, created lazily
```

`jsonb` for snapshots is deliberate: `ConfigurationSnapshot` and `PriceBreakdown` are deep, versioned,
never queried field-by-field, and must survive catalog schema evolution. Everything actually queried
(status, customer, date, number, totals) is a real column.

### 6.2 tapir endpoints

Endpoint *descriptions* live in `00-contract` so the Scala.js client is generated from the same
source; server logic lives in `02-infra`.

```
POST   /api/v1/baskets                            → BasketView            (anonymous or authed)
GET    /api/v1/baskets/current                    → BasketView
POST   /api/v1/baskets/current/items              AddItemRequest → BasketView
PATCH  /api/v1/baskets/current/items/{itemId}     UpdateQuantityRequest → BasketView
DELETE /api/v1/baskets/current/items/{itemId}     → BasketView
DELETE /api/v1/baskets/current                    → BasketView
POST   /api/v1/baskets/current/requote            → BasketView
POST   /api/v1/baskets/current/merge              MergeRequest → BasketView

POST   /api/v1/checkout/discount                  ApplyDiscountRequest → CheckoutQuoteView
GET    /api/v1/checkout/options                   → DeliveryOptions + PaymentMethods (customer-aware)
POST   /api/v1/checkout/quote                     CheckoutDraft → CheckoutQuoteView
POST   /api/v1/orders                             PlaceOrderRequest → OrderView          (201)

GET    /api/v1/orders/{id}                        → OrderView             (owner or staff)
GET    /api/v1/orders                             → PagedResult[OrderSummaryView]  (own orders)
POST   /api/v1/orders/{id}/cancel                 CancelRequest → OrderView

GET    /api/v1/staff/orders                       → PagedResult[OrderSummaryView]  (role: staff)
POST   /api/v1/staff/orders/{id}/confirm-payment  → OrderView
POST   /api/v1/staff/orders/{id}/reject           → OrderView
```

Error responses carry the **full accumulated list**, localized via the `Accept-Language` header
against the existing `message(lang)` machinery:

```json
{ "errors": [
  { "code": "PriceChanged", "message": "…", "field": "items[2]", "details": { "was": …, "now": … } }
] }
```

**Auth:** a tapir `securityInput` extracting `Authorization: Bearer` (or the anonymous
`X-Basket-Session` header), resolved through `IdentityPort.verify` into a `Principal` that every
server logic receives.

### 6.3 In-process port implementations

`order-intake/02-infra/adapters/`: `LocalCatalogPort`, `LocalPricingPort`, `LocalCustomerPort` — each
a thin `ZIO.succeed` / `ZIO.fromEither` wrapper over a direct call into the corresponding module.
When a module is later extracted to a service, **only these classes** are replaced by sttp/tapir
clients.

### 6.4 Outbox relay

A `ZStream` in `app` polling `outbox where published_at is null` and dispatching to in-process
subscribers (manufacturing, notifications). Swappable for Kafka or RabbitMQ later without touching
any core.

## 7. Identity, minimally, for order intake

`identity/01-core`:

- `User(id, email, passwordHash, status, roles, customerId?, createdAt, lastLoginAt?)`
- `PasswordPolicy` (pure); `PasswordHasher` port → Argon2 (BouncyCastle) impl in `02-infra`
- `TokenService` port: `issue(principal)` → `AccessToken` (15 min) + `RefreshToken` (30 d); verify; rotate
- `OtpAuthenticator` — today's `LoginService` (IČO/DIČ/e-mail → 5-minute OTP → 24-hour session),
  preserved as a second `AuthenticationMethod` alongside `PasswordAuthenticator`; both terminate in a JWT
- `Role { Customer, StaffOperator, StaffManager, Admin }`

Endpoints: `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout`,
`/password-reset/request`, `/password-reset/confirm`, `/otp/request`, `/otp/verify`,
`GET /api/v1/auth/me`.

**Guest → registered upgrade.** `PlaceOrder` with `createAccount: true` runs
`CustomerPort.registerFromCheckout` and `identity.register` in the same transaction, and merges the
anonymous basket. Guests who never register get an order with `customerId = None` and a snapshotted
`ContactInfo`; order lookup is by `number + email`.

## 8. Implementation phases

Assumes Track A (kernel, port façades, `app` skeleton, identity) from
[modular-architecture.md](modular-architecture.md) §6 is done.

### Phase 3 — basket, server-side

`order-intake/01-core` basket aggregate + policies (pure, `Validation`); `BasketRepository` +
Postgres adapter; the `/api/v1/baskets/*` endpoints; a `BasketBackend` field on `BuilderEnvironment`,
with the SPA switched to `HttpBasketBackend`.

*Verify:* add / update / remove / clear from the real UI persists across a page reload and across
devices; `ui-calculator` still works with the network disabled.

### Phase 4 — checkout & quoting

`PricingPort.quoteBasket` and `applyDiscount` wired through the façade; `/checkout/quote`,
`/checkout/discount`, `/checkout/options`. **Delete `DiscountService.lookupPercent`** — checkout now
honours the real `DiscountCodeService` rules, closing the §15 dual-discount gap.

*Verify:* an expired / exhausted / minimum-order code from `SampleDiscountCodes` is correctly refused
at checkout; delivery and payment options vary correctly by customer type.

### Phase 5 — the `Order` aggregate

Status state machine, `OrderNumber` generator, order / line / history / outbox tables, `PlaceOrder`
with the re-quote guard, `ConfirmPayment` / `Cancel` / `Reject`, order-read endpoints.
`CheckoutView`'s "Place Order" becomes real; `OrderHistoryView` and `CustomerPortalView` read live data.

*Verify:* placing an order in the browser writes rows to `orders` and `order_lines` and an
`OrderPlaced` row to `outbox`; the customer portal shows it; a stale price is rejected with a
confirm-and-retry.

### Phase 6 — events → manufacturing

Outbox relay; manufacturing subscribes to `OrderPlaced` and generates workflows from a
`ProductionOrderSnapshot`. **`ManufacturingOrder` stops embedding `Order`.**
`OrderApproved` / `OrderDispatched` flow back and advance order status.

*Verify:* a placed order appears in the manufacturing Order Approval queue with correct workflows;
approving it moves the order to `InProduction` in the customer portal.

## 9. Frontend migration

`modules/ui-productbuilder/.../ProductBuilderViewModel.scala` (916 LOC) currently holds basket,
`checkoutInfo`, `checkoutBasket` and `loginState` in one global `Var[BuilderState]`. It keeps that
shape — but mutations become API calls. This is exactly what the existing **`BuilderEnvironment`
seam** is for (`modules/ui-productbuilder/.../BuilderEnvironment.scala`): add a
`basketBackend: BasketBackend` field.

- `FullAppEnvironment` (SPA) → `HttpBasketBackend`, using the tapir-generated client from
  `order-intake/00-contract/js`.
- `CalculatorEnvironment` (widget) → `LocalBasketBackend`, today's in-memory `BasketService` plus
  `EmailOrderModal`. **The widget keeps working with zero network.**

`CheckoutView.scala` (990 LOC) keeps its five-step wizard as pure client state; its "✓ Place Order"
button stops faking and calls `POST /api/v1/orders`, rendering accumulated field errors from the
response. `LoginWidget.scala` switches from mock OTP to the real `/api/v1/auth/*`.
`OrderHistoryView.scala` and `CustomerPortalView.scala` switch from `sample` data to
`GET /api/v1/orders` — becoming genuinely useful for the first time.

## 10. Design choices, justified

| Choice | Why |
|---|---|
| Basket server-side, in order-intake (not its own module) | Basket is the pre-order state of the same commercial process; splitting it would make the handoff a cross-module contract for no gain. Server-side buys cross-device baskets, authoritative re-pricing, and abandoned-cart data. |
| Order number `MP-2026-000173`, year-scoped Postgres sequence | Human-quotable on the phone, sortable, no cross-year collision, no distributed-ID machinery needed in a monolith. |
| Optimistic locking (`version` column) on both aggregates | Concurrent basket edits from two tabs are common; pessimistic locks in an HTTP request path are not worth it. |
| `PlaceOrder` submits the whole wizard at once | Lets `Validation` accumulate every error into one response, which is the codebase's established contract — a step-by-step server machine would throw that away. |
| `jsonb` snapshots rather than normalized configuration tables | Configurations are deep and versioned, and are never queried field-by-field. Normalizing them would couple the order schema to catalog schema evolution. |
| Discounts owned by pricing, not order-intake | Closes the dual-discount gap by construction: there is only one place a discount can be computed. |
| Manufacturing gets a snapshot via an event, not the `Order` | Removes the hardest existing coupling and makes manufacturing extractable as a service without change. |