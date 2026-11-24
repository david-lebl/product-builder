package mpbuilder.platform
package blob

import kyo.*
import mpbuilder.shared.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.time.Duration

/** Authentication modes supported by [[GcsBlobStorage]]. */
enum GcsCredentials:
  /** No `Authorization` header (for local emulators such as `fake-gcs-server` or public buckets). */
  case Anonymous

  /** Static OAuth2 bearer token. */
  case AccessToken(token: String)

  /** Google Cloud service account key (`"type": "service_account"`). Signs RS256 JWT assertions. */
  case ServiceAccount(
      clientEmail: String,
      privateKeyPem: String,
      tokenUri: String = GcsConfig.DefaultTokenUri,
  )

  /** User credentials created by `gcloud auth application-default login` (`"type": "authorized_user"`). */
  case AuthorizedUser(
      clientId: String,
      clientSecret: String,
      refreshToken: String,
      tokenUri: String = GcsConfig.DefaultTokenUri,
  )

  /** GCE / GKE / Cloud Run instance metadata server. */
  case MetadataServer(url: String = GcsConfig.DefaultMetadataTokenUrl)

object GcsCredentials:

  private final case class CredentialJson(
      `type`: Option[String] = None,
      client_email: Option[String] = None,
      private_key: Option[String] = None,
      token_uri: Option[String] = None,
      client_id: Option[String] = None,
      client_secret: Option[String] = None,
      refresh_token: Option[String] = None,
  )

  private given Schema[CredentialJson] = Schema.derived

  /** Parses a Google Cloud credentials JSON document (`service_account` or `authorized_user`). */
  def fromJson(raw: String)(using Frame): Either[String, GcsCredentials] =
    Json.decode[CredentialJson](raw).toEither.left.map(e => s"Invalid GCS credentials JSON: ${e.getMessage}").flatMap { doc =>
      doc.`type` match
        case Some("service_account") =>
          for
            email <- doc.client_email.filter(_.nonEmpty).toRight("GCS service account JSON missing 'client_email'")
            key   <- doc.private_key.filter(_.nonEmpty).toRight("GCS service account JSON missing 'private_key'")
          yield GcsCredentials.ServiceAccount(
            clientEmail = email,
            privateKeyPem = key,
            tokenUri = doc.token_uri.filter(_.nonEmpty).getOrElse(GcsConfig.DefaultTokenUri),
          )
        case Some("authorized_user") =>
          for
            cid     <- doc.client_id.filter(_.nonEmpty).toRight("GCS authorized_user JSON missing 'client_id'")
            csecret <- doc.client_secret.filter(_.nonEmpty).toRight("GCS authorized_user JSON missing 'client_secret'")
            refresh <- doc.refresh_token.filter(_.nonEmpty).toRight("GCS authorized_user JSON missing 'refresh_token'")
          yield GcsCredentials.AuthorizedUser(
            clientId = cid,
            clientSecret = csecret,
            refreshToken = refresh,
            tokenUri = doc.token_uri.filter(_.nonEmpty).getOrElse(GcsConfig.DefaultTokenUri),
          )
        case Some(other) =>
          Left(s"Unsupported GCS credential type '$other' (supported: service_account, authorized_user)")
        case None =>
          Left("GCS credentials JSON missing 'type' field")
    }

  /** Reads and parses a Google Cloud credentials JSON file from disk. */
  def fromFile(path: Path)(using Frame): GcsCredentials < (Sync & Abort[String]) =
    Sync.defer {
      if !Files.isRegularFile(path) then Left(s"GCS credentials file does not exist: $path")
      else
        try fromJson(Files.readString(path, StandardCharsets.UTF_8))
        catch case e: Exception => Left(s"Failed to read GCS credentials file '$path': ${e.getMessage}")
    }.map(_.orAbort)

  /** Resolves Google Application Default Credentials (ADC):
    *   1. `GOOGLE_APPLICATION_CREDENTIALS` environment variable
    *   2. gcloud CLI well-known path (`$CLOUDSDK_CONFIG/application_default_credentials.json` or
    *      `~/.config/gcloud/application_default_credentials.json`)
    *   3. GCE / GKE / Cloud Run metadata server
    */
  def applicationDefault(env: Map[String, String])(using Frame): GcsCredentials < (Sync & Abort[String]) =
    env.get("GOOGLE_APPLICATION_CREDENTIALS").map(_.trim).filter(_.nonEmpty) match
      case Some(path) => fromFile(Paths.get(path))
      case None =>
        val gcloudDir = env.get("CLOUDSDK_CONFIG").map(_.trim).filter(_.nonEmpty)
          .map(Paths.get(_))
          .getOrElse(Paths.get(sys.props.getOrElse("user.home", ""), ".config", "gcloud"))
        val wellKnown = gcloudDir.resolve("application_default_credentials.json")
        Sync.defer(Files.isRegularFile(wellKnown)).map { exists =>
          (if exists then fromFile(wellKnown) else GcsCredentials.MetadataServer()): GcsCredentials < (Sync & Abort[String])
        }

