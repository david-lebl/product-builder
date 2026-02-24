package mpbuilder.domain.internal.rules

final case class CompatibilityRuleset(
    rules: List[CompatibilityRule],
    version: String,
)
