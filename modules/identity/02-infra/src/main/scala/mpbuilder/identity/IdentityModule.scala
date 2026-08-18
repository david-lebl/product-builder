package mpbuilder.identity

import mpbuilder.commons.*
import mpbuilder.identity.impl.*
import mpbuilder.identity.impl.crypto.Argon2PasswordHasher
import mpbuilder.identity.impl.jwt.JwtTokenIssuer
import mpbuilder.identity.impl.memory.{InMemoryOtpStore, InMemoryUserRepository, RandomIds}
import zio.*

/** How the identity context is assembled.
  *
  * The one public entry point in `02-infra`: the composition root wires this and never sees a
  * repository, a hasher or a token issuer. Which adapters are used is this file's business alone.
  */
object IdentityModule:

  /** In-memory adapters — everything works end to end, nothing survives a restart. */
  /** @param staffEmails
    *   addresses that receive staff roles when they register. Configuration, not self-service:
    *   the shop's own people are provisioned by whoever runs the shop, and a registration form
    *   that let a caller name their own roles would let anyone become an administrator.
    */
  def inMemory(jwtSecret: String, staffEmails: Set[String] = Set.empty): ULayer[AuthService] =
    ZLayer {
      for
        users <- Ref.make(Map.empty[String, User]).map(new InMemoryUserRepository(_))
        otpRef <- Ref.make(Map.empty[String, InMemoryOtpStore.Pending])
        otp = new InMemoryOtpStore(otpRef, RandomIds)
      yield AuthServiceLive(
        users = users,
        hasher = new Argon2PasswordHasher(),
        tokens = new JwtTokenIssuer(jwtSecret),
        otp = otp,
        ids = RandomIds,
        now = Clock.instant.map(i => Timestamp(i.toEpochMilli)),
        rolesFor = rolesFor(staffEmails),
      )
    }

  /** Staff get the operator role on top of the customer one: they buy things too, and taking that
    * away would make a member of staff unable to use the shop they work in.
    */
  private def rolesFor(staffEmails: Set[String])(email: Email): Set[Role] =
    val normalized = staffEmails.map(_.trim.toLowerCase).filter(_.nonEmpty)
    if normalized.contains(email.value.toLowerCase) then Set(Role.Customer, Role.Operator)
    else Set(Role.Customer)

  /** Same wiring, but with cheap hashing — Argon2 is deliberately slow, and a test suite that pays
    * 64 MiB and three passes per case for dozens of cases is a test suite people stop running.
    */
  def forTests(jwtSecret: String = "test-secret-not-for-production"): ULayer[AuthService] =
    ZLayer {
      for
        users <- Ref.make(Map.empty[String, User]).map(new InMemoryUserRepository(_))
        otpRef <- Ref.make(Map.empty[String, InMemoryOtpStore.Pending])
        otp = new InMemoryOtpStore(otpRef, RandomIds)
      yield AuthServiceLive(
        users = users,
        hasher = new Argon2PasswordHasher(iterations = 1, memoryKb = 1024, parallelism = 1),
        tokens = new JwtTokenIssuer(jwtSecret),
        otp = otp,
        ids = RandomIds,
        now = Clock.instant.map(i => Timestamp(i.toEpochMilli)),
      )
    }
