package mpbuilder.app

import kyo.*
import mpbuilder.app.http.HttpConfig
import mpbuilder.platform.db.DbConfig
import mpbuilder.shared.Effects

import java.nio.file.{Path, Paths}

enum StorageMode:
  /** Zero-DB local API dev: the in-memory `SampleShowcaseRuntime` behind HTTP. */
  case Memory
  /** Production: PostgreSQL (kyo-sql) + local-filesystem blob storage. */
  case Postgres

final case class AppConfig(
    storage: StorageMode,
    http: HttpConfig,
    db: Option[DbConfig],
    blobDir: Path,
    /** Public URL prefix under which stored blobs are served (`GET /api/v1/blobs/<key>`). */
    blobPublicBaseUrl: String,
    seed: Boolean,
)

object AppConfig:

  val usage: String =
    """Usage: backend.app.run [--storage=memory|postgres] [--port=8080] [--host=0.0.0.0]
      |                       [--blob-dir=./data/blobs] [--cors=https://shop.example] [--no-seed]
      |
      |Environment (flags win): STORAGE, PORT, HOST, BLOB_DIR, BLOB_PUBLIC_BASE_URL, CORS_ORIGINS (`*` or one origin),
      |                         DATABASE_URL (postgresql://host:5432/db), DATABASE_USER, DATABASE_PASSWORD, DATABASE_POOL_SIZE
      |""".stripMargin

  def load(args: Seq[String])(using Frame): AppConfig < (Sync & Abort[String]) =
    val flags: Map[String, String] = args.toList.flatMap { a =>
      a.stripPrefix("--").split("=", 2).toList match
        case k :: v :: Nil => Some(k -> v)
        case k :: Nil      => Some(k -> "true")
        case _             => None
    }.toMap

    Sync.defer(sys.env).map { env =>
      def setting(flag: String, envName: String, default: => String): String =
        flags.get(flag).orElse(env.get(envName)).getOrElse(default)

      val storage: StorageMode < Abort[String] = setting("storage", "STORAGE", "memory") match
        case "memory" | "mem"  => StorageMode.Memory
        case "postgres" | "pg" => StorageMode.Postgres
        case other             => Abort.fail(s"Unknown --storage=$other\n$usage")
      val rawPort                      = setting("port", "PORT", "8080")
      val port: Int < Abort[String]    = rawPort.toIntOption.fold(Abort.fail(s"Invalid port '$rawPort'"))(identity)
      val cors: String < Abort[String] = setting("cors", "CORS_ORIGINS", "*").split(',').map(_.trim).filter(_.nonEmpty).toList match
        case Nil           => "*"
        case single :: Nil => single
        case many          => Abort.fail(s"CORS_ORIGINS accepts `*` or a single origin, got: ${many.mkString(", ")}")
      def db(mode: StorageMode): Option[DbConfig] < (Sync & Abort[String]) = mode match
        case StorageMode.Postgres => DbConfig.fromEnv.map(Some(_))
        case StorageMode.Memory   => None

      for
        _       <- Effects.failWhen(flags.contains("help"))(usage)
        storage <- storage
        port    <- port
        cors    <- cors
        db      <- db(storage)
      yield AppConfig(
        storage = storage,
        http = HttpConfig(host = setting("host", "HOST", "0.0.0.0"), port = port, corsOrigin = cors),
        db = db,
        blobDir = Paths.get(setting("blob-dir", "BLOB_DIR", "./data/blobs")),
        blobPublicBaseUrl = setting("blob-public-base-url", "BLOB_PUBLIC_BASE_URL", "/api/v1/blobs"),
        seed = !flags.contains("no-seed"),
      )
    }
