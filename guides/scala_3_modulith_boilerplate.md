# Scala 3 + ZIO Modular Monolith (Hexagonal)

## 1. Directory Layout (Two Views: Logical Packages & Build Modules)

`shared/` sits at the **top** (pure primitives usable by `core` and adapters), while `platform/` sits at the **bottom** (common infrastructure usable **only** by outbound adapters and wired in `Main`). Because it is hard to visualize both **feature slices** and **layer boundaries** in a single tree, both views are shown below.

### 1.1 Logical Package View (`src/main/scala/com/example/`)

```text
src/main/scala/com/example/
├── Main.scala                         # Application composition root (wires ZLayers)
├── shared/                            # Top kernel: pure primitives (IDs, common errors, EventBus)
│   └── EventBus.scala                 #   Usable by core & adapters
├── orders/                            # Bounded Context 1: Orders
│   ├── OrdersModule.scala             # Public ZLayer assembly (03-wiring: core + inbound + outbound)
│   ├── OrderService.scala             # Public inbound port / API
│   ├── OrderDto.scala                 # Public models, domain errors (OrderError) & events (OrderPlaced)
│   ├── core/                          # private[orders] — Domain & Business Rules (NO platform imports)
│   │   ├── Order.scala                # Internal aggregate / entities
│   │   ├── OrderRepository.scala      # Outbound port (trait)
│   │   └── OrderServiceLive.scala     # Inbound port implementation
│   ├── http/                          # Inbound HTTP adapter (02-i-http) — NO platform or db imports
│   │   ├── api/                       # Pure Tapir endpoints & wire DTOs (01-i-tapir-api — zero core deps)
│   │   │   └── OrderEndpoints.scala
│   │   └── OrderHttpRoutes.scala      # Public type, private[orders] constructor — Tapir server routes
│   └── db/                            # Outbound DB adapter (02-o-infra / 02-o-db) — uses platform.db
│       └── PostgresOrderRepo.scala    # private[orders] — Persistence adapter
├── billing/                           # Bounded Context 2: Billing
│   ├── BillingModule.scala            # Public ZLayer assembly
│   ├── BillingService.scala           # Public inbound port / API
│   ├── core/                          # private[billing] — Domain & Business Rules (NO platform imports)
│   │   └── ...
│   ├── events/                        # Inbound event adapter (02-i-events)
│   │   └── OrderEventConsumer.scala   # Reacts to OrderPlaced events
│   └── acl/                           # Outbound ACL adapters (02-o-infra / 02-o-acl-*)
│       └── ...
└── platform/                          # Bottom infra: DB, external HTTP clients, AWS ZIO wrappers
    ├── PlatformModule.scala           # Composes shared infra ZLayers (ZLayer.makeSome)
    ├── db/
    │   ├── Database.scala             # Connection pool / query execution wrapper
    │   └── Migrations.scala           # Flyway / Liquibase startup runner (+ resources/db/migration/)
    ├── http/
    │   └── PaymentHttpClient.scala    # External service HTTP DTOs & client API
    └── aws/
        └── S3Client.scala             # Thin ZIO wrapper over AWS SDK client
```

### 1.2 Multi-Module Build View (Subprojects per Feature × Layer)

Each bounded context is a top-level folder containing numbered layer subprojects (`NN-<layer>-<name>`). **Dependency invariant:** a subproject at tier `NN` may only depend on lower-numbered tiers (`< NN`) — never on siblings at the same `NN` (neither within the same feature nor across features). All subprojects inside `orders/` still share the `com.example.orders` package hierarchy from **1.1**, so `private[orders]` visibility works seamlessly across them.

