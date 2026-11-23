package mpbuilder.app

import mpbuilder.app.http.HttpConfig
import mpbuilder.platform.db.DbConfig
import zio.*

import java.nio.file.{Path, Paths}

enum StorageMode:
  /** Zero-DB local API dev: the in-memory `SampleShowcaseRuntime` behind HTTP. */
  case Memory
  /** Production: PostgreSQL (Magnum + Flyway) + local-filesystem blob storage. */
  case Postgres

final case class AppConfig(
    storage: StorageMode,
    http: HttpConfig,
    db: Option[DbConfig],
    blobDir: Path,
    /** Public URL prefix under which stored blobs are served (see `SystemEndpoints.downloadBlob`). */
    blobPublicBaseUrl: String,
    seed: Boolean,
)

object AppConfig:

  val usage: String =
    """Usage: backend.app.run [--storage=memory|postgres] [--port=8080] [--host=0.0.0.0]
      |                       [--blob-dir=./data/blobs] [--cors=https://a.example,https://b.example] [--no-seed]
      |
      |Environment (flags win): STORAGE, PORT, HOST, BLOB_DIR, BLOB_PUBLIC_BASE_URL, CORS_ORIGINS,
      |                         DATABASE_URL (jdbc:postgresql://…), DATABASE_USER, DATABASE_PASSWORD, DATABASE_POOL_SIZE
      |""".stripMargin

  def load(args: Chunk[String]): IO[String, AppConfig] =
    val flags: Map[String, String] = args.toList.flatMap { a =>
      a.stripPrefix("--").split("=", 2).toList match
        case k :: v :: Nil => Some(k -> v)
        case k :: Nil      => Some(k -> "true")
        case _             => None
    }.toMap

    def setting(flag: String, env: String, default: => String): UIO[String] =
      flags.get(flag) match
        case Some(v) => ZIO.succeed(v)
        case None    => System.envOrElse(env, default).orDie

    for
      _        <- ZIO.fail(usage).when(flags.contains("help"))
      storage  <- setting("storage", "STORAGE", "memory").flatMap {
                    case "memory" | "mem"     => ZIO.succeed(StorageMode.Memory)
                    case "postgres" | "pg"    => ZIO.succeed(StorageMode.Postgres)
                    case other                => ZIO.fail(s"Unknown --storage=$other\n$usage")
                  }
      port     <- setting("port", "PORT", "8080").flatMap(p => ZIO.fromOption(p.toIntOption).orElseFail(s"Invalid port '$p'"))
      host     <- setting("host", "HOST", "0.0.0.0")
      cors     <- setting("cors", "CORS_ORIGINS", "*")
      blobDir  <- setting("blob-dir", "BLOB_DIR", "./data/blobs")
      blobBase <- setting("blob-public-base-url", "BLOB_PUBLIC_BASE_URL", "/api/v1/blobs")
      db       <- storage match
                    case StorageMode.Postgres => DbConfig.fromEnv.map(Some(_))
                    case StorageMode.Memory   => ZIO.none
    yield AppConfig(
      storage = storage,
      http = HttpConfig(host = host, port = port, corsOrigins = cors.split(',').map(_.trim).filter(_.nonEmpty).toList),
      db = db,
      blobDir = Paths.get(blobDir),
      blobPublicBaseUrl = blobBase,
      seed = !flags.contains("no-seed"),
    )
