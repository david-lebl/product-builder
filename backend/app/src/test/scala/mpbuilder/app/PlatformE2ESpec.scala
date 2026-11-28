package mpbuilder.app

import kyo.*
import kyo.test.*
import mpbuilder.app.http.{ApiHttpServer, HttpConfig}
import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.httpclient.{HttpBackofficeFacade, HttpStorefrontFacade, HttpTransport}
import mpbuilder.identity.*
import mpbuilder.identity.sample.SampleIdentity
import mpbuilder.ordering.*
import mpbuilder.ordering.sample.SampleDeliveryCatalog
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.DbConfig
import mpbuilder.shared.*
import mpbuilder.storefront.*

import java.net.URI
import java.net.http.{HttpClient as JHttpClient, HttpRequest as JHttpRequest, HttpResponse as JHttpResponse}
import java.nio.file.Files

/** End-to-end: Storefront → HTTP → Backoffice intake, exercised ONLY through the HTTP client adapters
  * against the real composition root (kyo-http server), in both `--storage=memory` and `--storage=postgres` modes.
  */
class PlatformE2ESpec extends Test[Any]:

  final case class Clients(sf: HttpStorefrontFacade, bo: HttpBackofficeFacade, baseUrl: String)

  /** Plain JDK client, independent of the code under test. */
  private def httpGet(url: String)(using Frame): (Int, Array[Byte], String) < Sync =
    Sync.defer {
      val res = JHttpClient.newHttpClient().send(JHttpRequest.newBuilder(URI.create(url)).GET().build(), JHttpResponse.BodyHandlers.ofByteArray())
      (res.statusCode(), res.body(), res.headers().firstValue("Content-Type").orElse(""))
    }

  /** What a browser on another origin sends before a JSON POST carrying a bearer token. */
  private def corsPreflight(url: String)(using Frame): (Int, Option[String], Option[String]) < Sync =
    Sync.defer {
      val req = JHttpRequest.newBuilder(URI.create(url))
        .method("OPTIONS", JHttpRequest.BodyPublishers.noBody())
        .header("Origin", "http://localhost:3000")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "content-type,authorization")
        .build()
      val res = JHttpClient.newHttpClient().send(req, JHttpResponse.BodyHandlers.discarding())
      def h(name: String) = Option(res.headers().firstValue(name).orElse(null))
      (res.statusCode(), h("Access-Control-Allow-Origin"), h("Access-Control-Allow-Headers"))
    }

  /** Boots the kyo-http server for a runtime on a random port (closed with the scope) and returns HTTP-backed facades. */
  def serve(rt: AppRuntime)(using Frame): Clients < (Async & Scope & Abort[Any]) =
    val handlers = ApiHttpServer.handlers(rt.storefront, rt.backoffice, rt.blobs, rt.storageLabel, rt.readiness())
    ApiHttpServer.start(handlers, HttpConfig(host = "127.0.0.1", port = 0)).map { server =>
      val base = s"http://127.0.0.1:${server.port}"
      Clients(
        new HttpStorefrontFacade(HttpTransport.default, base),
        new HttpBackofficeFacade(HttpTransport.default, base),
        base,
      )
    }

  private val tinyPdf: Chunk[Byte] = Chunk.from("%PDF-1.7\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes("UTF-8"))
  private val adminEmail           = SampleIdentity.adminEmployeePrincipal.email
  private val prepressEmail        = SampleIdentity.prepressEmployeePrincipal.email
  private val staffPass            = SampleIdentity.demoEmployeePassword

  final case class Placed(orderId: OrderId, artworkKey: String, history: List[OrderStatusEvent])

  /** Full customer → staff flow over HTTP. */
  def storefrontToIntake(c: Clients, tag: String)(using AssertScope): Placed < (Async & Abort[Any]) =
    for
      // ── Storefront: catalog, quote, login, draft artwork with a real file payload
      cat      <- c.sf.getCatalog
      bcCat     = cat.categories(SampleCatalog.businessCardsId)
      cmd       = StorefrontTranslators.presetToConfigureCmd(ConfigurationId.unsafe(s"cfg-$tag"), bcCat.id, bcCat.presets.head)
      quote    <- c.sf.configureAndQuote(cmd, Currency.CZK, ManufacturingSpeed.Standard, None, None, Language.En)
      badLogin <- c.sf.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), NonEmptyString.unsafe("wrong")).either
      auth     <- c.sf.loginWithPassword(EmailAddress.unsafe("jan@printshoppro.cz"), SampleIdentity.demoCustomerPassword)
      me       <- c.sf.currentCustomer(auth.session.token)
      draft    <- c.sf.uploadDraftArtwork(
                    sessionId = NonEmptyString.unsafe(s"sess-$tag"),
                    componentKey = cmd.components.head.key,
                    expectedTrimSize = Dimension2D.unsafeMm(90, 50),
                    expectedPageCount = Some(2),
                    payload = ArtworkUploadPayload(
                      fileName = NonEmptyString.unsafe("card front.pdf"),
                      downloadUrl = Some(NonEmptyString.unsafe(DataUrl.encode("application/pdf", tinyPdf))),
                      pageCount = Some(2),
                      trimSizeOverride = Some(Dimension2D.unsafeMm(90, 50)),
                    ),
                  )
      dlUrl     = draft.latestRevision.file.downloadUrl.map(_.value).getOrElse("")
      blob     <- httpGet(c.baseUrl + dlUrl)
      // ── Checkout as guest
      item      = QuickOrderItem(cmd, Currency.CZK)
      confirm  <- c.sf.submitCheckout(
                    SubmitCheckoutCmd(
                      sessionToken = None,
                      source = CheckoutSource.QuickSingleLine(item),
                      customer = OrderCustomerSnapshot(
                        customerId = None,
                        segment = CustomerSegment.Individual,
                        fullName = NonEmptyString.unsafe(s"Tereza $tag"),
                        email = EmailAddress.unsafe("tereza@example.cz"),
                      ),
                      billingAddress = Address(
                        NonEmptyString.unsafe("Spálená 12"), NonEmptyString.unsafe("Praha"),
                        NonEmptyString.unsafe("11000"), NonEmptyString.unsafe("CZ"),
                      ),
                      deliveryChoice = DeliveryOptionChoice.ShopPickup(SampleDeliveryCatalog.shopPragueId),
                      paymentMethod = PaymentMethod.BankTransferQR,
                      termsAccepted = true,
                      customerNote = Some("Call when ready"),
                    )
                  )
      orderId   = confirm.order.id
      // ── Backoffice over HTTP: RBAC, intake, lifecycle, audit trail
      custAsStaff <- c.bo.getDashboardSummary(auth.session.token).either
      prepress    <- c.bo.loginStaff(prepressEmail, staffPass)
      denied      <- c.bo.acceptOrder(prepress.session.token, orderId, None).either
      admin       <- c.bo.loginStaff(adminEmail, staffPass)
      intake      <- c.bo.listOrders(admin.session.token, OrderFilter(query = Some(s"Tereza $tag")))
      accepted    <- c.bo.acceptOrder(admin.session.token, orderId, Some(NonEmptyString.unsafe("Artwork OK")))
      inProd      <- c.bo.advanceOrderStage(admin.session.token, orderId, OrderStatus.InProduction, None)
      noted       <- c.bo.addOrderInternalNote(admin.session.token, orderId, NonEmptyString.unsafe(s"note-$tag"))
      missing     <- c.bo.getOrder(admin.session.token, OrderId.unsafe("ord-does-not-exist")).either
    yield
      assert(quote.pricing.effectiveQuote.netTotal.value > BigDecimal(0))
      assert(badLogin.isLeft)
      assert(me.contact.email == EmailAddress.unsafe("jan@printshoppro.cz"))
      assert(dlUrl.startsWith("/api/v1/blobs/drafts/"))
      assert(draft.latestRevision.file.checksumSha256.map(_.value).contains(Sha256.hex(tinyPdf.toArray)))
      assert(draft.latestRevision.file.sizeBytes == tinyPdf.size.toLong)
      assert(blob._1 == 200)
      assert(blob._2.toSeq == tinyPdf.toSeq)
      assert(blob._3 == "application/pdf")
      assert(custAsStaff.left.exists(_.isInstanceOf[BackofficeError.NotAnEmployeeSession.type]))
      assert(denied.left.exists(_.isInstanceOf[BackofficeError.Forbidden]))
      assert(intake.exists(_.id == orderId))
      assert(accepted.status == OrderStatus.Accepted)
      assert(inProd.status == OrderStatus.InProduction)
      assert(noted.internalNotes.exists(_.text.value == s"note-$tag"))
      assert(missing.left.exists {
          case BackofficeError.OrderFailure(_: OrderError.OrderNotFound) => true
          case _                                                         => false
        })
      Placed(orderId, draft.latestRevision.file.storageKey.value, noted.statusHistory)

  private def tmpBlobDir = Files.createTempDirectory("e2e-blobs-")

  private def appCfg(storage: StorageMode, db: Option[DbConfig]) =
    AppConfig(storage, HttpConfig(), db, tmpBlobDir, "/api/v1/blobs", seed = true)

  "Platform E2E over HTTP" - {
    "memory mode: storefront checkout lands in backoffice intake (typed errors, RBAC, blobs)".timeout(5.minutes) in {
      for
        rt      <- AppRuntime.memory(appCfg(StorageMode.Memory, None))
        c       <- serve(rt)
        _       <- storefrontToIntake(c, "mem")
        openapi <- httpGet(s"${c.baseUrl}/openapi.json")
        docs    <- httpGet(s"${c.baseUrl}/docs")
        sfPre   <- corsPreflight(s"${c.baseUrl}/api/v1/storefront/builder/quote")
        boPre   <- corsPreflight(s"${c.baseUrl}/api/v1/admin/orders/search")
      yield
        assert(openapi._1 == 200)
        assert(new String(openapi._2).contains("/api/v1/admin/orders/search"))
        assert(docs._1 == 200)
        // browsers on another origin (static SPA on :3000) must get a 2xx preflight
        assert(sfPre._1 / 100 == 2)
        assert(sfPre._2.contains("*"))
        assert(sfPre._3.exists(_.toLowerCase.contains("authorization")))
        assert(boPre._1 / 100 == 2)
        assert(boPre._2.contains("*"))
    }

    "postgres mode: seeded DB serves the flow, and state survives a full server restart".timeout(5.minutes) in {
      for
        dbCfg  <- PostgresTestSupport.freshDatabaseConfig
        cfg     = appCfg(StorageMode.Postgres, Some(dbCfg))
        placed <- Scope.run {
                    for
                      rt     <- AppRuntime.postgres(cfg, dbCfg)
                      c      <- serve(rt)
                      placed <- storefrontToIntake(c, "pg")
                    yield placed
                  }
        // ── second boot against the same database & blob directory
        _      <- Scope.run {
                    for
                      rt    <- AppRuntime.postgres(cfg, dbCfg)
                      c     <- serve(rt)
                      admin <- c.bo.loginStaff(adminEmail, staffPass)
                      order <- c.bo.getOrder(admin.session.token, placed.orderId)
                      all   <- c.bo.listOrders(admin.session.token, OrderFilter())
                      blob  <- httpGet(s"${c.baseUrl}/api/v1/blobs/${placed.artworkKey}")
                      ready <- httpGet(s"${c.baseUrl}/readyz")
                    yield
                      assert(order.status == OrderStatus.InProduction)
                      assert(order.statusHistory == placed.history) // immutable audit trail restored exactly
                      assert(placed.history.map(_.to.kind).takeRight(2) == List(OrderStatusKind.Accepted, OrderStatusKind.InProduction))
                      assert(order.internalNotes.exists(_.text.value == "note-pg"))
                      assert(all.size == 5) // 4 seeded demo orders + the new one (seeder did not run twice)
                      assert(blob._1 == 200)
                      assert(ready._1 == 200)
                  }
      yield ()
    }
  }
