# Express Manufacturing Analysis

> Analysis of express, standard, and economy manufacturing tiers for the product builder — covering pricing, estimated completion times, capacity constraints, and alternative approaches.

## 1. Context

The product builder already models the full lifecycle from product configuration through pricing to manufacturing. The pricing engine handles 17 rule types, the `WorkflowGenerator` derives production steps from `ProductConfiguration`, and the manufacturing UI tracks orders through a pull-based station queue.

What's **missing** is a customer-facing production speed tier that lets users choose how fast their order is manufactured — and see the price and estimated completion date change in real time inside the product builder.

Today, the `Priority` enum (`Rush`, `Normal`, `Low`) exists but is **internal** — set during order approval by shop staff. The customer has no visibility into it and no control over manufacturing urgency. Similarly, `DeliveryOption` (`CourierExpress`, `CourierStandard`, `CourierEconomy`) covers shipping speed but not production speed.

This analysis designs a **manufacturing speed tier** — exposed as a product parameter in the builder — that integrates with pricing, capacity planning, and estimated completion time.

---

## 2. Manufacturing Speed Tiers

### 2.1 Tier definitions

| Tier | Internal Priority | Target Turnaround | Use Case |
|---|---|---|---|
| **Express** | Rush | Same day / next business day | Urgent conference materials, last-minute event signage |
| **Standard** | Normal | 2–5 business days | Regular orders, steady production flow |
| **Economy** | Low | 5–10 business days | Bulk orders, price-sensitive customers, no urgency |

The turnaround targets are **guidelines**, not guarantees. The actual estimated completion date is computed dynamically (Section 4) and shown to the customer at selection time.

### 2.2 Why three tiers

Two tiers (fast vs slow) create a binary choice that most customers resolve by picking "fast." Three tiers create a meaningful middle ground — Standard is the sensible default, Express is a conscious premium choice, and Economy rewards patience with savings. This is a well-established pattern in logistics (overnight / ground / economy) and SaaS (enterprise / pro / free).

### 2.3 Customer-facing presentation

In the product builder, the manufacturing speed tier appears as a **radio group** (or card selector) in the product parameters section, alongside material, finish, and quantity. Each option shows:

```
┌─────────────────────────────────────────────────────┐
│  ⚡ Express                              +35% price │
│  Estimated completion: Tomorrow, 14:00              │
│  ⚠ Limited availability for quantities > 500        │
├─────────────────────────────────────────────────────┤
│  ● Standard (recommended)                   +0%     │
│  Estimated completion: Thursday, March 27           │
├─────────────────────────────────────────────────────┤
│  🐢 Economy                                −15%     │
│  Estimated completion: Monday, March 31             │
│  Best value for non-urgent orders                   │
└─────────────────────────────────────────────────────┘
```

Key details:
- The **estimated completion date** updates in real time as the customer changes quantity, material, or other parameters that affect production time
- The **price delta** is shown relative to Standard (the baseline), not as an absolute amount
- Availability warnings surface when Express is not feasible for the selected configuration (Section 6)

---

## 3. Pricing Integration

### 3.1 New pricing rule: `ManufacturingSpeedSurcharge`

The pricing engine uses rules-as-data. A new `PricingRule` variant captures the speed tier premium:

```scala
case ManufacturingSpeedSurcharge(
    tier: ManufacturingSpeed,
    multiplier: BigDecimal,           // applied to discounted subtotal
    queueMultiplierThresholds: List[QueueThreshold],  // dynamic surcharges
)

case class QueueThreshold(
    minUtilisation: BigDecimal,       // e.g. 0.70 (70% queue full)
    additionalMultiplier: BigDecimal, // e.g. 0.05 (add 5%)
)
```

### 3.2 Pricing behaviour per tier

| Tier | Base Multiplier | Queue-Sensitive | Setup Fee Affected |
|---|---|---|---|
| **Express** | 1.25–1.50× | Yes — surcharge increases with queue utilisation | No — setup fees are physical costs |
| **Standard** | 1.00× (baseline) | Mildly — small surcharge only at very high utilisation | No |
| **Economy** | 0.80–0.90× | No — price is fixed regardless of queue | No |

**Why queue-sensitive pricing for Express and Standard?**

The point of dynamic pricing is demand management. When the shop is busy:
- Express becomes more expensive → discourages rush orders that would strain capacity
- Standard gets a mild bump → nudges customers toward Economy
- Economy stays flat → always available as the value option

This is directly analogous to surge pricing in ride-sharing or peak-hour electricity tariffs. The customer always sees the final price before committing — there are no hidden adjustments.

### 3.3 Calculation flow (extended)

The existing pricing flow is:

```
subtotal → × quantityMultiplier → + setupFees → max(total, minimumOrderPrice)
```

With manufacturing speed, it becomes:

```
subtotal → × quantityMultiplier → × speedMultiplier(tier, queueUtil) → + setupFees → max(total, minimumOrderPrice)
```