/** Configuration for [[GcsBlobStorage]]. */
final case class GcsConfig(
    bucket: String,
    credentials: GcsCredentials = GcsCredentials.MetadataServer(),
    endpoint: String = GcsConfig.DefaultEndpoint,
    /** Optional object key prefix inside the bucket (e.g. `"prod/artwork"`). */
    keyPrefix: Option[String] = None,
    /** Public URL prefix returned in `StoredBlob.publicUrl` (e.g. `"/api/v1/blobs"` or CDN URL). */
    publicBaseUrl: Option[String] = None,
    maxBytes: Long = BlobStorage.DefaultMaxBytes,
    requestTimeout: Duration = Duration.ofSeconds(30),
    maxRetries: Int = 3,
):
  private val normalizedPrefix: Option[String] = GcsConfig.normalizePrefix(keyPrefix)
  val normalizedEndpoint: String               = endpoint.stripSuffix("/")

  /** Full GCS object name for a validated logical blob key. */
  def objectName(key: String): String =
    normalizedPrefix.fold(key)(p => s"$p/$key")

  def publicUrlFor(key: String): Option[String] =
    publicBaseUrl.map(base => s"${base.stripSuffix("/")}/$key")

object GcsConfig:

  val DefaultEndpoint: String         = "https://storage.googleapis.com"
  val DefaultTokenUri: String         = "https://oauth2.googleapis.com/token"
  val DefaultMetadataTokenUrl: String = "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token"
  val ScopeReadWrite: String          = "https://www.googleapis.com/auth/devstorage.read_write"

  private val BucketPattern = "^[a-z0-9][a-z0-9._-]{1,220}[a-z0-9]$".r

  def validateBucket(raw: String): Either[String, String] =
    val b = raw.trim
    if BucketPattern.matches(b) then Right(b)
    else Left(s"Invalid GCS bucket name '$raw' (must be 3-222 lowercase letters, digits, '.', '_', or '-')")

  def normalizePrefix(raw: Option[String]): Option[String] =
    raw.map(_.trim.stripPrefix("/").stripSuffix("/")).filter(_.nonEmpty)

  /** Loads GCS configuration from environment variables:
    *   - `GCS_BUCKET` (required)
    *   - `GCS_KEY_PREFIX` (optional)
    *   - `GCS_ENDPOINT` (optional, default `https://storage.googleapis.com`)
    *   - `GCS_ACCESS_TOKEN` (optional static bearer token)
    *   - `GCS_CREDENTIALS_FILE` or `GOOGLE_APPLICATION_CREDENTIALS` (optional path to credentials JSON)
    *   - `BLOB_PUBLIC_BASE_URL` (optional, default `/api/v1/blobs`)
    */
  def fromEnv(using Frame): GcsConfig < (Sync & Abort[String]) =
    Sync.defer(sys.env).map(fromMap(_, Map.empty))

  /** Loads GCS configuration from CLI flags (winning over environment variables). */
  def fromMap(env: Map[String, String], flags: Map[String, String])(using Frame): GcsConfig < (Sync & Abort[String]) =
    def opt(flag: String, envName: String): Option[String] =
      flags.get(flag).orElse(env.get(envName)).map(_.trim).filter(_.nonEmpty)

    val rawBucket   = opt("gcs-bucket", "GCS_BUCKET")
    val endpoint    = opt("gcs-endpoint", "GCS_ENDPOINT").getOrElse(DefaultEndpoint)
    val prefix      = normalizePrefix(opt("gcs-prefix", "GCS_KEY_PREFIX"))
    val publicBase  = opt("blob-public-base-url", "BLOB_PUBLIC_BASE_URL").orElse(Some("/api/v1/blobs"))
    val staticToken = opt("gcs-access-token", "GCS_ACCESS_TOKEN")
    val credFile    = opt("gcs-credentials", "GCS_CREDENTIALS_FILE")
    val isCustomEndpoint = endpoint.stripSuffix("/") != DefaultEndpoint

    val creds: GcsCredentials < (Sync & Abort[String]) =
      (staticToken, credFile) match
        case (Some(tok), _)  => GcsCredentials.AccessToken(tok)
        case (None, Some(f)) => GcsCredentials.fromFile(Paths.get(f))
        case (None, None) =>
          if isCustomEndpoint && !env.get("GOOGLE_APPLICATION_CREDENTIALS").exists(_.trim.nonEmpty) then
            GcsCredentials.Anonymous
          else GcsCredentials.applicationDefault(env)

    for
      bucket <- rawBucket.toRight("GCS_BUCKET (--gcs-bucket) is required when using GCS blob storage").flatMap(validateBucket).orAbort
      c      <- creds
    yield GcsConfig(
      bucket = bucket,
      credentials = c,
      endpoint = endpoint,
      keyPrefix = prefix,
      publicBaseUrl = publicBase,
    )
