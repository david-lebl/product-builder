package mpbuilder.catalog
package db

import core.*
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.platform.db.{Database, MigrationSet}

private[catalog] object CatalogDbCodecs:
  given SqlSchema.Column[ProductCatalog]       = Sql.jsonColumn
  given SqlSchema.Column[CompatibilityRuleset] = Sql.jsonColumn

import CatalogDbCodecs.given

private[catalog] final class PostgresCatalogRepository(db: Database) extends CatalogRepository:

  private def load[A](name: String)(using SqlSchema.Column[A]): A < Async =
    db.run(sql"SELECT doc FROM catalog.documents WHERE name = $name".as[A].run).map {
      _.headOption match
        case Some(doc) => doc
        case None => Abort.panic(new IllegalStateException(s"catalog.documents['$name'] is missing — run the DB seeder (--seed) first"))
    }

  private def store[A](name: String, version: String, value: A)(using SqlSchema.Column[A]): Unit < Async =
    db.tx {
      for
        _ <- sql"""INSERT INTO catalog.documents (name, version, doc) VALUES ($name, $version, $value)
                   ON CONFLICT (name) DO UPDATE SET version = EXCLUDED.version, doc = EXCLUDED.doc, updated_at = now()""".execute
        _ <- sql"INSERT INTO catalog.document_history (name, version, doc) VALUES ($name, $version, $value)".execute
      yield ()
    }

  override def loadCatalog: ProductCatalog < Async                    = load[ProductCatalog]("catalog")
  override def loadRuleset: CompatibilityRuleset < Async              = load[CompatibilityRuleset]("ruleset")
  override def saveCatalog(catalog: ProductCatalog): Unit < Async       = store("catalog", catalog.version.value, catalog)
  override def saveRuleset(ruleset: CompatibilityRuleset): Unit < Async = store("ruleset", ruleset.version.value, ruleset)

  def isEmpty: Boolean < Async =
    db.run(sql"SELECT count(*) FROM catalog.documents".as[Long].run.map(_.head == 0L))

object CatalogDb:
  val migrations: MigrationSet = MigrationSet("catalog", "V1__catalog.sql")

  /** `CatalogService` backed by PostgreSQL. */
  def service(db: Database): CatalogService = CatalogServiceLive.make(new PostgresCatalogRepository(db))

  /** Seeds the catalog & ruleset documents when the database is empty. Returns `true` when seeded. */
  def seedIfEmpty(db: Database, catalog: ProductCatalog, ruleset: CompatibilityRuleset): Boolean < Async =
    val repo = new PostgresCatalogRepository(db)
    repo.isEmpty.map { empty =>
      if empty then repo.saveCatalog(catalog).andThen(repo.saveRuleset(ruleset)).andThen(true) else false
    }