The speed multiplier is applied **after** the quantity discount but **before** setup fees. Setup fees represent physical machine costs that don't change with urgency. The minimum order price still applies as the final floor.

### 3.4 Price breakdown display

`PriceBreakdown` gains a new field:

```scala
speedSurcharge: Option[LineItem],    // "Express manufacturing: +35%"  or  "Economy discount: −15%"
```

This appears as a distinct line item in the price summary, so the customer understands exactly what they're paying for speed.

### 3.5 Busy hour / busy day multipliers

Beyond queue utilisation, certain time windows are predictably busier:

| Factor | Example | Effect |
|---|---|---|
| **Day of week** | Monday & Friday are peak order days | +5–10% Express surcharge |
| **Seasonal peaks** | Pre-Christmas (Nov–Dec), conference season (Sep) | +10–20% Express surcharge |
| **Time of day** | Orders placed after 14:00 may miss same-day Express cutoff | Shifts estimated completion to next day |

These are configured as `BusyPeriodMultiplier` rules with date/time ranges and multiplier values. They stack with queue-based multipliers up to a configurable cap.

```scala
case class BusyPeriodMultiplier(
    dayOfWeek: Option[Set[DayOfWeek]],  // e.g. Set(MONDAY, FRIDAY)
    monthRange: Option[(Month, Month)], // e.g. (NOVEMBER, DECEMBER)
    timeAfter: Option[LocalTime],       // e.g. 14:00
    additionalMultiplier: BigDecimal,   // stacks with base
)
```

---

## 4. Estimated Completion Date

### 4.1 Core algorithm

The estimated completion date is the primary decision factor for customers. It must be **honest** — overpromising and underdelivering destroys trust.

```
estimatedCompletion = orderPlacementTime
    + approvalDelay(tier)
    + productionDuration(config, tier, queueState)
    + bufferTime(tier)
```

Each component:

| Component | Express | Standard | Economy |
|---|---|---|---|
| **Approval delay** | 0–1h (fast-tracked) | 2–4h (normal queue) | 4–8h (batched) |
| **Production duration** | Base time × 1.0 | Base time × 1.0 | Base time × 1.0–1.2 |
| **Queue wait** | Near-zero (queue jump) | Current queue depth × avg step time | Deferred until slack capacity |
| **Buffer** | +1h safety margin | +4h safety margin | +1 business day |

**Approval delay** reflects that Express orders are reviewed immediately (queue jump in the approval queue), while Economy orders can be batched and reviewed in bulk during quieter periods.

**Production duration** is the same across tiers for the actual manufacturing steps — you can't print faster. The difference is **queue wait time** — Express orders jump to the front of every station queue, Standard waits its turn, Economy is explicitly deprioritised.

**Buffer** accounts for unexpected delays. Express has a small buffer because it's already queue-jumping. Economy has a generous buffer because there's no urgency.

### 4.2 Base production time estimation

The base production time for a `ProductConfiguration` is derived from its workflow steps:

```scala
def estimateProductionTime(config: ProductConfiguration, quantity: Int): Duration =
  val steps = WorkflowGenerator.deriveStepTypes(config)
  steps.map(step => estimateStepDuration(step.stationType, quantity)).sum
```

Step duration estimates are configured per station type:

| Station | Base Time | Per-Unit Time | Example (500 units) |
|---|---|---|---|
| Prepress | 30 min | 0 | 30 min |
| Digital Printer | 15 min setup | 0.5s/unit | 19 min |
| Offset Press | 45 min setup | 0.1s/unit | 46 min |
| Laminator | 10 min setup | 0.3s/unit | 13 min |
| Cutter | 5 min setup | 0.2s/unit | 7 min |
| Folder | 5 min setup | 0.3s/unit | 8 min |
| Binder | 10 min setup | 0.5s/unit | 14 min |
| QC | 15 min | 0 | 15 min |
| Packaging | 10 min | 0 | 10 min |

These estimates are **configurable** — the shop owner calibrates them based on their actual equipment speeds.

### 4.3 Queue state impact

The queue state is the critical variable. Estimation requires knowing:

1. **Current queue depth per station** — how many steps are Waiting or Ready
2. **Average processing time per station** — from `AnalyticsService.averageTimePerStation()`
3. **Number of active machines per station** — from `Machine` registry
4. **Where the new order would be inserted** — depends on tier priority

```scala
def estimateQueueWait(
    stationType: StationType,
    tier: ManufacturingSpeed,
    queueState: StationQueueState,
): Duration =
  val position = tier match
    case Express  => 0                          // front of queue
    case Standard => queueState.normalPosition  // after existing Rush/Normal
    case Economy  => queueState.totalDepth      // end of queue
  val avgStepTime = queueState.avgProcessingTime
  val machines = queueState.activeMachineCount.max(1)
  Duration.ofMillis((position * avgStepTime.toMillis) / machines)
```

