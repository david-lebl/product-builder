package mpbuilder.identity
package impl

import mpbuilder.commons.*
import zio.NonEmptyChunk
import zio.prelude.Validation

private[identity] final case class User(id: User.Id, data: User.Data)

private[identity] object User:

  opaque type Id = String
  object Id:
    def apply(raw: String): Id = raw
    extension (id: Id) def value: String = id

  final case class Data(
      email: Email,
      credentials: Credentials,
      status: Status,
      roles: Set[Role],
      customerId: Option[String],
      createdAt: Timestamp,
      lastLoginAt: Option[Timestamp],
  )

  /** How this user can prove who they are.
    *
    * A sum rather than a nullable password column: business customers who sign in by one-time code
    * have no password at all, and the model should say so instead of leaving a null that every
    * caller has to interpret. (Today's `CheckoutInfo.loginPassword` is the vestige of not doing this.)
    */
  enum Credentials:
    case Password(hash: PasswordHash)
    case OtpOnly

  /** Suspension carries its reason, so a suspended account cannot exist without one. */
  enum Status:
    case Active
    case PendingApproval
    case Suspended(reason: String)

/** A syntactically valid e-mail address. Parsed once, at the edge. */
private[identity] opaque type Email = String
private[identity] object Email:
  def parse(raw: String): Either[AuthError, Email] =
    val trimmed = raw.trim
    // Deliberately permissive: the only authoritative validation of an address is delivering to it.
    // This rejects obvious nonsense without refusing legitimate but unusual addresses.
    if trimmed.length >= 3 && trimmed.count(_ == '@') == 1 &&
      !trimmed.startsWith("@") && !trimmed.endsWith("@") && !trimmed.contains(' ')
    then Right(trimmed.toLowerCase)
    else Left(AuthError.InvalidEmail(raw))

  def unsafe(raw: String): Email = raw.toLowerCase

  extension (e: Email) def value: String = e

/** An opaque, self-describing password hash — algorithm and parameters travel with the digest, so
  * stored hashes stay verifiable after the cost parameters are raised.
  */
private[identity] opaque type PasswordHash = String
private[identity] object PasswordHash:
  def apply(encoded: String): PasswordHash = encoded
  extension (h: PasswordHash) def encoded: String = h

/** What counts as an acceptable password. Pure, so the rules are cheap to test and to change. */
private[identity] object PasswordPolicy:

  private val MinLength = 10

  def check(plain: String): Validation[Problem, Unit] =
    Validation.validateAll(
      List(
        rule(
          plain.length >= MinLength,
          "TooShort",
          s"Password must be at least $MinLength characters",
          s"Heslo musí mít alespoň $MinLength znaků",
        ),
        rule(
          plain.exists(_.isLetter),
          "NoLetter",
          "Password must contain a letter",
          "Heslo musí obsahovat písmeno",
        ),
        rule(
          plain.exists(_.isDigit),
          "NoDigit",
          "Password must contain a digit",
          "Heslo musí obsahovat číslici",
        ),
        rule(
          plain.trim == plain,
          "Padded",
          "Password must not start or end with a space",
          "Heslo nesmí začínat ani končit mezerou",
        ),
      )
    ).map(_ => ())

  private def rule(ok: Boolean, code: String, en: String, cs: String): Validation[Problem, Unit] =
    if ok then Validation.unit else Validation.fail(Problem(code, en, cs))