```text
.
├── build.sbt
├── shared/                            # Top kernel (EventBus, common IDs)
├── platform/                          # Bottom infra (db, migrations, http clients, aws wrappers)
├── orders/                            # Bounded Context 1: Orders
│   ├── 01-c-core/                     # Core + public Scala API (OrderDto, OrderError, OrderService, core/*) -> shared
│   ├── 01-i-tapir-api/                # Pure Tapir endpoint & HTTP DTO definitions only (zero core deps; same for 01-i-grpc-api/)
│   ├── 02-i-http/                     # Inbound HTTP router (OrderHttpRoutes) -> 01-i-tapir-api, 01-c-core
│   ├── 02-o-infra/                    # Outbound adapters (PostgresOrderRepo) -> 01-c-core, platform
│   │   # As outbound integrations grow, split 02-o-infra into focused adapters:
│   │   #   02-o-db/                   #   Persistence adapter (uses platform.db)
│   │   #   02-o-http/                 #   External HTTP adapter (uses platform.http)
│   │   #   02-o-acl-<other>/          #   ACL adapter calling another bounded context or external system
│   └── 03-wiring/                     # OrdersModule: assembles core + all 02-* adapters -> 02-i-http, 02-o-infra
├── billing/                           # Bounded Context 2: Billing
│   ├── 01-c-core/                     # Core + public Scala API -> shared ONLY (never depends on orders/01-c-core!)
│   ├── 02-i-events/                   # Inbound event consumer (OrderEventConsumer) -> billing/01-c-core, orders/01-c-core
│   ├── 02-o-acl-orders/               # Outbound ACL for sync calls to OrderService -> billing/01-c-core, orders/01-c-core
│   ├── 02-o-infra/                    # Outbound adapters (payment/S3) -> billing/01-c-core, platform
│   └── 03-wiring/                     # BillingModule -> 02-i-events, 02-o-acl-orders, 02-o-infra
└── app/                               # Composition root (Main.scala) — depends only on 03-wiring modules + platform
```

**Prefix convention (`NN-<layer>-<name>`):**
- `01-*` — Self-contained leaves (no dependencies between any `01-*` modules):
  - `01-c-core`: domain logic + merged public Scala API (depends **only** on `shared`).
  - `01-i-tapir-api` (or `01-i-grpc-api`): pure transport contract definitions & wire DTOs — **no routes/handlers**. Keeping `01-c-core` and `01-i-tapir-api` as siblings on `01` makes it impossible for core to accidentally import Tapir/gRPC DTOs, and keeps the API module lightweight for OpenAPI generation or client stubs.
- `02-*` — Adapters (`i` = inbound, `o` = outbound) bridging `01-*` contracts/core to transport, infrastructure, or other bounded contexts:
  - `02-i-http` / `02-i-events`: inbound server routes and cross-context event consumers (cannot see `02-o-infra` or `platform`).
  - `02-o-acl-<other>`: anti-corruption layer translating another bounded context's public API into this context's own outbound port.
  - `02-o-infra` (or split into `02-o-db`, `02-o-http`): infrastructure adapters (cannot see `01-i-tapir-api` or `02-i-http`).
- `03-wiring` — The context's own composition root (`OrdersModule`). It is the only subproject that sees both inbound and outbound adapters, so it is the only place that can assemble the full context. Because it lives in `com.example.orders`, it can construct `private[orders]` internals (`PostgresOrderRepo`, `OrderServiceLive`, `OrderHttpRoutes`) and hand `app/` only public types.
  - **Simpler alternative — a single `02-adapters` (or `02-infra`) module:** merge all inbound and outbound adapters of a context into one subproject (`-> 01-c-core, 01-i-tapir-api, platform`) and put `OrdersModule` there, so no `03-wiring` tier is needed. Trade-off: the build no longer stops inbound adapters from importing `platform`/`db` code (or outbound adapters from importing HTTP contracts); that boundary is back to package convention and code review. A good default for small contexts. Split into `02-i-*` / `02-o-*` + `03-wiring` once the adapters grow or the boundary starts getting violated.

---

## 2. Feature Root (`com.example.orders`) — Public Boundary

