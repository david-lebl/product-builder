package mpbuilder.identity

import mpbuilder.identity.core.*
import mpbuilder.shared.*
import zio.*
import zio.test.*
import java.time.Instant

object IdentityDomainSpec extends ZIOSpecDefault:

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

  private final class RecordingOtpDelivery(ref: Ref[Map[OtpChallengeId, OtpCode]]) extends OtpDeliveryPort:
    override def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): UIO[Unit] =
      ref.update(_ + (challengeId -> code))
    def codeFor(challengeId: OtpChallengeId): UIO[Option[OtpCode]] =
      ref.get.map(_.get(challengeId))

  private def testLayer(
      config: IdentityConfig = IdentityConfig(otpTtl = 5.minutes, otpMaxAttempts = 3, sessionTtl = 1.hour),
  ): ULayer[IdentityService & EventBus & RecordingOtpDelivery] =
    val busLayer = EventBus.live
    val deliveryLayer: ULayer[RecordingOtpDelivery] =
      ZLayer.fromZIO(Ref.make(Map.empty[OtpChallengeId, OtpCode]).map(new RecordingOtpDelivery(_)))

    val credLayer: ULayer[CredentialRepository] = ZLayer.fromZIO {
      Ref.make(Map.empty[EmailAddress, PasswordCredential]).map { ref =>
        new CredentialRepository:
          override def findByEmail(email: EmailAddress): UIO[Option[PasswordCredential]] = ref.get.map(_.get(email))
          override def save(credential: PasswordCredential): UIO[Unit]                   = ref.update(_ + (credential.email -> credential))
      }
    }

    val otpLayer: ULayer[OtpRepository] = ZLayer.fromZIO {
      Ref.make(Map.empty[OtpChallengeId, OtpChallenge]).map { ref =>
        new OtpRepository:
          override def findById(id: OtpChallengeId): UIO[Option[OtpChallenge]] = ref.get.map(_.get(id))
          override def save(challenge: OtpChallenge): UIO[Unit]                = ref.update(_ + (challenge.id -> challenge))
      }
    }

    val sessionLayer: ULayer[SessionRepository] = ZLayer.fromZIO {
      Ref.make(Map.empty[SessionToken, StoredSession]).map { ref =>
        new SessionRepository:
          override def findByToken(token: SessionToken): UIO[Option[StoredSession]] = ref.get.map(_.get(token))
          override def save(session: StoredSession): UIO[Unit]                      = ref.update(_ + (session.token -> session))
          override def revoke(token: SessionToken): UIO[Unit] =
            ref.update(_.updatedWith(token)(_.map(_.copy(revoked = true))))
      }
    }

    val lookupLayer: ULayer[CustomerAccountLookupPort] = ZLayer.succeed {
      new CustomerAccountLookupPort:
        override def resolveCustomer(identifier: LoginIdentifier): UIO[Option[ResolvedAccount]] =
          ZIO.succeed(identifier match
            case LoginIdentifier.Email(e) if e == customerPrincipal.email =>
              Some(ResolvedAccount(customerPrincipal, isActive = true))
            case LoginIdentifier.CompanyRegNo(r) if r.value == "12345678" =>
              Some(ResolvedAccount(customerPrincipal, isActive = true))
            case LoginIdentifier.VatId(v) if v.value == "CZ12345678" =>
              Some(ResolvedAccount(customerPrincipal, isActive = true))
            case LoginIdentifier.Email(e) if e == suspendedPrincipal.email =>
              Some(ResolvedAccount(suspendedPrincipal, isActive = false, inactiveReason = Some("Account is Suspended")))
            case _ => None
          )
    }

    val tokenGenLayer: ULayer[TokenGenerator] = ZLayer.fromZIO {
      Ref.make(0L).map { counter =>
        new TokenGenerator:
          override def generateSessionToken: UIO[SessionToken] =
            counter.updateAndGet(_ + 1).map(n => SessionToken.unsafe(s"tok-$n"))
          override def generateOtpChallengeId: UIO[OtpChallengeId] =
            counter.updateAndGet(_ + 1).map(n => OtpChallengeId.unsafe(s"otp-$n"))
          override def generateOtpCode: UIO[OtpCode] =
            counter.updateAndGet(_ + 1).map(n => OtpCode.unsafe(f"${(100000 + n) % 1000000}%06d"))
          override def generateSalt: UIO[String] =
            counter.updateAndGet(_ + 1).map(n => s"salt-$n")
      }
    }

    val svcLayer =
      (credLayer ++ otpLayer ++ sessionLayer ++ lookupLayer ++ deliveryLayer ++ tokenGenLayer ++ busLayer) >>>
        IdentityCoreLayers.layer(config)

    busLayer ++ deliveryLayer ++ svcLayer

  def spec = suite("IdentityDomainSpec")(
    test("supports email+password login for both Customer and Employee principals, and validates/revokes sessions") {
      ZIO.scoped {
        for
          _         <- TestClock.setTime(Instant.parse("2026-04-01T10:00:00Z"))
          svc       <- ZIO.service[IdentityService]
          bus       <- ZIO.service[EventBus]
          queue     <- bus.subscribe
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
          ev1       <- queue.take
          ev2       <- queue.take
          ev3       <- queue.take
        yield assertTrue(
          badLogin == Left(IdentityError.InvalidCredentials),
          custSess.principal == customerPrincipal,
          custSess.method == AuthMethod.Password,
          empSess.principal == employeePrincipal,
          empSess.principal.employeeIdOpt == Some(EmployeeId.unsafe("emp-1")),
          validated.token == custSess.token,
          revoked == Left(IdentityError.SessionNotFound),
          ev1.isInstanceOf[UserLoggedIn],
          ev2.isInstanceOf[UserLoggedIn],
          ev3.isInstanceOf[UserLoggedOut],
        )
      }.provide(testLayer())
    },
    test("supports agency OTP login by IČO / DIČ, rejects inactive accounts, and enforces single-use + attempt limits") {
      for
        _            <- TestClock.setTime(Instant.parse("2026-04-01T10:00:00Z"))
        svc          <- ZIO.service[IdentityService]
        delivery     <- ZIO.service[RecordingOtpDelivery]
        suspendedRes <- svc.requestCustomerOtp(LoginIdentifier.Email(suspendedPrincipal.email)).either
        chIco        <- svc.requestCustomerOtp(LoginIdentifier.CompanyRegNo(NonEmptyString.unsafe("12345678")))
        codeOpt      <- delivery.codeFor(chIco.challengeId)
        wrongAttempt <- svc.verifyOtp(chIco.challengeId, OtpCode.unsafe("000000")).either
        session      <- svc.verifyOtp(chIco.challengeId, codeOpt.get)
        replay       <- svc.verifyOtp(chIco.challengeId, codeOpt.get).either
        // Second challenge via VAT ID (DIČ) to test expiry
        chVat        <- svc.requestCustomerOtp(LoginIdentifier.VatId(NonEmptyString.unsafe("CZ12345678")))
        vatCode      <- delivery.codeFor(chVat.challengeId)
        _            <- TestClock.adjust(6.minutes)
        expired      <- svc.verifyOtp(chVat.challengeId, vatCode.get).either
      yield assertTrue(
        suspendedRes.isLeft,
        chIco.maskedDestination == "ja***@printshoppro.cz",
        wrongAttempt == Left(IdentityError.OtpInvalid(2)),
        session.principal == customerPrincipal,
        session.method == AuthMethod.Otp,
        replay == Left(IdentityError.OtpAlreadyUsed(chIco.challengeId)),
        expired == Left(IdentityError.OtpExpired(chVat.challengeId)),
      )
    }.provide(testLayer()),
    test("session expires after configured TTL") {
      for
        _    <- TestClock.setTime(Instant.parse("2026-04-01T10:00:00Z"))
        svc  <- ZIO.service[IdentityService]
        _    <- svc.registerPasswordCredential(
                  RegisterPasswordCmd(customerPrincipal, customerPrincipal.email, NonEmptyString.unsafe("pass123"))
                )
        sess <- svc.loginWithPassword(customerPrincipal.email, NonEmptyString.unsafe("pass123"))
        _    <- TestClock.adjust(2.hours)
        res  <- svc.validateSession(sess.token).either
      yield assertTrue(res == Left(IdentityError.SessionExpired))
    }.provide(testLayer()),
  )
