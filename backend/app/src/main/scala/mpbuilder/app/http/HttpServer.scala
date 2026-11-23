package mpbuilder.app.http

import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.backoffice.http.BackofficeRoutes
import mpbuilder.shared.BlobStorage
import mpbuilder.storefront.StorefrontFacade
import mpbuilder.storefront.http.StorefrontRoutes
import sttp.tapir.server.interceptor.cors.{CORSConfig, CORSInterceptor}
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.*
import zio.http.*

final case class HttpConfig(
    host: String = "0.0.0.0",
    port: Int = 8080,
    /** Allowed browser origins for CORS (`*` = any). The static SPA usually runs on another origin. */
    corsOrigins: List[String] = List("*"),
    /** Max request body (artwork uploads travel as base64 `data:` URLs inside JSON). */
    maxRequestBytes: Int = 80 * 1024 * 1024,
)

/** Assembles all Tapir server endpoints (+ OpenAPI / Swagger UI at `/docs`) into a ZIO HTTP server. */
object HttpServer:

  val ApiTitle   = "PrintCraft Platform API"
  val ApiVersion = "1.0.0"

  def serverEndpoints(
      storefront: StorefrontFacade,
      backoffice: BackofficeFacade,
      blobs: BlobStorage,
      storageLabel: String,
      readiness: IO[String, Unit],
  ): List[ZServerEndpoint[Any, Any]] =
    val api =
      StorefrontRoutes(storefront).endpoints ++
        BackofficeRoutes(backoffice).endpoints ++
        new SystemRoutes(blobs, storageLabel, ApiVersion, readiness).endpoints
    val docs = SwaggerInterpreter().fromServerEndpoints[Task](api, ApiTitle, ApiVersion)
    api ++ docs

  /** CORS is handled by Tapir's interceptor so browser preflights (`OPTIONS` before JSON POSTs and
    * `Authorization`-bearing requests) are answered for every endpoint. Credentials are not used
    * (bearer tokens travel in headers), which keeps `*` origins valid.
    */
  def corsConfig(cfg: HttpConfig): CORSConfig =
    val base = CORSConfig.default.allowAllMethods.reflectHeaders.denyCredentials.exposeAllHeaders
    if cfg.corsOrigins.contains("*") then base.allowAllOrigins
    else base.allowMatchingOrigins(origin => cfg.corsOrigins.contains(origin.stripSuffix("/")))

  def routes(endpoints: List[ZServerEndpoint[Any, Any]], cfg: HttpConfig): Routes[Any, Response] =
    val options =
      ZioHttpServerOptions
        .customiseInterceptors[Any]
        .corsInterceptor(CORSInterceptor.customOrThrow[Task](corsConfig(cfg)))
        .options
    ZioHttpInterpreter(options).toHttp(endpoints)

  /** Serves forever (until interrupted). */
  def serve(endpoints: List[ZServerEndpoint[Any, Any]], cfg: HttpConfig): Task[Nothing] =
    val serverConfig = Server.Config.default.binding(cfg.host, cfg.port).disableRequestStreaming(cfg.maxRequestBytes)
    ZIO.logInfo(s"$ApiTitle listening on http://${cfg.host}:${cfg.port} (Swagger UI: /docs)") *>
      Server.serve(routes(endpoints, cfg)).provide(ZLayer.succeed(serverConfig), Server.live)