Only files directly under `com.example.orders` are visible to other bounded contexts (such as `billing`). Keeping the company namespace and module root together (`package com.example.orders`) ensures cross-module imports stay explicit (`import com.example.shared.*`, `import com.example.platform.*`), while sub-packages inside the module chain beneath it to auto-import the module's public boundary.

Use typed domain errors (`IO[OrderError, A]`) instead of `Task[A]` so callers handle business failures explicitly via Scala 3's exhaustive pattern matching, with human-readable message text living directly on each error case. Use `opaque type` for IDs instead of `extends AnyVal` so IDs stay zero-allocation even inside generic `ZIO`/`IO` types.

```scala
package com.example.orders

import zio.*
import java.util.UUID

// Public Models, Events & Domain Errors
opaque type OrderId = UUID
object OrderId:
  def apply(value: UUID): OrderId = value
  extension (id: OrderId) def value: UUID = id

final case class CreateOrderCmd(customerId: UUID, amount: BigDecimal)
final case class OrderPlaced(orderId: OrderId, amount: BigDecimal)

enum OrderError(val message: String):
  case InvalidAmount(amount: BigDecimal) extends OrderError(s"Invalid amount: $amount")
  case AlreadyExists(orderId: OrderId)   extends OrderError(s"Order already exists: ${orderId.value}")
  case NotFound(orderId: OrderId)        extends OrderError(s"Order not found: ${orderId.value}")

// Public Inbound Port (Module API)
trait OrderService:
  def placeOrder(cmd: CreateOrderCmd): IO[OrderError, OrderId]

object OrderService:
  def placeOrder(cmd: CreateOrderCmd): ZIO[OrderService, OrderError, OrderId] =
    ZIO.serviceWithZIO[OrderService](_.placeOrder(cmd))
```

---

## 3. Core Sub-Package (`com.example.orders` → `core`) — Compile-Time Isolated

Everything inside `core` is marked `private[orders]`. If `com.example.billing` attempts to import `Order` or `OrderRepository`, the Scala compiler immediately rejects it.

Chaining `package com.example.orders` followed by `package core` auto-imports the feature root (`OrderId`, `CreateOrderCmd`, `OrderPlaced`, `OrderError`, `OrderService`), while cross-module dependencies (`com.example.shared.EventBus`) remain explicit. `core` **never** imports `com.example.platform`. Outbound ports like `OrderRepository` declare only the narrow domain errors they can meaningfully produce (`OrderError.AlreadyExists`), which widen automatically to `OrderError` inside `OrderServiceLive`.

```scala
package com.example.orders
package core

import com.example.shared.EventBus
import zio.*
import java.util.UUID

// Internal Domain Model (Hidden from other modules)
private[orders] final case class Order(
  id: OrderId,
  customerId: UUID,
  amount: BigDecimal,
  status: OrderStatus
)

private[orders] enum OrderStatus:
  case Pending, Confirmed

// Outbound Port (Speaks only domain types & domain errors)
private[orders] trait OrderRepository:
  def save(order: Order): IO[OrderError.AlreadyExists, Unit]

// Business Logic Implementation
private[orders] final class OrderServiceLive(
  repo: OrderRepository,
  eventBus: EventBus
) extends OrderService:

  override def placeOrder(cmd: CreateOrderCmd): IO[OrderError, OrderId] =
    for
      _     <- ZIO.fail(OrderError.InvalidAmount(cmd.amount)).when(cmd.amount <= 0)
      id    <- ZIO.succeed(OrderId(UUID.randomUUID()))
      order  = Order(id, cmd.customerId, cmd.amount, OrderStatus.Pending)
      _     <- repo.save(order)
      _     <- eventBus.publish(OrderPlaced(id, cmd.amount))
    yield id

private[orders] object OrderServiceLive:
  val layer: ZLayer[OrderRepository & EventBus, Nothing, OrderService] =
    ZLayer.fromFunction(OrderServiceLive.apply)
```

