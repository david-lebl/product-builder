package mpbuilder.app

import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.catalog.sample.SampleCatalog
import mpbuilder.app.http.{HttpConfig, HttpServer}
import mpbuilder.httpclient.{HttpBackofficeFacade, HttpStorefrontFacade, HttpTransport}
import mpbuilder.identity.*
import mpbuilder.identity.sample.SampleIdentity
import mpbuilder.ordering.*
import mpbuilder.ordering.sample.SampleDeliveryCatalog
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.DbConfig
import mpbuilder.shared.*
import mpbuilder.storefront.*
import zio.*
import zio.test.*

import java.net.{ServerSocket, URI}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.Files

/** End-to-end: Storefront → HTTP → Backoffice intake, exercised ONLY through the HTTP client adapters
  * against the real composition root, in both `--storage=memory` and `--storage=postgres` modes.
  */
object PlatformE2ESpec extends ZIOSpecDefault:

  final case class Clients(sf: HttpStorefrontFacade, bo: HttpBackofficeFacade, baseUrl: String)

  private def freePort: UIO[Int] =
    ZIO.succeed { val s = new ServerSocket(0); try s.getLocalPort finally s.close() }

  private def httpGet(url: String): Task[(Int, Array[Byte], String)] =
    ZIO.attemptBlocking {
      val res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
      (res.statusCode(), res.body(), res.headers().firstValue("Content-Type").orElse(""))
    }

  /** What a browser on another origin sends before a JSON POST carrying a bearer token. */
  private def corsPreflight(url: String): Task[(Int, Option[String], Option[String])] =
    ZIO.attemptBlocking {
      val req = HttpRequest.newBuilder(URI.create(url))
        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
        .header("Origin", "http://localhost:3000")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "content-type,authorization")
        .build()
      val res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding())
      def h(name: String) = Option(res.headers().firstValue(name).orElse(null))
      (res.statusCode(), h("Access-Control-Allow-Origin"), h("Access-Control-Allow-Headers"))
    }

  /** Boots the HTTP server for a runtime on a free port and returns HTTP-backed facades. */
  def serve(rt: AppRuntime): ZIO[Scope, Throwable, Clients] =
    for
      port <- freePort
      cfg   = HttpConfig(host = "127.0.0.1", port = port)
      eps   = HttpServer.serverEndpoints(rt.storefront, rt.backoffice, rt.blobs, rt.storageLabel, rt.readiness)
      _    <- HttpServer.serve(eps, cfg).forkScoped
      base  = s"http://127.0.0.1:$port"
      _    <- httpGet(s"$base/healthz").retry(Schedule.spaced(200.millis) && Schedule.recurs(100))
      tr    = HttpTransport(base)
    yield Clients(new HttpStorefrontFacade(tr), new HttpBackofficeFacade(tr), base)

  private val tinyPdf: Chunk[Byte] = Chunk.fromArray("%PDF-1.7\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes("UTF-8"))
  private val adminEmail           = SampleIdentity.adminEmployeePrincipal.email
  private val prepressEmail        = SampleIdentity.prepressEmployeePrincipal.email
  private val staffPass            = SampleIdentity.demoEmployeePassword

  final case class Placed(orderId: OrderId, artworkKey: String, history: List[OrderStatusEvent])

  /** Full customer → staff flow over HTTP. */
  def storefrontToIntake(c: Clients, tag: String): Task[(Placed, TestResult)] =
    val run: IO[Any, (Placed, TestResult)] = for
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
    yield (
      Placed(orderId, draft.latestRevision.file.storageKey.value, noted.statusHistory),
      assertTrue(
        quote.pricing.effectiveQuote.netTotal.value > BigDecimal(0),
        badLogin.isLeft,
        me.contact.email == EmailAddress.unsafe("jan@printshoppro.cz"),
        dlUrl.startsWith("/api/v1/blobs/drafts/"),
        draft.latestRevision.file.checksumSha256.map(_.value).contains(Sha256.hex(tinyPdf.toArray)),
        draft.latestRevision.file.sizeBytes == tinyPdf.size.toLong,
        blob._1 == 200,
        blob._2.toSeq == tinyPdf.toSeq,
        blob._3 == "application/pdf",
        custAsStaff.left.exists(_.isInstanceOf[BackofficeError.NotAnEmployeeSession.type]),
        denied.left.exists(_.isInstanceOf[BackofficeError.Forbidden]),
        intake.exists(_.id == orderId),
        accepted.status == OrderStatus.Accepted,
        inProd.status == OrderStatus.InProduction,
        noted.internalNotes.exists(_.text.value == s"note-$tag"),
        missing.left.exists {
          case BackofficeError.OrderFailure(_: OrderError.OrderNotFound) => true
          case _                                                         => false
        },
      ),
    )
    run.mapError {
      case t: Throwable => t
      case other        => new RuntimeException(s"Unexpected typed failure: $other")
    }

  private def tmpBlobDir = Files.createTempDirectory("e2e-blobs-")

  private def appCfg(storage: StorageMode, db: Option[DbConfig]) =
    AppConfig(storage, HttpConfig(), db, tmpBlobDir, "/api/v1/blobs", seed = true)

  def spec = suite("Platform E2E over HTTP")(
    test("memory mode: storefront checkout lands in backoffice intake (typed errors, RBAC, blobs)") {
      ZIO.scoped {
        for
          rt       <- ZIO.service[AppRuntime]
          c        <- serve(rt)
          (_, res) <- storefrontToIntake(c, "mem")
          openapi  <- httpGet(s"${c.baseUrl}/docs/docs.yaml")
          sfPre    <- corsPreflight(s"${c.baseUrl}/api/v1/storefront/builder/quote")
          boPre    <- corsPreflight(s"${c.baseUrl}/api/v1/admin/orders/search")
        yield res && assertTrue(
          openapi._1 == 200,
          new String(openapi._2).contains("/api/v1/admin/orders/search"),
          // browsers on another origin (static SPA on :3000) must get a 2xx preflight
          sfPre._1 / 100 == 2, sfPre._2.contains("*"), sfPre._3.exists(_.toLowerCase.contains("authorization")),
          boPre._1 / 100 == 2, boPre._2.contains("*"),
        )
      }.provideSome[Any](AppRuntime.memory(appCfg(StorageMode.Memory, None)))
    },
    test("postgres mode: seeded DB serves the flow, and state survives a full server restart") {
      for
        dbCfg  <- ZIO.service[DbConfig]
        blobs   = tmpBlobDir
        cfg     = appCfg(StorageMode.Postgres, Some(dbCfg)).copy(blobDir = blobs)
        first  <- ZIO.scoped {
                    for
                      rt     <- ZIO.service[AppRuntime]
                      c      <- serve(rt)
                      placed <- storefrontToIntake(c, "pg")
                    yield placed
                  }.provideSome[Any](AppRuntime.postgres(cfg, dbCfg))
        (placed, firstRes) = first
        // ── second boot against the same database & blob directory
        second <- ZIO.scoped {
                    for
                      rt    <- ZIO.service[AppRuntime]
                      c     <- serve(rt)
                      admin <- c.bo.loginStaff(adminEmail, staffPass)
                      order <- c.bo.getOrder(admin.session.token, placed.orderId)
                      all   <- c.bo.listOrders(admin.session.token, OrderFilter())
                      blob  <- httpGet(s"${c.baseUrl}/api/v1/blobs/${placed.artworkKey}")
                      ready <- httpGet(s"${c.baseUrl}/readyz")
                    yield assertTrue(
                      order.status == OrderStatus.InProduction,
                      order.statusHistory == placed.history, // immutable audit trail restored exactly
                      placed.history.map(_.to.kind).takeRight(2) == List(OrderStatusKind.Accepted, OrderStatusKind.InProduction),
                      order.internalNotes.exists(_.text.value == "note-pg"),
                      all.size == 5, // 4 seeded demo orders + the new one (seeder did not run twice)
                      blob._1 == 200,
                      ready._1 == 200,
                    )
                  }.provideSome[Any](AppRuntime.postgres(cfg, dbCfg))
      yield firstRes && second
    }.provideShared(PostgresTestSupport.containerLayer),
  ) @@ TestAspect.sequential @@ TestAspect.withLiveClock @@ TestAspect.timeout(5.minutes)
