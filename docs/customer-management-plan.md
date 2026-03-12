# Customer Management, Pricing & Login — Analysis & Implementation Plan

This document covers the analysis and phased implementation plan for customer management, customer-specific pricing, agency login, and discount code management.

---

## Current State

### What exists today

| Area | Current State |
|------|--------------|
| **Customer model** | `CheckoutInfo` has `customerType: CustomerType` enum (`Guest`, `Registered`, `RegisteredCorporate`) — used only during checkout, not persisted |
| **Login** | Checkout Step 1 (Authentication) has a Guest vs. Registered toggle with email/password fields — purely UI placeholders, no real auth |
| **Discount codes** | `DiscountService` is a static `Map[String, Int]` of 11 hardcoded codes (e.g., `"SAVE5" → 5`) — no expiration, constraints, or CRUD |
| **Pricing** | `Pricelist` is a flat `List[PricingRule]` with currency/version — same prices for all customers |
| **Order history** | No persistence layer — orders exist only in `Var[List[ManufacturingOrder]]` during a session |
| **Manufacturing UI** | 7 views (Dashboard, StationQueue, OrderApproval, OrderProgress, Employees, Machines, Analytics) — no customer-related views |

### Key architectural constraints

- **No backend** — all state is client-side (`Var`/`Signal`). Persistence is limited to `localStorage`.
- **Pure domain** — domain logic returns `Validation[E, A]`, never effects. Must stay Scala.js-compatible.
- **Bilingual** — all entities, errors, and UI use `LocalizedString` (English + Czech).
- **Rules as data** — pricing and compatibility use sealed ADTs interpreted by engines.

---

## Feature Analysis

### 1. Customer Model

A `Customer` entity that represents both agency (B2B) and future regular (B2C) customers.

```
Customer
├── id: CustomerId
├── customerType: CustomerType (Agency | Regular)  // reuse/extend existing enum
├── status: CustomerStatus (Active | Inactive | Suspended | PendingApproval)
├── tier: CustomerTier (Standard | Silver | Gold | Platinum)
├── companyInfo: Option[CompanyInfo]
│   ├── companyName: String
│   ├── businessId: String (IČO)
│   ├── vatId: Option[String] (DIČ)
│   └── contactPerson: String
├── contactInfo: ContactInfo
│   ├── email: String
│   ├── phone: Option[String]
│   └── address: Address
├── pricing: CustomerPricing
├── internalNotes: List[CustomerNote]
├── createdAt: Instant
├── lastOrderAt: Option[Instant]
└── tags: Set[String]
```

**CustomerType** — Extend the existing `CustomerType` enum:
- `Guest` — anonymous checkout (existing)
- `Agency` — B2B customer with login, custom pricing (new, primary scope)
- `Regular` — B2C registered customer (future, out of scope)

**CustomerTier** — Drives default discount levels and can be used in pricing rules:
- `Standard` — no automatic discount
- `Silver` — e.g., 5% default discount
- `Gold` — e.g., 10% default discount
- `Platinum` — e.g., 15% default discount

Tiers are informational/defaults — actual pricing is configured explicitly per customer.

### 2. Customer-Specific Pricing

The core requirement: agency customers see discounted prices already in the product configurator, not just at checkout.

**Approach: `CustomerPricing` as a composable overlay on top of the base `Pricelist`**

```
CustomerPricing
├── globalDiscount: Option[Percentage]          // e.g., 10% off everything
├── categoryDiscounts: Map[CategoryId, Percentage]  // per-category override
├── materialDiscounts: Map[MaterialId, Percentage]  // per-material override
├── fixedMaterialPrices: Map[MaterialId, Money]     // fixed price override (per unit/m²)
├── finishDiscounts: Map[FinishId, Percentage]       // per-finish override
├── customQuantityTiers: Option[List[QuantityTier]]  // custom volume breaks
└── minimumOrderOverride: Option[Money]             // custom minimum order
```

**Discount resolution precedence** (most specific wins):
1. Fixed price on material → replaces base material price entirely
2. Material-level percentage → applied to base material price
3. Category-level percentage → applied to subtotal for that category
4. Global percentage → applied to remaining undiscounted components

This is intentionally simple — no need for a full pricing rule engine per customer. The customer pricing is an **overlay** applied after the base `Pricelist` calculation.

