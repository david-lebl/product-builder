# Customer Management, Pricing & Login — Analysis & Implementation Plan

This document covers the analysis and phased implementation plan for customer management, customer-specific pricing, agency login, and discount code management.

---

## Current State

### What exists today

| Area | Current State |
|------|--------------|
| **Customer model** | `CheckoutInfo` has `customerType: CustomerType` enum (`Guest`, `Registered`, `RegisteredCorporate`) — used only during checkout, not persisted |
| **Contact/Address** | `ContactInfo(firstName, lastName, email, phone, company?, companyRegNo?, vatId?)` and `Address(street, city, zip, country)` exist in `order.scala` — used by `CheckoutInfo` |
| **Order model** | `Order(id, basket, checkoutInfo, total, currency)` — no customer ID linkage |
| **Login** | Checkout Step 1 (Authentication) has a Guest vs. Registered toggle with email/password fields — purely UI placeholders, no real auth |
| **Discount codes** | `DiscountService` is a static `Map[String, Int]` of 11 hardcoded codes (e.g., `"SAVE5" → 5`) — no expiration, constraints, or CRUD |
| **Pricing** | `Pricelist` is a flat `List[PricingRule]` with currency/version — same prices for all customers |
| **Order history** | No persistence layer — orders exist only in `Var[List[ManufacturingOrder]]` during a session |
| **Manufacturing UI** | 7 views via `ManufacturingRoute` enum (Dashboard, StationQueue, OrderApproval, OrderProgress, Employees, Machines, Analytics) — no customer-related views |
| **Timestamps** | Existing models use `Long` (Unix millis) for timestamps (e.g., `ManufacturingOrder.createdAt: Long`) |

### Key architectural constraints

- **No backend** — all state is client-side (`Var`/`Signal`). Persistence is limited to `localStorage`.
- **Pure domain** — domain logic returns `Validation[E, A]`, never effects. Must stay Scala.js-compatible.
- **Bilingual** — all entities, errors, and UI use `LocalizedString` (English + Czech).
- **Rules as data** — pricing and compatibility use sealed ADTs interpreted by engines.
- **Timestamps as `Long`** — all existing models use Unix millis (`Long`), not `java.time.Instant`, for Scala.js compatibility.

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
├── companyInfo: Option[CompanyInfo]            // NEW case class
│   ├── companyName: String
│   ├── businessId: String (IČO)
│   ├── vatId: Option[String] (DIČ)
│   └── contactPerson: String
├── contactInfo: ContactInfo                    // REUSE from order.scala
├── address: Address                            // REUSE from order.scala
├── pricing: CustomerPricing
├── internalNotes: List[CustomerNote]
├── createdAt: Long                             // Unix millis (consistent with codebase)
├── lastOrderAt: Option[Long]
└── tags: Set[String]                           // deferred — included in model but no UI in Phase 6
```

**Reusing existing types:** `ContactInfo` and `Address` from `order.scala` are reused directly. The existing `ContactInfo` already includes `company`, `companyRegNo` (IČO), and `vatId` (DIČ) — these fields overlap with `CompanyInfo`. For the `Customer` entity, `CompanyInfo` is a dedicated type for agency-specific structured data (company name, contact person), while `ContactInfo` carries personal contact details. When pre-filling checkout, map `Customer.companyInfo` fields to `ContactInfo.company`/`companyRegNo`/`vatId`.

**CustomerType** — Extend the existing `CustomerType` enum:
- `Guest` — anonymous checkout (existing, unchanged)
- `Agency` — B2B customer with login, custom pricing (new, primary scope)
- `Regular` — B2C registered customer (future, out of scope)

**Migration of existing values:**
- `Registered` — deprecated, replaced by `Regular` (future). Remove once checkout UI is updated.
- `RegisteredCorporate` — deprecated, replaced by `Agency`. Existing `CheckoutView` references must be updated in Phase 7.
- The `CheckoutInfo.customerType` field continues to use the same enum. When an agency customer is logged in, it is set to `Agency`; the checkout flow adapts accordingly (skip authentication, pre-fill company info, allow `InvoiceOnAccount` payment).

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
├── globalDiscount: Option[Percentage]              // e.g., 10% off everything
├── categoryDiscounts: Map[CategoryId, Percentage]  // per-category override
├── materialDiscounts: Map[MaterialId, Percentage]  // per-material override
├── fixedMaterialPrices: Map[MaterialId, Price]     // fixed price override — Price includes Currency
├── finishDiscounts: Map[FinishId, Percentage]       // per-finish override
├── customQuantityTiers: Option[List[QuantityTier]]  // custom order-quantity volume breaks
├── customSheetQuantityTiers: Option[List[SheetQuantityTier]]  // custom sheet-quantity volume breaks
└── minimumOrderOverride: Option[Money]             // custom minimum order
```

