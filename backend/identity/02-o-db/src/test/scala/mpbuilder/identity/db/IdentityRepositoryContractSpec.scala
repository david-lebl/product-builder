package mpbuilder.identity
package db

import core.*
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import zio.*
import zio.test.*

import java.time.Instant

/** The SAME behavioural contract runs against the in-memory and the PostgreSQL adapters. */
object IdentityRepositoryContractSpec extends ZIOSpecDefault:

  type Repos = CredentialRepository & OtpRepository & SessionRepository

  private val now = Instant.parse("2026-01-10T10:00:00Z")
  private val employee = Principal.Employee(
    EmployeeId.unsafe("emp-9"), NonEmptyString.unsafe("Contract Tester"), EmailAddress.unsafe("tester@printshop.cz"),
    Set(EmployeeRole.Admin, EmployeeRole.Prepress),
  )
  private val customer = Principal.Customer(CustomerId.unsafe("cust-1"), NonEmptyString.unsafe("Jan"), EmailAddress.unsafe("jan@example.cz"))

  val inMemory: ULayer[Repos] = ZLayer.fromZIOEnvironment(
    for
      c <- Ref.make(Map.empty[EmailAddress, PasswordCredential])
      o <- Ref.make(Map.empty[OtpChallengeId, OtpChallenge])
      s <- Ref.make(Map.empty[SessionToken, StoredSession])
    yield ZEnvironment[CredentialRepository, OtpRepository, SessionRepository](
      new CredentialRepository:
        def findByEmail(e: EmailAddress)        = c.get.map(_.get(e))
        override def listAll                    = c.get.map(_.values.toList.sortBy(_.email.value))
        def save(x: PasswordCredential)         = c.update(_ + (x.email -> x))
      ,
      new OtpRepository:
        def findById(id: OtpChallengeId) = o.get.map(_.get(id))
        def save(x: OtpChallenge)        = o.update(_ + (x.id -> x))
      ,
      new SessionRepository:
        def findByToken(t: SessionToken) = s.get.map(_.get(t))
        def save(x: StoredSession)       = s.update(_ + (x.token -> x))
        def revoke(t: SessionToken)      = s.update(_.updatedWith(t)(_.map(_.copy(revoked = true))))
    )
  )

  val postgres: URLayer[Database, Repos] =
    ZLayer.fromZIO(ZIO.serviceWithZIO[Database](PostgresTestSupport.truncateSchemas(_, List("identity")))) >>> IdentityDb.repositories

  def contract[R](label: String, fresh: ZLayer[R, Nothing, Repos]) = suite(label)(
    test("credentials: save, find by email, update in place, list") {
      for
        repo  <- ZIO.service[CredentialRepository]
        cred   = PasswordCredential(employee.email, "hash", "salt", employee)
        _     <- repo.save(cred)
        _     <- repo.save(PasswordCredential(customer.email, "h2", "s2", customer))
        found <- repo.findByEmail(employee.email)
        _     <- repo.save(cred.copy(isActive = false, passwordHash = "hash-2"))
        upd   <- repo.findByEmail(employee.email)
        all   <- repo.listAll
        none  <- repo.findByEmail(EmailAddress.unsafe("nobody@example.cz"))
      yield assertTrue(
        found.contains(cred),
        upd.exists(c => !c.isActive && c.passwordHash == "hash-2"),
        all.map(_.email.value) == List("jan@example.cz", "tester@printshop.cz"),
        none.isEmpty,
      )
    },
    test("otp challenges: save, find, update attempts") {
      for
        repo <- ZIO.service[OtpRepository]
        ch    = OtpChallenge(OtpChallengeId.unsafe("otp-1"), customer, "codehash", "salt", "ja***@example.cz", now, now.plusSeconds(300))
        _    <- repo.save(ch)
        _    <- repo.save(ch.copy(attempts = 2))
        back <- repo.findById(ch.id)
      yield assertTrue(back.contains(ch.copy(attempts = 2)))
    },
    test("sessions: save, find, revoke") {
      for
        repo <- ZIO.service[SessionRepository]
        sess  = StoredSession(SessionToken.unsafe("tok-1"), employee, AuthMethod.Password, now, now.plusSeconds(3600))
        _    <- repo.save(sess)
        a    <- repo.findByToken(sess.token)
        _    <- repo.revoke(sess.token)
        b    <- repo.findByToken(sess.token)
        _    <- repo.revoke(SessionToken.unsafe("missing"))
      yield assertTrue(a.contains(sess), b.exists(_.revoked))
    },
  ).provideSomeLayer[R](fresh)

  def spec = suite("Identity repository contract")(
    contract("in-memory", inMemory),
    contract("postgres", postgres).provideShared(PostgresTestSupport.database(List(IdentityDb.migrations))),
  ) @@ TestAspect.sequential