**Percentage** — opaque type over `BigDecimal`, range 0–100, with `applyTo(money: Money): Money` method.

#### Production cost protection

A separate concept: `ProductionCost` rules that define the floor price below which we should not sell.

```
ProductionCostRule
├── MaterialCost(materialId, costPerUnit)     // raw material cost
├── MaterialAreaCost(materialId, costPerM2)   // area-based material cost
├── ProcessCost(processType, costPerUnit)     // printing process cost
├── FinishCost(finishId, costPerUnit)         // finish processing cost
└── OverheadFactor(factor: BigDecimal)        // multiplier for overhead (e.g., 1.15 = 15% overhead)
```

The `ProductionCostCalculator` would compute a floor price from configuration, similar to `PriceCalculator` but simpler. When the customer-discounted price falls below this floor:
- **Warning** (not a hard block) — the operator creating the pricing sees a clear warning
- **Analytics** — orders sold below cost are flagged for reporting

This keeps the system flexible (operators can override for strategic accounts) while providing visibility.

### 3. Discount Code Management

Replace the hardcoded `DiscountService` with a proper model:

```
DiscountCode
├── id: DiscountCodeId
├── code: String (unique, case-insensitive)
├── discountType: DiscountType
│   ├── Percentage(value: BigDecimal)        // e.g., 10% off
│   ├── FixedAmount(value: Money)            // e.g., 50 CZK off
│   └── FreeDelivery                         // waives delivery cost
├── constraints: DiscountConstraints
│   ├── validFrom: Option[Instant]
│   ├── validUntil: Option[Instant]
│   ├── maxUses: Option[Int]
│   ├── currentUses: Int
│   ├── minimumOrderValue: Option[Money]
│   ├── allowedCategories: Set[CategoryId]   // empty = all
│   ├── allowedCustomerTypes: Set[CustomerType] // empty = all
│   └── allowedCustomerIds: Set[CustomerId]  // empty = all (for exclusive codes)
├── isActive: Boolean
├── createdBy: Option[EmployeeId]
└── createdAt: Instant
```

**DiscountCodeError** ADT:
- `CodeNotFound` — code doesn't exist
- `CodeExpired` — past `validUntil`
- `CodeNotYetValid` — before `validFrom`
- `CodeExhausted` — `currentUses >= maxUses`
- `CodeInactive` — manually deactivated
- `BelowMinimumOrder` — order value too low
- `CategoryNotEligible` — product category not in allowed set
- `CustomerNotEligible` — customer type/ID not allowed

**DiscountCodeService** — pure functions:
- `validate(code, orderValue, categoryIds, customer): Validation[DiscountCodeError, DiscountCode]`
- `apply(code, subtotal): Validation[DiscountCodeError, Money]` — returns discounted total
- `create/update/deactivate` — CRUD for operator management

### 4. Agency Login Mechanism

Lightweight OTP (one-time password) flow — no traditional username/password:

```
LoginSession
├── customerId: CustomerId
├── sessionToken: String
├── createdAt: Instant
├── expiresAt: Instant

OtpRequest
├── identifier: String            // Business ID, VAT, or email
├── identifierType: IdentifierType (BusinessId | VatId | Email)

OtpToken
├── token: String (6-digit numeric)
├── customerId: CustomerId
├── createdAt: Instant
├── expiresAt: Instant (e.g., +10 minutes)
├── isUsed: Boolean
```

**Flow:**
1. Customer enters Business ID, VAT number, or email on login screen
2. System looks up the agency customer by identifier
3. System generates a 6-digit OTP and "sends" it (in our no-backend world: displays it, or stores it for simulation)
4. Customer enters OTP → session created → customer pricing loaded
5. Session stored in `localStorage` with expiration

**Domain-side** — `LoginService` (pure):
- `lookupCustomer(identifier, identifierType, customers): Validation[LoginError, Customer]`
- `validateOtp(token, otpToken): Validation[LoginError, LoginSession]`

**LoginError** ADT: `CustomerNotFound`, `OtpExpired`, `OtpInvalid`, `CustomerInactive`, `CustomerSuspended`

Since there is no backend, the OTP simulation will generate the token client-side and display it (or auto-fill it) for demo purposes. The domain model is designed to be backend-ready when that layer is added.

### 5. Customer Management (Manufacturing UI)

New views within the manufacturing section for internal operator use:

