package mpbuilder.identity
package core

import mpbuilder.shared.*
import kyo.*
import java.nio.charset.StandardCharsets
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

// ── Pure Cross-Platform SHA-256 Hashing & Masking Helpers ────────────────────

private[identity] object SecretHasher:
  private val K: Array[Int] = Array(
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
  )

  private def sha256(input: Array[Byte]): Array[Byte] =
    val bitLen   = input.length.toLong * 8L
    val padZeros = (55 - (input.length % 64) + 64) % 64
    val padded   = new Array[Byte](input.length + 1 + padZeros + 8)
    java.lang.System.arraycopy(input, 0, padded, 0, input.length)
    padded(input.length) = 0x80.toByte
    var i = 0
    while i < 8 do
      padded(padded.length - 1 - i) = ((bitLen >>> (i * 8)) & 0xffL).toByte
      i += 1

    var h0 = 0x6a09e667
    var h1 = 0xbb67ae85
    var h2 = 0x3c6ef372
    var h3 = 0xa54ff53a
    var h4 = 0x510e527f
    var h5 = 0x9b05688c
    var h6 = 0x1f83d9ab
    var h7 = 0x5be0cd19

    val w = new Array[Int](64)
    var offset = 0
    while offset < padded.length do
      var j = 0
      while j < 16 do
        val base = offset + j * 4
        w(j) =
          ((padded(base) & 0xff) << 24) |
          ((padded(base + 1) & 0xff) << 16) |
          ((padded(base + 2) & 0xff) << 8) |
          (padded(base + 3) & 0xff)
        j += 1
      while j < 64 do
        val s0 = Integer.rotateRight(w(j - 15), 7) ^ Integer.rotateRight(w(j - 15), 18) ^ (w(j - 15) >>> 3)
        val s1 = Integer.rotateRight(w(j - 2), 17) ^ Integer.rotateRight(w(j - 2), 19) ^ (w(j - 2) >>> 10)
        w(j) = w(j - 16) + s0 + w(j - 7) + s1
        j += 1

      var a = h0; var b = h1; var c = h2; var d = h3
      var e = h4; var f = h5; var g = h6; var h = h7
      var t = 0
      while t < 64 do
        val s1    = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25)
        val ch    = (e & f) ^ (~e & g)
        val temp1 = h + s1 + ch + K(t) + w(t)
        val s0    = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22)
        val maj   = (a & b) ^ (a & c) ^ (b & c)
        val temp2 = s0 + maj
        h = g; g = f; f = e; e = d + temp1
        d = c; c = b; b = a; a = temp1 + temp2
        t += 1

      h0 += a; h1 += b; h2 += c; h3 += d
      h4 += e; h5 += f; h6 += g; h7 += h
      offset += 64

    val out = new Array[Byte](32)
    val hs  = Array(h0, h1, h2, h3, h4, h5, h6, h7)
    var k   = 0
    while k < 8 do
      out(k * 4)     = ((hs(k) >>> 24) & 0xff).toByte
      out(k * 4 + 1) = ((hs(k) >>> 16) & 0xff).toByte
      out(k * 4 + 2) = ((hs(k) >>> 8) & 0xff).toByte
      out(k * 4 + 3) = (hs(k) & 0xff).toByte
      k += 1
    out

  private def constantTimeEquals(a: Array[Byte], b: Array[Byte]): Boolean =
    if a.length != b.length then false
    else
      var diff = 0
      var i    = 0
      while i < a.length do
        diff |= (a(i) ^ b(i))
        i += 1
      diff == 0

  def hash(secret: String, salt: String): String =
    val bytes = sha256(s"$salt:$secret".getBytes(StandardCharsets.UTF_8))
    val hex   = "0123456789abcdef"
    val sb    = new StringBuilder(bytes.length * 2)
    var i     = 0
    while i < bytes.length do
      val v = bytes(i) & 0xff
      sb.append(hex.charAt(v >>> 4)).append(hex.charAt(v & 0x0f))
      i += 1
    sb.toString

  def verify(secret: String, salt: String, expectedHash: String): Boolean =
    val actual = hash(secret, salt)
    constantTimeEquals(
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
  def findByEmail(email: EmailAddress): Option[PasswordCredential] < Async
  def listAll: List[PasswordCredential] < Async = Kyo.lift(Nil)
  def save(credential: PasswordCredential): Unit < Async

private[identity] trait OtpRepository:
  def findById(id: OtpChallengeId): Option[OtpChallenge] < Async
  def save(challenge: OtpChallenge): Unit < Async

private[identity] trait SessionRepository:
  def findByToken(token: SessionToken): Option[StoredSession] < Async
  def save(session: StoredSession): Unit < Async
  def revoke(token: SessionToken): Unit < Async

private[identity] trait TokenGenerator:
  def generateSessionToken: SessionToken < Async
  def generateOtpChallengeId: OtpChallengeId < Async
  def generateOtpCode: OtpCode < Async
  def generateSalt: String < Async

private[identity] trait OtpDeliveryPort:
  def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): Unit < Async

