# External Manufacturing Analysis

> Specification for routing orders to external manufacturing partners — covering domain model additions, rule extensions, workflow hook, lead-time model, and minimal UI surface. No code changes are made in this session; implementation follows in a subsequent session.

---

## 1. Context & Goals

### 1.1 Why external manufacturing

The shop's current production pipeline handles everything in-house across 14 `StationType` variants. Two categories of orders cannot be fulfilled in-house:

| Category | Constraint | Current behaviour |
|---|---|---|
| **Oversized banners** (>150×150 cm) | In-house large-format printer tops out at 1500×1500 mm | Hard-blocked via `CompatibilityRule.SpecConstraint(bannersId, MaxDimension(1500, 1500), ...)` — order is rejected |
| **Spot UV varnish** (specialty process) | Requires dedicated UV coater not available on all shop configurations | Blocked at validation time |

The business wants to **accept** these orders and route them to vetted external manufacturing partners, with the following constraints:
- Only `ManufacturingSpeed.Standard` turnaround is offered for outsourced jobs (partners do not provide rush or economy service).
- Customer-facing ETAs shift from hours/days to weeks (typically 2–3), driven by partner lead time.
- Partner availability can vary (vacation, overbooked). Jobs placed during partner closures must surface a realistic ETA or be blocked gracefully.
- The production pipeline needs a visible handoff step so operators know the job left the building.

### 1.2 Non-goals (this phase)

The following are explicitly **out of scope** and must not be implemented in the follow-on session:
- Batch consolidation across multiple orders to the same partner.
- Full shipment-tracking state machine (outbound / in-transit / delivered-to-partner / returned).
- Partner self-service portal for availability or quote submission.
- Automated file transfer / artwork export to partner systems.
- Partner CRUD editor in the admin UI (partners are seeded via `SamplePartners`).
- Vacation calendar UI for partner availability management.

---

## 2. Domain Model Additions

All new types are placed in `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/` and must be pure (no ZIO effects) so they cross-compile to Scala.js.

### 2.1 `PartnerId` — opaque ID type

Mirrors `EmployeeId` / `MachineId` from `model/manufacturing.scala:27-45`:

```scala
// modules/domain/src/main/scala/mpbuilder/domain/manufacturing/ExternalPartner.scala
opaque type PartnerId = String
object PartnerId:
  def apply(value: String): Validation[String, PartnerId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("PartnerId must not be empty")

  def unsafe(value: String): PartnerId = value

  extension (id: PartnerId) def value: String = id
```

### 2.2 `PartnerAvailability`

```scala
final case class PartnerAvailability(
    onVacation: Boolean,
    unavailableUntil: Option[java.time.LocalDate],
    blockedDates: Set[java.time.LocalDate],
)
```

A partner is considered **unavailable** on a given date if:
- `onVacation == true`, or
- The date is `<= unavailableUntil.get` (when defined), or
- The date appears in `blockedDates`.

### 2.3 `ExternalPartner`

```scala
final case class ExternalPartner(
    id: PartnerId,
    name: LocalizedString,
    capabilities: Set[StationType],       // which in-house station(s) this partner substitutes
    supportedCategories: Set[CategoryId], // categories this partner can handle
    leadTimeBusinessDays: (Int, Int),     // (min, max) — drives customer-facing ETA
    priceMarkup: BigDecimal,              // e.g. 1.15 = 15% above base price
    availability: PartnerAvailability,
    contact: String,                      // email / phone / URL — for operator step notes
)
```

**`capabilities`** is the bridge to the workflow: it declares which `StationType` variant(s) this partner handles. The `WorkflowGenerator` replaces matching in-house steps with the `ExternalPartner` station step (Section 4).

### 2.4 `PartnerTierPolicy`

A single policy object (not a per-partner record) asserting that external partners expose **only** `ManufacturingSpeed.Standard`. Express and Economy are disabled categorically:

