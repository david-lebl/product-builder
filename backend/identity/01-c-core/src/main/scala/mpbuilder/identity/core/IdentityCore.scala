package mpbuilder.identity
package core

import mpbuilder.shared.*
import zio.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

// ── Internal Aggregates & Value Objects (compile-time isolated via `private[identity]`) ──

private[identity] final case class PasswordCredential(
    email: EmailAddress,
    passwordHash: String,
    salt: String,
    principal: Principal,
    isActive: Boolean = true,
)

private[identity] final case class OtpChallenge(
    id: OtpChallengeId,
    principal: Principal,
    codeHash: String,
    salt: String,
    maskedDestination: String,
    issuedAt: Instant,
    expiresAt: Instant,
    attempts: Int = 0,
    maxAttempts: Int = 5,
    consumed: Boolean = false,
):
  def toView: OtpChallengeView =
    OtpChallengeView(id, principal, maskedDestination, expiresAt)

private[identity] final case class StoredSession(
    token: SessionToken,
    principal: Principal,
    method: AuthMethod,
    issuedAt: Instant,
    expiresAt: Instant,
    revoked: Boolean = false,
):
  def toPublic: AuthSession =
    AuthSession(token, principal, method, issuedAt, expiresAt)

/** ACL projection returned by `CustomerAccountLookupPort` (decouples `identity` from `customers`). */
private[identity] final case class ResolvedAccount(
    principal: Principal,
    isActive: Boolean,
    inactiveReason: Option[String] = None,
)

private[identity] final case class IdentityConfig(
    otpTtl: Duration = 5.minutes,
    otpMaxAttempts: Int = 5,
    sessionTtl: Duration = 24.hours,
)

// ── Pure Hashing & Masking Helpers ───────────────────────────────────────────

private[identity] object SecretHasher:
  def hash(secret: String, salt: String): String =
    val md    = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(s"$salt:$secret".getBytes(StandardCharsets.UTF_8))
    bytes.map(b => f"$b%02x").mkString

  def verify(secret: String, salt: String, expectedHash: String): Boolean =
    val actual = hash(secret, salt)
    MessageDigest.isEqual(
      actual.getBytes(StandardCharsets.UTF_8),
      expectedHash.getBytes(StandardCharsets.UTF_8),
    )

  def maskEmail(email: EmailAddress): String =
    val raw = email.value
    raw.indexOf('@') match
      case idx if idx > 2 => s"${raw.take(2)}***${raw.substring(idx)}"
      case idx if idx > 0 => s"${raw.take(1)}***${raw.substring(idx)}"
      case _              => "***"

// ── Outbound Ports (implemented by DB / ACL / Email adapters) ────────────────

private[identity] trait CredentialRepository:
  def findByEmail(email: EmailAddress): UIO[Option[PasswordCredential]]
  def save(credential: PasswordCredential): UIO[Unit]

private[identity] trait OtpRepository:
  def findById(id: OtpChallengeId): UIO[Option[OtpChallenge]]
  def save(challenge: OtpChallenge): UIO[Unit]

private[identity] trait SessionRepository:
  def findByToken(token: SessionToken): UIO[Option[StoredSession]]
  def save(session: StoredSession): UIO[Unit]
  def revoke(token: SessionToken): UIO[Unit]

private[identity] trait TokenGenerator:
  def generateSessionToken: UIO[SessionToken]
  def generateOtpChallengeId: UIO[OtpChallengeId]
  def generateOtpCode: UIO[OtpCode]
  def generateSalt: UIO[String]

private[identity] trait OtpDeliveryPort:
  def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): UIO[Unit]

/** Outbound ACL port to resolve customer accounts by Email / IČO / DIČ without depending on `customers`. */
private[identity] trait CustomerAccountLookupPort:
  def resolveCustomer(identifier: LoginIdentifier): UIO[Option[ResolvedAccount]]

// ── Live Service Implementation ──────────────────────────────────────────────

