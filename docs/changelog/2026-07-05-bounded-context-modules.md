# 2026-07-05 — Bounded-Context Module Split (Diamond Architecture)

**PR:** N/A (branch `refactor/context-modules`)
**Author:** Claude (agent session)
**Type:** refactoring

## Summary

Dissolved the monolithic `domain` module (84 files across horizontal `model/`, `service/`, `pricing/`, `rules/`, `validation/`, `weight/`, `manufacturing/`, `sample/`, `codec/` packages) into **7 bounded-context Mill modules**, each cross-compiled JVM+JS with one flat package, per the diamond-architecture style. UI now depends on individual context modules; a future backend can do the same and add per-context `infra` adapter modules.

Dependency DAG (compiler-enforced via `moduleDeps`):

```
kernel ← catalog ← pricing ← { customer ← ordering, manufacturing } ← samples ← ui
```

## Changes Made

- **build.mill**: new `ContextModule` trait (inner `core` object with `jvm`/`js` targets + `jvm.test`, `contextDeps`/`testContextDeps`); 7 module definitions; `domain` module deleted; `ui.moduleDeps` switched to the six context `.core.js` targets.
- **modules/kernel/core** (`mpbuilder.kernel`): `ids.scala` (all cross-context IDs incl. `EmployeeId`, promoted from manufacturing), `language.scala`, `Money.scala`, `Percentage.scala` (extracted from `CustomerPricing`), `KernelCodecs.scala`.
- **modules/catalog/core** (`mpbuilder.catalog`): 10 catalog model files + compatibility rules + validation + weight calc + `CatalogQueryService`, `ConfigurationBuilder`, `manufacturingSpeed.scala` (extracted from manufacturing model), `CatalogCodecs.scala`.
- **modules/pricing/core** (`mpbuilder.pricing`): 12 pricing files + `PresetPriceService`, `CatalogExport.scala` (promoted from `DomainCodecs` nesting to top level), `PricingCodecs.scala`. `DomainCodecs` deleted.
- **modules/customer/core** (`mpbuilder.customer`): customer/login models + management/login services + `contact.scala` (`CustomerType`, `ContactInfo`, `Address` relocated from the order model).
- **modules/ordering/core** (`mpbuilder.ordering`): basket/order/discount models + basket/discount services.
- **modules/manufacturing/core** (`mpbuilder.manufacturing`): workflow/order aggregate, station scheduling & capacity types, 12 services (workflow engine/generator, queue scoring, utilisation, employee/machine management, analytics).
- **modules/samples/core** (`mpbuilder.samples`): all 9 `Sample*` seed-data objects.
- **Tests**: 22 specs redistributed into per-context `core/src/test` trees; context test targets depend on `samples` via `testContextDeps`.
- **Deleted**: `modules/domain/`, `build.sbt`, `project/` (legacy sbt build was already out of the pipeline).
- **CLAUDE.md**: build commands, architecture overview, and key paths rewritten for the new layout.

## Decisions & Rationale

- **Pragmatic DAG over strict ports/ACL**: all context types compile to Scala.js and are consumed directly by the UI as one type universe; ports + duplicated types + mappers would be pure cost until multiple deployables exist. Revisit at the adapter boundary when the backend arrives.
- **Nested `modules/<ctx>/core/` layout now**: leaves room for JVM-only `modules/<ctx>/infra` siblings (package `mpbuilder.<ctx>.adapter`) without a second file move.
- **`Money`/`Percentage`/IDs/`LocalizedString` → kernel**: dissolves the pre-existing `model ↔ pricing` dependency cycle (basket→PriceBreakdown, order/discount→Money, customer→CustomerPricing).
- **`ManufacturingSpeed` → catalog**: it is a customer-facing product option consumed by `PricingContext`; keeping it in manufacturing would have forced pricing→manufacturing. Its `toPriority` extension stays in manufacturing (returns `Priority`).
- **`CustomerType`/`ContactInfo`/`Address` → customer**: the `Customer` aggregate needs them; ordering→customer is a clean one-way edge (checkout uses them too).
- **`EmployeeId` → kernel**: used by manufacturing, customer notes, and the upcoming employee order entry.
- **Codecs split per context** with `export …given` chaining (`KernelCodecs` ⊂ `CatalogCodecs` ⊂ `PricingCodecs`) so call sites keep a single import.
- **One `samples` module, not per-context fixtures**: sample objects cross-reference each other's IDs, and the UI imports them at runtime.
- **Weight + rules/validation folded into catalog**: they operate purely on catalog types; standalone modules would add churn with no consumer.

## Issues Encountered

- **Selective-import dedupe trap**: mechanical sweeps that skipped files already containing `import mpbuilder.<ctx>.<Name>` missed files that also used same-package types; fixed by keying the dedupe check on the wildcard form and normalizing selective imports to wildcards (see troubleshooting entry).
- **Stale Zinc state after moving opaque types**: first compile after the kernel move reported thousands of bogus ambiguity errors; `./mill clean` resolved it.
- **`private[domain]` qualifiers**: `QueueScorer` used `private[domain]`, which broke when the enclosing package disappeared; changed to `private[manufacturing]` and moved `QueueScorerSpec` into the manufacturing test tree in the same step.

## Follow-up Items

- [ ] Re-target `feat/internal-order-entry` onto the new structure (`EmployeeOrderService`, `ManualDiscount` → `mpbuilder.ordering`).
- [ ] Browser smoke test of the app (extension unavailable this session; compile, fastLinkJS, and full test suite verified).
- [ ] Refresh the committed `modules/ui/src/main/resources/main.js` artifact from a new `fullLinkJS` build when convenient.
- [ ] Consider trimming the now-unused wildcard imports left behind by the mechanical migration (harmless; no `-Wunused` in scalacOptions).
