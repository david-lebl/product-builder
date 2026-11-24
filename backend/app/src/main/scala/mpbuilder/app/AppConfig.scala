package mpbuilder.app

import kyo.*
import mpbuilder.app.http.HttpConfig
import mpbuilder.platform.blob.GcsConfig
import mpbuilder.platform.db.DbConfig
import mpbuilder.shared.Effects

import java.nio.file.{Path, Paths}

enum StorageMode:
  /** Zero-DB local API dev: the in-memory `SampleShowcaseRuntime` behind HTTP. */
  case Memory
  /** Production: PostgreSQL (kyo-sql) + configured blob storage. */
  case Postgres

enum BlobStorageConfig:
  /** In-memory `BlobStorage.inMemory` (default for `--storage=memory`). */
  case Memory
  /** Local filesystem directory (`LocalFileBlobStorage`; default for `--storage=postgres`). */
  case Local(dir: Path)
  /** Google Cloud Storage bucket (`GcsBlobStorage`). */
  case Gcs(config: GcsConfig)

  def label: String = this match
    case Memory   => "memory"
    case Local(d) => s"local($d)"
    case Gcs(c)   => s"gcs(${c.bucket})"

final case class AppConfig(
    storage: StorageMode,
    http: HttpConfig,
    db: Option[DbConfig],
    blobStorage: BlobStorageConfig,
    /** Public URL prefix under which stored blobs are served (`GET /api/v1/blobs/<key>`). */
    blobPublicBaseUrl: String,
    seed: Boolean,
)

object AppConfig:

  val usage: String =
    """Usage: backend.app.run [--storage=memory|postgres] [--port=8080] [--host=0.0.0.0]
      |                       [--blob-storage=memory|local|gcs] [--blob-dir=./data/blobs]
      |                       [--gcs-bucket=<name>] [--gcs-prefix=<prefix>] [--gcs-endpoint=<url>]
      |                       [--gcs-credentials=<path>] [--cors=https://shop.example] [--no-seed]
      |
      |Environment (flags win): STORAGE, PORT, HOST, BLOB_STORAGE (memory|local|gcs), BLOB_DIR, BLOB_PUBLIC_BASE_URL,
      |                         GCS_BUCKET, GCS_KEY_PREFIX, GCS_ENDPOINT, GCS_CREDENTIALS_FILE / GOOGLE_APPLICATION_CREDENTIALS,
      |                         GCS_ACCESS_TOKEN, CORS_ORIGINS (`*` or one origin),
      |                         DATABASE_URL (postgresql://host:5432/db), DATABASE_USER, DATABASE_PASSWORD, DATABASE_POOL_SIZE
      |""".stripMargin

  def load(args: Seq[String])(using Frame): AppConfig < (Sync & Abort[String]) =
    Sync.defer(sys.env).map(loadFrom(args, _))

  def loadFrom(args: Seq[String], env: Map[String, String])(using Frame): AppConfig < (Sync & Abort[String]) =
    val flags: Map[String, String] = args.toList.flatMap { a =>
      a.stripPrefix("--").split("=", 2).toList match
        case k :: v :: Nil => Some(k -> v)
        case k :: Nil      => Some(k -> "true")
        case _             => None
    }.toMap

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
    def db(mode: StorageMode): Option[DbConfig] < Abort[String] = mode match
      case StorageMode.Postgres => DbConfig.fromMap(env).map(Some(_))
      case StorageMode.Memory   => None
    def blobStorageFor(mode: StorageMode): BlobStorageConfig < (Sync & Abort[String]) =
      val defaultBackend = mode match
        case StorageMode.Memory   => "memory"
        case StorageMode.Postgres => "local"
      setting("blob-storage", "BLOB_STORAGE", defaultBackend).toLowerCase match
        case "memory" | "mem" =>
          BlobStorageConfig.Memory
        case "local" | "file" | "fs" =>
          BlobStorageConfig.Local(Paths.get(setting("blob-dir", "BLOB_DIR", "./data/blobs")))
        case "gcs" | "google-cloud-storage" =>
          GcsConfig.fromMap(env, flags).map(BlobStorageConfig.Gcs(_))
        case other =>
          Abort.fail(s"Unknown --blob-storage=$other\n$usage")

    for
      _           <- Effects.failWhen(flags.contains("help"))(usage)
      storage     <- storage
      port        <- port
      cors        <- cors
      db          <- db(storage)
      blobStorage <- blobStorageFor(storage)
    yield AppConfig(
      storage = storage,
      http = HttpConfig(host = setting("host", "HOST", "0.0.0.0"), port = port, corsOrigin = cors),
      db = db,
      blobStorage = blobStorage,
      blobPublicBaseUrl = setting("blob-public-base-url", "BLOB_PUBLIC_BASE_URL", "/api/v1/blobs"),
      seed = !flags.contains("no-seed"),
    )
