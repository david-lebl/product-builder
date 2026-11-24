package mpbuilder.shared

import kyo.*

import java.nio.charset.StandardCharsets
import java.util.Base64

// ── Blob Storage Port (cross-platform, transport/vendor neutral) ─────────────

/** Metadata of a stored binary object (artwork print files, previews, …). */
final case class StoredBlob(
    key: String,
    mimeType: String,
    sizeBytes: Long,
    checksumSha256: String,
    /** Absolute or root-relative URL that serves this blob, when the adapter exposes one
      * (e.g. `/api/v1/blobs/<key>` for the JVM local-filesystem adapter). `None` for the
      * browser in-memory adapter, where callers keep their own `data:`/`blob:` URL.
      */
    publicUrl: Option[String],
)

final case class BlobContent(meta: StoredBlob, bytes: Chunk[Byte])

enum BlobError(val message: String):
  case InvalidKey(key: String)          extends BlobError(s"Invalid blob storage key: '$key'")
  case InvalidDataUrl(reason: String)   extends BlobError(s"Invalid data URL: $reason")
  case TooLarge(size: Long, max: Long)  extends BlobError(s"Blob of $size bytes exceeds the maximum of $max bytes")
  case StorageFailure(detail: String)   extends BlobError(s"Blob storage failure: $detail")

/** Outbound port for binary object storage.
  *
  * Adapters:
  *   - [[BlobStorage.inMemory]] — cross-platform `Ref`-backed store (static browser demo & tests)
  *   - `mpbuilder.platform.blob.LocalFileBlobStorage` — JVM local filesystem (dev server); S3/R2 later
  */
trait BlobStorage:
  def put(key: String, bytes: Chunk[Byte], mimeType: String)(using Frame): StoredBlob < (Async & Abort[BlobError])
  def get(key: String)(using Frame): Option[BlobContent] < (Async & Abort[BlobError])
  def delete(key: String)(using Frame): Unit < (Async & Abort[BlobError])

object BlobStorage:

  val DefaultMaxBytes: Long = 50L * 1024 * 1024

  private val KeyPattern = "^[A-Za-z0-9][A-Za-z0-9._/-]{0,511}$".r

  /** Keys are relative, slash-separated paths without `..` segments (safe for filesystem & S3). */
  def validateKey(key: String): Either[BlobError, String] =
    val segments = key.split('/')
    if KeyPattern.matches(key) && !segments.exists(s => s.isEmpty || s == "." || s == "..") then Right(key)
    else Left(BlobError.InvalidKey(key))

  def describe(key: String, bytes: Chunk[Byte], mimeType: String, publicUrl: Option[String]): StoredBlob =
    StoredBlob(key, mimeType, bytes.size.toLong, Sha256.hex(bytes.toArray), publicUrl)

  /** Cross-platform in-memory adapter. */
  def inMemory(
      publicBaseUrl: Option[String] = None,
      maxBytes: Long = DefaultMaxBytes,
  )(using Frame): BlobStorage < Sync =
    AtomicRef.init(Map.empty[String, BlobContent]).map(new InMemoryBlobStorage(_, publicBaseUrl, maxBytes))

final class InMemoryBlobStorage(
    ref: AtomicRef[Map[String, BlobContent]],
    publicBaseUrl: Option[String],
    maxBytes: Long,
) extends BlobStorage:

  override def put(key: String, bytes: Chunk[Byte], mimeType: String)(using Frame): StoredBlob < (Async & Abort[BlobError]) =
    for
      k   <- BlobStorage.validateKey(key).orAbort
      _   <- Effects.failWhen(bytes.size.toLong > maxBytes)(BlobError.TooLarge(bytes.size.toLong, maxBytes))
      meta = BlobStorage.describe(k, bytes, mimeType, publicBaseUrl.map(base => s"${base.stripSuffix("/")}/$k"))
      _   <- ref.update(_ + (k -> BlobContent(meta, bytes)))
    yield meta

  override def get(key: String)(using Frame): Option[BlobContent] < (Async & Abort[BlobError]) =
    BlobStorage.validateKey(key).orAbort.map(k => ref.get.map(_.get(k)))

  override def delete(key: String)(using Frame): Unit < (Async & Abort[BlobError]) =
    BlobStorage.validateKey(key).orAbort.map(k => ref.update(_ - k))

// ── `data:` URL support ──────────────────────────────────────────────────────