**Customer List View** — `SplitTableView[Customer]` with:
- Columns: Company Name, Business ID, Tier, Status, Last Order, Actions
- Filters: by tier, status, customer type
- Search: by company name, business ID, email
- Side panel: full customer details, notes, pricing summary

**Customer Detail / Edit View** — within side panel or expanded:
- Company info editing
- Contact info editing
- Tier selection
- Status management (activate/suspend/deactivate)
- Internal notes (timestamped, with author)
- Tags management

**Customer Pricing View** — dedicated pricing configuration:
- Global discount percentage slider/input
- Category discount table (category → percentage)
- Material discount table (material → percentage or fixed price)
- Finish discount table (finish → percentage)
- Custom quantity tiers editor
- **Production cost comparison** — side-by-side showing: base price, customer price, production cost, margin. Warnings highlighted when margin is negative or below threshold.

**Discount Code Management View** — `SplitTableView[DiscountCode]` with:
- Columns: Code, Type, Discount, Valid From/Until, Uses, Status, Actions
- Create/Edit form: code, type, value, constraints
- Activate/deactivate toggle
- Usage statistics

### 6. Customer-Aware Product Builder

When an agency customer is logged in:

- **Price preview** shows customer-specific prices with comparison to base price (strikethrough original, show discounted)
- **Basket totals** reflect customer pricing
- **Checkout** skips authentication step, pre-fills company/contact info
- **Login widget** in header area — shows company name when logged in, login button when not

### 7. Order History (Future-Ready)

Minimal initial scope — agency customers can see past orders:

```
OrderHistory
├── orders: List[OrderSummary]

OrderSummary
├── orderId: OrderId
├── orderDate: Instant
├── items: List[OrderItemSummary]
├── total: Money
├── status: OrderHistoryStatus (Placed | InProduction | Completed | Dispatched)
├── trackingNumber: Option[String]
```

Since there's no backend, this would be seeded from `ManufacturingOrder` data linked by customer. The model is designed so a real backend can populate it later.

---

## Implementation Phases

### Phase 1 — Customer Domain Model & Management Service

**Domain scope:** Core customer entity and CRUD service.

**Model (`model/customer.scala`):**
- `CustomerId` opaque type (in `ids.scala`)
- `Customer` case class with all fields listed above
- `CustomerStatus` enum: `Active`, `Inactive`, `Suspended`, `PendingApproval`
- `CustomerTier` enum: `Standard`, `Silver`, `Gold`, `Platinum`
- `CompanyInfo`, `ContactInfo`, `Address` case classes (reuse/align with `CheckoutInfo` address fields)
- `CustomerNote` case class: `text`, `createdAt`, `createdBy`
- `LocalizedString` for display names on enums

**Service (`service/CustomerManagementService.scala`):**
- Pure CRUD returning `Validation[CustomerManagementError, List[Customer]]`
- `addCustomer`, `updateCustomer`, `updateStatus`, `updateTier`, `addNote`, `removeCustomer`
- Validation: required fields, unique business ID, unique email

**Error (`service/CustomerManagementError.scala`):**
- ADT: `DuplicateBusinessId`, `DuplicateEmail`, `CustomerNotFound`, `InvalidStatus`, `MissingRequiredField`
- Bilingual `message(lang)` method

**Sample data (`sample/SampleCustomers.scala`):**
- 3-5 sample agency customers with varied tiers and statuses

**Tests (`CustomerManagementServiceSpec`):**
- CRUD operations, validation, status transitions, duplicate detection

**Estimated: ~15-20 tests**

---

### Phase 2 — Customer-Specific Pricing Model & Engine

**Domain scope:** Pricing overlay model, integration with `PriceCalculator`.

**Model (`pricing/CustomerPricing.scala`):**
- `Percentage` opaque type over `BigDecimal` (0–100 range)
- `CustomerPricing` case class with discount fields
- `CustomerPricingRule` sealed ADT:
  - `GlobalPercentageDiscount(percentage: Percentage)`
  - `CategoryPercentageDiscount(categoryId: CategoryId, percentage: Percentage)`
  - `MaterialPercentageDiscount(materialId: MaterialId, percentage: Percentage)`
  - `MaterialFixedPrice(materialId: MaterialId, price: Money)`
  - `FinishPercentageDiscount(finishId: FinishId, percentage: Percentage)`

