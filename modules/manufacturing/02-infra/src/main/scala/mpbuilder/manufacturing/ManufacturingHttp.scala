package mpbuilder.manufacturing

import mpbuilder.identity as id
import mpbuilder.manufacturing.impl.http.{ProductionEndpoints, ProductionRoutes}
import sttp.tapir.AnyEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.Task

/** The manufacturing context's HTTP surface, as the composition root sees it. */
object ManufacturingHttp:

  def routes(production: ProductionService, auth: id.AuthService): List[ServerEndpoint[Any, Task]] =
    ProductionRoutes(production, auth).endpoints

  val endpoints: List[AnyEndpoint] = ProductionEndpoints.all
