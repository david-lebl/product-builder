package mpbuilder.api

import mpbuilder.api.ApiKernelCodecs.given
import mpbuilder.api.ApiSupport.body
import sttp.model.StatusCode
import sttp.tapir.*

/** Operational endpoints: health/readiness probes and blob downloads (Phase 5 artwork files). */
object SystemEndpoints:

  val healthz =
    endpoint.get.in("healthz").out(body[HealthStatus]).tag("System").summary("Liveness probe")

  val readyz =
    endpoint.get.in("readyz").out(body[HealthStatus]).errorOut(statusCode(StatusCode.ServiceUnavailable).and(stringBody))
      .tag("System").summary("Readiness probe (database reachable)")

  /** `GET /api/v1/blobs/<storage key…>` streams a stored artwork file / preview. */
  val downloadBlob =
    endpoint.get
      .in("api" / ApiSupport.apiVersion / "blobs")
      .in(paths.description("Blob storage key, e.g. uploads/ord-10001/art-10002-flyer.pdf"))
      .errorOut(statusCode(StatusCode.NotFound).and(stringBody))
      .out(header[String]("Content-Type"))
      .out(header[String]("Cache-Control"))
      .out(byteArrayBody)
      .tag("System")
      .summary("Download a stored artwork file")