final case class DecodedDataUrl(mimeType: String, bytes: Chunk[Byte])

object DataUrl:

  def isDataUrl(raw: String): Boolean = raw.startsWith("data:")

  /** Parses RFC 2397 `data:[<mime>][;base64],<payload>` URLs. */
  def decode(raw: String): Either[BlobError, DecodedDataUrl] =
    if !isDataUrl(raw) then Left(BlobError.InvalidDataUrl("missing 'data:' scheme"))
    else
      val comma = raw.indexOf(',')
      if comma < 0 then Left(BlobError.InvalidDataUrl("missing ',' separator"))
      else
        val header   = raw.substring(5, comma)
        val payload  = raw.substring(comma + 1)
        val parts    = header.split(';').toList
        val isBase64 = parts.contains("base64")
        val mime     = parts.headOption.filter(p => p.nonEmpty && p != "base64").getOrElse("text/plain")
        try
          val bytes =
            if isBase64 then Base64.getDecoder.decode(payload.filterNot(_.isWhitespace))
            else percentDecode(payload)
          Right(DecodedDataUrl(mime, Chunk.from(bytes)))
        catch case e: IllegalArgumentException => Left(BlobError.InvalidDataUrl(e.getMessage))

  def encode(mimeType: String, bytes: Chunk[Byte]): String =
    s"data:$mimeType;base64,${Base64.getEncoder.encodeToString(bytes.toArray)}"

  private def percentDecode(s: String): Array[Byte] =
    val out = new java.io.ByteArrayOutputStream(s.length)
    var i   = 0
    while i < s.length do
      val c = s.charAt(i)
      if c == '%' && i + 2 < s.length then
        out.write(Integer.parseInt(s.substring(i + 1, i + 3), 16))
        i += 3
      else
        val b = c.toString.getBytes(StandardCharsets.UTF_8)
        out.write(b, 0, b.length)
        i += 1
    out.toByteArray

/** Result of moving inline (`data:` URL) artwork payloads into [[BlobStorage]]. */
final case class IngestedAsset(
    downloadUrl: Option[String],
    previewUrl: Option[String],
    sizeBytes: Option[Long],
    checksumSha256: Option[String],
)

object BlobIngest:

  /** Moves `data:` URL payloads into blob storage under `storageKey` (and `<storageKey>.preview`).
    *
    * Non-`data:` URLs (http(s), `blob:`) pass through untouched. When the adapter exposes a
    * `publicUrl`, the inline `data:` URL is replaced by it so large payloads never end up in
    * aggregates / database rows; otherwise (browser in-memory mode) the original URL is kept.
    */
  def ingest(
      blobs: BlobStorage,
      storageKey: String,
      downloadUrl: Option[String],
      previewUrl: Option[String],
  )(using Frame): IngestedAsset < (Async & Abort[BlobError]) =
    def store(key: String, url: Option[String]): (Option[String], Option[StoredBlob]) < (Async & Abort[BlobError]) =
      url match
        case Some(u) if DataUrl.isDataUrl(u) =>
          for
            decoded <- DataUrl.decode(u).orAbort
            meta    <- blobs.put(key, decoded.bytes, decoded.mimeType)
          yield (Some(meta.publicUrl.getOrElse(u)), Some(meta))
        case other => (other, None)

    val previewIsSameAsDownload = previewUrl.isDefined && previewUrl == downloadUrl
    for
      main <- store(storageKey, downloadUrl)
      prev <- (if previewIsSameAsDownload then (main._1, None)
               else store(s"$storageKey.preview", previewUrl)): (Option[String], Option[StoredBlob]) < (Async & Abort[BlobError])
    yield IngestedAsset(main._1, prev._1, main._2.map(_.sizeBytes), main._2.map(_.checksumSha256))

// ── Monotonic ID sequence port ───────────────────────────────────────────────

/** Source of unique, monotonically increasing numbers for human-friendly IDs (`ord-N`, `ORD-YYYY-NNNN`).
  * In-memory for the static demo; backed by a PostgreSQL `SEQUENCE` in persistent mode so IDs never
  * collide across server restarts.
  */
trait IdSequence:
  def next(using Frame): Long < Async

object IdSequence:
  def inMemory(start: Long = 0L)(using Frame): IdSequence < Sync =
    AtomicLong.init(start).map(counter => new IdSequence { def next(using Frame): Long < Async = counter.incrementAndGet })
