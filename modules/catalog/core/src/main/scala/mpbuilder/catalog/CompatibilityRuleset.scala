package mpbuilder.catalog

final case class CompatibilityRuleset(
    rules: List[CompatibilityRule],
    version: String,
)
