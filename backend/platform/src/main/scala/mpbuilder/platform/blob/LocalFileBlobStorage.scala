package mpbuilder.platform
package blob

import mpbuilder.shared.*
import zio.*

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

  private def resolve(key: String): IO[BlobError, Path] =
    ZIO.fromEither(BlobStorage.validateKey(key)).flatMap { k =>
      val p = normalizedRoot.resolve(k).normalize()
      if p.startsWith(normalizedRoot) then ZIO.succeed(p) else ZIO.fail(BlobError.InvalidKey(key))
    }

  private def metaPath(p: Path): Path = p.resolveSibling(p.getFileName.toString + ".meta")

  private def publicUrlFor(key: String): Option[String] =
    publicBaseUrl.map(base => s"${base.stripSuffix("/")}/$key")

  override def put(key: String, bytes: Chunk[Byte], mimeType: String): IO[BlobError, StoredBlob] =
    for
      path <- resolve(key)
      _    <- ZIO.fail(BlobError.TooLarge(bytes.size.toLong, maxBytes)).when(bytes.size.toLong > maxBytes)
      meta  = BlobStorage.describe(key, bytes, mimeType, publicUrlFor(key))
      _    <- ZIO
                .attemptBlocking {
                  Files.createDirectories(path.getParent)
                  val tmp = Files.createTempFile(path.getParent, ".upload-", ".tmp")
                  Files.write(tmp, bytes.toArray)
                  Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                  Files.writeString(metaPath(path), s"${meta.mimeType}\n${meta.checksumSha256}", StandardCharsets.UTF_8)
                }
                .mapError(e => BlobError.StorageFailure(e.getMessage))
    yield meta

  override def get(key: String): IO[BlobError, Option[BlobContent]] =
    resolve(key).flatMap { path =>
      ZIO
        .attemptBlocking {
          if !Files.isRegularFile(path) then None
          else
            val bytes = Chunk.fromArray(Files.readAllBytes(path))
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
        .mapError(e => BlobError.StorageFailure(e.getMessage))
    }

  override def delete(key: String): IO[BlobError, Unit] =
    resolve(key).flatMap { path =>
      ZIO
        .attemptBlocking {
          Files.deleteIfExists(path)
          Files.deleteIfExists(metaPath(path))
          ()
        }
        .mapError(e => BlobError.StorageFailure(e.getMessage))
    }

object LocalFileBlobStorage:
  def layer(root: Path, publicBaseUrl: Option[String]): ULayer[BlobStorage] =
    ZLayer.succeed(new LocalFileBlobStorage(root, publicBaseUrl))
