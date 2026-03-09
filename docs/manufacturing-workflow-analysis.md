# Manufacturing Workflow Analysis

> Planning document for the production queue / workshop management UI.

## 1. Context

The material-builder system already models the full product lifecycle up to checkout: product configuration (10 categories, 20+ materials, 4 printing methods, 15 finishes, fold/binding options), pricing, basket, and order placement. This document designs what happens **after** an order is placed — the manufacturing workflow that transforms an order into a physical product ready for pickup or shipping.

The target environment is a **small-to-medium print shop** where:
- A single employee may operate multiple station types (e.g., one person runs both the laminator and the cutter)
- The shop may have multiple machines of the same type (e.g., 2 digital printers, 1 large-format printer)
- Employees have machine-specific knowledge (which material is loaded, warm-up state) that affects scheduling efficiency

---

## 2. Manufacturing Stations

Derived from the existing domain model, the production stations map directly to processing steps already encoded in `ProductConfiguration`:

| Station Type | Triggered By | Domain Source |
|---|---|---|
| **Prepress** | Every order | Always required — file check, imposition, plate/RIP prep |
| **Digital Printer** | `PrintingProcessType.Digital` | `PrintingMethod` on configuration |
| **Offset Press** | `PrintingProcessType.Offset` | `PrintingMethod` on configuration |
| **Large Format / UV Inkjet** | `PrintingProcessType.UVCurableInkjet` | `PrintingMethod` on configuration |
| **Letterpress** | `PrintingProcessType.Letterpress` | `PrintingMethod` on configuration |
| **Cutter** | Sheet-based materials, die-cut finishes | `CuttingSurcharge` in pricing, `DieCut`/`ContourCut`/`KissCut` finishes |
| **Laminator** | `Lamination`, `Overlamination`, `SoftTouchCoating` finishes | `FinishType` on `SelectedFinish` |
| **UV Coater** | `UVCoating`, `AqueousCoating`, `Varnish` finishes | `FinishType` on `SelectedFinish` |
| **Embossing / Foil** | `Embossing`, `Debossing`, `FoilStamping`, `Thermography` | `FinishType` decorative category |
| **Folder** | `FoldType` spec present | `SpecKind.FoldType` (Half, Tri, Gate, Accordion, Z, Roll, French, Cross) |
| **Binding** | `BindingMethod` spec present | `SpecKind.BindingMethod` (SaddleStitch, Perfect, Spiral, WireO, Case) |
| **Large Format Finishing** | `Grommets`, `Hem`, `Mounting` finishes | `FinishType` large-format category |
| **Quality Control** | Every order | Always required — final inspection |
| **Packaging & Dispatch** | Every order | Always required — pack + handoff to delivery |

### Prepress — the often-forgotten first station

Before anything prints, someone must:
- Verify uploaded artwork (resolution, bleed, color profile)
- Create imposition layouts (gang multiple items on a sheet)
- Generate print-ready files (RIP for digital, plates for offset)
- Flag issues back to the customer

This is a critical bottleneck in small shops. It should be a first-class station in the workflow, not a side task. Prepress approval often **blocks all downstream stations**, so visibility into its queue is essential.

---

## 3. Workflow Generation from Product Configuration

The key insight is that **the workflow for each order item is fully determinable from its `ProductConfiguration`**. The system already knows every processing step — it uses that information for pricing. The same data drives the manufacturing route.

### 3.1 Workflow Template Derivation

```
ProductConfiguration → List[WorkflowStep]
```

A workflow step sequence for a booklet with matte lamination on the cover:

```
1. Prepress
2. Digital Printer    (Cover component — coated art paper 250gsm)
3. Digital Printer    (Body component — coated art paper 120gsm)
4. Laminator          (Cover — matte lamination, both sides)
5. Cutter             (Both components — sheet cutting)
6. Folder             (Body — saddle stitch prep fold)
7. Binding            (Assembly — saddle stitch)
8. Quality Control
9. Packaging & Dispatch
```

For multi-component products (booklets, calendars), components may have **independent parallel paths** that converge at binding/assembly:

```
         ┌─ Print Cover ─ Laminate Cover ─ Cut Cover ─┐
Prepress ─┤                                            ├─ Binding ─ QC ─ Pack
         └─ Print Body ── Cut Body ── Fold Body ──────┘
```

### 3.2 Proposed Domain Model

