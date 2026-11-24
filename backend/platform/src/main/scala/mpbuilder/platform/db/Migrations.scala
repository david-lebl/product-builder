package mpbuilder.platform
package db

import kyo.*
import mpbuilder.shared.Sha256

import java.nio.charset.StandardCharsets

/** A versioned migration set owned by one bounded context.
  *
  * Every context keeps its tables in its own PostgreSQL schema with its own history table
  * (`<schema>.schema_migrations`), so contexts evolve independently (modular-monolith isolation at
  * the data layer). Scripts live on the classpath at `db/migration/<schema>/V<n>__<desc>.sql` and are
  * listed explicitly, in order — never edit an applied script, add a new `V<n+1>__*.sql` instead.
  */
final case class MigrationSet(schema: String, scripts: List[String]):
  def resource(script: String): String = s"db/migration/$schema/$script"

object MigrationSet:
  def apply(schema: String, scripts: String*): MigrationSet = new MigrationSet(schema, scripts.toList)

/** Minimal Flyway-style migration runner built on kyo-sql.
  *
  * Each set runs under a PostgreSQL advisory lock (safe with several app instances starting at once),
  * applies every not-yet-recorded script in its own transaction, and refuses to start when an applied
  * script's checksum no longer matches the file on the classpath.
  */
object Migrations:

  /** Platform-owned objects (shared ID sequence). */
  val platform: MigrationSet = MigrationSet("platform", "V1__id_sequence.sql")

  private final case class Applied(version: Int, checksum: String)

  private final case class Script(version: Int, name: String, sql: String, checksum: String)

  private val ScriptName = "^V(\\d+)__.+\\.sql$".r

  private def identifier(schema: String)(using Frame): String < Abort[DbError] =
    if schema.matches("^[a-z_][a-z0-9_]*$") then schema
    else Abort.fail(DbError.MigrationFailed(s"illegal schema name '$schema'"))

  private def load(set: MigrationSet)(using Frame): List[Script] < (Sync & Abort[DbError]) =
    Kyo.foreach(set.scripts) { name =>
      name match
        case ScriptName(v) =>
          Sync.defer(Option(Migrations.getClass.getClassLoader.getResourceAsStream(set.resource(name)))).map {
            case None => Abort.fail(DbError.MigrationFailed(s"[${set.schema}] missing classpath resource ${set.resource(name)}"))
            case Some(in) =>
              Sync.defer {
                try
                  val sql = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                  Script(v.toInt, name, sql, Sha256.hex(sql.getBytes(StandardCharsets.UTF_8)))
                finally in.close()
              }
          }
        case _ => Abort.fail(DbError.MigrationFailed(s"[${set.schema}] '$name' is not named V<n>__<description>.sql"))
    }

  /** Stable advisory-lock key per schema. */
  private def lockKey(schema: String): Long = 0x5052_4e54_0000_0000L | (schema.hashCode.toLong & 0xffffffffL)

  def run(db: Database, sets: List[MigrationSet])(using Frame): Unit < (Async & Abort[DbError]) =
    Kyo.foreachDiscard(sets)(migrate(db, _))

  private def migrate(db: Database, set: MigrationSet)(using Frame): Unit < (Async & Abort[DbError]) =
    for
      schema  <- identifier(set.schema)
      scripts <- load(set)
      count   <- db.attempt {
                   DB.withAdvisoryLock(lockKey(schema)) {
                     for
                       _       <- DB.executeRaw(s"CREATE SCHEMA IF NOT EXISTS $schema")
                       _       <- DB.executeRaw(
                                    s"""CREATE TABLE IF NOT EXISTS $schema.schema_migrations (
                                       |  version    INTEGER     PRIMARY KEY,
                                       |  script     TEXT        NOT NULL,
                                       |  checksum   TEXT        NOT NULL,
                                       |  applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
                                       |)""".stripMargin
                                  )
                       applied <- (Sql.Fragment.lit[Applied](s"SELECT version, checksum FROM $schema.schema_migrations")).run
                       pending <- verify(schema, scripts, applied.map(a => a.version -> a.checksum).toMap)
                       _       <- Kyo.foreachDiscard(pending) { s =>
                                    DB.transaction {
                                      DB.executeRaw(s.sql).andThen(
                                        (sql"INSERT INTO " ++ Sql.Fragment.lit(s"$schema.schema_migrations") ++
                                          sql" (version, script, checksum) VALUES (${s.version}, ${s.name}, ${s.checksum})").execute
                                      )
                                    }
                                  }
                     yield pending.size
                   }
                 }.mapAbort {
                   case DbError.Unexpected(e) => DbError.MigrationFailed(s"[$schema] ${e.getMessage}")
                   case other                 => other
                 }
      _       <- Log.info(s"Migrations [$schema]: $count script(s) applied")
    yield ()

  private def verify(schema: String, scripts: List[Script], applied: Map[Int, String])(using
      Frame
  ): List[Script] < Abort[SqlException] =
    scripts.find(s => applied.get(s.version).exists(_ != s.checksum)) match
      case Some(changed) =>
        Abort.panic(new IllegalStateException(s"[$schema] applied migration ${changed.name} was modified after it ran"))
      case None => scripts.filterNot(s => applied.contains(s.version)).sortBy(_.version)