```scala
object PartnerTierPolicy:
  /** The only tier available when routing to an external partner. */
  val allowedTier: ManufacturingSpeed = ManufacturingSpeed.Standard
  val disabledTiers: Set[ManufacturingSpeed] =
    Set(ManufacturingSpeed.Express, ManufacturingSpeed.Economy)
```

Kept as a named policy object rather than a per-partner field so the invariant is expressed once. A future override point (e.g., per-partner `allowedTiers: Set[ManufacturingSpeed]`) is noted here for awareness but not added in this phase.

### 2.5 New `StationType` variant: `ExternalPartner`

A single generic variant is added to the `StationType` enum in `model/manufacturing.scala`:

```scala
enum StationType:
  // ... existing 14 variants ...
  case ExternalPartner   // new — represents hand-off to an external manufacturing partner
```

The specific partner identity is **not** encoded in the enum variant. Instead, it is carried on the `WorkflowStep` via a new optional field (Section 4.1). This keeps the enum clean and avoids combinatorial growth as new partners are onboarded.

Display name and icon extensions must also be added to `StationType`'s companion object:

```scala
case ExternalPartner => "External Partner"   // displayName
case ExternalPartner => "🏭"                 // icon (reuses factory icon; distinct from OffsetPress)
```

---

## 3. Rules

All extensions follow the existing "rules as sealed ADTs" principle — new variants are data, not functions, and are interpreted by pure engine code.

### 3.1 New `CompatibilityRule` variant: `RequiresExternalPartner`

Added to `modules/domain/src/main/scala/mpbuilder/domain/rules/CompatibilityRule.scala`:

```scala
case RequiresExternalPartner(
    categoryId: CategoryId,
    predicate: ConfigurationPredicate,   // when this predicate matches, external routing is required
    candidatePartners: Set[PartnerId],   // ordered by preference; first available is selected
    reason: LocalizedString,
)
```

**Semantics**: Unlike existing blocking rules, a configuration matching this predicate is **valid** — it is accepted and routed to one of the `candidatePartners`. The rule evaluator flags the configuration with an `ExternalRoutingRequired` annotation rather than raising a `ConfigurationError`.

#### 3.1.1 Migration of the banner max-dimension rule

The existing hard block in `SampleRules.scala`:

```scala
// BEFORE (blocking)
CompatibilityRule.SpecConstraint(
  cat.bannersId,
  SpecPredicate.MaxDimension(1500, 1500),
  "Banners up to 150×150 cm (in-house production)",
)
```

Is **replaced** (not supplemented) with:

```scala
// AFTER (routes to external partner for oversized banners)
CompatibilityRule.RequiresExternalPartner(
  categoryId = cat.bannersId,
  predicate = ConfigurationPredicate.Not(
    ConfigurationPredicate.Spec(SpecPredicate.MaxDimension(1500, 1500))
  ),
  candidatePartners = Set(PartnerId.unsafe("partner-large-format")),
  reason = LocalizedString(
    en = "Banners larger than 150×150 cm are produced by our large-format partner (2–3 weeks)",
    cs = "Bannery větší než 150×150 cm vyrábí náš partner pro velkoplošný tisk (2–3 týdny)",
  ),
)
```

The banner min-dimension constraint (`MinDimension(300, 200)`) is unchanged.

#### 3.1.2 Spot varnish external routing rule

```scala
CompatibilityRule.RequiresExternalPartner(
  categoryId = cat./* all categories that allow spot varnish */,
  predicate = ConfigurationPredicate.HasFinishId(cat.varnishId),
  candidatePartners = Set(PartnerId.unsafe("partner-spot-varnish")),
  reason = LocalizedString(
    en = "Spot UV varnish is applied by our specialist partner (1–2 weeks)",
    cs = "Spotový UV lak aplikuje náš specializovaný partner (1–2 týdny)",
  ),
)
```

Note: `FinishRequiresFinishType(cat.varnishId, FinishType.Lamination, ...)` (the "spot varnish requires lamination base" rule in `SampleRules.scala`) remains in force — the partner applies varnish only after the in-house lamination step.