```scala
// Workflow is generated per OrderItem (one ProductConfiguration)
case class ManufacturingWorkflow(
  id: WorkflowId,
  orderId: OrderId,
  orderItemIndex: Int,
  steps: List[WorkflowStep],
  status: WorkflowStatus,           // Pending, InProgress, Completed, OnHold, Cancelled
  priority: Priority,
  deadline: Option[Instant],
  createdAt: Instant
)

case class WorkflowStep(
  id: StepId,
  stationType: StationType,
  componentRole: Option[ComponentRole],  // None for cross-component steps (binding, QC, pack)
  dependsOn: Set[StepId],               // DAG — step can start when all dependencies complete
  status: StepStatus,                   // Waiting, Ready, InProgress, Completed, Skipped, Failed
  assignedTo: Option[EmployeeId],
  assignedMachine: Option[MachineId],
  startedAt: Option[Instant],
  completedAt: Option[Instant],
  notes: String
)

enum StationType:
  case Prepress
  case DigitalPrinter
  case OffsetPress
  case LargeFormatPrinter
  case Letterpress
  case Cutter
  case Laminator
  case UVCoater
  case EmbossingFoil
  case Folder
  case Binder
  case LargeFormatFinishing
  case QualityControl
  case Packaging
```

### 3.3 Step dependency rules

| Step | Depends on |
|---|---|
| Prepress | — (entry point) |
| Printing (any) | Prepress |
| Cutting | Printing of same component |
| Lamination / UV coating | Printing of same component (before or after cutting, configurable) |
| Embossing / Foil | Lamination if present, else printing |
| Folding | Cutting of same component |
| Binding | All component steps complete |
| QC | Binding if present, else all finishing steps |
| Packaging | QC |

The dependency model is a **DAG (directed acyclic graph)**, not a linear sequence. A step becomes `Ready` when all its `dependsOn` steps are `Completed`.

---

## 4. Pull vs Push — Recommendation: **Pull Model**

### 4.1 Why pull wins for small print shops

| Aspect | Pull (employee picks from queue) | Push (system assigns to station) |
|---|---|---|
| **Material changeover** | Employee knows what's loaded → batches similar jobs | System needs real-time machine state data to optimize |
| **Multi-station employees** | Employee sees unified queue across their stations | System must track employee-station assignments in real time |
| **Machine breakdowns** | Employee simply stops picking from that station | System must detect failure + reassign |
| **Simplicity** | Queue with filters — straightforward UI | Assignment algorithm + conflict resolution — complex |
| **Employee autonomy** | High — experienced operators make better micro-decisions | Low — rigid, frustrating when wrong |
| **Batching efficiency** | Natural — "I'll do all the lamination jobs while the machine is warm" | Requires explicit batching algorithm |

**Recommendation: Pull model with soft guidance.**

The system shows a prioritized queue. Employees pull work items. The system **suggests** optimal ordering (see Section 5) but never forces it. This respects operator expertise while providing visibility and priority guidance.

### 4.2 Pull model mechanics

1. A `WorkflowStep` becomes `Ready` when all dependencies are met
2. Ready steps appear in the queue for their `StationType`
3. An employee assigned to one or more station types sees all Ready steps for those types
4. Employee picks a step → status becomes `InProgress`, `assignedTo` is set
5. Employee completes the step → status becomes `Completed`, downstream steps re-evaluate readiness
6. If a step fails → status becomes `Failed`, workflow goes `OnHold`, supervisor is notified

### 4.3 Guardrails on the pull model

Pure pull can cause problems (cherry-picking easy jobs, ignoring urgent deadlines). Mitigations:

- **Visual urgency indicators**: color-coded by deadline proximity (green/yellow/orange/red/overdue)
- **Soft locks**: if a step has been Ready for too long without pickup, escalate to supervisor view
- **Batching suggestions**: "3 more jobs use the same material and are Ready" — nudge, not force
- **Audit trail**: every pickup and completion is logged with timestamps for accountability

---

## 5. Priority & Queue Ordering

### 5.1 Sorting factors (in order of weight)

1. **Deadline proximity** — orders with closer deadlines float to the top. Deadline comes from delivery method (Express > Standard > Economy) or a custom date set during order approval.

2. **Priority flag** — manual override set during order approval (Rush, Normal, Low). Rush orders get a visual badge and sort above same-deadline Normal orders.

3. **Order completeness** — if 4 out of 5 steps for an order are done, the remaining step should be prioritized to clear the order. A "nearly done" order sitting in one queue wastes all the work already invested. This is essentially a **shortest remaining processing time** heuristic.