### 4.4 Working hours and business days

Estimates must respect working hours. A print shop typically operates:

```scala
case class WorkingHours(
    openTime: LocalTime,       // e.g. 07:00
    closeTime: LocalTime,      // e.g. 17:00
    workDays: Set[DayOfWeek],  // e.g. Mon–Fri
    holidays: Set[LocalDate],  // e.g. public holidays
    timezone: ZoneId,
)
```

Key rules:
- **Cutoff time**: If an Express order is placed after a configurable cutoff (e.g., 14:00), same-day completion is not promised — the estimate shifts to next business day morning
- **Overnight**: Production time that extends past `closeTime` rolls over to `openTime` of the next business day
- **Weekends**: Saturday and Sunday (or configured non-work days) are skipped entirely
- **Holidays**: Treated as non-work days; configurable per shop

Example: An order placed at 16:00 on Friday with 4 hours of remaining production time → estimated completion: Monday 11:00 (07:00 + 4h).

### 4.5 Display format

The estimated completion date is shown differently depending on proximity:

| Proximity | Format | Example |
|---|---|---|
| Today | "Today, HH:MM" | "Today, 14:30" |
| Tomorrow | "Tomorrow, HH:MM" | "Tomorrow, 10:00" |
| This week | "Day, HH:MM" | "Thursday, 14:00" |
| Next week+ | "Day, Month DD" | "Monday, March 31" |
| 2+ weeks | "Month DD" | "April 7" |

Always rounded to the nearest 30-minute block — false precision (e.g., "14:17") undermines trust more than it helps.

### 4.6 Confidence indicators

For transparency, show a confidence range:

```
Express:  Tomorrow, 10:00 – 14:00  (high confidence)
Standard: Mar 27 – Mar 28          (medium confidence)
Economy:  Mar 31 – Apr 2           (normal confidence)
```