**Note on `fixedMaterialPrices`:** Uses `Price` (which includes `Currency`) instead of bare `Money`. The `CustomerPricelistResolver` validates that fixed prices match the target pricelist's currency and ignores mismatched entries. This supports the multi-currency setup (USD + CZK pricelists).

**Note on quantity tiers:** The codebase has two tier types — `QuantityTier` (by order quantity) and `SheetQuantityTier` (by total sheets used). Customer-specific overrides for both are supported. `SheetQuantityTier` takes precedence when `totalSheets > 0` and sheet tier rules exist, matching existing `PriceCalculator` behavior.

**Intentionally out of scope:** Fold type, binding method, and printing process surcharge discounts are not included in `CustomerPricing`. These surcharges are typically fixed operational costs, not negotiated per customer. If needed in the future, add `foldDiscounts`, `bindingDiscounts`, and `processDiscounts` maps.

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
│   ├── validFrom: Option[Long]              // Unix millis
│   ├── validUntil: Option[Long]             // Unix millis
│   ├── maxUses: Option[Int]
│   ├── currentUses: Int
│   ├── minimumOrderValue: Option[Money]
│   ├── allowedCategories: Set[CategoryId]   // empty = all
│   ├── allowedCustomerTypes: Set[CustomerType] // empty = all
│   └── allowedCustomerIds: Set[CustomerId]  // empty = all (for exclusive codes)
├── isActive: Boolean
├── createdBy: Option[EmployeeId]
└── createdAt: Long                          // Unix millis
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

**Discount code stacking (decision):** Discount codes apply **on top of customer pricing** (compound). The flow is: base pricelist → `CustomerPricelistResolver` → `PriceCalculator` → customer-priced total → `DiscountCodeService.apply` → final total. This matches the intent: customer pricing is a permanent negotiated rate, discount codes are promotional/one-time.

**Checkout integration:** The existing `CheckoutView` Step 5 (Summary) uses `DiscountService.lookupPercent` to validate codes and applies a simple percentage to the items total. This will be replaced by `DiscountCodeService.validate` + `apply`, which returns a `DiscountResult` with breakdown. The `CheckoutInfo.discountCode: String` field is retained — the new service resolves it at validation time.

### 4. Agency Login Mechanism

Lightweight OTP (one-time password) flow — no traditional username/password:

```
LoginSession
├── customerId: CustomerId
├── sessionToken: String
├── createdAt: Long               // Unix millis
├── expiresAt: Long               // Unix millis

OtpRequest
├── identifier: String            // Business ID, VAT, or email
├── identifierType: IdentifierType (BusinessId | VatId | Email)

OtpToken
├── token: String (6-digit numeric)
├── customerId: CustomerId
├── createdAt: Long               // Unix millis
├── expiresAt: Long               // Unix millis (e.g., +10 minutes)
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
├── orderDate: Long              // Unix millis
├── items: List[OrderItemSummary]
├── total: Money
├── currency: Currency
├── status: OrderHistoryStatus (Placed | InProduction | Completed | Dispatched)
├── trackingNumber: Option[String]
├── invoiceNumber: Option[String] // future invoice support
```

**Prerequisite:** The `Order` model (`order.scala`) must be extended with `customerId: Option[CustomerId]` to link orders to customers. Without this, order history cannot filter by customer. This change is introduced in Phase 1 alongside the customer model.

Since there's no backend, this would be seeded from `ManufacturingOrder` data linked by `Order.customerId`. The model is designed so a real backend can populate it later.

---

## Implementation Phases

### Phase 1 — Customer Domain Model & Management Service ✅

**Domain scope:** Core customer entity, CRUD service, and `Order` model extension.