**Engine (`pricing/CustomerPriceCalculator.scala`):**
- `calculate(config, pricelist, customerPricing): Validation[PricingError, PriceBreakdown]`
- Delegates to existing `PriceCalculator`, then applies customer overlay
- Two approaches considered:
  - **Option A: Post-calculation overlay** — calculate base price, then apply discounts to the breakdown. Simpler, but less precise for fixed material prices.
  - **Option B: Modified pricelist** — generate a customer-specific `Pricelist` by cloning base rules with adjusted prices, then feed into existing `PriceCalculator`. More precise, reuses existing engine.
  - **Recommended: Option B** — create a `CustomerPricelistResolver` that takes base `Pricelist` + `CustomerPricing` and produces an adjusted `Pricelist`. This way the existing `PriceCalculator` handles everything, and the breakdown naturally shows customer prices. The original base price can be calculated separately for comparison display.

**`CustomerPricelistResolver`:**
- `resolve(basePricelist, customerPricing): Pricelist`
- For `MaterialFixedPrice` → replace/override `MaterialBasePrice`/`MaterialAreaPrice` rules
- For percentage discounts → adjust the `Money` amounts in matching rules
- For `globalDiscount` → apply to all material and finish price rules
- Precedence: fixed > material-specific % > category % > global %

**Extension to `PriceBreakdown`:**
- Add `customerDiscount: Option[Money]` — total discount vs. base price
- Add `basePriceTotal: Option[Money]` — what the base price would have been (for comparison display)

**Tests (`CustomerPriceCalculatorSpec`):**
- Global discount applied correctly
- Material-specific discount overrides global
- Fixed material price replaces base
- Category discount
- Finish discount
- Precedence chain
- Combined discounts
- No discount (empty CustomerPricing)
- Price comparison (base vs. customer)

**Estimated: ~15-20 tests**

---

### Phase 3 — Production Cost Model & Warnings

**Domain scope:** Production cost floor calculation and margin analysis.

**Model (`pricing/ProductionCost.scala`):**
- `ProductionCostRule` sealed ADT:
  - `MaterialUnitCost(materialId: MaterialId, cost: Money)`
  - `MaterialAreaCost(materialId: MaterialId, costPerM2: Money)`
  - `ProcessCost(processType: ProcessType, costPerUnit: Money)`
  - `FinishCost(finishId: FinishId, costPerUnit: Money)`
  - `OverheadFactor(factor: BigDecimal)` — multiplier on total cost (e.g., 1.15)
- `ProductionCostSheet` — `List[ProductionCostRule]` (analogous to `Pricelist`)
- `CostAnalysis` — result type:
  - `productionCost: Money`
  - `sellingPrice: Money`
  - `margin: Money`
  - `marginPercentage: Percentage`
  - `isBelowCost: Boolean`
  - `warnings: List[CostWarning]`
- `CostWarning` ADT: `BelowProductionCost(shortfall: Money)`, `LowMargin(marginPct: Percentage, threshold: Percentage)`

**Service (`pricing/ProductionCostCalculator.scala`):**
- `calculateCost(config, costSheet): Validation[PricingError, Money]`
- `analyze(config, pricelist, customerPricing, costSheet): Validation[PricingError, CostAnalysis]`

**Sample data (`sample/SampleProductionCosts.scala`):**
- Cost rules for sample materials and finishes

**Tests (`ProductionCostSpec`):**
- Cost calculation for various configurations
- Margin analysis with and without customer discounts
- Below-cost detection
- Low-margin warnings

**Estimated: ~10-12 tests**

---

### Phase 4 — Discount Code Domain Model & Service

**Domain scope:** Replace hardcoded discount codes with proper model.

**Model (`model/discount.scala`):**
- `DiscountCodeId` opaque type
- `DiscountCode` case class (as described in analysis)
- `DiscountType` enum: `Percentage`, `FixedAmount`, `FreeDelivery`
- `DiscountConstraints` case class

**Service (`service/DiscountCodeService.scala`):**
- Replaces existing `DiscountService`
- `validate(code, context): Validation[DiscountCodeError, DiscountCode]`
- `applyDiscount(code, subtotal): Validation[DiscountCodeError, DiscountResult]`
- `DiscountResult`: `originalTotal`, `discountAmount`, `finalTotal`, `appliedCode`
- CRUD: `createCode`, `updateCode`, `deactivateCode`, `incrementUsage`

