package mpbuilder.domain.model

import zio.prelude.*

opaque type SessionId = String
object SessionId:
  def apply(value: String): Validation[String, SessionId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("SessionId must not be empty")

  def unsafe(value: String): SessionId = value

  extension (id: SessionId) def value: String = id

/** How the customer identifies themselves at login */
enum IdentifierType:
  case BusinessId, VatId, Email

  def displayName: LocalizedString = this match
    case BusinessId => LocalizedString("Business ID (IČO)", "IČO")
    case VatId      => LocalizedString("VAT ID (DIČ)", "DIČ")
    case Email      => LocalizedString("Email", "Email")

/** A request to generate an OTP for a customer */
final case class OtpRequest(
    customerId: CustomerId,
    identifier: String,
    identifierType: IdentifierType,
    requestedAt: Long,
)

/** A one-time password token issued to a customer */
final case class OtpToken(
    customerId: CustomerId,
    token: String,
    createdAt: Long,
    expiresAt: Long,
)

/** An active login session for an agency customer */
final case class LoginSession(
    sessionId: SessionId,
    customerId: CustomerId,
    createdAt: Long,
    expiresAt: Long,
)