**Model (`model/customer.scala`):**
- `CustomerId` opaque type (in `ids.scala`)
- `Customer` case class with all fields listed above (timestamps as `Long`)
- `CustomerStatus` enum: `Active`, `Inactive`, `Suspended`, `PendingApproval`
- `CustomerTier` enum: `Standard`, `Silver`, `Gold`, `Platinum`
- `CompanyInfo` case class (new — `companyName`, `businessId`, `vatId`, `contactPerson`)
- Reuse existing `ContactInfo` and `Address` from `order.scala` (no new types)
- `CustomerNote` case class: `text`, `createdAt: Long`, `createdBy: Option[EmployeeId]`
- `LocalizedString` display names for all new enums: `CustomerStatus` (4 values × 2 languages), `CustomerTier` (4 values × 2 languages)

**Model (`model/order.scala`) — extension:**
- Add `customerId: Option[CustomerId] = None` to `Order` case class
- This links orders to customers, enabling order history (Phase 8) and the "Orders" tab in customer management (Phase 6)

**CustomerType migration (`model/order.scala`):**
- Add `Agency` variant to existing `CustomerType` enum
- Deprecate `Registered` and `RegisteredCorporate` (keep for backward compatibility until Phase 7 updates `CheckoutView`)

**Service (`service/CustomerManagementService.scala`):**
- Pure CRUD returning `Validation[CustomerManagementError, List[Customer]]`
- `addCustomer`, `updateCustomer`, `updateStatus`, `updateTier`, `addNote`, `removeCustomer`
- Validation: required fields, unique business ID, unique email

**Error (`service/CustomerManagementError.scala`):**
- ADT: `DuplicateBusinessId`, `DuplicateEmail`, `CustomerNotFound`, `InvalidStatus`, `MissingRequiredField`
- Bilingual `message(lang)` method

**Sample data (`sample/SampleCustomers.scala`):**
- 8-10 sample agency customers with varied tiers, statuses, and pricing configurations
- Diverse data enables meaningful testing of UI filters, search, and list views

**Tests (`CustomerManagementServiceSpec`):**
- CRUD operations, validation, status transitions, duplicate detection

**Estimated: ~15-20 tests**

**Implementation notes:**
- 24 tests in `CustomerManagementServiceSpec` covering CRUD, validation, duplicate detection, error messages, and enum display names
- 10 sample agency customers in `SampleCustomers` with varied tiers, statuses, notes, and tags

---

### Phase 2 — Customer-Specific Pricing Model & Engine ✅

**Domain scope:** Pricing overlay model, integration with `PriceCalculator`.

**Model (`pricing/CustomerPricing.scala`):**
- `Percentage` opaque type over `BigDecimal` (0–100 range)
- `CustomerPricing` case class with discount fields (including both `customQuantityTiers` and `customSheetQuantityTiers`)
- `CustomerPricingRule` sealed ADT:
  - `GlobalPercentageDiscount(percentage: Percentage)`
  - `CategoryPercentageDiscount(categoryId: CategoryId, percentage: Percentage)`
  - `MaterialPercentageDiscount(materialId: MaterialId, percentage: Percentage)`
  - `MaterialFixedPrice(materialId: MaterialId, price: Price)` — `Price` includes `Currency` for multi-currency safety
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

**No changes to `PriceBreakdown`:**
With Option B (modified pricelist), comparison is done by computing two separate `PriceBreakdown`s — one with the base pricelist, one with the customer-adjusted pricelist. The ViewModel layer (not the domain model) holds both breakdowns and derives the discount/savings display. This keeps `PriceBreakdown` unchanged and avoids coupling it to the customer pricing concept.

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

**Implementation notes:**
- Option B (modified pricelist) was implemented via `CustomerPricelistResolver.resolve(basePricelist, customerPricing, categoryId)`
- The `categoryId` parameter is optional — used to apply category-level discounts to material prices for the target category
- `Percentage.applyTo(money)` computes `money * (1 - percentage/100)` — returns the discounted price
- `Customer` model extended with `pricing: CustomerPricing` field
- `SampleCustomers` updated with diverse pricing configurations (global discounts, material/category/finish discounts, fixed prices, custom tiers)
- 28 tests in `CustomerPricelistResolverSpec` covering: empty pricing, global discount, material-specific discount, fixed material price (including currency mismatch), category discount with precedence, finish discount, custom quantity tiers, minimum order override, price comparison, combined discounts, and `Percentage` type validation

---

