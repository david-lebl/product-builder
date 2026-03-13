package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*

/** Pure service for OTP-based agency login. */
object LoginService:

  /** Default OTP validity period: 5 minutes in milliseconds. */
  val otpValidityMillis: Long = 5L * 60L * 1000L

  /** Default session validity period: 24 hours in milliseconds. */
  val sessionValidityMillis: Long = 24L * 60L * 60L * 1000L

  /**
   * Look up a customer by identifier (business ID, VAT ID, or email).
   * Only Active customers can log in — Inactive and Suspended are rejected.
   * PendingApproval customers are also rejected (treated as not found).
   */
  def lookupCustomer(
      identifier: String,
      identifierType: IdentifierType,
      customers: List[Customer],
  ): Validation[LoginError, Customer] =
    val trimmed = identifier.trim
    val found = identifierType match
      case IdentifierType.BusinessId =>
        customers.find(c => c.companyInfo.exists(_.businessId == trimmed))
      case IdentifierType.VatId =>
        customers.find(c => c.companyInfo.exists(_.vatId.contains(trimmed)))
      case IdentifierType.Email =>
        customers.find(c => c.contactInfo.email.equalsIgnoreCase(trimmed))

    found match
      case None =>
        Validation.fail(LoginError.CustomerNotFound(trimmed, identifierType))
      case Some(c) => c.status match
        case CustomerStatus.Active =>
          Validation.succeed(c)
        case CustomerStatus.Inactive =>
          Validation.fail(LoginError.CustomerInactive(c.id))
        case CustomerStatus.Suspended =>
          Validation.fail(LoginError.CustomerSuspended(c.id))
        case CustomerStatus.PendingApproval =>
          Validation.fail(LoginError.CustomerNotFound(trimmed, identifierType))

  /**
   * Generate an OTP token for a customer.
   * Pure — generates a deterministic token from customer ID + timestamp for testability.
   * Real randomness should be injected at the UI layer.
   */
  def generateOtp(customer: Customer, now: Long): OtpToken =
    val hash = (customer.id.value + now.toString).hashCode.abs
    val token = f"${hash % 1000000}%06d"
    OtpToken(
      customerId = customer.id,
      token = token,
      createdAt = now,
      expiresAt = now + otpValidityMillis,
    )

  /**
   * Validate an OTP token input against the issued token.
   * Returns a LoginSession on success.
   */
  def validateOtp(
      inputToken: String,
      otpToken: OtpToken,
      now: Long,
  ): Validation[LoginError, LoginSession] =
    if now > otpToken.expiresAt then
      Validation.fail(LoginError.OtpExpired(otpToken.customerId))
    else if inputToken.trim != otpToken.token then
      Validation.fail(LoginError.OtpInvalid(otpToken.customerId))
    else
      val sessionHash = (otpToken.customerId.value + now.toString).hashCode.abs
      val sessionId = SessionId.unsafe(s"session-$sessionHash")
      Validation.succeed(LoginSession(
        sessionId = sessionId,
        customerId = otpToken.customerId,
        createdAt = now,
        expiresAt = now + sessionValidityMillis,
      ))

  /** Check whether a session is still valid at the given time. */
  def isSessionValid(session: LoginSession, now: Long): Boolean =
    now <= session.expiresAt
