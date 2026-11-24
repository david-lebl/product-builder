package mpbuilder.identity

import mpbuilder.identity.core.*
import mpbuilder.shared.*
import kyo.*
import kyo.test.*
import java.time.Instant

class IdentityDomainSpec extends Test[Any]:

  private val customerPrincipal = Principal.Customer(
    customerId = CustomerId.unsafe("cust-1"),
    displayName = NonEmptyString.unsafe("Jan Novák"),
    email = EmailAddress.unsafe("jan@printshoppro.cz"),
  )

  private val employeePrincipal = Principal.Employee(
    employeeId = EmployeeId.unsafe("emp-1"),
    displayName = NonEmptyString.unsafe("Admin Operator"),
    email = EmailAddress.unsafe("admin@printshop.cz"),
    roles = Set(EmployeeRole.Admin, EmployeeRole.Operator),
  )

  private val suspendedPrincipal = Principal.Customer(
    customerId = CustomerId.unsafe("cust-susp"),
    displayName = NonEmptyString.unsafe("Karel Malý"),
    email = EmailAddress.unsafe("karel@suspendedco.cz"),
  )

  private final class RecordingOtpDelivery(ref: AtomicRef[Map[OtpChallengeId, OtpCode]]) extends OtpDeliveryPort:
    override def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): Unit < Async =
      ref.update(_ + (challengeId -> code))
    def codeFor(challengeId: OtpChallengeId): Option[OtpCode] < Async =
      ref.get.map(_.get(challengeId))

  private final case class Fixture(svc: IdentityService, bus: EventBus, delivery: RecordingOtpDelivery)

  private val t0 = java.time.Instant.parse("2026-04-01T10:00:00Z")

  /** Runs `f` against a fresh in-memory identity service with a controlled clock starting at `t0`. */
  private def withFixture[A, S](
      config: IdentityConfig = IdentityConfig(otpTtl = 5.minutes, otpMaxAttempts = 3, sessionTtl = 1.hour),
  )(f: (Fixture, Clock.TimeControl) => A < S): A < (S & Async) =
    Clock.withTimeControl { clock =>
      for
        _        <- clock.set(kyo.Instant.fromJava(t0))
        bus      <- EventBus.init
        delivery <- AtomicRef.init(Map.empty[OtpChallengeId, OtpCode]).map(new RecordingOtpDelivery(_))
        credRef  <- AtomicRef.init(Map.empty[EmailAddress, PasswordCredential])
        otpRef   <- AtomicRef.init(Map.empty[OtpChallengeId, OtpChallenge])
        sessRef  <- AtomicRef.init(Map.empty[SessionToken, StoredSession])
        counter  <- AtomicLong.init(0L)
        creds = new CredentialRepository:
                  override def findByEmail(email: EmailAddress): Option[PasswordCredential] < Async = credRef.get.map(_.get(email))
                  override def save(credential: PasswordCredential): Unit < Async =
                    credRef.update(_ + (credential.email -> credential))
        otps = new OtpRepository:
                 override def findById(id: OtpChallengeId): Option[OtpChallenge] < Async = otpRef.get.map(_.get(id))
                 override def save(challenge: OtpChallenge): Unit < Async =
                   otpRef.update(_ + (challenge.id -> challenge))
        sessions = new SessionRepository:
                     override def findByToken(token: SessionToken): Option[StoredSession] < Async = sessRef.get.map(_.get(token))
                     override def save(session: StoredSession): Unit < Async =
                       sessRef.update(_ + (session.token -> session))
                     override def revoke(token: SessionToken): Unit < Async =
                       sessRef.update(_.updatedWith(token)(_.map(_.copy(revoked = true))))
        lookup = new CustomerAccountLookupPort:
                   override def resolveCustomer(identifier: LoginIdentifier): Option[ResolvedAccount] < Async =
                     identifier match
                       case LoginIdentifier.Email(e) if e == customerPrincipal.email =>
                         Some(ResolvedAccount(customerPrincipal, isActive = true))
                       case LoginIdentifier.CompanyRegNo(r) if r.value == "12345678" =>
                         Some(ResolvedAccount(customerPrincipal, isActive = true))
                       case LoginIdentifier.VatId(v) if v.value == "CZ12345678" =>
                         Some(ResolvedAccount(customerPrincipal, isActive = true))
                       case LoginIdentifier.Email(e) if e == suspendedPrincipal.email =>
                         Some(ResolvedAccount(suspendedPrincipal, isActive = false, inactiveReason = Some("Account is Suspended")))
                       case _ => None
        tokens = new TokenGenerator:
                   override def generateSessionToken: SessionToken < Async =
                     counter.incrementAndGet.map(n => SessionToken.unsafe(s"tok-$n"))
                   override def generateOtpChallengeId: OtpChallengeId < Async =
                     counter.incrementAndGet.map(n => OtpChallengeId.unsafe(s"otp-$n"))
                   override def generateOtpCode: OtpCode < Async =
                     counter.incrementAndGet.map(n => OtpCode.unsafe(f"${(100000 + n) % 1000000}%06d"))
                   override def generateSalt: String < Async =
                     counter.incrementAndGet.map(n => s"salt-$n")
        svc = IdentityCoreLayers.make(creds, otps, sessions, lookup, delivery, tokens, bus, config)
        out <- f(Fixture(svc, bus, delivery), clock)
      yield out
    }

  "IdentityDomainSpec" - {
    "supports email+password login for both Customer and Employee principals, and validates/revokes sessions" in {
      withFixture() { (fx, _) =>
        val svc = fx.svc
        for
          queue     <- fx.bus.subscribe
          _         <- svc.registerPasswordCredential(
                         RegisterPasswordCmd(customerPrincipal, customerPrincipal.email, NonEmptyString.unsafe("secret123"))
                       )
          _         <- svc.registerPasswordCredential(
                         RegisterPasswordCmd(employeePrincipal, employeePrincipal.email, NonEmptyString.unsafe("empPass!"))
                       )
          badLogin  <- svc.loginWithPassword(customerPrincipal.email, NonEmptyString.unsafe("wrong")).either
          custSess  <- svc.loginWithPassword(customerPrincipal.email, NonEmptyString.unsafe("secret123"))
          empSess   <- svc.loginWithPassword(employeePrincipal.email, NonEmptyString.unsafe("empPass!"))
          validated <- svc.validateSession(custSess.token)
          _         <- svc.logout(custSess.token)
          revoked   <- svc.validateSession(custSess.token).either
          events    <- queue.drain
          ev1        = events(0)
          ev2        = events(1)
          ev3        = events(2)
        yield {
          assert(badLogin == Left(IdentityError.InvalidCredentials))
          assert(custSess.principal == customerPrincipal)
          assert(custSess.method == AuthMethod.Password)
          assert(empSess.principal == employeePrincipal)
          assert(empSess.principal.employeeIdOpt == Some(EmployeeId.unsafe("emp-1")))
          assert(validated.token == custSess.token)
          assert(revoked == Left(IdentityError.SessionNotFound))
          assert(ev1.isInstanceOf[UserLoggedIn])
          assert(ev2.isInstanceOf[UserLoggedIn])
          assert(ev3.isInstanceOf[UserLoggedOut])
        }
      }
    }

    "supports agency OTP login by IČO / DIČ, rejects inactive accounts, and enforces single-use + attempt limits" in {
      withFixture() { (fx, clock) =>
      val svc      = fx.svc
      val delivery = fx.delivery
      for
        suspendedRes <- svc.requestCustomerOtp(LoginIdentifier.Email(suspendedPrincipal.email)).either
        chIco        <- svc.requestCustomerOtp(LoginIdentifier.CompanyRegNo(NonEmptyString.unsafe("12345678")))
        codeOpt      <- delivery.codeFor(chIco.challengeId)
        wrongAttempt <- svc.verifyOtp(chIco.challengeId, OtpCode.unsafe("000000")).either
        session      <- svc.verifyOtp(chIco.challengeId, codeOpt.get)
        replay       <- svc.verifyOtp(chIco.challengeId, codeOpt.get).either
        // Second challenge via VAT ID (DIČ) to test expiry
        chVat        <- svc.requestCustomerOtp(LoginIdentifier.VatId(NonEmptyString.unsafe("CZ12345678")))
        vatCode      <- delivery.codeFor(chVat.challengeId)
        _            <- clock.advance(6.minutes)
        expired      <- svc.verifyOtp(chVat.challengeId, vatCode.get).either
      yield {
        assert(suspendedRes.isLeft)
        assert(chIco.maskedDestination == "ja***@printshoppro.cz")
        assert(wrongAttempt == Left(IdentityError.OtpInvalid(2)))
        assert(session.principal == customerPrincipal)
        assert(session.method == AuthMethod.Otp)
        assert(replay == Left(IdentityError.OtpAlreadyUsed(chIco.challengeId)))
        assert(expired == Left(IdentityError.OtpExpired(chVat.challengeId)))
      }
      }
    }

    "session expires after configured TTL" in {
      withFixture() { (fx, clock) =>
      val svc = fx.svc
      for
        _    <- svc.registerPasswordCredential(
                  RegisterPasswordCmd(customerPrincipal, customerPrincipal.email, NonEmptyString.unsafe("pass123"))
                )
        sess <- svc.loginWithPassword(customerPrincipal.email, NonEmptyString.unsafe("pass123"))
        _    <- clock.advance(2.hours)
        res  <- svc.validateSession(sess.token).either
      yield assert(res == Left(IdentityError.SessionExpired))
      }
    }
  }