**Error (`service/DiscountCodeError.scala`):**
- Full ADT as described in analysis
- Bilingual messages

**Backward compatibility:**
- `DiscountService` can be deprecated and `DiscountCodeService` used in its place
- Migrate hardcoded codes to `SampleDiscountCodes`

**Tests (`DiscountCodeServiceSpec`):**
- Code validation (found, not found, expired, exhausted, inactive)
- Constraint checking (min order, category, customer type/ID)
- Discount application (percentage, fixed, free delivery)
- CRUD operations
- Case-insensitive lookup

**Estimated: ~18-22 tests**

---

### Phase 5 — Agency Login Domain Model

**Domain scope:** OTP-based login for agency customers.

**Model (`model/login.scala`):**
- `SessionId` opaque type
- `LoginSession` case class
- `OtpRequest`, `OtpToken` case classes
- `IdentifierType` enum: `BusinessId`, `VatId`, `Email`

**Service (`service/LoginService.scala`):**
- `lookupCustomer(identifier, identifierType, customers): Validation[LoginError, Customer]`
- `generateOtp(customer): OtpToken` (pure — generates deterministic token from customer ID + timestamp for testability, real randomness injected at UI layer)
- `validateOtp(inputToken, otpToken): Validation[LoginError, LoginSession]`
- `isSessionValid(session, now): Boolean`

**Error (`service/LoginError.scala`):**
- `CustomerNotFound`, `OtpExpired`, `OtpInvalid`, `CustomerInactive`, `CustomerSuspended`, `SessionExpired`
- Bilingual messages

**Tests (`LoginServiceSpec`):**
- Customer lookup by business ID, VAT, email
- Customer not found
- Inactive/suspended customer rejection
- OTP validation (valid, expired, wrong token)
- Session validity check

**Estimated: ~12-15 tests**

---

### Phase 6 — Customer Management UI (Manufacturing)

**UI scope:** Internal views for managing customers and their pricing within the manufacturing section.

**New manufacturing routes:**
- `Customers` (👤) — customer list and management
- `DiscountCodes` (🏷️) — discount code management

**ManufacturingViewModel extensions:**
- `customers: Var[List[Customer]]` — initialized from `SampleCustomers`
- `discountCodes: Var[List[DiscountCode]]` — initialized from `SampleDiscountCodes`
- Customer CRUD actions: `addCustomer`, `updateCustomer`, `updateCustomerStatus`, `updateCustomerTier`, `addCustomerNote`
- Customer pricing actions: `updateCustomerPricing`, `setGlobalDiscount`, `setCategoryDiscount`, `setMaterialDiscount`, `setFixedMaterialPrice`
- Discount code actions: `createDiscountCode`, `updateDiscountCode`, `toggleDiscountCodeActive`

**Views:**

**`CustomersView.scala`** — `SplitTableView[Customer]`:
- Table columns: Company Name, Business ID/VAT, Tier (badge), Status (badge), Email, Last Order Date
- Filter chips: by tier, by status
- Search: company name, business ID, email
- Row actions: Edit, Suspend/Activate
- Side panel tabs:
  - **Details** — company info, contact info, editable fields
  - **Pricing** — customer pricing configuration with:
    - Global discount input
    - Category discounts table (category selector + percentage)
    - Material discounts table (material selector + percentage or fixed price toggle)
    - Finish discounts table
    - **Cost analysis section** — for each configured discount, show base price vs. customer price vs. production cost for a sample configuration, with warnings
  - **Notes** — timestamped internal notes, add new note form
  - **Orders** — list of orders placed by this customer (from `ManufacturingOrder` data)

**`DiscountCodesView.scala`** — `SplitTableView[DiscountCode]`:
- Table columns: Code, Type, Value, Valid Period, Uses (current/max), Status, Actions
- Filter chips: by type, by status (active/expired/exhausted)
- Row actions: Edit, Activate/Deactivate
- Side panel:
  - Code and discount type/value editing
  - Constraint configuration (validity dates, max uses, min order, allowed categories/customers)
  - Usage statistics

**Estimated: ~600-800 lines across views**

---

### Phase 7 — Customer-Aware Product Builder UI

**UI scope:** Login widget, customer pricing integration in configurator.

**Login widget (`components/LoginWidget.scala`):**
- Displayed in the product builder header area
- States: logged out → entering identifier → entering OTP → logged in
- Logged-in state shows: company name, tier badge, logout button
- Login state persisted in `localStorage`

