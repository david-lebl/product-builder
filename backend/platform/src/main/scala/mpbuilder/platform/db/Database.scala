package mpbuilder.platform
package db

import com.augustnagro.magnum.{DbCon, DbTx}
import com.augustnagro.magnum.magzio.TransactorZIO
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import zio.*

import java.sql.SQLException
import javax.sql.DataSource

final case class DbConfig(
    jdbcUrl: String,
    user: String,
    password: String,
    maxPoolSize: Int = 10,
)

object DbConfig:
  /** Reads `DATABASE_URL` (JDBC), `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_POOL_SIZE`. */
  def fromEnv: IO[String, DbConfig] =
    for
      url  <- System.env("DATABASE_URL").orDie.someOrFail("DATABASE_URL is not set (e.g. jdbc:postgresql://localhost:5432/printcraft)")
      user <- System.envOrElse("DATABASE_USER", "printcraft").orDie
      pass <- System.envOrElse("DATABASE_PASSWORD", "printcraft").orDie
      pool <- System.envOrElse("DATABASE_POOL_SIZE", "10").orDie
    yield DbConfig(url, user, pass, pool.toIntOption.getOrElse(10))

/** Infrastructure-level database errors (confined to platform & `02-o-db` adapters). */
enum DbError(val message: String):
  case UniqueViolation(detail: String) extends DbError(s"Unique constraint violation: $detail")
  case MigrationFailed(detail: String) extends DbError(s"Database migration failed: $detail")
  case Unexpected(cause: Throwable)    extends DbError(s"Unexpected database error: ${cause.getMessage}")

object DbError:
  def fromThrowable(t: Throwable): DbError = t match
    case e: SQLException if e.getSQLState == "23505" => UniqueViolation(e.getMessage)
    case other                                       => Unexpected(other)

/** A Flyway migration set owned by one bounded context.
  *
  * Every context keeps its tables in its own PostgreSQL schema with its own Flyway history
  * table, so contexts evolve independently (modular-monolith isolation at the data layer).
  * Scripts live on the classpath at `db/migration/<schema>/V<n>__<desc>.sql`.
  */
final case class MigrationSet(schema: String):
  def location: String = s"classpath:db/migration/$schema"

object Migrations:
  /** Platform-owned objects (shared ID sequence). */
  val platform: MigrationSet = MigrationSet("platform")

  def run(dataSource: DataSource, sets: List[MigrationSet]): IO[DbError, Unit] =
    ZIO.foreachDiscard(sets) { set =>
      ZIO
        .attemptBlocking {
          Flyway
            .configure()
            .dataSource(dataSource)
            .schemas(set.schema)
            .createSchemas(true)
            .table("flyway_schema_history")
            .locations(set.location)
            .failOnMissingLocations(true)
            .load()
            .migrate()
        }
        .mapError(e => DbError.MigrationFailed(s"[${set.schema}] ${e.getMessage}"))
        .flatMap(res => ZIO.logInfo(s"Flyway [${set.schema}]: ${res.migrationsExecuted} migration(s) applied"))
    }

/** Thin, infallible-by-contract wrapper around Magnum's ZIO transactor.
  *
  * Repository ports in the domain cores are `UIO` (persistence is not a business failure),
  * so unexpected SQL errors become defects here; adapters that need to translate expected
  * errors (unique violations) use [[Database.attempt]].
  */
final class Database(val transactor: TransactorZIO):
  def run[A](f: DbCon ?=> A): UIO[A]                 = transactor.connect(f).orDie
  def tx[A](f: DbTx ?=> A): UIO[A]                   = transactor.transact(f).orDie
  def attempt[A](f: DbTx ?=> A): IO[DbError, A]      = transactor.transact(f).mapError(DbError.fromThrowable)

object Database:

  def dataSource: ZLayer[DbConfig, Throwable, DataSource] =
    ZLayer.scoped:
      for
        cfg <- ZIO.service[DbConfig]
        ds  <- ZIO.acquireRelease(
                 ZIO.attemptBlocking {
                   val hc = new HikariConfig()
                   hc.setJdbcUrl(cfg.jdbcUrl)
                   hc.setUsername(cfg.user)
                   hc.setPassword(cfg.password)
                   hc.setMaximumPoolSize(cfg.maxPoolSize)
                   hc.setPoolName("printcraft-pool")
                   new HikariDataSource(hc)
                 }
               )(ds => ZIO.attemptBlocking(ds.close()).orDie *> ZIO.logInfo("Closed connection pool"))
        _   <- ZIO.logInfo(s"Opened connection pool to ${cfg.jdbcUrl}")
      yield ds

  /** Runs every migration set, then exposes a ready-to-use [[Database]]. */
  def migrated(sets: List[MigrationSet]): ZLayer[DataSource, Throwable, Database] =
    TransactorZIO.layer.flatMap { env =>
      ZLayer.fromZIO:
        for
          ds <- ZIO.service[DataSource]
          _  <- Migrations.run(ds, Migrations.platform :: sets).mapError(e => new RuntimeException(e.message))
        yield new Database(env.get[TransactorZIO])
    }

  def live(sets: List[MigrationSet]): ZLayer[DbConfig, Throwable, Database] =
    dataSource >>> migrated(sets)