### 3.2 New `PricingRule` variant: `ExternalPartnerMarkup`

Added to `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala`:

```scala
case ExternalPartnerMarkup(partnerId: PartnerId, multiplier: BigDecimal)
```

**Insertion point in the calculation flow** (extending the existing pipeline documented in `pricing.md`):

```
subtotal
  → × quantityMultiplier       (QuantityTier / SheetQuantityTier)
  → × speedMultiplier          (ManufacturingSpeedSurcharge — always 1.0× for Standard)
  → × partnerMarkup            (ExternalPartnerMarkup — NEW, applied here)
  → + setupFees                (FinishTypeSetupFee, FinishSetupFee, etc.)
  → max(total, minimumOrderPrice)
```

The markup is applied **after** tier/quantity multipliers and **before** setup fees. This preserves the existing "setup fees are not discounted" invariant — physical setup costs (plates, dies) are not marked up via the partner rate.

`MinimumOrderPrice` continues to apply as the final floor and is not bypassed by the markup.

### 3.3 New validation error: `ExternalManufacturingError`

Added as a new error enum in `modules/domain/src/main/scala/mpbuilder/domain/validation/`:

```scala
enum ExternalManufacturingError:
  /** All candidate partners for the configuration are currently unavailable. */
  case PartnerUnavailable(
      partnerId: PartnerId,
      unavailableUntil: Option[java.time.LocalDate],
  )

  def message(lang: Language): String = this match
    case PartnerUnavailable(id, None) =>
      lang match
        case Language.En => s"Partner '${id.value}' is currently unavailable"
        case Language.Cs => s"Partner '${id.value}' není momentálně dostupný"
    case PartnerUnavailable(id, Some(until)) =>
      lang match
        case Language.En => s"Partner '${id.value}' is unavailable until ${until}"
        case Language.Cs => s"Partner '${id.value}' není dostupný do ${until}"
```

This error is surfaced when **all** `candidatePartners` in a `RequiresExternalPartner` rule are unavailable at order time. If at least one candidate partner is available, the order proceeds (the first available partner in the set is selected).

---

## 4. Workflow Generator Hook

`WorkflowGenerator.generate(config, ...)` in `modules/domain/src/main/scala/mpbuilder/domain/service/WorkflowGenerator.scala` is extended as follows.

### 4.1 `WorkflowStep` — new optional field

```scala
final case class WorkflowStep(
    // ... existing fields ...
    assignedPartner: Option[PartnerId] = None,              // NEW
    estimatedCompletionOverride: Option[java.time.Instant] = None, // NEW
)
```

- `assignedPartner` is set only when `stationType == StationType.ExternalPartner`.
- `estimatedCompletionOverride` allows operators to record a partner-quoted completion date that overrides the formula-based ETA (Section 5).

### 4.2 Generator logic

`WorkflowGenerator.generate` receives the active `CompatibilityRuleset` (or a pre-evaluated list of matching `RequiresExternalPartner` rules) alongside the existing parameters.

**Algorithm**:

1. Evaluate all `RequiresExternalPartner` rules against the configuration. Collect matching rules and their `candidatePartners`.
2. For each matching rule, select the **first available** partner from `candidatePartners` (checked against `PartnerAvailability` for the current date).
3. Determine which in-house station steps would have been generated for the configuration. Identify any step whose `stationType` is in `selectedPartner.capabilities`.
4. **Replace** those station steps with a single `ExternalPartner` step:
   ```scala
   WorkflowStep(
     id = gen.next(),
     stationType = StationType.ExternalPartner,
     componentRole = None,
     dependsOn = Set(prepressStep.id),   // always depends on prepress
     status = StepStatus.Waiting,
     assignedTo = None,
     assignedMachine = None,
     assignedPartner = Some(selectedPartner.id),
     startedAt = None,
     completedAt = None,
     notes = s"External production — ${selectedPartner.name(Language.En)}\nContact: ${selectedPartner.contact}",
   )
   ```
