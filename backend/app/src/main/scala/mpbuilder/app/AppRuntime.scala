package mpbuilder.app

import com.augustnagro.magnum.*
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
import mpbuilder.platform.blob.LocalFileBlobStorage
import mpbuilder.platform.db.*
import mpbuilder.pricing.PricingService
import mpbuilder.pricing.db.PricingDb
import mpbuilder.pricing.sample.*
import mpbuilder.sample.{DomainBridges, SampleShowcaseRuntime}
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade
import zio.*

/** What the HTTP server needs from either storage mode. */
final case class AppRuntime(
    storefront: StorefrontFacade,
    backoffice: BackofficeFacade,
    blobs: BlobStorage,
    storageLabel: String,
    readiness: IO[String, Unit],
)

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

  // ── Mode A: in-memory sample runtime behind HTTP (zero-DB API dev) ─────────

  def memory(cfg: AppConfig): ZLayer[Any, Nothing, AppRuntime] =
    val blobs = ZLayer.fromZIO(BlobStorage.inMemory(publicBaseUrl = Some(cfg.blobPublicBaseUrl)))
    SampleShowcaseRuntime.layer(blobStorage = blobs) >>> ZLayer.fromZIO(
      for
        sf <- ZIO.service[StorefrontFacade]
        bo <- ZIO.service[BackofficeFacade]
        bs <- ZIO.service[BlobStorage]
      yield AppRuntime(sf, bo, bs, "memory", ZIO.unit)
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

  def postgres(cfg: AppConfig, dbConfig: DbConfig): ZLayer[Any, Throwable, AppRuntime] =
    val blobLayer = LocalFileBlobStorage.layer(cfg.blobDir, Some(cfg.blobPublicBaseUrl))
    postgresServices(dbConfig, blobLayer) >>> ZLayer.fromZIO(
      for
        _  <- ZIO.when(cfg.seed)(seedIfEmpty)
        db <- ZIO.service[Database]
        sf <- ZIO.service[StorefrontFacade]
        bo <- ZIO.service[BackofficeFacade]
        bs <- ZIO.service[BlobStorage]
        ready = db.transactor.connect(sql"SELECT 1".query[Int].run()).unit.mapError(e => s"database unavailable: ${e.getMessage}")
      yield AppRuntime(sf, bo, bs, "postgres", ready)
    )

  def fromConfig(cfg: AppConfig): ZLayer[Any, Throwable, AppRuntime] =
    (cfg.storage, cfg.db) match
      case (StorageMode.Postgres, Some(db)) => postgres(cfg, db)
      case (StorageMode.Postgres, None)     => ZLayer.fail(new IllegalArgumentException("PostgreSQL mode requires DATABASE_URL"))
      case (StorageMode.Memory, _)          => memory(cfg)