4. **Batch affinity** — jobs that share the same material, finish, or machine setup as the currently running or most recent job get a soft boost. This reduces changeover waste. The system can detect this from `ProductConfiguration` data (same material ID, same finish, same ink configuration).

5. **FIFO within same priority** — all else being equal, older orders go first.

### 5.2 Calculated score

```scala
case class QueueScore(
  deadlineUrgency: Int,    // 0-100 based on hours until deadline
  priorityBoost: Int,      // Rush=30, Normal=0, Low=-10
  completenessBoost: Int,  // 0-20 based on % of order steps done
  batchAffinity: Int,      // 0-15 if matches current machine setup
  ageMinutes: Long         // tiebreaker
)
```

The score is **advisory** — it determines default sort order, but employees can re-sort or filter as they wish.

---

## 6. Order Lifecycle (End-to-End)

```
Customer places order
        │
        ▼
  ┌─────────────┐
  │  Order       │  Status: Placed
  │  Approval    │  Action: Staff reviews artwork, payment, feasibility
  │  Queue       │  Can: Approve, Reject, Request Changes, set Priority/Deadline
  └──────┬──────┘
         │ Approved
         ▼
  ┌─────────────┐
  │  Workflow    │  ManufacturingWorkflow generated from ProductConfiguration
  │  Generated   │  All steps created with dependencies
  └──────┬──────┘
         │
         ▼
  ┌─────────────┐
  │  Production  │  Steps flow through stations (pull model)
  │  In Progress │  Each step: Waiting → Ready → InProgress → Completed
  └──────┬──────┘
         │ All steps complete
         ▼
  ┌─────────────┐
  │  Ready for   │  Packaging complete, awaiting pickup/courier
  │  Dispatch    │
  └──────┬──────┘
         │ Handed off
         ▼
  ┌─────────────┐
  │  Completed   │  Archived with full timeline
  └─────────────┘
```

### 6.1 Order approval — the gatekeeper

Before manufacturing begins, an order must be **approved**. This is where:
- Artwork is reviewed (correct resolution, bleed, color space)
- Payment is verified (especially for `InvoiceOnAccount` and `BankTransferQR`)
- Feasibility is confirmed (material in stock, production capacity)
- Priority and deadline are set or adjusted
- Customer is contacted if changes are needed (artwork issues, material substitution)

The approval queue is itself a station — likely staffed by the shop owner or a prepress operator.

### 6.2 Order-level vs item-level workflows

An `Order` contains a `Basket` with multiple `BasketItem`s. Each item may be a different product (e.g., business cards + a brochure). Options:

**Recommended: one workflow per basket item, grouped by order.**

- Each `BasketItem` gets its own `ManufacturingWorkflow` (its own step sequence)
- The UI groups them under the parent `Order` for context
- Packaging step waits for ALL workflows in the order to complete before dispatch
- This allows items to progress independently through different station paths

---

## 7. Employee & Machine Model

### 7.1 Employee profiles

```scala
case class Employee(
  id: EmployeeId,
  name: String,
  stationCapabilities: Set[StationType],  // which stations they can operate
  isActive: Boolean
)
```

An employee's `stationCapabilities` determines what appears in their queue. A versatile employee who can run the digital printer, laminator, and cutter sees Ready steps for all three types in one unified view, filterable by station type.

### 7.2 Machine registry

```scala
case class Machine(
  id: MachineId,
  name: String,                    // "Konica Minolta C4080" or "Zünd G3 Cutter"
  stationType: StationType,
  status: MachineStatus,           // Online, Offline, Maintenance
  currentSetup: Option[MachineSetup]  // what material/config is currently loaded
)

case class MachineSetup(
  materialId: Option[MaterialId],
  lastJobFinishedAt: Option[Instant],
  notes: String                    // "320gsm glossy loaded, CMYK calibrated"
)
```

Machine tracking is **optional but valuable** — if maintained, it enables:
- Batch affinity scoring (jobs matching current setup get priority boost)
- Capacity visibility (which machines are busy vs idle)
- Maintenance scheduling

For MVP, machines can be omitted — employees just pick jobs and the system tracks which employee did what.

---

## 8. UI Views

### 8.1 Station Queue View (primary operator view)

The main screen for a production employee:

