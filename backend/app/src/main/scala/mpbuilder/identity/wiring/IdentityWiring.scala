package mpbuilder.identity
package wiring

import core.*
import db.IdentityDb
import mpbuilder.customers.CustomerService
import mpbuilder.identity.sample.{CustomerServiceLookupAdapter, SampleIdentity}
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import zio.*

import java.security.SecureRandom
import java.util.{Base64, UUID}

/** Cryptographically secure token generator for real deployments (sample runtime uses sequential demo tokens). */
private[identity] final class SecureTokenGenerator extends TokenGenerator:
  private val random = new SecureRandom()

  private def randomBytes(n: Int): Array[Byte] =
    val bytes = new Array[Byte](n)
    random.nextBytes(bytes)
    bytes

  override def generateSessionToken: UIO[SessionToken] =
    ZIO.succeed(SessionToken.unsafe(Base64.getUrlEncoder.withoutPadding.encodeToString(randomBytes(32))))
  override def generateOtpChallengeId: UIO[OtpChallengeId] =
    ZIO.succeed(OtpChallengeId.unsafe(s"otp-${UUID.randomUUID()}"))
  override def generateOtpCode: UIO[OtpCode] =
    ZIO.succeed(OtpCode.unsafe(f"${random.nextInt(900000) + 100000}%06d"))
  override def generateSalt: UIO[String] =
    ZIO.succeed(randomBytes(16).map(b => f"${b & 0xff}%02x").mkString)

/** Until transactional e-mail exists (Phase 6), one-time codes are written to the server log. */
private[identity] final class LoggingOtpDelivery extends OtpDeliveryPort:
  override def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): UIO[Unit] =
    ZIO.logWarning(s"[DEV OTP DELIVERY] challenge=${challengeId.value} to=${principal.email.value} code=${code.value}")

object IdentityWiring:

  /** `IdentityService` on PostgreSQL, bridged to `CustomerService` for IČO / DIČ / e-mail OTP lookups. */
  val postgres: URLayer[Database & CustomerService & EventBus, IdentityService] =
    val lookup: URLayer[CustomerService, CustomerAccountLookupPort] =
      ZLayer.fromFunction((cs: CustomerService) => new CustomerServiceLookupAdapter(cs))
    val delivery: ULayer[OtpDeliveryPort] = ZLayer.succeed(new LoggingOtpDelivery)
    val tokens: ULayer[TokenGenerator]    = ZLayer.succeed(new SecureTokenGenerator)
    (IdentityDb.repositories ++ lookup ++ delivery ++ tokens ++ ZLayer.service[EventBus]) >>> IdentityCoreLayers.layer()

  /** Registers demo customer & staff password credentials (idempotent). */
  val seedDemoCredentials: URIO[IdentityService, Int] =
    ZIO.serviceWithZIO[IdentityService] { svc =>
      val customers = SampleIdentity.demoCustomerLogins.map { case (cid, name, email) =>
        RegisterPasswordCmd(Principal.Customer(cid, NonEmptyString.unsafe(name), email), email, SampleIdentity.demoCustomerPassword)
      }
      val staff = SampleIdentity.demoStaff.map(emp => RegisterPasswordCmd(emp, emp.email, SampleIdentity.demoEmployeePassword))
      ZIO.foldLeft(customers ++ staff)(0)((n, cmd) => svc.registerPasswordCredential(cmd).as(n + 1).orElseSucceed(n))
    }
