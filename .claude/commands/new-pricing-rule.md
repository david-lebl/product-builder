# New Pricing Rule

Add a new variant to `PricingRule` and wire it into `PriceCalculator`.

## Step 1 — add the enum case

File: `modules/domain/src/main/scala/mpbuilder/domain/pricing/PricingRule.scala`

```scala
enum PricingRule:
  // ...existing cases...
  case MyNewRule(param: SomeType, cost: Money)
```

Keep it **data only** — no methods, no logic inside the case.

## Step 2 — handle it in the engine

File: `modules/domain/src/main/scala/mpbuilder/domain/pricing/PriceCalculator.scala`

Find the `match` on `PricingRule` and add a branch:

```scala
case PricingRule.MyNewRule(param, cost) =>
  // pure computation — return updated context or accumulate into subtotal
```

## Step 3 — add sample data (optional)

File: `modules/domain/src/main/scala/mpbuilder/domain/sample/SamplePricelist.scala`

## Step 4 — add a test

File: `modules/domain/src/test/scala/mpbuilder/domain/PriceCalculatorSpec.scala`

```scala
test("MyNewRule applies correctly") {
  // use SampleCatalog / SamplePricelist helpers
  // assert via assertTrue(breakdown.total == Money("..."))
}
```

## Pricing flow reminder

```
subtotal
  → × quantity multiplier        (discountedSubtotal)
  → × manufacturing speed factor
  → + setupFees                  (NOT multiplied — added after)
  → billable
  → max(billable, minimumOrderPrice)
```

Setup fee variants: add them to the `setupFees: List[LineItem]` path, not the subtotal path.