5. Post-return in-house steps `QualityControl` and `Packaging` depend on the `ExternalPartner` step, preserving the DAG:
   ```
   Prepress → ExternalPartner → QualityControl → Packaging
   ```
6. Intermediate in-house steps that the partner subsumes (e.g., `LargeFormatPrinter`, `LargeFormatFinishing` for the large-format banner partner) are simply omitted from the generated workflow.

**Invariants preserved**:
- Prepress is always in-house and is always the first step.
- `QualityControl` and `Packaging` are always in-house and always occur after the external step.
- No shipment-out / shipment-in state machine exists in this phase. The operator marks the `ExternalPartner` step **Completed** when the physical product is received back from the partner. Standard `StepStatus` states apply without modification.

### 4.3 Signature change

```scala
def generate(
    config: ProductConfiguration,
    orderId: OrderId,
    orderItemIndex: Int,
    workflowId: WorkflowId,
    priority: Priority = Priority.Normal,
    deadline: Option[Long] = None,
    createdAt: Long = System.currentTimeMillis(),
    externalPartners: Map[PartnerId, ExternalPartner] = Map.empty,  // NEW
    matchingExternalRules: List[CompatibilityRule.RequiresExternalPartner] = Nil, // NEW
): ManufacturingWorkflow
```

Passing these as parameters rather than reaching into a global keeps the function pure and testable.

---

## 5. Lead-Time Model

### 5.1 Base ETA formula

For a workflow containing an `ExternalPartner` step, the customer-facing ETA is computed as:

```
ETA = now
    + prepress duration            (from SampleManufacturing.stationTimeEstimates for Prepress)
    + partner.leadTimeBusinessDays.max  (in business days, using existing advanceByWorkingMinutes logic)
    + QC & packaging buffer         (fixed: 1 business day)
```

The existing `CompletionEstimator.estimate(...)` is **not** called for external workflows; instead, a new partner-aware branch in `CompletionEstimator` handles these:

```scala
def estimateExternal(
    partner: ExternalPartner,
    quantity: Int,
    stationEstimates: List[StationTimeEstimate],
    schedule: ShopSchedule,
    orderTime: LocalDateTime,
): CompletionEstimate
```

This function:
1. Computes prepress minutes using `estimateProductionTime(List(StationType.Prepress), quantity, stationEstimates)`.
2. Converts `partner.leadTimeBusinessDays._2` (max) to working minutes: `leadTimeMaxDays * workingHoursPerDay`.
3. Adds a fixed QC+packaging buffer of `1 business day = workingHoursPerDay * 60` minutes.
4. Uses `advanceByWorkingMinutes` (reused unchanged) to compute `latestCompletion`.
5. Uses `partner.leadTimeBusinessDays._1` (min) for `earliestCompletion`.

The existing shop-hour math, queue-wait estimation, and cutoff-time logic are **bypassed** for external workflows.

### 5.2 Manual override

When an operator sets `WorkflowStep.estimatedCompletionOverride`, the UI and `CompletionEstimator` surface that date directly instead of the formula-based estimate. This handles the common case where a partner has provided a specific quote date.

---

## 6. Tier Restrictions Integration

When a configuration matches any `RequiresExternalPartner` rule, the tier selector in the configurator must offer **only** `ManufacturingSpeed.Standard`.

Implementation: `TierRestrictionValidator` gains a new check:

```scala
// If any RequiresExternalPartner rule matches the configuration, only Standard is allowed.
if matchingExternalRules.nonEmpty && speed != ManufacturingSpeed.Standard then
  Validation.fail(TierRestrictionViolation.ExternalPartnerTierNotAllowed(speed))
```

A new `TierRestrictionViolation` variant is added:

```scala
case ExternalPartnerTierNotAllowed(requestedTier: ManufacturingSpeed)
```

