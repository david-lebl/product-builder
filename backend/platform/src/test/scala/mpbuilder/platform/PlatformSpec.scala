package mpbuilder.platform

import kyo.*
import kyo.test.*
import mpbuilder.platform.blob.LocalFileBlobStorage
import mpbuilder.platform.db.*
import mpbuilder.shared.*

import java.nio.file.Files

class PlatformSpec extends Test[Any]:

  "platform" - {
    "LocalFileBlobStorage" - {
      "stores, reads back (with checksum & mime) and deletes blobs" in {
        val root  = Files.createTempDirectory("blobs-")
        val store = new LocalFileBlobStorage(root, Some("/api/v1/blobs"))
        val bytes = Chunk.from("%PDF-1.7 hello".getBytes("UTF-8"))
        for
          meta <- store.put("uploads/ord-1/art-1-flyer.pdf", bytes, "application/pdf")
          back <- store.get("uploads/ord-1/art-1-flyer.pdf")
          _    <- store.delete("uploads/ord-1/art-1-flyer.pdf")
          gone <- store.get("uploads/ord-1/art-1-flyer.pdf")
        yield
          assert(meta.publicUrl.contains("/api/v1/blobs/uploads/ord-1/art-1-flyer.pdf"))
          assert(meta.checksumSha256 == Sha256.hex(bytes.toArray))
          assert(back.map(_.bytes).contains(bytes))
          assert(back.map(_.meta.mimeType).contains("application/pdf"))
          assert(gone.isEmpty)
      }

      "rejects path traversal keys" in {
        val store = new LocalFileBlobStorage(Files.createTempDirectory("blobs-"), None)
        for
          r1 <- store.put("../etc/passwd", Chunk(1.toByte), "text/plain").either
          r2 <- store.get("/absolute").either
          r3 <- store.get("a/../../b").either
        yield
          assert(r1.isLeft)
          assert(r2.isLeft)
          assert(r3.isLeft)
      }
    }

    "PostgreSQL (kyo-sql)" - {
      "platform migrations create a restart-safe id sequence starting at 10000" in {
        for
          db <- PostgresTestSupport.database(Nil)
          seq = new PostgresIdSequence(db)
          a  <- seq.next
          b  <- seq.next
        yield
          assert(a == 10000L)
          assert(b == 10001L)
      }

      "migrations are recorded once and are idempotent on re-run" in {
        for
          db      <- PostgresTestSupport.database(Nil)
          _       <- Migrations.run(db, List(Migrations.platform))
          applied <- db.run(sql"SELECT count(*) FROM platform.schema_migrations".as[Long].run.map(_.head))
        yield assert(applied == 1L)
      }

      "DbConfig turns legacy JDBC URLs into kyo-sql URLs with credentials" in {
        val cfg = DbConfig("jdbc:postgresql://localhost:5432/printcraft", "user", "p@ss", 4)
        assert(cfg.connectionUrl == "postgresql://user:p%40ss@localhost:5432/printcraft")
        assert(DbConfig("postgresql://a:b@h:1/d", "x", "y").connectionUrl == "postgresql://a:b@h:1/d")
      }
    }
  }