### Phase 3 — Production Cost Model & Warnings ✅

**Domain scope:** Production cost floor calculation and margin analysis.

**Model (`pricing/ProductionCost.scala`):**
- `ProductionCostRule` sealed ADT:
  - `MaterialUnitCost(materialId: MaterialId, cost: Money)`
  - `MaterialAreaCost(materialId: MaterialId, costPerM2: Money)`
  - `ProcessCost(processType: PrintingProcessType, costPerUnit: Money)`
  - `FinishCost(finishId: FinishId, costPerUnit: Money)`
  - `OverheadFactor(factor: BigDecimal)` — multiplier on total cost (e.g., 1.15)
- `ProductionCostSheet` — `List[ProductionCostRule]` with `currency: Currency` (analogous to `Pricelist`)
- `CostAnalysis` — result type:
  - `productionCost: Money`
  - `sellingPrice: Money`
  - `margin: Money`
  - `marginPercentage: Percentage`
  - `isBelowCost: Boolean`
  - `warnings: List[CostWarning]`
- `CostWarning` ADT: `BelowProductionCost(shortfall: Money)`, `LowMargin(marginPct: Percentage, threshold: Percentage)`
  - Bilingual `message(lang)` method (EN + CS)

**Service (`pricing/ProductionCostCalculator.scala`):**
- `calculateCost(config, costSheet): Validation[PricingError, Money]` — computes direct material + process + finish costs × overhead
- `analyze(config, pricelist, costSheet, lowMarginThreshold): Validation[PricingError, CostAnalysis]` — margin analysis against selling price
- `analyzeWithCustomerPricing(config, basePricelist, customerPricing, costSheet, lowMarginThreshold): Validation[PricingError, CostAnalysis]` — margin analysis using customer-resolved pricelist

**Sample data (`sample/SampleProductionCosts.scala`):**
- USD cost sheet: 8 material unit costs, 2 material area costs, 4 process costs, 6 finish costs, 1.15 overhead factor
- CZK cost sheet: proportionally scaled values for CZK currency

**Tests (`ProductionCostSpec`):**
- Cost calculation for various configurations
- Margin analysis with and without customer discounts
- Below-cost detection
- Low-margin warnings

**Estimated: ~10-12 tests**

**Implementation notes:**
- `ProductionCostCalculator` reuses `PriceCalculator.calculate` for selling price computation (no duplication)
- `analyzeWithCustomerPricing` delegates to `CustomerPricelistResolver.resolve` then `analyze`
- Cost calculation mirrors the area-based vs. unit-based material logic from `PriceCalculator` but simpler (no tiers, no setup fees, no ink config factors)
- Default low-margin threshold is 15% — configurable per analysis call
- Margin percentage computed as `(margin / productionCost) × 100`, clamped to ≥ 0
- 15 tests in `ProductionCostSpec` covering: unit-based costs, area-based costs, missing quantity, zero-cost defaults, healthy margin, below-cost detection, low-margin warnings, threshold customization, customer pricing impact, bilingual warning messages

---

### Phase 4 — Discount Code Domain Model & Service ✅

**Domain scope:** Replace hardcoded discount codes with proper model.

**Model (`model/discount.scala`):**
- `DiscountCodeId` opaque type (added to `ids.scala`)
- `DiscountCode` case class (as described in analysis)
- `DiscountType` enum: `Percentage`, `FixedAmount`, `FreeDelivery` — with `LocalizedString` display names (3 values x 2 languages)
- `DiscountConstraints` case class
- `DiscountResult` case class: `originalTotal`, `discountAmount`, `finalTotal`, `appliedCode`

**Service (`service/DiscountCodeService.scala`):**
- Replaces existing `DiscountService`
- `findByCode(codes, code)` — case-insensitive lookup
- `validate(codes, code, context): Validation[DiscountCodeError, DiscountCode]` — validates active, not expired, not exhausted, minimum order, category eligibility, customer eligibility
- `applyDiscount(codes, code, subtotal, context): Validation[DiscountCodeError, DiscountResult]` — validates + computes discount
- `DiscountValidationContext`: `orderValue`, `categoryIds`, `customerType`, `customerId`, `now`
- CRUD: `createCode`, `updateCode`, `deactivateCode`, `incrementUsage` — with uniqueness and value validation

