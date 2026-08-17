package mpbuilder.app

import mpbuilder.catalog.CatalogService
import mpbuilder.catalog.impl.legacy.LegacyCatalogService
import mpbuilder.customers.CustomerService
import mpbuilder.customers.impl.legacy.LegacyCustomerService
import mpbuilder.identity.{AuthService, IdentityHttp, IdentityModule}
import mpbuilder.orderintake.{BasketService, CheckoutService, OrderIntakeHttp, OrderIntakeModule}
import mpbuilder.pricing.PricingService
import mpbuilder.pricing.impl.legacy.LegacyPricingService
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.{Response, Routes, Server}

/** The composition root.
  *
  * The only module that sees any `02-infra`. It decides which adapters are used, wires the layer
  * graph, and mounts every context's routes on one server. No business logic belongs here — if a
  * rule needs changing, it should never be this file that changes.
  */
object Main extends ZIOAppDefault:

  private val defaultPort = 8080

  private val config: Task[AppConfig] = AppConfig.load

  def run: ZIO[Any, Throwable, Unit] =
    for
      cfg <- config
      _ <- ZIO.logInfo(s"Starting on http://localhost:${cfg.port} — docs at /docs")
      _ <- serve(cfg).provide(
        IdentityModule.inMemory(cfg.jwtSecret),
        // Catalog and pricing are still backed by the legacy domain and its sample data. Choosing
        // *which* adapter every context runs on is this file's job and nowhere else's — swapping
        // these two lines in Phase 7/8 is the whole of that migration, as far as the app is
        // concerned.
        ZLayer.succeed[CatalogService](legacyCatalog),
        ZLayer.succeed[PricingService](legacyPricing),
        LegacyCustomerService.layer,
        OrderIntakeModule.inMemory(),
        Server.defaultWithPort(cfg.port),
      )
    yield ()

  private val legacyCatalog: CatalogService =
    LegacyCatalogService.of(
      mpbuilder.domain.sample.SampleCatalog.catalog,
      mpbuilder.domain.sample.SampleRules.ruleset,
    )

  private val legacyPricing: PricingService = LegacyPricingService.of()

  private def serve(
      cfg: AppConfig
  ): ZIO[AuthService & BasketService & CheckoutService & Server, Throwable, Unit] =
    for
      auth <- ZIO.service[AuthService]
      baskets <- ZIO.service[BasketService]
      checkout <- ZIO.service[CheckoutService]
      apiRoutes = IdentityHttp.routes(auth) ++ OrderIntakeHttp.routes(baskets, checkout, auth)
      docRoutes = SwaggerInterpreter().fromEndpoints[Task](
        IdentityHttp.endpoints ++ OrderIntakeHttp.endpoints,
        "Material Builder API",
        "v1",
      )
      http = ZioHttpInterpreter().toHttp(apiRoutes ++ docRoutes)
      _ <- Server.serve(http ++ health)
      _ <- ZIO.never
    yield ()

  /** Liveness, deliberately trivial and dependency-free: it answers whether the process is up,
    * not whether every downstream is healthy. Conflating the two makes deploys fail for the
    * wrong reasons.
    */
  private val health: Routes[Any, Response] =
    import zio.http.*
    Routes(Method.GET / "health" -> handler(Response.text("ok")))

final case class AppConfig(port: Int, jwtSecret: String)

object AppConfig:

  /** Reads from the environment with development defaults.
    *
    * The JWT secret has a default so `mill app.run` works out of the box, and warns loudly when
    * that default is in use — a signing key that ships in source control is not a secret.
    */
  val load: Task[AppConfig] =
    for
      port <- System.env("PORT").map(_.flatMap(_.toIntOption).getOrElse(8080))
      secret <- System.env("JWT_SECRET")
      _ <- ZIO
        .logWarning(
          "JWT_SECRET is not set — using a well-known development key. " +
            "Set JWT_SECRET before running anywhere real."
        )
        .when(secret.isEmpty)
    yield AppConfig(port, secret.getOrElse("dev-only-secret-do-not-use-in-production"))