The per-category `TierRestriction` records in `SampleTierRestrictions.scala` themselves are **unchanged**.

---

## 7. Minimal UI Touchpoints

### 7.1 Basket / configurator

- **Price line**: when external routing is required, a badge renders beneath the unit price:
  ```
  🏭 External production (Partner X) — approx. 2–3 weeks
  ```
- **Tier selector**: Express and Economy options are **disabled** with a tooltip:
  ```
  "Not available for external production — Standard turnaround only"
  ```
  The existing `RadioGroup` / `SelectField` components support disabled options via a `disabled: Boolean` on `RadioOption` / `SelectOption`; no new UI-framework components are needed.
- **ETA display**: the `CompletionEstimate.formatRange` output will read in terms of calendar dates (e.g., "May 12 – May 16") rather than same-day/tomorrow. No new formatting logic is needed beyond the existing `monthDayStr` path in `CompletionEstimator.formatDateTime`.

### 7.2 Manufacturing dashboard (`ManufacturingViewModel`)

- **External step card**: `ExternalPartner` steps render with the 🏭 icon and a distinct background colour (CSS class `step-external`) to visually distinguish them from in-house steps.
- **Step detail panel**: surfaces:
  - Partner display name (from `ExternalPartner.name`)
  - Partner contact string (from `ExternalPartner.contact`)
  - Expected return date (from `estimatedCompletionOverride` if set, else formula-based ETA)
- No new Laminar components are required; the existing step detail panel is extended with conditional rendering.

### 7.3 Out of scope

The following UI features are explicitly deferred:
- Partner CRUD editor (partners are seeded via `SamplePartners`).
- Vacation calendar management UI for `PartnerAvailability`.
- Shipment tracking states (outbound / received) beyond the existing `StepStatus` enum.
- Customer-facing order confirmation copy changes (flagged; copy update only, no domain change).

---

## 8. Data Seeding

New file: `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePartners.scala`

Placement follows `SampleStations` (which lives in `SampleManufacturing.scala`). The two seed partners:

### `partner-large-format`

| Field | Value |
|---|---|
| `id` | `partner-large-format` |
| `name` | EN: "LargePrint Partner" / CS: "Partner pro velkoplošný tisk" |
| `capabilities` | `{StationType.LargeFormatPrinter, StationType.LargeFormatFinishing}` |
| `supportedCategories` | `{bannersId}` |
| `leadTimeBusinessDays` | `(10, 14)` |
| `priceMarkup` | `1.15` (15%) |
| `availability` | `PartnerAvailability(onVacation = false, unavailableUntil = None, blockedDates = Set.empty)` |
| `contact` | `"partner-lf@example.com"` |

### `partner-spot-varnish`

| Field | Value |
|---|---|
| `id` | `partner-spot-varnish` |
| `name` | EN: "Varnish Specialist" / CS: "Specialista na lak" |
| `capabilities` | `{StationType.UVCoater}` |
| `supportedCategories` | `{/* all categories where spot varnish is offered */}` |
| `leadTimeBusinessDays` | `(7, 10)` |
| `priceMarkup` | `1.20` (20%) |
| `availability` | `PartnerAvailability(onVacation = false, unavailableUntil = None, blockedDates = Set.empty)` |
| `contact` | `"partner-uv@example.com"` |

---

## 9. Other Implications