**Error (`service/DiscountCodeError.scala`):**
- Full ADT: `CodeNotFound`, `CodeExpired`, `CodeNotYetValid`, `CodeExhausted`, `CodeInactive`, `BelowMinimumOrder`, `CategoryNotEligible`, `CustomerNotEligible`, `DuplicateCode`, `CodeIdNotFound`, `InvalidDiscountValue`
- Bilingual messages (English + Czech)

**Backward compatibility:**
- `DiscountService` retained as-is (deprecated) — `DiscountCodeService` used in its place
- All 11 hardcoded codes migrated to `SampleDiscountCodes`

**Sample data (`sample/SampleDiscountCodes.scala`):**
- 11 migrated codes from `DiscountService` (SAVE5-SAVE50, WELCOME20, SUMMER10, PRINT15)
- 9 new rich codes: fixed amount (FLAT100), free delivery (FREESHIP), VIP-only (VIP25), expired (OLDCODE), future (UPCOMING), exhausted (USED100), inactive (DISABLED), category-restricted (CARDS10), agency-only (AGENCY15)
- 20 total sample discount codes

**Tests (`DiscountCodeServiceSpec`):**
- 29 tests covering: code validation (found, not found, expired, not-yet-valid, exhausted, inactive), constraint checking (min order, category, customer type/ID), discount application (percentage, fixed amount, free delivery), CRUD operations (create, update, deactivate, increment usage, uniqueness), case-insensitive lookup, bilingual display names, bilingual error messages

**Estimated: ~18-22 tests** → **Actual: 29 tests**

---

### Phase 5 — Agency Login Domain Model ✅

**Domain scope:** OTP-based login for agency customers.

**Model (`model/login.scala`):**
- `SessionId` opaque type (added to `login.scala` alongside other login types)
- `LoginSession` case class: `sessionId`, `customerId`, `createdAt`, `expiresAt`
- `OtpRequest` case class: `customerId`, `identifier`, `identifierType`, `requestedAt`
- `OtpToken` case class: `customerId`, `token`, `createdAt`, `expiresAt`
- `IdentifierType` enum: `BusinessId`, `VatId`, `Email` — with `LocalizedString` display names (3 values x 2 languages)

**Service (`service/LoginService.scala`):**
- `lookupCustomer(identifier, identifierType, customers): Validation[LoginError, Customer]` — case-insensitive email lookup, trims whitespace, rejects Inactive/Suspended/PendingApproval
- `generateOtp(customer, now): OtpToken` — pure, deterministic 6-digit token from customer ID + timestamp; 5-minute validity
- `validateOtp(inputToken, otpToken, now): Validation[LoginError, LoginSession]` — validates expiry and token match; 24-hour session validity
- `isSessionValid(session, now): Boolean`

**Error (`service/LoginError.scala`):**
- Full ADT: `CustomerNotFound`, `OtpExpired`, `OtpInvalid`, `CustomerInactive`, `CustomerSuspended`, `SessionExpired`
- Bilingual messages (English + Czech)

**Tests (`LoginServiceSpec`):**
- 20 tests covering: customer lookup (by business ID, VAT ID, email, case-insensitive, whitespace trimming), customer rejection (not found, inactive, suspended, pending-approval), OTP generation (6-digit format, deterministic, different timestamps → different tokens), OTP validation (correct, expired, wrong token, whitespace trimming), session validity (valid, expired, exact boundary), bilingual display names and error messages

**Estimated: ~12-15 tests** → **Actual: 20 tests**

---

### Phase 6 — Customer Management UI (Manufacturing)

**UI scope:** Internal views for managing customers and their pricing within the manufacturing section.

**New manufacturing routes (extend `ManufacturingRoute` enum in `ManufacturingModel.scala`):**
- `Customers` ("👤") — customer list and management
- `DiscountCodes` ("🏷️") — discount code management
- Add corresponding `case` arms in `ManufacturingApp.scala` route match

**ManufacturingViewModel extensions:**
- `customers: Var[List[Customer]]` — initialized from `SampleCustomers`
- `discountCodes: Var[List[DiscountCode]]` — initialized from `SampleDiscountCodes`

