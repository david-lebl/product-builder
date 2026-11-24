package mpbuilder.app

import kyo.*
import kyo.test.*
import mpbuilder.platform.blob.GcsCredentials
import mpbuilder.shared.*

class AppConfigSpec extends Test[Any]:

  "AppConfig" - {
    "defaults to memory blobs in memory mode and local directory blobs in postgres mode" in {
      for
        mem <- AppConfig.loadFrom(Nil, Map.empty)
        pg  <- AppConfig.loadFrom(Seq("--storage=postgres"), Map("DATABASE_URL" -> "postgresql://localhost:5432/printcraft"))
      yield
        assert(mem.storage == StorageMode.Memory)
        assert(mem.blobStorage == BlobStorageConfig.Memory)
        assert(mem.blobPublicBaseUrl == "/api/v1/blobs")
        assert(pg.storage == StorageMode.Postgres)
        assert(pg.blobStorage.isInstanceOf[BlobStorageConfig.Local])
    }

    "allows overriding blob storage independently of DB storage mode" in {
      for
        memLocal <- AppConfig.loadFrom(Seq("--storage=memory", "--blob-storage=local", "--blob-dir=/tmp/blobs"), Map.empty)
        pgMem    <- AppConfig.loadFrom(
                      Seq("--storage=postgres", "--blob-storage=memory"),
                      Map("DATABASE_URL" -> "postgresql://localhost:5432/printcraft"),
                    )
      yield
        assert(memLocal.blobStorage == BlobStorageConfig.Local(java.nio.file.Paths.get("/tmp/blobs")))
        assert(pgMem.blobStorage == BlobStorageConfig.Memory)
    }

    "loads GCS blob storage from flags and environment" in {
      for
        cfg <- AppConfig.loadFrom(
                 args = Seq("--blob-storage=gcs", "--gcs-bucket=printcraft-prod", "--gcs-prefix=artwork"),
                 env = Map("GCS_ENDPOINT" -> "http://127.0.0.1:4443"),
               )
      yield cfg.blobStorage match
        case BlobStorageConfig.Gcs(gcs) =>
          assert(gcs.bucket == "printcraft-prod")
          assert(gcs.keyPrefix.contains("artwork"))
          assert(gcs.endpoint == "http://127.0.0.1:4443")
          assert(gcs.credentials == GcsCredentials.Anonymous)
        case other =>
          fail(s"Expected GCS blob storage config, got $other")
    }

    "fails when --blob-storage=gcs is set without GCS_BUCKET" in {
      for res <- AppConfig.loadFrom(Seq("--blob-storage=gcs"), Map.empty).either
      yield assert(res.isLeft)
    }

    "fails on unknown --blob-storage" in {
      for res <- AppConfig.loadFrom(Seq("--blob-storage=ftp"), Map.empty).either
      yield assert(res.isLeft)
    }
  }
