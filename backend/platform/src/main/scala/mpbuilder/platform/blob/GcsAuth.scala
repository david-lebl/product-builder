package mpbuilder.platform
package blob

import kyo.*
import mpbuilder.shared.*

import java.net.URI
import java.net.URLEncoder
import java.net.http.{HttpClient as JHttpClient, HttpRequest as JHttpRequest, HttpResponse as JHttpResponse}
import java.nio.charset.StandardCharsets
import java.security.{KeyFactory, PrivateKey, Signature}
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CompletableFuture

/** OAuth2 bearer-token provider for Google Cloud Storage with thread-safe caching and automatic refresh. */
private[blob] final class GcsAuth(
    credentials: GcsCredentials,
    httpClient: JHttpClient,
    cache: AtomicRef[Option[GcsAuth.CachedToken]],
    timeout: Duration,
):

  /** Returns the `Authorization` header value (`"Bearer ..."`), or `None` for anonymous access. */
  def authorizationHeader(using Frame): Option[String] < (Async & Abort[BlobError]) =
    credentials match
      case GcsCredentials.Anonymous          => None
      case GcsCredentials.AccessToken(token) => Some(s"Bearer $token")
      case _ =>
        for
          now    <- Sync.defer(java.lang.System.currentTimeMillis() / 1000L)
          cached <- cache.get
          token  <- (cached match
                      case Some(t) if t.expiresAtEpochSeconds - GcsAuth.RefreshSkewSeconds > now =>
                        t.accessToken
                      case _ =>
                        fetchAndCache(now)
                    ): String < (Async & Abort[BlobError])
        yield Some(s"Bearer $token")

  /** Clears any cached token (e.g. after a 401 Unauthorized response). */
  def invalidate(using Frame): Unit < Sync =
    cache.set(None)

  private def fetchAndCache(nowEpochSeconds: Long)(using Frame): String < (Async & Abort[BlobError]) =
    for
      req  <- buildTokenRequest(nowEpochSeconds)
      resp <- GcsHttp.sendString(httpClient, req)
      _    <- Effects.failUnless(resp.statusCode() / 100 == 2)(
                BlobError.StorageFailure(s"GCS auth failed with HTTP ${resp.statusCode()}: ${resp.body().take(300)}")
              )
      parsed <- Json.decode[GcsAuth.TokenResponse](resp.body()).toEither.left
                  .map(e => BlobError.StorageFailure(s"Invalid GCS token response: ${e.getMessage}"))
                  .orAbort
      ttl     = parsed.expires_in.getOrElse(3600L)
      _      <- cache.set(Some(GcsAuth.CachedToken(parsed.access_token, nowEpochSeconds + ttl)))
    yield parsed.access_token

  private def buildTokenRequest(nowEpochSeconds: Long)(using Frame): JHttpRequest < (Sync & Abort[BlobError]) =
    credentials match
      case GcsCredentials.ServiceAccount(email, pem, tokenUri) =>
        Sync.defer(GcsAuth.signJwt(email, pem, tokenUri, nowEpochSeconds)).map(_.orAbort).map { jwt =>
          val body = s"grant_type=${GcsAuth.urlEncode("urn:ietf:params:oauth:grant-type:jwt-bearer")}&assertion=${GcsAuth.urlEncode(jwt)}"
          JHttpRequest.newBuilder(URI.create(tokenUri))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(JHttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build()
        }
      case GcsCredentials.AuthorizedUser(clientId, clientSecret, refreshToken, tokenUri) =>
        val body =
          s"grant_type=refresh_token&client_id=${GcsAuth.urlEncode(clientId)}" +
            s"&client_secret=${GcsAuth.urlEncode(clientSecret)}" +
            s"&refresh_token=${GcsAuth.urlEncode(refreshToken)}"
        JHttpRequest.newBuilder(URI.create(tokenUri))
          .timeout(timeout)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(JHttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
          .build()
      case GcsCredentials.MetadataServer(url) =>
        JHttpRequest.newBuilder(URI.create(url))
          .timeout(timeout)
          .header("Metadata-Flavor", "Google")
          .GET()
          .build()
      case GcsCredentials.Anonymous | GcsCredentials.AccessToken(_) =>
        Abort.fail(BlobError.StorageFailure("Unexpected token request for non-exchangeable credentials"))

private[blob] object GcsAuth:

  val RefreshSkewSeconds: Long = 60L

  final case class CachedToken(accessToken: String, expiresAtEpochSeconds: Long)

  final case class TokenResponse(access_token: String, expires_in: Option[Long] = None)
  given Schema[TokenResponse] = Schema.derived

  private final case class JwtHeader(alg: String, typ: String)
  private final case class JwtClaims(iss: String, scope: String, aud: String, iat: Long, exp: Long)
  private given Schema[JwtHeader] = Schema.derived
  private given Schema[JwtClaims] = Schema.derived

  private val base64Url = Base64.getUrlEncoder.withoutPadding()

  def make(credentials: GcsCredentials, httpClient: JHttpClient, timeout: Duration)(using Frame): GcsAuth < Sync =
    AtomicRef.init(Option.empty[CachedToken]).map(new GcsAuth(credentials, httpClient, _, timeout))

  def urlEncode(s: String): String =
    URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20")

  /** Parses a PKCS#8 PEM-encoded RSA private key. */
  def parsePrivateKey(pem: String): Either[BlobError, PrivateKey] =
    try
      val stripped = pem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "")
      val der  = Base64.getDecoder.decode(stripped)
      val spec = new PKCS8EncodedKeySpec(der)
      Right(KeyFactory.getInstance("RSA").generatePrivate(spec))
    catch case e: Exception => Left(BlobError.StorageFailure(s"Invalid RSA private key PEM: ${e.getMessage}"))

  /** Builds and signs a Google OAuth2 RS256 JWT assertion. */
  def signJwt(
      clientEmail: String,
      privateKeyPem: String,
      tokenUri: String,
      nowEpochSeconds: Long,
  )(using Frame): Either[BlobError, String] =
    parsePrivateKey(privateKeyPem).flatMap { key =>
      try
        val headerJson = Json.encode(JwtHeader("RS256", "JWT"))
        val claimsJson = Json.encode(
          JwtClaims(
            iss = clientEmail,
            scope = GcsConfig.ScopeReadWrite,
            aud = tokenUri,
            iat = nowEpochSeconds,
            exp = nowEpochSeconds + 3600L,
          )
        )
        val signingInput =
          s"${base64Url.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8))}." +
            s"${base64Url.encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8))}"
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(key)
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8))
        val encodedSig = base64Url.encodeToString(sig.sign())
        Right(s"$signingInput.$encodedSig")
      catch case e: Exception => Left(BlobError.StorageFailure(s"Failed to sign GCS JWT: ${e.getMessage}"))
    }

