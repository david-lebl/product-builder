package mpbuilder.platform

import mpbuilder.platform.blob.LocalFileBlobStorage
import mpbuilder.platform.db.*
import mpbuilder.shared.*
import zio.*
import zio.test.*

import java.nio.file.Files

object PlatformSpec extends ZIOSpecDefault:

  def spec = suite("platform")(
    suite("LocalFileBlobStorage")(
      test("stores, reads back (with checksum & mime) and deletes blobs") {
        val root  = Files.createTempDirectory("blobs-")
        val store = new LocalFileBlobStorage(root, Some("/api/v1/blobs"))
        val bytes = Chunk.fromArray("%PDF-1.7 hello".getBytes("UTF-8"))
        for
          meta <- store.put("uploads/ord-1/art-1-flyer.pdf", bytes, "application/pdf")
          back <- store.get("uploads/ord-1/art-1-flyer.pdf")
          _    <- store.delete("uploads/ord-1/art-1-flyer.pdf")
          gone <- store.get("uploads/ord-1/art-1-flyer.pdf")
        yield assertTrue(
          meta.publicUrl.contains("/api/v1/blobs/uploads/ord-1/art-1-flyer.pdf"),
          meta.checksumSha256 == Sha256.hex(bytes.toArray),
          back.map(_.bytes).contains(bytes),
          back.map(_.meta.mimeType).contains("application/pdf"),
          gone.isEmpty,
        )
      },
      test("rejects path traversal keys") {
        val store = new LocalFileBlobStorage(Files.createTempDirectory("blobs-"), None)
        for
          r1 <- store.put("../etc/passwd", Chunk.single(1.toByte), "text/plain").either
          r2 <- store.get("/absolute").either
          r3 <- store.get("a/../../b").either
        yield assertTrue(r1.isLeft, r2.isLeft, r3.isLeft)
      },
    ),
    suite("PostgreSQL")(
      test("platform migrations create a restart-safe id sequence starting at 10000") {
        for
          seq <- ZIO.serviceWith[Database](new PostgresIdSequence(_))
          a   <- seq.next
          b   <- seq.next
        yield assertTrue(a == 10000L, b == 10001L)
      }
    ).provideShared(PostgresTestSupport.database(Nil)) @@ TestAspect.withLiveClock,
  ) @@ TestAspect.sequential