private[identity] final class IdentityServiceLive(
    credentials: CredentialRepository,
    otps: OtpRepository,
    sessions: SessionRepository,
    customerLookup: CustomerAccountLookupPort,
    otpDelivery: OtpDeliveryPort,
    tokens: TokenGenerator,
    eventBus: EventBus,
    config: IdentityConfig = IdentityConfig(),
) extends IdentityService:

  override def registerPasswordCredential(cmd: RegisterPasswordCmd): IO[IdentityError, Unit] =
    for
      existing <- credentials.findByEmail(cmd.email)
      _        <- ZIO.fail(IdentityError.CredentialAlreadyExists(cmd.email)).when(existing.isDefined)
      salt     <- tokens.generateSalt
      hash      = SecretHasher.hash(cmd.rawPassword.value, salt)
      _        <- credentials.save(PasswordCredential(cmd.email, hash, salt, cmd.principal, isActive = true))
    yield ()

  override def loginWithPassword(
      email: EmailAddress,
      rawPassword: NonEmptyString,
  ): IO[IdentityError, AuthSession] =
    for
      now  <- Clock.instant
      cred <- credentials.findByEmail(email).someOrFail(IdentityError.InvalidCredentials)
      _    <- ZIO.fail(IdentityError.AccountNotActive("Account is disabled")).unless(cred.isActive)
      ok    = SecretHasher.verify(rawPassword.value, cred.salt, cred.passwordHash)
      _    <- ZIO.fail(IdentityError.InvalidCredentials).unless(ok)
      sess <- issueSession(cred.principal, AuthMethod.Password, now)
    yield sess

  override def requestCustomerOtp(identifier: LoginIdentifier): IO[IdentityError, OtpChallengeView] =
    for
      now      <- Clock.instant
      resolved <- customerLookup.resolveCustomer(identifier).someOrFail(IdentityError.AccountNotFound(identifier))
      _        <- ZIO
                    .fail(IdentityError.AccountNotActive(resolved.inactiveReason.getOrElse("Account not active")))
                    .unless(resolved.isActive)
      cid      <- tokens.generateOtpChallengeId
      code     <- tokens.generateOtpCode
      salt     <- tokens.generateSalt
      challenge = OtpChallenge(
                    id = cid,
                    principal = resolved.principal,
                    codeHash = SecretHasher.hash(code.value, salt),
                    salt = salt,
                    maskedDestination = SecretHasher.maskEmail(resolved.principal.email),
                    issuedAt = now,
                    expiresAt = now.plusMillis(config.otpTtl.toMillis),
                    attempts = 0,
                    maxAttempts = config.otpMaxAttempts,
                    consumed = false,
                  )
      _        <- otps.save(challenge)
      _        <- otpDelivery.sendOtp(cid, resolved.principal, code)
      _        <- eventBus.publish(OtpRequested(cid, resolved.principal, now))
    yield challenge.toView

  override def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): IO[IdentityError, AuthSession] =
    for
      now       <- Clock.instant
      challenge <- otps.findById(challengeId).someOrFail(IdentityError.OtpChallengeNotFound(challengeId))
      _         <- ZIO.fail(IdentityError.OtpAlreadyUsed(challengeId)).when(challenge.consumed)
      _         <- ZIO.fail(IdentityError.OtpExpired(challengeId)).when(now.isAfter(challenge.expiresAt))
      _         <- ZIO.fail(IdentityError.OtpTooManyAttempts(challengeId)).when(challenge.attempts >= challenge.maxAttempts)
      matches    = SecretHasher.verify(code.value, challenge.salt, challenge.codeHash)
      session   <-
        if matches then
          otps.save(challenge.copy(consumed = true)) *>
            issueSession(challenge.principal, AuthMethod.Otp, now)
        else
          val nextAttempts = challenge.attempts + 1
          val remaining    = math.max(0, challenge.maxAttempts - nextAttempts)
          otps.save(challenge.copy(attempts = nextAttempts)) *>
            (if remaining == 0 then ZIO.fail(IdentityError.OtpTooManyAttempts(challengeId))
             else ZIO.fail(IdentityError.OtpInvalid(remaining)))
    yield session

  override def validateSession(token: SessionToken): IO[IdentityError, AuthSession] =
    for
      now  <- Clock.instant
      sess <- sessions.findByToken(token).someOrFail(IdentityError.SessionNotFound)
      _    <- ZIO.fail(IdentityError.SessionNotFound).when(sess.revoked)
      _    <- ZIO.fail(IdentityError.SessionExpired).when(now.isAfter(sess.expiresAt))
    yield sess.toPublic

  override def logout(token: SessionToken): UIO[Unit] =
    for
      now     <- Clock.instant
      sessOpt <- sessions.findByToken(token)
      _       <- sessions.revoke(token)
      _       <- ZIO.foreachDiscard(sessOpt.filterNot(_.revoked))(s => eventBus.publish(UserLoggedOut(s.principal, now)))
    yield ()

  private def issueSession(principal: Principal, method: AuthMethod, now: Instant): UIO[AuthSession] =
    for
      token <- tokens.generateSessionToken
      stored = StoredSession(
                 token = token,
                 principal = principal,
                 method = method,
                 issuedAt = now,
                 expiresAt = now.plusMillis(config.sessionTtl.toMillis),
                 revoked = false,
               )
      _     <- sessions.save(stored)
      _     <- eventBus.publish(UserLoggedIn(principal, method, now))
    yield stored.toPublic

private[identity] object IdentityCoreLayers:
  def layer(config: IdentityConfig = IdentityConfig()): ZLayer[
    CredentialRepository & OtpRepository & SessionRepository & CustomerAccountLookupPort & OtpDeliveryPort & TokenGenerator & EventBus,
    Nothing,
    IdentityService,
  ] =
    ZLayer.fromFunction(
      (
          c: CredentialRepository,
          o: OtpRepository,
          s: SessionRepository,
          l: CustomerAccountLookupPort,
          d: OtpDeliveryPort,
          t: TokenGenerator,
          e: EventBus,
      ) => new IdentityServiceLive(c, o, s, l, d, t, e, config)
    )
