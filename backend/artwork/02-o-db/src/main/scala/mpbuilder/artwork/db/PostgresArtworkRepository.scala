package mpbuilder.artwork
package db

import com.augustnagro.magnum.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*
import zio.*

private[artwork] object ArtworkDbCodecs:
  given necArtworkRevision: JsonValueCodec[NonEmptyChunk[ArtworkRevision]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ArtworkRevision]](config), "ArtworkRevision list")
  given JsonValueCodec[ArtworkSubmission] = JsonCodecMaker.make(config)

  def ownerKey(o: ArtworkOwner): (String, String, Option[Int]) = o match
    case ArtworkOwner.DraftSession(sid)        => ("DraftSession", sid.value, None)
    case ArtworkOwner.OrderLineRef(oid, index) => ("OrderLineRef", oid.value, Some(index))

import ArtworkDbCodecs.{given, *}

private[artwork] final class PostgresArtworkRepository(db: Database) extends ArtworkRepository:

  override def findById(id: ArtworkId): UIO[Option[ArtworkSubmission]] =
    db.run(sql"SELECT doc::text FROM artwork.submissions WHERE id = ${id.value}".query[String].run().headOption.map(fromJsonUnsafe[ArtworkSubmission]))

  override def findByOwner(owner: ArtworkOwner): UIO[List[ArtworkSubmission]] =
    val (kind, ref, line) = ownerKey(owner)
    db.run(
      sql"""SELECT doc::text FROM artwork.submissions
            WHERE owner_kind = $kind AND owner_ref = $ref
              AND owner_line_index IS NOT DISTINCT FROM CAST($line AS INT)
            ORDER BY id""".query[String].run().toList.map(fromJsonUnsafe[ArtworkSubmission])
    )

  override def save(s: ArtworkSubmission): UIO[Unit] =
    val (kind, ref, line) = ownerKey(s.owner)
    val json              = toJson(s)
    db.tx(
      sql"""INSERT INTO artwork.submissions
              (id, owner_kind, owner_ref, owner_line_index, component_key, status, revision_count, doc)
            VALUES (${s.id.value}, $kind, $ref, $line, ${s.componentKey.value}, ${s.status.productPrefix},
                    ${s.revisions.size}, $json::jsonb)
            ON CONFLICT (id) DO UPDATE SET
              owner_kind = EXCLUDED.owner_kind, owner_ref = EXCLUDED.owner_ref,
              owner_line_index = EXCLUDED.owner_line_index, component_key = EXCLUDED.component_key,
              status = EXCLUDED.status, revision_count = EXCLUDED.revision_count,
              doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

  override def delete(id: ArtworkId): UIO[Unit] =
    db.tx(sql"DELETE FROM artwork.submissions WHERE id = ${id.value}".update.run()).unit

  def isEmpty: UIO[Boolean] =
    db.run(sql"SELECT count(*) FROM artwork.submissions".query[Long].run().head == 0L)

object ArtworkDb:
  val migrations: MigrationSet = MigrationSet("artwork")

  private[artwork] val repository: URLayer[Database, ArtworkRepository] =
    ZLayer.fromFunction((db: Database) => new PostgresArtworkRepository(db))

  /** `ArtworkService` backed by PostgreSQL. */
  val service: URLayer[Database & EventBus, ArtworkService] =
    (repository ++ ZLayer.service[EventBus]) >>> ArtworkServiceLive.layer

  private[artwork] def seedIfEmpty(submissions: List[ArtworkSubmission]): URIO[Database, Boolean] =
    ZIO.serviceWithZIO[Database] { db =>
      val repo = new PostgresArtworkRepository(db)
      repo.isEmpty.flatMap(empty => ZIO.when(empty)(ZIO.foreachDiscard(submissions)(repo.save)).map(_.isDefined))
    }
