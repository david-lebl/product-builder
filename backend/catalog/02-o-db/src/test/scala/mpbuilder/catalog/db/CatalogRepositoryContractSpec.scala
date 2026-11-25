package mpbuilder.catalog
package db

import core.*
import kyo.*
import kyo.test.*
import mpbuilder.catalog.sample.{CatalogSampleWiring, SampleCatalog, SampleRules}
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.shared.*

class CatalogRepositoryContractSpec extends Test[Any]:

  private type Fresh = CatalogRepository < (Async & Scope & Abort[Throwable])

  private val inMemory: Fresh = CatalogSampleWiring.repository()

  private def postgres: Fresh =
    PostgresTestSupport.database(List(CatalogDb.migrations)).map { db =>
      CatalogDb.seedIfEmpty(db, SampleCatalog.catalog, SampleRules.ruleset).andThen(new PostgresCatalogRepository(db))
    }

  private def contract(label: String, fresh: => Fresh): Unit =
    label - {
      "seeded catalog & ruleset load back losslessly" in {
        for
          repo <- fresh
          cat  <- repo.loadCatalog
          rs   <- repo.loadRuleset
        yield
          assert(cat == SampleCatalog.catalog)
          assert(rs == SampleRules.ruleset)
      }

      "saving a new catalog version replaces the active document" in {
        for
          repo <- fresh
          cat  <- repo.loadCatalog
          next  = cat.copy(version = Version.unsafe("contract-2"), showcases = Map.empty)
          _    <- repo.saveCatalog(next)
          back <- repo.loadCatalog
          rs   <- repo.loadRuleset
          _    <- repo.saveRuleset(rs.copy(rules = rs.rules.take(1)))
          rs2  <- repo.loadRuleset
        yield
          assert(back == next)
          assert(rs2.rules.size == 1)
      }
    }

  "Catalog repository contract" - {
    contract("in-memory", inMemory)
    contract("postgres", postgres)

    "postgres keeps an append-only version history" in {
      for
        db    <- PostgresTestSupport.database(List(CatalogDb.migrations))
        repo   = new PostgresCatalogRepository(db)
        first <- CatalogDb.seedIfEmpty(db, SampleCatalog.catalog, SampleRules.ruleset)
        again <- CatalogDb.seedIfEmpty(db, SampleCatalog.catalog, SampleRules.ruleset)
        _     <- repo.saveCatalog(SampleCatalog.catalog.copy(version = Version.unsafe("v-next")))
        hist  <- db.run(sql"SELECT version FROM catalog.document_history WHERE name = 'catalog' ORDER BY id".as[String].run)
      yield
        assert(first)
        assert(!again)
        assert(hist.toList == List(SampleCatalog.catalog.version.value, "v-next"))
    }
  }
