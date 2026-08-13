package mpbuilder.server.persistence

import javax.sql.DataSource

import org.flywaydb.core.Flyway
import zio.*

/** Runs Flyway migrations against the app DataSource before the server starts. */
object Migrations:

  val run: RIO[DataSource, Unit] =
    ZIO.serviceWithZIO[DataSource] { ds =>
      ZIO.attemptBlocking {
        Flyway.configure().dataSource(ds).load().migrate()
      }.unit
    }

  val layer: ZLayer[DataSource, Throwable, Unit] = ZLayer(run)
