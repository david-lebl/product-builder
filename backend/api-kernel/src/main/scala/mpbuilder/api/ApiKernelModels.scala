package mpbuilder.api

/** Error envelope on the wire: a human-readable `message` plus the typed, round-trippable `error`. */
final case class ApiError[E](message: String, error: E)

/** `/healthz` & `/readyz` payload. */
final case class HealthStatus(status: String, storage: String, version: String)
