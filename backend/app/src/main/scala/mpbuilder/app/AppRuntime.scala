package mpbuilder.app

import kyo.*
import mpbuilder.artwork.db.ArtworkDb
import mpbuilder.artwork.wiring.ArtworkWiring
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.catalog.db.CatalogDb
import mpbuilder.catalog.sample.{SampleCatalog, SampleRules}
import mpbuilder.customers.db.CustomersDb
import mpbuilder.customers.wiring.CustomerWiring
import mpbuilder.identity.IdentityService
import mpbuilder.identity.db.IdentityDb
import mpbuilder.identity.wiring.IdentityWiring
import mpbuilder.manufacturing.sample.ManufacturingSampleWiring
import mpbuilder.ordering.db.OrderingDb
import mpbuilder.ordering.wiring.OrderingWiring
import mpbuilder.platform.blob.LocalFileBlobStorage
import mpbuilder.platform.db.*
import mpbuilder.pricing.db.PricingDb
import mpbuilder.pricing.sample.*
import mpbuilder.sample.{DomainBridges, SampleShowcaseRuntime}
import mpbuilder.shared.*
import mpbuilder.storefront.StorefrontFacade

/** What the HTTP server needs from either storage mode. */
final case class AppRuntime(
    storefront: StorefrontFacade,
    backoffice: BackofficeFacade,
    blobs: BlobStorage,
    storageLabel: String,
    readiness: () => Unit < (Async & Abort[String]),
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

  def memory(cfg: AppConfig)(using Frame): AppRuntime < Async =
    SampleShowcaseRuntime.make(blobStorage = BlobStorage.inMemory(publicBaseUrl = Some(cfg.blobPublicBaseUrl))).map { rt =>
      AppRuntime(rt.storefront, rt.backoffice, rt.blobs, "memory", () => ())
    }

  // ── Mode B: PostgreSQL persistence ─────────────────────────────────────────

  /** Every PostgreSQL-backed service of the platform (shared by the server and the persistence E2E tests). */
  final case class PostgresServices(
      db: Database,
      eventBus: EventBus,
      blobs: BlobStorage,
      identity: IdentityService,
      storefront: StorefrontFacade,
      backoffice: BackofficeFacade,
  )

  def postgresServices(db: Database, blobs: BlobStorage)(using Frame): PostgresServices < Sync =
    for
      bus           <- EventBus.init
      manufacturing <- ManufacturingSampleWiring.service(bus)
    yield
      val catalog   = CatalogDb.service(db)
      val customers = CustomersDb.service(db, bus)
      val artwork   = ArtworkDb.service(db, bus)
      val ordering  = OrderingDb.services(db, bus)
      val identity  = IdentityWiring.postgres(db, customers, bus)
      val pricing   = PricingDb.service(db, bus, manufacturing.currentCapacity.map(DomainBridges.toDynamicPricingContext(_)))
      val ids       = new PostgresIdSequence(db)
      PostgresServices(
        db,
        bus,
        blobs,
        identity,
        StorefrontFacade.make(
          catalog, pricing, customers, identity, artwork, ordering.carts, ordering.checkoutOptions, ordering.orders,
          manufacturing, ids, blobs,
        ),
        BackofficeFacade.make(catalog, pricing, customers, identity, ordering.orders, ordering.checkoutOptions, artwork, blobs),
      )

  /** DB Seeder: fills an EMPTY database with the `backend/sample` showcase fixtures (idempotent). */
  def seedIfEmpty(db: Database, identity: IdentityService)(using Frame): Unit < Async =
    for
      catalog   <- CatalogDb.seedIfEmpty(db, SampleCatalog.catalog, SampleRules.ruleset)
      pricing   <- PricingDb.seedIfEmpty(
                     db,
                     pricelists = PricingSampleWiring.defaultPricelists(useSheetCzk = true).values.toList,
                     agreements = SampleAgreements.all,
                     promoCodes = SamplePromoCodes.all,
                     costSheets = List(SampleCostSheets.costSheetUsd, SampleCostSheets.costSheetCzk),
                   )
      customers <- CustomerWiring.seedIfEmpty(db)
      ordering  <- OrderingWiring.seedIfEmpty(db)
      artwork   <- ArtworkWiring.seedIfEmpty(db)
      logins    <- IdentityWiring.seedDemoCredentials(identity)
      _         <- Log.info(
                     s"DB seeder: catalog=$catalog pricing=$pricing customers=$customers ordering=$ordering " +
                       s"artwork=$artwork newCredentials=$logins"
                   )
    yield ()

  def postgres(cfg: AppConfig, dbConfig: DbConfig)(using Frame): AppRuntime < (Async & Scope & Abort[DbError]) =
    for
      db  <- Database.init(dbConfig, migrationSets)
      svc <- postgresServices(db, new LocalFileBlobStorage(cfg.blobDir, Some(cfg.blobPublicBaseUrl)))
      _   <- Effects.when(cfg.seed)(seedIfEmpty(db, svc.identity))
    yield AppRuntime(
      svc.storefront,
      svc.backoffice,
      svc.blobs,
      "postgres",
      () =>
        Abort.run[Throwable](Abort.catching[Throwable](db.run(sql"SELECT 1".as[Int].run))).map {
          case Result.Success(_) => ()
          case other             => Abort.fail(s"database unavailable: $other")
        },
    )

  def fromConfig(cfg: AppConfig)(using Frame): AppRuntime < (Async & Scope & Abort[String]) =
    (cfg.storage, cfg.db) match
      case (StorageMode.Postgres, Some(db)) => postgres(cfg, db).mapAbort(_.message)
      case (StorageMode.Postgres, None)     => Abort.fail("PostgreSQL mode requires DATABASE_URL")
      case (StorageMode.Memory, _)          => memory(cfg)
