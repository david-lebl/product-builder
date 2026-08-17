package mpbuilder.identity
package impl

import mpbuilder.commons.*
import zio.*

/** Orchestration only — every rule it enforces lives in [[PasswordPolicy]], [[User.Status]] or a
  * port. If a business decision appears here, it is in the wrong place.
  */
private[identity] final class AuthServiceLive(
    users: UserRepository,
    hasher: PasswordHasher,
    tokens: TokenIssuer,
    otp: OtpStore,
    ids: Ids,
    now: UIO[Timestamp],
) extends AuthService:

  /** Must match the store's TTL; both are stated once, here and in the store's default. */
  private val OtpTtlSeconds = 300L

  def register(input: Register): IO[AuthError, AuthTokens] =
    for
      email <- ZIO.fromEither(Email.parse(input.email))
      _ <- ZIO.fromEither(
        PasswordPolicy.check(input.password).toEither.left.map(AuthError.WeakPassword(_))
      )
      existing <- users.findByEmail(email)
      _ <- ZIO.fail(AuthError.EmailAlreadyRegistered(email.value)).when(existing.isDefined)
      hash <- hasher.hash(input.password)
      id <- ids.next.map(User.Id(_))
      timestamp <- now
      user = User(
        id,
        User.Data(
          email = email,
          credentials = User.Credentials.Password(hash),
          status = User.Status.Active,
          roles = Set(Role.Customer),
          customerId = input.customerId,
          createdAt = timestamp,
          lastLoginAt = None,
        ),
      )
      saved <- users.save(user)
      issued <- tokens.issue(principalOf(saved), timestamp)
    yield issued

  def login(input: LoginWithPassword): IO[AuthError, AuthTokens] =
    for
      email <- ZIO.fromEither(Email.parse(input.email)).orElseFail(AuthError.InvalidCredentials)
      found <- users.findByEmail(email)
      user <- verifyPassword(found, input.password)
      _ <- assertSignInAllowed(user)
      timestamp <- now
      _ <- users.save(user.copy(data = user.data.copy(lastLoginAt = Some(timestamp))))
      issued <- tokens.issue(principalOf(user), timestamp)
    yield issued

  def refresh(refreshToken: String): IO[AuthError, AuthTokens] =
    for
      timestamp <- now
      userId <- tokens.verifyRefresh(refreshToken, timestamp)
      // Roles are re-read rather than trusted from the token, so a revoked role takes effect at
      // the next refresh instead of lingering for the token's whole lifetime.
      user <- users.findById(userId).someOrFail(AuthError.TokenInvalid)
      _ <- assertSignInAllowed(user)
      issued <- tokens.issue(principalOf(user), timestamp)
    yield issued

  def verify(accessToken: String): IO[AuthError, Principal] =
    now.flatMap(tokens.verifyAccess(accessToken, _))

  def me(accessToken: String): IO[AuthError, UserView] =
    for
      principal <- verify(accessToken)
      user <- users.findById(User.Id(principal.userId)).someOrFail(AuthError.TokenInvalid)
    yield UserView(
      id = user.id.value,
      email = user.data.email.value,
      roles = user.data.roles.map(_.name),
      customerId = user.data.customerId,
      status = statusName(user.data.status),
    )

  def requestOtp(email: String): IO[AuthError, OtpChallenge] =
    for
      parsed <- ZIO.fromEither(Email.parse(email))
      found <- users.findByEmail(parsed)
      timestamp <- now
      challengeId <- found match
        case Some(user) => otp.issue(user.id, timestamp).map(_._1.challengeId)
        // No account: still return a challenge, so the response is indistinguishable from the
        // success case and the endpoint cannot be used to enumerate registered addresses. The
        // challenge simply never verifies.
        case None => ids.next
      // Built here, identically for both branches. Letting the store supply `deliveredTo` once
      // made the two responses differ — an empty value meant "this account exists", which is the
      // precise leak this branch-free construction exists to prevent.
    yield OtpChallenge(challengeId, mask(parsed.value), OtpTtlSeconds)

  def verifyOtp(challengeId: String, code: String): IO[AuthError, AuthTokens] =
    for
      timestamp <- now
      userId <- otp.consume(challengeId, code, timestamp)
      user <- users.findById(userId).someOrFail(AuthError.OtpIncorrect)
      _ <- assertSignInAllowed(user)
      _ <- users.save(user.copy(data = user.data.copy(lastLoginAt = Some(timestamp))))
      issued <- tokens.issue(principalOf(user), timestamp)
    yield issued

  // ── internals ────────────────────────────────────────────────────────────

  /** Runs the hash comparison even when no account exists, so a missing account and a wrong
    * password take the same time. Skipping the work would leak account existence through timing.
    */
  private def verifyPassword(found: Option[User], plain: String): IO[AuthError, User] =
    found.map(_.data.credentials) match
      case Some(User.Credentials.Password(hash)) =>
        hasher.verify(plain, hash).flatMap {
          case true  => ZIO.succeed(found.get)
          case false => ZIO.fail(AuthError.InvalidCredentials)
        }
      case Some(User.Credentials.OtpOnly) =>
        hasher.verify(plain, decoyHash) *> ZIO.fail(AuthError.InvalidCredentials)
      case None =>
        hasher.verify(plain, decoyHash) *> ZIO.fail(AuthError.InvalidCredentials)

  private val decoyHash: PasswordHash = PasswordHash(
    "argon2id$3$65536$1$ZGVjb3lzYWx0ZGVjb3lzYQ==$ZGVjb3loYXNoZGVjb3loYXNoZGVjb3loYXNoZGVjb3k="
  )

  private def assertSignInAllowed(user: User): IO[AuthError, Unit] = user.data.status match
    case User.Status.Active            => ZIO.unit
    case User.Status.PendingApproval   => ZIO.fail(AuthError.AccountNotApproved)
    case User.Status.Suspended(reason) => ZIO.fail(AuthError.AccountSuspended(reason))

  private def principalOf(user: User): Principal =
    Principal(
      userId = user.id.value,
      email = user.data.email.value,
      roles = user.data.roles,
      customerId = user.data.customerId,
    )

  private def statusName(status: User.Status): String = status match
    case User.Status.Active          => "Active"
    case User.Status.PendingApproval => "PendingApproval"
    case User.Status.Suspended(_)    => "Suspended"

  private def mask(email: String): String =
    email.split("@") match
      case Array(local, domain) if local.length > 2 => s"${local.take(2)}***@$domain"
      case Array(_, domain)                         => s"***@$domain"
      case _                                        => "***"
