package mpbuilder.app

import mpbuilder.catalog.CatalogService
import mpbuilder.catalog.impl.legacy.LegacyCatalogService
import mpbuilder.customers.CustomerService
import mpbuilder.customers.impl.legacy.LegacyCustomerService
import mpbuilder.identity.{AuthService, IdentityHttp, IdentityModule}
import mpbuilder.manufacturing.{ManufacturingHttp, ManufacturingModule, ProductionOutbox, ProductionService}
import mpbuilder.orderintake.{
  BasketService,
  CheckoutService,
  OrderIntakeHttp,
  OrderIntakeModule,
  OrderLifecycle,
  OrderOutbox,
  OrderService,
}
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
        IdentityModule.inMemory(cfg.jwtSecret, cfg.staffEmails),
        // Catalog and pricing are still backed by the legacy domain and its sample data. Choosing
        // *which* adapter every context runs on is this file's job and nowhere else's — swapping
        // these two lines in Phase 7/8 is the whole of that migration, as far as the app is
        // concerned.
        ZLayer.succeed[CatalogService](legacyCatalog),
        ZLayer.succeed[PricingService](legacyPricing),
        LegacyCustomerService.layer,
        OrderIntakeModule.inMemory(),
        ManufacturingModule.inMemory,
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
  ): ZIO[
    AuthService & BasketService & CheckoutService & OrderService & OrderLifecycle & OrderOutbox &
      ProductionService & ProductionOutbox & Server,
    Throwable,
    Unit,
  ] =
    for
      auth <- ZIO.service[AuthService]
      baskets <- ZIO.service[BasketService]
      checkout <- ZIO.service[CheckoutService]
      orders <- ZIO.service[OrderService]
      production <- ZIO.service[ProductionService]
      // The two contexts never call each other; this carries what one published to the other.
      _ <- EventRelay.run
      apiRoutes = IdentityHttp.routes(auth) ++
        OrderIntakeHttp.routes(baskets, checkout, orders, auth) ++
        ManufacturingHttp.routes(production, auth)
      docRoutes = SwaggerInterpreter().fromEndpoints[Task](
        IdentityHttp.endpoints ++ OrderIntakeHttp.endpoints ++ ManufacturingHttp.endpoints,
        "Material Builder API",
        "v1",
      )
      http = ZioHttpInterpreter().toHttp(apiRoutes ++ docRoutes)
      _ <- Server.serve(http ++ health ++ Assets.routes(cfg.assetRoot))
      _ <- ZIO.never
    yield ()

  /** Liveness, deliberately trivial and dependency-free: it answers whether the process is up,
    * not whether every downstream is healthy. Conflating the two makes deploys fail for the
    * wrong reasons.
    */
  private val health: Routes[Any, Response] =
    import zio.http.*
    Routes(Method.GET / "health" -> handler(Response.text("ok")))

final case class AppConfig(
    port: Int,
    jwtSecret: String,
    /** Addresses that register as staff. Empty means nobody, which is the right default: a shop
      * with no configured staff is a shop where nobody can approve production, not one where
      * anybody can.
      */
    staffEmails: Set[String],
    /** Where the built SPA lives. `None` when there is nothing built to serve. */
    assetRoot: Option[java.io.File],
)

object AppConfig:

  private val defaultAssetRoot = "dist"

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
      staff <- System
        .env("STAFF_EMAILS")
        .map(_.toList.flatMap(_.split(",").toList).map(_.trim).filter(_.nonEmpty).toSet)
      _ <- ZIO
        .logWarning(
          "STAFF_EMAILS is not set — no account can approve production or confirm a payment. " +
            "Set it to a comma-separated list of addresses that should register as staff."
        )
        .when(staff.isEmpty)
      configured <- System.env("ASSET_ROOT").map(_.getOrElse(defaultAssetRoot))
      assets = Option(java.io.File(configured)).filter(_.isDirectory)
      _ <- ZIO
        .logWarning(
          s"No SPA assets at '$configured' — serving the API only. " +
            "Run `mill ui.fullLinkJS` and copy the bundle there to serve the app itself."
        )
        .when(assets.isEmpty)
    yield AppConfig(
      port = port,
      jwtSecret = secret.getOrElse("dev-only-secret-do-not-use-in-production"),
      staffEmails = staff,
      assetRoot = assets,
    )