/** Outbound ACL port to resolve customer accounts by Email / IČO / DIČ without depending on `customers`. */
private[identity] trait CustomerAccountLookupPort:
  def resolveCustomer(identifier: LoginIdentifier): Option[ResolvedAccount] < Async

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

  override def registerPasswordCredential(cmd: RegisterPasswordCmd): Unit < (Async & Abort[IdentityError]) =
    for
      existing <- credentials.findByEmail(cmd.email)
      _        <- Effects.failWhen(existing.isDefined)(IdentityError.CredentialAlreadyExists(cmd.email))
      salt     <- tokens.generateSalt
      hash      = SecretHasher.hash(cmd.rawPassword.value, salt)
      _        <- credentials.save(PasswordCredential(cmd.email, hash, salt, cmd.principal, isActive = true))
    yield ()

  override def loginWithPassword(
      email: EmailAddress,
      rawPassword: NonEmptyString,
  ): AuthSession < (Async & Abort[IdentityError]) =
    for
      now  <- Effects.now
      cred <- credentials.findByEmail(email).someOrFail(IdentityError.InvalidCredentials)
      _    <- Effects.failUnless(cred.isActive)(IdentityError.AccountNotActive("Account is disabled"))
      ok    = SecretHasher.verify(rawPassword.value, cred.salt, cred.passwordHash)
      _    <- Effects.failUnless(ok)(IdentityError.InvalidCredentials)
      sess <- issueSession(cred.principal, AuthMethod.Password, now)
    yield sess

  override def requestCustomerOtp(identifier: LoginIdentifier): OtpChallengeView < (Async & Abort[IdentityError]) =
    for
      now      <- Effects.now
      resolved <- customerLookup.resolveCustomer(identifier).someOrFail(IdentityError.AccountNotFound(identifier))
      _        <- Effects.failUnless(resolved.isActive)(IdentityError.AccountNotActive(resolved.inactiveReason.getOrElse("Account not active")))
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

  override def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): AuthSession < (Async & Abort[IdentityError]) =
    for
      now       <- Effects.now
      challenge <- otps.findById(challengeId).someOrFail(IdentityError.OtpChallengeNotFound(challengeId))
      _         <- Effects.failWhen(challenge.consumed)(IdentityError.OtpAlreadyUsed(challengeId))
      _         <- Effects.failWhen(now.isAfter(challenge.expiresAt))(IdentityError.OtpExpired(challengeId))
      _         <- Effects.failWhen(challenge.attempts >= challenge.maxAttempts)(IdentityError.OtpTooManyAttempts(challengeId))
      matches    = SecretHasher.verify(code.value, challenge.salt, challenge.codeHash)
      session   <-
        if matches then
          otps.save(challenge.copy(consumed = true)) *>
            issueSession(challenge.principal, AuthMethod.Otp, now)
        else
          val nextAttempts = challenge.attempts + 1
          val remaining    = math.max(0, challenge.maxAttempts - nextAttempts)
          otps.save(challenge.copy(attempts = nextAttempts)) *>
            (if remaining == 0 then Abort.fail(IdentityError.OtpTooManyAttempts(challengeId))
             else Abort.fail(IdentityError.OtpInvalid(remaining)))
    yield session

  override def validateSession(token: SessionToken): AuthSession < (Async & Abort[IdentityError]) =
    for
      now  <- Effects.now
      sess <- sessions.findByToken(token).someOrFail(IdentityError.SessionNotFound)
      _    <- Effects.failWhen(sess.revoked)(IdentityError.SessionNotFound)
      _    <- Effects.failWhen(now.isAfter(sess.expiresAt))(IdentityError.SessionExpired)
    yield sess.toPublic

  override def logout(token: SessionToken): Unit < Async =
    for
      now     <- Effects.now
      sessOpt <- sessions.findByToken(token)
      _       <- sessions.revoke(token)
      _       <- Kyo.foreachDiscard(sessOpt.filterNot(_.revoked))(s => eventBus.publish(UserLoggedOut(s.principal, now)))
    yield ()

  override def listStaffMembers: List[StaffMemberView] < Async =
    credentials.listAll.map { creds =>
      creds.collect { case PasswordCredential(email, _, _, Principal.Employee(empId, name, _, roles), active) =>
        StaffMemberView(empId, name, email, roles, active)
      }.sortBy(_.employeeId.value)
    }

  override def createStaffMember(cmd: CreateStaffCmd): StaffMemberView < (Async & Abort[IdentityError]) =
    val principal = Principal.Employee(cmd.employeeId, cmd.displayName, cmd.email, cmd.roles)
    registerPasswordCredential(RegisterPasswordCmd(principal, cmd.email, cmd.initialPassword))
      .as(StaffMemberView(cmd.employeeId, cmd.displayName, cmd.email, cmd.roles, isActive = true))

  override def updateStaffRoles(
      employeeId: EmployeeId,
      roles: Set[EmployeeRole],
  ): StaffMemberView < (Async & Abort[IdentityError]) =
    for
      all  <- credentials.listAll
      cred <- all.find(_.principal.employeeIdOpt.contains(employeeId)).orFail(IdentityError.StaffNotFound(employeeId))
      emp  <- cred.principal match
                case e: Principal.Employee =>
                  Kyo.lift(new Principal.Employee(e.employeeId, e.displayName, e.email, roles))
                case _ =>
                  Abort.fail(IdentityError.StaffNotFound(employeeId))
      upd   = cred.copy(principal = emp)
      _    <- credentials.save(upd)
    yield StaffMemberView(emp.employeeId, emp.displayName, upd.email, emp.roles, upd.isActive)

  override def setStaffActive(
      employeeId: EmployeeId,
      isActive: Boolean,
  ): StaffMemberView < (Async & Abort[IdentityError]) =
    for
      all  <- credentials.listAll
      cred <- all.find(_.principal.employeeIdOpt.contains(employeeId)).orFail(IdentityError.StaffNotFound(employeeId))
      emp  <- cred.principal match
                case e: Principal.Employee => Kyo.lift(e)
                case _                     => Abort.fail(IdentityError.StaffNotFound(employeeId))
      upd   = cred.copy(isActive = isActive)
      _    <- credentials.save(upd)
    yield StaffMemberView(emp.employeeId, emp.displayName, upd.email, emp.roles, upd.isActive)

  override def resetPassword(email: EmailAddress, newRawPassword: NonEmptyString): Unit < (Async & Abort[IdentityError]) =
    for
      cred <- credentials.findByEmail(email).someOrFail(IdentityError.AccountNotFound(LoginIdentifier.Email(email)))
      salt <- tokens.generateSalt
      hash  = SecretHasher.hash(newRawPassword.value, salt)
      _    <- credentials.save(cred.copy(passwordHash = hash, salt = salt))
    yield ()

  private def issueSession(principal: Principal, method: AuthMethod, now: Instant): AuthSession < Async =
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
  def make(
      credentials: CredentialRepository,
      otps: OtpRepository,
      sessions: SessionRepository,
      customerLookup: CustomerAccountLookupPort,
      otpDelivery: OtpDeliveryPort,
      tokens: TokenGenerator,
      eventBus: EventBus,
      config: IdentityConfig = IdentityConfig(),
  ): IdentityService =
    new IdentityServiceLive(credentials, otps, sessions, customerLookup, otpDelivery, tokens, eventBus, config)