---

## 4. Platform Module (`com.example.platform`) — Common Infrastructure (Adapter-Only)

`platform` is the bottom counterpart to `shared`: it owns technical infrastructure (database pool & migrations, external HTTP client DTOs/APIs, AWS SDK ZIO wrappers) and typed infrastructure errors (`DbError`, `HttpClientError`, `AwsError`) so adapters can translate expected failures cleanly without matching on raw `Throwable`s.

### Database & Migrations (`platform.db`)

```scala
package com.example.platform
package db

import zio.*

final case class DbConfig(jdbcUrl: String, user: String, password: String)

// Infrastructure-level errors (confined to platform & adapters)
enum DbError(val message: String):
  case UniqueViolation(detail: String) extends DbError(s"Unique constraint violation: $detail")
  case Unexpected(cause: Throwable)    extends DbError(s"Unexpected database error: ${cause.getMessage}")

trait Database:
  def execute(sql: String): IO[DbError, Unit]

object Database:
  val live: ZLayer[DbConfig, DbError, Database] =
    ZLayer.scoped:
      for
        cfg <- ZIO.service[DbConfig]
        _   <- Migrations.run(cfg)
        _   <- ZIO.logInfo(s"Opening connection pool: ${cfg.jdbcUrl}")
        _   <- ZIO.addFinalizer(ZIO.logInfo("Closing connection pool"))
      yield new Database:
        override def execute(sql: String): IO[DbError, Unit] =
          ZIO.logInfo(s"[DB] $sql")

object Migrations:
  def run(cfg: DbConfig): IO[DbError, Unit] =
    ZIO.logInfo(s"Running Flyway/Liquibase migrations against ${cfg.jdbcUrl}")
```

### External Service HTTP Client & DTOs (`platform.http`)

```scala
package com.example.platform
package http

import zio.*

// External wire DTOs & infra errors (kept separate from domain models)
final case class ChargeRequestDto(orderRef: String, amountCents: Long)
final case class ChargeResponseDto(transactionId: String, status: String)

enum HttpClientError(val message: String):
  case RateLimited                               extends HttpClientError("Payment provider rate limit exceeded")
  case UnexpectedStatus(code: Int, body: String) extends HttpClientError(s"Unexpected HTTP status $code: $body")
  case TransportFailure(cause: Throwable)        extends HttpClientError(s"HTTP transport failure: ${cause.getMessage}")

// External HTTP Client API
trait PaymentHttpClient:
  def charge(req: ChargeRequestDto): IO[HttpClientError, ChargeResponseDto]

object PaymentHttpClient:
  val live: ULayer[PaymentHttpClient] =
    ZLayer.succeed(new PaymentHttpClient:
      override def charge(req: ChargeRequestDto): IO[HttpClientError, ChargeResponseDto] =
        ZIO.succeed(ChargeResponseDto(s"tx-${req.orderRef}", "OK"))
    )
```

### AWS ZIO Client Wrapper (`platform.aws`)

```scala
package com.example.platform
package aws

import zio.*

enum AwsError(val message: String):
  case BucketNotFound(bucket: String) extends AwsError(s"S3 bucket not found: $bucket")
  case Unexpected(cause: Throwable)   extends AwsError(s"Unexpected AWS error: ${cause.getMessage}")

// Thin ZIO wrapper over AWS SDK (e.g. S3AsyncClient)
trait S3Client:
  def putObject(bucket: String, key: String, bytes: Chunk[Byte]): IO[AwsError, Unit]

object S3Client:
  val live: ULayer[S3Client] =
    ZLayer.scoped:
      ZIO
        .acquireRelease(ZIO.logInfo("Creating AWS S3AsyncClient"))(_ =>
          ZIO.logInfo("Closing AWS S3AsyncClient")
        )
        .as(new S3Client:
          override def putObject(bucket: String, key: String, bytes: Chunk[Byte]): IO[AwsError, Unit] =
            ZIO.logInfo(s"[S3] PUT s3://$bucket/$key (${bytes.size} bytes)")
        )
```