```
┌─────────────────────────────────────────────────────────┐
│  My Stations: [Digital Printer ✓] [Laminator ✓] [Cutter]│  ← filter toggles
├─────────────────────────────────────────────────────────┤
│  Sort: Priority (default) │ Deadline │ Material batch    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  🔴 RUSH  Order #1042 — Business Cards (Main)          │
│  Step: Digital Print │ 350gsm Glossy │ CMYK 4/4         │
│  Deadline: Today 14:00 │ Qty: 500                       │
│  [Start Job]                                            │
│                                                         │
│  🟡  Order #1038 — Brochure (Main)                     │
│  Step: Digital Print │ 170gsm Matte │ CMYK 4/0          │
│  Deadline: Tomorrow │ Qty: 200                          │
│  💡 2 more jobs on same material                        │  ← batch hint
│  [Start Job]                                            │
│                                                         │
│  🟢  Order #1035 — Flyers (Main)                       │
│  Step: Lamination (Matte) │ after printing              │
│  Deadline: Thu │ Qty: 1000                              │
│  [Start Job]                                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

Key features:
- **Station type filter toggles** at the top — employee enables the stations they're currently working
- **Unified queue** across selected station types, sorted by priority score
- **Batch hints** — system detects when multiple Ready jobs share material/finish
- **One-tap start** — claims the job, starts timer
- **In-progress section** at the top showing currently claimed jobs with [Complete] / [Problem] actions

### 8.2 Order Approval View (manager/prepress)

```
┌─────────────────────────────────────────────────────────┐
│  Pending Approval (7)                                   │
├─────────────────────────────────────────────────────────┤
│  Order #1045 — 3 items │ Jan Novák │ Courier Express    │
│  Total: 2,450 CZK │ Payment: Card (confirmed)          │
│  Items: 500× Business Cards, 200× Brochures, 100× Flyers│
│  [Review] [Approve All] [Reject]                        │
│                                                         │
│  Order #1044 — 1 item │ Guest │ Pickup                  │
│  Total: 890 CZK │ Payment: Bank Transfer (⏳ pending)   │
│  Items: 1000× Stickers                                  │
│  ⚠️ Payment not confirmed                               │
│  [Review] [Hold for Payment]                            │
└─────────────────────────────────────────────────────────┘
```

### 8.3 Order Progress View (manager dashboard)

Shows all active orders with a visual pipeline:

```
Order #1042 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Business Cards  [✓ Prepress] [▶ Printing] [○ Cutting] [○ QC] [○ Pack]
  Deadline: Today 14:00  🔴 RUSH

Order #1038 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Brochure        [✓ Prepress] [✓ Printing] [▶ Lamination] [○ Cut] [○ Fold] [○ QC] [○ Pack]
  Deadline: Tomorrow  🟡
