package mpbuilder.server.http

import zio.http.*

/** Serves the built SPA from classpath resources (`public/`) in production;
  * during development the Vite dev server serves the frontend instead and
  * proxies `/api` here.
  */
object StaticRoutes:

  val routes: Routes[Any, Nothing] =
    Routes(
      Method.GET / Root -> Handler.fromResource("public/index.html").orElse(Handler.notFound)
    ) @@ Middleware.serveResources(Path.empty, "public")
