package mpbuilder.platform
package db

import kyo.*

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Connection settings for the kyo-sql PostgreSQL pool.
  *
  * @param url
  *   `postgresql://host:port/db` (a legacy `jdbc:postgresql://…` URL is accepted too)
  */
final case class DbConfig(
    url: String,
    user: String,
    password: String,
    maxPoolSize: Int = 10,
):
  /** The kyo-sql connection URL (`postgresql://user:password@host:port/db`). */
  def connectionUrl: String =
    val bare = url.trim.stripPrefix("jdbc:")
    val schemeEnd = bare.indexOf("://")
    if schemeEnd < 0 || bare.substring(schemeEnd + 3).takeWhile(_ != '/').contains('@') then bare
    else
      def enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)
      s"${bare.substring(0, schemeEnd + 3)}${enc(user)}:${enc(password)}@${bare.substring(schemeEnd + 3)}"

object DbConfig:
  /** Reads `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_POOL_SIZE`. */
  def fromEnv(using Frame): DbConfig < (Sync & Abort[String]) =
    Sync.defer(sys.env).map { env =>
      env.get("DATABASE_URL").filter(_.trim.nonEmpty) match
        case None => Abort.fail("DATABASE_URL is not set (e.g. postgresql://localhost:5432/printcraft)")
        case Some(url) =>
          DbConfig(
            url = url,
            user = env.getOrElse("DATABASE_USER", "printcraft"),
            password = env.getOrElse("DATABASE_PASSWORD", "printcraft"),
            maxPoolSize = env.get("DATABASE_POOL_SIZE").flatMap(_.toIntOption).getOrElse(10),
          )
    }

/** Infrastructure-level database errors (confined to platform & `02-o-db` adapters). */
enum DbError(val message: String):
  case UniqueViolation(detail: String) extends DbError(s"Unique constraint violation: $detail")
  case MigrationFailed(detail: String) extends DbError(s"Database migration failed: $detail")
  case Unexpected(cause: Throwable)    extends DbError(s"Unexpected database error: ${cause.getMessage}")

object DbError:
  def fromSqlException(e: SqlException): DbError = e match
    case s: SqlServerException if s.sqlState == "23505" => UniqueViolation(s.getMessage)
    case other                                          => Unexpected(other)

/** Effect row of a statement block run against the pool. */
type DbIO[+A] = A < (DB & Abort[SqlException] & Async)

/** Thin, infallible-by-contract wrapper around a kyo-sql [[SqlClient]] pool.
  *
  * Repository ports in the domain cores are `A < Async` (persistence is not a business failure),
  * so unexpected SQL errors become panics (defects) here; adapters that need to translate expected
  * errors (unique violations) use [[Database.attempt]].
  */
final class Database(val client: SqlClient):

  /** Runs statements, each leasing a pooled connection. */
  def run[A](v: => DbIO[A])(using Frame): A < Async =
    DB.run(client)(v).orPanic

  /** Runs statements inside one transaction (commit on success, rollback on failure). */
  def tx[A](v: => DbIO[A])(using Frame): A < Async =
    DB.run(client)(DB.transaction(v)).orPanic

  /** Like [[tx]], but surfaces SQL failures as typed [[DbError]]s. */
  def attempt[A](v: => DbIO[A])(using Frame): A < (Async & Abort[DbError]) =
    DB.run(client)(DB.transaction(v)).mapAbort(DbError.fromSqlException)

object Database:

  /** Opens a connection pool bound to the enclosing `Scope`, then runs every migration set. */
  def init(config: DbConfig, sets: List[MigrationSet])(using Frame): Database < (Async & Scope & Abort[DbError]) =
    for
      client <- SqlClient.init(config.connectionUrl, SqlConfig.default.maxConnections(config.maxPoolSize))
                  .mapAbort(e => DbError.Unexpected(e))
      db      = new Database(client)
      _      <- Log.info(s"Opened connection pool to ${client.address}")
      _      <- Migrations.run(db, Migrations.platform :: sets)
    yield db