**Persistence note:** In the current no-backend architecture, customer and discount code data live only in `Var` state. Refreshing the page resets to sample data. This is acceptable for the demo/prototype scope. When persistence is needed, serialize to `localStorage` (same pattern as login session). This is explicitly deferred — not a Phase 6 deliverable.
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
- Derive from `ManufacturingOrder` list filtered by `Order.customerId` (added in Phase 1)
- Map manufacturing statuses (`ApprovalStatus` / `WorkflowStatus`) to customer-facing `OrderHistoryStatus`

**Estimated: ~300-400 lines**

---

## Phase 9 — Catalog Configuration UI with JSON Persistence ✅

**Goal:** Provide an admin UI for creating and editing a custom product catalog, compatibility rules, and pricelists — with full JSON export/import for persistence. This enables non-developers to configure the system without code changes.

**Dependencies:** None (standalone, parallel with all other phases)

### 9.1 JSON Codec Infrastructure (Domain)

**File: `codec/DomainCodecs.scala`**

Added `zio-json` (v0.7.3) to the cross-compiled domain module and created comprehensive JSON codecs for all domain types:

- **Opaque type IDs** — `CategoryId`, `MaterialId`, `FinishId`, `PrintingMethodId`, `ConfigurationId` (encoded as JSON strings)
- **Enums** — `MaterialFamily`, `MaterialProperty`, `FinishType`, `FinishSide`, `ComponentRole`, `PrintingProcessType`, `SpecKind`, `Currency`, `InkType`, `Orientation`, `FoldType`, `BindingMethod` (encoded as string names)
- **Value objects** — `Money` (BigDecimal), `PaperWeight` (Int), `Quantity` (Int), `LocalizedString` (Map[String,String])
- **Entities** — `Material`, `Finish`, `PrintingMethod`, `ProductCategory`, `ComponentTemplate`
- **Rules** — `SpecPredicate`, `ConfigurationPredicate` (recursive ADT with And/Or/Not), `CompatibilityRule` (14 variants), `CompatibilityRuleset`
- **Pricing** — `PricingRule` (18 variants), `Pricelist`
- **Catalog** — `ProductCatalog` (with ID-keyed map codecs)
- **Export container** — `CatalogExport` (catalog + ruleset + pricelists)

**Tests: 27 round-trip tests** covering IDs, enums, value objects, entities, rules (including nested predicates), pricing rules, and full catalog/export serialization with sample data.

### 9.2 Abstract ADT-Derived Form Components

**File: `catalog/FormComponents.scala`**

Reusable form components that leverage Scala's enum system for type-safe UI generation:

| Component | Description |
|-----------|-------------|
| `textField` | Simple labeled text input |
| `numberField` | Numeric text input |
| `optionalNumberField` | Number input with `Option[Int]` binding |
| `enumSelect[E]` | Dropdown for any Scala 3 enum (optional selection) |
| `enumSelectRequired[E]` | Dropdown for any Scala 3 enum (required selection) |
| `enumCheckboxSet[E]` | Checkbox set for selecting `Set[E]` from enum values |
| `idCheckboxSet[Id]` | Checkbox set for selecting `Set[Id]` from available entities |
| `localizedStringEditor` | Dual-language (EN/CS) text inputs for `LocalizedString` |
| `moneyField` | BigDecimal input for `Money` values |
| `actionButton` / `dangerButton` | Styled action buttons |
| `sectionHeader` | Section heading |

These components are generic — `enumSelect`, `enumCheckboxSet`, and `idCheckboxSet` work with any Scala 3 enum or opaque type ID, reducing boilerplate across all editor views.

### 9.3 Catalog Editor UI

**Architecture:**

```
CatalogEditorApp.scala          — Main view with sidebar navigation
CatalogEditorModel.scala        — CatalogSection enum, EditState ADT, CatalogEditorState
CatalogEditorViewModel.scala    — Reactive state (Var[CatalogEditorState]) with CRUD operations
views/
  CategoryEditorView.scala      — Categories with nested ComponentTemplate editors
  MaterialEditorView.scala      — Materials with family/weight/properties
  FinishEditorView.scala        — Finishes with type/side selection
  PrintingMethodEditorView.scala — Printing methods with process type/max colors
  RulesEditorView.scala         — Compatibility rules with per-type form editors
  PricelistEditorView.scala     — Multi-pricelist management with per-type pricing rule editors
  ExportImportView.scala        — JSON export/import with catalog statistics
```

**Sections (7 sidebar tabs):**

