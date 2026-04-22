# 2026-04-22 — External Manufacturing Analysis Specification

**PR:** N/A
**Author:** copilot agent
**Type:** analysis

## Summary

Created a full specification document for routing orders to external manufacturing partners. The spec covers domain model additions (`PartnerId`, `ExternalPartner`, `PartnerAvailability`, `PartnerTierPolicy`), new `CompatibilityRule` and `PricingRule` variants, a `WorkflowGenerator` hook, a partner-aware `CompletionEstimator` branch, tier-restriction integration, minimal UI touchpoints, sample data seeding, and a verification checklist. No code changes were made; implementation is a follow-up session.

## Changes Made

- **Created**: `docs/analysis/external-manufacturing-analysis.md` — full specification per the plan outline (11 sections, ~490 lines)
- **Modified**: `docs/INDEX.md` — added link to the new document under "Analysis & Research"

## Decisions & Rationale

- **Single `ExternalPartner` `StationType` variant** (not one per partner): keeps the enum clean and avoids combinatorial growth as new partners are onboarded. The partner identity is carried on `WorkflowStep.assignedPartner: Option[PartnerId]` instead.
- **`RequiresExternalPartner` replaces the banner max-dimension `SpecConstraint`** rather than supplementing it: having both a blocking rule and a routing rule for the same predicate would create ambiguity in `RuleEvaluator`. The replacement is cleaner and is fully described in the spec.
- **`ExternalPartnerMarkup` inserted after tier multipliers, before setup fees**: preserves the existing "setup fees are not discounted" invariant that is documented in `pricing.md` and tested in `PriceCalculatorSpec`.
- **`PartnerTierPolicy` as a policy object, not a per-partner field**: expresses the invariant once; a future per-partner `allowedTiers` override point is noted in the spec without being added.
- **Separate `ExternalManufacturingError` enum** rather than a new `ConfigurationError` variant: external routing is not a configuration error — it is a valid routing outcome. Keeping the error namespaces separate avoids confusion in the `RuleEvaluator` and makes it easier to display partner-unavailable errors distinctly in the UI.
- **New `estimateExternal(...)` method on `CompletionEstimator`** rather than modifying the existing `estimate(...)`: the external estimation path bypasses queue/speed math entirely and should not be tangled with the in-house path. The existing `advanceByWorkingMinutes` and `formatDateTime` helpers are reused unchanged.

## Issues Encountered

None. This was a spec-writing session with no code changes.

## Follow-up Items

- [ ] Implement `PartnerId` opaque type and `ExternalPartner` / `PartnerAvailability` / `PartnerTierPolicy` in `modules/domain/src/main/scala/mpbuilder/domain/manufacturing/ExternalPartner.scala`
- [ ] Add `ExternalPartner` variant to `StationType` enum (with `displayName` and `icon` extensions)
- [ ] Add `assignedPartner` and `estimatedCompletionOverride` optional fields to `WorkflowStep`
- [ ] Add `RequiresExternalPartner` variant to `CompatibilityRule` enum
- [ ] Replace the banner `MaxDimension(1500, 1500)` `SpecConstraint` in `SampleRules.scala` with `RequiresExternalPartner`
- [ ] Add spot-varnish `RequiresExternalPartner` rule to `SampleRules.scala`
- [ ] Add `ExternalPartnerMarkup` variant to `PricingRule` enum and implement in `PriceCalculator`
- [ ] Add `ExternalManufacturingError` enum in `modules/domain/src/main/scala/mpbuilder/domain/validation/`
- [ ] Extend `WorkflowGenerator.generate(...)` with partner parameters and ExternalPartner step injection logic
- [ ] Add `estimateExternal(...)` method to `CompletionEstimator`
- [ ] Add `ExternalPartnerTierNotAllowed` variant to `TierRestrictionViolation` and extend `TierRestrictionValidator`
- [ ] Create `SamplePartners.scala` with `partner-large-format` and `partner-spot-varnish`
- [ ] Write unit tests: `PriceCalculatorSpec` (markup position), `WorkflowGeneratorSpec` (partner step replacement), `CompatibilityRuleSpec` (banner routing), `TierRestrictionValidatorSpec` (Express blocked)
- [ ] Update manufacturing UI to render external steps with distinct icon/colour and partner detail panel
- [ ] Promote spec to `docs/specs/` once implementation is complete and stable
