package mpbuilder.catalog
package db

import com.augustnagro.magnum.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.platform.db.{Database, MigrationSet}
import zio.*

private[catalog] final class PostgresCatalogRepository(db: Database) extends CatalogRepository:

  private def load[A: JsonValueCodec](name: String): UIO[A] =
    db.run(sql"SELECT doc::text FROM catalog.documents WHERE name = $name".query[String].run().headOption)
      .someOrElseZIO(ZIO.dieMessage(s"catalog.documents['$name'] is missing — run the DB seeder (--seed) first"))
      .map(fromJsonUnsafe[A])

  private def store[A: JsonValueCodec](name: String, version: String, value: A): UIO[Unit] =
    val json = toJson(value)
    db.tx {
      sql"""INSERT INTO catalog.documents (name, version, doc) VALUES ($name, $version, $json::jsonb)
            ON CONFLICT (name) DO UPDATE SET version = EXCLUDED.version, doc = EXCLUDED.doc, updated_at = now()"""
        .update.run()
      sql"INSERT INTO catalog.document_history (name, version, doc) VALUES ($name, $version, $json::jsonb)".update.run()
      ()
    }

  override def loadCatalog: UIO[ProductCatalog]                   = load[ProductCatalog]("catalog")
  override def loadRuleset: UIO[CompatibilityRuleset]             = load[CompatibilityRuleset]("ruleset")
  override def saveCatalog(catalog: ProductCatalog): UIO[Unit]    = store("catalog", catalog.version.value, catalog)
  override def saveRuleset(ruleset: CompatibilityRuleset): UIO[Unit] = store("ruleset", ruleset.version.value, ruleset)

  def isEmpty: UIO[Boolean] =
    db.run(sql"SELECT count(*) FROM catalog.documents".query[Long].run().head == 0L)

object CatalogDb:
  val migrations: MigrationSet = MigrationSet("catalog")

  /** `CatalogService` backed by PostgreSQL. */
  val service: URLayer[Database, CatalogService] =
    ZLayer.fromFunction((db: Database) => new PostgresCatalogRepository(db): CatalogRepository) >>> CatalogServiceLive.layer

  /** Seeds the catalog & ruleset documents when the database is empty. Returns `true` when seeded. */
  def seedIfEmpty(catalog: ProductCatalog, ruleset: CompatibilityRuleset): URIO[Database, Boolean] =
    ZIO.serviceWithZIO[Database] { db =>
      val repo = new PostgresCatalogRepository(db)
      repo.isEmpty.flatMap(empty =>
        ZIO.when(empty)(repo.saveCatalog(catalog) *> repo.saveRuleset(ruleset)).map(_.isDefined)
      )
    }
