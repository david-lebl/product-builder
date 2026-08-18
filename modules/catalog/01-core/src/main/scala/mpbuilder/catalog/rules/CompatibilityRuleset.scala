package mpbuilder.catalog.rules

final case class CompatibilityRuleset(
    rules: List[CompatibilityRule],
    version: String,
)
