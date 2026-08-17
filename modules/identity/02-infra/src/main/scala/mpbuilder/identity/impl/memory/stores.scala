package mpbuilder.identity
package impl
package memory

import mpbuilder.commons.*
import zio.*

import java.security.SecureRandom

/** Users held in a `Ref`.
  *
  * Real enough to run the whole sign-in flow end to end; replaced by a Postgres repository in
  * Phase 3, when there is finally something worth surviving a restart. Only this class changes.
  */
private[identity] final class InMemoryUserRepository(store: Ref[Map[String, User]])
    extends UserRepository:

  def findById(id: User.Id): UIO[Option[User]] =
    store.get.map(_.get(id.value))

  def findByEmail(email: Email): UIO[Option[User]] =
    store.get.map(_.values.find(_.data.email.value == email.value))

  def save(user: User): UIO[User] =
    store.update(_ + (user.id.value -> user)).as(user)

private[identity] object InMemoryUserRepository:
  val layer: ULayer[UserRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, User]).map(new InMemoryUserRepository(_)))

/** One-time codes, held in memory with their expiry.
  *
  * A code is single-use: `consume` removes the challenge whether or not the code matched, so a
  * six-digit code cannot be brute-forced by repeated attempts against the same challenge.
  */
private[identity] final class InMemoryOtpStore(
    challenges: Ref[Map[String, InMemoryOtpStore.Pending]],
    ids: Ids,
    ttl: Duration = 5.minutes,
) extends OtpStore:

  private val random = new SecureRandom()

  def issue(userId: User.Id, now: Timestamp): UIO[(OtpChallenge, String)] =
    for
      id <- ids.next
      code <- ZIO.succeed(f"${random.nextInt(1000000)}%06d")
      expiresAt = now.plusMillis(ttl.toMillis)
      _ <- challenges.update(_ + (id -> InMemoryOtpStore.Pending(userId, code, expiresAt)))
    yield (OtpChallenge(id, "", ttl.toSeconds), code)

  def consume(challengeId: String, code: String, now: Timestamp): IO[AuthError, User.Id] =
    challenges.modify { current =>
      current.get(challengeId) match
        case None => (Left(AuthError.OtpIncorrect), current)
        case Some(pending) =>
          // Removed on every attempt, correct or not — one challenge, one try.
          val remaining = current - challengeId
          if pending.expiresAt.epochMillis <= now.epochMillis then (Left(AuthError.OtpExpired), remaining)
          else if pending.code != code then (Left(AuthError.OtpIncorrect), remaining)
          else (Right(pending.userId), remaining)
    }.flatMap(ZIO.fromEither(_))

private[identity] object InMemoryOtpStore:
  final case class Pending(userId: User.Id, code: String, expiresAt: Timestamp)

  def layer(ids: Ids): ULayer[OtpStore] =
    ZLayer.fromZIO(Ref.make(Map.empty[String, Pending]).map(new InMemoryOtpStore(_, ids)))

private[identity] object RandomIds extends Ids:
  def next: UIO[String] = Random.nextUUID.map(_.toString)