### Platform Assembly (`PlatformModule.scala`)

Assembles the shared infrastructure environment using `ZLayer.makeSome`:

```scala
package com.example.platform

import aws.S3Client
import db.{Database, DbConfig, DbError}
import http.PaymentHttpClient
import zio.*

object PlatformModule:
  type Env = Database & PaymentHttpClient & S3Client

  val live: ZLayer[DbConfig, DbError, Env] =
    ZLayer.makeSome[DbConfig, Env](
      Database.live,
      PaymentHttpClient.live,
      S3Client.live
    )
```

---

## 5. Adapters: Dedicated Packages per Subproject (`orders.db`, `orders.http.api`, `orders.http`)

Each adapter subproject owns its own sub-package under `com.example.orders` (keeping `private[orders]` visibility and `com.example.orders` auto-imports). Only outbound adapters (`orders.db`, `orders.acl.*`) import `com.example.platform.*`.

### Outbound Persistence Adapter (`orders/02-o-infra` → `com.example.orders.db`)

```scala
package com.example.orders
package db

import core.*
import com.example.platform.db.{Database, DbError}
import zio.*

// Translates DbError -> domain error / defect
private[orders] final class PostgresOrderRepo(db: Database) extends OrderRepository:
  override def save(order: Order): IO[OrderError.AlreadyExists, Unit] =
    db.execute(s"INSERT INTO orders (id, amount) VALUES ('${order.id.value}', ${order.amount})")
      .catchAll:
        case DbError.UniqueViolation(_) => ZIO.fail(OrderError.AlreadyExists(order.id))
        case DbError.Unexpected(cause)  => ZIO.die(cause)

private[orders] object PostgresOrderRepo:
  val layer: ZLayer[Database, Nothing, OrderRepository] =
    ZLayer.fromFunction(PostgresOrderRepo.apply)
```

### Pure HTTP Contract (`orders/01-i-tapir-api` → `com.example.orders.http.api`)

```scala
package com.example.orders.http.api

import java.util.UUID

// Pure HTTP wire DTOs & Tapir endpoint specs (zero core dependency)
final case class CreateOrderRequest(customerId: UUID, amount: BigDecimal)
final case class CreateOrderResponse(orderId: UUID)
```

### Inbound HTTP Router (`orders/02-i-http` → `com.example.orders.http`)

