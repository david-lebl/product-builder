package mpbuilder.identity

import mpbuilder.shared.*
import zio.*
import zio.prelude.Validation
import java.time.Instant

// ── Context-local Opaque Types ───────────────────────────────────────────────

opaque type SessionToken = String
object SessionToken:
  def apply(raw: String): Validation[String, SessionToken] =
    val t = raw.trim
    if t.nonEmpty then Validation.succeed(t)
    else Validation.fail("SessionToken must not be empty")
  def unsafe(raw: String): SessionToken = raw
  extension (t: SessionToken) def value: String = t

opaque type OtpChallengeId = String
object OtpChallengeId:
  def apply(raw: String): Validation[String, OtpChallengeId] =
    val t = raw.trim
    if t.nonEmpty then Validation.succeed(t)
    else Validation.fail("OtpChallengeId must not be empty")
  def unsafe(raw: String): OtpChallengeId = raw
  extension (id: OtpChallengeId) def value: String = id

opaque type OtpCode = String
object OtpCode:
  private val Digits6 = "^\\d{6}$".r
  def apply(raw: String): Validation[String, OtpCode] =
    val t = raw.trim
    if Digits6.matches(t) then Validation.succeed(t)
    else Validation.fail(s"OtpCode must be exactly 6 digits, got '$raw'")
  def unsafe(raw: String): OtpCode = raw
  extension (c: OtpCode) def value: String = c

// ── Principals & Authentication Models ───────────────────────────────────────

enum EmployeeRole:
  case Operator, Prepress, Manager, Admin

/** Authenticated actor — designed from day one to support both customers and employees. */
enum Principal:
  case Customer(customerId: CustomerId, displayName: NonEmptyString, email: EmailAddress)
  case Employee(employeeId: EmployeeId, displayName: NonEmptyString, email: EmailAddress, roles: Set[EmployeeRole])

  def email: EmailAddress
  def displayName: NonEmptyString

  def customerIdOpt: Option[CustomerId] = this match
    case Customer(id, _, _) => Some(id)
    case _                  => None

  def employeeIdOpt: Option[EmployeeId] = this match
    case Employee(id, _, _, _) => Some(id)
    case _                     => None

/** Identifiers accepted for passwordless OTP login (matching FE Email / IČO / DIČ flow). */
enum LoginIdentifier:
  case Email(email: EmailAddress)
  case CompanyRegNo(registrationNumber: NonEmptyString)
  case VatId(vatId: NonEmptyString)

enum AuthMethod:
  case Password, Otp

final case class AuthSession(
    token: SessionToken,
    principal: Principal,
    method: AuthMethod,
    issuedAt: Instant,
    expiresAt: Instant,
)

final case class OtpChallengeView(
    challengeId: OtpChallengeId,
    principal: Principal,
    maskedDestination: String,
    expiresAt: Instant,
)

final case class RegisterPasswordCmd(
    principal: Principal,
    email: EmailAddress,
    rawPassword: NonEmptyString,
)

// ── Errors ───────────────────────────────────────────────────────────────────

enum IdentityError(val message: String):
  case InvalidCredentials
      extends IdentityError("Invalid email or password")
  case AccountNotActive(reason: String)
      extends IdentityError(s"Account is not active: $reason")
  case AccountNotFound(identifier: LoginIdentifier)
      extends IdentityError(s"No account found for identifier $identifier")
  case CredentialAlreadyExists(email: EmailAddress)
      extends IdentityError(s"Credentials already exist for '${email.value}'")
  case OtpChallengeNotFound(challengeId: OtpChallengeId)
      extends IdentityError(s"OTP challenge '${challengeId.value}' not found")
  case OtpExpired(challengeId: OtpChallengeId)
      extends IdentityError(s"OTP challenge '${challengeId.value}' has expired")
  case OtpAlreadyUsed(challengeId: OtpChallengeId)
      extends IdentityError(s"OTP challenge '${challengeId.value}' has already been used")
  case OtpInvalid(remainingAttempts: Int)
      extends IdentityError(s"Invalid OTP code ($remainingAttempts attempts remaining)")
  case OtpTooManyAttempts(challengeId: OtpChallengeId)
      extends IdentityError(s"Too many failed OTP attempts for challenge '${challengeId.value}'")
  case SessionNotFound
      extends IdentityError("Session not found or has been revoked")
  case SessionExpired
      extends IdentityError("Session has expired")

// ── Domain Events ────────────────────────────────────────────────────────────

final case class OtpRequested(
    challengeId: OtpChallengeId,
    principal: Principal,
    occurredAt: Instant,
) extends DomainEvent

final case class UserLoggedIn(
    principal: Principal,
    method: AuthMethod,
    occurredAt: Instant,
) extends DomainEvent

final case class UserLoggedOut(
    principal: Principal,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Port (Module API) ─────────────────────────────────────────

trait IdentityService:
  def registerPasswordCredential(cmd: RegisterPasswordCmd): IO[IdentityError, Unit]
  def loginWithPassword(email: EmailAddress, rawPassword: NonEmptyString): IO[IdentityError, AuthSession]
  def requestCustomerOtp(identifier: LoginIdentifier): IO[IdentityError, OtpChallengeView]
  def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): IO[IdentityError, AuthSession]
  def validateSession(token: SessionToken): IO[IdentityError, AuthSession]
  def logout(token: SessionToken): UIO[Unit]

object IdentityService:
  def loginWithPassword(email: EmailAddress, rawPassword: NonEmptyString): ZIO[IdentityService, IdentityError, AuthSession] =
    ZIO.serviceWithZIO[IdentityService](_.loginWithPassword(email, rawPassword))

  def requestCustomerOtp(identifier: LoginIdentifier): ZIO[IdentityService, IdentityError, OtpChallengeView] =
    ZIO.serviceWithZIO[IdentityService](_.requestCustomerOtp(identifier))

  def verifyOtp(challengeId: OtpChallengeId, code: OtpCode): ZIO[IdentityService, IdentityError, AuthSession] =
    ZIO.serviceWithZIO[IdentityService](_.verifyOtp(challengeId, code))

  def validateSession(token: SessionToken): ZIO[IdentityService, IdentityError, AuthSession] =
    ZIO.serviceWithZIO[IdentityService](_.validateSession(token))
