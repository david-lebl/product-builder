package mpbuilder.app

import mpbuilder.artwork.ArtworkService
import mpbuilder.artwork.db.ArtworkDb
import mpbuilder.artwork.wiring.ArtworkWiring
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.catalog.CatalogService
import mpbuilder.catalog.db.CatalogDb
import mpbuilder.catalog.sample.{SampleCatalog, SampleRules}
import mpbuilder.customers.CustomerService
import mpbuilder.customers.db.CustomersDb
import mpbuilder.customers.wiring.CustomerWiring
import mpbuilder.identity.IdentityService
import mpbuilder.identity.db.IdentityDb
import mpbuilder.identity.wiring.IdentityWiring
import mpbuilder.manufacturing.ManufacturingService
import mpbuilder.manufacturing.sample.ManufacturingSampleLayer
import mpbuilder.ordering.db.OrderingDb
import mpbuilder.ordering.wiring.OrderingWiring
import mpbuilder.platform.db.*
import mpbuilder.pricing.PricingService
import mpbuilder.pricing.db.PricingDb
import mpbuilder.pricing.sample.*
import mpbuilder.sample.DomainBridges
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade
import zio.*

/** Phase 3 composition: every bounded context on PostgreSQL (manufacturing stays in-memory) + DB seeder.
  * The HTTP server and the memory/postgres runtime switch arrive in Phase 4.
  */
object AppRuntime:

  /** Every context's migration set (manufacturing stays in-memory until its own chapter). */
  val migrationSets: List[MigrationSet] = List(
    IdentityDb.migrations,
    CustomersDb.migrations,
    CatalogDb.migrations,
    PricingDb.migrations,
    OrderingDb.migrations,
    ArtworkDb.migrations,
  )

  // ── Mode B: PostgreSQL persistence ─────────────────────────────────────────

  type PgEnv =
    Database & EventBus & BlobStorage & IdSequence & CatalogService & CustomerService & IdentityService &
      ArtworkService & mpbuilder.ordering.CartService & mpbuilder.ordering.CheckoutOptionsService &
      mpbuilder.ordering.OrderService & ManufacturingService & PricingService & StorefrontFacade & BackofficeFacade

  def postgresServices(dbConfig: DbConfig, blobLayer: ULayer[BlobStorage]): ZLayer[Any, Throwable, PgEnv] =
    val infra = (ZLayer.succeed(dbConfig) >>> Database.live(migrationSets)) ++ EventBus.live ++ blobLayer

    val contexts =
      CatalogDb.service ++
        CustomersDb.service ++
        ArtworkDb.service ++
        OrderingDb.services ++
        ManufacturingSampleLayer.live ++
        PostgresIdSequence.layer

    val dependent =
      IdentityWiring.postgres ++
        PricingDb.service[ManufacturingService](
          ZIO.serviceWithZIO[ManufacturingService](_.currentCapacity.map(DomainBridges.toDynamicPricingContext(_)))
        )

    infra >+> contexts >+> dependent >+> (StorefrontFacade.layer ++ BackofficeFacade.layer)

  /** DB Seeder: fills an EMPTY database with the `backend/sample` showcase fixtures (idempotent). */
  val seedIfEmpty: ZIO[Database & IdentityService, Nothing, Unit] =
    for
      catalog   <- CatalogDb.seedIfEmpty(SampleCatalog.catalog, SampleRules.ruleset)
      pricing   <- PricingDb.seedIfEmpty(
                     pricelists = PricingSampleLayer.defaultPricelists(useSheetCzk = true).values.toList,
                     agreements = SampleAgreements.all,
                     promoCodes = SamplePromoCodes.all,
                     costSheets = List(SampleCostSheets.costSheetUsd, SampleCostSheets.costSheetCzk),
                   )
      customers <- CustomerWiring.seedIfEmpty
      ordering  <- OrderingWiring.seedIfEmpty
      artwork   <- ArtworkWiring.seedIfEmpty
      logins    <- IdentityWiring.seedDemoCredentials
      _         <- ZIO.logInfo(
                     s"DB seeder: catalog=$catalog pricing=$pricing customers=$customers ordering=$ordering " +
                       s"artwork=$artwork newCredentials=$logins"
                   )
    yield ()