Attach cross-cutting context (`requestId`, `traceId`) once at the inbound edge using `ZIO.logAnnotate` (backed by ZIO's `FiberRef`) so all downstream `core` and `db` logs include it automatically without threading a `Context` parameter through service signatures.

`OrderHttpRoutes` is a **public type with a `private[orders]` constructor**: `app/` must be able to name it in order to mount it on the server, but only code inside `orders` (i.e. `OrdersModule`) can create one. In real code the public surface would be Tapir's `List[ZServerEndpoint[Any, Any]]`; `HttpResponse` stands in for that here.

```scala
package com.example.orders
package http

import api.{CreateOrderRequest, CreateOrderResponse}
import zio.*

// Bridges 01-i-tapir-api <-> 01-c-core (translates OrderError -> HTTP response)
final case class HttpResponse(status: Int, body: String)

final class OrderHttpRoutes private[orders] (service: OrderService):
  def handlePostOrder(requestId: String, req: CreateOrderRequest): UIO[HttpResponse] =
    ZIO.logAnnotate("requestId", requestId):
      service.placeOrder(CreateOrderCmd(req.customerId, req.amount)).fold(
        {
          case e: OrderError.InvalidAmount => HttpResponse(400, e.message)
          case e: OrderError.AlreadyExists => HttpResponse(409, e.message)
          case e: OrderError.NotFound      => HttpResponse(404, e.message)
        },
        id => HttpResponse(201, CreateOrderResponse(id.value).orderId.toString)
      )

private[orders] object OrderHttpRoutes:
  val layer: ZLayer[OrderService, Nothing, OrderHttpRoutes] =
    ZLayer.fromFunction(new OrderHttpRoutes(_))
```

---

## 6. Module Wiring (`OrdersModule.scala`) & Composition Root

`OrdersModule` lives in `orders/03-wiring`, the only subproject that depends on both `02-i-http` and `02-o-infra`. Being in package `com.example.orders`, it can wire every `private[orders]` piece, while its signature exposes only public types (`OrderService`, `OrderHttpRoutes`). `Main.scala` therefore assembles the application graph without seeing internal `core` or `db` types. `PlatformModule.live` supplies `Database`, `PaymentHttpClient`, and `S3Client` to the feature modules.

```scala
package com.example.orders

import core.*
import db.PostgresOrderRepo
import http.OrderHttpRoutes
import com.example.platform.db.Database
import com.example.shared.EventBus
import zio.*

object OrdersModule:
  // Internal dependencies (OrderRepository) are completely encapsulated;
  // platform infrastructure (Database) is injected from Main.
  // OrderService is exported for other contexts' ACLs; OrderHttpRoutes for the HTTP server.
  val live: ZLayer[EventBus & Database, Nothing, OrderService & OrderHttpRoutes] =
    ZLayer.makeSome[EventBus & Database, OrderService & OrderHttpRoutes](
      PostgresOrderRepo.layer,
      OrderServiceLive.layer,
      OrderHttpRoutes.layer
    )
```

```scala
package com.example

import orders.*
import orders.http.OrderHttpRoutes
import orders.http.api.CreateOrderRequest
import billing.*
import platform.PlatformModule
import platform.db.{DbConfig, DbError}
import shared.EventBus
import zio.*

object Main extends ZIOAppDefault:
  override def run: ZIO[Any, DbError, Unit] =
    // Stands in for mounting the routes on an HTTP server (e.g. ZioHttpInterpreter(...).toHttp(endpoints))
    val program: ZIO[OrderHttpRoutes, Nothing, Unit] =
      for
        routes <- ZIO.service[OrderHttpRoutes]
        res    <- routes.handlePostOrder("req-1", CreateOrderRequest(java.util.UUID.randomUUID(), BigDecimal("99.50")))
        _      <- ZIO.logInfo(s"POST /orders -> ${res.status} ${res.body}")
      yield ()

    program.provide(
      ZLayer.succeed(DbConfig("jdbc:postgresql://localhost/app", "app", "secret")),
      PlatformModule.live,
      EventBus.live,
      OrdersModule.live,
      BillingModule.live
    )
```

---

## 7. Multi-Module Build (`build.sbt`) — Compile-Time Boundary Enforcement

Scala's `private[...]` visibility isolates internals *between* bounded contexts, while a **multi-module build** enforces layer boundaries at compile time: every `01-c-core` depends **only** on `shared` (never on `platform`, `01-i-tapir-api`, or another context's `01-c-core`), `02-i-http` cannot see `02-o-infra` or `platform`, and `02-o-infra` cannot see HTTP contracts/routes.

