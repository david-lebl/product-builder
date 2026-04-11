# Manufacturing Speed Pipeline

How the product builder turns a configured product and a manufacturing-speed tier into an estimated completion window (earliest / latest).

## Inputs

| Input | Source | Notes |
|---|---|---|
| `ProductConfiguration` | Builder state | Category, components, finishes, quantity, specifications |
| `ManufacturingSpeed` | `SpecValue.ManufacturingSpeedSpec` | `Express`, `Standard`, or `Economy` |
| `List[StationTimeEstimate]` | `SampleManufacturing.stationTimeEstimates` | Per-station base + per-unit time |
| `Map[StationType, StationQueueState]` | `ProductBuilderViewModel.simulatedQueueState` | Simulated queue depth / avg processing / machine count |
| `ShopSchedule` | `SampleManufacturing.shopSchedule` | Working hours + cutoff times |
| `orderTime` | `ProductBuilderViewModel.currentLocalDateTime` | Browser local wall-clock time |

The entry point is `CompletionEstimator.estimate(...)`, called from `ProductBuilderViewModel.completionEstimate(speed)` which returns a `Signal[Option[CompletionEstimate]]` consumed by the price / delivery UI.

## Stages

The estimator runs a strict pipeline; each stage feeds the next. `CompletionEstimator.scala` is the only place that touches time arithmetic.

### 1. Derive station sequence

`ProductBuilderViewModel.deriveStepTypes(config)` produces a `List[StationType]` for the job:

```
Prepress → {DigitalPrinter | LargeFormatPrinter} → [Laminator] → Cutter → [Folder] → [Binder] → QualityControl → Packaging
```

- `LargeFormatPrinter` replaces `DigitalPrinter` for banner/roll-up categories.
- `Laminator` is added when any component has a `Lamination` or `Overlamination` finish.
- `Folder` is added when `FoldTypeSpec` is present.
- `Binder` is added when `BindingMethodSpec` is present.

The rest of the estimator is quantity- and product-aware only through this derived sequence plus the quantity value.

### 2. Production time

`CompletionEstimator.estimateProductionTime(steps, quantity, stationEstimates)` sums per-station costs:

```
stationMinutes = baseTimeMinutes + (perUnitSeconds * quantity) / 60
```

Defaults live in `SampleManufacturing.stationTimeEstimates`. Stations not in the map fall back to a flat 15-minute contribution. This is the only place the product's intrinsic complexity and quantity enter the time estimate.

### 3. Queue wait

`estimateQueueWait(steps, speed, stationQueues)` converts a simulated queue state into a wait budget per station. Position in queue depends on the tier:

| Tier | Position |
|---|---|
| `Express` | `0` — always jumped to the front |
| `Standard` | `queueDepth * 0.5` — roughly "after all rush orders" |
| `Economy` | `queueDepth` — goes to the back |

Wait time per station: `(position * avgProcessingTimeMinutes) / activeMachineCount`.

### 4. Approval delay

`approvalDelay(speed)` is a flat per-tier delay added to the total. **The values are *working* minutes**, not wall-clock minutes — they are consumed by `advanceByWorkingMinutes` and therefore skip nights, weekends, and holidays.

| Tier | Working minutes | Rough equivalent |
|---|---|---|
| `Express`  | `480`  | ~1 business day — prioritized prepress review |
| `Standard` | `1200` | ~2 business days — normal prepress queue |
| `Economy`  | `2400` | ~4 business days — batched prepress |

These numbers were calibrated so that a typical small/medium order lands at: Express → next business day; Standard → +2 business days; Economy → +4 business days.

### 5. Cutoff adjustment

`adjustForCutoff(orderTime, speed, schedule)` picks an "effective start" that is never outside working hours:

- If `orderTime` is on a non-working day (weekend / holiday), or later than the tier's cutoff, or after `closeTime`, the start is pushed to `nextWorkingDay @ openTime`.
- If `orderTime` is before `openTime` on a working day, the start is snapped forward to `openTime` the same day.
- Otherwise `orderTime` is used verbatim.

Cutoffs come from `ShopSchedule`:

| Tier | Cutoff |
|---|---|
| `Express`           | `14:00` |
| `Standard`/`Economy` | `16:00` |

### 6. Advance by working minutes

`advanceByWorkingMinutes(start, minutes, workingHours)` walks a `LocalDateTime` forward by *N working minutes*, rolling over across `closeTime → next workday openTime` and skipping non-working days. This is where every delay in the pipeline (production + queue + approval + buffer) is actually applied.

