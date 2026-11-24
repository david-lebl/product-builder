package mpbuilder.platform
package blob

import kyo.*
import mpbuilder.shared.*

import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.{HttpClient as JHttpClient, HttpRequest as JHttpRequest, HttpResponse as JHttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration as JDuration
import java.util.UUID

/** Google Cloud Storage (GCS) [[BlobStorage]] adapter speaking the GCS JSON API v1 over non-blocking
  * `java.net.http.HttpClient`.
  *
  *   - `put` uses `POST /upload/storage/v1/b/{bucket}/o?uploadType=multipart` to atomically write bytes,
  *     `contentType`, and custom `metadata.sha256`.
  *   - `get` reads metadata (`GET /storage/v1/b/{bucket}/o/{object}`) and media bytes (`?alt=media`),
  *     falling back to computing SHA-256 on the fly when `metadata.sha256` is absent.
  *   - `delete` issues `DELETE /storage/v1/b/{bucket}/o/{object}` and treats `404` as idempotent success.
  *
  * Automatically caches & refreshes OAuth2 tokens (service account, authorized user, or GCE/Cloud Run
  * metadata server), refreshes once on HTTP 401, and retries transient `408`/`429`/`5xx` responses.
  */
final class GcsBlobStorage private (
    val config: GcsConfig,
    auth: GcsAuth,
    httpClient: JHttpClient,
) extends BlobStorage:

  private val encodedBucket = GcsAuth.urlEncode(config.bucket)

  override def put(key: String, bytes: Chunk[Byte], mimeType: String)(using Frame): StoredBlob < (Async & Abort[BlobError]) =
    for
      k    <- BlobStorage.validateKey(key).orAbort
      _    <- Effects.failWhen(bytes.size.toLong > config.maxBytes)(BlobError.TooLarge(bytes.size.toLong, config.maxBytes))
      meta  = BlobStorage.describe(k, bytes, mimeType, config.publicUrlFor(k))
      obj   = config.objectName(k)
      boundary = s"mpbuilder-gcs-${UUID.randomUUID()}"
      body  = GcsBlobStorage.buildMultipartBody(boundary, obj, mimeType, meta.checksumSha256, bytes)
      uri   = URI.create(s"${config.normalizedEndpoint}/upload/storage/v1/b/$encodedBucket/o?uploadType=multipart")
      resp <- executeWithRetry { authHeader =>
                val b = JHttpRequest.newBuilder(uri)
                  .timeout(config.requestTimeout)
                  .header("Content-Type", s"multipart/related; boundary=$boundary")
                  .POST(JHttpRequest.BodyPublishers.ofByteArray(body))
                authHeader.foreach(b.header("Authorization", _))
                GcsHttp.sendString(httpClient, b.build())
              }
      _    <- Effects.failUnless(resp.statusCode() / 100 == 2)(
                BlobError.StorageFailure(s"GCS put '$obj' failed with HTTP ${resp.statusCode()}: ${resp.body().take(300)}")
              )
    yield meta

  override def get(key: String)(using Frame): Option[BlobContent] < (Async & Abort[BlobError]) =
    for
      k        <- BlobStorage.validateKey(key).orAbort
      obj       = config.objectName(k)
      encObj    = GcsAuth.urlEncode(obj)
      metaUri   = URI.create(s"${config.normalizedEndpoint}/storage/v1/b/$encodedBucket/o/$encObj")
      metaResp <- executeWithRetry { authHeader =>
                    val b = JHttpRequest.newBuilder(metaUri).timeout(config.requestTimeout).GET()
                    authHeader.foreach(b.header("Authorization", _))
                    GcsHttp.sendString(httpClient, b.build())
                  }
      result   <- (metaResp.statusCode() match
                    case 404 => None
                    case s if s / 100 == 2 =>
                      for
                        gcsMeta   <- Json.decode[GcsBlobStorage.GcsObjectMetadata](metaResp.body()).toEither.left
                                       .map(e => BlobError.StorageFailure(s"Invalid GCS metadata JSON for '$obj': ${e.getMessage}"))
                                       .orAbort
                        mediaUri   = URI.create(s"${config.normalizedEndpoint}/storage/v1/b/$encodedBucket/o/$encObj?alt=media")
                        mediaResp <- executeWithRetry { authHeader =>
                                       val b = JHttpRequest.newBuilder(mediaUri).timeout(config.requestTimeout).GET()
                                       authHeader.foreach(b.header("Authorization", _))
                                       GcsHttp.sendBytes(httpClient, b.build())
                                     }
                        out       <- (mediaResp.statusCode() match
                                       case 404 => None
                                       case ms if ms / 100 == 2 =>
                                         val bytes = Chunk.from(mediaResp.body())
                                         val headerMime = Option(mediaResp.headers().firstValue("Content-Type").orElse(null))
                                           .map(_.takeWhile(_ != ';').trim)
                                           .filter(_.nonEmpty)
                                         val mime = gcsMeta.contentType.filter(_.nonEmpty)
                                           .orElse(headerMime)
                                           .getOrElse("application/octet-stream")
                                         val sha = gcsMeta.metadata.flatMap(_.get("sha256")).filter(_.nonEmpty)
                                           .getOrElse(Sha256.hex(bytes.toArray))
                                         val stored = StoredBlob(k, mime, bytes.size.toLong, sha, config.publicUrlFor(k))
                                         Some(BlobContent(stored, bytes))
                                       case ms =>
                                         Abort.fail(BlobError.StorageFailure(s"GCS get media '$obj' failed with HTTP $ms"))
                                     ): Option[BlobContent] < (Async & Abort[BlobError])
                      yield out
                    case s =>
                      Abort.fail(BlobError.StorageFailure(s"GCS get metadata '$obj' failed with HTTP $s: ${metaResp.body().take(300)}"))
                  ): Option[BlobContent] < (Async & Abort[BlobError])
    yield result

  override def delete(key: String)(using Frame): Unit < (Async & Abort[BlobError]) =
    for
      k      <- BlobStorage.validateKey(key).orAbort
      obj     = config.objectName(k)
      encObj  = GcsAuth.urlEncode(obj)
      uri     = URI.create(s"${config.normalizedEndpoint}/storage/v1/b/$encodedBucket/o/$encObj")
      resp   <- executeWithRetry { authHeader =>
                  val b = JHttpRequest.newBuilder(uri).timeout(config.requestTimeout).DELETE()
                  authHeader.foreach(b.header("Authorization", _))
                  GcsHttp.sendString(httpClient, b.build())
                }
      _      <- (resp.statusCode() match
                  case s if s / 100 == 2 || s == 404 => ()
                  case s =>
                    Abort.fail(BlobError.StorageFailure(s"GCS delete '$obj' failed with HTTP $s: ${resp.body().take(300)}"))
                ): Unit < Abort[BlobError]
    yield ()

  /** Verifies that the configured bucket is reachable and accessible (useful for readiness probes). */
  def checkBucket(using Frame): Unit < (Async & Abort[BlobError]) =
    val uri = URI.create(s"${config.normalizedEndpoint}/storage/v1/b/$encodedBucket")
    executeWithRetry { authHeader =>
      val b = JHttpRequest.newBuilder(uri).timeout(config.requestTimeout).GET()
      authHeader.foreach(b.header("Authorization", _))
      GcsHttp.sendString(httpClient, b.build())
    }.map { resp =>
      Effects.failUnless(resp.statusCode() / 100 == 2)(
        BlobError.StorageFailure(s"GCS bucket '${config.bucket}' check failed with HTTP ${resp.statusCode()}: ${resp.body().take(300)}")
      )
    }

  private def executeWithRetry[T](
      send: Option[String] => JHttpResponse[T] < (Async & Abort[BlobError])
  )(using Frame): JHttpResponse[T] < (Async & Abort[BlobError]) =
    def loop(attempt: Int, authRefreshed: Boolean): JHttpResponse[T] < (Async & Abort[BlobError]) =
      auth.authorizationHeader.map { authHeader =>
        send(authHeader).either.map {
          case Right(resp) =>
            val code = resp.statusCode()
            if code == 401 && !authRefreshed && config.credentials != GcsCredentials.Anonymous then
              auth.invalidate.andThen(loop(attempt, authRefreshed = true))
            else if GcsBlobStorage.isRetryableStatus(code) && attempt < config.maxRetries then
              Async.sleep(GcsBlobStorage.retryDelay(attempt)).andThen(loop(attempt + 1, authRefreshed))
            else resp
          case Left(err) =>
            if attempt < config.maxRetries then
              Async.sleep(GcsBlobStorage.retryDelay(attempt)).andThen(loop(attempt + 1, authRefreshed))
            else Abort.fail(err)
        }
      }

    loop(attempt = 0, authRefreshed = false)

object GcsBlobStorage:

  private[blob] final case class GcsObjectMetadata(
      name: Option[String] = None,
      contentType: Option[String] = None,
      metadata: Option[Map[String, String]] = None,
  )
  private[blob] given Schema[GcsObjectMetadata] = Schema.derived

  /** Creates a [[GcsBlobStorage]] adapter with a shared `java.net.http.HttpClient`. */
  def make(
      config: GcsConfig,
      httpClient: JHttpClient = defaultHttpClient,
  )(using Frame): GcsBlobStorage < Sync =
    GcsAuth.make(config.credentials, httpClient, config.requestTimeout).map(new GcsBlobStorage(config, _, httpClient))

  lazy val defaultHttpClient: JHttpClient =
    JHttpClient.newBuilder()
      .connectTimeout(JDuration.ofSeconds(10))
      .followRedirects(JHttpClient.Redirect.NORMAL)
      .build()

  private[blob] def isRetryableStatus(code: Int): Boolean =
    code == 408 || code == 429 || code == 500 || code == 502 || code == 503 || code == 504

  private[blob] def retryDelay(attempt: Int): kyo.Duration =
    val millis = math.min(2000L, 100L * (1L << attempt))
    millis.millis

  /** Encodes a GCS JSON API `uploadType=multipart` (`multipart/related`) payload. */
  private[blob] def buildMultipartBody(
      boundary: String,
      objectName: String,
      mimeType: String,
      sha256: String,
      bytes: Chunk[Byte],
  )(using Frame): Array[Byte] =
    val metaJson = Json.encode(
      GcsObjectMetadata(
        name = Some(objectName),
        contentType = Some(mimeType),
        metadata = Some(Map("sha256" -> sha256)),
      )
    )
    val out = new ByteArrayOutputStream(metaJson.length + bytes.size + 256)
    def writeUtf8(s: String): Unit = out.write(s.getBytes(StandardCharsets.UTF_8))
    writeUtf8(s"--$boundary\r\n")
    writeUtf8("Content-Type: application/json; charset=UTF-8\r\n\r\n")
    writeUtf8(metaJson)
    writeUtf8(s"\r\n--$boundary\r\n")
    writeUtf8(s"Content-Type: $mimeType\r\n\r\n")
    out.write(bytes.toArray)
    writeUtf8(s"\r\n--$boundary--\r\n")
    out.toByteArray
