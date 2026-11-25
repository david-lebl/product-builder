package mpbuilder.identity
package db

import core.*
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.shared.*
import kyo.*
import kyo.test.*

import java.time.Instant

/** The SAME behavioural contract runs against the in-memory and the PostgreSQL adapters. */
class IdentityRepositoryContractSpec extends Test[Any]:

  private type Fresh = IdentityRepositories < (Async & Scope & Abort[Throwable])

  private val now = Instant.parse("2026-01-10T10:00:00Z")
  private val employee = Principal.Employee(
    EmployeeId.unsafe("emp-9"), NonEmptyString.unsafe("Contract Tester"), EmailAddress.unsafe("tester@printshop.cz"),
    Set(EmployeeRole.Admin, EmployeeRole.Prepress),
  )
  private val customer = Principal.Customer(CustomerId.unsafe("cust-1"), NonEmptyString.unsafe("Jan"), EmailAddress.unsafe("jan@example.cz"))

  private def inMemory: Fresh =
    for
      c <- AtomicRef.init(Map.empty[EmailAddress, PasswordCredential])
      o <- AtomicRef.init(Map.empty[OtpChallengeId, OtpChallenge])
      s <- AtomicRef.init(Map.empty[SessionToken, StoredSession])
    yield IdentityRepositories(
      new CredentialRepository:
        def findByEmail(e: EmailAddress): Option[PasswordCredential] < Async = c.get.map(_.get(e))
        override def listAll: List[PasswordCredential] < Async               = c.get.map(_.values.toList.sortBy(_.email.value))
        def save(x: PasswordCredential): Unit < Async                        = c.update(_ + (x.email -> x))
      ,
      new OtpRepository:
        def findById(id: OtpChallengeId): Option[OtpChallenge] < Async = o.get.map(_.get(id))
        def save(x: OtpChallenge): Unit < Async                        = o.update(_ + (x.id -> x))
      ,
      new SessionRepository:
        def findByToken(t: SessionToken): Option[StoredSession] < Async = s.get.map(_.get(t))
        def save(x: StoredSession): Unit < Async                        = s.update(_ + (x.token -> x))
        def revoke(t: SessionToken): Unit < Async                       = s.update(_.updatedWith(t)(_.map(_.copy(revoked = true))))
    )

  private def postgres: Fresh = PostgresTestSupport.database(List(IdentityDb.migrations)).map(IdentityDb.repositories)

  private def contract(label: String, fresh: => Fresh): Unit =
    label - {
      "credentials: save, find by email, update in place, list" in {
        for
          repo  <- fresh.map(_.credentials)
          cred   = PasswordCredential(employee.email, "hash", "salt", employee)
          _     <- repo.save(cred)
          _     <- repo.save(PasswordCredential(customer.email, "h2", "s2", customer))
          found <- repo.findByEmail(employee.email)
          _     <- repo.save(cred.copy(isActive = false, passwordHash = "hash-2"))
          upd   <- repo.findByEmail(employee.email)
          all   <- repo.listAll
          none  <- repo.findByEmail(EmailAddress.unsafe("nobody@example.cz"))
        yield
          assert(found.contains(cred))
          assert(upd.exists(c => !c.isActive && c.passwordHash == "hash-2"))
          assert(all.map(_.email.value) == List("jan@example.cz", "tester@printshop.cz"))
          assert(none.isEmpty)
      }

      "otp challenges: save, find, update attempts" in {
        for
          repo <- fresh.map(_.otps)
          ch    = OtpChallenge(OtpChallengeId.unsafe("otp-1"), customer, "codehash", "salt", "ja***@example.cz", now, now.plusSeconds(300))
          _    <- repo.save(ch)
          _    <- repo.save(ch.copy(attempts = 2))
          back <- repo.findById(ch.id)
        yield assert(back.contains(ch.copy(attempts = 2)))
      }

      "sessions: save, find, revoke" in {
        for
          repo <- fresh.map(_.sessions)
          sess  = StoredSession(SessionToken.unsafe("tok-1"), employee, AuthMethod.Password, now, now.plusSeconds(3600))
          _    <- repo.save(sess)
          a    <- repo.findByToken(sess.token)
          _    <- repo.revoke(sess.token)
          b    <- repo.findByToken(sess.token)
          _    <- repo.revoke(SessionToken.unsafe("missing"))
        yield
          assert(a.contains(sess))
          assert(b.exists(_.revoked))
      }
    }

  "Identity repository contract" - {
    contract("in-memory", inMemory)
    contract("postgres", postgres)
  }
