package mpbuilder.identity
package impl
package jwt

import mpbuilder.commons.*
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtOptions}
import zio.*
import zio.json.*

/** HS256 JWTs.
  *
  * A symmetric algorithm is right for a modular monolith: one process signs and verifies, so there
  * is no public key to distribute. Extracting a context to its own service is the point at which
  * this should become RS256 — the port makes that a one-class change.
  */
private[identity] final class JwtTokenIssuer(
    secret: String,
    accessTtl: Duration = 15.minutes,
    refreshTtl: Duration = 30.days,
) extends TokenIssuer:

  private val algorithm = JwtAlgorithm.HS256

  // NB: the user id is carried as `uid`, not `sub`. `sub` is a JWT *registered* claim, which
  // jwt-scala lifts out of the payload on decode — a field named `sub` would silently vanish.
  private case class AccessClaims(
      uid: String,
      email: String,
      roles: List[String],
      customerId: Option[String],
      typ: String,
  )
  private object AccessClaims:
    given JsonCodec[AccessClaims] = DeriveJsonCodec.gen[AccessClaims]

  def issue(principal: Principal, now: Timestamp): UIO[AuthTokens] =
    ZIO.succeed {
      val issuedAt = now.epochMillis / 1000
      val access = encode(
        AccessClaims(
          uid = principal.userId,
          email = principal.email,
          roles = principal.roles.map(_.name).toList,
          customerId = principal.customerId,
          typ = "access",
        ).toJson,
        issuedAt,
        accessTtl,
      )
      // The refresh token deliberately carries no roles — they are re-read from storage on
      // refresh, so revoking a role cannot be outlived by a 30-day token.
      val refresh = encode(
        AccessClaims(principal.userId, "", Nil, None, "refresh").toJson,
        issuedAt,
        refreshTtl,
      )
      AuthTokens(access, refresh, accessTtl.toSeconds)
    }

  def verifyAccess(token: String, now: Timestamp): IO[AuthError, Principal] =
    decode(token, now, expected = "access").flatMap { claims =>
      ZIO.succeed(
        Principal(
          userId = claims.uid,
          email = claims.email,
          roles = claims.roles.flatMap(Role.parse).toSet,
          customerId = claims.customerId,
        )
      )
    }

  def verifyRefresh(token: String, now: Timestamp): IO[AuthError, User.Id] =
    decode(token, now, expected = "refresh").map(claims => User.Id(claims.uid))

  private def encode(payload: String, issuedAt: Long, ttl: Duration): String =
    Jwt.encode(
      JwtClaim(content = payload, issuedAt = Some(issuedAt), expiration = Some(issuedAt + ttl.toSeconds)),
      secret,
      algorithm,
    )

  private def decode(token: String, now: Timestamp, expected: String): IO[AuthError, AccessClaims] =
    // jwt-scala validates `exp` against the *system* clock, which would silently override the
    // `now` this port is given — making the injected clock a lie and the expiry path untestable.
    // Signature verification (the part that must not be skipped) still happens; expiry is checked
    // below, against the timestamp the caller actually passed.
    ZIO
      .fromTry(
        Jwt.decode(token, secret, Seq(algorithm), JwtOptions(expiration = false, notBefore = false))
      )
      .orElseFail(AuthError.TokenInvalid)
      .flatMap { claim =>
        val expired = claim.expiration.exists(_ * 1000 <= now.epochMillis)
        if expired then ZIO.fail(AuthError.TokenExpired)
        else
          ZIO
            .fromEither(claim.content.fromJson[AccessClaims])
            .orElseFail(AuthError.TokenInvalid)
            .filterOrFail(_.typ == expected)(AuthError.TokenInvalid)
      }

private[identity] object JwtTokenIssuer:
  def layer(secret: String): ULayer[TokenIssuer] = ZLayer.succeed(new JwtTokenIssuer(secret))