```

### 8.4 Analytics View (owner)

- Average time per station type
- Bottleneck identification (which station has the longest queue)
- Employee throughput
- On-time delivery rate
- Popular batching opportunities

---

## 9. Handling Edge Cases

### 9.1 Reprints and rework

When QC fails an item:
- QC marks step as `Failed` with a note (e.g., "color shift on back side")
- Workflow status → `OnHold`
- Manager decides: **reprint** (resets specific steps to `Ready`) or **accept with discount**
- Rework steps get a `isRework: Boolean` flag for tracking waste

### 9.2 Partial order dispatch

Sometimes one item in a multi-item order is delayed. Options:
- **Ship what's ready** — split dispatch, additional shipping cost
- **Hold all** — wait until everything is done (default)
- Let the manager decide per order, configurable as shop default

### 9.3 Order modifications after approval

Customer calls to change quantity or add lamination after approval:
- Manager can modify the order → workflow is regenerated
- Already-completed steps are preserved if the change doesn't affect them
- New or changed steps are inserted into the DAG

### 9.4 Machine breakdown

- Employee marks machine as `Offline`
- In-progress steps on that machine → `Ready` (back to queue for another machine)
- If no alternative machine exists → `OnHold`, supervisor notified

### 9.5 Material out of stock

- Discovered at any point (ideally at approval)
- Workflow → `OnHold`, customer contacted for material substitution or wait
- Steps remain in `Waiting` — they won't pollute anyone's Ready queue

---

## 10. What You Might Be Missing

### 10.1 Imposition and ganging

Small print shops commonly **gang** multiple orders onto one sheet to save material. For example, 4 different business card orders printed on one SRA3 sheet. This means:

- A printing step might serve **multiple workflow items** simultaneously
- The cutter then separates them
- This is a significant efficiency optimization but adds complexity: steps from different orders become linked

**Recommendation for MVP**: skip ganging. Treat each order item independently. Add ganging as a future optimization — it's a prepress concern that can be overlaid on the workflow model later.

### 10.2 Drying / curing time

Some steps have mandatory wait times:
- UV coating needs curing time
- Lamination needs settling time before cutting
- Offset ink needs drying time before finishing

Model this as an optional `minimumWaitAfter: Option[Duration]` on `WorkflowStep`. The next step stays `Waiting` until both the dependency is `Completed` AND the wait time has elapsed. Practically this might just be an informational note shown to employees ("laminated 20 min ago — ready to cut").

### 10.3 Inventory / material stock tracking

The system knows which materials each job needs. Even without full inventory management, a simple stock check at approval time prevents starting jobs that can't be completed.

### 10.4 Outsourcing

Some finishes (e.g., foil stamping, case binding) might be outsourced. Model this as a station type `External` with:
- A shipment-out step and a receipt-back step
- Longer expected duration
- External vendor reference

### 10.5 File management

Each workflow needs associated files:
- Customer artwork (uploaded at order time)
- Prepress output (imposed PDFs, cut marks)
- Proofs (soft proof or hard proof sent to customer)

A simple file attachment per workflow step covers this. Consider linking to cloud storage rather than embedding files.

### 10.6 Customer notifications

Automated emails/SMS at key points:
- Order approved → "Your order is in production"
- QC passed → "Your order is ready for dispatch"
- Dispatched → "Your order is on its way" (with tracking if courier)

### 10.7 Estimated completion time

Based on current queue depth per station and average processing times, the system can estimate when an order will be done. Useful for:
- Setting realistic deadlines at approval
- Customer-facing "estimated ready by" dates
- Identifying capacity problems early

---

## 11. Implementation Plan

### Phase 1 — Core Workflow Engine (domain layer)

Extends the existing pure domain model. No effects, Scala.js compatible.

1. **`ManufacturingWorkflow` model** — `WorkflowId`, `WorkflowStep`, `StationType`, `StepStatus`, `WorkflowStatus` as value objects / enums in `model/`
2. **`WorkflowGenerator`** — pure function: `(ProductConfiguration, ProductSpecifications) => ManufacturingWorkflow`. Derives steps and DAG dependencies from the configuration. Exhaustive match on `PrintingProcessType`, `FinishType`, `SpecKind`.
3. **`WorkflowEngine`** — state transitions: `startStep`, `completeStep`, `failStep`, `skipStep`. Returns `Validation[WorkflowError, ManufacturingWorkflow]`. Enforces DAG constraints (can't start a step whose dependencies aren't met).
4. **`QueueScorer`** — pure priority calculation from deadline, priority flag, order completeness, batch affinity.
5. **Tests** — generate workflows from existing `SampleCatalog` configurations, verify step sequences and DAG correctness.

### Phase 2 — Employee & Machine Model

1. **`Employee` and `Machine` models** in `model/`
2. **`StationQueue` service** — filters Ready steps by station type, sorted by `QueueScorer`
3. **Batch detection** — identify groups of Ready steps with shared material/finish

### Phase 3 — Order Approval

1. **`OrderApproval` model** — approval status, reviewer, notes, artwork check status
2. **Approval → workflow generation** trigger
3. **Priority and deadline assignment** at approval time

### Phase 4 — UI (Laminar)

1. **Station Queue View** — Laminar reactive UI, `Var[List[ReadyStep]]` updated from backend
2. **Order Approval View** — review queue with artwork preview
3. **Order Progress View** — pipeline visualization per order
4. **Employee settings** — station capability toggles

### Phase 5 — Enhancements

1. Estimated completion time calculation
2. Customer notifications
3. Analytics dashboard
4. Drying/curing time tracking
5. External vendor (outsourcing) steps
6. Imposition and ganging support

---

## 12. Key Design Decisions Summary

| Decision | Choice | Rationale |
|---|---|---|
| Pull vs Push | **Pull** | Operator expertise, material awareness, multi-station flexibility |
| Workflow granularity | **Per basket item** | Independent paths, convergence at packaging |
| Step dependencies | **DAG** | Supports parallel component processing (cover + body) |
| Priority model | **Soft scoring** | Guides without forcing — respects operator judgment |
| Machine tracking | **Optional** | Nice-to-have for batch affinity, not required for MVP |
| Ganging | **Deferred** | High complexity, low MVP value |
| Domain purity | **Pure functions** | Consistent with existing architecture, Scala.js compatible |
| Workflow generation | **Derived from ProductConfiguration** | Single source of truth — pricing and manufacturing use same data |