1. **📦 Categories** — CRUD with nested `ComponentTemplate` editing (role, allowed materials/finishes, optional flag)
2. **📄 Materials** — CRUD with family, weight, material properties
3. **✨ Finishes** — CRUD with finish type and side
4. **🖨 Printing Methods** — CRUD with process type and max color count
5. **📏 Rules** — Add/edit/remove compatibility rules; rule type selector with contextual form fields for each of the 14 rule variants
6. **💰 Pricelist** — Multi-pricelist support (currency tabs), add/edit/remove pricing rules for each of the 18 rule variants
7. **📤 Export/Import** — Export current state to JSON, import from JSON, load sample data, catalog statistics display

**Route:** `AppRoute.CatalogEditor` accessible via "Catalog Editor" / "Editor katalogu" navigation button.

**Key design decisions:**
- **In-memory state** — All edits are held in `Var[CatalogEditorState]` until exported. No auto-persistence.
- **Sample data loading** — "Load Sample Data" button populates with the full `SampleCatalog` + `SampleRules` + `SamplePricelist` for quick testing.
- **Rule editors are exhaustive** — All 14 compatibility rule types and 18 pricing rule types have form editors with appropriate field sets.
- **No upstream dependency** — The catalog editor is fully standalone; it does not modify the live `ProductBuilderViewModel.catalog`.

**Estimated: ~1,500 lines (UI) + ~200 lines (codecs) + ~250 lines (tests)**

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

Phase 9: Catalog Configuration UI ─── (standalone, parallel)
          (JSON Persistence)
```

| Phase | Domain | UI | Tests | Dependencies | Status |
|-------|--------|-----|-------|-------------|--------|
| 1 — Customer Model | `customer.scala`, `CustomerManagementService`, `Order` extension | — | 24 | None | ✅ Done |
| 2 — Customer Pricing | `CustomerPricing`, `CustomerPricelistResolver` | — | 28 | Phase 1 | ✅ Done |
| 3 — Production Cost | `ProductionCost`, `ProductionCostCalculator` | — | 15 | Phase 2 | ✅ Done |
| 4 — Discount Codes | `DiscountCode`, `DiscountCodeService` | — | 29 | None (parallel with 1-3) | ✅ Done |
| 5 — Login Model | `LoginSession`, `LoginService` | — | 20 | Phase 1 | ✅ Done |
| 6 — Customer Mgmt UI | — | `CustomersView`, `DiscountCodesView` | — | Phases 1-5 | |
| 7 — Builder Integration | — | `LoginWidget`, PricePreview, Checkout, `CustomerType` migration | — | Phases 5, 6 | |
| 8 — Order History | — | `OrderHistoryView` | — | Phase 7 | |
| 9 — Catalog Config UI | `DomainCodecs` (zio-json) | `CatalogEditorApp`, `FormComponents`, 7 editor views | 27 | None (parallel) | ✅ Done |

**Total estimated new tests: ~100-120** (including 27 codec tests from Phase 9)

---

## Detailed Domain Model Diagram

```
mpbuilder.domain
├── model/
│   ├── customer.scala          [NEW — Phase 1]
│   │   ├── Customer
│   │   ├── CustomerStatus (Active/Inactive/Suspended/PendingApproval)
│   │   ├── CustomerTier (Standard/Silver/Gold/Platinum)
│   │   ├── CompanyInfo (new case class)
│   │   ├── CustomerNote
│   │   └── (reuses ContactInfo, Address from order.scala)
│   ├── order.scala             [EXTEND — Phase 1]
│   │   ├── Order               (+customerId: Option[CustomerId])
│   │   └── CustomerType        (+Agency variant; deprecate Registered, RegisteredCorporate)
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
│   │   ├── ProductionCostRule (sealed ADT)
│   │   ├── ProductionCostSheet
│   │   ├── CostAnalysis
│   │   └── CostWarning (sealed ADT)
│   └── ProductionCostCalculator.scala  [NEW — Phase 3]
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
├── codec/
│   └── DomainCodecs.scala          [NEW — Phase 9]
│       ├── JSON codecs for all domain types (zio-json)
│       └── CatalogExport (catalog + ruleset + pricelists)

