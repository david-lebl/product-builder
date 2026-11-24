package mpbuilder.artwork
package db

import core.*
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*

private[artwork] object ArtworkDbCodecs:
  given Schema[ArtworkSubmission]           = Schema.derived
  given SqlSchema.Column[ArtworkSubmission] = Sql.jsonColumn

  def ownerKey(o: ArtworkOwner): (String, String, Option[Int]) = o match
    case ArtworkOwner.DraftSession(sid)        => ("DraftSession", sid.value, None)
    case ArtworkOwner.OrderLineRef(oid, index) => ("OrderLineRef", oid.value, Some(index))

import ArtworkDbCodecs.{given, *}

private[artwork] final class PostgresArtworkRepository(db: Database) extends ArtworkRepository:

  override def findById(id: ArtworkId): Option[ArtworkSubmission] < Async =
    db.run(sql"SELECT doc FROM artwork.submissions WHERE id = ${id.value}".as[ArtworkSubmission].run.map(_.headOption))

  override def findByOwner(owner: ArtworkOwner): List[ArtworkSubmission] < Async =
    val (kind, ref, line) = ownerKey(owner)
    db.run(
      sql"""SELECT doc FROM artwork.submissions
            WHERE owner_kind = $kind AND owner_ref = $ref
              AND owner_line_index IS NOT DISTINCT FROM CAST($line AS INT)
            ORDER BY id""".as[ArtworkSubmission].run.map(_.toList)
    )

  override def save(s: ArtworkSubmission): Unit < Async =
    val (kind, ref, line) = ownerKey(s.owner)
    db.tx(
      sql"""INSERT INTO artwork.submissions
              (id, owner_kind, owner_ref, owner_line_index, component_key, status, revision_count, doc)
            VALUES (${s.id.value}, $kind, $ref, $line, ${s.componentKey.value}, ${s.status.productPrefix},
                    ${s.revisions.size}, $s)
            ON CONFLICT (id) DO UPDATE SET
              owner_kind = EXCLUDED.owner_kind, owner_ref = EXCLUDED.owner_ref,
              owner_line_index = EXCLUDED.owner_line_index, component_key = EXCLUDED.component_key,
              status = EXCLUDED.status, revision_count = EXCLUDED.revision_count,
              doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

  override def delete(id: ArtworkId): Unit < Async =
    db.tx(sql"DELETE FROM artwork.submissions WHERE id = ${id.value}".execute).unit

  def isEmpty: Boolean < Async =
    db.run(sql"SELECT count(*) FROM artwork.submissions".as[Long].run.map(_.head == 0L))

object ArtworkDb:
  val migrations: MigrationSet = MigrationSet("artwork", "V1__artwork.sql")

  private[artwork] def repository(db: Database): ArtworkRepository = new PostgresArtworkRepository(db)

  /** `ArtworkService` backed by PostgreSQL. */
  def service(db: Database, eventBus: EventBus): ArtworkService = ArtworkServiceLive.make(repository(db), eventBus)

  private[artwork] def seedIfEmpty(db: Database, submissions: List[ArtworkSubmission]): Boolean < Async =
    val repo = new PostgresArtworkRepository(db)
    repo.isEmpty.map(empty => if empty then Kyo.foreachDiscard(submissions)(repo.save).andThen(true) else false)
