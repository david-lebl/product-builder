package mpbuilder.platform

import mpbuilder.platform.db.*
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.*

/** Shared Testcontainers PostgreSQL fixture for `02-o-db` contract tests and app E2E tests. */
object PostgresTestSupport:

  val image: DockerImageName = DockerImageName.parse("postgres:17-alpine")

  /** One throwaway PostgreSQL 17 container per test suite. */
  val containerLayer: ZLayer[Any, Throwable, DbConfig] =
    ZLayer.scoped:
      ZIO
        .acquireRelease(
          ZIO.attemptBlocking {
            val c = new PostgreSQLContainer(image).withDatabaseName("printcraft_test")
            c.start()
            c
          }
        )(c => ZIO.attemptBlocking(c.stop()).orDie)
        .map(c => DbConfig(c.getJdbcUrl, c.getUsername, c.getPassword, maxPoolSize = 4))

  /** Fresh migrated database (selected context schemas) on a throwaway container. */
  def database(sets: List[MigrationSet]): ZLayer[Any, Throwable, Database] =
    containerLayer >>> Database.live(sets)

  /** Removes all rows in the given schemas (between tests sharing one container). Test-only: trusted input. */
  def truncateSchemas(db: Database, schemas: List[String]): UIO[Unit] =
    db.tx {
      val con   = summon[com.augustnagro.magnum.DbCon].connection
      val inSql = schemas.map(s => s"'${s.replace("'", "")}'").mkString(", ")
      val rs = con.createStatement().executeQuery(
        s"SELECT schemaname || '.' || tablename FROM pg_tables WHERE schemaname IN ($inSql) AND tablename <> 'flyway_schema_history'"
      )
      val tables = Iterator.continually(rs).takeWhile(_.next()).map(_.getString(1)).toList
      if tables.nonEmpty then con.createStatement().execute(s"TRUNCATE ${tables.mkString(", ")} CASCADE")
      ()
    }