```scala
// build.sbt
lazy val shared   = project.in(file("shared"))
lazy val platform = project.in(file("platform")).dependsOn(shared) // JDBC, Flyway, HTTP client, AWS SDK live ONLY here

// Bounded Context 1: Orders
lazy val ordersCore = project.in(file("orders/01-c-core"))
  .dependsOn(shared)                              // Pure domain + public API; CANNOT import platform or 01-i-tapir-api

lazy val ordersTapirApi = project.in(file("orders/01-i-tapir-api"))
                                                  // Pure Tapir endpoints & HTTP DTOs; zero internal deps (client/OpenAPI-safe)

lazy val ordersHttp = project.in(file("orders/02-i-http"))
  .dependsOn(ordersTapirApi, ordersCore)          // Inbound HTTP router (OrderHttpRoutes); CANNOT import 02-o-infra or platform

lazy val ordersInfra = project.in(file("orders/02-o-infra"))
  .dependsOn(ordersCore, platform)                // Outbound adapters (PostgresOrderRepo)
  // When outbound adapters grow, split into:
  //   ordersDb         = project.in(file("orders/02-o-db")).dependsOn(ordersCore, platform)
  //   ordersHttpClient = project.in(file("orders/02-o-http")).dependsOn(ordersCore, platform)

lazy val ordersWiring = project.in(file("orders/03-wiring"))
  .dependsOn(ordersHttp, ordersInfra)             // OrdersModule: the only place that sees inbound + outbound adapters
  // Simpler alternative for small contexts — one adapters module replaces ordersHttp, ordersInfra and ordersWiring:
  //   ordersAdapters = project.in(file("orders/02-adapters")).dependsOn(ordersCore, ordersTapirApi, platform) // + OrdersModule

// Bounded Context 2: Billing
lazy val billingCore = project.in(file("billing/01-c-core"))
  .dependsOn(shared)                              // Pure domain + own outbound ports; ZERO dependency on ordersCore!

lazy val billingEvents = project.in(file("billing/02-i-events"))
  .dependsOn(billingCore, ordersCore)             // Async: translates orders.OrderPlaced -> billing commands

lazy val billingAclOrders = project.in(file("billing/02-o-acl-orders"))
  .dependsOn(billingCore, ordersCore)             // Sync: implements billing's outbound port by calling orders.OrderService

lazy val billingInfra = project.in(file("billing/02-o-infra"))
  .dependsOn(billingCore, platform)

lazy val billingWiring = project.in(file("billing/03-wiring"))
  .dependsOn(billingEvents, billingAclOrders, billingInfra) // BillingModule

// Composition Root — sees only each context's public types via its 03-wiring module
lazy val app = project.in(file("app"))
  .dependsOn(ordersWiring, billingWiring, platform)
```

### Cross-Module Dependencies: ACL (`02-o-acl-*`) vs. Splitting `01-c-api`

- **Recommended — ACL on tier `02` (`02-o-acl-<other>`):** Every `01-c-core` defines its own outbound port in its own domain language and depends only on `shared`. All cross-context links live on tier `02` (`02-o-acl-*` for sync calls, `02-i-events` for async events), which makes **cyclic dependencies between cores structurally impossible** (even when `orders` and `billing` both call each other), enforces true DDD anti-corruption, and makes extracting a module into a separate service a single-adapter swap (in-process call → HTTP/gRPC client).
- **Alternative — Split `01-c-api` + `02-c-core` + `03-*`:** Extract public DTOs, events, errors, and service traits into a standalone `01-c-api` subproject, moving domain logic to `02-c-core` and adapters to `03-*`. Trade-off: it doubles core subprojects and couples callers directly to another context's DTOs if used from `02-c-core`, so prefer keeping `01-c-core` merged + ACLs by default, and consider `01-c-api` mainly as a **compile-caching optimization** if a `01-c-core` grows large with many downstream tier-`02` adapters.

---

## 8. Common Pitfall: Never Leak Infrastructure Concerns from Adapters

Outbound ports defined in `core` and implemented in outbound adapters (`db`, `acl`, etc.) must act as an **anti-corruption boundary** — they must always speak the **domain language** (domain types and domain errors) and never expose infrastructure details.

