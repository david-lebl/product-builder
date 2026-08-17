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
  def inMemory(jwtSecret: String): ULayer[AuthService] =
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
      )
    }

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
