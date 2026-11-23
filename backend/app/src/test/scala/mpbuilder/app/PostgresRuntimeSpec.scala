package mpbuilder.app

import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.identity.sample.SampleIdentity
import mpbuilder.ordering.*
import mpbuilder.ordering.sample.SampleDeliveryCatalog
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.blob.LocalFileBlobStorage
import mpbuilder.platform.db.DbConfig
import mpbuilder.shared.*
import mpbuilder.storefront.*
import zio.*
import zio.test.*

import java.nio.file.{Files, Path}

/** Phase 3: the facades running entirely on PostgreSQL — seeded, exercised, and rebuilt
  * from scratch against the same database (simulated server restart).
  */
object PostgresRuntimeSpec extends ZIOSpecDefault:

  private val adminEmail = SampleIdentity.adminEmployeePrincipal.email
  private val staffPass  = SampleIdentity.demoEmployeePassword
  private val pdf        = Chunk.fromArray("%PDF-1.7\n%%EOF".getBytes("UTF-8"))

  final case class FirstRun(orderId: OrderId, history: List[OrderStatusEvent], artworkKey: String, seededOrders: Int)

  private def runtime(db: DbConfig, blobDir: Path) =
    AppRuntime.postgresServices(db, LocalFileBlobStorage.layer(blobDir, Some("/api/v1/blobs")))

  private val firstRun: ZIO[StorefrontFacade & BackofficeFacade & mpbuilder.platform.db.Database & mpbuilder.identity.IdentityService, Any, FirstRun] =
    for
      _       <- AppRuntime.seedIfEmpty
      sf      <- ZIO.service[StorefrontFacade]
      bo      <- ZIO.service[BackofficeFacade]
      admin   <- bo.loginStaff(adminEmail, staffPass)
      seeded  <- bo.listOrders(admin.session.token, OrderFilter())
      cat     <- sf.getCatalog
      bcCat    = cat.categories(SampleCatalog.businessCardsId)
      cmd      = StorefrontTranslators.presetToConfigureCmd(ConfigurationId.unsafe("cfg-pg"), bcCat.id, bcCat.presets.head)
      quote   <- sf.configureAndQuote(cmd, Currency.CZK, ManufacturingSpeed.Standard, None, None, Language.En)
      draft   <- sf.uploadDraftArtwork(
                   NonEmptyString.unsafe("sess-pg"),
                   cmd.components.head.key,
                   Dimension2D.unsafeMm(90, 50),
                   Some(2),
                   ArtworkUploadPayload(
                     fileName = NonEmptyString.unsafe("front.pdf"),
                     downloadUrl = Some(NonEmptyString.unsafe(DataUrl.encode("application/pdf", pdf))),
                   ),
                 )
      item     = QuickOrderItem(cmd, Currency.CZK)
      confirm <- sf.submitCheckout(
                   SubmitCheckoutCmd(
                     sessionToken = None,
                     source = CheckoutSource.QuickSingleLine(item),
                     customer = OrderCustomerSnapshot(
                       customerId = None,
                       segment = CustomerSegment.Individual,
                       fullName = NonEmptyString.unsafe("Tereza Persistent"),
                       email = EmailAddress.unsafe("tereza@example.cz"),
                     ),
                     billingAddress = Address(
                       NonEmptyString.unsafe("Spálená 12"), NonEmptyString.unsafe("Praha"),
                       NonEmptyString.unsafe("11000"), NonEmptyString.unsafe("CZ"),
                     ),
                     deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                     paymentMethod = PaymentMethod.BankTransferQR,
                     customerNote = None,
                   )
                 )
      orderId  = confirm.order.id
      _       <- bo.acceptOrder(admin.session.token, orderId, None)
      _       <- bo.advanceOrderStage(admin.session.token, orderId, OrderStatus.InProduction, None)
      noted   <- bo.addOrderInternalNote(admin.session.token, orderId, NonEmptyString.unsafe("persist me"))
    yield FirstRun(orderId, noted.statusHistory, draft.latestRevision.file.storageKey.value, seeded.size)

  def spec = suite("Phase 3 — facades on PostgreSQL")(
    test("seeded database serves the facades and all state survives a full runtime rebuild") {
      for
        db      <- ZIO.service[DbConfig]
        blobDir <- ZIO.attempt(Files.createTempDirectory("pg-blobs-"))
        first   <- ZIO.scoped(firstRun.provideSomeLayer[Scope](runtime(db, blobDir)))
        second  <- ZIO.scoped {
                     (for
                       _     <- AppRuntime.seedIfEmpty // must be a no-op now
                       bo    <- ZIO.service[BackofficeFacade]
                       admin <- bo.loginStaff(adminEmail, staffPass)
                       order <- bo.getOrder(admin.session.token, first.orderId)
                       all   <- bo.listOrders(admin.session.token, OrderFilter())
                     yield (order, all.size)).provideSomeLayer[Scope](runtime(db, blobDir))
                   }
        (order, total) = second
        stored  <- ZIO.attempt(Files.readAllBytes(blobDir.resolve(first.artworkKey)))
      yield assertTrue(
        first.seededOrders == 4,
        order.status == OrderStatus.InProduction,
        order.statusHistory == first.history,
        order.internalNotes.exists(_.text.value == "persist me"),
        total == 5,
        Chunk.fromArray(stored) == pdf,
        // restart-safe IDs come from platform.id_sequence (starts at 10000)
        first.orderId.value.stripPrefix("ord-").toLongOption.exists(_ >= 10000L),
      )
    }.provideShared(PostgresTestSupport.containerLayer)
  ) @@ TestAspect.withLiveClock @@ TestAspect.timeout(5.minutes)
