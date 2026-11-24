package mpbuilder.app.http

import kyo.*
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.backoffice.http.BackofficeRoutes
import mpbuilder.shared.BlobStorage
import mpbuilder.storefront.StorefrontFacade
import mpbuilder.storefront.http.StorefrontRoutes

final case class HttpConfig(
    host: String = "0.0.0.0",
    port: Int = 8080,
    /** Allowed browser origin for CORS: `*` (any) or exactly one origin. The static SPA usually runs on another origin. */
    corsOrigin: String = "*",
    /** Max request body (artwork uploads travel as base64 `data:` URLs inside JSON). */
    maxRequestBytes: Int = 80 * 1024 * 1024,
)

/** Assembles every kyo-http handler (+ generated OpenAPI at `/openapi.json`, Swagger UI at `/docs`) into one server. */
object ApiHttpServer:

  val ApiTitle   = "PrintCraft Platform API"
  val ApiVersion = "1.0.0"

  def handlers(
      storefront: StorefrontFacade,
      backoffice: BackofficeFacade,
      blobs: BlobStorage,
      storageLabel: String,
      readiness: => Unit < (Async & Abort[String]),
  ): Seq[HttpHandler[?, ?, ?]] =
    StorefrontRoutes(storefront).handlers ++
      BackofficeRoutes(backoffice).handlers ++
      new SystemRoutes(blobs, storageLabel, ApiVersion, readiness).handlers

  /** CORS covers browser preflights (`OPTIONS` before JSON POSTs and `Authorization`-bearing requests) for every
    * route. Credentials are not used (bearer tokens travel in headers), which keeps `*` valid.
    */
  def serverConfig(cfg: HttpConfig): HttpServerConfig =
    HttpServerConfig.default
      .host(cfg.host)
      .port(cfg.port)
      .maxContentLength(cfg.maxRequestBytes)
      .cors(HttpServerConfig.Cors(allowOrigin = cfg.corsOrigin, allowHeaders = Seq("Content-Type", "Authorization")))
      .openApi("/openapi.json", ApiTitle, ApiVersion, Some("Storefront (`/api/v1/storefront`) and Staff Backoffice (`/api/v1/admin`) BFF APIs."))

  /** Binds the server to the enclosing `Scope` (closed when the scope ends). */
  def start(handlers: Seq[HttpHandler[?, ?, ?]], cfg: HttpConfig)(using Frame): HttpServer < (Async & Scope & Abort[HttpBindException]) =
    HttpServer.init(serverConfig(cfg))(handlers*).map { server =>
      Log.info(s"$ApiTitle listening on http://${cfg.host}:${server.port} (OpenAPI: /openapi.json, Swagger UI: /docs)").andThen(server)
    }
