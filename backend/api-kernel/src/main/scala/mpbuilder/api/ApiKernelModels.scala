package mpbuilder.api

/** Error envelope on the wire: a human-readable `message` plus the typed, round-trippable `error`. */
final case class ApiError[E](message: String, error: E)

/** `/healthz` & `/readyz` payload. */
final case class HealthStatus(status: String, storage: String, version: String)

/** Transport-neutral HTTP conventions shared by the server routes (JVM) and the HTTP clients (JVM + JS). */
object ApiConventions:

  val apiVersion = "v1"

  /** Percent-encodes one path segment (ids are validated, but stay safe for any character). */
  def segment(raw: String): String =
    java.net.URLEncoder.encode(raw, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")

  /** Maps a facade error to its HTTP status by inspecting the (possibly wrapped) error case name. */
  def statusOf(error: Product): Int =
    val name = error.productPrefix
    val nested = error.productIterator.collectFirst {
      case p: Product if error.productArity == 1 && p.getClass.getName.contains("Error") => p
    }
    nested match
      case Some(inner) => statusOf(inner)
      case None =>
        if Set("Unauthenticated", "InvalidCredentials", "SessionExpired", "SessionNotFound", "SessionRevoked").exists(name.contains) then 401
        else if Set("Forbidden", "NotOwned", "NotACustomerSession", "NotAnEmployeeSession", "AccountNotActive").exists(name.contains) then 403
        else if name.contains("NotFound") then 404
        else if Set("Already", "InUse", "Conflict", "Exists", "Locked").exists(name.contains) then 409
        else 400