| Topic | Impact | Resolution / Deferral |
|---|---|---|
| **Artwork / prepress files** | Partner needs the artwork file to produce the job | Phase 1: operator manually downloads artwork from the order and emails to partner. Notes field on the `ExternalPartner` step records the transmission status. Automated export is a Phase 2 task. |
| **Customer communication** | Order confirmation email/page must say "external production" | Copy change only — no domain model change required. Deferred to a content/UI update session. |
| **Cancellation / refund** | Once a job is handed to a partner, cancellation windows shorten significantly | Flagged. No domain changes in this phase — operators handle cancellations manually. A `CancellationPolicy` extension is a future concern. |
| **MinimumOrderPrice** | Should the floor apply before or after partner markup? | Markup is applied before setup fees; `MinimumOrderPrice` is applied last (unchanged). Partner markup does not bypass the floor. |
| **Payment timing** | Customer pays the shop; shop settles with partner externally | No domain change. Existing `PaymentStatus` enum covers the customer-facing flow. |
| **Audit trail** | Partner assignment and completion timestamp must be captured | `WorkflowStep.assignedPartner` + `completedAt` provide sufficient audit data. |
| **Multiple `RequiresExternalPartner` matches** | A configuration could theoretically match two rules (e.g., oversized banner *and* spot varnish) | Implementation must detect this case. In Phase 1, if both rules match and their `candidatePartners` differ, raise a validation error (too complex to route to two external partners simultaneously). This case does not arise with the two seed rules (different category/finish predicates). |

---

## 10. Extension Points Reused vs. Added

| Mechanism | Status | Notes |
|---|---|---|
| `CompatibilityRule` sealed enum | **Extended** — new `RequiresExternalPartner` variant | Follows existing ADT pattern; evaluated by `RuleEvaluator` |
| `PricingRule` sealed enum | **Extended** — new `ExternalPartnerMarkup` variant | Follows existing ADT pattern; evaluated by `PriceCalculator` |
| `StationType` enum | **Extended** — new `ExternalPartner` variant | Requires `displayName` and `icon` extensions in companion object |
| `WorkflowStep` case class | **Extended** — two new optional fields (`assignedPartner`, `estimatedCompletionOverride`) | Default `None` preserves all existing call sites |
| `ConfigurationError` enum | **Not changed** — external routing is not an error | A separate `ExternalManufacturingError` enum is added for partner-unavailable cases |
| `TierRestrictionValidator` | **Extended** — new check for external-partner tier restriction | Reuses existing `TierRestrictionViolation` pattern; adds one new variant |
| `CompletionEstimator` | **Extended** — new `estimateExternal(...)` method | Reuses `advanceByWorkingMinutes` and `formatDateTime`; bypasses queue/speed logic |
| `WorkflowGenerator.generate` | **Extended** — new parameters for external rules and partner map | Existing call sites unaffected (new params have defaults) |
| `Validation[E, A]` pattern | **Reused unchanged** | All new errors are accumulated in the same `Validation` chain |
| `LocalizedString` | **Reused unchanged** | Used for `ExternalPartner.name` and `RequiresExternalPartner.reason` |

---

## 11. Verification (post-implementation)

| Test | Location | What it verifies |
|---|---|---|
| External markup applied after multipliers, before setup fees | `PriceCalculatorSpec` | `ExternalPartnerMarkup(1.15)` on a 100-unit order — markup factor appears in the correct pipeline position |
| Partner step replaces in-house stations | `WorkflowGeneratorSpec` | Banner 200×200 cm → workflow contains exactly one `ExternalPartner` step; `LargeFormatPrinter` and `LargeFormatFinishing` steps are absent; `Prepress`, `QualityControl`, `Packaging` are present |
| Prepress / QC / Packaging retained | `WorkflowGeneratorSpec` | Same test as above — assert step count and station types |
| Banner >150 cm validates with `RequiresExternalPartner` | `CompatibilityRuleSpec` | Banner 2000×1000 mm — previously returned `SpecConstraintViolation`; now returns success with `ExternalRoutingRequired` annotation |
| Partner-unavailable error surfaced | `ExternalManufacturingErrorSpec` | All candidate partners on vacation → `PartnerUnavailable` error accumulated in `Validation` result |
| Tier selector blocks Express / Economy | `TierRestrictionValidatorSpec` | Configuration matching `RequiresExternalPartner` + speed `Express` → `ExternalPartnerTierNotAllowed` violation |
| UI smoke test | Manual | Banner at 200×200 cm in configurator: external badge visible, tier selector shows only Standard enabled, ETA reads ~2-week date range |
