package mpbuilder.platform
package db

import com.augustnagro.magnum.*
import mpbuilder.shared.IdSequence
import zio.*

/** `IdSequence` backed by the `platform.id_sequence` PostgreSQL sequence (restart-safe IDs). */
final class PostgresIdSequence(db: Database) extends IdSequence:
  override def next: UIO[Long] =
    db.run(sql"SELECT nextval('platform.id_sequence')".query[Long].run().head)

object PostgresIdSequence:
  val layer: URLayer[Database, IdSequence] = ZLayer.fromFunction(new PostgresIdSequence(_))
