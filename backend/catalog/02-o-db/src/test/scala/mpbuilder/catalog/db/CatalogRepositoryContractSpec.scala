package mpbuilder.catalog
package db

import core.*
import mpbuilder.catalog.sample.{CatalogSampleLayer, SampleCatalog, SampleRules}
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import zio.*
import zio.test.*

object CatalogRepositoryContractSpec extends ZIOSpecDefault:

  val inMemory: ULayer[CatalogRepository] = CatalogSampleLayer.repositoryLayer()

  val postgres: URLayer[Database, CatalogRepository] =
    ZLayer.fromZIO(
      ZIO.serviceWithZIO[Database](db =>
        PostgresTestSupport.truncateSchemas(db, List("catalog")) *>
          CatalogDb.seedIfEmpty(SampleCatalog.catalog, SampleRules.ruleset).provideEnvironment(ZEnvironment(db)).as(new PostgresCatalogRepository(db))
      )
    )

  def contract[R](label: String, fresh: ZLayer[R, Nothing, CatalogRepository]) = suite(label)(
    test("seeded catalog & ruleset load back losslessly") {
      for
        repo <- ZIO.service[CatalogRepository]
        cat  <- repo.loadCatalog
        rs   <- repo.loadRuleset
      yield assertTrue(cat == SampleCatalog.catalog, rs == SampleRules.ruleset)
    },
    test("saving a new catalog version replaces the active document") {
      for
        repo <- ZIO.service[CatalogRepository]
        cat  <- repo.loadCatalog
        next  = cat.copy(version = Version.unsafe("contract-2"), showcases = Map.empty)
        _    <- repo.saveCatalog(next)
        back <- repo.loadCatalog
        rs   <- repo.loadRuleset
        _    <- repo.saveRuleset(rs.copy(rules = rs.rules.take(1)))
        rs2  <- repo.loadRuleset
      yield assertTrue(back == next, rs2.rules.size == 1)
    },
  ).provideSomeLayer[R](fresh)

  def spec = suite("Catalog repository contract")(
    contract("in-memory", inMemory),
    (contract("postgres", postgres) + test("postgres keeps an append-only version history") {
      import com.augustnagro.magnum.*
      for
        db    <- ZIO.service[Database]
        _     <- PostgresTestSupport.truncateSchemas(db, List("catalog"))
        repo   = new PostgresCatalogRepository(db)
        _     <- CatalogDb.seedIfEmpty(SampleCatalog.catalog, SampleRules.ruleset)
        again <- CatalogDb.seedIfEmpty(SampleCatalog.catalog, SampleRules.ruleset)
        _     <- repo.saveCatalog(SampleCatalog.catalog.copy(version = Version.unsafe("v-next")))
        hist  <- db.run(sql"SELECT version FROM catalog.document_history WHERE name = 'catalog' ORDER BY id".query[String].run())
      yield assertTrue(!again, hist.toList == List(SampleCatalog.catalog.version.value, "v-next"))
    }).provideShared(PostgresTestSupport.database(List(CatalogDb.migrations))),
  ) @@ TestAspect.sequential