**ProductBuilderViewModel extensions:**
- `currentCustomer: Var[Option[Customer]]` — the logged-in agency customer
- `customerPricelist: Signal[Option[Pricelist]]` — derived from base pricelist + customer pricing via `CustomerPricelistResolver`
- Modify `validateConfiguration` and price calculation to use customer pricelist when available
- `loginCustomer(session)`, `logoutCustomer()` actions

**PricePreview updates:**
- When customer is logged in, show two-line pricing:
  - ~~Base price~~ (strikethrough) — what it would cost without customer discount
  - **Your price** (highlighted) — customer-discounted price
  - **You save: X CZK (Y%)** — savings summary
- When not logged in, show standard pricing

**CheckoutView updates:**
- Skip Authentication step when customer is already logged in
- Pre-fill company info and contact details from `Customer` data
- Show customer tier and applicable discounts in summary

**BasketView updates:**
- Show per-item customer pricing with savings
- Basket total reflects customer prices

**Estimated: ~400-500 lines of UI changes**

---

### Phase 8 — Order History View (Customer-Facing)

**UI scope:** Agency customers can see past orders from the product builder.

**New route in `AppRouter`:**
- Add `OrderHistory` route accessible when logged in

**View (`components/OrderHistoryView.scala`):**
- Table of past orders: date, items summary, total, status
- Expandable rows showing order items with configurations
- Status tracking (Placed → In Production → Completed → Dispatched)
- Tracking number display when available
- **Reorder** button — loads configuration back into the builder for repeat orders

**Data source:**
- Derive from `ManufacturingOrder` list filtered by customer ID
- Map manufacturing statuses to customer-facing `OrderHistoryStatus`

**Estimated: ~300-400 lines**

---

## Phase Summary & Dependencies

```
Phase 1: Customer Domain Model ──────────────────────┐
                                                      │
Phase 2: Customer Pricing Engine ◄────────────────────┤
                                                      │
Phase 3: Production Cost Model ◄──────────────────────┤
                                                      │
Phase 4: Discount Code Model ────────────────────┐    │
                                                  │    │
Phase 5: Agency Login Model ◄─────────────────┐  │    │
                                               │  │    │
Phase 6: Customer Management UI ◄──────────────┼──┼────┘
          (Manufacturing)                      │  │
                                               │  │
Phase 7: Customer-Aware Product Builder ◄──────┘  │
          (Login + Pricing in Configurator)        │
                                                   │
Phase 8: Order History View ◄──────────────────────┘
```

| Phase | Domain | UI | Tests | Dependencies |
|-------|--------|-----|-------|-------------|
| 1 — Customer Model | `customer.scala`, `CustomerManagementService` | — | ~15-20 | None |
| 2 — Customer Pricing | `CustomerPricing`, `CustomerPricelistResolver` | — | ~15-20 | Phase 1 |
| 3 — Production Cost | `ProductionCost`, `ProductionCostCalculator` | — | ~10-12 | Phase 2 |
| 4 — Discount Codes | `DiscountCode`, `DiscountCodeService` | — | ~18-22 | None (parallel with 1-3) |
| 5 — Login Model | `LoginSession`, `LoginService` | — | ~12-15 | Phase 1 |
| 6 — Customer Mgmt UI | — | `CustomersView`, `DiscountCodesView` | — | Phases 1-5 |
| 7 — Builder Integration | — | `LoginWidget`, PricePreview, Checkout | — | Phases 5, 6 |
| 8 — Order History | — | `OrderHistoryView` | — | Phase 7 |

**Total estimated new tests: ~70-90**

---

## Detailed Domain Model Diagram

