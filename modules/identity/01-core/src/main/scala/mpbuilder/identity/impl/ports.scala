package mpbuilder.identity
package impl

import mpbuilder.commons.*
import zio.{IO, UIO}

/** Where users are kept. Implemented in memory today, in Postgres from Phase 3. */
private[identity] trait UserRepository:
  def findById(id: User.Id): UIO[Option[User]]
  def findByEmail(email: Email): UIO[Option[User]]
  def save(user: User): UIO[User]

/** Password hashing. A port because the algorithm and its cost parameters must be replaceable
  * without touching the sign-in logic — and because a fast fake keeps tests from paying Argon2's
  * deliberate cost on every case.
  */
private[identity] trait PasswordHasher:
  def hash(plain: String): UIO[PasswordHash]
  def verify(plain: String, against: PasswordHash): UIO[Boolean]

private[identity] trait TokenIssuer:
  def issue(principal: Principal, now: Timestamp): UIO[AuthTokens]
  def verifyAccess(token: String, now: Timestamp): IO[AuthError, Principal]

  /** Refresh tokens carry only a subject: roles are re-read from storage on every refresh, so a
    * revoked role cannot survive in a long-lived token.
    */
  def verifyRefresh(token: String, now: Timestamp): IO[AuthError, User.Id]

private[identity] trait OtpStore:
  def issue(userId: User.Id, now: Timestamp): UIO[(OtpChallenge, String)]
  def consume(challengeId: String, code: String, now: Timestamp): IO[AuthError, User.Id]

/** Identifier generation, injected so tests are deterministic. */
private[identity] trait Ids:
  def next: UIO[String]
