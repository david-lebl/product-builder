package mpbuilder.domain.rules

final case class CompatibilityRuleset(
    rules: List[CompatibilityRule],
    version: String,
)
