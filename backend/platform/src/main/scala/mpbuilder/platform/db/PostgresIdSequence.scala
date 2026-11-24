package mpbuilder.platform
package db

import kyo.*
import mpbuilder.shared.IdSequence

/** `IdSequence` backed by the `platform.id_sequence` PostgreSQL sequence (restart-safe IDs). */
final class PostgresIdSequence(db: Database) extends IdSequence:
  override def next(using Frame): Long < Async =
    db.run(sql"SELECT nextval('platform.id_sequence')".as[Long].run.map(_.head))
