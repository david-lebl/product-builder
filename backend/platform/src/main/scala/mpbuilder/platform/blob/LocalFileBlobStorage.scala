package mpbuilder.platform
package blob

import mpbuilder.shared.*
import kyo.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}

/** JVM local-filesystem `BlobStorage` adapter for dev/staging (S3 / Cloudflare R2 later).
  *
  * Layout: `<root>/<key>` holds the bytes, `<root>/<key>.meta` a tiny `mime\nsha256` sidecar.
  * Keys are validated by [[BlobStorage.validateKey]] and resolved strictly inside `root`.
  */
final class LocalFileBlobStorage(
    root: Path,
    publicBaseUrl: Option[String],
    maxBytes: Long = BlobStorage.DefaultMaxBytes,
) extends BlobStorage:

  private val normalizedRoot = root.toAbsolutePath.normalize()

  private def resolve(key: String)(using Frame): Path < Abort[BlobError] =
    BlobStorage.validateKey(key).orAbort.map { k =>
      val p = normalizedRoot.resolve(k).normalize()
      if p.startsWith(normalizedRoot) then p else Abort.fail(BlobError.InvalidKey(key))
    }

  /** Runs blocking file I/O, mapping I/O failures to [[BlobError.StorageFailure]]. */
  private def io[A](thunk: => A)(using Frame): A < (Sync & Abort[BlobError]) =
    Sync.defer(Abort.catching[Exception](thunk)).mapAbort(e => BlobError.StorageFailure(e.getMessage))

  private def metaPath(p: Path): Path = p.resolveSibling(p.getFileName.toString + ".meta")

  private def publicUrlFor(key: String): Option[String] =
    publicBaseUrl.map(base => s"${base.stripSuffix("/")}/$key")

  override def put(key: String, bytes: Chunk[Byte], mimeType: String)(using Frame): StoredBlob < (Async & Abort[BlobError]) =
    for
      path <- resolve(key)
      _    <- Effects.failWhen(bytes.size.toLong > maxBytes)(BlobError.TooLarge(bytes.size.toLong, maxBytes))
      meta  = BlobStorage.describe(key, bytes, mimeType, publicUrlFor(key))
      _    <- io {
                  Files.createDirectories(path.getParent)
                  val tmp = Files.createTempFile(path.getParent, ".upload-", ".tmp")
                  Files.write(tmp, bytes.toArray)
                  Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                  Files.writeString(metaPath(path), s"${meta.mimeType}\n${meta.checksumSha256}", StandardCharsets.UTF_8)
                }
    yield meta

  override def get(key: String)(using Frame): Option[BlobContent] < (Async & Abort[BlobError]) =
    resolve(key).map { path =>
      io {
          if !Files.isRegularFile(path) then None
          else
            val bytes = Chunk.from(Files.readAllBytes(path))
            val mp    = metaPath(path)
            val (mime, sha) =
              if Files.isRegularFile(mp) then
                Files.readString(mp, StandardCharsets.UTF_8).split('\n').toList match
                  case m :: s :: _ => (m, s)
                  case m :: Nil    => (m, Sha256.hex(bytes.toArray))
                  case Nil         => ("application/octet-stream", Sha256.hex(bytes.toArray))
              else ("application/octet-stream", Sha256.hex(bytes.toArray))
            Some(BlobContent(StoredBlob(key, mime, bytes.size.toLong, sha, publicUrlFor(key)), bytes))
      }
    }

  override def delete(key: String)(using Frame): Unit < (Async & Abort[BlobError]) =
    resolve(key).map { path =>
      io {
        Files.deleteIfExists(path)
        Files.deleteIfExists(metaPath(path))
        ()
      }
    }
