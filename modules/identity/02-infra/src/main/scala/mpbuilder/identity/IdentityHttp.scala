package mpbuilder.identity

import mpbuilder.identity.impl.http.{AuthEndpoints, AuthRoutes}
import sttp.tapir.AnyEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.Task

/** The identity context's HTTP surface, as the composition root sees it.
  *
  * Exposing routes rather than a server keeps the decision of how to serve them — port, host,
  * middleware, which other contexts share the process — in one place: `app`.
  */
object IdentityHttp:

  def routes(auth: AuthService): List[ServerEndpoint[Any, Task]] = AuthRoutes(auth)

  /** Endpoint descriptions, for the OpenAPI document. */
  val endpoints: List[AnyEndpoint] = AuthEndpoints.all