/** Non-blocking bridge from `java.net.http.HttpClient.sendAsync` to Kyo `Async`. */
private[blob] object GcsHttp:

  def sendString(client: JHttpClient, request: JHttpRequest)(using Frame): JHttpResponse[String] < (Async & Abort[BlobError]) =
    fromCompletableFuture(client.sendAsync(request, JHttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)))

  def sendBytes(client: JHttpClient, request: JHttpRequest)(using Frame): JHttpResponse[Array[Byte]] < (Async & Abort[BlobError]) =
    fromCompletableFuture(client.sendAsync(request, JHttpResponse.BodyHandlers.ofByteArray()))

  private def fromCompletableFuture[A](cf: => CompletableFuture[A])(using Frame): A < (Async & Abort[BlobError]) =
    Promise.init[Either[BlobError, A], Any].map { promise =>
      Sync.defer {
        val future = cf
        future.whenComplete { (value, err) =>
          import AllowUnsafe.embrace.danger
          if err != null then
            val cause = Option(err.getCause).getOrElse(err)
            val msg   = Option(cause.getMessage).filter(_.nonEmpty).getOrElse(cause.getClass.getSimpleName)
            val _     = promise.unsafe.completeDiscard(Result.succeed(Left(BlobError.StorageFailure(msg))))
          else
            val _ = promise.unsafe.completeDiscard(Result.succeed(Right(value)))
        }
        ()
      }.andThen(promise.get.map(_.orAbort))
    }
