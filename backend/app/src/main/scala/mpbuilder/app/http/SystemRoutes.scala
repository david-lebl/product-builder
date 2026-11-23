package mpbuilder.app.http

import mpbuilder.api.{HealthStatus, SystemEndpoints as E}
import mpbuilder.shared.BlobStorage
import sttp.tapir.ztapir.*
import zio.*

/** Health/readiness probes and artwork blob downloads. */
final class SystemRoutes(
    blobs: BlobStorage,
    storageLabel: String,
    version: String,
    readiness: IO[String, Unit],
):
  private def status(s: String) = HealthStatus(s, storageLabel, version)

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    E.healthz.zServerLogic(_ => ZIO.succeed(status("ok"))),
    E.readyz.zServerLogic(_ => readiness.as(status("ready"))),
    E.downloadBlob.zServerLogic(segments =>
      blobs
        .get(segments.mkString("/"))
        .mapError(_.message)
        .someOrFail("Blob not found")
        .map(c => (c.meta.mimeType, "private, max-age=3600", c.bytes.toArray))
    ),
  )
