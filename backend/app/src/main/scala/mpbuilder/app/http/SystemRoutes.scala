package mpbuilder.app.http

import kyo.*
import mpbuilder.api.ApiKernelCodecs.given
import mpbuilder.api.HealthStatus
import mpbuilder.shared.*

/** Health/readiness probes, artwork blob downloads and the Swagger UI page. */
final class SystemRoutes(
    blobs: BlobStorage,
    storageLabel: String,
    version: String,
    readiness: => Unit < (Async & Abort[String]),
):
  private def status(s: String) = HealthStatus(s, storageLabel, version)

  val handlers: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw("healthz").response(_.bodyJson[HealthStatus]).metadata(_.tag("System").summary("Liveness probe"))
      .handler(_ => HttpResponse.ok(status("ok"))),
    HttpRoute.getRaw("readyz").response(_.bodyJson[HealthStatus])
      .metadata(_.tag("System").summary("Readiness probe (database reachable)"))
      .handler { _ =>
        Abort.run[String](readiness).map {
          case Result.Success(_) => HttpResponse.ok(status("ready"))
          case _                 => HttpResponse.halt(HttpResponse.serviceUnavailable)
        }
      },
    // `GET /api/v1/blobs/<storage key…>` streams a stored artwork file / preview.
    HttpRoute.getRaw("api" / "v1" / "blobs" / HttpPath.Rest("key")).response(_.bodyBinary)
      .metadata(_.tag("System").summary("Download a stored artwork file"))
      .handler { req =>
        Abort.run[BlobError](blobs.get(req.fields.key)).map {
          case Result.Success(Some(c)) =>
            HttpResponse.ok(Span.from(c.bytes.toArray))
              .setHeader("Content-Type", c.meta.mimeType)
              .cacheControl("private, max-age=3600")
          case _ => HttpResponse.halt(HttpResponse.notFound)
        }
      },
    HttpRoute.getRaw("docs").response(_.bodyText).metadata(_.tag("System").summary("Swagger UI"))
      .handler(_ => HttpResponse.ok(SystemRoutes.swaggerUi).setHeader("Content-Type", "text/html; charset=utf-8")),
  )

object SystemRoutes:
  /** Swagger UI (from a CDN) rendering the OpenAPI document kyo-http generates from the routes. */
  val swaggerUi: String =
    """<!doctype html>
      |<html lang="en">
      |<head>
      |  <meta charset="utf-8"/>
      |  <meta name="viewport" content="width=device-width, initial-scale=1"/>
      |  <title>PrintCraft Platform API</title>
      |  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14/swagger-ui.css"/>
      |</head>
      |<body>
      |  <div id="swagger-ui"></div>
      |  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
      |  <script>window.ui = SwaggerUIBundle({ url: "/openapi.json", dom_id: "#swagger-ui" });</script>
      |</body>
      |</html>
      |""".stripMargin