mpbuilder.ui.catalog                [NEW — Phase 9]
├── CatalogEditorApp.scala          (main editor view with sidebar)
├── CatalogEditorModel.scala        (CatalogSection, EditState, CatalogEditorState)
├── CatalogEditorViewModel.scala    (reactive CRUD + JSON import/export)
├── FormComponents.scala            (ADT-derived form components)
└── views/
    ├── CategoryEditorView.scala
    ├── MaterialEditorView.scala
    ├── FinishEditorView.scala
    ├── PrintingMethodEditorView.scala
    ├── RulesEditorView.scala
    ├── PricelistEditorView.scala
    └── ExportImportView.scala
```

---

## Design Decisions & Rationale

### Why Option B (modified pricelist) for customer pricing?

Generating a customer-specific `Pricelist` via `CustomerPricelistResolver` means:
- The existing `PriceCalculator` handles all calculation — no parallel pricing engine
- `PriceBreakdown` naturally shows customer prices in line items — no extension to `PriceBreakdown` needed
- Adding new `PricingRule` types automatically works with customer pricing
- Base price comparison is a second `PriceCalculator.calculate` call with the original pricelist
- The ViewModel layer holds both breakdowns (base and customer) and derives discount/savings display

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

### Why `Long` timestamps instead of `Instant`?

The existing codebase uses `Long` (Unix millis) for all timestamps — `ManufacturingOrder.createdAt`, `ManufacturingWorkflow.startedAt`, etc. Using `java.time.Instant` would require a polyfill for Scala.js compatibility (`scalajs-java-time` or similar). To stay consistent and avoid a new dependency, all new models use `Long`. A future migration to proper datetime types can be done across the whole codebase at once.

### Why reuse `ContactInfo`/`Address` from `order.scala`?

The existing `ContactInfo` already has company fields (`company`, `companyRegNo`, `vatId`). Creating a separate customer-specific `ContactInfo` would mean:
- Two similar but different types to maintain
- Mapping logic between them at checkout
- Confusion about which to use where

Instead, `Customer` reuses the existing types and adds `CompanyInfo` as a separate structured type for agency-specific data (company name, contact person). At checkout pre-fill time, `CompanyInfo` fields map to `ContactInfo.company`/`companyRegNo`/`vatId`.

### Why compound stacking for discount codes?

Discount codes apply on top of already-discounted customer prices (compound stacking). Rationale:
- Customer pricing and discount codes serve different purposes (negotiated rate vs. promotional)
- Applying codes only to the base price would make promotions less valuable for agency customers
- The flow is clear: base → customer overlay → discount code → final total

---

## Resolved Decisions

1. **Discount code stacking** — Discount codes apply on top of customer pricing (compound). See "Why compound stacking" rationale above.

2. **Multi-currency customer pricing** — `MaterialFixedPrice` uses `Price` (includes `Currency`). `CustomerPricelistResolver` validates currency match with the target pricelist and ignores mismatched entries.

3. **Timestamps** — All new models use `Long` (Unix millis) for consistency with existing codebase. See "Why `Long` timestamps" rationale above.

4. **`PriceBreakdown` unchanged** — No new fields on `PriceBreakdown`. Comparison is done at the ViewModel layer by computing two breakdowns (base vs. customer pricelist).

5. **`ContactInfo`/`Address` reuse** — Existing types from `order.scala` are reused. `CompanyInfo` is a new type for agency-specific fields. See rationale above.

6. **Fold/binding/process discounts** — Intentionally out of scope for `CustomerPricing`. These are operational costs, not typically negotiated per customer.

## Open Questions

1. **Customer pricing versioning** — Should we keep history of pricing changes? For now, no — the current pricing is the only pricing. History can be added later with timestamps.

2. **Order history data seeding** — How many sample orders to create for demo purposes? Recommendation: 5-8 orders per sample customer, with varied statuses.

3. **Invoice view** — Mentioned as future scope. The `OrderHistory` model includes `invoiceNumber: Option[String]` to support this later without model changes.

4. **`localStorage` persistence** — Currently all customer/discount code data resets on page refresh (in-memory `Var` only). Should Phase 6 include basic `localStorage` serialization, or is sample-data-only acceptable for the prototype? Recommendation: defer persistence, document as a known limitation.

5. **`Registered`/`RegisteredCorporate` removal timeline** — These deprecated `CustomerType` variants can be removed once Phase 7 updates all `CheckoutView` references. Should this be a hard requirement for Phase 7 completion, or a follow-up cleanup?