Express has a narrow range (less uncertainty because it's queue-jumping). Economy has a wider range (it depends on how busy the shop gets in the interim).

---

## 5. Queue Utilisation Model

### 5.1 Defining utilisation

Queue utilisation is a per-station metric:

```scala
case class StationUtilisation(
    stationType: StationType,
    queueDepth: Int,                // Waiting + Ready steps
    inProgressCount: Int,           // Currently being worked on
    machineCount: Int,              // Active machines for this station
    avgProcessingTimeMs: Long,      // Historical average
    estimatedClearTimeMs: Long,     // Time to drain queue at current rate
):
  def utilisationRatio: BigDecimal =
    if machineCount == 0 then BigDecimal(1)  // no machines = fully saturated (disables Express)
    else BigDecimal(queueDepth + inProgressCount) / (machineCount * optimalThroughput)
```

### 5.2 Global utilisation

For pricing and tier availability decisions, a global utilisation score aggregates across all stations:

```scala
def globalUtilisation(stations: List[StationUtilisation]): BigDecimal =
  if stations.isEmpty then BigDecimal(0)
  else stations.map(_.utilisationRatio).max  // bottleneck-driven
```

Using the **maximum** station utilisation (bottleneck) rather than the average is more accurate — if the laminator is at 95% capacity, it doesn't matter that the cutter is idle.

### 5.3 Utilisation thresholds for pricing

| Global Utilisation | Express Multiplier | Standard Multiplier | Economy |
|---|---|---|---|
| 0–50% (quiet) | 1.25× | 1.00× | 0.85× |
| 50–70% (normal) | 1.35× | 1.00× | 0.85× |
| 70–85% (busy) | 1.50× | 1.05× | 0.85× |
| 85–95% (very busy) | 1.75× | 1.10× | 0.85× |
| 95%+ (at capacity) | Unavailable | 1.15× | 0.85× |

Notice: Economy price **never changes** — it's the anchor that customers can always rely on. Express at 95%+ utilisation is simply disabled because it's physically impossible to prioritise when every machine is saturated.

---

## 6. Capacity Constraints and Restrictions

### 6.1 Quantity limits per tier

Not all orders can be Express. Physical constraints limit what's feasible:

| Constraint | Express | Standard | Economy |
|---|---|---|---|
| **Max quantity** | 500–2,000 (depends on product) | 5,000–10,000 | No limit |
| **Max components** | 2 (e.g., cover + body) | 4 | No limit |
| **Multi-material** | Restricted (single material preferred) | Allowed | Allowed |
| **Special finishes** | Limited to 1 finish type | Up to 3 | No limit |

These are configured per product category:

```scala
case class TierRestriction(
    categoryId: CategoryId,
    tier: ManufacturingSpeed,
    maxQuantity: Option[Int],
    maxComponents: Option[Int],
    maxFinishes: Option[Int],
    allowedFinishTypes: Option[Set[FinishType]],
    blockedMaterials: Option[Set[MaterialId]],
)
```

### 6.2 Station-specific constraints

Some stations have hard capacity limits that affect Express feasibility:

| Station | Constraint | Impact |
|---|---|---|
| **Offset Press** | Plate making takes 2–4 hours | Express not available for offset jobs |
| **Binding (Perfect)** | Glue drying time: 4–8 hours | Perfect binding cannot be Express same-day |
| **Embossing/Foil** | Die creation: 1–3 days for custom dies | Express only with stock dies |
| **Large Format** | Limited machine count (often 1) | Express capped at lower quantities |

### 6.3 Validation in the product builder

When the customer selects Express but their configuration violates a constraint, the UI should:

1. **Show the constraint** — "Express not available for quantities over 2,000. Consider Standard."
2. **Suggest alternatives** — "Reduce quantity to 2,000 for Express, or choose Standard for 5,000 units"
3. **Auto-fallback** — If Express becomes unavailable due to a configuration change (e.g., adding perfect binding), auto-select Standard with a notification

This uses the existing `Validation[E, A]` error accumulation pattern — `ManufacturingSpeedError` variants join the validation chain.

---

## 7. Domain Model Additions

### 7.1 Manufacturing speed enum

```scala
enum ManufacturingSpeed:
  case Express, Standard, Economy

object ManufacturingSpeed:
  extension (s: ManufacturingSpeed) def toPriority: Priority = s match
    case Express  => Priority.Rush
    case Standard => Priority.Normal
    case Economy  => Priority.Low
    
  extension (s: ManufacturingSpeed) def displayName: LocalizedString = ...
```

### 7.2 Station time estimate configuration

```scala
case class StationTimeEstimate(
    stationType: StationType,
    baseTimeMinutes: Int,        // fixed setup/overhead
    perUnitSeconds: BigDecimal,  // scales with quantity
    maxParallelUnits: Int,       // for batch-capable stations
)
```

### 7.3 Working hours configuration

```scala
case class ShopSchedule(
    workingHours: WorkingHours,
    expressCutoffTime: LocalTime,   // e.g. 14:00 — no same-day Express after this
    standardCutoffTime: LocalTime,  // e.g. 16:00 — orders after this start next day
)
```

### 7.4 Integration with ProductConfiguration

`ProductSpecifications` gains a new `SpecKind`:

```scala
case class ManufacturingSpeedSpec(speed: ManufacturingSpeed) extends SpecKind
```

This keeps the speed tier as a product parameter — selectable in the builder, persisted with the configuration, and available to both pricing and workflow generation.

### 7.5 Integration with WorkflowGenerator

When generating a `ManufacturingWorkflow`, the speed tier maps to:

```scala
val priority = config.specifications.manufacturingSpeed.toPriority
val deadline = estimateCompletionDate(config, tier, queueState, schedule)
```

The workflow is created with the appropriate `priority` and `deadline` derived from the tier, not manually set by staff.

---

## 8. Queue Scorer Extension

The existing `QueueScorer` already uses `Priority` for ordering. With manufacturing speed tiers, the priority is set automatically from the tier. No changes to the scorer itself are needed — the existing `priorityBoost` (Rush=30, Normal=0, Low=-10) and `deadlineUrgency` scoring handle the tier differentiation.

However, two enhancements improve the system:

### 8.1 Express lane guarantee

For Express orders, the scorer should guarantee they are **always** ahead of Standard/Economy orders at any given station, even if a Standard order is "nearly done" (high completeness boost). This prevents the edge case where a Standard order with 90% completeness bumps an Express order.

```scala
def score(...): QueueScore =
  val base = ... // existing calculation
  if tier == Express then base.copy(priorityBoost = base.priorityBoost + 50) // overwhelming priority
  else base
```

### 8.2 Economy batching bonus

Economy orders should be **batched together** more aggressively. If multiple Economy orders use the same material, they get an extra affinity bonus to encourage processing them as a group during quiet periods:

```scala
if tier == Economy then base.copy(batchAffinity = base.batchAffinity + 10)
```

---

## 9. Analytics Extension

### 9.1 Tier-based metrics

`AnalyticsService` should track performance per tier:

| Metric | Purpose |
|---|---|
| **Average turnaround by tier** | Are we meeting tier promises? |
| **On-time rate by tier** | Are we meeting tier promises? Configurable targets (suggested: Express ≥ 95%, Standard ≥ 90%, Economy ≥ 85%) |
| **Revenue split by tier** | What % of revenue comes from Express premiums? |
| **Tier selection distribution** | Are customers choosing Express? Is Economy cannibalising? |
| **Queue utilisation over time** | Heatmap of busy hours/days for pricing calibration |

### 9.2 Feedback loop for estimates

The system should track **estimated vs actual completion times** per tier and automatically adjust buffer times and station time estimates. If Express orders are consistently completing 2 hours after the estimate, the buffer should increase.

---

## 10. Alternative Approaches

### 10.1 Deadline-based pricing (instead of fixed tiers)

Instead of three fixed tiers, let the customer **pick a delivery date** from a calendar. The price adjusts dynamically based on how soon the date is:

```
┌──────────────────────────────────────────────────┐
│  When do you need this?                          │
│                                                  │
│  📅  Mar 25 (Tue)  ............  +45%  ⚡ Rush   │
│  📅  Mar 26 (Wed)  ............  +20%            │
│  📅  Mar 27 (Thu)  ............  +0%   ● Normal  │
│  📅  Mar 28 (Fri)  ............  −5%             │
│  📅  Mar 31 (Mon)  ............  −15%  🐢 Value  │
│  📅  Apr 2  (Wed)  ............  −20%            │
│  📅  Later         ............  −25%  Best deal │
└──────────────────────────────────────────────────┘
```

**Pros:**
- More granular — customer picks the exact day that works for them
- Natural visual representation — calendar or slider
- Continuous pricing curve — no arbitrary tier boundaries
- Customers feel more in control

**Cons:**
- More complex to implement — pricing is a continuous function, not discrete rules
- Harder to communicate simply ("What's your rush tier?" vs "pick a date")
- Date commitments are harder to honour than tier promises
- May encourage haggling ("What if I pick Thursday instead of Wednesday?")

**Verdict:** Could be offered as an advanced option alongside the three-tier system. The tiers map to specific calendar dates anyway, but the tiers are the default UX and the calendar is a "choose your own date" power-user feature.

### 10.2 Auction-based priority

Let customers **bid** for Express slots when the queue is busy. The top N bidders get Express treatment; others fall back to Standard.

**Pros:** True market-clearing price; maximises revenue during peaks.
**Cons:** Unpredictable pricing; complex UX; fairness concerns; bad for repeat customers.

**Verdict:** Interesting for very high-volume shops but too complex and potentially off-putting for a small-to-medium print shop.

### 10.3 Subscription tiers

Offer monthly subscriptions that include manufacturing speed benefits:

| Plan | Monthly Fee | Express Orders | Standard Discount |
|---|---|---|---|
| **Basic** | Free | Pay per order | 0% |
| **Pro** | 50 EUR/month | 5 included | −5% |
| **Enterprise** | 200 EUR/month | Unlimited | −10% |

**Pros:** Predictable revenue; customer loyalty; reduces per-order pricing complexity.
**Cons:** Requires customer accounts and billing infrastructure; doesn't solve the capacity management problem directly.

**Verdict:** Good future addition on top of per-order tiers. Subscriptions can grant tier discounts (e.g., Pro members get Express at Standard price for 5 orders/month).

### 10.4 Batch window pricing

Instead of per-order tiers, offer **batch windows** — fixed production runs at scheduled times:

```
┌──────────────────────────────────────────────────┐
│  Next batch runs:                                │
│                                                  │
│  🏭 Today 14:00   (closes in 2h)  ..  −10%      │
│  🏭 Tomorrow 08:00               ..   −15%      │
│  🏭 Friday 08:00                 ..   −20%      │
└──────────────────────────────────────────────────┘
```

Customers who submit before the batch deadline get a discount. The shop batches similar jobs together for efficiency.

**Pros:** Optimises production scheduling; clear deadlines; encourages batching.
**Cons:** Less flexible; requires batch scheduling infrastructure; doesn't work for Express.

**Verdict:** Excellent for Economy tier — Economy orders could be explicitly batched into scheduled production windows, improving shop efficiency and giving customers a predictable (and cheap) option.

### 10.5 Split manufacturing

For large orders, allow splitting across tiers:

```
5,000 business cards:
  → 500 Express (ready tomorrow)    +35%
  → 4,500 Economy (ready next week) −15%
```

**Pros:** Customer gets urgent partial delivery plus cost savings on the bulk.
**Cons:** Two separate production runs; more complex order tracking; packaging logistics.

**Verdict:** Valuable for large orders. Implementation requires per-line-item tier assignment rather than per-order tier.

### 10.6 Guaranteed vs best-effort Express

Offer two Express variants:

| Option | Price | Guarantee |
|---|---|---|
| **Guaranteed Express** | +50% | Full refund or next order free if deadline missed |
| **Best-effort Express** | +25% | Prioritised but no financial guarantee |

**Pros:** Customers who truly need it pay for certainty; casual "Express" users save money.
**Cons:** Guaranteed Express requires financial risk management and very accurate estimation.

**Verdict:** A good differentiation for shops with reliable throughput data. Start with best-effort; add guaranteed tier once estimation accuracy is proven via analytics.

---

## 11. Notification and Communication

### 11.1 Customer notifications

| Event | Notification |
|---|---|
| Order placed | Confirmation with estimated completion date and selected tier |
| Production started | "Your order is now in production" |
| Ahead of schedule | "Great news — your order may be ready earlier than expected" |
| Delay detected | "Your order is delayed; new estimate: [date]. We apologize for the inconvenience." |
| Ready for pickup/shipping | "Your order is ready!" |

### 11.2 Internal alerts

| Event | Alert |
|---|---|
| Express order placed | Immediate notification to approval queue (push, not pull) |
| Queue utilisation > 85% | Dashboard warning: "High load — consider pausing Express acceptance" |
| Express deadline at risk | Escalation alert to supervisor: "Express order #X may miss deadline" |
| Economy batch window approaching | "Economy batch closes in 1h — N orders queued" |

---

## 12. Configuration and Admin

### 12.1 Shop owner settings

The manufacturing speed system requires configurable parameters. These should live in a **shop configuration** section of the admin UI:

| Setting | Default | Purpose |
|---|---|---|
| Working hours | 07:00–17:00, Mon–Fri | Basis for all time calculations |
| Express cutoff time | 14:00 | Latest time for same-day Express |
| Express base multiplier | 1.35× | Price premium for Express |
| Economy base multiplier | 0.85× | Discount for Economy |
| Queue threshold (busy) | 70% | When Express surcharge increases |
| Queue threshold (critical) | 95% | When Express is disabled |
| Express max quantity (per category) | Varies | Physical production limit |
| Express surcharge cap | 2.0× | Maximum Express multiplier (protects customers) |
| Holiday calendar | (empty) | Non-working days |
| Station time estimates | (per station) | Calibrated processing times |

### 12.2 Per-category overrides

Some product categories may have different tier characteristics:

```scala
case class CategoryTierConfig(
    categoryId: CategoryId,
    expressAvailable: Boolean,
    expressMaxQuantity: Option[Int],
    expressMultiplierOverride: Option[BigDecimal],
    economyMultiplierOverride: Option[BigDecimal],
    additionalLeadTimeDays: Int,  // e.g., hardcover books need +2 days base
)
```

---

## 13. Edge Cases

| Scenario | Behaviour |
|---|---|
| **Order placed 5 minutes before closing** | Express estimate starts from next business day morning |
| **Friday afternoon Express order** | Estimated completion: Monday (weekend is not working time) |
| **Express order during holiday week** | Express may be unavailable; Standard estimate extends over holidays |
| **Quantity changed from 100 to 5,000** | Express may become unavailable; UI shows warning and suggests Standard |
| **All machines for a station offline** | Express and Standard disabled for affected products; Economy estimate extended |
| **Customer selects Express, then adds perfect binding** | Express becomes unavailable (binding cure time); auto-fallback to Standard with notification |
| **Queue drops from 90% to 30% while customer is configuring** | Price and estimates update in real time (debounced, not instant) |
| **Two Express orders compete for same slot** | Both are honoured (FIFO within Express); estimate adjusts for second order |
| **Economy order already in production, Express order arrives** | Express cannot preempt in-progress steps; it queues ahead of other Waiting steps |

---

## 14. Summary

The manufacturing speed tier system adds a customer-facing dimension to what is currently an internal priority system. It connects pricing (higher price for faster service), capacity management (queue-aware dynamic pricing that naturally manages demand), and delivery promises (honest, working-hours-aware completion estimates).

The three-tier model (Express / Standard / Economy) provides a clean, understandable UX while the underlying mechanics — queue utilisation tracking, busy period multipliers, station-specific time estimates — ensure accuracy and fairness.

The key design principles:
1. **Transparency** — customers see the estimated date and price impact before committing
2. **Honesty** — estimates respect working hours, holidays, and real queue state
3. **Demand management** — dynamic pricing steers demand toward Economy during peak times
4. **Graceful degradation** — Express is disabled when it can't be fulfilled, not overpromised
5. **Data-driven configuration** — all multipliers, thresholds, and time estimates are shop-configurable rules, not hardcoded logic

---

## 15. Implementation Status

*Last updated: March 2026*

This section compares the features described in this analysis with what has been implemented, identifies gaps, and proposes a roadmap for remaining work.

### 15.1 Completed features ✅

| # | Feature | Spec Section | Implementation | Status |
|---|---|---|---|---|
| 1 | **ManufacturingSpeed enum** (Express, Standard, Economy) | §7.1 | `manufacturing.scala` — enum with `toPriority`, `displayName(lang)`, `icon` extensions | ✅ Done |
| 2 | **Priority mapping** (Express→Rush, Standard→Normal, Economy→Low) | §7.1 | `ManufacturingSpeed.toPriority` extension method | ✅ Done |
| 3 | **ManufacturingSpeedSurcharge pricing rule** | §3.1 | `PricingRule.ManufacturingSpeedSurcharge` with tier, multiplier, queue thresholds | ✅ Done |
| 4 | **QueueThreshold type** | §3.1 | `QueueThreshold(minUtilisation, additionalMultiplier)` in separate file | ✅ Done |
| 5 | **BusyPeriodMultiplier type** | §3.5 | `BusyPeriodMultiplier(dayOfWeek, monthRange, timeAfter, additionalMultiplier)` | ✅ Done |
| 6 | **PricingContext** (dynamic pricing input) | §3.5, §5 | `PricingContext(globalUtilisation, busyPeriodMultipliers, currentTimeMillis, expressSurchargeCap)` | ✅ Done |
| 7 | **Speed surcharge in PriceCalculator** | §3.3 | `computeSpeedSurcharge()` — applied after quantity discount, before setup fees; queue + busy period adjustments; Economy fixed; cap at `expressSurchargeCap` | ✅ Done |
| 8 | **PriceBreakdown.speedSurcharge field** | §3.4 | `speedSurcharge: Option[LineItem]` with label showing percentage | ✅ Done |
| 9 | **UtilisationCalculator service** | §5.1–5.3 | `computeGlobalUtilisation` (bottleneck/max), `computeEffectiveMultiplier`, `isExpressAvailable` (95% threshold), `buildPricingContext` | ✅ Done |
| 10 | **StationUtilisation model** | §5.1 | `StationUtilisation` with `utilisationRatio` (optimal queue = 8 per machine) | ✅ Done |
| 11 | **CompletionEstimator service** | §4.1–4.5 | `estimate()` with tier-aware approval delays, queue positioning, buffer times, working-hours rollover, weekend/holiday skipping, 30-min rounding, display formatting | ✅ Done |
| 12 | **StationTimeEstimate configuration** | §7.2 | `StationTimeEstimate(stationType, baseTimeMinutes, perUnitSeconds, maxParallelUnits)` | ✅ Done |
| 13 | **ShopSchedule and WorkingHours** | §7.3, §4.4 | `ShopSchedule(workingHours, expressCutoffTime, standardCutoffTime)` with `WorkingHours(openTime, closeTime, workDays, holidays)` | ✅ Done |
| 14 | **ManufacturingSpeedSpec** | §7.4 | `SpecValue.ManufacturingSpeedSpec(speed)` integrated into `ProductSpecifications` via `SpecKind.ManufacturingSpeed` | ✅ Done |
| 15 | **Sample pricelists with speed rules** | §5.3 | Express (1.35×), Standard (1.00×), Economy (0.85×) with queue thresholds — present in USD, CZK, and CZK Sheet pricelists | ✅ Done |
| 16 | **UI radio card selector** | §2.3 | Radio card layout with icon, tier name, price delta, delivery estimate, description — bilingual EN/CS | ✅ Done |
| 17 | **Price preview integration** | §3.4 | Speed surcharge shown as line item in price breakdown display | ✅ Done |
| 18 | **JSON codecs** | — | `JsonEncoder`/`JsonDecoder` for `ManufacturingSpeed`, `QueueThreshold`, `BusyPeriodMultiplier`, `StationUtilisation`, `PricingContext` in `DomainCodecs.scala` | ✅ Done |
| 19 | **QueueScorer speed enhancements** | §8.1, §8.2 | Express lane priority boost (+50), Economy batching affinity | ✅ Done |
| 20 | **AnalyticsService tier metrics** | §9.1 | Tier-based metrics collection in `AnalyticsService` | ✅ Done |
| 21 | **Comprehensive test suite** | — | 684-line `ExpressManufacturingSpec.scala` covering pricing, completion estimation, utilisation, queue scoring, and analytics | ✅ Done |
| 22 | **Pricelist editor support** | — | Exhaustive pattern matches for `ManufacturingSpeedSurcharge` in `PricelistEditorView.scala` | ✅ Done |

### 15.2 Recently completed features (Phase A+B+C partial)

These features were implemented in the second batch, completing Phases A, B, and the admin portion of Phase C.

| # | Feature | Spec Section | Implementation | Status |
|---|---|---|---|---|
| 1 | **Live completion date display in speed cards** | §2.3, §4 | `CompletionEstimator.estimate()` wired to UI speed cards via `completionEstimate(speed)` signal. Shows dynamic "Tomorrow, 14:00" based on production time, queue wait, approval delay, buffer, and working hours. Falls back to static text when no config exists. | ✅ Done |
| 2 | **Real-time queue state integration** | §5, §4.3 | Simulated `StationUtilisation` data in ProductBuilderViewModel. `UtilisationCalculator.buildPricingContext()` constructs real `PricingContext`. `calculateWithContext()` replaces `calculate()` for all pricing. | ✅ Done |
| 3 | **Express availability check in UI** | §6.3 | `expressAvailable` signal uses `UtilisationCalculator.isExpressAvailable()`. When utilisation ≥ 95%, Express card is greyed out with `.speed-tier-card--disabled` CSS and warning message. | ✅ Done |
| 4 | **Capacity constraint validation** | §6.1–6.2 | `TierRestrictionValidator` service + `SampleTierRestrictions` with per-category Express limits (2000 for standard products, 500 for large format). Validates quantity, binding method (perfect/case binding blocked for Express), and blocked materials. | ✅ Done |
| 5 | **Dynamic pricing with real busy periods** | §3.5 | `BusyPeriodFilter.filterActive()` filters `SampleManufacturing.busyPeriodMultipliers` by current day-of-week, month range, and time-of-day. Active periods feed into `PricingContext`. Monday/Friday +5%, Nov–Dec +10%, September +5% all configured. | ✅ Done |
| 6 | **Admin configuration UI** | §12.1–12.2 | `ManufacturingSettingsView` in Manufacturing → Settings route (🔧). Editable working hours, cutoff times, pricing multipliers, Express surcharge cap, queue thresholds, busy period display, station time estimate table, and holiday calendar with add/remove. | ✅ Done |

### 15.3 Remaining features — next implementation phase

| # | Feature | Spec Section | What's Missing | Effort |
|---|---|---|---|---|
| 1 | **Analytics dashboard for tier metrics** | §9.1 | `AnalyticsService` collects tier-based metrics but there is no UI view to display them. Needs: average turnaround by tier, on-time rate, revenue split, tier selection distribution, queue utilisation heatmap. | Medium |
| 2 | **Customer notifications** | §11.1 | No notification system. Needs: order confirmation with tier and estimate, production started, ahead of schedule, delay detected, ready for pickup. Requires notification infrastructure (email/push/in-app). | Large |
| 3 | **Confidence range display** | §4.6 | `CompletionEstimate` returns `earliestCompletion` and `latestCompletion` but the UI does not show the range. Spec calls for "Tomorrow, 10:00 – 14:00 (high confidence)" format. | Small |
| 4 | **Per-category tier overrides** | §12.2 | `CategoryTierConfig` type is specified but not implemented. Some product categories need different Express multipliers, max quantities, or additional lead time. | Medium |
| 5 | **Settings persistence** | — | Admin settings are currently in-memory `Var`s. Needs localStorage or backend persistence so settings survive page reload. | Small |
| 6 | **Live station data feed** | §5 | Station utilisation is currently simulated with static data. Needs WebSocket or polling backend integration for real metrics. | Medium–Large |

### 15.4 Distant future — nice-to-have features

These features are discussed in the analysis as alternative or advanced approaches. They add significant value but require substantial new infrastructure and are not blockers for the core tier system.

| # | Feature | Spec Section | Description | Complexity |
|---|---|---|---|---|
| 1 | **Deadline-based calendar pricing** | §10.1 | Let customers pick a specific delivery date from a calendar; price adjusts on a continuous curve. Offered alongside tiers as a power-user option. | High |
| 2 | **Split manufacturing** | §10.5 | Split a large order across tiers (e.g., 500 Express + 4,500 Economy). Requires per-line-item tier assignment and multiple production runs. | High |
| 3 | **Guaranteed vs best-effort Express** | §10.6 | Two Express variants: guaranteed (+50%, refund if late) vs best-effort (+25%, priority but no guarantee). Requires financial risk management and estimation accuracy tracking. | High |
| 4 | **Subscription tiers** | §10.3 | Monthly plans with included Express orders and Standard discounts (Basic/Pro/Enterprise). Requires customer accounts and billing infrastructure. | Very High |
| 5 | **Economy batch window pricing** | §10.4 | Scheduled production windows for Economy orders ("Next batch: Friday 08:00, −20%"). Optimises shop scheduling and gives customers clear deadlines. | Medium |
| 6 | **Estimation feedback loop** | §9.2 | Track estimated vs actual completion times per tier; auto-adjust buffer times and station estimates. Self-calibrating estimation accuracy. | Medium |
| 7 | **Auction-based priority** | §10.2 | Customers bid for Express slots during peak demand. True market-clearing price. Complex UX, fairness concerns. Best for very high-volume shops. | Very High |
| 8 | **Internal alerts system** | §11.2 | Dashboard alerts: Express order placed (push notification), queue > 85% (capacity warning), Express deadline at risk (escalation), Economy batch closing. | Medium |
| 9 | **Real-time estimate updates** | §13 | Estimates update in real time as queue state changes while customer is configuring. Debounced, not instant. Requires WebSocket or polling for queue metrics. | Medium–High |

### 15.5 Recommended next steps

**Phase D — Analytics and polish (next sprint)**
1. Build analytics dashboard for tier performance metrics
2. Display confidence range alongside point estimate in speed cards
3. Persist admin settings to localStorage

**Phase E — Infrastructure**
4. Notification system for order lifecycle events
5. Live station data feed (WebSocket/polling)
6. Per-category tier overrides with admin UI

**Phase F — Advanced features (future roadmap)**
7. Deadline-based calendar pricing (power-user feature alongside tiers)
8. Split manufacturing for large orders
9. Estimation feedback loop for self-calibration
