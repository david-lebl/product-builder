package mpbuilder.identity
package wiring

import core.*
import db.IdentityDb
import mpbuilder.customers.CustomerService
import mpbuilder.identity.sample.{CustomerServiceLookupAdapter, SampleIdentity}
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import kyo.*

import java.security.SecureRandom
import java.util.{Base64, UUID}

/** Cryptographically secure token generator for real deployments (sample runtime uses sequential demo tokens). */
private[identity] final class SecureTokenGenerator extends TokenGenerator:
  private val random = new SecureRandom()

  private def randomBytes(n: Int): Array[Byte] =
    val bytes = new Array[Byte](n)
    random.nextBytes(bytes)
    bytes

  override def generateSessionToken: SessionToken < Async =
    Sync.defer(SessionToken.unsafe(Base64.getUrlEncoder.withoutPadding.encodeToString(randomBytes(32))))
  override def generateOtpChallengeId: OtpChallengeId < Async =
    Sync.defer(OtpChallengeId.unsafe(s"otp-${UUID.randomUUID()}"))
  override def generateOtpCode: OtpCode < Async =
    Sync.defer(OtpCode.unsafe(f"${random.nextInt(900000) + 100000}%06d"))
  override def generateSalt: String < Async =
    Sync.defer(randomBytes(16).map(b => f"${b & 0xff}%02x").mkString)

/** Until transactional e-mail exists (Phase 6), one-time codes are written to the server log. */
private[identity] final class LoggingOtpDelivery extends OtpDeliveryPort:
  override def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): Unit < Async =
    Log.warn(s"[DEV OTP DELIVERY] challenge=${challengeId.value} to=${principal.email.value} code=${code.value}")

object IdentityWiring:

  /** `IdentityService` on PostgreSQL, bridged to `CustomerService` for IČO / DIČ / e-mail OTP lookups. */
  def postgres(db: Database, customers: CustomerService, eventBus: EventBus): IdentityService =
    val repos = IdentityDb.repositories(db)
    IdentityCoreLayers.make(
      repos.credentials,
      repos.otps,
      repos.sessions,
      new CustomerServiceLookupAdapter(customers),
      new LoggingOtpDelivery,
      new SecureTokenGenerator,
      eventBus,
    )

  /** Registers demo customer & staff password credentials (idempotent). Returns how many were new. */
  def seedDemoCredentials(svc: IdentityService): Int < Async =
    val customers = SampleIdentity.demoCustomerLogins.map { case (cid, name, email) =>
      RegisterPasswordCmd(Principal.Customer(cid, NonEmptyString.unsafe(name), email), email, SampleIdentity.demoCustomerPassword)
    }
    val staff = SampleIdentity.demoStaff.map(emp => RegisterPasswordCmd(emp, emp.email, SampleIdentity.demoEmployeePassword))
    Kyo.foldLeft(customers ++ staff)(0)((n, cmd) => svc.registerPasswordCredential(cmd).either.map(r => if r.isRight then n + 1 else n))