```
mpbuilder.domain
├── model/
│   ├── customer.scala          [NEW — Phase 1]
│   │   ├── Customer
│   │   ├── CustomerStatus (Active/Inactive/Suspended/PendingApproval)
│   │   ├── CustomerTier (Standard/Silver/Gold/Platinum)
│   │   ├── CompanyInfo
│   │   ├── CustomerNote
│   │   └── ContactInfo / Address
│   ├── discount.scala          [NEW — Phase 4]
│   │   ├── DiscountCode
│   │   ├── DiscountType (Percentage/FixedAmount/FreeDelivery)
│   │   └── DiscountConstraints
│   ├── login.scala             [NEW — Phase 5]
│   │   ├── LoginSession
│   │   ├── OtpToken
│   │   └── IdentifierType
│   └── ids.scala               [EXTEND]
│       ├── CustomerId           [NEW]
│       ├── DiscountCodeId       [NEW]
│       └── SessionId            [NEW]
├── pricing/
│   ├── CustomerPricing.scala    [NEW — Phase 2]
│   │   ├── Percentage (opaque type)
│   │   ├── CustomerPricing
│   │   └── CustomerPricingRule (sealed ADT)
│   ├── CustomerPricelistResolver.scala  [NEW — Phase 2]
│   └── ProductionCost.scala     [NEW — Phase 3]
│       ├── ProductionCostRule (sealed ADT)
│       ├── ProductionCostSheet
│       ├── CostAnalysis
│       └── ProductionCostCalculator
├── service/
│   ├── CustomerManagementService.scala    [NEW — Phase 1]
│   ├── CustomerManagementError.scala      [NEW — Phase 1]
│   ├── DiscountCodeService.scala          [NEW — Phase 4]
│   ├── DiscountCodeError.scala            [NEW — Phase 4]
│   ├── LoginService.scala                 [NEW — Phase 5]
│   ├── LoginError.scala                   [NEW — Phase 5]
│   └── DiscountService.scala              [DEPRECATE — Phase 4]
└── sample/
    ├── SampleCustomers.scala       [NEW — Phase 1]
    ├── SampleProductionCosts.scala [NEW — Phase 3]
    └── SampleDiscountCodes.scala   [NEW — Phase 4]
```

---

## Design Decisions & Rationale

### Why Option B (modified pricelist) for customer pricing?

Generating a customer-specific `Pricelist` via `CustomerPricelistResolver` means:
- The existing `PriceCalculator` handles all calculation — no parallel pricing engine
- `PriceBreakdown` naturally shows customer prices in line items
- Adding new `PricingRule` types automatically works with customer pricing
- Base price comparison is a second `PriceCalculator.calculate` call with the original pricelist

The alternative (post-calculation overlay) would require duplicating breakdown logic and risks inconsistencies between the two approaches.

### Why separate `ProductionCostRule` from `PricingRule`?

Production costs and selling prices are fundamentally different concerns:
- Pricing rules evolve with market strategy; cost rules evolve with supplier contracts
- Different people manage them (sales vs. operations)
- Cost rules are simpler (no tiers, no setup fees, no minimum orders)
- Mixing them in one ADT would complicate both

### Why OTP instead of password for agency login?

- **No password management** — no reset flows, no hashing, no breach risk
- **Simpler domain model** — no credential storage
- **Agency use case fits** — infrequent logins, known email addresses
- **Backend-ready** — when email sending is added, the model works as-is

### Why tiers are informational, not pricing-driven?

Tiers provide a starting point and visual indicator, but actual pricing is explicitly configured per customer. This avoids:
- Surprise price changes when tier changes
- Complex tier→pricing inheritance rules
- Operators not understanding why a price is what it is

### Discount codes vs. customer pricing — when to use which?

| | Customer Pricing | Discount Codes |
|---|---|---|
| **Who** | Specific agency customer | Anyone with the code |
| **Duration** | Permanent (until changed) | Time-limited |
| **Scope** | All orders by that customer | Single order |
| **Managed by** | Account manager | Marketing/sales |
| **Stacks with** | Base pricelist | Customer pricing (applied after) |

Both can coexist: an agency customer gets their negotiated prices, and can also use a promotional discount code on top.

---

## Open Questions

1. **Discount code stacking** — Should discount codes apply on top of customer pricing (compound), or only on the base price? Recommendation: on top of customer price (compound), since they serve different purposes.

2. **Customer pricing versioning** — Should we keep history of pricing changes? For now, no — the current pricing is the only pricing. History can be added later with timestamps.

3. **Multi-currency customer pricing** — Should customer fixed prices be currency-specific? Yes — a `MaterialFixedPrice` should include currency, matching the pricelist currency.

4. **Order history data seeding** — How many sample orders to create for demo purposes? Recommendation: 5-8 orders per sample customer, with varied statuses.

5. **Invoice view** — Mentioned as future scope. The `OrderHistory` model should be designed with an `invoiceNumber: Option[String]` field to support this later without model changes.