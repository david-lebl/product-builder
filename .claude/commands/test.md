# Test

Run domain tests. All tests live under `modules/domain/src/test/` and use **ZIO Test**.

## Run all tests

```bash
mill domain.jvm.test
# or:
sbt domainJVM/test
```

## Run a single spec by name

```bash
mill 'domain.jvm.test.testOnly *<SpecName>'
# e.g.:
mill 'domain.jvm.test.testOnly *PriceCalculatorSpec'
mill 'domain.jvm.test.testOnly *CompatibilityRuleSpec'

# sbt equivalent:
sbt "domainJVM/testOnly *PriceCalculatorSpec"
```

## Test file locations

- `modules/domain/src/test/scala/mpbuilder/domain/`

## Notes

- Tests use `ZIOSpecDefault` — extend it and define `def spec`.
- Use `assertTrue(...)` for assertions.
- `.unsafe(...)` constructors are allowed in tests for creating IDs and sample values.

