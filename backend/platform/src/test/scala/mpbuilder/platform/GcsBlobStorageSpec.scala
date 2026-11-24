package mpbuilder.platform

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import kyo.*
import kyo.test.*
import mpbuilder.platform.blob.*
import mpbuilder.shared.*

import java.net.{InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.{KeyPairGenerator, PublicKey, Signature}
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GcsBlobStorageSpec extends Test[Any]:

  private final case class StoredObj(contentType: String, metadata: Map[String, String], bytes: Array[Byte])

  private final case class MockObjMeta(
      name: Option[String] = None,
      contentType: Option[String] = None,
      metadata: Option[Map[String, String]] = None,
  )
  private given Schema[MockObjMeta] = Schema.derived

  private def handler(f: HttpExchange => Unit): com.sun.net.httpserver.HttpHandler =
    new com.sun.net.httpserver.HttpHandler:
      override def handle(ex: HttpExchange): Unit = f(ex)

  /** Lightweight in-process GCS JSON API v1 + OAuth2 + Metadata mock server. */
  private final class FakeGcsServer(
      val bucket: String,
      requiredToken: Option[String] = None,
      failFirstNRequests: Int = 0,
      failStatus: Int = 503,
      oauthVerifyKey: Option[PublicKey] = None,
      tokensToIssue: Seq[String] = Seq("issued-token-1"),
  ):
    val objects            = new ConcurrentHashMap[String, StoredObj]()
    val gcsRequestCount    = new AtomicInteger(0)
    val tokenRequestCount  = new AtomicInteger(0)
    val lastAuthHeader     = new java.util.concurrent.atomic.AtomicReference[Option[String]](None)
    val lastMetadataFlavor = new java.util.concurrent.atomic.AtomicReference[Option[String]](None)
    val activeToken        = new java.util.concurrent.atomic.AtomicReference[Option[String]](requiredToken)

    private val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)

    val endpoint: String         = s"http://127.0.0.1:${server.getAddress.getPort}"
    val tokenUri: String         = s"$endpoint/oauth2/token"
    val metadataTokenUrl: String = s"$endpoint/computeMetadata/v1/instance/service-accounts/default/token"

    server.createContext("/oauth2/token", handler(handleOAuthToken))
    server.createContext("/computeMetadata/", handler(handleMetadataToken))
    server.createContext("/upload/storage/v1/b/", handler(handleUpload))
    server.createContext("/storage/v1/b/", handler(handleStorage))
    server.start()

    def stop(): Unit = server.stop(0)

    private def sendJson(ex: HttpExchange, code: Int, body: String): Unit =
      val bytes = body.getBytes(StandardCharsets.UTF_8)
      ex.getResponseHeaders.set("Content-Type", "application/json; charset=UTF-8")
      ex.sendResponseHeaders(code, bytes.length.toLong)
      val os = ex.getResponseBody
      os.write(bytes)
      os.close()

    private def sendBytes(ex: HttpExchange, code: Int, contentType: String, body: Array[Byte]): Unit =
      ex.getResponseHeaders.set("Content-Type", contentType)
      ex.sendResponseHeaders(code, body.length.toLong)
      val os = ex.getResponseBody
      os.write(body)
      os.close()

    private def checkAuthAndFailures(ex: HttpExchange): Boolean =
      val authOpt = Option(ex.getRequestHeaders.getFirst("Authorization"))
      lastAuthHeader.set(authOpt)
      val n = gcsRequestCount.incrementAndGet()
      if n <= failFirstNRequests then
        sendJson(ex, failStatus, """{"error":"transient failure"}""")
        false
      else
        activeToken.get() match
          case Some(expected) if !authOpt.contains(s"Bearer $expected") =>
            sendJson(ex, 401, """{"error":"unauthorized"}""")
            false
          case _ =>
            true

    private def handleOAuthToken(ex: HttpExchange): Unit =
      val body = new String(ex.getRequestBody.readAllBytes(), StandardCharsets.UTF_8)
      val idx  = tokenRequestCount.getAndIncrement()
      val params = body.split('&').flatMap { kv =>
        kv.split("=", 2).toList match
          case k :: v :: Nil => Some(k -> URLDecoder.decode(v, StandardCharsets.UTF_8))
          case _             => None
      }.toMap

      val verified = params.get("grant_type") match
        case Some("urn:ietf:params:oauth:grant-type:jwt-bearer") =>
          params.get("assertion").exists { jwt =>
            oauthVerifyKey.forall(pub => verifyRs256Jwt(jwt, pub))
          }
        case Some("refresh_token") =>
          params.get("refresh_token").contains("ref-tok-123") &&
            params.get("client_id").contains("cid-1") &&
            params.get("client_secret").contains("csec-1")
        case _ => false

      if !verified then sendJson(ex, 400, """{"error":"invalid_grant"}""")
      else
        val tok = tokensToIssue(math.min(idx, tokensToIssue.size - 1))
        activeToken.set(Some(tok))
        sendJson(ex, 200, s"""{"access_token":"$tok","expires_in":3600,"token_type":"Bearer"}""")

    private def handleMetadataToken(ex: HttpExchange): Unit =
      val flavor = Option(ex.getRequestHeaders.getFirst("Metadata-Flavor"))
      lastMetadataFlavor.set(flavor)
      if !flavor.contains("Google") then sendJson(ex, 403, """{"error":"missing Metadata-Flavor"}""")
      else
        val idx = tokenRequestCount.getAndIncrement()
        val tok = tokensToIssue(math.min(idx, tokensToIssue.size - 1))
        activeToken.set(Some(tok))
        sendJson(ex, 200, s"""{"access_token":"$tok","expires_in":3600,"token_type":"Bearer"}""")

    private def handleUpload(ex: HttpExchange): Unit =
      if !checkAuthAndFailures(ex) then ()
      else if ex.getRequestMethod != "POST" then sendJson(ex, 405, "{}")
      else
        val path = ex.getRequestURI.getRawPath
        if path != s"/upload/storage/v1/b/$bucket/o" then sendJson(ex, 404, """{"error":"bucket not found"}""")
        else
          val ct       = Option(ex.getRequestHeaders.getFirst("Content-Type")).getOrElse("")
          val boundary = ct.split("boundary=").lift(1).map(_.trim).getOrElse("")
          val raw      = ex.getRequestBody.readAllBytes()
          parseMultipart(raw, boundary) match
            case Left(err) => sendJson(ex, 400, s"""{"error":"$err"}""")
            case Right((metaJson, mediaBytes)) =>
              val parsed = Json.decode[MockObjMeta](metaJson).getOrThrow
              val name   = parsed.name.getOrElse("")
              val mime   = parsed.contentType.getOrElse("application/octet-stream")
              val meta   = parsed.metadata.getOrElse(Map.empty)
              objects.put(name, StoredObj(mime, meta, mediaBytes))
              sendJson(ex, 200, metaJson)

    private def handleStorage(ex: HttpExchange): Unit =
      if !checkAuthAndFailures(ex) then ()
      else
        val rawPath = ex.getRequestURI.getRawPath
        val prefix  = s"/storage/v1/b/$bucket"
        if rawPath == prefix && ex.getRequestMethod == "GET" then
          sendJson(ex, 200, s"""{"name":"$bucket"}""")
        else if !rawPath.startsWith(s"$prefix/o/") then
          sendJson(ex, 404, """{"error":"not found"}""")
        else
          val encodedObj = rawPath.stripPrefix(s"$prefix/o/")
          val objName    = URLDecoder.decode(encodedObj, StandardCharsets.UTF_8)
          val query      = Option(ex.getRequestURI.getRawQuery).getOrElse("")
          ex.getRequestMethod match
            case "GET" =>
              Option(objects.get(objName)) match
                case None => sendJson(ex, 404, """{"error":"not found"}""")
                case Some(obj) =>
                  if query.contains("alt=media") then sendBytes(ex, 200, obj.contentType, obj.bytes)
                  else
                    val metaJson = Json.encode(
                      MockObjMeta(
                        name = Some(objName),
                        contentType = Some(obj.contentType),
                        metadata = if obj.metadata.isEmpty then None else Some(obj.metadata),
                      )
                    )
                    sendJson(ex, 200, metaJson)
            case "DELETE" =>
              if objects.remove(objName) != null then
                ex.sendResponseHeaders(204, -1)
                ex.close()
              else sendJson(ex, 404, """{"error":"not found"}""")
            case _ =>
              sendJson(ex, 405, "{}")

  private def verifyRs256Jwt(jwt: String, pub: PublicKey): Boolean =
    jwt.split('.') match
      case Array(h, c, s) =>
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(pub)
        sig.update(s"$h.$c".getBytes(StandardCharsets.UTF_8))
        sig.verify(Base64.getUrlDecoder.decode(s))
      case _ => false

  private def parseMultipart(raw: Array[Byte], boundary: String): Either[String, (String, Array[Byte])] =
    if boundary.isEmpty then Left("missing boundary")
    else
      val delim     = s"--$boundary".getBytes(StandardCharsets.ISO_8859_1)
      val crlf2     = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)
      def indexOf(arr: Array[Byte], target: Array[Byte], from: Int): Int =
        var i = from
        while i <= arr.length - target.length do
          var j = 0
          while j < target.length && arr(i + j) == target(j) do j += 1
          if j == target.length then return i
          i += 1
        -1
      val p1Start = indexOf(raw, delim, 0)
      val p1Hdr   = indexOf(raw, crlf2, p1Start + delim.length)
      val p2Start = indexOf(raw, delim, p1Hdr + crlf2.length)
      val p2Hdr   = indexOf(raw, crlf2, p2Start + delim.length)
      val endPos  = indexOf(raw, delim, p2Hdr + crlf2.length)
      if p1Start < 0 || p1Hdr < 0 || p2Start < 0 || p2Hdr < 0 || endPos < 0 then Left("malformed multipart")
      else
        val metaBytes  = java.util.Arrays.copyOfRange(raw, p1Hdr + crlf2.length, p2Start - 2)
        val mediaBytes = java.util.Arrays.copyOfRange(raw, p2Hdr + crlf2.length, endPos - 2)
        Right((new String(metaBytes, StandardCharsets.UTF_8), mediaBytes))

  private def generateRsaKeyPair(): (PublicKey, String) =
    val kpg  = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    val kp   = kpg.generateKeyPair()
    val b64  = Base64.getMimeEncoder(64, Array('\n'.toByte)).encodeToString(kp.getPrivate.getEncoded)
    val pem  = s"-----BEGIN PRIVATE KEY-----\n$b64\n-----END PRIVATE KEY-----\n"
    (kp.getPublic, pem)

  private def withServer[A](server: => FakeGcsServer)(f: FakeGcsServer => A < (Async & Abort[Any]))(using Frame): A < (Async & Abort[Any]) =
    Scope.run {
      Scope.acquireRelease(Sync.defer(server))(s => Sync.defer(s.stop())).map(f)
    }

  private val samplePdf = Chunk.from("%PDF-1.7\nhello gcs\n%%EOF".getBytes(StandardCharsets.UTF_8))

  "GcsBlobStorage" - {
    "stores, reads back (with checksum, mime & publicUrl), and idempotently deletes blobs" in {
      withServer(new FakeGcsServer("printcraft-art")) { srv =>
        val cfg = GcsConfig(
          bucket = "printcraft-art",
          credentials = GcsCredentials.Anonymous,
          endpoint = srv.endpoint,
          publicBaseUrl = Some("/api/v1/blobs"),
        )
        for
          store <- GcsBlobStorage.make(cfg)
          meta  <- store.put("uploads/ord-1/art-1-flyer.pdf", samplePdf, "application/pdf")
          back  <- store.get("uploads/ord-1/art-1-flyer.pdf")
          _     <- store.delete("uploads/ord-1/art-1-flyer.pdf")
          _     <- store.delete("uploads/ord-1/art-1-flyer.pdf") // idempotent on 404
          gone  <- store.get("uploads/ord-1/art-1-flyer.pdf")
          _     <- store.checkBucket
        yield
          assert(meta.key == "uploads/ord-1/art-1-flyer.pdf")
          assert(meta.publicUrl.contains("/api/v1/blobs/uploads/ord-1/art-1-flyer.pdf"))
          assert(meta.checksumSha256 == Sha256.hex(samplePdf.toArray))
          assert(meta.sizeBytes == samplePdf.size.toLong)
          assert(back.map(_.bytes).contains(samplePdf))
          assert(back.map(_.meta.mimeType).contains("application/pdf"))
          assert(back.map(_.meta.checksumSha256).contains(meta.checksumSha256))
          assert(gone.isEmpty)
          assert(srv.lastAuthHeader.get().isEmpty)
      }
    }

    "applies keyPrefix in GCS while keeping logical key and publicUrl clean" in {
      withServer(new FakeGcsServer("printcraft-art")) { srv =>
        val cfg = GcsConfig(
          bucket = "printcraft-art",
          credentials = GcsCredentials.Anonymous,
          endpoint = srv.endpoint,
          keyPrefix = Some("/env/prod/"),
          publicBaseUrl = Some("https://cdn.example.com/blobs"),
        )
        for
          store <- GcsBlobStorage.make(cfg)
          meta  <- store.put("uploads/a.pdf", samplePdf, "application/pdf")
          back  <- store.get("uploads/a.pdf")
        yield
          assert(srv.objects.containsKey("env/prod/uploads/a.pdf"))
          assert(meta.key == "uploads/a.pdf")
          assert(meta.publicUrl.contains("https://cdn.example.com/blobs/uploads/a.pdf"))
          assert(back.map(_.bytes).contains(samplePdf))
      }
    }

    "computes SHA-256 fallback when GCS object has no custom sha256 metadata" in {
      withServer(new FakeGcsServer("printcraft-art")) { srv =>
        srv.objects.put("external/doc.pdf", StoredObj("application/pdf", Map.empty, samplePdf.toArray))
        val cfg = GcsConfig(bucket = "printcraft-art", credentials = GcsCredentials.Anonymous, endpoint = srv.endpoint)
        for
          store <- GcsBlobStorage.make(cfg)
          back  <- store.get("external/doc.pdf")
        yield
          assert(back.isDefined)
          assert(back.get.meta.checksumSha256 == Sha256.hex(samplePdf.toArray))
          assert(back.get.meta.mimeType == "application/pdf")
      }
    }

    "rejects invalid keys and enforces maxBytes without hitting GCS" in {
      withServer(new FakeGcsServer("printcraft-art")) { srv =>
        val cfg = GcsConfig(
          bucket = "printcraft-art",
          credentials = GcsCredentials.Anonymous,
          endpoint = srv.endpoint,
          maxBytes = 8,
        )
        for
          store <- GcsBlobStorage.make(cfg)
          r1    <- store.put("../secret", Chunk(1.toByte), "text/plain").either
          r2    <- store.get("/etc/passwd").either
          r3    <- store.delete("a//b").either
          r4    <- store.put("uploads/big.bin", Chunk.fill(9)(1.toByte), "application/octet-stream").either
        yield
          assert(r1 == Left(BlobError.InvalidKey("../secret")))
          assert(r2 == Left(BlobError.InvalidKey("/etc/passwd")))
          assert(r3 == Left(BlobError.InvalidKey("a//b")))
          assert(r4 == Left(BlobError.TooLarge(9, 8)))
          assert(srv.gcsRequestCount.get() == 0)
      }
    }

    "integrates with BlobIngest for data URLs" in {
      withServer(new FakeGcsServer("printcraft-art")) { srv =>
        val cfg     = GcsConfig("printcraft-art", GcsCredentials.Anonymous, srv.endpoint, publicBaseUrl = Some("/api/v1/blobs"))
        val dataUrl = DataUrl.encode("application/pdf", samplePdf)
        for
          store  <- GcsBlobStorage.make(cfg)
          result <- BlobIngest.ingest(store, "drafts/s1/card.pdf", Some(dataUrl), Some(dataUrl))
          stored <- store.get("drafts/s1/card.pdf")
        yield
          assert(result.downloadUrl.contains("/api/v1/blobs/drafts/s1/card.pdf"))
          assert(result.previewUrl == result.downloadUrl)
          assert(result.checksumSha256.contains(Sha256.hex(samplePdf.toArray)))
          assert(stored.map(_.bytes).contains(samplePdf))
      }
    }

    "authenticates with ServiceAccount RS256 JWT, caches token, and refreshes on 401" in {
      val (pub, pem) = generateRsaKeyPair()
      withServer(new FakeGcsServer("printcraft-art", oauthVerifyKey = Some(pub), tokensToIssue = Seq("tok-1", "tok-2"))) { srv =>
        val creds = GcsCredentials.ServiceAccount("svc@proj.iam.gserviceaccount.com", pem, srv.tokenUri)
        val cfg   = GcsConfig("printcraft-art", creds, srv.endpoint)
        for
          store <- GcsBlobStorage.make(cfg)
          _     <- store.put("a/1.bin", samplePdf, "application/pdf")
          _     <- store.get("a/1.bin")
          c1     = srv.tokenRequestCount.get()
          // Simulate token revocation on server -> client should invalidate & refresh on 401
          _     <- Sync.defer(srv.activeToken.set(Some("tok-2")))
          back  <- store.get("a/1.bin")
          c2     = srv.tokenRequestCount.get()
        yield
          assert(c1 == 1) // cached across put + get(meta) + get(media)
          assert(c2 == 2) // refreshed once after 401
          assert(back.map(_.bytes).contains(samplePdf))
          assert(srv.lastAuthHeader.get().contains("Bearer tok-2"))
      }
    }

    "authenticates with AuthorizedUser refresh token" in {
      withServer(new FakeGcsServer("printcraft-art", tokensToIssue = Seq("user-tok-1"))) { srv =>
        val creds = GcsCredentials.AuthorizedUser("cid-1", "csec-1", "ref-tok-123", srv.tokenUri)
        val cfg   = GcsConfig("printcraft-art", creds, srv.endpoint)
        for
          store <- GcsBlobStorage.make(cfg)
          _     <- store.put("u/1.pdf", samplePdf, "application/pdf")
          back  <- store.get("u/1.pdf")
        yield
          assert(srv.tokenRequestCount.get() == 1)
          assert(srv.lastAuthHeader.get().contains("Bearer user-tok-1"))
          assert(back.map(_.bytes).contains(samplePdf))
      }
    }

    "authenticates with MetadataServer using Metadata-Flavor header" in {
      withServer(new FakeGcsServer("printcraft-art", tokensToIssue = Seq("meta-tok-1"))) { srv =>
        val creds = GcsCredentials.MetadataServer(srv.metadataTokenUrl)
        val cfg   = GcsConfig("printcraft-art", creds, srv.endpoint)
        for
          store <- GcsBlobStorage.make(cfg)
          _     <- store.put("m/1.pdf", samplePdf, "application/pdf")
        yield
          assert(srv.lastMetadataFlavor.get().contains("Google"))
          assert(srv.lastAuthHeader.get().contains("Bearer meta-tok-1"))
      }
    }

    "retries transient 503 errors and surfaces permanent 403 failures" in {
      for
        _ <- withServer(new FakeGcsServer("printcraft-art", failFirstNRequests = 2, failStatus = 503)) { srv =>
               val cfg = GcsConfig("printcraft-art", GcsCredentials.Anonymous, srv.endpoint, maxRetries = 3)
               for
                 store <- GcsBlobStorage.make(cfg)
                 meta  <- store.put("retry/ok.pdf", samplePdf, "application/pdf")
               yield
                 assert(meta.key == "retry/ok.pdf")
                 assert(srv.gcsRequestCount.get() == 3)
             }
        _ <- withServer(new FakeGcsServer("printcraft-art", failFirstNRequests = 5, failStatus = 403)) { srv =>
               val cfg = GcsConfig("printcraft-art", GcsCredentials.Anonymous, srv.endpoint, maxRetries = 3)
               for
                 store <- GcsBlobStorage.make(cfg)
                 res   <- store.put("denied/a.pdf", samplePdf, "application/pdf").either
               yield
                 assert(res.left.exists(_.isInstanceOf[BlobError.StorageFailure]))
                 assert(srv.gcsRequestCount.get() == 1) // 403 is not retried
             }
      yield ()
    }

    "fails checkBucket on nonexistent bucket" in {
      withServer(new FakeGcsServer("real-bucket")) { srv =>
        val cfg = GcsConfig("other-bucket", GcsCredentials.Anonymous, srv.endpoint, maxRetries = 0)
        for
          store <- GcsBlobStorage.make(cfg)
          res   <- store.checkBucket.either
        yield assert(res.isLeft)
      }
    }

    "parses service_account and authorized_user credentials JSON and validates bucket names" in {
      val saJson =
        """{"type":"service_account","client_email":"a@b.iam.gserviceaccount.com","private_key":"PEM","token_uri":"https://t"}"""
      val auJson =
        """{"type":"authorized_user","client_id":"cid","client_secret":"sec","refresh_token":"ref"}"""
      val sa = GcsCredentials.fromJson(saJson)
      val au = GcsCredentials.fromJson(auJson)
      val bad = GcsCredentials.fromJson("""{"type":"unknown"}""")

      Scope.run {
        Scope
          .acquireRelease(Sync.defer {
            val f = Files.createTempFile("gcs-sa-", ".json")
            Files.writeString(f, saJson, StandardCharsets.UTF_8)
          })(f => Sync.defer { val _ = Files.deleteIfExists(f) })
          .map { tmpFile =>
            for
              fromFile   <- GcsCredentials.fromFile(tmpFile)
              cfgFromMap <- GcsConfig.fromMap(
                              env = Map("GCS_BUCKET" -> "my-print-bucket", "GCS_ENDPOINT" -> "http://localhost:4443"),
                              flags = Map("gcs-prefix" -> "/art/"),
                            )
            yield
              assert(sa == Right(GcsCredentials.ServiceAccount("a@b.iam.gserviceaccount.com", "PEM", "https://t")))
              assert(au == Right(GcsCredentials.AuthorizedUser("cid", "sec", "ref", GcsConfig.DefaultTokenUri)))
              assert(bad.isLeft)
              assert(fromFile == GcsCredentials.ServiceAccount("a@b.iam.gserviceaccount.com", "PEM", "https://t"))
              assert(cfgFromMap.bucket == "my-print-bucket")
              assert(cfgFromMap.keyPrefix.contains("art"))
              assert(cfgFromMap.credentials == GcsCredentials.Anonymous)
              assert(GcsConfig.validateBucket("valid-bucket_1.2").isRight)
              assert(GcsConfig.validateBucket("INVALID").isLeft)
              assert(GcsConfig.validateBucket("ab").isLeft)
          }
      }
    }
  }
