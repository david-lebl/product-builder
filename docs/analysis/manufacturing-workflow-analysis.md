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

### Design principle: simplified linear workflow presentation

Although the underlying step model is a **DAG**, the UI should present workflow progress as a **linear sequence** wherever possible. For the majority of products a linear step chain is accurate enough and far easier to understand at a glance. DAG branching (e.g., parallel cover/body paths) is collapsed into a single progress bar per item and expanded only on demand in the detail side panel. Keep the UI simple first; complexity is exposed progressively.

### Shared UI-framework components

All table-based views share a **universal `SplitTableView` component** defined in `modules/ui-framework` (`mpbuilder.uikit`). This component is domain-agnostic and provides:

- **Sortable data table** — column definitions with `header`, `accessor`, `sortable`, `width`; client-side multi-column sort
- **Search field** — full-text filter across all string-valued columns, debounced
- **Filter bar** — pluggable filter chips/dropdowns above the table; multi-select choice boxes for high-cardinality filters
- **Side panel** — opens horizontally (default) or vertically on narrow screens when a row is selected; renders an arbitrary `HtmlElement`; width is user-resizable
- **Row actions** — inline action buttons in the last column; configurable per row

This component must be implemented before any of the manufacturing views below, and is reused across Station Queue, Order Approval, Order Progress, and future views.

---

### 8.1 Dashboard View (landing page for shop staff)

The first screen an employee or manager sees. Gives an at-a-glance status of the whole shop.

**Summary cards (top row)**
| Card | Content |
|---|---|
| Awaiting Approval | Count of orders in Placed/PendingChanges status |
| In Production | Count of workflows currently InProgress |
| Ready for Dispatch | Count of orders where all workflows are complete |
| Overdue | Count of orders past their deadline |
| Today's completions | Count of orders completed today |

**Station status strip**
A compact row of station tiles (one per `StationType`). Each tile shows:
- Station name
- Queue depth (Ready steps count)
- Whether any machine for that type is currently InProgress
- Color indicator: green (idle, queue empty), blue (working), orange (queue > threshold), red (machine offline)

**Recent orders table** (last ~20 orders, any status)
Columns: Order ID, Customer, Items, Total, Status, Last updated. Clicking a row opens it in the Order Progress detail. No full-text search needed here — it's a recency feed, not a search surface.

**My in-progress jobs** (only shown when logged in as an employee)
A short list of steps the current user has claimed but not yet completed, with [Complete] / [Problem] inline actions. Ensures an employee always sees their own open work without navigating away.

---

### 8.2 Station Queue View (primary operator view)

The main working screen for production staff. Implemented using `SplitTableView`.

**Filter bar**
- **Station type** — multi-select; defaults to the employee's own `stationCapabilities`
- **Status** — multi-select: Ready, InProgress (my jobs first)
- **Priority** — multi-select: Rush, Normal, Low
- **Deadline** — choice: All, Today, Tomorrow, This week, Overdue
- **Search** — full-text over Order ID, customer name, product name, material name

**Table columns** (sortable unless noted)

| Column | Notes |
|---|---|
| Priority | Color-coded badge (Rush 🔴 / Normal 🟡 / Low 🟢); sort default |
| Order ID | Clickable; opens detail panel |
| Order # | Sequential human order number |
| Customer | Name and contact shortcut |
| Product | Product name + short spec description (material, ink config) |
| Current station | Station type for this step |
| Status | Step status with timestamp of last change |
| Deadline | Relative ("Today 14:00", "Tomorrow", "Overdue") |
| Actions | [Start] / [Complete] / [Problem] depending on step status |

**Side panel** (opens when a row is selected)

The side panel shows full order detail without leaving the queue view:

- **Order header** — order ID, customer name, contact info, delivery method, total
- **Files for printing** — list of attached files (customer artwork, prepress output, proofs) with download/preview links; upload action for prepress staff
- **Workflow progress** — linearised step chain for each basket item: `[✓ Prepress] [▶ Printing] [○ Cutting] [○ QC] [○ Pack]`; expand to full DAG on demand
- **Workflow timeline** — chronological event log: step started, completed, who, when, notes
- **Related items** — other basket items in the same order, each with their own mini-progress bar
- **Notes** — free-text notes added at approval or during production; add-note inline action

**Batch hint row**
When ≥ 2 Ready steps in the current filtered view share the same material, the table inserts a soft divider: `💡 3 jobs on 170gsm Matte Art — consider batching`.

**In-progress section**
Steps the current employee has claimed appear in a pinned top section above the main sorted list, with [Complete] / [Problem] actions prominent.

---

### 8.3 Order Approval View (manager / prepress)

Implemented using `SplitTableView`. The approval queue also doubles as the entry point for creating **in-house orders** on behalf of customers who request via phone or email.

**Filter bar**
- **Status** — multi-select: Placed, Pending Changes, On Hold, Rejected (defaults to Placed + Pending Changes)
- **Date range** — choice: Today, Yesterday, This week, Last week, This month, Custom range
- **Payment status** — multi-select: Confirmed, Pending, Failed
- **Delivery method** — multi-select: Pickup, Courier Standard, Courier Express
- **Search** — full-text over Order ID, customer name, item product name

**Table columns**

| Column | Notes |
|---|---|
| Order ID | |
| Date | Order placement date/time |
| Customer | Name; "(Guest)" for unauthenticated |
| Items | Count + summary, e.g. "3 items: 500× Cards, 200× Brochures…" |
| Total | Formatted currency |
| Payment | Status badge: Confirmed ✓ / Pending ⏳ / Failed ✗ |
| Delivery | Method + address shortcut |
| Status | Approval status badge |
| Actions | [Review] [Approve] [Reject] [Hold] |

