package mpbuilder.server

import com.typesafe.config.ConfigFactory
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import mpbuilder.server.http.{ApiRoutes, StaticRoutes}
import mpbuilder.server.persistence.{Migrations, OrderRepository}
import mpbuilder.server.service.OrderService
import zio.*
import zio.http.*

object Main extends ZIOAppDefault:

  private val httpPort: Int =
    ConfigFactory.load().getInt("mpbuilder.http.port")

  private val routes = (ApiRoutes.routes ++ StaticRoutes.routes) @@ Middleware.requestLogging()

  override def run =
    (Migrations.run *> Server.serve(routes))
      .provideSome[Scope](
        Server.defaultWithPort(httpPort),
        Quill.DataSource.fromPrefix("mpbuilder.db"),
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        OrderRepository.live,
        OrderService.live,
      )