Working hours default to `WorkingHours.default`: Mon–Fri, 07:00–17:00, Europe/Prague, no holidays. A business day is therefore 600 minutes (10 hours).

The full composition:

```
effectiveStart = adjustForCutoff(orderTime, speed, schedule)
totalMinutes   = production + queueWait + approval
earliestEnd    = advanceByWorkingMinutes(effectiveStart, totalMinutes, workingHours)
latestEnd      = advanceByWorkingMinutes(earliestEnd,    bufferTime(speed), workingHours)
```

### 7. Buffer

`bufferTime(speed)` is the gap between `earliestCompletion` and `latestCompletion`:

| Tier | Working minutes |
|---|---|
| `Express`  | `60`  |
| `Standard` | `240` |
| `Economy`  | `480` |

The buffer is *not* added into the production math — it produces the "range" shown in the UI so the commitment surface matches a realistic scheduling band.

### 8. Rounding & formatting

Both endpoints are rounded to the nearest 30-minute block (`roundToHalfHour`) to avoid showing implausibly precise times.

`formatDateTime(dt, now, lang)` then produces a human label relative to `now`:

- Same calendar date → `"Today, HH:mm"` / `"Dnes, HH:mm"`
- `now + 1 day` → `"Tomorrow, HH:mm"` / `"Zítra, HH:mm"`
- Within a week → `"<WeekdayName>, HH:mm"`
- Within two weeks → `"<WeekdayName>, <Month> <day>"`
- Otherwise → `"<Month> <day>"`

## "Now"

`ProductBuilderViewModel.currentLocalDateTime` returns the browser's local wall-clock time built from `scala.scalajs.js.Date`:

```scala
val d = new scala.scalajs.js.Date()
LocalDateTime.of(d.getFullYear().toInt, d.getMonth().toInt + 1, d.getDate().toInt,
                 d.getHours().toInt,    d.getMinutes().toInt,   d.getSeconds().toInt)
```

This used to use `ZoneOffset.UTC`, which in CET/CEST returned a "now" 1–2 hours behind the user's wall clock. The symptom was earliest-completion timestamps rendered *before* the user's real time. The shop schedule (`WorkingHours.default`) is tagged as `Europe/Prague`, but the `timezone` field is currently informational — nothing converts between zones. Basing "now" on browser-local time keeps everything in one frame of reference and is correct for Prague-based users without pulling in a Scala.js TZDB polyfill.

## Sample worked example

Digital-printed 500 flyers, no lamination, Standard tier, ordered Monday 10:00:

1. Steps: `[Prepress, DigitalPrinter, Cutter, QualityControl, Packaging]`
2. Production: `30 + (15 + (0.5*500)/60) + (5 + (0.2*500)/60) + 15 + 10 ≈ 81` min
3. Queue wait (Standard): sums to a few tens of minutes against simulated queues
4. Approval (Standard): `1200` min (~2 business days)
5. Cutoff: 10:00 Mon is before 16:00 → effective start = Mon 10:00
6. Advance by ~1 280 min working minutes from Mon 10:00 → roughly Wed ~11:40
7. Buffer 240 min → latest end Wed ~15:40
8. Rounded → `Wed 11:30 – Wed 15:30`, labelled `"Wednesday, 11:30 – Wednesday, 15:30"`

## Key files

| Area | Path |
|---|---|
| Estimator service | `modules/domain/src/main/scala/mpbuilder/domain/service/CompletionEstimator.scala` |
| Station time estimates & shop schedule | `modules/domain/src/main/scala/mpbuilder/domain/sample/SampleManufacturing.scala` |
| `ShopSchedule` / `WorkingHours` | `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/` |
| `StationTimeEstimate` | `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/StationTimeEstimate.scala` |
| Wiring from UI state | `modules/ui/src/main/scala/mpbuilder/ui/productbuilder/ProductBuilderViewModel.scala` (`completionEstimate`, `currentLocalDateTime`, `deriveStepTypes`, simulated queue state) |
| Tests | `modules/domain/src/test/scala/mpbuilder/domain/ExpressManufacturingSpec.scala` |

## Tuning notes

- Per-tier feel comes almost entirely from `approvalDelay` + `bufferTime` — change those before touching station times.
- `stationTimeEstimates` is where product complexity and quantity dependence live. Any "bigger jobs should take longer" change goes here.
- Cutoff times in `ShopSchedule.default` shift *when* an order rolls into the next day, not *how long* it takes once it starts.
- Queue wait is currently driven by a simulated map (`ProductBuilderViewModel.simulatedQueueState`). When a live queue source is wired up, only that map needs to be replaced — the estimator contract is unchanged.
