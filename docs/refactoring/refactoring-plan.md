# Codebase Refactoring Plan

> **Version:** 1.0  
> **Date:** 2026-03-15  
> **Scope:** Domain decomposition, nested `.copy()` elimination, package restructuring, backend module preparation

---

## Table of Contents

1. [Current State Analysis](#1-current-state-analysis)
2. [Phase 1 — Nested `.copy()` Refactoring with Chimney & Extension Methods](#2-phase-1--nested-copy-refactoring-with-chimney--extension-methods)
3. [Phase 2 — Domain Bounded Context Decomposition](#3-phase-2--domain-bounded-context-decomposition)
4. [Phase 3 — Package Structure Improvements](#4-phase-3--package-structure-improvements)
5. [Phase 4 — Backend Module with DDD & Hexagonal Architecture](#5-phase-4--backend-module-with-ddd--hexagonal-architecture)
6. [Implementation Roadmap](#6-implementation-roadmap)

---

## 1. Current State Analysis

### 1.1 Module Structure

```
product-builder/
├── modules/
│   ├── domain/          # Cross-compiled (JVM + JS), pure functional core
│   ├── ui/              # Scala.js + Laminar SPA
│   ├── ui-framework/    # Reusable Laminar components (domain-independent)
│   └── ui-showcase/     # UI framework demo
├── build.sbt            # Scala 3.3.3, ZIO 2.1.16, Laminar 17.2.0
└── docs/
```

### 1.2 Domain Package Layout (Current — Flat)

```
mpbuilder.domain/
├── model/           # 16 files — ALL domain entities in one package
│   ├── ids.scala, basket.scala, catalog.scala, category.scala,
│   │   component.scala, configuration.scala, customer.scala,
│   │   discount.scala, finish.scala, language.scala, login.scala,
│   │   manufacturing.scala, material.scala, order.scala,
│   │   printingmethod.scala, specification.scala
├── service/         # 19 files — ALL services in one package
├── pricing/         # 10 files — pricing engine
├── rules/           # 3 files — compatibility rules
├── validation/      # 3 files — configuration validation
├── weight/          # 3 files — weight calculation
├── codec/           # 1 file — JSON codecs
└── sample/          # 6 files — sample/test data
```

### 1.3 Dependency Graph (Current)

```
                    ┌─────────────────┐
                    │   model/ (16)    │  ← foundation, no deps
                    └────────┬────────┘
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ pricing/ │  │  rules/  │  │ weight/  │
        │   (10)   │  │   (3)    │  │   (3)    │
        └────┬─────┘  └────┬─────┘  └──────────┘
             │              │
             ▼              ▼
        ┌──────────────────────┐
        │    validation/ (3)   │  ← depends on model + rules
        └──────────┬───────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │    service/ (19)     │  ← depends on model, pricing, rules, validation
        └──────────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │  codec/, sample/     │  ← depends on model, pricing, rules
        └──────────────────────┘
```

**Key observation:** The dependency direction is clean (no circular deps), but everything lives in a single `mpbuilder.domain` namespace, making it difficult to reason about bounded context boundaries.

### 1.4 Coupling Issues

| Issue | Details |
|-------|---------|
| **model/ is a monolith** | 16 files covering 10+ bounded contexts share one package. `basket.scala` imports `pricing.PriceBreakdown`; `customer.scala` imports `pricing.CustomerPricing`; `order.scala` imports `pricing.{Money, Currency}`. |
| **service/ is a monolith** | 19 files: `BasketService`, `LoginService`, `WorkflowEngine`, `AnalyticsService` — all unrelated concerns in one package. |
| **Cross-context model coupling** | `BasketItem` directly embeds `ProductConfiguration` and `PriceBreakdown`. `ManufacturingOrder` embeds `Order`. These should be references (IDs), not embedded objects. |
| **No anti-corruption layers** | No boundaries between contexts. Manufacturing directly consumes catalog/order models. |

### 1.5 Nested `.copy()` Inventory

Found **11 nested `.copy()` calls** across 4 files:

| File | Count | Pattern |
|------|-------|---------|
| `service/DiscountCodeService.scala` | 1 | `c.copy(constraints = c.constraints.copy(currentUses = ...))` |
| `ui/ProductBuilderViewModel.scala` | 5 | `state.copy(componentStates = ... + (role -> cs.copy(...)))`, `s.copy(checkoutInfo = Some(info.copy(...)))` |
| `ui/calendar/CalendarModel.scala` | 2 | `page.copy(template = page.template.copy(background = ...))` |
| `ui/calendar/CalendarViewModel.scala` | 3 | Same pattern as CalendarModel |

Additionally, there are **~298 total `.copy()` calls** across the codebase, with the heaviest files being ViewModels (50+ calls each in `ProductBuilderViewModel`, `CatalogEditorViewModel`, `ManufacturingViewModel`).

---

## 2. Phase 1 — Nested `.copy()` Refactoring with Chimney & Extension Methods

### 2.1 Strategy

We use a two-pronged approach:

1. **Chimney** (`io.scalaland.chimney`) — Added as a dependency to the domain module for type-safe transformations between case classes. Chimney excels at mapping between different types (e.g., domain → DTO, create-form → entity) and will be essential when the backend module introduces API DTOs, persistence models, and anti-corruption layer mappings.

2. **Extension methods / helper methods** — For same-type nested field updates (the `.copy()` patterns), we add focused helper methods directly on the case classes. This is the idiomatic Scala 3 approach for deep updates and produces the cleanest code.

### 2.2 Chimney Dependency

```scala
// build.sbt — added to domain module
"io.scalaland" %%% "chimney" % "1.6.0"
```

Chimney supports Scala 3.3+ and Scala.js with full feature parity.

### 2.3 Domain Refactoring — DiscountCode

**Before:**
```scala
// DiscountCodeService.scala:110
c.copy(constraints = c.constraints.copy(currentUses = c.constraints.currentUses + 1))
```

**After — Extension method on DiscountCode:**
```scala
// In discount.scala
extension (dc: DiscountCode)
  def withIncrementedUsage: DiscountCode =
    dc.copy(constraints = dc.constraints.copy(currentUses = dc.constraints.currentUses + 1))

// DiscountCodeService.scala:110
c.withIncrementedUsage
```

### 2.4 UI Refactoring — CalendarPage

**Before:**
```scala
// CalendarViewModel.scala:293
page.copy(template = page.template.copy(background = PageBackground.SolidColor(color)))
```

**After — Helper methods on CalendarPage:**
```scala
// In CalendarModel.scala
extension (page: CalendarPage)
  def withBackground(bg: PageBackground): CalendarPage =
    page.copy(template = page.template.copy(background = bg))
  def withTemplateType(tt: CalendarTemplateType): CalendarPage =
    page.copy(template = page.template.copy(templateType = tt))

// CalendarViewModel.scala:293
page.withBackground(PageBackground.SolidColor(color))
```

### 2.5 UI Refactoring — ProductBuilderViewModel

**Before:**
```scala
state.copy(componentStates = state.componentStates + (role -> cs.copy(selectedFinishes = newFinishes)))
```

**After — Extension methods on BuilderState:**
```scala
extension (state: BuilderState)
  def updateComponentState(role: ComponentRole)(f: ComponentState => ComponentState): BuilderState =
    val cs = state.componentStates.getOrElse(role, ComponentState(role))
    state.copy(componentStates = state.componentStates + (role -> f(cs)))

// Usage:
state.updateComponentState(role)(_.copy(selectedFinishes = newFinishes))
```

**Before:**
```scala
s.copy(checkoutInfo = Some(info.copy(step = nextStep)))
```

**After — Extension on BuilderState:**
```scala
extension (state: BuilderState)
  def updateCheckoutStep(step: CheckoutStep): BuilderState =
    state.checkoutInfo match
      case Some(info) => state.copy(checkoutInfo = Some(info.copy(step = step)))
      case None       => state

// Usage:
s.updateCheckoutStep(nextStep)
```

### 2.6 Where Chimney Shines (Future)

When the backend module is added, Chimney will be essential for:

```scala
import io.scalaland.chimney.dsl._

// Domain → API DTO
val dto = customer.into[CustomerDTO]
  .withFieldComputed(_.fullName, c => s"${c.contactInfo.firstName} ${c.contactInfo.lastName}")
  .withFieldRenamed(_.companyInfo, _.company)
  .transform

// API Request → Domain Command
val command = request.into[CreateCustomerCommand]
  .withFieldConst(_.id, CustomerId.unsafe(UUID.randomUUID().toString))
  .withFieldConst(_.createdAt, Instant.now())
  .transform

// Patching with partial updates
val updated = existing.patchUsing(updateRequest)
```

---

## 3. Phase 2 — Domain Bounded Context Decomposition

### 3.1 Identified Bounded Contexts

Based on domain analysis, we identify **7 bounded contexts**:

| Context | Current Files | Cohesion |
|---------|--------------|----------|
| **Catalog** | `catalog.scala`, `category.scala`, `component.scala`, `finish.scala`, `material.scala`, `printingmethod.scala`, `specification.scala` | High — all about product structure |
| **Pricing** | `pricing/*` (10 files) | High — already a separate package |
| **Configuration** | `configuration.scala`, `validation/*`, `rules/*` | High — building + validating configs |
| **Order** | `order.scala`, `basket.scala` | Medium — checkout & cart |
| **Customer** | `customer.scala`, `discount.scala`, `login.scala` | Medium — customer lifecycle |
| **Manufacturing** | `manufacturing.scala` + related services | High — production workflow |
| **Shared Kernel** | `ids.scala`, `language.scala`, `Money`, `Currency` | Stable — shared value objects |

### 3.2 Target Package Structure

```
mpbuilder.domain/
├── shared/                          # Shared Kernel
│   ├── model/
│   │   ├── ids.scala                # All ID types
│   │   ├── language.scala           # LocalizedString, Language
│   │   └── types.scala              # Money, Currency, Percentage
│   └── validation/
│       └── DomainValidation.scala   # Common validation helpers
│
├── catalog/                         # Catalog Bounded Context
│   ├── model/
│   │   ├── ProductCatalog.scala
│   │   ├── ProductCategory.scala
│   │   ├── Material.scala
│   │   ├── Finish.scala
│   │   ├── PrintingMethod.scala
│   │   ├── Component.scala
│   │   └── Specification.scala
│   ├── service/
│   │   └── CatalogQueryService.scala
│   └── rules/
│       ├── CompatibilityRule.scala
│       ├── CompatibilityRuleset.scala
│       └── predicates.scala
│
├── pricing/                         # Pricing Bounded Context
│   ├── model/
│   │   ├── Pricelist.scala
│   │   ├── PricingRule.scala
│   │   ├── PriceBreakdown.scala
│   │   ├── CustomerPricing.scala
│   │   └── ProductionCost.scala
│   ├── service/
│   │   ├── PriceCalculator.scala
│   │   ├── ProductionCostCalculator.scala
│   │   └── CustomerPricelistResolver.scala
│   └── error/
│       └── PricingError.scala
│
├── configuration/                   # Configuration Bounded Context
│   ├── model/
│   │   └── ProductConfiguration.scala
│   ├── service/
│   │   └── ConfigurationBuilder.scala
│   ├── validation/
│   │   ├── ConfigurationValidator.scala
│   │   └── RuleEvaluator.scala
│   └── error/
│       └── ConfigurationError.scala
│
├── customer/                        # Customer Bounded Context
│   ├── model/
│   │   ├── Customer.scala
│   │   ├── Login.scala
│   │   └── Discount.scala
│   ├── service/
│   │   ├── CustomerManagementService.scala
│   │   ├── LoginService.scala
│   │   ├── DiscountCodeService.scala
│   │   └── DiscountService.scala
│   └── error/
│       ├── CustomerManagementError.scala
│       ├── LoginError.scala
│       └── DiscountCodeError.scala
│
├── order/                           # Order Bounded Context
│   ├── model/
│   │   ├── Order.scala
│   │   └── Basket.scala
│   ├── service/
│   │   └── BasketService.scala
│   └── error/
│       └── BasketError.scala
│
├── manufacturing/                   # Manufacturing Bounded Context
│   ├── model/
│   │   ├── ManufacturingOrder.scala
│   │   ├── Workflow.scala
│   │   ├── Employee.scala
│   │   └── Machine.scala
│   ├── service/
│   │   ├── WorkflowEngine.scala
│   │   ├── WorkflowGenerator.scala
│   │   ├── EmployeeManagementService.scala
│   │   ├── MachineManagementService.scala
│   │   ├── AnalyticsService.scala
│   │   └── QueueScorer.scala
│   └── error/
│       ├── WorkflowError.scala
│       └── ManagementError.scala
│
└── codec/                           # Cross-context serialization
    └── DomainCodecs.scala
```

### 3.3 Context Boundaries & Communication

```
┌──────────────────────────────────────────────────────────┐
│                    Shared Kernel                          │
│  (IDs, LocalizedString, Money, Currency, Percentage)     │
└──────────────┬───────────────────────────────────────────┘
               │ imported by all contexts
    ┌──────────┴──────────┐
    ▼                     ▼
┌─────────┐        ┌──────────┐
│ Catalog │◄───────│ Pricing  │  pricing references catalog IDs
└────┬────┘        └────┬─────┘
     │                  │
     ▼                  ▼
┌──────────────┐  ┌──────────┐
│Configuration │  │ Customer │  customer has pricing overrides
└──────┬───────┘  └────┬─────┘
       │               │
       ▼               ▼
    ┌─────────────────────┐
    │       Order         │  order references configs + customer
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │   Manufacturing     │  manufacturing processes orders
    └─────────────────────┘
```

### 3.4 Migration Strategy

**Step 1 — Extract Shared Kernel** (low risk)
- Move `ids.scala`, `language.scala`, `Money`, `Currency`, `Percentage` to `shared/model/`
- Update imports across the codebase
- All existing tests should pass unchanged

**Step 2 — Extract Catalog context** (medium risk)
- Move catalog-related models and services
- CatalogQueryService moves with its rules dependency
- Update imports

**Step 3 — Extract remaining contexts one at a time**
- Each extraction is an independent, testable step
- Run full test suite after each extraction

### 3.5 Cross-Context References — ID-Based vs Embedded

Currently, several models embed full objects from other contexts:

| Model | Embedded Object | Proposed Change |
|-------|----------------|-----------------|
| `BasketItem.configuration` | Full `ProductConfiguration` | Keep as-is (same JVM process, no network boundary) |
| `BasketItem.priceBreakdown` | Full `PriceBreakdown` | Keep as-is (computed value, not a reference) |
| `ManufacturingOrder.order` | Full `Order` | Keep as-is for now; extract to `OrderId` reference when backend module introduces persistence |
| `Customer.pricing` | `CustomerPricing` | Keep as-is (owned by Customer context) |

**Rationale:** Since this is a single-process application (cross-compiled to JS), embedding is appropriate. When the backend module introduces persistence boundaries, we'll switch to ID-based references with repository lookups.

---

## 4. Phase 3 — Package Structure Improvements

### 4.1 Current Issues

1. **Flat model/ package** — 16 unrelated files in one directory
2. **Flat service/ package** — 19 files mixing error types with service implementations
3. **Error types co-located with services** — e.g., `BasketError.scala` sits next to `LoginService.scala`
4. **No clear feature boundaries** — You need to know the architecture to find things

### 4.2 Naming Conventions

| Current | Proposed | Rationale |
|---------|----------|-----------|
| `model/ids.scala` | `shared/model/ids.scala` | IDs are shared kernel |
| `service/BasketError.scala` | `order/error/BasketError.scala` | Errors with their context |
| `pricing/Money.scala` | `shared/model/types.scala` | Money is used everywhere |
| `service/WorkflowEngine.scala` | `manufacturing/service/WorkflowEngine.scala` | With its context |

### 4.3 UI Module Alignment

When the domain is decomposed, the UI module should mirror the structure:

```
mpbuilder.ui/
├── catalog/          # Already exists ✓
├── manufacturing/    # Already exists ✓
├── customers/        # Already exists ✓
├── calendar/         # Already exists ✓
├── order/            # Extract from components/ (CheckoutView, BasketView)
├── components/       # Truly shared UI components only
└── shared/           # Shared UI utilities
```

---

## 5. Phase 4 — Backend Module with DDD & Hexagonal Architecture

### 5.1 New Module: `backend`

```scala
// build.sbt addition
lazy val backend = (project in file("modules/backend"))
  .dependsOn(domainJVM)
  .settings(commonSettings)
  .settings(
    name := "material-builder-backend",
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"              % "2.1.16",
      "dev.zio"       %% "zio-http"         % "3.x.x",
      "dev.zio"       %% "zio-json"         % "0.7.3",
      "io.scalaland"  %% "chimney"          % "1.6.0",   // DTO transformations
      "io.getquill"   %% "quill-zio"        % "4.x.x",   // Persistence
    ),
  )
```

### 5.2 Hexagonal Architecture Layout

```
modules/backend/src/main/scala/mpbuilder/backend/
├── application/                    # Application Services (Use Cases)
│   ├── catalog/
│   │   ├── CreateCategoryUseCase.scala
│   │   └── QueryCatalogUseCase.scala
│   ├── order/
│   │   ├── PlaceOrderUseCase.scala
│   │   └── CheckoutUseCase.scala
│   ├── customer/
│   │   └── RegisterCustomerUseCase.scala
│   └── manufacturing/
│       └── StartProductionUseCase.scala
│
├── port/                           # Ports (interfaces)
│   ├── inbound/                    # Driving ports
│   │   ├── CatalogApi.scala        # trait CatalogApi
│   │   ├── OrderApi.scala
│   │   └── CustomerApi.scala
│   └── outbound/                   # Driven ports (SPI)
│       ├── CatalogRepository.scala     # trait CatalogRepository
│       ├── OrderRepository.scala
│       ├── CustomerRepository.scala
│       ├── PaymentGateway.scala
│       └── NotificationService.scala
│
├── adapter/                        # Adapters (implementations)
│   ├── inbound/
│   │   └── http/                   # HTTP/REST adapters
│   │       ├── CatalogRoutes.scala
│   │       ├── OrderRoutes.scala
│   │       ├── CustomerRoutes.scala
│   │       └── dto/               # API DTOs
│   │           ├── CatalogDTO.scala
│   │           ├── OrderDTO.scala
│   │           └── CustomerDTO.scala
│   └── outbound/
│       ├── persistence/            # Database adapters
│       │   ├── PostgresCatalogRepository.scala
│       │   ├── PostgresOrderRepository.scala
│       │   └── schema/            # DB schema models
│       │       └── Tables.scala
│       ├── payment/
│       │   └── StripePaymentGateway.scala
│       └── notification/
│           └── EmailNotificationService.scala
│
└── config/                         # Application configuration
    ├── AppConfig.scala
    └── Main.scala                  # ZIO App entry point
```

### 5.3 Chimney for Anti-Corruption Layer

Chimney becomes critical in the backend module for mapping between layers:

```scala
// adapter/inbound/http/dto/CustomerDTO.scala
final case class CreateCustomerRequest(
  firstName: String,
  lastName: String,
  email: String,
  companyName: Option[String],
)

// In the HTTP adapter — DTO → Domain transformation
import io.scalaland.chimney.dsl._

val customer = request
  .into[Customer]
  .withFieldConst(_.id, CustomerId.unsafe(UUID.randomUUID().toString))
  .withFieldConst(_.status, CustomerStatus.PendingApproval)
  .withFieldConst(_.tier, CustomerTier.Standard)
  .withFieldComputed(_.contactInfo, r => ContactInfo(r.firstName, r.lastName, r.email, ...))
  .transform

// Domain → Persistence model
val row = customer
  .into[CustomerRow]
  .withFieldComputed(_.fullName, c => s"${c.contactInfo.firstName} ${c.contactInfo.lastName}")
  .withFieldConst(_.updatedAt, Instant.now())
  .transform

// Domain → API Response
val response = customer.transformInto[CustomerResponse] // auto-derived if field names match
```

### 5.4 Port Interfaces (ZIO-based)

```scala
// port/outbound/CatalogRepository.scala
trait CatalogRepository:
  def findCategory(id: CategoryId): ZIO[Any, RepositoryError, Option[ProductCategory]]
  def listCategories: ZIO[Any, RepositoryError, List[ProductCategory]]
  def save(category: ProductCategory): ZIO[Any, RepositoryError, Unit]

// port/outbound/OrderRepository.scala
trait OrderRepository:
  def findById(id: OrderId): ZIO[Any, RepositoryError, Option[Order]]
  def save(order: Order): ZIO[Any, RepositoryError, Unit]
  def findByCustomer(customerId: CustomerId): ZIO[Any, RepositoryError, List[Order]]
```

### 5.5 Use Case Pattern

```scala
// application/order/PlaceOrderUseCase.scala
final case class PlaceOrderUseCase(
  orderRepo: OrderRepository,
  basketService: BasketService,        // from domain module
  priceCalculator: PriceCalculator,    // from domain module
  paymentGateway: PaymentGateway,
  notificationService: NotificationService,
):
  def execute(command: PlaceOrderCommand): ZIO[Any, OrderError, Order] =
    for
      basket     <- ZIO.fromEither(basketService.validate(command.basket))
      breakdown  <- ZIO.fromEither(priceCalculator.calculate(...))
      order      <- ZIO.succeed(Order(...))
      _          <- orderRepo.save(order)
      _          <- paymentGateway.initiate(order)
      _          <- notificationService.sendOrderConfirmation(order)
    yield order
```

### 5.6 Dependency Rule

```
          ┌─────────────────────────────────────┐
          │          Domain Module               │
          │  (pure, no ZIO effects, cross-JS)    │
          └──────────────┬──────────────────────┘
                         │ depends on
          ┌──────────────▼──────────────────────┐
          │      Application Layer               │
          │  (use cases, orchestration)           │
          │  Uses ZIO effects                    │
          └──────────────┬──────────────────────┘
                         │ depends on (ports)
          ┌──────────────▼──────────────────────┐
          │         Port Layer                   │
          │  (traits/interfaces only)            │
          └──────────────┬──────────────────────┘
                         │ implemented by
          ┌──────────────▼──────────────────────┐
          │        Adapter Layer                 │
          │  (HTTP, DB, external services)       │
          │  Chimney maps between layers         │
          └─────────────────────────────────────┘
```

**Key principle:** The domain module remains effect-free and cross-compiles to Scala.js. ZIO effects are introduced only in the backend module's application and adapter layers.

---

## 6. Implementation Roadmap

### Priority & Effort

| Phase | Priority | Effort | Risk | Dependency |
|-------|----------|--------|------|------------|
| **Phase 1a**: Add Chimney dependency | 🟢 High | XS | Low | None |
| **Phase 1b**: Refactor nested `.copy()` | 🟢 High | S | Low | 1a |
| **Phase 2a**: Extract Shared Kernel | 🟡 Medium | S | Low | None |
| **Phase 2b**: Extract Catalog context | 🟡 Medium | M | Medium | 2a |
| **Phase 2c**: Extract remaining contexts | 🟡 Medium | L | Medium | 2b |
| **Phase 3**: Package structure cleanup | 🟡 Medium | M | Low | 2c |
| **Phase 4a**: Backend module scaffold | 🔵 Low | M | Low | None |
| **Phase 4b**: Port & adapter impl | 🔵 Low | XL | Medium | 4a, 2c |

### Suggested Order

```
Phase 1a+1b (immediate) ──► Phase 2a (next sprint) ──► Phase 2b-2c (iterative)
                                                              │
Phase 4a (can start in parallel) ────────────────────────────►│
                                                              ▼
                                                    Phase 3 + Phase 4b
```

### Success Criteria

- [ ] All 483+ existing tests pass after each phase
- [ ] No new dependencies beyond Chimney (Phase 1) and backend stack (Phase 4)
- [ ] Each bounded context can be understood independently
- [ ] Backend module compiles and has basic integration tests
- [ ] Chimney transformations are used for all cross-layer mappings in backend
