# Modular Architecture — Feature Modules, DDD, Hexagonal

> **Status:** proposed. This document defines the target architecture for decomposing the monolithic
> `domain` module into feature-aligned bounded contexts. The order-intake context is specified
> separately in [order-intake.md](order-intake.md).
>
> **References:** [scala-project-style-guide](https://github.com/david-lebl/scala-project-style-guide)
> for module/package structure and the dependency rules;
> [Modeling in Scala](https://kubuszok.com/2024/modeling-in-scala-part-1/) for the domain-modeling
> conventions in §5.

## 1. Why

Today the repo is a **single 14.5k-LOC `domain` module** plus four Scala.js UI modules. There is
**no backend at all**: no HTTP, no database, no persistence beyond `localStorage`.
`mpbuilder.domain.sample.*` is the de-facto database, imported by 12 of 23 test specs. "Place Order"
in `modules/ui/src/main/scala/mpbuilder/ui/components/CheckoutView.scala` clears the basket, resets
the form and navigates away — **no order is ever created, stored, or transmitted**.

The domain itself is good (rules-as-data, `Validation` error accumulation, opaque IDs, effect-free so
it cross-compiles to Scala.js) but it has **no internal boundaries**:

- flat `mpbuilder.domain.{model, pricing, rules, validation, weight, manufacturing, service, codec, sample}`
- `service/` is a 24-file bag mixing five prospective bounded contexts
- `model ⇄ pricing` is a genuine **cycle** — `model/{order,basket,customer,discount}.scala` import
  `pricing`, while all of `pricing` imports `model`. It only compiles because it is one compile unit.
- `model/manufacturing.scala:370` — `ManufacturingOrder` **embeds the whole `Order`**, so
  manufacturing hard-depends on the order aggregate and, through it, on live catalog entities
- `model/ids.scala` holds 10 opaque IDs spanning every future module in one file
- two competing discount systems: the rich, rules-aware `DiscountCodeService` versus the hardcoded
  `DiscountService.lookupPercent` that the checkout UI actually honours
  (see [`ideas/from-scratch-specification.md`](../ideas/from-scratch-specification.md) §15)

**Goal.** Decompose into feature-aligned bounded contexts, each with a two-layer hexagonal split
(`01-core` / `02-infra`), deployed as **one JVM process**, with cross-context calls restricted to
anti-corruption ports — so any context can later be lifted into its own service without rewriting
its internals.

## 2. Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Deployment | **Modular monolith**, one JVM process | Boundaries strict enough to extract later, without paying operational cost now |
| Module split | **Two layers**: `01-core`, `02-infra` | Public API and domain model inside a context are tightly coupled; a third contract module would be ceremony. Package visibility does the separating instead. |
| Backend stack | **ZIO 2 + tapir + Postgres** | tapir gives OpenAPI *and* a typed Scala.js client for the existing Laminar frontend |
| Identity | **Own `identity` context**, password + JWT | Full control; existing OTP business sign-in kept as a second method |
| Sequencing | **Thin slice first**, extractions back-filled | Order intake ships early; extractions land behind ports that are already proven |
| Basket | **Server-side**, owned by order-intake | Cross-device basket, authoritative re-pricing, abandoned-cart data |
| `ui-calculator` | **Stays fully offline** | It embeds on third-party sites. Constrains `catalog`/`pricing` `01-core` to stay JVM+JS and effect-free |
| `build.sbt` | **Deleted**, along with `project/` and stale `target/` | Mill is already the only build in CI and docs |

## 3. Module structure

Each bounded context is **two** build modules. There is no separate contract module — the *public
package* of `01-core` **is** the contract, and everything under `impl/` is free to change.

```
modules/<context>/
  01-core/     Public API  — service traits, DTOs, error ADTs, ID types.  (stable contract)
               impl/       — domain model, ports, Live implementations.   (private[<context>])
  02-infra/    impl/postgres/  repositories + DAOs
               impl/http/      tapir endpoints + server logic
               impl/adapters/  anti-corruption adapters onto other contexts
```

### 3.1 The cardinal rule

> **The public package of `01-core` is a stable contract. Everything in `impl` is free to change.**

Enforced by scoping: every file under `impl` is `private[<context>]`; infrastructure sub-packages
narrow further with `private[postgres]`, `private[http]`.

### 3.2 The three dependency rules (non-negotiable)

```
01-core   →  commons only.  NEVER another context's 01-core, never any 02-infra.
02-infra  →  its own 01-core, plus other contexts' 01-core (to write adapters).
             NEVER another context's 02-infra.
app       →  every 02-infra.
```

This is the part that makes extraction mechanical, and it is stricter than "modules talk through
interfaces". A context's core does not know that the other contexts *exist*. When order-intake needs
a price, it declares **its own port, in its own vocabulary**, in
`order-intake/01-core/.../impl/PricingPort.scala`, and an adapter in
`order-intake/02-infra/.../impl/adapters/PricingAdapter.scala` implements it by calling
`pricing`'s public `PricingService`. Extracting pricing to a service later replaces exactly one
class — that adapter.

`ui-*` modules are clients, not cores; they may depend on several `01-core` modules (JS targets).

### 3.3 Package layout and chained clauses

Package root per context: `mpbuilder.catalog`, `mpbuilder.pricing`, `mpbuilder.customers`,
`mpbuilder.identity`, `mpbuilder.orderintake`, `mpbuilder.manufacturing`, `mpbuilder.commons`.

```scala
// order-intake/02-infra/src/main/scala/mpbuilder/orderintake/impl/postgres/PostgresOrderRepository.scala
package mpbuilder.orderintake
package impl
package postgres

// every enclosing package is now in scope — no imports for OrderService, Order, OrderRepository
private[postgres] final class PostgresOrderRepository(...) extends OrderRepository:
  ...
```

### 3.4 Three representations, converted explicitly

| Representation | Purpose | Lives in |
|---|---|---|
| **Domain** | business rules, rich types, illegal states unrepresentable | `01-core/impl` |
| **DTO** | wire/serialization shape, what the public service trait speaks | `01-core` public package |
| **DAO** | table shape, flat and nullable | `02-infra/impl/postgres` |

Repository ports are declared in terms of **domain entities**; DAO mapping is the adapter's private
business. Never let a DAO leak past `impl/postgres`, and never let a domain entity onto the wire.

## 4. Context map

```
modules/
  commons/                    shared kernel — JVM+JS, depends on nothing
  catalog/         01-core  02-infra
  pricing/         01-core  02-infra
  customers/       01-core  02-infra
  identity/        01-core  02-infra
  order-intake/    01-core  02-infra
  manufacturing/   01-core  02-infra
  artwork/         01-core  02-infra      (later)
  notifications/   01-core  02-infra      (later)
  app/                        composition root: ZLayer wiring, one tapir server, Flyway
  legacy-domain/              temporary: today's `domain`, shrinking each phase
  ui-framework/ ui-productbuilder/ ui-calculator/ ui/ ui-showcase/   (unchanged)
```

### Dependency graph

```
                              commons
                                 ▲
   ┌──────────┬──────────┬───────┴──┬───────────┬──────────────┬──────────────┐
catalog     pricing   customers  identity   order-intake   manufacturing    (cores — mutually blind)
 01-core     01-core    01-core    01-core      01-core        01-core
   ▲            ▲          ▲          ▲            ▲              ▲
   └────────────┴──────────┴──────────┴────────────┼──────────────┘
                                                   │
                   order-intake/02-infra ──────────┘   adapters call other cores' public services
                                                   
                                  app  →  every 02-infra
```

### 4.1 `commons` — shared kernel (JVM + JS)

Everything genuinely shared, owned by nobody. Extracting this is what **breaks the `model ⇄ pricing`
cycle**.

| Type | Comes from |
|---|---|
| `Money`, `Currency`, `Price` | `pricing/Money.scala` — already has zero domain imports, just misplaced |
| `Percentage` | `pricing/CustomerPricing.scala:7` |
| `Language`, `LocalizedString` | `model/language.scala` |
| `Dimension`, `Quantity` | `model/specification.scala` |
| `Timestamp` | replaces the raw `Long` epoch millis used throughout |
| `NewType` / `Id` idiom | the `apply → Validation` / `unsafe` / `.value` pattern, copy-pasted ~16× |
| `DomainError` trait | `def message(lang: Language)` — repeated in 9 error ADTs |
| `Estimated[+A]` | see §5.4 |

`commons` holds **no business concepts** — no `CategoryId`, no `CustomerType`. Those belong to a
context. If two contexts need the same business concept, that is a signal to merge them or to add an
anti-corruption port, not to widen `commons`.

### 4.2 `catalog` — what can be built

**Public:** `CatalogService`, `ConfigurationService`, `CatalogError`, DTOs (`CategoryView`,
`MaterialView`, `ConfigurationRequest`, `ConfigurationView`), ID types (`Category.Id`, `Material.Id`,
`Finish.Id`, `PrintingMethod.Id`, `Configuration.Id`, `Preset.Id`).

**`impl`:** `ProductCatalog`, `ProductCategory`, `Material`, `Finish`, `PrintingMethod`,
`ComponentTemplate`, `CategoryPreset`, `ShowcaseProduct`, `ProductSpecifications`,
`ProductConfiguration`, `CompatibilityRule`/`Ruleset`/predicates, `RuleEvaluator`,
`ConfigurationValidator`, `ConfigurationBuilder`, `CatalogQueryService`, `WeightCalculator`, codecs.

> **Constraint:** `01-core` stays JVM+JS and effect-free — `ui-calculator` runs it in the browser.

### 4.3 `pricing` — what it costs

**Public:** `PricingService`, `DiscountService`, `PricingError`, DTOs (`PriceQuote`,
`PriceBreakdownView`, `SpeedOffer`, `AppliedDiscount`).

**`impl`:** `Pricelist`, `PricingRule` (~28 cases), `PriceCalculator`, `PriceBreakdown`,
`PricingContext`, `BusyPeriodMultiplier`, `QueueThreshold`, `CustomerPricing` +
`CustomerPricelistResolver`, `ProductionCost*`, `DiscountCode` + validation rules.

Discount codes live here, not in order-intake: they are price-affecting rules, and putting them here
is what lets us **delete `DiscountService.lookupPercent`** and close the §15 dual-discount gap by
construction — order-intake only ever asks for a quote, and the quote already has the discount applied.

`PricingContext` carries queue utilisation, so pricing is manufacturing-aware. It stays one-way:
pricing's public API takes `PricingContext` as plain input data; `pricing/02-infra` obtains it via a
`ShopLoadPort` adapted onto manufacturing's public service (today's
`UtilisationCalculator.buildPricingContext` becomes exactly that adapter).

> Same JVM+JS, effect-free constraint as catalog.

### 4.4 `customers` — who we sell to

**Public:** `CustomerService`, `CustomerError`, `Customer.Id`, DTOs (`CustomerView`,
`CustomerSummary`, `RegisterCustomer`).
**`impl`:** `Customer`, `CompanyInfo`, `CustomerNote`, `CustomerStatus`, `CustomerTier`, CRM logic.

`Customer` currently embeds `pricing.CustomerPricing`. Under the dependency rules that is now
illegal, and the fix is the right one anyway: `customers` stores a `PricingProfile.Id` reference,
and **pricing owns the negotiated-pricing overlay**. `pricing/02-infra` resolves the profile itself.
This also fixes the §15 "customer tier is a label, not a pricing mechanism" gap — tier→discount
mapping becomes pricing's business, where it can actually be applied.

### 4.5 `identity` — who is signed in *(mostly greenfield)*

**Public:** `AuthService`, `TokenService`, `AuthError`, `Principal`, `Role`, `User.Id`.
**`impl`:** `User` (credentials), password hashing, JWT issue/verify/refresh, `AuthSession`, and the
existing OTP flow (`OtpRequest`, `OtpToken`, `LoginSession`, `LoginService` — identify by IČO / DIČ /
e-mail, 5-minute OTP, 24-hour session), reframed as one `AuthenticationMethod` among several.

> **Critical separation:** `User` = credentials + roles (identity). `Customer` = business profile,
> company data, pricing, CRM notes (customers). Linked by `User.customerId: Option[Customer.Id]`.
> Today there is no `User` type at all, and `CheckoutInfo.loginPassword` is a vestigial field that
> nothing consumes — it is deleted.

**Roles:** `Customer`, `StaffOperator`, `StaffManager`, `Admin`. `Employee` (manufacturing) gains a
`userId` so staff can actually sign in — today `Employee` has station capabilities but no login.

### 4.6 `order-intake`

Basket, checkout, the `Order` aggregate and its lifecycle, order numbers, order history.
Specified in full in **[order-intake.md](order-intake.md)**.

### 4.7 `manufacturing` — how it gets made

**Public:** `ProductionService`, `WorkforceService`, `ShopLoadService`, `AnalyticsService`,
`ManufacturingError`, DTOs.
**`impl`:** everything in `model/manufacturing.scala` (workflows, steps, stations, `Employee`,
`Machine`, `ArtworkCheck`, `FulfilmentChecklist`), the `manufacturing/` config package
(`ShopSchedule`, `WorkingHours`, `StationTimeEstimate`, `StationUtilisation`, `TierRestriction`,
`CategoryTierConfig`), and `WorkflowGenerator`, `WorkflowEngine`, `QueueScorer`,
`CompletionEstimator`, `UtilisationCalculator`, `TierRestrictionValidator`.

**The key change:** `ManufacturingOrder` stops embedding `Order`. Manufacturing declares its own
`ProductionOrder` in its own vocabulary; `manufacturing/02-infra` subscribes to order-intake's
`OrderPlaced` event and translates. Approval, payment and fulfilment status stay here — that is
genuinely manufacturing's lifecycle, distinct from the order's commercial lifecycle.

### 4.8 `artwork`, `notifications` *(later)*

- **`artwork`** — visual-editor sessions, canvas elements, uploaded assets; today entirely
  client-side in `modules/ui/visualeditor/` + `modules/ui/persistence/` (localStorage). Order lines
  reference an `Artwork.Id`; order-intake never sees canvas data.
- **`notifications`** — order confirmation, OTP delivery, status e-mails; replaces the `mailto:`
  approach in `EmailOrderModal.scala`.

## 5. Domain modeling conventions

These apply inside every context's `impl` package. The existing domain already does some of this
(opaque IDs, rules-as-ADTs, `Validation`); this section makes the rest uniform.

### 5.1 Nest types in the companion of what they belong to

Entity files carry their own vocabulary rather than scattering it across a shared `ids.scala` and
`model/*.scala`. This is what replaces today's `model/ids.scala` holding 10 IDs for 6 contexts.

```scala
final case class Order(id: Order.Id, data: Order.Data)

object Order:
  opaque type Id = UUID
  object Id:
    def apply(raw: UUID): Id = raw
    def parse(s: String): Either[Order.Error, Id] = ...
    extension (id: Id) def value: UUID = id

  final case class Number(year: Int, sequence: Int):
    override def toString = f"MP-$year-$sequence%06d"

  final case class Data(
      number:   Number,
      status:   Status,
      buyer:    Buyer,
      lines:    NonEmptyList[Line],
      delivery: Delivery,
      payment:  Payment,
      totals:   Totals,
      placedAt: Timestamp
  )

  final case class Line(id: Line.Id, ...)
  object Line:
    opaque type Id = UUID
```

Referring code reads `Order.Id`, `Order.Status.Paid`, `Order.Line` — unambiguous without imports,
and the compiler stops `Customer.Id` from being passed where `Order.Id` is wanted.

### 5.2 Hierarchical status

A flat `enum Status { case Unpaid, Paid, InProgress, Shipped, Cancelled }` forces every predicate to
re-enumerate leaves, and every new leaf breaks every match. Model the **groups** as an intermediate
sealed layer:

```scala
sealed trait Status
object Status:
  /** Commercially live; production not yet authorised. Customer may still act. */
  sealed trait Open   extends Status
  /** Production authorised; the shop owes goods. */
  sealed trait Active extends Status
  /** Terminal — no further work will happen. */
  sealed trait Closed extends Status
  object Closed:
    sealed trait Fulfilled   extends Closed
    sealed trait Unfulfilled extends Closed
```

Business logic then dispatches on the **group**, and stays correct when a leaf is added:

```scala
def canCustomerCancel(o: Order): Boolean = o.data.status match
  case _: Status.Open   => true
  case _: Status.Active => false   // staff only
  case _: Status.Closed => false
```

The concrete hierarchy for `Order.Status` is in [order-intake.md](order-intake.md) §3.3.
The same shape applies to `Workflow.Status` in manufacturing, whose current flat
`WorkflowStatus`/`StepStatus` pair already begs for `Terminal` vs `Live` groups.

### 5.3 Sum types instead of correlated `Option`s

Wherever several nullable fields must agree, the model is lying. Replace with a sum. Concrete cases
in this codebase:

```scala
// today: ContactInfo(..., company: Option[String], companyRegNo: Option[String], vatId: Option[String])
//        — three Options that must be all-set or all-empty, enforced nowhere
enum Buyer:
  case Individual(name: PersonName, email: Email, phone: Phone)
  case Business(name: PersonName, email: Email, phone: Phone, company: CompanyInfo)

// today: DeliveryOption.PickupAtShop(locationId: String) + a separate surcharge lookup
enum Delivery:
  case Pickup (location: ShopLocation.Id, name: LocalizedString)
  case Courier(service: CourierService.Id, name: LocalizedString,
               surcharge: Money, estimate: LocalizedString)

// today: Basket has a nullable customer_id AND a nullable session_token
enum Owner:
  case Anonymous (session: Session.Token)
  case Registered(customer: Customer.Id)
```

**Corollary — don't model states you cannot handle.** `PaymentMethod.Card` exists today but is
non-functional ("coming soon", §15), so every consumer must reject it at runtime. It is simply
absent from the `Payment` ADT until it works; the UI shows a disabled affordance, and the domain
cannot represent an unpayable order.

### 5.4 Say *why* something is absent

`Option[A]` says "nothing here". Often the interesting information is the reason.

```scala
enum Estimated[+A]:                            // commons
  case Known(value: A)
  case Unavailable(reason: Estimated.Reason)
```

Two live cases:

- **Completion dates.** `BuilderEnvironment.completionText` returns `Option[String]` today, and the
  standalone calculator deliberately has no dates (indicative ranges + disclaimer instead). The
  reason — `IndicativeOnly` vs `ShopScheduleUnknown` — is exactly what the disclaimer text needs.
- **Speed availability.** Spec §8.3 lists four distinct reasons Express can be off the table. A
  `Set[ManufacturingSpeed]` throws all four away:

```scala
enum SpeedOffer:
  case Available  (speed: ManufacturingSpeed, surcharge: Money)
  case Unavailable(speed: ManufacturingSpeed, reason: SpeedOffer.Reason)
object SpeedOffer:
  enum Reason:
    case ShopSaturated
    case QuantityAboveCap(cap: Quantity)
    case BindingRequiresCuring(binding: BindingMethod)
    case MaterialExcluded(material: Material.Id)
```

This also converts the §15 "rush restriction fields defined but not enforced" gap from a silent
omission into a missing `Reason` case the compiler asks about.

### 5.5 Smart constructors, parse don't validate

Constructors private; parsing at the edge returns `Validation[E, A]` (keeping the codebase's
established error-accumulation contract, not `Either`). Once constructed, a value is valid
everywhere downstream and no inner layer re-checks it. This is already the convention for the opaque
IDs — extend it to `Email`, `PersonName`, `PostalCode`, `VatId`, `Quantity`.

## 6. Build wiring

Matching the existing `build.mill` style — explicit `Task.Sources` overrides, backticked names,
shared version vals.

```scala
val zioVersion    = "2.1.16"
val tapirVersion  = "1.11.13"
val quillVersion  = "4.8.6"       // quill-jdbc-zio
val flywayVersion = "11.3.0"

val coreMvnDeps = Seq(
  mvn"dev.zio::zio::${zioVersion}",
  mvn"dev.zio::zio-prelude::1.0.0-RC39",
  mvn"dev.zio::zio-json::0.7.3"
)

val infraMvnDeps = coreMvnDeps ++ Seq(
  mvn"com.softwaremill.sttp.tapir::tapir-zio-http-server::${tapirVersion}",
  mvn"com.softwaremill.sttp.tapir::tapir-json-zio::${tapirVersion}",
  mvn"com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle::${tapirVersion}",
  mvn"io.getquill::quill-jdbc-zio::${quillVersion}",
  mvn"org.postgresql:postgresql:42.7.5",
  mvn"org.flywaydb:flyway-core:${flywayVersion}",
  mvn"org.flywaydb:flyway-database-postgresql:${flywayVersion}",
  mvn"com.github.jwt-scala::jwt-zio-json::10.0.1",
  mvn"org.bouncycastle:bcprov-jdk18on:1.80"
)
```

Two shapes of core. `catalog` and `pricing` cross-compile (the offline widget); everything else is
JVM-only. Define `CoreModule` / `CrossCoreModule` / `InfraModule` traits once rather than
copy-pasting ~14 near-identical blocks:

```scala
object `order-intake` extends Module {
  private val base = rootDir / "modules" / "order-intake"

  object `01-core` extends ScalaModule {
    def scalaVersion  = scala3Version
    def scalacOptions = commonScalacOptions
    def sources       = Task.Sources(base / "01-core" / "src" / "main" / "scala")
    def moduleDeps    = Seq(commons.jvm)                 // ← commons ONLY
    def mvnDeps       = coreMvnDeps
    object test extends ScalaTests { /* zio-test, as domain.jvm.test does today */ }
  }

  object `02-infra` extends ScalaModule {
    def scalaVersion  = scala3Version
    def scalacOptions = commonScalacOptions
    def sources       = Task.Sources(base / "02-infra" / "src" / "main" / "scala")
    def resources     = Task.Sources(base / "02-infra" / "src" / "main" / "resources")  // db/migration
    def moduleDeps    = Seq(                              // own core + other cores, for adapters
      `01-core`,
      catalog.`01-core`.jvm, pricing.`01-core`.jvm,
      customers.`01-core`, identity.`01-core`
    )
    def mvnDeps       = infraMvnDeps
    object test extends ScalaTests { /* + testcontainers-backed Postgres */ }
  }
}
```

`app` depends on every `02-infra`, has a `ZIOAppDefault` main, one `ZLayer` graph, and one zio-http
server serving tapir routes + Swagger UI + the built SPA assets.

**Rule enforcement in CI:** a check asserting that no `01-core` module's `moduleDeps` contains
anything but `commons`, and that no `02-infra` references another `02-infra`. Add it in Phase 1,
while there are only three modules to police.

## 7. Implementation order

### Track A — foundations (first; unblocks everything)

**Phase 0 — `commons`. ✅ DONE.** Extracted `Money` / `Currency` / `Price` / `Percentage` /
`Language` / `LocalizedString` / `Dimension` / `Quantity` into `modules/commons` (cross-compiled
JVM+JS, `mpbuilder.commons`), and added `Timestamp`, `DomainError` and `Estimated[+A]`. All nine
bilingual error ADTs now extend `DomainError`, which supplies the English rendering they each used
to duplicate. `CommonsSpec` adds 23 tests covering `Money`/`Percentage` arithmetic, which had no
coverage at all before.
*Verified:* `mill __.compile` clean; `mill __.test` 648 passing (625 pre-existing + 23 new), 0
failing; `mill ui.fullLinkJS` and `mill ui-calculator.fullLinkJS` both link.

> **Cycle status — partly resolved, as expected.** The *kernel* edges are gone: `model` no longer
> imports `pricing` for `Money`/`Currency`, and `pricing` no longer owns `Percentage`. Two
> **business-concept** edges remain — `model/basket.scala → pricing.PriceBreakdown` and
> `model/customer.scala → pricing.CustomerPricing`. These cannot be removed by relocating shared
> types, because they are genuine references between two contexts' entities. They dissolve when
> `Basket` moves to order-intake (Phase 5) and `Customer` to customers (Phase 9). Until then the two
> packages remain mutually referencing inside the single `domain` compile unit.

**Phase 1 — first public services.** Create `catalog/01-core`, `pricing/01-core`,
`customers/01-core` holding only their **public service traits, DTOs and error ADTs**. Nothing else
moves yet. Add the CI dependency check.
*Verify:* everything compiles; service-contract tests exercise the façades against `SampleCatalog` /
`SamplePricelist`.

Two corrections to the original sketch, both found while doing Phase 0:

- **The delegating implementation goes in `02-infra`, not `01-core/impl`.** A `Live` inside
  `01-core` would have to depend on `domain`, breaking the "core depends on `commons` only" rule on
  day one. Putting the legacy-backed implementation in `catalog/02-infra` needs no exception — it is
  exactly what the infra layer is for, and swapping it for a real implementation later touches one
  module.
- **Renaming `domain` → `legacy-domain` is deferred to Phase 7.** It has 117 references across
  `CLAUDE.md`, `README.md`, seven guides and ten *historical* changelog entries that should not be
  rewritten. The rename buys nothing functional; it is worth doing once the module has actually
  shrunk, when the edit is smaller and means something.

**Known gap for Phase 1 sizing:** `codec/DomainCodecs.scala` has 92 `given`s but **none for
`ProductConfiguration`**, so there is no serialized form of a configuration today. Order-intake's
`ProductSpec` (a stored, versioned JSON snapshot) depends on one existing, so writing those codecs —
covering all 8 `SpecKind`s and all 7 `FinishParameters` variants — is part of Phase 1's catalog
façade, not a later detail.

**Phase 2 — `app` skeleton + `identity`.** Stand up `app` (ZIOAppDefault, zio-http, Flyway, Postgres
via docker-compose, Swagger UI), then build `identity` end to end: `User`, Argon2 hashing, JWT
issue/verify/refresh, roles, auth endpoints; port today's `LoginService` OTP flow in as a second method.
*Verify:* register → login → `/auth/me` against a real Postgres (testcontainers); the SPA's
`LoginWidget` signs in for real.

### Track B — order intake (the goal)

| Phase | Work | Verifiable when |
|---|---|---|
| **3** | Basket, server-side | Basket survives a page reload and a device switch; `ui-calculator` still works offline |
| **4** | Checkout & quoting; deletes `DiscountService.lookupPercent` | An expired / exhausted / min-order code from `SampleDiscountCodes` is correctly refused |
| **5** | The `Order` aggregate | Placing an order writes `orders`, `order_lines` and an `OrderPlaced` outbox row; the portal shows it |
| **6** | Events → manufacturing; `ManufacturingOrder` stops embedding `Order` | A placed order appears in the approval queue; approving it moves the order to `InProduction` |

Full detail in [order-intake.md](order-intake.md) §8.

### Track C — back-fill extractions (behind services already in use)

| Phase | Work |
|---|---|
| **7 — `catalog`** | Move catalog model + rules + validation + weight + codecs from `legacy-domain` into `catalog/01-core/impl`; keep JVM+JS. Split `SampleCatalog`/`SampleRules` into `catalog/testkit`. |
| **8 — `pricing`** | Same for pricing + discount codes + production cost + the customer pricing overlay. |
| **9 — `customers`** | Customer entity + CRM; tier→discount mapping lands in pricing. |
| **10 — `manufacturing`** | Split `model/manufacturing.scala` out of `model/`; move the six config files and eight services; persist workflows. |
| **11 — remainder** | `artwork`, `notifications`, analytics read-models. `legacy-domain` is now empty — delete it. |

Each is mechanical, because the public service contracts and their tests already exist and don't change.

## 8. Risks and handling

| Risk | Handling |
|---|---|
| **`model ⇄ pricing` cycle** | Phase 0 removed the kernel edges (done). The two remaining edges are entity references — `Basket → PriceBreakdown`, `Customer → CustomerPricing` — and are resolved by moving those entities to their contexts in Phases 5 and 9, not by relocating shared types. |
| **`ManufacturingOrder` embeds `Order`** | Phase 6. Manufacturing declares its own `ProductionOrder` and translates from the `OrderPlaced` event in its infra layer. `orderItemIndex` → `Order.Line.Id`. |
| **Two discount systems** | Discounts live only in `pricing`; order-intake only ever reads a quote. `DiscountService.lookupPercent` deleted in Phase 4. |
| **Anti-corruption ports feel like duplication** | They are — deliberately, and small. When two contexts share most of their vocabulary and always change together, the guide's advice applies: merge them into one context rather than adapt. Revisit if catalog and pricing start moving in lockstep. |
| **`sample/` is the database, and 12 of 23 specs import it** | Untouched until Phase 7, by which time everything real reads Postgres. Then split per context into `<context>/testkit`, one at a time, fixing that context's specs alongside. |
| **Scala.js constraint** | `commons` and `catalog`/`pricing` `01-core` stay cross-compiled and effect-free (the `ui-calculator` offline guarantee). Other cores are JVM-only. Every phase's verification includes `mill ui-calculator.fullLinkJS`. |
| **`Order` has zero tests today** | Phase 5 is the first thing with a real state machine — property-test the transition table over `Status` groups, and table-test every error's EN/CS message following the existing `LocalizationSpec` pattern. |
| **Surge pricing makes quotes go stale** | Explicit `PriceChanged` error plus confirm-and-retry, rather than silently repricing or silently honouring a stale quote. |
| **~14 new Mill modules** | Three shared module traits instead of copy-paste; `build.sbt` deleted so there is only one build to maintain. |
| **Drifting back into a ball of mud** | The dependency rules are machine-checkable (§6) and package visibility (`private[<context>]`) makes violations compile errors, not review findings. |

## 9. Verification baseline

Green at **every** phase boundary:

```bash
mill __.compile
mill __.test                     # 23 specs / ~600 tests today — must never regress
mill ui.fullLinkJS               # the SPA still links
mill ui-calculator.fullLinkJS    # the offline widget still links
```

From Phase 2 onward, additionally:

```bash
docker compose up -d postgres
mill app.run                          # Swagger UI at /docs, SPA at /
mill 'order-intake.02-infra.test'     # testcontainers-backed repository + endpoint tests
```

**End-to-end smoke, from Phase 5:** register → configure a product → add to basket → reload the page
(basket survives) → apply a discount code → place the order → see it in the customer portal → see it
in the manufacturing approval queue.