- **Speak domain language only:** Port signatures should accept and return domain aggregates, value objects, and domain enums — never SQL row types, HTTP wire DTOs (`ChargeRequestDto`), or AWS SDK classes. Adapters translate at the edge.
- **Never leak infrastructure errors:** Do not surface `SQLException`, Postgres error codes, HTTP status exceptions, or AWS SDK errors to `core`. Catch and map expected failure modes into domain errors inside the adapter (e.g., `DbError.UniqueViolation` → `OrderError.AlreadyExists`), and let truly unrecoverable infra failures fail fast as defects (`ZIO.die`) or a dedicated infrastructure error channel.
- **Repositories own aggregate storage (and their own transaction):** A repository should load and persist whole **aggregates** atomically. Because a well-designed aggregate is already the consistency boundary, the DB transaction belongs *inside* the repository method (`repo.save(order)`), not in `core`.
- **Never leak DB transactions (`Connection`, `Session`, `ConnectionIO`) into `core`:**
  - If cross-repository atomicity is strictly unavoidable, you *can* hide the mechanism behind an abstract trait (e.g., `trait UnitOfWork: def atomically[E, A](fa: IO[E, A]): IO[E, A]`), **but use this sparingly**.
  - Giving application services control over transaction scopes creates a major footgun: someone will eventually call an external HTTP client or AWS API *inside* the transaction block, holding DB locks and pool connections open for seconds or minutes.
  - When a workflow spans multiple aggregates, bounded contexts, or external HTTP/AWS calls, avoid long-lived DB transactions and use **Saga**, **Transactional Outbox**, or **Process Manager** patterns instead.

---

## 9. Configuration & Module Placement

### Configuration: Co-Locate Types, Centralize Loading in `app/`

Avoid creating a global `config/` subproject. Instead, declare each config case class next to the module that consumes it, and parse/load everything once in `app/`:

- **Infrastructure configs** (`DbConfig`, `PaymentHttpConfig`, `S3Config`) → live in `platform` (`platform.db`, `platform.http`, `platform.aws`).
- **Feature adapter configs** (e.g., `ShippingAclConfig`) → live inside that adapter subproject (`02-o-acl-shipping`).
- **Domain / business policy configs** (e.g., `OrderPolicy(maxAmount: BigDecimal, reservationTtl: Duration)`) → live in `01-c-core` as plain case classes with zero config-library dependencies.
- **Root `AppConfig` loading** → lives in `app/` (`Main.scala`), which reads `application.conf` / env vars (`zio-config` or ZIO's `ConfigProvider`) and slices sub-configs into individual `ZLayer`s:

```scala
// app/src/main/scala/com/example/AppConfig.scala
package com.example

import platform.db.DbConfig
import zio.*

final case class AppConfig(
  db: DbConfig
  // Add feature policies & adapter configs here as modules grow
)

object AppConfig:
  val dbLayer: ZLayer[AppConfig, Nothing, DbConfig] =
    ZLayer.service[AppConfig].project(_.db)
```

Because every module only declares and requires its own narrow config case class, splitting `02-o-infra` into `02-o-db` and `02-o-http` never forces shared config dependencies between adapters.

### Quick Placement Rule

| What is it? | Where it goes | Examples |
| :--- | :--- | :--- |
| Pure domain primitives / ZIO helpers shared across cores | `shared/` | `EventBus`, `Money`, `PageRequest`, common IDs |
| Reusable technical / SDK / runtime capability | `platform/` | `db`, `http`, `aws`, `telemetry` (OpenTelemetry/metrics) |
| Driving trigger calling a feature's `Service` | `<feature>/02-i-*` | `02-i-http`, `02-i-events`, `02-i-grpc` |
| Outbound persistence or external system ACL | `<feature>/02-o-*` | `02-o-infra`, `02-o-db`, `02-o-http`, `02-o-acl-*` |
| Context's ZLayer assembly (core + all adapters) | `<feature>/03-wiring` | `OrdersModule`, `BillingModule` |
| App-wide config loading, server boot, health endpoints | `app/` | `AppConfig`, `Main.scala`, HTTP server wiring |