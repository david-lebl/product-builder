# 2026-04-23 — External Manufacturing Routing: Implementation

**PR:** copilot/add-external-manufacturing-routing
**Author:** copilot agent
**Type:** feature

## Summary

Implemented the external manufacturing partner routing feature per the specification in `docs/analysis/external-manufacturing-analysis.md`. The domain model additions (`ExternalPartner`, `PartnerId`, `PartnerAvailability`, `PartnerTierPolicy`), rule extensions (`RequiresExternalPartner`, `ExternalPartnerMarkup`), workflow generator hooks, completion estimator extension, and sample data were already partially implemented in the prior session but left the repository with compile errors. This session fixed all compile errors, updated tests to match the new behavior, and added the full test suite described in spec section 11.

## Changes Made

### Domain Service Fix
- **Modified**: `modules/domain/src/main/scala/mpbuilder/domain/service/CatalogQueryService.scala`
  - Added `case CompatibilityRule.RequiresExternalPartner(_, _, _, _) => true` to the exhaustive match in `compatibleFinishes` — this rule is a routing annotation, not a finish restriction, so finishes remain compatible.

### Test Updates
- **Modified**: `modules/domain/src/test/scala/mpbuilder/domain/ConfigurationBuilderSpec.scala`
  - Updated test "banner 200×100 cm (2000×1000 mm) exceeds max dimension and is rejected" to reflect the new behavior: the banner is now **accepted** and routed to an external partner. Test now asserts `result.isSuccess` instead of checking for `SpecConstraintViolation`.

- **Modified**: `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`
  - Added suite "external partner markup" with two tests:
    1. `ExternalPartnerMarkup` applied after quantity multiplier, before setup fees — verifies the 15% markup adds to the discounted subtotal but does not affect setup fees.
    2. No markup line when `activePartnerId` is `None` — verifies backward compatibility.

- **Modified**: `modules/domain/src/test/scala/mpbuilder/domain/WorkflowGeneratorSpec.scala`
  - Added suite "external partner routing" with three tests:
    1. Oversized banner (2000×1000 mm) routes to `ExternalPartner` step; `LargeFormatPrinter` and `LargeFormatFinishing` are absent; `Prepress`, `QualityControl`, `Packaging` are present.
    2. `ExternalPartner` step has `assignedPartner` set and depends on `Prepress`.
    3. In-house banner (≤1500×1500 mm) does NOT get an `ExternalPartner` step.

- **Modified**: `modules/domain/src/test/scala/mpbuilder/domain/ExpressManufacturingSpec.scala`
  - Added `import mpbuilder.domain.rules.*`.
  - Added suite "TierRestrictionValidator external partner" with four tests:
    1. Express blocked when `RequiresExternalPartner` rule matches.
    2. Economy blocked when `RequiresExternalPartner` rule matches.
    3. Standard allowed even when `RequiresExternalPartner` rule matches.
    4. No external rules → no external tier violation.

### UI Fixes (pre-existing compile errors)
- **Modified**: `modules/ui/src/main/scala/mpbuilder/ui/catalog/views/RulesEditorView.scala`
  - Fixed pre-existing compile errors: `FormComponents.textField` and `FormComponents.enumSelectRequired` calls were missing required arguments (`placeholder` and `display` respectively) because Scala 3's `export` statement does not re-export default parameter values.
  - Added missing `placeholder = ""` to all `textField` calls.
  - Added `_.toString` display function to all `enumSelectRequired` calls.
  - Added `ScoringMaxCreasesForCategory`, `ScoringMaxCreasesForMaterial`, `ScoringMaxCreasesForPrintingProcess`, and `RequiresExternalPartner` cases to `ruleSummary`, `ruleTypeName`, and `extractReason` to fix non-exhaustive match warnings.

## Decisions & Rationale

- **`RequiresExternalPartner` does not filter finishes in `CatalogQueryService`**: The rule is a routing annotation, not a blocking rule. Finishes remain compatible regardless of external routing.
- **Banner test updated to expect success**: The `SpecConstraint(MaxDimension)` rule for oversized banners was intentionally replaced with `RequiresExternalPartner` in a prior session. The test must reflect the new behavior.
- **`placeholder = ""` for textField calls**: This is a Scala 3 `export` limitation — default parameter values are not propagated via `export`. The empty string is the correct default for form fields that need no placeholder hint.

## Issues Encountered

### Scala 3 `export` does not re-export default parameter values
**Symptom:** `RulesEditorView.scala` failed to compile with "missing argument for parameter placeholder" and "missing argument for parameter display" errors — even though those parameters had defaults in the original `FormComponents` definition.

**Cause:** Scala 3's `export` keyword re-exports method signatures but strips default parameter values. The re-exported methods appear to callers as if all parameters are required.

**Solution:** Pass the previously-defaulted arguments explicitly at each call site: `""` for `textField`'s `placeholder`, `_.toString` for `enumSelectRequired`'s `display`.

→ Added to `docs/troubleshooting.md`.

## Follow-up Items

- [ ] UI: add "External production" badge in the product configurator basket view when `RequiresExternalPartner` rule matches
- [ ] UI: disable Express/Economy tier radio options when external routing is required
- [ ] UI: render `ExternalPartner` workflow steps with distinct `step-external` CSS class and partner detail panel
- [ ] Add `ExternalPartnerMarkup` rules to `SamplePricelist.pricelist` and `pricelistCzk` for the two seed partners
- [ ] `estimateExternal` in `CompletionEstimator` is implemented — wire it up in `ManufacturingViewModel` for external-partner workflows
- [ ] Promote `docs/analysis/external-manufacturing-analysis.md` to `docs/specs/external-manufacturing.md` once UI touchpoints are complete