**Side panel**
Full order detail: all basket items with product specs, pricing breakdown per item, uploaded artwork thumbnails with file check flags (resolution, bleed, color profile), customer contact, payment reference, delivery info, internal notes.

**Creating an in-house order**
An **[+ New Order]** button (top-right of the view) opens a modal/drawer overlaying the current view. Inside:
- Search or select an existing customer, or enter contact details for a new one
- The existing **product builder** is embedded (or a simplified equivalent in the future) to configure and price the product
- On confirm, the order is created with status `Placed` and appears in the approval queue immediately, ready for standard approval flow

---

### 8.4 Order Progress View (manager / fulfilment)

Shows all active orders with workflow progress. Also the primary screen for **order completion, quality check, packaging, and dispatch**.

Implemented using `SplitTableView` with the same filter pattern.

**Filter bar**
- **Workflow status** — multi-select: In Production, Ready for Dispatch, On Hold, Completed
- **Date range** — same presets as approval view
- **Priority** — multi-select
- **Search** — full-text

**Table columns**

| Column | Notes |
|---|---|
| Order ID | |
| Customer | |
| Items | Count |
| Progress | Mini progress bar: completed steps / total steps |
| Current bottleneck | Station where the oldest Ready step is waiting |
| Status | Workflow aggregate status |
| Deadline | Relative with urgency color |
| Actions | Context-sensitive |

**Side panel — fulfilment workflow**

When an order is **Ready for Dispatch**, the side panel presents a structured fulfilment checklist:

1. **Collect items** — checklist of basket items; each can be marked as physically collected and verified against the order
2. **Quality check sign-off** — final QC checkbox with employee signature, notes, and optional photo attachment
3. **Package** — packaging type selection (box, envelope, roll); dimensions/weight input for shipping calculation
4. **Order delivery** — integrated delivery service selection (or manual courier entry); auto-fill address from order; generate shipping label
5. **Create delivery document / invoice** — generate a printable delivery note and/or invoice PDF; fields pre-filled from order and customer data; invoice number assigned; status updated to Dispatched
6. **Dispatch confirmation** — mark as dispatched, enter tracking number if applicable; triggers customer notification

**Order progress visualization**
Linearised per-item step chains (same as station queue side panel), but oriented horizontally across the full panel width for easy scanning. For multi-item orders, each item gets its own row.

---

### 8.5 Analytics View (owner)

- Average time per station type
- Bottleneck identification (which station has the longest queue)
- Employee throughput
- On-time delivery rate
- Popular batching opportunities

---

### 8.6 Invoices & Customer Management (future)

**Invoice view**
- Table of all generated invoices (order ID, customer, date, amount, status: Draft / Sent / Paid / Overdue)
- Filter by status, date range, customer
- Create/edit invoice, download PDF, mark as paid, send reminder
- Linked back to the originating order in Order Progress View

**Customer management view**
- Table of all customers (name, email, phone, order count, total spend, last order)
- Search + filter (active/inactive, date of last order)
- Customer detail: contact info, order history, notes, price tier / discount flag
- Create/edit customer record
- Used by the in-house order creation flow

---

### 8.7 Station Configuration View (future)

Allows a manager or admin to configure the station setup for the shop:

- List of registered machines with their type, name, and current status
- Enable / disable individual stations (e.g., take a machine offline for maintenance)
- Assign employees to station types
- Set capacity thresholds for queue-depth warnings on the dashboard
- Add / remove machine records as the shop's equipment changes

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

All table-based views depend on the shared `SplitTableView` component in `ui-framework`, which must be built first.

1. **`SplitTableView` in `ui-framework`** — domain-agnostic sortable table with search field, filter bar, and resizable side panel. Defined in `mpbuilder.uikit.containers`. Reused by all manufacturing views.
2. **Dashboard View** — summary cards, station status strip, recent orders feed, my in-progress jobs section
3. **Station Queue View** — operator table with station/priority/deadline filters, full side panel (files, workflow progress, timeline, related items)
4. **Order Approval View** — approval table with status/date/payment filters; in-house order creation modal embedding the product builder
5. **Order Progress View** — active orders table with fulfilment side panel (collect → QC → package → delivery → invoice)
6. **Employee settings** — station capability toggles, profile

### Phase 5 — Enhancements

1. Estimated completion time calculation
2. Customer notifications
3. Analytics / reporting dashboard
4. Drying/curing time tracking
5. External vendor (outsourcing) steps
6. Imposition and ganging support

### Phase 6 — Future Views

1. **Invoice view** — invoice table, PDF generation, payment status tracking
2. **Customer management view** — customer table, order history, contact management
3. **Station configuration view** — machine registry, enable/disable stations, employee-station assignments

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
| UI workflow presentation | **Linear by default, DAG on demand** | Simpler and sufficient for most products; DAG detail available in side panel |
| Reusable table component | **`SplitTableView` in ui-framework** | All views share the same sortable table + search + filter + side panel; no per-view duplication |
| In-house order creation | **Button + modal in Approval View** | Reuses existing product builder; fits the approval staff's natural workflow |
| Fulfilment (packaging/dispatch/invoice) | **Checklist in Order Progress side panel** | Keeps a single screen for end-to-end order management without a separate fulfilment app |
| Invoices & customer management | **Deferred to Phase 6** | Not needed for MVP production tracking; can be added without structural changes |
| Station configuration | **Deferred to Phase 6** | Manual setup is sufficient early on; UI config becomes valuable as station count grows |
