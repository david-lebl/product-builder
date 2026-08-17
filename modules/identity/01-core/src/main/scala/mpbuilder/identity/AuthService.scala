package mpbuilder.identity

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, ZIO}

// ── Requests ──────────────────────────────────────────────────────────────

final case class Register(
    email: String,
    password: String,
    customerId: Option[String] = None,
)

final case class LoginWithPassword(email: String, password: String)

final case class UserView(
    id: String,
    email: String,
    roles: Set[String],
    customerId: Option[String],
    status: String,
)

// ── Errors ────────────────────────────────────────────────────────────────

enum AuthError extends DomainError:

  /** Deliberately does not distinguish "no such account" from "wrong password".
    *
    * Telling them apart turns the sign-in form into an account-enumeration oracle: an attacker
    * could discover which e-mail addresses have accounts here without ever guessing a password.
    */
  case InvalidCredentials

  case AccountSuspended(reason: String)

  /** A corporate account still awaiting staff approval. Reported distinctly only *after*
    * credentials have been verified, so it leaks nothing to an anonymous caller.
    */
  case AccountNotApproved

  case EmailAlreadyRegistered(email: String)
  case InvalidEmail(value: String)
  case WeakPassword(problems: NonEmptyChunk[Problem])
  case TokenExpired
  case TokenInvalid
  case OtpExpired
  case OtpIncorrect
  case NotAuthorised

  def message(lang: Language): String = this match
    case InvalidCredentials =>
      lang match
        case Language.En => "E-mail or password is incorrect"
        case Language.Cs => "Nesprávný e-mail nebo heslo"
    case AccountSuspended(reason) =>
      lang match
        case Language.En => s"This account is suspended: $reason"
        case Language.Cs => s"Tento účet je pozastaven: $reason"
    case AccountNotApproved =>
      lang match
        case Language.En => "This account is awaiting approval"
        case Language.Cs => "Tento účet čeká na schválení"
    case EmailAlreadyRegistered(email) =>
      lang match
        case Language.En => s"An account already exists for $email"
        case Language.Cs => s"Účet pro $email již existuje"
    case InvalidEmail(value) =>
      lang match
        case Language.En => s"'$value' is not a valid e-mail address"
        case Language.Cs => s"'$value' není platná e-mailová adresa"
    case WeakPassword(problems) => problems.map(_.message(lang)).mkString("; ")
    case TokenExpired =>
      lang match
        case Language.En => "Your session has expired — please sign in again"
        case Language.Cs => "Vaše přihlášení vypršelo — přihlaste se prosím znovu"
    case TokenInvalid =>
      lang match
        case Language.En => "Invalid session token"
        case Language.Cs => "Neplatný token relace"
    case OtpExpired =>
      lang match
        case Language.En => "That code has expired — request a new one"
        case Language.Cs => "Kód vypršel — vyžádejte si nový"
    case OtpIncorrect =>
      lang match
        case Language.En => "That code is not correct"
        case Language.Cs => "Kód není správný"
    case NotAuthorised =>
      lang match
        case Language.En => "You are not allowed to do that"
        case Language.Cs => "K této akci nemáte oprávnění"

// ── Contract ──────────────────────────────────────────────────────────────

/** The identity context's public contract: who is signed in.
  *
  * Owns credentials and roles only. A customer's *business* profile — company data, addresses,
  * negotiated pricing — belongs to `customers`, linked by `customerId`. Keeping them apart is what
  * lets a customer exist without a login (a guest order) and a login exist without a customer
  * (staff).
  */
trait AuthService:

  def register(input: Register): IO[AuthError, AuthTokens]

  def login(input: LoginWithPassword): IO[AuthError, AuthTokens]

  /** Exchange a refresh token for a fresh pair. */
  def refresh(refreshToken: String): IO[AuthError, AuthTokens]

  /** Verify an access token. Every other context's authorisation starts here. */
  def verify(accessToken: String): IO[AuthError, Principal]

  def me(accessToken: String): IO[AuthError, UserView]

  /** Begin a one-time-code sign-in — the method business customers use instead of a password.
    *
    * Always succeeds for a well-formed identifier, whether or not an account exists, so the
    * endpoint cannot be used to discover which addresses are registered.
    */
  def requestOtp(email: String): IO[AuthError, OtpChallenge]

  def verifyOtp(challengeId: String, code: String): IO[AuthError, AuthTokens]

object AuthService:
  def register(input: Register): ZIO[AuthService, AuthError, AuthTokens] =
    ZIO.serviceWithZIO(_.register(input))

  def login(input: LoginWithPassword): ZIO[AuthService, AuthError, AuthTokens] =
    ZIO.serviceWithZIO(_.login(input))

  def refresh(token: String): ZIO[AuthService, AuthError, AuthTokens] =
    ZIO.serviceWithZIO(_.refresh(token))

  def verify(token: String): ZIO[AuthService, AuthError, Principal] =
    ZIO.serviceWithZIO(_.verify(token))

  def me(token: String): ZIO[AuthService, AuthError, UserView] =
    ZIO.serviceWithZIO(_.me(token))

  def requestOtp(email: String): ZIO[AuthService, AuthError, OtpChallenge] =
    ZIO.serviceWithZIO(_.requestOtp(email))

  def verifyOtp(challengeId: String, code: String): ZIO[AuthService, AuthError, AuthTokens] =
    ZIO.serviceWithZIO(_.verifyOtp(challengeId, code))
